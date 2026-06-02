package com.scrimslegends.app.viewmodel

import timber.log.Timber
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scrimslegends.app.data.model.Conversation
import com.scrimslegends.app.data.model.DeliveryStatus
import com.scrimslegends.app.data.model.Message
import com.scrimslegends.app.data.model.MessageType
import com.scrimslegends.app.data.model.MessageWithDelivery
import com.scrimslegends.app.data.repository.MessageRepositoryInterface
import com.scrimslegends.app.data.service.ChatConnectionState
import com.scrimslegends.app.data.service.SupabaseStorageUpload
import com.scrimslegends.app.util.FreeTierConfig
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

import java.util.UUID
import java.util.Collections

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MessageViewModel @Inject constructor(
    private val messageRepository: MessageRepositoryInterface
) : ViewModel() {

    private var chatSubscriptionJob: Job? = null
    private var chatPollingJob: Job? = null
    private var convPollingJob: Job? = null
    private var typingDebounceJob: Job? = null
    private var conversationUpdatesJob: Job? = null
    private var loadConversationsJob: Job? = null

    // ── UI State ──
    private val _conversations = MutableStateFlow<List<Conversation>>(emptyList())
    val conversations: StateFlow<List<Conversation>> = _conversations.asStateFlow()

    private val _selectedConversation = MutableStateFlow<Conversation?>(null)
    val selectedConversation: StateFlow<Conversation?> = _selectedConversation.asStateFlow()

    private val _messagesWithDelivery = MutableStateFlow<List<MessageWithDelivery>>(emptyList())
    val messagesWithDelivery: StateFlow<List<MessageWithDelivery>> = _messagesWithDelivery.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _connectionState = MutableStateFlow(ChatConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ChatConnectionState> = _connectionState.asStateFlow()

    private val _typingIndicator = MutableStateFlow(false)
    val typingIndicator: StateFlow<Boolean> = _typingIndicator.asStateFlow()

    // ── Reply-to state ──
    private val _replyingToMessage = MutableStateFlow<MessageWithDelivery?>(null)
    val replyingToMessage: StateFlow<MessageWithDelivery?> = _replyingToMessage.asStateFlow()

    // ── Pagination state ──
    private val _isLoadingOlder = MutableStateFlow(false)
    val isLoadingOlder: StateFlow<Boolean> = _isLoadingOlder.asStateFlow()
    private var _hasMoreMessages = true
    val hasMoreMessages: Boolean get() = _hasMoreMessages

    // ── Internal: Map-based O(1) message storage ──
    // Key = message.id (or clientMessageId for pending), preserves insertion order for sorting
    private val _messageMap = Collections.synchronizedMap(LinkedHashMap<String, MessageWithDelivery>())

    init {
        viewModelScope.launch {
            messageRepository.observeConnectionState()
                .distinctUntilChanged()
                .collect { _connectionState.value = it }
        }
    }

    fun loadConversations(userId: String, isRefresh: Boolean = false) {
        loadConversationsJob?.cancel()
        loadConversationsJob = viewModelScope.launch {
            if (isRefresh) _isRefreshing.value = true
            _isLoading.value = true
            messageRepository.getConversationsForUser(userId, forceRefresh = true).collect { result ->
                result.onSuccess { _conversations.value = it }
                    .onFailure { _error.value = it.message ?: "Failed to load conversations" }
                _isLoading.value = false
                _isRefreshing.value = false
            }
        }
    }

    fun loadConversation(conversationId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            messageRepository.getConversationById(conversationId).collect { result ->
                result.onSuccess { _selectedConversation.value = it }
                    .onFailure { _error.value = it.message ?: "Failed to load conversation" }
                _isLoading.value = false
            }
        }
    }

    fun markAsRead(conversationId: String, userId: String) {
        viewModelScope.launch {
            messageRepository.markConversationAsRead(conversationId, userId).collect {}
        }
    }

    /**
     * Pre-populate selectedConversation from the list so ChatScreen
     * has data instantly without waiting for a network call.
     */
    fun preSelectConversation(conversation: Conversation) {
        _selectedConversation.value = conversation
        if (conversation.messages.isNotEmpty()) {
            setMessagesWithDelivery(conversation.messages.map { MessageWithDelivery(message = it) })
        }
    }

    // ── Exponential backoff state (free-tier protection) ──
    private var convPollFailures = 0
    private var chatPollFailures = 0

    // ── Conversation list polling (REST fallback only) ──
    fun startConversationsPolling(userId: String) {
        convPollingJob?.cancel()
        convPollFailures = 0
        convPollingJob = viewModelScope.launch {
            while (isActive) {
                val result = messageRepository.getConversationsForUser(userId, forceRefresh = true)
                result.collect { res ->
                    res.onSuccess {
                        _conversations.value = it
                        convPollFailures = 0 // Reset on success
                    }.onFailure { err ->
                        val code = extractHttpCode(err.message)
                        if (code == 429 || code == 503) {
                            convPollFailures = (convPollFailures + 1).coerceAtMost(FreeTierConfig.BACKOFF_MAX_FAILURES)
                        }
                    }
                }
                val backoff = calculateBackoff(convPollFailures)
                delay(FreeTierConfig.CONVERSATION_POLL_INTERVAL_MS + backoff)
            }
        }
    }

    fun stopConversationsPolling() {
        convPollingJob?.cancel()
    }

    // ── Chat screen subscription (Realtime primary, no conflicting polling) ──
    fun startChatSubscription(conversationId: String, userId: String) {
        chatSubscriptionJob?.cancel()
        chatPollingJob?.cancel()
        messageRepository.unsubscribeFromMessages(conversationId)

        // Fire-and-forget: mark as read in background (don't block message loading)
        viewModelScope.launch {
            messageRepository.markConversationAsRead(conversationId, userId).collect {}
        }

        chatSubscriptionJob = viewModelScope.launch {
            // Only fetch from network if we don't already have messages loaded
            val current = _selectedConversation.value
            val needsFetch = current?.id != conversationId || current.messages.isEmpty()
            if (needsFetch) {
                clearMessages()
                messageRepository.getConversationById(conversationId).collect { result ->
                    result.onSuccess { conv ->
                        _selectedConversation.value = conv
                        conv?.messages?.let { msgs ->
                            setMessagesWithDelivery(msgs.map { MessageWithDelivery(message = it) })
                        }
                    }
                }
            }

            // Subscribe to new messages via Realtime
            // If we just fetched from network above, skip the bridge fetch to avoid double-loading
            val skipBridge = needsFetch
            messageRepository.subscribeToMessages(conversationId, skipBridgeFetch = skipBridge).collect { newMessage ->
                integrateMessage(newMessage)
            }
        }

        chatPollFailures = 0
        chatPollingJob = viewModelScope.launch {
            while (isActive) {
                delay(FreeTierConfig.CHAT_FALLBACK_POLL_INTERVAL_MS)
                // Only poll when realtime is not connected (fallback)
                if (_connectionState.value != ChatConnectionState.CONNECTED) {
                    val result = messageRepository.getConversationById(conversationId)
                    result.collect { res ->
                        res.onSuccess { conv ->
                            val polled = conv?.messages.orEmpty()
                            if (polled.isNotEmpty()) {
                                mergeServerMessages(polled)
                            }
                            chatPollFailures = 0
                        }.onFailure { err ->
                            val code = extractHttpCode(err.message)
                            if (code == 429 || code == 503) {
                                chatPollFailures = (chatPollFailures + 1).coerceAtMost(FreeTierConfig.BACKOFF_MAX_FAILURES)
                            }
                        }
                    }
                }
            }
        }

        // Typing status updates
        conversationUpdatesJob?.cancel()
        conversationUpdatesJob = viewModelScope.launch {
            messageRepository.subscribeToConversation(conversationId).collect { updated ->
                val current = _selectedConversation.value
                if (current != null && current.id == conversationId) {
                    _selectedConversation.value = current.copy(
                        isParticipantATyping = updated.isParticipantATyping,
                        isParticipantBTyping = updated.isParticipantBTyping
                    )
                    _typingIndicator.value = updated.isOtherTyping(userId)
                }
            }
        }
    }

    fun stopChatSubscription(conversationId: String) {
        chatSubscriptionJob?.cancel()
        chatPollingJob?.cancel()
        conversationUpdatesJob?.cancel()
        typingDebounceJob?.cancel()
        messageRepository.unsubscribeFromMessages(conversationId)
    }

    // ── Send message (idempotent with clientMessageId) ──
    fun sendMessage(conversationId: String, senderId: String, senderName: String, content: String) {
        val clientMessageId = "cm_${UUID.randomUUID()}"
        val replyTarget = _replyingToMessage.value

        val tempMessage = Message(
            id = clientMessageId,
            conversationId = conversationId,
            senderId = senderId,
            senderName = senderName,
            content = content,
            timestamp = System.currentTimeMillis(),
            isRead = true,
            type = MessageType.TEXT,
            replyToId = replyTarget?.message?.id,
            replyToSnippet = replyTarget?.message?.content?.take(80),
            replyToSenderName = replyTarget?.message?.senderName
        )

        // Optimistic UI: show as SENDING
        addOrUpdateMessage(clientMessageId, MessageWithDelivery(
            message = tempMessage,
            status = DeliveryStatus.SENDING,
            clientMessageId = clientMessageId
        ))
        _replyingToMessage.value = null // Clear reply after sending

        viewModelScope.launch {
            messageRepository.sendMessage(
                conversationId = conversationId,
                senderId = senderId,
                senderName = senderName,
                content = content,
                type = MessageType.TEXT,
                clientMessageId = clientMessageId,
                replyToId = replyTarget?.message?.id,
                replyToSnippet = replyTarget?.message?.content?.take(80),
                replyToSenderName = replyTarget?.message?.senderName
            ).collect { delivery ->
                updateDeliveryState(clientMessageId, delivery)
                if (delivery.status == DeliveryStatus.FAILED) {
                    _error.value = delivery.errorReason ?: "Failed to send"
                }
            }
        }
    }

    fun retryMessage(clientMessageId: String) {
        viewModelScope.launch {
            updateDeliveryState(clientMessageId, MessageWithDelivery(
                message = Message(id = clientMessageId),
                status = DeliveryStatus.SENDING,
                clientMessageId = clientMessageId
            ))
            messageRepository.retryMessage(clientMessageId).collect { delivery ->
                updateDeliveryState(clientMessageId, delivery)
            }
        }
    }

    fun cancelMessage(clientMessageId: String) {
        viewModelScope.launch {
            messageRepository.cancelMessage(clientMessageId)
            removeMessage(clientMessageId)
        }
    }

    fun sendImageMessage(conversationId: String, senderId: String, senderName: String, imageBytes: ByteArray) {
        val clientMessageId = "cm_${UUID.randomUUID()}"
        val replyTarget = _replyingToMessage.value

        val tempMessage = Message(
            id = clientMessageId,
            conversationId = conversationId,
            senderId = senderId,
            senderName = senderName,
            content = "[Image]",
            timestamp = System.currentTimeMillis(),
            isRead = true,
            type = MessageType.IMAGE,
            replyToId = replyTarget?.message?.id,
            replyToSnippet = replyTarget?.message?.content?.take(80),
            replyToSenderName = replyTarget?.message?.senderName
        )

        addOrUpdateMessage(clientMessageId, MessageWithDelivery(
            message = tempMessage,
            status = DeliveryStatus.SENDING,
            clientMessageId = clientMessageId
        ))
        _replyingToMessage.value = null

        viewModelScope.launch {
            _isLoading.value = true
            val path = "chat/$conversationId/${System.currentTimeMillis()}.png"
            val uploadResult = SupabaseStorageUpload.uploadFile("chat-media", path, imageBytes, "image/png")

            uploadResult.onSuccess { url ->
                messageRepository.sendMessage(
                    conversationId = conversationId,
                    senderId = senderId,
                    senderName = senderName,
                    content = "[Image]",
                    type = MessageType.IMAGE,
                    clientMessageId = clientMessageId,
                    imageUrl = url,
                    replyToId = replyTarget?.message?.id,
                    replyToSnippet = replyTarget?.message?.content?.take(80),
                    replyToSenderName = replyTarget?.message?.senderName
                ).collect { delivery ->
                    updateDeliveryState(clientMessageId, delivery)
                    if (delivery.status == DeliveryStatus.FAILED) {
                        _error.value = delivery.errorReason ?: "Failed to send image"
                    }
                }
            }.onFailure {
                _error.value = "Image upload failed: ${it.message}"
                updateDeliveryState(clientMessageId, MessageWithDelivery(
                    message = tempMessage,
                    status = DeliveryStatus.FAILED,
                    clientMessageId = clientMessageId,
                    errorReason = "Image upload failed: ${it.message}"
                ))
            }
            _isLoading.value = false
        }
    }


    // ── Typing status (debounced + distinctUntilChanged + auto-timeout) ──
    fun updateTypingStatus(conversationId: String, userId: String, isTyping: Boolean) {
        typingDebounceJob?.cancel()
        typingDebounceJob = viewModelScope.launch {
            if (isTyping) {
                messageRepository.setTypingStatus(conversationId, userId, true).collect {}
                delay(FreeTierConfig.TYPING_INDICATOR_DURATION_MS)
                messageRepository.setTypingStatus(conversationId, userId, false).collect {}
            } else {
                messageRepository.setTypingStatus(conversationId, userId, false).collect {}
            }
        }
    }

    // ── Apply message (scrim application) ──
    fun sendApplyMessage(
        scrimId: String,
        scrimTitle: String,
        applicantId: String,
        applicantName: String,
        applicantTeamId: String,
        applicantTeamName: String,
        scrimCreatorId: String,
        scrimCreatorName: String,
        scrimCreatorTeamId: String,
        scrimCreatorTeamName: String,
        teamPlayerCount: Int,
        teamMaxPlayers: Int,
        onConversationCreated: (Conversation) -> Unit = {}
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            messageRepository.sendApplyMessage(
                scrimId, scrimTitle, applicantId, applicantName, applicantTeamId, applicantTeamName,
                scrimCreatorId, scrimCreatorName, scrimCreatorTeamId, scrimCreatorTeamName,
                teamPlayerCount, teamMaxPlayers
            ).collect { result ->
                result.onSuccess {
                    _selectedConversation.value = it
                    onConversationCreated(it)
                }.onFailure { exception ->
                    _error.value = "Failed to create conversation: ${exception.message}"
                }
                _isLoading.value = false
            }
        }
    }

    // ── Direct message start ──
    fun startDirectConversation(
        senderId: String,
        senderName: String,
        recipientId: String,
        recipientName: String
    ) {
        viewModelScope.launch {
            Timber.d("MessageFlow", "VM: startDirectConversation sender=$senderId recipient=$recipientId")
            _isLoading.value = true
            try {
                kotlinx.coroutines.withTimeout(10000) {
                    messageRepository.startDirectConversation(
                        senderId, senderName, recipientId, recipientName
                    ).collect { result ->
                        result.onSuccess {
                            Timber.d("MessageFlow", "VM: success convId=${it.id}")
                            _selectedConversation.value = null
                            _selectedConversation.value = it
                            loadConversations(senderId)
                        }.onFailure {
                            Timber.d("MessageFlow", "VM: failure error=${it.message}")
                            _error.value = it.message ?: "Failed to start conversation"
                        }
                    }
                }
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                Timber.d("MessageFlow", "VM: timeout after 10s")
                _error.value = "Request timed out. Check your connection."
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                Timber.d("MessageFlow", "VM: exception ${e.javaClass.simpleName}: ${e.message}")
                _error.value = e.message ?: "Unknown error"
            }
            _isLoading.value = false
        }
    }

    // ── Ensure team conversations exist for all user's teams ──
    fun ensureTeamConversations(teams: List<com.scrimslegends.app.data.model.Team>, userId: String) {
        viewModelScope.launch {
            var anyCreated = false
            teams.forEach { team ->
                try {
                    messageRepository.getOrCreateTeamConversation(
                        teamId = team.id,
                        teamName = team.name,
                        leaderId = team.leaderId,
                        leaderName = team.players.find { it.id == team.leaderId }?.name ?: ""
                    ).collect { result ->
                        result.onSuccess { anyCreated = true }
                        result.onFailure { e ->
                            Timber.w("MessageVM", "Failed to ensure team conversation for ${team.name}: ${e.message}")
                        }
                    }
                } catch (e: Exception) {
                    if (e is kotlinx.coroutines.CancellationException) throw e
                    Timber.w("MessageVM", "Exception ensuring team conversation for ${team.name}: ${e.message}")
                }
            }
            // Refresh conversation list so team chats appear immediately
            if (anyCreated) {
                loadConversations(userId, isRefresh = true)
            }
        }
    }

    // ── State helpers (Map-based O(1)) ──

    /**
     * Rebuilds the sorted list from the internal map and emits to UI.
     * Call this after any Map mutation.
     */
    private fun emitMessagesFromMap() {
        val currentConv = _selectedConversation.value
        val historyClearedAt = currentConv?.historyClearedAt ?: 0L
        val sorted = _messageMap.values
            .filter { it.message.timestamp >= historyClearedAt }
            .sortedBy { it.message.timestamp }
        _messagesWithDelivery.value = sorted
        // Sync back to selectedConversation (lightweight — only if changed)
        if (currentConv != null) {
            val newMessageList = sorted.map { it.message }
            if (currentConv.messages.size != newMessageList.size ||
                currentConv.messages.zip(newMessageList).any { it.first != it.second }) {
                _selectedConversation.value = currentConv.copy(messages = newMessageList)
            }
        }
    }

    /** Add or update by key (O(1)) */
    private fun addOrUpdateMessage(key: String, msg: MessageWithDelivery) {
        _messageMap[key] = msg
        emitMessagesFromMap()
    }

    /** Remove by key (O(1)) */
    private fun removeMessage(clientMessageId: String) {
        _messageMap.remove(clientMessageId)
        emitMessagesFromMap()
    }

    private fun integrateMessage(newMessage: Message) {
        // O(1) lookup by server message ID
        val existing = _messageMap[newMessage.id]
        if (existing != null) {
            // Update existing (e.g., read receipt, edit, delete)
            addOrUpdateMessage(newMessage.id, existing.copy(message = newMessage))
            return
        }
        // O(1) lookup by pending signature: SENDING + senderId + content + time proximity
        val pendingKey = _messageMap.entries.find { (_, v) ->
            v.status == DeliveryStatus.SENDING &&
                v.message.senderId == newMessage.senderId &&
                v.message.content == newMessage.content &&
                kotlin.math.abs(v.message.timestamp - newMessage.timestamp) < 30_000
        }?.key
        if (pendingKey != null) {
            val pending = _messageMap.remove(pendingKey)
            if (pending != null) {
                addOrUpdateMessage(newMessage.id, MessageWithDelivery(
                    message = newMessage,
                    status = DeliveryStatus.SENT,
                    clientMessageId = pending.clientMessageId
                ))
                return
            }
        }
        // New message
        addOrUpdateMessage(newMessage.id, MessageWithDelivery(message = newMessage))
    }

    private fun mergeServerMessages(messages: List<Message>) {
        messages.forEach { integrateMessage(it) }
    }

    private fun updateDeliveryState(clientMessageId: String, delivery: MessageWithDelivery) {
        _messageMap[clientMessageId] = delivery
        // Also index by server id if now confirmed
        if (delivery.status == DeliveryStatus.SENT && delivery.message.id.isNotBlank() && delivery.message.id != clientMessageId) {
            _messageMap[delivery.message.id] = delivery
        }
        emitMessagesFromMap()
    }

    /** Initialize map from a list of messages (called on first load) */
    private fun setMessagesWithDelivery(messages: List<MessageWithDelivery>) {
        _messageMap.clear()
        messages.forEach { msg ->
            val key = msg.clientMessageId ?: msg.message.id
            _messageMap[key] = msg
        }
        emitMessagesFromMap()
    }

    // ── Reply-to ──
    fun setReplyTarget(message: MessageWithDelivery) {
        _replyingToMessage.value = message
    }
    fun clearReply() {
        _replyingToMessage.value = null
    }

    // ── Delete message ──
    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            // Optimistic: mark as deleted locally first
            val existing = _messageMap[messageId]
            if (existing != null) {
                addOrUpdateMessage(messageId, existing.copy(
                    message = existing.message.copy(isDeleted = true, content = "")
                ))
            }
            val result = messageRepository.deleteMessage(messageId)
            result.onFailure { e ->
                Timber.w("MessageVM", "Failed to delete message: ${e.message}")
                _error.value = "Failed to delete: ${e.message}"
            }
        }
    }

    // ── Load older messages (pagination) ──
    fun loadOlderMessages(conversationId: String) {
        val oldest = _messagesWithDelivery.value.firstOrNull()?.message?.timestamp ?: return
        viewModelScope.launch {
            _isLoadingOlder.value = true
            val result = messageRepository.loadOlderMessages(conversationId, oldest, limit = 50)
            result.onSuccess { older ->
                if (older.isEmpty()) {
                    _hasMoreMessages = false
                } else {
                    older.forEach { msg ->
                        if (msg.id !in _messageMap) {
                            _messageMap[msg.id] = MessageWithDelivery(message = msg)
                        }
                    }
                    emitMessagesFromMap()
                }
            }.onFailure { e ->
                Timber.w("MessageVM", "Failed to load older messages: ${e.message}")
            }
            _isLoadingOlder.value = false
        }
    }

    /** Reset pagination state when switching conversations */
    fun resetPagination() {
        _hasMoreMessages = true
        _isLoadingOlder.value = false
        _replyingToMessage.value = null
    }

    /** Clear loaded messages completely */
    fun clearMessages() {
        _messageMap.clear()
        emitMessagesFromMap()
    }

    /** Clear chat history for the current user */
    fun clearChatHistory(conversationId: String) {
        viewModelScope.launch {
            val result = messageRepository.clearChatHistory(conversationId)
            result.onSuccess {
                // Update local conversation state to reflect current time as historyClearedAt
                val currentConv = _selectedConversation.value
                if (currentConv != null && currentConv.id == conversationId) {
                    val updatedConv = currentConv.copy(historyClearedAt = System.currentTimeMillis())
                    _selectedConversation.value = updatedConv
                    emitMessagesFromMap() // Will re-filter and clear old messages from UI
                }
            }.onFailure { e ->
                Timber.w("MessageVM", "Failed to clear chat history: ${e.message}")
                _error.value = "Failed to clear history: ${e.message}"
            }
        }
    }

    fun setError(message: String) { _error.value = message }
    fun clearError() { _error.value = null }
    fun clearRefreshing() { _isRefreshing.value = false }

    // ── Free-tier backoff helpers ──

    /** Extract HTTP status code from exception message (e.g., "HTTP 429" -> 429) */
    private fun extractHttpCode(message: String?): Int {
        if (message == null) return 0
        val regex = Regex("HTTP\\s*(\\d{3})")
        return regex.find(message)?.groupValues?.get(1)?.toIntOrNull() ?: 0
    }

    /** Calculate exponential backoff delay based on consecutive failures. */
    private fun calculateBackoff(failureCount: Int): Long {
        if (failureCount <= 0) return 0L
        var delay = FreeTierConfig.BACKOFF_INITIAL_MS.toDouble()
        repeat(failureCount) { delay *= FreeTierConfig.BACKOFF_MULTIPLIER }
        return delay.toLong().coerceAtMost(FreeTierConfig.BACKOFF_MAX_MS)
    }

    override fun onCleared() {
        super.onCleared()
        chatSubscriptionJob?.cancel()
        chatPollingJob?.cancel()
        convPollingJob?.cancel()
        typingDebounceJob?.cancel()
        conversationUpdatesJob?.cancel()
        // Unsubscribe all active channels
        _selectedConversation.value?.id?.let { messageRepository.unsubscribeFromMessages(it) }
    }
}

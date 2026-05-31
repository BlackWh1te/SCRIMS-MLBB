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

    init {
        viewModelScope.launch {
            messageRepository.observeConnectionState()
                .distinctUntilChanged()
                .collect { _connectionState.value = it }
        }
    }

    // ── Conversation list ──
    fun loadConversations(userId: String, isRefresh: Boolean = false) {
        viewModelScope.launch {
            if (isRefresh) _isRefreshing.value = true
            _isLoading.value = true
            messageRepository.getConversationsForUser(userId, forceRefresh = true).collect { result ->
                result.onSuccess { _conversations.value = it }
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
                _isLoading.value = false
            }
        }
    }

    fun markAsRead(conversationId: String, userId: String) {
        viewModelScope.launch {
            messageRepository.markConversationAsRead(conversationId, userId).collect {}
        }
    }

    // ── Conversation list polling (REST fallback only) ──
    fun startConversationsPolling(userId: String) {
        convPollingJob?.cancel()
        convPollingJob = viewModelScope.launch {
            while (isActive) {
                messageRepository.getConversationsForUser(userId, forceRefresh = true).collect { result ->
                    result.onSuccess { _conversations.value = it }
                }
                delay(10_000)
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

        chatSubscriptionJob = viewModelScope.launch {
            // Mark as read on enter
            messageRepository.markConversationAsRead(conversationId, userId).collect {}

            // Load initial conversation
            messageRepository.getConversationById(conversationId).collect { result ->
                result.onSuccess { conv ->
                    _selectedConversation.value = conv
                    conv?.messages?.let { msgs ->
                        setMessagesWithDelivery(msgs.map { MessageWithDelivery(message = it) })
                    }
                }
            }

            // Subscribe to new messages via Realtime
            messageRepository.subscribeToMessages(conversationId).collect { newMessage ->
                integrateMessage(newMessage)
            }
        }

        chatPollingJob = viewModelScope.launch {
            while (isActive) {
                delay(3_000)
                messageRepository.getConversationById(conversationId).collect { result ->
                    result.onSuccess { conv ->
                        val polled = conv?.messages.orEmpty()
                        if (polled.isNotEmpty()) {
                            mergeServerMessages(polled)
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
        val tempMessage = Message(
            id = clientMessageId,
            conversationId = conversationId,
            senderId = senderId,
            senderName = senderName,
            content = content,
            timestamp = System.currentTimeMillis(),
            isRead = true,
            type = MessageType.TEXT
        )

        // Optimistic UI: show as SENDING
        val current = _messagesWithDelivery.value.toMutableList()
        current.add(
            MessageWithDelivery(
                message = tempMessage,
                status = DeliveryStatus.SENDING,
                clientMessageId = clientMessageId
            )
        )
        setMessagesWithDelivery(current)

        viewModelScope.launch {
            messageRepository.sendMessage(
                conversationId = conversationId,
                senderId = senderId,
                senderName = senderName,
                content = content,
                type = MessageType.TEXT,
                clientMessageId = clientMessageId
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
        viewModelScope.launch {
            _isLoading.value = true
            val clientMessageId = "cm_${UUID.randomUUID()}"
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
                    imageUrl = url
                ).collect { _isLoading.value = false }
            }.onFailure {
                _error.value = "Image upload failed: ${it.message}"
                _isLoading.value = false
            }
        }
    }


    // ── Typing status (debounced + distinctUntilChanged + auto-timeout) ──
    fun updateTypingStatus(conversationId: String, userId: String, isTyping: Boolean) {
        typingDebounceJob?.cancel()
        typingDebounceJob = viewModelScope.launch {
            if (isTyping) {
                messageRepository.setTypingStatus(conversationId, userId, true).collect {}
                delay(3000)
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
                Timber.d("MessageFlow", "VM: exception ${e.javaClass.simpleName}: ${e.message}")
                _error.value = e.message ?: "Unknown error"
            }
            _isLoading.value = false
        }
    }

    // ── Ensure team conversations exist for all user's teams ──
    fun ensureTeamConversations(teams: List<com.scrimslegends.app.data.model.Team>) {
        viewModelScope.launch {
            teams.forEach { team ->
                try {
                    messageRepository.getOrCreateTeamConversation(
                        teamId = team.id,
                        teamName = team.name,
                        leaderId = team.leaderId,
                        leaderName = ""
                    ).collect { }
                } catch (_: Exception) { /* Best effort */ }
            }
        }
    }

    // ── State helpers ──
    private fun integrateMessage(newMessage: Message) {
        val current = _messagesWithDelivery.value.toMutableList()
        val existingIndex = current.indexOfFirst { it.message.id == newMessage.id }
        val pendingIndex = current.indexOfFirst {
            it.clientMessageId != null && it.message.content == newMessage.content && it.message.senderId == newMessage.senderId
        }
        when {
            existingIndex != -1 -> {
                // Update existing (e.g., read receipt)
                current[existingIndex] = current[existingIndex].copy(message = newMessage)
            }
            pendingIndex != -1 -> {
                // Replace pending local message with server-confirmed message
                current[pendingIndex] = MessageWithDelivery(
                    message = newMessage,
                    status = DeliveryStatus.SENT,
                    clientMessageId = current[pendingIndex].clientMessageId
                )
            }
            else -> {
                current.add(MessageWithDelivery(message = newMessage))
            }
        }
        setMessagesWithDelivery(current.sortedBy { it.message.timestamp })
    }

    private fun updateDeliveryState(clientMessageId: String, delivery: MessageWithDelivery) {
        val current = _messagesWithDelivery.value.toMutableList()
        val index = current.indexOfFirst { it.clientMessageId == clientMessageId }
        if (index != -1) {
            current[index] = delivery
            setMessagesWithDelivery(current.sortedBy { it.message.timestamp })
        }
    }

    private fun removeMessage(clientMessageId: String) {
        setMessagesWithDelivery(_messagesWithDelivery.value.filter { it.clientMessageId != clientMessageId })
    }

    private fun mergeServerMessages(messages: List<Message>) {
        val current = _messagesWithDelivery.value.toMutableList()
        messages.forEach { serverMessage ->
            val existingIndex = current.indexOfFirst { it.message.id == serverMessage.id }
            val pendingIndex = current.indexOfFirst {
                it.clientMessageId != null &&
                    it.message.content == serverMessage.content &&
                    it.message.senderId == serverMessage.senderId
            }
            when {
                existingIndex != -1 -> current[existingIndex] = current[existingIndex].copy(message = serverMessage)
                pendingIndex != -1 -> current[pendingIndex] = MessageWithDelivery(
                    message = serverMessage,
                    status = DeliveryStatus.SENT,
                    clientMessageId = current[pendingIndex].clientMessageId
                )
                else -> current.add(MessageWithDelivery(message = serverMessage))
            }
        }
        setMessagesWithDelivery(current.sortedBy { it.message.timestamp })
    }

    private fun setMessagesWithDelivery(messages: List<MessageWithDelivery>) {
        _messagesWithDelivery.value = messages
        _selectedConversation.value = _selectedConversation.value?.copy(
            messages = messages.map { it.message }
        )
    }

    fun setError(message: String) { _error.value = message }
    fun clearError() { _error.value = null }
    fun clearRefreshing() { _isRefreshing.value = false }

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

package com.mlbb.scrim.viewmodel

import timber.log.Timber
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mlbb.scrim.data.model.Conversation
import com.mlbb.scrim.data.model.Message
import com.mlbb.scrim.data.model.MessageType
import com.mlbb.scrim.data.repository.MessageRepositoryInterface
import com.mlbb.scrim.data.repository.SupabaseMessageRepository
import com.mlbb.scrim.data.service.SupabaseStorageUpload
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.util.UUID

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MessageViewModel @Inject constructor(
    private val messageRepository: MessageRepositoryInterface
) : ViewModel() {


    private var chatPollingJob: Job? = null
    private var convPollingJob: Job? = null
    private var typingStatusJob: Job? = null
    private var conversationUpdatesJob: Job? = null

    private val _conversations = MutableStateFlow<List<Conversation>>(emptyList())
    val conversations: StateFlow<List<Conversation>> = _conversations.asStateFlow()

    private val _selectedConversation = MutableStateFlow<Conversation?>(null)
    val selectedConversation: StateFlow<Conversation?> = _selectedConversation.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun loadConversations(userId: String, isRefresh: Boolean = false) {
        viewModelScope.launch {
            if (isRefresh) _isRefreshing.value = true
            _isLoading.value = true
            messageRepository.getConversationsForUser(userId).collect { result ->
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

    fun startConversationsPolling(userId: String) {
        convPollingJob?.cancel()
        convPollingJob = viewModelScope.launch {
            while (isActive) {
                messageRepository.getConversationsForUser(userId).collect { result ->
                    result.onSuccess { _conversations.value = it }
                }
                delay(30_000) // Poll every 30s (reduced from 10s — cache handles freshness)
            }
        }
    }

    fun stopConversationsPolling() {
        convPollingJob?.cancel()
    }

    fun startChatPolling(conversationId: String, userId: String) {
        chatPollingJob?.cancel()
        chatPollingJob = viewModelScope.launch {
            // Mark as read when entering
            messageRepository.markConversationAsRead(conversationId, userId).collect {}

            // Load conversation first to ensure _selectedConversation is populated
            messageRepository.getConversationById(conversationId).collect { result ->
                result.onSuccess { _selectedConversation.value = it }
            }

            // Merge Realtime + polling fallback, deduplicate by message ID
            val realtimeFlow = messageRepository.subscribeToMessages(conversationId)
            val pollingFlow = flow {
                while (isActive) {
                    delay(30_000) // Poll every 30s as fallback (reduced from 15s — cache handles freshness)
                    try {
                        messageRepository.getConversationById(conversationId).collect { result ->
                            result.onSuccess { conv ->
                                conv?.messages?.forEach { msg -> emit(msg) }
                            }
                        }
                    } catch (_: Exception) { }
                }
            }
            merge(realtimeFlow, pollingFlow)
                .collect { newMessage ->
                    val current = _selectedConversation.value
                    if (current != null && current.id == conversationId) {
                        val existingIndex = current.messages.indexOfFirst { it.id == newMessage.id }
                        val localIndex = current.messages.indexOfFirst { it.id.startsWith("local_") && it.content == newMessage.content }
                        
                        if (existingIndex == -1) {
                            if (localIndex != -1) {
                                val updatedMessages = current.messages.toMutableList()
                                updatedMessages[localIndex] = newMessage
                                _selectedConversation.value = current.copy(messages = updatedMessages)
                            } else {
                                _selectedConversation.value = current.copy(
                                    messages = (current.messages + newMessage).sortedBy { it.timestamp }
                                )
                            }
                        }
                    }
                }
        }

        // Subscribe to conversation updates (typing status, etc.)
        conversationUpdatesJob?.cancel()
        conversationUpdatesJob = viewModelScope.launch {
            messageRepository.subscribeToConversation(conversationId).collect { updated ->
                val current = _selectedConversation.value
                if (current != null && current.id == conversationId) {
                    _selectedConversation.value = current.copy(
                        isParticipantATyping = updated.isParticipantATyping,
                        isParticipantBTyping = updated.isParticipantBTyping
                    )
                }
            }
        }
    }

    fun stopChatPolling() {
        chatPollingJob?.cancel()
        conversationUpdatesJob?.cancel()
    }

    override fun onCleared() {
        super.onCleared()
        chatPollingJob?.cancel()
        convPollingJob?.cancel()
        typingStatusJob?.cancel()
        conversationUpdatesJob?.cancel()
    }

    fun sendMessage(conversationId: String, senderId: String, senderName: String, content: String) {
        val tempId = "local_${UUID.randomUUID()}"
        val tempMessage = Message(
            id = tempId,
            conversationId = conversationId,
            senderId = senderId,
            senderName = senderName,
            content = content,
            timestamp = System.currentTimeMillis(),
            isRead = true,
            type = MessageType.TEXT
        )
        
        val current = _selectedConversation.value
        if (current != null && current.id == conversationId) {
            _selectedConversation.value = current.copy(
                messages = current.messages + tempMessage,
                lastMessage = content,
                lastMessageTime = tempMessage.timestamp
            )
        }

        viewModelScope.launch {
            messageRepository.sendMessage(
                conversationId = conversationId,
                senderId = senderId,
                senderName = senderName,
                content = content,
                type = MessageType.TEXT
            ).collect { result ->
                result.onSuccess { sentMessage ->
                    val currentConv = _selectedConversation.value
                    if (currentConv != null && currentConv.id == conversationId) {
                        val localIndex = currentConv.messages.indexOfFirst { it.id == tempId }
                        if (localIndex != -1) {
                            val updatedMessages = currentConv.messages.toMutableList()
                            updatedMessages[localIndex] = sentMessage
                            _selectedConversation.value = currentConv.copy(messages = updatedMessages)
                        }
                    }
                }.onFailure { _error.value = it.message }
            }
        }
    }

    fun sendImageMessage(conversationId: String, senderId: String, senderName: String, imageBytes: ByteArray) {
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
                    imageUrl = url
                ).collect { _isLoading.value = false }
            }.onFailure {
                _error.value = "Image upload failed: ${it.message}"
                _isLoading.value = false
            }
        }
    }

    fun sendVoiceMessage(conversationId: String, senderId: String, senderName: String, voiceBytes: ByteArray, duration: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            val path = "chat/$conversationId/${System.currentTimeMillis()}.m4a"
            val uploadResult = SupabaseStorageUpload.uploadFile("chat-media", path, voiceBytes, "audio/m4a")
            
            uploadResult.onSuccess { url ->
                messageRepository.sendMessage(
                    conversationId = conversationId,
                    senderId = senderId,
                    senderName = senderName,
                    content = "[Voice Note]",
                    type = MessageType.VOICE,
                    voiceUrl = url,
                    voiceDuration = duration
                ).collect { _isLoading.value = false }
            }.onFailure {
                _error.value = "Voice upload failed: ${it.message}"
                _isLoading.value = false
            }
        }
    }

    fun updateTypingStatus(conversationId: String, userId: String, isTyping: Boolean) {
        typingStatusJob?.cancel()
        typingStatusJob = viewModelScope.launch {
            // HARDENED: Debounce typing status: send true immediately, auto-false after 3s inactivity
            if (isTyping) {
                messageRepository.setTypingStatus(conversationId, userId, true).collect {}
                delay(3000)
                messageRepository.setTypingStatus(conversationId, userId, false).collect {}
            } else {
                messageRepository.setTypingStatus(conversationId, userId, false).collect {}
            }
        }
    }

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
        teamMaxPlayers: Int
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            messageRepository.sendApplyMessage(
                scrimId, scrimTitle, applicantId, applicantName, applicantTeamId, applicantTeamName,
                scrimCreatorId, scrimCreatorName, scrimCreatorTeamId, scrimCreatorTeamName,
                teamPlayerCount, teamMaxPlayers
            ).collect { result ->
                result.onSuccess { _selectedConversation.value = it }
                _isLoading.value = false
            }
        }
    }

    fun startDirectConversation(
        senderId: String,
        senderName: String,
        recipientId: String,
        recipientName: String
    ) {
        viewModelScope.launch {
            Timber.d("MessageFlow", "VM: startDirectConversation called sender=$senderId recipient=$recipientId")
            _isLoading.value = true
            try {
                withTimeout(10000) {
                    messageRepository.startDirectConversation(
                        senderId, senderName, recipientId, recipientName
                    ).collect { result ->
                        result.onSuccess {
                            Timber.d("MessageFlow", "VM: success convId=${it.id}")
                            // Clear first to guarantee StateFlow emits even if value is equal
                            _selectedConversation.value = null
                            _selectedConversation.value = it
                            // Optionally refresh list
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
            Timber.d("MessageFlow", "VM: isLoading set to false")
        }
    }

    fun setError(message: String) { _error.value = message }

    fun clearError() { _error.value = null }

    fun clearRefreshing() { _isRefreshing.value = false }
}

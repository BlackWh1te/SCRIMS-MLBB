package com.scrimslegends.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.scrimslegends.app.data.model.Conversation
import com.scrimslegends.app.data.model.DeliveryStatus
import com.scrimslegends.app.data.model.Message
import com.scrimslegends.app.data.model.MessageType
import com.scrimslegends.app.data.model.MessageWithDelivery
import com.scrimslegends.app.data.repository.MessageRepositoryInterface
import com.scrimslegends.app.data.service.ChatConnectionState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ChatRoomViewModel @Inject constructor(
    private val messageRepository: MessageRepositoryInterface
) : ViewModel() {

    private var chatSubscriptionJob: Job? = null
    private var conversationUpdatesJob: Job? = null
    private var typingDebounceJob: Job? = null

    private val _connectionState = MutableStateFlow(ChatConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ChatConnectionState> = _connectionState.asStateFlow()

    private val _selectedConversation = MutableStateFlow<Conversation?>(null)
    val selectedConversation: StateFlow<Conversation?> = _selectedConversation.asStateFlow()

    private val _messagesPaged = MutableStateFlow<PagingData<Message>>(PagingData.empty())
    val messagesPaged: StateFlow<PagingData<Message>> = _messagesPaged.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _typingIndicator = MutableStateFlow(false)
    val typingIndicator: StateFlow<Boolean> = _typingIndicator.asStateFlow()

    private val _replyingToMessage = MutableStateFlow<MessageWithDelivery?>(null)
    val replyingToMessage: StateFlow<MessageWithDelivery?> = _replyingToMessage.asStateFlow()

    init {
        viewModelScope.launch {
            messageRepository.observeConnectionState()
                .distinctUntilChanged()
                .collect { _connectionState.value = it }
        }
    }

    fun startChatSubscription(conversationId: String, userId: String) {
        chatSubscriptionJob?.cancel()
        
        chatSubscriptionJob = viewModelScope.launch {
            try {
                _isLoading.value = true
                messageRepository.getMessagesPaged(conversationId)
                    .cachedIn(viewModelScope)
                    .collect { pagingData ->
                        _messagesPaged.value = pagingData
                        _isLoading.value = false
                    }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load paged messages")
                _error.value = "Chat disconnected."
                _isLoading.value = false
            }
        }
        
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

        viewModelScope.launch {
            _isLoading.value = true
            messageRepository.getConversationById(conversationId).collect { result ->
                result.onSuccess { _selectedConversation.value = it }
                    .onFailure { _error.value = it.message ?: "Failed to load conversation" }
                _isLoading.value = false
            }
        }
    }

    fun stopChatSubscription(conversationId: String) {
        chatSubscriptionJob?.cancel()
        conversationUpdatesJob?.cancel()
        typingDebounceJob?.cancel()
        messageRepository.unsubscribeFromMessages(conversationId)
        messageRepository.cleanupConversation(conversationId)
        _selectedConversation.value = null
        _typingIndicator.value = false
        _replyingToMessage.value = null
        _messagesPaged.value = PagingData.empty()
    }

    fun preSelectConversation(conversation: Conversation) {
        _selectedConversation.value = conversation
    }

    fun sendMessage(conversationId: String, content: String) {
        val clientMessageId = "cm_${UUID.randomUUID()}"
        val replyTarget = _replyingToMessage.value
        _replyingToMessage.value = null

        viewModelScope.launch {
            messageRepository.sendMessage(
                conversationId = conversationId,
                content = content,
                type = MessageType.TEXT,
                clientMessageId = clientMessageId,
                replyToId = replyTarget?.message?.id,
                replyToSnippet = replyTarget?.message?.content?.take(80),
                replyToSenderName = replyTarget?.message?.senderName
            ).catch { e ->
                _error.value = e.message ?: "Failed to send message"
            }.collect { delivery ->
                if (delivery.status == DeliveryStatus.FAILED) {
                    _error.value = delivery.errorReason ?: "Failed to send"
                }
            }
        }
    }

    fun retryMessage(clientMessageId: String) {
        viewModelScope.launch {
            messageRepository.retryMessage(clientMessageId).catch { e ->
                _error.value = e.message ?: "Failed to retry message"
            }.collect {}
        }
    }

    fun cancelMessage(clientMessageId: String) {
        viewModelScope.launch {
            messageRepository.cancelMessage(clientMessageId)
        }
    }


    fun updateTypingStatus(conversationId: String, userId: String, isTyping: Boolean) {
        typingDebounceJob?.cancel()
        typingDebounceJob = viewModelScope.launch {
            messageRepository.setTypingStatus(conversationId, userId, isTyping).collect {}
        }
    }

    fun setReplyTarget(message: MessageWithDelivery) {
        _replyingToMessage.value = message
    }

    fun clearReply() {
        _replyingToMessage.value = null
    }

    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            messageRepository.deleteMessage(messageId).onFailure {
                _error.value = it.message ?: "Failed to delete message"
            }
        }
    }

    fun clearChatHistory(conversationId: String) {
        viewModelScope.launch {
            messageRepository.clearChatHistory(conversationId).onFailure {
                _error.value = it.message ?: "Failed to clear history"
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

    fun startDirectConversation(
        senderId: String,
        senderName: String,
        recipientId: String,
        recipientName: String,
        onConversationCreated: (Conversation) -> Unit = {}
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                kotlinx.coroutines.withTimeout(10000) {
                    messageRepository.startDirectConversation(
                        senderId, senderName, recipientId, recipientName
                    ).collect { result ->
                        result.onSuccess {
                            _selectedConversation.value = it
                            onConversationCreated(it)
                        }.onFailure {
                            _error.value = it.message ?: "Failed to start conversation"
                        }
                    }
                }
            } catch (e: Exception) {
                _error.value = "Timeout starting conversation"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun openTeamConversation(
        userId: String,
        userName: String,
        teamId: String,
        teamName: String,
        teamLogoUrl: String?,
        onConversationCreated: (Conversation) -> Unit = {}
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            messageRepository.getOrCreateTeamConversation(
                teamId, teamName, userId, userName
            ).collect { result ->
                result.onSuccess {
                    _selectedConversation.value = it
                    onConversationCreated(it)
                }.onFailure {
                    _error.value = it.message ?: "Failed to start team conversation"
                }
                _isLoading.value = false
            }
        }
    }

    fun clearError() { _error.value = null }

    override fun onCleared() {
        super.onCleared()
        chatSubscriptionJob?.cancel()
        conversationUpdatesJob?.cancel()
        typingDebounceJob?.cancel()
    }
}

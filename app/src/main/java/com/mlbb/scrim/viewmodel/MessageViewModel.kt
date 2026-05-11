package com.mlbb.scrim.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mlbb.scrim.data.model.Conversation
import com.mlbb.scrim.data.model.Message
import com.mlbb.scrim.data.model.MessageType
import com.mlbb.scrim.data.repository.MessageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MessageViewModel : ViewModel() {

    private val messageRepository = MessageRepository()

    private val _conversations = MutableStateFlow<List<Conversation>>(emptyList())
    val conversations: StateFlow<List<Conversation>> = _conversations.asStateFlow()

    private val _selectedConversation = MutableStateFlow<Conversation?>(null)
    val selectedConversation: StateFlow<Conversation?> = _selectedConversation.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _messageSent = MutableStateFlow(false)
    val messageSent: StateFlow<Boolean> = _messageSent.asStateFlow()

    fun loadConversations(userId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            messageRepository.getConversationsForUser(userId).collect { result ->
                result.onSuccess { list ->
                    _conversations.value = list
                    _isLoading.value = false
                }.onFailure { exception ->
                    _error.value = exception.message
                    _isLoading.value = false
                }
            }
        }
    }

    fun loadConversation(conversationId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            messageRepository.getConversationById(conversationId).collect { result ->
                result.onSuccess { conversation ->
                    _selectedConversation.value = conversation
                    _isLoading.value = false
                }.onFailure { exception ->
                    _error.value = exception.message
                    _isLoading.value = false
                }
            }
        }
    }

    fun sendMessage(
        conversationId: String,
        senderId: String,
        senderName: String,
        content: String
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _messageSent.value = false

            messageRepository.sendMessage(
                conversationId = conversationId,
                senderId = senderId,
                senderName = senderName,
                content = content,
                type = MessageType.TEXT
            ).collect { result ->
                result.onSuccess { message ->
                    _messageSent.value = true
                    loadConversation(conversationId)
                    _isLoading.value = false
                }.onFailure { exception ->
                    _error.value = exception.message
                    _isLoading.value = false
                }
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
            _error.value = null

            messageRepository.sendApplyMessage(
                scrimId = scrimId,
                scrimTitle = scrimTitle,
                applicantId = applicantId,
                applicantName = applicantName,
                applicantTeamId = applicantTeamId,
                applicantTeamName = applicantTeamName,
                scrimCreatorId = scrimCreatorId,
                scrimCreatorName = scrimCreatorName,
                scrimCreatorTeamId = scrimCreatorTeamId,
                scrimCreatorTeamName = scrimCreatorTeamName,
                teamPlayerCount = teamPlayerCount,
                teamMaxPlayers = teamMaxPlayers
            ).collect { result ->
                result.onSuccess { conversation ->
                    _selectedConversation.value = conversation
                    loadConversations(scrimCreatorId)
                    _isLoading.value = false
                }.onFailure { exception ->
                    _error.value = exception.message
                    _isLoading.value = false
                }
            }
        }
    }

    fun markAsRead(conversationId: String, userId: String) {
        viewModelScope.launch {
            messageRepository.markConversationAsRead(conversationId, userId).collect { result ->
                result.onSuccess {
                    loadConversations(userId)
                }
            }
        }
    }

    fun clearSelectedConversation() {
        _selectedConversation.value = null
    }

    fun clearError() {
        _error.value = null
    }

    fun clearMessageSent() {
        _messageSent.value = false
    }
}

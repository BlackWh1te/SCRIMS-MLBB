package com.scrimslegends.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scrimslegends.app.data.model.Conversation
import com.scrimslegends.app.data.model.Team
import com.scrimslegends.app.data.repository.MessageRepositoryInterface
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ConversationListViewModel @Inject constructor(
    private val messageRepository: MessageRepositoryInterface
) : ViewModel() {

    private var convPollingJob: Job? = null
    private var loadConversationsJob: Job? = null

    private val _conversations = MutableStateFlow<ImmutableList<Conversation>>(persistentListOf())
    val conversations: StateFlow<ImmutableList<Conversation>> = _conversations.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun loadConversations(userId: String, isRefresh: Boolean = false) {
        loadConversationsJob?.cancel()
        loadConversationsJob = viewModelScope.launch {
            if (isRefresh) _isRefreshing.value = true
            _isLoading.value = true
            messageRepository.getConversationsForUser(userId, forceRefresh = true).collect { result ->
                result.onSuccess { _conversations.value = it.toPersistentList() }
                    .onFailure { _error.value = it.message ?: "Failed to load conversations" }
                _isLoading.value = false
                _isRefreshing.value = false
            }
        }
    }

    // Polling removed. Inbox is refreshed via loadConversations (onResume) and RealtimeManager.


    fun markAsRead(conversationId: String, userId: String) {
        viewModelScope.launch {
            messageRepository.markConversationAsRead(conversationId, userId).collect {}
        }
    }

    fun clearError() { _error.value = null }
    fun clearRefreshing() { _isRefreshing.value = false }

    override fun onCleared() {
        super.onCleared()
    }
}

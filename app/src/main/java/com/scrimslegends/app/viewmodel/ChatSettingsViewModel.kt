package com.scrimslegends.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scrimslegends.app.data.model.BlockStatus
import com.scrimslegends.app.data.repository.MessageRepositoryInterface
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ChatSettingsViewModel @Inject constructor(
    private val messageRepository: MessageRepositoryInterface
) : ViewModel() {

    private val _blockStatus = MutableStateFlow<BlockStatus?>(null)
    val blockStatus: StateFlow<BlockStatus?> = _blockStatus.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun checkBlockStatus(userId1: String, userId2: String) {
        viewModelScope.launch {
            _blockStatus.value = null
            messageRepository.checkBlockStatus(userId1, userId2).onSuccess { status ->
                _blockStatus.value = status
            }.onFailure { err ->
                Timber.e("Failed to check block status: ${err.message}")
            }
        }
    }

    fun blockUser(blockerId: String, blockedId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            messageRepository.blockUser(blockerId, blockedId).onSuccess {
                checkBlockStatus(blockerId, blockedId)
            }.onFailure { err ->
                _error.value = "Failed to block user: ${err.message}"
            }
            _isLoading.value = false
        }
    }

    fun unblockUser(blockerId: String, blockedId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            messageRepository.unblockUser(blockerId, blockedId).onSuccess {
                checkBlockStatus(blockerId, blockedId)
            }.onFailure { err ->
                _error.value = "Failed to unblock user: ${err.message}"
            }
            _isLoading.value = false
        }
    }

    fun clearError() { _error.value = null }
}

package com.mlbb.scrim.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mlbb.scrim.data.model.Notification
import com.mlbb.scrim.data.repository.SupabaseNotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val repository: SupabaseNotificationRepository
) : ViewModel() {
    
    // User ID will be set from AuthViewModel via setUserId
    private var currentUserId: String? = null

    private var loadNotificationsJob: Job? = null
    private var markAsReadJob: Job? = null
    private var markAllAsReadJob: Job? = null
    private var deleteNotificationJob: Job? = null
    private var realtimeJob: Job? = null

    private val _notifications = MutableStateFlow<List<Notification>>(emptyList())
    val notifications: StateFlow<List<Notification>> = _notifications.asStateFlow()

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun setUserId(userId: String) {
        currentUserId = userId
        // Load notifications first, then start Realtime to avoid race where
        // Realtime events are overwritten by the REST response arriving later.
        loadNotifications(onComplete = {
            startRealtimeSubscription()
        })
    }

    /**
     * Subscribe to Realtime notifications for the current user.
     * New notifications appear instantly without polling.
     */
    fun startRealtimeSubscription() {
        val userId = currentUserId ?: return
        realtimeJob?.cancel()
        realtimeJob = viewModelScope.launch {
            repository.subscribeToNotifications(userId).collect { newNotification ->
                val current = _notifications.value.toMutableList()
                // Avoid duplicates
                if (current.none { it.id == newNotification.id }) {
                    current.add(0, newNotification)
                    _notifications.value = current
                    _unreadCount.value = current.count { !it.isRead }
                }
            }
        }
    }

    fun stopRealtimeSubscription() {
        realtimeJob?.cancel()
    }

    fun loadNotifications(onComplete: (() -> Unit)? = null, isRefresh: Boolean = false) {
        val userId = currentUserId ?: return
        loadNotificationsJob?.cancel()
        loadNotificationsJob = viewModelScope.launch {
            if (isRefresh) _isRefreshing.value = true
            _isLoading.value = true
            _error.value = null
            repository.getNotificationsForUser(userId).collect { result ->
                result.onSuccess { list ->
                    _notifications.value = list
                    _unreadCount.value = list.count { !it.isRead }
                    _isLoading.value = false
                    _isRefreshing.value = false
                    onComplete?.invoke()
                }.onFailure { exception ->
                    _error.value = exception.message
                    _isLoading.value = false
                    _isRefreshing.value = false
                }
            }
        }
    }

    fun markAsRead(notificationId: String) {
        markAsReadJob?.cancel()
        markAsReadJob = viewModelScope.launch {
            repository.markAsRead(notificationId).collect { result ->
                result.onSuccess {
                    loadNotifications()
                }.onFailure { exception ->
                    _error.value = exception.message
                }
            }
        }
    }

    fun markAllAsRead() {
        val userId = currentUserId ?: return
        markAllAsReadJob?.cancel()
        markAllAsReadJob = viewModelScope.launch {
            repository.markAllAsRead(userId).collect { result ->
                result.onSuccess {
                    loadNotifications()
                }.onFailure { exception ->
                    _error.value = exception.message
                }
            }
        }
    }

    fun deleteNotification(notificationId: String) {
        deleteNotificationJob?.cancel()
        deleteNotificationJob = viewModelScope.launch {
            repository.deleteNotification(notificationId).collect { result ->
                result.onSuccess {
                    loadNotifications()
                }.onFailure { exception ->
                    _error.value = exception.message
                }
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun clearRefreshing() {
        _isRefreshing.value = false
    }

    override fun onCleared() {
        super.onCleared()
        stopRealtimeSubscription()
    }
}

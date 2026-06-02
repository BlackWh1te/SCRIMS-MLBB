package com.scrimslegends.app.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scrimslegends.app.data.model.Notification
import com.scrimslegends.app.data.model.NotificationType
import com.scrimslegends.app.data.model.isMatchType
import com.scrimslegends.app.data.model.isMessageType
import com.scrimslegends.app.data.preferences.AppSettings
import com.scrimslegends.app.data.repository.SupabaseNotificationRepository
import com.scrimslegends.app.notifications.LocalNotificationHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Manages in-app notifications with settings-aware badge count.
 *
 * unreadCount is filtered by:
 *   notificationsEnabled = false → always 0 (master off)
 *   matchNotifications   = false → exclude match/scrim/tournament types
 *   messageNotifications = false → exclude MESSAGE type
 *
 * TEAM_INVITE and SYSTEM are never suppressed by category settings.
 */
@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val repository: SupabaseNotificationRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val appSettings = AppSettings(context)

    private var currentUserId: String? = null

    private var loadNotificationsJob: Job? = null
    private var markAllAsReadJob: Job? = null
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

    // ── Settings state (observed from DataStore) ──────────────
    private val _notificationsEnabled = MutableStateFlow(true)
    private val _matchNotifications   = MutableStateFlow(true)
    private val _messageNotifications = MutableStateFlow(true)
    private val _soundEnabled         = MutableStateFlow(true)
    private val _vibrationEnabled     = MutableStateFlow(true)

    init {
        observeSettings()
    }

    // ── Settings observation ──────────────────────────────────

    private fun observeSettings() {
        viewModelScope.launch {
            appSettings.notificationsEnabled.collect {
                _notificationsEnabled.value = it
                recomputeUnreadCount()
            }
        }
        viewModelScope.launch {
            appSettings.matchNotifications.collect {
                _matchNotifications.value = it
                recomputeUnreadCount()
            }
        }
        viewModelScope.launch {
            appSettings.messageNotifications.collect {
                _messageNotifications.value = it
                recomputeUnreadCount()
            }
        }
        viewModelScope.launch { appSettings.soundEnabled.collect     { _soundEnabled.value = it } }
        viewModelScope.launch { appSettings.vibrationEnabled.collect { _vibrationEnabled.value = it } }
    }

    /**
     * Recompute the badge count respecting the current notification settings.
     * Rules:
     *  - Master off  → 0
     *  - Match off   → skip SCRIM_*, MATCH_*, TOURNAMENT_*, XP_GAIN, TIER_UP
     *  - Message off → skip MESSAGE
     *  - TEAM_INVITE and SYSTEM always counted
     */
    private fun recomputeUnreadCount() {
        val list = _notifications.value
        if (!_notificationsEnabled.value) {
            _unreadCount.value = 0
            return
        }
        _unreadCount.value = list.count { n ->
            if (n.isRead) return@count false
            when {
                n.type.isMatchType()   && !_matchNotifications.value   -> false
                n.type.isMessageType() && !_messageNotifications.value -> false
                else -> true
            }
        }
    }

    // ── Public API ────────────────────────────────────────────

    fun setUserId(userId: String) {
        currentUserId = userId
        loadNotifications(onComplete = { startRealtimeSubscription() })
    }

    /**
     * Subscribe to Realtime notifications for the current user.
     * Ignores categories suppressed by settings so the badge stays consistent.
     */
    fun startRealtimeSubscription() {
        val userId = currentUserId ?: return
        realtimeJob?.cancel()
        realtimeJob = viewModelScope.launch {
            repository.subscribeToNotifications(userId).collect { newNotification ->
                // Skip if master toggle is off, or if the category is suppressed
                if (!shouldShowNotification(newNotification)) return@collect

                val current = _notifications.value.toMutableList()
                if (current.none { it.id == newNotification.id }) {
                    current.add(0, newNotification)
                    _notifications.value = current
                    recomputeUnreadCount()
                    // Post a heads-up system notification (respects sound/vibration settings)
                    LocalNotificationHelper.show(
                        context          = context,
                        notification     = newNotification,
                        soundEnabled     = _soundEnabled.value,
                        vibrationEnabled = _vibrationEnabled.value
                    )
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
                    recomputeUnreadCount()
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
        // Optimistic UI: mark as read locally immediately
        val current = _notifications.value.toMutableList()
        val index = current.indexOfFirst { it.id == notificationId }
        if (index != -1) {
            current[index] = current[index].copy(isRead = true)
            _notifications.value = current
            recomputeUnreadCount()
        }
        viewModelScope.launch {
            repository.markAsRead(notificationId).collect { result ->
                result.onSuccess {
                    // Full refresh to sync with server state
                    loadNotifications()
                }.onFailure {
                    _error.value = it.message
                    // Revert optimistic update on failure
                    loadNotifications()
                }
            }
        }
    }

    fun markAllAsRead() {
        val userId = currentUserId ?: return
        markAllAsReadJob?.cancel()
        markAllAsReadJob = viewModelScope.launch {
            repository.markAllAsRead(userId).collect { result ->
                result.onSuccess { loadNotifications() }
                    .onFailure { _error.value = it.message }
            }
        }
    }

    fun deleteNotification(notificationId: String) {
        viewModelScope.launch {
            repository.deleteNotification(notificationId).collect { result ->
                result.onSuccess { loadNotifications() }
                    .onFailure { _error.value = it.message }
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

    // ── Private helpers ───────────────────────────────────────

    /**
     * Returns false if settings say this notification should be suppressed.
     * NOTE: suppression here only affects the in-app delivery (badge + list).
     * The row is still stored in the DB; the user can see it if they re-enable
     * the setting.
     */
    private fun shouldShowNotification(n: Notification): Boolean {
        if (!_notificationsEnabled.value) return false
        if (n.type.isMatchType()   && !_matchNotifications.value)   return false
        if (n.type.isMessageType() && !_messageNotifications.value) return false
        return true
    }
}

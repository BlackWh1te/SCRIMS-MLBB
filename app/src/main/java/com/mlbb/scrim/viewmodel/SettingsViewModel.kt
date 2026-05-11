package com.mlbb.scrim.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mlbb.scrim.data.preferences.AppSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val appSettings = AppSettings(application.applicationContext)

    private val _notificationsEnabled = MutableStateFlow(true)
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()

    private val _matchNotifications = MutableStateFlow(true)
    val matchNotifications: StateFlow<Boolean> = _matchNotifications.asStateFlow()

    private val _messageNotifications = MutableStateFlow(true)
    val messageNotifications: StateFlow<Boolean> = _messageNotifications.asStateFlow()

    private val _soundEnabled = MutableStateFlow(true)
    val soundEnabled: StateFlow<Boolean> = _soundEnabled.asStateFlow()

    private val _vibrationEnabled = MutableStateFlow(true)
    val vibrationEnabled: StateFlow<Boolean> = _vibrationEnabled.asStateFlow()

    init {
        viewModelScope.launch {
            appSettings.notificationsEnabled.collect { _notificationsEnabled.value = it }
        }
        viewModelScope.launch {
            appSettings.matchNotifications.collect { _matchNotifications.value = it }
        }
        viewModelScope.launch {
            appSettings.messageNotifications.collect { _messageNotifications.value = it }
        }
        viewModelScope.launch {
            appSettings.soundEnabled.collect { _soundEnabled.value = it }
        }
        viewModelScope.launch {
            appSettings.vibrationEnabled.collect { _vibrationEnabled.value = it }
        }
    }

    fun toggleNotifications(enabled: Boolean) {
        viewModelScope.launch { appSettings.setNotifications(enabled) }
    }

    fun toggleMatchNotifications(enabled: Boolean) {
        viewModelScope.launch { appSettings.setMatchNotifications(enabled) }
    }

    fun toggleMessageNotifications(enabled: Boolean) {
        viewModelScope.launch { appSettings.setMessageNotifications(enabled) }
    }

    fun toggleSound(enabled: Boolean) {
        viewModelScope.launch { appSettings.setSound(enabled) }
    }

    fun toggleVibration(enabled: Boolean) {
        viewModelScope.launch { appSettings.setVibration(enabled) }
    }
}

package com.scrimslegends.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.scrimslegends.app.data.preferences.AppSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val appSettings = AppSettings(application.applicationContext)

    private var toggleNotificationsJob: Job? = null
    private var toggleMatchNotificationsJob: Job? = null
    private var toggleMessageNotificationsJob: Job? = null
    private var toggleSoundJob: Job? = null
    private var toggleVibrationJob: Job? = null
    private var setLanguageJob: Job? = null
    private var toggleDarkModeJob: Job? = null

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

    private val _languageCode = MutableStateFlow("en")
    val languageCode: StateFlow<String> = _languageCode.asStateFlow()

    private val _darkMode = MutableStateFlow(true)
    val darkMode: StateFlow<Boolean> = _darkMode.asStateFlow()

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
        viewModelScope.launch {
            appSettings.languageCode.collect { _languageCode.value = it }
        }
        viewModelScope.launch {
            appSettings.darkMode.collect { _darkMode.value = it }
        }
    }

    fun toggleNotifications(enabled: Boolean) {
        toggleNotificationsJob?.cancel()
        toggleNotificationsJob = viewModelScope.launch { appSettings.setNotifications(enabled) }
    }

    fun toggleMatchNotifications(enabled: Boolean) {
        toggleMatchNotificationsJob?.cancel()
        toggleMatchNotificationsJob = viewModelScope.launch { appSettings.setMatchNotifications(enabled) }
    }

    fun toggleMessageNotifications(enabled: Boolean) {
        toggleMessageNotificationsJob?.cancel()
        toggleMessageNotificationsJob = viewModelScope.launch { appSettings.setMessageNotifications(enabled) }
    }

    fun toggleSound(enabled: Boolean) {
        toggleSoundJob?.cancel()
        toggleSoundJob = viewModelScope.launch { appSettings.setSound(enabled) }
    }

    fun toggleVibration(enabled: Boolean) {
        toggleVibrationJob?.cancel()
        toggleVibrationJob = viewModelScope.launch { appSettings.setVibration(enabled) }
    }

    fun setLanguage(code: String) {
        setLanguageJob?.cancel()
        setLanguageJob = viewModelScope.launch { appSettings.setLanguageCode(code) }
    }

    fun toggleDarkMode(enabled: Boolean) {
        toggleDarkModeJob?.cancel()
        toggleDarkModeJob = viewModelScope.launch { appSettings.setDarkMode(enabled) }
    }
}

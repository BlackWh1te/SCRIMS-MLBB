package com.mlbb.scrim.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

class AppSettings(private val context: Context) {

    private object Keys {
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val MATCH_NOTIFICATIONS = booleanPreferencesKey("match_notifications")
        val MESSAGE_NOTIFICATIONS = booleanPreferencesKey("message_notifications")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
    }

    val notificationsEnabled: Flow<Boolean> = context.settingsDataStore.data.map { it[Keys.NOTIFICATIONS_ENABLED] ?: true }
    val matchNotifications: Flow<Boolean> = context.settingsDataStore.data.map { it[Keys.MATCH_NOTIFICATIONS] ?: true }
    val messageNotifications: Flow<Boolean> = context.settingsDataStore.data.map { it[Keys.MESSAGE_NOTIFICATIONS] ?: true }
    val soundEnabled: Flow<Boolean> = context.settingsDataStore.data.map { it[Keys.SOUND_ENABLED] ?: true }
    val vibrationEnabled: Flow<Boolean> = context.settingsDataStore.data.map { it[Keys.VIBRATION_ENABLED] ?: true }

    suspend fun setNotifications(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun setMatchNotifications(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.MATCH_NOTIFICATIONS] = enabled }
    }

    suspend fun setMessageNotifications(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.MESSAGE_NOTIFICATIONS] = enabled }
    }

    suspend fun setSound(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.SOUND_ENABLED] = enabled }
    }

    suspend fun setVibration(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.VIBRATION_ENABLED] = enabled }
    }
}

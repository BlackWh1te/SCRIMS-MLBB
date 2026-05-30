package com.scrimslegends.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.scrimslegends.app.security.SecurePreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

class AppSettings(private val context: Context) {

    private object Keys {
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val MATCH_NOTIFICATIONS = booleanPreferencesKey("match_notifications")
        val MESSAGE_NOTIFICATIONS = booleanPreferencesKey("message_notifications")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
        val LANGUAGE_CODE = stringPreferencesKey("language_code")
        val DARK_MODE = booleanPreferencesKey("dark_mode")

        // LFG post views tracking (one view per user per post)
        val VIEWED_POSTS = stringPreferencesKey("viewed_posts")
        val PRIVACY_CONSENT_ACCEPTED = booleanPreferencesKey("privacy_consent_accepted")
        val PRIVACY_CONSENT_VERSION = intPreferencesKey("privacy_consent_version")
    }

    val notificationsEnabled: Flow<Boolean> = context.settingsDataStore.data.map { it[Keys.NOTIFICATIONS_ENABLED] ?: true }
    val matchNotifications: Flow<Boolean> = context.settingsDataStore.data.map { it[Keys.MATCH_NOTIFICATIONS] ?: true }
    val messageNotifications: Flow<Boolean> = context.settingsDataStore.data.map { it[Keys.MESSAGE_NOTIFICATIONS] ?: true }
    val soundEnabled: Flow<Boolean> = context.settingsDataStore.data.map { it[Keys.SOUND_ENABLED] ?: true }
    val vibrationEnabled: Flow<Boolean> = context.settingsDataStore.data.map { it[Keys.VIBRATION_ENABLED] ?: true }
    val languageCode: Flow<String> = context.settingsDataStore.data.map { it[Keys.LANGUAGE_CODE] ?: "en" }
    val darkMode: Flow<Boolean> = context.settingsDataStore.data.map { it[Keys.DARK_MODE] ?: true }

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

    suspend fun setLanguageCode(code: String) {
        // Keep the sync value ahead of DataStore emission so Activity recreation
        // reads the new locale in attachBaseContext.
        setLanguageCodeSync(code)
        context.settingsDataStore.edit { it[Keys.LANGUAGE_CODE] = code }
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.DARK_MODE] = enabled }
    }

    // Synchronous methods for critical initialization paths (e.g., attachBaseContext)
    // Uses EncryptedSharedPreferences for defense-in-depth
    private val securePrefs: SecurePreferences by lazy {
        SecurePreferences.getInstance(context)
    }

    fun getLanguageCodeSync(default: String = "en"): String {
        return securePrefs.getString(Keys.LANGUAGE_CODE.name, default) ?: default
    }

    fun setLanguageCodeSync(code: String) {
        securePrefs.putString(Keys.LANGUAGE_CODE.name, code)
    }

    // ── LFG Post View Tracking (one view per user per post) ─────────────────

    suspend fun markPostViewed(userId: String, postId: String) {
        val key = "$userId:$postId"
        context.settingsDataStore.edit { prefs ->
            val current = prefs[Keys.VIEWED_POSTS] ?: ""
            if (!current.contains(key)) {
                prefs[Keys.VIEWED_POSTS] = if (current.isBlank()) key else "$current,$key"
            }
        }
    }

    suspend fun hasViewedPost(userId: String, postId: String): Boolean {
        val key = "$userId:$postId"
        return context.settingsDataStore.data.map { prefs ->
            val current = prefs[Keys.VIEWED_POSTS] ?: ""
            current.contains(key)
        }.first()
    }

    // ── Privacy Consent ──────────────────────────────────────────

    val privacyConsentAccepted: Flow<Boolean> = context.settingsDataStore.data.map {
        it[Keys.PRIVACY_CONSENT_ACCEPTED] == true && (it[Keys.PRIVACY_CONSENT_VERSION] ?: 0) >= CURRENT_PRIVACY_VERSION
    }

    suspend fun acceptPrivacyConsent() {
        context.settingsDataStore.edit {
            it[Keys.PRIVACY_CONSENT_ACCEPTED] = true
            it[Keys.PRIVACY_CONSENT_VERSION] = CURRENT_PRIVACY_VERSION
        }
    }

    companion object {
        private const val CURRENT_PRIVACY_VERSION = 1
    }
}

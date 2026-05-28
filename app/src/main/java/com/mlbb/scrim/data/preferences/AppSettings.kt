package com.mlbb.scrim.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.mlbb.scrim.security.SecurePreferences
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

        // X API v2 Quota Tracking (100 requests/month free tier)
        val X_API_REQUESTS_USED = intPreferencesKey("x_api_requests_used")
        val X_API_MONTH_START = longPreferencesKey("x_api_month_start")
        val X_API_LAST_FETCH = longPreferencesKey("x_api_last_fetch")
        val X_API_LAST_EXPLICIT_REFRESH = longPreferencesKey("x_api_last_explicit_refresh")

        // Drip-feed tracking (+1 article every 2 hours)
        val NEWS_DRIP_INDEX = intPreferencesKey("news_drip_index")
        val NEWS_DRIP_LAST_UPDATE = longPreferencesKey("news_drip_last_update")
        val NEWS_DRIP_COUNT_TOTAL = intPreferencesKey("news_drip_count_total")

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

    // X API v2 Quota Tracking
    val xApiRequestsUsed: Flow<Int> = context.settingsDataStore.data.map { it[Keys.X_API_REQUESTS_USED] ?: 0 }
    val xApiMonthStart: Flow<Long> = context.settingsDataStore.data.map { it[Keys.X_API_MONTH_START] ?: 0L }
    val xApiLastFetch: Flow<Long> = context.settingsDataStore.data.map { it[Keys.X_API_LAST_FETCH] ?: 0L }

    suspend fun incrementXApiRequest() {
        context.settingsDataStore.edit { prefs ->
            val current = prefs[Keys.X_API_REQUESTS_USED] ?: 0
            prefs[Keys.X_API_REQUESTS_USED] = current + 1
        }
    }

    suspend fun resetXApiQuota(monthStartTimestamp: Long) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.X_API_REQUESTS_USED] = 0
            prefs[Keys.X_API_MONTH_START] = monthStartTimestamp
        }
    }

    suspend fun setXApiLastFetch(timestamp: Long) {
        context.settingsDataStore.edit { it[Keys.X_API_LAST_FETCH] = timestamp }
    }

    val xApiLastExplicitRefresh: Flow<Long> = context.settingsDataStore.data.map { it[Keys.X_API_LAST_EXPLICIT_REFRESH] ?: 0L }

    suspend fun setXApiLastExplicitRefresh(timestamp: Long) {
        context.settingsDataStore.edit { it[Keys.X_API_LAST_EXPLICIT_REFRESH] = timestamp }
    }

    // ── Drip-feed Tracking ──────────────────────────────────────────

    /** Current drip offset — how many articles the user has unlocked */
    val newsDripIndex: Flow<Int> = context.settingsDataStore.data.map { it[Keys.NEWS_DRIP_INDEX] ?: 0 }

    /** When the drip index was last incremented */
    val newsDripLastUpdate: Flow<Long> = context.settingsDataStore.data.map { it[Keys.NEWS_DRIP_LAST_UPDATE] ?: 0L }

    /** Total articles in backend archive (for progress bar) */
    val newsDripCountTotal: Flow<Int> = context.settingsDataStore.data.map { it[Keys.NEWS_DRIP_COUNT_TOTAL] ?: 0 }

    suspend fun setNewsDripIndex(index: Int) {
        context.settingsDataStore.edit { it[Keys.NEWS_DRIP_INDEX] = index }
    }

    suspend fun setNewsDripLastUpdate(timestamp: Long) {
        context.settingsDataStore.edit { it[Keys.NEWS_DRIP_LAST_UPDATE] = timestamp }
    }

    suspend fun setNewsDripCountTotal(total: Int) {
        context.settingsDataStore.edit { it[Keys.NEWS_DRIP_COUNT_TOTAL] = total }
    }

    /**
     * Auto-increment drip index based on 2-hour intervals.
     * Call this before every news fetch. Returns how many new articles unlocked.
     */
    suspend fun tickNewsDrip(): Int {
        val now: Long = System.currentTimeMillis()
        val lastUpdate: Long = newsDripLastUpdate.first()
        val currentIndex: Int = newsDripIndex.first()
        val totalAvailable: Int = newsDripCountTotal.first()

        val diff: Long = now - lastUpdate
        val elapsedMs: Double = diff.toDouble()
        val elapsedHours: Double = elapsedMs / (1000.0 * 60.0 * 60.0)
        val ticks: Int = (elapsedHours / 2.0).toInt() // +1 every 2 hours

        val newIndex: Int = (currentIndex + ticks).coerceAtMost(totalAvailable)
        val newlyUnlocked: Int = newIndex - currentIndex

        if (newlyUnlocked > 0) {
            context.settingsDataStore.edit { prefs ->
                prefs[Keys.NEWS_DRIP_INDEX] = newIndex
                prefs[Keys.NEWS_DRIP_LAST_UPDATE] = now
            }
        }
        return newlyUnlocked
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

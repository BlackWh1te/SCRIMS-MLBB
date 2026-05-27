package com.mlbb.scrim

import android.app.Application
import android.os.StrictMode
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.mlbb.scrim.data.service.SupabaseRealtimeClient
import com.mlbb.scrim.data.service.SupabaseSession
import com.mlbb.scrim.security.SecurityUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import timber.log.Timber

import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Main Application class
 * Initializes security checks on app startup and manages Realtime WebSocket lifecycle
 */
@HiltAndroidApp
class MLBBScrimApplication : Application() {

    @Inject
    lateinit var realtimeClient: SupabaseRealtimeClient

    private val appScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onCreate() {
        super.onCreate()

        // Initialize logging: full logs in debug, WARN+ only in release
        if (isDebuggable()) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(ReleaseTree())
        }

        // Initialize Firebase Crashlytics (gracefully skips if google-services.json missing)
        try {
            FirebaseApp.initializeApp(this)
            FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(!isDebuggable())
            Timber.i("Firebase Crashlytics initialized")
        } catch (e: Exception) {
            Timber.w(e, "Firebase not configured (missing google-services.json?)")
        }

        // Run potentially slow initializations (like DB and security checks) in the background
        appScope.launch {
            SupabaseSession.initialize(this@MLBBScrimApplication)

            // Initialize security checks in all builds
            val securityResult = SecurityUtils.initialize(this@MLBBScrimApplication)
            if (securityResult.hasCriticalThreat && !isDebuggable()) {
                Timber.e("Critical security threat detected in production. Consider exiting app.")
                // In production, you may want to exit or show a security warning dialog
            }
        }

        // Enable StrictMode in debug builds for development
        if (isDebuggable()) {
            enableStrictMode()
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        // Clean up Realtime WebSocket connection
        realtimeClient.disconnect()
        appScope.cancel()
    }

    /**
     * Call when user signs in to eagerly connect the Realtime WebSocket.
     * This avoids the delay of lazy connection on first subscription.
     */
    fun onUserSignedIn() {
        Timber.d("User signed in — connecting Realtime eagerly")
        realtimeClient.connect()
    }

    /**
     * Call when user signs out to disconnect Realtime and clear subscriptions.
     */
    fun onUserSignedOut() {
        Timber.d("User signed out — disconnecting Realtime")
        realtimeClient.disconnect()
    }

    /**
     * Release-safe Timber tree: only WARN and ERROR are logged in production.
     * No DEBUG/INFO logs reach crash reporting or logcat in release builds.
     */
    private class ReleaseTree : Timber.Tree() {
        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            if (priority < android.util.Log.WARN) return
            // Strip any potential PII before sending to crash reporter
            val sanitized = sanitizeLogMessage(message)
            if (t != null) {
                android.util.Log.println(priority, tag, sanitized)
                // Future: send to Firebase Crashlytics here
            } else {
                android.util.Log.println(priority, tag, sanitized)
            }
        }

        private fun sanitizeLogMessage(message: String): String {
            return message
                .replace(Regex("(?i)(token|bearer|password|secret|key)=\\S+"), "$1=***REDACTED***")
                .replace(Regex("(?i)(Authorization: )Bearer \\S+"), "$1***REDACTED***")
        }
    }

    private fun isDebuggable(): Boolean {
        return (applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
    }

    private fun enableStrictMode() {
        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build()
        )
        StrictMode.setVmPolicy(
            StrictMode.VmPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build()
        )
    }
}

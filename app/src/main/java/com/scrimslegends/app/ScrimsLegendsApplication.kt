package com.scrimslegends.app

import android.app.Application
import android.os.StrictMode
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import com.scrimslegends.app.data.repository.MessageRepositoryInterface
import com.scrimslegends.app.data.service.SupabaseRealtimeClient
import com.scrimslegends.app.data.service.SupabaseSession
import com.scrimslegends.app.security.SecurityUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import timber.log.Timber

import com.scrimslegends.app.notifications.LocalNotificationHelper
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Main Application class
 * Initializes security checks on app startup and manages Realtime WebSocket lifecycle
 */
@HiltAndroidApp
class ScrimsLegendsApplication : Application() {

    @Inject
    lateinit var realtimeClient: SupabaseRealtimeClient

    @Inject
    lateinit var messageRepository: MessageRepositoryInterface

    private val appScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    override fun onCreate() {
        super.onCreate()

        // Create notification channels (Android O+); safe to call repeatedly
        LocalNotificationHelper.createChannels(this)

        // Initialize logging: full logs in debug, WARN+ only in release
        if (isDebuggable()) {
            Timber.plant(SafeDebugTree())
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
            SupabaseSession.initialize(this@ScrimsLegendsApplication)

            // Initialize security checks in all builds
            val securityResult = SecurityUtils.initialize(this@ScrimsLegendsApplication)
            if (securityResult.hasCriticalThreat && !isDebuggable()) {
                Timber.e("Critical security threat detected in production. Consider exiting app.")
                // In production, you may want to exit or show a security warning dialog
            }
        }

        // Enable StrictMode in debug builds for development
        if (isDebuggable()) {
            enableStrictMode()
        }

        registerNetworkRestoreSync()
    }

    override fun onTerminate() {
        super.onTerminate()
        // Clean up Realtime WebSocket connection
        unregisterNetworkRestoreSync()
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
    private inner class ReleaseTree : Timber.Tree() {
        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            if (priority < android.util.Log.WARN) return
            // Strip any potential PII before sending to crash reporter
            val sanitized = this@ScrimsLegendsApplication.sanitizeLogMessage(message)
            if (t != null) {
                android.util.Log.println(priority, tag, sanitized)
                // Future: send to Firebase Crashlytics here
            } else {
                android.util.Log.println(priority, tag, sanitized)
            }
        }
    }

    private fun sanitizeLogMessage(message: String): String {
        return message
            .replace(Regex("(?i)(Authorization:\\s*Bearer\\s+)\\S+"), "$1***REDACTED***")
            .replace(Regex("(?i)(apikey:\\s*)\\S+"), "$1***REDACTED***")
            .replace(Regex("(?i)(access_token=)[^&\\s]+"), "$1***REDACTED***")
            .replace(Regex("(?i)(refresh_token[\"']?\\s*[:=]\\s*)[\"']?[^\"'\\s,&]+[\"']?"), "$1***REDACTED***")
            .replace(Regex("(?i)(token|bearer|password|secret|key)=\\S+"), "$1=***REDACTED***")
    }

    /**
     * Debug-safe Timber tree that redacts auth tokens and secrets before logging.
     */
    private inner class SafeDebugTree : Timber.DebugTree() {
        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            val sanitized = this@ScrimsLegendsApplication.sanitizeLogMessage(message)
            super.log(priority, tag, sanitized, t)
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

    private fun registerNetworkRestoreSync() {
        val connectivityManager = getSystemService(ConnectivityManager::class.java) ?: return
        if (networkCallback != null) return

        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                appScope.launch {
                    try {
                        val result = messageRepository.syncOutbox()
                        result.onSuccess { count ->
                            if (count > 0) Timber.i("Synced $count queued messages after network restore")
                        }.onFailure { Timber.w(it, "Network restore outbox sync failed") }
                    } catch (e: Exception) {
                        Timber.w(e, "Network restore callback failed")
                    }
                    try {
                        realtimeClient.connect()
                    } catch (e: Exception) {
                        Timber.w(e, "Realtime reconnect after network restore failed")
                    }
                }
            }

            override fun onLost(network: Network) {
                Timber.d("Network lost; messaging will use outbox and polling fallback")
            }
        }

        try {
            connectivityManager.registerDefaultNetworkCallback(networkCallback!!)
        } catch (e: Exception) {
            Timber.w(e, "Failed to register network callback")
            networkCallback = null
        }
    }

    private fun unregisterNetworkRestoreSync() {
        val callback = networkCallback ?: return
        val connectivityManager = getSystemService(ConnectivityManager::class.java) ?: return
        try {
            connectivityManager.unregisterNetworkCallback(callback)
        } catch (_: Exception) {
        } finally {
            networkCallback = null
        }
    }
}

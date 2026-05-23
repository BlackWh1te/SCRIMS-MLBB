package com.mlbb.scrim

import android.app.Application
import android.os.StrictMode
import android.util.Log
import com.mlbb.scrim.data.service.SupabaseRealtimeClient
import com.mlbb.scrim.data.service.SupabaseSession
import com.mlbb.scrim.security.SecurityUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

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

        // Run potentially slow initializations (like DB and security checks) in the background
        appScope.launch {
            SupabaseSession.initialize(this@MLBBScrimApplication)

            // Initialize security checks in all builds
            val securityResult = SecurityUtils.initialize(this@MLBBScrimApplication)
            if (securityResult.hasCriticalThreat && !isDebuggable()) {
                Log.e("Security", "Critical security threat detected in production. Consider exiting app.")
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
        Log.d("MLBBScrimApp", "User signed in — connecting Realtime eagerly")
        realtimeClient.connect()
    }

    /**
     * Call when user signs out to disconnect Realtime and clear subscriptions.
     */
    fun onUserSignedOut() {
        Log.d("MLBBScrimApp", "User signed out — disconnecting Realtime")
        realtimeClient.disconnect()
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

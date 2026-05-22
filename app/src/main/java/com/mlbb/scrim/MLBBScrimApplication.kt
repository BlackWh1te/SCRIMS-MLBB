package com.mlbb.scrim

import android.app.Application
import android.os.StrictMode
import com.mlbb.scrim.data.service.SupabaseSession
import com.mlbb.scrim.security.SecurityUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

import dagger.hilt.android.HiltAndroidApp

/**
 * Main Application class
 * Initializes security checks on app startup
 */
@HiltAndroidApp
class MLBBScrimApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        
        // Run potentially slow initializations (like DB and security checks) in the background
        CoroutineScope(Dispatchers.IO).launch {
            SupabaseSession.initialize(this@MLBBScrimApplication)

            // Initialize security checks in all builds
            val securityResult = SecurityUtils.initialize(this@MLBBScrimApplication)
            if (securityResult.hasCriticalThreat && !isDebuggable()) {
                android.util.Log.e("Security", "Critical security threat detected in production. Consider exiting app.")
                // In production, you may want to exit or show a security warning dialog
            }
        }

        // Enable StrictMode in debug builds for development
        if (isDebuggable()) {
            enableStrictMode()
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

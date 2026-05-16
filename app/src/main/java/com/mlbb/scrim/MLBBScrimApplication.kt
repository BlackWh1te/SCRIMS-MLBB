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

            // Initialize security checks in release builds
            if (!isDebuggable()) {
                try {
                    SecurityUtils.initialize(this@MLBBScrimApplication)
                } catch (e: SecurityException) {
                    // Log security violations and exit app
                    android.util.Log.e("Security", "Security violation detected: ${e.message}")
                    // In production, you might want to exit the app or show a security warning
                    // System.exit(0)
                }
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

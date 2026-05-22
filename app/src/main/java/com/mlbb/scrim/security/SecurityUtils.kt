package com.mlbb.scrim.security

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import java.io.File

/**
 * Security utilities for detecting root, debug, and tampering attempts
 * Protects against Frida, ADB debugging, and reverse engineering
 */
object SecurityUtils {

    private var isSecurityInitialized = false
    private var appSignature: String? = null

    /**
     * Initialize security checks
     * Should be called in Application.onCreate()
     *
     * NOTE: Does NOT throw exceptions. Returns a result so callers can decide
     * whether to proceed, show a warning, or exit gracefully.
     */
    fun initialize(context: Context): SecurityCheckResult {
        if (isSecurityInitialized) {
            return lastCheckResult ?: SecurityCheckResult()
        }

        // Store original app signature for tamper detection
        appSignature = getAppSignature(context)
        isSecurityInitialized = true

        val result = SecurityCheckResult(
            isDebuggable = isDebuggable(context),
            isRooted = isRooted(),
            isDebuggerAttached = isDebuggerAttached(),
            isFridaDetected = isFridaDetected(),
            isXposedDetected = isXposedDetected(),
            isAppTampered = isAppTampered(context),
            isEmulator = isEmulator()
        )
        lastCheckResult = result

        if (result.isDebuggable) {
            android.util.Log.w("Security", "App is running in debug mode")
        }
        if (result.isRooted) {
            android.util.Log.e("Security", "Rooted device detected")
        }

        return result
    }

    data class SecurityCheckResult(
        val isDebuggable: Boolean = false,
        val isRooted: Boolean = false,
        val isDebuggerAttached: Boolean = false,
        val isFridaDetected: Boolean = false,
        val isXposedDetected: Boolean = false,
        val isAppTampered: Boolean = false,
        val isEmulator: Boolean = false
    ) {
        val hasCriticalThreat: Boolean
            get() = isRooted || isDebuggerAttached || isFridaDetected || isXposedDetected || isAppTampered
    }

    @Volatile
    private var lastCheckResult: SecurityCheckResult? = null

    /**
     * Check if device is rooted
     */
    fun isRooted(): Boolean {
        return checkRootMethod1() || checkRootMethod2() || checkRootMethod3()
    }

    private fun checkRootMethod1(): Boolean {
        val paths = arrayOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su",
            "/su/bin/su"
        )

        for (path in paths) {
            if (File(path).exists()) return true
        }
        return false
    }

    private fun checkRootMethod2(): Boolean {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("/system/xbin/which", "su"))
            process.waitFor() == 0
        } catch (e: Exception) {
            false
        }
    }

    private fun checkRootMethod3(): Boolean {
        val rootKeys = arrayOf(
            "test-keys",
            "dev-keys"
        )

        return try {
            val buildTags = Build.TAGS
            rootKeys.any { buildTags != null && buildTags.contains(it) }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Check if app is debuggable
     */
    fun isDebuggable(context: Context): Boolean {
        return (context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
    }

    /**
     * Check if debugger is attached
     */
    fun isDebuggerAttached(): Boolean {
        return android.os.Debug.isDebuggerConnected()
    }

    /**
     * Check if ADB is enabled
     */
    fun isAdbEnabled(context: Context): Boolean {
        return try {
            Settings.Global.getInt(
                context.contentResolver,
                Settings.Global.ADB_ENABLED,
                0
            ) == 1
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Check for emulator
     */
    fun isEmulator(): Boolean {
        return (Build.FINGERPRINT.startsWith("generic") ||
                Build.FINGERPRINT.startsWith("unknown") ||
                Build.MODEL.contains("google_sdk") ||
                Build.MODEL.contains("Emulator") ||
                Build.MODEL.contains("Android SDK built for x86") ||
                Build.MANUFACTURER.contains("Genymotion") ||
                (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic")) ||
                "google_sdk" == Build.PRODUCT)
    }

    /**
     * Check for Frida or other hooking frameworks
     */
    fun isFridaDetected(): Boolean {
        return checkFridaFiles() || checkFridaPorts() || checkFridaThreads()
    }

    private fun checkFridaFiles(): Boolean {
        val fridaFiles = arrayOf(
            "/data/local/tmp/frida-server",
            "/data/local/tmp/frida",
            "/data/local/tmp/re.frida.server",
            "/system/lib/libfrida.so",
            "/system/lib64/libfrida.so"
        )

        for (file in fridaFiles) {
            if (File(file).exists()) return true
        }
        return false
    }

    private fun checkFridaPorts(): Boolean {
        val fridaPorts = listOf(27042, 27043, 27047) // Common Frida ports

        return try {
            for (port in fridaPorts) {
                val socket = java.net.Socket("127.0.0.1", port)
                socket.close()
                return true
            }
            false
        } catch (e: Exception) {
            false
        }
    }

    private fun checkFridaThreads(): Boolean {
        return try {
            val threads = Thread.getAllStackTraces().keys
            threads.any { it.name.contains("frida") || it.name.contains("gum") }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Check for Xposed framework
     */
    fun isXposedDetected(): Boolean {
        return try {
            Class.forName("de.robv.android.xposed.XC_MethodHook")
            true
        } catch (e: ClassNotFoundException) {
            false
        }
    }

    /**
     * Get app signature for tamper detection
     */
    private fun getAppSignature(context: Context): String? {
        return try {
            @Suppress("DEPRECATION")
            val packageInfo = context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.GET_SIGNATURES
            )
            @Suppress("DEPRECATION")
            packageInfo.signatures?.firstOrNull()?.toCharsString()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Check if app signature has been tampered with
     */
    fun isAppTampered(context: Context): Boolean {
        if (!isSecurityInitialized || appSignature == null) return false

        val currentSignature = getAppSignature(context)
        return currentSignature != appSignature
    }

    /**
     * Perform comprehensive security check
     * Returns true if device is secure, false if security threats detected
     */
    fun performSecurityCheck(context: Context): Boolean {
        if (!isSecurityInitialized) {
            initialize(context)
        }

        return when {
            isRooted() -> {
                android.util.Log.e("Security", "Rooted device detected")
                false
            }
            isDebuggerAttached() -> {
                android.util.Log.e("Security", "Debugger attached")
                false
            }
            isFridaDetected() -> {
                android.util.Log.e("Security", "Frida detected")
                false
            }
            isXposedDetected() -> {
                android.util.Log.e("Security", "Xposed framework detected")
                false
            }
            isAppTampered(context) -> {
                android.util.Log.e("Security", "App tampering detected")
                false
            }
            else -> true
        }
    }

    /**
     * Check if device is secure for production use
     * Less strict than performSecurityCheck, allows emulators for testing
     */
    fun isDeviceSecureForProduction(context: Context): Boolean {
        if (!isSecurityInitialized) {
            initialize(context)
        }

        return when {
            isRooted() -> false
            isDebuggerAttached() -> false
            isFridaDetected() -> false
            isAppTampered(context) -> false
            else -> true
        }
    }
}

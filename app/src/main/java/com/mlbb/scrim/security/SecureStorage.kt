package com.mlbb.scrim.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import java.security.SecureRandom
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Secure storage for sensitive data using AES encryption
 * Provides encrypted storage for tokens, passwords, and other sensitive information
 */
class SecureStorage(context: Context) {

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("secure_storage", Context.MODE_PRIVATE)

    private val keyAlias = "mlbb_scrim_secure_key"
    private val encryptionKey: SecretKey by lazy {
        getOrCreateEncryptionKey()
    }

    /**
     * Get or create encryption key
     * In production, use Android Keystore for better security
     */
    private fun getOrCreateEncryptionKey(): SecretKey {
        val existingKey = sharedPreferences.getString(keyAlias, null)
        if (existingKey != null) {
            val keyBytes = Base64.decode(existingKey, Base64.DEFAULT)
            return SecretKeySpec(keyBytes, "AES")
        }

        // Generate new key
        val keyGenerator = KeyGenerator.getInstance("AES")
        keyGenerator.init(256)
        val key = keyGenerator.generateKey()

        // Store key (in production, use Android Keystore instead)
        val encodedKey = Base64.encodeToString(key.encoded, Base64.DEFAULT)
        sharedPreferences.edit().putString(keyAlias, encodedKey).apply()

        return key
    }

    /**
     * Encrypt data
     */
    fun encrypt(data: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, encryptionKey)

        val iv = cipher.iv
        val encryptedData = cipher.doFinal(data.toByteArray(Charsets.UTF_8))

        // Combine IV and encrypted data
        val combined = iv + encryptedData
        return Base64.encodeToString(combined, Base64.DEFAULT)
    }

    /**
     * Decrypt data
     */
    fun decrypt(encryptedData: String): String {
        try {
            val combined = Base64.decode(encryptedData, Base64.DEFAULT)

            // Extract IV (first 12 bytes for GCM)
            val iv = combined.copyOfRange(0, 12)
            val data = combined.copyOfRange(12, combined.size)

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val gcmSpec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, encryptionKey, gcmSpec)

            val decryptedData = cipher.doFinal(data)
            return String(decryptedData, Charsets.UTF_8)
        } catch (e: Exception) {
            throw SecurityException("Decryption failed", e)
        }
    }

    /**
     * Store encrypted string
     */
    fun storeEncrypted(key: String, value: String) {
        val encrypted = encrypt(value)
        sharedPreferences.edit().putString(key, encrypted).apply()
    }

    /**
     * Retrieve and decrypt string
     */
    fun getEncrypted(key: String, defaultValue: String = ""): String {
        val encrypted = sharedPreferences.getString(key, null) ?: return defaultValue
        return try {
            decrypt(encrypted)
        } catch (e: Exception) {
            defaultValue
        }
    }

    /**
     * Remove stored value
     */
    fun remove(key: String) {
        sharedPreferences.edit().remove(key).apply()
    }

    /**
     * Clear all stored data
     */
    fun clear() {
        sharedPreferences.edit().clear().apply()
    }

    /**
     * Generate secure random string
     */
    fun generateSecureToken(length: Int = 32): String {
        val random = SecureRandom()
        val bytes = ByteArray(length)
        random.nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.URL_SAFE).trimEnd('=')
    }

    companion object {
        @Volatile
        private var instance: SecureStorage? = null

        fun getInstance(context: Context): SecureStorage {
            return instance ?: synchronized(this) {
                instance ?: SecureStorage(context.applicationContext).also { instance = it }
            }
        }
    }
}

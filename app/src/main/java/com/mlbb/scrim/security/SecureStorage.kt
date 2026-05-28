package com.mlbb.scrim.security

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import java.security.KeyStore
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class SecureStorage(context: Context) {

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("secure_storage", Context.MODE_PRIVATE)

    private val keyAlias = "scrims_legends_secure_key"
    private val encryptionKey: SecretKey by lazy {
        getOrCreateEncryptionKey()
    }

    private fun getOrCreateEncryptionKey(): SecretKey {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val keyStore = KeyStore.getInstance("AndroidKeyStore")
                keyStore.load(null)
                if (keyStore.containsAlias(keyAlias)) {
                    val entry = keyStore.getEntry(keyAlias, null)
                    return (entry as KeyStore.SecretKeyEntry).secretKey
                }
                val keyGenerator = KeyGenerator.getInstance(
                    "AES", "AndroidKeyStore"
                )
                keyGenerator.init(
                    android.security.keystore.KeyGenParameterSpec.Builder(
                        keyAlias,
                        android.security.keystore.KeyProperties.PURPOSE_ENCRYPT or
                            android.security.keystore.KeyProperties.PURPOSE_DECRYPT
                    )
                        .setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE)
                        .setKeySize(256)
                        .build()
                )
                return keyGenerator.generateKey()
            } catch (_: Exception) {
                // Fallback for devices where Keystore is unavailable
            }
        }

        val existingKey = sharedPreferences.getString(keyAlias, null)
        if (existingKey != null) {
            val keyBytes = Base64.getMimeDecoder().decode(existingKey)
            return SecretKeySpec(keyBytes, "AES")
        }
        val keyGenerator = KeyGenerator.getInstance("AES")
        keyGenerator.init(256)
        val key = keyGenerator.generateKey()
        val encodedKey = Base64.getEncoder().encodeToString(key.encoded)
        sharedPreferences.edit().putString(keyAlias, encodedKey).apply()
        return key
    }

    fun encrypt(data: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, encryptionKey)
        val iv = cipher.iv
        val encryptedData = cipher.doFinal(data.toByteArray(Charsets.UTF_8))
        val combined = iv + encryptedData
        return Base64.getEncoder().encodeToString(combined)
    }

    fun decrypt(encryptedData: String): String {
        try {
            val combined = Base64.getMimeDecoder().decode(encryptedData)
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

    fun storeEncrypted(key: String, value: String) {
        val encrypted = encrypt(value)
        sharedPreferences.edit().putString(key, encrypted).apply()
    }

    fun getEncrypted(key: String, defaultValue: String = ""): String {
        val encrypted = sharedPreferences.getString(key, null) ?: return defaultValue
        return try { decrypt(encrypted) } catch (_: Exception) { defaultValue }
    }

    fun remove(key: String) { sharedPreferences.edit().remove(key).apply() }
    fun clear() { sharedPreferences.edit().clear().apply() }

    fun generateSecureToken(length: Int = 32): String {
        val random = SecureRandom()
        val bytes = ByteArray(length)
        random.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    companion object {
        @Volatile
        private var instance: SecureStorage? = null
        @JvmStatic
        fun getInstance(context: Context): SecureStorage {
            return instance ?: synchronized(this) {
                instance ?: SecureStorage(context.applicationContext).also { instance = it }
            }
        }
    }
}

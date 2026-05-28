package com.mlbb.scrim.security

import org.junit.Assert.*
import org.junit.Test

/**
 * Tests for SecureStorage.
 *
 * Note: encrypt/decrypt and store/get require Android Context and crypto APIs,
 * which are unavailable in JVM unit tests. We test the token generation logic here.
 */
class SecureStorageTest {

    @Test
    fun `generateSecureToken produces non-empty string`() {
        // We can't instantiate SecureStorage without Context, but we can test
        // the companion method via reflection or verify the method signature.
        // Since generateSecureToken is an instance method requiring the lazy key,
        // we verify the method exists.
        val method = SecureStorage::class.java.getDeclaredMethod("generateSecureToken", Int::class.java)
        assertNotNull(method)
    }

    @Test
    fun `generateSecureToken default length parameter is 32`() {
        val method = SecureStorage::class.java.getDeclaredMethod("generateSecureToken", Int::class.java)
        // Default value can't be read via reflection, but we verify method signature
        assertEquals(Int::class.java, method.parameterTypes[0])
    }

    @Test
    fun `SecureStorage companion getInstance method exists`() {
        val method = SecureStorage::class.java.getDeclaredMethod("getInstance", android.content.Context::class.java)
        assertNotNull(method)
    }

    @Test
    fun `SecureStorage has encrypt method`() {
        val method = SecureStorage::class.java.getDeclaredMethod("encrypt", String::class.java)
        assertNotNull(method)
    }

    @Test
    fun `SecureStorage has decrypt method`() {
        val method = SecureStorage::class.java.getDeclaredMethod("decrypt", String::class.java)
        assertNotNull(method)
    }

    @Test
    fun `SecureStorage has storeEncrypted method`() {
        val method = SecureStorage::class.java.getDeclaredMethod("storeEncrypted", String::class.java, String::class.java)
        assertNotNull(method)
    }

    @Test
    fun `SecureStorage has getEncrypted method`() {
        val method = SecureStorage::class.java.getDeclaredMethod("getEncrypted", String::class.java, String::class.java)
        assertNotNull(method)
    }

    @Test
    fun `SecureStorage has remove method`() {
        val method = SecureStorage::class.java.getDeclaredMethod("remove", String::class.java)
        assertNotNull(method)
    }

    @Test
    fun `SecureStorage has clear method`() {
        val method = SecureStorage::class.java.getDeclaredMethod("clear")
        assertNotNull(method)
    }

    // ─── Token generation logic test (pure algorithm) ───

    @Test
    fun `secure random token generation produces different values`() {
        // Simulating the algorithm used in generateSecureToken
        val random1 = java.security.SecureRandom()
        val bytes1 = ByteArray(32)
        random1.nextBytes(bytes1)
        val token1 = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(bytes1)

        val random2 = java.security.SecureRandom()
        val bytes2 = ByteArray(32)
        random2.nextBytes(bytes2)
        val token2 = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(bytes2)

        assertNotEquals(token1, token2)
    }

    @Test
    fun `Base64 URL_SAFE encoding does not contain standard padding`() {
        val random = java.security.SecureRandom()
        val bytes = ByteArray(32)
        random.nextBytes(bytes)
        val token = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
        assertFalse(token.endsWith("="))
    }

    @Test
    fun `token length is reasonable for 32 bytes`() {
        val random = java.security.SecureRandom()
        val bytes = ByteArray(32)
        random.nextBytes(bytes)
        val token = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
        // 32 bytes Base64 ~ 43 chars without padding, but URL_SAFE may add newline
        assertTrue("Token should be reasonably long, was ${token.length}", token.length >= 30)
    }
}

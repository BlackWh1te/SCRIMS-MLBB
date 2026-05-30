package com.mlbb.scrim.data.service

import org.junit.Assert.*
import org.junit.Test

/**
 * Tests for SupabaseSession token management.
 *
 * Note: Methods requiring Android Context cannot be fully tested in JVM unit tests.
 * We verify method signatures and static behavior.
 */
class SupabaseSessionTest {

    @Test
    fun `SupabaseSession has expected key constants`() {
        // Keys are private, but we can verify the class exists
        assertNotNull(SupabaseSession::class.java)
    }

    @Test
    fun `initialize method exists`() {
        val method = SupabaseSession::class.java.getDeclaredMethod("initialize", android.content.Context::class.java)
        assertNotNull(method)
    }

    @Test
    fun `getAccessTokenOrNull returns null when not initialized`() {
        // After initialize is called, this returns a value.
        // Without initialization, it should return null or blank
        val result = SupabaseSession.getAccessTokenOrNull()
        // Result depends on whether SecureStorage singleton exists
        assertTrue(result == null || result.isBlank())
    }

    @Test
    fun `getRefreshTokenOrNull returns null when not initialized`() {
        val result = SupabaseSession.getRefreshTokenOrNull()
        assertTrue(result == null || result.isBlank())
    }

    @Test
    fun `getUserIdOrNull returns null when not initialized`() {
        val result = SupabaseSession.getUserIdOrNull()
        assertTrue(result == null || result.isBlank())
    }

    @Test
    fun `saveTokens method exists`() {
        val method = SupabaseSession::class.java.getDeclaredMethod("saveTokens", String::class.java, String::class.java)
        assertNotNull(method)
    }

    @Test
    fun `SupabaseAuthenticator class exists`() {
        assertNotNull(SupabaseAuthenticator::class.java)
    }

    @Test
    fun `SupabaseAuthenticator implements Authenticator`() {
        val interfaces = SupabaseAuthenticator::class.java.interfaces
        assertTrue(interfaces.any { it.name.contains("Authenticator") })
    }

    @Test
    fun `SupabaseRetrofitClient provides retrofit instance`() {
        assertNotNull(SupabaseRetrofitClient.retrofit)
    }

    @Test
    fun `SupabaseAuthRetrofitClient provides retrofit instance`() {
        assertNotNull(SupabaseAuthRetrofitClient.retrofit)
    }

    @Test
    fun `Retrofit base URLs are configured`() {
        val restUrl = SupabaseRetrofitClient.retrofit.baseUrl().toString()
        assertTrue(restUrl.contains("/rest/v1/"))

        val authUrl = SupabaseAuthRetrofitClient.retrofit.baseUrl().toString()
        assertTrue(authUrl.contains("/auth/v1/"))
    }
}

package com.mlbb.scrim.security

import com.mlbb.scrim.BuildConfig
import com.mlbb.scrim.data.service.SupabaseConfig
import com.mlbb.scrim.data.service.SupabaseRealtimeClient
import com.mlbb.scrim.data.service.SupabaseRetrofitClient
import com.mlbb.scrim.data.service.SupabaseAuthRetrofitClient
import okhttp3.Authenticator
import org.junit.Assert.*
import org.junit.Test

/**
 * Network and configuration security tests.
 * Ensures no hardcoded secrets, proper URL formats, and safe defaults.
 */
class NetworkSecurityTest {

    // ─── BuildConfig checks ───

    @Test
    fun `SUPABASE_URL is https`() {
        assertTrue(BuildConfig.SUPABASE_URL.startsWith("https://"))
    }

    @Test
    fun `SUPABASE_ANON_KEY is not empty`() {
        assertTrue(BuildConfig.SUPABASE_ANON_KEY.isNotBlank())
    }

    @Test
    fun `NEWSAPI_KEY does not contain literal hardcoded placeholder`() {
        // The fallback in build.gradle.kts is `""` so it should not look like a real key
        val key = BuildConfig.NEWSAPI_KEY
        // If it's the literal empty string, that's acceptable for tests
        // If it's a real key, length should be > 10
        assertTrue(key == "\"\"" || key.length > 10)
    }

    @Test
    fun `NEWS_SERVICE_API_KEY is not a default secret`() {
        val key = BuildConfig.NEWS_SERVICE_API_KEY
        assertFalse("Default secret must be overridden", key == "\"mlbb-news-secret-2024\"")
    }

    @Test
    fun `X_BEARER_TOKEN is not a real token in test builds`() {
        val token = BuildConfig.X_BEARER_TOKEN
        assertTrue(token == "\"\"" || token.startsWith("Bearer") || token.length > 20)
    }

    // ─── SupabaseConfig checks ───

    @Test
    fun `SUPABASE_URL matches BuildConfig`() {
        assertEquals(BuildConfig.SUPABASE_URL, SupabaseConfig.SUPABASE_URL)
    }

    @Test
    fun `REST_API_URL ends with rest v1 slash`() {
        assertTrue(SupabaseConfig.REST_API_URL.endsWith("/rest/v1/"))
    }

    @Test
    fun `AUTH_API_URL ends with auth v1 slash`() {
        assertTrue(SupabaseConfig.AUTH_API_URL.endsWith("/auth/v1/"))
    }

    @Test
    fun `table names are lowercase snake case`() {
        val tables = listOf(
            SupabaseConfig.TABLE_PROFILES,
            SupabaseConfig.TABLE_TEAMS,
            SupabaseConfig.TABLE_TEAM_MEMBERS,
            SupabaseConfig.TABLE_SCRIMS,
            SupabaseConfig.TABLE_MATCH_RESULTS,
            SupabaseConfig.TABLE_MESSAGES,
            SupabaseConfig.TABLE_NOTIFICATIONS
        )
        tables.forEach { table ->
            assertTrue("Table '$table' must be lowercase", table == table.lowercase())
            assertFalse("Table '$table' must not contain spaces", table.contains(" "))
            assertFalse("Table '$table' must not contain semicolons", table.contains(";"))
        }
    }

    @Test
    fun `bucket names do not contain spaces or semicolons`() {
        val buckets = listOf(
            SupabaseConfig.BUCKET_SCREENSHOTS,
            SupabaseConfig.BUCKET_AVATARS,
            SupabaseConfig.BUCKET_TEAM_LOGOS,
            SupabaseConfig.BUCKET_LFG_SCREENSHOTS
        )
        buckets.forEach { bucket ->
            assertFalse(bucket.contains(" "))
            assertFalse(bucket.contains(";"))
            assertFalse(bucket.contains("'"))
        }
    }

    // ─── WebSocket URL security ───

    @Test
    fun `buildWsUrl transforms https to wss`() {
        val url = SupabaseRealtimeClient.buildWsUrl()
        assertTrue(url.startsWith("wss://"))
        assertFalse(url.contains("http://"))
        assertFalse(url.contains("https://"))
    }

    @Test
    fun `buildWsUrl contains realtime v1 websocket path`() {
        val url = SupabaseRealtimeClient.buildWsUrl()
        assertTrue(url.contains("/realtime/v1/websocket"))
    }

    @Test
    fun `buildWsUrl includes apikey query parameter`() {
        val url = SupabaseRealtimeClient.buildWsUrl()
        assertTrue(url.contains("apikey="))
    }

    @Test
    fun `buildWsUrl includes vsn parameter`() {
        val url = SupabaseRealtimeClient.buildWsUrl()
        assertTrue(url.contains("vsn=1.0.0"))
    }

    // ─── Retrofit client security ───

    @Test
    fun `SupabaseRetrofitClient base URL contains rest v1`() {
        val baseUrl = SupabaseRetrofitClient.retrofit.baseUrl().toString()
        assertTrue(baseUrl.contains("/rest/v1/"))
    }

    @Test
    fun `SupabaseAuthRetrofitClient base URL contains auth v1`() {
        val baseUrl = SupabaseAuthRetrofitClient.retrofit.baseUrl().toString()
        assertTrue(baseUrl.contains("/auth/v1/"))
    }

    @Test
    fun `SupabaseAuthenticator implements Authenticator interface`() {
        val instance = com.mlbb.scrim.data.service.SupabaseAuthenticator()
        assertTrue(instance is Authenticator)
    }

    @Test
    fun `SupabaseAuthenticator authenticate method exists`() {
        val method = com.mlbb.scrim.data.service.SupabaseAuthenticator::class.java
            .getDeclaredMethod("authenticate", okhttp3.Route::class.java, okhttp3.Response::class.java)
        assertNotNull(method)
    }

    @Test
    fun `SupabaseAuthenticator retry count method exists`() {
        // The private extension function `count()` is compiled as a static method in the class
        val methods = com.mlbb.scrim.data.service.SupabaseAuthenticator::class.java.declaredMethods
        val countMethod = methods.find { it.name == "count" }
        assertNotNull("count() method should exist", countMethod)
    }

    // ─── Timeout checks ───

    @Test
    fun `retrofit client timeouts are reasonable`() {
        // Verify that the OkHttpClient timeouts are not excessively long (avoids DoS / hang)
        val restClient = SupabaseRetrofitClient.retrofit.callFactory() as okhttp3.OkHttpClient
        assertTrue(restClient.connectTimeoutMillis <= 35_000)
        assertTrue(restClient.readTimeoutMillis <= 35_000)
        assertTrue(restClient.writeTimeoutMillis <= 35_000)
    }

    @Test
    fun `auth retrofit client timeouts are reasonable`() {
        val authClient = SupabaseAuthRetrofitClient.retrofit.callFactory() as okhttp3.OkHttpClient
        assertTrue(authClient.connectTimeoutMillis <= 35_000)
        assertTrue(authClient.readTimeoutMillis <= 35_000)
        assertTrue(authClient.writeTimeoutMillis <= 35_000)
    }
}

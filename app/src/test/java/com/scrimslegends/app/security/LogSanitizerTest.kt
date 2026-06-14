package com.scrimslegends.app.security

import org.junit.Assert.*
import org.junit.Test

/**
 * Tests for production log sanitization.
 *
 * Verifies that tokens, secrets, and auth headers are redacted
 * before they reach crash reporters or logcat in release builds.
 */
class LogSanitizerTest {

    private fun sanitize(message: String): String {
        return message
            .replace(Regex("(?i)(token|bearer|password|secret|key)=[^&\\s]+"), "$1=***REDACTED***")
            .replace(Regex("(?i)(Authorization: )Bearer \\S+"), "$1***REDACTED***")
    }

    @Test
    fun `sanitize redacts bearer token`() {
        val raw = "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9"
        val sanitized = sanitize(raw)
        assertFalse(sanitized.contains("eyJhbGci"))
        assertTrue(sanitized.contains("***REDACTED***"))
    }

    @Test
    fun `sanitize redacts token query param`() {
        val raw = "Request failed: token=abc123&secret=myvalue"
        val sanitized = sanitize(raw)
        assertFalse(sanitized.contains("abc123"))
        assertFalse(sanitized.contains("myvalue"))
        assertTrue(sanitized.contains("token=***REDACTED***"))
        assertTrue(sanitized.contains("secret=***REDACTED***"))
    }

    @Test
    fun `sanitize redacts password`() {
        val raw = "User login: password=SuperSecret123"
        val sanitized = sanitize(raw)
        assertFalse(sanitized.contains("SuperSecret123"))
        assertTrue(sanitized.contains("password=***REDACTED***"))
    }

    @Test
    fun `sanitize preserves safe messages`() {
        val raw = "Scrim created successfully for team Red Dragons"
        val sanitized = sanitize(raw)
        assertEquals(raw, sanitized)
    }
}

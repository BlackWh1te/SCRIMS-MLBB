package com.scrimslegends.app.util

import org.junit.Assert.*
import org.junit.Test

class DateUtilsTest {

    @Test
    fun `parseIsoToMillis parses Zulu timestamp`() {
        val result = DateUtils.parseIsoToMillis("2024-01-15T10:30:00Z", fallback = 0L)
        assertTrue("Expected positive millis for valid ISO date", result > 0L)
    }

    @Test
    fun `parseIsoToMillis parses XXX offset timestamp`() {
        val result = DateUtils.parseIsoToMillis("2024-01-15T10:30:00.000+00:00", fallback = 0L)
        assertTrue("Expected positive millis for ISO with offset", result > 0L)
    }

    @Test
    fun `parseIsoToMillis returns fallback for null`() {
        val fallback = 12345L
        assertEquals(fallback, DateUtils.parseIsoToMillis(null, fallback))
    }

    @Test
    fun `parseIsoToMillis returns fallback for blank`() {
        val fallback = 12345L
        assertEquals(fallback, DateUtils.parseIsoToMillis("   ", fallback))
    }

    @Test
    fun `parseIsoToMillis returns fallback for garbage string`() {
        val fallback = 99999L
        assertEquals(fallback, DateUtils.parseIsoToMillis("not-a-date", fallback))
    }

    @Test
    fun `formatIsoNow returns non-empty string`() {
        val iso = DateUtils.formatIsoNow()
        assertTrue("ISO string should not be empty", iso.isNotEmpty())
        assertTrue("ISO string should end with Z", iso.endsWith("Z"))
    }

    @Test
    fun `formatIsoUtc round-trips correctly`() {
        val now = System.currentTimeMillis()
        val iso = DateUtils.formatIsoUtc(now)
        val parsed = DateUtils.parseIsoToMillis(iso, fallback = 0L)
        // Allow 1 second tolerance due to second-level truncation
        assertTrue("Round-trip diff should be < 2000ms", kotlin.math.abs(now - parsed) < 2000)
    }
}

package com.scrimslegends.app.util

import org.junit.Assert.*
import org.junit.Test

class DateUtilsExtendedTest {

    @Test
    fun `parseIsoToMillis handles null input`() {
        val fallback = 12345L
        assertEquals(fallback, DateUtils.parseIsoToMillis(null, fallback))
    }

    @Test
    fun `parseIsoToMillis handles empty string`() {
        val fallback = 12345L
        assertEquals(fallback, DateUtils.parseIsoToMillis("", fallback))
    }

    @Test
    fun `parseIsoToMillis handles blank string`() {
        val fallback = 12345L
        assertEquals(fallback, DateUtils.parseIsoToMillis("   ", fallback))
    }

    @Test
    fun `parseIsoToMillis handles garbage string`() {
        val fallback = 99999L
        assertEquals(fallback, DateUtils.parseIsoToMillis("not-a-date-at-all", fallback))
    }

    @Test
    fun `parseIsoToMillis handles ISO with Z`() {
        val result = DateUtils.parseIsoToMillis("2024-01-15T10:30:00Z", fallback = 0L)
        assertTrue(result > 0L)
    }

    @Test
    fun `parseIsoToMillis handles ISO with milliseconds and Z`() {
        val result = DateUtils.parseIsoToMillis("2024-01-15T10:30:00.123Z", fallback = 0L)
        assertTrue(result > 0L)
    }

    @Test
    fun `parseIsoToMillis handles ISO with timezone offset`() {
        val result = DateUtils.parseIsoToMillis("2024-01-15T10:30:00.000+00:00", fallback = 0L)
        assertTrue(result > 0L)
    }

    @Test
    fun `parseIsoToMillis handles ISO without timezone`() {
        val result = DateUtils.parseIsoToMillis("2024-01-15T10:30:00", fallback = 0L)
        assertTrue(result > 0L)
    }

    @Test
    fun `parseIsoToMillis handles very old date`() {
        val result = DateUtils.parseIsoToMillis("1970-01-01T00:00:00Z", fallback = 0L)
        assertTrue(result >= 0L)
    }

    @Test
    fun `parseIsoToMillis handles future date`() {
        val result = DateUtils.parseIsoToMillis("2099-12-31T23:59:59Z", fallback = 0L)
        assertTrue(result > System.currentTimeMillis())
    }

    @Test
    fun `formatIsoUtc produces Z-suffixed string`() {
        val now = System.currentTimeMillis()
        val iso = DateUtils.formatIsoUtc(now)
        assertTrue(iso.endsWith("Z"))
        assertTrue(iso.contains("T"))
    }

    @Test
    fun `formatIsoUtcWithMs includes milliseconds`() {
        val now = System.currentTimeMillis()
        val iso = DateUtils.formatIsoUtcWithMs(now)
        assertTrue(iso.endsWith("Z"))
        assertTrue(iso.contains("."))
    }

    @Test
    fun `formatDate produces yyyy-MM-dd pattern`() {
        val now = System.currentTimeMillis()
        val date = DateUtils.formatDate(now)
        assertEquals(10, date.length)
        assertTrue(date.contains("-"))
    }

    @Test
    fun `formatTime produces HH mm ss pattern`() {
        val now = System.currentTimeMillis()
        val time = DateUtils.formatTime(now)
        assertEquals(8, time.length)
        assertTrue(time.contains(":"))
    }

    @Test
    fun `formatIsoNow returns non-empty`() {
        val iso = DateUtils.formatIsoNow()
        assertTrue(iso.isNotEmpty())
        assertTrue(iso.endsWith("Z"))
    }

    @Test
    fun `round-trip preserves approximate value`() {
        val now = 1705312200000L // fixed timestamp
        val iso = DateUtils.formatIsoUtc(now)
        val parsed = DateUtils.parseIsoToMillis(iso, fallback = 0L)
        // Allow 1 second tolerance
        assertTrue("Round-trip diff: ${kotlin.math.abs(now - parsed)}", kotlin.math.abs(now - parsed) < 2000)
    }

    @Test
    fun `parseIsoToMillis uses default fallback when not provided`() {
        val before = System.currentTimeMillis()
        val result = DateUtils.parseIsoToMillis("garbage")
        val after = System.currentTimeMillis()
        assertTrue(result in before..after)
    }
}

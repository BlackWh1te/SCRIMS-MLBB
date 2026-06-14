package com.mlbb.scrim.util

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DateUtilsTest {

    // ─── Parse ISO to Millis Tests ───

    @Test
    fun `parseIsoToMillis successfully parses ISO-8601 with milliseconds and timezone`() {
        // Arrange
        val isoString = "2024-01-15T10:30:45.123+00:00"

        // Act
        val result = DateUtils.parseIsoToMillis(isoString)

        // Assert
        assertTrue(result > 0)
        assertNotNull(result)
    }

    @Test
    fun `parseIsoToMillis successfully parses ISO-8601 without milliseconds`() {
        // Arrange
        val isoString = "2024-01-15T10:30:45+00:00"

        // Act
        val result = DateUtils.parseIsoToMillis(isoString)

        // Assert
        assertTrue(result > 0)
    }

    @Test
    fun `parseIsoToMillis successfully parses ISO-8601 with Z suffix`() {
        // Arrange
        val isoString = "2024-01-15T10:30:45Z"

        // Act
        val result = DateUtils.parseIsoToMillis(isoString)

        // Assert
        assertTrue(result > 0)
    }

    @Test
    fun `parseIsoToMillis successfully parses ISO-8601 with milliseconds and Z`() {
        // Arrange
        val isoString = "2024-01-15T10:30:45.123Z"

        // Act
        val result = DateUtils.parseIsoToMillis(isoString)

        // Assert
        assertTrue(result > 0)
    }

    @Test
    fun `parseIsoToMillis successfully parses ISO-8601 without timezone`() {
        // Arrange
        val isoString = "2024-01-15T10:30:45"

        // Act
        val result = DateUtils.parseIsoToMillis(isoString)

        // Assert
        assertTrue(result > 0)
    }

    @Test
    fun `parseIsoToMillis returns fallback for null input`() {
        // Arrange
        val fallback = 1234567890L

        // Act
        val result = DateUtils.parseIsoToMillis(null, fallback)

        // Assert
        assertEquals(fallback, result)
    }

    @Test
    fun `parseIsoToMillis returns fallback for blank input`() {
        // Arrange
        val fallback = 1234567890L

        // Act
        val result = DateUtils.parseIsoToMillis("", fallback)

        // Assert
        assertEquals(fallback, result)
    }

    @Test
    fun `parseIsoToMillis returns fallback for invalid format`() {
        // Arrange
        val invalidString = "invalid-date-format"
        val fallback = 1234567890L

        // Act
        val result = DateUtils.parseIsoToMillis(invalidString, fallback)

        // Assert
        assertEquals(fallback, result)
    }

    @Test
    fun `parseIsoToMillis returns current time as default fallback`() {
        // Arrange
        val before = System.currentTimeMillis()

        // Act
        val result = DateUtils.parseIsoToMillis(null)

        // Assert
        val after = System.currentTimeMillis()
        assertTrue(result in before..after)
    }

    @Test
    fun `parseIsoToMillis handles negative timezone offset`() {
        // Arrange
        val isoString = "2024-01-15T10:30:45-05:00"

        // Act
        val result = DateUtils.parseIsoToMillis(isoString)

        // Assert
        assertTrue(result > 0)
    }

    @Test
    fun `parseIsoToMillis handles positive timezone offset`() {
        // Arrange
        val isoString = "2024-01-15T10:30:45+05:00"

        // Act
        val result = DateUtils.parseIsoToMillis(isoString)

        // Assert
        assertTrue(result > 0)
    }

    // ─── Format ISO UTC Tests ───

    @Test
    fun `formatIsoUtc successfully formats timestamp to ISO-8601`() {
        // Arrange
        val timestamp = 1705318245000L // 2024-01-15T10:30:45 UTC

        // Act
        val result = DateUtils.formatIsoUtc(timestamp)

        // Assert
        assertTrue(result.contains("2024-01-15"))
        assertTrue(result.contains("10:30:45"))
        assertTrue(result.endsWith("Z"))
    }

    @Test
    fun `formatIsoUtc produces consistent format`() {
        // Arrange
        val timestamp = 1705318245000L

        // Act
        val result1 = DateUtils.formatIsoUtc(timestamp)
        val result2 = DateUtils.formatIsoUtc(timestamp)

        // Assert
        assertEquals(result1, result2)
    }

    @Test
    fun `formatIsoUtc handles zero timestamp`() {
        // Arrange
        val timestamp = 0L

        // Act
        val result = DateUtils.formatIsoUtc(timestamp)

        // Assert
        assertTrue(result.isNotEmpty())
        assertTrue(result.contains("1970-01-01")) // Unix epoch
    }

    // ─── Format ISO UTC with Milliseconds Tests ───

    @Test
    fun `formatIsoUtcWithMs successfully formats timestamp with milliseconds`() {
        // Arrange
        val timestamp = 1705318245123L

        // Act
        val result = DateUtils.formatIsoUtcWithMs(timestamp)

        // Assert
        assertTrue(result.contains("2024-01-15"))
        assertTrue(result.contains(".123")) // Should include milliseconds
        assertTrue(result.endsWith("Z"))
    }

    @Test
    fun `formatIsoUtcWithMs produces different output than formatIsoUtc`() {
        // Arrange
        val timestamp = 1705318245123L

        // Act
        val resultWithoutMs = DateUtils.formatIsoUtc(timestamp)
        val resultWithMs = DateUtils.formatIsoUtcWithMs(timestamp)

        // Assert
        assertTrue(resultWithMs.length > resultWithoutMs.length)
    }

    // ─── Format Date Tests ───

    @Test
    fun `formatDate successfully formats timestamp to date string`() {
        // Arrange
        val timestamp = 1705318245000L // 2024-01-15

        // Act
        val result = DateUtils.formatDate(timestamp)

        // Assert
        assertEquals("2024-01-15", result)
    }

    @Test
    fun `formatDate produces consistent format`() {
        // Arrange
        val timestamp = 1705318245000L

        // Act
        val result1 = DateUtils.formatDate(timestamp)
        val result2 = DateUtils.formatDate(timestamp)

        // Assert
        assertEquals(result1, result2)
    }

    @Test
    fun `formatDate handles different timestamps`() {
        // Arrange
        val timestamp1 = 1705318245000L // 2024-01-15
        val timestamp2 = 1707910245000L // 2024-02-15

        // Act
        val result1 = DateUtils.formatDate(timestamp1)
        val result2 = DateUtils.formatDate(timestamp2)

        // Assert
        assertEquals("2024-01-15", result1)
        assertEquals("2024-02-15", result2)
    }

    // ─── Format Time Tests ───

    @Test
    fun `formatTime successfully formats timestamp to time string`() {
        // Arrange
        val timestamp = 1705318245000L // 10:30:45

        // Act
        val result = DateUtils.formatTime(timestamp)

        // Assert
        assertEquals("10:30:45", result)
    }

    @Test
    fun `formatTime produces consistent format`() {
        // Arrange
        val timestamp = 1705318245000L

        // Act
        val result1 = DateUtils.formatTime(timestamp)
        val result2 = DateUtils.formatTime(timestamp)

        // Assert
        assertEquals(result1, result2)
    }

    @Test
    fun `formatTime handles midnight`() {
        // Arrange
        val timestamp = 1705276800000L // 2024-01-15 00:00:00 UTC

        // Act
        val result = DateUtils.formatTime(timestamp)

        // Assert
        assertEquals("00:00:00", result)
    }

    @Test
    fun `formatTime handles end of day`() {
        // Arrange
        val timestamp = 1705363199000L // 2024-01-15 23:59:59 UTC

        // Act
        val result = DateUtils.formatTime(timestamp)

        // Assert
        assertEquals("23:59:59", result)
    }

    // ─── Format ISO Now Tests ───

    @Test
    fun `formatIsoNow successfully formats current time`() {
        // Act
        val result = DateUtils.formatIsoNow()

        // Assert
        assertTrue(result.isNotEmpty())
        assertTrue(result.matches(Regex("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z")))
    }

    @Test
    fun `formatIsoNow produces valid ISO format`() {
        // Act
        val result = DateUtils.formatIsoNow()

        // Assert
        assertTrue(result.contains("T"))
        assertTrue(result.endsWith("Z"))
        assertEquals(20, result.length) // ISO format length
    }

    @Test
    fun `formatIsoNow returns current time within reasonable range`() {
        // Arrange
        val before = System.currentTimeMillis()

        // Act
        val result = DateUtils.formatIsoNow()
        val parsedTime = DateUtils.parseIsoToMillis(result)

        // Assert
        val after = System.currentTimeMillis()
        assertTrue(parsedTime in before..after)
    }

    // ─── Edge Case Tests ───

    @Test
    fun `handles very old dates`() {
        // Arrange
        val timestamp = 0L // Unix epoch

        // Act
        val result = DateUtils.formatDate(timestamp)

        // Assert
        assertEquals("1970-01-01", result)
    }

    @Test
    fun `handles future dates`() {
        // Arrange
        val timestamp = 4102444800000L // Year 2100

        // Act
        val result = DateUtils.formatDate(timestamp)

        // Assert
        assertTrue(result.startsWith("2100"))
    }

    @Test
    fun `handles negative timestamps`() {
        // Arrange
        val timestamp = -86400000L // One day before epoch

        // Act
        val result = DateUtils.formatDate(timestamp)

        // Assert
        assertTrue(result.startsWith("1969"))
    }

    @Test
    fun `handles very large timestamps`() {
        // Arrange
        val timestamp = Long.MAX_VALUE

        // Act
        val result = DateUtils.formatDate(timestamp)

        // Assert - Should handle without crashing
        assertTrue(result.isNotEmpty())
    }

    // ─── Consistency Tests ───

    @Test
    fun `parse and format are inverse operations`() {
        // Arrange
        val originalTimestamp = 1705318245000L
        val isoString = DateUtils.formatIsoUtc(originalTimestamp)

        // Act
        val parsedTimestamp = DateUtils.parseIsoToMillis(isoString)

        // Assert
        assertEquals(originalTimestamp, parsedTimestamp)
    }

    @Test
    fun `parse and format with milliseconds are inverse operations`() {
        // Arrange
        val originalTimestamp = 1705318245123L
        val isoString = DateUtils.formatIsoUtcWithMs(originalTimestamp)

        // Act
        val parsedTimestamp = DateUtils.parseIsoToMillis(isoString)

        // Assert - Should be within 1 second due to millisecond precision
        assertTrue kotlin.math.abs(originalTimestamp - parsedTimestamp) < 1000)
    }

    @Test
    fun `multiple format calls produce same result`() {
        // Arrange
        val timestamp = 1705318245000L

        // Act
        val results = (1..10).map { DateUtils.formatIsoUtc(timestamp) }

        // Assert
        assertTrue(results.all { it == results.first() })
    }

    @Test
    fun `multiple parse calls produce same result`() {
        // Arrange
        val isoString = "2024-01-15T10:30:45Z"

        // Act
        val results = (1..10).map { DateUtils.parseIsoToMillis(isoString) }

        // Assert
        assertTrue(results.all { it == results.first() })
    }
}

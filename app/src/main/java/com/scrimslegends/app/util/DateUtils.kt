package com.scrimslegends.app.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Centralized ISO date parsing to avoid duplication across the codebase.
 * All timestamps from Supabase are ISO-8601 formatted.
 */
object DateUtils {

    private val utcTimeZone = TimeZone.getTimeZone("UTC")

    private val isoPatterns by lazy {
        listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss"
        ).map { pattern ->
            SimpleDateFormat(pattern, Locale.US).apply { timeZone = utcTimeZone }
        }
    }

    private val isoFormatter by lazy {
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply { timeZone = utcTimeZone }
    }

    private val isoWithMsFormatter by lazy {
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply { timeZone = utcTimeZone }
    }

    private val dateFormatter by lazy {
        SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = utcTimeZone }
    }

    private val timeFormatter by lazy {
        SimpleDateFormat("HH:mm:ss", Locale.US).apply { timeZone = utcTimeZone }
    }

    /**
     * Parse an ISO-8601 timestamp string to epoch millis.
     * Tries multiple common patterns.
     */
    fun parseIsoToMillis(raw: String?, fallback: Long = System.currentTimeMillis()): Long {
        if (raw.isNullOrBlank()) return fallback
        for (formatter in isoPatterns) {
            try {
                formatter.parse(raw)?.time?.let { return it }
            } catch (_: Exception) {
            }
        }
        return fallback
    }

    /**
     * Format epoch millis as ISO-8601 UTC string.
     */
    fun formatIsoUtc(timestamp: Long): String = isoFormatter.format(Date(timestamp))

    fun formatIsoUtcWithMs(timestamp: Long): String = isoWithMsFormatter.format(Date(timestamp))

    fun formatDate(timestamp: Long): String = dateFormatter.format(Date(timestamp))

    fun formatTime(timestamp: Long): String = timeFormatter.format(Date(timestamp))

    /**
     * Format epoch millis as ISO-8601 UTC string (used for chat opens at, etc.)
     */
    fun formatIsoNow(): String = isoFormatter.format(Date())

    /**
     * Format epoch millis as a human-readable date+time string in the given region's timezone.
     * Appends the region display name in parentheses.
     * Falls back to UTC if regionTimeZoneId is null.
     */
    fun formatDetailedTimeInRegion(
        timestamp: Long,
        regionTimeZoneId: String? = null,
        regionDisplayName: String? = null
    ): String {
        val tz = if (regionTimeZoneId != null) TimeZone.getTimeZone(regionTimeZoneId) else utcTimeZone
        val sdf = SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", java.util.Locale.getDefault())
        sdf.timeZone = tz
        val formatted = sdf.format(Date(timestamp))
        return if (regionDisplayName != null) "$formatted ($regionDisplayName)" else formatted
    }
}

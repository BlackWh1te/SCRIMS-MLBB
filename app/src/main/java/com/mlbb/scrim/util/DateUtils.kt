package com.mlbb.scrim.util

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
        SimpleDateFormat("yyyy-MM-dd", Locale.US)
    }

    private val timeFormatter by lazy {
        SimpleDateFormat("HH:mm:ss", Locale.US)
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
}

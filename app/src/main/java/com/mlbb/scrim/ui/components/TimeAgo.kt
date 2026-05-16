package com.mlbb.scrim.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.mlbb.scrim.R

/**
 * Formats a timestamp into a human-readable "time ago" string.
 *
 * Examples: "Just now", "5 min ago", "2 hours ago", "Yesterday", "3 days ago"
 *
 * @param timestampMs Unix timestamp in milliseconds (e.g., article.publishedAt)
 * @param useShort If true, returns abbreviated forms (e.g., "5m" instead of "5 min ago")
 */
@Composable
fun timeAgo(timestampMs: Long, useShort: Boolean = false): String {
    val now = System.currentTimeMillis()
    val diffMs = now - timestampMs

    if (diffMs < 0) {
        return stringResource(R.string.just_now)
    }

    val seconds = diffMs / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24
    val weeks = days / 7
    val months = days / 30
    val years = days / 365

    return when {
        seconds < 60 -> stringResource(R.string.just_now)

        minutes < 60 -> {
            if (useShort) {
                stringResource(R.string.min_ago_short, minutes)
            } else {
                stringResource(R.string.minutes_ago, minutes)
            }
        }

        hours < 24 -> {
            if (useShort) {
                stringResource(R.string.hr_ago_short, hours)
            } else {
                stringResource(R.string.hours_ago, hours)
            }
        }

        days == 1L -> stringResource(R.string.yesterday)

        days < 7 -> {
            if (useShort) {
                stringResource(R.string.d_ago_short, days)
            } else {
                stringResource(R.string.days_ago, days)
            }
        }

        weeks < 4 -> {
            if (useShort) {
                stringResource(R.string.wk_ago_short, weeks)
            } else {
                stringResource(R.string.weeks_ago, weeks)
            }
        }

        months < 12 -> {
            if (useShort) {
                stringResource(R.string.mo_ago_short, months)
            } else {
                stringResource(R.string.months_ago, months)
            }
        }

        else -> {
            if (useShort) {
                stringResource(R.string.yr_ago_short, years)
            } else {
                stringResource(R.string.years_ago, years)
            }
        }
    }
}

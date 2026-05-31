package com.scrimslegends.app.util

import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import android.widget.Toast
import com.scrimslegends.app.data.model.Scrim

/**
 * Launches the device's default calendar app (e.g. Google Calendar) with a pre-filled event
 * for a confirmed scrim.
 */
object CalendarIntentHelper {

    private const val DURATION_BO1_MIN = 30L
    private const val DURATION_BO2_MIN = 45L
    private const val DURATION_BO3_MIN = 60L
    private const val DURATION_BO5_MIN = 90L

    fun addScrimToCalendar(context: Context, scrim: Scrim): Boolean {
        val durationMinutes = when (scrim.bestOf.games) {
            1 -> DURATION_BO1_MIN
            2 -> DURATION_BO2_MIN
            3 -> DURATION_BO3_MIN
            5 -> DURATION_BO5_MIN
            else -> DURATION_BO3_MIN
        }

        val startMillis = scrim.scheduledTime
        val endMillis = startMillis + durationMinutes * 60 * 1000L

        val title = buildString {
            append("MLBB Scrim")
            if (scrim.teamName.isNotBlank()) {
                append(": ").append(scrim.teamName)
                if (!scrim.opponentTeamName.isNullOrBlank()) {
                    append(" vs ").append(scrim.opponentTeamName)
                }
            }
        }

        val description = buildString {
            appendLine("Game Mode: ${scrim.gameMode.displayName}")
            appendLine("Region: ${scrim.region.displayName} (${scrim.region.utcOffset})")
            appendLine("Skill Level: ${scrim.skillLevel.name}")
            appendLine("Format: ${scrim.bestOf.displayName}")
            if (scrim.description.isNotBlank()) {
                appendLine()
                appendLine("Details: ${scrim.description}")
            }
        }

        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, title)
            putExtra(CalendarContract.Events.DESCRIPTION, description)
            putExtra(CalendarContract.Events.EVENT_LOCATION, "Online")
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMillis)
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endMillis)
        }

        return try {
            context.startActivity(intent)
            true
        } catch (_: android.content.ActivityNotFoundException) {
            Toast.makeText(context, "No calendar app found", Toast.LENGTH_SHORT).show()
            false
        }
    }
}

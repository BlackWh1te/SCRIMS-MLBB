package com.scrimslegends.app.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import com.scrimslegends.app.ui.theme.ErrorRed
import com.scrimslegends.app.ui.theme.SuccessGreen
import com.scrimslegends.app.ui.theme.WarningOrange
import kotlinx.coroutines.delay

/**
 * Live countdown timer for an upcoming scrim.
 * Updates every second while the composable is active.
 *
 * @param targetTime Epoch millis when the scrim starts
 * @param style TextStyle applied to the countdown text
 * @param baseColor Fallback color used when > 1 hour remains
 */
@Composable
fun ScrimCountdown(
    targetTime: Long,
    style: TextStyle = TextStyle.Default,
    baseColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    var remaining by remember { mutableStateOf(targetTime - System.currentTimeMillis()) }

    LaunchedEffect(targetTime) {
        while (true) {
            remaining = targetTime - System.currentTimeMillis()
            delay(1000L)
        }
    }

    val (text, color) = when {
        remaining <= 0L -> "Starting now..." to SuccessGreen
        remaining < 5 * 60 * 1000L -> formatCountdown(remaining) to ErrorRed   // < 5 min
        remaining < 60 * 60 * 1000L -> formatCountdown(remaining) to WarningOrange // < 1 hour
        else -> formatCountdown(remaining) to baseColor
    }

    Text(
        text = text,
        style = style.copy(color = color, fontWeight = FontWeight.SemiBold)
    )
}

private fun formatCountdown(ms: Long): String {
    val totalSeconds = ms / 1000
    val days = totalSeconds / 86400
    val hours = (totalSeconds % 86400) / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    return when {
        days > 0 -> String.format("%dd %02dh %02dm", days, hours, minutes)
        hours > 0 -> String.format("%02dh %02dm %02ds", hours, minutes, seconds)
        else -> String.format("%02dm %02ds", minutes, seconds)
    }
}

package com.scrimslegends.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scrimslegends.app.data.model.Scrim
import com.scrimslegends.app.data.model.ScrimStatus
import com.scrimslegends.app.ui.components.AnimatedEntrance
import com.scrimslegends.app.ui.components.GlassBackButton
import com.scrimslegends.app.R
import androidx.compose.ui.res.stringResource
import com.scrimslegends.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ScheduleScreen(
    scrims: List<Scrim>,
    teams: List<com.scrimslegends.app.data.model.Team> = emptyList(),
    onNavigateBack: () -> Unit,
    onScrimClick: (String) -> Unit
) {
    val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())

    // Only show scrims the user is actively involved in (same logic as HomeScreen)
    val userTeamIds = teams.map { it.id }.toSet()
    val grouped = remember(scrims, teams) {
        scrims.filter { scrim ->
            val isHost = scrim.teamId in userTeamIds
            val isOpponent = scrim.opponentTeamId in userTeamIds
            when {
                isHost -> scrim.status != ScrimStatus.OPEN && scrim.status != ScrimStatus.CANCELLED
                isOpponent -> true
                else -> false
            }
        }
            .sortedBy { it.scheduledTime }
            .groupBy { getDayLabel(it.scheduledTime) }
            .toList()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = heroGradientBrush())
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            AnimatedEntrance(delayMillis = 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                        .padding(top = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GlassBackButton(onClick = onNavigateBack)
                    Text(
                        text = stringResource(R.string.schedule),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = White
                        )
                    )
                    Spacer(modifier = Modifier.width(44.dp))
                }
            }

            if (grouped.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = MidGray,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.no_upcoming_scrims),
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = LightGray,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.create_join_scrim_hint),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MidGray
                            )
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    contentPadding = PaddingValues(vertical = 20.dp)
                ) {
                    grouped.forEachIndexed { sectionIndex, (dayLabel, dayScrims) ->
                        item(key = dayLabel) {
                            AnimatedEntrance(delayMillis = sectionIndex * 100) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(BluePrimary)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = dayLabel,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = White
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(1.dp)
                                            .background(MidGray.copy(alpha = 0.3f))
                                    )
                                }
                            }
                        }

                        items(dayScrims, key = { it.id }) { scrim ->
                            val index = dayScrims.indexOf(scrim)
                            AnimatedEntrance(delayMillis = sectionIndex * 100 + index * 60) {
                                ScheduleCard(
                                    scrim = scrim,
                                    timeFormatter = timeFormatter,
                                    onClick = { onScrimClick(scrim.id) }
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScheduleCard(
    scrim: Scrim,
    timeFormatter: SimpleDateFormat,
    onClick: () -> Unit
) {
    val statusColor = when (scrim.status) {
        ScrimStatus.OPEN -> SuccessGreen
        ScrimStatus.FILLED -> BluePrimary
        ScrimStatus.READY_CHECK -> WarningOrange
        ScrimStatus.IN_PROGRESS -> WarningOrange
        ScrimStatus.COMPLETED -> MidGray
        ScrimStatus.CANCELLED -> ErrorRed
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 6.dp,
                spotColor = BluePrimary.copy(alpha = 0.15f),
                shape = RoundedCornerShape(16.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = DarkNavy
        ),
        shape = RoundedCornerShape(16.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Time column
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(56.dp)
            ) {
                Text(
                    text = timeFormatter.format(Date(scrim.scheduledTime)),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = White
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Vertical divider
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(48.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(statusColor.copy(alpha = 0.4f))
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = scrim.teamName,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = White
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.SportsEsports,
                        contentDescription = null,
                        tint = MidGray,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${scrim.gameMode.displayName} · ${scrim.region.displayName} ${scrim.region.utcOffset}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 13.sp,
                            color = MidGray
                        )
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.People,
                        contentDescription = null,
                        tint = MidGray,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.players_count, scrim.currentPlayers, scrim.maxPlayers),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 13.sp,
                            color = MidGray
                        )
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = scrim.status.name.lowercase().replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = statusColor
                        )
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "View",
                tint = MidGray,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

private fun getDayLabel(timestamp: Long): String {
    val now = Calendar.getInstance()
    val target = Calendar.getInstance().apply { timeInMillis = timestamp }

    return when {
        isSameDay(now, target) -> "Today"
        isSameDay(now.apply { add(Calendar.DAY_OF_YEAR, 1) }, target) -> "Tomorrow"
        else -> {
            val sdf = SimpleDateFormat("EEEE, MMM dd", Locale.getDefault())
            sdf.format(Date(timestamp))
        }
    }
}

private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}

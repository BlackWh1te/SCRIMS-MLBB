package com.mlbb.scrim.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mlbb.scrim.data.model.Player
import com.mlbb.scrim.data.model.ScrimRosterEntry
import com.mlbb.scrim.ui.theme.*
import com.mlbb.scrim.ui.components.AnimatedEntrance
import com.mlbb.scrim.ui.components.GlassBackButton
import com.mlbb.scrim.ui.components.GradientButton

/**
 * Screen where the captain selects which team members are active (playing)
 * and which are substitutes (bench) for a specific scrim.
 * Active players gain/lose pts, substitutes are unaffected.
 */
@Composable
fun ScrimRosterScreen(
    teamName: String,
    teamId: String,
    players: List<Player>,
    existingRoster: List<ScrimRosterEntry>,
    minActivePlayers: Int = 5,
    onNavigateBack: () -> Unit,
    onConfirmRoster: (List<ScrimRosterEntry>) -> Unit
) {
    // Initialize roster state from existing or default
    var rosterEntries by remember(existingRoster, players) {
        mutableStateOf(
            if (existingRoster.isNotEmpty()) {
                existingRoster.toList()
            } else {
                players.map { player ->
                    ScrimRosterEntry(
                        playerId = player.id,
                        playerName = player.name,
                        teamId = teamId,
                        isActive = false  // Default: substitute, captain must activate
                    )
                }
            }
        )
    }

    val activeCount = rosterEntries.count { it.isActive }
    val substituteCount = rosterEntries.count { !it.isActive }
    val isValid = activeCount >= minActivePlayers

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = heroGradientBrush())
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
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
                    GlassBackButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.background(
                            color = ErrorRed.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(12.dp)
                        )
                    )

                    Text(
                        text = "Select Roster",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = White
                        )
                    )

                    Spacer(modifier = Modifier.width(44.dp))
                }
            }

            // Team Info + Roster Summary
            AnimatedEntrance(delayMillis = 100) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .shadow(
                            elevation = 4.dp,
                            spotColor = BluePrimary.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(16.dp)
                        ),
                    colors = CardDefaults.cardColors(containerColor = DarkNavy),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Text(
                            text = teamName,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = White
                            )
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            RosterStatBox(
                                modifier = Modifier.weight(1f),
                                label = "Active",
                                value = "$activeCount",
                                tint = SuccessGreen
                            )
                            RosterStatBox(
                                modifier = Modifier.weight(1f),
                                label = "Substitutes",
                                value = "$substituteCount",
                                tint = WarningOrange
                            )
                            RosterStatBox(
                                modifier = Modifier.weight(1f),
                                label = "Required",
                                value = "$minActivePlayers+",
                                tint = if (isValid) SuccessGreen else ErrorRed
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Info banner
            AnimatedEntrance(delayMillis = 150) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isValid) SuccessGreen.copy(alpha = 0.08f)
                        else WarningOrange.copy(alpha = 0.08f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isValid) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (isValid) SuccessGreen else WarningOrange,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (isValid)
                                "Roster valid: $activeCount active players selected"
                            else
                                "Need at least $minActivePlayers active players (currently $activeCount)",
                            fontSize = 13.sp,
                            color = if (isValid) SuccessGreen else WarningOrange
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Section: Active Players
            AnimatedEntrance(delayMillis = 200) {
                Text(
                    text = "ACTIVE — Playing (pts affected)",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SuccessGreen
                    ),
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Player List
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 20.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                itemsIndexed(rosterEntries) { index, entry ->
                    AnimatedEntrance(delayMillis = 250 + index * 40) {
                        RosterPlayerCard(
                            entry = entry,
                            onToggleActive = {
                                rosterEntries = rosterEntries.toMutableList().apply {
                                    this[index] = entry.copy(isActive = !entry.isActive)
                                }
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // Confirm Button
            AnimatedEntrance(delayMillis = 400) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    // Points explanation
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = DarkNavy.copy(alpha = 0.6f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = BluePrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp)
                            )
                            Text(
                                text = "Active players: +25 pts (win) / -15 pts (loss). Substitutes: 0 pts.",
                                fontSize = 12.sp,
                                color = LightGray
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    GradientButton(
                        text = if (isValid) "Confirm Roster ($activeCount active)" else "Need $minActivePlayers+ active players",
                        onClick = {
                            if (isValid) {
                                onConfirmRoster(rosterEntries.toList())
                            }
                        },
                        gradient = if (isValid) GoldGradient else listOf(Color.Gray, Color.DarkGray),
                        enabled = isValid,
                        height = 56.dp
                    )
                }
            }
        }
    }
}

@Composable
private fun RosterPlayerCard(
    entry: ScrimRosterEntry,
    onToggleActive: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 2.dp,
                spotColor = Color.Black.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (entry.isActive) SuccessGreen.copy(alpha = 0.08f) else DarkNavy
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggleActive() }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Player avatar
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = if (entry.isActive)
                                listOf(SuccessGreen, Color(0xFF006400))
                            else
                                listOf(BluePrimary, Color(0xFF0A5A9F))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = entry.playerName.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = White
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Player name
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.playerName,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = White
                    )
                )
                Text(
                    text = if (entry.isActive) "Active — pts affected" else "Substitute — no pts change",
                    fontSize = 12.sp,
                    color = if (entry.isActive) SuccessGreen else MidGray
                )
            }

            // Toggle switch
            Switch(
                checked = entry.isActive,
                onCheckedChange = { onToggleActive() },
                colors = SwitchDefaults.colors(
                    checkedTrackColor = SuccessGreen.copy(alpha = 0.5f),
                    checkedThumbColor = SuccessGreen,
                    uncheckedTrackColor = White.copy(alpha = 0.1f),
                    uncheckedThumbColor = MidGray
                )
            )
        }
    }
}

@Composable
private fun RosterStatBox(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    tint: Color
) {
    Box(
        modifier = modifier
            .background(
                color = tint.copy(alpha = 0.08f),
                shape = RoundedCornerShape(10.dp)
            )
            .padding(vertical = 10.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = tint
            )
            Text(
                text = label,
                fontSize = 10.sp,
                color = LightGray
            )
        }
    }
}

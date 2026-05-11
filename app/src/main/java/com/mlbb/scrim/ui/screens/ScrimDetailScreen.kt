package com.mlbb.scrim.ui.screens

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mlbb.scrim.data.model.Scrim
import com.mlbb.scrim.data.model.ScrimStatus
import com.mlbb.scrim.ui.theme.*
import com.mlbb.scrim.ui.components.AnimatedEntrance
import com.mlbb.scrim.ui.components.GlassBackButton
import com.mlbb.scrim.ui.components.GradientButton
import com.mlbb.scrim.ui.components.EnhancedStatusBadge
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ScrimDetailScreen(
    scrim: Scrim,
    currentUserId: String,
    applicantTeamId: String? = null,
    applicantTeamName: String? = null,
    onNavigateBack: () -> Unit,
    onJoinScrim: (String) -> Unit,
    onLeaveScrim: (String) -> Unit,
    onApplyScrim: (Scrim) -> Unit = {},
    onCancelScrim: ((String) -> Unit)? = null
) {
    var showCancelDialog by remember { mutableStateOf(false) }
    var isJoined by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = heroGradientBrush()
            )
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
                    GlassBackButton(onClick = onNavigateBack)

                    Text(
                        text = "Scrim Details",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = White
                        )
                    )

                    Spacer(modifier = Modifier.width(44.dp))
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                contentPadding = PaddingValues(vertical = 24.dp)
            ) {
                // Team Header Card
                item {
                    AnimatedEntrance(delayMillis = 100) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(
                                    elevation = 8.dp,
                                    spotColor = BluePrimary.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(20.dp)
                                ),
                            colors = CardDefaults.cardColors(
                                containerColor = DarkNavy
                            ),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // Team Avatar with gradient
                                Box(
                                    modifier = Modifier
                                        .size(90.dp)
                                        .shadow(
                                            elevation = 12.dp,
                                            spotColor = BluePrimary.copy(alpha = 0.3f),
                                            shape = CircleShape
                                        )
                                        .clip(CircleShape)
                                        .background(
                                            brush = Brush.verticalGradient(
                                                colors = listOf(BluePrimary, Color(0xFF0A5A9F))
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = scrim.teamName.firstOrNull()?.uppercaseChar()?.toString() ?: "T",
                                        fontSize = 40.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = White
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Team Name
                                Text(
                                    text = scrim.teamName,
                                    style = MaterialTheme.typography.headlineLarge.copy(
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = White
                                    )
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                // Status Badge
                                EnhancedStatusBadge(
                                    text = when (scrim.status) {
                                        ScrimStatus.OPEN -> "Open"
                                        ScrimStatus.FILLED -> "Filled"
                                        ScrimStatus.IN_PROGRESS -> "In Progress"
                                        ScrimStatus.COMPLETED -> "Completed"
                                        ScrimStatus.CANCELLED -> "Cancelled"
                                    },
                                    color = when (scrim.status) {
                                        ScrimStatus.OPEN -> SuccessGreen
                                        ScrimStatus.FILLED -> WarningOrange
                                        ScrimStatus.IN_PROGRESS -> BluePrimary
                                        ScrimStatus.COMPLETED -> LightGray
                                        ScrimStatus.CANCELLED -> ErrorRed
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }

                // Scrim Info
                item {
                    AnimatedEntrance(delayMillis = 200) {
                        Text(
                            text = "Scrim Info",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontSize = 22.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = White
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                }

                item {
                    AnimatedEntrance(delayMillis = 250) {
                        InfoCard(
                            icon = Icons.Default.SportsEsports,
                            label = "Game Mode",
                            value = scrim.gameMode.name
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }

                item {
                    AnimatedEntrance(delayMillis = 300) {
                        InfoCard(
                            icon = Icons.Default.Public,
                            label = "Region",
                            value = scrim.region.name
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }

                item {
                    AnimatedEntrance(delayMillis = 350) {
                        InfoCard(
                            icon = Icons.Default.Star,
                            label = "Skill Level",
                            value = scrim.skillLevel.name
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }

                item {
                    AnimatedEntrance(delayMillis = 400) {
                        InfoCard(
                            icon = Icons.Default.AccessTime,
                            label = "Scheduled Time",
                            value = formatDetailedTime(scrim.scheduledTime)
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }

                item {
                    AnimatedEntrance(delayMillis = 450) {
                        InfoCard(
                            icon = Icons.Default.Person,
                            label = "Players",
                            value = "${scrim.currentPlayers} / ${scrim.maxPlayers}"
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Description
                if (scrim.description.isNotBlank()) {
                    item {
                        AnimatedEntrance(delayMillis = 500) {
                            Text(
                                text = "Description",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = White
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))
                    }

                    item {
                        AnimatedEntrance(delayMillis = 550) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .shadow(
                                        elevation = 4.dp,
                                        spotColor = Color.Black.copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(16.dp)
                                    ),
                                colors = CardDefaults.cardColors(
                                    containerColor = DarkNavy
                                ),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text(
                                    text = scrim.description,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontSize = 16.sp,
                                        color = LightGray
                                    ),
                                    modifier = Modifier.padding(24.dp)
                                )
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(28.dp))
                    }
                }

                // Action Button
                item {
                    val isCreator = scrim.teamLeader == currentUserId
                    val (buttonText, buttonEnabled, buttonGradient) = when (scrim.status) {
                        ScrimStatus.OPEN -> {
                            if (scrim.currentPlayers < scrim.maxPlayers) {
                                Triple("Join Scrim", true, SuccessGradient)
                            } else {
                                Triple("Scrim Full", false, listOf(Color.Gray, Color.DarkGray))
                            }
                        }
                        ScrimStatus.FILLED -> Triple("Scrim Full", false, listOf(Color.Gray, Color.DarkGray))
                        ScrimStatus.IN_PROGRESS -> Triple("In Progress", false, listOf(BluePrimary, Color(0xFF0A5A9F)))
                        ScrimStatus.COMPLETED -> Triple("Completed", false, listOf(Color.Gray, Color.DarkGray))
                        ScrimStatus.CANCELLED -> Triple("Cancelled", false, listOf(Color.Gray, Color.DarkGray))
                    }

                    AnimatedEntrance(delayMillis = 600) {
                        if (isCreator) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                GradientButton(
                                    text = "Your Scrim",
                                    onClick = { },
                                    enabled = false,
                                    gradient = listOf(Color.Gray, Color.DarkGray),
                                    height = 56.dp
                                )
                                if ((scrim.status == ScrimStatus.OPEN || scrim.status == ScrimStatus.FILLED) && onCancelScrim != null) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    GradientButton(
                                        text = "Cancel Scrim",
                                        onClick = { showCancelDialog = true },
                                        gradient = listOf(ErrorRed, ErrorRed.copy(alpha = 0.7f)),
                                        height = 56.dp
                                    )
                                }
                            }
                        } else {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                GradientButton(
                                    text = buttonText,
                                    onClick = { if (buttonEnabled && scrim.status == ScrimStatus.OPEN) onJoinScrim(scrim.id) },
                                    enabled = buttonEnabled,
                                    gradient = buttonGradient,
                                    height = 56.dp
                                )

                                if (!applicantTeamId.isNullOrBlank() && scrim.status == ScrimStatus.OPEN) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    GradientButton(
                                        text = "Apply with Team",
                                        onClick = { onApplyScrim(scrim) },
                                        enabled = true,
                                        gradient = BlueGradient,
                                        height = 56.dp
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }

    // Cancel Scrim Dialog
    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            containerColor = DarkNavy,
            title = {
                Text(
                    text = "Cancel Scrim?",
                    color = White,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "This will cancel the scrim and notify all participants. Are you sure?",
                    color = LightGray,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onCancelScrim?.invoke(scrim.id)
                        showCancelDialog = false
                    }
                ) {
                    Text("Cancel Scrim", color = ErrorRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) {
                    Text("Keep Scrim", color = MidGray)
                }
            }
        )
    }
}

@Composable
fun InfoCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                spotColor = Color.Black.copy(alpha = 0.1f),
                shape = RoundedCornerShape(16.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = DarkNavy
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(
                        color = BluePrimary.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(14.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = BluePrimary,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.width(18.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.sp,
                        color = LightGray,
                        fontWeight = FontWeight.Medium
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 17.sp,
                        color = White,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
        }
    }
}

fun formatDetailedTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

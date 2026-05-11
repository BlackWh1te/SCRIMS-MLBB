package com.mlbb.scrim.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mlbb.scrim.ui.theme.*
import com.mlbb.scrim.ui.components.AnimatedEntrance
import com.mlbb.scrim.ui.components.GradientButton
import com.mlbb.scrim.ui.components.EmptyState
import com.mlbb.scrim.ui.components.ShimmerCard
import com.mlbb.scrim.ui.components.GlassBackButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.graphicsLayer

@Composable
fun TeamListScreen(
    teams: List<com.mlbb.scrim.data.model.Team>,
    isLoading: Boolean = false,
    onNavigateBack: () -> Unit,
    onNavigateToCreateTeam: () -> Unit,
    onNavigateToJoinTeam: () -> Unit = {},
    onNavigateToTeamDetail: (com.mlbb.scrim.data.model.Team) -> Unit,
    onRefresh: () -> Unit = {}
) {
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
                        text = "My Teams",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = White
                        )
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Refresh button
                        IconButton(
                            onClick = onRefresh,
                            modifier = Modifier
                                .size(44.dp)
                                .background(
                                    color = White.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh",
                                tint = LightGray,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // Join button
                        IconButton(
                            onClick = onNavigateToJoinTeam,
                            modifier = Modifier
                                .size(44.dp)
                                .background(
                                    color = SuccessGreen.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Default.GroupAdd,
                                contentDescription = "Join Team",
                                tint = SuccessGreen,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // Create button
                        IconButton(
                            onClick = onNavigateToCreateTeam,
                            modifier = Modifier
                                .size(44.dp)
                                .background(
                                    color = BluePrimary,
                                    shape = RoundedCornerShape(12.dp)
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Create Team",
                                tint = White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }

            if (isLoading) {
                // Shimmer Loading State
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    repeat(3) {
                        ShimmerCard(
                            modifier = Modifier.padding(vertical = 4.dp),
                            shape = RoundedCornerShape(16.dp)
                        )
                    }
                }
            } else if (teams.isEmpty()) {
                // Empty State
                EmptyState(
                    icon = Icons.Default.GroupAdd,
                    title = "No teams yet",
                    subtitle = "Create your first team or join one with an invite code",
                    modifier = Modifier.fillMaxSize(),
                    action = {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            GradientButton(
                                text = "Create Team",
                                onClick = onNavigateToCreateTeam,
                                gradient = GoldGradient
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            com.mlbb.scrim.ui.components.GhostButton(
                                text = "Join Team",
                                onClick = onNavigateToJoinTeam,
                                borderColor = SuccessGreen,
                                contentColor = SuccessGreen
                            )
                        }
                    }
                )
            } else {
                // Team List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    contentPadding = PaddingValues(vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(teams) { index, team ->
                        AnimatedEntrance(delayMillis = index * 60) {
                            TeamCard(
                                team = team,
                                onClick = { onNavigateToTeamDetail(team) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamCard(
    team: com.mlbb.scrim.data.model.Team,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = tween(100, easing = AppEaseOutCubic),
        label = "teamCardScale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (isPressed) 2.dp else 4.dp,
                spotColor = Color.Black.copy(alpha = 0.1f),
                shape = RoundedCornerShape(16.dp)
            )
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        colors = CardDefaults.cardColors(containerColor = DarkNavy),
        shape = RoundedCornerShape(16.dp),
        onClick = onClick,
        interactionSource = interactionSource
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Team Avatar
            Surface(
                modifier = Modifier.size(56.dp),
                shape = RoundedCornerShape(14.dp),
                color = BluePrimary.copy(alpha = 0.15f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = team.name.firstOrNull()?.uppercaseChar()?.toString() ?: "T",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = BluePrimary
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Team Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = team.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = White
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Players",
                        tint = LightGray,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${team.players.size} / ${team.maxPlayers} players",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 14.sp,
                            color = LightGray
                        )
                    )
                }
            }

            // Chevron
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "View team",
                tint = LightGray.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

package com.scrimslegends.app.ui.screens

import androidx.compose.material3.MaterialTheme
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.graphicsLayer
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
import com.scrimslegends.app.ui.theme.*
import com.scrimslegends.app.ui.components.AnimatedEntrance
import com.scrimslegends.app.ui.components.GradientButton
import com.scrimslegends.app.ui.components.EmptyState
import com.scrimslegends.app.ui.components.TeamListSkeleton
import com.scrimslegends.app.ui.components.GlassBackButton
import com.scrimslegends.app.ui.components.PullToRefreshContainer
import com.scrimslegends.app.ui.utils.HapticFeedback
import com.scrimslegends.app.R
import androidx.compose.ui.res.stringResource
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext

@Composable
fun TeamListScreen(
    teams: List<com.scrimslegends.app.data.model.Team>,
    isLoading: Boolean = false,
    isRefreshing: Boolean = false,
    onNavigateBack: () -> Unit,
    onNavigateToCreateTeam: () -> Unit,
    onNavigateToJoinTeam: () -> Unit = {},
    onNavigateToFindTeams: () -> Unit = {},
    onNavigateToTeamDetail: (com.scrimslegends.app.data.model.Team) -> Unit,
    onRefresh: () -> Unit = {}
) {
    val appTextPrimary = appTextPrimaryColor()
    val appTextSecondary = appTextSecondaryColor()
    val appElevatedSurface = appElevatedSurfaceColor()

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
                        text = stringResource(R.string.my_teams),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = appTextPrimary
                        )
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Refresh button
                        IconButton(
                            onClick = onRefresh,
                            modifier = Modifier
                            .size(44.dp)
                            .background(
                                    color = appElevatedSurface,
                                    shape = RoundedCornerShape(12.dp)
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = stringResource(R.string.refresh),
                                tint = appTextSecondary,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // Find teams button
                        IconButton(
                            onClick = onNavigateToFindTeams,
                            modifier = Modifier
                                .size(44.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = stringResource(R.string.find_teams),
                                tint = MaterialTheme.colorScheme.primary,
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
                                contentDescription = stringResource(R.string.join_team),
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
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = RoundedCornerShape(12.dp)
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = stringResource(R.string.create_team),
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }

            PullToRefreshContainer(
                isRefreshing = isRefreshing,
                onRefresh = onRefresh,
                modifier = Modifier.weight(1f)
            ) {
                when {
                    isLoading && teams.isEmpty() -> {
                        TeamListSkeleton(
                            modifier = Modifier.fillMaxSize(),
                            itemCount = 4
                        )
                    }
                    teams.isEmpty() -> {
                        EmptyState(
                            icon = Icons.Default.GroupAdd,
                            title = stringResource(R.string.no_teams_yet),
                            subtitle = stringResource(R.string.no_teams_subtitle),
                            modifier = Modifier.fillMaxSize(),
                            action = {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    GradientButton(
                                        text = stringResource(R.string.create_team),
                                        onClick = onNavigateToCreateTeam,
                                        gradient = PremiumBlueGradient
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    com.scrimslegends.app.ui.components.GhostButton(
                                        text = stringResource(R.string.join_team),
                                        onClick = onNavigateToJoinTeam,
                                        borderColor = SuccessGreen,
                                        contentColor = SuccessGreen
                                    )
                                }
                            }
                        )
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 120.dp),
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
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamCard(
    team: com.scrimslegends.app.data.model.Team,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val context = LocalContext.current
    val appSurface = appSurfaceColor()
    val appTextPrimary = appTextPrimaryColor()
    val appTextSecondary = appTextSecondaryColor()

    LaunchedEffect(isPressed) {
        if (isPressed) {
            HapticFeedback.performClick(context)
        }
    }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = tween(100, easing = AppEaseOutCubic),
        label = "teamCardScale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, appBorderColor(), RoundedCornerShape(16.dp))
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        colors = CardDefaults.cardColors(containerColor = appSurface),
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
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = team.name.firstOrNull()?.uppercaseChar()?.toString() ?: "T",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
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
                        color = appTextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = stringResource(R.string.players),
                        tint = appTextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.players_count, team.players.size, team.maxPlayers),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 14.sp,
                            color = appTextSecondary
                        )
                    )
                }
            }

            // Chevron
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = stringResource(R.string.view_team),
                tint = appTextSecondary.copy(alpha = 0.75f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

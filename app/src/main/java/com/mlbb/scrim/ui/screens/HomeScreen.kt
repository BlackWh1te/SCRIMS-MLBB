package com.mlbb.scrim.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mlbb.scrim.ui.theme.*
import com.mlbb.scrim.ui.components.AnimatedEntrance
import com.mlbb.scrim.ui.components.TierBadge

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    userProfile: com.mlbb.scrim.data.model.UserProfile?,
    onLogout: () -> Unit,
    onNavigateToCreateTeam: () -> Unit = {},
    onNavigateToCreateScrim: () -> Unit = {},
    onNavigateToLeaderboard: () -> Unit = {},
    onNavigateToMatchHistory: () -> Unit = {},
    onNavigateToSchedule: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    notificationCount: Int = 0
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = heroGradientBrush())
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // Header
            AnimatedEntrance(delayMillis = 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Welcome back,",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = 14.sp,
                                color = MidGray
                            )
                        )
                        Text(
                            text = userProfile?.username ?: "Player",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold,
                                color = White
                            )
                        )
                        if (userProfile != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            TierBadge(tierName = userProfile.currentTier.displayName)
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Notification Bell
                        Box {
                            IconButton(
                                onClick = onNavigateToNotifications,
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(
                                        color = White.copy(alpha = 0.08f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = "Notifications",
                                    tint = if (notificationCount > 0) GoldPrimary else LightGray,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            if (notificationCount > 0) {
                                Badge(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .offset(x = 2.dp, y = (-2).dp),
                                    containerColor = ErrorRed,
                                    contentColor = White
                                ) {
                                    Text(
                                        text = notificationCount.coerceAtMost(9).toString(),
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }

                        // Logout
                        IconButton(
                            onClick = onLogout,
                            modifier = Modifier
                                .size(44.dp)
                                .background(
                                    color = ErrorRed.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Default.ExitToApp,
                                contentDescription = "Logout",
                                tint = ErrorRed,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Stats Row
            AnimatedEntrance(delayMillis = 100) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        icon = Icons.Default.EmojiEvents,
                        label = "Matches",
                        value = (userProfile?.totalMatches ?: 0).toString(),
                        color = GoldPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        icon = Icons.Default.Star,
                        label = "Win Rate",
                        value = userProfile?.winRate ?: "0%",
                        color = SuccessGreen,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        icon = Icons.Default.TrendingUp,
                        label = "XP",
                        value = userProfile?.xp?.let { "${it / 1000}.${(it % 1000) / 100}k" } ?: "0",
                        color = BluePrimary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Quick Actions
            AnimatedEntrance(delayMillis = 200) {
                Text(
                    text = "Quick Actions",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = White
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Post Scrim Card
            AnimatedEntrance(delayMillis = 250) {
                ActionRowCard(
                    icon = Icons.Default.PostAdd,
                    title = "Post a Scrim",
                    subtitle = "List your team for a match",
                    color = BluePrimary,
                    onClick = onNavigateToCreateScrim
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Create Team Card
            AnimatedEntrance(delayMillis = 300) {
                ActionRowCard(
                    icon = Icons.Default.AddCircle,
                    title = "Create Team",
                    subtitle = "Build your squad",
                    color = WarningOrange,
                    onClick = onNavigateToCreateTeam
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // View Leaderboard Card
            AnimatedEntrance(delayMillis = 325) {
                ActionRowCard(
                    icon = Icons.Default.EmojiEvents,
                    title = "Leaderboard",
                    subtitle = "See top players & teams",
                    color = Purple,
                    onClick = onNavigateToLeaderboard
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Match History Card
            AnimatedEntrance(delayMillis = 337) {
                ActionRowCard(
                    icon = Icons.Default.History,
                    title = "Match History",
                    subtitle = "View past matches",
                    color = SuccessGreen,
                    onClick = onNavigateToMatchHistory
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Schedule Card
            AnimatedEntrance(delayMillis = 344) {
                ActionRowCard(
                    icon = Icons.Default.CalendarMonth,
                    title = "Schedule",
                    subtitle = "Upcoming scrims",
                    color = BluePrimary,
                    onClick = onNavigateToSchedule
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // XP & Ranking Card
            AnimatedEntrance(delayMillis = 350) {
                XpProgressCard(userProfile = userProfile)
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun StatCard(
    icon: ImageVector,
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .shadow(
                elevation = 6.dp,
                spotColor = color.copy(alpha = 0.25f),
                shape = RoundedCornerShape(16.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = DarkNavy),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = color.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = White
                )
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 12.sp,
                    color = MidGray
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActionRowCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                spotColor = color.copy(alpha = 0.2f),
                shape = RoundedCornerShape(16.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = DarkNavy),
        shape = RoundedCornerShape(16.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(color, color.copy(alpha = 0.7f))
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = White
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 13.sp,
                        color = MidGray
                    )
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = LightGray.copy(alpha = 0.4f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun XpProgressCard(userProfile: com.mlbb.scrim.data.model.UserProfile?) {
    val tier = userProfile?.currentTier ?: com.mlbb.scrim.data.model.RankTier.BRONZE
    val xp = userProfile?.xp ?: 0
    val progress = userProfile?.xpProgress ?: 0f
    val xpToNext = userProfile?.xpToNext ?: 0
    val nextTier = userProfile?.nextTierName ?: "Silver"

    val tierColor = when (tier) {
        com.mlbb.scrim.data.model.RankTier.BRONZE -> Bronze
        com.mlbb.scrim.data.model.RankTier.SILVER -> Silver
        com.mlbb.scrim.data.model.RankTier.GOLD -> GoldPrimary
        com.mlbb.scrim.data.model.RankTier.PLATINUM -> Platinum
        com.mlbb.scrim.data.model.RankTier.DIAMOND -> Diamond
        com.mlbb.scrim.data.model.RankTier.MASTER -> Purple
        com.mlbb.scrim.data.model.RankTier.GRANDMASTER -> Grandmaster
    }

    val tierGradient = when (tier) {
        com.mlbb.scrim.data.model.RankTier.BRONZE -> listOf(Bronze, Color(0xFF8B4513))
        com.mlbb.scrim.data.model.RankTier.SILVER -> listOf(Silver, Color(0xFF808080))
        com.mlbb.scrim.data.model.RankTier.GOLD -> GoldGradient
        com.mlbb.scrim.data.model.RankTier.PLATINUM -> listOf(Platinum, Color(0xFFB0B0B0))
        com.mlbb.scrim.data.model.RankTier.DIAMOND -> listOf(Diamond, Cyan)
        com.mlbb.scrim.data.model.RankTier.MASTER -> PurpleGradient
        com.mlbb.scrim.data.model.RankTier.GRANDMASTER -> listOf(GoldPrimary, ErrorRed)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 6.dp,
                spotColor = tierColor.copy(alpha = 0.2f),
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = tierColor,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${tier.displayName} Tier",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = tierColor
                        )
                    )
                }
                Text(
                    text = "$xp / ${tier.maxXp + 1} XP",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 12.sp,
                        color = MidGray
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // XP Progress Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .background(
                        color = DarkSurface,
                        shape = RoundedCornerShape(4.dp)
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress.coerceIn(0.05f, 1f))
                        .height(8.dp)
                        .background(
                            brush = Brush.horizontalGradient(colors = tierGradient),
                            shape = RoundedCornerShape(4.dp)
                        )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (xpToNext > 0) "$xpToNext XP to $nextTier" else "Max tier reached!",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 12.sp,
                    color = MidGray
                )
            )
        }
    }
}

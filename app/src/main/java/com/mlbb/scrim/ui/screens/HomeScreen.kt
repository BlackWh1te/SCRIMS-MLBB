package com.mlbb.scrim.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar
import com.mlbb.scrim.ui.theme.*
import com.mlbb.scrim.ui.components.AnimatedEntrance
import com.mlbb.scrim.ui.components.PremiumGlassCard

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
    onNavigateToScrimDetail: (String) -> Unit = {},
    scrims: List<com.mlbb.scrim.data.model.Scrim> = emptyList(),
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
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
        ) {
            // ── Header ──────────────────────────────────────────
            AnimatedEntrance(delayMillis = 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(top = 20.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                        val greeting = when {
                            hour < 6  -> "Late night,"
                            hour < 12 -> "Good morning,"
                            hour < 17 -> "Good afternoon,"
                            else      -> "Good evening,"
                        }
                        Text(
                            text  = greeting,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color         = TextSecondary,
                                letterSpacing = 0.sp
                            )
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text  = userProfile?.username ?: "Player",
                            style = iOSTitle1.copy(color = TextPrimary)
                        )
                    }

                    // Notification button
                    Box {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(SurfaceOverlay)
                                .border(1.dp, GlassBorder, RoundedCornerShape(14.dp))
                                .clickable { onNavigateToNotifications() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector        = Icons.Default.Notifications,
                                contentDescription = "Notifications",
                                tint               = if (notificationCount > 0) GoldPrimary else TextSecondary,
                                modifier           = Modifier.size(22.dp)
                            )
                        }
                        if (notificationCount > 0) {
                            Badge(
                                modifier       = Modifier.align(Alignment.TopEnd).offset(x = 4.dp, y = (-4).dp),
                                containerColor = ErrorRed,
                                contentColor   = White
                            ) {
                                Text(
                                    notificationCount.coerceAtMost(9).toString(),
                                    fontSize   = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Stat Strip ─────────────────────────────────────
            AnimatedEntrance(delayMillis = 80) {
                Row(
                    modifier            = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HomeStatCard(
                        icon     = Icons.Default.SportsEsports,
                        label    = "Matches",
                        value    = (userProfile?.totalMatches ?: 0).toString(),
                        gradient = BlueGradient,
                        modifier = Modifier.weight(1f)
                    )
                    HomeStatCard(
                        icon     = Icons.Default.EmojiEvents,
                        label    = "Wins",
                        value    = (userProfile?.wins ?: 0).toString(),
                        gradient = SuccessGradient,
                        modifier = Modifier.weight(1f)
                    )
                    HomeStatCard(
                        icon     = Icons.Default.TrendingUp,
                        label    = "Win Rate",
                        value    = userProfile?.winRate ?: "—",
                        gradient = GoldGradient,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── Upcoming Scrims ─────────────────────────────────
            val upcomingScrims = scrims.filter {
                it.status == com.mlbb.scrim.data.model.ScrimStatus.OPEN ||
                it.status == com.mlbb.scrim.data.model.ScrimStatus.FILLED
            }.sortedBy { it.scheduledTime }.take(5)

            if (upcomingScrims.isNotEmpty()) {
                AnimatedEntrance(delayMillis = 140) {
                    Row(
                        modifier              = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(BluePrimary.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.CalendarMonth, null,
                                    tint     = BluePrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "Upcoming Scrims",
                                style = iOSTitle3.copy(color = TextPrimary)
                            )
                        }
                        TextButton(onClick = onNavigateToSchedule) {
                            Text(
                                "See all",
                                style      = iOSCallout.copy(
                                    color      = BluePrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                AnimatedEntrance(delayMillis = 180) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        upcomingScrims.forEach { scrim ->
                            ScrimCarouselCard(
                                scrim   = scrim,
                                onClick = { onNavigateToScrimDetail(scrim.id) }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }

            // ── Quick Actions Label ─────────────────────────────
            AnimatedEntrance(delayMillis = 200) {
                Text(
                    "Quick Actions",
                    style    = iOSCaption1.copy(
                        color         = TextSecondary,
                        fontWeight    = FontWeight.SemiBold,
                        letterSpacing = 0.8.sp
                    ),
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }

            Spacer(Modifier.height(10.dp))

            // ── Hero Action: Post Scrim ─────────────────────────
            AnimatedEntrance(delayMillis = 220) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(Color(0xFF1565C0), BluePrimary, Color(0xFF7C4DFF)),
                                start  = Offset(0f, 0f),
                                end    = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                            )
                        )
                        .clickable { onNavigateToCreateScrim() }
                        .padding(horizontal = 20.dp, vertical = 18.dp)
                ) {
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(White.copy(alpha = 0.20f), RoundedCornerShape(14.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Add, "Post Scrim", tint = White, modifier = Modifier.size(26.dp))
                            }
                            Column {
                                Text(
                                    "Post a Scrim",
                                    style = iOSTitle3.copy(color = White)
                                )
                                Text(
                                    "List your team for a match",
                                    style = iOSCaption1.copy(color = White.copy(alpha = 0.75f))
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .background(White.copy(alpha = 0.18f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward, null,
                                tint     = White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // ── 2-Col Action Grid ───────────────────────────────
            AnimatedEntrance(delayMillis = 260) {
                Row(
                    modifier              = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HomeActionCard(
                        icon     = Icons.Default.EmojiEvents,
                        title    = "Leaderboard",
                        subtitle = "Top players",
                        gradient = GoldGradient,
                        onClick  = onNavigateToLeaderboard,
                        modifier = Modifier.weight(1f)
                    )
                    HomeActionCard(
                        icon     = Icons.Default.History,
                        title    = "History",
                        subtitle = "Past matches",
                        gradient = SuccessGradient,
                        onClick  = onNavigateToMatchHistory,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            AnimatedEntrance(delayMillis = 300) {
                Row(
                    modifier              = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HomeActionCard(
                        icon     = Icons.Default.CalendarMonth,
                        title    = "Schedule",
                        subtitle = "Upcoming",
                        gradient = BlueGradient,
                        onClick  = onNavigateToSchedule,
                        modifier = Modifier.weight(1f)
                    )
                    HomeActionCard(
                        icon     = Icons.Default.GroupAdd,
                        title    = "Create Team",
                        subtitle = "Build squad",
                        gradient = listOf(Color(0xFFFF9800), Color(0xFFFF6D00)),
                        onClick  = onNavigateToCreateTeam,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── XP Progress Card ────────────────────────────────
            AnimatedEntrance(delayMillis = 340) {
                XpProgressCard(
                    userProfile = userProfile,
                    modifier    = Modifier.padding(horizontal = 20.dp)
                )
            }

            // Bottom padding for nav bar
            Spacer(Modifier.height(96.dp))
        }
    }
}

// ── Stat Card ───────────────────────────────────────────────

@Composable
private fun HomeStatCard(
    icon    : ImageVector,
    label   : String,
    value   : String,
    gradient: List<Color>,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceCard)
            .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
            .padding(vertical = 14.dp, horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(Brush.linearGradient(gradient), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = White, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text(
                value,
                style = PremiumStatsM.copy(fontSize = 18.sp, color = TextPrimary)
            )
            Spacer(Modifier.height(2.dp))
            Text(
                label,
                style = iOSCaption2.copy(color = TextSecondary)
            )
        }
    }
}

// ── Action Card ─────────────────────────────────────────────

@Composable
private fun HomeActionCard(
    icon    : ImageVector,
    title   : String,
    subtitle: String,
    gradient: List<Color>,
    modifier: Modifier = Modifier,
    onClick : () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceCard)
            .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Brush.linearGradient(gradient), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = White, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(title,    style = iOSHeadline.copy(color = TextPrimary))
                Text(subtitle, style = iOSCaption1.copy(color = TextSecondary))
            }
        }
    }
}

// ── XP Progress Card ────────────────────────────────────────

@Composable
private fun XpProgressCard(
    userProfile: com.mlbb.scrim.data.model.UserProfile?,
    modifier   : Modifier = Modifier
) {
    val tier      = userProfile?.currentTier ?: com.mlbb.scrim.data.model.RankTier.BRONZE
    val xp        = userProfile?.xp ?: 0
    val progress  = userProfile?.xpProgress ?: 0f
    val xpToNext  = userProfile?.xpToNext ?: 0
    val nextTier  = userProfile?.nextTierName ?: "Silver"
    val tierColor = tier.tierColor
    val tierGrad  = tier.badgeGradient

    // Animate progress bar
    val animatedProgress by animateFloatAsState(
        targetValue   = progress.coerceIn(0.04f, 1f),
        animationSpec = tween(800, easing = AppEaseOutCubic),
        label         = "xpProgress"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceCard)
            .border(1.dp, tierColor.copy(alpha = 0.25f), RoundedCornerShape(20.dp))
            .padding(18.dp)
    ) {
        Column {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                Brush.linearGradient(tierGrad),
                                RoundedCornerShape(10.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.EmojiEvents, null,
                            tint     = White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            "${tier.displayName} Tier",
                            style = iOSHeadline.copy(color = tierColor)
                        )
                        Text(
                            "$xp XP total",
                            style = iOSCaption2.copy(color = TextSecondary)
                        )
                    }
                }
                Text(
                    "${(progress * 100).toInt()}%",
                    style = PremiumStatsM.copy(fontSize = 16.sp, color = tierColor)
                )
            }

            Spacer(Modifier.height(14.dp))

            // Track
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(SurfaceOverlay)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(4.dp))
                        .background(Brush.horizontalGradient(tierGrad))
                )
            }

            Spacer(Modifier.height(8.dp))

            Text(
                if (xpToNext > 0) "$xpToNext XP to $nextTier" else "🏆 Max tier reached!",
                style = iOSCaption1.copy(color = TextSecondary)
            )
        }
    }
}

// ── Scrim Carousel Card ─────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScrimCarouselCard(
    scrim  : com.mlbb.scrim.data.model.Scrim,
    onClick: () -> Unit
) {
    val statusColor = when (scrim.status) {
        com.mlbb.scrim.data.model.ScrimStatus.OPEN   -> SuccessGreen
        com.mlbb.scrim.data.model.ScrimStatus.FILLED -> WarningOrange
        else                                          -> MidGray
    }

    Box(
        modifier = Modifier
            .width(180.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceCard)
            .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(14.dp)
    ) {
        Column {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    scrim.teamName,
                    style    = iOSHeadline.copy(color = TextPrimary),
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(statusColor, CircleShape)
                )
            }

            Spacer(Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.SportsEsports, null, tint = TextSecondary, modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                    scrim.gameMode.name.lowercase().replaceFirstChar { it.uppercase() },
                    style = iOSCaption1.copy(color = TextSecondary)
                )
            }

            Spacer(Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.People, null, tint = TextSecondary, modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                    "${scrim.currentPlayers}/${scrim.maxPlayers}",
                    style = iOSCaption1.copy(color = TextSecondary)
                )
            }

            Spacer(Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .background(BluePrimary.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    scrim.region.name,
                    style = iOSCaption2.copy(color = BluePrimary, fontWeight = FontWeight.SemiBold)
                )
            }
        }
    }
}

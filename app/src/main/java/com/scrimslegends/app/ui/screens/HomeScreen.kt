package com.scrimslegends.app.ui.screens

import androidx.compose.material3.MaterialTheme
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Chat
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar
import com.scrimslegends.app.R
import androidx.compose.ui.res.stringResource
import com.scrimslegends.app.ui.theme.*
import com.scrimslegends.app.ui.components.AnimatedEntrance
import com.scrimslegends.app.ui.components.LivePulseDot
import com.scrimslegends.app.ui.components.PremiumGlassCard
import com.scrimslegends.app.ui.components.PullToRefreshContainer
import com.scrimslegends.app.ui.components.RankBadge
import com.scrimslegends.app.ui.components.RankBadgeSize
import com.scrimslegends.app.ui.components.ScrimCountdown

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    userProfile: com.scrimslegends.app.data.model.UserProfile?,
    onNavigateToCreateTeam: () -> Unit = {},
    onNavigateToCreateScrim: () -> Unit = {},
    onNavigateToLeaderboard: () -> Unit = {},
    onNavigateToMatchHistory: () -> Unit = {},
    onNavigateToSchedule: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToScrimDetail: (String) -> Unit = {},
    onNavigateToTeamDetail: (String) -> Unit = {},
    onNavigateToTournamentList: () -> Unit = {},
    onNavigateToJoinTeam: () -> Unit = {},
    onJoinDiscord: () -> Unit = {},
    scrims: List<com.scrimslegends.app.data.model.Scrim> = emptyList(),
    teams: List<com.scrimslegends.app.data.model.Team> = emptyList(),
    notificationCount: Int = 0,
    isRefreshing: Boolean = false,
    isTournamentHost: Boolean = false,
    onRefresh: () -> Unit = {}
) {
    PullToRefreshContainer(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize()
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
                            hour < 6  -> stringResource(R.string.greeting_late_night)
                            hour < 12 -> stringResource(R.string.greeting_good_morning)
                            hour < 17 -> stringResource(R.string.greeting_good_afternoon)
                            else      -> stringResource(R.string.greeting_good_evening)
                        }
                        Text(
                            text  = greeting,
                            style = iOSCallout.copy(
                                color         = appTextSecondaryColor(),
                                letterSpacing = 0.sp
                            )
                        )
                        Spacer(Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text  = userProfile?.username ?: stringResource(R.string.player_default),
                                style = iOSTitle1.copy(color = appTextPrimaryColor())
                            )
                            if (userProfile?.shortId?.isNotBlank() == true) {
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "ID: ${userProfile.shortId}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = MaterialTheme.colorScheme.secondary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                            if (isTournamentHost) {
                                Spacer(Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f))
                                        .border(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                        .clickable { onNavigateToTournamentList() }
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.EmojiEvents,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            text = "Host",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = MaterialTheme.colorScheme.secondary,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Notification button
                    Box {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(appElevatedSurfaceColor())
                                .border(1.dp, appBorderColor(), RoundedCornerShape(14.dp))
                                .clickable { onNavigateToNotifications() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector        = Icons.Default.Notifications,
                                contentDescription = stringResource(R.string.notifications),
                                tint               = if (notificationCount > 0) MaterialTheme.colorScheme.secondary else appTextSecondaryColor(),
                                modifier           = Modifier.size(22.dp)
                            )
                        }
                        if (notificationCount > 0) {
                            Badge(
                                modifier       = Modifier.align(Alignment.TopEnd).offset(x = 4.dp, y = (-4).dp),
                                containerColor = ErrorRed,
                                contentColor = MaterialTheme.colorScheme.onSurface
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

            Spacer(Modifier.height(16.dp))

            // ── Player Rank Hero Card ────────────────────────────
            AnimatedEntrance(delayMillis = 40) {
                PlayerRankCard(
                    userProfile = userProfile,
                    modifier    = Modifier.padding(horizontal = 20.dp)
                )
            }

            Spacer(Modifier.height(14.dp))

            AnimatedEntrance(delayMillis = 40) {
                ScrimIntroCard(
                    hasTeam = teams.isNotEmpty(),
                    onFindScrim = onNavigateToSchedule,
                    onCreateScrim = onNavigateToCreateScrim,
                    onFindTeam = onNavigateToJoinTeam,
                    onCreateTeam = onNavigateToCreateTeam,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }

            Spacer(Modifier.height(32.dp))

            // ── My Teams ────────────────────────────────────────
            if (teams.isNotEmpty()) {
                Box {
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
                                    .size(32.dp)
                                    .background(SuccessGreen.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Groups, null,
                                    tint     = SuccessGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Text(
                                stringResource(R.string.my_teams),
                                style = iOSTitle3.copy(color = appTextPrimaryColor())
                            )
                        }
                        TextButton(onClick = onNavigateToCreateTeam) {
                            Text(
                                "Create Team",
                                style      = iOSCallout.copy(
                                    color      = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                Box {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(teams.take(5)) { team ->
                            TeamHomeCard(
                                team  = team,
                                onClick = { onNavigateToTeamDetail(team.id) }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(32.dp))
            } else {
                Box {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(SuccessGreen.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Groups, null,
                                        tint     = SuccessGreen,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    stringResource(R.string.my_teams),
                                    style = iOSTitle3.copy(color = appTextPrimaryColor())
                                )
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(appSurfaceColor())
                                .border(1.dp, appBorderColor(), RoundedCornerShape(16.dp))
                                .clickable { onNavigateToCreateTeam() }
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.GroupAdd, contentDescription = null, tint = appTextSecondaryColor(), modifier = Modifier.size(32.dp))
                                Spacer(Modifier.height(8.dp))
                                Text(stringResource(R.string.home_no_team_title), style = MaterialTheme.typography.bodyMedium.copy(color = appTextSecondaryColor(), fontWeight = FontWeight.Medium))
                                Spacer(Modifier.height(4.dp))
                                Text(stringResource(R.string.home_no_team_subtitle), style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold))
                            }
                        }
                    }
                }
                Spacer(Modifier.height(32.dp))
            }

            // ── Upcoming Scrims ─────────────────────────────────
            // - Host: user's team created the scrim
            // - Opponent: user's team was accepted (opponentTeamId matches)
            val userTeamIds = teams.map { it.id }.toSet()
            val upcomingScrims = scrims.filter { scrim ->
                val isHost = scrim.teamId in userTeamIds
                val isOpponent = scrim.opponentTeamId in userTeamIds

                if (scrim.status == com.scrimslegends.app.data.model.ScrimStatus.COMPLETED ||
                    scrim.status == com.scrimslegends.app.data.model.ScrimStatus.CANCELLED) {
                    return@filter false
                }

                when {
                    isHost -> scrim.status !in setOf(
                        com.scrimslegends.app.data.model.ScrimStatus.OPEN,
                        com.scrimslegends.app.data.model.ScrimStatus.PENDING
                    )
                    isOpponent -> true
                    else -> false
                }
            }.sortedBy { it.scheduledTime }.take(5)

            if (upcomingScrims.isNotEmpty()) {
                Box {
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
                                    .size(32.dp)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.CalendarMonth, null,
                                    tint     = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Text(
                                stringResource(R.string.upcoming_scrims),
                                style = iOSTitle3.copy(color = appTextPrimaryColor())
                            )
                            Spacer(Modifier.width(8.dp))
                            LivePulseDot(color = SuccessGreen)
                        }
                        TextButton(onClick = onNavigateToSchedule) {
                            Text(
                                stringResource(R.string.see_all),
                                style      = iOSCallout.copy(
                                    color      = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                Box {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(upcomingScrims) { scrim ->
                            ScrimCarouselCard(
                                scrim   = scrim,
                                onClick = { onNavigateToScrimDetail(scrim.id) }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(32.dp))
            } else {
                Box {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.CalendarMonth, null,
                                        tint     = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(Modifier.width(10.dp))
                            Text(
                                stringResource(R.string.upcoming_scrims),
                                style = iOSTitle3.copy(color = appTextPrimaryColor())
                            )
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(appSurfaceColor())
                                .border(1.dp, appBorderColor(), RoundedCornerShape(16.dp))
                                .clickable { onNavigateToSchedule() }
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.EventBusy, contentDescription = null, tint = appTextSecondaryColor(), modifier = Modifier.size(32.dp))
                                Spacer(Modifier.height(8.dp))
                                Text(stringResource(R.string.home_no_scrims_title), style = MaterialTheme.typography.bodyMedium.copy(color = appTextSecondaryColor(), fontWeight = FontWeight.Medium))
                                Spacer(Modifier.height(4.dp))
                                Text(stringResource(R.string.home_no_scrims_subtitle), style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold))
                            }
                        }
                    }
                }
                Spacer(Modifier.height(32.dp))
            }

            // ── Quick Actions Label ─────────────────────────────
            Box {
                HowScrimsWorkCard(
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }

            Spacer(Modifier.height(32.dp))

            Box {
                Text(
                    stringResource(R.string.quick_actions),
                    style    = iOSCaption1.copy(
                        color         = appTextSecondaryColor(),
                        fontWeight    = FontWeight.SemiBold,
                        letterSpacing = 0.8.sp
                    ),
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }

            Spacer(Modifier.height(16.dp))

            // ── Hero Action: Post Scrim ─────────────────────────
            AnimatedEntrance(delayMillis = 80) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(color = MaterialTheme.colorScheme.primary)
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
                                    .background(Color.White.copy(alpha = 0.20f), RoundedCornerShape(14.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Add, stringResource(R.string.post_scrim), tint = Color.White, modifier = Modifier.size(26.dp))
                            }
                            Column {
                                Text(
                                    stringResource(R.string.post_scrim),
                                    style = iOSTitle3.copy(color = Color.White)
                                )
                                Text(
                                    stringResource(R.string.post_scrim_sub),
                                    style = iOSCaption1.copy(color = Color.White.copy(alpha = 0.75f))
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .background(Color.White.copy(alpha = 0.18f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward, null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── 2-Col Action Grid ───────────────────────────────
            AnimatedEntrance(delayMillis = 80) {
                Row(
                    modifier              = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    HomeActionCard(
                        icon     = Icons.Default.EmojiEvents,
                        title    = stringResource(R.string.leaderboard),
                        subtitle = stringResource(R.string.leaderboard_sub),
                        color    = MaterialTheme.colorScheme.secondary,
                        onClick  = onNavigateToLeaderboard,
                        modifier = Modifier.weight(1f)
                    )
                    HomeActionCard(
                        icon     = Icons.Default.History,
                        title    = stringResource(R.string.match_history),
                        subtitle = stringResource(R.string.match_history_sub),
                        color    = SuccessGreen,
                        onClick  = onNavigateToMatchHistory,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Box {
                Row(
                    modifier              = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HomeActionCard(
                        icon     = Icons.Default.CalendarMonth,
                        title    = stringResource(R.string.schedule),
                        subtitle = stringResource(R.string.schedule_sub),
                        color    = MaterialTheme.colorScheme.primary,
                        onClick  = onNavigateToSchedule,
                        modifier = Modifier.weight(1f)
                    )
                    HomeActionCard(
                        icon     = Icons.Default.GroupAdd,
                        title    = stringResource(R.string.create_team),
                        subtitle = stringResource(R.string.create_team_sub),
                        color    = MaterialTheme.colorScheme.secondary,
                        onClick  = onNavigateToCreateTeam,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Box {
                Row(
                    modifier              = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HomeActionCard(
                        icon     = Icons.Default.Search,
                        title    = stringResource(R.string.join_team),
                        subtitle = stringResource(R.string.join_team_sub),
                        color    = PurplePrimary,
                        onClick  = onNavigateToJoinTeam,
                        modifier = Modifier.weight(1f)
                    )
                    HomeActionCard(
                        icon     = Icons.AutoMirrored.Filled.Chat,
                        title    = stringResource(R.string.join_discord),
                        subtitle = stringResource(R.string.join_discord_sub),
                        color    = Color(0xFF5865F2),
                        onClick  = onJoinDiscord,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Bottom padding for nav bar
            Spacer(Modifier.height(96.dp))
        }
    }
}
}

// ── Stat Card ───────────────────────────────────────────────

@Composable
private fun ScrimIntroCard(
    hasTeam: Boolean,
    onFindScrim: () -> Unit,
    onCreateScrim: () -> Unit,
    onFindTeam: () -> Unit,
    onCreateTeam: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showHelp by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(appSurfaceColor())
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.22f), RoundedCornerShape(22.dp))
            .padding(18.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(R.string.home_scrims_title),
                            style = iOSTitle2.copy(color = appTextPrimaryColor())
                        )
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.HelpOutline,
                            contentDescription = stringResource(R.string.what_is_scrim_title),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(20.dp)
                                .clickable { showHelp = true }
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.home_scrims_subtitle),
                        style = iOSBody.copy(color = appTextSecondaryColor())
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ScrimIntroButton(
                    text = stringResource(R.string.find_scrim_action),
                    icon = Icons.Default.Search,
                    color = MaterialTheme.colorScheme.primary,
                    onClick = onFindScrim,
                    modifier = Modifier.weight(1f)
                )
                ScrimIntroButton(
                    text = stringResource(R.string.create_scrim_action),
                    icon = Icons.Default.Add,
                    color = MaterialTheme.colorScheme.secondary,
                    onClick = onCreateScrim,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ScrimIntroButton(
                    text = if (hasTeam) stringResource(R.string.find_team_action) else stringResource(R.string.start_find_team_action),
                    icon = Icons.Default.Groups,
                    color = SuccessGreen,
                    onClick = onFindTeam,
                    modifier = Modifier.weight(1f)
                )
                ScrimIntroButton(
                    text = stringResource(R.string.create_team_action),
                    icon = Icons.Default.GroupAdd,
                    color = WarningOrange,
                    onClick = onCreateTeam,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    if (showHelp) {
        AlertDialog(
            onDismissRequest = { showHelp = false },
            containerColor = appSurfaceColor(),
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    text = stringResource(R.string.what_is_scrim_title),
                    color = appTextPrimaryColor(),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.what_is_scrim_body),
                    color = appTextSecondaryColor(),
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                TextButton(onClick = { showHelp = false }) {
                    Text(stringResource(R.string.ok), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
private fun ScrimIntroButton(
    text: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(color.copy(alpha = 0.13f))
            .border(1.dp, color.copy(alpha = 0.30f), RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                text = text,
                style = iOSCaption1.copy(
                    color = appTextPrimaryColor(),
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun HowScrimsWorkCard(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(appElevatedSurfaceColor())
            .border(1.dp, appBorderColor(), RoundedCornerShape(20.dp))
            .padding(horizontal = 16.dp, vertical = 18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(Modifier.width(10.dp))
            Text(
                text = stringResource(R.string.how_scrims_work_title),
                style = iOSTitle3.copy(color = appTextPrimaryColor())
            )
        }
        Spacer(Modifier.height(14.dp))
        HowScrimsWorkStep("1", stringResource(R.string.how_scrims_work_step_team))
        Spacer(Modifier.height(10.dp))
        HowScrimsWorkStep("2", stringResource(R.string.how_scrims_work_step_apply))
        Spacer(Modifier.height(10.dp))
        HowScrimsWorkStep("3", stringResource(R.string.how_scrims_work_step_results))
    }
}

@Composable
private fun HowScrimsWorkStep(
    number: String,
    text: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f))
                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                number,
                color = MaterialTheme.colorScheme.primary,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = text,
            style = iOSCallout.copy(
                color = appTextSecondaryColor(),
                fontWeight = FontWeight.Medium
            )
        )
    }
}

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
            .background(appSurfaceColor())
            .border(1.dp, appBorderColor(), RoundedCornerShape(16.dp))
            .padding(vertical = 16.dp, horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(gradient.firstOrNull()?.copy(alpha = 0.14f) ?: Color.Transparent)
                    .border(1.dp, gradient.firstOrNull()?.copy(alpha = 0.22f) ?: Color.Transparent, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = gradient.firstOrNull() ?: MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text(
                value,
                style = PremiumStatsM.copy(fontSize = 18.sp, color = appTextPrimaryColor()),
                maxLines = 1
            )
            Spacer(Modifier.height(2.dp))
            Text(
                label,
                style = iOSCaption2.copy(color = appTextSecondaryColor()),
                maxLines = 1
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
    color   : Color,
    modifier: Modifier = Modifier,
    onClick : () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "actionCardScale"
    )

    Box(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(20.dp))
            .background(appSurfaceColor())
            .border(1.dp, appBorderColor(), RoundedCornerShape(20.dp))
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
    ) {
        // Colored top accent bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(color, color.copy(alpha = 0.40f))
                    ),
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                )
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(color.copy(alpha = 0.14f), RoundedCornerShape(13.dp))
                    .border(1.dp, color.copy(alpha = 0.22f), RoundedCornerShape(13.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title,    style = iOSHeadline.copy(color = appTextPrimaryColor()), maxLines = 1)
                Text(subtitle, style = iOSCaption1.copy(color = appTextSecondaryColor()), maxLines = 1)
            }
        }
    }
}

// ── Player Rank Hero Card ────────────────────────────────────

@Composable
private fun PlayerRankCard(
    userProfile: com.scrimslegends.app.data.model.UserProfile?,
    modifier   : Modifier = Modifier
) {
    val tier          = userProfile?.currentTier ?: com.scrimslegends.app.data.model.RankTier.WARRIOR
    val tierColor     = tier.tierColor
    val tierGrad      = tier.badgeGradient
    val xp            = userProfile?.xp ?: 0
    val progress      = userProfile?.xpProgress ?: 0f
    val xpToNext      = userProfile?.xpToNext ?: 0
    val nextTierName  = userProfile?.nextTierName ?: "Silver"

    val animatedProgress by animateFloatAsState(
        targetValue   = progress.coerceIn(0.03f, 1f),
        animationSpec = tween(900, delayMillis = 300, easing = AppEaseOutCubic),
        label         = "rankCardXP"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        tierColor.copy(alpha = 0.18f),
                        appSurfaceColor(),
                        appSurfaceColor().copy(alpha = 0.95f)
                    )
                )
            )
            .border(1.dp, tierColor.copy(alpha = 0.30f), RoundedCornerShape(22.dp))
            .padding(18.dp)
    ) {
        Column {
            // ── Rank badge + tier info + win rate ──
            Row(
                modifier          = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RankBadge(
                    tier     = tier,
                    size     = RankBadgeSize.LARGE,
                    modifier = Modifier.size(52.dp)
                )
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text  = tier.displayName,
                        style = iOSTitle3.copy(color = tierColor)
                    )
                    Text(
                        text  = if (xpToNext > 0)
                            "$xp XP  •  $xpToNext to $nextTierName"
                        else
                            "$xp XP  •  Max Tier!",
                        style = iOSCaption1.copy(color = appTextSecondaryColor())
                    )
                }
                // Win-rate pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(tierColor.copy(alpha = 0.14f))
                        .border(1.dp, tierColor.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text  = userProfile?.winRate ?: "0%",
                            style = PremiumStatsM.copy(fontSize = 17.sp, color = tierColor)
                        )
                        Text(
                            text  = "WIN RATE",
                            style = iOSCaption2.copy(
                                color         = appTextSecondaryColor(),
                                letterSpacing = 0.5.sp
                            )
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── XP Progress bar ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f))
                    .border(
                        width = 1.dp,
                        color = appBorderColor().copy(alpha = 0.4f),
                        shape = RoundedCornerShape(5.dp)
                    )
            ) {
                // Fill
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(5.dp))
                        .background(Brush.horizontalGradient(tierGrad))
                )
                // Shimmer shine overlay
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(5.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.White.copy(alpha = 0.3f),
                                    Color.Transparent
                                )
                            )
                        )
                )
            }

            Spacer(Modifier.height(16.dp))

            // ── Stats row ──
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HeroStatPill(
                    label    = "Matches",
                    value    = (userProfile?.totalMatches ?: 0).toString(),
                    color    = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                HeroStatPill(
                    label    = "Wins",
                    value    = (userProfile?.wins ?: 0).toString(),
                    color    = SuccessGreen,
                    modifier = Modifier.weight(1f)
                )
                HeroStatPill(
                    label    = "Losses",
                    value    = (userProfile?.losses ?: 0).toString(),
                    color    = ErrorRed,
                    modifier = Modifier.weight(1f)
                )
                HeroStatPill(
                    label    = "Points",
                    value    = userProfile?.ptsDisplay ?: "0",
                    color    = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun HeroStatPill(
    label   : String,
    value   : String,
    color   : Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier         = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(color.copy(alpha = 0.11f))
            .border(
                width = 1.dp,
                color = color.copy(alpha = 0.22f),
                shape = RoundedCornerShape(14.dp)
            )
            .padding(vertical = 11.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text     = value,
                style    = PremiumStatsM.copy(fontSize = 17.sp, color = color),
                maxLines = 1
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text     = label,
                style    = iOSCaption2.copy(
                    color = appTextSecondaryColor(),
                    fontWeight = FontWeight.Medium
                ),
                maxLines = 1
            )
        }
    }
}

// ── XP Progress Card ────────────────────────────────────────

@Composable
private fun XpProgressCard(
    userProfile: com.scrimslegends.app.data.model.UserProfile?,
    modifier   : Modifier = Modifier
) {
    val tier      = userProfile?.currentTier ?: com.scrimslegends.app.data.model.RankTier.WARRIOR
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
            .background(appSurfaceColor())
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
                    com.scrimslegends.app.ui.components.RankBadge(
                        tier = tier,
                        size = com.scrimslegends.app.ui.components.RankBadgeSize.MEDIUM,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            "${tier.displayName} Tier",
                            style = iOSHeadline.copy(color = tierColor)
                        )
                        Text(
                            "$xp XP total",
                            style = iOSCaption2.copy(color = appTextSecondaryColor())
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
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(5.dp))
                        .background(Brush.horizontalGradient(tierGrad))
                )
                // Shimmer shine overlay
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(5.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.White.copy(alpha = 0.3f),
                                    Color.Transparent
                                )
                            )
                        )
                )
            }

            Spacer(Modifier.height(8.dp))

            Text(
                if (xpToNext > 0) "$xpToNext XP to $nextTier" else "🏆 Max tier reached!",
                style = iOSCaption1.copy(color = appTextSecondaryColor())
            )
        }
    }
}

// ── Scrim Carousel Card ─────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScrimCarouselCard(
    scrim  : com.scrimslegends.app.data.model.Scrim,
    onClick: () -> Unit
) {
    val statusColor = when (scrim.status) {
        com.scrimslegends.app.data.model.ScrimStatus.OPEN   -> SuccessGreen
        com.scrimslegends.app.data.model.ScrimStatus.FILLED -> WarningOrange
        else                                          -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val interactionSource2 = remember { MutableInteractionSource() }
    val isPressed2 by interactionSource2.collectIsPressedAsState()
    val carouselScale by animateFloatAsState(
        targetValue = if (isPressed2) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "carouselCardScale"
    )

    Box(
        modifier = Modifier
            .graphicsLayer { scaleX = carouselScale; scaleY = carouselScale }
            .width(220.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(appSurfaceColor())
            .border(1.dp, statusColor.copy(alpha = 0.28f), RoundedCornerShape(20.dp))
            .clickable(interactionSource = interactionSource2, indication = null) { onClick() }
            .padding(14.dp)
    ) {
        Column {
            // Status bar at top
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(statusColor, CircleShape)
                    )
                    Text(
                        text  = scrim.status.name.replace("_", " "),
                        style = iOSCaption2.copy(color = statusColor, fontWeight = FontWeight.SemiBold)
                    )
                }
                // Region tag
                Box(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    Text(
                        scrim.region.name,
                        style = iOSCaption2.copy(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Text(
                scrim.teamName,
                style    = iOSHeadline.copy(color = appTextPrimaryColor()),
                maxLines = 1
            )

            Spacer(Modifier.height(4.dp))

            // Live countdown to scrim start
            ScrimCountdown(
                targetTime = scrim.scheduledTime,
                style = iOSCaption1,
                baseColor = appTextSecondaryColor()
            )

            Spacer(Modifier.height(4.dp))

            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.SportsEsports, null, tint = appTextSecondaryColor(), modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(scrim.gameMode.displayName, style = iOSCaption1.copy(color = appTextSecondaryColor()))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.People, null, tint = statusColor.copy(alpha = 0.8f), modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(3.dp))
                    Text(
                        "${scrim.currentPlayers}/${scrim.maxPlayers}",
                        style = iOSCaption1.copy(color = statusColor, fontWeight = FontWeight.SemiBold)
                    )
                }
            }
        }
    }
}

// ── Team Home Card ─────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TeamHomeCard(
    team  : com.scrimslegends.app.data.model.Team,
    onClick: () -> Unit
) {
    val isFull = team.players.size >= team.maxPlayers
    val statusColor = if (isFull) SuccessGreen else MaterialTheme.colorScheme.primary
    val statusText  = if (isFull) "Full" else "Recruiting"

    val teamInteraction = remember { MutableInteractionSource() }
    val isTeamPressed by teamInteraction.collectIsPressedAsState()
    val teamCardScale by animateFloatAsState(
        targetValue = if (isTeamPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "teamCardScale"
    )

    Box(
        modifier = Modifier
            .graphicsLayer { scaleX = teamCardScale; scaleY = teamCardScale }
            .width(220.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(appSurfaceColor())
            .border(1.dp, statusColor.copy(alpha = 0.25f), RoundedCornerShape(20.dp))
            .clickable(interactionSource = teamInteraction, indication = null) { onClick() }
            .padding(14.dp)
    ) {
        Column {
            // Team avatar + status badge
            Row(
                modifier          = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Team avatar circle with initials
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(statusColor.copy(alpha = 0.20f))
                        .border(1.dp, statusColor.copy(alpha = 0.30f), RoundedCornerShape(13.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text  = team.name.take(2).uppercase(),
                        style = iOSHeadline.copy(color = statusColor, fontWeight = FontWeight.ExtraBold)
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        team.name,
                        style    = iOSHeadline.copy(color = appTextPrimaryColor()),
                        maxLines = 1
                    )
                    Box(
                        modifier = Modifier
                            .background(statusColor.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            statusText,
                            style = iOSCaption2.copy(color = statusColor, fontWeight = FontWeight.SemiBold)
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Player count progress
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Groups, null, tint = statusColor.copy(alpha = 0.8f), modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "${team.players.size}/${team.maxPlayers}",
                        style = iOSCaption1.copy(color = appTextSecondaryColor())
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(3.dp))
                    Text(
                        "${team.reputation}",
                        style = iOSCaption1.copy(color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.SemiBold)
                    )
                }
            }
        }
    }
}

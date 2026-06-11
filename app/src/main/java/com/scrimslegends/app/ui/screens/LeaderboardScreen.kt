package com.scrimslegends.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import com.scrimslegends.app.data.model.LeaderboardEntry
import com.scrimslegends.app.data.model.RankTier
import com.scrimslegends.app.data.model.TeamLeaderboardEntry
import com.scrimslegends.app.R
import androidx.compose.ui.res.stringResource
import com.scrimslegends.app.ui.theme.*
import com.scrimslegends.app.ui.components.AnimatedEntrance
import com.scrimslegends.app.ui.components.GlassBackButton
import com.scrimslegends.app.ui.components.RankBadge
import com.scrimslegends.app.ui.components.RankBadgeSize
import com.scrimslegends.app.ui.components.PullToRefreshContainer
import com.scrimslegends.app.ui.components.ReportDialog
import com.scrimslegends.app.ui.components.LottieLoadingIndicator
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale

private enum class LeaderboardMode {
    PLAYERS,
    TEAMS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(
    entries: List<LeaderboardEntry>,
    teamEntries: List<TeamLeaderboardEntry> = emptyList(),
    isLoading: Boolean,
    isRefreshing: Boolean = false,
    error: String?,
    selectedTier: RankTier? = null,
    onTierFilter: (RankTier?) -> Unit = {},
    onNavigateBack: () -> Unit,
    onRefresh: () -> Unit = {},
    onDismissError: () -> Unit = {},
    onTeamSelected: (String) -> Unit = {},
    onReportUser: (userId: String, username: String, avatarUrl: String?) -> Unit = { _, _, _ -> }
) {
    var reportTarget by remember { mutableStateOf<Triple<String, String, String?>?>(null) }
    var selectedMode by remember { mutableStateOf(LeaderboardMode.PLAYERS) }
    val isTeamsMode = selectedMode == LeaderboardMode.TEAMS
    val appTextPrimary = appTextPrimaryColor()
    val appTextSecondary = appTextSecondaryColor()
    val appElevatedSurface = appElevatedSurfaceColor()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = heroGradientBrush())
            .statusBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            AnimatedEntrance(delayMillis = 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, top = 20.dp, end = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GlassBackButton(onClick = onNavigateBack)

                    Text(
                        text = stringResource(R.string.leaderboard),
                        style = iOSTitle2.copy(color = appTextPrimary)
                    )

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
                }
            }

            // Error display
            if (error != null) {
                AnimatedEntrance(delayMillis = 0) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = ErrorRed.copy(alpha = 0.1f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = stringResource(R.string.error),
                                    tint = ErrorRed,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = error,
                                    color = appTextPrimary,
                                    fontSize = 13.sp
                                )
                            }
                            IconButton(
                                onClick = onDismissError,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = stringResource(R.string.dismiss),
                                    tint = appTextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            PullToRefreshContainer(
                isRefreshing = isRefreshing,
                onRefresh = onRefresh,
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    AnimatedEntrance(delayMillis = 75) {
                        LeaderboardModeToggle(
                            selectedMode = selectedMode,
                            onModeSelected = { selectedMode = it }
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    AnimatedEntrance(delayMillis = 100) {
                        if (!isTeamsMode) {
                            ScrollableTierFilter(
                                selectedTier = selectedTier,
                                onTierSelected = onTierFilter
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    when {
                        isLoading && (if (isTeamsMode) teamEntries.isEmpty() else entries.isEmpty()) -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                LottieLoadingIndicator(size = 80.dp)
                            }
                        }
                        (if (isTeamsMode) teamEntries.isEmpty() else entries.isEmpty()) -> {
                            if (error != null) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(40.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ErrorOutline,
                                        contentDescription = null,
                                        tint = ErrorRed.copy(alpha = 0.6f),
                                        modifier = Modifier.size(72.dp)
                                    )
                                    Spacer(modifier = Modifier.height(20.dp))
                                    Text(
                                        text = "Failed to load",
                                        style = iOSTitle3.copy(color = appTextPrimary)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = error,
                                        style = iOSFootnote.copy(color = appTextSecondary),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            } else {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(40.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.EmojiEvents,
                                        contentDescription = null,
                                        tint = appTextSecondary.copy(alpha = 0.55f),
                                        modifier = Modifier.size(72.dp)
                                    )
                                    Spacer(modifier = Modifier.height(20.dp))
                                    Text(
                                        text = if (isTeamsMode)
                                            "No teams on leaderboard yet"
                                        else if (selectedTier != null)
                                            "No ${selectedTier.displayName} players yet"
                                        else
                                            stringResource(R.string.no_entries),
                                        style = iOSTitle3.copy(color = appTextPrimary)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = stringResource(R.string.leaderboard_empty_hint),
                                        style = iOSFootnote.copy(color = appTextSecondary),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                        else -> {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 120.dp)
                            ) {
                                if (isTeamsMode) {
                                    itemsIndexed(teamEntries, key = { _, e -> e.teamId }) { index, teamEntry ->
                                        AnimatedEntrance(delayMillis = 150 + minOf(index, 15) * 60) {
                                            TeamLeaderboardRow(
                                                entry = teamEntry,
                                                isTopThree = teamEntry.rank <= 3,
                                                onClick = { onTeamSelected(teamEntry.teamId) }
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(10.dp))
                                    }
                                } else {
                                    itemsIndexed(entries, key = { _, e -> e.playerId }) { index, entry ->
                                        AnimatedEntrance(delayMillis = 150 + minOf(index, 15) * 60) {
                                            LeaderboardRow(
                                                entry = entry,
                                                isTopThree = entry.rank <= 3,
                                                onReportUser = { userId, username, avatarUrl ->
                                                    reportTarget = Triple(userId, username, avatarUrl)
                                                }
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(10.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (reportTarget != null) {
                ReportDialog(
                    targetName = reportTarget!!.second,
                    reasons = com.scrimslegends.app.ui.components.UserReportReason.values().map { it.label },
                    onDismiss = { reportTarget = null },
                    onSubmit = { reason, description ->
                        onReportUser(reportTarget!!.first, reportTarget!!.second, reportTarget!!.third)
                        reportTarget = null
                    }
                )
            }
        }
    }
}

@Composable
private fun LeaderboardModeToggle(
    selectedMode: LeaderboardMode,
    onModeSelected: (LeaderboardMode) -> Unit
) {
    val appElevatedSurface = appElevatedSurfaceColor()
    val appTextSecondary = appTextSecondaryColor()
    val appBorder = appBorderColor()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(appElevatedSurface)
            .border(1.dp, appBorder, RoundedCornerShape(14.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        LeaderboardMode.values().forEach { mode ->
            val selected = selectedMode == mode
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (selected) BluePrimary else Color.Transparent)
                    .clickable { onModeSelected(mode) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (mode == LeaderboardMode.PLAYERS) Icons.Default.Person else Icons.Default.Groups,
                        contentDescription = null,
                        tint = if (selected) White else appTextSecondary,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (mode == LeaderboardMode.PLAYERS) "Players" else "Teams",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selected) White else appTextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun ScrollableTierFilter(
    selectedTier: RankTier?,
    onTierSelected: (RankTier?) -> Unit
) {
    val tiers = remember { listOf(null) + RankTier.values() }
    val scrollState = rememberScrollState()
    val appElevatedSurface = appElevatedSurfaceColor()
    val appTextSecondary = appTextSecondaryColor()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        tiers.forEach { tier ->
            val isSelected = selectedTier == tier
            val display = tier?.displayName ?: "All"
            val chipColor = when (tier) {
                RankTier.WARRIOR -> Bronze
                RankTier.ELITE -> SolverBlue
                RankTier.MASTER -> GoldRank
                RankTier.GRANDMASTER -> GrandmasterPurple
                RankTier.EPIC -> EpicCyan
                RankTier.LEGEND -> LegendRed
                RankTier.MYTHIC -> MythicCrimson
                RankTier.MYTHICAL_HONOR -> HonorBlue
                RankTier.MYTHICAL_GLORY -> GloryPink
                RankTier.MYTHICAL_IMMORTAL -> ImmortalRed
                null -> BluePrimary
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(9999.dp))
                    .background(
                        color = if (isSelected) chipColor.copy(alpha = 0.25f)
                        else appElevatedSurface
                    )
                    .clickable { onTierSelected(tier) }
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (tier != null) {
                        Box(modifier = Modifier.size(18.dp)) {
                            RankBadge(tier = tier, size = RankBadgeSize.SMALL)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = display,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) chipColor else appTextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun TeamLeaderboardRow(
    entry: TeamLeaderboardEntry,
    isTopThree: Boolean,
    onClick: () -> Unit
) {
    val rankColor = when (entry.rank) {
        1 -> GoldPrimary
        2 -> Silver
        3 -> Bronze
        else -> MidGray
    }
    val appSurface = appSurfaceColor()
    val appElevatedSurface = appElevatedSurfaceColor()
    val appTextPrimary = appTextPrimaryColor()
    val appTextSecondary = appTextSecondaryColor()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (isTopThree) 8.dp else 2.dp,
                spotColor = if (isTopThree) rankColor.copy(alpha = 0.35f) else Color.Black.copy(alpha = 0.05f),
                shape = RoundedCornerShape(16.dp)
            )
            .border(
                width = if (isTopThree) 1.5.dp else 0.dp,
                brush = Brush.linearGradient(
                    colors = if (isTopThree) listOf(rankColor, rankColor.copy(alpha = 0.1f)) else listOf(Color.Transparent, Color.Transparent)
                ),
                shape = RoundedCornerShape(16.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = if (isTopThree) appElevatedSurface else appSurface),
        shape = RoundedCornerShape(16.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(rankColor.copy(alpha = if (isTopThree) 0.14f else 0.08f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (entry.rank <= 3) {
                    Icon(
                        imageVector = if (entry.rank == 1) Icons.Default.EmojiEvents else Icons.Default.Star,
                        contentDescription = null,
                        tint = rankColor,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Text(
                        text = entry.rank.toString(),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = rankColor
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Brush.verticalGradient(listOf(GoldPrimary.copy(alpha = 0.30f), BluePrimary.copy(alpha = 0.22f)))),
                contentAlignment = Alignment.Center
            ) {
                if (!entry.logoUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = entry.logoUrl,
                        contentDescription = entry.teamName,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = entry.teamName.firstOrNull()?.uppercaseChar()?.toString() ?: "T",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = White
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.teamName,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = appTextPrimary,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Groups, contentDescription = null, tint = appTextSecondary, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${entry.memberCount} players",
                        fontSize = 12.sp,
                        color = appTextSecondary
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Icon(Icons.Default.SportsEsports, contentDescription = null, tint = appTextSecondary, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${entry.totalMatches} ranked",
                        fontSize = 12.sp,
                        color = appTextSecondary
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                RankBadge(tier = entry.currentTier, size = RankBadgeSize.MEDIUM)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "${entry.points} PTS",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = GoldPrimary
                )
                Text(
                    text = "Rep ${String.format(java.util.Locale.US, "%.1f", entry.reputation)}",
                    fontSize = 11.sp,
                    color = appTextSecondary
                )
            }
        }
    }
}

@Composable
private fun LeaderboardRow(
    entry: LeaderboardEntry,
    isTopThree: Boolean,
    onReportUser: (userId: String, username: String, avatarUrl: String?) -> Unit = { _, _, _ -> }
) {
    val rankColor = when (entry.rank) {
        1 -> GoldPrimary
        2 -> Silver
        3 -> Bronze
        else -> MidGray
    }
    val appSurface = appSurfaceColor()
    val appElevatedSurface = appElevatedSurfaceColor()
    val appTextPrimary = appTextPrimaryColor()
    val appTextSecondary = appTextSecondaryColor()

    val rankBg = when (entry.rank) {
        1 -> GoldPrimary.copy(alpha = 0.12f)
        2 -> Silver.copy(alpha = 0.12f)
        3 -> Bronze.copy(alpha = 0.12f)
        else -> Color.Transparent
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (isTopThree) 8.dp else 2.dp,
                spotColor = if (isTopThree) rankColor.copy(alpha = 0.4f) else Color.Black.copy(alpha = 0.05f),
                shape = RoundedCornerShape(16.dp)
            )
            .border(
                width = if (isTopThree) 1.5.dp else 0.dp,
                brush = Brush.linearGradient(
                    colors = if (isTopThree) listOf(rankColor, rankColor.copy(alpha = 0.1f)) else listOf(Color.Transparent, Color.Transparent)
                ),
                shape = RoundedCornerShape(16.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isTopThree) appElevatedSurface else appSurface
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rank number
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = rankBg,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (entry.rank <= 3) {
                    Icon(
                        imageVector = when (entry.rank) {
                            1 -> Icons.Default.EmojiEvents
                            else -> Icons.Default.Star
                        },
                        contentDescription = null,
                        tint = rankColor,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Text(
                        text = entry.rank.toString(),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = rankColor
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Avatar
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .shadow(
                        elevation = 4.dp,
                        spotColor = BluePrimary.copy(alpha = 0.2f),
                        shape = CircleShape
                    )
                    .clip(CircleShape)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(BluePrimary.copy(alpha = 0.4f), BluePrimary.copy(alpha = 0.1f))
                        )
                    )
                    .clickable {
                        onReportUser(entry.playerId, entry.username, entry.avatarUrl)
                    },
                contentAlignment = Alignment.Center
            ) {
                if (!entry.avatarUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = entry.avatarUrl,
                        contentDescription = entry.username,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = entry.username.firstOrNull()?.uppercaseChar()?.toString() ?: "P",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = BluePrimary
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Name & team
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.username,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = appTextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = entry.teamName,
                    fontSize = 12.sp,
                    color = appTextSecondary
                )
            }

            // Stats
            Column(horizontalAlignment = Alignment.End) {
                RankBadge(tier = entry.currentTier, size = RankBadgeSize.MEDIUM)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "${entry.xp} PTS",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = GoldPrimary
                )
                val totalGames = entry.wins + entry.losses
                val wr = if (totalGames > 0) (entry.wins * 100f / totalGames) else 0f
                val wrStr = String.format(java.util.Locale.US, "%.1f", wr) + "% WR"
                Text(
                    text = "${entry.wins}W / ${entry.losses}L ($wrStr)",
                    fontSize = 11.sp,
                    color = appTextSecondary
                )
            }
        }
    }
}

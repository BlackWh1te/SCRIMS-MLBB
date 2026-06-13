package com.scrimslegends.app.ui.screens

import androidx.compose.material3.MaterialTheme
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import com.scrimslegends.app.data.model.TeamRating
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scrimslegends.app.R
import androidx.compose.ui.res.stringResource
import com.scrimslegends.app.ui.theme.*
import coil.compose.AsyncImage
import com.scrimslegends.app.ui.components.AnimatedEntrance
import com.scrimslegends.app.ui.components.GlassBackButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import com.scrimslegends.app.data.model.MatchResult
import com.scrimslegends.app.data.model.VerificationStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamDetailScreen(
    team: com.scrimslegends.app.data.model.Team,
    isLeader: Boolean = false,
    currentUserId: String = "",
    teamStats: Map<String, Any> = emptyMap(),
    weeklyWins: Int = 0,
    weeklyLosses: Int = 0,
    teamRatings: List<com.scrimslegends.app.data.model.TeamRating> = emptyList(),
    matchHistory: List<MatchResult> = emptyList(),
    isMatchHistoryLoading: Boolean = false,
    onNavigateBack: () -> Unit,
    onUpdatePlayerRole: ((playerId: String, newRole: com.scrimslegends.app.data.model.PlayerRole) -> Unit)? = null,
    onRemovePlayer: ((playerId: String) -> Unit)? = null,
    onLeaveTeam: (() -> Unit)? = null,
    onDisbandTeam: (() -> Unit)? = null,
    onInvitePlayer: (() -> Unit)? = null,
    onAddPlayer: ((name: String, email: String, role: com.scrimslegends.app.data.model.PlayerRole) -> Unit)? = null,
    applications: List<com.scrimslegends.app.data.model.TeamApplication> = emptyList(),
    onAcceptApplication: ((applicationId: String) -> Unit)? = null,
    onDeclineApplication: ((applicationId: String) -> Unit)? = null,
    onLoadStats: () -> Unit = {},
    onSubmitRating: ((rating: Int, feedback: String) -> Unit)? = null,
    onUpdateLogo: ((android.net.Uri) -> Unit)? = null,
    isUpdatingLogo: Boolean = false,
    onApplyToTeam: ((message: String) -> Unit)? = null,
    onOpenTeamChat: (() -> Unit)? = null,
    onOpenMatchResult: ((MatchResult) -> Unit)? = null
) {
    LaunchedEffect(team.id) {
        onLoadStats()
    }

    // Use matchHistory as source of truth if available, otherwise fallback to teamStats
    val computedTotal = matchHistory.sumOf { it.bestOf }
    val computedWins = matchHistory.sumOf { if (it.confirmedWinnerId == team.id) it.bestOf else 0 }
    val computedLosses = matchHistory.sumOf { if (it.confirmedWinnerId != null && it.confirmedWinnerId != team.id && !it.isDraw) it.bestOf else 0 }

    val totalScrims = if (!isMatchHistoryLoading && matchHistory.isNotEmpty()) computedTotal else ((teamStats["total_scrims"] ?: teamStats["total_matches"]) as? Number)?.toInt() ?: 0
    val wins = if (!isMatchHistoryLoading && matchHistory.isNotEmpty()) computedWins else ((teamStats["wins"] ?: teamStats["win_count"]) as? Number)?.toInt() ?: 0
    val losses = if (!isMatchHistoryLoading && matchHistory.isNotEmpty()) computedLosses else ((teamStats["losses"] ?: teamStats["loss_count"]) as? Number)?.toInt() ?: 0
    val totalPoints = team.players.sumOf { it.pts }
    val matchesPlayed = wins + losses
    val winRate = if (matchesPlayed > 0) "${(wins * 100 / matchesPlayed)}%" else "0%"
    val avgRating = if (teamRatings.isNotEmpty())
        String.format("%.1f", teamRatings.map { it.rating }.average())
    else "—"

    var showLeaveDialog by remember { mutableStateOf(false) }
    var showDisbandDialog by remember { mutableStateOf(false) }
    var showRemoveDialog by remember { mutableStateOf(false) }
    var showInviteDialog by remember { mutableStateOf(false) }
    var showAddPlayerDialog by remember { mutableStateOf(false) }
    var showApplyDialog by remember { mutableStateOf(false) }
    var playerToRemove by remember { mutableStateOf<com.scrimslegends.app.data.model.Player?>(null) }
    var playerToChangeRole by remember { mutableStateOf<com.scrimslegends.app.data.model.Player?>(null) }

    val inviteCode = remember(team.id) {
        "SL-${team.name.take(3).uppercase()}${team.id.takeLast(4).uppercase()}"
    }
    val isTeamMember = isLeader || team.leaderId == currentUserId || team.players.any { it.id == currentUserId }

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = mutableListOf("Overview", "Roster")
    tabs.add("History")
    // Only show Manage tab if there's manage actions or leader
    if (isLeader || onLeaveTeam != null) {
        tabs.add("Manage")
    }
    val safeSelectedTabIndex = selectedTabIndex.coerceIn(0, tabs.lastIndex)
    val selectedTab = tabs[safeSelectedTabIndex]

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = heroGradientBrush()
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // Header
            AnimatedEntrance(delayMillis = 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GlassBackButton(onClick = onNavigateBack)

                    Text(
                        text = stringResource(R.string.team_details),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = appTextPrimaryColor()
                        )
                    )

                    Spacer(modifier = Modifier.width(44.dp))
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 120.dp)
            ) {
                // Team Header Card
                item {
                    AnimatedEntrance(delayMillis = 100) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, appBorderColor(), RoundedCornerShape(20.dp)),
                            colors = CardDefaults.cardColors(
                                containerColor = appSurfaceColor()
                            ),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // Team Avatar with gradient
                                Box(
                                    modifier = Modifier
                                        .size(90.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))
                                        .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.28f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = team.name.firstOrNull()?.uppercaseChar()?.toString() ?: stringResource(R.string.team_initial_fallback),
                                        fontSize = 40.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Team Name
                                Text(
                                    text = team.name,
                                    style = MaterialTheme.typography.headlineLarge.copy(
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = appTextPrimaryColor()
                                    )
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // Player Count
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = stringResource(R.string.content_desc_players),
                                        tint = appTextSecondaryColor(),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = stringResource(R.string.players_count, team.players.size, 7),
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontSize = 15.sp,
                                            color = appTextSecondaryColor()
                                        )
                                    )
                                }

                                if (onApplyToTeam != null && !isTeamMember) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(
                                        onClick = { showApplyDialog = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                    ) {
                                        Text("Apply to Join")
                                    }
                                }

                                if (onOpenTeamChat != null && isTeamMember) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Button(
                                        onClick = onOpenTeamChat,
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Groups,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.background,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = stringResource(R.string.team_chat_default_name),
                                            color = MaterialTheme.colorScheme.background,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }

                // TabRow Section
                item {
                    AnimatedEntrance(delayMillis = 150) {
                        TabRow(
                            selectedTabIndex = safeSelectedTabIndex,
                            containerColor = Color.Transparent,
                            contentColor = appTextPrimaryColor(),
                            indicator = { tabPositions ->
                                TabRowDefaults.Indicator(
                                    Modifier.tabIndicatorOffset(tabPositions[safeSelectedTabIndex]),
                                    color = MaterialTheme.colorScheme.primary,
                                    height = 3.dp
                                )
                            },
                            divider = {
                                Divider(color = appBorderColor())
                            }
                        ) {
                            tabs.forEachIndexed { index, title ->
                                Tab(
                                    selected = safeSelectedTabIndex == index,
                                    onClick = { selectedTabIndex = index },
                                    text = { 
                                        Text(
                                            text = title, 
                                            fontWeight = if (safeSelectedTabIndex == index) FontWeight.Bold else FontWeight.Medium,
                                            color = if (safeSelectedTabIndex == index) MaterialTheme.colorScheme.primary else appTextSecondaryColor()
                                        ) 
                                    }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // ─── OVERVIEW TAB ───
                if (selectedTab == "Overview") {
                    // Team Stats Section (real data from get_team_stats RPC)
                    item {
                    AnimatedEntrance(delayMillis = 175) {
                        Text(
                            text = stringResource(R.string.team_stats),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontSize = 20.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = appTextPrimaryColor()
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    AnimatedEntrance(delayMillis = 185) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                TeamStatBox(
                                    modifier = Modifier.weight(1f),
                                    icon = Icons.Default.SportsEsports,
                                    label = stringResource(R.string.scrims),
                                    value = totalScrims.toString(),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                TeamStatBox(
                                    modifier = Modifier.weight(1f),
                                    icon = Icons.Default.EmojiEvents,
                                    label = stringResource(R.string.wins),
                                    value = wins.toString(),
                                    tint = SuccessGreen
                                )
                                TeamStatBox(
                                    modifier = Modifier.weight(1f),
                                    icon = Icons.Default.TrendingDown,
                                    label = stringResource(R.string.losses),
                                    value = losses.toString(),
                                    tint = ErrorRed
                                )
                            }
                            TeamStatBox(
                                modifier = Modifier.fillMaxWidth(),
                                icon = Icons.Default.TrendingUp,
                                label = stringResource(R.string.win_rate),
                                value = winRate,
                                tint = MaterialTheme.colorScheme.secondary,
                                isLarge = true
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }

                // Squad Stats — Player PTS List
                item {
                    AnimatedEntrance(delayMillis = 190) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = appSurfaceColor()),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp)
                            ) {
                                // Header row: title + total PTS
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Squad Stats",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontSize = 17.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = appTextPrimaryColor()
                                        )
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "${if (totalPoints >= 0) "+" else ""}$totalPoints PTS",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Player list sorted by PTS
                                val sortedPlayers = remember(team.players) {
                                    team.players.sortedByDescending { it.pts }
                                }

                                sortedPlayers.forEachIndexed { index, player ->
                                    val isTopContributor = index == 0 && player.pts > 0
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .then(
                                                if (isTopContributor) Modifier
                                                    .background(
                                                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.06f),
                                                        RoundedCornerShape(12.dp)
                                                    )
                                                else Modifier
                                            )
                                            .padding(horizontal = 8.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Rank number
                                        Box(
                                            modifier = Modifier.width(28.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isTopContributor) {
                                                Icon(
                                                    imageVector = Icons.Default.EmojiEvents,
                                                    contentDescription = "Top",
                                                    tint = MaterialTheme.colorScheme.secondary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            } else {
                                                Text(
                                                    text = "${index + 1}",
                                                    style = MaterialTheme.typography.bodyMedium.copy(
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = appTextSecondaryColor()
                                                    )
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(10.dp))

                                        // Avatar
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (!player.avatarUrl.isNullOrBlank()) {
                                                coil.compose.AsyncImage(
                                                    model = player.avatarUrl,
                                                    contentDescription = player.name,
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .clip(CircleShape),
                                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                                )
                                            } else {
                                                Text(
                                                    text = player.name.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        // Name + Role
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = player.name,
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = appTextPrimaryColor()
                                                ),
                                                maxLines = 1,
                                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = when (player.role) {
                                                    com.scrimslegends.app.data.model.PlayerRole.LEADER -> "Leader"
                                                    com.scrimslegends.app.data.model.PlayerRole.CO_LEADER -> "Co-Leader"
                                                    com.scrimslegends.app.data.model.PlayerRole.MEMBER -> "Member"
                                                },
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontSize = 11.sp,
                                                    color = appTextSecondaryColor()
                                                )
                                            )
                                        }

                                        // PTS
                                        Text(
                                            text = "${if (player.pts >= 0) "+" else ""}${player.pts}",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = if (player.pts >= 0) MaterialTheme.colorScheme.secondary else ErrorRed
                                            )
                                        )
                                    }

                                    // Divider between players (not after last)
                                    if (index < sortedPlayers.lastIndex) {
                                        androidx.compose.material3.HorizontalDivider(
                                            modifier = Modifier.padding(horizontal = 8.dp),
                                            thickness = 0.5.dp,
                                            color = appBorderColor().copy(alpha = 0.3f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // Team Ratings & Feedback
                if (teamRatings.isNotEmpty()) {
                    item {
                        AnimatedEntrance(delayMillis = 200) {
                            Text(
                                text = stringResource(R.string.ratings_and_feedback),
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = appTextPrimaryColor()
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    items(teamRatings.take(5)) { rating ->
                        AnimatedEntrance(delayMillis = 220) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = appSurfaceColor()),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = rating.raterTeamName,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                color = appTextPrimaryColor()
                                            )
                                        )
                                        Row {
                                            repeat(rating.rating) {
                                                Icon(
                                                    imageVector = Icons.Default.Star,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.secondary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                    if (rating.feedback.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = rating.feedback,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = appTextSecondaryColor()
                                            )
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
                } // End Overview Tab

                // ─── ROSTER TAB ───
                if (selectedTab == "Roster") {
                    // Players Section
                item {
                    AnimatedEntrance(delayMillis = 200) {
                        Text(
                            text = stringResource(R.string.players),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontSize = 20.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = appTextPrimaryColor()
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Player List
                itemsIndexed(team.players, key = { _, p -> p.id }) { index, player ->
                    AnimatedEntrance(delayMillis = 250 + index * 60) {
                        PlayerCard(
                            player = player,
                            isLeader = isLeader,
                            onChangeRole = if (isLeader && player.role != com.scrimslegends.app.data.model.PlayerRole.LEADER) {
                                { onUpdatePlayerRole?.invoke(player.id, it) }
                            } else null,
                            onKick = if (isLeader && player.role != com.scrimslegends.app.data.model.PlayerRole.LEADER) {
                                { onRemovePlayer?.invoke(player.id) }
                            } else null
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Add Player Button (if team not full)
                if (team.players.size < 7) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))

                        // Add Player directly
                        AnimatedEntrance(delayMillis = 250 + team.players.size * 60) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, appBorderColor(), RoundedCornerShape(20.dp)),
                                colors = CardDefaults.cardColors(
                                    containerColor = appSurfaceColor()
                                ),
                                shape = RoundedCornerShape(20.dp),
                                onClick = { showAddPlayerDialog = true }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = stringResource(R.string.content_desc_add_player),
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = stringResource(R.string.add_player),
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Invite by Code
                        AnimatedEntrance(delayMillis = 260 + team.players.size * 60) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, appBorderColor(), RoundedCornerShape(20.dp)),
                                colors = CardDefaults.cardColors(
                                    containerColor = appSurfaceColor()
                                ),
                                shape = RoundedCornerShape(20.dp),
                                onClick = { showInviteDialog = true }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = stringResource(R.string.content_desc_invite_code),
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = stringResource(R.string.invite_by_code),
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
                } // End Roster Tab

                // ─── MANAGE TAB ───
                if (selectedTab == "History") {
                    item {
                        AnimatedEntrance(delayMillis = 200) {
                            Text(
                                text = "Ranked match history",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = appTextPrimaryColor()
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    when {
                        isMatchHistoryLoading && matchHistory.isEmpty() -> {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 36.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                        matchHistory.isEmpty() -> {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = appSurfaceColor()),
                                    shape = RoundedCornerShape(20.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.History,
                                            contentDescription = null,
                                            tint = appTextSecondaryColor().copy(alpha = 0.5f),
                                            modifier = Modifier.size(48.dp)
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = "No ranked matches yet",
                                            color = appTextSecondaryColor(),
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Classic scrims are not saved in team history.",
                                            color = appTextSecondaryColor(),
                                            fontSize = 12.sp,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                        else -> {
                            items(matchHistory, key = { it.id }) { match ->
                                AnimatedEntrance(delayMillis = 240) {
                                    TeamHistoryCard(
                                        match = match,
                                        teamId = team.id,
                                        onClick = { onOpenMatchResult?.invoke(match) }
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                            }
                        }
                    }
                }

                if (selectedTab == "Manage") {
                    // Pending Applications (Leader only)
                if (isLeader && applications.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(24.dp))

                        AnimatedEntrance(delayMillis = 380) {
                            Text(
                                text = stringResource(R.string.join_requests_count, applications.size),
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = appTextPrimaryColor()
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        applications.forEachIndexed { index, app ->
                            AnimatedEntrance(delayMillis = 390 + index * 60) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, appBorderColor(), RoundedCornerShape(20.dp)),
                                    colors = CardDefaults.cardColors(
                                        containerColor = appSurfaceColor()
                                    ),
                                    shape = RoundedCornerShape(20.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(44.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        brush = Brush.verticalGradient(
                                                            colors = listOf(
                                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                                            )
                                                        )
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = app.applicantName.firstOrNull()?.uppercaseChar()?.toString() ?: stringResource(R.string.unknown_applicant_initial),
                                                    fontSize = 18.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = app.applicantName,
                                                    style = MaterialTheme.typography.titleMedium.copy(
                                                        fontSize = 16.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = appTextPrimaryColor()
                                                    )
                                                )
                                                if (!app.message.isNullOrBlank()) {
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(
                                                        text = app.message,
                                                        style = MaterialTheme.typography.bodySmall.copy(
                                                            fontSize = 12.sp,
                                                            color = appTextSecondaryColor()
                                                        ),
                                                        maxLines = 2,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            OutlinedButton(
                                                onClick = { onDeclineApplication?.invoke(app.id) },
                                                modifier = Modifier.weight(1f),
                                                colors = ButtonDefaults.outlinedButtonColors(
                                                    contentColor = ErrorRed
                                                ),
                                                border = androidx.compose.foundation.BorderStroke(
                                                    1.dp,
                                                    ErrorRed.copy(alpha = 0.5f)
                                                ),
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Text(stringResource(R.string.decline), fontWeight = FontWeight.SemiBold)
                                            }
                                            Button(
                                                onClick = { onAcceptApplication?.invoke(app.id) },
                                                modifier = Modifier.weight(1f),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = SuccessGreen
                                                ),
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Text(stringResource(R.string.accept), fontWeight = FontWeight.SemiBold)
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                        }
                    }
                }

                // Team Actions
                item {
                    Spacer(modifier = Modifier.height(24.dp))

                    AnimatedEntrance(delayMillis = 400) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            if (isLeader) {
                                // Disband Team
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, ErrorRed.copy(alpha = 0.35f), RoundedCornerShape(20.dp)),
                                    colors = CardDefaults.cardColors(
                                        containerColor = ErrorRed.copy(alpha = 0.08f)
                                    ),
                                    shape = RoundedCornerShape(20.dp),
                                    onClick = { showDisbandDialog = true }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(20.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = stringResource(R.string.content_desc_disband),
                                            tint = ErrorRed,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = stringResource(R.string.disband_team),
                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = ErrorRed
                                            )
                                        )
                                    }
                                }
                            } else {
                                // Leave Team
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, WarningOrange.copy(alpha = 0.35f), RoundedCornerShape(20.dp)),
                                    colors = CardDefaults.cardColors(
                                        containerColor = WarningOrange.copy(alpha = 0.08f)
                                    ),
                                    shape = RoundedCornerShape(20.dp),
                                    onClick = { showLeaveDialog = true }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(20.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ExitToApp,
                                            contentDescription = stringResource(R.string.content_desc_leave),
                                            tint = WarningOrange,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = stringResource(R.string.leave_team),
                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = WarningOrange
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                } // End Manage Tab
            }
        }
    }

    // --- Apply To Team Dialog ---
    if (showApplyDialog) {
        var applyMessage by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showApplyDialog = false },
            containerColor = appSurfaceColor(),
            title = {
                Text(
                    text = stringResource(R.string.apply_to_team),
                    color = appTextPrimaryColor(),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "Would you like to include a message with your application?",
                        color = appTextSecondaryColor(),
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = applyMessage,
                        onValueChange = { applyMessage = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("e.g. Im a Mythic Roamer Main...", color = appTextSecondaryColor()) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = appTextPrimaryColor(),
                            unfocusedTextColor = appTextPrimaryColor(),
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = appBorderColor(),
                            focusedContainerColor = appElevatedSurfaceColor(),
                            unfocusedContainerColor = appElevatedSurfaceColor()
                        ),
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onApplyToTeam?.invoke(applyMessage)
                        showApplyDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Apply", color = appTextPrimaryColor())
                }
            },
            dismissButton = {
                TextButton(onClick = { showApplyDialog = false }) {
                    Text(stringResource(R.string.cancel), color = appTextSecondaryColor())
                }
            }
        )
    }

    // Leave Team Dialog
    if (showLeaveDialog) {
        AlertDialog(
            onDismissRequest = { showLeaveDialog = false },
            containerColor = appSurfaceColor(),
            title = {
                Text(
                    text = stringResource(R.string.leave_team_confirm),
                    color = appTextPrimaryColor(),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.leave_team_message, team.name),
                    color = appTextSecondaryColor(),
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onLeaveTeam?.invoke()
                        showLeaveDialog = false
                    }
                ) {
                    Text(stringResource(R.string.leave), color = ErrorRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveDialog = false }) {
                    Text(stringResource(R.string.cancel), color = appTextSecondaryColor())
                }
            }
        )
    }

    // Disband Team Dialog
    if (showDisbandDialog) {
        AlertDialog(
            onDismissRequest = { showDisbandDialog = false },
            containerColor = appSurfaceColor(),
            title = {
                Text(
                    text = stringResource(R.string.disband_team_confirm),
                    color = appTextPrimaryColor(),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.disband_team_message, team.name),
                    color = appTextSecondaryColor(),
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDisbandTeam?.invoke()
                        showDisbandDialog = false
                    }
                ) {
                    Text(stringResource(R.string.disband), color = ErrorRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDisbandDialog = false }) {
                    Text(stringResource(R.string.cancel), color = appTextSecondaryColor())
                }
            }
        )
    }

    // Remove Player Dialog
    playerToRemove?.let { player ->
        if (showRemoveDialog) {
            AlertDialog(
                onDismissRequest = { showRemoveDialog = false },
                containerColor = appSurfaceColor(),
                title = {
                    Text(
                        text = stringResource(R.string.remove_player_confirm),
                        color = appTextPrimaryColor(),
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = stringResource(R.string.remove_player_message, player.name),
                        color = appTextSecondaryColor(),
                        fontSize = 14.sp
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onRemovePlayer?.invoke(player.id)
                            showRemoveDialog = false
                            playerToRemove = null
                        }
                    ) {
                        Text(stringResource(R.string.remove), color = ErrorRed)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showRemoveDialog = false }) {
                        Text(stringResource(R.string.cancel), color = appTextSecondaryColor())
                    }
                }
            )
        }
    }

    // Invite Player Dialog
    if (showInviteDialog) {
        InvitePlayerDialog(
            teamName = team.name,
            inviteCode = inviteCode,
            onDismiss = { showInviteDialog = false }
        )
    }

    // Add Player Dialog
    if (showAddPlayerDialog) {
        AddPlayerDialog(
            teamName = team.name,
            onDismiss = { showAddPlayerDialog = false },
            onAddPlayer = { name, email, role ->
                onAddPlayer?.invoke(name, email, role)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TeamHistoryCard(
    match: MatchResult,
    teamId: String,
    onClick: () -> Unit
) {
    val isTeamA = match.teamAId == teamId
    val opponentName = if (isTeamA) match.teamBName else match.teamAName
    val statusText = when (match.verificationStatus) {
        VerificationStatus.CONFIRMED -> when {
            match.isDraw -> "Draw"
            match.confirmedWinnerId == teamId -> "Win"
            else -> "Loss"
        }
        VerificationStatus.PENDING -> "Pending"
        VerificationStatus.DISPUTED -> "Disputed"
        VerificationStatus.ADMIN_REVIEW -> "Review"
        VerificationStatus.AUTO_CANCELLED -> "Cancelled"
        VerificationStatus.ADMIN_RESOLVED -> "Resolved"
    }
    val statusColor = when (statusText) {
        "Win" -> SuccessGreen
        "Loss", "Cancelled" -> ErrorRed
        "Draw", "Resolved" -> MaterialTheme.colorScheme.onSurfaceVariant
        "Disputed" -> Purple
        "Review" -> MaterialTheme.colorScheme.primary
        else -> WarningOrange
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, appBorderColor(), RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(containerColor = appSurfaceColor()),
        shape = RoundedCornerShape(20.dp),
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
                    .size(44.dp)
                    .background(statusColor.copy(alpha = 0.14f), RoundedCornerShape(12.dp))
                    .border(1.dp, statusColor.copy(alpha = 0.28f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = statusText.take(1),
                    color = statusColor,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "vs ${opponentName.ifBlank { "Opponent" }}",
                    color = appTextPrimaryColor(),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(match.createdAt)),
                    color = appTextSecondaryColor(),
                    fontSize = 12.sp
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = statusText,
                    color = statusColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (match.isConfirmed) "Ranked" else match.verificationStatus.name.replace("_", " "),
                    color = appTextSecondaryColor().copy(alpha = 0.75f),
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
fun PlayerCard(
    player: com.scrimslegends.app.data.model.Player,
    isLeader: Boolean = false,
    onChangeRole: ((com.scrimslegends.app.data.model.PlayerRole) -> Unit)? = null,
    onKick: (() -> Unit)? = null
) {
    var showRoleDialog by remember { mutableStateOf(false) }
    var showHandoverConfirm by remember { mutableStateOf(false) }
    var showKickConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, appBorderColor(), RoundedCornerShape(20.dp))
            .clickable {
                if (isLeader && player.role != com.scrimslegends.app.data.model.PlayerRole.LEADER) {
                    showRoleDialog = true
                }
            },
        colors = CardDefaults.cardColors(
            containerColor = appSurfaceColor()
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Player Avatar
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (player.avatarUrl != null) {
                    AsyncImage(
                        model = player.avatarUrl,
                        contentDescription = player.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = player.name.firstOrNull()?.uppercaseChar()?.toString() ?: stringResource(R.string.player_initial_fallback),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Player Info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = player.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = appTextPrimaryColor()
                    )
                )

            }

            // Role Badge & Actions
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when (player.role) {
                        com.scrimslegends.app.data.model.PlayerRole.LEADER -> WarningOrange.copy(alpha = 0.15f)
                        com.scrimslegends.app.data.model.PlayerRole.CO_LEADER -> SuccessGreen.copy(alpha = 0.15f)
                        else -> appElevatedSurfaceColor()
                    }
                ) {
                    Text(
                        text = player.role.name.replace("_", " "),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = when (player.role) {
                            com.scrimslegends.app.data.model.PlayerRole.LEADER -> WarningOrange
                            com.scrimslegends.app.data.model.PlayerRole.CO_LEADER -> SuccessGreen
                            else -> appTextSecondaryColor()
                        },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }

                if (isLeader && player.role != com.scrimslegends.app.data.model.PlayerRole.LEADER) {
                    var showMenu by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = stringResource(R.string.content_desc_player_actions),
                                tint = appTextSecondaryColor(),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier.background(appSurfaceColor())
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.change_role), color = appTextPrimaryColor()) },
                                onClick = {
                                    showMenu = false
                                    showRoleDialog = true
                                },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.handover_leadership), color = MaterialTheme.colorScheme.secondary) },
                                onClick = {
                                    showMenu = false
                                    showHandoverConfirm = true
                                },
                                leadingIcon = { Icon(Icons.Default.Stars, contentDescription = null, tint = MaterialTheme.colorScheme.secondary) }
                            )
                            Divider(color = appBorderColor())
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.kick_player), color = ErrorRed) },
                                onClick = {
                                    showMenu = false
                                    showKickConfirm = true
                                },
                                leadingIcon = { Icon(Icons.Default.PersonRemove, contentDescription = null, tint = ErrorRed) }
                            )
                        }
                    }
                }
            } // End of LazyColumn

        }
    }

    // Role Change Dialog
    if (showRoleDialog) {
        AlertDialog(
            onDismissRequest = { showRoleDialog = false },
            containerColor = appSurfaceColor(),
            title = {
                Text(
                    text = stringResource(R.string.change_role),
                    color = appTextPrimaryColor(),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = stringResource(R.string.select_new_role, player.name),
                        color = appTextSecondaryColor(),
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    com.scrimslegends.app.data.model.PlayerRole.values().filter { it != com.scrimslegends.app.data.model.PlayerRole.LEADER }.forEach { role ->
                        val isSelected = player.role == role
                        Button(
                            onClick = {
                                onChangeRole?.invoke(role)
                                showRoleDialog = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) {
                                    when (role) {
                                        com.scrimslegends.app.data.model.PlayerRole.CO_LEADER -> SuccessGreen.copy(alpha = 0.3f)
                                        else -> appElevatedSurfaceColor()
                                    }
                                } else appElevatedSurfaceColor()
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = role.name.replace("_", " "),
                                    color = when (role) {
                                        com.scrimslegends.app.data.model.PlayerRole.CO_LEADER -> SuccessGreen
                                        else -> White
                                    },
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = SuccessGreen,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showRoleDialog = false }) {
                    Text(stringResource(R.string.cancel), color = appTextSecondaryColor())
                }
            }
        )
    }

    // Handover Confirmation
    if (showHandoverConfirm) {
        AlertDialog(
            onDismissRequest = { showHandoverConfirm = false },
            containerColor = appSurfaceColor(),
            title = {
                Text(
                    text = stringResource(R.string.handover_leadership_confirm),
                    color = appTextPrimaryColor(),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.handover_leadership_message, player.name),
                    color = appTextSecondaryColor(),
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onChangeRole?.invoke(com.scrimslegends.app.data.model.PlayerRole.LEADER)
                        showHandoverConfirm = false
                    }
                ) {
                    Text(stringResource(R.string.handover), color = MaterialTheme.colorScheme.secondary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showHandoverConfirm = false }) {
                    Text(stringResource(R.string.cancel), color = appTextSecondaryColor())
                }
            }
        )
    }

    // Kick Confirmation
    if (showKickConfirm) {
        AlertDialog(
            onDismissRequest = { showKickConfirm = false },
            containerColor = appSurfaceColor(),
            title = {
                Text(
                    text = stringResource(R.string.kick_player_confirm),
                    color = appTextPrimaryColor(),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.kick_player_message, player.name),
                    color = appTextSecondaryColor(),
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onKick?.invoke()
                        showKickConfirm = false
                    }
                ) {
                    Text(stringResource(R.string.kick), color = ErrorRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showKickConfirm = false }) {
                    Text(stringResource(R.string.cancel), color = appTextSecondaryColor())
                }
            }
        )
    }
}

@Composable
private fun MiniBar(label: String, value: Int, color: Color, max: Int = 20) {
    val heightFraction = (value.toFloat() / max).coerceIn(0.05f, 1f)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold,
                color = color
            )
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .width(24.dp)
                .height(80.dp)
                .background(Color.DarkGray.copy(alpha = 0.3f), RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(heightFraction)
                    .background(color.copy(alpha = 0.85f), RoundedCornerShape(4.dp))
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 11.sp,
                color = appTextSecondaryColor()
            )
        )
    }
}

@Composable
private fun TeamStatBox(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: String,
    tint: Color,
    isLarge: Boolean = false
) {
    Card(
        modifier = modifier
            .border(1.dp, appBorderColor(), RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(
            containerColor = appSurfaceColor()
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (isLarge) 24.dp else 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(if (isLarge) 40.dp else 28.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontSize = if (isLarge) 32.sp else 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = appTextPrimaryColor()
                )
            )
            Spacer(modifier = Modifier.height(if (isLarge) 6.dp else 4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = if (isLarge) 14.sp else 12.sp,
                    fontWeight = if (isLarge) FontWeight.SemiBold else FontWeight.Normal,
                    color = appTextSecondaryColor()
                )
            )
        }
    }
}

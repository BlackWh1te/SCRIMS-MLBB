package com.scrimslegends.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
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
    onSubmitRating: ((rating: Int, feedback: String) -> Unit)? = null
) {
    LaunchedEffect(team.id) {
        onLoadStats()
    }

    // Pre-compute stats so they're available across all LazyColumn items
    val totalScrims = (teamStats["total_scrims"] as? Number)?.toInt() ?: 0
    val wins = (teamStats["wins"] as? Number)?.toInt() ?: 0
    val losses = (teamStats["losses"] as? Number)?.toInt() ?: 0
    val totalPoints = (teamStats["total_points"] as? Number)?.toInt() ?: 0
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
    var playerToRemove by remember { mutableStateOf<com.scrimslegends.app.data.model.Player?>(null) }

    val inviteCode = remember(team.id) {
        "SL-${team.name.take(3).uppercase()}${team.id.takeLast(4).uppercase()}"
    }

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = mutableListOf("Overview", "Roster")
    // Only show Manage tab if there's manage actions or leader
    if (isLeader || onLeaveTeam != null) {
        tabs.add("Manage")
    }

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
                            color = White
                        )
                    )

                    Spacer(modifier = Modifier.width(44.dp))
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                contentPadding = PaddingValues(vertical = 20.dp)
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
                                    .padding(24.dp),
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
                                        text = team.name.firstOrNull()?.uppercaseChar()?.toString() ?: stringResource(R.string.team_initial_fallback),
                                        fontSize = 40.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = White
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Team Name
                                Text(
                                    text = team.name,
                                    style = MaterialTheme.typography.headlineLarge.copy(
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = White
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
                                        tint = LightGray,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = stringResource(R.string.players_count, team.players.size, 7),
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontSize = 15.sp,
                                            color = LightGray
                                        )
                                    )
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
                            selectedTabIndex = selectedTabIndex,
                            containerColor = Color.Transparent,
                            contentColor = White,
                            indicator = { tabPositions ->
                                TabRowDefaults.Indicator(
                                    Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                                    color = GoldPrimary,
                                    height = 3.dp
                                )
                            },
                            divider = {
                                Divider(color = GlassBorder)
                            }
                        ) {
                            tabs.forEachIndexed { index, title ->
                                Tab(
                                    selected = selectedTabIndex == index,
                                    onClick = { selectedTabIndex = index },
                                    text = { 
                                        Text(
                                            text = title, 
                                            fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Medium,
                                            color = if (selectedTabIndex == index) GoldPrimary else LightGray
                                        ) 
                                    }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // ─── OVERVIEW TAB ───
                if (tabs[selectedTabIndex] == "Overview") {
                    // Team Stats Section (real data from get_team_stats RPC)
                    item {
                    AnimatedEntrance(delayMillis = 175) {
                        Text(
                            text = stringResource(R.string.team_stats),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontSize = 20.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = White
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    AnimatedEntrance(delayMillis = 185) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            TeamStatBox(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Default.SportsEsports,
                                label = stringResource(R.string.scrims),
                                value = totalScrims.toString(),
                                tint = BluePrimary
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
                                icon = Icons.Default.TrendingUp,
                                label = stringResource(R.string.win_rate),
                                value = winRate,
                                tint = GoldPrimary
                            )
                            TeamStatBox(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Default.Star,
                                label = stringResource(R.string.avg_rating),
                                value = avgRating,
                                tint = Purple
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }

                // Weekly Wins / Losses mini-graph
                item {
                    AnimatedEntrance(delayMillis = 190) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = DarkNavy),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = stringResource(R.string.this_week),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = White
                                    )
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    MiniBar(label = stringResource(R.string.wins), value = weeklyWins, color = SuccessGreen)
                                    MiniBar(label = stringResource(R.string.losses), value = weeklyLosses, color = ErrorRed)
                                    MiniBar(label = stringResource(R.string.points), value = totalPoints.coerceAtMost(100), color = GoldPrimary)
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
                                    color = White
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    items(teamRatings.take(5)) { rating ->
                        AnimatedEntrance(delayMillis = 220) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = DarkNavy),
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
                                                color = White
                                            )
                                        )
                                        Row {
                                            repeat(rating.rating) {
                                                Icon(
                                                    imageVector = Icons.Default.Star,
                                                    contentDescription = null,
                                                    tint = GoldPrimary,
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
                                                color = LightGray
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
                if (tabs[selectedTabIndex] == "Roster") {
                    // Players Section
                item {
                    AnimatedEntrance(delayMillis = 200) {
                        Text(
                            text = stringResource(R.string.players),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontSize = 20.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = White
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
                                    .shadow(
                                        elevation = 4.dp,
                                        spotColor = Color.Black.copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(16.dp)
                                    ),
                                colors = CardDefaults.cardColors(
                                    containerColor = DarkNavy
                                ),
                                shape = RoundedCornerShape(16.dp),
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
                                        tint = BluePrimary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = stringResource(R.string.add_player),
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = BluePrimary
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
                                    .shadow(
                                        elevation = 4.dp,
                                        spotColor = GoldPrimary.copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(16.dp)
                                    ),
                                colors = CardDefaults.cardColors(
                                    containerColor = DarkNavy
                                ),
                                shape = RoundedCornerShape(16.dp),
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
                                        tint = GoldPrimary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = stringResource(R.string.invite_by_code),
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = GoldPrimary
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
                } // End Roster Tab

                // ─── MANAGE TAB ───
                if (tabs.getOrNull(selectedTabIndex) == "Manage") {
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
                                    color = White
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        applications.forEachIndexed { index, app ->
                            AnimatedEntrance(delayMillis = 390 + index * 60) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .shadow(
                                            elevation = 4.dp,
                                            spotColor = GoldPrimary.copy(alpha = 0.1f),
                                            shape = RoundedCornerShape(16.dp)
                                        ),
                                    colors = CardDefaults.cardColors(
                                        containerColor = DarkNavy
                                    ),
                                    shape = RoundedCornerShape(16.dp)
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
                                                                GoldPrimary.copy(alpha = 0.3f),
                                                                GoldPrimary.copy(alpha = 0.1f)
                                                            )
                                                        )
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = app.applicantName.firstOrNull()?.uppercaseChar()?.toString() ?: stringResource(R.string.unknown_applicant_initial),
                                                    fontSize = 18.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = GoldPrimary
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = app.applicantName,
                                                    style = MaterialTheme.typography.titleMedium.copy(
                                                        fontSize = 16.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = White
                                                    )
                                                )
                                                if (!app.message.isNullOrBlank()) {
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(
                                                        text = app.message,
                                                        style = MaterialTheme.typography.bodySmall.copy(
                                                            fontSize = 12.sp,
                                                            color = LightGray
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
                                        .shadow(
                                            elevation = 4.dp,
                                            spotColor = ErrorRed.copy(alpha = 0.1f),
                                            shape = RoundedCornerShape(16.dp)
                                        ),
                                    colors = CardDefaults.cardColors(
                                        containerColor = ErrorRed.copy(alpha = 0.08f)
                                    ),
                                    shape = RoundedCornerShape(16.dp),
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
                                        .shadow(
                                            elevation = 4.dp,
                                            spotColor = WarningOrange.copy(alpha = 0.1f),
                                            shape = RoundedCornerShape(16.dp)
                                        ),
                                    colors = CardDefaults.cardColors(
                                        containerColor = WarningOrange.copy(alpha = 0.08f)
                                    ),
                                    shape = RoundedCornerShape(16.dp),
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

    // Leave Team Dialog
    if (showLeaveDialog) {
        AlertDialog(
            onDismissRequest = { showLeaveDialog = false },
            containerColor = DarkNavy,
            title = {
                Text(
                    text = stringResource(R.string.leave_team_confirm),
                    color = White,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.leave_team_message, team.name),
                    color = LightGray,
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
                    Text(stringResource(R.string.cancel), color = MidGray)
                }
            }
        )
    }

    // Disband Team Dialog
    if (showDisbandDialog) {
        AlertDialog(
            onDismissRequest = { showDisbandDialog = false },
            containerColor = DarkNavy,
            title = {
                Text(
                    text = stringResource(R.string.disband_team_confirm),
                    color = White,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.disband_team_message, team.name),
                    color = LightGray,
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
                    Text(stringResource(R.string.cancel), color = MidGray)
                }
            }
        )
    }

    // Remove Player Dialog
    playerToRemove?.let { player ->
        if (showRemoveDialog) {
            AlertDialog(
                onDismissRequest = { showRemoveDialog = false },
                containerColor = DarkNavy,
                title = {
                    Text(
                        text = stringResource(R.string.remove_player_confirm),
                        color = White,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = stringResource(R.string.remove_player_message, player.name),
                        color = LightGray,
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
                        Text(stringResource(R.string.cancel), color = MidGray)
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
            .shadow(
                elevation = 4.dp,
                spotColor = Color.Black.copy(alpha = 0.1f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable {
                if (isLeader && player.role != com.scrimslegends.app.data.model.PlayerRole.LEADER) {
                    showRoleDialog = true
                }
            },
        colors = CardDefaults.cardColors(
            containerColor = DarkNavy
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Player Avatar with gradient
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .shadow(
                        elevation = 6.dp,
                        spotColor = BluePrimary.copy(alpha = 0.2f),
                        shape = CircleShape
                    )
                    .clip(CircleShape)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(BluePrimary.copy(alpha = 0.3f), BluePrimary.copy(alpha = 0.1f))
                        )
                    ),
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
                        color = BluePrimary
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
                        color = White
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.email_label, player.email),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.sp,
                        color = LightGray
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
                        else -> White.copy(alpha = 0.15f)
                    }
                ) {
                    Text(
                        text = player.role.name.replace("_", " "),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = when (player.role) {
                            com.scrimslegends.app.data.model.PlayerRole.LEADER -> WarningOrange
                            com.scrimslegends.app.data.model.PlayerRole.CO_LEADER -> SuccessGreen
                            else -> LightGray
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
                                tint = MidGray,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier.background(DarkNavy)
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.change_role), color = White) },
                                onClick = {
                                    showMenu = false
                                    showRoleDialog = true
                                },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = BluePrimary) }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.handover_leadership), color = GoldPrimary) },
                                onClick = {
                                    showMenu = false
                                    showHandoverConfirm = true
                                },
                                leadingIcon = { Icon(Icons.Default.Stars, contentDescription = null, tint = GoldPrimary) }
                            )
                            Divider(color = White.copy(alpha = 0.1f))
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
            }
        }
    }

    // Role Change Dialog
    if (showRoleDialog) {
        AlertDialog(
            onDismissRequest = { showRoleDialog = false },
            containerColor = DarkNavy,
            title = {
                Text(
                    text = stringResource(R.string.change_role),
                    color = White,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = stringResource(R.string.select_new_role, player.name),
                        color = LightGray,
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
                                        else -> White.copy(alpha = 0.2f)
                                    }
                                } else DarkSurface.copy(alpha = 0.6f)
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
                    Text(stringResource(R.string.cancel), color = MidGray)
                }
            }
        )
    }

    // Handover Confirmation
    if (showHandoverConfirm) {
        AlertDialog(
            onDismissRequest = { showHandoverConfirm = false },
            containerColor = DarkNavy,
            title = {
                Text(
                    text = stringResource(R.string.handover_leadership_confirm),
                    color = White,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.handover_leadership_message, player.name),
                    color = LightGray,
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
                    Text(stringResource(R.string.handover), color = GoldPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showHandoverConfirm = false }) {
                    Text(stringResource(R.string.cancel), color = MidGray)
                }
            }
        )
    }

    // Kick Confirmation
    if (showKickConfirm) {
        AlertDialog(
            onDismissRequest = { showKickConfirm = false },
            containerColor = DarkNavy,
            title = {
                Text(
                    text = stringResource(R.string.kick_player_confirm),
                    color = White,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.kick_player_message, player.name),
                    color = LightGray,
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
                    Text(stringResource(R.string.cancel), color = MidGray)
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
                color = LightGray
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
    tint: Color
) {
    Card(
        modifier = modifier
            .shadow(
                elevation = 4.dp,
                spotColor = tint.copy(alpha = 0.15f),
                shape = RoundedCornerShape(16.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = DarkNavy
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = White
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                    color = LightGray
                )
            )
        }
    }
}

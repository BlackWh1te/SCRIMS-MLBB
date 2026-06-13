package com.scrimslegends.app.ui.screens

import androidx.compose.material3.MaterialTheme
import android.net.Uri
import timber.log.Timber
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scrimslegends.app.data.model.ApplicationStatus
import com.scrimslegends.app.data.model.BestOf
import com.scrimslegends.app.data.model.Scrim
import com.scrimslegends.app.data.model.ScrimApplication
import com.scrimslegends.app.data.model.ScrimGameResult
import com.scrimslegends.app.data.model.ScrimGameStatus
import com.scrimslegends.app.data.model.ScrimRosterEntry
import com.scrimslegends.app.data.model.ScrimStatus
import com.scrimslegends.app.util.CalendarIntentHelper
import com.scrimslegends.app.data.model.Team
import com.scrimslegends.app.data.service.SupabaseConfig
import com.scrimslegends.app.data.service.SupabaseStorageUpload
import com.scrimslegends.app.ui.theme.*
import com.scrimslegends.app.R
import androidx.compose.ui.res.stringResource
import com.scrimslegends.app.ui.components.AnimatedEntrance
import com.scrimslegends.app.ui.components.GlassBackButton
import com.scrimslegends.app.ui.components.GradientButton
import com.scrimslegends.app.ui.components.EnhancedStatusBadge
import coil.compose.SubcomposeAsyncImage
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@Composable
fun ScrimDetailScreen(
    scrim: Scrim,
    currentUserId: String,
    isLoading: Boolean = false,
    error: String? = null,
    teams: List<Team> = emptyList(),                          // All user's teams (for multi-team apply)
    onNavigateBack: () -> Unit,
    onJoinScrim: (String) -> Unit = {},
    onLeaveScrim: (String) -> Unit = {},
    // ── Apply callback: scrim, teamId, teamName, selectedPlayerIds ──
    onApplyScrim: (Scrim, String, String, List<String>) -> Unit = { _, _, _, _ -> },
    onApproveApplication: (String, String) -> Unit = { _, _ -> },
    onRejectApplication: (String, String) -> Unit = { _, _ -> },
    onCancelApplication: (String, String) -> Unit = { _, _ -> },
    onCancelScrim: ((String) -> Unit)? = null,
    onNavigateToChat: ((String) -> Unit)? = null,
    // ── New scrim flow callbacks ──
    onNavigateToRoster: ((String, String) -> Unit)? = null,  // scrimId, teamId
    onMarkReady: ((String, String) -> Unit)? = null,        // scrimId, teamId
    onUploadScreenshot: ((String, String, String) -> Unit)? = null, // scrimId, teamId, screenshotUrl
    onUploadGameScreenshot: ((String, String, Int, String) -> Unit)? = null, // scrimId, teamId, gameNumber, screenshotUrl
    onSelectGameWinner: ((String, Int, String) -> Unit)? = null, // scrimId, gameNumber, winnerTeamId
    onChangeSeriesFormat: ((String, Int) -> Unit)? = null,  // scrimId, newBestOf
    onCompleteScrim: ((String, String?) -> Unit)? = null      // scrimId, winnerTeamId
) {
    var showCancelDialog by remember { mutableStateOf(false) }

    // ── Apply flow state ──
    var showTeamPicker by remember { mutableStateOf(false) }
    var showPlayerPicker by remember { mutableStateOf(false) }
    var selectedApplyTeam by remember { mutableStateOf<Team?>(null) }
    var selectedPlayerIds by remember { mutableStateOf<Set<String>>(emptySet()) }

    // Derive leader-capable teams (teams where current user IS the leader AND has min players)
    val leaderTeams = teams.filter { it.leaderId == currentUserId && it.meetsMinPlayers }
    val canApply = (scrim.status == ScrimStatus.OPEN || scrim.status == ScrimStatus.PENDING) && leaderTeams.isNotEmpty()

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
                        text = stringResource(R.string.scrim_details),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )

                    Spacer(modifier = Modifier.width(44.dp))
                }
            }

            AnimatedVisibility(
                visible = error != null,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(ErrorRed.copy(alpha = 0.15f))
                        .border(1.dp, ErrorRed.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.ErrorOutline,
                            contentDescription = "Error",
                            tint = ErrorRed,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = error ?: "",
                            color = ErrorRed,
                            style = iOSCaption1.copy(fontWeight = FontWeight.Medium)
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 120.dp)
            ) {
                // Team Header Card
                item {
                    AnimatedEntrance(delayMillis = 20) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, appBorderColor(), RoundedCornerShape(20.dp)),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.background
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
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))
                                        .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.28f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = scrim.teamName.firstOrNull()?.uppercaseChar()?.toString() ?: "T",
                                        fontSize = 40.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Team Name
                                Text(
                                    text = scrim.teamName,
                                    style = MaterialTheme.typography.headlineLarge.copy(
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                // Status Badge
                                EnhancedStatusBadge(
                                    text = when (scrim.status) {
                                        ScrimStatus.OPEN -> stringResource(R.string.scrim_status_open)
                                        ScrimStatus.FILLED -> stringResource(R.string.scrim_status_filled)
                                        ScrimStatus.READY_CHECK -> stringResource(R.string.ready_check)
                                        ScrimStatus.IN_PROGRESS -> stringResource(R.string.scrim_status_in_progress)
                                        ScrimStatus.COMPLETED -> stringResource(R.string.scrim_status_completed)
                                        ScrimStatus.CANCELLED -> stringResource(R.string.scrim_status_cancelled)
                                        else -> stringResource(R.string.scrim_status_open)
                                    },
                                    color = when (scrim.status) {
                                        ScrimStatus.OPEN -> SuccessGreen
                                        ScrimStatus.FILLED -> WarningOrange
                                        ScrimStatus.READY_CHECK -> WarningOrange
                                        ScrimStatus.IN_PROGRESS -> MaterialTheme.colorScheme.primary
                                        ScrimStatus.COMPLETED -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                        ScrimStatus.CANCELLED -> ErrorRed
                                        else -> SuccessGreen
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }

                item {
                    AnimatedEntrance(delayMillis = 40) {
                        Text(
                            text = stringResource(R.string.scrim_info),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontSize = 22.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                }

                item {
                    AnimatedEntrance(delayMillis = 50) {
                        InfoCard(
                            icon = Icons.Default.SportsEsports,
                            label = stringResource(R.string.game_mode),
                            value = scrim.gameMode.displayName
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }

                item {
                    AnimatedEntrance(delayMillis = 60) {
                        InfoCard(
                            icon = Icons.Default.Public,
                            label = stringResource(R.string.region),
                            value = scrim.region.displayName
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }

                item {
                    AnimatedEntrance(delayMillis = 70) {
                        InfoCard(
                            icon = Icons.Default.Star,
                            label = stringResource(R.string.skill_level),
                            value = scrim.skillLevel.name
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }

                item {
                    AnimatedEntrance(delayMillis = 80) {
                        InfoCard(
                            icon = Icons.Default.SportsScore,
                            label = stringResource(R.string.format_label),
                            value = scrim.bestOf.displayName
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }

                item {
                    AnimatedEntrance(delayMillis = 90) {
                        InfoCard(
                            icon = Icons.Default.AccessTime,
                            label = stringResource(R.string.scheduled_time),
                            value = formatDetailedTime(scrim.scheduledTime, scrim.region.timeZoneId, scrim.region.displayName)
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }

                item {
                    AnimatedEntrance(delayMillis = 100) {
                        InfoCard(
                            icon = Icons.Default.Person,
                            label = stringResource(R.string.players),
                            value = "${scrim.currentPlayers} / ${scrim.maxPlayers}"
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }

                if (scrim.description.isNotBlank()) {
                    item {
                        AnimatedEntrance(delayMillis = 110) {
                            Text(
                                text = stringResource(R.string.description),
                                style = iOSTitle2.copy(color = MaterialTheme.colorScheme.onSurface)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    item {
                        AnimatedEntrance(delayMillis = 120) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
                            ) {
                                Text(
                                    text = scrim.description,
                                    style = iOSBody.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                                    modifier = Modifier.padding(20.dp)
                                )
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(28.dp))
                    }
                }

                if (scrim.teamARoster.isNotEmpty() || scrim.teamBRoster.isNotEmpty()) {
                    item {
                        AnimatedEntrance(delayMillis = 130) {
                            Text(
                                text = stringResource(R.string.rosters),
                                style = iOSTitle2.copy(color = MaterialTheme.colorScheme.onSurface)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    if (scrim.teamARoster.isNotEmpty()) {
                        item {
                            AnimatedEntrance(delayMillis = 140) {
                                RosterDisplayCard(
                                    teamName = scrim.teamName,
                                    roster = scrim.teamARoster
                                )
                            }
                        }
                        item { Spacer(modifier = Modifier.height(12.dp)) }
                    }

                    if (scrim.teamBRoster.isNotEmpty()) {
                        item {
                            AnimatedEntrance(delayMillis = 150) {
                                RosterDisplayCard(
                                    teamName = scrim.opponentTeamName ?: stringResource(R.string.opponent_label),
                                    roster = scrim.teamBRoster
                                )
                            }
                        }
                        item { Spacer(modifier = Modifier.height(12.dp)) }
                    }
                }

                item {
                    val userTeamIds = teams.map { it.id }.toSet()
                    val isHost = teams.any { it.id == scrim.teamId && it.leaderId == currentUserId }
                    val myPendingApplication = scrim.applications.find {
                        it.applicantTeamId in userTeamIds && it.status == ApplicationStatus.PENDING
                    }
                    val isOpponent = scrim.opponentTeamId in userTeamIds
                    val myOpponentTeam = teams.find { it.id == scrim.opponentTeamId }

                    AnimatedEntrance(delayMillis = 150) {
                        when {
                            isHost -> HostActions(
                                scrim = scrim,
                                currentTeamId = scrim.teamId,
                                isLoading = isLoading,
                                onCancelScrim = { showCancelDialog = true },
                                onApprove = { appId -> onApproveApplication(scrim.id, appId) },
                                onReject = { appId -> onRejectApplication(scrim.id, appId) },
                                onNavigateToChat = onNavigateToChat,
                                onNavigateToRoster = onNavigateToRoster,
                                onMarkReady = onMarkReady,
                                onUploadScreenshot = onUploadScreenshot,
                                onUploadGameScreenshot = onUploadGameScreenshot,
                                onSelectGameWinner = onSelectGameWinner,
                                onChangeSeriesFormat = onChangeSeriesFormat,
                                onCompleteScrim = onCompleteScrim
                            )

                            isOpponent -> OpponentActions(
                                scrim = scrim,
                                currentTeamId = myOpponentTeam?.id ?: "",
                                isLoading = isLoading,
                                onNavigateToChat = onNavigateToChat,
                                onNavigateToRoster = onNavigateToRoster,
                                onMarkReady = onMarkReady,
                                onUploadScreenshot = onUploadScreenshot,
                                onUploadGameScreenshot = onUploadGameScreenshot,
                                onSelectGameWinner = onSelectGameWinner,
                                onChangeSeriesFormat = onChangeSeriesFormat,
                                onCompleteScrim = onCompleteScrim
                            )

                            myPendingApplication != null -> ApplicantStatusCard(
                                application = myPendingApplication,
                                scrim = scrim,
                                onCancel = { onCancelApplication(scrim.id, myPendingApplication.id) },
                                onNavigateToChat = onNavigateToChat,
                                onNavigateToRoster = onNavigateToRoster,
                                onMarkReady = onMarkReady,
                                onUploadScreenshot = onUploadScreenshot,
                                onCompleteScrim = onCompleteScrim
                            )

                            canApply -> {
                                val firstTeam = leaderTeams.first()
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    GradientButton(
                                        text = stringResource(R.string.apply_with_team),
                                        onClick = {
                                            if (leaderTeams.size == 1) {
                                                selectedApplyTeam = firstTeam
                                                selectedPlayerIds = firstTeam.players.take(5).map { it.id }.toSet()
                                                showPlayerPicker = true
                                            } else {
                                                showTeamPicker = true
                                            }
                                        },
                                        gradient = BlueGradient,
                                        height = 56.dp
                                    )
                                    if (leaderTeams.size == 1) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = stringResource(R.string.applying_as, firstTeam.name),
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f).copy(alpha = 0.6f),
                                            modifier = Modifier.align(Alignment.CenterHorizontally)
                                        )
                                    }
                                }
                            }

                            (scrim.status == ScrimStatus.OPEN || scrim.status == ScrimStatus.PENDING) && teams.any { it.leaderId == currentUserId && !it.meetsMinPlayers } -> {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    GradientButton(
                                        text = stringResource(R.string.need_5_plus_players),
                                        onClick = { },
                                        enabled = false,
                                        gradient = listOf(Color.Gray, Color.DarkGray),
                                        height = 56.dp
                                    )
                                    Text(
                                        text = stringResource(R.string.team_needs_5_players),
                                        fontSize = 13.sp,
                                        color = WarningOrange,
                                        modifier = Modifier.align(Alignment.CenterHorizontally)
                                    )
                                }
                            }

                            (scrim.status == ScrimStatus.OPEN || scrim.status == ScrimStatus.PENDING) -> {
                                GradientButton(
                                    text = stringResource(R.string.team_leaders_only),
                                    onClick = { },
                                    enabled = false,
                                    gradient = listOf(Color.Gray, Color.DarkGray),
                                    height = 56.dp
                                )
                            }

                            else -> ScrimStatusCard(scrim = scrim)
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }

    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            containerColor = MaterialTheme.colorScheme.background,
            title = {
                Text(
                    text = stringResource(R.string.cancel_scrim),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.cancel_scrim_confirm),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
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
                    Text(stringResource(R.string.cancel_scrim), color = ErrorRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) {
                    Text(stringResource(R.string.keep_scrim), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    if (showTeamPicker) {
        TeamPickerDialog(
            teams = leaderTeams,
            onTeamSelected = { team ->
                selectedApplyTeam = team
                selectedPlayerIds = team.players.take(5).map { it.id }.toSet()
                showTeamPicker = false
                showPlayerPicker = true
            },
            onDismiss = { showTeamPicker = false }
        )
    }

    selectedApplyTeam?.let { applyTeam ->
        if (showPlayerPicker) {
        PlayerPickerDialog(
            team = applyTeam,
            selectedPlayerIds = selectedPlayerIds,
            onPlayerToggle = { playerId ->
                selectedPlayerIds = if (playerId in selectedPlayerIds) {
                    selectedPlayerIds - playerId
                } else {
                    selectedPlayerIds + playerId
                }
            },
            onConfirm = {
                onApplyScrim(
                    scrim,
                    applyTeam.id,
                    applyTeam.name,
                    selectedPlayerIds.toList()
                )
                showPlayerPicker = false
                selectedApplyTeam = null
                selectedPlayerIds = emptySet()
            },
            onDismiss = {
                showPlayerPicker = false
                selectedApplyTeam = null
                selectedPlayerIds = emptySet()
            }
        )
        }
    }
}

@Composable
fun InfoCard(
    icon : androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
    ) {
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.22f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector        = icon,
                    contentDescription = label,
                    tint               = MaterialTheme.colorScheme.primary,
                    modifier           = Modifier.size(20.dp)
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text  = label,
                    style = iOSCaption1.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text  = value,
                    style = iOSHeadline.copy(color = MaterialTheme.colorScheme.onSurface)
                )
            }
        }
    }
}

@Composable
private fun HostActions(
    scrim: Scrim,
    currentTeamId: String?,
    isLoading: Boolean,
    onCancelScrim: () -> Unit,
    onApprove: (String) -> Unit,
    onReject: (String) -> Unit,
    onNavigateToChat: ((String) -> Unit)?,
    onNavigateToRoster: ((String, String) -> Unit)?,
    onMarkReady: ((String, String) -> Unit)?,
    onUploadScreenshot: ((String, String, String) -> Unit)?,
    onUploadGameScreenshot: ((String, String, Int, String) -> Unit)? = null,
    onSelectGameWinner: ((String, Int, String) -> Unit)? = null,
    onChangeSeriesFormat: ((String, Int) -> Unit)? = null,
    onCompleteScrim: ((String, String?) -> Unit)?
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        when (scrim.status) {
            ScrimStatus.OPEN, ScrimStatus.PENDING -> {
                val pendingApps = scrim.applications.filter { it.status == ApplicationStatus.PENDING }
                if (pendingApps.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.pending_applications, pendingApps.size),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    pendingApps.forEach { app ->
                        ApplicationCard(
                            application = app,
                            isLoading = isLoading,
                            onApprove = { onApprove(app.id) },
                            onReject = { onReject(app.id) }
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                } else {
                    GradientButton(
                        text = stringResource(R.string.your_scrim_waiting),
                        onClick = { },
                        enabled = false,
                        gradient = listOf(Color.Gray, Color.DarkGray),
                        height = 56.dp
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                GradientButton(
                    text = stringResource(R.string.cancel_scrim),
                    onClick = onCancelScrim,
                    isLoading = isLoading,
                    gradient = listOf(ErrorRed, ErrorRed.copy(alpha = 0.7f)),
                    height = 56.dp
                )
            }

            ScrimStatus.FILLED -> {
                scrim.opponentTeamName?.let { opponentName ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, appBorderColor(), RoundedCornerShape(20.dp)),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .background(SuccessGreen.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.CheckCircle, null, tint = SuccessGreen, modifier = Modifier.size(24.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(stringResource(R.string.opponent_label), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
                                    Text(opponentName, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                }
                                val context = LocalContext.current
                                IconButton(
                                    onClick = { CalendarIntentHelper.addScrimToCalendar(context, scrim) },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarMonth,
                                        contentDescription = "Add to calendar",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            if (currentTeamId != null && onNavigateToRoster != null) {
                                GradientButton(
                                    text = stringResource(R.string.select_roster),
                                    onClick = { onNavigateToRoster(scrim.id, currentTeamId) },
                                    gradient = BlueGradient,
                                    height = 48.dp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            if (scrim.isChatOpen) {
                                GradientButton(
                                    text = stringResource(R.string.open_chat),
                                    onClick = { scrim.conversationId?.let { onNavigateToChat?.invoke(it) } },
                                    gradient = PremiumBlueGradient,
                                    height = 48.dp
                                )
                            } else {
                                ChatGateCountdown(timeUntilOpens = scrim.timeUntilChatOpens)
                            }
                        }
                    }
                }
            }

            ScrimStatus.READY_CHECK -> {
                ReadyCheckSection(
                    scrim = scrim,
                    currentTeamId = currentTeamId,
                    isLoading = isLoading,
                    onMarkReady = onMarkReady,
                    onNavigateToChat = onNavigateToChat
                )
            }

            ScrimStatus.IN_PROGRESS -> {
                InProgressSection(
                    scrim = scrim,
                    currentTeamId = currentTeamId,
                    isLoading = isLoading,
                    onUploadScreenshot = onUploadScreenshot,
                    onUploadGameScreenshot = onUploadGameScreenshot,
                    onSelectGameWinner = onSelectGameWinner,
                    onChangeSeriesFormat = onChangeSeriesFormat,
                    onCompleteScrim = onCompleteScrim,
                    onNavigateToChat = onNavigateToChat
                )
            }

            ScrimStatus.COMPLETED -> ScrimStatusCard(scrim)
            ScrimStatus.CANCELLED -> ScrimStatusCard(scrim)
        }
    }
}

@Composable
private fun ApplicationCard(
    application: ScrimApplication,
    isLoading: Boolean = false,
    onApprove  : () -> Unit,
    onReject   : () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!application.applicantTeamAvatarUrl.isNullOrBlank()) {
                    SubcomposeAsyncImage(
                        model = application.applicantTeamAvatarUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop,
                        loading = {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    strokeWidth = 2.dp
                                )
                            }
                        },
                        error = {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    application.applicantTeamName.firstOrNull()?.uppercaseChar()?.toString() ?: "T",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            application.applicantTeamName.firstOrNull()?.uppercaseChar()?.toString() ?: "T",
                            fontSize   = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        application.applicantTeamName,
                        style = iOSHeadline.copy(color = MaterialTheme.colorScheme.onSurface)
                    )
                    Text(
                        stringResource(R.string.leader_name, application.applicantTeamLeaderName),
                        style = iOSCaption1.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                    Text(
                        stringResource(R.string.team_players_count, application.applicantTeamPlayers.size, application.applicantTeamPlayers.size.coerceAtLeast(5)),
                        style = iOSCaption1.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
            }

            if (application.applicantTeamPlayers.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = stringResource(R.string.applicant_roster),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    application.applicantTeamPlayers.take(7).forEach { player ->
                        PlayerChip(name = player.name, avatarUrl = player.avatarUrl)
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
            Spacer(Modifier.height(12.dp))

            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                GradientButton(
                    text     = stringResource(R.string.approve),
                    onClick  = onApprove,
                    isLoading = isLoading,
                    gradient = SuccessGradient,
                    modifier = Modifier.weight(1f),
                    height   = 44.dp
                )
                GradientButton(
                    text     = stringResource(R.string.reject),
                    onClick  = onReject,
                    isLoading = isLoading,
                    gradient = ErrorGradient,
                    modifier = Modifier.weight(1f),
                    height   = 44.dp
                )
            }
        }
    }
}

@Composable
private fun PlayerChip(name: String, avatarUrl: String?) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!avatarUrl.isNullOrBlank()) {
            SubcomposeAsyncImage(
                model = avatarUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    name.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        Spacer(Modifier.width(6.dp))
        Text(
            text = name,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            maxLines = 1
        )
    }
}

@Composable
private fun ApplicantStatusCard(
    application: ScrimApplication,
    scrim: Scrim,
    onCancel: () -> Unit,
    onNavigateToChat: ((String) -> Unit)?,
    onNavigateToRoster: ((String, String) -> Unit)?,
    onMarkReady: ((String, String) -> Unit)?,
    onUploadScreenshot: ((String, String, String) -> Unit)?,
    onCompleteScrim: ((String, String?) -> Unit)?
) {
    val statusColor = when (application.status) {
        ApplicationStatus.PENDING -> WarningOrange
        ApplicationStatus.APPROVED -> SuccessGreen
        ApplicationStatus.REJECTED -> ErrorRed
        ApplicationStatus.CANCELLED -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val statusText = when (application.status) {
        ApplicationStatus.PENDING -> stringResource(R.string.application_pending)
        ApplicationStatus.APPROVED -> stringResource(R.string.application_approved)
        ApplicationStatus.REJECTED -> stringResource(R.string.application_rejected)
        ApplicationStatus.CANCELLED -> stringResource(R.string.application_cancelled)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, appBorderColor(), RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(statusColor.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (application.status) {
                            ApplicationStatus.PENDING -> Icons.Default.HourglassEmpty
                            ApplicationStatus.APPROVED -> Icons.Default.CheckCircle
                            ApplicationStatus.REJECTED -> Icons.Default.Cancel
                            ApplicationStatus.CANCELLED -> Icons.Default.Cancel
                        },
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(statusText, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = statusColor)
                    if (application.status == ApplicationStatus.APPROVED) {
                        Text(stringResource(R.string.scrim_team_label, scrim.teamName), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
                    }
                }
            }

            if (application.status == ApplicationStatus.APPROVED) {
                Spacer(modifier = Modifier.height(12.dp))
                if (scrim.isChatOpen) {
                    GradientButton(
                        text = stringResource(R.string.open_chat),
                        onClick = { scrim.conversationId?.let { onNavigateToChat?.invoke(it) } },
                        gradient = PremiumBlueGradient,
                        height = 48.dp
                    )
                } else {
                    ChatGateCountdown(timeUntilOpens = scrim.timeUntilChatOpens)
                }
            }

            if (application.status == ApplicationStatus.PENDING) {
                Spacer(modifier = Modifier.height(12.dp))
                TextButton(onClick = onCancel, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text(stringResource(R.string.withdraw_application), color = ErrorRed, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
private fun ScrimStatusCard(scrim: Scrim) {
    val (label, color) = when (scrim.status) {
        ScrimStatus.OPEN, ScrimStatus.PENDING -> stringResource(R.string.scrim_status_open) to SuccessGreen
        ScrimStatus.FILLED -> stringResource(R.string.scrim_status_filled) to WarningOrange
        ScrimStatus.READY_CHECK -> stringResource(R.string.ready_check) to WarningOrange
        ScrimStatus.IN_PROGRESS -> stringResource(R.string.scrim_status_in_progress) to MaterialTheme.colorScheme.primary
        ScrimStatus.COMPLETED -> stringResource(R.string.scrim_status_completed) to MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
        ScrimStatus.CANCELLED -> stringResource(R.string.scrim_status_cancelled) to ErrorRed
        else -> stringResource(R.string.scrim_status_open) to SuccessGreen
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, appBorderColor(), RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(color, CircleShape)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.scrim_label, label),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = color
            )
        }
    }
}

@Composable
private fun OpponentActions(
    scrim: Scrim,
    currentTeamId: String?,
    isLoading: Boolean = false,
    onNavigateToChat: ((String) -> Unit)?,
    onNavigateToRoster: ((String, String) -> Unit)?,
    onMarkReady: ((String, String) -> Unit)?,
    onUploadScreenshot: ((String, String, String) -> Unit)?,
    onUploadGameScreenshot: ((String, String, Int, String) -> Unit)? = null,
    onSelectGameWinner: ((String, Int, String) -> Unit)? = null,
    onChangeSeriesFormat: ((String, Int) -> Unit)? = null,
    onCompleteScrim: ((String, String?) -> Unit)?
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        when (scrim.status) {
            ScrimStatus.FILLED -> {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, appBorderColor(), RoundedCornerShape(20.dp)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(SuccessGreen.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.CheckCircle, null, tint = SuccessGreen, modifier = Modifier.size(24.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(stringResource(R.string.scrim_team_label, scrim.teamName), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
                                Text(stringResource(R.string.application_approved), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (currentTeamId != null && onNavigateToRoster != null) {
                            GradientButton(
                                text = stringResource(R.string.select_roster),
                                onClick = { onNavigateToRoster(scrim.id, currentTeamId) },
                                gradient = BlueGradient,
                                height = 48.dp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        if (scrim.isChatOpen) {
                            GradientButton(
                                text = stringResource(R.string.open_chat),
                                onClick = { scrim.conversationId?.let { onNavigateToChat?.invoke(it) } },
                                gradient = PremiumBlueGradient,
                                height = 48.dp
                            )
                        } else {
                            ChatGateCountdown(timeUntilOpens = scrim.timeUntilChatOpens)
                        }
                    }
                }
            }

            ScrimStatus.READY_CHECK -> {
                ReadyCheckSection(
                    scrim = scrim,
                    currentTeamId = currentTeamId,
                    isLoading = isLoading,
                    onMarkReady = onMarkReady,
                    onNavigateToChat = onNavigateToChat
                )
            }

            ScrimStatus.IN_PROGRESS -> {
                InProgressSection(
                    scrim = scrim,
                    currentTeamId = currentTeamId,
                    isLoading = isLoading,
                    onUploadScreenshot = onUploadScreenshot,
                    onUploadGameScreenshot = onUploadGameScreenshot,
                    onSelectGameWinner = onSelectGameWinner,
                    onChangeSeriesFormat = onChangeSeriesFormat,
                    onCompleteScrim = onCompleteScrim,
                    onNavigateToChat = onNavigateToChat
                )
            }

            ScrimStatus.COMPLETED -> ScrimStatusCard(scrim)
            ScrimStatus.CANCELLED -> ScrimStatusCard(scrim)
            else -> ScrimStatusCard(scrim)
        }
    }
}

@Composable
private fun TeamPickerDialog(
    teams: List<Team>,
    onTeamSelected: (Team) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.background,
        title = {
            Text(
                text = stringResource(R.string.select_team_apply),
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                teams.forEach { team ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clickable { onTeamSelected(team) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    team.name.firstOrNull()?.uppercaseChar()?.toString() ?: "T",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = team.name,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = stringResource(R.string.team_players_count, team.currentPlayerCount, team.maxPlayers),
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}

@Composable
private fun PlayerPickerDialog(
    team: Team,
    selectedPlayerIds: Set<String>,
    onPlayerToggle: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val minPlayers = team.minPlayers
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.background,
        title = {
            Text(
                text = stringResource(R.string.select_roster_players, team.name),
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text = stringResource(R.string.select_at_least_players, minPlayers),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                team.players.forEach { player ->
                    val isSelected = player.id in selectedPlayerIds
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent)
                            .clickable { onPlayerToggle(player.id) }
                            .padding(horizontal = 8.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { onPlayerToggle(player.id) },
                            colors = CheckboxDefaults.colors(
                                checkedColor = MaterialTheme.colorScheme.primary,
                                uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                checkmarkColor = White
                            )
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = player.name,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = player.role.name.lowercase().replaceFirstChar { it.uppercase() },
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = selectedPlayerIds.size >= minPlayers
            ) {
                Text(
                    stringResource(R.string.confirm_players, selectedPlayerIds.size),
                    color = if (selectedPlayerIds.size >= minPlayers) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}

@Composable
private fun ChatGateCountdown(timeUntilOpens: Long) {
    var remaining by remember { mutableLongStateOf(timeUntilOpens) }

    LaunchedEffect(timeUntilOpens) {
        remaining = timeUntilOpens
        while (remaining > 0) {
            kotlinx.coroutines.delay(1000)
            remaining = (remaining - 1000).coerceAtLeast(0)
        }
    }

    val hours = TimeUnit.MILLISECONDS.toHours(remaining)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(remaining) % 60
    val seconds = TimeUnit.MILLISECONDS.toSeconds(remaining) % 60

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.LockClock,
                contentDescription = null,
                tint = WarningOrange,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.chat_opens_in),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = String.format("%02d:%02d:%02d", hours, minutes, seconds),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = WarningOrange,
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.chat_unlocks_2h),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f).copy(alpha = 0.6f)
            )
        }
    }
}

fun formatDetailedTime(
    timestamp: Long,
    regionTimeZoneId: String? = null,
    regionDisplayName: String? = null
): String {
    val tz = if (regionTimeZoneId != null)
        java.util.TimeZone.getTimeZone(regionTimeZoneId)
    else
        java.util.TimeZone.getTimeZone("UTC")
    val sdf = SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault())
    sdf.timeZone = tz
    val formatted = sdf.format(Date(timestamp))
    return if (regionDisplayName != null) "$formatted ($regionDisplayName)" else formatted
}

@Composable
private fun ReadyCheckSection(
    scrim: Scrim,
    currentTeamId: String?,
    isLoading: Boolean,
    onMarkReady: ((String, String) -> Unit)?,
    onNavigateToChat: ((String) -> Unit)?
) {
    val isTeamA = currentTeamId == scrim.teamId
    val myTeamReady = if (isTeamA) scrim.teamAReady else scrim.teamBReady
    val opponentReady = if (isTeamA) scrim.teamBReady else scrim.teamAReady

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, appBorderColor(), RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
            val pulseAlpha by infiniteTransition.animateFloat(
                initialValue = 0.4f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(800, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ), label = "pulseAlpha"
            )

            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(
                        color = WarningOrange.copy(alpha = pulseAlpha * 0.2f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = WarningOrange,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.match_time),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = WarningOrange
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ReadyIndicator(
                    teamName = scrim.teamName,
                    isReady = scrim.teamAReady
                )
                ReadyIndicator(
                    teamName = scrim.opponentTeamName ?: stringResource(R.string.opponent_label),
                    isReady = scrim.teamBReady
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (!myTeamReady && currentTeamId != null && onMarkReady != null) {
                GradientButton(
                    text = stringResource(R.string.ready),
                    onClick = { onMarkReady(scrim.id, currentTeamId) },
                    isLoading = isLoading,
                    gradient = PremiumBlueGradient,
                    enabled = !isLoading,
                    height = 56.dp
                )
            } else if (myTeamReady) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = SuccessGreen.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, null, tint = SuccessGreen, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.you_are_ready), color = SuccessGreen, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            if (!opponentReady) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.waiting_opponent_ready),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (scrim.isChatOpen) {
                GradientButton(
                    text = stringResource(R.string.open_chat),
                    onClick = { scrim.conversationId?.let { onNavigateToChat?.invoke(it) } },
                    gradient = BlueGradient,
                    height = 44.dp
                )
            }
        }
    }
}

@Composable
private fun ReadyIndicator(
    teamName: String,
    isReady: Boolean
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    color = if (isReady) SuccessGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isReady) Icons.Default.Check else Icons.Default.Close,
                contentDescription = null,
                tint = if (isReady) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = teamName.take(10),
            fontSize = 11.sp,
            color = if (isReady) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

@Composable
private fun InProgressSection(
    scrim: Scrim,
    currentTeamId: String?,
    isLoading: Boolean,
    onUploadScreenshot: ((String, String, String) -> Unit)?,
    onUploadGameScreenshot: ((String, String, Int, String) -> Unit)?,
    onSelectGameWinner: ((String, Int, String) -> Unit)?,
    onChangeSeriesFormat: ((String, Int) -> Unit)?,
    onCompleteScrim: ((String, String?) -> Unit)?,
    onNavigateToChat: ((String) -> Unit)?
) {
    val isTeamA = currentTeamId == scrim.teamId
    val totalGames = scrim.bestOf.games
    val gamesWithBothScreenshots = scrim.gamesWithBothScreenshots
    val gamesWithWinner = scrim.gamesWithWinner
    val canCompleteSeries = scrim.canCompleteScrim
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var activeUploadGame by remember { mutableStateOf<Int?>(null) }
    var isUploading by remember { mutableStateOf(false) }
    var uploadError by remember { mutableStateOf<String?>(null) }
    var showSeriesWinnerDialog by remember { mutableStateOf(false) }
    var showChangeFormatDialog by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        val gameNum = activeUploadGame
        if (uri != null && currentTeamId != null && gameNum != null) {
            isUploading = true
            uploadError = null
            coroutineScope.launch {
                try {
                    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    if (bytes != null) {
                        val compressedBytes = com.scrimslegends.app.util.ImageUtils.compressImage(bytes)
                        val path = "screenshots/${scrim.id}_${currentTeamId}_game${gameNum}_${System.currentTimeMillis()}.jpg"
                        val result = SupabaseStorageUpload.uploadFile(
                            bucket = SupabaseConfig.BUCKET_SCREENSHOTS,
                            path = path,
                            fileBytes = compressedBytes,
                            contentType = "image/jpeg"
                        )
                        result.onSuccess { url ->
                            onUploadGameScreenshot?.invoke(scrim.id, currentTeamId, gameNum, url)
                        }.onFailure { error ->
                            uploadError = error.message
                        }
                    } else {
                        uploadError = context.getString(R.string.error_failed_read_image)
                    }
                } catch (e: Exception) {
                    uploadError = e.message
                } finally {
                    isUploading = false
                    activeUploadGame = null
                }
            }
        }
    }

    val confirmedGames = scrim.gamesWithWinner
    val availableFormats = when (scrim.bestOf) {
        BestOf.BO5 -> listOf(BestOf.BO3, BestOf.BO2, BestOf.BO1).filter { it.games >= confirmedGames }
        BestOf.BO3 -> listOf(BestOf.BO2, BestOf.BO1).filter { it.games >= confirmedGames }
        BestOf.BO2 -> listOf(BestOf.BO1).filter { it.games >= confirmedGames }
        else -> emptyList()
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, appBorderColor(), RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.match_in_progress),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Series progress bar
            if (totalGames > 1) {
                SeriesProgressBar(
                    totalGames = totalGames,
                    gamesWithBothScreenshots = gamesWithBothScreenshots,
                    gamesWithWinner = gamesWithWinner
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Per-game cards
            if (scrim.gameResults.isNotEmpty()) {
                scrim.gameResults.sortedBy { it.gameNumber }.forEach { gameResult ->
                    GameResultCard(
                        gameResult = gameResult,
                        scrim = scrim,
                        isTeamA = isTeamA,
                        currentTeamId = currentTeamId,
                        isUploading = isUploading && activeUploadGame == gameResult.gameNumber,
                        onUploadClick = { gameNum ->
                            activeUploadGame = gameNum
                            imagePickerLauncher.launch("image/*")
                        },
                        onSelectWinner = { gameNum, winnerId ->
                            onSelectGameWinner?.invoke(scrim.id, gameNum, winnerId)
                        }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
            } else {
                // Legacy fallback: single screenshot upload for old scrims without game results
                LegacyScreenshotUpload(
                    scrim = scrim,
                    isTeamA = isTeamA,
                    currentTeamId = currentTeamId,
                    isUploading = isUploading,
                    onUploadScreenshot = onUploadScreenshot
                )
            }

            uploadError?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = it,
                    color = ErrorRed,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Complete Series button
            val seriesWinner = scrim.seriesWinnerTeamId
            GradientButton(
                text = when {
                    !canCompleteSeries -> "Complete all games first"
                    seriesWinner == null -> "Select all game winners"
                    else -> "Complete Series"
                },
                onClick = {
                    if (canCompleteSeries && seriesWinner != null) {
                        showSeriesWinnerDialog = true
                    }
                },
                enabled = canCompleteSeries && seriesWinner != null,
                isLoading = isLoading,
                gradient = if (canCompleteSeries && seriesWinner != null) PremiumBlueGradient else listOf(Color.Gray, Color.DarkGray),
                height = 48.dp
            )

            if (canCompleteSeries && seriesWinner != null) {
                Spacer(modifier = Modifier.height(4.dp))
                val winnerName = if (seriesWinner == scrim.teamId) scrim.teamName else (scrim.opponentTeamName ?: "Opponent")
                Text(
                    text = "$winnerName wins the series",
                    color = SuccessGreen,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Change format button (only if there are smaller valid formats)
            if (availableFormats.isNotEmpty()) {
                TextButton(
                    onClick = { showChangeFormatDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Change format (${scrim.bestOf.displayName} → shorter)",
                        color = WarningOrange,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Chat button
            if (scrim.isChatOpen) {
                GradientButton(
                    text = stringResource(R.string.open_chat),
                    onClick = { scrim.conversationId?.let { onNavigateToChat?.invoke(it) } },
                    gradient = BlueGradient,
                    height = 44.dp
                )
            }
        }
    }

    // Change format dialog
    if (showChangeFormatDialog) {
        AlertDialog(
            onDismissRequest = { showChangeFormatDialog = false },
            containerColor = MaterialTheme.colorScheme.background,
            title = {
                Text("Change Series Format?", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text(
                        "Teams can't continue playing. Choose a smaller format that includes the games already played.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    availableFormats.forEach { format ->
                        val isSelected = format == scrim.bestOf
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) WarningOrange.copy(alpha = 0.15f) else Color.Transparent)
                                .clickable {
                                    onChangeSeriesFormat?.invoke(scrim.id, format.games)
                                    showChangeFormatDialog = false
                                }
                                .padding(horizontal = 12.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = format.displayName,
                                color = if (isSelected) WarningOrange else White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f)
                            )
                            if (isSelected) {
                                Text("Current", color = WarningOrange, fontSize = 12.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showChangeFormatDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    // Series completion confirmation dialog
    if (showSeriesWinnerDialog) {
        val finalWinner = scrim.seriesWinnerTeamId
        val isTie = finalWinner == null && scrim.bestOf == BestOf.BO2
        AlertDialog(
            onDismissRequest = { showSeriesWinnerDialog = false },
            containerColor = MaterialTheme.colorScheme.background,
            title = {
                Text(
                    text = if (isTie) "Complete Series?" else "Complete Series?",
                    color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = if (isTie) {
                            "All ${scrim.bestOf.games} games have results. The series ended in a tie (1-1)."
                        } else {
                            "All ${scrim.bestOf.games} games have results. The series winner is ${if (finalWinner == scrim.teamId) scrim.teamName else (scrim.opponentTeamName ?: "Opponent")}."
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        fontSize = 14.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onCompleteScrim?.invoke(scrim.id, finalWinner)
                    showSeriesWinnerDialog = false
                }) {
                    Text("Confirm", color = SuccessGreen, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSeriesWinnerDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }
}

// ═════════════════════════════════════════════════════════════════
// SERIES PROGRESS BAR
// ═════════════════════════════════════════════════════════════════

@Composable
private fun SeriesProgressBar(
    totalGames: Int,
    gamesWithBothScreenshots: Int,
    gamesWithWinner: Int
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Series Progress",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "$gamesWithWinner/$totalGames decided",
                fontSize = 12.sp,
                color = if (gamesWithWinner == totalGames) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            repeat(totalGames) { index ->
                val gameNum = index + 1
                val hasWinner = index < gamesWithWinner
                val hasScreenshots = index < gamesWithBothScreenshots
                val color = when {
                    hasWinner -> SuccessGreen
                    hasScreenshots -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .background(color, RoundedCornerShape(3.dp))
                )
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════
// GAME RESULT CARD — Per-game screenshot + winner
// ═════════════════════════════════════════════════════════════════

@Composable
private fun GameResultCard(
    gameResult: ScrimGameResult,
    scrim: Scrim,
    isTeamA: Boolean,
    currentTeamId: String?,
    isUploading: Boolean,
    onUploadClick: (Int) -> Unit,
    onSelectWinner: (Int, String) -> Unit
) {
    val myScreenshot = if (isTeamA) gameResult.teamAScreenshotUrl else gameResult.teamBScreenshotUrl
    val opponentScreenshot = if (isTeamA) gameResult.teamBScreenshotUrl else gameResult.teamAScreenshotUrl

    val statusColor = when {
        gameResult.isDisputed -> ErrorRed
        gameResult.winnerTeamId != null || gameResult.adminOverrideWinnerId != null -> SuccessGreen
        gameResult.status == ScrimGameStatus.WINNER_SELECTED -> WarningOrange
        gameResult.bothScreenshotsUploaded -> MaterialTheme.colorScheme.primary
        myScreenshot != null -> WarningOrange
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val statusText = when {
        gameResult.isDisputed -> "Disputed — admin review required"
        gameResult.adminOverrideWinnerId != null -> "Winner confirmed by admin"
        gameResult.winnerTeamId != null -> "Winner: ${if (gameResult.winnerTeamId == scrim.teamId) scrim.teamName else (scrim.opponentTeamName ?: "Opponent")}"
        gameResult.status == ScrimGameStatus.WINNER_SELECTED -> "Awaiting opponent confirmation"
        gameResult.bothScreenshotsUploaded -> "Both uploaded — select winner"
        myScreenshot != null -> "Awaiting opponent screenshot"
        else -> "Upload screenshot"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Game header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(statusColor.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${gameResult.gameNumber}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusColor
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Game ${gameResult.gameNumber}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = statusText,
                    fontSize = 11.sp,
                    color = statusColor,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Screenshot row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // My team's screenshot slot
                ScreenshotSlot(
                    label = "Your team",
                    screenshotUrl = myScreenshot,
                    isUploading = isUploading,
                    onUploadClick = { onUploadClick(gameResult.gameNumber) },
                    canUpload = currentTeamId != null && myScreenshot == null,
                    modifier = Modifier.weight(1f)
                )

                // Opponent screenshot slot
                ScreenshotSlot(
                    label = "Opponent",
                    screenshotUrl = opponentScreenshot,
                    isUploading = false,
                    onUploadClick = {},
                    canUpload = false,
                    isReadOnly = true,
                    modifier = Modifier.weight(1f)
                )
            }

            // Winner selection — only show if both uploaded, not disputed, and current team hasn't picked yet
            val mySelection = if (isTeamA) gameResult.teamASelectedWinnerId else gameResult.teamBSelectedWinnerId
            val opponentSelection = if (isTeamA) gameResult.teamBSelectedWinnerId else gameResult.teamASelectedWinnerId
            val canSelectWinner = gameResult.bothScreenshotsUploaded
                    && !gameResult.isDisputed
                    && gameResult.winnerTeamId == null
                    && gameResult.adminOverrideWinnerId == null
                    && currentTeamId != null
                    && mySelection == null

            if (canSelectWinner) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Winner:",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                    WinnerChip(
                        text = scrim.teamName,
                        onClick = { onSelectWinner(gameResult.gameNumber, scrim.teamId) },
                        modifier = Modifier.weight(1f)
                    )
                    val opponentId = scrim.opponentTeamId
                    if (opponentId != null) {
                        WinnerChip(
                            text = scrim.opponentTeamName ?: "Opponent",
                            onClick = { onSelectWinner(gameResult.gameNumber, opponentId) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Show what the current team selected (waiting for opponent)
            if (mySelection != null && gameResult.winnerTeamId == null && gameResult.adminOverrideWinnerId == null && !gameResult.isDisputed) {
                Spacer(modifier = Modifier.height(8.dp))
                val myPickName = if (mySelection == scrim.teamId) scrim.teamName else (scrim.opponentTeamName ?: "Opponent")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CheckCircle, null, tint = WarningOrange, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Your team selected: $myPickName (awaiting opponent)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = WarningOrange
                    )
                }
            }

            // Show opponent's selection if they picked and current team hasn't
            if (opponentSelection != null && mySelection == null && !gameResult.isDisputed) {
                Spacer(modifier = Modifier.height(6.dp))
                val opponentPickName = if (opponentSelection == scrim.teamId) scrim.teamName else (scrim.opponentTeamName ?: "Opponent")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Info, null, tint = InfoBlue, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Opponent selected: $opponentPickName",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = InfoBlue
                    )
                }
            }

            // Dispute banner
            if (gameResult.isDisputed) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ErrorRed.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Warning, null, tint = ErrorRed, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Winner dispute — under admin review",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = ErrorRed
                    )
                }
            }

            // Show confirmed winner badge (from agreement or admin override)
            val confirmedWinnerId = gameResult.adminOverrideWinnerId ?: gameResult.winnerTeamId
            confirmedWinnerId?.let { winnerId ->
                Spacer(modifier = Modifier.height(8.dp))
                val winnerName = if (winnerId == scrim.teamId) scrim.teamName else (scrim.opponentTeamName ?: "Opponent")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.EmojiEvents, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "$winnerName won${if (gameResult.adminOverrideWinnerId != null) " (admin)" else ""}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════
// SCREENSHOT SLOT — Upload or preview
// ═════════════════════════════════════════════════════════════════

@Composable
private fun ScreenshotSlot(
    label: String,
    screenshotUrl: String?,
    isUploading: Boolean,
    onUploadClick: () -> Unit,
    canUpload: Boolean,
    isReadOnly: Boolean = false,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(90.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (screenshotUrl != null) SuccessGreen.copy(alpha = 0.06f)
            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f)
        ),
        shape = RoundedCornerShape(10.dp),
        onClick = { if (canUpload && !isUploading) onUploadClick() }
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            when {
                isUploading -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.primary, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Uploading…", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
                    }
                }
                screenshotUrl != null -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.CheckCircle, null, tint = SuccessGreen, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(label, fontSize = 11.sp, color = SuccessGreen, fontWeight = FontWeight.Medium)
                        Text("Uploaded", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                    }
                }
                canUpload -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.PhotoCamera, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
                        Text("Tap to upload", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                    }
                }
                isReadOnly -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.HourglassEmpty, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Waiting…", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                    }
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════
// LEGACY SCREENSHOT UPLOAD — For old scrims without game results
// ═════════════════════════════════════════════════════════════════

@Composable
private fun LegacyScreenshotUpload(
    scrim: Scrim,
    isTeamA: Boolean,
    currentTeamId: String?,
    isUploading: Boolean,
    onUploadScreenshot: ((String, String, String) -> Unit)?
) {
    val myScreenshotUploaded = if (isTeamA) scrim.teamAScreenshotUrl != null else scrim.teamBScreenshotUrl != null
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var localUploading by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null && currentTeamId != null) {
            localUploading = true
            coroutineScope.launch {
                try {
                    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    if (bytes != null) {
                        val compressedBytes = com.scrimslegends.app.util.ImageUtils.compressImage(bytes)
                        val path = "screenshots/${scrim.id}_${currentTeamId}_${System.currentTimeMillis()}.jpg"
                        val result = SupabaseStorageUpload.uploadFile(
                            bucket = SupabaseConfig.BUCKET_SCREENSHOTS,
                            path = path,
                            fileBytes = compressedBytes,
                            contentType = "image/jpeg"
                        )
                        result.onSuccess { url ->
                            onUploadScreenshot?.invoke(scrim.id, currentTeamId, url)
                        }
                    }
                } catch (e: Exception) {
                    Timber.w("Screenshot upload failed: ${e.message}")
                }
                localUploading = false
            }
        }
    }

    if (!myScreenshotUploaded && currentTeamId != null && onUploadScreenshot != null) {
        if (localUploading) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.primary, strokeWidth = 2.dp)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Uploading…", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f), fontSize = 13.sp)
        } else {
            GradientButton(
                text = "Attach Screenshot",
                onClick = { imagePickerLauncher.launch("image/*") },
                gradient = BlueGradient,
                height = 48.dp
            )
        }
    } else if (myScreenshotUploaded) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SuccessGreen.copy(alpha = 0.1f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.CheckCircle, null, tint = SuccessGreen, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Screenshot uploaded", color = SuccessGreen, fontWeight = FontWeight.Medium)
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════
// ROSTER DISPLAY CARD — Shows active/substitute players
// ═════════════════════════════════════════════════════════════════

@Composable
private fun RosterDisplayCard(
    teamName: String,
    roster: List<ScrimRosterEntry>
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = teamName,
                style = iOSHeadline.copy(color = MaterialTheme.colorScheme.onSurface)
            )
            Spacer(modifier = Modifier.height(12.dp))

            val activePlayers = roster.filter { it.isActive }
            val substitutes = roster.filter { !it.isActive }

            if (activePlayers.isNotEmpty()) {
                Text(
                    text = "ACTIVE (${activePlayers.size})",
                    style = iOSCaption2.copy(color = SuccessGreen, fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(4.dp))
                activePlayers.forEach { entry ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(SuccessGreen.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = SuccessGreen,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(entry.playerName, style = iOSBody.copy(color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium))
                    }
                }
            }

            if (substitutes.isNotEmpty()) {
                if (activePlayers.isNotEmpty()) Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "SUBSTITUTES (${substitutes.size})",
                    style = iOSCaption2.copy(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(4.dp))
                substitutes.forEach { entry ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.PersonOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(entry.playerName, style = iOSBody.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, fontWeight = FontWeight.Medium))
                    }
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════
// WINNER CHIP — Small selectable team name chip
// ═════════════════════════════════════════════════════════════════

@Composable
private fun WinnerChip(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(SuccessGreen.copy(alpha = 0.15f))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = SuccessGreen,
            maxLines = 1
        )
    }
}

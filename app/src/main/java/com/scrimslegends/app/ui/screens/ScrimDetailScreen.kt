package com.scrimslegends.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import com.scrimslegends.app.data.service.SupabaseConfig
import com.scrimslegends.app.data.service.SupabaseStorageUpload
import com.scrimslegends.app.ui.theme.*
import com.scrimslegends.app.R
import androidx.compose.ui.res.stringResource
import com.scrimslegends.app.ui.components.AnimatedEntrance
import com.scrimslegends.app.ui.components.GlassBackButton
import com.scrimslegends.app.ui.components.GradientButton
import com.scrimslegends.app.ui.components.EnhancedStatusBadge
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@Composable
fun ScrimDetailScreen(
    scrim: Scrim,
    currentUserId: String,
    currentUserTeamId: String? = null,
    currentUserTeamName: String? = null,
    isTeamLeader: Boolean = false,
    teamHasMinPlayers: Boolean = false,
    onNavigateBack: () -> Unit,
    onJoinScrim: (String) -> Unit = {},
    onLeaveScrim: (String) -> Unit = {},
    onApplyScrim: (Scrim) -> Unit = {},
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
    onCompleteScrim: ((String, String) -> Unit)? = null      // scrimId, winnerTeamId
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
                        text = stringResource(R.string.scrim_details),
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
                                                colors = BlueGradient
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
                                        ScrimStatus.IN_PROGRESS -> BluePrimary
                                        ScrimStatus.COMPLETED -> LightGray
                                        ScrimStatus.CANCELLED -> ErrorRed
                                        else -> SuccessGreen
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
                            text = stringResource(R.string.scrim_info),
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
                            label = stringResource(R.string.game_mode),
                            value = scrim.gameMode.displayName
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
                            label = stringResource(R.string.region),
                            value = scrim.region.displayName
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
                            label = stringResource(R.string.skill_level),
                            value = scrim.skillLevel.name
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }

                item {
                    AnimatedEntrance(delayMillis = 375) {
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
                    AnimatedEntrance(delayMillis = 400) {
                        InfoCard(
                            icon = Icons.Default.AccessTime,
                            label = stringResource(R.string.scheduled_time),
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
                            label = stringResource(R.string.players),
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
                                text = stringResource(R.string.description),
                                style = iOSTitle2.copy(color = TextPrimary)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    item {
                        AnimatedEntrance(delayMillis = 550) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(SurfaceCard)
                                    .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
                            ) {
                                Text(
                                    text = scrim.description,
                                    style = iOSBody.copy(color = TextSecondary),
                                    modifier = Modifier.padding(20.dp)
                                )
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(28.dp))
                    }
                }

                // Rosters
                if (scrim.teamARoster.isNotEmpty() || scrim.teamBRoster.isNotEmpty()) {
                    item {
                        AnimatedEntrance(delayMillis = 580) {
                            Text(
                                text = stringResource(R.string.rosters),
                                style = iOSTitle2.copy(color = TextPrimary)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Team A Roster
                    if (scrim.teamARoster.isNotEmpty()) {
                        item {
                            AnimatedEntrance(delayMillis = 590) {
                                RosterDisplayCard(
                                    teamName = scrim.teamName,
                                    roster = scrim.teamARoster
                                )
                            }
                        }
                        item { Spacer(modifier = Modifier.height(12.dp)) }
                    }

                    // Team B Roster
                    if (scrim.teamBRoster.isNotEmpty()) {
                        item {
                            AnimatedEntrance(delayMillis = 600) {
                                RosterDisplayCard(
                                    teamName = scrim.opponentTeamName ?: stringResource(R.string.opponent_label),
                                    roster = scrim.teamBRoster
                                )
                            }
                        }
                        item { Spacer(modifier = Modifier.height(12.dp)) }
                    }
                }

                // ═══════════════════════════════════════════════════════
                // ACTION AREA — Team vs Team Application Flow + Ready/Screenshot/Complete
                // ═══════════════════════════════════════════════════════
                item {
                    val isHost = scrim.teamId == currentUserTeamId && isTeamLeader
                    val myApplication = scrim.applications.find { it.applicantTeamId == currentUserTeamId }
                    val isOpponent = scrim.opponentTeamId == currentUserTeamId
                    val hasPendingApps = scrim.applications.any { it.status == ApplicationStatus.PENDING }

                    AnimatedEntrance(delayMillis = 600) {
                        when {
                            // ── HOST VIEW ──
                            isHost -> HostActions(
                                scrim = scrim,
                                currentTeamId = currentUserTeamId,
                                onCancelScrim = { showCancelDialog = true },
                                onApprove = { appId ->
                                    onApproveApplication(scrim.id, appId)
                                },
                                onReject = { appId -> onRejectApplication(scrim.id, appId) },
                                onNavigateToChat = onNavigateToChat,
                                onNavigateToRoster = onNavigateToRoster,
                                onMarkReady = onMarkReady,
                                onUploadScreenshot = onUploadScreenshot,
                                onUploadGameScreenshot = onUploadGameScreenshot,
                                onSelectGameWinner = onSelectGameWinner,
                                onCompleteScrim = onCompleteScrim
                            )

                            // ── APPLICANT VIEW: Already applied ──
                            myApplication != null -> ApplicantStatusCard(
                                application = myApplication,
                                scrim = scrim,
                                onCancel = { onCancelApplication(scrim.id, myApplication.id) },
                                onNavigateToChat = onNavigateToChat,
                                onNavigateToRoster = onNavigateToRoster,
                                onMarkReady = onMarkReady,
                                onUploadScreenshot = onUploadScreenshot,
                                onCompleteScrim = onCompleteScrim
                            )

                            // ── VISITOR VIEW: Can apply ──
                            scrim.status == ScrimStatus.OPEN && isTeamLeader && teamHasMinPlayers -> {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    GradientButton(
                                        text = stringResource(R.string.apply_with_team),
                                        onClick = { onApplyScrim(scrim) },
                                        gradient = BlueGradient,
                                        height = 56.dp
                                    )
                                    if (currentUserTeamName != null) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = stringResource(R.string.applying_as, currentUserTeamName),
                                            fontSize = 13.sp,
                                            color = LightGray.copy(alpha = 0.6f),
                                            modifier = Modifier.align(Alignment.CenterHorizontally)
                                        )
                                    }
                                }
                            }

                            // ── VISITOR: No team or not enough players ──
                            scrim.status == ScrimStatus.OPEN && isTeamLeader && !teamHasMinPlayers -> {
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

                            // ── VISITOR: Not a team leader ──
                            scrim.status == ScrimStatus.OPEN -> {
                                GradientButton(
                                    text = stringResource(R.string.team_leaders_only),
                                    onClick = { },
                                    enabled = false,
                                    gradient = listOf(Color.Gray, Color.DarkGray),
                                    height = 56.dp
                                )
                            }

                            // ── FILLED / IN_PROGRESS / COMPLETED / CANCELLED ──
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

    // Cancel Scrim Dialog
    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            containerColor = DarkNavy,
            title = {
                Text(
                    text = stringResource(R.string.cancel_scrim),
                    color = White,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.cancel_scrim_confirm),
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
                    Text(stringResource(R.string.cancel_scrim), color = ErrorRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) {
                    Text(stringResource(R.string.keep_scrim), color = MidGray)
                }
            }
        )
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
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceCard)
            .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
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
                    .background(BluePrimary.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                    .border(1.dp, BluePrimary.copy(alpha = 0.22f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector        = icon,
                    contentDescription = label,
                    tint               = BluePrimary,
                    modifier           = Modifier.size(20.dp)
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text  = label,
                    style = iOSCaption1.copy(color = TextSecondary, fontWeight = FontWeight.Medium)
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text  = value,
                    style = iOSHeadline.copy(color = TextPrimary)
                )
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════
// HOST ACTIONS — Approve/Reject applications, view opponent
// ═════════════════════════════════════════════════════════════════

@Composable
private fun HostActions(
    scrim: Scrim,
    currentTeamId: String?,
    onCancelScrim: () -> Unit,
    onApprove: (String) -> Unit,
    onReject: (String) -> Unit,
    onNavigateToChat: ((String) -> Unit)?,
    onNavigateToRoster: ((String, String) -> Unit)?,
    onMarkReady: ((String, String) -> Unit)?,
    onUploadScreenshot: ((String, String, String) -> Unit)?,
    onUploadGameScreenshot: ((String, String, Int, String) -> Unit)? = null,
    onSelectGameWinner: ((String, Int, String) -> Unit)? = null,
    onCompleteScrim: ((String, String) -> Unit)?
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        when (scrim.status) {
            ScrimStatus.OPEN -> {
                // Show pending applications
                val pendingApps = scrim.applications.filter { it.status == ApplicationStatus.PENDING }
                if (pendingApps.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.pending_applications, pendingApps.size),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = White
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    pendingApps.forEach { app ->
                        ApplicationCard(
                            application = app,
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
                    gradient = listOf(ErrorRed, ErrorRed.copy(alpha = 0.7f)),
                    height = 56.dp
                )
            }

            ScrimStatus.FILLED -> {
                // Opponent accepted — show roster selection + chat
                scrim.opponentTeamName?.let { opponentName ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(4.dp, RoundedCornerShape(16.dp)),
                        colors = CardDefaults.cardColors(containerColor = DarkNavy),
                        shape = RoundedCornerShape(16.dp)
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
                                    Text(stringResource(R.string.opponent_label), fontSize = 13.sp, color = LightGray)
                                    Text(opponentName, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = White)
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Select roster button
                            if (currentTeamId != null && onNavigateToRoster != null) {
                                GradientButton(
                                    text = stringResource(R.string.select_roster),
                                    onClick = { onNavigateToRoster(scrim.id, currentTeamId) },
                                    gradient = BlueGradient,
                                    height = 48.dp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            // Chat gate countdown
                            if (scrim.isChatOpen) {
                                GradientButton(
                                    text = stringResource(R.string.open_chat),
                                    onClick = { scrim.conversationId?.let { onNavigateToChat?.invoke(it) } },
                                    gradient = GoldGradient,
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
                // Ready button appears at match time
                ReadyCheckSection(
                    scrim = scrim,
                    currentTeamId = currentTeamId,
                    onMarkReady = onMarkReady,
                    onNavigateToChat = onNavigateToChat
                )
            }

            ScrimStatus.IN_PROGRESS -> {
                // Both ready — show screenshot + complete
                InProgressSection(
                    scrim = scrim,
                    currentTeamId = currentTeamId,
                    onUploadScreenshot = onUploadScreenshot,
                    onUploadGameScreenshot = onUploadGameScreenshot,
                    onSelectGameWinner = onSelectGameWinner,
                    onCompleteScrim = onCompleteScrim,
                    onNavigateToChat = onNavigateToChat
                )
            }

            ScrimStatus.COMPLETED -> ScrimStatusCard(scrim)
            ScrimStatus.CANCELLED -> ScrimStatusCard(scrim)
        }
    }
}

// ═════════════════════════════════════════════════════════════════
// APPLICATION CARD — Shows a pending application with approve/reject
// ═════════════════════════════════════════════════════════════════

@Composable
private fun ApplicationCard(
    application: ScrimApplication,
    onApprove  : () -> Unit,
    onReject   : () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceCard)
            .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Team avatar
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Brush.linearGradient(BlueGradient)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        application.applicantTeamName.firstOrNull()?.uppercaseChar()?.toString() ?: "T",
                        fontSize   = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color      = White
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        application.applicantTeamName,
                        style = iOSHeadline.copy(color = TextPrimary)
                    )
                    Text(
                        stringResource(R.string.leader_name, application.applicantTeamLeaderName),
                        style = iOSCaption1.copy(color = TextSecondary)
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = GlassBorder)
            Spacer(Modifier.height(12.dp))

            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                GradientButton(
                    text     = stringResource(R.string.approve),
                    onClick  = onApprove,
                    gradient = SuccessGradient,
                    modifier = Modifier.weight(1f),
                    height   = 44.dp
                )
                GradientButton(
                    text     = stringResource(R.string.reject),
                    onClick  = onReject,
                    gradient = ErrorGradient,
                    modifier = Modifier.weight(1f),
                    height   = 44.dp
                )
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════
// APPLICANT STATUS CARD — Shows current application status
// ═════════════════════════════════════════════════════════════════

@Composable
private fun ApplicantStatusCard(
    application: ScrimApplication,
    scrim: Scrim,
    onCancel: () -> Unit,
    onNavigateToChat: ((String) -> Unit)?,
    onNavigateToRoster: ((String, String) -> Unit)?,
    onMarkReady: ((String, String) -> Unit)?,
    onUploadScreenshot: ((String, String, String) -> Unit)?,
    onCompleteScrim: ((String, String) -> Unit)?
) {
    val statusColor = when (application.status) {
        ApplicationStatus.PENDING -> WarningOrange
        ApplicationStatus.APPROVED -> SuccessGreen
        ApplicationStatus.REJECTED -> ErrorRed
        ApplicationStatus.CANCELLED -> MidGray
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
            .shadow(4.dp, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkNavy),
        shape = RoundedCornerShape(16.dp)
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
                        Text(stringResource(R.string.scrim_team_label, scrim.teamName), fontSize = 13.sp, color = LightGray)
                    }
                }
            }

            if (application.status == ApplicationStatus.APPROVED) {
                Spacer(modifier = Modifier.height(12.dp))
                if (scrim.isChatOpen) {
                    GradientButton(
                        text = stringResource(R.string.open_chat),
                        onClick = { scrim.conversationId?.let { onNavigateToChat?.invoke(it) } },
                        gradient = GoldGradient,
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

// ═════════════════════════════════════════════════════════════════
// SCRIM STATUS CARD — Read-only status display
// ═════════════════════════════════════════════════════════════════

@Composable
private fun ScrimStatusCard(scrim: Scrim) {
    val (label, color) = when (scrim.status) {
        ScrimStatus.OPEN -> stringResource(R.string.scrim_status_open) to SuccessGreen
        ScrimStatus.FILLED -> stringResource(R.string.scrim_status_filled) to WarningOrange
        ScrimStatus.READY_CHECK -> stringResource(R.string.ready_check) to WarningOrange
        ScrimStatus.IN_PROGRESS -> stringResource(R.string.scrim_status_in_progress) to BluePrimary
        ScrimStatus.COMPLETED -> stringResource(R.string.scrim_status_completed) to LightGray
        ScrimStatus.CANCELLED -> stringResource(R.string.scrim_status_cancelled) to ErrorRed
        else -> stringResource(R.string.scrim_status_open) to SuccessGreen
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkNavy),
        shape = RoundedCornerShape(16.dp)
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

// ═════════════════════════════════════════════════════════════════
// CHAT GATE COUNTDOWN — Shows timer until chat opens (2h before)
// ═════════════════════════════════════════════════════════════════

@Composable
private fun ChatGateCountdown(timeUntilOpens: Long) {
    var remaining by remember { mutableLongStateOf(timeUntilOpens) }

    LaunchedEffect(timeUntilOpens) {
        while (remaining > 0) {
            kotlinx.coroutines.delay(1000)
            if (remaining > 0) {
                remaining = (remaining - 1000).coerceAtLeast(0)
            }
        }
    }

    val hours = TimeUnit.MILLISECONDS.toHours(remaining)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(remaining) % 60
    val seconds = TimeUnit.MILLISECONDS.toSeconds(remaining) % 60

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
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
                color = LightGray
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
                color = LightGray.copy(alpha = 0.6f)
            )
        }
    }
}

fun formatDetailedTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

// ═════════════════════════════════════════════════════════════════
// READY CHECK SECTION — Both captains must press Ready
// ═════════════════════════════════════════════════════════════════

@Composable
private fun ReadyCheckSection(
    scrim: Scrim,
    currentTeamId: String?,
    onMarkReady: ((String, String) -> Unit)?,
    onNavigateToChat: ((String) -> Unit)?
) {
    val isTeamA = currentTeamId == scrim.teamId
    val myTeamReady = if (isTeamA) scrim.teamAReady else scrim.teamBReady
    val opponentReady = if (isTeamA) scrim.teamBReady else scrim.teamAReady

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkNavy),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Pulsing ready icon
            val infiniteTransition = rememberInfiniteTransition()
            val pulseAlpha by infiniteTransition.animateFloat(
                initialValue = 0.4f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(800, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                )
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

            // Ready status indicators
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

            // Ready button (only if not already ready)
            if (!myTeamReady && currentTeamId != null && onMarkReady != null) {
                GradientButton(
                    text = stringResource(R.string.ready),
                    onClick = { onMarkReady(scrim.id, currentTeamId) },
                    gradient = GoldGradient,
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
                    color = LightGray
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

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
                    color = if (isReady) SuccessGreen.copy(alpha = 0.15f) else MidGray.copy(alpha = 0.1f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isReady) Icons.Default.Check else Icons.Default.Close,
                contentDescription = null,
                tint = if (isReady) SuccessGreen else MidGray,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = teamName.take(10),
            fontSize = 11.sp,
            color = if (isReady) SuccessGreen else MidGray,
            maxLines = 1
        )
    }
}

// ═════════════════════════════════════════════════════════════════
// IN PROGRESS SECTION — Per-game screenshot upload + series result
// ═════════════════════════════════════════════════════════════════

@Composable
private fun InProgressSection(
    scrim: Scrim,
    currentTeamId: String?,
    onUploadScreenshot: ((String, String, String) -> Unit)?,
    onUploadGameScreenshot: ((String, String, Int, String) -> Unit)?,
    onSelectGameWinner: ((String, Int, String) -> Unit)?,
    onCompleteScrim: ((String, String) -> Unit)?,
    onNavigateToChat: ((String) -> Unit)?
) {
    val isTeamA = currentTeamId == scrim.teamId
    val totalGames = scrim.bestOf.games
    val gamesReady = scrim.gameResults.size
    val gamesWithBothScreenshots = scrim.gamesWithBothScreenshots
    val gamesWithWinner = scrim.gamesWithWinner
    val canCompleteSeries = scrim.canCompleteScrim
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Track which game is being uploaded
    var activeUploadGame by remember { mutableStateOf<Int?>(null) }
    var isUploading by remember { mutableStateOf(false) }
    var uploadError by remember { mutableStateOf<String?>(null) }
    var showSeriesWinnerDialog by remember { mutableStateOf(false) }

    // Create game result rows if they don't exist yet (BO1 legacy or newly-filled scrims)
    // In a real app this is handled by the backend on scrim creation/fill.
    // Here we just show the UI based on what the backend provides.

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        val gameNum = activeUploadGame
        if (uri != null && currentTeamId != null && gameNum != null) {
            isUploading = true
            uploadError = null
            coroutineScope.launch {
                try {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val bytes = inputStream?.readBytes()
                    inputStream?.close()
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

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkNavy),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header: Match in progress + series progress
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(BluePrimary, CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.match_in_progress),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = BluePrimary
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
                gradient = if (canCompleteSeries && seriesWinner != null) GoldGradient else listOf(Color.Gray, Color.DarkGray),
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

    // Series completion confirmation dialog
    if (showSeriesWinnerDialog) {
        val finalWinner = scrim.seriesWinnerTeamId
        AlertDialog(
            onDismissRequest = { showSeriesWinnerDialog = false },
            containerColor = DarkNavy,
            title = {
                Text("Complete Series?", color = White, fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text(
                        "All ${scrim.bestOf.games} games have results. The series winner is ${if (finalWinner == scrim.teamId) scrim.teamName else (scrim.opponentTeamName ?: "Opponent")}.",
                        color = LightGray,
                        fontSize = 14.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    finalWinner?.let { onCompleteScrim?.invoke(scrim.id, it) }
                    showSeriesWinnerDialog = false
                }) {
                    Text("Confirm", color = SuccessGreen, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSeriesWinnerDialog = false }) {
                    Text("Cancel", color = MidGray)
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
                color = White
            )
            Text(
                text = "$gamesWithWinner/$totalGames decided",
                fontSize = 12.sp,
                color = if (gamesWithWinner == totalGames) SuccessGreen else LightGray
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
                    hasScreenshots -> BluePrimary
                    else -> White.copy(alpha = 0.15f)
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
    var showWinnerPicker by remember { mutableStateOf(false) }

    val statusColor = when {
        gameResult.winnerTeamId != null -> SuccessGreen
        gameResult.bothScreenshotsUploaded -> BluePrimary
        myScreenshot != null -> WarningOrange
        else -> MidGray
    }

    val statusText = when {
        gameResult.winnerTeamId != null -> "Winner: ${if (gameResult.winnerTeamId == scrim.teamId) scrim.teamName else (scrim.opponentTeamName ?: "Opponent")}"
        gameResult.bothScreenshotsUploaded -> "Both uploaded — select winner"
        myScreenshot != null -> "Awaiting opponent screenshot"
        else -> "Upload screenshot"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
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
                        color = White
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

            // Winner selection (only when both screenshots uploaded and no winner yet)
            if (gameResult.bothScreenshotsUploaded && gameResult.winnerTeamId == null && currentTeamId != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Winner:",
                        fontSize = 13.sp,
                        color = LightGray,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                    WinnerChip(
                        text = scrim.teamName,
                        onClick = { onSelectWinner(gameResult.gameNumber, scrim.teamId) },
                        modifier = Modifier.weight(1f)
                    )
                    if (scrim.opponentTeamId != null) {
                        WinnerChip(
                            text = scrim.opponentTeamName ?: "Opponent",
                            onClick = { onSelectWinner(gameResult.gameNumber, scrim.opponentTeamId!!) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Show winner badge if selected
            gameResult.winnerTeamId?.let { winnerId ->
                Spacer(modifier = Modifier.height(8.dp))
                val winnerName = if (winnerId == scrim.teamId) scrim.teamName else (scrim.opponentTeamName ?: "Opponent")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.EmojiEvents, null, tint = GoldPrimary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "$winnerName won",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = GoldPrimary
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
            else White.copy(alpha = 0.04f)
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
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = BluePrimary, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Uploading…", fontSize = 11.sp, color = LightGray)
                    }
                }
                screenshotUrl != null -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.CheckCircle, null, tint = SuccessGreen, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(label, fontSize = 11.sp, color = SuccessGreen, fontWeight = FontWeight.Medium)
                        Text("Uploaded", fontSize = 10.sp, color = TextTertiary)
                    }
                }
                canUpload -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.PhotoCamera, null, tint = BluePrimary, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(label, fontSize = 11.sp, color = LightGray)
                        Text("Tap to upload", fontSize = 10.sp, color = TextTertiary)
                    }
                }
                isReadOnly -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.HourglassEmpty, null, tint = MidGray, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(label, fontSize = 11.sp, color = MidGray)
                        Text("Waiting…", fontSize = 10.sp, color = TextTertiary)
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
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val bytes = inputStream?.readBytes()
                    inputStream?.close()
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
                } catch (_: Exception) { }
                localUploading = false
            }
        }
    }

    if (!myScreenshotUploaded && currentTeamId != null && onUploadScreenshot != null) {
        if (localUploading) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = BluePrimary, strokeWidth = 2.dp)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Uploading…", color = LightGray, fontSize = 13.sp)
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
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceCard)
            .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = teamName,
                style = iOSHeadline.copy(color = TextPrimary)
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
                        modifier = Modifier.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(SuccessGreen.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = SuccessGreen,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(entry.playerName, style = iOSBody.copy(color = TextPrimary, fontSize = 14.sp))
                    }
                }
            }

            if (substitutes.isNotEmpty()) {
                if (activePlayers.isNotEmpty()) Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "SUBSTITUTES (${substitutes.size})",
                    style = iOSCaption2.copy(color = TextTertiary, fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(4.dp))
                substitutes.forEach { entry ->
                    Row(
                        modifier = Modifier.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.PersonOutline,
                            contentDescription = null,
                            tint = TextTertiary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(entry.playerName, style = iOSBody.copy(color = TextSecondary, fontSize = 14.sp))
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

package com.mlbb.scrim.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalUriHandler
import com.mlbb.scrim.data.model.*
import com.mlbb.scrim.ui.theme.*
import com.mlbb.scrim.ui.components.*
import com.mlbb.scrim.R
import androidx.compose.ui.res.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TournamentDetailScreen(
    tournament: Tournament?,
    requirements: List<TournamentRequirement>,
    teams: List<TournamentTeam>,
    matches: List<TournamentSwissMatch>,
    roomSecret: TournamentMatchRoomSecret? = null,
    isLoading: Boolean,
    error: String? = null,
    myTeams: List<Team> = emptyList(),
    myApplications: List<TournamentApplication> = emptyList(),
    isHost: Boolean = false,
    onNavigateBack: () -> Unit,
    onApply: (String, String) -> Unit = { _, _ -> },
    onCheckIn: (String, String) -> Unit = { _, _ -> },
    onRefresh: () -> Unit = {},
    onDismissError: () -> Unit = {},
    onNavigateToChat: (String) -> Unit = {},
    onNavigateToEdit: (String) -> Unit = {},
    // Host actions
    onGeneratePairings: (String) -> Unit = {},
    onReviewApplication: (String, Boolean, String?) -> Unit = { _, _, _ -> },
    onSubmitMatchResult: (String, String?, Boolean) -> Unit = { _, _, _ -> },
    onCancelTournament: (String, String?) -> Unit = { _, _ -> },
    onCompleteTournament: (String) -> Unit = {},
    onDisqualifyTeam: (String, String, String) -> Unit = { _, _, _ -> },
    onLoadRoomSecret: (String) -> Unit = {},
    onResolveDispute: (matchId: String, winnerTeamId: String?, isDraw: Boolean, resolution: String) -> Unit = { _, _, _, _ -> }
) {
    val t = tournament ?: return

    var showApplyDialog by remember { mutableStateOf(false) }
    var selectedTeamId by remember { mutableStateOf<String?>(null) }
    var showCancelDialog by remember { mutableStateOf(false) }
    var showResultDialog by remember { mutableStateOf(false) }
    var selectedMatchId by remember { mutableStateOf<String?>(null) }
    var showDqDialog by remember { mutableStateOf(false) }
    var selectedDqTeamId by remember { mutableStateOf<String?>(null) }
    var showDisputeDialog by remember { mutableStateOf(false) }
    var disputeMatchId by remember { mutableStateOf<String?>(null) }
    var disputeResolution by remember { mutableStateOf("") }
    var disputeWinnerTeamId by remember { mutableStateOf<String?>(null) }
    var disputeIsDraw by remember { mutableStateOf(false) }
    // selectedMatchTeams is used to display team names in the resolve dialog
    var disputeMatch by remember { mutableStateOf<TournamentSwissMatch?>(null) }

    // Check if user already applied
    val myApp = myApplications.find { it.tournamentId == t.id }
    val hasApplied = myApp != null

    // Status color
    val statusColor = when (t.status) {
        TournamentStatus.REGISTRATION -> SuccessGreen
        TournamentStatus.CHECK_IN -> WarningOrange
        TournamentStatus.IN_PROGRESS -> ErrorRed
        TournamentStatus.COMPLETED -> BluePrimary
        TournamentStatus.CANCELLED -> TextTertiary
        else -> TextTertiary
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = heroGradientBrush())
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ── Header ──
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = DarkNavy.copy(alpha = 0.5f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GlassBackButton(onClick = onNavigateBack)
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = stringResource(R.string.tournament_detail),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = White
                        )
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.size(44.dp)) // Balance
                }
            }

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = GoldPrimary, modifier = Modifier.size(48.dp))
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // ── Tournament Hero Card (Enhanced) ──
                    item {
                        TournamentHeroCard(tournament = t, statusColor = statusColor)
                    }

                    // ── Requirements ──
                    if (requirements.isNotEmpty()) {
                        item {
                            SectionHeader(title = stringResource(R.string.tournament_requirements), icon = Icons.Default.Checklist)
                        }
                        items(requirements) { req ->
                            RequirementRow(requirement = req)
                        }
                    }

                    // ── Winner Podium (completed tournaments) ──
                    if (t.isCompleted && teams.isNotEmpty()) {
                        item {
                            TournamentPodium(teams = teams)
                        }
                    }

                    // ── Standings ──
                    if (teams.isNotEmpty()) {
                        item {
                            SectionHeader(title = stringResource(R.string.tournament_standings), icon = Icons.Default.Leaderboard)
                        }
                        item {
                            StandingsTable(teams = teams)
                        }
                    }

                    // ── Matches (with round filter tabs) ──
                    if (matches.isNotEmpty()) {
                        item {
                            SectionHeader(title = stringResource(R.string.tournament_matches), icon = Icons.Default.SportsEsports)
                        }
                        item {
                            RoundFilteredMatches(
                                matches = matches,
                                isHost = isHost,
                                myTeamIds = myTeams.map { it.id },
                                bestOf = t.bestOf,
                                onNavigateToChat = onNavigateToChat,
                                onSubmitResult = { mid -> selectedMatchId = mid; showResultDialog = true },
                                onViewRoomSecret = onLoadRoomSecret,
                                onResolveDispute = { match ->
                                    disputeMatch = match
                                    disputeMatchId = match.id
                                    disputeWinnerTeamId = null
                                    disputeIsDraw = false
                                    disputeResolution = ""
                                    showDisputeDialog = true
                                }
                            )
                        }
                    }

                    // ── Room Secret ──
                    if (roomSecret != null) {
                        item {
                            SectionHeader(title = "Room Credentials", icon = Icons.Default.VideogameAsset)
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = SurfaceElevated),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    com.mlbb.scrim.ui.components.MaskedSecretField(
                                        label = "Room ID",
                                        value = roomSecret.roomId
                                    )
                                    roomSecret.roomPassword?.let { pwd ->
                                        Spacer(modifier = Modifier.height(8.dp))
                                        com.mlbb.scrim.ui.components.MaskedSecretField(
                                            label = "Password",
                                            value = pwd
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // ── Host Management Panel ──
                    if (isHost && t.status != TournamentStatus.COMPLETED && t.status != TournamentStatus.CANCELLED) {
                        item {
                            SectionHeader(title = "Host Actions", icon = Icons.Default.AdminPanelSettings)
                        }
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Edit Settings (registration phase only)
                                    if (t.isOpen) {
                                        HostActionButton(
                                            icon = Icons.Default.Edit,
                                            label = "Edit Settings",
                                            color = BluePrimary,
                                            onClick = { onNavigateToEdit(t.id) },
                                            enabled = !isLoading
                                        )
                                    }

                                    // Generate Pairings (check_in or in_progress with no matches yet)
                                    if ((t.isCheckIn || (t.isLive && matches.isEmpty())) && teams.count { it.checkedIn } >= 4) {
                                        HostActionButton(
                                            icon = Icons.Default.Casino,
                                            label = "Generate Round ${t.currentRound + 1} Pairings",
                                            color = GoldPrimary,
                                            onClick = { onGeneratePairings(t.id) },
                                            enabled = !isLoading
                                        )
                                    }

                                    // Pending applications (registration phase)
                                    val pendingApps = myApplications.filter {
                                        it.tournamentId == t.id && it.status == TournamentApplicationStatus.PENDING
                                    }
                                    if (t.isOpen && pendingApps.isNotEmpty()) {
                                        HostActionButton(
                                            icon = Icons.Default.HowToReg,
                                            label = "Review Applications (${pendingApps.size})",
                                            color = BluePrimary,
                                            onClick = { /* Show review UI */ },
                                            enabled = !isLoading
                                        )
                                        pendingApps.forEach { app ->
                                            ApplicationReviewRow(
                                                app = app,
                                                onApprove = { onReviewApplication(app.id, true, null) },
                                                onReject = { onReviewApplication(app.id, false, "Rejected by host") }
                                            )
                                        }
                                    }

                                    // Complete Tournament
                                    if (t.isLive && matches.all { it.status == MatchStatus.COMPLETED || it.status == MatchStatus.CANCELLED || it.status == MatchStatus.BYE } && matches.isNotEmpty()) {
                                        HostActionButton(
                                            icon = Icons.Default.EmojiEvents,
                                            label = "Complete Tournament",
                                            color = SuccessGreen,
                                            onClick = { onCompleteTournament(t.id) },
                                            enabled = !isLoading
                                        )
                                    }

                                    // Cancel Tournament
                                    if (t.status != TournamentStatus.DRAFT) {
                                        HostActionButton(
                                            icon = Icons.Default.Cancel,
                                            label = "Cancel Tournament",
                                            color = ErrorRed,
                                            onClick = { showCancelDialog = true },
                                            enabled = !isLoading
                                        )
                                    }

                                    // Disqualify Team (in_progress)
                                    if (t.isLive) {
                                        val activeTeams = teams.filter { !it.isDisqualified }
                                        if (activeTeams.isNotEmpty()) {
                                            HostActionButton(
                                                icon = Icons.Default.PersonOff,
                                                label = "Disqualify Team",
                                                color = WarningOrange,
                                                onClick = { showDqDialog = true },
                                                enabled = !isLoading
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ── Check-in Button (for team leaders in check_in phase) ──
                    if (t.isCheckIn) {
                        val myCheckedInTeamIds = teams.filter { it.checkedIn }.map { it.teamId }
                        val myTeamsInTournament = myTeams.filter { team ->
                            teams.any { it.teamId == team.id }
                        }
                        val uncheckedTeams = myTeamsInTournament.filter { it.id !in myCheckedInTeamIds }

                        if (uncheckedTeams.isNotEmpty()) {
                            item {
                                SectionHeader(title = "Check In", icon = Icons.Default.CheckCircle)
                            }
                            uncheckedTeams.forEach { team ->
                                item {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = WarningOrange.copy(alpha = 0.1f)),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = team.name,
                                                    style = MaterialTheme.typography.bodyMedium.copy(
                                                        color = White, fontWeight = FontWeight.SemiBold
                                                    )
                                                )
                                                Text(
                                                    text = stringResource(R.string.tournament_not_checked_in),
                                                    style = MaterialTheme.typography.labelSmall.copy(color = LightGray.copy(alpha = 0.6f))
                                                )
                                            }
                                            Button(
                                                onClick = { onCheckIn(t.id, team.id) },
                                                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text("Check In", color = DarkNavy, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        } else if (myTeamsInTournament.isNotEmpty()) {
                            item {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    color = SuccessGreen.copy(alpha = 0.1f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Default.CheckCircle, null, tint = SuccessGreen)
                                        Text("All your teams are checked in!", style = MaterialTheme.typography.bodyMedium.copy(color = SuccessGreen))
                                    }
                                }
                            }
                        }
                    }

                    // ── Apply button ──
                    if (t.isOpen && !hasApplied && myTeams.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            iOSPrimaryButton(
                                text = stringResource(R.string.tournament_apply),
                                onClick = { showApplyDialog = true },
                                backgroundColor = GoldPrimary,
                                contentColor = DarkNavy
                            )
                        }
                    } else if (myApp != null) {
                        item {
                            val app = myApp
                            val appStatusColor = when (app.status) {
                                TournamentApplicationStatus.PENDING -> WarningOrange
                                TournamentApplicationStatus.ACCEPTED -> SuccessGreen
                                TournamentApplicationStatus.REJECTED -> ErrorRed
                                TournamentApplicationStatus.BLOCKED -> ErrorRed
                            }
                            val appStatusText = when (app.status) {
                                TournamentApplicationStatus.PENDING -> stringResource(R.string.tournament_applied)
                                TournamentApplicationStatus.ACCEPTED -> stringResource(R.string.tournament_accepted)
                                TournamentApplicationStatus.REJECTED -> stringResource(R.string.tournament_rejected)
                                TournamentApplicationStatus.BLOCKED -> stringResource(R.string.tournament_blocked)
                            }
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = appStatusColor.copy(alpha = 0.15f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = when (app.status) {
                                            TournamentApplicationStatus.ACCEPTED -> Icons.Default.CheckCircle
                                            TournamentApplicationStatus.REJECTED, TournamentApplicationStatus.BLOCKED -> Icons.Default.Cancel
                                            else -> Icons.Default.Schedule
                                        },
                                        contentDescription = null,
                                        tint = appStatusColor
                                    )
                                    Text(
                                        text = appStatusText,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = appStatusColor,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // Bottom padding
                    item { Spacer(modifier = Modifier.height(32.dp)) }
                }
            }
        }

        // ── Apply Bottom Sheet ──
        if (showApplyDialog) {
            AlertDialog(
                onDismissRequest = { showApplyDialog = false },
                containerColor = SurfaceCard,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 24.dp, bottomEnd = 24.dp),
                title = {
                    Column {
                        // Drag handle style header
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Box(modifier = Modifier.width(40.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(Separator))
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(
                                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(GoldPrimary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.EmojiEvents, null, tint = GoldPrimary, modifier = Modifier.size(22.dp))
                            }
                            Column {
                                Text(
                                    text = stringResource(R.string.tournament_apply_confirm),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = White)
                                )
                                Text(
                                    text = t.title,
                                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
                                    maxLines = 1, overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                },
                text = {
                    Column {
                        Text(
                            text = stringResource(R.string.tournament_select_team),
                            style = MaterialTheme.typography.labelMedium.copy(color = TextTertiary, fontWeight = FontWeight.SemiBold)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        myTeams.forEach { team ->
                            val isSelected = selectedTeamId == team.id
                            val animBorder by animateColorAsState(
                                if (isSelected) GoldPrimary else Separator,
                                label = "team_border"
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (isSelected) GoldPrimary.copy(alpha = 0.1f) else SurfaceElevated)
                                    .clickable { selectedTeamId = team.id }
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier.size(36.dp).clip(CircleShape)
                                        .background(if (isSelected) GoldPrimary.copy(alpha = 0.2f) else Separator.copy(alpha = 0.5f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(Icons.Default.Check, null, tint = GoldPrimary, modifier = Modifier.size(18.dp))
                                    } else {
                                        Icon(Icons.Default.Groups, null, tint = TextTertiary, modifier = Modifier.size(18.dp))
                                    }
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = team.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = if (isSelected) GoldPrimary else White,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        )
                                    )
                                    Text(
                                        text = "${team.currentPlayerCount}/${team.maxPlayers} players",
                                        style = MaterialTheme.typography.labelSmall.copy(color = TextTertiary, fontSize = 11.sp)
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { selectedTeamId?.let { tid -> onApply(t.id, tid); showApplyDialog = false } },
                        enabled = selectedTeamId != null,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, disabledContainerColor = GoldPrimary.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.EmojiEvents, null, tint = DarkNavy, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.tournament_apply), color = DarkNavy, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showApplyDialog = false }, modifier = Modifier.fillMaxWidth()) {
                        Text("Cancel", color = TextSecondary)
                    }
                }
            )
        }

        // Error
        error?.let {
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                containerColor = ErrorRed,
                action = { TextButton(onClick = onDismissError) { Text("OK", color = White) } }
            ) { Text(it, color = White) }
        }

        // Cancel Tournament Dialog
        if (showCancelDialog) {
            var cancelReason by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { showCancelDialog = false },
                title = { Text("Cancel Tournament", color = White) },
                text = {
                    Column {
                        Text("Are you sure you want to cancel this tournament? This cannot be undone.", style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary))
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = cancelReason,
                            onValueChange = { cancelReason = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Reason (optional)", color = TextTertiary) },
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ErrorRed,
                                unfocusedBorderColor = Separator,
                                focusedContainerColor = SurfaceElevated,
                                unfocusedContainerColor = SurfaceElevated,
                                cursorColor = ErrorRed,
                                focusedTextColor = White,
                                unfocusedTextColor = White
                            )
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        onCancelTournament(t.id, cancelReason.ifBlank { null })
                        showCancelDialog = false
                    }) { Text("Cancel Tournament", color = ErrorRed) }
                },
                dismissButton = {
                    TextButton(onClick = { showCancelDialog = false }) { Text("Go Back", color = TextSecondary) }
                },
                containerColor = DarkNavy
            )
        }

        // Submit Match Result Dialog
        if (showResultDialog && selectedMatchId != null) {
            val match = matches.find { it.id == selectedMatchId }
            if (match != null) {
                var selectedWinner by remember { mutableStateOf<String?>(null) }
                AlertDialog(
                    onDismissRequest = { showResultDialog = false; selectedMatchId = null },
                    title = { Text("Submit Result", color = White) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("R${match.roundNumber} M${match.matchNumber}", style = MaterialTheme.typography.labelSmall.copy(color = TextTertiary))
                            // Team A
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (selectedWinner == match.teamAId) GoldPrimary.copy(alpha = 0.2f) else SurfaceElevated)
                                    .clickable { selectedWinner = match.teamAId }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = selectedWinner == match.teamAId, onClick = { selectedWinner = match.teamAId }, colors = RadioButtonDefaults.colors(selectedColor = GoldPrimary))
                                Text(match.teamAName, style = MaterialTheme.typography.bodyMedium.copy(color = White))
                            }
                            // Team B
                            if (match.teamBId != null) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (selectedWinner == match.teamBId) GoldPrimary.copy(alpha = 0.2f) else SurfaceElevated)
                                        .clickable { selectedWinner = match.teamBId }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(selected = selectedWinner == match.teamBId, onClick = { selectedWinner = match.teamBId }, colors = RadioButtonDefaults.colors(selectedColor = GoldPrimary))
                                    Text(match.teamBName ?: "Unknown", style = MaterialTheme.typography.bodyMedium.copy(color = White))
                                }
                            }
                            // Draw option (BO2 only)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (selectedWinner == "draw") PurplePrimary.copy(alpha = 0.2f) else SurfaceElevated)
                                    .clickable { selectedWinner = "draw" }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = selectedWinner == "draw", onClick = { selectedWinner = "draw" }, colors = RadioButtonDefaults.colors(selectedColor = PurplePrimary))
                                Text("Draw", style = MaterialTheme.typography.bodyMedium.copy(color = White))
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                if (selectedWinner == "draw") {
                                    onSubmitMatchResult(match.id, null, true)
                                } else {
                                    onSubmitMatchResult(match.id, selectedWinner, false)
                                }
                                showResultDialog = false
                                selectedMatchId = null
                            },
                            enabled = selectedWinner != null
                        ) { Text("Submit", color = GoldPrimary) }
                    },
                    dismissButton = {
                        TextButton(onClick = { showResultDialog = false; selectedMatchId = null }) { Text("Cancel", color = TextSecondary) }
                    },
                    containerColor = DarkNavy
                )
            }
        }

        // Disqualify Team Dialog
        if (showDqDialog) {
            var dqReason by remember { mutableStateOf("") }
            val activeTeams = teams.filter { !it.isDisqualified }
            AlertDialog(
                onDismissRequest = { showDqDialog = false; selectedDqTeamId = null },
                title = { Text("Disqualify Team", color = White) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Select a team to disqualify:", style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary))
                        activeTeams.forEach { team ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (selectedDqTeamId == team.teamId) WarningOrange.copy(alpha = 0.2f) else SurfaceElevated)
                                    .clickable { selectedDqTeamId = team.teamId }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = selectedDqTeamId == team.teamId, onClick = { selectedDqTeamId = team.teamId }, colors = RadioButtonDefaults.colors(selectedColor = WarningOrange))
                                Text(team.teamName, style = MaterialTheme.typography.bodyMedium.copy(color = White))
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = dqReason,
                            onValueChange = { dqReason = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Reason (required)", color = TextTertiary) },
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = WarningOrange,
                                unfocusedBorderColor = Separator,
                                focusedContainerColor = SurfaceElevated,
                                unfocusedContainerColor = SurfaceElevated,
                                cursorColor = WarningOrange,
                                focusedTextColor = White,
                                unfocusedTextColor = White
                            )
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            selectedDqTeamId?.let { tid ->
                                onDisqualifyTeam(t.id, tid, dqReason.ifBlank { "Disqualified by host" })
                            }
                            showDqDialog = false
                            selectedDqTeamId = null
                        },
                        enabled = selectedDqTeamId != null && dqReason.isNotBlank()
                    ) { Text("Disqualify", color = WarningOrange) }
                },
                dismissButton = {
                    TextButton(onClick = { showDqDialog = false; selectedDqTeamId = null }) { Text("Cancel", color = TextSecondary) }
                },
                containerColor = DarkNavy
            )
        }

        // ── Resolve Dispute Dialog ──
        if (showDisputeDialog && disputeMatchId != null) {
            val dm = disputeMatch
            AlertDialog(
                onDismissRequest = { showDisputeDialog = false },
                containerColor = DarkNavy,
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Gavel, null, tint = WarningOrange, modifier = Modifier.size(18.dp))
                        Text("Resolve Dispute", color = White)
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (dm != null) {
                            Text(
                                "${dm.teamAName}  vs  ${dm.teamBName ?: "BYE"}",
                                style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary, fontWeight = FontWeight.Bold)
                            )
                        }
                        // Draw option
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Checkbox(
                                checked = disputeIsDraw,
                                onCheckedChange = { disputeIsDraw = it; if (it) disputeWinnerTeamId = null },
                                colors = CheckboxDefaults.colors(checkedColor = GoldPrimary)
                            )
                            Text("Declare draw", color = if (disputeIsDraw) GoldPrimary else TextSecondary)
                        }
                        // Winner selector (hidden when draw)
                        if (!disputeIsDraw && dm != null) {
                            Text("Select winner:", style = MaterialTheme.typography.labelMedium.copy(color = TextTertiary))
                            listOfNotNull(
                                dm.teamAId to dm.teamAName,
                                dm.teamBId?.let { it to (dm.teamBName ?: "Team B") }
                            ).forEach { (id, name) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { disputeWinnerTeamId = id }
                                        .background(
                                            if (disputeWinnerTeamId == id) GoldPrimary.copy(alpha = 0.12f) else Color.Transparent,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    RadioButton(
                                        selected = disputeWinnerTeamId == id,
                                        onClick = { disputeWinnerTeamId = id },
                                        colors = RadioButtonDefaults.colors(selectedColor = GoldPrimary)
                                    )
                                    Text(name, color = if (disputeWinnerTeamId == id) GoldPrimary else White)
                                }
                            }
                        }
                        // Resolution notes
                        OutlinedTextField(
                            value = disputeResolution,
                            onValueChange = { disputeResolution = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Resolution notes (required)", color = TextTertiary, fontSize = 12.sp) },
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = WarningOrange,
                                unfocusedBorderColor = Separator,
                                focusedContainerColor = SurfaceElevated,
                                unfocusedContainerColor = SurfaceElevated,
                                focusedTextColor = White,
                                unfocusedTextColor = White
                            )
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onResolveDispute(disputeMatchId!!, if (disputeIsDraw) null else disputeWinnerTeamId, disputeIsDraw, disputeResolution)
                            showDisputeDialog = false
                        },
                        enabled = disputeResolution.isNotBlank() && (disputeIsDraw || disputeWinnerTeamId != null)
                    ) { Text("Resolve", color = WarningOrange, fontWeight = FontWeight.Bold) }
                },
                dismissButton = {
                    TextButton(onClick = { showDisputeDialog = false }) { Text("Cancel", color = TextSecondary) }
                }
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(20.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Bold,
                color = White,
                fontSize = 16.sp
            )
        )
    }
}

@Composable
private fun InfoBlock(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = White,
                fontSize = 16.sp
            )
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                color = TextTertiary,
                fontSize = 10.sp
            )
        )
    }
}

@Composable
private fun RequirementRow(requirement: TournamentRequirement) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = SurfaceElevated
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = when (requirement.type) {
                    RequirementType.TELEGRAM_SUBSCRIBE -> Icons.Default.Chat
                    RequirementType.YOUTUBE_SUBSCRIBE -> Icons.Default.PlayCircle
                    else -> Icons.Default.Link
                },
                contentDescription = null,
                tint = GoldPrimary,
                modifier = Modifier.size(20.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = requirement.label,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = White,
                        fontWeight = FontWeight.Medium
                    )
                )
                requirement.url?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall.copy(color = BluePrimary, fontSize = 10.sp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// ENHANCED STANDINGS TABLE
// ═══════════════════════════════════════════════════════════════

@Composable
private fun StandingsTable(teams: List<TournamentTeam>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceElevated)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("#", modifier = Modifier.width(36.dp), style = MaterialTheme.typography.labelSmall.copy(color = TextTertiary, fontWeight = FontWeight.Bold))
                Text("TEAM", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall.copy(color = TextTertiary, fontWeight = FontWeight.Bold))
                Text("W", modifier = Modifier.width(28.dp), style = MaterialTheme.typography.labelSmall.copy(color = SuccessGreen, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center))
                Text("L", modifier = Modifier.width(28.dp), style = MaterialTheme.typography.labelSmall.copy(color = ErrorRed, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center))
                Text("PTS", modifier = Modifier.width(36.dp), style = MaterialTheme.typography.labelSmall.copy(color = GoldPrimary, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center))
                Text("BH", modifier = Modifier.width(36.dp), style = MaterialTheme.typography.labelSmall.copy(color = PurplePrimary, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center))
            }
            HorizontalDivider(thickness = 0.5.dp, color = Separator)

            teams.forEachIndexed { index, team ->
                val rank = index + 1
                val rankColor = when (rank) {
                    1 -> Color(0xFFFFD700)   // gold
                    2 -> Color(0xFFC0C0C0)   // silver
                    3 -> Color(0xFFCD7F32)   // bronze
                    else -> TextTertiary
                }
                val bgColor = when {
                    team.isDisqualified -> ErrorRed.copy(alpha = 0.04f)
                    rank == 1 -> GoldPrimary.copy(alpha = 0.04f)
                    rank <= 3 -> White.copy(alpha = 0.02f)
                    else -> Color.Transparent
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(bgColor)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Rank / medal
                    Box(modifier = Modifier.width(36.dp)) {
                        if (rank <= 3 && !team.isDisqualified) {
                            Text(
                                text = when (rank) { 1 -> "🥇"; 2 -> "🥈"; else -> "🥉" },
                                fontSize = 18.sp
                            )
                        } else {
                            Text(
                                text = "$rank",
                                style = MaterialTheme.typography.labelMedium.copy(color = TextTertiary, fontWeight = FontWeight.Bold)
                            )
                        }
                    }

                    // Team name + check-in status
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = team.teamName,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = if (team.isDisqualified) ErrorRed.copy(alpha = 0.7f) else if (rank <= 3) rankColor else White,
                                    fontWeight = if (rank <= 3) FontWeight.Bold else FontWeight.SemiBold
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (team.checkedIn) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(SuccessGreen.copy(alpha = 0.15f))
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text("✓ IN", style = MaterialTheme.typography.labelSmall.copy(color = SuccessGreen, fontSize = 8.sp, fontWeight = FontWeight.Bold))
                                }
                            }
                            if (team.isDisqualified) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(ErrorRed.copy(alpha = 0.15f))
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text("DQ", style = MaterialTheme.typography.labelSmall.copy(color = ErrorRed, fontSize = 8.sp, fontWeight = FontWeight.Bold))
                                }
                            }
                        }
                    }

                    Text(
                        text = "${team.swissWins}",
                        modifier = Modifier.width(28.dp),
                        style = MaterialTheme.typography.bodyMedium.copy(color = SuccessGreen, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    )
                    Text(
                        text = "${team.swissLosses}",
                        modifier = Modifier.width(28.dp),
                        style = MaterialTheme.typography.bodyMedium.copy(color = ErrorRed.copy(alpha = 0.8f), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    )
                    Text(
                        text = "${team.swissPoints}",
                        modifier = Modifier.width(36.dp),
                        style = MaterialTheme.typography.bodyMedium.copy(color = GoldPrimary, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
                    )
                    Text(
                        text = String.format("%.1f", team.buchholzScore),
                        modifier = Modifier.width(36.dp),
                        style = MaterialTheme.typography.bodySmall.copy(color = PurplePrimary, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
                    )
                }
                if (index < teams.size - 1) {
                    HorizontalDivider(thickness = 0.5.dp, color = Separator.copy(alpha = 0.5f), modifier = Modifier.padding(horizontal = 16.dp))
                }
            }
        }
    }
}

@Composable
private fun StatLabel(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Bold,
                color = color,
                fontSize = 14.sp
            )
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                color = TextTertiary,
                fontSize = 8.sp
            )
        )
    }
}

// ═══════════════════════════════════════════════════════════════
// ROUND-FILTERED MATCHES + VS CARDS
// ═══════════════════════════════════════════════════════════════

@Composable
private fun RoundFilteredMatches(
    matches: List<TournamentSwissMatch>,
    isHost: Boolean,
    myTeamIds: List<String> = emptyList(),
    bestOf: Int = 1,
    onNavigateToChat: (String) -> Unit,
    onSubmitResult: (String) -> Unit,
    onViewRoomSecret: (String) -> Unit,
    onResolveDispute: (TournamentSwissMatch) -> Unit = {}
) {
    val rounds = matches.map { it.roundNumber }.distinct().sorted()
    var selectedRound by remember { mutableStateOf(rounds.firstOrNull() ?: 1) }
    val filteredMatches = matches.filter { it.roundNumber == selectedRound }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Round tabs
        if (rounds.size > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rounds.forEach { round ->
                    val isActive = selectedRound == round
                    val roundHasMyMatch = matches.any { it.roundNumber == round && it.isMyMatch }
                    Surface(
                        modifier = Modifier.clickable { selectedRound = round },
                        shape = RoundedCornerShape(20.dp),
                        color = if (isActive) GoldPrimary.copy(alpha = 0.2f) else SurfaceElevated,
                        border = if (isActive) androidx.compose.foundation.BorderStroke(1.dp, GoldPrimary) else null
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Round $round",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = if (isActive) GoldPrimary else TextSecondary,
                                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium
                                )
                            )
                            if (roundHasMyMatch) {
                                Box(
                                    modifier = Modifier.size(6.dp).background(GoldPrimary, CircleShape)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Match cards for selected round
        filteredMatches.forEach { match ->
            val myTeamId = when {
                match.teamAId in myTeamIds -> match.teamAId
                match.teamBId != null && match.teamBId in myTeamIds -> match.teamBId
                else -> null
            }
            MatchVsCard(
                match = match,
                isHost = isHost,
                myTeamId = myTeamId,
                bestOf = bestOf,
                onNavigateToChat = onNavigateToChat,
                onSubmitResult = onSubmitResult,
                onViewRoomSecret = onViewRoomSecret,
                onResolveDispute = onResolveDispute
            )
        }
    }
}

@Composable
private fun MatchVsCard(
    match: TournamentSwissMatch,
    isHost: Boolean,
    myTeamId: String? = null,
    bestOf: Int = 1,
    onNavigateToChat: (String) -> Unit,
    onSubmitResult: (String) -> Unit,
    onViewRoomSecret: (String) -> Unit,
    onResolveDispute: (TournamentSwissMatch) -> Unit = {}
) {
    val uriHandler = LocalUriHandler.current
    // Per-match roster game-number toggle (only relevant for BO2 and user's own matches)
    var selectedGameNumber by remember(match.id) { mutableIntStateOf(1) }
    var showRosterInfo by remember(match.id) { mutableStateOf(false) }
    val matchStatusColor = when (match.status) {
        MatchStatus.SCHEDULED -> BluePrimary
        MatchStatus.IN_PROGRESS -> ErrorRed
        MatchStatus.COMPLETED -> SuccessGreen
        MatchStatus.DISPUTED -> WarningOrange
        MatchStatus.CANCELLED, MatchStatus.BYE -> TextTertiary
    }
    val isCompleted = match.status == MatchStatus.COMPLETED
    val teamAWon = isCompleted && match.winnerTeamId == match.teamAId
    val teamBWon = isCompleted && match.winnerTeamId == match.teamBId

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (match.isMyMatch) 6.dp else 0.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = if (match.isMyMatch) GoldPrimary else Color.Black,
                spotColor = if (match.isMyMatch) GoldPrimary else Color.Black
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (match.isMyMatch) GoldPrimary.copy(alpha = 0.05f) else SurfaceElevated
        ),
        shape = RoundedCornerShape(16.dp),
        border = if (match.isMyMatch) androidx.compose.foundation.BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.35f)) else null
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Top bar: match info + status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(SurfaceCard)
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text("R${match.roundNumber} · M${match.matchNumber}",
                            style = MaterialTheme.typography.labelSmall.copy(color = TextTertiary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold))
                    }
                    if (match.isMyMatch) {
                        Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(GoldPrimary.copy(alpha = 0.15f)).padding(horizontal = 6.dp, vertical = 3.dp)) {
                            Text("YOUR MATCH", style = MaterialTheme.typography.labelSmall.copy(color = GoldPrimary, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold))
                        }
                    }
                }
                Surface(shape = RoundedCornerShape(12.dp), color = matchStatusColor.copy(alpha = 0.15f)) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (match.status == MatchStatus.IN_PROGRESS) {
                            Box(modifier = Modifier.size(5.dp).background(ErrorRed, CircleShape))
                        }
                        Text(
                            text = match.status.value.replace("_", " ").uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(color = matchStatusColor, fontWeight = FontWeight.Bold, fontSize = 9.sp)
                        )
                    }
                }
            }

            // VS row
            if (match.isBye) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = match.teamAName,
                        style = MaterialTheme.typography.bodyMedium.copy(color = White, fontWeight = FontWeight.SemiBold),
                        modifier = Modifier.weight(1f)
                    )
                    Surface(shape = RoundedCornerShape(8.dp), color = SurfaceCard) {
                        Text(
                            text = stringResource(R.string.tournament_bye),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            style = MaterialTheme.typography.labelSmall.copy(color = TextTertiary, fontWeight = FontWeight.Medium)
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Team A
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (teamAWon) Text("👑", fontSize = 13.sp)
                            Text(
                                text = match.teamAName,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = if (teamAWon) GoldPrimary else if (isCompleted && !match.isDraw) White.copy(alpha = 0.45f) else White,
                                    fontWeight = if (teamAWon) FontWeight.ExtraBold else FontWeight.SemiBold
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (isCompleted && match.gameAScore > 0) {
                            Text(
                                text = "${match.gameAScore} pts",
                                style = MaterialTheme.typography.labelSmall.copy(color = if (teamAWon) GoldPrimary else TextTertiary, fontSize = 10.sp)
                            )
                        }
                    }

                    // VS divider
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        if (isCompleted) {
                            Text(
                                text = if (match.isDraw) "=" else "vs",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    color = if (match.isDraw) PurplePrimary else TextTertiary,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            )
                        } else {
                            Text(
                                text = "VS",
                                style = MaterialTheme.typography.labelSmall.copy(color = TextTertiary, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                            )
                        }
                    }

                    // Team B
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.End
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = match.teamBName ?: "TBD",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = if (teamBWon) GoldPrimary else if (isCompleted && !match.isDraw) White.copy(alpha = 0.45f) else if (match.teamBId == null) TextTertiary else White,
                                    fontWeight = if (teamBWon) FontWeight.ExtraBold else FontWeight.SemiBold
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.End
                            )
                            if (teamBWon) Text("👑", fontSize = 13.sp)
                        }
                        if (isCompleted && match.gameBScore > 0) {
                            Text(
                                text = "${match.gameBScore} pts",
                                style = MaterialTheme.typography.labelSmall.copy(color = if (teamBWon) GoldPrimary else TextTertiary, fontSize = 10.sp),
                                textAlign = TextAlign.End
                            )
                        }
                    }
                }
            }

            // Scheduled time (if set)
            match.scheduledAt?.let { scheduledAt ->
                if (scheduledAt > 0 && !isCompleted) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.Schedule, null, tint = TextTertiary, modifier = Modifier.size(12.dp))
                        Text(
                            text = formatDeadline(scheduledAt),
                            style = MaterialTheme.typography.labelSmall.copy(color = TextTertiary, fontSize = 10.sp)
                        )
                    }
                }
            }

            // Action row
            if (match.conversationId != null || (isHost && match.status in listOf(MatchStatus.SCHEDULED, MatchStatus.IN_PROGRESS))) {
                HorizontalDivider(thickness = 0.5.dp, color = Separator.copy(alpha = 0.5f))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (match.conversationId != null) {
                        OutlinedButton(
                            onClick = { onNavigateToChat(match.conversationId!!) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BluePrimary.copy(alpha = 0.4f)),
                            contentPadding = PaddingValues(vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.ChatBubbleOutline, null, tint = BluePrimary, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Chat", color = BluePrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    if (isHost && match.status in listOf(MatchStatus.SCHEDULED, MatchStatus.IN_PROGRESS)) {
                        OutlinedButton(
                            onClick = { onSubmitResult(match.id) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.4f)),
                            contentPadding = PaddingValues(vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Edit, null, tint = GoldPrimary, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Result", color = GoldPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                        OutlinedButton(
                            onClick = { onViewRoomSecret(match.id) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, PurplePrimary.copy(alpha = 0.4f)),
                            contentPadding = PaddingValues(vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.VideogameAsset, null, tint = PurplePrimary, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Room", color = PurplePrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    // BO2 game toggle — visible to participants in active BO2 matches
                    if (myTeamId != null && bestOf >= 2 &&
                        match.status in listOf(MatchStatus.SCHEDULED, MatchStatus.IN_PROGRESS)
                    ) {
                        // Game 1 / Game 2 selector chip
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(1, 2).forEach { g ->
                                FilterChip(
                                    selected = selectedGameNumber == g,
                                    onClick  = { selectedGameNumber = g },
                                    label    = { Text("Game $g", fontSize = 11.sp) },
                                    modifier = Modifier.height(30.dp),
                                    colors   = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = GoldPrimary.copy(alpha = 0.2f),
                                        selectedLabelColor = GoldPrimary,
                                        containerColor = SurfaceElevated,
                                        labelColor = LightGray
                                    )
                                )
                            }
                            // Label showing which game lineup is active
                            Text(
                                "lineup",
                                modifier = Modifier.align(Alignment.CenterVertically),
                                style = MaterialTheme.typography.labelSmall.copy(color = TextTertiary)
                            )
                        }
                    }
                    // Watch Live button — visible to everyone when stream URL is set
                    if (!match.liveStreamUrl.isNullOrBlank()) {
                        OutlinedButton(
                            onClick = {
                                try { uriHandler.openUri(match.liveStreamUrl) } catch (_: Exception) {}
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, ErrorRed.copy(alpha = 0.6f)),
                            contentPadding = PaddingValues(vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Videocam, null, tint = ErrorRed, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Watch Live", color = ErrorRed, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    if (isHost && match.status == MatchStatus.DISPUTED) {
                        OutlinedButton(
                            onClick = { onResolveDispute(match) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, WarningOrange.copy(alpha = 0.6f)),
                            contentPadding = PaddingValues(vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Gavel, null, tint = WarningOrange, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Resolve Dispute", color = WarningOrange, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

private fun formatDeadline(timestamp: Long): String {
    if (timestamp <= 0) return "TBD"
    val remaining = timestamp - System.currentTimeMillis()
    if (remaining <= 0) return "Closed"
    val hours = remaining / 3600000
    val minutes = (remaining % 3600000) / 60000
    return when {
        hours > 24 -> "${hours / 24}d ${hours % 24}h"
        hours > 0 -> "${hours}h ${minutes}m"
        else -> "${minutes}m"
    }
}

@Composable
private fun HostActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = color.copy(alpha = 0.15f),
            contentColor = color,
            disabledContainerColor = color.copy(alpha = 0.05f),
            disabledContentColor = color.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Icon(icon, null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(label, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
    }
}

@Composable
private fun ApplicationReviewRow(
    app: TournamentApplication,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = SurfaceElevated
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.teamName,
                    style = MaterialTheme.typography.bodyMedium.copy(color = White, fontWeight = FontWeight.SemiBold)
                )
                Text(
                    text = stringResource(R.string.tournament_attempt, app.attemptNumber),
                    style = MaterialTheme.typography.labelSmall.copy(color = TextTertiary, fontSize = 10.sp)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                IconButton(
                    onClick = onApprove,
                    modifier = Modifier.size(32.dp).background(SuccessGreen.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                ) {
                    Icon(Icons.Default.Check, "Approve", tint = SuccessGreen, modifier = Modifier.size(16.dp))
                }
                IconButton(
                    onClick = onReject,
                    modifier = Modifier.size(32.dp).background(ErrorRed.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                ) {
                    Icon(Icons.Default.Close, "Reject", tint = ErrorRed, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// TOURNAMENT HERO CARD (with live countdown + team fill bar)
// ═══════════════════════════════════════════════════════════════

@Composable
private fun TournamentHeroCard(tournament: Tournament, statusColor: Color) {
    // Real-time countdown ticker
    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(tournament.registrationDeadline, tournament.checkInDeadline, tournament.status) {
        if (tournament.isOpen || tournament.isCheckIn) {
            while (true) {
                kotlinx.coroutines.delay(1000)
                currentTime = System.currentTimeMillis()
            }
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "live_pulse")
    val livePulse by infiniteTransition.animateFloat(
        initialValue = 0.5f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse),
        label = "pulse"
    )

    val deadlineMs = when {
        tournament.isOpen -> tournament.registrationDeadline
        tournament.isCheckIn -> tournament.checkInDeadline
        else -> 0L
    }
    val remaining = if (deadlineMs > 0) maxOf(0L, deadlineMs - currentTime) else 0L
    val isUrgent = remaining in 1..3_600_000 // < 1 hour

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = statusColor.copy(alpha = 0.2f),
                spotColor = statusColor.copy(alpha = 0.15f)
            ),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = if (tournament.isLive || tournament.isOpen) 1.dp else 0.5.dp,
            color = if (tournament.isLive) ErrorRed.copy(alpha = livePulse * 0.6f)
                    else if (tournament.isOpen) SuccessGreen.copy(alpha = livePulse * 0.5f)
                    else Separator
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Status accent bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(
                        brush = Brush.horizontalGradient(listOf(statusColor, statusColor.copy(alpha = 0.2f))),
                        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                    )
            )

            Column(modifier = Modifier.padding(20.dp)) {
                // Status + Live badge row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(shape = RoundedCornerShape(20.dp), color = statusColor.copy(alpha = 0.15f)) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (tournament.isLive) {
                                Box(modifier = Modifier.size(6.dp).background(ErrorRed.copy(alpha = livePulse), CircleShape))
                            }
                            Text(
                                text = tournament.status.value.replace("_", " ").uppercase(),
                                style = MaterialTheme.typography.labelSmall.copy(color = statusColor, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
                            )
                        }
                    }
                    // Prize
                    Surface(shape = RoundedCornerShape(20.dp), color = GoldPrimary.copy(alpha = 0.1f)) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.EmojiEvents, null, tint = GoldPrimary, modifier = Modifier.size(14.dp))
                            Text(
                                text = tournament.prizeDisplay.replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.labelSmall.copy(color = GoldPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp),
                                maxLines = 1, overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Title + host
                Text(
                    text = tournament.title,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold, color = White, letterSpacing = (-0.5).sp)
                )
                Text(
                    text = stringResource(R.string.tournament_hosted_by, tournament.hostUsername),
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                )

                // Description
                if (tournament.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = tournament.description,
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, lineHeight = 18.sp),
                        maxLines = 3, overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Info grid — 4 equal blocks
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    InfoBlock(label = "MAX TEAMS", value = "${tournament.maxTeams}")
                    InfoBlock(label = "FORMAT", value = "BO${tournament.bestOf}")
                    InfoBlock(label = "REGION", value = tournament.region)
                    InfoBlock(label = "SKILL", value = tournament.skillLevel.take(6))
                }

                if (tournament.swissRounds != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        InfoBlock(label = "ROUNDS", value = "${tournament.swissRounds}")
                        InfoBlock(label = "MIN SIZE", value = "${tournament.minTeamSize}v${tournament.minTeamSize}")
                        InfoBlock(label = "ROUND", value = if (tournament.currentRound > 0) "#${tournament.currentRound}" else "—")
                        if (tournament.isLiveStreamEnabled) {
                            InfoBlock(label = "STREAM", value = "🔴 LIVE")
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }

                // ── Team Slots Fill Bar ──
                Spacer(modifier = Modifier.height(16.dp))
                val fillFraction = if (tournament.maxTeams > 0) (tournament.teamCount.toFloat() / tournament.maxTeams).coerceIn(0f, 1f) else 0f
                val isFull = tournament.teamCount >= tournament.maxTeams && tournament.maxTeams > 0
                val barColor = when {
                    isFull -> ErrorRed
                    fillFraction > 0.75f -> WarningOrange
                    else -> SuccessGreen
                }
                val animatedFill by animateFloatAsState(fillFraction, tween(900, easing = FastOutSlowInEasing), label = "hero_fill")
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Groups, null, tint = barColor, modifier = Modifier.size(14.dp))
                            Text(
                                text = "${tournament.teamCount} / ${tournament.maxTeams} teams registered",
                                style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontWeight = FontWeight.Medium)
                            )
                        }
                        if (isFull) {
                            Surface(shape = RoundedCornerShape(20.dp), color = ErrorRed.copy(alpha = 0.15f), border = androidx.compose.foundation.BorderStroke(1.dp, ErrorRed.copy(alpha = 0.5f))) {
                                Text("FULL", modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp), style = MaterialTheme.typography.labelSmall.copy(color = ErrorRed, fontWeight = FontWeight.ExtraBold, fontSize = 9.sp))
                            }
                        } else {
                            Text("${(fillFraction * 100).toInt()}%", style = MaterialTheme.typography.labelSmall.copy(color = barColor, fontWeight = FontWeight.Bold, fontSize = 11.sp))
                        }
                    }
                    Box(
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(Separator)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(animatedFill)
                                .fillMaxHeight()
                                .background(Brush.horizontalGradient(listOf(barColor.copy(alpha = 0.8f), barColor)), RoundedCornerShape(3.dp))
                        )
                    }
                }

                // ── Countdown timer ──
                if (remaining > 0) {
                    Spacer(modifier = Modifier.height(12.dp))
                    val countdownLabel = when {
                        tournament.isOpen -> "Registration closes in"
                        tournament.isCheckIn -> "Check-in closes in"
                        else -> "Starts in"
                    }
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = if (isUrgent) ErrorRed.copy(alpha = 0.08f) else WarningOrange.copy(alpha = 0.07f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isUrgent) ErrorRed.copy(alpha = 0.4f) else WarningOrange.copy(alpha = 0.3f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = null,
                                tint = if (isUrgent) ErrorRed else WarningOrange,
                                modifier = Modifier.size(16.dp)
                            )
                            Column {
                                Text(countdownLabel, style = MaterialTheme.typography.labelSmall.copy(color = TextTertiary, fontSize = 10.sp))
                                Text(
                                    text = formatCountdown(remaining),
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        color = if (isUrgent) ErrorRed else WarningOrange,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 16.sp,
                                        letterSpacing = 1.sp
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatCountdown(ms: Long): String {
    if (ms <= 0) return "00:00:00"
    val totalSeconds = ms / 1000
    val days = totalSeconds / 86400
    val hours = (totalSeconds % 86400) / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (days > 0) "${days}d ${hours.toString().padStart(2,'0')}:${minutes.toString().padStart(2,'0')}:${seconds.toString().padStart(2,'0')}"
    else "${hours.toString().padStart(2,'0')}:${minutes.toString().padStart(2,'0')}:${seconds.toString().padStart(2,'0')}"
}

// ═══════════════════════════════════════════════════════════════
// WINNER PODIUM
// ═══════════════════════════════════════════════════════════════

@Composable
private fun TournamentPodium(teams: List<TournamentTeam>) {
    val top3 = teams.filter { !it.isDisqualified }.take(3)
    if (top3.isEmpty()) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(GoldPrimary.copy(alpha = 0.06f), Color.Transparent)))
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.EmojiEvents, null, tint = GoldPrimary, modifier = Modifier.size(20.dp))
                Text(
                    text = "Tournament Champions",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold, color = GoldPrimary, letterSpacing = 0.5.sp)
                )
                Icon(Icons.Default.EmojiEvents, null, tint = GoldPrimary, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(20.dp))

            // Podium layout: 2nd | 1st | 3rd
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                // 2nd place
                if (top3.size >= 2) {
                    PodiumSlot(team = top3[1], rank = 2, podiumHeight = 70.dp, modifier = Modifier.weight(1f))
                } else { Spacer(modifier = Modifier.weight(1f)) }

                // 1st place (tallest)
                PodiumSlot(team = top3[0], rank = 1, podiumHeight = 100.dp, modifier = Modifier.weight(1f))

                // 3rd place
                if (top3.size >= 3) {
                    PodiumSlot(team = top3[2], rank = 3, podiumHeight = 50.dp, modifier = Modifier.weight(1f))
                } else { Spacer(modifier = Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun PodiumSlot(
    team: TournamentTeam,
    rank: Int,
    podiumHeight: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
    val podiumColor = when (rank) {
        1 -> Color(0xFFFFD700)
        2 -> Color(0xFFC0C0C0)
        else -> Color(0xFFCD7F32)
    }
    val medal = when (rank) { 1 -> "🥇"; 2 -> "🥈"; else -> "🥉" }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {
        Text(medal, fontSize = if (rank == 1) 32.sp else 24.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = team.teamName,
            style = MaterialTheme.typography.labelSmall.copy(
                color = podiumColor,
                fontWeight = FontWeight.ExtraBold,
                fontSize = if (rank == 1) 13.sp else 11.sp,
                textAlign = TextAlign.Center
            ),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
        Text(
            text = "${team.swissPoints} pts",
            style = MaterialTheme.typography.labelSmall.copy(color = TextTertiary, fontSize = 10.sp)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(podiumHeight)
                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                .background(
                    brush = Brush.verticalGradient(
                        listOf(podiumColor.copy(alpha = 0.4f), podiumColor.copy(alpha = 0.15f))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "#$rank",
                style = MaterialTheme.typography.titleLarge.copy(
                    color = podiumColor,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = if (rank == 1) 28.sp else 20.sp
                )
            )
        }
    }
}



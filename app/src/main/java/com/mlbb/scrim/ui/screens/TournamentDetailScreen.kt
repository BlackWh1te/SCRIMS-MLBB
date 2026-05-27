package com.mlbb.scrim.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    onLoadRoomSecret: (String) -> Unit = {}
) {
    val t = tournament ?: return

    var showApplyDialog by remember { mutableStateOf(false) }
    var selectedTeamId by remember { mutableStateOf<String?>(null) }
    var showCancelDialog by remember { mutableStateOf(false) }
    var showResultDialog by remember { mutableStateOf(false) }
    var selectedMatchId by remember { mutableStateOf<String?>(null) }
    var showDqDialog by remember { mutableStateOf(false) }
    var selectedDqTeamId by remember { mutableStateOf<String?>(null) }

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
                    // ── Tournament Hero Card ──
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                            shape = RoundedCornerShape(20.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (t.isLive) 1.dp else 0.5.dp,
                                color = if (t.isLive) ErrorRed.copy(alpha = 0.4f) else Separator
                            )
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                // Status + Prize row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(20.dp),
                                        color = statusColor.copy(alpha = 0.15f)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            if (t.isLive) {
                                                Box(modifier = Modifier.size(6.dp).background(ErrorRed, CircleShape))
                                            }
                                            Text(
                                                text = t.status.value.replace("_", " ").uppercase(),
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = statusColor,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp
                                                )
                                            )
                                        }
                                    }
                                    Text(
                                        text = t.prizeDisplay.uppercase(),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = GoldPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    text = t.title,
                                    style = MaterialTheme.typography.headlineSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = White
                                    )
                                )

                                Text(
                                    text = stringResource(R.string.tournament_hosted_by, t.hostUsername),
                                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                                )

                                if (t.description.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = t.description,
                                        style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary),
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Info grid
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    InfoBlock(label = stringResource(R.string.tournament_max_teams), value = "${t.maxTeams}")
                                    InfoBlock(label = stringResource(R.string.tournament_best_of), value = "BO${t.bestOf}")
                                    InfoBlock(label = stringResource(R.string.tournament_region), value = t.region)
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    InfoBlock(label = stringResource(R.string.tournament_skill_level), value = t.skillLevel)
                                    InfoBlock(label = stringResource(R.string.tournament_min_team_size), value = "${t.minTeamSize}")
                                    if (t.swissRounds != null) {
                                        InfoBlock(label = "Rounds", value = "${t.swissRounds}")
                                    }
                                }

                                // Registration deadline
                                if (t.isOpen && t.registrationDeadline > 0) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Schedule,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = WarningOrange
                                        )
                                        Text(
                                            text = stringResource(R.string.tournament_registration_closes, formatDeadline(t.registrationDeadline)),
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = WarningOrange,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        )
                                    }
                                }
                            }
                        }
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

                    // ── Standings ──
                    if (teams.isNotEmpty()) {
                        item {
                            SectionHeader(title = stringResource(R.string.tournament_standings), icon = Icons.Default.Leaderboard)
                        }
                        itemsIndexed(teams) { index, team ->
                            TeamStandingRow(team = team, rank = index + 1)
                        }
                    }

                    // ── Matches ──
                    if (matches.isNotEmpty()) {
                        item {
                            SectionHeader(title = stringResource(R.string.tournament_matches), icon = Icons.Default.SportsEsports)
                        }
                        items(matches) { match ->
                            MatchRow(
                                match = match,
                                onNavigateToChat = onNavigateToChat,
                                isHost = isHost,
                                onSubmitResult = { mid ->
                                    selectedMatchId = mid
                                    showResultDialog = true
                                },
                                onViewRoomSecret = { mid -> onLoadRoomSecret(mid) }
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

        // Apply dialog
        if (showApplyDialog) {
            AlertDialog(
                onDismissRequest = { showApplyDialog = false },
                title = { Text(stringResource(R.string.tournament_apply_confirm)) },
                text = {
                    Column {
                        Text(
                            text = stringResource(R.string.tournament_apply_confirm_msg),
                            style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.tournament_select_team),
                            style = MaterialTheme.typography.labelMedium.copy(color = LightGray)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        myTeams.forEach { team ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (selectedTeamId == team.id) GoldPrimary.copy(alpha = 0.2f) else SurfaceElevated
                                    )
                                    .clickable { selectedTeamId = team.id }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedTeamId == team.id,
                                    onClick = { selectedTeamId = team.id },
                                    colors = RadioButtonDefaults.colors(selectedColor = GoldPrimary)
                                )
                                Text(
                                    text = team.name,
                                    style = MaterialTheme.typography.bodyMedium.copy(color = White)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${team.currentPlayerCount}/${team.maxPlayers}",
                                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            selectedTeamId?.let { tid ->
                                onApply(t.id, tid)
                                showApplyDialog = false
                            }
                        },
                        enabled = selectedTeamId != null
                    ) {
                        Text(stringResource(R.string.tournament_apply), color = GoldPrimary)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showApplyDialog = false }) {
                        Text("Cancel", color = TextSecondary)
                    }
                },
                containerColor = DarkNavy
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

@Composable
private fun TeamStandingRow(team: TournamentTeam, rank: Int) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = SurfaceElevated
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rank
            Text(
                text = "#$rank",
                style = MaterialTheme.typography.labelMedium.copy(
                    color = when (rank) {
                        1 -> GoldPrimary
                        2 -> LightGray
                        3 -> Color(0xFFCD7F32) // bronze
                        else -> TextTertiary
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                ),
                modifier = Modifier.width(32.dp)
            )

            // Team name
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = team.teamName,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = if (team.isDisqualified) ErrorRed else White,
                        fontWeight = FontWeight.SemiBold
                    )
                )
                if (team.isDisqualified) {
                    Text(
                        text = "DQ",
                        style = MaterialTheme.typography.labelSmall.copy(color = ErrorRed, fontSize = 9.sp)
                    )
                }
            }

            // Stats
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatLabel(stringResource(R.string.tournament_wins), "${team.swissWins}", SuccessGreen)
                StatLabel(stringResource(R.string.tournament_losses), "${team.swissLosses}", ErrorRed)
                StatLabel(stringResource(R.string.tournament_points), "${team.swissPoints}", GoldPrimary)
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

@Composable
private fun MatchRow(
    match: TournamentSwissMatch,
    onNavigateToChat: (String) -> Unit,
    isHost: Boolean = false,
    onSubmitResult: (String) -> Unit = {},
    onViewRoomSecret: (String) -> Unit = {}
) {
    val matchStatusColor = when (match.status) {
        MatchStatus.SCHEDULED -> BluePrimary
        MatchStatus.IN_PROGRESS -> ErrorRed
        MatchStatus.COMPLETED -> SuccessGreen
        MatchStatus.DISPUTED -> WarningOrange
        MatchStatus.CANCELLED -> TextTertiary
        MatchStatus.BYE -> TextTertiary
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = if (match.isMyMatch) GoldPrimary.copy(alpha = 0.05f) else SurfaceElevated,
        border = if (match.isMyMatch) androidx.compose.foundation.BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.3f)) else null
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Round info
            Column(modifier = Modifier.width(48.dp)) {
                Text(
                    text = "R${match.roundNumber}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = TextTertiary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                )
                Text(
                    text = "M${match.matchNumber}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = TextTertiary,
                        fontSize = 9.sp
                    )
                )
            }

            // Teams
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = match.teamAName,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = if (match.winnerTeamId == match.teamAId) GoldPrimary else White,
                        fontWeight = if (match.winnerTeamId == match.teamAId) FontWeight.Bold else FontWeight.Medium
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (match.isBye) stringResource(R.string.tournament_bye) else (match.teamBName ?: "TBD"),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = if (match.winnerTeamId == match.teamBId) GoldPrimary else TextSecondary,
                        fontWeight = if (match.winnerTeamId == match.teamBId) FontWeight.Bold else FontWeight.Medium
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Status
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = matchStatusColor.copy(alpha = 0.15f)
            ) {
                Text(
                    text = match.status.value.replace("_", " ").uppercase(),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = matchStatusColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp
                    )
                )
            }

            // Host: Submit result button (for scheduled/in_progress matches)
            if (isHost && match.status in listOf(MatchStatus.SCHEDULED, MatchStatus.IN_PROGRESS)) {
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(
                    onClick = { onSubmitResult(match.id) },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Default.Edit, "Submit Result", tint = GoldPrimary, modifier = Modifier.size(16.dp))
                }
            }

            // Host: View room secret
            if (isHost && match.status in listOf(MatchStatus.SCHEDULED, MatchStatus.IN_PROGRESS)) {
                IconButton(
                    onClick = { onViewRoomSecret(match.id) },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Default.VideogameAsset, "Room", tint = BluePrimary, modifier = Modifier.size(16.dp))
                }
            }

            // Chat button
            if (match.conversationId != null) {
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(
                    onClick = { onNavigateToChat(match.conversationId!!) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ChatBubbleOutline,
                        contentDescription = "Chat",
                        tint = BluePrimary,
                        modifier = Modifier.size(18.dp)
                    )
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



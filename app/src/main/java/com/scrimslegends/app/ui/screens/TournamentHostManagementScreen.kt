package com.scrimslegends.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scrimslegends.app.data.model.Tournament
import com.scrimslegends.app.data.model.TournamentStatus
import com.scrimslegends.app.ui.theme.*
import com.scrimslegends.app.ui.components.*
import com.scrimslegends.app.R
import androidx.compose.ui.res.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TournamentHostManagementScreen(
    hostedTournaments: List<Tournament>,
    isLoading: Boolean,
    isRefreshing: Boolean,
    error: String? = null,
    onNavigateBack: () -> Unit,
    onNavigateToTournamentDetail: (String) -> Unit,
    onNavigateToEditTournament: (String) -> Unit,
    onCancelTournament: (String, String?) -> Unit,
    onCompleteTournament: (String) -> Unit,
    onRefresh: () -> Unit = {},
    onDismissError: () -> Unit = {}
) {
    var showCancelDialog by remember { mutableStateOf(false) }
    var cancelTournamentId by remember { mutableStateOf<String?>(null) }
    var cancelReason by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = heroGradientBrush())
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = DarkNavy.copy(alpha = 0.5f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(top = 8.dp, bottom = 16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        GlassBackButton(onClick = onNavigateBack)

                        Text(
                            text = stringResource(R.string.host_management_title),
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = White
                            )
                        )

                        Spacer(modifier = Modifier.size(44.dp))
                    }

                    // Stats
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PremiumChip(
                            text = "${hostedTournaments.size} HOSTED",
                            icon = Icons.Default.EmojiEvents,
                            color = GoldPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        PremiumChip(
                            text = "${hostedTournaments.count { it.status == TournamentStatus.REGISTRATION }} OPEN",
                            icon = Icons.Default.CheckCircle,
                            color = SuccessGreen,
                            modifier = Modifier.weight(1f)
                        )
                        PremiumChip(
                            text = "${hostedTournaments.count { it.status == TournamentStatus.IN_PROGRESS }} LIVE",
                            icon = Icons.Default.PlayArrow,
                            color = ErrorRed,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Tournament List
            if (isLoading && hostedTournaments.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = GoldPrimary, modifier = Modifier.size(48.dp))
                }
            } else if (hostedTournaments.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = TextTertiary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.my_tournaments_empty),
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = TextSecondary,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.my_tournaments_empty_desc),
                            style = MaterialTheme.typography.bodyMedium.copy(color = TextTertiary)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    items(hostedTournaments, key = { it.id }) { tournament ->
                        AnimatedEntrance(delayMillis = hostedTournaments.indexOf(tournament) * 50) {
                            HostTournamentCard(
                                tournament = tournament,
                                onViewDetail = { onNavigateToTournamentDetail(tournament.id) },
                                onEdit = { onNavigateToEditTournament(tournament.id) },
                                onCancel = {
                                    cancelTournamentId = tournament.id
                                    showCancelDialog = true
                                },
                                onComplete = { onCompleteTournament(tournament.id) }
                            )
                        }
                    }
                }
            }
        }

        // Cancel dialog
        if (showCancelDialog) {
            AlertDialog(
                onDismissRequest = { showCancelDialog = false },
                title = { Text("Cancel Tournament", color = White) },
                text = {
                    Column {
                        Text(
                            "This will cancel the tournament and notify all participants.",
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = cancelReason,
                            onValueChange = { cancelReason = it },
                            label = { Text("Reason (optional)", color = TextTertiary) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = White,
                                unfocusedTextColor = White,
                                focusedBorderColor = GoldPrimary,
                                unfocusedBorderColor = Separator
                            )
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            cancelTournamentId?.let { onCancelTournament(it, cancelReason.ifBlank { null }) }
                            showCancelDialog = false
                            cancelTournamentId = null
                            cancelReason = ""
                        }
                    ) {
                        Text("Cancel Tournament", color = ErrorRed)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCancelDialog = false }) {
                        Text("Dismiss", color = LightGray)
                    }
                },
                containerColor = SurfaceCard
            )
        }

        // Error snackbar
        error?.let {
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                containerColor = ErrorRed,
                action = {
                    TextButton(onClick = onDismissError) { Text("OK", color = White) }
                }
            ) {
                Text(it, color = White)
            }
        }
    }
}

@Composable
private fun HostTournamentCard(
    tournament: Tournament,
    onViewDetail: () -> Unit,
    onEdit: () -> Unit,
    onCancel: () -> Unit,
    onComplete: () -> Unit
) {
    val statusColor = when (tournament.status) {
        TournamentStatus.REGISTRATION -> SuccessGreen
        TournamentStatus.CHECK_IN -> WarningOrange
        TournamentStatus.IN_PROGRESS -> ErrorRed
        TournamentStatus.COMPLETED -> BluePrimary
        TournamentStatus.CANCELLED -> TextTertiary
        else -> TextTertiary
    }

    val statusLabel = when (tournament.status) {
        TournamentStatus.REGISTRATION -> "REGISTRATION"
        TournamentStatus.CHECK_IN -> "CHECK-IN"
        TournamentStatus.IN_PROGRESS -> "LIVE"
        TournamentStatus.COMPLETED -> "COMPLETED"
        TournamentStatus.CANCELLED -> "CANCELLED"
        else -> tournament.status.value.replace("_", " ").uppercase()
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = if (tournament.isLive) 1.dp else 0.5.dp,
            color = if (tournament.isLive) ErrorRed.copy(alpha = 0.4f) else Separator
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Status + actions row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = statusColor.copy(alpha = 0.15f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (tournament.isLive) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(ErrorRed, androidx.compose.foundation.shape.CircleShape)
                            )
                        }
                        Text(
                            text = statusLabel,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = statusColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        )
                    }
                }

                // Quick actions
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(BluePrimary.copy(alpha = 0.2f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = BluePrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    if (tournament.status == TournamentStatus.IN_PROGRESS) {
                        IconButton(
                            onClick = onComplete,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(SuccessGreen.copy(alpha = 0.2f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Complete",
                                tint = SuccessGreen,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    if (tournament.status != TournamentStatus.CANCELLED && tournament.status != TournamentStatus.COMPLETED) {
                        IconButton(
                            onClick = onCancel,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(ErrorRed.copy(alpha = 0.2f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Cancel,
                                contentDescription = "Cancel",
                                tint = ErrorRed,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Logo + Title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (!tournament.logoUrl.isNullOrBlank()) {
                    coil.compose.AsyncImage(
                        model = tournament.logoUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceElevated),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(GoldPrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.EmojiEvents, null, tint = GoldPrimary, modifier = Modifier.size(18.dp))
                    }
                }
                Text(
                    text = tournament.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = White,
                        fontSize = 18.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Info row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                HostInfoChip(icon = Icons.Default.Groups, label = "${tournament.maxTeams} teams max")
                HostInfoChip(icon = Icons.Default.SportsEsports, label = "BO${tournament.bestOf}")
                HostInfoChip(icon = Icons.Default.Public, label = tournament.region)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // View detail button
            iOSPrimaryButton(
                text = stringResource(R.string.view_detail),
                onClick = onViewDetail,
                backgroundColor = SurfaceElevated,
                contentColor = White
            )
        }
    }
}

@Composable
private fun HostInfoChip(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
        )
    }
}

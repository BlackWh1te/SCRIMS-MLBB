package com.mlbb.scrim.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mlbb.scrim.data.model.MatchResult
import com.mlbb.scrim.data.model.VerificationStatus
import com.mlbb.scrim.ui.theme.*
import com.mlbb.scrim.R
import androidx.compose.ui.res.stringResource
import com.mlbb.scrim.ui.components.AnimatedEntrance
import com.mlbb.scrim.ui.components.GlassBackButton
import com.mlbb.scrim.ui.components.GradientButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportMatchResultScreen(
    matchResult: MatchResult,
    currentUserId: String,
    currentUserName: String,
    currentTeamId: String,
    onNavigateBack: () -> Unit,
    onReportResult: (
        matchResultId: String,
        teamId: String,
        reporterId: String,
        reporterName: String,
        reportedWinnerId: String,
        notes: String?
    ) -> Unit,
    isLoading: Boolean = false,
    reportSuccess: Boolean = false,
    onClearSuccess: () -> Unit = {}
) {
    var selectedWinnerId by remember { mutableStateOf<String?>(null) }
    var notes by remember { mutableStateOf("") }
    var screenshotUploaded by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    LaunchedEffect(reportSuccess) {
        if (reportSuccess) {
            onNavigateBack()
            onClearSuccess()
        }
    }

    val hasAlreadyReported = when (currentTeamId) {
        matchResult.teamAId -> matchResult.teamAReport != null
        matchResult.teamBId -> matchResult.teamBReport != null
        else -> true
    }

    val isConfirmed = matchResult.verificationStatus == VerificationStatus.CONFIRMED
    val isDisputed = matchResult.verificationStatus == VerificationStatus.DISPUTED

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = heroGradientBrush())
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
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
                        text = stringResource(R.string.report_result),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = White
                        )
                    )

                    Spacer(modifier = Modifier.width(44.dp))
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Match Info Card
                AnimatedEntrance(delayMillis = 100) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(
                                elevation = 8.dp,
                                spotColor = Color.Black.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(20.dp)
                            ),
                        colors = CardDefaults.cardColors(containerColor = DarkNavy),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = stringResource(R.string.match_label),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontSize = 14.sp,
                                    color = MidGray
                                )
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TeamBadge(name = matchResult.teamAName, isWinner = matchResult.confirmedWinnerId == matchResult.teamAId)

                                Text(
                                    text = stringResource(R.string.vs_label),
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = GoldPrimary
                                    )
                                )

                                TeamBadge(name = matchResult.teamBName, isWinner = matchResult.confirmedWinnerId == matchResult.teamBId)
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            VerificationStatusBadge(status = matchResult.verificationStatus)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                when {
                    isConfirmed -> {
                        AnimatedEntrance(delayMillis = 200) {
                            ConfirmedResultCard(matchResult = matchResult)
                        }
                    }
                    hasAlreadyReported -> {
                        AnimatedEntrance(delayMillis = 200) {
                            AlreadyReportedCard()
                        }
                    }
                    isDisputed && matchResult.pendingReporterTeamId != currentTeamId -> {
                        AnimatedEntrance(delayMillis = 200) {
                            DisputeCard(matchResult = matchResult)
                        }
                    }
                    else -> {
                        // Report Form
                        AnimatedEntrance(delayMillis = 200) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .shadow(
                                        elevation = 8.dp,
                                        spotColor = Color.Black.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(20.dp)
                                    ),
                                colors = CardDefaults.cardColors(containerColor = DarkNavy),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp)
                                ) {
                                    Text(
                                        text = stringResource(R.string.who_won),
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = White
                                        )
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))

                                    // Winner Selection
                                    WinnerSelectionButton(
                                        teamName = matchResult.teamAName,
                                        teamId = matchResult.teamAId,
                                        isSelected = selectedWinnerId == matchResult.teamAId,
                                        onClick = { selectedWinnerId = matchResult.teamAId }
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    WinnerSelectionButton(
                                        teamName = matchResult.teamBName,
                                        teamId = matchResult.teamBId,
                                        isSelected = selectedWinnerId == matchResult.teamBId,
                                        onClick = { selectedWinnerId = matchResult.teamBId }
                                    )

                                    Spacer(modifier = Modifier.height(20.dp))

                                    // Notes
                                    OutlinedTextField(
                                        value = notes,
                                        onValueChange = { notes = it },
                                        label = { Text("Optional notes") },
                                        placeholder = { Text("e.g., We won 2-0, clean game") },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = GoldPrimary,
                                            unfocusedBorderColor = White.copy(alpha = 0.3f),
                                            focusedLabelColor = GoldPrimary,
                                            unfocusedLabelColor = White.copy(alpha = 0.7f),
                                            cursorColor = GoldPrimary,
                                            focusedTextColor = White,
                                            unfocusedTextColor = White
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        minLines = 3,
                                        maxLines = 5
                                    )

                                    Spacer(modifier = Modifier.height(20.dp))

                                    // Screenshot Upload Section
                                    Text(
                                        text = stringResource(R.string.screenshot_evidence),
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = White
                                        )
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    if (!screenshotUploaded) {
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(120.dp)
                                                .shadow(
                                                    elevation = 2.dp,
                                                    spotColor = Color.Black.copy(alpha = 0.1f),
                                                    shape = RoundedCornerShape(12.dp)
                                                ),
                                            colors = CardDefaults.cardColors(
                                                containerColor = White.copy(alpha = 0.05f)
                                            ),
                                            shape = RoundedCornerShape(12.dp),
                                            onClick = { screenshotUploaded = true }
                                        ) {
                                            Column(
                                                modifier = Modifier.fillMaxSize(),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.CameraAlt,
                                                    contentDescription = "Upload",
                                                    tint = BluePrimary,
                                                    modifier = Modifier.size(32.dp)
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text(
                                                    text = stringResource(R.string.tap_upload_screenshot),
                                                    fontSize = 14.sp,
                                                    color = LightGray
                                                )
                                            }
                                        }
                                    } else {
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(120.dp)
                                                .shadow(
                                                    elevation = 4.dp,
                                                    spotColor = SuccessGreen.copy(alpha = 0.2f),
                                                    shape = RoundedCornerShape(12.dp)
                                                ),
                                            colors = CardDefaults.cardColors(
                                                containerColor = SuccessGreen.copy(alpha = 0.08f)
                                            ),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(16.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(48.dp)
                                                            .background(
                                                                color = SuccessGreen.copy(alpha = 0.15f),
                                                                shape = RoundedCornerShape(10.dp)
                                                            ),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Image,
                                                            contentDescription = null,
                                                            tint = SuccessGreen,
                                                            modifier = Modifier.size(24.dp)
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.width(12.dp))
                                                    Column {
                                                        Text(
                                                            text = stringResource(R.string.screenshot_uploaded),
                                                            fontSize = 15.sp,
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = SuccessGreen
                                                        )
                                                        Text(
                                                            text = stringResource(R.string.match_screenshot_attached),
                                                            fontSize = 13.sp,
                                                            color = LightGray
                                                        )
                                                    }
                                                }
                                                IconButton(
                                                    onClick = { screenshotUploaded = false }
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Close,
                                                        contentDescription = "Remove",
                                                        tint = MidGray,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    AnimatedVisibility(
                                        visible = errorMessage.isNotEmpty(),
                                        enter = fadeIn() + expandVertically(),
                                        exit = fadeOut() + shrinkVertically()
                                    ) {
                                        Text(
                                            text = errorMessage,
                                            color = ErrorRed,
                                            fontSize = 13.sp,
                                            modifier = Modifier.padding(bottom = 12.dp)
                                        )
                                    }

                                    GradientButton(
                                        text = if (isLoading) "Submitting..." else "Submit Report",
                                        onClick = {
                                            when {
                                                selectedWinnerId == null -> {
                                                    errorMessage = "Please select a winner"
                                                }
                                                else -> {
                                                    errorMessage = ""
                                                    selectedWinnerId?.let { winnerId ->
                                                        onReportResult(
                                                            matchResult.id,
                                                            currentTeamId,
                                                            currentUserId,
                                                            currentUserName,
                                                            winnerId,
                                                            notes.takeIf { it.isNotBlank() }
                                                        )
                                                    }
                                                }
                                            }
                                        },
                                        gradient = GoldGradient,
                                        enabled = !isLoading
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Info Card
                AnimatedEntrance(delayMillis = 300) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(
                                elevation = 4.dp,
                                spotColor = Color.Black.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(16.dp)
                            ),
                        colors = CardDefaults.cardColors(containerColor = DarkNavy),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(
                                        color = BluePrimary.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(12.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Info",
                                    tint = BluePrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.how_it_works),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = White
                                    )
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = stringResource(R.string.both_leaders_report),
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = 13.sp,
                                        color = LightGray
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

@Composable
private fun TeamBadge(name: String, isWinner: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(
                    brush = if (isWinner)
                        Brush.verticalGradient(colors = GoldGradient)
                    else
                        Brush.verticalGradient(colors = listOf(DarkSurface, DarkNavy)),
                    shape = RoundedCornerShape(16.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = name.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = White
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = White
            )
        )
    }
}

@Composable
private fun VerificationStatusBadge(status: VerificationStatus) {
    val (icon, color, label) = when (status) {
        VerificationStatus.PENDING -> Triple(Icons.Default.Schedule, WarningOrange, "Awaiting Reports")
        VerificationStatus.CONFIRMED -> Triple(Icons.Default.CheckCircle, SuccessGreen, "Confirmed")
        VerificationStatus.DISPUTED -> Triple(Icons.Default.Warning, ErrorRed, "Disputed")
        VerificationStatus.ADMIN_REVIEW -> Triple(Icons.Default.Gavel, Purple, "Admin Review")
        VerificationStatus.AUTO_CANCELLED -> Triple(Icons.Default.Cancel, ErrorRed, "Cancelled")
        VerificationStatus.ADMIN_RESOLVED -> Triple(Icons.Default.CheckCircle, LightGray, "Resolved")
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(
                color = color.copy(alpha = 0.15f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = color
            )
        )
    }
}

@Composable
private fun WinnerSelectionButton(
    teamName: String,
    teamId: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundBrush = if (isSelected) {
        Brush.horizontalGradient(colors = GoldGradient)
    } else {
        Brush.horizontalGradient(colors = listOf(DarkSurface.copy(alpha = 0.8f), DarkSurface.copy(alpha = 0.6f)))
    }

    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = backgroundBrush,
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = DarkBlue,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = stringResource(R.string.wins_exclamation, teamName),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = if (isSelected) DarkBlue else White
                    )
                )
            }
        }
    }
}

@Composable
private fun ConfirmedResultCard(matchResult: MatchResult) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                spotColor = SuccessGreen.copy(alpha = 0.3f),
                shape = RoundedCornerShape(20.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = DarkNavy),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = SuccessGreen,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.result_confirmed),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = SuccessGreen
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            val winnerName = when (matchResult.confirmedWinnerId) {
                matchResult.teamAId -> matchResult.teamAName
                matchResult.teamBId -> matchResult.teamBName
                else -> "Unknown"
            }
            Text(
                text = stringResource(R.string.winner_label, winnerName),
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 16.sp,
                    color = White
                )
            )
        }
    }
}

@Composable
private fun AlreadyReportedCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                spotColor = BluePrimary.copy(alpha = 0.3f),
                shape = RoundedCornerShape(20.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = DarkNavy),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Schedule,
                contentDescription = null,
                tint = BluePrimary,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.report_submitted),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = BluePrimary
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.waiting_other_team),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.sp,
                    color = LightGray,
                    textAlign = TextAlign.Center
                )
            )
        }
    }
}

@Composable
private fun DisputeCard(matchResult: MatchResult) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                spotColor = ErrorRed.copy(alpha = 0.3f),
                shape = RoundedCornerShape(20.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = DarkNavy),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = ErrorRed,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.result_disputed),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = ErrorRed
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.different_winner_flagged),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.sp,
                    color = LightGray,
                    textAlign = TextAlign.Center
                )
            )

            if (matchResult.adminNotes != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.admin_notes, matchResult.adminNotes),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 13.sp,
                        color = MidGray,
                        textAlign = TextAlign.Center
                    )
                )
            }
        }
    }
}

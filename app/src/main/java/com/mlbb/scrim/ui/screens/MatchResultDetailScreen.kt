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
import com.mlbb.scrim.ui.components.AnimatedEntrance
import com.mlbb.scrim.ui.components.GlassBackButton
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MatchResultDetailScreen(
    matchResult: MatchResult,
    onNavigateBack: () -> Unit,
    onNavigateToReport: ((MatchResult) -> Unit)? = null,
    currentUserTeamId: String? = null
) {
    val canReport = currentUserTeamId != null &&
            (matchResult.teamAId == currentUserTeamId || matchResult.teamBId == currentUserTeamId) &&
            matchResult.verificationStatus != VerificationStatus.CONFIRMED &&
            matchResult.pendingReporterTeamId == currentUserTeamId

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = heroGradientBrush())
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
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
                        text = "Match Details",
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
                    .padding(horizontal = 20.dp)
            ) {
                // Match Header Card
                AnimatedEntrance(delayMillis = 100) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(
                                elevation = 8.dp,
                                spotColor = when (matchResult.verificationStatus) {
                                    VerificationStatus.CONFIRMED -> SuccessGreen.copy(alpha = 0.3f)
                                    VerificationStatus.DISPUTED -> ErrorRed.copy(alpha = 0.3f)
                                    else -> BluePrimary.copy(alpha = 0.3f)
                                },
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
                                text = "Match Result",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontSize = 14.sp,
                                    color = MidGray
                                )
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TeamBlock(
                                    name = matchResult.teamAName,
                                    isWinner = matchResult.confirmedWinnerId == matchResult.teamAId,
                                    hasReported = matchResult.teamAReport != null,
                                    report = matchResult.teamAReport
                                )

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "VS",
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = GoldPrimary
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    VerificationStatusChip(matchResult.verificationStatus)
                                }

                                TeamBlock(
                                    name = matchResult.teamBName,
                                    isWinner = matchResult.confirmedWinnerId == matchResult.teamBId,
                                    hasReported = matchResult.teamBReport != null,
                                    report = matchResult.teamBReport
                                )
                            }

                            if (matchResult.confirmedWinnerId != null) {
                                Spacer(modifier = Modifier.height(16.dp))
                                val winnerName = when (matchResult.confirmedWinnerId) {
                                    matchResult.teamAId -> matchResult.teamAName
                                    matchResult.teamBId -> matchResult.teamBName
                                    else -> "Unknown"
                                }
                                Text(
                                    text = "$winnerName wins!",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = GoldPrimary
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Report Details
                if (matchResult.teamAReport != null || matchResult.teamBReport != null) {
                    AnimatedEntrance(delayMillis = 200) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(
                                    elevation = 6.dp,
                                    spotColor = Color.Black.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(16.dp)
                                ),
                            colors = CardDefaults.cardColors(containerColor = DarkNavy),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp)
                            ) {
                                Text(
                                    text = "Team Reports",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = White
                                    )
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                matchResult.teamAReport?.let { report ->
                                    ReportItem(
                                        teamName = matchResult.teamAName,
                                        reporterName = report.reporterName,
                                        reportedWinner = when (report.reportedWinnerId) {
                                            matchResult.teamAId -> matchResult.teamAName
                                            matchResult.teamBId -> matchResult.teamBName
                                            else -> "Unknown"
                                        },
                                        notes = report.notes,
                                        reportedAt = report.reportedAt
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                }

                                matchResult.teamBReport?.let { report ->
                                    ReportItem(
                                        teamName = matchResult.teamBName,
                                        reporterName = report.reporterName,
                                        reportedWinner = when (report.reportedWinnerId) {
                                            matchResult.teamAId -> matchResult.teamAName
                                            matchResult.teamBId -> matchResult.teamBName
                                            else -> "Unknown"
                                        },
                                        notes = report.notes,
                                        reportedAt = report.reportedAt
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Screenshot Section
                if (matchResult.screenshotUrl != null) {
                    AnimatedEntrance(delayMillis = 250) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(
                                    elevation = 6.dp,
                                    spotColor = Color.Black.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(16.dp)
                                ),
                            colors = CardDefaults.cardColors(containerColor = DarkNavy),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp)
                            ) {
                                Text(
                                    text = "Screenshot Evidence",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = White
                                    )
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp)
                                        .background(
                                            brush = Brush.verticalGradient(
                                                colors = listOf(
                                                    DarkSurface,
                                                    DarkNavy
                                                )
                                            ),
                                            shape = RoundedCornerShape(12.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            imageVector = Icons.Default.Image,
                                            contentDescription = null,
                                            tint = MidGray,
                                            modifier = Modifier.size(48.dp)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Screenshot placeholder",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontSize = 14.sp,
                                                color = MidGray
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Admin Notes
                if (matchResult.adminNotes != null) {
                    AnimatedEntrance(delayMillis = 300) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(
                                    elevation = 6.dp,
                                    spotColor = Purple.copy(alpha = 0.3f),
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
                                        .size(40.dp)
                                        .background(
                                            color = Purple.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(10.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Gavel,
                                        contentDescription = null,
                                        tint = Purple,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Admin Decision",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Purple
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = matchResult.adminNotes,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontSize = 14.sp,
                                            color = LightGray
                                        )
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Match Metadata
                AnimatedEntrance(delayMillis = 350) {
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
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                        ) {
                            Text(
                                text = "Match Info",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = White
                                )
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            InfoRow(label = "Match ID", value = matchResult.id.take(8))
                            InfoRow(
                                label = "Created",
                                value = formatTimestamp(matchResult.createdAt)
                            )
                            matchResult.resolvedAt?.let {
                                InfoRow(label = "Resolved", value = formatTimestamp(it))
                            }
                        }
                    }
                }

                // Report Button
                if (canReport && onNavigateToReport != null) {
                    Spacer(modifier = Modifier.height(20.dp))
                    AnimatedEntrance(delayMillis = 400) {
                        Button(
                            onClick = { onNavigateToReport(matchResult) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GoldPrimary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                tint = DarkBlue,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Report Match Result",
                                color = DarkBlue,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun TeamBlock(
    name: String,
    isWinner: Boolean,
    hasReported: Boolean,
    report: com.mlbb.scrim.data.model.TeamReport?
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(
                    brush = if (isWinner)
                        Brush.verticalGradient(colors = GoldGradient)
                    else
                        Brush.verticalGradient(
                            colors = listOf(
                                DarkSurface,
                                DarkNavy
                            )
                        ),
                    shape = RoundedCornerShape(16.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = name.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = White
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 14.sp,
                fontWeight = if (isWinner) FontWeight.Bold else FontWeight.Medium,
                color = if (isWinner) GoldPrimary else White
            )
        )

        if (hasReported) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = SuccessGreen,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Reported",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.sp,
                        color = SuccessGreen
                    )
                )
            }
        }
    }
}

@Composable
private fun VerificationStatusChip(status: VerificationStatus) {
    val (icon, color, label) = when (status) {
        VerificationStatus.PENDING -> Triple(
            Icons.Default.Schedule,
            WarningOrange,
            "Pending"
        )
        VerificationStatus.CONFIRMED -> Triple(
            Icons.Default.CheckCircle,
            SuccessGreen,
            "Confirmed"
        )
        VerificationStatus.DISPUTED -> Triple(
            Icons.Default.Warning,
            ErrorRed,
            "Disputed"
        )
        VerificationStatus.ADMIN_REVIEW -> Triple(
            Icons.Default.Gavel,
            Purple,
            "Review"
        )
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(
                color = color.copy(alpha = 0.15f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = color
            )
        )
    }
}

@Composable
private fun ReportItem(
    teamName: String,
    reporterName: String,
    reportedWinner: String,
    notes: String?,
    reportedAt: Long
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = DarkSurface.copy(alpha = 0.6f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = teamName,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = White
                    )
                )
                Text(
                    text = formatTimestamp(reportedAt),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.sp,
                        color = MidGray
                    )
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Reporter: $reporterName",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 13.sp,
                    color = LightGray
                )
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Claimed winner: $reportedWinner",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 13.sp,
                    color = BluePrimary,
                    fontWeight = FontWeight.Medium
                )
            )

            if (!notes.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Notes: $notes",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 12.sp,
                        color = MidGray
                    )
                )
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 13.sp,
                color = MidGray
            )
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 13.sp,
                color = LightGray
            )
        )
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

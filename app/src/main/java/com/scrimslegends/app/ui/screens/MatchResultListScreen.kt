package com.scrimslegends.app.ui.screens

import androidx.compose.material3.MaterialTheme
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.border
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.scrimslegends.app.data.model.MatchResult
import com.scrimslegends.app.data.model.VerificationStatus
import com.scrimslegends.app.ui.theme.*
import com.scrimslegends.app.R
import androidx.compose.ui.res.stringResource
import com.scrimslegends.app.ui.components.AnimatedEntrance
import com.scrimslegends.app.ui.components.GlassBackButton
import com.scrimslegends.app.ui.components.EmptyState
import com.scrimslegends.app.ui.components.PremiumLoadingState
import com.scrimslegends.app.ui.components.SectionHeader
import com.scrimslegends.app.ui.components.PullToRefreshContainer
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MatchResultListScreen(
    matchResults: List<MatchResult>,
    isLoading: Boolean,
    isRefreshing: Boolean = false,
    onNavigateBack: () -> Unit,
    onNavigateToMatchResultDetail: (MatchResult) -> Unit,
    onNavigateToReportResult: ((MatchResult) -> Unit)? = null,
    currentUserTeamId: String? = null,
    onRefresh: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = heroGradientBrush())
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
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
                        text = stringResource(R.string.match_history_title),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )

                    IconButton(
                        onClick = onRefresh,
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(12.dp)
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.refresh),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            PullToRefreshContainer(
                isRefreshing = isRefreshing,
                onRefresh = onRefresh,
                modifier = Modifier.weight(1f)
            ) {
                when {
                    isLoading && matchResults.isEmpty() -> {
                        PremiumLoadingState(
                            message = "Loading matches...",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    matchResults.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            EmptyState(
                                icon = Icons.Default.SportsEsports,
                                title = stringResource(R.string.no_matches_yet),
                                subtitle = stringResource(R.string.complete_scrim_to_see_results)
                            )
                        }
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 20.dp),
                            contentPadding = PaddingValues(vertical = 16.dp)
                        ) {
                            item {
                                SectionHeader(
                                    title = "Recent Matches (${matchResults.size})"
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                            }

                            itemsIndexed(matchResults, key = { _, r -> r.id }) { index, result ->
                                AnimatedEntrance(delayMillis = index * 60) {
                                    MatchResultCard(
                                        matchResult = result,
                                        onClick = { onNavigateToMatchResultDetail(result) },
                                        onReportClick = onNavigateToReportResult?.let { cb ->
                                            { cb(result) }
                                        },
                                        currentUserTeamId = currentUserTeamId
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MatchResultCard(
    matchResult: MatchResult,
    onClick: () -> Unit,
    onReportClick: (() -> Unit)? = null,
    currentUserTeamId: String? = null
) {
    val canReport = currentUserTeamId != null &&
            (matchResult.teamAId == currentUserTeamId || matchResult.teamBId == currentUserTeamId) &&
            matchResult.verificationStatus != VerificationStatus.CONFIRMED

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = when (matchResult.verificationStatus) {
                    VerificationStatus.CONFIRMED -> SuccessGreen.copy(alpha = 0.30f)
                    VerificationStatus.DISPUTED  -> ErrorRed.copy(alpha = 0.30f)
                    else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)
                },
                shape = RoundedCornerShape(16.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .clickable { onClick() }
        ) {
            // Teams row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TeamNameWithStatus(
                    name = matchResult.teamAName,
                    isWinner = matchResult.confirmedWinnerId == matchResult.teamAId,
                    hasReported = matchResult.teamAReport != null
                )

                Text(
                    text = stringResource(R.string.vs_label),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    ),
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                TeamNameWithStatus(
                    name = matchResult.teamBName,
                    isWinner = matchResult.confirmedWinnerId == matchResult.teamBId,
                    hasReported = matchResult.teamBReport != null
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), thickness = 1.dp)

            Spacer(modifier = Modifier.height(12.dp))

            // Bottom row: status + date + report button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusDot(matchResult.verificationStatus)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = formatStatus(matchResult.verificationStatus),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 13.sp,
                            color = statusColor(matchResult.verificationStatus)
                        )
                    )
                }

                Text(
                    text = formatDate(matchResult.createdAt),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }

            if (canReport && onReportClick != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onReportClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.report_result),
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun TeamNameWithStatus(
    name: String,
    isWinner: Boolean,
    hasReported: Boolean
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isWinner) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 15.sp,
                    fontWeight = if (isWinner) FontWeight.Bold else FontWeight.Medium,
                    color = if (isWinner) MaterialTheme.colorScheme.secondary else White
                )
            )
        }
        if (hasReported) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = stringResource(R.string.reported),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 10.sp,
                    color = SuccessGreen.copy(alpha = 0.8f)
                )
            )
        }
    }
}

@Composable
private fun StatusDot(status: VerificationStatus) {
    Box(
        modifier = Modifier
            .size(8.dp)
            .background(
                color = statusColor(status),
                shape = RoundedCornerShape(4.dp)
            )
    )
}

@Composable
private fun statusColor(status: VerificationStatus): Color {
    return when (status) {
        VerificationStatus.PENDING -> WarningOrange
        VerificationStatus.CONFIRMED -> SuccessGreen
        VerificationStatus.DISPUTED -> ErrorRed
        VerificationStatus.ADMIN_REVIEW -> Purple
        VerificationStatus.AUTO_CANCELLED -> ErrorRed
        VerificationStatus.ADMIN_RESOLVED -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
    }
}

private fun formatStatus(status: VerificationStatus): String {
    return when (status) {
        VerificationStatus.PENDING -> "Pending"
        VerificationStatus.CONFIRMED -> "Confirmed"
        VerificationStatus.DISPUTED -> "Disputed"
        VerificationStatus.ADMIN_REVIEW -> "Review"
        VerificationStatus.AUTO_CANCELLED -> "Cancelled"
        VerificationStatus.ADMIN_RESOLVED -> "Resolved"
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

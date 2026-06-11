package com.scrimslegends.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scrimslegends.app.data.model.MatchResult
import com.scrimslegends.app.data.model.MatchType
import com.scrimslegends.app.data.model.VerificationStatus
import com.scrimslegends.app.ui.theme.*
import com.scrimslegends.app.R
import androidx.compose.ui.res.stringResource
import com.scrimslegends.app.ui.components.AnimatedEntrance
import com.scrimslegends.app.ui.components.GlassBackButton
import com.scrimslegends.app.ui.components.PullToRefreshContainer
import com.scrimslegends.app.ui.components.LottieLoadingIndicator
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchHistoryScreen(
    matchResults: List<MatchResult>,
    isLoading: Boolean,
    isRefreshing: Boolean = false,
    currentUserTeamIds: Set<String>,
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (MatchResult) -> Unit,
    onRefresh: () -> Unit = {},
    error: String? = null
) {
    val appTextPrimary = appTextPrimaryColor()
    val appTextSecondary = appTextSecondaryColor()
    val appElevatedSurface = appElevatedSurfaceColor()

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
                            color = appTextPrimary
                        )
                    )

                    IconButton(
                        onClick = onRefresh,
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                color = appElevatedSurface,
                                shape = RoundedCornerShape(12.dp)
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.refresh),
                            tint = appTextSecondary,
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
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            LottieLoadingIndicator(size = 40.dp)
                        }
                    }
                    matchResults.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = null,
                                    tint = appTextSecondary.copy(alpha = 0.55f),
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = stringResource(R.string.no_matches_yet),
                                    color = appTextPrimary,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = stringResource(R.string.complete_scrim_history),
                                    color = appTextSecondary,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 100.dp)
                        ) {
                            itemsIndexed(matchResults, key = { _, m -> m.id }) { index, match ->
                                AnimatedEntrance(delayMillis = 100 + index * 60) {
                                    MatchHistoryCard(
                                        match = match,
                                        currentUserTeamIds = currentUserTeamIds,
                                        onClick = { onNavigateToDetail(match) }
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                            }
                        }
                    }
                }
            }
        }

        if (error != null) {
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .padding(bottom = 80.dp),
                containerColor = ErrorRed,
                contentColor = White
            ) {
                Text(text = error)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MatchHistoryCard(
    match: MatchResult,
    currentUserTeamIds: Set<String>,
    onClick: () -> Unit
) {
    val isParticipant = match.teamAId in currentUserTeamIds || match.teamBId in currentUserTeamIds
    val isWinner = match.confirmedWinnerId != null && match.confirmedWinnerId in currentUserTeamIds
    val isDraw = match.isDraw
    val statusColor = when (match.verificationStatus) {
        VerificationStatus.CONFIRMED -> if (isDraw) MidGray else if (!isParticipant) SuccessGreen else if (isWinner) SuccessGreen else ErrorRed
        VerificationStatus.PENDING -> WarningOrange
        VerificationStatus.DISPUTED -> Purple
        VerificationStatus.ADMIN_REVIEW -> BluePrimary
        VerificationStatus.AUTO_CANCELLED -> ErrorRed
        VerificationStatus.ADMIN_RESOLVED -> LightGray
    }

    val statusText = when (match.verificationStatus) {
        VerificationStatus.CONFIRMED -> if (isDraw) "Draw" else if (!isParticipant) "Completed" else if (isWinner) "Victory" else "Defeat"
        VerificationStatus.PENDING -> "Pending"
        VerificationStatus.DISPUTED -> "Disputed"
        VerificationStatus.ADMIN_REVIEW -> "Under Review"
        VerificationStatus.AUTO_CANCELLED -> "Auto-Cancelled"
        VerificationStatus.ADMIN_RESOLVED -> "Resolved"
    }
    val appSurface = appSurfaceColor()
    val appElevatedSurface = appElevatedSurfaceColor()
    val appTextSecondary = appTextSecondaryColor()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 6.dp,
                spotColor = statusColor.copy(alpha = 0.2f),
                shape = RoundedCornerShape(16.dp)
            )
            .drawBehind {
                drawRect(
                    color = statusColor,
                    size = androidx.compose.ui.geometry.Size(12.dp.toPx(), size.height)
                )
            },
        colors = CardDefaults.cardColors(containerColor = appSurface),
        shape = RoundedCornerShape(16.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(
                                color = statusColor,
                                shape = CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = statusText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = statusColor
                    )
                    if (match.matchType == MatchType.TOURNAMENT) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .background(Purple.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                .border(1.dp, Purple.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.tournament_match),
                                color = Purple,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Text(
                    text = SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(match.createdAt)),
                    fontSize = 12.sp,
                    color = appTextSecondary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Tournament info
            if (match.matchType == MatchType.TOURNAMENT && match.tournamentTitle != null) {
                Text(
                    text = "${match.tournamentTitle}${match.roundNumber?.let { " - Round $it" } ?: ""}",
                    fontSize = 12.sp,
                    color = Purple.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(6.dp))
            }

            // Teams
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TeamNameBox(match.teamAName, match.teamAId == match.confirmedWinnerId)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.vs_label),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = appTextSecondary
                )
                Spacer(modifier = Modifier.width(8.dp))
                TeamNameBox(match.teamBName, match.teamBId == match.confirmedWinnerId)
            }

            if (match.verificationStatus == VerificationStatus.CONFIRMED && !match.isDraw) {
                Spacer(modifier = Modifier.height(8.dp))
                val winnerName = if (match.confirmedWinnerId == match.teamAId) match.teamAName else match.teamBName
                Text(
                    text = stringResource(R.string.winner_result, winnerName),
                    fontSize = 12.sp,
                    color = SuccessGreen,
                    fontWeight = FontWeight.Medium
                )
            }

            if (match.isDisputed) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.result_disputed_waiting),
                    fontSize = 12.sp,
                    color = WarningOrange
                )
            }
        }
    }
}

@Composable
private fun TeamNameBox(name: String, isWinner: Boolean) {
    val appElevatedSurface = appElevatedSurfaceColor()
    val appTextPrimary = appTextPrimaryColor()
    Box(
        modifier = Modifier
            .background(
                color = if (isWinner) SuccessGreen.copy(alpha = 0.12f) else appElevatedSurface,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = name,
            fontSize = 13.sp,
            fontWeight = if (isWinner) FontWeight.Bold else FontWeight.Medium,
            color = if (isWinner) SuccessGreen else appTextPrimary
        )
    }
}

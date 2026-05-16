package com.mlbb.scrim.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mlbb.scrim.data.model.MatchResult
import com.mlbb.scrim.data.model.VerificationStatus
import com.mlbb.scrim.ui.theme.*
import com.mlbb.scrim.R
import androidx.compose.ui.res.stringResource
import com.mlbb.scrim.ui.components.AnimatedEntrance
import com.mlbb.scrim.ui.components.GlassBackButton
import com.mlbb.scrim.ui.components.PullToRefreshContainer
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchHistoryScreen(
    matchResults: List<MatchResult>,
    isLoading: Boolean,
    currentUserTeamId: String?,
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (MatchResult) -> Unit,
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
                            color = White
                        )
                    )

                    IconButton(
                        onClick = onRefresh,
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                color = Color.White.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(12.dp)
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = LightGray,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            PullToRefreshContainer(
                isRefreshing = isLoading,
                onRefresh = onRefresh,
                modifier = Modifier.weight(1f)
            ) {
                when {
                    isLoading && matchResults.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = GoldPrimary)
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
                                    tint = LightGray.copy(alpha = 0.5f),
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = stringResource(R.string.no_matches_yet),
                                    color = LightGray,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = stringResource(R.string.complete_scrim_history),
                                    color = MidGray,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 20.dp),
                            contentPadding = PaddingValues(vertical = 12.dp)
                        ) {
                            itemsIndexed(matchResults) { index, match ->
                                AnimatedEntrance(delayMillis = 100 + index * 60) {
                                    MatchHistoryCard(
                                        match = match,
                                        currentUserTeamId = currentUserTeamId,
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
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MatchHistoryCard(
    match: MatchResult,
    currentUserTeamId: String?,
    onClick: () -> Unit
) {
    val isWinner = match.confirmedWinnerId == currentUserTeamId
    val statusColor = when (match.verificationStatus) {
        VerificationStatus.CONFIRMED -> if (isWinner) SuccessGreen else ErrorRed
        VerificationStatus.PENDING -> WarningOrange
        VerificationStatus.DISPUTED -> Purple
        VerificationStatus.ADMIN_REVIEW -> BluePrimary
        VerificationStatus.AUTO_CANCELLED -> ErrorRed
        VerificationStatus.ADMIN_RESOLVED -> LightGray
    }

    val statusText = when (match.verificationStatus) {
        VerificationStatus.CONFIRMED -> if (isWinner) "Victory" else "Defeat"
        VerificationStatus.PENDING -> "Pending"
        VerificationStatus.DISPUTED -> "Disputed"
        VerificationStatus.ADMIN_REVIEW -> "Under Review"
        VerificationStatus.AUTO_CANCELLED -> "Auto-Cancelled"
        VerificationStatus.ADMIN_RESOLVED -> "Resolved"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                spotColor = statusColor.copy(alpha = 0.1f),
                shape = RoundedCornerShape(16.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = DarkNavy),
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
                }
                Text(
                    text = SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(match.createdAt)),
                    fontSize = 12.sp,
                    color = MidGray
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

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
                    color = MidGray
                )
                Spacer(modifier = Modifier.width(8.dp))
                TeamNameBox(match.teamBName, match.teamBId == match.confirmedWinnerId)
            }

            if (match.verificationStatus == VerificationStatus.CONFIRMED) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.winner_result, if (match.confirmedWinnerId == match.teamAId) match.teamAName else match.teamBName),
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
    Box(
        modifier = Modifier
            .background(
                color = if (isWinner) SuccessGreen.copy(alpha = 0.12f) else White.copy(alpha = 0.06f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = name,
            fontSize = 13.sp,
            fontWeight = if (isWinner) FontWeight.Bold else FontWeight.Medium,
            color = if (isWinner) SuccessGreen else White
        )
    }
}

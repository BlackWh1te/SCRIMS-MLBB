package com.mlbb.scrim.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mlbb.scrim.data.model.LeaderboardEntry
import com.mlbb.scrim.data.model.RankTier
import com.mlbb.scrim.R
import androidx.compose.ui.res.stringResource
import com.mlbb.scrim.ui.theme.*
import com.mlbb.scrim.ui.components.AnimatedEntrance
import com.mlbb.scrim.ui.components.GlassBackButton
import com.mlbb.scrim.ui.components.RankBadge
import com.mlbb.scrim.ui.components.RankBadgeSize
import com.mlbb.scrim.ui.components.PullToRefreshContainer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(
    entries: List<LeaderboardEntry>,
    isLoading: Boolean,
    isRefreshing: Boolean = false,
    error: String?,
    selectedTier: RankTier? = null,
    onTierFilter: (RankTier?) -> Unit = {},
    onNavigateBack: () -> Unit,
    onRefresh: () -> Unit = {},
    onDismissError: () -> Unit = {}
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
                        .padding(start = 20.dp, top = 20.dp, end = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GlassBackButton(onClick = onNavigateBack)

                    Text(
                        text = stringResource(R.string.leaderboard),
                        style = iOSTitle2.copy(color = White)
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

            // Error display
            if (error != null) {
                AnimatedEntrance(delayMillis = 0) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = ErrorRed.copy(alpha = 0.1f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = "Error",
                                    tint = ErrorRed,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = error,
                                    color = White,
                                    fontSize = 13.sp
                                )
                            }
                            IconButton(
                                onClick = onDismissError,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss",
                                    tint = White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            PullToRefreshContainer(
                isRefreshing = isRefreshing,
                onRefresh = onRefresh,
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Tier Filter Chips
                    AnimatedEntrance(delayMillis = 100) {
                        ScrollableTierFilter(
                            selectedTier = selectedTier,
                            onTierSelected = onTierFilter
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    when {
                        isLoading && entries.isEmpty() -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = GoldPrimary)
                            }
                        }
                        entries.isEmpty() -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(40.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EmojiEvents,
                                    contentDescription = null,
                                    tint = LightGray.copy(alpha = 0.4f),
                                    modifier = Modifier.size(72.dp)
                                )
                                Spacer(modifier = Modifier.height(20.dp))
                                Text(
                                    text = stringResource(R.string.no_entries),
                                    style = iOSTitle3.copy(color = White)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Complete scrims to earn points and climb the ranks",
                                    style = iOSFootnote.copy(color = MidGray),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                        else -> {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 20.dp),
                                contentPadding = PaddingValues(vertical = 12.dp)
                            ) {
                                itemsIndexed(entries) { index, entry ->
                                    AnimatedEntrance(delayMillis = 150 + index * 60) {
                                        LeaderboardRow(
                                            entry = entry,
                                            isTopThree = entry.rank <= 3
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
}

@Composable
private fun ScrollableTierFilter(
    selectedTier: RankTier?,
    onTierSelected: (RankTier?) -> Unit
) {
    val tiers = listOf(null) + RankTier.values().toList()
    val scrollState = rememberScrollState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        tiers.forEach { tier ->
            val isSelected = selectedTier == tier
            val display = tier?.displayName ?: "All"
            val chipColor = when (tier) {
                RankTier.BRONZE -> Bronze
                RankTier.SOLVER -> SolverBlue
                RankTier.GOLD -> GoldRank
                RankTier.GRANDMASTER -> GrandmasterPurple
                RankTier.EPIC -> EpicCyan
                RankTier.LEGEND -> LegendRed
                RankTier.MYTHIC -> MythicCrimson
                null -> BluePrimary
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(9999.dp))
                    .background(
                        color = if (isSelected) chipColor.copy(alpha = 0.25f)
                        else Color.White.copy(alpha = 0.06f)
                    )
                    .clickable { onTierSelected(tier) }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    text = display,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) chipColor else MidGray
                )
            }
        }
    }
}

@Composable
private fun LeaderboardRow(
    entry: LeaderboardEntry,
    isTopThree: Boolean
) {
    val rankColor = when (entry.rank) {
        1 -> GoldPrimary
        2 -> Silver
        3 -> Bronze
        else -> MidGray
    }

    val rankBg = when (entry.rank) {
        1 -> GoldPrimary.copy(alpha = 0.12f)
        2 -> Silver.copy(alpha = 0.12f)
        3 -> Bronze.copy(alpha = 0.12f)
        else -> Color.Transparent
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (isTopThree) 6.dp else 3.dp,
                spotColor = if (isTopThree) rankColor.copy(alpha = 0.2f) else Color.Black.copy(alpha = 0.05f),
                shape = RoundedCornerShape(16.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isTopThree) DarkSurface else DarkNavy
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rank number
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = rankBg,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (entry.rank <= 3) {
                    Icon(
                        imageVector = when (entry.rank) {
                            1 -> Icons.Default.EmojiEvents
                            else -> Icons.Default.Star
                        },
                        contentDescription = null,
                        tint = rankColor,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Text(
                        text = entry.rank.toString(),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = rankColor
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Avatar
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .shadow(
                        elevation = 4.dp,
                        spotColor = BluePrimary.copy(alpha = 0.2f),
                        shape = CircleShape
                    )
                    .clip(CircleShape)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(BluePrimary.copy(alpha = 0.4f), BluePrimary.copy(alpha = 0.1f))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = entry.username.firstOrNull()?.uppercaseChar()?.toString() ?: "P",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = BluePrimary
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Name & team
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.username,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = White
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = entry.teamName,
                    fontSize = 12.sp,
                    color = MidGray
                )
            }

            // Stats
            Column(horizontalAlignment = Alignment.End) {
                RankBadge(tier = entry.currentTier, size = RankBadgeSize.MEDIUM)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "${entry.xp} XP",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = GoldPrimary
                )
                Text(
                    text = "${entry.wins}W / ${entry.losses}L",
                    fontSize = 11.sp,
                    color = MidGray
                )
            }
        }
    }
}

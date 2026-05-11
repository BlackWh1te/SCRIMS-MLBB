package com.mlbb.scrim.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mlbb.scrim.data.model.LeaderboardEntry
import com.mlbb.scrim.data.model.RankTier
import com.mlbb.scrim.ui.theme.*
import com.mlbb.scrim.ui.components.AnimatedEntrance
import com.mlbb.scrim.ui.components.GlassBackButton
import com.mlbb.scrim.ui.components.TierBadge

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(
    entries: List<LeaderboardEntry>,
    isLoading: Boolean,
    selectedTier: RankTier? = null,
    onTierFilter: (RankTier?) -> Unit = {},
    onNavigateBack: () -> Unit,
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
                        text = "Leaderboard",
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

            // Tier Filter Chips
            AnimatedEntrance(delayMillis = 100) {
                ScrollableTierFilter(
                    selectedTier = selectedTier,
                    onTierSelected = onTierFilter
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = GoldPrimary)
                }
            } else if (entries.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = null,
                            tint = LightGray.copy(alpha = 0.5f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No entries yet",
                            color = LightGray,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    itemsIndexed(entries) { index, entry ->
                        AnimatedEntrance(delayMillis = 150 + index * 80) {
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

@Composable
private fun ScrollableTierFilter(
    selectedTier: RankTier?,
    onTierSelected: (RankTier?) -> Unit
) {
    val tiers = listOf(null) + RankTier.values().toList()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        tiers.forEach { tier ->
            val isSelected = selectedTier == tier
            val display = tier?.displayName ?: "All"
            val chipColor = when (tier) {
                RankTier.BRONZE -> Bronze
                RankTier.SILVER -> Silver
                RankTier.GOLD -> GoldPrimary
                RankTier.PLATINUM -> Platinum
                RankTier.DIAMOND -> Diamond
                RankTier.MASTER -> Purple
                RankTier.GRANDMASTER -> ErrorRed
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
                TierBadge(tierName = entry.currentTier.displayName)
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

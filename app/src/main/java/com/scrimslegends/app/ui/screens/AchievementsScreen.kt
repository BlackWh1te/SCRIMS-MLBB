package com.scrimslegends.app.ui.screens

import androidx.compose.material3.MaterialTheme
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scrimslegends.app.R
import com.scrimslegends.app.data.model.Achievement
import com.scrimslegends.app.data.model.AchievementCategory
import com.scrimslegends.app.data.model.PlayerAchievements
import com.scrimslegends.app.ui.components.AchievementCard
import com.scrimslegends.app.ui.components.AchievementBadge
import com.scrimslegends.app.ui.components.AnimatedEntrance
import com.scrimslegends.app.ui.components.GlassBackButton
import com.scrimslegends.app.ui.components.LottieLoadingIndicator
import com.scrimslegends.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementsScreen(
    achievements: PlayerAchievements?,
    isLoading: Boolean = false,
    error: String? = null,
    onNavigateBack: () -> Unit
) {
    var selectedCategory by remember { mutableStateOf<AchievementCategory?>(null) }
    val allAchievements = Achievement.values().toList()
    val unlockedCount = if (achievements != null) allAchievements.count { achievements.isUnlocked(it) } else 0
    val progress = if (allAchievements.isNotEmpty()) unlockedCount.toFloat() / allAchievements.size else 0f

    val displayAchievements = if (selectedCategory == null) {
        allAchievements
    } else {
        allAchievements.filter { it.category == selectedCategory }
    }

    // Find closest achievement
    val closestAchievement = if (achievements != null) {
        allAchievements
            .filter { !achievements.isUnlocked(it) && it.condition.maxProgress > 1 }
            .maxByOrNull { 
                val pct = achievements.getProgressPercentage(it)
                if (pct.isNaN()) 0f else pct
            } ?: allAchievements.firstOrNull()
    } else {
        allAchievements.firstOrNull()
    }

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
                        text = "Trophy Room",
                        style = iOSTitle2.copy(color = MaterialTheme.colorScheme.onSurface)
                    )

                    Text(
                        text = "$unlockedCount / ${allAchievements.size}",
                        style = iOSTitle3.copy(color = MaterialTheme.colorScheme.secondary)
                    )
                }
            }

            // Up Next Hero Card
            AnimatedEntrance(delayMillis = 50) {
                if (closestAchievement != null && achievements != null && !isLoading && error == null) {
                    UpNextCard(closestAchievement, achievements)
                } else if (isLoading) {
                    Box(modifier = Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                        LottieLoadingIndicator(size = 80.dp)
                    }
                } else if (error != null) {
                    Text(
                        text = error,
                        color = ErrorRed,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Categories Filter
            AnimatedEntrance(delayMillis = 100) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        CategoryChip(
                            title = "All",
                            isSelected = selectedCategory == null,
                            onClick = { selectedCategory = null }
                        )
                    }
                    items(AchievementCategory.values()) { category ->
                        CategoryChip(
                            title = category.title,
                            isSelected = selectedCategory == category,
                            onClick = { selectedCategory = category }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Achievement list
            if (achievements != null && !isLoading) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(displayAchievements, key = { it.id }) { achievement ->
                        AnimatedEntrance(delayMillis = 150) {
                            AchievementCard(
                                achievement = achievement,
                                stats = achievements
                            )
                        }
                    }
                }
            } else if (!isLoading && error == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No achievements available", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun UpNextCard(achievement: Achievement, stats: PlayerAchievements) {
    val rawPercentage = stats.getProgressPercentage(achievement)
    val percentage = if (rawPercentage.isNaN()) 0f else rawPercentage.coerceIn(0f, 1f)
    val current = stats.getProgress(achievement)
    val max = achievement.condition.maxProgress

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.surface.copy(alpha=0.8f), MaterialTheme.colorScheme.background.copy(alpha=0.6f))))
            .padding(2.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "🌟 UP NEXT",
                    color = MaterialTheme.colorScheme.secondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "${(percentage * 100).toInt()}%",
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                AchievementBadge(
                    achievement = achievement,
                    size = com.scrimslegends.app.ui.components.AchievementBadgeSize.LARGE
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(text = achievement.displayName, color = MaterialTheme.colorScheme.onSurface, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(text = achievement.description, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f), fontSize = 13.sp)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            
            // Progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), RoundedCornerShape(4.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(percentage)
                        .height(8.dp)
                        .background(
                            brush = Brush.horizontalGradient(listOf(achievement.badgeColor, achievement.glowColor)),
                            shape = RoundedCornerShape(4.dp)
                        )
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "$current / $max ${if(max > 1) "Progress" else "Completed"}",
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                fontSize = 11.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.End
            )
        }
    }
}

@Composable
fun CategoryChip(title: String, isSelected: Boolean, onClick: () -> Unit) {
    val bgColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
    val textColor = if (isSelected) White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = title,
            color = textColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

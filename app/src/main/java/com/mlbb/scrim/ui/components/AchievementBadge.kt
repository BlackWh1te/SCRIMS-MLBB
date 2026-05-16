package com.mlbb.scrim.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mlbb.scrim.data.model.Achievement
import com.mlbb.scrim.ui.theme.*
import com.mlbb.scrim.R
import androidx.compose.ui.res.stringResource

// ═══════════════════════════════════════════════════════════════════
// ACHIEVEMENT BADGE — Compact icon for profile, leaderboard, etc.
// ═══════════════════════════════════════════════════════════════════

@Composable
fun AchievementBadge(
    achievement: Achievement,
    modifier: Modifier = Modifier,
    size: AchievementBadgeSize = AchievementBadgeSize.MEDIUM
) {
    val s = size.dp.dp
    val shape = RoundedCornerShape(size.corner.dp)

    Box(
        modifier = modifier
            .size(s)
            .shadow(
                elevation = 4.dp,
                spotColor = achievement.glowColor,
                shape = shape
            ),
        contentAlignment = Alignment.Center
    ) {
        val context = androidx.compose.ui.platform.LocalContext.current
        val resourceId = remember(achievement.id) {
            val resName = "badge_${achievement.id}"
            context.resources.getIdentifier(resName, "drawable", context.packageName)
        }
        
        if (resourceId != 0) {
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(id = resourceId),
                contentDescription = achievement.displayName,
                modifier = Modifier.fillMaxSize(),
                contentScale = androidx.compose.ui.layout.ContentScale.Fit
            )
        } else {
            // Fallback
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                achievement.badgeColor.copy(alpha = 0.9f),
                                achievement.badgeColor.copy(alpha = 0.5f)
                            )
                        ),
                        shape = shape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = achievement.iconLetter,
                    fontSize = (size.dp * 0.45).sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }
        }
    }
}

enum class AchievementBadgeSize(val dp: Int, val corner: Int) {
    SMALL(28, 6),
    MEDIUM(40, 8),
    LARGE(56, 10)
}

// ═══════════════════════════════════════════════════════════════════
// ACHIEVEMENT CARD — Full detail card for the achievements screen
// ═══════════════════════════════════════════════════════════════════

@Composable
fun AchievementCard(
    achievement: Achievement,
    stats: com.mlbb.scrim.data.model.PlayerAchievements,
    onClick: () -> Unit = {}
) {
    val isUnlocked = stats.isUnlocked(achievement)
    val percentage = stats.getProgressPercentage(achievement)
    val current = stats.getProgress(achievement)
    val max = achievement.condition.maxProgress
    
    val alpha = if (isUnlocked) 1f else 0.45f
    val shape = RoundedCornerShape(16.dp)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (isUnlocked) 4.dp else 1.dp,
                spotColor = if (isUnlocked) achievement.glowColor else Color.Black.copy(alpha = 0.05f),
                shape = shape
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isUnlocked) DarkNavy else DarkNavy.copy(alpha = 0.6f)
        ),
        shape = shape,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Badge icon
            AchievementBadge(
                achievement = achievement,
                size = AchievementBadgeSize.LARGE,
                modifier = Modifier.alpha(alpha)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = achievement.displayName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isUnlocked) White else LightGray.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = achievement.description,
                    fontSize = 13.sp,
                    color = if (isUnlocked) LightGray else LightGray.copy(alpha = 0.4f),
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                
                // Progress Bar
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(3.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(percentage)
                                .height(6.dp)
                                .background(
                                    brush = Brush.horizontalGradient(listOf(achievement.badgeColor, achievement.glowColor)),
                                    shape = RoundedCornerShape(3.dp)
                                )
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "$current / $max",
                        fontSize = 11.sp,
                        color = LightGray,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// ACHIEVEMENT ROW — Horizontal scrollable badges for profile
// ═══════════════════════════════════════════════════════════════════

@Composable
fun AchievementBadgeRow(
    achievements: List<Achievement>,
    modifier: Modifier = Modifier
) {
    if (achievements.isEmpty()) return

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        achievements.take(5).forEach { achievement ->
            AchievementBadge(achievement = achievement, size = AchievementBadgeSize.SMALL)
        }
        if (achievements.size > 5) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(
                        color = White.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(6.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+${achievements.size - 5}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = LightGray
                )
            }
        }
    }
}

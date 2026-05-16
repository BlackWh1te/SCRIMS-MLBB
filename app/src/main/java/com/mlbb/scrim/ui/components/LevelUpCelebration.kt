package com.mlbb.scrim.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.mlbb.scrim.data.model.RankTier
import com.mlbb.scrim.ui.theme.*
import com.mlbb.scrim.R
import androidx.compose.ui.res.stringResource

/**
 * Animated rank-up celebration overlay.
 * Shows when a player crosses into a new tier.
 */
@Composable
fun LevelUpCelebration(
    newTier: RankTier,
    onDismiss: () -> Unit
) {
    var visible by remember { mutableStateOf(true) }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(300)) + scaleIn(tween(400, easing = AppEaseOutCubic)),
        exit = fadeOut(tween(300)) + scaleOut(tween(300))
    ) {
        Dialog(onDismissRequest = {
            visible = false
            onDismiss()
        }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                newTier.tierColor.copy(alpha = 0.2f),
                                DarkNavy.copy(alpha = 0.95f)
                            )
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .padding(32.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Animated badge
                    val scale by rememberInfiniteTransition(label = "pulse").animateFloat(
                        initialValue = 0.95f,
                        targetValue = 1.05f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(800, easing = AppEaseOutCubic),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "badgePulse"
                    )

                    Box(modifier = Modifier.scale(scale)) {
                        RankBadge(tier = newTier, size = RankBadgeSize.LARGE)
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = stringResource(R.string.rank_up),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        color = White
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = stringResource(R.string.you_reached),
                        fontSize = 16.sp,
                        color = LightGray
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = newTier.displayName,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = newTier.tierColor
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    GradientButton(
                        text = stringResource(R.string.awesome),
                        onClick = {
                            visible = false
                            onDismiss()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    )
                }
            }
        }
    }
}

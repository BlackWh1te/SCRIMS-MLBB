package com.mlbb.scrim.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mlbb.scrim.ui.theme.*

@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit
) {
    // Animations
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    var textVisible by remember { mutableStateOf(false) }
    var taglineVisible by remember { mutableStateOf(false) }

    // Entrance + exit orchestration
    var contentVisible by remember { mutableStateOf(true) }
    var exitAnimation by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        textVisible = true
        kotlinx.coroutines.delay(600)
        taglineVisible = true
        kotlinx.coroutines.delay(2000)
        exitAnimation = true
        kotlinx.coroutines.delay(500)
        contentVisible = false
        onSplashFinished()
    }

    if (!contentVisible) return

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0A1628),
                        Color(0xFF0D1B2A),
                        Color(0xFF1E3A5F)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Animated Logo Container
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .scale(if (exitAnimation) 0.8f else pulseScale),
                contentAlignment = Alignment.Center
            ) {
                // Outer glow ring
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(GoldPrimary.copy(alpha = glowAlpha))
                )

                // Inner dark circle
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF1E3A5F),
                                    Color(0xFF0D1B2A)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "ML",
                        fontSize = 42.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = GoldPrimary,
                        modifier = Modifier.scale(if (exitAnimation) 0.9f else 1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // App Name
            AnimatedVisibility(
                visible = textVisible && !exitAnimation,
                enter = fadeIn(tween(800)) + slideInVertically(
                    initialOffsetY = { it / 3 },
                    animationSpec = tween(800, easing = EaseOutCubic)
                ),
                exit = fadeOut(tween(300)) + slideOutVertically(
                    targetOffsetY = { -it / 4 },
                    animationSpec = tween(300)
                )
            ) {
                Text(
                    text = "MLBB Scrim Host",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = White
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Tagline
            AnimatedVisibility(
                visible = taglineVisible && !exitAnimation,
                enter = fadeIn(tween(600)) + slideInVertically(
                    initialOffsetY = { it / 4 },
                    animationSpec = tween(600, easing = EaseOutCubic)
                ),
                exit = fadeOut(tween(200))
            ) {
                Text(
                    text = "Compete. Rank. Dominate.",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = GoldPrimary.copy(alpha = 0.8f),
                        letterSpacing = 2.sp
                    )
                )
            }
        }
    }
}

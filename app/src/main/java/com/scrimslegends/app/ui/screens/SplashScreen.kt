package com.scrimslegends.app.ui.screens

import androidx.compose.material3.MaterialTheme
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scrimslegends.app.R
import androidx.compose.ui.res.stringResource
import com.scrimslegends.app.ui.theme.*
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun SplashScreen(
    onFinish: () -> Unit,
    delayMillis: Long = 1500
) {
    var startAnimation by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(100)
        startAnimation = true
        delay(delayMillis)
        if (currentCoroutineContext().isActive) {
            onFinish()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                    )
                )
            )
    ) {
        // Animated ambient orbs in background
        AmbientOrbs()

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo with ring animation
            Box(
                modifier = Modifier.size(120.dp),
                contentAlignment = Alignment.Center
            ) {
                // Outer pulsing ring
                AnimatedRing(
                    visible = startAnimation,
                    delayMillis = 150,
                    size = 120.dp,
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                )
                AnimatedRing(
                    visible = startAnimation,
                    delayMillis = 250,
                    size = 100.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                )

                // Center glow
                val glowAlpha by animateFloatAsState(
                    targetValue = if (startAnimation) 1f else 0f,
                    animationSpec = tween(400, delayMillis = 150),
                    label = "glowAlpha"
                )

                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f * glowAlpha),
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f * glowAlpha),
                                    Color.Transparent
                                )
                            ),
                            shape = CircleShape
                        )
                )

                // ML text logo
                val logoScale by animateFloatAsState(
                    targetValue = if (startAnimation) 1f else 0.6f,
                    animationSpec = tween(350),
                    label = "logoScale"
                )

                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(id = com.scrimslegends.app.R.drawable.logo),
                    contentDescription = stringResource(R.string.content_desc_app_logo),
                    modifier = Modifier.size(80.dp).scale(logoScale)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // App name
            val titleAlpha by animateFloatAsState(
                targetValue = if (startAnimation) 1f else 0f,
                animationSpec = tween(300, delayMillis = 300),
                label = "titleAlpha"
            )

            Text(
                text = stringResource(R.string.app_title),
                modifier = Modifier.alpha(titleAlpha),
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                letterSpacing = 1.5.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Tagline
            val taglineAlpha by animateFloatAsState(
                targetValue = if (startAnimation) 1f else 0f,
                animationSpec = tween(300, delayMillis = 400),
                label = "taglineAlpha"
            )

            Text(
                text = stringResource(R.string.tagline),
                modifier = Modifier.alpha(taglineAlpha),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                letterSpacing = 2.sp
            )
        }

        // Bottom loading bar
        val progressAlpha by animateFloatAsState(
            targetValue = if (startAnimation) 1f else 0f,
            animationSpec = tween(250, delayMillis = 500),
            label = "progressAlpha"
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 64.dp)
                .alpha(progressAlpha),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LoadingBar(durationMillis = 2000)

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.loading),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun AnimatedRing(
    visible: Boolean,
    delayMillis: Int,
    size: androidx.compose.ui.unit.Dp,
    color: androidx.compose.ui.graphics.Color
) {
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.5f,
        animationSpec = tween(400, delayMillis = delayMillis),
        label = "ringScale"
    )

    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(400, delayMillis = delayMillis),
        label = "ringAlpha"
    )

    Box(
        modifier = Modifier
            .size(size)
            .scale(scale)
            .alpha(alpha)
            .background(color, CircleShape)
    )
}

@Composable
private fun LoadingBar(durationMillis: Int = 2000) {
    val infiniteTransition = rememberInfiniteTransition(label = "loading")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "loadingProgress"
    )

    Box(
        modifier = Modifier
            .width(120.dp)
            .height(3.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, androidx.compose.foundation.shape.RoundedCornerShape(2.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                    ),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(2.dp)
                )
        )
    }
}

@Composable
private fun AmbientOrbs() {
    val infiniteTransition = rememberInfiniteTransition(label = "orbs")

    // Multiple floating orbs for ambient background
    val orbs = listOf(
        OrbConfig(0.15f, 0.25f, MaterialTheme.colorScheme.secondary.copy(alpha = 0.04f), 8000, 0),
        OrbConfig(0.85f, 0.15f, MaterialTheme.colorScheme.primary.copy(alpha = 0.03f), 9000, 1),
        OrbConfig(0.5f, 0.75f, Purple.copy(alpha = 0.03f), 7000, 2)
    )

    Box(modifier = Modifier.fillMaxSize()) {
        orbs.forEach { orb ->
            val offsetX by infiniteTransition.animateFloat(
                initialValue = -20f,
                targetValue = 20f,
                animationSpec = infiniteRepeatable(
                    animation = tween(orb.duration, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "orbX${orb.index}"
            )

            val offsetY by infiniteTransition.animateFloat(
                initialValue = -15f,
                targetValue = 15f,
                animationSpec = infiniteRepeatable(
                    animation = tween(orb.duration + 500, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "orbY${orb.index}"
            )

            Box(
                modifier = Modifier
                    .fillMaxSize(orb.sizeFraction)
                    .offset(x = offsetX.dp, y = offsetY.dp)
                    .background(orb.color, CircleShape)
                    .align(
                        when (orb.index) {
                            0 -> Alignment.TopStart
                            1 -> Alignment.TopEnd
                            else -> Alignment.BottomCenter
                        }
                    )
            )
        }
    }
}

private data class OrbConfig(
    val sizeFraction: Float,
    val yFraction: Float,
    val color: androidx.compose.ui.graphics.Color,
    val duration: Int,
    val index: Int
)

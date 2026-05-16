package com.mlbb.scrim.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mlbb.scrim.ui.theme.*
import kotlin.math.roundToInt

/**
 * A luxury slider-based CAPTCHA component that prevents automated bot registrations.
 * Features smooth animations, glassmorphic styling, and haptic-like visual feedback.
 */
@Composable
fun PremiumCaptcha(
    onVerified: (Boolean) -> Unit
) {
    var isVerified by remember { mutableStateOf(false) }
    var offsetX by remember { mutableStateOf(0f) }
    val maxOffset = with(LocalDensity.current) { 240.dp.toPx() } // Approximate slider length
    
    val animatedOffsetX by animateFloatAsState(
        targetValue = if (isVerified) maxOffset else offsetX,
        animationSpec = if (isVerified) spring(dampingRatio = 0.8f, stiffness = 400f) else spring(),
        label = "SliderAnimation"
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
        ) {
            Icon(
                Icons.Default.Shield,
                contentDescription = null,
                tint = if (isVerified) GoldPrimary else TextSecondary,
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = if (isVerified) "Identity Verified" else "Security Check",
                style = iOSCaption1.copy(
                    color = if (isVerified) GoldPrimary else TextSecondary,
                    fontWeight = FontWeight.Medium
                )
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(SurfaceOverlay)
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(GlassBorder.copy(alpha = 0.5f), Color.Transparent)
                    ),
                    shape = RoundedCornerShape(28.dp)
                ),
            contentAlignment = Alignment.CenterStart
        ) {
            // Track Text
            Text(
                text = "Slide to verify",
                style = iOSBody.copy(
                    color = TextTertiary,
                    fontSize = 14.sp,
                    letterSpacing = 0.5.sp
                ),
                modifier = Modifier
                    .align(Alignment.Center)
                    .alpha(if (isVerified) 0f else (1f - (offsetX / maxOffset)).coerceIn(0f, 1f))
            )

            // Success background glow
            if (isVerified) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(BluePrimary.copy(alpha = 0.2f), Color.Transparent)
                            )
                        )
                )
            }

            // The Slider Handle
            Box(
                modifier = Modifier
                    .offset { IntOffset(animatedOffsetX.roundToInt(), 0) }
                    .padding(4.dp)
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        brush = if (isVerified) {
                            Brush.linearGradient(colors = listOf(GoldPrimary, Color(0xFFD4AF37)))
                        } else {
                            Brush.linearGradient(colors = listOf(BluePrimary, Color(0xFF0D47A1)))
                        }
                    )
                    .shadow(
                        elevation = 8.dp,
                        shape = CircleShape,
                        ambientColor = if (isVerified) GoldPrimary else BluePrimary
                    )
                    .pointerInput(isVerified) {
                        if (!isVerified) {
                            detectDragGestures(
                                onDragEnd = {
                                    if (offsetX >= maxOffset * 0.9f) {
                                        isVerified = true
                                        onVerified(true)
                                    } else {
                                        offsetX = 0f
                                    }
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    val newX = (offsetX + dragAmount.x).coerceIn(0f, maxOffset)
                                    offsetX = newX
                                }
                            )
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isVerified) Icons.Default.Check else Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = if (isVerified) DarkNavy else White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun Modifier.alpha(alpha: Float): Modifier = this.drawBehind {
    drawRect(Color.Transparent, alpha = alpha)
}

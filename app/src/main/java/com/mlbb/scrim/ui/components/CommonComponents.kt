package com.mlbb.scrim.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mlbb.scrim.ui.theme.*

// ============================================
// Gradient Button (Primary Action)
// ============================================

@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    gradient: List<Color> = GoldGradient,
    contentColor: Color = DarkBlue,
    height: androidx.compose.ui.unit.Dp = 52.dp
) {
    val brush = Brush.horizontalGradient(colors = gradient)
    val alpha by animateFloatAsState(
        targetValue = if (enabled) 1f else 0.5f,
        label = "buttonAlpha"
    )

    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(height),
        enabled = enabled && !isLoading,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = contentColor,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = LightGray
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
        contentPadding = PaddingValues(0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(brush = brush, shape = RoundedCornerShape(12.dp))
                .graphicsLayer { this.alpha = alpha },
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = contentColor,
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = text,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

// ============================================
// Ghost Button (Secondary Action)
// ============================================

@Composable
fun GhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    borderColor: Color = GoldPrimary,
    contentColor: Color = GoldPrimary
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        enabled = enabled,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = contentColor
        ),
        border = ButtonDefaults.outlinedButtonBorder.copy(
            brush = SolidColor(borderColor)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = text,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.5.sp
        )
    }
}

// ============================================
// Glow Card with Optional Border
// ============================================

@Composable
fun GlowCard(
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(16.dp),
    glowColor: Color = BlueGlow,
    borderColor: Color? = null,
    content: @Composable () -> Unit
) {
    val density = androidx.compose.ui.platform.LocalDensity.current
    val elevation = 8.dp

    Card(
        modifier = modifier
            .drawWithContent {
                drawContent()
                if (borderColor != null) {
                    drawIntoCanvas { canvas ->
                        val paint = android.graphics.Paint().apply {
                            isAntiAlias = true
                            style = android.graphics.Paint.Style.STROKE
                            strokeWidth = with(density) { 1.5.dp.toPx() }
                            color = borderColor.toArgb()
                            maskFilter = android.graphics.BlurMaskFilter(
                                with(density) { 2.dp.toPx() },
                                android.graphics.BlurMaskFilter.Blur.NORMAL
                            )
                        }
                        val rect = androidx.compose.ui.geometry.Rect(
                            0f, 0f, size.width, size.height
                        )
                        val roundedRect = android.graphics.RectF(
                            rect.left, rect.top, rect.right, rect.bottom
                        )
                        val corners = with(density) { 16.dp.toPx() }
                        canvas.nativeCanvas.drawRoundRect(roundedRect, corners, corners, paint)
                    }
                }
            }
            .graphicsLayer {
                shadowElevation = with(density) { elevation.toPx() }
                spotShadowColor = glowColor
                ambientShadowColor = glowColor.copy(alpha = 0.3f)
            },
        colors = CardDefaults.cardColors(containerColor = DarkNavy),
        shape = shape
    ) {
        content()
    }
}

// ============================================
// Animated Entrance for List Items
// ============================================

@Composable
fun AnimatedEntrance(
    modifier: Modifier = Modifier,
    delayMillis: Int = 0,
    content: @Composable () -> Unit
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(delayMillis.toLong())
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(
            animationSpec = tween(durationMillis = 400, delayMillis = 0)
        ) + slideInVertically(
            animationSpec = tween(durationMillis = 400, easing = AppEaseOutCubic),
            initialOffsetY = { it / 5 }
        ),
        exit = fadeOut() + slideOutVertically(),
        modifier = modifier
    ) {
        content()
    }
}

// ============================================
// Tier Badge Component
// ============================================

@Composable
fun TierBadge(
    tierName: String,
    modifier: Modifier = Modifier
) {
    val (gradient, textColor) = when (tierName.lowercase()) {
        "bronze" -> listOf(Color(0xFFCD7F32), Color(0xFF8B4513)) to Color.White
        "silver" -> listOf(Color(0xFFC0C0C0), Color(0xFF808080)) to Color.White
        "gold" -> listOf(Color(0xFFFFD700), Color(0xFFFFA500)) to DarkBlue
        "platinum" -> listOf(Color(0xFFE5E4E2), Color(0xFFB0B0B0)) to DarkBlue
        "diamond" -> listOf(Color(0xFFB9F2FF), Color(0xFF00BFFF)) to DarkBlue
        "master" -> listOf(Color(0xFFFF00FF), Color(0xFF8B008B)) to Color.White
        "grandmaster" -> listOf(Color(0xFFFFD700), Color(0xFFFF0000)) to Color.White
        else -> listOf(Color(0xFF7C4DFF), Color(0xFF4A148C)) to Color.White
    }

    val brush = Brush.horizontalGradient(colors = gradient)

    Box(
        modifier = modifier
            .background(brush = brush, shape = RoundedCornerShape(9999.dp))
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = tierName.uppercase(),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
            letterSpacing = 0.8.sp
        )
    }
}

// ============================================
// Enhanced Info Chip with Icon
// ============================================

@Composable
fun EnhancedInfoChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    tint: Color = LightGray,
    backgroundColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = backgroundColor
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                fontSize = 13.sp,
                color = tint,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// ============================================
// Hero Section Background
// ============================================

@Composable
fun HeroBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = HeroGradient
                )
            )
    ) {
        content()
    }
}

// ============================================
// Status Badge (Enhanced)
// ============================================

@Composable
fun EnhancedStatusBadge(
    text: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    val pulseAnim = rememberInfiniteTransition(label = "pulse")
    val scale by pulseAnim.animateFloat(
        initialValue = 1f,
        targetValue = if (text.equals("open", ignoreCase = true)) 1.05f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                if (text.equals("open", ignoreCase = true)) {
                    scaleX = scale
                    scaleY = scale
                }
            }
            .background(
                color = color.copy(alpha = 0.15f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
    }
}

// ============================================
// Section Header
// ============================================

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = White
            )
        )
        action?.invoke()
    }
}

// ============================================
// Empty State Illustration
// ============================================

@Composable
fun EmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    action: @Composable (() -> Unit)? = null
) {
    AnimatedEntrance {
        Column(
            modifier = modifier
                .fillMaxSize()
            .padding(40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                modifier = Modifier.size(100.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = LightGray.copy(alpha = 0.6f),
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = title,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = White
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = subtitle,
                fontSize = 15.sp,
                color = LightGray,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            if (action != null) {
                Spacer(modifier = Modifier.height(32.dp))
                action()
            }
        }
    }
}

// ============================================
// Back Button with Glass Effect
// ============================================

@Composable
fun GlassBackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(44.dp)
            .background(
                color = Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        Icon(
            imageVector = Icons.Default.ArrowBack,
            contentDescription = "Back",
            tint = White,
            modifier = Modifier.size(22.dp)
        )
    }
}

// ============================================
// Floating Action Button with Gradient
// ============================================

@Composable
fun GradientFAB(
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    gradient: List<Color> = BlueGradient
) {
    val brush = Brush.linearGradient(colors = gradient)

    FloatingActionButton(
        onClick = onClick,
        modifier = modifier.size(56.dp),
        shape = RoundedCornerShape(16.dp),
        containerColor = Color.Transparent,
        elevation = FloatingActionButtonDefaults.elevation(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(brush = brush),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

// ============================================
// Shimmer Loading Skeletons
// ============================================

@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(8.dp)
) {
    val shimmerColors = listOf(
        MidGray.copy(alpha = 0.15f),
        MidGray.copy(alpha = 0.3f),
        MidGray.copy(alpha = 0.15f)
    )

    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnim - 200f, translateAnim - 200f),
        end = Offset(translateAnim, translateAnim)
    )

    Box(
        modifier = modifier
            .clip(shape)
            .background(brush)
    )
}

@Composable
fun ScrimListSkeleton(
    modifier: Modifier = Modifier,
    itemCount: Int = 5
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        repeat(itemCount) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                colors = CardDefaults.cardColors(containerColor = DarkNavy),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ShimmerBox(
                        modifier = Modifier.size(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        ShimmerBox(
                            modifier = Modifier
                                .width(140.dp)
                                .height(18.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        ShimmerBox(
                            modifier = Modifier
                                .width(200.dp)
                                .height(14.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row {
                            ShimmerBox(
                                modifier = Modifier
                                    .width(60.dp)
                                    .height(12.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            ShimmerBox(
                                modifier = Modifier
                                    .width(80.dp)
                                    .height(12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TeamListSkeleton(
    modifier: Modifier = Modifier,
    itemCount: Int = 4
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        repeat(itemCount) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp),
                colors = CardDefaults.cardColors(containerColor = DarkNavy),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ShimmerBox(
                        modifier = Modifier.size(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        ShimmerBox(
                            modifier = Modifier
                                .width(120.dp)
                                .height(18.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        ShimmerBox(
                            modifier = Modifier
                                .width(100.dp)
                                .height(14.dp)
                        )
                    }
                    ShimmerBox(
                        modifier = Modifier
                            .width(56.dp)
                            .height(28.dp),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun NotificationListSkeleton(
    modifier: Modifier = Modifier,
    itemCount: Int = 6
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        repeat(itemCount) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(76.dp),
                colors = CardDefaults.cardColors(containerColor = DarkNavy),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ShimmerBox(
                        modifier = Modifier.size(40.dp),
                        shape = RoundedCornerShape(8.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        ShimmerBox(
                            modifier = Modifier
                                .width(180.dp)
                                .height(16.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        ShimmerBox(
                            modifier = Modifier
                                .width(120.dp)
                                .height(12.dp)
                        )
                    }
                    ShimmerBox(
                        modifier = Modifier
                            .width(40.dp)
                            .height(12.dp)
                    )
                }
            }
        }
    }
}

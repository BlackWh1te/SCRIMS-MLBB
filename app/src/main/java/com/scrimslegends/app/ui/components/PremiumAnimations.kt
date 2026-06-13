package com.scrimslegends.app.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.unit.dp
import com.scrimslegends.app.ui.theme.*

// ============================================
// PREMIUM ENTRANCE ANIMATIONS
// ============================================
// Sophisticated entrance animations with staggered delays and easing

@Composable
fun PremiumFadeIn(
    modifier: Modifier = Modifier,
    delayMillis: Int = 0,
    durationMillis: Int = 300,
    content: @Composable AnimatedVisibilityScope.() -> Unit
) {
    AnimatedVisibility(
        visible = true,
        modifier = modifier,
        enter = fadeIn(
            animationSpec = tween(
                durationMillis = durationMillis,
                delayMillis = delayMillis,
                easing = AppEaseOutCubic
            )
        ) + expandVertically(
            animationSpec = tween(
                durationMillis = durationMillis,
                delayMillis = delayMillis,
                easing = AppEaseOutCubic
            ),
            expandFrom = Alignment.Top
        ),
        content = content
    )
}

@Composable
fun PremiumSlideIn(
    modifier: Modifier = Modifier,
    delayMillis: Int = 0,
    content: @Composable AnimatedVisibilityScope.() -> Unit
) {
    AnimatedVisibility(
        visible = true,
        modifier = modifier,
        enter = slideInVertically(
            animationSpec = tween(
                durationMillis = 300,
                delayMillis = delayMillis,
                easing = AppEaseOutCubic
            ),
            initialOffsetY = { it / 3 }
        ) + fadeIn(
            animationSpec = tween(
                durationMillis = 250,
                delayMillis = delayMillis,
                easing = AppEaseOutCubic
            )
        ),
        content = content
    )
}

@Composable
fun PremiumScaleIn(
    modifier: Modifier = Modifier,
    delayMillis: Int = 0,
    content: @Composable AnimatedVisibilityScope.() -> Unit
) {
    AnimatedVisibility(
        visible = true,
        modifier = modifier,
        enter = scaleIn(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            ),
            initialScale = 0.85f
        ) + fadeIn(
            animationSpec = tween(
                durationMillis = 300,
                delayMillis = delayMillis,
                easing = AppEaseOutCubic
            )
        ),
        content = content
    )
}

// ============================================
// PREMIUM MICRO-INTERACTIONS
// ============================================
// Subtle interactions for premium feel

@Composable
fun rememberPremiumPressState(): PressState {
    var isPressed by remember { mutableStateOf(false) }
    val pressProgress by animateFloatAsState(
        targetValue = if (isPressed) 1f else 0f,
        animationSpec = tween(150),
        label = "pressProgress"
    )
    
    return remember(pressProgress) {
        PressState(
            isPressed = isPressed,
            pressProgress = pressProgress,
            setPressed = { isPressed = it }
        )
    }
}

data class PressState(
    val isPressed: Boolean,
    val pressProgress: Float,
    val setPressed: (Boolean) -> Unit
)

fun Modifier.premiumPress(pressState: PressState): Modifier = pointerInput(Unit) {
    detectTapGestures(
        onPress = {
            pressState.setPressed(true)
            tryAwaitRelease()
            pressState.setPressed(false)
        }
    )
}

// ============================================
// PREMIUM RIPPLE EFFECT
// ============================================
// Custom ripple effect with gradient colors

@Composable
fun Modifier.premiumRipple(
    color: Color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f),
    radius: Float = 100f
): Modifier = this.then(
    PremiumRippleElement(color, radius)
)

private class PremiumRippleElement(
    private val color: Color,
    private val radius: Float
) : ModifierNodeElement<PremiumRippleNode>() {
    override fun create(): PremiumRippleNode = PremiumRippleNode(color, radius)
    
    override fun update(node: PremiumRippleNode) {
        node.color = color
        node.radius = radius
    }
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PremiumRippleElement) return false
        return color == other.color && radius == other.radius
    }
    
    override fun hashCode(): Int {
        var result = color.hashCode()
        result = 31 * result + radius.hashCode()
        return result
    }
}

private class PremiumRippleNode(
    var color: Color,
    var radius: Float
) : DrawModifierNode, Modifier.Node() {
    override fun ContentDrawScope.draw() {
        drawContent()
        // Simplified ripple effect - just draw a subtle overlay
        drawCircle(
            color = color,
            radius = radius,
            alpha = 0.3f,
            blendMode = BlendMode.SrcOver
        )
    }
}

// ============================================
// PREMIUM SHIMMER EFFECT
// ============================================
// Sophisticated loading shimmer with gradient animation

@Composable
fun Modifier.premiumShimmer(
    isShimmering: Boolean = true,
    baseColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    highlightColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
): Modifier {
    if (!isShimmering) return this
    
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerOffset"
    )
    
    return this.drawWithContent {
        drawContent()
        val width = size.width
        val startX = -width + (width * 2) * ((shimmerOffset + 1f) / 2f)
        // Simplified shimmer effect
        drawRect(
            brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                colors = listOf(
                    baseColor,
                    highlightColor,
                    baseColor
                ),
                startX = startX,
                endX = startX + width * 2
            ),
            blendMode = BlendMode.SrcOver
        )
    }
}

// ============================================
// PREMIUM PULSE EFFECT
// ============================================
// Subtle pulsing animation for emphasis

@Composable
fun Modifier.premiumPulse(
    isPulsing: Boolean = true,
    pulseColor: Color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f),
    pulseRadius: Float = 20f
): Modifier {
    if (!isPulsing) return this
    
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = AppEaseOutCubic),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseScale"
    )
    
    return this.drawWithContent {
        drawContent()
        
        drawCircle(
            color = pulseColor,
            radius = pulseRadius * pulseScale,
            alpha = 0.5f
        )
    }
}

// ============================================
// PREMIUM GLOW EFFECT
// ============================================
// Subtle glow for emphasis and depth

@Composable
fun Modifier.premiumGlow(
    color: Color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
    radius: Float = 16f
): Modifier = this.drawWithContent {
    drawContent()
    
    drawRoundRect(
        color = color,
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = radius / 2f)
    )
}

// ============================================
// PREMIUM PARALLAX EFFECT
// ============================================
// Subtle parallax scrolling effect (simplified version)

@Composable
fun rememberPremiumParallaxState(): ParallaxState {
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    
    return remember {
        ParallaxState(
            offsetX = offsetX,
            offsetY = offsetY,
            setOffsetX = { offsetX = it },
            setOffsetY = { offsetY = it }
        )
    }
}

data class ParallaxState(
    val offsetX: Float,
    val offsetY: Float,
    val setOffsetX: (Float) -> Unit,
    val setOffsetY: (Float) -> Unit
)

@Suppress("UNUSED_PARAMETER")
fun Modifier.premiumParallax(
    parallaxState: ParallaxState,
    factor: Float = 0.5f
): Modifier = this // Simplified - parallax effect requires more complex implementation

// ============================================
// PREMIUM STAGGERED LIST ANIMATION
// ============================================
// Staggered entrance for list items

@Composable
fun <T> PremiumStaggeredList(
    items: List<T>,
    modifier: Modifier = Modifier,
    itemDelay: Int = 50,
    itemContent: @Composable (T, Int) -> Unit
) {
    androidx.compose.foundation.layout.Column(modifier = modifier) {
        items.forEachIndexed { index, item ->
            androidx.compose.runtime.key(item) {
                PremiumFadeIn(
                    delayMillis = index * itemDelay,
                    durationMillis = 300
                ) {
                    itemContent(item, index)
                }
            }
        }
    }
}

// ============================================
// PREMIUM SPRING ANIMATION
// ============================================
// Bouncy spring animation for interactions

@Composable
fun rememberPremiumSpringAnimation(
    targetValue: Float
): Float {
    return animateFloatAsState(
        targetValue = targetValue,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "springAnimation"
    ).value
}

// ============================================
// PREMIUM ROTATION ANIMATION
// ============================================
// Smooth rotation for icons and elements

@Composable
fun Modifier.premiumRotation(
    targetRotation: Float,
    durationMillis: Int = 300
): Modifier {
    val rotation by animateFloatAsState(
        targetValue = targetRotation,
        animationSpec = tween(
            durationMillis = durationMillis,
            easing = AppEaseInOutCubic
        ),
        label = "rotation"
    )
    
    return this.rotate(rotation)
}

// ============================================
// PREMIUM CROSSFADE ANIMATION
// ============================================
// Smooth crossfade between content

@Composable
fun <T> PremiumCrossfade(
    targetState: T,
    modifier: Modifier = Modifier,
    content: @Composable (T) -> Unit
) {
    AnimatedContent(
        targetState = targetState,
        modifier = modifier,
        transitionSpec = {
            fadeIn(
                animationSpec = tween(300, easing = AppEaseOutCubic)
            ) togetherWith fadeOut(
                animationSpec = tween(300, easing = AppEaseInCubic)
            )
        }
    ) { state ->
        content(state)
    }
}

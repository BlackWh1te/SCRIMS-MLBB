package com.scrimslegends.app.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Premium shimmer loading effect for skeleton screens.
 * Creates a smooth animated gradient sweep for loading placeholders.
 */
@Composable
fun ShimmerBrush(
    showShimmer: Boolean = true,
    targetValue: Float = 1000f
): Brush {
    return if (showShimmer) {
        val shimmerColors = listOf(
            MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
        )

        val transition = rememberInfiniteTransition(label = "shimmer")
        val translateAnimation = transition.animateFloat(
            initialValue = 0f,
            targetValue = targetValue,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "shimmer_translate"
        )

        Brush.linearGradient(
            colors = shimmerColors,
            start = Offset.Zero,
            end = Offset(x = translateAnimation.value, y = translateAnimation.value)
        )
    } else {
        Brush.linearGradient(
            colors = listOf(Color.Transparent, Color.Transparent),
            start = Offset.Zero,
            end = Offset.Zero
        )
    }
}

@Composable
fun ShimmerItem(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(12.dp)
) {
    Spacer(
        modifier = modifier
            .clip(shape)
            .background(ShimmerBrush())
    )
}

@Composable
fun ShimmerCard(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(ShimmerBrush())
            .padding(20.dp)
    ) {
        ShimmerItem(
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .height(20.dp)
                .padding(bottom = 12.dp),
            shape = RoundedCornerShape(8.dp)
        )
        ShimmerItem(
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp)
                .padding(bottom = 8.dp),
            shape = RoundedCornerShape(6.dp)
        )
        ShimmerItem(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(14.dp),
            shape = RoundedCornerShape(6.dp)
        )
    }
}

@Composable
fun ShimmerList(
    itemCount: Int = 5,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        repeat(itemCount) { index ->
            ShimmerItem(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}

@Composable
fun ShimmerCircle(
    size: androidx.compose.ui.unit.Dp = 48.dp,
    modifier: Modifier = Modifier
) {
    ShimmerItem(
        modifier = modifier.size(size),
        shape = RoundedCornerShape(percent = 50)
    )
}

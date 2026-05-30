package com.scrimslegends.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.scrimslegends.app.ui.theme.GoldPrimary
import com.scrimslegends.app.ui.theme.MidGray

@Composable
fun StarRatingBar(
    rating: Int,
    onRatingChanged: (Int) -> Unit,
    modifier: Modifier = Modifier,
    starSize: Int = 36,
    maxStars: Int = 5
) {
    Row(modifier = modifier) {
        repeat(maxStars) { index ->
            val starValue = index + 1
            val isFilled = starValue <= rating
            val animatedScale by animateFloatAsState(
                targetValue = if (isFilled) 1.15f else 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                label = "starScale"
            )
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = "$starValue stars",
                tint = if (isFilled) GoldPrimary else MidGray.copy(alpha = 0.3f),
                modifier = Modifier
                    .size(starSize.dp)
                    .clickable { onRatingChanged(starValue) }
                    .padding(horizontal = 2.dp)
            )
        }
    }
}

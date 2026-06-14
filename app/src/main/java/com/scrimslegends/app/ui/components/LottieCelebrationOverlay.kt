package com.scrimslegends.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieComposition
import com.scrimslegends.app.R
import kotlinx.coroutines.delay

@Composable
fun LottieCelebrationOverlay(
    isVisible: Boolean,
    onAnimationFinished: () -> Unit
) {
    if (!isVisible) return

    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.celebration_animation))
    
    LaunchedEffect(isVisible) {
        if (isVisible) {
            delay(3000) // Assumes animation is ~3 seconds long
            onAnimationFinished()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f)),
        contentAlignment = Alignment.Center
    ) {
        LottieAnimation(
            composition = composition,
            modifier = Modifier.fillMaxSize()
        )
    }
}

package com.mlbb.scrim.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColors
import androidx.compose.material3.darkColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// MLBB Color Palette
val GoldPrimary = Color(0xFFFFD700)
val BluePrimary = Color(0xFF1E90FF)
val DarkBlue = Color(0xFF0A1628)
val DarkNavy = Color(0xFF0D1B2A)

val White = Color(0xFFFFFFFF)
val LightGray = Color(0xFFE0E0E0)
val SuccessGreen = Color(0xFF00C853)
val ErrorRed = Color(0xFFFF3D00)
val WarningOrange = Color(0xFFFF9100)

val Purple = Color(0xFF7C4DFF)
val Cyan = Color(0xFF00E5FF)
val Pink = Color(0xFFFF4081)

private val DarkColorScheme = darkColors(
    primary = GoldPrimary,
    secondary = BluePrimary,
    tertiary = Purple,
    background = DarkBlue,
    surface = DarkNavy,
    onPrimary = DarkBlue,
    onSecondary = White,
    onTertiary = White,
    onBackground = White,
    onSurface = LightGray
)

@Composable
fun MLBBScrimHostTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
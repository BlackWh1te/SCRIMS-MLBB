package com.mlbb.scrim.ui.theme

import androidx.compose.animation.core.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// ============================================
// MLBB Scrim Host - Epic Gaming Color System
// ============================================

// Primary Colors (MLBB Official)
val GoldPrimary = Color(0xFFFFD700)
val BluePrimary = Color(0xFF1E90FF)
val DarkBlue = Color(0xFF0A1628)
val DarkNavy = Color(0xFF0D1B2A)
val DarkSurface = Color(0xFF111E2E)

// Secondary Colors
val White = Color(0xFFFFFFFF)
val LightGray = Color(0xFFE0E0E0)
val MidGray = Color(0xFF8A96A8)
val SuccessGreen = Color(0xFF00C853)
val ErrorRed = Color(0xFFFF3D00)
val WarningOrange = Color(0xFFFF9100)

// Accent Colors
val Purple = Color(0xFF7C4DFF)
val Cyan = Color(0xFF00E5FF)
val Pink = Color(0xFFFF4081)

// Tier System Colors
val Bronze = Color(0xFFCD7F32)
val Silver = Color(0xFFC0C0C0)
val Gold = Color(0xFFFFD700)
val Platinum = Color(0xFFE5E4E2)
val Diamond = Color(0xFFB9F2FF)
val Master = Color(0xFFFF00FF)
val Grandmaster = Color(0xFFFFD700)

// Glow & Effect Colors
val GoldGlow = Color(0x66FFD700)
val BlueGlow = Color(0x661E90FF)
val PurpleGlow = Color(0x667C4DFF)
val GreenGlow = Color(0x6600C853)

// Gradient definitions
val GoldGradient = listOf(Color(0xFFFFD700), Color(0xFFFFA500))
val BlueGradient = listOf(Color(0xFF1E90FF), Color(0xFF0A5A9F))
val PurpleGradient = listOf(Color(0xFF7C4DFF), Color(0xFF4A148C))
val HeroGradient = listOf(Color(0xFF0A1628), Color(0xFF1E3A5F), Color(0xFF0D1B2A))
val SuccessGradient = listOf(Color(0xFF00C853), Color(0xFF009624))

private val LightColorScheme = lightColorScheme(
    primary = BluePrimary,
    onPrimary = White,
    primaryContainer = BluePrimary.copy(alpha = 0.15f),
    onPrimaryContainer = BluePrimary,
    secondary = GoldPrimary,
    onSecondary = DarkBlue,
    secondaryContainer = GoldPrimary.copy(alpha = 0.15f),
    onSecondaryContainer = GoldPrimary,
    tertiary = Purple,
    onTertiary = White,
    tertiaryContainer = Purple.copy(alpha = 0.15f),
    onTertiaryContainer = Purple,
    background = Color(0xFFF0F2F5),
    onBackground = Color(0xFF1A1A1A),
    surface = White,
    onSurface = Color(0xFF1A1A1A),
    surfaceVariant = Color(0xFFE8ECF1),
    onSurfaceVariant = Color(0xFF5A6575),
    error = ErrorRed,
    onError = White,
    outline = Color(0xFFCAD1D9),
    outlineVariant = Color(0xFFE2E8F0),
    scrim = Color(0xFF000000).copy(alpha = 0.6f)
)

private val DarkColorScheme = darkColorScheme(
    primary = BluePrimary,
    onPrimary = White,
    primaryContainer = BluePrimary.copy(alpha = 0.15f),
    onPrimaryContainer = BluePrimary,
    secondary = GoldPrimary,
    onSecondary = DarkBlue,
    secondaryContainer = GoldPrimary.copy(alpha = 0.15f),
    onSecondaryContainer = GoldPrimary,
    tertiary = Purple,
    onTertiary = White,
    tertiaryContainer = Purple.copy(alpha = 0.15f),
    onTertiaryContainer = Purple,
    background = DarkBlue,
    onBackground = White,
    surface = DarkNavy,
    onSurface = White,
    surfaceVariant = DarkSurface,
    onSurfaceVariant = MidGray,
    error = ErrorRed,
    onError = White,
    outline = Color(0xFF2A3A4D),
    outlineVariant = Color(0xFF1E2D3D),
    scrim = Color(0xFF000000).copy(alpha = 0.7f)
)

// Animation Specs
val AppEaseOutCubic = CubicBezierEasing(0.33f, 1f, 0.68f, 1f)
val AppEaseInOutCubic = CubicBezierEasing(0.65f, 0f, 0.35f, 1f)

// Reusable gradient brush for gold elements
fun goldGradientBrush(): Brush = Brush.horizontalGradient(colors = GoldGradient)
fun blueGradientBrush(): Brush = Brush.horizontalGradient(colors = BlueGradient)
fun heroGradientBrush(): Brush = Brush.verticalGradient(colors = HeroGradient)

val AppShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)
)

@Composable
fun MLBBScrimHostTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}

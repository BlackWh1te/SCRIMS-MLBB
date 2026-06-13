package com.scrimslegends.app.ui.theme

import androidx.compose.animation.core.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ============================================
// Scrims Legends — Android Premium Design System
// Optimized for vertical phone screens
// ============================================

val LightColorScheme = lightColorScheme(
    primary              = LightPrimary,
    onPrimary            = White,
    primaryContainer     = LightPrimary.copy(alpha = 0.12f),
    onPrimaryContainer   = LightPrimary,
    secondary            = LightSecondary,
    onSecondary          = White,
    secondaryContainer   = LightSecondary.copy(alpha = 0.12f),
    onSecondaryContainer = LightSecondary,
    tertiary             = LightTertiary,
    onTertiary           = White,
    tertiaryContainer    = LightTertiary.copy(alpha = 0.12f),
    onTertiaryContainer  = LightTertiary,
    background           = LightBackground,
    onBackground         = LightTextPrimary,
    surface              = LightSurface,
    onSurface            = LightTextPrimary,
    surfaceVariant       = LightSurfaceVariant,
    onSurfaceVariant     = LightTextSecondary,
    error                = ErrorRed,
    onError              = White,
    outline              = LightBorder,
    outlineVariant       = LightBorderVariant,
    scrim                = Color(0xFF000000).copy(alpha = 0.45f)
)

val DarkColorScheme = darkColorScheme(
    primary              = BluePrimary,
    onPrimary            = White,
    primaryContainer     = BluePrimary.copy(alpha = 0.15f),
    onPrimaryContainer   = BluePrimary,
    secondary            = BluePrimary,
    onSecondary          = DarkBlue,
    secondaryContainer   = BluePrimary.copy(alpha = 0.15f),
    onSecondaryContainer = BluePrimary,
    tertiary             = PurplePrimary,
    onTertiary           = White,
    tertiaryContainer    = PurplePrimary.copy(alpha = 0.15f),
    onTertiaryContainer  = PurplePrimary,
    background           = DarkNavy,
    onBackground         = TextPrimary,
    surface              = SurfaceElevated,
    onSurface            = TextPrimary,
    surfaceVariant       = SurfaceOverlay,
    onSurfaceVariant     = TextSecondary,
    error                = ErrorRed,
    onError              = White,
    outline              = Separator,
    outlineVariant       = Color(0xFF0E1A2D),
    scrim                = Color(0xFF000000).copy(alpha = 0.60f)
)

// ============================================
// Animation Easing Curves
// ============================================
val AppEaseOutCubic    = CubicBezierEasing(0.33f, 1f, 0.68f, 1f)
val AppEaseInOutCubic  = CubicBezierEasing(0.65f, 0f, 0.35f, 1f)
val AppEaseOutQuart    = CubicBezierEasing(0.25f, 1f, 0.5f, 1f)
val AppEaseInCubic     = CubicBezierEasing(0.32f, 0f, 0.67f, 0f)
val AppEaseSpring      = androidx.compose.animation.core.SpringSpec<Float>(
    dampingRatio = Spring.DampingRatioMediumBouncy,
    stiffness    = Spring.StiffnessMediumLow
)

// ============================================
// Theme Entry Point
// ============================================
@Composable
fun ScrimsLegendsTheme(
    darkTheme: Boolean = false, // We will later change this to read from user preferences
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = Typography,
        shapes      = AppShapes,
        content     = content
    )
}

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
    primaryContainer     = Color(0xFFDDEBFF),
    onPrimaryContainer   = Color(0xFF0A3D6E),
    secondary            = LightSecondary,
    onSecondary          = White,
    secondaryContainer   = Color(0xFFD8E6FB),
    onSecondaryContainer = Color(0xFF0B335E),
    tertiary             = LightTertiary,
    onTertiary           = White,
    tertiaryContainer    = Color(0xFFE9E0FB),
    onTertiaryContainer  = Color(0xFF301B5C),
    background           = LightBackground,
    onBackground         = LightTextPrimary,
    surface              = LightSurface,
    onSurface            = LightTextPrimary,
    surfaceVariant       = LightSurfaceVariant,
    onSurfaceVariant     = LightTextSecondary,
    // Tonal surface ladder (used by cards, sheets, menus, nav)
    surfaceContainerLowest  = Color(0xFFFFFFFF),
    surfaceContainerLow     = Color(0xFFF8FAFD),
    surfaceContainer        = Color(0xFFF2F5FA),
    surfaceContainerHigh    = Color(0xFFEBEFF6),
    surfaceContainerHighest = Color(0xFFE4EAF3),
    surfaceTint          = LightPrimary,
    inverseSurface       = Color(0xFF1A2233),
    inverseOnSurface     = Color(0xFFF0F4FF),
    inversePrimary       = Color(0xFF9CC8FF),
    error                = ErrorRed,
    onError              = White,
    errorContainer       = Color(0xFFFFE0D6),
    onErrorContainer     = Color(0xFF6E1500),
    outline              = LightBorder,
    outlineVariant       = LightBorderVariant,
    scrim                = Color(0xFF000000).copy(alpha = 0.45f)
)

val DarkColorScheme = darkColorScheme(
    primary              = BluePrimary,
    onPrimary            = White,
    primaryContainer     = Color(0xFF12365C),
    onPrimaryContainer   = Color(0xFFBFDDFF),
    secondary            = BluePrimary,
    onSecondary          = DarkBlue,
    secondaryContainer   = Color(0xFF12365C),
    onSecondaryContainer = Color(0xFFBFDDFF),
    tertiary             = PurplePrimary,
    onTertiary           = White,
    tertiaryContainer    = Color(0xFF2E2154),
    onTertiaryContainer  = Color(0xFFD8CCFF),
    background           = DarkNavy,
    onBackground         = TextPrimary,
    surface              = SurfaceElevated,
    onSurface            = TextPrimary,
    surfaceVariant       = SurfaceOverlay,
    onSurfaceVariant     = TextSecondary,
    // Tonal surface ladder — clearer elevation separation on the dark navy stack
    surfaceContainerLowest  = Color(0xFF080F1C),
    surfaceContainerLow     = Color(0xFF0D1828),
    surfaceContainer        = Color(0xFF111E32),
    surfaceContainerHigh    = Color(0xFF16243B),
    surfaceContainerHighest = Color(0xFF1C2C46),
    surfaceTint          = BluePrimary,
    inverseSurface       = Color(0xFFE4EAF3),
    inverseOnSurface     = Color(0xFF0E1A2D),
    inversePrimary       = Color(0xFF0A4D8C),
    error                = ErrorRed,
    onError              = White,
    errorContainer       = Color(0xFF5A1700),
    onErrorContainer     = Color(0xFFFFDAD0),
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

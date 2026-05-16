package com.voicechanger.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = Color(0xFF007AFF),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE3F0FF),
    onPrimaryContainer = Color(0xFF00224D),
    secondary = Color(0xFF5856D6),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEAE9FF),
    onSecondaryContainer = Color(0xFF0F0F59),
    tertiary = Color(0xFFFF2D55),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE5E9),
    onTertiaryContainer = Color(0xFF660014),
    error = Color(0xFFFF3B30),
    onError = Color.White,
    errorContainer = Color(0xFFFFE5E3),
    onErrorContainer = Color(0xFF660500),
    background = Color(0xFFF2F2F7),
    onBackground = Color(0xFF1C1C1E),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1C1C1E),
    surfaceVariant = Color(0xFFF2F2F7),
    onSurfaceVariant = Color(0xFF8E8E93),
    outline = Color(0xFFE5E5EA),
    inverseOnSurface = Color(0xFFF2F2F7),
    inverseSurface = Color(0xFF1C1C1E),
    inversePrimary = Color(0xFF5CA8FF),
    surfaceTint = Color(0xFF007AFF),
    outlineVariant = Color(0xFFD1D1D6),
    scrim = Color(0xFF000000)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF0A84FF),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF0A2F5C),
    onPrimaryContainer = Color(0xFFBBDCFF),
    secondary = Color(0xFF5E5CE6),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF1F1E5C),
    onSecondaryContainer = Color(0xFFD0CFFF),
    tertiary = Color(0xFFFF375F),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF5C0015),
    onTertiaryContainer = Color(0xFFFFC2CC),
    error = Color(0xFFFF453A),
    onError = Color.White,
    errorContainer = Color(0xFF5C0A05),
    onErrorContainer = Color(0xFFFFC2BF),
    background = Color(0xFF000000),
    onBackground = Color(0xFFFFFFFF),
    surface = Color(0xFF1C1C1E),
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFF2C2C2E),
    onSurfaceVariant = Color(0xFF8E8E93),
    outline = Color(0xFF38383A),
    inverseOnSurface = Color(0xFF1C1C1E),
    inverseSurface = Color(0xFFF2F2F7),
    inversePrimary = Color(0xFF0A84FF),
    surfaceTint = Color(0xFF0A84FF),
    outlineVariant = Color(0xFF38383A),
    scrim = Color(0xFF000000)
)

@Composable
fun VoiceChangerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = IosTypography,
        content = content
    )
}

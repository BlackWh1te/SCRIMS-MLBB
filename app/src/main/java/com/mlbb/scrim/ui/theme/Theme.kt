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
// Scrims Legends — Android Premium Design System
// Optimized for vertical phone screens
// ============================================

// ── Brand Core ──────────────────────────────
val GoldPrimary    = Color(0xFFFFBB00)   // Richer amber-gold
val BluePrimary    = Color(0xFF2196F3)   // Vivid Android blue
val PurplePrimary  = Color(0xFF7C4DFF)   // Electric purple

// ── Dark Surface Stack ───────────────────────
val DarkBlue       = Color(0xFF080F1C)   // Deepest background
val DarkNavy       = Color(0xFF0B1525)   // App background
val DarkSurface    = Color(0xFF0E1A2D)   // Card base
val SurfaceBase    = Color(0xFF0B1525)
val SurfaceElevated= Color(0xFF101D30)
val SurfaceOverlay = Color(0xFF162036)
val SurfaceCard    = Color(0xFF0F1B2E)
val SurfaceGlass   = Color(0xFF142038).copy(alpha = 0.80f)

// ── Semantic Colors ──────────────────────────
val SuccessGreen   = Color(0xFF00C853)
val ErrorRed       = Color(0xFFFF3D00)
val WarningOrange  = Color(0xFFFF9100)
val InfoBlue       = Color(0xFF29B6F6)

// ── Text Colors ─────────────────────────────
val White          = Color(0xFFFFFFFF)
val TextPrimary    = Color(0xFFF0F4FF)
val TextSecondary  = Color(0xFF8DA0BC)
val TextTertiary   = Color(0xFF4E6080)
val LightGray      = Color(0xFFCDD6E8)
val MidGray        = Color(0xFF7D90A8)
val DimGray        = Color(0xFF4A5C72)

// ── Separator / Border ───────────────────────
val Separator      = Color(0xFF1E2F45)
val SeparatorLight = Color(0xFF283D56)
val GlassBorder    = Color(0xFFFFFFFF).copy(alpha = 0.10f)
val GlassLight     = Color(0xFFFFFFFF).copy(alpha = 0.06f)
val GlassHighlight = Color(0xFFFFFFFF).copy(alpha = 0.04f)
val GlassShadow    = Color(0xFF000000).copy(alpha = 0.20f)

// ── Android System Colors ────────────────────
val AndroidBlue    = Color(0xFF2196F3)
val AndroidGreen   = Color(0xFF4CAF50)
val AndroidOrange  = Color(0xFFFF9800)
val AndroidRed     = Color(0xFFF44336)
val AndroidTeal    = Color(0xFF00BCD4)
val AndroidPurple  = Color(0xFF9C27B0)

// ── Legacy iOS aliases (kept for compatibility) ──
val iOSBlue        = Color(0xFF2196F3)
val iOSGreen       = Color(0xFF4CAF50)
val iOSOrange      = Color(0xFFFF9800)
val iOSRed         = Color(0xFFF44336)
val iOSYellow      = Color(0xFFFFCC00)
val iOSPurple      = Color(0xFF9C27B0)
val iOSPink        = Color(0xFFE91E63)
val iOSTeal        = Color(0xFF00BCD4)
val iOSIndigo      = Color(0xFF5C6BC0)
val Purple         = PurplePrimary
val Cyan           = Color(0xFF00E5FF)
val Pink           = Color(0xFFFF4081)

// ── Shadow Colors ────────────────────────────
val ShadowLight    = Color(0xFF000000).copy(alpha = 0.10f)
val ShadowMedium   = Color(0xFF000000).copy(alpha = 0.18f)
val ShadowHeavy    = Color(0xFF000000).copy(alpha = 0.28f)

// ── Glow Colors ──────────────────────────────
val GoldGlow       = Color(0x50FFBB00)
val BlueGlow       = Color(0x402196F3)
val PurpleGlow     = Color(0x407C4DFF)
val GreenGlow      = Color(0x3400C853)

// ── Rank / Tier System ───────────────────────
val Bronze         = Color(0xFFCD7F32)
val Silver         = Color(0xFFC0C0C0)
val Gold           = Color(0xFFFFD700)
val Platinum       = Color(0xFFE5E4E2)
val Diamond        = Color(0xFFB9F2FF)
val Master         = Color(0xFFFF00FF)
val Grandmaster    = Color(0xFFFFD700)
val SolverBlue     = Color(0xFF4A90D9)
val GoldRank       = Color(0xFFFFB800)
val GrandmasterPurple = Color(0xFF9B59B6)
val EpicCyan       = Color(0xFF00CED1)
val LegendRed      = Color(0xFFFF4757)
val MythicCrimson  = Color(0xFFFF1B1B)

// ── Top Badge Colors ─────────────────────────
val Top1Gold       = Color(0xFFFFD700)
val Top2Silver     = Color(0xFFC0C0C0)
val Top3Bronze     = Color(0xFFCD7F32)
val CrownGlow      = Color(0x40FFD700)

// ============================================
// Gradient Definitions
// ============================================

val GoldGradient = listOf(Color(0xFFFFBB00), Color(0xFFFF9500), Color(0xFFFF6B00))
val BlueGradient = listOf(Color(0xFF2196F3), Color(0xFF1565C0), Color(0xFF0D47A1))
val PurpleGradient = listOf(Color(0xFF7C4DFF), Color(0xFF5E35B1), Color(0xFF4527A0))

val HeroGradient = listOf(
    Color(0xFF080F1C),
    Color(0xFF0B1525),
    Color(0xFF0E1A2D),
    Color(0xFF121F35)
)

val CardGradient = listOf(Color(0xFF0F1B2E), Color(0xFF0D1826), Color(0xFF0B1623))
val CardGradientHover = listOf(Color(0xFF162036), Color(0xFF131D2E), Color(0xFF111B2C))

val SurfaceGradientElevated = listOf(Color(0xFF101D30), Color(0xFF0D1A2A))
val SurfaceGradientGlass    = listOf(Color(0xFF162036).copy(alpha = 0.88f), Color(0xFF102030).copy(alpha = 0.88f))

// Status gradients
val SuccessGradient = listOf(Color(0xFF00C853), Color(0xFF00E676), Color(0xFF009624))
val WarningGradient = listOf(Color(0xFFFF9100), Color(0xFFFFAB40), Color(0xFFFF6D00))
val ErrorGradient   = listOf(Color(0xFFFF3D00), Color(0xFFFF5252), Color(0xFFD50000))

// Rank gradients
val BronzeGradient      = listOf(Color(0xFFCD7F32), Color(0xFFB87333), Color(0xFF8B4513))
val SilverGradient      = listOf(Color(0xFFE8E8E8), Color(0xFFC0C0C0), Color(0xFFA0A0A0))
val GoldRankGradient    = listOf(Color(0xFFFFBB00), Color(0xFFFFC107), Color(0xFFFF9800))
val PlatinumGradient    = listOf(Color(0xFFE5E4E2), Color(0xFFD4D4D4), Color(0xFFB8B8B8))
val DiamondGradient     = listOf(Color(0xFFB9F2FF), Color(0xFF80DEEA), Color(0xFF4DD0E1))
val MasterGradient      = listOf(Color(0xFFE040FB), Color(0xFFAA00FF), Color(0xFF7C4DFF))
val GrandmasterGradient = listOf(Color(0xFFFFBB00), Color(0xFFFF1744), Color(0xFFD50000))

// Premium accent gradients
val PremiumBlueGradient   = listOf(Color(0xFF2196F3), Color(0xFF1565C0), Color(0xFF0D47A1))
val PremiumGreenGradient  = listOf(Color(0xFF4CAF50), Color(0xFF2E7D32), Color(0xFF1B5E20))
val PremiumOrangeGradient = listOf(Color(0xFFFF9800), Color(0xFFFF6D00), Color(0xFFE65100))

// Glow gradient lists
val GoldGlowGradient   = listOf(GoldPrimary.copy(alpha = 0.35f), GoldPrimary.copy(alpha = 0.15f), GoldPrimary.copy(alpha = 0.0f))
val BlueGlowGradient   = listOf(BluePrimary.copy(alpha = 0.30f), BluePrimary.copy(alpha = 0.12f), BluePrimary.copy(alpha = 0.0f))
val PurpleGlowGradient = listOf(Purple.copy(alpha = 0.30f), Purple.copy(alpha = 0.12f), Purple.copy(alpha = 0.0f))

// ============================================
// Gradient Brush Factories
// ============================================
fun goldGradientBrush()    : Brush = Brush.horizontalGradient(colors = GoldGradient)
fun blueGradientBrush()    : Brush = Brush.horizontalGradient(colors = BlueGradient)
fun purpleGradientBrush()  : Brush = Brush.horizontalGradient(colors = PurpleGradient)
val HeroGradientBrush: Brush = Brush.verticalGradient(colors = HeroGradient)
fun heroGradientBrush()    : Brush = HeroGradientBrush
fun cardGradientBrush()    : Brush = Brush.verticalGradient(colors = CardGradient)
fun successGradientBrush() : Brush = Brush.horizontalGradient(colors = SuccessGradient)
fun warningGradientBrush() : Brush = Brush.horizontalGradient(colors = WarningGradient)
fun errorGradientBrush()   : Brush = Brush.horizontalGradient(colors = ErrorGradient)

fun bronzeGradientBrush()      : Brush = Brush.horizontalGradient(colors = BronzeGradient)
fun silverGradientBrush()      : Brush = Brush.horizontalGradient(colors = SilverGradient)
fun goldRankGradientBrush()    : Brush = Brush.horizontalGradient(colors = GoldRankGradient)
fun platinumGradientBrush()    : Brush = Brush.horizontalGradient(colors = PlatinumGradient)
fun diamondGradientBrush()     : Brush = Brush.horizontalGradient(colors = DiamondGradient)
fun masterGradientBrush()      : Brush = Brush.horizontalGradient(colors = MasterGradient)
fun grandmasterGradientBrush() : Brush = Brush.horizontalGradient(colors = GrandmasterGradient)
fun premiumBlueGradientBrush() : Brush = Brush.horizontalGradient(colors = PremiumBlueGradient)
fun premiumGreenGradientBrush(): Brush = Brush.horizontalGradient(colors = PremiumGreenGradient)
fun premiumOrangeGradientBrush():Brush = Brush.horizontalGradient(colors = PremiumOrangeGradient)

fun goldGlowBrush()   : Brush = Brush.radialGradient(colors = GoldGlowGradient)
fun blueGlowBrush()   : Brush = Brush.radialGradient(colors = BlueGlowGradient)
fun purpleGlowBrush() : Brush = Brush.radialGradient(colors = PurpleGlowGradient)

// ============================================
// Material Color Schemes
// ============================================

private val LightColorScheme = lightColorScheme(
    primary              = BluePrimary,
    onPrimary            = White,
    primaryContainer     = BluePrimary.copy(alpha = 0.12f),
    onPrimaryContainer   = BluePrimary,
    secondary            = GoldPrimary,
    onSecondary          = DarkBlue,
    secondaryContainer   = GoldPrimary.copy(alpha = 0.12f),
    onSecondaryContainer = GoldPrimary,
    tertiary             = Purple,
    onTertiary           = White,
    tertiaryContainer    = Purple.copy(alpha = 0.12f),
    onTertiaryContainer  = Purple,
    background           = Color(0xFFF2F4F8),
    onBackground         = Color(0xFF1A1C2E),
    surface              = Color(0xFFFFFFFF),
    onSurface            = Color(0xFF1A1C2E),
    surfaceVariant       = Color(0xFFE3E8F0),
    onSurfaceVariant     = Color(0xFF6B7A96),
    error                = ErrorRed,
    onError              = White,
    outline              = Color(0xFFC0CBDB),
    outlineVariant       = Color(0xFFEDF0F5),
    scrim                = Color(0xFF000000).copy(alpha = 0.45f)
)

private val DarkColorScheme = darkColorScheme(
    primary              = BluePrimary,
    onPrimary            = White,
    primaryContainer     = BluePrimary.copy(alpha = 0.15f),
    onPrimaryContainer   = BluePrimary,
    secondary            = GoldPrimary,
    onSecondary          = DarkBlue,
    secondaryContainer   = GoldPrimary.copy(alpha = 0.15f),
    onSecondaryContainer = GoldPrimary,
    tertiary             = Purple,
    onTertiary           = White,
    tertiaryContainer    = Purple.copy(alpha = 0.15f),
    onTertiaryContainer  = Purple,
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
// Shape System — Android-style rounded corners
// ============================================

val AppShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
    small      = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
    medium     = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
    large      = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(28.dp)
)

// Semantic shape tokens
val iOSButtonShape  = androidx.compose.foundation.shape.RoundedCornerShape(14.dp)
val iOSCardShape    = androidx.compose.foundation.shape.RoundedCornerShape(18.dp)
val iOSSheetShape   = androidx.compose.foundation.shape.RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 0.dp, bottomEnd = 0.dp)
val iOSChipShape    = androidx.compose.foundation.shape.RoundedCornerShape(9999.dp)
val iOSInputShape   = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)

// ============================================
// Theme Entry Point
// ============================================

@Composable
fun ScrimsLegendsTheme(
    darkTheme: Boolean = true,
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

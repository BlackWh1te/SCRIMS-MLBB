@file:OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)

package com.scrimslegends.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.scrimslegends.app.R

// ============================================
// Scrims Legends typography system
// Optimized for Android vertical screens
// ============================================
// Brand fonts:
//   Display  -> Rajdhani  (futuristic, semi-condensed) — titles, headers, tier badges
//   Stats    -> Teko      (tall, condensed)            — XP, ranks, scores, counts
//   Body     -> system sans-serif (Roboto)             — clean, highly readable body text

// Rajdhani ships as discrete static weights.
val DisplayFontFamily = FontFamily(
    Font(R.font.rajdhani_regular,  FontWeight.Normal),
    Font(R.font.rajdhani_medium,   FontWeight.Medium),
    Font(R.font.rajdhani_semibold, FontWeight.SemiBold),
    Font(R.font.rajdhani_bold,     FontWeight.Bold)
)

// Body stays on the platform sans-serif (Roboto) for maximum legibility at small sizes.
val BodyFontFamily = FontFamily.SansSerif

// Teko ships as a single variable font; map logical weights via weight variation
// settings (applied on API 26+, gracefully falls back to the default instance below).
val StatsFontFamily = FontFamily(
    Font(R.font.teko_variable, FontWeight.Normal,   variationSettings = FontVariation.Settings(FontVariation.weight(400))),
    Font(R.font.teko_variable, FontWeight.Medium,   variationSettings = FontVariation.Settings(FontVariation.weight(500))),
    Font(R.font.teko_variable, FontWeight.SemiBold, variationSettings = FontVariation.Settings(FontVariation.weight(600))),
    Font(R.font.teko_variable, FontWeight.Bold,     variationSettings = FontVariation.Settings(FontVariation.weight(700)))
)

// ============================================
// Material3 Typography Scale
// ============================================

val Typography = Typography(
    // Display — Hero titles, splash
    displayLarge = TextStyle(
        fontFamily   = DisplayFontFamily,
        fontWeight   = FontWeight.Bold,
        fontSize     = 36.sp,
        lineHeight   = 44.sp,
        letterSpacing = (-0.5).sp
    ),
    displayMedium = TextStyle(
        fontFamily   = DisplayFontFamily,
        fontWeight   = FontWeight.Bold,
        fontSize     = 30.sp,
        lineHeight   = 38.sp,
        letterSpacing = (-0.3).sp
    ),
    displaySmall = TextStyle(
        fontFamily   = DisplayFontFamily,
        fontWeight   = FontWeight.Bold,
        fontSize     = 24.sp,
        lineHeight   = 32.sp,
        letterSpacing = (-0.2).sp
    ),

    // Headline — Screen titles, section headers
    headlineLarge = TextStyle(
        fontFamily   = DisplayFontFamily,
        fontWeight   = FontWeight.Bold,
        fontSize     = 22.sp,
        lineHeight   = 28.sp,
        letterSpacing = (-0.2).sp
    ),
    headlineMedium = TextStyle(
        fontFamily   = BodyFontFamily,
        fontWeight   = FontWeight.SemiBold,
        fontSize     = 18.sp,
        lineHeight   = 24.sp,
        letterSpacing = (-0.1).sp
    ),
    headlineSmall = TextStyle(
        fontFamily   = BodyFontFamily,
        fontWeight   = FontWeight.SemiBold,
        fontSize     = 16.sp,
        lineHeight   = 22.sp,
        letterSpacing = 0.sp
    ),

    // Title — Card titles, nav bar
    titleLarge = TextStyle(
        fontFamily   = BodyFontFamily,
        fontWeight   = FontWeight.Bold,
        fontSize     = 17.sp,
        lineHeight   = 22.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily   = BodyFontFamily,
        fontWeight   = FontWeight.SemiBold,
        fontSize     = 15.sp,
        lineHeight   = 20.sp,
        letterSpacing = 0.sp
    ),
    titleSmall = TextStyle(
        fontFamily   = BodyFontFamily,
        fontWeight   = FontWeight.SemiBold,
        fontSize     = 13.sp,
        lineHeight   = 18.sp,
        letterSpacing = 0.sp
    ),

    // Body — Reading text
    bodyLarge = TextStyle(
        fontFamily   = BodyFontFamily,
        fontWeight   = FontWeight.Normal,
        fontSize     = 16.sp,
        lineHeight   = 24.sp,
        letterSpacing = 0.sp
    ),
    bodyMedium = TextStyle(
        fontFamily   = BodyFontFamily,
        fontWeight   = FontWeight.Normal,
        fontSize     = 14.sp,
        lineHeight   = 20.sp,
        letterSpacing = 0.sp
    ),
    bodySmall = TextStyle(
        fontFamily   = BodyFontFamily,
        fontWeight   = FontWeight.Normal,
        fontSize     = 12.sp,
        lineHeight   = 16.sp,
        letterSpacing = 0.1.sp
    ),

    // Label — Chips, tags, captions
    labelLarge = TextStyle(
        fontFamily   = BodyFontFamily,
        fontWeight   = FontWeight.Medium,
        fontSize     = 13.sp,
        lineHeight   = 16.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily   = BodyFontFamily,
        fontWeight   = FontWeight.Medium,
        fontSize     = 11.sp,
        lineHeight   = 14.sp,
        letterSpacing = 0.3.sp
    ),
    labelSmall = TextStyle(
        fontFamily   = BodyFontFamily,
        fontWeight   = FontWeight.Medium,
        fontSize     = 10.sp,
        lineHeight   = 12.sp,
        letterSpacing = 0.5.sp
    )
)

// ============================================
// Extended App Typography (semantic aliases)
// ============================================

// iOS-compatible aliases (used across screens)
val iOSTitle1 = TextStyle(
    fontFamily   = DisplayFontFamily,
    fontWeight   = FontWeight.Bold,
    fontSize     = 28.sp,
    lineHeight   = 34.sp,
    letterSpacing = (-0.5).sp
)
val iOSTitle2 = TextStyle(
    fontFamily   = DisplayFontFamily,
    fontWeight   = FontWeight.Bold,
    fontSize     = 22.sp,
    lineHeight   = 28.sp,
    letterSpacing = (-0.3).sp
)
val iOSTitle3 = TextStyle(
    fontFamily   = DisplayFontFamily,
    fontWeight   = FontWeight.SemiBold,
    fontSize     = 18.sp,
    lineHeight   = 24.sp,
    letterSpacing = (-0.2).sp
)
val iOSHeadline = TextStyle(
    fontFamily   = BodyFontFamily,
    fontWeight   = FontWeight.SemiBold,
    fontSize     = 16.sp,
    lineHeight   = 22.sp,
    letterSpacing = 0.sp
)
val iOSBody = TextStyle(
    fontFamily   = BodyFontFamily,
    fontWeight   = FontWeight.Normal,
    fontSize     = 16.sp,
    lineHeight   = 24.sp,
    letterSpacing = 0.sp
)
val iOSCallout = TextStyle(
    fontFamily   = BodyFontFamily,
    fontWeight   = FontWeight.Normal,
    fontSize     = 15.sp,
    lineHeight   = 21.sp,
    letterSpacing = 0.sp
)
val iOSFootnote = TextStyle(
    fontFamily   = BodyFontFamily,
    fontWeight   = FontWeight.Normal,
    fontSize     = 13.sp,
    lineHeight   = 18.sp,
    letterSpacing = 0.sp
)
val iOSCaption1 = TextStyle(
    fontFamily   = BodyFontFamily,
    fontWeight   = FontWeight.Normal,
    fontSize     = 12.sp,
    lineHeight   = 16.sp,
    letterSpacing = 0.sp
)
val iOSCaption2 = TextStyle(
    fontFamily   = BodyFontFamily,
    fontWeight   = FontWeight.Normal,
    fontSize     = 11.sp,
    lineHeight   = 14.sp,
    letterSpacing = 0.2.sp
)

// ── Stats Display ─────────────────────────────
val StatsTextStyle = TextStyle(
    fontFamily   = StatsFontFamily,
    fontWeight   = FontWeight.Bold,
    fontSize     = 20.sp,
    lineHeight   = 24.sp,
    letterSpacing = 0.sp
)
val TierBadgeTextStyle = TextStyle(
    fontFamily   = DisplayFontFamily,
    fontWeight   = FontWeight.Bold,
    fontSize     = 11.sp,
    lineHeight   = 14.sp,
    letterSpacing = 0.5.sp
)

// ── Premium Display ───────────────────────────
val PremiumDisplayXL = TextStyle(
    fontFamily   = DisplayFontFamily,
    fontWeight   = FontWeight.Bold,
    fontSize     = 48.sp,
    lineHeight   = 56.sp,
    letterSpacing = (-1.0).sp
)
val PremiumDisplayL = TextStyle(
    fontFamily   = DisplayFontFamily,
    fontWeight   = FontWeight.Bold,
    fontSize     = 40.sp,
    lineHeight   = 48.sp,
    letterSpacing = (-0.8).sp
)
val PremiumDisplayM = TextStyle(
    fontFamily   = DisplayFontFamily,
    fontWeight   = FontWeight.Bold,
    fontSize     = 32.sp,
    lineHeight   = 40.sp,
    letterSpacing = (-0.5).sp
)

// ── Premium Stats ─────────────────────────────
val PremiumStatsXL = TextStyle(
    fontFamily   = StatsFontFamily,
    fontWeight   = FontWeight.Bold,
    fontSize     = 34.sp,
    lineHeight   = 40.sp,
    letterSpacing = 0.sp
)
val PremiumStatsL = TextStyle(
    fontFamily   = StatsFontFamily,
    fontWeight   = FontWeight.Bold,
    fontSize     = 28.sp,
    lineHeight   = 32.sp,
    letterSpacing = 0.sp
)
val PremiumStatsM = TextStyle(
    fontFamily   = StatsFontFamily,
    fontWeight   = FontWeight.Bold,
    fontSize     = 22.sp,
    lineHeight   = 28.sp,
    letterSpacing = 0.sp
)

// ── Premium Buttons ───────────────────────────
val PremiumButton = TextStyle(
    fontFamily   = BodyFontFamily,
    fontWeight   = FontWeight.SemiBold,
    fontSize     = 16.sp,
    lineHeight   = 20.sp,
    letterSpacing = 0.sp
)
val PremiumButtonSmall = TextStyle(
    fontFamily   = BodyFontFamily,
    fontWeight   = FontWeight.SemiBold,
    fontSize     = 14.sp,
    lineHeight   = 18.sp,
    letterSpacing = 0.sp
)

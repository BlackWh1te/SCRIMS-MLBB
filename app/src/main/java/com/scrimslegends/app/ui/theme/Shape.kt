package com.scrimslegends.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// ============================================
// Shape System — Android-style rounded corners
// ============================================

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(Dimens.radiusXSmall),
    small      = RoundedCornerShape(Dimens.radiusSmall),
    medium     = RoundedCornerShape(Dimens.radiusMedium),
    large      = RoundedCornerShape(Dimens.radiusLarge),
    extraLarge = RoundedCornerShape(Dimens.radiusXLarge)
)

// Semantic shape tokens
val iOSButtonShape  = RoundedCornerShape(Dimens.radiusLarge)
val iOSCardShape    = RoundedCornerShape(Dimens.radiusIOSCard)
val iOSSheetShape   = RoundedCornerShape(
    topStart = Dimens.radiusXLarge,
    topEnd = Dimens.radiusXLarge,
    bottomStart = 0.dp,
    bottomEnd = 0.dp
)
val iOSChipShape    = RoundedCornerShape(Dimens.radiusPill)
val iOSInputShape   = RoundedCornerShape(Dimens.radiusMedium)

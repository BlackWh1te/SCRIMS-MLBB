package com.scrimslegends.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// ============================================
// Shape System — Android-style rounded corners
// ============================================

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small      = RoundedCornerShape(12.dp),
    medium     = RoundedCornerShape(20.dp),
    large      = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

// Semantic shape tokens
val iOSButtonShape  = RoundedCornerShape(16.dp)
val iOSCardShape    = RoundedCornerShape(22.dp)
val iOSSheetShape   = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 0.dp, bottomEnd = 0.dp)
val iOSChipShape    = RoundedCornerShape(9999.dp)
val iOSInputShape   = RoundedCornerShape(14.dp)

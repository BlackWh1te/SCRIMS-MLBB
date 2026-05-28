package com.mlbb.scrim.ui.components

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class AppWindowSizeClass {
    Compact,
    Medium,
    Expanded
}

@Stable
data class ResponsiveMetrics(
    val windowSizeClass: AppWindowSizeClass,
    val horizontalPadding: Dp,
    val contentMaxWidth: Dp,
    val cardSpacing: Dp,
    val bottomNavHorizontalPadding: Dp,
    val bottomNavBottomPadding: Dp,
    val bottomNavHeight: Dp,
    val bottomNavGlowHeight: Dp,
    val bottomNavCornerRadius: Dp,
    val bottomNavIconSize: Dp,
    val bottomNavLabelSize: TextUnit,
    val showBottomNavLabels: Boolean,
    val profileAvatarSize: Dp,
    val profileStatColumns: Int
) {
    val isCompact: Boolean
        get() = windowSizeClass == AppWindowSizeClass.Compact
}

@Composable
fun rememberResponsiveMetrics(): ResponsiveMetrics {
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
        .takeUnless { it == Configuration.SCREEN_WIDTH_DP_UNDEFINED }
        ?: 360

    return remember(screenWidthDp) {
        when {
            screenWidthDp < 360 -> ResponsiveMetrics(
                windowSizeClass = AppWindowSizeClass.Compact,
                horizontalPadding = 14.dp,
                contentMaxWidth = 520.dp,
                cardSpacing = 10.dp,
                bottomNavHorizontalPadding = 6.dp,
                bottomNavBottomPadding = 8.dp,
                bottomNavHeight = 60.dp,
                bottomNavGlowHeight = 68.dp,
                bottomNavCornerRadius = 22.dp,
                bottomNavIconSize = 23.dp,
                bottomNavLabelSize = 9.sp,
                showBottomNavLabels = false,
                profileAvatarSize = 96.dp,
                profileStatColumns = 2
            )
            screenWidthDp < 600 -> ResponsiveMetrics(
                windowSizeClass = AppWindowSizeClass.Medium,
                horizontalPadding = 20.dp,
                contentMaxWidth = 560.dp,
                cardSpacing = 12.dp,
                bottomNavHorizontalPadding = 8.dp,
                bottomNavBottomPadding = 10.dp,
                bottomNavHeight = 72.dp,
                bottomNavGlowHeight = 80.dp,
                bottomNavCornerRadius = 28.dp,
                bottomNavIconSize = 26.dp,
                bottomNavLabelSize = 10.sp,
                showBottomNavLabels = true,
                profileAvatarSize = 110.dp,
                profileStatColumns = 3
            )
            else -> ResponsiveMetrics(
                windowSizeClass = AppWindowSizeClass.Expanded,
                horizontalPadding = 28.dp,
                contentMaxWidth = 680.dp,
                cardSpacing = 14.dp,
                bottomNavHorizontalPadding = 24.dp,
                bottomNavBottomPadding = 12.dp,
                bottomNavHeight = 76.dp,
                bottomNavGlowHeight = 84.dp,
                bottomNavCornerRadius = 28.dp,
                bottomNavIconSize = 27.dp,
                bottomNavLabelSize = 11.sp,
                showBottomNavLabels = true,
                profileAvatarSize = 118.dp,
                profileStatColumns = 3
            )
        }
    }
}

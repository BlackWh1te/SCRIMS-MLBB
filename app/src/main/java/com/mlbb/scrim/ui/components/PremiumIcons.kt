package com.mlbb.scrim.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mlbb.scrim.ui.theme.*

// ============================================
// PREMIUM ICON SYSTEM
// ============================================
// Consistent icon styling with gradients, glows, and backgrounds

// ============================================
// PREMIUM ICON WITH GRADIENT BACKGROUND
// ============================================

@Composable
fun PremiumGradientIcon(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    gradient: List<Color> = GoldGradient,
    iconTint: Color = White
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .background(Brush.horizontalGradient(gradient)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(size * 0.5f)
        )
    }
}

// ============================================
// PREMIUM ICON WITH GLOW
// ============================================

@Composable
fun PremiumGlowIcon(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    iconColor: Color = GoldPrimary,
    glowColor: Color = GoldPrimary.copy(alpha = 0.3f),
    glowRadius: Float = 20f
) {
    Box(
        modifier = modifier
            .size(size)
            .premiumGlow(glowColor, glowRadius),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(size)
        )
    }
}

// ============================================
// PREMIUM CIRCLE ICON
// ============================================

@Composable
fun PremiumCircleIcon(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    backgroundColor: Color = SurfaceOverlay,
    iconColor: Color = GoldPrimary,
    borderColor: Color = GoldPrimary
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(backgroundColor)
            .premiumGlow(borderColor.copy(alpha = 0.2f), radius = size.value / 2),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(size * 0.5f)
        )
    }
}

// ============================================
// PREMIUM ICON BUTTON
// ============================================

@Composable
fun PremiumIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    backgroundColor: Color = SurfaceGlass,
    iconColor: Color = LightGray,
    accentColor: Color = GoldPrimary
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        backgroundColor,
                        Color.Transparent
                    )
                )
            )
            .premiumGlow(accentColor.copy(alpha = 0.1f), radius = size.value / 2)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(size * 0.5f)
        )
    }
}

// ============================================
// PREMIUM STATUS ICONS
// ============================================

@Composable
fun PremiumSuccessIcon(modifier: Modifier = Modifier, size: Dp = 24.dp) {
    PremiumGlowIcon(
        icon = Icons.Default.CheckCircle,
        modifier = modifier,
        size = size,
        iconColor = iOSGreen,
        glowColor = iOSGreen.copy(alpha = 0.3f),
        glowRadius = 15f
    )
}

@Composable
fun PremiumErrorIcon(modifier: Modifier = Modifier, size: Dp = 24.dp) {
    PremiumGlowIcon(
        icon = Icons.Default.Cancel,
        modifier = modifier,
        size = size,
        iconColor = iOSRed,
        glowColor = iOSRed.copy(alpha = 0.3f),
        glowRadius = 15f
    )
}

@Composable
fun PremiumWarningIcon(modifier: Modifier = Modifier, size: Dp = 24.dp) {
    PremiumGlowIcon(
        icon = Icons.Default.Warning,
        modifier = modifier,
        size = size,
        iconColor = iOSOrange,
        glowColor = iOSOrange.copy(alpha = 0.3f),
        glowRadius = 15f
    )
}

@Composable
fun PremiumInfoIcon(modifier: Modifier = Modifier, size: Dp = 24.dp) {
    PremiumGlowIcon(
        icon = Icons.Default.Info,
        modifier = modifier,
        size = size,
        iconColor = iOSBlue,
        glowColor = iOSBlue.copy(alpha = 0.3f),
        glowRadius = 15f
    )
}

// ============================================
// PREMIUM ACTION ICONS
// ============================================

@Composable
fun PremiumSearchIcon(modifier: Modifier = Modifier, size: Dp = 24.dp) {
    PremiumGlowIcon(
        icon = Icons.Default.Search,
        modifier = modifier,
        size = size,
        iconColor = BluePrimary,
        glowColor = BluePrimary.copy(alpha = 0.3f),
        glowRadius = 15f
    )
}

@Composable
fun PremiumAddIcon(modifier: Modifier = Modifier, size: Dp = 24.dp) {
    PremiumGlowIcon(
        icon = Icons.Default.Add,
        modifier = modifier,
        size = size,
        iconColor = GoldPrimary,
        glowColor = GoldPrimary.copy(alpha = 0.3f),
        glowRadius = 15f
    )
}

@Composable
fun PremiumFavoriteIcon(
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    isFavorite: Boolean = false
) {
    PremiumGlowIcon(
        icon = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
        modifier = modifier,
        size = size,
        iconColor = if (isFavorite) iOSRed else LightGray,
        glowColor = if (isFavorite) iOSRed.copy(alpha = 0.3f) else Color.Transparent,
        glowRadius = 15f
    )
}

@Composable
fun PremiumSettingsIcon(modifier: Modifier = Modifier, size: Dp = 24.dp) {
    PremiumGlowIcon(
        icon = Icons.Default.Settings,
        modifier = modifier,
        size = size,
        iconColor = MidGray,
        glowColor = MidGray.copy(alpha = 0.2f),
        glowRadius = 15f
    )
}

@Composable
fun PremiumNotificationIcon(
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    hasNotification: Boolean = false
) {
    PremiumGlowIcon(
        icon = Icons.Default.Notifications,
        modifier = modifier,
        size = size,
        iconColor = if (hasNotification) GoldPrimary else LightGray,
        glowColor = if (hasNotification) GoldPrimary.copy(alpha = 0.3f) else Color.Transparent,
        glowRadius = 15f
    )
}

// ============================================
// PREMIUM GAMING ICONS
// ============================================

@Composable
fun PremiumTrophyIcon(modifier: Modifier = Modifier, size: Dp = 24.dp) {
    PremiumGradientIcon(
        icon = Icons.Default.EmojiEvents,
        modifier = modifier,
        size = size,
        gradient = GoldGradient,
        iconTint = White
    )
}

@Composable
fun PremiumGroupIcon(modifier: Modifier = Modifier, size: Dp = 24.dp) {
    PremiumGlowIcon(
        icon = Icons.Default.Group,
        modifier = modifier,
        size = size,
        iconColor = Purple,
        glowColor = Purple.copy(alpha = 0.3f),
        glowRadius = 15f
    )
}

@Composable
fun PremiumPersonIcon(modifier: Modifier = Modifier, size: Dp = 24.dp) {
    PremiumGlowIcon(
        icon = Icons.Default.Person,
        modifier = modifier,
        size = size,
        iconColor = BluePrimary,
        glowColor = BluePrimary.copy(alpha = 0.3f),
        glowRadius = 15f
    )
}

@Composable
fun PremiumHistoryIcon(modifier: Modifier = Modifier, size: Dp = 24.dp) {
    PremiumGlowIcon(
        icon = Icons.Default.History,
        modifier = modifier,
        size = size,
        iconColor = iOSGreen,
        glowColor = iOSGreen.copy(alpha = 0.3f),
        glowRadius = 15f
    )
}

@Composable
fun PremiumCalendarIcon(modifier: Modifier = Modifier, size: Dp = 24.dp) {
    PremiumGlowIcon(
        icon = Icons.Default.CalendarMonth,
        modifier = modifier,
        size = size,
        iconColor = iOSOrange,
        glowColor = iOSOrange.copy(alpha = 0.3f),
        glowRadius = 15f
    )
}

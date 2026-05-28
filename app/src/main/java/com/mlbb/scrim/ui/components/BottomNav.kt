package com.mlbb.scrim.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.annotation.StringRes
import androidx.compose.ui.platform.LocalDensity
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.mlbb.scrim.R
import com.mlbb.scrim.ui.theme.*

// ============================================
// MLBB Scrim Host — Premium Bottom Navigation
// Glass-dock design with gold spotlight
// ============================================

sealed class BottomNavItem(
    val route: String,
    @StringRes val labelRes: Int,
    val icon: ImageVector,
    val selectedIcon: ImageVector
) {
    data object Home         : BottomNavItem("home",            R.string.nav_home,           Icons.Default.Home,            Icons.Default.Home)
    data object Scrims       : BottomNavItem("scrim_list",      R.string.nav_scrims,         Icons.Default.SportsEsports,   Icons.Default.SportsEsports)
    data object Tournaments  : BottomNavItem("tournament_list", R.string.nav_tournaments,    Icons.Default.EmojiEvents,     Icons.Default.EmojiEvents)
    data object PlayerFinder : BottomNavItem("player_finder",   R.string.nav_player_finder,  Icons.Default.PersonSearch,    Icons.Default.PersonSearch)
    data object Messages     : BottomNavItem("message_list",    R.string.nav_messages,       Icons.AutoMirrored.Filled.Chat, Icons.AutoMirrored.Filled.Chat)
    data object Profile      : BottomNavItem("profile",         R.string.nav_profile,        Icons.Default.PersonOutline,   Icons.Default.Person)
}

val bottomNavItems = listOf(
    BottomNavItem.Home,
    BottomNavItem.Scrims,
    BottomNavItem.Tournaments,
    BottomNavItem.PlayerFinder,
    BottomNavItem.Messages,
    BottomNavItem.Profile
)

@Composable
fun AppBottomNav(
    navController: NavHostController,
    unreadMessageCount: Int = 0,
    notificationCount: Int = 0,
    pendingInviteCount: Int = 0
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Only show bottom nav on main tabs
    if (currentRoute !in bottomNavItems.map { it.route }) return

    val responsive = rememberResponsiveMetrics()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(
                start = responsive.bottomNavHorizontalPadding,
                end = responsive.bottomNavHorizontalPadding,
                bottom = responsive.bottomNavBottomPadding
            )
    ) {
        // Main glass dock container
        DockContainer(responsive = responsive) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(responsive.bottomNavHeight)
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                bottomNavItems.forEach { item ->
                    val isSelected = currentRoute == item.route
                    val badgeCount = when (item) {
                        is BottomNavItem.Home     -> notificationCount
                        is BottomNavItem.Messages -> unreadMessageCount
                        is BottomNavItem.Profile  -> pendingInviteCount
                        else -> 0
                    }
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        DockItem(
                            item       = item,
                            isSelected = isSelected,
                            badgeCount = badgeCount,
                            responsive = responsive,
                            onClick    = {
                                if (!isSelected) {
                                    navController.navigate(item.route) {
                                        popUpTo(item.route) { inclusive = true }
                                        launchSingleTop = true
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DockContainer(
    responsive: ResponsiveMetrics,
    content: @Composable () -> Unit
) {
    // Outer glow blur layer - slightly larger than dock
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(responsive.bottomNavGlowHeight)
            .clip(RoundedCornerShape(responsive.bottomNavCornerRadius))
            .blur(24.dp)
            .alpha(0.6f)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        GoldPrimary.copy(alpha = 0.12f),
                        Color.Transparent,
                        Color.Transparent
                    )
                )
            )
    )

    // Main dock surface
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(responsive.bottomNavHeight)
            .clip(RoundedCornerShape(responsive.bottomNavCornerRadius))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        SurfaceGlass.copy(alpha = 0.72f), // 72% opacity per DESIGN.md
                        DarkNavy.copy(alpha = 0.85f)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        White.copy(alpha = 0.12f),
                        Color.Transparent
                    )
                ),
                shape = RoundedCornerShape(responsive.bottomNavCornerRadius)
            )
            .drawBehind {
                // Subtle top highlight line
                drawLine(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            GoldPrimary.copy(alpha = 0.15f),
                            GlassBorder.copy(alpha = 0.3f),
                            GoldPrimary.copy(alpha = 0.15f),
                            Color.Transparent
                        )
                    ),
                    start = Offset(0f, 0.5f),
                    end   = Offset(size.width, 0.5f),
                    strokeWidth = 1f
                )
            }
    ) {
        content()
    }
}

@Composable
private fun DockItem(
    item      : BottomNavItem,
    isSelected: Boolean,
    badgeCount: Int,
    responsive: ResponsiveMetrics,
    onClick   : () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue   = if (isSelected) {
            if (responsive.isCompact) 1.07f else 1.12f
        } else {
            1f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness    = Spring.StiffnessMediumLow
        ),
        label = "dockScale"
    )

    val iconColor by animateColorAsState(
        targetValue   = if (isSelected) GoldPrimary else TextTertiary,
        animationSpec = tween(250, easing = AppEaseOutCubic),
        label = "dockIconColor"
    )

    val labelColor by animateColorAsState(
        targetValue   = if (isSelected) GoldPrimary.copy(alpha = 0.95f) else TextTertiary.copy(alpha = 0.55f),
        animationSpec = tween(250, easing = AppEaseOutCubic),
        label = "dockLabelColor"
    )

    Box(
        modifier = Modifier
            .fillMaxHeight()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication        = null,
                onClick           = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        // Active background pill — gold-tinted per MLBB design
        // Uses AnimatedVisibility for smooth scale+fade instead of raw alpha
        AnimatedVisibility(
            visible = isSelected,
            enter   = scaleIn(animationSpec = tween(150, easing = AppEaseOutCubic), initialScale = 0.85f) +
                      fadeIn(animationSpec = tween(150, easing = AppEaseOutCubic)),
            exit    = scaleOut(animationSpec = tween(120, easing = FastOutLinearInEasing), targetScale = 0.85f) +
                      fadeOut(animationSpec = tween(120, easing = FastOutLinearInEasing))
        ) {
            Box(
                modifier = Modifier
                    .size(
                        width  = if (responsive.isCompact) 42.dp else 48.dp,
                        height = if (responsive.isCompact) 36.dp else 40.dp
                    )
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                GoldPrimary.copy(alpha = 0.12f),
                                GoldPrimary.copy(alpha = 0.04f)
                            )
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .drawBehind {
                        // Gold top border highlight
                        drawLine(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    GoldPrimary.copy(alpha = 0.5f),
                                    Color.Transparent
                                )
                            ),
                            start = Offset(0f, 0.5f),
                            end = Offset(size.width, 0.5f),
                            strokeWidth = 1.5f
                        )
                    }
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.scale(scale)
        ) {
            Box {
                Icon(
                    imageVector        = if (isSelected) item.selectedIcon else item.icon,
                    contentDescription = stringResource(item.labelRes),
                    tint               = iconColor,
                    modifier           = Modifier.size(responsive.bottomNavIconSize)
                )

                // Badge
                if (badgeCount > 0) {
                    Badge(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 6.dp, y = (-4).dp),
                        containerColor = ErrorRed,
                        contentColor   = White
                    ) {
                        Text(
                            text       = if (badgeCount > 99) "99+" else badgeCount.toString(),
                            fontSize   = 8.sp,
                            fontWeight = FontWeight.ExtraBold,
                            lineHeight = 8.sp
                        )
                    }
                }
            }

            if (responsive.showBottomNavLabels) {
                Spacer(Modifier.height(3.dp))
                Text(
                    text          = stringResource(item.labelRes),
                    color         = labelColor,
                    fontSize      = responsive.bottomNavLabelSize,
                    fontWeight    = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    letterSpacing = 0.sp,
                    maxLines      = 1,
                    overflow      = TextOverflow.Ellipsis
                )
            } else if (isSelected) {
                // Tiny gold dot indicator when labels are hidden (compact screens)
                Spacer(Modifier.height(3.dp))
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .background(GoldPrimary, CircleShape)
                )
            }
        }
    }
}

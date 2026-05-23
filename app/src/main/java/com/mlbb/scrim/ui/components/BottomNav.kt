package com.mlbb.scrim.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.annotation.StringRes
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.mlbb.scrim.R
import com.mlbb.scrim.ui.theme.*

sealed class BottomNavItem(
    val route: String,
    @StringRes val labelRes: Int,
    val icon: ImageVector,
    val selectedIcon: ImageVector
) {
    data object Home         : BottomNavItem("home",          R.string.nav_home,          Icons.Default.Home,            Icons.Default.Home)
    data object Scrims       : BottomNavItem("scrim_list",    R.string.nav_scrims,         Icons.Default.SportsEsports,   Icons.Default.SportsEsports)
    data object PlayerFinder : BottomNavItem("player_finder", R.string.nav_player_finder,  Icons.Default.PersonSearch,    Icons.Default.PersonSearch)
    data object Messages     : BottomNavItem("message_list",  R.string.nav_messages,       Icons.Default.ChatBubbleOutline,Icons.Default.Chat)
    data object Profile      : BottomNavItem("profile",       R.string.nav_profile,        Icons.Default.PersonOutline,   Icons.Default.Person)
}

val bottomNavItems = listOf(
    BottomNavItem.Home,
    BottomNavItem.Scrims,
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

    // Floating pill container
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        // Background blur surface
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            SurfaceElevated.copy(alpha = 0.96f),
                            DarkNavy.copy(alpha = 0.98f)
                        )
                    )
                )
                .drawBehind {
                    // Top edge highlight
                    drawLine(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                GlassBorder,
                                GlassBorder,
                                Color.Transparent
                            )
                        ),
                        start = Offset(0f, 0f),
                        end   = Offset(size.width, 0f),
                        strokeWidth = 1f
                    )
                }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                bottomNavItems.forEach { item ->
                    val isSelected = currentRoute == item.route
                    val badgeCount = when (item) {
                        is BottomNavItem.Home     -> notificationCount
                        is BottomNavItem.Messages -> unreadMessageCount
                        is BottomNavItem.Profile  -> pendingInviteCount
                        else -> 0
                    }
                    BottomNavItemButton(
                        item          = item,
                        isSelected    = isSelected,
                        badgeCount    = badgeCount,
                        onClick       = {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RowScope.BottomNavItemButton(
    item      : BottomNavItem,
    isSelected: Boolean,
    badgeCount: Int,
    onClick   : () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue    = if (isSelected) 1.10f else 1f,
        animationSpec  = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label          = "navScale"
    )
    val iconColor by animateColorAsState(
        targetValue   = if (isSelected) GoldPrimary else DimGray,
        animationSpec = tween(220),
        label         = "iconColor"
    )
    val bgAlpha by animateFloatAsState(
        targetValue   = if (isSelected) 1f else 0f,
        animationSpec = tween(220),
        label         = "bgAlpha"
    )

    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        // Active pill indicator
        if (isSelected) {
            Box(
                modifier = Modifier
                    .width(48.dp)
                    .height(44.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                GoldPrimary.copy(alpha = 0.18f),
                                GoldPrimary.copy(alpha = 0.08f)
                            )
                        )
                    )
            )
        }

        Box(
            modifier = Modifier
                .scale(scale)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box {
                    Icon(
                        imageVector     = if (isSelected) item.selectedIcon else item.icon,
                        contentDescription = stringResource(item.labelRes),
                        tint            = iconColor,
                        modifier        = Modifier.size(24.dp)
                    )
                    if (badgeCount > 0) {
                        Badge(
                            modifier       = Modifier.align(Alignment.TopEnd).offset(x = 8.dp, y = (-6).dp),
                            containerColor = ErrorRed,
                            contentColor   = White
                        ) {
                            Text(badgeCount.coerceAtMost(9).toString(), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Label — always visible but dim when inactive
                Spacer(Modifier.height(3.dp))
                Text(
                    text          = stringResource(item.labelRes),
                    color         = if (isSelected) GoldPrimary else DimGray,
                    fontSize      = 9.sp,
                    fontWeight    = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    letterSpacing = 0.sp,
                    maxLines      = 1
                )
            }
        }
    }
}
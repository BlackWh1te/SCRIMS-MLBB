package com.scrimslegends.app.ui.components

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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
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
import com.scrimslegends.app.R
import com.scrimslegends.app.ui.theme.*

// ============================================
// Scrims Legends — Premium Bottom Navigation v2
// Floating glass dock with animated gold spotlight
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
    val selectedIndex = bottomNavItems.indexOfFirst { it.route == currentRoute }.takeIf { it >= 0 } ?: 0
    val colors = MaterialTheme.colorScheme
    val navSurface = colors.surface
    val navTop = colors.surfaceVariant.copy(alpha = 0.82f)
    val navBorder = colors.outline.copy(alpha = 0.58f)

    // Animated offset for the neon line
    val indicatorOffset by animateFloatAsState(
        targetValue = selectedIndex.toFloat(),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "indicatorOffset"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
    ) {
        // Main glass bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (responsive.isCompact) 65.dp else 75.dp)
                .background(
                    color = navSurface
                )
                .drawBehind {
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                navTop,
                                navSurface
                            )
                        )
                    )
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.10f),
                                Color.Transparent
                            )
                        ),
                        topLeft = Offset(0f, -8.dp.toPx()),
                        size = Size(size.width, 8.dp.toPx())
                    )
                    
                    drawLine(
                        color = navBorder,
                        start = Offset(0f, 0f),
                        end = Offset(size.width, 0f),
                        strokeWidth = 1.25.dp.toPx()
                    )

                    // Sliding Neon Line
                    val tabWidth = size.width / bottomNavItems.size
                    val startX = indicatorOffset * tabWidth
                    val centerX = startX + (tabWidth / 2f)
                    val lineLength = tabWidth * 0.6f // The line takes up 60% of the tab width
                    val neonStartX = centerX - (lineLength / 2f)
                    val neonEndX = centerX + (lineLength / 2f)

                    // Draw the neon glow
                    drawLine(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                GoldPrimary,
                                GoldPrimary,
                                Color.Transparent
                            ),
                            startX = neonStartX - (tabWidth * 0.2f),
                            endX = neonEndX + (tabWidth * 0.2f)
                        ),
                        start = Offset(neonStartX - (tabWidth * 0.2f), 0f),
                        end = Offset(neonEndX + (tabWidth * 0.2f), 0f),
                        strokeWidth = 3.dp.toPx()
                    )
                    
                    // Draw the sharp neon core
                    drawLine(
                        color = GoldPrimary,
                        start = Offset(neonStartX, 0f),
                        end = Offset(neonEndX, 0f),
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    
                    // Glow beam shooting downwards
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                GoldPrimary.copy(alpha = 0.25f),
                                Color.Transparent
                            ),
                            startY = 0f,
                            endY = size.height * 0.6f
                        ),
                        topLeft = Offset(neonStartX - 15f, 0f),
                        size = Size(lineLength + 30f, size.height * 0.6f)
                    )
                }
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                bottomNavItems.forEachIndexed { index, item ->
                    val isSelected = index == selectedIndex
                    val badgeCount = when (item) {
                        is BottomNavItem.Home     -> notificationCount
                        is BottomNavItem.Messages -> unreadMessageCount
                        is BottomNavItem.Profile  -> pendingInviteCount
                        else -> 0
                    }
                    CyberNavItem(
                        item = item,
                        isSelected = isSelected,
                        badgeCount = badgeCount,
                        responsive = responsive,
                        onClick = {
                            if (!isSelected) {
                                navController.navigate(item.route) {
                                    popUpTo(item.route) { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun CyberNavItem(
    item: BottomNavItem,
    isSelected: Boolean,
    badgeCount: Int,
    responsive: ResponsiveMetrics,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.15f else 0.95f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "iconScale"
    )

    val iconColor by animateColorAsState(
        targetValue = if (isSelected) GoldPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.76f),
        animationSpec = tween(250),
        label = "iconColor"
    )

    val labelAlpha by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = tween(200),
        label = "labelAlpha"
    )
    
    val labelHeight by animateDpAsState(
        targetValue = if (isSelected) 14.dp else 0.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy),
        label = "labelHeight"
    )

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.scale(scale)
        ) {
            Box(contentAlignment = Alignment.Center) {
                // Subtle glow behind the icon instead of a solid shadow shape
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(if (responsive.isCompact) 40.dp else 44.dp)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(GoldPrimary.copy(alpha = 0.2f), Color.Transparent)
                                ),
                                shape = CircleShape
                            )
                    )
                }

                Icon(
                    imageVector = if (isSelected) item.selectedIcon else item.icon,
                    contentDescription = stringResource(item.labelRes),
                    tint = iconColor,
                    modifier = Modifier.size(if (responsive.isCompact) 26.dp else 28.dp)
                )

                // ── Badge ──
                if (badgeCount > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 6.dp, y = (-4).dp)
                            .size(if (badgeCount > 9) 18.dp else 16.dp)
                            .shadow(4.dp, CircleShape, spotColor = ErrorRed)
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(Color(0xFFFF5252), Color(0xFFFF1744))
                                ),
                                shape = CircleShape
                            )
                            .border(1.dp, MaterialTheme.colorScheme.surface, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (badgeCount > 99) "99+" else badgeCount.toString(),
                            color = White,
                            fontSize = if (badgeCount > 9) 8.sp else 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            lineHeight = 9.sp
                        )
                    }
                }
            }

            // Animated Label under icon
            if (labelHeight > 0.dp) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = stringResource(item.labelRes),
                    color = GoldPrimary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Visible,
                    modifier = Modifier.alpha(labelAlpha).height(labelHeight)
                )
            }
        }
    }
}

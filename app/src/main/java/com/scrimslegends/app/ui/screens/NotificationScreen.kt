package com.scrimslegends.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scrimslegends.app.data.model.Notification
import com.scrimslegends.app.data.model.NotificationType
import com.scrimslegends.app.R
import androidx.compose.ui.res.stringResource
import com.scrimslegends.app.ui.theme.*
import com.scrimslegends.app.ui.components.AnimatedEntrance
import com.scrimslegends.app.ui.components.GlassBackButton
import com.scrimslegends.app.ui.components.NotificationListSkeleton
import com.scrimslegends.app.ui.components.PullToRefreshContainer
import com.scrimslegends.app.ui.components.SwipeToAction

@Composable
fun NotificationScreen(
    notifications: List<Notification>,
    isLoading: Boolean,
    isRefreshing: Boolean = false,
    error: String?,
    onNavigateBack: () -> Unit,
    onMarkAsRead: (String) -> Unit,
    onMarkAllAsRead: () -> Unit,
    onDelete: (String) -> Unit,
    onNotificationClick: (Notification) -> Unit = {},
    onRefresh: () -> Unit = {},
    onDismissError: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = heroGradientBrush())
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            AnimatedEntrance(delayMillis = 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, top = 20.dp, end = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GlassBackButton(onClick = onNavigateBack)

                    Text(
                        text = stringResource(R.string.notifications),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = White
                        )
                    )

                    if (notifications.any { !it.isRead }) {
                        TextButton(onClick = onMarkAllAsRead) {
                            Text(
                                text = stringResource(R.string.mark_all_read),
                                color = BluePrimary,
                                fontSize = 13.sp
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.width(80.dp))
                    }
                }
            }

            // Error display
            if (error != null) {
                AnimatedEntrance(delayMillis = 0) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = ErrorRed.copy(alpha = 0.1f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = stringResource(R.string.error),
                                    tint = ErrorRed,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = error,
                                    color = White,
                                    fontSize = 13.sp
                                )
                            }
                            IconButton(
                                onClick = onDismissError,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = stringResource(R.string.dismiss),
                                    tint = White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            PullToRefreshContainer(
                isRefreshing = isRefreshing,
                onRefresh = onRefresh,
                modifier = Modifier.weight(1f)
            ) {
                when {
                    isLoading && notifications.isEmpty() -> {
                        NotificationListSkeleton(
                            modifier = Modifier.fillMaxSize(),
                            itemCount = 6
                        )
                    }
                    notifications.isEmpty() -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsNone,
                                contentDescription = null,
                                tint = LightGray.copy(alpha = 0.4f),
                                modifier = Modifier.size(72.dp)
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Text(
                                text = stringResource(R.string.no_notifications),
                                style = iOSTitle3.copy(color = White)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.notifications_hint),
                                style = iOSFootnote.copy(color = MidGray),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 20.dp),
                            contentPadding = PaddingValues(vertical = 12.dp)
                        ) {
                            itemsIndexed(notifications, key = { _, n -> n.id }) { index, notification ->
                                AnimatedEntrance(delayMillis = index * 60) {
                                    SwipeToAction(
                                        actions = {
                                            IconButton(
                                                onClick = { onDelete(notification.id) },
                                                modifier = Modifier.size(48.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = stringResource(R.string.delete),
                                                    tint = ErrorRed,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                        }
                                    ) {
                                        NotificationRow(
                                            notification = notification,
                                            onClick = {
                                                // Navigation + mark-as-read handled in onNotificationClick
                                                onNotificationClick(notification)
                                            },
                                            onDismiss = { onDelete(notification.id) }
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotificationRow(
    notification: Notification,
    onClick: () -> Unit,
    onDismiss: () -> Unit
) {
    val (icon, iconColor, bgColor) = when (notification.type) {
        // ── Scrim ────────────────────────────────────────────────────────
        NotificationType.SCRIM_INVITE ->
            Triple(Icons.Default.SportsEsports, BluePrimary,   BluePrimary.copy(alpha = 0.12f))
        NotificationType.SCRIM_APPLICATION_NEW ->
            Triple(Icons.Default.SportsEsports, BluePrimary,   BluePrimary.copy(alpha = 0.12f))
        NotificationType.SCRIM_APPLICATION_APPROVED ->
            Triple(Icons.Default.CheckCircle,   SuccessGreen,  SuccessGreen.copy(alpha = 0.12f))
        NotificationType.SCRIM_APPLICATION_REJECTED ->
            Triple(Icons.Default.Cancel,        ErrorRed,      ErrorRed.copy(alpha = 0.12f))
        NotificationType.SCRIM_OPPONENT_FOUND ->
            Triple(Icons.Default.SportsEsports,  BluePrimary,   BluePrimary.copy(alpha = 0.12f))
        // ── Match ────────────────────────────────────────────────────────
        NotificationType.MATCH_RESULT ->
            Triple(Icons.Default.EmojiEvents,   GoldPrimary,   GoldPrimary.copy(alpha = 0.12f))
        // ── Team ─────────────────────────────────────────────────────────
        NotificationType.TEAM_INVITE ->
            Triple(Icons.Default.Group,         SuccessGreen,  SuccessGreen.copy(alpha = 0.12f))
        // ── Message ──────────────────────────────────────────────────────
        NotificationType.MESSAGE ->
            Triple(Icons.Default.ChatBubble,    Purple,        Purple.copy(alpha = 0.12f))
        // ── System / Progress ────────────────────────────────────────────
        NotificationType.SYSTEM ->
            Triple(Icons.Default.Info,          LightGray,     White.copy(alpha = 0.08f))
        NotificationType.XP_GAIN ->
            Triple(Icons.Default.TrendingUp,    WarningOrange, WarningOrange.copy(alpha = 0.12f))
        NotificationType.TIER_UP ->
            Triple(Icons.Default.Star,          Grandmaster,   Grandmaster.copy(alpha = 0.12f))
        // ── Tournament (DB-generated) ────────────────────────────────────
        NotificationType.TOURNAMENT_APPLICATION_NEW ->
            Triple(Icons.Default.EmojiEvents,   GoldPrimary,   GoldPrimary.copy(alpha = 0.12f))
        NotificationType.TOURNAMENT_APPLICATION_ACCEPTED ->
            Triple(Icons.Default.CheckCircle,   SuccessGreen,  SuccessGreen.copy(alpha = 0.12f))
        NotificationType.TOURNAMENT_APPLICATION_REJECTED ->
            Triple(Icons.Default.Cancel,        ErrorRed,      ErrorRed.copy(alpha = 0.12f))
        NotificationType.TOURNAMENT_APPLICATION_BLOCKED ->
            Triple(Icons.Default.Block,         ErrorRed,      ErrorRed.copy(alpha = 0.12f))
        NotificationType.TOURNAMENT_CANCELLED ->
            Triple(Icons.Default.Cancel,        ErrorRed,      ErrorRed.copy(alpha = 0.12f))
        NotificationType.TOURNAMENT_COMPLETED ->
            Triple(Icons.Default.EmojiEvents,   GoldPrimary,   GoldPrimary.copy(alpha = 0.12f))
        NotificationType.TOURNAMENT_ROUND_ADVANCED ->
            Triple(Icons.Default.PlayArrow,     BluePrimary,   BluePrimary.copy(alpha = 0.12f))
        NotificationType.TOURNAMENT_TEAM_DISQUALIFIED ->
            Triple(Icons.Default.PersonOff,     ErrorRed,      ErrorRed.copy(alpha = 0.12f))
        NotificationType.TOURNAMENT_ROSTER_LOCKED ->
            Triple(Icons.Default.Lock,          Purple,        Purple.copy(alpha = 0.12f))
        NotificationType.TOURNAMENT_MATCH_RESULT ->
            Triple(Icons.Default.EmojiEvents,   SuccessGreen,  SuccessGreen.copy(alpha = 0.12f))
        // ── Tournament (legacy app-generated names) ──────────────────────
        NotificationType.TOURNAMENT_APPLICATION_STATUS ->
            Triple(Icons.Default.EmojiEvents,   GoldPrimary,   GoldPrimary.copy(alpha = 0.12f))
        NotificationType.TOURNAMENT_MATCH_SCHEDULED ->
            Triple(Icons.Default.Schedule,      BluePrimary,   BluePrimary.copy(alpha = 0.12f))
        NotificationType.TOURNAMENT_ROUND_START ->
            Triple(Icons.Default.PlayArrow,     ErrorRed,      ErrorRed.copy(alpha = 0.12f))
        NotificationType.TOURNAMENT_HOST_REQUEST_STATUS ->
            Triple(Icons.Default.Verified,      Purple,        Purple.copy(alpha = 0.12f))
        NotificationType.TOURNAMENT_HOST_APPROVED ->
            Triple(Icons.Default.Verified,      SuccessGreen,  SuccessGreen.copy(alpha = 0.12f))
        NotificationType.TOURNAMENT_HOST_REJECTED ->
            Triple(Icons.Default.Cancel,        ErrorRed,      ErrorRed.copy(alpha = 0.12f))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (notification.isRead) 2.dp else 4.dp,
                spotColor = if (!notification.isRead) iconColor.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.05f),
                shape = RoundedCornerShape(16.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (notification.isRead) DarkNavy else DarkSurface
        ),
        shape = RoundedCornerShape(16.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Unread indicator
            if (!notification.isRead) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(iconColor, shape = CircleShape)
                )
                Spacer(modifier = Modifier.width(10.dp))
            } else {
                Spacer(modifier = Modifier.width(18.dp))
            }

            // Icon
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(bgColor, shape = RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = notification.title,
                    fontSize = 15.sp,
                    fontWeight = if (notification.isRead) FontWeight.Medium else FontWeight.Bold,
                    color = White
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = notification.message,
                    fontSize = 13.sp,
                    color = if (notification.isRead) MidGray else LightGray,
                    maxLines = 2
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatNotificationTime(notification.timestamp),
                    fontSize = 11.sp,
                    color = MidGray
                )
            }

            // Dismiss
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = MidGray,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

private fun formatNotificationTime(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    return when {
        diff < 60000 -> "Just now"
        diff < 3600000 -> "${diff / 60000}m ago"
        diff < 86400000 -> "${diff / 3600000}h ago"
        diff < 604800000 -> "${diff / 86400000}d ago"
        else -> java.text.SimpleDateFormat("MMM dd", java.util.Locale.getDefault()).format(java.util.Date(timestamp))
    }
}

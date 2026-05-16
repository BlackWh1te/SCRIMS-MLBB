package com.mlbb.scrim.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mlbb.scrim.data.model.Conversation
import com.mlbb.scrim.R
import androidx.compose.ui.res.stringResource
import com.mlbb.scrim.ui.theme.*
import com.mlbb.scrim.ui.components.AnimatedEntrance
import com.mlbb.scrim.ui.components.GlassBackButton
import com.mlbb.scrim.ui.components.EmptyState
import com.mlbb.scrim.ui.components.PullToRefreshContainer
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageListScreen(
    conversations  : List<Conversation>,
    isLoading      : Boolean,
    currentUserId  : String,
    onNavigateBack : () -> Unit = {},
    isTab          : Boolean = false,
    onNavigateToChat: (Conversation) -> Unit,
    onRefresh      : () -> Unit = {}
) {
    val totalUnread = conversations.sumOf { it.unreadCount }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = heroGradientBrush())
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Header ──────────────────────────────────────────
            AnimatedEntrance(delayMillis = 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .drawBehind {
                            drawLine(
                                color       = GlassBorder,
                                start       = Offset(0f, size.height),
                                end         = Offset(size.width, size.height),
                                strokeWidth = 1f
                            )
                        }
                        .background(DarkNavy.copy(alpha = 0.50f))
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        if (!isTab) {
                            GlassBackButton(onClick = onNavigateBack)
                        } else {
                            Spacer(Modifier.size(44.dp))
                        }

                        // Title + unread badge
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text  = stringResource(R.string.messages),
                                style = iOSTitle3.copy(color = TextPrimary)
                            )
                            if (totalUnread > 0) {
                                Spacer(Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .height(20.dp)
                                        .widthIn(min = 20.dp)
                                        .background(ErrorRed, CircleShape)
                                        .padding(horizontal = 5.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        totalUnread.coerceAtMost(99).toString(),
                                        fontSize   = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color      = White
                                    )
                                }
                            }
                        }

                        // Refresh button
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(SurfaceOverlay)
                                .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
                                .clickable { onRefresh() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Refresh, "Refresh",
                                tint     = TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // ── Content ─────────────────────────────────────────
            PullToRefreshContainer(
                isRefreshing = isLoading,
                onRefresh    = onRefresh,
                modifier     = Modifier.weight(1f)
            ) {
                when {
                    isLoading && conversations.isEmpty() -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                color       = GoldPrimary,
                                modifier    = Modifier.size(36.dp),
                                strokeWidth = 3.dp
                            )
                        }
                    }

                    conversations.isEmpty() -> {
                        EmptyState(
                            icon     = Icons.Default.ChatBubble,
                            title    = "No messages yet",
                            subtitle = "When teams apply to your scrims or you apply to theirs, conversations will appear here."
                        )
                    }

                    else -> {
                        LazyColumn(
                            modifier       = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                start  = 16.dp,
                                end    = 16.dp,
                                top    = 12.dp,
                                bottom = 96.dp          // room for nav bar
                            ),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            itemsIndexed(conversations) { index, conversation ->
                                AnimatedEntrance(delayMillis = index * 50) {
                                    ConversationCard(
                                        conversation  = conversation,
                                        currentUserId = currentUserId,
                                        onClick       = { onNavigateToChat(conversation) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Conversation Card ────────────────────────────────────────

@Composable
private fun ConversationCard(
    conversation  : Conversation,
    currentUserId : String,
    onClick       : () -> Unit
) {
    val isCurrentUserParticipantA = conversation.participantAId == currentUserId
    val otherName = if (isCurrentUserParticipantA) conversation.participantBName  else conversation.participantAName
    val otherTeam = if (isCurrentUserParticipantA) conversation.participantBTeamName else conversation.participantATeamName
    val hasUnread = conversation.unreadCount > 0

    // Pick a stable avatar gradient per name
    val avatarColors = remember(otherName) {
        val hash = otherName.hashCode()
        val palettes = listOf(
            listOf(Color(0xFF2196F3), Color(0xFF1565C0)),
            listOf(Color(0xFF7C4DFF), Color(0xFF4527A0)),
            listOf(Color(0xFF00BCD4), Color(0xFF006064)),
            listOf(Color(0xFFFF9800), Color(0xFFE65100)),
            listOf(Color(0xFF4CAF50), Color(0xFF1B5E20))
        )
        palettes[Math.abs(hash) % palettes.size]
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(
                if (hasUnread) SurfaceCard
                else SurfaceCard.copy(alpha = 0.75f)
            )
            .border(
                width  = 1.dp,
                color  = if (hasUnread) BluePrimary.copy(alpha = 0.20f) else GlassBorder,
                shape  = RoundedCornerShape(18.dp)
            )
            .clickable { onClick() }
    ) {
        // Unread left accent stripe
        if (hasUnread) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .width(3.dp)
                    .height(42.dp)
                    .clip(RoundedCornerShape(topEnd = 3.dp, bottomEnd = 3.dp))
                    .background(Brush.verticalGradient(BlueGradient))
            )
        }

        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.linearGradient(avatarColors)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text       = otherName.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                    fontSize   = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color      = White
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                // Name + time row
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text(
                        text     = otherName,
                        style    = iOSHeadline.copy(
                            color      = TextPrimary,
                            fontWeight = if (hasUnread) FontWeight.Bold else FontWeight.SemiBold
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text  = formatMessageTime(conversation.lastMessageTime),
                        style = iOSCaption2.copy(
                            color      = if (hasUnread) BluePrimary else TextTertiary,
                            fontWeight = if (hasUnread) FontWeight.SemiBold else FontWeight.Normal
                        )
                    )
                }

                Spacer(Modifier.height(2.dp))

                // Team name
                if (otherTeam.isNotBlank()) {
                    Text(
                        text     = otherTeam,
                        style    = iOSCaption1.copy(color = BluePrimary),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(3.dp))
                }

                // Last message + unread badge
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text(
                        text     = conversation.lastMessage,
                        style    = iOSFootnote.copy(
                            color      = if (hasUnread) LightGray else TextSecondary,
                            fontWeight = if (hasUnread) FontWeight.Medium else FontWeight.Normal
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    if (hasUnread) {
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .height(20.dp)
                                .widthIn(min = 20.dp)
                                .background(
                                    brush = Brush.horizontalGradient(BlueGradient),
                                    shape = CircleShape
                                )
                                .padding(horizontal = 5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                conversation.unreadCount.coerceAtMost(9).toString(),
                                fontSize   = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color      = White
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Time Formatter ───────────────────────────────────────────

private fun formatMessageTime(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    return when {
        diff < 60_000L          -> "now"
        diff < 3_600_000L       -> "${diff / 60_000}m"
        diff < 86_400_000L      -> "${diff / 3_600_000}h"
        diff < 604_800_000L     -> SimpleDateFormat("EEE", Locale.getDefault()).format(Date(timestamp))
        else                    -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timestamp))
    }
}

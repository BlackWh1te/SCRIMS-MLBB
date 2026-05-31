package com.scrimslegends.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.layout.ContentScale
import coil.compose.SubcomposeAsyncImage
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scrimslegends.app.data.model.Conversation
import com.scrimslegends.app.R
import androidx.compose.ui.res.stringResource
import com.scrimslegends.app.ui.theme.*
import com.scrimslegends.app.ui.components.AnimatedEntrance
import com.scrimslegends.app.ui.components.GlassBackButton
import com.scrimslegends.app.ui.components.EmptyState
import com.scrimslegends.app.ui.components.ErrorSnackbar
import com.scrimslegends.app.ui.components.PullToRefreshContainer
import com.scrimslegends.app.ui.components.ReportDialog
import com.scrimslegends.app.ui.components.UserReportReason
import java.text.SimpleDateFormat
import java.util.*

// ─── Filter Tabs ─────────────────────────────────────────────

private enum class MessageFilter { ALL, UNREAD }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageListScreen(
    conversations   : List<Conversation>,
    isLoading       : Boolean,
    currentUserId   : String,
    onNavigateBack  : () -> Unit = {},
    isTab           : Boolean = false,
    onNavigateToChat: (Conversation) -> Unit,
    onRefresh       : () -> Unit = {},
    isRefreshing    : Boolean = false,
    error           : String? = null,
    onDismissError  : () -> Unit = {},
    onReportUser    : (userId: String, username: String) -> Unit = { _, _ -> },
    teamConversations: List<Conversation> = emptyList()
) {
    val totalUnread = conversations.sumOf { it.unreadCount } + teamConversations.sumOf { it.unreadCount }
    val hasAnyConversation = conversations.isNotEmpty() || teamConversations.isNotEmpty()
    var reportTarget by remember { mutableStateOf<Pair<String, String>?>(null) }

    // Search + filter state
    var searchQuery by remember { mutableStateOf("") }
    var activeFilter by remember { mutableStateOf(MessageFilter.ALL) }
    var isSearchActive by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    // Filtered conversations
    val filteredConversations = remember(conversations, searchQuery, activeFilter) {
        var list = conversations
        if (activeFilter == MessageFilter.UNREAD) list = list.filter { it.unreadCount > 0 }
        if (searchQuery.isNotBlank()) {
            list = list.filter { conv -> conversationMatchesSearch(conv, currentUserId, searchQuery) }
        }
        list
    }
    val visibleTeamConversations = remember(teamConversations, searchQuery, currentUserId) {
        if (searchQuery.isBlank()) teamConversations
        else teamConversations.filter { conversationMatchesSearch(it, currentUserId, searchQuery) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = heroGradientBrush())
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Header ──────────────────────────────────────────
            AnimatedEntrance(delayMillis = 0) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .background(DarkNavy.copy(alpha = 0.75f))
                        .drawBehind {
                            drawLine(
                                color       = GlassBorder,
                                start       = Offset(0f, size.height),
                                end         = Offset(size.width, size.height),
                                strokeWidth = 1f
                            )
                        }
                ) {
                    // Title row
                    Row(
                        modifier              = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        if (!isTab) {
                            GlassBackButton(onClick = onNavigateBack)
                        } else {
                            Spacer(Modifier.size(40.dp))
                        }

                        // Title + unread badge
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text  = stringResource(R.string.messages),
                                style = iOSTitle3.copy(
                                    color      = TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            if (totalUnread > 0) {
                                Spacer(Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .height(20.dp)
                                        .widthIn(min = 20.dp)
                                        .background(
                                            brush = Brush.horizontalGradient(
                                                listOf(GoldPrimary, Color(0xFFFF9500))
                                            ),
                                            shape = CircleShape
                                        )
                                        .padding(horizontal = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        totalUnread.coerceAtMost(99).toString(),
                                        fontSize   = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color      = DarkBlue
                                    )
                                }
                            }
                        }

                        // Search toggle
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSearchActive) GoldPrimary.copy(alpha = 0.15f)
                                    else SurfaceOverlay
                                )
                                .border(
                                    1.dp,
                                    if (isSearchActive) GoldPrimary.copy(alpha = 0.4f) else GlassBorder,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    isSearchActive = !isSearchActive
                                    if (!isSearchActive) {
                                        searchQuery = ""
                                        focusManager.clearFocus()
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Search, null,
                                tint     = if (isSearchActive) GoldPrimary else TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Search bar (animated)
                    AnimatedVisibility(
                        visible = isSearchActive,
                        enter   = expandVertically(
                            animationSpec = tween(220, easing = AppEaseOutCubic)
                        ) + fadeIn(tween(180)),
                        exit    = shrinkVertically(
                            animationSpec = tween(180, easing = AppEaseInCubic)
                        ) + fadeOut(tween(130))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                        ) {
                            OutlinedTextField(
                                value         = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder   = {
                                    Text(
                                        "Search conversations…",
                                        color    = TextTertiary,
                                        fontSize = 14.sp
                                    )
                                },
                                leadingIcon   = {
                                    Icon(
                                        Icons.Default.Search, null,
                                        tint     = TextTertiary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                trailingIcon  = if (searchQuery.isNotBlank()) {
                                    {
                                        IconButton(onClick = { searchQuery = "" }) {
                                            Icon(
                                                Icons.Default.Clear, null,
                                                tint     = TextTertiary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                } else null,
                                modifier      = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(focusRequester)
                                    .height(48.dp),
                                colors        = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor      = GoldPrimary.copy(alpha = 0.5f),
                                    unfocusedBorderColor    = GlassBorder,
                                    focusedContainerColor   = SurfaceOverlay,
                                    unfocusedContainerColor = SurfaceOverlay,
                                    focusedTextColor        = TextPrimary,
                                    unfocusedTextColor      = TextPrimary,
                                    cursorColor             = GoldPrimary
                                ),
                                shape         = RoundedCornerShape(14.dp),
                                textStyle     = iOSBody.copy(fontSize = 14.sp),
                                singleLine    = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(
                                    onSearch = { keyboardController?.hide() }
                                )
                            )
                        }
                        LaunchedEffect(isSearchActive) {
                            if (isSearchActive) focusRequester.requestFocus()
                        }
                    }

                    // Filter tabs
                    if (!isSearchActive || searchQuery.isBlank()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 16.dp, bottom = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MessageFilter.entries.forEach { filter ->
                                val isActive = activeFilter == filter
                                val tabLabel = when (filter) {
                                    MessageFilter.ALL -> "All"
                                    MessageFilter.UNREAD -> if (totalUnread > 0) "Unread ($totalUnread)" else "Unread"
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(
                                            if (isActive) GoldPrimary.copy(alpha = 0.18f)
                                            else SurfaceOverlay.copy(alpha = 0.6f)
                                        )
                                        .border(
                                            1.dp,
                                            if (isActive) GoldPrimary.copy(alpha = 0.45f) else GlassBorder,
                                            RoundedCornerShape(20.dp)
                                        )
                                        .clickable { activeFilter = filter }
                                        .padding(horizontal = 14.dp, vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text       = tabLabel,
                                        fontSize   = 12.sp,
                                        fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                                        color      = if (isActive) GoldPrimary else TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── Content ─────────────────────────────────────────
            PullToRefreshContainer(
                isRefreshing = isRefreshing,
                onRefresh    = onRefresh,
                modifier     = Modifier.weight(1f)
            ) {
                when {
                    isLoading && !hasAnyConversation -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(
                                    color       = GoldPrimary,
                                    modifier    = Modifier.size(36.dp),
                                    strokeWidth = 3.dp
                                )
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    "Loading conversations…",
                                    color    = TextTertiary,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }

                    !hasAnyConversation || (filteredConversations.isEmpty() && visibleTeamConversations.isEmpty()) -> {
                        EmptyState(
                            icon     = Icons.Default.ChatBubble,
                            title    = if (activeFilter == MessageFilter.UNREAD && hasAnyConversation)
                                "All caught up!"
                            else
                                stringResource(R.string.no_messages_yet),
                            subtitle = if (activeFilter == MessageFilter.UNREAD && hasAnyConversation)
                                "No unread messages"
                            else
                                stringResource(R.string.no_messages_subtitle)
                        )
                    }

                    else -> {
                        LazyColumn(
                            modifier       = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                start  = 14.dp,
                                end    = 14.dp,
                                top    = 10.dp,
                                bottom = 100.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // ── Pinned team chats (always at top if visible) ──
                            visibleTeamConversations.forEachIndexed { index, teamConv ->
                                item(key = "team_chat_${teamConv.id}") {
                                    AnimatedEntrance(delayMillis = index * 100) {
                                        TeamChatCard(
                                            conversation = teamConv,
                                            onClick      = { onNavigateToChat(teamConv) }
                                        )
                                    }
                                }
                            }

                            // ── Section: Today ──
                            val todayConvs = filteredConversations.filter {
                                isToday(it.lastMessageTime)
                            }
                            val olderConvs = filteredConversations.filter {
                                !isToday(it.lastMessageTime)
                            }

                            if (todayConvs.isNotEmpty()) {
                                item(key = "header_today") {
                                    SectionHeader(title = "Today")
                                }
                                itemsIndexed(
                                    todayConvs,
                                    key = { _, c -> "today_${c.id}" }
                                ) { index, conversation ->
                                    AnimatedEntrance(delayMillis = index * 40) {
                                        ConversationCard(
                                            conversation  = conversation,
                                            currentUserId = currentUserId,
                                            onClick       = { onNavigateToChat(conversation) },
                                            onReport      = { userId, username ->
                                                reportTarget = Pair(userId, username)
                                            }
                                        )
                                    }
                                }
                            }

                            if (olderConvs.isNotEmpty()) {
                                item(key = "header_earlier") {
                                    SectionHeader(title = "Earlier")
                                }
                                itemsIndexed(
                                    olderConvs,
                                    key = { _, c -> "older_${c.id}" }
                                ) { index, conversation ->
                                    AnimatedEntrance(delayMillis = index * 40) {
                                        ConversationCard(
                                            conversation  = conversation,
                                            currentUserId = currentUserId,
                                            onClick       = { onNavigateToChat(conversation) },
                                            onReport      = { userId, username ->
                                                reportTarget = Pair(userId, username)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Error snackbar
        ErrorSnackbar(
            error     = error,
            onDismiss = onDismissError,
            modifier  = Modifier.align(Alignment.BottomCenter)
        )
    }

    if (reportTarget != null) {
        ReportDialog(
            targetName  = reportTarget!!.second,
            reasons     = UserReportReason.values().map { it.label },
            onDismiss   = { reportTarget = null },
            onSubmit    = { _, _ ->
                onReportUser(reportTarget!!.first, reportTarget!!.second)
                reportTarget = null
            }
        )
    }
}

// ── Section Header ──────────────────────────────────────────

@Composable
private fun SectionHeader(title: String) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text       = title,
            fontSize   = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color      = TextTertiary,
            letterSpacing = 0.8.sp
        )
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(GlassBorder)
        )
    }
}

// ── Conversation Card ────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ConversationCard(
    conversation  : Conversation,
    currentUserId : String,
    onClick       : () -> Unit,
    onReport      : (userId: String, username: String) -> Unit = { _, _ -> }
) {
    val isCurrentUserParticipantA = conversation.participantAId == currentUserId
    val otherName = when {
        conversation.isTeamChat -> conversation.groupName.takeIf { it.isNotBlank() }
            ?: stringResource(R.string.team_chat_default_name)
        isCurrentUserParticipantA -> conversation.participantBName
        else -> conversation.participantAName
    }
    val otherUserId    = if (isCurrentUserParticipantA) conversation.participantBId else conversation.participantAId
    val otherTeam      = if (isCurrentUserParticipantA) conversation.participantBTeamName else conversation.participantATeamName
    val otherAvatarUrl = if (isCurrentUserParticipantA) conversation.participantBAvatarUrl else conversation.participantAAvatarUrl
    val hasUnread = conversation.unreadCount > 0

    // Stable gradient per name
    val avatarColors = remember(otherName) {
        val palettes = listOf(
            BlueGradient,
            PurpleGradient,
            listOf(Color(0xFF00BCD4), Color(0xFF006064)),
            PremiumOrangeGradient,
            PremiumGreenGradient
        )
        palettes[Math.abs(otherName.hashCode()) % palettes.size]
    }

    val cardBg by animateColorAsState(
        targetValue   = if (hasUnread) SurfaceCard else SurfaceCard.copy(alpha = 0.65f),
        animationSpec = tween(200),
        label         = "cardBg"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(cardBg)
            .border(
                width  = if (hasUnread) 1.dp else 0.5.dp,
                color  = if (hasUnread) GoldPrimary.copy(alpha = 0.22f) else GlassBorder,
                shape  = RoundedCornerShape(18.dp)
            )
            .combinedClickable(
                onClick      = { onClick() },
                onLongClick  = { onReport(otherUserId, otherName) }
            )
    ) {
        // Unread left accent stripe
        if (hasUnread) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .width(3.dp)
                    .height(48.dp)
                    .clip(RoundedCornerShape(topEnd = 3.dp, bottomEnd = 3.dp))
                    .background(Brush.verticalGradient(GoldGradient))
            )
        }

        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar + online indicator
            Box {
                if (otherAvatarUrl != null) {
                    SubcomposeAsyncImage(
                        model              = otherAvatarUrl,
                        contentDescription = otherName,
                        modifier           = Modifier
                            .size(50.dp)
                            .clip(CircleShape),
                        contentScale       = ContentScale.Crop,
                        loading = {
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(CircleShape)
                                    .background(Brush.linearGradient(avatarColors)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text       = otherName.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                                    fontSize   = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color      = White
                                )
                            }
                        },
                        error = {
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(CircleShape)
                                    .background(Brush.linearGradient(avatarColors)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text       = otherName.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                                    fontSize   = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color      = White
                                )
                            }
                        }
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(avatarColors)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text       = otherName.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                            fontSize   = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color      = White
                        )
                    }
                }
                // Online dot placeholder (gray = offline/unknown)
                Box(
                    modifier = Modifier
                        .size(13.dp)
                        .align(Alignment.BottomEnd)
                        .background(DarkNavy, CircleShape)
                        .padding(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(DimGray, CircleShape)
                    )
                }
            }

            Spacer(Modifier.width(13.dp))

            Column(modifier = Modifier.weight(1f)) {
                // Name + time row
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text(
                        text     = otherName,
                        fontSize = 15.sp,
                        fontWeight = if (hasUnread) FontWeight.Bold else FontWeight.SemiBold,
                        color    = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text  = formatMessageTime(conversation.lastMessageTime),
                        fontSize   = 11.sp,
                        color      = if (hasUnread) GoldPrimary.copy(alpha = 0.85f) else TextTertiary,
                        fontWeight = if (hasUnread) FontWeight.SemiBold else FontWeight.Normal
                    )
                }

                Spacer(Modifier.height(2.dp))

                // Team tag
                if (otherTeam.isNotBlank()) {
                    Text(
                        text     = otherTeam,
                        fontSize = 11.sp,
                        color    = BluePrimary.copy(alpha = 0.8f),
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
                        fontSize = 13.sp,
                        color    = if (hasUnread) LightGray else TextSecondary,
                        fontWeight = if (hasUnread) FontWeight.Medium else FontWeight.Normal,
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
                                    brush = Brush.horizontalGradient(GoldGradient),
                                    shape = CircleShape
                                )
                                .padding(horizontal = 5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                conversation.unreadCount.coerceAtMost(9).toString(),
                                fontSize   = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color      = DarkBlue
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Team Chat Card (pinned group chat) ──────────────────────

@Composable
private fun TeamChatCard(
    conversation: Conversation,
    onClick: () -> Unit
) {
    val hasUnread   = conversation.unreadCount > 0
    val displayName = conversation.groupName.takeIf { it.isNotBlank() }
        ?: stringResource(R.string.team_chat_default_name)
    val memberCount = conversation.participantCount

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                brush = Brush.verticalGradient(
                    listOf(
                        Color(0xFF1A2340).copy(alpha = 0.90f),
                        Color(0xFF0F1B2E).copy(alpha = 0.95f)
                    )
                )
            )
            .border(
                width = 1.5.dp,
                brush = Brush.horizontalGradient(GoldGradient),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable { onClick() }
    ) {
        // Subtle gold glow at top edge
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth(0.6f)
                .height(1.dp)
                .background(
                    brush = Brush.horizontalGradient(GoldGlowGradient),
                    shape = RoundedCornerShape(2.dp)
                )
        )

        // Left gold accent stripe
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .width(3.dp)
                .height(52.dp)
                .clip(RoundedCornerShape(topEnd = 3.dp, bottomEnd = 3.dp))
                .background(Brush.verticalGradient(GoldGradient))
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 14.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Group avatar — stacked circles for team feel
            Box(
                modifier = Modifier.size(50.dp),
                contentAlignment = Alignment.Center
            ) {
                // Background circle (offset back-right)
                Box(
                    modifier = Modifier
                        .offset(x = 4.dp, y = (-2).dp)
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Brush.radialGradient(BlueGradient))
                )
                // Foreground circle
                Box(
                    modifier = Modifier
                        .offset(x = (-4).dp, y = 2.dp)
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Brush.radialGradient(GoldGradient)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector        = Icons.Default.Group,
                        contentDescription = null,
                        tint               = DarkBlue,
                        modifier           = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(Modifier.width(13.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Text(
                            text       = displayName,
                            fontSize   = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color      = TextPrimary,
                            maxLines   = 1,
                            overflow   = TextOverflow.Ellipsis,
                            modifier   = Modifier.weight(1f, fill = false)
                        )
                        Spacer(Modifier.width(6.dp))
                        // TEAM badge
                        Box(
                            modifier = Modifier
                                .height(17.dp)
                                .background(
                                    brush = Brush.horizontalGradient(GoldGradient),
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text       = stringResource(R.string.team_badge),
                                fontSize   = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color      = DarkBlue
                            )
                        }
                        Spacer(Modifier.width(4.dp))
                        // Member count pill
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .background(
                                    SurfaceOverlay.copy(alpha = 0.7f),
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = TextTertiary,
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(Modifier.width(2.dp))
                            Text(
                                text       = memberCount.toString(),
                                fontSize   = 9.sp,
                                fontWeight = FontWeight.SemiBold,
                                color      = TextSecondary
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Pinned icon
                        Icon(
                            imageVector = Icons.Default.Bookmark,
                            contentDescription = null,
                            tint       = GoldPrimary.copy(alpha = 0.5f),
                            modifier   = Modifier.size(12.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text       = formatMessageTime(conversation.lastMessageTime),
                            fontSize   = 11.sp,
                            color      = if (hasUnread) GoldPrimary.copy(alpha = 0.85f) else TextTertiary,
                            fontWeight = if (hasUnread) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))

                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text(
                        text     = conversation.lastMessage.ifBlank {
                            stringResource(R.string.team_chat_placeholder)
                        },
                        fontSize = 13.sp,
                        color    = if (hasUnread) LightGray else TextSecondary,
                        fontWeight = if (hasUnread) FontWeight.Medium else FontWeight.Normal,
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
                                    brush = Brush.horizontalGradient(GoldGradient),
                                    shape = CircleShape
                                )
                                .padding(horizontal = 5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                conversation.unreadCount.coerceAtMost(9).toString(),
                                fontSize   = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color      = DarkBlue
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Helpers ──────────────────────────────────────────────────

private fun isToday(timestamp: Long): Boolean {
    val cal1 = Calendar.getInstance().apply { timeInMillis = timestamp }
    val cal2 = Calendar.getInstance()
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
           cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}

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

private fun conversationMatchesSearch(
    conversation: Conversation,
    currentUserId: String,
    searchQuery: String
): Boolean {
    val isMe = conversation.participantAId == currentUserId
    val otherName = if (isMe) conversation.participantBName else conversation.participantAName
    val otherTeam = if (isMe) conversation.participantBTeamName else conversation.participantATeamName
    return otherName.contains(searchQuery, ignoreCase = true) ||
        otherTeam.contains(searchQuery, ignoreCase = true) ||
        conversation.lastMessage.contains(searchQuery, ignoreCase = true) ||
        conversation.groupName.contains(searchQuery, ignoreCase = true)
}

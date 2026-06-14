package com.scrimslegends.app.ui.screens

import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.compositeOver
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
import com.scrimslegends.app.ui.components.PremiumLoadingState
import com.scrimslegends.app.ui.components.PullToRefreshContainer
import com.scrimslegends.app.ui.components.ReportDialog
import com.scrimslegends.app.ui.components.UserReportReason
import java.text.SimpleDateFormat
import java.util.*

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList

// ─── Filter Tabs ─────────────────────────────────────────────

private enum class MessageFilter { ALL, UNREAD }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageListScreen(
    conversations   : ImmutableList<Conversation>,
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
    teamConversations: ImmutableList<Conversation> = persistentListOf()
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
    val appSurface = appSurfaceColor()
    val appElevatedSurface = appElevatedSurfaceColor()
    val appTextPrimary = appTextPrimaryColor()
    val appTextSecondary = appTextSecondaryColor()
    val appBorder = appBorderColor()

    // Filtered conversations
    val filteredConversations = remember(conversations, searchQuery, activeFilter) {
        var list = conversations
        if (activeFilter == MessageFilter.UNREAD) list = list.filter { it.unreadCount > 0 }.toPersistentList()
        if (searchQuery.isNotBlank()) {
            list = list.filter { conv -> conversationMatchesSearch(conv, currentUserId, searchQuery) }.toPersistentList()
        }
        list
    }
    val visibleTeamConversations = remember(teamConversations, searchQuery, activeFilter, currentUserId) {
        var list = teamConversations
        if (activeFilter == MessageFilter.UNREAD) list = list.filter { it.unreadCount > 0 }.toPersistentList()
        if (searchQuery.isNotBlank()) {
            list = list.filter { conversationMatchesSearch(it, currentUserId, searchQuery) }.toPersistentList()
        }
        list
    }
    val scrimConversations = remember(filteredConversations) {
        filteredConversations.filter { it.scrimId != null }
    }
    val directConversations = remember(filteredConversations) {
        filteredConversations.filter { it.scrimId == null }
    }
    val pinnedScrimConversations = scrimConversations.filter { it.isPinned }
    val regularScrimConversations = scrimConversations.filterNot { it.isPinned }
    val pinnedDirectConversations = directConversations.filter { it.isPinned }
    val regularDirectConversations = directConversations.filterNot { it.isPinned }

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
                        .background(appSurface.copy(alpha = 0.98f))
                        .drawBehind {
                            drawLine(
                                color       = appBorder,
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
                                    color      = appTextPrimary,
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
                                                listOf(MaterialTheme.colorScheme.secondary, Color(0xFFFF9500))
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
                                        color      = MaterialTheme.colorScheme.background
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
                                    if (isSearchActive) MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                                    else appElevatedSurface
                                )
                                .border(
                                    1.dp,
                                    if (isSearchActive) MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f) else appBorder,
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
                                tint     = if (isSearchActive) MaterialTheme.colorScheme.secondary else appTextSecondary,
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
                                        color    = appTextSecondary,
                                        fontSize = 14.sp
                                    )
                                },
                                leadingIcon   = {
                                    Icon(
                                        Icons.Default.Search, null,
                                        tint     = appTextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                trailingIcon  = if (searchQuery.isNotBlank()) {
                                    {
                                        IconButton(onClick = { searchQuery = "" }) {
                                            Icon(
                                                Icons.Default.Clear, null,
                                                tint     = appTextSecondary,
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
                                    focusedBorderColor      = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f),
                                    unfocusedBorderColor    = appBorder,
                                    focusedContainerColor   = appElevatedSurface,
                                    unfocusedContainerColor = appElevatedSurface,
                                    focusedTextColor        = appTextPrimary,
                                    unfocusedTextColor      = appTextPrimary,
                                    cursorColor             = MaterialTheme.colorScheme.secondary
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
                                            if (isActive) MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f)
                                            else appElevatedSurface
                                        )
                                        .border(
                                            1.dp,
                                            if (isActive) MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f) else appBorder,
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
                                        color      = if (isActive) MaterialTheme.colorScheme.secondary else appTextSecondary
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
                        PremiumLoadingState(
                            message = "Loading conversations...",
                            modifier = Modifier.fillMaxSize()
                        )
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

                            if (pinnedScrimConversations.isNotEmpty()) {
                                item(key = "header_pinned_scrims") {
                                    SectionHeader(title = "Pinned Scrims")
                                }
                                itemsIndexed(
                                    pinnedScrimConversations,
                                    key = { _, c -> "pinned_${c.id}" }
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

                            if (regularScrimConversations.isNotEmpty()) {
                                item(key = "header_scrim_chats") {
                                    SectionHeader(title = "Scrim Chats")
                                }
                                itemsIndexed(
                                    regularScrimConversations,
                                    key = { _, c -> "scrim_${c.id}" }
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

                            if (pinnedDirectConversations.isNotEmpty()) {
                                item(key = "header_pinned_chats") {
                                    SectionHeader(title = "Pinned Chats")
                                }
                                itemsIndexed(
                                    pinnedDirectConversations,
                                    key = { _, c -> "pinned_dm_${c.id}" }
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

                            // ── Section: Today ──
                            val todayConvs = regularDirectConversations.filter {
                                isToday(it.lastMessageTime)
                            }
                            val olderConvs = regularDirectConversations.filter {
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

    reportTarget?.let { target ->
        ReportDialog(
            targetName  = target.second,
            reasons     = UserReportReason.values().map { it.label },
            onDismiss   = { reportTarget = null },
            onSubmit    = { _, _ ->
                onReportUser(target.first, target.second)
                reportTarget = null
            }
        )
    }
}

// ── Section Header ──────────────────────────────────────────

@Composable
private fun SectionHeader(title: String) {
    val appTextSecondary = appTextSecondaryColor()
    val appBorder = appBorderColor()
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
            color      = appTextSecondary,
            letterSpacing = 0.8.sp
        )
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(appBorder)
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
    val otherLastSeen = if (!conversation.isTeamChat) conversation.otherLastSeen(currentUserId) else null
    val isOtherOnline = !conversation.isTeamChat && conversation.isOtherOnline(currentUserId)
    val statusText = when {
        conversation.isTeamChat -> otherTeam
        isOtherOnline -> stringResource(R.string.online_status)
        otherLastSeen != null -> stringResource(R.string.last_seen_status, formatLastSeenCompact(otherLastSeen))
        else -> stringResource(R.string.offline_status)
    }
    val hasUnread = conversation.unreadCount > 0
    val isScrimChat = conversation.scrimId.isNotBlank()
    val appSurface = appSurfaceColor()
    val appElevatedSurface = appElevatedSurfaceColor()
    val appTextPrimary = appTextPrimaryColor()
    val appTextSecondary = appTextSecondaryColor()
    val appBorder = appBorderColor()

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
        targetValue   = if (hasUnread) MaterialTheme.colorScheme.secondary.copy(alpha = 0.04f).compositeOver(appSurface) else appSurface.copy(alpha = 0.92f),
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
                color  = if (hasUnread) MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f) else appBorder,
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
                    .background(Brush.verticalGradient(PremiumBlueGradient))
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
                                    color = MaterialTheme.colorScheme.onSurface
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
                                    color = MaterialTheme.colorScheme.onSurface
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
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                if (!conversation.isTeamChat) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(if (isOtherOnline) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant)
                            .border(2.dp, appSurface, CircleShape)
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
                        color    = appTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(Modifier.width(6.dp))
                    if (conversation.isPinned) {
                        Icon(
                            imageVector = Icons.Default.Bookmark,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.55f),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                    }
                    if (isScrimChat) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                    }
                    Text(
                        text  = formatMessageTime(conversation.lastMessageTime),
                        fontSize   = 11.sp,
                        color      = if (hasUnread) MaterialTheme.colorScheme.secondary.copy(alpha = 0.85f) else appTextSecondary,
                        fontWeight = if (hasUnread) FontWeight.SemiBold else FontWeight.Normal
                    )
                }

                Spacer(Modifier.height(2.dp))

                if (statusText.isNotBlank()) {
                    Text(
                        text     = if (!conversation.isTeamChat && otherTeam.isNotBlank()) "$statusText - $otherTeam" else statusText,
                        fontSize = 11.sp,
                        color    = if (isOtherOnline) SuccessGreen else MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
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
                        color    = if (hasUnread) appTextPrimary else appTextSecondary,
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
                                    brush = Brush.horizontalGradient(PremiumBlueGradient),
                                    shape = CircleShape
                                )
                                .padding(horizontal = 5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                conversation.unreadCount.coerceAtMost(9).toString(),
                                fontSize   = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color      = MaterialTheme.colorScheme.background
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
    val appSurface = appSurfaceColor()
    val appElevatedSurface = appElevatedSurfaceColor()
    val appTextPrimary = appTextPrimaryColor()
    val appTextSecondary = appTextSecondaryColor()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(appSurface)
            .border(
                width = 1.5.dp,
                brush = Brush.horizontalGradient(PremiumBlueGradient),
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
                    brush = Brush.horizontalGradient(BlueGlowGradient),
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
                .background(Brush.verticalGradient(PremiumBlueGradient))
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
                        .background(Brush.radialGradient(PremiumBlueGradient)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector        = Icons.Default.Group,
                        contentDescription = null,
                        tint               = MaterialTheme.colorScheme.background,
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
                            color      = appTextPrimary,
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
                                    brush = Brush.horizontalGradient(PremiumBlueGradient),
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text       = stringResource(R.string.team_badge),
                                fontSize   = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color      = MaterialTheme.colorScheme.background
                            )
                        }
                        Spacer(Modifier.width(4.dp))
                        // Member count pill
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .background(
                                    appElevatedSurface,
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = appTextSecondary,
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(Modifier.width(2.dp))
                            Text(
                                text       = memberCount.toString(),
                                fontSize   = 9.sp,
                                fontWeight = FontWeight.SemiBold,
                                color      = appTextSecondary
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Pinned icon
                        Icon(
                            imageVector = Icons.Default.Bookmark,
                            contentDescription = null,
                            tint       = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f),
                            modifier   = Modifier.size(12.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text       = formatMessageTime(conversation.lastMessageTime),
                            fontSize   = 11.sp,
                            color      = if (hasUnread) MaterialTheme.colorScheme.secondary.copy(alpha = 0.85f) else appTextSecondary,
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
                        color    = if (hasUnread) appTextPrimary else appTextSecondary,
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
                                    brush = Brush.horizontalGradient(PremiumBlueGradient),
                                    shape = CircleShape
                                )
                                .padding(horizontal = 5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                conversation.unreadCount.coerceAtMost(9).toString(),
                                fontSize   = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color      = MaterialTheme.colorScheme.background
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

private fun formatLastSeenCompact(timestamp: Long): String {
    val diff = (System.currentTimeMillis() - timestamp).coerceAtLeast(0L)
    return when {
        diff < 60_000L -> "just now"
        diff < 3_600_000L -> "${diff / 60_000L}m ago"
        diff < 86_400_000L -> "${diff / 3_600_000L}h ago"
        diff < 604_800_000L -> "${diff / 86_400_000L}d ago"
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timestamp))
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

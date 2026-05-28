package com.mlbb.scrim.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import coil.compose.SubcomposeAsyncImage
import com.mlbb.scrim.R
import com.mlbb.scrim.data.model.Conversation
import com.mlbb.scrim.data.model.DeliveryStatus
import com.mlbb.scrim.data.model.Message
import com.mlbb.scrim.data.model.MessageType
import com.mlbb.scrim.data.model.MessageWithDelivery
import com.mlbb.scrim.data.model.Team
import com.mlbb.scrim.ui.components.ErrorSnackbar
import com.mlbb.scrim.ui.components.GlassBackButton
import com.mlbb.scrim.ui.components.PullToRefreshContainer
import com.mlbb.scrim.ui.theme.*
import com.mlbb.scrim.util.ContentModerationUtils
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
@Suppress("UNUSED_PARAMETER")
fun ChatScreen(
    conversation    : Conversation,
    currentUserId   : String,
    currentUserName : String,
    onNavigateBack  : () -> Unit,
    onSendMessage   : (String) -> Unit,
    onSendImage     : () -> Unit,
    onUpdateTyping  : (Boolean) -> Unit,
    onViewTeamInfo  : (teamId: String, teamName: String) -> Unit,
    onReportUser    : (userId: String, userName: String) -> Unit = { _, _ -> },
    isLoading       : Boolean = false,
    error           : String? = null,
    onDismissError  : () -> Unit = {},
    teamInfo        : Team? = null,
    isRefreshing    : Boolean = false,
    onRefresh       : () -> Unit = {},
    messagesWithDelivery: List<MessageWithDelivery> = emptyList(),
    onRetryMessage  : (String) -> Unit = {},
    onCancelMessage : (String) -> Unit = {}
) {
    var messageText by remember { mutableStateOf("") }
    var moderationError by remember { mutableStateOf<String?>(null) }
    val listState   = rememberLazyListState()
    val scope       = rememberCoroutineScope()
    val displayedMessages = remember(conversation.messages, messagesWithDelivery) {
        if (messagesWithDelivery.isNotEmpty()) messagesWithDelivery
        else conversation.messages.map { MessageWithDelivery(message = it) }
    }

    LaunchedEffect(displayedMessages.size) {
        if (displayedMessages.isNotEmpty()) {
            listState.animateScrollToItem(displayedMessages.size - 1)
        }
    }

    LaunchedEffect(Unit) {
        snapshotFlow { messageText }
            .collect { text ->
                if (text.isNotEmpty()) {
                    onUpdateTyping(true)
                    kotlinx.coroutines.delay(3000)
                    onUpdateTyping(false)
                } else {
                    onUpdateTyping(false)
                }
            }
    }

    val isCurrentUserParticipantA = conversation.participantAId == currentUserId
    val otherName      = if (isCurrentUserParticipantA) conversation.participantBName  else conversation.participantAName
    val otherTeam      = if (isCurrentUserParticipantA) conversation.participantBTeamName else conversation.participantATeamName
    val otherTeamId    = if (isCurrentUserParticipantA) conversation.participantBTeamId else conversation.participantATeamId
    val otherUserId    = if (isCurrentUserParticipantA) conversation.participantBId else conversation.participantAId
    val otherAvatarUrl = if (isCurrentUserParticipantA) conversation.participantBAvatarUrl else conversation.participantAAvatarUrl
    val isOtherTyping  = conversation.isOtherTyping(currentUserId)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = heroGradientBrush())
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Header ──────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .drawBehind {
                        // Bottom separator line
                        drawLine(
                            color       = GlassBorder,
                            start       = Offset(0f, size.height),
                            end         = Offset(size.width, size.height),
                            strokeWidth = 1f
                        )
                    }
                    .background(DarkNavy.copy(alpha = 0.92f))
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Row(
                    modifier          = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Back
                    GlassBackButton(onClick = onNavigateBack)

                    Spacer(Modifier.width(12.dp))

                    // Avatar
                    Box {
                        if (otherAvatarUrl != null) {
                            SubcomposeAsyncImage(
                                model              = otherAvatarUrl,
                                contentDescription = otherName,
                                modifier           = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape),
                                contentScale       = ContentScale.Crop,
                                loading = {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .background(brush = Brush.linearGradient(BlueGradient), shape = CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text       = otherName.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                                            color      = White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize   = 18.sp
                                        )
                                    }
                                },
                                error = {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .background(brush = Brush.linearGradient(BlueGradient), shape = CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text       = otherName.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                                            color      = White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize   = 18.sp
                                        )
                                    }
                                }
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(
                                        brush = Brush.linearGradient(BlueGradient),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text       = otherName.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                                    color      = White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize   = 18.sp
                                )
                            }
                        }
                        // Presence dot — neutral gray (no real-time presence tracking yet)
                        Box(
                            modifier = Modifier
                                .size(12.dp)
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

                    Spacer(Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text     = otherName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color    = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        AnimatedContent(
                            targetState = isOtherTyping,
                            label       = "typingStatus",
                            transitionSpec = { fadeIn(tween(150)) togetherWith fadeOut(tween(150)) }
                        ) { typing ->
                            if (typing) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    TypingDots()
                                    Spacer(Modifier.width(6.dp))
                                    Text(stringResource(R.string.typing_status), fontSize = 12.sp, color = GoldPrimary)
                                }
                            } else {
                                if (otherTeam.isNotBlank()) {
                                    Text(
                                        text     = otherTeam,
                                        fontSize = 12.sp,
                                        color    = TextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    // Report button
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(SurfaceOverlay)
                            .border(1.dp, GlassBorder, RoundedCornerShape(10.dp))
                            .clickable { onReportUser(otherUserId, otherName) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Report, null,
                            tint     = ErrorRed.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(Modifier.width(8.dp))

                    // Info button
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(SurfaceOverlay)
                            .border(1.dp, GlassBorder, RoundedCornerShape(10.dp))
                            .clickable { onViewTeamInfo(otherTeamId, otherTeam) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Info, null,
                            tint     = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // ── Messages List ───────────────────────────────────
            Box(modifier = Modifier.weight(1f)) {
                PullToRefreshContainer(
                    isRefreshing = isRefreshing,
                    onRefresh = onRefresh,
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (displayedMessages.isEmpty()) {
                        EmptyChatState(otherTeamName = otherTeam)
                    } else {
                        LazyColumn(
                            modifier       = Modifier.fillMaxSize(),
                            state          = listState,
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            itemsIndexed(displayedMessages, key = { _, item -> item.clientMessageId ?: item.message.id }) { index, item ->
                                val message = item.message
                                val isFromMe = message.senderId == currentUserId

                                val prevMessage = if (index > 0) displayedMessages[index - 1].message else null
                                val nextMessage = if (index < displayedMessages.size - 1) displayedMessages[index + 1].message else null

                                val showDateSeparator = prevMessage == null || !isSameDay(prevMessage.timestamp, message.timestamp)
                                val isFirstInGroup = prevMessage == null || prevMessage.senderId != message.senderId || showDateSeparator
                                val isLastInGroup  = nextMessage == null || nextMessage.senderId != message.senderId || !isSameDay(nextMessage.timestamp, message.timestamp)

                                if (showDateSeparator) DateSeparator(timestamp = message.timestamp)

                                MessageBubble(
                                    message        = message,
                                    isFromMe       = isFromMe,
                                    isFirstInGroup = isFirstInGroup,
                                    isLastInGroup  = isLastInGroup,
                                    deliveryStatus = item.status,
                                    clientMessageId = item.clientMessageId,
                                    onRetryMessage = onRetryMessage,
                                    onCancelMessage = onCancelMessage,
                                    onViewTeamInfo = { onViewTeamInfo(otherTeamId, otherTeam) }
                                )
                            }
                        }
                    }
                }

                // Scroll FAB
                val showScrollFab by remember { derivedStateOf { listState.firstVisibleItemIndex > 2 } }
                androidx.compose.animation.AnimatedVisibility(
                    visible  = showScrollFab,
                    enter    = fadeIn() + scaleIn(),
                    exit     = fadeOut() + scaleOut(),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 12.dp, end = 16.dp)
                ) {
                    SmallFloatingActionButton(
                        onClick        = { scope.launch { listState.animateScrollToItem(displayedMessages.size - 1) } },
                        containerColor = GoldPrimary,
                        contentColor   = DarkBlue,
                        shape          = CircleShape,
                        modifier       = Modifier.size(40.dp)
                    ) {
                        Icon(Icons.Default.KeyboardArrowDown, null, modifier = Modifier.size(20.dp))
                    }
                }
            }

            // ── Input Area ──────────────────────────────────────
            if (conversation.isChatOpenNow) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .drawBehind {
                            drawLine(
                                color       = GlassBorder,
                                start       = Offset(0f, 0f),
                                end         = Offset(size.width, 0f),
                                strokeWidth = 1f
                            )
                        }
                        .background(DarkNavy.copy(alpha = 0.97f))
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    // Moderation error
                    androidx.compose.animation.AnimatedVisibility(
                        visible = moderationError != null,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(ErrorRed.copy(alpha = 0.10f))
                                .border(1.dp, ErrorRed.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.ErrorOutline, null,
                                tint = ErrorRed, modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                moderationError ?: "",
                                color = ErrorRed, fontSize = 12.sp
                            )
                        }
                    }
                    Row(
                        modifier          = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        // Media buttons
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(SurfaceOverlay)
                                    .border(1.dp, GlassBorder, CircleShape)
                                    .clickable { onSendImage() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Outlined.Image, null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                            }
                        }

                        Spacer(Modifier.width(8.dp))

                        // Text field
                        OutlinedTextField(
                            value         = messageText,
                            onValueChange = { messageText = it },
                            placeholder   = {
                                Text(stringResource(R.string.message_placeholder), color = TextTertiary, fontSize = 15.sp)
                            },
                            modifier      = Modifier.weight(1f),
                            colors        = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor      = GoldPrimary.copy(alpha = 0.5f),
                                unfocusedBorderColor    = GlassBorder,
                                focusedContainerColor   = SurfaceOverlay,
                                unfocusedContainerColor = SurfaceOverlay,
                                focusedTextColor        = TextPrimary,
                                unfocusedTextColor      = TextPrimary,
                                cursorColor             = GoldPrimary
                            ),
                            shape         = RoundedCornerShape(22.dp),
                            textStyle     = iOSBody.copy(fontSize = 15.sp),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(
                                onSend = {
                                    if (messageText.isNotBlank()) {
                                        val result = ContentModerationUtils.validateChatMessage(messageText)
                                        when (result) {
                                            is ContentModerationUtils.ValidationResult.Valid -> {
                                                moderationError = null
                                                onSendMessage(messageText)
                                                messageText = ""
                                            }
                                            is ContentModerationUtils.ValidationResult.Blocked -> {
                                                moderationError = result.reason
                                            }
                                        }
                                    }
                                }
                            ),
                            maxLines = 5
                        )

                        Spacer(Modifier.width(8.dp))

                        // Send button
                        val sendEnabled = messageText.isNotBlank()
                        val sendBg by animateColorAsState(
                            targetValue   = if (sendEnabled) GoldPrimary else SurfaceOverlay,
                            animationSpec = tween(200),
                            label         = "sendBg"
                        )
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(sendBg)
                                .clickable {
                                    if (sendEnabled) {
                                        val result = ContentModerationUtils.validateChatMessage(messageText)
                                        when (result) {
                                            is ContentModerationUtils.ValidationResult.Valid -> {
                                                moderationError = null
                                                onSendMessage(messageText)
                                                messageText = ""
                                            }
                                            is ContentModerationUtils.ValidationResult.Blocked -> {
                                                moderationError = result.reason
                                            }
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Send, null,
                                tint     = if (sendEnabled) DarkBlue else TextTertiary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            } else {
                ChatLockedOverlay(timeUntilOpens = conversation.timeUntilChatOpens)
            }
        }

        // Error snackbar
        ErrorSnackbar(
            error = error,
            onDismiss = onDismissError,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

// ── Typing Indicator ────────────────────────────────────────

@Composable
private fun TypingDots() {
    val infiniteTransition = rememberInfiniteTransition(label = "typing")
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        repeat(3) { index ->
            val alpha by infiniteTransition.animateFloat(
                initialValue  = 0.3f,
                targetValue   = 1.0f,
                animationSpec = infiniteRepeatable(
                    animation  = tween(600, delayMillis = index * 200),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "dot$index"
            )
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .background(GoldPrimary.copy(alpha = alpha), CircleShape)
            )
        }
    }
}

// ── Message Bubble ──────────────────────────────────────────

@Composable
private fun MessageBubble(
    message       : Message,
    isFromMe      : Boolean,
    isFirstInGroup: Boolean,
    isLastInGroup : Boolean,
    deliveryStatus: DeliveryStatus = DeliveryStatus.SENT,
    clientMessageId: String? = null,
    onRetryMessage: (String) -> Unit = {},
    onCancelMessage: (String) -> Unit = {},
    onViewTeamInfo: () -> Unit
) {
    val topStartR = if (isFromMe || !isFirstInGroup) 18.dp else 4.dp
    val topEndR   = if (!isFromMe || !isFirstInGroup) 18.dp else 4.dp
    val bubbleShape = RoundedCornerShape(
        topStart    = topStartR,
        topEnd      = topEndR,
        bottomStart = 18.dp,
        bottomEnd   = 18.dp
    )

    // Sent: blue→navy gradient with subtle gold shimmer at top
    val myBubbleBrush = Brush.linearGradient(
        colors = listOf(Color(0xFF1E6FD9), Color(0xFF1250A0), Color(0xFF0D3B7A)),
        start  = Offset(0f, 0f),
        end    = Offset(0f, Float.POSITIVE_INFINITY)
    )

    Box(
        modifier         = Modifier
            .fillMaxWidth()
            .padding(top = if (isFirstInGroup) 10.dp else 2.dp),
        contentAlignment = if (isFromMe) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Column(
            horizontalAlignment = if (isFromMe) Alignment.End else Alignment.Start,
            modifier            = Modifier.widthIn(max = 310.dp)
        ) {
            // Sender name for received messages in first position
            if (!isFromMe && isFirstInGroup) {
                Text(
                    text       = message.senderName,
                    fontSize   = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color      = BluePrimary,
                    modifier   = Modifier.padding(start = 14.dp, bottom = 3.dp)
                )
            }

            // Bubble
            Box(
                modifier = Modifier
                    .clip(bubbleShape)
                    .background(
                        if (isFromMe) myBubbleBrush
                        else Brush.linearGradient(
                            listOf(
                                SurfaceElevated,
                                SurfaceCard
                            )
                        )
                    )
                    .then(
                        if (!isFromMe) Modifier.border(
                            0.5.dp,
                            GlassBorder,
                            bubbleShape
                        ) else Modifier
                    )
                    .padding(horizontal = 13.dp, vertical = 9.dp)
            ) {
                when (message.type) {
                    MessageType.IMAGE -> ImageContent(url = message.imageUrl ?: "")
                    MessageType.VOICE -> Text(
                        text = "🎤 Voice Note",
                        color = if (isFromMe) White.copy(alpha = 0.7f) else TextSecondary,
                        fontSize = 14.sp
                    )
                    MessageType.APPLY -> ApplyContent(message.content, onViewTeamInfo)
                    else              -> Text(
                        text       = message.content,
                        color      = if (isFromMe) White else TextPrimary,
                        fontSize   = 15.sp,
                        lineHeight = 21.sp
                    )
                }
            }

            // Timestamp + read receipts
            if (isLastInGroup) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier          = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        formatChatTime(message.timestamp),
                        fontSize = 10.sp,
                        color    = TextTertiary
                    )
                    if (isFromMe) {
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            imageVector        = when {
                                deliveryStatus == DeliveryStatus.FAILED  -> Icons.Default.Error
                                deliveryStatus == DeliveryStatus.SENDING ||
                                deliveryStatus == DeliveryStatus.PENDING -> Icons.Default.Schedule
                                message.isRead                           -> Icons.Default.DoneAll
                                else                                     -> Icons.Default.Done
                            },
                            contentDescription = null,
                            tint               = when {
                                deliveryStatus == DeliveryStatus.FAILED  -> ErrorRed
                                deliveryStatus == DeliveryStatus.SENDING ||
                                deliveryStatus == DeliveryStatus.PENDING -> WarningOrange.copy(alpha = 0.8f)
                                message.isRead                           -> GoldPrimary.copy(alpha = 0.85f)
                                else                                     -> TextTertiary.copy(alpha = 0.7f)
                            },
                            modifier           = Modifier.size(12.dp)
                        )
                    }
                }
            }

            if (isFromMe && deliveryStatus == DeliveryStatus.FAILED && clientMessageId != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(end = 8.dp, bottom = 4.dp)
                ) {
                    Text(
                        text = "Failed",
                        fontSize = 10.sp,
                        color = ErrorRed
                    )
                    Text(
                        text = "Retry",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = GoldPrimary,
                        modifier = Modifier.clickable { onRetryMessage(clientMessageId) }
                    )
                    Text(
                        text = "Cancel",
                        fontSize = 10.sp,
                        color = TextTertiary,
                        modifier = Modifier.clickable { onCancelMessage(clientMessageId) }
                    )
                }
            }
        }
    }
}

// ── Media Components ────────────────────────────────────────

@Composable
private fun ImageContent(url: String) {
    SubcomposeAsyncImage(
        model            = url,
        contentDescription = stringResource(R.string.shared_image),
        modifier         = Modifier
            .widthIn(max = 230.dp)
            .heightIn(max = 280.dp)
            .clip(RoundedCornerShape(12.dp)),
        contentScale     = ContentScale.FillWidth,
        loading          = {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(SurfaceOverlay),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = GoldPrimary, strokeWidth = 2.dp)
            }
        }
    )
}

@Composable
private fun ApplyContent(content: String, onView: () -> Unit) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.EmojiEvents, null, tint = GoldPrimary, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.scrim_application), fontWeight = FontWeight.Bold, color = GoldPrimary, fontSize = 13.sp)
        }
        Spacer(Modifier.height(6.dp))
        Text(content, color = TextPrimary, fontSize = 13.sp, lineHeight = 18.sp)
        Spacer(Modifier.height(10.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(White.copy(alpha = 0.14f))
                .clickable { onView() }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                stringResource(R.string.review_application),
                color      = White,
                fontWeight = FontWeight.SemiBold,
                fontSize   = 13.sp
            )
        }
    }
}

// ── Date Separator ──────────────────────────────────────────

@Composable
private fun DateSeparator(timestamp: Long) {
    Box(
        modifier         = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .background(SurfaceOverlay, CircleShape)
                .border(1.dp, GlassBorder, CircleShape)
                .padding(horizontal = 16.dp, vertical = 5.dp)
        ) {
            Text(
                formatDateHeader(timestamp),
                fontSize   = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color      = TextSecondary,
                letterSpacing = 0.5.sp
            )
        }
    }
}

// ── Chat Locked Overlay ─────────────────────────────────────

@Composable
private fun ChatLockedOverlay(timeUntilOpens: Long) {
    var remaining by remember { mutableLongStateOf(timeUntilOpens) }
    LaunchedEffect(timeUntilOpens) {
        while (remaining > 0) {
            kotlinx.coroutines.delay(1000)
            remaining = (remaining - 1000).coerceAtLeast(0)
        }
    }
    val hours   = remaining / 3600000
    val minutes = (remaining / 60000) % 60
    val seconds = (remaining / 1000) % 60

    Box(
        Modifier
            .fillMaxWidth()
            .background(DarkNavy.copy(alpha = 0.98f))
            .navigationBarsPadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(WarningOrange.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                    .border(1.dp, WarningOrange.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Lock, null, tint = WarningOrange, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.chat_opens_in), fontSize = 13.sp, color = TextSecondary)
            Text(
                String.format("%02d:%02d:%02d", hours, minutes, seconds),
                fontSize   = 34.sp,
                fontWeight = FontWeight.Black,
                color      = WarningOrange,
                letterSpacing = (-0.5).sp
            )
        }
    }
}

// ── Empty Chat State ──────────────────────────────────────

@Composable
private fun EmptyChatState(otherTeamName: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.ChatBubble,
            contentDescription = null,
            tint = LightGray.copy(alpha = 0.4f),
            modifier = Modifier.size(72.dp)
        )
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = stringResource(R.string.no_messages_yet),
            style = MaterialTheme.typography.titleMedium.copy(
                color = White,
                fontWeight = FontWeight.SemiBold
            )
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (otherTeamName.isNotBlank()) {
                stringResource(R.string.start_conversation_with, otherTeamName)
            } else {
                stringResource(R.string.start_conversation)
            },
            style = MaterialTheme.typography.bodyMedium.copy(
                color = MidGray
            ),
            textAlign = TextAlign.Center
        )
    }
}

// ── Helpers ─────────────────────────────────────────────────

private fun isSameDay(t1: Long, t2: Long): Boolean {
    val c1 = Calendar.getInstance().apply { timeInMillis = t1 }
    val c2 = Calendar.getInstance().apply { timeInMillis = t2 }
    return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
           c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
}

private fun formatDateHeader(timestamp: Long): String {
    val now = Calendar.getInstance()
    return when {
        isSameDay(timestamp, now.timeInMillis)           -> "TODAY"
        isSameDay(timestamp, now.timeInMillis - 86400000) -> "YESTERDAY"
        else -> SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                    .format(Date(timestamp)).uppercase()
    }
}

private fun formatChatTime(timestamp: Long): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))

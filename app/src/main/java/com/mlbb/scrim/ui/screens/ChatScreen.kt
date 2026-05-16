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
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.mlbb.scrim.data.model.Conversation
import com.mlbb.scrim.data.model.Message
import com.mlbb.scrim.data.model.MessageType
import com.mlbb.scrim.data.model.Team
import com.mlbb.scrim.ui.components.GlassBackButton
import com.mlbb.scrim.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ChatScreen(
    conversation    : Conversation,
    currentUserId   : String,
    currentUserName : String,
    onNavigateBack  : () -> Unit,
    onSendMessage   : (String) -> Unit,
    onSendImage     : () -> Unit,
    onSendVoice     : () -> Unit,
    onUpdateTyping  : (Boolean) -> Unit,
    onViewTeamInfo  : (teamId: String, teamName: String) -> Unit,
    isLoading       : Boolean = false,
    teamInfo        : Team? = null
) {
    var messageText by remember { mutableStateOf("") }
    val listState   = rememberLazyListState()
    val scope       = rememberCoroutineScope()

    LaunchedEffect(conversation.messages.size) {
        if (conversation.messages.isNotEmpty()) {
            listState.animateScrollToItem(conversation.messages.size - 1)
        }
    }

    LaunchedEffect(messageText) {
        if (messageText.isNotEmpty()) {
            onUpdateTyping(true)
            kotlinx.coroutines.delay(3000)
            onUpdateTyping(false)
        } else {
            onUpdateTyping(false)
        }
    }

    val isCurrentUserParticipantA = conversation.participantAId == currentUserId
    val otherName   = if (isCurrentUserParticipantA) conversation.participantBName  else conversation.participantAName
    val otherTeam   = if (isCurrentUserParticipantA) conversation.participantBTeamName else conversation.participantATeamName
    val otherTeamId = if (isCurrentUserParticipantA) conversation.participantBTeamId else conversation.participantATeamId
    val isOtherTyping = conversation.isOtherTyping(currentUserId)

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
                        // Online dot
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
                                    .background(SuccessGreen, CircleShape)
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
                                    Text("typing…", fontSize = 12.sp, color = GoldPrimary)
                                }
                            } else {
                                Text(
                                    text     = if (otherTeam.isNotBlank()) otherTeam else "Online",
                                    fontSize = 12.sp,
                                    color    = SuccessGreen,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

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
                LazyColumn(
                    modifier       = Modifier.fillMaxSize(),
                    state          = listState,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    itemsIndexed(conversation.messages, key = { _, msg -> msg.id }) { index, message ->
                        val isFromMe = message.senderId == currentUserId

                        val prevMessage = if (index > 0) conversation.messages[index - 1] else null
                        val nextMessage = if (index < conversation.messages.size - 1) conversation.messages[index + 1] else null

                        val showDateSeparator = prevMessage == null || !isSameDay(prevMessage.timestamp, message.timestamp)
                        val isFirstInGroup = prevMessage == null || prevMessage.senderId != message.senderId || showDateSeparator
                        val isLastInGroup  = nextMessage == null || nextMessage.senderId != message.senderId || !isSameDay(nextMessage.timestamp, message.timestamp)

                        if (showDateSeparator) DateSeparator(timestamp = message.timestamp)

                        MessageBubble(
                            message        = message,
                            isFromMe       = isFromMe,
                            isFirstInGroup = isFirstInGroup,
                            isLastInGroup  = isLastInGroup,
                            onViewTeamInfo = { onViewTeamInfo(otherTeamId, otherTeam) }
                        )
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
                        onClick        = { scope.launch { listState.animateScrollToItem(conversation.messages.size - 1) } },
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
                        .background(Color(0xFF0A1525).copy(alpha = 0.97f))
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
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
                            Spacer(Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(SurfaceOverlay)
                                    .border(1.dp, GlassBorder, CircleShape)
                                    .clickable { onSendVoice() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Outlined.Mic, null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                            }
                        }

                        Spacer(Modifier.width(8.dp))

                        // Text field
                        OutlinedTextField(
                            value         = messageText,
                            onValueChange = { messageText = it },
                            placeholder   = {
                                Text("Message…", color = TextTertiary, fontSize = 15.sp)
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
                                        onSendMessage(messageText)
                                        messageText = ""
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
                                        onSendMessage(messageText)
                                        messageText = ""
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Send, null,
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

    val myBubbleBrush = Brush.linearGradient(
        colors = listOf(BluePrimary, Color(0xFF1565C0)),
        start  = Offset(0f, 0f),
        end    = Offset(0f, Float.POSITIVE_INFINITY)
    )

    Box(
        modifier         = Modifier
            .fillMaxWidth()
            .padding(top = if (isFirstInGroup) 8.dp else 1.dp),
        contentAlignment = if (isFromMe) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Column(
            horizontalAlignment = if (isFromMe) Alignment.End else Alignment.Start,
            modifier            = Modifier.widthIn(max = 300.dp)
        ) {
            // Sender name for group messages
            if (!isFromMe && isFirstInGroup) {
                Text(
                    text     = message.senderName,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color    = BluePrimary,
                    modifier = Modifier.padding(start = 12.dp, bottom = 4.dp)
                )
            }

            // Bubble
            Box(
                modifier = Modifier
                    .clip(bubbleShape)
                    .background(
                        if (isFromMe) myBubbleBrush
                        else Brush.linearGradient(listOf(SurfaceCard, SurfaceCard))
                    )
                    .then(
                        if (!isFromMe) Modifier.border(1.dp, GlassBorder, bubbleShape)
                        else Modifier
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                when (message.type) {
                    MessageType.IMAGE -> ImageContent(url = message.imageUrl ?: "")
                    MessageType.VOICE -> VoiceContent(duration = message.voiceDuration ?: 0)
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
                            imageVector        = if (message.isRead) Icons.Default.DoneAll else Icons.Default.Done,
                            contentDescription = null,
                            tint               = if (message.isRead) AndroidTeal else TextTertiary,
                            modifier           = Modifier.size(12.dp)
                        )
                    }
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
        contentDescription = "Shared image",
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
private fun VoiceContent(duration: Int) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .background(White.copy(alpha = 0.18f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.PlayArrow, null, tint = White, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(10.dp))
        // Waveform
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.width(90.dp)) {
            val bars = listOf(6, 12, 18, 14, 20, 10, 16, 8, 22, 12, 18, 6, 14, 10, 8)
            bars.forEach { h ->
                Box(
                    Modifier
                        .width(3.dp)
                        .height(h.dp)
                        .clip(RoundedCornerShape(1.5.dp))
                        .background(White.copy(alpha = 0.55f))
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        Text("${duration}s", fontSize = 12.sp, color = White.copy(alpha = 0.8f))
    }
}

@Composable
private fun ApplyContent(content: String, onView: () -> Unit) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.EmojiEvents, null, tint = GoldPrimary, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Scrim Application", fontWeight = FontWeight.Bold, color = GoldPrimary, fontSize = 13.sp)
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
                "Review Application",
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
            .background(Color(0xFF0A1525).copy(alpha = 0.98f))
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
            Text("Chat opens in", fontSize = 13.sp, color = TextSecondary)
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

package com.mlbb.scrim.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mlbb.scrim.data.model.Conversation
import com.mlbb.scrim.data.model.Message
import com.mlbb.scrim.data.model.MessageType
import com.mlbb.scrim.data.model.Player
import com.mlbb.scrim.data.model.Team
import com.mlbb.scrim.ui.theme.*
import com.mlbb.scrim.ui.components.AnimatedEntrance
import com.mlbb.scrim.ui.components.GlassBackButton
import com.mlbb.scrim.ui.components.GradientButton
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ChatScreen(
    conversation: Conversation,
    currentUserId: String,
    currentUserName: String,
    onNavigateBack: () -> Unit,
    onSendMessage: (String) -> Unit,
    onViewTeamInfo: (teamId: String, teamName: String) -> Unit,
    isLoading: Boolean = false,
    teamInfo: Team? = null
) {
    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(conversation.messages.size) {
        if (conversation.messages.isNotEmpty()) {
            listState.animateScrollToItem(conversation.messages.size - 1)
        }
    }

    val isCurrentUserParticipantA = conversation.participantAId == currentUserId
    val otherName = if (isCurrentUserParticipantA) conversation.participantBName else conversation.participantAName
    val otherTeam = if (isCurrentUserParticipantA) conversation.participantBTeamName else conversation.participantATeamName
    val otherTeamId = if (isCurrentUserParticipantA) conversation.participantBTeamId else conversation.participantATeamId

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = heroGradientBrush())
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            AnimatedEntrance(delayMillis = 0) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .padding(top = 20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        GlassBackButton(onClick = onNavigateBack)

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = otherName,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = White
                                )
                            )
                            Text(
                                text = otherTeam,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = 12.sp,
                                    color = BluePrimary
                                )
                            )
                        }

                        Spacer(modifier = Modifier.width(44.dp))
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = conversation.scrimTitle,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 12.sp,
                            color = MidGray,
                            textAlign = TextAlign.Center
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Messages
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                state = listState,
                contentPadding = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(conversation.messages, key = { it.id }) { message ->
                    MessageBubble(
                        message = message,
                        isFromMe = message.senderId == currentUserId,
                        onViewTeamInfo = { onViewTeamInfo(otherTeamId, otherTeam) }
                    )
                }
            }

            // Team Info Card (if available)
            if (teamInfo != null) {
                TeamInfoCard(
                    team = teamInfo,
                    onViewTeamInfo = { onViewTeamInfo(teamInfo.id, teamInfo.name) }
                )
            }

            // Input
            AnimatedEntrance(delayMillis = 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                        .padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        placeholder = {
                            Text(
                                text = "Type a message...",
                                color = MidGray
                            )
                        },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldPrimary,
                            unfocusedBorderColor = White.copy(alpha = 0.2f),
                            focusedContainerColor = DarkNavy,
                            unfocusedContainerColor = DarkNavy,
                            focusedTextColor = White,
                            unfocusedTextColor = White,
                            cursorColor = GoldPrimary
                        ),
                        shape = RoundedCornerShape(20.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                if (messageText.isNotBlank()) {
                                    onSendMessage(messageText)
                                    messageText = ""
                                }
                            }
                        ),
                        maxLines = 3
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            if (messageText.isNotBlank()) {
                                onSendMessage(messageText)
                                messageText = ""
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                brush = Brush.horizontalGradient(colors = GoldGradient),
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Send",
                            tint = DarkBlue,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(
    message: Message,
    isFromMe: Boolean,
    onViewTeamInfo: () -> Unit
) {
    val backgroundColor = when (message.type) {
        MessageType.SYSTEM -> DarkSurface.copy(alpha = 0.6f)
        MessageType.APPLY -> DarkSurface.copy(alpha = 0.9f)
        else -> if (isFromMe) BluePrimary else DarkSurface
    }

    val textColor = when (message.type) {
        MessageType.SYSTEM -> MidGray
        MessageType.APPLY -> White
        else -> White
    }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = when {
            message.type == MessageType.SYSTEM -> Alignment.Center
            isFromMe -> Alignment.CenterEnd
            else -> Alignment.CenterStart
        }
    ) {
        when (message.type) {
            MessageType.SYSTEM -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(
                            color = backgroundColor,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = WarningOrange,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = message.content,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 12.sp,
                            color = textColor
                        )
                    )
                }
            }

            MessageType.APPLY -> {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .shadow(
                            elevation = 4.dp,
                            spotColor = GoldPrimary.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(16.dp)
                        ),
                    colors = CardDefaults.cardColors(containerColor = DarkNavy),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(
                                        brush = Brush.verticalGradient(colors = GoldGradient),
                                        shape = RoundedCornerShape(12.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Group,
                                    contentDescription = null,
                                    tint = DarkBlue,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Team Application",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = GoldPrimary
                                    )
                                )
                                Text(
                                    text = "From: ${message.senderName}",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = 12.sp,
                                        color = MidGray
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Divider(color = White.copy(alpha = 0.1f))

                        Spacer(modifier = Modifier.height(12.dp))

                        // Parse and display team info
                        val parts = message.content.split(" | ")
                        parts.forEach { part ->
                            val kv = part.split(": ", limit = 2)
                            if (kv.size == 2) {
                                Row(modifier = Modifier.padding(vertical = 2.dp)) {
                                    Text(
                                        text = "${kv[0]}: ",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontSize = 13.sp,
                                            color = MidGray
                                        )
                                    )
                                    Text(
                                        text = kv[1],
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = White
                                        )
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        GradientButton(
                            text = "View Team Players",
                            onClick = onViewTeamInfo,
                            gradient = GoldGradient,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            else -> {
                Column(horizontalAlignment = if (isFromMe) Alignment.End else Alignment.Start) {
                    if (!isFromMe) {
                        Text(
                            text = message.senderName,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.sp,
                                color = MidGray
                            ),
                            modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .background(
                                color = backgroundColor,
                                shape = RoundedCornerShape(
                                    topStart = if (isFromMe) 16.dp else 4.dp,
                                    topEnd = if (isFromMe) 4.dp else 16.dp,
                                    bottomStart = 16.dp,
                                    bottomEnd = 16.dp
                                )
                            )
                            .padding(12.dp)
                    ) {
                        Text(
                            text = message.content,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 14.sp,
                                color = textColor
                            )
                        )
                    }

                    Text(
                        text = formatChatTime(message.timestamp),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 10.sp,
                            color = MidGray
                        ),
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TeamInfoCard(
    team: Team,
    onViewTeamInfo: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .shadow(
                elevation = 6.dp,
                spotColor = BluePrimary.copy(alpha = 0.2f),
                shape = RoundedCornerShape(16.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = DarkNavy),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = team.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = White
                    )
                )
                Text(
                    text = "${team.players.size}/${team.maxPlayers} players",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 12.sp,
                        color = BluePrimary
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            team.players.forEach { player ->
                PlayerRow(player = player)
            }
        }
    }
}

@Composable
private fun PlayerRow(player: Player) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(
                    brush = Brush.verticalGradient(
                        colors = if (player.role == com.mlbb.scrim.data.model.PlayerRole.LEADER)
                            GoldGradient
                        else
                            listOf(DarkSurface, DarkNavy)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = player.name.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = White
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = player.name,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 13.sp,
                    color = White
                )
            )
        }

        val roleColor = when (player.role) {
            com.mlbb.scrim.data.model.PlayerRole.LEADER -> GoldPrimary
            com.mlbb.scrim.data.model.PlayerRole.CO_LEADER -> BluePrimary
            else -> MidGray
        }

        Text(
            text = player.role.name.replace("_", " "),
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 10.sp,
                color = roleColor,
                fontWeight = FontWeight.Medium
            ),
            modifier = Modifier
                .background(
                    color = roleColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                )
                .padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

private fun formatChatTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

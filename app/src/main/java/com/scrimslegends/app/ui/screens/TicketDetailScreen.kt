package com.scrimslegends.app.ui.screens

import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scrimslegends.app.data.service.SupportMessageDto
import com.scrimslegends.app.data.service.SupportTicketDto
import com.scrimslegends.app.ui.components.GlassBackButton
import com.scrimslegends.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TicketDetailScreen(
    ticket: SupportTicketDto,
    messages: List<SupportMessageDto>,
    onNavigateBack: () -> Unit,
    onSendMessage: (String) -> Unit
) {
    var replyText by remember { mutableStateOf("") }

    val statusColor = when (ticket.status.lowercase()) {
        "open" -> Color(0xFF4CAF50)
        "in progress" -> Color(0xFFFF9800)
        "waiting for user" -> Color(0xFFF44336)
        "resolved", "closed" -> Color(0xFF9E9E9E)
        else -> Color.Gray
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(20.dp)
                    .padding(top = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlassBackButton(onClick = onNavigateBack)
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Ticket #${ticket.id?.substring(0, 8)}",
                        style = iOSTitle3.copy(color = MaterialTheme.colorScheme.onSurface)
                    )
                    Text(
                        text = ticket.status,
                        color = statusColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Original Ticket Message
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                    .padding(16.dp)
            ) {
                Column {
                    Text(ticket.subject, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(ticket.message, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f), fontSize = 14.sp)
                }
            }

            // Chat Messages Timeline
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(messages, key = { it.id ?: "" }) { msg ->
                    MessageBubble(message = msg)
                }
            }

            // Input field
            if (ticket.status.lowercase() != "closed" && ticket.status.lowercase() != "resolved") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(16.dp)
                        .navigationBarsPadding(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = replyText,
                        onValueChange = { replyText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Type your reply...", color = Color.DarkGray) },
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            containerColor = MaterialTheme.colorScheme.background,
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    IconButton(
                        onClick = {
                            if (replyText.isNotBlank()) {
                                onSendMessage(replyText)
                                replyText = ""
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Filled.Send, contentDescription = "Send", tint = MaterialTheme.colorScheme.onSurface)
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(20.dp)
                        .navigationBarsPadding(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("This ticket has been ${ticket.status.lowercase()}.", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
                }
            }
        }
    }
}

@Composable
fun MessageBubble(message: SupportMessageDto) {
    val isUser = !message.isAdminReply
    val align = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    val bgColor = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
    val textColor = if (isUser) White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = align
    ) {
        Column(horizontalAlignment = if (isUser) Alignment.End else Alignment.Start) {
            if (!isUser) {
                Text("Support Agent", color = MaterialTheme.colorScheme.secondary, fontSize = 12.sp, modifier = Modifier.padding(bottom = 4.dp))
            }
            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isUser) 16.dp else 4.dp,
                            bottomEnd = if (isUser) 4.dp else 16.dp
                        )
                    )
                    .background(bgColor)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(text = message.message, color = textColor, fontSize = 15.sp)
            }
            Text(
                text = message.createdAt?.substring(11, 16) ?: "",
                color = Color.DarkGray,
                fontSize = 10.sp,
                modifier = Modifier.padding(top = 4.dp, start = 4.dp, end = 4.dp)
            )
        }
    }
}

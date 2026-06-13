package com.scrimslegends.app.ui.screens

import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scrimslegends.app.data.service.SupportTicketDto
import com.scrimslegends.app.ui.components.GlassBackButton
import com.scrimslegends.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportScreen(
    tickets: List<SupportTicketDto>,
    onNavigateBack: () -> Unit,
    onNavigateToTicket: (String) -> Unit,
    onCreateTicket: () -> Unit
) {
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
                    .padding(20.dp)
                    .padding(top = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlassBackButton(onClick = onNavigateBack)
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Support Tickets",
                    style = iOSTitle2.copy(color = MaterialTheme.colorScheme.onSurface)
                )
            }

            if (tickets.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No support tickets found.", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(tickets, key = { it.id ?: "" }) { ticket ->
                        SupportTicketItem(ticket = ticket, onClick = { ticket.id?.let { onNavigateToTicket(it) } })
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = onCreateTicket,
            containerColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Create Ticket", tint = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
fun SupportTicketItem(ticket: SupportTicketDto, onClick: () -> Unit) {
    val statusColor = when (ticket.status.lowercase()) {
        "open" -> Color(0xFF4CAF50)
        "in progress" -> Color(0xFFFF9800)
        "waiting for user" -> Color(0xFFF44336)
        "resolved", "closed" -> Color(0xFF9E9E9E)
        else -> Color.Gray
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = ticket.subject,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(statusColor.copy(alpha = 0.2f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = ticket.status,
                        color = statusColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = ticket.message,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                fontSize = 14.sp,
                maxLines = 2
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = ticket.createdAt?.substring(0, 10) ?: "",
                color = Color.DarkGray,
                fontSize = 12.sp
            )
        }
    }
}

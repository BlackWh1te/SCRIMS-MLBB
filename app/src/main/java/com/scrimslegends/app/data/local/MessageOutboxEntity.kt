package com.scrimslegends.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "message_outbox")
data class MessageOutboxEntity(
    @PrimaryKey
    val clientMessageId: String = UUID.randomUUID().toString(),
    val conversationId: String,
    val content: String,
    val isTeamMessage: Boolean,
    val isScrimMessage: Boolean,
    
    // Status can be: PENDING, RETRYING, FAILED_PERMANENT, SENT
    val status: String = "PENDING",
    val retryCount: Int = 0,
    val nextRetryAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
)

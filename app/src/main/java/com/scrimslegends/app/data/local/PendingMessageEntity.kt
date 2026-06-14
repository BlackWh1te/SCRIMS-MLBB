package com.scrimslegends.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.scrimslegends.app.data.model.DeliveryStatus
import com.scrimslegends.app.data.model.Message
import com.scrimslegends.app.data.model.MessageType

/**
 * Outbox table for messages that are queued locally and not yet acknowledged by the server.
 *
 * Guarantees at-least-once delivery via WorkManager + exponential backoff.
 */
@Entity(tableName = "pending_messages")
data class PendingMessageEntity(
    @PrimaryKey val clientMessageId: String,      // UUID generated locally; idempotency key
    val conversationId: String,
    val senderId: String,
    val senderName: String,
    val content: String,
    val type: String,                              // MessageType.name
    val imageUrl: String? = null,
    val voiceUrl: String? = null,
    val voiceDuration: Int? = null,
    val replyToId: String? = null,
    val replyToSnippet: String? = null,
    val replyToSenderName: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val status: String = DeliveryStatus.PENDING.name,
    val retryCount: Int = 0,
    val nextRetryAt: Long = 0L,
    val errorReason: String? = null,
    val failedAt: Long? = null
) {
    fun toDomainModel(): com.scrimslegends.app.data.model.MessageWithDelivery {
        return com.scrimslegends.app.data.model.MessageWithDelivery(
            message = Message(
                id = clientMessageId,
                conversationId = conversationId,
                senderId = senderId,
                senderName = senderName,
                content = content,
                timestamp = createdAt,
                isRead = true,
                type = MessageType.valueOf(type),
                imageUrl = imageUrl,
                voiceUrl = voiceUrl,
                voiceDuration = voiceDuration,
                replyToId = replyToId,
                replyToSnippet = replyToSnippet,
                replyToSenderName = replyToSenderName
            ),
            status = DeliveryStatus.valueOf(status),
            clientMessageId = clientMessageId,
            retryCount = retryCount,
            failedAt = failedAt,
            errorReason = errorReason
        )
    }
}

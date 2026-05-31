package com.scrimslegends.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.scrimslegends.app.data.model.Message
import com.scrimslegends.app.data.model.MessageType

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val matchId: String? = null,
    val senderId: String,
    val senderTeamId: String? = null,
    val senderName: String,
    val content: String,
    val timestamp: Long,
    val isRead: Boolean,
    val readAt: Long?,
    val type: String,
    val imageUrl: String?,
    val voiceUrl: String?,
    val voiceDuration: Int?,
    val deliveryStatus: String = "SENT",
    val clientMessageId: String? = null,
    // ── Reply support ──
    val replyToId: String? = null,
    val replyToSnippet: String? = null,
    val replyToSenderName: String? = null,
    // ── Soft delete ──
    val isDeleted: Boolean = false
) {
    fun toDomainModel(): Message {
        val messageType = try { MessageType.valueOf(type) } catch (_: Exception) { MessageType.TEXT }
        return Message(
            id = id,
            conversationId = conversationId,
            matchId = matchId,
            senderId = senderId,
            senderTeamId = senderTeamId,
            senderName = senderName,
            content = content,
            timestamp = timestamp,
            isRead = isRead,
            readAt = readAt,
            type = messageType,
            imageUrl = imageUrl,
            voiceUrl = voiceUrl,
            voiceDuration = voiceDuration,
            replyToId = replyToId,
            replyToSnippet = replyToSnippet,
            replyToSenderName = replyToSenderName,
            isDeleted = isDeleted
        )
    }
}

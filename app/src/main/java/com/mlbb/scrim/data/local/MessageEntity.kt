package com.mlbb.scrim.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.mlbb.scrim.data.model.Message
import com.mlbb.scrim.data.model.MessageType

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
    val voiceDuration: Int?
) {
    fun toDomainModel() = Message(
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
        type = MessageType.valueOf(type),
        imageUrl = imageUrl,
        voiceUrl = voiceUrl,
        voiceDuration = voiceDuration
    )
}

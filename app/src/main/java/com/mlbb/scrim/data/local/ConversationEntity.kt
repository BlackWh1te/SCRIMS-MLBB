package com.mlbb.scrim.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.mlbb.scrim.data.model.Conversation

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey val id: String,
    val scrimId: String?,
    val scrimTitle: String?,
    val participantAId: String,
    val participantAName: String,
    val participantATeamId: String?,
    val participantATeamName: String,
    val participantBId: String,
    val participantBName: String,
    val participantBTeamId: String?,
    val participantBTeamName: String,
    val lastMessage: String,
    val lastMessageTime: Long,
    val chatOpensAt: Long,
    val isParticipantATyping: Boolean,
    val isParticipantBTyping: Boolean
) {
    fun toDomainModel() = Conversation(
        id = id,
        scrimId = scrimId ?: "",
        scrimTitle = scrimTitle ?: "",
        participantAId = participantAId,
        participantAName = participantAName,
        participantATeamId = participantATeamId ?: "",
        participantATeamName = participantATeamName,
        participantBId = participantBId,
        participantBName = participantBName,
        participantBTeamId = participantBTeamId ?: "",
        participantBTeamName = participantBTeamName,
        lastMessage = lastMessage,
        lastMessageTime = lastMessageTime,
        chatOpensAt = chatOpensAt,
        isParticipantATyping = isParticipantATyping,
        isParticipantBTyping = isParticipantBTyping,
        messages = emptyList() // Loaded separately
    )
}

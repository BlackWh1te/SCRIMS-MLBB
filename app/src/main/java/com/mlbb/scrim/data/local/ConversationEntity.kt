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
    val participantAAvatarUrl: String? = null,
    val participantBId: String,
    val participantBName: String,
    val participantBTeamId: String?,
    val participantBTeamName: String,
    val participantBAvatarUrl: String? = null,
    val lastMessage: String,
    val lastMessageTime: Long,
    val chatOpensAt: Long,
    val isParticipantATyping: Boolean,
    val isParticipantBTyping: Boolean,
    // ── Tournament match chat ──
    val tournamentMatchId: String? = null,
    val participantCount: Int = 2,
    val isGroupChat: Boolean = false,
    val unreadCount: Int = 0,
    // ── Team group chat ──
    val teamId: String? = null,
    val isTeamChat: Boolean = false,
    val isPinned: Boolean = false,
    val groupName: String? = null
) {
    fun toDomainModel() = Conversation(
        id = id,
        scrimId = scrimId ?: "",
        scrimTitle = scrimTitle ?: "",
        participantAId = participantAId,
        participantAName = participantAName,
        participantATeamId = participantATeamId ?: "",
        participantATeamName = participantATeamName,
        participantAAvatarUrl = participantAAvatarUrl,
        participantBId = participantBId,
        participantBName = participantBName,
        participantBTeamId = participantBTeamId ?: "",
        participantBTeamName = participantBTeamName,
        participantBAvatarUrl = participantBAvatarUrl,
        lastMessage = lastMessage,
        lastMessageTime = lastMessageTime,
        chatOpensAt = chatOpensAt,
        isParticipantATyping = isParticipantATyping,
        isParticipantBTyping = isParticipantBTyping,
        messages = emptyList(), // Loaded separately
        // ── Tournament match chat ──
        tournamentMatchId = tournamentMatchId,
        participantCount = participantCount,
        isGroupChat = isGroupChat,
        unreadCount = unreadCount,
        // ── Team group chat ──
        teamId = teamId,
        isTeamChat = isTeamChat,
        isPinned = isPinned,
        groupName = groupName ?: ""
    )
}

package com.mlbb.scrim.data.model

data class Message(
    val id: String = "",
    val conversationId: String = "",
    val matchId: String? = null,
    val senderId: String = "",
    val senderTeamId: String? = null,
    val senderName: String = "",
    val content: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val readAt: Long? = null,
    val type: MessageType = MessageType.TEXT,
    val imageUrl: String? = null,
    val voiceUrl: String? = null,
    val voiceDuration: Int? = null
)

data class Conversation(
    val id: String = "",
    val scrimId: String = "",
    val scrimTitle: String = "",
    val participantAId: String = "",
    val participantAName: String = "",
    val participantATeamId: String = "",
    val participantATeamName: String = "",
    val participantBId: String = "",
    val participantBName: String = "",
    val participantBTeamId: String = "",
    val participantBTeamName: String = "",
    val lastMessage: String = "",
    val lastMessageTime: Long = System.currentTimeMillis(),
    val unreadCount: Int = 0,
    val messages: List<Message> = emptyList(),
    // ── Chat gating fields ──
    val chatOpensAt: Long = System.currentTimeMillis(),
    val isChatLocked: Boolean = true,      // true until chatOpensAt
    val adminVisible: Boolean = true,       // Always visible to admins for review
    // ── Real-time Status ──
    val isParticipantATyping: Boolean = false,
    val isParticipantBTyping: Boolean = false
) {
    val timeUntilChatOpens: Long
        get() = (chatOpensAt - System.currentTimeMillis()).coerceAtLeast(0)

    val isChatOpenNow: Boolean
        get() = System.currentTimeMillis() >= chatOpensAt

    fun isOtherTyping(currentUserId: String): Boolean {
        return if (currentUserId == participantAId) isParticipantBTyping else isParticipantATyping
    }
}

enum class MessageType {
    TEXT,
    SYSTEM,
    APPLY,
    IMAGE,
    VOICE
}

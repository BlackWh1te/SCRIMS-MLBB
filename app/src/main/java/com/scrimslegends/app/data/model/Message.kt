package com.scrimslegends.app.data.model

import androidx.compose.runtime.Stable

@Stable
data class Message(
    val id: String = "",
    val conversationId: String = "",
    val matchId: String? = null,
    val senderId: String = "",
    val senderTeamId: String? = null,
    val senderName: String = "",
    val senderAvatarUrl: String? = null,
    val content: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val readAt: Long? = null,
    val type: MessageType = MessageType.TEXT,
    val imageUrl: String? = null,
    val voiceUrl: String? = null,
    val voiceDuration: Int? = null,
    // ── Reply support ──
    val replyToId: String? = null,
    val replyToSnippet: String? = null,   // first 80 chars of replied message
    val replyToSenderName: String? = null,
    // ── Soft delete ──
    val isDeleted: Boolean = false
)

@Stable
data class Conversation(
    val id: String = "",
    val scrimId: String = "",
    val scrimTitle: String = "",
    val participantAId: String = "",
    val participantAName: String = "",
    val participantATeamId: String = "",
    val participantATeamName: String = "",
    val participantAAvatarUrl: String? = null,
    val participantALastSeen: Long? = null,
    val participantBId: String = "",
    val participantBName: String = "",
    val participantBTeamId: String = "",
    val participantBTeamName: String = "",
    val participantBAvatarUrl: String? = null,
    val participantBLastSeen: Long? = null,
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
    val isParticipantBTyping: Boolean = false,
    // ── Tournament match chat ──
    val tournamentMatchId: String? = null,  // Set for tournament match group chats
    val participantCount: Int = 2,          // 2 for scrim chat, 3+ for tournament match chat
    val isGroupChat: Boolean = false,       // true for tournament match chats
    // ── Team group chat ──
    val teamId: String? = null,             // Set for team group chats
    val isTeamChat: Boolean = false,        // true = this is the team's group chat
    val isPinned: Boolean = false,          // pinned at top of message list
    val groupName: String = "",             // display name for the group
    // ── New-messages tracking ──
    val lastSeenMessageId: String? = null,   // last message ID the user saw (for "new messages" separator)
    // ── Clear History ──
    val historyClearedAt: Long = 0L
) {
    val timeUntilChatOpens: Long
        get() = (chatOpensAt - com.scrimslegends.app.util.ServerTimeProvider.getSynchronizedTime()).coerceAtLeast(0)

    val isChatOpenNow: Boolean
        get() = com.scrimslegends.app.util.ServerTimeProvider.getSynchronizedTime() >= chatOpensAt

    fun isOtherTyping(currentUserId: String): Boolean {
        return if (currentUserId == participantAId) isParticipantBTyping else isParticipantATyping
    }

    fun otherLastSeen(currentUserId: String): Long? {
        return if (currentUserId == participantAId) participantBLastSeen else participantALastSeen
    }

    fun isOtherOnline(currentUserId: String, now: Long = System.currentTimeMillis()): Boolean {
        val lastSeen = otherLastSeen(currentUserId) ?: return false
        return now - lastSeen <= ONLINE_WINDOW_MS
    }

    companion object {
        private const val ONLINE_WINDOW_MS = 5 * 60 * 1000L
    }
}

enum class MessageType {
    TEXT,
    SYSTEM,
    APPLY,
    IMAGE,
    VOICE
}

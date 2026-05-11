package com.mlbb.scrim.data.model

enum class NotificationType {
    SCRIM_INVITE,
    MATCH_RESULT,
    TEAM_INVITE,
    MESSAGE,
    SYSTEM,
    XP_GAIN,
    TIER_UP
}

data class Notification(
    val id: String = "",
    val type: NotificationType = NotificationType.SYSTEM,
    val title: String = "",
    val message: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val actionId: String = "", // scrimId, teamId, etc.
    val imageUrl: String? = null
) {
    val icon: String
        get() = when (type) {
            NotificationType.SCRIM_INVITE -> "sports_esports"
            NotificationType.MATCH_RESULT -> "emoji_events"
            NotificationType.TEAM_INVITE -> "group"
            NotificationType.MESSAGE -> "chat"
            NotificationType.SYSTEM -> "info"
            NotificationType.XP_GAIN -> "trending_up"
            NotificationType.TIER_UP -> "star"
        }
}

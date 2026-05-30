package com.scrimslegends.app.data.model

data class TeamInvite(
    val id: String = "",
    val teamId: String = "",
    val teamName: String = "",
    val invitedBy: String = "",         // Player who sent the invite
    val invitedByName: String = "",
    val invitedUserId: String = "",      // Player who received the invite
    val invitedUserName: String = "",
    val status: InviteStatus = InviteStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis(),
    val respondedAt: Long? = null
)

enum class InviteStatus {
    PENDING,    // Waiting for player to respond
    ACCEPTED,   // Player joined the team
    DECLINED,   // Player declined the invite
    EXPIRED,    // Invite expired (e.g., team full or too old)
    CANCELLED   // Inviter cancelled the invite
}

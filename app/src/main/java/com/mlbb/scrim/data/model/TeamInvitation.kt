package com.mlbb.scrim.data.model

import java.util.UUID

data class TeamInvitation(
    val id: UUID,
    val teamId: UUID,
    val teamName: String,
    val invitedUserId: UUID,
    val invitedUserName: String,
    val invitedBy: UUID,
    val invitedByName: String,
    val status: String,
    val createdAt: String
)

enum class InvitationStatus(val displayName: String) {
    PENDING("Pending"),
    ACCEPTED("Accepted"),
    REJECTED("Rejected")
}
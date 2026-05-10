package com.mlbb.scrim.data.model

import java.util.UUID

data class Match(
    val id: UUID,
    val teamAId: UUID,
    val teamAName: String,
    val teamBId: UUID,
    val teamBName: String,
    val scheduledDate: String,
    val scheduledTime: String,
    val roomId: String? = null,
    val roomPassword: String? = null,
    val status: String,
    val createdAt: String
)

enum class MatchStatus(val displayName: String) {
    SCHEDULED("Scheduled"),
    IN_PROGRESS("In Progress"),
    WAITING_SCREENSHOTS("Waiting Screenshots"),
    COMPLETED("Completed"),
    DISPUTED("Disputed")
}

data class Message(
    val id: UUID,
    val matchId: UUID,
    val senderId: UUID,
    val senderName: String,
    val senderTeamId: UUID,
    val senderTeamName: String,
    val content: String,
    val createdAt: String
)

data class MatchResult(
    val id: UUID,
    val matchId: UUID,
    val teamAScreenshotUrl: String? = null,
    val teamBScreenshotUrl: String? = null,
    val winnerTeamId: UUID? = null,
    val winnerTeamName: String? = null,
    val adminVerified: Boolean = false,
    val verifiedBy: UUID? = null,
    val verifiedByName: String? = null,
    val verificationNotes: String? = null,
    val xpAwarded: Boolean = false,
    val createdAt: String
)
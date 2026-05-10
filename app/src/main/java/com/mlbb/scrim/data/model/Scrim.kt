package com.mlbb.scrim.data.model

import java.util.UUID

data class Scrim(
    val id: UUID,
    val teamId: UUID,
    val teamName: String,
    val teamTier: String,
    val teamDivision: Int,
    val scheduledDate: String,
    val scheduledTime: String,
    val status: String,
    val description: String? = null,
    val createdAt: String
)

enum class ScrimStatus(val displayName: String) {
    OPEN("Open"),
    PENDING("Pending"),
    MATCHED("Matched"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled")
}

data class ScrimApplication(
    val id: UUID,
    val scrimId: UUID,
    val applicantTeamId: UUID,
    val applicantTeamName: String,
    val status: String,
    val appliedAt: String
)

enum class ApplicationStatus(val displayName: String) {
    PENDING("Pending"),
    ACCEPTED("Accepted"),
    REJECTED("Rejected")
}
package com.mlbb.scrim.data.model

data class MatchResult(
    val id: String = "",
    val scrimId: String = "",
    val teamAId: String = "",
    val teamAName: String = "",
    val teamBId: String = "",
    val teamBName: String = "",
    val teamAReport: TeamReport? = null,
    val teamBReport: TeamReport? = null,
    val screenshotUrl: String? = null,
    val verificationStatus: VerificationStatus = VerificationStatus.PENDING,
    val confirmedWinnerId: String? = null,
    val adminNotes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val resolvedAt: Long? = null
) {
    val isDisputed: Boolean
        get() = teamAReport != null && teamBReport != null &&
                teamAReport.reportedWinnerId != teamBReport.reportedWinnerId

    val isConfirmed: Boolean
        get() = verificationStatus == VerificationStatus.CONFIRMED

    val bothTeamsReported: Boolean
        get() = teamAReport != null && teamBReport != null

    val pendingReporterTeamId: String?
        get() = when {
            teamAReport == null -> teamAId
            teamBReport == null -> teamBId
            else -> null
        }
}

data class TeamReport(
    val reporterId: String = "",
    val reporterName: String = "",
    val reportedWinnerId: String = "",
    val reportedAt: Long = System.currentTimeMillis(),
    val notes: String? = null
)

enum class VerificationStatus {
    PENDING,
    CONFIRMED,
    DISPUTED,
    ADMIN_REVIEW
}

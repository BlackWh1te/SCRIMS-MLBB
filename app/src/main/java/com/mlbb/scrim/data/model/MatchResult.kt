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
    val resolvedAt: Long? = null,
    // ── Team Rosters (fetched from scrim) ──
    val teamARoster: List<RosterPlayerInfo> = emptyList(),
    val teamBRoster: List<RosterPlayerInfo> = emptyList(),
    // ── Admin review fields ──
    val adminVerdict: AdminVerdict? = null,
    val punishedTeamId: String? = null,
    val punishmentDurationHours: Int = 0,
    val reviewedByAdminId: String? = null,
    val reviewedAt: Long? = null,
    val noShowTeamId: String? = null,     // Team that didn't show up
    val matchActuallyPlayed: Boolean = false,
    // ── Match type: scrim or tournament ──
    val matchType: MatchType = MatchType.SCRIM,
    val tournamentTitle: String? = null,
    val roundNumber: Int? = null
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
            teamAReport == null -> teamAId.takeIf { it.isNotBlank() }
            teamBReport == null -> teamBId.takeIf { it.isNotBlank() }
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
    ADMIN_REVIEW,
    AUTO_CANCELLED,     // No result submitted within deadline
    ADMIN_RESOLVED      // Admin reviewed and gave verdict
}

enum class AdminVerdict {
    TEAM_A_NO_SHOW,     // Team A didn't show up
    TEAM_B_NO_SHOW,     // Team B didn't show up
    BOTH_NO_SHOW,       // Neither team showed up
    MATCH_PLAYED,       // Match actually happened
    TECHNICAL_ISSUE,    // Server/client issues prevented match
    INVALID_REPORT,     // False report / trolling
    UNDER_REVIEW        // Still investigating
}

enum class MatchType {
    SCRIM,
    TOURNAMENT
}

/** Player info from scrim roster for match result display */
data class RosterPlayerInfo(
    val playerId: String = "",
    val playerName: String = "",
    val role: String = "",
    val isActive: Boolean = false,  // true = played, false = substitute
    val isWinner: Boolean = false,
    val pointsChange: Int = 0
)

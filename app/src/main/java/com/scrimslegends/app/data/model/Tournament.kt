package com.scrimslegends.app.data.model

import com.google.gson.annotations.SerializedName

// ── Tournament Status ──────────────────────────────────────────

enum class TournamentStatus(val value: String) {
    DRAFT("draft"),
    REGISTRATION("registration"),
    CHECK_IN("check_in"),
    IN_PROGRESS("in_progress"),
    COMPLETED("completed"),
    CANCELLED("cancelled");

    companion object {
        fun fromValue(value: String) = entries.find { it.value == value } ?: DRAFT
    }
}

enum class PrizeType(val value: String) {
    DIAMONDS("diamonds"),
    SKIN("skin"),
    STAR_PASS("star_pass"),
    OTHER("other");

    companion object {
        fun fromValue(value: String) = entries.find { it.value == value } ?: OTHER
    }
}

enum class RequirementType(val value: String) {
    TELEGRAM_SUBSCRIBE("telegram_subscribe"),
    YOUTUBE_SUBSCRIBE("youtube_subscribe"),
    CUSTOM("custom");

    companion object {
        fun fromValue(value: String) = entries.find { it.value == value } ?: CUSTOM
    }
}

enum class TournamentApplicationStatus(val value: String) {
    PENDING("pending"),
    ACCEPTED("accepted"),
    REJECTED("rejected"),
    BLOCKED("blocked");

    companion object {
        fun fromValue(value: String) = entries.find { it.value == value } ?: PENDING
    }
}

enum class MatchStatus(val value: String) {
    SCHEDULED("scheduled"),
    IN_PROGRESS("in_progress"),
    COMPLETED("completed"),
    DISPUTED("disputed"),
    CANCELLED("cancelled"),
    BYE("bye");

    companion object {
        fun fromValue(value: String) = entries.find { it.value == value } ?: SCHEDULED
    }
}

// ── Tournament ──────────────────────────────────────────────────

data class Tournament(
    val id: String = "",
    val hostUserId: String = "",
    val hostUsername: String = "",
    val title: String = "",
    val description: String = "",
    val logoUrl: String? = null,
    val prizeType: PrizeType = PrizeType.OTHER,
    val prizeDescription: String? = null,
    val maxTeams: Int = 16,
    val minTeamSize: Int = 5,
    val bestOf: Int = 1,
    val region: String = "EU",
    val skillLevel: String = "ALL",
    val swissRounds: Int? = null,
    val currentRound: Int = 0,
    val status: TournamentStatus = TournamentStatus.DRAFT,
    val registrationDeadline: Long = 0L,
    val checkInDeadline: Long = 0L,
    val isLiveStreamEnabled: Boolean = false,
    val isFlagged: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    // Joined data
    val requirements: List<TournamentRequirement> = emptyList(),
    val teamCount: Int = 0,
    val hostTrustScore: Double = 0.0,
) {
    val isOpen: Boolean get() = status == TournamentStatus.REGISTRATION
    val isLive: Boolean get() = status == TournamentStatus.IN_PROGRESS
    val isCheckIn: Boolean get() = status == TournamentStatus.CHECK_IN
    val isCompleted: Boolean get() = status == TournamentStatus.COMPLETED
    val isCancelled: Boolean get() = status == TournamentStatus.CANCELLED

    val registrationTimeRemaining: Long
        get() = (registrationDeadline - System.currentTimeMillis()).coerceAtLeast(0)

    val prizeDisplay: String
        get() = prizeDescription ?: prizeType.value.replace("_", " ")
}

// ── Tournament Requirement ───────────────────────────────────────

data class TournamentRequirement(
    val id: String = "",
    val tournamentId: String = "",
    val type: RequirementType = RequirementType.CUSTOM,
    val label: String = "",
    val url: String? = null,
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

// ── Tournament Application ─────────────────────────────────────

data class TournamentApplication(
    val id: String = "",
    val tournamentId: String = "",
    val teamId: String = "",
    val teamName: String = "",
    val status: TournamentApplicationStatus = TournamentApplicationStatus.PENDING,
    val rejectionReason: String? = null,
    val attemptNumber: Int = 1,
    val appliedAt: Long = System.currentTimeMillis(),
    val reviewedAt: Long? = null
)

// ── Tournament Team (with Swiss scores) ─────────────────────────

data class TournamentTeam(
    val id: String = "",
    val tournamentId: String = "",
    val teamId: String = "",
    val teamName: String = "",
    val checkedIn: Boolean = false,
    val checkedInAt: Long? = null,
    val swissWins: Int = 0,
    val swissLosses: Int = 0,
    val swissDraws: Int = 0,
    val swissPoints: Int = 0,
    val buchholzScore: Double = 0.0,
    val sonnebornBerger: Double = 0.0,
    val finalPlacement: Int? = null,
    val isDisqualified: Boolean = false,
    val disqualificationReason: String? = null
)

// ── Swiss Match ─────────────────────────────────────────────────

data class TournamentSwissMatch(
    val id: String = "",
    val tournamentId: String = "",
    val roundNumber: Int = 0,
    val matchNumber: Int = 0,
    val teamAId: String = "",
    val teamAName: String = "",
    val teamBId: String? = null,
    val teamBName: String? = null,
    val conversationId: String? = null,
    val status: MatchStatus = MatchStatus.SCHEDULED,
    val scheduledAt: Long? = null,
    val noShowGracePeriodMin: Int = 15,
    val matchAutoCompleteAt: Long? = null,
    val winnerTeamId: String? = null,
    val winnerTeamName: String? = null,
    val isDraw: Boolean = false,
    val gameAScore: Int = 0,
    val gameBScore: Int = 0,
    val resultSubmittedAt: Long? = null,
    val disputeReason: String? = null,
    val liveStreamUrl: String? = null,
    val isBye: Boolean = false,
    val isMyMatch: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    val scheduledTimeRemaining: Long
        get() = (scheduledAt ?: 0L).let { (it - System.currentTimeMillis()).coerceAtLeast(0) }
}

// ── Tournament Match Roster ─────────────────────────────────────

data class TournamentMatchRoster(
    val id: String = "",
    val matchId: String = "",
    val teamId: String = "",
    val userId: String = "",
    val username: String = "",
    val gameNumber: Int = 1,
    val isActive: Boolean = true,
    val assignedBy: String = "",
    val assignedAt: Long = System.currentTimeMillis()
)

// ── Tournament Host Request ─────────────────────────────────────

data class TournamentHostRequest(
    val id: String = "",
    val userId: String = "",
    val motivation: String = "",
    val experience: String? = null,
    val telegramChannel: String? = null,
    val socialLinks: List<String> = emptyList(),
    val status: String = "pending",
    val adminNotes: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

// ── Room Secret (participant-only) ──────────────────────────────

data class TournamentMatchRoomSecret(
    val id: String = "",
    val matchId: String = "",
    val roomId: String = "",
    val roomPassword: String? = null,
    val droppedBy: String = "",
    val droppedAt: Long = System.currentTimeMillis()
)

// ── Tournament Player Stats ───────────────────────────────────

data class TournamentPlayerStats(
    val id: String = "",
    val tournamentId: String = "",
    val userId: String = "",
    val teamId: String = "",
    val placement: Int? = null,
    val matchesWon: Int = 0,
    val matchesLost: Int = 0,
    val matchesDrawn: Int = 0,
    val pointsEarned: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
) {
    val totalMatches: Int get() = matchesWon + matchesLost + matchesDrawn
    val winRate: Float get() = if (totalMatches > 0) (matchesWon * 100f / totalMatches) else 0f
}

// ── Host Account ────────────────────────────────────────────────

data class TournamentHostAccount(
    val id: String = "",
    val tournamentId: String = "",
    val hostUserId: String = "",
    val authUserId: String? = null,
    val email: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

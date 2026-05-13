package com.mlbb.scrim.data.model

data class Scrim(
    val id: String = "",
    val teamId: String = "",
    val teamName: String = "",
    val teamLeader: String = "",
    val gameMode: GameMode = GameMode.RANKED,
    val region: Region = Region.EU,
    val skillLevel: SkillLevel = SkillLevel.ALL,
    val bestOf: BestOf = BestOf.BO1,
    val scheduledTime: Long = System.currentTimeMillis(),
    val maxPlayers: Int = 10,
    val currentPlayers: Int = 0,
    val status: ScrimStatus = ScrimStatus.OPEN,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    // ── Team-vs-team fields ──
    val opponentTeamId: String? = null,
    val opponentTeamName: String? = null,
    val opponentTeamLeader: String? = null,
    val applications: List<ScrimApplication> = emptyList(),
    val conversationId: String? = null,
    val resultSubmittedAt: Long? = null,
    val cancellationReason: String? = null,
    val cancelledBy: String? = null,
    // ── Scrim rosters ──
    val teamARoster: List<ScrimRosterEntry> = emptyList(),
    val teamBRoster: List<ScrimRosterEntry> = emptyList(),
    // ── Ready flow ──
    val teamAReady: Boolean = false,
    val teamBReady: Boolean = false,
    val teamAReadyAt: Long? = null,
    val teamBReadyAt: Long? = null,
    // ── Screenshot flow ──
    val teamAScreenshotUrl: String? = null,
    val teamBScreenshotUrl: String? = null,
    val teamAScreenshotUploadedAt: Long? = null,
    val teamBScreenshotUploadedAt: Long? = null,
    // ── Winner ──
    val winnerTeamId: String? = null
) {
    /** Ready buttons appear at match start time */
    val isReadyPhase: Boolean
        get() = status == ScrimStatus.READY_CHECK &&
                System.currentTimeMillis() >= scheduledTime

    /** Both captains ready → can attach screenshots */
    val bothReady: Boolean
        get() = teamAReady && teamBReady

    /** Can complete scrim only after screenshot uploaded */
    val canCompleteScrim: Boolean
        get() = bothReady && (teamAScreenshotUrl != null || teamBScreenshotUrl != null)

    /** Chat opens 2 hours before scheduled time */
    val chatOpensAt: Long
        get() = scheduledTime - (2 * 60 * 60 * 1000)

    /** Result must be submitted within 1 hour after scheduled time */
    val resultDeadline: Long
        get() = scheduledTime + (60 * 60 * 1000)

    /** Extended deadline: 2 hours after scheduled time for auto-cancel */
    val autoCancelDeadline: Long
        get() = scheduledTime + (2 * 60 * 60 * 1000)

    val isChatOpen: Boolean
        get() = System.currentTimeMillis() >= chatOpensAt

    val isResultOverdue: Boolean
        get() = System.currentTimeMillis() > resultDeadline

    val isAutoCancelOverdue: Boolean
        get() = System.currentTimeMillis() > autoCancelDeadline

    val timeUntilChatOpens: Long
        get() = (chatOpensAt - System.currentTimeMillis()).coerceAtLeast(0)

    /** Active roster for Team A (players who gain/lose pts) */
    val teamAActiveRoster: List<ScrimRosterEntry>
        get() = teamARoster.filter { it.isActive }

    /** Active roster for Team B (players who gain/lose pts) */
    val teamBActiveRoster: List<ScrimRosterEntry>
        get() = teamBRoster.filter { it.isActive }

    /** Substitute roster for Team A (no pts change) */
    val teamASubstitutes: List<ScrimRosterEntry>
        get() = teamARoster.filter { !it.isActive }

    /** Substitute roster for Team B (no pts change) */
    val teamBSubstitutes: List<ScrimRosterEntry>
        get() = teamBRoster.filter { !it.isActive }
}

/** Entry in a scrim roster — captain assigns players as active or substitute */
data class ScrimRosterEntry(
    val playerId: String = "",
    val playerName: String = "",
    val teamId: String = "",
    val isActive: Boolean = false  // true = playing (pts affected), false = substitute (no pts)
)

enum class BestOf(val games: Int, val displayName: String) {
    BO1(1, "Best of 1"),
    BO2(2, "Best of 2"),
    BO3(3, "Best of 3"),
    BO4(4, "Best of 4"),
    BO5(5, "Best of 5");

    companion object {
        fun fromGames(games: Int): BestOf = values().find { it.games == games } ?: BO1
    }
}

enum class GameMode {
    RANKED,
    CUSTOM,
    TOURNAMENT,
    CASUAL
}

enum class Region(val displayName: String, val utcOffset: String) {
    UTC("UTC", "UTC+0"),
    EU("Europe", "UTC+1"),
    MCK("Moscow", "UTC+3"),
    SA("South Asia", "UTC+5:30"),
    ASIA("Southeast Asia", "UTC+8"),
    KRD("Korea", "UTC+9"),
    NA("North America", "UTC-5"),
    EKB("East KB", "UTC+9");

    companion object {
        fun fromDisplayName(name: String): Region = values().find { it.displayName == name } ?: UTC
    }
}

enum class SkillLevel {
    ALL,
    BEGINNER,
    INTERMEDIATE,
    ADVANCED,
    PRO
}

enum class ScrimStatus {
    OPEN,           // Posted, waiting for opponent
    FILLED,         // Opponent accepted, chat open
    READY_CHECK,    // Match time reached, waiting for both captains to ready
    IN_PROGRESS,    // Both ready, match being played
    COMPLETED,      // Match finished, result submitted
    CANCELLED       // Cancelled by host or auto-cancelled
}

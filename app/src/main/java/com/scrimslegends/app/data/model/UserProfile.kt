package com.scrimslegends.app.data.model

data class UserProfile(
    val id: String = "",
    val username: String = "",
    val email: String = "",
    val shortId: String = "",
    val inGameId: String = "",
    val avatarUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val xp: Int = 0,
    val pts: Int = 0,
    val totalMatches: Int = 0,
    val wins: Int = 0,
    val losses: Int = 0,
    val currentTier: RankTier = RankTier.WARRIOR,
    val emailVerified: Boolean = false,
    val isBanned: Boolean = false,
    val banReason: String? = null,
    val bannedAt: String? = null,
    val mainHeroes: List<String> = emptyList(),
    val role: String = "",
    val bio: String = "",
    // Tournament host fields
    val telegramUsername: String? = null,
    val isTournamentHost: Boolean = false,
    val hostTrustScore: Float = 5.0f,
    val tournamentsHosted: Int = 0,
    val tournamentsCompleted: Int = 0,
    val tournamentsCancelled: Int = 0,
    // Freeze/penalty fields
    val frozenUntil: String? = null,
) {
    val winRate: String
        get() = if (totalMatches > 0) "${(wins * 100 / totalMatches)}%" else "0%"

    val winRateFloat: Float
        get() = if (totalMatches > 0) (wins * 100f / totalMatches) else 0f

    val xpToNext: Int
        get() = RankTier.xpToNextTier(xp)

    val xpProgress: Float
        get() = RankTier.xpProgressInTier(xp)

    val nextTierName: String
        get() = RankTier.nextTier(currentTier)?.displayName ?: "Max"

    val ptsDisplay: String
        get() = if (pts >= 0) "+$pts" else "$pts"

    val isFrozen: Boolean
        get() {
            val until = frozenUntil ?: return false
            return try {
                java.time.Instant.parse(until).isAfter(java.time.Instant.now())
            } catch (_: Exception) { false }
        }
}

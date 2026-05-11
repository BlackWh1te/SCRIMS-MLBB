package com.mlbb.scrim.data.model

data class UserProfile(
    val id: String = "",
    val username: String = "",
    val email: String = "",
    val inGameId: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val xp: Int = 0,
    val totalMatches: Int = 0,
    val wins: Int = 0,
    val losses: Int = 0,
    val currentTier: RankTier = RankTier.BRONZE
) {
    val winRate: String
        get() = if (totalMatches > 0) "${(wins * 100 / totalMatches)}%" else "0%"

    val xpToNext: Int
        get() = RankTier.xpToNextTier(xp)

    val xpProgress: Float
        get() = RankTier.xpProgressInTier(xp)

    val nextTierName: String
        get() = RankTier.nextTier(currentTier)?.displayName ?: "Max"
}

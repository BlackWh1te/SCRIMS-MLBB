package com.mlbb.scrim.data.model

data class LeaderboardEntry(
    val rank: Int = 0,
    val playerId: String = "",
    val username: String = "",
    val teamName: String = "",
    val xp: Int = 0,
    val wins: Int = 0,
    val losses: Int = 0,
    val totalMatches: Int = 0,
    val currentTier: RankTier = RankTier.BRONZE
) {
    val winRate: String
        get() = if (totalMatches > 0) "${(wins * 100 / totalMatches)}%" else "0%"
}

package com.scrimslegends.app.data.model

data class LeaderboardEntry(
    val rank: Int = 0,
    val playerId: String = "",
    val username: String = "",
    val teamName: String = "",
    val avatarUrl: String? = null,
    val xp: Int = 0,
    val wins: Int = 0,
    val losses: Int = 0,
    val totalMatches: Int = 0,
    val currentTier: RankTier = RankTier.WARRIOR
) {
    val winRate: String
        get() = if (totalMatches > 0) "${(wins * 100 / totalMatches)}%" else "0%"
}

data class TeamLeaderboardEntry(
    val rank: Int = 0,
    val teamId: String = "",
    val teamName: String = "",
    val logoUrl: String? = null,
    val memberCount: Int = 0,
    val points: Int = 0,
    val wins: Int = 0,
    val losses: Int = 0,
    val totalMatches: Int = 0,
    val reputation: Float = 5.0f,
    val currentTier: RankTier = RankTier.WARRIOR
) {
    val winRate: String
        get() = if (totalMatches > 0) "${(wins * 100 / totalMatches)}%" else "0%"
}

package com.scrimslegends.app.data.model

data class Player(
    val id: String = "",
    val name: String = "",
    val role: PlayerRole = PlayerRole.MEMBER,
    val email: String = "",
    val joinedAt: Long = System.currentTimeMillis(),
    val avatarUrl: String? = null,
    // ── Player stats ──
    val pts: Int = 0,              // Points earned/lost from scrims
    val wins: Int = 0,
    val losses: Int = 0,
    val matchesPlayed: Int = 0
) {
    val winRate: Float
        get() = if (matchesPlayed > 0) (wins * 100f / matchesPlayed) else 0f

    val winRateDisplay: String
        get() = if (matchesPlayed > 0) "${(wins * 100 / matchesPlayed)}%" else "0%"
}

enum class PlayerRole {
    LEADER,
    CO_LEADER,
    MEMBER
}

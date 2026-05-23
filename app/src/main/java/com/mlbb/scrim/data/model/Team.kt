package com.mlbb.scrim.data.model

data class Team(
    val id: String = "",
    val name: String = "",
    val leaderId: String = "",
    val players: List<Player> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val maxPlayers: Int = 7,
    val minPlayers: Int = 5,
    val reputation: Float = 5.0f,
    val canPostScrimsUntil: Long = 0L,
    val totalScrims: Int = 0,
    val completedScrims: Int = 0,
    val noShows: Int = 0,
    val logoUrl: String? = null,
    val isOpenForApplications: Boolean = false
) {
    val currentPlayerCount: Int
        get() = players.size

    val canAddPlayer: Boolean
        get() = currentPlayerCount < maxPlayers

    val isFull: Boolean
        get() = currentPlayerCount >= maxPlayers

    val meetsMinPlayers: Boolean
        get() = currentPlayerCount >= minPlayers

    val isBannedFromPosting: Boolean
        get() = canPostScrimsUntil > System.currentTimeMillis()

    val displayReputation: String
        get() = String.format("%.1f", reputation.coerceIn(1.0f, 5.0f))
}

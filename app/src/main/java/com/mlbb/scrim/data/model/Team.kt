package com.mlbb.scrim.data.model

data class Team(
    val id: String = "",
    val name: String = "",
    val leaderId: String = "",
    val players: List<Player> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val maxPlayers: Int = 7,
    val minPlayers: Int = 5,       // Minimum to participate in scrims
    val reputation: Float = 5.0f,  // 1.0 - 5.0 star rating
    val canPostScrimsUntil: Long = 0L, // 0 = no ban, timestamp = banned until
    val totalScrims: Int = 0,
    val completedScrims: Int = 0,
    val noShows: Int = 0
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

package com.mlbb.scrim.data.model

data class Team(
    val id: String = "",
    val name: String = "",
    val leaderId: String = "",
    val players: List<Player> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val maxPlayers: Int = 7
) {
    val currentPlayerCount: Int
        get() = players.size
    
    val canAddPlayer: Boolean
        get() = currentPlayerCount < maxPlayers
    
    val isFull: Boolean
        get() = currentPlayerCount >= maxPlayers
}

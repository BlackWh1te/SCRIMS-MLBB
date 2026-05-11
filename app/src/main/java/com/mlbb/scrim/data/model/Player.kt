package com.mlbb.scrim.data.model

data class Player(
    val id: String = "",
    val name: String = "",
    val role: PlayerRole = PlayerRole.MEMBER,
    val email: String = "",
    val joinedAt: Long = System.currentTimeMillis()
)

enum class PlayerRole {
    LEADER,
    CO_LEADER,
    MEMBER
}

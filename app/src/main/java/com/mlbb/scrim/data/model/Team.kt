package com.mlbb.scrim.data.model

import java.util.UUID

data class Team(
    val id: UUID,
    val name: String,
    val leaderId: UUID,
    val description: String? = null,
    val minPlayers: Int = 3,
    val maxPlayers: Int = 7,
    val availableDays: List<Int> = listOf(1, 2, 3, 4, 5, 6, 7),
    val availableTimeStart: String? = null,
    val availableTimeEnd: String? = null,
    val timezone: String = "UTC",
    val totalXp: Int = 0,
    val currentTier: String = "Bronze",
    val currentDivision: Int = 1,
    val createdAt: String
)

enum class Tier(val displayName: String, val color: String) {
    BRONZE("Bronze", "#CD7F32"),
    SILVER("Silver", "#C0C0C0"),
    GOLD("Gold", "#FFD700"),
    PLATINUM("Platinum", "#E5E4E2"),
    DIAMOND("Diamond", "#B9F2FF"),
    MASTER("Master", "#FF00FF"),
    GRANDMASTER("Grandmaster", "#FFD700");

    companion object {
        fun fromString(value: String): Tier {
            return values().find { it.name.equals(value, ignoreCase = true) } ?: BRONZE
        }
    }
}

data class TeamMember(
    val id: UUID,
    val teamId: UUID,
    val userId: UUID,
    val role: String,
    val joinedAt: String
)

enum class TeamRole(val displayName: String) {
    LEADER("Leader"),
    CO_LEADER("Co-Leader"),
    MEMBER("Member")
}
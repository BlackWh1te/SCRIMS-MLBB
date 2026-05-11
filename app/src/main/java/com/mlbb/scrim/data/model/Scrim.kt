package com.mlbb.scrim.data.model

data class Scrim(
    val id: String = "",
    val teamId: String = "",
    val teamName: String = "",
    val teamLeader: String = "",
    val gameMode: GameMode = GameMode.RANKED,
    val region: Region = Region.EU,
    val skillLevel: SkillLevel = SkillLevel.ALL,
    val scheduledTime: Long = System.currentTimeMillis(),
    val maxPlayers: Int = 10,
    val currentPlayers: Int = 0,
    val status: ScrimStatus = ScrimStatus.OPEN,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

enum class GameMode {
    RANKED,
    CUSTOM,
    TOURNAMENT,
    CASUAL
}

enum class Region {
    NA,
    EU,
    ASIA,
    SA,
    MCK,
    KRD,
    EKB
}

enum class SkillLevel {
    ALL,
    BEGINNER,
    INTERMEDIATE,
    ADVANCED,
    PRO
}

enum class ScrimStatus {
    OPEN,
    FILLED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED
}

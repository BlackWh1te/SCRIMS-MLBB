package com.mlbb.scrim.data.model

/**
 * LFG (Looking For Group) post — solo players seeking teams.
 */
data class LfgPost(
    val id: String = "",
    val playerId: String = "",
    val playerName: String = "",
    val role: GameRole = GameRole.FLEX,
    val region: Region = Region.UTC,
    val skillLevel: SkillLevel = SkillLevel.ALL,
    val preferredModes: List<GameMode> = emptyList(),
    val message: String = "",
    val mainHeroes: List<String> = emptyList(),
    val bio: String = "",
    val rank: String = "",
    val totalMatches: Int = 0,
    val winRate: String = "",
    val rankedWinRate: String = "",
    val wins: Int = 0,
    val losses: Int = 0,
    val pts: Int = 0,
    val inGameId: String = "",
    val city: String = "",
    val screenshotUrl: String = "",
    val isAvailable: Boolean = true,
    val useMic: Boolean = false,
    val playstyleTags: List<String> = emptyList(),
    val discord: String = "",
    val telegram: String = "",
    val vk: String = "",
    val facebook: String = "",
    val avatarUrl: String? = null,
    val viewCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

enum class GameRole(val displayName: String) {
    TANK("Tank"),
    FIGHTER("Fighter"),
    ASSASSIN("Assassin"),
    MAGE("Mage"),
    MARKSMAN("Marksman"),
    SUPPORT("Support"),
    FLEX("Flex")
}

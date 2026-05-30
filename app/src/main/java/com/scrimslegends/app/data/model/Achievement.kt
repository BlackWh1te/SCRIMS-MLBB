package com.scrimslegends.app.data.model

import androidx.compose.ui.graphics.Color
import com.scrimslegends.app.ui.theme.*

enum class AchievementCategory(val title: String) {
    COMBAT("Combat & Gameplay"),
    SOCIAL("Social & Community"),
    ELITE("Elite Streaks"),
    GENERAL("Time & Consistency")
}

sealed class AchievementCondition {
    abstract val maxProgress: Int
    
    data class MatchesPlayed(override val maxProgress: Int) : AchievementCondition()
    data class WinStreak(override val maxProgress: Int) : AchievementCondition()
    data class ScrimsCreated(override val maxProgress: Int) : AchievementCondition()
    data class TeamsCreated(override val maxProgress: Int) : AchievementCondition()
    data class RatingsGiven(override val maxProgress: Int) : AchievementCondition()
    data object FlawlessVictory : AchievementCondition() { override val maxProgress = 1 }
    data class WinAtTier(val tier: RankTier) : AchievementCondition() { override val maxProgress = 1 }
    data class ReachTier(val tier: RankTier) : AchievementCondition() { override val maxProgress = 1 }
    data object RegionalTop : AchievementCondition() { override val maxProgress = 1 }
    
    // New Advanced Conditions
    data class RoleWins(val role: String, override val maxProgress: Int) : AchievementCondition()
    data class InvitesAccepted(override val maxProgress: Int) : AchievementCondition()
    data class NightWins(override val maxProgress: Int) : AchievementCondition()
    data class WeekendScrims(override val maxProgress: Int) : AchievementCondition()
    data class HighRatingMaintained(override val maxProgress: Int) : AchievementCondition()
    data object UnderdogWin : AchievementCondition() { override val maxProgress = 1 }
    data class PlaySameTeam(override val maxProgress: Int) : AchievementCondition()
}

enum class AchievementTier {
    BRONZE, SILVER, GOLD, EPIC, GRANDMASTER, LEGEND, MYTHIC
}

enum class Achievement(
    val id: String,
    val displayName: String,
    val description: String,
    val iconLetter: String,
    val badgeColor: Color,
    val glowColor: Color,
    val condition: AchievementCondition,
    val tier: AchievementTier = AchievementTier.BRONZE,
    val category: AchievementCategory = AchievementCategory.GENERAL
) {
    FIRST_SCRIM(
        id = "first_scrim",
        displayName = "First Blood",
        description = "Play your first scrim match",
        iconLetter = "1",
        badgeColor = Bronze,
        glowColor = Bronze.copy(alpha = 0.4f),
        condition = AchievementCondition.MatchesPlayed(1),
        tier = AchievementTier.BRONZE,
        category = AchievementCategory.GENERAL
    ),
    WIN_STREAK_5(
        id = "win_streak_5",
        displayName = "On Fire",
        description = "Win 5 scrims in a row",
        iconLetter = "5",
        badgeColor = GoldRank,
        glowColor = GoldRank.copy(alpha = 0.4f),
        condition = AchievementCondition.WinStreak(5),
        tier = AchievementTier.GOLD,
        category = AchievementCategory.ELITE
    ),
    UNSTOPPABLE(
        id = "win_streak_10",
        displayName = "Unstoppable",
        description = "Win 10 scrims in a row",
        iconLetter = "10",
        badgeColor = EpicCyan,
        glowColor = EpicCyan.copy(alpha = 0.5f),
        condition = AchievementCondition.WinStreak(10),
        tier = AchievementTier.EPIC,
        category = AchievementCategory.ELITE
    ),
    GODLIKE(
        id = "win_streak_20",
        displayName = "Godlike",
        description = "Win 20 scrims in a row",
        iconLetter = "20",
        badgeColor = MythicCrimson,
        glowColor = MythicCrimson.copy(alpha = 0.6f),
        condition = AchievementCondition.WinStreak(20),
        tier = AchievementTier.MYTHIC,
        category = AchievementCategory.ELITE
    ),
    SCRIM_HOST_10(
        id = "scrim_host_10",
        displayName = "Scrim Host",
        description = "Create 10 scrim posts",
        iconLetter = "H",
        badgeColor = SolverBlue,
        glowColor = SolverBlue.copy(alpha = 0.4f),
        condition = AchievementCondition.ScrimsCreated(10),
        tier = AchievementTier.SILVER,
        category = AchievementCategory.SOCIAL
    ),
    FLAWLESS_VICTORY(
        id = "flawless_victory",
        displayName = "Flawless",
        description = "Win a scrim 10-0",
        iconLetter = "F",
        badgeColor = EpicCyan,
        glowColor = EpicCyan.copy(alpha = 0.4f),
        condition = AchievementCondition.FlawlessVictory,
        tier = AchievementTier.EPIC,
        category = AchievementCategory.COMBAT
    ),
    VETERAN_100(
        id = "veteran_100",
        displayName = "Veteran",
        description = "Complete 100 scrim matches",
        iconLetter = "V",
        badgeColor = GrandmasterPurple,
        glowColor = GrandmasterPurple.copy(alpha = 0.4f),
        condition = AchievementCondition.MatchesPlayed(100),
        tier = AchievementTier.GRANDMASTER,
        category = AchievementCategory.GENERAL
    ),
    LEGEND_WIN(
        id = "legend_win",
        displayName = "Legendary",
        description = "Win a scrim while in Legend tier",
        iconLetter = "L",
        badgeColor = LegendRed,
        glowColor = LegendRed.copy(alpha = 0.4f),
        condition = AchievementCondition.WinAtTier(RankTier.LEGEND),
        tier = AchievementTier.LEGEND,
        category = AchievementCategory.COMBAT
    ),
    MYTHIC_REACHED(
        id = "mythic_reached",
        displayName = "Mythic",
        description = "Reach Mythic rank",
        iconLetter = "M",
        badgeColor = MythicCrimson,
        glowColor = MythicCrimson.copy(alpha = 0.5f),
        condition = AchievementCondition.ReachTier(RankTier.MYTHIC),
        tier = AchievementTier.MYTHIC,
        category = AchievementCategory.GENERAL
    ),
    REGIONAL_TOP(
        id = "regional_top",
        displayName = "Regional Dominator",
        description = "Earn a TOP 1, 2, or 3 regional badge",
        iconLetter = "R",
        badgeColor = Top1Gold,
        glowColor = Top1Gold.copy(alpha = 0.5f),
        condition = AchievementCondition.RegionalTop,
        tier = AchievementTier.GOLD,
        category = AchievementCategory.ELITE
    ),
    TEAM_CREATOR(
        id = "team_creator",
        displayName = "Founder",
        description = "Create your first team",
        iconLetter = "T",
        badgeColor = SuccessGreen,
        glowColor = SuccessGreen.copy(alpha = 0.4f),
        condition = AchievementCondition.TeamsCreated(1),
        tier = AchievementTier.BRONZE,
        category = AchievementCategory.SOCIAL
    ),
    RATED_10(
        id = "rated_10",
        displayName = "Critic",
        description = "Rate 10 opponents after scrims",
        iconLetter = "C",
        badgeColor = iOSBlue,
        glowColor = iOSBlue.copy(alpha = 0.4f),
        condition = AchievementCondition.RatingsGiven(10),
        tier = AchievementTier.SILVER,
        category = AchievementCategory.SOCIAL
    ),
    ASSASSIN_MASTER(
        id = "assassin_master",
        displayName = "The Assassin",
        description = "Win 20 matches as Jungler",
        iconLetter = "J",
        badgeColor = GrandmasterPurple,
        glowColor = GrandmasterPurple.copy(alpha = 0.5f),
        condition = AchievementCondition.RoleWins("Jungler", 20),
        tier = AchievementTier.GRANDMASTER,
        category = AchievementCategory.COMBAT
    ),
    ROAMER_MASTER(
        id = "roamer_master",
        displayName = "The Wall",
        description = "Win 20 matches as Roamer",
        iconLetter = "W",
        badgeColor = LegendRed,
        glowColor = LegendRed.copy(alpha = 0.5f),
        condition = AchievementCondition.RoleWins("Roamer", 20),
        tier = AchievementTier.GRANDMASTER,
        category = AchievementCategory.COMBAT
    ),
    NIGHT_OWL(
        id = "night_owl",
        displayName = "Night Owl",
        description = "Win 5 scrims between 12AM and 4AM",
        iconLetter = "N",
        badgeColor = DarkNavy,
        glowColor = Purple.copy(alpha = 0.5f),
        condition = AchievementCondition.NightWins(5),
        tier = AchievementTier.SILVER,
        category = AchievementCategory.GENERAL
    ),
    FIVE_STAR(
        id = "five_star",
        displayName = "Five-Star Host",
        description = "Maintain a 5.0 rating over 20 matches",
        iconLetter = "★",
        badgeColor = Top1Gold,
        glowColor = Top1Gold.copy(alpha = 0.6f),
        condition = AchievementCondition.HighRatingMaintained(20),
        tier = AchievementTier.EPIC,
        category = AchievementCategory.SOCIAL
    ),
    DAVID_VS_GOLIATH(
        id = "david_goliath",
        displayName = "Giant Slayer",
        description = "Win against a team 1+ tier above yours",
        iconLetter = "D",
        badgeColor = MythicCrimson,
        glowColor = MythicCrimson.copy(alpha = 0.6f),
        condition = AchievementCondition.UnderdogWin,
        tier = AchievementTier.MYTHIC,
        category = AchievementCategory.COMBAT
    );

    companion object {
        fun checkUnlocks(profile: UserProfile, stats: PlayerAchievements): List<Achievement> {
            return values().filter { achievement ->
                when (val cond = achievement.condition) {
                    is AchievementCondition.MatchesPlayed -> stats.matchesPlayed >= cond.maxProgress
                    is AchievementCondition.WinStreak -> stats.bestWinStreak >= cond.maxProgress
                    is AchievementCondition.ScrimsCreated -> stats.scrimsCreated >= cond.maxProgress
                    is AchievementCondition.TeamsCreated -> stats.teamsCreated >= cond.maxProgress
                    is AchievementCondition.RatingsGiven -> stats.ratingsGiven >= cond.maxProgress
                    is AchievementCondition.FlawlessVictory -> stats.hasFlawlessVictory
                    is AchievementCondition.WinAtTier -> profile.currentTier.ordinal >= cond.tier.ordinal
                    is AchievementCondition.ReachTier -> profile.currentTier.ordinal >= cond.tier.ordinal
                    is AchievementCondition.RegionalTop -> stats.hasRegionalTop
                    is AchievementCondition.RoleWins -> {
                        when (cond.role) {
                            "Jungler" -> stats.junglerWins >= cond.maxProgress
                            "Roamer" -> stats.roamerWins >= cond.maxProgress
                            else -> false
                        }
                    }
                    is AchievementCondition.NightWins -> stats.nightWins >= cond.maxProgress
                    is AchievementCondition.WeekendScrims -> stats.weekendScrims >= cond.maxProgress
                    is AchievementCondition.HighRatingMaintained -> stats.fiveStarMatches >= cond.maxProgress
                    is AchievementCondition.UnderdogWin -> stats.hasUnderdogWin
                    is AchievementCondition.PlaySameTeam -> stats.maxSameTeamPlays >= cond.maxProgress
                    is AchievementCondition.InvitesAccepted -> stats.invitesAccepted >= cond.maxProgress
                }
            }
        }
    }
}

data class PlayerAchievements(
    val playerId: String = "",
    val unlockedAchievements: List<String> = emptyList(),
    val matchesPlayed: Int = 0,
    val currentWinStreak: Int = 0,
    val bestWinStreak: Int = 0,
    val scrimsCreated: Int = 0,
    val teamsCreated: Int = 0,
    val ratingsGiven: Int = 0,
    val hasRegionalTop: Boolean = false,
    
    // New Advanced Stats
    val junglerWins: Int = 0,
    val roamerWins: Int = 0,
    val goldLanerWins: Int = 0,
    val nightWins: Int = 0,
    val weekendScrims: Int = 0,
    val fiveStarMatches: Int = 0,
    val hasFlawlessVictory: Boolean = false,
    val hasUnderdogWin: Boolean = false,
    val maxSameTeamPlays: Int = 0,
    val invitesAccepted: Int = 0
) {
    fun isUnlocked(achievement: Achievement): Boolean = achievement.id in unlockedAchievements
    
    fun getProgress(achievement: Achievement): Int {
        if (isUnlocked(achievement)) return achievement.condition.maxProgress
        
        return when (val cond = achievement.condition) {
            is AchievementCondition.MatchesPlayed -> matchesPlayed
            is AchievementCondition.WinStreak -> bestWinStreak
            is AchievementCondition.ScrimsCreated -> scrimsCreated
            is AchievementCondition.TeamsCreated -> teamsCreated
            is AchievementCondition.RatingsGiven -> ratingsGiven
            is AchievementCondition.FlawlessVictory -> if (hasFlawlessVictory) 1 else 0
            is AchievementCondition.WinAtTier -> 0
            is AchievementCondition.ReachTier -> 0
            is AchievementCondition.RegionalTop -> if (hasRegionalTop) 1 else 0
            is AchievementCondition.RoleWins -> {
                when (cond.role) {
                    "Jungler" -> junglerWins
                    "Roamer" -> roamerWins
                    else -> 0
                }
            }
            is AchievementCondition.NightWins -> nightWins
            is AchievementCondition.WeekendScrims -> weekendScrims
            is AchievementCondition.HighRatingMaintained -> fiveStarMatches
            is AchievementCondition.UnderdogWin -> if (hasUnderdogWin) 1 else 0
            is AchievementCondition.PlaySameTeam -> maxSameTeamPlays
            is AchievementCondition.InvitesAccepted -> invitesAccepted
        }
    }
    
    fun getProgressPercentage(achievement: Achievement): Float {
        val max = achievement.condition.maxProgress
        if (max == 0) return 0f
        val current = getProgress(achievement)
        return (current.toFloat() / max.toFloat()).coerceIn(0f, 1f)
    }
}

package com.scrimslegends.app.data.model

import androidx.compose.ui.graphics.Color
import com.scrimslegends.app.ui.theme.*

/**
 * Custom 7-Tier Rank System for Scrims Legends
 * Bronze → Solver → Gold → Grandmaster → Epic → Legend → Mythic
 */
enum class RankTier(
    val displayName: String,
    val shortName: String,
    val minXp: Int,
    val maxXp: Int,
    val tierColor: Color,
    val badgeGradient: List<Color>,
    val textColor: Color = Color.White
) {
    WARRIOR(
        displayName = "Warrior",
        shortName = "W",
        minXp = 0,
        maxXp = 999,
        tierColor = WarriorBrown,
        badgeGradient = WarriorGradient,
    ),
    ELITE(
        displayName = "Elite",
        shortName = "E",
        minXp = 1000,
        maxXp = 2499,
        tierColor = EliteSilver,
        badgeGradient = EliteGradient,
    ),
    MASTER(
        displayName = "Master",
        shortName = "M",
        minXp = 2500,
        maxXp = 4999,
        tierColor = MasterGold,
        badgeGradient = MasterGoldGradient,
        textColor = DarkBlue
    ),
    GRANDMASTER(
        displayName = "Grandmaster",
        shortName = "GM",
        minXp = 5000,
        maxXp = 7999,
        tierColor = GrandmasterPurple,
        badgeGradient = GrandmasterGradient,
    ),
    EPIC(
        displayName = "Epic",
        shortName = "EP",
        minXp = 8000,
        maxXp = 11999,
        tierColor = EpicCyan,
        badgeGradient = EpicGradient,
    ),
    LEGEND(
        displayName = "Legend",
        shortName = "L",
        minXp = 12000,
        maxXp = 16999,
        tierColor = LegendRed,
        badgeGradient = LegendGradient,
    ),
    MYTHIC(
        displayName = "Mythic",
        shortName = "MY",
        minXp = 17000,
        maxXp = 24999,
        tierColor = MythicCrimson,
        badgeGradient = MythicGradient,
    ),
    MYTHICAL_HONOR(
        displayName = "Mythical Honor",
        shortName = "MH",
        minXp = 25000,
        maxXp = 34999,
        tierColor = HonorBlue,
        badgeGradient = MythicalHonorGradient,
    ),
    MYTHICAL_GLORY(
        displayName = "Mythical Glory",
        shortName = "MG",
        minXp = 35000,
        maxXp = 49999,
        tierColor = GloryPink,
        badgeGradient = MythicalGloryGradient,
    ),
    MYTHICAL_IMMORTAL(
        displayName = "Mythical Immortal",
        shortName = "MI",
        minXp = 50000,
        maxXp = Int.MAX_VALUE,
        tierColor = ImmortalRed,
        badgeGradient = MythicalImmortalGradient,
    );

    companion object {
        fun fromXp(xp: Int): RankTier {
            return values().find { xp in it.minXp..it.maxXp } ?: WARRIOR
        }

        fun nextTier(current: RankTier): RankTier? {
            val idx = values().indexOf(current)
            return if (idx < values().size - 1) values()[idx + 1] else null
        }

        fun xpToNextTier(xp: Int): Int {
            val current = fromXp(xp)
            val next = nextTier(current) ?: return 0
            return next.minXp - xp
        }

        fun xpProgressInTier(xp: Int): Float {
            val current = fromXp(xp)
            val range = current.maxXp.toLong() - current.minXp.toLong() + 1L
            val progress = xp.toLong() - current.minXp.toLong()
            return (progress.toFloat() / range).coerceIn(0f, 1f)
        }
    }
}

/**
 * Regional ranking badge for top performers in specific regions.
 * Tracks TOP1, TOP2, TOP3 in RU scrims (KRD, MSK, EKB).
 */
enum class RegionalRank(val displayPrefix: String, val rank: Int, val badgeColor: Color) {
    TOP1("TOP 1", 1, BluePrimary),
    TOP2("TOP 2", 2, Silver),
    TOP3("TOP 3", 3, Bronze);

    companion object {
        fun fromWins(wins: Int, region: String): RegionalRank? {
            // KRD = Krasnodar, MSK = Moscow, EKB = Ekaterinburg
            // These are RU-region scrim servers
            val ruRegions = listOf("KRD", "MSK", "EKB")
            if (region !in ruRegions) return null
            return when {
                wins >= 50 -> TOP1
                wins >= 30 -> TOP2
                wins >= 15 -> TOP3
                else -> null
            }
        }
    }
}

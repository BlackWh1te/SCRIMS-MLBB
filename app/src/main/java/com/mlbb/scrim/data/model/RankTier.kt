package com.mlbb.scrim.data.model

import androidx.compose.ui.graphics.Color
import com.mlbb.scrim.ui.theme.*

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
    BRONZE(
        displayName = "Bronze",
        shortName = "B",
        minXp = 0,
        maxXp = 999,
        tierColor = Bronze,
        badgeGradient = listOf(Color(0xFFCD7F32), Color(0xFF8B4513)),
    ),
    SOLVER(
        displayName = "Solver",
        shortName = "S",
        minXp = 1000,
        maxXp = 2499,
        tierColor = SolverBlue,
        badgeGradient = listOf(Color(0xFF4A90D9), Color(0xFF1A5490)),
    ),
    GOLD(
        displayName = "Gold",
        shortName = "G",
        minXp = 2500,
        maxXp = 4999,
        tierColor = GoldRank,
        badgeGradient = listOf(Color(0xFFFFD700), Color(0xFFFF8C00)),
        textColor = DarkBlue
    ),
    GRANDMASTER(
        displayName = "Grandmaster",
        shortName = "GM",
        minXp = 5000,
        maxXp = 7999,
        tierColor = GrandmasterPurple,
        badgeGradient = listOf(Color(0xFF9B59B6), Color(0xFF6C3483)),
    ),
    EPIC(
        displayName = "Epic",
        shortName = "E",
        minXp = 8000,
        maxXp = 11999,
        tierColor = EpicCyan,
        badgeGradient = listOf(Color(0xFF00CED1), Color(0xFF008B8B)),
    ),
    LEGEND(
        displayName = "Legend",
        shortName = "L",
        minXp = 12000,
        maxXp = 16999,
        tierColor = LegendRed,
        badgeGradient = listOf(Color(0xFFFF4757), Color(0xFF8B0000)),
    ),
    MYTHIC(
        displayName = "Mythic",
        shortName = "M",
        minXp = 17000,
        maxXp = Int.MAX_VALUE,
        tierColor = MythicCrimson,
        badgeGradient = listOf(Color(0xFFFF1B1B), Color(0xFF0A0A0A), Color(0xFFFFD700)),
    );

    companion object {
        fun fromXp(xp: Int): RankTier {
            return values().find { xp in it.minXp..it.maxXp } ?: BRONZE
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
    TOP1("TOP 1", 1, GoldPrimary),
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

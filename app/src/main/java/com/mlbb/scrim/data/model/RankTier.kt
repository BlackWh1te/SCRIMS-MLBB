package com.mlbb.scrim.data.model

import androidx.compose.ui.graphics.Color
import com.mlbb.scrim.ui.theme.*

enum class RankTier(val displayName: String, val minXp: Int, val maxXp: Int, val tierColor: Color) {
    BRONZE("Bronze", 0, 999, Bronze),
    SILVER("Silver", 1000, 2499, Silver),
    GOLD("Gold", 2500, 4999, Gold),
    PLATINUM("Platinum", 5000, 7999, Platinum),
    DIAMOND("Diamond", 8000, 11999, Diamond),
    MASTER("Master", 12000, 16999, Master),
    GRANDMASTER("Grandmaster", 17000, Int.MAX_VALUE, Grandmaster);

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
            val range = current.maxXp - current.minXp + 1
            val progress = xp - current.minXp
            return (progress.toFloat() / range).coerceIn(0f, 1f)
        }
    }
}

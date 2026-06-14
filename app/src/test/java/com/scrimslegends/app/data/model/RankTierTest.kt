package com.scrimslegends.app.data.model

import org.junit.Assert.*
import org.junit.Test

class RankTierTest {

    // ─── fromXp tests ───

    @Test
    fun `fromXp returns BRONZE for 0 XP`() {
        assertEquals(RankTier.BRONZE, RankTier.fromXp(0))
    }

    @Test
    fun `fromXp returns BRONZE for 999 XP`() {
        assertEquals(RankTier.BRONZE, RankTier.fromXp(999))
    }

    @Test
    fun `fromXp returns SOLVER for 1000 XP`() {
        assertEquals(RankTier.SOLVER, RankTier.fromXp(1000))
    }

    @Test
    fun `fromXp returns SOLVER for 2499 XP`() {
        assertEquals(RankTier.SOLVER, RankTier.fromXp(2499))
    }

    @Test
    fun `fromXp returns GOLD for 2500 XP`() {
        assertEquals(RankTier.GOLD, RankTier.fromXp(2500))
    }

    @Test
    fun `fromXp returns GOLD for 4999 XP`() {
        assertEquals(RankTier.GOLD, RankTier.fromXp(4999))
    }

    @Test
    fun `fromXp returns GRANDMASTER for 5000 XP`() {
        assertEquals(RankTier.GRANDMASTER, RankTier.fromXp(5000))
    }

    @Test
    fun `fromXp returns EPIC for 8000 XP`() {
        assertEquals(RankTier.EPIC, RankTier.fromXp(8000))
    }

    @Test
    fun `fromXp returns LEGEND for 12000 XP`() {
        assertEquals(RankTier.LEGEND, RankTier.fromXp(12000))
    }

    @Test
    fun `fromXp returns MYTHIC for 17000 XP`() {
        assertEquals(RankTier.MYTHIC, RankTier.fromXp(17000))
    }

    @Test
    fun `fromXp returns BRONZE for negative XP`() {
        assertEquals(RankTier.BRONZE, RankTier.fromXp(-100))
    }

    @Test
    fun `fromXp returns MYTHIC for very high XP`() {
        assertEquals(RankTier.MYTHIC, RankTier.fromXp(Int.MAX_VALUE))
    }

    // ─── nextTier tests ───

    @Test
    fun `nextTier advances BRONZE to SOLVER`() {
        assertEquals(RankTier.SOLVER, RankTier.nextTier(RankTier.BRONZE))
    }

    @Test
    fun `nextTier advances SOLVER to GOLD`() {
        assertEquals(RankTier.GOLD, RankTier.nextTier(RankTier.SOLVER))
    }

    @Test
    fun `nextTier advances GOLD to GRANDMASTER`() {
        assertEquals(RankTier.GRANDMASTER, RankTier.nextTier(RankTier.GOLD))
    }

    @Test
    fun `nextTier advances GRANDMASTER to EPIC`() {
        assertEquals(RankTier.EPIC, RankTier.nextTier(RankTier.GRANDMASTER))
    }

    @Test
    fun `nextTier advances EPIC to LEGEND`() {
        assertEquals(RankTier.LEGEND, RankTier.nextTier(RankTier.EPIC))
    }

    @Test
    fun `nextTier advances LEGEND to MYTHIC`() {
        assertEquals(RankTier.MYTHIC, RankTier.nextTier(RankTier.LEGEND))
    }

    @Test
    fun `nextTier returns null for MYTHIC`() {
        assertNull(RankTier.nextTier(RankTier.MYTHIC))
    }

    // ─── xpToNextTier tests ───

    @Test
    fun `xpToNextTier returns 1000 for 0 XP`() {
        assertEquals(1000, RankTier.xpToNextTier(0))
    }

    @Test
    fun `xpToNextTier returns 1 for 999 XP`() {
        assertEquals(1, RankTier.xpToNextTier(999))
    }

    @Test
    fun `xpToNextTier returns 1500 for 1000 XP`() {
        assertEquals(1500, RankTier.xpToNextTier(1000))
    }

    @Test
    fun `xpToNextTier returns 0 for MYTHIC tier`() {
        assertEquals(0, RankTier.xpToNextTier(17000))
    }

    @Test
    fun `xpToNextTier returns 0 for max XP`() {
        assertEquals(0, RankTier.xpToNextTier(Int.MAX_VALUE))
    }

    // ─── xpProgressInTier tests ───

    @Test
    fun `xpProgressInTier returns 0 at tier start`() {
        assertEquals(0f, RankTier.xpProgressInTier(0), 0.001f)
    }

    @Test
    fun `xpProgressInTier returns approximately 0_5 at tier midpoint`() {
        val mid = (RankTier.BRONZE.minXp + RankTier.BRONZE.maxXp) / 2
        val progress = RankTier.xpProgressInTier(mid)
        assertTrue("Progress at midpoint should be around 0.5, was $progress", progress in 0.4f..0.6f)
    }

    @Test
    fun `xpProgressInTier returns 1 at tier end`() {
        assertEquals(1f, RankTier.xpProgressInTier(999), 0.001f)
    }

    @Test
    fun `xpProgressInTier coerces to 1 for tier max XP`() {
        assertEquals(1f, RankTier.xpProgressInTier(2499), 0.001f)
    }

    @Test
    fun `xpProgressInTier coerces to 0 for negative XP`() {
        assertEquals(0f, RankTier.xpProgressInTier(-100), 0.001f)
    }

    // ─── Tier properties ───

    @Test
    fun `all tiers have valid display names`() {
        RankTier.values().forEach { tier ->
            assertTrue("Tier ${tier.name} displayName should not be blank", tier.displayName.isNotBlank())
        }
    }

    @Test
    fun `all tiers have valid short names`() {
        RankTier.values().forEach { tier ->
            assertTrue("Tier ${tier.name} shortName should not be blank", tier.shortName.isNotBlank())
        }
    }

    @Test
    fun `tier minXp increases monotonically`() {
        val tiers = RankTier.values()
        for (i in 1 until tiers.size) {
            assertTrue(
                "${tiers[i].name} minXp should be > ${tiers[i-1].name} minXp",
                tiers[i].minXp > tiers[i-1].minXp
            )
        }
    }

    @Test
    fun `tier maxXp increases monotonically`() {
        val tiers = RankTier.values()
        for (i in 1 until tiers.size) {
            assertTrue(
                "${tiers[i].name} maxXp should be > ${tiers[i-1].name} maxXp",
                tiers[i].maxXp > tiers[i-1].maxXp
            )
        }
    }

    @Test
    fun `MYTHIC maxXp is Int_MAX_VALUE`() {
        assertEquals(Int.MAX_VALUE, RankTier.MYTHIC.maxXp)
    }

    // ─── UserProfile tier integration ───

    @Test
    fun `userProfile xpToNext uses RankTier correctly`() {
        val profile = UserProfile(xp = 500)
        assertEquals(500, profile.xpToNext)
    }

    @Test
    fun `userProfile xpProgress returns value in 0 to 1 range`() {
        val profile = UserProfile(xp = 500)
        assertTrue(profile.xpProgress in 0f..1f)
    }

    @Test
    fun `userProfile nextTierName returns correct next tier`() {
        val profile = UserProfile(xp = 500, currentTier = RankTier.BRONZE)
        assertEquals("Solver", profile.nextTierName)
    }

    @Test
    fun `userProfile nextTierName returns Max for MYTHIC`() {
        val profile = UserProfile(xp = 17000, currentTier = RankTier.MYTHIC)
        assertEquals("Max", profile.nextTierName)
    }
}

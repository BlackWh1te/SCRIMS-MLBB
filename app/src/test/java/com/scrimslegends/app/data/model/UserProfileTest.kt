package com.scrimslegends.app.data.model

import org.junit.Assert.*
import org.junit.Test

class UserProfileTest {

    @Test
    fun `default profile has zero stats`() {
        val profile = UserProfile()
        assertEquals(0, profile.xp)
        assertEquals(0, profile.pts)
        assertEquals(0, profile.totalMatches)
        assertEquals(0, profile.wins)
        assertEquals(0, profile.losses)
    }

    @Test
    fun `winRate returns correct percentage`() {
        val profile = UserProfile(totalMatches = 10, wins = 7, losses = 3)
        assertEquals("70%", profile.winRate)
    }

    @Test
    fun `winRate returns 0 percent when no matches`() {
        val profile = UserProfile()
        assertEquals("0%", profile.winRate)
    }

    @Test
    fun `winRate handles division truncation`() {
        val profile = UserProfile(totalMatches = 3, wins = 1, losses = 2)
        // Integer division: 1 * 100 / 3 = 33
        assertEquals("33%", profile.winRate)
    }

    @Test
    fun `winRateFloat returns correct float value`() {
        val profile = UserProfile(totalMatches = 4, wins = 1)
        assertEquals(25f, profile.winRateFloat, 0.001f)
    }

    @Test
    fun `winRateFloat returns 0 when no matches`() {
        val profile = UserProfile()
        assertEquals(0f, profile.winRateFloat, 0.001f)
    }

    @Test
    fun `xpToNext returns correct value for bronze tier`() {
        val profile = UserProfile(xp = 500)
        assertEquals(500, profile.xpToNext)
    }

    @Test
    fun `xpToNext returns 0 for mythic tier`() {
        val profile = UserProfile(xp = 20000)
        assertEquals(0, profile.xpToNext)
    }

    @Test
    fun `xpProgress returns value between 0 and 1`() {
        val profile = UserProfile(xp = 500)
        val progress = profile.xpProgress
        assertTrue("Progress should be in [0,1], was $progress", progress in 0f..1f)
    }

    @Test
    fun `nextTierName returns correct tier`() {
        val profile = UserProfile(currentTier = RankTier.BRONZE)
        assertEquals("Solver", profile.nextTierName)
    }

    @Test
    fun `nextTierName returns Max for mythic`() {
        val profile = UserProfile(currentTier = RankTier.MYTHIC)
        assertEquals("Max", profile.nextTierName)
    }

    @Test
    fun `ptsDisplay shows plus for positive`() {
        val profile = UserProfile(pts = 25)
        assertEquals("+25", profile.ptsDisplay)
    }

    @Test
    fun `ptsDisplay shows minus for negative`() {
        val profile = UserProfile(pts = -15)
        assertEquals("-15", profile.ptsDisplay)
    }

    @Test
    fun `ptsDisplay shows zero`() {
        val profile = UserProfile(pts = 0)
        assertEquals("+0", profile.ptsDisplay)
    }

    @Test
    fun `default emailVerified is false`() {
        assertFalse(UserProfile().emailVerified)
    }

    @Test
    fun `default isBanned is false`() {
        assertFalse(UserProfile().isBanned)
    }

    @Test
    fun `default currentTier is BRONZE`() {
        assertEquals(RankTier.BRONZE, UserProfile().currentTier)
    }

    @Test
    fun `mainHeroes defaults to empty list`() {
        assertTrue(UserProfile().mainHeroes.isEmpty())
    }

    @Test
    fun `copy creates independent instance`() {
        val original = UserProfile(id = "1", username = "Test", xp = 1000)
        val copy = original.copy(xp = 2000)
        assertEquals("1", copy.id)
        assertEquals("Test", copy.username)
        assertEquals(1000, original.xp)
        assertEquals(2000, copy.xp)
    }

    @Test
    fun `equals works for identical profiles`() {
        val p1 = UserProfile(id = "1", username = "Test")
        val p2 = UserProfile(id = "1", username = "Test")
        assertEquals(p1, p2)
    }

    @Test
    fun `equals fails for different profiles`() {
        val p1 = UserProfile(id = "1")
        val p2 = UserProfile(id = "2")
        assertNotEquals(p1, p2)
    }
}

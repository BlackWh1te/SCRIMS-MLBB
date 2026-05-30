package com.mlbb.scrim.data.model

import org.junit.Assert.*
import org.junit.Test

class AchievementTest {

    // ─── Achievement.unlock check tests ───

    @Test
    fun `FIRST_SCRIM unlocks after 1 match`() {
        val profile = UserProfile()
        val stats = PlayerAchievements(matchesPlayed = 1)
        val unlocked = Achievement.checkUnlocks(profile, stats)
        assertTrue(unlocked.contains(Achievement.FIRST_SCRIM))
    }

    @Test
    fun `FIRST_SCRIM does not unlock at 0 matches`() {
        val profile = UserProfile()
        val stats = PlayerAchievements(matchesPlayed = 0)
        val unlocked = Achievement.checkUnlocks(profile, stats)
        assertFalse(unlocked.contains(Achievement.FIRST_SCRIM))
    }

    @Test
    fun `WIN_STREAK_5 unlocks after 5 win streak`() {
        val profile = UserProfile()
        val stats = PlayerAchievements(bestWinStreak = 5)
        val unlocked = Achievement.checkUnlocks(profile, stats)
        assertTrue(unlocked.contains(Achievement.WIN_STREAK_5))
    }

    @Test
    fun `WIN_STREAK_5 does not unlock at 4 win streak`() {
        val profile = UserProfile()
        val stats = PlayerAchievements(bestWinStreak = 4)
        val unlocked = Achievement.checkUnlocks(profile, stats)
        assertFalse(unlocked.contains(Achievement.WIN_STREAK_5))
    }

    @Test
    fun `UNSTOPPABLE unlocks after 10 win streak`() {
        val profile = UserProfile()
        val stats = PlayerAchievements(bestWinStreak = 10)
        val unlocked = Achievement.checkUnlocks(profile, stats)
        assertTrue(unlocked.contains(Achievement.UNSTOPPABLE))
    }

    @Test
    fun `GODLIKE unlocks after 20 win streak`() {
        val profile = UserProfile()
        val stats = PlayerAchievements(bestWinStreak = 20)
        val unlocked = Achievement.checkUnlocks(profile, stats)
        assertTrue(unlocked.contains(Achievement.GODLIKE))
    }

    @Test
    fun `SCRIM_HOST_10 unlocks after 10 scrims created`() {
        val profile = UserProfile()
        val stats = PlayerAchievements(scrimsCreated = 10)
        val unlocked = Achievement.checkUnlocks(profile, stats)
        assertTrue(unlocked.contains(Achievement.SCRIM_HOST_10))
    }

    @Test
    fun `FLAWLESS_VICTORY unlocks when hasFlawlessVictory is true`() {
        val profile = UserProfile()
        val stats = PlayerAchievements(hasFlawlessVictory = true)
        val unlocked = Achievement.checkUnlocks(profile, stats)
        assertTrue(unlocked.contains(Achievement.FLAWLESS_VICTORY))
    }

    @Test
    fun `VETERAN_100 unlocks after 100 matches`() {
        val profile = UserProfile()
        val stats = PlayerAchievements(matchesPlayed = 100)
        val unlocked = Achievement.checkUnlocks(profile, stats)
        assertTrue(unlocked.contains(Achievement.VETERAN_100))
    }

    @Test
    fun `LEGEND_WIN unlocks when in Legend tier or above`() {
        val profile = UserProfile(currentTier = RankTier.LEGEND)
        val stats = PlayerAchievements()
        val unlocked = Achievement.checkUnlocks(profile, stats)
        assertTrue(unlocked.contains(Achievement.LEGEND_WIN))
    }

    @Test
    fun `LEGEND_WIN unlocks when in Mythic tier`() {
        val profile = UserProfile(currentTier = RankTier.MYTHIC)
        val stats = PlayerAchievements()
        val unlocked = Achievement.checkUnlocks(profile, stats)
        assertTrue(unlocked.contains(Achievement.LEGEND_WIN))
    }

    @Test
    fun `LEGEND_WIN does not unlock in Gold tier`() {
        val profile = UserProfile(currentTier = RankTier.GOLD)
        val stats = PlayerAchievements()
        val unlocked = Achievement.checkUnlocks(profile, stats)
        assertFalse(unlocked.contains(Achievement.LEGEND_WIN))
    }

    @Test
    fun `MYTHIC_REACHED unlocks when in Mythic tier`() {
        val profile = UserProfile(currentTier = RankTier.MYTHIC)
        val stats = PlayerAchievements()
        val unlocked = Achievement.checkUnlocks(profile, stats)
        assertTrue(unlocked.contains(Achievement.MYTHIC_REACHED))
    }

    @Test
    fun `MYTHIC_REACHED does not unlock in Legend tier`() {
        val profile = UserProfile(currentTier = RankTier.LEGEND)
        val stats = PlayerAchievements()
        val unlocked = Achievement.checkUnlocks(profile, stats)
        assertFalse(unlocked.contains(Achievement.MYTHIC_REACHED))
    }

    @Test
    fun `TEAM_CREATOR unlocks after creating 1 team`() {
        val profile = UserProfile()
        val stats = PlayerAchievements(teamsCreated = 1)
        val unlocked = Achievement.checkUnlocks(profile, stats)
        assertTrue(unlocked.contains(Achievement.TEAM_CREATOR))
    }

    @Test
    fun `RATED_10 unlocks after 10 ratings`() {
        val profile = UserProfile()
        val stats = PlayerAchievements(ratingsGiven = 10)
        val unlocked = Achievement.checkUnlocks(profile, stats)
        assertTrue(unlocked.contains(Achievement.RATED_10))
    }

    @Test
    fun `ASSASSIN_MASTER unlocks after 20 jungler wins`() {
        val profile = UserProfile()
        val stats = PlayerAchievements(junglerWins = 20)
        val unlocked = Achievement.checkUnlocks(profile, stats)
        assertTrue(unlocked.contains(Achievement.ASSASSIN_MASTER))
    }

    @Test
    fun `ROAMER_MASTER unlocks after 20 roamer wins`() {
        val profile = UserProfile()
        val stats = PlayerAchievements(roamerWins = 20)
        val unlocked = Achievement.checkUnlocks(profile, stats)
        assertTrue(unlocked.contains(Achievement.ROAMER_MASTER))
    }

    @Test
    fun `NIGHT_OWL unlocks after 5 night wins`() {
        val profile = UserProfile()
        val stats = PlayerAchievements(nightWins = 5)
        val unlocked = Achievement.checkUnlocks(profile, stats)
        assertTrue(unlocked.contains(Achievement.NIGHT_OWL))
    }

    @Test
    fun `FIVE_STAR unlocks after maintaining high rating for 20 matches`() {
        val profile = UserProfile()
        val stats = PlayerAchievements(fiveStarMatches = 20)
        val unlocked = Achievement.checkUnlocks(profile, stats)
        assertTrue(unlocked.contains(Achievement.FIVE_STAR))
    }

    @Test
    fun `DAVID_VS_GOLIATH unlocks when hasUnderdogWin is true`() {
        val profile = UserProfile()
        val stats = PlayerAchievements(hasUnderdogWin = true)
        val unlocked = Achievement.checkUnlocks(profile, stats)
        assertTrue(unlocked.contains(Achievement.DAVID_VS_GOLIATH))
    }

    // ─── PlayerAchievements progress tests ───

    @Test
    fun `isUnlocked returns true when achievement id in list`() {
        val stats = PlayerAchievements(unlockedAchievements = listOf("first_scrim"))
        assertTrue(stats.isUnlocked(Achievement.FIRST_SCRIM))
    }

    @Test
    fun `isUnlocked returns false when achievement id not in list`() {
        val stats = PlayerAchievements()
        assertFalse(stats.isUnlocked(Achievement.FIRST_SCRIM))
    }

    @Test
    fun `getProgress returns maxProgress when unlocked`() {
        val stats = PlayerAchievements(unlockedAchievements = listOf("win_streak_5"))
        assertEquals(5, stats.getProgress(Achievement.WIN_STREAK_5))
    }

    @Test
    fun `getProgress returns current value when not unlocked`() {
        val stats = PlayerAchievements(bestWinStreak = 3)
        assertEquals(3, stats.getProgress(Achievement.WIN_STREAK_5))
    }

    @Test
    fun `getProgressPercentage returns 1_0 when unlocked`() {
        val stats = PlayerAchievements(unlockedAchievements = listOf("first_scrim"))
        assertEquals(1.0f, stats.getProgressPercentage(Achievement.FIRST_SCRIM), 0.001f)
    }

    @Test
    fun `getProgressPercentage returns correct fraction`() {
        val stats = PlayerAchievements(bestWinStreak = 3)
        assertEquals(0.6f, stats.getProgressPercentage(Achievement.WIN_STREAK_5), 0.001f)
    }

    @Test
    fun `getProgressPercentage returns 0 when maxProgress is 0`() {
        val stats = PlayerAchievements()
        // WinAtTier has maxProgress = 1 but returns 0 for progress
        assertEquals(0f, stats.getProgressPercentage(Achievement.LEGEND_WIN), 0.001f)
    }

    @Test
    fun `getProgressPercentage coerces above 1_0`() {
        val stats = PlayerAchievements(matchesPlayed = 200)
        assertEquals(1.0f, stats.getProgressPercentage(Achievement.FIRST_SCRIM), 0.001f)
    }

    @Test
    fun `all achievements have unique ids`() {
        val ids = Achievement.values().map { it.id }
        assertEquals(Achievement.values().size, ids.toSet().size)
    }

    @Test
    fun `all achievements have non-empty display names`() {
        Achievement.values().forEach { achievement ->
            assertTrue("Achievement ${achievement.id} should have non-empty displayName",
                achievement.displayName.isNotBlank())
        }
    }

    @Test
    fun `all achievements have non-empty descriptions`() {
        Achievement.values().forEach { achievement ->
            assertTrue("Achievement ${achievement.id} should have non-empty description",
                achievement.description.isNotBlank())
        }
    }
}

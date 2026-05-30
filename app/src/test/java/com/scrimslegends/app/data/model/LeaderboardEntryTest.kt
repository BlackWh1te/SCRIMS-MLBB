package com.scrimslegends.app.data.model

import org.junit.Assert.*
import org.junit.Test

class LeaderboardEntryTest {

    @Test
    fun `winRate returns correct percentage`() {
        val entry = LeaderboardEntry(wins = 75, totalMatches = 100)
        assertEquals("75%", entry.winRate)
    }

    @Test
    fun `winRate returns 0 percent when no matches`() {
        val entry = LeaderboardEntry()
        assertEquals("0%", entry.winRate)
    }

    @Test
    fun `winRate handles truncation`() {
        val entry = LeaderboardEntry(wins = 1, totalMatches = 3)
        assertEquals("33%", entry.winRate)
    }

    @Test
    fun `default currentTier is BRONZE`() {
        assertEquals(RankTier.BRONZE, LeaderboardEntry().currentTier)
    }

    @Test
    fun `default values are zero or empty`() {
        val entry = LeaderboardEntry()
        assertEquals(0, entry.rank)
        assertEquals("", entry.playerId)
        assertEquals("", entry.username)
        assertEquals("", entry.teamName)
        assertEquals(0, entry.xp)
        assertEquals(0, entry.wins)
        assertEquals(0, entry.losses)
        assertEquals(0, entry.totalMatches)
    }

    @Test
    fun `winRate uses integer division`() {
        val entry = LeaderboardEntry(wins = 2, totalMatches = 3)
        // 2 * 100 / 3 = 66 (integer division)
        assertEquals("66%", entry.winRate)
    }
}

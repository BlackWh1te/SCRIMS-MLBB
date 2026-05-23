package com.mlbb.scrim.data.model

import org.junit.Assert.*
import org.junit.Test

class PlayerTest {

    @Test
    fun `default player has zero stats`() {
        val player = Player()
        assertEquals(0, player.pts)
        assertEquals(0, player.wins)
        assertEquals(0, player.losses)
        assertEquals(0, player.matchesPlayed)
    }

    @Test
    fun `winRate returns correct percentage`() {
        val player = Player(wins = 8, losses = 2, matchesPlayed = 10)
        assertEquals(80f, player.winRate, 0.001f)
    }

    @Test
    fun `winRate returns 0 when no matches`() {
        val player = Player()
        assertEquals(0f, player.winRate, 0.001f)
    }

    @Test
    fun `winRateDisplay shows correct percentage`() {
        val player = Player(wins = 3, losses = 1, matchesPlayed = 4)
        assertEquals("75%", player.winRateDisplay)
    }

    @Test
    fun `winRateDisplay shows 0 percent when no matches`() {
        val player = Player()
        assertEquals("0%", player.winRateDisplay)
    }

    @Test
    fun `winRateDisplay handles truncation correctly`() {
        val player = Player(wins = 1, matchesPlayed = 3)
        // 1 * 100 / 3 = 33 (integer truncation)
        assertEquals("33%", player.winRateDisplay)
    }

    @Test
    fun `default role is MEMBER`() {
        assertEquals(PlayerRole.MEMBER, Player().role)
    }

    @Test
    fun `PlayerRole enum has correct values`() {
        assertEquals(3, PlayerRole.values().size)
        assertNotNull(PlayerRole.LEADER)
        assertNotNull(PlayerRole.CO_LEADER)
        assertNotNull(PlayerRole.MEMBER)
    }

    @Test
    fun `joinedAt defaults to current time`() {
        val before = System.currentTimeMillis()
        val player = Player()
        val after = System.currentTimeMillis()
        assertTrue(player.joinedAt in before..after)
    }
}

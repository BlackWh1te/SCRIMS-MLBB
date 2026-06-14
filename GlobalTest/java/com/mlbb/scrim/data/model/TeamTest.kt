package com.mlbb.scrim.data.model

import org.junit.Assert.*
import org.junit.Test

class TeamTest {

    @Test
    fun `default team has empty player list`() {
        val team = Team()
        assertEquals(0, team.currentPlayerCount)
    }

    @Test
    fun `currentPlayerCount returns correct size`() {
        val team = Team(players = listOf(
            Player(id = "1", name = "A"),
            Player(id = "2", name = "B"),
            Player(id = "3", name = "C")
        ))
        assertEquals(3, team.currentPlayerCount)
    }

    @Test
    fun `canAddPlayer returns true when below max`() {
        val team = Team(players = List(5) { Player(id = "$it", name = "P$it") })
        assertTrue(team.canAddPlayer)
    }

    @Test
    fun `canAddPlayer returns false when at max`() {
        val team = Team(players = List(7) { Player(id = "$it", name = "P$it") })
        assertFalse(team.canAddPlayer)
    }

    @Test
    fun `canAddPlayer returns false when over max`() {
        val team = Team(players = List(8) { Player(id = "$it", name = "P$it") })
        assertFalse(team.canAddPlayer)
    }

    @Test
    fun `isFull returns true at maxPlayers`() {
        val team = Team(players = List(7) { Player(id = "$it", name = "P$it") })
        assertTrue(team.isFull)
    }

    @Test
    fun `isFull returns false below maxPlayers`() {
        val team = Team(players = List(6) { Player(id = "$it", name = "P$it") })
        assertFalse(team.isFull)
    }

    @Test
    fun `meetsMinPlayers returns true at minPlayers`() {
        val team = Team(players = List(5) { Player(id = "$it", name = "P$it") })
        assertTrue(team.meetsMinPlayers)
    }

    @Test
    fun `meetsMinPlayers returns false below minPlayers`() {
        val team = Team(players = List(4) { Player(id = "$it", name = "P$it") })
        assertFalse(team.meetsMinPlayers)
    }

    @Test
    fun `meetsMinPlayers returns true above minPlayers`() {
        val team = Team(players = List(6) { Player(id = "$it", name = "P$it") })
        assertTrue(team.meetsMinPlayers)
    }

    @Test
    fun `isBannedFromPosting returns true when ban time is in future`() {
        val futureTime = System.currentTimeMillis() + 3600000
        val team = Team(canPostScrimsUntil = futureTime)
        assertTrue(team.isBannedFromPosting)
    }

    @Test
    fun `isBannedFromPosting returns false when ban time is in past`() {
        val pastTime = System.currentTimeMillis() - 3600000
        val team = Team(canPostScrimsUntil = pastTime)
        assertFalse(team.isBannedFromPosting)
    }

    @Test
    fun `isBannedFromPosting returns false for zero`() {
        val team = Team(canPostScrimsUntil = 0L)
        assertFalse(team.isBannedFromPosting)
    }

    @Test
    fun `displayReputation formats to one decimal place`() {
        val team = Team(reputation = 4.75f)
        assertEquals("4.8", team.displayReputation)
    }

    @Test
    fun `displayReputation coerces above maximum`() {
        val team = Team(reputation = 10.0f)
        assertEquals("5.0", team.displayReputation)
    }

    @Test
    fun `displayReputation coerces below minimum`() {
        val team = Team(reputation = 0.0f)
        assertEquals("1.0", team.displayReputation)
    }

    @Test
    fun `default maxPlayers is 7`() {
        assertEquals(7, Team().maxPlayers)
    }

    @Test
    fun `default minPlayers is 5`() {
        assertEquals(5, Team().minPlayers)
    }

    @Test
    fun `team defaults are reasonable`() {
        val team = Team()
        assertEquals("", team.id)
        assertEquals("", team.name)
        assertEquals("", team.leaderId)
        assertTrue(team.players.isEmpty())
        assertEquals(5.0f, team.reputation, 0.001f)
        assertNull(team.logoUrl)
        assertFalse(team.isOpenForApplications)
    }
}

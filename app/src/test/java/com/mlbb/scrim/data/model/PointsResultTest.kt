package com.mlbb.scrim.data.model

import com.mlbb.scrim.data.repository.PointsResult
import com.mlbb.scrim.data.repository.PlayerPointsChange
import org.junit.Assert.*
import org.junit.Test

class PointsResultTest {

    @Test
    fun `empty PointsResult has empty lists`() {
        val result = PointsResult.empty()
        assertTrue(result.teamAChanges.isEmpty())
        assertTrue(result.teamBChanges.isEmpty())
        assertTrue(result.teamASubstitutes.isEmpty())
        assertTrue(result.teamBSubstitutes.isEmpty())
        assertEquals("", result.winnerTeamId)
    }

    @Test
    fun `PlayerPointsChange defaults`() {
        val change = PlayerPointsChange()
        assertEquals("", change.playerId)
        assertEquals("", change.playerName)
        assertEquals("", change.teamId)
        assertEquals(0, change.pointsChange)
        assertFalse(change.isWinner)
        assertFalse(change.isSubstitute)
    }

    @Test
    fun `PlayerPointsChange with positive points`() {
        val change = PlayerPointsChange(
            playerId = "p1",
            playerName = "Player1",
            teamId = "teamA",
            pointsChange = 25,
            isWinner = true
        )
        assertEquals(25, change.pointsChange)
        assertTrue(change.isWinner)
    }

    @Test
    fun `PlayerPointsChange with negative points`() {
        val change = PlayerPointsChange(
            playerId = "p1",
            pointsChange = -15,
            isWinner = false
        )
        assertEquals(-15, change.pointsChange)
        assertFalse(change.isWinner)
    }

    @Test
    fun `PlayerPointsChange substitute has zero points`() {
        val change = PlayerPointsChange(
            playerId = "p1",
            pointsChange = 0,
            isWinner = false,
            isSubstitute = true
        )
        assertEquals(0, change.pointsChange)
        assertTrue(change.isSubstitute)
    }

    @Test
    fun `PointsResult with data preserves winner`() {
        val result = PointsResult(
            winnerTeamId = "teamA",
            teamAChanges = listOf(PlayerPointsChange(teamId = "teamA", pointsChange = 25)),
            teamBChanges = listOf(PlayerPointsChange(teamId = "teamB", pointsChange = -15)),
            teamASubstitutes = emptyList(),
            teamBSubstitutes = emptyList()
        )
        assertEquals("teamA", result.winnerTeamId)
        assertEquals(1, result.teamAChanges.size)
        assertEquals(1, result.teamBChanges.size)
    }
}

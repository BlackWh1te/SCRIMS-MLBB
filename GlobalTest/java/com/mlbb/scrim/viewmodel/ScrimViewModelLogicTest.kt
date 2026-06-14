package com.mlbb.scrim.viewmodel

import com.mlbb.scrim.data.model.Player
import com.mlbb.scrim.data.model.Scrim
import com.mlbb.scrim.data.model.ScrimRosterEntry
import com.mlbb.scrim.data.model.ScrimStatus
import com.mlbb.scrim.data.repository.PointsResult
import com.mlbb.scrim.data.repository.ScrimRepository
import com.mlbb.scrim.data.repository.PlayerPointsChange
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for pure logic inside ScrimViewModel and ScrimRepository
 * that do not require Android framework or Hilt injection.
 */
class ScrimViewModelLogicTest {

    // ─── buildDefaultRoster tests ───

    @Test
    fun `buildDefaultRoster marks all players as substitutes`() {
        val players = listOf(
            Player(id = "p1", name = "Alice"),
            Player(id = "p2", name = "Bob")
        )
        val roster = ScrimViewModel::class.java.getDeclaredMethod(
            "buildDefaultRoster", String::class.java, List::class.java
        ).apply { isAccessible = true }
            .invoke(null, "team1", players) as List<ScrimRosterEntry>

        assertEquals(2, roster.size)
        assertTrue(roster.all { !it.isActive })
        assertEquals("p1", roster[0].playerId)
        assertEquals("Alice", roster[0].playerName)
    }

    @Test
    fun `buildDefaultRoster returns empty list for empty team`() {
        val roster = ScrimViewModel::class.java.getDeclaredMethod(
            "buildDefaultRoster", String::class.java, List::class.java
        ).apply { isAccessible = true }
            .invoke(null, "team1", emptyList<Player>()) as List<ScrimRosterEntry>

        assertTrue(roster.isEmpty())
    }

    // ─── Points calculation tests ───

    @Test
    fun `calculatePointsChanges returns empty when no winner`() {
        val repo = ScrimRepository()
        val scrim = Scrim(teamId = "t1", winnerTeamId = null)
        val result = repo.calculatePointsChanges(scrim)
        assertTrue(result.allActiveChanges.isEmpty())
        assertEquals("", result.winnerTeamId)
    }

    @Test
    fun `calculatePointsChanges awards win points to active winners`() {
        val repo = ScrimRepository()
        val scrim = Scrim(
            teamId = "t1",
            teamAActiveRoster = listOf(
                ScrimRosterEntry(playerId = "p1", playerName = "Alice", teamId = "t1", isActive = true)
            ),
            teamBActiveRoster = listOf(
                ScrimRosterEntry(playerId = "p2", playerName = "Bob", teamId = "t2", isActive = true)
            ),
            winnerTeamId = "t1"
        )
        val result = repo.calculatePointsChanges(scrim)
        val aliceChange = result.teamAChanges.first { it.playerId == "p1" }
        val bobChange = result.teamBChanges.first { it.playerId == "p2" }

        assertEquals(ScrimRepository.PTS_PER_WIN, aliceChange.pointsChange)
        assertTrue(aliceChange.isWinner)
        assertEquals(-ScrimRepository.PTS_PER_LOSS, bobChange.pointsChange)
        assertFalse(bobChange.isWinner)
    }

    @Test
    fun `calculatePointsChanges gives zero points to substitutes`() {
        val repo = ScrimRepository()
        val scrim = Scrim(
            teamId = "t1",
            teamAActiveRoster = listOf(
                ScrimRosterEntry(playerId = "p1", playerName = "Alice", teamId = "t1", isActive = true)
            ),
            teamASubstitutes = listOf(
                ScrimRosterEntry(playerId = "p3", playerName = "Charlie", teamId = "t1", isActive = false)
            ),
            teamBActiveRoster = emptyList(),
            teamBSubstitutes = emptyList(),
            winnerTeamId = "t1"
        )
        val result = repo.calculatePointsChanges(scrim)
        val subChange = result.teamASubstitutes.first { it.playerId == "p3" }
        assertEquals(0, subChange.pointsChange)
        assertTrue(subChange.isSubstitute)
        assertFalse(subChange.isWinner)
    }

    @Test
    fun `calculatePointsChanges handles empty rosters`() {
        val repo = ScrimRepository()
        val scrim = Scrim(
            teamId = "t1",
            teamAActiveRoster = emptyList(),
            teamBActiveRoster = emptyList(),
            winnerTeamId = "t1"
        )
        val result = repo.calculatePointsChanges(scrim)
        assertTrue(result.teamAChanges.isEmpty())
        assertTrue(result.teamBChanges.isEmpty())
    }

    @Test
    fun `PointsResult empty has no changes`() {
        val empty = PointsResult.empty()
        assertTrue(empty.teamAChanges.isEmpty())
        assertTrue(empty.teamBChanges.isEmpty())
        assertTrue(empty.teamASubstitutes.isEmpty())
        assertTrue(empty.teamBSubstitutes.isEmpty())
        assertEquals("", empty.winnerTeamId)
    }

    @Test
    fun `PointsResult allActiveChanges aggregates both teams`() {
        val result = PointsResult(
            teamAChanges = listOf(PlayerPointsChange("p1", "A", "t1", 10, true)),
            teamBChanges = listOf(PlayerPointsChange("p2", "B", "t2", -5, false)),
            teamASubstitutes = emptyList(),
            teamBSubstitutes = emptyList(),
            winnerTeamId = "t1"
        )
        assertEquals(2, result.allActiveChanges.size)
    }

    // ─── Scrim constants security ───

    @Test
    fun `points per win is positive`() {
        assertTrue(ScrimRepository.PTS_PER_WIN > 0)
    }

    @Test
    fun `points per loss is positive`() {
        assertTrue(ScrimRepository.PTS_PER_LOSS > 0)
    }

    @Test
    fun `points per win is greater than points per loss`() {
        assertTrue(ScrimRepository.PTS_PER_WIN > ScrimRepository.PTS_PER_LOSS)
    }

    // ─── Scrim model security / edge cases ───

    @Test
    fun `Scrim copy does not mutate original`() {
        val original = Scrim(id = "1", teamId = "t1", status = ScrimStatus.OPEN)
        val copy = original.copy(status = ScrimStatus.FILLED)
        assertEquals(ScrimStatus.OPEN, original.status)
        assertEquals(ScrimStatus.FILLED, copy.status)
    }

    @Test
    fun `Scrim isChatOpen when scheduledTime is in past`() {
        val past = System.currentTimeMillis() - 1000
        val scrim = Scrim(scheduledTime = past, status = ScrimStatus.IN_PROGRESS)
        assertTrue(scrim.isChatOpen)
    }

    @Test
    fun `Scrim isResultOverdue when more than 2 hours past scheduled`() {
        val past = System.currentTimeMillis() - (3 * 60 * 60 * 1000)
        val scrim = Scrim(scheduledTime = past)
        assertTrue(scrim.isResultOverdue)
    }
}

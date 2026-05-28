package com.mlbb.scrim.data.model

import org.junit.Assert.*
import org.junit.Test

class ScrimTest {

    @Test
    fun `default scrim has OPEN status`() {
        val scrim = Scrim()
        assertEquals(ScrimStatus.OPEN, scrim.status)
    }

    @Test
    fun `default maxPlayers is 10`() {
        assertEquals(10, Scrim().maxPlayers)
    }

    @Test
    fun `default currentPlayers is 0`() {
        assertEquals(0, Scrim().currentPlayers)
    }

    @Test
    fun `bothReady returns true when both teams ready`() {
        val scrim = Scrim(teamAReady = true, teamBReady = true)
        assertTrue(scrim.bothReady)
    }

    @Test
    fun `bothReady returns false when only teamA ready`() {
        val scrim = Scrim(teamAReady = true, teamBReady = false)
        assertFalse(scrim.bothReady)
    }

    @Test
    fun `bothReady returns false when only teamB ready`() {
        val scrim = Scrim(teamAReady = false, teamBReady = true)
        assertFalse(scrim.bothReady)
    }

    @Test
    fun `bothReady returns false when neither ready`() {
        val scrim = Scrim(teamAReady = false, teamBReady = false)
        assertFalse(scrim.bothReady)
    }

    @Test
    fun `canCompleteScrim returns true when both ready and screenshot exists`() {
        val scrim = Scrim(
            teamAReady = true,
            teamBReady = true,
            teamAScreenshotUrl = "https://example.com/screenshot.png"
        )
        assertTrue(scrim.canCompleteScrim)
    }

    @Test
    fun `canCompleteScrim returns false when not both ready`() {
        val scrim = Scrim(
            teamAReady = true,
            teamBReady = false,
            teamAScreenshotUrl = "https://example.com/screenshot.png"
        )
        assertFalse(scrim.canCompleteScrim)
    }

    @Test
    fun `canCompleteScrim returns false when no screenshot`() {
        val scrim = Scrim(teamAReady = true, teamBReady = true)
        assertFalse(scrim.canCompleteScrim)
    }

    @Test
    fun `chatOpensAt is 2 hours before scheduled time`() {
        val scheduled = System.currentTimeMillis() + 3600000
        val scrim = Scrim(scheduledTime = scheduled)
        assertEquals(scheduled - (2 * 60 * 60 * 1000), scrim.chatOpensAt)
    }

    @Test
    fun `resultDeadline is 1 hour after scheduled time`() {
        val scheduled = System.currentTimeMillis()
        val scrim = Scrim(scheduledTime = scheduled)
        assertEquals(scheduled + (60 * 60 * 1000), scrim.resultDeadline)
    }

    @Test
    fun `autoCancelDeadline is 2 hours after scheduled time`() {
        val scheduled = System.currentTimeMillis()
        val scrim = Scrim(scheduledTime = scheduled)
        assertEquals(scheduled + (2 * 60 * 60 * 1000), scrim.autoCancelDeadline)
    }

    @Test
    fun `timeUntilChatOpens returns 0 when chat is already open`() {
        val past = System.currentTimeMillis() - 3600000
        val scrim = Scrim(scheduledTime = past)
        assertEquals(0L, scrim.timeUntilChatOpens)
    }

    @Test
    fun `timeUntilChatOpens returns positive when chat not yet open`() {
        val future = System.currentTimeMillis() + 10800000
        val scrim = Scrim(scheduledTime = future)
        assertTrue(scrim.timeUntilChatOpens > 0)
    }

    @Test
    fun `teamAActiveRoster filters only active players`() {
        val scrim = Scrim(teamARoster = listOf(
            ScrimRosterEntry(playerId = "1", isActive = true),
            ScrimRosterEntry(playerId = "2", isActive = false),
            ScrimRosterEntry(playerId = "3", isActive = true)
        ))
        assertEquals(2, scrim.teamAActiveRoster.size)
        assertTrue(scrim.teamAActiveRoster.all { it.isActive })
    }

    @Test
    fun `teamASubstitutes filters only inactive players`() {
        val scrim = Scrim(teamARoster = listOf(
            ScrimRosterEntry(playerId = "1", isActive = true),
            ScrimRosterEntry(playerId = "2", isActive = false),
            ScrimRosterEntry(playerId = "3", isActive = true)
        ))
        assertEquals(1, scrim.teamASubstitutes.size)
        assertFalse(scrim.teamASubstitutes.first().isActive)
    }

    @Test
    fun `empty rosters return empty lists`() {
        val scrim = Scrim()
        assertTrue(scrim.teamAActiveRoster.isEmpty())
        assertTrue(scrim.teamBActiveRoster.isEmpty())
        assertTrue(scrim.teamASubstitutes.isEmpty())
        assertTrue(scrim.teamBSubstitutes.isEmpty())
    }

    // ─── BestOf enum tests ───

    @Test
    fun `BestOf fromGames returns correct enum`() {
        assertEquals(BestOf.BO1, BestOf.fromGames(1))
        assertEquals(BestOf.BO3, BestOf.fromGames(3))
        assertEquals(BestOf.BO5, BestOf.fromGames(5))
    }

    @Test
    fun `BestOf fromGames defaults to BO1 for unknown value`() {
        assertEquals(BestOf.BO1, BestOf.fromGames(99))
    }

    @Test
    fun `BestOf games property matches enum`() {
        assertEquals(1, BestOf.BO1.games)
        assertEquals(3, BestOf.BO3.games)
        assertEquals(5, BestOf.BO5.games)
    }

    // ─── Region enum tests ───

    @Test
    fun `Region fromDisplayName returns correct region`() {
        assertEquals(Region.EU, Region.fromDisplayName("Europe"))
        assertEquals(Region.NA, Region.fromDisplayName("North America"))
        assertEquals(Region.ASIA, Region.fromDisplayName("Southeast Asia"))
    }

    @Test
    fun `Region fromDisplayName defaults to UTC for unknown name`() {
        assertEquals(Region.UTC, Region.fromDisplayName("Unknown"))
    }

    @Test
    fun `all regions have display names`() {
        Region.values().forEach { region ->
            assertTrue("Region ${region.name} should have non-empty displayName", region.displayName.isNotBlank())
        }
    }

    // ─── ScrimStatus enum tests ───

    @Test
    fun `ScrimStatus has all expected values`() {
        val expected = setOf(ScrimStatus.OPEN, ScrimStatus.FILLED, ScrimStatus.READY_CHECK,
            ScrimStatus.IN_PROGRESS, ScrimStatus.COMPLETED, ScrimStatus.CANCELLED)
        assertEquals(expected, ScrimStatus.values().toSet())
    }
}

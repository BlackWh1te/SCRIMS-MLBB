package com.mlbb.scrim.data.model

import org.junit.Assert.*
import org.junit.Test

class MatchResultTest {

    @Test
    fun `isDisputed returns true when teams report different winners`() {
        val result = MatchResult(
            teamAReport = TeamReport(reportedWinnerId = "teamA"),
            teamBReport = TeamReport(reportedWinnerId = "teamB")
        )
        assertTrue(result.isDisputed)
    }

    @Test
    fun `isDisputed returns false when teams report same winner`() {
        val result = MatchResult(
            teamAReport = TeamReport(reportedWinnerId = "teamA"),
            teamBReport = TeamReport(reportedWinnerId = "teamA")
        )
        assertFalse(result.isDisputed)
    }

    @Test
    fun `isDisputed returns false when only one report exists`() {
        val result = MatchResult(
            teamAReport = TeamReport(reportedWinnerId = "teamA"),
            teamBReport = null
        )
        assertFalse(result.isDisputed)
    }

    @Test
    fun `isDisputed returns false when no reports exist`() {
        val result = MatchResult()
        assertFalse(result.isDisputed)
    }

    @Test
    fun `isConfirmed returns true when status is CONFIRMED`() {
        val result = MatchResult(verificationStatus = VerificationStatus.CONFIRMED)
        assertTrue(result.isConfirmed)
    }

    @Test
    fun `isConfirmed returns false for other statuses`() {
        val disputed = MatchResult(verificationStatus = VerificationStatus.DISPUTED)
        val pending = MatchResult(verificationStatus = VerificationStatus.PENDING)
        assertFalse(disputed.isConfirmed)
        assertFalse(pending.isConfirmed)
    }

    @Test
    fun `bothTeamsReported returns true when both reports exist`() {
        val result = MatchResult(
            teamAReport = TeamReport(),
            teamBReport = TeamReport()
        )
        assertTrue(result.bothTeamsReported)
    }

    @Test
    fun `bothTeamsReported returns false when only one report exists`() {
        val result = MatchResult(teamAReport = TeamReport())
        assertFalse(result.bothTeamsReported)
    }

    @Test
    fun `pendingReporterTeamId returns teamAId when teamA missing report`() {
        val result = MatchResult(
            teamAId = "teamA",
            teamBId = "teamB",
            teamBReport = TeamReport()
        )
        assertEquals("teamA", result.pendingReporterTeamId)
    }

    @Test
    fun `pendingReporterTeamId returns teamBId when teamB missing report`() {
        val result = MatchResult(
            teamAId = "teamA",
            teamBId = "teamB",
            teamAReport = TeamReport()
        )
        assertEquals("teamB", result.pendingReporterTeamId)
    }

    @Test
    fun `pendingReporterTeamId returns null when both reported`() {
        val result = MatchResult(
            teamAId = "teamA",
            teamBId = "teamB",
            teamAReport = TeamReport(),
            teamBReport = TeamReport()
        )
        assertNull(result.pendingReporterTeamId)
    }

    @Test
    fun `default verificationStatus is PENDING`() {
        assertEquals(VerificationStatus.PENDING, MatchResult().verificationStatus)
    }

    @Test
    fun `VerificationStatus has expected values`() {
        val values = VerificationStatus.values()
        assertTrue(values.contains(VerificationStatus.PENDING))
        assertTrue(values.contains(VerificationStatus.CONFIRMED))
        assertTrue(values.contains(VerificationStatus.DISPUTED))
        assertTrue(values.contains(VerificationStatus.ADMIN_REVIEW))
        assertTrue(values.contains(VerificationStatus.AUTO_CANCELLED))
        assertTrue(values.contains(VerificationStatus.ADMIN_RESOLVED))
    }

    @Test
    fun `AdminVerdict has expected values`() {
        val values = AdminVerdict.values()
        assertTrue(values.contains(AdminVerdict.TEAM_A_NO_SHOW))
        assertTrue(values.contains(AdminVerdict.TEAM_B_NO_SHOW))
        assertTrue(values.contains(AdminVerdict.BOTH_NO_SHOW))
        assertTrue(values.contains(AdminVerdict.MATCH_PLAYED))
        assertTrue(values.contains(AdminVerdict.TECHNICAL_ISSUE))
        assertTrue(values.contains(AdminVerdict.INVALID_REPORT))
        assertTrue(values.contains(AdminVerdict.UNDER_REVIEW))
    }

    @Test
    fun `RosterPlayerInfo default values`() {
        val info = RosterPlayerInfo()
        assertEquals("", info.playerId)
        assertEquals("", info.playerName)
        assertFalse(info.isActive)
        assertFalse(info.isWinner)
        assertEquals(0, info.pointsChange)
    }
}

package com.mlbb.scrim.data.repository

import com.mlbb.scrim.data.model.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class MatchResultRepositoryTest {

    private lateinit var repository: MatchResultRepository

    @Before
    fun setup() {
        repository = MatchResultRepository()
    }

    // ─── Get tests ───

    @Test
    fun `getAllMatchResults returns sample data`() = runBlocking {
        val result = repository.getAllMatchResults().first()
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()?.isNotEmpty() == true)
    }

    @Test
    fun `getMatchResultById returns existing`() = runBlocking {
        val all = repository.getAllMatchResults().first().getOrNull()!!
        val first = all.first()
        val result = repository.getMatchResultById(first.id).first()
        assertTrue(result.isSuccess)
        assertNotNull(result.getOrNull())
    }

    @Test
    fun `getMatchResultById returns null for nonexistent`() = runBlocking {
        val result = repository.getMatchResultById("nonexistent").first()
        assertTrue(result.isSuccess)
        assertNull(result.getOrNull())
    }

    @Test
    fun `getMatchResultsForTeam filters correctly`() = runBlocking {
        val result = repository.getMatchResultsForTeam("team1").first()
        assertTrue(result.isSuccess)
        val matches = result.getOrNull()!!
        assertTrue(matches.all { it.teamAId == "team1" || it.teamBId == "team1" })
    }

    @Test
    fun `getMatchResultsForScrim returns match`() = runBlocking {
        val result = repository.getMatchResultsForScrim("scrim1").first()
        assertTrue(result.isSuccess)
        assertNotNull(result.getOrNull())
    }

    // ─── Report result tests ───

    @Test
    fun `reportResult adds teamA report`() = runBlocking {
        val all = repository.getAllMatchResults().first().getOrNull()!!
        val match = all.first { it.verificationStatus == VerificationStatus.PENDING }
        val result = repository.reportResult(
            scrimId = match.scrimId,
            teamId = match.teamAId,
            reporterId = "player1",
            reporterName = "Player1",
            reportedWinnerId = match.teamAId
        ).first()
        assertTrue(result.isSuccess)
        assertNotNull(result.getOrNull()?.teamAReport)
    }

    @Test
    fun `reportResult returns failure for nonexistent match`() = runBlocking {
        val result = repository.reportResult(
            scrimId = "nonexistent",
            teamId = "team1",
            reporterId = "player1",
            reporterName = "Player1",
            reportedWinnerId = "team1"
        ).first()
        assertTrue(result.isFailure)
    }

    @Test
    fun `reportResult returns failure for unrelated team`() = runBlocking {
        val all = repository.getAllMatchResults().first().getOrNull()!!
        val match = all.first()
        val result = repository.reportResult(
            scrimId = match.scrimId,
            teamId = "unrelated_team",
            reporterId = "player1",
            reporterName = "Player1",
            reportedWinnerId = "team1"
        ).first()
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("not part") == true)
    }

    @Test
    fun `reportResult confirms when both teams agree`() = runBlocking {
        val all = repository.getAllMatchResults().first().getOrNull()!!
        val match = all.first { it.verificationStatus == VerificationStatus.PENDING }

        repository.reportResult(match.scrimId, match.teamAId, "p1", "P1", match.teamAId).first()
        val result = repository.reportResult(match.scrimId, match.teamBId, "p2", "P2", match.teamAId).first()

        assertTrue(result.isSuccess)
        assertEquals(VerificationStatus.CONFIRMED, result.getOrNull()?.verificationStatus)
        assertEquals(match.teamAId, result.getOrNull()?.confirmedWinnerId)
        assertNotNull(result.getOrNull()?.resolvedAt)
    }

    @Test
    fun `reportResult disputes when teams disagree`() = runBlocking {
        val all = repository.getAllMatchResults().first().getOrNull()!!
        val match = all.first { it.verificationStatus == VerificationStatus.PENDING }

        repository.reportResult(match.scrimId, match.teamAId, "p1", "P1", match.teamAId).first()
        val result = repository.reportResult(match.scrimId, match.teamBId, "p2", "P2", match.teamBId).first()

        assertTrue(result.isSuccess)
        assertEquals(VerificationStatus.DISPUTED, result.getOrNull()?.verificationStatus)
        assertNull(result.getOrNull()?.confirmedWinnerId)
    }

    // ─── Create match result tests ───

    @Test
    fun `createMatchResult creates new match`() = runBlocking {
        val result = repository.createMatchResult(
            scrimId = "newScrim",
            teamAId = "teamA",
            teamAName = "Team A",
            teamBId = "teamB",
            teamBName = "Team B"
        ).first()
        assertTrue(result.isSuccess)
        assertNotNull(result.getOrNull()?.id)
        assertEquals("newScrim", result.getOrNull()?.scrimId)
    }

    // ─── Resolve dispute tests ───

    @Test
    fun `resolveDispute updates status to CONFIRMED`() = runBlocking {
        val all = repository.getAllMatchResults().first().getOrNull()!!
        val match = all.first { it.verificationStatus == VerificationStatus.DISPUTED }
        val result = repository.resolveDispute(match.id, match.teamAId, "Admin resolved").first()
        assertTrue(result.isSuccess)
        assertEquals(VerificationStatus.CONFIRMED, result.getOrNull()?.verificationStatus)
        assertEquals(match.teamAId, result.getOrNull()?.confirmedWinnerId)
        assertEquals("Admin resolved", result.getOrNull()?.adminNotes)
        assertNotNull(result.getOrNull()?.resolvedAt)
    }

    @Test
    fun `resolveDispute fails for nonexistent match`() = runBlocking {
        val result = repository.resolveDispute("nonexistent", "teamA", null).first()
        assertTrue(result.isFailure)
    }

    // ─── Upload screenshot tests ───

    @Test
    fun `uploadScreenshot updates url`() = runBlocking {
        val all = repository.getAllMatchResults().first().getOrNull()!!
        val match = all.first()
        val result = repository.uploadScreenshot(match.id, "https://example.com/new.png").first()
        assertTrue(result.isSuccess)
        assertEquals("https://example.com/new.png", result.getOrNull()?.screenshotUrl)
    }

    @Test
    fun `uploadScreenshot fails for nonexistent match`() = runBlocking {
        val result = repository.uploadScreenshot("nonexistent", "url").first()
        assertTrue(result.isFailure)
    }
}

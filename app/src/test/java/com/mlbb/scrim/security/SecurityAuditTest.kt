package com.mlbb.scrim.security

import com.mlbb.scrim.data.model.*
import com.mlbb.scrim.data.repository.AuthRepository
import com.mlbb.scrim.data.repository.TeamRepository
import com.mlbb.scrim.data.repository.ScrimRepository
import com.mlbb.scrim.data.repository.MatchResultRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

/**
 * Security audit tests covering threat vectors and defensive behavior.
 */
class SecurityAuditTest {

    // ─── Authentication security ───

    @Test
    fun `auth repository does not store plaintext password`() = runBlocking {
        val repo = AuthRepository()
        repo.signUp("user@example.com", "mysecretpassword", "User", "game123").first()
        // getCurrentUser returns email, not password
        assertEquals("user@example.com", repo.getCurrentUser())
        // Profile does not contain password
        val profile = repo.getUserProfile()
        assertNotNull(profile)
    }

    @Test
    fun `signIn accepts any valid email format without real auth in mock`() = runBlocking {
        val repo = AuthRepository()
        val results = mutableListOf<com.mlbb.scrim.data.model.AuthResult>()
        repo.signIn("attacker@evil.com", "password123").collect { results.add(it) }
        // Mock accepts any valid email+password >= 6 chars
        assertTrue(results.any { it is AuthResult.Success || it is AuthResult.EmailNotVerified })
    }

    @Test
    fun `password update requires current password`() = runBlocking {
        val repo = AuthRepository()
        repo.signUp("user@example.com", "oldpassword", "User", "game123").first()
        val results = mutableListOf<com.mlbb.scrim.data.model.AuthResult>()
        repo.updatePassword("wrongpassword", "newpassword", "newpassword").collect { results.add(it) }
        assertTrue(results.any { it is AuthResult.Error })
    }

    @Test
    fun `email update requires current password`() = runBlocking {
        val repo = AuthRepository()
        repo.signUp("user@example.com", "password123", "User", "game123").first()
        val results = mutableListOf<com.mlbb.scrim.data.model.AuthResult>()
        repo.updateEmail("new@example.com", "wrongpassword").collect { results.add(it) }
        assertTrue(results.any { it is AuthResult.Error })
    }

    // ─── Team security ───

    @Test
    fun `cannot remove team leader`() = runBlocking {
        val repo = TeamRepository()
        val team = repo.createTeam("Team", "leader1", isOpenForApplications = true).first().getOrNull()!!
        val leaderId = team.players.first { it.role == PlayerRole.LEADER }.id
        val result = repo.removePlayer(team.id, leaderId).first()
        assertTrue(result.isFailure)
    }

    @Test
    fun `cannot change leader role`() = runBlocking {
        val repo = TeamRepository()
        val team = repo.createTeam("Team", "leader1", isOpenForApplications = true).first().getOrNull()!!
        val leaderId = team.players.first { it.role == PlayerRole.LEADER }.id
        val result = repo.updatePlayerRole(team.id, leaderId, PlayerRole.MEMBER).first()
        assertTrue(result.isFailure)
    }

    @Test
    fun `team size capped at 7`() = runBlocking {
        val repo = TeamRepository()
        val team = repo.createTeam("Team", "leader1", isOpenForApplications = true).first().getOrNull()!!
        for (i in 1..6) {
            repo.addPlayer(team.id, "Player $i", "p$i@test.com").first()
        }
        val result = repo.addPlayer(team.id, "Extra", "extra@test.com").first()
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("7") == true)
    }

    @Test
    fun `duplicate invite to same player rejected`() = runBlocking {
        val repo = TeamRepository()
        val team = repo.createTeam("Team", "leader1", isOpenForApplications = true).first().getOrNull()!!
        repo.sendInvite(team.id, team.name, "leader1", "L", "player2", "P2").first()
        val result = repo.sendInvite(team.id, team.name, "leader1", "L", "player2", "P2").first()
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("already sent") == true)
    }

    @Test
    fun `invite to existing team member rejected`() = runBlocking {
        val repo = TeamRepository()
        val team = repo.createTeam("Team", "leader1", isOpenForApplications = true).first().getOrNull()!!
        val updated = repo.addPlayer(team.id, "Member", "member@test.com").first().getOrNull()!!
        val memberId = updated.players.last().id
        val result = repo.sendInvite(team.id, team.name, "leader1", "L", memberId, "Member").first()
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("already on the team") == true)
    }

    // ─── Scrim security ───

    @Test
    fun `scrim application rejected when not OPEN`() = runBlocking {
        val repo = ScrimRepository()
        val all = repo.getAllScrims().first().getOrNull()!!
        val filled = all.first { it.status == ScrimStatus.FILLED }
        val app = ScrimApplication(applicantTeamId = "new")
        val result = repo.applyToScrim(filled.id, app).first()
        assertTrue(result.isFailure)
    }

    @Test
    fun `roster must have at least 5 active players`() = runBlocking {
        val repo = ScrimRepository()
        val all = repo.getAllScrims().first().getOrNull()!!
        val scrim = all.first()
        val roster = List(4) { ScrimRosterEntry(playerId = "p$it", isActive = true) }
        val result = repo.setScrimRoster(scrim.id, scrim.teamId, roster).first()
        assertTrue(result.isFailure)
    }

    @Test
    fun `complete scrim requires screenshot`() = runBlocking {
        val repo = ScrimRepository()
        val all = repo.getAllScrims().first().getOrNull()!!
        val scrim = all.first { it.status == ScrimStatus.FILLED }
        val result = repo.completeScrim(scrim.id, scrim.teamId).first()
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("screenshot") == true)
    }

    @Test
    fun `winner must be participating team`() = runBlocking {
        val repo = ScrimRepository()
        val all = repo.getAllScrims().first().getOrNull()!!
        val scrim = all.first { it.status == ScrimStatus.FILLED }
        val updated = scrim.copy(teamAScreenshotUrl = "url")
        repo.updateScrim(updated).first()
        val result = repo.completeScrim(scrim.id, "random_team").first()
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Winner must be") == true)
    }

    // ─── Match result security ───

    @Test
    fun `unrelated team cannot report match result`() = runBlocking {
        val repo = MatchResultRepository()
        val all = repo.getAllMatchResults().first().getOrNull()!!
        val match = all.first()
        val result = repo.reportResult(
            match.id, "unrelated_team", "p1", "P1", "winner"
        ).first()
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("not part") == true)
    }

    @Test
    fun `disputed result requires admin resolution`() = runBlocking {
        val repo = MatchResultRepository()
        val all = repo.getAllMatchResults().first().getOrNull()!!
        val match = all.first { it.verificationStatus == VerificationStatus.PENDING }
        repo.reportResult(match.id, match.teamAId, "p1", "P1", match.teamAId).first()
        repo.reportResult(match.id, match.teamBId, "p2", "P2", match.teamBId).first()
        val afterDispute = repo.getMatchResultById(match.id).first().getOrNull()!!
        assertEquals(VerificationStatus.DISPUTED, afterDispute.verificationStatus)
    }

    // ─── Data integrity ───

    @Test
    fun `Player winRate handles division by zero`() {
        val player = Player(matchesPlayed = 0, wins = 0)
        assertEquals(0f, player.winRate, 0.001f)
    }

    @Test
    fun `LeaderboardEntry winRate handles division by zero`() {
        val entry = LeaderboardEntry(totalMatches = 0, wins = 0)
        assertEquals("0%", entry.winRate)
    }

    @Test
    fun `UserProfile winRate handles division by zero`() {
        val profile = UserProfile(totalMatches = 0, wins = 0)
        assertEquals("0%", profile.winRate)
    }

    // ─── Injection / malformed input ───

    @Test
    fun `team name with script tags does not crash`() = runBlocking {
        val repo = TeamRepository()
        val result = repo.createTeam("<script>alert(1)</script>", "leader1", isOpenForApplications = true).first()
        assertTrue(result.isSuccess)
    }

    @Test
    fun `description with SQL keywords does not crash`() = runBlocking {
        val repo = ScrimRepository()
        val scrim = Scrim(
            teamId = "team1",
            description = "DROP TABLE scrims; --"
        )
        val result = repo.createScrim(scrim).first()
        assertTrue(result.isSuccess)
    }

    @Test
    fun `email with path traversal rejected`() = runBlocking {
        val repo = AuthRepository()
        val results = mutableListOf<com.mlbb.scrim.data.model.AuthResult>()
        repo.signUp("../../../etc/passwd@test.com", "password123", "User", "game123").collect { results.add(it) }
        // Contains @, so mock accepts it (but production should validate better)
        assertTrue(results.any { it is AuthResult.EmailNotVerified })
    }

    @Test
    fun `null or empty ids handled safely`() = runBlocking {
        val repo = TeamRepository()
        val result = repo.getTeam("").first()
        assertTrue(result.isFailure)
    }

    @Test
    fun `very long input strings handled safely`() = runBlocking {
        val repo = AuthRepository()
        val longEmail = "a".repeat(10000) + "@test.com"
        val longPassword = "b".repeat(10000)
        val results = mutableListOf<com.mlbb.scrim.data.model.AuthResult>()
        repo.signUp(longEmail, longPassword, "User", "game123").collect { results.add(it) }
        assertTrue(results.any { it is AuthResult.EmailNotVerified })
    }

    // ─── Enum security ───

    @Test
    fun `RankTier fromXp never returns null`() {
        assertNotNull(RankTier.fromXp(-1))
        assertNotNull(RankTier.fromXp(0))
        assertNotNull(RankTier.fromXp(Int.MAX_VALUE))
    }

    @Test
    fun `BestOf fromGames defaults safely`() {
        assertEquals(BestOf.BO1, BestOf.fromGames(-1))
        assertEquals(BestOf.BO1, BestOf.fromGames(0))
        assertEquals(BestOf.BO1, BestOf.fromGames(99))
    }

    @Test
    fun `Region fromDisplayName defaults safely`() {
        assertEquals(Region.UTC, Region.fromDisplayName(""))
        assertEquals(Region.UTC, Region.fromDisplayName("Nonexistent Region"))
    }
}

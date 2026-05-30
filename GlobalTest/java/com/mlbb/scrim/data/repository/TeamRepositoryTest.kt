package com.mlbb.scrim.data.repository

import com.mlbb.scrim.data.model.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class TeamRepositoryTest {

    private lateinit var repository: TeamRepository

    @Before
    fun setup() {
        repository = TeamRepository()
    }

    // ─── Create team tests ───

    @Test
    fun `createTeam returns success`() = runBlocking {
        val result = repository.createTeam("Test Team", "leader1", isOpenForApplications = true).first()
        assertTrue(result.isSuccess)
    }

    @Test
    fun `createTeam adds team to repository`() = runBlocking {
        repository.createTeam("Test Team", "leader1", isOpenForApplications = true).first()
        val teams = repository.getTeams().first().getOrNull()
        assertEquals(1, teams?.size)
    }

    @Test
    fun `createTeam sets currentTeamId`() = runBlocking {
        val result = repository.createTeam("Test Team", "leader1", isOpenForApplications = true).first()
        val team = result.getOrNull()
        assertEquals(team?.id, repository.getCurrentTeamId())
    }

    @Test
    fun `createTeam team has leader player`() = runBlocking {
        val result = repository.createTeam("Test Team", "leader1", isOpenForApplications = true).first()
        val team = result.getOrNull()
        assertEquals(1, team?.players?.size)
        assertEquals(PlayerRole.LEADER, team?.players?.first()?.role)
    }

    // ─── Get team tests ───

    @Test
    fun `getTeam returns existing team`() = runBlocking {
        val created = repository.createTeam("Test Team", "leader1", isOpenForApplications = true).first().getOrNull()!!
        val result = repository.getTeam(created.id).first()
        assertTrue(result.isSuccess)
        assertEquals("Test Team", result.getOrNull()?.name)
    }

    @Test
    fun `getTeam returns failure for nonexistent id`() = runBlocking {
        val result = repository.getTeam("nonexistent").first()
        assertTrue(result.isFailure)
    }

    // ─── Add player tests ───

    @Test
    fun `addPlayer increases player count`() = runBlocking {
        val team = repository.createTeam("Test Team", "leader1", isOpenForApplications = true).first().getOrNull()!!
        val result = repository.addPlayer(team.id, "New Player", "player@example.com").first()
        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrNull()?.players?.size)
    }

    @Test
    fun `addPlayer fails when team is full`() = runBlocking {
        val team = repository.createTeam("Test Team", "leader1", isOpenForApplications = true).first().getOrNull()!!
        // Add players until full (max 7, already has 1 leader)
        for (i in 1..6) {
            repository.addPlayer(team.id, "Player $i", "p$i@example.com").first()
        }
        val result = repository.addPlayer(team.id, "Extra Player", "extra@example.com").first()
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("full") == true)
    }

    @Test
    fun `addPlayer fails for nonexistent team`() = runBlocking {
        val result = repository.addPlayer("nonexistent", "Player", "p@example.com").first()
        assertTrue(result.isFailure)
    }

    // ─── Remove player tests ───

    @Test
    fun `removePlayer decreases player count`() = runBlocking {
        val team = repository.createTeam("Test Team", "leader1", isOpenForApplications = true).first().getOrNull()!!
        val updated = repository.addPlayer(team.id, "New Player", "player@example.com").first().getOrNull()!!
        val playerToRemove = updated.players.last().id
        val result = repository.removePlayer(team.id, playerToRemove).first()
        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()?.players?.size)
    }

    @Test
    fun `removePlayer fails when removing leader`() = runBlocking {
        val team = repository.createTeam("Test Team", "leader1", isOpenForApplications = true).first().getOrNull()!!
        val leaderId = team.players.first { it.role == PlayerRole.LEADER }.id
        val result = repository.removePlayer(team.id, leaderId).first()
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("leader") == true)
    }

    @Test
    fun `removePlayer fails for nonexistent team`() = runBlocking {
        val result = repository.removePlayer("nonexistent", "player1").first()
        assertTrue(result.isFailure)
    }

    // ─── Update player role tests ───

    @Test
    fun `updatePlayerRole changes member role`() = runBlocking {
        val team = repository.createTeam("Test Team", "leader1", isOpenForApplications = true).first().getOrNull()!!
        val updated = repository.addPlayer(team.id, "New Player", "player@example.com").first().getOrNull()!!
        val memberId = updated.players.last().id
        val result = repository.updatePlayerRole(team.id, memberId, PlayerRole.CO_LEADER).first()
        assertTrue(result.isSuccess)
        assertEquals(PlayerRole.CO_LEADER, result.getOrNull()?.players?.last()?.role)
    }

    @Test
    fun `updatePlayerRole fails when changing leader role`() = runBlocking {
        val team = repository.createTeam("Test Team", "leader1", isOpenForApplications = true).first().getOrNull()!!
        val leaderId = team.players.first { it.role == PlayerRole.LEADER }.id
        val result = repository.updatePlayerRole(team.id, leaderId, PlayerRole.MEMBER).first()
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("leader") == true)
    }

    @Test
    fun `updatePlayerRole fails for nonexistent player`() = runBlocking {
        val team = repository.createTeam("Test Team", "leader1", isOpenForApplications = true).first().getOrNull()!!
        val result = repository.updatePlayerRole(team.id, "nonexistent", PlayerRole.CO_LEADER).first()
        assertTrue(result.isFailure)
    }

    // ─── Delete team tests ───

    @Test
    fun `deleteTeam removes team`() = runBlocking {
        val team = repository.createTeam("Test Team", "leader1", isOpenForApplications = true).first().getOrNull()!!
        val result = repository.deleteTeam(team.id).first()
        assertTrue(result.isSuccess)
        val teams = repository.getTeams().first().getOrNull()
        assertTrue(teams?.isEmpty() == true)
    }

    // ─── Invite flow tests ───

    @Test
    fun `sendInvite returns success`() = runBlocking {
        val team = repository.createTeam("Test Team", "leader1", isOpenForApplications = true).first().getOrNull()!!
        val result = repository.sendInvite(
            teamId = team.id,
            teamName = team.name,
            invitedBy = "leader1",
            invitedByName = "Leader",
            invitedUserId = "player2",
            invitedUserName = "Player 2"
        ).first()
        assertTrue(result.isSuccess)
    }

    @Test
    fun `sendInvite fails for nonexistent team`() = runBlocking {
        val result = repository.sendInvite(
            teamId = "nonexistent",
            teamName = "Fake",
            invitedBy = "leader1",
            invitedByName = "Leader",
            invitedUserId = "player2",
            invitedUserName = "Player 2"
        ).first()
        assertTrue(result.isFailure)
    }

    @Test
    fun `sendInvite fails when team is full`() = runBlocking {
        val team = repository.createTeam("Test Team", "leader1", isOpenForApplications = true).first().getOrNull()!!
        for (i in 1..6) {
            repository.addPlayer(team.id, "Player $i", "p$i@example.com").first()
        }
        val result = repository.sendInvite(
            teamId = team.id,
            teamName = team.name,
            invitedBy = "leader1",
            invitedByName = "Leader",
            invitedUserId = "player2",
            invitedUserName = "Player 2"
        ).first()
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("full") == true)
    }

    @Test
    fun `sendInvite fails for duplicate pending invite`() = runBlocking {
        val team = repository.createTeam("Test Team", "leader1", isOpenForApplications = true).first().getOrNull()!!
        repository.sendInvite(team.id, team.name, "leader1", "Leader", "player2", "Player 2").first()
        val result = repository.sendInvite(
            teamId = team.id,
            teamName = team.name,
            invitedBy = "leader1",
            invitedByName = "Leader",
            invitedUserId = "player2",
            invitedUserName = "Player 2"
        ).first()
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("already sent") == true)
    }

    @Test
    fun `sendInvite fails when player already on team`() = runBlocking {
        val team = repository.createTeam("Test Team", "leader1", isOpenForApplications = true).first().getOrNull()!!
        val updated = repository.addPlayer(team.id, "Existing", "existing@example.com").first().getOrNull()!!
        val existingId = updated.players.last().id
        val result = repository.sendInvite(
            teamId = team.id,
            teamName = team.name,
            invitedBy = "leader1",
            invitedByName = "Leader",
            invitedUserId = existingId,
            invitedUserName = "Existing"
        ).first()
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("already on the team") == true)
    }

    @Test
    fun `acceptInvite adds player to team`() = runBlocking {
        val team = repository.createTeam("Test Team", "leader1", isOpenForApplications = true).first().getOrNull()!!
        val invite = repository.sendInvite(team.id, team.name, "leader1", "Leader", "player2", "Player 2").first().getOrNull()!!
        val result = repository.acceptInvite(invite.id).first()
        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrNull()?.players?.size)
    }

    @Test
    fun `acceptInvite fails for nonexistent invite`() = runBlocking {
        val result = repository.acceptInvite("nonexistent").first()
        assertTrue(result.isFailure)
    }

    @Test
    fun `acceptInvite fails when invite not pending`() = runBlocking {
        val team = repository.createTeam("Test Team", "leader1", isOpenForApplications = true).first().getOrNull()!!
        val invite = repository.sendInvite(team.id, team.name, "leader1", "Leader", "player2", "Player 2").first().getOrNull()!!
        repository.declineInvite(invite.id).first()
        val result = repository.acceptInvite(invite.id).first()
        assertTrue(result.isFailure)
    }

    @Test
    fun `acceptInvite expires when team becomes full`() = runBlocking {
        val team = repository.createTeam("Test Team", "leader1", isOpenForApplications = true).first().getOrNull()!!
        val invite = repository.sendInvite(team.id, team.name, "leader1", "Leader", "player2", "Player 2").first().getOrNull()!!
        // Fill team while invite is pending
        for (i in 1..6) {
            repository.addPlayer(team.id, "Player $i", "p$i@example.com").first()
        }
        val result = repository.acceptInvite(invite.id).first()
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("full") == true)
    }

    @Test
    fun `declineInvite updates status`() = runBlocking {
        val team = repository.createTeam("Test Team", "leader1", isOpenForApplications = true).first().getOrNull()!!
        val invite = repository.sendInvite(team.id, team.name, "leader1", "Leader", "player2", "Player 2").first().getOrNull()!!
        val result = repository.declineInvite(invite.id).first()
        assertTrue(result.isSuccess)
    }

    @Test
    fun `getInvitesForPlayer returns pending invites`() = runBlocking {
        val team = repository.createTeam("Test Team", "leader1", isOpenForApplications = true).first().getOrNull()!!
        repository.sendInvite(team.id, team.name, "leader1", "Leader", "player2", "Player 2").first()
        val result = repository.getInvitesForPlayer("player2").first()
        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()?.size)
    }

    // ─── Application flow tests ───

    @Test
    fun `applyToTeam creates application`() = runBlocking {
        val team = repository.createTeam("Test Team", "leader1", isOpenForApplications = true).first().getOrNull()!!
        val result = repository.applyToTeam(team.id, "applicant1", "Please let me join").first()
        assertTrue(result.isSuccess)
        assertEquals(TeamApplicationStatus.PENDING, result.getOrNull()?.status)
    }

    @Test
    fun `applyToTeam fails when team not open`() = runBlocking {
        val team = repository.createTeam("Test Team", "leader1", isOpenForApplications = false).first().getOrNull()!!
        val result = repository.applyToTeam(team.id, "applicant1", "Please let me join").first()
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("not accepting") == true)
    }

    @Test
    fun `applyToTeam fails when already member`() = runBlocking {
        val team = repository.createTeam("Test Team", "leader1", isOpenForApplications = true).first().getOrNull()!!
        val updated = repository.addPlayer(team.id, "Member", "member@example.com").first().getOrNull()!!
        val memberId = updated.players.last().id
        val result = repository.applyToTeam(team.id, memberId, "Please let me join").first()
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("already a member") == true)
    }

    @Test
    fun `applyToTeam fails for duplicate pending application`() = runBlocking {
        val team = repository.createTeam("Test Team", "leader1", isOpenForApplications = true).first().getOrNull()!!
        repository.applyToTeam(team.id, "applicant1", "First application").first()
        val result = repository.applyToTeam(team.id, "applicant1", "Second application").first()
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("already have a pending") == true)
    }

    @Test
    fun `acceptApplication adds player to team`() = runBlocking {
        val team = repository.createTeam("Test Team", "leader1", isOpenForApplications = true).first().getOrNull()!!
        val app = repository.applyToTeam(team.id, "applicant1", "Please let me join").first().getOrNull()!!
        val result = repository.acceptApplication(app.id).first()
        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrNull()?.players?.size)
    }

    @Test
    fun `acceptApplication fails when team full`() = runBlocking {
        val team = repository.createTeam("Test Team", "leader1", isOpenForApplications = true).first().getOrNull()!!
        val app = repository.applyToTeam(team.id, "applicant1", "Please let me join").first().getOrNull()!!
        for (i in 1..6) {
            repository.addPlayer(team.id, "Player $i", "p$i@example.com").first()
        }
        val result = repository.acceptApplication(app.id).first()
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("full") == true)
    }

    @Test
    fun `declineApplication updates status`() = runBlocking {
        val team = repository.createTeam("Test Team", "leader1", isOpenForApplications = true).first().getOrNull()!!
        val app = repository.applyToTeam(team.id, "applicant1", "Please let me join").first().getOrNull()!!
        val result = repository.declineApplication(app.id).first()
        assertTrue(result.isSuccess)
    }

    // ─── Invite link tests ───

    @Test
    fun `generateInviteLink contains team id`() {
        val link = repository.generateInviteLink("team123", "Test Team")
        assertTrue(link.contains("team123"))
    }

    @Test
    fun `generateInviteLink encodes spaces`() {
        val link = repository.generateInviteLink("team123", "Test Team Name")
        assertFalse(link.contains(" "))
    }

    // ─── Open teams tests ───

    @Test
    fun `getOpenTeams filters only open teams`() = runBlocking {
        repository.createTeam("Open Team", "leader1", isOpenForApplications = true).first()
        repository.createTeam("Closed Team", "leader2", isOpenForApplications = false).first()
        val result = repository.getOpenTeams().first()
        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()?.size)
        assertEquals("Open Team", result.getOrNull()?.first()?.name)
    }
}

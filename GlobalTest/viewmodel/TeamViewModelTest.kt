package com.mlbb.scrim.viewmodel

import android.app.Application
import android.net.Uri
import com.mlbb.scrim.data.model.PlayerRole
import com.mlbb.scrim.data.model.Team
import com.mlbb.scrim.data.model.TeamInvite
import com.mlbb.scrim.data.model.TeamRating
import com.mlbb.scrim.data.repository.TeamRepositoryInterface
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class TeamViewModelTest {

    private lateinit var viewModel: TeamViewModel
    private lateinit var mockRepository: TeamRepositoryInterface
    private lateinit var mockApplication: Application
    private lateinit var testDispatcher: TestDispatcher

    @Before
    fun setup() {
        mockRepository = mockk(relaxed = true)
        mockApplication = mockk(relaxed = true)
        testDispatcher = StandardTestDispatcher()

        Dispatchers.setMain(testDispatcher)

        viewModel = TeamViewModel(
            teamRepository = mockRepository,
            application = mockApplication
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ─── Initialization Tests ───

    @Test
    fun `ViewModel initializes with empty state`() {
        // Assert
        assertTrue(viewModel.teams.value.isEmpty())
        assertEquals(null, viewModel.currentTeam.value)
        assertFalse(viewModel.isLoading.value)
        assertEquals(null, viewModel.errorMessage.value)
        assertEquals(null, viewModel.createSuccess.value)
        assertTrue(viewModel.pendingInvites.value.isEmpty())
        assertTrue(viewModel.teamInvites.value.isEmpty())
        assertTrue(viewModel.openTeams.value.isEmpty())
        assertTrue(viewModel.teamApplications.value.isEmpty())
        assertFalse(viewModel.applicationSuccess.value)
        assertTrue(viewModel.teamStats.value.isEmpty())
        assertTrue(viewModel.teamRatings.value.isEmpty())
        assertEquals(0.0, viewModel.averageRating.value)
    }

    // ─── Load Teams Tests ───

    @Test
    fun `loadTeams successfully loads teams for user`() {
        // Arrange
        val userId = "user123"
        val mockTeams = listOf(
            createMockTeam(id = "1", name = "Team1"),
            createMockTeam(id = "2", name = "Team2")
        )
        coEvery { mockRepository.getTeamsForUser(userId) } returns flow { emit(Result.success(mockTeams)) }

        // Act
        viewModel.setUserId(userId)
        advanceUntilIdle()

        // Assert
        assertEquals(mockTeams, viewModel.teams.value)
        assertFalse(viewModel.isLoading.value)
    }

    @Test
    fun `loadTeams loads all teams when userId is null`() {
        // Arrange
        val mockTeams = listOf(createMockTeam(id = "1"))
        coEvery { mockRepository.getTeams() } returns flow { emit(Result.success(mockTeams)) }

        // Act
        viewModel.loadTeams()
        advanceUntilIdle()

        // Assert
        assertEquals(mockTeams, viewModel.teams.value)
        assertFalse(viewModel.isLoading.value)
    }

    @Test
    fun `loadTeams handles error`() {
        // Arrange
        val errorMessage = "Failed to load teams"
        coEvery { mockRepository.getTeams() } returns flow { emit(Result.failure(Exception(errorMessage))) }

        // Act
        viewModel.loadTeams()
        advanceUntilIdle()

        // Assert
        assertEquals(errorMessage, viewModel.errorMessage.value)
        assertFalse(viewModel.isLoading.value)
    }

    @Test
    fun `loadTeams sets refreshing flag when isRefresh is true`() {
        // Arrange
        coEvery { mockRepository.getTeams() } returns flow { emit(Result.success(emptyList())) }

        // Act
        viewModel.loadTeams(isRefresh = true)
        advanceUntilIdle()

        // Assert
        assertFalse(viewModel.isRefreshing.value) // Should be false after completion
    }

    // ─── Set User ID Tests ───

    @Test
    fun `setUserId clears stale data when user changes`() {
        // Arrange
        val initialTeams = listOf(createMockTeam(id = "1"))
        viewModel.teams.value = initialTeams
        viewModel.currentTeam.value = createMockTeam(id = "current")

        // Act
        viewModel.setUserId("newUser123")
        advanceUntilIdle()

        // Assert
        assertTrue(viewModel.teams.value.isEmpty())
        assertEquals(null, viewModel.currentTeam.value)
    }

    @Test
    fun `setUserId does not clear data when user is same`() {
        // Arrange
        val userId = "user123"
        val initialTeams = listOf(createMockTeam(id = "1"))
        viewModel.teams.value = initialTeams
        viewModel.currentTeam.value = createMockTeam(id = "current")
        coEvery { mockRepository.getTeamsForUser(userId) } returns flow { emit(Result.success(initialTeams)) }

        // Act
        viewModel.setUserId(userId)
        advanceUntilIdle()

        // Assert - Data should remain
        assertFalse(viewModel.teams.value.isEmpty())
    }

    @Test
    fun `setUserId triggers loadTeams for new user`() {
        // Arrange
        val userId = "user123"
        val mockTeams = listOf(createMockTeam(id = "1"))
        coEvery { mockRepository.getTeamsForUser(userId) } returns flow { emit(Result.success(mockTeams)) }

        // Act
        viewModel.setUserId(userId)
        advanceUntilIdle()

        // Assert
        coVerify { mockRepository.getTeamsForUser(userId) }
    }

    // ─── Create Team Tests ───

    @Test
    fun `createTeam successfully creates team`() {
        // Arrange
        val teamName = "New Team"
        val leaderId = "leader123"
        val mockTeam = createMockTeam(id = "new-id", name = teamName)
        coEvery { mockRepository.createTeam(teamName, leaderId, isOpenForApplications = any()) } returns flow { emit(Result.success(mockTeam)) }
        coEvery { mockRepository.getTeamsForUser(leaderId) } returns flow { emit(Result.success(listOf(mockTeam))) }

        // Act
        viewModel.createTeam(teamName, leaderId)
        advanceUntilIdle()

        // Assert
        assertEquals(mockTeam, viewModel.currentTeam.value)
        assertEquals(mockTeam, viewModel.createSuccess.value)
    }

    @Test
    fun `createTeam handles error`() {
        // Arrange
        val errorMessage = "Failed to create team"
        coEvery { mockRepository.createTeam(any(), any(), isOpenForApplications = any()) } returns flow { emit(Result.failure(Exception(errorMessage))) }

        // Act
        viewModel.createTeam("Test Team", "leader123")
        advanceUntilIdle()

        // Assert
        assertEquals(errorMessage, viewModel.errorMessage.value)
        assertEquals(null, viewModel.createSuccess.value)
    }

    @Test
    fun `createTeam rejects oversized logo`() {
        // Arrange
        val largeLogoUri = mockk<Uri>()
        val mockTeam = createMockTeam(id = "new-id")
        coEvery { mockRepository.createTeam(any(), any(), isOpenForApplications = any()) } returns flow { emit(Result.success(mockTeam)) }
        coEvery { mockRepository.getTeamsForUser(any()) } returns flow { emit(Result.success(listOf(mockTeam))) }
        
        // Mock content resolver to return large byte array (>3MB)
        val largeByteArray = ByteArray(4 * 1024 * 1024) // 4MB
        mockkStatic(android.net.Uri::class)
        every { mockApplication.contentResolver.openInputStream(largeLogoUri) } returns mockk {
            every { readBytes() } returns largeByteArray
        }

        // Act
        viewModel.createTeam("Test Team", "leader123", logoUri = largeLogoUri)
        advanceUntilIdle()

        // Assert
        assertEquals("Logo is too large. Max size is 3MB.", viewModel.errorMessage.value)
    }

    // ─── Load Team Tests ───

    @Test
    fun `loadTeam successfully loads team by ID`() {
        // Arrange
        val teamId = "team123"
        val mockTeam = createMockTeam(id = teamId)
        coEvery { mockRepository.getTeam(teamId) } returns flow { emit(Result.success(mockTeam)) }

        // Act
        viewModel.loadTeam(teamId)
        advanceUntilIdle()

        // Assert
        assertEquals(mockTeam, viewModel.currentTeam.value)
        assertFalse(viewModel.isLoading.value)
    }

    @Test
    fun `loadTeam handles error`() {
        // Arrange
        val errorMessage = "Team not found"
        coEvery { mockRepository.getTeam(any()) } returns flow { emit(Result.failure(Exception(errorMessage))) }

        // Act
        viewModel.loadTeam("team123")
        advanceUntilIdle()

        // Assert
        assertEquals(errorMessage, viewModel.errorMessage.value)
        assertEquals(null, viewModel.currentTeam.value)
    }

    // ─── Add Player Tests ───

    @Test
    fun `addPlayer successfully adds player to team`() {
        // Arrange
        val teamId = "team123"
        val updatedTeam = createMockTeam(id = teamId)
        coEvery { mockRepository.addPlayer(teamId, "PlayerName", "player@email.com") } returns flow { emit(Result.success(updatedTeam)) }
        coEvery { mockRepository.getTeamsForUser(any()) } returns flow { emit(Result.success(listOf(updatedTeam))) }

        // Act
        viewModel.addPlayer(teamId, "PlayerName", "player@email.com")
        advanceUntilIdle()

        // Assert
        assertEquals(updatedTeam, viewModel.currentTeam.value)
        coVerify { mockRepository.addPlayer(teamId, "PlayerName", "player@email.com") }
    }

    @Test
    fun `addPlayer handles error`() {
        // Arrange
        val errorMessage = "Failed to add player"
        coEvery { mockRepository.addPlayer(any(), any(), any()) } returns flow { emit(Result.failure(Exception(errorMessage))) }

        // Act
        viewModel.addPlayer("team123", "PlayerName", "player@email.com")
        advanceUntilIdle()

        // Assert
        assertEquals(errorMessage, viewModel.errorMessage.value)
    }

    // ─── Remove Player Tests ───

    @Test
    fun `removePlayer successfully removes player from team`() {
        // Arrange
        val teamId = "team123"
        val playerId = "player123"
        val updatedTeam = createMockTeam(id = teamId)
        coEvery { mockRepository.removePlayer(teamId, playerId) } returns flow { emit(Result.success(updatedTeam)) }
        coEvery { mockRepository.getTeamsForUser(any()) } returns flow { emit(Result.success(listOf(updatedTeam))) }

        // Act
        viewModel.removePlayer(teamId, playerId)
        advanceUntilIdle()

        // Assert
        assertEquals(updatedTeam, viewModel.currentTeam.value)
        coVerify { mockRepository.removePlayer(teamId, playerId) }
    }

    @Test
    fun `removePlayer handles error`() {
        // Arrange
        val errorMessage = "Failed to remove player"
        coEvery { mockRepository.removePlayer(any(), any()) } returns flow { emit(Result.failure(Exception(errorMessage))) }

        // Act
        viewModel.removePlayer("team123", "player123")
        advanceUntilIdle()

        // Assert
        assertEquals(errorMessage, viewModel.errorMessage.value)
    }

    // ─── Update Player Role Tests ───

    @Test
    fun `updatePlayerRole successfully updates player role`() {
        // Arrange
        val teamId = "team123"
        val playerId = "player123"
        val newRole = PlayerRole.CARRY
        val updatedTeam = createMockTeam(id = teamId)
        coEvery { mockRepository.updatePlayerRole(teamId, playerId, newRole) } returns flow { emit(Result.success(updatedTeam)) }
        coEvery { mockRepository.getTeamsForUser(any()) } returns flow { emit(Result.success(listOf(updatedTeam))) }

        // Act
        viewModel.updatePlayerRole(teamId, playerId, newRole)
        advanceUntilIdle()

        // Assert
        assertEquals(updatedTeam, viewModel.currentTeam.value)
        coVerify { mockRepository.updatePlayerRole(teamId, playerId, newRole) }
    }

    @Test
    fun `updatePlayerRole handles error`() {
        // Arrange
        val errorMessage = "Failed to update role"
        coEvery { mockRepository.updatePlayerRole(any(), any(), any()) } returns flow { emit(Result.failure(Exception(errorMessage))) }

        // Act
        viewModel.updatePlayerRole("team123", "player123", PlayerRole.CARRY)
        advanceUntilIdle()

        // Assert
        assertEquals(errorMessage, viewModel.errorMessage.value)
    }

    // ─── Leave Team Tests ───

    @Test
    fun `leaveTeam successfully removes player from team`() {
        // Arrange
        val teamId = "team123"
        val playerId = "player123"
        val updatedTeam = createMockTeam(id = teamId)
        coEvery { mockRepository.removePlayer(teamId, playerId) } returns flow { emit(Result.success(updatedTeam)) }
        coEvery { mockRepository.getTeamsForUser(any()) } returns flow { emit(Result.success(listOf(updatedTeam))) }

        // Act
        viewModel.leaveTeam(teamId, playerId)
        advanceUntilIdle()

        // Assert
        assertEquals(updatedTeam, viewModel.currentTeam.value)
        coVerify { mockRepository.removePlayer(teamId, playerId) }
    }

    // ─── Delete Team Tests ───

    @Test
    fun `deleteTeam successfully deletes team`() {
        // Arrange
        val teamId = "team123"
        viewModel.currentTeam.value = createMockTeam(id = teamId)
        coEvery { mockRepository.deleteTeam(teamId) } returns flow { emit(Result.success(Unit)) }
        coEvery { mockRepository.getTeamsForUser(any()) } returns flow { emit(Result.success(emptyList())) }

        // Act
        viewModel.deleteTeam(teamId)
        advanceUntilIdle()

        // Assert
        assertEquals(null, viewModel.currentTeam.value)
        coVerify { mockRepository.deleteTeam(teamId) }
    }

    @Test
    fun `deleteTeam handles error`() {
        // Arrange
        val errorMessage = "Failed to delete team"
        coEvery { mockRepository.deleteTeam(any()) } returns flow { emit(Result.failure(Exception(errorMessage))) }

        // Act
        viewModel.deleteTeam("team123")
        advanceUntilIdle()

        // Assert
        assertEquals(errorMessage, viewModel.errorMessage.value)
    }

    // ─── Invite Tests ───

    @Test
    fun `sendInvite successfully sends team invite`() {
        // Arrange
        val teamId = "team123"
        coEvery { mockRepository.sendInvite(any(), any(), any(), any(), any(), any()) } returns flow { emit(Result.success(Unit)) }
        coEvery { mockRepository.getInvitesForTeam(teamId) } returns flow { emit(Result.success(emptyList())) }

        // Act
        viewModel.sendInvite(teamId, "TeamName", "inviterId", "Inviter", "invitedId", "Invited")
        advanceUntilIdle()

        // Assert
        coVerify { mockRepository.sendInvite(teamId, "TeamName", "inviterId", "Inviter", "invitedId", "Invited") }
    }

    @Test
    fun `acceptInvite successfully accepts team invite`() {
        // Arrange
        val inviteId = "invite123"
        val team = createMockTeam(id = "team123")
        coEvery { mockRepository.acceptInvite(inviteId) } returns flow { emit(Result.success(team)) }
        coEvery { mockRepository.getTeamsForUser(any()) } returns flow { emit(Result.success(listOf(team))) }
        coEvery { mockRepository.getInvitesForPlayer(any()) } returns flow { emit(Result.success(emptyList())) }

        // Act
        viewModel.acceptInvite(inviteId)
        advanceUntilIdle()

        // Assert
        assertEquals(team, viewModel.currentTeam.value)
        coVerify { mockRepository.acceptInvite(inviteId) }
    }

    @Test
    fun `declineInvite successfully declines team invite`() {
        // Arrange
        val inviteId = "invite123"
        val existingInvite = createMockTeamInvite(id = inviteId)
        viewModel.pendingInvites.value = listOf(existingInvite)
        coEvery { mockRepository.declineInvite(inviteId) } returns flow { emit(Result.success(Unit)) }

        // Act
        viewModel.declineInvite(inviteId)
        advanceUntilIdle()

        // Assert
        assertTrue(viewModel.pendingInvites.value.isEmpty())
        coVerify { mockRepository.declineInvite(inviteId) }
    }

    @Test
    fun `loadPendingInvites successfully loads invites for player`() {
        // Arrange
        val userId = "user123"
        val mockInvites = listOf(createMockTeamInvite(id = "1"))
        coEvery { mockRepository.getInvitesForPlayer(userId) } returns flow { emit(Result.success(mockInvites)) }

        // Act
        viewModel.loadPendingInvites(userId)
        advanceUntilIdle()

        // Assert
        assertEquals(mockInvites, viewModel.pendingInvites.value)
    }

    @Test
    fun `loadTeamInvites successfully loads invites for team`() {
        // Arrange
        val teamId = "team123"
        val mockInvites = listOf(createMockTeamInvite(id = "1"))
        coEvery { mockRepository.getInvitesForTeam(teamId) } returns flow { emit(Result.success(mockInvites)) }

        // Act
        viewModel.loadTeamInvites(teamId)
        advanceUntilIdle()

        // Assert
        assertEquals(mockInvites, viewModel.teamInvites.value)
    }

    // ─── Application Tests ───

    @Test
    fun `loadOpenTeams successfully loads open teams`() {
        // Arrange
        val mockTeams = listOf(createMockTeam(id = "1"))
        coEvery { mockRepository.getOpenTeams() } returns flow { emit(Result.success(mockTeams)) }

        // Act
        viewModel.loadOpenTeams()
        advanceUntilIdle()

        // Assert
        assertEquals(mockTeams, viewModel.openTeams.value)
        assertFalse(viewModel.isLoading.value)
    }

    @Test
    fun `applyToTeam successfully applies to team`() {
        // Arrange
        val teamId = "team123"
        val userId = "user123"
        viewModel.setUserId(userId)
        coEvery { mockRepository.applyToTeam(teamId, userId, any()) } returns flow { emit(Result.success(Unit)) }

        // Act
        viewModel.applyToTeam(teamId, "I want to join")
        advanceUntilIdle()

        // Assert
        assertTrue(viewModel.applicationSuccess.value)
        coVerify { mockRepository.applyToTeam(teamId, userId, any()) }
    }

    @Test
    fun `applyToTeam does nothing when userId is null`() {
        // Arrange
        viewModel.setUserId(null)
        coEvery { mockRepository.applyToTeam(any(), any(), any()) } returns flow { emit(Result.success(Unit)) }

        // Act
        viewModel.applyToTeam("team123", "I want to join")
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 0) { mockRepository.applyToTeam(any(), any(), any()) }
    }

    @Test
    fun `loadTeamApplications successfully loads team applications`() {
        // Arrange
        val teamId = "team123"
        val mockApps = listOf(com.mlbb.scrim.data.model.TeamApplication(id = "1"))
        coEvery { mockRepository.getTeamApplications(teamId) } returns flow { emit(Result.success(mockApps)) }

        // Act
        viewModel.loadTeamApplications(teamId)
        advanceUntilIdle()

        // Assert
        assertEquals(mockApps, viewModel.teamApplications.value)
    }

    @Test
    fun `acceptApplication successfully accepts team application`() {
        // Arrange
        val applicationId = "app123"
        val team = createMockTeam(id = "team123")
        coEvery { mockRepository.acceptApplication(applicationId) } returns flow { emit(Result.success(team)) }
        coEvery { mockRepository.getTeamApplications(team.id) } returns flow { emit(Result.success(emptyList())) }

        // Act
        viewModel.acceptApplication(applicationId)
        advanceUntilIdle()

        // Assert
        assertEquals(team, viewModel.currentTeam.value)
        coVerify { mockRepository.acceptApplication(applicationId) }
    }

    @Test
    fun `declineApplication successfully declines team application`() {
        // Arrange
        val applicationId = "app123"
        val teamId = "team123"
        coEvery { mockRepository.declineApplication(applicationId) } returns flow { emit(Result.success(Unit)) }
        coEvery { mockRepository.getTeamApplications(teamId) } returns flow { emit(Result.success(emptyList())) }

        // Act
        viewModel.declineApplication(applicationId, teamId)
        advanceUntilIdle()

        // Assert
        coVerify { mockRepository.declineApplication(applicationId) }
    }

    // ─── Stats & Ratings Tests ───

    @Test
    fun `loadTeamStats successfully loads team stats`() {
        // Arrange
        val teamId = "team123"
        val mockStats = mapOf("wins" to 10, "losses" to 5)
        coEvery { mockRepository.getTeamStats(teamId) } returns flow { emit(Result.success(mockStats)) }

        // Act
        viewModel.loadTeamStats(teamId)
        advanceUntilIdle()

        // Assert
        assertEquals(mockStats, viewModel.teamStats.value)
    }

    @Test
    fun `loadTeamRatings successfully loads team ratings`() {
        // Arrange
        val teamId = "team123"
        val mockRatings = listOf(TeamRating(id = "1", teamId = teamId, rating = 5))
        coEvery { mockRepository.getTeamRatings(teamId) } returns flow { emit(Result.success(mockRatings)) }

        // Act
        viewModel.loadTeamRatings(teamId)
        advanceUntilIdle()

        // Assert
        assertEquals(mockRatings, viewModel.teamRatings.value)
    }

    @Test
    fun `submitTeamRating successfully submits rating`() {
        // Arrange
        val teamId = "team123"
        val raterTeamId = "rater123"
        val raterUserId = "user123"
        coEvery { mockRepository.submitTeamRating(teamId, raterTeamId, raterUserId, 5, "Good team") } returns flow { emit(Result.success(Unit)) }
        coEvery { mockRepository.getTeamRatings(teamId) } returns flow { emit(Result.success(emptyList())) }

        // Act
        viewModel.submitTeamRating(teamId, raterTeamId, raterUserId, 5, "Good team")
        advanceUntilIdle()

        // Assert
        coVerify { mockRepository.submitTeamRating(teamId, raterTeamId, raterUserId, 5, "Good team") }
    }

    // ─── Clear State Tests ───

    @Test
    fun `clearApplicationSuccess clears application success flag`() {
        // Arrange
        viewModel._applicationSuccess.value = true

        // Act
        viewModel.clearApplicationSuccess()

        // Assert
        assertFalse(viewModel.applicationSuccess.value)
    }

    @Test
    fun `clearErrorMessage clears error message`() {
        // Arrange
        viewModel._errorMessage.value = "Test error"

        // Act
        viewModel.clearErrorMessage()

        // Assert
        assertEquals(null, viewModel.errorMessage.value)
    }

    @Test
    fun `clearRefreshing clears refreshing flag`() {
        // Arrange
        viewModel._isRefreshing.value = true

        // Act
        viewModel.clearRefreshing()

        // Assert
        assertFalse(viewModel.isRefreshing.value)
    }

    @Test
    fun `clearCreateSuccess clears create success`() {
        // Arrange
        viewModel._createSuccess.value = createMockTeam(id = "1")

        // Act
        viewModel.clearCreateSuccess()

        // Assert
        assertEquals(null, viewModel.createSuccess.value)
    }

    @Test
    fun `clearCurrentTeam clears current team`() {
        // Arrange
        viewModel._currentTeam.value = createMockTeam(id = "1")

        // Act
        viewModel.clearCurrentTeam()

        // Assert
        assertEquals(null, viewModel.currentTeam.value)
    }

    @Test
    fun `clearTeamStats clears team stats and ratings`() {
        // Arrange
        viewModel._teamStats.value = mapOf("wins" to 10)
        viewModel._teamRatings.value = listOf(TeamRating(id = "1", teamId = "1", rating = 5))
        viewModel._averageRating.value = 4.5

        // Act
        viewModel.clearTeamStats()

        // Assert
        assertTrue(viewModel.teamStats.value.isEmpty())
        assertTrue(viewModel.teamRatings.value.isEmpty())
        assertEquals(0.0, viewModel.averageRating.value)
    }

    // ─── Realtime Subscription Tests ───

    @Test
    fun `subscribeToTeamUpdates subscribes to team realtime updates`() {
        // Arrange
        val teamId = "team123"
        val updatedTeam = createMockTeam(id = teamId, name = "Updated Team")
        coEvery { mockRepository.subscribeToTeam(teamId) } returns flow { emit(updatedTeam) }

        // Act
        viewModel.subscribeToTeamUpdates(teamId)
        advanceUntilIdle()

        // Assert
        assertEquals(updatedTeam, viewModel.currentTeam.value)
    }

    @Test
    fun `subscribeToTeamInvites subscribes to invite realtime updates`() {
        // Arrange
        val userId = "user123"
        val newInvite = createMockTeamInvite(id = "new-invite")
        coEvery { mockRepository.subscribeToTeamInvites(userId) } returns flow { emit(newInvite) }

        // Act
        viewModel.subscribeToTeamInvites(userId)
        advanceUntilIdle()

        // Assert
        // Verify subscription was called (actual behavior depends on implementation)
        coVerify { mockRepository.subscribeToTeamInvites(userId) }
    }

    // ─── Helper Functions ───

    private fun createMockTeam(
        id: String = "test-id",
        name: String = "Test Team"
    ): Team {
        return Team(
            id = id,
            name = name,
            leaderId = "leader123",
            leaderName = "Leader",
            players = emptyList(),
            region = com.mlbb.scrim.data.model.Region.NORTH_AMERICA,
            skillLevel = com.mlbb.scrim.data.model.SkillLevel.DIAMOND,
            isOpenForApplications = false,
            createdAt = System.currentTimeMillis()
        )
    }

    private fun createMockTeamInvite(
        id: String = "invite-id"
    ): TeamInvite {
        return TeamInvite(
            id = id,
            teamId = "team123",
            teamName = "Test Team",
            invitedBy = "inviter123",
            invitedByName = "Inviter",
            invitedUserId = "invited123",
            invitedUserName = "Invited",
            status = com.mlbb.scrim.data.model.InviteStatus.PENDING,
            createdAt = System.currentTimeMillis()
        )
    }
}

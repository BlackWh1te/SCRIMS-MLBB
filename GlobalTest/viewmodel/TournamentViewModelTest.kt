package com.mlbb.scrim.viewmodel

import com.mlbb.scrim.data.model.*
import com.mlbb.scrim.data.repository.TournamentRepositoryInterface
import com.mlbb.scrim.data.service.SupabaseSession
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
class TournamentViewModelTest {

    private lateinit var viewModel: TournamentViewModel
    private lateinit var mockRepository: TournamentRepositoryInterface
    private lateinit var testDispatcher: TestDispatcher

    @Before
    fun setup() {
        mockRepository = mockk(relaxed = true)
        testDispatcher = StandardTestDispatcher()

        Dispatchers.setMain(testDispatcher)

        // Mock SupabaseSession
        mockkObject(SupabaseSession)
        every { SupabaseSession.getUserIdOrNull() } returns "user123"

        viewModel = TournamentViewModel(mockRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    // ─── Initialization Tests ───

    @Test
    fun `ViewModel initializes with empty state`() {
        // Assert
        assertTrue(viewModel.tournaments.value.isEmpty())
        assertTrue(viewModel.hostedTournaments.value.isEmpty())
        assertEquals(null, viewModel.selectedTournament.value)
        assertTrue(viewModel.requirements.value.isEmpty())
        assertTrue(viewModel.tournamentTeams.value.isEmpty())
        assertTrue(viewModel.matches.value.isEmpty())
        assertTrue(viewModel.myApplications.value.isEmpty())
        assertEquals(null, viewModel.myHostRequest.value)
        assertTrue(viewModel.matchRoster.value.isEmpty())
        assertEquals(null, viewModel.roomSecret.value)
        assertFalse(viewModel.isLoading.value)
        assertFalse(viewModel.isRefreshing.value)
        assertEquals(null, viewModel.error.value)
        assertEquals(null, viewModel.applyResult.value)
        assertEquals(null, viewModel.hostRequestResult.value)
        assertEquals(null, viewModel.createResult.value)
        assertEquals(null, viewModel.updateResult.value)
        assertEquals(null, viewModel.statusFilter.value)
        assertEquals(null, viewModel.regionFilter.value)
        assertEquals(null, viewModel.skillLevelFilter.value)
        assertTrue(viewModel.myTeamIds.value.isEmpty())
    }

    // ─── Load Tournaments Tests ───

    @Test
    fun `loadTournaments successfully loads tournaments`() {
        // Arrange
        val mockTournaments = listOf(
            createMockTournament(id = "1", name = "Tournament1"),
            createMockTournament(id = "2", name = "Tournament2")
        )
        coEvery { mockRepository.getTournaments(any(), any(), any()) } returns Result.success(mockTournaments)

        // Act
        viewModel.loadTournaments()
        advanceUntilIdle()

        // Assert
        assertEquals(mockTournaments, viewModel.tournaments.value)
        assertFalse(viewModel.isLoading.value)
    }

    @Test
    fun `loadTournaments filters hosted tournaments for current user`() {
        // Arrange
        val mockTournaments = listOf(
            createMockTournament(id = "1", name = "Tournament1", hostUserId = "user123"),
            createMockTournament(id = "2", name = "Tournament2", hostUserId = "other456")
        )
        coEvery { mockRepository.getTournaments(any(), any(), any()) } returns Result.success(mockTournaments)

        // Act
        viewModel.loadTournaments()
        advanceUntilIdle()

        // Assert
        assertEquals(1, viewModel.hostedTournaments.value.size)
        assertEquals("Tournament1", viewModel.hostedTournaments.value.first().name)
    }

    @Test
    fun `loadTournaments handles error`() {
        // Arrange
        val errorMessage = "Failed to load tournaments"
        coEvery { mockRepository.getTournaments(any(), any(), any()) } returns Result.failure(Exception(errorMessage))

        // Act
        viewModel.loadTournaments()
        advanceUntilIdle()

        // Assert
        assertEquals(errorMessage, viewModel.error.value)
        assertFalse(viewModel.isLoading.value)
    }

    @Test
    fun `loadTournaments sets refreshing flag when isRefresh is true`() {
        // Arrange
        coEvery { mockRepository.getTournaments(any(), any(), any()) } returns Result.success(emptyList())

        // Act
        viewModel.loadTournaments(isRefresh = true)
        advanceUntilIdle()

        // Assert
        assertFalse(viewModel.isRefreshing.value) // Should be false after completion
    }

    // ─── Filter Tests ───

    @Test
    fun `setStatusFilter sets filter and reloads tournaments`() {
        // Arrange
        coEvery { mockRepository.getTournaments(any(), any(), any()) } returns Result.success(emptyList())

        // Act
        viewModel.setStatusFilter("active")
        advanceUntilIdle()

        // Assert
        assertEquals("active", viewModel.statusFilter.value)
        coVerify { mockRepository.getTournaments("active", null, null) }
    }

    @Test
    fun `setRegionFilter sets filter and reloads tournaments`() {
        // Arrange
        coEvery { mockRepository.getTournaments(any(), any(), any()) } returns Result.success(emptyList())

        // Act
        viewModel.setRegionFilter("NA")
        advanceUntilIdle()

        // Assert
        assertEquals("NA", viewModel.regionFilter.value)
        coVerify { mockRepository.getTournaments(null, "NA", null) }
    }

    @Test
    fun `setSkillLevelFilter sets filter and reloads tournaments`() {
        // Arrange
        coEvery { mockRepository.getTournaments(any(), any(), any()) } returns Result.success(emptyList())

        // Act
        viewModel.setSkillLevelFilter("Diamond")
        advanceUntilIdle()

        // Assert
        assertEquals("Diamond", viewModel.skillLevelFilter.value)
        coVerify { mockRepository.getTournaments(null, null, "Diamond") }
    }

    // ─── Load Tournament by ID Tests ───

    @Test
    fun `loadTournamentById successfully loads tournament and related data`() {
        // Arrange
        val tournamentId = "tournament123"
        val mockTournament = createMockTournament(id = tournamentId)
        val mockRequirements = listOf(createMockTournamentRequirement())
        val mockTeams = listOf(createMockTournamentTeam())
        val mockMatches = listOf(createMockTournamentSwissMatch())

        coEvery { mockRepository.getTournamentById(tournamentId) } returns Result.success(mockTournament)
        coEvery { mockRepository.getTournamentRequirements(tournamentId) } returns Result.success(mockRequirements)
        coEvery { mockRepository.getTournamentTeams(tournamentId) } returns Result.success(mockTeams)
        coEvery { mockRepository.getTournamentMatches(tournamentId) } returns Result.success(mockMatches)
        coEvery { mockRepository.getMyApplications(any()) } returns Result.success(emptyList())

        // Act
        viewModel.loadTournamentById(tournamentId)
        advanceUntilIdle()

        // Assert
        assertEquals(mockTournament, viewModel.selectedTournament.value)
        assertEquals(mockRequirements, viewModel.requirements.value)
        assertEquals(mockTeams, viewModel.tournamentTeams.value)
        assertEquals(mockMatches, viewModel.matches.value)
        assertFalse(viewModel.isLoading.value)
    }

    @Test
    fun `loadTournamentById handles error`() {
        // Arrange
        val errorMessage = "Tournament not found"
        coEvery { mockRepository.getTournamentById(any()) } returns Result.failure(Exception(errorMessage))

        // Act
        viewModel.loadTournamentById("tournament123")
        advanceUntilIdle()

        // Assert
        assertEquals(errorMessage, viewModel.error.value)
        assertEquals(null, viewModel.selectedTournament.value)
    }

    // ─── My Team IDs Tests ───

    @Test
    fun `setMyTeamIds updates team IDs and marks matches`() {
        // Arrange
        val teamIds = listOf("team1", "team2")
        val mockMatches = listOf(
            createMockTournamentSwissMatch(teamAId = "team1", teamBId = "team3"),
            createMockTournamentSwissMatch(teamAId = "team4", teamBId = "team5")
        )
        viewModel.matches.value = mockMatches

        // Act
        viewModel.setMyTeamIds(teamIds)

        // Assert
        assertEquals(teamIds, viewModel.myTeamIds.value)
        assertTrue(viewModel.matches.value[0].isMyMatch)
        assertFalse(viewModel.matches.value[1].isMyMatch)
    }

    // ─── Applications Tests ───

    @Test
    fun `loadMyApplications successfully loads applications`() {
        // Arrange
        val mockApplications = listOf(createMockTournamentApplication())
        coEvery { mockRepository.getMyApplications("user123") } returns Result.success(mockApplications)

        // Act
        viewModel.loadMyApplications()
        advanceUntilIdle()

        // Assert
        assertEquals(mockApplications, viewModel.myApplications.value)
        assertFalse(viewModel.isLoading.value)
    }

    @Test
    fun `loadMyApplications does nothing when user is not logged in`() {
        // Arrange
        every { SupabaseSession.getUserIdOrNull() } returns null

        // Act
        viewModel.loadMyApplications()
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 0) { mockRepository.getMyApplications(any()) }
    }

    @Test
    fun `applyForTournament successfully submits application`() {
        // Arrange
        val tournamentId = "tournament123"
        val teamId = "team123"
        val resultData = mapOf("applicationId" to "app123")
        coEvery { mockRepository.applyForTournament(tournamentId, teamId) } returns Result.success(resultData)
        coEvery { mockRepository.getMyApplications(any()) } returns Result.success(emptyList())

        // Act
        viewModel.applyForTournament(tournamentId, teamId)
        advanceUntilIdle()

        // Assert
        assertEquals(Result.success(resultData), viewModel.applyResult.value)
        coVerify { mockRepository.applyForTournament(tournamentId, teamId) }
    }

    @Test
    fun `applyForTournament handles error`() {
        // Arrange
        val errorMessage = "Failed to apply"
        coEvery { mockRepository.applyForTournament(any(), any()) } returns Result.failure(Exception(errorMessage))

        // Act
        viewModel.applyForTournament("tournament123", "team123")
        advanceUntilIdle()

        // Assert
        assertEquals(errorMessage, viewModel.error.value)
        assertTrue(viewModel.applyResult.value?.isFailure == true)
    }

    // ─── Host Request Tests ───

    @Test
    fun `loadMyHostRequest successfully loads host request`() {
        // Arrange
        val mockRequest = createMockTournamentHostRequest()
        coEvery { mockRepository.getMyHostRequest("user123") } returns Result.success(mockRequest)

        // Act
        viewModel.loadMyHostRequest()
        advanceUntilIdle()

        // Assert
        assertEquals(mockRequest, viewModel.myHostRequest.value)
    }

    @Test
    fun `submitHostRequest successfully submits request`() {
        // Arrange
        val mockRequest = createMockTournamentHostRequest()
        coEvery { mockRepository.submitHostRequest(any(), any(), any(), any()) } returns Result.success(mockRequest)

        // Act
        viewModel.submitHostRequest(
            motivation = "I want to host",
            experience = "5 years",
            telegramChannel = "@host",
            socialLinks = listOf("https://twitter.com/host")
        )
        advanceUntilIdle()

        // Assert
        assertEquals(mockRequest, viewModel.myHostRequest.value)
        assertEquals(Result.success(mockRequest), viewModel.hostRequestResult.value)
    }

    @Test
    fun `submitHostRequest handles error`() {
        // Arrange
        val errorMessage = "Failed to submit request"
        coEvery { mockRepository.submitHostRequest(any(), any(), any(), any()) } returns Result.failure(Exception(errorMessage))

        // Act
        viewModel.submitHostRequest("motivation", null, null, emptyList())
        advanceUntilIdle()

        // Assert
        assertEquals(errorMessage, viewModel.error.value)
        assertTrue(viewModel.hostRequestResult.value?.isFailure == true)
    }

    // ─── Create Tournament Tests ───

    @Test
    fun `createTournament successfully creates tournament`() {
        // Arrange
        val mockTournament = createMockTournament(id = "new-tournament")
        coEvery { mockRepository.createTournament(any()) } returns Result.success(mockTournament)
        coEvery { mockRepository.getTournaments(any(), any(), any()) } returns Result.success(listOf(mockTournament))

        // Act
        viewModel.createTournament(mockTournament)
        advanceUntilIdle()

        // Assert
        assertEquals(mockTournament, viewModel.selectedTournament.value)
        assertEquals(Result.success(mockTournament), viewModel.createResult.value)
    }

    @Test
    fun `createTournament handles error`() {
        // Arrange
        val errorMessage = "Failed to create tournament"
        coEvery { mockRepository.createTournament(any()) } returns Result.failure(Exception(errorMessage))

        // Act
        viewModel.createTournament(createMockTournament())
        advanceUntilIdle()

        // Assert
        assertEquals(errorMessage, viewModel.error.value)
        assertTrue(viewModel.createResult.value?.isFailure == true)
    }

    // ─── Update Tournament Tests ───

    @Test
    fun `updateTournament successfully updates tournament`() {
        // Arrange
        val tournamentId = "tournament123"
        val updates = mapOf("name" to "Updated Name")
        val updatedTournament = createMockTournament(id = tournamentId, name = "Updated Name")
        coEvery { mockRepository.updateTournament(tournamentId, updates) } returns Result.success(updatedTournament)
        coEvery { mockRepository.getTournaments(any(), any(), any()) } returns Result.success(listOf(updatedTournament))

        // Act
        viewModel.updateTournament(tournamentId, updates)
        advanceUntilIdle()

        // Assert
        assertEquals(updatedTournament, viewModel.selectedTournament.value)
        assertEquals(Result.success(updatedTournament), viewModel.updateResult.value)
    }

    @Test
    fun `updateTournament handles error`() {
        // Arrange
        val errorMessage = "Failed to update tournament"
        coEvery { mockRepository.updateTournament(any(), any()) } returns Result.failure(Exception(errorMessage))

        // Act
        viewModel.updateTournament("tournament123", emptyMap())
        advanceUntilIdle()

        // Assert
        assertEquals(errorMessage, viewModel.error.value)
        assertTrue(viewModel.updateResult.value?.isFailure == true)
    }

    // ─── Tournament Matches Tests ───

    @Test
    fun `loadTournamentMatches successfully loads matches and marks my matches`() {
        // Arrange
        val tournamentId = "tournament123"
        val teamIds = listOf("team1", "team2")
        viewModel.myTeamIds.value = teamIds
        val mockMatches = listOf(
            createMockTournamentSwissMatch(teamAId = "team1", teamBId = "team3"),
            createMockTournamentSwissMatch(teamAId = "team4", teamBId = "team5")
        )
        coEvery { mockRepository.getTournamentMatches(tournamentId) } returns Result.success(mockMatches)

        // Act
        viewModel.loadTournamentMatches(tournamentId)
        advanceUntilIdle()

        // Assert
        assertEquals(mockMatches.size, viewModel.matches.value.size)
        assertTrue(viewModel.matches.value[0].isMyMatch)
        assertFalse(viewModel.matches.value[1].isMyMatch)
    }

    // ─── Match Roster Tests ───

    @Test
    fun `loadMatchRoster successfully loads roster`() {
        // Arrange
        val matchId = "match123"
        val teamId = "team123"
        val mockRoster = listOf(createMockTournamentMatchRoster())
        coEvery { mockRepository.getMatchRoster(matchId, teamId, 1) } returns Result.success(mockRoster)

        // Act
        viewModel.loadMatchRoster(matchId, teamId)
        advanceUntilIdle()

        // Assert
        assertEquals(mockRoster, viewModel.matchRoster.value)
    }

    @Test
    fun `setMatchRoster successfully sets roster`() {
        // Arrange
        val matchId = "match123"
        val teamId = "team123"
        val playerIds = listOf("player1", "player2")
        coEvery { mockRepository.setMatchRoster(matchId, teamId, 1, playerIds) } returns Result.success(Unit)

        // Act
        viewModel.setMatchRoster(matchId, teamId, 1, playerIds)
        advanceUntilIdle()

        // Assert
        coVerify { mockRepository.setMatchRoster(matchId, teamId, 1, playerIds) }
    }

    @Test
    fun `setMatchRoster handles error`() {
        // Arrange
        val errorMessage = "Failed to set roster"
        coEvery { mockRepository.setMatchRoster(any(), any(), any(), any()) } returns Result.failure(Exception(errorMessage))

        // Act
        viewModel.setMatchRoster("match123", "team123", 1, emptyList())
        advanceUntilIdle()

        // Assert
        assertEquals(errorMessage, viewModel.error.value)
    }

    // ─── Room Secret Tests ───

    @Test
    fun `loadRoomSecret successfully loads room secret`() {
        // Arrange
        val matchId = "match123"
        val mockSecret = createMockTournamentMatchRoomSecret()
        coEvery { mockRepository.getMatchRoomSecret(matchId) } returns Result.success(mockSecret)

        // Act
        viewModel.loadRoomSecret(matchId)
        advanceUntilIdle()

        // Assert
        assertEquals(mockSecret, viewModel.roomSecret.value)
    }

    // ─── Swiss Pairing Tests ───

    @Test
    fun `generateSwissPairings successfully generates pairings`() {
        // Arrange
        val tournamentId = "tournament123"
        val mockTournament = createMockTournament(id = tournamentId)
        val mockMatches = listOf(createMockTournamentSwissMatch())
        coEvery { mockRepository.generateSwissPairings(tournamentId) } returns Result.success(Unit)
        coEvery { mockRepository.getTournamentById(tournamentId) } returns Result.success(mockTournament)
        coEvery { mockRepository.getTournamentRequirements(tournamentId) } returns Result.success(emptyList())
        coEvery { mockRepository.getTournamentTeams(tournamentId) } returns Result.success(emptyList())
        coEvery { mockRepository.getTournamentMatches(tournamentId) } returns Result.success(mockMatches)
        coEvery { mockRepository.getMyApplications(any()) } returns Result.success(emptyList())

        // Act
        viewModel.generateSwissPairings(tournamentId)
        advanceUntilIdle()

        // Assert
        coVerify { mockRepository.generateSwissPairings(tournamentId) }
    }

    // ─── Match Result Tests ───

    @Test
    fun `submitMatchResult successfully submits result`() {
        // Arrange
        val matchId = "match123"
        val winnerTeamId = "team1"
        val mockTournament = createMockTournament(id = "tournament123")
        coEvery { mockRepository.submitMatchResult(matchId, winnerTeamId, false) } returns Result.success(Unit)
        coEvery { mockRepository.getTournamentById(any()) } returns Result.success(mockTournament)
        coEvery { mockRepository.getTournamentRequirements(any()) } returns Result.success(emptyList())
        coEvery { mockRepository.getTournamentTeams(any()) } returns Result.success(emptyList())
        coEvery { mockRepository.getTournamentMatches(any()) } returns Result.success(emptyList())
        coEvery { mockRepository.getMyApplications(any()) } returns Result.success(emptyList())

        // Act
        viewModel.submitMatchResult(matchId, winnerTeamId, false)
        advanceUntilIdle()

        // Assert
        coVerify { mockRepository.submitMatchResult(matchId, winnerTeamId, false) }
    }

    // ─── Tournament Management Tests ───

    @Test
    fun `awardMatchPoints successfully awards points`() {
        // Arrange
        val matchId = "match123"
        coEvery { mockRepository.awardMatchPoints(matchId) } returns Result.success(Unit)

        // Act
        viewModel.awardMatchPoints(matchId)
        advanceUntilIdle()

        // Assert
        coVerify { mockRepository.awardMatchPoints(matchId) }
    }

    @Test
    fun `updateTournamentScores successfully updates scores`() {
        // Arrange
        val tournamentId = "tournament123"
        val mockTournament = createMockTournament(id = tournamentId)
        coEvery { mockRepository.updateTournamentScores(tournamentId) } returns Result.success(Unit)
        coEvery { mockRepository.getTournamentById(tournamentId) } returns Result.success(mockTournament)
        coEvery { mockRepository.getTournamentRequirements(tournamentId) } returns Result.success(emptyList())
        coEvery { mockRepository.getTournamentTeams(tournamentId) } returns Result.success(emptyList())
        coEvery { mockRepository.getTournamentMatches(tournamentId) } returns Result.success(emptyList())
        coEvery { mockRepository.getMyApplications(any()) } returns Result.success(emptyList())

        // Act
        viewModel.updateTournamentScores(tournamentId)
        advanceUntilIdle()

        // Assert
        coVerify { mockRepository.updateTournamentScores(tournamentId) }
    }

    @Test
    fun `disqualifyTeam successfully disqualifies team`() {
        // Arrange
        val tournamentId = "tournament123"
        val teamId = "team123"
        val reason = "Cheating"
        val mockTournament = createMockTournament(id = tournamentId)
        coEvery { mockRepository.disqualifyTeam(tournamentId, teamId, reason) } returns Result.success(Unit)
        coEvery { mockRepository.getTournamentById(tournamentId) } returns Result.success(mockTournament)
        coEvery { mockRepository.getTournamentRequirements(tournamentId) } returns Result.success(emptyList())
        coEvery { mockRepository.getTournamentTeams(tournamentId) } returns Result.success(emptyList())
        coEvery { mockRepository.getTournamentMatches(tournamentId) } returns Result.success(emptyList())
        coEvery { mockRepository.getMyApplications(any()) } returns Result.success(emptyList())

        // Act
        viewModel.disqualifyTeam(tournamentId, teamId, reason)
        advanceUntilIdle()

        // Assert
        coVerify { mockRepository.disqualifyTeam(tournamentId, teamId, reason) }
    }

    @Test
    fun `cancelTournament successfully cancels tournament`() {
        // Arrange
        val tournamentId = "tournament123"
        val reason = "Not enough participants"
        val mockTournament = createMockTournament(id = tournamentId)
        coEvery { mockRepository.cancelTournament(tournamentId, reason) } returns Result.success(Unit)
        coEvery { mockRepository.getTournamentById(tournamentId) } returns Result.success(mockTournament)
        coEvery { mockRepository.getTournamentRequirements(tournamentId) } returns Result.success(emptyList())
        coEvery { mockRepository.getTournamentTeams(tournamentId) } returns Result.success(emptyList())
        coEvery { mockRepository.getTournamentMatches(tournamentId) } returns Result.success(emptyList())
        coEvery { mockRepository.getMyApplications(any()) } returns Result.success(emptyList())
        coEvery { mockRepository.getTournaments(any(), any(), any()) } returns Result.success(emptyList())

        // Act
        viewModel.cancelTournament(tournamentId, reason)
        advanceUntilIdle()

        // Assert
        coVerify { mockRepository.cancelTournament(tournamentId, reason) }
    }

    @Test
    fun `checkInTeam successfully checks in team`() {
        // Arrange
        val tournamentId = "tournament123"
        val teamId = "team123"
        val mockTournament = createMockTournament(id = tournamentId)
        coEvery { mockRepository.checkInTeam(tournamentId, teamId) } returns Result.success(Unit)
        coEvery { mockRepository.getTournamentById(tournamentId) } returns Result.success(mockTournament)
        coEvery { mockRepository.getTournamentRequirements(tournamentId) } returns Result.success(emptyList())
        coEvery { mockRepository.getTournamentTeams(tournamentId) } returns Result.success(emptyList())
        coEvery { mockRepository.getTournamentMatches(tournamentId) } returns Result.success(emptyList())
        coEvery { mockRepository.getMyApplications(any()) } returns Result.success(emptyList())

        // Act
        viewModel.checkInTeam(tournamentId, teamId)
        advanceUntilIdle()

        // Assert
        coVerify { mockRepository.checkInTeam(tournamentId, teamId) }
    }

    // ─── Clear State Tests ───

    @Test
    fun `clearError clears error message`() {
        // Arrange
        viewModel._error.value = "Test error"

        // Act
        viewModel.clearError()

        // Assert
        assertEquals(null, viewModel.error.value)
    }

    @Test
    fun `clearApplyResult clears apply result`() {
        // Arrange
        viewModel._applyResult.value = Result.success(emptyMap())

        // Act
        viewModel.clearApplyResult()

        // Assert
        assertEquals(null, viewModel.applyResult.value)
    }

    @Test
    fun `clearCreateResult clears create result`() {
        // Arrange
        viewModel._createResult.value = Result.success(createMockTournament())

        // Act
        viewModel.clearCreateResult()

        // Assert
        assertEquals(null, viewModel.createResult.value)
    }

    @Test
    fun `clearUpdateResult clears update result`() {
        // Arrange
        viewModel._updateResult.value = Result.success(createMockTournament())

        // Act
        viewModel.clearUpdateResult()

        // Assert
        assertEquals(null, viewModel.updateResult.value)
    }

    @Test
    fun `clearHostRequestResult clears host request result`() {
        // Arrange
        viewModel._hostRequestResult.value = Result.success(createMockTournamentHostRequest())

        // Act
        viewModel.clearHostRequestResult()

        // Assert
        assertEquals(null, viewModel.hostRequestResult.value)
    }

    // ─── Helper Functions ───

    private fun createMockTournament(
        id: String = "tournament-id",
        name: String = "Test Tournament",
        hostUserId: String = "user123"
    ): Tournament {
        return Tournament(
            id = id,
            name = name,
            description = "Test description",
            hostUserId = hostUserId,
            hostUserName = "Host",
            region = "NA",
            skillLevel = "Diamond",
            maxTeams = 16,
            status = "registration",
            format = "swiss",
            rules = emptyList(),
            requirements = emptyList(),
            createdAt = System.currentTimeMillis(),
            startsAt = System.currentTimeMillis() + 86400000
        )
    }

    private fun createMockTournamentRequirement(): TournamentRequirement {
        return TournamentRequirement(
            id = "req1",
            tournamentId = "tournament123",
            type = "min_rank",
            value = "Diamond",
            description = "Minimum Diamond rank"
        )
    }

    private fun createMockTournamentTeam(): TournamentTeam {
        return TournamentTeam(
            id = "team1",
            tournamentId = "tournament123",
            teamId = "team123",
            teamName = "Team 1",
            points = 0,
            wins = 0,
            losses = 0,
            draws = 0,
            buchholz = 0.0,
            opponents = emptyList()
        )
    }

    private fun createMockTournamentSwissMatch(
        teamAId: String = "team1",
        teamBId: String = "team2"
    ): TournamentSwissMatch {
        return TournamentSwissMatch(
            id = "match1",
            tournamentId = "tournament123",
            round = 1,
            teamAId = teamAId,
            teamBId = teamBId,
            teamAScore = 0,
            teamBScore = 0,
            status = "scheduled",
            scheduledAt = System.currentTimeMillis() + 3600000,
            isMyMatch = false
        )
    }

    private fun createMockTournamentApplication(): TournamentApplication {
        return TournamentApplication(
            id = "app1",
            tournamentId = "tournament123",
            teamId = "team123",
            teamName = "Team 1",
            status = "pending",
            appliedAt = System.currentTimeMillis()
        )
    }

    private fun createMockTournamentHostRequest(): TournamentHostRequest {
        return TournamentHostRequest(
            id = "host1",
            userId = "user123",
            motivation = "I want to host tournaments",
            experience = "5 years",
            telegramChannel = "@host",
            socialLinks = listOf("https://twitter.com/host"),
            status = "pending",
            createdAt = System.currentTimeMillis()
        )
    }

    private fun createMockTournamentMatchRoster(): TournamentMatchRoster {
        return TournamentMatchRoster(
            id = "roster1",
            matchId = "match1",
            teamId = "team1",
            gameNumber = 1,
            playerIds = listOf("player1", "player2", "player3", "player4", "player5")
        )
    }

    private fun createMockTournamentMatchRoomSecret(): TournamentMatchRoomSecret {
        return TournamentMatchRoomSecret(
            id = "secret1",
            matchId = "match1",
            roomSecret = "secret123",
            expiresAt = System.currentTimeMillis() + 3600000
        )
    }
}

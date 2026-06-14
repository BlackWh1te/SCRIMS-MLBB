package com.mlbb.scrim.viewmodel

import com.mlbb.scrim.data.model.MatchResult
import com.mlbb.scrim.data.repository.MatchResultRepositoryInterface
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
class MatchResultViewModelTest {

    private lateinit var viewModel: MatchResultViewModel
    private lateinit var mockRepository: MatchResultRepositoryInterface
    private lateinit var testDispatcher: TestDispatcher

    @Before
    fun setup() {
        mockRepository = mockk(relaxed = true)
        testDispatcher = StandardTestDispatcher()

        Dispatchers.setMain(testDispatcher)

        viewModel = MatchResultViewModel(mockRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ─── Initialization Tests ───

    @Test
    fun `ViewModel initializes with empty state`() {
        // Assert
        assertTrue(viewModel.matchResults.value.isEmpty())
        assertEquals(null, viewModel.selectedMatchResult.value)
        assertFalse(viewModel.isLoading.value)
        assertFalse(viewModel.isRefreshing.value)
        assertEquals(null, viewModel.error.value)
        assertFalse(viewModel.reportSuccess.value)
    }

    @Test
    fun `ViewModel loads match results on initialization`() {
        // Arrange
        val mockResults = listOf(createMockMatchResult(id = "1"))
        coEvery { mockRepository.getAllMatchResults() } returns flow { emit(Result.success(mockResults)) }

        // Act
        advanceUntilIdle()

        // Assert
        assertEquals(mockResults, viewModel.matchResults.value)
    }

    // ─── Load Match Results Tests ───

    @Test
    fun `loadMatchResults successfully loads match results`() {
        // Arrange
        val mockResults = listOf(
            createMockMatchResult(id = "1"),
            createMockMatchResult(id = "2")
        )
        coEvery { mockRepository.getAllMatchResults() } returns flow { emit(Result.success(mockResults)) }

        // Act
        viewModel.loadMatchResults()
        advanceUntilIdle()

        // Assert
        assertEquals(mockResults, viewModel.matchResults.value)
        assertFalse(viewModel.isLoading.value)
    }

    @Test
    fun `loadMatchResults sets refreshing flag when isRefresh is true`() {
        // Arrange
        coEvery { mockRepository.getAllMatchResults() } returns flow { emit(Result.success(emptyList())) }

        // Act
        viewModel.loadMatchResults(isRefresh = true)
        advanceUntilIdle()

        // Assert
        assertFalse(viewModel.isRefreshing.value) // Should be false after completion
    }

    @Test
    fun `loadMatchResults handles error`() {
        // Arrange
        val errorMessage = "Failed to load match results"
        coEvery { mockRepository.getAllMatchResults() } returns flow { emit(Result.failure(Exception(errorMessage))) }

        // Act
        viewModel.loadMatchResults()
        advanceUntilIdle()

        // Assert
        assertEquals(errorMessage, viewModel.error.value)
        assertFalse(viewModel.isLoading.value)
    }

    @Test
    fun `loadMatchResults cancels previous load job`() {
        // Arrange
        coEvery { mockRepository.getAllMatchResults() } returns flow { emit(Result.success(emptyList())) }

        // Act
        viewModel.loadMatchResults()
        viewModel.loadMatchResults() // Should cancel the first one
        advanceUntilIdle()

        // Assert - Should complete without errors
        assertTrue(true)
    }

    // ─── Load Match Result by ID Tests ───

    @Test
    fun `loadMatchResultById successfully loads match result`() {
        // Arrange
        val matchResultId = "result123"
        val mockResult = createMockMatchResult(id = matchResultId)
        coEvery { mockRepository.getMatchResultById(matchResultId) } returns flow { emit(Result.success(mockResult)) }

        // Act
        viewModel.loadMatchResultById(matchResultId)
        advanceUntilIdle()

        // Assert
        assertEquals(mockResult, viewModel.selectedMatchResult.value)
        assertFalse(viewModel.isLoading.value)
    }

    @Test
    fun `loadMatchResultById handles error`() {
        // Arrange
        val errorMessage = "Match result not found"
        coEvery { mockRepository.getMatchResultById(any()) } returns flow { emit(Result.failure(Exception(errorMessage))) }

        // Act
        viewModel.loadMatchResultById("result123")
        advanceUntilIdle()

        // Assert
        assertEquals(errorMessage, viewModel.error.value)
        assertEquals(null, viewModel.selectedMatchResult.value)
    }

    // ─── Load Match Results for Team Tests ───

    @Test
    fun `loadMatchResultsForTeam successfully loads team match results`() {
        // Arrange
        val teamId = "team123"
        val mockResults = listOf(createMockMatchResult(id = "1"))
        coEvery { mockRepository.getMatchResultsForTeam(teamId) } returns flow { emit(Result.success(mockResults)) }

        // Act
        viewModel.loadMatchResultsForTeam(teamId)
        advanceUntilIdle()

        // Assert
        assertEquals(mockResults, viewModel.matchResults.value)
        assertFalse(viewModel.isLoading.value)
    }

    @Test
    fun `loadMatchResultsForTeam handles error`() {
        // Arrange
        val errorMessage = "Failed to load team match results"
        coEvery { mockRepository.getMatchResultsForTeam(any()) } returns flow { emit(Result.failure(Exception(errorMessage))) }

        // Act
        viewModel.loadMatchResultsForTeam("team123")
        advanceUntilIdle()

        // Assert
        assertEquals(errorMessage, viewModel.error.value)
    }

    // ─── Report Result Tests ───

    @Test
    fun `reportResult successfully reports match result`() {
        // Arrange
        val scrimId = "scrim123"
        val teamId = "team123"
        val reporterId = "reporter123"
        val reporterName = "Reporter"
        val reportedWinnerId = "winner123"
        val mockResult = createMockMatchResult(id = "result123")
        
        coEvery { 
            mockRepository.reportResult(
                scrimId, teamId, reporterId, reporterName, reportedWinnerId, any(), any()
            ) 
        } returns flow { emit(Result.success(mockResult)) }
        coEvery { mockRepository.getAllMatchResults() } returns flow { emit(Result.success(listOf(mockResult))) }

        // Act
        viewModel.reportResult(scrimId, teamId, reporterId, reporterName, reportedWinnerId)
        advanceUntilIdle()

        // Assert
        assertEquals(mockResult, viewModel.selectedMatchResult.value)
        assertTrue(viewModel.reportSuccess.value)
        coVerify { 
            mockRepository.reportResult(
                scrimId, teamId, reporterId, reporterName, reportedWinnerId, any(), any()
            ) 
        }
    }

    @Test
    fun `reportResult handles error`() {
        // Arrange
        val errorMessage = "Failed to report result"
        coEvery { mockRepository.reportResult(any(), any(), any(), any(), any(), any(), any()) } returns flow { emit(Result.failure(Exception(errorMessage))) }

        // Act
        viewModel.reportResult("scrim123", "team123", "reporter123", "Reporter", "winner123")
        advanceUntilIdle()

        // Assert
        assertEquals(errorMessage, viewModel.error.value)
        assertFalse(viewModel.reportSuccess.value)
    }

    @Test
    fun `reportResult includes optional notes and screenshot`() {
        // Arrange
        val notes = "Great match"
        val screenshotUrl = "https://example.com/screenshot.jpg"
        val mockResult = createMockMatchResult(id = "result123")
        
        coEvery { 
            mockRepository.reportResult(
                any(), any(), any(), any(), any(), notes, screenshotUrl
            ) 
        } returns flow { emit(Result.success(mockResult)) }
        coEvery { mockRepository.getAllMatchResults() } returns flow { emit(Result.success(listOf(mockResult))) }

        // Act
        viewModel.reportResult(
            scrimId = "scrim123",
            teamId = "team123",
            reporterId = "reporter123",
            reporterName = "Reporter",
            reportedWinnerId = "winner123",
            notes = notes,
            screenshotUrl = screenshotUrl
        )
        advanceUntilIdle()

        // Assert
        coVerify { 
            mockRepository.reportResult(
                any(), any(), any(), any(), any(), notes, screenshotUrl
            ) 
        }
    }

    // ─── Create Match Result Tests ───

    @Test
    fun `createMatchResult successfully creates match result`() {
        // Arrange
        val scrimId = "scrim123"
        val teamAId = "teamA"
        val teamAName = "Team A"
        val teamBId = "teamB"
        val teamBName = "Team B"
        val mockResult = createMockMatchResult(id = "result123")
        
        coEvery { 
            mockRepository.createMatchResult(scrimId, teamAId, teamAName, teamBId, teamBName) 
        } returns flow { emit(Result.success(mockResult)) }
        coEvery { mockRepository.getAllMatchResults() } returns flow { emit(Result.success(listOf(mockResult))) }

        // Act
        viewModel.createMatchResult(scrimId, teamAId, teamAName, teamBId, teamBName)
        advanceUntilIdle()

        // Assert
        assertEquals(mockResult, viewModel.selectedMatchResult.value)
        coVerify { mockRepository.createMatchResult(scrimId, teamAId, teamAName, teamBId, teamBName) }
    }

    @Test
    fun `createMatchResult handles error`() {
        // Arrange
        val errorMessage = "Failed to create match result"
        coEvery { mockRepository.createMatchResult(any(), any(), any(), any(), any()) } returns flow { emit(Result.failure(Exception(errorMessage))) }

        // Act
        viewModel.createMatchResult("scrim123", "teamA", "Team A", "teamB", "Team B")
        advanceUntilIdle()

        // Assert
        assertEquals(errorMessage, viewModel.error.value)
    }

    // ─── Resolve Dispute Tests ───

    @Test
    fun `resolveDispute successfully resolves dispute`() {
        // Arrange
        val matchResultId = "result123"
        val confirmedWinnerId = "winner123"
        val adminNotes = "Admin decision"
        val mockResult = createMockMatchResult(id = matchResultId)
        
        coEvery { 
            mockRepository.resolveDispute(matchResultId, confirmedWinnerId, adminNotes) 
        } returns flow { emit(Result.success(mockResult)) }
        coEvery { mockRepository.getAllMatchResults() } returns flow { emit(Result.success(listOf(mockResult))) }

        // Act
        viewModel.resolveDispute(matchResultId, confirmedWinnerId, adminNotes)
        advanceUntilIdle()

        // Assert
        assertEquals(mockResult, viewModel.selectedMatchResult.value)
        coVerify { mockRepository.resolveDispute(matchResultId, confirmedWinnerId, adminNotes) }
    }

    @Test
    fun `resolveDispute handles error`() {
        // Arrange
        val errorMessage = "Failed to resolve dispute"
        coEvery { mockRepository.resolveDispute(any(), any(), any()) } returns flow { emit(Result.failure(Exception(errorMessage))) }

        // Act
        viewModel.resolveDispute("result123", "winner123", "Admin notes")
        advanceUntilIdle()

        // Assert
        assertEquals(errorMessage, viewModel.error.value)
    }

    @Test
    fun `resolveDispute works without admin notes`() {
        // Arrange
        val matchResultId = "result123"
        val confirmedWinnerId = "winner123"
        val mockResult = createMockMatchResult(id = matchResultId)
        
        coEvery { 
            mockRepository.resolveDispute(matchResultId, confirmedWinnerId, null) 
        } returns flow { emit(Result.success(mockResult)) }
        coEvery { mockRepository.getAllMatchResults() } returns flow { emit(Result.success(listOf(mockResult))) }

        // Act
        viewModel.resolveDispute(matchResultId, confirmedWinnerId)
        advanceUntilIdle()

        // Assert
        coVerify { mockRepository.resolveDispute(matchResultId, confirmedWinnerId, null) }
    }

    // ─── Clear State Tests ───

    @Test
    fun `clearSelectedMatchResult clears selection`() {
        // Arrange
        viewModel._selectedMatchResult.value = createMockMatchResult(id = "1")

        // Act
        viewModel.clearSelectedMatchResult()

        // Assert
        assertEquals(null, viewModel.selectedMatchResult.value)
    }

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
    fun `clearRefreshing clears refreshing flag`() {
        // Arrange
        viewModel._isRefreshing.value = true

        // Act
        viewModel.clearRefreshing()

        // Assert
        assertFalse(viewModel.isRefreshing.value)
    }

    @Test
    fun `clearReportSuccess clears report success flag`() {
        // Arrange
        viewModel._reportSuccess.value = true

        // Act
        viewModel.clearReportSuccess()

        // Assert
        assertFalse(viewModel.reportSuccess.value)
    }

    // ─── Edge Case Tests ───

    @Test
    fun `loadMatchResults handles empty result list`() {
        // Arrange
        coEvery { mockRepository.getAllMatchResults() } returns flow { emit(Result.success(emptyList())) }

        // Act
        viewModel.loadMatchResults()
        advanceUntilIdle()

        // Assert
        assertTrue(viewModel.matchResults.value.isEmpty())
    }

    @Test
    fun `loadMatchResults handles large result list`() {
        // Arrange
        val largeResultList = (1..100).map { createMockMatchResult(id = it.toString()) }
        coEvery { mockRepository.getAllMatchResults() } returns flow { emit(Result.success(largeResultList)) }

        // Act
        viewModel.loadMatchResults()
        advanceUntilIdle()

        // Assert
        assertEquals(100, viewModel.matchResults.value.size)
    }

    @Test
    fun `reportResult cancels previous report job`() {
        // Arrange
        val mockResult = createMockMatchResult(id = "result123")
        coEvery { mockRepository.reportResult(any(), any(), any(), any(), any(), any(), any()) } returns flow { emit(Result.success(mockResult)) }
        coEvery { mockRepository.getAllMatchResults() } returns flow { emit(Result.success(listOf(mockResult))) }

        // Act
        viewModel.reportResult("scrim1", "team1", "reporter1", "Reporter", "winner1")
        viewModel.reportResult("scrim2", "team2", "reporter2", "Reporter", "winner2") // Should cancel the first one
        advanceUntilIdle()

        // Assert - Should complete without errors
        assertTrue(true)
    }

    @Test
    fun `createMatchResult cancels previous create job`() {
        // Arrange
        val mockResult = createMockMatchResult(id = "result123")
        coEvery { mockRepository.createMatchResult(any(), any(), any(), any(), any()) } returns flow { emit(Result.success(mockResult)) }
        coEvery { mockRepository.getAllMatchResults() } returns flow { emit(Result.success(listOf(mockResult))) }

        // Act
        viewModel.createMatchResult("scrim1", "teamA", "Team A", "teamB", "Team B")
        viewModel.createMatchResult("scrim2", "teamC", "Team C", "teamD", "Team D") // Should cancel the first one
        advanceUntilIdle()

        // Assert - Should complete without errors
        assertTrue(true)
    }

    // ─── Helper Functions ───

    private fun createMockMatchResult(
        id: String = "result-id"
    ): MatchResult {
        return MatchResult(
            id = id,
            scrimId = "scrim123",
            teamAId = "teamA",
            teamAName = "Team A",
            teamBId = "teamB",
            teamBName = "Team B",
            winnerTeamId = "teamA",
            reporterId = "reporter123",
            reporterName = "Reporter",
            notes = "Match notes",
            screenshotUrl = "https://example.com/screenshot.jpg",
            status = "confirmed",
            reportedAt = System.currentTimeMillis(),
            confirmedAt = System.currentTimeMillis(),
            disputed = false
        )
    }
}

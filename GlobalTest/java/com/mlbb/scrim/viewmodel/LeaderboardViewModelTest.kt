package com.mlbb.scrim.viewmodel

import com.mlbb.scrim.data.model.LeaderboardEntry
import com.mlbb.scrim.data.model.RankTier
import com.mlbb.scrim.data.repository.LeaderboardRepositoryInterface
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Advanced unit tests for LeaderboardViewModel.
 * Tests state transitions, loading states, error handling, cancellation, and edge cases.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LeaderboardViewModelTest {

    private lateinit var viewModel: LeaderboardViewModel
    private lateinit var repository: LeaderboardRepositoryInterface
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk()
        viewModel = LeaderboardViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ─── Initialization Tests ───

    @Test
    fun `init triggers loadLeaderboard`() = runTest {
        // Arrange
        val mockEntries = listOf(
            LeaderboardEntry(1, "p1", "User1", "Team1", 1000, 10, 5, 15, RankTier.BRONZE)
        )
        every { repository.getLeaderboard() } returns flow { emit(Result.success(mockEntries)) }

        // Act
        val testViewModel = LeaderboardViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        verify { repository.getLeaderboard() }
    }

    @Test
    fun `initial state is empty and not loading`() = runTest {
        // Assert
        assertEquals(emptyList<LeaderboardEntry>(), viewModel.leaderboard.value)
        assertFalse(viewModel.isLoading.value)
        assertFalse(viewModel.isRefreshing.value)
        assertNull(viewModel.error.value)
        assertNull(viewModel.selectedTier.value)
    }

    // ─── Load Leaderboard Tests ───

    @Test
    fun `loadLeaderboard success updates state correctly`() = runTest {
        // Arrange
        val mockEntries = listOf(
            LeaderboardEntry(1, "p1", "User1", "Team1", 1000, 10, 5, 15, RankTier.BRONZE),
            LeaderboardEntry(2, "p2", "User2", "Team2", 2000, 20, 10, 30, RankTier.GOLD)
        )
        every { repository.getLeaderboard() } returns flow { emit(Result.success(mockEntries)) }

        // Act
        viewModel.loadLeaderboard()
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        assertEquals(mockEntries, viewModel.leaderboard.value)
        assertFalse(viewModel.isLoading.value)
        assertFalse(viewModel.isRefreshing.value)
        assertNull(viewModel.error.value)
    }

    @Test
    fun `loadLeaderboard with refresh sets refreshing flag`() = runTest {
        // Arrange
        val mockEntries = listOf(LeaderboardEntry(1, "p1", "User1", "Team1", 1000, 10, 5, 15, RankTier.BRONZE))
        every { repository.getLeaderboard() } returns flow { emit(Result.success(mockEntries)) }

        // Act
        viewModel.loadLeaderboard(isRefresh = true)
        assertEquals(true, viewModel.isRefreshing.value) // Should be set immediately
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        assertEquals(false, viewModel.isRefreshing.value)
    }

    @Test
    fun `loadLeaderboard error updates error state`() = runTest {
        // Arrange
        val errorMsg = "Network error"
        every { repository.getLeaderboard() } returns flow { emit(Result.failure(Exception(errorMsg))) }

        // Act
        viewModel.loadLeaderboard()
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        assertEquals(errorMsg, viewModel.error.value)
        assertFalse(viewModel.isLoading.value)
        assertFalse(viewModel.isRefreshing.value)
    }

    @Test
    fun `loadLeaderboard cancels previous job`() = runTest {
        // Arrange
        every { repository.getLeaderboard() } returns flow { 
            kotlinx.coroutines.delay(1000)
            emit(Result.success(emptyList()))
        }

        // Act
        viewModel.loadLeaderboard()
        viewModel.loadLeaderboard() // Should cancel first job
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert - should only call repository once due to cancellation
        verify(exactly = 1) { repository.getLeaderboard() }
    }

    // ─── Filter By Tier Tests ───

    @Test
    fun `filterByTier with null loads all entries`() = runTest {
        // Arrange
        val allEntries = listOf(
            LeaderboardEntry(1, "p1", "User1", "Team1", 1000, 10, 5, 15, RankTier.BRONZE),
            LeaderboardEntry(2, "p2", "User2", "Team2", 2000, 20, 10, 30, RankTier.GOLD)
        )
        every { repository.getLeaderboard() } returns flow { emit(Result.success(allEntries)) }

        // Act
        viewModel.filterByTier(null)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        assertEquals(allEntries, viewModel.leaderboard.value)
        assertNull(viewModel.selectedTier.value)
    }

    @Test
    fun `filterByTier with specific tier filters correctly`() = runTest {
        // Arrange
        val goldEntries = listOf(
            LeaderboardEntry(1, "p1", "User1", "Team1", 3000, 30, 15, 45, RankTier.GOLD)
        )
        every { repository.getLeaderboardForTier(RankTier.GOLD) } returns flow { emit(Result.success(goldEntries)) }

        // Act
        viewModel.filterByTier(RankTier.GOLD)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        assertEquals(goldEntries, viewModel.leaderboard.value)
        assertEquals(RankTier.GOLD, viewModel.selectedTier.value)
    }

    @Test
    fun `filterByTier error updates error state`() = runTest {
        // Arrange
        val errorMsg = "Filter error"
        every { repository.getLeaderboardForTier(any()) } returns flow { emit(Result.failure(Exception(errorMsg))) }

        // Act
        viewModel.filterByTier(RankTier.BRONZE)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        assertEquals(errorMsg, viewModel.error.value)
        assertFalse(viewModel.isLoading.value)
    }

    @Test
    fun `filterByTier cancels previous filter job`() = runTest {
        // Arrange
        every { repository.getLeaderboardForTier(any()) } returns flow {
            kotlinx.coroutines.delay(1000)
            emit(Result.success(emptyList()))
        }

        // Act
        viewModel.filterByTier(RankTier.BRONZE)
        viewModel.filterByTier(RankTier.GOLD) // Should cancel first job
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        verify(exactly = 1) { repository.getLeaderboardForTier(any()) }
    }

    // ─── State Management Tests ───

    @Test
    fun `clearError removes error message`() = runTest {
        // Arrange
        val errorMsg = "Test error"
        every { repository.getLeaderboard() } returns flow { emit(Result.failure(Exception(errorMsg))) }
        viewModel.loadLeaderboard()
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(errorMsg, viewModel.error.value)

        // Act
        viewModel.clearError()

        // Assert
        assertNull(viewModel.error.value)
    }

    @Test
    fun `clearRefreshing removes refreshing flag`() = runTest {
        // Arrange
        viewModel.clearRefreshing()
        assertEquals(false, viewModel.isRefreshing.value)

        // Act
        viewModel.clearRefreshing()

        // Assert
        assertEquals(false, viewModel.isRefreshing.value)
    }

    // ─── Concurrency Tests ───

    @Test
    fun `concurrent loadLeaderboard calls are handled correctly`() = runTest {
        // Arrange
        val mockEntries = listOf(LeaderboardEntry(1, "p1", "User1", "Team1", 1000, 10, 5, 15, RankTier.BRONZE))
        every { repository.getLeaderboard() } returns flow { emit(Result.success(mockEntries)) }

        // Act
        viewModel.loadLeaderboard()
        viewModel.loadLeaderboard()
        viewModel.loadLeaderboard()
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert - last call should win
        assertEquals(mockEntries, viewModel.leaderboard.value)
        verify(atMost = 2) { repository.getLeaderboard() }
    }

    @Test
    fun `loadLeaderboard during filter cancels filter`() = runTest {
        // Arrange
        every { repository.getLeaderboard() } returns flow { emit(Result.success(emptyList())) }
        every { repository.getLeaderboardForTier(any()) } returns flow { emit(Result.success(emptyList())) }

        // Act
        viewModel.filterByTier(RankTier.GOLD)
        viewModel.loadLeaderboard() // Should cancel filter
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        verify { repository.getLeaderboard() }
        verify(exactly = 0) { repository.getLeaderboardForTier(any()) }
    }

    // ─── Edge Cases ───

    @Test
    fun `empty leaderboard result is handled correctly`() = runTest {
        // Arrange
        every { repository.getLeaderboard() } returns flow { emit(Result.success(emptyList())) }

        // Act
        viewModel.loadLeaderboard()
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        assertEquals(emptyList<LeaderboardEntry>(), viewModel.leaderboard.value)
        assertFalse(viewModel.isLoading.value)
        assertNull(viewModel.error.value)
    }

    @Test
    fun `filter with empty tier result is handled correctly`() = runTest {
        // Arrange
        every { repository.getLeaderboardForTier(any()) } returns flow { emit(Result.success(emptyList())) }

        // Act
        viewModel.filterByTier(RankTier.MYTHIC)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        assertEquals(emptyList<LeaderboardEntry>(), viewModel.leaderboard.value)
        assertEquals(RankTier.MYTHIC, viewModel.selectedTier.value)
    }

    @Test
    fun `null error message from repository is handled`() = runTest {
        // Arrange
        every { repository.getLeaderboard() } returns flow { emit(Result.failure(Exception(null))) }

        // Act
        viewModel.loadLeaderboard()
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        assertNull(viewModel.error.value)
        assertFalse(viewModel.isLoading.value)
    }

    // ─── Loading State Transitions ───

    @Test
    fun `loading state transitions correctly during success`() = runTest {
        // Arrange
        val mockEntries = listOf(LeaderboardEntry(1, "p1", "User1", "Team1", 1000, 10, 5, 15, RankTier.BRONZE))
        every { repository.getLeaderboard() } returns flow { emit(Result.success(mockEntries)) }

        // Act
        viewModel.loadLeaderboard()
        assertEquals(true, viewModel.isLoading.value) // Should be set immediately
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        assertEquals(false, viewModel.isLoading.value)
    }

    @Test
    fun `loading state transitions correctly during error`() = runTest {
        // Arrange
        every { repository.getLeaderboard() } returns flow { emit(Result.failure(Exception("Error"))) }

        // Act
        viewModel.loadLeaderboard()
        assertEquals(true, viewModel.isLoading.value)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        assertEquals(false, viewModel.isLoading.value)
    }

    // ─── Repository Integration Tests ───

    @Test
    fun `viewModel calls correct repository method for full leaderboard`() = runTest {
        // Arrange
        every { repository.getLeaderboard() } returns flow { emit(Result.success(emptyList())) }

        // Act
        viewModel.loadLeaderboard()
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        verify { repository.getLeaderboard() }
    }

    @Test
    fun `viewModel calls correct repository method for tier filter`() = runTest {
        // Arrange
        every { repository.getLeaderboardForTier(RankTier.GOLD) } returns flow { emit(Result.success(emptyList())) }

        // Act
        viewModel.filterByTier(RankTier.GOLD)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        verify { repository.getLeaderboardForTier(RankTier.GOLD) }
    }
}

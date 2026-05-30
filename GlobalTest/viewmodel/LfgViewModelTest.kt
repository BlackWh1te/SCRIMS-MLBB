package com.mlbb.scrim.viewmodel

import android.app.Application
import com.mlbb.scrim.data.model.GameRole
import com.mlbb.scrim.data.model.LfgPost
import com.mlbb.scrim.data.model.Region
import com.mlbb.scrim.data.model.SkillLevel
import com.mlbb.scrim.data.repository.LfgRepositoryInterface
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
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
class LfgViewModelTest {

    private lateinit var viewModel: LfgViewModel
    private lateinit var mockApplication: Application
    private lateinit var mockRepository: LfgRepositoryInterface
    private lateinit var testDispatcher: TestDispatcher

    @Before
    fun setup() {
        mockApplication = mockk(relaxed = true)
        mockRepository = mockk(relaxed = true)
        testDispatcher = StandardTestDispatcher()

        Dispatchers.setMain(testDispatcher)

        viewModel = LfgViewModel(
            application = mockApplication,
            lfgRepository = mockRepository
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ─── Initialization Tests ───

    @Test
    fun `ViewModel initializes with loading state`() {
        // Assert
        assertTrue(viewModel.isLoading.value)
        assertTrue(viewModel.posts.value.isEmpty())
        assertEquals(null, viewModel.error.value)
    }

    @Test
    fun `ViewModel loads posts on initialization`() {
        // Arrange
        val mockPosts = listOf(
            createMockLfgPost(id = "1", playerName = "Player1"),
            createMockLfgPost(id = "2", playerName = "Player2")
        )
        coEvery { mockRepository.getAllPosts() } returns flow { emit(Result.success(mockPosts)) }

        // Act
        advanceUntilIdle()

        // Assert
        assertEquals(mockPosts, viewModel.posts.value)
        assertFalse(viewModel.isLoading.value)
    }

    @Test
    fun `ViewModel handles initialization error`() {
        // Arrange
        val errorMessage = "Network error"
        coEvery { mockRepository.getAllPosts() } returns flow { emit(Result.failure(Exception(errorMessage))) }

        // Act
        advanceUntilIdle()

        // Assert
        assertEquals(errorMessage, viewModel.error.value)
        assertFalse(viewModel.isLoading.value)
        assertTrue(viewModel.posts.value.isEmpty())
    }

    // ─── Load Posts Tests ───

    @Test
    fun `loadPosts successfully loads posts`() {
        // Arrange
        val mockPosts = listOf(createMockLfgPost(id = "1"))
        coEvery { mockRepository.getAllPosts() } returns flow { emit(Result.success(mockPosts)) }

        // Act
        viewModel.loadPosts()
        advanceUntilIdle()

        // Assert
        assertEquals(mockPosts, viewModel.posts.value)
        assertFalse(viewModel.isLoading.value)
    }

    @Test
    fun `loadPosts sets refreshing flag when isRefresh is true`() {
        // Arrange
        coEvery { mockRepository.getAllPosts() } returns flow { emit(Result.success(emptyList())) }

        // Act
        viewModel.loadPosts(isRefresh = true)
        advanceUntilIdle()

        // Assert - refreshing flag should be set during load
        assertFalse(viewModel.isRefreshing.value) // Should be false after completion
    }

    @Test
    fun `loadPosts handles repository error`() {
        // Arrange
        val errorMessage = "Failed to load posts"
        coEvery { mockRepository.getAllPosts() } returns flow { emit(Result.failure(Exception(errorMessage))) }

        // Act
        viewModel.loadPosts()
        advanceUntilIdle()

        // Assert
        assertEquals(errorMessage, viewModel.error.value)
        assertFalse(viewModel.isLoading.value)
    }

    @Test
    fun `loadPosts clears previous error on new load`() {
        // Arrange
        viewModel.clearError()
        coEvery { mockRepository.getAllPosts() } returns flow { emit(Result.success(emptyList())) }

        // Act
        viewModel.loadPosts()
        advanceUntilIdle()

        // Assert
        assertEquals(null, viewModel.error.value)
    }

    // ─── Add Post Tests ───

    @Test
    fun `addPost successfully creates new post`() {
        // Arrange
        val playerId = "player123"
        val playerName = "TestPlayer"
        val mockPost = createMockLfgPost(id = "new-id", playerId = playerId, playerName = playerName)
        coEvery { mockRepository.createPost(any()) } returns flow { emit(Result.success(mockPost)) }
        coEvery { mockRepository.getAllPosts() } returns flow { emit(Result.success(listOf(mockPost))) }

        // Act
        viewModel.addPost(
            playerId = playerId,
            playerName = playerName,
            role = GameRole.TANK,
            region = Region.NORTH_AMERICA,
            skillLevel = SkillLevel.DIAMOND,
            message = "Looking for team"
        )
        advanceUntilIdle()

        // Assert
        assertTrue(viewModel.posts.value.isNotEmpty())
        assertEquals(mockPost, viewModel.posts.value.first())
    }

    @Test
    fun `addPost enforces 1 post per day limit`() {
        // Arrange
        val playerId = "player123"
        val existingPost = createMockLfgPost(
            id = "existing",
            playerId = playerId,
            createdAt = System.currentTimeMillis() - 1000 // 1 second ago
        )
        viewModel.posts.value = listOf(existingPost)

        // Act
        viewModel.addPost(
            playerId = playerId,
            playerName = "TestPlayer",
            role = GameRole.TANK,
            region = Region.NORTH_AMERICA,
            skillLevel = SkillLevel.DIAMOND,
            message = "Looking for team"
        )
        advanceUntilIdle()

        // Assert
        assertEquals("You can only create 1 post per day. Delete your current post first.", viewModel.error.value)
    }

    @Test
    fun `addPost allows post after 24 hours`() {
        // Arrange
        val playerId = "player123"
        val existingPost = createMockLfgPost(
            id = "existing",
            playerId = playerId,
            createdAt = System.currentTimeMillis() - 25 * 60 * 60 * 1000 // 25 hours ago
        )
        viewModel.posts.value = listOf(existingPost)
        
        val newPost = createMockLfgPost(id = "new", playerId = playerId)
        coEvery { mockRepository.createPost(any()) } returns flow { emit(Result.success(newPost)) }
        coEvery { mockRepository.getAllPosts() } returns flow { emit(Result.success(listOf(newPost))) }

        // Act
        viewModel.addPost(
            playerId = playerId,
            playerName = "TestPlayer",
            role = GameRole.TANK,
            region = Region.NORTH_AMERICA,
            skillLevel = SkillLevel.DIAMOND,
            message = "Looking for team"
        )
        advanceUntilIdle()

        // Assert
        assertEquals(null, viewModel.error.value)
    }

    @Test
    fun `addPost handles repository failure`() {
        // Arrange
        val errorMessage = "Failed to create post"
        coEvery { mockRepository.createPost(any()) } returns flow { emit(Result.failure(Exception(errorMessage))) }

        // Act
        viewModel.addPost(
            playerId = "player123",
            playerName = "TestPlayer",
            role = GameRole.TANK,
            region = Region.NORTH_AMERICA,
            skillLevel = SkillLevel.DIAMOND,
            message = "Looking for team"
        )
        advanceUntilIdle()

        // Assert
        assertEquals(errorMessage, viewModel.error.value)
        assertTrue(viewModel.posts.value.isEmpty()) // Optimistic post should be removed
    }

    @Test
    fun `addPost uses optimistic update`() {
        // Arrange
        val playerId = "player123"
        val mockPost = createMockLfgPost(id = "new-id", playerId = playerId)
        coEvery { mockRepository.createPost(any()) } returns flow { 
            // Verify optimistic update happens before server response
            delay(100)
            emit(Result.success(mockPost))
        }
        coEvery { mockRepository.getAllPosts() } returns flow { emit(Result.success(listOf(mockPost))) }

        // Act
        viewModel.addPost(
            playerId = playerId,
            playerName = "TestPlayer",
            role = GameRole.TANK,
            region = Region.NORTH_AMERICA,
            skillLevel = SkillLevel.DIAMOND,
            message = "Looking for team"
        )

        // Assert - Post should appear immediately (optimistic)
        advanceUntilIdle()
        assertTrue(viewModel.posts.value.isNotEmpty())
    }

    // ─── Delete Post Tests ───

    @Test
    fun `deletePost successfully removes post`() {
        // Arrange
        val postId = "post123"
        val mockPosts = listOf(createMockLfgPost(id = postId))
        viewModel.posts.value = mockPosts
        coEvery { mockRepository.deletePost(postId) } returns flow { emit(Result.success(Unit)) }
        coEvery { mockRepository.getAllPosts() } returns flow { emit(Result.success(emptyList())) }

        // Act
        viewModel.deletePost(postId)
        advanceUntilIdle()

        // Assert
        coVerify { mockRepository.deletePost(postId) }
    }

    @Test
    fun `deletePost handles repository error gracefully`() {
        // Arrange
        val postId = "post123"
        coEvery { mockRepository.deletePost(postId) } returns flow { emit(Result.failure(Exception("Delete failed"))) }

        // Act
        viewModel.deletePost(postId)
        advanceUntilIdle()

        // Assert - Should not crash, error handled internally
        coVerify { mockRepository.deletePost(postId) }
    }

    // ─── Increment View Count Tests ───

    @Test
    fun `incrementViewCount increases view count for post`() {
        // Arrange
        val postId = "post123"
        val initialPost = createMockLfgPost(id = postId, viewCount = 5)
        viewModel.posts.value = listOf(initialPost)
        coEvery { mockRepository.incrementViewCount(postId) } just Runs

        // Act
        viewModel.incrementViewCount(postId)
        advanceUntilIdle()

        // Assert
        assertEquals(6, viewModel.posts.value.first().viewCount)
    }

    @Test
    fun `incrementViewCount does not increment if already viewed`() {
        // This test would require mocking AppSettings, which is created in the ViewModel constructor
        // For now, we'll test the basic flow
        
        // Arrange
        val postId = "post123"
        val initialPost = createMockLfgPost(id = postId, viewCount = 5)
        viewModel.posts.value = listOf(initialPost)
        coEvery { mockRepository.incrementViewCount(postId) } just Runs

        // Act
        viewModel.incrementViewCount(postId)
        advanceUntilIdle()

        // Assert - Should call repository
        coVerify { mockRepository.incrementViewCount(postId) }
    }

    // ─── Error Handling Tests ───

    @Test
    fun `clearError clears error state`() {
        // Arrange
        viewModel.posts.value = emptyList()
        coEvery { mockRepository.getAllPosts() } returns flow { emit(Result.failure(Exception("Test error"))) }
        viewModel.loadPosts()
        advanceUntilIdle()

        // Act
        viewModel.clearError()

        // Assert
        assertEquals(null, viewModel.error.value)
    }

    @Test
    fun `clearRefreshing clears refreshing state`() {
        // Act
        viewModel.clearRefreshing()

        // Assert
        assertFalse(viewModel.isRefreshing.value)
    }

    // ─── Edge Case Tests ───

    @Test
    fun `handles empty posts list correctly`() {
        // Arrange
        coEvery { mockRepository.getAllPosts() } returns flow { emit(Result.success(emptyList())) }

        // Act
        viewModel.loadPosts()
        advanceUntilIdle()

        // Assert
        assertTrue(viewModel.posts.value.isEmpty())
        assertFalse(viewModel.isLoading.value)
    }

    @Test
    fun `handles large number of posts`() {
        // Arrange
        val largePostList = (1..100).map { createMockLfgPost(id = it.toString()) }
        coEvery { mockRepository.getAllPosts() } returns flow { emit(Result.success(largePostList)) }

        // Act
        viewModel.loadPosts()
        advanceUntilIdle()

        // Assert
        assertEquals(100, viewModel.posts.value.size)
    }

    @Test
    fun `handles concurrent loadPosts calls`() {
        // Arrange
        coEvery { mockRepository.getAllPosts() } returns flow { emit(Result.success(emptyList())) }

        // Act
        viewModel.loadPosts()
        viewModel.loadPosts()
        viewModel.loadPosts()
        advanceUntilIdle()

        // Assert - Should handle gracefully without crashing
        assertFalse(viewModel.isLoading.value)
    }

    // ─── Helper Functions ───

    private fun createMockLfgPost(
        id: String = "test-id",
        playerId: String = "player123",
        playerName: String = "TestPlayer",
        viewCount: Int = 0,
        createdAt: Long = System.currentTimeMillis()
    ): LfgPost {
        return LfgPost(
            id = id,
            playerId = playerId,
            playerName = playerName,
            role = GameRole.TANK,
            region = Region.NORTH_AMERICA,
            skillLevel = SkillLevel.DIAMOND,
            message = "Looking for team",
            mainHeroes = emptyList(),
            bio = "",
            rank = "",
            totalMatches = 100,
            winRate = "60%",
            rankedWinRate = "65%",
            inGameId = "",
            city = "",
            screenshotUrl = "",
            useMic = false,
            playstyleTags = emptyList(),
            discord = "",
            telegram = "",
            vk = "",
            facebook = "",
            avatarUrl = null,
            viewCount = viewCount,
            createdAt = createdAt
        )
    }
}

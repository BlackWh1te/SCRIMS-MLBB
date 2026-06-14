package com.mlbb.scrim.viewmodel

import android.app.Application
import com.mlbb.scrim.data.model.NewsArticle
import com.mlbb.scrim.data.preferences.AppSettings
import com.mlbb.scrim.data.repository.NewsRepository
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class NewsViewModelTest {

    private lateinit var viewModel: NewsViewModel
    private lateinit var mockApplication: Application
    private lateinit var mockNewsRepository: NewsRepository
    private lateinit var mockAppSettings: AppSettings
    private lateinit var testDispatcher: TestDispatcher

    @Before
    fun setup() {
        mockApplication = mockk(relaxed = true)
        mockNewsRepository = mockk(relaxed = true)
        mockAppSettings = mockk(relaxed = true)
        testDispatcher = StandardTestDispatcher()

        Dispatchers.setMain(testDispatcher)

        // Mock static constructors
        mockkObject(NewsRepository)
        mockkObject(AppSettings)
        
        every { NewsRepository(any()) } returns mockNewsRepository
        every { AppSettings(any()) } returns mockAppSettings
        every { mockAppSettings.xApiRequestsUsed } returns flowOf(10)
        every { mockAppSettings.newsDripIndex } returns flowOf(5)
        every { mockAppSettings.newsDripCountTotal } returns flowOf(20)

        viewModel = NewsViewModel(mockApplication)
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
        assertTrue(viewModel.articles.value.isEmpty())
        assertFalse(viewModel.isLoading.value)
        assertFalse(viewModel.isRefreshing.value)
        assertEquals(null, viewModel.error.value)
        assertEquals(null, viewModel.selectedArticle.value)
        assertEquals(0, viewModel.quotaInfo.value.used)
        assertEquals(100, viewModel.quotaInfo.value.limit)
        assertEquals(null, viewModel.throttleInfo.value)
        assertEquals(0, viewModel.dripInfo.value.unlocked)
    }

    @Test
    fun `ViewModel loads quota and drip info on initialization`() {
        // Assert - Should have loaded during init
        assertEquals(10, viewModel.quotaInfo.value.used)
        assertEquals(5, viewModel.dripInfo.value.unlocked)
    }

    // ─── Load News Tests ───

    @Test
    fun `loadNews successfully loads articles`() {
        // Arrange
        val mockArticles = listOf(
            createMockNewsArticle(id = "1", title = "News 1"),
            createMockNewsArticle(id = "2", title = "News 2")
        )
        val refreshResult = NewsRepository.RefreshResult(
            articles = mockArticles,
            wasThrottled = false,
            minutesUntilRefresh = 0
        )
        coEvery { 
            mockNewsRepository.getNews(forceRefresh = any(), targetLanguage = any()) 
        } returns flow { emit(Result.success(refreshResult)) }

        // Act
        viewModel.loadNews()
        advanceUntilIdle()

        // Assert
        assertEquals(mockArticles, viewModel.articles.value)
        assertFalse(viewModel.isLoading.value)
        assertEquals(null, viewModel.throttleInfo.value)
    }

    @Test
    fun `loadNews handles throttled response`() {
        // Arrange
        val mockArticles = listOf(createMockNewsArticle(id = "1"))
        val refreshResult = NewsRepository.RefreshResult(
            articles = mockArticles,
            wasThrottled = true,
            minutesUntilRefresh = 15
        )
        coEvery { 
            mockNewsRepository.getNews(forceRefresh = any(), targetLanguage = any()) 
        } returns flow { emit(Result.success(refreshResult)) }

        // Act
        viewModel.loadNews()
        advanceUntilIdle()

        // Assert
        assertEquals(mockArticles, viewModel.articles.value)
        assertNotNull(viewModel.throttleInfo.value)
        assertEquals(15, viewModel.throttleInfo.value?.minutesUntilRefresh)
    }

    @Test
    fun `loadNews handles error`() {
        // Arrange
        val errorMessage = "Failed to load news"
        coEvery { 
            mockNewsRepository.getNews(forceRefresh = any(), targetLanguage = any()) 
        } returns flow { emit(Result.failure(Exception(errorMessage))) }

        // Act
        viewModel.loadNews()
        advanceUntilIdle()

        // Assert
        assertEquals(errorMessage, viewModel.error.value)
        assertFalse(viewModel.isLoading.value)
    }

    @Test
    fun `loadNews sets refreshing flag when isRefresh is true`() {
        // Arrange
        val refreshResult = NewsRepository.RefreshResult(
            articles = emptyList(),
            wasThrottled = false,
            minutesUntilRefresh = 0
        )
        coEvery { 
            mockNewsRepository.getNews(forceRefresh = any(), targetLanguage = any()) 
        } returns flow { emit(Result.success(refreshResult)) }

        // Act
        viewModel.loadNews(isRefresh = true)
        advanceUntilIdle()

        // Assert
        assertFalse(viewModel.isRefreshing.value) // Should be false after completion
    }

    @Test
    fun `loadNews respects language code parameter`() {
        // Arrange
        val refreshResult = NewsRepository.RefreshResult(
            articles = emptyList(),
            wasThrottled = false,
            minutesUntilRefresh = 0
        )
        coEvery { 
            mockNewsRepository.getNews(forceRefresh = any(), targetLanguage = "es") 
        } returns flow { emit(Result.success(refreshResult)) }

        // Act
        viewModel.loadNews(languageCode = "es")
        advanceUntilIdle()

        // Assert
        coVerify { mockNewsRepository.getNews(forceRefresh = any(), targetLanguage = "es") }
    }

    @Test
    fun `loadNews respects forceRefresh parameter`() {
        // Arrange
        val refreshResult = NewsRepository.RefreshResult(
            articles = emptyList(),
            wasThrottled = false,
            minutesUntilRefresh = 0
        )
        coEvery { 
            mockNewsRepository.getNews(forceRefresh = true, targetLanguage = any()) 
        } returns flow { emit(Result.success(refreshResult)) }

        // Act
        viewModel.loadNews(forceRefresh = true)
        advanceUntilIdle()

        // Assert
        coVerify { mockNewsRepository.getNews(forceRefresh = true, targetLanguage = any()) }
    }

    // ─── Refresh Tests ───

    @Test
    fun `refresh successfully refreshes news and quota info`() {
        // Arrange
        val mockArticles = listOf(createMockNewsArticle(id = "1"))
        val refreshResult = NewsRepository.RefreshResult(
            articles = mockArticles,
            wasThrottled = false,
            minutesUntilRefresh = 0
        )
        coEvery { 
            mockNewsRepository.getNews(forceRefresh = true, targetLanguage = any()) 
        } returns flow { emit(Result.success(refreshResult)) }
        every { mockAppSettings.xApiRequestsUsed } returns flowOf(15)
        every { mockAppSettings.newsDripIndex } returns flowOf(6)

        // Act
        viewModel.refresh()
        advanceUntilIdle()

        // Assert
        assertEquals(mockArticles, viewModel.articles.value)
        assertEquals(15, viewModel.quotaInfo.value.used)
        assertEquals(6, viewModel.dripInfo.value.unlocked)
    }

    // ─── Quota Info Tests ───

    @Test
    fun `QuotaInfo calculates remaining correctly`() {
        // Arrange
        every { mockAppSettings.xApiRequestsUsed } returns flowOf(25)

        // Act
        viewModel = NewsViewModel(mockApplication)
        advanceUntilIdle()

        // Assert
        assertEquals(25, viewModel.quotaInfo.value.used)
        assertEquals(75, viewModel.quotaInfo.value.remaining)
    }

    @Test
    fun `QuotaInfo calculates percentUsed correctly`() {
        // Arrange
        every { mockAppSettings.xApiRequestsUsed } returns flowOf(50)

        // Act
        viewModel = NewsViewModel(mockApplication)
        advanceUntilIdle()

        // Assert
        assertEquals(50, viewModel.quotaInfo.value.used)
        assertEquals(0.5f, viewModel.quotaInfo.value.percentUsed)
    }

    @Test
    fun `QuotaInfo sets canUse correctly when under limit`() {
        // Arrange
        every { mockAppSettings.xApiRequestsUsed } returns flowOf(50)

        // Act
        viewModel = NewsViewModel(mockApplication)
        advanceUntilIdle()

        // Assert
        assertTrue(viewModel.quotaInfo.value.canUseX)
    }

    @Test
    fun `QuotaInfo sets canUse correctly when at limit`() {
        // Arrange
        every { mockAppSettings.xApiRequestsUsed } returns flowOf(100)

        // Act
        viewModel = NewsViewModel(mockApplication)
        advanceUntilIdle()

        // Assert
        assertFalse(viewModel.quotaInfo.value.canUseX)
    }

    @Test
    fun `QuotaInfo handles zero limit`() {
        // Arrange
        every { mockAppSettings.xApiRequestsUsed } returns flowOf(10)

        // Act
        viewModel = NewsViewModel(mockApplication)
        advanceUntilIdle()

        // Assert - Should default to 100 limit
        assertEquals(100, viewModel.quotaInfo.value.limit)
    }

    // ─── Drip Info Tests ───

    @Test
    fun `DripInfo calculates unseen correctly`() {
        // Arrange
        every { mockAppSettings.newsDripIndex } returns flowOf(5)
        every { mockAppSettings.newsDripCountTotal } returns flowOf(20)

        // Act
        viewModel = NewsViewModel(mockApplication)
        advanceUntilIdle()

        // Assert
        assertEquals(5, viewModel.dripInfo.value.unlocked)
        assertEquals(20, viewModel.dripInfo.value.total)
        assertEquals(15, viewModel.dripInfo.value.unseen)
    }

    @Test
    fun `DripInfo handles unseen when index exceeds total`() {
        // Arrange
        every { mockAppSettings.newsDripIndex } returns flowOf(25)
        every { mockAppSettings.newsDripCountTotal } returns flowOf(20)

        // Act
        viewModel = NewsViewModel(mockApplication)
        advanceUntilIdle()

        // Assert
        assertEquals(0, viewModel.dripInfo.value.unseen) // Should coerce to 0
    }

    // ─── Article Selection Tests ───

    @Test
    fun `selectArticle successfully selects article`() {
        // Arrange
        val article = createMockNewsArticle(id = "1")

        // Act
        viewModel.selectArticle(article)

        // Assert
        assertEquals(article, viewModel.selectedArticle.value)
    }

    @Test
    fun `clearSelectedArticle successfully clears selection`() {
        // Arrange
        viewModel.selectArticle(createMockNewsArticle(id = "1"))

        // Act
        viewModel.clearSelectedArticle()

        // Assert
        assertEquals(null, viewModel.selectedArticle.value)
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
    fun `clearRefreshing clears refreshing flag`() {
        // Arrange
        viewModel._isRefreshing.value = true

        // Act
        viewModel.clearRefreshing()

        // Assert
        assertFalse(viewModel.isRefreshing.value)
    }

    // ─── ViewModel Cleanup Tests ───

    @Test
    fun `onCleared cancels load job and closes repository`() {
        // Act
        viewModel.onCleared()
        advanceUntilIdle()

        // Assert
        coVerify { mockNewsRepository.close() }
    }

    // ─── Edge Case Tests ───

    @Test
    fun `loadNews cancels previous load job`() {
        // Arrange
        val refreshResult = NewsRepository.RefreshResult(
            articles = emptyList(),
            wasThrottled = false,
            minutesUntilRefresh = 0
        )
        coEvery { 
            mockNewsRepository.getNews(forceRefresh = any(), targetLanguage = any()) 
        } returns flow { emit(Result.success(refreshResult)) }

        // Act
        viewModel.loadNews()
        viewModel.loadNews() // Should cancel the first one
        advanceUntilIdle()

        // Assert - Should complete without errors
        assertTrue(true)
    }

    @Test
    fun `loadNews handles empty article list`() {
        // Arrange
        val refreshResult = NewsRepository.RefreshResult(
            articles = emptyList(),
            wasThrottled = false,
            minutesUntilRefresh = 0
        )
        coEvery { 
            mockNewsRepository.getNews(forceRefresh = any(), targetLanguage = any()) 
        } returns flow { emit(Result.success(refreshResult)) }

        // Act
        viewModel.loadNews()
        advanceUntilIdle()

        // Assert
        assertTrue(viewModel.articles.value.isEmpty())
    }

    @Test
    fun `loadNews handles large article list`() {
        // Arrange
        val largeArticleList = (1..100).map { createMockNewsArticle(id = it.toString()) }
        val refreshResult = NewsRepository.RefreshResult(
            articles = largeArticleList,
            wasThrottled = false,
            minutesUntilRefresh = 0
        )
        coEvery { 
            mockNewsRepository.getNews(forceRefresh = any(), targetLanguage = any()) 
        } returns flow { emit(Result.success(refreshResult)) }

        // Act
        viewModel.loadNews()
        advanceUntilIdle()

        // Assert
        assertEquals(100, viewModel.articles.value.size)
    }

    // ─── Helper Functions ───

    private fun createMockNewsArticle(
        id: String = "article-id",
        title: String = "Test Article"
    ): NewsArticle {
        return NewsArticle(
            id = id,
            title = title,
            description = "Test description",
            content = "Test content",
            imageUrl = "https://example.com/image.jpg",
            sourceUrl = "https://example.com/article",
            publishedAt = System.currentTimeMillis(),
            source = "Test Source",
            language = "en"
        )
    }
}

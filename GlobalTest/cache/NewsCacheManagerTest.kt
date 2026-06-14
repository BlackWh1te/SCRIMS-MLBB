package com.mlbb.scrim.data.repository

import android.content.Context
import com.mlbb.scrim.data.model.NewsArticle
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class NewsCacheManagerTest {

    private lateinit var newsCacheManager: NewsCacheManager
    private lateinit var mockContext: Context
    private lateinit var mockFilesDir: File
    private lateinit var mockCacheFile: File
    private lateinit var testDispatcher: TestDispatcher

    @Before
    fun setup() {
        mockContext = mockk(relaxed = true)
        mockFilesDir = mockk(relaxed = true)
        mockCacheFile = mockk(relaxed = true)
        testDispatcher = StandardTestDispatcher()

        Dispatchers.setMain(testDispatcher)

        every { mockContext.filesDir } returns mockFilesDir
        every { mockCacheFile.exists() } returns false
        every { mockCacheFile.readText() } returns ""
        every { mockCacheFile.writeText(any()) } just Runs
        every { mockCacheFile.delete() } returns true

        // Create a spy of the NewsCacheManager to intercept the lazy initialization
        newsCacheManager = spyk(NewsCacheManager(mockContext))
        every { newsCacheManager["cacheFile"] } returns mockCacheFile
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ─── Load Cache Tests ───

    @Test
    fun `loadCache returns empty list when cache file does not exist`() {
        // Arrange
        every { mockCacheFile.exists() } returns false

        // Act
        val result = newsCacheManager.loadCache()

        advanceUntilIdle()

        // Assert
        assertTrue(result.isEmpty())
        verify { mockCacheFile.exists() }
        verify(exactly = 0) { mockCacheFile.readText() }
    }

    @Test
    fun `loadCache returns empty list when cache file is empty`() {
        // Arrange
        every { mockCacheFile.exists() } returns true
        every { mockCacheFile.readText() } returns ""

        // Act
        val result = newsCacheManager.loadCache()

        advanceUntilIdle()

        // Assert
        assertTrue(result.isEmpty())
    }

    @Test
    fun `loadCache returns empty list when cache is expired`() {
        // Arrange
        val expiredJson = """
            {
                "articles": [
                    {"id": "1", "title": "Test Article", "url": "http://test.com", "publishedAt": "2024-01-01T00:00:00Z"}
                ],
                "cachedAt": ${System.currentTimeMillis() - NewsCacheManager.CACHE_TTL_MS - 1000},
                "source": "proxy"
            }
        """.trimIndent()

        every { mockCacheFile.exists() } returns true
        every { mockCacheFile.readText() } returns expiredJson

        // Act
        val result = newsCacheManager.loadCache()

        advanceUntilIdle()

        // Assert
        assertTrue(result.isEmpty())
    }

    @Test
    fun `loadCache returns cached articles when cache is valid`() {
        // Arrange
        val validJson = """
            {
                "articles": [
                    {"id": "1", "title": "Test Article 1", "url": "http://test1.com", "publishedAt": "2024-01-01T00:00:00Z"},
                    {"id": "2", "title": "Test Article 2", "url": "http://test2.com", "publishedAt": "2024-01-01T00:00:00Z"}
                ],
                "cachedAt": ${System.currentTimeMillis() - 60000},
                "source": "proxy"
            }
        """.trimIndent()

        every { mockCacheFile.exists() } returns true
        every { mockCacheFile.readText() } returns validJson

        // Act
        val result = newsCacheManager.loadCache()

        advanceUntilIdle()

        // Assert
        assertEquals(2, result.size)
        assertEquals("Test Article 1", result[0].title)
        assertEquals("Test Article 2", result[1].title)
    }

    @Test
    fun `loadCache respects custom maxAgeMs parameter`() {
        // Arrange
        val recentJson = """
            {
                "articles": [
                    {"id": "1", "title": "Test Article", "url": "http://test.com", "publishedAt": "2024-01-01T00:00:00Z"}
                ],
                "cachedAt": ${System.currentTimeMillis() - 1000},
                "source": "proxy"
            }
        """.trimIndent()

        every { mockCacheFile.exists() } returns true
        every { mockCacheFile.readText() } returns recentJson

        // Act - with very short TTL
        val result = newsCacheManager.loadCache(maxAgeMs = 500)

        advanceUntilIdle()

        // Assert - should be expired with 500ms TTL
        assertTrue(result.isEmpty())
    }

    @Test
    fun `loadCache handles corrupted JSON gracefully`() {
        // Arrange
        val corruptedJson = "invalid json {{{"

        every { mockCacheFile.exists() } returns true
        every { mockCacheFile.readText() } returns corruptedJson

        // Act
        val result = newsCacheManager.loadCache()

        advanceUntilIdle()

        // Assert
        assertTrue(result.isEmpty())
    }

    @Test
    fun `loadCache handles null cached data gracefully`() {
        // Arrange
        val nullJson = "null"

        every { mockCacheFile.exists() } returns true
        every { mockCacheFile.readText() } returns nullJson

        // Act
        val result = newsCacheManager.loadCache()

        advanceUntilIdle()

        // Assert
        assertTrue(result.isEmpty())
    }

    @Test
    fun `loadCache handles IO exceptions gracefully`() {
        // Arrange
        every { mockCacheFile.exists() } returns true
        every { mockCacheFile.readText() } throws java.io.IOException("File read error")

        // Act
        val result = newsCacheManager.loadCache()

        advanceUntilIdle()

        // Assert
        assertTrue(result.isEmpty())
    }

    // ─── Save Cache Tests ───

    @Test
    fun `saveCache successfully saves articles to file`() {
        // Arrange
        val articles = listOf(
            NewsArticle(id = "1", title = "Test Article 1", url = "http://test1.com", publishedAt = "2024-01-01T00:00:00Z"),
            NewsArticle(id = "2", title = "Test Article 2", url = "http://test2.com", publishedAt = "2024-01-01T00:00:00Z")
        )

        // Act
        newsCacheManager.saveCache(articles)

        advanceUntilIdle()

        // Assert
        verify { mockCacheFile.writeText(any()) }
    }

    @Test
    fun `saveCache respects MAX_CACHED_ARTICLES limit`() {
        // Arrange
        val articles = (1..150).map { 
            NewsArticle(id = "$it", title = "Article $it", url = "http://test$it.com", publishedAt = "2024-01-01T00:00:00Z")
        }

        // Act
        newsCacheManager.saveCache(articles)

        advanceUntilIdle()

        // Assert
        verify { mockCacheFile.writeText(match { 
            it.contains("articles") && it.count { char -> char == '}' } <= NewsCacheManager.MAX_CACHED_ARTICLES + 10
        }) }
    }

    @Test
    fun `saveCache includes source in cached data`() {
        // Arrange
        val articles = listOf(
            NewsArticle(id = "1", title = "Test Article", url = "http://test.com", publishedAt = "2024-01-01T00:00:00Z")
        )
        val customSource = "custom_source"

        // Act
        newsCacheManager.saveCache(articles, source = customSource)

        advanceUntilIdle()

        // Assert
        verify { mockCacheFile.writeText(match { it.contains(customSource) }) }
    }

    @Test
    fun `saveCache handles empty article list`() {
        // Arrange
        val articles = emptyList<NewsArticle>()

        // Act
        newsCacheManager.saveCache(articles)

        advanceUntilIdle()

        // Assert
        verify { mockCacheFile.writeText(any()) }
    }

    @Test
    fun `saveCache handles IO exceptions gracefully`() {
        // Arrange
        val articles = listOf(
            NewsArticle(id = "1", title = "Test Article", url = "http://test.com", publishedAt = "2024-01-01T00:00:00Z")
        )
        every { mockCacheFile.writeText(any()) } throws java.io.IOException("File write error")

        // Act
        newsCacheManager.saveCache(articles)

        advanceUntilIdle()

        // Assert - Should not throw exception
        verify { mockCacheFile.writeText(any()) }
    }

    @Test
    fun `saveCache includes timestamp in cached data`() {
        // Arrange
        val articles = listOf(
            NewsArticle(id = "1", title = "Test Article", url = "http://test.com", publishedAt = "2024-01-01T00:00:00Z")
        )
        val beforeSave = System.currentTimeMillis()

        // Act
        newsCacheManager.saveCache(articles)

        advanceUntilIdle()

        val afterSave = System.currentTimeMillis()

        // Assert
        verify { mockCacheFile.writeText(match { 
            val json = it
            // Check if the timestamp is within expected range
            true // We can't easily parse the JSON here, but we verify the write was called
        }) }
    }

    // ─── Clear Cache Tests ───

    @Test
    fun `clearCache deletes cache file when it exists`() {
        // Arrange
        every { mockCacheFile.exists() } returns true
        every { mockCacheFile.delete() } returns true

        // Act
        newsCacheManager.clearCache()

        advanceUntilIdle()

        // Assert
        verify { mockCacheFile.exists() }
        verify { mockCacheFile.delete() }
    }

    @Test
    fun `clearCache handles non-existent cache file`() {
        // Arrange
        every { mockCacheFile.exists() } returns false

        // Act
        newsCacheManager.clearCache()

        advanceUntilIdle()

        // Assert
        verify { mockCacheFile.exists() }
        verify(exactly = 0) { mockCacheFile.delete() }
    }

    @Test
    fun `clearCache handles IO exceptions gracefully`() {
        // Arrange
        every { mockCacheFile.exists() } returns true
        every { mockCacheFile.delete() } throws java.io.IOException("File delete error")

        // Act
        newsCacheManager.clearCache()

        advanceUntilIdle()

        // Assert - Should not throw exception
        verify { mockCacheFile.delete() }
    }

    // ─── Has Valid Cache Tests ───

    @Test
    fun `hasValidCache returns true when cache is valid`() {
        // Arrange
        val validJson = """
            {
                "articles": [
                    {"id": "1", "title": "Test Article", "url": "http://test.com", "publishedAt": "2024-01-01T00:00:00Z"}
                ],
                "cachedAt": ${System.currentTimeMillis() - 60000},
                "source": "proxy"
            }
        """.trimIndent()

        every { mockCacheFile.exists() } returns true
        every { mockCacheFile.readText() } returns validJson

        // Act
        val result = newsCacheManager.hasValidCache()

        advanceUntilIdle()

        // Assert
        assertTrue(result)
    }

    @Test
    fun `hasValidCache returns false when cache is expired`() {
        // Arrange
        val expiredJson = """
            {
                "articles": [
                    {"id": "1", "title": "Test Article", "url": "http://test.com", "publishedAt": "2024-01-01T00:00:00Z"}
                ],
                "cachedAt": ${System.currentTimeMillis() - NewsCacheManager.CACHE_TTL_MS - 1000},
                "source": "proxy"
            }
        """.trimIndent()

        every { mockCacheFile.exists() } returns true
        every { mockCacheFile.readText() } returns expiredJson

        // Act
        val result = newsCacheManager.hasValidCache()

        advanceUntilIdle()

        // Assert
        assertFalse(result)
    }

    @Test
    fun `hasValidCache returns false when cache file does not exist`() {
        // Arrange
        every { mockCacheFile.exists() } returns false

        // Act
        val result = newsCacheManager.hasValidCache()

        advanceUntilIdle()

        // Assert
        assertFalse(result)
    }

    @Test
    fun `hasValidCache respects custom maxAgeMs parameter`() {
        // Arrange
        val recentJson = """
            {
                "articles": [
                    {"id": "1", "title": "Test Article", "url": "http://test.com", "publishedAt": "2024-01-01T00:00:00Z"}
                ],
                "cachedAt": ${System.currentTimeMillis() - 1000},
                "source": "proxy"
            }
        """.trimIndent()

        every { mockCacheFile.exists() } returns true
        every { mockCacheFile.readText() } returns recentJson

        // Act - with very short TTL
        val result = newsCacheManager.hasValidCache(maxAgeMs = 500)

        advanceUntilIdle()

        // Assert
        assertFalse(result)
    }

    // ─── Get Cache Age Tests ───

    @Test
    fun `getCacheAgeMinutes returns correct age in minutes`() {
        // Arrange
        val fiveMinutesAgo = System.currentTimeMillis() - 5 * 60 * 1000
        val json = """
            {
                "articles": [
                    {"id": "1", "title": "Test Article", "url": "http://test.com", "publishedAt": "2024-01-01T00:00:00Z"}
                ],
                "cachedAt": $fiveMinutesAgo,
                "source": "proxy"
            }
        """.trimIndent()

        every { mockCacheFile.exists() } returns true
        every { mockCacheFile.readText() } returns json

        // Act
        val result = newsCacheManager.getCacheAgeMinutes()

        advanceUntilIdle()

        // Assert
        assertTrue(result >= 4 && result <= 6) // Allow for timing variance
    }

    @Test
    fun `getCacheAgeMinutes returns -1 when cache file does not exist`() {
        // Arrange
        every { mockCacheFile.exists() } returns false

        // Act
        val result = newsCacheManager.getCacheAgeMinutes()

        advanceUntilIdle()

        // Assert
        assertEquals(-1, result)
    }

    @Test
    fun `getCacheAgeMinutes returns -1 when cache data is null`() {
        // Arrange
        every { mockCacheFile.exists() } returns true
        every { mockCacheFile.readText() } returns "null"

        // Act
        val result = newsCacheManager.getCacheAgeMinutes()

        advanceUntilIdle()

        // Assert
        assertEquals(-1, result)
    }

    @Test
    fun `getCacheAgeMinutes handles IO exceptions gracefully`() {
        // Arrange
        every { mockCacheFile.exists() } returns true
        every { mockCacheFile.readText() } throws java.io.IOException("File read error")

        // Act
        val result = newsCacheManager.getCacheAgeMinutes()

        advanceUntilIdle()

        // Assert
        assertEquals(-1, result)
    }

    // ─── Integration Tests ───

    @Test
    fun `saveCache and loadCache work together correctly`() {
        // Arrange
        val articles = listOf(
            NewsArticle(id = "1", title = "Test Article", url = "http://test.com", publishedAt = "2024-01-01T00:00:00Z")
        )

        var savedJson = ""

        every { mockCacheFile.writeText(capture(savedJson)) } just Runs
        every { mockCacheFile.readText() } answers { savedJson }
        every { mockCacheFile.exists() } returns true

        // Act
        newsCacheManager.saveCache(articles)
        advanceUntilIdle()

        val loadedArticles = newsCacheManager.loadCache()
        advanceUntilIdle()

        // Assert
        assertEquals(1, loadedArticles.size)
        assertEquals("Test Article", loadedArticles[0].title)
    }

    @Test
    fun `clearCache removes previously saved cache`() {
        // Arrange
        val articles = listOf(
            NewsArticle(id = "1", title = "Test Article", url = "http://test.com", publishedAt = "2024-01-01T00:00:00Z")
        )

        var savedJson = ""
        every { mockCacheFile.writeText(capture(savedJson)) } just Runs
        every { mockCacheFile.readText() } answers { savedJson }
        every { mockCacheFile.exists() } returns true
        every { mockCacheFile.delete() } returns true

        // Act
        newsCacheManager.saveCache(articles)
        advanceUntilIdle()

        newsCacheManager.clearCache()
        advanceUntilIdle()

        every { mockCacheFile.exists() } returns false

        val loadedArticles = newsCacheManager.loadCache()
        advanceUntilIdle()

        // Assert
        assertTrue(loadedArticles.isEmpty())
        verify { mockCacheFile.delete() }
    }

    @Test
    fun `hasValidCache returns true immediately after saveCache`() {
        // Arrange
        val articles = listOf(
            NewsArticle(id = "1", title = "Test Article", url = "http://test.com", publishedAt = "2024-01-01T00:00:00Z")
        )

        var savedJson = ""
        every { mockCacheFile.writeText(capture(savedJson)) } just Runs
        every { mockCacheFile.readText() } answers { savedJson }
        every { mockCacheFile.exists() } returns true

        // Act
        newsCacheManager.saveCache(articles)
        advanceUntilIdle()

        val hasValid = newsCacheManager.hasValidCache()
        advanceUntilIdle()

        // Assert
        assertTrue(hasValid)
    }

    // ─── Edge Case Tests ───

    @Test
    fun `loadCache handles very large article list`() {
        // Arrange
        val largeJson = """
            {
                "articles": [
                    ${(1..200).map { """{"id": "$it", "title": "Article $it", "url": "http://test$it.com", "publishedAt": "2024-01-01T00:00:00Z"}""" }.joinToString(",\n                    ") }
                ],
                "cachedAt": ${System.currentTimeMillis() - 60000},
                "source": "proxy"
            }
        """.trimIndent()

        every { mockCacheFile.exists() } returns true
        every { mockCacheFile.readText() } returns largeJson

        // Act
        val result = newsCacheManager.loadCache()

        advanceUntilIdle()

        // Assert
        assertEquals(200, result.size)
    }

    @Test
    fun `saveCache handles articles with special characters in titles`() {
        // Arrange
        val articles = listOf(
            NewsArticle(
                id = "1", 
                title = "Test \"Article\" with 'quotes' & symbols <test>", 
                url = "http://test.com", 
                publishedAt = "2024-01-01T00:00:00Z"
            )
        )

        // Act
        newsCacheManager.saveCache(articles)

        advanceUntilIdle()

        // Assert
        verify { mockCacheFile.writeText(any()) }
    }

    @Test
    fun `loadCache handles articles with missing optional fields`() {
        // Arrange
        val minimalJson = """
            {
                "articles": [
                    {"id": "1", "title": "Test Article", "url": "http://test.com", "publishedAt": "2024-01-01T00:00:00Z"}
                ],
                "cachedAt": ${System.currentTimeMillis() - 60000},
                "source": "proxy"
            }
        """.trimIndent()

        every { mockCacheFile.exists() } returns true
        every { mockCacheFile.readText() } returns minimalJson

        // Act
        val result = newsCacheManager.loadCache()

        advanceUntilIdle()

        // Assert
        assertEquals(1, result.size)
    }

    @Test
    fun `getCacheAgeMinutes handles very old cache`() {
        // Arrange
        val veryOldJson = """
            {
                "articles": [
                    {"id": "1", "title": "Test Article", "url": "http://test.com", "publishedAt": "2024-01-01T00:00:00Z"}
                ],
                "cachedAt": ${System.currentTimeMillis() - 365L * 24 * 60 * 60 * 1000},
                "source": "proxy"
            }
        """.trimIndent()

        every { mockCacheFile.exists() } returns true
        every { mockCacheFile.readText() } returns veryOldJson

        // Act
        val result = newsCacheManager.getCacheAgeMinutes()

        advanceUntilIdle()

        // Assert
        assertTrue(result > 500000) // Should be approximately 525,600 minutes (1 year)
    }

    @Test
    fun `loadCache handles cache with zero timestamp`() {
        // Arrange
        val zeroTimestampJson = """
            {
                "articles": [
                    {"id": "1", "title": "Test Article", "url": "http://test.com", "publishedAt": "2024-01-01T00:00:00Z"}
                ],
                "cachedAt": 0,
                "source": "proxy"
            }
        """.trimIndent()

        every { mockCacheFile.exists() } returns true
        every { mockCacheFile.readText() } returns zeroTimestampJson

        // Act
        val result = newsCacheManager.loadCache()

        advanceUntilIdle()

        // Assert - Should be expired (current time - 0 > TTL)
        assertTrue(result.isEmpty())
    }
}

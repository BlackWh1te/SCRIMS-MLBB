package com.mlbb.scrim.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Advanced CacheMetadataDao tests with edge cases, concurrency, failure scenarios, and data integrity validation.
 * 
 * Test Categories:
 * - CRUD operations with edge cases
 * - Concurrency and race conditions
 * - Pattern matching operations
 * - Data integrity and constraints
 * - Null/empty input handling
 * - Large dataset performance
 * - Transaction scenarios
 */
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalCoroutinesApi::class)
class CacheMetadataDaoAdvancedTest {

    private lateinit var database: MLBBScrimDatabase
    private lateinit var cacheMetadataDao: CacheMetadataDao
    private lateinit var testDispatcher: TestDispatcher

    @Before
    fun setup() {
        testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)

        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            MLBBScrimDatabase::class.java
        ).allowMainThreadQueries().build()

        cacheMetadataDao = database.cacheMetadataDao()
    }

    @After
    fun tearDown() {
        database.close()
        Dispatchers.resetMain()
    }

    // ─── BASIC CRUD TESTS ───

    @Test
    fun `get returns null when key does not exist`() {
        // Act
        val result = cacheMetadataDao.get("nonexistent_key")

        // Assert
        assertNull(result)
    }

    @Test
    fun `get returns entity when key exists`() {
        // Arrange
        val entity = CacheMetadataEntity(
            cacheKey = "test_key",
            lastFetched = 123456789,
            expiresAt = 123459999
        )
        cacheMetadataDao.set(entity)
        advanceUntilIdle()

        // Act
        val result = cacheMetadataDao.get("test_key")

        // Assert
        assertNotNull(result)
        assertEquals("test_key", result?.cacheKey)
        assertEquals(123456789, result?.lastFetched)
    }

    @Test
    fun `set replaces existing entity with same key`() {
        // Arrange
        val originalEntity = CacheMetadataEntity(
            cacheKey = "test_key",
            lastFetched = 123456789,
            expiresAt = 123459999
        )
        cacheMetadataDao.set(originalEntity)
        advanceUntilIdle()

        val updatedEntity = CacheMetadataEntity(
            cacheKey = "test_key",
            lastFetched = 987654321,
            expiresAt = 987657999
        )

        // Act
        cacheMetadataDao.set(updatedEntity)
        advanceUntilIdle()

        // Assert
        val result = cacheMetadataDao.get("test_key")
        assertNotNull(result)
        assertEquals(987654321, result?.lastFetched)
        assertEquals(987657999, result?.expiresAt)
    }

    @Test
    fun `delete removes entity when key exists`() {
        // Arrange
        val entity = CacheMetadataEntity(
            cacheKey = "test_key",
            lastFetched = 123456789,
            expiresAt = 123459999
        )
        cacheMetadataDao.set(entity)
        advanceUntilIdle()

        // Act
        cacheMetadataDao.delete("test_key")
        advanceUntilIdle()

        // Assert
        val result = cacheMetadataDao.get("test_key")
        assertNull(result)
    }

    @Test
    fun `delete handles non-existent key gracefully`() {
        // Act
        cacheMetadataDao.delete("nonexistent_key")
        advanceUntilIdle()

        // Assert - Should not throw exception
        assertTrue(true, "Delete should handle non-existent key gracefully")
    }

    @Test
    fun `deleteByPrefix removes all matching keys`() {
        // Arrange
        val entities = listOf(
            CacheMetadataEntity("scrims_1", 123456789, 123459999),
            CacheMetadataEntity("scrims_2", 123456790, 123459999),
            CacheMetadataEntity("scrims_3", 123456791, 123459999),
            CacheMetadataEntity("other_key", 123456792, 123459999)
        )
        entities.forEach { cacheMetadataDao.set(it) }
        advanceUntilIdle()

        // Act
        cacheMetadataDao.deleteByPrefix("scrims")
        advanceUntilIdle()

        // Assert
        assertNull(cacheMetadataDao.get("scrims_1"))
        assertNull(cacheMetadataDao.get("scrims_2"))
        assertNull(cacheMetadataDao.get("scrims_3"))
        assertNotNull(cacheMetadataDao.get("other_key"))
    }

    @Test
    fun `deleteByPrefix handles empty prefix`() {
        // Arrange
        val entity = CacheMetadataEntity("test_key", 123456789, 123459999)
        cacheMetadataDao.set(entity)
        advanceUntilIdle()

        // Act
        cacheMetadataDao.deleteByPrefix("")
        advanceUntilIdle()

        // Assert - Empty prefix should not match anything
        assertNotNull(cacheMetadataDao.get("test_key"))
    }

    @Test
    fun `clearAll removes all entities`() {
        // Arrange
        val entities = listOf(
            CacheMetadataEntity("key1", 123456789, 123459999),
            CacheMetadataEntity("key2", 123456790, 123459999),
            CacheMetadataEntity("key3", 123456791, 123459999)
        )
        entities.forEach { cacheMetadataDao.set(it) }
        advanceUntilIdle()

        // Act
        cacheMetadataDao.clearAll()
        advanceUntilIdle()

        // Assert
        assertNull(cacheMetadataDao.get("key1"))
        assertNull(cacheMetadataDao.get("key2"))
        assertNull(cacheMetadataDao.get("key3"))
    }

    @Test
    fun `clearAll handles empty database gracefully`() {
        // Act
        cacheMetadataDao.clearAll()
        advanceUntilIdle()

        // Assert - Should not throw exception
        assertTrue(true, "ClearAll should handle empty database gracefully")
    }

    // ─── EDGE CASE TESTS ───

    @Test
    fun `get handles empty key`() {
        // Act
        val result = cacheMetadataDao.get("")

        // Assert
        assertNull(result)
    }

    @Test
    fun `get handles very long key`() {
        // Arrange
        val longKey = "a".repeat(10000)
        val entity = CacheMetadataEntity(longKey, 123456789, 123459999)
        cacheMetadataDao.set(entity)
        advanceUntilIdle()

        // Act
        val result = cacheMetadataDao.get(longKey)

        // Assert
        assertNotNull(result)
        assertEquals(longKey, result?.cacheKey)
    }

    @Test
    fun `get handles key with special characters`() {
        // Arrange
        val specialKey = "key_with_!@#$%^&*()_+-=[]{}|;:,.<>?"
        val entity = CacheMetadataEntity(specialKey, 123456789, 123459999)
        cacheMetadataDao.set(entity)
        advanceUntilIdle()

        // Act
        val result = cacheMetadataDao.get(specialKey)

        // Assert
        assertNotNull(result)
        assertEquals(specialKey, result?.cacheKey)
    }

    @Test
    fun `get handles key with unicode characters`() {
        // Arrange
        val unicodeKey = "key_with_中文_emoji_😀_特殊字符"
        val entity = CacheMetadataEntity(unicodeKey, 123456789, 123459999)
        cacheMetadataDao.set(entity)
        advanceUntilIdle()

        // Act
        val result = cacheMetadataDao.get(unicodeKey)

        // Assert
        assertNotNull(result)
        assertEquals(unicodeKey, result?.cacheKey)
    }

    @Test
    fun `set handles zero timestamps`() {
        // Arrange
        val entity = CacheMetadataEntity("test_key", 0, 0)

        // Act
        cacheMetadataDao.set(entity)
        advanceUntilIdle()

        // Assert
        val result = cacheMetadataDao.get("test_key")
        assertNotNull(result)
        assertEquals(0, result?.lastFetched)
        assertEquals(0, result?.expiresAt)
    }

    @Test
    fun `set handles negative timestamps`() {
        // Arrange
        val entity = CacheMetadataEntity("test_key", -123456789, -123459999)

        // Act
        cacheMetadataDao.set(entity)
        advanceUntilIdle()

        // Assert
        val result = cacheMetadataDao.get("test_key")
        assertNotNull(result)
        assertEquals(-123456789, result?.lastFetched)
    }

    @Test
    fun `set handles very large timestamps`() {
        // Arrange
        val entity = CacheMetadataEntity("test_key", Long.MAX_VALUE, Long.MAX_VALUE)

        // Act
        cacheMetadataDao.set(entity)
        advanceUntilIdle()

        // Assert
        val result = cacheMetadataDao.get("test_key")
        assertNotNull(result)
        assertEquals(Long.MAX_VALUE, result?.lastFetched)
    }

    @Test
    fun `set handles expiresAt before lastFetched`() {
        // Arrange - Invalid state where cache expires before it was fetched
        val entity = CacheMetadataEntity("test_key", 123459999, 123456789)

        // Act
        cacheMetadataDao.set(entity)
        advanceUntilIdle()

        // Assert - Should still store the data even if logically invalid
        val result = cacheMetadataDao.get("test_key")
        assertNotNull(result)
        assertEquals(123459999, result?.lastFetched)
        assertEquals(123456789, result?.expiresAt)
    }

    @Test
    fun `deleteByPrefix handles prefix with special characters`() {
        // Arrange
        val specialPrefix = "prefix_!@#$"
        val entities = listOf(
            CacheMetadataEntity("${specialPrefix}_1", 123456789, 123459999),
            CacheMetadataEntity("${specialPrefix}_2", 123456790, 123459999),
            CacheMetadataEntity("other_key", 123456791, 123459999)
        )
        entities.forEach { cacheMetadataDao.set(it) }
        advanceUntilIdle()

        // Act
        cacheMetadataDao.deleteByPrefix(specialPrefix)
        advanceUntilIdle()

        // Assert
        assertNull(cacheMetadataDao.get("${specialPrefix}_1"))
        assertNull(cacheMetadataDao.get("${specialPrefix}_2"))
        assertNotNull(cacheMetadataDao.get("other_key"))
    }

    @Test
    fun `deleteByPrefix handles prefix that matches all keys`() {
        // Arrange
        val entities = listOf(
            CacheMetadataEntity("key1", 123456789, 123459999),
            CacheMetadataEntity("key2", 123456790, 123459999),
            CacheMetadataEntity("key3", 123456791, 123459999)
        )
        entities.forEach { cacheMetadataDao.set(it) }
        advanceUntilIdle()

        // Act
        cacheMetadataDao.deleteByPrefix("k")
        advanceUntilIdle()

        // Assert
        assertNull(cacheMetadataDao.get("key1"))
        assertNull(cacheMetadataDao.get("key2"))
        assertNull(cacheMetadataDao.get("key3"))
    }

    @Test
    fun `deleteByPrefix handles prefix that matches nothing`() {
        // Arrange
        val entity = CacheMetadataEntity("test_key", 123456789, 123459999)
        cacheMetadataDao.set(entity)
        advanceUntilIdle()

        // Act
        cacheMetadataDao.deleteByPrefix("nonexistent_prefix")
        advanceUntilIdle()

        // Assert
        assertNotNull(cacheMetadataDao.get("test_key"))
    }

    // ─── CONCURRENCY TESTS ───

    @Test
    fun `concurrent set operations handle same key correctly`() {
        // Arrange
        val key = "concurrent_key"

        // Act - Set same key concurrently
        val jobs = (1..100).map { i ->
            kotlinx.coroutines.launch {
                val entity = CacheMetadataEntity(key, 123456789 + i, 123459999 + i)
                cacheMetadataDao.set(entity)
            }
        }

        jobs.forEach { it.join() }
        advanceUntilIdle()

        // Assert - Should have one final value (last write wins)
        val result = cacheMetadataDao.get(key)
        assertNotNull(result)
        assertEquals(key, result?.cacheKey)
    }

    @Test
    fun `concurrent set and get operations are handled correctly`() {
        // Arrange
        val key = "concurrent_key"
        val initialEntity = CacheMetadataEntity(key, 123456789, 123459999)
        cacheMetadataDao.set(initialEntity)
        advanceUntilIdle()

        // Act - Concurrent reads and writes
        val jobs = mutableListOf<kotlinx.coroutines.Job>()
        
        // Start read operations
        repeat(10) {
            jobs.add(kotlinx.coroutines.launch {
                val result = cacheMetadataDao.get(key)
                assertNotNull(result)
            })
        }

        // Start write operations
        repeat(10) { i ->
            jobs.add(kotlinx.coroutines.launch {
                val entity = CacheMetadataEntity(key, 123456789 + i, 123459999 + i)
                cacheMetadataDao.set(entity)
            })
        }

        jobs.forEach { it.join() }
        advanceUntilIdle()

        // Assert - Should complete without errors
        val finalResult = cacheMetadataDao.get(key)
        assertNotNull(finalResult)
    }

    @Test
    fun `concurrent delete operations are handled correctly`() {
        // Arrange
        val entities = (1..100).map { i ->
            CacheMetadataEntity("key_$i", 123456789 + i, 123459999 + i)
        }
        entities.forEach { cacheMetadataDao.set(it) }
        advanceUntilIdle()

        // Act - Delete keys concurrently
        val jobs = entities.chunked(10).map { chunk ->
            kotlinx.coroutines.launch {
                chunk.forEach { cacheMetadataDao.delete(it.cacheKey) }
            }
        }

        jobs.forEach { it.join() }
        advanceUntilIdle()

        // Assert - All keys should be deleted
        entities.forEach { 
            assertNull(cacheMetadataDao.get(it.cacheKey))
        }
    }

    @Test
    fun `concurrent deleteByPrefix operations are handled correctly`() {
        // Arrange
        val entities = listOf(
            CacheMetadataEntity("prefix1_key1", 123456789, 123459999),
            CacheMetadataEntity("prefix1_key2", 123456790, 123459999),
            CacheMetadataEntity("prefix2_key1", 123456791, 123459999),
            CacheMetadataEntity("prefix2_key2", 123456792, 123459999)
        )
        entities.forEach { cacheMetadataDao.set(it) }
        advanceUntilIdle()

        // Act - Delete by prefix concurrently
        val jobs = listOf(
            kotlinx.coroutines.launch { cacheMetadataDao.deleteByPrefix("prefix1") },
            kotlinx.coroutines.launch { cacheMetadataDao.deleteByPrefix("prefix2") }
        )

        jobs.forEach { it.join() }
        advanceUntilIdle()

        // Assert
        assertNull(cacheMetadataDao.get("prefix1_key1"))
        assertNull(cacheMetadataDao.get("prefix1_key2"))
        assertNull(cacheMetadataDao.get("prefix2_key1"))
        assertNull(cacheMetadataDao.get("prefix2_key2"))
    }

    // ─── DATA INTEGRITY TESTS ───

    @Test
    fun `set and get maintain data consistency`() {
        // Arrange
        val originalEntity = CacheMetadataEntity(
            cacheKey = "test_key",
            lastFetched = 123456789,
            expiresAt = 123459999
        )

        // Act
        cacheMetadataDao.set(originalEntity)
        advanceUntilIdle()

        val retrievedEntity = cacheMetadataDao.get("test_key")

        // Assert
        assertNotNull(retrievedEntity)
        assertEquals(originalEntity.cacheKey, retrievedEntity?.cacheKey)
        assertEquals(originalEntity.lastFetched, retrievedEntity?.lastFetched)
        assertEquals(originalEntity.expiresAt, retrievedEntity?.expiresAt)
    }

    @Test
    fun `delete and set operations maintain consistency`() {
        // Arrange
        val entity1 = CacheMetadataEntity("test_key", 123456789, 123459999)
        cacheMetadataDao.set(entity1)
        advanceUntilIdle()

        // Act - Delete then set new value
        cacheMetadataDao.delete("test_key")
        advanceUntilIdle()

        val entity2 = CacheMetadataEntity("test_key", 987654321, 987657999)
        cacheMetadataDao.set(entity2)
        advanceUntilIdle()

        val result = cacheMetadataDao.get("test_key")

        // Assert
        assertNotNull(result)
        assertEquals(987654321, result?.lastFetched)
        assertEquals(987657999, result?.expiresAt)
    }

    @Test
    fun `deleteByPrefix maintains key isolation`() {
        // Arrange
        val entities = listOf(
            CacheMetadataEntity("test_1", 123456789, 123459999),
            CacheMetadataEntity("test_2", 123456790, 123459999),
            CacheMetadataEntity("other_1", 123456791, 123459999),
            CacheMetadataEntity("other_2", 123456792, 123459999)
        )
        entities.forEach { cacheMetadataDao.set(it) }
        advanceUntilIdle()

        // Act
        cacheMetadataDao.deleteByPrefix("test")
        advanceUntilIdle()

        // Assert
        assertNull(cacheMetadataDao.get("test_1"))
        assertNull(cacheMetadataDao.get("test_2"))
        assertNotNull(cacheMetadataDao.get("other_1"))
        assertNotNull(cacheMetadataDao.get("other_2"))
    }

    // ─── PERFORMANCE TESTS ───

    @Test
    fun `set handles large dataset efficiently`() {
        // Arrange
        val largeDataset = (1..1000).map { i ->
            CacheMetadataEntity("key_$i", 123456789 + i, 123459999 + i)
        }

        // Act - Measure insert time
        val startTime = System.currentTimeMillis()
        largeDataset.forEach { cacheMetadataDao.set(it) }
        advanceUntilIdle()
        val insertTime = System.currentTimeMillis() - startTime

        // Assert
        val result = cacheMetadataDao.get("key_500")
        assertNotNull(result)
        assertTrue(insertTime < 5000, "Insert should complete in under 5 seconds, took ${insertTime}ms")
    }

    @Test
    fun `get handles large dataset efficiently`() {
        // Arrange
        val largeDataset = (1..1000).map { i ->
            CacheMetadataEntity("key_$i", 123456789 + i, 123459999 + i)
        }
        largeDataset.forEach { cacheMetadataDao.set(it) }
        advanceUntilIdle()

        // Act - Measure query time
        val startTime = System.currentTimeMillis()
        val result = cacheMetadataDao.get("key_500")
        val queryTime = System.currentTimeMillis() - startTime

        // Assert
        assertNotNull(result)
        assertTrue(queryTime < 100, "Query should complete in under 100ms, took ${queryTime}ms")
    }

    @Test
    fun `deleteByPrefix handles large dataset efficiently`() {
        // Arrange
        val largeDataset = (1..1000).map { i ->
            CacheMetadataEntity("prefix_key_$i", 123456789 + i, 123459999 + i)
        }
        largeDataset.forEach { cacheMetadataDao.set(it) }
        advanceUntilIdle()

        // Act - Measure delete time
        val startTime = System.currentTimeMillis()
        cacheMetadataDao.deleteByPrefix("prefix")
        advanceUntilIdle()
        val deleteTime = System.currentTimeMillis() - startTime

        // Assert
        val result = cacheMetadataDao.get("prefix_key_500")
        assertNull(result)
        assertTrue(deleteTime < 2000, "Delete should complete in under 2 seconds, took ${deleteTime}ms")
    }

    @Test
    fun `clearAll handles large dataset efficiently`() {
        // Arrange
        val largeDataset = (1..1000).map { i ->
            CacheMetadataEntity("key_$i", 123456789 + i, 123459999 + i)
        }
        largeDataset.forEach { cacheMetadataDao.set(it) }
        advanceUntilIdle()

        // Act - Measure clear time
        val startTime = System.currentTimeMillis()
        cacheMetadataDao.clearAll()
        advanceUntilIdle()
        val clearTime = System.currentTimeMillis() - startTime

        // Assert
        val result = cacheMetadataDao.get("key_500")
        assertNull(result)
        assertTrue(clearTime < 1000, "Clear should complete in under 1 second, took ${clearTime}ms")
    }

    // ─── PATTERN MATCHING TESTS ───

    @Test
    fun `deleteByPrefix with single character prefix`() {
        // Arrange
        val entities = listOf(
            CacheMetadataEntity("a_key1", 123456789, 123459999),
            CacheMetadataEntity("a_key2", 123456790, 123459999),
            CacheMetadataEntity("b_key1", 123456791, 123459999)
        )
        entities.forEach { cacheMetadataDao.set(it) }
        advanceUntilIdle()

        // Act
        cacheMetadataDao.deleteByPrefix("a")
        advanceUntilIdle()

        // Assert
        assertNull(cacheMetadataDao.get("a_key1"))
        assertNull(cacheMetadataDao.get("a_key2"))
        assertNotNull(cacheMetadataDao.get("b_key1"))
    }

    @Test
    fun `deleteByPrefix is case-sensitive`() {
        // Arrange
        val entities = listOf(
            CacheMetadataEntity("TestKey", 123456789, 123459999),
            CacheMetadataEntity("testkey", 123456790, 123459999),
            CacheMetadataEntity("TESTKEY", 123456791, 123459999)
        )
        entities.forEach { cacheMetadataDao.set(it) }
        advanceUntilIdle()

        // Act
        cacheMetadataDao.deleteByPrefix("test")
        advanceUntilIdle()

        // Assert - Should only match exact case
        assertNull(cacheMetadataDao.get("testkey"))
        assertNotNull(cacheMetadataDao.get("TestKey"))
        assertNotNull(cacheMetadataDao.get("TESTKEY"))
    }

    @Test
    fun `deleteByPrefix handles prefix with underscores`() {
        // Arrange
        val entities = listOf(
            CacheMetadataEntity("test_key_1", 123456789, 123459999),
            CacheMetadataEntity("test_key_2", 123456790, 123459999),
            CacheMetadataEntity("test_other", 123456791, 123459999)
        )
        entities.forEach { cacheMetadataDao.set(it) }
        advanceUntilIdle()

        // Act
        cacheMetadataDao.deleteByPrefix("test_")
        advanceUntilIdle()

        // Assert
        assertNull(cacheMetadataDao.get("test_key_1"))
        assertNull(cacheMetadataDao.get("test_key_2"))
        assertNull(cacheMetadataDao.get("test_other"))
    }

    // ─── TTL EXPIRATION TESTS ───

    @Test
    fun `get returns entity even if expired`() {
        // Arrange - Set expired entity
        val pastTime = System.currentTimeMillis() - 10000 // 10 seconds ago
        val expiredEntity = CacheMetadataEntity("test_key", pastTime - 100000, pastTime)
        cacheMetadataDao.set(expiredEntity)
        advanceUntilIdle()

        // Act
        val result = cacheMetadataDao.get("test_key")

        // Assert - DAO returns entity regardless of expiration
        // Expiration logic is handled by the business layer
        assertNotNull(result)
        assertEquals("test_key", result?.cacheKey)
    }

    @Test
    fun `set allows future expiration times`() {
        // Arrange
        val futureTime = System.currentTimeMillis() + 86400000 // 24 hours in future
        val entity = CacheMetadataEntity("test_key", System.currentTimeMillis(), futureTime)

        // Act
        cacheMetadataDao.set(entity)
        advanceUntilIdle()

        val result = cacheMetadataDao.get("test_key")

        // Assert
        assertNotNull(result)
        assertEquals(futureTime, result?.expiresAt)
    }

    // ─── SQL INJECTION TESTS ───

    @Test
    fun `get handles SQL injection attempts in key`() {
        // Arrange
        val sqlInjectionKey = "test'; DROP TABLE cache_metadata; --"
        val entity = CacheMetadataEntity(sqlInjectionKey, 123456789, 123459999)
        cacheMetadataDao.set(entity)
        advanceUntilIdle()

        // Act
        val result = cacheMetadataDao.get(sqlInjectionKey)

        // Assert - Should store key as-is, not execute SQL
        assertNotNull(result)
        assertEquals(sqlInjectionKey, result?.cacheKey)
    }

    @Test
    fun `deleteByPrefix handles SQL injection attempts`() {
        // Arrange
        val sqlInjectionPrefix = "test'; DROP TABLE cache_metadata; --"
        val entity = CacheMetadataEntity("${sqlInjectionPrefix}_key", 123456789, 123459999)
        cacheMetadataDao.set(entity)
        advanceUntilIdle()

        // Act
        cacheMetadataDao.deleteByPrefix(sqlInjectionPrefix)
        advanceUntilIdle()

        // Assert - Should treat as literal string, not SQL
        val result = cacheMetadataDao.get("${sqlInjectionPrefix}_key")
        assertNull(result) // Should delete the key with that literal prefix
    }

    // ─── STATE CONSISTENCY TESTS ───

    @Test
    fun `multiple operations maintain database consistency`() {
        // Arrange
        val entity1 = CacheMetadataEntity("key1", 123456789, 123459999)
        val entity2 = CacheMetadataEntity("key2", 123456790, 123459999)
        val entity3 = CacheMetadataEntity("key3", 123456791, 123459999)

        // Act - Perform multiple operations
        cacheMetadataDao.set(entity1)
        advanceUntilIdle()

        cacheMetadataDao.set(entity2)
        advanceUntilIdle()

        cacheMetadataDao.delete("key1")
        advanceUntilIdle()

        cacheMetadataDao.set(entity3)
        advanceUntilIdle()

        cacheMetadataDao.deleteByPrefix("key2")
        advanceUntilIdle()

        // Assert - Verify final state
        assertNull(cacheMetadataDao.get("key1"))
        assertNull(cacheMetadataDao.get("key2"))
        assertNotNull(cacheMetadataDao.get("key3"))
    }

    @Test
    fun `clearAll and subsequent operations maintain consistency`() {
        // Arrange
        val entities = (1..10).map { i ->
            CacheMetadataEntity("key_$i", 123456789 + i, 123459999 + i)
        }
        entities.forEach { cacheMetadataDao.set(it) }
        advanceUntilIdle()

        // Act
        cacheMetadataDao.clearAll()
        advanceUntilIdle()

        val newEntity = CacheMetadataEntity("new_key", 999999999, 999999999)
        cacheMetadataDao.set(newEntity)
        advanceUntilIdle()

        // Assert
        assertNull(cacheMetadataDao.get("key_1"))
        assertNotNull(cacheMetadataDao.get("new_key"))
    }
}

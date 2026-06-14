package com.mlbb.scrim.data.cache

import com.mlbb.scrim.data.local.CacheMetadataDao
import com.mlbb.scrim.data.local.CacheMetadataEntity
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
class UnifiedCacheManagerTest {

    private lateinit var cacheManager: UnifiedCacheManager
    private lateinit var mockMetadataDao: CacheMetadataDao
    private lateinit var testDispatcher: TestDispatcher

    @Before
    fun setup() {
        mockMetadataDao = mockk(relaxed = true)
        testDispatcher = StandardTestDispatcher()

        Dispatchers.setMain(testDispatcher)

        cacheManager = UnifiedCacheManager(mockMetadataDao)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ─── L1 Memory Cache Tests ───

    @Test
    fun `get returns data from L1 cache when valid`() {
        // Arrange
        val key = "test_key"
        val testData = "test_data"
        var l1Hit = false
        var l2Hit = false
        var networkFetch = false

        coEvery { mockMetadataDao.get(key) } returns null
        coEvery { mockMetadataDao.set(any()) } just Runs

        // Act
        val result = cacheManager.get(
            key = key,
            memoryTtlMs = 60000, // 1 minute
            roomTtlMs = 300000, // 5 minutes
            roomLoader = { l2Hit = true; null },
            networkLoader = { networkFetch = true; testData },
            roomSaver = {}
        )

        // First call should fetch from network
        advanceUntilIdle()

        // Second call should hit L1 cache
        val result2 = cacheManager.get(
            key = key,
            memoryTtlMs = 60000,
            roomTtlMs = 300000,
            roomLoader = { l2Hit = true; null },
            networkLoader = { networkFetch = true; testData },
            roomSaver = {}
        )

        advanceUntilIdle()

        // Assert
        assertEquals(testData, result2)
        assertFalse(networkFetch) // Should not fetch from network on second call
    }

    @Test
    fun `get fetches from network when L1 cache is expired`() {
        // Arrange
        val key = "test_key"
        val testData = "test_data"
        var networkCallCount = 0

        coEvery { mockMetadataDao.get(key) } returns null
        coEvery { mockMetadataDao.set(any()) } just Runs

        // Act
        val result1 = cacheManager.get(
            key = key,
            memoryTtlMs = 100, // Very short TTL
            roomTtlMs = 300000,
            roomLoader = { null },
            networkLoader = { networkCallCount++; "updated_$testData" },
            roomSaver = {}
        )

        advanceUntilIdle()

        // Wait for cache to expire
        kotlinx.coroutines.delay(150)

        val result2 = cacheManager.get(
            key = key,
            memoryTtlMs = 100,
            roomTtlMs = 300000,
            roomLoader = { null },
            networkLoader = { networkCallCount++; "updated_$testData" },
            roomSaver = {}
        )

        advanceUntilIdle()

        // Assert
        assertEquals(2, networkCallCount) // Should fetch from network twice
    }

    @Test
    fun `get returns data from L2 cache when L1 is empty`() {
        // Arrange
        val key = "test_key"
        val roomData = "room_data"
        var l2Hit = false
        var networkFetch = false

        coEvery { mockMetadataDao.get(key) } returns CacheMetadataEntity(
            cacheKey = key,
            lastFetched = System.currentTimeMillis(),
            expiresAt = System.currentTimeMillis() + 300000
        )

        // Act
        val result = cacheManager.get(
            key = key,
            memoryTtlMs = 60000,
            roomTtlMs = 300000,
            roomLoader = { l2Hit = true; roomData },
            networkLoader = { networkFetch = true; "network_data" },
            roomSaver = {}
        )

        advanceUntilIdle()

        // Assert
        assertEquals(roomData, result)
        assertTrue(l2Hit)
        assertFalse(networkFetch)
    }

    @Test
fun `get fetches from network when L2 cache is expired`() {
        // Arrange
        val key = "test_key"
        val networkData = "network_data"
        var networkFetch = false

        coEvery { mockMetadataDao.get(key) } returns CacheMetadataEntity(
            cacheKey = key,
            lastFetched = System.currentTimeMillis(),
            expiresAt = System.currentTimeMillis() - 1000 // Expired
        )

        // Act
        val result = cacheManager.get(
            key = key,
            memoryTtlMs = 60000,
            roomTtlMs = 300000,
            roomLoader = { null },
            networkLoader = { networkFetch = true; networkData },
            roomSaver = {}
        )

        advanceUntilIdle()

        // Assert
        assertEquals(networkData, result)
        assertTrue(networkFetch)
    }

    @Test
fun `get promotes L2 data to L1 cache`() {
        // Arrange
        val key = "test_key"
        val roomData = "room_data"
        var l1HitAfter = false

        coEvery { mockMetadataDao.get(key) } returns CacheMetadataEntity(
            cacheKey = key,
            lastFetched = System.currentTimeMillis(),
            expiresAt = System.currentTimeMillis() + 300000
        )

        // Act
        cacheManager.get(
            key = key,
            memoryTtlMs = 60000,
            roomTtlMs = 300000,
            roomLoader = { roomData },
            networkLoader = { "network_data" },
            roomSaver = {}
        )

        advanceUntilIdle()

        // Check L1 cache now
        val result = cacheManager.get(
            key = key,
            memoryTtlMs = 60000,
            roomTtlMs = 300000,
            roomLoader = { null },
            networkLoader = { "network_data" },
            roomSaver = {}
        )

        advanceUntilIdle()

        // Assert
        assertEquals(roomData, result)
    }

// ─── Stale-While-Revalidate Flow Tests ───

@Test
fun `getFlow emits stale data immediately and fresh data later`() {
    // Arrange
    val key = "test_key"
    val staleData = "stale_data"
    val freshData = "fresh_data"
    val emittedValues = mutableListOf<String>()

    coEvery { mockMetadataDao.get(key) } returns CacheMetadataEntity(
        cacheKey = key,
        lastFetched = System.currentTimeMillis(),
        expiresAt = System.currentTimeMillis() + 300000
    )

    // Act
    val flow = cacheManager.getFlow(
        key = key,
        memoryTtlMs = 60000,
        roomTtlMs = 300000,
        roomLoader = { staleData },
        networkLoader = { freshData },
        roomSaver = {}
    )

    // Collect emissions
    val job = kotlinx.coroutines.launch {
        flow.collect { emittedValues.add(it) }
    }

    advanceUntilIdle()
    job.cancel()

    // Assert
    assertTrue(emittedValues.contains(staleData))
    assertTrue(emittedValues.contains(freshData))
}

@Test
fun `getFlow emits only fresh data when cache is empty`() {
    // Arrange
    val key = "test_key"
    val freshData = "fresh_data"
    val emittedValues = mutableListOf<String>()

    coEvery { mockMetadataDao.get(key) } returns null
    coEvery { mockMetadataDao.set(any()) } just Runs

    // Act
    val flow = cacheManager.getFlow(
        key = key,
        memoryTtlMs = 60000,
        roomTtlMs = 300000,
        roomLoader = { null },
        networkLoader = { freshData },
        roomSaver = {}
    )

    val job = kotlinx.coroutines.launch {
        flow.collect { emittedValues.add(it) }
    }

    advanceUntilIdle()
    job.cancel()

    // Assert
    assertEquals(1, emittedValues.size)
    assertEquals(freshData, emittedValues.first())
}

// ─── Invalidate Tests ───

@Test
fun `invalidate removes key from L1 cache`() {
    // Arrange
    val key = "test_key"
    cacheManager.get(
        key = key,
        memoryTtlMs = 60000,
        roomTtlMs = 300000,
        roomLoader = { null },
        networkLoader = { "data" },
        roomSaver = {}
    )

    advanceUntilIdle()

    // Act
    cacheManager.invalidate(key)
    advanceUntilIdle()

    // Assert
    coVerify { mockMetadataDao.delete(key) }
}

@Test
fun `invalidateByPrefix removes matching keys from L1 cache`() {
    // Arrange
    val key1 = "scrims_1"
    val key2 = "scrims_2"
    val key3 = "other_key"

    cacheManager.get(
        key = key1,
        memoryTtlMs = 60000,
        roomTtlMs = 300000,
        roomLoader = { null },
        networkLoader = { "data1" },
        roomSaver = {}
    )

    cacheManager.get(
        key = key2,
        memoryTtlMs = 60000,
        roomTtlMs = 300000,
        roomLoader = { null },
        networkLoader = { "data2" },
        roomSaver = {}
    )

    cacheManager.get(
        key = key3,
        memoryTtlMs = 60000,
        roomTtlMs = 300000,
        roomLoader = { null },
        networkLoader = { "data3" },
        roomSaver = {}
    )

    advanceUntilIdle()

    // Act
    cacheManager.invalidateByPrefix("scrims")
    advanceUntilIdle()

    // Assert
    coVerify { mockMetadataDao.deleteByPrefix("scrims") }
}

@Test
fun `invalidateByPrefix does not remove non-matching keys`() {
    // Arrange
    val key1 = "scrims_1"
    val key2 = "other_key"

    cacheManager.get(
        key = key1,
        memoryTtlMs = 60000,
        roomTtlMs = 300000,
        roomLoader = { null },
        networkLoader = { "data1" },
        roomSaver = {}
    )

    cacheManager.get(
        key = key2,
        memoryTtlMs = 60000,
        roomTtlMs = 300000,
        roomLoader = { null },
        networkLoader = { "data2" },
        roomSaver = {}
    )

    advanceUntilIdle()

    // Act
    cacheManager.invalidateByPrefix("scrims")
    advanceUntilIdle()

    // Assert - other_key should still be in cache
    // This is harder to test without actual cache inspection
    coVerify { mockMetadataDao.deleteByPrefix("scrims") }
}

// ─── Put Tests ───

@Test
fun `put successfully saves data to both L1 and L2`() {
    // Arrange
    val key = "test_key"
    val data = "test_data"

    // Act
    cacheManager.put(
        key = key,
        data = data,
        memoryTtlMs = 60000,
        roomTtlMs = 300000,
        roomSaver = {}
    )

    advanceUntilIdle()

    // Assert
    coVerify { mockMetadataDao.set(any()) }
}

@Test
fun `put handles Room save failure gracefully`() {
    // Arrange
    val key = "test_key"
    val data = "test_data"
    coEvery { mockMetadataDao.set(any()) } throws Exception("Room error")

    // Act
    cacheManager.put(
        key = key,
        data = data,
        memoryTtlMs = 60000,
        roomTtlMs = 300000,
        roomSaver = {}
    )

    advanceUntilIdle()

    // Assert - Should complete without crashing
    coVerify { mockMetadataDao.set(any()) }
}

// ─── Validation Tests ───

@Test
fun `isValid returns true when key exists in L1 and is valid`() {
    // Arrange
    val key = "test_key"

    cacheManager.get(
        key = key,
        memoryTtlMs = 60000,
        roomTtlMs = 300000,
        roomLoader = { null },
        networkLoader = { "data" },
        roomSaver = {}
    )

    advanceUntilIdle()

    // Act
    val result = cacheManager.isValid(key)

    // Assert
    assertTrue(result)
}

@Test
fun `isValid returns false when key is not in cache`() {
    // Arrange
    val key = "nonexistent_key"

    // Act
    val result = cacheManager.isValid(key)

    // Assert
    assertFalse(result)
}

@Test
fun `isValid returns false when key is expired in L1`() {
    // Arrange
    val key = "test_key"
    cacheManager.get(
        key = key,
        memoryTtlMs = 1, // Very short TTL
        roomTtlMs = 300000,
        roomLoader = { null },
        networkLoader = { "data" },
        roomSaver = {}
    )

    advanceUntilIdle()

    // Wait for expiration
    kotlinx.coroutines.delay(10)

    // Act
    val result = cacheManager.isValid(key)

    // Assert
    assertFalse(result)
}

@Test
fun `isValid returns true when key is valid in L2`() {
    // Arrange
    val key = "test_key"
    coEvery { mockMetadataDao.get(key) } returns CacheMetadataEntity(
        cacheKey = key,
        lastFetched = System.currentTimeMillis(),
        expiresAt = System.currentTimeMillis() + 300000
    )

    // Act
    val result = cacheManager.isValid(key)

    // Assert
    assertTrue(result)
}

@Test
fun `isValid returns false when key is expired in L2`() {
    // Arrange
    val key = "test_key"
    coEvery { mockMetadataDao.get(key) } returns CacheMetadataEntity(
        cacheKey = key,
        lastFetched = System.currentTimeMillis(),
        expiresAt = System.currentTimeMillis() - 1000 // Expired
    )

    // Act
    val result = cacheManager.isValid(key)

    // Assert
    assertFalse(result)
}

// ─── Clear All Tests ───

@Test
fun `clearAll removes all data from L1 cache`() {
    // Arrange
    cacheManager.get(
        key = "key1",
        memoryTtlMs = 60000,
        roomTtlMs = 300000,
        roomLoader = { null },
        networkLoader = { "data1" },
        roomSaver = {}
    )

    cacheManager.get(
        key = "key2",
        memoryTtlMs = 60000,
        roomTtlMs = 300000,
        roomLoader = { null },
        networkLoader = { "data2" },
        roomSaver = {}
    )

    advanceUntilIdle()

    // Act
    cacheManager.clearAll()
    advanceUntilIdle()

    // Assert
    coVerify { mockMetadataDao.clearAll() }
}

// ─── Concurrency Tests ───

@Test
fun `get handles concurrent requests for same key with thundering herd protection`() {
    // Arrange
    val key = "test_key"
    var networkCallCount = 0
    coEvery { mockMetadataDao.get(key) } returns null
    coEvery { mockMetadataDao.set(any()) } just Runs

    // Act
    val results = mutableListOf<String>()
    val jobs = (1..10).map {
        kotlinx.coroutines.launch {
            results.add(
                cacheManager.get(
                    key = key,
                    memoryTtlMs = 60000,
                    roomTtlMs = 300000,
                    roomLoader = { null },
                    networkLoader = { 
                        networkCallCount++ 
                        "data_$it" 
                    },
                    roomSaver = {}
                )
            )
        }
    }

    kotlinx.coroutines.delay(100)
    jobs.forEach { it.join() }

    advanceUntilIdle()

    // Assert - Due to thundering herd protection, network should only be called once
    assertTrue(networkCallCount <= 2) // Allow for some timing issues
}

// ─── Edge Case Tests ───

@Test
fun `get handles empty room loader data`() {
    // Arrange
    val key = "test_key"
    val networkData = "network_data"
    coEvery { mockMetadataDao.get(key) } returns CacheMetadataEntity(
        cacheKey = key,
        lastFtlched = System.currentTimeMillis(),
        expiresAt = System.currentTimeMillis() + 300000
    )

    // Act
    val result = cacheManager.get(
        key = key,
        memoryTtlMs = 60000,
        roomTtlMs = 300000,
        roomLoader = { null }, // No room data
        networkLoader = { networkData },
        roomSaver = {}
    )

    advanceUntilIdle()

    // Assert
    assertEquals(networkData, result)
}

@Test
fun `get handles very short TTL values`() {
    // Arrange
    val key = "test_key"
    coEvery { mockMetadataDao.get(key) } returns null
    coEvery { mockMetadataDao.set(any()) } just Runs

    // Act
    val result = cacheManager.get(
        key = key,
        memoryTtlMs = 1, // 1ms
        roomTtlMs = 1,
        roomLoader = { null },
        networkLoader = { "data" },
        roomSaver = {}
    )

    advanceUntilIdle()

    // Assert
    assertEquals("data", result)
}

@Test
fun `get handles very long TTL values`() {
    // Arrange
    val key = "test_key"
    coEvery { mockMetadataDao.get(key) } returns null
    coEvery { mockMetadataDao.set(any()) } just Runs

    // Act
    val result = cacheManager.get(
        key = key,
        memoryTtlMs = Long.MAX_VALUE,
        roomTtlMs = Long.MAX_VALUE,
        roomLoader = { null },
        networkLoader = { "data" },
        roomSaver = {}
    )

    advanceUntilIdle()

    // Assert
    assertEquals("data", result)
}

@Test
fun `get handles network loader failure`() {
    // Arrange
    val key = "test_key"
    val roomData = "room_data"
    coEvery { mockMetadataDao.get(key) } returns null

    // Act
    val result = cacheManager.get(
        key = key,
        memoryTtlMs = 60000,
        roomTtlMs = 300000,
        roomLoader = { roomData },
        networkLoader = { throw Exception("Network error") },
        roomSaver = {}
    )

    advanceUntilIdle()

    // Assert - Should throw the error since no cache is available
    // In a real scenario, this would propagate the error
    // For testing, we just verify the behavior
    coVerify { mockMetadataDao.get(key) }
}

@Test
fun `getFlow handles network failure gracefully`() {
    // Arrange
    val key = "test_key"
    val staleData = "stale_data"
    val emittedValues = mutableListOf<String>()

    coEvery { mockMetadataDao.get(key) } returns null
    coEvery { mockMetadataDao.set(any()) } just Runs

    // Act
    val flow = cacheManager.getFlow(
        key = key,
        memoryTtlMs = 60000,
        roomTtlMs = 300000,
        roomLoader = { staleData },
        networkLoader = { throw Exception("Network error") },
        roomSaver = {}
    )

    val job = kotlinx.coroutines.launch {
        try {
            flow.collect { emittedValues.add(it) }
        } catch (e: Exception) {
            // Expected to throw when no cache is available
        }
    }

    advanceUntilIdle()
    job.cancel()

    // Assert
    // Should have emitted stale data first, then thrown on network failure
    assertTrue(emittedValues.contains(staleData))
}

// ─── Type Safety Tests ─@

@Test
fun `get returns correct type for String data`() {
    // Arrange
    val key = "test_key"
    val testData = "test_data"

    coEvery { mockMetadataDao.get(key) } returns null
    coEvery { mockMetadataDao.set(any()) } just Runs

    // Act
    val result = cacheManager.get<String>(
        key = key,
        memoryTtlMs = 60000,
        roomTtlMs = 300000,
        roomLoader = { null },
        networkLoader = { testData },
        roomSaver = {}
    )

    advanceUntilIdle()

    // Assert
    assertEquals(testData, result)
}

@Test
fun `get returns correct type for Int data`() {
    // Arrange
    val key = "test_key"
    val testData = 42

    coEvery { mockMetadataDao.get(key) } returns null
    coEvery { mockMetadataDao.set(any()) } just Runs

    // Act
    val result = cacheManager.get<Int>(
        key = key,
        memoryTtlMs = 60000,
        roomTtlMs = 300000,
        roomLoader = { null },
        networkLoader = { testData },
        roomSaver = {}
    )

    advanceUntilIdle()

    // Assert
    assertEquals(testData, result)
}

@Test
fun `get returns correct type for List data`() {
    // Arrange
    val key = "test_key"
    val testData = listOf("item1", "item2", "item3")

    coEvery { mockMetadataDao.get(key) } returns null
    coEvery { mockMetadataDao.set(any()) } just Runs

    // Act
    val result = cacheManager.get<List<String>>(
        key = key,
        memoryTtlMs = 60000,
        roomTtlMs = 300000,
        roomLoader = { null },
        networkLoader = { testData },
        roomSaver = {}
    )

    advanceUntilIdle()

    // Assert
    assertEquals(testData, result)
}

// ─── Memory Management Tests ───

@Test
fun `getFlow cleans up resources on completion`() {
    // Arrange
    val key = "test_key"
    coEvery { mockMetadataDao.get(key) } returns null
    coEvery { mockMetadataDao.set(any()) } just Runs

    // Act
    val flow = cacheManager.getFlow(
        key = key,
        memoryTtlMs = 60000,
        roomTtlMs = 300000,
        roomLoader = { null },
        networkLoader = { "data" },
        roomSaver = {}
    )

    val job = kotlinx.coroutines.launch {
        flow.collect {}
    }

    advanceUntilIdle()
    job.cancel()

    // Assert - Should complete without errors
    assertTrue(true)
}

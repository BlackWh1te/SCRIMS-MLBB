package com.scrimslegends.app.data.cache

import timber.log.Timber
import com.scrimslegends.app.data.local.CacheMetadataDao
import com.scrimslegends.app.data.local.CacheMetadataEntity
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

/**
 * Unified cache coordinator implementing stale-while-revalidate pattern.
 *
 * L1 = In-memory ConcurrentHashMap with TTL
 * L2 = Room database with TTL tracked via CacheMetadataDao
 *
 * Usage pattern:
 *   1. Screen opens → get() returns L1 cache if valid
 *   2. If L1 miss → reads from L2 (Room), returns if valid
 *   3. If L2 miss or expired → calls network loader, saves to L1+L2
 *   4. On writes → invalidate() clears the key from L1+L2
 */
class UnifiedCacheManager(private val metadataDao: CacheMetadataDao) {

    companion object {
        private const val TAG = "UnifiedCacheManager"
    }

    // ─── In-Memory L1 Cache ───

    private data class MemoryEntry(
        val data: Any,
        val cachedAt: Long,
        val ttlMs: Long
    ) {
        fun isValid(): Boolean = (System.currentTimeMillis() - cachedAt) < ttlMs
    }

    private val memoryCache = ConcurrentHashMap<String, MemoryEntry>()
    private val fetchLocks = ConcurrentHashMap<String, Mutex>()

    /**
     * Get data with full L1 → L2 → Network fallback.
     *
     * @param key           Unique cache key (e.g. "leaderboard", "teams_list")
     * @param memoryTtlMs   How long to keep data in memory before revalidation
     * @param roomTtlMs     How long Room data is considered valid
     * @param roomLoader    Lambda to load data from Room (returns null if no cached data)
     * @param networkLoader Lambda to fetch fresh data from network
     * @param roomSaver     Lambda to save fresh data to Room
     * @return              The data (from cache or network)
     */
    @Suppress("UNCHECKED_CAST")
    suspend fun <T> get(
        key: String,
        memoryTtlMs: Long,
        roomTtlMs: Long,
        roomLoader: suspend () -> T?,
        networkLoader: suspend () -> T,
        roomSaver: suspend (T) -> Unit
    ): T {
        // L1: Memory cache
        val memEntry = memoryCache[key]
        if (memEntry != null && memEntry.isValid()) {
            Timber.d(TAG, "L1 HIT [$key] (${(System.currentTimeMillis() - memEntry.cachedAt) / 1000}s old)")
            return memEntry.data as T
        }

        // Prevent thundering herd: only one coroutine fetches per key
        val mutex = fetchLocks.getOrPut(key) { Mutex() }
        return mutex.withLock {
            // Double-check after acquiring lock (another coroutine may have populated it)
            val recheck = memoryCache[key]
            if (recheck != null && recheck.isValid()) {
                return@withLock recheck.data as T
            }

            // L2: Room cache
            val metadata = metadataDao.get(key)
            if (metadata != null && System.currentTimeMillis() < metadata.expiresAt) {
                val roomData = roomLoader()
                if (roomData != null) {
                    Timber.d(TAG, "L2 HIT [$key] (Room, expires in ${(metadata.expiresAt - System.currentTimeMillis()) / 1000}s)")
                    // Promote to L1
                    memoryCache[key] = MemoryEntry(roomData as Any, System.currentTimeMillis(), memoryTtlMs)
                    return@withLock roomData
                }
            }

            // L3: Network fetch
            Timber.d(TAG, "MISS [$key] → fetching from network")
            val freshData = networkLoader()

            // Save to L1
            memoryCache[key] = MemoryEntry(freshData as Any, System.currentTimeMillis(), memoryTtlMs)

            // Save to L2
            try {
                roomSaver(freshData)
                metadataDao.set(CacheMetadataEntity(
                    cacheKey = key,
                    lastFetched = System.currentTimeMillis(),
                    expiresAt = System.currentTimeMillis() + roomTtlMs
                ))
            } catch (e: Exception) {
                Timber.w(TAG, "Failed to save [$key] to Room", e)
            }

            freshData
        }
    }

    /**
     * Get data with stale-while-revalidate using Kotlin Flow.
     * Emits stale data from Room immediately if available, then fetches fresh data
     * from network in the background and emits it again if the cache was expired.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> getFlow(
        key: String,
        memoryTtlMs: Long,
        roomTtlMs: Long,
        roomLoader: suspend () -> T?,
        networkLoader: suspend () -> T,
        roomSaver: suspend (T) -> Unit
    ): kotlinx.coroutines.flow.Flow<T> = kotlinx.coroutines.flow.flow {
        var needNetworkFetch = true

        // Try L1 first
        val memEntry = memoryCache[key]
        if (memEntry != null) {
            emit(memEntry.data as T)
            if (memEntry.isValid()) {
                needNetworkFetch = false
            }
        }

        // Try L2
        if (needNetworkFetch) {
            val metadata = metadataDao.get(key)
            if (metadata != null) {
                val roomData = roomLoader()
                if (roomData != null) {
                    val isStillValid = System.currentTimeMillis() < metadata.expiresAt
                    if (memEntry == null) {
                        // Emit Room data if we didn't emit memory data
                        emit(roomData)
                    }
                    if (isStillValid) {
                        needNetworkFetch = false
                        memoryCache[key] = MemoryEntry(roomData as Any, System.currentTimeMillis(), memoryTtlMs)
                    }
                }
            }
        }

        // Network Fetch
        if (needNetworkFetch) {
            try {
                val freshData = networkLoader()
                memoryCache[key] = MemoryEntry(freshData as Any, System.currentTimeMillis(), memoryTtlMs)
                roomSaver(freshData)
                metadataDao.set(CacheMetadataEntity(
                    cacheKey = key,
                    lastFetched = System.currentTimeMillis(),
                    expiresAt = System.currentTimeMillis() + roomTtlMs
                ))
                emit(freshData)
            } catch (e: Exception) {
                Timber.w(TAG, "Network fetch failed for [$key]", e)
                // If we haven't emitted anything at all, throw the error
                if (memoryCache[key] == null && metadataDao.get(key) == null) {
                    throw e
                }
            }
        }
    }

    /**
     * Invalidate a specific cache key (both L1 and L2 metadata).
     * Call this after creates, updates, or deletes.
     */
    suspend fun invalidate(key: String) {
        memoryCache.remove(key)
        metadataDao.delete(key)
        Timber.d(TAG, "INVALIDATED [$key]")
    }

    /**
     * Invalidate all keys matching a prefix.
     * e.g. invalidateByPrefix("scrims") clears "scrims", "scrims_team_abc", etc.
     */
    suspend fun invalidateByPrefix(prefix: String) {
        memoryCache.keys.filter { it.startsWith(prefix) }.forEach { memoryCache.remove(it) }
        metadataDao.deleteByPrefix(prefix)
        Timber.d(TAG, "INVALIDATED prefix [$prefix*]")
    }

    /**
     * Force-put data into both L1 and L2 (useful after a write that returns updated data).
     */
    @Suppress("UNCHECKED_CAST")
    suspend fun <T> put(
        key: String,
        data: T,
        memoryTtlMs: Long,
        roomTtlMs: Long,
        roomSaver: suspend (T) -> Unit
    ) {
        memoryCache[key] = MemoryEntry(data as Any, System.currentTimeMillis(), memoryTtlMs)
        try {
            roomSaver(data)
            metadataDao.set(CacheMetadataEntity(
                cacheKey = key,
                lastFetched = System.currentTimeMillis(),
                expiresAt = System.currentTimeMillis() + roomTtlMs
            ))
        } catch (e: Exception) {
            Timber.w(TAG, "Failed to put [$key] to Room", e)
        }
    }

    /**
     * Check if a key exists and is valid in L1 or L2.
     */
    suspend fun isValid(key: String): Boolean {
        val memEntry = memoryCache[key]
        if (memEntry != null && memEntry.isValid()) return true
        val metadata = metadataDao.get(key)
        return metadata != null && System.currentTimeMillis() < metadata.expiresAt
    }

    /**
     * Clear ALL cached data (memory + metadata).
     */
    suspend fun clearAll() {
        memoryCache.clear()
        metadataDao.clearAll()
        Timber.d(TAG, "ALL CACHES CLEARED")
    }
}

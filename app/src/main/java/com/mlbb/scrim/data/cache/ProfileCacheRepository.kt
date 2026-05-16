package com.mlbb.scrim.data.cache

import android.util.Log
import com.mlbb.scrim.data.local.ProfileDao
import com.mlbb.scrim.data.local.ProfileEntity
import com.mlbb.scrim.data.service.ProfileDto
import com.mlbb.scrim.data.service.PostgrestFilter
import com.mlbb.scrim.data.service.SupabaseApiService
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

/**
 * Shared profile lookup cache to eliminate N+1 queries across all repositories.
 *
 * Problem: MatchResultRepository, TeamRepository, and ScrimRepository all fetch
 * individual profiles one-by-one when mapping DTOs to domain models.
 *
 * Solution: This class maintains an in-memory cache of recently-fetched profiles
 * and supports batch-fetching via Supabase's `in.()` filter.
 *
 * Memory TTL: 30 minutes (profiles rarely change)
 * Fallback: Room ProfileDao for offline access
 */
class ProfileCacheRepository(
    private val api: SupabaseApiService,
    private val profileDao: ProfileDao
) {
    companion object {
        private const val TAG = "ProfileCache"
        private const val MEMORY_TTL_MS = 30L * 60 * 1000 // 30 minutes
    }

    private data class CachedProfile(
        val dto: ProfileDto,
        val cachedAt: Long
    ) {
        fun isValid(): Boolean = (System.currentTimeMillis() - cachedAt) < MEMORY_TTL_MS
    }

    private val cache = ConcurrentHashMap<String, CachedProfile>()
    private val batchMutex = Mutex()

    /**
     * Get a single profile by user ID.
     * Checks memory → Room → network, in order.
     */
    suspend fun getProfile(userId: String): ProfileDto? {
        // L1: Memory
        val cached = cache[userId]
        if (cached != null && cached.isValid()) {
            return cached.dto
        }

        // L2: Room (quick fallback)
        val roomProfile = try {
            profileDao.getProfileById(userId).firstOrNull()
        } catch (_: Exception) { null }

        // L3: Network
        return try {
            val response = api.getProfileById(PostgrestFilter.eq(userId))
            val dto = response.body()?.firstOrNull()
            if (dto != null) {
                cache[userId] = CachedProfile(dto, System.currentTimeMillis())
                // Persist to Room
                try {
                    profileDao.insertProfile(ProfileEntity(
                        id = dto.id,
                        username = dto.username,
                        fullName = null,
                        avatarUrl = null,
                        rank = null,
                        role = dto.role,
                        bio = dto.bio,
                        points = 0,
                        lastUpdated = System.currentTimeMillis()
                    ))
                } catch (_: Exception) { }
            }
            dto ?: roomProfile?.let { ProfileDto(id = it.id, username = it.username) }
        } catch (e: Exception) {
            Log.w(TAG, "Network fetch failed for $userId, using Room fallback")
            roomProfile?.let { ProfileDto(id = it.id, username = it.username) }
        }
    }

    /**
     * Batch-fetch profiles for multiple user IDs.
     * Only fetches IDs not already cached. Returns a map of userId → ProfileDto.
     *
     * This is the key method that fixes the N+1 problem:
     * Instead of 10 individual API calls, we make 1 bulk call.
     */
    suspend fun getProfiles(userIds: List<String>): Map<String, ProfileDto> {
        if (userIds.isEmpty()) return emptyMap()

        val result = mutableMapOf<String, ProfileDto>()
        val uncachedIds = mutableListOf<String>()

        // Collect what we already have in memory
        for (id in userIds.distinct()) {
            val cached = cache[id]
            if (cached != null && cached.isValid()) {
                result[id] = cached.dto
            } else {
                uncachedIds.add(id)
            }
        }

        if (uncachedIds.isEmpty()) {
            Log.d(TAG, "Batch HIT: all ${userIds.size} profiles from memory")
            return result
        }

        // Batch fetch uncached profiles (single API call)
        return batchMutex.withLock {
            // Double-check after lock
            val stillUncached = uncachedIds.filter { id ->
                val rechecked = cache[id]
                if (rechecked != null && rechecked.isValid()) {
                    result[id] = rechecked.dto
                    false
                } else true
            }

            if (stillUncached.isNotEmpty()) {
                try {
                    val response = api.getProfiles(
                        idFilter = PostgrestFilter.inList(stillUncached)
                    )
                    val fetched = response.body() ?: emptyList()
                    Log.d(TAG, "Batch FETCH: ${stillUncached.size} requested, ${fetched.size} returned")

                    val entities = mutableListOf<ProfileEntity>()
                    for (dto in fetched) {
                        cache[dto.id] = CachedProfile(dto, System.currentTimeMillis())
                        result[dto.id] = dto
                        entities.add(ProfileEntity(
                            id = dto.id,
                            username = dto.username,
                            fullName = null,
                            avatarUrl = null,
                            rank = null,
                            role = dto.role,
                            bio = dto.bio,
                            points = 0,
                            lastUpdated = System.currentTimeMillis()
                        ))
                    }

                    // Bulk persist to Room
                    if (entities.isNotEmpty()) {
                        try { profileDao.insertProfiles(entities) } catch (_: Exception) { }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Batch fetch failed, trying Room fallback", e)
                    // Fallback: try Room for each uncached ID
                    for (id in stillUncached) {
                        try {
                            val entity = profileDao.getProfileById(id).firstOrNull()
                            if (entity != null) {
                                result[id] = ProfileDto(id = entity.id, username = entity.username)
                            }
                        } catch (_: Exception) { }
                    }
                }
            }

            result
        }
    }

    /**
     * Get username for a user ID (convenience method used by many repositories).
     */
    suspend fun getUsername(userId: String): String {
        return getProfile(userId)?.username ?: userId.take(8)
    }

    /**
     * Invalidate a specific user's cached profile (e.g. after profile update).
     */
    fun invalidate(userId: String) {
        cache.remove(userId)
    }

    /**
     * Clear all cached profiles.
     */
    fun clearAll() {
        cache.clear()
    }
}

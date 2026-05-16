package com.mlbb.scrim.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Tracks per-key cache freshness timestamps.
 * Used by UnifiedCacheManager to decide if Room data is still valid
 * without deserializing the actual cached content.
 */
@Entity(tableName = "cache_metadata")
data class CacheMetadataEntity(
    @PrimaryKey val cacheKey: String,   // e.g. "leaderboard", "scrims_team_abc123"
    val lastFetched: Long,              // System.currentTimeMillis() when data was saved
    val expiresAt: Long                 // lastFetched + TTL
)

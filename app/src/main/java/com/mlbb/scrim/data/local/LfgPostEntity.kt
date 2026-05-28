package com.mlbb.scrim.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Cached LFG post for offline access and reduced API calls.
 */
@Entity(tableName = "cached_lfg_posts")
data class LfgPostEntity(
    @PrimaryKey val id: String,
    val playerId: String,
    val playerName: String,
    val role: String,
    val region: String,
    val skillLevel: String,
    val message: String,
    val mainHeroesJson: String? = null,
    val bio: String? = null,
    val rank: String? = null,
    val totalMatches: Int = 0,
    val winRate: String? = null,
    val rankedWinRate: String? = null,
    val wins: Int = 0,
    val losses: Int = 0,
    val pts: Int = 0,
    val inGameId: String? = null,
    val city: String? = null,
    val screenshotUrl: String? = null,
    val isAvailable: Boolean = true,
    val useMic: Boolean = false,
    val playstyleTagsJson: String? = null,
    val discord: String? = null,
    val telegram: String? = null,
    val vk: String? = null,
    val facebook: String? = null,
    val avatarUrl: String? = null,
    val viewCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val lastUpdated: Long = System.currentTimeMillis()
)

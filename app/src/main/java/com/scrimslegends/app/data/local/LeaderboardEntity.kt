package com.scrimslegends.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Cached leaderboard entry for offline access and reduced API calls.
 */
@Entity(tableName = "cached_leaderboard")
data class LeaderboardEntity(
    @PrimaryKey val odinalRank: Int,
    val userId: String,
    val username: String,
    val avatarUrl: String? = null,
    val pts: Int = 0,
    val wins: Int = 0,
    val losses: Int = 0,
    val matchesPlayed: Int = 0,
    val tier: String = "WARRIOR",
    val lastUpdated: Long = System.currentTimeMillis()
)

package com.mlbb.scrim.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Cached profile information for offline access and faster loading.
 */
@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey val id: String,
    val username: String,
    val fullName: String?,
    val avatarUrl: String?,
    val email: String? = null,
    val inGameId: String? = null,
    val rank: String?,
    val role: String?,
    val bio: String? = null,
    val mainHeroes: String? = null,
    val points: Int = 0,
    val isBanned: Boolean = false,
    val lastUpdated: Long = System.currentTimeMillis()
)

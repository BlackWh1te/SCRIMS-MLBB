package com.mlbb.scrim.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Cached scrim data for offline access and reduced API calls.
 */
@Entity(tableName = "cached_scrims")
data class ScrimEntity(
    @PrimaryKey val id: String,
    val teamId: String,
    val scheduledDate: String,
    val scheduledTime: String,
    val bestOf: Int = 1,
    val status: String = "Open",
    val description: String?,
    val opponentTeamId: String?,
    val opponentTeamName: String?,
    val winnerTeamId: String?,
    val teamAReady: Boolean = false,
    val teamBReady: Boolean = false,
    val teamAReadyAt: String? = null,
    val teamBReadyAt: String? = null,
    val teamAScreenshotUrl: String?,
    val teamBScreenshotUrl: String?,
    val gameMode: String = "RANKED",
    val region: String = "EU",
    val skillLevel: String = "ALL",
    val maxPlayers: Int = 10,
    val currentPlayers: Int = 0,
    val createdAt: String = "",
    val lastUpdated: Long = System.currentTimeMillis()
)

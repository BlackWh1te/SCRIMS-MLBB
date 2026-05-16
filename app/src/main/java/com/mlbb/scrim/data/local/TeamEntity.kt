package com.mlbb.scrim.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Cached team data for offline access and reduced API calls.
 * Stores the full JSON-serialized team DTO to avoid complex multi-table schemas.
 */
@Entity(tableName = "cached_teams")
data class TeamEntity(
    @PrimaryKey val id: String,
    val name: String,
    val leaderId: String,
    val description: String?,
    val minPlayers: Int = 5,
    val maxPlayers: Int = 7,
    val completedScrims: Int = 0,
    val reputation: Float = 5.0f,
    val noShows: Int = 0,
    /** JSON-serialized list of player IDs for quick member lookup */
    val memberIdsJson: String? = null,
    val lastUpdated: Long = System.currentTimeMillis()
)

package com.scrimslegends.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Cached tournament data for offline access.
 */
@Entity(tableName = "cached_tournaments")
data class TournamentEntity(
    @PrimaryKey val id: String,
    val hostUserId: String,
    val hostUsername: String,
    val title: String,
    val description: String,
    val logoUrl: String?,
    val prizeType: String,
    val prizeDescription: String?,
    val maxTeams: Int,
    val minTeamSize: Int,
    val bestOf: Int,
    val region: String,
    val skillLevel: String,
    val swissRounds: Int?,
    val currentRound: Int,
    val status: String,
    val registrationDeadline: Long,
    val checkInDeadline: Long,
    val isLiveStreamEnabled: Boolean,
    val isFlagged: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    /** JSON-serialized List<TournamentRequirement> */
    val requirementsJson: String?,
    val teamCount: Int,
    val hostTrustScore: Double
)

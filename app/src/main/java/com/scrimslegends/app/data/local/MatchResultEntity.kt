package com.scrimslegends.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Cached match result data for offline access and reduced API calls.
 */
@Entity(tableName = "cached_match_results")
data class MatchResultEntity(
    @PrimaryKey val id: String,
    val scrimId: String,
    val teamAId: String,
    val teamAName: String,
    val teamBId: String,
    val teamBName: String,
    /** JSON-serialized TeamReport */
    val teamAReportJson: String?,
    /** JSON-serialized TeamReport */
    val teamBReportJson: String?,
    val screenshotUrl: String?,
    val verificationStatus: String,
    val confirmedWinnerId: String?,
    val adminNotes: String?,
    val createdAt: Long,
    val resolvedAt: Long?,
    /** JSON-serialized List<RosterPlayerInfo> */
    val teamARosterJson: String?,
    /** JSON-serialized List<RosterPlayerInfo> */
    val teamBRosterJson: String?,
    val adminVerdict: String?,
    val punishedTeamId: String?,
    val punishmentDurationHours: Int,
    val reviewedByAdminId: String?,
    val reviewedAt: Long?,
    val noShowTeamId: String?,
    val matchActuallyPlayed: Boolean,
    val matchType: String,
    val tournamentTitle: String?,
    val roundNumber: Int?
)

package com.scrimslegends.app.data.repository

import com.scrimslegends.app.data.model.MatchResult
import kotlinx.coroutines.flow.Flow

interface MatchResultRepositoryInterface {
    suspend fun getAllMatchResults(): Flow<Result<List<MatchResult>>>
    suspend fun getMatchResultById(id: String): Flow<Result<MatchResult?>>
    suspend fun getMatchResultsForScrim(scrimId: String): Flow<Result<MatchResult?>>
    suspend fun getMatchResultsForTeam(teamId: String): Flow<Result<List<MatchResult>>>
    suspend fun reportResult(
        scrimId: String,
        teamId: String,
        reporterId: String,
        reporterName: String,
        reportedWinnerId: String,
        notes: String? = null,
        screenshotUrl: String? = null
    ): Flow<Result<MatchResult>>
    suspend fun createMatchResult(
        scrimId: String,
        teamAId: String,
        teamAName: String,
        teamBId: String,
        teamBName: String
    ): Flow<Result<MatchResult>>
    suspend fun resolveDispute(
        matchResultId: String,
        confirmedWinnerId: String,
        adminNotes: String? = null
    ): Flow<Result<MatchResult>>
    suspend fun uploadScreenshot(
        matchResultId: String,
        screenshotUrl: String
    ): Flow<Result<MatchResult>>
}

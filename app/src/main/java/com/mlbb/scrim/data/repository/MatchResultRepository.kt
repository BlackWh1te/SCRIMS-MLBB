package com.mlbb.scrim.data.repository

import com.mlbb.scrim.data.model.MatchResult
import com.mlbb.scrim.data.model.TeamReport
import com.mlbb.scrim.data.model.VerificationStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class MatchResultRepository {

    private val matchResults = mutableListOf<MatchResult>()

    init {
        matchResults.addAll(
            listOf(
                MatchResult(
                    id = java.util.UUID.randomUUID().toString(),
                    scrimId = "scrim1",
                    teamAId = "team1",
                    teamAName = "Elite Squad",
                    teamBId = "team2",
                    teamBName = "Phoenix Rising",
                    teamAReport = TeamReport(
                        reporterId = "player1",
                        reporterName = "EliteLeader",
                        reportedWinnerId = "team1",
                        reportedAt = System.currentTimeMillis() - 86400000,
                        notes = "Clean sweep 2-0"
                    ),
                    teamBReport = TeamReport(
                        reporterId = "player2",
                        reporterName = "PhoenixLeader",
                        reportedWinnerId = "team1",
                        reportedAt = System.currentTimeMillis() - 86000000,
                        notes = "They outplayed us"
                    ),
                    verificationStatus = VerificationStatus.CONFIRMED,
                    confirmedWinnerId = "team1",
                    createdAt = System.currentTimeMillis() - 86400000,
                    resolvedAt = System.currentTimeMillis() - 86000000
                ),
                MatchResult(
                    id = java.util.UUID.randomUUID().toString(),
                    scrimId = "scrim2",
                    teamAId = "team3",
                    teamAName = "Shadow Wolves",
                    teamBId = "team4",
                    teamBName = "Cyber Legion",
                    teamAReport = TeamReport(
                        reporterId = "player3",
                        reporterName = "WolfLeader",
                        reportedWinnerId = "team3",
                        reportedAt = System.currentTimeMillis() - 43200000,
                        notes = "We won the final teamfight"
                    ),
                    teamBReport = TeamReport(
                        reporterId = "player4",
                        reporterName = "CyberLeader",
                        reportedWinnerId = "team4",
                        reportedAt = System.currentTimeMillis() - 43000000,
                        notes = "Actually we won, their MVP was AFK last fight"
                    ),
                    verificationStatus = VerificationStatus.DISPUTED,
                    createdAt = System.currentTimeMillis() - 86400000
                ),
                MatchResult(
                    id = java.util.UUID.randomUUID().toString(),
                    scrimId = "scrim3",
                    teamAId = "team1",
                    teamAName = "Elite Squad",
                    teamBId = "team5",
                    teamBName = "Nova Gaming",
                    teamAReport = TeamReport(
                        reporterId = "player1",
                        reporterName = "EliteLeader",
                        reportedWinnerId = "team5",
                        reportedAt = System.currentTimeMillis() - 18000000,
                        notes = "They carried hard"
                    ),
                    verificationStatus = VerificationStatus.PENDING,
                    createdAt = System.currentTimeMillis() - 86400000
                )
            )
        )
    }

    suspend fun getAllMatchResults(): Flow<Result<List<MatchResult>>> = flow {
        kotlinx.coroutines.delay(300)
        emit(Result.success(matchResults.toList().sortedByDescending { it.createdAt }))
    }

    suspend fun getMatchResultById(id: String): Flow<Result<MatchResult?>> = flow {
        kotlinx.coroutines.delay(300)
        emit(Result.success(matchResults.find { it.id == id }))
    }

    suspend fun getMatchResultsForScrim(scrimId: String): Flow<Result<MatchResult?>> = flow {
        kotlinx.coroutines.delay(300)
        emit(Result.success(matchResults.find { it.scrimId == scrimId }))
    }

    suspend fun getMatchResultsForTeam(teamId: String): Flow<Result<List<MatchResult>>> = flow {
        kotlinx.coroutines.delay(300)
        val results = matchResults.filter {
            it.teamAId == teamId || it.teamBId == teamId
        }.sortedByDescending { it.createdAt }
        emit(Result.success(results))
    }

    suspend fun reportResult(
        matchResultId: String,
        teamId: String,
        reporterId: String,
        reporterName: String,
        reportedWinnerId: String,
        notes: String? = null,
        screenshotUrl: String? = null
    ): Flow<Result<MatchResult>> = flow {
        kotlinx.coroutines.delay(500)

        val index = matchResults.indexOfFirst { it.id == matchResultId }
        if (index == -1) {
            emit(Result.failure(Exception("Match result not found")))
            return@flow
        }

        val matchResult = matchResults[index]
        val report = TeamReport(
            reporterId = reporterId,
            reporterName = reporterName,
            reportedWinnerId = reportedWinnerId,
            notes = notes
        )

        val updatedResult = when (teamId) {
            matchResult.teamAId -> {
                val newResult = matchResult.copy(
                    teamAReport = report,
                    screenshotUrl = screenshotUrl ?: matchResult.screenshotUrl
                )
                newResult
            }
            matchResult.teamBId -> {
                val newResult = matchResult.copy(
                    teamBReport = report,
                    screenshotUrl = screenshotUrl ?: matchResult.screenshotUrl
                )
                newResult
            }
            else -> {
                emit(Result.failure(Exception("Team is not part of this match")))
                return@flow
            }
        }

        val finalResult = if (
            updatedResult.teamAReport != null &&
            updatedResult.teamBReport != null
        ) {
            if (updatedResult.teamAReport.reportedWinnerId == updatedResult.teamBReport.reportedWinnerId) {
                updatedResult.copy(
                    verificationStatus = VerificationStatus.CONFIRMED,
                    confirmedWinnerId = updatedResult.teamAReport.reportedWinnerId,
                    resolvedAt = System.currentTimeMillis()
                )
            } else {
                updatedResult.copy(
                    verificationStatus = VerificationStatus.DISPUTED
                )
            }
        } else {
            updatedResult
        }

        matchResults[index] = finalResult
        emit(Result.success(finalResult))
    }

    suspend fun createMatchResult(
        scrimId: String,
        teamAId: String,
        teamAName: String,
        teamBId: String,
        teamBName: String
    ): Flow<Result<MatchResult>> = flow {
        kotlinx.coroutines.delay(500)

        val matchResult = MatchResult(
            id = java.util.UUID.randomUUID().toString(),
            scrimId = scrimId,
            teamAId = teamAId,
            teamAName = teamAName,
            teamBId = teamBId,
            teamBName = teamBName
        )
        matchResults.add(matchResult)
        emit(Result.success(matchResult))
    }

    suspend fun resolveDispute(
        matchResultId: String,
        confirmedWinnerId: String,
        adminNotes: String? = null
    ): Flow<Result<MatchResult>> = flow {
        kotlinx.coroutines.delay(500)

        val index = matchResults.indexOfFirst { it.id == matchResultId }
        if (index == -1) {
            emit(Result.failure(Exception("Match result not found")))
            return@flow
        }

        val updated = matchResults[index].copy(
            verificationStatus = VerificationStatus.CONFIRMED,
            confirmedWinnerId = confirmedWinnerId,
            adminNotes = adminNotes,
            resolvedAt = System.currentTimeMillis()
        )
        matchResults[index] = updated
        emit(Result.success(updated))
    }

    suspend fun uploadScreenshot(
        matchResultId: String,
        screenshotUrl: String
    ): Flow<Result<MatchResult>> = flow {
        kotlinx.coroutines.delay(500)

        val index = matchResults.indexOfFirst { it.id == matchResultId }
        if (index == -1) {
            emit(Result.failure(Exception("Match result not found")))
            return@flow
        }

        val updated = matchResults[index].copy(screenshotUrl = screenshotUrl)
        matchResults[index] = updated
        emit(Result.success(updated))
    }
}

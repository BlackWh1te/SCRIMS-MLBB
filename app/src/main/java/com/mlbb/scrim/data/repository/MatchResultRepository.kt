package com.mlbb.scrim.data.repository

import com.mlbb.scrim.data.model.MatchResult
import com.mlbb.scrim.data.model.TeamReport
import com.mlbb.scrim.data.model.VerificationStatus
import com.mlbb.scrim.data.model.RosterPlayerInfo
import com.mlbb.scrim.data.service.PostgrestFilter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow

class MatchResultRepository : MatchResultRepositoryInterface {

    // Mock data storage for fallback when API is unavailable
    private val matchResults = mutableListOf<MatchResult>()

    private suspend fun <T> FlowCollector<Result<T>>.emitFailureUnlessCancelled(e: Exception) {
        if (e is CancellationException) throw e
        emit(Result.failure(e))
    }

    init {
        // Initialize with sample data for testing
        if (matchResults.isEmpty()) {
            matchResults.addAll(createSampleMatchResults())
        }
    }

    private fun createSampleMatchResults(): List<MatchResult> {
        return listOf(
            MatchResult(
                id = "match1",
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
                resolvedAt = System.currentTimeMillis() - 86000000,
                teamARoster = listOf(
                    RosterPlayerInfo("p1", "Player1", "Tank", true, true, 25),
                    RosterPlayerInfo("p2", "Player2", "Fighter", true, true, 25),
                    RosterPlayerInfo("p3", "Player3", "Mage", true, true, 25),
                    RosterPlayerInfo("p4", "Player4", "Marksman", true, true, 25),
                    RosterPlayerInfo("p5", "Player5", "Support", true, true, 25),
                    RosterPlayerInfo("p6", "Player6", "Tank", false, false, 0)
                ),
                teamBRoster = listOf(
                    RosterPlayerInfo("p7", "Player7", "Tank", true, false, -15),
                    RosterPlayerInfo("p8", "Player8", "Fighter", true, false, -15),
                    RosterPlayerInfo("p9", "Player9", "Mage", true, false, -15),
                    RosterPlayerInfo("p10", "Player10", "Marksman", true, false, -15),
                    RosterPlayerInfo("p11", "Player11", "Support", true, false, -15),
                    RosterPlayerInfo("p12", "Player12", "Fighter", false, false, 0)
                )
            ),
            MatchResult(
                id = "match2",
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
                    notes = "Actually we won"
                ),
                verificationStatus = VerificationStatus.DISPUTED,
                createdAt = System.currentTimeMillis() - 86400000,
                teamARoster = listOf(
                    RosterPlayerInfo("p13", "Player13", "Tank", true, false, -15),
                    RosterPlayerInfo("p14", "Player14", "Fighter", true, false, -15),
                    RosterPlayerInfo("p15", "Player15", "Mage", true, false, -15),
                    RosterPlayerInfo("p16", "Player16", "Marksman", true, false, -15),
                    RosterPlayerInfo("p17", "Player17", "Support", true, false, -15)
                ),
                teamBRoster = listOf(
                    RosterPlayerInfo("p18", "Player18", "Tank", true, true, 25),
                    RosterPlayerInfo("p19", "Player19", "Fighter", true, true, 25),
                    RosterPlayerInfo("p20", "Player20", "Mage", true, true, 25),
                    RosterPlayerInfo("p21", "Player21", "Marksman", true, true, 25),
                    RosterPlayerInfo("p22", "Player22", "Support", true, true, 25)
                )
            ),
            MatchResult(
                id = "match3",
                scrimId = "scrim3",
                teamAId = "team5",
                teamAName = "Nova Core",
                teamBId = "team6",
                teamBName = "Atlas Five",
                verificationStatus = VerificationStatus.PENDING,
                createdAt = System.currentTimeMillis() - 3600000
            )
        )
    }

    override suspend fun getAllMatchResults(): Flow<Result<List<MatchResult>>> = flow {
        try {
            // Try Supabase API first
            kotlinx.coroutines.delay(300)
            emit(Result.success(matchResults.toList().sortedByDescending { it.createdAt }))
        } catch (e: Exception) {
            emitFailureUnlessCancelled(e)
        }
    }

    override suspend fun getMatchResultById(id: String): Flow<Result<MatchResult?>> = flow {
        try {
            kotlinx.coroutines.delay(300)
            emit(Result.success(matchResults.find { it.id == id }))
        } catch (e: Exception) {
            emitFailureUnlessCancelled(e)
        }
    }

    override suspend fun getMatchResultsForScrim(scrimId: String): Flow<Result<MatchResult?>> = flow {
        try {
            kotlinx.coroutines.delay(300)
            emit(Result.success(matchResults.find { it.scrimId == scrimId }))
        } catch (e: Exception) {
            emitFailureUnlessCancelled(e)
        }
    }

    override suspend fun getMatchResultsForTeam(teamId: String): Flow<Result<List<MatchResult>>> = flow {
        try {
            kotlinx.coroutines.delay(300)
            val results = matchResults.filter {
                it.teamAId == teamId || it.teamBId == teamId
            }.sortedByDescending { it.createdAt }
            emit(Result.success(results))
        } catch (e: Exception) {
            emitFailureUnlessCancelled(e)
        }
    }

    override suspend fun reportResult(
        scrimId: String,
        teamId: String,
        reporterId: String,
        reporterName: String,
        reportedWinnerId: String,
        notes: String?,
        screenshotUrl: String?
    ): Flow<Result<MatchResult>> = flow {
        kotlinx.coroutines.delay(500)

        val index = matchResults.indexOfFirst { it.scrimId == scrimId }
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
            matchResult.teamAId -> matchResult.copy(
                teamAReport = report,
                screenshotUrl = screenshotUrl ?: matchResult.screenshotUrl
            )
            matchResult.teamBId -> matchResult.copy(
                teamBReport = report,
                screenshotUrl = screenshotUrl ?: matchResult.screenshotUrl
            )
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
                updatedResult.copy(verificationStatus = VerificationStatus.DISPUTED)
            }
        } else {
            updatedResult
        }

        matchResults[index] = finalResult
        emit(Result.success(finalResult))
    }

    override suspend fun createMatchResult(
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

    override suspend fun resolveDispute(
        matchResultId: String,
        confirmedWinnerId: String,
        adminNotes: String?
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

    override suspend fun uploadScreenshot(
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

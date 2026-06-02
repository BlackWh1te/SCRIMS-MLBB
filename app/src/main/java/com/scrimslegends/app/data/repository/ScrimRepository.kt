package com.scrimslegends.app.data.repository

import com.scrimslegends.app.data.model.ApplicationStatus
import com.scrimslegends.app.data.model.BestOf
import com.scrimslegends.app.data.model.Scrim
import com.scrimslegends.app.data.model.ScrimApplication
import com.scrimslegends.app.data.model.ScrimRosterEntry
import com.scrimslegends.app.data.model.ScrimStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf

/**
 * Scrim repository managing scrim postings, applications, rosters, and match results.
 *
 * Current implementation: In-memory mock for UI development.
 * Next step: Integrate with Supabase (table: scrims, scrim_applications, scrim_rosters).
 */
class ScrimRepository : ScrimRepositoryInterface {

    private val scrims = mutableListOf<Scrim>()

    // Points configuration
    companion object {
        const val PTS_PER_WIN = 25
        const val PTS_PER_LOSS = 15
    }

    init {
        // Add some sample scrims for testing
        scrims.addAll(
            listOf(
                Scrim(
                    id = java.util.UUID.randomUUID().toString(),
                    teamId = "team1",
                    teamName = "Elite Squad",
                    teamLeader = "player1",
                    bestOf = BestOf.BO3,
                    region = com.scrimslegends.app.data.model.Region.EU,
                    skillLevel = com.scrimslegends.app.data.model.SkillLevel.ADVANCED,
                    scheduledTime = System.currentTimeMillis() + 3600000,
                    status = com.scrimslegends.app.data.model.ScrimStatus.OPEN,
                    description = "Looking for a BO3 scrim"
                ),
                Scrim(
                    id = java.util.UUID.randomUUID().toString(),
                    teamId = "team2",
                    teamName = "Phoenix Rising",
                    teamLeader = "player2",
                    bestOf = BestOf.BO1,
                    region = com.scrimslegends.app.data.model.Region.NA,
                    skillLevel = com.scrimslegends.app.data.model.SkillLevel.INTERMEDIATE,
                    scheduledTime = System.currentTimeMillis() + 7200000,
                    status = com.scrimslegends.app.data.model.ScrimStatus.OPEN,
                    description = "BO1 custom scrim, all welcome"
                ),
                Scrim(
                    id = java.util.UUID.randomUUID().toString(),
                    teamId = "team3",
                    teamName = "Moscow Wolves",
                    teamLeader = "player3",
                    bestOf = BestOf.BO5,
                    region = com.scrimslegends.app.data.model.Region.MSK,
                    skillLevel = com.scrimslegends.app.data.model.SkillLevel.PRO,
                    scheduledTime = System.currentTimeMillis() + 86400000,
                    status = com.scrimslegends.app.data.model.ScrimStatus.OPEN,
                    description = "BO5 practice match tomorrow"
                ),
                Scrim(
                    id = java.util.UUID.randomUUID().toString(),
                    teamId = "team4",
                    teamName = "Night Owls",
                    teamLeader = "player4",
                    bestOf = BestOf.BO1,
                    region = com.scrimslegends.app.data.model.Region.ASIA,
                    skillLevel = com.scrimslegends.app.data.model.SkillLevel.ALL,
                    scheduledTime = System.currentTimeMillis() + 18000000,
                    status = com.scrimslegends.app.data.model.ScrimStatus.OPEN,
                    description = "Late night BO1 scrim"
                ),
                Scrim(
                    id = java.util.UUID.randomUUID().toString(),
                    teamId = "team5",
                    teamName = "Apex Predators",
                    teamLeader = "player5",
                    bestOf = BestOf.BO3,
                    region = com.scrimslegends.app.data.model.Region.NA,
                    skillLevel = com.scrimslegends.app.data.model.SkillLevel.ADVANCED,
                    scheduledTime = System.currentTimeMillis() + 172800000,
                    status = com.scrimslegends.app.data.model.ScrimStatus.FILLED,
                    description = "BO3 tournament practice",
                    opponentTeamId = "team6",
                    opponentTeamName = "Storm Riders"
                ),
                Scrim(
                    id = java.util.UUID.randomUUID().toString(),
                    teamId = "team6",
                    teamName = "Storm Riders",
                    teamLeader = "player6",
                    bestOf = BestOf.BO1,
                    region = com.scrimslegends.app.data.model.Region.EU,
                    skillLevel = com.scrimslegends.app.data.model.SkillLevel.INTERMEDIATE,
                    scheduledTime = System.currentTimeMillis() + 259200000,
                    status = com.scrimslegends.app.data.model.ScrimStatus.OPEN,
                    description = "Weekend BO1 scrim"
                )
            )
        )
    }

    override fun getAllScrims(page: Int, pageSize: Int): Flow<Result<List<Scrim>>> = flowOf(Result.success(scrims.toList()))

    override fun getScrimById(id: String): Flow<Result<Scrim?>> = flowOf(Result.success(scrims.find { it.id == id }))

    override fun getScrimsByTeam(teamId: String): Flow<Result<List<Scrim>>> =
        flowOf(Result.success(scrims.filter { it.teamId == teamId || it.opponentTeamId == teamId }))

    override fun searchScrims(
        query: String,
        gameMode: com.scrimslegends.app.data.model.GameMode?,
        region: com.scrimslegends.app.data.model.Region?,
        skillLevel: com.scrimslegends.app.data.model.SkillLevel?,
        status: com.scrimslegends.app.data.model.ScrimStatus?
    ): Flow<Result<List<Scrim>>> {
        val queryLower = query.lowercase().trim()
        val results = scrims.filter { scrim ->
            val matchesText = queryLower.isEmpty() ||
                scrim.teamName.lowercase().contains(queryLower) ||
                scrim.description.lowercase().contains(queryLower)
            matchesText &&
            (gameMode == null || scrim.gameMode == gameMode) &&
            (region == null || scrim.region == region) &&
            (skillLevel == null || scrim.skillLevel == skillLevel) &&
            (status == null || scrim.status == status)
        }
        return flowOf(Result.success(results))
    }

    override suspend fun createScrim(scrim: Scrim): Flow<Result<Scrim>> = flow {
        delay(500)
        val newScrim = scrim.copy(id = java.util.UUID.randomUUID().toString())
        scrims.add(newScrim)
        emit(Result.success(newScrim))
    }

    override suspend fun updateScrim(scrim: Scrim): Flow<Result<Scrim>> = flow {
        delay(500)
        val index = scrims.indexOfFirst { it.id == scrim.id }
        if (index != -1) {
            scrims[index] = scrim
            emit(Result.success(scrim))
        } else {
            emit(Result.failure(Exception("Scrim not found")))
        }
    }

    override suspend fun deleteScrim(id: String): Flow<Result<Unit>> = flow {
        delay(500)
        val removed = scrims.removeIf { it.id == id }
        if (removed) {
            emit(Result.success(Unit))
        } else {
            emit(Result.failure(Exception("Scrim not found")))
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // TEAM VS TEAM APPLICATION FLOW
    // ═══════════════════════════════════════════════════════════════

    override suspend fun applyToScrim(scrimId: String, application: ScrimApplication): Flow<Result<Scrim>> = flow {
        delay(500)
        val index = scrims.indexOfFirst { it.id == scrimId }
        if (index != -1) {
            val scrim = scrims[index]
            if (scrim.status != ScrimStatus.OPEN && scrim.status != ScrimStatus.PENDING) {
                emit(Result.failure(Exception("Scrim is no longer open for applications")))
                return@flow
            }
            val newApp = application.copy(id = java.util.UUID.randomUUID().toString())
            val updatedApps = scrim.applications + newApp
            scrims[index] = scrim.copy(applications = updatedApps)
            emit(Result.success(scrims[index]))
        } else {
            emit(Result.failure(Exception("Scrim not found")))
        }
    }

    override suspend fun approveApplication(scrimId: String, applicationId: String, conversationId: String): Flow<Result<Scrim>> = flow {
        delay(500)
        val index = scrims.indexOfFirst { it.id == scrimId }
        if (index != -1) {
            val scrim = scrims[index]
            val app = scrim.applications.find { it.id == applicationId }
            if (app == null) {
                emit(Result.failure(Exception("Application not found")))
                return@flow
            }
            val updatedApps = scrim.applications.map {
                if (it.id == applicationId) it.copy(status = ApplicationStatus.APPROVED, respondedAt = System.currentTimeMillis())
                else if (it.status == ApplicationStatus.PENDING) it.copy(status = ApplicationStatus.CANCELLED)
                else it
            }
            scrims[index] = scrim.copy(
                status = ScrimStatus.FILLED,
                opponentTeamId = app.applicantTeamId,
                opponentTeamName = app.applicantTeamName,
                opponentTeamLeader = app.applicantTeamLeader,
                applications = updatedApps,
                conversationId = conversationId
            )
            emit(Result.success(scrims[index]))
        } else {
            emit(Result.failure(Exception("Scrim not found")))
        }
    }

    override suspend fun rejectApplication(scrimId: String, applicationId: String): Flow<Result<Scrim>> = flow {
        delay(500)
        val index = scrims.indexOfFirst { it.id == scrimId }
        if (index != -1) {
            val scrim = scrims[index]
            val updatedApps = scrim.applications.map {
                if (it.id == applicationId) it.copy(status = ApplicationStatus.REJECTED, respondedAt = System.currentTimeMillis())
                else it
            }
            scrims[index] = scrim.copy(applications = updatedApps)
            emit(Result.success(scrims[index]))
        } else {
            emit(Result.failure(Exception("Scrim not found")))
        }
    }

    override suspend fun cancelApplication(scrimId: String, applicationId: String): Flow<Result<Scrim>> = flow {
        delay(500)
        val index = scrims.indexOfFirst { it.id == scrimId }
        if (index != -1) {
            val scrim = scrims[index]
            val updatedApps = scrim.applications.map {
                if (it.id == applicationId) it.copy(status = ApplicationStatus.CANCELLED)
                else it
            }
            scrims[index] = scrim.copy(applications = updatedApps)
            emit(Result.success(scrims[index]))
        } else {
            emit(Result.failure(Exception("Scrim not found")))
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // SCRIM ROSTER — Captain assigns active/substitute players
    // ═══════════════════════════════════════════════════════════════

    /** Captain sets the roster for their team in a scrim */
    override suspend fun setScrimRoster(
        scrimId: String,
        teamId: String,
        roster: List<ScrimRosterEntry>
    ): Flow<Result<Scrim>> = flow {
        delay(500)
        val index = scrims.indexOfFirst { it.id == scrimId }
        if (index == -1) {
            emit(Result.failure(Exception("Scrim not found")))
            return@flow
        }
        val scrim = scrims[index]
        val isTeamA = scrim.teamId == teamId

        // Validate: minimum 5 active players
        val activeCount = roster.count { it.isActive }
        if (activeCount < 5) {
            emit(Result.failure(Exception("Minimum 5 active players required for scrim")))
            return@flow
        }

        val updatedScrim = if (isTeamA) {
            scrim.copy(teamARoster = roster)
        } else {
            scrim.copy(teamBRoster = roster)
        }
        scrims[index] = updatedScrim
        emit(Result.success(updatedScrim))
    }

    // ═══════════════════════════════════════════════════════════════
    // READY FLOW — Captains press Ready at match start time
    // ═══════════════════════════════════════════════════════════════

    /** Transition scrim to READY_CHECK status at match time */
    override suspend fun transitionToReadyCheck(scrimId: String): Flow<Result<Scrim>> = flow {
        delay(300)
        val index = scrims.indexOfFirst { it.id == scrimId }
        if (index == -1) {
            emit(Result.failure(Exception("Scrim not found")))
            return@flow
        }
        val scrim = scrims[index]
        if (scrim.status != ScrimStatus.FILLED) {
            emit(Result.failure(Exception("Scrim must be in FILLED status to start ready check")))
            return@flow
        }
        scrims[index] = scrim.copy(status = ScrimStatus.READY_CHECK)
        emit(Result.success(scrims[index]))
    }

    /** Captain presses Ready button */
    override suspend fun markReady(scrimId: String, teamId: String): Flow<Result<Scrim>> = flow {
        delay(300)
        val index = scrims.indexOfFirst { it.id == scrimId }
        if (index == -1) {
            emit(Result.failure(Exception("Scrim not found")))
            return@flow
        }
        val scrim = scrims[index]
        val isTeamA = scrim.teamId == teamId

        val updatedScrim = if (isTeamA) {
            scrim.copy(teamAReady = true, teamAReadyAt = System.currentTimeMillis())
        } else {
            scrim.copy(teamBReady = true, teamBReadyAt = System.currentTimeMillis())
        }

        // If both ready, transition to IN_PROGRESS
        val finalScrim = if (updatedScrim.teamAReady && updatedScrim.teamBReady) {
            updatedScrim.copy(status = ScrimStatus.IN_PROGRESS)
        } else {
            updatedScrim
        }

        scrims[index] = finalScrim
        emit(Result.success(finalScrim))
    }

    // ═══════════════════════════════════════════════════════════════
    // SCREENSHOT FLOW — Attach screenshot before completing
    // ═══════════════════════════════════════════════════════════════

    /** Captain uploads a screenshot */
    override suspend fun uploadScreenshot(scrimId: String, teamId: String, screenshotUrl: String): Flow<Result<Scrim>> = flow {
        delay(500)
        val index = scrims.indexOfFirst { it.id == scrimId }
        if (index == -1) {
            emit(Result.failure(Exception("Scrim not found")))
            return@flow
        }
        val scrim = scrims[index]
        if (scrim.status != ScrimStatus.IN_PROGRESS) {
            emit(Result.failure(Exception("Scrim must be in progress to upload screenshot")))
            return@flow
        }
        val isTeamA = scrim.teamId == teamId

        val updatedScrim = if (isTeamA) {
            scrim.copy(
                teamAScreenshotUrl = screenshotUrl,
                teamAScreenshotUploadedAt = System.currentTimeMillis()
            )
        } else {
            scrim.copy(
                teamBScreenshotUrl = screenshotUrl,
                teamBScreenshotUploadedAt = System.currentTimeMillis()
            )
        }
        scrims[index] = updatedScrim
        emit(Result.success(updatedScrim))
    }

    override suspend fun uploadGameScreenshot(scrimId: String, teamId: String, gameNumber: Int, screenshotUrl: String): Flow<Result<Scrim>> = flow {
        delay(500)
        val index = scrims.indexOfFirst { it.id == scrimId }
        if (index == -1) { emit(Result.failure(Exception("Scrim not found"))); return@flow }
        val scrim = scrims[index]
        val isTeamA = scrim.teamId == teamId
        val updatedGames = scrim.gameResults.map { game ->
            if (game.gameNumber == gameNumber) {
                if (isTeamA) game.copy(teamAScreenshotUrl = screenshotUrl, teamAScreenshotUploadedAt = System.currentTimeMillis())
                else game.copy(teamBScreenshotUrl = screenshotUrl, teamBScreenshotUploadedAt = System.currentTimeMillis())
            } else game
        }
        scrims[index] = scrim.copy(gameResults = updatedGames)
        emit(Result.success(scrims[index]))
    }

    override suspend fun selectGameWinner(scrimId: String, gameNumber: Int, winnerTeamId: String): Flow<Result<Scrim>> = flow {
        delay(300)
        val index = scrims.indexOfFirst { it.id == scrimId }
        if (index == -1) { emit(Result.failure(Exception("Scrim not found"))); return@flow }
        val scrim = scrims[index]
        val updatedGames = scrim.gameResults.map { game ->
            if (game.gameNumber == gameNumber) game.copy(winnerTeamId = winnerTeamId) else game
        }
        scrims[index] = scrim.copy(gameResults = updatedGames)
        emit(Result.success(scrims[index]))
    }

    // ═══════════════════════════════════════════════════════════════
    // COMPLETE SCRIM — Select winner, award/deduct points
    // ═══════════════════════════════════════════════════════════════

    /** Change series format mid-series (mock) */
    override suspend fun changeSeriesFormat(scrimId: String, newBestOf: Int): Flow<Result<Scrim>> = flow {
        delay(300)
        val index = scrims.indexOfFirst { it.id == scrimId }
        if (index == -1) {
            emit(Result.failure(Exception("Scrim not found")))
            return@flow
        }
        val scrim = scrims[index]
        val newBestOfEnum = BestOf.fromGames(newBestOf)
        scrims[index] = scrim.copy(bestOf = newBestOfEnum, gameResults = scrim.gameResults.take(newBestOf))
        emit(Result.success(scrims[index]))
    }

    /** Complete scrim: must have screenshot uploaded, select winner */
    override suspend fun completeScrim(scrimId: String, winnerTeamId: String?): Flow<Result<Scrim>> = flow {
        delay(500)
        val index = scrims.indexOfFirst { it.id == scrimId }
        if (index == -1) {
            emit(Result.failure(Exception("Scrim not found")))
            return@flow
        }
        val scrim = scrims[index]

        // Validate: must have at least one screenshot
        if (scrim.teamAScreenshotUrl == null && scrim.teamBScreenshotUrl == null) {
            emit(Result.failure(Exception("At least one screenshot must be uploaded before completing")))
            return@flow
        }

        // Validate: winner must be one of the two teams
        if (winnerTeamId != scrim.teamId && winnerTeamId != scrim.opponentTeamId) {
            emit(Result.failure(Exception("Winner must be one of the participating teams")))
            return@flow
        }

        val completedScrim = scrim.copy(
            status = ScrimStatus.COMPLETED,
            winnerTeamId = winnerTeamId,
            resultSubmittedAt = System.currentTimeMillis()
        )
        scrims[index] = completedScrim
        emit(Result.success(completedScrim))
    }

    /** Calculate and return points changes for active roster players */
    override fun calculatePointsChanges(scrim: Scrim): PointsResult {
        val winnerTeamId = scrim.winnerTeamId ?: return PointsResult.empty()

        val teamAChanges = scrim.teamAActiveRoster.map { entry ->
            val isWinner = entry.teamId == winnerTeamId
            PlayerPointsChange(
                playerId = entry.playerId,
                playerName = entry.playerName,
                teamId = entry.teamId,
                pointsChange = if (isWinner) PTS_PER_WIN else -PTS_PER_LOSS,
                isWinner = isWinner
            )
        }

        val teamBChanges = scrim.teamBActiveRoster.map { entry ->
            val isWinner = entry.teamId == winnerTeamId
            PlayerPointsChange(
                playerId = entry.playerId,
                playerName = entry.playerName,
                teamId = entry.teamId,
                pointsChange = if (isWinner) PTS_PER_WIN else -PTS_PER_LOSS,
                isWinner = isWinner
            )
        }

        // Substitutes get 0 points change
        val teamASubs = scrim.teamASubstitutes.map { entry ->
            PlayerPointsChange(
                playerId = entry.playerId,
                playerName = entry.playerName,
                teamId = entry.teamId,
                pointsChange = 0,
                isWinner = false,
                isSubstitute = true
            )
        }
        val teamBSubs = scrim.teamBSubstitutes.map { entry ->
            PlayerPointsChange(
                playerId = entry.playerId,
                playerName = entry.playerName,
                teamId = entry.teamId,
                pointsChange = 0,
                isWinner = false,
                isSubstitute = true
            )
        }

        return PointsResult(
            teamAChanges = teamAChanges,
            teamBChanges = teamBChanges,
            teamASubstitutes = teamASubs,
            teamBSubstitutes = teamBSubs,
            winnerTeamId = winnerTeamId
        )
    }

    override suspend fun submitResult(
        scrimId: String,
        reporterId: String,
        winnerTeamId: String,
        notes: String?,
        screenshotUrl: String?
    ): Flow<Result<Scrim>> = flow {
        delay(500)
        val index = scrims.indexOfFirst { it.id == scrimId }
        if (index != -1) {
            val scrim = scrims[index]
            scrims[index] = scrim.copy(
                status = ScrimStatus.COMPLETED,
                winnerTeamId = winnerTeamId,
                resultSubmittedAt = System.currentTimeMillis()
            )
            emit(Result.success(scrims[index]))
        } else {
            emit(Result.failure(Exception("Scrim not found")))
        }
    }

    override suspend fun createAutoCancelledRecord(scrimId: String): Flow<Result<Unit>> = flow {
        delay(300)
        emit(Result.success(Unit))
    }

    override suspend fun cancelScrim(scrimId: String, reason: String, cancelledBy: String): Flow<Result<Unit>> = flow {
        delay(300)
        val index = scrims.indexOfFirst { it.id == scrimId }
        if (index != -1) {
            val scrim = scrims[index]
            scrims[index] = scrim.copy(
                status = ScrimStatus.CANCELLED,
                cancellationReason = reason,
                cancelledBy = cancelledBy
            )
            emit(Result.success(Unit))
        } else {
            emit(Result.failure(Exception("Scrim not found")))
        }
    }

    override fun subscribeToScrim(scrimId: String): Flow<Scrim> = flow {
        // Mock repository does not support Realtime subscriptions
    }

    override fun subscribeToAllScrims(): Flow<Scrim> = flow {
        // Mock repository does not support Realtime subscriptions
    }
}

/** Result of points calculation for a completed scrim */
data class PointsResult(
    val teamAChanges: List<PlayerPointsChange>,
    val teamBChanges: List<PlayerPointsChange>,
    val teamASubstitutes: List<PlayerPointsChange>,
    val teamBSubstitutes: List<PlayerPointsChange>,
    val winnerTeamId: String
) {
    val allActiveChanges: List<PlayerPointsChange>
        get() = teamAChanges + teamBChanges

    val allSubstitutes: List<PlayerPointsChange>
        get() = teamASubstitutes + teamBSubstitutes

    companion object {
        fun empty() = PointsResult(
            teamAChanges = emptyList(),
            teamBChanges = emptyList(),
            teamASubstitutes = emptyList(),
            teamBSubstitutes = emptyList(),
            winnerTeamId = ""
        )
    }
}

/** Points change for a single player */
data class PlayerPointsChange(
    val playerId: String = "",
    val playerName: String = "",
    val teamId: String = "",
    val pointsChange: Int = 0,      // positive = gain, negative = loss, 0 = substitute
    val isWinner: Boolean = false,
    val isSubstitute: Boolean = false
)

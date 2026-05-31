package com.scrimslegends.app.data.repository

import com.scrimslegends.app.data.cache.UnifiedCacheManager
import com.scrimslegends.app.data.local.ScrimDao
import com.scrimslegends.app.data.local.ScrimEntity
import com.scrimslegends.app.data.model.*
import com.scrimslegends.app.data.service.*
import com.scrimslegends.app.security.AuthorizationUtils
import com.scrimslegends.app.util.DateUtils
import timber.log.Timber
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow

/**
 * Supabase-backed scrim repository with caching.
 * Memory TTL: 2 min | Room TTL: 10 min
 */
class SupabaseScrimRepository(
    private val cacheManager: UnifiedCacheManager,
    private val scrimDao: ScrimDao,
    private val realtimeClient: SupabaseRealtimeClient
) : ScrimRepositoryInterface {

    private val api = SupabaseService.api

    companion object {
        const val PTS_PER_WIN = 25
        const val PTS_PER_LOSS = 15
        private const val CACHE_KEY_ALL = "scrims_all"
        private const val CACHE_KEY_TEAM_PREFIX = "scrims_team_"
        private const val MEM_TTL = 2L * 60 * 1000
        private const val ROOM_TTL = 10L * 60 * 1000
        private const val MAX_RETRY = 3
        private const val RETRY_DELAY_MS = 500L
        private const val PAGE_SIZE = 50

        // ── ScrimStatus ↔ DB string mapping ──
        // DB constraint: valid_scrim_status CHECK (status IN ('Open','Pending','Accepted','Ready','In Progress','Completed','Cancelled'))
        fun toDbStatus(status: ScrimStatus): String = when (status) {
            ScrimStatus.OPEN        -> "Open"
            ScrimStatus.FILLED      -> "Accepted"
            ScrimStatus.READY_CHECK -> "Ready"
            ScrimStatus.IN_PROGRESS -> "In Progress"
            ScrimStatus.COMPLETED   -> "Completed"
            ScrimStatus.CANCELLED  -> "Cancelled"
        }

        fun fromDbStatus(dbStatus: String): ScrimStatus = when (dbStatus) {
            "Open"        -> ScrimStatus.OPEN
            "Pending"     -> ScrimStatus.OPEN   // DB has no direct equivalent; closest semantic match
            "Accepted"    -> ScrimStatus.FILLED
            "Ready"       -> ScrimStatus.READY_CHECK
            "In Progress" -> ScrimStatus.IN_PROGRESS
            "Completed"   -> ScrimStatus.COMPLETED
            "Cancelled"   -> ScrimStatus.CANCELLED
            else          -> ScrimStatus.OPEN
        }

        // ── ApplicationStatus ↔ DB string mapping ──
        // DB constraint: valid_application_status CHECK (status IN ('Pending','Accepted','Rejected','Cancelled'))
        fun toDbApplicationStatus(status: ApplicationStatus): String = when (status) {
            ApplicationStatus.PENDING   -> "Pending"
            ApplicationStatus.APPROVED  -> "Accepted"
            ApplicationStatus.REJECTED  -> "Rejected"
            ApplicationStatus.CANCELLED -> "Cancelled"
        }

        fun fromDbApplicationStatus(dbStatus: String): ApplicationStatus = when (dbStatus) {
            "Pending"    -> ApplicationStatus.PENDING
            "Accepted"   -> ApplicationStatus.APPROVED
            "Rejected"   -> ApplicationStatus.REJECTED
            "Cancelled"  -> ApplicationStatus.CANCELLED
            else         -> ApplicationStatus.PENDING
        }

        // ── ScrimGameStatus ↔ DB string mapping ──
        // DB constraint: valid_game_result_status CHECK (status IN ('Pending','Awaiting Opponent','Both Uploaded','Winner Selected','Disputed','Confirmed'))
        fun toDbGameStatus(status: ScrimGameStatus): String = when (status) {
            ScrimGameStatus.PENDING           -> "Pending"
            ScrimGameStatus.AWAITING_OPPONENT -> "Awaiting Opponent"
            ScrimGameStatus.BOTH_UPLOADED     -> "Both Uploaded"
            ScrimGameStatus.WINNER_SELECTED   -> "Winner Selected"
            ScrimGameStatus.DISPUTED          -> "Disputed"
            ScrimGameStatus.CONFIRMED         -> "Confirmed"
        }

        fun fromDbGameStatus(dbStatus: String): ScrimGameStatus = when (dbStatus) {
            "Pending"           -> ScrimGameStatus.PENDING
            "Awaiting Opponent" -> ScrimGameStatus.AWAITING_OPPONENT
            "Both Uploaded"     -> ScrimGameStatus.BOTH_UPLOADED
            "Winner Selected"   -> ScrimGameStatus.WINNER_SELECTED
            "Disputed"          -> ScrimGameStatus.DISPUTED
            "Confirmed"         -> ScrimGameStatus.CONFIRMED
            else                -> ScrimGameStatus.PENDING
        }
    }

    /**
     * Retry a suspend block with exponential backoff.
     */
    private suspend fun <T> withRetry(block: suspend () -> T): T {
        var lastException: Exception? = null
        for (attempt in 1..MAX_RETRY) {
            try {
                return block()
            } catch (e: Exception) {
                lastException = e
                if (attempt < MAX_RETRY) delay(RETRY_DELAY_MS * attempt)
            }
        }
        throw lastException ?: Exception("Retry exhausted")
    }

    private suspend fun invalidateScrimCaches() {
        cacheManager.invalidateByPrefix("scrims_")
    }

    override fun getAllScrims(): Flow<Result<List<Scrim>>> = flow {
        try {
            cacheManager.getFlow<List<Scrim>>(
                key = SupabaseSession.userScopedKey(CACHE_KEY_ALL), memoryTtlMs = MEM_TTL, roomTtlMs = ROOM_TTL,
                roomLoader = {
                    val c = scrimDao.getAll()
                    if (c.isNotEmpty()) c.map { mapEntityToScrim(it) } else null
                },
                networkLoader = {
                    // TODO: implement proper pagination (offset/limit) when UI supports it
                    // Cap at 200 rows to avoid unbounded data transfer on initial load
                    val r = api.getScrims(range = "0-199")
                    if (r.isSuccessful) r.body()?.map { mapDtoToScrim(it) } ?: emptyList()
                    else throw Exception("Failed to fetch scrims")
                },
                roomSaver = { list ->
                    scrimDao.deleteAll()
                    scrimDao.insertAll(list.map { mapScrimToEntity(it) })
                }
            ).collect { scrims ->
                emit(Result.success(scrims))
            }
        } catch (e: Exception) {
            val c = scrimDao.getAll()
            if (c.isNotEmpty()) emit(Result.success(c.map { mapEntityToScrim(it) }))
            else emit(Result.failure(e))
        }
    }

    override fun getScrimsByTeam(teamId: String): Flow<Result<List<Scrim>>> = flow {
        try {
            val key = "$CACHE_KEY_TEAM_PREFIX$teamId"
            cacheManager.getFlow<List<Scrim>>(
                key = key, memoryTtlMs = MEM_TTL, roomTtlMs = ROOM_TTL,
                roomLoader = {
                    val c = scrimDao.getByTeam(teamId)
                    if (c.isNotEmpty()) c.map { mapEntityToScrim(it) } else null
                },
                networkLoader = {
                    // Defensive: limit to 200 rows to avoid unbounded data transfer.
                    // PostgREST OR filtering (team_id=eq.X OR opponent_team_id=eq.X) would be ideal
                    // but requires a dedicated RPC or complex query string. For now, client-side filter
                    // with a cap is the safest available option.
                    val r = api.getScrims(range = "0-199")
                    if (r.isSuccessful) r.body()?.map { mapDtoToScrim(it) }?.filter {
                        it.teamId == teamId || it.opponentTeamId == teamId
                    } ?: emptyList()
                    else throw Exception("Failed to fetch team scrims")
                },
                roomSaver = { list ->
                    // Just insert the ones for this team, don't delete everything
                    scrimDao.insertAll(list.map { mapScrimToEntity(it) })
                }
            ).collect { scrims ->
                emit(Result.success(scrims))
            }
        } catch (e: Exception) {
            val c = scrimDao.getByTeam(teamId)
            if (c.isNotEmpty()) emit(Result.success(c.map { mapEntityToScrim(it) }))
            else emit(Result.failure(e))
        }
    }

    override fun getScrimById(id: String): Flow<Result<Scrim?>> = flow {
        try {
            val r = api.getScrimById(PostgrestFilter.eq(id))
            if (r.isSuccessful) {
                val s = r.body()?.firstOrNull()
                if (s != null) {
                    val gameResults = fetchGameResultsForScrim(s.id)
                    val applications = fetchApplicationsForScrim(s.id)
                    val rosters = fetchRostersForScrim(s.id)
                    emit(Result.success(mapDtoToScrim(s, gameResults, applications, rosters)))
                } else emit(Result.success(null))
            } else emit(Result.failure(Exception("Failed to fetch scrim")))
        } catch (e: Exception) {
            val c = scrimDao.getById(id)
            if (c != null) emit(Result.success(mapEntityToScrim(c)))
            else emit(Result.failure(e))
        }
    }

    override fun searchScrims(query: String, gameMode: GameMode?, region: Region?, skillLevel: SkillLevel?, status: ScrimStatus?): Flow<Result<List<Scrim>>> = flow {
        try {
            val r = api.getScrims(
                range = "0-99",
                status = status?.let { PostgrestFilter.eq(toDbStatus(it)) },
                gameMode = gameMode?.let { PostgrestFilter.eq(it.name) },
                region = region?.let { PostgrestFilter.eq(it.name) },
                skillLevel = skillLevel?.let { PostgrestFilter.eq(it.name) }
            )
            if (r.isSuccessful) {
                val q = query.lowercase().trim()
                val scrims = r.body()?.map { mapDtoToScrim(it) }?.filter { s ->
                    q.isEmpty() || s.teamName.lowercase().contains(q) || s.description.lowercase().contains(q)
                } ?: emptyList()
                emit(Result.success(scrims))
            } else emit(Result.failure(Exception("Failed to search scrims")))
        } catch (e: Exception) { emit(Result.failure(e)) }
    }

    override suspend fun createScrim(scrim: Scrim): Flow<Result<Scrim>> = flow {
        try {
            AuthorizationUtils.requireOwner(scrim.teamLeader, "create scrim")
                .onFailure { emit(Result.failure(it)); return@flow }
            // Validate best_of against DB constraint (1, 2, 3, 5)
            if (scrim.bestOf.games !in setOf(1, 2, 3, 5)) {
                emit(Result.failure(Exception("Invalid best-of value: ${scrim.bestOf.games}. Allowed: 1, 2, 3, 5")))
                return@flow
            }
            // Send only required fields as a Map — omit `status` so the DB DEFAULT ('Open') is used.
            // This avoids CHECK constraint violations if the live DB constraint uses different casing.
            // Also send region as enum name (e.g. "EU") not displayName ("Europe") to match DB default.
            val body = mutableMapOf<String, Any>(
                "team_id" to scrim.teamId,
                "scheduled_date" to DateUtils.formatDate(scrim.scheduledTime),
                "scheduled_time" to DateUtils.formatTime(scrim.scheduledTime),
                "best_of" to scrim.bestOf.games,
                "game_mode" to scrim.gameMode.name,
                "region" to scrim.region.name,
                "skill_level" to scrim.skillLevel.name,
                "max_players" to scrim.maxPlayers,
                "current_players" to scrim.currentPlayers
            )
            scrim.teamName.takeIf { it.isNotBlank() }?.let { body["team_name"] = it }
            scrim.description.takeIf { it.isNotBlank() }?.let { body["description"] = it }
            val r = api.createScrim(body)
            if (r.isSuccessful) {
                val created = r.body()?.firstOrNull()
                if (created != null) {
                    // Create empty game result rows for each game in the series.
                    // All must succeed; if any fail, delete the scrim and fail the operation
                    // to prevent incomplete game result sets that block completion.
                    val gameCount = scrim.bestOf.games
                    var gameResultFailures = 0
                    for (gameNum in 1..gameCount) {
                        try {
                            val gr = api.createScrimGameResult(
                                ScrimGameResultDto(
                                    scrimId = created.id,
                                    gameNumber = gameNum
                                )
                            )
                            if (!gr.isSuccessful) {
                                gameResultFailures++
                                Timber.w("ScrimRepo", "Failed to create game result $gameNum: ${gr.errorBody()?.string()}")
                            }
                        } catch (e: Exception) {
                            gameResultFailures++
                            Timber.w("ScrimRepo", "Exception creating game result $gameNum", e)
                        }
                    }
                    if (gameResultFailures > 0) {
                        // Rollback: delete the scrim so incomplete data doesn't persist
                        try {
                            api.deleteScrim(PostgrestFilter.eq(created.id))
                        } catch (e: Exception) {
                            Timber.w("ScrimRepo", "Rollback delete failed for scrim ${created.id}", e)
                        }
                        emit(Result.failure(Exception("Failed to create game results ($gameResultFailures/$gameCount failures). Scrim creation rolled back.")))
                        return@flow
                    }
                    invalidateScrimCaches()
                    emit(Result.success(mapDtoToScrim(created)))
                }
                else emit(Result.failure(Exception("Scrim creation failed")))
            } else emit(Result.failure(Exception("Error: ${r.errorBody()?.string()}")))
        } catch (e: Exception) { emit(Result.failure(e)) }
    }

    override suspend fun updateScrim(scrim: Scrim): Flow<Result<Scrim>> = flow {
        try {
            // Ownership: only the team leader that created the scrim may update it
            val existingResponse = api.getScrimById(PostgrestFilter.eq(scrim.id))
            val existing = existingResponse.body()?.firstOrNull()
            if (existing == null) { emit(Result.failure(Exception("Scrim not found"))); return@flow }
            val hostTeam = api.getTeamById(PostgrestFilter.eq(existing.teamId)).body()?.firstOrNull()
            if (hostTeam == null) { emit(Result.failure(Exception("Host team not found"))); return@flow }
            AuthorizationUtils.requireOwner(hostTeam.leaderId, "update this scrim")
                .onFailure { emit(Result.failure(it)); return@flow }

            val dto = mapScrimToDto(scrim)
            val updates = mutableMapOf<String, Any>("team_id" to dto.teamId, "scheduled_date" to dto.scheduledDate, "scheduled_time" to dto.scheduledTime, "best_of" to dto.bestOf, "status" to dto.status)
            dto.teamName?.takeIf { it.isNotBlank() }?.let { updates["team_name"] = it }
            dto.description?.let { updates["description"] = it }
            dto.opponentTeamId?.let { updates["opponent_team_id"] = it }
            dto.opponentTeamName?.let { updates["opponent_team_name"] = it }
            dto.winnerTeamId?.let { updates["winner_team_id"] = it }
            dto.conversationId?.let { updates["conversation_id"] = it }
            dto.resultSubmittedAt?.let { updates["result_submitted_at"] = it }
            dto.cancellationReason?.let { updates["cancellation_reason"] = it }
            dto.cancelledBy?.let { updates["cancelled_by"] = it }
            updates["game_mode"] = dto.gameMode
            updates["region"] = dto.region
            updates["skill_level"] = dto.skillLevel
            updates["max_players"] = dto.maxPlayers
            updates["current_players"] = dto.currentPlayers
            val r = api.updateScrim(PostgrestFilter.eq(scrim.id), updates)
            if (r.isSuccessful) {
                val u = r.body()?.firstOrNull()
                if (u != null) { invalidateScrimCaches(); emit(Result.success(mapDtoToScrim(u))) }
                else emit(Result.failure(Exception("Update failed")))
            } else emit(Result.failure(Exception("Error updating scrim")))
        } catch (e: Exception) { emit(Result.failure(e)) }
    }

    override suspend fun deleteScrim(id: String): Flow<Result<Unit>> = flow {
        try {
            // Ownership: only the team leader that created the scrim may delete it
            val existingResponse = api.getScrimById(PostgrestFilter.eq(id))
            val existing = existingResponse.body()?.firstOrNull()
            if (existing == null) { emit(Result.failure(Exception("Scrim not found"))); return@flow }
            val hostTeam = api.getTeamById(PostgrestFilter.eq(existing.teamId)).body()?.firstOrNull()
            if (hostTeam == null) { emit(Result.failure(Exception("Host team not found"))); return@flow }
            AuthorizationUtils.requireOwner(hostTeam.leaderId, "delete this scrim")
                .onFailure { emit(Result.failure(it)); return@flow }

            val r = api.deleteScrim(PostgrestFilter.eq(id))
            if (r.isSuccessful) { invalidateScrimCaches(); scrimDao.deleteById(id); emit(Result.success(Unit)) }
            else emit(Result.failure(Exception("Failed to delete scrim")))
        } catch (e: Exception) { emit(Result.failure(e)) }
    }

    override suspend fun applyToScrim(scrimId: String, application: ScrimApplication): Flow<Result<Scrim>> = flow {
        try {
            // Use atomic RPC: locks scrim row, verifies OPEN, prevents applying to filled scrims
            val rpcResult = api.applyToScrimRpc(mapOf(
                "p_scrim_id" to scrimId,
                "p_applicant_team_id" to application.applicantTeamId
            ))
            if (!rpcResult.isSuccessful) {
                emit(Result.failure(Exception("Failed to apply: ${rpcResult.errorBody()?.string() ?: "Unknown error"}")))
                return@flow
            }
            val body = rpcResult.body()
            val success = body?.get("success") as? Boolean ?: false
            if (!success) {
                val error = body?.get("error") as? String ?: "Apply failed"
                emit(Result.failure(Exception(error)))
                return@flow
            }
            invalidateScrimCaches()
            getScrimById(scrimId).collect { result -> emit(result.map { it ?: throw Exception("Scrim not found after apply") }) }
        } catch (e: Exception) { emit(Result.failure(e)) }
    }

    override suspend fun approveApplication(scrimId: String, applicationId: String, conversationId: String): Flow<Result<Scrim>> = flow {
        try {
            // Use atomic RPC: approves the app, cancels others, locks scrim in a single DB transaction
            val rpcResult = api.approveScrimApplication(mapOf(
                "p_application_id" to applicationId,
                "p_conversation_id" to conversationId
            ))
            if (!rpcResult.isSuccessful) {
                val errorBody = rpcResult.errorBody()?.string() ?: "Unknown error"
                emit(Result.failure(Exception("Failed to approve application: $errorBody")))
                return@flow
            }

            val body = rpcResult.body()
            val success = body?.get("success") as? Boolean ?: false
            if (!success) {
                val error = body?.get("error") as? String ?: "Approval failed"
                emit(Result.failure(Exception(error)))
                return@flow
            }

            // Ensure game results exist (fallback for old scrims created before migration)
            val existingGames = fetchGameResultsForScrim(scrimId)
            if (existingGames.isEmpty()) {
                val scrimResponse = api.getScrimById(PostgrestFilter.eq(scrimId))
                val scrimDto = scrimResponse.body()?.firstOrNull()
                if (scrimDto != null) {
                    val gameCount = BestOf.fromGames(scrimDto.bestOf).games
                    for (gameNum in 1..gameCount) {
                        try {
                            api.createScrimGameResult(ScrimGameResultDto(scrimId = scrimId, gameNumber = gameNum))
                        } catch (_: Exception) { }
                    }
                }
            }

            invalidateScrimCaches()
            // Fetch fully populated scrim (with applications, rosters, game results)
            getScrimById(scrimId).collect { result -> emit(result.map { it ?: throw Exception("Scrim not found after approval") }) }
        } catch (e: Exception) { emit(Result.failure(e)) }
    }

    override suspend fun rejectApplication(scrimId: String, applicationId: String): Flow<Result<Scrim>> = flow {
        try {
            // Use atomic RPC: locks application row, verifies still pending, rejects atomically
            val rpcResult = api.rejectScrimApplicationRpc(mapOf("p_application_id" to applicationId))
            if (!rpcResult.isSuccessful) {
                emit(Result.failure(Exception("Failed to reject: ${rpcResult.errorBody()?.string() ?: "Unknown error"}")))
                return@flow
            }
            val body = rpcResult.body()
            val success = body?.get("success") as? Boolean ?: false
            if (!success) {
                val error = body?.get("error") as? String ?: "Reject failed"
                emit(Result.failure(Exception(error)))
                return@flow
            }
            invalidateScrimCaches()
            getScrimById(scrimId).collect { result -> emit(result.map { it ?: throw Exception("Scrim not found after reject") }) }
        } catch (e: Exception) { emit(Result.failure(e)) }
    }

    override suspend fun cancelApplication(scrimId: String, applicationId: String): Flow<Result<Scrim>> = flow {
        try {
            // Use atomic RPC: locks application row, verifies still pending + caller is applicant leader
            val rpcResult = api.cancelScrimApplicationRpc(mapOf("p_application_id" to applicationId))
            if (!rpcResult.isSuccessful) {
                emit(Result.failure(Exception("Failed to cancel: ${rpcResult.errorBody()?.string() ?: "Unknown error"}")))
                return@flow
            }
            val body = rpcResult.body()
            val success = body?.get("success") as? Boolean ?: false
            if (!success) {
                val error = body?.get("error") as? String ?: "Cancel failed"
                emit(Result.failure(Exception(error)))
                return@flow
            }
            invalidateScrimCaches()
            getScrimById(scrimId).collect { result -> emit(result.map { it ?: throw Exception("Scrim not found after cancel") }) }
        } catch (e: Exception) { emit(Result.failure(e)) }
    }

    override suspend fun setScrimRoster(scrimId: String, teamId: String, roster: List<ScrimRosterEntry>): Flow<Result<Scrim>> = flow {
        try {
            // Use atomic RPC: locks scrim row, verifies leader, deletes old roster, inserts new entries in one transaction
            val rpcResult = api.setScrimRosterRpc(mapOf(
                "p_scrim_id" to scrimId,
                "p_team_id" to teamId,
                "p_player_ids" to roster.filter { it.isActive }.map { it.playerId }
            ))
            if (!rpcResult.isSuccessful) {
                emit(Result.failure(Exception("Failed to set roster: ${rpcResult.errorBody()?.string() ?: "Unknown error"}")))
                return@flow
            }
            val body = rpcResult.body()
            val success = body?.get("success") as? Boolean ?: false
            if (!success) {
                val error = body?.get("error") as? String ?: "Set roster failed"
                emit(Result.failure(Exception(error)))
                return@flow
            }
            invalidateScrimCaches()
            getScrimById(scrimId).collect { result -> emit(result.map { it ?: throw Exception("Scrim not found after roster update") }) }
        } catch (e: Exception) { emit(Result.failure(e)) }
    }

    override suspend fun transitionToReadyCheck(scrimId: String): Flow<Result<Scrim>> = flow {
        try {
            // Use atomic RPC: validates host leader, status=Filled, opponent exists, time reached
            val rpcResult = api.transitionToReadyCheckRpc(mapOf("p_scrim_id" to scrimId))
            if (!rpcResult.isSuccessful) {
                emit(Result.failure(Exception("Failed to start ready check: ${rpcResult.errorBody()?.string() ?: "Unknown error"}")))
                return@flow
            }
            val body = rpcResult.body()
            val success = body?.get("success") as? Boolean ?: false
            if (!success) {
                val error = body?.get("error") as? String ?: "Ready check failed"
                emit(Result.failure(Exception(error)))
                return@flow
            }
            invalidateScrimCaches()
            getScrimById(scrimId).collect { result -> emit(result.map { it ?: throw Exception("Scrim not found after ready check") }) }
        } catch (e: Exception) { emit(Result.failure(e)) }
    }

    override suspend fun markReady(scrimId: String, teamId: String): Flow<Result<Scrim>> = flow {
        try {
            // Use atomic RPC: handles race condition with row locking + auto-transition to In Progress
            val rpcResult = api.markScrimReady(mapOf("p_scrim_id" to scrimId, "p_team_id" to teamId))
            if (!rpcResult.isSuccessful) {
                emit(Result.failure(Exception("Failed to mark ready: ${rpcResult.errorBody()?.string() ?: "Unknown error"}")))
                return@flow
            }
            val body = rpcResult.body()
            val success = body?.get("success") as? Boolean ?: false
            if (!success) {
                val error = body?.get("error") as? String ?: "Mark ready failed"
                emit(Result.failure(Exception(error)))
                return@flow
            }
            invalidateScrimCaches()
            getScrimById(scrimId).collect { result -> emit(result.map { it ?: throw Exception("Scrim not found after mark ready") }) }
        } catch (e: Exception) { emit(Result.failure(e)) }
    }

    override suspend fun uploadScreenshot(scrimId: String, teamId: String, screenshotUrl: String): Flow<Result<Scrim>> = flow {
        try {
            // Use atomic RPC: locks scrim row, validates in-progress, participant, leader
            val rpcResult = api.uploadScrimScreenshotRpc(mapOf(
                "p_scrim_id" to scrimId,
                "p_team_id" to teamId,
                "p_screenshot_url" to screenshotUrl
            ))
            if (!rpcResult.isSuccessful) {
                emit(Result.failure(Exception("Failed to upload screenshot: ${rpcResult.errorBody()?.string() ?: "Unknown error"}")))
                return@flow
            }
            val body = rpcResult.body()
            val success = body?.get("success") as? Boolean ?: false
            if (!success) {
                val error = body?.get("error") as? String ?: "Upload failed"
                emit(Result.failure(Exception(error)))
                return@flow
            }
            invalidateScrimCaches()
            getScrimById(scrimId).collect { result -> emit(result.map { it ?: throw Exception("Scrim not found after upload") }) }
        } catch (e: Exception) { emit(Result.failure(e)) }
    }

    override suspend fun completeScrim(scrimId: String, winnerTeamId: String?): Flow<Result<Scrim>> = flow {
        try {
            // Use atomic RPC: validates all games have screenshots + winners, locks row, completes scrim
            // For BO2 ties, winnerTeamId is null — omit the key so RPC receives NULL
            val params = mutableMapOf<String, Any>("p_scrim_id" to scrimId)
            if (!winnerTeamId.isNullOrBlank()) {
                params["p_winner_team_id"] = winnerTeamId
            }
            val rpcResult = api.completeScrimRpc(params)
            if (!rpcResult.isSuccessful) {
                emit(Result.failure(Exception("Failed to complete scrim: ${rpcResult.errorBody()?.string() ?: "Unknown error"}")))
                return@flow
            }
            val body = rpcResult.body()
            val success = body?.get("success") as? Boolean ?: false
            if (!success) {
                val error = body?.get("error") as? String ?: "Complete scrim failed"
                emit(Result.failure(Exception(error)))
                return@flow
            }

            // Best-effort downstream: match record, match result, and points awarding.
            // These are secondary to scrim completion; failures are logged but do not fail the operation.
            var matchId: String? = null
            try {
                val emr = api.getMatches(scrimId = scrimId)
                if (emr.isSuccessful) {
                    val em = emr.body()?.firstOrNull()
                    if (em != null) {
                        matchId = em.id
                    } else {
                        val updated = api.getScrimById(PostgrestFilter.eq(scrimId)).body()?.firstOrNull()
                        val teamBId = updated?.opponentTeamId
                        if (teamBId != null) {
                            val mr = api.createMatch(MatchDto(scrimId = scrimId, teamAId = updated.teamId, teamBId = teamBId, scheduledDate = updated.scheduledDate, scheduledTime = updated.scheduledTime, status = "Completed"))
                            if (mr.isSuccessful) matchId = mr.body()?.firstOrNull()?.id else Timber.w("ScrimRepo", "createMatch failed: ${mr.errorBody()?.string()}")
                        }
                    }
                } else {
                    Timber.w("ScrimRepo", "getMatches failed: ${emr.errorBody()?.string()}")
                }
            } catch (e: Exception) { Timber.w("ScrimRepo", "Failed to find/create match", e) }

            try {
                if (matchId != null) {
                    val emr = api.getMatchResults(PostgrestFilter.eq(matchId))
                    if (emr.isSuccessful) {
                        val existing = emr.body()?.firstOrNull()
                        if (existing == null) {
                            val cmr = api.createMatchResult(MatchResultDto(matchId = matchId, winnerTeamId = winnerTeamId))
                            if (!cmr.isSuccessful) Timber.w("ScrimRepo", "createMatchResult failed: ${cmr.errorBody()?.string()}")
                        } else if (!winnerTeamId.isNullOrBlank()) {
                            val umr = api.updateMatchResult(PostgrestFilter.eq(existing.id), mapOf("winner_team_id" to winnerTeamId))
                            if (!umr.isSuccessful) Timber.w("ScrimRepo", "updateMatchResult failed: ${umr.errorBody()?.string()}")
                        }
                    } else {
                        Timber.w("ScrimRepo", "getMatchResults failed: ${emr.errorBody()?.string()}")
                    }
                }
            } catch (e: Exception) { Timber.w("ScrimRepo", "Failed to create/update match result", e) }

            try {
                if (!winnerTeamId.isNullOrBlank()) {
                    val ar = api.awardScrimPoints(mapOf("p_scrim_id" to scrimId, "p_winner_team_id" to winnerTeamId, "p_pts_per_win" to SupabaseMatchResultRepository.WIN_POINTS, "p_pts_per_loss" to SupabaseMatchResultRepository.LOSS_POINTS_ABS))
                    if (!ar.isSuccessful) Timber.w("ScrimRepo", "awardScrimPoints failed: ${ar.errorBody()?.string()}")
                }
            } catch (e: Exception) { Timber.w("ScrimRepo", "Failed to award scrim points", e) }

            invalidateScrimCaches()
            getScrimById(scrimId).collect { result -> emit(result.map { it ?: throw Exception("Scrim not found after completion") }) }
        } catch (e: Exception) { emit(Result.failure(e)) }
    }

    override fun calculatePointsChanges(scrim: Scrim): PointsResult {
        val winnerTeamId = scrim.winnerTeamId ?: return PointsResult.empty()
        val teamAChanges = scrim.teamAActiveRoster.map { e -> PlayerPointsChange(playerId = e.playerId, playerName = e.playerName, teamId = e.teamId, pointsChange = if (e.teamId == winnerTeamId) PTS_PER_WIN else -PTS_PER_LOSS, isWinner = e.teamId == winnerTeamId) }
        val teamBChanges = scrim.teamBActiveRoster.map { e -> PlayerPointsChange(playerId = e.playerId, playerName = e.playerName, teamId = e.teamId, pointsChange = if (e.teamId == winnerTeamId) PTS_PER_WIN else -PTS_PER_LOSS, isWinner = e.teamId == winnerTeamId) }
        val teamASubs = scrim.teamASubstitutes.map { e -> PlayerPointsChange(playerId = e.playerId, playerName = e.playerName, teamId = e.teamId, pointsChange = 0, isWinner = false, isSubstitute = true) }
        val teamBSubs = scrim.teamBSubstitutes.map { e -> PlayerPointsChange(playerId = e.playerId, playerName = e.playerName, teamId = e.teamId, pointsChange = 0, isWinner = false, isSubstitute = true) }
        return PointsResult(teamAChanges = teamAChanges, teamBChanges = teamBChanges, teamASubstitutes = teamASubs, teamBSubstitutes = teamBSubs, winnerTeamId = winnerTeamId)
    }

    override suspend fun submitResult(scrimId: String, reporterId: String, winnerTeamId: String, notes: String?, screenshotUrl: String?): Flow<Result<Scrim>> = flow {
        try {
            // Use atomic complete_scrim RPC: validates all games have screenshots + winners
            val rpcResult = api.completeScrimRpc(mapOf("p_scrim_id" to scrimId, "p_winner_team_id" to winnerTeamId))
            if (!rpcResult.isSuccessful) {
                emit(Result.failure(Exception("Failed to submit result: ${rpcResult.errorBody()?.string() ?: "Unknown error"}")))
                return@flow
            }
            val body = rpcResult.body()
            val success = body?.get("success") as? Boolean ?: false
            if (!success) {
                val error = body?.get("error") as? String ?: "Submit result failed"
                emit(Result.failure(Exception(error)))
                return@flow
            }
            invalidateScrimCaches()
            getScrimById(scrimId).collect { result -> emit(result.map { it ?: throw Exception("Scrim not found after submit") }) }
        } catch (e: Exception) { emit(Result.failure(e)) }
    }

    override suspend fun createAutoCancelledRecord(scrimId: String): Flow<Result<Unit>> = flow {
        try {
            // Use atomic RPC: locks row, prevents double-cancel, cleans up pending applications
            val rpcResult = api.autoCancelScrimRpc(mapOf("p_scrim_id" to scrimId))
            if (!rpcResult.isSuccessful) {
                emit(Result.failure(Exception("Failed to auto-cancel: ${rpcResult.errorBody()?.string() ?: "Unknown error"}")))
                return@flow
            }
            val body = rpcResult.body()
            val success = body?.get("success") as? Boolean ?: false
            if (!success) {
                val error = body?.get("error") as? String ?: "Auto-cancel failed"
                emit(Result.failure(Exception(error)))
                return@flow
            }
            invalidateScrimCaches()
            emit(Result.success(Unit))
        } catch (e: Exception) { emit(Result.failure(e)) }
    }

    override suspend fun cancelScrim(scrimId: String, reason: String, cancelledBy: String): Flow<Result<Unit>> = flow {
        try {
            val rpcResult = api.cancelScrimRpc(mapOf(
                "p_scrim_id" to scrimId,
                "p_reason" to reason,
                "p_cancelled_by" to cancelledBy
            ))
            if (!rpcResult.isSuccessful) {
                emit(Result.failure(Exception("Failed to cancel scrim: ${rpcResult.errorBody()?.string() ?: "Unknown error"}")))
                return@flow
            }
            val body = rpcResult.body()
            val success = body?.get("success") as? Boolean ?: false
            if (!success) {
                val error = body?.get("error") as? String ?: "Cancel scrim failed"
                emit(Result.failure(Exception(error)))
                return@flow
            }
            invalidateScrimCaches()
            emit(Result.success(Unit))
        } catch (e: Exception) { emit(Result.failure(e)) }
    }

    // ═══════════════════════════════════════════════════════════════
    // REALTIME SUBSCRIPTIONS
    // ═══════════════════════════════════════════════════════════════

    override fun subscribeToScrim(scrimId: String): Flow<Scrim> = flow {
        try {
            realtimeClient.connect()
            val channelName = "public:scrims:scrim_$scrimId"
            realtimeClient.subscribe(
                channelName = channelName,
                configs = listOf(
                    SupabaseRealtimeClient.PostgresChangeConfig(
                        event = "UPDATE",
                        table = SupabaseConfig.TABLE_SCRIMS,
                        filter = "id=eq.$scrimId"
                    )
                )
            ).filter { event ->
                event.eventType == SupabaseRealtimeClient.EVENT_UPDATE && event.record != null
            }.collect { event ->
                try {
                    val dto = parseRealtimeRecordToScrimDto(event.record!!)
                    if (dto.id == scrimId) {
                        invalidateScrimCaches()
                        val gameResults = fetchGameResultsForScrim(dto.id)
                        val applications = fetchApplicationsForScrim(dto.id)
                        val rosters = fetchRostersForScrim(dto.id)
                        emit(mapDtoToScrim(dto, gameResults, applications, rosters))
                    }
                } catch (e: Exception) {
                    Timber.w("ScrimRepo", "Failed to parse Realtime UPDATE: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Timber.w("ScrimRepo", "Realtime subscription failed for scrim $scrimId: ${e.message}")
        }
    }

    override fun subscribeToAllScrims(): Flow<Scrim> = flow {
        try {
            realtimeClient.connect()
            val channelName = "public:scrims:all"
            realtimeClient.subscribe(
                channelName = channelName,
                configs = listOf(
                    SupabaseRealtimeClient.PostgresChangeConfig(
                        event = "*",
                        table = SupabaseConfig.TABLE_SCRIMS
                    )
                )
            ).collect { event ->
                try {
                    // Skip DELETE events — repository emits Scrim objects; consumer handles removal
                    if (event.eventType == SupabaseRealtimeClient.EVENT_DELETE) {
                        Timber.d("ScrimRepo", "Realtime DELETE for scrim ${event.oldRecord?.get("id")?.asString} — skipping emit")
                        return@collect
                    }
                    val record = event.record
                    if (record != null) {
                        val dto = parseRealtimeRecordToScrimDto(record)
                        invalidateScrimCaches()
                        val gameResults = fetchGameResultsForScrim(dto.id)
                        val applications = fetchApplicationsForScrim(dto.id)
                        val rosters = fetchRostersForScrim(dto.id)
                        emit(mapDtoToScrim(dto, gameResults, applications, rosters))
                    }
                } catch (e: Exception) {
                    Timber.w("ScrimRepo", "Failed to parse Realtime event: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Timber.w("ScrimRepo", "Realtime subscription failed for all scrims: ${e.message}")
        }
    }

    /**
     * Parse a Realtime record (JsonObject) into a ScrimDto.
     */
    private fun parseRealtimeRecordToScrimDto(record: com.google.gson.JsonObject): ScrimDto {
        return ScrimDto(
            id = record.get("id")?.asString ?: "",
            teamId = record.get("team_id")?.asString ?: "",
            teamName = record.get("team_name")?.asString,
            scheduledDate = record.get("scheduled_date")?.asString ?: "",
            scheduledTime = record.get("scheduled_time")?.asString ?: "",
            bestOf = record.get("best_of")?.asInt ?: 1,
            status = record.get("status")?.asString ?: "Open",
            description = record.get("description")?.asString,
            opponentTeamId = record.get("opponent_team_id")?.asString,
            opponentTeamName = record.get("opponent_team_name")?.asString,
            winnerTeamId = record.get("winner_team_id")?.asString,
            teamAReady = record.get("team_a_ready")?.asBoolean ?: false,
            teamBReady = record.get("team_b_ready")?.asBoolean ?: false,
            teamAReadyAt = record.get("team_a_ready_at")?.asString,
            teamBReadyAt = record.get("team_b_ready_at")?.asString,
            teamAScreenshotUrl = record.get("team_a_screenshot_url")?.asString,
            teamBScreenshotUrl = record.get("team_b_screenshot_url")?.asString,
            teamAScreenshotUploadedAt = record.get("team_a_screenshot_uploaded_at")?.asString,
            teamBScreenshotUploadedAt = record.get("team_b_screenshot_uploaded_at")?.asString,
            conversationId = record.get("conversation_id")?.asString,
            resultSubmittedAt = record.get("result_submitted_at")?.asString,
            cancellationReason = record.get("cancellation_reason")?.asString,
            cancelledBy = record.get("cancelled_by")?.asString,
            gameMode = record.get("game_mode")?.asString ?: "RANKED",
            region = record.get("region")?.asString ?: "EU",
            skillLevel = record.get("skill_level")?.asString ?: "ALL",
            maxPlayers = record.get("max_players")?.asInt ?: 10,
            currentPlayers = record.get("current_players")?.asInt ?: 0,
        )
    }

    // ─── Mapping ───

    private fun mapScrimToDto(scrim: Scrim): ScrimDto {
        return ScrimDto(
            id = scrim.id.takeIf { it.isNotBlank() } ?: java.util.UUID.randomUUID().toString(),
            teamId = scrim.teamId,
            teamName = scrim.teamName.takeIf { it.isNotBlank() },
            scheduledDate = DateUtils.formatDate(scrim.scheduledTime),
            scheduledTime = DateUtils.formatTime(scrim.scheduledTime),
            bestOf = scrim.bestOf.games,
            status = toDbStatus(scrim.status),
            description = scrim.description.takeIf { it.isNotBlank() },
            opponentTeamId = scrim.opponentTeamId,
            opponentTeamName = scrim.opponentTeamName,
            winnerTeamId = scrim.winnerTeamId,
            teamAReady = scrim.teamAReady,
            teamBReady = scrim.teamBReady,
            teamAReadyAt = scrim.teamAReadyAt?.let { DateUtils.formatIsoUtc(it) },
            teamBReadyAt = scrim.teamBReadyAt?.let { DateUtils.formatIsoUtc(it) },
            teamAScreenshotUrl = scrim.teamAScreenshotUrl,
            teamBScreenshotUrl = scrim.teamBScreenshotUrl,
            teamAScreenshotUploadedAt = scrim.teamAScreenshotUploadedAt?.let { DateUtils.formatIsoUtc(it) },
            teamBScreenshotUploadedAt = scrim.teamBScreenshotUploadedAt?.let { DateUtils.formatIsoUtc(it) },
            gameMode = scrim.gameMode.name,
            region = scrim.region.name,
            skillLevel = scrim.skillLevel.name,
            maxPlayers = scrim.maxPlayers,
            currentPlayers = scrim.currentPlayers
        )
    }

    private fun mapDtoToScrim(
        dto: ScrimDto,
        gameResults: List<ScrimGameResult> = emptyList(),
        applications: List<ScrimApplication> = emptyList(),
        rosters: List<ScrimRosterEntry> = emptyList()
    ): Scrim {
        val scheduledTime = DateUtils.parseIsoToMillis("${dto.scheduledDate}T${dto.scheduledTime}")
        val parseTs = { raw: String? -> DateUtils.parseIsoToMillis(raw) }
        val teamARoster = rosters.filter { it.teamId == dto.teamId }
        val teamBRoster = rosters.filter { it.teamId == dto.opponentTeamId }
        return Scrim(
            id = dto.id,
            teamId = dto.teamId,
            teamName = dto.teamName ?: "",
            teamLeader = "",
            gameMode = try { GameMode.valueOf(dto.gameMode) } catch (_: Exception) { GameMode.RANKED },
            region = try { Region.valueOf(dto.region) } catch (_: Exception) { try { Region.fromDisplayName(dto.region) } catch (_: Exception) { Region.EU } },
            skillLevel = try { SkillLevel.valueOf(dto.skillLevel) } catch (_: Exception) { SkillLevel.ALL },
            bestOf = BestOf.fromGames(dto.bestOf),
            scheduledTime = scheduledTime,
            maxPlayers = dto.maxPlayers,
            currentPlayers = dto.currentPlayers,
            status = fromDbStatus(dto.status),
            description = dto.description ?: "",
            opponentTeamId = dto.opponentTeamId,
            opponentTeamName = dto.opponentTeamName,
            winnerTeamId = dto.winnerTeamId,
            teamAReady = dto.teamAReady,
            teamBReady = dto.teamBReady,
            teamAReadyAt = parseTs(dto.teamAReadyAt),
            teamBReadyAt = parseTs(dto.teamBReadyAt),
            teamAScreenshotUrl = dto.teamAScreenshotUrl,
            teamBScreenshotUrl = dto.teamBScreenshotUrl,
            teamAScreenshotUploadedAt = parseTs(dto.teamAScreenshotUploadedAt),
            teamBScreenshotUploadedAt = parseTs(dto.teamBScreenshotUploadedAt),
            conversationId = dto.conversationId,
            resultSubmittedAt = parseTs(dto.resultSubmittedAt),
            cancellationReason = dto.cancellationReason,
            cancelledBy = dto.cancelledBy,
            applications = applications,
            teamARoster = teamARoster,
            teamBRoster = teamBRoster,
            gameResults = gameResults
        )
    }

    private fun mapScrimToEntity(scrim: Scrim): ScrimEntity {
        return ScrimEntity(
            id = scrim.id, teamId = scrim.teamId,
            teamName = scrim.teamName,
            teamLeader = scrim.teamLeader,
            scheduledDate = DateUtils.formatDate(scrim.scheduledTime),
            scheduledTime = DateUtils.formatTime(scrim.scheduledTime),
            bestOf = scrim.bestOf.games, status = scrim.status.name,
            description = scrim.description,
            opponentTeamId = scrim.opponentTeamId, opponentTeamName = scrim.opponentTeamName,
            winnerTeamId = scrim.winnerTeamId,
            teamAReady = scrim.teamAReady, teamBReady = scrim.teamBReady,
            teamAReadyAt = scrim.teamAReadyAt?.let { DateUtils.formatIsoUtc(it) },
            teamBReadyAt = scrim.teamBReadyAt?.let { DateUtils.formatIsoUtc(it) },
            teamAScreenshotUrl = scrim.teamAScreenshotUrl, teamBScreenshotUrl = scrim.teamBScreenshotUrl,
            teamAScreenshotUploadedAt = scrim.teamAScreenshotUploadedAt?.let { DateUtils.formatIsoUtc(it) },
            teamBScreenshotUploadedAt = scrim.teamBScreenshotUploadedAt?.let { DateUtils.formatIsoUtc(it) },
            conversationId = scrim.conversationId,
            resultSubmittedAt = scrim.resultSubmittedAt?.let { DateUtils.formatIsoUtc(it) },
            cancellationReason = scrim.cancellationReason,
            cancelledBy = scrim.cancelledBy,
            gameMode = scrim.gameMode.name, region = scrim.region.name,
            skillLevel = scrim.skillLevel.name,
            maxPlayers = scrim.maxPlayers, currentPlayers = scrim.currentPlayers,
            createdAt = DateUtils.formatIsoUtc(scrim.createdAt)
        )
    }

    private fun mapEntityToScrim(e: ScrimEntity): Scrim {
        val scheduledTime = DateUtils.parseIsoToMillis("${e.scheduledDate}T${e.scheduledTime}")
        val parseTs = { raw: String? -> DateUtils.parseIsoToMillis(raw) }
        return Scrim(
            id = e.id, teamId = e.teamId,
            teamName = e.teamName,
            teamLeader = e.teamLeader,
            gameMode = try { GameMode.valueOf(e.gameMode) } catch (_: Exception) { GameMode.RANKED },
            region = try { Region.valueOf(e.region) } catch (_: Exception) { try { Region.fromDisplayName(e.region) } catch (_: Exception) { Region.EU } },
            skillLevel = try { SkillLevel.valueOf(e.skillLevel) } catch (_: Exception) { SkillLevel.ALL },
            bestOf = BestOf.fromGames(e.bestOf),
            scheduledTime = scheduledTime,
            maxPlayers = e.maxPlayers, currentPlayers = e.currentPlayers,
            status = try { ScrimStatus.valueOf(e.status) } catch (_: Exception) { ScrimStatus.OPEN },
            description = e.description ?: "",
            opponentTeamId = e.opponentTeamId, opponentTeamName = e.opponentTeamName,
            winnerTeamId = e.winnerTeamId,
            teamAReady = e.teamAReady, teamBReady = e.teamBReady,
            teamAReadyAt = parseTs(e.teamAReadyAt), teamBReadyAt = parseTs(e.teamBReadyAt),
            teamAScreenshotUrl = e.teamAScreenshotUrl, teamBScreenshotUrl = e.teamBScreenshotUrl,
            teamAScreenshotUploadedAt = parseTs(e.teamAScreenshotUploadedAt),
            teamBScreenshotUploadedAt = parseTs(e.teamBScreenshotUploadedAt),
            conversationId = e.conversationId,
            resultSubmittedAt = parseTs(e.resultSubmittedAt),
            cancellationReason = e.cancellationReason,
            cancelledBy = e.cancelledBy,
            createdAt = try { DateUtils.parseIsoToMillis(e.createdAt) } catch (_: Exception) { System.currentTimeMillis() }
        )
    }

    // ─── ScrimGameResult mapping ───

    private fun mapDtoToScrimGameResult(dto: ScrimGameResultDto): ScrimGameResult {
        return ScrimGameResult(
            id = dto.id ?: "",  // null on CREATE (not sent), present on READ from DB
            scrimId = dto.scrimId,
            gameNumber = dto.gameNumber,
            teamAScreenshotUrl = dto.teamAScreenshotUrl,
            teamBScreenshotUrl = dto.teamBScreenshotUrl,
            teamAScreenshotUploadedAt = dto.teamAScreenshotUploadedAt?.let { DateUtils.parseIsoToMillis(it) },
            teamBScreenshotUploadedAt = dto.teamBScreenshotUploadedAt?.let { DateUtils.parseIsoToMillis(it) },
            winnerTeamId = dto.winnerTeamId,
            teamASelectedWinnerId = dto.teamASelectedWinnerId,
            teamBSelectedWinnerId = dto.teamBSelectedWinnerId,
            adminOverrideWinnerId = dto.adminOverrideWinnerId,
            isDisputed = dto.isDisputed,
            status = fromDbGameStatus(dto.status)
        )
    }

    private fun mapScrimGameResultToDto(result: ScrimGameResult): ScrimGameResultDto {
        return ScrimGameResultDto(
            id = result.id.takeIf { it.isNotBlank() },  // null on CREATE so DB DEFAULT is used
            scrimId = result.scrimId,
            gameNumber = result.gameNumber,
            teamAScreenshotUrl = result.teamAScreenshotUrl,
            teamBScreenshotUrl = result.teamBScreenshotUrl,
            teamAScreenshotUploadedAt = result.teamAScreenshotUploadedAt?.let { DateUtils.formatIsoUtc(it) },
            teamBScreenshotUploadedAt = result.teamBScreenshotUploadedAt?.let { DateUtils.formatIsoUtc(it) },
            winnerTeamId = result.winnerTeamId,
            teamASelectedWinnerId = result.teamASelectedWinnerId,
            teamBSelectedWinnerId = result.teamBSelectedWinnerId,
            adminOverrideWinnerId = result.adminOverrideWinnerId,
            isDisputed = result.isDisputed,
            status = toDbGameStatus(result.status)
        )
    }

    // ─── Fetch game results for a scrim ───

    private suspend fun fetchGameResultsForScrim(scrimId: String): List<ScrimGameResult> {
        return try {
            val r = api.getScrimGameResults(scrimId = PostgrestFilter.eq(scrimId))
            if (r.isSuccessful) {
                r.body()?.map { mapDtoToScrimGameResult(it) } ?: emptyList()
            } else {
                Timber.w("ScrimRepo", "Failed to fetch game results for scrim $scrimId: ${r.errorBody()?.string()}")
                emptyList()
            }
        } catch (e: Exception) {
            Timber.w("ScrimRepo", "Exception fetching game results for scrim $scrimId", e)
            emptyList()
        }
    }

    // ─── Fetch applications for a scrim ───

    private suspend fun fetchApplicationsForScrim(scrimId: String): List<ScrimApplication> {
        return try {
            val r = api.getScrimApplications(PostgrestFilter.eq(scrimId))
            if (!r.isSuccessful) {
                Timber.w("ScrimRepo", "Failed to fetch applications for scrim $scrimId")
                return emptyList()
            }
            val dtos = r.body() ?: return emptyList()
            if (dtos.isEmpty()) return emptyList()

            // Batch fetch applicant teams
            val teamIds = dtos.map { it.applicantTeamId }.distinct().filter { it.isNotBlank() }
            val teamsById = if (teamIds.isNotEmpty()) {
                try {
                    val tr = api.getTeamsByIds(PostgrestFilter.inList(teamIds))
                    if (tr.isSuccessful) {
                        tr.body()?.associateBy { it.id } ?: emptyMap()
                    } else emptyMap()
                } catch (_: Exception) { emptyMap() }
            } else emptyMap()

            // Batch fetch team members for all applicant teams
            val allUserIds = mutableListOf<String>()
            val membersByTeamId = mutableMapOf<String, List<TeamMemberDto>>()
            if (teamIds.isNotEmpty()) {
                try {
                    val mr = api.getTeamMembers(teamId = PostgrestFilter.inList(teamIds))
                    if (mr.isSuccessful) {
                        val members = mr.body() ?: emptyList()
                        membersByTeamId.putAll(members.groupBy { it.teamId })
                        allUserIds.addAll(members.map { it.userId }.distinct())
                    }
                } catch (_: Exception) { }
            }

            // Batch fetch profiles for player names
            val profilesById = if (allUserIds.isNotEmpty()) {
                try {
                    val pr = api.getProfiles(idFilter = PostgrestFilter.inList(allUserIds))
                    if (pr.isSuccessful) {
                        pr.body()?.associateBy { it.id } ?: emptyMap()
                    } else emptyMap()
                } catch (_: Exception) { emptyMap() }
            } else emptyMap()

            return dtos.map { dto ->
                val team = teamsById[dto.applicantTeamId]
                val members = membersByTeamId[dto.applicantTeamId] ?: emptyList()
                val players = members.map { m ->
                    val profile = profilesById[m.userId]
                    Player(
                        id = m.userId,
                        name = profile?.username ?: m.userId.take(8),
                        role = if (m.role == TeamRole.LEADER) PlayerRole.LEADER else PlayerRole.MEMBER,
                        email = profile?.email ?: "",
                        avatarUrl = profile?.avatarUrl
                    )
                }
                ScrimApplication(
                    id = dto.id ?: "",
                    scrimId = dto.scrimId,
                    applicantTeamId = dto.applicantTeamId,
                    applicantTeamName = team?.name ?: "",
                    applicantTeamLeader = team?.leaderId ?: "",
                    applicantTeamLeaderName = profilesById[team?.leaderId]?.username ?: "",
                    applicantTeamAvatarUrl = team?.logoUrl,
                    applicantTeamPlayers = players,
                    status = fromDbApplicationStatus(dto.status),
                    appliedAt = dto.appliedAt?.let { DateUtils.parseIsoToMillis(it) } ?: System.currentTimeMillis()
                )
            }
        } catch (e: Exception) {
            Timber.w("ScrimRepo", "Exception fetching applications for scrim $scrimId", e)
            emptyList()
        }
    }

    // ─── Fetch rosters for a scrim ───

    private suspend fun fetchRostersForScrim(scrimId: String): List<ScrimRosterEntry> {
        return try {
            val r = api.getScrimRosters(PostgrestFilter.eq(scrimId))
            if (r.isSuccessful) {
                r.body()?.map { mapDtoToScrimRosterEntry(it) } ?: emptyList()
            } else {
                Timber.w("ScrimRepo", "Failed to fetch rosters for scrim $scrimId")
                emptyList()
            }
        } catch (e: Exception) {
            Timber.w("ScrimRepo", "Exception fetching rosters for scrim $scrimId", e)
            emptyList()
        }
    }

    private fun mapDtoToScrimRosterEntry(dto: ScrimRosterDto): ScrimRosterEntry {
        return ScrimRosterEntry(
            playerId = dto.userId,
            playerName = "", // Not available in ScrimRosterDto; resolved by UI
            teamId = dto.teamId,
            isActive = dto.isActive
        )
    }

    // ─── Per-game screenshot upload ───

    override suspend fun uploadGameScreenshot(scrimId: String, teamId: String, gameNumber: Int, screenshotUrl: String): Flow<Result<Scrim>> = flow {
        try {
            // Use atomic RPC: locks scrim + game result rows, derives is_team_a from DB
            // No pre-read needed — eliminates race condition where team_id changes between read and write
            val rpcResult = api.uploadGameScreenshotRpc(mapOf(
                "p_scrim_id" to scrimId,
                "p_game_number" to gameNumber,
                "p_team_id" to teamId,
                "p_screenshot_url" to screenshotUrl
            ))
            if (!rpcResult.isSuccessful) {
                emit(Result.failure(Exception("Failed to upload screenshot: ${rpcResult.errorBody()?.string() ?: "Unknown error"}")))
                return@flow
            }
            val body = rpcResult.body()
            val success = body?.get("success") as? Boolean ?: false
            if (!success) {
                val error = body?.get("error") as? String ?: "Upload failed"
                emit(Result.failure(Exception(error)))
                return@flow
            }
            invalidateScrimCaches()
            getScrimById(scrimId).collect { result ->
                result.getOrNull()?.let { emit(Result.success(it)) }
                    ?: emit(Result.failure(Exception("Scrim not found after update")))
            }
        } catch (e: Exception) { emit(Result.failure(e)) }
    }

    // ─── Per-game winner selection ───

    override suspend fun selectGameWinner(scrimId: String, gameNumber: Int, winnerTeamId: String): Flow<Result<Scrim>> = flow {
        try {
            // Use atomic RPC: validates screenshots exist, locks rows, prevents race condition
            val rpcResult = api.selectGameWinnerRpc(mapOf(
                "p_scrim_id" to scrimId,
                "p_game_number" to gameNumber,
                "p_winner_team_id" to winnerTeamId
            ))
            if (!rpcResult.isSuccessful) {
                emit(Result.failure(Exception("Failed to select winner: ${rpcResult.errorBody()?.string() ?: "Unknown error"}")))
                return@flow
            }
            val body = rpcResult.body()
            val success = body?.get("success") as? Boolean ?: false
            if (!success) {
                val error = body?.get("error") as? String ?: "Select winner failed"
                emit(Result.failure(Exception(error)))
                return@flow
            }
            invalidateScrimCaches()
            getScrimById(scrimId).collect { result ->
                result.getOrNull()?.let { emit(Result.success(it)) }
                    ?: emit(Result.failure(Exception("Scrim not found after update")))
            }
        } catch (e: Exception) { emit(Result.failure(e)) }
    }

    override suspend fun changeSeriesFormat(scrimId: String, newBestOf: Int): Flow<Result<Scrim>> = flow {
        try {
            val rpcResult = api.changeSeriesFormatRpc(mapOf(
                "p_scrim_id" to scrimId,
                "p_new_best_of" to newBestOf
            ))
            if (!rpcResult.isSuccessful) {
                emit(Result.failure(Exception("Failed to change format: ${rpcResult.errorBody()?.string() ?: "Unknown error"}")))
                return@flow
            }
            val body = rpcResult.body()
            val success = body?.get("success") as? Boolean ?: false
            if (!success) {
                val error = body?.get("error") as? String ?: "Change format failed"
                emit(Result.failure(Exception(error)))
                return@flow
            }
            invalidateScrimCaches()
            getScrimById(scrimId).collect { result ->
                result.getOrNull()?.let { emit(Result.success(it)) }
                    ?: emit(Result.failure(Exception("Scrim not found after format change")))
            }
        } catch (e: Exception) { emit(Result.failure(e)) }
    }
}

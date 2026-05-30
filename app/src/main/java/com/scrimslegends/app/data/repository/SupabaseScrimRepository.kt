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
                    val r = api.getScrims(range = "0-49")
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
                    val r = api.getScrims()
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
                emit(Result.success(s?.let { mapDtoToScrim(it) }))
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
                status = status?.let { PostgrestFilter.eq(it.name) },
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
            val dto = mapScrimToDto(scrim)
            val r = api.createScrim(dto)
            if (r.isSuccessful) {
                val created = r.body()?.firstOrNull()
                if (created != null) { invalidateScrimCaches(); emit(Result.success(mapDtoToScrim(created))) }
                else emit(Result.failure(Exception("Scrim creation failed")))
            } else emit(Result.failure(Exception("Error: ${r.errorBody()?.string()}")))
        } catch (e: Exception) { emit(Result.failure(e)) }
    }

    override suspend fun updateScrim(scrim: Scrim): Flow<Result<Scrim>> = flow {
        try {
            // Ownership: only the team that created the scrim may update it
            val existingResponse = api.getScrimById(PostgrestFilter.eq(scrim.id))
            val existing = existingResponse.body()?.firstOrNull()
            if (existing == null) { emit(Result.failure(Exception("Scrim not found"))); return@flow }
            AuthorizationUtils.requireOwner(existing.teamId, "update this scrim")
                .onFailure { emit(Result.failure(it)); return@flow }

            val dto = mapScrimToDto(scrim)
            val updates = mutableMapOf<String, Any>("team_id" to dto.teamId, "scheduled_date" to dto.scheduledDate, "scheduled_time" to dto.scheduledTime, "best_of" to dto.bestOf, "status" to dto.status)
            dto.description?.let { updates["description"] = it }
            dto.opponentTeamId?.let { updates["opponent_team_id"] = it }
            dto.opponentTeamName?.let { updates["opponent_team_name"] = it }
            dto.winnerTeamId?.let { updates["winner_team_id"] = it }
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
            // Ownership: only the team that created the scrim may delete it
            val existingResponse = api.getScrimById(PostgrestFilter.eq(id))
            val existing = existingResponse.body()?.firstOrNull()
            if (existing == null) { emit(Result.failure(Exception("Scrim not found"))); return@flow }
            AuthorizationUtils.requireOwner(existing.teamId, "delete this scrim")
                .onFailure { emit(Result.failure(it)); return@flow }

            val r = api.deleteScrim(PostgrestFilter.eq(id))
            if (r.isSuccessful) { invalidateScrimCaches(); scrimDao.deleteById(id); emit(Result.success(Unit)) }
            else emit(Result.failure(Exception("Failed to delete scrim")))
        } catch (e: Exception) { emit(Result.failure(e)) }
    }

    override suspend fun applyToScrim(scrimId: String, application: ScrimApplication): Flow<Result<Scrim>> = flow {
        try {
            val dto = ScrimApplicationDto(scrimId = scrimId, applicantTeamId = application.applicantTeamId, status = "Pending")
            val r = api.createScrimApplication(dto)
            if (r.isSuccessful) {
                invalidateScrimCaches()
                getScrimById(scrimId).collect { result -> emit(result.map { it ?: throw Exception("Scrim not found after update") }) }
            } else emit(Result.failure(Exception("Failed to apply: ${r.errorBody()?.string()}")))
        } catch (e: Exception) { emit(Result.failure(e)) }
    }

    override suspend fun approveApplication(scrimId: String, applicationId: String, conversationId: String): Flow<Result<Scrim>> = flow {
        try {
            // Ownership: only the scrim poster may approve applications
            val scrimResponse = api.getScrimById(PostgrestFilter.eq(scrimId))
            val scrim = scrimResponse.body()?.firstOrNull()
            if (scrim == null) { emit(Result.failure(Exception("Scrim not found"))); return@flow }
            AuthorizationUtils.requireOwner(scrim.teamId, "approve applications for this scrim")
                .onFailure { emit(Result.failure(it)); return@flow }

            val appResponse = api.getScrimApplications(PostgrestFilter.eq(applicationId))
            if (!appResponse.isSuccessful) { emit(Result.failure(Exception("Failed to fetch application"))); return@flow }
            val application = appResponse.body()?.firstOrNull() ?: run { emit(Result.failure(Exception("Application not found"))); return@flow }
            api.updateScrimApplication(PostgrestFilter.eq(applicationId), mapOf("status" to "APPROVED"))
            api.updateScrimApplicationsBulk(scrimId = PostgrestFilter.eq(scrimId), status = PostgrestFilter.eq("Pending"), body = mapOf("status" to "CANCELLED"))
            val updates = mutableMapOf<String, Any>("status" to "FILLED", "opponent_team_id" to application.applicantTeamId, "conversation_id" to conversationId)
            try { api.getTeamById(PostgrestFilter.eq(application.applicantTeamId)).body()?.firstOrNull()?.name?.let { updates["opponent_team_name"] = it } } catch (_: Exception) { }
            val r = api.updateScrim(PostgrestFilter.eq(scrimId), updates)
            if (r.isSuccessful) {
                val u = r.body()?.firstOrNull()
                if (u != null) { invalidateScrimCaches(); emit(Result.success(mapDtoToScrim(u))) }
                else emit(Result.failure(Exception("Approve failed")))
            } else emit(Result.failure(Exception("Error approving application")))
        } catch (e: Exception) { emit(Result.failure(e)) }
    }

    override suspend fun rejectApplication(scrimId: String, applicationId: String): Flow<Result<Scrim>> = flow {
        try {
            // Ownership: only the scrim poster may reject applications
            val scrimResponse = api.getScrimById(PostgrestFilter.eq(scrimId))
            val scrim = scrimResponse.body()?.firstOrNull()
            if (scrim == null) { emit(Result.failure(Exception("Scrim not found"))); return@flow }
            AuthorizationUtils.requireOwner(scrim.teamId, "reject applications for this scrim")
                .onFailure { emit(Result.failure(it)); return@flow }

            val r = api.updateScrimApplication(PostgrestFilter.eq(applicationId), mapOf("status" to "REJECTED"))
            if (r.isSuccessful) { invalidateScrimCaches(); getScrimById(scrimId).collect { result -> emit(result.map { it ?: throw Exception("Scrim not found") }) } }
            else emit(Result.failure(Exception("Failed to reject application")))
        } catch (e: Exception) { emit(Result.failure(e)) }
    }

    override suspend fun cancelApplication(scrimId: String, applicationId: String): Flow<Result<Scrim>> = flow {
        try {
            // Ownership: only the applicant team may cancel their application
            val appResponse = api.getScrimApplications(PostgrestFilter.eq(applicationId))
            if (!appResponse.isSuccessful || appResponse.body().isNullOrEmpty()) {
                emit(Result.failure(Exception("Application not found")))
                return@flow
            }
            val app = appResponse.body()!!.first()
            AuthorizationUtils.requireOwner(app.applicantTeamId, "cancel this application")
                .onFailure { emit(Result.failure(it)); return@flow }

            val r = api.updateScrimApplication(PostgrestFilter.eq(applicationId), mapOf("status" to "CANCELLED"))
            if (r.isSuccessful) { invalidateScrimCaches(); getScrimById(scrimId).collect { result -> emit(result.map { it ?: throw Exception("Scrim not found") }) } }
            else emit(Result.failure(Exception("Failed to cancel application")))
        } catch (e: Exception) { emit(Result.failure(e)) }
    }

    override suspend fun setScrimRoster(scrimId: String, teamId: String, roster: List<ScrimRosterEntry>): Flow<Result<Scrim>> = flow {
        try {
            // Ownership: only participating teams may set their roster
            val scrimResponse = api.getScrimById(PostgrestFilter.eq(scrimId))
            val scrim = scrimResponse.body()?.firstOrNull()
            if (scrim == null) { emit(Result.failure(Exception("Scrim not found"))); return@flow }
            val participantIds = listOfNotNull(scrim.teamId, scrim.opponentTeamId)
            AuthorizationUtils.requireParticipant(participantIds, "set roster for this scrim")
                .onFailure { emit(Result.failure(it)); return@flow }

            val existing = api.getScrimRosters(PostgrestFilter.eq(scrimId), PostgrestFilter.eq(teamId))
            if (existing.isSuccessful) { existing.body()?.forEach { api.deleteScrimRosterEntry(PostgrestFilter.eq(scrimId), PostgrestFilter.eq(teamId), PostgrestFilter.eq(it.userId)) } }
            roster.forEach { entry -> api.createScrimRosterEntry(ScrimRosterDto(scrimId = scrimId, teamId = teamId, userId = entry.playerId, isActive = entry.isActive)) }
            invalidateScrimCaches()
            getScrimById(scrimId).collect { result -> emit(result.map { it ?: throw Exception("Scrim not found") }) }
        } catch (e: Exception) { emit(Result.failure(e)) }
    }

    override suspend fun transitionToReadyCheck(scrimId: String): Flow<Result<Scrim>> = flow {
        try {
            // Ownership: only the scrim poster may transition to ready check
            val scrimResponse = api.getScrimById(PostgrestFilter.eq(scrimId))
            val scrim = scrimResponse.body()?.firstOrNull()
            if (scrim == null) { emit(Result.failure(Exception("Scrim not found"))); return@flow }
            AuthorizationUtils.requireOwner(scrim.teamId, "transition this scrim to ready check")
                .onFailure { emit(Result.failure(it)); return@flow }

            val r = api.updateScrim(PostgrestFilter.eq(scrimId), mapOf("status" to "READY_CHECK"))
            if (r.isSuccessful) { val u = r.body()?.firstOrNull(); if (u != null) { invalidateScrimCaches(); emit(Result.success(mapDtoToScrim(u))) } else emit(Result.failure(Exception("Transition failed"))) }
            else emit(Result.failure(Exception("Error transitioning scrim")))
        } catch (e: Exception) { emit(Result.failure(e)) }
    }

    override suspend fun markReady(scrimId: String, teamId: String): Flow<Result<Scrim>> = flow {
        try {
            val sr = api.getScrimById(PostgrestFilter.eq(scrimId))
            if (!sr.isSuccessful) { emit(Result.failure(Exception("Failed to fetch scrim"))); return@flow }
            val existing = sr.body()?.firstOrNull() ?: run { emit(Result.failure(Exception("Scrim not found"))); return@flow }
            val participantIds = listOfNotNull(existing.teamId, existing.opponentTeamId)
            AuthorizationUtils.requireParticipant(participantIds, "mark ready for this scrim")
                .onFailure { emit(Result.failure(it)); return@flow }

            val isTeamA = existing.teamId == teamId
            val nowIso = DateUtils.formatIsoUtc(System.currentTimeMillis())
            val updates = mutableMapOf<String, Any>()
            if (isTeamA) { updates["team_a_ready"] = true; updates["team_a_ready_at"] = nowIso } else { updates["team_b_ready"] = true; updates["team_b_ready_at"] = nowIso }
            if (if (isTeamA) true && existing.teamBReady else existing.teamAReady && true) updates["status"] = "IN_PROGRESS"
            val r = api.updateScrim(PostgrestFilter.eq(scrimId), updates)
            if (r.isSuccessful) { val u = r.body()?.firstOrNull(); if (u != null) { invalidateScrimCaches(); emit(Result.success(mapDtoToScrim(u))) } else emit(Result.failure(Exception("Mark ready failed"))) }
            else emit(Result.failure(Exception("Error marking ready: ${r.errorBody()?.string()}")))
        } catch (e: Exception) { emit(Result.failure(e)) }
    }

    override suspend fun uploadScreenshot(scrimId: String, teamId: String, screenshotUrl: String): Flow<Result<Scrim>> = flow {
        try {
            val sr = api.getScrimById(PostgrestFilter.eq(scrimId))
            if (!sr.isSuccessful) { emit(Result.failure(Exception("Failed to fetch scrim for upload"))); return@flow }
            val existing = sr.body()?.firstOrNull() ?: run { emit(Result.failure(Exception("Scrim not found"))); return@flow }
            val participantIds = listOfNotNull(existing.teamId, existing.opponentTeamId)
            AuthorizationUtils.requireParticipant(participantIds, "upload screenshots for this scrim")
                .onFailure { emit(Result.failure(it)); return@flow }

            val isTeamA = existing.teamId == teamId
            val nowIso = DateUtils.formatIsoUtc(System.currentTimeMillis())
            val updates = mutableMapOf<String, Any>()
            if (isTeamA) { updates["team_a_screenshot_url"] = screenshotUrl; updates["team_a_screenshot_uploaded_at"] = nowIso } else { updates["team_b_screenshot_url"] = screenshotUrl; updates["team_b_screenshot_uploaded_at"] = nowIso }
            val r = api.updateScrim(PostgrestFilter.eq(scrimId), updates)
            if (r.isSuccessful) { val u = r.body()?.firstOrNull(); if (u != null) { invalidateScrimCaches(); emit(Result.success(mapDtoToScrim(u))) } else emit(Result.failure(Exception("Upload succeeded but no data returned"))) }
            else emit(Result.failure(Exception("Error uploading screenshot: ${r.errorBody()?.string()}")))
        } catch (e: Exception) { emit(Result.failure(e)) }
    }

    override suspend fun completeScrim(scrimId: String, winnerTeamId: String): Flow<Result<Scrim>> = flow {
        try {
            // Ownership: only participating teams may complete the scrim
            val scrimResponse = api.getScrimById(PostgrestFilter.eq(scrimId))
            val existingScrim = scrimResponse.body()?.firstOrNull()
            if (existingScrim == null) { emit(Result.failure(Exception("Scrim not found"))); return@flow }
            val participantIds = listOfNotNull(existingScrim.teamId, existingScrim.opponentTeamId)
            AuthorizationUtils.requireParticipant(participantIds, "complete this scrim")
                .onFailure { emit(Result.failure(it)); return@flow }

            val r = api.updateScrim(PostgrestFilter.eq(scrimId), mapOf("status" to "COMPLETED", "winner_team_id" to winnerTeamId))
            if (!r.isSuccessful) { emit(Result.failure(Exception("Error completing scrim: ${r.errorBody()?.string()}"))); return@flow }
            val updated = r.body()?.firstOrNull() ?: run { emit(Result.failure(Exception("Complete failed: no data returned"))); return@flow }
            var matchId: String? = null
            try {
                val em = api.getMatches(scrimId = scrimId).body()?.firstOrNull()
                if (em != null) { matchId = em.id } else {
                    val teamBId = updated.opponentTeamId
                    if (teamBId != null) { val mr = api.createMatch(MatchDto(scrimId = scrimId, teamAId = updated.teamId, teamBId = teamBId, scheduledDate = updated.scheduledDate, scheduledTime = updated.scheduledTime, status = "Completed")); if (mr.isSuccessful) matchId = mr.body()?.firstOrNull()?.id }
                }
            } catch (e: Exception) { Timber.w("ScrimRepo", "Failed to find/create match", e) }
            try { if (matchId != null) { val emr = api.getMatchResults(PostgrestFilter.eq(matchId)); val existing = emr.body()?.firstOrNull(); if (existing == null) api.createMatchResult(MatchResultDto(matchId = matchId, winnerTeamId = winnerTeamId)) else api.updateMatchResult(PostgrestFilter.eq(existing.id), mapOf("winner_team_id" to winnerTeamId)) } } catch (e: Exception) { Timber.w("ScrimRepo", "Failed to create/update match result", e) }
            try { api.awardScrimPoints(mapOf("p_scrim_id" to scrimId, "p_winner_team_id" to winnerTeamId, "p_pts_per_win" to SupabaseMatchResultRepository.WIN_POINTS, "p_pts_per_loss" to SupabaseMatchResultRepository.LOSS_POINTS_ABS)) } catch (e: Exception) { Timber.w("ScrimRepo", "Failed to award scrim points", e) }
            invalidateScrimCaches()
            emit(Result.success(mapDtoToScrim(updated)))
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
            // Ownership: only participating teams may submit results
            val scrimResponse = api.getScrimById(PostgrestFilter.eq(scrimId))
            val existingScrim = scrimResponse.body()?.firstOrNull()
            if (existingScrim == null) { emit(Result.failure(Exception("Scrim not found"))); return@flow }
            val participantIds = listOfNotNull(existingScrim.teamId, existingScrim.opponentTeamId)
            AuthorizationUtils.requireParticipant(participantIds, "submit results for this scrim")
                .onFailure { emit(Result.failure(it)); return@flow }

            val r = api.updateScrim(PostgrestFilter.eq(scrimId), mapOf("status" to "COMPLETED", "winner_team_id" to winnerTeamId))
            if (r.isSuccessful) { val u = r.body()?.firstOrNull(); if (u != null) { invalidateScrimCaches(); emit(Result.success(mapDtoToScrim(u))) } else emit(Result.failure(Exception("Submit result failed"))) }
            else emit(Result.failure(Exception("Error submitting result")))
        } catch (e: Exception) { emit(Result.failure(e)) }
    }

    override suspend fun createAutoCancelledRecord(scrimId: String): Flow<Result<Unit>> = flow {
        try {
            // Mark scrim as auto-cancelled and notify participants
            val r = api.updateScrim(
                PostgrestFilter.eq(scrimId),
                mapOf("status" to "CANCELLED", "cancelled_at" to DateUtils.formatIsoNow(), "cancellation_reason" to "Auto-cancelled: opponent no-show")
            )
            if (r.isSuccessful) {
                invalidateScrimCaches()
                emit(Result.success(Unit))
            } else {
                emit(Result.failure(Exception("Failed to auto-cancel scrim")))
            }
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
                        emit(mapDtoToScrim(dto))
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
                    val record = event.record
                    if (record != null) {
                        val dto = parseRealtimeRecordToScrimDto(record)
                        invalidateScrimCaches()
                        emit(mapDtoToScrim(dto))
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
            scheduledDate = DateUtils.formatDate(scrim.scheduledTime),
            scheduledTime = DateUtils.formatTime(scrim.scheduledTime),
            bestOf = scrim.bestOf.games,
            status = scrim.status.name,
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
            region = scrim.region.displayName,
            skillLevel = scrim.skillLevel.name,
            maxPlayers = scrim.maxPlayers,
            currentPlayers = scrim.currentPlayers
        )
    }

    private fun mapDtoToScrim(dto: ScrimDto): Scrim {
        val scheduledTime = DateUtils.parseIsoToMillis("${dto.scheduledDate}T${dto.scheduledTime}")
        val parseTs = { raw: String? -> DateUtils.parseIsoToMillis(raw) }
        return Scrim(
            id = dto.id,
            teamId = dto.teamId,
            teamName = dto.opponentTeamName ?: "",
            teamLeader = "",
            gameMode = try { GameMode.valueOf(dto.gameMode) } catch (_: Exception) { GameMode.RANKED },
            region = try { Region.fromDisplayName(dto.region) } catch (_: Exception) { Region.EU },
            skillLevel = try { SkillLevel.valueOf(dto.skillLevel) } catch (_: Exception) { SkillLevel.ALL },
            bestOf = BestOf.fromGames(dto.bestOf),
            scheduledTime = scheduledTime,
            maxPlayers = dto.maxPlayers,
            currentPlayers = dto.currentPlayers,
            status = ScrimStatus.valueOf(dto.status),
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
            teamBScreenshotUploadedAt = parseTs(dto.teamBScreenshotUploadedAt)
        )
    }

    private fun mapScrimToEntity(scrim: Scrim): ScrimEntity {
        return ScrimEntity(
            id = scrim.id, teamId = scrim.teamId,
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
            gameMode = scrim.gameMode.name, region = scrim.region.displayName,
            skillLevel = scrim.skillLevel.name,
            maxPlayers = scrim.maxPlayers, currentPlayers = scrim.currentPlayers
        )
    }

    private fun mapEntityToScrim(e: ScrimEntity): Scrim {
        val scheduledTime = DateUtils.parseIsoToMillis("${e.scheduledDate}T${e.scheduledTime}")
        val parseTs = { raw: String? -> DateUtils.parseIsoToMillis(raw) }
        return Scrim(
            id = e.id, teamId = e.teamId,
            teamName = e.opponentTeamName ?: "", teamLeader = "",
            gameMode = try { GameMode.valueOf(e.gameMode) } catch (_: Exception) { GameMode.RANKED },
            region = try { Region.fromDisplayName(e.region) } catch (_: Exception) { Region.EU },
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
            teamAScreenshotUrl = e.teamAScreenshotUrl, teamBScreenshotUrl = e.teamBScreenshotUrl
        )
    }
}

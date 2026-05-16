package com.mlbb.scrim.data.repository

import com.mlbb.scrim.data.cache.UnifiedCacheManager
import com.mlbb.scrim.data.local.ScrimDao
import com.mlbb.scrim.data.local.ScrimEntity
import com.mlbb.scrim.data.model.*
import com.mlbb.scrim.data.service.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.text.SimpleDateFormat
import java.util.*

/**
 * Supabase-backed scrim repository with caching.
 * Memory TTL: 2 min | Room TTL: 10 min
 */
class SupabaseScrimRepository(
    private val cacheManager: UnifiedCacheManager,
    private val scrimDao: ScrimDao
) : ScrimRepositoryInterface {

    private val api = SupabaseService.api

    companion object {
        const val PTS_PER_WIN = 25
        const val PTS_PER_LOSS = 15
        private const val CACHE_KEY_ALL = "scrims_all"
        private const val CACHE_KEY_TEAM_PREFIX = "scrims_team_"
        private const val MEM_TTL = 2L * 60 * 1000
        private const val ROOM_TTL = 10L * 60 * 1000
    }

    private suspend fun invalidateScrimCaches() {
        cacheManager.invalidateByPrefix("scrims_")
    }

    override fun getAllScrims(): Flow<Result<List<Scrim>>> = flow {
        try {
            cacheManager.getFlow<List<Scrim>>(
                key = CACHE_KEY_ALL, memoryTtlMs = MEM_TTL, roomTtlMs = ROOM_TTL,
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
            val r = api.updateScrimApplication(PostgrestFilter.eq(applicationId), mapOf("status" to "REJECTED"))
            if (r.isSuccessful) { invalidateScrimCaches(); getScrimById(scrimId).collect { result -> emit(result.map { it ?: throw Exception("Scrim not found") }) } }
            else emit(Result.failure(Exception("Failed to reject application")))
        } catch (e: Exception) { emit(Result.failure(e)) }
    }

    override suspend fun cancelApplication(scrimId: String, applicationId: String): Flow<Result<Scrim>> = flow {
        try {
            val r = api.updateScrimApplication(PostgrestFilter.eq(applicationId), mapOf("status" to "CANCELLED"))
            if (r.isSuccessful) { invalidateScrimCaches(); getScrimById(scrimId).collect { result -> emit(result.map { it ?: throw Exception("Scrim not found") }) } }
            else emit(Result.failure(Exception("Failed to cancel application")))
        } catch (e: Exception) { emit(Result.failure(e)) }
    }

    override suspend fun setScrimRoster(scrimId: String, teamId: String, roster: List<ScrimRosterEntry>): Flow<Result<Scrim>> = flow {
        try {
            val existing = api.getScrimRosters(PostgrestFilter.eq(scrimId), PostgrestFilter.eq(teamId))
            if (existing.isSuccessful) { existing.body()?.forEach { api.deleteScrimRosterEntry(PostgrestFilter.eq(scrimId), PostgrestFilter.eq(teamId), PostgrestFilter.eq(it.userId)) } }
            roster.forEach { entry -> api.createScrimRosterEntry(ScrimRosterDto(scrimId = scrimId, teamId = teamId, userId = entry.playerId, isActive = entry.isActive)) }
            invalidateScrimCaches()
            getScrimById(scrimId).collect { result -> emit(result.map { it ?: throw Exception("Scrim not found") }) }
        } catch (e: Exception) { emit(Result.failure(e)) }
    }

    override suspend fun transitionToReadyCheck(scrimId: String): Flow<Result<Scrim>> = flow {
        try {
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
            val isTeamA = existing.teamId == teamId
            val nowIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())
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
            val isTeamA = existing.teamId == teamId
            val nowIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())
            val updates = mutableMapOf<String, Any>()
            if (isTeamA) { updates["team_a_screenshot_url"] = screenshotUrl; updates["team_a_screenshot_uploaded_at"] = nowIso } else { updates["team_b_screenshot_url"] = screenshotUrl; updates["team_b_screenshot_uploaded_at"] = nowIso }
            val r = api.updateScrim(PostgrestFilter.eq(scrimId), updates)
            if (r.isSuccessful) { val u = r.body()?.firstOrNull(); if (u != null) { invalidateScrimCaches(); emit(Result.success(mapDtoToScrim(u))) } else emit(Result.failure(Exception("Upload succeeded but no data returned"))) }
            else emit(Result.failure(Exception("Error uploading screenshot: ${r.errorBody()?.string()}")))
        } catch (e: Exception) { emit(Result.failure(e)) }
    }

    override suspend fun completeScrim(scrimId: String, winnerTeamId: String): Flow<Result<Scrim>> = flow {
        try {
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
            } catch (_: Exception) { }
            try { if (matchId != null) { val emr = api.getMatchResults(PostgrestFilter.eq(matchId)); if (emr.body().isNullOrEmpty()) api.createMatchResult(MatchResultDto(matchId = matchId, winnerTeamId = winnerTeamId)) else api.updateMatchResult(PostgrestFilter.eq(emr.body()!!.first().id), mapOf("winner_team_id" to winnerTeamId)) } } catch (_: Exception) { }
            try { api.awardScrimPoints(mapOf("p_scrim_id" to scrimId, "p_winner_team_id" to winnerTeamId, "p_pts_per_win" to SupabaseMatchResultRepository.WIN_POINTS, "p_pts_per_loss" to SupabaseMatchResultRepository.LOSS_POINTS_ABS)) } catch (_: Exception) { }
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
            val r = api.updateScrim(PostgrestFilter.eq(scrimId), mapOf("status" to "COMPLETED", "winner_team_id" to winnerTeamId))
            if (r.isSuccessful) { val u = r.body()?.firstOrNull(); if (u != null) { invalidateScrimCaches(); emit(Result.success(mapDtoToScrim(u))) } else emit(Result.failure(Exception("Submit result failed"))) }
            else emit(Result.failure(Exception("Error submitting result")))
        } catch (e: Exception) { emit(Result.failure(e)) }
    }

    override suspend fun createAutoCancelledRecord(scrimId: String): Flow<Result<Unit>> = flow { emit(Result.success(Unit)) }

    // ─── Mapping ───

    private fun mapScrimToDto(scrim: Scrim): ScrimDto {
        val df = SimpleDateFormat("yyyy-MM-dd", Locale.US); val tf = SimpleDateFormat("HH:mm:ss", Locale.US); val d = Date(scrim.scheduledTime)
        return ScrimDto(id = scrim.id.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString(), teamId = scrim.teamId, scheduledDate = df.format(d), scheduledTime = tf.format(d), bestOf = scrim.bestOf.games, status = scrim.status.name, description = scrim.description.takeIf { it.isNotBlank() }, opponentTeamId = scrim.opponentTeamId, opponentTeamName = scrim.opponentTeamName, winnerTeamId = scrim.winnerTeamId, teamAReady = scrim.teamAReady, teamBReady = scrim.teamBReady, teamAScreenshotUrl = scrim.teamAScreenshotUrl, teamBScreenshotUrl = scrim.teamBScreenshotUrl, teamAScreenshotUploadedAt = scrim.teamAScreenshotUploadedAt?.let { df.format(Date(it)) + " " + tf.format(Date(it)) }, teamBScreenshotUploadedAt = scrim.teamBScreenshotUploadedAt?.let { df.format(Date(it)) + " " + tf.format(Date(it)) })
    }

    private fun mapDtoToScrim(dto: ScrimDto): Scrim {
        val scheduledTime = try { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).parse("${dto.scheduledDate} ${dto.scheduledTime}")?.time ?: System.currentTimeMillis() } catch (_: Exception) { System.currentTimeMillis() }
        val parseTs = { raw: String? -> raw?.let { try { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).parse(it)?.time ?: SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).parse(it)?.time } catch (_: Exception) { null } } }
        return Scrim(id = dto.id, teamId = dto.teamId, teamName = dto.opponentTeamName ?: "", teamLeader = "", bestOf = BestOf.fromGames(dto.bestOf), scheduledTime = scheduledTime, status = ScrimStatus.valueOf(dto.status), description = dto.description ?: "", opponentTeamId = dto.opponentTeamId, opponentTeamName = dto.opponentTeamName, winnerTeamId = dto.winnerTeamId, teamAReady = dto.teamAReady, teamBReady = dto.teamBReady, teamAScreenshotUrl = dto.teamAScreenshotUrl, teamBScreenshotUrl = dto.teamBScreenshotUrl, teamAScreenshotUploadedAt = parseTs(dto.teamAScreenshotUploadedAt), teamBScreenshotUploadedAt = parseTs(dto.teamBScreenshotUploadedAt))
    }

    private fun mapScrimToEntity(scrim: Scrim): ScrimEntity {
        val df = SimpleDateFormat("yyyy-MM-dd", Locale.US); val tf = SimpleDateFormat("HH:mm:ss", Locale.US); val d = Date(scrim.scheduledTime)
        return ScrimEntity(id = scrim.id, teamId = scrim.teamId, scheduledDate = df.format(d), scheduledTime = tf.format(d), bestOf = scrim.bestOf.games, status = scrim.status.name, description = scrim.description, opponentTeamId = scrim.opponentTeamId, opponentTeamName = scrim.opponentTeamName, winnerTeamId = scrim.winnerTeamId, teamAReady = scrim.teamAReady, teamBReady = scrim.teamBReady, teamAScreenshotUrl = scrim.teamAScreenshotUrl, teamBScreenshotUrl = scrim.teamBScreenshotUrl)
    }

    private fun mapEntityToScrim(e: ScrimEntity): Scrim {
        val scheduledTime = try { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).parse("${e.scheduledDate} ${e.scheduledTime}")?.time ?: System.currentTimeMillis() } catch (_: Exception) { System.currentTimeMillis() }
        return Scrim(id = e.id, teamId = e.teamId, teamName = e.opponentTeamName ?: "", teamLeader = "", bestOf = BestOf.fromGames(e.bestOf), scheduledTime = scheduledTime, status = try { ScrimStatus.valueOf(e.status) } catch (_: Exception) { ScrimStatus.OPEN }, description = e.description ?: "", opponentTeamId = e.opponentTeamId, opponentTeamName = e.opponentTeamName, winnerTeamId = e.winnerTeamId, teamAReady = e.teamAReady, teamBReady = e.teamBReady, teamAScreenshotUrl = e.teamAScreenshotUrl, teamBScreenshotUrl = e.teamBScreenshotUrl)
    }
}

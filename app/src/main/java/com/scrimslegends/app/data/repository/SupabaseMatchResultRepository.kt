package com.scrimslegends.app.data.repository

import com.scrimslegends.app.data.cache.ProfileCacheRepository
import com.scrimslegends.app.data.model.*
import com.scrimslegends.app.data.service.*
import com.scrimslegends.app.util.DateUtils
import timber.log.Timber
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Supabase-backed match result repository.
 *
 * CACHE OPTIMIZATION: Uses ProfileCacheRepository for batch profile lookups
 * instead of individual API calls per roster entry (fixes N+1 query explosion).
 *
 * Before: 12+ API calls per match result (1 scrim + 1 roster + N profiles)
 * After:  3 API calls per match result (1 scrim + 1 roster + 1 batch profiles)
 */
class SupabaseMatchResultRepository(
    private val profileCache: ProfileCacheRepository
) : MatchResultRepositoryInterface {

    companion object {
        // Points values: WIN_POINTS is added, LOSS_POINTS_ABS is the magnitude passed to the RPC.
        // P0-4 FIX: The award_scrim_points RPC negates the loss internally (-p_pts_per_loss),
        // so we must pass a POSITIVE value. We no longer pass -15; we pass 15.
        const val WIN_POINTS = 25
        const val LOSS_POINTS = -15                  // used for local UI display only
        const val LOSS_POINTS_ABS = 15               // used when calling the RPC
    }

    private val api = SupabaseService.api

    // ─── Team name cache (avoids repeated fetchTeamName calls) ───
    // HARDENED: Bounded LRU cache (max 50 entries) to prevent unbounded growth
    private val teamNameCache = object : LinkedHashMap<String, String>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>?): Boolean {
            return size > 50
        }
    }

    override suspend fun getAllMatchResults(): Flow<Result<List<MatchResult>>> = flow {
        try {
            // Fetch scrim match results
            val scrimResults = mutableListOf<MatchResult>()
            val response = api.getScrims(status = "COMPLETED", order = "created_at.desc")
            if (response.isSuccessful) {
                val scrims = response.body() ?: emptyList()
                scrimResults.addAll(scrims.map { mapScrimToMatchResult(it) })
            }

            // Fetch completed tournament Swiss matches
            val tournamentResults = mutableListOf<MatchResult>()
            try {
                val swissResponse = api.getTournamentSwissMatches(
                    select = "*,tournaments(id,title)",
                    order = "created_at.desc"
                )
                if (swissResponse.isSuccessful) {
                    val swissMatches = swissResponse.body() ?: emptyList()
                    for (match in swissMatches) {
                        val status = match["status"]?.toString() ?: ""
                        if (status == "completed") {
                            val tournamentMap = match["tournaments"] as? Map<String, Any?>
                            val tournamentTitle = tournamentMap?.get("title")?.toString()
                            tournamentResults.add(mapSwissMatchToMatchResult(match, tournamentTitle))
                        }
                    }
                }
            } catch (e: Exception) { Timber.w("MatchResultRepo", "Failed to fetch tournament matches for history", e) }

            // Merge and sort by createdAt descending
            val allResults = (scrimResults + tournamentResults).sortedByDescending { it.createdAt }
            emit(Result.success(allResults))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    override suspend fun getMatchResultById(id: String): Flow<Result<MatchResult?>> = flow {
        try {
            // Try to find by match_results.id first
            val mrResponse = api.getMatchResults(PostgrestFilter.eq(id))
            if (mrResponse.isSuccessful && mrResponse.body()?.isNotEmpty() == true) {
                val mr = mrResponse.body()!!.first()
                // match_results.match_id now references matches.id, not scrims.id
                val matchResponse = api.getMatchById(PostgrestFilter.eq(mr.matchId))
                val matchDto = matchResponse.body()?.firstOrNull()
                val scrim = if (matchDto != null) {
                    api.getScrimById(PostgrestFilter.eq(matchDto.scrimId)).body()?.firstOrNull()
                } else null
                if (scrim != null) {
                    emit(Result.success(mapScrimToMatchResult(scrim, mr)))
                } else {
                    emit(Result.success(null))
                }
            } else {
                // Fallback: find by match id -> scrim
                val matchResponse = api.getMatchById(PostgrestFilter.eq(id))
                val matchDto = matchResponse.body()?.firstOrNull()
                val scrim = if (matchDto != null) {
                    api.getScrimById(PostgrestFilter.eq(matchDto.scrimId)).body()?.firstOrNull()
                } else null
                if (scrim != null) {
                    emit(Result.success(mapScrimToMatchResult(scrim)))
                } else {
                    emit(Result.success(null))
                }
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    override suspend fun getMatchResultsForScrim(scrimId: String): Flow<Result<MatchResult?>> = flow {
        try {
            // Find the match by scrim_id, then its match_result
            val matchResponse = api.getMatches(scrimId = scrimId)
            val matchDto = matchResponse.body()?.firstOrNull()
            if (matchDto != null) {
                val mrResponse = api.getMatchResults(PostgrestFilter.eq(matchDto.id))
                val mr = mrResponse.body()?.firstOrNull()
                val scrimResponse = api.getScrimById(PostgrestFilter.eq(scrimId))
                val scrim = scrimResponse.body()?.firstOrNull()
                if (scrim != null) {
                    emit(Result.success(mapScrimToMatchResult(scrim, mr)))
                } else {
                    emit(Result.success(null))
                }
            } else {
                emit(Result.success(null))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    override suspend fun getMatchResultsForTeam(teamId: String): Flow<Result<List<MatchResult>>> = flow {
        try {
            // HARDENED: Server-side filter instead of fetching ALL matches
            val matchResponse = api.getMatchesForTeam(
                orFilter = "(team_a_id.eq.$teamId,team_b_id.eq.$teamId)"
            )
            if (matchResponse.isSuccessful) {
                val matches = matchResponse.body() ?: emptyList()
                if (matches.isEmpty()) {
                    emit(Result.success(emptyList()))
                    return@flow
                }

                // Batch-fetch all scrims by their IDs in ONE call
                val scrimIds = matches.map { it.scrimId }.distinct()
                val scrims = if (scrimIds.isNotEmpty()) {
                    api.getScrimsByIds(PostgrestFilter.inList(scrimIds)).body() ?: emptyList()
                } else emptyList()
                val scrimMap = scrims.associateBy { it.id }

                // Batch-fetch all match_results by match IDs in ONE call
                val matchIds = matches.map { it.id }.distinct()
                val matchResults = if (matchIds.isNotEmpty()) {
                    api.getMatchResultsByMatchIds(PostgrestFilter.inList(matchIds)).body() ?: emptyList()
                } else emptyList()
                val mrMap = matchResults.associateBy { it.matchId }

                // Map everything without individual API calls
                val results = matches.mapNotNull { matchDto ->
                    val scrim = scrimMap[matchDto.scrimId]
                    val mr = mrMap[matchDto.id]
                    if (scrim != null) mapScrimToMatchResult(scrim, mr) else null
                }
                emit(Result.success(results))
            } else {
                emit(Result.failure(Exception("Failed to fetch team match results")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
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
        try {
            // Fetch the scrim
            val scrimResponse = api.getScrimById(PostgrestFilter.eq(scrimId))
            if (!scrimResponse.isSuccessful) {
                emit(Result.failure(Exception("Failed to fetch scrim for reporting")))
                return@flow
            }
            val scrim = scrimResponse.body()?.firstOrNull()
            if (scrim == null) {
                emit(Result.failure(Exception("Scrim not found")))
                return@flow
            }

            // P0-1 FIX: Resolve the matches.id for this scrim — match_results.match_id FK requires it
            val matchId = resolveOrCreateMatchId(scrim)
            if (matchId == null) {
                emit(Result.failure(Exception("Could not resolve match record for scrim")))
                return@flow
            }

            // Update or create the match_result row, referencing matches.id (not scrims.id)
            val mrResponse = api.getMatchResults(PostgrestFilter.eq(matchId))
            val existingMr = mrResponse.body()?.firstOrNull()
            val isTeamA = scrim.teamId == teamId

            if (existingMr != null) {
                api.updateMatchResult(
                    PostgrestFilter.eq(existingMr.id),
                    mutableMapOf<String, Any>("winner_team_id" to reportedWinnerId).apply {
                        screenshotUrl?.let {
                            if (isTeamA) put("team_a_screenshot_url", it) else put("team_b_screenshot_url", it)
                        }
                    }
                )
            } else {
                api.createMatchResult(
                    MatchResultDto(
                        matchId = matchId,  // ← correct FK to matches.id
                        winnerTeamId = reportedWinnerId,
                        teamAScreenshotUrl = if (isTeamA) screenshotUrl else null,
                        teamBScreenshotUrl = if (!isTeamA) screenshotUrl else null
                    )
                )
            }

            emit(Result.success(mapScrimToMatchResult(scrim)))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    override suspend fun createMatchResult(
        scrimId: String,
        teamAId: String,
        teamAName: String,
        teamBId: String,
        teamBName: String
    ): Flow<Result<MatchResult>> = flow {
        try {
            // Fetch the scrim first
            val scrimResp = api.getScrimById(PostgrestFilter.eq(scrimId))
            val scrim = scrimResp.body()?.firstOrNull()
                ?: run { emit(Result.failure(Exception("Scrim not found"))); return@flow }

            // P0-1 FIX: create/resolve a match row and use its id as the FK
            val matchId = resolveOrCreateMatchId(scrim)
                ?: run { emit(Result.failure(Exception("Could not create match record"))); return@flow }

            val response = api.createMatchResult(
                MatchResultDto(matchId = matchId)  // ← correct FK to matches.id
            )
            if (response.isSuccessful) {
                emit(Result.success(mapScrimToMatchResult(scrim)))
            } else {
                emit(Result.failure(Exception("Failed to create match result")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    /**
     * P0-1 HELPER: Fetches the match row for a scrim, or creates one if it doesn't exist.
     * Returns the matches.id to use as FK for match_results and messages.
     */
    private suspend fun resolveOrCreateMatchId(scrim: ScrimDto): String? {
        return try {
            // Check if a match already exists for this scrim
            val existing = api.getMatches(scrimId = PostgrestFilter.eq(scrim.id)).body()?.firstOrNull()
            if (existing != null) {
                existing.id
            } else {
                // Create the match row
                val created = api.createMatch(
                    MatchDto(
                        scrimId = scrim.id,
                        teamAId = scrim.teamId,
                        teamBId = scrim.opponentTeamId ?: "",
                        status = "IN_PROGRESS"
                    )
                ).body()?.firstOrNull()
                created?.id
            }
        } catch (e: Exception) { Timber.w("MatchResultRepo", "Failed to create match record", e); null }
    }

    override suspend fun resolveDispute(
        matchResultId: String,
        confirmedWinnerId: String,
        adminNotes: String?
    ): Flow<Result<MatchResult>> = flow {
        try {
            // HARDENED: Verify caller is an admin before resolving disputes
            val currentUserId = com.scrimslegends.app.data.service.SupabaseSession.getUserIdOrNull()
            val isAdmin = currentUserId?.let { uid ->
                try {
                    api.getProfileById(id = "eq.$uid").body()?.firstOrNull()?.isAdmin == true
                } catch (_: Exception) { false }
            } ?: false
            if (!isAdmin) {
                emit(Result.failure(Exception("Admin authorization required to resolve disputes")))
                return@flow
            }

            val mrResponse = api.getMatchResults(PostgrestFilter.eq(matchResultId))
            val mr = mrResponse.body()?.firstOrNull()
            if (mr != null) {
                api.updateMatchResult(
                    PostgrestFilter.eq(mr.id),
                    mutableMapOf<String, Any>(
                        "winner_team_id" to confirmedWinnerId,
                        "admin_verified" to true
                    ).apply {
                        adminNotes?.let { put("verification_notes", it) }
                    }
                )
            }
            // BUGFIX: resolve scrim through match_results -> matches -> scrims chain
            val scrim = if (mr != null) {
                val matchResp = api.getMatchById(PostgrestFilter.eq(mr.matchId))
                val matchDto = matchResp.body()?.firstOrNull()
                if (matchDto != null) {
                    api.getScrimById(PostgrestFilter.eq(matchDto.scrimId)).body()?.firstOrNull()
                } else null
            } else null
            if (scrim != null) {
                emit(Result.success(mapScrimToMatchResult(scrim, mr)))
            } else {
                emit(Result.failure(Exception("Match result not found")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    override suspend fun uploadScreenshot(
        matchResultId: String,
        screenshotUrl: String
    ): Flow<Result<MatchResult>> = flow {
        try {
            val mrResponse = api.getMatchResults(PostgrestFilter.eq(matchResultId))
            val mr = mrResponse.body()?.firstOrNull()
            if (mr != null) {
                val currentUserId = com.scrimslegends.app.data.service.SupabaseSession.getUserIdOrNull()
                // Fetch match to determine which team the user is on
                val matchResponse = api.getMatches(PostgrestFilter.eq(mr.matchId))
                val match = matchResponse.body()?.firstOrNull()
                val isTeamA = match != null && currentUserId != null && isUserInTeam(match.teamAId, currentUserId)
                val column = if (isTeamA) "team_a_screenshot_url" else "team_b_screenshot_url"
                api.updateMatchResult(
                    PostgrestFilter.eq(mr.id),
                    mapOf(column to screenshotUrl)
                )
            }
            val scrimResp = api.getScrimById(PostgrestFilter.eq(matchResultId))
            val scrim = scrimResp.body()?.firstOrNull()
            if (scrim != null) {
                emit(Result.success(mapScrimToMatchResult(scrim)))
            } else {
                emit(Result.failure(Exception("Match result not found")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    private suspend fun isUserInTeam(teamId: String, userId: String): Boolean {
        return try {
            val members = api.getTeamMembers(PostgrestFilter.eq(teamId))
            members.body()?.any { it.userId == userId } == true
        } catch (e: Exception) { Timber.w("MatchResultRepo", "Failed to check team membership", e); false }
    }

    // ─── Mapping (N+1 FIXED: uses batch profile cache) ───

    private suspend fun mapScrimToMatchResult(scrimDto: ScrimDto, mrDto: MatchResultDto? = null): MatchResult {
        val isTeamA = scrimDto.winnerTeamId == scrimDto.teamId

        // Fetch team names (with local cache to avoid redundant calls)
        val teamAName = fetchTeamNameCached(scrimDto.teamId)
        val teamBName = scrimDto.opponentTeamId?.let { fetchTeamNameCached(it) } ?: scrimDto.opponentTeamName ?: ""

        // Fetch rosters for this scrim (single API call)
        val rosterEntries = try {
            api.getScrimRosters(PostgrestFilter.eq(scrimDto.id)).body() ?: emptyList()
        } catch (e: Exception) { Timber.w("MatchResultRepo", "Failed to load scrim rosters", e); emptyList() }

        val winnerTeamId = scrimDto.winnerTeamId ?: mrDto?.winnerTeamId

        // ── N+1 FIX: Batch-fetch ALL profiles for the roster in ONE call ──
        val allUserIds = rosterEntries.map { it.userId }
        val profileMap = profileCache.getProfiles(allUserIds)

        val teamARoster = rosterEntries
            .filter { it.teamId == scrimDto.teamId }
            .map { entry ->
                val username = profileMap[entry.userId]?.username ?: entry.userId.take(8)
                val isWinner = entry.teamId == winnerTeamId
                RosterPlayerInfo(
                    playerId = entry.userId,
                    playerName = username,
                    role = if (entry.isActive) "Active" else "Sub",
                    isActive = entry.isActive,
                    isWinner = isWinner,
                    pointsChange = if (entry.isActive) (if (isWinner) WIN_POINTS else LOSS_POINTS) else 0
                )
            }

        val teamBRoster = rosterEntries
            .filter { it.teamId == scrimDto.opponentTeamId }
            .map { entry ->
                val username = profileMap[entry.userId]?.username ?: entry.userId.take(8)
                val isWinner = entry.teamId == winnerTeamId
                RosterPlayerInfo(
                    playerId = entry.userId,
                    playerName = username,
                    role = if (entry.isActive) "Active" else "Sub",
                    isActive = entry.isActive,
                    isWinner = isWinner,
                    pointsChange = if (entry.isActive) (if (isWinner) WIN_POINTS else LOSS_POINTS) else 0
                )
            }

        val teamAReport = winnerTeamId?.let {
            TeamReport(
                reporterId = scrimDto.teamId,
                reporterName = "",
                reportedWinnerId = if (isTeamA) scrimDto.teamId else scrimDto.opponentTeamId ?: "",
                reportedAt = System.currentTimeMillis()
            )
        }
        val teamBReport = winnerTeamId?.let {
            TeamReport(
                reporterId = scrimDto.opponentTeamId ?: "",
                reporterName = "",
                reportedWinnerId = if (!isTeamA) scrimDto.opponentTeamId ?: "" else scrimDto.teamId,
                reportedAt = System.currentTimeMillis()
            )
        }

        return MatchResult(
            id = mrDto?.id ?: scrimDto.id,
            scrimId = scrimDto.id,
            teamAId = scrimDto.teamId,
            teamAName = teamAName,
            teamBId = scrimDto.opponentTeamId ?: "",
            teamBName = teamBName,
            teamAReport = teamAReport,
            teamBReport = teamBReport,
            screenshotUrl = mrDto?.teamAScreenshotUrl ?: scrimDto.teamAScreenshotUrl,
            verificationStatus = when {
                mrDto?.adminVerified == true -> VerificationStatus.CONFIRMED
                scrimDto.winnerTeamId != null -> VerificationStatus.CONFIRMED
                else -> VerificationStatus.PENDING
            },
            confirmedWinnerId = winnerTeamId,
            adminNotes = mrDto?.verificationNotes,
            resolvedAt = mrDto?.let { DateUtils.parseIsoToMillis(it.createdAt) }
                ?: scrimDto.resultSubmittedAt?.let { DateUtils.parseIsoToMillis(it) },
            teamARoster = teamARoster,
            teamBRoster = teamBRoster
        )
    }

    /**
     * Cached team name lookup — avoids hitting the API for the same team ID multiple times.
     */
    private suspend fun fetchTeamNameCached(teamId: String): String {
        teamNameCache[teamId]?.let { return it }
        val name = try {
            api.getTeamById(PostgrestFilter.eq(teamId)).body()?.firstOrNull()?.name ?: ""
        } catch (e: Exception) { Timber.w("MatchResultRepo", "Failed to fetch team name", e); "" }
        if (name.isNotEmpty()) teamNameCache[teamId] = name
        return name
    }

    /**
     * Map a tournament Swiss match row (Map<String, Any?>) to MatchResult.
     * Used to show tournament matches in match history alongside scrims.
     */
    private suspend fun mapSwissMatchToMatchResult(
        match: Map<String, Any?>,
        tournamentTitle: String?
    ): MatchResult {
        val matchId = match["id"]?.toString() ?: ""
        val teamAId = match["team_a_id"]?.toString() ?: ""
        val teamBId = match["team_b_id"]?.toString() ?: ""
        val winnerTeamId = match["winner_team_id"]?.toString()
        val isDraw = match["is_draw"]?.toString()?.toBooleanStrictOrNull() == true
        val roundNumber = (match["round_number"]?.toString()?.toIntOrNull()) ?: 0
        val createdAtStr = match["created_at"]?.toString() ?: ""

        val teamAName = fetchTeamNameCached(teamAId)
        val teamBName = if (teamBId.isNotBlank()) fetchTeamNameCached(teamBId) else "BYE"

        val verificationStatus = when {
            winnerTeamId != null -> VerificationStatus.CONFIRMED
            isDraw -> VerificationStatus.CONFIRMED
            match["is_disputed"]?.toString()?.toBooleanStrictOrNull() == true -> VerificationStatus.DISPUTED
            else -> VerificationStatus.PENDING
        }

        return MatchResult(
            id = matchId,
            scrimId = match["tournament_id"]?.toString() ?: "",
            teamAId = teamAId,
            teamAName = teamAName,
            teamBId = teamBId,
            teamBName = teamBName,
            verificationStatus = verificationStatus,
            confirmedWinnerId = winnerTeamId,
            createdAt = DateUtils.parseIsoToMillis(createdAtStr),
            resolvedAt = if (winnerTeamId != null || isDraw) DateUtils.parseIsoToMillis(createdAtStr) else null,
            matchType = MatchType.TOURNAMENT,
            tournamentTitle = tournamentTitle,
            roundNumber = roundNumber
        )
    }

}

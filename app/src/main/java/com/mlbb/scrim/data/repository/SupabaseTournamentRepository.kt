package com.mlbb.scrim.data.repository

import com.mlbb.scrim.data.cache.UnifiedCacheManager
import com.mlbb.scrim.data.model.*
import com.mlbb.scrim.data.service.*
import com.mlbb.scrim.security.AuthorizationUtils
import android.util.Log
import kotlinx.coroutines.delay

/**
 * Supabase-backed tournament repository.
 * Uses REST API via Retrofit + RPC calls for mutations.
 */
class SupabaseTournamentRepository(
    private val cacheManager: UnifiedCacheManager
) : TournamentRepositoryInterface {

    private val api = SupabaseService.api

    companion object {
        private const val TAG = "TournamentRepo"
        private const val CACHE_KEY_LIST = "tournaments_list"
        private const val CACHE_KEY_DETAIL_PREFIX = "tournament_detail_"
        private const val CACHE_KEY_APPS_PREFIX = "tournament_apps_"
        private const val CACHE_KEY_MATCHES_PREFIX = "tournament_matches_"
        private const val MEM_TTL = 30_000L // 30 sec
        private const val ROOM_TTL = 120_000L // 2 min
        private const val MAX_RETRY = 3
        private const val RETRY_DELAY_MS = 500L
    }

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

    private suspend fun invalidateTournamentCaches() {
        cacheManager.invalidateByPrefix("tournaments_")
    }

    private suspend fun requireTournamentHost(tournamentId: String): Result<String> {
        val tournamentResponse = api.getTournamentById(PostgrestFilter.eq(tournamentId))
        val tournament = tournamentResponse.body()?.firstOrNull()
            ?: return Result.failure(Exception("Tournament not found"))
        val hostUserId = (tournament["host_user_id"] as? String) ?: ""
        return AuthorizationUtils.requireOwner(hostUserId, "manage this tournament")
    }

    // ── Tournament list ──────────────────────────────────────────

    override suspend fun getTournaments(
        status: String?,
        region: String?,
        skillLevel: String?
    ): Result<List<Tournament>> = try {
        withRetry {
            val statusFilter = status?.let { "eq.$it" } ?: "neq.draft"
            val response = api.getTournaments(
                select = "*,profiles:host_user_id(username)",
                status = statusFilter,
                region = region?.let { "eq.$it" },
                skillLevel = skillLevel?.let { "eq.$it" },
                order = "created_at.desc",
                range = "0-49"
            )
            if (response.isSuccessful) {
                val tournaments = response.body()?.map { mapDtoToTournament(it) } ?: emptyList()
                Result.success(tournaments)
            } else {
                Result.failure(Exception("Failed to fetch tournaments: ${response.code()}"))
            }
        }
    } catch (e: Exception) {
        Log.e(TAG, "Error fetching tournaments", e)
        Result.failure(e)
    }

    // ── Tournament detail ────────────────────────────────────────

    override suspend fun getTournamentById(tournamentId: String): Result<Tournament> = try {
        withRetry {
            val response = api.getTournamentById(
                id = PostgrestFilter.eq(tournamentId),
                select = "*,profiles:host_user_id(username)"
            )
            if (response.isSuccessful) {
                val dto = response.body()?.firstOrNull() ?: throw Exception("Tournament not found")
                Result.success(mapDtoToTournament(dto))
            } else {
                Result.failure(Exception("Tournament not found: ${response.code()}"))
            }
        }
    } catch (e: Exception) {
        Log.e(TAG, "Error fetching tournament $tournamentId", e)
        Result.failure(e)
    }

    // ── Requirements ─────────────────────────────────────────────

    override suspend fun getTournamentRequirements(tournamentId: String): Result<List<TournamentRequirement>> = try {
        withRetry {
            val response = api.getTournamentRequirements(
                tournamentId = PostgrestFilter.eq(tournamentId)
            )
            if (response.isSuccessful) {
                val reqs = response.body()?.map { mapDtoToRequirement(it) } ?: emptyList()
                Result.success(reqs)
            } else {
                Result.failure(Exception("Failed to fetch requirements"))
            }
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    // ── Tournament teams ────────────────────────────────────────

    override suspend fun getTournamentTeams(tournamentId: String): Result<List<TournamentTeam>> = try {
        withRetry {
            val response = api.getTournamentTeams(
                tournamentId = PostgrestFilter.eq(tournamentId),
                select = "*,teams:team_id(name)"
            )
            if (response.isSuccessful) {
                val teams = response.body()?.map { mapDtoToTournamentTeam(it) } ?: emptyList()
                Result.success(teams)
            } else {
                Result.failure(Exception("Failed to fetch tournament teams"))
            }
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    // ── Applications ─────────────────────────────────────────────

    override suspend fun getMyApplications(userId: String): Result<List<TournamentApplication>> = try {
        withRetry {
            // Get team IDs where user is a member
            val teamResponse = api.getTeamMembers(userId = PostgrestFilter.eq(userId), select = "team_id")
            if (!teamResponse.isSuccessful) {
                Result.failure<List<TournamentApplication>>(Exception("Failed to fetch teams"))
            } else {
                val teamIds = teamResponse.body()?.map { it.teamId }?.filter { it.isNotEmpty() }
                if (teamIds.isNullOrEmpty()) {
                    Result.success(emptyList())
                } else {
                    val response = api.getTournamentApplications(
                        teamId = PostgrestFilter.inList(teamIds),
                        select = "*,teams:team_id(name)",
                        order = "applied_at.desc"
                    )
                    if (response.isSuccessful) {
                        val apps = response.body()?.map { mapDtoToApplication(it) } ?: emptyList()
                        Result.success(apps)
                    } else {
                        Result.failure(Exception("Failed to fetch applications"))
                    }
                }
            }
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun applyForTournament(tournamentId: String, teamId: String): Result<Map<String, Any>> = try {
        val params = mapOf("p_tournament_id" to tournamentId, "p_team_id" to teamId)
        val response = api.rpcApplyForTournament(params)
        if (response.isSuccessful) {
            val result = response.body() ?: mapOf("success" to true)
            invalidateTournamentCaches()
            Result.success(result)
        } else {
            Result.failure(Exception("Failed to apply for tournament"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    // ── Host request ────────────────────────────────────────────

    override suspend fun submitHostRequest(
        motivation: String,
        experience: String?,
        telegramChannel: String?,
        socialLinks: List<String>
    ): Result<TournamentHostRequest> {
        val userId = SupabaseSession.getUserIdOrNull()
            ?: return Result.failure(Exception("Not authenticated"))
        return try {
            val body = mutableMapOf<String, Any>(
                "user_id" to userId,
                "motivation" to motivation,
                "social_links" to socialLinks
            )
            experience?.let { body["experience"] = it }
            telegramChannel?.let { body["telegram_channel"] = it }
            val response = api.insertTournamentHostRequest(body)
            if (response.isSuccessful) {
                val dto = response.body()?.firstOrNull() ?: throw Exception("No response")
                Result.success(mapDtoToHostRequest(dto))
            } else {
                Result.failure(Exception("Failed to submit host request"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getMyHostRequest(userId: String): Result<TournamentHostRequest?> = try {
        val response = api.getTournamentHostRequests(
            userId = PostgrestFilter.eq(userId),
            limit = "1"
        )
        if (response.isSuccessful) {
            val list = response.body() ?: emptyList()
            Result.success(list.firstOrNull()?.let { mapDtoToHostRequest(it) })
        } else {
            Result.failure(Exception("Failed to fetch host request"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    // ── Create tournament ────────────────────────────────────────

    override suspend fun createTournament(tournament: Tournament): Result<Tournament> {
        return try {
            val userId = SupabaseSession.getUserIdOrNull()
                ?: return Result.failure(Exception("Not authenticated"))
        val body = mutableMapOf<String, Any>(
            "host_user_id" to userId,
            "title" to tournament.title,
            "description" to tournament.description,
            "prize_type" to tournament.prizeType.value,
            "max_teams" to tournament.maxTeams,
            "min_team_size" to tournament.minTeamSize,
            "best_of" to tournament.bestOf,
            "region" to tournament.region,
            "skill_level" to tournament.skillLevel,
            "status" to "registration",
            "is_live_stream_enabled" to tournament.isLiveStreamEnabled
        )
        tournament.logoUrl?.let { body["logo_url"] = it }
        tournament.prizeDescription?.let { body["prize_description"] = it }
        tournament.swissRounds?.let { body["swiss_rounds"] = it }

        // registration_deadline and check_in_deadline are NOT NULL in DB — always provide values
        val regDeadline = if (tournament.registrationDeadline > 0) tournament.registrationDeadline
            else System.currentTimeMillis() + 24 * 60 * 60 * 1000L   // default: 24h from now
        val checkInDeadline = if (tournament.checkInDeadline > 0) tournament.checkInDeadline
            else regDeadline - 30 * 60 * 1000L                        // default: 30min before reg closes
        body["registration_deadline"] = regDeadline.toIsoString()
        body["check_in_deadline"] = checkInDeadline.toIsoString()

        val response = api.insertTournament(body)
        if (response.isSuccessful) {
            val dto = response.body()?.firstOrNull() ?: throw Exception("No response")
            invalidateTournamentCaches()
            Result.success(mapDtoToTournament(dto))
        } else {
            val errorBody = response.errorBody()?.string() ?: "Unknown error"
            Log.e(TAG, "Tournament creation failed: ${response.code()} — $errorBody")
            // Parse common PostgREST errors into user-friendly messages
            val userMessage = when {
                errorBody.contains("weekly_tournament_limit", ignoreCase = true) ||
                    errorBody.contains("1 tournament per 7 days", ignoreCase = true) ->
                    "You can only create 1 tournament per 7 days"
                errorBody.contains("violates row-level security", ignoreCase = true) ->
                    "You don't have permission to create tournaments. Make sure you're approved as a host."
                errorBody.contains("violates check constraint", ignoreCase = true) ->
                    "Invalid tournament data. Check all fields and try again."
                else -> "Failed to create tournament: $errorBody"
            }
            Result.failure(Exception(userMessage))
        }
    } catch (e: Exception) {
        Log.e(TAG, "Tournament creation exception", e)
        Result.failure(e)
    }
    }

    // ── Update tournament (host only, registration phase) ──────────

    override suspend fun updateTournament(tournamentId: String, updates: Map<String, Any?>): Result<Tournament> = try {
        // Ownership: only the tournament host may update it
        val tournamentResponse = api.getTournamentById(PostgrestFilter.eq(tournamentId))
        val tournament = tournamentResponse.body()?.firstOrNull()
        if (tournament == null) return Result.failure(Exception("Tournament not found"))
        val hostUserId = (tournament["host_user_id"] as? String) ?: ""
        AuthorizationUtils.requireOwner(hostUserId, "update this tournament")
            .onFailure { return Result.failure(it) }

        val body = updates.filterValues { it != null }.mapValues { it.value as Any }.toMutableMap()
        body["updated_at"] = java.time.Instant.now().toString()
        val response = api.updateTournament(
            id = PostgrestFilter.eq(tournamentId),
            body = body
        )
        if (response.isSuccessful) {
            val dto = response.body()?.firstOrNull() ?: throw Exception("No response")
            invalidateTournamentCaches()
            Result.success(mapDtoToTournament(dto))
        } else {
            val errorBody = response.errorBody()?.string() ?: "Unknown error"
            Log.e(TAG, "Tournament update failed: ${response.code()} — $errorBody")
            val userMessage = when {
                errorBody.contains("violates row-level security", ignoreCase = true) ->
                    "You don't have permission to update this tournament."
                errorBody.contains("violates check constraint", ignoreCase = true) ->
                    "Invalid tournament data. Check all fields and try again."
                else -> "Failed to update tournament: $errorBody"
            }
            Result.failure(Exception(userMessage))
        }
    } catch (e: Exception) {
        Log.e(TAG, "Tournament update exception", e)
        Result.failure(e)
    }

    // ── Swiss matches ───────────────────────────────────────────

    override suspend fun getTournamentMatches(tournamentId: String): Result<List<TournamentSwissMatch>> = try {
        withRetry {
            val response = api.getTournamentSwissMatches(
                tournamentId = PostgrestFilter.eq(tournamentId),
                select = "*,team_a:teams!tournament_swiss_matches_team_a_id_fkey(name),team_b:teams!tournament_swiss_matches_team_b_id_fkey(name),winner:teams!tournament_swiss_matches_winner_team_id_fkey(name)"
            )
            if (response.isSuccessful) {
                val matches = response.body()?.map { mapDtoToSwissMatch(it) } ?: emptyList()
                Result.success(matches)
            } else {
                Result.failure(Exception("Failed to fetch matches"))
            }
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    // ── Match roster ─────────────────────────────────────────────

    override suspend fun setMatchRoster(
        matchId: String,
        teamId: String,
        gameNumber: Int,
        playerIds: List<String>
    ): Result<Map<String, Any>> = try {
        val params = mapOf(
            "p_match_id" to matchId,
            "p_team_id" to teamId,
            "p_game_number" to gameNumber,
            "p_player_ids" to playerIds
        )
        val response = api.rpcSetTournamentMatchRoster(params)
        if (response.isSuccessful) {
            val result = response.body() ?: mapOf("success" to true)
            Result.success(result)
        } else {
            Result.failure(Exception("Failed to set roster"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getMatchRoster(matchId: String, teamId: String, gameNumber: Int): Result<List<TournamentMatchRoster>> = try {
        val response = api.getTournamentMatchRosters(
            matchId = PostgrestFilter.eq(matchId),
            teamId = PostgrestFilter.eq(teamId),
            gameNumber = PostgrestFilter.eq(gameNumber.toString()),
            select = "*,profiles:user_id(username)"
        )
        if (response.isSuccessful) {
            val rosters = response.body()?.map { mapDtoToMatchRoster(it) } ?: emptyList()
            Result.success(rosters)
        } else {
            Result.failure(Exception("Failed to fetch roster"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    // ── Room secrets ─────────────────────────────────────────────

    override suspend fun getMatchRoomSecret(matchId: String): Result<TournamentMatchRoomSecret?> = try {
        val response = api.getTournamentMatchRoomSecrets(
            matchId = PostgrestFilter.eq(matchId),
            limit = "1"
        )
        if (response.isSuccessful) {
            val list = response.body() ?: emptyList()
            Result.success(list.firstOrNull()?.let { mapDtoToRoomSecret(it) })
        } else {
            Result.failure(Exception("Failed to fetch room secret"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    // ── Host account ────────────────────────────────────────────

    override suspend fun getHostAccount(tournamentId: String): Result<TournamentHostAccount?> = try {
        val response = api.getTournamentHostAccounts(
            tournamentId = PostgrestFilter.eq(tournamentId),
            limit = "1"
        )
        if (response.isSuccessful) {
            val list = response.body() ?: emptyList()
            Result.success(list.firstOrNull()?.let { mapDtoToHostAccount(it) })
        } else {
            Result.failure(Exception("Failed to fetch host account"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun createHostAccount(tournamentId: String, hostUserId: String): Result<Map<String, Any>> = try {
        requireTournamentHost(tournamentId).onFailure { return Result.failure(it) }
        val body = mapOf("tournament_id" to tournamentId, "host_user_id" to hostUserId)
        val response = api.createHostAccount(body)
        if (response.isSuccessful) {
            val result = response.body()?.firstOrNull()?.mapValues { it.value as Any } ?: mapOf("success" to true)
            Result.success(result)
        } else {
            Result.failure(Exception("Failed to create host account"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    // ── Swiss pairing & tournament management ────────────────────

    override suspend fun generateSwissPairings(tournamentId: String): Result<Map<String, Any>> = try {
        requireTournamentHost(tournamentId).onFailure { return Result.failure(it) }
        val params = mapOf("p_tournament_id" to tournamentId)
        val response = api.rpcGenerateSwissPairings(params)
        if (response.isSuccessful) {
            invalidateTournamentCaches()
            Result.success(response.body() ?: mapOf("success" to true))
        } else {
            Result.failure(Exception("Failed to generate pairings"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun submitMatchResult(matchId: String, winnerTeamId: String?, isDraw: Boolean): Result<Map<String, Any>> = try {
        val params = mutableMapOf<String, Any>("p_match_id" to matchId, "p_is_draw" to isDraw)
        winnerTeamId?.let { params["p_winner_team_id"] = it }
        val response = api.rpcSubmitTournamentMatchResult(params)
        if (response.isSuccessful) {
            invalidateTournamentCaches()
            Result.success(response.body() ?: mapOf("success" to true))
        } else {
            Result.failure(Exception("Failed to submit match result"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun awardMatchPoints(matchId: String): Result<Map<String, Any>> = try {
        val params = mapOf("p_match_id" to matchId)
        val response = api.rpcAwardTournamentMatchPoints(params)
        if (response.isSuccessful) {
            Result.success(response.body() ?: mapOf("success" to true))
        } else {
            Result.failure(Exception("Failed to award points"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun updateTournamentScores(tournamentId: String): Result<Map<String, Any>> = try {
        requireTournamentHost(tournamentId).onFailure { return Result.failure(it) }
        val params = mapOf("p_tournament_id" to tournamentId)
        val response = api.rpcUpdateTournamentScores(params)
        if (response.isSuccessful) {
            invalidateTournamentCaches()
            Result.success(response.body() ?: mapOf("success" to true))
        } else {
            Result.failure(Exception("Failed to update scores"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun recalculateTiebreakers(tournamentId: String): Result<Map<String, Any>> = try {
        requireTournamentHost(tournamentId).onFailure { return Result.failure(it) }
        val params = mapOf("p_tournament_id" to tournamentId)
        val response = api.rpcRecalculateTiebreakers(params)
        if (response.isSuccessful) {
            Result.success(response.body() ?: mapOf("success" to true))
        } else {
            Result.failure(Exception("Failed to recalculate tiebreakers"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun disqualifyTeam(tournamentId: String, teamId: String, reason: String): Result<Map<String, Any>> = try {
        requireTournamentHost(tournamentId).onFailure { return Result.failure(it) }
        val params = mapOf("p_tournament_id" to tournamentId, "p_team_id" to teamId, "p_reason" to reason)
        val response = api.rpcDisqualifyTournamentTeam(params)
        if (response.isSuccessful) {
            invalidateTournamentCaches()
            Result.success(response.body() ?: mapOf("success" to true))
        } else {
            Result.failure(Exception("Failed to disqualify team"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun checkNoShows(tournamentId: String): Result<Map<String, Any>> = try {
        requireTournamentHost(tournamentId).onFailure { return Result.failure(it) }
        val params = mapOf("p_tournament_id" to tournamentId)
        val response = api.rpcCheckTournamentNoShows(params)
        if (response.isSuccessful) {
            Result.success(response.body() ?: mapOf("success" to true))
        } else {
            Result.failure(Exception("Failed to check no-shows"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun cancelTournament(tournamentId: String, reason: String?): Result<Map<String, Any>> = try {
        requireTournamentHost(tournamentId).onFailure { return Result.failure(it) }
        val params = mutableMapOf<String, Any>("p_tournament_id" to tournamentId)
        reason?.let { params["p_cancellation_reason"] = it }
        val response = api.rpcCancelTournament(params)
        if (response.isSuccessful) {
            invalidateTournamentCaches()
            Result.success(response.body() ?: mapOf("success" to true))
        } else {
            Result.failure(Exception("Failed to cancel tournament"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun completeTournament(tournamentId: String): Result<Map<String, Any>> = try {
        requireTournamentHost(tournamentId).onFailure { return Result.failure(it) }
        val params = mapOf("p_tournament_id" to tournamentId)
        val response = api.rpcCompleteTournament(params)
        if (response.isSuccessful) {
            invalidateTournamentCaches()
            Result.success(response.body() ?: mapOf("success" to true))
        } else {
            Result.failure(Exception("Failed to complete tournament"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun checkInTeam(tournamentId: String, teamId: String): Result<Map<String, Any>> = try {
        val params = mapOf("p_tournament_id" to tournamentId, "p_team_id" to teamId)
        val response = api.rpcCheckInTournamentTeam(params)
        if (response.isSuccessful) {
            invalidateTournamentCaches()
            Result.success(response.body() ?: mapOf("success" to true))
        } else {
            Result.failure(Exception("Failed to check in"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun reviewApplication(applicationId: String, approved: Boolean, rejectionReason: String?): Result<Map<String, Any>> = try {
        // Fetch application to get tournamentId for host check
        val appResponse = api.getTournamentApplications(
            id = PostgrestFilter.eq(applicationId),
            select = "tournament_id",
            limit = "1"
        )
        val app = appResponse.body()?.firstOrNull()
        val tournamentId = (app?.get("tournament_id") as? String) ?: ""
        if (tournamentId.isNotBlank()) {
            requireTournamentHost(tournamentId).onFailure { return Result.failure(it) }
        }
        val params = mutableMapOf<String, Any>(
            "p_application_id" to applicationId,
            "p_decision" to if (approved) "accepted" else "rejected"
        )
        rejectionReason?.let { params["p_rejection_reason"] = it }
        val response = api.rpcReviewTournamentApplication(params)
        if (response.isSuccessful) {
            invalidateTournamentCaches()
            Result.success(response.body() ?: mapOf("success" to true))
        } else {
            Result.failure(Exception("Failed to review application"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    // ── DTO mappers ─────────────────────────────────────────────

    @Suppress("UNCHECKED_CAST")
    private fun mapDtoToTournament(dto: Map<String, Any?>): Tournament {
        val profiles = dto["profiles"] as? Map<String, Any?>
        return Tournament(
            id = (dto["id"] as? String) ?: "",
            hostUserId = (dto["host_user_id"] as? String) ?: "",
            hostUsername = (profiles?.get("username") as? String) ?: "",
            title = (dto["title"] as? String) ?: "",
            description = (dto["description"] as? String) ?: "",
            logoUrl = dto["logo_url"] as? String,
            prizeType = PrizeType.fromValue((dto["prize_type"] as? String) ?: ""),
            prizeDescription = dto["prize_description"] as? String,
            maxTeams = (dto["max_teams"] as? Number)?.toInt() ?: 16,
            minTeamSize = (dto["min_team_size"] as? Number)?.toInt() ?: 5,
            bestOf = (dto["best_of"] as? Number)?.toInt() ?: 1,
            region = (dto["region"] as? String) ?: "EU",
            skillLevel = (dto["skill_level"] as? String) ?: "ALL",
            swissRounds = (dto["swiss_rounds"] as? Number)?.toInt(),
            currentRound = (dto["current_round"] as? Number)?.toInt() ?: 0,
            status = TournamentStatus.fromValue((dto["status"] as? String) ?: ""),
            registrationDeadline = parseTimestamp(dto["registration_deadline"]),
            checkInDeadline = parseTimestamp(dto["check_in_deadline"]),
            isLiveStreamEnabled = (dto["is_live_stream_enabled"] as? Boolean) ?: false,
            isFlagged = (dto["is_flagged"] as? Boolean) ?: false,
            createdAt = parseTimestamp(dto["created_at"]),
            updatedAt = parseTimestamp(dto["updated_at"]),
            teamCount = (dto["team_count"] as? Number)?.toInt() ?: 0,
        )
    }

    private fun mapDtoToRequirement(dto: Map<String, Any?>): TournamentRequirement {
        return TournamentRequirement(
            id = (dto["id"] as? String) ?: "",
            tournamentId = (dto["tournament_id"] as? String) ?: "",
            type = RequirementType.fromValue((dto["type"] as? String) ?: ""),
            label = (dto["label"] as? String) ?: "",
            url = dto["url"] as? String,
            sortOrder = (dto["sort_order"] as? Number)?.toInt() ?: 0,
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun mapDtoToTournamentTeam(dto: Map<String, Any?>): TournamentTeam {
        val teams = dto["teams"] as? Map<String, Any?>
        return TournamentTeam(
            id = (dto["id"] as? String) ?: "",
            tournamentId = (dto["tournament_id"] as? String) ?: "",
            teamId = (dto["team_id"] as? String) ?: "",
            teamName = (teams?.get("name") as? String) ?: "Unknown",
            checkedIn = (dto["checked_in"] as? Boolean) ?: false,
            swissWins = (dto["swiss_wins"] as? Number)?.toInt() ?: 0,
            swissLosses = (dto["swiss_losses"] as? Number)?.toInt() ?: 0,
            swissDraws = (dto["swiss_draws"] as? Number)?.toInt() ?: 0,
            swissPoints = (dto["swiss_points"] as? Number)?.toInt() ?: 0,
            buchholzScore = (dto["buchholz_score"] as? Number)?.toDouble() ?: 0.0,
            sonnebornBerger = (dto["sonneborn_berger"] as? Number)?.toDouble() ?: 0.0,
            finalPlacement = (dto["final_placement"] as? Number)?.toInt(),
            isDisqualified = (dto["is_disqualified"] as? Boolean) ?: false,
            disqualificationReason = dto["disqualification_reason"] as? String,
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun mapDtoToApplication(dto: Map<String, Any?>): TournamentApplication {
        val teams = dto["teams"] as? Map<String, Any?>
        return TournamentApplication(
            id = (dto["id"] as? String) ?: "",
            tournamentId = (dto["tournament_id"] as? String) ?: "",
            teamId = (dto["team_id"] as? String) ?: "",
            teamName = (teams?.get("name") as? String) ?: "Unknown",
            status = TournamentApplicationStatus.fromValue((dto["status"] as? String) ?: ""),
            rejectionReason = dto["rejection_reason"] as? String,
            attemptNumber = (dto["attempt_number"] as? Number)?.toInt() ?: 1,
            appliedAt = parseTimestamp(dto["applied_at"]),
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun mapDtoToSwissMatch(dto: Map<String, Any?>): TournamentSwissMatch {
        val teamA = dto["team_a"] as? Map<String, Any?>
        val teamB = dto["team_b"] as? Map<String, Any?>
        val winner = dto["winner"] as? Map<String, Any?>
        return TournamentSwissMatch(
            id = (dto["id"] as? String) ?: "",
            tournamentId = (dto["tournament_id"] as? String) ?: "",
            roundNumber = (dto["round_number"] as? Number)?.toInt() ?: 0,
            matchNumber = (dto["match_number"] as? Number)?.toInt() ?: 0,
            teamAId = (dto["team_a_id"] as? String) ?: "",
            teamAName = (teamA?.get("name") as? String) ?: "Unknown",
            teamBId = dto["team_b_id"] as? String,
            teamBName = teamB?.get("name") as? String ?: if (dto["team_b_id"] == null) "BYE" else "Unknown",
            conversationId = dto["conversation_id"] as? String,
            status = MatchStatus.fromValue((dto["status"] as? String) ?: ""),
            scheduledAt = parseTimestamp(dto["scheduled_at"]),
            winnerTeamId = dto["winner_team_id"] as? String,
            winnerTeamName = winner?.get("name") as? String,
            isDraw = (dto["is_draw"] as? Boolean) ?: false,
            liveStreamUrl = dto["live_stream_url"] as? String,
            isBye = dto["team_b_id"] == null,
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun mapDtoToMatchRoster(dto: Map<String, Any?>): TournamentMatchRoster {
        val profiles = dto["profiles"] as? Map<String, Any?>
        return TournamentMatchRoster(
            id = (dto["id"] as? String) ?: "",
            matchId = (dto["match_id"] as? String) ?: "",
            teamId = (dto["team_id"] as? String) ?: "",
            userId = (dto["user_id"] as? String) ?: "",
            username = (profiles?.get("username") as? String) ?: "Unknown",
            gameNumber = (dto["game_number"] as? Number)?.toInt() ?: 1,
            isActive = (dto["is_active"] as? Boolean) ?: true,
        )
    }

    private fun mapDtoToRoomSecret(dto: Map<String, Any?>): TournamentMatchRoomSecret {
        return TournamentMatchRoomSecret(
            id = (dto["id"] as? String) ?: "",
            matchId = (dto["match_id"] as? String) ?: "",
            roomId = (dto["room_id"] as? String) ?: "",
            roomPassword = dto["room_password"] as? String,
            droppedBy = (dto["dropped_by"] as? String) ?: "",
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun mapDtoToHostRequest(dto: Map<String, Any?>): TournamentHostRequest {
        val links = dto["social_links"]
        val socialLinksList = when (links) {
            is List<*> -> links.filterIsInstance<String>()
            else -> emptyList()
        }
        return TournamentHostRequest(
            id = (dto["id"] as? String) ?: "",
            userId = (dto["user_id"] as? String) ?: "",
            motivation = (dto["motivation"] as? String) ?: "",
            experience = dto["experience"] as? String,
            telegramChannel = dto["telegram_channel"] as? String,
            socialLinks = socialLinksList,
            status = (dto["status"] as? String) ?: "pending",
            adminNotes = dto["admin_notes"] as? String,
        )
    }

    private fun mapDtoToHostAccount(dto: Map<String, Any?>): TournamentHostAccount {
        return TournamentHostAccount(
            id = (dto["id"] as? String) ?: "",
            tournamentId = (dto["tournament_id"] as? String) ?: "",
            hostUserId = (dto["host_user_id"] as? String) ?: "",
            authUserId = dto["auth_user_id"] as? String,
            email = (dto["email"] as? String) ?: "",
        )
    }

    // ── Helpers ──────────────────────────────────────────────────

    private fun parseTimestamp(value: Any?): Long {
        return when (value) {
            is String -> {
                try { java.time.Instant.parse(value).toEpochMilli() }
                catch (_: Exception) { 0L }
            }
            is Number -> value.toLong()
            else -> 0L
        }
    }

    private fun Long.toIsoString(): String {
        return if (this > 0) java.time.Instant.ofEpochMilli(this).toString()
        else java.time.Instant.now().toString()
    }
}

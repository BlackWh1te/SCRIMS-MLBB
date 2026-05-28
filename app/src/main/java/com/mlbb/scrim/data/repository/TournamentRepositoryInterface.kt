package com.mlbb.scrim.data.repository

import com.mlbb.scrim.data.model.*

interface TournamentRepositoryInterface {

    // ── Tournament list ──────────────────────────────────────────
    suspend fun getTournaments(
        status: String? = null,
        region: String? = null,
        skillLevel: String? = null
    ): Result<List<Tournament>>

    // ── Tournament detail ────────────────────────────────────────
    suspend fun getTournamentById(tournamentId: String): Result<Tournament>

    // ── Tournament requirements ──────────────────────────────────
    suspend fun getTournamentRequirements(tournamentId: String): Result<List<TournamentRequirement>>

    suspend fun createRequirements(tournamentId: String, requirements: List<TournamentRequirement>): Result<Unit>

    suspend fun deleteRequirement(requirementId: String): Result<Unit>

    // ── Tournament teams ─────────────────────────────────────────
    suspend fun getTournamentTeams(tournamentId: String): Result<List<TournamentTeam>>

    // ── Tournament applications ──────────────────────────────────
    suspend fun getMyApplications(userId: String): Result<List<TournamentApplication>>

    suspend fun applyForTournament(tournamentId: String, teamId: String): Result<Map<String, Any>>

    // ── Host request ────────────────────────────────────────────
    suspend fun submitHostRequest(
        motivation: String,
        experience: String?,
        telegramChannel: String?,
        socialLinks: List<String>
    ): Result<TournamentHostRequest>

    suspend fun getMyHostRequest(userId: String): Result<TournamentHostRequest?>

    // ── Create tournament ────────────────────────────────────────
    suspend fun createTournament(tournament: Tournament): Result<Tournament>

    // ── Update tournament (host only, registration phase) ────────
    suspend fun updateTournament(tournamentId: String, updates: Map<String, Any?>): Result<Tournament>

    // ── Swiss matches ───────────────────────────────────────────
    suspend fun getTournamentMatches(tournamentId: String): Result<List<TournamentSwissMatch>>

    // ── Match roster ────────────────────────────────────────────
    suspend fun setMatchRoster(
        matchId: String,
        teamId: String,
        gameNumber: Int,
        playerIds: List<String>
    ): Result<Map<String, Any>>

    suspend fun getMatchRoster(matchId: String, teamId: String, gameNumber: Int): Result<List<TournamentMatchRoster>>

    // ── Room secrets ────────────────────────────────────────────
    suspend fun getMatchRoomSecret(matchId: String): Result<TournamentMatchRoomSecret?>

    // ── Host account ────────────────────────────────────────────
    suspend fun getHostAccount(tournamentId: String): Result<TournamentHostAccount?>

    suspend fun createHostAccount(tournamentId: String, hostUserId: String): Result<Map<String, Any>>

    suspend fun getTournamentPlayerStats(tournamentId: String): Result<List<TournamentPlayerStats>>

    // ── Swiss pairing & tournament management ────────────────────
    suspend fun generateSwissPairings(tournamentId: String): Result<Map<String, Any>>

    suspend fun submitMatchResult(matchId: String, winnerTeamId: String?, isDraw: Boolean, gameAScore: Int = 0, gameBScore: Int = 0): Result<Map<String, Any>>

    suspend fun awardMatchPoints(matchId: String): Result<Map<String, Any>>

    suspend fun updateTournamentScores(tournamentId: String): Result<Map<String, Any>>

    suspend fun recalculateTiebreakers(tournamentId: String): Result<Map<String, Any>>

    suspend fun disqualifyTeam(tournamentId: String, teamId: String, reason: String): Result<Map<String, Any>>

    suspend fun checkNoShows(tournamentId: String): Result<Map<String, Any>>

    suspend fun cancelTournament(tournamentId: String, reason: String? = null): Result<Map<String, Any>>

    suspend fun completeTournament(tournamentId: String): Result<Map<String, Any>>

    suspend fun checkInTeam(tournamentId: String, teamId: String): Result<Map<String, Any>>

    suspend fun reviewApplication(applicationId: String, approved: Boolean, rejectionReason: String?): Result<Map<String, Any>>

    suspend fun resolveDispute(matchId: String, winnerTeamId: String?, isDraw: Boolean, resolution: String): Result<Map<String, Any>>

    suspend fun uploadTournamentLogo(tournamentId: String, fileBytes: ByteArray, contentType: String): Result<String>
}

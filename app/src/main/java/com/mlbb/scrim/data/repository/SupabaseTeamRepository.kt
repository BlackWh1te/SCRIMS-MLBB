package com.mlbb.scrim.data.repository

import com.mlbb.scrim.data.cache.ProfileCacheRepository
import com.mlbb.scrim.data.cache.UnifiedCacheManager
import com.mlbb.scrim.data.local.TeamDao
import com.mlbb.scrim.data.local.TeamEntity
import com.mlbb.scrim.data.model.*
import com.mlbb.scrim.data.service.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Supabase-backed team repository with caching.
 * Memory TTL: 5 min | Room TTL: 1 hour
 * Uses ProfileCacheRepository for batch member profile lookups.
 */
class SupabaseTeamRepository(
    private val cacheManager: UnifiedCacheManager,
    private val teamDao: TeamDao,
    private val profileCache: ProfileCacheRepository
) : TeamRepositoryInterface {

    private val api = SupabaseService.api

    companion object {
        private const val CACHE_KEY_ALL = "teams_all"
        private const val CACHE_KEY_PREFIX = "teams_"
        private const val MEM_TTL = 5L * 60 * 1000
        private const val ROOM_TTL = 60L * 60 * 1000
    }

    private suspend fun invalidateTeamCaches() { cacheManager.invalidateByPrefix("teams_") }

    override suspend fun createTeam(name: String, leaderId: String, description: String): Flow<Result<Team>> = flow {
        try {
            val request = CreateTeamRequest(name = name, leaderId = leaderId, description = description, minPlayers = 5, maxPlayers = 7)
            val response = api.createTeam(request)
            if (response.isSuccessful) {
                val created = response.body()?.firstOrNull()
                if (created != null) {
                    api.addTeamMember(AddTeamMemberRequest(teamId = created.id, userId = leaderId, role = "Leader"))
                    invalidateTeamCaches()
                    emit(Result.success(mapTeamDtoToModel(created)))
                } else emit(Result.failure(Exception("Team creation failed")))
            } else emit(Result.failure(Exception("Error: ${response.errorBody()?.string()}")))
        } catch (e: Exception) { emit(Result.failure(e)) }
    }

    override suspend fun getTeams(): Flow<Result<List<Team>>> = flow {
        try {
            cacheManager.getFlow<List<Team>>(
                key = CACHE_KEY_ALL, memoryTtlMs = MEM_TTL, roomTtlMs = ROOM_TTL,
                roomLoader = {
                    val c = teamDao.getAll()
                    if (c.isNotEmpty()) c.map { mapEntityToTeam(it) } else null
                },
                networkLoader = {
                    val r = api.getTeams()
                    if (r.isSuccessful) r.body()?.map { mapTeamDtoToModel(it) } ?: emptyList()
                    else throw Exception("Failed to fetch teams")
                },
                roomSaver = { list ->
                    teamDao.deleteAll()
                    teamDao.insertAll(list.map { mapTeamToEntity(it) })
                }
            ).collect { teams ->
                emit(Result.success(teams))
            }
        } catch (e: Exception) {
            val c = teamDao.getAll()
            if (c.isNotEmpty()) emit(Result.success(c.map { mapEntityToTeam(it) }))
            else emit(Result.failure(e))
        }
    }

    override suspend fun getTeam(teamId: String): Flow<Result<Team>> = flow {
        try {
            val key = "$CACHE_KEY_PREFIX$teamId"
            cacheManager.getFlow<Team>(
                key = key, memoryTtlMs = MEM_TTL, roomTtlMs = ROOM_TTL,
                roomLoader = {
                    val c = teamDao.getById(teamId)
                    if (c != null) mapEntityToTeam(c) else null
                },
                networkLoader = {
                    val r = api.getTeamById(PostgrestFilter.eq(teamId))
                    if (r.isSuccessful) {
                        val t = r.body()?.firstOrNull() ?: throw Exception("Team not found")
                        mapTeamDtoToModel(t)
                    } else throw Exception("Failed to fetch team")
                },
                roomSaver = { team -> teamDao.insert(mapTeamToEntity(team)) }
            ).collect { team ->
                emit(Result.success(team))
            }
        } catch (e: Exception) {
            val c = teamDao.getById(teamId)
            if (c != null) emit(Result.success(mapEntityToTeam(c)))
            else emit(Result.failure(e))
        }
    }

    override suspend fun addPlayer(teamId: String, playerName: String, playerEmail: String): Flow<Result<Team>> = flow {
        emit(Result.failure(Exception("Invite system: Use invitePlayer instead")))
    }

    override suspend fun updatePlayerRole(teamId: String, playerId: String, newRole: PlayerRole): Flow<Result<Team>> = flow {
        try {
            val roleStr = if (newRole == PlayerRole.LEADER) "Leader" else "Member"
            val r = api.updateTeamMemberRole(PostgrestFilter.eq(teamId), PostgrestFilter.eq(playerId), mapOf("role" to roleStr))
            if (r.isSuccessful) { invalidateTeamCaches(); getTeam(teamId).collect { emit(it) } }
            else emit(Result.failure(Exception("Failed to update player role")))
        } catch (e: Exception) { emit(Result.failure(e)) }
    }

    override suspend fun deleteTeam(teamId: String): Flow<Result<Unit>> = flow {
        try {
            val r = api.deleteTeam(PostgrestFilter.eq(teamId))
            if (r.isSuccessful) { invalidateTeamCaches(); teamDao.deleteById(teamId); emit(Result.success(Unit)) }
            else emit(Result.failure(Exception("Failed to delete team")))
        } catch (e: Exception) { emit(Result.failure(e)) }
    }

    override suspend fun sendInvite(teamId: String, teamName: String, invitedBy: String, invitedByName: String, invitedUserId: String, invitedUserName: String): Flow<Result<TeamInvite>> = flow {
        try {
            val r = api.addTeamMember(AddTeamMemberRequest(teamId = teamId, userId = invitedUserId, role = "Invited"))
            if (r.isSuccessful) {
                invalidateTeamCaches()
                emit(Result.success(TeamInvite(id = java.util.UUID.randomUUID().toString(), teamId = teamId, teamName = teamName, invitedBy = invitedBy, invitedByName = invitedByName, invitedUserId = invitedUserId, invitedUserName = invitedUserName, status = InviteStatus.PENDING)))
            } else emit(Result.failure(Exception("Failed to send invite")))
        } catch (e: Exception) { emit(Result.failure(e)) }
    }

    override suspend fun acceptInvite(inviteId: String): Flow<Result<Team>> = flow {
        try {
            val mr = api.getTeamMembers(id = PostgrestFilter.eq(inviteId))
            if (!mr.isSuccessful || mr.body().isNullOrEmpty()) { emit(Result.failure(Exception("Invite not found"))); return@flow }
            val member = mr.body()!!.first()
            val ur = api.updateTeamMemberRole(PostgrestFilter.eq(member.teamId), PostgrestFilter.eq(member.userId), mapOf("role" to "Member"))
            if (!ur.isSuccessful) { emit(Result.failure(Exception("Failed to accept invite"))); return@flow }
            invalidateTeamCaches()
            getTeam(member.teamId).collect { emit(it) }
        } catch (e: Exception) { emit(Result.failure(e)) }
    }

    override suspend fun declineInvite(inviteId: String): Flow<Result<Unit>> = flow {
        try {
            val mr = api.getTeamMembers(id = PostgrestFilter.eq(inviteId))
            if (!mr.isSuccessful || mr.body().isNullOrEmpty()) { emit(Result.success(Unit)); return@flow }
            val member = mr.body()!!.first()
            val dr = api.removeTeamMember(PostgrestFilter.eq(member.teamId), PostgrestFilter.eq(member.userId))
            if (dr.isSuccessful) { invalidateTeamCaches(); emit(Result.success(Unit)) }
            else emit(Result.failure(Exception("Failed to decline invite")))
        } catch (e: Exception) { emit(Result.failure(e)) }
    }

    override suspend fun getInvitesForPlayer(userId: String): Flow<Result<List<TeamInvite>>> = flow {
        try {
            val r = api.getTeamMembers(userId = PostgrestFilter.eq(userId))
            if (r.isSuccessful) {
                val invites = (r.body() ?: emptyList()).filter { it.role == "Invited" }.map { m -> TeamInvite(id = m.id, teamId = m.teamId, teamName = "Team ${m.teamId}", invitedBy = "", invitedByName = "", invitedUserId = userId, invitedUserName = "", status = InviteStatus.PENDING) }
                emit(Result.success(invites))
            } else emit(Result.failure(Exception("Failed to fetch invites")))
        } catch (e: Exception) { emit(Result.failure(e)) }
    }

    override suspend fun getInvitesForTeam(teamId: String): Flow<Result<List<TeamInvite>>> = flow {
        try {
            val r = api.getTeamMembers(teamId = PostgrestFilter.eq(teamId))
            if (r.isSuccessful) {
                val invites = (r.body() ?: emptyList()).filter { it.role == "Invited" }.map { m -> TeamInvite(id = m.id, teamId = teamId, teamName = "", invitedBy = "", invitedByName = "", invitedUserId = m.userId, invitedUserName = "", status = InviteStatus.PENDING) }
                emit(Result.success(invites))
            } else emit(Result.failure(Exception("Failed to fetch team invites")))
        } catch (e: Exception) { emit(Result.failure(e)) }
    }

    override suspend fun removePlayer(teamId: String, playerId: String): Flow<Result<Team>> = flow {
        try {
            api.removeTeamMember(PostgrestFilter.eq(teamId), PostgrestFilter.eq(playerId))
            invalidateTeamCaches()
            getTeam(teamId).collect { emit(it) }
        } catch (e: Exception) { emit(Result.failure(e)) }
    }

    // ─── Mapping ───

    private suspend fun mapTeamDtoToModel(dto: TeamDto): Team {
        val membersResponse = api.getTeamMembers(teamId = PostgrestFilter.eq(dto.id))
        val members = membersResponse.body() ?: emptyList()
        // N+1 FIX: batch profile fetch via ProfileCacheRepository
        val userIds = members.map { it.userId }
        val profilesByUserId = profileCache.getProfiles(userIds)
        return Team(id = dto.id, name = dto.name, leaderId = dto.leaderId, players = members.map { m ->
            val profile = profilesByUserId[m.userId]
            Player(id = m.userId, name = profile?.username ?: m.userId.take(8), role = if (m.role == "Leader") PlayerRole.LEADER else PlayerRole.MEMBER, email = profile?.email ?: "")
        }, maxPlayers = dto.maxPlayers, minPlayers = dto.minPlayers, totalScrims = dto.completedScrims, completedScrims = dto.completedScrims, reputation = dto.reputation, noShows = dto.noShows, canPostScrimsUntil = parseCanPostScrimsUntil(dto.canPostScrimsUntil), createdAt = parseCreatedAt(dto.createdAt))
    }

    private fun mapTeamToEntity(team: Team): TeamEntity {
        return TeamEntity(id = team.id, name = team.name, leaderId = team.leaderId, description = null, minPlayers = team.minPlayers, maxPlayers = team.maxPlayers, completedScrims = team.completedScrims, reputation = team.reputation, noShows = team.noShows, memberIdsJson = team.players.joinToString(",") { it.id })
    }

    private fun mapEntityToTeam(e: TeamEntity): Team {
        val memberIds = e.memberIdsJson?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
        return Team(id = e.id, name = e.name, leaderId = e.leaderId, players = memberIds.map { Player(id = it, name = it.take(8), role = PlayerRole.MEMBER, email = "") }, maxPlayers = e.maxPlayers, minPlayers = e.minPlayers, completedScrims = e.completedScrims, reputation = e.reputation, noShows = e.noShows)
    }

    private fun parseCanPostScrimsUntil(raw: String?): Long {
        if (raw.isNullOrBlank()) return 0L
        return try { java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", java.util.Locale.US).parse(raw)?.time ?: 0L } catch (_: Exception) { try { java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", java.util.Locale.US).parse(raw)?.time ?: 0L } catch (_: Exception) { 0L } }
    }

    private fun parseCreatedAt(raw: String?): Long {
        if (raw.isNullOrBlank()) return System.currentTimeMillis()
        return try { java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", java.util.Locale.US).parse(raw)?.time ?: System.currentTimeMillis() } catch (_: Exception) { try { java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", java.util.Locale.US).parse(raw)?.time ?: System.currentTimeMillis() } catch (_: Exception) { System.currentTimeMillis() } }
    }
}

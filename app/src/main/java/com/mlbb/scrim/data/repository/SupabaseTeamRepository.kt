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

    override suspend fun createTeam(name: String, leaderId: String, description: String, isOpenForApplications: Boolean): Flow<Result<Team>> = flow {
        try {
            val request = CreateTeamRequest(name = name, leaderId = leaderId, description = description, minPlayers = 5, maxPlayers = 7, isOpenForApplications = isOpenForApplications)
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

    override suspend fun getTeamsForUser(userId: String): Flow<Result<List<Team>>> = flow {
        try {
            // Step 1: Find all team_members rows for this user
            val membersResponse = api.getTeamMembers(userId = PostgrestFilter.eq(userId))
            if (!membersResponse.isSuccessful) {
                emit(Result.failure(Exception("Failed to fetch user team memberships")))
                return@flow
            }
            val memberships = membersResponse.body() ?: emptyList()
            // Filter out "Invited" status — only show teams where user is Leader or Member
            val activeTeamIds = memberships
                .filter { it.role != "Invited" }
                .map { it.teamId }
                .distinct()

            if (activeTeamIds.isEmpty()) {
                emit(Result.success(emptyList()))
                return@flow
            }

            // Step 2: Fetch those teams by ID
            val teamsResponse = api.getTeamsByIds(PostgrestFilter.inList(activeTeamIds))
            if (!teamsResponse.isSuccessful) {
                emit(Result.failure(Exception("Failed to fetch user teams")))
                return@flow
            }
            val teamDtos = teamsResponse.body() ?: emptyList()
            val teams = teamDtos.map { mapTeamDtoToModel(it) }
            emit(Result.success(teams))
        } catch (e: Exception) {
            emit(Result.failure(e))
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
        try {
            // Look up user by email
            val profileResponse = api.getProfileByEmail(email = playerEmail)
            if (!profileResponse.isSuccessful) {
                emit(Result.failure(Exception("Failed to look up user by email")))
                return@flow
            }
            val profiles = profileResponse.body() ?: emptyList()
            if (profiles.isEmpty()) {
                emit(Result.failure(Exception("No user found with email: $playerEmail")))
                return@flow
            }
            val userId = profiles.first().id

            // Check if already a member
            val existing = api.getTeamMembers(teamId = PostgrestFilter.eq(teamId), userId = PostgrestFilter.eq(userId))
            if (existing.isSuccessful && !existing.body().isNullOrEmpty()) {
                emit(Result.failure(Exception("This player is already in the team")))
                return@flow
            }

            // Add as Member
            val addResponse = api.addTeamMember(AddTeamMemberRequest(teamId = teamId, userId = userId, role = "Member"))
            if (addResponse.isSuccessful) {
                invalidateTeamCaches()
                getTeam(teamId).collect { emit(it) }
            } else {
                emit(Result.failure(Exception("Failed to add player: ${addResponse.errorBody()?.string()}")))
            }
        } catch (e: Exception) { emit(Result.failure(e)) }
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
            if (!r.isSuccessful) { emit(Result.failure(Exception("Failed to fetch invites"))); return@flow }
            val memberships = (r.body() ?: emptyList()).filter { it.role == "Invited" }
            if (memberships.isEmpty()) { emit(Result.success(emptyList())); return@flow }

            // Batch fetch team names
            val teamIds = memberships.map { it.teamId }.distinct()
            val teamsResponse = api.getTeamsByIds(PostgrestFilter.inList(teamIds))
            val teamNameMap = if (teamsResponse.isSuccessful) {
                (teamsResponse.body() ?: emptyList()).associate { it.id to it.name }
            } else emptyMap()

            // Batch fetch inviter profiles (leader is the likely inviter)
            val leaderIds = memberships.mapNotNull { teamNameMap[it.teamId]?.let { _ -> null } }
                .let { _ -> teamsResponse.body()?.map { it.leaderId } ?: emptyList() }
            val leaderProfiles = if (leaderIds.isNotEmpty()) profileCache.getProfiles(leaderIds) else emptyMap()

            val invites = memberships.map { m ->
                val teamName = teamNameMap[m.teamId] ?: "Team ${m.teamId.take(6)}"
                val leaderId = teamsResponse.body()?.find { it.id == m.teamId }?.leaderId ?: ""
                val leaderProfile = leaderProfiles[leaderId]
                TeamInvite(
                    id = m.id,
                    teamId = m.teamId,
                    teamName = teamName,
                    invitedBy = leaderId,
                    invitedByName = leaderProfile?.username ?: "Team Leader",
                    invitedUserId = userId,
                    invitedUserName = "",
                    status = InviteStatus.PENDING
                )
            }
            emit(Result.success(invites))
        } catch (e: Exception) { emit(Result.failure(e)) }
    }

    override suspend fun getInvitesForTeam(teamId: String): Flow<Result<List<TeamInvite>>> = flow {
        try {
            val r = api.getTeamMembers(teamId = PostgrestFilter.eq(teamId))
            if (!r.isSuccessful) { emit(Result.failure(Exception("Failed to fetch team invites"))); return@flow }
            val memberships = (r.body() ?: emptyList()).filter { it.role == "Invited" }
            if (memberships.isEmpty()) { emit(Result.success(emptyList())); return@flow }

            // Fetch team name
            val teamResponse = api.getTeamById(PostgrestFilter.eq(teamId))
            val teamName = if (teamResponse.isSuccessful) teamResponse.body()?.firstOrNull()?.name ?: "" else ""

            // Batch fetch invited user profiles
            val userIds = memberships.map { it.userId }.distinct()
            val profiles = if (userIds.isNotEmpty()) profileCache.getProfiles(userIds) else emptyMap()

            val invites = memberships.map { m ->
                val profile = profiles[m.userId]
                TeamInvite(
                    id = m.id,
                    teamId = teamId,
                    teamName = teamName,
                    invitedBy = "",
                    invitedByName = "",
                    invitedUserId = m.userId,
                    invitedUserName = profile?.username ?: m.userId.take(8),
                    status = InviteStatus.PENDING
                )
            }
            emit(Result.success(invites))
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
        }, maxPlayers = dto.maxPlayers, minPlayers = dto.minPlayers, totalScrims = dto.completedScrims, completedScrims = dto.completedScrims, reputation = dto.reputation, noShows = dto.noShows, canPostScrimsUntil = parseCanPostScrimsUntil(dto.canPostScrimsUntil), logoUrl = dto.logoUrl, isOpenForApplications = dto.isOpenForApplications, createdAt = parseCreatedAt(dto.createdAt))
    }

    private fun mapTeamToEntity(team: Team): TeamEntity {
        return TeamEntity(id = team.id, name = team.name, leaderId = team.leaderId, description = null, minPlayers = team.minPlayers, maxPlayers = team.maxPlayers, completedScrims = team.completedScrims, reputation = team.reputation, noShows = team.noShows, memberIdsJson = team.players.joinToString(",") { it.id }, logoUrl = team.logoUrl, isOpenForApplications = team.isOpenForApplications)
    }

    private fun mapEntityToTeam(e: TeamEntity): Team {
        val memberIds = e.memberIdsJson?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
        return Team(id = e.id, name = e.name, leaderId = e.leaderId, players = memberIds.map { Player(id = it, name = it.take(8), role = PlayerRole.MEMBER, email = "") }, maxPlayers = e.maxPlayers, minPlayers = e.minPlayers, completedScrims = e.completedScrims, reputation = e.reputation, noShows = e.noShows, isOpenForApplications = e.isOpenForApplications)
    }

    private fun parseCanPostScrimsUntil(raw: String?): Long {
        if (raw.isNullOrBlank()) return 0L
        return try { java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", java.util.Locale.US).parse(raw)?.time ?: 0L } catch (_: Exception) { try { java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", java.util.Locale.US).parse(raw)?.time ?: 0L } catch (_: Exception) { 0L } }
    }

    private fun parseCreatedAt(raw: String?): Long {
        if (raw.isNullOrBlank()) return System.currentTimeMillis()
        return try { java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", java.util.Locale.US).parse(raw)?.time ?: System.currentTimeMillis() } catch (_: Exception) { try { java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", java.util.Locale.US).parse(raw)?.time ?: System.currentTimeMillis() } catch (_: Exception) { System.currentTimeMillis() } }
    }

    suspend fun uploadTeamLogo(teamId: String, fileBytes: ByteArray, contentType: String = "image/jpeg"): Result<String> = try {
        val path = "logos/${teamId}_${System.currentTimeMillis()}.${if (contentType.contains("jpeg")) "jpg" else "png"}"
        val uploadResult = SupabaseStorageUpload.uploadFile(
            bucket = SupabaseConfig.BUCKET_TEAM_LOGOS,
            path = path,
            fileBytes = fileBytes,
            contentType = contentType
        )
        uploadResult.onSuccess { publicUrl ->
            api.updateTeam(teamId, mapOf("logo_url" to publicUrl))
            invalidateTeamCaches()
        }
        uploadResult
    } catch (e: Exception) {
        Result.failure(e)
    }

    // ─── Team Application Methods ───

    override suspend fun getOpenTeams(): Flow<Result<List<Team>>> = flow {
        try {
            val r = api.getTeams()
            if (r.isSuccessful) {
                val teams = (r.body() ?: emptyList())
                    .filter { it.isOpenForApplications }
                    .map { mapTeamDtoToModel(it) }
                emit(Result.success(teams))
            } else emit(Result.failure(Exception("Failed to fetch open teams")))
        } catch (e: Exception) { emit(Result.failure(e)) }
    }

    override suspend fun applyToTeam(teamId: String, applicantUserId: String, message: String?): Flow<Result<TeamApplication>> = flow {
        try {
            val request = TeamApplicationDto(teamId = teamId, applicantUserId = applicantUserId, message = message, status = "Pending")
            val r = api.createTeamApplication(request)
            if (r.isSuccessful) {
                val created = r.body()?.firstOrNull()
                if (created != null) {
                    emit(Result.success(mapTeamApplicationDtoToModel(created)))
                } else emit(Result.failure(Exception("Application failed")))
            } else emit(Result.failure(Exception("Failed to apply: ${r.errorBody()?.string()}")))
        } catch (e: Exception) { emit(Result.failure(e)) }
    }

    override suspend fun getTeamApplications(teamId: String): Flow<Result<List<TeamApplication>>> = flow {
        try {
            val r = api.getTeamApplications(teamId = teamId, status = "Pending")
            if (r.isSuccessful) {
                val apps = (r.body() ?: emptyList()).map { mapTeamApplicationDtoToModel(it) }
                emit(Result.success(apps))
            } else emit(Result.failure(Exception("Failed to fetch applications")))
        } catch (e: Exception) { emit(Result.failure(e)) }
    }

    override suspend fun getMyApplications(userId: String): Flow<Result<List<TeamApplication>>> = flow {
        try {
            val r = api.getTeamApplications(applicantUserId = userId)
            if (r.isSuccessful) {
                val apps = (r.body() ?: emptyList()).map { mapTeamApplicationDtoToModel(it) }
                emit(Result.success(apps))
            } else emit(Result.failure(Exception("Failed to fetch applications")))
        } catch (e: Exception) { emit(Result.failure(e)) }
    }

    override suspend fun acceptApplication(applicationId: String): Flow<Result<Team>> = flow {
        try {
            val r = api.updateTeamApplication(applicationId, mapOf("status" to "Accepted", "responded_at" to java.time.Instant.now().toString()))
            if (r.isSuccessful) {
                val app = r.body()?.firstOrNull()
                if (app != null) {
                    // Add applicant as team member
                    api.addTeamMember(AddTeamMemberRequest(teamId = app.teamId, userId = app.applicantUserId, role = "Member"))
                    invalidateTeamCaches()
                    getTeam(app.teamId).collect { emit(it) }
                } else emit(Result.failure(Exception("Application not found")))
            } else emit(Result.failure(Exception("Failed to accept application")))
        } catch (e: Exception) { emit(Result.failure(e)) }
    }

    override suspend fun declineApplication(applicationId: String): Flow<Result<Unit>> = flow {
        try {
            val r = api.updateTeamApplication(applicationId, mapOf("status" to "Declined", "responded_at" to java.time.Instant.now().toString()))
            if (r.isSuccessful) emit(Result.success(Unit))
            else emit(Result.failure(Exception("Failed to decline application")))
        } catch (e: Exception) { emit(Result.failure(e)) }
    }

    private fun mapTeamApplicationDtoToModel(dto: TeamApplicationDto): TeamApplication {
        return TeamApplication(
            id = dto.id,
            teamId = dto.teamId,
            applicantUserId = dto.applicantUserId,
            status = when (dto.status) {
                "Accepted" -> com.mlbb.scrim.data.model.TeamApplicationStatus.ACCEPTED
                "Declined" -> com.mlbb.scrim.data.model.TeamApplicationStatus.DECLINED
                else -> com.mlbb.scrim.data.model.TeamApplicationStatus.PENDING
            },
            message = dto.message,
            createdAt = parseCreatedAt(dto.createdAt),
            respondedAt = dto.respondedAt?.let { parseCreatedAt(it) }
        )
    }
}

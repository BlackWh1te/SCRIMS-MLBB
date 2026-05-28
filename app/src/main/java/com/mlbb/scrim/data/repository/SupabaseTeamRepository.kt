package com.mlbb.scrim.data.repository

import com.mlbb.scrim.data.cache.ProfileCacheRepository
import com.mlbb.scrim.data.cache.UnifiedCacheManager
import com.mlbb.scrim.data.local.TeamDao
import com.mlbb.scrim.data.local.TeamEntity
import com.mlbb.scrim.data.model.*
import com.mlbb.scrim.data.service.*
import com.mlbb.scrim.security.AuthorizationUtils
import com.mlbb.scrim.util.DateUtils
import timber.log.Timber
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow

/**
 * Supabase-backed team repository with caching.
 * Memory TTL: 5 min | Room TTL: 1 hour
 * Uses ProfileCacheRepository for batch member profile lookups.
 */
class SupabaseTeamRepository(
    private val cacheManager: UnifiedCacheManager,
    private val teamDao: TeamDao,
    private val profileCache: ProfileCacheRepository,
    private val realtimeClient: SupabaseRealtimeClient
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
            // 1-team limit: check if user already belongs to a team
            val existingTeamsResponse = api.getTeamMembers(userId = PostgrestFilter.eq(leaderId))
            if (existingTeamsResponse.isSuccessful) {
                val memberships = existingTeamsResponse.body() ?: emptyList()
                val activeMemberships = memberships.filter { it.role != TeamRole.INVITED }
                if (activeMemberships.isNotEmpty()) {
                    emit(Result.failure(Exception("You can only be in one team at a time. Leave your current team first.")))
                    return@flow
                }
            }

            val request = CreateTeamRequest(name = name, leaderId = leaderId, description = description, minPlayers = 5, maxPlayers = 7, isOpenForApplications = isOpenForApplications)
            val response = api.createTeam(request)
            if (response.isSuccessful) {
                val created = response.body()?.firstOrNull()
                if (created != null) {
                    api.addTeamMember(AddTeamMemberRequest(teamId = created.id, userId = leaderId, role = TeamRole.LEADER))
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
            // Filter out invited status — only show teams where user is active
            val activeTeamIds = memberships
                .filter { it.role != TeamRole.INVITED }
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
            val addResponse = api.addTeamMember(AddTeamMemberRequest(teamId = teamId, userId = userId, role = TeamRole.MEMBER))
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
            // Ownership: only team leader may change roles
            val teamResponse = api.getTeamById(PostgrestFilter.eq(teamId))
            val team = teamResponse.body()?.firstOrNull()
            if (team == null) { emit(Result.failure(Exception("Team not found"))); return@flow }
            AuthorizationUtils.requireLeader(team.leaderId, "update player roles")
                .onFailure { emit(Result.failure(it)); return@flow }

            val roleStr = when (newRole) {
                PlayerRole.LEADER -> TeamRole.LEADER
                PlayerRole.CO_LEADER -> TeamRole.CO_LEADER
                PlayerRole.MEMBER -> TeamRole.MEMBER
            }

            // If we are handing over leadership
            if (newRole == PlayerRole.LEADER) {
                // 1. Get the current team to find the old leader
                val teamResponse = api.getTeamById(PostgrestFilter.eq(teamId))
                val oldLeaderId = teamResponse.body()?.firstOrNull()?.leaderId

                // 2. Update the teams table with the new leader_id
                api.updateTeam(teamId, mapOf("leader_id" to playerId))

                // 3. Demote the old leader to CO_LEADER (if they exist and are different from the new leader)
                if (oldLeaderId != null && oldLeaderId != playerId) {
                    api.updateTeamMemberRole(PostgrestFilter.eq(teamId), PostgrestFilter.eq(oldLeaderId), mapOf("role" to TeamRole.CO_LEADER))
                }
            }

            // 4. Update the target member's role
            val r = api.updateTeamMemberRole(PostgrestFilter.eq(teamId), PostgrestFilter.eq(playerId), mapOf("role" to roleStr))
            
            if (r.isSuccessful) {
                invalidateTeamCaches()
                getTeam(teamId).collect { emit(it) }
            } else {
                emit(Result.failure(Exception("Failed to update player role")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    override suspend fun deleteTeam(teamId: String): Flow<Result<Unit>> = flow {
        try {
            // Ownership: only team leader may delete the team
            val teamResponse = api.getTeamById(PostgrestFilter.eq(teamId))
            val team = teamResponse.body()?.firstOrNull()
            if (team == null) { emit(Result.failure(Exception("Team not found"))); return@flow }
            AuthorizationUtils.requireLeader(team.leaderId, "delete this team")
                .onFailure { emit(Result.failure(it)); return@flow }

            val r = api.deleteTeam(PostgrestFilter.eq(teamId))
            if (r.isSuccessful) { invalidateTeamCaches(); teamDao.deleteById(teamId); emit(Result.success(Unit)) }
            else emit(Result.failure(Exception("Failed to delete team")))
        } catch (e: Exception) { emit(Result.failure(e)) }
    }

    override suspend fun sendInvite(teamId: String, teamName: String, invitedBy: String, invitedByName: String, invitedUserId: String, invitedUserName: String): Flow<Result<TeamInvite>> = flow {
        try {
            // Ownership: only team leader may send invites
            val teamResponse = api.getTeamById(PostgrestFilter.eq(teamId))
            val team = teamResponse.body()?.firstOrNull()
            if (team == null) { emit(Result.failure(Exception("Team not found"))); return@flow }
            AuthorizationUtils.requireLeader(team.leaderId, "send invites for this team")
                .onFailure { emit(Result.failure(it)); return@flow }

            val r = api.addTeamMember(AddTeamMemberRequest(teamId = teamId, userId = invitedUserId, role = TeamRole.INVITED))
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
            // Ownership: only the invited user may accept the invite
            AuthorizationUtils.requireOwner(member.userId, "accept this invite")
                .onFailure { emit(Result.failure(it)); return@flow }
            val ur = api.updateTeamMemberRole(PostgrestFilter.eq(member.teamId), PostgrestFilter.eq(member.userId), mapOf("role" to TeamRole.MEMBER))
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
            // Ownership: only the invited user (or team leader) may decline the invite
            val userId = AuthorizationUtils.currentUserId()
            val isLeader = try {
                api.getTeamById(PostgrestFilter.eq(member.teamId)).body()?.firstOrNull()?.leaderId == userId
            } catch (_: Exception) { false }
            if (userId != member.userId && !isLeader) {
                emit(Result.failure(SecurityException("Forbidden: you do not have permission to decline this invite")))
                return@flow
            }
            val dr = api.removeTeamMember(PostgrestFilter.eq(member.teamId), PostgrestFilter.eq(member.userId))
            if (dr.isSuccessful) { invalidateTeamCaches(); emit(Result.success(Unit)) }
            else emit(Result.failure(Exception("Failed to decline invite")))
        } catch (e: Exception) { emit(Result.failure(e)) }
    }

    override suspend fun getInvitesForPlayer(userId: String): Flow<Result<List<TeamInvite>>> = flow {
        try {
            val r = api.getTeamMembers(userId = PostgrestFilter.eq(userId))
            if (!r.isSuccessful) { emit(Result.failure(Exception("Failed to fetch invites"))); return@flow }
            val memberships = (r.body() ?: emptyList()).filter { it.role == TeamRole.INVITED }
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
            val memberships = (r.body() ?: emptyList()).filter { it.role == TeamRole.INVITED }
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
            // Ownership: only team leader or the player themselves may remove a player
            val teamResponse = api.getTeamById(PostgrestFilter.eq(teamId))
            val team = teamResponse.body()?.firstOrNull()
            if (team == null) { emit(Result.failure(Exception("Team not found"))); return@flow }
            val userId = AuthorizationUtils.currentUserId()
            if (userId != team.leaderId && userId != playerId) {
                emit(Result.failure(SecurityException("Forbidden: you do not have permission to remove this player")))
                return@flow
            }
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
            Player(id = m.userId, name = profile?.username ?: m.userId.take(8), role = if (m.role == TeamRole.LEADER) PlayerRole.LEADER else PlayerRole.MEMBER, email = profile?.email ?: "", avatarUrl = profile?.avatarUrl)
        }, maxPlayers = dto.maxPlayers, minPlayers = dto.minPlayers, totalScrims = dto.completedScrims, completedScrims = dto.completedScrims, reputation = dto.reputation, noShows = dto.noShows, canPostScrimsUntil = parseCanPostScrimsUntil(dto.canPostScrimsUntil), logoUrl = dto.logoUrl, isOpenForApplications = dto.isOpenForApplications, createdAt = parseCreatedAt(dto.createdAt))
    }

    private fun mapTeamToEntity(team: Team): TeamEntity {
        return TeamEntity(id = team.id, name = team.name, leaderId = team.leaderId, description = null, minPlayers = team.minPlayers, maxPlayers = team.maxPlayers, completedScrims = team.completedScrims, reputation = team.reputation, noShows = team.noShows, memberIdsJson = team.players.joinToString(",") { it.id }, logoUrl = team.logoUrl, isOpenForApplications = team.isOpenForApplications)
    }

    private fun mapEntityToTeam(e: TeamEntity): Team {
        val memberIds = e.memberIdsJson?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
        return Team(id = e.id, name = e.name, leaderId = e.leaderId, players = memberIds.map { Player(id = it, name = it.take(8), role = PlayerRole.MEMBER, email = "") }, maxPlayers = e.maxPlayers, minPlayers = e.minPlayers, completedScrims = e.completedScrims, reputation = e.reputation, noShows = e.noShows, isOpenForApplications = e.isOpenForApplications)
    }

    private fun parseCanPostScrimsUntil(raw: String?): Long = DateUtils.parseIsoToMillis(raw, fallback = 0L)

    private fun parseCreatedAt(raw: String?): Long = DateUtils.parseIsoToMillis(raw)

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
            // Fetch application first to determine team and verify ownership
            val appResponse = api.getTeamApplicationById(applicationId)
            if (!appResponse.isSuccessful || appResponse.body().isNullOrEmpty()) {
                emit(Result.failure(Exception("Application not found")))
                return@flow
            }
            val app = appResponse.body()!!.first()
            val teamResponse = api.getTeamById(PostgrestFilter.eq(app.teamId))
            val team = teamResponse.body()?.firstOrNull()
            if (team == null) { emit(Result.failure(Exception("Team not found"))); return@flow }
            AuthorizationUtils.requireLeader(team.leaderId, "accept applications for this team")
                .onFailure { emit(Result.failure(it)); return@flow }

            val r = api.updateTeamApplication(applicationId, mapOf("status" to "Accepted", "responded_at" to DateUtils.formatIsoUtc(System.currentTimeMillis())))
            if (r.isSuccessful) {
                val updatedApp = r.body()?.firstOrNull()
                if (updatedApp != null) {
                    // Add applicant as team member
                    api.addTeamMember(AddTeamMemberRequest(teamId = updatedApp.teamId, userId = updatedApp.applicantUserId, role = TeamRole.MEMBER))
                    invalidateTeamCaches()
                    getTeam(updatedApp.teamId).collect { emit(it) }
                } else emit(Result.failure(Exception("Application not found")))
            } else emit(Result.failure(Exception("Failed to accept application")))
        } catch (e: Exception) { emit(Result.failure(e)) }
    }

    override suspend fun declineApplication(applicationId: String): Flow<Result<Unit>> = flow {
        try {
            // Fetch application first to determine team and verify ownership
            val appResponse = api.getTeamApplicationById(applicationId)
            if (!appResponse.isSuccessful || appResponse.body().isNullOrEmpty()) {
                emit(Result.failure(Exception("Application not found")))
                return@flow
            }
            val app = appResponse.body()!!.first()
            val teamResponse = api.getTeamById(PostgrestFilter.eq(app.teamId))
            val team = teamResponse.body()?.firstOrNull()
            if (team == null) { emit(Result.failure(Exception("Team not found"))); return@flow }
            AuthorizationUtils.requireLeader(team.leaderId, "decline applications for this team")
                .onFailure { emit(Result.failure(it)); return@flow }

            val r = api.updateTeamApplication(applicationId, mapOf("status" to "Declined", "responded_at" to DateUtils.formatIsoUtc(System.currentTimeMillis())))
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

    // ═══════════════════════════════════════════════════════════════
    // REALTIME SUBSCRIPTIONS
    // ═══════════════════════════════════════════════════════════════

    override fun subscribeToTeam(teamId: String): Flow<Team> = flow {
        try {
            realtimeClient.connect()
            val channelName = "public:teams:team_$teamId"
            realtimeClient.subscribe(
                channelName = channelName,
                configs = listOf(
                    SupabaseRealtimeClient.PostgresChangeConfig(
                        event = "UPDATE",
                        table = SupabaseConfig.TABLE_TEAMS,
                        filter = "id=eq.$teamId"
                    )
                )
            ).filter { event ->
                event.eventType == SupabaseRealtimeClient.EVENT_UPDATE && event.record != null
            }.collect { event ->
                try {
                    val dto = parseRealtimeRecordToTeamDto(event.record!!)
                    if (dto.id == teamId) {
                        invalidateTeamCaches()
                        emit(mapTeamDtoToModel(dto))
                    }
                } catch (e: Exception) {
                    Timber.w("TeamRepo", "Failed to parse Realtime UPDATE: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Timber.w("TeamRepo", "Realtime subscription failed for team $teamId: ${e.message}")
        }
    }

    override fun subscribeToTeamInvites(userId: String): Flow<TeamInvite> = flow {
        try {
            realtimeClient.connect()
            val channelName = "public:team_invitations:user_$userId"
            realtimeClient.subscribe(
                channelName = channelName,
                configs = listOf(
                    SupabaseRealtimeClient.PostgresChangeConfig(
                        event = "*",  // Listen for both INSERT (new invite) and UPDATE (accept/decline)
                        table = SupabaseConfig.TABLE_TEAM_INVITATIONS,
                        filter = "invited_user_id=eq.$userId"
                    )
                )
            ).filter { event ->
                (event.eventType == SupabaseRealtimeClient.EVENT_INSERT ||
                        event.eventType == SupabaseRealtimeClient.EVENT_UPDATE) && event.record != null
            }.collect { event ->
                try {
                    val partial = parseRealtimeRecordToTeamInvite(event.record!!)
                    // Fetch team name and inviter profile to populate blank names
                    val enriched = enrichTeamInvite(partial)
                    emit(enriched)
                } catch (e: Exception) {
                    Timber.w("TeamRepo", "Failed to parse Realtime invite event: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Timber.w("TeamRepo", "Realtime subscription failed for invites user $userId: ${e.message}")
        }
    }

    /**
     * Fetch team name and inviter profile to fill in blank names from Realtime payload.
     */
    private suspend fun enrichTeamInvite(invite: TeamInvite): TeamInvite {
        var result = invite
        // Fetch team name if missing
        if (invite.teamName.isBlank()) {
            try {
                val teamResponse = api.getTeamById(PostgrestFilter.eq(invite.teamId))
                if (teamResponse.isSuccessful) {
                    result = result.copy(teamName = teamResponse.body()?.firstOrNull()?.name ?: "")
                }
            } catch (e: Exception) { Timber.w("TeamRepo", "Failed to enrich team name", e) }
        }
        // Fetch inviter profile if name missing
        if (invite.invitedByName.isBlank() && invite.invitedBy.isNotBlank()) {
            try {
                val profiles = profileCache.getProfiles(listOf(invite.invitedBy))
                val name = profiles[invite.invitedBy]?.username ?: ""
                if (name.isNotBlank()) {
                    result = result.copy(invitedByName = name)
                }
            } catch (e: Exception) { Timber.w("TeamRepo", "Failed to enrich inviter name", e) }
        }
        // Fetch invited user profile if name missing
        if (invite.invitedUserName.isBlank() && invite.invitedUserId.isNotBlank()) {
            try {
                val profiles = profileCache.getProfiles(listOf(invite.invitedUserId))
                val name = profiles[invite.invitedUserId]?.username ?: ""
                if (name.isNotBlank()) {
                    result = result.copy(invitedUserName = name)
                }
            } catch (e: Exception) { Timber.w("TeamRepo", "Failed to enrich invited user name", e) }
        }
        return result
    }

    private fun parseRealtimeRecordToTeamDto(record: com.google.gson.JsonObject): TeamDto {
        return TeamDto(
            id = record.get("id")?.asString ?: "",
            name = record.get("name")?.asString ?: "",
            leaderId = record.get("leader_id")?.asString ?: "",
            description = record.get("description")?.asString,
            minPlayers = record.get("min_players")?.asInt ?: 5,
            maxPlayers = record.get("max_players")?.asInt ?: 7,
            totalXp = record.get("total_xp")?.asInt ?: 0,
            currentTier = record.get("current_tier")?.asString ?: "Bronze",
            currentDivision = record.get("current_division")?.asInt ?: 1,
            availableDays = record.get("available_days")?.asJsonArray?.map { it.asString },
            totalScrims = record.get("total_scrims")?.asInt ?: 0,
            completedScrims = record.get("completed_scrims")?.asInt ?: 0,
            reputation = record.get("reputation")?.asFloat ?: 5.0f,
            canPostScrimsUntil = record.get("can_post_scrims_until")?.asString,
            noShows = record.get("no_shows")?.asInt ?: 0,
            availableTimeStart = record.get("available_time_start")?.asString,
            availableTimeEnd = record.get("available_time_end")?.asString,
            timezone = record.get("timezone")?.asString,
            isOpenForApplications = record.get("is_open_for_applications")?.asBoolean ?: false,
            createdAt = record.get("created_at")?.asString ?: ""
        )
    }

    // ─── Team Stats & Ratings ───

    override suspend fun getTeamStats(teamId: String): Flow<Result<Map<String, Any>>> = flow {
        try {
            val response = api.getTeamStats(mapOf("p_team_id" to teamId))
            if (response.isSuccessful) {
                emit(Result.success(response.body() ?: emptyMap()))
            } else {
                emit(Result.failure(Exception("Failed to load team stats: ${response.code()}")))
            }
        } catch (e: Exception) { emit(Result.failure(e)) }
    }

    override suspend fun getTeamRatings(teamId: String): Flow<Result<List<TeamRating>>> = flow {
        try {
            val response = api.getTeamRatings(teamId)
            if (response.isSuccessful) {
                val ratings = response.body()?.map { dto ->
                    TeamRating(
                        id = dto.id,
                        teamId = dto.teamId,
                        raterTeamId = dto.raterTeamId,
                        raterTeamName = dto.raterTeamName,
                        raterUserName = dto.raterUserName,
                        rating = dto.rating,
                        feedback = dto.feedback ?: "",
                        createdAt = DateUtils.parseIsoToMillis(dto.createdAt)
                    )
                } ?: emptyList()
                emit(Result.success(ratings))
            } else {
                emit(Result.failure(Exception("Failed to load ratings: ${response.code()}")))
            }
        } catch (e: Exception) { emit(Result.failure(e)) }
    }

    override suspend fun submitTeamRating(
        teamId: String,
        raterTeamId: String,
        raterUserId: String,
        rating: Int,
        feedback: String
    ): Flow<Result<Unit>> = flow {
        try {
            val response = api.createTeamRating(mapOf(
                "team_id" to teamId,
                "rater_team_id" to raterTeamId,
                "rater_user_id" to raterUserId,
                "rating" to rating.coerceIn(1, 5),
                "feedback" to feedback
            ))
            if (response.isSuccessful) {
                emit(Result.success(Unit))
            } else {
                emit(Result.failure(Exception("Failed to submit rating: ${response.code()}")))
            }
        } catch (e: Exception) { emit(Result.failure(e)) }
    }

    private fun parseRealtimeRecordToTeamInvite(record: com.google.gson.JsonObject): TeamInvite {
        return TeamInvite(
            id = record.get("id")?.asString ?: "",
            teamId = record.get("team_id")?.asString ?: "",
            teamName = "",  // Will be populated on next fetch
            invitedUserId = record.get("invited_user_id")?.asString ?: "",
            invitedUserName = "",
            invitedBy = record.get("invited_by")?.asString ?: "",
            invitedByName = "",
            status = when (record.get("status")?.asString) {
                "Accepted" -> com.mlbb.scrim.data.model.InviteStatus.ACCEPTED
                "Declined", "Rejected" -> com.mlbb.scrim.data.model.InviteStatus.DECLINED
                else -> com.mlbb.scrim.data.model.InviteStatus.PENDING
            },
            createdAt = DateUtils.parseIsoToMillis(record.get("created_at")?.asString)
        )
    }
}

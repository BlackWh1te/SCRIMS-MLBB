package com.scrimslegends.app.data.repository

import com.scrimslegends.app.data.cache.ProfileCacheRepository
import com.scrimslegends.app.data.cache.UnifiedCacheManager
import com.scrimslegends.app.data.local.LeaderboardDao
import com.scrimslegends.app.data.local.LeaderboardEntity
import com.scrimslegends.app.data.model.LeaderboardEntry
import com.scrimslegends.app.data.model.RankTier
import com.scrimslegends.app.data.model.TeamLeaderboardEntry
import com.scrimslegends.app.data.service.LeaderboardEntryDto
import com.scrimslegends.app.data.service.PostgrestFilter
import com.scrimslegends.app.data.service.SupabaseConfig
import com.scrimslegends.app.data.service.SupabaseRealtimeClient
import com.scrimslegends.app.data.service.SupabaseService
import com.scrimslegends.app.data.service.TeamDto
import timber.log.Timber
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow

class SupabaseLeaderboardRepository(
    private val cacheManager: UnifiedCacheManager,
    private val leaderboardDao: LeaderboardDao,
    private val realtimeClient: SupabaseRealtimeClient
) : LeaderboardRepositoryInterface {

    companion object {
        private const val CACHE_KEY = "leaderboard"
        private const val CACHE_KEY_TIER_PREFIX = "leaderboard_tier_"
        private const val MEMORY_TTL_MS = 15L * 60 * 1000  // 15 minutes
        private const val ROOM_TTL_MS = 60L * 60 * 1000    // 1 hour
    }

    private val api = SupabaseService.api

    override suspend fun getLeaderboard(): Flow<Result<List<LeaderboardEntry>>> = flow {
        try {
            cacheManager.getFlow<List<LeaderboardEntry>>(
                key = CACHE_KEY,
                memoryTtlMs = MEMORY_TTL_MS,
                roomTtlMs = ROOM_TTL_MS,
                roomLoader = {
                    val cached = leaderboardDao.getAll()
                    if (cached.isNotEmpty()) cached.map { mapEntityToModel(it) } else null
                },
                networkLoader = {
                    val response = api.getLeaderboard()
                    if (response.isSuccessful) {
                        response.body()?.mapIndexed { index, dto ->
                            mapDtoToModel(dto, index + 1)
                        } ?: emptyList()
                    } else {
                        throw Exception("Failed to fetch leaderboard")
                    }
                },
                roomSaver = { entries ->
                    leaderboardDao.deleteAll()
                    leaderboardDao.insertAll(entries.map { mapModelToEntity(it) })
                }
            ).collect { entries ->
                emit(Result.success(entries))
            }
        } catch (e: Exception) {
            // Try Room fallback on network failure
            try {
                val cached = leaderboardDao.getAll()
                if (cached.isNotEmpty()) {
                    emit(Result.success(cached.map { mapEntityToModel(it) }))
                } else {
                    emit(Result.failure(e))
                }
            } catch (_: Exception) {
                emit(Result.failure(e))
            }
        }
    }

    override suspend fun getLeaderboardForTier(tier: RankTier): Flow<Result<List<LeaderboardEntry>>> = flow {
        try {
            val cacheKey = "$CACHE_KEY_TIER_PREFIX${tier.name}"
            val entries = cacheManager.get<List<LeaderboardEntry>>(
                key = cacheKey,
                memoryTtlMs = MEMORY_TTL_MS,
                roomTtlMs = ROOM_TTL_MS,
                roomLoader = {
                    val cached = leaderboardDao.getAll().filter { it.tier == tier.name }
                    if (cached.isNotEmpty()) cached.map { mapEntityToModel(it) } else null
                },
                networkLoader = {
                    val response = api.getLeaderboard(range = "0-99")
                    if (response.isSuccessful) {
                        response.body()?.mapIndexed { index, dto ->
                            mapDtoToModel(dto, index + 1)
                        }?.filter { it.currentTier == tier } ?: emptyList()
                    } else {
                        throw Exception("Failed to fetch leaderboard for tier")
                    }
                },
                roomSaver = { /* Already saved by main leaderboard fetch */ }
            )
            emit(Result.success(entries))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    override suspend fun getTeamLeaderboard(): Flow<Result<List<TeamLeaderboardEntry>>> = flow {
        try {
            val teamsResponse = api.getTeams()
            if (!teamsResponse.isSuccessful) {
                emit(Result.failure(Exception("Failed to fetch team leaderboard")))
                return@flow
            }

            val teamDtos = teamsResponse.body() ?: emptyList()
            if (teamDtos.isEmpty()) {
                emit(Result.success(emptyList()))
                return@flow
            }

            val teamIds = teamDtos.map { it.id }.filter { it.isNotBlank() }
            val memberCounts = mutableMapOf<String, Int>()
            if (teamIds.isNotEmpty()) {
                try {
                    val membersResponse = api.getTeamMembers(teamId = PostgrestFilter.inList(teamIds))
                    if (membersResponse.isSuccessful) {
                        val members = membersResponse.body() ?: emptyList()
                        memberCounts.putAll(members.groupingBy { it.teamId }.eachCount())
                    }
                } catch (e: Exception) {
                    Timber.w("LeaderboardRepo", "Failed to fetch team member counts: ${e.message}")
                }
            }

            val rankedTeams = teamDtos
                .map { dto -> mapTeamDtoToLeaderboardEntry(dto, memberCounts[dto.id] ?: 0) }
                .sortedWith(
                    compareByDescending<TeamLeaderboardEntry> { it.points }
                        .thenByDescending { it.totalMatches }
                        .thenByDescending { it.reputation }
                        .thenBy { it.teamName.lowercase() }
                )
                .mapIndexed { index, entry -> entry.copy(rank = index + 1) }

            emit(Result.success(rankedTeams))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    private fun mapDtoToModel(dto: LeaderboardEntryDto, rank: Int): LeaderboardEntry {
        val pts = dto.pts
        val tier = when {
            pts >= 17000 -> RankTier.MYTHIC
            pts >= 12000 -> RankTier.LEGEND
            pts >= 8000 -> RankTier.EPIC
            pts >= 5000 -> RankTier.GRANDMASTER
            pts >= 2500 -> RankTier.MASTER
            pts >= 1000 -> RankTier.ELITE
            else -> RankTier.WARRIOR
        }

        return LeaderboardEntry(
            rank = rank,
            playerId = dto.userId,
            username = dto.profile?.username ?: "Player",
            teamName = "",
            avatarUrl = dto.profile?.avatarUrl,
            xp = pts,
            wins = dto.wins,
            losses = dto.losses,
            totalMatches = dto.matchesPlay,
            currentTier = tier
        )
    }

    private fun mapTeamDtoToLeaderboardEntry(dto: TeamDto, memberCount: Int): TeamLeaderboardEntry {
        val completed = dto.completedScrims.coerceAtLeast(0)
        return TeamLeaderboardEntry(
            teamId = dto.id,
            teamName = dto.name.ifBlank { "Team" },
            logoUrl = dto.logoUrl,
            memberCount = memberCount,
            points = dto.totalXp,
            wins = 0,
            losses = 0,
            totalMatches = completed,
            reputation = dto.reputation,
            currentTier = parseTeamTier(dto.currentTier, dto.totalXp)
        )
    }

    private fun parseTeamTier(tierName: String, points: Int): RankTier {
        return RankTier.values().firstOrNull {
            it.name.equals(tierName, ignoreCase = true) ||
                it.displayName.equals(tierName, ignoreCase = true)
        } ?: RankTier.fromXp(points)
    }

    private fun mapModelToEntity(entry: LeaderboardEntry): LeaderboardEntity {
        return LeaderboardEntity(
            odinalRank = entry.rank,
            userId = entry.playerId,
            username = entry.username,
            avatarUrl = entry.avatarUrl,
            pts = entry.xp,
            wins = entry.wins,
            losses = entry.losses,
            matchesPlayed = entry.totalMatches,
            tier = entry.currentTier.name,
            lastUpdated = System.currentTimeMillis()
        )
    }

    private fun mapEntityToModel(entity: LeaderboardEntity): LeaderboardEntry {
        return LeaderboardEntry(
            rank = entity.odinalRank,
            playerId = entity.userId,
            username = entity.username,
            teamName = "",
            avatarUrl = entity.avatarUrl,
            xp = entity.pts,
            wins = entity.wins,
            losses = entity.losses,
            totalMatches = entity.matchesPlayed,
            currentTier = try { RankTier.valueOf(entity.tier) } catch (_: Exception) { RankTier.WARRIOR }
        )
    }

    // ═══════════════════════════════════════════════════════════════
    // REALTIME SUBSCRIPTIONS
    // ═══════════════════════════════════════════════════════════════

    override fun subscribeToLeaderboard(): Flow<LeaderboardEntry> = flow {
        try {
            realtimeClient.connect()
            val channelName = "public:player_stats"
            realtimeClient.subscribe(
                channelName = channelName,
                configs = listOf(
                    SupabaseRealtimeClient.PostgresChangeConfig(
                        event = "UPDATE",
                        table = SupabaseConfig.TABLE_PLAYER_STATS
                    )
                )
            ).filter { event ->
                event.eventType == SupabaseRealtimeClient.EVENT_UPDATE && event.record != null
            }.collect { event ->
                try {
                    val record = event.record ?: return@collect
                    val entry = parseRealtimeRecordToLeaderboardEntry(record)
                    emit(entry)
                } catch (e: Exception) {
                    Timber.w("LeaderboardRepo", "Failed to parse Realtime UPDATE: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Timber.w("LeaderboardRepo", "Realtime subscription failed for leaderboard: ${e.message}")
        }
    }

    private fun parseRealtimeRecordToLeaderboardEntry(record: com.google.gson.JsonObject): LeaderboardEntry {
        val pts = record.get("pts")?.asInt ?: 0
        val tier = when {
            pts >= 17000 -> RankTier.MYTHIC
            pts >= 12000 -> RankTier.LEGEND
            pts >= 8000 -> RankTier.EPIC
            pts >= 5000 -> RankTier.GRANDMASTER
            pts >= 2500 -> RankTier.MASTER
            pts >= 1000 -> RankTier.ELITE
            else -> RankTier.WARRIOR
        }
        return LeaderboardEntry(
            rank = 0, // Rank is positional, will be recalculated on full refresh
            playerId = record.get("user_id")?.asString ?: "",
            username = "", // Profile name fetched separately
            teamName = "",
            xp = pts,
            wins = record.get("wins")?.asInt ?: 0,
            losses = record.get("losses")?.asInt ?: 0,
            totalMatches = record.get("matches_play")?.asInt ?: 0,
            currentTier = tier
        )
    }
}

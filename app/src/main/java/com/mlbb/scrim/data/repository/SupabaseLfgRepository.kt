package com.mlbb.scrim.data.repository

import com.mlbb.scrim.data.cache.UnifiedCacheManager
import com.mlbb.scrim.data.local.LfgPostDao
import com.mlbb.scrim.data.local.LfgPostEntity
import com.mlbb.scrim.data.model.GameRole
import com.mlbb.scrim.data.model.LfgPost
import com.mlbb.scrim.data.model.Region
import com.mlbb.scrim.data.model.SkillLevel
import com.mlbb.scrim.data.service.*
import com.mlbb.scrim.security.AuthorizationUtils
import com.mlbb.scrim.util.DateUtils
import timber.log.Timber
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow

class SupabaseLfgRepository(
    private val cacheManager: UnifiedCacheManager,
    private val lfgPostDao: LfgPostDao,
    private val realtimeClient: SupabaseRealtimeClient
) : LfgRepositoryInterface {

    companion object {
        private const val CACHE_KEY_ALL = "lfg_all"
        private const val CACHE_KEY_PLAYER_PREFIX = "lfg_player_"
        private const val MEMORY_TTL_MS = 5L * 60 * 1000   // 5 minutes
        private const val ROOM_TTL_MS = 30L * 60 * 1000    // 30 minutes
    }

    private val api = SupabaseService.api

    override fun getAllPosts(): Flow<Result<List<LfgPost>>> = flow {
        try {
            cacheManager.getFlow<List<LfgPost>>(
                key = SupabaseSession.userScopedKey(CACHE_KEY_ALL),
                memoryTtlMs = MEMORY_TTL_MS,
                roomTtlMs = ROOM_TTL_MS,
                roomLoader = {
                    val cached = lfgPostDao.getAll()
                    if (cached.isNotEmpty()) cached.map { mapEntityToModel(it) } else null
                },
                networkLoader = {
                    val response = api.getLfgPosts(range = "0-49")
                    if (response.isSuccessful) {
                        response.body()?.map { mapDtoToModel(it) } ?: emptyList()
                    } else {
                        throw Exception("Failed to fetch LFG posts")
                    }
                },
                roomSaver = { posts ->
                    lfgPostDao.deleteAll()
                    lfgPostDao.insertAll(posts.map { mapModelToEntity(it) })
                }
            ).collect { posts ->
                emit(Result.success(posts))
            }
        } catch (e: Exception) {
            // Fallback to Room
            try {
                val cached = lfgPostDao.getAll()
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

    override fun createPost(post: LfgPost): Flow<Result<LfgPost>> = flow {
        try {
            // Ensure player_id matches the authenticated user (required by RLS)
            val authUserId = SupabaseSession.getUserIdOrNull()
            if (authUserId != null && post.playerId != authUserId) {
                emit(Result.failure(Exception("Player ID must match authenticated user")))
                return@flow
            }
            val dto = LfgPostDto(
                id = null,  // Let Supabase auto-generate UUID
                playerId = post.playerId,
                playerName = post.playerName,
                role = post.role.name,
                region = post.region.name,
                skillLevel = post.skillLevel.name,
                message = post.message,
                mainHeroes = post.mainHeroes,
                bio = post.bio,
                rank = post.rank,
                totalMatches = post.totalMatches,
                winRate = post.winRate,
                rankedWinRate = post.rankedWinRate,
                wins = post.wins,
                losses = post.losses,
                pts = post.pts,
                inGameId = post.inGameId,
                city = post.city,
                screenshotUrl = post.screenshotUrl,
                isAvailable = post.isAvailable,
                useMic = post.useMic,
                playstyleTags = post.playstyleTags,
                discord = post.discord,
                telegram = post.telegram,
                vk = post.vk,
                facebook = post.facebook,
                avatarUrl = post.avatarUrl
            )
            val response = api.createLfgPost(dto)
            if (response.isSuccessful) {
                val created = response.body()?.firstOrNull()
                if (created != null) {
                    // Invalidate cache on write (both memory and Room)
                    cacheManager.invalidateByPrefix("lfg_")
                    lfgPostDao.deleteAll()
                    emit(Result.success(mapDtoToModel(created)))
                } else {
                    emit(Result.failure(Exception("Created post not returned")))
                }
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                Timber.e("LfgRepo", "LFG post creation failed: ${response.code()} — $errorBody")
                emit(Result.failure(Exception("Failed to create LFG post: ${response.code()}")))
            }
        } catch (e: Exception) {
            Timber.e("LfgRepo", "LFG post creation exception", e)
            emit(Result.failure(e))
        }
    }

    override fun deletePost(postId: String): Flow<Result<Unit>> = flow {
        try {
            // Ownership: only the post author may delete it
            val postResponse = api.getLfgPostById(PostgrestFilter.eq(postId))
            val post = postResponse.body()?.firstOrNull()
            if (post == null) { emit(Result.failure(Exception("Post not found"))); return@flow }
            AuthorizationUtils.requireOwner(post.playerId, "delete this LFG post")
                .onFailure { emit(Result.failure(it)); return@flow }

            val response = api.deleteLfgPost(PostgrestFilter.eq(postId))
            if (response.isSuccessful) {
                // Invalidate cache on delete
                cacheManager.invalidateByPrefix("lfg_")
                lfgPostDao.deleteById(postId)
                emit(Result.success(Unit))
            } else {
                emit(Result.failure(Exception("Failed to delete LFG post")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    override suspend fun incrementViewCount(postId: String): Result<Unit> = try {
        // Use RPC for atomic increment; fall back to read-then-write if RPC unavailable
        val response = api.rpcIncrementLfgViewCount(mapOf("p_post_id" to postId))
        if (response.isSuccessful) {
            // Invalidate LFG cache so next load fetches fresh view counts from server
            cacheManager.invalidate(CACHE_KEY_ALL)
            Result.success(Unit)
        } else {
            // Fallback: read current count and PATCH with +1
            try {
                val current = api.getLfgPosts(playerId = null, range = null)
                if (current.isSuccessful) {
                    val post = current.body()?.find { it.id == postId }
                    val newCount = (post?.viewCount ?: 0) + 1
                    val patchResponse = api.updateLfgPost(
                        id = PostgrestFilter.eq(postId),
                        body = mapOf("view_count" to newCount)
                    )
                    if (patchResponse.isSuccessful) {
                        cacheManager.invalidate(SupabaseSession.userScopedKey(CACHE_KEY_ALL))
                        Result.success(Unit)
                    }
                    else Result.failure(Exception("Failed to increment view count"))
                } else {
                    Result.failure(Exception("Failed to fetch post for view count update"))
                }
            } catch (e2: Exception) {
                Result.failure(e2)
            }
        }
    } catch (e: Exception) {
        // Fallback for when RPC doesn't exist yet
        try {
            val patchResponse = api.updateLfgPost(
                id = PostgrestFilter.eq(postId),
                body = mapOf("view_count" to 1)  // Best-effort: set to 1 if we can't read current
            )
            if (patchResponse.isSuccessful) {
                cacheManager.invalidate(SupabaseSession.userScopedKey(CACHE_KEY_ALL))
                Result.success(Unit)
            }
            else Result.failure(Exception("Failed to increment view count"))
        } catch (e2: Exception) {
            Result.failure(e2)
        }
    }

    override fun getPostsByPlayer(playerId: String): Flow<Result<List<LfgPost>>> = flow {
        try {
            val cacheKey = "$CACHE_KEY_PLAYER_PREFIX$playerId"
            val posts = cacheManager.get<List<LfgPost>>(
                key = cacheKey,
                memoryTtlMs = MEMORY_TTL_MS,
                roomTtlMs = ROOM_TTL_MS,
                roomLoader = {
                    val cached = lfgPostDao.getByPlayer(playerId)
                    if (cached.isNotEmpty()) cached.map { mapEntityToModel(it) } else null
                },
                networkLoader = {
                    val response = api.getLfgPosts(playerId = PostgrestFilter.eq(playerId))
                    if (response.isSuccessful) {
                        response.body()?.map { mapDtoToModel(it) } ?: emptyList()
                    } else {
                        throw Exception("Failed to fetch player LFG posts")
                    }
                },
                roomSaver = { posts ->
                    lfgPostDao.insertAll(posts.map { mapModelToEntity(it) })
                }
            )
            emit(Result.success(posts))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    // ─── Mapping: DTO ↔ Model ↔ Entity ───

    private fun mapDtoToModel(dto: LfgPostDto): LfgPost {
        return LfgPost(
            id = dto.id ?: "",
            playerId = dto.playerId,
            playerName = dto.playerName,
            role = try { GameRole.valueOf(dto.role) } catch (_: Exception) { GameRole.FLEX },
            region = try { Region.valueOf(dto.region) } catch (_: Exception) { Region.UTC },
            skillLevel = try { SkillLevel.valueOf(dto.skillLevel) } catch (_: Exception) { SkillLevel.ALL },
            message = dto.message,
            mainHeroes = dto.mainHeroes ?: emptyList(),
            bio = dto.bio ?: "",
            rank = dto.rank ?: "",
            totalMatches = dto.totalMatches ?: 0,
            winRate = dto.winRate ?: "",
            rankedWinRate = dto.rankedWinRate ?: "",
            wins = dto.wins ?: 0,
            losses = dto.losses ?: 0,
            pts = dto.pts ?: 0,
            inGameId = dto.inGameId ?: "",
            city = dto.city ?: "",
            screenshotUrl = dto.screenshotUrl ?: "",
            isAvailable = dto.isAvailable ?: true,
            useMic = dto.useMic ?: false,
            playstyleTags = dto.playstyleTags ?: emptyList(),
            discord = dto.discord ?: "",
            telegram = dto.telegram ?: "",
            vk = dto.vk ?: "",
            facebook = dto.facebook ?: "",
            avatarUrl = dto.avatarUrl,
            viewCount = dto.viewCount ?: 0,
            createdAt = DateUtils.parseIsoToMillis(dto.createdAt)
        )
    }

    private fun mapModelToEntity(post: LfgPost): LfgPostEntity {
        return LfgPostEntity(
            id = post.id,
            playerId = post.playerId,
            playerName = post.playerName,
            role = post.role.name,
            region = post.region.name,
            skillLevel = post.skillLevel.name,
            message = post.message,
            mainHeroesJson = post.mainHeroes.joinToString(","),
            bio = post.bio,
            rank = post.rank,
            totalMatches = post.totalMatches,
            winRate = post.winRate,
            rankedWinRate = post.rankedWinRate,
            wins = post.wins,
            losses = post.losses,
            pts = post.pts,
            inGameId = post.inGameId,
            city = post.city,
            screenshotUrl = post.screenshotUrl,
            isAvailable = post.isAvailable,
            useMic = post.useMic,
            playstyleTagsJson = post.playstyleTags.joinToString(","),
            discord = post.discord,
            telegram = post.telegram,
            vk = post.vk,
            facebook = post.facebook,
            avatarUrl = post.avatarUrl,
            viewCount = post.viewCount,
            createdAt = post.createdAt,
            lastUpdated = System.currentTimeMillis()
        )
    }

    private fun mapEntityToModel(entity: LfgPostEntity): LfgPost {
        return LfgPost(
            id = entity.id,
            playerId = entity.playerId,
            playerName = entity.playerName,
            role = try { GameRole.valueOf(entity.role) } catch (_: Exception) { GameRole.FLEX },
            region = try { Region.valueOf(entity.region) } catch (_: Exception) { Region.UTC },
            skillLevel = try { SkillLevel.valueOf(entity.skillLevel) } catch (_: Exception) { SkillLevel.ALL },
            message = entity.message,
            mainHeroes = entity.mainHeroesJson?.split(",")?.filter { it.isNotBlank() } ?: emptyList(),
            bio = entity.bio ?: "",
            rank = entity.rank ?: "",
            totalMatches = entity.totalMatches,
            winRate = entity.winRate ?: "",
            rankedWinRate = entity.rankedWinRate ?: "",
            wins = entity.wins,
            losses = entity.losses,
            pts = entity.pts,
            inGameId = entity.inGameId ?: "",
            city = entity.city ?: "",
            screenshotUrl = entity.screenshotUrl ?: "",
            isAvailable = entity.isAvailable,
            useMic = entity.useMic,
            playstyleTags = entity.playstyleTagsJson?.split(",")?.filter { it.isNotBlank() } ?: emptyList(),
            discord = entity.discord ?: "",
            telegram = entity.telegram ?: "",
            vk = entity.vk ?: "",
            facebook = entity.facebook ?: "",
            avatarUrl = entity.avatarUrl,
            viewCount = entity.viewCount,
            createdAt = entity.createdAt
        )
    }

    // ═══════════════════════════════════════════════════════════════
    // REALTIME SUBSCRIPTIONS
    // ═══════════════════════════════════════════════════════════════

    override fun subscribeToLfgPosts(): Flow<LfgPost> = flow {
        try {
            realtimeClient.connect()
            val channelName = "public:lfg_posts"
            realtimeClient.subscribe(
                channelName = channelName,
                configs = listOf(
                    SupabaseRealtimeClient.PostgresChangeConfig(
                        event = "*",
                        table = SupabaseConfig.TABLE_LFG_POSTS
                    )
                )
            ).filter { event ->
                (event.eventType == SupabaseRealtimeClient.EVENT_INSERT ||
                        event.eventType == SupabaseRealtimeClient.EVENT_UPDATE ||
                        event.eventType == SupabaseRealtimeClient.EVENT_DELETE) && event.record != null
            }.collect { event ->
                try {
                    val post = mapDtoToModel(parseRealtimeRecordToLfgPostDto(event.record!!))
                    emit(post)
                } catch (e: Exception) {
                    Timber.w("LfgRepo", "Failed to parse Realtime LFG event: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Timber.w("LfgRepo", "Realtime subscription failed for LFG posts: ${e.message}")
        }
    }

    private fun parseRealtimeRecordToLfgPostDto(record: com.google.gson.JsonObject): LfgPostDto {
        return LfgPostDto(
            id = record.get("id")?.asString,
            playerId = record.get("player_id")?.asString ?: "",
            playerName = record.get("player_name")?.asString ?: "",
            role = record.get("role")?.asString ?: "FLEX",
            region = record.get("region")?.asString ?: "UTC",
            skillLevel = record.get("skill_level")?.asString ?: "ALL",
            message = record.get("message")?.asString ?: "",
            mainHeroes = record.get("main_heroes")?.asJsonArray?.map { it.asString },
            bio = record.get("bio")?.asString,
            rank = record.get("rank")?.asString,
            totalMatches = record.get("total_matches")?.asInt,
            winRate = record.get("win_rate")?.asString,
            rankedWinRate = record.get("ranked_win_rate")?.asString,
            wins = record.get("wins")?.asInt,
            losses = record.get("losses")?.asInt,
            pts = record.get("pts")?.asInt,
            inGameId = record.get("in_game_id")?.asString,
            city = record.get("city")?.asString,
            screenshotUrl = record.get("screenshot_url")?.asString,
            isAvailable = record.get("is_available")?.asBoolean,
            useMic = record.get("use_mic")?.asBoolean,
            playstyleTags = record.get("playstyle_tags")?.asJsonArray?.map { it.asString },
            discord = record.get("discord")?.asString,
            telegram = record.get("telegram")?.asString,
            vk = record.get("vk")?.asString,
            facebook = record.get("facebook")?.asString,
            avatarUrl = record.get("avatar_url")?.asString,
            viewCount = record.get("view_count")?.asInt,
            createdAt = record.get("created_at")?.asString ?: ""
        )
    }

}

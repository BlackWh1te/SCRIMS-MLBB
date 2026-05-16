package com.mlbb.scrim.data.repository

import com.mlbb.scrim.data.cache.UnifiedCacheManager
import com.mlbb.scrim.data.local.LfgPostDao
import com.mlbb.scrim.data.local.LfgPostEntity
import com.mlbb.scrim.data.model.GameRole
import com.mlbb.scrim.data.model.LfgPost
import com.mlbb.scrim.data.model.Region
import com.mlbb.scrim.data.model.SkillLevel
import com.mlbb.scrim.data.service.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.text.SimpleDateFormat
import java.util.*

class SupabaseLfgRepository(
    private val cacheManager: UnifiedCacheManager,
    private val lfgPostDao: LfgPostDao
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
                key = CACHE_KEY_ALL,
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
            val dto = LfgPostDto(
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
                isAvailable = post.isAvailable,
                useMic = post.useMic,
                playstyleTags = post.playstyleTags,
                discord = post.discord,
                telegram = post.telegram,
                vk = post.vk,
                facebook = post.facebook
            )
            val response = api.createLfgPost(dto)
            if (response.isSuccessful) {
                val created = response.body()?.firstOrNull()
                if (created != null) {
                    // Invalidate cache on write
                    cacheManager.invalidateByPrefix("lfg_")
                    emit(Result.success(mapDtoToModel(created)))
                } else {
                    emit(Result.failure(Exception("Created post not returned")))
                }
            } else {
                emit(Result.failure(Exception("Failed to create LFG post")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    override fun deletePost(postId: String): Flow<Result<Unit>> = flow {
        try {
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
            isAvailable = dto.isAvailable ?: true,
            useMic = dto.useMic ?: false,
            playstyleTags = dto.playstyleTags ?: emptyList(),
            discord = dto.discord ?: "",
            telegram = dto.telegram ?: "",
            vk = dto.vk ?: "",
            facebook = dto.facebook ?: "",
            createdAt = parseDateTime(dto.createdAt)
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
            isAvailable = post.isAvailable,
            useMic = post.useMic,
            playstyleTagsJson = post.playstyleTags.joinToString(","),
            discord = post.discord,
            telegram = post.telegram,
            vk = post.vk,
            facebook = post.facebook,
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
            isAvailable = entity.isAvailable,
            useMic = entity.useMic,
            playstyleTags = entity.playstyleTagsJson?.split(",")?.filter { it.isNotBlank() } ?: emptyList(),
            discord = entity.discord ?: "",
            telegram = entity.telegram ?: "",
            vk = entity.vk ?: "",
            facebook = entity.facebook ?: "",
            createdAt = entity.createdAt
        )
    }

    private fun parseDateTime(dateTime: String?): Long {
        return try {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
                .parse(dateTime ?: "")?.time ?: System.currentTimeMillis()
        } catch (_: Exception) {
            try {
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
                    .parse(dateTime ?: "")?.time ?: System.currentTimeMillis()
            } catch (_: Exception) {
                System.currentTimeMillis()
            }
        }
    }
}

package com.scrimslegends.app.data.repository

import com.scrimslegends.app.data.cache.UnifiedCacheManager
import com.scrimslegends.app.data.local.NotificationDao
import com.scrimslegends.app.data.local.NotificationEntity
import com.scrimslegends.app.data.model.Notification
import com.scrimslegends.app.data.model.NotificationType
import com.scrimslegends.app.data.service.NotificationDto
import com.scrimslegends.app.data.service.PostgrestFilter
import com.scrimslegends.app.data.service.SupabaseConfig
import com.scrimslegends.app.data.service.SupabaseRealtimeClient
import com.scrimslegends.app.data.service.SupabaseService
import com.scrimslegends.app.util.DateUtils
import timber.log.Timber
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow

/**
 * Supabase-backed notification repository with caching.
 * Memory TTL: 1 min | Room TTL: 15 min
 */
class SupabaseNotificationRepository(
    private val cacheManager: UnifiedCacheManager,
    private val notificationDao: NotificationDao,
    private val realtimeClient: SupabaseRealtimeClient
) {
    companion object {
        private const val CACHE_KEY_PREFIX = "notifications_"
        private const val MEMORY_TTL_MS = 1L * 60 * 1000   // 1 minute
        private const val ROOM_TTL_MS = 15L * 60 * 1000    // 15 minutes
    }

    private val api = SupabaseService.api

    suspend fun getNotificationsForUser(userId: String): Flow<Result<List<Notification>>> = flow {
        val cacheKey = "$CACHE_KEY_PREFIX$userId"
        try {
            cacheManager.getFlow<List<Notification>>(
                key = cacheKey,
                memoryTtlMs = MEMORY_TTL_MS,
                roomTtlMs = ROOM_TTL_MS,
                roomLoader = {
                    val cached = notificationDao.getForUser(userId)
                    if (cached.isNotEmpty()) cached.map { mapEntityToModel(it) } else null
                },
                networkLoader = {
                    val response = api.getNotifications(userId = PostgrestFilter.eq(userId))
                    if (response.isSuccessful) {
                        response.body()?.map { mapDtoToModel(it) } ?: emptyList()
                    } else {
                        throw Exception("Failed to fetch notifications")
                    }
                },
                roomSaver = { notifications ->
                    notificationDao.deleteAllForUser(userId)
                    notificationDao.insertAll(notifications.map { mapModelToEntity(it, userId) })
                }
            ).collect { notifications ->
                emit(Result.success(notifications))
            }
        } catch (e: Exception) {
            // Fallback to Room
            try {
                val cached = notificationDao.getForUser(userId)
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

    suspend fun markAsRead(notificationId: String): Flow<Result<Unit>> = flow {
        try {
            val response = api.markNotificationAsRead(PostgrestFilter.eq(notificationId))
            if (response.isSuccessful) {
                // Update local cache too
                notificationDao.markAsRead(notificationId)
                cacheManager.invalidateByPrefix(CACHE_KEY_PREFIX)
                emit(Result.success(Unit))
            } else {
                emit(Result.failure(Exception("Failed to mark notification as read")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    suspend fun markAllAsRead(userId: String): Flow<Result<Unit>> = flow {
        try {
            val response = api.markAllNotificationsAsRead(
                userId = PostgrestFilter.eq(userId),
                body = mapOf("is_read" to true)
            )
            if (response.isSuccessful) {
                notificationDao.markAllAsRead(userId)
                cacheManager.invalidateByPrefix(CACHE_KEY_PREFIX)
                emit(Result.success(Unit))
            } else {
                emit(Result.failure(Exception("Failed to mark all as read")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    suspend fun deleteNotification(notificationId: String): Flow<Result<Unit>> = flow {
        try {
            val response = api.deleteNotification(PostgrestFilter.eq(notificationId))
            if (response.isSuccessful) {
                notificationDao.deleteById(notificationId)
                cacheManager.invalidateByPrefix(CACHE_KEY_PREFIX)
                emit(Result.success(Unit))
            } else {
                emit(Result.failure(Exception("Failed to delete notification")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    suspend fun createNotification(userId: String, type: NotificationType, title: String, message: String, actionId: String? = null): Flow<Result<Notification>> = flow {
        try {
            val dto = NotificationDto(userId = userId, type = type.name, title = title, message = message, actionId = actionId)
            val response = api.createNotification(dto)
            if (response.isSuccessful) {
                val created = response.body()?.firstOrNull()
                if (created != null) {
                    cacheManager.invalidateByPrefix(CACHE_KEY_PREFIX)
                    emit(Result.success(mapDtoToModel(created)))
                } else {
                    emit(Result.failure(Exception("Failed to create notification")))
                }
            } else {
                emit(Result.failure(Exception(response.errorBody()?.string() ?: "Unknown error")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    fun getUnreadCount(notifications: List<Notification>): Int = notifications.count { !it.isRead }

    // ═══════════════════════════════════════════════════════════════
    // REALTIME SUBSCRIPTIONS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Subscribe to Realtime notifications for a specific user.
     * Emits new Notification objects as they are INSERTed into the database.
     */
    fun subscribeToNotifications(userId: String): Flow<Notification> = flow {
        try {
            realtimeClient.connect()
            val channelName = "public:app_notifications:user_$userId"
            realtimeClient.subscribe(
                channelName = channelName,
                configs = listOf(
                    SupabaseRealtimeClient.PostgresChangeConfig(
                        event = "INSERT",
                        table = "app_notifications",
                        filter = "user_id=eq.$userId"
                    )
                )
            ).filter { event ->
                event.eventType == SupabaseRealtimeClient.EVENT_INSERT && event.record != null
            }.collect { event ->
                try {
                    val record = event.record ?: return@collect
                    val dto = parseRealtimeRecordToNotificationDto(record)
                    if (dto.userId == userId) {
                        // Invalidate cache and persist to Room
                        cacheManager.invalidateByPrefix(CACHE_KEY_PREFIX)
                        try {
                            notificationDao.insert(mapModelToEntity(mapDtoToModel(dto), userId))
                        } catch (e: Exception) {
                            Timber.w("NotifRepo", "Failed to persist notification to Room: ${e.message}")
                        }
                        emit(mapDtoToModel(dto))
                    }
                } catch (e: Exception) {
                    Timber.w("NotifRepo", "Failed to parse Realtime INSERT: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Timber.w("NotifRepo", "Realtime subscription failed for user $userId: ${e.message}")
        }
    }

    /**
     * Parse a Realtime INSERT record (JsonObject) into a NotificationDto.
     */
    private fun parseRealtimeRecordToNotificationDto(record: com.google.gson.JsonObject): NotificationDto {
        fun safeStr(key: String): String? =
            record.get(key)?.takeIf { !it.isJsonNull }?.asString?.takeIf { it.isNotBlank() }
        return NotificationDto(
            id        = safeStr("id")         ?: "",
            userId    = safeStr("user_id")    ?: "",
            type      = safeStr("type")       ?: "SYSTEM",
            title     = safeStr("title")      ?: "",
            message   = safeStr("message"),              // canonical; may be null on old rows
            body      = safeStr("body"),                 // legacy fallback
            actionId  = safeStr("action_id"),
            isRead    = record.get("is_read")?.takeIf { !it.isJsonNull }?.asBoolean ?: false,
            createdAt = safeStr("created_at") ?: ""
        )
    }

    // ─── Mapping ───

    private fun mapDtoToModel(dto: NotificationDto): Notification {
        return Notification(
            id = dto.id,
            type = try { NotificationType.valueOf(dto.type) } catch (_: Exception) { NotificationType.SYSTEM },
            title = dto.title,
            message = dto.resolvedMessage,   // coalesces message → body → ""
            timestamp = DateUtils.parseIsoToMillis(dto.createdAt),
            isRead = dto.isRead,
            actionId = dto.actionId ?: ""
        )
    }

    private fun mapModelToEntity(notification: Notification, userId: String): NotificationEntity {
        return NotificationEntity(
            id = notification.id,
            userId = userId,
            type = notification.type.name,
            title = notification.title,
            message = notification.message,
            actionId = notification.actionId,
            isRead = notification.isRead,
            timestamp = notification.timestamp,
            lastUpdated = System.currentTimeMillis()
        )
    }

    private fun mapEntityToModel(entity: NotificationEntity): Notification {
        return Notification(
            id = entity.id,
            type = try { NotificationType.valueOf(entity.type) } catch (_: Exception) { NotificationType.SYSTEM },
            title = entity.title,
            message = entity.message,
            timestamp = entity.timestamp,
            isRead = entity.isRead,
            actionId = entity.actionId ?: ""
        )
    }

}
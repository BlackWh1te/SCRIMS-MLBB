package com.scrimslegends.app.data.service

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.scrimslegends.app.data.local.MessageDao
import com.scrimslegends.app.data.local.MessageEntity
import com.scrimslegends.app.data.model.MessageType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import timber.log.Timber
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * RealtimeManager orchestrates multiplexed Realtime subscriptions for Messaging v2.
 * It opens exactly ONE WebSocket channel (`user_${userId}`) and routes all events
 * into local Room DB or shared flows.
 */
@Singleton
class RealtimeManager @Inject constructor(
    private val realtimeClient: SupabaseRealtimeClient,
    private val messageDao: MessageDao,
    private val applicationScope: CoroutineScope,
    private val gson: Gson
) {
    companion object {
        private const val TAG = "RealtimeManager"
    }

    private val _notifications = MutableSharedFlow<JsonObject>()
    val notifications = _notifications.asSharedFlow()

    fun startMultiplexedSubscription(userId: String) {
        val channelName = "user_$userId"
        
        // Define Postgres CDC filters
        val configs = listOf(
            SupabaseRealtimeClient.PostgresChangeConfig(
                event = "*",
                schema = "public",
                table = "user_notifications",
                filter = "user_id=eq.$userId"
            ),
            SupabaseRealtimeClient.PostgresChangeConfig(
                event = "INSERT",
                schema = "public",
                table = "messages",
                // Filtering purely on recipient/participant would require a view or joining,
                // Supabase Realtime only allows column filters on the base table.
                // For simplicity, we assume RLS handles sending us only authorized messages.
                filter = null 
            ),
            SupabaseRealtimeClient.PostgresChangeConfig(
                event = "*",
                schema = "public",
                table = "conversations",
                // Ideally filter participant_a_id=eq.$userId, but we need OR logic.
                // Assuming RLS restricts what we receive.
                filter = null
            )
        )
        
        Timber.d(TAG, "Starting multiplexed subscription for $userId")
        
        realtimeClient.subscribe(channelName, configs)
            .onEach { event ->
                try {
                    when (event.table) {
                        "user_notifications" -> {
                            event.record?.let { _notifications.emit(it) }
                        }
                        "messages" -> {
                            if (event.eventType == "INSERT" || event.eventType == "UPDATE") {
                                event.record?.let { parseAndInsertMessage(it) }
                            }
                        }
                        "conversations" -> {
                            // Update conversation logic will be handled here or via Paging
                        }
                    }
                } catch (e: Exception) {
                    Timber.e(TAG, "Error handling Realtime event for ${event.table}: ${e.message}")
                }
            }
            .launchIn(applicationScope)
    }

    private suspend fun parseAndInsertMessage(record: JsonObject) {
        try {
            val id = record.get("id")?.asString ?: return
            val conversationId = record.get("conversation_id")?.asString ?: return
            val senderId = record.get("sender_id")?.asString ?: return
            
            val typeString = record.get("type")?.asString ?: "TEXT"
            
            val createdAtStr = record.get("created_at")?.asString
            val timestamp = if (createdAtStr != null) {
                Instant.parse(createdAtStr).toEpochMilli()
            } else {
                System.currentTimeMillis()
            }

            val readAtStr = record.get("read_at")?.takeIf { !it.isJsonNull }?.asString
            val readAt = readAtStr?.let { Instant.parse(it).toEpochMilli() }

            val entity = MessageEntity(
                id = id,
                conversationId = conversationId,
                matchId = record.get("match_id")?.takeIf { !it.isJsonNull }?.asString,
                senderId = senderId,
                senderTeamId = record.get("sender_team_id")?.takeIf { !it.isJsonNull }?.asString,
                senderName = record.get("sender_name")?.takeIf { !it.isJsonNull }?.asString ?: "Unknown",
                content = record.get("content")?.takeIf { !it.isJsonNull }?.asString ?: "",
                timestamp = timestamp,
                isRead = record.get("is_read")?.takeIf { !it.isJsonNull }?.asBoolean ?: false,
                readAt = readAt,
                type = typeString,
                imageUrl = record.get("image_url")?.takeIf { !it.isJsonNull }?.asString,
                voiceUrl = record.get("voice_url")?.takeIf { !it.isJsonNull }?.asString,
                voiceDuration = record.get("voice_duration")?.takeIf { !it.isJsonNull }?.asInt,
                deliveryStatus = record.get("delivery_status")?.takeIf { !it.isJsonNull }?.asString ?: "DELIVERED",
                clientMessageId = record.get("client_message_id")?.takeIf { !it.isJsonNull }?.asString,
                replyToId = record.get("reply_to_id")?.takeIf { !it.isJsonNull }?.asString,
                replyToSnippet = record.get("reply_to_snippet")?.takeIf { !it.isJsonNull }?.asString,
                replyToSenderName = record.get("reply_to_sender_name")?.takeIf { !it.isJsonNull }?.asString,
                isDeleted = record.get("is_deleted")?.takeIf { !it.isJsonNull }?.asBoolean ?: false
            )

            // Insert into Room to auto-invalidate PagingSource
            messageDao.insertMessage(entity)
            Timber.d(TAG, "Inserted Realtime message $id into Room")
        } catch (e: Exception) {
            Timber.e(TAG, "Failed to parse Realtime message: ${e.message}")
        }
    }

    fun stopSubscription(userId: String) {
        Timber.d(TAG, "Stopping multiplexed subscription for $userId")
        val channelName = "user_$userId"
        realtimeClient.unsubscribe(channelName)
    }
}


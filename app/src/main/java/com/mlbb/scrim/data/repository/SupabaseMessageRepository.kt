package com.mlbb.scrim.data.repository

import android.util.Log
import com.mlbb.scrim.data.model.Conversation
import com.mlbb.scrim.data.model.Message
import com.mlbb.scrim.data.model.MessageType
import com.mlbb.scrim.data.service.MessageDto
import com.mlbb.scrim.data.service.ConversationDto
import com.mlbb.scrim.data.service.SupabaseConfig
import com.mlbb.scrim.data.service.SupabaseRealtimeClient
import com.mlbb.scrim.data.service.SupabaseService
import com.mlbb.scrim.util.DateUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow

import java.util.*

import com.mlbb.scrim.data.local.ConversationDao
import com.mlbb.scrim.data.local.MessageDao

class SupabaseMessageRepository(
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao,
    private val realtimeClient: SupabaseRealtimeClient
) : MessageRepositoryInterface {

    private val api = SupabaseService.api

    companion object {
        private const val TAG = "MessageRepo"
    }

    override suspend fun getConversationsForUser(userId: String): Flow<Result<List<Conversation>>> = flow {
        try {
            // Phase 4: Emit cached Room data first for instant UI
            try {
                val cached = conversationDao.getConversationsForUser(userId).first()
                if (cached.isNotEmpty()) {
                    emit(Result.success(cached.map { it.toDomainModel() }))
                }
            } catch (e: Exception) { Log.w(TAG, "Failed to load cached conversations", e) }

            // Then fetch fresh from network
            val response = api.getConversationsForUserRpc(mapOf("p_user_id" to userId))
            if (response.isSuccessful) {
                val list = response.body()?.map { mapDtoToConversation(it) } ?: emptyList()
                // Save to Room for offline access
                try {
                    conversationDao.insertConversations(list.map { mapConversationToEntity(it) })
                } catch (e: Exception) { Log.w(TAG, "Failed to persist conversations to Room", e) }
                emit(Result.success(list))
            } else {
                emit(Result.failure(Exception("Failed to load conversations: ${response.code()}")))
            }
        } catch (e: Exception) {
            // Offline fallback: try Room
            try {
                val cached = conversationDao.getConversationsForUser(userId).first()
                if (cached.isNotEmpty()) {
                    emit(Result.success(cached.map { it.toDomainModel() }))
                } else {
                    emit(Result.failure(e))
                }
            } catch (_: Exception) {
                emit(Result.failure(e))
            }
        }
    }

    override suspend fun getConversationById(conversationId: String): Flow<Result<Conversation?>> = flow {
        try {
            val response = api.getConversations(idFilter = "eq.$conversationId")
            if (response.isSuccessful) {
                val dto = response.body()?.firstOrNull()
                if (dto != null) {
                    val messagesResponse = api.getMessages(conversationId = conversationId)
                    val messages = if (messagesResponse.isSuccessful) {
                        messagesResponse.body()?.map { mapDtoToMessage(it) } ?: emptyList()
                    } else emptyList()
                    
                    emit(Result.success(mapDtoToConversation(dto).copy(messages = messages)))
                } else {
                    emit(Result.success(null))
                }
            } else {
                emit(Result.failure(Exception("Failed to load conversation: ${response.code()}")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    override suspend fun getOrCreateConversation(
        scrimId: String,
        scrimTitle: String,
        participantAId: String,
        participantAName: String,
        participantATeamId: String,
        participantATeamName: String,
        participantBId: String,
        participantBName: String,
        participantBTeamId: String,
        participantBTeamName: String
    ): Flow<Result<Conversation>> = flow {
        try {
            // Search for existing conversation with SAME scrim_id AND SAME participants
            // (either direction: A=sender+B=recipient OR A=recipient+B=sender)
            val filter = "and(scrim_id.eq.$scrimId,participant_a_id.eq.$participantAId,participant_b_id.eq.$participantBId),and(scrim_id.eq.$scrimId,participant_a_id.eq.$participantBId,participant_b_id.eq.$participantAId)"
            val existing = api.getConversations(orFilter = filter)
            if (existing.isSuccessful && !existing.body().isNullOrEmpty()) {
                emit(Result.success(mapDtoToConversation(existing.body()!!.first())))
            } else {
                val newConv = ConversationDto(
                    scrimId = scrimId,
                    participantAId = participantAId,
                    participantAName = participantAName,
                    participantATeamId = participantATeamId,
                    participantATeamName = participantATeamName,
                    participantBId = participantBId,
                    participantBName = participantBName,
                    participantBTeamId = participantBTeamId,
                    participantBTeamName = participantBTeamName,
                    lastMessage = "",
                    lastMessageTime = DateUtils.formatIsoUtc(System.currentTimeMillis()),
                    chatOpensAt = DateUtils.formatIsoUtc(System.currentTimeMillis() + 300_000)
                )
                val response = api.createConversation(newConv)
                if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                    val created = mapDtoToConversation(response.body()!!.first())
                    conversationDao.insertConversation(mapConversationToEntity(created))
                    emit(Result.success(created))
                } else {
                    emit(Result.failure(Exception("Failed to create conversation: ${response.code()}")))
                }
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    override suspend fun sendMessage(
        conversationId: String,
        senderId: String,
        senderName: String,
        content: String,
        type: MessageType,
        imageUrl: String?,
        voiceUrl: String?,
        voiceDuration: Int?
    ): Flow<Result<Message>> = flow {
        try {
            // ── CHAT GATE ENFORCEMENT ──
            // Client-side validation: do not allow messages before chatOpensAt.
            // NOTE: This is a best-effort guard. The definitive enforcement must be
            // implemented via Supabase Row Level Security (RLS) on the messages table.
            val convResponse = api.getConversations(idFilter = "eq.$conversationId")
            if (convResponse.isSuccessful) {
                val conv = convResponse.body()?.firstOrNull()
                val chatOpensAt = DateUtils.parseIsoToMillis(conv?.chatOpensAt, fallback = 0L)
                if (System.currentTimeMillis() < chatOpensAt) {
                    val secondsRemaining = (chatOpensAt - System.currentTimeMillis()) / 1000
                    emit(Result.failure(Exception("Chat is locked. Opens in ${secondsRemaining}s")))
                    return@flow
                }
            }

            val dto = MessageDto(
                conversationId = conversationId,
                senderId = senderId,
                senderName = senderName,
                content = content,
                type = type.name,
                imageUrl = imageUrl,
                voice_url = voiceUrl,
                voiceDuration = voiceDuration
            )
            val response = api.sendMessage(dto)
            if (response.isSuccessful) {
                val sent = response.body()?.firstOrNull()
                if (sent != null) {
                    val message = mapDtoToMessage(sent)
                    // Phase 4: Persist sent message to Room
                    try {
                        messageDao.insertMessage(mapMessageToEntity(message))
                        conversationDao.updateLastMessage(conversationId, content, message.timestamp)
                    } catch (e: Exception) { Log.w(TAG, "Failed to persist sent message to Room", e) }
                    // Also PATCH server-side conversation metadata (best-effort; DB trigger is primary)
                    try {
                        api.updateConversation(
                            conversationId,
                            mapOf(
                                "last_message" to content,
                                "last_message_time" to DateUtils.formatIsoUtc(message.timestamp)
                            )
                        )
                    } catch (e: Exception) { Log.w(TAG, "Failed to update conversation last_message on server", e) }
                    emit(Result.success(message))
                } else {
                    emit(Result.failure(Exception("Message sent but not returned")))
                }
            } else {
                emit(Result.failure(Exception("Failed to send message: ${response.code()}")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    override suspend fun sendApplyMessage(
        scrimId: String,
        scrimTitle: String,
        applicantId: String,
        applicantName: String,
        applicantTeamId: String,
        applicantTeamName: String,
        scrimCreatorId: String,
        scrimCreatorName: String,
        scrimCreatorTeamId: String,
        scrimCreatorTeamName: String,
        teamPlayerCount: Int,
        teamMaxPlayers: Int
    ): Flow<Result<Conversation>> = flow {
        try {
            val convResult = getOrCreateConversation(
                scrimId = scrimId,
                scrimTitle = scrimTitle,
                participantAId = applicantId,
                participantAName = applicantName,
                participantATeamId = applicantTeamId,
                participantATeamName = applicantTeamName,
                participantBId = scrimCreatorId,
                participantBName = scrimCreatorName,
                participantBTeamId = scrimCreatorTeamId,
                participantBTeamName = scrimCreatorTeamName
            )
            var conversation: Conversation? = null
            convResult.collect { result ->
                result.onSuccess { conv -> conversation = conv }
            }
            val conv = conversation ?: run {
                emit(Result.failure(Exception("Failed to create conversation")))
                return@flow
            }

            val applyContent = "$applicantName ($applicantTeamName) applied to join \"$scrimTitle\" [$teamPlayerCount/$teamMaxPlayers players]"
            val messageDto = MessageDto(
                conversationId = conv.id,
                senderId = applicantId,
                senderName = applicantName,
                content = applyContent,
                type = "apply"
            )
            val msgResponse = api.sendMessage(messageDto)
            if (msgResponse.isSuccessful) {
                emit(Result.success(conv))
            } else {
                emit(Result.failure(Exception("Failed to send apply message: ${msgResponse.code()}")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    override suspend fun markConversationAsRead(conversationId: String, userId: String): Flow<Result<Unit>> = flow {
        try {
            api.markConversationAsRead(mapOf("p_conversation_id" to conversationId, "p_user_id" to userId))
            emit(Result.success(Unit))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    override suspend fun setTypingStatus(
        conversationId: String,
        userId: String,
        isTyping: Boolean
    ): Flow<Result<Unit>> = flow {
        try {
            // Determine if user is A or B (requires fetching conversation first or passing role)
            // For now, assume we just update the specific column
            val response = api.getConversations(idFilter = "eq.$conversationId")
            if (response.isSuccessful) {
                val conv = response.body()?.firstOrNull()
                if (conv != null) {
                    val field = if (userId == conv.participantAId) "participant_a_typing" else "participant_b_typing"
                    api.updateConversation(conversationId, mapOf(field to isTyping))
                    emit(Result.success(Unit))
                }
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    override suspend fun startDirectConversation(
        senderId: String,
        senderName: String,
        recipientId: String,
        recipientName: String
    ): Flow<Result<Conversation>> = flow {
        try {
            // Check for existing direct conversation between these two
            // (Filter logic: (A=sender AND B=recipient) OR (A=recipient AND B=sender))
            val filter = "and(participant_a_id.eq.$senderId,participant_b_id.eq.$recipientId),and(participant_a_id.eq.$recipientId,participant_b_id.eq.$senderId)"
            val existing = api.getConversations(orFilter = filter)
            
            if (existing.isSuccessful && !existing.body().isNullOrEmpty()) {
                emit(Result.success(mapDtoToConversation(existing.body()!!.first())))
            } else {
                // Create new direct conversation
                val newConvDto = com.mlbb.scrim.data.service.ConversationDto(
                    id = UUID.randomUUID().toString(),
                    participantAId = senderId,
                    participantAName = senderName,
                    participantBId = recipientId,
                    participantBName = recipientName,
                    lastMessage = "Conversation started",
                    lastMessageTime = DateUtils.formatIsoUtc(System.currentTimeMillis())
                )
                val createResponse = api.createConversation(newConvDto)
                if (createResponse.isSuccessful) {
                    emit(Result.success(mapDtoToConversation(createResponse.body()!!.first())))
                } else {
                    emit(Result.failure(Exception("Failed to create direct conversation: ${createResponse.code()}")))
                }
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    override fun subscribeToMessages(conversationId: String): Flow<Message> = flow {
        // ── Phase 1: Emit cached Room messages for instant display ──
        try {
            val cached = messageDao.getMessagesForConversation(conversationId).first()
            cached.forEach { entity -> emit(entity.toDomainModel()) }
        } catch (e: Exception) { Log.w(TAG, "Failed to load cached messages", e) }

        // ── Phase 2: Supabase Realtime (WebSocket) for live updates ──
        try {
            realtimeClient.connect()
            val channelName = "public:${SupabaseConfig.TABLE_MESSAGES}:conv_$conversationId"
            realtimeClient.subscribe(
                channelName = channelName,
                configs = listOf(
                    SupabaseRealtimeClient.PostgresChangeConfig(
                        event = "INSERT",
                        table = SupabaseConfig.TABLE_MESSAGES,
                        filter = "conversation_id=eq.$conversationId"
                    )
                )
            ).filter { event ->
                event.eventType == SupabaseRealtimeClient.EVENT_INSERT && event.record != null
            }.collect { event ->
                try {
                    val dto = parseRealtimeRecordToMessageDto(event.record!!)
                    if (dto.conversationId == conversationId) {
                        val message = mapDtoToMessage(dto)
                        try {
                            messageDao.insertMessage(mapMessageToEntity(message))
                        } catch (e: Exception) { Log.w(TAG, "Failed to persist realtime message to Room", e) }
                        emit(message)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to parse Realtime INSERT: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Realtime subscription ended for messages: ${e.message}")
        }
    }

    override fun subscribeToConversation(conversationId: String): Flow<Conversation> = flow {
        // Supabase Realtime for conversation UPDATE events
        try {
            realtimeClient.connect()
            val channelName = "public:conversations:conv_$conversationId"
            realtimeClient.subscribe(
                channelName = channelName,
                configs = listOf(
                    SupabaseRealtimeClient.PostgresChangeConfig(
                        event = "UPDATE",
                        table = "conversations",
                        filter = "id=eq.$conversationId"
                    )
                )
            ).filter { event ->
                event.eventType == SupabaseRealtimeClient.EVENT_UPDATE && event.record != null
            }.collect { event ->
                try {
                    val dto = parseRealtimeRecordToConversationDto(event.record!!)
                    if (dto.id == conversationId) {
                        emit(mapDtoToConversation(dto))
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to parse Realtime UPDATE for conversation: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Realtime subscription ended for conversation: ${e.message}")
        }
    }

    // ─── Mapping ───

    private fun mapDtoToMessage(dto: MessageDto): Message {
        return Message(
            id = dto.id,
            conversationId = dto.conversationId,
            matchId = dto.matchId,
            senderId = dto.senderId,
            senderTeamId = dto.senderTeamId,
            senderName = dto.senderName ?: "Unknown",
            content = dto.content,
            timestamp = DateUtils.parseIsoToMillis(dto.createdAt),
            isRead = dto.isRead,
            readAt = dto.readAt?.let { DateUtils.parseIsoToMillis(it) },
            type = MessageType.valueOf(dto.type),
            imageUrl = dto.imageUrl,
            voiceUrl = dto.voice_url,
            voiceDuration = dto.voiceDuration
        )
    }

    private fun mapDtoToConversation(dto: com.mlbb.scrim.data.service.ConversationDto): Conversation {
        return Conversation(
            id = dto.id,
            scrimId = dto.scrimId ?: "",
            scrimTitle = "", // Not always needed for list
            participantAId = dto.participantAId,
            participantAName = dto.participantAName,
            participantATeamId = dto.participantATeamId,
            participantATeamName = dto.participantATeamName,
            participantBId = dto.participantBId,
            participantBName = dto.participantBName,
            participantBTeamId = dto.participantBTeamId,
            participantBTeamName = dto.participantBTeamName,
            lastMessage = dto.lastMessage,
            lastMessageTime = DateUtils.parseIsoToMillis(dto.lastMessageTime),
            unreadCount = dto.unreadCount,
            chatOpensAt = DateUtils.parseIsoToMillis(dto.chatOpensAt),
            isParticipantATyping = dto.participantATyping,
            isParticipantBTyping = dto.participantBTyping
        )
    }

    /**
     * Parse a Realtime INSERT record (JsonObject) into a MessageDto.
     * Realtime payloads use snake_case column names matching the DB schema.
     */
    private fun parseRealtimeRecordToMessageDto(record: com.google.gson.JsonObject): MessageDto {
        return MessageDto(
            id = record.get("id")?.asString ?: "",
            conversationId = record.get("conversation_id")?.asString ?: "",
            matchId = record.get("match_id")?.asString,
            senderId = record.get("sender_id")?.asString ?: "",
            senderTeamId = record.get("sender_team_id")?.asString,
            senderName = record.get("sender_name")?.asString,
            content = record.get("content")?.asString ?: "",
            type = record.get("type")?.asString ?: "TEXT",
            createdAt = record.get("created_at")?.asString ?: "",
            isRead = record.get("is_read")?.asBoolean ?: false,
            readAt = record.get("read_at")?.asString,
            imageUrl = record.get("image_url")?.asString,
            voice_url = record.get("voice_url")?.asString,
            voiceDuration = record.get("voice_duration")?.asInt
        )
    }

    /**
     * Parse a Realtime UPDATE record (JsonObject) into a ConversationDto.
     * Realtime payloads use snake_case column names matching the DB schema.
     */
    private fun parseRealtimeRecordToConversationDto(record: com.google.gson.JsonObject): ConversationDto {
        return ConversationDto(
            id = record.get("id")?.asString ?: "",
            scrimId = record.get("scrim_id")?.asString,
            participantAId = record.get("participant_a_id")?.asString ?: "",
            participantAName = record.get("participant_a_name")?.asString ?: "",
            participantATeamId = record.get("participant_a_team_id")?.asString ?: "",
            participantATeamName = record.get("participant_a_team_name")?.asString ?: "",
            participantBId = record.get("participant_b_id")?.asString ?: "",
            participantBName = record.get("participant_b_name")?.asString ?: "",
            participantBTeamId = record.get("participant_b_team_id")?.asString ?: "",
            participantBTeamName = record.get("participant_b_team_name")?.asString ?: "",
            lastMessage = record.get("last_message")?.asString ?: "",
            lastMessageTime = record.get("last_message_time")?.asString ?: "",
            chatOpensAt = record.get("chat_opens_at")?.asString ?: "",
            participantATyping = record.get("participant_a_typing")?.asBoolean ?: false,
            participantBTyping = record.get("participant_b_typing")?.asBoolean ?: false
        )
    }

    // ─── Room Entity Mappers ───

    private fun mapConversationToEntity(conv: Conversation): com.mlbb.scrim.data.local.ConversationEntity {
        return com.mlbb.scrim.data.local.ConversationEntity(
            id = conv.id,
            scrimId = conv.scrimId,
            scrimTitle = conv.scrimTitle,
            participantAId = conv.participantAId,
            participantAName = conv.participantAName,
            participantATeamId = conv.participantATeamId,
            participantATeamName = conv.participantATeamName,
            participantBId = conv.participantBId,
            participantBName = conv.participantBName,
            participantBTeamId = conv.participantBTeamId,
            participantBTeamName = conv.participantBTeamName,
            lastMessage = conv.lastMessage,
            lastMessageTime = conv.lastMessageTime,
            chatOpensAt = conv.chatOpensAt,
            isParticipantATyping = conv.isParticipantATyping,
            isParticipantBTyping = conv.isParticipantBTyping
        )
    }

    private fun mapMessageToEntity(msg: Message): com.mlbb.scrim.data.local.MessageEntity {
        return com.mlbb.scrim.data.local.MessageEntity(
            id = msg.id,
            conversationId = msg.conversationId,
            matchId = msg.matchId,
            senderId = msg.senderId,
            senderTeamId = msg.senderTeamId,
            senderName = msg.senderName,
            content = msg.content,
            timestamp = msg.timestamp,
            isRead = msg.isRead,
            readAt = msg.readAt,
            type = msg.type.name,
            imageUrl = msg.imageUrl,
            voiceUrl = msg.voiceUrl,
            voiceDuration = msg.voiceDuration
        )
    }
}

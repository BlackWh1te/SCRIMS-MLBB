package com.mlbb.scrim.data.repository

import com.mlbb.scrim.data.model.Conversation
import com.mlbb.scrim.data.model.Message
import com.mlbb.scrim.data.model.MessageType
import com.mlbb.scrim.data.service.MessageDto
import com.mlbb.scrim.data.service.ConversationDto
import com.mlbb.scrim.data.service.SupabaseService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.text.SimpleDateFormat
import java.util.*

import com.mlbb.scrim.data.local.ConversationDao
import com.mlbb.scrim.data.local.MessageDao

class SupabaseMessageRepository(
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao
) : MessageRepositoryInterface {

    private val api = SupabaseService.api

    override suspend fun getConversationsForUser(userId: String): Flow<Result<List<Conversation>>> = flow {
        try {
            // Phase 4: Emit cached Room data first for instant UI
            try {
                val cached = conversationDao.getConversationsForUser(userId)
                cached.collect { entities ->
                    if (entities.isNotEmpty()) {
                        emit(Result.success(entities.map { it.toDomainModel() }))
                    }
                    return@collect // Only take the first emission
                }
            } catch (_: Exception) { /* Room empty, proceed to network */ }

            // Then fetch fresh from network
            val response = api.getConversations(orFilter = "participant_a_id.eq.$userId,participant_b_id.eq.$userId")
            if (response.isSuccessful) {
                val list = response.body()?.map { mapDtoToConversation(it) } ?: emptyList()
                val sorted = list.sortedByDescending { it.lastMessageTime }
                // Save to Room for offline access
                try {
                    conversationDao.insertConversations(sorted.map { mapConversationToEntity(it) })
                } catch (_: Exception) { }
                emit(Result.success(sorted))
            } else {
                emit(Result.failure(Exception("Failed to load conversations: ${response.code()}")))
            }
        } catch (e: Exception) {
            // Offline fallback: try Room
            try {
                conversationDao.getConversationsForUser(userId).collect { entities ->
                    if (entities.isNotEmpty()) {
                        emit(Result.success(entities.map { it.toDomainModel() }))
                    } else {
                        emit(Result.failure(e))
                    }
                    return@collect
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
            val existing = api.getConversations(orFilter = "scrim_id.eq.$scrimId")
            if (existing.isSuccessful && !existing.body().isNullOrEmpty()) {
                emit(Result.success(mapDtoToConversation(existing.body()!!.first())))
            } else {
                val newConv = ConversationDto(
                    scrimId = scrimId,
                    participantAId = participantAId,
                    participantAName = participantAName,
                    participantATeamName = participantATeamName,
                    participantBId = participantBId,
                    participantBName = participantBName,
                    participantBTeamName = participantBTeamName,
                    lastMessage = "",
                    lastMessageTime = java.time.Instant.now().toString(),
                    chatOpensAt = java.time.Instant.now().plusSeconds(300).toString()
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
                val chatOpensAt = conv?.chatOpensAt?.let { parseTimestamp(it) } ?: 0L
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
                    } catch (_: Exception) { }
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
            api.markConversationAsRead(mapOf("conversation_id" to conversationId, "user_id" to userId))
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
                    lastMessageTime = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(Date())
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
        // Phase 4: Cursor-based polling — only fetch NEW messages since last known
        var lastTimestamp = ""

        // Load existing messages from Room first for instant display
        try {
            messageDao.getMessagesForConversation(conversationId).collect { entities ->
                entities.forEach { entity ->
                    emit(entity.toDomainModel())
                }
                if (entities.isNotEmpty()) {
                    lastTimestamp = entities.last().let {
                        java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault())
                            .format(java.util.Date(it.timestamp))
                    }
                }
                return@collect // Only take first emission
            }
        } catch (_: Exception) { }

        // Then poll for new messages only
        var lastId = ""
        while (true) {
            delay(3000) // Poll every 3s
            try {
                // If we have a last timestamp, only fetch newer messages
                val response = if (lastTimestamp.isNotEmpty()) {
                    api.getMessages(
                        conversationId = conversationId,
                        createdAfter = "gt.$lastTimestamp"
                    )
                } else {
                    api.getMessages(conversationId = conversationId)
                }

                if (response.isSuccessful) {
                    val messages = response.body() ?: emptyList()
                    val newMessages = messages.filter { it.id > lastId }
                    if (newMessages.isNotEmpty()) {
                        // Save new messages to Room
                        try {
                            messageDao.insertMessages(newMessages.map { dto ->
                                mapMessageToEntity(mapDtoToMessage(dto))
                            })
                        } catch (_: Exception) { }

                        newMessages.forEach { msg ->
                            emit(mapDtoToMessage(msg))
                            lastId = msg.id
                        }
                        // Update cursor
                        lastTimestamp = newMessages.last().createdAt
                    }
                }
            } catch (_: Exception) {}
        }
    }

    override fun subscribeToConversation(conversationId: String): Flow<Conversation> = flow {
        while (true) {
            delay(5000)
            try {
                val response = api.getConversations(idFilter = "eq.$conversationId")
                if (response.isSuccessful) {
                    val dto = response.body()?.firstOrNull()
                    if (dto != null) emit(mapDtoToConversation(dto))
                }
            } catch (_: Exception) {}
        }
    }

    private fun mapDtoToMessage(dto: MessageDto): Message {
        return Message(
            id = dto.id,
            conversationId = dto.conversationId,
            senderId = dto.senderId,
            senderName = dto.senderName ?: "Unknown",
            content = dto.content,
            timestamp = parseTimestamp(dto.createdAt),
            isRead = dto.isRead,
            readAt = dto.readAt?.let { parseTimestamp(it) },
            type = MessageType.valueOf(dto.type),
            imageUrl = dto.imageUrl,
            voiceUrl = dto.voice_url,
            voiceDuration = dto.voiceDuration
        )
    }

    private fun mapDtoToConversation(dto: com.mlbb.scrim.data.service.ConversationDto): Conversation {
        return Conversation(
            id = dto.id,
            scrimId = dto.scrimId,
            scrimTitle = "", // Not always needed for list
            participantAId = dto.participantAId,
            participantAName = dto.participantAName,
            participantATeamName = dto.participantATeamName,
            participantBId = dto.participantBId,
            participantBName = dto.participantBName,
            participantBTeamName = dto.participantBTeamName,
            lastMessage = dto.lastMessage,
            lastMessageTime = parseTimestamp(dto.lastMessageTime),
            chatOpensAt = parseTimestamp(dto.chatOpensAt),
            isParticipantATyping = dto.participantATyping,
            isParticipantBTyping = dto.participantBTyping
        )
    }

    private fun parseTimestamp(iso: String): Long {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            sdf.parse(iso)?.time ?: System.currentTimeMillis()
        } catch (_: Exception) {
            System.currentTimeMillis()
        }
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
            senderId = msg.senderId,
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

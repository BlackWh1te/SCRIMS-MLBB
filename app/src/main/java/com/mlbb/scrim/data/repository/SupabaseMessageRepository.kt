package com.mlbb.scrim.data.repository

import timber.log.Timber
import com.mlbb.scrim.data.model.Conversation
import com.mlbb.scrim.data.model.Message
import com.mlbb.scrim.data.model.MessageType
import com.mlbb.scrim.data.service.MessageDto
import com.mlbb.scrim.data.service.ConversationDto
import com.mlbb.scrim.data.service.PostgrestFilter
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
import com.mlbb.scrim.data.cache.UnifiedCacheManager
import java.util.concurrent.ConcurrentHashMap

class SupabaseMessageRepository(
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao,
    private val realtimeClient: SupabaseRealtimeClient,
    private val cacheManager: UnifiedCacheManager
) : MessageRepositoryInterface {

    private val api = SupabaseService.api

    companion object {
        private const val TAG = "MessageRepo"
        private const val CACHE_KEY_CONVERSATIONS_PREFIX = "conversations_"
        private const val CACHE_KEY_CONVERSATION_PREFIX = "conversation_"
        private const val CONV_MEMORY_TTL = 2L * 60 * 1000   // 2 min (conversations change often)
        private const val CONV_ROOM_TTL = 10L * 60 * 1000     // 10 min
        private const val SINGLE_CONV_MEMORY_TTL = 5L * 60 * 1000  // 5 min
        private const val SINGLE_CONV_ROOM_TTL = 15L * 60 * 1000   // 15 min
    }

    // ── In-memory conversation lookup cache ──
    // Avoids redundant API calls for startDirectConversation, setTypingStatus, sendMessage gate
    // HARDENED: Bounded LRU cache (max 20 entries) to prevent OOM on heavy chat usage
    private data class CachedConversation(
        val conversation: Conversation,
        val cachedAt: Long,
        var lastMessageFetch: Long = 0L
    ) {
        fun isValid(): Boolean = (System.currentTimeMillis() - cachedAt) < CONV_MEMORY_TTL
        fun areMessagesFresh(): Boolean = (System.currentTimeMillis() - lastMessageFetch) < 60_000
    }
    private val conversationLookupCache = object : LinkedHashMap<String, CachedConversation>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, CachedConversation>?): Boolean {
            return size > 20
        }
    }

    private fun cacheConversation(conv: Conversation) {
        conversationLookupCache[conv.id] = CachedConversation(conv, System.currentTimeMillis())
    }

    private fun getCachedConversation(conversationId: String): Conversation? {
        val cached = conversationLookupCache[conversationId]
        return if (cached != null && cached.isValid()) cached.conversation else null
    }

    private fun invalidateConversationCache(conversationId: String) {
        conversationLookupCache.remove(conversationId)
    }

    override suspend fun getConversationsForUser(userId: String): Flow<Result<List<Conversation>>> = flow {
        try {
            val cacheKey = "${CACHE_KEY_CONVERSATIONS_PREFIX}$userId"
            cacheManager.getFlow<List<Conversation>>(
                key = cacheKey,
                memoryTtlMs = CONV_MEMORY_TTL,
                roomTtlMs = CONV_ROOM_TTL,
                roomLoader = {
                    val cached = conversationDao.getConversationsForUser(userId).first()
                    if (cached.isNotEmpty()) cached.map { it.toDomainModel() } else null
                },
                networkLoader = {
                    val response = api.getConversationsForUserRpc(mapOf("p_user_id" to userId))
                    if (response.isSuccessful) {
                        response.body()?.map { mapDtoToConversation(it) } ?: emptyList()
                    } else {
                        val errorBody = response.errorBody()?.string()
                        Timber.e(TAG, "Failed to load conversations: ${response.code()} body=$errorBody")
                        throw Exception("Failed to load conversations: ${response.code()}")
                    }
                },
                roomSaver = { conversations ->
                    conversationDao.insertConversations(conversations.map { mapConversationToEntity(it) })
                    // Also populate lookup cache
                    conversations.forEach { cacheConversation(it) }
                }
            ).collect { conversations ->
                emit(Result.success(conversations))
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
            // L1: Check in-memory lookup cache first
            val cachedEntry = conversationLookupCache[conversationId]
            if (cachedEntry != null && cachedEntry.isValid()) {
                // Try Room messages first for instant display, then refresh from network
                val roomMessages = try {
                    messageDao.getMessagesForConversation(conversationId).first().map { it.toDomainModel() }
                } catch (_: Exception) { emptyList() }

                if (roomMessages.isNotEmpty() && cachedEntry.areMessagesFresh()) {
                    emit(Result.success(cachedEntry.conversation.copy(messages = roomMessages)))
                    return@flow
                }

                // HARDENED: Don't emit stale Room data when about to fetch fresh network data.
                // This prevents UI flicker from double emits.
                // Then fetch fresh from network
                val messagesResponse = api.getMessages(conversationId = PostgrestFilter.eq(conversationId))
                val messages = if (messagesResponse.isSuccessful) {
                    cachedEntry.lastMessageFetch = System.currentTimeMillis()
                    messagesResponse.body()?.map { mapDtoToMessage(it) } ?: emptyList()
                } else emptyList()
                // Persist network messages to Room for next startup
                if (messages.isNotEmpty()) {
                    try { messageDao.insertMessages(messages.map { mapMessageToEntity(it) }) } catch (_: Exception) {}
                }
                emit(Result.success(cachedEntry.conversation.copy(messages = messages)))
                return@flow
            }

            // L2: Check Room
            try {
                val roomConv = conversationDao.getConversationById(conversationId).first()
                if (roomConv != null) {
                    val domainConv = roomConv.toDomainModel()
                    val newCacheEntry = CachedConversation(domainConv, System.currentTimeMillis())
                    conversationLookupCache[conversationId] = newCacheEntry
                    // Load messages from Room first for instant display
                    val roomMessages = try {
                        messageDao.getMessagesForConversation(conversationId).first().map { it.toDomainModel() }
                    } catch (_: Exception) { emptyList() }

                    // HARDENED: Only emit Room data if fresh (< 60s); otherwise skip to network single emit
                    val roomMessagesFresh = roomMessages.isNotEmpty() &&
                        (System.currentTimeMillis() - (roomMessages.lastOrNull()?.timestamp ?: 0L)) < 60_000
                    if (roomMessagesFresh) {
                        emit(Result.success(domainConv.copy(messages = roomMessages)))
                        return@flow
                    }

                    // Then refresh from network
                    val messagesResponse = api.getMessages(conversationId = PostgrestFilter.eq(conversationId))
                    val messages = if (messagesResponse.isSuccessful) {
                        newCacheEntry.lastMessageFetch = System.currentTimeMillis()
                        messagesResponse.body()?.map { mapDtoToMessage(it) } ?: emptyList()
                    } else emptyList()
                    if (messages.isNotEmpty()) {
                        try { messageDao.insertMessages(messages.map { mapMessageToEntity(it) }) } catch (_: Exception) {}
                    }
                    emit(Result.success(domainConv.copy(messages = messages)))
                    return@flow
                }
            } catch (e: Exception) { Timber.w(TAG, "Room lookup failed for getConversationById", e) }

            // L3: Network
            val response = api.getConversations(idFilter = "eq.$conversationId")
            if (response.isSuccessful) {
                val dto = response.body()?.firstOrNull()
                if (dto != null) {
                    val conv = mapDtoToConversation(dto)
                    cacheConversation(conv)
                    val messagesResponse = api.getMessages(conversationId = PostgrestFilter.eq(conversationId))
                    val messages = if (messagesResponse.isSuccessful) {
                        val cachedEntry = conversationLookupCache[conversationId]
                        if (cachedEntry != null) cachedEntry.lastMessageFetch = System.currentTimeMillis()
                        messagesResponse.body()?.map { mapDtoToMessage(it) } ?: emptyList()
                    } else emptyList()
                    // Persist to Room
                    try {
                        conversationDao.insertConversation(mapConversationToEntity(conv))
                        if (messages.isNotEmpty()) {
                            messageDao.insertMessages(messages.map { mapMessageToEntity(it) })
                        }
                    } catch (_: Exception) {}
                    emit(Result.success(conv.copy(messages = messages)))
                } else {
                    emit(Result.success(null))
                }
            } else {
                val errorBody = response.errorBody()?.string()
                Timber.e(TAG, "Failed to load conversation: ${response.code()} body=$errorBody")
                emit(Result.failure(Exception("Failed to load conversation: ${response.code()}")))
            }
        } catch (e: Exception) {
            // Offline fallback: try Room for conversation + messages
            try {
                val roomConv = conversationDao.getConversationById(conversationId).first()
                if (roomConv != null) {
                    val roomMessages = messageDao.getMessagesForConversation(conversationId).first().map { it.toDomainModel() }
                    emit(Result.success(roomConv.toDomainModel().copy(messages = roomMessages)))
                } else {
                    emit(Result.failure(e))
                }
            } catch (_: Exception) {
                emit(Result.failure(e))
            }
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
            // Check lookup cache for existing conversation with these participants + scrim
            val cachedMatch = conversationLookupCache.values.find { entry ->
                entry.isValid() && entry.conversation.scrimId == scrimId &&
                ((entry.conversation.participantAId == participantAId && entry.conversation.participantBId == participantBId) ||
                 (entry.conversation.participantAId == participantBId && entry.conversation.participantBId == participantAId))
            }
            if (cachedMatch != null) {
                Timber.d(TAG, "getOrCreateConversation: cache HIT for scrim $scrimId")
                emit(Result.success(cachedMatch.conversation))
                return@flow
            }

            // Search for existing conversation with SAME scrim_id AND SAME participants
            // Query direction 1: A=sender, B=recipient
            val existing1 = api.getConversations(
                scrimId = PostgrestFilter.eq(scrimId),
                participantAId = PostgrestFilter.eq(participantAId),
                participantBId = PostgrestFilter.eq(participantBId)
            )
            if (existing1.isSuccessful && !existing1.body().isNullOrEmpty()) {
                val conv = mapDtoToConversation(existing1.body()!!.first())
                cacheConversation(conv)
                emit(Result.success(conv))
                return@flow
            }
            // Query direction 2: A=recipient, B=sender
            val existing2 = api.getConversations(
                scrimId = PostgrestFilter.eq(scrimId),
                participantAId = PostgrestFilter.eq(participantBId),
                participantBId = PostgrestFilter.eq(participantAId)
            )
            if (existing2.isSuccessful && !existing2.body().isNullOrEmpty()) {
                val conv = mapDtoToConversation(existing2.body()!!.first())
                cacheConversation(conv)
                emit(Result.success(conv))
                return@flow
            }

            // Create new conversation (HARDENED: let DB auto-generate id)
            val newConvBody = mapOf(
                "scrim_id" to scrimId,
                "participant_a_id" to participantAId,
                "participant_a_name" to participantAName,
                "participant_a_team_id" to participantATeamId,
                "participant_a_team_name" to participantATeamName,
                "participant_b_id" to participantBId,
                "participant_b_name" to participantBName,
                "participant_b_team_id" to participantBTeamId,
                "participant_b_team_name" to participantBTeamName,
                "last_message" to "",
                "last_message_time" to DateUtils.formatIsoUtc(System.currentTimeMillis()),
                "chat_opens_at" to DateUtils.formatIsoUtc(System.currentTimeMillis() + 300_000)
            )
            val response = api.createConversation(newConvBody)
            if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                val created = mapDtoToConversation(response.body()!!.first())
                cacheConversation(created)
                conversationDao.insertConversation(mapConversationToEntity(created))
                // Invalidate list cache since we added a new conversation
                cacheManager.invalidateByPrefix(CACHE_KEY_CONVERSATIONS_PREFIX)
                emit(Result.success(created))
            } else {
                emit(Result.failure(Exception("Failed to create conversation: ${response.code()}")))
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
            // Use cached conversation instead of fetching from API
            val cachedConv = getCachedConversation(conversationId)
            if (cachedConv != null) {
                val chatOpensAt = cachedConv.chatOpensAt
                if (chatOpensAt > 0L && System.currentTimeMillis() < chatOpensAt) {
                    val secondsRemaining = (chatOpensAt - System.currentTimeMillis()) / 1000
                    emit(Result.failure(Exception("Chat is locked. Opens in ${secondsRemaining}s")))
                    return@flow
                }
            } else {
                // Fallback: fetch from API only if not cached
                val convResponse = api.getConversations(idFilter = "eq.$conversationId")
                if (convResponse.isSuccessful) {
                    val conv = convResponse.body()?.firstOrNull()
                    val chatOpensAt = DateUtils.parseIsoToMillis(conv?.chatOpensAt, fallback = 0L)
                    if (System.currentTimeMillis() < chatOpensAt) {
                        val secondsRemaining = (chatOpensAt - System.currentTimeMillis()) / 1000
                        emit(Result.failure(Exception("Chat is locked. Opens in ${secondsRemaining}s")))
                        return@flow
                    }
                    // Cache for next time
                    if (conv != null) cacheConversation(mapDtoToConversation(conv))
                }
            }

            // HARDENED: Content validation before sending
            if (content.isBlank() && imageUrl.isNullOrBlank() && voiceUrl.isNullOrBlank()) {
                emit(Result.failure(Exception("Message cannot be empty")))
                return@flow
            }
            if (content.length > 2000) {
                emit(Result.failure(Exception("Message too long (max 2000 characters)")))
                return@flow
            }
            val sanitized = content.replace(Regex("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]"), "")

            val dto = MessageDto(
                conversationId = conversationId,
                senderId = senderId,
                senderName = senderName,
                content = sanitized,
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
                    } catch (e: Exception) { Timber.w(TAG, "Failed to persist sent message to Room", e) }
                    // Also PATCH server-side conversation metadata (best-effort; DB trigger is primary)
                    try {
                        api.updateConversation(
                            conversationId,
                            mapOf(
                                "last_message" to content,
                                "last_message_time" to DateUtils.formatIsoUtc(message.timestamp)
                            )
                        )
                    } catch (e: Exception) { Timber.w(TAG, "Failed to update conversation last_message on server", e) }
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
            // HARDENED: Also update Room so messages don't re-appear as unread after app restart
            try {
                messageDao.markMessagesAsRead(conversationId, userId, System.currentTimeMillis())
            } catch (e: Exception) { Timber.w(TAG, "Failed to mark messages as read in Room", e) }
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
            // Use cached conversation to determine A vs B instead of API call
            val cachedConv = getCachedConversation(conversationId)
            if (cachedConv != null) {
                val field = if (userId == cachedConv.participantAId) "participant_a_typing" else "participant_b_typing"
                api.updateConversation(conversationId, mapOf(field to isTyping))
                emit(Result.success(Unit))
            } else {
                // Fallback: fetch from API only if not cached
                val response = api.getConversations(idFilter = "eq.$conversationId")
                if (response.isSuccessful) {
                    val conv = response.body()?.firstOrNull()
                    if (conv != null) {
                        val domainConv = mapDtoToConversation(conv)
                        cacheConversation(domainConv)
                        val field = if (userId == domainConv.participantAId) "participant_a_typing" else "participant_b_typing"
                        api.updateConversation(conversationId, mapOf(field to isTyping))
                        emit(Result.success(Unit))
                    }
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
            Timber.d("MessageFlow", "Repo: startDirectConversation sender=$senderId recipient=$recipientId")

            // L1: Check in-memory lookup cache for existing direct conversation
            val cachedMatch = conversationLookupCache.values.find { entry ->
                entry.isValid() && entry.conversation.scrimId.isEmpty() &&
                ((entry.conversation.participantAId == senderId && entry.conversation.participantBId == recipientId) ||
                 (entry.conversation.participantAId == recipientId && entry.conversation.participantBId == senderId))
            }
            if (cachedMatch != null) {
                Timber.d("MessageFlow", "Repo: cache HIT for direct conversation")
                emit(Result.success(cachedMatch.conversation))
                return@flow
            }

            // L2: Check Room for existing direct conversation
            try {
                val roomConvs = conversationDao.getConversationsForUser(senderId).first()
                val existing = roomConvs.find { entity ->
                    entity.scrimId.isNullOrEmpty() &&
                    ((entity.participantAId == senderId && entity.participantBId == recipientId) ||
                     (entity.participantAId == recipientId && entity.participantBId == senderId))
                }
                if (existing != null) {
                    val domainConv = existing.toDomainModel()
                    cacheConversation(domainConv)
                    Timber.d("MessageFlow", "Repo: Room HIT for direct conversation")
                    emit(Result.success(domainConv))
                    return@flow
                }
            } catch (e: Exception) { Timber.w(TAG, "Room lookup failed in startDirectConversation", e) }

            // L3: Check for existing direct conversation via API — query both directions separately
            // Direction 1: A=sender, B=recipient
            val existing1 = api.getConversations(
                participantAId = PostgrestFilter.eq(senderId),
                participantBId = PostgrestFilter.eq(recipientId)
            )
            Timber.d("MessageFlow", "Repo: dir1 code=${existing1.code()} size=${existing1.body()?.size}")
            if (existing1.isSuccessful && !existing1.body().isNullOrEmpty()) {
                Timber.d("MessageFlow", "Repo: found existing conversation (dir1)")
                val conv = mapDtoToConversation(existing1.body()!!.first())
                cacheConversation(conv)
                emit(Result.success(conv))
                return@flow
            }
            // Direction 2: A=recipient, B=sender
            val existing2 = api.getConversations(
                participantAId = PostgrestFilter.eq(recipientId),
                participantBId = PostgrestFilter.eq(senderId)
            )
            Timber.d("MessageFlow", "Repo: dir2 code=${existing2.code()} size=${existing2.body()?.size}")
            if (existing2.isSuccessful && !existing2.body().isNullOrEmpty()) {
                Timber.d("MessageFlow", "Repo: found existing conversation (dir2)")
                val conv = mapDtoToConversation(existing2.body()!!.first())
                cacheConversation(conv)
                emit(Result.success(conv))
                return@flow
            }

            // Create new direct conversation (HARDENED: let DB auto-generate id)
            Timber.d("MessageFlow", "Repo: no existing, creating new")
            val newConvBody = mapOf(
                "participant_a_id" to senderId,
                "participant_a_name" to senderName,
                "participant_b_id" to recipientId,
                "participant_b_name" to recipientName,
                "last_message" to "Conversation started",
                "last_message_time" to DateUtils.formatIsoUtc(System.currentTimeMillis())
            )
            val createResponse = api.createConversation(newConvBody)
            Timber.d("MessageFlow", "Repo: create code=${createResponse.code()} isSuccessful=${createResponse.isSuccessful}")
            if (createResponse.isSuccessful) {
                val body = createResponse.body()
                Timber.d("MessageFlow", "Repo: create bodySize=${body?.size}")
                if (!body.isNullOrEmpty()) {
                    val created = mapDtoToConversation(body.first())
                    cacheConversation(created)
                    conversationDao.insertConversation(mapConversationToEntity(created))
                    // Invalidate list cache since we added a new conversation
                    cacheManager.invalidateByPrefix(CACHE_KEY_CONVERSATIONS_PREFIX)
                    emit(Result.success(created))
                } else {
                    emit(Result.failure(Exception("Created conversation returned empty body")))
                }
            } else {
                emit(Result.failure(Exception("Failed to create direct conversation: ${createResponse.code()}")))
            }
        } catch (e: Exception) {
            Timber.d("MessageFlow", "Repo: exception ${e.javaClass.simpleName}: ${e.message}")
            emit(Result.failure(e))
        }
    }

    override fun subscribeToMessages(conversationId: String): Flow<Message> = flow {
        // ── Phase 1: Emit cached Room messages for instant display ──
        val cachedIds = mutableSetOf<String>()
        try {
            val cached = messageDao.getMessagesForConversation(conversationId).first()
            cached.forEach { entity ->
                cachedIds.add(entity.id)
                emit(entity.toDomainModel())
            }
        } catch (e: Exception) { Timber.w(TAG, "Failed to load cached messages", e) }

        // ── Phase 2: Bridge fetch — get any messages added between cache and Realtime start ──
        try {
            val latestResponse = api.getMessages(
                conversationId = PostgrestFilter.eq(conversationId),
                order = "created_at.asc"
            )
            if (latestResponse.isSuccessful) {
                val serverMessages = latestResponse.body()?.map { mapDtoToMessage(it) } ?: emptyList()
                serverMessages.forEach { msg ->
                    if (msg.id !in cachedIds) {
                        cachedIds.add(msg.id)
                        emit(msg)
                    }
                }
                // Persist all server messages to Room for next startup
                try {
                    messageDao.insertMessages(serverMessages.map { mapMessageToEntity(it) })
                } catch (_: Exception) {}
            }
        } catch (e: Exception) { Timber.w(TAG, "Bridge fetch failed for messages", e) }

        // ── Phase 3: Supabase Realtime (WebSocket) for live updates ──
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
                        if (message.id !in cachedIds) {
                            cachedIds.add(message.id)
                            try {
                                messageDao.insertMessage(mapMessageToEntity(message))
                            } catch (e: Exception) { Timber.w(TAG, "Failed to persist realtime message to Room", e) }
                            emit(message)
                        }
                    }
                } catch (e: Exception) {
                    Timber.w(TAG, "Failed to parse Realtime INSERT: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Timber.w(TAG, "Realtime subscription ended for messages: ${e.message}")
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
                    Timber.w(TAG, "Failed to parse Realtime UPDATE for conversation: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Timber.w(TAG, "Realtime subscription ended for conversation: ${e.message}")
        }
    }

    // ─── Mapping ───

    private fun mapDtoToMessage(dto: MessageDto): Message {
        return Message(
            id = dto.id ?: "",
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
            scrimTitle = "",
            participantAId = dto.participantAId ?: "",
            participantAName = dto.participantAName ?: "",
            participantATeamId = dto.participantATeamId ?: "",
            participantATeamName = dto.participantATeamName ?: "",
            participantBId = dto.participantBId ?: "",
            participantBName = dto.participantBName ?: "",
            participantBTeamId = dto.participantBTeamId ?: "",
            participantBTeamName = dto.participantBTeamName ?: "",
            lastMessage = dto.lastMessage ?: "",
            lastMessageTime = DateUtils.parseIsoToMillis(dto.lastMessageTime),
            unreadCount = dto.unreadCount ?: 0,
            chatOpensAt = DateUtils.parseIsoToMillis(dto.chatOpensAt),
            isParticipantATyping = dto.participantATyping ?: false,
            isParticipantBTyping = dto.participantBTyping ?: false,
            // ── Tournament match chat ──
            tournamentMatchId = dto.tournamentMatchId,
            participantCount = dto.participantCount,
            isGroupChat = dto.tournamentMatchId != null || dto.participantCount > 2
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
            tournamentMatchId = record.get("tournament_match_id")?.asString,
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
            participantBTyping = record.get("participant_b_typing")?.asBoolean ?: false,
            participantCount = record.get("participant_count")?.asInt ?: 2
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
            isParticipantBTyping = conv.isParticipantBTyping,
            // ── Tournament match chat ──
            tournamentMatchId = conv.tournamentMatchId,
            participantCount = conv.participantCount,
            isGroupChat = conv.isGroupChat,
            unreadCount = conv.unreadCount
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

package com.scrimslegends.app.data.repository

import timber.log.Timber
import com.scrimslegends.app.data.model.Conversation
import com.scrimslegends.app.data.model.DeliveryStatus
import com.scrimslegends.app.data.model.Message
import com.scrimslegends.app.data.model.MessageType
import com.scrimslegends.app.data.model.MessageWithDelivery
import com.scrimslegends.app.data.service.ChatConnectionState
import com.scrimslegends.app.data.service.MessageDto
import com.scrimslegends.app.data.service.ConversationDto
import com.scrimslegends.app.data.service.PostgrestFilter
import com.scrimslegends.app.data.service.SupabaseConfig
import com.scrimslegends.app.data.service.SupabaseRealtimeClient
import com.scrimslegends.app.data.service.SupabaseService
import com.scrimslegends.app.util.DateUtils
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull

import java.util.*

import com.scrimslegends.app.data.local.ConversationDao
import com.scrimslegends.app.data.local.ConversationEntity
import com.scrimslegends.app.data.local.MessageDao
import com.scrimslegends.app.data.local.MessageEntity
import com.scrimslegends.app.data.local.PendingMessageDao
import com.scrimslegends.app.data.local.PendingMessageEntity
import com.scrimslegends.app.data.cache.UnifiedCacheManager

class SupabaseMessageRepository(
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao,
    private val pendingMessageDao: PendingMessageDao,
    private val realtimeClient: SupabaseRealtimeClient,
    private val cacheManager: UnifiedCacheManager
) : MessageRepositoryInterface {

    private val api = SupabaseService.api

    companion object {
        private const val TAG = "MessageRepo"
        private const val CACHE_KEY_CONVERSATIONS_PREFIX = "conversations_"
        private const val CACHE_KEY_CONVERSATION_PREFIX = "conversation_"
        private const val CONV_MEMORY_TTL = 2L * 60 * 1000
        private const val CONV_ROOM_TTL = 10L * 60 * 1000
        private const val SINGLE_CONV_MEMORY_TTL = 5L * 60 * 1000
        private const val SINGLE_CONV_ROOM_TTL = 15L * 60 * 1000
        private const val MAX_RETRY_COUNT = 5
        private const val BASE_RETRY_DELAY_MS = 1000L
        private const val MAX_MESSAGE_LENGTH = 2000
    }

    // ── Repository-scoped coroutine scope ──
    // Using a named scope instead of GlobalScope so the internal bridge coroutine
    // is tied to the repository's lifecycle and can be cancelled on cleanup.
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // ── Concurrency guards ──
    // Per-conversation send locks — prevents cross-conversation serialization
    private val sendLocks = mutableMapOf<String, Mutex>()

    private fun getSendMutex(conversationId: String): Mutex {
        return synchronized(sendLocks) {
            sendLocks.getOrPut(conversationId) { Mutex() }
        }
    }
    private val cacheMutex = Mutex()
    private val activeSubscriptions = Collections.synchronizedSet(HashSet<String>())

    // ── In-memory conversation lookup cache ──
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

    // ── Cache metrics (lightweight) ──
    private var cacheHits = 0L
    private var cacheMisses = 0L

    private fun recordCacheHit() { cacheHits++ }
    private fun recordCacheMiss() { cacheMisses++ }

    // ── Connection state bridge ──
    private val _connectionState = MutableStateFlow(ChatConnectionState.DISCONNECTED)
    override fun observeConnectionState(): Flow<ChatConnectionState> = _connectionState.asStateFlow()

    init {
        // Bridge realtime client internal state to our domain state.
        // Uses repositoryScope (not GlobalScope) so this coroutine is properly
        // cancelled when the repository is no longer needed.
        repositoryScope.launch {
            realtimeClient.connectionState.collect { internalState ->
                _connectionState.value = when (internalState) {
                    SupabaseRealtimeClient.ConnectionState.CONNECTED -> ChatConnectionState.CONNECTED
                    SupabaseRealtimeClient.ConnectionState.CONNECTING -> ChatConnectionState.CONNECTING
                    SupabaseRealtimeClient.ConnectionState.RECONNECTING -> ChatConnectionState.RECONNECTING
                    SupabaseRealtimeClient.ConnectionState.DISCONNECTED -> {
                        if (activeSubscriptions.isNotEmpty()) ChatConnectionState.FALLBACK_POLLING
                        else ChatConnectionState.DISCONNECTED
                    }
                }
            }
        }
    }

    // ── Cache helpers (Mutex-protected) ──
    private suspend fun cacheConversation(conv: Conversation) {
        cacheMutex.withLock {
            conversationLookupCache[conv.id] = CachedConversation(conv, System.currentTimeMillis())
        }
    }

    private suspend fun getCachedConversation(conversationId: String): Conversation? {
        return cacheMutex.withLock {
            val cached = conversationLookupCache[conversationId]
            if (cached != null && cached.isValid()) {
                recordCacheHit()
                cached.conversation
            } else {
                recordCacheMiss()
                null
            }
        }
    }

    private suspend fun invalidateConversationCache(conversationId: String) {
        cacheMutex.withLock { conversationLookupCache.remove(conversationId) }
    }

    // ── Conversation list ──
    override suspend fun getConversationsForUser(userId: String, forceRefresh: Boolean): Flow<Result<List<Conversation>>> = flow {
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
                    conversations.forEach { cacheConversation(it) }
                },
                forceRefresh = forceRefresh
            ).collect { conversations ->
                emit(Result.success(conversations))
            }
        } catch (e: Exception) {
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

    // ── Single conversation (deduplicated, no double emit) ──
    override suspend fun getConversationById(conversationId: String): Flow<Result<Conversation?>> = flow {
        try {
            val cachedEntry = cacheMutex.withLock { conversationLookupCache[conversationId] }
            if (cachedEntry != null && cachedEntry.isValid()) {
                val roomMessages = try {
                    messageDao.getMessagesForConversation(conversationId).first().map { it.toDomainModel() }
                } catch (_: Exception) { emptyList() }

                if (roomMessages.isNotEmpty() && cachedEntry.areMessagesFresh()) {
                    emit(Result.success(cachedEntry.conversation.copy(messages = roomMessages)))
                    return@flow
                }

                val messagesResponse = api.getMessages(conversationId = PostgrestFilter.eq(conversationId))
                val messages = if (messagesResponse.isSuccessful) {
                    cachedEntry.lastMessageFetch = System.currentTimeMillis()
                    messagesResponse.body()?.map { mapDtoToMessage(it) } ?: emptyList()
                } else emptyList()
                if (messages.isNotEmpty()) {
                    try { messageDao.insertMessages(messages.map { mapMessageToEntity(it) }) } catch (_: Exception) {}
                }
                emit(Result.success(cachedEntry.conversation.copy(messages = messages)))
                return@flow
            }

            try {
                val roomConv = conversationDao.getConversationById(conversationId).first()
                if (roomConv != null) {
                    val domainConv = roomConv.toDomainModel()
                    val newCacheEntry = CachedConversation(domainConv, System.currentTimeMillis())
                    cacheMutex.withLock { conversationLookupCache[conversationId] = newCacheEntry }
                    val roomMessages = try {
                        messageDao.getMessagesForConversation(conversationId).first().map { it.toDomainModel() }
                    } catch (_: Exception) { emptyList() }

                    val roomMessagesFresh = roomMessages.isNotEmpty() &&
                        (System.currentTimeMillis() - (roomMessages.lastOrNull()?.timestamp ?: 0L)) < 60_000
                    if (roomMessagesFresh) {
                        emit(Result.success(domainConv.copy(messages = roomMessages)))
                        return@flow
                    }

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

            val response = api.getConversations(idFilter = "eq.$conversationId")
            if (response.isSuccessful) {
                val dto = response.body()?.firstOrNull()
                if (dto != null) {
                    val conv = mapDtoToConversation(dto)
                    cacheConversation(conv)
                    val messagesResponse = api.getMessages(conversationId = PostgrestFilter.eq(conversationId))
                    val messages = if (messagesResponse.isSuccessful) {
                        val entry = cacheMutex.withLock { conversationLookupCache[conversationId] }
                        if (entry != null) entry.lastMessageFetch = System.currentTimeMillis()
                        messagesResponse.body()?.map { mapDtoToMessage(it) } ?: emptyList()
                    } else emptyList()
                    try {
                        conversationDao.insertConversation(mapConversationToEntity(conv))
                        if (messages.isNotEmpty()) messageDao.insertMessages(messages.map { mapMessageToEntity(it) })
                    } catch (_: Exception) {}
                    emit(Result.success(conv.copy(messages = messages)))
                } else {
                    emit(Result.success(null))
                }
            } else {
                emit(Result.failure(Exception("Failed to load conversation: ${response.code()}")))
            }
        } catch (e: Exception) {
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

    // ── Create scrim conversation ──
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
            Timber.d("getOrCreateConversation: scrim=$scrimId a=$participantAId b=$participantBId")
            val cachedMatch = cacheMutex.withLock {
                conversationLookupCache.values.find { entry ->
                    entry.isValid() && entry.conversation.scrimId == scrimId &&
                    ((entry.conversation.participantAId == participantAId && entry.conversation.participantBId == participantBId) ||
                     (entry.conversation.participantAId == participantBId && entry.conversation.participantBId == participantAId))
                }
            }
            if (cachedMatch != null) {
                Timber.d("getOrCreateConversation: found cached ${cachedMatch.conversation.id}")
                emit(Result.success(cachedMatch.conversation))
                return@flow
            }

            Timber.d("getOrCreateConversation: checking DB for existing conversation")
            val existing1 = api.getConversations(
                scrimId = PostgrestFilter.eq(scrimId),
                participantAId = PostgrestFilter.eq(participantAId),
                participantBId = PostgrestFilter.eq(participantBId)
            )
            if (existing1.isSuccessful && !existing1.body().isNullOrEmpty()) {
                Timber.d("getOrCreateConversation: found existing1 ${existing1.body()!!.first().id}")
                val conv = mapDtoToConversation(existing1.body()!!.first())
                cacheConversation(conv)
                emit(Result.success(conv))
                return@flow
            } else if (!existing1.isSuccessful) {
                Timber.e("getOrCreateConversation: existing1 query failed ${existing1.code()} ${existing1.errorBody()?.string()}")
            }
            val existing2 = api.getConversations(
                scrimId = PostgrestFilter.eq(scrimId),
                participantAId = PostgrestFilter.eq(participantBId),
                participantBId = PostgrestFilter.eq(participantAId)
            )
            if (existing2.isSuccessful && !existing2.body().isNullOrEmpty()) {
                Timber.d("getOrCreateConversation: found existing2 ${existing2.body()!!.first().id}")
                val conv = mapDtoToConversation(existing2.body()!!.first())
                cacheConversation(conv)
                emit(Result.success(conv))
                return@flow
            } else if (!existing2.isSuccessful) {
                Timber.e("getOrCreateConversation: existing2 query failed ${existing2.code()} ${existing2.errorBody()?.string()}")
            }

            Timber.d("getOrCreateConversation: creating new conversation")
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
                "chat_opens_at" to DateUtils.formatIsoUtc(System.currentTimeMillis())
            )
            val response = api.createConversation(newConvBody)
            if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                val created = mapDtoToConversation(response.body()!!.first())
                cacheConversation(created)
                conversationDao.insertConversation(mapConversationToEntity(created))
                cacheManager.invalidateByPrefix(CACHE_KEY_CONVERSATIONS_PREFIX)
                Timber.d("getOrCreateConversation: created new conversation ${created.id}")
                emit(Result.success(created))
            } else {
                val err = "Failed to create conversation: ${response.code()} ${response.errorBody()?.string()}"
                Timber.e("getOrCreateConversation: $err")
                emit(Result.failure(Exception(err)))
            }
        } catch (e: Exception) {
            Timber.e("getOrCreateConversation: exception", e)
            emit(Result.failure(e))
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // IDEMPOTENT MESSAGE SEND (production-grade)
    // ═══════════════════════════════════════════════════════════════════════

    override suspend fun sendMessage(
        conversationId: String,
        senderId: String,
        senderName: String,
        content: String,
        type: MessageType,
        clientMessageId: String,
        imageUrl: String?,
        voiceUrl: String?,
        voiceDuration: Int?,
        replyToId: String?,
        replyToSnippet: String?,
        replyToSenderName: String?
    ): Flow<MessageWithDelivery> = flow {
        // 1. Persist to outbox immediately (survives process death)
        val pending = PendingMessageEntity(
            clientMessageId = clientMessageId,
            conversationId = conversationId,
            senderId = senderId,
            senderName = senderName,
            content = content,
            type = type.name,
            imageUrl = imageUrl,
            voiceUrl = voiceUrl,
            voiceDuration = voiceDuration,
            replyToId = replyToId,
            replyToSnippet = replyToSnippet,
            replyToSenderName = replyToSenderName,
            status = DeliveryStatus.PENDING.name
        )
        pendingMessageDao.insert(pending)
        emit(pending.toDomainModel())

        // 2. Attempt network delivery
        val result = sendMessageInternal(pending, replyToId, replyToSnippet, replyToSenderName)
        emit(result)
    }

    private suspend fun sendMessageInternal(
        pending: PendingMessageEntity,
        replyToId: String? = null,
        replyToSnippet: String? = null,
        replyToSenderName: String? = null
    ): MessageWithDelivery {
        return getSendMutex(pending.conversationId).withLock {
            // Idempotency: if already sent (e.g., previous attempt succeeded but client crashed),
            // return the existing success without re-sending.
            val existing = pendingMessageDao.getByClientId(pending.clientMessageId)
            if (existing != null && DeliveryStatus.valueOf(existing.status) == DeliveryStatus.SENT) {
                return@withLock existing.toDomainModel()
            }

            pendingMessageDao.updateStatus(pending.clientMessageId, DeliveryStatus.SENDING.name)

            try {
                // Chat gate
                val cachedConv = getCachedConversation(pending.conversationId)
                if (cachedConv != null) {
                    if (cachedConv.chatOpensAt > 0L && System.currentTimeMillis() < cachedConv.chatOpensAt) {
                        val reason = "Chat is locked"
                        pendingMessageDao.markFailed(pending.clientMessageId, reason)
                        return@withLock pending.copy(status = DeliveryStatus.FAILED.name, errorReason = reason).toDomainModel()
                    }
                }

                // Content validation
                if (pending.content.isBlank() && pending.imageUrl.isNullOrBlank() && pending.voiceUrl.isNullOrBlank()) {
                    val reason = "Message cannot be empty"
                    pendingMessageDao.markFailed(pending.clientMessageId, reason)
                    return@withLock pending.copy(status = DeliveryStatus.FAILED.name, errorReason = reason).toDomainModel()
                }
                if (pending.content.length > MAX_MESSAGE_LENGTH) {
                    val reason = "Message too long (max $MAX_MESSAGE_LENGTH)"
                    pendingMessageDao.markFailed(pending.clientMessageId, reason)
                    return@withLock pending.copy(status = DeliveryStatus.FAILED.name, errorReason = reason).toDomainModel()
                }
                val sanitized = pending.content.replace(Regex("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]"), "")

                val dto = MessageDto(
                    id = null, // server generates
                    conversationId = pending.conversationId,
                    senderId = pending.senderId,
                    senderName = pending.senderName,
                    content = sanitized,
                    type = pending.type,
                    imageUrl = pending.imageUrl,
                    voice_url = pending.voiceUrl,
                    voiceDuration = pending.voiceDuration,
                    clientMessageId = pending.clientMessageId,
                    deliveryStatus = DeliveryStatus.PENDING.name.lowercase(),
                    replyToId = replyToId,
                    replyToSnippet = replyToSnippet,
                    replyToSenderName = replyToSenderName
                )
                val response = api.sendMessage(dto)
                if (response.isSuccessful) {
                    val sent = response.body()?.firstOrNull()
                    if (sent != null) {
                        val message = mapDtoToMessage(sent).copy(
                            // Preserve client id for deduplication
                            // Server doesn't know about clientMessageId, but we map it locally
                        )
                        // Persist as SENT in Room
                        try {
                            messageDao.insertMessage(
                                mapMessageToEntity(message).copy(
                                    deliveryStatus = DeliveryStatus.SENT.name,
                                    clientMessageId = pending.clientMessageId
                                )
                            )
                            conversationDao.updateLastMessage(
                                pending.conversationId,
                                sanitized,
                                message.timestamp
                            )
                        } catch (e: Exception) { Timber.w(TAG, "Failed to persist sent message", e) }

                        // Remove from outbox
                        pendingMessageDao.delete(pending.clientMessageId)

                        // Best-effort server patch
                        try {
                            api.updateConversation(
                                pending.conversationId,
                                mapOf(
                                    "last_message" to sanitized,
                                    "last_message_time" to DateUtils.formatIsoUtc(message.timestamp)
                                )
                            )
                        } catch (e: Exception) { Timber.w(TAG, "Failed to update conversation last_message", e) }

                        return@withLock MessageWithDelivery(
                            message = message,
                            status = DeliveryStatus.SENT,
                            clientMessageId = pending.clientMessageId
                        )
                    }
                }
                if (response.code() == 409) {
                    val duplicate = api.getMessages(
                        conversationId = PostgrestFilter.eq(pending.conversationId),
                        clientMessageId = PostgrestFilter.eq(pending.clientMessageId)
                    )
                    val existing = if (duplicate.isSuccessful) duplicate.body()?.firstOrNull() else null
                    if (existing != null) {
                        val message = mapDtoToMessage(existing)
                        try {
                            messageDao.insertMessage(
                                mapMessageToEntity(message).copy(
                                    deliveryStatus = DeliveryStatus.SENT.name,
                                    clientMessageId = pending.clientMessageId
                                )
                            )
                            conversationDao.updateLastMessage(
                                pending.conversationId,
                                message.content,
                                message.timestamp
                            )
                        } catch (e: Exception) { Timber.w(TAG, "Failed to persist duplicate-confirmed message", e) }
                        pendingMessageDao.delete(pending.clientMessageId)
                        return@withLock MessageWithDelivery(
                            message = message,
                            status = DeliveryStatus.SENT,
                            clientMessageId = pending.clientMessageId
                        )
                    }
                }
                // Retryable failure
                return@withLock handleRetryableFailure(pending, "HTTP ${response.code()}")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(TAG, "Send exception", e)
                return@withLock handleRetryableFailure(pending, e.message ?: "Network error")
            }
        }
    }

    private suspend fun handleRetryableFailure(
        pending: PendingMessageEntity,
        reason: String
    ): MessageWithDelivery {
        val newRetryCount = pending.retryCount + 1
        if (newRetryCount >= MAX_RETRY_COUNT) {
            pendingMessageDao.markFailed(pending.clientMessageId, reason)
            return pending.copy(
                status = DeliveryStatus.FAILED.name,
                retryCount = newRetryCount,
                errorReason = reason,
                failedAt = System.currentTimeMillis()
            ).toDomainModel()
        }
        val backoff = (BASE_RETRY_DELAY_MS * (1L shl newRetryCount.coerceAtMost(4)))
            .coerceAtMost(30_000L)
        val nextRetryAt = System.currentTimeMillis() + backoff
        pendingMessageDao.markRetry(
            pending.clientMessageId,
            DeliveryStatus.PENDING.name,
            nextRetryAt
        )
        return pending.copy(
            status = DeliveryStatus.PENDING.name,
            retryCount = newRetryCount,
            nextRetryAt = nextRetryAt,
            errorReason = reason
        ).toDomainModel()
    }

    override suspend fun retryMessage(clientMessageId: String): Flow<MessageWithDelivery> = flow {
        val pending = pendingMessageDao.getByClientId(clientMessageId)
        if (pending != null) {
            emit(pending.toDomainModel())
            val result = sendMessageInternal(
                pending = pending.copy(retryCount = pending.retryCount),
                replyToId = pending.replyToId,
                replyToSnippet = pending.replyToSnippet,
                replyToSenderName = pending.replyToSenderName
            )
            emit(result)
        } else {
            emit(
                MessageWithDelivery(
                    message = Message(),
                    status = DeliveryStatus.FAILED,
                    errorReason = "Message not found in outbox"
                )
            )
        }
    }

    override suspend fun cancelMessage(clientMessageId: String): Result<Unit> {
        return try {
            pendingMessageDao.delete(clientMessageId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteMessage(messageId: String): Result<Unit> {
        return try {
            val response = api.deleteMessage(
                id = "eq.$messageId",
                body = mapOf("is_deleted" to true, "content" to "")
            )
            if (response.isSuccessful) {
                try { messageDao.softDeleteMessage(messageId) } catch (_: Exception) {}
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to delete message: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun loadOlderMessages(conversationId: String, beforeTimestamp: Long, limit: Int): Result<List<Message>> {
        return try {
            val beforeIso = DateUtils.formatIsoUtc(beforeTimestamp)
            val response = api.getMessages(
                conversationId = PostgrestFilter.eq(conversationId),
                createdAfter = "lt.$beforeIso",
                order = "created_at.desc",
                limit = limit
            )
            if (response.isSuccessful) {
                val messages = response.body()?.map { mapDtoToMessage(it) }?.reversed() ?: emptyList()
                if (messages.isNotEmpty()) {
                    try { messageDao.insertMessages(messages.map { mapMessageToEntity(it) }) } catch (_: Exception) {}
                }
                Result.success(messages)
            } else {
                Result.failure(Exception("Failed to load older messages: ${response.code()}"))
            }
        } catch (e: Exception) {
            // Fallback to Room cache
            try {
                val roomMessages = messageDao.getMessagesPage(conversationId, limit, 0)
                    .filter { it.timestamp < beforeTimestamp }
                    .map { it.toDomainModel() }
                if (roomMessages.isNotEmpty()) Result.success(roomMessages)
                else Result.failure(e)
            } catch (_: Exception) {
                Result.failure(e)
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // OUTBOX SYNC (WorkManager entrypoint)
    // ═══════════════════════════════════════════════════════════════════════

    override suspend fun syncOutbox(): Result<Int> {
        val ready = pendingMessageDao.getMessagesReadyForRetry()
        if (ready.isEmpty()) return Result.success(0)

        var synced = 0
        for (pending in ready) {
            val result = sendMessageInternal(pending)
            if (result.status == DeliveryStatus.SENT) synced++
            // Small delay between sends to avoid overwhelming the server
            delay(150)
        }
        // Prune old sent records
        pendingMessageDao.pruneSent(System.currentTimeMillis() - 24 * 60 * 60 * 1000)
        return Result.success(synced)
    }

    // ── Apply message (scrim application) ──
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
            Timber.d("sendApplyMessage: scrim=$scrimId applicant=$applicantId creator=$scrimCreatorId")
            val convResult = getOrCreateConversation(
                scrimId = scrimId, scrimTitle = scrimTitle,
                participantAId = applicantId, participantAName = applicantName,
                participantATeamId = applicantTeamId, participantATeamName = applicantTeamName,
                participantBId = scrimCreatorId, participantBName = scrimCreatorName,
                participantBTeamId = scrimCreatorTeamId, participantBTeamName = scrimCreatorTeamName
            )
            var conversation: Conversation? = null
            convResult.collect { result ->
                result
                    .onSuccess { conv -> conversation = conv; Timber.d("sendApplyMessage: conversation found/created ${conv.id}") }
                    .onFailure { Timber.e("sendApplyMessage: failed to get/create conversation", it) }
            }
            val conv = conversation ?: run {
                emit(Result.failure(Exception("Failed to create conversation")))
                return@flow
            }

            // In the approve flow, the HOST calls this function, so sender MUST be the host
            // (messages RLS requires sender_id = auth.uid()).
            val senderId = scrimCreatorId
            val senderName = scrimCreatorName

            val applyContent = "$applicantName ($applicantTeamName) applied to join \"$scrimTitle\" [$teamPlayerCount/$teamMaxPlayers players]"
            val messageDto = MessageDto(
                conversationId = conv.id,
                senderId = senderId,
                senderName = senderName,
                content = applyContent,
                type = "apply"
            )
            Timber.d("sendApplyMessage: sending message as host $senderId")
            val msgResponse = api.sendMessage(messageDto)
            if (msgResponse.isSuccessful) {
                Timber.d("sendApplyMessage: message sent successfully")
                emit(Result.success(conv))
            } else {
                val errBody = msgResponse.errorBody()?.string() ?: ""
                Timber.e("sendApplyMessage: failed to send message ${msgResponse.code()} — $errBody")
                // Still return the conversation even if message fails —
                // the caller (approve) should proceed with approveApplication regardless
                emit(Result.success(conv))
            }
        } catch (e: Exception) {
            Timber.e("sendApplyMessage: exception", e)
            emit(Result.failure(e))
        }
    }

    // ── Read receipts ──
    override suspend fun markConversationAsRead(conversationId: String, userId: String): Flow<Result<Unit>> = flow {
        try {
            api.markConversationAsRead(mapOf("p_conversation_id" to conversationId, "p_reader_id" to userId))
            try {
                messageDao.markMessagesAsRead(conversationId, userId, System.currentTimeMillis())
            } catch (e: Exception) { Timber.w(TAG, "Failed to mark messages as read in Room", e) }
            emit(Result.success(Unit))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    // ── Typing status ──
    override suspend fun setTypingStatus(
        conversationId: String,
        userId: String,
        isTyping: Boolean
    ): Flow<Result<Unit>> = flow {
        try {
            val cachedConv = getCachedConversation(conversationId)
            val conv = cachedConv ?: run {
                val response = api.getConversations(idFilter = "eq.$conversationId")
                if (response.isSuccessful) {
                    response.body()?.firstOrNull()?.let { mapDtoToConversation(it) }
                        ?.also { cacheConversation(it) }
                } else null
            }
            if (conv != null) {
                val field = if (userId == conv.participantAId) "participant_a_typing" else "participant_b_typing"
                api.updateConversation(conversationId, mapOf(field to isTyping))
                emit(Result.success(Unit))
            } else {
                emit(Result.failure(Exception("Conversation not found for typing status")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // REALTIME SUBSCRIPTION (lifecycle-managed, no polling conflict)
    // ═══════════════════════════════════════════════════════════════════════

    override fun subscribeToMessages(conversationId: String, skipBridgeFetch: Boolean): Flow<Message> = flow {
        activeSubscriptions.add(conversationId)
        val cachedIds = mutableSetOf<String>()

        // Phase 1: Emit cached Room messages for instant display
        try {
            val cached = messageDao.getMessagesForConversation(conversationId).first()
            cached.forEach { entity ->
                cachedIds.add(entity.id)
                emit(entity.toDomainModel())
            }
        } catch (e: Exception) { Timber.w(TAG, "Failed to load cached messages", e) }

        // Phase 2: Bridge fetch — delta between cache and Realtime start
        // Skip if caller already loaded messages via getConversationById
        if (!skipBridgeFetch) {
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
                    try {
                        messageDao.insertMessages(serverMessages.map { mapMessageToEntity(it) })
                    } catch (_: Exception) {}
                }
            } catch (e: Exception) { Timber.w(TAG, "Bridge fetch failed", e) }
        }

        // Phase 3: Realtime (primary) with fallback polling only if WebSocket dies
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
                            } catch (e: Exception) { Timber.w(TAG, "Failed to persist realtime message", e) }
                            emit(message)
                        }
                    }
                } catch (e: Exception) {
                    Timber.w(TAG, "Failed to parse Realtime INSERT: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Timber.w(TAG, "Realtime ended for $conversationId: ${e.message}")
        } finally {
            activeSubscriptions.remove(conversationId)
        }
    }

    override fun unsubscribeFromMessages(conversationId: String) {
        activeSubscriptions.remove(conversationId)
        val channelName = "public:${SupabaseConfig.TABLE_MESSAGES}:conv_$conversationId"
        try { realtimeClient.unsubscribe(channelName) } catch (_: Exception) {}
    }

    override fun subscribeToConversation(conversationId: String): Flow<Conversation> = flow {
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

    // ── Direct message start ──
    override suspend fun startDirectConversation(
        senderId: String,
        senderName: String,
        recipientId: String,
        recipientName: String
    ): Flow<Result<Conversation>> = flow {
        try {
            val cachedMatch = cacheMutex.withLock {
                conversationLookupCache.values.find { entry ->
                    entry.isValid() && entry.conversation.scrimId.isEmpty() &&
                    ((entry.conversation.participantAId == senderId && entry.conversation.participantBId == recipientId) ||
                     (entry.conversation.participantAId == recipientId && entry.conversation.participantBId == senderId))
                }
            }
            if (cachedMatch != null) {
                emit(Result.success(cachedMatch.conversation))
                return@flow
            }

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
                    emit(Result.success(domainConv))
                    return@flow
                }
            } catch (e: Exception) { Timber.w(TAG, "Room lookup failed in startDirectConversation", e) }

            val existing1 = api.getConversations(
                participantAId = PostgrestFilter.eq(senderId),
                participantBId = PostgrestFilter.eq(recipientId)
            )
            if (existing1.isSuccessful && !existing1.body().isNullOrEmpty()) {
                val conv = mapDtoToConversation(existing1.body()!!.first())
                cacheConversation(conv)
                emit(Result.success(conv))
                return@flow
            }
            val existing2 = api.getConversations(
                participantAId = PostgrestFilter.eq(recipientId),
                participantBId = PostgrestFilter.eq(senderId)
            )
            if (existing2.isSuccessful && !existing2.body().isNullOrEmpty()) {
                val conv = mapDtoToConversation(existing2.body()!!.first())
                cacheConversation(conv)
                emit(Result.success(conv))
                return@flow
            }

            val newConvBody = mapOf(
                "participant_a_id" to senderId,
                "participant_a_name" to senderName,
                "participant_b_id" to recipientId,
                "participant_b_name" to recipientName,
                "last_message" to "Conversation started",
                "last_message_time" to DateUtils.formatIsoUtc(System.currentTimeMillis()),
                "chat_opens_at" to DateUtils.formatIsoUtc(System.currentTimeMillis())
            )
            val createResponse = api.createConversation(newConvBody)
            if (createResponse.isSuccessful) {
                val body = createResponse.body()
                if (!body.isNullOrEmpty()) {
                    val created = mapDtoToConversation(body.first())
                    cacheConversation(created)
                    conversationDao.insertConversation(mapConversationToEntity(created))
                    cacheManager.invalidateByPrefix(CACHE_KEY_CONVERSATIONS_PREFIX)
                    emit(Result.success(created))
                } else {
                    emit(Result.failure(Exception("Created conversation returned empty body")))
                }
            } else {
                emit(Result.failure(Exception("Failed to create direct conversation: ${createResponse.code()}")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    // ── Team conversation ──
    override suspend fun getOrCreateTeamConversation(
        teamId: String,
        teamName: String,
        leaderId: String,
        leaderName: String
    ): Flow<Result<Conversation>> = flow {
        try {
            val response = api.getOrCreateTeamConversation(
                mapOf(
                    "p_team_id" to teamId,
                    "p_team_name" to teamName,
                    "p_leader_id" to leaderId,
                    "p_leader_name" to leaderName
                )
            )
            if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                val conv = mapDtoToConversation(response.body()!!.first())
                cacheConversation(conv)
                conversationDao.insertConversation(mapConversationToEntity(conv))
                cacheManager.invalidateByPrefix(CACHE_KEY_CONVERSATIONS_PREFIX)
                emit(Result.success(conv))
            } else {
                emit(Result.failure(Exception("Failed to get/create team conversation: ${response.code()}")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Mapping functions (unchanged from previous implementation)
    // ═══════════════════════════════════════════════════════════════════════

    private fun mapDtoToMessage(dto: MessageDto): Message {
        val messageType = try { MessageType.valueOf(dto.type.uppercase()) } catch (_: Exception) { MessageType.TEXT }
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
            type = messageType,
            imageUrl = dto.imageUrl,
            voiceUrl = dto.voice_url,
            voiceDuration = dto.voiceDuration,
            replyToId = dto.replyToId,
            replyToSnippet = dto.replyToSnippet,
            replyToSenderName = dto.replyToSenderName,
            isDeleted = dto.isDeleted
        )
    }

    private fun mapDtoToConversation(dto: ConversationDto): Conversation {
        return Conversation(
            id = dto.id,
            scrimId = dto.scrimId ?: "",
            scrimTitle = "",
            participantAId = dto.participantAId ?: "",
            participantAName = dto.participantAName ?: "",
            participantATeamId = dto.participantATeamId ?: "",
            participantATeamName = dto.participantATeamName ?: "",
            participantAAvatarUrl = dto.participantAAvatarUrl,
            participantBId = dto.participantBId ?: "",
            participantBName = dto.participantBName ?: "",
            participantBTeamId = dto.participantBTeamId ?: "",
            participantBTeamName = dto.participantBTeamName ?: "",
            participantBAvatarUrl = dto.participantBAvatarUrl,
            lastMessage = dto.lastMessage ?: "",
            lastMessageTime = DateUtils.parseIsoToMillis(dto.lastMessageTime),
            unreadCount = dto.unreadCount ?: 0,
            chatOpensAt = DateUtils.parseIsoToMillis(dto.chatOpensAt),
            isParticipantATyping = dto.participantATyping ?: false,
            isParticipantBTyping = dto.participantBTyping ?: false,
            tournamentMatchId = dto.tournamentMatchId,
            participantCount = dto.participantCount,
            isGroupChat = dto.tournamentMatchId != null || dto.participantCount > 2 || dto.isTeamChat,
            teamId = dto.teamId,
            isTeamChat = dto.isTeamChat,
            isPinned = dto.isPinned,
            groupName = dto.groupName ?: ""
        )
    }

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
            voiceDuration = record.get("voice_duration")?.asInt,
            clientMessageId = record.get("client_message_id")?.asString,
            deliveryStatus = record.get("delivery_status")?.asString,
            replyToId = record.get("reply_to_id")?.takeIf { !it.isJsonNull }?.asString,
            replyToSnippet = record.get("reply_to_snippet")?.takeIf { !it.isJsonNull }?.asString,
            replyToSenderName = record.get("reply_to_sender_name")?.takeIf { !it.isJsonNull }?.asString,
            isDeleted = record.get("is_deleted")?.asBoolean ?: false
        )
    }

    private fun parseRealtimeRecordToConversationDto(record: com.google.gson.JsonObject): ConversationDto {
        return ConversationDto(
            id = record.get("id")?.asString ?: "",
            scrimId = record.get("scrim_id")?.asString,
            tournamentMatchId = record.get("tournament_match_id")?.asString,
            participantAId = record.get("participant_a_id")?.asString ?: "",
            participantAName = record.get("participant_a_name")?.asString ?: "",
            participantATeamId = record.get("participant_a_team_id")?.asString ?: "",
            participantATeamName = record.get("participant_a_team_name")?.asString ?: "",
            participantAAvatarUrl = record.get("participant_a_avatar_url")?.takeIf { !it.isJsonNull }?.asString,
            participantBId = record.get("participant_b_id")?.asString ?: "",
            participantBName = record.get("participant_b_name")?.asString ?: "",
            participantBTeamId = record.get("participant_b_team_id")?.asString ?: "",
            participantBTeamName = record.get("participant_b_team_name")?.asString ?: "",
            participantBAvatarUrl = record.get("participant_b_avatar_url")?.takeIf { !it.isJsonNull }?.asString,
            lastMessage = record.get("last_message")?.asString ?: "",
            lastMessageTime = record.get("last_message_time")?.asString ?: "",
            chatOpensAt = record.get("chat_opens_at")?.asString ?: "",
            participantATyping = record.get("participant_a_typing")?.asBoolean ?: false,
            participantBTyping = record.get("participant_b_typing")?.asBoolean ?: false,
            participantCount = record.get("participant_count")?.asInt ?: 2,
            teamId = record.get("team_id")?.takeIf { !it.isJsonNull }?.asString,
            isTeamChat = record.get("is_team_chat")?.asBoolean ?: false,
            isPinned = record.get("is_pinned")?.asBoolean ?: false,
            groupName = record.get("group_name")?.takeIf { !it.isJsonNull }?.asString,
            unreadCount = record.get("unread_count")?.asInt ?: 0
        )
    }

    private fun mapConversationToEntity(conv: Conversation): ConversationEntity {
        return ConversationEntity(
            id = conv.id,
            scrimId = conv.scrimId,
            scrimTitle = conv.scrimTitle,
            participantAId = conv.participantAId,
            participantAName = conv.participantAName,
            participantATeamId = conv.participantATeamId,
            participantATeamName = conv.participantATeamName,
            participantAAvatarUrl = conv.participantAAvatarUrl,
            participantBId = conv.participantBId,
            participantBName = conv.participantBName,
            participantBTeamId = conv.participantBTeamId,
            participantBTeamName = conv.participantBTeamName,
            participantBAvatarUrl = conv.participantBAvatarUrl,
            lastMessage = conv.lastMessage,
            lastMessageTime = conv.lastMessageTime,
            chatOpensAt = conv.chatOpensAt,
            isParticipantATyping = conv.isParticipantATyping,
            isParticipantBTyping = conv.isParticipantBTyping,
            tournamentMatchId = conv.tournamentMatchId,
            participantCount = conv.participantCount,
            isGroupChat = conv.isGroupChat,
            unreadCount = conv.unreadCount,
            teamId = conv.teamId,
            isTeamChat = conv.isTeamChat,
            isPinned = conv.isPinned,
            groupName = conv.groupName
        )
    }

    private fun mapMessageToEntity(msg: Message): MessageEntity {
        return MessageEntity(
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
            voiceDuration = msg.voiceDuration,
            deliveryStatus = DeliveryStatus.SENT.name,
            clientMessageId = null,
            replyToId = msg.replyToId,
            replyToSnippet = msg.replyToSnippet,
            replyToSenderName = msg.replyToSenderName,
            isDeleted = msg.isDeleted
        )
    }
}

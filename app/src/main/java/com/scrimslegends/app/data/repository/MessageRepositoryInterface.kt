package com.scrimslegends.app.data.repository

import com.scrimslegends.app.data.model.Conversation
import com.scrimslegends.app.data.model.Message
import com.scrimslegends.app.data.model.MessageType
import kotlinx.coroutines.flow.Flow

interface MessageRepositoryInterface {
    suspend fun getConversationsForUser(userId: String): Flow<Result<List<Conversation>>>
    suspend fun getConversationById(conversationId: String): Flow<Result<Conversation?>>
    suspend fun getOrCreateConversation(
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
    ): Flow<Result<Conversation>>

    /**
     * Send a message with idempotent delivery.
     *
     * @param clientMessageId Local UUID for deduplication. Same value on retry = same message.
     * @return Flow emitting PENDING → SENDING → SENT (or FAILED).
     */
    suspend fun sendMessage(
        conversationId: String,
        senderId: String,
        senderName: String,
        content: String,
        type: MessageType,
        clientMessageId: String,
        imageUrl: String? = null,
        voiceUrl: String? = null,
        voiceDuration: Int? = null
    ): Flow<com.scrimslegends.app.data.model.MessageWithDelivery>

    /**
     * Retry a previously failed message by its clientMessageId.
     */
    suspend fun retryMessage(clientMessageId: String): Flow<com.scrimslegends.app.data.model.MessageWithDelivery>

    /**
     * Cancel a pending message (removes from outbox).
     */
    suspend fun cancelMessage(clientMessageId: String): Result<Unit>

    suspend fun sendApplyMessage(
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
    ): Flow<Result<Conversation>>

    suspend fun markConversationAsRead(conversationId: String, userId: String): Flow<Result<Unit>>

    suspend fun setTypingStatus(conversationId: String, userId: String, isTyping: Boolean): Flow<Result<Unit>>

    /**
     * Subscribe to messages with full lifecycle management.
     * Automatically handles bridge fetch + Realtime + fallback polling.
     */
    fun subscribeToMessages(conversationId: String): Flow<Message>

    fun subscribeToConversation(conversationId: String): Flow<Conversation>

    suspend fun startDirectConversation(
        senderId: String,
        senderName: String,
        recipientId: String,
        recipientName: String
    ): Flow<Result<Conversation>>

    /**
     * Explicitly unsubscribe from a message stream and clean up resources.
     */
    fun unsubscribeFromMessages(conversationId: String)

    /**
     * Sync all pending outbox messages immediately (called by WorkManager).
     */
    suspend fun syncOutbox(): Result<Int>

    /**
     * Observe connection state of the messaging transport.
     */
    fun observeConnectionState(): Flow<com.scrimslegends.app.data.service.ChatConnectionState>
}

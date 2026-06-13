package com.scrimslegends.app.data.repository

import com.scrimslegends.app.data.model.Conversation
import com.scrimslegends.app.data.model.Message
import com.scrimslegends.app.data.model.MessageType
import com.scrimslegends.app.data.model.MessageWithDelivery
import kotlinx.coroutines.flow.Flow
import androidx.paging.PagingData

interface MessageRepositoryInterface {
    suspend fun getConversationsForUser(userId: String, forceRefresh: Boolean = false): Flow<Result<List<Conversation>>>
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
        content: String,
        type: MessageType,
        clientMessageId: String,
        senderId: String,
        senderName: String,
        imageUrl: String? = null,
        voiceUrl: String? = null,
        voiceDuration: Int? = null,
        replyToId: String? = null,
        replyToSnippet: String? = null,
        replyToSenderName: String? = null
    ): Flow<com.scrimslegends.app.data.model.MessageWithDelivery>

    /**
     * Retry a previously failed message by its clientMessageId.
     */
    suspend fun retryMessage(clientMessageId: String): Flow<com.scrimslegends.app.data.model.MessageWithDelivery>

    /**
     * Cancel a pending message (removes from outbox).
     */
    suspend fun cancelMessage(clientMessageId: String): Result<Unit>

    /**
     * Soft-delete a message (sets is_deleted = true, clears content).
     */
    suspend fun deleteMessage(messageId: String): Result<Unit>

    /**
     * Clears the chat history for the current user.
     */
    suspend fun clearChatHistory(conversationId: String): Result<Unit>

    /**
     * Load older messages for pagination (before the given timestamp).
     * Returns a stream of PagingData driven by Room and RemoteMediator.
     */
    fun getMessagesPaged(conversationId: String): Flow<PagingData<MessageWithDelivery>>

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
     * @param skipBridgeFetch If true, skip the Phase 2 bridge fetch (use when
     *        messages were already loaded via getConversationById).
     */
    fun subscribeToMessages(conversationId: String, skipBridgeFetch: Boolean = false): Flow<Message>

    fun subscribeToConversation(conversationId: String): Flow<Conversation>

    suspend fun startDirectConversation(
        senderId: String,
        senderName: String,
        recipientId: String,
        recipientName: String
    ): Flow<Result<Conversation>>

    suspend fun getOrCreateTeamConversation(
        teamId: String,
        teamName: String,
        leaderId: String,
        leaderName: String
    ): Flow<Result<Conversation>>

    /**
     * Explicitly unsubscribe from a message stream and clean up resources.
     */
    fun unsubscribeFromMessages(conversationId: String)
    fun cleanupConversation(conversationId: String)

    /**
     * Sync all pending outbox messages immediately (called by WorkManager).
     */
    suspend fun syncOutbox(): Result<Int>

    /**
     * Observe connection state of the messaging transport.
     */
    fun observeConnectionState(): Flow<com.scrimslegends.app.data.service.ChatConnectionState>

    /**
     * Block a user so they cannot send messages to the current user.
     */
    suspend fun blockUser(blockerId: String, blockedId: String): Result<Unit>

    /**
     * Unblock a previously blocked user.
     */
    suspend fun unblockUser(blockerId: String, blockedId: String): Result<Unit>

    /**
     * Check if a block exists between two users.
     */
    suspend fun checkBlockStatus(user1Id: String, user2Id: String): Result<com.scrimslegends.app.data.model.BlockStatus>
}

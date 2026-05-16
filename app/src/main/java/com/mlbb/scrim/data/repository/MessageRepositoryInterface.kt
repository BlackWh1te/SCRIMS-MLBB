package com.mlbb.scrim.data.repository

import com.mlbb.scrim.data.model.Conversation
import com.mlbb.scrim.data.model.Message
import com.mlbb.scrim.data.model.MessageType
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

    suspend fun sendMessage(
        conversationId: String,
        senderId: String,
        senderName: String,
        content: String,
        type: MessageType,
        imageUrl: String? = null,
        voiceUrl: String? = null,
        voiceDuration: Int? = null
    ): Flow<Result<Message>>

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
    
    fun subscribeToMessages(conversationId: String): Flow<Message>
    
    fun subscribeToConversation(conversationId: String): Flow<Conversation>
    suspend fun startDirectConversation(
        senderId: String,
        senderName: String,
        recipientId: String,
        recipientName: String
    ): Flow<Result<Conversation>>
}

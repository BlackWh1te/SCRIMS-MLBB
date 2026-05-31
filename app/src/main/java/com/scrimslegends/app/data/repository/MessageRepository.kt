package com.scrimslegends.app.data.repository

import com.scrimslegends.app.data.model.Conversation
import com.scrimslegends.app.data.model.DeliveryStatus
import com.scrimslegends.app.data.model.Message
import com.scrimslegends.app.data.model.MessageType
import com.scrimslegends.app.data.model.MessageWithDelivery
import com.scrimslegends.app.data.service.ChatConnectionState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class MessageRepository : MessageRepositoryInterface {

    private val conversations = mutableListOf<Conversation>()

    init {
        // Mock conversation for demo
        val convId = java.util.UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        conversations.add(
            Conversation(
                id = convId,
                scrimId = "scrim1",
                scrimTitle = "Ranked Scrim - Elite Squad vs Phoenix Rising",
                participantAId = "player1",
                participantAName = "EliteLeader",
                participantATeamId = "team1",
                participantATeamName = "Elite Squad",
                participantBId = "player2",
                participantBName = "PhoenixLeader",
                participantBTeamId = "team2",
                participantBTeamName = "Phoenix Rising",
                lastMessage = "We are ready for the match at 8PM!",
                lastMessageTime = now - 3600000,
                unreadCount = 2,
                messages = listOf(
                    Message(
                        id = java.util.UUID.randomUUID().toString(),
                        conversationId = convId,
                        senderId = "player2",
                        senderName = "PhoenixLeader",
                        content = "Hey! We saw your scrim post. Our team is interested.",
                        timestamp = now - 7200000,
                        type = MessageType.TEXT
                    ),
                    Message(
                        id = java.util.UUID.randomUUID().toString(),
                        conversationId = convId,
                        senderId = "system",
                        senderName = "System",
                        content = "Phoenix Rising has applied to join your scrim.",
                        timestamp = now - 7000000,
                        type = MessageType.SYSTEM
                    ),
                    Message(
                        id = java.util.UUID.randomUUID().toString(),
                        conversationId = convId,
                        senderId = "player2",
                        senderName = "PhoenixLeader",
                        content = "Team ID: team2 | Team Name: Phoenix Rising | Players: 5/7",
                        timestamp = now - 6900000,
                        type = MessageType.APPLY
                    ),
                    Message(
                        id = java.util.UUID.randomUUID().toString(),
                        conversationId = convId,
                        senderId = "player1",
                        senderName = "EliteLeader",
                        content = "Great! What time works for you?",
                        timestamp = now - 5000000,
                        type = MessageType.TEXT
                    ),
                    Message(
                        id = java.util.UUID.randomUUID().toString(),
                        conversationId = convId,
                        senderId = "player2",
                        senderName = "PhoenixLeader",
                        content = "We are ready for the match at 8PM!",
                        timestamp = now - 3600000,
                        type = MessageType.TEXT
                    )
                )
            )
        )
    }

    override suspend fun getConversationsForUser(userId: String, forceRefresh: Boolean): Flow<Result<List<Conversation>>> = flow {
        kotlinx.coroutines.delay(300)
        val userConversations = conversations.filter {
            it.participantAId == userId || it.participantBId == userId
        }.sortedByDescending { it.lastMessageTime }
        emit(Result.success(userConversations))
    }

    override suspend fun getConversationById(conversationId: String): Flow<Result<Conversation?>> = flow {
        kotlinx.coroutines.delay(200)
        val conversation = conversations.find { it.id == conversationId }
        emit(Result.success(conversation))
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
        kotlinx.coroutines.delay(300)

        val existing = conversations.find { it.scrimId == scrimId }
        if (existing != null) {
            emit(Result.success(existing))
            return@flow
        }

        val newConversation = Conversation(
            id = java.util.UUID.randomUUID().toString(),
            scrimId = scrimId,
            scrimTitle = scrimTitle,
            participantAId = participantAId,
            participantAName = participantAName,
            participantATeamId = participantATeamId,
            participantATeamName = participantATeamName,
            participantBId = participantBId,
            participantBName = participantBName,
            participantBTeamId = participantBTeamId,
            participantBTeamName = participantBTeamName
        )
        conversations.add(newConversation)
        emit(Result.success(newConversation))
    }

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
        kotlinx.coroutines.delay(300)

        val index = conversations.indexOfFirst { it.id == conversationId }
        if (index == -1) {
            emit(MessageWithDelivery(Message(), DeliveryStatus.FAILED, clientMessageId, errorReason = "Conversation not found"))
            return@flow
        }

        val message = Message(
            id = java.util.UUID.randomUUID().toString(),
            conversationId = conversationId,
            senderId = senderId,
            senderName = senderName,
            content = content,
            type = type,
            imageUrl = imageUrl,
            voiceUrl = voiceUrl,
            voiceDuration = voiceDuration,
            replyToId = replyToId,
            replyToSnippet = replyToSnippet,
            replyToSenderName = replyToSenderName
        )

        val conversation = conversations[index]
        val updatedMessages = conversation.messages + message
        val updatedConversation = conversation.copy(
            messages = updatedMessages,
            lastMessage = content,
            lastMessageTime = System.currentTimeMillis()
        )
        conversations[index] = updatedConversation

        emit(MessageWithDelivery(message, DeliveryStatus.SENT, clientMessageId))
    }

    override suspend fun retryMessage(clientMessageId: String): Flow<MessageWithDelivery> = flow {
        emit(MessageWithDelivery(Message(), DeliveryStatus.FAILED, clientMessageId, errorReason = "Mock retry not supported"))
    }

    override suspend fun cancelMessage(clientMessageId: String): Result<Unit> = Result.success(Unit)

    override suspend fun deleteMessage(messageId: String): Result<Unit> = Result.success(Unit)

    override suspend fun loadOlderMessages(conversationId: String, beforeTimestamp: Long, limit: Int): Result<List<Message>> =
        Result.success(emptyList())

    override suspend fun syncOutbox(): Result<Int> = Result.success(0)
    override fun unsubscribeFromMessages(conversationId: String) {}
    override fun observeConnectionState(): Flow<ChatConnectionState> = flow { emit(ChatConnectionState.CONNECTED) }

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
        kotlinx.coroutines.delay(500)

        // Get or create conversation
        val convResult = getOrCreateConversation(
            scrimId = scrimId,
            scrimTitle = scrimTitle,
            participantAId = scrimCreatorId,
            participantAName = scrimCreatorName,
            participantATeamId = scrimCreatorTeamId,
            participantATeamName = scrimCreatorTeamName,
            participantBId = applicantId,
            participantBName = applicantName,
            participantBTeamId = applicantTeamId,
            participantBTeamName = applicantTeamName
        )

        var conversation: Conversation? = null
        convResult.collect { result ->
            result.onSuccess { conv ->
                conversation = conv
            }
        }

        val conv = conversation
        if (conv == null) {
            emit(Result.failure(Exception("Failed to create conversation")))
            return@flow
        }

        val convId = conv.id

        // Send system message
        sendMessage(
            conversationId = convId,
            senderId = "system",
            senderName = "System",
            content = "$applicantTeamName has applied to join your scrim.",
            type = MessageType.SYSTEM,
            clientMessageId = "mock_${java.util.UUID.randomUUID()}"
        ).collect {}

        // Send apply info message with team details
        sendMessage(
            conversationId = convId,
            senderId = applicantId,
            senderName = applicantName,
            content = "Team ID: $applicantTeamId | Team Name: $applicantTeamName | Players: $teamPlayerCount/$teamMaxPlayers",
            type = MessageType.APPLY,
            clientMessageId = "mock_${java.util.UUID.randomUUID()}"
        ).collect {}

        // Update unread count for the scrim creator
        val idx = conversations.indexOfFirst { it.id == convId }
        if (idx != -1) {
            conversations[idx] = conversations[idx].copy(
                unreadCount = conversations[idx].unreadCount + 2
            )
        }

        emit(Result.success(conversations.find { it.id == convId }!!))
    }

    override suspend fun markConversationAsRead(conversationId: String, userId: String): Flow<Result<Unit>> = flow {
        kotlinx.coroutines.delay(200)

        val index = conversations.indexOfFirst { it.id == conversationId }
        if (index == -1) {
            emit(Result.failure(Exception("Conversation not found")))
            return@flow
        }

        val conversation = conversations[index]
        val updatedMessages = conversation.messages.map { msg ->
            if (msg.senderId != userId) msg.copy(isRead = true) else msg
        }

        conversations[index] = conversation.copy(
            messages = updatedMessages,
            unreadCount = 0
        )

        emit(Result.success(Unit))
    }

    override suspend fun startDirectConversation(
        senderId: String,
        senderName: String,
        recipientId: String,
        recipientName: String
    ): Flow<Result<Conversation>> = flow {
        kotlinx.coroutines.delay(300)
        val convId = java.util.UUID.randomUUID().toString()
        val newConv = Conversation(
            id = convId,
            participantAId = senderId,
            participantAName = senderName,
            participantBId = recipientId,
            participantBName = recipientName,
            lastMessage = "Direct conversation started",
            lastMessageTime = System.currentTimeMillis()
        )
        conversations.add(newConv)
        emit(Result.success(newConv))
    }

    override suspend fun setTypingStatus(conversationId: String, userId: String, isTyping: Boolean): Flow<Result<Unit>> = flow {
        emit(Result.success(Unit))
    }

    override fun subscribeToMessages(conversationId: String, skipBridgeFetch: Boolean): Flow<Message> = flow {
        // No-op for mock
    }

    override fun subscribeToConversation(conversationId: String): Flow<Conversation> = flow {
        // No-op for mock
    }

    override suspend fun getOrCreateTeamConversation(
        teamId: String,
        teamName: String,
        leaderId: String,
        leaderName: String
    ): Flow<Result<Conversation>> = flow {
        kotlinx.coroutines.delay(300)
        val convId = java.util.UUID.randomUUID().toString()
        val newConv = Conversation(
            id = convId,
            teamId = teamId,
            isTeamChat = true,
            groupName = teamName,
            participantAId = leaderId,
            participantAName = leaderName,
            participantATeamId = teamId,
            participantATeamName = teamName,
            lastMessage = "Team chat created",
            lastMessageTime = System.currentTimeMillis()
        )
        conversations.add(newConv)
        emit(Result.success(newConv))
    }
}

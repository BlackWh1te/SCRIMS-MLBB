package com.mlbb.scrim.data.repository

import com.mlbb.scrim.data.model.Conversation
import com.mlbb.scrim.data.model.Message
import com.mlbb.scrim.data.model.MessageType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class MessageRepositoryTest {

    private lateinit var messageRepository: MessageRepository
    private lateinit var testDispatcher: TestDispatcher

    @Before
    fun setup() {
        messageRepository = MessageRepository()
        testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ─── Get Conversations For User Tests ───

    @Test
    fun `getConversationsForUser returns conversations where user is participant A`() {
        // Act
        val result = messageRepository.getConversationsForUser("player1").first()

        advanceUntilIdle()

        // Assert
        assertTrue(result.isSuccess)
        val conversations = result.getOrNull()!!
        assertTrue(conversations.isNotEmpty())
        conversations.forEach { conv ->
            assertTrue(conv.participantAId == "player1" || conv.participantBId == "player1")
        }
    }

    @Test
    fun `getConversationsForUser returns conversations where user is participant B`() {
        // Act
        val result = messageRepository.getConversationsForUser("player2").first()

        advanceUntilIdle()

        // Assert
        assertTrue(result.isSuccess)
        val conversations = result.getOrNull()!!
        assertTrue(conversations.isNotEmpty())
        conversations.forEach { conv ->
            assertTrue(conv.participantAId == "player2" || conv.participantBId == "player2")
        }
    }

    @Test
    fun `getConversationsForUser returns empty list for non-existent user`() {
        // Act
        val result = messageRepository.getConversationsForUser("nonexistent_user").first()

        advanceUntilIdle()

        // Assert
        assertTrue(result.isSuccess)
        val conversations = result.getOrNull()!!
        assertTrue(conversations.isEmpty())
    }

    @Test
    fun `getConversationsForUser returns conversations sorted by last message time`() {
        // Act
        val result = messageRepository.getConversationsForUser("player1").first()

        advanceUntilIdle()

        // Assert
        assertTrue(result.isSuccess)
        val conversations = result.getOrNull()!!
        if (conversations.size > 1) {
            for (i in 0 until conversations.size - 1) {
                assertTrue(conversations[i].lastMessageTime >= conversations[i + 1].lastMessageTime)
            }
        }
    }

    // ─── Get Conversation By Id Tests ───

    @Test
    fun `getConversationById returns conversation when it exists`() {
        // Arrange
        val conversations = messageRepository.getConversationsForUser("player1").first().getOrNull()!!
        val conversationId = conversations.first().id

        // Act
        val result = messageRepository.getConversationById(conversationId).first()

        advanceUntilIdle()

        // Assert
        assertTrue(result.isSuccess)
        val conversation = result.getOrNull()
        assertNotNull(conversation)
        assertEquals(conversationId, conversation!!.id)
    }

    @Test
    fun `getConversationById returns null when conversation does not exist`() {
        // Act
        val result = messageRepository.getConversationById("nonexistent_id").first()

        advanceUntilIdle()

        // Assert
        assertTrue(result.isSuccess)
        val conversation = result.getOrNull()
        assertEquals(null, conversation)
    }

    // ─── Get Or Create Conversation Tests ───

    @Test
    fun `getOrCreateConversation creates new conversation when it does not exist`() {
        // Act
        val result = messageRepository.getOrCreateConversation(
            scrimId = "new_scrim",
            scrimTitle = "New Scrim",
            participantAId = "user1",
            participantAName = "User1",
            participantATeamId = "team1",
            participantATeamName = "Team1",
            participantBId = "user2",
            participantBName = "User2",
            participantBTeamId = "team2",
            participantBTeamName = "Team2"
        ).first()

        advanceUntilIdle()

        // Assert
        assertTrue(result.isSuccess)
        val conversation = result.getOrNull()!!
        assertEquals("new_scrim", conversation.scrimId)
        assertEquals("New Scrim", conversation.scrimTitle)
    }

    @Test
    fun `getOrCreateConversation returns existing conversation when it exists`() {
        // Arrange - First create a conversation
        val firstResult = messageRepository.getOrCreateConversation(
            scrimId = "existing_scrim",
            scrimTitle = "Existing Scrim",
            participantAId = "user1",
            participantAName = "User1",
            participantATeamId = "team1",
            participantATeamName = "Team1",
            participantBId = "user2",
            participantBName = "User2",
            participantBTeamId = "team2",
            participantBTeamName = "Team2"
        ).first()

        advanceUntilIdle()

        val firstConversationId = firstResult.getOrNull()!!.id

        // Act - Try to create the same conversation again
        val secondResult = messageRepository.getOrCreateConversation(
            scrimId = "existing_scrim",
            scrimTitle = "Different Title",
            participantAId = "user1",
            participantAName = "User1",
            participantATeamId = "team1",
            participantATeamName = "Team1",
            participantBId = "user2",
            participantBName = "User2",
            participantBTeamId = "team2",
            participantBTeamName = "Team2"
        ).first()

        advanceUntilIdle()

        // Assert
        assertTrue(secondResult.isSuccess)
        val secondConversation = secondResult.getOrNull()!!
        assertEquals(firstConversationId, secondConversation.id) // Should return the same conversation
        assertEquals("Existing Scrim", secondConversation.scrimTitle) // Original title preserved
    }

    // ─── Send Message Tests ───

    @Test
    fun `sendMessage successfully adds message to conversation`() {
        // Arrange
        val conversations = messageRepository.getConversationsForUser("player1").first().getOrNull()!!
        val conversationId = conversations.first().id
        val initialMessageCount = conversations.first().messages.size

        // Act
        val result = messageRepository.sendMessage(
            conversationId = conversationId,
            senderId = "player1",
            senderName = "Player1",
            content = "Test message",
            type = MessageType.TEXT,
            imageUrl = null,
            voiceUrl = null,
            voiceDuration = null
        ).first()

        advanceUntilIdle()

        // Assert
        assertTrue(result.isSuccess)
        val updatedConversation = messageRepository.getConversationById(conversationId).first().getOrNull()
        assertNotNull(updatedConversation)
        assertEquals(initialMessageCount + 1, updatedConversation!!.messages.size)
    }

    @Test
    fun `sendMessage returns failure when conversation does not exist`() {
        // Act
        val result = messageRepository.sendMessage(
            conversationId = "nonexistent_id",
            senderId = "player1",
            senderName = "Player1",
            content = "Test message",
            type = MessageType.TEXT,
            imageUrl = null,
            voiceUrl = null,
            voiceDuration = null
        ).first()

        advanceUntilIdle()

        // Assert
        assertTrue(result.isFailure)
    }

    @Test
    fun `sendMessage updates last message and last message time`() {
        // Arrange
        val conversations = messageRepository.getConversationsForUser("player1").first().getOrNull()!!
        val conversationId = conversations.first().id
        val newMessage = "New test message"

        // Act
        messageRepository.sendMessage(
            conversationId = conversationId,
            senderId = "player1",
            senderName = "Player1",
            content = newMessage,
            type = MessageType.TEXT,
            imageUrl = null,
            voiceUrl = null,
            voiceDuration = null
        ).first()

        advanceUntilIdle()

        val updatedConversation = messageRepository.getConversationById(conversationId).first().getOrNull()

        // Assert
        assertNotNull(updatedConversation)
        assertEquals(newMessage, updatedConversation!!.lastMessage)
        assertTrue(updatedConversation.lastMessageTime > conversations.first().lastMessageTime)
    }

    @Test
    fun `sendMessage handles message with image URL`() {
        // Arrange
        val conversations = messageRepository.getConversationsForUser("player1").first().getOrNull()!!
        val conversationId = conversations.first().id
        val imageUrl = "http://example.com/image.jpg"

        // Act
        val result = messageRepository.sendMessage(
            conversationId = conversationId,
            senderId = "player1",
            senderName = "Player1",
            content = "Check out this image",
            type = MessageType.IMAGE,
            imageUrl = imageUrl,
            voiceUrl = null,
            voiceDuration = null
        ).first()

        advanceUntilIdle()

        // Assert
        assertTrue(result.isSuccess)
        val message = result.getOrNull()!!
        assertEquals(imageUrl, message.imageUrl)
    }

    @Test
    fun `sendMessage handles message with voice URL`() {
        // Arrange
        val conversations = messageRepository.getConversationsForUser("player1").first().getOrNull()!!
        val conversationId = conversations.first().id
        val voiceUrl = "http://example.com/voice.mp3"
        val voiceDuration = 30

        // Act
        val result = messageRepository.sendMessage(
            conversationId = conversationId,
            senderId = "player1",
            senderName = "Player1",
            content = "Voice message",
            type = MessageType.VOICE,
            imageUrl = null,
            voiceUrl = voiceUrl,
            voiceDuration = voiceDuration
        ).first()

        advanceUntilIdle()

        // Assert
        assertTrue(result.isSuccess)
        val message = result.getOrNull()!!
        assertEquals(voiceUrl, message.voiceUrl)
        assertEquals(voiceDuration, message.voiceDuration)
    }

    // ─── Send Apply Message Tests ───

    @Test
    fun `sendApplyMessage creates conversation and sends apply messages`() {
        // Act
        val result = messageRepository.sendApplyMessage(
            scrimId = "apply_scrim",
            scrimTitle = "Apply Scrim",
            applicantId = "applicant1",
            applicantName = "Applicant1",
            applicantTeamId = "team_applicant",
            applicantTeamName = "Applicant Team",
            scrimCreatorId = "creator1",
            scrimCreatorName = "Creator1",
            scrimCreatorTeamId = "team_creator",
            scrimCreatorTeamName = "Creator Team",
            teamPlayerCount = 5,
            teamMaxPlayers = 7
        ).first()

        advanceUntilIdle()

        // Assert
        assertTrue(result.isSuccess)
        val conversation = result.getOrNull()!!
        assertNotNull(conversation)
        assertEquals("apply_scrim", conversation.scrimId)
    }

    @Test
    fun `sendApplyMessage increases unread count for scrim creator`() {
        // Act
        val result = messageRepository.sendApplyMessage(
            scrimId = "unread_scrim",
            scrimTitle = "Unread Scrim",
            applicantId = "applicant1",
            applicantName = "Applicant1",
            applicantTeamId = "team_applicant",
            applicantTeamName = "Applicant Team",
            scrimCreatorId = "creator1",
            scrimCreatorName = "Creator1",
            scrimCreatorTeamId = "team_creator",
            scrimCreatorTeamName = "Creator Team",
            teamPlayerCount = 5,
            teamMaxPlayers = 7
        ).first()

        advanceUntilIdle()

        // Assert
        assertTrue(result.isSuccess)
        val conversation = result.getOrNull()!!
        assertTrue(conversation.unreadCount > 0)
    }

    @Test
    fun `sendApplyMessage uses existing conversation if available`() {
        // Arrange - First create a conversation
        val firstResult = messageRepository.getOrCreateConversation(
            scrimId = "reuse_scrim",
            scrimTitle = "Reuse Scrim",
            participantAId = "creator1",
            participantAName = "Creator1",
            participantATeamId = "team_creator",
            participantATeamName = "Creator Team",
            participantBId = "applicant1",
            participantBName = "Applicant1",
            participantBTeamId = "team_applicant",
            participantBTeamName = "Applicant Team"
        ).first()

        advanceUntilIdle()

        val firstConversationId = firstResult.getOrNull()!!.id

        // Act - Send apply message
        val secondResult = messageRepository.sendApplyMessage(
            scrimId = "reuse_scrim",
            scrimTitle = "Reuse Scrim",
            applicantId = "applicant1",
            applicantName = "Applicant1",
            applicantTeamId = "team_applicant",
            applicantTeamName = "Applicant Team",
            scrimCreatorId = "creator1",
            scrimCreatorName = "Creator1",
            scrimCreatorTeamId = "team_creator",
            scrimCreatorTeamName = "Creator Team",
            teamPlayerCount = 5,
            teamMaxPlayers = 7
        ).first()

        advanceUntilIdle()

        // Assert
        assertTrue(secondResult.isSuccess)
        val secondConversation = secondResult.getOrNull()!!
        assertEquals(firstConversationId, secondConversation.id)
    }

    // ─── Mark Conversation As Read Tests ───

    @Test
    fun `markConversationAsRead sets unread count to zero`() {
        // Arrange
        val conversations = messageRepository.getConversationsForUser("player1").first().getOrNull()!!
        val conversationId = conversations.first().id

        // Act
        val result = messageRepository.markConversationAsRead(conversationId, "player1").first()

        advanceUntilIdle()

        // Assert
        assertTrue(result.isSuccess)
        val updatedConversation = messageRepository.getConversationById(conversationId).first().getOrNull()
        assertNotNull(updatedConversation)
        assertEquals(0, updatedConversation!!.unreadCount)
    }

    @Test
    fun `markConversationAsRead marks messages from other users as read`() {
        // Arrange
        val conversations = messageRepository.getConversationsForUser("player1").first().getOrNull()!!
        val conversationId = conversations.first().id

        // Act
        val result = messageRepository.markConversationAsRead(conversationId, "player1").first()

        advanceUntilIdle()

        // Assert
        assertTrue(result.isSuccess)
        val updatedConversation = messageRepository.getConversationById(conversationId).first().getOrNull()
        assertNotNull(updatedConversation)
        
        // Check that messages from other users are marked as read
        val otherUserMessages = updatedConversation!!.messages.filter { it.senderId != "player1" }
        otherUserMessages.forEach { assertTrue(it.isRead) }
    }

    @Test
    fun `markConversationAsRead returns failure when conversation does not exist`() {
        // Act
        val result = messageRepository.markConversationAsRead("nonexistent_id", "player1").first()

        advanceUntilIdle()

        // Assert
        assertTrue(result.isFailure)
    }

    // ─── Start Direct Conversation Tests ───

    @Test
    fun `startDirectConversation creates new conversation with correct participants`() {
        // Act
        val result = messageRepository.startDirectConversation(
            senderId = "user1",
            senderName = "User1",
            recipientId = "user2",
            recipientName = "User2"
        ).first()

        advanceUntilIdle()

        // Assert
        assertTrue(result.isSuccess)
        val conversation = result.getOrNull()!!
        assertEquals("user1", conversation.participantAId)
        assertEquals("User1", conversation.participantAName)
        assertEquals("user2", conversation.participantBId)
        assertEquals("User2", conversation.participantBName)
    }

    @Test
    fun `startDirectConversation sets initial last message`() {
        // Act
        val result = messageRepository.startDirectConversation(
            senderId = "user1",
            senderName = "User1",
            recipientId = "user2",
            recipientName = "User2"
        ).first()

        advanceUntilIdle()

        // Assert
        assertTrue(result.isSuccess)
        val conversation = result.getOrNull()!!
        assertEquals("Direct conversation started", conversation.lastMessage)
    }

    // ─── Set Typing Status Tests ───

    @Test
    fun `setTypingStatus returns success`() {
        // Act
        val result = messageRepository.setTypingStatus(
            conversationId = "conv_id",
            userId = "user1",
            isTyping = true
        ).first()

        advanceUntilIdle()

        // Assert
        assertTrue(result.isSuccess)
    }

    // ─── Subscribe Tests ───

    @Test
    fun `subscribeToMessages returns flow`() {
        // Act
        val flow = messageRepository.subscribeToMessages("conversation_id")

        // Assert
        assertNotNull(flow)
    }

    @Test
    fun `subscribeToConversation returns flow`() {
        // Act
        val flow = messageRepository.subscribeToConversation("conversation_id")

        // Assert
        assertNotNull(flow)
    }

    // ─── Integration Tests ───

    @Test
    fun `full messaging flow works correctly`() {
        // Arrange
        val scrimId = "integration_scrim"
        val creatorId = "creator1"
        val applicantId = "applicant1"

        // Act - Create conversation via apply message
        val applyResult = messageRepository.sendApplyMessage(
            scrimId = scrimId,
            scrimTitle = "Integration Scrim",
            applicantId = applicantId,
            applicantName = "Applicant1",
            applicantTeamId = "team_applicant",
            applicantTeamName = "Applicant Team",
            scrimCreatorId = creatorId,
            scrimCreatorName = "Creator1",
            scrimCreatorTeamId = "team_creator",
            scrimCreatorTeamName = "Creator Team",
            teamPlayerCount = 5,
            teamMaxPlayers = 7
        ).first()

        advanceUntilIdle()

        assertTrue(applyResult.isSuccess)
        val conversation = applyResult.getOrNull()!!
        val conversationId = conversation.id

        // Send a reply message
        val replyResult = messageRepository.sendMessage(
            conversationId = conversationId,
            senderId = creatorId,
            senderName = "Creator1",
            content = "Thanks for applying!",
            type = MessageType.TEXT,
            imageUrl = null,
            voiceUrl = null,
            voiceDuration = null
        ).first()

        advanceUntilIdle()

        assertTrue(replyResult.isSuccess)

        // Mark as read
        val readResult = messageRepository.markConversationAsRead(conversationId, applicantId).first()

        advanceUntilIdle()

        assertTrue(readResult.isSuccess)

        // Verify final state
        val finalConversation = messageRepository.getConversationById(conversationId).first().getOrNull()
        assertNotNull(finalConversation)
        assertTrue(finalConversation!!.messages.size >= 3) // System + Apply + Reply
        assertEquals(0, finalConversation.unreadCount)
    }

    // ─── Edge Case Tests ───

    @Test
    fun `sendMessage handles empty content`() {
        // Arrange
        val conversations = messageRepository.getConversationsForUser("player1").first().getOrNull()!!
        val conversationId = conversations.first().id

        // Act
        val result = messageRepository.sendMessage(
            conversationId = conversationId,
            senderId = "player1",
            senderName = "Player1",
            content = "",
            type = MessageType.TEXT,
            imageUrl = null,
            voiceUrl = null,
            voiceDuration = null
        ).first()

        advanceUntilIdle()

        // Assert
        assertTrue(result.isSuccess)
    }

    @Test
    fun `sendMessage handles very long content`() {
        // Arrange
        val conversations = messageRepository.getConversationsForUser("player1").first().getOrNull()!!
        val conversationId = conversations.first().id
        val longContent = "A".repeat(10000)

        // Act
        val result = messageRepository.sendMessage(
            conversationId = conversationId,
            senderId = "player1",
            senderName = "Player1",
            content = longContent,
            type = MessageType.TEXT,
            imageUrl = null,
            voiceUrl = null,
            voiceDuration = null
        ).first()

        advanceUntilIdle()

        // Assert
        assertTrue(result.isSuccess)
        val message = result.getOrNull()!!
        assertEquals(longContent, message.content)
    }

    @Test
    fun `sendApplyMessage handles zero player count`() {
        // Act
        val result = messageRepository.sendApplyMessage(
            scrimId = "zero_players",
            scrimTitle = "Zero Players Scrim",
            applicantId = "applicant1",
            applicantName = "Applicant1",
            applicantTeamId = "team_applicant",
            applicantTeamName = "Applicant Team",
            scrimCreatorId = "creator1",
            scrimCreatorName = "Creator1",
            scrimCreatorTeamId = "team_creator",
            scrimCreatorTeamName = "Creator Team",
            teamPlayerCount = 0,
            teamMaxPlayers = 7
        ).first()

        advanceUntilIdle()

        // Assert
        assertTrue(result.isSuccess)
    }

    @Test
    fun `sendApplyMessage handles max player count`() {
        // Act
        val result = messageRepository.sendApplyMessage(
            scrimId = "max_players",
            scrimTitle = "Max Players Scrim",
            applicantId = "applicant1",
            applicantName = "Applicant1",
            applicantTeamId = "team_applicant",
            applicantTeamName = "Applicant Team",
            scrimCreatorId = "creator1",
            scrimCreatorName = "Creator1",
            scrimCreatorTeamId = "team_creator",
            scrimCreatorTeamName = "Creator Team",
            teamPlayerCount = 7,
            teamMaxPlayers = 7
        ).first()

        advanceUntilIdle()

        // Assert
        assertTrue(result.isSuccess)
    }

    @Test
    fun `getOrCreateConversation handles same user as both participants`() {
        // Act
        val result = messageRepository.getOrCreateConversation(
            scrimId = "same_user_scrim",
            scrimTitle = "Same User Scrim",
            participantAId = "user1",
            participantAName = "User1",
            participantATeamId = "team1",
            participantATeamName = "Team1",
            participantBId = "user1", // Same user
            participantBName = "User1",
            participantBTeamId = "team1",
            participantBTeamName = "Team1"
        ).first()

        advanceUntilIdle()

        // Assert
        assertTrue(result.isSuccess)
        val conversation = result.getOrNull()!!
        assertEquals("user1", conversation.participantAId)
        assertEquals("user1", conversation.participantBId)
    }
}

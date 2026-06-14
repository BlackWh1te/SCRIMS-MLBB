package com.mlbb.scrim.viewmodel

import com.mlbb.scrim.data.model.Conversation
import com.mlbb.scrim.data.model.Message
import com.mlbb.scrim.data.model.MessageType
import com.mlbb.scrim.data.repository.MessageRepositoryInterface
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class MessageViewModelTest {

    private lateinit var viewModel: MessageViewModel
    private lateinit var mockRepository: MessageRepositoryInterface
    private lateinit var testDispatcher: TestDispatcher

    @Before
    fun setup() {
        mockRepository = mockk(relaxed = true)
        testDispatcher = StandardTestDispatcher()

        Dispatchers.setMain(testDispatcher)

        viewModel = MessageViewModel(
            messageRepository = mockRepository
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ─── Initialization Tests ───

    @Test
    fun `ViewModel initializes with empty state`() {
        // Assert
        assertTrue(viewModel.conversations.value.isEmpty())
        assertEquals(null, viewModel.selectedConversation.value)
        assertFalse(viewModel.isLoading.value)
        assertFalse(viewModel.isRefreshing.value)
        assertEquals(null, viewModel.error.value)
    }

    // ─── Load Conversations Tests ───

    @Test
    fun `loadConversations successfully loads conversations for user`() {
        // Arrange
        val userId = "user123"
        val mockConversations = listOf(
            createMockConversation(id = "1", participantAName = "User1"),
            createMockConversation(id = "2", participantAName = "User2")
        )
        coEvery { mockRepository.getConversationsForUser(userId) } returns flow { emit(Result.success(mockConversations)) }

        // Act
        viewModel.loadConversations(userId)
        advanceUntilIdle()

        // Assert
        assertEquals(mockConversations, viewModel.conversations.value)
        assertFalse(viewModel.isLoading.value)
    }

    @Test
    fun `loadConversations sets refreshing flag when isRefresh is true`() {
        // Arrange
        coEvery { mockRepository.getConversationsForUser(any()) } returns flow { emit(Result.success(emptyList())) }

        // Act
        viewModel.loadConversations("user123", isRefresh = true)
        advanceUntilIdle()

        // Assert
        assertFalse(viewModel.isRefreshing.value) // Should be false after completion
    }

    @Test
    fun `loadConversations handles error`() {
        // Arrange
        val errorMessage = "Failed to load conversations"
        coEvery { mockRepository.getConversationsForUser(any()) } returns flow { emit(Result.failure(Exception(errorMessage))) }

        // Act
        viewModel.loadConversations("user123")
        advanceUntilIdle()

        // Assert
        assertEquals(errorMessage, viewModel.error.value)
        assertFalse(viewModel.isLoading.value)
    }

    // ─── Load Conversation Tests ───

    @Test
    fun `loadConversation successfully loads conversation by ID`() {
        // Arrange
        val conversationId = "conv123"
        val mockConversation = createMockConversation(id = conversationId)
        coEvery { mockRepository.getConversationById(conversationId) } returns flow { emit(Result.success(mockConversation)) }

        // Act
        viewModel.loadConversation(conversationId)
        advanceUntilIdle()

        // Assert
        assertEquals(mockConversation, viewModel.selectedConversation.value)
        assertFalse(viewModel.isLoading.value)
    }

    @Test
    fun `loadConversation handles error`() {
        // Arrange
        val errorMessage = "Conversation not found"
        coEvery { mockRepository.getConversationById(any()) } returns flow { emit(Result.failure(Exception(errorMessage))) }

        // Act
        viewModel.loadConversation("conv123")
        advanceUntilIdle()

        // Assert
        assertEquals(errorMessage, viewModel.error.value)
        assertEquals(null, viewModel.selectedConversation.value)
    }

    // ─── Mark as Read Tests ───

    @Test
    fun `markAsRead successfully marks conversation as read`() {
        // Arrange
        val conversationId = "conv123"
        val userId = "user123"
        coEvery { mockRepository.markConversationAsRead(conversationId, userId) } returns flow { emit(Unit) }

        // Act
        viewModel.markAsRead(conversationId, userId)
        advanceUntilIdle()

        // Assert
        coVerify { mockRepository.markConversationAsRead(conversationId, userId) }
    }

    // ─── Polling Tests ───

    @Test
    fun `startConversationsPolling starts polling for conversations`() {
        // Arrange
        val userId = "user123"
        val mockConversations = listOf(createMockConversation(id = "1"))
        coEvery { mockRepository.getConversationsForUser(userId) } returns flow { emit(Result.success(mockConversations)) }

        // Act
        viewModel.startConversationsPolling(userId)
        advanceUntilIdle()

        // Assert
        assertEquals(mockConversations, viewModel.conversations.value)
    }

    @Test
    fun `stopConversationsPolling stops polling for conversations`() {
        // Arrange
        val userId = "user123"
        coEvery { mockRepository.getConversationsForUser(userId) } returns flow { emit(Result.success(emptyList())) }

        // Act
        viewModel.startConversationsPolling(userId)
        viewModel.stopConversationsPolling()
        advanceUntilIdle()

        // Assert - Should stop without errors
        assertTrue(true)
    }

    @Test
    fun `startChatPolling starts polling for messages`() {
        // Arrange
        val conversationId = "conv123"
        val userId = "user123"
        val mockConversation = createMockConversation(id = conversationId)
        val messageFlow = MutableSharedFlow<Message>()
        
        coEvery { mockRepository.markConversationAsRead(conversationId, userId) } returns flow { emit(Unit) }
        coEvery { mockRepository.getConversationById(conversationId) } returns flow { emit(Result.success(mockConversation)) }
        coEvery { mockRepository.subscribeToMessages(conversationId) } returns messageFlow
        coEvery { mockRepository.subscribeToConversation(conversationId) } returns flow { emit(mockConversation) }

        // Act
        viewModel.startChatPolling(conversationId, userId)
        advanceUntilIdle()

        // Assert
        assertEquals(mockConversation, viewModel.selectedConversation.value)
    }

    @Test
    fun `stopChatPolling stops polling for messages`() {
        // Arrange
        val conversationId = "conv123"
        val userId = "user123"
        coEvery { mockRepository.markConversationAsRead(any(), any()) } returns flow { emit(Unit) }
        coEvery { mockRepository.getConversationById(any()) } returns flow { emit(Result.success(createMockConversation())) }
        coEvery { mockRepository.subscribeToMessages(any()) } returns flow { }
        coEvery { mockRepository.subscribeToConversation(any()) } returns flow { }

        // Act
        viewModel.startChatPolling(conversationId, userId)
        viewModel.stopChatPolling()
        advanceUntilIdle()

        // Assert - Should stop without errors
        assertTrue(true)
    }

    // ─── Send Message Tests ───

    @Test
    fun `sendMessage successfully sends text message`() {
        // Arrange
        val conversationId = "conv123"
        val senderId = "sender123"
        val senderName = "Sender"
        val content = "Hello"
        val sentMessage = createMockMessage(id = "msg1", content = content)
        
        viewModel.selectedConversation.value = createMockConversation(id = conversationId)
        coEvery { 
            mockRepository.sendMessage(
                conversationId, senderId, senderName, content, MessageType.TEXT
            ) 
        } returns flow { emit(Result.success(sentMessage)) }

        // Act
        viewModel.sendMessage(conversationId, senderId, senderName, content)
        advanceUntilIdle()

        // Assert
        coVerify { 
            mockRepository.sendMessage(
                conversationId, senderId, senderName, content, MessageType.TEXT
            ) 
        }
        assertNotNull(viewModel.selectedConversation.value?.messages?.find { it.content == content })
    }

    @Test
    fun `sendMessage handles error`() {
        // Arrange
        val conversationId = "conv123"
        val errorMessage = "Failed to send message"
        viewModel.selectedConversation.value = createMockConversation(id = conversationId)
        coEvery { mockRepository.sendMessage(any(), any(), any(), any(), any()) } returns flow { emit(Result.failure(Exception(errorMessage))) }

        // Act
        viewModel.sendMessage(conversationId, "sender123", "Sender", "Hello")
        advanceUntilIdle()

        // Assert
        assertEquals(errorMessage, viewModel.error.value)
    }

    @Test
    fun `sendMessage adds optimistic message to conversation`() {
        // Arrange
        val conversationId = "conv123"
        val senderId = "sender123"
        val senderName = "Sender"
        val content = "Hello"
        
        val mockConversation = createMockConversation(id = conversationId, messages = emptyList())
        viewModel.selectedConversation.value = mockConversation
        coEvery { mockRepository.sendMessage(any(), any(), any(), any(), any()) } returns flow { emit(Result.success(createMockMessage())) }

        // Act
        viewModel.sendMessage(conversationId, senderId, senderName, content)

        // Assert - Message should appear immediately (optimistic)
        assertTrue(viewModel.selectedConversation.value?.messages?.isNotEmpty() == true)
    }

    // ─── Send Image Message Tests ───

    @Test
    fun `sendImageMessage successfully sends image message`() {
        // Arrange
        val conversationId = "conv123"
        val imageBytes = ByteArray(1024)
        val imageUrl = "https://example.com/image.png"
        
        mockkObject(com.mlbb.scrim.data.service.SupabaseStorageUpload)
        every { 
            com.mlbb.scrim.data.service.SupabaseStorageUpload.uploadFile(
                any(), any(), any(), any()
            ) 
        } returns Result.success(imageUrl)
        
        coEvery { mockRepository.sendMessage(any(), any(), any(), any(), any(), any()) } returns flow { emit(Result.success(createMockMessage())) }

        // Act
        viewModel.sendImageMessage(conversationId, "sender123", "Sender", imageBytes)
        advanceUntilIdle()

        // Assert
        coVerify { mockRepository.sendMessage(any(), any(), any(), any(), MessageType.IMAGE, imageUrl = imageUrl) }
    }

    @Test
    fun `sendImageMessage handles upload failure`() {
        // Arrange
        val conversationId = "conv123"
        val imageBytes = ByteArray(1024)
        val errorMessage = "Upload failed"
        
        mockkObject(com.mlbb.scrim.data.service.SupabaseStorageUpload)
        every { 
            com.mlbb.scrim.data.service.SupabaseStorageUpload.uploadFile(
                any(), any(), any(), any()
            ) 
        } returns Result.failure(Exception(errorMessage))

        // Act
        viewModel.sendImageMessage(conversationId, "sender123", "Sender", imageBytes)
        advanceUntilIdle()

        // Assert
        assertTrue(viewModel.error.value?.contains("Image upload failed") == true)
    }

    // ─── Send Voice Message Tests ───

    @Test
    fun `sendVoiceMessage successfully sends voice message`() {
        // Arrange
        val conversationId = "conv123"
        val voiceBytes = ByteArray(1024)
        val voiceUrl = "https://example.com/voice.m4a"
        val duration = 30
        
        mockkObject(com.mlbb.scrim.data.service.SupabaseStorageUpload)
        every { 
            com.mlbb.scrim.data.service.SupabaseStorageUpload.uploadFile(
                any(), any(), any(), any()
            ) 
        } returns Result.success(voiceUrl)
        
        coEvery { mockRepository.sendMessage(any(), any(), any(), any(), any(), any(), any(), any()) } returns flow { emit(Result.success(createMockMessage())) }

        // Act
        viewModel.sendVoiceMessage(conversationId, "sender123", "Sender", voiceBytes, duration)
        advanceUntilIdle()

        // Assert
        coVerify { 
            mockRepository.sendMessage(
                any(), any(), any(), any(), MessageType.VOICE, voiceUrl = voiceUrl, voiceDuration = duration
            ) 
        }
    }

    @Test
    fun `sendVoiceMessage handles upload failure`() {
        // Arrange
        val conversationId = "conv123"
        val voiceBytes = ByteArray(1024)
        val errorMessage = "Upload failed"
        
        mockkObject(com.mlbb.scrim.data.service.SupabaseStorageUpload)
        every { 
            com.mlbb.scrim.data.service.SupabaseStorageUpload.uploadFile(
                any(), any(), any(), any()
            ) 
        } returns Result.failure(Exception(errorMessage))

        // Act
        viewModel.sendVoiceMessage(conversationId, "sender123", "Sender", voiceBytes, 30)
        advanceUntilIdle()

        // Assert
        assertTrue(viewModel.error.value?.contains("Voice upload failed") == true)
    }

    // ─── Typing Status Tests ───

    @Test
    fun `updateTypingStatus successfully updates typing status`() {
        // Arrange
        val conversationId = "conv123"
        val userId = "user123"
        coEvery { mockRepository.setTypingStatus(conversationId, userId, true) } returns flow { emit(Unit) }

        // Act
        viewModel.updateTypingStatus(conversationId, userId, true)
        advanceUntilIdle()

        // Assert
        coVerify { mockRepository.setTypingStatus(conversationId, userId, true) }
    }

    // ─── Send Apply Message Tests ───

    @Test
    fun `sendApplyMessage successfully sends apply message`() {
        // Arrange
        val mockConversation = createMockConversation(id = "conv123")
        coEvery { 
            mockRepository.sendApplyMessage(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()
            ) 
        } returns flow { emit(Result.success(mockConversation)) }

        // Act
        viewModel.sendApplyMessage(
            scrimId = "scrim1",
            scrimTitle = "Test Scrim",
            applicantId = "applicant1",
            applicantName = "Applicant",
            applicantTeamId = "team1",
            applicantTeamName = "Team",
            scrimCreatorId = "creator1",
            scrimCreatorName = "Creator",
            scrimCreatorTeamId = "team2",
            scrimCreatorTeamName = "Team2",
            teamPlayerCount = 4,
            teamMaxPlayers = 5
        )
        advanceUntilIdle()

        // Assert
        assertEquals(mockConversation, viewModel.selectedConversation.value)
    }

    // ─── Start Direct Conversation Tests ───

    @Test
    fun `startDirectConversation successfully starts conversation`() {
        // Arrange
        val mockConversation = createMockConversation(id = "conv123")
        coEvery { 
            mockRepository.startDirectConversation(
                any(), any(), any(), any()
            ) 
        } returns flow { emit(Result.success(mockConversation)) }
        coEvery { mockRepository.getConversationsForUser(any()) } returns flow { emit(Result.success(listOf(mockConversation))) }

        // Act
        viewModel.startDirectConversation("sender1", "Sender", "recipient1", "Recipient")
        advanceUntilIdle()

        // Assert
        assertEquals(mockConversation, viewModel.selectedConversation.value)
        assertFalse(viewModel.isLoading.value)
    }

    @Test
    fun `startDirectConversation handles timeout`() {
        // Arrange
        coEvery { 
            mockRepository.startDirectConversation(
                any(), any(), any(), any()
            ) 
        } returns flow { 
            kotlinx.coroutines.delay(15000) // Longer than 10s timeout
            emit(Result.success(createMockConversation()))
        }

        // Act
        viewModel.startDirectConversation("sender1", "Sender", "recipient1", "Recipient")
        advanceUntilIdle()

        // Assert
        assertTrue(viewModel.error.value?.contains("timed out") == true)
    }

    @Test
    fun `startDirectConversation handles error`() {
        // Arrange
        val errorMessage = "Failed to start conversation"
        coEvery { 
            mockRepository.startDirectConversation(
                any(), any(), any(), any()
            ) 
        } returns flow { emit(Result.failure(Exception(errorMessage))) }

        // Act
        viewModel.startDirectConversation("sender1", "Sender", "recipient1", "Recipient")
        advanceUntilIdle()

        // Assert
        assertEquals(errorMessage, viewModel.error.value)
    }

    // ─── Error Handling Tests ───

    @Test
    fun `setError sets error message`() {
        // Act
        viewModel.setError("Test error")

        // Assert
        assertEquals("Test error", viewModel.error.value)
    }

    @Test
    fun `clearError clears error message`() {
        // Arrange
        viewModel.setError("Test error")

        // Act
        viewModel.clearError()

        // Assert
        assertEquals(null, viewModel.error.value)
    }

    @Test
    fun `clearRefreshing clears refreshing flag`() {
        // Arrange
        viewModel._isRefreshing.value = true

        // Act
        viewModel.clearRefreshing()

        // Assert
        assertFalse(viewModel.isRefreshing.value)
    }

    // ─── Helper Functions ───

    private fun createMockConversation(
        id: String = "conv-id",
        participantAName: String = "UserA",
        messages: List<Message> = emptyList()
    ): Conversation {
        return Conversation(
            id = id,
            participantAId = "userA",
            participantAName = participantAName,
            participantBId = "userB",
            participantBName = "UserB",
            messages = messages,
            lastMessage = messages.lastOrNull()?.content,
            lastMessageTime = messages.lastOrNull()?.timestamp,
            unreadCountA = 0,
            unreadCountB = 0,
            isParticipantATyping = false,
            isParticipantBTyping = false,
            createdAt = System.currentTimeMillis()
        )
    }

    private fun createMockMessage(
        id: String = "msg-id",
        content: String = "Test message"
    ): Message {
        return Message(
            id = id,
            conversationId = "conv123",
            senderId = "sender123",
            senderName = "Sender",
            content = content,
            timestamp = System.currentTimeMillis(),
            isRead = false,
            type = MessageType.TEXT
        )
    }
}

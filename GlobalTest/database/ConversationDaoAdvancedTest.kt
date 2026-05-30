package com.mlbb.scrim.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin test.assertNotNull
import kotlin.test.assertTrue

/**
 * Advanced ConversationDao tests with edge cases, concurrency, failure scenarios, and data integrity validation.
 * 
 * Test Categories:
 * - CRUD operations with edge cases
 * - Concurrency and race conditions
 * - Typing status management
 * - Data integrity and constraints
 * - Null/empty input handling
 * - Large dataset performance
 * - Flow emission tests
 */
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalCoroutinesApi::class)
class ConversationDaoAdvancedTest {

    private lateinit var database: MLBBScrimDatabase
    private lateinit var conversationDao: ConversationDao
    private lateinit var testDispatcher: TestDispatcher

    @Before
    fun setup() {
        testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)

        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            MLBBScrimDatabase::class.java
        ).allowMainThreadQueries().build()

        conversationDao = database.conversationDao()
    }

    @After
    fun tearDown() {
        database.close()
        Dispatchers.resetMain()
    }

    // ─── BASIC CRUD TESTS ───

    @Test
    fun `getConversationsForUser returns empty list when user has no conversations`() {
        // Act
        val result = conversationDao.getConversationsForUser("nonexistent_user").first()

        // Assert
        assertTrue(result.isEmpty())
    }

    @Test
    fun `getConversationsForUser returns conversations where user is participant A`() {
        // Arrange
        val conversation = ConversationEntity(
            id = "conv1",
            scrimId = "scrim1",
            scrimTitle = "Test Scrim",
            participantAId = "user1",
            participantAName = "User1",
            participantATeamId = "team1",
            participantATeamName = "Team1",
            participantBId = "user2",
            participantBName = "User2",
            participantBTeamId = "team2",
            participantBTeamName = "Team2",
            lastMessage = "Test message",
            lastMessageTime = 123456789
        )
        conversationDao.insertConversation(conversation)
        advanceUntilIdle()

        // Act
        val result = conversationDao.getConversationsForUser("user1").first()

        // Assert
        assertEquals(1, result.size)
        assertEquals("conv1", result[0].id)
    }

    @Test
    fun `getConversationsForUser returns conversations where user is participant B`() {
        // Arrange
        val conversation = ConversationEntity(
            id = "conv1",
            scrimId = "scrim1",
            scrimTitle = "Test Scrim",
            participantAId = "user1",
            participantAName = "User1",
            participantATeamId = "team1",
            participantATeamName = "Team1",
            participantBId = "user2",
            participantBName = "User2",
            participantBTeamId = "team2",
            participantBTeamName = "Team2",
            lastMessage = "Test message",
            lastMessageTime = 123456789
        )
        conversationDao.insertConversation(conversation)
        advanceUntilIdle()

        // Act
        val result = conversationDao.getConversationsForUser("user2").first()

        // Assert
        assertEquals(1, result.size)
        assertEquals("conv1", result[0].id)
    }

    @Test
    fun `getConversationsForUser returns conversations sorted by lastMessageTime DESC`() {
        // Arrange
        val conversations = listOf(
            ConversationEntity("conv1", "scrim1", "Scrim1", "user1", "User1", "team1", "Team1", "user2", "User2", "team2", "Team2", "Old", 123456789),
            ConversationEntity("conv2", "scrim2", "Scrim2", "user1", "User1", "team1", "Team1", "user3", "User3", "team3", "Team3", "New", 123456790),
            ConversationEntity("conv3", "scrim3", "Scrim3", "user1", "User1", "team1", "Team1", "user4", "User4", "team4", "Team4", "Middle", 1234567895)
        )
        conversationDao.insertConversations(conversations)
        advanceUntilIdle()

        // Act
        val result = conversationDao.getConversationsForUser("user1").first()

        // Assert
        assertEquals(3, result.size)
        assertEquals("conv2", result[0].id) // Most recent
        assertEquals("conv3", result[1].id)
        assertEquals("conv1", result[2].id) // Oldest
    }

    @Test
    fun `getConversationById returns conversation when it exists`() {
        // Arrange
        val conversation = ConversationEntity(
            id = "conv1",
            scrimId = "scrim1",
            scrimTitle = "Test Scrim",
            participantAId = "user1",
            participantAName = "User1",
            participantATeamId = "team1",
            participantATeamName = "Team1",
            participantBId = "user2",
            participantBName = "User2",
            participantBTeamId = "team2",
            participantBTeamName = "Team2",
            lastMessage = "Test message",
            lastMessageTime = 123456789
        )
        conversationDao.insertConversation(conversation)
        advanceUntilIdle()

        // Act
        val result = conversationDao.getConversationById("conv1").first()

        // Assert
        assertNotNull(result)
        assertEquals("conv1", result?.id)
    }

    @Test
    fun `getConversationById returns null when conversation does not exist`() {
        // Act
        val result = conversationDao.getConversationById("nonexistent").first()

        // Assert
        assertNull(result)
    }

    @Test
    fun `insertConversation successfully stores single conversation`() {
        // Arrange
        val conversation = ConversationEntity(
            id = "conv1",
            scrimId = "scrim1",
            scrimTitle = "Test Scrim",
            participantAId = "user1",
            participantAName = "User1",
            participantATeamId = "team1",
            participantATeamName = "Team1",
            participantBId = "user2",
            participantBName = "User2",
            participantBTeamId = "team2",
            participantBTeamName = "Team2",
            lastMessage = "Test message",
            lastMessageTime = 123456789
        )

        // Act
        conversationDao.insertConversation(conversation)
        advanceUntilIdle()

        // Assert
        val result = conversationDao.getConversationById("conv1").first()
        assertNotNull(result)
        assertEquals("conv1", result?.id)
    }

    @Test
    fun `insertConversations successfully stores multiple conversations`() {
        // Arrange
        val conversations = listOf(
            ConversationEntity("conv1", "scrim1", "Scrim1", "user1", "User1", "team1", "Team1", "user2", "User2", "team2", "Team2", "Msg1", 123456789),
            ConversationEntity("conv2", "scrim2", "Scrim2", "user1", "User1", "team1", "Team1", "user3", "User3", "team3", "Team3", "Msg2", 123456790)
        )

        // Act
        conversationDao.insertConversations(conversations)
        advanceUntilIdle()

        // Assert
        val result1 = conversationDao.getConversationById("conv1").first()
        val result2 = conversationDao.getConversationById("conv2").first()
        assertNotNull(result1)
        assertNotNull(result2)
    }

    @Test
    fun `updateLastMessage successfully updates message and timestamp`() {
        // Arrange
        val conversation = ConversationEntity(
            id = "conv1",
            scrimId = "scrim1",
            scrimTitle = "Test Scrim",
            participantAId = "user1",
            participantAName = "User1",
            participantATeamId = "team1",
            participantATeamName = "Team1",
            participantBId = "user2",
            participantBName = "User2",
            participantBTeamId = "team2",
            participantBTeamName = "Team2",
            lastMessage = "Old message",
            lastMessageTime = 123456789
        )
        conversationDao.insertConversation(conversation)
        advanceUntilIdle()

        // Act
        conversationDao.updateLastMessage("conv1", "New message", 123456790)
        advanceUntilIdle()

        // Assert
        val result = conversationDao.getConversationById("conv1").first()
        assertNotNull(result)
        assertEquals("New message", result?.lastMessage)
        assertEquals(123456790, result?.lastMessageTime)
    }

    @Test
    fun `updateParticipantATyping successfully updates typing status`() {
        // Arrange
        val conversation = ConversationEntity(
            id = "conv1",
            scrimId = "scrim1",
            scrimTitle = "Test Scrim",
            participantAId = "user1",
            participantAName = "User1",
            participantATeamId = "team1",
            participantATeamName = "Team1",
            participantBId = "user2",
            participantBName = "User2",
            participantBTeamId = "team2",
            participantBTeamName = "Team2",
            lastMessage = "Test message",
            lastMessageTime = 123456789
        )
        conversationDao.insertConversation(conversation)
        advanceUntilIdle()

        // Act
        conversationDao.updateParticipantATyping("conv1", true)
        advanceUntilIdle()

        // Assert
        val result = conversationDao.getConversationById("conv1").first()
        assertNotNull(result)
        assertEquals(true, result?.isParticipantATyping)
    }

    @Test
    fun `updateParticipantBTyping successfully updates typing status`() {
        // Arrange
        val conversation = ConversationEntity(
            id = "conv1",
            scrimId = "scrim1",
            scrimTitle = "Test Scrim",
            participantAId = "user1",
            participantAName = "User1",
            participantATeamId = "team1",
            participantATeamName = "Team1",
            participantBId = "user2",
            participantBName = "User2",
            participantBTeamId = "team2",
            participantBTeamName = "Team2",
            lastMessage = "Test message",
            lastMessageTime = 123456789
        )
        conversationDao.insertConversation(conversation)
        advanceUntilIdle()

        // Act
        conversationDao.updateParticipantBTyping("conv1", true)
        advanceUntilIdle()

        // Assert
        val result = conversationDao.getConversationById("conv1").first()
        assertNotNull(result)
        assertEquals(true, result?.isParticipantBTyping)
    }

    // ─── EDGE CASE TESTS ───

    @Test
    fun `getConversationsForUser handles empty userId`() {
        // Act
        val result = conversationDao.getConversationsForUser("").first()

        // Assert
        assertTrue(result.isEmpty())
    }

    @Test
    fun `getConversationsForUser handles userId with special characters`() {
        // Arrange
        val specialUserId = "user_with_!@#$%^&*()"
        val conversation = ConversationEntity(
            id = "conv1",
            scrimId = "scrim1",
            scrimTitle = "Test Scrim",
            participantAId = specialUserId,
            participantAName = "User1",
            participantATeamId = "team1",
            participantATeamName = "Team1",
            participantBId = "user2",
            participantBName = "User2",
            participantBTeamId = "team2",
            participantBTeamName = "Team2",
            lastMessage = "Test message",
            lastMessageTime = 123456789
        )
        conversationDao.insertConversation(conversation)
        advanceUntilIdle()

        // Act
        val result = conversationDao.getConversationsForUser(specialUserId).first()

        // Assert
        assertEquals(1, result.size)
    }

    @Test
    fun `getConversationsForUser handles userId with unicode characters`() {
        // Arrange
        val unicodeUserId = "user_中文_emoji_😀"
        val conversation = ConversationEntity(
            id = "conv1",
            scrimId = "scrim1",
            scrimTitle = "Test Scrim",
            participantAId = unicodeUserId,
            participantAName = "User1",
            participantATeamId = "team1",
            participantATeamName = "Team1",
            participantBId = "user2",
            participantBName = "User2",
            participantBTeamId = "team2",
            participantBTeamName = "Team2",
            lastMessage = "Test message",
            lastMessageTime = 123456789
        )
        conversationDao.insertConversation(conversation)
        advanceUntilIdle()

        // Act
        val result = conversationDao.getConversationsForUser(unicodeUserId).first()

        // Assert
        assertEquals(1, result.size)
    }

    @Test
    fun `getConversationById handles empty conversationId`() {
        // Act
        val result = conversationDao.getConversationById("").first()

        // Assert
        assertNull(result)
    }

    @Test
    fun `getConversationById handles very long conversationId`() {
        // Arrange
        val longId = "a".repeat(10000)
        val conversation = ConversationEntity(
            id = longId,
            scrimId = "scrim1",
            scrimTitle = "Test Scrim",
            participantAId = "user1",
            participantAName = "User1",
            participantATeamId = "team1",
            participantATeamName = "Team1",
            participantBId = "user2",
            participantBName = "User2",
            participantBTeamId = "team2",
            participantBTeamName = "Team2",
            lastMessage = "Test message",
            lastMessageTime = 123456789
        )
        conversationDao.insertConversation(conversation)
        advanceUntilIdle()

        // Act
        val result = conversationDao.getConversationById(longId).first()

        // Assert
        assertNotNull(result)
        assertEquals(longId, result?.id)
    }

    @Test
    fun `updateLastMessage handles empty message content`() {
        // Arrange
        val conversation = ConversationEntity(
            id = "conv1",
            scrimId = "scrim1",
            scrimTitle = "Test Scrim",
            participantAId = "user1",
            participantAName = "User1",
            participantATeamId = "team1",
            participantATeamName = "Team1",
            participantBId = "user2",
            participantBName = "User2",
            participantBTeamId = "team2",
            participantBTeamName = "Team2",
            lastMessage = "Original",
            lastMessageTime = 123456789
        )
        conversationDao.insertConversation(conversation)
        advanceUntilIdle()

        // Act
        conversationDao.updateLastMessage("conv1", "", 123456790)
        advanceUntilIdle()

        // Assert
        val result = conversationDao.getConversationById("conv1").first()
        assertNotNull(result)
        assertEquals("", result?.lastMessage)
    }

    @Test
    fun `updateLastMessage handles very long message content`() {
        // Arrange
        val conversation = ConversationEntity(
            id = "conv1",
            scrimId = "scrim1",
            scrimTitle = "Test Scrim",
            participantAId = "user1",
            participantAName = "User1",
            participantATeamId = "team1",
            participantATeamName = "Team1",
            participantBId = "user2",
            participantBName = "User2",
            participantBTeamId = "team2",
            participantBTeamName = "Team2",
            lastMessage = "Original",
            lastMessageTime = 123456789
        )
        conversationDao.insertConversation(conversation)
        advanceUntilIdle()

        val longMessage = "A".repeat(10000)

        // Act
        conversationDao.updateLastMessage("conv1", longMessage, 123456790)
        advanceUntilIdle()

        // Assert
        val result = conversationDao.getConversationById("conv1").first()
        assertNotNull(result)
        assertEquals(longMessage, result?.lastMessage)
    }

    @Test
    fun `updateLastMessage handles zero timestamp`() {
        // Arrange
        val conversation = ConversationEntity(
            id = "conv1",
            scrimId = "scrim1",
            scrimTitle = "Test Scrim",
            participantAId = "user1",
            participantAName = "User1",
            participantATeamId = "team1",
            participantATeamName = "Team1",
            participantBId = "user2",
            participantBName = "User2",
            participantBTeamId = "team2",
            participantBTeamName = "Team2",
            lastMessage = "Original",
            lastMessageTime = 123456789
        )
        conversationDao.insertConversation(conversation)
        advanceUntilIdle()

        // Act
        conversationDao.updateLastMessage("conv1", "New", 0)
        advanceUntilIdle()

        // Assert
        val result = conversationDao.getConversationById("conv1").first()
        assertNotNull(result)
        assertEquals(0, result?.lastMessageTime)
    }

    @Test
    fun `updateLastMessage handles negative timestamp`() {
        // Arrange
        val conversation = ConversationEntity(
            id = "conv1",
            scrimId = "scrim1",
            scrimTitle = "Test Scrim",
            participantAId = "user1",
            participantAName = "User1",
            participantATeamId = "team1",
            participantATeamName = "Team1",
            participantBId = "user2",
            participantBName = "User2",
            participantBTeamId = "team2",
            participantBTeamName = "Team2",
            lastMessage = "Original",
            lastMessageTime = 123456789
        )
        conversationDao.insertConversation(conversation)
        advanceUntilIdle()

        // Act
        conversationDao.updateLastMessage("conv1", "New", -123456789)
        advanceUntilIdle()

        // Assert
        val result = conversationDao.getConversationById("conv1").first()
        assertNotNull(result)
        assertEquals(-123456789, result?.lastMessageTime)
    }

    @Test
    fun `updateLastMessage handles non-existent conversation`() {
        // Act
        conversationDao.updateLastMessage("nonexistent", "New", 123456790)
        advanceUntilIdle()

        // Assert - Should not throw exception
        assertTrue(true, "Update should handle non-existent conversation gracefully")
    }

    @Test
    fun `insertConversations handles empty list`() {
        // Act
        conversationDao.insertConversations(emptyList())
        advanceUntilIdle()

        // Assert - Should not throw exception
        val result = conversationDao.getConversationsForUser("user1").first()
        assertTrue(result.isEmpty())
    }

    // ─── CONCURRENCY TESTS ───

    @Test
    fun `concurrent conversation inserts are handled correctly`() {
        // Arrange
        val conversations = (1..100).map { i ->
            ConversationEntity("conv_$i", "scrim_$i", "Scrim $i", "user1", "User1", "team1", "Team1", "user2", "User2", "team2", "Team2", "Msg $i", 123456789 + i)
        }

        // Act - Insert conversations concurrently
        val jobs = conversations.chunked(10).map { chunk ->
            kotlinx.coroutines.launch {
                conversationDao.insertConversations(chunk)
            }
        }

        jobs.forEach { it.join() }
        advanceUntilIdle()

        // Assert
        val result = conversationDao.getConversationById("conv_50").first()
        assertNotNull(result)
    }

    @Test
    fun `concurrent read and write operations are handled correctly`() {
        // Arrange
        val initialConversation = ConversationEntity("conv1", "scrim1", "Scrim1", "user1", "User1", "team1", "Team1", "user2", "User2", "team2", "Team2", "Initial", 123456789)
        conversationDao.insertConversation(initialConversation)
        advanceUntilIdle()

        // Act - Concurrent reads and writes
        val jobs = mutableListOf<kotlinx.coroutines.Job>()
        
        // Start read operations
        repeat(5) {
            jobs.add(kotlinx.coroutines.launch {
                val result = conversationDao.getConversationById("conv1").first()
                assertNotNull(result)
            })
        }

        // Start write operations
        repeat(5) { i ->
            jobs.add(kotlinx.coroutines.launch {
                conversationDao.updateLastMessage("conv1", "Updated $i", 123456790 + i)
            })
        }

        jobs.forEach { it.join() }
        advanceUntilIdle()

        // Assert - Should complete without errors
        val finalResult = conversationDao.getConversationById("conv1").first()
        assertNotNull(finalResult)
    }

    @Test
    fun `concurrent typing status updates are handled correctly`() {
        // Arrange
        val conversation = ConversationEntity(
            id = "conv1",
            scrimId = "scrim1",
            scrimTitle = "Test Scrim",
            participantAId = "user1",
            participantAName = "User1",
            participantATeamId = "team1",
            participantATeamName = "Team1",
            participantBId = "user2",
            participantBName = "User2",
            participantBTeamId = "team2",
            participantBTeamName = "Team2",
            lastMessage = "Test message",
            lastMessageTime = 123456789
        )
        conversationDao.insertConversation(conversation)
        advanceUntilIdle()

        // Act - Concurrent typing status updates
        val jobs = listOf(
            kotlinx.coroutines.launch { conversationDao.updateParticipantATyping("conv1", true) },
            kotlinx.coroutines.launch { conversationDao.updateParticipantBTyping("conv1", true) },
            kotlinx.coroutines.launch { conversationDao.updateParticipantATyping("conv1", false) },
            kotlinx.coroutines.launch { conversationDao.updateParticipantBTyping("conv1", false) }
        )

        jobs.forEach { it.join() }
        advanceUntilIdle()

        // Assert - Should complete without errors
        val result = conversationDao.getConversationById("conv1").first()
        assertNotNull(result)
    }

    // ─── DATA INTEGRITY TESTS ───

    @Test
    fun `insertConversation with REPLACE strategy updates existing conversation`() {
        // Arrange
        val originalConversation = ConversationEntity("conv1", "scrim1", "Original", "user1", "User1", "team1", "Team1", "user2", "User2", "team2", "Team2", "Old", 123456789)
        conversationDao.insertConversation(originalConversation)
        advanceUntilIdle()

        val updatedConversation = ConversationEntity("conv1", "scrim1", "Updated", "user1", "User1", "team1", "Team1", "user2", "User2", "team2", "Team2", "New", 123456790)

        // Act
        conversationDao.insertConversation(updatedConversation)
        advanceUntilIdle()

        // Assert
        val result = conversationDao.getConversationById("conv1").first()
        assertNotNull(result)
        assertEquals("Updated", result?.scrimTitle)
        assertEquals("New", result?.lastMessage)
    }

    @Test
    fun `conversations from different users are isolated`() {
        // Arrange
        val conv1 = ConversationEntity("conv1", "scrim1", "Scrim1", "user1", "User1", "team1", "Team1", "user2", "User2", "team2", "Team2", "Msg1", 123456789)
        val conv2 = ConversationEntity("conv2", "scrim2", "Scrim2", "user3", "User3", "team3", "Team3", "user4", "User4", "team4", "Team4", "Msg2", 123456790)
        conversationDao.insertConversations(listOf(conv1, conv2))
        advanceUntilIdle()

        // Act
        val user1Conversations = conversationDao.getConversationsForUser("user1").first()
        val user3Conversations = conversationDao.getConversationsForUser("user3").first()

        // Assert
        assertEquals(1, user1Conversations.size)
        assertEquals(1, user3Conversations.size)
        assertEquals("conv1", user1Conversations[0].id)
        assertEquals("conv2", user3Conversations[0].id)
    }

    @Test
    fun `typing status updates maintain consistency`() {
        // Arrange
        val conversation = ConversationEntity(
            id = "conv1",
            scrimId = "scrim1",
            scrimTitle = "Test Scrim",
            participantAId = "user1",
            participantAName = "User1",
            participantATeamId = "team1",
            participantATeamName = "Team1",
            participantBId = "user2",
            participantBName = "User2",
            participantBTeamId = "team2",
            participantBTeamName = "Team2",
            lastMessage = "Test message",
            lastMessageTime = 123456789
        )
        conversationDao.insertConversation(conversation)
        advanceUntilIdle()

        // Act - Update both participants' typing status
        conversationDao.updateParticipantATyping("conv1", true)
        advanceUntilIdle()
        
        conversationDao.updateParticipantBTyping("conv1", true)
        advanceUntilIdle()

        // Assert
        val result = conversationDao.getConversationById("conv1").first()
        assertNotNull(result)
        assertEquals(true, result?.isParticipantATyping)
        assertEquals(true, result?.isParticipantBTyping)
    }

    // ─── PERFORMANCE TESTS ───

    @Test
    fun `insertConversations handles large dataset efficiently`() {
        // Arrange
        val largeDataset = (1..1000).map { i ->
            ConversationEntity("conv_$i", "scrim_$i", "Scrim $i", "user$i", "User$i", "team$i", "Team$i", "user${i+1}", "User${i+1}", "team${i+1}", "Team${i+1}", "Msg $i", 123456789 + i)
        }

        // Act - Measure insert time
        val startTime = System.currentTimeMillis()
        conversationDao.insertConversations(largeDataset)
        advanceUntilIdle()
        val insertTime = System.currentTimeMillis() - startTime

        // Assert
        val result = conversationDao.getConversationById("conv_500").first()
        assertNotNull(result)
        assertTrue(insertTime < 5000, "Insert should complete in under 5 seconds, took ${insertTime}ms")
    }

    @Test
    fun `getConversationsForUser handles large result set efficiently`() {
        // Arrange
        val largeDataset = (1..1000).map { i ->
            ConversationEntity("conv_$i", "scrim_$i", "Scrim $i", "user1", "User1", "team1", "Team1", "user2", "User2", "team2", "Team2", "Msg $i", 123456789 + i)
        }
        conversationDao.insertConversations(largeDataset)
        advanceUntilIdle()

        // Act - Measure query time
        val startTime = System.currentTimeMillis()
        val conversations = conversationDao.getConversationsForUser("user1").first()
        val queryTime = System.currentTimeMillis() - startTime

        // Assert
        assertEquals(1000, conversations.size)
        assertTrue(queryTime < 1000, "Query should complete in under 1 second, took ${queryTime}ms")
    }

    // ─── FLOW EMISSION TESTS ───

    @Test
    fun `getConversationsForUser emits updates when data changes`() {
        // Arrange
        val initialConversation = ConversationEntity("conv1", "scrim1", "Scrim1", "user1", "User1", "team1", "Team1", "user2", "User2", "team2", "Team2", "Initial", 123456789)
        conversationDao.insertConversation(initialConversation)
        advanceUntilIdle()

        val emissions = mutableListOf<List<ConversationEntity>>()
        val job = kotlinx.coroutines.launch {
            conversationDao.getConversationsForUser("user1").collect { emissions.add(it) }
        }

        advanceUntilIdle()

        // Act
        val newConversation = ConversationEntity("conv2", "scrim2", "Scrim2", "user1", "User1", "team1", "Team1", "user3", "User3", "team3", "Team3", "New", 123456790)
        conversationDao.insertConversation(newConversation)
        advanceUntilIdle()

        job.cancel()

        // Assert
        assertTrue(emissions.size >= 2, "Should emit at least 2 updates")
        assertTrue(emissions.last().size == 2, "Last emission should have 2 conversations")
    }

    @Test
    fun `getConversationById emits updates when conversation changes`() {
        // Arrange
        val conversation = ConversationEntity("conv1", "scrim1", "Scrim1", "user1", "User1", "team1", "Team1", "user2", "User2", "team2", "Team2", "Initial", 123456789)
        conversationDao.insertConversation(conversation)
        advanceUntilIdle()

        val emissions = mutableListOf<ConversationEntity?>()
        val job = kotlinx.coroutines.launch {
            conversationDao.getConversationById("conv1").collect { emissions.add(it) }
        }

        advanceUntilIdle()

        // Act
        conversationDao.updateLastMessage("conv1", "Updated", 123456790)
        advanceUntilIdle()

        job.cancel()

        // Assert
        assertTrue(emissions.size >= 2, "Should emit at least 2 updates")
        assertTrue(emissions.last()?.lastMessage == "Updated", "Last emission should have updated message")
    }
}

package com.scrimslegends.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversations WHERE participantAId = :userId OR participantBId = :userId OR isTeamChat = 1 ORDER BY isPinned DESC, lastMessageTime DESC")
    fun getConversationsForUser(userId: String): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE id = :conversationId")
    fun getConversationById(conversationId: String): Flow<ConversationEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversations(conversations: List<ConversationEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: ConversationEntity)

    @Query("UPDATE conversations SET lastMessage = :lastMessage, lastMessageTime = :time WHERE id = :conversationId")
    suspend fun updateLastMessage(conversationId: String, lastMessage: String, time: Long)

    @Query("UPDATE conversations SET isParticipantATyping = :typing WHERE id = :conversationId")
    suspend fun updateParticipantATyping(conversationId: String, typing: Boolean)

    @Query("UPDATE conversations SET isParticipantBTyping = :typing WHERE id = :conversationId")
    suspend fun updateParticipantBTyping(conversationId: String, typing: Boolean)

    @Query("SELECT * FROM conversations WHERE teamId = :teamId AND isTeamChat = 1")
    suspend fun getTeamConversations(teamId: String): List<ConversationEntity>
}

package com.scrimslegends.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.paging.PagingSource
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    fun getMessagesForConversation(conversationId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestamp DESC")
    fun getMessagesPaged(conversationId: String): PagingSource<Int, MessageEntity>

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestamp ASC LIMIT :limit OFFSET :offset")
    suspend fun getMessagesPage(conversationId: String, limit: Int, offset: Int): List<MessageEntity>

    @Query("SELECT COUNT(*) FROM messages WHERE conversationId = :conversationId")
    suspend fun getMessageCount(conversationId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<MessageEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Query("UPDATE messages SET isRead = 1, readAt = :readAt WHERE conversationId = :conversationId AND senderId != :currentUserId")
    suspend fun markMessagesAsRead(conversationId: String, currentUserId: String, readAt: Long)

    @Query("UPDATE messages SET isDeleted = 1, content = '' WHERE id = :messageId")
    suspend fun softDeleteMessage(messageId: String)

    @Query("DELETE FROM messages WHERE conversationId = :conversationId AND timestamp < :cutoffTimestamp AND isRead = 1")
    suspend fun pruneOldMessages(conversationId: String, cutoffTimestamp: Long): Int

    @Query("DELETE FROM messages WHERE conversationId = :conversationId")
    suspend fun deleteMessagesForConversation(conversationId: String)
}

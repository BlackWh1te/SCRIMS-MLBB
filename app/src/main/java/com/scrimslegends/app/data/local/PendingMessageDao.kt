package com.scrimslegends.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingMessageDao {
    @Query("SELECT * FROM pending_messages WHERE status IN ('PENDING','SENDING','FAILED') ORDER BY createdAt ASC")
    fun getPendingMessages(): Flow<List<PendingMessageEntity>>

    @Query("SELECT * FROM pending_messages WHERE conversationId = :conversationId AND status IN ('PENDING','SENDING','FAILED') ORDER BY createdAt ASC")
    fun getPendingMessagesForConversation(conversationId: String): Flow<List<PendingMessageEntity>>

    @Query(
        "SELECT * FROM pending_messages WHERE " +
        "(status IN ('PENDING','FAILED') AND nextRetryAt <= :now) " +
        "OR (status = 'SENDING' AND createdAt < :staleThreshold) " +
        "ORDER BY createdAt ASC"
    )
    suspend fun getMessagesReadyForRetry(
        now: Long = System.currentTimeMillis(),
        staleThreshold: Long = System.currentTimeMillis() - 300_000
    ): List<PendingMessageEntity>

    @Query("SELECT * FROM pending_messages WHERE clientMessageId = :clientMessageId LIMIT 1")
    suspend fun getByClientId(clientMessageId: String): PendingMessageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: PendingMessageEntity)

    @Query("UPDATE pending_messages SET status = :status WHERE clientMessageId = :clientMessageId")
    suspend fun updateStatus(clientMessageId: String, status: String)

    @Query("UPDATE pending_messages SET status = :status, retryCount = retryCount + 1, nextRetryAt = :nextRetryAt WHERE clientMessageId = :clientMessageId")
    suspend fun markRetry(clientMessageId: String, status: String, nextRetryAt: Long)

    @Query("UPDATE pending_messages SET status = 'FAILED', failedAt = :now, errorReason = :reason WHERE clientMessageId = :clientMessageId")
    suspend fun markFailed(clientMessageId: String, reason: String, now: Long = System.currentTimeMillis())

    @Query("DELETE FROM pending_messages WHERE clientMessageId = :clientMessageId")
    suspend fun delete(clientMessageId: String)

    @Query("DELETE FROM pending_messages WHERE status = 'SENT' AND createdAt < :cutoff")
    suspend fun pruneSent(cutoff: Long)

    @Query("SELECT COUNT(*) FROM pending_messages WHERE status IN ('PENDING','SENDING','FAILED')")
    suspend fun pendingCount(): Int
}

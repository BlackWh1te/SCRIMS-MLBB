package com.scrimslegends.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface MessageOutboxDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOrIgnore(outboxEntity: MessageOutboxEntity): Long

    @Query("SELECT * FROM message_outbox WHERE status IN ('PENDING', 'RETRYING') AND nextRetryAt <= :currentTime ORDER BY createdAt ASC LIMIT :limit")
    suspend fun getPendingMessages(currentTime: Long = System.currentTimeMillis(), limit: Int = 50): List<MessageOutboxEntity>

    @Query("UPDATE message_outbox SET status = 'SENT' WHERE clientMessageId = :clientMessageId")
    suspend fun markAsSent(clientMessageId: String)

    @Query("UPDATE message_outbox SET status = 'FAILED_PERMANENT' WHERE clientMessageId = :clientMessageId")
    suspend fun markAsFailedPermanent(clientMessageId: String)

    @Query("UPDATE message_outbox SET status = 'RETRYING', retryCount = retryCount + 1, nextRetryAt = :nextRetryAt WHERE clientMessageId = :clientMessageId")
    suspend fun updateRetrySchedule(clientMessageId: String, nextRetryAt: Long)

    @Query("DELETE FROM message_outbox WHERE status = 'SENT'")
    suspend fun purgeSentMessages()
}

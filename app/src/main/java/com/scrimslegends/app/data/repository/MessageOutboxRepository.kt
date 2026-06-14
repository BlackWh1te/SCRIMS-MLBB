package com.scrimslegends.app.data.repository

import com.scrimslegends.app.data.local.MessageOutboxDao
import com.scrimslegends.app.data.local.MessageOutboxEntity
import javax.inject.Inject
import javax.inject.Singleton

interface MessageOutboxRepositoryInterface {
    suspend fun enqueueMessage(entity: MessageOutboxEntity)
    suspend fun getPendingMessages(limit: Int = 50): List<MessageOutboxEntity>
    suspend fun markAsSent(clientMessageId: String)
    suspend fun markAsFailedPermanent(clientMessageId: String)
    suspend fun updateRetrySchedule(clientMessageId: String, nextRetryAt: Long)
    suspend fun purgeSentMessages()
}

@Singleton
class MessageOutboxRepository @Inject constructor(
    private val dao: MessageOutboxDao
) : MessageOutboxRepositoryInterface {
    override suspend fun enqueueMessage(entity: MessageOutboxEntity) {
        dao.insertOrIgnore(entity)
    }

    override suspend fun getPendingMessages(limit: Int): List<MessageOutboxEntity> {
        return dao.getPendingMessages(System.currentTimeMillis(), limit)
    }

    override suspend fun markAsSent(clientMessageId: String) {
        dao.markAsSent(clientMessageId)
    }

    override suspend fun markAsFailedPermanent(clientMessageId: String) {
        dao.markAsFailedPermanent(clientMessageId)
    }

    override suspend fun updateRetrySchedule(clientMessageId: String, nextRetryAt: Long) {
        dao.updateRetrySchedule(clientMessageId, nextRetryAt)
    }

    override suspend fun purgeSentMessages() {
        dao.purgeSentMessages()
    }
}

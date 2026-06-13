package com.scrimslegends.app.domain.usecase

import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.scrimslegends.app.data.local.MessageOutboxEntity
import com.scrimslegends.app.data.repository.MessageOutboxRepositoryInterface
import com.scrimslegends.app.worker.OfflineMessageWorker
import javax.inject.Inject

class EnqueueOfflineMessageUseCase @Inject constructor(
    private val outboxRepository: MessageOutboxRepositoryInterface,
    private val workManager: WorkManager
) {
    suspend operator fun invoke(
        conversationId: String,
        content: String,
        clientMessageId: String,
        isTeamMessage: Boolean = false,
        isScrimMessage: Boolean = false
    ) {
        // 1. Write to outbox (Room INSERT IGNORE)
        val entity = MessageOutboxEntity(
            clientMessageId = clientMessageId,
            conversationId = conversationId,
            content = content,
            isTeamMessage = isTeamMessage,
            isScrimMessage = isScrimMessage
        )
        outboxRepository.enqueueMessage(entity)

        // 2. Schedule worker to drain the outbox
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = OneTimeWorkRequestBuilder<OfflineMessageWorker>()
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniqueWork(
            "OfflineMessageWorker_Immediate",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }
}

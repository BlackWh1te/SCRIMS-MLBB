package com.scrimslegends.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.scrimslegends.app.data.repository.MessageRepositoryInterface
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

/**
 * Periodic WorkManager worker that drains the message outbox.
 *
 * Runs on battery-unconstrained, network-connected constraints.
 * Exponential backoff on failure.
 */
@HiltWorker
class MessageSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val messageRepository: MessageRepositoryInterface
) : CoroutineWorker(context, params) {

    companion object {
        const val TAG = "MessageSyncWorker"
        const val WORK_NAME = "message_outbox_sync"
    }

    override suspend fun doWork(): Result {
        return try {
            val result = messageRepository.syncOutbox()
            result.fold(
                onSuccess = { count ->
                    Timber.d(TAG, "Synced $count messages from outbox")
                    Result.success()
                },
                onFailure = { e ->
                    Timber.e(TAG, "Outbox sync failed", e)
                    Result.retry()
                }
            )
        } catch (e: Exception) {
            Timber.e(TAG, "Worker crashed", e)
            Result.retry()
        }
    }
}

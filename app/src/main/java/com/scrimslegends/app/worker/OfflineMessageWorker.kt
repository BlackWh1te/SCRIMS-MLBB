package com.scrimslegends.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.scrimslegends.app.data.repository.MessageOutboxRepositoryInterface
import com.scrimslegends.app.data.service.SupabaseApiService
import com.scrimslegends.app.data.service.SupabaseService
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

@HiltWorker
class OfflineMessageWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val outboxRepository: MessageOutboxRepositoryInterface
) : CoroutineWorker(context, workerParams) {

    private val api: SupabaseApiService = SupabaseService.api

    override suspend fun doWork(): Result {
        val pendingMessages = outboxRepository.getPendingMessages()
        if (pendingMessages.isEmpty()) {
            return Result.success()
        }

        var hasFailures = false

        for (msg in pendingMessages) {
            try {
                // Determine whether it's a team chat, scrim chat, or standard conversation
                val params = mutableMapOf<String, Any>(
                    "p_conversation_id" to msg.conversationId,
                    "p_content" to msg.content,
                    "p_client_message_id" to msg.clientMessageId
                )
                
                if (msg.isTeamMessage) {
                    params["p_is_team_chat"] = true
                }
                if (msg.isScrimMessage) {
                    params["p_is_scrim_chat"] = true
                }

                val response = api.rpcSendMessageSecure(params)

                if (response.isSuccessful) {
                    outboxRepository.markAsSent(msg.clientMessageId)
                } else {
                    val code = response.code()
                    // 409 Conflict could mean Supabase UNIQUE(client_message_id) constraint triggered,
                    // which means the message was actually already delivered.
                    if (code == 409 || response.errorBody()?.string()?.contains("duplicate key") == true) {
                        outboxRepository.markAsSent(msg.clientMessageId)
                    } else {
                        handleRetry(msg)
                        hasFailures = true
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Offline worker failed to send message")
                handleRetry(msg)
                hasFailures = true
            }
        }

        outboxRepository.purgeSentMessages()

        return if (hasFailures) Result.retry() else Result.success()
    }

    private suspend fun handleRetry(msg: com.scrimslegends.app.data.local.MessageOutboxEntity) {
        // Exponential backoff: 5s, 10s, 20s, 40s, 80s
        if (msg.retryCount >= 5) {
            outboxRepository.markAsFailedPermanent(msg.clientMessageId)
        } else {
            val nextDelay = 5000L * (1 shl msg.retryCount) // 5s, 10s, 20s, 40s, 80s
            // Hard cap 5 mins (300000L) is technically not hit with 5 retries (max 80s), but safe.
            outboxRepository.updateRetrySchedule(msg.clientMessageId, System.currentTimeMillis() + nextDelay)
        }
    }

    companion object {
        const val WORK_NAME = "OfflineMessageWorker"
    }
}

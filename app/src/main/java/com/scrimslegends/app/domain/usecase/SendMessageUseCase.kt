package com.scrimslegends.app.domain.usecase

import com.scrimslegends.app.data.service.SupabaseApiService
import com.scrimslegends.app.data.service.SupabaseService
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject

class SendMessageUseCase @Inject constructor(
    private val enqueueOfflineMessageUseCase: EnqueueOfflineMessageUseCase
) {
    private val api: SupabaseApiService = SupabaseService.api

    suspend operator fun invoke(
        conversationId: String,
        content: String,
        isTeamMessage: Boolean = false,
        isScrimMessage: Boolean = false
    ): Result<Unit> {
        val clientMessageId = UUID.randomUUID().toString()

        return try {
            val params = mutableMapOf<String, Any>(
                "p_conversation_id" to conversationId,
                "p_content" to content,
                "p_client_message_id" to clientMessageId
            )
            
            if (isTeamMessage) {
                params["p_is_team_chat"] = true
            }
            if (isScrimMessage) {
                params["p_is_scrim_chat"] = true
            }

            val response = api.rpcSendMessageSecure(params)

            if (response.isSuccessful || response.code() == 409) {
                // 409 means it was already delivered (idempotency triggered)
                Result.success(Unit)
            } else {
                Timber.e("Network send failed, enqueuing to offline worker: \${response.errorBody()?.string()}")
                enqueueOfflineMessageUseCase(
                    conversationId, content, clientMessageId, isTeamMessage, isScrimMessage
                )
                Result.success(Unit) // Still report success to UI because it's queued
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception during network send, enqueuing to offline worker")
            enqueueOfflineMessageUseCase(
                conversationId, content, clientMessageId, isTeamMessage, isScrimMessage
            )
            Result.success(Unit) // Still report success to UI because it's queued
        }
    }
}

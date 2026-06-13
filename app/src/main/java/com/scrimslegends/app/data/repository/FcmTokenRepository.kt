package com.scrimslegends.app.data.repository

import com.scrimslegends.app.data.service.SupabaseApiService
import com.scrimslegends.app.data.service.SupabaseService
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FcmTokenRepository @Inject constructor(
    // Alternatively, this can be provided through DI, 
    // but the pattern in this codebase seems to use SupabaseService.api mostly.
    // For testability, it's better to inject it, but RepositoryModule needs to provide it.
    // Let's rely on SupabaseService.api since DI module doesn't provide SupabaseApiService directly.
) : FcmTokenRepositoryInterface {

    private val api: SupabaseApiService = SupabaseService.api

    override suspend fun registerToken(token: String): Result<Unit> {
        return try {
            val response = api.rpcUpsertFcmToken(mapOf("p_token" to token))
            if (response.isSuccessful) {
                Timber.d("Successfully registered FCM token to Supabase")
                Result.success(Unit)
            } else {
                Timber.e("Failed to register FCM token: \${response.errorBody()?.string()}")
                Result.failure(Exception("HTTP \${response.code()}"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to register FCM token exception")
            Result.failure(e)
        }
    }

    override suspend fun deleteToken(token: String): Result<Unit> {
        return try {
            val response = api.rpcDeleteFcmToken(mapOf("p_token" to token))
            if (response.isSuccessful) {
                Timber.d("Successfully deleted FCM token from Supabase")
                Result.success(Unit)
            } else {
                Timber.e("Failed to delete FCM token: \${response.errorBody()?.string()}")
                Result.failure(Exception("HTTP \${response.code()}"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete FCM token exception")
            Result.failure(e)
        }
    }
}

package com.mlbb.scrim.data.repository

import com.mlbb.scrim.data.model.Profile
import com.mlbb.scrim.data.service.SupabaseClient
import io.github.jan.supabase.auth.exception.AuthRestException
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AuthRepository {

    suspend fun login(email: String, password: String): Result<Profile> = withContext(Dispatchers.IO) {
        try {
            SupabaseClient.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }

            val userId = SupabaseClient.auth.currentUserOrNull()?.id
                ?: return@withContext Result.failure(Exception("User ID not found"))

            val profile = SupabaseClient.postgrest.from("profiles")
                .select {
                    filter {
                        eq("id", userId)
                    }
                }
                .decodeSingle<Profile>()

            Result.success(profile)
        } catch (e: AuthRestException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun register(email: String, password: String, username: String): Result<Profile> = withContext(Dispatchers.IO) {
        try {
            SupabaseClient.auth.signUpWith(Email) {
                this.email = email
                this.password = password
            }

            // Profile will be created automatically by database trigger
            // Wait a moment for the trigger to execute
            kotlinx.coroutines.delay(1000)

            val userId = SupabaseClient.auth.currentUserOrNull()?.id
                ?: return@withContext Result.failure(Exception("User ID not found"))

            // Update username (trigger sets it to email prefix)
            SupabaseClient.postgrest.from("profiles")
                .update {
                    set("username", username)
                } {
                    filter {
                        eq("id", userId)
                    }
                }

            val profile = SupabaseClient.postgrest.from("profiles")
                .select {
                    filter {
                        eq("id", userId)
                    }
                }
                .decodeSingle<Profile>()

            Result.success(profile)
        } catch (e: AuthRestException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun logout(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            SupabaseClient.auth.signOut()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCurrentUser(): Result<Profile?> = withContext(Dispatchers.IO) {
        try {
            val userId = SupabaseClient.auth.currentUserOrNull()?.id
                ?: return@withContext Result.success(null)

            val profile = SupabaseClient.postgrest.from("profiles")
                .select {
                    filter {
                        eq("id", userId)
                    }
                }
                .decodeSingle<Profile>()

            Result.success(profile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun isLoggedIn(): Boolean {
        return SupabaseClient.auth.currentUserOrNull() != null
    }
}
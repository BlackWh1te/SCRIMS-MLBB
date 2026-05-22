package com.mlbb.scrim.data.repository

import com.mlbb.scrim.data.model.AuthResult
import com.mlbb.scrim.data.model.UserProfile
import kotlinx.coroutines.flow.Flow

/**
 * Common interface for authentication repositories.
 * Implemented by both mock and Supabase-backed repositories.
 */
interface AuthRepositoryInterface {

    suspend fun signUp(email: String, password: String, username: String, inGameId: String): Flow<AuthResult>

    suspend fun sendOtp(email: String, username: String, inGameId: String): Flow<AuthResult>

    suspend fun verifyOtp(email: String, token: String, password: String): Flow<AuthResult>

    suspend fun signIn(email: String, password: String): Flow<AuthResult>

    suspend fun signOut(): Flow<AuthResult>
    
    suspend fun deleteAccount(): Flow<AuthResult>

    suspend fun confirmEmail(): Flow<AuthResult>

    suspend fun resendVerificationEmail(email: String): Flow<AuthResult>

    suspend fun updateProfile(username: String, inGameId: String, role: String? = null, bio: String? = null, mainHeroes: List<String>? = null): Flow<AuthResult>
    
    suspend fun updateAvatar(avatarUrl: String): Flow<AuthResult>

    suspend fun uploadAndSetAvatar(fileBytes: ByteArray, contentType: String = "image/jpeg"): Flow<AuthResult>

    suspend fun updateEmail(newEmail: String, currentPassword: String): Flow<AuthResult>

    suspend fun updatePassword(currentPassword: String, newPassword: String, confirmPassword: String): Flow<AuthResult>

    fun getCurrentUser(): String?

    suspend fun getUserProfile(): UserProfile?

    suspend fun isLoggedIn(): Boolean

    fun isVerificationExpired(): Boolean

    fun secondsUntilDeletion(): Long

    suspend fun purgeIfExpired(): Boolean

    suspend fun updateLocationAndLastSeen()
}

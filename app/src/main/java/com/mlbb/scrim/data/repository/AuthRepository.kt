package com.mlbb.scrim.data.repository

import com.mlbb.scrim.data.model.AuthResult
import com.mlbb.scrim.data.model.RankTier
import com.mlbb.scrim.data.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

// Mock AuthRepository for UI testing without Supabase
// TODO: Replace with actual Supabase implementation when dependencies are resolved
class AuthRepository {
    
    private var currentUser: String? = null
    private var userProfile: UserProfile? = null
    
    init {
        // Reset to logged out state on initialization
        currentUser = null
        userProfile = null
    }
    
    suspend fun signUp(email: String, password: String, username: String, inGameId: String): Flow<AuthResult> = flow {
        emit(AuthResult.Loading)
        kotlinx.coroutines.delay(1000) // Simulate network delay
        
        // Mock validation
        if (email.contains("@") && password.length >= 6 && username.isNotBlank()) {
            currentUser = email
            userProfile = UserProfile(
                id = java.util.UUID.randomUUID().toString(),
                username = username,
                email = email,
                inGameId = inGameId,
                xp = 2450,
                totalMatches = 12,
                wins = 8,
                losses = 4,
                currentTier = RankTier.GOLD
            )
            emit(AuthResult.Success)
        } else {
            emit(AuthResult.Error("Invalid email, password (min 6 characters), or username"))
        }
    }
    
    suspend fun signIn(email: String, password: String): Flow<AuthResult> = flow {
        emit(AuthResult.Loading)
        kotlinx.coroutines.delay(1000) // Simulate network delay
        
        // Mock validation - accept any valid email format
        if (email.contains("@") && password.length >= 6) {
            currentUser = email
            // Set a mock profile on sign in if none exists
            if (userProfile == null) {
                userProfile = UserProfile(
                    id = java.util.UUID.randomUUID().toString(),
                    username = email.substringBefore("@"),
                    email = email,
                    inGameId = "",
                    xp = 2450,
                    totalMatches = 12,
                    wins = 8,
                    losses = 4,
                    currentTier = RankTier.GOLD
                )
            }
            emit(AuthResult.Success)
        } else {
            emit(AuthResult.Error("Invalid email or password"))
        }
    }
    
    suspend fun signOut(): Flow<AuthResult> = flow {
        emit(AuthResult.Loading)
        kotlinx.coroutines.delay(500) // Simulate network delay
        currentUser = null
        userProfile = null
        emit(AuthResult.Success)
    }
    
    suspend fun updateProfile(username: String, inGameId: String): Flow<AuthResult> = flow {
        emit(AuthResult.Loading)
        kotlinx.coroutines.delay(500) // Simulate network delay

        if (userProfile != null) {
            userProfile = userProfile!!.copy(
                username = username,
                inGameId = inGameId
            )
            emit(AuthResult.Success)
        } else {
            emit(AuthResult.Error("No user profile found"))
        }
    }

    suspend fun updateEmail(newEmail: String, currentPassword: String): Flow<AuthResult> = flow {
        emit(AuthResult.Loading)
        kotlinx.coroutines.delay(800) // Simulate network delay

        if (userProfile == null) {
            emit(AuthResult.Error("No user profile found. Please sign in again."))
            return@flow
        }

        // Mock validation: require password verification
        if (currentPassword.length < 6) {
            emit(AuthResult.Error("Current password is incorrect."))
            return@flow
        }

        if (!newEmail.contains("@") || newEmail.isBlank()) {
            emit(AuthResult.Error("Please enter a valid email address."))
            return@flow
        }

        // Update email in profile and current user
        userProfile = userProfile!!.copy(email = newEmail)
        currentUser = newEmail
        emit(AuthResult.Success)
    }

    suspend fun updatePassword(currentPassword: String, newPassword: String, confirmPassword: String): Flow<AuthResult> = flow {
        emit(AuthResult.Loading)
        kotlinx.coroutines.delay(800) // Simulate network delay

        if (userProfile == null) {
            emit(AuthResult.Error("No user profile found. Please sign in again."))
            return@flow
        }

        // Mock validation
        if (currentPassword.length < 6) {
            emit(AuthResult.Error("Current password is incorrect."))
            return@flow
        }

        if (newPassword.length < 6) {
            emit(AuthResult.Error("New password must be at least 6 characters."))
            return@flow
        }

        if (newPassword != confirmPassword) {
            emit(AuthResult.Error("New passwords do not match."))
            return@flow
        }

        if (currentPassword == newPassword) {
            emit(AuthResult.Error("New password must be different from current password."))
            return@flow
        }

        emit(AuthResult.Success)
    }

    fun getCurrentUser(): String? {
        return currentUser
    }
    
    fun getUserProfile(): UserProfile? {
        return userProfile
    }
    
    fun isLoggedIn(): Boolean {
        return currentUser != null
    }
}

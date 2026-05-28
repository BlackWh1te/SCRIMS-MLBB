package com.mlbb.scrim.data.repository

import com.mlbb.scrim.data.model.AuthResult
import com.mlbb.scrim.data.model.RankTier
import com.mlbb.scrim.data.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Authentication repository handling user sign-up, sign-in, and profile management.
 *
 * Current implementation: In-memory mock for UI development and testing.
 * Next step: Integrate with Supabase Auth (see SupabaseClient.kt for configuration).
 * The Supabase dependencies are prepared in build.gradle.kts and ready to be enabled.
 */
class AuthRepository : AuthRepositoryInterface {

    companion object {
        /** Unverified accounts are auto-deleted after 1 hour. */
        const val VERIFICATION_WINDOW_MS = 3_600_000L // 1 hour
    }

    private var currentUser: String? = null
    private var userProfile: UserProfile? = null
    private var storedPassword: String? = null

    init {
        // Reset to logged out state on initialization
        currentUser = null
        userProfile = null
        storedPassword = null
    }

    /**
     * Returns true if the unverified account has exceeded the 1-hour window.
     */
    override fun isVerificationExpired(): Boolean {
        val profile = userProfile ?: return false
        if (profile.emailVerified) return false
        val elapsed = System.currentTimeMillis() - profile.createdAt
        return elapsed > VERIFICATION_WINDOW_MS
    }

    /**
     * Returns remaining seconds before the unverified account is auto-deleted.
     * Returns 0 if already expired or not applicable.
     */
    override fun secondsUntilDeletion(): Long {
        val profile = userProfile ?: return 0L
        if (profile.emailVerified) return 0L
        val elapsed = System.currentTimeMillis() - profile.createdAt
        val remaining = VERIFICATION_WINDOW_MS - elapsed
        return if (remaining > 0) remaining / 1000 else 0L
    }

    /**
     * Deletes the unverified account if the verification window has expired.
     * Returns true if the account was deleted.
     */
    override suspend fun purgeIfExpired(): Boolean {
        if (isVerificationExpired()) {
            currentUser = null
            userProfile = null
            storedPassword = null
            return true
        }
        return false
    }
    
    override suspend fun sendOtp(email: String, username: String, inGameId: String): Flow<AuthResult> = flow {
        emit(AuthResult.Loading)
        kotlinx.coroutines.delay(800)
        if (email.contains("@")) {
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
                currentTier = RankTier.GOLD,
                emailVerified = false
            )
            emit(AuthResult.EmailNotVerified(email))
        } else {
            emit(AuthResult.Error("Invalid email address"))
        }
    }

    override suspend fun verifyOtp(email: String, token: String, password: String): Flow<AuthResult> = flow {
        emit(AuthResult.Loading)
        kotlinx.coroutines.delay(1000)
        if (token.length == 8 && token.all { it.isDigit() }) {
            userProfile = userProfile?.copy(emailVerified = true)
            storedPassword = password
            emit(AuthResult.Success)
        } else {
            emit(AuthResult.Error("Invalid code. Please enter the 8-digit code from your email."))
        }
    }

    override suspend fun sendPasswordResetOtp(email: String): Flow<AuthResult> = flow {
        emit(AuthResult.Loading)
        kotlinx.coroutines.delay(800)
        if (email.contains("@")) {
            emit(AuthResult.EmailNotVerified(email))
        } else {
            emit(AuthResult.Error("Invalid email address"))
        }
    }

    override suspend fun verifyPasswordResetOtp(email: String, token: String, newPassword: String): Flow<AuthResult> = flow {
        emit(AuthResult.Loading)
        kotlinx.coroutines.delay(1000)
        when {
            token.length != 8 || !token.all { it.isDigit() } ->
                emit(AuthResult.Error("Invalid code. Please enter the 8-digit code from your email."))
            newPassword.length < 6 ->
                emit(AuthResult.Error("New password must be at least 6 characters."))
            else -> emit(AuthResult.Success)
        }
    }

    override suspend fun signUp(email: String, password: String, username: String, inGameId: String): Flow<AuthResult> = flow {
        emit(AuthResult.Loading)
        kotlinx.coroutines.delay(1200) // Simulate network delay

        // Mock validation
        if (email.contains("@") && password.length >= 6 && username.isNotBlank()) {
            currentUser = email
            storedPassword = password
            userProfile = UserProfile(
                id = java.util.UUID.randomUUID().toString(),
                username = username,
                email = email,
                inGameId = inGameId,
                xp = 2450,
                totalMatches = 12,
                wins = 8,
                losses = 4,
                currentTier = RankTier.GOLD,
                emailVerified = false
            )
            // Require email verification before full access
            emit(AuthResult.EmailNotVerified(email))
        } else {
            emit(AuthResult.Error("Invalid email, password (min 6 characters), or username"))
        }
    }

    override suspend fun signIn(email: String, password: String): Flow<AuthResult> = flow {
        emit(AuthResult.Loading)
        kotlinx.coroutines.delay(1000) // Simulate network delay

        // Mock validation - accept any valid email format
        if (email.contains("@") && password.length >= 6) {
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
                    currentTier = RankTier.GOLD,
                    emailVerified = true // Assume verified for mock sign-in flow
                )
            }
            currentUser = email

            // Check if email is verified before allowing access
            if (userProfile?.emailVerified == false) {
                emit(AuthResult.EmailNotVerified(email))
            } else {
                emit(AuthResult.Success)
            }
        } else {
            emit(AuthResult.Error("Invalid email or password"))
        }
    }

    /**
     * Simulates confirming the email via a confirmation link.
     * In a real Supabase app, this would be triggered by a deep link
     * when the user taps the confirmation email.
     * If the 1-hour window has expired, the account is deleted.
     */
    override suspend fun confirmEmail(): Flow<AuthResult> = flow {
        emit(AuthResult.Loading)
        kotlinx.coroutines.delay(800)
        if (purgeIfExpired()) {
            emit(AuthResult.Error("Verification window expired. Your account has been deleted. Please sign up again."))
            return@flow
        }
        userProfile = userProfile?.copy(emailVerified = true)
        emit(AuthResult.Success)
    }

    /**
     * Simulates resending the confirmation email.
     * In a real Supabase app, this calls auth.resend().
     */
    override suspend fun resendVerificationEmail(email: String): Flow<AuthResult> = flow {
        emit(AuthResult.Loading)
        kotlinx.coroutines.delay(1000)
        emit(AuthResult.Success)
    }
    
    override suspend fun signOut(): Flow<AuthResult> = flow {
        emit(AuthResult.Loading)
        kotlinx.coroutines.delay(500) // Simulate network delay
        currentUser = null
        userProfile = null
        storedPassword = null
        emit(AuthResult.Success)
    }

    override suspend fun deleteAccount(): Flow<AuthResult> = flow {
        emit(AuthResult.Loading)
        kotlinx.coroutines.delay(800) // Simulate network delay
        currentUser = null
        userProfile = null
        storedPassword = null
        emit(AuthResult.Success)
    }
    
    override suspend fun updateProfile(username: String, inGameId: String, role: String?, bio: String?, mainHeroes: List<String>?): Flow<AuthResult> = flow {
        emit(AuthResult.Loading)
        kotlinx.coroutines.delay(500) // Simulate network delay

        if (userProfile != null) {
            userProfile = userProfile!!.copy(
                username = username,
                inGameId = inGameId,
                role = role ?: userProfile!!.role,
                bio = bio ?: userProfile!!.bio,
                mainHeroes = mainHeroes ?: userProfile!!.mainHeroes
            )
            emit(AuthResult.Success)
        } else {
            emit(AuthResult.Error("No user profile found"))
        }
    }

    override suspend fun updateAvatar(avatarUrl: String): Flow<AuthResult> = flow {
        emit(AuthResult.Loading)
        kotlinx.coroutines.delay(500)

        // Avatar feature not implemented in current UserProfile model
        emit(AuthResult.Error("Avatar update not supported"))
    }

    override suspend fun uploadAndSetAvatar(fileBytes: ByteArray, contentType: String): Flow<AuthResult> = flow {
        emit(AuthResult.Loading)
        kotlinx.coroutines.delay(500)
        emit(AuthResult.Error("Avatar upload not supported in mock repository"))
    }

    override suspend fun updateEmail(newEmail: String, currentPassword: String): Flow<AuthResult> = flow {
        emit(AuthResult.Loading)
        kotlinx.coroutines.delay(800) // Simulate network delay

        if (userProfile == null) {
            emit(AuthResult.Error("No user profile found. Please sign in again."))
            return@flow
        }

        // Mock validation: require password verification
        if (currentPassword.length < 6 || storedPassword?.let { currentPassword != it } == true) {
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

    override suspend fun updatePassword(currentPassword: String, newPassword: String, confirmPassword: String): Flow<AuthResult> = flow {
        emit(AuthResult.Loading)
        kotlinx.coroutines.delay(800) // Simulate network delay

        if (userProfile == null) {
            emit(AuthResult.Error("No user profile found. Please sign in again."))
            return@flow
        }

        // Mock validation
        if (currentPassword.length < 6 || storedPassword?.let { currentPassword != it } == true) {
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

        storedPassword = newPassword
        emit(AuthResult.Success)
    }

    override fun getCurrentUser(): String? {
        return currentUser
    }
    
    override suspend fun getUserProfile(): UserProfile? {
        return userProfile
    }

    override suspend fun invalidateProfileCache() {
        // No-op for mock repository
    }

    override suspend fun isLoggedIn(): Boolean {
        purgeIfExpired() // silently clean up expired unverified accounts
        return currentUser != null
    }

    override suspend fun updateLocationAndLastSeen() {
        // Mock implementation: do nothing for the local mock repo
        kotlinx.coroutines.delay(100)
    }
}

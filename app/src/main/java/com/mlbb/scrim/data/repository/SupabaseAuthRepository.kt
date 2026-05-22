package com.mlbb.scrim.data.repository

import android.content.Context
import com.mlbb.scrim.data.model.AuthResult
import com.mlbb.scrim.data.model.RankTier
import com.mlbb.scrim.data.model.UserProfile
import com.mlbb.scrim.data.service.*
import com.mlbb.scrim.security.SecureStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.firstOrNull
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

import com.mlbb.scrim.data.cache.UnifiedCacheManager

/**
 * Supabase-backed authentication repository.
 *
 * Uses Supabase Auth REST API for real user authentication.
 * Stores JWT tokens securely using encrypted SharedPreferences.
 */
class SupabaseAuthRepository(
    private val context: Context,
    private val cacheManager: UnifiedCacheManager
) : AuthRepositoryInterface {

    companion object {
        private const val KEY_ACCESS_TOKEN = "supabase_access_token"
        private const val KEY_REFRESH_TOKEN = "supabase_refresh_token"
        private const val KEY_USER_ID = "supabase_user_id"
        private const val KEY_USER_EMAIL = "supabase_user_email"
        private const val VERIFICATION_WINDOW_MS = 3_600_000L // 1 hour
    }

    private val secureStorage = SecureStorage.getInstance(context)
    private val api = SupabaseService.api
    private val authApi = SupabaseAuthServiceClient.api
    private val otpApi = OtpApiClient.service

    private var cachedProfile: UserProfile? = null
    private var pendingUsername: String = ""
    private var pendingInGameId: String = ""

    // ─── Token Management ───

    private fun getAccessToken(): String? = secureStorage.getEncrypted(KEY_ACCESS_TOKEN, "")
        .takeIf { it.isNotBlank() }

    private fun getRefreshToken(): String? = secureStorage.getEncrypted(KEY_REFRESH_TOKEN, "")
        .takeIf { it.isNotBlank() }

    private fun getUserId(): String? = secureStorage.getEncrypted(KEY_USER_ID, "")
        .takeIf { it.isNotBlank() }

    private fun saveTokens(accessToken: String, refreshToken: String, userId: String) {
        secureStorage.storeEncrypted(KEY_ACCESS_TOKEN, accessToken)
        secureStorage.storeEncrypted(KEY_REFRESH_TOKEN, refreshToken)
        secureStorage.storeEncrypted(KEY_USER_ID, userId)
    }

    private suspend fun clearTokens() {
        secureStorage.remove(KEY_ACCESS_TOKEN)
        secureStorage.remove(KEY_REFRESH_TOKEN)
        secureStorage.remove(KEY_USER_ID)
        secureStorage.remove(KEY_USER_EMAIL)
        cachedProfile = null
        try { cacheManager.invalidateByPrefix("current_user_profile_") } catch (_: Exception) {}
    }

    private fun authHeader(): String? = getAccessToken()?.let { "Bearer $it" }

    private fun parseTimestamp(raw: String?): Long {
        if (raw.isNullOrBlank()) return System.currentTimeMillis()

        val patterns = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss"
        )

        for (pattern in patterns) {
            try {
                val formatter = SimpleDateFormat(pattern, Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                val parsed = formatter.parse(raw)
                if (parsed != null) {
                    return parsed.time
                }
            } catch (_: Exception) {
            }
        }

        return System.currentTimeMillis()
    }

    private fun mapUserProfile(
        userId: String,
        profileDto: ProfileDto?,
        statsDto: PlayerStatsDto?,
        authUser: SupabaseUser?
    ): UserProfile {
        val pts = statsDto?.pts ?: 0
        val rankXp = pts.coerceAtLeast(0)
        val fallbackEmail = authUser?.email ?: secureStorage.getEncrypted(KEY_USER_EMAIL, "")
        val fallbackUsername = (authUser?.userMetadata?.get("username") as? String)
            ?: fallbackEmail.substringBefore('@', "Player")
        val fallbackInGameId = (authUser?.userMetadata?.get("mlbb_id") as? String).orEmpty()

        return UserProfile(
            id = userId,
            username = profileDto?.username?.takeIf { it.isNotBlank() } ?: fallbackUsername,
            email = profileDto?.email?.takeIf { it.isNotBlank() } ?: fallbackEmail,
            inGameId = profileDto?.mlbbId?.takeIf { it.isNotBlank() } ?: fallbackInGameId,
            createdAt = profileDto?.createdAt?.let(::parseTimestamp) ?: System.currentTimeMillis(),
            xp = rankXp,
            pts = pts,
            totalMatches = statsDto?.matchesPlay ?: 0,
            wins = statsDto?.wins ?: 0,
            losses = statsDto?.losses ?: 0,
            currentTier = RankTier.fromXp(rankXp),
            emailVerified = profileDto?.emailVerified == true || authUser?.emailConfirmedAt != null,
            isBanned = profileDto?.isBanned ?: false,
            mainHeroes = profileDto?.mainHeroes ?: emptyList(),
            role = profileDto?.role ?: "",
            bio = profileDto?.bio ?: ""
        )
    }

    private fun mapOtpError(errorBody: String, fallbackPrefix: String): String {
        return when {
            errorBody.contains("User already registered", ignoreCase = true) ->
                "This email is already registered. Please sign in instead."
            errorBody.contains("validation_failed", ignoreCase = true) ->
                "Invalid email address. Please check and try again."
            errorBody.contains("email rate limit exceeded", ignoreCase = true) ||
                errorBody.contains("over_email_send_rate_limit", ignoreCase = true) ||
                errorBody.contains("over email send rate limit", ignoreCase = true) ->
                "Too many verification emails were requested. Please wait a few minutes and try again."
            else -> "$fallbackPrefix: $errorBody"
        }
    }

    // ─── OTP-based Sign-up ───

    override suspend fun sendOtp(email: String, username: String, inGameId: String): Flow<AuthResult> = flow {
        emit(AuthResult.Loading)
        try {
            // Check if MLBB ID is already taken or banned
            val checkResponse = api.getProfileByMlbbId(PostgrestFilter.eq(inGameId))
            if (checkResponse.isSuccessful && checkResponse.body()?.isNotEmpty() == true) {
                val profile = checkResponse.body()!!.first()
                if (profile.isBanned) {
                    emit(AuthResult.Error("This Game ID is banned and cannot be used for new accounts."))
                } else {
                    emit(AuthResult.Error("This Game ID is already linked to another account."))
                }
                return@flow
            }

            pendingUsername = username
            pendingInGameId = inGameId

            // Use Supabase's built-in OTP — sends email directly from Supabase servers
            val response = authApi.sendOtp(OtpRequest(email = email, type = "signup"))
            if (response.isSuccessful) {
                emit(AuthResult.EmailNotVerified(email))
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                val message = mapOtpError(errorBody, "Failed to send verification code")
                emit(AuthResult.Error(message))
            }
        } catch (e: Exception) {
            emit(AuthResult.Error("Failed to send verification code: ${e.message}"))
        }
    }

    override suspend fun verifyOtp(email: String, token: String, password: String): Flow<AuthResult> = flow {
        emit(AuthResult.Loading)
        try {
            // Step 1: Verify OTP with Supabase Auth
            val verifyResponse = authApi.verifyOtp(VerifyOtpRequest(email = email, token = token, type = "signup"))
            if (verifyResponse.isSuccessful) {
                val authData = verifyResponse.body()
                if (authData?.user != null && authData.accessToken != null) {
                    // OTP verified — user exists in Supabase Auth now
                    authData.refreshToken?.let { refresh ->
                        saveTokens(authData.accessToken, refresh, authData.user.id)
                    }
                    secureStorage.storeEncrypted(KEY_USER_EMAIL, email)

                    // Step 2: Set password (Supabase OTP signup doesn't set password)
                    try {
                        authApi.updateUser(
                            authHeader = "Bearer ${authData.accessToken}",
                            request = mapOf("password" to password)
                        )
                    } catch (_: Exception) { }

                    // Step 3: Update profile with username and inGameId
                    // The DB trigger may take a moment to create the profile row after OTP verify,
                    // so retry with a small delay if the first attempt fails.
                    try {
                        val profileUpdate = mapOf(
                            "username" to (pendingUsername.ifBlank { email.substringBefore("@") }),
                            "mlbb_id" to (pendingInGameId.ifBlank { "" })
                        )
                        val updateResult = api.updateProfile(PostgrestFilter.eq(authData.user.id), profileUpdate)
                        if (!updateResult.isSuccessful) {
                            // Profile row may not exist yet — try PATCH → POST fallback
                            kotlinx.coroutines.delay(500)
                            try {
                                api.createProfile(ProfileDto(
                                    id = authData.user.id,
                                    username = pendingUsername.ifBlank { email.substringBefore("@") },
                                    email = email,
                                    mlbbId = pendingInGameId.ifBlank { "" }
                                ))
                            } catch (_: Exception) { }
                        }
                    } catch (_: Exception) { }

                    pendingUsername = ""
                    pendingInGameId = ""
                    emit(AuthResult.Success)
                } else {
                    emit(AuthResult.Error("Verification succeeded, but no session was created. Please try logging in."))
                }
            } else {
                val errorBody = verifyResponse.errorBody()?.string() ?: "Unknown error"
                val message = when {
                    errorBody.contains("otp_expired", ignoreCase = true) ->
                        "Code expired. Please request a new one."
                    errorBody.contains("token_expired", ignoreCase = true) ->
                        "Code expired. Please request a new one."
                    errorBody.contains("user_not_found", ignoreCase = true) ->
                        "User not found. Please sign up again."
                    else -> "Invalid code. Please try again."
                }
                emit(AuthResult.Error(message))
            }
        } catch (e: Exception) {
            emit(AuthResult.Error("Verification failed: ${e.message}"))
        }
    }

    // ─── Auth Operations ───

    override suspend fun signUp(email: String, password: String, username: String, inGameId: String): Flow<AuthResult> = flow {
        emit(AuthResult.Loading)
        try {
            // Check if MLBB ID is already taken or banned
            val checkResponse = api.getProfileByMlbbId(PostgrestFilter.eq(inGameId))
            if (checkResponse.isSuccessful && checkResponse.body()?.isNotEmpty() == true) {
                val profile = checkResponse.body()!!.first()
                if (profile.isBanned) {
                    emit(AuthResult.Error("This Game ID is banned and cannot be used for new accounts."))
                } else {
                    emit(AuthResult.Error("This Game ID is already linked to another account."))
                }
                return@flow
            }

            val response = authApi.signUp(SignUpRequest(
                email = email,
                password = password,
                data = mapOf("username" to username, "mlbb_id" to inGameId)
            ))

            if (response.isSuccessful) {
                val authData = response.body()
                if (authData?.user != null) {
                    // Save tokens
                    authData.accessToken?.let { token ->
                        authData.refreshToken?.let { refresh ->
                            saveTokens(token, refresh, authData.user.id)
                        }
                    }

                    // Update profile in database (trigger auto-creates, we just update it)
                    authData.user.id.let { userId ->
                        try {
                            api.updateProfile(PostgrestFilter.eq(userId), mapOf(
                                "username" to username,
                                "email" to email,
                                "mlbb_id" to inGameId
                            ))
                        } catch (_: Exception) {
                        }
                    }

                    // Check if email is confirmed
                    if (authData.user.emailConfirmedAt != null) {
                        emit(AuthResult.Success)
                    } else {
                        emit(AuthResult.EmailNotVerified(email))
                    }
                } else {
                    emit(AuthResult.Error("Sign up failed: No user data returned"))
                }
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                emit(AuthResult.Error("Sign up failed: $errorBody"))
            }
        } catch (e: Exception) {
            emit(AuthResult.Error("Sign up failed: ${e.message}"))
        }
    }

    override suspend fun signIn(email: String, password: String): Flow<AuthResult> = flow {
        emit(AuthResult.Loading)
        try {
            val response = authApi.signIn(SignInRequest(email = email, password = password))

            if (response.isSuccessful) {
                val authData = response.body()
                if (authData?.user != null) {
                    // Save tokens
                    authData.accessToken?.let { token ->
                        authData.refreshToken?.let { refresh ->
                            saveTokens(token, refresh, authData.user.id)
                        }
                    }
                    secureStorage.storeEncrypted(KEY_USER_EMAIL, email)

                    // Check if email is confirmed
                    if (authData.user.emailConfirmedAt != null) {
                        emit(AuthResult.Success)
                    } else {
                        emit(AuthResult.EmailNotVerified(email))
                    }
                } else {
                    emit(AuthResult.Error("Sign in failed: No user data returned"))
                }
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                val errorCode = response.code()
                val friendlyMessage = when {
                    // Invalid credentials (wrong password or non-existent user)
                    errorCode == 400 || errorBody.contains("invalid_grant", ignoreCase = true) ||
                    errorBody.contains("Invalid login credentials", ignoreCase = true) ||
                    errorBody.contains("Email not confirmed", ignoreCase = true) == false ->
                        "Invalid email or password. Please check your credentials and try again."
                    // Email not verified
                    errorBody.contains("Email not confirmed", ignoreCase = true) ->
                        "Please verify your email address first. Check your inbox for the verification link."
                    // Account locked or disabled
                    errorBody.contains("locked", ignoreCase = true) ||
                    errorBody.contains("disabled", ignoreCase = true) ||
                    errorBody.contains("blocked", ignoreCase = true) ->
                        "Your account has been locked. Please try again later or contact support."
                    // Network/server errors
                    errorCode == 503 || errorCode == 502 || errorCode == 504 ->
                        "Service temporarily unavailable. Please try again later."
                    // Fallback for other errors
                    else -> "Sign in failed: Invalid email or password."
                }
                emit(AuthResult.Error(friendlyMessage))
            }
        } catch (e: Exception) {
            emit(AuthResult.Error("Sign in failed. Please check your internet connection and try again."))
        }
    }

    override suspend fun signOut(): Flow<AuthResult> = flow {
        emit(AuthResult.Loading)
        try {
            authHeader()?.let { header ->
                authApi.signOut(header)
            }
            clearTokens()
            emit(AuthResult.Success)
        } catch (e: Exception) {
            clearTokens() // Always clear local tokens
            emit(AuthResult.Success)
        }
    }

    override suspend fun deleteAccount(): Flow<AuthResult> = flow {
        emit(AuthResult.Loading)
        try {
            // P2-4 FIX: The delete_user RPC doesn't exist in the schema (returns 404).
            // Use a soft-delete approach: mark the profile as deleted, then sign out.
            // A Supabase Edge Function or database trigger should handle auth.users cleanup.
            val userId = getUserId()
            if (userId != null) {
                api.deactivateUser(
                    userId = PostgrestFilter.eq(userId),
                    body = mapOf("deleted" to true, "deleted_at" to java.time.Instant.now().toString())
                )
            }
            clearTokens()
            emit(AuthResult.Success)
        } catch (e: Exception) {
            clearTokens()
            emit(AuthResult.Error("Failed to delete account: ${e.message}"))
        }
    }

    override suspend fun confirmEmail(): Flow<AuthResult> = flow {
        emit(AuthResult.Loading)
        try {
            // In Supabase, email is confirmed via deep link. 
            // This method just checks current user status.
            authHeader()?.let { header ->
                val response = authApi.getCurrentUser(header)
                if (response.isSuccessful) {
                    val user = response.body()
                    if (user?.emailConfirmedAt != null) {
                        emit(AuthResult.Success)
                    } else {
                        if (isVerificationExpired()) {
                            clearTokens()
                            emit(AuthResult.Error("Verification window expired. Your account has been deleted. Please sign up again."))
                        } else {
                            emit(AuthResult.EmailNotVerified(user?.email ?: ""))
                        }
                    }
                } else {
                    emit(AuthResult.Error("Could not verify email status"))
                }
            } ?: emit(AuthResult.Error("Not authenticated"))
        } catch (e: Exception) {
            emit(AuthResult.Error("Error: ${e.message}"))
        }
    }

    override suspend fun resendVerificationEmail(email: String): Flow<AuthResult> = flow {
        emit(AuthResult.Loading)
        try {
            val response = otpApi.sendOtp(SendOtpBackendRequest(email))
            if (response.isSuccessful) {
                emit(AuthResult.Success)
            } else {
                val errorBody = response.errorBody()?.string() ?: response.body()?.message ?: "Unknown error"
                val message = when {
                    errorBody.contains("User already registered", ignoreCase = true) ->
                        "This account is already verified. Please sign in."
                    else -> mapOtpError(errorBody, "Failed to resend code")
                }
                emit(AuthResult.Error(message))
            }
        } catch (e: Exception) {
            emit(AuthResult.Error("Failed to resend code: ${e.message}"))
        }
    }

    // ─── Profile Operations ───

    override suspend fun updateProfile(username: String, inGameId: String, role: String?, bio: String?, mainHeroes: List<String>?): Flow<AuthResult> = flow {
        emit(AuthResult.Loading)
        try {
            getUserId()?.let { userId ->
                // Check if MLBB ID is already taken by someone else
                val checkResponse = api.getProfileByMlbbId(PostgrestFilter.eq(inGameId))
                if (checkResponse.isSuccessful && checkResponse.body()?.isNotEmpty() == true) {
                    val existing = checkResponse.body()!!.first()
                    if (existing.id != userId) {
                        emit(AuthResult.Error("This Game ID is already linked to another account."))
                        return@flow
                    }
                }

                val updateMap = mutableMapOf<String, Any>(
                    "username" to username,
                    "mlbb_id" to inGameId
                )
                if (role != null) updateMap["role"] = role
                if (bio != null) updateMap["bio"] = bio
                if (mainHeroes != null) updateMap["main_heroes"] = mainHeroes

                val response = api.updateProfile(PostgrestFilter.eq(userId), updateMap)
                if (response.isSuccessful) {
                    cacheManager.invalidate("current_user_profile_$userId")
                    getUserProfile() // Refresh Room cache
                    emit(AuthResult.Success)
                } else {
                    emit(AuthResult.Error("Failed to update profile"))
                }
            } ?: emit(AuthResult.Error("Not authenticated"))
        } catch (e: Exception) {
            emit(AuthResult.Error("Error: ${e.message}"))
        }
    }

    override suspend fun updateAvatar(avatarUrl: String): Flow<AuthResult> = flow {
        emit(AuthResult.Loading)
        try {
            val userId = getUserId() ?: throw Exception("Not authenticated")
            val response = api.updateProfile(userId, mapOf("avatar_url" to avatarUrl))
            if (response.isSuccessful) {
                cacheManager.invalidate("profile_$userId")
                emit(AuthResult.Success)
            } else {
                emit(AuthResult.Error("Failed to update avatar: ${response.code()}"))
            }
        } catch (e: Exception) {
            emit(AuthResult.Error("Error: ${e.message}"))
        }
    }

    override suspend fun uploadAndSetAvatar(fileBytes: ByteArray, contentType: String): Flow<AuthResult> = flow {
        emit(AuthResult.Loading)
        try {
            val userId = getUserId() ?: throw Exception("Not authenticated")
            val path = "avatars/${userId}_${System.currentTimeMillis()}.${if (contentType.contains("jpeg")) "jpg" else "png"}"
            val uploadResult = SupabaseStorageUpload.uploadFile(
                bucket = SupabaseConfig.BUCKET_AVATARS,
                path = path,
                fileBytes = fileBytes,
                contentType = contentType
            )
            uploadResult.onSuccess { publicUrl ->
                val updateResponse = api.updateProfile(userId, mapOf("avatar_url" to publicUrl))
                if (updateResponse.isSuccessful) {
                    cacheManager.invalidate("profile_$userId")
                    emit(AuthResult.Success)
                } else {
                    emit(AuthResult.Error("Avatar uploaded but profile update failed"))
                }
            }.onFailure { e ->
                emit(AuthResult.Error("Upload failed: ${e.message}"))
            }
        } catch (e: Exception) {
            emit(AuthResult.Error("Error: ${e.message}"))
        }
    }

    override suspend fun updateEmail(newEmail: String, currentPassword: String): Flow<AuthResult> = flow {
        emit(AuthResult.Loading)
        try {
            // In Supabase, updating email requires current password
            // This is typically done through the auth.update() method
            // For REST API, we'd need to call the auth endpoint directly
            kotlinx.coroutines.delay(800)

            getUserId()?.let { userId ->
                val response = api.updateProfile(PostgrestFilter.eq(userId), mapOf("email" to newEmail))
                if (response.isSuccessful) {
                    cacheManager.invalidate("current_user_profile_$userId")
                    secureStorage.storeEncrypted(KEY_USER_EMAIL, newEmail)
                    emit(AuthResult.Success)
                } else {
                    emit(AuthResult.Error("Failed to update email"))
                }
            } ?: emit(AuthResult.Error("Not authenticated"))
        } catch (e: Exception) {
            emit(AuthResult.Error("Error: ${e.message}"))
        }
    }

    override suspend fun updatePassword(currentPassword: String, newPassword: String, confirmPassword: String): Flow<AuthResult> = flow {
        emit(AuthResult.Loading)
        try {
            if (newPassword != confirmPassword) {
                emit(AuthResult.Error("New passwords do not match."))
                return@flow
            }
            if (newPassword.length < 6) {
                emit(AuthResult.Error("New password must be at least 6 characters."))
                return@flow
            }
            if (currentPassword == newPassword) {
                emit(AuthResult.Error("New password must be different from current password."))
                return@flow
            }

            authHeader()?.let { header ->
                val response = authApi.updateUser(header, mapOf("password" to newPassword))
                if (response.isSuccessful) {
                    emit(AuthResult.Success)
                } else {
                    emit(AuthResult.Error("Failed to update password: ${response.errorBody()?.string()}"))
                }
            } ?: emit(AuthResult.Error("Not authenticated"))
        } catch (e: Exception) {
            emit(AuthResult.Error("Error: ${e.message}"))
        }
    }

    // ─── User State ───

    override fun getCurrentUser(): String? = getUserId()

    override suspend fun getUserProfile(): UserProfile? {
        val userId = getUserId() ?: return null

        val db = com.mlbb.scrim.data.local.MLBBScrimDatabase.getDatabase(context)
        val profileDao = db.profileDao()

        return try {
            cacheManager.get(
                key = "current_user_profile_$userId",
                memoryTtlMs = 60_000L, // 1 minute memory
                roomTtlMs = 300_000L,  // 5 minutes Room
                roomLoader = {
                    val local = profileDao.getProfileById(userId).firstOrNull()?.let { mapEntityToProfile(it) }
                    if (local != null) cachedProfile = local
                    local
                },
                networkLoader = {
                    val authUser = authHeader()?.let { header ->
                        authApi.getCurrentUser(header).body()
                    }
                    val profileResponse = api.getProfileById(PostgrestFilter.eq(userId))
                    val statsResponse = api.getPlayerStats(PostgrestFilter.eq(userId))

                    if (!profileResponse.isSuccessful || !statsResponse.isSuccessful) {
                        throw Exception("Failed to load profile from network")
                    }

                    val profile = mapUserProfile(
                        userId = userId,
                        profileDto = profileResponse.body()?.firstOrNull(),
                        statsDto = statsResponse.body()?.firstOrNull(),
                        authUser = authUser
                    )
                    cachedProfile = profile
                    profile
                },
                roomSaver = { userProfile ->
                    profileDao.insertProfile(mapProfileToEntity(userProfile))
                }
            )
        } catch (e: Exception) {
            // Offline fallback if network fails and cache is empty
            val fallbackEmail = secureStorage.getEncrypted(KEY_USER_EMAIL, "")
            UserProfile(
                id = userId,
                username = fallbackEmail.substringBefore('@', "Player"),
                email = fallbackEmail,
                inGameId = "",
                emailVerified = true
            )
        }
    }

    private fun mapEntityToProfile(entity: com.mlbb.scrim.data.local.ProfileEntity): UserProfile {
        return UserProfile(
            id = entity.id,
            username = entity.username,
            email = entity.email ?: "",
            inGameId = entity.inGameId ?: "",
            currentTier = RankTier.values().find { it.name == entity.rank } ?: RankTier.BRONZE,
            pts = entity.points,
            isBanned = entity.isBanned
        )
    }

    private fun mapProfileToEntity(profile: UserProfile): com.mlbb.scrim.data.local.ProfileEntity {
        return com.mlbb.scrim.data.local.ProfileEntity(
            id = profile.id,
            username = profile.username,
            fullName = null,
            avatarUrl = profile.avatarUrl,
            email = profile.email,
            inGameId = profile.inGameId,
            rank = profile.currentTier.name,
            role = null,
            bio = profile.bio,
            mainHeroes = profile.mainHeroes.joinToString(","),
            points = profile.pts,
            isBanned = profile.isBanned,
            lastUpdated = System.currentTimeMillis()
        )
    }

    override suspend fun isLoggedIn(): Boolean = getAccessToken() != null

    // ─── Verification Window (kept for compatibility) ───

    override fun isVerificationExpired(): Boolean {
        val profile = cachedProfile ?: return false
        if (profile.emailVerified) return false
        val elapsed = System.currentTimeMillis() - profile.createdAt
        return elapsed > VERIFICATION_WINDOW_MS
    }

    override fun secondsUntilDeletion(): Long {
        val profile = cachedProfile ?: return 0L
        if (profile.emailVerified) return 0L
        val elapsed = System.currentTimeMillis() - profile.createdAt
        val remaining = VERIFICATION_WINDOW_MS - elapsed
        return if (remaining > 0) remaining / 1000 else 0L
    }

    override suspend fun purgeIfExpired(): Boolean {
        return if (isVerificationExpired()) {
            clearTokens()
            true
        } else false
    }

    override suspend fun updateLocationAndLastSeen() {
        try {
            val userId = getUserId() ?: return
            
            val client = okhttp3.OkHttpClient()
            val request = okhttp3.Request.Builder()
                .url("https://get.geojs.io/v1/ip/geo.json")
                .build()
                
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val bodyString = response.body?.string()
                if (bodyString != null) {
                    val jsonObject = org.json.JSONObject(bodyString)
                    val country = jsonObject.optString("country", "")
                    val city = jsonObject.optString("city", "")
                    
                    if (country.isNotEmpty() || city.isNotEmpty()) {
                        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US)
                        sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
                        val nowIso = sdf.format(java.util.Date())
                        
                        val updateMap = mapOf(
                            "country" to country,
                            "city" to city,
                            "last_seen" to nowIso
                        )
                        api.updateProfile(PostgrestFilter.eq(userId), updateMap)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

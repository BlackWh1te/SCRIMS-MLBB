package com.scrimslegends.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.SavedStateHandleSaveableApi
import com.scrimslegends.app.data.model.AuthResult
import com.scrimslegends.app.data.model.UserProfile
import com.scrimslegends.app.data.repository.AuthRepositoryInterface
import com.scrimslegends.app.data.service.SupabaseRealtimeClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    application: Application,
    private val savedStateHandle: SavedStateHandle,
    private val authRepository: AuthRepositoryInterface,
    private val realtimeClient: SupabaseRealtimeClient
) : AndroidViewModel(application) {

    /**
     * Toggle between mock and Supabase backends.
     * Set to true to use the real Supabase database.
     */
    companion object {
        const val USE_SUPABASE = true
        private const val VERIFICATION_WINDOW_SECONDS = 3_600L
        private const val MAX_AVATAR_SIZE_BYTES = 3L * 1024 * 1024 // 3MB

        // SavedStateHandle keys — survives process death
        private const val KEY_PENDING_EMAIL = "pending_email"
        private const val KEY_PENDING_PASSWORD = "pending_password"
        private const val KEY_PENDING_USERNAME = "pending_username"
        private const val KEY_PENDING_IN_GAME_ID = "pending_in_game_id"
        private const val KEY_PENDING_VERIFICATION_STARTED_AT = "pending_verification_started_at"
    }

    private var signUpJob: Job? = null
    private var signInJob: Job? = null
    private var signOutJob: Job? = null
    private var updateProfileJob: Job? = null
    private var updateEmailJob: Job? = null
    private var updatePasswordJob: Job? = null
    private var confirmEmailJob: Job? = null
    private var resendEmailJob: Job? = null
    private var verifyOtpJob: Job? = null
    private var sendPasswordResetOtpJob: Job? = null
    private var verifyPasswordResetOtpJob: Job? = null
    private var deleteAccountJob: Job? = null
    private var uploadAvatarJob: Job? = null

    /**
     * Pending credentials stored during OTP sign-up flow.
     * Backed by SavedStateHandle so they survive Android process death
     * (e.g. when the user switches to Gmail to copy the OTP code).
     */
    private var pendingEmail: String
        get() = savedStateHandle[KEY_PENDING_EMAIL] ?: ""
        set(value) { savedStateHandle[KEY_PENDING_EMAIL] = value }

    private var pendingPassword: String
        get() = savedStateHandle[KEY_PENDING_PASSWORD] ?: ""
        set(value) { savedStateHandle[KEY_PENDING_PASSWORD] = value }

    private var pendingUsername: String
        get() = savedStateHandle[KEY_PENDING_USERNAME] ?: ""
        set(value) { savedStateHandle[KEY_PENDING_USERNAME] = value }

    private var pendingInGameId: String
        get() = savedStateHandle[KEY_PENDING_IN_GAME_ID] ?: ""
        set(value) { savedStateHandle[KEY_PENDING_IN_GAME_ID] = value }

    private var pendingVerificationStartedAtMs: Long?
        get() = savedStateHandle[KEY_PENDING_VERIFICATION_STARTED_AT]
        set(value) { savedStateHandle[KEY_PENDING_VERIFICATION_STARTED_AT] = value }

    private val _authState = MutableStateFlow<AuthResult>(AuthResult.Idle)
    val authState: StateFlow<AuthResult> = _authState.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _isInitializing = MutableStateFlow(true)
    val isInitializing: StateFlow<Boolean> = _isInitializing.asStateFlow()

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    private val _isProfileRefreshing = MutableStateFlow(false)
    val isProfileRefreshing: StateFlow<Boolean> = _isProfileRefreshing.asStateFlow()

    /** True when the verification email was resent successfully. */
    private val _resentSuccess = MutableStateFlow(false)
    val resentSuccess: StateFlow<Boolean> = _resentSuccess.asStateFlow()

    /** Remaining seconds before an unverified account is auto-deleted (1h window). */
    fun secondsUntilDeletion(): Long {
        val repositorySeconds = authRepository.secondsUntilDeletion()
        if (repositorySeconds > 0) {
            return repositorySeconds
        }

        val verificationStartedAtMs = pendingVerificationStartedAtMs ?: return VERIFICATION_WINDOW_SECONDS
        val elapsedSeconds = (System.currentTimeMillis() - verificationStartedAtMs) / 1000
        return (VERIFICATION_WINDOW_SECONDS - elapsedSeconds).coerceAtLeast(0L)
    }

    init {
        viewModelScope.launch {
            checkAuthStatus()
        }
    }

    private suspend fun checkAuthStatus() {
        _isInitializing.value = true
        try {
            val hasToken = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                authRepository.isLoggedIn()
            }
            
            if (hasToken) {
                _isLoggedIn.value = true
                // Eagerly connect Realtime on cold start with existing session
                realtimeClient.connect()
                
                // Load profile in background
                val profile = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    try {
                        authRepository.getUserProfile()
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to load profile during init")
                        null
                    }
                }
                handleProfileFetch(profile)
                
                // Update location and last seen in a completely separate job so it never blocks UI
                viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                    try {
                        authRepository.updateLocationAndLastSeen()
                    } catch (e: Exception) {
                        Timber.w(e, "Silent location update failure")
                    }
                }
            } else {
                _isLoggedIn.value = false
                _userProfile.value = null
            }
        } catch (e: Exception) {
            Timber.e(e, "Critical init failure")
            _isLoggedIn.value = false
        } finally {
            _isInitializing.value = false
        }
    }

    private suspend fun handleProfileFetch(profile: UserProfile?) {
        if (profile?.isBanned == true) {
            // Keep user logged in but mark as banned — BannedScreen will handle the UI
            _userProfile.value = profile
            _isLoggedIn.value = true
            // Disconnect Realtime — banned user should not receive live updates
            realtimeClient.disconnect()
        } else {
            _userProfile.value = profile
        }
    }

    fun signUp(email: String, password: String, username: String, inGameId: String) {
        signUpJob?.cancel()
        _authState.value = AuthResult.Loading
        // Store credentials for OTP verification step (survives process death)
        pendingEmail = email
        pendingPassword = password
        pendingUsername = username
        pendingInGameId = inGameId
        pendingVerificationStartedAtMs = System.currentTimeMillis()
        signUpJob = viewModelScope.launch {
            authRepository.sendOtp(email, username, inGameId).collect { result ->
                _authState.value = result
                if (result is AuthResult.Error) {
                    pendingVerificationStartedAtMs = null
                }
            }
        }
    }

    /** Verify the 8-digit OTP code sent to the user's email. */
    fun verifyOtp(token: String) {
        verifyOtpJob?.cancel()
        _authState.value = AuthResult.Loading
        verifyOtpJob = viewModelScope.launch {
            authRepository.verifyOtp(pendingEmail, token, pendingPassword).collect { result ->
                _authState.value = result
                if (result is AuthResult.Success) {
                    _isLoggedIn.value = true
                    // Eagerly connect Realtime WebSocket on sign-in
                    realtimeClient.connect()
                    handleProfileFetch(authRepository.getUserProfile())
                    // Update location on fresh login
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        authRepository.updateLocationAndLastSeen()
                    }
                    // Clear pending credentials
                    pendingEmail = ""
                    pendingPassword = ""
                    pendingUsername = ""
                    pendingInGameId = ""
                    pendingVerificationStartedAtMs = null
                }
            }
        }
    }

    fun signIn(email: String, password: String) {
        signInJob?.cancel()
        _authState.value = AuthResult.Loading
        signInJob = viewModelScope.launch {
            authRepository.signIn(email, password).collect { result ->
                _authState.value = result
                if (result is AuthResult.Success) {
                    _isLoggedIn.value = true
                    // Eagerly connect Realtime WebSocket on sign-in
                    realtimeClient.connect()
                    handleProfileFetch(authRepository.getUserProfile())
                    // Update location on fresh login
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        authRepository.updateLocationAndLastSeen()
                    }
                    pendingVerificationStartedAtMs = null
                }
                // EmailNotVerified is handled by the UI layer
            }
        }
    }

    /** Call this when the user taps "I have verified" after clicking the email link. */
    fun confirmEmail() {
        confirmEmailJob?.cancel()
        _authState.value = AuthResult.Loading
        confirmEmailJob = viewModelScope.launch {
            authRepository.confirmEmail().collect { result ->
                _authState.value = result
                if (result is AuthResult.Success) {
                    _isLoggedIn.value = true
                    // Eagerly connect Realtime WebSocket on sign-in
                    realtimeClient.connect()
                    handleProfileFetch(authRepository.getUserProfile())
                    // Update location on fresh login
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        authRepository.updateLocationAndLastSeen()
                    }
                    pendingVerificationStartedAtMs = null
                }
            }
        }
    }

    /** Resend the confirmation email. */
    fun resendVerificationEmail(email: String) {
        resendEmailJob?.cancel()
        _resentSuccess.value = false
        resendEmailJob = viewModelScope.launch {
            authRepository.resendVerificationEmail(email).collect { result ->
                if (result is AuthResult.Success) {
                    _resentSuccess.value = true
                }
                // We don't emit this as authState; UI watches resentSuccess separately
            }
        }
    }

    fun sendPasswordResetOtp(email: String) {
        sendPasswordResetOtpJob?.cancel()
        _authState.value = AuthResult.Loading
        sendPasswordResetOtpJob = viewModelScope.launch {
            authRepository.sendPasswordResetOtp(email).collect { result ->
                _authState.value = result
            }
        }
    }

    fun verifyPasswordResetOtp(email: String, token: String, newPassword: String) {
        verifyPasswordResetOtpJob?.cancel()
        _authState.value = AuthResult.Loading
        verifyPasswordResetOtpJob = viewModelScope.launch {
            authRepository.verifyPasswordResetOtp(email, token, newPassword).collect { result ->
                _authState.value = result
                if (result is AuthResult.Success) {
                    _isLoggedIn.value = false
                    _userProfile.value = null
                    realtimeClient.disconnect()
                }
            }
        }
    }
    
    fun signOut() {
        signOutJob?.cancel()
        signOutJob = viewModelScope.launch {
            authRepository.signOut().collect { result ->
                if (result is AuthResult.Success) {
                    // Disconnect Realtime WebSocket to prevent stale auth state
                    realtimeClient.disconnect()
                    _isLoggedIn.value = false
                    _userProfile.value = null
                    pendingVerificationStartedAtMs = null
                    // Reset authState to Idle (NOT Success) to prevent
                    // the Login screen's LaunchedEffect from navigating back to Home
                    _authState.value = AuthResult.Idle
                }
            }
        }
    }
    
    fun deleteAccount() {
        deleteAccountJob?.cancel()
        _authState.value = AuthResult.Loading
        deleteAccountJob = viewModelScope.launch {
            authRepository.deleteAccount().collect { result ->
                if (result is AuthResult.Success) {
                    // Disconnect Realtime WebSocket — account no longer exists
                    realtimeClient.disconnect()
                    _isLoggedIn.value = false
                    _userProfile.value = null
                    pendingVerificationStartedAtMs = null
                    // Reset authState to Idle to prevent Login screen from
                    // navigating back to Home
                    _authState.value = AuthResult.Idle
                }
            }
        }
    }
    
    fun updateProfile(username: String, inGameId: String, role: String? = null, bio: String? = null, mainHeroes: List<String>? = null) {
        updateProfileJob?.cancel()
        updateProfileJob = viewModelScope.launch {
            authRepository.updateProfile(username, inGameId, role, bio, mainHeroes).collect { result ->
                _authState.value = result
                if (result is AuthResult.Success) {
                    handleProfileFetch(authRepository.getUserProfile())
                }
            }
        }
    }

    fun updateEmail(newEmail: String, currentPassword: String) {
        updateEmailJob?.cancel()
        updateEmailJob = viewModelScope.launch {
            authRepository.updateEmail(newEmail, currentPassword).collect { result ->
                _authState.value = result
                if (result is AuthResult.Success) {
                    handleProfileFetch(authRepository.getUserProfile())
                }
            }
        }
    }

    fun updatePassword(currentPassword: String, newPassword: String, confirmPassword: String) {
        updatePasswordJob?.cancel()
        updatePasswordJob = viewModelScope.launch {
            authRepository.updatePassword(currentPassword, newPassword, confirmPassword).collect { result ->
                _authState.value = result
            }
        }
    }

    fun uploadAvatar(uri: android.net.Uri) {
        uploadAvatarJob?.cancel()
        uploadAvatarJob = viewModelScope.launch {
            _authState.value = AuthResult.Loading
            try {
                val context = getApplication<Application>()
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                if (bytes == null) {
                    _authState.value = AuthResult.Error("Failed to read image")
                    return@launch
                }
                if (bytes.size > MAX_AVATAR_SIZE_BYTES) {
                    _authState.value = AuthResult.Error("Image is too large. Max size is 3MB.")
                    return@launch
                }
                authRepository.uploadAndSetAvatar(bytes, "image/jpeg").collect { result ->
                    _authState.value = result
                    if (result is AuthResult.Success) {
                        handleProfileFetch(authRepository.getUserProfile())
                    }
                }
            } catch (e: Exception) {
                _authState.value = AuthResult.Error("Upload failed: ${e.message}")
            }
        }
    }

    fun refreshProfile() {
        viewModelScope.launch {
            _isProfileRefreshing.value = true
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                authRepository.invalidateProfileCache()
            }
            val profile = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                authRepository.getUserProfile()
            }
            handleProfileFetch(profile)
            _isProfileRefreshing.value = false
        }
    }

    fun resetAuthState() {
        _authState.value = AuthResult.Idle
    }
}

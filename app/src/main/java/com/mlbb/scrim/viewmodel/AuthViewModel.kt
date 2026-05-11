package com.mlbb.scrim.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mlbb.scrim.data.model.AuthResult
import com.mlbb.scrim.data.model.UserProfile
import com.mlbb.scrim.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {
    
    private val authRepository = AuthRepository()
    
    private val _authState = MutableStateFlow<AuthResult>(AuthResult.Success)
    val authState: StateFlow<AuthResult> = _authState.asStateFlow()
    
    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()
    
    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()
    
    init {
        checkAuthStatus()
    }
    
    private fun checkAuthStatus() {
        _isLoggedIn.value = authRepository.isLoggedIn()
        _userProfile.value = authRepository.getUserProfile()
    }
    
    fun signUp(email: String, password: String, username: String, inGameId: String) {
        viewModelScope.launch {
            authRepository.signUp(email, password, username, inGameId).collect { result ->
                _authState.value = result
                if (result is AuthResult.Success) {
                    _isLoggedIn.value = true
                    _userProfile.value = authRepository.getUserProfile()
                }
            }
        }
    }
    
    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            authRepository.signIn(email, password).collect { result ->
                _authState.value = result
                if (result is AuthResult.Success) {
                    _isLoggedIn.value = true
                    _userProfile.value = authRepository.getUserProfile()
                }
            }
        }
    }
    
    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut().collect { result ->
                _authState.value = result
                if (result is AuthResult.Success) {
                    _isLoggedIn.value = false
                    _userProfile.value = null
                }
            }
        }
    }
    
    fun updateProfile(username: String, inGameId: String) {
        viewModelScope.launch {
            authRepository.updateProfile(username, inGameId).collect { result ->
                _authState.value = result
                if (result is AuthResult.Success) {
                    _userProfile.value = authRepository.getUserProfile()
                }
            }
        }
    }

    fun updateEmail(newEmail: String, currentPassword: String) {
        viewModelScope.launch {
            authRepository.updateEmail(newEmail, currentPassword).collect { result ->
                _authState.value = result
                if (result is AuthResult.Success) {
                    _userProfile.value = authRepository.getUserProfile()
                }
            }
        }
    }

    fun updatePassword(currentPassword: String, newPassword: String, confirmPassword: String) {
        viewModelScope.launch {
            authRepository.updatePassword(currentPassword, newPassword, confirmPassword).collect { result ->
                _authState.value = result
            }
        }
    }

    fun resetAuthState() {
        _authState.value = AuthResult.Success
    }
}

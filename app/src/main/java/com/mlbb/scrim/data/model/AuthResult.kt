package com.mlbb.scrim.data.model

sealed class AuthResult {
    /** Neutral state — no auth action has been taken yet. */
    data object Idle : AuthResult()
    data object Success : AuthResult()
    data class Error(val message: String) : AuthResult()
    data object Loading : AuthResult()
    /**
     * Emitted after sign-up when email confirmation is required.
     * The user must verify their email before full access is granted.
     */
    data class EmailNotVerified(val email: String) : AuthResult()
}

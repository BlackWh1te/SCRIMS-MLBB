package com.mlbb.scrim.viewmodel

import com.mlbb.scrim.data.repository.AuthRepository
import com.mlbb.scrim.data.model.UserProfile
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for AuthViewModel constants and AuthRepository time-based logic.
 * Does not require Android runtime.
 */
class AuthViewModelLogicTest {

    // ─── Constants verification ───

    @Test
    fun `USE_SUPABASE is true`() {
        assertTrue(AuthViewModel.USE_SUPABASE)
    }

    @Test
    fun `MAX_AVATAR_SIZE_BYTES is 3MB`() {
        assertEquals(3L * 1024 * 1024, AuthViewModel.MAX_AVATAR_SIZE_BYTES)
    }

    @Test
    fun `VERIFICATION_WINDOW_SECONDS is 3600`() {
        val field = AuthViewModel::class.java.getDeclaredField("VERIFICATION_WINDOW_SECONDS")
        field.isAccessible = true
        assertEquals(3_600L, field.getLong(null))
    }

    // ─── AuthRepository expiration logic ───

    @Test
    fun `isVerificationExpired returns false for fresh account`() {
        val repo = AuthRepository()
        val now = System.currentTimeMillis()
        val profile = UserProfile(
            email = "test@test.com",
            emailVerified = false,
            createdAt = now
        )
        // We need to set the profile inside the repo. Since userProfile is private,
        // we trigger signUp then check behavior indirectly, or we test via reflection.
        // Instead we test the pure logic on the profile via reflection on the method:
        val method = AuthRepository::class.java.getDeclaredMethod("isVerificationExpired")
        method.isAccessible = true

        // Set private field userProfile
        val profileField = AuthRepository::class.java.getDeclaredField("userProfile")
        profileField.isAccessible = true
        profileField.set(repo, profile)

        assertFalse(method.invoke(repo) as Boolean)
    }

    @Test
    fun `isVerificationExpired returns true after window plus one ms`() {
        val repo = AuthRepository()
        val now = System.currentTimeMillis()
        val profile = UserProfile(
            email = "test@test.com",
            emailVerified = false,
            createdAt = now - AuthRepository.VERIFICATION_WINDOW_MS - 1
        )
        val profileField = AuthRepository::class.java.getDeclaredField("userProfile")
        profileField.isAccessible = true
        profileField.set(repo, profile)

        val method = AuthRepository::class.java.getDeclaredMethod("isVerificationExpired")
        method.isAccessible = true
        assertTrue(method.invoke(repo) as Boolean)
    }

    @Test
    fun `secondsUntilDeletion returns 0 for verified account`() {
        val repo = AuthRepository()
        val profile = UserProfile(email = "test@test.com", emailVerified = true)
        val profileField = AuthRepository::class.java.getDeclaredField("userProfile")
        profileField.isAccessible = true
        profileField.set(repo, profile)

        val method = AuthRepository::class.java.getDeclaredMethod("secondsUntilDeletion")
        method.isAccessible = true
        assertEquals(0L, method.invoke(repo))
    }

    @Test
    fun `secondsUntilDeletion counts down correctly at boundary`() {
        val repo = AuthRepository()
        val now = System.currentTimeMillis()
        val profile = UserProfile(
            email = "test@test.com",
            emailVerified = false,
            createdAt = now - 1000L
        )
        val profileField = AuthRepository::class.java.getDeclaredField("userProfile")
        profileField.isAccessible = true
        profileField.set(repo, profile)

        val method = AuthRepository::class.java.getDeclaredMethod("secondsUntilDeletion")
        method.isAccessible = true
        val result = method.invoke(repo) as Long
        // Should be roughly 3599 seconds
        assertTrue(result in 3590..3600)
    }

    @Test
    fun `purgeIfExpired returns false when not expired`() = runBlocking {
        val repo = AuthRepository()
        val now = System.currentTimeMillis()
        val profile = UserProfile(
            email = "test@test.com",
            emailVerified = false,
            createdAt = now
        )
        val profileField = AuthRepository::class.java.getDeclaredField("userProfile")
        profileField.isAccessible = true
        profileField.set(repo, profile)

        val result = repo.purgeIfExpired()
        assertFalse(result)
    }

    @Test
    fun `purgeIfExpired returns true when expired`() = runBlocking {
        val repo = AuthRepository()
        val now = System.currentTimeMillis()
        val profile = UserProfile(
            email = "test@test.com",
            emailVerified = false,
            createdAt = now - AuthRepository.VERIFICATION_WINDOW_MS - 1
        )
        val profileField = AuthRepository::class.java.getDeclaredField("userProfile")
        profileField.isAccessible = true
        profileField.set(repo, profile)

        val result = repo.purgeIfExpired()
        assertTrue(result)
    }

    // ─── TeamViewModel constant ───

    @Test
    fun `MAX_LOGO_SIZE_BYTES is 3MB`() {
        val field = TeamViewModel::class.java.getDeclaredField("MAX_LOGO_SIZE_BYTES")
        field.isAccessible = true
        assertEquals(3L * 1024 * 1024, field.getLong(null))
    }
}

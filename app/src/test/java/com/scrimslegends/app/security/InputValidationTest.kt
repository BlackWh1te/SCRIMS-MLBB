package com.scrimslegends.app.security

import com.scrimslegends.app.data.model.*
import com.scrimslegends.app.data.repository.AuthRepository
import com.scrimslegends.app.data.repository.TeamRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

/**
 * Security-focused input validation tests.
 * Tests boundary conditions, injection attempts, and malformed input handling.
 */
class InputValidationTest {

    // ─── Email validation tests ───

    @Test
    fun `email without at sign rejected`() = runBlocking {
        val repo = AuthRepository()
        val results = mutableListOf<AuthResult>()
        repo.signUp("invalid-email", "password123", "User", "game123").collect { results.add(it) }
        assertTrue(results.any { it is AuthResult.Error })
    }

    @Test
    fun `email with multiple at signs accepted by basic check`() = runBlocking {
        // The mock only checks for @ presence, not validity
        val repo = AuthRepository()
        val results = mutableListOf<AuthResult>()
        repo.signUp("user@@example.com", "password123", "User", "game123").collect { results.add(it) }
        assertTrue(results.any { it is AuthResult.EmailNotVerified })
    }

    @Test
    fun `empty email rejected`() = runBlocking {
        val repo = AuthRepository()
        val results = mutableListOf<AuthResult>()
        repo.signUp("", "password123", "User", "game123").collect { results.add(it) }
        assertTrue(results.any { it is AuthResult.Error })
    }

    @Test
    fun `password of exactly 6 characters accepted`() = runBlocking {
        val repo = AuthRepository()
        val results = mutableListOf<AuthResult>()
        repo.signUp("user@example.com", "123456", "User", "game123").collect { results.add(it) }
        assertTrue(results.any { it is AuthResult.EmailNotVerified })
    }

    @Test
    fun `password of 5 characters rejected`() = runBlocking {
        val repo = AuthRepository()
        val results = mutableListOf<AuthResult>()
        repo.signUp("user@example.com", "12345", "User", "game123").collect { results.add(it) }
        assertTrue(results.any { it is AuthResult.Error })
    }

    @Test
    fun `blank username rejected`() = runBlocking {
        val repo = AuthRepository()
        val results = mutableListOf<AuthResult>()
        repo.signUp("user@example.com", "password123", "", "game123").collect { results.add(it) }
        assertTrue(results.any { it is AuthResult.Error })
    }

    @Test
    fun `whitespace-only username accepted by basic check`() = runBlocking {
        // The mock only checks isNotBlank, whitespace-only passes
        val repo = AuthRepository()
        val results = mutableListOf<AuthResult>()
        repo.signUp("user@example.com", "password123", "   ", "game123").collect { results.add(it) }
        // "   " is blank, so it should be rejected
        assertTrue(results.any { it is AuthResult.Error })
    }

    // ─── Team name validation ───

    @Test
    fun `createTeam with empty name succeeds in mock`() = runBlocking {
        val repo = TeamRepository()
        val result = repo.createTeam("", "leader1", isOpenForApplications = true).first()
        assertTrue(result.isSuccess)
    }

    @Test
    fun `createTeam with very long name succeeds in mock`() = runBlocking {
        val repo = TeamRepository()
        val longName = "A".repeat(1000)
        val result = repo.createTeam(longName, "leader1", isOpenForApplications = true).first()
        assertTrue(result.isSuccess)
    }

    // ─── ID injection tests ───

    @Test
    fun `getTeam with SQL injection pattern handled safely`() = runBlocking {
        val repo = TeamRepository()
        val result = repo.getTeam("' OR '1'='1").first()
        // Should return failure, not crash or return all data
        assertTrue(result.isFailure)
    }

    @Test
    fun `deleteTeam with malformed id handled safely`() = runBlocking {
        val repo = TeamRepository()
        val result = repo.deleteTeam("../../../etc/passwd").first()
        // Should succeed harmlessly (no matching team)
        assertTrue(result.isSuccess)
    }

    // ─── OTP token validation ───

    @Test
    fun `verifyOtp with exactly 8 digits succeeds`() = runBlocking {
        val repo = AuthRepository()
        repo.sendOtp("test@example.com", "User", "game123").first()
        val results = mutableListOf<AuthResult>()
        repo.verifyOtp("test@example.com", "12345678", "password123").collect { results.add(it) }
        assertTrue(results.any { it is AuthResult.Success })
    }

    @Test
    fun `verifyOtp with 7 digits fails`() = runBlocking {
        val repo = AuthRepository()
        repo.sendOtp("test@example.com", "User", "game123").first()
        val results = mutableListOf<AuthResult>()
        repo.verifyOtp("test@example.com", "1234567", "password123").collect { results.add(it) }
        assertTrue(results.any { it is AuthResult.Error })
    }

    @Test
    fun `verifyOtp with 9 digits fails`() = runBlocking {
        val repo = AuthRepository()
        repo.sendOtp("test@example.com", "User", "game123").first()
        val results = mutableListOf<AuthResult>()
        repo.verifyOtp("test@example.com", "123456789", "password123").collect { results.add(it) }
        assertTrue(results.any { it is AuthResult.Error })
    }

    @Test
    fun `verifyOtp with mixed alphanumeric fails`() = runBlocking {
        val repo = AuthRepository()
        repo.sendOtp("test@example.com", "User", "game123").first()
        val results = mutableListOf<AuthResult>()
        repo.verifyOtp("test@example.com", "1234abc8", "password123").collect { results.add(it) }
        assertTrue(results.any { it is AuthResult.Error })
    }

    @Test
    fun `verifyOtp with special characters fails`() = runBlocking {
        val repo = AuthRepository()
        repo.sendOtp("test@example.com", "User", "game123").first()
        val results = mutableListOf<AuthResult>()
        repo.verifyOtp("test@example.com", "1234!@#$", "password123").collect { results.add(it) }
        assertTrue(results.any { it is AuthResult.Error })
    }

    // ─── Profile field boundary tests ───

    @Test
    fun `UserProfile handles negative XP gracefully`() {
        val profile = UserProfile(xp = -1000)
        assertEquals(RankTier.BRONZE, profile.currentTier)
        assertTrue(profile.xpProgress in 0f..1f)
    }

    @Test
    fun `UserProfile handles negative matches`() {
        val profile = UserProfile(totalMatches = -5, wins = -1)
        // Win rate should not crash
        assertEquals("0%", profile.winRate)
        assertEquals(0f, profile.winRateFloat, 0.001f)
    }

    @Test
    fun `UserProfile handles wins greater than totalMatches`() {
        val profile = UserProfile(totalMatches = 5, wins = 10)
        assertEquals("200%", profile.winRate)
        assertEquals(200f, profile.winRateFloat, 0.001f)
    }

    @Test
    fun `UserProfile handles max int XP`() {
        val profile = UserProfile(xp = Int.MAX_VALUE)
        assertEquals(RankTier.MYTHIC, RankTier.fromXp(profile.xp))
        assertEquals(0, profile.xpToNext)
    }

    // ─── Team boundary tests ───

    @Test
    fun `Team handles zero maxPlayers`() {
        val team = Team(maxPlayers = 0, players = emptyList())
        assertFalse(team.canAddPlayer)
        assertTrue(team.isFull)
    }

    @Test
    fun `Team handles negative reputation`() {
        val team = Team(reputation = -10f)
        assertEquals("1.0", team.displayReputation)
    }

    @Test
    fun `Team handles very high reputation`() {
        val team = Team(reputation = 1000f)
        assertEquals("5.0", team.displayReputation)
    }

    @Test
    fun `Team handles exactly max players`() {
        val team = Team(maxPlayers = 5, players = List(5) { Player(id = "$it") })
        assertTrue(team.isFull)
        assertFalse(team.canAddPlayer)
        assertTrue(team.meetsMinPlayers)
    }

    // ─── Scrim boundary tests ───

    @Test
    fun `Scrim handles zero scheduled time`() {
        val scrim = Scrim(scheduledTime = 0)
        assertTrue(scrim.isChatOpen)
        assertTrue(scrim.isResultOverdue)
        assertTrue(scrim.isAutoCancelOverdue)
    }

    @Test
    fun `Scrim handles far future scheduled time`() {
        val future = System.currentTimeMillis() + 86400000 * 365 // 1 year
        val scrim = Scrim(scheduledTime = future)
        assertFalse(scrim.isChatOpen)
        assertFalse(scrim.isResultOverdue)
        assertFalse(scrim.isAutoCancelOverdue)
    }

    // ─── Message boundary tests ───

    @Test
    fun `Message handles empty content`() {
        val message = Message(content = "")
        assertEquals("", message.content)
    }

    @Test
    fun `Message handles very long content`() {
        val longContent = "A".repeat(100000)
        val message = Message(content = longContent)
        assertEquals(100000, message.content.length)
    }

    // ─── LfgPost boundary tests ───

    @Test
    fun `LfgPost handles empty social links`() {
        val post = LfgPost(discord = "", telegram = "", vk = "", facebook = "")
        assertEquals("", post.discord)
    }

    @Test
    fun `LfgPost handles negative totalMatches`() {
        val post = LfgPost(totalMatches = -5)
        assertEquals(-5, post.totalMatches)
    }
}

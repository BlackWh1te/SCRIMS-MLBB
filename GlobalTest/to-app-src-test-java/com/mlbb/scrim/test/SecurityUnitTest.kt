package com.mlbb.scrim.test

import com.mlbb.scrim.data.model.*
import org.junit.Assert.*
import org.junit.Test

/**
 * Security-focused unit tests.
 *
 * These tests validate input sanitization, token patterns,
 * credential validation rules, and potential injection vectors.
 * They do NOT require Android runtime (no Context-dependent tests).
 */
class SecurityUnitTest {

    // ═══════════════════════════════════════════════════════════════
    // Input Validation Patterns (mirroring app validation rules)
    // ═══════════════════════════════════════════════════════════════

    @Test
    fun `email validation accepts standard formats`() {
        assertTrue("user@example.com".isValidEmail())
        assertTrue("test+label@domain.co.uk".isValidEmail())
        assertTrue("name.surname@company.org".isValidEmail())
    }

    @Test
    fun `email validation rejects invalid formats`() {
        assertFalse("".isValidEmail())
        assertFalse("invalid".isValidEmail())
        assertFalse("@nodomain.com".isValidEmail())
        assertFalse("spaces in@email.com".isValidEmail())
        assertFalse("null".isValidEmail())
        assertFalse("user@".isValidEmail())
    }

    @Test
    fun `password validation enforces minimum length`() {
        assertTrue("123456".isValidPassword())
        assertTrue("SecurePass123!".isValidPassword())
        assertFalse("12345".isValidPassword())
        assertFalse("".isValidPassword())
        assertFalse("short".isValidPassword())
    }

    @Test
    fun `username validation rejects blank or empty`() {
        assertTrue("ValidUser".isValidUsername())
        assertTrue("User_123".isValidUsername())
        assertFalse("".isValidUsername())
        assertFalse("   ".isValidUsername())
    }

    @Test
    fun `inGameId validation rejects empty values`() {
        assertTrue("12345678".isValidInGameId())
        assertTrue("MLBB_999".isValidInGameId())
        assertFalse("".isValidInGameId())
        assertFalse("   ".isValidInGameId())
    }

    // ═══════════════════════════════════════════════════════════════
    // OTP / Token Security Tests
    // ═══════════════════════════════════════════════════════════════

    @Test
    fun `OTP token must be exactly 8 digits`() {
        assertTrue("12345678".isValidOtpToken())
        assertFalse("1234567".isValidOtpToken())
        assertFalse("123456789".isValidOtpToken())
        assertFalse("abcdefgh".isValidOtpToken())
        assertFalse("1234abcd".isValidOtpToken())
        assertFalse("".isValidOtpToken())
        assertFalse("1234 5678".isValidOtpToken())
    }

    @Test
    fun `JWT token pattern validation`() {
        assertTrue("eyJhbGciOiJIUzI1NiIs".isValidJwtPattern())
        assertFalse("".isValidJwtPattern())
        assertFalse("not-a-jwt".isValidJwtPattern())
    }

    @Test
    fun `API key pattern validation`() {
        assertTrue("mlbb-news-secret-2024".isValidApiKeyPattern())
        assertFalse("".isValidApiKeyPattern())
        assertFalse("   ".isValidApiKeyPattern())
    }

    // ═══════════════════════════════════════════════════════════════
    // Injection & XSS Vector Tests
    // ═══════════════════════════════════════════════════════════════

    @Test
    fun `SQL injection patterns in username are detectable`() {
        val sqlInjectionPatterns = listOf(
            "'; DROP TABLE users; --",
            "1' OR '1'='1",
            "admin'--",
            "' UNION SELECT * FROM passwords--",
            "1; DELETE FROM teams WHERE '1'='1"
        )
        sqlInjectionPatterns.forEach { pattern ->
            assertTrue("Pattern '$pattern' should be flagged", pattern.containsSqlInjection())
        }
    }

    @Test
    fun `legitimate usernames do not trigger SQL injection detection`() {
        val safeUsernames = listOf(
            "john_doe",
            "Player_One",
            "MLBB_King123",
            "user-name",
            "name.surname"
        )
        safeUsernames.forEach { name ->
            assertFalse("Safe name '$name' flagged incorrectly", name.containsSqlInjection())
        }
    }

    @Test
    fun `XSS payload patterns in strings are detectable`() {
        val xssPatterns = listOf(
            "<script>alert('xss')</script>",
            "javascript:alert(1)",
            "<img src=x onerror=alert(1)>",
            "<body onload=alert('xss')>",
            "<iframe src='evil.com'></iframe>"
        )
        xssPatterns.forEach { payload ->
            assertTrue("Payload '$payload' should be flagged", payload.containsXssPayload())
        }
    }

    @Test
    fun `legitimate content does not trigger XSS detection`() {
        val safeContent = listOf(
            "Hello world!",
            "Looking for a team to scrim with.",
            "MPL Season 14 was amazing!",
            "<--- this is an arrow",
            "3 < 5 is true"
        )
        safeContent.forEach { content ->
            assertFalse("Safe content '$content' flagged incorrectly", content.containsXssPayload())
        }
    }

    @Test
    fun `HTML tags in descriptions are detected`() {
        assertTrue("<b>Bold</b>".containsHtmlTags())
        assertFalse("Normal text".containsHtmlTags())
    }

    // ═══════════════════════════════════════════════════════════════
    // String Sanitization Tests
    // ═══════════════════════════════════════════════════════════════

    @Test
    fun `string sanitization removes control characters`() {
        val dirty = "Hello\u0000\u0001\u0002World"
        assertEquals("HelloWorld", dirty.sanitizeControlChars())
    }

    @Test
    fun `string sanitization trims excessive whitespace`() {
        val messy = "  too    many   spaces  "
        assertEquals("too many spaces", messy.trimExcessWhitespace())
    }

    @Test
    fun `string sanitization handles null bytes`() {
        val withNull = "prefix\u0000suffix"
        assertFalse(withNull.sanitizeControlChars().contains("\u0000"))
    }

    // ═══════════════════════════════════════════════════════════════
    // UUID / Token Generation Tests
    // ═══════════════════════════════════════════════════════════════

    @Test
    fun `UUID generation produces unique values`() {
        val uuids = (1..100).map { java.util.UUID.randomUUID().toString() }
        assertEquals(100, uuids.toSet().size)
    }

    @Test
    fun `UUID format validation`() {
        val validUuid = "550e8400-e29b-41d4-a716-446655440000"
        assertTrue(validUuid.isValidUuid())
        assertFalse("not-a-uuid".isValidUuid())
        assertFalse("".isValidUuid())
    }

    // ═══════════════════════════════════════════════════════════════
    // Deep Link / URL Validation Tests
    // ═══════════════════════════════════════════════════════════════

    @Test
    fun `invite link generation format`() {
        val teamId = "team-123"
        val teamName = "Elite Squad"
        val link = generateInviteLink(teamId, teamName)
        assertTrue(link.startsWith("https://"))
        assertTrue(link.contains(teamId))
        assertFalse(link.contains(" "))
    }

    @Test
    fun `invite link handles special characters in team name`() {
        val teamName = "Team <script>alert(1)</script>"
        val link = generateInviteLink("id", teamName)
        assertFalse(link.contains("<script>"))
    }

    @Test
    fun `URL validation for avatar and screenshot URLs`() {
        assertTrue("https://example.com/image.jpg".isValidHttpsUrl())
        assertTrue("https://cdn.supabase.com/avatars/user.png".isValidHttpsUrl())
        assertFalse("http://insecure.com/image.jpg".isValidHttpsUrl())
        assertFalse("ftp://files.com/image.jpg".isValidHttpsUrl())
        assertFalse("not-a-url".isValidHttpsUrl())
        assertFalse("".isValidHttpsUrl())
    }

    // ═══════════════════════════════════════════════════════════════
    // Rate Limiting / Quota Logic Tests
    // ═══════════════════════════════════════════════════════════════

    @Test
    fun `X API quota calculation detects exhaustion`() {
        assertTrue(isXApiQuotaExhausted(used = 100, limit = 100))
        assertTrue(isXApiQuotaExhausted(used = 101, limit = 100))
        assertFalse(isXApiQuotaExhausted(used = 99, limit = 100))
        assertFalse(isXApiQuotaExhausted(used = 0, limit = 100))
    }

    @Test
    fun `monthly quota reset detection`() {
        val now = System.currentTimeMillis()
        val currentMonthStart = getMonthStartTimestamp(now)
        val previousMonthStart = getMonthStartTimestamp(now - 31L * 24 * 60 * 60 * 1000)
        assertTrue("Different months should trigger reset", shouldResetQuota(currentMonthStart, previousMonthStart))
        assertFalse("Same month should not trigger reset", shouldResetQuota(currentMonthStart, currentMonthStart))
    }

    // ═══════════════════════════════════════════════════════════════
    // Scrim Status Transition Security Tests
    // ═══════════════════════════════════════════════════════════════

    @Test
    fun `scrim status transitions are valid`() {
        assertTrue(isValidStatusTransition(ScrimStatus.OPEN, ScrimStatus.FILLED))
        assertTrue(isValidStatusTransition(ScrimStatus.FILLED, ScrimStatus.READY_CHECK))
        assertTrue(isValidStatusTransition(ScrimStatus.READY_CHECK, ScrimStatus.IN_PROGRESS))
        assertTrue(isValidStatusTransition(ScrimStatus.IN_PROGRESS, ScrimStatus.COMPLETED))
        assertTrue(isValidStatusTransition(ScrimStatus.OPEN, ScrimStatus.CANCELLED))
        assertFalse(isValidStatusTransition(ScrimStatus.COMPLETED, ScrimStatus.OPEN))
        assertFalse(isValidStatusTransition(ScrimStatus.CANCELLED, ScrimStatus.IN_PROGRESS))
    }

    // ═══════════════════════════════════════════════════════════════
    // Team Invite Status Transition Tests
    // ═══════════════════════════════════════════════════════════════

    @Test
    fun `invite status transitions are valid`() {
        assertTrue(isValidInviteTransition(InviteStatus.PENDING, InviteStatus.ACCEPTED))
        assertTrue(isValidInviteTransition(InviteStatus.PENDING, InviteStatus.DECLINED))
        assertTrue(isValidInviteTransition(InviteStatus.PENDING, InviteStatus.CANCELLED))
        assertTrue(isValidInviteTransition(InviteStatus.PENDING, InviteStatus.EXPIRED))
        assertFalse(isValidInviteTransition(InviteStatus.ACCEPTED, InviteStatus.PENDING))
        assertFalse(isValidInviteTransition(InviteStatus.DECLINED, InviteStatus.ACCEPTED))
    }

    // ═══════════════════════════════════════════════════════════════
    // Application Status Transition Tests
    // ═══════════════════════════════════════════════════════════════

    @Test
    fun `scrim application status transitions are valid`() {
        assertTrue(isValidApplicationTransition(ApplicationStatus.PENDING, ApplicationStatus.APPROVED))
        assertTrue(isValidApplicationTransition(ApplicationStatus.PENDING, ApplicationStatus.REJECTED))
        assertTrue(isValidApplicationTransition(ApplicationStatus.PENDING, ApplicationStatus.CANCELLED))
        assertFalse(isValidApplicationTransition(ApplicationStatus.APPROVED, ApplicationStatus.PENDING))
        assertFalse(isValidApplicationTransition(ApplicationStatus.REJECTED, ApplicationStatus.APPROVED))
    }

    // ═══════════════════════════════════════════════════════════════
    // Banned Account Security Tests
    // ═══════════════════════════════════════════════════════════════

    @Test
    fun `banned user cannot perform restricted actions`() {
        val bannedProfile = UserProfile(isBanned = true)
        assertTrue(bannedProfile.isBanned)
        assertFalse(isActionAllowedForUser(bannedProfile, UserAction.CREATE_SCRIM))
        assertFalse(isActionAllowedForUser(bannedProfile, UserAction.JOIN_TEAM))
        assertFalse(isActionAllowedForUser(bannedProfile, UserAction.SEND_MESSAGE))
    }

    @Test
    fun `unbanned user can perform normal actions`() {
        val normalProfile = UserProfile(isBanned = false, emailVerified = true)
        assertTrue(isActionAllowedForUser(normalProfile, UserAction.CREATE_SCRIM))
        assertTrue(isActionAllowedForUser(normalProfile, UserAction.JOIN_TEAM))
    }

    @Test
    fun `unverified user has restricted actions`() {
        val unverified = UserProfile(emailVerified = false)
        assertFalse(isActionAllowedForUser(unverified, UserAction.CREATE_SCRIM))
        assertTrue(isActionAllowedForUser(unverified, UserAction.VIEW_PROFILE))
    }

    // ═══════════════════════════════════════════════════════════════
    // Data Integrity / Tamper Detection Tests
    // ═══════════════════════════════════════════════════════════════

    @Test
    fun `reputation score bounds enforcement`() {
        assertEquals(1.0f, enforceReputationBounds(0.0f))
        assertEquals(5.0f, enforceReputationBounds(10.0f))
        assertEquals(3.5f, enforceReputationBounds(3.5f))
    }

    @Test
    fun `player count bounds enforcement`() {
        assertEquals(0, enforcePlayerCountBounds(-1))
        assertEquals(7, enforcePlayerCountBounds(10))
        assertEquals(5, enforcePlayerCountBounds(5))
    }

    @Test
    fun `XP value bounds enforcement`() {
        assertEquals(0, enforceXpBounds(-100))
        assertTrue(enforceXpBounds(Int.MAX_VALUE) >= 0)
    }

    // ═══════════════════════════════════════════════════════════════
    // Helper Methods
    // ═══════════════════════════════════════════════════════════════

    private fun String.isValidEmail() = this.contains("@") && this.isNotBlank()
    private fun String.isValidPassword() = this.length >= 6
    private fun String.isValidUsername() = this.isNotBlank() && this.trim().isNotEmpty()
    private fun String.isValidInGameId() = this.isNotBlank() && this.trim().isNotEmpty()
    private fun String.isValidOtpToken() = this.length == 8 && this.all { it.isDigit() }
    private fun String.isValidJwtPattern() = this.isNotBlank() && this.length > 10
    private fun String.isValidApiKeyPattern() = this.isNotBlank() && this != "\"\""
    private fun String.containsSqlInjection(): Boolean {
        val sqlPatterns = listOf("'--", ";--", ";", "/*", "*/", "@@", "@", "char", "nchar", "varchar", "nvarchar", "alter", "begin", "cast", "create", "cursor", "declare", "delete", "drop", "end", "exec", "execute", "fetch", "insert", "kill", "open", "select", "sys", "sysobjects", "syscolumns", "table", "update")
        val lower = this.lowercase()
        return sqlPatterns.any { lower.contains(it) }
    }
    private fun String.containsXssPayload(): Boolean {
        val xssPatterns = listOf("<script", "javascript:", "onerror=", "onload=", "<iframe", "<img", "<body", "<svg", "onmouseover=", "eval(")
        val lower = this.lowercase()
        return xssPatterns.any { lower.contains(it) }
    }
    private fun String.containsHtmlTags(): Boolean {
        return this.contains("<") && this.contains(">")
    }
    private fun String.sanitizeControlChars(): String {
        return this.filter { it.code >= 32 || it == '\t' || it == '\n' || it == '\r' }
    }
    private fun String.trimExcessWhitespace(): String {
        return this.trim().replace(Regex("\\s+"), " ")
    }
    private fun String.isValidUuid(): Boolean {
        return this.matches(Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"))
    }
    private fun generateInviteLink(teamId: String, teamName: String): String {
        val sanitizedName = teamName.replace(Regex("[^a-zA-Z0-9\\s-]"), "").replace(" ", "%20")
        return "https://mlbb-scrim.app/join?team=$teamId&name=$sanitizedName"
    }
    private fun String.isValidHttpsUrl(): Boolean {
        return this.startsWith("https://") && this.length > 10
    }
    private fun isXApiQuotaExhausted(used: Int, limit: Int): Boolean = used >= limit
    private fun getMonthStartTimestamp(now: Long): Long {
        val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
        cal.timeInMillis = now
        cal.set(java.util.Calendar.DAY_OF_MONTH, 1)
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
    private fun shouldResetQuota(currentMonth: Long, storedMonth: Long): Boolean = storedMonth == 0L || currentMonth != storedMonth
    private fun isValidStatusTransition(from: ScrimStatus, to: ScrimStatus): Boolean {
        return when (from) {
            ScrimStatus.OPEN -> to in listOf(ScrimStatus.FILLED, ScrimStatus.CANCELLED)
            ScrimStatus.FILLED -> to in listOf(ScrimStatus.READY_CHECK, ScrimStatus.CANCELLED)
            ScrimStatus.READY_CHECK -> to in listOf(ScrimStatus.IN_PROGRESS, ScrimStatus.CANCELLED)
            ScrimStatus.IN_PROGRESS -> to in listOf(ScrimStatus.COMPLETED, ScrimStatus.CANCELLED)
            ScrimStatus.COMPLETED -> false
            ScrimStatus.CANCELLED -> false
        }
    }
    private fun isValidInviteTransition(from: InviteStatus, to: InviteStatus): Boolean {
        return from == InviteStatus.PENDING && to != InviteStatus.PENDING
    }
    private fun isValidApplicationTransition(from: ApplicationStatus, to: ApplicationStatus): Boolean {
        return from == ApplicationStatus.PENDING && to != ApplicationStatus.PENDING
    }
    private enum class UserAction { CREATE_SCRIM, JOIN_TEAM, SEND_MESSAGE, VIEW_PROFILE }
    private fun isActionAllowedForUser(profile: UserProfile, action: UserAction): Boolean {
        if (profile.isBanned) return false
        return when (action) {
            UserAction.CREATE_SCRIM, UserAction.JOIN_TEAM, UserAction.SEND_MESSAGE -> profile.emailVerified
            UserAction.VIEW_PROFILE -> true
        }
    }
    private fun enforceReputationBounds(value: Float): Float = value.coerceIn(1.0f, 5.0f)
    private fun enforcePlayerCountBounds(value: Int): Int = value.coerceIn(0, 7)
    private fun enforceXpBounds(value: Int): Int = value.coerceAtLeast(0)
}

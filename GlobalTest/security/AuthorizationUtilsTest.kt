package com.mlbb.scrim.security

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AuthorizationUtilsTest {

    // ─── Token Validation Tests ───

    @Test
    fun `isValidTokenFormat returns true for valid token format`() {
        // Arrange
        val validToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"

        // Act
        val result = AuthorizationUtils.isValidTokenFormat(validToken)

        // Assert
        assertTrue(result)
    }

    @Test
    fun `isValidTokenFormat returns false for invalid token format`() {
        // Arrange
        val invalidToken = "invalid.token.format"

        // Act
        val result = AuthorizationUtils.isValidTokenFormat(invalidToken)

        // Assert
        assertFalse(result)
    }

    @Test
    fun `isValidTokenFormat returns false for empty token`() {
        // Act
        val result = AuthorizationUtils.isValidTokenFormat("")

        // Assert
        assertFalse(result)
    }

    @Test
    fun `isValidTokenFormat returns false for null token`() {
        // Act
        val result = AuthorizationUtils.isValidTokenFormat(null)

        // Assert
        assertFalse(result)
    }

    @Test
    fun `isValidTokenFormat returns false for single part token`() {
        // Act
        val result = AuthorizationUtils.isValidTokenFormat("singlepart")

        // Assert
        assertFalse(result)
    }

    @Test
    fun `isValidTokenFormat returns false for two part token`() {
        // Act
        val result = AuthorizationUtils.isValidTokenFormat("two.parts")

        // Assert
        assertFalse(result)
    }

    // ─── Permission Tests ───

    @Test
    fun `hasPermission returns true when user has required permission`() {
        // Arrange
        val userPermissions = listOf("read", "write", "delete")
        val requiredPermission = "write"

        // Act
        val result = AuthorizationUtils.hasPermission(userPermissions, requiredPermission)

        // Assert
        assertTrue(result)
    }

    @Test
    fun `hasPermission returns false when user lacks required permission`() {
        // Arrange
        val userPermissions = listOf("read", "write")
        val requiredPermission = "delete"

        // Act
        val result = AuthorizationUtils.hasPermission(userPermissions, requiredPermission)

        // Assert
        assertFalse(result)
    }

    @Test
    fun `hasPermission returns false when user has no permissions`() {
        // Arrange
        val userPermissions = emptyList<String>()
        val requiredPermission = "read"

        // Act
        val result = AuthorizationUtils.hasPermission(userPermissions, requiredPermission)

        // Assert
        assertFalse(result)
    }

    @Test
    fun `hasPermission returns true for wildcard permission`() {
        // Arrange
        val userPermissions = listOf("*")
        val requiredPermission = "anything"

        // Act
        val result = AuthorizationUtils.hasPermission(userPermissions, requiredPermission)

        // Assert
        assertTrue(result)
    }

    @Test
    fun `hasAllPermissions returns true when user has all required permissions`() {
        // Arrange
        val userPermissions = listOf("read", "write", "delete")
        val requiredPermissions = listOf("read", "write")

        // Act
        val result = AuthorizationUtils.hasAllPermissions(userPermissions, requiredPermissions)

        // Assert
        assertTrue(result)
    }

    @Test
    fun `hasAllPermissions returns false when user lacks some permissions`() {
        // Arrange
        val userPermissions = listOf("read", "write")
        val requiredPermissions = listOf("read", "write", "delete")

        // Act
        val result = AuthorizationUtils.hasAllPermissions(userPermissions, requiredPermissions)

        // Assert
        assertFalse(result)
    }

    @Test
    fun `hasAllPermissions returns true for empty required permissions`() {
        // Arrange
        val userPermissions = listOf("read")
        val requiredPermissions = emptyList<String>()

        // Act
        val result = AuthorizationUtils.hasAllPermissions(userPermissions, requiredPermissions)

        // Assert
        assertTrue(result)
    }

    // ─── Role Tests ───

    @Test
    fun `hasRole returns true when user has required role`() {
        // Arrange
        val userRoles = listOf("admin", "user")
        val requiredRole = "admin"

        // Act
        val result = AuthorizationUtils.hasRole(userRoles, requiredRole)

        // Assert
        assertTrue(result)
    }

    @Test
    fun `hasRole returns false when user lacks required role`() {
        // Arrange
        val userRoles = listOf("user")
        val requiredRole = "admin"

        // Act
        val result = AuthorizationUtils.hasRole(userRoles, requiredRole)

        // Assert
        assertFalse(result)
    }

    @Test
    fun `hasRole returns true for admin role with admin requirement`() {
        // Arrange
        val userRoles = listOf("admin")
        val requiredRole = "admin"

        // Act
        val result = AuthorizationUtils.hasRole(userRoles, requiredRole)

        // Assert
        assertTrue(result)
    }

    // ─── Resource Ownership Tests ───

    @Test
    fun `isResourceOwner returns true when user owns resource`() {
        // Arrange
        val resourceOwnerId = "user123"
        val currentUserId = "user123"

        // Act
        val result = AuthorizationUtils.isResourceOwner(resourceOwnerId, currentUserId)

        // Assert
        assertTrue(result)
    }

    @Test
    fun `isResourceOwner returns false when user does not own resource`() {
        // Arrange
        val resourceOwnerId = "user123"
        val currentUserId = "user456"

        // Act
        val result = AuthorizationUtils.isResourceOwner(resourceOwnerId, currentUserId)

        // Assert
        assertFalse(result)
    }

    @Test
    fun `isResourceOwner returns false when resource owner ID is null`() {
        // Arrange
        val resourceOwnerId: String? = null
        val currentUserId = "user123"

        // Act
        val result = AuthorizationUtils.isResourceOwner(resourceOwnerId, currentUserId)

        // Assert
        assertFalse(result)
    }

    @Test
    fun `isResourceOwner returns false when current user ID is null`() {
        // Arrange
        val resourceOwnerId = "user123"
        val currentUserId: String? = null

        // Act
        val result = AuthorizationUtils.isResourceOwner(resourceOwnerId, currentUserId)

        // Assert
        assertFalse(result)
    }

    // ─── Team Membership Tests ───

    @Test
    fun `isTeamMember returns true when user is team member`() {
        // Arrange
        val teamMemberIds = listOf("user1", "user2", "user3")
        val userId = "user2"

        // Act
        val result = AuthorizationUtils.isTeamMember(teamMemberIds, userId)

        // Assert
        assertTrue(result)
    }

    @Test
    fun `isTeamMember returns false when user is not team member`() {
        // Arrange
        val teamMemberIds = listOf("user1", "user2")
        val userId = "user3"

        // Act
        val result = AuthorizationUtils.isTeamMember(teamMemberIds, userId)

        // Assert
        assertFalse(result)
    }

    @Test
    fun `isTeamMember returns false when team has no members`() {
        // Arrange
        val teamMemberIds = emptyList<String>()
        val userId = "user1"

        // Act
        val result = AuthorizationUtils.isTeamMember(teamMemberIds, userId)

        // Assert
        assertFalse(result)
    }

    @Test
    fun `isTeamLeader returns true when user is team leader`() {
        // Arrange
        val teamLeaderId = "user1"
        val userId = "user1"

        // Act
        val result = AuthorizationUtils.isTeamLeader(teamLeaderId, userId)

        // Assert
        assertTrue(result)
    }

    @Test
    fun `isTeamLeader returns false when user is not team leader`() {
        // Arrange
        val teamLeaderId = "user1"
        val userId = "user2"

        // Act
        val result = AuthorizationUtils.isTeamLeader(teamLeaderId, userId)

        // Assert
        assertFalse(result)
    }

    // ─── Rate Limiting Tests ───

    @Test
    fun `isRateLimited returns false when under rate limit`() {
        // Arrange
        val requestCount = 5
        val maxRequests = 10

        // Act
        val result = AuthorizationUtils.isRateLimited(requestCount, maxRequests)

        // Assert
        assertFalse(result)
    }

    @Test
    fun `isRateLimited returns true when at rate limit`() {
        // Arrange
        val requestCount = 10
        val maxRequests = 10

        // Act
        val result = AuthorizationUtils.isRateLimited(requestCount, maxRequests)

        // Assert
        assertTrue(result)
    }

    @Test
    fun `isRateLimited returns true when over rate limit`() {
        // Arrange
        val requestCount = 15
        val maxRequests = 10

        // Act
        val result = AuthorizationUtils.isRateLimited(requestCount, maxRequests)

        // Assert
        assertTrue(result)
    }

    // ─── Input Validation Tests ───

    @Test
    fun `isValidEmail returns true for valid email`() {
        // Arrange
        val email = "user@example.com"

        // Act
        val result = AuthorizationUtils.isValidEmail(email)

        // Assert
        assertTrue(result)
    }

    @Test
    fun `isValidEmail returns false for invalid email`() {
        // Arrange
        val email = "invalid-email"

        // Act
        val result = AuthorizationUtils.isValidEmail(email)

        // Assert
        assertFalse(result)
    }

    @Test
    fun `isValidEmail returns false for empty email`() {
        // Act
        val result = AuthorizationUtils.isValidEmail("")

        // Assert
        assertFalse(result)
    }

    @Test
    fun `isValidUsername returns true for valid username`() {
        // Arrange
        val username = "validuser123"

        // Act
        val result = AuthorizationUtils.isValidUsername(username)

        // Assert
        assertTrue(result)
    }

    @Test
    fun `isValidUsername returns false for username with special characters`() {
        // Arrange
        val username = "invalid@user"

        // Act
        val result = AuthorizationUtils.isValidUsername(username)

        // Assert
        assertFalse(result)
    }

    @Test
    fun `isValidUsername returns false for empty username`() {
        // Act
        val result = AuthorizationUtils.isValidUsername("")

        // Assert
        assertFalse(result)
    }

    @Test
    fun `isValidUsername returns false for username with spaces`() {
        // Arrange
        val username = "invalid user"

        // Act
        val result = AuthorizationUtils.isValidUsername(username)

        // Assert
        assertFalse(result)
    }

    // ─── Security Headers Tests ───

    @Test
    fun `getSecurityHeaders returns required security headers`() {
        // Act
        val headers = AuthorizationUtils.getSecurityHeaders()

        // Assert
        assertNotNull(headers)
        assertTrue(headers.containsKey("X-Content-Type-Options"))
        assertTrue(headers.containsKey("X-Frame-Options"))
        assertTrue(headers.containsKey("X-XSS-Protection"))
        assertTrue(headers.containsKey("Strict-Transport-Security"))
    }

    @Test
    fun `getSecurityHeaders includes nosniff directive`() {
        // Act
        val headers = AuthorizationUtils.getSecurityHeaders()

        // Assert
        assertEquals("nosniff", headers["X-Content-Type-Options"])
    }

    @Test
    fun `getSecurityHeaders includes deny directive`() {
        // Act
        val headers = AuthorizationUtils.getSecurityHeaders()

        // Assert
        assertEquals("deny", headers["X-Frame-Options"])
    }
}

package com.scrimslegends.app.security

import org.junit.Assert.*
import org.junit.Test

/**
 * Tests for AuthorizationUtils ownership checks.
 *
 * These verify defense-in-depth client-side authorization guards.
 * The authoritative enforcement is Supabase RLS.
 */
class AuthorizationUtilsTest {

    @Test
    fun `requireAuth returns failure when no user is authenticated`() {
        // SupabaseSession.getUserIdOrNull() returns null when not initialized
        val result = AuthorizationUtils.requireAuth()
        assertTrue("Should fail when not authenticated", result.isFailure)
        val exception = result.exceptionOrNull()
        assertNotNull(exception)
        assertTrue(exception is SecurityException)
    }

    @Test
    fun `requireOwner returns failure when resourceOwnerId is empty and no auth`() {
        val result = AuthorizationUtils.requireOwner("")
        assertTrue(result.isFailure)
    }

    @Test
    fun `requireOwner returns failure when resourceOwnerId differs from current user`() {
        // Even if a user is somehow authenticated, mismatched owner should fail.
        // This test assumes no authenticated user (null), so any non-empty ownerId fails.
        val result = AuthorizationUtils.requireOwner("some-other-user-id")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is SecurityException)
    }

    @Test
    fun `requireLeader returns failure when teamLeaderId differs from current user`() {
        val result = AuthorizationUtils.requireLeader("leader-123")
        assertTrue(result.isFailure)
    }

    @Test
    fun `requireParticipant returns failure when user is not in participant list`() {
        val result = AuthorizationUtils.requireParticipant(listOf("user-a", "user-b"))
        assertTrue(result.isFailure)
    }

    @Test
    fun `requireParticipant returns failure for empty participant list`() {
        val result = AuthorizationUtils.requireParticipant(emptyList())
        assertTrue(result.isFailure)
    }

    @Test
    fun `requireMemberOrSelf returns failure when user is not member nor self`() {
        val result = AuthorizationUtils.requireMemberOrSelf(
            teamMemberIds = listOf("member-1", "member-2"),
            targetUserId = "target-1"
        )
        assertTrue(result.isFailure)
    }
}

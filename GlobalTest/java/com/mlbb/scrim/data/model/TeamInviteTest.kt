package com.mlbb.scrim.data.model

import org.junit.Assert.*
import org.junit.Test

class TeamInviteTest {

    @Test
    fun `default status is PENDING`() {
        assertEquals(InviteStatus.PENDING, TeamInvite().status)
    }

    @Test
    fun `InviteStatus has all expected values`() {
        val expected = setOf(
            InviteStatus.PENDING,
            InviteStatus.ACCEPTED,
            InviteStatus.DECLINED,
            InviteStatus.EXPIRED,
            InviteStatus.CANCELLED
        )
        assertEquals(expected, InviteStatus.values().toSet())
    }

    @Test
    fun `default createdAt is near current time`() {
        val before = System.currentTimeMillis()
        val invite = TeamInvite()
        val after = System.currentTimeMillis()
        assertTrue(invite.createdAt in before..after)
    }

    @Test
    fun `respondedAt defaults to null`() {
        assertNull(TeamInvite().respondedAt)
    }

    @Test
    fun `team fields default to empty`() {
        val invite = TeamInvite()
        assertEquals("", invite.teamId)
        assertEquals("", invite.teamName)
        assertEquals("", invite.invitedBy)
        assertEquals("", invite.invitedByName)
        assertEquals("", invite.invitedUserId)
        assertEquals("", invite.invitedUserName)
    }

    @Test
    fun `copy changes status correctly`() {
        val invite = TeamInvite(status = InviteStatus.PENDING)
        val updated = invite.copy(status = InviteStatus.ACCEPTED)
        assertEquals(InviteStatus.ACCEPTED, updated.status)
        assertEquals(InviteStatus.PENDING, invite.status)
    }
}

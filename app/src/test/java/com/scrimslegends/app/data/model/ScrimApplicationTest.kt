package com.scrimslegends.app.data.model

import org.junit.Assert.*
import org.junit.Test

class ScrimApplicationTest {

    @Test
    fun `default status is PENDING`() {
        assertEquals(ApplicationStatus.PENDING, ScrimApplication().status)
    }

    @Test
    fun `ApplicationStatus has expected values`() {
        val expected = setOf(
            ApplicationStatus.PENDING,
            ApplicationStatus.APPROVED,
            ApplicationStatus.REJECTED,
            ApplicationStatus.CANCELLED
        )
        assertEquals(expected, ApplicationStatus.values().toSet())
    }

    @Test
    fun `TeamApplicationStatus has expected values`() {
        val expected = setOf(
            TeamApplicationStatus.PENDING,
            TeamApplicationStatus.ACCEPTED,
            TeamApplicationStatus.DECLINED
        )
        assertEquals(expected, TeamApplicationStatus.values().toSet())
    }

    @Test
    fun `default fields are empty or zero`() {
        val app = ScrimApplication()
        assertEquals("", app.id)
        assertEquals("", app.scrimId)
        assertEquals("", app.applicantTeamId)
        assertEquals("", app.applicantTeamName)
        assertEquals("", app.applicantTeamLeader)
        assertEquals("", app.applicantTeamLeaderName)
        assertNull(app.notes)
        assertNull(app.respondedAt)
    }

    @Test
    fun `default appliedAt is near current time`() {
        val before = System.currentTimeMillis()
        val app = ScrimApplication()
        val after = System.currentTimeMillis()
        assertTrue(app.appliedAt in before..after)
    }

    @Test
    fun `copy changes status correctly`() {
        val app = ScrimApplication(status = ApplicationStatus.PENDING)
        val updated = app.copy(status = ApplicationStatus.APPROVED)
        assertEquals(ApplicationStatus.APPROVED, updated.status)
        assertEquals(ApplicationStatus.PENDING, app.status)
    }
}

package com.mlbb.scrim.data.model

import org.junit.Assert.*
import org.junit.Test

class TeamRoleExtendedTest {

    @Test
    fun `role constants are not blank`() {
        assertEquals("Leader", TeamRole.LEADER)
        assertEquals("Co-Leader", TeamRole.CO_LEADER)
        assertEquals("Member", TeamRole.MEMBER)
        assertEquals("Invited", TeamRole.INVITED)
    }

    @Test
    fun `role constants are distinct`() {
        val roles = setOf(TeamRole.LEADER, TeamRole.CO_LEADER, TeamRole.MEMBER, TeamRole.INVITED)
        assertEquals("All role constants should be unique", 4, roles.size)
    }

    @Test
    fun `TeamRole enum count matches constants`() {
        assertEquals(4, TeamRole.values().size)
    }

    @Test
    fun `TeamRole values are correct`() {
        val values = TeamRole.values()
        assertTrue(values.contains(TeamRole.LEADER))
        assertTrue(values.contains(TeamRole.CO_LEADER))
        assertTrue(values.contains(TeamRole.MEMBER))
        assertTrue(values.contains(TeamRole.INVITED))
    }

    @Test
    fun `TeamRole valueOf works for all values`() {
        TeamRole.values().forEach { role ->
            assertEquals(role, java.lang.Enum.valueOf(TeamRole::class.java, role.name))
        }
    }
}

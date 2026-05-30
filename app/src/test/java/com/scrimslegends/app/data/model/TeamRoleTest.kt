package com.scrimslegends.app.data.model

import org.junit.Assert.*
import org.junit.Test

class TeamRoleTest {

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
}

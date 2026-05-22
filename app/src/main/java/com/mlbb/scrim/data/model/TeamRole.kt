package com.mlbb.scrim.data.model

/**
 * Database-level role strings used in team_members table.
 * Use these instead of raw string literals to prevent typos.
 */
object TeamRole {
    const val LEADER = "Leader"
    const val CO_LEADER = "Co-Leader"
    const val MEMBER = "Member"
    const val INVITED = "Invited"
}

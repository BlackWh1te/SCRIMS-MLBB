package com.scrimslegends.app.data.service

import com.scrimslegends.app.BuildConfig
import org.junit.Assert.*
import org.junit.Test

class SupabaseConfigTest {

    @Test
    fun `SUPABASE_URL is not empty`() {
        assertTrue(SupabaseConfig.SUPABASE_URL.isNotBlank())
    }

    @Test
    fun `SUPABASE_ANON_KEY is not empty`() {
        assertTrue(SupabaseConfig.SUPABASE_ANON_KEY.isNotBlank())
    }

    @Test
    fun `REST_API_URL ends with rest v1`() {
        assertTrue(SupabaseConfig.REST_API_URL.endsWith("/rest/v1/"))
    }

    @Test
    fun `AUTH_API_URL ends with auth v1`() {
        assertTrue(SupabaseConfig.AUTH_API_URL.endsWith("/auth/v1/"))
    }

    @Test
    fun `table names are not empty`() {
        assertTrue(SupabaseConfig.TABLE_PROFILES.isNotBlank())
        assertTrue(SupabaseConfig.TABLE_TEAMS.isNotBlank())
        assertTrue(SupabaseConfig.TABLE_TEAM_MEMBERS.isNotBlank())
        assertTrue(SupabaseConfig.TABLE_TEAM_INVITATIONS.isNotBlank())
        assertTrue(SupabaseConfig.TABLE_PLAYER_STATS.isNotBlank())
        assertTrue(SupabaseConfig.TABLE_SCRIMS.isNotBlank())
        assertTrue(SupabaseConfig.TABLE_SCRIM_APPLICATIONS.isNotBlank())
        assertTrue(SupabaseConfig.TABLE_SCRIM_ROSTERS.isNotBlank())
        assertTrue(SupabaseConfig.TABLE_MATCHES.isNotBlank())
        assertTrue(SupabaseConfig.TABLE_MESSAGES.isNotBlank())
        assertTrue(SupabaseConfig.TABLE_MATCH_RESULTS.isNotBlank())
        assertTrue(SupabaseConfig.TABLE_LFG_POSTS.isNotBlank())
        assertTrue(SupabaseConfig.TABLE_CONVERSATIONS.isNotBlank())
        assertTrue(SupabaseConfig.TABLE_NOTIFICATIONS.isNotBlank())
    }

    @Test
    fun `bucket names are not empty`() {
        assertTrue(SupabaseConfig.BUCKET_SCREENSHOTS.isNotBlank())
        assertTrue(SupabaseConfig.BUCKET_AVATARS.isNotBlank())
        assertTrue(SupabaseConfig.BUCKET_TEAM_LOGOS.isNotBlank())
        assertTrue(SupabaseConfig.BUCKET_LFG_SCREENSHOTS.isNotBlank())
    }

    @Test
    fun `table names follow lowercase snake_case convention`() {
        val tables = listOf(
            SupabaseConfig.TABLE_PROFILES,
            SupabaseConfig.TABLE_TEAMS,
            SupabaseConfig.TABLE_TEAM_MEMBERS,
            SupabaseConfig.TABLE_TEAM_INVITATIONS,
            SupabaseConfig.TABLE_PLAYER_STATS,
            SupabaseConfig.TABLE_SCRIMS,
            SupabaseConfig.TABLE_SCRIM_APPLICATIONS,
            SupabaseConfig.TABLE_SCRIM_ROSTERS,
            SupabaseConfig.TABLE_MATCHES,
            SupabaseConfig.TABLE_MESSAGES,
            SupabaseConfig.TABLE_MATCH_RESULTS,
            SupabaseConfig.TABLE_LFG_POSTS,
            SupabaseConfig.TABLE_CONVERSATIONS,
            SupabaseConfig.TABLE_NOTIFICATIONS
        )
        tables.forEach { table ->
            assertTrue("Table name '$table' should be lowercase", table == table.lowercase())
            assertFalse("Table name '$table' should not contain spaces", table.contains(" "))
        }
    }

    @Test
    fun `BuildConfig fields are accessible`() {
        // Verify the BuildConfig values exist and are strings
        assertNotNull(BuildConfig.SUPABASE_URL)
        assertNotNull(BuildConfig.SUPABASE_ANON_KEY)
    }
}

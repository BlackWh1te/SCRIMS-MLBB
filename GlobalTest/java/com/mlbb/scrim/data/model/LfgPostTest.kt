package com.mlbb.scrim.data.model

import org.junit.Assert.*
import org.junit.Test

class LfgPostTest {

    @Test
    fun `default LfgPost has empty fields`() {
        val post = LfgPost()
        assertEquals("", post.id)
        assertEquals("", post.playerId)
        assertEquals("", post.playerName)
        assertTrue(post.mainHeroes.isEmpty())
        assertTrue(post.playstyleTags.isEmpty())
    }

    @Test
    fun `default role is FLEX`() {
        assertEquals(GameRole.FLEX, LfgPost().role)
    }

    @Test
    fun `default region is UTC`() {
        assertEquals(Region.UTC, LfgPost().region)
    }

    @Test
    fun `default skillLevel is ALL`() {
        assertEquals(SkillLevel.ALL, LfgPost().skillLevel)
    }

    @Test
    fun `default isAvailable is true`() {
        assertTrue(LfgPost().isAvailable)
    }

    @Test
    fun `default useMic is false`() {
        assertFalse(LfgPost().useMic)
    }

    @Test
    fun `GameRole enum has all roles`() {
        val roles = GameRole.values()
        assertTrue(roles.contains(GameRole.TANK))
        assertTrue(roles.contains(GameRole.FIGHTER))
        assertTrue(roles.contains(GameRole.ASSASSIN))
        assertTrue(roles.contains(GameRole.MAGE))
        assertTrue(roles.contains(GameRole.MARKSMAN))
        assertTrue(roles.contains(GameRole.SUPPORT))
        assertTrue(roles.contains(GameRole.FLEX))
    }

    @Test
    fun `all GameRoles have non-empty display names`() {
        GameRole.values().forEach { role ->
            assertTrue("Role ${role.name} should have non-empty displayName",
                role.displayName.isNotBlank())
        }
    }

    @Test
    fun `GameRole display names are unique`() {
        val names = GameRole.values().map { it.displayName }
        assertEquals(GameRole.values().size, names.toSet().size)
    }

    @Test
    fun `LfgPost social links default to empty`() {
        val post = LfgPost()
        assertEquals("", post.discord)
        assertEquals("", post.telegram)
        assertEquals("", post.vk)
        assertEquals("", post.facebook)
    }

    @Test
    fun `preferredModes defaults to empty`() {
        assertTrue(LfgPost().preferredModes.isEmpty())
    }

    @Test
    fun `city defaults to empty`() {
        assertEquals("", LfgPost().city)
    }

    @Test
    fun `winRate defaults to empty`() {
        assertEquals("", LfgPost().winRate)
    }

    @Test
    fun `rankedWinRate defaults to empty`() {
        assertEquals("", LfgPost().rankedWinRate)
    }

    @Test
    fun `totalMatches defaults to 0`() {
        assertEquals(0, LfgPost().totalMatches)
    }
}

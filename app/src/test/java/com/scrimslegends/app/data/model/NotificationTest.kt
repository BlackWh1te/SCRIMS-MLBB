package com.scrimslegends.app.data.model

import org.junit.Assert.*
import org.junit.Test

class NotificationTest {

    @Test
    fun `icon mapping for SCRIM_INVITE`() {
        val n = Notification(type = NotificationType.SCRIM_INVITE)
        assertEquals("sports_esports", n.icon)
    }

    @Test
    fun `icon mapping for MATCH_RESULT`() {
        val n = Notification(type = NotificationType.MATCH_RESULT)
        assertEquals("emoji_events", n.icon)
    }

    @Test
    fun `icon mapping for TEAM_INVITE`() {
        val n = Notification(type = NotificationType.TEAM_INVITE)
        assertEquals("group", n.icon)
    }

    @Test
    fun `icon mapping for MESSAGE`() {
        val n = Notification(type = NotificationType.MESSAGE)
        assertEquals("chat", n.icon)
    }

    @Test
    fun `icon mapping for SYSTEM`() {
        val n = Notification(type = NotificationType.SYSTEM)
        assertEquals("info", n.icon)
    }

    @Test
    fun `icon mapping for XP_GAIN`() {
        val n = Notification(type = NotificationType.XP_GAIN)
        assertEquals("trending_up", n.icon)
    }

    @Test
    fun `icon mapping for TIER_UP`() {
        val n = Notification(type = NotificationType.TIER_UP)
        assertEquals("star", n.icon)
    }

    @Test
    fun `default notification type is SYSTEM`() {
        assertEquals(NotificationType.SYSTEM, Notification().type)
    }

    @Test
    fun `default isRead is false`() {
        assertFalse(Notification().isRead)
    }

    @Test
    fun `all notification types have non-empty icons`() {
        val icons = NotificationType.values().map { Notification(type = it).icon }
        assertTrue(icons.all { it.isNotBlank() })
    }

    @Test
    fun `NotificationType contains expected core values`() {
        val expected = setOf(
            NotificationType.SCRIM_INVITE,
            NotificationType.MATCH_RESULT,
            NotificationType.TEAM_INVITE,
            NotificationType.MESSAGE,
            NotificationType.SYSTEM,
            NotificationType.XP_GAIN,
            NotificationType.TIER_UP
        )
        assertTrue(NotificationType.values().toSet().containsAll(expected))
    }
}

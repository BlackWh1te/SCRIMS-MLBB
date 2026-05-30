package com.mlbb.scrim.data.model

import org.junit.Assert.*
import org.junit.Test

class MessageTest {

    @Test
    fun `default message type is TEXT`() {
        assertEquals(MessageType.TEXT, Message().type)
    }

    @Test
    fun `default isRead is false`() {
        assertFalse(Message().isRead)
    }

    @Test
    fun `default timestamp is near current time`() {
        val before = System.currentTimeMillis()
        val message = Message()
        val after = System.currentTimeMillis()
        assertTrue(message.timestamp in before..after)
    }

    @Test
    fun `conversation timeUntilChatOpens returns 0 when chat is open`() {
        val past = System.currentTimeMillis() - 3600000
        val conversation = Conversation(chatOpensAt = past)
        assertEquals(0L, conversation.timeUntilChatOpens)
    }

    @Test
    fun `conversation isChatOpenNow returns true when time passed`() {
        val past = System.currentTimeMillis() - 3600000
        val conversation = Conversation(chatOpensAt = past)
        assertTrue(conversation.isChatOpenNow)
    }

    @Test
    fun `conversation isChatOpenNow returns false when time not reached`() {
        val future = System.currentTimeMillis() + 3600000
        val conversation = Conversation(chatOpensAt = future)
        assertFalse(conversation.isChatOpenNow)
    }

    @Test
    fun `isOtherTyping returns participantB typing for participantA`() {
        val conversation = Conversation(
            participantAId = "userA",
            participantBId = "userB",
            isParticipantBTyping = true,
            isParticipantATyping = false
        )
        assertTrue(conversation.isOtherTyping("userA"))
    }

    @Test
    fun `isOtherTyping returns participantA typing for participantB`() {
        val conversation = Conversation(
            participantAId = "userA",
            participantBId = "userB",
            isParticipantATyping = true,
            isParticipantBTyping = false
        )
        assertTrue(conversation.isOtherTyping("userB"))
    }

    @Test
    fun `isOtherTyping returns false when self typing`() {
        val conversation = Conversation(
            participantAId = "userA",
            participantBId = "userB",
            isParticipantATyping = true,
            isParticipantBTyping = false
        )
        assertFalse(conversation.isOtherTyping("userA"))
    }

    @Test
    fun `isOtherTyping handles unknown user gracefully`() {
        val conversation = Conversation(
            participantAId = "userA",
            participantBId = "userB"
        )
        assertFalse(conversation.isOtherTyping("unknown"))
    }

    @Test
    fun `conversation defaults`() {
        val conv = Conversation()
        assertEquals("", conv.id)
        assertEquals("", conv.scrimId)
        assertTrue(conv.messages.isEmpty())
        assertEquals(0, conv.unreadCount)
        assertTrue(conv.isChatLocked)
        assertTrue(conv.adminVisible)
    }

    @Test
    fun `MessageType enum has all values`() {
        val values = MessageType.values()
        assertTrue(values.contains(MessageType.TEXT))
        assertTrue(values.contains(MessageType.SYSTEM))
        assertTrue(values.contains(MessageType.APPLY))
        assertTrue(values.contains(MessageType.IMAGE))
        assertTrue(values.contains(MessageType.VOICE))
    }
}

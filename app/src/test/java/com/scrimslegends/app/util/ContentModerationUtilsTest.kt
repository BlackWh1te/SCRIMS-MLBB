package com.scrimslegends.app.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContentModerationUtilsTest {

    @Test
    fun `hello is not flagged as profanity`() {
        assertFalse(ContentModerationUtils.containsProfanity("hello team"))
        assertFalse(ContentModerationUtils.containsProfanity("shell strategy"))
    }

    @Test
    fun `chat validation allows normal greetings`() {
        val result = ContentModerationUtils.validateChatMessage("hello warriors")
        assertTrue(result is ContentModerationUtils.ValidationResult.Valid)
    }

    @Test
    fun `chat validation still catches obfuscated profanity`() {
        assertTrue(ContentModerationUtils.containsProfanity("f u c k"))
        assertTrue(ContentModerationUtils.containsProfanity("what the hell"))
        assertTrue(ContentModerationUtils.containsProfanity("kill yourself"))
    }

    @Test
    fun `chat validation does not hard-block profanity matches`() {
        val result = ContentModerationUtils.validateChatMessage("what the hell")
        assertTrue(result is ContentModerationUtils.ValidationResult.Valid)
    }
}

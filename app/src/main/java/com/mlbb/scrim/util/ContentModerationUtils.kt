package com.mlbb.scrim.util

/**
 * Client-side content moderation utilities.
 *
 * These checks run locally before content reaches the server.
 * They are a first-line defense; the backend also enforces rules.
 *
 * - Profanity filter: blocks or masks common offensive words in chat
 * - Spam detection: caps lock, excessive repetition
 */
object ContentModerationUtils {

    // Basic profanity word list (English). Expand per locale as needed.
    private val PROFANITY_LIST = setOf(
        "fuck", "shit", "bitch", "asshole", "damn", "cunt", "dick", "cock",
        "pussy", "whore", "slut", "nigger", "faggot", "retard", "kill yourself",
        "kys", "stupid", "idiot", "moron", "dumbass", "bastard", "hell"
    )

    /**
     * Checks if [text] contains any profanity.
     * Comparison is case-insensitive and catches simple obfuscation like
     * "f*ck", "f u c k", "fck".
     */
    fun containsProfanity(text: String): Boolean {
        val normalized = text.lowercase()
            .replace(Regex("[^a-z\\s]"), "") // strip symbols
            .replace(Regex("\\s+"), "")      // strip spaces
        return PROFANITY_LIST.any { normalized.contains(it) }
    }

    /**
     * Masks profanity in [text] with asterisks.
     * Returns the cleaned text.
     */
    fun maskProfanity(text: String): String {
        var result = text
        PROFANITY_LIST.forEach { word ->
            val regex = Regex("(?i)\\b${Regex.escape(word)}\\b")
            result = regex.replace(result, "*".repeat(word.length))
        }
        return result
    }

    /**
     * Validates a chat message before sending.
     * Returns a ValidationResult: either Valid or Blocked with a reason.
     */
    fun validateChatMessage(text: String): ValidationResult {
        val trimmed = text.trim()
        if (trimmed.isBlank()) {
            return ValidationResult.Blocked("Message cannot be empty")
        }
        if (trimmed.length > 2000) {
            return ValidationResult.Blocked("Message is too long (max 2000 characters)")
        }
        if (containsProfanity(trimmed)) {
            return ValidationResult.Blocked("Message contains inappropriate language")
        }
        // Spam: more than 80% caps
        val letters = trimmed.filter { it.isLetter() }
        if (letters.length > 5 && letters.count { it.isUpperCase() } / letters.length.toFloat() > 0.8f) {
            return ValidationResult.Blocked("Please don't shout (too many capitals)")
        }
        // Spam: excessive repetition
        if (Regex("(.)\\1{9,}").containsMatchIn(trimmed)) {
            return ValidationResult.Blocked("Excessive character repetition detected")
        }
        return ValidationResult.Valid
    }

    /**
     * Validates a team name or username.
     */
    fun validateUsername(text: String): ValidationResult {
        val trimmed = text.trim()
        if (trimmed.length < 2) return ValidationResult.Blocked("Too short")
        if (trimmed.length > 30) return ValidationResult.Blocked("Too long (max 30)")
        if (containsProfanity(trimmed)) return ValidationResult.Blocked("Inappropriate language")
        return ValidationResult.Valid
    }

    sealed class ValidationResult {
        object Valid : ValidationResult()
        data class Blocked(val reason: String) : ValidationResult()
    }
}

package com.scrimslegends.app.util

/**
 * Client-side content moderation utilities.
 *
 * These checks run locally before content reaches the server.
 * They are a first-line defense; the backend also enforces rules.
 *
 * - Profanity filter: blocks usernames and can mask content for moderation UI
 * - Spam detection: caps lock, excessive repetition
 */
object ContentModerationUtils {

    // Basic profanity word list (English). Expand per locale as needed.
    private val PROFANITY_LIST = setOf(
        "fuck", "shit", "bitch", "asshole", "damn", "cunt", "dick", "cock",
        "pussy", "whore", "slut", "nigger", "faggot", "retard", "kill yourself",
        "kys", "stupid", "idiot", "moron", "dumbass", "bastard", "hell"
    )
    private val PROFANITY_PATTERNS = PROFANITY_LIST.map { term -> buildProfanityPattern(term) }

    private fun buildProfanityPattern(term: String): Regex {
        val body = term.trim()
            .lowercase()
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .joinToString("[^a-z0-9]*") { token ->
                token.map { char -> Regex.escape(char.toString()) }
                    .joinToString("[^a-z0-9]*")
            }

        return Regex("(?<![a-z0-9])$body(?![a-z0-9])", RegexOption.IGNORE_CASE)
    }

    /**
     * Checks if [text] contains any profanity.
     * Comparison is case-insensitive and catches simple obfuscation like
     * "f*ck", "f u c k", and avoids substring hits like "hello".
     */
    fun containsProfanity(text: String): Boolean {
        return PROFANITY_PATTERNS.any { it.containsMatchIn(text) }
    }

    /**
     * Masks profanity in [text] with asterisks.
     * Returns the cleaned text.
     */
    fun maskProfanity(text: String): String {
        var result = text
        PROFANITY_PATTERNS.forEach { regex ->
            result = regex.replace(result) { match -> "*".repeat(match.value.length) }
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

    /**
     * Validates an in-game ID before it is stored.
     */
    fun validateInGameId(text: String): ValidationResult {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return ValidationResult.Blocked("In-game ID cannot be empty")
        if (trimmed.length > 30) return ValidationResult.Blocked("In-game ID is too long (max 30 characters)")
        return ValidationResult.Valid
    }

    sealed class ValidationResult {
        object Valid : ValidationResult()
        data class Blocked(val reason: String) : ValidationResult()
    }
}

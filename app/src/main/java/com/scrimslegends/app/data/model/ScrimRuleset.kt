package com.scrimslegends.app.data.model

/**
 * Pre-defined scrim ruleset templates.
 */
enum class ScrimRuleset(
    val displayName: String,
    val description: String,
    val maxGames: Int,
    val pickMode: PickMode,
    val heroBanCount: Int = 0,
    val timeLimitMinutes: Int = 60
) {
    BO1_BLIND(
        displayName = "Best of 1 — Blind Pick",
        description = "Single game, blind pick (no bans)",
        maxGames = 1,
        pickMode = PickMode.BLIND,
        heroBanCount = 0,
        timeLimitMinutes = 30
    ),
    BO1_DRAFT(
        displayName = "Best of 1 — Draft",
        description = "Single game with draft pick and 3 bans per team",
        maxGames = 1,
        pickMode = PickMode.DRAFT,
        heroBanCount = 3,
        timeLimitMinutes = 45
    ),
    BO3_DRAFT(
        displayName = "Best of 3 — Draft",
        description = "First to 2 wins, draft pick, 3 bans per team",
        maxGames = 3,
        pickMode = PickMode.DRAFT,
        heroBanCount = 3,
        timeLimitMinutes = 90
    ),
    BO5_DRAFT(
        displayName = "Best of 5 — Draft",
        description = "First to 3 wins, draft pick, 5 bans per team",
        maxGames = 5,
        pickMode = PickMode.DRAFT,
        heroBanCount = 5,
        timeLimitMinutes = 150
    ),
    CUSTOM(
        displayName = "Custom Rules",
        description = "Set your own rules",
        maxGames = 1,
        pickMode = PickMode.BLIND,
        heroBanCount = 0,
        timeLimitMinutes = 60
    );

    companion object {
        fun default() = BO1_BLIND
    }
}

enum class PickMode {
    BLIND, DRAFT
}

package com.mlbb.scrim.data.model

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

/**
 * Property-based tests for ScrimRuleset enum covering enum behavior,
 * validation, invariants, and business logic.
 * 
 * Test Categories:
 * - Enum property validation
 * - State invariants
 * - Business logic validation
 * - Companion object behavior
 * - PickMode enum tests
 */
class ScrimRulesetPropertyTest {

    // ─── ENUM PROPERTY VALIDATION TESTS ───

    @Test
    fun `all rulesets have non-empty display names`() {
        // Property: All rulesets should have valid display names
        ScrimRuleset.entries.forEach { ruleset ->
            assertTrue(ruleset.displayName.isNotEmpty(), 
                      "${ruleset.name} should have non-empty display name")
            assertTrue(ruleset.displayName.length <= 50, 
                      "${ruleset.name} display name should be reasonable length")
        }
    }

    @Test
    fun `all rulesets have non-empty descriptions`() {
        // Property: All rulesets should have valid descriptions
        ScrimRuleset.entries.forEach { ruleset ->
            assertTrue(ruleset.description.isNotEmpty(), 
                      "${ruleset.name} should have non-empty description")
        }
    }

    @Test
    fun `maxGames should be positive and reasonable`() {
        // Property: maxGames should be 1-5 for valid rulesets
        ScrimRuleset.entries.forEach { ruleset ->
            assertTrue(ruleset.maxGames > 0, 
                      "${ruleset.name} maxGames should be positive")
            assertTrue(ruleset.maxGames <= 5, 
                      "${ruleset.name} maxGames should be reasonable (<=5)")
        }
    }

    @Test
    fun `heroBanCount should be non-negative`() {
        // Property: heroBanCount should be >= 0
        ScrimRuleset.entries.forEach { ruleset ->
            assertTrue(ruleset.heroBanCount >= 0, 
                      "${ruleset.name} heroBanCount should be non-negative")
            assertTrue(ruleset.heroBanCount <= 5, 
                      "${ruleset.name} heroBanCount should be reasonable (<=5)")
        }
    }

    @Test
    fun `timeLimitMinutes should be positive and reasonable`() {
        // Property: timeLimitMinutes should be 30-180 minutes
        ScrimRuleset.entries.forEach { ruleset ->
            assertTrue(ruleset.timeLimitMinutes > 0, 
                      "${ruleset.name} timeLimitMinutes should be positive")
            assertTrue(ruleset.timeLimitMinutes <= 180, 
                      "${ruleset.name} timeLimitMinutes should be reasonable (<=180)")
        }
    }

    // ─── STATE INVARIANT TESTS ───

    @Test
    fun `BO1 rulesets have maxGames equal to 1`() {
        // Property: BO1 rulesets should have exactly 1 game
        val bo1Rulesets = listOf(
            ScrimRuleset.BO1_BLIND,
            ScrimRuleset.BO1_DRAFT
        )

        bo1Rulesets.forEach { ruleset ->
            assertEquals(1, ruleset.maxGames, 
                         "${ruleset.name} should have maxGames=1")
        }
    }

    @Test
    fun `BO3 rulesets have maxGames equal to 3`() {
        // Property: BO3 rulesets should have exactly 3 games
        val bo3Rulesets = listOf(
            ScrimRuleset.BO3_DRAFT
        )

        bo3Rulesets.forEach { ruleset ->
            assertEquals(3, ruleset.maxGames, 
                         "${ruleset.name} should have maxGames=3")
        }
    }

    @Test
    fun `BO5 rulesets have maxGames equal to 5`() {
        // Property: BO5 rulesets should have exactly 5 games
        val bo5Rulesets = listOf(
            ScrimRuleset.BO5_DRAFT
        )

        bo5Rulesets.forEach { ruleset ->
            assertEquals(5, ruleset.maxGames, 
                         "${ruleset.name} should have maxGames=5")
        }
    }

    @Test
    fun `BLIND pick rulesets have heroBanCount equal to 0`() {
        // Property: Blind pick should have no bans
        val blindPickRulesets = ScrimRuleset.entries.filter { 
            it.pickMode == PickMode.BLIND 
        }

        blindPickRulesets.forEach { ruleset ->
            assertEquals(0, ruleset.heroBanCount, 
                         "${ruleset.name} (BLIND) should have heroBanCount=0")
        }
    }

    @Test
    fun `DRAFT pick rulesets have positive heroBanCount`() {
        // Property: Draft pick should have bans
        val draftPickRulesets = ScrimRuleset.entries.filter { 
            it.pickMode == PickMode.DRAFT 
        }

        draftPickRulesets.forEach { ruleset ->
            assertTrue(ruleset.heroBanCount > 0, 
                      "${ruleset.name} (DRAFT) should have heroBanCount>0")
        }
    }

    @Test
    fun `time limit increases with maxGames`() {
        // Property: More games should have longer time limits
        val bo1Time = ScrimRuleset.BO1_BLIND.timeLimitMinutes
        val bo3Time = ScrimRuleset.BO3_DRAFT.timeLimitMinutes
        val bo5Time = ScrimRuleset.BO5_DRAFT.timeLimitMinutes

        assertTrue(bo1Time < bo3Time, "BO1 should be shorter than BO3")
        assertTrue(bo3Time < bo5Time, "BO3 should be shorter than BO5")
    }

    // ─── BUSINESS LOGIC TESTS ───

    @Test
    fun `draft mode has longer time limit than blind mode for same maxGames`() {
        // Property: Draft mode should take longer than blind mode
        val bo1BlindTime = ScrimRuleset.BO1_BLIND.timeLimitMinutes
        val bo1DraftTime = ScrimRuleset.BO1_DRAFT.timeLimitMinutes

        assertTrue(bo1DraftTime > bo1BlindTime, 
                  "Draft mode should have longer time limit than blind mode")
    }

    @Test
    fun `higher BO series has more bans`() {
        // Property: Higher BO series should have more bans
        val bo1Bans = ScrimRuleset.BO1_DRAFT.heroBanCount
        val bo3Bans = ScrimRuleset.BO3_DRAFT.heroBanCount
        val bo5Bans = ScrimRuleset.BO5_DRAFT.heroBanCount

        assertTrue(bo1Bans <= bo3Bans, "BO1 should have <= bans than BO3")
        assertTrue(bo3Bans <= bo5Bans, "BO3 should have <= bans than BO5")
    }

    @Test
    fun `display names follow consistent format`() {
        // Property: Display names should follow consistent format
        ScrimRuleset.entries.forEach { ruleset ->
            assertTrue(ruleset.displayName.contains("Best of") || ruleset.displayName == "Custom Rules",
                      "${ruleset.name} display name should follow format")
        }
    }

    @Test
    fun `descriptions are informative`() {
        // Property: Descriptions should contain key information
        val bo1BlindDesc = ScrimRuleset.BO1_BLIND.description
        val bo1DraftDesc = ScrimRuleset.BO1_DRAFT.description
        val bo3DraftDesc = ScrimRuleset.BO3_DRAFT.description

        assertTrue(bo1BlindDesc.contains("blind"), "BO1_BLIND description should mention blind pick")
        assertTrue(bo1DraftDesc.contains("draft"), "BO1_DRAFT description should mention draft")
        assertTrue(bo3DraftDesc.contains("2 wins"), "BO3_DRAFT description should mention win condition")
    }

    // ─── COMPANION OBJECT TESTS ───

    @Test
    fun `default returns BO1_BLIND`() {
        // Property: Default ruleset should be BO1_BLIND
        val default = ScrimRuleset.default()
        assertEquals(ScrimRuleset.BO1_BLIND, default, 
                     "Default ruleset should be BO1_BLIND")
    }

    @Test
    fun `default is always the same`() {
        // Property: Default should be consistent across calls
        val default1 = ScrimRuleset.default()
        val default2 = ScrimRuleset.default()
        assertEquals(default1, default2, 
                     "Default ruleset should be consistent")
    }

    // ─── PICKMODE ENUM TESTS ───

    @Test
    fun `PickMode has exactly two values`() {
        // Property: PickMode should have exactly BLIND and DRAFT
        assertEquals(2, PickMode.entries.size, 
                     "PickMode should have exactly 2 values")
    }

    @Test
    fun `PickMode values are distinct`() {
        // Property: PickMode values should be distinct
        val pickModes = PickMode.entries
        val uniquePickModes = pickModes.toSet()
        assertEquals(pickModes.size, uniquePickModes.size, 
                     "PickMode values should be distinct")
    }

    @Test
    fun `all ScrimRulesets have valid PickMode`() {
        // Property: All rulesets should have valid PickMode
        ScrimRuleset.entries.forEach { ruleset ->
            assertTrue(ruleset.pickMode in PickMode.entries, 
                      "${ruleset.name} should have valid PickMode")
        }
    }

    @Test
    fun `BLIND and DRAFT modes are mutually exclusive`() {
        // Property: A ruleset cannot be both BLIND and DRAFT
        ScrimRuleset.entries.forEach { ruleset ->
            val isBlind = ruleset.pickMode == PickMode.BLIND
            val isDraft = ruleset.pickMode == PickMode.DRAFT
            assertTrue(isBlind xor isDraft, 
                      "${ruleset.name} should be either BLIND or DRAFT, not both")
        }
    }

    // ─── ENUM ITERATION TESTS ───

    @Test
    fun `all enum values are accessible`() {
        // Property: All enum values should be accessible
        val allRulesets = ScrimRuleset.entries
        assertEquals(5, allRulesets.size, 
                     "Should have exactly 5 ruleset options")
    }

    @Test
    fun `enum names match constant names`() {
        // Property: Enum names should match constant names
        val expectedNames = listOf(
            "BO1_BLIND", "BO1_DRAFT", "BO3_DRAFT", "BO5_DRAFT", "CUSTOM"
        )
        val actualNames = ScrimRuleset.entries.map { it.name }
        
        assertEquals(expectedNames.sorted(), actualNames.sorted(), 
                     "Enum names should match expected values")
    }

    // ─── SERIALIZATION TESTS ───

    @Test
    fun `enum values can be converted to string and back`() {
        // Property: Enum should be serializable
        ScrimRuleset.entries.forEach { ruleset ->
            val name = ruleset.name
            val reconstructed = ScrimRuleset.valueOf(name)
            assertEquals(ruleset, reconstructed, 
                         "${ruleset.name} should round-trip through string")
        }
    }

    @Test
    fun `enum ordinal is consistent`() {
        // Property: Enum ordinal should be consistent
        val ordinals = ScrimRuleset.entries.map { it.ordinal }
        val expectedOrdinals = listOf(0, 1, 2, 3, 4)
        assertEquals(expectedOrdinals, ordinals, 
                     "Enum ordinals should be consistent")
    }

    // ─── EDGE CASE TESTS ───

    @Test
    fun `CUSTOM ruleset has minimal restrictions`() {
        // Property: CUSTOM ruleset should have minimal restrictions
        val custom = ScrimRuleset.CUSTOM
        
        assertEquals(1, custom.maxGames, "CUSTOM should have maxGames=1")
        assertEquals(0, custom.heroBanCount, "CUSTOM should have heroBanCount=0")
        assertEquals(PickMode.BLIND, custom.pickMode, "CUSTOM should use BLIND mode")
        assertEquals(60, custom.timeLimitMinutes, "CUSTOM should have 60 minute time limit")
    }

    @Test
    fun `BO5_DRAFT has maximum values`() {
        // Property: BO5_DRAFT should have maximum values
        val bo5 = ScrimRuleset.BO5_DRAFT
        
        assertEquals(5, bo5.maxGames, "BO5 should have maxGames=5")
        assertEquals(5, bo5.heroBanCount, "BO5 should have heroBanCount=5")
        assertEquals(150, bo5.timeLimitMinutes, "BO5 should have 150 minute time limit")
    }

    @Test
    fun `BO1_BLIND has minimum values`() {
        // Property: BO1_BLIND should have minimum values
        val bo1 = ScrimRuleset.BO1_BLIND
        
        assertEquals(1, bo1.maxGames, "BO1 should have maxGames=1")
        assertEquals(0, bo1.heroBanCount, "BO1_BLIND should have heroBanCount=0")
        assertEquals(30, bo1.timeLimitMinutes, "BO1_BLIND should have 30 minute time limit")
    }

    // ─── CALCULATION TESTS ───

    @Test
    fun `total match time can be calculated from ruleset`() {
        // Property: Total match time = maxGames * (timeLimit / maxGames) should be reasonable
        ScrimRuleset.entries.forEach { ruleset ->
            val totalTime = ruleset.maxGames * ruleset.timeLimitMinutes
            assertTrue(totalTime > 0, 
                      "${ruleset.name} total match time should be positive")
            assertTrue(totalTime <= 900, 
                      "${ruleset.name} total match time should be reasonable (<=15 hours)")
        }
    }

    @Test
    fun `ban phase time is reasonable subset of total time`() {
        // Property: Ban phase should be reasonable portion of total time
        val draftRulesets = ScrimRuleset.entries.filter { it.pickMode == PickMode.DRAFT }
        
        draftRulesets.forEach { ruleset ->
            val estimatedBanTime = ruleset.heroBanCount * 2 // Assume 2 min per ban
            val totalTime = ruleset.timeLimitMinutes
            val banRatio = estimatedBanTime.toDouble() / totalTime
            
            assertTrue(banRatio < 0.3, 
                      "${ruleset.name} ban phase should be < 30% of total time")
        }
    }

    // ─── COMPARISON TESTS ───

    @Test
    fun `rulesets can be sorted by complexity`() {
        // Property: Rulesets can be sorted by maxGames
        val sorted = ScrimRuleset.entries.sortedBy { it.maxGames }
        
        assertEquals(ScrimRuleset.BO1_BLIND.maxGames, sorted[0].maxGames)
        assertEquals(ScrimRuleset.BO1_DRAFT.maxGames, sorted[1].maxGames)
        assertEquals(ScrimRuleset.BO3_DRAFT.maxGames, sorted[2].maxGames)
        assertEquals(ScrimRuleset.BO5_DRAFT.maxGames, sorted[3].maxGames)
    }

    @Test
    fun `rulesets can be sorted by time limit`() {
        // Property: Rulesets can be sorted by time limit
        val sorted = ScrimRuleset.entries.sortedBy { it.timeLimitMinutes }
        
        assertEquals(ScrimRuleset.BO1_BLIND.timeLimitMinutes, sorted[0].timeLimitMinutes)
        assertTrue(sorted.last().timeLimitMinutes > sorted.first().timeLimitMinutes)
    }

    @Test
    fun `rulesets can be filtered by pick mode`() {
        // Property: Rulesets can be filtered by pick mode
        val blindRulesets = ScrimRuleset.entries.filter { it.pickMode == PickMode.BLIND }
        val draftRulesets = ScrimRuleset.entries.filter { it.pickMode == PickMode.DRAFT }
        
        assertTrue(blindRulesets.isNotEmpty(), "Should have BLIND rulesets")
        assertTrue(draftRulesets.isNotEmpty(), "Should have DRAFT rulesets")
        assertEquals(ScrimRuleset.entries.size, blindRulesets.size + draftRulesets.size)
    }

    // ─── VALIDATION TESTS ───

    @Test
    fun `all rulesets have valid business logic combinations`() {
        // Property: All rulesets should have valid combinations of properties
        ScrimRuleset.entries.forEach { ruleset ->
            // Business rule: Draft mode should have bans
            if (ruleset.pickMode == PickMode.DRAFT) {
                assertTrue(ruleset.heroBanCount > 0, 
                          "${ruleset.name} (DRAFT) should have bans")
            }
            
            // Business rule: Blind mode should have no bans
            if (ruleset.pickMode == PickMode.BLIND) {
                assertEquals(0, ruleset.heroBanCount, 
                           "${ruleset.name} (BLIND) should have no bans")
            }
            
            // Business rule: Higher BO should have longer time
            if (ruleset.maxGames == 5) {
                assertTrue(ruleset.timeLimitMinutes >= 120, 
                          "${ruleset.name} (BO5) should have >= 120 min time limit")
            }
        }
    }

    @Test
    fun `no duplicate ruleset configurations`() {
        // Property: No two rulesets should have identical configurations
        val configurations = ScrimRuleset.entries.map { 
            "${it.maxGames}-${it.pickMode}-${it.heroBanCount}-${it.timeLimitMinutes}" 
        }
        val uniqueConfigurations = configurations.toSet()
        
        assertEquals(configurations.size, uniqueConfigurations.size, 
                     "No two rulesets should have identical configurations")
    }
}

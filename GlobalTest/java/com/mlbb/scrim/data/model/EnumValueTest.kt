package com.mlbb.scrim.data.model

import org.junit.Assert.*
import org.junit.Test

/**
 * Exhaustive enum value tests ensuring all enum values are valid
 * and have expected properties.
 */
class EnumValueTest {

    @Test
    fun `RankTier has exactly 7 tiers`() {
        assertEquals(7, RankTier.values().size)
    }

    @Test
    fun `RankTier values in correct order`() {
        val values = RankTier.values()
        assertEquals(RankTier.BRONZE, values[0])
        assertEquals(RankTier.SOLVER, values[1])
        assertEquals(RankTier.GOLD, values[2])
        assertEquals(RankTier.GRANDMASTER, values[3])
        assertEquals(RankTier.EPIC, values[4])
        assertEquals(RankTier.LEGEND, values[5])
        assertEquals(RankTier.MYTHIC, values[6])
    }

    @Test
    fun `BestOf has exactly 5 values`() {
        assertEquals(5, BestOf.values().size)
    }

    @Test
    fun `GameMode has exactly 4 values`() {
        assertEquals(4, GameMode.values().size)
    }

    @Test
    fun `Region has exactly 8 values`() {
        assertEquals(8, Region.values().size)
    }

    @Test
    fun `SkillLevel has exactly 5 values`() {
        assertEquals(5, SkillLevel.values().size)
    }

    @Test
    fun `ScrimStatus has exactly 6 values`() {
        assertEquals(6, ScrimStatus.values().size)
    }

    @Test
    fun `ApplicationStatus has exactly 4 values`() {
        assertEquals(4, ApplicationStatus.values().size)
    }

    @Test
    fun `TeamApplicationStatus has exactly 3 values`() {
        assertEquals(3, TeamApplicationStatus.values().size)
    }

    @Test
    fun `InviteStatus has exactly 5 values`() {
        assertEquals(5, InviteStatus.values().size)
    }

    @Test
    fun `PlayerRole has exactly 3 values`() {
        assertEquals(3, PlayerRole.values().size)
    }

    @Test
    fun `MessageType has exactly 5 values`() {
        assertEquals(5, MessageType.values().size)
    }

    @Test
    fun `NotificationType has exactly 7 values`() {
        assertEquals(7, NotificationType.values().size)
    }

    @Test
    fun `VerificationStatus has exactly 6 values`() {
        assertEquals(6, VerificationStatus.values().size)
    }

    @Test
    fun `AdminVerdict has exactly 7 values`() {
        assertEquals(7, AdminVerdict.values().size)
    }

    @Test
    fun `GameRole has exactly 7 values`() {
        assertEquals(7, GameRole.values().size)
    }

    @Test
    fun `RegionalRank has exactly 3 values`() {
        assertEquals(3, RegionalRank.values().size)
    }

    @Test
    fun `Achievement has exactly 17 values`() {
        assertEquals(17, Achievement.values().size)
    }

    @Test
    fun `AchievementTier has exactly 7 values`() {
        assertEquals(7, AchievementTier.values().size)
    }

    @Test
    fun `AchievementCategory has exactly 4 values`() {
        assertEquals(4, AchievementCategory.values().size)
    }

    @Test
    fun `all enum classes use enum semantics correctly`() {
        // Verify no enum has duplicate ordinal values
        val enums = listOf(
            RankTier::class.java,
            BestOf::class.java,
            GameMode::class.java,
            Region::class.java,
            SkillLevel::class.java,
            ScrimStatus::class.java,
            ApplicationStatus::class.java,
            TeamApplicationStatus::class.java,
            InviteStatus::class.java,
            PlayerRole::class.java,
            MessageType::class.java,
            NotificationType::class.java,
            VerificationStatus::class.java,
            AdminVerdict::class.java,
            GameRole::class.java,
            RegionalRank::class.java,
            AchievementTier::class.java,
            AchievementCategory::class.java
        )

        enums.forEach { enumClass ->
            val values = enumClass.enumConstants as Array<Enum<*>>
            val ordinals = values.map { it.ordinal }
            assertEquals(
                "${enumClass.simpleName} should have unique ordinals",
                ordinals.size, ordinals.toSet().size
            )
        }
    }

    @Test
    fun `all enum classes support valueOf`() {
        val enumClasses = listOf(
            RankTier::class.java,
            BestOf::class.java,
            GameMode::class.java,
            Region::class.java,
            SkillLevel::class.java,
            ScrimStatus::class.java,
            ApplicationStatus::class.java,
            TeamApplicationStatus::class.java,
            InviteStatus::class.java,
            PlayerRole::class.java,
            MessageType::class.java,
            NotificationType::class.java,
            VerificationStatus::class.java,
            AdminVerdict::class.java,
            GameRole::class.java,
            RegionalRank::class.java
        )

        enumClasses.forEach { enumClass ->
            val values = enumClass.enumConstants as Array<Enum<*>>
            values.forEach { value ->
                val lookedUp = java.lang.Enum.valueOf(enumClass, value.name)
                assertSame(value, lookedUp)
            }
        }
    }
}

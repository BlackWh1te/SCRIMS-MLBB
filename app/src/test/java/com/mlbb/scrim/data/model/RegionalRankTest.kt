package com.mlbb.scrim.data.model

import org.junit.Assert.*
import org.junit.Test

class RegionalRankTest {

    @Test
    fun `fromWins returns null for non-RU region`() {
        assertNull(RegionalRank.fromWins(wins = 100, region = "NA"))
    }

    @Test
    fun `fromWins returns TOP1 for KRD with 50 wins`() {
        assertEquals(RegionalRank.TOP1, RegionalRank.fromWins(wins = 50, region = "KRD"))
    }

    @Test
    fun `fromWins returns TOP1 for MSK with 100 wins`() {
        assertEquals(RegionalRank.TOP1, RegionalRank.fromWins(wins = 100, region = "MSK"))
    }

    @Test
    fun `fromWins returns TOP2 for EKB with 30 wins`() {
        assertEquals(RegionalRank.TOP2, RegionalRank.fromWins(wins = 30, region = "EKB"))
    }

    @Test
    fun `fromWins returns TOP3 for MCK with 15 wins`() {
        assertEquals(RegionalRank.TOP3, RegionalRank.fromWins(wins = 15, region = "MCK"))
    }

    @Test
    fun `fromWins returns null for exactly 14 wins`() {
        assertNull(RegionalRank.fromWins(wins = 14, region = "KRD"))
    }

    @Test
    fun `fromWins returns TOP2 for exactly 49 wins`() {
        assertEquals(RegionalRank.TOP2, RegionalRank.fromWins(wins = 49, region = "KRD"))
    }

    @Test
    fun `fromWins returns TOP3 for exactly 29 wins`() {
        assertEquals(RegionalRank.TOP3, RegionalRank.fromWins(wins = 29, region = "KRD"))
    }

    @Test
    fun `fromWins returns null for 0 wins`() {
        assertNull(RegionalRank.fromWins(wins = 0, region = "KRD"))
    }

    @Test
    fun `RU regions include KRD MSK EKB MCK`() {
        // These should all return non-null for sufficient wins
        assertNotNull(RegionalRank.fromWins(wins = 50, region = "KRD"))
        assertNotNull(RegionalRank.fromWins(wins = 50, region = "MSK"))
        assertNotNull(RegionalRank.fromWins(wins = 50, region = "EKB"))
        assertNotNull(RegionalRank.fromWins(wins = 50, region = "MCK"))
    }

    @Test
    fun `TOP1 has rank 1`() {
        assertEquals(1, RegionalRank.TOP1.rank)
    }

    @Test
    fun `TOP2 has rank 2`() {
        assertEquals(2, RegionalRank.TOP2.rank)
    }

    @Test
    fun `TOP3 has rank 3`() {
        assertEquals(3, RegionalRank.TOP3.rank)
    }

    @Test
    fun `display prefixes are correct`() {
        assertEquals("TOP 1", RegionalRank.TOP1.displayPrefix)
        assertEquals("TOP 2", RegionalRank.TOP2.displayPrefix)
        assertEquals("TOP 3", RegionalRank.TOP3.displayPrefix)
    }
}

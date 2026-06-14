package com.mlbb.scrim.data.repository

import com.mlbb.scrim.data.model.LeaderboardEntry
import com.mlbb.scrim.data.model.RankTier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class LeaderboardRepositoryTest {

    private lateinit var repository: LeaderboardRepository
    private lateinit var testDispatcher: TestDispatcher

    @Before
    fun setup() {
        repository = LeaderboardRepository()
        testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ─── Get Leaderboard Tests ───

    @Test
    fun `getLeaderboard returns all entries sorted by rank`() {
        // Act
        val result = repository.getLeaderboard().first()

        advanceUntilIdle()

        // Assert
        assertTrue(result.isSuccess)
        val entries = result.getOrNull()!!
        assertEquals(10, entries.size)
        assertEquals(1, entries[0].rank)
        assertEquals(10, entries[9].rank)
    }

    @Test
    fun `getLeaderboard returns entries with correct data`() {
        // Act
        val result = repository.getLeaderboard().first()

        advanceUntilIdle()

        // Assert
        assertTrue(result.isSuccess)
        val entries = result.getOrNull()!!
        
        // Check first entry (MYTHIC)
        assertEquals("ShadowSlayer", entries[0].username)
        assertEquals("Shadow Wolves", entries[0].teamName)
        assertEquals(18500, entries[0].xp)
        assertEquals(RankTier.MYTHIC, entries[0].currentTier)
        
        // Check second entry (LEGEND)
        assertEquals("PhoenixRise", entries[1].username)
        assertEquals(RankTier.LEGEND, entries[1].currentTier)
    }

    @Test
    fun `getLeaderboard includes all rank tiers`() {
        // Act
        val result = repository.getLeaderboard().first()

        advanceUntilIdle()

        // Assert
        assertTrue(result.isSuccess)
        val entries = result.getOrNull()!!
        
        val tiers = entries.map { it.currentTier }.distinct()
        assertTrue(tiers.contains(RankTier.MYTHIC))
        assertTrue(tiers.contains(RankTier.LEGEND))
        assertTrue(tiers.contains(RankTier.EPIC))
        assertTrue(tiers.contains(RankTier.GRANDMASTER))
        assertTrue(tiers.contains(RankTier.GOLD))
        assertTrue(tiers.contains(RankTier.SOLVER))
        assertTrue(tiers.contains(RankTier.BRONZE))
    }

    // ─── Get Leaderboard For Tier Tests ───

    @Test
    fun `getLeaderboardForTier returns only entries for specified tier`() {
        // Act
        val result = repository.getLeaderboardForTier(RankTier.LEGEND).first()

        advanceUntilIdle()

        // Assert
        assertTrue(result.isSuccess)
        val entries = result.getOrNull()!!
        assertEquals(2, entries.size)
        entries.forEach { assertEquals(RankTier.LEGEND, it.currentTier) }
    }

    @Test
    fun `getLeaderboardForTier returns empty list for tier with no entries`() {
        // Act
        val result = repository.getLeaderboardForTier(RankTier.MYTHIC).first()

        advanceUntilIdle()

        // Assert
        assertTrue(result.isSuccess)
        val entries = result.getOrNull()!!
        // MYTHIC has 1 entry
        assertEquals(1, entries.size)
    }

    @Test
    fun `getLeaderboardForTier returns sorted entries by rank`() {
        // Act
        val result = repository.getLeaderboardForTier(RankTier.GOLD).first()

        advanceUntilIdle()

        // Assert
        assertTrue(result.isSuccess)
        val entries = result.getOrNull()!!
        assertEquals(2, entries.size)
        assertTrue(entries[0].rank < entries[1].rank)
    }

    @Test
    fun `getLeaderboardForTier handles all rank tiers correctly`() {
        // Act & Assert for each tier
        val tiers = listOf(
            RankTier.MYTHIC to 1,
            RankTier.LEGEND to 2,
            RankTier.EPIC to 2,
            RankTier.GRANDMASTER to 1,
            RankTier.GOLD to 2,
            RankTier.SOLVER to 1,
            RankTier.BRONZE to 1
        )

        tiers.forEach { (tier, expectedCount) ->
            val result = repository.getLeaderboardForTier(tier).first()
            advanceUntilIdle()
            assertTrue(result.isSuccess)
            val entries = result.getOrNull()!!
            assertEquals(expectedCount, entries.size, "Tier $tier should have $expectedCount entries")
        }
    }

    // ─── AddOrUpdate Entry Tests ───

    @Test
    fun `addOrUpdateEntry adds new entry when player does not exist`() {
        // Arrange
        val newEntry = LeaderboardEntry(
            rank = 0,
            playerId = "new_player",
            username = "NewPlayer",
            teamName = "New Team",
            xp = 5000,
            wins = 10,
            losses = 5,
            totalMatches = 15,
            currentTier = RankTier.GRANDMASTER
        )

        // Act
        repository.addOrUpdateEntry(newEntry)

        // Assert
        val result = repository.getLeaderboard().first()
        advanceUntilIdle()
        
        assertTrue(result.isSuccess)
        val entries = result.getOrNull()!!
        assertEquals(11, entries.size)
        assertTrue(entries.any { it.playerId == "new_player" })
    }

    @Test
    fun `addOrUpdateEntry updates existing entry when player exists`() {
        // Arrange
        val updatedEntry = LeaderboardEntry(
            rank = 1,
            playerId = "p1", // Existing player
            username = "ShadowSlayer",
            teamName = "Shadow Wolves",
            xp = 20000, // Increased XP
            wins = 50,
            losses = 12,
            totalMatches = 62,
            currentTier = RankTier.MYTHIC
        )

        // Act
        repository.addOrUpdateEntry(updatedEntry)

        // Assert
        val result = repository.getLeaderboard().first()
        advanceUntilIdle()
        
        assertTrue(result.isSuccess)
        val entries = result.getOrNull()!!
        assertEquals(10, entries.size) // Should still be 10, not 11
        val playerEntry = entries.find { it.playerId == "p1" }
        assertEquals(20000, playerEntry?.xp)
        assertEquals(62, playerEntry?.totalMatches)
    }

    @Test
    fun `addOrUpdateEntry recalculates ranks based on XP`() {
        // Arrange
        val highXpEntry = LeaderboardEntry(
            rank = 0,
            playerId = "new_top_player",
            username = "TopPlayer",
            teamName = "Top Team",
            xp = 30000, // Higher than current #1
            wins = 100,
            losses = 0,
            totalMatches = 100,
            currentTier = RankTier.MYTHIC
        )

        // Act
        repository.addOrUpdateEntry(highXpEntry)

        // Assert
        val result = repository.getLeaderboard().first()
        advanceUntilIdle()
        
        assertTrue(result.isSuccess)
        val entries = result.getOrNull()!!
        
        val newTopPlayer = entries.find { it.playerId == "new_top_player" }
        assertEquals(1, newTopPlayer?.rank)
        
        val oldTopPlayer = entries.find { it.playerId == "p1" }
        assertEquals(2, oldTopPlayer?.rank) // Should be demoted to rank 2
    }

    @Test
    fun `addOrUpdateEntry maintains correct rank ordering`() {
        // Arrange
        val entriesToAdd = listOf(
            LeaderboardEntry(rank = 0, playerId = "p11", username = "Player11", teamName = "Team11", xp = 25000, wins = 50, losses = 10, totalMatches = 60, currentTier = RankTier.MYTHIC),
            LeaderboardEntry(rank = 0, playerId = "p12", username = "Player12", teamName = "Team12", xp = 5000, wins = 10, losses = 5, totalMatches = 15, currentTier = RankTier.GRANDMASTER),
            LeaderboardEntry(rank = 0, playerId = "p13", username = "Player13", teamName = "Team13", xp = 15000, wins = 30, losses = 10, totalMatches = 40, currentTier = RankTier.LEGEND)
        )

        // Act
        entriesToAdd.forEach { repository.addOrUpdateEntry(it) }

        // Assert
        val result = repository.getLeaderboard().first()
        advanceUntilIdle()
        
        assertTrue(result.isSuccess)
        val entries = result.getOrNull()!!
        assertEquals(13, entries.size)
        
        // Check ranks are sequential and ordered by XP
        entries.forEachIndexed { index, entry ->
            assertEquals(index + 1, entry.rank)
        }
        
        // Check XP is in descending order
        var previousXp = Long.MAX_VALUE
        entries.forEach { entry ->
            assertTrue(entry.xp <= previousXp)
            previousXp = entry.xp
        }
    }

    @Test
    fun `addOrUpdateEntry handles multiple updates to same player`() {
        // Arrange
        val playerEntry = LeaderboardEntry(
            rank = 0,
            playerId = "p1",
            username = "ShadowSlayer",
            teamName = "Shadow Wolves",
            xp = 19000,
            wins = 46,
            losses = 12,
            totalMatches = 58,
            currentTier = RankTier.MYTHIC
        )

        // Act - First update
        repository.addOrUpdateEntry(playerEntry)

        // Second update
        val secondUpdate = playerEntry.copy(xp = 19500, wins = 47, totalMatches = 59)
        repository.addOrUpdateEntry(secondUpdate)

        // Assert
        val result = repository.getLeaderboard().first()
        advanceUntilIdle()
        
        assertTrue(result.isSuccess)
        val entries = result.getOrNull()!!
        assertEquals(10, entries.size) // Should still be 10
        
        val playerEntryResult = entries.find { it.playerId == "p1" }
        assertEquals(19500, playerEntryResult?.xp)
        assertEquals(47, playerEntryResult?.wins)
        assertEquals(1, playerEntryResult?.rank) // Should still be rank 1
    }

    // ─── Integration Tests ───

    @Test
    fun `getLeaderboard reflects changes from addOrUpdateEntry`() {
        // Arrange
        val initialResult = repository.getLeaderboard().first()
        advanceUntilIdle()
        val initialCount = initialResult.getOrNull()!!.size

        // Act
        val newEntry = LeaderboardEntry(
            rank = 0,
            playerId = "new_player",
            username = "NewPlayer",
            teamName = "New Team",
            xp = 5000,
            wins = 10,
            losses = 5,
            totalMatches = 15,
            currentTier = RankTier.GRANDMASTER
        )
        repository.addOrUpdateEntry(newEntry)

        val finalResult = repository.getLeaderboard().first()
        advanceUntilIdle()

        // Assert
        assertTrue(finalResult.isSuccess)
        val finalEntries = finalResult.getOrNull()!!
        assertEquals(initialCount + 1, finalEntries.size)
        assertTrue(finalEntries.any { it.playerId == "new_player" })
    }

    @Test
    fun `getLeaderboardForTier reflects changes from addOrUpdateEntry`() {
        // Arrange
        val initialResult = repository.getLeaderboardForTier(RankTier.GRANDMASTER).first()
        advanceUntilIdle()
        val initialCount = initialResult.getOrNull()!!.size

        // Act
        val newEntry = LeaderboardEntry(
            rank = 0,
            playerId = "new_grandmaster",
            username = "NewGrandmaster",
            teamName = "New Team",
            xp = 6000,
            wins = 12,
            losses = 8,
            totalMatches = 20,
            currentTier = RankTier.GRANDMASTER
        )
        repository.addOrUpdateEntry(newEntry)

        val finalResult = repository.getLeaderboardForTier(RankTier.GRANDMASTER).first()
        advanceUntilIdle()

        // Assert
        assertTrue(finalResult.isSuccess)
        val finalEntries = finalResult.getOrNull()!!
        assertEquals(initialCount + 1, finalEntries.size)
        assertTrue(finalEntries.any { it.playerId == "new_grandmaster" })
    }

    // ─── Edge Case Tests ───

    @Test
    fun `addOrUpdateEntry handles entry with zero XP`() {
        // Arrange
        val zeroXpEntry = LeaderboardEntry(
            rank = 0,
            playerId = "zero_xp_player",
            username = "ZeroXP",
            teamName = "Zero Team",
            xp = 0,
            wins = 0,
            losses = 0,
            totalMatches = 0,
            currentTier = RankTier.BRONZE
        )

        // Act
        repository.addOrUpdateEntry(zeroXpEntry)

        // Assert
        val result = repository.getLeaderboard().first()
        advanceUntilIdle()
        
        assertTrue(result.isSuccess)
        val entries = result.getOrNull()!!
        assertTrue(entries.any { it.playerId == "zero_xp_player" })
        
        // Zero XP should be at the bottom
        val zeroXpPlayer = entries.find { it.playerId == "zero_xp_player" }
        assertEquals(11, zeroXpPlayer?.rank)
    }

    @Test
    fun `addOrUpdateEntry handles entry with very high XP`() {
        // Arrange
        val highXpEntry = LeaderboardEntry(
            rank = 0,
            playerId = "infinite_xp_player",
            username = "InfiniteXP",
            teamName = "Infinite Team",
            xp = Long.MAX_VALUE,
            wins = 1000,
            losses = 0,
            totalMatches = 1000,
            currentTier = RankTier.MYTHIC
        )

        // Act
        repository.addOrUpdateEntry(highXpEntry)

        // Assert
        val result = repository.getLeaderboard().first()
        advanceUntilIdle()
        
        assertTrue(result.isSuccess)
        val entries = result.getOrNull()!!
        val infiniteXpPlayer = entries.find { it.playerId == "infinite_xp_player" }
        assertEquals(1, infiniteXpPlayer?.rank)
    }

    @Test
    fun `getLeaderboardForTier handles tier with single entry`() {
        // Act
        val result = repository.getLeaderboardForTier(RankTier.MYTHIC).first()

        advanceUntilIdle()

        // Assert
        assertTrue(result.isSuccess)
        val entries = result.getOrNull()!!
        assertEquals(1, entries.size)
        assertEquals(RankTier.MYTHIC, entries[0].currentTier)
    }

    @Test
    fun `addOrUpdateEntry maintains tier consistency`() {
        // Arrange
        val goldEntry = LeaderboardEntry(
            rank = 0,
            playerId = "gold_player",
            username = "GoldPlayer",
            teamName = "Gold Team",
            xp = 3000,
            wins = 8,
            losses = 7,
            totalMatches = 15,
            currentTier = RankTier.GOLD
        )

        // Act
        repository.addOrUpdateEntry(goldEntry)

        // Assert
        val goldResult = repository.getLeaderboardForTier(RankTier.GOLD).first()
        advanceUntilIdle()
        
        assertTrue(goldResult.isSuccess)
        val goldEntries = goldResult.getOrNull()!!
        assertTrue(goldEntries.any { it.playerId == "gold_player" })
        assertEquals(RankTier.GOLD, goldEntries.find { it.playerId == "gold_player" }?.currentTier)
    }

    // ─── Data Integrity Tests ───

    @Test
    fun `getLeaderboard returns entries with correct win-loss ratios`() {
        // Act
        val result = repository.getLeaderboard().first()
        advanceUntilIdle()

        // Assert
        assertTrue(result.isSuccess)
        val entries = result.getOrNull()!!
        
        entries.forEach { entry ->
            assertEquals(entry.wins + entry.losses, entry.totalMatches)
        }
    }

    @Test
    fun `getLeaderboard returns entries with valid rank tiers`() {
        // Act
        val result = repository.getLeaderboard().first()
        advanceUntilIdle()

        // Assert
        assertTrue(result.isSuccess)
        val entries = result.getOrNull()!!
        
        entries.forEach { entry ->
            assertTrue(entry.currentTier in RankTier.values())
        }
    }

    @Test
    fun `addOrUpdateEntry preserves non-updated fields`() {
        // Arrange
        val updatedEntry = LeaderboardEntry(
            rank = 1,
            playerId = "p1",
            username = "ShadowSlayer", // Same username
            teamName = "Shadow Wolves", // Same team name
            xp = 20000, // Updated XP
            wins = 50, // Updated wins
            losses = 12, // Same losses
            totalMatches = 62, // Updated total matches
            currentTier = RankTier.MYTHIC // Same tier
        )

        // Act
        repository.addOrUpdateEntry(updatedEntry)

        // Assert
        val result = repository.getLeaderboard().first()
        advanceUntilIdle()
        
        assertTrue(result.isSuccess)
        val entries = result.getOrNull()!!
        val playerEntry = entries.find { it.playerId == "p1" }
        assertEquals("ShadowSlayer", playerEntry?.username)
        assertEquals("Shadow Wolves", playerEntry?.teamName)
        assertEquals(12, playerEntry?.losses)
        assertEquals(RankTier.MYTHIC, playerEntry?.currentTier)
    }
}

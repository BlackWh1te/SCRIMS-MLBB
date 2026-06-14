package com.mlbb.scrim.data.local

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertNull
import kotlin.test.assertNotNull

/**
 * Advanced tests for LeaderboardDao covering:
 * - CRUD operations
 * - Sorting and ordering
 * - Bulk operations
 * - Edge cases
 * - Data integrity
 * - Performance scenarios
 */
@RunWith(AndroidJUnit4::class)
class LeaderboardDaoAdvancedTest {

    private lateinit var database: AppDatabase
    private lateinit var leaderboardDao: LeaderboardDao

    @Before
    fun setup() {
        database = AppDatabase.createInMemory()
        leaderboardDao = database.leaderboardDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    // ─── CRUD OPERATIONS ───

    @Test
    fun `insertAll should store multiple leaderboard entries`() = runTest {
        val entries = listOf(
            LeaderboardEntity(
                id = "player1",
                playerName = "Player1",
                rank = 1,
                points = 1000,
                tier = "Mythic",
                ordinalRank = 1
            ),
            LeaderboardEntity(
                id = "player2",
                playerName = "Player2",
                rank = 2,
                points = 900,
                tier = "Legendary",
                ordinalRank = 2
            ),
            LeaderboardEntity(
                id = "player3",
                playerName = "Player3",
                rank = 3,
                points = 800,
                tier = "Epic",
                ordinalRank = 3
            )
        )

        leaderboardDao.insertAll(entries)
        val retrieved = leaderboardDao.getAll()

        assertEquals(3, retrieved.size)
        assertEquals("Player1", retrieved[0].playerName)
        assertEquals("Player2", retrieved[1].playerName)
        assertEquals("Player3", retrieved[2].playerName)
    }

    @Test
    fun `getAll should return entries ordered by ordinalRank ASC`() = runTest {
        val entries = listOf(
            LeaderboardEntity(
                id = "player3",
                playerName = "Player3",
                rank = 3,
                points = 800,
                tier = "Epic",
                ordinalRank = 3
            ),
            LeaderboardEntity(
                id = "player1",
                playerName = "Player1",
                rank = 1,
                points = 1000,
                tier = "Mythic",
                ordinalRank = 1
            ),
            LeaderboardEntity(
                id = "player2",
                playerName = "Player2",
                rank = 2,
                points = 900,
                tier = "Legendary",
                ordinalRank = 2
            )
        )

        leaderboardDao.insertAll(entries)
        val retrieved = leaderboardDao.getAll()

        assertEquals(1, retrieved[0].ordinalRank)
        assertEquals(2, retrieved[1].ordinalRank)
        assertEquals(3, retrieved[2].ordinalRank)
    }

    @Test
    fun `getAll should return empty list when no entries exist`() = runTest {
        val retrieved = leaderboardDao.getAll()

        assertEquals(0, retrieved.size)
    }

    @Test
    fun `deleteAll should remove all leaderboard entries`() = runTest {
        val entries = listOf(
            LeaderboardEntity(
                id = "player1",
                playerName = "Player1",
                rank = 1,
                points = 1000,
                tier = "Mythic",
                ordinalRank = 1
            ),
            LeaderboardEntity(
                id = "player2",
                playerName = "Player2",
                rank = 2,
                points = 900,
                tier = "Legendary",
                ordinalRank = 2
            )
        )

        leaderboardDao.insertAll(entries)
        assertEquals(2, leaderboardDao.getAll().size)

        leaderboardDao.deleteAll()
        assertEquals(0, leaderboardDao.getAll().size)
    }

    // ─── BULK OPERATIONS ───

    @Test
    fun `insertAll should handle empty list`() = runTest {
        leaderboardDao.insertAll(emptyList())
        val retrieved = leaderboardDao.getAll()

        assertEquals(0, retrieved.size)
    }

    @Test
    fun `insertAll should handle single entry`() = runTest {
        val entry = LeaderboardEntity(
            id = "player1",
            playerName = "Player1",
            rank = 1,
            points = 1000,
            tier = "Mythic",
            ordinalRank = 1
        )

        leaderboardDao.insertAll(listOf(entry))
        val retrieved = leaderboardDao.getAll()

        assertEquals(1, retrieved.size)
        assertEquals("Player1", retrieved[0].playerName)
    }

    @Test
    fun `insertAll should replace existing entries on conflict`() = runTest {
        val originalEntry = LeaderboardEntity(
            id = "player1",
            playerName = "Player1",
            rank = 1,
            points = 1000,
            tier = "Mythic",
            ordinalRank = 1
        )

        val updatedEntry = LeaderboardEntity(
            id = "player1",
            playerName = "Player1Updated",
            rank = 2,
            points = 1100,
            tier = "Legendary",
            ordinalRank = 2
        )

        leaderboardDao.insertAll(listOf(originalEntry))
        leaderboardDao.insertAll(listOf(updatedEntry))

        val retrieved = leaderboardDao.getAll()
        assertEquals(1, retrieved.size)
        assertEquals("Player1Updated", retrieved[0].playerName)
        assertEquals(1100, retrieved[0].points)
    }

    @Test
    fun `insertAll should handle large dataset`() = runTest {
        val entries = (1..1000).map { i ->
            LeaderboardEntity(
                id = "player$i",
                playerName = "Player$i",
                rank = i,
                points = 1000 - i,
                tier = "Tier${i % 10}",
                ordinalRank = i
            )
        }

        leaderboardDao.insertAll(entries)
        val retrieved = leaderboardDao.getAll()

        assertEquals(1000, retrieved.size)
    }

    // ─── EDGE CASES ───

    @Test
    fun `insertAll should handle entries with same ordinalRank`() = runTest {
        val entries = listOf(
            LeaderboardEntity(
                id = "player1",
                playerName = "Player1",
                rank = 1,
                points = 1000,
                tier = "Mythic",
                ordinalRank = 1
            ),
            LeaderboardEntity(
                id = "player2",
                playerName = "Player2",
                rank = 1,
                points = 1000,
                tier = "Mythic",
                ordinalRank = 1
            )
        )

        leaderboardDao.insertAll(entries)
        val retrieved = leaderboardDao.getAll()

        assertEquals(2, retrieved.size)
    }

    @Test
    fun `insertAll should handle entries with zero ordinalRank`() = runTest {
        val entry = LeaderboardEntity(
            id = "player1",
            playerName = "Player1",
            rank = 0,
            points = 0,
            tier = "Unranked",
            ordinalRank = 0
        )

        leaderboardDao.insertAll(listOf(entry))
        val retrieved = leaderboardDao.getAll()

        assertEquals(1, retrieved.size)
        assertEquals(0, retrieved[0].ordinalRank)
    }

    @Test
    fun `insertAll should handle entries with negative points`() = runTest {
        val entry = LeaderboardEntity(
            id = "player1",
            playerName = "Player1",
            rank = 1,
            points = -100,
            tier = "Unranked",
            ordinalRank = 1
        )

        leaderboardDao.insertAll(listOf(entry))
        val retrieved = leaderboardDao.getAll()

        assertEquals(1, retrieved.size)
        assertEquals(-100, retrieved[0].points)
    }

    @Test
    fun `insertAll should handle entries with very long player names`() = runTest {
        val longName = "A".repeat(10000)
        val entry = LeaderboardEntity(
            id = "player1",
            playerName = longName,
            rank = 1,
            points = 1000,
            tier = "Mythic",
            ordinalRank = 1
        )

        leaderboardDao.insertAll(listOf(entry))
        val retrieved = leaderboardDao.getAll()

        assertEquals(1, retrieved.size)
        assertEquals(longName, retrieved[0].playerName)
    }

    @Test
    fun `insertAll should handle entries with special characters in names`() = runTest {
        val specialName = "Player!@#$%^&*()"
        val entry = LeaderboardEntity(
            id = "player1",
            playerName = specialName,
            rank = 1,
            points = 1000,
            tier = "Mythic",
            ordinalRank = 1
        )

        leaderboardDao.insertAll(listOf(entry))
        val retrieved = leaderboardDao.getAll()

        assertEquals(1, retrieved.size)
        assertEquals(specialName, retrieved[0].playerName)
    }

    @Test
    fun `insertAll should handle entries with unicode characters`() = runTest {
        val unicodeName = "玩家123 😀 特殊字符"
        val entry = LeaderboardEntity(
            id = "player1",
            playerName = unicodeName,
            rank = 1,
            points = 1000,
            tier = "Mythic",
            ordinalRank = 1
        )

        leaderboardDao.insertAll(listOf(entry))
        val retrieved = leaderboardDao.getAll()

        assertEquals(1, retrieved.size)
        assertEquals(unicodeName, retrieved[0].playerName)
    }

    // ─── DATA INTEGRITY ───

    @Test
    fun `insertAll should maintain data consistency`() = runTest {
        val entries = listOf(
            LeaderboardEntity(
                id = "player1",
                playerName = "Player1",
                rank = 1,
                points = 1000,
                tier = "Mythic",
                ordinalRank = 1
            ),
            LeaderboardEntity(
                id = "player2",
                playerName = "Player2",
                rank = 2,
                points = 900,
                tier = "Legendary",
                ordinalRank = 2
            )
        )

        leaderboardDao.insertAll(entries)
        val retrieved = leaderboardDao.getAll()

        entries.forEach { original ->
            val found = retrieved.find { it.id == original.id }
            assertNotNull(found)
            assertEquals(original.playerName, found.playerName)
            assertEquals(original.rank, found.rank)
            assertEquals(original.points, found.points)
            assertEquals(original.tier, found.tier)
            assertEquals(original.ordinalRank, found.ordinalRank)
        }
    }

    @Test
    fun `insertAll should handle duplicate IDs with REPLACE strategy`() = runTest {
        val entry1 = LeaderboardEntity(
            id = "player1",
            playerName = "Player1",
            rank = 1,
            points = 1000,
            tier = "Mythic",
            ordinalRank = 1
        )

        val entry2 = LeaderboardEntity(
            id = "player1",
            playerName = "Player2",
            rank = 2,
            points = 900,
            tier = "Legendary",
            ordinalRank = 2
        )

        leaderboardDao.insertAll(listOf(entry1))
        leaderboardDao.insertAll(listOf(entry2))

        val retrieved = leaderboardDao.getAll()
        assertEquals(1, retrieved.size)
        assertEquals("Player2", retrieved[0].playerName)
    }

    // ─── PERFORMANCE SCENARIOS ───

    @Test
    fun `getAll should perform efficiently with large dataset`() = runTest {
        val entries = (1..1000).map { i ->
            LeaderboardEntity(
                id = "player$i",
                playerName = "Player$i",
                rank = i,
                points = 1000 - i,
                tier = "Tier${i % 10}",
                ordinalRank = i
            )
        }

        leaderboardDao.insertAll(entries)

        val startTime = System.currentTimeMillis()
        val retrieved = leaderboardDao.getAll()
        val endTime = System.currentTimeMillis()

        assertEquals(1000, retrieved.size)
        assertTrue(endTime - startTime < 1000, "getAll should complete in under 1 second for 1000 entries")
    }

    @Test
    fun `insertAll should perform efficiently with large dataset`() = runTest {
        val entries = (1..1000).map { i ->
            LeaderboardEntity(
                id = "player$i",
                playerName = "Player$i",
                rank = i,
                points = 1000 - i,
                tier = "Tier${i % 10}",
                ordinalRank = i
            )
        }

        val startTime = System.currentTimeMillis()
        leaderboardDao.insertAll(entries)
        val endTime = System.currentTimeMillis()

        assertTrue(endTime - startTime < 1000, "insertAll should complete in under 1 second for 1000 entries")
    }

    @Test
    fun `deleteAll should perform efficiently with large dataset`() = runTest {
        val entries = (1..1000).map { i ->
            LeaderboardEntity(
                id = "player$i",
                playerName = "Player$i",
                rank = i,
                points = 1000 - i,
                tier = "Tier${i % 10}",
                ordinalRank = i
            )
        }

        leaderboardDao.insertAll(entries)

        val startTime = System.currentTimeMillis()
        leaderboardDao.deleteAll()
        val endTime = System.currentTimeMillis()

        assertEquals(0, leaderboardDao.getAll().size)
        assertTrue(endTime - startTime < 1000, "deleteAll should complete in under 1 second for 1000 entries")
    }

    // ─── ORDERING TESTS ───

    @Test
    fun `getAll should maintain correct order after multiple inserts`() = runTest {
        val entries1 = listOf(
            LeaderboardEntity(
                id = "player3",
                playerName = "Player3",
                rank = 3,
                points = 800,
                tier = "Epic",
                ordinalRank = 3
            ),
            LeaderboardEntity(
                id = "player1",
                playerName = "Player1",
                rank = 1,
                points = 1000,
                tier = "Mythic",
                ordinalRank = 1
            )
        )

        val entries2 = listOf(
            LeaderboardEntity(
                id = "player2",
                playerName = "Player2",
                rank = 2,
                points = 900,
                tier = "Legendary",
                ordinalRank = 2
            )
        )

        leaderboardDao.insertAll(entries1)
        leaderboardDao.insertAll(entries2)

        val retrieved = leaderboardDao.getAll()
        assertEquals(1, retrieved[0].ordinalRank)
        assertEquals(2, retrieved[1].ordinalRank)
        assertEquals(3, retrieved[2].ordinalRank)
    }

    @Test
    fun `getAll should handle reverse ordinal order insertion`() = runTest {
        val entries = (10 downTo 1).map { i ->
            LeaderboardEntity(
                id = "player$i",
                playerName = "Player$i",
                rank = i,
                points = 1000 - i,
                tier = "Tier${i % 10}",
                ordinalRank = i
            )
        }

        leaderboardDao.insertAll(entries)
        val retrieved = leaderboardDao.getAll()

        assertEquals(1, retrieved[0].ordinalRank)
        assertEquals(10, retrieved[9].ordinalRank)
    }

    // ─── STATE MANAGEMENT ───

    @Test
    fun `deleteAll should handle empty database`() = runTest {
        leaderboardDao.deleteAll()
        val retrieved = leaderboardDao.getAll()

        assertEquals(0, retrieved.size)
    }

    @Test
    fun `insertAll after deleteAll should work correctly`() = runTest {
        val entries1 = listOf(
            LeaderboardEntity(
                id = "player1",
                playerName = "Player1",
                rank = 1,
                points = 1000,
                tier = "Mythic",
                ordinalRank = 1
            )
        )

        leaderboardDao.insertAll(entries1)
        leaderboardDao.deleteAll()

        val entries2 = listOf(
            LeaderboardEntity(
                id = "player2",
                playerName = "Player2",
                rank = 2,
                points = 900,
                tier = "Legendary",
                ordinalRank = 2
            )
        )

        leaderboardDao.insertAll(entries2)
        val retrieved = leaderboardDao.getAll()

        assertEquals(1, retrieved.size)
        assertEquals("Player2", retrieved[0].playerName)
    }

    // ─── TIER VALIDATION ───

    @Test
    fun `insertAll should handle various tier values`() = runTest {
        val tiers = listOf("Mythic", "Legendary", "Epic", "Rare", "Common", "Unranked")
        val entries = tiers.mapIndexed { index, tier ->
            LeaderboardEntity(
                id = "player$index",
                playerName = "Player$index",
                rank = index + 1,
                points = 1000 - index * 100,
                tier = tier,
                ordinalRank = index + 1
            )
        }

        leaderboardDao.insertAll(entries)
        val retrieved = leaderboardDao.getAll()

        assertEquals(tiers.size, retrieved.size)
        retrieved.forEachIndexed { index, entity ->
            assertEquals(tiers[index], entity.tier)
        }
    }

    @Test
    fun `insertAll should handle empty tier string`() = runTest {
        val entry = LeaderboardEntity(
            id = "player1",
            playerName = "Player1",
            rank = 1,
            points = 1000,
            tier = "",
            ordinalRank = 1
        )

        leaderboardDao.insertAll(listOf(entry))
        val retrieved = leaderboardDao.getAll()

        assertEquals(1, retrieved.size)
        assertEquals("", retrieved[0].tier)
    }
}

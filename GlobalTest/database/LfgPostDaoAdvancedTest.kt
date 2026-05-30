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
 * Advanced tests for LfgPostDao covering:
 * - CRUD operations
 * - Query operations with filters
 * - Sorting and ordering
 * - Bulk operations
 * - Edge cases
 * - Data integrity
 * - Performance scenarios
 */
@RunWith(AndroidJUnit4::class)
class LfgPostDaoAdvancedTest {

    private lateinit var database: AppDatabase
    private lateinit var lfgPostDao: LfgPostDao

    @Before
    fun setup() {
        database = AppDatabase.createInMemory()
        lfgPostDao = database.lfgPostDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    // ─── CRUD OPERATIONS ───

    @Test
    fun `insertAll should store multiple LFG posts`() = runTest {
        val posts = listOf(
            LfgPostEntity(
                id = "post1",
                playerId = "player1",
                playerName = "Player1",
                rank = "Mythic",
                region = "EU",
                mode = "Ranked",
                message = "Looking for team",
                createdAt = System.currentTimeMillis()
            ),
            LfgPostEntity(
                id = "post2",
                playerId = "player2",
                playerName = "Player2",
                rank = "Legendary",
                region = "NA",
                mode = "Casual",
                message = "Need support",
                createdAt = System.currentTimeMillis() - 3600000
            ),
            LfgPostEntity(
                id = "post3",
                playerId = "player3",
                playerName = "Player3",
                rank = "Epic",
                region = "ASIA",
                mode = "Ranked",
                message = "LF team",
                createdAt = System.currentTimeMillis() - 7200000
            )
        )

        lfgPostDao.insertAll(posts)
        val retrieved = lfgPostDao.getAll()

        assertEquals(3, retrieved.size)
        assertEquals("post1", retrieved[0].id) // Most recent first
        assertEquals("post2", retrieved[1].id)
        assertEquals("post3", retrieved[2].id)
    }

    @Test
    fun `getAll should return posts ordered by createdAt DESC`() = runTest {
        val now = System.currentTimeMillis()
        val posts = listOf(
            LfgPostEntity(
                id = "post1",
                playerId = "player1",
                playerName = "Player1",
                rank = "Mythic",
                region = "EU",
                mode = "Ranked",
                message = "Looking for team",
                createdAt = now - 7200000 // Oldest
            ),
            LfgPostEntity(
                id = "post2",
                playerId = "player2",
                playerName = "Player2",
                rank = "Legendary",
                region = "NA",
                mode = "Casual",
                message = "Need support",
                createdAt = now - 3600000 // Middle
            ),
            LfgPostEntity(
                id = "post3",
                playerId = "player3",
                playerName = "Player3",
                rank = "Epic",
                region = "ASIA",
                mode = "Ranked",
                message = "LF team",
                createdAt = now // Newest
            )
        )

        lfgPostDao.insertAll(posts)
        val retrieved = lfgPostDao.getAll()

        assertEquals("post3", retrieved[0].id) // Newest first
        assertEquals("post2", retrieved[1].id)
        assertEquals("post1", retrieved[2].id) // Oldest last
    }

    @Test
    fun `getByPlayer should return posts for specific player`() = runTest {
        val posts = listOf(
            LfgPostEntity(
                id = "post1",
                playerId = "player1",
                playerName = "Player1",
                rank = "Mythic",
                region = "EU",
                mode = "Ranked",
                message = "Looking for team",
                createdAt = System.currentTimeMillis()
            ),
            LfgPostEntity(
                id = "post2",
                playerId = "player1",
                playerName = "Player1",
                rank = "Legendary",
                region = "NA",
                mode = "Casual",
                message = "Need support",
                createdAt = System.currentTimeMillis() - 3600000
            ),
            LfgPostEntity(
                id = "post3",
                playerId = "player2",
                playerName = "Player2",
                rank = "Epic",
                region = "ASIA",
                mode = "Ranked",
                message = "LF team",
                createdAt = System.currentTimeMillis()
            )
        )

        lfgPostDao.insertAll(posts)
        val player1Posts = lfgPostDao.getByPlayer("player1")
        val player2Posts = lfgPostDao.getByPlayer("player2")

        assertEquals(2, player1Posts.size)
        assertEquals(1, player2Posts.size)
        assertTrue(player1Posts.all { it.playerId == "player1" })
        assertTrue(player2Posts.all { it.playerId == "player2" })
    }

    @Test
    fun `getByPlayer should return empty list for non-existent player`() = runTest {
        val posts = listOf(
            LfgPostEntity(
                id = "post1",
                playerId = "player1",
                playerName = "Player1",
                rank = "Mythic",
                region = "EU",
                mode = "Ranked",
                message = "Looking for team",
                createdAt = System.currentTimeMillis()
            )
        )

        lfgPostDao.insertAll(posts)
        val retrieved = lfgPostDao.getByPlayer("non_existent_player")

        assertEquals(0, retrieved.size)
    }

    @Test
    fun `deleteAll should remove all LFG posts`() = runTest {
        val posts = listOf(
            LfgPostEntity(
                id = "post1",
                playerId = "player1",
                playerName = "Player1",
                rank = "Mythic",
                region = "EU",
                mode = "Ranked",
                message = "Looking for team",
                createdAt = System.currentTimeMillis()
            ),
            LfgPostEntity(
                id = "post2",
                playerId = "player2",
                playerName = "Player2",
                rank = "Legendary",
                region = "NA",
                mode = "Casual",
                message = "Need support",
                createdAt = System.currentTimeMillis()
            )
        )

        lfgPostDao.insertAll(posts)
        assertEquals(2, lfgPostDao.getAll().size)

        lfgPostDao.deleteAll()
        assertEquals(0, lfgPostDao.getAll().size)
    }

    @Test
    fun `deleteById should remove specific post`() = runTest {
        val posts = listOf(
            LfgPostEntity(
                id = "post1",
                playerId = "player1",
                playerName = "Player1",
                rank = "Mythic",
                region = "EU",
                mode = "Ranked",
                message = "Looking for team",
                createdAt = System.currentTimeMillis()
            ),
            LfgPostEntity(
                id = "post2",
                playerId = "player2",
                playerName = "Player2",
                rank = "Legendary",
                region = "NA",
                mode = "Casual",
                message = "Need support",
                createdAt = System.currentTimeMillis()
            )
        )

        lfgPostDao.insertAll(posts)
        lfgPostDao.deleteById("post1")

        val retrieved = lfgPostDao.getAll()
        assertEquals(1, retrieved.size)
        assertEquals("post2", retrieved[0].id)
    }

    @Test
    fun `deleteById should handle non-existent post`() = runTest {
        val posts = listOf(
            LfgPostEntity(
                id = "post1",
                playerId = "player1",
                playerName = "Player1",
                rank = "Mythic",
                region = "EU",
                mode = "Ranked",
                message = "Looking for team",
                createdAt = System.currentTimeMillis()
            )
        )

        lfgPostDao.insertAll(posts)
        lfgPostDao.deleteById("non_existent_post")

        assertEquals(1, lfgPostDao.getAll().size)
    }

    // ─── BULK OPERATIONS ───

    @Test
    fun `insertAll should handle empty list`() = runTest {
        lfgPostDao.insertAll(emptyList())
        val retrieved = lfgPostDao.getAll()

        assertEquals(0, retrieved.size)
    }

    @Test
    fun `insertAll should handle single entry`() = runTest {
        val post = LfgPostEntity(
            id = "post1",
            playerId = "player1",
            playerName = "Player1",
            rank = "Mythic",
            region = "EU",
            mode = "Ranked",
            message = "Looking for team",
            createdAt = System.currentTimeMillis()
        )

        lfgPostDao.insertAll(listOf(post))
        val retrieved = lfgPostDao.getAll()

        assertEquals(1, retrieved.size)
        assertEquals("post1", retrieved[0].id)
    }

    @Test
    fun `insertAll should replace existing posts on conflict`() = runTest {
        val originalPost = LfgPostEntity(
            id = "post1",
            playerId = "player1",
            playerName = "Player1",
            rank = "Mythic",
            region = "EU",
            mode = "Ranked",
            message = "Looking for team",
            createdAt = System.currentTimeMillis()
        )

        val updatedPost = LfgPostEntity(
            id = "post1",
            playerId = "player1",
            playerName = "Player1",
            rank = "Legendary",
            region = "NA",
            mode = "Casual",
            message = "Updated message",
            createdAt = System.currentTimeMillis() + 3600000
        )

        lfgPostDao.insertAll(listOf(originalPost))
        lfgPostDao.insertAll(listOf(updatedPost))

        val retrieved = lfgPostDao.getAll()
        assertEquals(1, retrieved.size)
        assertEquals("Updated message", retrieved[0].message)
        assertEquals("Legendary", retrieved[0].rank)
    }

    @Test
    fun `insertAll should handle large dataset`() = runTest {
        val posts = (1..1000).map { i ->
            LfgPostEntity(
                id = "post$i",
                playerId = "player$i",
                playerName = "Player$i",
                rank = "Rank${i % 10}",
                region = "Region${i % 5}",
                mode = "Mode${i % 3}",
                message = "Message $i",
                createdAt = System.currentTimeMillis() - (i * 1000)
            )
        }

        lfgPostDao.insertAll(posts)
        val retrieved = lfgPostDao.getAll()

        assertEquals(1000, retrieved.size)
    }

    // ─── EDGE CASES ───

    @Test
    fun `insertAll should handle posts with same timestamp`() = runTest {
        val timestamp = System.currentTimeMillis()
        val posts = listOf(
            LfgPostEntity(
                id = "post1",
                playerId = "player1",
                playerName = "Player1",
                rank = "Mythic",
                region = "EU",
                mode = "Ranked",
                message = "Looking for team",
                createdAt = timestamp
            ),
            LfgPostEntity(
                id = "post2",
                playerId = "player2",
                playerName = "Player2",
                rank = "Legendary",
                region = "NA",
                mode = "Casual",
                message = "Need support",
                createdAt = timestamp
            )
        )

        lfgPostDao.insertAll(posts)
        val retrieved = lfgPostDao.getAll()

        assertEquals(2, retrieved.size)
    }

    @Test
    fun `insertAll should handle posts with zero timestamp`() = runTest {
        val post = LfgPostEntity(
            id = "post1",
            playerId = "player1",
            playerName = "Player1",
            rank = "Mythic",
            region = "EU",
            mode = "Ranked",
            message = "Looking for team",
            createdAt = 0
        )

        lfgPostDao.insertAll(listOf(post))
        val retrieved = lfgPostDao.getAll()

        assertEquals(1, retrieved.size)
        assertEquals(0, retrieved[0].createdAt)
    }

    @Test
    fun `insertAll should handle posts with negative timestamp`() = runTest {
        val post = LfgPostEntity(
            id = "post1",
            playerId = "player1",
            playerName = "Player1",
            rank = "Mythic",
            region = "EU",
            mode = "Ranked",
            message = "Looking for team",
            createdAt = -123456789
        )

        lfgPostDao.insertAll(listOf(post))
        val retrieved = lfgPostDao.getAll()

        assertEquals(1, retrieved.size)
        assertEquals(-123456789, retrieved[0].createdAt)
    }

    @Test
    fun `insertAll should handle posts with very long messages`() = runTest {
        val longMessage = "A".repeat(10000)
        val post = LfgPostEntity(
            id = "post1",
            playerId = "player1",
            playerName = "Player1",
            rank = "Mythic",
            region = "EU",
            mode = "Ranked",
            message = longMessage,
            createdAt = System.currentTimeMillis()
        )

        lfgPostDao.insertAll(listOf(post))
        val retrieved = lfgPostDao.getAll()

        assertEquals(1, retrieved.size)
        assertEquals(longMessage, retrieved[0].message)
    }

    @Test
    fun `insertAll should handle posts with special characters in messages`() = runTest {
        val specialMessage = "Message!@#$%^&*()"
        val post = LfgPostEntity(
            id = "post1",
            playerId = "player1",
            playerName = "Player1",
            rank = "Mythic",
            region = "EU",
            mode = "Ranked",
            message = specialMessage,
            createdAt = System.currentTimeMillis()
        )

        lfgPostDao.insertAll(listOf(post))
        val retrieved = lfgPostDao.getAll()

        assertEquals(1, retrieved.size)
        assertEquals(specialMessage, retrieved[0].message)
    }

    @Test
    fun `insertAll should handle posts with unicode characters`() = runTest {
        val unicodeMessage = "寻找队友 🎉 特殊字符"
        val post = LfgPostEntity(
            id = "post1",
            playerId = "player1",
            playerName = "Player1",
            rank = "Mythic",
            region = "EU",
            mode = "Ranked",
            message = unicodeMessage,
            createdAt = System.currentTimeMillis()
        )

        lfgPostDao.insertAll(listOf(post))
        val retrieved = lfgPostDao.getAll()

        assertEquals(1, retrieved.size)
        assertEquals(unicodeMessage, retrieved[0].message)
    }

    @Test
    fun `insertAll should handle posts with empty message`() = runTest {
        val post = LfgPostEntity(
            id = "post1",
            playerId = "player1",
            playerName = "Player1",
            rank = "Mythic",
            region = "EU",
            mode = "Ranked",
            message = "",
            createdAt = System.currentTimeMillis()
        )

        lfgPostDao.insertAll(listOf(post))
        val retrieved = lfgPostDao.getAll()

        assertEquals(1, retrieved.size)
        assertEquals("", retrieved[0].message)
    }

    // ─── DATA INTEGRITY ───

    @Test
    fun `insertAll should maintain data consistency`() = runTest {
        val posts = listOf(
            LfgPostEntity(
                id = "post1",
                playerId = "player1",
                playerName = "Player1",
                rank = "Mythic",
                region = "EU",
                mode = "Ranked",
                message = "Looking for team",
                createdAt = System.currentTimeMillis()
            ),
            LfgPostEntity(
                id = "post2",
                playerId = "player2",
                playerName = "Player2",
                rank = "Legendary",
                region = "NA",
                mode = "Casual",
                message = "Need support",
                createdAt = System.currentTimeMillis()
            )
        )

        lfgPostDao.insertAll(posts)
        val retrieved = lfgPostDao.getAll()

        posts.forEach { original ->
            val found = retrieved.find { it.id == original.id }
            assertNotNull(found)
            assertEquals(original.playerId, found.playerId)
            assertEquals(original.playerName, found.playerName)
            assertEquals(original.rank, found.rank)
            assertEquals(original.region, found.region)
            assertEquals(original.mode, found.mode)
            assertEquals(original.message, found.message)
            assertEquals(original.createdAt, found.createdAt)
        }
    }

    @Test
    fun `getByPlayer should maintain correct order`() = runTest {
        val now = System.currentTimeMillis()
        val posts = listOf(
            LfgPostEntity(
                id = "post1",
                playerId = "player1",
                playerName = "Player1",
                rank = "Mythic",
                region = "EU",
                mode = "Ranked",
                message = "Looking for team",
                createdAt = now - 7200000
            ),
            LfgPostEntity(
                id = "post2",
                playerId = "player1",
                playerName = "Player1",
                rank = "Legendary",
                region = "NA",
                mode = "Casual",
                message = "Need support",
                createdAt = now - 3600000
            ),
            LfgPostEntity(
                id = "post3",
                playerId = "player1",
                playerName = "Player1",
                rank = "Epic",
                region = "ASIA",
                mode = "Ranked",
                message = "LF team",
                createdAt = now
            )
        )

        lfgPostDao.insertAll(posts)
        val retrieved = lfgPostDao.getByPlayer("player1")

        assertEquals(3, retrieved.size)
        assertEquals("post3", retrieved[0].id) // Most recent first
        assertEquals("post2", retrieved[1].id)
        assertEquals("post1", retrieved[2].id) // Oldest last
    }

    // ─── PERFORMANCE SCENARIOS ───

    @Test
    fun `getAll should perform efficiently with large dataset`() = runTest {
        val posts = (1..1000).map { i ->
            LfgPostEntity(
                id = "post$i",
                playerId = "player${i % 100}", // 100 unique players
                playerName = "Player$i",
                rank = "Rank${i % 10}",
                region = "Region${i % 5}",
                mode = "Mode${i % 3}",
                message = "Message $i",
                createdAt = System.currentTimeMillis() - (i * 1000)
            )
        }

        lfgPostDao.insertAll(posts)

        val startTime = System.currentTimeMillis()
        val retrieved = lfgPostDao.getAll()
        val endTime = System.currentTimeMillis()

        assertEquals(1000, retrieved.size)
        assertTrue(endTime - startTime < 1000, "getAll should complete in under 1 second for 1000 posts")
    }

    @Test
    fun `getByPlayer should perform efficiently with large dataset`() = runTest {
        val posts = (1..1000).map { i ->
            LfgPostEntity(
                id = "post$i",
                playerId = "player${i % 100}",
                playerName = "Player$i",
                rank = "Rank${i % 10}",
                region = "Region${i % 5}",
                mode = "Mode${i % 3}",
                message = "Message $i",
                createdAt = System.currentTimeMillis() - (i * 1000)
            )
        }

        lfgPostDao.insertAll(posts)

        val startTime = System.currentTimeMillis()
        val retrieved = lfgPostDao.getByPlayer("player1")
        val endTime = System.currentTimeMillis()

        assertEquals(10, retrieved.size) // player1 appears 10 times
        assertTrue(endTime - startTime < 1000, "getByPlayer should complete in under 1 second")
    }

    @Test
    fun `insertAll should perform efficiently with large dataset`() = runTest {
        val posts = (1..1000).map { i ->
            LfgPostEntity(
                id = "post$i",
                playerId = "player$i",
                playerName = "Player$i",
                rank = "Rank${i % 10}",
                region = "Region${i % 5}",
                mode = "Mode${i % 3}",
                message = "Message $i",
                createdAt = System.currentTimeMillis() - (i * 1000)
            )
        }

        val startTime = System.currentTimeMillis()
        lfgPostDao.insertAll(posts)
        val endTime = System.currentTimeMillis()

        assertTrue(endTime - startTime < 1000, "insertAll should complete in under 1 second for 1000 posts")
    }

    // ─── STATE MANAGEMENT ───

    @Test
    fun `deleteAll should handle empty database`() = runTest {
        lfgPostDao.deleteAll()
        val retrieved = lfgPostDao.getAll()

        assertEquals(0, retrieved.size)
    }

    @Test
    fun `insertAll after deleteAll should work correctly`() = runTest {
        val posts1 = listOf(
            LfgPostEntity(
                id = "post1",
                playerId = "player1",
                playerName = "Player1",
                rank = "Mythic",
                region = "EU",
                mode = "Ranked",
                message = "Looking for team",
                createdAt = System.currentTimeMillis()
            )
        )

        lfgPostDao.insertAll(posts1)
        lfgPostDao.deleteAll()

        val posts2 = listOf(
            LfgPostEntity(
                id = "post2",
                playerId = "player2",
                playerName = "Player2",
                rank = "Legendary",
                region = "NA",
                mode = "Casual",
                message = "Need support",
                createdAt = System.currentTimeMillis()
            )
        )

        lfgPostDao.insertAll(posts2)
        val retrieved = lfgPostDao.getAll()

        assertEquals(1, retrieved.size)
        assertEquals("post2", retrieved[0].id)
    }

    // ─── FILTERING TESTS ───

    @Test
    fun `getByPlayer should handle player with many posts`() = runTest {
        val posts = (1..100).map { i ->
            LfgPostEntity(
                id = "post$i",
                playerId = "player1", // Same player
                playerName = "Player1",
                rank = "Rank${i % 10}",
                region = "Region${i % 5}",
                mode = "Mode${i % 3}",
                message = "Message $i",
                createdAt = System.currentTimeMillis() - (i * 1000)
            )
        }

        lfgPostDao.insertAll(posts)
        val retrieved = lfgPostDao.getByPlayer("player1")

        assertEquals(100, retrieved.size)
        assertTrue(retrieved.all { it.playerId == "player1" })
    }

    @Test
    fun `getByPlayer should return posts in correct order`() = runTest {
        val now = System.currentTimeMillis()
        val posts = (1..10).map { i ->
            LfgPostEntity(
                id = "post$i",
                playerId = "player1",
                playerName = "Player1",
                rank = "Rank${i % 10}",
                region = "Region${i % 5}",
                mode = "Mode${i % 3}",
                message = "Message $i",
                createdAt = now - (i * 100000)
            )
        }

        lfgPostDao.insertAll(posts)
        val retrieved = lfgPostDao.getByPlayer("player1")

        assertEquals(10, retrieved.size)
        assertEquals("post1", retrieved[0].id) // Most recent
        assertEquals("post10", retrieved[9].id) // Oldest
    }
}

package com.mlbb.scrim.data.local

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.flow.first
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertNull
import kotlin.test.assertNotNull

/**
 * Advanced tests for ProfileDao covering:
 * - CRUD operations
 * - Flow-based queries
 * - Bulk operations
 * - Edge cases
 * - Data integrity
 * - Performance scenarios
 */
@RunWith(AndroidJUnit4::class)
class ProfileDaoAdvancedTest {

    private lateinit var database: AppDatabase
    private lateinit var profileDao: ProfileDao

    @Before
    fun setup() {
        database = AppDatabase.createInMemory()
        profileDao = database.profileDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    // ─── CRUD OPERATIONS ───

    @Test
    fun `insertProfile should store single profile`() = runTest {
        val profile = ProfileEntity(
            id = "profile1",
            username = "Player1",
            rank = "Mythic",
            region = "EU",
            avatarUrl = "https://example.com/avatar.jpg",
            bio = "Professional player",
            createdAt = System.currentTimeMillis()
        )

        profileDao.insertProfile(profile)
        val retrieved = profileDao.getProfileById("profile1").first()

        assertNotNull(retrieved)
        assertEquals("Player1", retrieved.username)
        assertEquals("Mythic", retrieved.rank)
    }

    @Test
    fun `insertProfiles should store multiple profiles`() = runTest {
        val profiles = listOf(
            ProfileEntity(
                id = "profile1",
                username = "Player1",
                rank = "Mythic",
                region = "EU",
                avatarUrl = "https://example.com/avatar1.jpg",
                bio = "Professional player",
                createdAt = System.currentTimeMillis()
            ),
            ProfileEntity(
                id = "profile2",
                username = "Player2",
                rank = "Legendary",
                region = "NA",
                avatarUrl = "https://example.com/avatar2.jpg",
                bio = "Casual player",
                createdAt = System.currentTimeMillis()
            ),
            ProfileEntity(
                id = "profile3",
                username = "Player3",
                rank = "Epic",
                region = "ASIA",
                avatarUrl = "https://example.com/avatar3.jpg",
                bio = "New player",
                createdAt = System.currentTimeMillis()
            )
        )

        profileDao.insertProfiles(profiles)
        
        val profile1 = profileDao.getProfileById("profile1").first()
        val profile2 = profileDao.getProfileById("profile2").first()
        val profile3 = profileDao.getProfileById("profile3").first()

        assertNotNull(profile1)
        assertNotNull(profile2)
        assertNotNull(profile3)
        assertEquals("Player1", profile1.username)
        assertEquals("Player2", profile2.username)
        assertEquals("Player3", profile3.username)
    }

    @Test
    fun `getProfileById should return null for non-existent profile`() = runTest {
        val retrieved = profileDao.getProfileById("non_existent_profile").first()

        assertNull(retrieved)
    }

    @Test
    fun `deleteProfile should remove specific profile`() = runTest {
        val profile = ProfileEntity(
            id = "profile1",
            username = "Player1",
            rank = "Mythic",
            region = "EU",
            avatarUrl = "https://example.com/avatar.jpg",
            bio = "Professional player",
            createdAt = System.currentTimeMillis()
        )

        profileDao.insertProfile(profile)
        profileDao.deleteProfile("profile1")

        val retrieved = profileDao.getProfileById("profile1").first()
        assertNull(retrieved)
    }

    @Test
    fun `deleteProfile should handle non-existent profile`() = runTest {
        val profile = ProfileEntity(
            id = "profile1",
            username = "Player1",
            rank = "Mythic",
            region = "EU",
            avatarUrl = "https://example.com/avatar.jpg",
            bio = "Professional player",
            createdAt = System.currentTimeMillis()
        )

        profileDao.insertProfile(profile)
        profileDao.deleteProfile("non_existent_profile")

        val retrieved = profileDao.getProfileById("profile1").first()
        assertNotNull(retrieved) // Original profile should still exist
    }

    // ─── FLOW-BASED QUERIES ───

    @Test
    fun `getProfileById should emit updates when profile changes`() = runTest {
        val originalProfile = ProfileEntity(
            id = "profile1",
            username = "Player1",
            rank = "Mythic",
            region = "EU",
            avatarUrl = "https://example.com/avatar.jpg",
            bio = "Professional player",
            createdAt = System.currentTimeMillis()
        )

        profileDao.insertProfile(originalProfile)
        val firstEmission = profileDao.getProfileById("profile1").first()
        assertEquals("Player1", firstEmission.username)

        val updatedProfile = originalProfile.copy(username = "Player1Updated", rank = "Legendary")
        profileDao.insertProfile(updatedProfile)
        val secondEmission = profileDao.getProfileById("profile1").first()
        assertEquals("Player1Updated", secondEmission.username)
        assertEquals("Legendary", secondEmission.rank)
    }

    @Test
    fun `getProfileById should emit null when profile is deleted`() = runTest {
        val profile = ProfileEntity(
            id = "profile1",
            username = "Player1",
            rank = "Mythic",
            region = "EU",
            avatarUrl = "https://example.com/avatar.jpg",
            bio = "Professional player",
            createdAt = System.currentTimeMillis()
        )

        profileDao.insertProfile(profile)
        val firstEmission = profileDao.getProfileById("profile1").first()
        assertNotNull(firstEmission)

        profileDao.deleteProfile("profile1")
        val secondEmission = profileDao.getProfileById("profile1").first()
        assertNull(secondEmission)
    }

    // ─── BULK OPERATIONS ───

    @Test
    fun `insertProfiles should handle empty list`() = runTest {
        profileDao.insertProfiles(emptyList())
        val retrieved = profileDao.getProfileById("profile1").first()

        assertNull(retrieved)
    }

    @Test
    fun `insertProfiles should handle single entry`() = runTest {
        val profile = ProfileEntity(
            id = "profile1",
            username = "Player1",
            rank = "Mythic",
            region = "EU",
            avatarUrl = "https://example.com/avatar.jpg",
            bio = "Professional player",
            createdAt = System.currentTimeMillis()
        )

        profileDao.insertProfiles(listOf(profile))
        val retrieved = profileDao.getProfileById("profile1").first()

        assertNotNull(retrieved)
        assertEquals("Player1", retrieved.username)
    }

    @Test
    fun `insertProfiles should replace existing profiles on conflict`() = runTest {
        val originalProfile = ProfileEntity(
            id = "profile1",
            username = "Player1",
            rank = "Mythic",
            region = "EU",
            avatarUrl = "https://example.com/avatar.jpg",
            bio = "Professional player",
            createdAt = System.currentTimeMillis()
        )

        val updatedProfile = ProfileEntity(
            id = "profile1",
            username = "Player1Updated",
            rank = "Legendary",
            region = "NA",
            avatarUrl = "https://example.com/avatar_updated.jpg",
            bio = "Updated bio",
            createdAt = System.currentTimeMillis() + 3600000
        )

        profileDao.insertProfiles(listOf(originalProfile))
        profileDao.insertProfiles(listOf(updatedProfile))

        val retrieved = profileDao.getProfileById("profile1").first()
        assertEquals("Player1Updated", retrieved.username)
        assertEquals("Legendary", retrieved.rank)
        assertEquals("NA", retrieved.region)
    }

    @Test
    fun `insertProfiles should handle large dataset`() = runTest {
        val profiles = (1..1000).map { i ->
            ProfileEntity(
                id = "profile$i",
                username = "Player$i",
                rank = "Rank${i % 10}",
                region = "Region${i % 5}",
                avatarUrl = "https://example.com/avatar$i.jpg",
                bio = "Bio $i",
                createdAt = System.currentTimeMillis() - (i * 1000)
            )
        }

        profileDao.insertProfiles(profiles)
        
        val profile1 = profileDao.getProfileById("profile1").first()
        val profile500 = profileDao.getProfileById("profile500").first()
        val profile1000 = profileDao.getProfileById("profile1000").first()

        assertNotNull(profile1)
        assertNotNull(profile500)
        assertNotNull(profile1000)
    }

    // ─── EDGE CASES ───

    @Test
    fun `insertProfile should handle profile with null avatarUrl`() = runTest {
        val profile = ProfileEntity(
            id = "profile1",
            username = "Player1",
            rank = "Mythic",
            region = "EU",
            avatarUrl = null,
            bio = "Professional player",
            createdAt = System.currentTimeMillis()
        )

        profileDao.insertProfile(profile)
        val retrieved = profileDao.getProfileById("profile1").first()

        assertNotNull(retrieved)
        assertNull(retrieved.avatarUrl)
    }

    @Test
    fun `insertProfile should handle profile with null bio`() = runTest {
        val profile = ProfileEntity(
            id = "profile1",
            username = "Player1",
            rank = "Mythic",
            region = "EU",
            avatarUrl = "https://example.com/avatar.jpg",
            bio = null,
            createdAt = System.currentTimeMillis()
        )

        profileDao.insertProfile(profile)
        val retrieved = profileDao.getProfileById("profile1").first()

        assertNotNull(retrieved)
        assertNull(retrieved.bio)
    }

    @Test
    fun `insertProfile should handle profile with very long username`() = runTest {
        val longUsername = "A".repeat(10000)
        val profile = ProfileEntity(
            id = "profile1",
            username = longUsername,
            rank = "Mythic",
            region = "EU",
            avatarUrl = "https://example.com/avatar.jpg",
            bio = "Professional player",
            createdAt = System.currentTimeMillis()
        )

        profileDao.insertProfile(profile)
        val retrieved = profileDao.getProfileById("profile1").first()

        assertNotNull(retrieved)
        assertEquals(longUsername, retrieved.username)
    }

    @Test
    fun `insertProfile should handle profile with very long bio`() = runTest {
        val longBio = "A".repeat(10000)
        val profile = ProfileEntity(
            id = "profile1",
            username = "Player1",
            rank = "Mythic",
            region = "EU",
            avatarUrl = "https://example.com/avatar.jpg",
            bio = longBio,
            createdAt = System.currentTimeMillis()
        )

        profileDao.insertProfile(profile)
        val retrieved = profileDao.getProfileById("profile1").first()

        assertNotNull(retrieved)
        assertEquals(longBio, retrieved.bio)
    }

    @Test
    fun `insertProfile should handle profile with special characters`() = runTest {
        val specialUsername = "Player!@#$%^&*()"
        val specialBio = "Bio!@#$%^&*()"
        val profile = ProfileEntity(
            id = "profile1",
            username = specialUsername,
            rank = "Mythic",
            region = "EU",
            avatarUrl = "https://example.com/avatar.jpg",
            bio = specialBio,
            createdAt = System.currentTimeMillis()
        )

        profileDao.insertProfile(profile)
        val retrieved = profileDao.getProfileById("profile1").first()

        assertNotNull(retrieved)
        assertEquals(specialUsername, retrieved.username)
        assertEquals(specialBio, retrieved.bio)
    }

    @Test
    fun `insertProfile should handle profile with unicode characters`() = runTest {
        val unicodeUsername = "玩家123 😀"
        val unicodeBio = "简介 🎉 特殊字符"
        val profile = ProfileEntity(
            id = "profile1",
            username = unicodeUsername,
            rank = "Mythic",
            region = "EU",
            avatarUrl = "https://example.com/avatar.jpg",
            bio = unicodeBio,
            createdAt = System.currentTimeMillis()
        )

        profileDao.insertProfile(profile)
        val retrieved = profileDao.getProfileById("profile1").first()

        assertNotNull(retrieved)
        assertEquals(unicodeUsername, retrieved.username)
        assertEquals(unicodeBio, retrieved.bio)
    }

    @Test
    fun `insertProfile should handle profile with empty username and bio`() = runTest {
        val profile = ProfileEntity(
            id = "profile1",
            username = "",
            rank = "Mythic",
            region = "EU",
            avatarUrl = "https://example.com/avatar.jpg",
            bio = "",
            createdAt = System.currentTimeMillis()
        )

        profileDao.insertProfile(profile)
        val retrieved = profileDao.getProfileById("profile1").first()

        assertNotNull(retrieved)
        assertEquals("", retrieved.username)
        assertEquals("", retrieved.bio)
    }

    @Test
    fun `insertProfile should handle profile with zero timestamp`() = runTest {
        val profile = ProfileEntity(
            id = "profile1",
            username = "Player1",
            rank = "Mythic",
            region = "EU",
            avatarUrl = "https://example.com/avatar.jpg",
            bio = "Professional player",
            createdAt = 0
        )

        profileDao.insertProfile(profile)
        val retrieved = profileDao.getProfileById("profile1").first()

        assertNotNull(retrieved)
        assertEquals(0, retrieved.createdAt)
    }

    @Test
    fun `insertProfile should handle profile with negative timestamp`() = runTest {
        val profile = ProfileEntity(
            id = "profile1",
            username = "Player1",
            rank = "Mythic",
            region = "EU",
            avatarUrl = "https://example.com/avatar.jpg",
            bio = "Professional player",
            createdAt = -123456789
        )

        profileDao.insertProfile(profile)
        val retrieved = profileDao.getProfileById("profile1").first()

        assertNotNull(retrieved)
        assertEquals(-123456789, retrieved.createdAt)
    }

    // ─── DATA INTEGRITY ───

    @Test
    fun `insertProfile should maintain data consistency`() = runTest {
        val profile = ProfileEntity(
            id = "profile1",
            username = "Player1",
            rank = "Mythic",
            region = "EU",
            avatarUrl = "https://example.com/avatar.jpg",
            bio = "Professional player",
            createdAt = System.currentTimeMillis()
        )

        profileDao.insertProfile(profile)
        val retrieved = profileDao.getProfileById("profile1").first()

        assertEquals(profile.id, retrieved.id)
        assertEquals(profile.username, retrieved.username)
        assertEquals(profile.rank, retrieved.rank)
        assertEquals(profile.region, retrieved.region)
        assertEquals(profile.avatarUrl, retrieved.avatarUrl)
        assertEquals(profile.bio, retrieved.bio)
        assertEquals(profile.createdAt, retrieved.createdAt)
    }

    @Test
    fun `insertProfiles should maintain data consistency`() = runTest {
        val profiles = listOf(
            ProfileEntity(
                id = "profile1",
                username = "Player1",
                rank = "Mythic",
                region = "EU",
                avatarUrl = "https://example.com/avatar1.jpg",
                bio = "Bio 1",
                createdAt = System.currentTimeMillis()
            ),
            ProfileEntity(
                id = "profile2",
                username = "Player2",
                rank = "Legendary",
                region = "NA",
                avatarUrl = "https://example.com/avatar2.jpg",
                bio = "Bio 2",
                createdAt = System.currentTimeMillis()
            )
        )

        profileDao.insertProfiles(profiles)

        profiles.forEach { original ->
            val retrieved = profileDao.getProfileById(original.id).first()
            assertNotNull(retrieved)
            assertEquals(original.id, retrieved.id)
            assertEquals(original.username, retrieved.username)
            assertEquals(original.rank, retrieved.rank)
            assertEquals(original.region, retrieved.region)
            assertEquals(original.avatarUrl, retrieved.avatarUrl)
            assertEquals(original.bio, retrieved.bio)
            assertEquals(original.createdAt, retrieved.createdAt)
        }
    }

    // ─── PERFORMANCE SCENARIOS ───

    @Test
    fun `getProfileById should perform efficiently with large dataset`() = runTest {
        val profiles = (1..1000).map { i ->
            ProfileEntity(
                id = "profile$i",
                username = "Player$i",
                rank = "Rank${i % 10}",
                region = "Region${i % 5}",
                avatarUrl = "https://example.com/avatar$i.jpg",
                bio = "Bio $i",
                createdAt = System.currentTimeMillis() - (i * 1000)
            )
        }

        profileDao.insertProfiles(profiles)

        val startTime = System.currentTimeMillis()
        val retrieved = profileDao.getProfileById("profile500").first()
        val endTime = System.currentTimeMillis()

        assertNotNull(retrieved)
        assertTrue(endTime - startTime < 1000, "getProfileById should complete in under 1 second")
    }

    @Test
    fun `insertProfiles should perform efficiently with large dataset`() = runTest {
        val profiles = (1..1000).map { i ->
            ProfileEntity(
                id = "profile$i",
                username = "Player$i",
                rank = "Rank${i % 10}",
                region = "Region${i % 5}",
                avatarUrl = "https://example.com/avatar$i.jpg",
                bio = "Bio $i",
                createdAt = System.currentTimeMillis() - (i * 1000)
            )
        }

        val startTime = System.currentTimeMillis()
        profileDao.insertProfiles(profiles)
        val endTime = System.currentTimeMillis()

        assertTrue(endTime - startTime < 1000, "insertProfiles should complete in under 1 second for 1000 profiles")
    }

    // ─── STATE MANAGEMENT ───

    @Test
    fun `deleteProfile should handle empty database`() = runTest {
        profileDao.deleteProfile("profile1")
        val retrieved = profileDao.getProfileById("profile1").first()

        assertNull(retrieved)
    }

    @Test
    fun `insertProfile after delete should work correctly`() = runTest {
        val profile1 = ProfileEntity(
            id = "profile1",
            username = "Player1",
            rank = "Mythic",
            region = "EU",
            avatarUrl = "https://example.com/avatar.jpg",
            bio = "Professional player",
            createdAt = System.currentTimeMillis()
        )

        profileDao.insertProfile(profile1)
        profileDao.deleteProfile("profile1")

        val profile2 = ProfileEntity(
            id = "profile1",
            username = "Player1Updated",
            rank = "Legendary",
            region = "NA",
            avatarUrl = "https://example.com/avatar_updated.jpg",
            bio = "Updated bio",
            createdAt = System.currentTimeMillis()
        )

        profileDao.insertProfile(profile2)
        val retrieved = profileDao.getProfileById("profile1").first()

        assertNotNull(retrieved)
        assertEquals("Player1Updated", retrieved.username)
    }

    // ─── NULL HANDLING ───

    @Test
    fun `insertProfile should handle all null optional fields`() = runTest {
        val profile = ProfileEntity(
            id = "profile1",
            username = "Player1",
            rank = "Mythic",
            region = "EU",
            avatarUrl = null,
            bio = null,
            createdAt = System.currentTimeMillis()
        )

        profileDao.insertProfile(profile)
        val retrieved = profileDao.getProfileById("profile1").first()

        assertNotNull(retrieved)
        assertNull(retrieved.avatarUrl)
        assertNull(retrieved.bio)
    }

    @Test
    fun `insertProfile should handle mixed null and non-null fields`() = runTest {
        val profile = ProfileEntity(
            id = "profile1",
            username = "Player1",
            rank = "Mythic",
            region = "EU",
            avatarUrl = "https://example.com/avatar.jpg",
            bio = null,
            createdAt = System.currentTimeMillis()
        )

        profileDao.insertProfile(profile)
        val retrieved = profileDao.getProfileById("profile1").first()

        assertNotNull(retrieved)
        assertEquals("https://example.com/avatar.jpg", retrieved.avatarUrl)
        assertNull(retrieved.bio)
    }
}

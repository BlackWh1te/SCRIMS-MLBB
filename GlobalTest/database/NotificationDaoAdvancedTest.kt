package com.mlbb.scrim.data.local

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

/**
 * Advanced tests for NotificationDao covering:
 * - CRUD operations
 * - Query operations with filters
 * - Update operations
 * - Bulk operations
 * - Edge cases
 * - Data integrity
 * - Performance scenarios
 */
@RunWith(AndroidJUnit4::class)
class NotificationDaoAdvancedTest {

    private lateinit var database: AppDatabase
    private lateinit var notificationDao: NotificationDao

    @Before
    fun setup() {
        database = AppDatabase.createInMemory()
        notificationDao = database.notificationDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    // ─── CRUD OPERATIONS ───

    @Test
    fun `insertAll should store multiple notifications`() = runTest {
        val notifications = listOf(
            NotificationEntity(
                id = "notif1",
                userId = "user1",
                title = "New Message",
                body = "You have a new message",
                type = "message",
                timestamp = System.currentTimeMillis(),
                isRead = 0
            ),
            NotificationEntity(
                id = "notif2",
                userId = "user1",
                title = "Team Invite",
                body = "You have been invited to a team",
                type = "team_invite",
                timestamp = System.currentTimeMillis() - 3600000,
                isRead = 0
            ),
            NotificationEntity(
                id = "notif3",
                userId = "user2",
                title = "Scrim Update",
                body = "Your scrim has been updated",
                type = "scrim",
                timestamp = System.currentTimeMillis(),
                isRead = 0
            )
        )

        notificationDao.insertAll(notifications)
        val retrieved = notificationDao.getForUser("user1")

        assertEquals(2, retrieved.size)
        assertEquals("New Message", retrieved[0].title)
        assertEquals("Team Invite", retrieved[1].title)
    }

    @Test
    fun `insert should store single notification`() = runTest {
        val notification = NotificationEntity(
            id = "notif1",
            userId = "user1",
            title = "New Message",
            body = "You have a new message",
            type = "message",
            timestamp = System.currentTimeMillis(),
            isRead = 0
        )

        notificationDao.insert(notification)
        val retrieved = notificationDao.getForUser("user1")

        assertEquals(1, retrieved.size)
        assertEquals("New Message", retrieved[0].title)
    }

    @Test
    fun `getForUser should return notifications ordered by timestamp DESC`() = runTest {
        val now = System.currentTimeMillis()
        val notifications = listOf(
            NotificationEntity(
                id = "notif1",
                userId = "user1",
                title = "Oldest",
                body = "Oldest notification",
                type = "message",
                timestamp = now - 7200000,
                isRead = 0
            ),
            NotificationEntity(
                id = "notif2",
                userId = "user1",
                title = "Middle",
                body = "Middle notification",
                type = "team_invite",
                timestamp = now - 3600000,
                isRead = 0
            ),
            NotificationEntity(
                id = "notif3",
                userId = "user1",
                title = "Newest",
                body = "Newest notification",
                type = "scrim",
                timestamp = now,
                isRead = 0
            )
        )

        notificationDao.insertAll(notifications)
        val retrieved = notificationDao.getForUser("user1")

        assertEquals("Newest", retrieved[0].title) // Most recent first
        assertEquals("Middle", retrieved[1].title)
        assertEquals("Oldest", retrieved[2].title) // Oldest last
    }

    @Test
    fun `getForUser should return empty list for non-existent user`() = runTest {
        val notifications = listOf(
            NotificationEntity(
                id = "notif1",
                userId = "user1",
                title = "New Message",
                body = "You have a new message",
                type = "message",
                timestamp = System.currentTimeMillis(),
                isRead = 0
            )
        )

        notificationDao.insertAll(notifications)
        val retrieved = notificationDao.getForUser("non_existent_user")

        assertEquals(0, retrieved.size)
    }

    @Test
    fun `markAsRead should update specific notification`() = runTest {
        val notification = NotificationEntity(
            id = "notif1",
            userId = "user1",
            title = "New Message",
            body = "You have a new message",
            type = "message",
            timestamp = System.currentTimeMillis(),
            isRead = 0
        )

        notificationDao.insert(notification)
        notificationDao.markAsRead("notif1")

        val retrieved = notificationDao.getForUser("user1")
        assertEquals(1, retrieved.size)
        assertEquals(1, retrieved[0].isRead) // Should be marked as read
    }

    @Test
    fun `markAllAsRead should update all notifications for user`() = runTest {
        val notifications = listOf(
            NotificationEntity(
                id = "notif1",
                userId = "user1",
                title = "Notification 1",
                body = "Body 1",
                type = "message",
                timestamp = System.currentTimeMillis(),
                isRead = 0
            ),
            NotificationEntity(
                id = "notif2",
                userId = "user1",
                title = "Notification 2",
                body = "Body 2",
                type = "team_invite",
                timestamp = System.currentTimeMillis() - 3600000,
                isRead = 0
            ),
            NotificationEntity(
                id = "notif3",
                userId = "user2",
                title = "Other User Notification",
                body = "Body 3",
                type = "scrim",
                timestamp = System.currentTimeMillis(),
                isRead = 0
            )
        )

        notificationDao.insertAll(notifications)
        notificationDao.markAllAsRead("user1")

        val user1Notifications = notificationDao.getForUser("user1")
        val user2Notifications = notificationDao.getForUser("user2")

        assertEquals(2, user1Notifications.size)
        assertTrue(user1Notifications.all { it.isRead == 1 }) // All user1 notifications should be read

        assertEquals(1, user2Notifications.size)
        assertEquals(0, user2Notifications[0].isRead) // user2 notification should remain unread
    }

    @Test
    fun `deleteById should remove specific notification`() = runTest {
        val notifications = listOf(
            NotificationEntity(
                id = "notif1",
                userId = "user1",
                title = "Notification 1",
                body = "Body 1",
                type = "message",
                timestamp = System.currentTimeMillis(),
                isRead = 0
            ),
            NotificationEntity(
                id = "notif2",
                userId = "user1",
                title = "Notification 2",
                body = "Body 2",
                type = "team_invite",
                timestamp = System.currentTimeMillis(),
                isRead = 0
            )
        )

        notificationDao.insertAll(notifications)
        notificationDao.deleteById("notif1")

        val retrieved = notificationDao.getForUser("user1")
        assertEquals(1, retrieved.size)
        assertEquals("notif2", retrieved[0].id)
    }

    @Test
    fun `deleteAllForUser should remove all notifications for user`() = runTest {
        val notifications = listOf(
            NotificationEntity(
                id = "notif1",
                userId = "user1",
                title = "Notification 1",
                body = "Body 1",
                type = "message",
                timestamp = System.currentTimeMillis(),
                isRead = 0
            ),
            NotificationEntity(
                id = "notif2",
                userId = "user1",
                title = "Notification 2",
                body = "Body 2",
                type = "team_invite",
                timestamp = System.currentTimeMillis(),
                isRead = 0
            ),
            NotificationEntity(
                id = "notif3",
                userId = "user2",
                title = "Other User Notification",
                body = "Body 3",
                type = "scrim",
                timestamp = System.currentTimeMillis(),
                isRead = 0
            )
        )

        notificationDao.insertAll(notifications)
        notificationDao.deleteAllForUser("user1")

        val user1Notifications = notificationDao.getForUser("user1")
        val user2Notifications = notificationDao.getForUser("user2")

        assertEquals(0, user1Notifications.size) // All user1 notifications should be deleted
        assertEquals(1, user2Notifications.size) // user2 notification should remain
    }

    // ─── BULK OPERATIONS ───

    @Test
    fun `insertAll should handle empty list`() = runTest {
        notificationDao.insertAll(emptyList())
        val retrieved = notificationDao.getForUser("user1")

        assertEquals(0, retrieved.size)
    }

    @Test
    fun `insertAll should handle single entry`() = runTest {
        val notification = NotificationEntity(
            id = "notif1",
            userId = "user1",
            title = "New Message",
            body = "You have a new message",
            type = "message",
            timestamp = System.currentTimeMillis(),
            isRead = 0
        )

        notificationDao.insertAll(listOf(notification))
        val retrieved = notificationDao.getForUser("user1")

        assertEquals(1, retrieved.size)
    }

    @Test
    fun `insertAll should replace existing notifications on conflict`() = runTest {
        val originalNotification = NotificationEntity(
            id = "notif1",
            userId = "user1",
            title = "Original Title",
            body = "Original body",
            type = "message",
            timestamp = System.currentTimeMillis(),
            isRead = 0
        )

        val updatedNotification = NotificationEntity(
            id = "notif1",
            userId = "user1",
            title = "Updated Title",
            body = "Updated body",
            type = "team_invite",
            timestamp = System.currentTimeMillis() + 3600000,
            isRead = 1
        )

        notificationDao.insertAll(listOf(originalNotification))
        notificationDao.insertAll(listOf(updatedNotification))

        val retrieved = notificationDao.getForUser("user1")
        assertEquals(1, retrieved.size)
        assertEquals("Updated Title", retrieved[0].title)
        assertEquals(1, retrieved[0].isRead)
    }

    @Test
    fun `insertAll should handle large dataset`() = runTest {
        val notifications = (1..1000).map { i ->
            NotificationEntity(
                id = "notif$i",
                userId = "user${i % 10}",
                title = "Title $i",
                body = "Body $i",
                type = "type${i % 5}",
                timestamp = System.currentTimeMillis() - (i * 1000),
                isRead = if (i % 2 == 0) 1 else 0
            )
        }

        notificationDao.insertAll(notifications)
        val retrieved = notificationDao.getForUser("user1")

        assertEquals(100, retrieved.size) // user1 has 100 notifications
    }

    // ─── UPDATE OPERATIONS ───

    @Test
    fun `markAsRead should handle non-existent notification`() = runTest {
        val notification = NotificationEntity(
            id = "notif1",
            userId = "user1",
            title = "New Message",
            body = "You have a new message",
            type = "message",
            timestamp = System.currentTimeMillis(),
            isRead = 0
        )

        notificationDao.insert(notification)
        notificationDao.markAsRead("non_existent_notif")

        val retrieved = notificationDao.getForUser("user1")
        assertEquals(1, retrieved.size)
        assertEquals(0, retrieved[0].isRead) // Should remain unchanged
    }

    @Test
    fun `markAllAsRead should handle user with no notifications`() = runTest {
        notificationDao.markAllAsRead("user1")
        val retrieved = notificationDao.getForUser("user1")

        assertEquals(0, retrieved.size)
    }

    @Test
    fun `markAllAsRead should handle already read notifications`() = runTest {
        val notifications = listOf(
            NotificationEntity(
                id = "notif1",
                userId = "user1",
                title = "Notification 1",
                body = "Body 1",
                type = "message",
                timestamp = System.currentTimeMillis(),
                isRead = 1
            ),
            NotificationEntity(
                id = "notif2",
                userId = "user1",
                title = "Notification 2",
                body = "Body 2",
                type = "team_invite",
                timestamp = System.currentTimeMillis(),
                isRead = 0
            )
        )

        notificationDao.insertAll(notifications)
        notificationDao.markAllAsRead("user1")

        val retrieved = notificationDao.getForUser("user1")
        assertEquals(2, retrieved.size)
        assertTrue(retrieved.all { it.isRead == 1 }) // All should be read
    }

    // ─── EDGE CASES ───

    @Test
    fun `insertAll should handle notifications with same timestamp`() = runTest {
        val timestamp = System.currentTimeMillis()
        val notifications = listOf(
            NotificationEntity(
                id = "notif1",
                userId = "user1",
                title = "Notification 1",
                body = "Body 1",
                type = "message",
                timestamp = timestamp,
                isRead = 0
            ),
            NotificationEntity(
                id = "notif2",
                userId = "user1",
                title = "Notification 2",
                body = "Body 2",
                type = "team_invite",
                timestamp = timestamp,
                isRead = 0
            )
        )

        notificationDao.insertAll(notifications)
        val retrieved = notificationDao.getForUser("user1")

        assertEquals(2, retrieved.size)
    }

    @Test
    fun `insertAll should handle notifications with zero timestamp`() = runTest {
        val notification = NotificationEntity(
            id = "notif1",
            userId = "user1",
            title = "New Message",
            body = "You have a new message",
            type = "message",
            timestamp = 0,
            isRead = 0
        )

        notificationDao.insertAll(listOf(notification))
        val retrieved = notificationDao.getForUser("user1")

        assertEquals(1, retrieved.size)
        assertEquals(0, retrieved[0].timestamp)
    }

    @Test
    fun `insertAll should handle notifications with negative timestamp`() = runTest {
        val notification = NotificationEntity(
            id = "notif1",
            userId = "user1",
            title = "New Message",
            body = "You have a new message",
            type = "message",
            timestamp = -123456789,
            isRead = 0
        )

        notificationDao.insertAll(listOf(notification))
        val retrieved = notificationDao.getForUser("user1")

        assertEquals(1, retrieved.size)
        assertEquals(-123456789, retrieved[0].timestamp)
    }

    @Test
    fun `insertAll should handle notifications with very long titles`() = runTest {
        val longTitle = "A".repeat(10000)
        val notification = NotificationEntity(
            id = "notif1",
            userId = "user1",
            title = longTitle,
            body = "You have a new message",
            type = "message",
            timestamp = System.currentTimeMillis(),
            isRead = 0
        )

        notificationDao.insertAll(listOf(notification))
        val retrieved = notificationDao.getForUser("user1")

        assertEquals(1, retrieved.size)
        assertEquals(longTitle, retrieved[0].title)
    }

    @Test
    fun `insertAll should handle notifications with very long bodies`() = runTest {
        val longBody = "A".repeat(10000)
        val notification = NotificationEntity(
            id = "notif1",
            userId = "user1",
            title = "New Message",
            body = longBody,
            type = "message",
            timestamp = System.currentTimeMillis(),
            isRead = 0
        )

        notificationDao.insertAll(listOf(notification))
        val retrieved = notificationDao.getForUser("user1")

        assertEquals(1, retrieved.size)
        assertEquals(longBody, retrieved[0].body)
    }

    @Test
    fun `insertAll should handle notifications with special characters`() = runTest {
        val specialTitle = "Title!@#$%^&*()"
        val specialBody = "Body!@#$%^&*()"
        val notification = NotificationEntity(
            id = "notif1",
            userId = "user1",
            title = specialTitle,
            body = specialBody,
            type = "message",
            timestamp = System.currentTimeMillis(),
            isRead = 0
        )

        notificationDao.insertAll(listOf(notification))
        val retrieved = notificationDao.getForUser("user1")

        assertEquals(1, retrieved.size)
        assertEquals(specialTitle, retrieved[0].title)
        assertEquals(specialBody, retrieved[0].body)
    }

    @Test
    fun `insertAll should handle notifications with unicode characters`() = runTest {
        val unicodeTitle = "标题 🎉 特殊字符"
        val unicodeBody = "内容 🎉 特殊字符"
        val notification = NotificationEntity(
            id = "notif1",
            userId = "user1",
            title = unicodeTitle,
            body = unicodeBody,
            type = "message",
            timestamp = System.currentTimeMillis(),
            isRead = 0
        )

        notificationDao.insertAll(listOf(notification))
        val retrieved = notificationDao.getForUser("user1")

        assertEquals(1, retrieved.size)
        assertEquals(unicodeTitle, retrieved[0].title)
        assertEquals(unicodeBody, retrieved[0].body)
    }

    @Test
    fun `insertAll should handle notifications with empty title and body`() = runTest {
        val notification = NotificationEntity(
            id = "notif1",
            userId = "user1",
            title = "",
            body = "",
            type = "message",
            timestamp = System.currentTimeMillis(),
            isRead = 0
        )

        notificationDao.insertAll(listOf(notification))
        val retrieved = notificationDao.getForUser("user1")

        assertEquals(1, retrieved.size)
        assertEquals("", retrieved[0].title)
        assertEquals("", retrieved[0].body)
    }

    // ─── DATA INTEGRITY ───

    @Test
    fun `insertAll should maintain data consistency`() = runTest {
        val notifications = listOf(
            NotificationEntity(
                id = "notif1",
                userId = "user1",
                title = "Title 1",
                body = "Body 1",
                type = "message",
                timestamp = System.currentTimeMillis(),
                isRead = 0
            ),
            NotificationEntity(
                id = "notif2",
                userId = "user2",
                title = "Title 2",
                body = "Body 2",
                type = "team_invite",
                timestamp = System.currentTimeMillis(),
                isRead = 1
            )
        )

        notificationDao.insertAll(notifications)
        val user1Notifications = notificationDao.getForUser("user1")
        val user2Notifications = notificationDao.getForUser("user2")

        notifications.forEach { original ->
            val retrieved = if (original.userId == "user1") {
                user1Notifications.find { it.id == original.id }
            } else {
                user2Notifications.find { it.id == original.id }
            }
            
            assertNotNull(retrieved)
            assertEquals(original.userId, retrieved.userId)
            assertEquals(original.title, retrieved.title)
            assertEquals(original.body, retrieved.body)
            assertEquals(original.type, retrieved.type)
            assertEquals(original.timestamp, retrieved.timestamp)
            assertEquals(original.isRead, retrieved.isRead)
        }
    }

    // ─── PERFORMANCE SCENARIOS ───

    @Test
    fun `getForUser should perform efficiently with large dataset`() = runTest {
        val notifications = (1..1000).map { i ->
            NotificationEntity(
                id = "notif$i",
                userId = "user${i % 10}",
                title = "Title $i",
                body = "Body $i",
                type = "type${i % 5}",
                timestamp = System.currentTimeMillis() - (i * 1000),
                isRead = if (i % 2 == 0) 1 else 0
            )
        }

        notificationDao.insertAll(notifications)

        val startTime = System.currentTimeMillis()
        val retrieved = notificationDao.getForUser("user1")
        val endTime = System.currentTimeMillis()

        assertEquals(100, retrieved.size)
        assertTrue(endTime - startTime < 1000, "getForUser should complete in under 1 second for 100 notifications")
    }

    @Test
    fun `markAllAsRead should perform efficiently with large dataset`() = runTest {
        val notifications = (1..1000).map { i ->
            NotificationEntity(
                id = "notif$i",
                userId = "user${i % 10}",
                title = "Title $i",
                body = "Body $i",
                type = "type${i % 5}",
                timestamp = System.currentTimeMillis() - (i * 1000),
                isRead = 0
            )
        }

        notificationDao.insertAll(notifications)

        val startTime = System.currentTimeMillis()
        notificationDao.markAllAsRead("user1")
        val endTime = System.currentTimeMillis()

        assertTrue(endTime - startTime < 1000, "markAllAsRead should complete in under 1 second for 100 notifications")
    }

    // ─── STATE MANAGEMENT ───

    @Test
    fun `deleteById should handle non-existent notification`() = runTest {
        val notification = NotificationEntity(
            id = "notif1",
            userId = "user1",
            title = "New Message",
            body = "You have a new message",
            type = "message",
            timestamp = System.currentTimeMillis(),
            isRead = 0
        )

        notificationDao.insert(notification)
        notificationDao.deleteById("non_existent_notif")

        val retrieved = notificationDao.getForUser("user1")
        assertEquals(1, retrieved.size)
    }

    @Test
    fun `deleteAllForUser should handle user with no notifications`() = runTest {
        notificationDao.deleteAllForUser("user1")
        val retrieved = notificationDao.getForUser("user1")

        assertEquals(0, retrieved.size)
    }

    @Test
    fun `insert after delete should work correctly`() = runTest {
        val notification1 = NotificationEntity(
            id = "notif1",
            userId = "user1",
            title = "Notification 1",
            body = "Body 1",
            type = "message",
            timestamp = System.currentTimeMillis(),
            isRead = 0
        )

        notificationDao.insert(notification1)
        notificationDao.deleteById("notif1")

        val notification2 = NotificationEntity(
            id = "notif2",
            userId = "user1",
            title = "Notification 2",
            body = "Body 2",
            type = "team_invite",
            timestamp = System.currentTimeMillis(),
            isRead = 0
        )

        notificationDao.insert(notification2)
        val retrieved = notificationDao.getForUser("user1")

        assertEquals(1, retrieved.size)
        assertEquals("notif2", retrieved[0].id)
    }
}

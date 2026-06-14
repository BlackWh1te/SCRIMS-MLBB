package com.mlbb.scrim.data.repository

import com.mlbb.scrim.data.model.Notification
import com.mlbb.scrim.data.model.NotificationType
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
class NotificationRepositoryTest {

    private lateinit var notificationRepository: NotificationRepository
    private lateinit var testDispatcher: TestDispatcher

    @Before
    fun setup() {
        notificationRepository = NotificationRepository()
        testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ─── Get Notifications Tests ───

    @Test
    fun `getNotifications returns all notifications sorted by timestamp descending`() {
        // Act
        val result = notificationRepository.getNotifications().first()

        advanceUntilIdle()

        // Assert
        assertTrue(result.isSuccess)
        val notifications = result.getOrNull()!!
        assertTrue(notifications.isNotEmpty())
        
        // Check sorted by timestamp descending
        for (i in 0 until notifications.size - 1) {
            assertTrue(notifications[i].timestamp >= notifications[i + 1].timestamp)
        }
    }

    @Test
    fun `getNotifications returns notifications with correct data`() {
        // Act
        val result = notificationRepository.getNotifications().first()

        advanceUntilIdle()

        // Assert
        assertTrue(result.isSuccess)
        val notifications = result.getOrNull()!!
        
        // Check first notification (should be most recent)
        assertTrue(notifications.first().timestamp > notifications.last().timestamp)
    }

    @Test
    fun `getNotifications includes all notification types`() {
        // Act
        val result = notificationRepository.getNotifications().first()

        advanceUntilIdle()

        // Assert
        assertTrue(result.isSuccess)
        val notifications = result.getOrNull()!!
        
        val types = notifications.map { it.type }.distinct()
        assertTrue(types.contains(NotificationType.SCRIM_INVITE))
        assertTrue(types.contains(NotificationType.MATCH_RESULT))
        assertTrue(types.contains(NotificationType.XP_GAIN))
        assertTrue(types.contains(NotificationType.TEAM_INVITE))
        assertTrue(types.contains(NotificationType.MESSAGE))
        assertTrue(types.contains(NotificationType.SYSTEM))
    }

    // ─── Mark As Read Tests ───

    @Test
    fun `markAsRead marks specific notification as read`() {
        // Arrange
        val notifications = notificationRepository.getNotifications().first().getOrNull()!!
        val unreadNotification = notifications.find { !it.isRead }
        assertNotNull(unreadNotification)
        val notificationId = unreadNotification!!.id

        // Act
        val result = notificationRepository.markAsRead(notificationId).first()

        advanceUntilIdle()

        // Assert
        assertTrue(result.isSuccess)
        val updatedNotifications = notificationRepository.getNotifications().first().getOrNull()!!
        val updatedNotification = updatedNotifications.find { it.id == notificationId }
        assertTrue(updatedNotification?.isRead == true)
    }

    @Test
    fun `markAsRead handles non-existent notification ID gracefully`() {
        // Act
        val result = notificationRepository.markAsRead("nonexistent_id").first()

        advanceUntilIdle()

        // Assert
        assertTrue(result.isSuccess) // Should succeed even if notification doesn't exist
    }

    @Test
    fun `markAsRead does not affect other notifications`() {
        // Arrange
        val notifications = notificationRepository.getNotifications().first().getOrNull()!!
        val unreadNotifications = notifications.filter { !it.isRead }
        val firstUnreadId = unreadNotifications.first().id
        val initialUnreadCount = unreadNotifications.size

        // Act
        notificationRepository.markAsRead(firstUnreadId).first()

        advanceUntilIdle()

        // Assert
        val updatedNotifications = notificationRepository.getNotifications().first().getOrNull()!!
        val updatedUnreadCount = updatedNotifications.count { !it.isRead }
        assertEquals(initialUnreadCount - 1, updatedUnreadCount)
    }

    // ─── Mark All As Read Tests ───

    @Test
    fun `markAllAsRead marks all notifications as read`() {
        // Arrange
        val notifications = notificationRepository.getNotifications().first().getOrNull()!!
        val initialUnreadCount = notifications.count { !it.isRead }
        assertTrue(initialUnreadCount > 0) // Ensure there are unread notifications

        // Act
        val result = notificationRepository.markAllAsRead().first()

        advanceUntilIdle()

        // Assert
        assertTrue(result.isSuccess)
        val updatedNotifications = notificationRepository.getNotifications().first().getOrNull()!!
        val finalUnreadCount = updatedNotifications.count { !it.isRead }
        assertEquals(0, finalUnreadCount)
    }

    @Test
    fun `markAllAsRead handles empty notification list gracefully`() {
        // This test would require a repository with no initial notifications
        // For now, we just verify it doesn't crash
        val result = notificationRepository.markAllAsRead().first()

        advanceUntilIdle()

        // Assert
        assertTrue(result.isSuccess)
    }

    // ─── Delete Notification Tests ───

    @Test
    fun `deleteNotification removes specific notification`() {
        // Arrange
        val notifications = notificationRepository.getNotifications().first().getOrNull()!!
        val notificationToDelete = notifications.first()
        val initialCount = notifications.size
        val notificationId = notificationToDelete.id

        // Act
        val result = notificationRepository.deleteNotification(notificationId).first()

        advanceUntilIdle()

        // Assert
        assertTrue(result.isSuccess)
        val updatedNotifications = notificationRepository.getNotifications().first().getOrNull()!!
        assertEquals(initialCount - 1, updatedNotifications.size)
        assertFalse(updatedNotifications.any { it.id == notificationId })
    }

    @Test
    fun `deleteNotification handles non-existent notification ID gracefully`() {
        // Arrange
        val initialCount = notificationRepository.getNotifications().first().getOrNull()!!.size

        // Act
        val result = notificationRepository.deleteNotification("nonexistent_id").first()

        advanceUntilIdle()

        // Assert
        assertTrue(result.isSuccess)
        val finalCount = notificationRepository.getNotifications().first().getOrNull()!!.size
        assertEquals(initialCount, finalCount) // Count should remain the same
    }

    @Test
    fun `deleteNotification can remove all notifications one by one`() {
        // Arrange
        val notifications = notificationRepository.getNotifications().first().getOrNull()!!
        val notificationIds = notifications.map { it.id }

        // Act
        notificationIds.forEach { id ->
            notificationRepository.deleteNotification(id).first()
            advanceUntilIdle()
        }

        // Assert
        val finalNotifications = notificationRepository.getNotifications().first().getOrNull()!!
        assertTrue(finalNotifications.isEmpty())
    }

    // ─── Get Unread Count Tests ───

    @Test
    fun `getUnreadCount returns correct number of unread notifications`() {
        // Act
        val unreadCount = notificationRepository.getUnreadCount()

        // Assert
        val notifications = notificationRepository.getNotifications().first().getOrNull()!!
        val expectedCount = notifications.count { !it.isRead }
        assertEquals(expectedCount, unreadCount)
    }

    @Test
    fun `getUnreadCount updates after marking notification as read`() {
        // Arrange
        val initialUnreadCount = notificationRepository.getUnreadCount()
        val notifications = notificationRepository.getNotifications().first().getOrNull()!!
        val unreadNotification = notifications.find { !it.isRead }
        assertNotNull(unreadNotification)

        // Act
        notificationRepository.markAsRead(unreadNotification!!.id).first()
        advanceUntilIdle()

        // Assert
        val finalUnreadCount = notificationRepository.getUnreadCount()
        assertEquals(initialUnreadCount - 1, finalUnreadCount)
    }

    @Test
    fun `getUnreadCount updates after marking all as read`() {
        // Arrange
        notificationRepository.markAllAsRead().first()
        advanceUntilIdle()

        // Act
        val unreadCount = notificationRepository.getUnreadCount()

        // Assert
        assertEquals(0, unreadCount)
    }

    @Test
    fun `getUnreadCount updates after deleting notification`() {
        // Arrange
        val initialUnreadCount = notificationRepository.getUnreadCount()
        val notifications = notificationRepository.getNotifications().first().getOrNull()!!
        val unreadNotification = notifications.find { !it.isRead }
        assertNotNull(unreadNotification)

        // Act
        notificationRepository.deleteNotification(unreadNotification!!.id).first()
        advanceUntilIdle()

        // Assert
        val finalUnreadCount = notificationRepository.getUnreadCount()
        assertEquals(initialUnreadCount - 1, finalUnreadCount)
    }

    // ─── Integration Tests ───

    @Test
    fun `full notification lifecycle works correctly`() {
        // Arrange
        val notifications = notificationRepository.getNotifications().first().getOrNull()!!
        val initialCount = notifications.size
        val initialUnreadCount = notificationRepository.getUnreadCount()
        
        // Find an unread notification
        val unreadNotification = notifications.find { !it.isRead }
        assertNotNull(unreadNotification)
        val notificationId = unreadNotification!!.id

        // Act - Mark as read
        notificationRepository.markAsRead(notificationId).first()
        advanceUntilIdle()

        // Verify marked as read
        val afterMarkUnread = notificationRepository.getUnreadCount()
        assertEquals(initialUnreadCount - 1, afterMarkUnread)

        // Delete the notification
        notificationRepository.deleteNotification(notificationId).first()
        advanceUntilIdle()

        // Verify deleted
        val finalNotifications = notificationRepository.getNotifications().first().getOrNull()!!
        assertEquals(initialCount - 1, finalNotifications.size)
        assertFalse(finalNotifications.any { it.id == notificationId })
    }

    @Test
    fun `markAllAsRead followed by delete works correctly`() {
        // Arrange
        val initialCount = notificationRepository.getNotifications().first().getOrNull()!!.size

        // Act - Mark all as read
        notificationRepository.markAllAsRead().first()
        advanceUntilIdle()

        // Verify all read
        assertEquals(0, notificationRepository.getUnreadCount())

        // Delete a notification
        val notifications = notificationRepository.getNotifications().first().getOrNull()!!
        val notificationToDelete = notifications.first()
        notificationRepository.deleteNotification(notificationToDelete.id).first()
        advanceUntilIdle()

        // Verify deleted
        val finalNotifications = notificationRepository.getNotifications().first().getOrNull()!!
        assertEquals(initialCount - 1, finalNotifications.size)
        assertEquals(0, notificationRepository.getUnreadCount()) // Still 0 unread
    }

    // ─── Edge Case Tests ───

    @Test
    fun `markAsRead can be called multiple times on same notification`() {
        // Arrange
        val notifications = notificationRepository.getNotifications().first().getOrNull()!!
        val unreadNotification = notifications.find { !it.isRead }
        assertNotNull(unreadNotification)
        val notificationId = unreadNotification!!.id

        // Act - Mark as read twice
        notificationRepository.markAsRead(notificationId).first()
        advanceUntilIdle()
        
        notificationRepository.markAsRead(notificationId).first()
        advanceUntilIdle()

        // Assert
        val updatedNotifications = notificationRepository.getNotifications().first().getOrNull()!!
        val updatedNotification = updatedNotifications.find { it.id == notificationId }
        assertTrue(updatedNotification?.isRead == true)
    }

    @Test
    fun `deleteNotification can be called multiple times on same notification`() {
        // Arrange
        val notifications = notificationRepository.getNotifications().first().getOrNull()!!
        val notificationToDelete = notifications.first()
        val notificationId = notificationToDelete.id

        // Act - Delete twice
        notificationRepository.deleteNotification(notificationId).first()
        advanceUntilIdle()
        
        notificationRepository.deleteNotification(notificationId).first()
        advanceUntilIdle()

        // Assert
        val finalNotifications = notificationRepository.getNotifications().first().getOrNull()!!
        assertFalse(finalNotifications.any { it.id == notificationId })
    }

    @Test
    fun `getUnreadCount handles all read notifications`() {
        // Arrange
        notificationRepository.markAllAsRead().first()
        advanceUntilIdle()

        // Act
        val unreadCount = notificationRepository.getUnreadCount()

        // Assert
        assertEquals(0, unreadCount)
    }

    @Test
    fun `getNotifications maintains sorting after operations`() {
        // Arrange
        notificationRepository.markAsRead("n1").first()
        advanceUntilIdle()
        
        notificationRepository.deleteNotification("n2").first()
        advanceUntilIdle()

        // Act
        val result = notificationRepository.getNotifications().first()

        advanceUntilIdle()

        // Assert
        assertTrue(result.isSuccess)
        val notifications = result.getOrNull()!!
        
        // Check still sorted by timestamp descending
        for (i in 0 until notifications.size - 1) {
            assertTrue(notifications[i].timestamp >= notifications[i + 1].timestamp)
        }
    }

    // ─── Data Integrity Tests ───

    @Test
    fun `notifications have valid types`() {
        // Act
        val result = notificationRepository.getNotifications().first()

        advanceUntilIdle()

        // Assert
        assertTrue(result.isSuccess)
        val notifications = result.getOrNull()!!
        
        val validTypes = NotificationType.values().toSet()
        notifications.forEach { notification ->
            assertTrue(notification.type in validTypes)
        }
    }

    @Test
    fun `notifications have non-empty IDs`() {
        // Act
        val result = notificationRepository.getNotifications().first()

        advanceUntilIdle()

        // Assert
        assertTrue(result.isSuccess)
        val notifications = result.getOrNull()!!
        
        notifications.forEach { notification ->
            assertTrue(notification.id.isNotEmpty())
        }
    }

    @Test
    fun `notifications have valid timestamps`() {
        // Act
        val result = notificationRepository.getNotifications().first()

        advanceUntilIdle()

        // Assert
        assertTrue(result.isSuccess)
        val notifications = result.getOrNull()!!
        
        notifications.forEach { notification ->
            assertTrue(notification.timestamp > 0)
        }
    }

    @Test
    fun `notifications have non-empty titles`() {
        // Act
        val result = notificationRepository.getNotifications().first()

        advanceUntilIdle()

        // Assert
        assertTrue(result.isSuccess)
        val notifications = result.getOrNull()!!
        
        notifications.forEach { notification ->
            assertTrue(notification.title.isNotEmpty())
        }
    }

    @Test
    fun `notifications have non-empty messages`() {
        // Act
        val result = notificationRepository.getNotifications().first()

        advanceUntilIdle()

        // Assert
        assertTrue(result.isSuccess)
        val notifications = result.getOrNull()!!
        
        notifications.forEach { notification ->
            assertTrue(notification.message.isNotEmpty())
        }
    }
}

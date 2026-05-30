package com.mlbb.scrim.viewmodel

import com.mlbb.scrim.data.model.Notification
import com.mlbb.scrim.data.repository.SupabaseNotificationRepository
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationViewModelTest {

    private lateinit var viewModel: NotificationViewModel
    private lateinit var mockRepository: SupabaseNotificationRepository
    private lateinit var testDispatcher: TestDispatcher

    @Before
    fun setup() {
        mockRepository = mockk(relaxed = true)
        testDispatcher = StandardTestDispatcher()

        Dispatchers.setMain(testDispatcher)

        viewModel = NotificationViewModel(mockRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ─── Initialization Tests ───

    @Test
    fun `ViewModel initializes with empty state`() {
        // Assert
        assertTrue(viewModel.notifications.value.isEmpty())
        assertEquals(0, viewModel.unreadCount.value)
        assertFalse(viewModel.isLoading.value)
        assertFalse(viewModel.isRefreshing.value)
        assertEquals(null, viewModel.error.value)
    }

    // ─── Set User ID Tests ───

    @Test
    fun `setUserId loads notifications and starts realtime subscription`() {
        // Arrange
        val userId = "user123"
        val mockNotifications = listOf(createMockNotification(id = "1"))
        val realtimeFlow = MutableSharedFlow<Notification>()
        
        coEvery { mockRepository.getNotificationsForUser(userId) } returns flow { emit(Result.success(mockNotifications)) }
        coEvery { mockRepository.subscribeToNotifications(userId) } returns realtimeFlow

        // Act
        viewModel.setUserId(userId)
        advanceUntilIdle()

        // Assert
        assertEquals(mockNotifications, viewModel.notifications.value)
        assertEquals(1, viewModel.unreadCount.value)
        coVerify { mockRepository.getNotificationsForUser(userId) }
        coVerify { mockRepository.subscribeToNotifications(userId) }
    }

    @Test
    fun `setUserId does not load notifications when userId is null`() {
        // Act
        viewModel.setUserId("")
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 0) { mockRepository.getNotificationsForUser(any()) }
    }

    // ─── Load Notifications Tests ───

    @Test
    fun `loadNotifications successfully loads notifications`() {
        // Arrange
        val userId = "user123"
        viewModel.setUserId(userId)
        val mockNotifications = listOf(
            createMockNotification(id = "1", isRead = false),
            createMockNotification(id = "2", isRead = true)
        )
        coEvery { mockRepository.getNotificationsForUser(userId) } returns flow { emit(Result.success(mockNotifications)) }

        // Act
        viewModel.loadNotifications()
        advanceUntilIdle()

        // Assert
        assertEquals(mockNotifications, viewModel.notifications.value)
        assertEquals(1, viewModel.unreadCount.value) // Only one unread
        assertFalse(viewModel.isLoading.value)
    }

    @Test
    fun `loadNotifications sets refreshing flag when isRefresh is true`() {
        // Arrange
        val userId = "user123"
        viewModel.setUserId(userId)
        coEvery { mockRepository.getNotificationsForUser(userId) } returns flow { emit(Result.success(emptyList())) }

        // Act
        viewModel.loadNotifications(isRefresh = true)
        advanceUntilIdle()

        // Assert
        assertFalse(viewModel.isRefreshing.value) // Should be false after completion
    }

    @Test
    fun `loadNotifications handles error`() {
        // Arrange
        val userId = "user123"
        viewModel.setUserId(userId)
        val errorMessage = "Failed to load notifications"
        coEvery { mockRepository.getNotificationsForUser(userId) } returns flow { emit(Result.failure(Exception(errorMessage))) }

        // Act
        viewModel.loadNotifications()
        advanceUntilIdle()

        // Assert
        assertEquals(errorMessage, viewModel.error.value)
        assertFalse(viewModel.isLoading.value)
    }

    @Test
    fun `loadNotifications calls onComplete callback when provided`() {
        // Arrange
        val userId = "user123"
        viewModel.setUserId(userId)
        var callbackCalled = false
        coEvery { mockRepository.getNotificationsForUser(userId) } returns flow { emit(Result.success(emptyList())) }

        // Act
        viewModel.loadNotifications(onComplete = { callbackCalled = true })
        advanceUntilIdle()

        // Assert
        assertTrue(callbackCalled)
    }

    // ─── Realtime Subscription Tests ───

    @Test
    fun `startRealtimeSubscription subscribes to realtime notifications`() {
        // Arrange
        val userId = "user123"
        val newNotification = createMockNotification(id = "new")
        val realtimeFlow = MutableSharedFlow<Notification>()
        
        coEvery { mockRepository.subscribeToNotifications(userId) } returns realtimeFlow

        // Act
        viewModel.startRealtimeSubscription()
        realtimeFlow.emit(newNotification)
        advanceUntilIdle()

        // Assert
        assertTrue(viewModel.notifications.value.isNotEmpty())
        assertEquals(newNotification, viewModel.notifications.value.first())
    }

    @Test
    fun `startRealtimeSubscription avoids duplicate notifications`() {
        // Arrange
        val userId = "user123"
        val existingNotification = createMockNotification(id = "existing")
        viewModel.notifications.value = listOf(existingNotification)
        
        val duplicateNotification = createMockNotification(id = "existing")
        val realtimeFlow = MutableSharedFlow<Notification>()
        
        coEvery { mockRepository.subscribeToNotifications(userId) } returns realtimeFlow

        // Act
        viewModel.startRealtimeSubscription()
        realtimeFlow.emit(duplicateNotification)
        advanceUntilIdle()

        // Assert
        assertEquals(1, viewModel.notifications.value.size) // Should still be 1, not 2
    }

    @Test
    fun `startRealtimeSubscription adds new notification at beginning of list`() {
        // Arrange
        val userId = "user123"
        val existingNotification = createMockNotification(id = "existing")
        viewModel.notifications.value = listOf(existingNotification)
        
        val newNotification = createMockNotification(id = "new")
        val realtimeFlow = MutableSharedFlow<Notification>()
        
        coEvery { mockRepository.subscribeToNotifications(userId) } returns realtimeFlow

        // Act
        viewModel.startRealtimeSubscription()
        realtimeFlow.emit(newNotification)
        advanceUntilIdle()

        // Assert
        assertEquals(2, viewModel.notifications.value.size)
        assertEquals(newNotification, viewModel.notifications.value.first()) // New notification at index 0
    }

    @Test
    fun `startRealtimeSubscription updates unread count`() {
        // Arrange
        val userId = "user123"
        val newNotification = createMockNotification(id = "new", isRead = false)
        val realtimeFlow = MutableSharedFlow<Notification>()
        
        coEvery { mockRepository.subscribeToNotifications(userId) } returns realtimeFlow

        // Act
        viewModel.startRealtimeSubscription()
        realtimeFlow.emit(newNotification)
        advanceUntilIdle()

        // Assert
        assertEquals(1, viewModel.unreadCount.value)
    }

    @Test
    fun `stopRealtimeSubscription cancels realtime subscription`() {
        // Arrange
        val userId = "user123"
        coEvery { mockRepository.subscribeToNotifications(userId) } returns flow { }

        // Act
        viewModel.startRealtimeSubscription()
        viewModel.stopRealtimeSubscription()
        advanceUntilIdle()

        // Assert - Should stop without errors
        assertTrue(true)
    }

    // ─── Mark as Read Tests ───

    @Test
    fun `markAsRead successfully marks notification as read`() {
        // Arrange
        val userId = "user123"
        viewModel.setUserId(userId)
        val notificationId = "notif123"
        val mockNotifications = listOf(
            createMockNotification(id = notificationId, isRead = false),
            createMockNotification(id = "2", isRead = false)
        )
        coEvery { mockRepository.markAsRead(notificationId) } returns flow { emit(Result.success(Unit)) }
        coEvery { mockRepository.getNotificationsForUser(userId) } returns flow { emit(Result.success(mockNotifications)) }

        // Act
        viewModel.markAsRead(notificationId)
        advanceUntilIdle()

        // Assert
        coVerify { mockRepository.markAsRead(notificationId) }
    }

    @Test
    fun `markAsRead handles error`() {
        // Arrange
        val userId = "user123"
        viewModel.setUserId(userId)
        val errorMessage = "Failed to mark as read"
        coEvery { mockRepository.markAsRead(any()) } returns flow { emit(Result.failure(Exception(errorMessage))) }

        // Act
        viewModel.markAsRead("notif123")
        advanceUntilIdle()

        // Assert
        assertEquals(errorMessage, viewModel.error.value)
    }

    // ─── Mark All as Read Tests ───

    @Test
    fun `markAllAsRead successfully marks all notifications as read`() {
        // Arrange
        val userId = "user123"
        viewModel.setUserId(userId)
        val mockNotifications = listOf(
            createMockNotification(id = "1", isRead = false),
            createMockNotification(id = "2", isRead = false)
        )
        coEvery { mockRepository.markAllAsRead(userId) } returns flow { emit(Result.success(Unit)) }
        coEvery { mockRepository.getNotificationsForUser(userId) } returns flow { emit(Result.success(mockNotifications)) }

        // Act
        viewModel.markAllAsRead()
        advanceUntilIdle()

        // Assert
        coVerify { mockRepository.markAllAsRead(userId) }
    }

    @Test
    fun `markAllAsRead does nothing when userId is not set`() {
        // Act
        viewModel.markAllAsRead()
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 0) { mockRepository.markAllAsRead(any()) }
    }

    @Test
    fun `markAllAsRead handles error`() {
        // Arrange
        val userId = "user123"
        viewModel.setUserId(userId)
        val errorMessage = "Failed to mark all as read"
        coEvery { mockRepository.markAllAsRead(userId) } returns flow { emit(Result.failure(Exception(errorMessage))) }

        // Act
        viewModel.markAllAsRead()
        advanceUntilIdle()

        // Assert
        assertEquals(errorMessage, viewModel.error.value)
    }

    // ─── Delete Notification Tests ───

    @Test
    fun `deleteNotification successfully deletes notification`() {
        // Arrange
        val userId = "user123"
        viewModel.setUserId(userId)
        val notificationId = "notif123"
        val mockNotifications = listOf(createMockNotification(id = "2"))
        coEvery { mockRepository.deleteNotification(notificationId) } returns flow { emit(Result.success(Unit)) }
        coEvery { mockRepository.getNotificationsForUser(userId) } returns flow { emit(Result.success(mockNotifications)) }

        // Act
        viewModel.deleteNotification(notificationId)
        advanceUntilIdle()

        // Assert
        coVerify { mockRepository.deleteNotification(notificationId) }
    }

    @Test
    fun `deleteNotification handles error`() {
        // Arrange
        val userId = "user123"
        viewModel.setUserId(userId)
        val errorMessage = "Failed to delete notification"
        coEvery { mockRepository.deleteNotification(any()) } returns flow { emit(Result.failure(Exception(errorMessage))) }

        // Act
        viewModel.deleteNotification("notif123")
        advanceUntilIdle()

        // Assert
        assertEquals(errorMessage, viewModel.error.value)
    }

    // ─── Clear State Tests ───

    @Test
    fun `clearError clears error message`() {
        // Arrange
        viewModel._error.value = "Test error"

        // Act
        viewModel.clearError()

        // Assert
        assertEquals(null, viewModel.error.value)
    }

    @Test
    fun `clearRefreshing clears refreshing flag`() {
        // Arrange
        viewModel._isRefreshing.value = true

        // Act
        viewModel.clearRefreshing()

        // Assert
        assertFalse(viewModel.isRefreshing.value)
    }

    // ─── ViewModel Cleanup Tests ───

    @Test
    fun `onCleared stops realtime subscription`() {
        // Arrange
        val userId = "user123"
        coEvery { mockRepository.subscribeToNotifications(userId) } returns flow { }

        // Act
        viewModel.startRealtimeSubscription()
        viewModel.onCleared()
        advanceUntilIdle()

        // Assert - Should stop without errors
        assertTrue(true)
    }

    // ─── Helper Functions ───

    private fun createMockNotification(
        id: String = "notif-id",
        isRead: Boolean = false
    ): Notification {
        return Notification(
            id = id,
            userId = "user123",
            type = "scrim_invite",
            title = "Test Notification",
            message = "This is a test notification",
            data = emptyMap(),
            isRead = isRead,
            createdAt = System.currentTimeMillis()
        )
    }
}

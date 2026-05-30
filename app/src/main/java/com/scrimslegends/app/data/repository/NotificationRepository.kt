package com.scrimslegends.app.data.repository

import com.scrimslegends.app.data.model.Notification
import com.scrimslegends.app.data.model.NotificationType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class NotificationRepository {

    private val notifications = mutableListOf(
        Notification(
            id = "n1",
            type = NotificationType.SCRIM_INVITE,
            title = "New Scrim Invite",
            message = "Phoenix Rising wants to scrim your team at 8PM",
            timestamp = System.currentTimeMillis() - 3600000,
            isRead = false,
            actionId = "scrim1"
        ),
        Notification(
            id = "n2",
            type = NotificationType.MATCH_RESULT,
            title = "Match Result Confirmed",
            message = "Your match vs Shadow Wolves has been confirmed. Winner: Elite Squad!",
            timestamp = System.currentTimeMillis() - 7200000,
            isRead = false,
            actionId = "match1"
        ),
        Notification(
            id = "n3",
            type = NotificationType.XP_GAIN,
            title = "XP Gained!",
            message = "You earned +150 XP for winning the scrim",
            timestamp = System.currentTimeMillis() - 10800000,
            isRead = true,
            actionId = ""
        ),
        Notification(
            id = "n4",
            type = NotificationType.TEAM_INVITE,
            title = "Team Invitation",
            message = "You received an invite to join Nova Gaming",
            timestamp = System.currentTimeMillis() - 18000000,
            isRead = true,
            actionId = "team5"
        ),
        Notification(
            id = "n5",
            type = NotificationType.MESSAGE,
            title = "New Message",
            message = "PhoenixLeader: We are ready for the match at 8PM!",
            timestamp = System.currentTimeMillis() - 3600000,
            isRead = false,
            actionId = "conv1"
        ),
        Notification(
            id = "n6",
            type = NotificationType.SYSTEM,
            title = "Welcome to Scrims Legends",
            message = "Complete your profile and create a team to get started!",
            timestamp = System.currentTimeMillis() - 86400000,
            isRead = true,
            actionId = ""
        )
    )

    suspend fun getNotifications(): Flow<Result<List<Notification>>> = flow {
        kotlinx.coroutines.delay(300)
        emit(Result.success(notifications.sortedByDescending { it.timestamp }))
    }

    suspend fun markAsRead(notificationId: String): Flow<Result<Unit>> = flow {
        kotlinx.coroutines.delay(200)
        val index = notifications.indexOfFirst { it.id == notificationId }
        if (index != -1) {
            notifications[index] = notifications[index].copy(isRead = true)
        }
        emit(Result.success(Unit))
    }

    suspend fun markAllAsRead(): Flow<Result<Unit>> = flow {
        kotlinx.coroutines.delay(300)
        notifications.forEachIndexed { index, notification ->
            notifications[index] = notification.copy(isRead = true)
        }
        emit(Result.success(Unit))
    }

    suspend fun deleteNotification(notificationId: String): Flow<Result<Unit>> = flow {
        kotlinx.coroutines.delay(200)
        notifications.removeIf { it.id == notificationId }
        emit(Result.success(Unit))
    }

    fun getUnreadCount(): Int = notifications.count { !it.isRead }
}

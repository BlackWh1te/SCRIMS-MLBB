package com.scrimslegends.app.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object NotificationChannelManager {

    const val CHANNEL_MESSAGES = "messages_channel"
    const val CHANNEL_TEAM_CHAT = "team_chat_channel"
    const val CHANNEL_SCRIM_CHAT = "scrim_chat_channel"
    const val CHANNEL_SYSTEM = "system_channel"

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val messagesChannel = NotificationChannel(
                CHANNEL_MESSAGES,
                "Direct Messages",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for direct messages"
                enableVibration(true)
            }

            val teamChatChannel = NotificationChannel(
                CHANNEL_TEAM_CHAT,
                "Team Chat",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for team chat"
                enableVibration(true)
            }

            val scrimChatChannel = NotificationChannel(
                CHANNEL_SCRIM_CHAT,
                "Scrim Chat",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for scrim chat"
                enableVibration(true)
            }

            val systemChannel = NotificationChannel(
                CHANNEL_SYSTEM,
                "System Events",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "System notifications and alerts"
            }

            notificationManager.createNotificationChannels(
                listOf(messagesChannel, teamChatChannel, scrimChatChannel, systemChannel)
            )
        }
    }
}

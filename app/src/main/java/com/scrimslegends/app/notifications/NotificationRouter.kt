package com.scrimslegends.app.notifications

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.scrimslegends.app.MainActivity
import com.scrimslegends.app.R
import timber.log.Timber
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRouter @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun route(data: Map<String, String>) {
        val type = data["type"] ?: "system_event"
        val title = data["title"] ?: "Scrims Legends"
        val body = data["body"] ?: ""

        val channelId = when (type) {
            "message_received" -> NotificationChannelManager.CHANNEL_MESSAGES
            "team_message" -> NotificationChannelManager.CHANNEL_TEAM_CHAT
            "scrim_message" -> NotificationChannelManager.CHANNEL_SCRIM_CHAT
            "system_event" -> NotificationChannelManager.CHANNEL_SYSTEM
            else -> NotificationChannelManager.CHANNEL_SYSTEM
        }

        val notificationId = System.currentTimeMillis().toInt()

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            // Add extras based on type to navigate to correct screen
            data.forEach { (key, value) -> putExtra(key, value) }
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, channelId)
            // Using a default icon. Make sure to replace with your actual notification icon resource.
            .setSmallIcon(R.drawable.ic_launcher_foreground) 
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Timber.w("POST_NOTIFICATIONS permission not granted. Dropping notification.")
                return
            }
        }

        try {
            with(NotificationManagerCompat.from(context)) {
                notify(notificationId, builder.build())
            }
        } catch (e: SecurityException) {
            Timber.e(e, "SecurityException when trying to post notification")
        }
    }
}

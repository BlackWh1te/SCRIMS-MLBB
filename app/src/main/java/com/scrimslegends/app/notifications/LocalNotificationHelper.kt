package com.scrimslegends.app.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.scrimslegends.app.R
import com.scrimslegends.app.data.model.Notification
import com.scrimslegends.app.data.model.NotificationType

/**
 * Manages Android system (local) notifications.
 *
 * Responsibilities:
 *  - Create the required [NotificationChannel] once on app startup.
 *  - Post a heads-up notification when a new in-app notification arrives while
 *    the app is in the foreground (and permission has been granted on API 33+).
 *
 * Background push (when app is NOT running) requires FCM and is not yet
 * implemented. This class handles the "app is open" use-case only.
 *
 * Call [createChannels] from [com.scrimslegends.app.ScrimsLegendsApplication.onCreate].
 * Call [show] from [com.scrimslegends.app.viewmodel.NotificationViewModel] whenever a
 * new notification arrives via Realtime and is not suppressed by settings.
 */
object LocalNotificationHelper {

    // ── Channel definitions ───────────────────────────────────────────────────

    /** Channel for scrim, team, match, tournament and system alerts. */
    const val CHANNEL_ALERTS  = "scrims_legends_alerts"

    /** Channel for direct messages (separate so users can silence it alone). */
    const val CHANNEL_MESSAGES = "scrims_legends_messages"

    // ── Channel bootstrap ─────────────────────────────────────────────────────

    /**
     * Create notification channels. Safe to call multiple times — Android
     * deduplicates on channel ID.  Must be called before any notification is
     * posted (Android O requirement).
     */
    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ALERTS,
                "Scrim & Tournament Alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Scrim applications, match results, team invites, and tournament updates"
                enableVibration(true)
                setShowBadge(true)
            }
        )

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_MESSAGES,
                "Messages",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Direct messages from other team leaders"
                enableVibration(true)
                setShowBadge(true)
            }
        )
    }

    // ── Post notification ─────────────────────────────────────────────────────

    /**
     * Post a local Android notification for [notification].
     *
     * Silently returns (no-op) if:
     * - [POST_NOTIFICATIONS] permission is not granted (Android 13+)
     * - Notifications are disabled in Android Settings for the app
     *
     * @param soundEnabled     when false, notification is posted silently
     * @param vibrationEnabled when false, vibration pattern is suppressed
     */
    fun show(
        context: Context,
        notification: Notification,
        soundEnabled: Boolean = true,
        vibrationEnabled: Boolean = true
    ) {
        // Runtime permission check for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val channelId = if (notification.type.isMessageChannel()) CHANNEL_MESSAGES else CHANNEL_ALERTS

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.logo)
            .setContentTitle(notification.title)
            .setContentText(notification.message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(notification.message))
            .setPriority(
                if (notification.type.isMessageChannel())
                    NotificationCompat.PRIORITY_HIGH
                else
                    NotificationCompat.PRIORITY_DEFAULT
            )
            .setAutoCancel(true)

        if (!soundEnabled) builder.setSilent(true)
        if (!vibrationEnabled) builder.setVibrate(longArrayOf(0L))

        // Use a stable int ID derived from notification UUID so repeated
        // delivery of the same notification replaces rather than stacks.
        val notifId = notification.id.hashCode()

        NotificationManagerCompat.from(context).notify(notifId, builder.build())
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Returns true for types that should post to the high-priority Messages channel. */
    private fun NotificationType.isMessageChannel(): Boolean =
        this == NotificationType.MESSAGE
}

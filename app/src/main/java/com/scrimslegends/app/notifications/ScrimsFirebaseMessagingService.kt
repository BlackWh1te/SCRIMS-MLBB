package com.scrimslegends.app.notifications

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.scrimslegends.app.data.repository.FcmTokenRepositoryInterface
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class ScrimsFirebaseMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var fcmTokenRepository: FcmTokenRepositoryInterface

    @Inject
    lateinit var notificationRouter: NotificationRouter

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Timber.d("FCM Token refreshed: \$token")
        serviceScope.launch {
            // Attempt to register token. 
            // Note: If user is not logged in, Supabase RPC will fail which is expected.
            // When user logs in, we should also manually register the token from the UI layer.
            fcmTokenRepository.registerToken(token)
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Timber.d("FCM Message received from: \${remoteMessage.from}")

        // 1. Data payload
        if (remoteMessage.data.isNotEmpty()) {
            Timber.d("FCM Message data payload: \${remoteMessage.data}")
            notificationRouter.route(remoteMessage.data)
        }

        // 2. Notification payload (if sent by server)
        // Usually we want data-only messages for custom routing.
        remoteMessage.notification?.let {
            Timber.d("FCM Message Notification Body: \${it.body}")
            // If data payload wasn't routed, you could fallback here.
            // But typically, we rely on data payload for custom handling.
        }
    }
}

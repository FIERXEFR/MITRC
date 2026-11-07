package com.mitrc.ac.`in`.notifications

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.mitrc.ac.`in`.R
import com.mitrc.ac.`in`.SplashActivity
import com.mitrc.ac.`in`.data.PushRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Receives data-only FCM messages from the `send-push` Edge Function and renders
 * them locally. Building the notification here (instead of letting FCM auto-show
 * a `notification` payload) keeps one code path and gives the tap a precise
 * target: the notifications sheet.
 */
class MitrcMessagingService : FirebaseMessagingService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        // If nobody is signed in the token is registered after the next login by
        // HomeScreen. Trying here would create a row with no owner.
        if (FirebaseAuth.getInstance().currentUser == null) return
        scope.launch {
            PushRepository.registerCurrentDevice(
                PushRepository.appVersion(this@MitrcMessagingService)
            )
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        val notificationId = data["notification_id"]?.toLongOrNull()?.toInt()
            ?: (System.currentTimeMillis() and 0x7FFFFFFFL).toInt()

        val title = data["title"]?.takeIf { it.isNotBlank() }
            ?: message.notification?.title
            ?: "MITRC"
        val body = data["body"]?.takeIf { it.isNotBlank() }
            ?: message.notification?.body
            ?: "You have a new college notice."
        val priority = data["priority"]

        NotificationChannels.ensure(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            Log.i(TAG, "Skipping notification $notificationId: POST_NOTIFICATIONS not granted")
            return
        }

        val manager = NotificationManagerCompat.from(this)
        if (!manager.areNotificationsEnabled()) {
            Log.i(TAG, "Skipping notification $notificationId: notifications disabled")
            return
        }

        val intent = Intent(this, SplashActivity::class.java).apply {
            putExtra(SplashActivity.EXTRA_OPEN_NOTIFICATIONS, true)
            putExtra(SplashActivity.EXTRA_NOTIFICATION_ID, notificationId)
            // Skip the 2.6s branding animation; SplashActivity still resolves the
            // correct destination (admin / login / onboarding / portal).
            putExtra(SplashActivity.EXTRA_SKIP_SPLASH, true)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val channel = NotificationChannels.channelFor(priority)
        val notification = NotificationCompat.Builder(this, channel)
            .setSmallIcon(R.drawable.ic_stat_notify)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(
                if (channel == NotificationChannels.ALERTS) {
                    NotificationCompat.PRIORITY_HIGH
                } else {
                    NotificationCompat.PRIORITY_DEFAULT
                }
            )
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .build()

        runCatching { manager.notify(notificationId, notification) }
            .onFailure { Log.w(TAG, "Could not post notification $notificationId", it) }
    }

    companion object {
        private const val TAG = "MitrcMessagingService"
    }
}

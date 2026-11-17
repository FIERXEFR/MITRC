package com.mitrc.ac.`in`.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentResolver
import android.content.Context
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import com.mitrc.ac.`in`.R

object NotificationChannels {

    const val ALERTS = "mitrc_alerts"
    const val NOTICES = "mitrc_notices"

    fun ensure(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return

        val sound = notificationSound(context)

        manager.createNotificationChannel(
            NotificationChannel(ALERTS, "Urgent notices", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Timetable changes, cancellations and other time-sensitive alerts."
                enableVibration(true)
                setSound(sound, notificationAudioAttributes())
            }
        )
        manager.createNotificationChannel(
            NotificationChannel(NOTICES, "Notices", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "College notices and announcements."
                setSound(sound, notificationAudioAttributes())
            }
        )
    }

    fun channelFor(priority: String?): String =
        if (priority.equals("high", ignoreCase = true) ||
            priority.equals("urgent", ignoreCase = true)
        ) {
            ALERTS
        } else {
            NOTICES
        }

    private fun notificationSound(context: Context): Uri = Uri.parse(
        "${ContentResolver.SCHEME_ANDROID_RESOURCE}://${context.packageName}/${R.raw.startnotifi}"
    )

    private fun notificationAudioAttributes(): AudioAttributes =
        AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
}

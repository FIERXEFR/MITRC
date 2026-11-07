package com.mitrc.ac.`in`.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

/**
 * Two Android channels so urgent notices can vibrate and heads-up while routine
 * announcements stay quiet. Created once from [com.mitrc.ac.in.MITRCApplication].
 */
object NotificationChannels {

    const val ALERTS = "mitrc_alerts"
    const val NOTICES = "mitrc_notices"

    fun ensure(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return

        manager.createNotificationChannel(
            NotificationChannel(ALERTS, "Urgent notices", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Timetable changes, cancellations and other time-sensitive alerts."
                enableVibration(true)
            }
        )
        manager.createNotificationChannel(
            NotificationChannel(NOTICES, "Notices", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "College notices and announcements."
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
}

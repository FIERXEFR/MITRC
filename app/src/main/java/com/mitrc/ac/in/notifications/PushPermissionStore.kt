package com.mitrc.ac.`in`.notifications

import android.content.Context

/**
 * Remembers whether the post-login notification prompt has been shown, so the
 * app asks once instead of nagging on every launch.
 */
object PushPermissionStore {

    private const val PREFS = "mitrc_push"
    private const val KEY_PROMPTED = "permission_prompted"

    fun wasPrompted(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_PROMPTED, false)

    fun markPrompted(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_PROMPTED, true)
            .apply()
    }
}

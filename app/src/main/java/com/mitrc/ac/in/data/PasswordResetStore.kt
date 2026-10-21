package com.mitrc.ac.`in`.data

import android.content.Context
import android.content.SharedPreferences

/**
 * Throttles Firebase password-reset emails to one per hour, per device, per email address.
 *
 * The timestamp is persisted so clearing the app from the recents list (or a process death)
 * does not hand the user a fresh allowance. A link that was never requested has no entry.
 */
object PasswordResetStore {

    private const val FILE_NAME = "mitrc_prefs"
    private const val KEY_PREFIX = "password_reset_at_"
    private const val COOLDOWN_MILLIS = 60L * 60L * 1000L

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        if (::prefs.isInitialized) return
        synchronized(this) {
            if (::prefs.isInitialized) return
            prefs = context.applicationContext
                .getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
        }
    }

    /** Milliseconds left before another link may be sent to [email] (0 when allowed). */
    fun remainingMillis(email: String): Long {
        if (!::prefs.isInitialized) return 0L
        val elapsed = System.currentTimeMillis() - lastSentAt(email)
        return (COOLDOWN_MILLIS - elapsed).coerceIn(0L, COOLDOWN_MILLIS)
    }

    /** Records a successful send so the cooldown starts now. */
    fun markSent(email: String) {
        if (!::prefs.isInitialized) return
        prefs.edit().putLong(key(email), System.currentTimeMillis()).apply()
    }

    private fun lastSentAt(email: String): Long =
        if (::prefs.isInitialized) prefs.getLong(key(email), 0L) else 0L

    private fun key(email: String) = KEY_PREFIX + email.trim().lowercase()
}

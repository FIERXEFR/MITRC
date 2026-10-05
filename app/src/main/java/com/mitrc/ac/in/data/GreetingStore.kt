package com.mitrc.ac.`in`.data

import android.content.Context
import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Decides which greeting the portal header shows.
 *
 * The very first time the app is opened on a given calendar day the student gets
 * **"Welcome"**; every later open the same day gets **"HEY"**. The date is persisted so the
 * greeting survives process death and app restarts, and the time of day is never consulted -
 * the old morning/afternoon/night wording is gone.
 */
object GreetingStore {

    private const val FILE_NAME = "mitrc_prefs"
    private const val KEY_LAST_GREETING_DAY = "last_greeting_day"

    private lateinit var prefs: SharedPreferences
    private val dayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    fun init(context: Context) {
        if (::prefs.isInitialized) return
        synchronized(this) {
            if (::prefs.isInitialized) return
            prefs = context.applicationContext
                .getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
        }
    }

    /**
     * Returns "Welcome" the first call of the day (and records the day), "HEY" afterwards.
     *
     * Falls back to "HEY" if [init] has not run yet, which keeps the header usable even if a
     * preview or test renders it in isolation.
     */
    fun greeting(): String {
        val today = dayFormat.format(Date())
        if (!::prefs.isInitialized) return "HEY"

        val last = prefs.getString(KEY_LAST_GREETING_DAY, null)
        if (last == today) return "HEY"

        prefs.edit().putString(KEY_LAST_GREETING_DAY, today).apply()
        return "Welcome"
    }
}

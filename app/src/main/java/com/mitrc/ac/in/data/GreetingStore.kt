package com.mitrc.ac.`in`.data

import android.content.Context
import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

    fun greeting(): String {
        val today = dayFormat.format(Date())
        if (!::prefs.isInitialized) return "Hi !"

        val last = prefs.getString(KEY_LAST_GREETING_DAY, null)
        if (last == today) return "Hi !"

        prefs.edit().putString(KEY_LAST_GREETING_DAY, today).apply()
        return "Welcome"
    }
}

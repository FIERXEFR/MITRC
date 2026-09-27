package com.mitrc.ac.`in`.data

import android.content.Context
import android.content.SharedPreferences

/**
 * Remembers whether the user has ever completed a login.
 *
 * Onboarding keeps showing on **every** app launch while this is false, so a student who only
 * taps Skip still gets the introduction next time. The first successful sign-in sets it, after
 * which onboarding is never shown again (even if the user signs out later).
 */
object OnboardingStore {

    private const val FILE_NAME = "mitrc_prefs"
    private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        if (::prefs.isInitialized) return
        synchronized(this) {
            if (::prefs.isInitialized) return
            prefs = context.applicationContext
                .getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
        }
    }

    val isCompleted: Boolean
        get() = ::prefs.isInitialized && prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)

    fun markCompleted() {
        if (!::prefs.isInitialized) return
        prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, true).apply()
    }
}

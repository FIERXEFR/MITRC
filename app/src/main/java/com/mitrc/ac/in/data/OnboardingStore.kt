package com.mitrc.ac.`in`.data

import android.content.Context
import android.content.SharedPreferences

/**
 * Remembers whether the first-run onboarding flow has already been seen,
 * so it only ever shows once (until the app data is cleared).
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

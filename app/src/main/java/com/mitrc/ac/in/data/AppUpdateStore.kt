package com.mitrc.ac.`in`.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull

/**
 * Local state for the update/maintenance gate.
 *
 * The big packet is the APK itself. The small packet is a server-driven payload: the app stores
 * the last small version it accepted and the JSON payload that came with it. Nothing here can
 * replace compiled code; it exists so copy, feature flags, links and other wired-in behavior can
 * change without shipping a new APK.
 */
object AppUpdateStore {

    private const val FILE_NAME = "mitrc_prefs"
    private const val KEY_SMALL_VERSION = "app_small_version"
    private const val KEY_SMALL_PAYLOAD = "app_small_payload"
    private const val KEY_DISMISSED_BIG_VERSION = "app_dismissed_big_version"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        if (::prefs.isInitialized) return
        synchronized(this) {
            if (::prefs.isInitialized) return
            prefs = context.applicationContext
                .getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
        }
    }

    val smallVersion: String
        get() = if (::prefs.isInitialized) prefs.getString(KEY_SMALL_VERSION, "").orEmpty() else ""

    val smallPayload: String
        get() = if (::prefs.isInitialized) prefs.getString(KEY_SMALL_PAYLOAD, "").orEmpty() else ""

    val dismissedBigVersion: String
        get() = if (::prefs.isInitialized) prefs.getString(KEY_DISMISSED_BIG_VERSION, "").orEmpty() else ""

    fun markSmallSeen(version: String, payload: JsonObject) {
        if (!::prefs.isInitialized) return
        prefs.edit()
            .putString(KEY_SMALL_VERSION, version)
            .putString(KEY_SMALL_PAYLOAD, payload.toString())
            .apply()
    }

    fun markBigDismissed(version: String) {
        if (!::prefs.isInitialized) return
        prefs.edit().putString(KEY_DISMISSED_BIG_VERSION, version).apply()
    }
}

/** Convenience readers for values inside the accepted small-packet JSON payload. */
object AppRemoteConfig {

    private val json = Json { ignoreUnknownKeys = true }

    private fun payload(): JsonObject? = runCatching {
        if (AppUpdateStore.smallPayload.isBlank()) return null
        json.parseToJsonElement(AppUpdateStore.smallPayload) as? JsonObject
    }.getOrNull()

    fun string(key: String, fallback: String? = null): String? {
        val value = payload()?.get(key) as? JsonPrimitive ?: return fallback
        return value.content.takeIf { it.isNotBlank() } ?: fallback
    }

    fun boolean(key: String, fallback: Boolean = false): Boolean {
        val value = payload()?.get(key) as? JsonPrimitive ?: return fallback
        return value.booleanOrNull ?: fallback
    }
}

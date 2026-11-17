package com.mitrc.ac.`in`.data

import android.content.Context
import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

object PushRepository {

    private const val TAG = "PushRepository"

    fun appVersion(context: Context): String? = runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName
    }.getOrNull()

    suspend fun currentToken(): String? = runCatching {
        FirebaseMessaging.getInstance().token.await()
    }.onFailure {
        if (it is CancellationException) throw it
        Log.w(TAG, "FCM token unavailable: ${it.message}")
    }.getOrNull()

    suspend fun registerCurrentDevice(appVersion: String?): Result<Unit> = runCatching {
        val token = currentToken() ?: error("Firebase did not return an FCM token")
        val params = buildJsonObject {
            put("p_token", token)
            put("p_platform", "android")
            put("p_app_version", appVersion)
        }
        SupabaseManager.requireClient().postgrest.rpc("register_device_token", params)
        Unit
    }.onFailure {
        if (it is CancellationException) throw it
        Log.w(TAG, "Device token registration failed: ${it.message}")
    }

    suspend fun unregisterCurrentDevice(): Result<Unit> = runCatching {
        val token = currentToken() ?: return@runCatching
        val params = buildJsonObject { put("p_token", token) }
        SupabaseManager.requireClient().postgrest.rpc("unregister_device_token", params)
        Unit
    }.onFailure {
        if (it is CancellationException) throw it
        Log.w(TAG, "Device token unregister failed: ${it.message}")
    }
}

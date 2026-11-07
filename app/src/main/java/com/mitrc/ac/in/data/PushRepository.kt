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

/**
 * Registers this installation's FCM token with the `device_tokens` table through
 * the two SECURITY DEFINER RPCs. The table itself has no client policies, so a
 * signed-in user can only ever touch their own token.
 */
object PushRepository {

    private const val TAG = "PushRepository"

    fun appVersion(context: Context): String? = runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName
    }.getOrNull()

    suspend fun currentToken(): String? = runCatching {
        FirebaseMessaging.getInstance().token.await()
    }.onFailure {
        // runCatching also catches CancellationException; rethrow it so callers
        // (e.g. withTimeoutOrNull in signOut) can cancel cleanly.
        if (it is CancellationException) throw it
        Log.w(TAG, "FCM token unavailable: ${it.message}")
    }.getOrNull()

    /** Best-effort: a failed registration never blocks the portal. */
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

    /**
     * Called while Firebase still has a user, because the RPC resolves the owner
     * from the request JWT. After [com.mitrc.ac.in.auth.AuthRepository] signs out
     * the token would no longer match and the row would stay behind.
     */
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

package com.mitrc.ac.`in`.data

import android.content.Context
import android.util.Log
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order

/**
 * Reads and writes the single-row remote control document in `public.app_update`.
 *
 * Reads are public so the gate can run before login. Writes are protected by the
 * `app_update_admin_write` RLS policy and only succeed for an account in `admin_db`.
 */
object AppUpdateRepository {

    private const val TAG = "AppUpdateRepository"

    suspend fun fetchLatest(): Result<AppUpdateRow?> = runCatching {
        SupabaseManager.withAuthRetry {
            SupabaseManager.requireClient().postgrest[SupabaseTableData.Tables.APP_UPDATE]
                .select { order("updated_at", Order.DESCENDING) }
                .decodeList<AppUpdateRow>()
                .firstOrNull()
        }
    }.onFailure {
        Log.w(TAG, "Could not load app_update: ${it.message}")
    }

    suspend fun save(row: AppUpdateRow): Result<Unit> = runCatching {
        SupabaseManager.withAuthRetry {
            SupabaseManager.requireClient().postgrest[SupabaseTableData.Tables.APP_UPDATE]
                .upsert(row)
        }
        Unit
    }.onFailure {
        Log.w(TAG, "Could not save app_update: ${it.message}")
    }

    fun currentVersionName(context: Context): String = runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName
    }.getOrNull().orEmpty().ifBlank { "0" }

    /**
     * Compares dotted versions numerically, so `1.10` is newer than `1.9`. Text that is not
     * numeric (for example `1.0-beta`) still works because the non-digit separators are ignored.
     */
    fun isNewer(remote: String?, local: String?): Boolean {
        val remoteText = remote?.trim().orEmpty().removePrefix("v")
        val localText = local?.trim().orEmpty().removePrefix("v")
        if (remoteText.isBlank()) return false
        if (localText.isBlank()) return true

        val remoteParts = versionParts(remoteText)
        val localParts = versionParts(localText)
        if (remoteParts.isEmpty() || localParts.isEmpty()) {
            return remoteText > localText
        }

        val size = maxOf(remoteParts.size, localParts.size)
        for (index in 0 until size) {
            val remotePart = remoteParts.getOrElse(index) { 0 }
            val localPart = localParts.getOrElse(index) { 0 }
            if (remotePart != localPart) return remotePart > localPart
        }
        return false
    }

    private fun versionParts(value: String): List<Long> =
        Regex("\\d+").findAll(value).mapNotNull { it.value.toLongOrNull() }.toList()
}

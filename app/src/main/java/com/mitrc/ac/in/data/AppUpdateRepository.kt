package com.mitrc.ac.`in`.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Snapshot of the two-row remote control document in `public.app_update`.
 *
 * A null field means the row does not exist on the server yet; callers must treat that as
 * "feature off" instead of guessing defaults.
 */
data class AppUpdateConfig(
    val update: AppUpdateRow? = null,
    val maintenance: AppUpdateRow? = null,
)

/**
 * Reads and writes the two-row remote control document in `public.app_update`.
 *
 * Reads are public so the gate can run before login. Writes are protected by the
 * `app_update_admin_write` RLS policy and only succeed for an account in `admin_db`.
 *
 * The rows are addressed by [CONFIG_UPDATE] / [CONFIG_MAINTENANCE] and pinned to
 * [UPDATE_ROW_ID] / [MAINTENANCE_ROW_ID] by the `app_update_config_id_check` constraint, so the
 * admin screen can never create a third row or swap the two.
 */
object AppUpdateRepository {

    private const val TAG = "AppUpdateRepository"

    const val CONFIG_UPDATE = "app_update"
    const val CONFIG_MAINTENANCE = "app_maintenance"

    const val UPDATE_ROW_ID = 1L
    const val MAINTENANCE_ROW_ID = 2L

    /** Loads both rows in one round trip and separates them by `config_key`. */
    suspend fun fetchConfig(): Result<AppUpdateConfig> = runCatching {
        SupabaseManager.withAuthRetry {
            val rows = SupabaseManager.requireClient().postgrest[SupabaseTableData.Tables.APP_UPDATE]
                .select()
                .decodeList<AppUpdateRow>()

            AppUpdateConfig(
                update = rows.firstOrNull { it.configKey == CONFIG_UPDATE }
                    ?: rows.firstOrNull { it.id == UPDATE_ROW_ID && it.configKey != CONFIG_MAINTENANCE },
                maintenance = rows.firstOrNull { it.configKey == CONFIG_MAINTENANCE }
                    ?: rows.firstOrNull { it.id == MAINTENANCE_ROW_ID && it.configKey != CONFIG_UPDATE },
            )
        }
    }.onFailure {
        // runCatching also catches CancellationException; rethrow it so callers
        // are not left with a silently swallowed cancellation.
        if (it is CancellationException) throw it
        Log.w(TAG, "Could not load app_update: ${it.message}")
    }

    /** Convenience for callers that only care about the release row (e.g. "Check for update"). */
    suspend fun fetchLatest(): Result<AppUpdateRow?> = fetchConfig().map { it.update }

    /** Saves only the release row (id 1). Maintenance columns are never touched. */
    suspend fun saveUpdate(row: AppUpdateRow): Result<Unit> {
        // Encode the write explicitly instead of relying on the model's default-value
        // elision. With `encodeDefaults = false`, a value equal to its Kotlin default
        // is dropped from the body, and PostgREST's upsert then leaves the old column
        // untouched. `updated_at` and `updated_by` are set by the
        // `app_update_before_write` trigger, so they are omitted here.
        val payload = buildJsonObject {
            put("id", UPDATE_ROW_ID)
            put("config_key", CONFIG_UPDATE)
            put("is_bigpacket", row.isBigPacket)
            put("small_version", row.smallVersion)
            put("big_version", row.bigVersion)
            put("is_force", row.isForce)
            put("download_url", row.downloadUrl)
            put("app_status", row.appStatus)
            put("dialog_title", row.dialogTitle)
            put("dialog_body", row.dialogBody)
            put("whats_new", row.whatsNew)
            put("small_payload", row.smallPayload)
        }
        return write(payload, "release row")
    }

    /** Saves only the maintenance row (id 2). Release columns are never touched. */
    suspend fun saveMaintenance(row: AppUpdateRow): Result<Unit> {
        val payload = buildJsonObject {
            put("id", MAINTENANCE_ROW_ID)
            put("config_key", CONFIG_MAINTENANCE)
            put("is_maintenance", row.isMaintenance)
            put("maintenance_title", row.maintenanceTitle)
            put("maintenance_message", row.maintenanceMessage)
            put("maintenance_until", row.maintenanceUntil)
        }
        return write(payload, "maintenance row")
    }

    private suspend fun write(payload: JsonObject, label: String): Result<Unit> = runCatching {
        SupabaseManager.withAuthRetry {
            SupabaseManager.requireClient().postgrest[SupabaseTableData.Tables.APP_UPDATE]
                .upsert(payload)
        }
        Unit
    }.onFailure {
        if (it is CancellationException) throw it
        Log.w(TAG, "Could not save $label: ${it.message}")
    }

    fun currentVersionName(context: Context): String = runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName
    }.getOrNull().orEmpty().ifBlank { "0" }

    /**
     * Opens the admin-configured download page in the device browser. The app deliberately does
     * not download or install the APK itself; the page handles the download and Android install.
     */
    fun openDownloadPage(context: Context, url: String?): Boolean {
        val target = url?.trim().orEmpty()
        if (target.isEmpty()) return false
        val normalized = if (
            target.startsWith("https://", ignoreCase = true) ||
            target.startsWith("http://", ignoreCase = true)
        ) {
            target
        } else {
            "https://$target"
        }
        return runCatching {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(normalized))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
            true
        }.getOrDefault(false)
    }

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

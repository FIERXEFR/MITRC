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

data class AppUpdateConfig(
    val update: AppUpdateRow? = null,
    val maintenance: AppUpdateRow? = null,
)

object AppUpdateRepository {

    private const val TAG = "AppUpdateRepository"

    const val CONFIG_UPDATE = "app_update"
    const val CONFIG_MAINTENANCE = "app_maintenance"

    const val UPDATE_ROW_ID = 1L
    const val MAINTENANCE_ROW_ID = 2L

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
        if (it is CancellationException) throw it
        Log.w(TAG, "Could not load app_update: ${it.message}")
    }

    suspend fun fetchLatest(): Result<AppUpdateRow?> = fetchConfig().map { it.update }

    suspend fun saveUpdate(row: AppUpdateRow): Result<Unit> {
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

package com.mitrc.ac.`in`.auth

import android.content.Context
import android.util.Log
import com.mitrc.ac.`in`.R
import com.mitrc.ac.`in`.data.AdminDbRow
import com.mitrc.ac.`in`.data.BranchRow
import com.mitrc.ac.`in`.data.ClassGroupRow
import com.mitrc.ac.`in`.data.ClassRow
import com.mitrc.ac.`in`.data.CourseRow
import com.mitrc.ac.`in`.data.EnrollmentRow
import com.mitrc.ac.`in`.data.FacultyMasterRow
import com.mitrc.ac.`in`.data.StudentRow
import com.mitrc.ac.`in`.data.SupabaseManager
import com.mitrc.ac.`in`.data.SupabaseTableData
import com.mitrc.ac.`in`.data.blankToNull
import io.github.jan.supabase.postgrest.postgrest
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * The admin panel's data-entry gate & system management repository.
 * Refactored for robust RLS handling and verbose debugging.
 */
object AdminRepository {

    private const val TAG = "AdminRepository"
    private const val SIGN_UP_URL = "https://identitytoolkit.googleapis.com/v1/accounts:signUp"
    private const val DELETE_URL = "https://identitytoolkit.googleapis.com/v1/accounts:delete"

    private val json = Json { ignoreUnknownKeys = true }
    @Volatile private var apiKey: String = ""

    private val http: HttpClient by lazy {
        HttpClient(OkHttp) {
            expectSuccess = false
            install(HttpTimeout) {
                requestTimeoutMillis = 20_000
                connectTimeoutMillis = 10_000
            }
        }
    }

    fun init(context: Context) {
        apiKey = runCatching { context.applicationContext.getString(R.string.google_api_key) }.getOrDefault("")
    }

    suspend fun resolveStaffGate(uid: String?, email: String?): StaffGate {
        val id = uid?.trim().orEmpty()
        val mail = email?.trim().orEmpty().lowercase()
        Log.d(TAG, "Resolving staff gate for UID: $id, Email: $mail")

        return try {
            SupabaseManager.withAuthRetry {
                resolveStaffGateOnce(id, mail)
            }
        } catch (error: Throwable) {
            Log.e(TAG, "Connection or RLS Failure. Check Supabase JWT Settings.", error)
            StaffGate.Failed("${error::class.simpleName}: ${error.message}")
        }
    }

    private suspend fun resolveStaffGateOnce(id: String, mail: String): StaffGate {
        val client = SupabaseManager.requireClient()

        // 1. Check Admin Table
        val adminRows = try {
            client.postgrest[SupabaseTableData.Tables.ADMIN_DB]
                .select {
                    filter {
                        or {
                            if (id.isNotEmpty()) eq("uid", id)
                            if (mail.isNotEmpty()) ilike("email", mail)
                        }
                    }
                }
                .decodeList<AdminDbRow>()
        } catch (e: Exception) {
            Log.w(TAG, "Admin table check skipped/failed: ${e.message}")
            emptyList()
        }

        val matchingAdmin = adminRows.firstOrNull()
        if (matchingAdmin != null) {
            Log.i(TAG, "Staff resolution: SUCCESS (Matched in admin_db)")
            if (id.isNotEmpty() && !matchingAdmin.uid.equals(id, ignoreCase = true)) {
                runCatching {
                    client.postgrest[SupabaseTableData.Tables.ADMIN_DB]
                        .update({ set("uid", id) }) { filter { eq("email", matchingAdmin.email ?: mail) } }
                }
            }
            return StaffGate.Admin
        }

        // 2. Check Faculty Master
        Log.d(TAG, "Probing faculty_master for staff record...")
        val facultyRows = try {
            client.postgrest[SupabaseTableData.Tables.FACULTY_MASTER]
                .select {
                    filter {
                        or {
                            if (id.isNotEmpty()) eq("firebase_uid", id)
                            if (mail.isNotEmpty()) ilike("email", mail)
                        }
                    }
                }
                .decodeList<FacultyMasterRow>()
        } catch (e: Exception) {
            Log.e(TAG, "CRITICAL: faculty_master query failed. Is Supabase JWT Issuer set to Firebase?", e)
            throw e 
        }

        Log.d(TAG, "faculty_master query returned ${facultyRows.size} matches")
        val matchingStaff = facultyRows.firstOrNull()

        if (matchingStaff != null) {
            Log.i(TAG, "Staff resolution: SUCCESS (Faculty: ${matchingStaff.name})")
            if (id.isNotEmpty() && matchingStaff.firebaseUid.isNullOrEmpty()) {
                Log.i(TAG, "Linking missing Firebase UID $id to email $mail")
                runCatching {
                    client.postgrest[SupabaseTableData.Tables.FACULTY_MASTER]
                        .update({ set("firebase_uid", id) }) { filter { ilike("email", matchingStaff.email ?: mail) } }
                }
            }
            return StaffGate.Staff
        }

        Log.w(TAG, "Staff resolution: FAILED (No matching record for Email: $mail)")
        return StaffGate.Neither
    }

    sealed interface StaffGate {
        data object Admin : StaffGate
        data object Staff : StaffGate
        data object Neither : StaffGate
        data class Failed(val reason: String) : StaffGate
    }

    // --- Creation Helpers ---
    suspend fun createStaffAccount(entry: StaffEntry): Result<String> =
        createAccountAndRow(entry.email, entry.password, entry.name, SupabaseTableData.Tables.FACULTY_MASTER) { uid ->
            FacultyMasterRow(firebaseUid = uid, email = entry.email.blankToNull(), name = entry.name.blankToNull(), role = entry.role.blankToNull())
        }

    private suspend fun createAccountAndRow(email: String, password: String, name: String, table: String, buildRow: (String) -> Any): Result<String> {
        if (!SupabaseManager.isConfigured) return Result.failure(IllegalStateException("Supabase not configured"))
        val created = createAuthAccount(email, password, name).getOrElse { return Result.failure(it) }
        val inserted = runCatching { SupabaseManager.requireClient().postgrest[table].insert(buildRow(created.uid)) }
        if (inserted.isFailure) {
            deleteAuthAccount(created.idToken)
            return Result.failure(IllegalStateException("DB Failure: ${inserted.exceptionOrNull()?.message}"))
        }
        return Result.success(created.uid)
    }

    suspend fun getCourses(): List<CourseRow> = runCatching { SupabaseManager.requireClient().postgrest[SupabaseTableData.Tables.COURSES].select().decodeList<CourseRow>() }.getOrDefault(emptyList())
    suspend fun getBranches(cid: Int): List<BranchRow> = runCatching { SupabaseManager.requireClient().postgrest[SupabaseTableData.Tables.BRANCHES].select { filter { eq("course_id", cid) } }.decodeList<BranchRow>() }.getOrDefault(emptyList())
    suspend fun getClasses(): List<ClassRow> = runCatching { SupabaseManager.requireClient().postgrest[SupabaseTableData.Tables.CLASSES].select().decodeList<ClassRow>() }.getOrDefault(emptyList())
    suspend fun getClassGroups(cid: Int): List<ClassGroupRow> = runCatching { SupabaseManager.requireClient().postgrest[SupabaseTableData.Tables.CLASS_GROUPS].select { filter { eq("class_id", cid) } }.decodeList<ClassGroupRow>() }.getOrDefault(emptyList())

    private data class CreatedAccount(val uid: String, val idToken: String)
    private suspend fun createAuthAccount(e: String, p: String, n: String): Result<CreatedAccount> {
        if (apiKey.isBlank()) return Result.failure(IllegalStateException("API key missing"))
        return runCatching {
            val resp = http.post("$SIGN_UP_URL?key=$apiKey") {
                contentType(ContentType.Application.Json)
                setBody(json.encodeToString(SignUpRequest(e, p, n)))
            }
            if (!resp.status.isSuccess()) throw IllegalStateException(resp.bodyAsText())
            val parsed = json.decodeFromString<SignUpResponse>(resp.bodyAsText())
            CreatedAccount(parsed.localId, parsed.idToken)
        }
    }
    private suspend fun deleteAuthAccount(t: String) { runCatching { http.post("$DELETE_URL?key=$apiKey") { contentType(ContentType.Application.Json); setBody(json.encodeToString(DeleteRequest(t))) } } }
    @Serializable private data class SignUpRequest(val email: String, val password: String, val displayName: String, val returnSecureToken: Boolean = true)
    @Serializable private data class SignUpResponse(@SerialName("localId") val localId: String, @SerialName("idToken") val idToken: String)
    @Serializable private data class DeleteRequest(@SerialName("idToken") val idToken: String)
    @Serializable private data class FirebaseErrorResponse(val error: FirebaseErrorBody)
    @Serializable private data class FirebaseErrorBody(val message: String)
}

data class StudentEntry(val email: String, val password: String, val name: String, val serialNo: String = "", val classId: Int? = null, val groupId: Int? = null)
data class StaffEntry(val email: String, val password: String, val name: String, val department: String = "", val role: String = "")

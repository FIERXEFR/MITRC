package com.mitrc.ac.`in`.auth

import android.content.Context
import android.util.Log
import com.mitrc.ac.`in`.R
import com.mitrc.ac.`in`.data.AdminDbRow
import com.mitrc.ac.`in`.data.BranchRow
import com.mitrc.ac.`in`.data.ClassGroupRow
import com.mitrc.ac.`in`.data.ClassRow
import com.mitrc.ac.`in`.data.CoordinatorRow
import com.mitrc.ac.`in`.data.CourseRow
import com.mitrc.ac.`in`.data.EnrollmentRow
import com.mitrc.ac.`in`.data.StaffDbRow
import com.mitrc.ac.`in`.data.StudentRow
import com.mitrc.ac.`in`.data.SupabaseManager
import com.mitrc.ac.`in`.data.SupabaseTableData
import com.mitrc.ac.`in`.data.TeacherRow
import com.mitrc.ac.`in`.data.blankToNull
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
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
 */
object AdminRepository {

    private const val TAG = "AdminRepository"

    private const val SIGN_UP_URL =
        "https://identitytoolkit.googleapis.com/v1/accounts:signUp"
    private const val DELETE_URL =
        "https://identitytoolkit.googleapis.com/v1/accounts:delete"

    private val json = Json { ignoreUnknownKeys = true }

    @Volatile
    private var apiKey: String = ""

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
        apiKey = runCatching {
            context.applicationContext.getString(R.string.google_api_key)
        }.getOrDefault("")
    }

    // -----------------------------------------------------------------------------------------
    // Role resolution
    // -----------------------------------------------------------------------------------------

    suspend fun isAdmin(uid: String?, email: String? = null): Boolean =
        resolveStaffGate(uid, email) is StaffGate.Admin

    suspend fun resolveStaffGate(uid: String?, email: String?): StaffGate {
        val id = uid?.trim().orEmpty()
        val mail = email?.trim().orEmpty()
        if (id.isEmpty() && mail.isEmpty()) return StaffGate.Neither

        return try {
            // Wrapped so a token rejected on the first request after sign-in (the exact moment
            // this gate runs) gets one refresh + retry instead of failing the login outright.
            SupabaseManager.withAuthRetry {
                resolveStaffGateOnce(id, mail)
            }
        } catch (error: Throwable) {
            Log.e(TAG, "staff gate lookup failed for uid=$id", error)
            StaffGate.Failed("${error::class.simpleName}: ${error.message}")
        }
    }

    private suspend fun resolveStaffGateOnce(id: String, mail: String): StaffGate {
        val client = SupabaseManager.requireClient()

        // 1. Probe admin_db (Strict non-empty match)
        val adminRows = client.postgrest[SupabaseTableData.Tables.ADMIN_DB]
            .select()
            .decodeList<AdminDbRow>()

        val matchingAdmin = adminRows.firstOrNull { row ->
            val dbUid = row.uid.trim()
            val dbEmail = row.email?.trim().orEmpty()
            (id.isNotEmpty() && dbUid.isNotEmpty() && dbUid.equals(id, ignoreCase = true)) ||
            (mail.isNotEmpty() && dbEmail.isNotEmpty() && dbEmail.equals(mail, ignoreCase = true))
        }

        if (matchingAdmin != null) {
            Log.i(TAG, "resolveStaffGate: Matched in admin_db for email=$mail, uid=$id")
            if (!matchingAdmin.uid.equals(id, ignoreCase = true) && id.isNotEmpty()) {
                runCatching {
                    client.postgrest[SupabaseTableData.Tables.ADMIN_DB]
                        .update({ set("uid", id) }) {
                            filter { eq("email", matchingAdmin.email ?: mail) }
                        }
                }
            }
            return StaffGate.Admin
        }

        // 2. Probe staff_db (Strict non-empty match)
        val staffRows = client.postgrest[SupabaseTableData.Tables.STAFF_DB]
            .select()
            .decodeList<StaffDbRow>()

        val matchingStaff = staffRows.firstOrNull { row ->
            val dbUid = row.uid.trim()
            val dbEmail = row.emailId?.trim().orEmpty()
            (id.isNotEmpty() && dbUid.isNotEmpty() && dbUid.equals(id, ignoreCase = true)) ||
            (mail.isNotEmpty() && dbEmail.isNotEmpty() && dbEmail.equals(mail, ignoreCase = true))
        }

        if (matchingStaff != null) {
            Log.i(TAG, "resolveStaffGate: Matched in staff_db for email=$mail, uid=$id")
            if (!matchingStaff.uid.equals(id, ignoreCase = true) && id.isNotEmpty()) {
                runCatching {
                    client.postgrest[SupabaseTableData.Tables.STAFF_DB]
                        .update({ set("uid", id) }) {
                            filter { eq("email_id", matchingStaff.emailId ?: mail) }
                        }
                }
            }
            return StaffGate.Staff
        }

        Log.w(TAG, "resolveStaffGate: No match in admin_db or staff_db for email='$mail', uid='$id'")
        return StaffGate.Neither
    }

    sealed interface StaffGate {
        data object Admin : StaffGate
        data object Staff : StaffGate
        data object Neither : StaffGate
        data class Failed(val reason: String) : StaffGate
    }

    // -----------------------------------------------------------------------------------------
    // Account creation
    // -----------------------------------------------------------------------------------------

    suspend fun createStudentAccount(entry: StudentEntry): Result<String> {
        val result = createAccountAndRow(
            email = entry.email,
            password = entry.password,
            displayName = entry.name,
            table = SupabaseTableData.Tables.STUDENTS,
        ) { uid ->
            StudentRow(
                uid = uid,
                name = entry.name.blankToNull(),
                serialNo = entry.serialNo.blankToNull(),
                fatherName = entry.fatherName.blankToNull(),
                studentPhoneNo = entry.studentPhoneNo.blankToNull(),
                fatherPhoneNo = entry.fatherPhoneNo.blankToNull(),
                studentEmail = entry.email.blankToNull(),
            )
        }

        val uid = result.getOrNull()
        if (uid != null && entry.classId != null && entry.classId > 0) {
            runCatching {
                SupabaseManager.requireClient().postgrest[SupabaseTableData.Tables.ENROLLMENTS]
                    .insert(
                        EnrollmentRow(
                            studentUid = uid,
                            classId = entry.classId,
                            groupId = entry.groupId
                        )
                    )
            }.onFailure { e ->
                Log.w(TAG, "Account created but enrollment insert failed for $uid", e)
            }
        }

        return result
    }

    suspend fun createStaffAccount(entry: StaffEntry): Result<String> =
        createAccountAndRow(
            email = entry.email,
            password = entry.password,
            displayName = entry.name,
            table = SupabaseTableData.Tables.STAFF_DB,
        ) { uid ->
            StaffDbRow(
                uid = uid,
                emailId = entry.email.blankToNull(),
                department = entry.department.blankToNull(),
                role = entry.role.blankToNull(),
                name = entry.name.blankToNull(),
                phoneNo = entry.phoneNo.blankToNull(),
                gender = entry.gender.blankToNull(),
                designation = entry.designation.blankToNull(),
            )
        }

    private suspend fun createAccountAndRow(
        email: String,
        password: String,
        displayName: String,
        table: String,
        buildRow: (uid: String) -> Any,
    ): Result<String> {
        if (!SupabaseManager.isConfigured) {
            return Result.failure(
                IllegalStateException("Supabase is not configured.")
            )
        }

        val created = createAuthAccount(email, password, displayName)
            .getOrElse { return Result.failure(it) }

        val row = buildRow(created.uid)
        val inserted = runCatching {
            val client = SupabaseManager.requireClient()
            when (row) {
                is StudentRow -> client.postgrest[table].insert(row)
                is StaffDbRow -> client.postgrest[table].insert(row)
                else -> error("Unsupported row type ${row::class.simpleName}")
            }
        }

        inserted.exceptionOrNull()?.let { error ->
            Log.w(TAG, "supabase insert into $table failed, rolling back auth account", error)
            deleteAuthAccount(created.idToken)
            return Result.failure(
                IllegalStateException(
                    "The account was created but the $table record could not be saved: " +
                        error.message.orEmpty()
                )
            )
        }

        return Result.success(created.uid)
    }

    // -----------------------------------------------------------------------------------------
    // Helper queries for academic structure & users
    // -----------------------------------------------------------------------------------------

    private val defaultCourses = listOf(
        CourseRow(id = 1, name = "B.Tech"),
        CourseRow(id = 2, name = "BCA"),
        CourseRow(id = 3, name = "BBA"),
        CourseRow(id = 4, name = "MBA"),
        CourseRow(id = 5, name = "M.Tech")
    )

    suspend fun getCourses(): List<CourseRow> = try {
        val client = SupabaseManager.requireClient()
        val list = client.postgrest[SupabaseTableData.Tables.COURSES]
            .select()
            .decodeList<CourseRow>()
        if (list.isNotEmpty()) list else defaultCourses
    } catch (error: Throwable) {
        Log.e(TAG, "getCourses failed, returning defaults", error)
        defaultCourses
    }

    private fun getDefaultBranches(courseId: Int): List<BranchRow> = when (courseId) {
        1 -> listOf(
            BranchRow(id = 1, courseId = 1, name = "CSE"),
            BranchRow(id = 2, courseId = 1, name = "AI&ML"),
            BranchRow(id = 3, courseId = 1, name = "AI&DS")
        )
        2 -> listOf(
            BranchRow(id = 4, courseId = 2, name = "BCA Computer Applications")
        )
        else -> listOf(
            BranchRow(id = 10 + courseId, courseId = courseId, name = "General")
        )
    }

    suspend fun getBranches(courseId: Int): List<BranchRow> = try {
        val client = SupabaseManager.requireClient()
        val list = client.postgrest[SupabaseTableData.Tables.BRANCHES]
            .select { filter { eq("course_id", courseId) } }
            .decodeList<BranchRow>()
        if (list.isNotEmpty()) list else getDefaultBranches(courseId)
    } catch (error: Throwable) {
        Log.e(TAG, "getBranches failed for courseId=$courseId, returning defaults", error)
        getDefaultBranches(courseId)
    }

    private fun getDefaultClasses(branchId: Int): List<ClassRow> = listOf(
        ClassRow(id = 1, branchId = branchId, semester = 1, section = "A", academicYear = "2026-27"),
        ClassRow(id = 2, branchId = branchId, semester = 1, section = "B", academicYear = "2026-27"),
        ClassRow(id = 3, branchId = branchId, semester = 2, section = "A", academicYear = "2026-27")
    )

    suspend fun getClassesForBranch(branchId: Int): List<ClassRow> = try {
        val client = SupabaseManager.requireClient()
        val list = client.postgrest[SupabaseTableData.Tables.CLASSES]
            .select { filter { eq("branch_id", branchId) } }
            .decodeList<ClassRow>()
        if (list.isNotEmpty()) list else getDefaultClasses(branchId)
    } catch (error: Throwable) {
        Log.e(TAG, "getClassesForBranch failed for branchId=$branchId, returning defaults", error)
        getDefaultClasses(branchId)
    }

    suspend fun getClasses(): List<ClassRow> = try {
        val client = SupabaseManager.requireClient()
        client.postgrest[SupabaseTableData.Tables.CLASSES]
            .select()
            .decodeList<ClassRow>()
    } catch (error: Throwable) {
        Log.e(TAG, "getClasses failed", error)
        emptyList()
    }

    suspend fun getClassGroups(classId: Int): List<ClassGroupRow> = try {
        val client = SupabaseManager.requireClient()
        client.postgrest[SupabaseTableData.Tables.CLASS_GROUPS]
            .select { filter { eq("class_id", classId) } }
            .decodeList<ClassGroupRow>()
    } catch (error: Throwable) {
        Log.e(TAG, "getClassGroups failed for classId=$classId", error)
        emptyList()
    }

    suspend fun getTeachers(): List<TeacherRow> = runCatching {
        SupabaseManager.requireClient().postgrest[SupabaseTableData.Tables.TEACHERS]
            .select()
            .decodeList<TeacherRow>()
    }.getOrDefault(emptyList())

    suspend fun getCoordinators(): List<CoordinatorRow> = runCatching {
        SupabaseManager.requireClient().postgrest[SupabaseTableData.Tables.COORDINATORS]
            .select()
            .decodeList<CoordinatorRow>()
    }.getOrDefault(emptyList())

    suspend fun getTeacherByUid(uid: String): TeacherRow? = runCatching {
        SupabaseManager.requireClient().postgrest[SupabaseTableData.Tables.TEACHERS]
            .select { filter { eq("firebase_uid", uid) } }
            .decodeList<TeacherRow>()
            .firstOrNull()
    }.getOrNull()

    suspend fun getCoordinatorByUid(uid: String): CoordinatorRow? = runCatching {
        SupabaseManager.requireClient().postgrest[SupabaseTableData.Tables.COORDINATORS]
            .select { filter { eq("firebase_uid", uid) } }
            .decodeList<CoordinatorRow>()
            .firstOrNull()
    }.getOrNull()

    // -----------------------------------------------------------------------------------------
    // Firebase Identity Toolkit REST
    // -----------------------------------------------------------------------------------------

    private data class CreatedAccount(val uid: String, val idToken: String)

    private suspend fun createAuthAccount(
        email: String,
        password: String,
        displayName: String,
    ): Result<CreatedAccount> {
        if (apiKey.isBlank()) {
            return Result.failure(
                IllegalStateException("Firebase Web API key is missing from google-services.json")
            )
        }

        val outcome = runCatching {
            val response = http.post("$SIGN_UP_URL?key=$apiKey") {
                contentType(ContentType.Application.Json)
                setBody(
                    json.encodeToString(
                        SignUpRequest(
                            email = email,
                            password = password,
                            displayName = displayName,
                        )
                    )
                )
            }
            val body = response.bodyAsText()
            if (!response.status.isSuccess()) {
                throw IllegalStateException(errorMessageOf(body))
            }
            val parsed = json.decodeFromString<SignUpResponse>(body)
            if (parsed.localId.isBlank()) {
                throw IllegalStateException("Account creation returned no user id")
            }
            CreatedAccount(uid = parsed.localId, idToken = parsed.idToken)
        }

        val account = outcome.getOrNull()
        if (account != null) return Result.success(account)

        val error = outcome.exceptionOrNull() ?: IllegalStateException("Account creation failed")
        return Result.failure(IllegalStateException(friendlyAuthError(error.message), error))
    }

    private suspend fun deleteAuthAccount(idToken: String) {
        if (apiKey.isBlank() || idToken.isBlank()) return
        runCatching {
            http.post("$DELETE_URL?key=$apiKey") {
                contentType(ContentType.Application.Json)
                setBody(json.encodeToString(DeleteRequest(idToken)))
            }
        }.onFailure { Log.w(TAG, "failed to roll back auth account", it) }
    }

    private fun errorMessageOf(body: String): String? = runCatching {
        json.decodeFromString<FirebaseErrorResponse>(body).error.message
    }.getOrNull()

    private fun friendlyAuthError(raw: String?): String {
        val message = raw.orEmpty()
        return when {
            message.isBlank() -> "Account creation failed. Please try again"
            message.contains("EMAIL_EXISTS") -> "An account with this email already exists"
            message.contains("INVALID_EMAIL") -> "Enter a valid email address"
            message.contains("WEAK_PASSWORD") -> "Password must be at least 6 characters"
            message.contains("TOO_MANY_ATTEMPTS") -> "Too many attempts. Please try again later"
            message.contains("OPERATION_NOT_ALLOWED") ->
                "Email/password sign-up is disabled for this Firebase project"
            message.contains("INVALID_LOGIN_CREDENTIALS") -> "Invalid email or password"
            message.contains("network", ignoreCase = true) ||
                message.contains("Unable to resolve host", ignoreCase = true) ->
                "Network error - check your internet connection"
            else -> message
        }
    }

    @Serializable
    private data class SignUpRequest(
        val email: String,
        val password: String,
        val displayName: String = "",
        @SerialName("returnSecureToken") val returnSecureToken: Boolean = true,
    )

    @Serializable
    private data class SignUpResponse(
        @SerialName("localId") val localId: String = "",
        @SerialName("idToken") val idToken: String = "",
    )

    @Serializable
    private data class DeleteRequest(@SerialName("idToken") val idToken: String)

    @Serializable
    private data class FirebaseErrorResponse(val error: FirebaseErrorBody = FirebaseErrorBody())

    @Serializable
    private data class FirebaseErrorBody(val code: Int = 0, val message: String = "")
}

data class StudentEntry(
    val email: String,
    val password: String,
    val name: String,
    val serialNo: String = "",
    val fatherName: String = "",
    val studentPhoneNo: String = "",
    val fatherPhoneNo: String = "",
    val classId: Int? = null,
    val groupId: Int? = null,
)

data class StaffEntry(
    val email: String,
    val password: String,
    val name: String,
    val department: String = "",
    val role: String = "",
    val phoneNo: String = "",
    val gender: String = "",
    val designation: String = "",
)

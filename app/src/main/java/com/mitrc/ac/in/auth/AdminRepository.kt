package com.mitrc.ac.`in`.auth

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.mitrc.ac.`in`.R
import com.mitrc.ac.`in`.data.AdminDbRow
import com.mitrc.ac.`in`.data.BranchRow
import com.mitrc.ac.`in`.data.ClassGroupRow
import com.mitrc.ac.`in`.data.ClassRow
import com.mitrc.ac.`in`.data.CourseRow
import com.mitrc.ac.`in`.data.EnrollmentRow
import com.mitrc.ac.`in`.data.FacultyMasterRow
import com.mitrc.ac.`in`.data.NewNotificationRow
import com.mitrc.ac.`in`.data.NotificationRow
import com.mitrc.ac.`in`.data.PortalRepository
import com.mitrc.ac.`in`.data.StaffDbRow
import com.mitrc.ac.`in`.data.StaffDirectoryView
import com.mitrc.ac.`in`.data.StudentProfileView
import com.mitrc.ac.`in`.data.StudentRow
import com.mitrc.ac.`in`.data.SupabaseManager
import com.mitrc.ac.`in`.data.SupabaseTableData
import com.mitrc.ac.`in`.data.blankToNull
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Count
import io.github.jan.supabase.postgrest.query.Order
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object AdminRepository {

    private const val TAG = "AdminRepository"
    private const val SIGN_UP_URL = "https://identitytoolkit.googleapis.com/v1/accounts:signUp"
    private const val DELETE_URL = "https://identitytoolkit.googleapis.com/v1/accounts:delete"
    private const val ACTIVATION_FUNCTION = "admin-account"

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

    suspend fun isAdmin(uid: String?, email: String?): Boolean {
        val id = uid?.trim().orEmpty()
        val mail = email?.trim().orEmpty().lowercase()
        if (id.isEmpty() && mail.isEmpty()) return false

        return try {
            SupabaseManager.withAuthRetry {
                val client = SupabaseManager.requireClient()
                val rows = client.postgrest[SupabaseTableData.Tables.ADMIN_DB]
                    .select {
                        filter {
                            if (id.isNotEmpty() && mail.isNotEmpty()) {
                                or {
                                    eq("uid", id)
                                    ilike("email", mail)
                                }
                            } else if (id.isNotEmpty()) {
                                eq("uid", id)
                            } else {
                                ilike("email", mail)
                            }
                        }
                    }
                    .decodeList<AdminDbRow>()

                val match = rows.firstOrNull() ?: return@withAuthRetry false
                if (id.isNotEmpty() && !match.uid.equals(id, ignoreCase = true)) {
                    runCatching {
                        client.postgrest[SupabaseTableData.Tables.ADMIN_DB]
                            .update({ set("uid", id) }) {
                                filter { ilike("email", match.email ?: mail) }
                            }
                    }
                }
                true
            }
        } catch (error: Throwable) {
            Log.w(TAG, "admin_db check failed: ${error.message}")
            false
        }
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
        Log.d(TAG, "Resolving staff gate for UID: '$id', Email: '$mail'")

        val adminRows = try {
            client.postgrest[SupabaseTableData.Tables.ADMIN_DB]
                .select {
                    filter {
                        if (id.isNotEmpty() && mail.isNotEmpty()) {
                            or {
                                eq("uid", id)
                                ilike("email", mail)
                            }
                        } else if (id.isNotEmpty()) {
                            eq("uid", id)
                        } else if (mail.isNotEmpty()) {
                            ilike("email", mail)
                        }
                    }
                }
                .decodeList<AdminDbRow>()
        } catch (e: Exception) {
            Log.w(TAG, "admin_db check skipped/failed: ${e.message}")
            emptyList()
        }

        val matchingAdmin = adminRows.firstOrNull()
        if (matchingAdmin != null) {
            Log.i(TAG, "Staff resolution: SUCCESS (Matched in admin_db)")
            if (id.isNotEmpty() && !matchingAdmin.uid.equals(id, ignoreCase = true)) {
                runCatching {
                    client.postgrest[SupabaseTableData.Tables.ADMIN_DB]
                        .update({ set("uid", id) }) { filter { ilike("email", matchingAdmin.email ?: mail) } }
                }
            }
            return StaffGate.Admin
        }

        Log.d(TAG, "Linking faculty account via link_my_faculty_account RPC...")
        runCatching {
            PortalRepository.linkMyFacultyAccount()
        }.onFailure {
            Log.w(TAG, "link_my_faculty_account note: ${it.message}")
        }

        val portalsRes = PortalRepository.getMyPortals()
        if (portalsRes.isSuccess) {
            val portals = portalsRes.getOrThrow()
            if (portals.isFaculty || portals.isTeacher || portals.isCoordinator || portals.isHod || portals.isMentor) {
                Log.i(TAG, "Staff resolution: SUCCESS via my_portals (isCoordinator=${portals.isCoordinator}, isTeacher=${portals.isTeacher})")
                return StaffGate.Staff
            }
        }

        Log.d(TAG, "Probing faculty table for staff record...")
        val facultyRows = try {
            client.postgrest[SupabaseTableData.Tables.FACULTY]
                .select {
                    filter {
                        if (id.isNotEmpty() && mail.isNotEmpty()) {
                            or {
                                eq("firebase_uid", id)
                                ilike("email", mail)
                            }
                        } else if (id.isNotEmpty()) {
                            eq("firebase_uid", id)
                        } else if (mail.isNotEmpty()) {
                            ilike("email", mail)
                        }
                    }
                }
                .decodeList<FacultyMasterRow>()
        } catch (e: Exception) {
            Log.w(TAG, "faculty table query skipped/failed: ${e.message}")
            emptyList()
        }

        val matchingFaculty = facultyRows.firstOrNull()
        if (matchingFaculty != null) {
            Log.i(TAG, "Staff resolution: SUCCESS (Faculty: ${matchingFaculty.name})")
            if (id.isNotEmpty() && matchingFaculty.firebaseUid.isNullOrEmpty() && mail.isNotEmpty()) {
                runCatching {
                    client.postgrest[SupabaseTableData.Tables.FACULTY]
                        .update({ set("firebase_uid", id) }) { filter { ilike("email", mail) } }
                }
            }
            return StaffGate.Staff
        }

        Log.d(TAG, "Probing v_staff_directory view...")
        val staffDirRows = try {
            client.postgrest[SupabaseTableData.Views.V_STAFF_DIRECTORY]
                .select {
                    filter {
                        if (id.isNotEmpty() && mail.isNotEmpty()) {
                            or {
                                eq("firebase_uid", id)
                                ilike("email", mail)
                            }
                        } else if (id.isNotEmpty()) {
                            eq("firebase_uid", id)
                        } else if (mail.isNotEmpty()) {
                            ilike("email", mail)
                        }
                    }
                }
                .decodeList<StaffDirectoryView>()
        } catch (e: Exception) {
            Log.w(TAG, "v_staff_directory query skipped/failed: ${e.message}")
            emptyList()
        }

        val matchingStaffDir = staffDirRows.firstOrNull()
        if (matchingStaffDir != null) {
            Log.i(TAG, "Staff resolution: SUCCESS (Staff Directory: ${matchingStaffDir.name})")
            if (id.isNotEmpty() && mail.isNotEmpty()) {
                runCatching {
                    client.postgrest[SupabaseTableData.Tables.FACULTY]
                        .update({ set("firebase_uid", id) }) { filter { ilike("email", mail) } }
                }
            }
            return StaffGate.Staff
        }

        Log.d(TAG, "Probing faculty_master table...")
        val facultyMasterRows = try {
            client.postgrest[SupabaseTableData.Tables.FACULTY_MASTER]
                .select {
                    filter {
                        if (id.isNotEmpty() && mail.isNotEmpty()) {
                            or {
                                eq("firebase_uid", id)
                                ilike("email", mail)
                            }
                        } else if (id.isNotEmpty()) {
                            eq("firebase_uid", id)
                        } else if (mail.isNotEmpty()) {
                            ilike("email", mail)
                        }
                    }
                }
                .decodeList<FacultyMasterRow>()
        } catch (e: Exception) {
            Log.w(TAG, "faculty_master query skipped/failed: ${e.message}")
            emptyList()
        }

        val matchingFacultyMaster = facultyMasterRows.firstOrNull()
        if (matchingFacultyMaster != null) {
            Log.i(TAG, "Staff resolution: SUCCESS (Faculty Master: ${matchingFacultyMaster.name})")
            if (id.isNotEmpty() && mail.isNotEmpty()) {
                runCatching {
                    client.postgrest[SupabaseTableData.Tables.FACULTY_MASTER]
                        .update({ set("firebase_uid", id) }) { filter { ilike("email", mail) } }
                }
            }
            return StaffGate.Staff
        }

        Log.d(TAG, "Probing staff_db table...")
        val staffDbRows = try {
            client.postgrest[SupabaseTableData.Tables.STAFF_DB]
                .select {
                    filter {
                        if (id.isNotEmpty() && mail.isNotEmpty()) {
                            or {
                                eq("uid", id)
                                ilike("email_id", mail)
                            }
                        } else if (id.isNotEmpty()) {
                            eq("uid", id)
                        } else if (mail.isNotEmpty()) {
                            ilike("email_id", mail)
                        }
                    }
                }
                .decodeList<StaffDbRow>()
        } catch (e: Exception) {
            Log.w(TAG, "staff_db query skipped/failed: ${e.message}")
            emptyList()
        }

        val matchingStaffDb = staffDbRows.firstOrNull()
        if (matchingStaffDb != null) {
            Log.i(TAG, "Staff resolution: SUCCESS (Staff DB: ${matchingStaffDb.name})")
            return StaffGate.Staff
        }

        Log.w(TAG, "Staff resolution: FAILED (No matching record for UID: '$id', Email: '$mail')")
        return StaffGate.Neither
    }

    sealed interface StaffGate {
        data object Admin : StaffGate
        data object Staff : StaffGate
        data object Neither : StaffGate
        data class Failed(val reason: String) : StaffGate
    }

    suspend fun createStudentAccount(entry: StudentEntry): Result<String> {
        if (!SupabaseManager.isConfigured) return Result.failure(IllegalStateException("Supabase not configured"))
        val created = createActivatedAccount(entry.email, entry.password, entry.name).getOrElse { return Result.failure(it) }
        val studentRow = StudentRow(
            uid = created.uid,
            name = entry.name.blankToNull(),
            serialNo = entry.serialNo.blankToNull(),
            fatherName = entry.fatherName.blankToNull(),
            studentPhoneNo = entry.studentPhoneNo.blankToNull(),
            fatherPhoneNo = entry.fatherPhoneNo.blankToNull(),
            studentEmail = entry.email.blankToNull()
        )
        val inserted = runCatching {
            SupabaseManager.requireClient().postgrest[SupabaseTableData.Tables.STUDENTS].insert(studentRow)
        }
        if (inserted.isFailure) {
            deleteAuthAccount(created.idToken)
            return Result.failure(IllegalStateException("DB Failure: ${inserted.exceptionOrNull()?.message}"))
        }
        val classId = entry.classId
        if (classId != null && classId > 0) {
            val enrollmentRow = EnrollmentRow(
                studentUid = created.uid,
                classId = classId,
                groupId = entry.groupId
            )
            runCatching {
                SupabaseManager.requireClient().postgrest[SupabaseTableData.Tables.ENROLLMENTS].insert(enrollmentRow)
            }
        }
        return Result.success(created.uid)
    }

    suspend fun createStaffAccount(entry: StaffEntry): Result<String> =
        createAccountAndRow(entry.email, entry.password, entry.name, SupabaseTableData.Tables.FACULTY_MASTER) { uid ->
            FacultyMasterRow(
                firebaseUid = uid,
                email = entry.email.blankToNull(),
                name = entry.name.blankToNull(),
                phoneNo = entry.phoneNo.blankToNull(),
                designation = entry.designation.blankToNull(),
                gender = entry.gender.blankToNull()
            )
        }

    private suspend fun createAccountAndRow(email: String, password: String, name: String, table: String, buildRow: (String) -> Any): Result<String> {
        if (!SupabaseManager.isConfigured) return Result.failure(IllegalStateException("Supabase not configured"))
        val created = createActivatedAccount(email, password, name).getOrElse { return Result.failure(it) }
        val inserted = runCatching { SupabaseManager.requireClient().postgrest[table].insert(buildRow(created.uid)) }
        if (inserted.isFailure) {
            deleteAuthAccount(created.idToken)
            return Result.failure(IllegalStateException("DB Failure: ${inserted.exceptionOrNull()?.message}"))
        }
        return Result.success(created.uid)
    }

    private suspend fun createActivatedAccount(email: String, password: String, name: String): Result<CreatedAccount> {
        val created = createAuthAccount(email, password, name).getOrElse { return Result.failure(it) }
        val activated = activateCreatedAccount(created.uid)
        if (activated.isFailure) {
            deleteAuthAccount(created.idToken)
            return Result.failure(
                IllegalStateException(
                    "Firebase account created, but activation failed: " +
                        (activated.exceptionOrNull()?.message ?: "unknown error")
                )
            )
        }
        return Result.success(created)
    }

    private suspend fun activateCreatedAccount(uid: String): Result<Unit> = runCatching {
        val base = SupabaseManager.supabaseUrl().trim().trimEnd('/')
        if (base.isEmpty()) error("Supabase URL is not configured.")
        val admin = FirebaseAuth.getInstance().currentUser
            ?: error("Your admin session expired. Sign in again.")
        val token = admin.getIdToken(true).await()?.token
            ?: error("Could not obtain your admin token. Sign in again.")

        val response = http.post("$base/functions/v1/$ACTIVATION_FUNCTION") {
            header(HttpHeaders.Authorization, "Bearer $token")
            header("apikey", SupabaseManager.supabaseAnonKey())
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(ActivationRequest(uid = uid)))
        }
        val text = response.bodyAsText()
        val parsed = runCatching { json.decodeFromString<ActivationResponse>(text) }.getOrNull()
        if (!response.status.isSuccess() || parsed?.ok != true) {
            error(parsed?.error ?: "Account activation failed (${response.status.value})")
        }
    }

    suspend fun getCourses(): List<CourseRow> = runCatching { SupabaseManager.requireClient().postgrest[SupabaseTableData.Tables.COURSES].select().decodeList<CourseRow>() }.getOrDefault(emptyList())
    suspend fun getBranches(cid: Int): List<BranchRow> = runCatching { SupabaseManager.requireClient().postgrest[SupabaseTableData.Tables.BRANCHES].select { filter { eq("course_id", cid) } }.decodeList<BranchRow>() }.getOrDefault(emptyList())
    suspend fun getClasses(): List<ClassRow> = runCatching { SupabaseManager.requireClient().postgrest[SupabaseTableData.Tables.CLASSES].select().decodeList<ClassRow>() }.getOrDefault(emptyList())
    suspend fun getClassesForBranch(bid: Int): List<ClassRow> = runCatching { SupabaseManager.requireClient().postgrest[SupabaseTableData.Tables.CLASSES].select { filter { eq("branch_id", bid) } }.decodeList<ClassRow>() }.getOrDefault(emptyList())
    suspend fun getClassGroups(cid: Int): List<ClassGroupRow> = runCatching { SupabaseManager.requireClient().postgrest[SupabaseTableData.Tables.CLASS_GROUPS].select { filter { eq("class_id", cid) } }.decodeList<ClassGroupRow>() }.getOrDefault(emptyList())


    suspend fun sendNotification(row: NewNotificationRow): Result<NotificationRow> = runCatching {
        SupabaseManager.withAuthRetry {
            val inserted = SupabaseManager.requireClient().postgrest["notifications"]
                .insert(row) { select() }
                .decodeList<NotificationRow>()

            inserted.firstOrNull()
                ?: error("The notification was not posted: the database returned no inserted row.")
        }
    }.onFailure {
        if (it is CancellationException) throw it
        Log.w(TAG, "Notification insert failed: ${it.message}")
    }

    suspend fun getNotificationDelivery(id: Long): Result<NotificationRow> = runCatching {
        SupabaseManager.withAuthRetry {
            SupabaseManager.requireClient().postgrest["notifications"]
                .select {
                    filter { eq("id", id) }
                    limit(1)
                }
                .decodeList<NotificationRow>()
                .firstOrNull()
                ?: error("Notification #$id is no longer readable.")
        }
    }.onFailure {
        if (it is CancellationException) throw it
        Log.w(TAG, "Notification delivery read failed: ${it.message}")
    }

    suspend fun getRecentNotifications(max: Long = 8): Result<List<NotificationRow>> = runCatching {
        SupabaseManager.withAuthRetry {
            SupabaseManager.requireClient().postgrest["notifications"]
                .select {
                    order("created_at", Order.DESCENDING)
                    limit(max)
                }
                .decodeList<NotificationRow>()
        }
    }.onFailure {
        if (it is CancellationException) throw it
        Log.w(TAG, "Recent notifications failed: ${it.message}")
    }

    suspend fun cancelNotification(id: Long): Result<Unit> = runCatching {
        SupabaseManager.withAuthRetry {
            SupabaseManager.requireClient().postgrest["notifications"]
                .update({ set("is_cancelled", true) }) {
                    filter { eq("id", id) }
                }
        }
        Unit
    }.onFailure {
        if (it is CancellationException) throw it
        Log.w(TAG, "Notification cancel failed: ${it.message}")
    }


    suspend fun getDashboardStats(): Result<AdminDashboardStats> = runCatching {
        SupabaseManager.withAuthRetry {
            val client = SupabaseManager.requireClient()

            suspend fun countRows(table: String, column: String): Long? = runCatching {
                client.postgrest[table]
                    .select(columns = Columns.list(column)) {
                        count(Count.EXACT)
                        limit(1)
                    }
                    .countOrNull()
            }.getOrNull()

            coroutineScope {
                val students = async { countRows(SupabaseTableData.Tables.STUDENTS, "uid") }
                val staff = async { countRows(SupabaseTableData.Views.V_STAFF_DIRECTORY, "faculty_id") }
                val classes = async { countRows(SupabaseTableData.Tables.CLASSES, "id") }
                val courses = async { countRows(SupabaseTableData.Tables.COURSES, "id") }
                val branches = async { countRows(SupabaseTableData.Tables.BRANCHES, "id") }
                val admins = async { countRows(SupabaseTableData.Tables.ADMIN_DB, "uid") }

                AdminDashboardStats(
                    students = students.await(),
                    staff = staff.await(),
                    classes = classes.await(),
                    courses = courses.await(),
                    branches = branches.await(),
                    admins = admins.await(),
                )
            }
        }
    }.onFailure {
        if (it is CancellationException) throw it
        Log.w(TAG, "Dashboard stats failed: ${it.message}")
    }


    private fun searchPattern(rawQuery: String): String? {
        val term = rawQuery.trim()
            .replace(",", " ")
            .replace("(", " ")
            .replace(")", " ")
            .replace("%", " ")
            .replace("*", " ")
            .trim()
        return term.takeIf { it.isNotEmpty() }?.let { "%$it%" }
    }

    suspend fun searchStudents(rawQuery: String, max: Long = 30): Result<List<StudentProfileView>> = runCatching {
        val pattern = searchPattern(rawQuery) ?: return@runCatching emptyList()
        SupabaseManager.withAuthRetry {
            val client = SupabaseManager.requireClient()

            val directory = runCatching {
                client.postgrest[SupabaseTableData.Views.V_STUDENT_DIRECTORY]
                    .select {
                        filter {
                            or {
                                ilike("name", pattern)
                                ilike("serial_no", pattern)
                                ilike("student_email", pattern)
                            }
                        }
                        order("created_at", Order.DESCENDING)
                        limit(max)
                    }
                    .decodeList<StudentProfileView>()
            }

            directory.getOrElse {
                client.postgrest[SupabaseTableData.Tables.STUDENTS]
                    .select {
                        filter {
                            or {
                                ilike("name", pattern)
                                ilike("serial_no", pattern)
                                ilike("student_email", pattern)
                            }
                        }
                        order("created_at", Order.DESCENDING)
                        limit(max)
                    }
                    .decodeList<StudentRow>()
                    .map { row ->
                        StudentProfileView(
                            studentUid = row.uid,
                            name = row.name,
                            serialNo = row.serialNo,
                            fatherName = row.fatherName,
                            studentPhoneNo = row.studentPhoneNo,
                            fatherPhoneNo = row.fatherPhoneNo,
                            studentEmail = row.studentEmail,
                            groupName = row.groupName,
                            isLocked = row.isLocked,
                            isPtm = row.isPtm,
                            lockReason = row.lockReason,
                            ptmSchedule = row.ptmSchedule,
                            createdAt = row.createdAt,
                        )
                    }
            }
        }
    }.onFailure {
        if (it is CancellationException) throw it
        Log.w(TAG, "Student search failed: ${it.message}")
    }

    suspend fun getRecentStudents(max: Long = 15): Result<List<StudentProfileView>> = runCatching {
        SupabaseManager.withAuthRetry {
            val client = SupabaseManager.requireClient()

            val directory = runCatching {
                client.postgrest[SupabaseTableData.Views.V_STUDENT_DIRECTORY]
                    .select {
                        order("created_at", Order.DESCENDING)
                        limit(max)
                    }
                    .decodeList<StudentProfileView>()
            }

            directory.getOrElse {
                client.postgrest[SupabaseTableData.Tables.STUDENTS]
                    .select {
                        order("created_at", Order.DESCENDING)
                        limit(max)
                    }
                    .decodeList<StudentRow>()
                    .map { row ->
                        StudentProfileView(
                            studentUid = row.uid,
                            name = row.name,
                            serialNo = row.serialNo,
                            fatherName = row.fatherName,
                            studentPhoneNo = row.studentPhoneNo,
                            fatherPhoneNo = row.fatherPhoneNo,
                            studentEmail = row.studentEmail,
                            groupName = row.groupName,
                            isLocked = row.isLocked,
                            isPtm = row.isPtm,
                            lockReason = row.lockReason,
                            ptmSchedule = row.ptmSchedule,
                            createdAt = row.createdAt,
                        )
                    }
            }
        }
    }.onFailure {
        if (it is CancellationException) throw it
        Log.w(TAG, "Recent students failed: ${it.message}")
    }

    suspend fun searchStaff(rawQuery: String, max: Long = 30): Result<List<StaffDirectoryView>> = runCatching {
        val pattern = searchPattern(rawQuery) ?: return@runCatching emptyList()
        SupabaseManager.withAuthRetry {
            val client = SupabaseManager.requireClient()

            val directory = runCatching {
                client.postgrest[SupabaseTableData.Views.V_STAFF_DIRECTORY]
                    .select {
                        filter {
                            or {
                                ilike("name", pattern)
                                ilike("display_name", pattern)
                                ilike("email", pattern)
                                ilike("designation", pattern)
                                ilike("department_name", pattern)
                            }
                        }
                        order("created_at", Order.DESCENDING)
                        limit(max)
                    }
                    .decodeList<StaffDirectoryView>()
                    .distinctBy { it.facultyId }
            }

            val rawStaff = directory.getOrElse {
                client.postgrest[SupabaseTableData.Tables.FACULTY_MASTER]
                    .select {
                        filter {
                            or {
                                ilike("name", pattern)
                                ilike("email", pattern)
                                ilike("designation", pattern)
                            }
                        }
                        order("created_at", Order.DESCENDING)
                        limit(max)
                    }
                    .decodeList<FacultyMasterRow>()
                    .map { row ->
                        StaffDirectoryView(
                            facultyId = row.id,
                            firebaseUid = row.firebaseUid,
                            title = row.title,
                            name = row.name,
                            displayName = listOfNotNull(row.title, row.name)
                                .joinToString(" ")
                                .trim()
                                .ifEmpty { null },
                            email = row.email,
                            phoneNo = row.phoneNo,
                            designation = row.designation,
                            departmentId = row.departmentId,
                            isActive = row.isActive,
                            createdAt = row.createdAt,
                        )
                    }
            }
            val (adminUids, adminEmails) = fetchAdminIdentifiers()
            markAdmins(rawStaff, adminUids, adminEmails)
        }
    }.onFailure {
        if (it is CancellationException) throw it
        Log.w(TAG, "Staff search failed: ${it.message}")
    }

    suspend fun getRecentStaff(max: Long = 15): Result<List<StaffDirectoryView>> = runCatching {
        SupabaseManager.withAuthRetry {
            val client = SupabaseManager.requireClient()

            val directory = runCatching {
                client.postgrest[SupabaseTableData.Views.V_STAFF_DIRECTORY]
                    .select {
                        order("created_at", Order.DESCENDING)
                        limit(max)
                    }
                    .decodeList<StaffDirectoryView>()
                    .distinctBy { it.facultyId }
            }

            val rawStaff = directory.getOrElse {
                client.postgrest[SupabaseTableData.Tables.FACULTY_MASTER]
                    .select {
                        order("created_at", Order.DESCENDING)
                        limit(max)
                    }
                    .decodeList<FacultyMasterRow>()
                    .map { row ->
                        StaffDirectoryView(
                            facultyId = row.id,
                            firebaseUid = row.firebaseUid,
                            title = row.title,
                            name = row.name,
                            displayName = listOfNotNull(row.title, row.name)
                                .joinToString(" ")
                                .trim()
                                .ifEmpty { null },
                            email = row.email,
                            phoneNo = row.phoneNo,
                            designation = row.designation,
                            departmentId = row.departmentId,
                            isActive = row.isActive,
                            createdAt = row.createdAt,
                        )
                    }
            }
            val (adminUids, adminEmails) = fetchAdminIdentifiers()
            markAdmins(rawStaff, adminUids, adminEmails)
        }
    }.onFailure {
        if (it is CancellationException) throw it
        Log.w(TAG, "Recent staff failed: ${it.message}")
    }

    private suspend fun fetchAdminIdentifiers(): Pair<Set<String>, Set<String>> {
        return runCatching {
            val client = SupabaseManager.requireClient()
            val rows = client.postgrest[SupabaseTableData.Tables.ADMIN_DB]
                .select()
                .decodeList<AdminDbRow>()
            val uids = rows.mapNotNull { it.uid.trim().takeIf { s -> s.isNotEmpty() } }.toSet()
            val emails = rows.mapNotNull { it.email?.trim()?.lowercase()?.takeIf { s -> s.isNotEmpty() } }.toSet()
            uids to emails
        }.getOrDefault(emptySet<String>() to emptySet<String>())
    }

    private fun markAdmins(staffList: List<StaffDirectoryView>, adminUids: Set<String>, adminEmails: Set<String>): List<StaffDirectoryView> {
        if (adminUids.isEmpty() && adminEmails.isEmpty()) return staffList
        return staffList.map { staff ->
            val isAd = (staff.firebaseUid != null && adminUids.contains(staff.firebaseUid.trim())) ||
                (staff.email != null && adminEmails.contains(staff.email.trim().lowercase()))
            if (isAd) staff.copy(isAdmin = true) else staff
        }
    }

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
    @Serializable private data class ActivationRequest(val action: String = "activate", val uid: String)
    @Serializable private data class ActivationResponse(val ok: Boolean = false, val error: String? = null)
    @Serializable private data class FirebaseErrorResponse(val error: FirebaseErrorBody)
    @Serializable private data class FirebaseErrorBody(val message: String)
}

data class AdminDashboardStats(
    val students: Long? = null,
    val staff: Long? = null,
    val classes: Long? = null,
    val courses: Long? = null,
    val branches: Long? = null,
    val admins: Long? = null,
)

data class StudentEntry(
    val email: String,
    val password: String,
    val name: String,
    val serialNo: String = "",
    val fatherName: String = "",
    val studentPhoneNo: String = "",
    val fatherPhoneNo: String = "",
    val classId: Int? = null,
    val groupId: Int? = null
)

data class StaffEntry(
    val email: String,
    val password: String,
    val name: String,
    val department: String = "",
    val role: String = "",
    val phoneNo: String = "",
    val gender: String = "",
    val designation: String = ""
)

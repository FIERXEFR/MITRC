package com.mitrc.ac.`in`.data

import android.util.Log
import io.github.jan.supabase.postgrest.exception.PostgrestRestException
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.postgrest.rpc
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.put

object PortalRepository {

    private const val TAG = "PortalRepository"
    private val json = Json { ignoreUnknownKeys = true }

    private suspend fun <T> query(block: suspend () -> T): Result<T> = runCatching {
        SupabaseManager.withAuthRetry { block() }
    }

    // -----------------------------------------------------------------------------------------
    // Student Queries
    // -----------------------------------------------------------------------------------------

    suspend fun getStudentProfile(uid: String): Result<StudentProfileView?> = query {
        val client = SupabaseManager.requireClient()
        Log.d(TAG, "getStudentProfile for uid=$uid")

        val fromView = runCatching {
            client.postgrest[SupabaseTableData.Views.V_STUDENT_PROFILES]
                .select { filter { eq("student_uid", uid) } }
                .decodeList<StudentProfileView>()
                .firstOrNull()
        }

        if (fromView.isSuccess && fromView.getOrNull() != null) {
            val prof = fromView.getOrThrow()
            if (prof != null && prof.name.isNullOrBlank()) {
                // The view can serve a row whose `name` is still NULL (student row written before
                // the name was captured). The `students` table is authoritative, so backfill it
                // here rather than greeting the user as "Student" for the rest of the session.
                val tabledName = runCatching {
                    client.postgrest[SupabaseTableData.Tables.STUDENTS]
                        .select { filter { eq("uid", uid) } }
                        .decodeList<StudentRow>()
                        .firstOrNull()?.name
                }.getOrNull()
                if (!tabledName.isNullOrBlank()) {
                    Log.i(TAG, "Student name backfilled from students table")
                    return@query prof.copy(name = tabledName)
                }
            }
            Log.i(TAG, "Student profile loaded from v_student_profiles (classId=${prof?.classId})")
            return@query prof
        } else {
            Log.w(TAG, "v_student_profiles query returned null/failed: ${fromView.exceptionOrNull()?.message}")
        }

        // Fallback: Query students table & enrollments
        val fromStudentTable = runCatching {
            client.postgrest[SupabaseTableData.Tables.STUDENTS]
                .select { filter { eq("uid", uid) } }
                .decodeList<StudentRow>()
                .firstOrNull()
        }.getOrNull()

        if (fromStudentTable != null) {
            val enrollment = runCatching {
                client.postgrest[SupabaseTableData.Tables.ENROLLMENTS]
                    .select { filter { eq("student_uid", uid) } }
                    .decodeList<EnrollmentRow>()
                    .firstOrNull()
            }.getOrNull()

            Log.i(TAG, "Student profile resolved from students table (classId=${enrollment?.classId})")
            return@query StudentProfileView(
                studentUid = fromStudentTable.uid,
                name = fromStudentTable.name,
                serialNo = fromStudentTable.serialNo,
                fatherName = fromStudentTable.fatherName,
                studentPhoneNo = fromStudentTable.studentPhoneNo,
                fatherPhoneNo = fromStudentTable.fatherPhoneNo,
                studentEmail = fromStudentTable.studentEmail,
                groupName = fromStudentTable.groupName,
                isLocked = fromStudentTable.isLocked,
                isPtm = fromStudentTable.isPtm,
                classId = enrollment?.classId
            )
        }

        null
    }

    suspend fun getStudentSubjects(classId: Int): Result<List<ClassSubjectView>> = query {
        if (classId <= 0) {
            Log.w(TAG, "getStudentSubjects: student has no class yet (classId=$classId); skipping subject query.")
            return@query emptyList()
        }

        val client = SupabaseManager.requireClient()
        val tokenInfo = SupabaseManager.getAuthTokenInfo()
        Log.d(TAG, "getStudentSubjects: Querying v_class_subjects for classId=$classId. Auth Token Info: $tokenInfo")

        val responseResult = runCatching {
            client.postgrest[SupabaseTableData.Views.V_CLASS_SUBJECTS]
                .select { filter { eq("class_id", classId) } }
        }

        if (responseResult.isSuccess) {
            val response = responseResult.getOrThrow()
            val list = runCatching { response.decodeList<ClassSubjectView>() }.getOrDefault(emptyList())

            if (list.isEmpty()) {
                Log.w(
                    TAG,
                    "v_class_subjects LIST IS EMPTY!\n" +
                    "HTTP Status: 200 (Success)\n" +
                    "classId: $classId\n" +
                    "Authorization Header Token Info: $tokenInfo"
                )
            } else {
                Log.i(
                    TAG,
                    "v_class_subjects returned ${list.size} subject cards.\n" +
                    "HTTP Status: 200 (Success)\n" +
                    "classId: $classId\n" +
                    "Authorization Header Token Info: $tokenInfo"
                )
            }
            return@query list
        } else {
            val error = responseResult.exceptionOrNull()
            val httpStatus = if (error is PostgrestRestException) {
                error.statusCode.toString()
            } else {
                "UNKNOWN"
            }

            Log.e(
                TAG,
                "v_class_subjects QUERY FAILED!\n" +
                "HTTP Status: $httpStatus\n" +
                "classId: $classId\n" +
                "Authorization Header Token Info: $tokenInfo\n" +
                "Error: ${error?.message}",
                error
            )
            return@query emptyList()
        }
    }

    suspend fun getStudentAttendance(uid: String): Result<List<AttendanceSummaryView>> = query {
        SupabaseManager.requireClient().postgrest[SupabaseTableData.Views.V_ATTENDANCE_SUMMARY]
            .select { filter { eq("student_uid", uid) } }
            .decodeList<AttendanceSummaryView>()
    }

    suspend fun getStudentMarks(uid: String): Result<List<MarkView>> = query {
        SupabaseManager.requireClient().postgrest[SupabaseTableData.Views.V_MARKS]
            .select { filter { eq("student_uid", uid) } }
            .decodeList<MarkView>()
    }

    suspend fun getMyTimetable(): Result<List<MyTimetableEntryView>> = query {
        SupabaseManager.requireClient().postgrest[SupabaseTableData.Views.V_MY_TIMETABLE]
            .select()
            .decodeList<MyTimetableEntryView>()
    }

    suspend fun getPublishedNotes(): Result<List<VNoteView>> = query {
        SupabaseManager.requireClient().postgrest[SupabaseTableData.Views.V_NOTES]
            .select { filter { eq("is_visible", true) } }
            .decodeList<VNoteView>()
    }

    suspend fun createNote(note: NoteInsertRow): Result<Unit> = query {
        SupabaseManager.requireClient().postgrest[SupabaseTableData.Tables.NOTES]
            .insert(note)
    }

    // -----------------------------------------------------------------------------------------
    // Auth & Portals RPCs
    // -----------------------------------------------------------------------------------------

    suspend fun linkMyFacultyAccount(): Result<Unit> = query {
        Log.d(TAG, "Calling link_my_faculty_account RPC...")
        SupabaseManager.requireClient().postgrest.rpc("link_my_faculty_account")
    }

    suspend fun getMyPortals(): Result<MyPortalsResponse> = query {
        Log.d(TAG, "Calling my_portals RPC...")
        val response = SupabaseManager.requireClient().postgrest.rpc("my_portals")
        // my_portals returns a single row or a list of one row depending on PostgREST version
        runCatching {
            response.decodeAs<MyPortalsResponse>()
        }.getOrElse {
            response.decodeList<MyPortalsResponse>().firstOrNull() ?: MyPortalsResponse()
        }
    }.onFailure { Log.w(TAG, "my_portals RPC failed: ${it::class.simpleName}: ${it.message}") }

    // -----------------------------------------------------------------------------------------
    // Teacher / Faculty Queries
    // -----------------------------------------------------------------------------------------

    suspend fun getTeacherProfile(uid: String): Result<FacultyMasterRow?> = query {
        SupabaseManager.requireClient().postgrest[SupabaseTableData.Tables.FACULTY_MASTER]
            .select { filter { eq("firebase_uid", uid) } }
            .decodeList<FacultyMasterRow>()
            .firstOrNull()
    }

    /**
     * Resolves the signed-in user's faculty_master row by firebase_uid first, then by verified
     * email. A staff account whose firebase_uid link hasn't landed yet (the link write is
     * blocked unless an UPDATE policy or SECURITY DEFINER RPC exists) is only reachable by
     * email - without this every role probe downstream silently resolves to "student".
     */
    suspend fun getFacultyByUidOrEmail(uid: String, email: String): Result<FacultyMasterRow?> = query {
        val table = SupabaseManager.requireClient().postgrest[SupabaseTableData.Tables.FACULTY_MASTER]
        val byUid = if (uid.isNotBlank()) {
            runCatching {
                table.select { filter { eq("firebase_uid", uid) } }
                    .decodeList<FacultyMasterRow>()
                    .firstOrNull()
            }.getOrNull()
        } else null
        byUid ?: if (email.isNotBlank()) {
            runCatching {
                table.select { filter { ilike("email", email.trim()) } }
                    .decodeList<FacultyMasterRow>()
                    .firstOrNull()
            }.getOrNull()
        } else null
    }

    /**
     * Best-effort backlink of [uid] onto the faculty_master row for [email], only when the row
     * has no firebase_uid yet (never clobbers an existing link). Silently no-ops under RLS when
     * no UPDATE policy exists - the RPC or a policy is the durable fix.
     */
    suspend fun linkFacultyUidByEmail(uid: String, email: String): Result<Unit> = query {
        SupabaseManager.requireClient().postgrest[SupabaseTableData.Tables.FACULTY_MASTER]
            .update({ set("firebase_uid", uid) }) {
                filter {
                    ilike("email", email.trim())
                    // PostgREST `is.null`; supabase-kt 3.1.1 has no isNull() helper.
                    filter("firebase_uid", FilterOperator.IS, null)
                }
            }
    }

    suspend fun getTeacherSubjects(facultyId: Int): Result<List<ClassSubjectView>> = query {
        SupabaseManager.requireClient().postgrest[SupabaseTableData.Views.V_SUBJECT_TEACHERS]
            .select { filter { eq("faculty_id", facultyId) } }
            .decodeList<ClassSubjectView>()
    }

    suspend fun getEnrolledStudentsForClass(classId: Int): Result<List<StudentProfileView>> = query {
        val rows = SupabaseManager.requireClient().postgrest[SupabaseTableData.Views.V_STUDENT_PROFILES]
            .select { filter { eq("class_id", classId) } }
            .decodeList<StudentProfileView>()
        withStudentIdentities(rows)
    }

    suspend fun getExistingAttendance(classSubjectId: Int, date: String, period: Int): Result<List<AttendanceRecordRow>> = query {
        val client = SupabaseManager.requireClient()
        val session = client.postgrest[SupabaseTableData.Tables.ATTENDANCE_SESSIONS]
            .select {
                filter {
                    eq("class_subject_id", classSubjectId)
                    eq("class_date", date)
                    eq("period", period)
                }
            }
            .decodeList<AttendanceSessionRow>()
            .firstOrNull()

        if (session == null || session.id.isBlank()) {
            emptyList()
        } else {
            client.postgrest[SupabaseTableData.Tables.ATTENDANCE_RECORDS]
                .select { filter { eq("session_id", session.id) } }
                .decodeList<AttendanceRecordRow>()
        }
    }

    suspend fun getAssessments(classSubjectId: Int): Result<List<AssessmentRow>> = query {
        SupabaseManager.requireClient().postgrest[SupabaseTableData.Tables.ASSESSMENTS]
            .select { filter { eq("class_subject_id", classSubjectId) } }
            .decodeList<AssessmentRow>()
    }

    suspend fun createAssessment(assessment: AssessmentRow): Result<AssessmentRow> = query {
        SupabaseManager.requireClient().postgrest[SupabaseTableData.Tables.ASSESSMENTS]
            .insert(assessment) { select() }
            .decodeSingle<AssessmentRow>()
    }

    suspend fun saveMarks(marksList: List<MarkRow>): Result<Unit> = query {
        SupabaseManager.requireClient().postgrest[SupabaseTableData.Tables.MARKS]
            .upsert(marksList)
    }

    suspend fun saveAttendance(
        classSubjectId: Int,
        classDate: String,
        period: Int,
        records: List<TakeAttendanceRecord>
    ): Result<String> = query {
        val client = SupabaseManager.requireClient()
        val recordsJson = json.encodeToJsonElement(records)
        val params = buildJsonObject {
            put("p_class_subject_id", classSubjectId)
            put("p_class_date", classDate)
            put("p_period", period)
            put("p_records", recordsJson)
        }
        val response = client.postgrest.rpc("take_attendance", params)
        response.decodeAs<String>()
    }

    // -----------------------------------------------------------------------------------------
    // Coordinator & Mentee Queries
    // -----------------------------------------------------------------------------------------

    suspend fun getMyCoordinatorClasses(): Result<List<MyCoordinatorClassView>> = query {
        SupabaseManager.requireClient().postgrest[SupabaseTableData.Views.V_MY_COORDINATOR_CLASSES]
            .select()
            .decodeList<MyCoordinatorClassView>()
    }

    suspend fun getCoordinatorsList(): Result<List<CoordinatorView>> = query {
        SupabaseManager.requireClient().postgrest[SupabaseTableData.Views.V_COORDINATORS]
            .select()
            .decodeList<CoordinatorView>()
    }

    suspend fun getStaffDirectory(): Result<List<StaffDirectoryView>> = query {
        SupabaseManager.requireClient().postgrest[SupabaseTableData.Views.V_STAFF_DIRECTORY]
            .select()
            .decodeList<StaffDirectoryView>()
    }

    suspend fun getMyMentees(): Result<List<MyMenteeView>> = query {
        SupabaseManager.requireClient().postgrest[SupabaseTableData.Views.V_MY_MENTEES]
            .select()
            .decodeList<MyMenteeView>()
    }

    suspend fun getMyMenteesAttendance(): Result<List<MyMenteeAttendanceView>> = query {
        SupabaseManager.requireClient().postgrest[SupabaseTableData.Views.V_MY_MENTEES_ATTENDANCE]
            .select()
            .decodeList<MyMenteeAttendanceView>()
    }

    /**
     * Query students across specified class IDs (from v_my_coordinator_classes).
     *
     * `v_student_directory` supplies the class/semester/section mapping, while the `students`
     * table is authoritative for the person: name, roll number, emails, phone numbers, lab
     * group and the lock/PTM flags. The directory can lag behind an admin edit (or serve a row
     * written before the name was captured), so the identity fields are re-read from `students`
     * and win field-by-field.
     */
    suspend fun getStudentsForClasses(classIds: List<Int>): Result<List<StudentProfileView>> = query {
        if (classIds.isEmpty()) return@query emptyList()
        val rows = SupabaseManager.requireClient().postgrest[SupabaseTableData.Views.V_STUDENT_DIRECTORY]
            .select {
                filter {
                    isIn("class_id", classIds)
                }
            }
            .decodeList<StudentProfileView>()
        withStudentIdentities(rows)
    }

    /**
     * Overlays each directory row with its `students` row. Best-effort: if the identity query is
     * blocked or fails, the directory rows are returned untouched so the caller still renders.
     */
    private suspend fun withStudentIdentities(
        rows: List<StudentProfileView>
    ): List<StudentProfileView> {
        val uids = rows.map { it.studentUid }.filter { it.isNotBlank() }.distinct()
        if (uids.isEmpty()) return rows

        val identities = runCatching {
            SupabaseManager.requireClient().postgrest[SupabaseTableData.Tables.STUDENTS]
                .select { filter { isIn("uid", uids) } }
                .decodeList<StudentRow>()
                .associateBy { it.uid }
        }.getOrDefault(emptyMap<String, StudentRow>())

        if (identities.isEmpty()) return rows

        return rows.map { row ->
            val identity = identities[row.studentUid] ?: return@map row
            row.copy(
                name = identity.name ?: row.name,
                serialNo = identity.serialNo ?: row.serialNo,
                fatherName = identity.fatherName ?: row.fatherName,
                studentPhoneNo = identity.studentPhoneNo ?: row.studentPhoneNo,
                fatherPhoneNo = identity.fatherPhoneNo ?: row.fatherPhoneNo,
                studentEmail = identity.studentEmail ?: row.studentEmail,
                groupName = identity.groupName ?: row.groupName,
                isLocked = identity.isLocked ?: row.isLocked,
                isPtm = identity.isPtm ?: row.isPtm
            )
        }
    }

    suspend fun updateStudentLockStatus(studentUid: String, isLocked: Boolean): Result<Unit> = query {
        SupabaseManager.requireClient().postgrest[SupabaseTableData.Tables.STUDENTS]
            .update({ set("is_locked", isLocked) }) {
                filter { eq("uid", studentUid) }
            }
    }

    suspend fun updateStudentPtmStatus(studentUid: String, isPtm: Boolean): Result<Unit> = query {
        SupabaseManager.requireClient().postgrest[SupabaseTableData.Tables.STUDENTS]
            .update({ set("is_ptm", isPtm) }) {
                filter { eq("uid", studentUid) }
            }
    }

    suspend fun getTimetables(classId: Int? = null): Result<List<TimetableRow>> = query {
        SupabaseManager.requireClient().postgrest[SupabaseTableData.Tables.TIMETABLES]
            .select {
                if (classId != null && classId > 0) {
                    filter { eq("class_id", classId) }
                }
            }
            .decodeList<TimetableRow>()
    }

    suspend fun getTimetableEntries(timetableId: Int): Result<List<TimetableEntryView>> = query {
        SupabaseManager.requireClient().postgrest[SupabaseTableData.Views.V_TIMETABLE_ENTRIES]
            .select { filter { eq("timetable_id", timetableId) } }
            .decodeList<TimetableEntryView>()
    }

    suspend fun getTimetablePeriods(timetableId: Int): Result<List<TimetablePeriodRow>> = query {
        SupabaseManager.requireClient().postgrest[SupabaseTableData.Tables.TIMETABLE_PERIODS]
            .select { filter { eq("timetable_id", timetableId) } }
            .decodeList<TimetablePeriodRow>()
    }

    suspend fun duplicateTimetable(sourceId: Int, effectiveFrom: String): Result<Int> = query {
        val client = SupabaseManager.requireClient()
        val params = buildJsonObject {
            put("p_source", sourceId)
            put("p_effective_from", effectiveFrom)
        }
        val response = client.postgrest.rpc("duplicate_timetable", params)
        response.decodeAs<Int>()
    }

    suspend fun updateTimetableHeader(timetable: TimetableRow): Result<Unit> = query {
        SupabaseManager.requireClient().postgrest[SupabaseTableData.Tables.TIMETABLES]
            .update(timetable) { filter { eq("id", timetable.id) } }
    }

    suspend fun upsertTimetableEntry(entry: TimetableEntryRow): Result<Unit> = query {
        SupabaseManager.requireClient().postgrest[SupabaseTableData.Tables.TIMETABLE_ENTRIES]
            .upsert(entry)
    }

    suspend fun deleteTimetableEntry(entryId: Int): Result<Unit> = query {
        SupabaseManager.requireClient().postgrest[SupabaseTableData.Tables.TIMETABLE_ENTRIES]
            .delete { filter { eq("id", entryId) } }
    }

    suspend fun setStudentGroup(studentUid: String, classId: Int, groupId: Int?): Result<Unit> = query {
        val client = SupabaseManager.requireClient()
        val params = buildJsonObject {
            put("p_student_uid", studentUid)
            put("p_class_id", classId)
            if (groupId != null && groupId > 0) {
                put("p_group_id", groupId)
            } else {
                put("p_group_id", null as String?)
            }
        }
        client.postgrest.rpc("set_student_group", params)
    }

    suspend fun getSubjectTeachers(classId: Int): Result<List<ClassSubjectView>> = query {
        SupabaseManager.requireClient().postgrest[SupabaseTableData.Views.V_SUBJECT_TEACHERS]
            .select { filter { eq("class_id", classId) } }
            .decodeList<ClassSubjectView>()
    }
}

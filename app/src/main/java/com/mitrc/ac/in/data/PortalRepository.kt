package com.mitrc.ac.`in`.data

import android.util.Log
import io.github.jan.supabase.postgrest.exception.PostgrestRestException
import io.github.jan.supabase.postgrest.postgrest
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

    suspend fun getPublishedNotes(): Result<List<NoteRow>> = query {
        SupabaseManager.requireClient().postgrest[SupabaseTableData.Tables.NOTES]
            .select { filter { eq("is_visible", true) } }
            .decodeList<NoteRow>()
    }

    // -----------------------------------------------------------------------------------------
    // Teacher / Faculty Queries
    // -----------------------------------------------------------------------------------------

    suspend fun getTeacherProfile(uid: String): Result<FacultyMasterRow?> = query {
        SupabaseManager.requireClient().postgrest[SupabaseTableData.Tables.FACULTY_MASTER]
            .select { filter { eq("firebase_uid", uid) } }
            .decodeList<FacultyMasterRow>()
            .firstOrNull()
    }

    suspend fun getTeacherSubjects(teacherId: Int): Result<List<ClassSubjectView>> = query {
        SupabaseManager.requireClient().postgrest[SupabaseTableData.Views.V_SUBJECT_TEACHERS]
            .select { filter { eq("teacher_id", teacherId) } }
            .decodeList<ClassSubjectView>()
    }

    suspend fun getEnrolledStudentsForClass(classId: Int): Result<List<StudentProfileView>> = query {
        SupabaseManager.requireClient().postgrest[SupabaseTableData.Views.V_STUDENT_PROFILES]
            .select { filter { eq("class_id", classId) } }
            .decodeList<StudentProfileView>()
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
    // Coordinator Queries
    // -----------------------------------------------------------------------------------------

    suspend fun getCoordinatorProfile(uid: String): Result<FacultyMasterRow?> = query {
        SupabaseManager.requireClient().postgrest[SupabaseTableData.Tables.FACULTY_MASTER]
            .select { 
                filter { 
                    eq("firebase_uid", uid) 
                    eq("is_coordinator", true)
                } 
            }
            .decodeList<FacultyMasterRow>()
            .firstOrNull()
    }

    suspend fun getCoordinators(): Result<List<FacultyMasterRow>> = query {
        SupabaseManager.requireClient().postgrest[SupabaseTableData.Tables.FACULTY_MASTER]
            .select { filter { eq("is_coordinator", true) } }
            .decodeList<FacultyMasterRow>()
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

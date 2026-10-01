package com.mitrc.ac.`in`.data

import android.util.Log
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.put

object PortalRepository {

    private const val TAG = "PortalRepository"
    private val json = Json { ignoreUnknownKeys = true }

    // -----------------------------------------------------------------------------------------
    // Student Queries
    // -----------------------------------------------------------------------------------------

    suspend fun getStudentProfile(uid: String): Result<StudentDirectoryView?> = runCatching {
        SupabaseManager.requireClient().postgrest[SupabaseTableData.Views.V_STUDENT_DIRECTORY]
            .select { filter { eq("uid", uid) } }
            .decodeList<StudentDirectoryView>()
            .firstOrNull()
    }

    suspend fun getStudentSubjects(uid: String): Result<List<StudentSubjectView>> = runCatching {
        SupabaseManager.requireClient().postgrest[SupabaseTableData.Views.V_STUDENT_SUBJECTS]
            .select { filter { eq("student_uid", uid) } }
            .decodeList<StudentSubjectView>()
    }

    suspend fun getStudentAttendance(uid: String): Result<List<AttendanceSummaryView>> = runCatching {
        SupabaseManager.requireClient().postgrest[SupabaseTableData.Views.V_ATTENDANCE_SUMMARY]
            .select { filter { eq("student_uid", uid) } }
            .decodeList<AttendanceSummaryView>()
    }

    suspend fun getStudentMarks(uid: String): Result<List<MarkView>> = runCatching {
        SupabaseManager.requireClient().postgrest[SupabaseTableData.Views.V_MARKS]
            .select { filter { eq("student_uid", uid) } }
            .decodeList<MarkView>()
    }

    suspend fun getMyTimetable(): Result<List<MyTimetableEntryView>> = runCatching {
        SupabaseManager.requireClient().postgrest[SupabaseTableData.Views.V_MY_TIMETABLE]
            .select()
            .decodeList<MyTimetableEntryView>()
    }

    // -----------------------------------------------------------------------------------------
    // Teacher Queries & Attendance RPC
    // -----------------------------------------------------------------------------------------

    suspend fun getTeacherProfile(uid: String): Result<TeacherRow?> = runCatching {
        SupabaseManager.requireClient().postgrest[SupabaseTableData.Tables.TEACHERS]
            .select { filter { eq("firebase_uid", uid) } }
            .decodeList<TeacherRow>()
            .firstOrNull()
    }

    suspend fun getTeacherSubjects(teacherId: Int): Result<List<SubjectTeacherView>> = runCatching {
        SupabaseManager.requireClient().postgrest[SupabaseTableData.Views.V_SUBJECT_TEACHERS]
            .select { filter { eq("teacher_id", teacherId) } }
            .decodeList<SubjectTeacherView>()
    }

    suspend fun getEnrolledStudentsForClass(classId: Int): Result<List<StudentDirectoryView>> = runCatching {
        SupabaseManager.requireClient().postgrest[SupabaseTableData.Views.V_STUDENT_DIRECTORY]
            .select { filter { eq("class_id", classId) } }
            .decodeList<StudentDirectoryView>()
    }

    suspend fun getExistingAttendance(classSubjectId: Int, date: String, period: Int): Result<List<AttendanceRecordRow>> = runCatching {
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

    suspend fun getAssessments(classSubjectId: Int): Result<List<AssessmentRow>> = runCatching {
        SupabaseManager.requireClient().postgrest[SupabaseTableData.Tables.ASSESSMENTS]
            .select { filter { eq("class_subject_id", classSubjectId) } }
            .decodeList<AssessmentRow>()
    }

    suspend fun createAssessment(assessment: AssessmentRow): Result<AssessmentRow> = runCatching {
        SupabaseManager.requireClient().postgrest[SupabaseTableData.Tables.ASSESSMENTS]
            .insert(assessment) { select() }
            .decodeSingle<AssessmentRow>()
    }

    suspend fun saveMarks(marksList: List<MarkRow>): Result<Unit> = runCatching {
        SupabaseManager.requireClient().postgrest[SupabaseTableData.Tables.MARKS]
            .upsert(marksList)
    }

    suspend fun saveAttendance(
        classSubjectId: Int,
        classDate: String,
        period: Int,
        records: List<TakeAttendanceRecord>
    ): Result<String> = runCatching {
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
    // Coordinator Queries & Timetable Operations
    // -----------------------------------------------------------------------------------------

    suspend fun getCoordinatorProfile(uid: String): Result<CoordinatorRow?> = runCatching {
        SupabaseManager.requireClient().postgrest[SupabaseTableData.Tables.COORDINATORS]
            .select { filter { eq("firebase_uid", uid) } }
            .decodeList<CoordinatorRow>()
            .firstOrNull()
    }

    suspend fun getTimetables(classId: Int? = null): Result<List<TimetableRow>> = runCatching {
        SupabaseManager.requireClient().postgrest[SupabaseTableData.Tables.TIMETABLES]
            .select {
                if (classId != null && classId > 0) {
                    filter { eq("class_id", classId) }
                }
            }
            .decodeList<TimetableRow>()
    }

    suspend fun getTimetableEntries(timetableId: Int): Result<List<TimetableEntryView>> = runCatching {
        SupabaseManager.requireClient().postgrest[SupabaseTableData.Views.V_TIMETABLE_ENTRIES]
            .select { filter { eq("timetable_id", timetableId) } }
            .decodeList<TimetableEntryView>()
    }

    suspend fun getTimetablePeriods(timetableId: Int): Result<List<TimetablePeriodRow>> = runCatching {
        SupabaseManager.requireClient().postgrest[SupabaseTableData.Tables.TIMETABLE_PERIODS]
            .select { filter { eq("timetable_id", timetableId) } }
            .decodeList<TimetablePeriodRow>()
    }

    suspend fun duplicateTimetable(sourceId: Int, effectiveFrom: String): Result<Int> = runCatching {
        val client = SupabaseManager.requireClient()
        val params = buildJsonObject {
            put("p_source", sourceId)
            put("p_effective_from", effectiveFrom)
        }
        val response = client.postgrest.rpc("duplicate_timetable", params)
        response.decodeAs<Int>()
    }

    suspend fun updateTimetableHeader(timetable: TimetableRow): Result<Unit> = runCatching {
        SupabaseManager.requireClient().postgrest[SupabaseTableData.Tables.TIMETABLES]
            .update(timetable) { filter { eq("id", timetable.id) } }
    }

    suspend fun upsertTimetableEntry(entry: TimetableEntryRow): Result<Unit> = runCatching {
        SupabaseManager.requireClient().postgrest[SupabaseTableData.Tables.TIMETABLE_ENTRIES]
            .upsert(entry)
    }

    suspend fun deleteTimetableEntry(entryId: Int): Result<Unit> = runCatching {
        SupabaseManager.requireClient().postgrest[SupabaseTableData.Tables.TIMETABLE_ENTRIES]
            .delete { filter { eq("id", entryId) } }
    }

    suspend fun setStudentGroup(studentUid: String, classId: Int, groupId: Int?): Result<Unit> = runCatching {
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

    suspend fun getSubjectTeachers(classId: Int): Result<List<SubjectTeacherView>> = runCatching {
        SupabaseManager.requireClient().postgrest[SupabaseTableData.Views.V_SUBJECT_TEACHERS]
            .select { filter { eq("class_id", classId) } }
            .decodeList<SubjectTeacherView>()
    }
}

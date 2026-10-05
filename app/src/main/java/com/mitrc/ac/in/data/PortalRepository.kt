package com.mitrc.ac.`in`.data

import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.put

object PortalRepository {

    private val json = Json { ignoreUnknownKeys = true }

    private suspend fun <T> query(block: suspend () -> T): Result<T> = runCatching {
        SupabaseManager.withAuthRetry { block() }
    }

    // -----------------------------------------------------------------------------------------
    // Student Queries
    // -----------------------------------------------------------------------------------------

    suspend fun getStudentProfile(uid: String): Result<StudentProfileView?> = query {
        SupabaseManager.requireClient().postgrest[SupabaseTableData.Views.V_STUDENT_PROFILES]
            .select { filter { eq("uid", uid) } }
            .decodeList<StudentProfileView>()
            .firstOrNull()
    }

    suspend fun getStudentSubjects(uid: String): Result<List<StudentSubjectView>> = query {
        // Updated to use the new view name if needed, assuming V_STUDENT_SUBJECTS is still valid or similar
        SupabaseManager.requireClient().postgrest[SupabaseTableData.Views.V_STUDENT_SUBJECTS]
            .select { filter { eq("student_uid", uid) } }
            .decodeList<StudentSubjectView>()
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
        SupabaseManager.requireClient().postgrest[SupabaseTableData.Views.V_CLASS_SUBJECTS]
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
        SupabaseManager.requireClient().postgrest[SupabaseTableData.Views.V_CLASS_SUBJECTS]
            .select { filter { eq("class_id", classId) } }
            .decodeList<ClassSubjectView>()
    }
}

package com.mitrc.ac.`in`.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Single source of truth for every Supabase (PostgREST) table, view, and row model.
 * Column names match exact database definitions (snake_case).
 */
object SupabaseTableData {

    object Tables {
        const val ADMIN_DB = "admin_db"
        const val STUDENTS = "students"
        const val FACULTY = "faculty"
        const val FACULTY_MASTER = "faculty_master"
        const val COURSES = "courses"
        const val BRANCHES = "branches"
        const val SUBJECTS = "subjects"
        const val CLASSES = "classes"
        const val CLASS_SUBJECTS = "class_subjects"
        const val CLASS_SUBJECT_TEACHERS = "class_subject_teachers"
        const val CLASS_GROUPS = "class_groups"
        const val ENROLLMENTS = "enrollments"
        const val ASSESSMENTS = "assessments"
        const val MARKS = "marks"
        const val ATTENDANCE_SESSIONS = "attendance_sessions"
        const val ATTENDANCE_RECORDS = "attendance_records"
        const val TIMETABLES = "timetables"
        const val TIMETABLE_PERIODS = "timetable_periods"
        const val TIMETABLE_ENTRIES = "timetable_entries"
        const val NOTES = "notes"
    }

    object Views {
        const val V_STUDENT_DIRECTORY = "v_student_directory"
        const val V_STUDENT_PROFILES = "v_student_profiles"
        const val V_STUDENT_SUBJECTS = "v_student_subjects"
        const val V_CLASS_SUBJECTS = "v_class_subjects"
        const val V_SUBJECT_TEACHERS = "v_subject_teachers"
        const val V_STAFF_DIRECTORY = "v_staff_directory"
        const val V_ATTENDANCE_SUMMARY = "v_attendance_summary"
        const val V_MARKS = "v_marks"
        const val V_ACTIVE_TIMETABLES = "v_active_timetables"
        const val V_TIMETABLE_ENTRIES = "v_timetable_entries"
        const val V_MY_TIMETABLE = "v_my_timetable"
    }
}

internal fun String?.blankToNull(): String? = this?.trim()?.takeIf { it.isNotEmpty() }

// ---------------------------------------------------------------------------------------------
// Row shapes - Table Models
// ---------------------------------------------------------------------------------------------

@Serializable
data class AdminDbRow(
    @SerialName("uid") val uid: String,
    @SerialName("name") val name: String = "",
    @SerialName("email") val email: String? = null,
)

@Serializable
data class StudentRow(
    @SerialName("uid") val uid: String,
    @SerialName("name") val name: String? = null,
    @SerialName("serial_no") val serialNo: String? = null,
    @SerialName("father_name") val fatherName: String? = null,
    @SerialName("student_phone_no") val studentPhoneNo: String? = null,
    @SerialName("father_phone_no") val fatherPhoneNo: String? = null,
    @SerialName("student_email") val studentEmail: String? = null,
    @SerialName("group_name") val groupName: String? = null,
    @SerialName("is_locked") val isLocked: Boolean? = null,
    @SerialName("is_ptm") val isPtm: Boolean? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)

@Serializable
data class FacultyMasterRow(
    @SerialName("id") val id: Int = 0,
    @SerialName("firebase_uid") val firebaseUid: String? = null,
    @SerialName("email") val email: String? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("phone_no") val phoneNo: String? = null,
    @SerialName("department") val department: String? = null,
    @SerialName("designation") val designation: String? = null,
    @SerialName("role") val role: String? = null,
    @SerialName("is_coordinator") val isCoordinator: Boolean = false,
)

@Serializable
data class CourseRow(
    @SerialName("id") val id: Int = 0,
    @SerialName("name") val name: String,
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
data class BranchRow(
    @SerialName("id") val id: Int = 0,
    @SerialName("course_id") val courseId: Int,
    @SerialName("name") val name: String,
)

@Serializable
data class SubjectRow(
    @SerialName("id") val id: Int = 0,
    @SerialName("code") val code: String = "",
    @SerialName("name") val name: String = "",
    @SerialName("kind") val kind: String = "theory", // 'theory' | 'lab'
)

@Serializable
data class ClassRow(
    @SerialName("id") val id: Int = 0,
    @SerialName("branch_id") val branchId: Int,
    @SerialName("semester") val semester: Int,
    @SerialName("section") val section: String,
    @SerialName("academic_year") val academicYear: String,
)

@Serializable
data class ClassSubjectRow(
    @SerialName("id") val id: Int = 0,
    @SerialName("class_id") val classId: Int,
    @SerialName("subject_id") val subjectId: Int,
)

@Serializable
data class ClassGroupRow(
    @SerialName("id") val id: Int = 0,
    @SerialName("class_id") val classId: Int,
    @SerialName("name") val name: String,
)

@Serializable
data class EnrollmentRow(
    @SerialName("student_uid") val studentUid: String,
    @SerialName("class_id") val classId: Int,
    @SerialName("enrolled_at") val enrolledAt: String? = null,
    @SerialName("group_id") val groupId: Int? = null,
)

@Serializable
data class AssessmentRow(
    @SerialName("id") val id: Int = 0,
    @SerialName("class_subject_id") val classSubjectId: Int,
    @SerialName("kind") val kind: String, // UT|EXP|ASSIGNMENT|MID|END
    @SerialName("number") val number: Int,
    @SerialName("max_marks") val maxMarks: Double? = null,
)

@Serializable
data class MarkRow(
    @SerialName("assessment_id") val assessmentId: Int,
    @SerialName("student_uid") val studentUid: String,
    @SerialName("marks") val marks: Double? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)

@Serializable
data class AttendanceSessionRow(
    @SerialName("id") val id: String = "",
    @SerialName("class_subject_id") val classSubjectId: Int,
    @SerialName("class_date") val classDate: String,
    @SerialName("period") val period: Int,
    @SerialName("taken_by") val takenBy: Int? = null, // Faculty ID
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)

@Serializable
data class AttendanceRecordRow(
    @SerialName("session_id") val sessionId: String,
    @SerialName("student_uid") val studentUid: String,
    @SerialName("status") val status: String, // 'P'|'A'|'L'|'OD'
)

@Serializable
data class TimetableRow(
    @SerialName("id") val id: Int = 0,
    @SerialName("class_id") val classId: Int,
    @SerialName("effective_from") val effectiveFrom: String,
    @SerialName("room") val room: String? = null,
    @SerialName("is_published") val isPublished: Boolean = false,
    @SerialName("published_at") val publishedAt: String? = null,
    @SerialName("lunch_after_period") val lunchAfterPeriod: Int? = null,
    @SerialName("lunch_start") val lunchStart: String? = null,
    @SerialName("lunch_end") val lunchEnd: String? = null,
)

@Serializable
data class TimetablePeriodRow(
    @SerialName("timetable_id") val timetableId: Int,
    @SerialName("period") val period: Int,
    @SerialName("start_time") val startTime: String,
    @SerialName("end_time") val endTime: String,
)

@Serializable
data class TimetableEntryRow(
    @SerialName("id") val id: Int = 0,
    @SerialName("timetable_id") val timetableId: Int,
    @SerialName("day_of_week") val dayOfWeek: Int, // 1=Mon..7=Sun
    @SerialName("start_period") val startPeriod: Int,
    @SerialName("end_period") val endPeriod: Int,
    @SerialName("class_subject_id") val classSubjectId: Int,
    @SerialName("group_id") val groupId: Int? = null,
    @SerialName("teacher_id") val teacherId: Int? = null,
    @SerialName("room") val room: String? = null,
)

// ---------------------------------------------------------------------------------------------
// Row shapes - Views
// ---------------------------------------------------------------------------------------------

@Serializable
data class StudentProfileView(
    @SerialName("student_uid") val studentUid: String = "",
    @SerialName("name") val name: String? = null,
    @SerialName("serial_no") val serialNo: String? = null,
    @SerialName("father_name") val fatherName: String? = null,
    @SerialName("student_phone_no") val studentPhoneNo: String? = null,
    @SerialName("father_phone_no") val fatherPhoneNo: String? = null,
    @SerialName("student_email") val studentEmail: String? = null,
    @SerialName("group_name") val groupName: String? = null,
    @SerialName("is_locked") val isLocked: Boolean? = null,
    @SerialName("is_ptm") val isPtm: Boolean? = null,
    @SerialName("course") val course: String? = null,
    @SerialName("branch") val branch: String? = null,
    @SerialName("semester") val semester: Int? = null,
    @SerialName("section") val section: String? = null,
    @SerialName("academic_year") val academicYear: String? = null,
    @SerialName("class_id") val classId: Int? = null,
) {
    val uid: String get() = studentUid
}

@Serializable
data class AttendanceSummaryView(
    @SerialName("student_uid") val studentUid: String,
    @SerialName("class_subject_id") val classSubjectId: Int,
    @SerialName("subject_code") val subjectCode: String,
    @SerialName("subject_name") val subjectName: String,
    @SerialName("attended") val attended: Long,
    @SerialName("total") val total: Long,
    @SerialName("percentage") val percentage: Double,
)

@Serializable
data class MarkView(
    @SerialName("student_uid") val studentUid: String,
    @SerialName("class_subject_id") val classSubjectId: Int,
    @SerialName("subject_code") val subjectCode: String,
    @SerialName("subject_name") val subjectName: String,
    @SerialName("kind") val kind: String,
    @SerialName("number") val number: Int,
    @SerialName("marks") val marks: Double? = null,
    @SerialName("max_marks") val maxMarks: Double? = null,
)

@Serializable
data class ClassSubjectView(
    @SerialName("class_subject_id") val rawClassSubjectId: Int? = null,
    @SerialName("id") val rawId: Int? = null,
    @SerialName("class_id") val rawClassId: Int? = null,
    @SerialName("subject_code") val rawSubjectCode: String? = null,
    @SerialName("code") val rawCode: String? = null,
    @SerialName("subject_name") val rawSubjectName: String? = null,
    @SerialName("name") val rawName: String? = null,
    @SerialName("subject_kind") val rawSubjectKind: String? = null,
    @SerialName("kind") val rawKind: String? = null,
    @SerialName("teacher_id") val rawTeacherId: Int? = null,
    @SerialName("teacher_name") val rawTeacherName: String? = null,
    @SerialName("teacher_names") val rawTeacherNames: String? = null,
    @SerialName("teacher_email") val rawTeacherEmail: String? = null,
    @SerialName("notes_count") val rawNotesCount: Int? = null,
) {
    val classSubjectId: Int get() = rawClassSubjectId ?: rawId ?: 0
    val classId: Int get() = rawClassId ?: 0
    val subjectCode: String get() = rawSubjectCode ?: rawCode ?: "SUB"
    val subjectName: String get() = rawSubjectName ?: rawName ?: "Subject"
    val subjectKind: String get() = rawSubjectKind ?: rawKind ?: "theory"
    val teacherId: Int get() = rawTeacherId ?: 0
    val teacherName: String get() = rawTeacherNames ?: rawTeacherName ?: "Faculty Not Assigned"
    val teacherNames: String get() = teacherName
    val teacherEmail: String? get() = rawTeacherEmail
    val notesCount: Int get() = rawNotesCount ?: 0
}

@Serializable
data class StaffDirectoryView(
    @SerialName("id") val id: Int,
    @SerialName("firebase_uid") val firebaseUid: String? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("email") val email: String? = null,
    @SerialName("phone_no") val phoneNo: String? = null,
    @SerialName("department") val department: String? = null,
    @SerialName("designation") val designation: String? = null,
    @SerialName("is_coordinator") val isCoordinator: Boolean = false,
    @SerialName("mentor_class_id") val mentorClassId: Int? = null,
)

@Serializable
data class ActiveTimetable(
    @SerialName("timetable_id") val timetableId: Int,
    @SerialName("class_id") val classId: Int,
    @SerialName("effective_from") val effectiveFrom: String,
    @SerialName("room") val room: String? = null,
)

@Serializable
data class TimetableEntryView(
    @SerialName("entry_id") val entryId: Int,
    @SerialName("timetable_id") val timetableId: Int,
    @SerialName("class_id") val classId: Int,
    @SerialName("effective_from") val effectiveFrom: String,
    @SerialName("is_published") val isPublished: Boolean,
    @SerialName("class_room") val classRoom: String? = null,
    @SerialName("day_of_week") val dayOfWeek: Int,
    @SerialName("day_name") val dayName: String? = null,
    @SerialName("start_period") val startPeriod: Int,
    @SerialName("end_period") val endPeriod: Int,
    @SerialName("start_time") val startTime: String? = null,
    @SerialName("end_time") val endTime: String? = null,
    @SerialName("subject_code") val subjectCode: String,
    @SerialName("subject_name") val subjectName: String,
    @SerialName("subject_kind") val subjectKind: String,
    @SerialName("group_id") val groupId: Int? = null,
    @SerialName("group_name") val groupName: String? = null,
    @SerialName("teacher_name") val teacherName: String? = null,
    @SerialName("room") val room: String? = null,
)

@Serializable
data class MyTimetableEntryView(
    @SerialName("entry_id") val entryId: Int,
    @SerialName("timetable_id") val timetableId: Int,
    @SerialName("class_id") val classId: Int,
    @SerialName("effective_from") val effectiveFrom: String,
    @SerialName("is_published") val isPublished: Boolean,
    @SerialName("class_room") val classRoom: String? = null,
    @SerialName("day_of_week") val dayOfWeek: Int,
    @SerialName("day_name") val dayName: String? = null,
    @SerialName("start_period") val startPeriod: Int,
    @SerialName("end_period") val endPeriod: Int,
    @SerialName("start_time") val startTime: String? = null,
    @SerialName("end_time") val endTime: String? = null,
    @SerialName("subject_code") val subjectCode: String,
    @SerialName("subject_name") val subjectName: String,
    @SerialName("subject_kind") val subjectKind: String,
    @SerialName("group_id") val groupId: Int? = null,
    @SerialName("group_name") val groupName: String? = null,
    @SerialName("teacher_name") val teacherName: String? = null,
    @SerialName("room") val room: String? = null,
)

// ---------------------------------------------------------------------------------------------
// RPC Payloads
// ---------------------------------------------------------------------------------------------

@Serializable
data class TakeAttendanceRecord(
    @SerialName("uid") val uid: String,
    @SerialName("status") val status: String, // 'P'|'A'|'L'|'OD'
)

@Serializable
data class NoteRow(
    @SerialName("id") val id: Int = 0,
    @SerialName("class_subject_id") val classSubjectId: Int,
    @SerialName("group_id") val groupId: Int? = null,
    @SerialName("title") val title: String,
    @SerialName("description") val description: String? = null,
    @SerialName("category") val category: String = "notes",
    @SerialName("drive_url") val driveUrl: String,
    @SerialName("teacher_id") val teacherId: Int,
    @SerialName("uploaded_by") val uploadedBy: String? = null,
    @SerialName("is_visible") val isVisible: Boolean = true,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

typealias StudentDirectoryView = StudentProfileView
typealias StudentSubjectView = ClassSubjectView
typealias SubjectTeacherView = StaffDirectoryView

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
        const val STAFF_DB = "staff_db"
        const val DEPARTMENTS = "departments"
        const val COURSES = "courses"
        const val BRANCHES = "branches"
        const val CLASSES = "classes"
        const val CLASS_GROUPS = "class_groups"
        const val SUBJECTS = "subjects"
        const val CLASS_SUBJECTS = "class_subjects"
        const val ENROLLMENTS = "enrollments"
        const val COURSE_HODS = "course_hods"
        const val COURSE_COORDINATORS = "course_coordinators"
        const val CLASS_SUBJECT_TEACHERS = "class_subject_teachers"
        const val CLASS_MENTORS = "class_mentors"
        const val TIMETABLES = "timetables"
        const val TIMETABLE_PERIODS = "timetable_periods"
        const val TIMETABLE_ENTRIES = "timetable_entries"
        const val ATTENDANCE_SESSIONS = "attendance_sessions"
        const val ATTENDANCE_RECORDS = "attendance_records"
        const val ASSESSMENTS = "assessments"
        const val MARKS = "marks"
        const val NOTES = "notes"
        const val APP_UPDATE = "app_update"
    }

    object Views {
        const val V_STUDENT_DIRECTORY = "v_student_directory"
        const val V_STUDENT_PROFILES = "v_student_profiles"
        const val V_STUDENT_SUBJECTS = "v_student_subjects"
        const val V_MY_SUBJECT_CARDS = "v_my_subject_cards"
        const val V_STAFF_DIRECTORY = "v_staff_directory"
        const val V_ATTENDANCE_SUMMARY = "v_attendance_summary"
        const val V_MARKS = "v_marks"
        const val V_CLASSES = "v_classes"
        const val V_CLASS_SUBJECTS = "v_class_subjects"
        const val V_SUBJECT_TEACHERS = "v_subject_teachers"
        const val V_NOTES = "v_notes"
        const val V_ACTIVE_TIMETABLES = "v_active_timetables"
        const val V_TIMETABLE_ENTRIES = "v_timetable_entries"
        const val V_MY_TIMETABLE = "v_my_timetable"
        const val V_FACULTY = "v_faculty"
        const val V_COORDINATORS = "v_coordinators"
        const val V_COURSE_HODS = "v_course_hods"
        const val V_CLASS_MENTORS = "v_class_mentors"
        const val V_MY_MENTOR = "v_my_mentor"
        const val V_MY_MENTEES = "v_my_mentees"
        const val V_MY_MENTEES_ATTENDANCE = "v_my_mentees_attendance"
        const val V_STUDENTS_WITHOUT_MENTOR = "v_students_without_mentor"
        const val V_MY_COORDINATOR_CLASSES = "v_my_coordinator_classes"
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
    /** Shown verbatim on the student's lock screen; null when unlocked or no reason was typed. */
    @SerialName("lock_reason") val lockReason: String? = null,
    /** Timestamptz of the scheduled PTM (ISO-8601), displayed on the student's lock screen. */
    @SerialName("ptm_schedule") val ptmSchedule: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)

@Serializable
data class FacultyMasterRow(
    @SerialName("id") val id: Int = 0,
    @SerialName("firebase_uid") val firebaseUid: String? = null,
    @SerialName("email") val email: String? = null,
    @SerialName("phone_no") val phoneNo: String? = null,
    @SerialName("title") val title: String? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("designation") val designation: String? = null,
    @SerialName("department_id") val departmentId: Int? = null,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("gender") val gender: String? = null,
    @SerialName("qualification") val qualification: String? = null,
    @SerialName("profile_url") val profileUrl: String? = null,
)

@Serializable
data class StaffDbRow(
    @SerialName("uid") val uid: String = "",
    @SerialName("email_id") val emailId: String? = null,
    @SerialName("department") val department: String? = null,
    @SerialName("role") val role: String? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("phone_no") val phoneNo: String? = null,
    @SerialName("gender") val gender: String? = null,
    @SerialName("designation") val designation: String? = null,
)

@Serializable
data class DepartmentRow(
    @SerialName("id") val id: Int = 0,
    @SerialName("code") val code: String = "",
    @SerialName("name") val name: String = "",
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
data class CourseHodRow(
    @SerialName("course_id") val courseId: Int,
    @SerialName("faculty_id") val facultyId: Int,
)

@Serializable
data class CourseCoordinatorRow(
    @SerialName("id") val id: Int = 0,
    @SerialName("course_id") val courseId: Int,
    @SerialName("faculty_id") val facultyId: Int,
    @SerialName("study_year") val studyYear: Int? = null,
    @SerialName("semester") val semester: Int? = null,
    @SerialName("is_tb_incharge") val isTbIncharge: Boolean = false,
    @SerialName("have_rights") val haveRights: Boolean = true,
    @SerialName("is_active") val isActive: Boolean = true,
)

@Serializable
data class ClassSubjectTeacherRow(
    @SerialName("class_subject_id") val classSubjectId: Int,
    @SerialName("faculty_id") val facultyId: Int,
)

@Serializable
data class ClassMentorRow(
    @SerialName("id") val id: Int = 0,
    @SerialName("faculty_id") val facultyId: Int,
    @SerialName("class_id") val classId: Int,
    @SerialName("group_id") val groupId: Int? = null,
    @SerialName("is_active") val isActive: Boolean = true,
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

/**
 * A student's mark joined with the session it belongs to. Backs the attendance calendar, which
 * needs the `class_date` per record rather than a session id.
 */
@Serializable
data class AttendanceRecordWithSessionRow(
    @SerialName("status") val status: String = "A",
    @SerialName("attendance_sessions") val session: AttendanceSessionDate? = null,
)

@Serializable
data class AttendanceSessionDate(
    @SerialName("class_date") val classDate: String? = null,
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

/**
 * Insert shape for `timetables`: omits `id` so PostgREST uses the identity sequence.
 */
@Serializable
data class NewTimetableRow(
    @SerialName("class_id") val classId: Int,
    @SerialName("effective_from") val effectiveFrom: String,
    @SerialName("room") val room: String? = null,
    @SerialName("is_published") val isPublished: Boolean = false,
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
    @SerialName("faculty_id") val facultyId: Int? = null,
    @SerialName("room") val room: String? = null,
)

/**
 * Insert shape for `timetable_entries`: it deliberately omits `id` so PostgREST lets the table's
 * identity column generate the next value instead of receiving a literal `0`.
 */
@Serializable
data class NewTimetableEntryRow(
    @SerialName("timetable_id") val timetableId: Int,
    @SerialName("day_of_week") val dayOfWeek: Int,
    @SerialName("start_period") val startPeriod: Int,
    @SerialName("end_period") val endPeriod: Int,
    @SerialName("class_subject_id") val classSubjectId: Int,
    @SerialName("group_id") val groupId: Int? = null,
    @SerialName("faculty_id") val facultyId: Int? = null,
    @SerialName("room") val room: String? = null,
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
    @SerialName("faculty_id") val facultyId: Int? = null,
    @SerialName("uploaded_by") val uploadedBy: String? = null,
    @SerialName("is_visible") val isVisible: Boolean = true,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

/** Omit faculty_id and uploaded_by on note inserts; the database fills them. */
@Serializable
data class NoteInsertRow(
    @SerialName("class_subject_id") val classSubjectId: Int,
    @SerialName("group_id") val groupId: Int? = null,
    @SerialName("title") val title: String,
    @SerialName("description") val description: String? = null,
    @SerialName("category") val category: String = "notes",
    @SerialName("drive_url") val driveUrl: String,
    @SerialName("is_visible") val isVisible: Boolean = true,
)

@Serializable
data class AppUpdateRow(
    @SerialName("version_code") val versionCode: Int = 0,
    @SerialName("version_name") val versionName: String = "",
    @SerialName("min_version_code") val minVersionCode: Int = 0,
    @SerialName("apk_url") val apkUrl: String? = null,
    @SerialName("changelog") val changelog: String? = null,
    @SerialName("is_mandatory") val isMandatory: Boolean = false,
)

// ---------------------------------------------------------------------------------------------
// Row shapes - Views & RPC Responses
// ---------------------------------------------------------------------------------------------

@Serializable
data class MyPortalsResponse(
    @SerialName("role") val role: String = "student",
    @SerialName("is_admin") val isAdmin: Boolean = false,
    @SerialName("is_student") val isStudent: Boolean = false,
    @SerialName("is_faculty") val isFaculty: Boolean = false,
    @SerialName("is_teacher") val isTeacher: Boolean = false,
    @SerialName("is_mentor") val isMentor: Boolean = false,
    @SerialName("is_coordinator") val isCoordinator: Boolean = false,
    @SerialName("is_hod") val isHod: Boolean = false,
    @SerialName("is_tb_incharge") val isTbIncharge: Boolean = false,
    @SerialName("faculty_id") val facultyId: Int? = null,
)

@Serializable
data class StudentProfileView(
    @SerialName("student_uid") private val _studentUid: String = "",
    @SerialName("uid") private val _uid: String = "",
    @SerialName("name") val name: String? = null,
    @SerialName("serial_no") val serialNo: String? = null,
    @SerialName("father_name") val fatherName: String? = null,
    @SerialName("student_phone_no") val studentPhoneNo: String? = null,
    @SerialName("father_phone_no") val fatherPhoneNo: String? = null,
    @SerialName("student_email") val studentEmail: String? = null,
    @SerialName("group_id") val groupId: Int? = null,
    @SerialName("group_name") val groupName: String? = null,
    @SerialName("is_locked") val isLocked: Boolean? = null,
    @SerialName("is_ptm") val isPtm: Boolean? = null,
    @SerialName("lock_reason") val lockReason: String? = null,
    @SerialName("ptm_schedule") val ptmSchedule: String? = null,
    @SerialName("course") val course: String? = null,
    @SerialName("branch") val branch: String? = null,
    @SerialName("semester") val semester: Int? = null,
    @SerialName("section") val section: String? = null,
    @SerialName("academic_year") val academicYear: String? = null,
    @SerialName("class_id") val classId: Int? = null,
    @SerialName("explicit_student_uid") private val _explicitStudentUid: String = "",
) {
    val studentUid: String
        get() = _explicitStudentUid.trim().ifEmpty {
            _studentUid.trim().ifEmpty {
                _uid.trim()
            }
        }

    val uid: String get() = studentUid

    constructor(
        studentUid: String,
        name: String? = null,
        serialNo: String? = null,
        fatherName: String? = null,
        studentPhoneNo: String? = null,
        fatherPhoneNo: String? = null,
        studentEmail: String? = null,
        groupId: Int? = null,
        groupName: String? = null,
        isLocked: Boolean? = null,
        isPtm: Boolean? = null,
        lockReason: String? = null,
        ptmSchedule: String? = null,
        course: String? = null,
        branch: String? = null,
        semester: Int? = null,
        section: String? = null,
        academicYear: String? = null,
        classId: Int? = null,
    ) : this(
        _studentUid = studentUid,
        _uid = studentUid,
        name = name,
        serialNo = serialNo,
        fatherName = fatherName,
        studentPhoneNo = studentPhoneNo,
        fatherPhoneNo = fatherPhoneNo,
        studentEmail = studentEmail,
        groupId = groupId,
        groupName = groupName,
        isLocked = isLocked,
        isPtm = isPtm,
        lockReason = lockReason,
        ptmSchedule = ptmSchedule,
        course = course,
        branch = branch,
        semester = semester,
        section = section,
        academicYear = academicYear,
        classId = classId,
        _explicitStudentUid = studentUid,
    )
}

@Serializable
data class MyCoordinatorClassView(
    @SerialName("class_id") val classId: Int,
    @SerialName("course_id") val courseId: Int,
    @SerialName("course") val course: String? = null,
    @SerialName("branch_id") val branchId: Int? = null,
    @SerialName("branch") val branch: String? = null,
    @SerialName("semester") val semester: Int,
    @SerialName("section") val section: String,
    @SerialName("academic_year") val academicYear: String? = null,
)

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

/**
 * One calendar day of a student's attendance, folded together from every marked period that day.
 * Built client-side from `attendance_sessions` + `attendance_records`; `date` is ISO `yyyy-MM-dd`.
 */
data class DailyAttendance(
    val date: String,
    val present: Int = 0,
    val absent: Int = 0,
    val leave: Int = 0,
    val onDuty: Int = 0,
) {
    /** Periods that count as attended (on-duty also counts as attended). */
    val attended: Int get() = present + onDuty

    /** Periods that count against the student. */
    val missed: Int get() = absent + leave

    /** True when the day is mostly attended, false when mostly missed, null when nothing marked. */
    val attendedDay: Boolean?
        get() = when {
            attended == 0 && missed == 0 -> null
            attended >= missed -> true
            else -> false
        }
}

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
    @SerialName("faculty_id") val rawFacultyId: Int? = null,
    @SerialName("teacher_id") val legacyTeacherId: Int? = null,
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
    val facultyId: Int get() = rawFacultyId ?: legacyTeacherId ?: 0
    val teacherId: Int get() = facultyId
    val teacherName: String get() = rawTeacherNames ?: rawTeacherName ?: "Faculty Not Assigned"
    val teacherNames: String get() = teacherName
    val teacherEmail: String? get() = rawTeacherEmail
    val notesCount: Int get() = rawNotesCount ?: 0
}

@Serializable
data class VNoteView(
    @SerialName("id") val id: Int,
    @SerialName("class_subject_id") val classSubjectId: Int,
    @SerialName("class_id") val classId: Int,
    @SerialName("subject_code") val subjectCode: String,
    @SerialName("subject_name") val subjectName: String,
    @SerialName("subject_kind") val subjectKind: String,
    @SerialName("group_id") val groupId: Int? = null,
    @SerialName("group_name") val groupName: String? = null,
    @SerialName("title") val title: String,
    @SerialName("description") val description: String? = null,
    @SerialName("category") val category: String = "notes",
    @SerialName("drive_url") val driveUrl: String,
    @SerialName("faculty_id") val facultyId: Int? = null,
    @SerialName("teacher_name") val teacherName: String? = null,
    @SerialName("uploaded_by") val uploadedBy: String? = null,
    @SerialName("is_visible") val isVisible: Boolean = true,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
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
    @SerialName("faculty_id") val facultyId: Int? = null,
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
    @SerialName("faculty_id") val facultyId: Int? = null,
    @SerialName("teacher_name") val teacherName: String? = null,
    @SerialName("room") val room: String? = null,
)

@Serializable
data class VFacultyView(
    @SerialName("id") val id: Int,
    @SerialName("title") val title: String? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("gender") val gender: String? = null,
    @SerialName("designation") val designation: String? = null,
    @SerialName("department_id") val departmentId: Int? = null,
    @SerialName("department_code") val departmentCode: String? = null,
    @SerialName("department") val department: String? = null,
    @SerialName("email") val email: String? = null,
    @SerialName("phone_no") val phoneNo: String? = null,
    @SerialName("profile_url") val profileUrl: String? = null,
    @SerialName("qualification") val qualification: String? = null,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("firebase_uid") val firebaseUid: String? = null,
)

@Serializable
data class StaffDirectoryView(
    @SerialName("faculty_id") val facultyId: Int,
    @SerialName("firebase_uid") val firebaseUid: String? = null,
    @SerialName("title") val title: String? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("email") val email: String? = null,
    @SerialName("phone_no") val phoneNo: String? = null,
    @SerialName("designation") val designation: String? = null,
    @SerialName("qualification") val qualification: String? = null,
    @SerialName("profile_url") val profileUrl: String? = null,
    @SerialName("department_id") val departmentId: Int? = null,
    @SerialName("department_code") val departmentCode: String? = null,
    @SerialName("department_name") val departmentName: String? = null,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("mentor_class_id") val mentorClassId: Int? = null,
    @SerialName("mentor_group_id") val mentorGroupId: Int? = null,
    @SerialName("is_coordinator") val isCoordinator: Boolean = false,
    @SerialName("is_hod") val isHod: Boolean = false,
)

@Serializable
data class CoordinatorView(
    @SerialName("id") val id: Int,
    @SerialName("faculty_id") val facultyId: Int,
    @SerialName("name") val name: String? = null,
    @SerialName("email") val email: String? = null,
    @SerialName("phone_no") val phoneNo: String? = null,
    @SerialName("designation") val designation: String? = null,
    @SerialName("department") val department: String? = null,
    @SerialName("profile_url") val profileUrl: String? = null,
    @SerialName("have_rights") val haveRights: Boolean = true,
    @SerialName("is_tbincharge") val isTbIncharge: Boolean = false,
    @SerialName("course_id") val courseId: Int,
    @SerialName("course") val course: String? = null,
    @SerialName("study_year") val studyYear: Int? = null,
    @SerialName("semester") val semester: Int? = null,
    @SerialName("role_label") val roleLabel: String? = null,
)

@Serializable
data class CourseHodView(
    @SerialName("course_id") val courseId: Int,
    @SerialName("course") val course: String? = null,
    @SerialName("faculty_id") val facultyId: Int,
    @SerialName("hod_name") val hodName: String? = null,
    @SerialName("designation") val designation: String? = null,
    @SerialName("email") val email: String? = null,
    @SerialName("phone_no") val phoneNo: String? = null,
    @SerialName("profile_url") val profileUrl: String? = null,
)

@Serializable
data class ClassMentorView(
    @SerialName("id") val id: Int,
    @SerialName("faculty_id") val facultyId: Int,
    @SerialName("mentor_title") val mentorTitle: String? = null,
    @SerialName("mentor_name") val mentorName: String? = null,
    @SerialName("designation") val designation: String? = null,
    @SerialName("email") val email: String? = null,
    @SerialName("phone_no") val phoneNo: String? = null,
    @SerialName("profile_url") val profileUrl: String? = null,
    @SerialName("class_id") val classId: Int,
    @SerialName("course") val course: String? = null,
    @SerialName("branch") val branch: String? = null,
    @SerialName("semester") val semester: Int,
    @SerialName("section") val section: String,
    @SerialName("academic_year") val academicYear: String? = null,
    @SerialName("group_id") val groupId: Int? = null,
    @SerialName("group_name") val groupName: String? = null,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("assigned_at") val assignedAt: String? = null,
    @SerialName("ended_at") val endedAt: String? = null,
)

@Serializable
data class MyMenteeView(
    @SerialName("student_uid") val studentUid: String,
    @SerialName("name") val name: String? = null,
    @SerialName("serial_no") val serialNo: String? = null,
    @SerialName("father_name") val fatherName: String? = null,
    @SerialName("student_phone_no") val studentPhoneNo: String? = null,
    @SerialName("father_phone_no") val fatherPhoneNo: String? = null,
    @SerialName("student_email") val studentEmail: String? = null,
    @SerialName("is_ptm") val isPtm: Boolean? = null,
    @SerialName("is_locked") val isLocked: Boolean? = null,
    @SerialName("class_id") val classId: Int,
    @SerialName("course") val course: String? = null,
    @SerialName("branch") val branch: String? = null,
    @SerialName("semester") val semester: Int,
    @SerialName("section") val section: String,
    @SerialName("academic_year") val academicYear: String? = null,
    @SerialName("group_id") val groupId: Int? = null,
    @SerialName("group_name") val groupName: String? = null,
    @SerialName("class_mentor_id") val classMentorId: Int? = null,
)

@Serializable
data class MyMenteeAttendanceView(
    @SerialName("student_uid") val studentUid: String,
    @SerialName("name") val name: String? = null,
    @SerialName("serial_no") val serialNo: String? = null,
    @SerialName("class_id") val classId: Int,
    @SerialName("group_id") val groupId: Int? = null,
    @SerialName("group_name") val groupName: String? = null,
    @SerialName("attended") val attended: Long,
    @SerialName("total") val total: Long,
    @SerialName("percentage") val percentage: Double,
)

// ---------------------------------------------------------------------------------------------
// RPC Payloads
// ---------------------------------------------------------------------------------------------

@Serializable
data class TakeAttendanceRecord(
    @SerialName("uid") val uid: String,
    @SerialName("status") val status: String, // 'P'|'A'|'L'|'OD'
)

typealias StudentDirectoryView = StudentProfileView
typealias StudentSubjectView = ClassSubjectView
typealias SubjectTeacherView = StaffDirectoryView

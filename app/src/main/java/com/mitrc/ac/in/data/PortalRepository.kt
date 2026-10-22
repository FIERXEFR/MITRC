package com.mitrc.ac.`in`.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.query.PostgrestUpdate
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.postgrest.rpc
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.put

/**
 * The coordinator a locked/PTM student should contact. Resolved from `v_coordinators` for the
 * student's course (see [PortalRepository.getStudentCoordinator]) since `students` only stores the
 * lock flags, not who flipped them.
 */
data class StudentCoordinatorContact(
    val name: String? = null,
    val designation: String? = null,
    val email: String? = null,
    val phoneNo: String? = null,
)

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
            var prof = fromView.getOrThrow()!!
            if (prof.name.isNullOrBlank()) {
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
                    prof = prof.copy(name = tabledName)
                }
            }
            Log.i(TAG, "Student profile loaded from v_student_profiles (classId=${prof.classId})")
            return@query withStudentLockState(prof)
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
                lockReason = fromStudentTable.lockReason,
                ptmSchedule = fromStudentTable.ptmSchedule,
                classId = enrollment?.classId
            )
        }

        null
    }

    /**
     * `v_student_profiles` predates `lock_reason` / `ptm_schedule`, and a view is not rebuilt in
     * lockstep with its table, so the authoritative `students` row is overlaid here. Without this
     * the lock screen could keep showing a stale state (or no reason and no PTM time at all).
     * A blocked/failed read keeps whatever the view already returned.
     */
    private suspend fun withStudentLockState(profile: StudentProfileView): StudentProfileView {
        if (profile.studentUid.isBlank()) return profile
        val row = runCatching {
            SupabaseManager.requireClient().postgrest[SupabaseTableData.Tables.STUDENTS]
                .select { filter { eq("uid", profile.studentUid) } }
                .decodeList<StudentRow>()
                .firstOrNull()
        }.getOrNull() ?: return profile

        return profile.copy(
            isLocked = row.isLocked ?: profile.isLocked,
            isPtm = row.isPtm ?: profile.isPtm,
            lockReason = row.lockReason ?: profile.lockReason,
            ptmSchedule = row.ptmSchedule ?: profile.ptmSchedule
        )
    }

    suspend fun getStudentSubjects(classId: Int): Result<List<ClassSubjectView>> = query {
        if (classId <= 0) {
            Log.w(TAG, "getStudentSubjects: student has no class yet (classId=$classId); skipping subject query.")
            return@query emptyList()
        }

        val client = SupabaseManager.requireClient()

        // Fast path: the joined view already carries subject + faculty names.
        val fromView = runCatching {
            client.postgrest[SupabaseTableData.Views.V_CLASS_SUBJECTS]
                .select { filter { eq("class_id", classId) } }
                .decodeList<ClassSubjectView>()
        }.getOrDefault(emptyList())

        if (fromView.isNotEmpty()) {
            Log.i(TAG, "getStudentSubjects: ${fromView.size} subjects from v_class_subjects (class $classId)")
            return@query fromView
        }

        // On deployments where the view runs as security_invoker it returns nothing for a
        // student, so build the same cards from the base tables the student can read.
        val fromTables = subjectsFromBaseTables(classId)
        Log.i(TAG, "getStudentSubjects: ${fromTables.size} subjects from base tables (class $classId)")
        fromTables
    }

    /**
     * `class_subjects` -> `subjects` -> `class_subject_teachers` -> `faculty_master`, i.e. the
     * actual faculty assigned to this class, not a placeholder. A subject whose teacher row is
     * missing is still returned with "Faculty Not Assigned" rather than dropped.
     */
    private suspend fun subjectsFromBaseTables(classId: Int): List<ClassSubjectView> {
        val client = SupabaseManager.requireClient()

        val classSubjects = runCatching {
            client.postgrest[SupabaseTableData.Tables.CLASS_SUBJECTS]
                .select { filter { eq("class_id", classId) } }
                .decodeList<ClassSubjectRow>()
        }.getOrDefault(emptyList())
        if (classSubjects.isEmpty()) return emptyList()

        val subjectIds = classSubjects.map { it.subjectId }.distinct()
        val subjects = runCatching {
            client.postgrest[SupabaseTableData.Tables.SUBJECTS]
                .select { filter { isIn("id", subjectIds) } }
                .decodeList<SubjectRow>()
                .associateBy { it.id }
        }.getOrDefault(emptyMap())

        val classSubjectIds = classSubjects.map { it.id }
        val links = runCatching {
            client.postgrest[SupabaseTableData.Tables.CLASS_SUBJECT_TEACHERS]
                .select { filter { isIn("class_subject_id", classSubjectIds) } }
                .decodeList<ClassSubjectTeacherRow>()
        }.getOrDefault(emptyList())

        val facultyIds = links.map { it.facultyId }.distinct()
        val faculty = if (facultyIds.isEmpty()) emptyMap() else runCatching {
            client.postgrest[SupabaseTableData.Tables.FACULTY_MASTER]
                .select { filter { isIn("id", facultyIds) } }
                .decodeList<FacultyMasterRow>()
                .associateBy { it.id }
        }.getOrDefault(emptyMap())

        val teachersByClassSubject = links
            .groupBy { it.classSubjectId }
            .mapValues { (_, rows) -> rows.mapNotNull { faculty[it.facultyId] }.distinctBy { it.id } }

        return classSubjects.mapNotNull { classSubject ->
            val subject = subjects[classSubject.subjectId] ?: return@mapNotNull null
            val teachers = teachersByClassSubject[classSubject.id].orEmpty()
            val teacherNames = teachers.mapNotNull { it.displayName() }.joinToString(", ")

            ClassSubjectView(
                rawClassSubjectId = classSubject.id,
                rawClassId = classId,
                rawSubjectCode = subject.code,
                rawSubjectName = subject.name,
                rawSubjectKind = subject.kind,
                rawFacultyId = teachers.firstOrNull()?.id,
                rawTeacherName = teacherNames.ifBlank { null },
                rawTeacherEmail = teachers.firstOrNull { !it.email.isNullOrBlank() }?.email
            )
        }
    }

    /** "Dr." + "Amit Kumar Yadav" -> "Dr. Amit Kumar Yadav". */
    private fun FacultyMasterRow.displayName(): String? =
        listOfNotNull(
            title?.trim()?.takeIf { it.isNotEmpty() },
            name?.trim()?.takeIf { it.isNotEmpty() }
        ).joinToString(" ").takeIf { it.isNotEmpty() }

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

    /**
     * Per-day attendance behind the student calendar.
     *
     * One request: `attendance_records` embedded with its session so each mark carries the
     * `class_date` it belongs to. Filtering on the embedded date keeps the payload to the window
     * shown by the calendar instead of every record the student has ever had.
     *
     * The recorded sessions are optional for a student deployment. If the embed is not readable,
     * this logs and returns an empty list so the calendar falls back to its holiday-only view.
     */
    suspend fun getStudentDailyAttendance(
        uid: String,
        fromIsoDate: String,
        toIsoDate: String,
    ): Result<List<DailyAttendance>> = query {
        if (uid.isBlank()) return@query emptyList()

        val records = runCatching {
            SupabaseManager.requireClient().postgrest[SupabaseTableData.Tables.ATTENDANCE_RECORDS]
                .select(columns = Columns.raw("status,attendance_sessions!inner(class_date)")) {
                    filter {
                        eq("student_uid", uid)
                        gte("attendance_sessions.class_date", fromIsoDate)
                        lte("attendance_sessions.class_date", toIsoDate)
                    }
                }
                .decodeList<AttendanceRecordWithSessionRow>()
        }.getOrElse {
            Log.w(TAG, "getStudentDailyAttendance unreadable (${it.message})")
            return@query emptyList()
        }

        records
            .mapNotNull { record ->
                val date = record.session?.classDate?.takeIf { it.isNotBlank() }
                    ?: return@mapNotNull null
                date to record.status.trim().uppercase()
            }
            .groupBy({ it.first }, { it.second })
            .map { (date, statuses) ->
                DailyAttendance(
                    date = date,
                    present = statuses.count { it == "P" },
                    absent = statuses.count { it == "A" },
                    leave = statuses.count { it == "L" },
                    onDuty = statuses.count { it == "OD" }
                )
            }
            .sortedBy { it.date }
    }

    /**
     * The student's live timetable, synced from the timetable data itself.
     *
     * Three read paths are tried in order, because the raw timetable tables are RLS-protected in
     * most deployments and only the views are granted to a student:
     *
     * 1. `v_my_timetable` - the server-side student view (fast path).
     * 2. `v_timetable_entries` filtered by the student's class - the joined view the timetable
     *    editor reads, so it also works when `timetables` / `timetable_entries` are not readable.
     * 3. The base tables: class -> newest published/effective `timetables` row ->
     *    `timetable_periods` -> `timetable_entries` joined with `class_subjects`, `subjects`,
     *    `faculty_master` and `class_groups`.
     *
     * The student's class is the exact class of their course/branch/semester/section, so the
     * result is scoped to that course rather than to every class of the course. Lab slots are
     * filtered to the student's own lab group.
     */
    suspend fun getMyTimetable(classId: Int? = null): Result<List<MyTimetableEntryView>> = query {
        val client = SupabaseManager.requireClient()
        val uid = FirebaseAuth.getInstance().currentUser?.uid?.trim().orEmpty()

        val fromView = runCatching {
            client.postgrest[SupabaseTableData.Views.V_MY_TIMETABLE]
                .select()
                .decodeList<MyTimetableEntryView>()
        }.getOrElse {
            Log.w(TAG, "v_my_timetable query failed: ${it.message}")
            emptyList()
        }
        if (fromView.isNotEmpty()) {
            Log.i(TAG, "getMyTimetable: ${fromView.size} entries from v_my_timetable")
            return@query fromView
        }

        val context = resolveStudentClassContext(uid, classId)
        if (context == null) {
            Log.w(TAG, "getMyTimetable: could not resolve the student's class")
            return@query emptyList()
        }
        Log.i(TAG, "getMyTimetable: class_id=${context.classId}, group_id=${context.groupId}")

        // The joined view carries subject, teacher and clock times in one row.
        suspend fun viewEntriesFor(classIds: List<Int>): List<TimetableEntryView> = runCatching {
            client.postgrest[SupabaseTableData.Views.V_TIMETABLE_ENTRIES]
                .select { filter { isIn("class_id", classIds) } }
                .decodeList<TimetableEntryView>()
        }.getOrElse {
            Log.w(TAG, "v_timetable_entries query failed: ${it.message}")
            emptyList()
        }

        var viewEntries = viewEntriesFor(listOf(context.classId))
        if (viewEntries.isEmpty()) {
            // No rows for the student's own class: check the other sections of the same course +
            // semester, so a timetable keyed by course still resolves instead of coming up empty.
            val siblings = siblingClassIds(context.classId)
            if (siblings.isNotEmpty()) {
                viewEntries = viewEntriesFor(siblings)
            }
        }

        val ownClassEntries = viewEntries.filter { it.classId == context.classId }
        val pool = ownClassEntries.ifEmpty { viewEntries }

        val visibleViewEntries = pool.filter { entry ->
            entry.groupId == null || context.groupId == null || entry.groupId == context.groupId
        }
        val effectiveViewEntries = effectiveTimetableEntries(visibleViewEntries)
        if (effectiveViewEntries.isNotEmpty()) {
            Log.i(TAG, "getMyTimetable: ${effectiveViewEntries.size} entries from v_timetable_entries")
            return@query effectiveViewEntries.map { it.toMyTimetableEntry() }
        }

        val derived = deriveMyTimetable(context)
        Log.i(TAG, "getMyTimetable: ${derived.size} entries derived from the timetable tables")
        derived
    }

    /** Same rule on the joined view: effective + published first, then newest, then any. */
    private fun effectiveTimetableEntries(entries: List<TimetableEntryView>): List<TimetableEntryView> {
        if (entries.isEmpty()) return emptyList()
        val today = java.time.LocalDate.now().toString()
        val chosen = entries.groupBy { it.timetableId }.values
            .sortedWith(
                compareByDescending<List<TimetableEntryView>> {
                    it.first().isPublished && it.first().effectiveFrom <= today
                }
                    .thenByDescending { it.first().isPublished }
                    .thenByDescending { it.first().effectiveFrom }
            )
            .firstOrNull()
            .orEmpty()
        return chosen.sortedWith(compareBy({ it.dayOfWeek }, { it.startPeriod }))
    }

    private fun TimetableEntryView.toMyTimetableEntry(): MyTimetableEntryView = MyTimetableEntryView(
        entryId = entryId,
        timetableId = timetableId,
        classId = classId,
        effectiveFrom = effectiveFrom,
        isPublished = isPublished,
        classRoom = classRoom,
        dayOfWeek = dayOfWeek,
        dayName = dayName,
        startPeriod = startPeriod,
        endPeriod = endPeriod,
        startTime = startTime,
        endTime = endTime,
        subjectCode = subjectCode,
        subjectName = subjectName,
        subjectKind = subjectKind,
        groupId = groupId,
        groupName = groupName,
        facultyId = facultyId,
        teacherName = teacherName,
        room = room
    )

    /** Student's class + lab group. The class from the loaded profile wins when available. */
    private data class StudentClassContext(val classId: Int, val groupId: Int?)

    private suspend fun resolveStudentClassContext(
        uid: String,
        knownClassId: Int? = null
    ): StudentClassContext? {
        val client = SupabaseManager.requireClient()

        val enrollment = if (uid.isEmpty()) null else runCatching {
            client.postgrest[SupabaseTableData.Tables.ENROLLMENTS]
                .select { filter { eq("student_uid", uid) } }
                .decodeList<EnrollmentRow>()
                .maxByOrNull { it.enrolledAt.orEmpty() }
        }.getOrNull()

        val classId = knownClassId?.takeIf { it > 0 }
            ?: enrollment?.classId?.takeIf { it > 0 }
            ?: if (uid.isEmpty()) null else runCatching {
                client.postgrest[SupabaseTableData.Views.V_STUDENT_PROFILES]
                    .select { filter { eq("student_uid", uid) } }
                    .decodeList<StudentProfileView>()
                    .firstOrNull()
                    ?.classId
            }.getOrNull()

        if (classId == null || classId <= 0) return null
        return StudentClassContext(classId, enrollment?.groupId)
    }

    /**
     * Builds [MyTimetableEntryView] rows from `timetables` / `timetable_periods` /
     * `timetable_entries`. Returns empty (never throws) when the student has no readable
     * timetable, which lets the UI keep its empty state instead of failing the whole portal.
     */
    private suspend fun deriveMyTimetable(context: StudentClassContext): List<MyTimetableEntryView> {
        val client = SupabaseManager.requireClient()
        Log.i(TAG, "getMyTimetable: deriving for class_id=${context.classId}, group_id=${context.groupId}")

        suspend fun timetablesFor(classIds: List<Int>): List<TimetableRow> = runCatching {
            client.postgrest[SupabaseTableData.Tables.TIMETABLES]
                .select {
                    filter { isIn("class_id", classIds) }
                    order("effective_from", Order.DESCENDING)
                }
                .decodeList<TimetableRow>()
        }.getOrElse {
            Log.w(TAG, "timetables query failed: ${it.message}")
            emptyList()
        }

        // The student's own class wins. Only when it has no timetable at all do we look at the
        // other sections of the same course + semester, so a timetable keyed by the course still
        // reaches the student instead of an empty screen.
        var forClass = timetablesFor(listOf(context.classId))
        if (forClass.isEmpty()) {
            val siblings = siblingClassIds(context.classId)
            if (siblings.isNotEmpty()) {
                Log.i(TAG, "getMyTimetable: no timetable for class ${context.classId}, trying ${siblings.size} sibling class(es)")
                forClass = timetablesFor(siblings)
            }
        }

        val ownClass = forClass.filter { it.classId == context.classId }
        val pool = ownClass.ifEmpty { forClass }
        if (pool.isEmpty()) return emptyList()

        // Effective + published first, then newest published, then whatever exists (a draft).
        val today = java.time.LocalDate.now().toString()
        val published = pool.filter { it.isPublished }
        val timetable = published.firstOrNull { it.effectiveFrom <= today }
            ?: published.firstOrNull()
            ?: pool.firstOrNull()
            ?: return emptyList()

        // The joined view already carries subject, teacher and clock times for this timetable.
        val joined = runCatching {
            client.postgrest[SupabaseTableData.Views.V_TIMETABLE_ENTRIES]
                .select { filter { eq("timetable_id", timetable.id) } }
                .decodeList<TimetableEntryView>()
        }.getOrDefault(emptyList())
            .filter { entry ->
                entry.groupId == null || context.groupId == null || entry.groupId == context.groupId
            }
        if (joined.isNotEmpty()) {
            Log.i(TAG, "getMyTimetable: timetable=${timetable.id} via v_timetable_entries (${joined.size})")
            return joined
                .sortedWith(compareBy({ it.dayOfWeek }, { it.startPeriod }))
                .map { it.toMyTimetableEntry() }
        }

        val periods = runCatching {
            client.postgrest[SupabaseTableData.Tables.TIMETABLE_PERIODS]
                .select { filter { eq("timetable_id", timetable.id) } }
                .decodeList<TimetablePeriodRow>()
                .associateBy { it.period }
        }.getOrDefault(emptyMap())

        val entries = runCatching {
            client.postgrest[SupabaseTableData.Tables.TIMETABLE_ENTRIES]
                .select { filter { eq("timetable_id", timetable.id) } }
                .decodeList<TimetableEntryRow>()
        }.getOrElse {
            Log.w(TAG, "timetable_entries query failed: ${it.message}")
            emptyList()
        }

        // A lab slot is scheduled per group; only the student's group (or shared slots) applies.
        val scopedEntries = entries.filter { entry ->
            entry.groupId == null || context.groupId == null || entry.groupId == context.groupId
        }
        if (scopedEntries.isEmpty()) return emptyList()

        val classSubjects = runCatching {
            client.postgrest[SupabaseTableData.Tables.CLASS_SUBJECTS]
                .select { filter { eq("class_id", timetable.classId) } }
                .decodeList<ClassSubjectRow>()
                .associateBy { it.id }
        }.getOrDefault(emptyMap())

        val subjectIds = classSubjects.values.map { it.subjectId }.distinct()
        val subjects = if (subjectIds.isEmpty()) emptyMap() else runCatching {
            client.postgrest[SupabaseTableData.Tables.SUBJECTS]
                .select { filter { isIn("id", subjectIds) } }
                .decodeList<SubjectRow>()
                .associateBy { it.id }
        }.getOrDefault(emptyMap())

        val facultyIds = scopedEntries.mapNotNull { it.facultyId }.distinct()
        val faculty = if (facultyIds.isEmpty()) emptyMap() else runCatching {
            client.postgrest[SupabaseTableData.Tables.FACULTY_MASTER]
                .select { filter { isIn("id", facultyIds) } }
                .decodeList<FacultyMasterRow>()
                .associateBy { it.id }
        }.getOrDefault(emptyMap())

        val groups = runCatching {
            client.postgrest[SupabaseTableData.Tables.CLASS_GROUPS]
                .select { filter { eq("class_id", timetable.classId) } }
                .decodeList<ClassGroupRow>()
                .associateBy { it.id }
        }.getOrDefault(emptyMap())

        val dayNames = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

        Log.i(
            TAG,
            "getMyTimetable: timetable=${timetable.id} (class ${timetable.classId}, from " +
                "${timetable.effectiveFrom}), entries=${entries.size}, visible=${scopedEntries.size}"
        )

        return scopedEntries.map { entry ->
            val subject = classSubjects[entry.classSubjectId]?.let { subjects[it.subjectId] }
            val startPeriod = periods[entry.startPeriod]
            val endPeriod = periods[entry.endPeriod]
            val teacher = entry.facultyId?.let { id ->
                faculty[id]?.let { row ->
                    listOfNotNull(
                        row.title?.trim()?.takeIf { it.isNotEmpty() },
                        row.name?.trim()?.takeIf { it.isNotEmpty() }
                    ).joinToString(" ").takeIf { it.isNotEmpty() }
                }
            }

            MyTimetableEntryView(
                entryId = entry.id,
                timetableId = timetable.id,
                classId = timetable.classId,
                effectiveFrom = timetable.effectiveFrom,
                isPublished = timetable.isPublished,
                classRoom = timetable.room,
                dayOfWeek = entry.dayOfWeek,
                dayName = dayNames.getOrNull(entry.dayOfWeek - 1),
                startPeriod = entry.startPeriod,
                endPeriod = entry.endPeriod,
                startTime = startPeriod?.startTime,
                endTime = endPeriod?.endTime,
                subjectCode = subject?.code?.takeIf { it.isNotBlank() } ?: "SUB",
                subjectName = subject?.name?.takeIf { it.isNotBlank() } ?: "Subject",
                subjectKind = subject?.kind?.takeIf { it.isNotBlank() } ?: "theory",
                groupId = entry.groupId,
                groupName = entry.groupId?.let { groups[it]?.name },
                facultyId = entry.facultyId,
                teacherName = teacher,
                room = entry.room?.takeIf { it.isNotBlank() } ?: timetable.room
            )
        }
    }

    /**
     * Other classes of the same course + semester as [classId] (class -> branch -> course).
     * Best-effort: an unreadable `classes`/`branches` yields an empty list.
     */
    private suspend fun siblingClassIds(classId: Int): List<Int> {
        val client = SupabaseManager.requireClient()

        val cls = runCatching {
            client.postgrest[SupabaseTableData.Tables.CLASSES]
                .select { filter { eq("id", classId) } }
                .decodeList<ClassRow>()
                .firstOrNull()
        }.getOrNull() ?: return emptyList()

        val branch = runCatching {
            client.postgrest[SupabaseTableData.Tables.BRANCHES]
                .select { filter { eq("id", cls.branchId) } }
                .decodeList<BranchRow>()
                .firstOrNull()
        }.getOrNull() ?: return emptyList()

        val courseBranches = runCatching {
            client.postgrest[SupabaseTableData.Tables.BRANCHES]
                .select { filter { eq("course_id", branch.courseId) } }
                .decodeList<BranchRow>()
        }.getOrDefault(emptyList())

        val branchIds = courseBranches.map { it.id }.ifEmpty { listOf(branch.id) }
        return runCatching {
            client.postgrest[SupabaseTableData.Tables.CLASSES]
                .select {
                    filter {
                        isIn("branch_id", branchIds)
                        eq("semester", cls.semester)
                    }
                }
                .decodeList<ClassRow>()
                .map { it.id }
                .filter { it != classId }
        }.getOrDefault(emptyList())
    }

    /**
     * Notes this student is allowed to open: visible rows for a subject of their own class, and
     * either shared with the whole class (`group_id` null) or scoped to their lab group.
     *
     * Reads the base `notes` table rather than `v_notes`, because the view is security_invoker in
     * this deployment and returns nothing for a student. The class-scoped subject ids double as
     * the access check on the client, on top of the table's RLS policy.
     */
    suspend fun getPublishedNotes(
        uid: String,
        classId: Int? = null
    ): Result<List<NoteRow>> = query {
        val client = SupabaseManager.requireClient()
        val context = resolveStudentClassContext(uid, classId) ?: return@query emptyList()

        val classSubjectIds = runCatching {
            client.postgrest[SupabaseTableData.Tables.CLASS_SUBJECTS]
                .select { filter { eq("class_id", context.classId) } }
                .decodeList<ClassSubjectRow>()
                .map { it.id }
        }.getOrDefault(emptyList())
        if (classSubjectIds.isEmpty()) return@query emptyList()

        val rows = runCatching {
            client.postgrest[SupabaseTableData.Tables.NOTES]
                .select {
                    filter {
                        eq("is_visible", true)
                        isIn("class_subject_id", classSubjectIds)
                    }
                    order("created_at", Order.DESCENDING)
                }
                .decodeList<NoteRow>()
        }.getOrDefault(emptyList())

        val visible = rows.filter { it.groupId == null || it.groupId == context.groupId }
        Log.i(
            TAG,
            "getPublishedNotes: ${visible.size} of ${rows.size} notes visible for class " +
                "${context.classId}, group ${context.groupId}"
        )
        visible
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
        val identities = fetchStudentIdentities(rows.map { it.studentUid })
        rows.map { overlayIdentity(it, identities[it.studentUid]) }
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

    /**
     * Classes the signed-in coordinator is responsible for.
     *
     * `v_my_coordinator_classes` is the authoritative source, but it resolves the caller
     * server-side and can come back empty when that mapping is stale (for example when
     * `faculty_master.firebase_uid` has not been linked yet, or the view is keyed on
     * `auth.uid()`). In that case the assignment is rebuilt client-side from
     * `my_portals.faculty_id` -> `course_coordinators` -> `branches` -> `classes`, so the portal
     * keeps working without any database change.
     */
    suspend fun getMyCoordinatorClasses(): Result<List<MyCoordinatorClassView>> = query {
        val client = SupabaseManager.requireClient()
        val fromView = runCatching {
            client.postgrest[SupabaseTableData.Views.V_MY_COORDINATOR_CLASSES]
                .select()
                .decodeList<MyCoordinatorClassView>()
        }
        val viewRows = fromView.getOrNull().orEmpty()
        if (viewRows.isNotEmpty()) return@query viewRows

        val derived = runCatching { deriveCoordinatorClasses() }.getOrElse {
            Log.w(TAG, "coordinator class derivation failed: ${it.message}")
            emptyList()
        }
        Log.d(TAG, "getMyCoordinatorClasses: view=${viewRows.size} derived=${derived.size}")
        if (derived.isNotEmpty()) derived else fromView.getOrElse { throw it }
    }

    /** Rebuilds the coordinator's class list from the underlying assignment tables. */
    private suspend fun deriveCoordinatorClasses(): List<MyCoordinatorClassView> {
        val client = SupabaseManager.requireClient()
        val facultyId = resolveFacultyId() ?: return emptyList()
        Log.d(TAG, "deriveCoordinatorClasses: faculty_id=$facultyId")

        var assignments: List<Pair<Int, Int?>> = runCatching {
            client.postgrest[SupabaseTableData.Tables.COURSE_COORDINATORS]
                .select { filter { eq("faculty_id", facultyId) } }
                .decodeList<CourseCoordinatorRow>()
                .map { it.courseId to it.semester }
        }.getOrElse {
            Log.w(TAG, "course_coordinators query failed: ${it.message}")
            emptyList()
        }

        // Retry through the coordinator view (faculty id, then email) when the table read is
        // blocked or the assignment row was never linked to this faculty id.
        if (assignments.isEmpty()) {
            val email = FirebaseAuth.getInstance().currentUser?.email.orEmpty()
            assignments = runCatching {
                client.postgrest[SupabaseTableData.Views.V_COORDINATORS]
                    .select()
                    .decodeList<CoordinatorView>()
                    .filter {
                        it.facultyId == facultyId ||
                            (email.isNotBlank() && it.email.equals(email, ignoreCase = true))
                    }
                    .map { it.courseId to it.semester }
            }.getOrElse {
                Log.w(TAG, "v_coordinators fallback failed: ${it.message}")
                emptyList()
            }
        }
        if (assignments.isEmpty()) return emptyList()

        val courses = runCatching {
            client.postgrest[SupabaseTableData.Tables.COURSES]
                .select()
                .decodeList<CourseRow>()
        }.getOrDefault(emptyList()).associateBy { it.id }
        val branches = runCatching {
            client.postgrest[SupabaseTableData.Tables.BRANCHES]
                .select()
                .decodeList<BranchRow>()
        }.getOrDefault(emptyList())
        val classes = runCatching {
            client.postgrest[SupabaseTableData.Tables.CLASSES]
                .select()
                .decodeList<ClassRow>()
        }.getOrElse {
            Log.w(TAG, "classes table query failed, trying v_classes: ${it.message}")
            runCatching {
                client.postgrest[SupabaseTableData.Views.V_CLASSES]
                    .select()
                    .decodeList<ClassRow>()
            }.getOrDefault(emptyList())
        }
        val branchesByCourse = branches.groupBy { it.courseId }

        return assignments.flatMap { (courseId, semester) ->
            (branchesByCourse[courseId] ?: emptyList()).flatMap { branch ->
                classes
                    .filter {
                        it.branchId == branch.id &&
                            (semester == null || it.semester == semester)
                    }
                    .map { cls ->
                        MyCoordinatorClassView(
                            classId = cls.id,
                            courseId = courseId,
                            course = courses[courseId]?.name,
                            branchId = branch.id,
                            branch = branch.name,
                            semester = cls.semester,
                            section = cls.section,
                            academicYear = cls.academicYear
                        )
                    }
            }
        }.distinctBy { it.classId }
    }

    /**
     * Faculty row of the signed-in staff account. `my_portals.faculty_id` is preferred; when the
     * RPC has no row (for example the account was only matched by email), `faculty_master` is
     * looked up by firebase_uid and then by email.
     */
    private suspend fun resolveFacultyId(): Int? {
        val portals = getMyPortals().getOrNull()
        portals?.facultyId?.let { return it }

        // A pure student has no faculty row; skip the extra lookups on that path.
        if (portals != null &&
            !portals.isFaculty && !portals.isTeacher && !portals.isMentor &&
            !portals.isCoordinator && !portals.isHod && !portals.isTbIncharge
        ) {
            return null
        }

        val user = FirebaseAuth.getInstance().currentUser ?: return null
        val email = user.email.orEmpty()
        if (user.uid.isBlank() && email.isBlank()) return null
        return getFacultyByUidOrEmail(user.uid, email).getOrNull()?.id
    }

    suspend fun getCoordinatorsList(): Result<List<CoordinatorView>> = query {
        SupabaseManager.requireClient().postgrest[SupabaseTableData.Views.V_COORDINATORS]
            .select()
            .decodeList<CoordinatorView>()
    }

    /**
     * Coordinator to name on the student's lock / PTM screen, resolved from the student's own
     * class (class -> branch -> course) and matched against `v_coordinators`. Only rows with
     * `have_rights = true` qualify; a semester-specific coordinator wins over a course-wide one.
     *
     * Best effort: if the view or the class mapping is not readable by a student account this
     * returns null and the lock screen falls back to a generic "your class coordinator".
     */
    suspend fun getStudentCoordinator(
        classId: Int?,
        courseName: String? = null,
        semester: Int? = null
    ): Result<StudentCoordinatorContact?> = query {
        val coordinators = runCatching {
            SupabaseManager.requireClient().postgrest[SupabaseTableData.Views.V_COORDINATORS]
                .select()
                .decodeList<CoordinatorView>()
        }.getOrElse {
            Log.w(TAG, "v_coordinators query failed for the lock screen: ${it.message}")
            return@query null
        }.filter { it.haveRights }

        if (coordinators.isEmpty()) return@query null

        val courseId = resolveCourseId(classId)
        // Match on the id first; the course name is only a fallback for accounts that cannot read
        // `classes`/`branches`.
        val scoped = coordinators.filter { row ->
            when {
                courseId != null -> row.courseId == courseId
                !courseName.isNullOrBlank() -> row.course.equals(courseName.trim(), ignoreCase = true)
                else -> false
            }
        }
        if (scoped.isEmpty()) return@query null

        val match = scoped.firstOrNull { semester != null && it.semester == semester } ?: scoped.first()
        StudentCoordinatorContact(
            name = match.name?.trim()?.takeIf { it.isNotEmpty() },
            designation = match.designation?.trim()?.takeIf { it.isNotEmpty() },
            email = match.email?.trim()?.takeIf { it.isNotEmpty() },
            phoneNo = match.phoneNo?.trim()?.takeIf { it.isNotEmpty() }
        )
    }

    /** `classes.branch_id` -> `branches.course_id`, used to scope coordinator lookups. */
    private suspend fun resolveCourseId(classId: Int?): Int? {
        val id = classId ?: return null
        if (id <= 0) return null
        val client = SupabaseManager.requireClient()

        val klass = runCatching {
            client.postgrest[SupabaseTableData.Tables.CLASSES]
                .select { filter { eq("id", id) } }
                .decodeList<ClassRow>()
                .firstOrNull()
        }.getOrNull() ?: return null

        return runCatching {
            client.postgrest[SupabaseTableData.Tables.BRANCHES]
                .select { filter { eq("id", klass.branchId) } }
                .decodeList<BranchRow>()
                .firstOrNull()
                ?.courseId
        }.getOrNull()
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
     * Query students across the coordinator's classes.
     *
     * Membership is resolved from several sources and unioned, so one restricted source cannot
     * blank the list:
     * - `v_student_directory` provides the class/semester/section mapping and the person's name;
     * - `v_student_profiles` backs it up when the directory view is not readable by faculty;
     * - `enrollments` provides the authoritative uid ↔ class_id mapping;
     * - `students` is authoritative for identity fields (name, roll number, emails, phone
     *   numbers, lab group, lock/PTM flags) and wins field-by-field when present.
     *
     * A student enrolled in one of [classes] but missing from the directory view still renders a
     * complete card built from `students` + the class metadata. If every source is empty, the
     * result is empty; a failure of `students` alone never hides directory rows.
     */
    suspend fun getStudentsForClasses(
        classes: List<MyCoordinatorClassView>
    ): Result<List<StudentProfileView>> = query {
        if (classes.isEmpty()) return@query emptyList()
        val classIds = classes.map { it.classId }
        val classById = classes.associateBy { it.classId }
        val client = SupabaseManager.requireClient()

        val directory = runCatching {
            client.postgrest[SupabaseTableData.Views.V_STUDENT_DIRECTORY]
                .select { filter { isIn("class_id", classIds) } }
                .decodeList<StudentProfileView>()
        }.getOrElse {
            Log.w(TAG, "v_student_directory query failed: ${it.message}")
            emptyList()
        }

        // The directory view can be restricted for faculty accounts. v_student_profiles carries
        // the same class mapping for a faculty-visible roster, so it backs up an empty directory.
        val profiles = if (directory.isEmpty()) {
            runCatching {
                client.postgrest[SupabaseTableData.Views.V_STUDENT_PROFILES]
                    .select { filter { isIn("class_id", classIds) } }
                    .decodeList<StudentProfileView>()
            }.getOrElse {
                Log.w(TAG, "v_student_profiles query failed: ${it.message}")
                emptyList()
            }
        } else {
            emptyList()
        }

        val enrollments = runCatching {
            client.postgrest[SupabaseTableData.Tables.ENROLLMENTS]
                .select { filter { isIn("class_id", classIds) } }
                .decodeList<EnrollmentRow>()
                .filter { classById.containsKey(it.classId) }
        }.getOrElse {
            Log.w(TAG, "enrollments query failed: ${it.message}")
            emptyList()
        }

        val identities = fetchStudentIdentities(
            directory.map { it.studentUid } +
                profiles.map { it.studentUid } +
                enrollments.map { it.studentUid }
        )

        Log.d(
            TAG,
            "getStudentsForClasses: classes=${classes.size}, directory=${directory.size}, " +
                "profiles=${profiles.size}, enrollments=${enrollments.size}, " +
                "identities=${identities.size}"
        )

        if (directory.isEmpty() && profiles.isEmpty() && enrollments.isEmpty()) {
            return@query emptyList()
        }

        val rows = LinkedHashMap<String, StudentProfileView>()
        directory.forEach { rows[it.studentUid] = it }
        profiles.forEach { if (!rows.containsKey(it.studentUid)) rows[it.studentUid] = it }

        enrollments.forEach { enrollment ->
            val uid = enrollment.studentUid
            if (uid.isBlank() || rows.containsKey(uid)) return@forEach
            val klass = classById[enrollment.classId] ?: return@forEach
            val identity = identities[uid]
            rows[uid] = StudentProfileView(
                studentUid = uid,
                name = identity?.name,
                serialNo = identity?.serialNo,
                fatherName = identity?.fatherName,
                studentPhoneNo = identity?.studentPhoneNo,
                fatherPhoneNo = identity?.fatherPhoneNo,
                studentEmail = identity?.studentEmail,
                groupName = identity?.groupName,
                isLocked = identity?.isLocked,
                isPtm = identity?.isPtm,
                course = klass.course,
                branch = klass.branch,
                semester = klass.semester,
                section = klass.section,
                academicYear = klass.academicYear,
                classId = klass.classId
            )
        }

        // A student can still surface twice under two different uids (duplicate `students` rows,
        // or a per-class key in a view). The roster is one card per person, so collapse rows that
        // would render as the same card.
        val seen = HashSet<String>()
        rows.values
            .map { overlayIdentity(it, identities[it.studentUid]) }
            .filter { seen.add(it.rosterKey()) }
    }

    /**
     * Identity used to collapse the coordinator roster. The roll number is the authoritative key
     * in a class list, so a non-blank `serial_no` + name identifies one student even when two rows
     * carry different uids or class metadata. Without a roll number we fall back to the name plus
     * class metadata, and finally to the uid (rows that cannot be told apart safely).
     */
    private fun StudentProfileView.rosterKey(): String {
        fun String?.normalised(): String =
            this?.trim()?.replace(Regex("\\s+"), " ")?.lowercase().orEmpty()

        val roll = serialNo.normalised()
        val name = this.name.normalised()
        return when {
            roll.isNotEmpty() -> "roll:$roll|$name"
            name.isNotEmpty() -> listOf(
                "name:$name",
                section.normalised(),
                semester?.toString().orEmpty(),
                branch.normalised(),
                academicYear.normalised()
            ).joinToString("|")
            else -> "uid:${studentUid.trim().lowercase()}"
        }
    }

    /** Reads `students` rows for [uids]. Best-effort: a blocked/failed read yields an empty map. */
    private suspend fun fetchStudentIdentities(uids: List<String>): Map<String, StudentRow> {
        val clean = uids.filter { it.isNotBlank() }.distinct()
        if (clean.isEmpty()) return emptyMap()
        return runCatching {
            SupabaseManager.requireClient().postgrest[SupabaseTableData.Tables.STUDENTS]
                .select { filter { isIn("uid", clean) } }
                .decodeList<StudentRow>()
                .associateBy { it.uid }
        }.getOrElse {
            Log.w(TAG, "students identity query failed: ${it.message}")
            emptyMap()
        }
    }

    /**
     * Overlays the `students` row on a directory row, field by field. Blank strings in `students`
     * are treated as missing so they never erase a real value coming from the directory.
     */
    private fun overlayIdentity(
        row: StudentProfileView,
        identity: StudentRow?
    ): StudentProfileView {
        if (identity == null) return row
        fun String?.orCurrent(current: String?): String? =
            this?.takeIf { it.isNotBlank() } ?: current

        return row.copy(
            name = identity.name.orCurrent(row.name),
            serialNo = identity.serialNo.orCurrent(row.serialNo),
            fatherName = identity.fatherName.orCurrent(row.fatherName),
            studentPhoneNo = identity.studentPhoneNo.orCurrent(row.studentPhoneNo),
            fatherPhoneNo = identity.fatherPhoneNo.orCurrent(row.fatherPhoneNo),
            studentEmail = identity.studentEmail.orCurrent(row.studentEmail),
            groupName = identity.groupName.orCurrent(row.groupName),
            isLocked = identity.isLocked ?: row.isLocked,
            isPtm = identity.isPtm ?: row.isPtm,
            lockReason = identity.lockReason ?: row.lockReason,
            ptmSchedule = identity.ptmSchedule ?: row.ptmSchedule
        )
    }

    /**
     * Locks or unlocks a profile. [reason] is stored in `students.lock_reason` and shown verbatim
     * on the student's lock screen; it is cleared on unlock and whenever no reason is typed.
     */
    suspend fun updateStudentLockStatus(
        studentUid: String,
        isLocked: Boolean,
        reason: String? = null
    ): Result<Unit> = updateStudentRow(studentUid) {
        val clean = reason?.trim().orEmpty()
        set("is_locked", isLocked)
        if (isLocked && clean.isNotEmpty()) set("lock_reason", clean) else setToNull("lock_reason")
    }

    /**
     * Schedules or cancels a PTM. [scheduledAt] is an ISO-8601 instant written to
     * `students.ptm_schedule`; [lockProfile] stores the lock flag and its reason in the same
     * UPDATE so a PTM scheduled "with lock" can never half-apply.
     */
    suspend fun updateStudentPtmStatus(
        studentUid: String,
        isPtm: Boolean,
        scheduledAt: String? = null,
        lockProfile: Boolean = false,
        lockReason: String? = null
    ): Result<Unit> = updateStudentRow(studentUid) {
        val schedule = scheduledAt?.trim().orEmpty()
        set("is_ptm", isPtm)
        if (isPtm && schedule.isNotEmpty()) set("ptm_schedule", schedule) else setToNull("ptm_schedule")

        if (lockProfile) {
            set("is_locked", true)
            val clean = lockReason?.trim().orEmpty()
            if (clean.isNotEmpty()) set("lock_reason", clean)
        }
    }

    /**
     * Applies one UPDATE to one `students` row.
     *
     * PostgREST answers an UPDATE with `204 No Content` even when zero rows matched, so the old
     * version of these calls reported success while RLS (or a non-matching uid) silently dropped
     * the write. Asking for the representation back with `select()` makes "nothing was updated"
     * detectable, and that is surfaced as a failure instead of a fake success.
     */
    private suspend fun updateStudentRow(
        studentUid: String,
        update: PostgrestUpdate.() -> Unit
    ): Result<Unit> = query {
        val uid = studentUid.trim()
        if (uid.isEmpty()) error("This student has no uid, so the change can't be saved.")

        val updated = SupabaseManager.requireClient().postgrest[SupabaseTableData.Tables.STUDENTS]
            .update(update) {
                filter { eq("uid", uid) }
                select()
            }
            .decodeList<StudentRow>()

        if (updated.isEmpty()) {
            error(
                "The change was not saved: the database reported no updated student row. " +
                    "Update access to public.students is missing for this coordinator account."
            )
        }
    }

    suspend fun getTimetables(classId: Int? = null): Result<List<TimetableRow>> = query {
        SupabaseManager.requireClient().postgrest[SupabaseTableData.Tables.TIMETABLES]
            .select {
                if (classId != null && classId > 0) {
                    filter { eq("class_id", classId) }
                }
                order("effective_from", Order.DESCENDING)
            }
            .decodeList<TimetableRow>()
    }

    /**
     * Timetables belonging to the classes the signed-in coordinator rules. The coordinator editor
     * must never list another course's classes, which is what an unfiltered `timetables` select
     * returned.
     */
    suspend fun getTimetablesForClasses(classIds: List<Int>): Result<List<TimetableRow>> = query {
        val ids = classIds.filter { it > 0 }.distinct()
        if (ids.isEmpty()) return@query emptyList()

        SupabaseManager.requireClient().postgrest[SupabaseTableData.Tables.TIMETABLES]
            .select {
                filter { isIn("class_id", ids) }
                order("effective_from", Order.DESCENDING)
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

package com.mitrc.ac.`in`.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.query.PostgrestUpdate
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.put

data class StudentCoordinatorContact(
    val name: String? = null,
    val designation: String? = null,
    val email: String? = null,
    val phoneNo: String? = null,
)

data class TeacherClassInfo(
    val classId: Int,
    val courseName: String? = null,
    val branchName: String? = null,
    val semester: Int? = null,
    val section: String? = null,
    val academicYear: String? = null,
) {
    val label: String
        get() = listOfNotNull(
            courseName?.trim()?.takeIf { it.isNotEmpty() },
            branchName?.trim()?.takeIf { it.isNotEmpty() },
            semester?.let { "Sem $it" },
            section?.trim()?.takeIf { it.isNotEmpty() }?.let { "Sec $it" },
        ).joinToString("  ·  ").ifEmpty { "Class $classId" }
}

object PortalRepository {

    private const val TAG = "PortalRepository"
    private val json = Json { ignoreUnknownKeys = true }

    private suspend fun <T> query(block: suspend () -> T): Result<T> = try {
        Result.success(
            withContext(Dispatchers.IO) {
                SupabaseManager.withAuthRetry { block() }
            }
        )
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (error: Throwable) {
        Result.failure(error)
    }


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
            val prof = fromView.getOrThrow()!!
            Log.i(TAG, "Student profile loaded from v_student_profiles (classId=${prof.classId})")
            return@query withStudentLockState(prof)
        } else {
            Log.w(TAG, "v_student_profiles query returned null/failed: ${fromView.exceptionOrNull()?.message}")
        }

        val fromStudentTable = runCatching {
            client.postgrest[SupabaseTableData.Tables.STUDENTS]
                .select { filter { eq("uid", uid) } }
                .decodeList<StudentRow>()
                .firstOrNull()
        }

        if (fromStudentTable.isSuccess && fromStudentTable.getOrNull() != null) {
            val studentRow = fromStudentTable.getOrThrow()!!
            val enrollment = runCatching {
                client.postgrest[SupabaseTableData.Tables.ENROLLMENTS]
                    .select { filter { eq("student_uid", uid) } }
                    .decodeList<EnrollmentRow>()
                    .firstOrNull()
            }.getOrNull()

            Log.i(TAG, "Student profile resolved from students table (classId=${enrollment?.classId})")
            return@query StudentProfileView(
                studentUid = studentRow.uid,
                name = studentRow.name,
                serialNo = studentRow.serialNo,
                fatherName = studentRow.fatherName,
                studentPhoneNo = studentRow.studentPhoneNo,
                fatherPhoneNo = studentRow.fatherPhoneNo,
                studentEmail = studentRow.studentEmail,
                groupName = studentRow.groupName,
                isLocked = studentRow.isLocked,
                isPtm = studentRow.isPtm,
                lockReason = studentRow.lockReason,
                ptmSchedule = studentRow.ptmSchedule,
                classId = enrollment?.classId
            )
        }

        val viewError = fromView.exceptionOrNull()
        val tableError = fromStudentTable.exceptionOrNull()
        if (viewError != null && tableError != null) throw viewError

        null
    }

    suspend fun getStudentName(uid: String, email: String?): Result<String?> = query {
        val client = SupabaseManager.requireClient()

        val byUid = runCatching {
            client.postgrest[SupabaseTableData.Tables.STUDENTS]
                .select { filter { eq("uid", uid) } }
                .decodeList<StudentRow>()
                .firstOrNull()
                ?.name
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
        }.getOrNull()
        if (byUid != null) return@query byUid

        val mail = email?.trim()?.takeIf { it.isNotEmpty() } ?: return@query null
        runCatching {
            client.postgrest[SupabaseTableData.Tables.STUDENTS]
                .select { filter { ilike("student_email", mail) } }
                .decodeList<StudentRow>()
                .firstOrNull()
                ?.name
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
        }.getOrNull()
    }

    private suspend fun withStudentLockState(profile: StudentProfileView): StudentProfileView {
        if (profile.studentUid.isBlank()) return profile
        val row = runCatching {
            SupabaseManager.requireClient().postgrest[SupabaseTableData.Tables.STUDENTS]
                .select { filter { eq("uid", profile.studentUid) } }
                .decodeList<StudentRow>()
                .firstOrNull()
        }.getOrNull() ?: return profile

        return profile.copy(
            name = if (profile.name.isNullOrBlank() && !row.name.isNullOrBlank()) row.name else profile.name,
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

        val fromView = runCatching {
            client.postgrest[SupabaseTableData.Views.V_CLASS_SUBJECTS]
                .select { filter { eq("class_id", classId) } }
                .decodeList<ClassSubjectView>()
        }.getOrDefault(emptyList())

        if (fromView.isNotEmpty()) {
            Log.i(TAG, "getStudentSubjects: ${fromView.size} subjects from v_class_subjects (class $classId)")
            return@query fromView
        }

        val fromTables = subjectsFromBaseTables(classId)
        Log.i(TAG, "getStudentSubjects: ${fromTables.size} subjects from base tables (class $classId)")
        fromTables
    }

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

    suspend fun getMyTimetable(classId: Int? = null): Result<List<MyTimetableEntryView>> = query {
        val client = SupabaseManager.requireClient()
        val uid = FirebaseAuth.getInstance().currentUser?.uid?.trim().orEmpty()

        val fromView = if (classId == null) {
            runCatching {
                client.postgrest[SupabaseTableData.Views.V_MY_TIMETABLE]
                    .select()
                    .decodeList<MyTimetableEntryView>()
            }.getOrElse {
                Log.w(TAG, "v_my_timetable query failed: ${it.message}")
                emptyList()
            }
        } else {
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
        subjectCode = subjectCode.orEmpty(),
        subjectName = subjectName.orEmpty(),
        subjectKind = subjectKind ?: "theory",
        groupId = groupId,
        groupName = groupName,
        facultyId = facultyId,
        teacherName = teacherName,
        room = room
    )

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

        val today = java.time.LocalDate.now().toString()
        val published = pool.filter { it.isPublished }
        val timetable = published.firstOrNull { it.effectiveFrom <= today }
            ?: published.firstOrNull()
            ?: pool.firstOrNull()
            ?: return emptyList()

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


    suspend fun linkMyFacultyAccount(): Result<Unit> = query {
        Log.d(TAG, "Calling link_my_faculty_account RPC...")
        SupabaseManager.requireClient().postgrest.rpc("link_my_faculty_account")
    }

    suspend fun getMyPortals(): Result<MyPortalsResponse> = query {
        Log.d(TAG, "Calling my_portals RPC...")
        val response = SupabaseManager.requireClient().postgrest.rpc("my_portals")
        runCatching {
            response.decodeAs<MyPortalsResponse>()
        }.getOrElse {
            response.decodeList<MyPortalsResponse>().firstOrNull() ?: MyPortalsResponse()
        }
    }.onFailure { Log.w(TAG, "my_portals RPC failed: ${it::class.simpleName}: ${it.message}") }


    suspend fun getTeacherProfile(uid: String): Result<FacultyMasterRow?> = query {
        SupabaseManager.requireClient().postgrest[SupabaseTableData.Tables.FACULTY_MASTER]
            .select { filter { eq("firebase_uid", uid) } }
            .decodeList<FacultyMasterRow>()
            .firstOrNull()
    }

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

    suspend fun linkFacultyUidByEmail(uid: String, email: String): Result<Unit> = query {
        SupabaseManager.requireClient().postgrest[SupabaseTableData.Tables.FACULTY_MASTER]
            .update({ set("firebase_uid", uid) }) {
                filter {
                    ilike("email", email.trim())
                    filter("firebase_uid", FilterOperator.IS, null)
                }
            }
    }

    suspend fun getTeacherSubjects(facultyId: Int): Result<List<ClassSubjectView>> = query {
        SupabaseManager.requireClient().postgrest[SupabaseTableData.Views.V_SUBJECT_TEACHERS]
            .select { filter { eq("faculty_id", facultyId) } }
            .decodeList<ClassSubjectView>()
    }

    suspend fun getClassInfo(classIds: List<Int>): Result<Map<Int, TeacherClassInfo>> = query {
        val ids = classIds.filter { it > 0 }.distinct()
        if (ids.isEmpty()) return@query emptyMap()

        runCatching {
            val client = SupabaseManager.requireClient()
            val classes = client.postgrest[SupabaseTableData.Tables.CLASSES]
                .select { filter { isIn("id", ids) } }
                .decodeList<ClassRow>()

            val branchIds = classes.map { it.branchId }.distinct()
            val branches = if (branchIds.isEmpty()) {
                emptyList()
            } else {
                client.postgrest[SupabaseTableData.Tables.BRANCHES]
                    .select { filter { isIn("id", branchIds) } }
                    .decodeList<BranchRow>()
            }

            val courseIds = branches.map { it.courseId }.distinct()
            val courses = if (courseIds.isEmpty()) {
                emptyList()
            } else {
                client.postgrest[SupabaseTableData.Tables.COURSES]
                    .select { filter { isIn("id", courseIds) } }
                    .decodeList<CourseRow>()
            }

            val branchById = branches.associateBy { it.id }
            val courseById = courses.associateBy { it.id }
            classes.associate { cls ->
                val branch = branchById[cls.branchId]
                cls.id to TeacherClassInfo(
                    classId = cls.id,
                    courseName = branch?.courseId?.let { courseById[it]?.name },
                    branchName = branch?.name,
                    semester = cls.semester,
                    section = cls.section,
                    academicYear = cls.academicYear,
                )
            }
        }.getOrElse {
            Log.w(TAG, "getClassInfo failed for ids=$ids: ${it.message}")
            emptyMap()
        }
    }

    suspend fun getClassGroups(classId: Int): Result<List<ClassGroupRow>> = query {
        SupabaseManager.requireClient().postgrest[SupabaseTableData.Tables.CLASS_GROUPS]
            .select {
                filter { eq("class_id", classId) }
                order("name", Order.ASCENDING)
            }
            .decodeList<ClassGroupRow>()
    }

    suspend fun getNotesForSubject(classSubjectId: Int): Result<List<NoteRow>> = query {
        SupabaseManager.requireClient().postgrest[SupabaseTableData.Tables.NOTES]
            .select {
                filter { eq("class_subject_id", classSubjectId) }
                order("created_at", Order.DESCENDING)
            }
            .decodeList<NoteRow>()
    }

    suspend fun getNotificationsForSubjects(
        classSubjectIds: List<Int>,
        groupIds: List<Int> = emptyList(),
    ): Result<List<NotificationRow>> = query {
        val subjectIds = classSubjectIds.filter { it > 0 }.distinct()
        val groups = groupIds.filter { it > 0 }.distinct()
        if (subjectIds.isEmpty() && groups.isEmpty()) return@query emptyList()

        val client = SupabaseManager.requireClient()
        val subjectNotices = if (subjectIds.isEmpty()) {
            emptyList()
        } else {
            client.postgrest["notifications"]
                .select {
                    filter { isIn("class_subject_id", subjectIds) }
                    order("created_at", Order.DESCENDING)
                }
                .decodeList<NotificationRow>()
        }

        val groupNotices = if (groups.isEmpty()) {
            emptyList()
        } else {
            client.postgrest["notifications"]
                .select {
                    filter { isIn("group_id", groups) }
                    order("created_at", Order.DESCENDING)
                }
                .decodeList<NotificationRow>()
        }

        (subjectNotices + groupNotices)
            .distinctBy { it.id }
            .sortedByDescending { it.createdAt.orEmpty() }
    }

    suspend fun createNotification(row: NewNotificationRow): Result<Unit> = query {
        val inserted = SupabaseManager.requireClient().postgrest["notifications"]
            .insert(row) { select() }
            .decodeList<NotificationRow>()

        if (inserted.isEmpty()) {
            error("The notice was not posted: the database returned no inserted row.")
        }
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
            .select {
                filter { eq("class_subject_id", classSubjectId) }
                order("id", Order.DESCENDING)
            }
            .decodeList<AssessmentRow>()
    }

    suspend fun createAssessment(assessment: AssessmentRow): Result<AssessmentRow> = query {
        SupabaseManager.requireClient().postgrest[SupabaseTableData.Tables.ASSESSMENTS]
            .insert(assessment) { select() }
            .decodeSingle<AssessmentRow>()
    }

    suspend fun getMarksForAssessment(assessmentId: Int): Result<List<MarkRow>> = query {
        SupabaseManager.requireClient().postgrest[SupabaseTableData.Tables.MARKS]
            .select { filter { eq("assessment_id", assessmentId) } }
            .decodeList<MarkRow>()
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

    private suspend fun resolveFacultyId(): Int? {
        val portals = getMyPortals().getOrNull()
        portals?.facultyId?.let { return it }

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

        val groupRows = runCatching {
            client.postgrest[SupabaseTableData.Tables.CLASS_GROUPS]
                .select { filter { isIn("class_id", classIds) } }
                .decodeList<ClassGroupRow>()
        }.getOrElse { emptyList() }
        val groupMap = groupRows.associateBy { it.id }
        val enrollmentMap = enrollments.associateBy { it.studentUid }

        val seen = HashSet<String>()
        rows.values
            .map { row ->
                val enrollment = enrollmentMap[row.studentUid]
                val gId = row.groupId ?: enrollment?.groupId
                val gName = row.groupName?.takeIf { it.isNotBlank() }
                    ?: (gId?.let { groupMap[it]?.name })

                val updatedRow = row.copy(
                    groupId = gId,
                    groupName = gName
                )
                overlayIdentity(updatedRow, identities[row.studentUid])
            }
            .filter { seen.add(it.rosterKey()) }
    }

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

    suspend fun updateStudentLockStatus(
        studentUid: String,
        isLocked: Boolean,
        reason: String? = null
    ): Result<Unit> = updateStudentRow(studentUid) {
        val clean = reason?.trim().orEmpty()
        set("is_locked", isLocked)
        if (isLocked && clean.isNotEmpty()) set("lock_reason", clean) else setToNull("lock_reason")
    }

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

    private suspend fun updateStudentRow(
        studentUid: String,
        update: PostgrestUpdate.() -> Unit
    ): Result<Unit> = query {
        val uid = studentUid.trim()
        if (uid.isEmpty()) error("This student has no uid, so the change can't be saved.")

        val client = runCatching { SupabaseManager.requireAdminClient() }
            .getOrDefault(SupabaseManager.requireClient())

        val updated = client.postgrest[SupabaseTableData.Tables.STUDENTS]
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

    suspend fun getNotifications(): Result<List<NotificationRow>> = query {
        SupabaseManager.requireClient().postgrest["notifications"]
            .select { order("created_at", Order.DESCENDING) }
            .decodeList<NotificationRow>()
    }

    suspend fun getSubjectTeachers(classId: Int): Result<List<ClassSubjectView>> = query {
        SupabaseManager.requireClient().postgrest[SupabaseTableData.Views.V_SUBJECT_TEACHERS]
            .select { filter { eq("class_id", classId) } }
            .decodeList<ClassSubjectView>()
    }
}

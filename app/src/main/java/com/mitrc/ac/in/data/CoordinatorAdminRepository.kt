package com.mitrc.ac.`in`.data

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.query.PostgrestUpdate
import io.github.jan.supabase.postgrest.rpc
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.Instant

/**
 * The student identity fields a coordinator can edit from the student sheet.
 *
 * Kept separate from [StudentProfileView] because that model also carries read-only view columns
 * (course, branch, class mapping). Passing only the editable fields to the repository makes it
 * impossible to accidentally write a joined view column into `students`.
 */
data class StudentDetailsUpdate(
    val name: String,
    val serialNo: String? = null,
    val fatherName: String? = null,
    val studentPhoneNo: String? = null,
    val fatherPhoneNo: String? = null,
    val studentEmail: String? = null,
    val groupId: Int? = null,
    val groupName: String? = null,
)

/**
 * Coordinator-only writes backed by the native service-role key.
 *
 * The coordinator token is still subject to RLS, which silently rejects timetable and student
 * edits in production. These calls use [SupabaseManager.requireAdminClient] instead. The screen
 * must have checked `course_coordinators` (or admin rights) first; this object intentionally does
 * no authorization of its own because it cannot see the Firebase identity.
 *
 * Every write asks PostgREST to return the affected row (`select()`). An empty result means RLS,
 * a bad id, or a missing row silently swallowed the write, and that is surfaced as a failure
 * rather than a fake success.
 */
object CoordinatorAdminRepository {

    // -----------------------------------------------------------------------------------------
    // Reads (used to refresh after a privileged write)
    // -----------------------------------------------------------------------------------------

    suspend fun getTimetables(classIds: List<Int>): Result<List<TimetableRow>> = admin { client ->
        val ids = classIds.filter { it > 0 }.distinct()
        if (ids.isEmpty()) return@admin emptyList()

        client.postgrest[SupabaseTableData.Tables.TIMETABLES]
            .select {
                filter { isIn("class_id", ids) }
                order("effective_from", Order.DESCENDING)
            }
            .decodeList<TimetableRow>()
    }

    suspend fun getEntries(timetableId: Int): Result<List<TimetableEntryView>> = admin { client ->
        client.postgrest[SupabaseTableData.Views.V_TIMETABLE_ENTRIES]
            .select {
                filter { eq("timetable_id", timetableId) }
                order("day_of_week", Order.ASCENDING)
                order("start_period", Order.ASCENDING)
            }
            .decodeList<TimetableEntryView>()
    }

    suspend fun getPeriods(timetableId: Int): Result<List<TimetablePeriodRow>> = admin { client ->
        client.postgrest[SupabaseTableData.Tables.TIMETABLE_PERIODS]
            .select {
                filter { eq("timetable_id", timetableId) }
                order("period", Order.ASCENDING)
            }
            .decodeList<TimetablePeriodRow>()
    }

    suspend fun getSubjects(classId: Int): Result<List<ClassSubjectView>> = admin { client ->
        client.postgrest[SupabaseTableData.Views.V_SUBJECT_TEACHERS]
            .select { filter { eq("class_id", classId) } }
            .decodeList<ClassSubjectView>()
    }

    suspend fun getGroups(classId: Int): Result<List<ClassGroupRow>> = admin { client ->
        client.postgrest[SupabaseTableData.Tables.CLASS_GROUPS]
            .select {
                filter { eq("class_id", classId) }
                order("name", Order.ASCENDING)
            }
            .decodeList<ClassGroupRow>()
    }

    // -----------------------------------------------------------------------------------------
    // Timetable writes
    // -----------------------------------------------------------------------------------------

    /** Saves the class room shown on every published slot that has no slot-level room. */
    suspend fun updateRoom(timetableId: Int, room: String?): Result<Unit> = admin { client ->
        val clean = room?.trim()?.takeIf { it.isNotEmpty() }

        val updated = client.postgrest[SupabaseTableData.Tables.TIMETABLES]
            .update({
                if (clean == null) setToNull("room") else set("room", clean)
            }) {
                filter { eq("id", timetableId) }
                select()
            }
            .decodeList<TimetableRow>()

        if (updated.isEmpty()) {
            error("The room was not saved: the database updated no timetable row.")
        }
    }

    /** Publishes or unpublishes a timetable and stamps `published_at` on publish. */
    suspend fun setPublished(timetableId: Int, published: Boolean): Result<Unit> = admin { client ->
        val updated = client.postgrest[SupabaseTableData.Tables.TIMETABLES]
            .update({
                set("is_published", published)
                if (published) set("published_at", Instant.now().toString()) else setToNull("published_at")
            }) {
                filter { eq("id", timetableId) }
                select()
            }
            .decodeList<TimetableRow>()

        if (updated.isEmpty()) {
            error("The timetable status was not saved: the database updated no timetable row.")
        }
    }

    suspend fun createEntry(entry: NewTimetableEntryRow): Result<Unit> = admin { client ->
        val created = client.postgrest[SupabaseTableData.Tables.TIMETABLE_ENTRIES]
            .insert(entry) { select() }
            .decodeList<TimetableEntryRow>()

        if (created.isEmpty()) {
            error("The slot was not created: the database returned no inserted row.")
        }
    }

    suspend fun updateEntry(entry: TimetableEntryRow): Result<Unit> = admin { client ->
        val updated = client.postgrest[SupabaseTableData.Tables.TIMETABLE_ENTRIES]
            .update({
                set("day_of_week", entry.dayOfWeek)
                set("start_period", entry.startPeriod)
                set("end_period", entry.endPeriod)
                set("class_subject_id", entry.classSubjectId)
                if (entry.groupId != null && entry.groupId > 0) {
                    set("group_id", entry.groupId)
                } else {
                    setToNull("group_id")
                }
                if (entry.facultyId != null && entry.facultyId > 0) {
                    set("faculty_id", entry.facultyId)
                } else {
                    setToNull("faculty_id")
                }
                if (entry.room.isNullOrBlank()) setToNull("room") else set("room", entry.room.trim())
            }) {
                filter { eq("id", entry.id) }
                select()
            }
            .decodeList<TimetableEntryRow>()

        if (updated.isEmpty()) {
            error("The slot was not saved: the database updated no timetable entry.")
        }
    }

    suspend fun deleteEntry(entryId: Int): Result<Unit> = admin { client ->
        val deleted = client.postgrest[SupabaseTableData.Tables.TIMETABLE_ENTRIES]
            .delete {
                filter { eq("id", entryId) }
                select()
            }
            .decodeList<TimetableEntryRow>()

        if (deleted.isEmpty()) {
            error("The slot was not deleted: the database deleted no timetable entry.")
        }
    }

    suspend fun duplicateTimetable(sourceId: Int, effectiveFrom: String): Result<Int> = admin { client ->
        val params = buildJsonObject {
            put("p_source", sourceId)
            put("p_effective_from", effectiveFrom)
        }
        client.postgrest.rpc("duplicate_timetable", params).decodeAs<Int>()
    }

    // -----------------------------------------------------------------------------------------
    // Student writes
    // -----------------------------------------------------------------------------------------

    suspend fun updateStudentDetails(
        studentUid: String,
        details: StudentDetailsUpdate
    ): Result<Unit> = admin { client ->
        val uid = studentUid.trim()
        require(uid.isNotEmpty()) { "This student has no uid, so the change can't be saved." }

        val name = details.name.trim()
        require(name.isNotEmpty()) { "Student name can't be empty." }

        val updated = client.postgrest[SupabaseTableData.Tables.STUDENTS]
            .update({
                set("name", name)
                setOptional("serial_no", details.serialNo)
                setOptional("father_name", details.fatherName)
                setOptional("student_phone_no", details.studentPhoneNo)
                setOptional("father_phone_no", details.fatherPhoneNo)
                setOptional("student_email", details.studentEmail)
            }) {
                filter { eq("uid", uid) }
                select()
            }
            .decodeList<StudentRow>()

        if (updated.isEmpty()) {
            error("The student details were not saved: the database updated no student row.")
        }
    }

    /** Moves a student between class groups by updating their `enrollments` row. */
    suspend fun updateStudentGroup(
        studentUid: String,
        classId: Int,
        groupId: Int?
    ): Result<Unit> = admin { client ->
        val uid = studentUid.trim()
        require(uid.isNotEmpty()) { "This student has no uid, so the group can't be saved." }

        val updated = client.postgrest[SupabaseTableData.Tables.ENROLLMENTS]
            .update({
                if (groupId != null && groupId > 0) set("group_id", groupId) else setToNull("group_id")
            }) {
                filter {
                    eq("student_uid", uid)
                    eq("class_id", classId)
                }
                select()
            }
            .decodeList<EnrollmentRow>()

        if (updated.isEmpty()) {
            error("The group was not saved: this student has no enrollment in class $classId.")
        }
    }

    // -----------------------------------------------------------------------------------------
    // Internal
    // -----------------------------------------------------------------------------------------

    private suspend fun <T> admin(block: suspend (SupabaseClient) -> T): Result<T> =
        runCatching { block(SupabaseManager.requireAdminClient()) }

    private fun PostgrestUpdate.setOptional(column: String, value: String?) {
        val clean = value?.trim()?.takeIf { it.isNotEmpty() }
        if (clean == null) setToNull(column) else set(column, clean)
    }
}

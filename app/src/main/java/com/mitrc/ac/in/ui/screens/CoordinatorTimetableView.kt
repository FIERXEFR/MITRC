package com.mitrc.ac.`in`.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Publish
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mitrc.ac.`in`.auth.AdminRepository
import com.mitrc.ac.`in`.data.ClassGroupRow
import com.mitrc.ac.`in`.data.CoordinatorAdminRepository
import com.mitrc.ac.`in`.data.FacultyMasterRow
import com.mitrc.ac.`in`.data.NewTimetableEntryRow
import com.mitrc.ac.`in`.data.PortalRepository
import com.mitrc.ac.`in`.data.ClassSubjectView
import com.mitrc.ac.`in`.data.TimetableEntryRow
import com.mitrc.ac.`in`.data.TimetableEntryView
import com.mitrc.ac.`in`.data.TimetableRow
import com.mitrc.ac.`in`.ui.theme.ErrorRed
import com.mitrc.ac.`in`.ui.theme.Gold
import com.mitrc.ac.`in`.ui.theme.Navy
import com.mitrc.ac.`in`.ui.theme.NavySoft
import com.mitrc.ac.`in`.ui.theme.Slate
import com.mitrc.ac.`in`.ui.theme.SuccessGreen
import com.mitrc.ac.`in`.ui.theme.SurfaceWhite
import com.mitrc.ac.`in`.ui.theme.TextPrimary
import com.mitrc.ac.`in`.ui.theme.TextSecondary
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Composable
fun CoordinatorTimetableView(userUid: String) {
    val scope = rememberCoroutineScope()

    var loading by remember { mutableStateOf(true) }
    var isCoordinatorAllowed by remember { mutableStateOf(false) }
    var coordinatorProfile by remember { mutableStateOf<FacultyMasterRow?>(null) }
    var ruledClasses by remember { mutableStateOf<List<com.mitrc.ac.`in`.data.MyCoordinatorClassView>>(emptyList()) }
    var timetables by remember { mutableStateOf<List<TimetableRow>>(emptyList()) }
    var selectedTimetable by remember { mutableStateOf<TimetableRow?>(null) }

    var entries by remember { mutableStateOf<List<TimetableEntryView>>(emptyList()) }
    var subjects by remember { mutableStateOf<List<ClassSubjectView>>(emptyList()) }
    var groups by remember { mutableStateOf<List<ClassGroupRow>>(emptyList()) }

    var actionMsg by remember { mutableStateOf<String?>(null) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    var newEffectiveFrom by remember { mutableStateOf(getTodayIstDate()) }

    // Room editor + slot dialog state. The dialog is reused for both create and edit: a null
    // `editingEntry` means "new slot".
    var roomDraft by remember { mutableStateOf("") }
    var showSlotDialog by remember { mutableStateOf(false) }
    var editingEntry by remember { mutableStateOf<TimetableEntryView?>(null) }

    LaunchedEffect(userUid) {
        loading = true
        val portals = PortalRepository.getMyPortals().getOrNull()

        // Same resolution the rest of the coordinator portal uses: `my_portals` can leave
        // is_coordinator false even though `course_coordinators` grants the rights, so the ruled
        // class list is the fallback. Checking only my_portals locked valid coordinators out here.
        val classesRes = PortalRepository.getMyCoordinatorClasses()
        ruledClasses = classesRes.getOrDefault(emptyList())
        isCoordinatorAllowed = portals?.isCoordinator == true ||
            portals?.isAdmin == true ||
            ruledClasses.isNotEmpty()

        if (isCoordinatorAllowed) {
            val profRes = PortalRepository.getTeacherProfile(userUid)
            coordinatorProfile = profRes.getOrNull()

            val ttRes = PortalRepository.getTimetablesForClasses(ruledClasses.map { it.classId })
            timetables = ttRes.getOrDefault(emptyList())
            if (timetables.isNotEmpty()) {
                selectedTimetable = timetables.first()
            }
        }
        loading = false
    }

    LaunchedEffect(selectedTimetable) {
        actionMsg = null
        errorMsg = null
        val tt = selectedTimetable
        if (tt != null) {
            roomDraft = tt.room.orEmpty()
            val entriesRes = PortalRepository.getTimetableEntries(tt.id)
            entries = entriesRes.getOrDefault(emptyList())

            val subTeachersRes = PortalRepository.getSubjectTeachers(tt.classId)
            subjects = subTeachersRes.getOrDefault(emptyList())

            groups = AdminRepository.getClassGroups(tt.classId)
        }
    }

    if (loading) {
        CoordinatorTimetableSkeleton()
        return
    }

    if (!isCoordinatorAllowed) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Coordinator Access Required",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = ErrorRed
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Your account does not have active coordinator rights in course_coordinators. Contact admin.",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            }
        }
        return
    }

    CoordinatorRefreshBox(
        onRefresh = {
            val refreshed = PortalRepository.getTimetablesForClasses(ruledClasses.map { it.classId })
            timetables = refreshed.getOrDefault(emptyList())
            val current = selectedTimetable ?: timetables.firstOrNull()
            if (current != null) {
                roomDraft = current.room.orEmpty()
                entries = PortalRepository.getTimetableEntries(current.id).getOrDefault(emptyList())
                subjects = PortalRepository.getSubjectTeachers(current.classId).getOrDefault(emptyList())
                groups = AdminRepository.getClassGroups(current.classId)
            }
        },
        skeleton = { CoordinatorTimetableSkeleton() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(
                text = "TIMETABLE MANAGER & DRAFTS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 1.5.sp
            )
            Spacer(Modifier.height(10.dp))

        if (timetables.isEmpty()) {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.padding(18.dp)) {
                    Text(text = "No timetables created yet.", fontSize = 13.sp, color = TextSecondary)
                }
            }
        } else {
            // Timetable selector chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                timetables.forEach { tt ->
                    val isSel = selectedTimetable?.id == tt.id
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSel) Navy else SurfaceWhite)
                            .clickable { selectedTimetable = tt }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = "TT #${tt.id} (w.e.f. ${tt.effectiveFrom})${if (tt.isPublished) " [PUB]" else " [DRAFT]"}",
                            color = if (isSel) Color.White else TextPrimary,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            val currentTT = selectedTimetable
            if (currentTT != null) {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Class ID: ${currentTT.classId} | w.e.f. ${currentTT.effectiveFrom}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Navy
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (currentTT.isPublished) SuccessGreen else Gold)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (currentTT.isPublished) "PUBLISHED" else "DRAFT",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(Modifier.height(14.dp))

                        // Duplicate Timetable RPC Action
                        OutlinedTextField(
                            value = newEffectiveFrom,
                            onValueChange = { newEffectiveFrom = it.trim() },
                            label = { Text("New Effective Date (YYYY-MM-DD)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    busy = true
                                    actionMsg = null
                                    errorMsg = null
                                    scope.launch {
                                        val res = CoordinatorAdminRepository.duplicateTimetable(currentTT.id, newEffectiveFrom)
                                        busy = false
                                        res.fold(
                                            onSuccess = { newId ->
                                                actionMsg = "Duplicated as new draft TT #$newId"
                                                val refresh = CoordinatorAdminRepository
                                                    .getTimetables(ruledClasses.map { it.classId })
                                                    .getOrDefault(timetables)
                                                timetables = refresh
                                                selectedTimetable = refresh.firstOrNull { it.id == newId }
                                                    ?: selectedTimetable
                                            },
                                            onFailure = { err ->
                                                errorMsg = err.message ?: "Failed to duplicate timetable"
                                            }
                                        )
                                    }
                                },
                                enabled = !busy,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = NavySoft),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Outlined.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(text = "Duplicate Draft", fontSize = 12.sp)
                            }

                            Button(
                                onClick = {
                                    busy = true
                                    actionMsg = null
                                    errorMsg = null
                                    scope.launch {
                                        val nextPublished = !currentTT.isPublished
                                        val res = CoordinatorAdminRepository.setPublished(currentTT.id, nextPublished)
                                        busy = false
                                        res.fold(
                                            onSuccess = {
                                                val updated = currentTT.copy(
                                                    isPublished = nextPublished,
                                                    publishedAt = if (nextPublished) {
                                                        java.time.Instant.now().toString()
                                                    } else {
                                                        null
                                                    }
                                                )
                                                selectedTimetable = updated
                                                timetables = timetables.map { if (it.id == updated.id) updated else it }
                                                actionMsg = "Timetable status updated to ${if (updated.isPublished) "Published" else "Draft"}"
                                            },
                                            onFailure = { err ->
                                                errorMsg = err.message ?: "Failed to update timetable status"
                                            }
                                        )
                                    }
                                },
                                enabled = !busy,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = if (currentTT.isPublished) Gold else SuccessGreen),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Outlined.Publish, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(text = if (currentTT.isPublished) "Unpublish" else "Publish", fontSize = 12.sp)
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        AnimatedVisibility(visible = actionMsg != null) {
                            Text(text = actionMsg.orEmpty(), color = SuccessGreen, fontSize = 12.sp)
                        }
                        AnimatedVisibility(visible = errorMsg != null) {
                            Text(text = errorMsg.orEmpty(), color = ErrorRed, fontSize = 12.sp)
                        }

                        Spacer(Modifier.height(14.dp))

                        Text(
                            text = "CLASS ROOM",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            letterSpacing = 1.2.sp
                        )
                        Spacer(Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = roomDraft,
                                onValueChange = { roomDraft = it },
                                label = { Text("Room (e.g. 204)") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    busy = true
                                    actionMsg = null
                                    errorMsg = null
                                    scope.launch {
                                        val res = CoordinatorAdminRepository.updateRoom(currentTT.id, roomDraft)
                                        busy = false
                                        res.fold(
                                            onSuccess = {
                                                val updated = currentTT.copy(
                                                    room = roomDraft.trim().takeIf { it.isNotEmpty() }
                                                )
                                                selectedTimetable = updated
                                                timetables = timetables.map { if (it.id == updated.id) updated else it }
                                                actionMsg = "Class room saved."
                                            },
                                            onFailure = { err ->
                                                errorMsg = err.message ?: "Failed to save the room"
                                            }
                                        )
                                    }
                                },
                                enabled = !busy,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Navy)
                            ) {
                                Text(text = "Save", fontSize = 12.sp)
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "TIMETABLE SLOTS (${entries.size})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary,
                                letterSpacing = 1.2.sp
                            )
                            TextButton(
                                onClick = {
                                    editingEntry = null
                                    showSlotDialog = true
                                }
                            ) {
                                Icon(
                                    Icons.Outlined.Add,
                                    contentDescription = null,
                                    tint = Navy,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(text = "Add slot", color = Navy, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        Spacer(Modifier.height(8.dp))

                        if (entries.isEmpty()) {
                            Text(text = "No slot entries in this draft yet.", fontSize = 12.sp, color = TextSecondary)
                        } else {
                            entries.forEach { entry ->
                                SlotEntryRow(
                                    entry = entry,
                                    onEdit = {
                                        editingEntry = entry
                                        showSlotDialog = true
                                    },
                                    onDelete = {
                                        errorMsg = null
                                        scope.launch {
                                            CoordinatorAdminRepository.deleteEntry(entry.entryId).fold(
                                                onSuccess = {
                                                    actionMsg = "Slot deleted."
                                                    entries = CoordinatorAdminRepository
                                                        .getEntries(currentTT.id)
                                                        .getOrDefault(entries)
                                                },
                                                onFailure = { err ->
                                                    errorMsg = err.message ?: "Failed to delete the slot"
                                                }
                                            )
                                        }
                                    }
                                )
                                Spacer(Modifier.height(6.dp))
                            }
                        }
                    }
                }

                if (showSlotDialog) {
                    TimetableSlotDialog(
                        timetableId = currentTT.id,
                        entry = editingEntry,
                        subjects = subjects,
                        groups = groups,
                        onDismiss = { showSlotDialog = false },
                        onSaved = {
                            showSlotDialog = false
                            actionMsg = if (editingEntry == null) "Slot added." else "Slot saved."
                            errorMsg = null
                            scope.launch {
                                entries = CoordinatorAdminRepository
                                    .getEntries(currentTT.id)
                                    .getOrDefault(entries)
                            }
                        },
                        onError = { message ->
                            errorMsg = message
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Signatories Section
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "OFFICIAL SIGNATORIES",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 1.2.sp
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(text = coordinatorProfile?.name ?: "Course Coordinator", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text(
                            text = coordinatorProfile?.designation ?: "Coordinator",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
        }
    }
}

@Composable
private fun SlotEntryRow(
    entry: TimetableEntryView,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val daysMap = mapOf(1 to "Mon", 2 to "Tue", 3 to "Wed", 4 to "Thu", 5 to "Fri", 6 to "Sat")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Slate)
            .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${daysMap[entry.dayOfWeek]} | Period ${entry.startPeriod}${if (entry.endPeriod > entry.startPeriod) "-${entry.endPeriod}" else ""}",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Navy
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "${entry.subjectName} (${entry.subjectCode})",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            if (!entry.teacherName.isNullOrBlank()) {
                Text(text = "Teacher: ${entry.teacherName}", fontSize = 11.sp, color = TextSecondary)
            }
            val place = entry.room?.takeIf { it.isNotBlank() } ?: entry.classRoom?.takeIf { it.isNotBlank() }
            if (place != null) {
                Text(text = "Room: $place", fontSize = 11.sp, color = TextSecondary)
            }
        }

        IconButton(onClick = onEdit) {
            Icon(
                Icons.Outlined.Edit,
                contentDescription = "Edit Entry",
                tint = Navy,
                modifier = Modifier.size(18.dp)
            )
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Outlined.Delete, contentDescription = "Delete Entry", tint = ErrorRed, modifier = Modifier.size(18.dp))
        }
    }
}

/**
 * Create/edit dialog for one timetable slot. The same form is used for both flows: a null [entry]
 * means the slot will be inserted, otherwise the existing row is updated in place.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TimetableSlotDialog(
    timetableId: Int,
    entry: TimetableEntryView?,
    subjects: List<ClassSubjectView>,
    groups: List<ClassGroupRow>,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    onError: (String) -> Unit
) {
    val scope = rememberCoroutineScope()

    // `v_timetable_entries` exposes the subject labels but not `class_subject_id`, so the
    // enrolment row is matched back by its unique code/name pair when editing an existing slot.
    val initialSubjectId = remember(entry, subjects) {
        entry?.let { target ->
            subjects.firstOrNull {
                it.subjectCode == target.subjectCode && it.subjectName == target.subjectName
            }?.classSubjectId
        } ?: subjects.firstOrNull()?.classSubjectId ?: 0
    }

    var day by remember(entry) { mutableIntStateOf(entry?.dayOfWeek ?: 1) }
    var startPeriod by remember(entry) { mutableIntStateOf(entry?.startPeriod ?: 1) }
    var endPeriod by remember(entry) {
        mutableIntStateOf(entry?.endPeriod ?: entry?.startPeriod ?: 1)
    }
    var subjectId by remember(entry, initialSubjectId) { mutableIntStateOf(initialSubjectId) }
    var groupId by remember(entry) { mutableStateOf(entry?.groupId) }
    var room by remember(entry) { mutableStateOf(entry?.room.orEmpty()) }

    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val selectedSubject = subjects.firstOrNull { it.classSubjectId == subjectId }
    val days = listOf(1 to "Mon", 2 to "Tue", 3 to "Wed", 4 to "Thu", 5 to "Fri", 6 to "Sat")
    val periodChoices = (1..10).toList()

    suspend fun save() {
        if (subjectId <= 0) {
            error = "Choose a subject for this slot."
            return
        }
        if (startPeriod > endPeriod) {
            error = "The start period must be before the end period."
            return
        }

        saving = true
        error = null
        val facultyId = selectedSubject?.facultyId?.takeIf { it > 0 } ?: entry?.facultyId
        val cleanRoom = room.trim().takeIf { it.isNotEmpty() }

        val result = if (entry == null) {
            CoordinatorAdminRepository.createEntry(
                NewTimetableEntryRow(
                    timetableId = timetableId,
                    dayOfWeek = day,
                    startPeriod = startPeriod,
                    endPeriod = endPeriod,
                    classSubjectId = subjectId,
                    groupId = groupId,
                    facultyId = facultyId,
                    room = cleanRoom
                )
            )
        } else {
            CoordinatorAdminRepository.updateEntry(
                TimetableEntryRow(
                    id = entry.entryId,
                    timetableId = timetableId,
                    dayOfWeek = day,
                    startPeriod = startPeriod,
                    endPeriod = endPeriod,
                    classSubjectId = subjectId,
                    groupId = groupId,
                    facultyId = facultyId,
                    room = cleanRoom
                )
            )
        }

        saving = false
        result.fold(
            onSuccess = { onSaved() },
            onFailure = { failure ->
                val message = failure.message ?: "Couldn't save this slot."
                error = message
                onError(message)
            }
        )
    }

    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        title = {
            Text(
                text = if (entry == null) "Add Timetable Slot" else "Edit Timetable Slot",
                color = Navy,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                SlotDialogLabel("Day")
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    days.forEach { (value, label) ->
                        TimetableChoiceChip(label = label, selected = day == value) { day = value }
                    }
                }

                SlotDialogLabel("Start period")
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    periodChoices.forEach { period ->
                        TimetableChoiceChip(
                            label = "$period",
                            selected = startPeriod == period
                        ) {
                            startPeriod = period
                            if (endPeriod < period) endPeriod = period
                        }
                    }
                }

                SlotDialogLabel("End period")
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    periodChoices.forEach { period ->
                        TimetableChoiceChip(
                            label = "$period",
                            selected = endPeriod == period
                        ) { endPeriod = period }
                    }
                }

                SlotDialogLabel("Subject")
                if (subjects.isEmpty()) {
                    Text(
                        text = "No subjects are linked to this class yet.",
                        color = ErrorRed,
                        fontSize = 11.5.sp
                    )
                } else {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        subjects.forEach { subject ->
                            TimetableChoiceChip(
                                label = subject.subjectCode,
                                selected = subjectId == subject.classSubjectId
                            ) { subjectId = subject.classSubjectId }
                        }
                    }
                    if (selectedSubject != null) {
                        Text(
                            text = "${selectedSubject.subjectName} · ${selectedSubject.teacherName}",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                if (groups.isNotEmpty()) {
                    SlotDialogLabel("Group (optional)")
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        TimetableChoiceChip(label = "All groups", selected = groupId == null) {
                            groupId = null
                        }
                        groups.forEach { group ->
                            TimetableChoiceChip(
                                label = group.name,
                                selected = groupId == group.id
                            ) { groupId = group.id }
                        }
                    }
                }

                OutlinedTextField(
                    value = room,
                    onValueChange = { room = it },
                    label = { Text("Room (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                if (error != null) {
                    Text(text = error.orEmpty(), color = ErrorRed, fontSize = 11.5.sp)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { scope.launch { save() } },
                enabled = !saving && subjects.isNotEmpty()
            ) {
                Text(
                    text = if (saving) "Saving..." else "Save slot",
                    color = if (saving) TextSecondary else Navy,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !saving) {
                Text("Cancel", color = TextSecondary, fontSize = 13.sp)
            }
        },
        containerColor = SurfaceWhite
    )
}

@Composable
private fun SlotDialogLabel(text: String) {
    Text(
        text = text.uppercase(),
        color = TextSecondary,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp
    )
}

@Composable
private fun TimetableChoiceChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(9.dp))
            .background(if (selected) Navy else Slate)
            .clickable(onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 7.dp)
    ) {
        Text(
            text = label,
            color = if (selected) Color.White else TextPrimary,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

private fun getTodayIstDate(): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    sdf.timeZone = TimeZone.getTimeZone("Asia/Kolkata")
    return sdf.format(Date())
}
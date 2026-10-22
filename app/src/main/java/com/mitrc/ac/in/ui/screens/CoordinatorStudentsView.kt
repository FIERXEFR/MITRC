package com.mitrc.ac.`in`.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PersonOff
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mitrc.ac.`in`.data.AttendanceSummaryView
import com.mitrc.ac.`in`.data.ClassGroupRow
import com.mitrc.ac.`in`.data.CoordinatorAdminRepository
import com.mitrc.ac.`in`.data.MarkView
import com.mitrc.ac.`in`.data.PortalRepository
import com.mitrc.ac.`in`.data.StudentDetailsUpdate
import com.mitrc.ac.`in`.data.StudentProfileView
import com.mitrc.ac.`in`.ui.theme.DividerSoft
import com.mitrc.ac.`in`.ui.theme.ErrorRed
import com.mitrc.ac.`in`.ui.theme.Gold
import com.mitrc.ac.`in`.ui.theme.GoldDeep
import com.mitrc.ac.`in`.ui.theme.Navy
import com.mitrc.ac.`in`.ui.theme.NavySoft
import com.mitrc.ac.`in`.ui.theme.Slate
import com.mitrc.ac.`in`.ui.theme.SuccessGreen
import com.mitrc.ac.`in`.ui.theme.SurfaceWhite
import com.mitrc.ac.`in`.ui.theme.TextPrimary
import com.mitrc.ac.`in`.ui.theme.TextSecondary
import com.mitrc.ac.`in`.ui.theme.WarningAmber
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch

// ---------------------------------------------------------------------------
// Main screen composable
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoordinatorStudentsView(userUid: String) {
    val scope = rememberCoroutineScope()

    // ---- state ----
    var allStudents by remember { mutableStateOf<List<StudentProfileView>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedStudent by remember { mutableStateOf<StudentProfileView?>(null) }
    var loading by remember { mutableStateOf(true) }
    var classCount by remember { mutableStateOf(0) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var actionError by remember { mutableStateOf<String?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Identity (name, roll, phone, email, group, lock/PTM flags) is read from the `students`
    // table; the class mapping comes from v_student_directory (with v_student_profiles and
    // enrollments as fallbacks). The repository merges them, with `students` winning on every
    // identity field.
    suspend fun loadStudents() {
        errorMsg = null
        val classes = PortalRepository.getMyCoordinatorClasses().getOrElse {
            errorMsg = it.message ?: "Couldn't load your classes."
            return
        }
        classCount = classes.size

        PortalRepository.getStudentsForClasses(classes)
            .onSuccess { allStudents = it }
            .onFailure { errorMsg = it.message }
    }

    // Applies a saved change to both the roster and the open sheet, so the UI never shows an
    // update the database rejected (failures surface through `actionError` instead).
    fun applyStudentChange(uid: String, transform: (StudentProfileView) -> StudentProfileView) {
        allStudents = allStudents.map { if (it.studentUid == uid) transform(it) else it }
        selectedStudent = selectedStudent?.let { if (it.studentUid == uid) transform(it) else it }
    }

    // ---- load ----
    LaunchedEffect(userUid) {
        loading = true
        loadStudents()
        loading = false
    }

    // ---- filtered list sorted A-Z alphabetically ----
    val filtered = remember(allStudents, searchQuery) {
        val q = searchQuery.trim().lowercase()
        val list = if (q.isEmpty()) allStudents
        else allStudents.filter {
            it.name?.lowercase()?.contains(q) == true ||
                    it.serialNo?.lowercase()?.contains(q) == true ||
                    it.studentEmail?.lowercase()?.contains(q) == true ||
                    it.section?.lowercase()?.contains(q) == true ||
                    it.branch?.lowercase()?.contains(q) == true
        }
        list.sortedBy { it.name?.lowercase() ?: "" }
    }

    // ---- UI ----
    // The tab title lives in the portal hero now; this tab deliberately has no second header.
    CoordinatorRefreshBox(
        onRefresh = { loadStudents() },
        skeleton = { CoordinatorStudentListSkeleton() }
    ) {
        // Initial load shows the same shimmer preview the pull-to-refresh uses instead of a
        // centred spinner, so the tab never flashes a blank screen.
        if (loading) {
            CoordinatorStudentListSkeleton()
            return@CoordinatorRefreshBox
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Slate)
        ) {
            SearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                onClear = { searchQuery = "" },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )

            StudentsCountRow(count = filtered.size, totalCount = allStudents.size)

            when {
                errorMsg != null -> ErrorState(
                    message = errorMsg!!,
                    onRetry = {
                        scope.launch {
                            loading = true
                            loadStudents()
                            loading = false
                        }
                    }
                )
                classCount == 0 -> EmptyState(
                    icon = Icons.Outlined.Group,
                    title = "No Classes Assigned",
                    subtitle = "Your coordinator account isn't linked to any class yet."
                )
                filtered.isEmpty() && allStudents.isEmpty() -> EmptyState(
                    icon = Icons.Outlined.Group,
                    title = "No Students Yet",
                    subtitle = "No students were found in your assigned classes. Pull down to refresh."
                )
                filtered.isEmpty() -> EmptyState(
                    icon = Icons.Outlined.Search,
                    title = "No Results",
                    subtitle = "Try a different name, roll number, or section."
                )
                else -> StudentList(
                    students = filtered,
                    onStudentClick = { selectedStudent = it }
                )
            }
        }
    }

    // Detail bottom sheet
    if (selectedStudent != null) {
        ModalBottomSheet(
            onDismissRequest = { selectedStudent = null },
            sheetState = sheetState,
            containerColor = SurfaceWhite,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            StudentDetailSheet(
                student = selectedStudent!!,
                onDismiss = { selectedStudent = null },
                onLockToggle = { uid, locked, reason ->
                    scope.launch {
                        PortalRepository.updateStudentLockStatus(uid, locked, reason)
                            .onSuccess {
                                val clean = reason?.trim()?.takeIf { it.isNotEmpty() }
                                applyStudentChange(uid) {
                                    it.copy(
                                        isLocked = locked,
                                        lockReason = if (locked) clean else null
                                    )
                                }
                            }
                            .onFailure {
                                actionError = it.message ?: "Couldn't update this student."
                            }
                    }
                },
                onPtmUpdate = { uid, update ->
                    scope.launch {
                        PortalRepository.updateStudentPtmStatus(
                            studentUid = uid,
                            isPtm = update.scheduled,
                            scheduledAt = update.scheduledAt,
                            lockProfile = update.lockProfile,
                            lockReason = update.lockReason
                        )
                            .onSuccess {
                                val clean = update.lockReason?.trim()?.takeIf { it.isNotEmpty() }
                                applyStudentChange(uid) {
                                    it.copy(
                                        isPtm = update.scheduled,
                                        ptmSchedule = if (update.scheduled) update.scheduledAt else null,
                                        isLocked = if (update.lockProfile) true else it.isLocked,
                                        lockReason = if (update.lockProfile && clean != null) clean else it.lockReason
                                    )
                                }
                            }
                            .onFailure {
                                actionError = it.message ?: "Couldn't update this student."
                            }
                    }
                },
                onDetailsSaved = { uid, update ->
                    // The edit dialog already wrote through the service-role client. Mirror the
                    // saved values in the roster and the open sheet so the UI can't disagree
                    // with the database.
                    applyStudentChange(uid) {
                        it.copy(
                            name = update.name,
                            serialNo = update.serialNo,
                            fatherName = update.fatherName,
                            studentPhoneNo = update.studentPhoneNo,
                            fatherPhoneNo = update.fatherPhoneNo,
                            studentEmail = update.studentEmail,
                            groupId = update.groupId,
                            groupName = update.groupName
                        )
                    }
                }
            )
        }
    }

    if (actionError != null) {
        AlertDialog(
            onDismissRequest = { actionError = null },
            title = { Text("Update failed", color = TextPrimary, fontWeight = FontWeight.SemiBold) },
            text = { Text(actionError!!, color = TextSecondary, fontSize = 13.sp) },
            confirmButton = {
                TextButton(onClick = { actionError = null }) {
                    Text("OK", color = Navy, fontWeight = FontWeight.SemiBold)
                }
            },
            containerColor = SurfaceWhite
        )
    }
}

// ---------------------------------------------------------------------------
// Count line (replaces the old second hero header)
// ---------------------------------------------------------------------------

@Composable
private fun StudentsCountRow(count: Int, totalCount: Int) {
    val label = if (count == totalCount) "$totalCount enrolled"
    else "$count of $totalCount students"

    Text(
        text = label,
        color = TextSecondary,
        fontSize = 11.5.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 8.dp)
    )
}

// ---------------------------------------------------------------------------
// Search bar
// ---------------------------------------------------------------------------

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = {
            Text(
                "Search by name, roll no, section…",
                color = TextSecondary.copy(alpha = 0.6f),
                fontSize = 14.sp
            )
        },
        leadingIcon = {
            Icon(Icons.Outlined.Search, contentDescription = null, tint = Navy, modifier = Modifier.size(20.dp))
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = onClear) {
                    Icon(Icons.Outlined.Close, contentDescription = "Clear", tint = TextSecondary, modifier = Modifier.size(18.dp))
                }
            }
        },
        shape = RoundedCornerShape(16.dp),
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = {}),
        colors = OutlinedTextFieldDefaults.colors(
            // The Material defaults inherit onSurface from the (dark-capable) app theme, which
            // rendered white text on this white card. Pin every slot to the portal palette.
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
            disabledTextColor = TextPrimary.copy(alpha = 0.6f),
            cursorColor = Navy,
            focusedBorderColor = Navy,
            unfocusedBorderColor = DividerSoft,
            focusedContainerColor = SurfaceWhite,
            unfocusedContainerColor = SurfaceWhite,
            focusedLeadingIconColor = Navy,
            unfocusedLeadingIconColor = Navy,
            focusedTrailingIconColor = TextSecondary,
            unfocusedTrailingIconColor = TextSecondary,
            focusedPlaceholderColor = TextSecondary.copy(alpha = 0.6f),
            unfocusedPlaceholderColor = TextSecondary.copy(alpha = 0.6f)
        )
    )
}

// ---------------------------------------------------------------------------
// Student list
// ---------------------------------------------------------------------------

@Composable
private fun StudentList(
    students: List<StudentProfileView>,
    onStudentClick: (StudentProfileView) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(students, key = { it.studentUid }) { student ->
            StudentCard(student = student, onClick = { onStudentClick(student) })
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StudentCard(student: StudentProfileView, onClick: () -> Unit) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar circle
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.verticalGradient(listOf(NavySoft, Navy))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = (student.name?.firstOrNull()?.uppercaseChar() ?: '?').toString(),
                    color = SurfaceWhite,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = student.name ?: "Unknown Student",
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = buildString {
                        student.serialNo?.let { append("Roll: $it") }
                        student.section?.let { if (isNotEmpty()) append(" · "); append("Sec $it") }
                        student.semester?.let { if (isNotEmpty()) append(" · "); append("Sem $it") }
                    },
                    color = TextSecondary,
                    fontSize = 12.sp
                )
                Spacer(Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    student.branch?.let { StudentChip(it, Navy) }
                    student.academicYear?.let { StudentChip(it, GoldDeep) }
                    student.groupName?.let { StudentChip(it, SuccessGreen) }
                    if (student.isLocked == true) StudentChip("Locked", ErrorRed)
                    if (student.isPtm == true) StudentChip("PTM Scheduled", SuccessGreen)
                }
            }
            Icon(
                Icons.Outlined.ExpandMore,
                contentDescription = null,
                tint = TextSecondary.copy(alpha = 0.5f),
                modifier = Modifier.size(18.dp).rotate(-90f)
            )
        }
    }
}

@Composable
private fun StudentChip(label: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.10f))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
            .padding(horizontal = 7.dp, vertical = 2.dp)
    ) {
        Text(label, color = color, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
    }
}

// ---------------------------------------------------------------------------
// Student detail bottom sheet
// ---------------------------------------------------------------------------

@Composable
private fun StudentDetailSheet(
    student: StudentProfileView,
    onDismiss: () -> Unit,
    onLockToggle: (String, Boolean, String?) -> Unit,
    onPtmUpdate: (String, PtmUpdate) -> Unit,
    onDetailsSaved: (String, StudentDetailsUpdate) -> Unit
) {
    val scope = rememberCoroutineScope()
    var attendance by remember { mutableStateOf<List<AttendanceSummaryView>>(emptyList()) }
    var marks by remember { mutableStateOf<List<MarkView>>(emptyList()) }
    var loadingDetails by remember { mutableStateOf(true) }

    // confirmation dialogs
    var showLockDialog by remember { mutableStateOf(false) }
    var showPtmDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }

    LaunchedEffect(student.studentUid) {
        loadingDetails = true
        PortalRepository.getStudentAttendance(student.studentUid).onSuccess { attendance = it }
        PortalRepository.getStudentMarks(student.studentUid).onSuccess { marks = it }
        loadingDetails = false
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 32.dp)
    ) {
        // drag handle
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 8.dp, bottom = 16.dp)
                .size(width = 40.dp, height = 4.dp)
                .clip(CircleShape)
                .background(DividerSoft)
        )

        // ---- Avatar + name ----
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(Brush.verticalGradient(listOf(NavySoft, Navy))),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    (student.name?.firstOrNull()?.uppercaseChar() ?: '?').toString(),
                    color = SurfaceWhite, fontSize = 26.sp, fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    student.name ?: "Unknown",
                    color = TextPrimary, fontSize = 19.sp, fontWeight = FontWeight.Bold,
                    maxLines = 2, overflow = TextOverflow.Ellipsis
                )
                student.serialNo?.let {
                    Text("Roll No: $it", color = TextSecondary, fontSize = 12.sp)
                }
            }
            IconButton(onClick = { showEditDialog = true }) {
                Icon(
                    Icons.Outlined.Edit,
                    contentDescription = "Edit student details",
                    tint = Navy
                )
            }
            IconButton(onClick = onDismiss) {
                Icon(Icons.Outlined.Close, contentDescription = "Close", tint = TextSecondary)
            }
        }

        Spacer(Modifier.height(16.dp))
        Divider(color = DividerSoft, thickness = 1.dp)
        Spacer(Modifier.height(16.dp))

        // ---- Class info chips ----
        DetailSectionLabel("Class Information", Icons.Outlined.Badge)
        Spacer(Modifier.height(8.dp))
        @OptIn(ExperimentalLayoutApi::class)
        FlowRow(
            modifier = Modifier.padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            student.course?.let { InfoChip("Course", it, Navy) }
            student.branch?.let { InfoChip("Branch", it, Navy) }
            student.semester?.let { InfoChip("Semester", "$it", GoldDeep) }
            student.section?.let { InfoChip("Section", it, GoldDeep) }
            student.academicYear?.let { InfoChip("Year", it, GoldDeep) }
            student.groupName?.let { InfoChip("Group", it, SuccessGreen) }
        }

        Spacer(Modifier.height(20.dp))
        Divider(color = DividerSoft, thickness = 1.dp)
        Spacer(Modifier.height(16.dp))

        // ---- Contact ----
        DetailSectionLabel("Contact Details", Icons.Outlined.Call)
        Spacer(Modifier.height(12.dp))

        ContactRow(Icons.Outlined.AccountCircle, "Student", student.studentPhoneNo ?: "Not available")
        student.studentEmail?.let {
            ContactRow(Icons.Outlined.Email, "Email", it)
        }
        ContactRow(Icons.Outlined.Person, "Father", student.fatherName ?: "Not available")
        ContactRow(Icons.Outlined.Call, "Father's Phone", student.fatherPhoneNo ?: "Not available")

        Spacer(Modifier.height(20.dp))
        Divider(color = DividerSoft, thickness = 1.dp)
        Spacer(Modifier.height(16.dp))

        // ---- Actions ----
        DetailSectionLabel("Coordinator Actions", Icons.Outlined.Star)
        Spacer(Modifier.height(12.dp))

        ActionToggleRow(
            icon = if (student.isLocked == true) Icons.Outlined.Lock else Icons.Outlined.LockOpen,
            iconTint = if (student.isLocked == true) ErrorRed else SuccessGreen,
            title = "Account Lock",
            subtitle = if (student.isLocked == true) "Profile is currently locked" else "Profile is active",
            checked = student.isLocked == true,
            onToggle = { showLockDialog = true }
        )
        if (student.isLocked == true) {
            val reason = student.lockReason?.trim()?.takeIf { it.isNotEmpty() }
            ActionNote(
                if (reason != null) "Reason shown to student: $reason"
                else "No reason set - the student sees a generic message."
            )
        }
        Spacer(Modifier.height(8.dp))
        ActionToggleRow(
            icon = Icons.Outlined.NotificationsActive,
            iconTint = if (student.isPtm == true) Gold else TextSecondary,
            title = "Schedule PTM",
            subtitle = if (student.isPtm == true) "PTM has been scheduled" else "No PTM scheduled",
            checked = student.isPtm == true,
            onToggle = { showPtmDialog = true }
        )
        if (student.isPtm == true) {
            val at = formatPtmSchedule(student.ptmSchedule)
            ActionNote(if (at != null) "PTM scheduled for: $at" else "PTM time has not been set.")
        }

        Spacer(Modifier.height(20.dp))
        Divider(color = DividerSoft, thickness = 1.dp)
        Spacer(Modifier.height(16.dp))

        // ---- Attendance ----
        DetailSectionLabel("Attendance Summary", Icons.Outlined.CheckCircle)
        Spacer(Modifier.height(12.dp))

        if (loadingDetails) {
            Box(Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Navy, strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
            }
        } else if (attendance.isEmpty()) {
            EmptyDetailNote("No attendance records found.")
        } else {
            Column(modifier = Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                attendance.forEach { row ->
                    AttendanceSubjectRow(row)
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        Divider(color = DividerSoft, thickness = 1.dp)
        Spacer(Modifier.height(16.dp))

        // ---- Marks ----
        DetailSectionLabel("Assessment Marks", Icons.Outlined.Info)
        Spacer(Modifier.height(12.dp))

        if (!loadingDetails && marks.isEmpty()) {
            EmptyDetailNote("No marks recorded yet.")
        } else if (!loadingDetails) {
            val grouped = marks.groupBy { it.subjectName }
            Column(modifier = Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                grouped.forEach { (subject, subjectMarks) ->
                    MarksSubjectSection(subject, subjectMarks)
                }
            }
        }

        Spacer(Modifier.height(32.dp))
    }

    // ---- Lock dialog: reason on lock, plain confirm on unlock ----
    if (showLockDialog) {
        if (student.isLocked != true) {
            LockProfileDialog(
                studentName = student.name ?: "this student",
                onDismiss = { showLockDialog = false },
                onConfirm = { reason ->
                    showLockDialog = false
                    onLockToggle(student.studentUid, true, reason)
                }
            )
        } else {
            ConfirmDialog(
                title = "Unlock Profile?",
                message = "Unlocking will restore ${student.name}'s access. The saved reason is cleared.",
                confirmLabel = "Unlock",
                confirmColor = SuccessGreen,
                onConfirm = {
                    showLockDialog = false
                    onLockToggle(student.studentUid, false, null)
                },
                onDismiss = { showLockDialog = false }
            )
        }
    }

    // ---- PTM dialog: pick date + time, optionally lock in the same step ----
    if (showPtmDialog) {
        if (student.isPtm == true) {
            ConfirmDialog(
                title = "Cancel PTM?",
                message = "This will cancel the scheduled PTM for ${student.name}.",
                confirmLabel = "Cancel PTM",
                confirmColor = ErrorRed,
                onConfirm = {
                    showPtmDialog = false
                    onPtmUpdate(student.studentUid, PtmUpdate(scheduled = false))
                },
                onDismiss = { showPtmDialog = false }
            )
        } else {
            SchedulePtmDialog(
                studentName = student.name ?: "this student",
                alreadyLocked = student.isLocked == true,
                onDismiss = { showPtmDialog = false },
                onConfirm = { scheduledAt, lockProfile, lockReason ->
                    showPtmDialog = false
                    onPtmUpdate(
                        student.studentUid,
                        PtmUpdate(
                            scheduled = true,
                            scheduledAt = scheduledAt,
                            lockProfile = lockProfile,
                            lockReason = lockReason
                        )
                    )
                }
            )
        }
    }

    // ---- Edit identity: name, roll, parent/contact details, class group ----
    if (showEditDialog) {
        EditStudentDetailsDialog(
            student = student,
            onDismiss = { showEditDialog = false },
            onSaved = { update ->
                showEditDialog = false
                onDetailsSaved(student.studentUid, update)
            }
        )
    }
}

// ---------------------------------------------------------------------------
// Student details editor
// ---------------------------------------------------------------------------

/**
 * Edits the identity columns of `students` (and optionally the class group in `enrollments`)
 * through the coordinator service-role client. Groups are loaded lazily because a class can have
 * none, in which case the group picker is hidden.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditStudentDetailsDialog(
    student: StudentProfileView,
    onDismiss: () -> Unit,
    onSaved: (StudentDetailsUpdate) -> Unit
) {
    val scope = rememberCoroutineScope()

    var name by remember(student.studentUid) { mutableStateOf(student.name.orEmpty()) }
    var serialNo by remember(student.studentUid) { mutableStateOf(student.serialNo.orEmpty()) }
    var fatherName by remember(student.studentUid) { mutableStateOf(student.fatherName.orEmpty()) }
    var studentPhone by remember(student.studentUid) { mutableStateOf(student.studentPhoneNo.orEmpty()) }
    var fatherPhone by remember(student.studentUid) { mutableStateOf(student.fatherPhoneNo.orEmpty()) }
    var studentEmail by remember(student.studentUid) { mutableStateOf(student.studentEmail.orEmpty()) }
    var groupId by remember(student.studentUid) { mutableStateOf(student.groupId) }

    var groups by remember(student.studentUid) { mutableStateOf<List<ClassGroupRow>>(emptyList()) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(student.classId) {
        val classId = student.classId ?: return@LaunchedEffect
        groups = CoordinatorAdminRepository.getGroups(classId).getOrDefault(emptyList())
    }

    fun save() {
        val cleanName = name.trim()
        if (cleanName.isEmpty()) {
            error = "Student name can't be empty."
            return
        }

        val selectedGroup = groups.firstOrNull { it.id == groupId }
        val resolvedGroupName = when {
            groupId == null -> null
            selectedGroup != null -> selectedGroup.name
            groupId == student.groupId -> student.groupName
            else -> groups.firstOrNull { it.id == groupId }?.name ?: student.groupName ?: "Group $groupId"
        }

        val update = StudentDetailsUpdate(
            name = cleanName,
            serialNo = serialNo.trim().takeIf { it.isNotEmpty() },
            fatherName = fatherName.trim().takeIf { it.isNotEmpty() },
            studentPhoneNo = studentPhone.trim().takeIf { it.isNotEmpty() },
            fatherPhoneNo = fatherPhone.trim().takeIf { it.isNotEmpty() },
            studentEmail = studentEmail.trim().takeIf { it.isNotEmpty() },
            groupId = groupId,
            groupName = resolvedGroupName
        )

        saving = true
        error = null
        scope.launch {
            val detailsResult = CoordinatorAdminRepository.updateStudentDetails(student.studentUid, update)
            val groupChanged = groupId != student.groupId
            val classId = student.classId

            val groupFailure = if (detailsResult.isSuccess && groupChanged && classId != null) {
                CoordinatorAdminRepository
                    .updateStudentGroup(student.studentUid, classId, groupId)
                    .exceptionOrNull()
            } else {
                null
            }

            saving = false
            val failure = detailsResult.exceptionOrNull() ?: groupFailure
            if (failure != null) {
                error = failure.message ?: "Couldn't save this student's details."
            } else {
                onSaved(update)
            }
        }
    }

    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        title = {
            Text(
                text = "Edit Student Details",
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
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                EditField(value = name, onValueChange = { name = it }, label = "Full name")
                EditField(value = serialNo, onValueChange = { serialNo = it }, label = "Roll / serial no")
                EditField(value = fatherName, onValueChange = { fatherName = it }, label = "Father's name")
                EditField(
                    value = studentPhone,
                    onValueChange = { studentPhone = it },
                    label = "Student phone"
                )
                EditField(
                    value = fatherPhone,
                    onValueChange = { fatherPhone = it },
                    label = "Father's phone"
                )
                EditField(
                    value = studentEmail,
                    onValueChange = { studentEmail = it },
                    label = "Student email"
                )

                if (groups.isNotEmpty()) {
                    Text(
                        text = "CLASS GROUP",
                        color = TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        EditChoiceChip(label = "No group", selected = groupId == null) {
                            groupId = null
                        }
                        groups.forEach { group ->
                            EditChoiceChip(
                                label = group.name,
                                selected = groupId == group.id
                            ) { groupId = group.id }
                        }
                    }
                }

                if (error != null) {
                    Text(text = error.orEmpty(), color = ErrorRed, fontSize = 11.5.sp)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { save() }, enabled = !saving) {
                Text(
                    text = if (saving) "Saving..." else "Save changes",
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
private fun EditField(value: String, onValueChange: (String) -> Unit, label: String) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
            focusedBorderColor = Navy,
            unfocusedBorderColor = DividerSoft,
            focusedLabelColor = Navy,
            unfocusedLabelColor = TextSecondary,
            cursorColor = Navy
        )
    )
}

@Composable
private fun EditChoiceChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(9.dp))
            .background(if (selected) Navy else Slate)
            .clickable(onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 7.dp)
    ) {
        Text(
            text = label,
            color = if (selected) SurfaceWhite else TextPrimary,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

// ---------------------------------------------------------------------------
// PTM / lock dialogs
// ---------------------------------------------------------------------------

/** One PTM change, so the sheet can hand the whole intent to the repository in one update. */
private data class PtmUpdate(
    val scheduled: Boolean,
    val scheduledAt: String? = null,
    val lockProfile: Boolean = false,
    val lockReason: String? = null
)

@Composable
private fun ActionNote(text: String) {
    Text(
        text = text,
        color = TextSecondary,
        fontSize = 11.5.sp,
        lineHeight = 16.sp,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 3.dp)
    )
}

/**
 * Locking always asks for the reason first - that text is what the student reads on the lock
 * screen. The reason is optional so a coordinator can still lock someone quickly.
 */
@Composable
private fun LockProfileDialog(
    studentName: String,
    onDismiss: () -> Unit,
    onConfirm: (String?) -> Unit
) {
    var reason by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceWhite,
        shape = RoundedCornerShape(22.dp),
        icon = {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(ErrorRed.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.Lock,
                    contentDescription = null,
                    tint = ErrorRed,
                    modifier = Modifier.size(22.dp)
                )
            }
        },
        title = {
            Text(
                text = "Lock profile?",
                color = TextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column {
                Text(
                    text = "The student's portal stays closed until you unlock it. " +
                        "Add a reason - it is shown on their lock screen.",
                    color = TextSecondary,
                    fontSize = 12.5.sp,
                    lineHeight = 18.sp
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it.take(240) },
                    label = { Text("Reason (optional)") },
                    placeholder = { Text("e.g. Attendance below 75%") },
                    minLines = 2,
                    maxLines = 4,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Navy,
                        unfocusedBorderColor = DividerSoft,
                        focusedLabelColor = Navy,
                        unfocusedLabelColor = TextSecondary,
                        cursorColor = Navy
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = studentName,
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(reason.trim().takeIf { it.isNotEmpty() }) }) {
                Text("Lock Profile", color = ErrorRed, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}

/**
 * Scheduling a PTM asks for the date, the time, and - because a PTM usually needs the student to
 * stop using the portal that day - whether to lock the profile in the same step. All of it is
 * written by one UPDATE so the PTM can never be saved without the lock it was paired with.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SchedulePtmDialog(
    studentName: String,
    alreadyLocked: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (scheduledAtIso: String, lockProfile: Boolean, lockReason: String?) -> Unit
) {
    val zone = remember { ZoneId.systemDefault() }
    val now = remember { LocalDateTime.now(zone) }

    var date by remember { mutableStateOf(now.toLocalDate().plusDays(1)) }
    var hour by remember { mutableIntStateOf(10) }
    var minute by remember { mutableIntStateOf(0) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var lockProfile by remember { mutableStateOf(!alreadyLocked) }
    var reason by remember { mutableStateOf("") }

    val scheduled = remember(date, hour, minute, zone) {
        date.atTime(hour, minute).atZone(zone)
    }
    val scheduledIso = remember(scheduled) { scheduled.toInstant().toString() }
    val dateLabel = remember(date) {
        date.format(DateTimeFormatter.ofPattern("EEE, dd MMM yyyy", Locale.getDefault()))
    }
    val timeLabel = remember(hour, minute) {
        LocalTime.of(hour, minute).format(DateTimeFormatter.ofPattern("hh:mm a", Locale.getDefault()))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceWhite,
        shape = RoundedCornerShape(22.dp),
        icon = {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Gold.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.CalendarMonth,
                    contentDescription = null,
                    tint = GoldDeep,
                    modifier = Modifier.size(22.dp)
                )
            }
        },
        title = {
            Text(
                text = "Schedule PTM",
                color = TextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text = "Pick when the PTM is scheduled. $studentName sees this on their lock screen.",
                    color = TextSecondary,
                    fontSize = 12.5.sp,
                    lineHeight = 18.sp
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PickerField(
                        label = "Date",
                        value = dateLabel,
                        modifier = Modifier.weight(1f),
                        onClick = { showDatePicker = true }
                    )
                    PickerField(
                        label = "Time",
                        value = timeLabel,
                        modifier = Modifier.weight(1f),
                        onClick = { showTimePicker = true }
                    )
                }

                Spacer(Modifier.height(14.dp))
                Divider(color = DividerSoft, thickness = 1.dp)
                Spacer(Modifier.height(10.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Lock the profile too",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (alreadyLocked) {
                                "This profile is already locked."
                            } else {
                                "The student can't open the app until you unlock it."
                            },
                            color = TextSecondary,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                    Switch(
                        checked = lockProfile || alreadyLocked,
                        onCheckedChange = { lockProfile = it },
                        enabled = !alreadyLocked,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = SurfaceWhite,
                            checkedTrackColor = Navy,
                            uncheckedThumbColor = SurfaceWhite,
                            uncheckedTrackColor = DividerSoft
                        )
                    )
                }

                if (lockProfile && !alreadyLocked) {
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = reason,
                        onValueChange = { reason = it.take(240) },
                        label = { Text("Lock reason (optional)") },
                        placeholder = { Text("e.g. PTM on campus - portal paused") },
                        minLines = 2,
                        maxLines = 3,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Navy,
                            unfocusedBorderColor = DividerSoft,
                            focusedLabelColor = Navy,
                            unfocusedLabelColor = TextSecondary,
                            cursorColor = Navy
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        scheduledIso,
                        lockProfile || alreadyLocked,
                        reason.trim().takeIf { it.isNotEmpty() }
                    )
                }
            ) {
                Text("Schedule", color = Navy, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = date
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            date = Instant.ofEpochMilli(millis)
                                .atZone(ZoneOffset.UTC)
                                .toLocalDate()
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK", color = Navy, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        ) {
            DatePicker(
                state = pickerState,
                title = null,
                headline = null,
                showModeToggle = false
            )
        }
    }

    if (showTimePicker) {
        val timeState = rememberTimePickerState(
            initialHour = hour,
            initialMinute = minute,
            is24Hour = false
        )
        Dialog(
            onDismissRequest = { showTimePicker = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = SurfaceWhite
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Select time",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    TimePicker(state = timeState)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showTimePicker = false }) {
                            Text("Cancel", color = TextSecondary)
                        }
                        TextButton(
                            onClick = {
                                hour = timeState.hour
                                minute = timeState.minute
                                showTimePicker = false
                            }
                        ) {
                            Text("OK", color = Navy, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PickerField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, DividerSoft, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(
            text = label.uppercase(),
            color = TextSecondary,
            fontSize = 9.5.sp,
            letterSpacing = 1.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = value,
            color = TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

// ---------------------------------------------------------------------------
// Reusable sheet components
// ---------------------------------------------------------------------------

@Composable
private fun DetailSectionLabel(title: String, icon: ImageVector) {
    Row(
        modifier = Modifier.padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Navy, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(
            title.uppercase(),
            color = TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp
        )
    }
}

@Composable
private fun InfoChip(label: String, value: String, color: Color) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.08f))
            .border(1.dp, color.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(label, color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Medium)
        Text(value, color = color, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ContactRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Navy.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Navy, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(label, color = TextSecondary, fontSize = 11.sp)
            Text(value, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun ActionToggleRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    checked: Boolean,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Slate),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = TextSecondary, fontSize = 12.sp)
            }
            Switch(
                checked = checked,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = SurfaceWhite,
                    checkedTrackColor = iconTint,
                    uncheckedThumbColor = SurfaceWhite,
                    uncheckedTrackColor = TextSecondary.copy(alpha = 0.3f)
                )
            )
        }
    }
}

@Composable
private fun AttendanceSubjectRow(row: AttendanceSummaryView) {
    val pct = (row.percentage / 100f).toFloat().coerceIn(0f, 1f)
    val barColor = when {
        row.percentage >= 75 -> SuccessGreen
        row.percentage >= 60 -> WarningAmber
        else -> ErrorRed
    }
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        row.subjectName,
                        color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        row.subjectCode,
                        color = TextSecondary, fontSize = 11.sp
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    "${row.attended}/${row.total}",
                    color = TextSecondary, fontSize = 12.sp
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "${"%.1f".format(row.percentage)}%",
                    color = barColor, fontSize = 14.sp, fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { pct },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = barColor,
                trackColor = barColor.copy(alpha = 0.12f)
            )
        }
    }
}

@Composable
private fun MarksSubjectSection(subject: String, rows: List<MarkView>) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                subject,
                color = Navy, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                maxLines = 2, overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(10.dp))
            rows.forEach { mark ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${mark.kind} ${if (mark.number > 0) mark.number else ""}".trim(),
                        color = TextSecondary, fontSize = 12.sp, modifier = Modifier.weight(1f)
                    )
                    val display = if (mark.marks != null && mark.maxMarks != null)
                        "${"%.1f".format(mark.marks)} / ${"%.0f".format(mark.maxMarks)}"
                    else "—"
                    Text(display, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                if (rows.last() != mark) Divider(color = DividerSoft, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 2.dp))
            }
        }
    }
}

@Composable
private fun EmptyDetailNote(message: String) {
    Text(
        message,
        color = TextSecondary,
        fontSize = 13.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
    )
}

@Composable
private fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    confirmColor: Color,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold, color = TextPrimary) },
        text = { Text(message, color = TextSecondary, fontSize = 14.sp) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = confirmColor)
            ) {
                Text(confirmLabel, color = SurfaceWhite, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        },
        containerColor = SurfaceWhite,
        shape = RoundedCornerShape(20.dp)
    )
}

// ---------------------------------------------------------------------------
// Generic states
// ---------------------------------------------------------------------------

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Icon(Icons.Outlined.PersonOff, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(12.dp))
            Text("Couldn't load students", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Text(message, color = TextSecondary, fontSize = 13.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onRetry,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Navy)
            ) {
                Text("Retry", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun EmptyState(icon: ImageVector, title: String, subtitle: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Icon(icon, contentDescription = null, tint = Navy.copy(alpha = 0.3f), modifier = Modifier.size(60.dp))
            Spacer(Modifier.height(14.dp))
            Text(title, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Text(subtitle, color = TextSecondary, fontSize = 13.sp, textAlign = TextAlign.Center)
        }
    }
}
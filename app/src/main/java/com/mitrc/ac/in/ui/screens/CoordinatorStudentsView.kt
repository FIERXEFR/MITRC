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
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Email
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mitrc.ac.`in`.data.AttendanceSummaryView
import com.mitrc.ac.`in`.data.MarkView
import com.mitrc.ac.`in`.data.PortalRepository
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
    var errorMsg by remember { mutableStateOf<String?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Identity (name, roll, phone, email, group, lock/PTM flags) is read from the `students`
    // table; the class mapping comes from v_student_directory. The repository merges the two,
    // with `students` winning on every identity field.
    suspend fun loadStudents() {
        errorMsg = null
        val classIds = PortalRepository.getMyCoordinatorClasses()
            .getOrElse { emptyList() }
            .map { it.classId }

        PortalRepository.getStudentsForClasses(classIds)
            .onSuccess { allStudents = it }
            .onFailure { errorMsg = it.message }
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
                loading -> LoadingState()
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
                filtered.isEmpty() && allStudents.isEmpty() -> EmptyState(
                    icon = Icons.Outlined.Group,
                    title = "No Students Yet",
                    subtitle = "No students are enrolled in your assigned classes."
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
                onLockToggle = { uid, locked ->
                    scope.launch {
                        PortalRepository.updateStudentLockStatus(uid, locked)
                            .onSuccess {
                                allStudents = allStudents.map { s ->
                                    if (s.studentUid == uid) s.copy(isLocked = locked) else s
                                }
                                selectedStudent = selectedStudent?.let {
                                    if (it.studentUid == uid) it.copy(isLocked = locked) else it
                                }
                            }
                    }
                },
                onPtmToggle = { uid, ptm ->
                    scope.launch {
                        PortalRepository.updateStudentPtmStatus(uid, ptm)
                            .onSuccess {
                                allStudents = allStudents.map { s ->
                                    if (s.studentUid == uid) s.copy(isPtm = ptm) else s
                                }
                                selectedStudent = selectedStudent?.let {
                                    if (it.studentUid == uid) it.copy(isPtm = ptm) else it
                                }
                            }
                    }
                }
            )
        }
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
                "Search by name, roll no, sectionâ€¦",
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
                        student.section?.let { if (isNotEmpty()) append(" Â· "); append("Sec $it") }
                        student.semester?.let { if (isNotEmpty()) append(" Â· "); append("Sem $it") }
                    },
                    color = TextSecondary,
                    fontSize = 12.sp
                )
                Spacer(Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    student.branch?.let { StudentChip(it, Navy) }
                    student.academicYear?.let { StudentChip(it, GoldDeep) }
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
    onLockToggle: (String, Boolean) -> Unit,
    onPtmToggle: (String, Boolean) -> Unit
) {
    val scope = rememberCoroutineScope()
    var attendance by remember { mutableStateOf<List<AttendanceSummaryView>>(emptyList()) }
    var marks by remember { mutableStateOf<List<MarkView>>(emptyList()) }
    var loadingDetails by remember { mutableStateOf(true) }

    // confirmation dialogs
    var showLockDialog by remember { mutableStateOf(false) }
    var showPtmDialog by remember { mutableStateOf(false) }

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
        Spacer(Modifier.height(8.dp))
        ActionToggleRow(
            icon = Icons.Outlined.NotificationsActive,
            iconTint = if (student.isPtm == true) Gold else TextSecondary,
            title = "Schedule PTM",
            subtitle = if (student.isPtm == true) "PTM has been scheduled" else "No PTM scheduled",
            checked = student.isPtm == true,
            onToggle = { showPtmDialog = true }
        )

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

    // ---- Lock confirmation dialog ---- //
    if (showLockDialog) {
        val targetLocked = student.isLocked != true
        ConfirmDialog(
            title = if (targetLocked) "Lock Profile?" else "Unlock Profile?",
            message = if (targetLocked)
                "Locking ${student.name}'s profile will restrict their access. Continue?"
            else
                "Unlocking will restore ${student.name}'s access. Continue?",
            confirmLabel = if (targetLocked) "Lock" else "Unlock",
            confirmColor = if (targetLocked) ErrorRed else SuccessGreen,
            onConfirm = {
                showLockDialog = false
                onLockToggle(student.studentUid, targetLocked)
            },
            onDismiss = { showLockDialog = false }
        )
    }

    // ---- PTM confirmation dialog ----
    if (showPtmDialog) {
        val targetPtm = student.isPtm != true
        ConfirmDialog(
            title = if (targetPtm) "Schedule PTM?" else "Cancel PTM?",
            message = if (targetPtm)
                "This will mark a PTM for ${student.name}. Parents will be notified."
            else
                "This will cancel the scheduled PTM for ${student.name}.",
            confirmLabel = if (targetPtm) "Schedule" else "Cancel PTM",
            confirmColor = if (targetPtm) Gold else ErrorRed,
            onConfirm = {
                showPtmDialog = false
                onPtmToggle(student.studentUid, targetPtm)
            },
            onDismiss = { showPtmDialog = false }
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
                    else "â€”"
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
private fun LoadingState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = Navy, strokeWidth = 3.dp)
            Spacer(Modifier.height(12.dp))
            Text("Loading studentsâ€¦", color = TextSecondary, fontSize = 14.sp)
        }
    }
}

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
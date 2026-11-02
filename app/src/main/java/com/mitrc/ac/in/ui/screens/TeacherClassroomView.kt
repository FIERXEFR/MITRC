package com.mitrc.ac.`in`.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.FactCheck
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mitrc.ac.`in`.data.AssessmentRow
import com.mitrc.ac.`in`.data.ClassSubjectView
import com.mitrc.ac.`in`.data.MarkRow
import com.mitrc.ac.`in`.data.PortalRepository
import com.mitrc.ac.`in`.data.StudentProfileView
import com.mitrc.ac.`in`.data.TakeAttendanceRecord
import com.mitrc.ac.`in`.data.TeacherClassInfo
import com.mitrc.ac.`in`.ui.theme.DividerSoft
import com.mitrc.ac.`in`.ui.theme.ErrorRed
import com.mitrc.ac.`in`.ui.theme.Gold
import com.mitrc.ac.`in`.ui.theme.Navy
import com.mitrc.ac.`in`.ui.theme.NavyDeep
import com.mitrc.ac.`in`.ui.theme.Slate
import com.mitrc.ac.`in`.ui.theme.SuccessGreen
import com.mitrc.ac.`in`.ui.theme.SurfaceWhite
import com.mitrc.ac.`in`.ui.theme.TextPrimary
import com.mitrc.ac.`in`.ui.theme.TextSecondary
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch

enum class TeacherWorkMode(val label: String) {
    ATTENDANCE("Attendance"),
    MARKS("Marks"),
    MATERIALS("Materials"),
}

private val TeacherWorkMode.icon
    get() = when (this) {
        TeacherWorkMode.ATTENDANCE -> Icons.Outlined.FactCheck
        TeacherWorkMode.MARKS -> Icons.Outlined.Analytics
        TeacherWorkMode.MATERIALS -> Icons.Outlined.MenuBook
    }

private val TeacherIstZone: ZoneId = ZoneId.of("Asia/Kolkata")
private val TeacherDatePattern: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEE, dd MMM yyyy", Locale.US)
private val TeacherDayChipPattern: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEE", Locale.US)
private val TeacherMonthChipPattern: DateTimeFormatter =
    DateTimeFormatter.ofPattern("MMM", Locale.US)
private val TeacherPresentGreen = Color(0xFF15803D)

private fun teacherTodayIst(): LocalDate = LocalDate.now(TeacherIstZone)

private fun teacherDateLabel(raw: String): String =
    runCatching { LocalDate.parse(raw).format(TeacherDatePattern) }.getOrDefault(raw)

private fun teacherRelativeDay(raw: String): String {
    val date = runCatching { LocalDate.parse(raw) }.getOrNull() ?: return "Selected day"
    val today = teacherTodayIst()
    return when {
        date == today -> "Today"
        date == today.minusDays(1) -> "Yesterday"
        date == today.minusDays(2) -> "2 days ago"
        else -> "${date.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }}"
    }
}

@Composable
fun TeacherClassroomView(
    subjects: List<ClassSubjectView>,
    selectedSubject: ClassSubjectView?,
    classInfo: Map<Int, TeacherClassInfo>,
    onSelectSubject: (ClassSubjectView) -> Unit,
    activeMode: TeacherWorkMode,
    onModeChange: (TeacherWorkMode) -> Unit,
) {
    var selectedDate by rememberSaveable { mutableStateOf(teacherTodayIst().toString()) }

    var students by remember { mutableStateOf<List<StudentProfileView>>(emptyList()) }
    var loadingStudents by remember { mutableStateOf(false) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    val attendanceMap = remember { mutableStateMapOf<String, String>() }

    var saving by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var reloadTick by remember { mutableIntStateOf(0) }
    var classDataRefreshTick by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    val loadClassroom: suspend () -> Unit = {
        val subject = selectedSubject
        statusMessage = null
        errorMessage = null
        if (subject == null) {
            students = emptyList()
            attendanceMap.clear()
            loadingStudents = false
            loadError = null
        } else {
            loadingStudents = true
            loadError = null
            val studentResult = PortalRepository.getEnrolledStudentsForClass(subject.classId)
            students = studentResult
                .getOrDefault(emptyList())
                .sortedBy { it.name?.lowercase() ?: "" }
            loadError = studentResult.exceptionOrNull()?.message

            val existing = PortalRepository.getExistingAttendance(
                subject.classSubjectId,
                selectedDate,
                period = 1,
            ).getOrDefault(emptyList())

            attendanceMap.clear()
            students.forEach { student ->
                attendanceMap[student.uid] = existing
                    .firstOrNull { it.studentUid == student.uid }
                    ?.status
                    ?: "P"
            }
            loadingStudents = false
        }
    }

    LaunchedEffect(selectedSubject?.classSubjectId, selectedDate, reloadTick) {
        loadClassroom()
    }

    val filteredStudents = remember(students, searchQuery) {
        val query = searchQuery.trim().lowercase()
        if (query.isEmpty()) {
            students
        } else {
            students.filter { student ->
                student.name?.lowercase()?.contains(query) == true ||
                    student.serialNo?.lowercase()?.contains(query) == true ||
                    student.groupName?.lowercase()?.contains(query) == true
            }
        }
    }

    val presentCount = students.count { attendanceMap[it.uid] == "P" }
    val absentCount = students.count { attendanceMap[it.uid] == "A" }
    val leaveCount = students.count { attendanceMap[it.uid] == "L" }
    val onDutyCount = students.count { attendanceMap[it.uid] == "OD" }

    TeacherRefreshBox(
        onRefresh = {
            loadClassroom()
            classDataRefreshTick++
        },
        skeleton = { TeacherTabSkeleton(TeacherTabSkeletonKind.Classroom) },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Slate),
        ) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item {
                    TeacherClassPickerCard(
                        subjects = subjects,
                        selectedSubject = selectedSubject,
                        classInfo = classInfo,
                        label = when (activeMode) {
                            TeacherWorkMode.ATTENDANCE -> "ATTENDANCE FOR"
                            TeacherWorkMode.MARKS -> "MARKING FOR"
                            TeacherWorkMode.MATERIALS -> "MATERIALS FOR"
                        },
                        compact = true,
                        onSelectSubject = onSelectSubject,
                    )
                }

                item {
                    TeacherModeSwitch(
                        activeMode = activeMode,
                        onModeChange = {
                            onModeChange(it)
                            statusMessage = null
                            errorMessage = null
                        },
                    )
                }

                if (activeMode == TeacherWorkMode.ATTENDANCE) {
                    item {
                        AttendanceDateCard(
                            selectedDate = selectedDate,
                            onDateChange = {
                                selectedDate = it
                                statusMessage = null
                                errorMessage = null
                            },
                        )
                    }

                    item {
                        AttendanceSummaryRow(
                            present = presentCount,
                            absent = absentCount,
                            leave = leaveCount,
                            onDuty = onDutyCount,
                        )
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "STUDENTS (${filteredStudents.size})",
                                color = TextSecondary,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp,
                                modifier = Modifier.weight(1f),
                            )
                            TextButton(
                                onClick = {
                                    students.forEach { attendanceMap[it.uid] = "P" }
                                    statusMessage = "All students marked present. Save to record it."
                                    errorMessage = null
                                },
                                enabled = students.isNotEmpty(),
                            ) {
                                Text(
                                    text = "Mark all present",
                                    color = Navy,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            singleLine = true,
                            placeholder = { Text("Search by name, roll no. or group") },
                            leadingIcon = {
                                Icon(Icons.Outlined.Search, contentDescription = null, tint = TextSecondary)
                            },
                            trailingIcon = {
                                if (searchQuery.isNotBlank()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(
                                            Icons.Outlined.Close,
                                            contentDescription = "Clear student search",
                                            tint = TextSecondary,
                                        )
                                    }
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = teacherFieldColors(),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    when {
                        loadingStudents -> item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 28.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(
                                        color = Navy,
                                        strokeWidth = 2.5.dp,
                                        modifier = Modifier.size(28.dp),
                                    )
                                    Spacer(Modifier.height(10.dp))
                                    Text(
                                        text = "Loading the class roster...",
                                        color = TextSecondary,
                                        fontSize = 12.sp,
                                    )
                                }
                            }
                        }

                        loadError != null && students.isEmpty() -> item {
                            TeacherEmptyState(
                                icon = Icons.Outlined.Groups,
                                title = "Could not load students",
                                body = loadError.orEmpty(),
                                actionLabel = "Try again",
                                onAction = { reloadTick++ },
                            )
                        }

                        students.isEmpty() -> item {
                            TeacherEmptyState(
                                icon = Icons.Outlined.Groups,
                                title = "No students enrolled",
                                body = "Students will appear here once they are enrolled in this class.",
                            )
                        }

                        filteredStudents.isEmpty() -> item {
                            TeacherEmptyState(
                                icon = Icons.Outlined.Search,
                                title = "No matching students",
                                body = "Try a different name, roll number or group.",
                                actionLabel = "Clear search",
                                onAction = { searchQuery = "" },
                            )
                        }

                        else -> items(filteredStudents, key = { it.uid }) { student ->
                            StudentAttendanceCard(
                                student = student,
                                currentStatus = attendanceMap[student.uid] ?: "P",
                                onStatusChange = { status ->
                                    attendanceMap[student.uid] = status
                                    statusMessage = null
                                    errorMessage = null
                                },
                            )
                        }
                    }

                    item { Spacer(Modifier.height(6.dp)) }
                } else if (activeMode == TeacherWorkMode.MARKS) {
                    item {
                        TeacherMarksPanel(
                            subject = selectedSubject,
                            refreshKey = classDataRefreshTick,
                        )
                    }
                } else {
                    item {
                        TeacherResourcesView(
                            selectedSubject = selectedSubject,
                            refreshKey = classDataRefreshTick,
                        )
                    }
                }
            }

        AnimatedVisibility(
            visible = activeMode == TeacherWorkMode.ATTENDANCE,
            enter = expandVertically(animationSpec = tween(180, easing = FastOutSlowInEasing)) + fadeIn(tween(160)),
            exit = shrinkVertically(animationSpec = tween(140)) + fadeOut(tween(120)),
        ) {
            AttendanceSaveBar(
                present = presentCount,
                absent = absentCount,
                total = students.size,
                saving = saving,
                statusMessage = statusMessage,
                errorMessage = errorMessage,
                enabled = selectedSubject != null && students.isNotEmpty(),
                onSave = {
                    val subject = selectedSubject
                    when {
                        subject == null -> Unit
                        selectedDate > teacherTodayIst().toString() -> {
                            errorMessage = "Attendance cannot be taken for a future date."
                        }
                        else -> {
                            val dateToSave = selectedDate
                            val records = students.map { student ->
                                TakeAttendanceRecord(
                                    uid = student.uid,
                                    status = attendanceMap[student.uid] ?: "P",
                                )
                            }

                            saving = true
                            statusMessage = null
                            errorMessage = null
                            scope.launch {
                                val result = PortalRepository.saveAttendance(
                                    classSubjectId = subject.classSubjectId,
                                    classDate = dateToSave,
                                    period = 1,
                                    records = records,
                                )
                                saving = false
                                result.fold(
                                    onSuccess = {
                                        statusMessage = "Attendance saved for ${teacherDateLabel(dateToSave)}."
                                    },
                                    onFailure = { error ->
                                        val message = error.message.orEmpty()
                                        errorMessage = when {
                                            message.contains("42501") ||
                                                message.contains("row-level security", ignoreCase = true) ->
                                                "You are not assigned to this subject."
                                            message.isBlank() -> "Attendance could not be saved."
                                            else -> message
                                        }
                                    },
                                )
                            }
                        }
                    }
                },
            )
        }
    }
    }
}

@Composable
private fun TeacherModeSwitch(
    activeMode: TeacherWorkMode,
    onModeChange: (TeacherWorkMode) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = SurfaceWhite,
        border = androidx.compose.foundation.BorderStroke(1.dp, DividerSoft),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            TeacherWorkMode.entries.forEach { mode ->
                val selected = activeMode == mode
                val background by animateColorAsState(
                    targetValue = if (selected) Navy else Color.Transparent,
                    animationSpec = tween(180),
                    label = "teacherModeBackground",
                )
                val foreground by animateColorAsState(
                    targetValue = if (selected) Color.White else TextSecondary,
                    animationSpec = tween(180),
                    label = "teacherModeForeground",
                )

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(background)
                        .clickable { onModeChange(mode) }
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = mode.icon,
                        contentDescription = null,
                        tint = foreground,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(7.dp))
                    Text(
                        text = mode.label,
                        color = foreground,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun AttendanceDateCard(
    selectedDate: String,
    onDateChange: (String) -> Unit,
) {
    val parsedDate = runCatching { LocalDate.parse(selectedDate) }.getOrDefault(teacherTodayIst())
    val today = teacherTodayIst()
    val stripEnd = minOf(parsedDate.plusDays(3), today)
    val stripStart = stripEnd.minusDays(6)
    val stripDates = (0L..6L).map { stripStart.plusDays(it) }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = SurfaceWhite,
        border = androidx.compose.foundation.BorderStroke(1.dp, DividerSoft),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = { onDateChange(parsedDate.minusDays(1).toString()) },
                    enabled = !parsedDate.isBefore(today.minusDays(60)),
                ) {
                    Icon(Icons.Outlined.ArrowBack, contentDescription = "Previous day", tint = Navy)
                }

                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "ATTENDANCE DATE",
                        color = TextSecondary,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = teacherDateLabel(selectedDate),
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = teacherRelativeDay(selectedDate),
                        color = if (parsedDate == today) SuccessGreen else TextSecondary,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                IconButton(
                    onClick = { onDateChange(parsedDate.plusDays(1).toString()) },
                    enabled = parsedDate.isBefore(today),
                ) {
                    Icon(
                        Icons.Outlined.ArrowForward,
                        contentDescription = "Next day",
                        tint = if (parsedDate.isBefore(today)) Navy else TextSecondary.copy(alpha = 0.35f),
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            TeacherDivider()
            Spacer(Modifier.height(12.dp))

            Text(
                text = "PICK A DATE",
                color = TextSecondary,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
            )
            Spacer(Modifier.height(7.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                stripDates.forEach { date ->
                    val selected = date == parsedDate
                    val isToday = date == today
                    val shape = RoundedCornerShape(12.dp)
                    Column(
                        modifier = Modifier
                            .width(58.dp)
                            .heightIn(min = 66.dp)
                            .clip(shape)
                            .background(if (selected) Navy else Slate)
                            .border(
                                1.dp,
                                when {
                                    selected -> Navy
                                    isToday -> Gold.copy(alpha = 0.65f)
                                    else -> DividerSoft
                                },
                                shape,
                            )
                            .clickable { onDateChange(date.toString()) }
                            .padding(vertical = 7.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = date.format(TeacherDayChipPattern).uppercase(Locale.US),
                            color = if (selected) Color.White.copy(alpha = 0.72f) else TextSecondary,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                        )
                        Text(
                            text = date.dayOfMonth.toString().padStart(2, '0'),
                            color = if (selected) Color.White else TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                        )
                        Text(
                            text = if (isToday) "TODAY" else date.format(TeacherMonthChipPattern).uppercase(Locale.US),
                            color = if (selected) Gold else TextSecondary,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.4.sp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AttendanceSummaryRow(
    present: Int,
    absent: Int,
    leave: Int,
    onDuty: Int,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        AttendanceSummaryTile("P", present, TeacherPresentGreen, Modifier.weight(1f))
        AttendanceSummaryTile("A", absent, ErrorRed, Modifier.weight(1f))
        AttendanceSummaryTile("L", leave, Gold, Modifier.weight(1f))
        AttendanceSummaryTile("OD", onDuty, Navy, Modifier.weight(1f))
    }
}

@Composable
private fun AttendanceSummaryTile(
    code: String,
    count: Int,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = accent.copy(alpha = 0.08f),
        border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.18f)),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = count.toString(),
                color = if (code == "L") NavyDeep else accent,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
            )
            Text(
                text = code,
                color = TextSecondary,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun StudentAttendanceCard(
    student: StudentProfileView,
    currentStatus: String,
    onStatusChange: (String) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = SurfaceWhite,
        border = androidx.compose.foundation.BorderStroke(1.dp, DividerSoft),
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = tween(180, easing = FastOutSlowInEasing)),
    ) {
        Column(modifier = Modifier.padding(13.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Navy.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = teacherInitials(student.name),
                        color = Navy,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
                Spacer(Modifier.width(11.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = student.name?.trim()?.takeIf { it.isNotEmpty() } ?: "Student",
                        color = TextPrimary,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = buildString {
                            append("Roll ")
                            append(student.serialNo?.trim()?.takeIf { it.isNotEmpty() } ?: "—")
                            student.groupName?.trim()?.takeIf { it.isNotEmpty() }?.let {
                                append("  ·  Group ")
                                append(it)
                            }
                        },
                        color = TextSecondary,
                        fontSize = 10.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                OnlineStatusPill(status = currentStatus)
            }

            Spacer(Modifier.height(11.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                listOf(
                    "P" to "Present",
                    "A" to "Absent",
                    "L" to "Leave",
                    "OD" to "On duty",
                ).forEach { (code, label) ->
                    AttendanceStatusButton(
                        code = code,
                        label = label,
                        selected = currentStatus == code,
                        onClick = { onStatusChange(code) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun OnlineStatusPill(status: String) {
    val (label, color) = when (status) {
        "P" -> "Present" to TeacherPresentGreen
        "A" -> "Absent" to ErrorRed
        "L" -> "Leave" to Gold
        else -> "On duty" to Navy
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(7.dp))
            .background(color.copy(alpha = 0.11f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Text(
            text = label,
            color = if (status == "L") NavyDeep else color,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun AttendanceStatusButton(
    code: String,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = when (code) {
        "P" -> TeacherPresentGreen
        "A" -> ErrorRed
        "L" -> Gold
        else -> Navy
    }
    val background by animateColorAsState(
        targetValue = if (selected) accent else Slate,
        animationSpec = tween(160),
        label = "attendanceStatusBackground",
    )
    val foreground by animateColorAsState(
        targetValue = if (selected) {
            if (code == "L") NavyDeep else Color.White
        } else {
            TextSecondary
        },
        animationSpec = tween(160),
        label = "attendanceStatusForeground",
    )

    Box(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(background)
            .border(
                1.dp,
                if (selected) accent else DividerSoft,
                RoundedCornerShape(11.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = code,
                color = foreground,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
            )
            Text(
                text = label,
                color = foreground.copy(alpha = 0.82f),
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun AttendanceSaveBar(
    present: Int,
    absent: Int,
    total: Int,
    saving: Boolean,
    statusMessage: String?,
    errorMessage: String?,
    enabled: Boolean,
    onSave: () -> Unit,
) {
    Surface(
        color = SurfaceWhite,
        shadowElevation = 12.dp,
        tonalElevation = 6.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            TeacherStatusBanner(statusMessage, isError = false)
            TeacherStatusBanner(errorMessage, isError = true)

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (total == 0) "No students to save" else "$present present · $absent absent",
                        color = TextPrimary,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = if (total == 0) "Select an enrolled class first." else "Daily attendance · $total students",
                        color = TextSecondary,
                        fontSize = 10.sp,
                    )
                }

                Button(
                    onClick = onSave,
                    enabled = enabled && !saving,
                    shape = RoundedCornerShape(13.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Navy,
                        contentColor = Color.White,
                        disabledContainerColor = Navy.copy(alpha = 0.35f),
                        disabledContentColor = Color.White.copy(alpha = 0.8f),
                    ),
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    if (saving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Icon(Icons.Outlined.Save, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Save", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun TeacherMarksPanel(
    subject: ClassSubjectView?,
    refreshKey: Int = 0,
) {
    if (subject == null) {
        TeacherEmptyState(
            icon = Icons.Outlined.Analytics,
            title = "Choose a class",
            body = "Select one of your assigned classes above to enter marks.",
        )
        return
    }

    val scope = rememberCoroutineScope()
    var assessments by remember(subject.classSubjectId) { mutableStateOf<List<AssessmentRow>>(emptyList()) }
    var selectedAssessment by remember(subject.classSubjectId) { mutableStateOf<AssessmentRow?>(null) }
    var kind by remember(subject.classSubjectId) { mutableStateOf("UT") }
    var numberStr by remember(subject.classSubjectId) { mutableStateOf("1") }
    var maxMarksStr by remember(subject.classSubjectId) { mutableStateOf("20.0") }
    var students by remember(subject.classSubjectId) { mutableStateOf<List<StudentProfileView>>(emptyList()) }
    val marksMap = remember(subject.classSubjectId) { mutableStateMapOf<String, String>() }
    var saving by remember(subject.classSubjectId) { mutableStateOf(false) }
    var assessmentMessage by remember(subject.classSubjectId) { mutableStateOf<String?>(null) }
    var assessmentError by remember(subject.classSubjectId) { mutableStateOf<String?>(null) }
    var marksMessage by remember(subject.classSubjectId) { mutableStateOf<String?>(null) }
    var marksError by remember(subject.classSubjectId) { mutableStateOf<String?>(null) }

    LaunchedEffect(subject.classSubjectId, refreshKey) {
        val assessmentResult = PortalRepository.getAssessments(subject.classSubjectId)
        assessments = assessmentResult.getOrDefault(emptyList())
        selectedAssessment = assessments.firstOrNull()

        val studentResult = PortalRepository.getEnrolledStudentsForClass(subject.classId)
        students = studentResult
            .getOrDefault(emptyList())
            .sortedBy { it.name?.lowercase() ?: "" }
    }

    LaunchedEffect(selectedAssessment?.id, refreshKey) {
        marksMap.clear()
        marksError = null
        marksMessage = null
        val assessmentId = selectedAssessment?.id ?: return@LaunchedEffect
        PortalRepository.getMarksForAssessment(assessmentId).fold(
            onSuccess = { rows ->
                rows.forEach { row ->
                    marksMap[row.studentUid] = row.marks?.let { value ->
                        if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()
                    }.orEmpty()
                }
            },
            onFailure = { error ->
                marksError = error.message?.ifBlank { "Existing marks could not be loaded." }
                    ?: "Existing marks could not be loaded."
            },
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = SurfaceWhite,
            border = androidx.compose.foundation.BorderStroke(1.dp, DividerSoft),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(15.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "ASSESSMENT",
                    color = TextSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.1.sp,
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    listOf("UT", "EXP", "ASSIGNMENT", "MID", "END").forEach { option ->
                        TeacherChoiceChip(
                            label = option,
                            selected = kind == option,
                            onClick = { kind = option },
                        )
                    }
                }

                if (assessments.isNotEmpty()) {
                    Text(
                        text = "SAVED ASSESSMENTS",
                        color = TextSecondary,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        assessments.forEach { assessment ->
                            TeacherChoiceChip(
                                label = "${assessment.kind} #${assessment.number}",
                                selected = selectedAssessment?.id == assessment.id,
                                onClick = { selectedAssessment = assessment },
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                ) {
                    OutlinedTextField(
                        value = numberStr,
                        onValueChange = { numberStr = it },
                        label = { Text("Number") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = teacherFieldColors(),
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = maxMarksStr,
                        onValueChange = { maxMarksStr = it },
                        label = { Text("Max marks") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = teacherFieldColors(),
                        modifier = Modifier.weight(1f),
                    )
                }

                TeacherPrimaryButton(
                    label = "Create assessment",
                    icon = Icons.Outlined.EventAvailable,
                    loading = saving,
                    onClick = {
                        val number = numberStr.toIntOrNull() ?: 1
                        val maxMarks = maxMarksStr.toDoubleOrNull() ?: 20.0
                        saving = true
                        assessmentMessage = null
                        assessmentError = null
                        scope.launch {
                            val result = PortalRepository.createAssessment(
                                AssessmentRow(
                                    classSubjectId = subject.classSubjectId,
                                    kind = kind,
                                    number = number,
                                    maxMarks = maxMarks,
                                ),
                            )
                            saving = false
                            result.fold(
                                onSuccess = { created ->
                                    selectedAssessment = created
                                    assessmentMessage = "Assessment ready."
                                    assessments = PortalRepository
                                        .getAssessments(subject.classSubjectId)
                                        .getOrDefault(emptyList())
                                },
                                onFailure = { err ->
                                    assessmentError = err.message?.ifBlank { "Could not create assessment." }
                                        ?: "Could not create assessment."
                                },
                            )
                        }
                    },
                )

                TeacherStatusBanner(assessmentMessage, isError = false)
                TeacherStatusBanner(assessmentError, isError = true)
            }
        }

        val assessment = selectedAssessment
        if (assessment != null && students.isNotEmpty()) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = SurfaceWhite,
                border = androidx.compose.foundation.BorderStroke(1.dp, DividerSoft),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(15.dp),
                    verticalArrangement = Arrangement.spacedBy(9.dp),
                ) {
                    Text(
                        text = "SCORES · ${assessment.kind} #${assessment.number}",
                        color = TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.1.sp,
                    )

                    students.forEach { student ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Slate)
                                .padding(horizontal = 11.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = student.name?.trim()?.takeIf { it.isNotEmpty() } ?: "Student",
                                    color = TextPrimary,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = "Roll ${student.serialNo?.trim()?.takeIf { it.isNotEmpty() } ?: "—"}",
                                    color = TextSecondary,
                                    fontSize = 10.sp,
                                )
                            }
                            OutlinedTextField(
                                value = marksMap[student.uid] ?: "",
                                onValueChange = { marksMap[student.uid] = it },
                                placeholder = { Text("Marks") },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                colors = teacherFieldColors(),
                                modifier = Modifier.width(92.dp),
                            )
                        }
                    }

                    TeacherStatusBanner(marksMessage, isError = false)
                    TeacherStatusBanner(marksError, isError = true)

                    TeacherPrimaryButton(
                        label = "Save marks",
                        icon = Icons.Outlined.Save,
                        loading = saving,
                        onClick = {
                            val markRows = students.mapNotNull { student ->
                                val score = marksMap[student.uid]?.toDoubleOrNull()
                                    ?: return@mapNotNull null
                                MarkRow(
                                    assessmentId = assessment.id,
                                    studentUid = student.uid,
                                    marks = score,
                                )
                            }
                            if (markRows.isEmpty()) {
                                marksError = "Enter at least one score before saving."
                            } else {
                                saving = true
                                marksMessage = null
                                marksError = null
                                scope.launch {
                                    val result = PortalRepository.saveMarks(markRows)
                                    saving = false
                                    result.fold(
                                        onSuccess = { marksMessage = "Marks saved successfully." },
                                        onFailure = { err ->
                                            marksError = err.message?.ifBlank { "Marks could not be saved." }
                                                ?: "Marks could not be saved."
                                        },
                                    )
                                }
                            }
                        },
                    )
                }
            }
        } else if (students.isEmpty()) {
            TeacherEmptyState(
                icon = Icons.Outlined.Person,
                title = "No students enrolled",
                body = "Marks can be entered once students are enrolled in this class.",
            )
        } else {
            TeacherEmptyState(
                icon = Icons.Outlined.Analytics,
                title = "Create an assessment",
                body = "Choose a type and create an assessment slot to begin entering marks.",
            )
        }
    }
}


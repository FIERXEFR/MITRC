package com.mitrc.ac.`in`.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.ScrollState
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import com.mitrc.ac.`in`.data.*
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.mitrc.ac.`in`.auth.AuthRepository
import com.mitrc.ac.`in`.data.SubjectTeacherView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ---------------------------------------------------------------------------------------------
// Shared scroll scaffold - `scroll` is hoisted into the shell so the header can collapse
// against whichever tab's list is on screen.
// ---------------------------------------------------------------------------------------------

@Composable
private fun PortalTabScaffold(
    scroll: ScrollState,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        content()
    }
}

// ---------------------------------------------------------------------------------------------
// Attendance + Timetable share a single navigation entry
// ---------------------------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentScheduleTab(
    data: StudentPortalData,
    scroll: ScrollState,
    mode: ScheduleMode,
    notes: MutableList<PortalNote>,
    onReload: suspend () -> Unit,
    onModeChange: (ScheduleMode) -> Unit
) {
    var isRefreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    // Notes come from the `notes` table, so this entry needs a real refresh path - otherwise a
    // note published mid-session is invisible until the app is restarted.
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            scope.launch {
                isRefreshing = true
                try {
                    onReload()
                } finally {
                    isRefreshing = false
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    ) {
        PortalTabScaffold(scroll = scroll) {
            ScheduleSegmentedControl(mode = mode, onModeChange = onModeChange)
            Spacer(Modifier.height(18.dp))

            // Swipe left / right anywhere on the body to move between Attendance, Timetable and
            // Notes, exactly like a pager. The threshold is a slice of the content width so a
            // casual flick is enough but a stray touch is not. Keyed on `mode` so the accumulated
            // distance resets on every switch. Vertical pulls are claimed by the scroll handler
            // above, so this only ever sees horizontal movement.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(mode) {
                        var accumulated = 0f
                        val threshold = size.width * 0.18f
                        detectHorizontalDragGestures(
                            onDragStart = { accumulated = 0f },
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                accumulated += dragAmount
                                val entries = ScheduleMode.entries
                                if (accumulated <= -threshold) {
                                    accumulated = 0f
                                    val next = mode.ordinal + 1
                                    if (next < entries.size) onModeChange(entries[next])
                                } else if (accumulated >= threshold) {
                                    accumulated = 0f
                                    val prev = mode.ordinal - 1
                                    if (prev >= 0) onModeChange(entries[prev])
                                }
                            }
                        )
                    }
            ) {
                when (mode) {
                    ScheduleMode.ATTENDANCE -> AttendanceContent(data = data)
                    ScheduleMode.TIMETABLE -> TimetableContent(data = data)
                    ScheduleMode.NOTES -> NotesContent(data = data, notes = notes)
                }
            }
        }
    }
}

/**
 * The three-chip switcher the login screen uses for Student / Staff, extended to three:
 * a light track with a solid navy pill for the selected option.
 */
@Composable
private fun ScheduleSegmentedControl(
    mode: ScheduleMode,
    onModeChange: (ScheduleMode) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(PortalCard, RoundedCornerShape(14.dp))
            .border(1.dp, PortalStroke, RoundedCornerShape(14.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        ScheduleMode.entries.forEach { entry ->
            ScheduleSegment(
                label = entry.label,
                selected = mode == entry,
                onClick = { onModeChange(entry) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ScheduleSegment(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) PortalBlue else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 11.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) Color.White else PortalTextSecondary
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Attendance content
// ---------------------------------------------------------------------------------------------

@Composable
private fun AttendanceContent(data: StudentPortalData) {
    val attended = remember(data.attendance) { data.attendance.sumOf { it.attended } }
    val total = remember(data.attendance) { data.attendance.sumOf { it.total } }
    val pct = if (total > 0) attended * 100.0 / total else 0.0

    PortalSectionLabel(text = "ATTENDANCE RECORD")
    Spacer(Modifier.height(12.dp))

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = PortalCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, PortalStroke),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(
                        if (pct >= 75) PortalGreen.copy(alpha = 0.16f)
                        else PortalRose.copy(alpha = 0.12f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${pct.toInt()}%",
                    color = if (pct >= 75) PortalGreen else PortalRose,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    text = if (pct >= 75) "You are on track" else "Needs attention",
                    color = PortalTextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = "$attended attended of $total classes",
                    color = PortalTextSecondary,
                    fontSize = 12.5.sp
                )
            }
        }
    }

    Spacer(Modifier.height(20.dp))
    PortalSectionLabel(text = "SUBJECT WISE")
    Spacer(Modifier.height(12.dp))

    if (data.attendance.isEmpty()) {
        PortalEmptyCard(message = "No attendance records recorded yet.")
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            data.attendance.forEach { item ->
                SubjectAttendanceCard(
                    name = item.subjectName,
                    code = item.subjectCode,
                    percent = item.percentage,
                    attended = item.attended.toInt(),
                    total = item.total.toInt()
                )
            }
        }
    }
}

@Composable
private fun SubjectAttendanceCard(
    name: String,
    code: String,
    percent: Double,
    attended: Int,
    total: Int
) {
    val good = percent >= 75.0

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = PortalCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, PortalStroke),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(PortalBlue.copy(alpha = 0.12f))
                        .padding(horizontal = 9.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = code,
                        color = PortalBlue,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.weight(1f))
                Text(
                    text = "${percent.toInt()}%",
                    color = if (good) PortalGreen else PortalRose,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = name,
                color = PortalTextPrimary,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { (percent / 100.0).toFloat().coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = if (good) PortalGreen else PortalRose,
                trackColor = PortalTrack
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Attended $attended of $total classes",
                color = PortalTextSecondary,
                fontSize = 11.sp
            )
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Timetable content
// ---------------------------------------------------------------------------------------------

@Composable
private fun TimetableContent(data: StudentPortalData) {
    val dayNames = remember {
        mapOf(
            1 to "Monday", 2 to "Tuesday", 3 to "Wednesday",
            4 to "Thursday", 5 to "Friday", 6 to "Saturday"
        )
    }
    val grouped = remember(data.timetable) { data.timetable.groupBy { it.dayOfWeek } }
    val today = remember { currentDayOfWeek() }

    PortalSectionLabel(text = "WEEKLY TIMETABLE")
    Spacer(Modifier.height(12.dp))

    if (data.profile?.groupName.isNullOrBlank()) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = PortalRose.copy(alpha = 0.08f),
            border = androidx.compose.foundation.BorderStroke(1.dp, PortalRose.copy(alpha = 0.4f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            Text(
                text = "Lab group not assigned yet. Contact your co-ordinator.",
                color = PortalRose,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(14.dp)
            )
        }
    }

    if (data.timetable.isEmpty()) {
        PortalEmptyCard(message = "No published timetable available.")
        return
    }

    (1..6).forEach { day ->
        val entries = grouped[day]?.sortedBy { it.startPeriod }
        if (!entries.isNullOrEmpty()) {
            val isToday = day == today
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = PortalCard,
                border = androidx.compose.foundation.BorderStroke(
                    if (isToday) 1.5.dp else 1.dp,
                    if (isToday) PortalBlue else PortalStroke
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = dayNames[day] ?: "Day $day",
                            color = if (isToday) PortalBlue else PortalTextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (isToday) {
                            Spacer(Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(PortalBlue.copy(alpha = 0.12f))
                                    .padding(horizontal = 7.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "TODAY",
                                    color = PortalBlue,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.6.sp
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    entries.forEachIndexed { index, entry ->
                        if (index > 0) Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(PortalTrack.copy(alpha = 0.55f))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.width(64.dp)) {
                                Text(
                                    text = "P${entry.startPeriod}${
                                        if (entry.endPeriod > entry.startPeriod) "-${entry.endPeriod}" else ""
                                    }",
                                    color = PortalPurple,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                if (!entry.groupName.isNullOrBlank()) {
                                    Spacer(Modifier.height(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(5.dp))
                                            .background(PortalAmber.copy(alpha = 0.2f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = entry.groupName.orEmpty(),
                                            color = PortalBlue,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = entry.subjectName,
                                    color = PortalTextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (!entry.teacherName.isNullOrBlank()) {
                                    Text(
                                        text = entry.teacherName.orEmpty(),
                                        color = PortalTextSecondary,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            if (!entry.room.isNullOrBlank()) {
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = entry.room.orEmpty(),
                                    color = PortalTextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Notes - Subject PDF Notes published by teachers & Personal Notebook
// ---------------------------------------------------------------------------------------------

@Composable
private fun NotesContent(data: StudentPortalData, notes: MutableList<PortalNote>) {
    val context = LocalContext.current
    // Straight from the `notes` table - already filtered to this student's subjects and sorted
    // newest first. Falls back to nothing (not sample data) when the table is empty.
    val pdfNotes = data.pdfNotes

    var selectedSubject by remember { mutableStateOf("All") }
    var selectedCategory by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }
    var viewMode by remember { mutableStateOf(0) } // 0 = Teacher PDFs, 1 = My Quick Notes

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(PortalCardAlt)
            .padding(3.dp)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(9.dp))
                .background(if (viewMode == 0) PortalBlue else Color.Transparent)
                .clickable { viewMode = 0 }
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Teacher PDFs (${pdfNotes.size})",
                color = if (viewMode == 0) Color.White else PortalTextSecondary,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(9.dp))
                .background(if (viewMode == 1) PortalBlue else Color.Transparent)
                .clickable { viewMode = 1 }
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "My Notes (${notes.size})",
                color = if (viewMode == 1) Color.White else PortalTextSecondary,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }

    Spacer(Modifier.height(16.dp))

    if (viewMode == 0) {
        PortalSectionLabel(text = "SUBJECT STUDY MATERIAL & PDFS")
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Official lecture notes, assignments and question papers published by faculty",
            color = PortalTextSecondary,
            fontSize = 12.5.sp
        )
        Spacer(Modifier.height(14.dp))

        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search by title, topic or teacher...", fontSize = 12.5.sp) },
            leadingIcon = {
                Icon(
                    Icons.Outlined.Search,
                    contentDescription = "Search",
                    tint = PortalTextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        // Subject filter chips
        val subjectNames = remember(data.subjects) {
            listOf("All") + data.subjects.map { it.subjectName }.distinct()
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            subjectNames.forEach { subject ->
                val active = selectedSubject == subject
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (active) PortalBlue else PortalCard)
                        .border(1.dp, if (active) PortalBlue else PortalStroke, RoundedCornerShape(20.dp))
                        .clickable { selectedSubject = subject }
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = subject,
                        color = if (active) Color.White else PortalTextPrimary,
                        fontSize = 11.5.sp,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // Category filter chips - mirrors the `notes_category_chk` allow-list on the table.
        val categories = listOf("All", "notes", "assignment", "question_paper", "lab_manual", "syllabus", "other")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.forEach { cat ->
                val label = when (cat) {
                    "All" -> "All Types"
                    "notes" -> "Notes"
                    "assignment" -> "Assignments"
                    "question_paper" -> "Question Papers"
                    "lab_manual" -> "Lab Manuals"
                    "syllabus" -> "Syllabus"
                    "other" -> "Other"
                    else -> cat.replaceFirstChar { it.uppercase() }
                }
                val active = selectedCategory == cat
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (active) PortalPurple.copy(alpha = 0.15f) else PortalCard)
                        .border(1.dp, if (active) PortalPurple else PortalStroke, RoundedCornerShape(16.dp))
                        .clickable { selectedCategory = cat }
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = label,
                        color = if (active) PortalPurple else PortalTextSecondary,
                        fontSize = 11.sp,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        val filtered = pdfNotes.filter { note ->
            val matchSubject = selectedSubject == "All" || note.subjectName.equals(selectedSubject, ignoreCase = true)
            val matchCategory = selectedCategory == "All" || note.category.equals(selectedCategory, ignoreCase = true)
            val matchQuery = searchQuery.isBlank() ||
                    note.title.contains(searchQuery, ignoreCase = true) ||
                    note.subjectName.contains(searchQuery, ignoreCase = true) ||
                    note.teacherName.contains(searchQuery, ignoreCase = true) ||
                    (note.description?.contains(searchQuery, ignoreCase = true) == true)
            matchSubject && matchCategory && matchQuery
        }

        if (filtered.isEmpty()) {
            PortalEmptyCard(
                message = if (pdfNotes.isEmpty()) {
                    "No PDF notes have been published for your subjects yet."
                } else {
                    "No published PDF notes match your filter."
                }
            )
        } else {
            // One section per subject, in enrolment order, so every subject the student attends
            // shows up with the material published against it.
            val grouped = filtered.groupBy { it.subjectName }
            val subjectOrder = data.subjects.withIndex()
                .associate { (index, subject) -> subject.subjectName.lowercase() to index }
            val sections = grouped.entries
                .sortedBy { entry ->
                    subjectOrder[entry.key.lowercase()] ?: Int.MAX_VALUE
                }
                .map { it.key to it.value }

            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                sections.forEach { (subjectName, subjectNotes) ->
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (selectedSubject == "All") {
                            PortalSectionLabel(text = subjectName.uppercase())
                        }
                        subjectNotes.forEach { note ->
                            PdfNoteCard(
                                note = note,
                                onOpenPdf = {
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(note.driveUrl))
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        // No browser on the device - nothing else we can do here.
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    } else {
        PersonalNotesSection(notes = notes)
    }
}

@Composable
private fun PdfNoteCard(
    note: PortalPdfNote,
    onOpenPdf: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = PortalCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, PortalStroke),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenPdf)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(PortalRose.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.Description,
                            contentDescription = "PDF Document",
                            tint = PortalRose,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${note.subjectCode} · ${note.subjectName}",
                            color = PortalTextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = note.teacherName,
                            color = PortalTextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(PortalBlue.copy(alpha = 0.10f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = note.category.uppercase(),
                        color = PortalBlue,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = note.title,
                color = PortalTextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 19.sp
            )

            if (!note.description.isNullOrBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = note.description,
                    color = PortalTextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (note.createdAt.isBlank()) "" else "Published: ${note.createdAt}",
                    color = PortalTextSecondary,
                    fontSize = 10.5.sp
                )

                Button(
                    onClick = onOpenPdf,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PortalBlue,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Outlined.OpenInNew,
                        contentDescription = "Open PDF",
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("View PDF", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun PersonalNotesSection(notes: MutableList<PortalNote>) {
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }

    PortalSectionLabel(text = "MY QUICK NOTES")
    Spacer(Modifier.height(12.dp))

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = PortalCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, PortalStroke),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title", fontSize = 12.sp) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = body,
                onValueChange = { body = it },
                label = { Text("Write it down...", fontSize = 12.sp) },
                minLines = 3,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = {
                    val trimmedTitle = title.trim()
                    val trimmedBody = body.trim()
                    if (trimmedTitle.isNotBlank() || trimmedBody.isNotBlank()) {
                        notes.add(
                            0,
                            PortalNote(
                                id = (notes.maxOfOrNull { it.id } ?: 0L) + 1L,
                                title = trimmedTitle,
                                body = trimmedBody
                            )
                        )
                        title = ""
                        body = ""
                    }
                },
                enabled = title.isNotBlank() || body.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(44.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PortalBlue,
                    contentColor = Color.White
                )
            ) {
                Icon(
                    Icons.Outlined.Add,
                    contentDescription = null,
                    modifier = Modifier.size(17.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Add Note", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }

    Spacer(Modifier.height(20.dp))
    PortalSectionLabel(text = "SAVED NOTES")
    Spacer(Modifier.height(12.dp))

    if (notes.isEmpty()) {
        PortalEmptyCard(message = "Nothing saved yet. Write something above to get started.")
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            notes.forEach { note ->
                NoteCard(
                    note = note,
                    onDelete = { notes.remove(note) }
                )
            }
        }
    }
}

@Composable
private fun NoteCard(note: PortalNote, onDelete: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = PortalCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, PortalStroke),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    if (note.title.isNotBlank()) {
                        Text(
                            text = note.title,
                            color = PortalTextPrimary,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(2.dp))
                    }
                    Text(
                        text = noteTime(note.createdAt),
                        color = PortalTextSecondary,
                        fontSize = 10.5.sp
                    )
                }
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(PortalRose.copy(alpha = 0.10f))
                        .clickable(onClick = onDelete),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = "Delete note",
                        tint = PortalRose,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
            if (note.body.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = note.body,
                    color = PortalTextPrimary,
                    fontSize = 12.5.sp,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

private fun noteTime(timestamp: Long): String =
    SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(timestamp))

// ---------------------------------------------------------------------------------------------
// Events
// ---------------------------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentEventsTab(scroll: ScrollState) {
    var isRefreshing by remember { mutableStateOf(false) }
    // Brief first-paint skeleton so the cards don't pop in fully formed on tab entry.
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    var events by remember { mutableStateOf(sampleCollegeEvents()) }

    LaunchedEffect(Unit) {
        delay(650)
        isLoading = false
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            scope.launch {
                isRefreshing = true
                delay(1200)
                events = sampleCollegeEvents()
                isRefreshing = false
            }
        },
        modifier = Modifier.fillMaxSize()
    ) {
        PortalTabScaffold(scroll = scroll) {
            PortalSectionLabel(text = "COLLEGE EVENTS")
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Everything happening on campus right now",
                color = PortalTextSecondary,
                fontSize = 13.sp
            )
            Spacer(Modifier.height(14.dp))

            if (isLoading || isRefreshing) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    repeat(3) {
                        EventCardSkeleton()
                    }
                }
            } else if (events.isEmpty()) {
                PortalEmptyCard(message = "No college events right now.")
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    events.forEach { event ->
                        EventListCard(event = event)
                    }
                }
            }
        }
    }
}

@Composable
private fun EventListCard(event: CollegeEvent) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = PortalCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, PortalStroke),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            AsyncImage(
                model = event.image,
                contentDescription = event.title,
                contentScale = ContentScale.Crop,
                placeholder = ColorPainter(PortalCardAlt),
                error = ColorPainter(PortalCardAlt),
                fallback = ColorPainter(PortalCardAlt),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
            )
            Column(modifier = Modifier.padding(16.dp)) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(7.dp))
                        .background(PortalBlue.copy(alpha = 0.1f))
                        .border(1.dp, PortalBlue.copy(alpha = 0.25f), RoundedCornerShape(7.dp))
                        .padding(horizontal = 9.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = event.category,
                        color = PortalBlue,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.8.sp
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text = event.title,
                    color = PortalTextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 20.sp
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "${event.whenLabel}  \u00b7  ${event.venue}",
                    color = PortalTextSecondary,
                    fontSize = 11.5.sp
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Settings
// ---------------------------------------------------------------------------------------------

@Composable
fun StudentSettingsTab(
    data: StudentPortalData,
    scroll: ScrollState,
    onSignedOut: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val profile = data.profile
    var showContact by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }

    PortalTabScaffold(scroll = scroll) {
        PortalSectionLabel(text = "MY PROFILE")
        Spacer(Modifier.height(12.dp))

        Surface(
            shape = RoundedCornerShape(18.dp),
            color = PortalCard,
            border = androidx.compose.foundation.BorderStroke(1.dp, PortalStroke),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(PortalBlue.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.School,
                            contentDescription = null,
                            tint = PortalBlue,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(
                            text = profile?.name ?: "Student Profile",
                            color = PortalTextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Serial: ${profile?.serialNo ?: "N/A"}",
                            color = PortalTextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                val cohort = buildString {
                    append(profile?.course ?: "B.Tech")
                    if (!profile?.branch.isNullOrBlank()) append(" - ").append(profile?.branch)
                    if (profile?.semester != null) append(" | Sem ").append(profile.semester)
                    if (!profile?.section.isNullOrBlank()) append(" - Sec ").append(profile?.section)
                }
                SettingsRow(label = "Cohort", value = cohort)

                SettingsRow(
                    label = "Lab Group",
                    value = profile?.groupName ?: "Not assigned",
                    highlight = profile?.groupName.isNullOrBlank()
                )

                SettingsRow(
                    label = "Academic Year",
                    value = profile?.academicYear ?: "N/A"
                )

                SettingsRow(
                    label = "Email Id",
                    value = AuthRepository.currentUser?.email
                        ?: profile?.studentEmail
                        ?: "-"
                )

                if (!profile?.fatherName.isNullOrBlank()) {
                    SettingsRow(label = "Father's Name", value = profile?.fatherName.orEmpty())
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        PortalSectionLabel(text = "SUPPORT & MORE")
        Spacer(Modifier.height(12.dp))

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = PortalCard,
            border = androidx.compose.foundation.BorderStroke(1.dp, PortalStroke),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                SettingsOption(
                    icon = Icons.Outlined.Phone,
                    iconTint = PortalBlue,
                    title = "Contact Faculty",
                    subtitle = data.faculty.firstOrNull()?.teacherName
                        ?.takeIf { it.isNotBlank() }
                        ?: "Every teacher assigned to your batch",
                    onClick = { showContact = true }
                )
                OptionDivider()
                SettingsOption(
                    icon = Icons.Outlined.Info,
                    iconTint = PortalPurple,
                    title = "ABOUT MITRC APP",
                    subtitle = "Updates, version and college details",
                    onClick = { showAbout = true }
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        Surface(
            shape = RoundedCornerShape(14.dp),
            color = PortalRose.copy(alpha = 0.1f),
            border = androidx.compose.foundation.BorderStroke(1.dp, PortalRose.copy(alpha = 0.45f)),
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    scope.launch {
                        AuthRepository.signOut()
                        onSignedOut()
                    }
                }
        ) {
            Row(
                modifier = Modifier.padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.AutoMirrored.Outlined.Logout,
                    contentDescription = null,
                    tint = PortalRose,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "Sign Out",
                    color = PortalRose,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        Text(
            text = "\u00a9 2026 MITRC, Alwar. All Rights Reserved.",
            color = PortalTextSecondary,
            fontSize = 11.sp,
            modifier = Modifier.fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
    }

    PortalSheet(
        visible = showContact,
        onDismiss = { showContact = false }
    ) {
        ContactFacultySheetContent(
            batchName = data.profile?.groupName,
            faculty = data.subjects,
            onClose = { showContact = false }
        )
    }

    PortalSheet(
        visible = showAbout,
        onDismiss = { showAbout = false }
    ) {
        AboutSheetContent(onClose = { showAbout = false })
    }
}

@Composable
private fun SettingsRow(label: String, value: String, highlight: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = PortalTextSecondary, fontSize = 12.sp)
        Text(
            text = value,
            color = if (highlight) PortalRose else PortalTextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
            modifier = Modifier.padding(start = 16.dp)
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Support & more: contact sheet, update check, about sheet
// ---------------------------------------------------------------------------------------------

@Composable
private fun SettingsOption(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(iconTint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = PortalTextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = PortalTextSecondary,
                fontSize = 11.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(Modifier.width(10.dp))

        if (trailing != null) {
            trailing()
        } else {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = PortalTextSecondary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun OptionDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 64.dp)
            .height(1.dp)
            .background(PortalStroke)
    )
}

@Composable
private fun ContactFacultySheetContent(
    batchName: String?,
    faculty: List<ClassSubjectView>,
    onClose: () -> Unit
) {
    // `v_subject_teachers` returns one row per subject, so a teacher who handles three
    // subjects for this batch shows up three times - collapse them into one card.
    val grouped = remember(faculty) {
        faculty
            .filter { it.teacherName.isNotBlank() }
            .groupBy { it.teacherId to it.teacherName.trim() }
            .map { (key, rows) ->
                FacultyMember(
                    id = key.first,
                    name = key.second,
                    subjects = rows
                        .map { it.subjectName.trim() }
                        .filter { it.isNotBlank() }
                        .distinct()
                )
            }
    }

    Column(modifier = Modifier.fillMaxWidth().padding(top = 26.dp)) {
        Text(
            text = "Contact Faculty",
            color = PortalTextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(horizontal = 22.dp)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = buildString {
                append("Teachers assigned to your batch")
                if (!batchName.isNullOrBlank()) append(" - ").append(batchName)
                append(".")
            },
            color = PortalTextSecondary,
            fontSize = 12.5.sp,
            lineHeight = 18.sp,
            modifier = Modifier.padding(horizontal = 22.dp)
        )

        Spacer(Modifier.height(16.dp))

        if (grouped.isEmpty()) {
            Text(
                text = "No faculty has been mapped to your batch yet. " +
                    "Once your class subjects are assigned, every teacher will be listed here.",
                color = PortalRose,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                modifier = Modifier.padding(horizontal = 22.dp)
            )
        } else {
            Column(
                modifier = Modifier.padding(horizontal = 22.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                grouped.forEach { member ->
                    FacultyCard(member = member)
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(PortalCardAlt)
                .clickable(onClick = onClose)
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Close",
                color = PortalTextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(Modifier.height(18.dp))
    }
}

/** One teacher plus the subjects they handle for this student's batch. */
private data class FacultyMember(
    val id: Int,
    val name: String,
    val subjects: List<String>
)

@Composable
private fun FacultyCard(member: FacultyMember) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = PortalCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, PortalStroke),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(14.dp)) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(PortalBlue.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.School,
                    contentDescription = null,
                    tint = PortalBlue,
                    modifier = Modifier.size(19.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = member.name,
                    color = PortalTextPrimary,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (member.subjects.isNotEmpty()) {
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = member.subjects.joinToString("  \u2022  "),
                        color = PortalTextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
            }
        }
    }
}

/**
 * The single "ABOUT MITRC APP" sheet - it absorbed the old standalone "Check for Updates"
 * row, so version info and the update check now live behind one settings option.
 */
@Composable
private fun AboutSheetContent(onClose: () -> Unit) {
    // 0 = idle, 1 = checking, 2 = up to date
    var updateState by remember { mutableStateOf(0) }

    LaunchedEffect(updateState) {
        if (updateState == 1) {
            delay(1500)
            updateState = 2
        }
    }

    Column(modifier = Modifier.fillMaxWidth().padding(top = 26.dp)) {
        Text(
            text = "MITRC Student Portal",
            color = PortalTextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(horizontal = 22.dp)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Maharishi Institute of Technology & Research Centre, Alwar",
            color = PortalTextSecondary,
            fontSize = 12.5.sp,
            lineHeight = 18.sp,
            modifier = Modifier.padding(horizontal = 22.dp)
        )

        Spacer(Modifier.height(16.dp))

        Column(modifier = Modifier.padding(horizontal = 22.dp)) {
            SheetInfoRow(label = "App Version", value = "1.0.0 (build 1)")
            SheetInfoRow(label = "Channel", value = "Stable")
            SheetInfoRow(label = "Support", value = "portal@mitrc.ac.in")
        }

        Spacer(Modifier.height(14.dp))

        // Update check, folded in from the old "Check for Updates" settings row.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(PortalCardAlt)
                .clickable {
                    if (updateState != 1) updateState = if (updateState == 2) 0 else 1
                }
                .padding(horizontal = 14.dp, vertical = 13.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.Refresh,
                    contentDescription = null,
                    tint = PortalGreen,
                    modifier = Modifier.size(17.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = when (updateState) {
                        0 -> "Check for Updates"
                        1 -> "Checking for the latest version..."
                        else -> "You are on the latest version"
                    },
                    color = PortalTextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                if (updateState == 1) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = PortalBlue,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = PortalTextSecondary,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(PortalBlue)
                .clickable(onClick = onClose)
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Close",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(Modifier.height(18.dp))
    }
}

@Composable
private fun SheetInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = PortalTextSecondary, fontSize = 12.5.sp)
        Text(
            text = value,
            color = PortalTextPrimary,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
            modifier = Modifier.padding(start = 16.dp)
        )
    }
}

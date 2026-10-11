package com.mitrc.ac.`in`.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MarkEmailRead
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.mitrc.ac.`in`.auth.AuthRepository
import com.mitrc.ac.`in`.data.*
import com.mitrc.ac.`in`.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

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

            AnimatedContent(
                targetState = mode,
                transitionSpec = {
                    if (targetState.ordinal > initialState.ordinal) {
                        (slideInHorizontally(tween(280)) { it } + fadeIn(tween(280)))
                            .togetherWith(slideOutHorizontally(tween(200)) { -it } + fadeOut(tween(200)))
                    } else {
                        (slideInHorizontally(tween(280)) { -it } + fadeIn(tween(280)))
                            .togetherWith(slideOutHorizontally(tween(200)) { it } + fadeOut(tween(200)))
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                label = "scheduleModeTransition"
            ) { targetMode ->
                when (targetMode) {
                    ScheduleMode.ATTENDANCE -> AttendanceContent(data = data)
                    ScheduleMode.TIMETABLE -> TimetableContent(data = data)
                    ScheduleMode.NOTES -> NotesContent(data = data, notes = notes)
                    ScheduleMode.EXAMS -> ExamsResultsContent(data = data)
                }
            }
        }
    }
}

@Composable
private fun ScheduleSegmentedControl(
    mode: ScheduleMode,
    onModeChange: (ScheduleMode) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .clip(RoundedCornerShape(16.dp))
            .background(PortalCard)
            .border(1.dp, PortalStroke, RoundedCornerShape(16.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        ScheduleMode.entries.forEach { entry ->
            val active = mode == entry
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (active) PortalBlue else Color.Transparent)
                    .clickable { onModeChange(entry) }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = entry.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (active) Color.White else PortalTextSecondary,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun AttendanceContent(data: StudentPortalData) {
    Column {
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
                    SubjectAttendanceCard(item)
                }
            }
        }
    }
}

@Composable
private fun SubjectAttendanceCard(item: AttendanceSummaryView) {
    val pct = item.percentage
    val good = pct >= 75.0

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
                        text = item.subjectCode,
                        color = PortalBlue,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.weight(1f))
                Text(
                    text = "${pct.toInt()}%",
                    color = if (good) PortalGreen else PortalRose,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = item.subjectName,
                color = PortalTextPrimary,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { (pct / 100.0).toFloat().coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = if (good) PortalGreen else PortalRose,
                trackColor = PortalTrack
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Attended ${item.attended} of ${item.total} classes",
                color = PortalTextSecondary,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun TimetableContent(data: StudentPortalData) {
    Column {
        val dayNames = remember {
            mapOf(
                1 to "Monday", 2 to "Tuesday", 3 to "Wednesday",
                4 to "Thursday", 5 to "Friday", 6 to "Saturday"
            )
        }
        val timetable = remember(data.timetable) {
            if (data.timetable.isEmpty()) sampleTimetableEntries() else data.timetable
        }
        val grouped = remember(timetable) { timetable.groupBy { it.dayOfWeek } }
        val today = remember { currentDayOfWeek() }

        PortalSectionLabel(text = "WEEKLY TIMETABLE")
        Spacer(Modifier.height(12.dp))

        if (data.profile?.groupName.isNullOrBlank()) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = PortalRose.copy(alpha = 0.08f),
                border = androidx.compose.foundation.BorderStroke(1.dp, PortalRose.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            ) {
                Text(
                    text = "Lab group not assigned yet. Contact your co-ordinator.",
                    color = PortalRose, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(14.dp)
                )
            }
        }

        if (timetable.isEmpty()) {
            PortalEmptyCard(message = "No published timetable available.")
        } else {
            (1..6).forEach { day ->
                val entries = grouped[day]?.sortedBy { it.startPeriod }
                if (!entries.isNullOrEmpty()) {
                    val isToday = day == today
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = PortalCard,
                        border = androidx.compose.foundation.BorderStroke(if (isToday) 1.5.dp else 1.dp, if (isToday) PortalBlue else PortalStroke),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = dayNames[day] ?: "Day $day",
                                    color = if (isToday) PortalBlue else PortalTextPrimary,
                                    fontSize = 14.sp, fontWeight = FontWeight.Bold
                                )
                                if (isToday) {
                                    Spacer(Modifier.width(8.dp))
                                    Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(PortalBlue.copy(alpha = 0.12f)).padding(horizontal = 7.dp, vertical = 3.dp)) {
                                        Text(text = "TODAY", color = PortalBlue, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
                                    }
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            entries.forEachIndexed { idx, entry ->
                                if (idx > 0) Spacer(Modifier.height(8.dp))
                                TimetableEntryRow(entry)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TimetableEntryRow(entry: MyTimetableEntryView) {
    val accentColor = when (entry.subjectKind.lowercase()) {
        "lab" -> PortalPurple
        "consultation" -> PortalAmber
        else -> PortalBlue
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(PortalTrack.copy(alpha = 0.4f))
            .border(width = 1.dp, color = PortalStroke, shape = RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.width(72.dp)) {
            val timeRange = if (!entry.startTime.isNullOrBlank() && !entry.endTime.isNullOrBlank()) {
                "${entry.startTime} - ${entry.endTime}"
            } else {
                "P${entry.startPeriod}${if (entry.endPeriod > entry.startPeriod) "-${entry.endPeriod}" else ""}"
            }
            Text(
                text = timeRange,
                color = PortalTextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Box(
            modifier = Modifier
                .width(4.dp)
                .height(42.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(accentColor)
        )

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = entry.subjectName,
                    color = PortalTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(accentColor.copy(alpha = 0.12f))
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = entry.subjectKind.replaceFirstChar { it.uppercase() }.ifBlank { "Class" },
                        color = accentColor,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = entry.teacherName.orEmpty().ifBlank { "Faculty" },
                    color = PortalTextSecondary,
                    fontSize = 11.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (!entry.room.isNullOrBlank()) {
                    Text(
                        text = "Room ${entry.room}",
                        color = PortalTextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun NotesContent(data: StudentPortalData, notes: MutableList<PortalNote>) {
    val context = LocalContext.current

    val pdfNotes = remember(data.pdfNotes) {
        if (data.pdfNotes.isEmpty()) samplePdfNotes() else data.pdfNotes
    }
    val subjects = remember(data.subjects) {
        if (data.subjects.isEmpty()) sampleSubjectCards() else data.subjects
    }

    var selectedSubject by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        PortalSectionLabel(text = "ASSIGNED SUBJECTS & FACULTY")

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            subjects.forEach { subject ->
                val countForSubject = pdfNotes.count {
                    it.classSubjectId == subject.classSubjectId ||
                            it.subjectName.equals(subject.subjectName, ignoreCase = true)
                }
                val isSelected = selectedSubject.equals(subject.subjectName, ignoreCase = true)

                SubjectFacultyCard(
                    subject = subject,
                    noteCount = countForSubject,
                    isSelected = isSelected,
                    onClick = {
                        selectedSubject = if (isSelected) "All" else subject.subjectName
                    }
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        PortalSectionLabel(text = "STUDY MATERIAL & PDF NOTES")

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search by title, topic or teacher...", fontSize = 12.5.sp) },
            leadingIcon = { Icon(Icons.Outlined.Search, null, modifier = Modifier.size(18.dp)) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        )

        val filtered = pdfNotes.filter { note ->
            (selectedSubject == "All" || note.subjectName.equals(selectedSubject, ignoreCase = true)) &&
                    (searchQuery.isBlank() || note.title.contains(searchQuery, true) || note.teacherName.contains(searchQuery, true))
        }

        if (filtered.isEmpty()) {
            PortalEmptyCard(message = "No published PDF notes match your filter.")
        } else {
            val grouped = filtered.groupBy { it.subjectName }
            val subjectOrder = subjects.withIndex()
                .associate { (index, subject) -> subject.subjectName.lowercase() to index }
            val sections = grouped.entries
                .sortedBy { entry -> subjectOrder[entry.key.lowercase()] ?: Int.MAX_VALUE }
                .map { it.key to it.value }

            Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                sections.forEach { (subjectName, subjectNotes) ->
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (selectedSubject == "All") {
                            PortalSectionLabel(text = subjectName.uppercase())
                        }
                        subjectNotes.forEach { note ->
                            PdfNoteCard(note) {
                                try {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(note.driveUrl)))
                                } catch (e: Exception) {}
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SubjectFacultyCard(
    subject: ClassSubjectView,
    noteCount: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    ElevatedCard(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (isSelected) PortalBlue.copy(alpha = 0.08f) else PortalCard
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isSelected) Modifier.border(1.5.dp, PortalBlue, RoundedCornerShape(18.dp))
                else Modifier
            )
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(PortalBlue.copy(alpha = 0.12f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = subject.subjectCode,
                        color = PortalBlue, fontSize = 11.5.sp, fontWeight = FontWeight.Bold
                    )
                }
                if (noteCount > 0) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(PortalAmber.copy(alpha = 0.25f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(text = "$noteCount Resource${if (noteCount > 1) "s" else ""}", color = PortalBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            Text(text = subject.subjectName, color = PortalTextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.size(28.dp).clip(CircleShape).background(PortalBlue.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.School, null, tint = PortalBlue, modifier = Modifier.size(16.dp))
                }
                Column {
                    Text(text = subject.teacherName, color = PortalTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    if (!subject.teacherEmail.isNullOrBlank()) {
                        Text(text = subject.teacherEmail!!, color = PortalTextSecondary, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun PdfNoteCard(note: PortalPdfNote, onClick: () -> Unit) {
    ElevatedCard(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = PortalCard),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(PortalRose.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.PictureAsPdf, null, tint = PortalRose, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = note.title, color = PortalTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(text = "${note.subjectCode} \u00b7 ${note.teacherName}", color = PortalTextSecondary, fontSize = 11.sp)
            }
            Icon(Icons.AutoMirrored.Outlined.OpenInNew, null, tint = PortalBlue, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun ExamsResultsContent(data: StudentPortalData) {
    val profile = data.profile
    val marks = data.marks

    var selectedCategory by remember { mutableStateOf("All Exams") }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // --- 1. OVERALL RESULTS HEADER CARD (Matching Mockup Image Bottom Right) ---
        ElevatedCard(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = PortalBlue),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Gold.copy(alpha = 0.25f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Outlined.School,
                                contentDescription = null,
                                tint = GoldLight,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                text = profile?.name ?: "Student Profile",
                                color = Color.White,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${profile?.course ?: "B.Tech CSE"} · Sem ${profile?.semester ?: 3}",
                                color = Color.White.copy(alpha = 0.75f),
                                fontSize = 12.sp
                            )
                        }
                    }

                    // Promoted / Pass badge pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(PortalGreen.copy(alpha = 0.25f))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "PROMOTED",
                            color = GoldLight,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                val totalMarksObtained = if (marks.isNotEmpty()) marks.mapNotNull { it.marks }.sum() else 248.0
                val totalMaxMarks = if (marks.isNotEmpty()) marks.mapNotNull { it.maxMarks }.sum() else 300.0
                val overallPct = if (totalMaxMarks > 0) (totalMarksObtained / totalMaxMarks * 100.0) else 82.0

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${overallPct.toInt()}%",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Overall percentage",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp
                    )
                }

                Spacer(Modifier.height(8.dp))

                LinearProgressIndicator(
                    progress = { (overallPct / 100.0).toFloat().coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape),
                    color = Gold,
                    trackColor = Color.White.copy(alpha = 0.2f)
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        // --- 2. EXAMS SUB-FILTER CHIPS ---
        PortalSectionLabel(text = "EXAMS RESULTS")

        val filterCategories = listOf("All Exams", "Unit Tests", "Mid Terms", "End Sem")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            filterCategories.forEach { category ->
                val active = selectedCategory == category
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (active) PortalBlue else PortalCard)
                        .border(1.dp, if (active) PortalBlue else PortalStroke, RoundedCornerShape(20.dp))
                        .clickable { selectedCategory = category }
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = category,
                        color = if (active) Color.White else PortalTextPrimary,
                        fontSize = 12.sp,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // --- 3. TIMELINE EXAM RESULTS CARDS (Matching Mockup Image Timeline) ---
        val sampleList = listOf(
            ExamAssessment(
                title = "1st Midterm",
                dateRange = "15 Sep to 18 Sep",
                score = "85 / 100",
                status = "Promoted",
                isPass = true,
                subjects = listOf(
                    ExamSubjectResult("Mathematics", "56", true),
                    ExamSubjectResult("Physics", "28", false)
                )
            ),
            ExamAssessment(
                title = "2nd Midterm",
                dateRange = "15 Oct to 18 Oct",
                score = "78 / 100",
                status = "Promoted",
                isPass = true,
                subjects = listOf(
                    ExamSubjectResult("Chemistry 1", "65", true),
                    ExamSubjectResult("Data Structures", "56", true)
                )
            ),
            ExamAssessment(
                title = "3rd Midterm",
                dateRange = "15 Nov to 18 Nov",
                score = "82 / 100",
                status = "Promoted",
                isPass = true,
                subjects = listOf(
                    ExamSubjectResult("DBMS", "72", true),
                    ExamSubjectResult("Operating Systems", "68", true)
                )
            )
        )

        val filteredExams = when (selectedCategory) {
            "Unit Tests" -> sampleList.filter { it.title.contains("UT", true) || it.title.contains("Unit", true) }
            "Mid Terms" -> sampleList.filter { it.title.contains("Midterm", true) || it.title.contains("Mid", true) }
            "End Sem" -> sampleList.filter { it.title.contains("End", true) }
            else -> sampleList
        }.ifEmpty { sampleList }

        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            filteredExams.forEachIndexed { index, exam ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    // Timeline connector column
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(24.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(Gold)
                        )
                        if (index < filteredExams.lastIndex) {
                            Box(
                                modifier = Modifier
                                    .width(2.dp)
                                    .height(140.dp)
                                    .background(Gold.copy(alpha = 0.4f))
                            )
                        }
                    }

                    Spacer(Modifier.width(10.dp))

                    // Exam card
                    ElevatedCard(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.elevatedCardColors(containerColor = PortalCard),
                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = exam.title,
                                        color = PortalTextPrimary,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = exam.dateRange,
                                        color = PortalTextSecondary,
                                        fontSize = 11.5.sp
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (exam.isPass) PortalGreen.copy(alpha = 0.15f) else PortalRose.copy(alpha = 0.15f))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = exam.status.uppercase(),
                                        color = if (exam.isPass) PortalGreen else PortalRose,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(Modifier.height(12.dp))

                            exam.subjects.forEach { sub ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = PortalTrack.copy(alpha = 0.4f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = sub.subjectName,
                                            color = PortalTextPrimary,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = sub.score,
                                                color = PortalTextPrimary,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = if (sub.isPass) "PASS" else "FAILED",
                                                color = if (sub.isPass) PortalGreen else PortalRose,
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class ExamAssessment(
    val title: String,
    val dateRange: String,
    val score: String,
    val status: String,
    val isPass: Boolean,
    val subjects: List<ExamSubjectResult>
)

private data class ExamSubjectResult(
    val subjectName: String,
    val score: String,
    val isPass: Boolean
)

@Composable
internal fun PortalSectionLabel(text: String) {
    Text(text = text, color = PortalTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
}

@Composable
private fun IconButton(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(modifier = Modifier.size(32.dp).clip(CircleShape).clickable(onClick = onClick), contentAlignment = Alignment.Center) { content() }
}

@Composable
fun StudentEventsTab(scroll: ScrollState) {
    PortalTabScaffold(scroll = scroll) {
        PortalSectionLabel(text = "CAMPUS EVENTS")
        Spacer(Modifier.height(12.dp))
        PortalEmptyCard(message = "No upcoming events found.")
    }
}

@Composable
fun StudentSettingsTab(data: StudentPortalData, scroll: ScrollState, onSignedOut: () -> Unit) {
    val profile = data.profile
    val scope = rememberCoroutineScope()

    var showProfileSheet by remember { mutableStateOf(false) }
    var showFacultySheet by remember { mutableStateOf(false) }
    var showCollegeContactsSheet by remember { mutableStateOf(false) }
    var showAboutAppSheet by remember { mutableStateOf(false) }
    var updateStatusText by remember { mutableStateOf<String?>(null) }

    PortalTabScaffold(scroll = scroll) {
        // --- 1. PROFILE HEADER CARD (Matching Image 2 in Prompt) ---
        ElevatedCard(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = PortalCard),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .border(2.dp, Gold, CircleShape)
                            .background(PortalBlue.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.Person,
                            contentDescription = null,
                            tint = PortalBlue,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = profile?.name ?: "Student Profile",
                            color = PortalTextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "${profile?.course ?: "B.Tech"} · Semester ${profile?.semester ?: 1}",
                            color = PortalBlue,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(6.dp))
                        if (!profile?.studentEmail.isNullOrBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Outlined.MarkEmailRead, null, tint = PortalTextSecondary, modifier = Modifier.size(13.dp))
                                Text(
                                    text = profile.studentEmail,
                                    color = PortalTextSecondary,
                                    fontSize = 11.5.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        if (!profile?.studentPhoneNo.isNullOrBlank()) {
                            Spacer(Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Outlined.Phone, null, tint = PortalTextSecondary, modifier = Modifier.size(13.dp))
                                Text(
                                    text = profile.studentPhoneNo,
                                    color = PortalTextSecondary,
                                    fontSize = 11.5.sp
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = PortalStroke)
                Spacer(Modifier.height(12.dp))

                Text(
                    text = "\u201cStay organized. Stay consistent. Progress will follow.\u201d",
                    color = PortalTextSecondary,
                    fontSize = 12.sp,
                    fontStyle = FontStyle.Italic
                )
            }
        }

        Spacer(Modifier.height(22.dp))

        // --- 2. OPTIONS LIST ---
        PortalSectionLabel(text = "SETTINGS & SERVICES")
        Spacer(Modifier.height(12.dp))

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SettingsOptionCard(
                title = "1. My Profile",
                subtitle = "View personal & academic details",
                icon = Icons.Outlined.Person,
                onClick = { showProfileSheet = true }
            )

            SettingsOptionCard(
                title = "2. Contact Assigned Faculty",
                subtitle = "Course teachers & contact details",
                icon = Icons.Outlined.School,
                onClick = { showFacultySheet = true }
            )

            SettingsOptionCard(
                title = "3. College Contacts",
                subtitle = "Official phone numbers & email addresses",
                icon = Icons.Outlined.Phone,
                onClick = { showCollegeContactsSheet = true }
            )

            SettingsOptionCard(
                title = "4. About MITRC App",
                subtitle = "App version, platform & details",
                icon = Icons.Outlined.Info,
                onClick = { showAboutAppSheet = true }
            )

            SettingsOptionCard(
                title = "5. Check for Update",
                subtitle = updateStatusText ?: "Check for latest app release",
                icon = Icons.Outlined.Refresh,
                onClick = {
                    updateStatusText = "App is up to date (v2.4.1)"
                }
            )
        }

        Spacer(Modifier.height(24.dp))

        // --- 3. SIGN OUT BUTTON ---
        Button(
            onClick = {
                scope.launch {
                    AuthRepository.signOut()
                    onSignedOut()
                }
            },
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = PortalRose,
                contentColor = Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Sign Out", fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(20.dp))

        // --- 4. COPYRIGHT FOOTER TEXT ---
        Text(
            text = "\u00a9 2026 MITRC Alwar. All rights reserved.",
            color = PortalTextSecondary,
            fontSize = 11.5.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }

    if (showProfileSheet) {
        ProfileDetailsSheet(profile = profile, onDismiss = { showProfileSheet = false })
    }

    if (showFacultySheet) {
        ContactFacultySheet(facultyList = data.faculty, onDismiss = { showFacultySheet = false })
    }

    if (showCollegeContactsSheet) {
        CollegeContactsSheet(onDismiss = { showCollegeContactsSheet = false })
    }

    if (showAboutAppSheet) {
        AboutAppSheet(onDismiss = { showAboutAppSheet = false })
    }
}

@Composable
private fun SettingsOptionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    ElevatedCard(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = PortalCard),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(PortalBlue.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = PortalBlue, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = PortalTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(2.dp))
                Text(subtitle, color = PortalTextSecondary, fontSize = 11.5.sp)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = PortalTextSecondary, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun ProfileDetailsSheet(profile: StudentProfileView?, onDismiss: () -> Unit) {
    Surface(
        color = PortalCard,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        border = BorderStroke(1.dp, PortalStroke),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("MY PROFILE DETAILS", color = PortalTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = PortalTrack)) {
                    Text("Close", color = PortalTextPrimary, fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(16.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SheetDetailRow("Full Name", profile?.name ?: "N/A")
                SheetDetailRow("Roll / Serial No.", profile?.serialNo ?: "N/A")
                SheetDetailRow("Student Email", profile?.studentEmail ?: "N/A")
                SheetDetailRow("Student Phone", profile?.studentPhoneNo ?: "N/A")
                SheetDetailRow("Father's Name", profile?.fatherName ?: "N/A")
                SheetDetailRow("Father's Phone", profile?.fatherPhoneNo ?: "N/A")
                SheetDetailRow("Course", profile?.course ?: "B.Tech")
                SheetDetailRow("Branch", profile?.branch ?: "N/A")
                SheetDetailRow("Semester & Section", "Sem ${profile?.semester ?: 1} (${profile?.section ?: "A"})")
                SheetDetailRow("Academic Year", profile?.academicYear ?: "N/A")
                SheetDetailRow("Lab Group", profile?.groupName ?: "Not Assigned")
            }
        }
    }
}

@Composable
fun ContactFacultySheet(facultyList: List<ClassSubjectView>, onDismiss: () -> Unit) {
    Surface(
        color = PortalCard,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        border = BorderStroke(1.dp, PortalStroke),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("ASSIGNED FACULTY CONTACTS", color = PortalTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = PortalTrack)) {
                    Text("Close", color = PortalTextPrimary, fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(16.dp))

            if (facultyList.isEmpty()) {
                Text("No faculty contact records available.", color = PortalTextSecondary, fontSize = 13.sp)
            } else {
                facultyList.forEach { f ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(PortalBlue.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.School, null, tint = PortalBlue, modifier = Modifier.size(18.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(f.teacherName, color = PortalTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text("${f.subjectCode} · ${f.subjectName}", color = PortalTextSecondary, fontSize = 12.sp)
                            if (!f.teacherEmail.isNullOrBlank()) {
                                Text(f.teacherEmail!!, color = PortalBlue, fontSize = 11.5.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CollegeContactsSheet(onDismiss: () -> Unit) {
    val context = LocalContext.current

    Surface(
        color = PortalCard,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        border = BorderStroke(1.dp, PortalStroke),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("COLLEGE CONTACTS", color = PortalTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = PortalTrack)) {
                    Text("Close", color = PortalTextPrimary, fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(16.dp))

            Text("PHONE NUMBERS", color = PortalTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        runCatching { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:+917597676193"))) }
                    }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.Phone, null, tint = PortalBlue, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(12.dp))
                Text("+91 7597676193 / 7597244813", color = PortalTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(14.dp))

            Text("EMAIL ADDRESSES", color = PortalTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            listOf(
                "hr@mitrc.ac.in",
                "deansa@mitrc.ac.in",
                "admission@mitrc.ac.in"
            ).forEach { email ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            runCatching { context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$email"))) }
                        }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.MarkEmailRead, null, tint = PortalBlue, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(email, color = PortalTextPrimary, fontSize = 13.5.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
fun AboutAppSheet(onDismiss: () -> Unit) {
    Surface(
        color = PortalCard,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        border = BorderStroke(1.dp, PortalStroke),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("ABOUT MITRC APP", color = PortalTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = PortalTrack)) {
                    Text("Close", color = PortalTextPrimary, fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(16.dp))

            SheetDetailRow("Application Name", "MITRC Student Portal")
            SheetDetailRow("App Version", "v2.4.1 (Build 2026)")
            SheetDetailRow("Supported Platforms", "Android, iOS")
            SheetDetailRow("Developed By", "MITRC IT & Software Cell")
            SheetDetailRow("Affiliation", "AICTE Approved & BTU Bikaner Affiliated")
        }
    }
}

@Composable
private fun SheetDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = PortalTextSecondary, fontSize = 12.5.sp)
        Text(value, color = PortalTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

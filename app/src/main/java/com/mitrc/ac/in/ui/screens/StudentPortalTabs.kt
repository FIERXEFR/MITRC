package com.mitrc.ac.`in`.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
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
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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

            Column(
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

@Composable
private fun ScheduleSegmentedControl(
    mode: ScheduleMode,
    onModeChange: (ScheduleMode) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(PortalCard)
            .border(1.dp, PortalStroke, RoundedCornerShape(14.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        ScheduleMode.entries.forEach { entry ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (mode == entry) PortalBlue else Color.Transparent)
                    .clickable { onModeChange(entry) }
                    .padding(vertical = 11.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = entry.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (mode == entry) Color.White else PortalTextSecondary
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
        val grouped = remember(data.timetable) { data.timetable.groupBy { it.dayOfWeek } }
        val today = remember { currentDayOfWeek() }
        val dayNames = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")

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

        if (data.timetable.isEmpty()) {
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
                                    text = dayNames.getOrNull(day - 1) ?: "Day $day",
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
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(PortalTrack.copy(alpha = 0.55f)).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.width(64.dp)) {
            Text(text = "P${entry.startPeriod}${if (entry.endPeriod > entry.startPeriod) "-${entry.endPeriod}" else ""}", color = PortalPurple, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            if (!entry.groupName.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                Box(modifier = Modifier.clip(RoundedCornerShape(5.dp)).background(PortalAmber.copy(alpha = 0.2f)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                    Text(text = entry.groupName.orEmpty(), color = PortalBlue, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                }
            }
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = entry.subjectName, color = PortalTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (!entry.teacherName.isNullOrBlank()) {
                Text(text = entry.teacherName.orEmpty(), color = PortalTextSecondary, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (!entry.room.isNullOrBlank()) {
            Spacer(Modifier.width(8.dp))
            Text(text = entry.room.orEmpty(), color = PortalTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun NotesContent(data: StudentPortalData, notes: MutableList<PortalNote>) {
    val context = LocalContext.current
    val pdfNotes = data.pdfNotes
    var viewMode by remember { mutableStateOf(0) } // 0 = Teacher PDFs, 1 = My Quick Notes

    Column {
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
            TeacherPdfSection(data, pdfNotes, context)
        } else {
            PersonalNotesSection(notes)
        }
    }
}

@Composable
private fun TeacherPdfSection(data: StudentPortalData, pdfNotes: List<PortalPdfNote>, context: android.content.Context) {
    var selectedSubject by remember { mutableStateOf("All") }
    var selectedCategory by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }

    Column {
        PortalSectionLabel(text = "SUBJECT STUDY MATERIAL & PDFS")
        Spacer(Modifier.height(6.dp))
        Text(text = "Official lecture notes and question papers published by faculty", color = PortalTextSecondary, fontSize = 12.5.sp)
        Spacer(Modifier.height(14.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search by title or teacher...", fontSize = 12.5.sp) },
            leadingIcon = { Icon(Icons.Outlined.Search, null, modifier = Modifier.size(18.dp)) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))

        val filtered = pdfNotes.filter { note ->
            (selectedSubject == "All" || note.subjectName.equals(selectedSubject, ignoreCase = true)) &&
            (selectedCategory == "All" || note.category.equals(selectedCategory, ignoreCase = true)) &&
            (searchQuery.isBlank() || note.title.contains(searchQuery, true) || note.teacherName.contains(searchQuery, true))
        }

        if (filtered.isEmpty()) {
            PortalEmptyCard(message = "No matching notes found.")
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                filtered.forEach { note ->
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

@Composable
private fun PdfNoteCard(note: PortalPdfNote, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(18.dp), color = PortalCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, PortalStroke),
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
private fun PersonalNotesSection(notes: MutableList<PortalNote>) {
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }

    Column {
        PortalSectionLabel(text = "MY QUICK NOTES")
        Spacer(Modifier.height(12.dp))
        Surface(shape = RoundedCornerShape(18.dp), color = PortalCard, border = androidx.compose.foundation.BorderStroke(1.dp, PortalStroke), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = title, onValueChange = { title = it },
                    label = { Text("Title", fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = body, onValueChange = { body = it },
                    label = { Text("Write it down...", fontSize = 12.sp) },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        if (title.isNotBlank() || body.isNotBlank()) {
                            notes.add(0, PortalNote(id = System.currentTimeMillis(), title = title, body = body))
                            title = ""; body = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PortalBlue)
                ) {
                    Icon(Icons.Outlined.Add, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Save to Notebook")
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        PortalSectionLabel(text = "SAVED NOTES")
        Spacer(Modifier.height(12.dp))
        if (notes.isEmpty()) {
            PortalEmptyCard("No saved notes in your notebook.")
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                notes.forEach { note ->
                    SavedNoteCard(note) { notes.remove(note) }
                }
            }
        }
    }
}

@Composable
private fun SavedNoteCard(note: PortalNote, onDelete: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = PortalCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, PortalStroke),
        modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }
    ) {
        Column(modifier = Modifier.padding(16.dp).animateContentSize(spring(dampingRatio = Spring.DampingRatioLowBouncy))) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    if (note.title.isNotBlank()) {
                        Text(text = note.title, color = PortalTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(text = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(note.createdAt)), color = PortalTextSecondary, fontSize = 10.5.sp)
                }
                IconButton(onClick = onDelete) { Icon(Icons.Outlined.Delete, null, tint = PortalRose, modifier = Modifier.size(18.dp)) }
            }
            if (expanded && note.body.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                HorizontalDivider(color = PortalStroke)
                Spacer(Modifier.height(8.dp))
                Text(text = note.body, color = PortalTextPrimary, fontSize = 13.sp, lineHeight = 19.sp)
            } else if (note.body.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(text = note.body, color = PortalTextSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

private fun currentDayOfWeek(): Int = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_WEEK).let {
    if (it == java.util.Calendar.SUNDAY) 7 else it - 1
}

@Composable
internal fun PortalSectionLabel(text: String) {
    Text(text = text, color = PortalTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
}

@Composable
internal fun PortalEmptyCard(message: String) {
    Surface(shape = RoundedCornerShape(16.dp), color = PortalCardAlt, border = androidx.compose.foundation.BorderStroke(1.dp, PortalStroke), modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
            Text(text = message, color = PortalTextSecondary, fontSize = 13.sp, textAlign = TextAlign.Center)
        }
    }
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
    val scope = rememberCoroutineScope()
    PortalTabScaffold(scroll = scroll) {
        PortalSectionLabel(text = "MY PROFILE")
        Spacer(Modifier.height(12.dp))
        Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = PortalCard), border = androidx.compose.foundation.BorderStroke(1.dp, PortalStroke), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = data.profile?.name ?: "Student Name", fontWeight = FontWeight.Bold, color = PortalTextPrimary)
                Text(text = data.profile?.studentEmail ?: "", fontSize = 12.sp, color = PortalTextSecondary)
            }
        }
        Spacer(Modifier.height(24.dp))
        Button(onClick = { scope.launch { AuthRepository.signOut(); onSignedOut() } }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = PortalRose)) {
            Text("Sign Out")
        }
    }
}

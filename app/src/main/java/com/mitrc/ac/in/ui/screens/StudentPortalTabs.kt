package com.mitrc.ac.`in`.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.distinctUntilChanged
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
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MarkEmailRead
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Schedule
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
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Brush
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.mitrc.ac.`in`.auth.AuthRepository
import com.mitrc.ac.`in`.data.*
import com.mitrc.ac.`in`.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
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
    attendanceScroll: ScrollState,
    timetableScroll: ScrollState,
    notesScroll: ScrollState,
    examsScroll: ScrollState,
    mode: ScheduleMode,
    notes: MutableList<PortalNote>,
    onReload: suspend () -> Unit,
    onModeChange: (ScheduleMode) -> Unit
) {
    var isRefreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    val schedulePagerState = rememberPagerState(
        initialPage = ScheduleMode.entries.indexOf(mode).coerceAtLeast(0),
        pageCount = { ScheduleMode.entries.size }
    )

    LaunchedEffect(mode) {
        val targetIdx = ScheduleMode.entries.indexOf(mode)
        if (schedulePagerState.currentPage != targetIdx && !schedulePagerState.isScrollInProgress) {
            schedulePagerState.animateScrollToPage(targetIdx, animationSpec = tween(300, easing = FastOutSlowInEasing))
        }
    }

    LaunchedEffect(schedulePagerState) {
        snapshotFlow { schedulePagerState.settledPage }
            .distinctUntilChanged()
            .collect { page ->
                val newMode = ScheduleMode.entries.getOrElse(page) { ScheduleMode.ATTENDANCE }
                if (newMode.name != mode.name) {
                    onModeChange(newMode)
                }
            }
    }

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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            ScheduleSegmentedControl(
                mode = mode,
                onModeChange = { selectedMode ->
                    onModeChange(selectedMode)
                    scope.launch {
                        schedulePagerState.animateScrollToPage(
                            ScheduleMode.entries.indexOf(selectedMode),
                            animationSpec = tween(300, easing = FastOutSlowInEasing)
                        )
                    }
                }
            )
            Spacer(Modifier.height(18.dp))

            HorizontalPager(
                state = schedulePagerState,
                beyondViewportPageCount = 1,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { page ->
                val pageMode = ScheduleMode.entries[page]
                val pageScroll = when (pageMode) {
                    ScheduleMode.ATTENDANCE -> attendanceScroll
                    ScheduleMode.TIMETABLE -> timetableScroll
                    ScheduleMode.NOTES -> notesScroll
                    ScheduleMode.EXAMS -> examsScroll
                }
                Column(modifier = Modifier.fillMaxSize().verticalScroll(pageScroll)) {
                    when (pageMode) {
                        ScheduleMode.ATTENDANCE -> AttendanceContent(data = data)
                        ScheduleMode.TIMETABLE -> TimetableContent(data = data, refreshing = isRefreshing)
                        ScheduleMode.NOTES -> NotesContent(data = data, notes = notes)
                        ScheduleMode.EXAMS -> ExamsResultsContent(data = data)
                    }
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

// ---------------------------------------------------------------------------------------------
// Timetable - a week strip plus a single-day timeline, so the student reads one day at a time
// instead of scanning six stacked day cards.
// ---------------------------------------------------------------------------------------------

private val scheduleDayNames =
    listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")

private data class WeekDayCell(
    val dayOfWeek: Int,
    val shortLabel: String,
    val dayOfMonth: Int,
    val isToday: Boolean,
    val hasClasses: Boolean
)

private fun currentWeekCells(timetable: List<MyTimetableEntryView>): List<WeekDayCell> {
    val todayDow = currentDayOfWeek()
    val monday = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, -(todayDow - 1)) }
    val labels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    val busyDays = timetable.map { it.dayOfWeek }.toSet()

    return (0..5).map { offset ->
        val day = (monday.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, offset) }
        WeekDayCell(
            dayOfWeek = offset + 1,
            shortLabel = labels[offset],
            dayOfMonth = day.get(Calendar.DAY_OF_MONTH),
            isToday = offset + 1 == todayDow,
            hasClasses = busyDays.contains(offset + 1)
        )
    }
}

private fun currentWeekLabel(): String {
    val todayDow = currentDayOfWeek()
    val monday = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, -(todayDow - 1)) }
    val saturday = (monday.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, 5) }
    val format = SimpleDateFormat("d MMM", Locale.US)
    return "${monday.get(Calendar.DAY_OF_MONTH)} – ${format.format(saturday.time)}"
}

/**
 * The college's period clock, used whenever a timetable row has no explicit start/end time.
 * Lunch runs 1:15 PM - 1:45 PM, which is why the 5th lecture starts at 1:45 PM.
 */
private val periodStartMinutes = mapOf(
    1 to 9 * 60 + 15,
    2 to 10 * 60 + 15,
    3 to 11 * 60 + 15,
    4 to 12 * 60 + 15,
    5 to 13 * 60 + 45,
    6 to 14 * 60 + 45
)
private val periodEndMinutes = mapOf(
    1 to 10 * 60 + 15,
    2 to 11 * 60 + 15,
    3 to 12 * 60 + 15,
    4 to 13 * 60 + 15,
    5 to 14 * 60 + 45,
    6 to 15 * 60 + 45
)

private data class ClockTime(val label: String, val suffix: String)

private fun clockTimeOf(minutes: Int): ClockTime {
    val hour24 = (minutes / 60) % 24
    val minute = minutes % 60
    val suffix = if (hour24 >= 12) "PM" else "AM"
    val hour12 = if (hour24 % 12 == 0) 12 else hour24 % 12
    return ClockTime(String.format(Locale.US, "%d:%02d", hour12, minute), suffix)
}

/** Parses a stored "HH:mm" / "H:mm:ss" value; null when the row carries no usable clock time. */
private fun parseClock(raw: String?): ClockTime? {
    val value = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    val hour = value.substringBefore(':').toIntOrNull() ?: return null
    if (hour !in 0..23) return null
    val minute = value.substringAfter(':', "00").take(2).toIntOrNull()?.coerceIn(0, 59) ?: 0
    return clockTimeOf(hour * 60 + minute)
}

/** Start of a class: the stored clock time when present, otherwise the period's fixed slot. */
private fun entryStart(entry: MyTimetableEntryView): ClockTime? =
    parseClock(entry.startTime) ?: periodStartMinutes[entry.startPeriod]?.let(::clockTimeOf)

/** End of a class: the stored clock time, otherwise the end of its last period. */
private fun entryEnd(entry: MyTimetableEntryView): ClockTime? =
    parseClock(entry.endTime)
        ?: periodEndMinutes[entry.endPeriod]?.let(::clockTimeOf)
        ?: periodEndMinutes[entry.startPeriod]?.let(::clockTimeOf)

private fun entryTimeRange(entry: MyTimetableEntryView): String {
    val start = entryStart(entry) ?: return "Period ${entry.startPeriod}"
    val end = entryEnd(entry) ?: return "${start.label} ${start.suffix}"
    return "${start.label} ${start.suffix} – ${end.label} ${end.suffix}"
}

private fun roomLabel(value: String): String =
    if (value.startsWith("room", ignoreCase = true)) value else "Room $value"

@Composable
private fun TimetableContent(data: StudentPortalData, refreshing: Boolean) {
    val timetable = remember(data.timetable) {
        if (data.timetable.isEmpty()) sampleTimetableEntries() else data.timetable
    }
    val todayDow = remember { currentDayOfWeek() }
    var selectedDay by remember { mutableStateOf(todayDow.coerceIn(1, 6)) }

    val weekCells = remember(timetable, todayDow) { currentWeekCells(timetable) }
    val weekRange = remember(todayDow) { currentWeekLabel() }
    val dayEntries = remember(timetable, selectedDay) {
        timetable.filter { it.dayOfWeek == selectedDay }.sortedBy { it.startPeriod }
    }

    val isToday = selectedDay == todayDow
    val dayName = scheduleDayNames[selectedDay - 1]

    if (refreshing) {
        TimetableSkeleton()
        return
    }

    if (data.profile?.groupName.isNullOrBlank()) {
        LabGroupNotice()
        Spacer(Modifier.height(14.dp))
    }

    WeekPickerCard(
        weekRange = weekRange,
        cells = weekCells,
        selectedDay = selectedDay,
        onSelect = { selectedDay = it }
    )

    Spacer(Modifier.height(20.dp))

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = if (isToday) "Today's Classes" else "$dayName's Classes",
            color = PortalTextPrimary,
            fontSize = 17.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = when (dayEntries.size) {
                0 -> "No classes scheduled"
                1 -> "1 class scheduled"
                else -> "${dayEntries.size} classes scheduled"
            },
            color = PortalTextSecondary,
            fontSize = 11.5.sp
        )
    }

    Spacer(Modifier.height(14.dp))

    if (dayEntries.isEmpty()) {
        EmptyDayCard(dayName = dayName)
    } else {
        dayEntries.forEachIndexed { index, entry ->
            ScheduleTimelineRow(
                entry = entry,
                isLast = index == dayEntries.lastIndex
            )
        }
    }
}

@Composable
private fun WeekPickerCard(
    weekRange: String,
    cells: List<WeekDayCell>,
    selectedDay: Int,
    onSelect: (Int) -> Unit
) {
    val busyDays = cells.count { it.hasClasses }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = PortalCard,
        border = BorderStroke(1.dp, PortalStroke),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "THIS WEEK",
                        color = PortalTextSecondary,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.4.sp
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = weekRange,
                        color = PortalTextPrimary,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                Text(
                    text = if (busyDays == 0) "No classes" else "$busyDays of 6 days",
                    color = PortalTextSecondary,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                cells.forEach { cell ->
                    val selected = cell.dayOfWeek == selectedDay
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSelect(cell.dayOfWeek) }
                            .padding(vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = cell.shortLabel,
                            color = if (selected) PortalBlue else PortalTextSecondary,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.height(5.dp))
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .then(
                                    when {
                                        selected -> Modifier.background(PortalBlue)
                                        cell.isToday -> Modifier.border(
                                            1.dp,
                                            PortalBlue.copy(alpha = 0.45f),
                                            CircleShape
                                        )

                                        else -> Modifier
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = cell.dayOfMonth.toString(),
                                color = when {
                                    selected -> Color.White
                                    cell.isToday -> PortalBlue
                                    else -> PortalTextPrimary
                                },
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.height(5.dp))
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        !cell.hasClasses -> Color.Transparent
                                        selected -> PortalBlue
                                        else -> PortalBlue.copy(alpha = 0.35f)
                                    }
                                )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ScheduleTimelineRow(
    entry: MyTimetableEntryView,
    isLast: Boolean
) {
    val accent = when (entry.subjectKind.lowercase()) {
        "lab" -> PortalPurple
        "consultation" -> PortalAmber
        else -> PortalBlue
    }
    val start = entryStart(entry)
    val timeRange = entryTimeRange(entry)

    val place = entry.room?.takeUnless { it.isBlank() } ?: entry.classRoom?.takeUnless { it.isBlank() }
    val faculty = entry.teacherName?.takeUnless { it.isBlank() }
    val meta = listOfNotNull(place?.let(::roomLabel), faculty).joinToString("  ·  ")

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = accent.copy(alpha = 0.06f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.16f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = if (isLast) 0.dp else 12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.width(44.dp)) {
                Text(
                    text = start?.label ?: "—",
                    color = PortalTextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                if (start != null) {
                    Text(
                        text = start.suffix,
                        color = PortalTextSecondary,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Box(
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .width(1.dp)
                    .height(34.dp)
                    .background(accent.copy(alpha = 0.22f))
            )

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        text = entry.subjectName,
                        color = PortalTextPrimary,
                        fontSize = 14.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(7.dp))
                            .background(accent.copy(alpha = 0.14f))
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = entry.subjectKind.replaceFirstChar { it.uppercase() }
                                .ifBlank { "Class" },
                            color = accent,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                Spacer(Modifier.height(7.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.Schedule,
                        contentDescription = null,
                        tint = accent.copy(alpha = 0.85f),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = timeRange,
                        color = PortalTextSecondary,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                    if (entry.subjectCode.isNotBlank()) {
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(3.dp)
                                .clip(CircleShape)
                                .background(PortalTextSecondary.copy(alpha = 0.5f))
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = entry.subjectCode,
                            color = PortalTextSecondary.copy(alpha = 0.9f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (meta.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = meta,
                        color = PortalTextSecondary.copy(alpha = 0.9f),
                        fontSize = 10.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun TimetableSkeleton() {
    Column {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = PortalCard,
            border = BorderStroke(1.dp, PortalStroke),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp)) {
                Box(
                    modifier = Modifier
                        .width(64.dp)
                        .height(9.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .shimmerEffect()
                )
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .width(108.dp)
                        .height(15.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .shimmerEffect()
                )
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    repeat(6) {
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(18.dp)
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .shimmerEffect()
                            )
                            Spacer(Modifier.height(9.dp))
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .shimmerEffect()
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        Box(
            modifier = Modifier
                .width(148.dp)
                .height(17.dp)
                .clip(RoundedCornerShape(4.dp))
                .shimmerEffect()
        )
        Spacer(Modifier.height(7.dp))
        Box(
            modifier = Modifier
                .width(96.dp)
                .height(10.dp)
                .clip(RoundedCornerShape(3.dp))
                .shimmerEffect()
        )

        Spacer(Modifier.height(16.dp))

        repeat(3) { index ->
            TimetableRowSkeleton()
            if (index < 2) Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun TimetableRowSkeleton() {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = PortalCard,
        border = BorderStroke(1.dp, PortalStroke),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.width(44.dp)) {
                Box(
                    modifier = Modifier
                        .width(30.dp)
                        .height(13.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .shimmerEffect()
                )
                Spacer(Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .width(16.dp)
                        .height(8.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .shimmerEffect()
                )
            }
            Spacer(Modifier.width(25.dp))
            Column(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.72f)
                        .height(13.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .shimmerEffect()
                )
                Spacer(Modifier.height(9.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.46f)
                        .height(10.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .shimmerEffect()
                )
            }
            Spacer(Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .size(width = 42.dp, height = 18.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .shimmerEffect()
            )
        }
    }
}

@Composable
private fun EmptyDayCard(dayName: String) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = PortalCard,
        border = BorderStroke(1.dp, PortalStroke),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(PortalBlue.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.EventAvailable,
                    contentDescription = null,
                    tint = PortalBlue,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = "No classes on $dayName",
                color = PortalTextPrimary,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = "Enjoy the break — nothing is scheduled.",
                color = PortalTextSecondary,
                fontSize = 11.5.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun LabGroupNotice() {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = PortalRose.copy(alpha = 0.06f),
        border = BorderStroke(1.dp, PortalRose.copy(alpha = 0.28f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Outlined.Info,
                contentDescription = null,
                tint = PortalRose,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = "Lab group not assigned yet. Contact your co-ordinator.",
                color = PortalRose,
                fontSize = 11.5.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
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
        PortalSectionLabel(text = "RESULTS")

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
    var showLogoutDialog by remember { mutableStateOf(false) }
    var signingOut by remember { mutableStateOf(false) }
    var signOutComplete by remember { mutableStateOf(false) }

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
                title = "My Profile",
                subtitle = "View personal & academic details",
                icon = Icons.Outlined.Person,
                onClick = { showProfileSheet = true }
            )

            SettingsOptionCard(
                title = "Contact Assigned Faculty",
                subtitle = "Course teachers & contact details",
                icon = Icons.Outlined.School,
                onClick = { showFacultySheet = true }
            )

            SettingsOptionCard(
                title = "College Contacts",
                subtitle = "Official phone numbers & email addresses",
                icon = Icons.Outlined.Phone,
                onClick = { showCollegeContactsSheet = true }
            )

            SettingsOptionCard(
                title = "About MITRC App",
                subtitle = "App version, platform & details",
                icon = Icons.Outlined.Info,
                onClick = { showAboutAppSheet = true }
            )

            SettingsOptionCard(
                title = "Check for Update",
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
            onClick = { showLogoutDialog = true },
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = PortalBlue,
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

    PortalSheet(
        visible = showProfileSheet,
        onDismiss = { showProfileSheet = false }
    ) {
        ProfileDetailsSheet(
            profile = profile,
            onDismiss = { showProfileSheet = false }
        )
    }

    PortalSheet(
        visible = showFacultySheet,
        onDismiss = { showFacultySheet = false }
    ) {
        ContactFacultySheet(
            facultyList = data.faculty,
            onDismiss = { showFacultySheet = false }
        )
    }

    PortalSheet(
        visible = showCollegeContactsSheet,
        onDismiss = { showCollegeContactsSheet = false }
    ) {
        CollegeContactsSheet(
            onDismiss = { showCollegeContactsSheet = false }
        )
    }

    PortalSheet(
        visible = showAboutAppSheet,
        onDismiss = { showAboutAppSheet = false }
    ) {
        AboutAppSheet(
            onDismiss = { showAboutAppSheet = false }
        )
    }

    if (showLogoutDialog) {
        LogoutConfirmDialog(
            signingOut = signingOut,
            signOutComplete = signOutComplete,
            onConfirm = {
                signingOut = true
                scope.launch {
                    AuthRepository.signOut()
                    delay(250)
                    signOutComplete = true
                }
            },
            onDismiss = { if (!signingOut) showLogoutDialog = false },
            onExitAnimated = onSignedOut
        )
    }
}

@Composable
internal fun LogoutConfirmDialog(
    signingOut: Boolean,
    signOutComplete: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    onExitAnimated: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { visible = true }

    // Once sign-out is done, play the dialog's scale-down/fade exit before the screen swaps.
    LaunchedEffect(signOutComplete) {
        if (signOutComplete) {
            visible = false
            delay(200)
            onExitAnimated()
        }
    }

    val animateOutThenDismiss: () -> Unit = {
        visible = false
        scope.launch { delay(190); onDismiss() }
    }

    Dialog(
        onDismissRequest = { if (!signingOut) animateOutThenDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
                .padding(horizontal = 36.dp),
            contentAlignment = Alignment.Center
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = scaleIn(
                    initialScale = 0.88f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                ) + fadeIn(tween(220, easing = FastOutSlowInEasing)),
                exit = scaleOut(
                    targetScale = 0.92f,
                    animationSpec = tween(170, easing = FastOutSlowInEasing)
                ) + fadeOut(tween(150))
            ) {
                Surface(
                    shape = RoundedCornerShape(26.dp),
                    color = PortalCard,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(NavySoft, NavyDeep))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.AutoMirrored.Outlined.Logout,
                                contentDescription = null,
                                tint = GoldLight,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        Spacer(Modifier.height(16.dp))

                        Text(
                            text = "Sign Out?",
                            color = PortalTextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold
                        )

                        Spacer(Modifier.height(8.dp))

                        Text(
                            text = "You'll be signed out of your portal.\nYou can sign back in anytime.",
                            color = PortalTextSecondary,
                            fontSize = 13.5.sp,
                            lineHeight = 19.sp,
                            textAlign = TextAlign.Center
                        )

                        Spacer(Modifier.height(24.dp))

                        if (signingOut) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    color = PortalBlue,
                                    strokeWidth = 2.5.dp,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = "Signing out...",
                                    color = PortalTextSecondary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedButton(
                                    onClick = animateOutThenDismiss,
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, PortalStroke),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PortalTextPrimary),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                ) {
                                    Text("Cancel", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                }
                                Button(
                                    onClick = onConfirm,
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = PortalBlue,
                                        contentColor = Color.White
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                ) {
                                    Text("Sign Out", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
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

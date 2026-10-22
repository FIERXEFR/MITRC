package com.mitrc.ac.`in`.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.MarkEmailRead
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Place
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
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
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
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.ceil

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

            // Tapping the segmented control is the only way to change the segment: a swipe
            // belongs to the portal's own tab pager, and nesting two horizontal pagers left the
            // outer one stranded between Home and Academics.
            HorizontalPager(
                state = schedulePagerState,
                beyondViewportPageCount = 1,
                userScrollEnabled = false,
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
                        ScheduleMode.NOTES -> NotesContent(
                            data = data,
                            notes = notes,
                            loading = isRefreshing
                        )
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
    val attended = remember(data.attendance) { data.attendance.sumOf { it.attended } }
    val total = remember(data.attendance) { data.attendance.sumOf { it.total } }
    val pct = if (total > 0) attended * 100.0 / total else 0.0

    val today = remember { LocalDate.now() }
    val daysByDate = remember(data.dailyAttendance) {
        data.dailyAttendance.mapNotNull { day ->
            runCatching { LocalDate.parse(day.date) }.getOrNull()?.let { it to day }
        }.toMap()
    }
    val streak = remember(data.dailyAttendance) { attendanceStreak(data.dailyAttendance) }
    var visibleMonth by remember { mutableStateOf(YearMonth.from(today)) }

    // Every assigned subject gets a card, even before any attendance is marked for it. The summary
    // view only returns subjects that already have records, so it is merged onto the enrolment list.
    val attendanceBySubject = remember(data.attendance) {
        data.attendance.associateBy { it.classSubjectId }
    }
    val subjectRows = remember(data.subjects, data.attendance) {
        val rows = data.subjects.map { subject ->
            SubjectAttendanceRow(
                subject = subject,
                summary = attendanceBySubject[subject.classSubjectId]
            )
        }
        rows.ifEmpty {
            data.attendance.map { SubjectAttendanceRow(subject = null, summary = it) }
        }
    }

    Column {
        PortalSectionLabel(text = "ATTENDANCE RECORD")
        Spacer(Modifier.height(12.dp))

        AttendanceSummaryCard(attended = attended, total = total, pct = pct)

        Spacer(Modifier.height(20.dp))
        PortalSectionLabel(text = "MONTH AT A GLANCE")
        Spacer(Modifier.height(12.dp))
        AttendanceCalendarCard(
            month = visibleMonth,
            today = today,
            days = daysByDate,
            onPrevious = { visibleMonth = visibleMonth.minusMonths(1) },
            onNext = {
                if (visibleMonth < YearMonth.from(today)) {
                    visibleMonth = visibleMonth.plusMonths(1)
                }
            }
        )

        Spacer(Modifier.height(14.dp))
        AttendanceStreakCard(streak = streak, days = daysByDate, today = today)

        Spacer(Modifier.height(20.dp))
        PortalSectionLabel(text = "SUBJECT WISE")
        Spacer(Modifier.height(12.dp))

        if (subjectRows.isEmpty()) {
            PortalEmptyCard(message = "No subjects are assigned to your class yet.")
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                subjectRows.forEach { row ->
                    SubjectAttendanceCard(row)
                }
            }
        }
    }
}

/** Overall percentage: a ring instead of a flat badge, plus the classes needed to clear 75%. */
@Composable
private fun AttendanceSummaryCard(attended: Long, total: Long, pct: Double) {
    val onTrack = pct >= 75.0
    val accent = if (onTrack) PortalGreen else PortalRose
    val needed = classesNeededFor75(attended, total)

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = PortalCard,
        border = BorderStroke(1.dp, PortalStroke),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AttendanceRing(
                percent = pct,
                color = accent,
                diameter = 78.dp,
                strokeWidth = 7.dp,
                labelSize = 16.sp
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (onTrack) "You are on track" else "Needs attention",
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
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(accent)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = when {
                            total <= 0L -> "No classes marked yet"
                            onTrack -> "Above the 75% requirement"
                            else -> "Attend $needed more ${if (needed == 1L) "class" else "classes"} to reach 75%"
                        },
                        color = accent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

/** Extra classes required before `attended / total` reaches 75%. */
private fun classesNeededFor75(attended: Long, total: Long): Long {
    if (total <= 0L) return 0L
    if (attended * 100.0 / total >= 75.0) return 0L
    return ceil((0.75 * total - attended) / 0.25).toLong().coerceAtLeast(1L)
}

/** Rounded ring progress with the percentage centred inside. */
@Composable
private fun AttendanceRing(
    percent: Double,
    label: String = "${percent.toInt()}%",
    color: Color,
    diameter: Dp,
    strokeWidth: Dp,
    labelSize: TextUnit
) {
    val sweep by animateFloatAsState(
        targetValue = (percent / 100.0).toFloat().coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "attendanceRing"
    )

    Box(modifier = Modifier.size(diameter), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = strokeWidth.toPx()
            val inset = stroke / 2f
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(
                color = PortalTrack,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            if (sweep > 0.001f) {
                drawArc(
                    color = color,
                    startAngle = -90f,
                    sweepAngle = sweep * 360f,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            }
        }
        Text(
            text = label,
            color = color,
            fontSize = labelSize,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Attendance calendar - present days filled green, absent filled red, Sundays a red holiday dot
// and other institute holidays a blue dot.
// ---------------------------------------------------------------------------------------------

private val weekdayShortLabels = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")

/** Fixed-date institute holidays (MM-DD); festival dates vary yearly and are not guessed here. */
private val instituteHolidays = setOf("01-01", "01-26", "05-01", "08-15", "10-02", "12-25")

private fun isInstituteHoliday(date: LocalDate): Boolean =
    "%02d-%02d".format(date.monthValue, date.dayOfMonth) in instituteHolidays

@Composable
private fun AttendanceCalendarCard(
    month: YearMonth,
    today: LocalDate,
    days: Map<LocalDate, DailyAttendance>,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = PortalCard,
        border = BorderStroke(1.dp, PortalStroke),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = month.format(
                            DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())
                        ),
                        color = PortalTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Present, absent and holidays at a glance",
                        color = PortalTextSecondary,
                        fontSize = 10.5.sp
                    )
                }
                CalendarArrow(
                    icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    enabled = true,
                    onClick = onPrevious
                )
                Spacer(Modifier.width(6.dp))
                CalendarArrow(
                    icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    enabled = month < YearMonth.from(today),
                    onClick = onNext
                )
            }

            Spacer(Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                weekdayShortLabels.forEach { label ->
                    Text(
                        text = label,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        color = PortalTextSecondary,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.6.sp
                    )
                }
            }
            Spacer(Modifier.height(8.dp))

            CalendarMonthGrid(month = month, today = today, days = days)

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = PortalStroke)
            Spacer(Modifier.height(10.dp))
            AttendanceLegend()
        }
    }
}

@Composable
private fun CalendarArrow(icon: ImageVector, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(if (enabled) PortalBlue.copy(alpha = 0.08f) else Color.Transparent)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (enabled) PortalBlue else PortalStroke,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun CalendarMonthGrid(
    month: YearMonth,
    today: LocalDate,
    days: Map<LocalDate, DailyAttendance>
) {
    val leadingBlanks = month.atDay(1).dayOfWeek.value - 1 // Monday-first grid
    val daysInMonth = month.lengthOfMonth()
    val rowCount = (leadingBlanks + daysInMonth + 6) / 7

    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        repeat(rowCount) { row ->
            Row(modifier = Modifier.fillMaxWidth()) {
                repeat(7) { column ->
                    val dayOfMonth = row * 7 + column - leadingBlanks + 1
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        if (dayOfMonth in 1..daysInMonth) {
                            val date = month.atDay(dayOfMonth)
                            CalendarDayCell(date = date, today = today, record = days[date])
                        } else {
                            Spacer(Modifier.size(34.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarDayCell(date: LocalDate, today: LocalDate, record: DailyAttendance?) {
    val attendedDay = record?.attendedDay
    val isToday = date == today
    val isSunday = date.dayOfWeek == DayOfWeek.SUNDAY
    val isHoliday = isSunday || isInstituteHoliday(date)
    val showHolidayDot = isHoliday && attendedDay == null
    val dotColor = if (isSunday) PortalRose else PortalBlue

    val fill = when (attendedDay) {
        true -> PortalGreen
        false -> PortalRose
        null -> Color.Transparent
    }
    val textColor = when {
        attendedDay != null -> Color.White
        isToday -> PortalBlue
        isHoliday -> PortalTextSecondary
        date.isAfter(today) -> PortalTextSecondary.copy(alpha = 0.45f)
        else -> PortalTextPrimary
    }

    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(fill)
            .then(if (isToday) Modifier.border(1.5.dp, PortalBlue, CircleShape) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = date.dayOfMonth.toString(),
            color = textColor,
            fontSize = 12.sp,
            fontWeight = if (attendedDay != null || isToday) FontWeight.Bold else FontWeight.Medium,
            modifier = Modifier.offset(y = if (showHolidayDot) (-2).dp else 0.dp)
        )
        if (showHolidayDot) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 3.dp)
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
        }
    }
}

@Composable
private fun AttendanceLegend() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        LegendEntry(label = "Present", color = PortalGreen, filled = true)
        LegendEntry(label = "Absent", color = PortalRose, filled = true)
        LegendEntry(label = "Sunday", color = PortalRose, filled = false)
        LegendEntry(label = "Holiday", color = PortalBlue, filled = false)
    }
}

@Composable
private fun LegendEntry(label: String, color: Color, filled: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .then(
                    if (filled) Modifier.background(color)
                    else Modifier.border(1.5.dp, color, CircleShape)
                )
        )
        Spacer(Modifier.width(5.dp))
        Text(
            text = label,
            color = PortalTextSecondary,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Attendance streak - a bit of fun on top of the numbers.
// ---------------------------------------------------------------------------------------------

@Composable
private fun AttendanceStreakCard(
    streak: AttendanceStreak,
    days: Map<LocalDate, DailyAttendance>,
    today: LocalDate
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = PortalCard,
        border = BorderStroke(1.dp, PortalStroke),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(PortalAmber.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.LocalFireDepartment,
                        contentDescription = null,
                        tint = PortalAmber,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Attendance streak",
                        color = PortalTextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = if (streak.current > 0) {
                            "${pluralDays(streak.current)} in a row \u00b7 best ${streak.best}"
                        } else {
                            "Attend class today to start a streak"
                        },
                        color = PortalTextSecondary,
                        fontSize = 11.5.sp
                    )
                }
                Spacer(Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(PortalBlue)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "${streak.current}d",
                        color = PortalAmber,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                (6 downTo 0).forEach { offset ->
                    val date = today.minusDays(offset.toLong())
                    StreakDay(date = date, today = today, attended = days[date]?.attendedDay)
                }
            }
        }
    }
}

@Composable
private fun StreakDay(date: LocalDate, today: LocalDate, attended: Boolean?) {
    val fill = when (attended) {
        true -> PortalGreen
        false -> PortalRose
        null -> PortalTrack
    }
    val isToday = date == today

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(fill),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = date.dayOfWeek.name.take(1),
                color = if (attended == null) PortalTextSecondary else Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = date.dayOfMonth.toString(),
            color = if (isToday) PortalBlue else PortalTextSecondary,
            fontSize = 9.5.sp,
            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium
        )
    }
}

private fun pluralDays(count: Int): String = if (count == 1) "1 day" else "$count days"


@Composable
private fun SubjectAttendanceCard(row: SubjectAttendanceRow) {
    val summary = row.summary
    val code = summary?.subjectCode?.takeIf { it.isNotBlank() }
        ?: row.subject?.subjectCode
        ?: "SUBJECT"
    val name = summary?.subjectName?.takeIf { it.isNotBlank() }
        ?: row.subject?.subjectName
        ?: "Subject"
    val marked = summary != null && summary.total > 0
    val pct = summary?.percentage ?: 0.0
    val good = marked && pct >= 75.0
    val accent = when {
        !marked -> PortalTextSecondary
        good -> PortalGreen
        else -> PortalRose
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = PortalCard,
        border = BorderStroke(1.dp, PortalStroke),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(PortalBlue.copy(alpha = 0.12f))
                        .padding(horizontal = 9.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = code,
                        color = PortalBlue,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = name,
                    color = PortalTextPrimary,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 17.sp
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = if (marked) {
                        "Attended ${summary?.attended ?: 0} of ${summary?.total ?: 0} classes"
                    } else {
                        "No classes marked for this subject yet"
                    },
                    color = PortalTextSecondary,
                    fontSize = 11.sp
                )
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(accent)
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = when {
                            !marked -> "Not marked"
                            good -> "On track"
                            else -> "Below 75%"
                        },
                        color = accent,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(Modifier.width(14.dp))

            AttendanceRing(
                percent = pct,
                label = if (marked) "${pct.toInt()}%" else "\u2014",
                color = accent,
                diameter = 58.dp,
                strokeWidth = 5.5.dp,
                labelSize = 12.5.sp
            )
        }
    }
}

/** One row of the subject-wise list: the enrolled subject plus its summary when one exists. */
private data class SubjectAttendanceRow(
    val subject: ClassSubjectView?,
    val summary: AttendanceSummaryView?
)

// ---------------------------------------------------------------------------------------------
// Timetable - a week strip plus a single-day timeline, so the student reads one day at a time
// instead of scanning six stacked day cards.
// -------------------------------------------------------------------------------------------

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
    val timetable = data.timetable
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

    // Same overline treatment as "ATTENDANCE RECORD", so the tab reads as a titled screen
    // instead of starting abruptly with the week strip.
    PortalSectionLabel(text = "CLASS TIMETABLE")
    Spacer(Modifier.height(12.dp))

    if (data.timetable.isEmpty()) {
        PortalEmptyCard(message = "No timetable has been published for your class yet.")
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
private fun NotesContent(
    data: StudentPortalData,
    notes: MutableList<PortalNote>,
    loading: Boolean = false
) {
    if (loading) {
        NotesTabSkeleton()
        return
    }

    val subjects = data.subjects
    val facultyGroups = remember(subjects, data.pdfNotes) {
        buildFacultyNoteGroups(subjects, data.pdfNotes)
    }

    var searchQuery by remember { mutableStateOf("") }
    val expandedGroups = remember { mutableStateMapOf<String, Boolean>() }

    val query = searchQuery.trim()
    val visibleGroups = remember(facultyGroups, query) {
        if (query.isEmpty()) {
            facultyGroups
        } else {
            facultyGroups.mapNotNull { group ->
                val groupMatches = group.subjectName.contains(query, true) ||
                        group.subjectCode.contains(query, true) ||
                        group.teacherName.contains(query, true)
                if (groupMatches) {
                    group
                } else {
                    val matchedChapters = group.chapters.filter { it.title.contains(query, true) }
                    if (matchedChapters.isEmpty()) null else group.copy(chapters = matchedChapters)
                }
            }
        }
    }

    // A search should reveal what matched instead of leaving every card collapsed.
    LaunchedEffect(query) {
        if (query.isNotEmpty()) {
            visibleGroups.forEach { expandedGroups[it.key] = true }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PortalSectionLabel(text = "FACULTY & SUBJECT NOTES")
            Spacer(Modifier.weight(1f))
            Text(
                text = "${facultyGroups.size} subjects",
                color = PortalTextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Text(
            text = "Open a faculty card to see the chapter-wise notes shared for that subject.",
            color = PortalTextSecondary,
            fontSize = 12.sp,
            lineHeight = 17.sp
        )

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = {
                Text(
                    text = "Search subject, faculty or chapter...",
                    color = PortalTextSecondary,
                    fontSize = 12.5.sp
                )
            },
            leadingIcon = {
                Icon(
                    Icons.Outlined.Search,
                    contentDescription = null,
                    tint = PortalTextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            },
            trailingIcon = if (searchQuery.isNotBlank()) {
                {
                    Icon(
                        Icons.Outlined.Close,
                        contentDescription = "Clear search",
                        tint = PortalTextSecondary,
                        modifier = Modifier
                            .size(18.dp)
                            .clickable {
                                // Clearing the query also folds every card the search expanded.
                                searchQuery = ""
                                expandedGroups.clear()
                            }
                    )
                }
            } else null,
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            textStyle = TextStyle(color = PortalTextPrimary, fontSize = 12.5.sp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = PortalTextPrimary,
                unfocusedTextColor = PortalTextPrimary,
                focusedContainerColor = PortalCard,
                unfocusedContainerColor = PortalCard,
                focusedBorderColor = PortalBlue,
                unfocusedBorderColor = PortalStroke,
                cursorColor = PortalBlue,
                focusedLeadingIconColor = PortalTextSecondary,
                unfocusedLeadingIconColor = PortalTextSecondary,
                focusedTrailingIconColor = PortalTextSecondary,
                unfocusedTrailingIconColor = PortalTextSecondary,
                focusedPlaceholderColor = PortalTextSecondary,
                unfocusedPlaceholderColor = PortalTextSecondary
            ),
            modifier = Modifier.fillMaxWidth()
        )

        if (visibleGroups.isEmpty()) {
            PortalEmptyCard(
                message = if (facultyGroups.isEmpty()) {
                    "No subjects are assigned to your class yet."
                } else {
                    "No faculty or chapter matches \"$query\"."
                }
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                visibleGroups.forEach { group ->
                    FacultyNotesCard(
                        group = group,
                        expanded = expandedGroups[group.key] == true,
                        onToggle = { expandedGroups[group.key] = expandedGroups[group.key] != true }
                    )
                }
            }
        }
    }
}

/** Shimmer preview that mirrors the Notes tab while it reloads. */
@Composable
private fun NotesTabSkeleton() {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(168.dp)
                    .height(11.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .shimmerEffect()
            )
            Spacer(Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .width(62.dp)
                    .height(11.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .shimmerEffect()
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth(0.82f)
                .height(10.dp)
                .clip(RoundedCornerShape(4.dp))
                .shimmerEffect()
        )

        Surface(
            shape = RoundedCornerShape(14.dp),
            color = PortalCard,
            border = BorderStroke(1.dp, PortalStroke),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .shimmerEffect()
                )
                Spacer(Modifier.width(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.55f)
                        .height(11.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .shimmerEffect()
                )
            }
        }

        repeat(4) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = PortalCard,
                border = BorderStroke(1.dp, PortalStroke),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(78.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .shimmerEffect()
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .width(132.dp)
                                .height(12.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .shimmerEffect()
                        )
                        Spacer(Modifier.height(7.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.72f)
                                .height(10.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .shimmerEffect()
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Box(
                        modifier = Modifier
                            .width(54.dp)
                            .height(10.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .shimmerEffect()
                    )
                }
            }
        }
    }
}

@Composable
private fun FacultyNotesCard(
    group: FacultyNotesGroup,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(240, easing = FastOutSlowInEasing),
        label = "chevronRotation"
    )
    val containerColor by animateColorAsState(
        targetValue = if (expanded) PortalBlue.copy(alpha = 0.06f) else PortalCard,
        animationSpec = tween(200),
        label = "cardContainer"
    )
    val borderColor by animateColorAsState(
        targetValue = if (expanded) PortalBlue.copy(alpha = 0.45f) else PortalStroke,
        animationSpec = tween(200),
        label = "cardBorder"
    )

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = tween(240, easing = FastOutSlowInEasing))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onToggle),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(PortalBlue.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = facultyInitials(group.teacherName),
                        color = PortalBlue,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = group.teacherName,
                        color = PortalTextPrimary,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = "${group.subjectName} \u00b7 ${group.subjectCode}",
                        color = PortalTextSecondary,
                        fontSize = 11.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(Modifier.width(10.dp))

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (group.chapters.isEmpty()) "No notes" else "${group.chapters.size} chapters",
                        color = PortalTextSecondary,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(2.dp))
                    Icon(
                        Icons.Outlined.ExpandMore,
                        contentDescription = if (expanded) "Hide chapters" else "Show chapters",
                        tint = PortalBlue,
                        modifier = Modifier
                            .size(22.dp)
                            .rotate(chevronRotation)
                    )
                }
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(
                    animationSpec = tween(240, easing = FastOutSlowInEasing)
                ) + fadeIn(tween(180)),
                exit = shrinkVertically(
                    animationSpec = tween(200, easing = FastOutSlowInEasing)
                ) + fadeOut(tween(120))
            ) {
                Column {
                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider(color = PortalStroke)
                    if (group.chapters.isEmpty()) {
                        Text(
                            text = "No chapters published for this subject yet.",
                            color = PortalTextSecondary,
                            fontSize = 11.5.sp,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    } else {
                        val context = LocalContext.current
                        group.chapters.forEachIndexed { index, note ->
                            ChapterRow(index = index + 1, note = note) {
                                val opened = runCatching {
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW, Uri.parse(note.driveUrl))
                                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    )
                                }.isSuccess
                                if (!opened) {
                                    Toast.makeText(
                                        context,
                                        "No app can open this link",
                                        Toast.LENGTH_SHORT
                                    ).show()
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
private fun ChapterRow(index: Int, note: PortalPdfNote, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(PortalBlue.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$index",
                color = PortalBlue,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = note.title,
                color = PortalTextPrimary,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 16.sp
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "${noteCategoryLabel(note.category)} \u00b7 ${note.createdAt}",
                color = PortalTextSecondary,
                fontSize = 10.5.sp
            )
        }

        Spacer(Modifier.width(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Outlined.PictureAsPdf,
                contentDescription = null,
                tint = PortalRose,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(6.dp))
            Icon(
                Icons.AutoMirrored.Outlined.OpenInNew,
                contentDescription = "Open in Drive",
                tint = PortalBlue,
                modifier = Modifier.size(15.dp)
            )
        }
    }
}

/** "Dr. Meena Sharma" -> "MS", "Rahul Verma" -> "RV"; a single word falls back to its initial. */
private fun facultyInitials(name: String): String {
    val stripped = name.replace(Regex("^(Dr|Mr|Mrs|Ms|Prof)\\.?\\s+", RegexOption.IGNORE_CASE), "")
    val words = stripped.split(' ').filter { it.isNotBlank() }
    return when {
        words.isEmpty() -> "F"
        words.size == 1 -> words.first().take(1).uppercase()
        else -> (words.first().take(1) + words[1].take(1)).uppercase()
    }
}

private fun noteCategoryLabel(category: String): String = when (category.lowercase()) {
    "notes" -> "Notes"
    "assignment" -> "Assignment"
    "question_paper" -> "Question paper"
    "syllabus" -> "Syllabus"
    "lab_manual" -> "Lab manual"
    else -> "Material"
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
    val events = remember { sampleCollegeEvents() }

    PortalTabScaffold(scroll = scroll) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PortalSectionLabel(text = "CAMPUS EVENTS")
            Spacer(Modifier.weight(1f))
            Text(
                text = "${events.size} events",
                color = PortalTextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(Modifier.height(6.dp))
        Text(
            text = "Hackathons, fests and exhibitions hosted at MITRC.",
            color = PortalTextSecondary,
            fontSize = 12.sp
        )

        Spacer(Modifier.height(16.dp))

        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            events.forEach { event -> EventListCard(event = event) }
        }

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun EventListCard(event: CollegeEvent) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = PortalCard,
        border = BorderStroke(1.dp, PortalStroke),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(168.dp)
            ) {
                AsyncImage(
                    model = event.image,
                    contentDescription = event.title,
                    contentScale = ContentScale.Crop,
                    placeholder = ColorPainter(PortalCardAlt),
                    error = ColorPainter(PortalCardAlt),
                    fallback = ColorPainter(PortalCardAlt),
                    modifier = Modifier.fillMaxSize()
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Transparent,
                                    Color(0x00001736),
                                    Color(0x8C001736)
                                )
                            )
                        )
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(Color.White.copy(alpha = 0.18f))
                        .border(1.dp, Color.White.copy(alpha = 0.38f), RoundedCornerShape(7.dp))
                        .padding(horizontal = 9.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = event.category,
                        color = GoldLight,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.8.sp
                    )
                }
            }

            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = event.title,
                    color = PortalTextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 19.sp
                )

                Spacer(Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Outlined.Schedule,
                        contentDescription = null,
                        tint = PortalBlue,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = event.whenLabel,
                        color = PortalTextSecondary,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Outlined.Place,
                        contentDescription = null,
                        tint = PortalBlue,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = event.venue,
                        color = PortalTextSecondary,
                        fontSize = 11.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
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

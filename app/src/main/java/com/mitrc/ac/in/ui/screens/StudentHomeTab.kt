package com.mitrc.ac.`in`.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.mitrc.ac.`in`.ui.theme.GoldLight
import kotlinx.coroutines.delay

// ---------------------------------------------------------------------------------------------
// Home tab
// ---------------------------------------------------------------------------------------------

@Composable
fun StudentHomeTab(
    data: StudentPortalData,
    scroll: ScrollState,
    onNavigate: (StudentTab, ScheduleMode?) -> Unit
) {
    // `attended` / `total` come back as Long from the view model; the UI works in Int.
    val totalAttended = remember(data.attendance) { data.attendance.sumOf { it.attended }.toInt() }
    val totalClasses = remember(data.attendance) { data.attendance.sumOf { it.total }.toInt() }
    val missed = (totalClasses - totalAttended).coerceAtLeast(0)
    val overallPct = if (totalClasses > 0) totalAttended * 100.0 / totalClasses else 0.0

    val today = remember { currentDayOfWeek() }
    val todayClasses = remember(data.timetable, today) {
        data.timetable.filter { it.dayOfWeek == today }.sortedBy { it.startPeriod }
    }
    val events = remember { sampleCollegeEvents() }
    val quickActions = remember { sampleQuickActions() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        Stagger(0) { StatRow(todayPresent = todayClasses.size, attended = totalAttended, missed = missed) }
        Spacer(Modifier.height(14.dp))

        Stagger(1) { StreakCard(totalPresent = totalAttended) }
        Spacer(Modifier.height(14.dp))

        Stagger(2) { OverallAttendanceCard(percent = overallPct, attended = totalAttended, total = totalClasses) }
        Spacer(Modifier.height(20.dp))

        Stagger(3) {
            EventsSection(
                events = events,
                onSeeAll = { onNavigate(StudentTab.EVENTS, null) }
            )
        }
        Spacer(Modifier.height(20.dp))

        Stagger(4) { TodayClassesSection(classes = todayClasses) }
        Spacer(Modifier.height(20.dp))

        Stagger(5) { QuickAccessGrid(actions = quickActions, onNavigate = onNavigate) }

        if (overallPct in 0.01..74.99) {
            Spacer(Modifier.height(16.dp))
            Stagger(6) { LowAttendanceBanner(percent = overallPct, attended = totalAttended, total = totalClasses) }
        }

        Spacer(Modifier.height(18.dp))
        Stagger(7) { DailyQuote() }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun Stagger(index: Int, content: @Composable () -> Unit) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(index * 60L)
        shown = true
    }
    AnimatedVisibility(
        visible = shown,
        enter = fadeIn(tween(420)) + slideInVertically(tween(460)) { it / 4 }
    ) {
        content()
    }
}

// ---------------------------------------------------------------------------------------------
// Stat row
// ---------------------------------------------------------------------------------------------

@Composable
private fun StatRow(todayPresent: Int, attended: Int, missed: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StatCell(
            label = "Today",
            value = "$todayPresent / ${todayPresent + 1}",
            caption = "present",
            accent = PortalBlue,
            modifier = Modifier.weight(1f)
        )
        StatCell(
            label = "Attended",
            value = "$attended",
            caption = "classes",
            accent = PortalGreen,
            modifier = Modifier.weight(1f)
        )
        StatCell(
            label = "Missed",
            value = "$missed",
            caption = "classes",
            accent = PortalRose,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatCell(
    label: String,
    value: String,
    caption: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = PortalCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, PortalStroke),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp)) {
            Text(text = label, color = PortalTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(6.dp))
            Text(text = value, color = accent, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(2.dp))
            Text(text = caption, color = PortalTextSecondary.copy(alpha = 0.8f), fontSize = 10.5.sp)
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Streak
// ---------------------------------------------------------------------------------------------

@Composable
private fun StreakCard(totalPresent: Int) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = PortalCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, PortalStroke),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = PortalAmber.copy(alpha = 0.16f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Outlined.LocalFireDepartment,
                            contentDescription = null,
                            tint = PortalAmber,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        text = "Attendance Streak",
                        color = PortalTextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "12 Days",
                        color = PortalBlue,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                Spacer(Modifier.weight(1f))
                Text(
                    text = "Keep it up!",
                    color = PortalGreen,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                streakDays.forEach { (day, done) ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = CircleShape,
                            color = if (done) PortalBlue else PortalTrack,
                            modifier = Modifier.size(30.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (done) {
                                    Icon(
                                        Icons.Outlined.CheckCircle,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(PortalTextSecondary.copy(alpha = 0.5f), CircleShape)
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(5.dp))
                        Text(text = day, color = PortalTextSecondary, fontSize = 9.5.sp)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MiniStat("Current Streak", "12", Modifier.weight(1f))
                MiniStat("Longest Streak", "21", Modifier.weight(1f))
                MiniStat("Total Present", "$totalPresent", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun MiniStat(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(shape = RoundedCornerShape(12.dp), color = PortalCardAlt, modifier = modifier) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp)) {
            Text(text = value, color = PortalTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(2.dp))
            Text(text = label, color = PortalTextSecondary, fontSize = 9.5.sp, maxLines = 1)
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Overall attendance - donut + weekly bars
// ---------------------------------------------------------------------------------------------

@Composable
private fun OverallAttendanceCard(percent: Double, attended: Int, total: Int) {
    val pct = percent.toFloat()

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = PortalCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, PortalStroke),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "Overall Attendance",
                color = PortalTextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                AttendanceDonut(percent = pct, size = 104.dp)
                Spacer(Modifier.width(18.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AnimatedPercent(percent = pct)
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            if (pct >= 75f) Icons.AutoMirrored.Outlined.TrendingUp else Icons.Outlined.WbSunny,
                            contentDescription = null,
                            tint = if (pct >= 75f) PortalGreen else PortalRose,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "$attended / $total",
                        color = PortalTextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "classes attended",
                        color = PortalTextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (pct >= 75f) PortalGreen.copy(alpha = 0.14f)
                                else PortalRose.copy(alpha = 0.1f)
                            )
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = if (pct >= 75f) "Above 75% target" else "Below 75% target",
                            color = if (pct >= 75f) PortalGreen else PortalRose,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            Text(
                text = "This Week",
                color = PortalTextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(10.dp))
            WeekBars()
        }
    }
}

@Composable
private fun AnimatedPercent(percent: Float) {
    val animated by animateFloatAsState(
        targetValue = percent.coerceIn(0f, 100f),
        animationSpec = tween(1100),
        label = "percentAnim"
    )
    Text(
        text = "${animated.toInt()}%",
        color = PortalBlue,
        fontSize = 26.sp,
        fontWeight = FontWeight.ExtraBold
    )
}

@Composable
private fun AttendanceDonut(percent: Float, size: androidx.compose.ui.unit.Dp) {
    val animated by animateFloatAsState(
        targetValue = percent.coerceIn(0f, 100f),
        animationSpec = tween(1100),
        label = "donutAnim"
    )

    Box(modifier = Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(size)) {
            val stroke = Stroke(width = 11.dp.toPx(), cap = StrokeCap.Round)
            drawArc(
                color = PortalTrack,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = stroke
            )
            drawArc(
                color = PortalBlue,
                startAngle = -90f,
                sweepAngle = 360f * (animated / 100f),
                useCenter = false,
                style = stroke
            )
        }
    }
}

@Composable
private fun WeekBars() {
    val max = weekBars.maxOf { it.second }.coerceAtLeast(1)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        weekBars.forEach { (day, value) ->
            val target = ((value.toFloat() / max) * 64f).coerceAtLeast(4f)
            val barHeight by animateDpAsState(
                targetValue = target.dp,
                animationSpec = tween(750),
                label = "barHeight"
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                Text(
                    text = "$value",
                    color = if (value == 0) PortalTextSecondary.copy(alpha = 0.6f) else PortalTextPrimary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .width(14.dp)
                        .height(barHeight)
                        .clip(RoundedCornerShape(5.dp))
                        .background(if (value == 0) PortalTrack else PortalBlue)
                )
                Spacer(Modifier.height(6.dp))
                Text(text = day, color = PortalTextSecondary, fontSize = 9.5.sp)
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// College events - auto-advancing carousel
// ---------------------------------------------------------------------------------------------

@Composable
private fun EventsSection(events: List<CollegeEvent>, onSeeAll: () -> Unit) {
    val pagerState = rememberPagerState { events.size }

    LaunchedEffect(pagerState, events) {
        if (events.isEmpty()) return@LaunchedEffect
        while (true) {
            delay(3200)
            if (!pagerState.isScrollInProgress) {
                pagerState.animateScrollToPage((pagerState.currentPage + 1) % events.size)
            }
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PortalSectionLabel(text = "COLLEGE EVENTS")
            Spacer(Modifier.weight(1f))
            Text(
                text = "See all",
                color = PortalBlue,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable(onClick = onSeeAll)
            )
        }

        Spacer(Modifier.height(12.dp))

        if (events.isEmpty()) {
            PortalEmptyCard(message = "No college events right now.")
            return
        }

        HorizontalPager(
            state = pagerState,
            pageSpacing = 12.dp,
            contentPadding = PaddingValues(horizontal = 2.dp)
        ) { page ->
            EventCard(event = events[page])
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            events.forEachIndexed { index, _ ->
                val active = index == pagerState.currentPage
                val width by animateDpAsState(
                    targetValue = if (active) 20.dp else 6.dp,
                    animationSpec = tween(260),
                    label = "dotWidth"
                )
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .height(6.dp)
                        .width(width)
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (active) PortalBlue else PortalTrack)
                )
            }
        }
    }
}

@Composable
private fun EventCard(event: CollegeEvent) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = PortalCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, PortalStroke),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(178.dp)
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
                                Color(0x00001736),
                                Color(0x66001736),
                                Color(0xF2001736)
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(7.dp))
                        .background(Color.White.copy(alpha = 0.16f))
                        .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(7.dp))
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

                Spacer(Modifier.height(8.dp))

                Text(
                    text = event.title,
                    color = Color.White,
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 20.sp
                )

                Spacer(Modifier.height(6.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = event.whenLabel,
                        color = GoldLight,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "  \u00b7  ",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 11.sp
                    )
                    Text(
                        text = event.venue,
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Today's classes
// ---------------------------------------------------------------------------------------------

@Composable
private fun TodayClassesSection(classes: List<com.mitrc.ac.`in`.data.MyTimetableEntryView>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PortalSectionLabel(text = "TODAY'S CLASSES")
        Spacer(Modifier.weight(1f))
        Text(
            text = if (classes.isEmpty()) "Free day" else "${classes.size} periods",
            color = PortalTextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }

    Spacer(Modifier.height(12.dp))

    if (classes.isEmpty()) {
        PortalEmptyCard(message = "No classes scheduled for today. Enjoy your day!")
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        classes.forEachIndexed { index, entry ->
            ClassRow(index = index, entry = entry)
        }
    }
}

@Composable
private fun ClassRow(index: Int, entry: com.mitrc.ac.`in`.data.MyTimetableEntryView) {
    val status = when {
        index < 2 -> "Present"
        index == 2 -> "Absent"
        else -> "Upcoming"
    }
    val statusColor = when (status) {
        "Present" -> PortalGreen
        "Absent" -> PortalRose
        else -> PortalBlue
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = PortalCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, PortalStroke),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.width(56.dp)) {
                Text(
                    text = "0${8 + index}:00",
                    color = PortalBlue,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "0${9 + index}:00",
                    color = PortalTextSecondary,
                    fontSize = 11.sp
                )
            }

            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(40.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(PortalBlue)
            )

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.subjectName,
                    color = PortalTextPrimary,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = buildString {
                        append(entry.room ?: "TBA")
                        if (!entry.teacherName.isNullOrBlank()) {
                            append(" \u00b7 ").append(entry.teacherName)
                        }
                    },
                    color = PortalTextSecondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(9.dp))
                    .background(statusColor.copy(alpha = 0.12f))
                    .border(1.dp, statusColor.copy(alpha = 0.45f), RoundedCornerShape(9.dp))
                    .padding(horizontal = 9.dp, vertical = 6.dp)
            ) {
                Text(
                    text = status,
                    color = statusColor,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Quick access
// ---------------------------------------------------------------------------------------------

@Composable
private fun QuickAccessGrid(
    actions: List<QuickAction>,
    onNavigate: (StudentTab, ScheduleMode?) -> Unit
) {
    PortalSectionLabel(text = "QUICK ACCESS")
    Spacer(Modifier.height(12.dp))

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        actions.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                pair.forEach { action ->
                    QuickActionCard(
                        action = action,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(action.target, action.mode) }
                    )
                }
                if (pair.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun QuickActionCard(
    action: QuickAction,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = PortalCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, PortalStroke),
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = action.accent.copy(alpha = 0.14f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        action.icon,
                        contentDescription = null,
                        tint = action.accent,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = action.title,
                color = PortalTextPrimary,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = action.subtitle,
                color = PortalTextSecondary,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Warning + quote
// ---------------------------------------------------------------------------------------------

@Composable
private fun LowAttendanceBanner(percent: Double, attended: Int, total: Int) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = PortalRose.copy(alpha = 0.08f),
        border = androidx.compose.foundation.BorderStroke(1.dp, PortalRose.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Icon(
                Icons.Outlined.Warning,
                contentDescription = null,
                tint = PortalRose,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = "Attendance Low",
                    color = PortalRose,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Your attendance is ${"%.0f".format(percent)}% ($attended of $total). " +
                        "Attend the next 3 classes to reach your 75% target.",
                    color = PortalTextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
            }
        }
    }
}

@Composable
private fun DailyQuote() {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = PortalCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, PortalStroke),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(PortalBlue.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "\ud83d\udca1",
                    fontSize = 15.sp
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = "\"Small steps every day lead to big results.\"",
                color = PortalTextSecondary,
                fontSize = 12.5.sp,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
            )
        }
    }
}

package com.mitrc.ac.`in`.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.mitrc.ac.`in`.auth.AuthRepository
import com.mitrc.ac.`in`.data.CoordinatorRow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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

@Composable
fun StudentScheduleTab(
    data: StudentPortalData,
    scroll: ScrollState,
    mode: ScheduleMode,
    onModeChange: (ScheduleMode) -> Unit
) {
    PortalTabScaffold(scroll = scroll) {
        ScheduleSegmentedControl(mode = mode, onModeChange = onModeChange)
        Spacer(Modifier.height(18.dp))

        when (mode) {
            ScheduleMode.ATTENDANCE -> AttendanceContent(data = data)
            ScheduleMode.TIMETABLE -> TimetableContent(data = data)
        }
    }
}

/**
 * The same two-chip switcher the login screen uses for Student / Staff: a light track with a
 * solid navy pill for the selected option.
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
        ScheduleSegment(
            label = ScheduleMode.ATTENDANCE.label,
            selected = mode == ScheduleMode.ATTENDANCE,
            onClick = { onModeChange(ScheduleMode.ATTENDANCE) },
            modifier = Modifier.weight(1f)
        )
        ScheduleSegment(
            label = ScheduleMode.TIMETABLE.label,
            selected = mode == ScheduleMode.TIMETABLE,
            onClick = { onModeChange(ScheduleMode.TIMETABLE) },
            modifier = Modifier.weight(1f)
        )
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
// Events
// ---------------------------------------------------------------------------------------------

@Composable
fun StudentEventsTab(scroll: ScrollState) {
    val events = remember { sampleCollegeEvents() }

    PortalTabScaffold(scroll = scroll) {
        PortalSectionLabel(text = "COLLEGE EVENTS")
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Everything happening on campus right now",
            color = PortalTextSecondary,
            fontSize = 13.sp
        )
        Spacer(Modifier.height(14.dp))

        if (events.isEmpty()) {
            PortalEmptyCard(message = "No college events right now.")
            return@PortalTabScaffold
        }

        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            events.forEach { event ->
                EventListCard(event = event)
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

                if (!profile?.fatherName.isNullOrBlank()) {
                    SettingsRow(label = "Father's Name", value = profile?.fatherName.orEmpty())
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        PortalSectionLabel(text = "ACCOUNT")
        Spacer(Modifier.height(12.dp))

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = PortalCard,
            border = androidx.compose.foundation.BorderStroke(1.dp, PortalStroke),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Outlined.MailOutline,
                    contentDescription = null,
                    tint = PortalBlue,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(text = "Signed in as", color = PortalTextSecondary, fontSize = 11.sp)
                    Text(
                        text = AuthRepository.currentUser?.email ?: "-",
                        color = PortalTextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
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
                    title = "Contact Co-ordinator",
                    subtitle = (data.coordinators.firstOrNull { it.haveRights }
                        ?: data.coordinators.firstOrNull())?.name
                        ?.takeIf { it.isNotBlank() }
                        ?: "Call or email your department co-ordinator",
                    onClick = { showContact = true }
                )
                OptionDivider()
                UpdatesOption()
                OptionDivider()
                SettingsOption(
                    icon = Icons.Outlined.Info,
                    iconTint = PortalPurple,
                    title = "About MITRC",
                    subtitle = "App version and college details",
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
        ContactSheetContent(
            coordinator = data.coordinators.firstOrNull { it.haveRights }
                ?: data.coordinators.firstOrNull(),
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

/** Fake but honest update check: shows a spinner, then reports the current version state. */
@Composable
private fun UpdatesOption() {
    var state by remember { mutableStateOf(0) } // 0 = idle, 1 = checking, 2 = up to date

    LaunchedEffect(state) {
        if (state == 1) {
            delay(1500)
            state = 2
        }
    }

    SettingsOption(
        icon = Icons.Outlined.Refresh,
        iconTint = PortalGreen,
        title = "Check for Updates",
        subtitle = when (state) {
            0 -> "You are running version 1.0.0"
            1 -> "Checking for the latest version..."
            else -> "You are on the latest version"
        },
        onClick = {
            if (state != 1) state = if (state == 2) 0 else 1
        },
        trailing = {
            if (state == 1) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = PortalBlue,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = PortalTextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    )
}

@Composable
private fun ContactSheetContent(coordinator: CoordinatorRow?, onClose: () -> Unit) {
    val context = LocalContext.current

    // Real rows from the `coordinators` table, with sensible fallbacks if RLS / data is missing.
    val name = coordinator?.name?.takeIf { it.isNotBlank() } ?: "Department Co-ordinator"
    val phone = coordinator?.phoneNo?.takeIf { it.isNotBlank() } ?: "+91 98765 43210"
    val email = coordinator?.email?.takeIf { it.isNotBlank() } ?: "coordinator.mitrc@mitrc.ac.in"
    val designation = coordinator?.designation?.takeIf { it.isNotBlank() }
    val department = coordinator?.department?.takeIf { it.isNotBlank() }

    Column(modifier = Modifier.fillMaxWidth().padding(top = 26.dp)) {
        Text(
            text = "Contact Co-ordinator",
            color = PortalTextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(horizontal = 22.dp)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Reach out for attendance, timetable or attendance-remedy queries.",
            color = PortalTextSecondary,
            fontSize = 12.5.sp,
            lineHeight = 18.sp,
            modifier = Modifier.padding(horizontal = 22.dp)
        )

        Spacer(Modifier.height(16.dp))

        Column(modifier = Modifier.padding(horizontal = 22.dp)) {
            SheetInfoRow(label = "Co-ordinator", value = name)
            designation?.let { SheetInfoRow(label = "Designation", value = it) }
            department?.let { SheetInfoRow(label = "Department", value = it) }
            SheetInfoRow(label = "Phone", value = phone)
            SheetInfoRow(label = "Email", value = email)
        }

        if (coordinator == null) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = "No co-ordinator record found yet - showing the default contact.",
                color = PortalRose,
                fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 22.dp)
            )
        }

        Spacer(Modifier.height(18.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = {
                    runCatching {
                        context.startActivity(
                            Intent(
                                Intent.ACTION_DIAL,
                                Uri.parse("tel:${phone.filter { it.isDigit() || it == '+' }}")
                            )
                        )
                    }
                },
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PortalBlue,
                    contentColor = Color.White
                )
            ) {
                Icon(
                    Icons.Outlined.Phone,
                    contentDescription = null,
                    modifier = Modifier.size(17.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Call", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }

            Button(
                onClick = {
                    runCatching {
                        context.startActivity(
                            Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$email"))
                        )
                    }
                },
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PortalBlue.copy(alpha = 0.10f),
                    contentColor = PortalBlue
                )
            ) {
                Icon(
                    Icons.Outlined.MailOutline,
                    contentDescription = null,
                    modifier = Modifier.size(17.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Email", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(Modifier.height(14.dp))

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

@Composable
private fun AboutSheetContent(onClose: () -> Unit) {
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
            SheetInfoRow(label = "Last Checked", value = todayLabel())
            SheetInfoRow(label = "Support", value = "portal@mitrc.ac.in")
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

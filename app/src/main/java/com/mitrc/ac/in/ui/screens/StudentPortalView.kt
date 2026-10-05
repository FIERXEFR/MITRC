package com.mitrc.ac.`in`.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Grade
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mitrc.ac.`in`.data.AttendanceSummaryView
import com.mitrc.ac.`in`.data.MarkView
import com.mitrc.ac.`in`.data.MyTimetableEntryView
import com.mitrc.ac.`in`.data.PortalRepository
import com.mitrc.ac.`in`.data.StudentDirectoryView
import com.mitrc.ac.`in`.data.StudentSubjectView
import com.mitrc.ac.`in`.ui.theme.ErrorRed
import com.mitrc.ac.`in`.ui.theme.Gold
import com.mitrc.ac.`in`.ui.theme.GoldLight
import com.mitrc.ac.`in`.ui.theme.Navy
import com.mitrc.ac.`in`.ui.theme.NavySoft
import com.mitrc.ac.`in`.ui.theme.Slate
import com.mitrc.ac.`in`.ui.theme.SuccessGreen
import com.mitrc.ac.`in`.ui.theme.SurfaceWhite
import com.mitrc.ac.`in`.ui.theme.TextPrimary
import com.mitrc.ac.`in`.ui.theme.TextSecondary

@Composable
fun StudentPortalView(userUid: String) {
    var loading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    var profile by remember { mutableStateOf<StudentDirectoryView?>(null) }
    var subjects by remember { mutableStateOf<List<StudentSubjectView>>(emptyList()) }
    var attendance by remember { mutableStateOf<List<AttendanceSummaryView>>(emptyList()) }
    var marks by remember { mutableStateOf<List<MarkView>>(emptyList()) }
    var timetable by remember { mutableStateOf<List<MyTimetableEntryView>>(emptyList()) }

    LaunchedEffect(userUid) {
        loading = true
        errorMsg = null

        val profileRes = PortalRepository.getStudentProfile(userUid)
        val classId = profileRes.getOrNull()?.classId ?: 0
        val subjectsRes = if (classId > 0) PortalRepository.getStudentSubjects(classId) else Result.success(emptyList())
        val attendanceRes = PortalRepository.getStudentAttendance(userUid)
        val marksRes = PortalRepository.getStudentMarks(userUid)
        val timetableRes = PortalRepository.getMyTimetable()

        profile = profileRes.getOrNull()
        subjects = subjectsRes.getOrDefault(emptyList())
        attendance = attendanceRes.getOrDefault(emptyList())
        marks = marksRes.getOrDefault(emptyList())
        timetable = timetableRes.getOrDefault(emptyList())

        if (profileRes.isFailure) {
            errorMsg = profileRes.exceptionOrNull()?.message ?: "Failed to load student profile."
        }

        loading = false
    }

    if (loading) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Navy)
        }
        return
    }

    if (errorMsg != null) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFEE2E2)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.ErrorOutline, contentDescription = null, tint = ErrorRed)
                Spacer(Modifier.width(12.dp))
                Text(text = errorMsg.orEmpty(), color = ErrorRed, fontSize = 14.sp)
            }
        }
        return
    }

    val p = profile
    if (p == null) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "No Student Profile Found",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = TextPrimary
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Your account is not linked to a student record. Contact admin.",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            }
        }
        return
    }

    Column {
        // Profile Card
        StudentProfileCard(p)

        Spacer(Modifier.height(20.dp))

        // Enrolled Subjects
        SectionHeader("MY ACADEMIC SUBJECTS")
        Spacer(Modifier.height(10.dp))
        if (subjects.isEmpty()) {
            EmptyCard("No enrolled subjects found.")
        } else {
            subjects.forEach { sub ->
                StudentSubjectCard(sub)
                Spacer(Modifier.height(8.dp))
            }
        }

        Spacer(Modifier.height(20.dp))

        // Attendance Percentage
        SectionHeader("ATTENDANCE SUMMARY")
        Spacer(Modifier.height(10.dp))
        if (attendance.isEmpty()) {
            EmptyCard("No attendance records recorded yet.")
        } else {
            attendance.forEach { att ->
                AttendanceItemCard(att)
                Spacer(Modifier.height(8.dp))
            }
        }

        Spacer(Modifier.height(20.dp))

        // Marks Summary
        SectionHeader("MARKS & EVALUATIONS")
        Spacer(Modifier.height(10.dp))
        if (marks.isEmpty()) {
            EmptyCard("No marks published yet.")
        } else {
            val groupedMarks = marks.groupBy { it.subjectCode }
            groupedMarks.forEach { (code, markList) ->
                SubjectMarksCard(code, markList)
                Spacer(Modifier.height(10.dp))
            }
        }

        Spacer(Modifier.height(20.dp))

        // Timetable
        SectionHeader("LIVE TIMETABLE")
        Spacer(Modifier.height(10.dp))
        StudentTimetableGrid(timetable, hasGroupAssigned = !p.groupName.isNull_or_blank())
    }
}

private fun String?.isNull_or_blank(): Boolean = this == null || this.trim().isEmpty()

@Composable
private fun StudentProfileCard(profile: StudentDirectoryView) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = Navy.copy(alpha = 0.1f),
                    modifier = Modifier.size(50.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Person, contentDescription = null, tint = Navy, modifier = Modifier.size(28.dp))
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        text = profile.name ?: "Student Profile",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Serial: ${profile.serialNo ?: "N/A"}",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            val classLabel = buildString {
                append(profile.course ?: "B.Tech")
                if (!profile.branch.isNull_or_blank()) append(" - ").append(profile.branch)
                if (profile.semester != null) append(" | Sem ").append(profile.semester)
                if (!profile.section.isNull_or_blank()) append(" - Sec ").append(profile.section)
                if (!profile.academicYear.isNull_or_blank()) append(" (").append(profile.academicYear).append(")")
            }

            InfoRow(label = "Academic Cohort", value = classLabel)

            val groupText = if (!profile.groupName.isNull_or_blank()) {
                profile.groupName.orEmpty()
            } else {
                "Lab group not assigned yet. Contact your coordinator."
            }
            InfoRow(
                label = "Lab Group",
                value = groupText,
                isHighlight = profile.groupName.isNull_or_blank()
            )

            if (!profile.fatherName.isNull_or_blank()) {
                InfoRow(label = "Father's Name", value = profile.fatherName.orEmpty())
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String, isHighlight: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 12.sp, color = TextSecondary)
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isHighlight) Gold else TextPrimary
        )
    }
}

@Composable
private fun StudentSubjectCard(subject: StudentSubjectView) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (subject.subjectKind == "lab") Gold.copy(alpha = 0.15f) else Navy.copy(alpha = 0.1f))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = subject.subjectCode,
                    color = if (subject.subjectKind == "lab") Gold else Navy,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = subject.subjectName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Text(
                    text = subject.subjectKind.uppercase(),
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun AttendanceItemCard(item: AttendanceSummaryView) {
    val pct = item.percentage
    val isGood = pct >= 75.0

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${item.subjectName} (${item.subjectCode})",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "${item.percentage}%",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isGood) SuccessGreen else ErrorRed
                )
            }
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { (pct / 100f).toFloat().coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = if (isGood) SuccessGreen else ErrorRed,
                trackColor = Slate
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Attended ${item.attended} of ${item.total} classes",
                fontSize = 11.sp,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun SubjectMarksCard(subjectCode: String, markList: List<MarkView>) {
    val first = markList.firstOrNull()
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "${first?.subjectName ?: subjectCode} ($subjectCode)",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Navy
            )
            Spacer(Modifier.height(10.dp))
            markList.forEach { m ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${m.kind} ${m.number}",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    Text(
                        text = if (m.marks != null) "${m.marks} / ${m.maxMarks ?: 10.0}" else "N/A",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun StudentTimetableGrid(
    entries: List<MyTimetableEntryView>,
    hasGroupAssigned: Boolean
) {
    if (!hasGroupAssigned) {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Gold.copy(alpha = 0.12f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp)
        ) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Info, contentDescription = null, tint = Gold)
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "Lab group not assigned yet. Contact your coordinator.",
                    fontSize = 12.sp,
                    color = TextPrimary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }

    if (entries.isEmpty()) {
        EmptyCard("No published timetable available.")
        return
    }

    val daysMap = mapOf(
        1 to "Monday",
        2 to "Tuesday",
        3 to "Wednesday",
        4 to "Thursday",
        5 to "Friday",
        6 to "Saturday"
    )

    val groupedByDay = entries.groupBy { it.dayOfWeek }

    Column {
        (1..6).forEach { dayNum ->
            val dayEntries = groupedByDay[dayNum]
            if (!dayEntries.isNullOrEmpty()) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = daysMap[dayNum] ?: "Day $dayNum",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Navy
                        )
                        Spacer(Modifier.height(8.dp))

                        val sorted = dayEntries.sortedBy { it.startPeriod }
                        sorted.forEach { entry ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Slate)
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "Period ${entry.startPeriod}${if (entry.endPeriod > entry.startPeriod) "-${entry.endPeriod}" else ""}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Navy
                                        )
                                        if (!entry.groupName.isNull_or_blank()) {
                                            Spacer(Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(Gold.copy(alpha = 0.2f))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = entry.groupName.orEmpty(),
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Gold
                                                )
                                            }
                                        }
                                    }
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = "${entry.subjectName} (${entry.subjectCode})",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary
                                    )
                                    if (!entry.teacherName.isNull_or_blank()) {
                                        Text(
                                            text = "Teacher: ${entry.teacherName}",
                                            fontSize = 11.sp,
                                            color = TextSecondary
                                        )
                                    }
                                }
                                if (!entry.room.isNull_or_blank()) {
                                    Text(
                                        text = entry.room.orEmpty(),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextSecondary
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

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = TextSecondary,
        letterSpacing = 1.5.sp
    )
}

@Composable
private fun EmptyCard(message: String) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier.padding(18.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(text = message, fontSize = 13.sp, color = TextSecondary)
        }
    }
}

package com.mitrc.ac.`in`.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.mitrc.ac.`in`.R
import com.mitrc.ac.`in`.data.*
import com.mitrc.ac.`in`.ui.theme.DarkTextMuted
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
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone


val PortalBackground = Slate
val PortalCard = SurfaceWhite
val PortalCardAlt = Slate
val PortalStroke = DividerSoft
val PortalTextPrimary = TextPrimary
val PortalTextSecondary = TextSecondary
val PortalTrack = DividerSoft

val PortalBlue = Navy
val PortalPurple = NavySoft
val PortalAmber = Gold
val PortalCyan = GoldDeep
val PortalRose = ErrorRed
val PortalGreen = SuccessGreen

val PortalSkeletonBase = DividerSoft
val PortalSkeletonShine = DarkTextMuted

internal fun formatPtmSchedule(raw: String?): String? {
    val value = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    val instant = runCatching { OffsetDateTime.parse(value).toInstant() }
        .recoverCatching { Instant.parse(value) }
        .getOrNull() ?: return value
    return instant
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("EEE, dd MMM yyyy â€¢ hh:mm a", Locale.getDefault()))
}

internal val LocalShimmerProgress = staticCompositionLocalOf<State<Float>?> { null }

@Composable
private fun rememberShimmerProgress(): State<Float> {
    val transition = rememberInfiniteTransition(label = "shimmerTransition")
    return transition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerProgress"
    )
}

@Composable
fun Modifier.shimmerEffect(): Modifier {
    val sharedProgress = LocalShimmerProgress.current
    val progress = sharedProgress ?: rememberShimmerProgress()

    return this.drawBehind {
        val sweep = progress.value
        val width = size.width.coerceAtLeast(1f)
        val band = width * 0.55f
        val centre = width * sweep
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(PortalSkeletonBase, PortalSkeletonShine, PortalSkeletonBase),
                start = Offset(centre - band, 0f),
                end = Offset(centre + band, size.height.coerceAtLeast(1f))
            )
        )
    }
}

enum class StudentTab(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Outlined.Home),
    ATTENDANCE("Academics", Icons.Outlined.Checklist),
    EVENTS("Events", Icons.Outlined.Campaign),
    SETTINGS("Settings", Icons.Outlined.Settings);

    companion object {
        fun fromName(name: String?): StudentTab =
            entries.firstOrNull { it.name == name } ?: HOME
    }
}

enum class ScheduleMode(val label: String) {
    ATTENDANCE("Attendance"),
    TIMETABLE("Timetable"),
    NOTES("Notes"),
    EXAMS("Results");

    companion object {
        fun fromName(name: String?): ScheduleMode =
            entries.firstOrNull { it.name == name } ?: ATTENDANCE
    }
}

data class PortalNote(
    val id: Long,
    val title: String,
    val body: String,
    val createdAt: Long = System.currentTimeMillis()
)

data class PortalPdfNote(
    val id: Int,
    val classSubjectId: Int = 0,
    val subjectName: String,
    val subjectCode: String,
    val title: String,
    val description: String?,
    val category: String,
    val driveUrl: String,
    val teacherName: String,
    val createdAt: String
)

data class FacultyNotesGroup(
    val subjectName: String,
    val subjectCode: String,
    val teacherName: String,
    val teacherEmail: String?,
    val chapters: List<PortalPdfNote>
) {
    val key: String get() = "$subjectCode|$subjectName|$teacherName"
}

fun buildFacultyNoteGroups(
    subjects: List<ClassSubjectView>,
    notes: List<PortalPdfNote>
): List<FacultyNotesGroup> {
    val used = mutableSetOf<Int>()
    val groups = mutableListOf<FacultyNotesGroup>()

    subjects.forEach { subject ->
        val chapters = notes
            .filter { note ->
                note.id !in used && (
                    (note.classSubjectId != 0 && note.classSubjectId == subject.classSubjectId) ||
                        note.subjectName.equals(subject.subjectName, ignoreCase = true)
                    )
            }
            .sortedChapters()

        used += chapters.map { it.id }
        groups += FacultyNotesGroup(
            subjectName = subject.subjectName,
            subjectCode = subject.subjectCode,
            teacherName = subject.teacherName,
            teacherEmail = subject.teacherEmail,
            chapters = chapters
        )
    }

    notes.filter { it.id !in used }
        .groupBy { "${it.subjectCode}|${it.subjectName}|${it.teacherName}" }
        .values
        .forEach { leftovers ->
            val first = leftovers.first()
            groups += FacultyNotesGroup(
                subjectName = first.subjectName,
                subjectCode = first.subjectCode,
                teacherName = first.teacherName,
                teacherEmail = null,
                chapters = leftovers.sortedChapters()
            )
        }

    return groups
}

private fun List<PortalPdfNote>.sortedChapters(): List<PortalPdfNote> =
    sortedWith(compareBy({ !it.title.startsWith("Unit ", ignoreCase = true) }, { it.title }))


fun List<NoteRow>.toPortalPdfNotes(
    subjects: List<ClassSubjectView>
): List<PortalPdfNote> {
    if (isEmpty()) return emptyList()

    val enrolledBySubjectId = subjects.associateBy { it.classSubjectId }

    return sortedByDescending { it.createdAt.orEmpty() }
        .mapNotNull { row ->
            val subject = enrolledBySubjectId[row.classSubjectId]
            if (!row.isVisible) return@mapNotNull null
            if (row.title.isBlank() || row.driveUrl.isBlank()) return@mapNotNull null

            PortalPdfNote(
                id = row.id,
                classSubjectId = row.classSubjectId,
                subjectName = subject?.subjectName ?: "Subject #${row.classSubjectId}",
                subjectCode = subject?.subjectCode ?: "NOTES",
                title = row.title,
                description = row.description,
                category = row.category,
                driveUrl = row.driveUrl,
                teacherName = subject?.teacherName?.ifBlank { "Faculty" } ?: "Faculty",
                createdAt = formatNoteDate(row.createdAt)
            )
        }
}

@JvmName("vNoteViewToPortalPdfNotes")
fun List<com.mitrc.ac.`in`.data.VNoteView>.toPortalPdfNotes(
    subjects: List<ClassSubjectView>
): List<PortalPdfNote> {
    if (isEmpty()) return emptyList()

    val enrolledBySubjectId = subjects.associateBy { it.classSubjectId }

    return sortedByDescending { it.createdAt.orEmpty() }
        .mapNotNull { row ->
            val subject = enrolledBySubjectId[row.classSubjectId]
            if (!row.isVisible) return@mapNotNull null
            if (row.title.isBlank() || row.driveUrl.isBlank()) return@mapNotNull null

            PortalPdfNote(
                id = row.id,
                classSubjectId = row.classSubjectId,
                subjectName = row.subjectName.ifBlank { subject?.subjectName ?: "Subject #${row.classSubjectId}" },
                subjectCode = row.subjectCode.ifBlank { subject?.subjectCode ?: "NOTES" },
                title = row.title,
                description = row.description,
                category = row.category,
                driveUrl = row.driveUrl,
                teacherName = row.teacherName?.ifBlank { subject?.teacherName ?: "Faculty" } ?: "Faculty",
                createdAt = formatNoteDate(row.createdAt)
            )
        }
}

private fun formatNoteDate(raw: String?): String {
    val iso = raw?.trim().orEmpty()
    if (iso.isEmpty()) return ""
    return try {
        val parsed = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
            .apply { timeZone = TimeZone.getTimeZone("UTC") }
            .parse(iso.substringBefore("+").substringBefore("Z"))
            ?: return iso.take(10)
        SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(parsed)
    } catch (e: Exception) {
        iso.take(10)
    }
}


data class CollegeEvent(
    val id: Int,
    val title: String,
    val category: String,
    val whenLabel: String,
    val venue: String,
    val image: Any
)

fun sampleCollegeEvents(): List<CollegeEvent> = listOf(
    CollegeEvent(
        id = 1,
        title = "Techspark \u201926 \u2014 Annual Tech Exhibition",
        category = "TECH FEST",
        whenLabel = "12 \u2013 19 OCT 2026 \u00b7 9:15 AM",
        venue = "Central Lawn, MITRC",
        image = R.drawable.techspark26
    ),
    CollegeEvent(
        id = 2,
        title = "Arambh \u2014 Annual Campus Fest",
        category = "CULTURAL",
        whenLabel = "13 \u2013 15 Feb 2026 \u00b7 05:00 PM",
        venue = "Open Air Amphitheatre",
        image = "https://mitrc.ac.in/images/campus/arambh-fest.jpg"
    ),
    CollegeEvent(
        id = 3,
        title = "Smart India Hackathon \u2014 MITRC Nodal Centre",
        category = "HACKATHON",
        whenLabel = "25 \u2013 26 Aug 2022",
        venue = "Innovation Hall, Block C",
        image = "https://mitrc.ac.in/images/hackathons/sih-2022.jpg"
    ),
    CollegeEvent(
        id = 4,
        title = "Manipal Hackathon \u2014 Inter-College Edition",
        category = "HACKATHON",
        whenLabel = "15 \u2013 16 Oct 2022",
        venue = "Innovation Lab, Block A",
        image = "https://mitrc.ac.in/images/hackathons/manipal-hackathon.jpg"
    ),
    CollegeEvent(
        id = 5,
        title = "Toycathon \u2014 Toy Innovation Challenge",
        category = "INNOVATION",
        whenLabel = "10 \u2013 12 Jun 2021",
        venue = "Design Studio, Block B",
        image = "https://mitrc.ac.in/images/hackathons/toycathon.png"
    ),
    CollegeEvent(
        id = 6,
        title = "Samadhan \u2014 National Solution Challenge",
        category = "HACKATHON",
        whenLabel = "21 \u2013 22 Feb 2020",
        venue = "Seminar Hall 2, MITRC",
        image = "https://mitrc.ac.in/images/hackathons/samadhan.jpg"
    ),
    CollegeEvent(
        id = 7,
        title = "CVA \u2014 National Level Hackathon",
        category = "HACKATHON",
        whenLabel = "08 \u2013 09 Mar 2019",
        venue = "Computer Science Block",
        image = "https://mitrc.ac.in/images/hackathons/cva-2019.jpg"
    )
)


enum class NotificationTone { INFO, ALERT, SUCCESS }

data class PortalNotification(
    val id: Int,
    val title: String,
    val body: String,
    val time: String,
    val tone: NotificationTone,
    val image: Any? = null,
    val unread: Boolean = true
)

fun sampleNotifications(): List<PortalNotification> = listOf(
    PortalNotification(
        id = 1,
        title = "Attendance below 75%",
        body = "Your Computer Architecture attendance is at 71%. Attend the next 3 classes to cross the university requirement.",
        time = "10 min ago",
        tone = NotificationTone.ALERT
    ),
    PortalNotification(
        id = 2,
        title = "Techspark \u201926 registrations open",
        body = "Team registrations for the annual tech exhibition are now live. Last date to register is 14 Nov.",
        time = "1 hr ago",
        tone = NotificationTone.INFO,
        image = R.drawable.techspark26
    ),
    PortalNotification(
        id = 3,
        title = "Marks published \u2014 UT-2",
        body = "Unit Test 2 marks for Web Application Development are now visible in your analytics tab.",
        time = "3 hrs ago",
        tone = NotificationTone.SUCCESS
    ),
    PortalNotification(
        id = 4,
        title = "Arambh \u201926 \u2014 campus fest dates announced",
        body = "Arambh returns on 13 \u2013 15 Feb with 40+ cultural events across the amphitheatre and main lawn.",
        time = "Yesterday",
        tone = NotificationTone.INFO,
        image = "https://mitrc.ac.in/images/campus/arambh-fest.jpg"
    ),
    PortalNotification(
        id = 5,
        title = "Smart India Hackathon \u2014 MITRC nodal centre",
        body = "MITRC will host the SIH internal round in Innovation Hall, Block C. Team registrations close 30 Oct.",
        time = "2 days ago",
        tone = NotificationTone.INFO,
        image = "https://mitrc.ac.in/images/hackathons/sih-2022.jpg"
    ),
    PortalNotification(
        id = 6,
        title = "Library book due",
        body = "\"Operating Systems Concepts\" is due for return on 30 Sep.",
        time = "2 days ago",
        tone = NotificationTone.ALERT
    ),
    PortalNotification(
        id = 7,
        title = "Welcome to the Student Portal",
        body = "Your profile has been linked. Use the tabs below to track attendance, timetable and events.",
        time = "1 week ago",
        tone = NotificationTone.SUCCESS
    )
)


data class QuickAction(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val target: StudentTab,
    val accent: Color,
    val mode: ScheduleMode? = null
)

fun sampleQuickActions(): List<QuickAction> = listOf(
    QuickAction(
        title = "View Attendance",
        subtitle = "Subject-wise record",
        icon = Icons.Outlined.Checklist,
        target = StudentTab.ATTENDANCE,
        accent = PortalBlue,
        mode = ScheduleMode.ATTENDANCE
    ),
    QuickAction(
        title = "My Timetable",
        subtitle = "Plan your week",
        icon = Icons.Outlined.CalendarMonth,
        target = StudentTab.ATTENDANCE,
        accent = PortalPurple,
        mode = ScheduleMode.TIMETABLE
    ),
    QuickAction(
        title = "Campus Events",
        subtitle = "What's happening",
        icon = Icons.Outlined.Campaign,
        target = StudentTab.EVENTS,
        accent = PortalCyan
    ),
    QuickAction(
        title = "My Profile",
        subtitle = "Account & sign out",
        icon = Icons.Outlined.Person,
        target = StudentTab.SETTINGS,
        accent = PortalGreen
    )
)


fun todayLabel(): String =
    SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault()).format(Date())

fun firstNameOf(fullName: String?): String {
    val trimmed = fullName?.trim().orEmpty()
    if (trimmed.isEmpty()) return "Student"
    return trimmed.split(Regex("\\s+")).first()
}

fun portalDisplayName(profile: StudentProfileView?): String {
    profile?.name?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
    return "Student"
}

fun currentDayOfWeek(): Int = Calendar.getInstance().get(Calendar.DAY_OF_WEEK).let {
    if (it == Calendar.SUNDAY) 7 else it - 1
}

val streakDays: List<Pair<String, Boolean>>
    get() = listOf(
        "Mon" to true, "Tue" to true, "Wed" to true,
        "Thu" to true, "Fri" to true, "Sat" to true, "Sun" to false
    )

val weekBars: List<Pair<String, Int>>
    get() = listOf("Mon" to 7, "Tue" to 6, "Wed" to 6, "Thu" to 5, "Fri" to 6, "Sat" to 4, "Sun" to 0)

data class AttendanceStreak(val current: Int, val best: Int)

fun attendanceStreak(days: List<DailyAttendance>): AttendanceStreak {
    val states = days
        .mapNotNull { day ->
            val date = runCatching { LocalDate.parse(day.date) }.getOrNull() ?: return@mapNotNull null
            day.attendedDay?.let { date to it }
        }
        .sortedBy { it.first }

    var best = 0
    var run = 0
    states.forEach { (_, attended) ->
        if (attended) {
            run += 1
            if (run > best) best = run
        } else {
            run = 0
        }
    }

    return AttendanceStreak(current = run, best = best)
}

fun samplePersonalNotes(): List<PortalNote> = listOf(
    PortalNote(
        id = 9001,
        title = "OS unit 2 - CPU scheduling",
        body = "Revise RR time quantum edge cases before the mid sem."
    ),
    PortalNote(
        id = 9002,
        title = "DBMS assignment",
        body = "Normalise the given relation up to BCNF and draw the dependency diagram."
    ),
    PortalNote(
        id = 9003,
        title = "Lab record",
        body = "Submit the CN lab record to Mr. Yadav before Friday."
    )
)


@Composable
fun EventCardSkeleton() {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = PortalCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, PortalStroke),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .shimmerEffect()
            )
            Column(modifier = Modifier.padding(16.dp)) {
                Box(
                    modifier = Modifier
                        .size(width = 70.dp, height = 20.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .shimmerEffect()
                )
                Spacer(Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(20.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .shimmerEffect()
                )
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.55f)
                        .height(14.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .shimmerEffect()
                )
            }
        }
    }
}

@Composable
fun NotificationRowSkeleton() {
    ElevatedCard(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = PortalCard),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .shimmerEffect()
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .height(16.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .shimmerEffect()
                    )
                    Box(
                        modifier = Modifier
                            .width(45.dp)
                            .height(12.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .shimmerEffect()
                    )
                }
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.95f)
                        .height(13.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .shimmerEffect()
                )
                Spacer(Modifier.height(5.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.75f)
                        .height(13.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .shimmerEffect()
                )
            }
        }
    }
}

@Composable
fun HomeTabSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            repeat(3) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = PortalCard,
                    border = androidx.compose.foundation.BorderStroke(1.dp, PortalStroke),
                    modifier = Modifier
                        .weight(1f)
                        .height(72.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(
                            modifier = Modifier
                                .width(35.dp)
                                .height(12.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .shimmerEffect()
                        )
                        Box(
                            modifier = Modifier
                                .width(24.dp)
                                .height(22.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .shimmerEffect()
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(14.dp))

        Surface(
            shape = RoundedCornerShape(20.dp),
            color = PortalCard,
            border = androidx.compose.foundation.BorderStroke(1.dp, PortalStroke),
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .height(16.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .shimmerEffect()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    repeat(7) {
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .shimmerEffect()
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(14.dp))

        Surface(
            shape = RoundedCornerShape(20.dp),
            color = PortalCard,
            border = androidx.compose.foundation.BorderStroke(1.dp, PortalStroke),
            modifier = Modifier
                .fillMaxWidth()
                .height(125.dp)
        ) {
            Row(
                modifier = Modifier.padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .shimmerEffect()
                )
                Spacer(Modifier.width(16.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .height(18.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .shimmerEffect()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .height(14.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .shimmerEffect()
                    )
                }
            }
        }
        Spacer(Modifier.height(20.dp))

        EventCardSkeleton()
    }
}

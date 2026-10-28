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
import androidx.compose.runtime.getValue
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

// ---------------------------------------------------------------------------------------------
// Portal palette.
//
// Every value here is a straight reference to the existing MITRC theme (see ui/theme/Color.kt),
// so the student portal renders in exactly the same Navy + Gold language as the onboarding and
// login screens. Nothing here introduces a new colour.
// ---------------------------------------------------------------------------------------------

val PortalBackground = Slate
val PortalCard = SurfaceWhite
val PortalCardAlt = Slate
val PortalStroke = DividerSoft
val PortalTextPrimary = TextPrimary
val PortalTextSecondary = TextSecondary
val PortalTrack = DividerSoft

/** Primary accent - matches the login chip / button navy. */
val PortalBlue = Navy
/** Secondary accent. */
val PortalPurple = NavySoft
/** Caution accent - gold is only ever used behind icons, borders or on navy, never as body text. */
val PortalAmber = Gold
/** Highlight accent for category chips. */
val PortalCyan = GoldDeep
val PortalRose = ErrorRed
val PortalGreen = SuccessGreen

/**
 * Skeleton fill - a mid grey so the block is actually visible against a white portal card.
 * (The old value was `Slate`, i.e. white-on-white in light theme.)
 */
val PortalSkeletonBase = DividerSoft
/** The darker grey band that sweeps across [PortalSkeletonBase] during a shimmer. */
val PortalSkeletonShine = DarkTextMuted

/**
 * Formats a `students.ptm_schedule` timestamp for display. PostgREST returns ISO-8601 with an
 * offset (`2026-10-22T10:30:00+00:00`) or a `Z`; anything unparsable is shown verbatim so the
 * scheduled time is never silently hidden.
 */
internal fun formatPtmSchedule(raw: String?): String? {
    val value = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    val instant = runCatching { OffsetDateTime.parse(value).toInstant() }
        .recoverCatching { Instant.parse(value) }
        .getOrNull() ?: return value
    return instant
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("EEE, dd MMM yyyy â€¢ hh:mm a", Locale.getDefault()))
}

/**
 * Reusable shimmer modifier for skeleton previews across all cards & screens.
 *
 * Draws a mid-grey block with a darker slate band travelling across it, left to right. The band
 * is deliberately darker than the base rather than lighter - a light highlight only shows up on a
 * dark surface, which is why the previous near-white pulse was invisible in light theme.
 */
@Composable
fun Modifier.shimmerEffect(): Modifier {
    val transition = rememberInfiniteTransition(label = "shimmerTransition")
    val progress by transition.animateFloat(
        // -1f .. 2f puts the band fully off one edge at each end, so it never pops into view.
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerProgress"
    )
    // Deliberately read here, during composition: the frame that moves the band then rebuilds
    // this modifier and invalidates the draw pass. Reading it only inside `drawBehind` would
    // happen after the draw pass was already considered clean, so the band would never move.
    val sweep = progress

    return this.drawBehind {
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

/** The four fixed destinations in the bottom navigation bar. */
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

/** Attendance, weekly timetable, study notes and exam results share one navigation entry. */
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

/**
 * One student note.
 */
data class PortalNote(
    val id: Long,
    val title: String,
    val body: String,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Teacher-published PDF note / study resource.
 */
data class PortalPdfNote(
    val id: Int,
    val classSubjectId: Int = 0,
    val subjectName: String,
    val subjectCode: String,
    val title: String,
    val description: String?,
    val category: String, // 'notes', 'assignment', 'question_paper', 'syllabus', 'lab_manual', 'other'
    val driveUrl: String,
    val teacherName: String,
    val createdAt: String
)

/**
 * One card in the Notes segment: the faculty member, the subject they teach this class and the
 * chapter-wise notes published for it.
 */
data class FacultyNotesGroup(
    val subjectName: String,
    val subjectCode: String,
    val teacherName: String,
    val teacherEmail: String?,
    val chapters: List<PortalPdfNote>
) {
    /** Stable identity for expand/collapse state, independent of list order. */
    val key: String get() = "$subjectCode|$subjectName|$teacherName"
}

/**
 * Groups published notes into one card per enrolled subject, in the student's enrolment order.
 * Anything that cannot be matched to an enrolled subject still gets its own card so notes never
 * silently disappear.
 */
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

/** Units first (Unit 1, Unit 2, ...), then assignments, papers and other material. */
private fun List<PortalPdfNote>.sortedChapters(): List<PortalPdfNote> =
    sortedWith(compareBy({ !it.title.startsWith("Unit ", ignoreCase = true) }, { it.title }))

// ---------------------------------------------------------------------------------------------
// notes table -> card model
// ---------------------------------------------------------------------------------------------

/**
 * Turns raw `notes` rows into the card model the Notes segment renders.
 *
 * Only rows whose `class_subject_id` belongs to one of this student's enrolled subjects survive,
 * so nobody sees material for a subject they do not attend. Subject name / code come from the
 * enrolment row and the teacher's name from `v_subject_teachers`; a row that cannot be resolved
 * to a subject is dropped rather than rendered with a blank header. Newest first, which matches
 * the `notes_class_subject_id_created_at_idx` index the query is planned against.
 */
fun List<NoteRow>.toPortalPdfNotes(
    subjects: List<ClassSubjectView>
): List<PortalPdfNote> {
    if (isEmpty()) return emptyList()

    val enrolledBySubjectId = subjects.associateBy { it.classSubjectId }

    return sortedByDescending { it.createdAt.orEmpty() } // ISO-8601 sorts correctly as plain text
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

/** `2026-09-20T11:32:00+00:00` -> `20 Sep 2026`; anything unparseable is passed through. */
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

// ---------------------------------------------------------------------------------------------
// College events - the auto-advancing carousel on the Home tab.
// ---------------------------------------------------------------------------------------------

data class CollegeEvent(
    val id: Int,
    val title: String,
    val category: String,
    val whenLabel: String,
    val venue: String,
    val image: Any
)

/**
 * Event feed shared by the Home carousel and the Events tab, so both surfaces show the same
 * cards. The posters are the college's own event artwork.
 */
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

// ---------------------------------------------------------------------------------------------
// Notifications - opened from the bell in the header. Some carry an image, some do not.
// ---------------------------------------------------------------------------------------------

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

// ---------------------------------------------------------------------------------------------
// Quick access tiles - each one is wired to a real tab, so they actually navigate.
// ---------------------------------------------------------------------------------------------

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

// ---------------------------------------------------------------------------------------------
// Small presentation helpers
// ---------------------------------------------------------------------------------------------

fun todayLabel(): String =
    SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault()).format(Date())

fun firstNameOf(fullName: String?): String {
    val trimmed = fullName?.trim().orEmpty()
    if (trimmed.isEmpty()) return "Student"
    return trimmed.split(Regex("\\s+")).first()
}

/**
 * Name shown on the portal header and avatar.
 *
 * The `students` row is the only authority: the app must never greet a student with the local
 * part of their email address. [StudentPortalShell] resolves the database name into
 * `StudentPortalData.displayName`, so this stays a last-resort label.
 */
fun portalDisplayName(profile: StudentProfileView?): String {
    profile?.name?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
    return "Student"
}

/** 1 = Monday ... 7 = Sunday, matching `MyTimetableEntryView.dayOfWeek`. */
fun currentDayOfWeek(): Int = Calendar.getInstance().get(Calendar.DAY_OF_WEEK).let {
    if (it == Calendar.SUNDAY) 7 else it - 1
}

/** Deterministic stub data behind the streak card. */
val streakDays: List<Pair<String, Boolean>>
    get() = listOf(
        "Mon" to true, "Tue" to true, "Wed" to true,
        "Thu" to true, "Fri" to true, "Sat" to true, "Sun" to false
    )

/** Attendance for each of the last 7 days, used by the small bar chart. */
val weekBars: List<Pair<String, Int>>
    get() = listOf("Mon" to 7, "Tue" to 6, "Wed" to 6, "Thu" to 5, "Fri" to 6, "Sat" to 4, "Sun" to 0)

/** Current and best consecutive-present runs shown on the attendance streak card. */
data class AttendanceStreak(val current: Int, val best: Int)

/**
 * Counts consecutive attended days over the days that actually had a marked period. Days with no
 * records (weekends, holidays, days faculty have not marked yet) neither extend nor break a run.
 */
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

    // `run` after the loop is the streak ending at the most recently marked day.
    return AttendanceStreak(current = run, best = best)
}

/** Placeholder entries for the notebook's "Saved notes" list. */
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

// ---------------------------------------------------------------------------------------------
// Skeleton Previews for Cards across Home, Events & Notifications
// ---------------------------------------------------------------------------------------------

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
        // Stat row skeleton
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

        // Streak card skeleton
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

        // Overall attendance card skeleton
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

        // Events carousel skeleton
        EventCardSkeleton()
    }
}

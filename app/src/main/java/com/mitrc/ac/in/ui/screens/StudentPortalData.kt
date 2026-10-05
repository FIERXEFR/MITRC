package com.mitrc.ac.`in`.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.mitrc.ac.`in`.data.NoteRow
import com.mitrc.ac.`in`.data.StudentSubjectView
import com.mitrc.ac.`in`.data.SubjectTeacherView
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
 * Reusable shimmer modifier for skeleton previews across all cards & screens.
 */
@Composable
fun Modifier.shimmerEffect(): Modifier {
    val transition = rememberInfiniteTransition(label = "shimmerTransition")
    val alpha by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.70f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmerAlpha"
    )
    return this.background(PortalCardAlt.copy(alpha = alpha))
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

/** Attendance, the weekly timetable and the notebook share one navigation entry. */
enum class ScheduleMode(val label: String) {
    ATTENDANCE("Attendance"),
    TIMETABLE("Timetable"),
    NOTES("Notes");

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
    subjects: List<StudentSubjectView>,
    subjectTeachers: List<SubjectTeacherView>
): List<PortalPdfNote> {
    if (isEmpty()) return emptyList()

    val enrolledBySubjectId = subjects.associateBy { it.classSubjectId }
    // One teacher handles several subjects, so index by teacher id rather than subject id.
    val teacherNameById = subjectTeachers
        .filter { it.teacherId > 0 && it.teacherName.isNotBlank() }
        .associate { it.teacherId to it.teacherName.trim() }

    return sortedByDescending { it.createdAt.orEmpty() } // ISO-8601 sorts correctly as plain text
        .mapNotNull { row ->
            val subject = enrolledBySubjectId[row.classSubjectId] ?: return@mapNotNull null
            if (!row.isVisible) return@mapNotNull null
            if (row.title.isBlank() || row.driveUrl.isBlank()) return@mapNotNull null

            PortalPdfNote(
                id = row.id,
                classSubjectId = row.classSubjectId,
                subjectName = subject.subjectName,
                subjectCode = subject.subjectCode,
                title = row.title,
                description = row.description,
                category = row.category,
                driveUrl = row.driveUrl,
                teacherName = teacherNameById[row.teacherId] ?: "Faculty",
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
    val image: String
)

/**
 * Placeholder event feed. `picsum.photos` seeds return a stable image per seed, so the carousel
 * looks identical on every launch instead of reshuffling.
 */
fun sampleCollegeEvents(): List<CollegeEvent> = listOf(
    CollegeEvent(
        id = 1,
        title = "TechFest 2026 \u2014 National Level Hackathon",
        category = "TECH",
        whenLabel = "28 Sep \u00b7 09:00 AM",
        venue = "Innovation Hall, Block C",
        image = "https://picsum.photos/seed/mitrc-techfest/900/600"
    ),
    CollegeEvent(
        id = 2,
        title = "Annual Cultural Night \u2014 Rhythms of Rajasthan",
        category = "CULTURAL",
        whenLabel = "02 Oct \u00b7 06:30 PM",
        venue = "Open Air Amphitheatre",
        image = "https://picsum.photos/seed/mitrc-cultural/900/600"
    ),
    CollegeEvent(
        id = 3,
        title = "Campus Placement Drive \u2014 TCS & Infosys",
        category = "PLACEMENT",
        whenLabel = "05 Oct \u00b7 08:00 AM",
        venue = "Training & Placement Cell",
        image = "https://picsum.photos/seed/mitrc-placement/900/600"
    ),
    CollegeEvent(
        id = 4,
        title = "International Yoga & Wellness Workshop",
        category = "WELLNESS",
        whenLabel = "09 Oct \u00b7 07:00 AM",
        venue = "Sports Complex",
        image = "https://picsum.photos/seed/mitrc-yoga/900/600"
    ),
    CollegeEvent(
        id = 5,
        title = "Robotics League \u2014 Inter-College Finals",
        category = "TECH",
        whenLabel = "12 Oct \u00b7 10:00 AM",
        venue = "Robotics Lab, Block A",
        image = "https://picsum.photos/seed/mitrc-robotics/900/600"
    ),
    CollegeEvent(
        id = 6,
        title = "Industry Guest Lecture on AI & Data Science",
        category = "SEMINAR",
        whenLabel = "16 Oct \u00b7 11:00 AM",
        venue = "Seminar Hall 2",
        image = "https://picsum.photos/seed/mitrc-ai-lecture/900/600"
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
    val image: String? = null,
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
        title = "TechFest 2026 registrations open",
        body = "Team registrations for the national level hackathon are now live. Last date is 26 Sep.",
        time = "1 hr ago",
        tone = NotificationTone.INFO,
        image = "https://picsum.photos/seed/notif-techfest/800/500"
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
        title = "Timetable revised for Week 6",
        body = "Thursday's second period has been shifted to Seminar Hall 2 for all Section B students.",
        time = "Yesterday",
        tone = NotificationTone.INFO,
        image = "https://picsum.photos/seed/notif-timetable/800/500"
    ),
    PortalNotification(
        id = 5,
        title = "Placement drive shortlist",
        body = "The Training & Placement Cell has published the first shortlist for the TCS drive.",
        time = "2 days ago",
        tone = NotificationTone.INFO,
        image = "https://picsum.photos/seed/notif-placement/800/500"
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

/** 1 = Monday ... 7 = Sunday, matching `MyTimetableEntryView.dayOfWeek`. */
fun currentDayOfWeek(): Int = Calendar.getInstance().get(Calendar.DAY_OF_WEEK).let {
    if (it == Calendar.SUNDAY) 7 else it
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

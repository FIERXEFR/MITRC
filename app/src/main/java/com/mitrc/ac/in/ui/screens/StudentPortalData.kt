package com.mitrc.ac.`in`.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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

/** The four fixed destinations in the bottom navigation bar. */
enum class StudentTab(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Outlined.Home),
    ATTENDANCE("Attendance", Icons.Outlined.Checklist),
    EVENTS("Events", Icons.Outlined.Campaign),
    SETTINGS("Settings", Icons.Outlined.Settings);

    companion object {
        fun fromName(name: String?): StudentTab =
            entries.firstOrNull { it.name == name } ?: HOME
    }
}

/** Attendance and the weekly timetable share one navigation entry. */
enum class ScheduleMode(val label: String) {
    ATTENDANCE("Attendance"),
    TIMETABLE("Timetable");

    companion object {
        fun fromName(name: String?): ScheduleMode =
            entries.firstOrNull { it.name == name } ?: ATTENDANCE
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

fun greetingForHour(hour: Int): String = when (hour) {
    in 5..11 -> "Good Morning"
    in 12..16 -> "Good Afternoon"
    in 17..20 -> "Good Evening"
    else -> "Good Night"
}

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

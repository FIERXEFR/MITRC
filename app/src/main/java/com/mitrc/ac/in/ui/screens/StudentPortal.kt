package com.mitrc.ac.`in`.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MarkEmailRead
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.mitrc.ac.`in`.R
import com.mitrc.ac.`in`.auth.AuthRepository
import com.mitrc.ac.`in`.data.*
import com.mitrc.ac.`in`.ui.theme.DarkTextMuted
import com.mitrc.ac.`in`.ui.theme.DividerSoft
import com.mitrc.ac.`in`.ui.theme.ErrorRed
import com.mitrc.ac.`in`.ui.theme.Gold
import com.mitrc.ac.`in`.ui.theme.GoldDeep
import com.mitrc.ac.`in`.ui.theme.GoldLight
import com.mitrc.ac.`in`.ui.theme.Navy
import com.mitrc.ac.`in`.ui.theme.NavyDeep
import com.mitrc.ac.`in`.ui.theme.NavySoft
import com.mitrc.ac.`in`.ui.theme.Slate
import com.mitrc.ac.`in`.ui.theme.SuccessGreen
import com.mitrc.ac.`in`.ui.theme.SurfaceWhite
import com.mitrc.ac.`in`.ui.theme.TextPrimary
import com.mitrc.ac.`in`.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.roundToInt

// =============================================================================================
// STUDENT PORTAL - MERGED UNIFIED UI SYSTEM
// =============================================================================================

// ---------------------------------------------------------------------------------------------
// Portal Palette & Theme
// ---------------------------------------------------------------------------------------------

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

// ---------------------------------------------------------------------------------------------
// Enums & Models
// ---------------------------------------------------------------------------------------------

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
    NOTES("Notes");

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

data class StudentPortalData(
    val profile: StudentProfileView?,
    val subjects: List<ClassSubjectView>,
    val attendance: List<AttendanceSummaryView>,
    val marks: List<MarkView>,
    val timetable: List<MyTimetableEntryView>,
    val error: String?,
    val faculty: List<ClassSubjectView> = emptyList(),
    val pdfNotes: List<PortalPdfNote> = emptyList()
)

data class CollegeEvent(
    val id: Int,
    val title: String,
    val category: String,
    val whenLabel: String,
    val venue: String,
    val image: String
)

fun sampleCollegeEvents(): List<CollegeEvent> = listOf(
    CollegeEvent(
        id = 1,
        title = "TechFest 2026 \u2014 National Level Hackathon",
        category = "TECH",
        whenLabel = "28 Sep \u00b7 09:00 AM",
        venue = "Innovation Hall, Block C",
        image = ImageKitConfig.resizedWebp("default-image.jpg", 800)
    ),
    CollegeEvent(
        id = 2,
        title = "Annual Cultural Night \u2014 Rhythms of Rajasthan",
        category = "CULTURAL",
        whenLabel = "02 Oct \u00b7 06:30 PM",
        venue = "Open Air Amphitheatre",
        image = ImageKitConfig.resizedWebp("default-image.jpg", 800)
    ),
    CollegeEvent(
        id = 3,
        title = "Campus Placement Drive \u2014 TCS & Infosys",
        category = "PLACEMENT",
        whenLabel = "05 Oct \u00b7 08:00 AM",
        venue = "Training & Placement Cell",
        image = ImageKitConfig.resizedWebp("default-image.jpg", 800)
    )
)

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
        title = "Attendance update",
        body = "Subject-wise attendance summary updated for Week 6.",
        time = "10 min ago",
        tone = NotificationTone.INFO
    ),
    PortalNotification(
        id = 2,
        title = "TechFest 2026 registration",
        body = "Team registrations for hackathon are open.",
        time = "1 hr ago",
        tone = NotificationTone.INFO
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

// ---------------------------------------------------------------------------------------------
// Utilities & Skeleton Loaders
// ---------------------------------------------------------------------------------------------

@Composable
fun Modifier.shimmerEffect(): Modifier {
    val transition = rememberInfiniteTransition(label = "shimmerTransition")
    val progress by transition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerProgress"
    )
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

fun todayLabel(): String =
    SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault()).format(Date())

fun firstNameOf(fullName: String?): String {
    val trimmed = fullName?.trim().orEmpty()
    if (trimmed.isEmpty()) return "Student"
    return trimmed.split(Regex("\\s+")).first()
}

fun currentDayOfWeek(): Int = Calendar.getInstance().get(Calendar.DAY_OF_WEEK).let {
    if (it == Calendar.SUNDAY) 7 else it
}

val streakDays: List<Pair<String, Boolean>>
    get() = listOf(
        "Mon" to true, "Tue" to true, "Wed" to true,
        "Thu" to true, "Fri" to true, "Sat" to true, "Sun" to false
    )

val weekBars: List<Pair<String, Int>>
    get() = listOf("Mon" to 7, "Tue" to 6, "Wed" to 6, "Thu" to 5, "Fri" to 6, "Sat" to 4, "Sun" to 0)

@Composable
internal fun PortalEmptyCard(message: String) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = PortalCard,
        border = BorderStroke(1.dp, PortalStroke),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(PortalTrack),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.Info,
                    contentDescription = null,
                    tint = PortalTextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = message,
                color = PortalTextSecondary,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Core Data Loading
// ---------------------------------------------------------------------------------------------

private suspend fun loadPortalData(userUid: String): StudentPortalData {
    val profile = PortalRepository.getStudentProfile(userUid)
    val classId = profile.getOrNull()?.classId ?: 0
    val subjects = PortalRepository.getStudentSubjects(classId).getOrDefault(emptyList())

    return StudentPortalData(
        profile = profile.getOrNull(),
        subjects = subjects,
        attendance = PortalRepository.getStudentAttendance(userUid).getOrDefault(emptyList()),
        marks = PortalRepository.getStudentMarks(userUid).getOrDefault(emptyList()),
        timetable = PortalRepository.getMyTimetable().getOrDefault(emptyList()),
        faculty = subjects,
        pdfNotes = PortalRepository.getPublishedNotes()
            .getOrDefault(emptyList())
            .toPortalPdfNotes(subjects = subjects),
        error = if (profile.isFailure) {
            profile.exceptionOrNull()?.message ?: "Failed to load student profile."
        } else null
    )
}

// ---------------------------------------------------------------------------------------------
// Student Portal Main Entry Composable
// ---------------------------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentPortalShell(userUid: String, onSignedOut: () -> Unit) {
    var data by remember { mutableStateOf<StudentPortalData?>(null) }
    var activeTab by rememberSaveable { mutableStateOf(StudentTab.HOME) }
    var scheduleMode by rememberSaveable { mutableStateOf(ScheduleMode.ATTENDANCE) }

    var isRefreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    var showFacultySheet by remember { mutableStateOf(false) }
    var showNotifSheet by remember { mutableStateOf(false) }

    val notifications = remember { sampleNotifications().toMutableStateList() }
    val personalNotes = remember { mutableStateListOf<PortalNote>() }

    fun refreshData() {
        scope.launch {
            isRefreshing = true
            val fresh = loadPortalData(userUid)
            data = fresh
            isRefreshing = false
        }
    }

    LaunchedEffect(userUid) {
        if (data == null) {
            val fresh = loadPortalData(userUid)
            data = fresh
        }
    }

    val homeScroll = rememberScrollState()
    val academicsScroll = rememberScrollState()
    val eventsScroll = rememberScrollState()
    val settingsScroll = rememberScrollState()

    val currentScroll = when (activeTab) {
        StudentTab.HOME -> homeScroll
        StudentTab.ATTENDANCE -> academicsScroll
        StudentTab.EVENTS -> eventsScroll
        StudentTab.SETTINGS -> settingsScroll
    }

    val haptic = LocalHapticFeedback.current

    val unreadNotifCount = remember(notifications) { notifications.count { it.unread } }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PortalBackground)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            StudentHeader(
                profile = data?.profile,
                activeTab = activeTab,
                scheduleMode = scheduleMode,
                unreadNotifCount = unreadNotifCount,
                onFacultyClick = { showFacultySheet = true },
                onNotifClick = { showNotifSheet = true }
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = ::refreshData,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(currentScroll)
                            .navigationBarsPadding()
                            .padding(bottom = 80.dp)
                    ) {
                        AnimatedContent(
                            targetState = activeTab,
                            label = "tabTransition"
                        ) { tab ->
                            when (tab) {
                                StudentTab.HOME -> {
                                    val d = data
                                    if (d == null) {
                                        HomeTabSkeleton()
                                    } else {
                                        StudentHomeTab(
                                            data = d,
                                            onNavigateTab = { target, mode ->
                                                activeTab = target
                                                if (mode != null) scheduleMode = mode
                                            },
                                            onOpenNotifs = { showNotifSheet = true }
                                        )
                                    }
                                }
                                StudentTab.ATTENDANCE -> {
                                    val d = data
                                    if (d == null) {
                                        HomeTabSkeleton()
                                    } else {
                                        AcademicsTabContent(
                                            data = d,
                                            mode = scheduleMode,
                                            onModeChanged = { scheduleMode = it },
                                            notes = personalNotes
                                        )
                                    }
                                }
                                StudentTab.EVENTS -> {
                                    EventsTabContent()
                                }
                                StudentTab.SETTINGS -> {
                                    val d = data
                                    if (d != null) {
                                        StudentSettingsTab(
                                            data = d,
                                            scroll = settingsScroll,
                                            onSignedOut = onSignedOut
                                        )
                                    } else {
                                        HomeTabSkeleton()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        StudentBottomNavigation(
            activeTab = activeTab,
            onTabSelected = { tab ->
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                activeTab = tab
            },
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        if (showFacultySheet) {
            ContactFacultySheet(
                facultyList = data?.faculty ?: emptyList(),
                onDismiss = { showFacultySheet = false }
            )
        }

        if (showNotifSheet) {
            NotificationsSheet(
                notifications = notifications,
                onDismiss = { showNotifSheet = false }
            )
        }
    }
}

/** Alias for StudentPortalShell to satisfy StudentPortal.kt single file UI */
@Composable
fun StudentPortal(userUid: String, onSignedOut: () -> Unit) {
    StudentPortalShell(userUid = userUid, onSignedOut = onSignedOut)
}

// ---------------------------------------------------------------------------------------------
// Header Composable
// ---------------------------------------------------------------------------------------------

@Composable
private fun StudentHeader(
    profile: StudentProfileView?,
    activeTab: StudentTab,
    scheduleMode: ScheduleMode,
    unreadNotifCount: Int,
    onFacultyClick: () -> Unit,
    onNotifClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(NavyDeep, Navy)))
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = todayLabel().uppercase(),
                        color = GoldLight,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Hello, ${firstNameOf(profile?.name)} \uD83D\uDC4B",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.12f))
                            .clickable(onClick = onFacultyClick),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.School,
                            contentDescription = "Contact Faculty",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.12f))
                            .clickable(onClick = onNotifClick),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.Notifications,
                            contentDescription = "Notifications",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        if (unreadNotifCount > 0) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .align(Alignment.TopEnd)
                                    .offset(x = (-2).dp, y = 2.dp)
                                    .background(Gold, CircleShape)
                            )
                        }
                    }
                }
            }

            if (profile != null) {
                Spacer(Modifier.height(10.dp))
                val subline = buildString {
                    append(profile.course ?: "Course")
                    if (!profile.branch.isNullOrBlank()) append(" - ").append(profile.branch)
                    if (profile.semester != null && profile.semester > 0) append(" | Sem ").append(profile.semester)
                    if (!profile.section.isNullOrBlank()) append(" (").append(profile.section).append(")")
                }
                Text(
                    text = subline,
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Home Tab Content
// ---------------------------------------------------------------------------------------------

@Composable
fun StudentHomeTab(
    data: StudentPortalData,
    onNavigateTab: (StudentTab, ScheduleMode?) -> Unit,
    onOpenNotifs: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Quick Action Tiles
        Text(
            text = "QUICK ACTIONS",
            color = PortalTextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            sampleQuickActions().take(2).forEach { action ->
                QuickActionCard(
                    action = action,
                    onClick = { onNavigateTab(action.target, action.mode) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            sampleQuickActions().drop(2).take(2).forEach { action ->
                QuickActionCard(
                    action = action,
                    onClick = { onNavigateTab(action.target, action.mode) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // Campus Events
        Text(
            text = "FEATURED CAMPUS EVENTS",
            color = PortalTextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp
        )

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            sampleCollegeEvents().forEach { event ->
                EventCard(event = event)
            }
        }
    }
}

@Composable
private fun QuickActionCard(action: QuickAction, onClick: () -> Unit, modifier: Modifier = Modifier) {
    ElevatedCard(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = PortalCard),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(action.accent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = action.icon,
                    contentDescription = null,
                    tint = action.accent,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = action.title,
                color = PortalTextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = action.subtitle,
                color = PortalTextSecondary,
                fontSize = 11.5.sp
            )
        }
    }
}

@Composable
private fun EventCard(event: CollegeEvent) {
    ElevatedCard(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = PortalCard),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            AsyncImage(
                model = event.image,
                contentDescription = event.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            )
            Column(modifier = Modifier.padding(16.dp)) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(PortalBlue.copy(alpha = 0.1f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = event.category,
                        color = PortalBlue,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = event.title,
                    color = PortalTextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${event.whenLabel}  \u00b7  ${event.venue}",
                    color = PortalTextSecondary,
                    fontSize = 12.sp
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Academics Tab Content
// ---------------------------------------------------------------------------------------------

@Composable
private fun AcademicsTabContent(
    data: StudentPortalData,
    mode: ScheduleMode,
    onModeChanged: (ScheduleMode) -> Unit,
    notes: MutableList<PortalNote>
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(PortalCardAlt)
                .padding(3.dp)
        ) {
            ScheduleMode.entries.forEach { item ->
                val active = mode == item
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(9.dp))
                        .background(if (active) PortalBlue else Color.Transparent)
                        .clickable { onModeChanged(item) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = item.label,
                        color = if (active) Color.White else PortalTextSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        when (mode) {
            ScheduleMode.ATTENDANCE -> AttendanceContent(data = data)
            ScheduleMode.TIMETABLE -> TimetableContent(data = data)
            ScheduleMode.NOTES -> NotesContent(data = data, notes = notes)
        }
    }
}

@Composable
private fun AttendanceContent(data: StudentPortalData) {
    if (data.attendance.isEmpty()) {
        PortalEmptyCard(message = "No attendance records recorded yet.")
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            data.attendance.forEach { summary ->
                ElevatedCard(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = PortalCard),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = summary.subjectCode,
                                color = PortalBlue,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${summary.percentage.roundToInt()}%",
                                color = if (summary.percentage >= 75) PortalGreen else PortalRose,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = summary.subjectName,
                            color = PortalTextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { (summary.percentage / 100.0).toFloat().coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(CircleShape),
                            color = if (summary.percentage >= 75) PortalGreen else PortalRose,
                            trackColor = PortalTrack,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Attended ${summary.attended} of ${summary.total} classes",
                            color = PortalTextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TimetableContent(data: StudentPortalData) {
    if (data.timetable.isEmpty()) {
        PortalEmptyCard(message = "No published timetable available.")
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            data.timetable.forEach { entry ->
                ElevatedCard(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = PortalCard),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = entry.dayName ?: "Day ${entry.dayOfWeek}",
                                color = PortalBlue,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${entry.startTime.orEmpty()} - ${entry.endTime.orEmpty()}",
                                color = PortalTextSecondary,
                                fontSize = 12.sp
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "${entry.subjectCode} · ${entry.subjectName}",
                            color = PortalTextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (!entry.teacherName.isNullOrBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = entry.teacherName,
                                color = PortalTextSecondary,
                                fontSize = 12.sp
                            )
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
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = 6.dp,
            pressedElevation = 2.dp
        ),
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
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(PortalBlue.copy(alpha = 0.12f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = subject.subjectCode.ifBlank { "SUB" },
                            color = PortalBlue,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (subject.subjectKind.equals("lab", ignoreCase = true))
                                    PortalPurple.copy(alpha = 0.12f)
                                else PortalGreen.copy(alpha = 0.12f)
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = subject.subjectKind.replaceFirstChar { it.uppercase() }.ifBlank { "Theory" },
                            color = if (subject.subjectKind.equals("lab", ignoreCase = true)) PortalPurple else PortalGreen,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (noteCount > 0) PortalAmber.copy(alpha = 0.25f) else PortalTrack)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (noteCount > 0) "$noteCount PDF${if (noteCount > 1) "s" else ""}" else "No PDFs",
                        color = if (noteCount > 0) PortalBlue else PortalTextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text = subject.subjectName,
                style = MaterialTheme.typography.titleMedium,
                color = PortalTextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )

            Spacer(Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(PortalBlue.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.School,
                        contentDescription = null,
                        tint = PortalBlue,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Column {
                    Text(
                        text = subject.teacherName.ifBlank { "Faculty Not Assigned" },
                        color = PortalTextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    val email = subject.teacherEmail
                    if (!email.isNullOrBlank()) {
                        Text(
                            text = email,
                            color = PortalTextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotesContent(data: StudentPortalData, notes: MutableList<PortalNote>) {
    val context = LocalContext.current
    val pdfNotes = data.pdfNotes
    val subjects = data.subjects

    var selectedSubject by remember { mutableStateOf("All") }
    var selectedCategory by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // --- ASSIGNED SUBJECTS & FACULTY SECTION ---
        Text(
            text = "ASSIGNED SUBJECTS & FACULTY",
            color = PortalTextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp
        )

        if (subjects.isEmpty()) {
            PortalEmptyCard(message = "No subjects assigned yet.")
        } else {
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
        }

        Spacer(Modifier.height(8.dp))

        // --- STUDY MATERIAL & PDF NOTES SECTION ---
        Text(
            text = "STUDY MATERIAL & PDF NOTES",
            color = PortalTextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp
        )

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search by title, topic or teacher...", fontSize = 12.5.sp) },
            leadingIcon = {
                Icon(
                    Icons.Outlined.Search,
                    contentDescription = "Search",
                    tint = PortalTextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        )

        val filtered = pdfNotes.filter { note ->
            val matchSubject = selectedSubject == "All" || note.subjectName.equals(selectedSubject, ignoreCase = true)
            val matchCategory = selectedCategory == "All" || note.category.equals(selectedCategory, ignoreCase = true)
            val matchQuery = searchQuery.isBlank() ||
                    note.title.contains(searchQuery, ignoreCase = true) ||
                    note.subjectName.contains(searchQuery, ignoreCase = true) ||
                    note.teacherName.contains(searchQuery, ignoreCase = true)
            matchSubject && matchCategory && matchQuery
        }

        if (filtered.isEmpty()) {
            PortalEmptyCard(
                message = if (pdfNotes.isEmpty()) "No PDF notes have been published yet." else "No published PDF notes match your filter."
            )
        } else {
            filtered.forEach { note ->
                PdfNoteCard(
                    note = note,
                    onOpenPdf = {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(note.driveUrl))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            // No browser app available
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun PdfNoteCard(note: PortalPdfNote, onOpenPdf: () -> Unit) {
    ElevatedCard(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = PortalCard),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenPdf)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${note.subjectCode} · ${note.subjectName}",
                    color = PortalBlue,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(PortalBlue.copy(alpha = 0.1f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = note.category.uppercase(),
                        color = PortalBlue,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = note.title,
                color = PortalTextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            if (!note.description.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = note.description,
                    color = PortalTextSecondary,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "By ${note.teacherName} · ${note.createdAt}",
                    color = PortalTextSecondary,
                    fontSize = 11.sp
                )
                Icon(
                    Icons.AutoMirrored.Outlined.OpenInNew,
                    contentDescription = "Open PDF",
                    tint = PortalBlue,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Events Tab
// ---------------------------------------------------------------------------------------------

@Composable
private fun EventsTabContent() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "ALL CAMPUS EVENTS",
            color = PortalTextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp
        )

        sampleCollegeEvents().forEach { event ->
            EventCard(event = event)
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Settings Tab Content
// ---------------------------------------------------------------------------------------------

@Composable
fun StudentSettingsTab(data: StudentPortalData, scroll: ScrollState, onSignedOut: () -> Unit) {
    val profile = data.profile
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "ACCOUNT & PROFILE",
            color = PortalTextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp
        )

        ElevatedCard(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = PortalCard),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = profile?.name ?: "Student Name",
                    color = PortalTextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = profile?.studentEmail ?: "student@mitrc.ac.in",
                    color = PortalTextSecondary,
                    fontSize = 13.sp
                )
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    Column {
                        Text("Roll / Serial No.", color = PortalTextSecondary, fontSize = 11.sp)
                        Text(profile?.serialNo ?: "-", color = PortalTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Column {
                        Text("Section", color = PortalTextSecondary, fontSize = 11.sp)
                        Text(profile?.section ?: "-", color = PortalTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        Button(
            onClick = {
                scope.launch {
                    AuthRepository.signOut()
                    onSignedOut()
                }
            },
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = ErrorRed,
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
    }
}

// ---------------------------------------------------------------------------------------------
// Navigation Bar & Sheets
// ---------------------------------------------------------------------------------------------

@Composable
private fun StudentBottomNavigation(
    activeTab: StudentTab,
    onTabSelected: (StudentTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = PortalCard,
        tonalElevation = 8.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, PortalStroke),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StudentTab.entries.forEach { tab ->
                val selected = activeTab == tab
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable { onTabSelected(tab) }
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.label,
                        tint = if (selected) PortalBlue else PortalTextSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = tab.label,
                        color = if (selected) PortalBlue else PortalTextSecondary,
                        fontSize = 11.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun ContactFacultySheet(
    facultyList: List<ClassSubjectView>,
    onDismiss: () -> Unit
) {
    Surface(
        color = PortalCard,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, PortalStroke),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "COURSE FACULTY",
                    color = PortalTextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = PortalTrack)
                ) {
                    Text("Close", color = PortalTextPrimary, fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(16.dp))

            if (facultyList.isEmpty()) {
                Text("No faculty contacts available.", color = PortalTextSecondary, fontSize = 13.sp)
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
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(PortalBlue.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.Person, contentDescription = null, tint = PortalBlue, modifier = Modifier.size(18.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(f.teacherName, color = PortalTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text(f.subjectName, color = PortalTextSecondary, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationsSheet(
    notifications: List<PortalNotification>,
    onDismiss: () -> Unit
) {
    Surface(
        color = PortalCard,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, PortalStroke),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "NOTIFICATIONS",
                    color = PortalTextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = PortalTrack)
                ) {
                    Text("Close", color = PortalTextPrimary, fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(16.dp))

            notifications.forEach { n ->
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    Text(n.title, color = PortalTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(2.dp))
                    Text(n.body, color = PortalTextSecondary, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun HomeTabSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .clip(RoundedCornerShape(18.dp))
                .shimmerEffect()
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(18.dp))
                .shimmerEffect()
        )
    }
}

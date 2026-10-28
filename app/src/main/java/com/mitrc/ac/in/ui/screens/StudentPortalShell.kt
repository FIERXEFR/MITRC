package com.mitrc.ac.`in`.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.MarkEmailRead
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.roundToInt
import com.mitrc.ac.`in`.R
import com.mitrc.ac.`in`.auth.AuthRepository
import com.mitrc.ac.`in`.data.*
import com.mitrc.ac.`in`.ui.theme.Gold
import com.mitrc.ac.`in`.ui.theme.GoldLight
import com.mitrc.ac.`in`.ui.theme.Navy
import com.mitrc.ac.`in`.ui.theme.NavyDeep

/** Everything the portal tabs need, loaded once in the shell and shared down. */
data class StudentPortalData(
    val profile: StudentProfileView?,
    val subjects: List<ClassSubjectView>,
    val attendance: List<AttendanceSummaryView>,
    val marks: List<MarkView>,
    val timetable: List<MyTimetableEntryView>,
    /** One entry per calendar day that had a marked period; empty until faculty mark attendance. */
    val dailyAttendance: List<DailyAttendance> = emptyList(),
    val error: String?,
    /** `v_my_subject_cards` rows for this student - backs the "Contact Faculty" sheet. */
    val faculty: List<ClassSubjectView> = emptyList(),
    /**
     * Teacher-published PDFs from the `notes` table, already filtered down to the subjects this
     * student is enrolled in - backs the Notes segment of the Academics tab.
     */
    val pdfNotes: List<PortalPdfNote> = emptyList(),
    /** Coordinator contact shown on the lock / PTM screen; null while the profile is active. */
    val coordinator: StudentCoordinatorContact? = null,
    /**
     * The student's real name resolved from the `students` row (never the email local part).
     * Null only while the portal is still loading or when no name exists anywhere.
     */
    val displayName: String? = null
)

/**
 * Fetches everything [StudentPortalShell] renders. Split out of the shell's [LaunchedEffect] so
 * the pull-to-refresh gestures can re-run it silently while the current data stays on screen.
 *
 * `getOrDefault` keeps one dead query (say a notes fetch that fails) from blanking the whole
 * portal; a failed profile still surfaces as [StudentPortalData.error] so the Retry screen shows.
 */
private suspend fun loadPortalData(userUid: String): StudentPortalData {
    val profileResult = PortalRepository.getStudentProfile(userUid)
    val profile = profileResult.getOrNull()

    // Greeting name: the `students` row is authoritative. Only when it has no name do we try a
    // direct lookup (uid, then student_email) and finally the Firebase display name. The email's
    // local part is never used - "Welcome, amit.yadav" is not this student's name.
    val displayName = profile?.name?.trim()?.takeIf { it.isNotEmpty() }
        ?: PortalRepository.getStudentName(
            uid = userUid,
            email = profile?.studentEmail ?: AuthRepository.currentUser?.email
        ).getOrNull()
        ?: AuthRepository.currentUser?.displayName?.trim()?.takeIf { it.isNotEmpty() }

    val classId = profile?.classId ?: 0
    val subjects = if (classId > 0) {
        PortalRepository.getStudentSubjects(classId).getOrDefault(emptyList())
    } else {
        emptyList()
    }

    // Only needed for the lock / PTM screen; skipped for an active profile so the extra
    // coordinator lookup never costs anything on the normal path.
    val coordinator = if (profile?.isLocked == true || profile?.isPtm == true) {
        PortalRepository.getStudentCoordinator(
            classId = profile.classId,
            courseName = profile.course,
            semester = profile.semester
        ).getOrNull()
    } else {
        null
    }

    val timetable = PortalRepository.getMyTimetable(classId.takeIf { it > 0 }).getOrDefault(emptyList())

    // Six months of day-by-day attendance powers the calendar's month navigation. The table pair
    // is empty in fresh deployments, so this returns [] and the calendar falls back to holidays.
    val today = LocalDate.now()
    val dailyAttendance = PortalRepository.getStudentDailyAttendance(
        uid = userUid,
        fromIsoDate = today.minusMonths(6).withDayOfMonth(1).toString(),
        toIsoDate = today.toString()
    ).getOrDefault(emptyList())

    return StudentPortalData(
        profile = profile,
        subjects = subjects,
        attendance = PortalRepository.getStudentAttendance(userUid).getOrDefault(emptyList()),
        marks = PortalRepository.getStudentMarks(userUid).getOrDefault(emptyList()),
        timetable = timetable,
        dailyAttendance = dailyAttendance,
        faculty = subjects,
        // Notes live in their own table; the mapper drops rows outside this student's subjects.
        pdfNotes = PortalRepository.getPublishedNotes(userUid, classId.takeIf { it > 0 })
            .getOrDefault(emptyList())
            .toPortalPdfNotes(subjects = subjects),
        coordinator = coordinator,
        displayName = displayName,
        error = if (profileResult.isFailure) {
            profileResult.exceptionOrNull()?.message ?: "Failed to load your student profile."
        } else {
            null
        }
    )
}

/**
 * The student portal's own scaffold: a collapsible navy header, a fixed bottom navigation bar,
 * and one scrollable content slot that swaps between tabs.
 *
 * It is rendered by [HomeScreen] instead of HomeScreen's shared header/sign-out scaffolding, so
 * the teacher and co-ordinator paths are untouched.
 */
@Composable
fun StudentPortalShell(userUid: String, onSignedOut: () -> Unit) {
    var data by remember { mutableStateOf<StudentPortalData?>(null) }
    var showNotifications by remember { mutableStateOf(false) }
    var showProfile by remember { mutableStateOf(false) }

    // Session-local notebook for the student's own jottings (the `notes` table is for
    // teacher-published PDFs instead). Hoisted here so switching tabs does not discard them.
    val notes = remember { mutableStateListOf<PortalNote>() }

    // Portrait-locked in the manifest, but persisted anyway so a process death keeps the tab.
    var tabName by rememberSaveable { mutableStateOf(StudentTab.HOME.name) }
    val tab = StudentTab.fromName(tabName)

    var scheduleModeName by rememberSaveable { mutableStateOf(ScheduleMode.ATTENDANCE.name) }
    val scheduleMode = ScheduleMode.fromName(scheduleModeName)

    // One scroll position per tab so switching tabs keeps each tab where the user left it.
    // Academics gets one state per mode: the pager composes neighbouring pages at the same time,
    // and two scrollables writing a single ScrollState fight over its value/maxValue.
    val homeScroll = rememberScrollState()
    val attendanceScroll = rememberScrollState()
    val timetableScroll = rememberScrollState()
    val notesScroll = rememberScrollState()
    val examsScroll = rememberScrollState()
    val eventsScroll = rememberScrollState()
    val settingsScroll = rememberScrollState()

    // The hero folds by consuming scroll deltas through nested scroll instead of mirroring the
    // list offset. Mirroring scrolled the content and grew the viewport at the same time, so on
    // the timetable the scroll range shrank under the current offset near the bottom and clamped
    // back and forth - the double hitch. The coordinator portal already folds this way.
    val foldDistancePx = with(LocalDensity.current) { 96.dp.toPx() }
    val headerOffset = remember { mutableFloatStateOf(0f) }
    val headerNestedScroll = remember(foldDistancePx) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                if (delta == 0f) return Offset.Zero
                // Dragging up folds the hero; dragging down unfolds it before the list moves.
                // Once it is fully unfolded the drag passes through to pull-to-refresh.
                val next = (headerOffset.floatValue - delta).coerceIn(0f, foldDistancePx)
                val used = next - headerOffset.floatValue
                if (used == 0f) return Offset.Zero
                headerOffset.floatValue = next
                return Offset(0f, -used)
            }
        }
    }

    LaunchedEffect(tab, scheduleMode) { headerOffset.floatValue = 0f }

    // Reloadable: a transient failure (e.g. a token that expired mid-session) can be retried
    // from the error screen instead of leaving the user stranded.
    var reloadKey by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(userUid, reloadKey) {
        data = null
        data = loadPortalData(userUid)
    }

    // Silent re-fetch used by the pull-to-refresh gestures: the current data stays on screen
    // instead of blanking out to the full-screen loading skeleton.
    val refreshData: suspend () -> Unit = {
        data = loadPortalData(userUid)
    }

    // A locked profile, or a scheduled PTM, takes over the whole portal: no header, no tabs and
    // no back out. The screen re-checks in the background and closes itself once the coordinator
    // unlocks the profile.
    data?.profile?.takeIf { it.isLocked == true || it.isPtm == true }?.let { locked ->
        StudentLockScreen(
            profile = locked,
            coordinator = data?.coordinator,
            onRefresh = refreshData
        )
        return
    }

    val pagerState = rememberPagerState(
        initialPage = StudentTab.entries.indexOf(tab).coerceAtLeast(0),
        pageCount = { StudentTab.entries.size }
    )

    LaunchedEffect(tab) {
        val targetIndex = StudentTab.entries.indexOf(tab)
        if (pagerState.currentPage != targetIndex && !pagerState.isScrollInProgress) {
            pagerState.animateScrollToPage(targetIndex, animationSpec = tween(300, easing = FastOutSlowInEasing))
        }
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }
            .distinctUntilChanged()
            .collect { page ->
                val settledTab = StudentTab.entries.getOrElse(page) { StudentTab.HOME }
                if (settledTab.name != tabName) {
                    tabName = settledTab.name
                }
            }
    }

    // Back never exits the student portal: it closes any open overlay, otherwise returns to the
    // Home tab; on Home itself the press is swallowed entirely.
    BackHandler {
        when {
            showNotifications -> showNotifications = false
            showProfile -> showProfile = false
            tab != StudentTab.HOME -> tabName = StudentTab.HOME.name
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PortalBackground)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            PortalHeader(
                tab = tab,
                scheduleMode = scheduleMode,
                name = data?.displayName,
                loading = data == null,
                scrolledPx = { headerOffset.floatValue },
                onBellClick = { showNotifications = true },
                onAvatarClick = { showProfile = true }
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .nestedScroll(headerNestedScroll)
            ) {
                val current = data
                if (current == null) {
                    // Load a skeleton that matches the tab the student is actually on, so the
                    // Academics tab never shows the Home layout while attendance is loading.
                    if (tab == StudentTab.ATTENDANCE && scheduleMode == ScheduleMode.ATTENDANCE) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(attendanceScroll)
                                .padding(horizontal = 20.dp, vertical = 18.dp)
                        ) {
                            AttendanceSkeleton()
                        }
                    } else {
                        PortalLoading()
                    }
                } else if (current.profile == null) {
                    if (current.error != null) {
                        PortalErrorState(
                            message = current.error,
                            onRetry = { reloadKey++ }
                        )
                    } else {
                        PortalNoProfileState()
                    }
                } else {
                    HorizontalPager(
                        state = pagerState,
                        beyondViewportPageCount = 3,
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        when (StudentTab.entries[page]) {
                            StudentTab.HOME -> StudentHomeTab(
                                data = current,
                                scroll = homeScroll,
                                onReload = refreshData,
                                onNavigate = { nextTab, mode ->
                                    if (mode != null) scheduleModeName = mode.name
                                    tabName = nextTab.name
                                    scope.launch {
                                        pagerState.animateScrollToPage(StudentTab.entries.indexOf(nextTab), animationSpec = tween(300, easing = FastOutSlowInEasing))
                                    }
                                }
                            )

                            StudentTab.ATTENDANCE -> StudentScheduleTab(
                                data = current,
                                attendanceScroll = attendanceScroll,
                                timetableScroll = timetableScroll,
                                notesScroll = notesScroll,
                                examsScroll = examsScroll,
                                mode = scheduleMode,
                                notes = notes,
                                onReload = refreshData,
                                onModeChange = { scheduleModeName = it.name }
                            )

                            StudentTab.EVENTS -> StudentEventsTab(scroll = eventsScroll)

                            StudentTab.SETTINGS -> StudentSettingsTab(
                                data = current,
                                scroll = settingsScroll,
                                onSignedOut = onSignedOut
                            )
                        }
                    }
                }
            }

            StudentBottomBar(
                selected = tab,
                onSelect = { selectedTab ->
                    tabName = selectedTab.name
                    scope.launch {
                        pagerState.animateScrollToPage(StudentTab.entries.indexOf(selectedTab), animationSpec = tween(300, easing = FastOutSlowInEasing))
                    }
                }
            )
        }

        AnimatedVisibility(
            visible = showNotifications,
            enter = fadeIn(tween(240)) + slideInHorizontally(tween(320)) { it },
            exit = fadeOut(tween(180)) + slideOutHorizontally(tween(260)) { it },
            modifier = Modifier.fillMaxSize()
        ) {
            NotificationsScreen(onBack = { showNotifications = false })
        }

        PortalSheet(
            visible = showProfile,
            onDismiss = { showProfile = false }
        ) {
            ProfileSheetContent(
                data = data,
                onClose = { showProfile = false }
            )
        }
    }
}

@Composable
private fun rememberScrollState() = androidx.compose.foundation.rememberScrollState()

// ---------------------------------------------------------------------------------------------
// Header - navy gradient with the gold orb, matching the login and onboarding headers.
// The hero text rolls up and fades as the tab's list scrolls; the bell and avatar stay pinned so
// they remain reachable at any scroll position.
// ---------------------------------------------------------------------------------------------

@Composable
private fun PortalHeader(
    tab: StudentTab,
    scheduleMode: ScheduleMode,
    name: String?,
    loading: Boolean,
    scrolledPx: () -> Float,
    onBellClick: () -> Unit,
    onAvatarClick: () -> Unit
) {
    // "Welcome" the first open of the day, "HEY" for every later open - resolved once and
    // held for the lifetime of the shell.
    val greeting = remember { GreetingStore.greeting() }

    val title = when {
        // No placeholder text while loading: the hero shows a shimmer instead so a student is
        // never greeted with a derived name that is about to be replaced.
        loading -> ""

        tab == StudentTab.HOME ->
            "$greeting, ${firstNameOf(name)} \ud83d\udc4b"

        tab == StudentTab.ATTENDANCE -> scheduleMode.label
        else -> tab.label
    }

    PortalHeroHeader(
        overline = "STUDENT PORTAL",
        title = title,
        name = name,
        scrolledPx = scrolledPx,
        // The Settings tab already lists the profile in full, so the avatar duplicate is
        // hidden there.
        showAvatar = tab != StudentTab.SETTINGS,
        onBellClick = onBellClick,
        onAvatarClick = onAvatarClick
    )
}

/**
 * The shared collapsible navy hero: identical logo, type scale, gold orb and fold distance in
 * both portals, so the coordinator header is not a lookalike but the same composable.
 *
 * [scrolledPx] is deliberately read *only* inside the layout / graphicsLayer lambdas - neither
 * is composition - so scrolling never recomposes the header.
 */
@Composable
internal fun PortalHeroHeader(
    overline: String,
    title: String,
    name: String?,
    scrolledPx: () -> Float,
    showAvatar: Boolean = true,
    onBellClick: () -> Unit,
    onAvatarClick: () -> Unit
) {
    val unread = remember { sampleNotifications().count { it.unread } }

    val headerShape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
    val verticalPad = 6.dp
    // Content budget: logo 16 + 4 + overline 12 + 3 + title 24 + 2 + date 13 = 74.
    val heroContentHeight = 74.dp
    val fullHeaderHeight = heroContentHeight + verticalPad * 2

    val density = LocalDensity.current
    // The fold used to finish in 36.dp of scroll, which is why it snapped instead of rolling.
    // It now takes ~96.dp, i.e. a little longer than the card is tall, so the whole collapse
    // is one continuous gesture.
    val foldDistancePx = with(density) { 96.dp.toPx() }
    val fullHeaderHeightPx = with(density) { fullHeaderHeight.toPx() }

    // The scroll offset is deliberately read *only* inside the offset / layer / measure
    // lambdas: none of them are composition, so scrolling the list no longer recomposes the
    // header on every frame. That per-frame recomposition was the source of the stutter.
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(headerShape)
            .background(
                brush = Brush.verticalGradient(listOf(NavyDeep, Navy)),
                shape = headerShape
            )
            .statusBarsPadding()
            .layout { measurable, constraints ->
                val collapse = (scrolledPx() / foldDistancePx).coerceIn(0f, 1f)
                val heightPx = (fullHeaderHeightPx * (1f - collapse))
                    .roundToInt()
                    .coerceIn(0, constraints.maxHeight)
                val placeable = measurable.measure(
                    constraints.copy(minHeight = heightPx, maxHeight = heightPx)
                )
                layout(constraints.maxWidth, heightPx) { placeable.place(0, 0) }
            }
            .padding(start = 22.dp, end = 22.dp, top = verticalPad, bottom = verticalPad)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(150.dp)
                .offset(x = 56.dp, y = (-58).dp)
                .background(Gold.copy(alpha = 0.09f), CircleShape)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                // Draw-phase only: the roll-up and the fade happen on the graphics layer, so a
                // scroll frame never re-lays out the hero column (nor recomposes the header).
                .graphicsLayer {
                    val collapse = (scrolledPx() / foldDistancePx).coerceIn(0f, 1f)
                    translationY = -fullHeaderHeightPx * collapse
                    alpha = 1f - collapse
                },
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Image(
                    painter = painterResource(R.drawable.logo_mitrc_white),
                    contentDescription = "MITRC logo",
                    contentScale = ContentScale.Fit,
                    alignment = Alignment.CenterStart,
                    modifier = Modifier
                        .width(74.dp)
                        .height(16.dp)
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = overline,
                    color = GoldLight,
                    fontSize = 10.sp,
                    lineHeight = 12.sp,
                    letterSpacing = 1.8.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(Modifier.height(3.dp))

                AnimatedContent(
                    targetState = title,
                    // Same rule as the tab switch: the old title leaves first (and slides clear),
                    // then the new one rolls in, so two titles are never legible at the same time.
                    transitionSpec = {
                        (fadeIn(tween(340, delayMillis = 150)) +
                            slideInVertically(tween(400, delayMillis = 150)) { -it / 4 })
                            .togetherWith(
                                fadeOut(tween(150)) +
                                    slideOutVertically(tween(200)) { it / 4 }
                            )
                    },
                    label = "headerTitle"
                ) { text ->
                    if (text.isBlank()) {
                        // Placeholder sized like the headline so the hero never flashes a
                        // derived name and doesn't jump when the real one arrives.
                        Box(
                            modifier = Modifier
                                .padding(vertical = 3.dp)
                                .width(176.dp)
                                .height(18.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .shimmerEffect()
                        )
                    } else {
                        Text(
                            text = text,
                            color = Color.White,
                            fontSize = 20.sp,
                            lineHeight = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.4).sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(Modifier.height(2.dp))

                Text(
                    text = todayLabel(),
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 11.5.sp,
                    lineHeight = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(Modifier.width(8.dp))

            // Pinned to the top with a fixed offset so their position relative to the hero
            // column never shifts while the card folds.
            Box(modifier = Modifier.padding(top = 18.dp)) {
                NotificationBell(unreadCount = unread, onClick = onBellClick)
            }

            Spacer(Modifier.width(6.dp))

            if (showAvatar) {
                Box(modifier = Modifier.padding(top = 18.dp)) {
                    StudentAvatar(name = name, onClick = onAvatarClick)
                }
            }
        }
    }
}

@Composable
private fun NotificationBell(unreadCount: Int, onClick: () -> Unit) {
    Box {
        Surface(
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.10f),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.20f)),
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .clickable(onClick = onClick)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Outlined.Notifications,
                    contentDescription = "Notifications",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        if (unreadCount > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(10.dp)
                    .background(PortalRose, CircleShape)
                    .border(2.dp, Navy, CircleShape)
            )
        }
    }
}

@Composable
private fun StudentAvatar(name: String?, onClick: () -> Unit) {
    val initials = firstNameOf(name).take(1).uppercase()
    Surface(
        shape = CircleShape,
        border = androidx.compose.foundation.BorderStroke(1.5.dp, Gold),
        color = Gold.copy(alpha = 0.18f),
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (initials == "S" && firstNameOf(name) == "Student") {
                Icon(
                    Icons.Outlined.Person,
                    contentDescription = "Profile",
                    tint = Gold,
                    modifier = Modifier.size(19.dp)
                )
            } else {
                Text(
                    text = initials,
                    color = Gold,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Bottom navigation - the same light track / navy pill treatment as the login tab switcher.
// ---------------------------------------------------------------------------------------------

@Composable
private fun StudentBottomBar(
    selected: StudentTab,
    onSelect: (StudentTab) -> Unit
) {
    Surface(
        color = PortalCard,
        shadowElevation = 16.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(PortalCardAlt, RoundedCornerShape(18.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            StudentTab.entries.forEach { item ->
                val isSelected = item == selected
                val scale by animateFloatAsState(
                    targetValue = if (isSelected) 1.06f else 1f,
                    animationSpec = tween(240),
                    label = "navScale"
                )
                val container by animateColorAsState(
                    targetValue = if (isSelected) PortalBlue else Color.Transparent,
                    animationSpec = tween(240),
                    label = "navBg"
                )
                val content by animateColorAsState(
                    targetValue = if (isSelected) Color.White else PortalTextSecondary,
                    animationSpec = tween(240),
                    label = "navContent"
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(13.dp))
                        .background(container)
                        .clickable { onSelect(item) }
                        .padding(vertical = 9.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        item.icon,
                        contentDescription = item.label,
                        tint = content,
                        modifier = Modifier
                            .size(22.dp)
                            .scale(scale)
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = item.label,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        color = content,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Shared bottom sheet - scrim + slide-up, used by the profile and contact sheets.
// ---------------------------------------------------------------------------------------------

@Composable
internal fun PortalSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(220)),
        exit = fadeOut(tween(180)),
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
                .clickable(onClick = onDismiss)
        )
    }

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(tween(340)) { it },
        exit = slideOutVertically(tween(240)) { it },
        modifier = Modifier.fillMaxSize()
    ) {
        // The drag state lives *inside* the visibility block, so it is thrown away together
        // with the content: every time the sheet is reopened it starts back at offset 0.
        var dragOffset by remember { mutableStateOf(0f) }
        var dragging by remember { mutableStateOf(false) }
        val dragThresholdPx = with(LocalDensity.current) { 110.dp.toPx() }

        // While the finger is down the value simply *is* the finger position (snap), so the
        // card tracks the gesture with no lag. The moment the gesture ends the target becomes
        // 0 and the same state object springs the card back to its resting position.
        val offsetY by animateFloatAsState(
            targetValue = if (dragging) dragOffset else 0f,
            animationSpec = if (dragging) {
                snap<Float>()
            } else {
                spring<Float>(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            },
            label = "sheetDragOffset"
        )

        // If the sheet is dismissed mid-drag and reopened before the exit animation finishes,
        // the content is still in composition - make sure it starts back at rest.
        LaunchedEffect(visible) {
            if (visible) {
                dragging = false
                dragOffset = 0f
            }
        }

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            Surface(
                shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
                color = PortalCard,
                shadowElevation = 24.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    // Layout-phase read: the finger-tracking offset re-lays out only this card
                    // and never recomposes the sheet.
                    .offset { IntOffset(0, offsetY.roundToInt()) }
                    .draggable(
                        state = rememberDraggableState { delta ->
                            // Downward only - the sheet can never be pulled past its rest spot.
                            dragOffset = (dragOffset + delta).coerceAtLeast(0f)
                        },
                        orientation = Orientation.Vertical,
                        onDragStarted = { dragging = true },
                        onDragStopped = { velocity ->
                            if (dragOffset > dragThresholdPx || velocity > 2500f) {
                                // Far enough down (or a confident flick): close the sheet and
                                // leave the offset where the finger left it, so the card keeps
                                // travelling down while the exit animation plays.
                                onDismiss()
                            } else {
                                // Not far enough: release the finger and spring back to rest.
                                dragging = false
                                dragOffset = 0f
                            }
                        }
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
            ) {
                // The inset is applied to the content, not the Surface: padding the Surface
                // itself lifted the whole card above the nav bar and left a transparent gap
                // underneath it. Now the card runs to the physical screen edge and only the
                // content is kept clear of the system bars.
                Box(modifier = Modifier.navigationBarsPadding()) {
                    SheetHandle()
                    content()
                }
            }
        }
    }
}

@Composable
private fun SheetHandle() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(42.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(PortalStroke)
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Profile sheet
// ---------------------------------------------------------------------------------------------

@Composable
private fun ProfileSheetContent(
    data: StudentPortalData?,
    onClose: () -> Unit
) {
    val profile = data?.profile

    Column(modifier = Modifier.fillMaxWidth().padding(top = 26.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                border = androidx.compose.foundation.BorderStroke(2.dp, PortalAmber),
                color = PortalAmber.copy(alpha = 0.16f),
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Outlined.School,
                        contentDescription = null,
                        tint = PortalAmber,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(
                    text = data?.displayName ?: portalDisplayName(profile).takeIf { it != "Student" } ?: "Student Profile",
                    color = PortalTextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Serial: ${profile?.serialNo ?: "N/A"}",
                    color = PortalTextSecondary,
                    fontSize = 12.5.sp
                )
            }
        }

        Spacer(Modifier.height(18.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(PortalStroke)
        )

        Column(modifier = Modifier.padding(horizontal = 22.dp, vertical = 14.dp)) {
            SheetRow(
                label = "Cohort",
                value = buildString {
                    append(profile?.course ?: "B.Tech")
                    if (!profile?.branch.isNullOrBlank()) append(" - ").append(profile?.branch)
                    if (profile?.semester != null) append(" | Sem ").append(profile.semester)
                    if (!profile?.section.isNullOrBlank()) append(" - Sec ").append(profile?.section)
                }
            )
            SheetRow(
                label = "Lab Group",
                value = profile?.groupName ?: "Not assigned",
                highlight = profile?.groupName.isNullOrBlank()
            )
            SheetRow(label = "Academic Year", value = profile?.academicYear ?: "N/A")
            if (!profile?.fatherName.isNullOrBlank()) {
                SheetRow(label = "Father's Name", value = profile?.fatherName.orEmpty())
            }
            SheetRow(label = "Signed in as", value = AuthRepositoryEmail())
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 14.dp)
        ) {
            Button(
                onClick = onClose,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PortalBlue.copy(alpha = 0.10f),
                    contentColor = PortalBlue
                )
            ) {
                Text("Close", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(Modifier.height(6.dp))
    }
}

@Composable
private fun AuthRepositoryEmail(): String =
    com.mitrc.ac.`in`.auth.AuthRepository.currentUser?.email ?: "-"

@Composable
private fun SheetRow(label: String, value: String, highlight: Boolean = false) {
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
            color = if (highlight) PortalRose else PortalTextPrimary,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
            modifier = Modifier.padding(start = 16.dp)
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Loading / error
// ---------------------------------------------------------------------------------------------

@Composable
internal fun PortalLoading() {
    HomeTabSkeleton()
}

/**
 * Full-screen failure state. [message] is the raw exception text, so it is only shown when it
 * looks readable; auth failures in particular are mapped to something a student can act on.
 */
@Composable
internal fun PortalErrorState(message: String, onRetry: (() -> Unit)? = null) {
    val raw = message.lowercase()
    val isAuthProblem = listOf("jwt", "token", "unauthorized", "expired", "pgrst301")
        .any { marker -> raw.contains(marker) }
    val detail = when {
        isAuthProblem -> "Your session expired. Tap retry to sign you back in."
        message.isBlank() -> "Check your internet connection and try again."
        else -> message
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Outlined.ErrorOutline,
                contentDescription = null,
                tint = PortalRose,
                modifier = Modifier.size(40.dp)
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = "Something went wrong",
                color = PortalTextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = detail,
                color = PortalTextSecondary,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )

            if (onRetry != null) {
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = onRetry,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PortalBlue,
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        Icons.Outlined.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(text = "Retry", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

/** Signed in, but no `students` row is linked to this account yet. */
@Composable
internal fun PortalNoProfileState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(CircleShape)
                    .background(PortalBlue.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.Person,
                    contentDescription = null,
                    tint = PortalBlue,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = "No Student Profile Found",
                color = PortalTextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Your account is not linked to a student record yet. Contact the admin panel.",
                color = PortalTextSecondary,
                fontSize = 13.sp
            )
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Notifications - opened from the bell, mocked for now.
// ---------------------------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NotificationsScreen(onBack: () -> Unit) {
    var isRefreshing by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val notifications = remember { mutableStateListOf<PortalNotification>() }

    suspend fun loadNotifications() {
        val rows = PortalRepository.getNotifications().getOrDefault(emptyList())
        notifications.clear()
        if (rows.isNotEmpty()) {
            notifications.addAll(
                rows.map { row ->
                    val tone = when (row.priority) {
                        "urgent", "high" -> NotificationTone.ALERT
                        else -> NotificationTone.INFO
                    }
                    PortalNotification(
                        id = row.id.toInt(),
                        title = row.title,
                        body = row.body ?: "",
                        time = row.createdAt?.take(10) ?: "Recent",
                        tone = tone,
                        image = row.imageUrl,
                        unread = true
                    )
                }
            )
        } else {
            notifications.addAll(sampleNotifications())
        }
    }

    LaunchedEffect(Unit) {
        isLoading = true
        loadNotifications()
        isLoading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PortalBackground)
            // The overlay is drawn over the portal header, and a tap that lands on a non-clickable
            // child (title text, the mark-all icon's padding) would otherwise fall through to the
            // avatar underneath and pop the profile sheet. Consuming here stops that.
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 8.dp, end = 20.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Back",
                    tint = PortalTextPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(Modifier.width(6.dp))

            Text(
                text = "Notifications",
                color = PortalTextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold
            )

            Spacer(Modifier.weight(1f))

            Icon(
                Icons.Outlined.MarkEmailRead,
                contentDescription = "Mark all as read",
                tint = PortalBlue,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable {
                        for (index in notifications.indices) {
                            notifications[index] = notifications[index].copy(unread = false)
                        }
                    }
                    .padding(6.dp)
                    .size(20.dp)
            )
        }

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                scope.launch {
                    isRefreshing = true
                    loadNotifications()
                    isRefreshing = false
                }
            },
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 20.dp,
                    end = 20.dp,
                    top = 4.dp,
                    bottom = 32.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (isLoading || isRefreshing) {
                    items(4) {
                        NotificationRowSkeleton()
                    }
                } else {
                    items(notifications, key = { it.id }) { item ->
                        NotificationRow(item = item)
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationRow(item: PortalNotification) {
    val accent = when (item.tone) {
        NotificationTone.INFO -> PortalBlue
        NotificationTone.ALERT -> PortalRose
        NotificationTone.SUCCESS -> PortalGreen
    }
    val icon = when (item.tone) {
        NotificationTone.INFO -> Icons.Outlined.Campaign
        NotificationTone.ALERT -> Icons.Outlined.ErrorOutline
        NotificationTone.SUCCESS -> Icons.Outlined.CheckCircle
    }

    ElevatedCard(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = PortalCard),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp),
        modifier = Modifier
            .fillMaxWidth()
            // The BOM's ElevatedCard has no border parameter, so the unread accent is drawn as a
            // modifier border over the raised surface instead.
            .border(
                width = 1.dp,
                color = if (item.unread) accent.copy(alpha = 0.5f) else PortalStroke,
                shape = RoundedCornerShape(18.dp)
            )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Surface(
                    shape = CircleShape,
                    color = accent.copy(alpha = 0.12f),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            icon,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = item.title,
                            color = PortalTextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (item.unread) {
                            Spacer(Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(PortalRose, CircleShape)
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = item.body,
                        color = PortalTextSecondary,
                        fontSize = 12.5.sp,
                        lineHeight = 18.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = item.time,
                        color = PortalTextSecondary.copy(alpha = 0.7f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            val image = item.image
            if (image != null) {
                Spacer(Modifier.height(12.dp))
                AsyncImage(
                    model = image,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    placeholder = ColorPainter(PortalCardAlt),
                    error = ColorPainter(PortalCardAlt),
                    fallback = ColorPainter(PortalCardAlt),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .clip(RoundedCornerShape(14.dp))
                )
            }
        }
    }
}

/** Small shared section label used by the portal tabs. */
@Composable
internal fun PortalSectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = PortalTextSecondary,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.6.sp,
        modifier = modifier
    )
}

/** Small shared empty-state card used by the portal tabs. */
@Composable
internal fun PortalEmptyCard(message: String) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = PortalCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, PortalStroke),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier.padding(22.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(text = message, color = PortalTextSecondary, fontSize = 13.sp)
        }
    }
}

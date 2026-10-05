package com.mitrc.ac.`in`.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.mitrc.ac.`in`.R
import com.mitrc.ac.`in`.data.AttendanceSummaryView
import com.mitrc.ac.`in`.data.CoordinatorRow
import com.mitrc.ac.`in`.data.MarkView
import com.mitrc.ac.`in`.data.MyTimetableEntryView
import com.mitrc.ac.`in`.data.PortalRepository
import com.mitrc.ac.`in`.data.StudentDirectoryView
import com.mitrc.ac.`in`.data.StudentSubjectView
import com.mitrc.ac.`in`.ui.theme.Gold
import com.mitrc.ac.`in`.ui.theme.GoldLight
import com.mitrc.ac.`in`.ui.theme.Navy
import com.mitrc.ac.`in`.ui.theme.NavyDeep

/** Everything the portal tabs need, loaded once in the shell and shared down. */
data class StudentPortalData(
    val profile: StudentDirectoryView?,
    val subjects: List<StudentSubjectView>,
    val attendance: List<AttendanceSummaryView>,
    val marks: List<MarkView>,
    val timetable: List<MyTimetableEntryView>,
    val coordinators: List<CoordinatorRow>,
    val error: String?
)

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

    // Portrait-locked in the manifest, but persisted anyway so a process death keeps the tab.
    var tabName by rememberSaveable { mutableStateOf(StudentTab.HOME.name) }
    val tab = StudentTab.fromName(tabName)

    var scheduleModeName by rememberSaveable { mutableStateOf(ScheduleMode.ATTENDANCE.name) }
    val scheduleMode = ScheduleMode.fromName(scheduleModeName)

    // One scroll position per tab so the header collapses against whichever list is on screen and
    // switching tabs keeps each tab where the user left it.
    val homeScroll = rememberScrollState()
    val scheduleScroll = rememberScrollState()
    val eventsScroll = rememberScrollState()
    val settingsScroll = rememberScrollState()

    // Reloadable: a transient failure (e.g. a token that expired mid-session) can be retried
    // from the error screen instead of leaving the user stranded.
    var reloadKey by remember { mutableStateOf(0) }

    LaunchedEffect(userUid, reloadKey) {
        data = null
        val profile = PortalRepository.getStudentProfile(userUid)
        data = StudentPortalData(
            profile = profile.getOrNull(),
            subjects = PortalRepository.getStudentSubjects(userUid).getOrDefault(emptyList()),
            attendance = PortalRepository.getStudentAttendance(userUid).getOrDefault(emptyList()),
            marks = PortalRepository.getStudentMarks(userUid).getOrDefault(emptyList()),
            timetable = PortalRepository.getMyTimetable().getOrDefault(emptyList()),
            coordinators = PortalRepository.getCoordinators().getOrDefault(emptyList()),
            error = if (profile.isFailure) {
                profile.exceptionOrNull()?.message ?: "Failed to load your student profile."
            } else {
                null
            }
        )
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
                name = data?.profile?.name,
                scroll = when (tab) {
                    StudentTab.HOME -> homeScroll
                    StudentTab.ATTENDANCE -> scheduleScroll
                    StudentTab.EVENTS -> eventsScroll
                    StudentTab.SETTINGS -> settingsScroll
                },
                onBellClick = { showNotifications = true },
                onAvatarClick = { showProfile = true }
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                val current = data
                if (current == null) {
                    PortalLoading()
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
                    AnimatedContent(
                        targetState = tab,
                        transitionSpec = {
                            fadeIn(tween(320, easing = { 1f - (1f - it) * (1f - it) }))
                                .togetherWith(fadeOut(tween(160)))
                        },
                        modifier = Modifier.fillMaxSize(),
                        label = "studentTabSwitch"
                    ) { active ->
                        when (active) {
                            StudentTab.HOME -> StudentHomeTab(
                                data = current,
                                scroll = homeScroll,
                                onNavigate = { nextTab, mode ->
                                    if (mode != null) scheduleModeName = mode.name
                                    tabName = nextTab.name
                                }
                            )

                            StudentTab.ATTENDANCE -> StudentScheduleTab(
                                data = current,
                                scroll = scheduleScroll,
                                mode = scheduleMode,
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
                onSelect = { tabName = it.name }
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
    scroll: ScrollState,
    onBellClick: () -> Unit,
    onAvatarClick: () -> Unit
) {
    val unread = remember { sampleNotifications().count { it.unread } }

    val density = LocalDensity.current
    val maxCollapsePx = with(density) { 36.dp.toPx() }
    val collapse = (scroll.value / maxCollapsePx).coerceIn(0f, 1f)

    val title = when {
        tab == StudentTab.HOME ->
            "${greetingForHour(java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY))}, ${firstNameOf(name)} \ud83d\udc4b"

        tab == StudentTab.ATTENDANCE -> scheduleMode.label
        else -> tab.label
    }

    val headerShape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
    val verticalPad = 6.dp
    // Content budget: logo 16 + 4 + overline 12 + 3 + title 24 + 2 + date 13 = 74.
    val heroContentHeight = 74.dp
    val fullHeaderHeight = heroContentHeight + verticalPad * 2
    // 1f - collapse keeps the card anchored to its bottom edge, so the whole card (logo, hero
    // text, bell and avatar) rolls up under the status bar instead of only the text shrinking.
    val headerHeight = fullHeaderHeight * (1f - collapse)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(headerShape)
            .background(
                brush = Brush.verticalGradient(listOf(NavyDeep, Navy)),
                shape = headerShape
            )
            .statusBarsPadding()
            .height(headerHeight)
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
                .offset(y = -fullHeaderHeight * collapse)
                .alpha(1f - collapse),
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
                    text = "STUDENT PORTAL",
                    color = GoldLight,
                    fontSize = 10.sp,
                    lineHeight = 12.sp,
                    letterSpacing = 1.8.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(Modifier.height(3.dp))

                AnimatedContent(
                    targetState = title,
                    transitionSpec = {
                        (fadeIn(tween(380)) + slideInVertically(tween(420)) { -it / 4 })
                            .togetherWith(
                                fadeOut(tween(200)) +
                                    slideOutVertically(tween(300)) { it / 4 }
                            )
                    },
                    label = "headerTitle"
                ) { text ->
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

            Box(modifier = Modifier.padding(top = 18.dp)) {
                StudentAvatar(name = name, onClick = onAvatarClick)
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
                    text = profile?.name ?: "Student Profile",
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
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = PortalBlue)
    }
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

@Composable
private fun NotificationsScreen(onBack: () -> Unit) {
    val notifications = remember { sampleNotifications().toMutableStateList() }

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
            items(notifications) { item ->
                NotificationRow(item = item)
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

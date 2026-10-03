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
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.mitrc.ac.`in`.data.AttendanceSummaryView
import com.mitrc.ac.`in`.data.MarkView
import com.mitrc.ac.`in`.data.MyTimetableEntryView
import com.mitrc.ac.`in`.data.PortalRepository
import com.mitrc.ac.`in`.data.StudentDirectoryView
import com.mitrc.ac.`in`.data.StudentSubjectView
import com.mitrc.ac.`in`.ui.theme.Gold
import com.mitrc.ac.`in`.ui.theme.GoldLight
import com.mitrc.ac.`in`.ui.theme.Navy
import com.mitrc.ac.`in`.ui.theme.NavyDeep
import kotlinx.coroutines.delay

/** Everything the portal tabs need, loaded once in the shell and shared down. */
data class StudentPortalData(
    val profile: StudentDirectoryView?,
    val subjects: List<StudentSubjectView>,
    val attendance: List<AttendanceSummaryView>,
    val marks: List<MarkView>,
    val timetable: List<MyTimetableEntryView>,
    val error: String?
)

/**
 * The student portal's own scaffold: a fixed navy header, a fixed bottom navigation bar, and one
 * scrollable content slot that swaps between tabs.
 *
 * It is rendered by [HomeScreen] instead of HomeScreen's shared header/sign-out scaffolding, so
 * the teacher and co-ordinator paths are untouched.
 */
@Composable
fun StudentPortalShell(userUid: String, onSignedOut: () -> Unit) {
    var data by remember { mutableStateOf<StudentPortalData?>(null) }
    var showNotifications by remember { mutableStateOf(false) }

    // Portrait-locked in the manifest, but persisted anyway so a process death keeps the tab.
    var tabName by rememberSaveable { mutableStateOf(StudentTab.HOME.name) }
    val tab = StudentTab.fromName(tabName)

    var scheduleModeName by rememberSaveable { mutableStateOf(ScheduleMode.ATTENDANCE.name) }
    val scheduleMode = ScheduleMode.fromName(scheduleModeName)

    LaunchedEffect(userUid) {
        val profile = PortalRepository.getStudentProfile(userUid)
        data = StudentPortalData(
            profile = profile.getOrNull(),
            subjects = PortalRepository.getStudentSubjects(userUid).getOrDefault(emptyList()),
            attendance = PortalRepository.getStudentAttendance(userUid).getOrDefault(emptyList()),
            marks = PortalRepository.getStudentMarks(userUid).getOrDefault(emptyList()),
            timetable = PortalRepository.getMyTimetable().getOrDefault(emptyList()),
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
                onBellClick = { showNotifications = true }
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
                        PortalErrorState(message = current.error)
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
                                onNavigate = { nextTab, mode ->
                                    if (mode != null) scheduleModeName = mode.name
                                    tabName = nextTab.name
                                }
                            )

                            StudentTab.ATTENDANCE -> StudentScheduleTab(
                                data = current,
                                mode = scheduleMode,
                                onModeChange = { scheduleModeName = it.name }
                            )

                            StudentTab.EVENTS -> StudentEventsTab()

                            StudentTab.SETTINGS -> StudentSettingsTab(
                                data = current,
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
    }
}

// ---------------------------------------------------------------------------------------------
// Header - navy gradient with the gold orb, matching the login and onboarding headers.
// ---------------------------------------------------------------------------------------------

@Composable
private fun PortalHeader(
    tab: StudentTab,
    scheduleMode: ScheduleMode,
    name: String?,
    onBellClick: () -> Unit
) {
    val unread = remember { sampleNotifications().count { it.unread } }

    val title = when {
        tab == StudentTab.HOME ->
            "${greetingForHour(java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY))}, ${firstNameOf(name)} \ud83d\udc4b"

        tab == StudentTab.ATTENDANCE -> scheduleMode.label
        else -> tab.label
    }

    val headerShape = RoundedCornerShape(bottomStart = 36.dp, bottomEnd = 36.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(headerShape)
            .background(
                brush = Brush.verticalGradient(listOf(NavyDeep, Navy)),
                shape = headerShape
            )
            .statusBarsPadding()
            .padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 26.dp)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(190.dp)
                .offset(x = 66.dp, y = (-84).dp)
                .background(Gold.copy(alpha = 0.09f), CircleShape)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "STUDENT PORTAL",
                    color = GoldLight,
                    fontSize = 11.sp,
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(Modifier.height(8.dp))

                AnimatedContent(
                    targetState = title,
                    transitionSpec = {
                        (fadeIn(tween(380)) + slideInVertically(tween(420)) { -it / 4 })
                            .togetherWith(
                                fadeOut(tween(200)) +
                                    androidx.compose.animation.slideOutVertically(tween(300)) { it / 4 }
                            )
                    },
                    label = "headerTitle"
                ) { text ->
                    Text(
                        text = text,
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.4).sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(Modifier.height(4.dp))

                Text(
                    text = todayLabel(),
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(Modifier.width(12.dp))

            NotificationBell(unreadCount = unread, onClick = onBellClick)

            Spacer(Modifier.width(10.dp))

            StudentAvatar(name = name)
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
                .size(44.dp)
                .clip(CircleShape)
                .clickable(onClick = onClick)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Outlined.Notifications,
                    contentDescription = "Notifications",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        if (unreadCount > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(11.dp)
                    .background(PortalRose, CircleShape)
                    .border(2.dp, Navy, CircleShape)
            )
        }
    }
}

@Composable
private fun StudentAvatar(name: String?) {
    val initials = firstNameOf(name).take(1).uppercase()
    Surface(
        shape = CircleShape,
        border = androidx.compose.foundation.BorderStroke(1.5.dp, Gold),
        color = Gold.copy(alpha = 0.18f),
        modifier = Modifier.size(44.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (initials == "S" && firstNameOf(name) == "Student") {
                Icon(
                    Icons.Outlined.Person,
                    contentDescription = null,
                    tint = Gold,
                    modifier = Modifier.size(22.dp)
                )
            } else {
                Text(
                    text = initials,
                    color = Gold,
                    fontSize = 16.sp,
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

@Composable
internal fun PortalErrorState(message: String) {
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
                text = message,
                color = PortalTextSecondary,
                fontSize = 13.sp
            )
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
    val notifications = remember { sampleNotifications() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PortalBackground)
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
                modifier = Modifier.size(20.dp)
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
            itemsIndexed(notifications) { index, item ->
                NotificationRow(index = index, item = item)
            }
        }
    }
}

@Composable
private fun NotificationRow(index: Int, item: PortalNotification) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(index * 55L)
        shown = true
    }

    AnimatedVisibility(
        visible = shown,
        enter = fadeIn(tween(380)) + slideInVertically(tween(440)) { it / 3 }
    ) {
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

        Surface(
            shape = RoundedCornerShape(18.dp),
            color = PortalCard,
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (item.unread) accent.copy(alpha = 0.5f) else PortalStroke
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

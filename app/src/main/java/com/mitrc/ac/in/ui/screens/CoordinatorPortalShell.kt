package com.mitrc.ac.`in`.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
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
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.EventNote
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Class
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.EventNote
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mitrc.ac.`in`.auth.AuthRepository
import com.mitrc.ac.`in`.data.CoordinatorAdminRepository
import com.mitrc.ac.`in`.data.CourseRow
import com.mitrc.ac.`in`.data.EventRow
import com.mitrc.ac.`in`.data.FacultyMasterRow
import com.mitrc.ac.`in`.data.GreetingStore
import com.mitrc.ac.`in`.data.GoogleDriveStorage
import com.mitrc.ac.`in`.data.MyCoordinatorClassView
import com.mitrc.ac.`in`.data.NewEventRow
import com.mitrc.ac.`in`.data.NewNotificationRow
import com.mitrc.ac.`in`.data.NoteInsertRow
import com.mitrc.ac.`in`.data.PortalRepository
import com.mitrc.ac.`in`.ui.theme.DividerSoft
import com.mitrc.ac.`in`.ui.theme.ErrorRed
import com.mitrc.ac.`in`.ui.theme.Gold
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

sealed class CoordinatorTab(val route: String, val label: String, val icon: ImageVector) {
    data object Overview : CoordinatorTab("overview", "Overview", Icons.Outlined.Dashboard)
    data object Students : CoordinatorTab("students", "Students", Icons.Outlined.Group)
    data object Timetable : CoordinatorTab("timetable", "Timetable", Icons.Outlined.CalendarMonth)
    data object Events : CoordinatorTab("events", "Events", Icons.Outlined.Event)
    data object Settings : CoordinatorTab("settings", "Settings", Icons.Outlined.Settings)
}

@Composable
fun CoordinatorPortalShell(
    userUid: String,
    onSignedOut: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableStateOf<CoordinatorTab>(CoordinatorTab.Overview) }

    var showNotifications by remember { mutableStateOf(false) }
    var showProfileSheet by remember { mutableStateOf(false) }
    var showPostNoticeDialog by remember { mutableStateOf(false) }
    var showPostEventDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var signingOut by remember { mutableStateOf(false) }
    var signOutComplete by remember { mutableStateOf(false) }

    // The coordinator's own faculty_master row and the classes they rule. Hoisted into the
    // shell because the hero greeting, the profile sheet and the Settings tab all read it.
    var faculty by remember { mutableStateOf<FacultyMasterRow?>(null) }
    var coordinatorClasses by remember { mutableStateOf<List<MyCoordinatorClassView>>(emptyList()) }

    val refreshProfile: suspend () -> Unit = {
        val email = AuthRepository.currentUser?.email.orEmpty()
        faculty = PortalRepository.getFacultyByUidOrEmail(userUid, email).getOrNull()
        coordinatorClasses = PortalRepository.getMyCoordinatorClasses().getOrDefault(emptyList())
    }

    LaunchedEffect(userUid) { refreshProfile() }

    val coordinatorName = remember(faculty, userUid) {
        faculty?.name?.trim()?.takeIf { it.isNotEmpty() }
            ?: AuthRepository.currentUser?.displayName?.trim()?.takeIf { it.isNotEmpty() }
            ?: AuthRepository.currentUser?.email?.substringBefore('@')
    }

    // Same rule as the student portal: "Welcome" on the first open of the day, "Hi !" after.
    val greeting = remember { GreetingStore.greeting() }
    val headerTitle = when (selectedTab) {
        CoordinatorTab.Overview -> "$greeting, ${firstNameOf(coordinatorName)} \ud83d\udc4b"
        else -> selectedTab.label
    }

    // One accumulated offset for the hero, fed by the nested-scroll connection below. Works
    // for LazyColumn and plain verticalScroll content alike, so every tab folds the same
    // header the student portal folds.
    val foldDistancePx = with(LocalDensity.current) { 96.dp.toPx() }
    val headerOffset = remember { mutableFloatStateOf(0f) }
    val nestedScroll = remember(foldDistancePx) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                if (delta == 0f) return Offset.Zero
                // Dragging up (delta < 0) folds the hero; dragging down (delta > 0) unfolds it
                // before the list starts moving. Once it is fully unfolded the drag passes
                // through, which leaves pull-to-refresh free to take the over-pull.
                val next = (headerOffset.floatValue - delta).coerceIn(0f, foldDistancePx)
                val used = next - headerOffset.floatValue
                if (used == 0f) return Offset.Zero
                headerOffset.floatValue = next
                return Offset(0f, -used)
            }
        }
    }

    LaunchedEffect(selectedTab) { headerOffset.floatValue = 0f }

    BackHandler {
        when {
            showNotifications -> showNotifications = false
            showProfileSheet -> showProfileSheet = false
            showPostNoticeDialog -> showPostNoticeDialog = false
            showPostEventDialog -> showPostEventDialog = false
            showLogoutDialog && !signingOut -> showLogoutDialog = false
        }
    }

    Scaffold(
        bottomBar = {
            CoordinatorBottomBar(
                selected = selectedTab,
                onSelect = { selectedTab = it }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Slate)
                .padding(innerPadding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                PortalHeroHeader(
                    overline = "COORDINATOR PORTAL",
                    title = headerTitle,
                    name = coordinatorName,
                    scrolledPx = { headerOffset.floatValue },
                    onBellClick = { showNotifications = true },
                    onAvatarClick = { showProfileSheet = true }
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .nestedScroll(nestedScroll)
                ) {
                    AnimatedContent(
                        targetState = selectedTab,
                        transitionSpec = {
                            fadeIn(tween(260)).togetherWith(fadeOut(tween(180)))
                        },
                        modifier = Modifier.fillMaxSize(),
                        label = "CoordinatorPortalContent"
                    ) { targetTab ->
                        when (targetTab) {
                            CoordinatorTab.Overview -> {
                                CoordinatorOverviewView(
                                    onRefresh = refreshProfile,
                                    onNavigateStudents = { selectedTab = CoordinatorTab.Students },
                                    onNavigateTimetable = { selectedTab = CoordinatorTab.Timetable }
                                )
                            }
                            CoordinatorTab.Students -> {
                                CoordinatorStudentsView(userUid = userUid)
                            }
                            CoordinatorTab.Timetable -> {
                                CoordinatorTimetableView(userUid = userUid)
                            }
                            CoordinatorTab.Events -> {
                                CoordinatorEventsView(
                                    onRefresh = refreshProfile,
                                    onPostEvent = { showPostEventDialog = true }
                                )
                            }
                            CoordinatorTab.Settings -> {
                                CoordinatorSettingsView(
                                    coordinatorName = coordinatorName,
                                    coordinatorDesignation = faculty?.designation,
                                    coordinatorEmail = faculty?.email
                                        ?: AuthRepository.currentUser?.email,
                                    onRefresh = refreshProfile,
                                    onSignOutRequest = { showLogoutDialog = true }
                                )
                            }
                        }
                    }
                }
            }

            if (showPostNoticeDialog) {
                PostNoticeDialog(
                    onDismiss = { showPostNoticeDialog = false },
                    onPosted = { showPostNoticeDialog = false }
                )
            }

            if (showPostEventDialog) {
                PostEventDialog(
                    onDismiss = { showPostEventDialog = false },
                    onPosted = { showPostEventDialog = false }
                )
            }
            if (showLogoutDialog) {
                LogoutConfirmDialog(
                    signingOut = signingOut,
                    signOutComplete = signOutComplete,
                    onConfirm = {
                        signingOut = true
                        scope.launch {
                            AuthRepository.signOut()
                            delay(250)
                            signOutComplete = true
                        }
                    },
                    onDismiss = { if (!signingOut) showLogoutDialog = false },
                    onExitAnimated = onSignedOut
                )
            }

            PortalSheet(
                visible = showProfileSheet,
                onDismiss = { showProfileSheet = false }
            ) {
                CoordinatorProfileSheet(
                    faculty = faculty,
                    classes = coordinatorClasses,
                    fallbackName = coordinatorName,
                    onClose = { showProfileSheet = false }
                )
            }

            AnimatedVisibility(
                visible = showNotifications,
                enter = fadeIn(tween(240)) + slideInHorizontally(tween(320)) { it },
                exit = fadeOut(tween(180)) + slideOutHorizontally(tween(260)) { it },
                modifier = Modifier.fillMaxSize()
            ) {
                Scaffold(
                    floatingActionButton = {
                        FloatingActionButton(
                            onClick = { showPostNoticeDialog = true },
                            containerColor = Gold,
                            contentColor = NavyDeep,
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Outlined.Campaign, contentDescription = "Post Notice")
                                Text("Post Notice", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                        NotificationsScreen(onBack = { showNotifications = false })
                    }
                }
            }
        }
    }
}

/**
 * Pull-to-refresh wrapper shared by every coordinator tab. Mirrors the student portal exactly:
 * the same gentle [HapticFeedbackType.LongPress] tap on the gesture, and the tab's shimmer
 * skeleton in place of its content for the duration of the refresh.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CoordinatorRefreshBox(
    onRefresh: suspend () -> Unit,
    skeleton: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    var isRefreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            scope.launch {
                isRefreshing = true
                // `finally` so a failed query still drops the skeleton instead of wedging it.
                try {
                    onRefresh()
                } finally {
                    isRefreshing = false
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    ) {
        if (isRefreshing) skeleton() else content()
    }
}

// ---------------------------------------------------------------------------------------------
// Coordinator's own profile sheet - the counterpart of the student profile sheet.
// ---------------------------------------------------------------------------------------------

@Composable
private fun CoordinatorProfileSheet(
    faculty: FacultyMasterRow?,
    classes: List<MyCoordinatorClassView>,
    fallbackName: String?,
    onClose: () -> Unit
) {
    val name = faculty?.name?.trim()?.takeIf { it.isNotEmpty() }
        ?: fallbackName?.trim()?.takeIf { it.isNotEmpty() }
        ?: "Coordinator"

    Column(modifier = Modifier.fillMaxWidth().padding(top = 26.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                border = BorderStroke(2.dp, Gold),
                color = Gold.copy(alpha = 0.16f),
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = name.firstOrNull()?.uppercaseChar()?.toString() ?: "C",
                        color = Navy,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(
                    text = name,
                    color = PortalTextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = faculty?.designation ?: "Course Coordinator",
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
            CoordinatorSheetRow(
                label = "Employee ID",
                value = faculty?.id?.takeIf { it > 0 }?.toString() ?: "Not linked"
            )
            CoordinatorSheetRow(
                label = "Email",
                value = faculty?.email ?: AuthRepository.currentUser?.email ?: "-"
            )
            CoordinatorSheetRow(
                label = "Phone",
                value = faculty?.phoneNo ?: "Not available",
                highlight = faculty?.phoneNo.isNullOrBlank()
            )
            CoordinatorSheetRow(
                label = "Portal role",
                value = "Course Coordinator"
            )
            CoordinatorSheetRow(
                label = "Signed in as",
                value = AuthRepository.currentUser?.email ?: "-"
            )
        }

        if (classes.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(PortalStroke)
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = "COORDINATED CLASSES",
                color = PortalTextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                modifier = Modifier.padding(horizontal = 22.dp)
            )
            Spacer(Modifier.height(8.dp))
            Column(
                modifier = Modifier.padding(horizontal = 22.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                classes.forEach { item ->
                    Text(
                        text = buildString {
                            append(item.course ?: "Class #${item.classId}")
                            if (!item.branch.isNullOrBlank()) append(" · ").append(item.branch)
                            append(" · Sem ").append(item.semester)
                            append(" · Sec ").append(item.section)
                        },
                        color = PortalTextPrimary,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 18.dp)
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
private fun CoordinatorSheetRow(label: String, value: String, highlight: Boolean = false) {
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
            textAlign = TextAlign.End,
            modifier = Modifier.padding(start = 16.dp)
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Bottom navigation - mirrors the student portal's light track / navy pill treatment.
// ---------------------------------------------------------------------------------------------

@Composable
private fun CoordinatorBottomBar(
    selected: CoordinatorTab,
    onSelect: (CoordinatorTab) -> Unit
) {
    val tabs = listOf(
        CoordinatorTab.Overview,
        CoordinatorTab.Students,
        CoordinatorTab.Timetable,
        CoordinatorTab.Events,
        CoordinatorTab.Settings
    )

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
            tabs.forEach { item ->
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

@Composable
fun CoordinatorOverviewView(
    onRefresh: suspend () -> Unit,
    onNavigateStudents: () -> Unit,
    onNavigateTimetable: () -> Unit
) {
    CoordinatorRefreshBox(
        onRefresh = onRefresh,
        skeleton = { CoordinatorOverviewSkeleton() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
        // --- 1. DAILY SUMMARY SNAPSHOT ---
        Text(
            text = "DAILY ACADEMIC SUMMARY",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = TextSecondary,
            letterSpacing = 1.2.sp
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                title = "Total Students",
                value = "426",
                subtext = "Active Cohort",
                icon = Icons.Outlined.Group,
                color = NavySoft,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "Today Present",
                value = "382",
                subtext = "89.6% Attendance",
                icon = Icons.Outlined.CheckCircle,
                color = SuccessGreen,
                modifier = Modifier.weight(1f)
            )
        }

        // Summary Bar Card
        ElevatedCard(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = SurfaceWhite),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Everyday Summary", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text("382 / 426 Present", fontSize = 12.sp, color = PortalBlue, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { 0.896f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape),
                    color = SuccessGreen,
                    trackColor = DividerSoft
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // --- 2. RECENT ACTIVITIES & LEAVE APPLICATIONS ---
        Text(
            text = "RECENT ACTIVITIES & LEAVE REQUESTS",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = TextSecondary,
            letterSpacing = 1.2.sp
        )

        val leaveRequests = remember {
            listOf(
                LeaveItem("Rahul Sharma", "Sem 3 - Sec A", "Medical Leave (12 Oct - 14 Oct)", "Pending"),
                LeaveItem("Priya Verma", "Sem 5 - Sec B", "SIH Hackathon Duty Leave (15 Oct)", "Pending"),
                LeaveItem("Aman Gupta", "Sem 3 - Sec A", "Family Emergency Leave (10 Oct)", "Approved")
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            leaveRequests.forEach { leave ->
                LeaveRequestCard(leave)
            }
        }

        Spacer(Modifier.height(8.dp))

        // --- 3. QUICK ACTIONS GRID ---
        Text(
            text = "QUICK MANAGEMENT ACTIONS",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = TextSecondary,
            letterSpacing = 1.2.sp
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ActionCard(
                title = "Students",
                desc = "Search & PTM",
                icon = Icons.Outlined.Group,
                onClick = onNavigateStudents,
                modifier = Modifier.weight(1f)
            )
            ActionCard(
                title = "Timetable",
                desc = "Edit schedules",
                icon = Icons.Outlined.CalendarMonth,
                onClick = onNavigateTimetable,
                modifier = Modifier.weight(1f)
            )
        }
        }
    }
}

@Composable
private fun LeaveRequestCard(leave: LeaveItem) {
    var status by remember { mutableStateOf(leave.status) }

    ElevatedCard(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Navy.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Outlined.EventNote, contentDescription = null, tint = Navy, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(leave.studentName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text("${leave.section} · ${leave.reason}", fontSize = 12.sp, color = TextSecondary)
            }
            if (status == "Pending") {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(SuccessGreen.copy(alpha = 0.15f))
                            .clickable { status = "Approved" }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("Approve", color = SuccessGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(ErrorRed.copy(alpha = 0.15f))
                            .clickable { status = "Rejected" }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("Reject", color = ErrorRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (status == "Approved") SuccessGreen.copy(alpha = 0.15f) else ErrorRed.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(status.uppercase(), color = if (status == "Approved") SuccessGreen else ErrorRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private data class LeaveItem(
    val studentName: String,
    val section: String,
    val reason: String,
    val status: String
)

@Composable
private fun MetricCard(
    title: String,
    value: String,
    subtext: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(color.copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text(text = value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(Modifier.height(2.dp))
            Text(text = title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
            Text(text = subtext, fontSize = 10.5.sp, color = TextSecondary.copy(alpha = 0.7f))
        }
    }
}

@Composable
private fun ActionCard(
    title: String,
    desc: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(imageVector = icon, contentDescription = null, tint = Navy, modifier = Modifier.size(22.dp))
            Spacer(Modifier.height(8.dp))
            Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text(text = desc, fontSize = 11.sp, color = TextSecondary)
        }
    }
}

@Composable
private fun mitrcFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    disabledTextColor = TextPrimary.copy(alpha = 0.6f),
    cursorColor = Navy,
    focusedBorderColor = Navy,
    unfocusedBorderColor = DividerSoft,
    focusedContainerColor = SurfaceWhite,
    unfocusedContainerColor = SurfaceWhite,
    focusedLabelColor = TextSecondary,
    unfocusedLabelColor = TextSecondary
)

@Composable
private fun CoordinatorEventsView(
    onRefresh: suspend () -> Unit,
    onPostEvent: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var events by remember { mutableStateOf<List<EventRow>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        loading = true
        events = CoordinatorAdminRepository.getEvents().getOrDefault(emptyList())
        loading = false
    }

    CoordinatorRefreshBox(
        onRefresh = {
            scope.launch { onRefresh() }
            events = CoordinatorAdminRepository.getEvents().getOrDefault(emptyList())
        },
        skeleton = { CoordinatorResourcesSkeleton() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("CAMPUS EVENTS & ACTIVITIES", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextSecondary, letterSpacing = 1.2.sp)
                Button(
                    onClick = onPostEvent,
                    colors = ButtonDefaults.buttonColors(containerColor = Navy),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Outlined.Add, null, modifier = Modifier.size(16.dp), tint = Color.White)
                    Spacer(Modifier.width(4.dp))
                    Text("Post Event", fontSize = 12.sp, color = Color.White)
                }
            }

            if (events.isEmpty()) {
                ElevatedCard(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = SurfaceWhite),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No events posted yet.", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Spacer(Modifier.height(6.dp))
                        Text("Tap 'Post Event' above to publish campus events.", fontSize = 12.sp, color = TextSecondary)
                    }
                }
            } else {
                events.forEach { event ->
                    ElevatedCard(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.elevatedCardColors(containerColor = SurfaceWhite),
                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(event.title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Navy)
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Gold.copy(alpha = 0.2f))
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(event.category.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Navy)
                                }
                            }
                            if (!event.description.isNullOrBlank()) {
                                Spacer(Modifier.height(6.dp))
                                Text(event.description, fontSize = 13.sp, color = TextPrimary)
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                if (!event.venue.isNullOrBlank()) {
                                    Text("📍 ${event.venue}", fontSize = 11.5.sp, color = TextSecondary)
                                }
                                Text("📅 ${event.startsAt.take(10)}", fontSize = 11.5.sp, color = TextSecondary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PostEventDialog(
    onDismiss: () -> Unit,
    onPosted: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var venue by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("general") }
    var startsAt by remember { mutableStateOf(getTodayIsoDateTime()) }
    var registrationUrl by remember { mutableStateOf("") }

    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var imageUrl by remember { mutableStateOf<String?>(null) }
    var imageId by remember { mutableStateOf<String?>(null) }

    var posting by remember { mutableStateOf(false) }
    var uploadingImage by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val imageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            imageUri = uri
            uploadingImage = true
            scope.launch {
                GoogleDriveStorage.upload(context, uri, folder = "Events")
                    .fold(
                        onSuccess = { res ->
                            imageUrl = res.url
                            imageId = res.fileId
                            uploadingImage = false
                        },
                        onFailure = { err ->
                            error = "Image upload failed: ${err.message}"
                            uploadingImage = false
                        }
                    )
            }
        }
    }

    val categories = listOf("general", "cultural", "technical", "sports", "academic", "workshop", "seminar", "other")

    AlertDialog(
        onDismissRequest = { if (!posting) onDismiss() },
        title = { Text("Post Campus Event", color = Navy, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Event Title") },
                    singleLine = true,
                    colors = mitrcFieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (Optional)") },
                    minLines = 3,
                    colors = mitrcFieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = venue,
                    onValueChange = { venue = it },
                    label = { Text("Venue (Optional)") },
                    singleLine = true,
                    colors = mitrcFieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = startsAt,
                    onValueChange = { startsAt = it },
                    label = { Text("Starts At (ISO-8601 e.g. 2026-03-01T10:00:00Z)") },
                    singleLine = true,
                    colors = mitrcFieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = registrationUrl,
                    onValueChange = { registrationUrl = it },
                    label = { Text("Registration URL (Optional)") },
                    singleLine = true,
                    colors = mitrcFieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Text("CATEGORY", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    categories.forEach { cat ->
                        NoticeChoiceChip(label = cat, selected = category == cat) { category = cat }
                    }
                }

                Button(
                    onClick = { imageLauncher.launch("image/*") },
                    enabled = !uploadingImage && !posting,
                    colors = ButtonDefaults.buttonColors(containerColor = NavySoft),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Outlined.Image, null, modifier = Modifier.size(16.dp), tint = Color.White)
                    Spacer(Modifier.width(6.dp))
                    Text(if (uploadingImage) "Uploading..." else if (imageUrl != null) "Change Attached Image" else "Attach Event Image", color = Color.White, fontSize = 12.sp)
                }

                if (imageUrl != null) {
                    Text("Image attached successfully!", color = SuccessGreen, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }

                if (error != null) {
                    Text(error.orEmpty(), color = ErrorRed, fontSize = 11.5.sp)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (title.isNotBlank() && startsAt.isNotBlank()) {
                        scope.launch {
                            posting = true
                            error = null
                            val res = CoordinatorAdminRepository.createEvent(
                                NewEventRow(
                                    title = title.trim(),
                                    description = description.trim().takeIf { it.isNotEmpty() },
                                    imageUrl = imageUrl,
                                    imageId = imageId,
                                    category = category,
                                    venue = venue.trim().takeIf { it.isNotEmpty() },
                                    startsAt = startsAt.trim(),
                                    registrationUrl = registrationUrl.trim().takeIf { it.isNotEmpty() }
                                )
                            )
                            posting = false
                            res.fold(
                                onSuccess = { onPosted() },
                                { err -> error = err.message ?: "Failed to post event" }
                            )
                        }
                    }
                },
                enabled = !posting && !uploadingImage && title.isNotBlank()
            ) {
                Text(if (posting) "Posting..." else "Post Event", color = Navy, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !posting) {
                Text("Cancel", color = TextSecondary)
            }
        },
        containerColor = SurfaceWhite
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PostNoticeDialog(
    onDismiss: () -> Unit,
    onPosted: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("announcement") }
    var priority by remember { mutableStateOf("normal") }
    var audience by remember { mutableStateOf("everyone") }

    var courses by remember { mutableStateOf<List<CourseRow>>(emptyList()) }
    var classes by remember { mutableStateOf<List<MyCoordinatorClassView>>(emptyList()) }
    var selectedCourseId by remember { mutableStateOf<Int?>(null) }
    var selectedClassId by remember { mutableStateOf<Int?>(null) }

    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var imageUrl by remember { mutableStateOf<String?>(null) }
    var imageId by remember { mutableStateOf<String?>(null) }

    var posting by remember { mutableStateOf(false) }
    var uploadingImage by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(audience) {
        if (audience == "course" && courses.isEmpty()) {
            courses = CoordinatorAdminRepository.getCourses()
        } else if (audience == "class" && classes.isEmpty()) {
            classes = CoordinatorAdminRepository.getAllClassesWithDetails().getOrDefault(emptyList())
        }
    }

    val imageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            imageUri = uri
            uploadingImage = true
            scope.launch {
                GoogleDriveStorage.upload(context, uri, folder = "Notifications")
                    .fold(
                        onSuccess = { res ->
                            imageUrl = res.url
                            imageId = res.fileId
                            uploadingImage = false
                        },
                        onFailure = { err ->
                            error = "Image upload failed: ${err.message}"
                            uploadingImage = false
                        }
                    )
            }
        }
    }

    val categories = listOf("announcement", "exam", "holiday", "event", "timetable", "notes", "attendance", "marks", "alert", "system")
    val priorities = listOf("normal", "high", "urgent")
    val audiences = listOf("everyone", "all_students", "all_staff", "course", "class", "group", "class_subject")

    AlertDialog(
        onDismissRequest = { if (!posting) onDismiss() },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Campaign, null, tint = Navy, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(8.dp))
                Text("Broadcast Official Notice", color = Navy, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Notice Title") },
                    singleLine = true,
                    colors = mitrcFieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = body,
                    onValueChange = { body = it },
                    label = { Text("Notice Body / Message") },
                    minLines = 3,
                    colors = mitrcFieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Text("CATEGORY", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    categories.forEach { cat ->
                        NoticeChoiceChip(label = cat, selected = category == cat) { category = cat }
                    }
                }

                Text("PRIORITY", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    priorities.forEach { pri ->
                        NoticeChoiceChip(label = pri, selected = priority == pri) { priority = pri }
                    }
                }

                Text("AUDIENCE TARGET", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    audiences.forEach { aud ->
                        NoticeChoiceChip(label = aud, selected = audience == aud) { audience = aud }
                    }
                }

                if (audience == "course" && courses.isNotEmpty()) {
                    Text("SELECT COURSE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        courses.forEach { course ->
                            NoticeChoiceChip(label = course.name, selected = selectedCourseId == course.id) {
                                selectedCourseId = course.id
                            }
                        }
                    }
                }

                if (audience == "class" && classes.isNotEmpty()) {
                    Text("SELECT CLASS / BATCH", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        classes.forEach { cls ->
                            val label = "${cls.course ?: ""} Sem ${cls.semester} ${cls.section}"
                            NoticeChoiceChip(label = label, selected = selectedClassId == cls.classId) {
                                selectedClassId = cls.classId
                            }
                        }
                    }
                }

                Button(
                    onClick = { imageLauncher.launch("image/*") },
                    enabled = !uploadingImage && !posting,
                    colors = ButtonDefaults.buttonColors(containerColor = NavySoft),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Outlined.Image, null, modifier = Modifier.size(16.dp), tint = Color.White)
                    Spacer(Modifier.width(6.dp))
                    Text(if (uploadingImage) "Uploading..." else if (imageUrl != null) "Change Attached Image" else "Attach Notice Image", color = Color.White, fontSize = 12.sp)
                }

                if (imageUrl != null) {
                    Text("Image attached successfully!", color = SuccessGreen, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }

                if (error != null) {
                    Text(error.orEmpty(), color = ErrorRed, fontSize = 11.5.sp)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (title.isNotBlank()) {
                        scope.launch {
                            posting = true
                            error = null
                            val res = CoordinatorAdminRepository.createNotification(
                                NewNotificationRow(
                                    title = title.trim(),
                                    body = body.trim().takeIf { it.isNotEmpty() },
                                    imageUrl = imageUrl,
                                    imageId = imageId,
                                    category = category,
                                    priority = priority,
                                    audience = audience,
                                    courseId = selectedCourseId,
                                    classId = selectedClassId
                                )
                            )
                            posting = false
                            res.fold(
                                onSuccess = { onPosted() },
                                { err -> error = err.message ?: "Failed to post notification" }
                            )
                        }
                    }
                },
                enabled = !posting && !uploadingImage && title.isNotBlank()
            ) {
                Text(if (posting) "Broadcasting..." else "Broadcast Notice", color = Navy, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !posting) {
                Text("Cancel", color = TextSecondary)
            }
        },
        containerColor = SurfaceWhite
    )
}

@Composable
private fun NoticeChoiceChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(9.dp))
            .background(if (selected) Navy else Slate)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            color = if (selected) Color.White else TextPrimary,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

private fun getTodayIsoDateTime(): String {
    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US)
    sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
    return sdf.format(java.util.Date())
}

@Composable
private fun CoordinatorSettingsView(
    coordinatorName: String?,
    coordinatorDesignation: String?,
    coordinatorEmail: String?,
    onRefresh: suspend () -> Unit,
    onSignOutRequest: () -> Unit
) {
    var updateStatusText by remember { mutableStateOf<String?>(null) }
    val displayName = coordinatorName?.trim()?.takeIf { it.isNotEmpty() } ?: "Coordinator"

    CoordinatorRefreshBox(
        onRefresh = onRefresh,
        skeleton = { CoordinatorSettingsSkeleton() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ElevatedCard(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = SurfaceWhite),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .border(2.dp, Gold, CircleShape)
                                .background(Gold.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = displayName.firstOrNull()?.uppercaseChar()?.toString() ?: "C",
                                color = Navy,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column {
                            Text(displayName, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text(
                                text = coordinatorDesignation?.takeIf { it.isNotBlank() }
                                    ?: "Academic Management Desk",
                                fontSize = 12.5.sp,
                                color = Navy,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (!coordinatorEmail.isNullOrBlank()) {
                                Text(
                                    text = coordinatorEmail,
                                    fontSize = 11.5.sp,
                                    color = TextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            Text("SETTINGS & SERVICES", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextSecondary, letterSpacing = 1.2.sp)

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                CoordinatorSettingsCard("Check for Update", updateStatusText ?: "Check latest app release", Icons.Outlined.Refresh) {
                    updateStatusText = "App is up to date (v2.4.1)"
                }
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = onSignOutRequest,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ErrorRed, contentColor = Color.White),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Icon(Icons.AutoMirrored.Outlined.Logout, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Sign Out", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = "\u00a9 2026 MITRC Alwar. All rights reserved.",
                color = TextSecondary,
                fontSize = 11.5.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun CoordinatorSettingsCard(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit) {
    ElevatedCard(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Navy, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(subtitle, color = TextSecondary, fontSize = 11.5.sp)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = TextSecondary, modifier = Modifier.size(20.dp))
        }
    }
}



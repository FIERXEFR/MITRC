package com.mitrc.ac.`in`.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.FactCheck
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.mitrc.ac.`in`.auth.AuthRepository
import com.mitrc.ac.`in`.data.ClassSubjectView
import com.mitrc.ac.`in`.data.FacultyMasterRow
import com.mitrc.ac.`in`.data.GreetingStore
import com.mitrc.ac.`in`.data.PortalRepository
import com.mitrc.ac.`in`.data.TeacherClassInfo
import com.mitrc.ac.`in`.ui.theme.DividerSoft
import com.mitrc.ac.`in`.ui.theme.Gold
import com.mitrc.ac.`in`.ui.theme.NavyDeep
import com.mitrc.ac.`in`.ui.theme.Slate
import com.mitrc.ac.`in`.ui.theme.SurfaceWhite
import kotlinx.coroutines.launch

internal enum class TeacherTab(
    val label: String,
    val icon: ImageVector,
) {
    Overview("Overview", Icons.Outlined.Dashboard),
    Classroom("Classroom", Icons.Outlined.FactCheck),
    Notices("Notices", Icons.Outlined.Campaign),
    Settings("Settings", Icons.Outlined.Settings),
}

@Composable
fun TeacherPortalShell(
    userUid: String,
    onSignedOut: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var selectedTabName by rememberSaveable { mutableStateOf(TeacherTab.Overview.name) }
    val selectedTab = TeacherTab.entries.firstOrNull { it.name == selectedTabName } ?: TeacherTab.Overview

    var classroomModeName by rememberSaveable { mutableStateOf(TeacherWorkMode.ATTENDANCE.name) }
    val classroomMode = TeacherWorkMode.entries
        .firstOrNull { it.name == classroomModeName }
        ?: TeacherWorkMode.ATTENDANCE

    var loading by remember { mutableStateOf(true) }
    var teacher by remember { mutableStateOf<FacultyMasterRow?>(null) }
    var subjects by remember { mutableStateOf<List<ClassSubjectView>>(emptyList()) }
    var classInfo by remember { mutableStateOf<Map<Int, TeacherClassInfo>>(emptyMap()) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var reloadTick by remember { mutableIntStateOf(0) }
    var selectedClassSubjectId by rememberSaveable { mutableIntStateOf(0) }

    LaunchedEffect(userUid, reloadTick) {
        loading = true
        loadError = null

        val email = FirebaseAuth.getInstance().currentUser?.email.orEmpty()
        val profile = PortalRepository.getTeacherProfile(userUid).getOrNull()
            ?: PortalRepository.getFacultyByUidOrEmail(userUid, email).getOrNull()

        if (profile == null) {
            teacher = null
            subjects = emptyList()
            classInfo = emptyMap()
            loadError = "No faculty profile is linked to this account. Ask the admin to link your login to your faculty record."
            loading = false
            return@LaunchedEffect
        }

        teacher = profile
        val assignmentResult = PortalRepository.getTeacherSubjects(profile.id)
        val assignments = assignmentResult
            .getOrDefault(emptyList())
            .filter { it.classSubjectId > 0 && it.classId > 0 }
            .distinctBy { it.classSubjectId }

        subjects = assignments
        classInfo = if (assignments.isEmpty()) {
            emptyMap()
        } else {
            PortalRepository.getClassInfo(assignments.map { it.classId })
                .getOrDefault(emptyMap())
        }

        if (assignmentResult.isFailure) {
            loadError = assignmentResult.exceptionOrNull()?.message
                ?.ifBlank { "Your class assignments could not be loaded." }
                ?: "Your class assignments could not be loaded."
        } else if (assignments.isNotEmpty() && assignments.none { it.classSubjectId == selectedClassSubjectId }) {
            selectedClassSubjectId = assignments.first().classSubjectId
        }

        loading = false
    }

    val selectedSubject = subjects.firstOrNull { it.classSubjectId == selectedClassSubjectId }
        ?: subjects.firstOrNull()
    val profile = teacher
    val greeting = remember { GreetingStore.greeting() }

    // Same fold gesture as the student portal: the hero collapses through nested scroll instead
    // of mirroring list offsets, so the content never double-hitches at the end of a scroll.
    val foldDistancePx = with(LocalDensity.current) { 96.dp.toPx() }
    val headerOffset = remember { mutableFloatStateOf(0f) }
    val headerNestedScroll = remember(foldDistancePx) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                if (delta == 0f) return Offset.Zero
                val next = (headerOffset.floatValue - delta).coerceIn(0f, foldDistancePx)
                val used = next - headerOffset.floatValue
                if (used == 0f) return Offset.Zero
                headerOffset.floatValue = next
                return Offset(0f, -used)
            }
        }
    }
    LaunchedEffect(selectedTab, classroomModeName) { headerOffset.floatValue = 0f }

    val headerTitle = when {
        loading -> ""
        selectedTab == TeacherTab.Overview -> profile?.name
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.let { "$greeting, ${firstNameOf(it)} \ud83d\udc4b" }
            ?: "Teacher portal"
        else -> selectedTab.label
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            PortalHeroHeader(
                overline = "TEACHER PORTAL",
                title = headerTitle,
                name = profile?.name,
                scrolledPx = { headerOffset.floatValue },
                showAvatar = selectedTab != TeacherTab.Settings,
                unreadCount = 0,
                onBellClick = { selectedTabName = TeacherTab.Notices.name },
                onAvatarClick = { selectedTabName = TeacherTab.Settings.name },
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .nestedScroll(headerNestedScroll),
            ) {
                when {
                    loading -> TeacherPortalSkeleton()

                    profile == null -> TeacherProfileUnavailable(
                        message = loadError ?: "Your faculty workspace is not available right now.",
                        onRetry = { reloadTick++ },
                        onSignOut = {
                            scope.launch {
                                AuthRepository.signOut()
                                onSignedOut()
                            }
                        },
                    )

                    subjects.isEmpty() -> TeacherNoAssignments(
                        message = loadError,
                        onRetry = { reloadTick++ },
                        onSignOut = {
                            scope.launch {
                                AuthRepository.signOut()
                                onSignedOut()
                            }
                        },
                    )

                    else -> AnimatedContent(
                        targetState = selectedTab,
                        transitionSpec = {
                            val direction = if (targetState.ordinal >= initialState.ordinal) 1 else -1
                            (slideInHorizontally(
                                animationSpec = tween(240, easing = FastOutSlowInEasing),
                                initialOffsetX = { direction * it / 8 },
                            ) + fadeIn(tween(210))) togetherWith
                                (slideOutHorizontally(
                                    animationSpec = tween(190, easing = FastOutSlowInEasing),
                                    targetOffsetX = { -direction * it / 10 },
                                ) + fadeOut(tween(150)))
                        },
                        label = "teacherTab",
                        modifier = Modifier.fillMaxSize(),
                    ) { tab ->
                        when (tab) {
                            TeacherTab.Overview -> TeacherOverviewView(
                                teacher = profile,
                                subjects = subjects,
                                selectedSubject = selectedSubject,
                                classInfo = classInfo,
                                onSelectSubject = { selectedClassSubjectId = it.classSubjectId },
                                onOpenClassroom = {
                                    classroomModeName = TeacherWorkMode.ATTENDANCE.name
                                    selectedTabName = TeacherTab.Classroom.name
                                },
                                onOpenMaterials = {
                                    classroomModeName = TeacherWorkMode.MATERIALS.name
                                    selectedTabName = TeacherTab.Classroom.name
                                },
                                onOpenNotices = { selectedTabName = TeacherTab.Notices.name },
                                onSignOut = {
                                    scope.launch {
                                        AuthRepository.signOut()
                                        onSignedOut()
                                    }
                                },
                            )

                            TeacherTab.Classroom -> TeacherClassroomView(
                                subjects = subjects,
                                selectedSubject = selectedSubject,
                                classInfo = classInfo,
                                onSelectSubject = { selectedClassSubjectId = it.classSubjectId },
                                activeMode = classroomMode,
                                onModeChange = { classroomModeName = it.name },
                            )

                            TeacherTab.Notices -> TeacherNoticesView(
                                subjects = subjects,
                                selectedSubject = selectedSubject,
                                classInfo = classInfo,
                                onSelectSubject = { selectedClassSubjectId = it.classSubjectId },
                            )

                            TeacherTab.Settings -> TeacherSettingsView(
                                teacher = profile,
                                subjects = subjects,
                                selectedSubject = selectedSubject,
                                classInfo = classInfo,
                                onSelectSubject = { selectedClassSubjectId = it.classSubjectId },
                                onSignOut = {
                                    scope.launch {
                                        AuthRepository.signOut()
                                        onSignedOut()
                                    }
                                },
                            )
                        }
                    }
                }
            }

            PortalBottomBar(
                items = TeacherTab.entries.map { PortalNavItem(label = it.label, icon = it.icon) },
                selectedIndex = TeacherTab.entries.indexOf(selectedTab),
                onSelect = { index -> selectedTabName = TeacherTab.entries[index].name },
            )
        }
    }
}

@Composable
private fun TeacherPortalSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Box(
                Modifier
                    .width(74.dp)
                    .height(10.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .shimmerEffect(),
            )
            Box(
                Modifier
                    .width(168.dp)
                    .height(22.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .shimmerEffect(),
            )
            Box(
                Modifier
                    .width(236.dp)
                    .height(12.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .shimmerEffect(),
            )
        }

        Surface(
            shape = RoundedCornerShape(24.dp),
            color = NavyDeep,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(Gold.copy(alpha = 0.18f)),
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        Box(
                            Modifier
                                .width(88.dp)
                                .height(9.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.White.copy(alpha = 0.16f)),
                        )
                        Box(
                            Modifier
                                .width(160.dp)
                                .height(18.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(Color.White.copy(alpha = 0.2f)),
                        )
                        Box(
                            Modifier
                                .width(210.dp)
                                .height(10.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.White.copy(alpha = 0.12f)),
                        )
                    }
                }
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color.White.copy(alpha = 0.1f)),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(alpha = 0.1f)),
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Box(
                            Modifier
                                .width(74.dp)
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.White.copy(alpha = 0.13f)),
                        )
                        Box(
                            Modifier
                                .fillMaxWidth(0.78f)
                                .height(12.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.White.copy(alpha = 0.18f)),
                        )
                    }
                    Box(
                        Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.1f)),
                    )
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(3) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = SurfaceWhite,
                    border = BorderStroke(1.dp, DividerSoft),
                    modifier = Modifier.weight(1f),
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        Box(
                            Modifier
                                .width(30.dp)
                                .height(20.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .shimmerEffect(),
                        )
                        Box(
                            Modifier
                                .fillMaxWidth(0.72f)
                                .height(10.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .shimmerEffect(),
                        )
                        Box(
                            Modifier
                                .fillMaxWidth(0.5f)
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .shimmerEffect(),
                        )
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
            repeat(3) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = SurfaceWhite,
                    border = BorderStroke(1.dp, DividerSoft),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .shimmerEffect(),
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Box(
                                Modifier
                                    .width(138.dp)
                                    .height(12.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .shimmerEffect(),
                            )
                            Box(
                                Modifier
                                    .fillMaxWidth(0.68f)
                                    .height(9.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .shimmerEffect(),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TeacherProfileUnavailable(
    message: String,
    onRetry: () -> Unit,
    onSignOut: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TeacherEmptyState(
                icon = Icons.Outlined.Person,
                title = "Faculty profile not linked",
                body = message,
                actionLabel = "Try again",
                onAction = onRetry,
            )
            TeacherPrimaryButton(
                label = "Sign out",
                icon = Icons.AutoMirrored.Outlined.Logout,
                onClick = onSignOut,
            )
        }
    }
}

@Composable
private fun TeacherNoAssignments(
    message: String?,
    onRetry: () -> Unit,
    onSignOut: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            TeacherEmptyState(
                icon = Icons.Outlined.FactCheck,
                title = "No classes assigned",
                body = message?.takeIf { it.isNotBlank() }
                    ?: "Your faculty account is active, but no class or subject is assigned yet. Ask the admin to add your teaching assignment.",
                actionLabel = "Refresh",
                onAction = onRetry,
            )
            TeacherPrimaryButton(
                label = "Sign out",
                icon = Icons.AutoMirrored.Outlined.Logout,
                onClick = onSignOut,
            )
        }
    }
}

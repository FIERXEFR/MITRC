package com.mitrc.ac.`in`.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mitrc.ac.`in`.auth.AdminDashboardStats
import com.mitrc.ac.`in`.auth.AdminRepository
import com.mitrc.ac.`in`.auth.AuthRepository
import com.mitrc.ac.`in`.data.AppUpdateConfig
import com.mitrc.ac.`in`.data.AppUpdateRepository
import com.mitrc.ac.`in`.data.StaffDirectoryView
import com.mitrc.ac.`in`.data.StudentProfileView
import com.mitrc.ac.`in`.ui.theme.DividerSoft
import com.mitrc.ac.`in`.ui.theme.ErrorRed
import com.mitrc.ac.`in`.ui.theme.Gold
import com.mitrc.ac.`in`.ui.theme.GoldLight
import com.mitrc.ac.`in`.ui.theme.Navy
import com.mitrc.ac.`in`.ui.theme.NavyDeep
import com.mitrc.ac.`in`.ui.theme.SuccessGreen
import com.mitrc.ac.`in`.ui.theme.SurfaceWhite
import com.mitrc.ac.`in`.ui.theme.TextPrimary
import com.mitrc.ac.`in`.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter


@Composable
private fun SkeletonBlock(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(6.dp)
) {
    Box(modifier = modifier.clip(shape).shimmerEffect())
}

@Composable
private fun SkeletonCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(18.dp),
    content: @Composable () -> Unit
) {
    Surface(
        shape = shape,
        color = SurfaceWhite,
        shadowElevation = 1.dp,
        modifier = modifier
    ) {
        content()
    }
}

@Composable
internal fun AdminDashboardSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SkeletonBlock(Modifier.width(160.dp).height(12.dp))

        repeat(3) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                repeat(2) {
                    SkeletonCard(modifier = Modifier.weight(1f).height(94.dp)) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SkeletonBlock(Modifier.size(32.dp), RoundedCornerShape(10.dp))
                            SkeletonBlock(Modifier.width(48.dp).height(20.dp))
                            SkeletonBlock(Modifier.width(68.dp).height(10.dp))
                        }
                    }
                }
            }
        }

        SkeletonBlock(Modifier.width(140.dp).height(12.dp))

        SkeletonCard(modifier = Modifier.fillMaxWidth().height(140.dp)) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                repeat(3) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SkeletonBlock(Modifier.size(18.dp), CircleShape)
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            SkeletonBlock(Modifier.width(120.dp).height(12.dp))
                            SkeletonBlock(Modifier.width(180.dp).height(10.dp))
                        }
                        SkeletonBlock(Modifier.width(36.dp).height(12.dp))
                    }
                }
            }
        }
    }
}

@Composable
internal fun AdminSearchSkeleton() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        repeat(5) {
            SkeletonCard(modifier = Modifier.fillMaxWidth().height(78.dp)) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SkeletonBlock(Modifier.size(38.dp), CircleShape)
                    Spacer(Modifier.width(10.dp))
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SkeletonBlock(Modifier.width(140.dp).height(14.dp))
                        SkeletonBlock(Modifier.width(200.dp).height(11.dp))
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AdminDashboardTab(
    adminName: String?,
    onOpenAppManagement: () -> Unit,
    onOpenSearch: () -> Unit,
    onRegister: (AdminEntryTab) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    var stats by remember { mutableStateOf<AdminDashboardStats?>(null) }
    var statsLoading by remember { mutableStateOf(true) }
    var statsError by remember { mutableStateOf<String?>(null) }
    var config by remember { mutableStateOf<AppUpdateConfig?>(null) }
    var isRefreshing by remember { mutableStateOf(false) }

    suspend fun load() {
        statsLoading = true
        statsError = null
        AdminRepository.getDashboardStats()
            .onSuccess { stats = it }
            .onFailure { statsError = it.message ?: "Could not load campus insights" }
        config = AppUpdateRepository.fetchConfig().getOrNull()
        statsLoading = false
    }

    LaunchedEffect(Unit) { load() }

    val installedVersion = AppUpdateRepository.currentVersionName(context)
    val maintenanceOn = config?.maintenance?.isMaintenance == true
    val releaseStatus = config?.update?.appStatus?.trim()?.lowercase().orEmpty()
    val releasePublished = releaseStatus !in setOf("inactive", "disabled", "draft", "off")

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            scope.launch {
                isRefreshing = true
                try { load() } finally { isRefreshing = false }
            }
        },
        modifier = Modifier.fillMaxSize()
    ) {
        if (statsLoading || isRefreshing) {
            AdminDashboardSkeleton()
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (statsError != null) {
                    Surface(
                        color = ErrorRed.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = statsError.orEmpty(),
                            color = ErrorRed,
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                AdminSectionLabel("INSIGHTS")

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard("Students", stats?.students, Icons.Outlined.School, Navy, Modifier.weight(1f))
                    StatCard("Faculty", stats?.staff, Icons.Outlined.Groups, Color(0xFF0F766E), Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard("Classes", stats?.classes, Icons.Outlined.MenuBook, Color(0xFF7C3AED), Modifier.weight(1f))
                    StatCard("Courses", stats?.courses, Icons.Outlined.AccountTree, Color(0xFFB45309), Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard("Branches", stats?.branches, Icons.Outlined.Badge, Color(0xFF1D4ED8), Modifier.weight(1f))
                    StatCard("Admins", stats?.admins, Icons.Outlined.AdminPanelSettings, Color(0xFFBE123C), Modifier.weight(1f))
                }

                AdminSectionLabel("SYSTEM STATUS")

                Surface(
                    color = SurfaceWhite,
                    shape = RoundedCornerShape(20.dp),
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        StatusRow(
                            icon = Icons.Outlined.SystemUpdate,
                            title = "Maintenance mode",
                            detail = if (maintenanceOn) "Users are blocked right now" else "The app is available to everyone",
                            value = if (maintenanceOn) "ON" else "OFF",
                            valueColor = if (maintenanceOn) ErrorRed else SuccessGreen
                        )
                        StatusDivider()
                        StatusRow(
                            icon = Icons.Outlined.VerifiedUser,
                            title = "Release channel",
                            detail = if (releasePublished) "Update checks are active" else "Release row is unpublished",
                            value = if (releasePublished) "LIVE" else "OFF",
                            valueColor = if (releasePublished) SuccessGreen else TextSecondary
                        )
                        StatusDivider()
                        StatusRow(
                            icon = Icons.Outlined.Badge,
                            title = "Installed build",
                            detail = "This device / admin panel",
                            value = "v$installedVersion",
                            valueColor = TextPrimary
                        )
                        Spacer(Modifier.height(6.dp))
                        TextButton(onClick = onOpenAppManagement) {
                            Text("Manage updates, maintenance & notifications", color = Navy, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                AdminSectionLabel("QUICK ACTIONS")

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ActionCard(
                        title = "Register Student",
                        subtitle = "Create a student account",
                        icon = Icons.Outlined.PersonAdd,
                        onClick = { onRegister(AdminEntryTab.STUDENT) },
                        modifier = Modifier.weight(1f)
                    )
                    ActionCard(
                        title = "Register Staff",
                        subtitle = "Create a faculty account",
                        icon = Icons.Outlined.Groups,
                        onClick = { onRegister(AdminEntryTab.STAFF) },
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ActionCard(
                        title = "Search Users",
                        subtitle = "Students & staff",
                        icon = Icons.Outlined.Search,
                        onClick = onOpenSearch,
                        modifier = Modifier.weight(1f)
                    )
                    ActionCard(
                        title = "Control Panel",
                        subtitle = "Updates, maintenance & notices",
                        icon = Icons.Outlined.Tune,
                        onClick = onOpenAppManagement,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(6.dp))
            }
        }
    }
}

@Composable
private fun StatCard(
    label: String,
    value: Long?,
    icon: ImageVector,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = SurfaceWhite,
        shape = RoundedCornerShape(18.dp),
        shadowElevation = 1.dp,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(accent.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(17.dp))
            }
            Text(
                text = value?.toString() ?: "—",
                color = TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = label,
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun ActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = SurfaceWhite,
        shape = RoundedCornerShape(18.dp),
        shadowElevation = 1.dp,
        onClick = onClick,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(Navy, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = GoldLight, modifier = Modifier.size(18.dp))
            }
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                color = TextSecondary,
                fontSize = 11.sp,
                lineHeight = 15.sp
            )
        }
    }
}

@Composable
private fun StatusRow(
    icon: ImageVector,
    title: String,
    detail: String,
    value: String,
    valueColor: Color,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Navy, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(detail, color = TextSecondary, fontSize = 11.sp, lineHeight = 15.sp)
        }
        Text(
            text = value,
            color = valueColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@Composable
private fun StatusDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
            .height(1.dp)
            .background(DividerSoft)
    )
}

@Composable
private fun AdminSectionLabel(text: String) {
    Text(
        text = text,
        color = TextSecondary,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.2.sp,
        modifier = Modifier.padding(top = 2.dp)
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AdminSearchTab(
    onRegister: (AdminEntryTab) -> Unit
) {
    var isStaff by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var students by remember { mutableStateOf<List<StudentProfileView>>(emptyList()) }
    var staff by remember { mutableStateOf<List<StaffDirectoryView>>(emptyList()) }

    var recentStudents by remember { mutableStateOf<List<StudentProfileView>>(emptyList()) }
    var recentStaff by remember { mutableStateOf<List<StaffDirectoryView>>(emptyList()) }
    var recentLoading by remember { mutableStateOf(true) }
    var recentStudentsError by remember { mutableStateOf<String?>(null) }
    var recentStaffError by remember { mutableStateOf<String?>(null) }
    var isRefreshing by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    suspend fun reloadRecent() {
        recentLoading = true
        recentStudentsError = null
        recentStaffError = null

        val studentsResult = AdminRepository.getRecentStudents()
        val staffResult = AdminRepository.getRecentStaff()

        recentStudents = studentsResult.getOrDefault(emptyList())
        recentStaff = staffResult.getOrDefault(emptyList())
        recentStudentsError = studentsResult.exceptionOrNull()?.message
            ?.takeIf { it.isNotBlank() }
        recentStaffError = staffResult.exceptionOrNull()?.message
            ?.takeIf { it.isNotBlank() }
        recentLoading = false
    }

    LaunchedEffect(Unit) { reloadRecent() }

    LaunchedEffect(query, isStaff) {
        val term = query.trim()
        error = null
        if (term.length < 2) {
            students = emptyList()
            staff = emptyList()
            loading = false
            return@LaunchedEffect
        }

        loading = true
        delay(300)
        if (isStaff) {
            AdminRepository.searchStaff(term).fold(
                onSuccess = { staff = it },
                onFailure = { error = it.message ?: "Staff search failed"; staff = emptyList() }
            )
        } else {
            AdminRepository.searchStudents(term).fold(
                onSuccess = { students = it },
                onFailure = { error = it.message ?: "Student search failed"; students = emptyList() }
            )
        }
        loading = false
    }

    val resultCount = if (isStaff) staff.size else students.size

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            scope.launch {
                isRefreshing = true
                try { reloadRecent() } finally { isRefreshing = false }
            }
        },
        modifier = Modifier.fillMaxSize()
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PortalSegmentedTabs(
                    labels = listOf("Student", "Staff"),
                    selectedIndex = if (isStaff) 1 else 0,
                    onSelect = { isStaff = it == 1 }
                )

                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    placeholder = {
                        Text(if (isStaff) "Name, email or designation" else "Name, roll number or email")
                    },
                    leadingIcon = {
                        Icon(Icons.Outlined.Search, contentDescription = null, tint = TextSecondary)
                    },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(Icons.Outlined.Close, contentDescription = "Clear search", tint = TextSecondary)
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = Navy,
                        focusedBorderColor = Navy,
                        unfocusedBorderColor = DividerSoft,
                        focusedContainerColor = SurfaceWhite,
                        unfocusedContainerColor = SurfaceWhite,
                        focusedLeadingIconColor = Navy,
                        unfocusedLeadingIconColor = TextSecondary,
                        focusedTrailingIconColor = TextSecondary,
                        unfocusedTrailingIconColor = TextSecondary,
                        focusedPlaceholderColor = TextSecondary.copy(alpha = 0.6f),
                        unfocusedPlaceholderColor = TextSecondary.copy(alpha = 0.6f)
                    )
                )

                when {
                    loading || isRefreshing -> {
                        AdminSearchSkeleton()
                    }

                    query.isBlank() -> {
                        RecentLanding(
                            isStaff = isStaff,
                            students = recentStudents,
                            staff = recentStaff,
                            loading = recentLoading || isRefreshing,
                            error = if (isStaff) recentStaffError else recentStudentsError,
                        )
                    }

                    error != null -> {
                        Text(
                            text = error.orEmpty(),
                            color = ErrorRed,
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        )
                    }

                    query.trim().length < 2 -> {
                        SearchEmptyState(
                            title = "Search the campus directory",
                            detail = if (isStaff) {
                                "Type at least 2 characters of a staff name, email or designation."
                            } else {
                                "Type at least 2 characters of a student name, roll number or email."
                            }
                        )
                    }

                    resultCount == 0 -> {
                        SearchEmptyState(
                            title = "No matches found",
                            detail = "Nothing in the directory matches \"${query.trim()}\"."
                        )
                    }

                    else -> {
                        Text(
                            text = "$resultCount ${if (isStaff) "staff" else "student"}${if (resultCount == 1) "" else "s"} found",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (isStaff) {
                                items(staff, key = { it.facultyId }) { person -> StaffResultCard(person) }
                            } else {
                                items(students, key = { it.studentUid }) { student -> StudentResultCard(student) }
                            }
                            item { Spacer(Modifier.height(6.dp)) }
                        }
                    }
                }
            }

            FloatingActionButton(
                onClick = { onRegister(if (isStaff) AdminEntryTab.STAFF else AdminEntryTab.STUDENT) },
                containerColor = Gold,
                contentColor = NavyDeep,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = "Register", modifier = Modifier.size(20.dp))
                    Text(
                        text = if (isStaff) "Register Staff" else "Register Student",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun RecentLanding(
    isStaff: Boolean,
    students: List<StudentProfileView>,
    staff: List<StaffDirectoryView>,
    loading: Boolean,
    error: String?,
) {
    when {
        loading -> {
            AdminSearchSkeleton()
        }

        error != null -> {
            Text(
                text = error,
                color = ErrorRed,
                fontSize = 12.sp,
                lineHeight = 17.sp
            )
        }

        isStaff && staff.isEmpty() -> {
            SearchEmptyState(
                title = "No staff yet",
                detail = "Faculty accounts appear here as soon as they are created."
            )
        }

        !isStaff && students.isEmpty() -> {
            SearchEmptyState(
                title = "No students yet",
                detail = "Student accounts appear here as soon as they are created."
            )
        }

        else -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AdminSectionLabel("RECENTLY ADDED")
                Spacer(Modifier.weight(1f))
                Text(
                    text = "Newest first",
                    color = TextSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (isStaff) {
                    items(staff, key = { it.facultyId }) { person ->
                        StaffResultCard(person, showAddedAt = true)
                    }
                } else {
                    items(students, key = { it.studentUid }) { student ->
                        StudentResultCard(student, showAddedAt = true)
                    }
                }
                item { Spacer(Modifier.height(6.dp)) }
            }
        }
    }
}

@Composable
private fun SearchEmptyState(title: String, detail: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 40.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(Navy.copy(alpha = 0.08f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Search, contentDescription = null, tint = Navy, modifier = Modifier.size(26.dp))
            }
            Spacer(Modifier.height(14.dp))
            Text(title, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(
                text = detail,
                color = TextSecondary,
                fontSize = 12.sp,
                lineHeight = 17.sp
            )
        }
    }
}

@Composable
private fun StudentResultCard(
    student: StudentProfileView,
    showAddedAt: Boolean = false,
) {
    val classLine = listOfNotNull(
        student.course?.takeIf { it.isNotBlank() },
        student.branch?.takeIf { it.isNotBlank() },
        student.semester?.let { "Sem $it" },
        student.section?.takeIf { it.isNotBlank() }?.let { "Sec $it" },
    ).joinToString(" • ")

    Surface(
        color = SurfaceWhite,
        shape = RoundedCornerShape(18.dp),
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(Navy.copy(alpha = 0.10f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initialsOf(student.name),
                        color = Navy,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = student.name?.trim()?.takeIf { it.isNotEmpty() } ?: "Unnamed student",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = listOfNotNull(
                            student.serialNo?.takeIf { it.isNotBlank() }?.let { "Roll $it" },
                            classLine.takeIf { it.isNotEmpty() },
                        ).joinToString(" • ").ifEmpty { "Student" },
                        color = TextSecondary,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (student.isLocked == true) {
                    ResultBadge("LOCKED", ErrorRed)
                }
            }

            student.studentEmail?.takeIf { it.isNotBlank() }?.let { DetailRow(Icons.Outlined.Email, it) }
            student.studentPhoneNo?.takeIf { it.isNotBlank() }?.let { DetailRow(Icons.Outlined.Phone, it) }
            if (showAddedAt) {
                formatAddedAt(student.createdAt)?.let { DetailRow(Icons.Outlined.Schedule, "Added $it") }
            }
        }
    }
}

@Composable
private fun StaffResultCard(
    person: StaffDirectoryView,
    showAddedAt: Boolean = false,
) {
    val displayName = person.displayName?.trim()?.takeIf { it.isNotEmpty() }
        ?: person.name?.trim()?.takeIf { it.isNotEmpty() }
        ?: "Unnamed staff member"

    Surface(
        color = SurfaceWhite,
        shape = RoundedCornerShape(18.dp),
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(Gold.copy(alpha = 0.16f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initialsOf(displayName),
                        color = Color(0xFF92400E),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = displayName,
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = listOfNotNull(
                            person.designation?.takeIf { it.isNotBlank() },
                            person.departmentName?.takeIf { it.isNotBlank() },
                        ).joinToString(" • ").ifEmpty { "Staff" },
                        color = TextSecondary,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (person.isAdmin) {
                    ResultBadge("ADMIN", Color(0xFFBE123C))
                } else if (person.isHod) {
                    ResultBadge("HOD", Navy)
                } else if (person.isCoordinator) {
                    ResultBadge("COORDINATOR", Color(0xFF0F766E))
                }
            }

            person.email?.takeIf { it.isNotBlank() }?.let { DetailRow(Icons.Outlined.Email, it) }
            person.phoneNo?.takeIf { it.isNotBlank() }?.let { DetailRow(Icons.Outlined.Phone, it) }
            if (showAddedAt) {
                formatAddedAt(person.createdAt)?.let { DetailRow(Icons.Outlined.Schedule, "Added $it") }
            }
        }
    }
}

@Composable
private fun DetailRow(icon: ImageVector, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(7.dp))
        Text(
            text = value,
            color = TextSecondary,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ResultBadge(label: String, color: Color) {
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(7.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = label,
            color = color,
            fontSize = 9.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.6.sp
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AdminSettingsTab(
    adminName: String?,
    email: String?,
    onSignedOut: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    var showSignOut by remember { mutableStateOf(false) }
    var signingOut by remember { mutableStateOf(false) }
    var signOutComplete by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }

    val versionName = AppUpdateRepository.currentVersionName(context)

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            scope.launch {
                isRefreshing = true
                try { delay(300) } finally { isRefreshing = false }
            }
        },
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                color = SurfaceWhite,
                shape = RoundedCornerShape(20.dp),
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .background(Navy, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initialsOf(adminName),
                            color = GoldLight,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = adminName?.trim()?.takeIf { it.isNotEmpty() } ?: "Administrator",
                            color = TextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = censorEmail(email),
                            color = TextSecondary,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    ResultBadge("ADMIN", Navy)
                }
            }

            AdminSectionLabel("ACCOUNT")

            Surface(
                color = SurfaceWhite,
                shape = RoundedCornerShape(20.dp),
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SettingsRow(
                        icon = Icons.Outlined.Lock,
                        title = "Role",
                        value = "Administrator"
                    )
                    StatusDivider()
                    SettingsRow(
                        icon = Icons.Outlined.Email,
                        title = "Email",
                        value = censorEmail(email)
                    )
                }
            }

            AdminSectionLabel("APP")

            Surface(
                color = SurfaceWhite,
                shape = RoundedCornerShape(20.dp),
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SettingsRow(
                        icon = Icons.Outlined.SystemUpdate,
                        title = "Version",
                        value = "v$versionName"
                    )
                    StatusDivider()
                    SettingsRow(
                        icon = Icons.Outlined.AdminPanelSettings,
                        title = "Package",
                        value = context.packageName
                    )
                }
            }

            Button(
                onClick = { showSignOut = true },
                enabled = !signingOut,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFEE2E2),
                    contentColor = Color(0xFFDC2626)
                )
            ) {
                Icon(Icons.Outlined.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Sign Out from Portal", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }

            Text(
                text = "© 2026 MITRC, Alwar. All Rights Reserved.",
                color = TextSecondary,
                fontSize = 10.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp, bottom = 10.dp)
            )
        }
    }

    if (showSignOut) {
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
            onDismiss = { if (!signingOut) showSignOut = false },
            onExitAnimated = onSignedOut
        )
    }
}

@Composable
private fun SettingsRow(icon: ImageVector, title: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Navy, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(title, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.weight(1f))
        Text(
            text = value,
            color = TextSecondary,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}


internal fun initialsOf(name: String?): String {
    val parts = name?.trim()?.split(Regex("\\s+"))?.filter { it.isNotBlank() }.orEmpty()
    return when {
        parts.isEmpty() -> "A"
        parts.size == 1 -> parts.first().take(1).uppercase()
        else -> (parts[0].take(1) + parts[1].take(1)).uppercase()
    }
}

private val addedAtFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")

private fun formatAddedAt(raw: String?): String? {
    val text = raw?.trim().orEmpty()
    if (text.isEmpty()) return null
    return runCatching { OffsetDateTime.parse(text) }
        .getOrNull()
        ?.let { addedAtFormatter.format(it) }
}

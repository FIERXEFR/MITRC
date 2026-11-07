package com.mitrc.ac.`in`.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.AssignmentInd
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContactPhone
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SupervisorAccount
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.Work
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mitrc.ac.`in`.R
import com.mitrc.ac.`in`.auth.AdminRepository
import com.mitrc.ac.`in`.auth.AuthRepository
import com.mitrc.ac.`in`.auth.StaffEntry
import com.mitrc.ac.`in`.auth.StudentEntry
import com.mitrc.ac.`in`.data.AdminDbRow
import com.mitrc.ac.`in`.data.BranchRow
import com.mitrc.ac.`in`.data.ClassGroupRow
import com.mitrc.ac.`in`.data.ClassRow
import com.mitrc.ac.`in`.data.CourseRow
import com.mitrc.ac.`in`.data.GreetingStore
import com.mitrc.ac.`in`.data.PushRepository
import com.mitrc.ac.`in`.data.SupabaseManager
import com.mitrc.ac.`in`.data.SupabaseTableData
import com.mitrc.ac.`in`.ui.theme.DividerSoft
import com.mitrc.ac.`in`.ui.theme.ErrorRed
import com.mitrc.ac.`in`.ui.theme.Gold
import com.mitrc.ac.`in`.ui.theme.GoldLight
import com.mitrc.ac.`in`.ui.theme.Navy
import com.mitrc.ac.`in`.ui.theme.NavyDeep
import com.mitrc.ac.`in`.ui.theme.Slate
import com.mitrc.ac.`in`.ui.theme.SuccessGreen
import com.mitrc.ac.`in`.ui.theme.SurfaceWhite
import com.mitrc.ac.`in`.ui.theme.TextPrimary
import com.mitrc.ac.`in`.ui.theme.TextSecondary
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

enum class AdminEntryTab(val label: String) {
    STUDENT("STUDENT"),
    STAFF("STAFF")
}

/** Top-level destinations of the admin console, rendered by the shared portal bottom bar. */
internal enum class AdminTab(
    val label: String,
    val title: String,
    val icon: ImageVector,
) {
    DASHBOARD("Dashboard", "DASHBOARD", Icons.Outlined.Dashboard),
    SEARCH("Search", "SEARCH", Icons.Outlined.Search),
    APP("Control Panel", "CONTROL PANEL", Icons.Outlined.Tune),
    SETTINGS("Settings", "SETTINGS", Icons.Outlined.Settings),
}

/**
 * Admin console shell: the same bottom navigation treatment as the student and teacher portals,
 * with four destinations - Dashboard, Search, Control Panel and Settings.
 *
 * Account registration lives behind the dashboard's quick actions so the nav bar stays focused on
 * day-to-day monitoring and management.
 */
@Composable
fun AdminPanelScreen(onSignedOut: () -> Unit) {
    val user = AuthRepository.currentUser
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var selectedTabName by rememberSaveable { mutableStateOf(AdminTab.DASHBOARD.name) }
    val selectedTab = AdminTab.entries.firstOrNull { it.name == selectedTabName } ?: AdminTab.DASHBOARD

    val pagerState = rememberPagerState(
        initialPage = AdminTab.entries.indexOf(selectedTab).coerceAtLeast(0),
        pageCount = { AdminTab.entries.size },
    )

    fun goToTab(tab: AdminTab) {
        val targetIndex = AdminTab.entries.indexOf(tab)
        if (
            selectedTabName == tab.name &&
            pagerState.currentPage == targetIndex &&
            !pagerState.isScrollInProgress
        ) {
            return
        }
        selectedTabName = tab.name
        scope.launch {
            pagerState.animateScrollToPage(
                page = targetIndex,
                animationSpec = tween(300, easing = FastOutSlowInEasing),
            )
        }
    }

    LaunchedEffect(pagerState.currentPage, pagerState.isScrollInProgress) {
        if (!pagerState.isScrollInProgress) {
            val targetTab = AdminTab.entries.getOrNull(pagerState.currentPage)
            if (targetTab != null && targetTab.name != selectedTabName) {
                selectedTabName = targetTab.name
            }
        }
    }

    // Registration is a sub-flow opened from the dashboard; it hides the nav bar while open.
    var registerTabName by rememberSaveable { mutableStateOf<String?>(null) }
    val registerTab = registerTabName?.let { name ->
        AdminEntryTab.entries.firstOrNull { it.name == name }
    }

    var initialLoading by remember { mutableStateOf(true) }
    var adminName by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(user?.uid) {
        initialLoading = true
        val uid = user?.uid
        if (!uid.isNullOrBlank()) {
            val client = SupabaseManager.requireClient()
            val row = runCatching {
                client.postgrest[SupabaseTableData.Tables.ADMIN_DB]
                    .select { filter { eq("uid", uid) } }
                    .decodeList<AdminDbRow>()
                    .firstOrNull()
            }.getOrNull()
            adminName = row?.name?.ifBlank { null }

            // The admin panel is a separate activity from HomeScreen, so nothing else registers
            // this device's FCM token for an admin account. Register best-effort (never prompts
            // for the notification permission) so notices posted from the Control Panel can
            // reach this device while testing pushes.
            PushRepository.registerCurrentDevice(PushRepository.appVersion(context))
        }
        initialLoading = false
    }

    BackHandler(enabled = registerTab != null || selectedTab != AdminTab.DASHBOARD) {
        if (registerTab != null) {
            registerTabName = null
        } else if (selectedTab != AdminTab.DASHBOARD) {
            goToTab(AdminTab.DASHBOARD)
        }
    }

    val greeting = remember { GreetingStore.greeting() }
    val headerTitle = when {
        registerTab != null -> "${registerTab.label} REGISTRATION"
        selectedTab == AdminTab.DASHBOARD -> "$greeting, ${firstNameOf(adminName)}"
        else -> selectedTab.label
    }

    val foldDistancePx = with(LocalDensity.current) { 96.dp.toPx() }
    val headerOffset = remember { mutableFloatStateOf(0f) }
    val nestedScroll = remember(foldDistancePx) {
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

    LaunchedEffect(selectedTabName, registerTabName) { headerOffset.floatValue = 0f }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate)
    ) {
        AdminHeader(
            title = headerTitle,
            adminName = adminName,
            scrolledPx = { headerOffset.floatValue },
            showBack = registerTab != null,
            onBack = { registerTabName = null }
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .nestedScroll(nestedScroll)
        ) {
            when {
                initialLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Navy, modifier = Modifier.size(44.dp))
                            Spacer(Modifier.height(16.dp))
                            Text(
                                text = "Loading...",
                                color = TextSecondary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                else -> HorizontalPager(
                    state = pagerState,
                    beyondViewportPageCount = 1,
                    modifier = Modifier.fillMaxSize(),
                ) { page ->
                    when (AdminTab.entries[page]) {
                        AdminTab.DASHBOARD -> AdminDashboardTab(
                            adminName = adminName,
                            onOpenAppManagement = { goToTab(AdminTab.APP) },
                            onOpenSearch = { goToTab(AdminTab.SEARCH) },
                            onRegister = { tab ->
                                goToTab(AdminTab.SEARCH)
                                registerTabName = tab.name
                            }
                        )

                        AdminTab.APP -> AdminAppManagementTab()

                        AdminTab.SEARCH -> AdminSearchTab(onRegister = { registerTabName = it.name })

                        AdminTab.SETTINGS -> AdminSettingsTab(
                            adminName = adminName,
                            email = user?.email,
                            onSignedOut = onSignedOut
                        )
                    }
                }
            }

            if (registerTab != null) {
                AdminRegistrationDialog(
                    initialTab = registerTab,
                    onDismiss = { registerTabName = null }
                )
            }
        }

        if (registerTab == null) {
            PortalBottomBar(
                items = AdminTab.entries.map { PortalNavItem(label = it.label, icon = it.icon) },
                selectedIndex = AdminTab.entries.indexOf(selectedTab),
                onSelect = { index ->
                    AdminTab.entries.getOrNull(index)?.let { goToTab(it) }
                }
            )
        }
    }
}

@Composable
private fun AdminHeader(
    title: String,
    adminName: String?,
    scrolledPx: () -> Float,
    showBack: Boolean,
    onBack: () -> Unit,
) {
    val headerShape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
    val verticalPad = 6.dp
    val heroContentHeight = 74.dp
    val fullHeaderHeight = heroContentHeight + verticalPad * 2

    val density = LocalDensity.current
    val foldDistancePx = with(density) { 96.dp.toPx() }
    val fullHeaderHeightPx = with(density) { fullHeaderHeight.toPx() }

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
                .graphicsLayer {
                    val collapse = (scrolledPx() / foldDistancePx).coerceIn(0f, 1f)
                    translationY = -fullHeaderHeightPx * collapse
                    alpha = 1f - collapse
                },
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (showBack) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .padding(end = 6.dp)
                                .size(22.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                    }
                    Image(
                        painter = painterResource(R.drawable.logo_mitrc_white),
                        contentDescription = "MITRC logo",
                        contentScale = ContentScale.Fit,
                        alignment = Alignment.CenterStart,
                        modifier = Modifier
                            .width(74.dp)
                            .height(16.dp)
                    )
                }

                Spacer(Modifier.height(4.dp))

                Text(
                    text = "ADMIN PORTAL",
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
                        (fadeIn(tween(340, delayMillis = 150)) +
                            slideInVertically(tween(400, delayMillis = 150)) { -it / 4 })
                            .togetherWith(
                                fadeOut(tween(150)) +
                                    slideOutVertically(tween(200)) { it / 4 }
                            )
                    },
                    label = "adminHeaderTitle"
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

            Surface(
                shape = CircleShape,
                color = Gold.copy(alpha = 0.18f),
                border = BorderStroke(1.5.dp, Gold),
                modifier = Modifier
                    .padding(top = 18.dp)
                    .size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Outlined.AdminPanelSettings,
                        contentDescription = "Admin Panel",
                        tint = Gold,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AdminRegistrationDialog(
    initialTab: AdminEntryTab,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    val studentForm = remember { StudentFormState() }
    val staffForm = remember { StaffFormState() }

    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }

    fun submit() {
        error = null
        notice = null

        if (!SupabaseManager.isConfigured) {
            error = "Supabase is not configured yet"
            return
        }

        if (initialTab == AdminEntryTab.STUDENT) {
            val email = studentForm.email.trim()
            val password = studentForm.password
            val name = studentForm.name.trim()
            val serialNo = studentForm.serialNo.trim()
            val fatherName = studentForm.fatherName.trim()
            val studentPhone = studentForm.studentPhoneNo.trim()
            val fatherPhone = studentForm.fatherPhoneNo.trim()

            if (email.isEmpty() || password.isEmpty() || name.isEmpty() ||
                serialNo.isEmpty() || fatherName.isEmpty() || studentPhone.isEmpty() || fatherPhone.isEmpty()
            ) {
                error = "Please fill in all mandatory fields marked with *"
                return
            }

            if (!email.contains('@') || !email.contains('.')) {
                error = "Enter a valid email address"
                return
            }

            if (password.length < 6) {
                error = "Password must be at least 6 characters"
                return
            }

            focusManager.clearFocus()
            loading = true
            scope.launch {
                val result = AdminRepository.createStudentAccount(
                    StudentEntry(
                        email = email,
                        password = password,
                        name = name,
                        serialNo = serialNo,
                        fatherName = fatherName,
                        studentPhoneNo = studentPhone,
                        fatherPhoneNo = fatherPhone,
                        classId = studentForm.classId,
                        groupId = studentForm.groupId,
                    )
                )
                loading = false
                result.fold(
                    onSuccess = {
                        notice = "Account created for $name!"
                        studentForm.clearAll()
                    },
                    onFailure = { error = it.message ?: "Account creation failed" }
                )
            }
        } else {
            val email = staffForm.email.trim()
            val password = staffForm.password
            val name = staffForm.name.trim()
            val designation = staffForm.designation.trim()
            val department = staffForm.department.trim()
            val role = staffForm.role.trim()
            val phoneNo = staffForm.phoneNo.trim()

            if (email.isEmpty() || password.isEmpty() || name.isEmpty() ||
                designation.isEmpty() || department.isEmpty() || role.isEmpty() || phoneNo.isEmpty()
            ) {
                error = "Please fill in all mandatory fields marked with *"
                return
            }

            if (!email.contains('@') || !email.contains('.')) {
                error = "Enter a valid email address"
                return
            }

            if (password.length < 6) {
                error = "Password must be at least 6 characters"
                return
            }

            focusManager.clearFocus()
            loading = true
            scope.launch {
                val result = AdminRepository.createStaffAccount(
                    StaffEntry(
                        email = email,
                        password = password,
                        name = name,
                        department = department,
                        role = role,
                        phoneNo = phoneNo,
                        gender = staffForm.gender.trim(),
                        designation = designation,
                    )
                )
                loading = false
                result.fold(
                    onSuccess = {
                        notice = "Staff account created for $name!"
                        staffForm.clearAll()
                    },
                    onFailure = { error = it.message ?: "Account creation failed" }
                )
            }
        }
    }

    Dialog(
        onDismissRequest = { if (!loading) onDismiss() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
                .padding(horizontal = 20.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(26.dp),
                color = SurfaceWhite,
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.88f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Navy, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Outlined.PersonAdd,
                                    contentDescription = null,
                                    tint = GoldLight,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = if (initialTab == AdminEntryTab.STUDENT) "Register Student Account" else "Register Staff Account",
                                color = TextPrimary,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        IconButton(onClick = onDismiss, enabled = !loading) {
                            Icon(Icons.Outlined.Close, contentDescription = "Close", tint = TextSecondary)
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {
                        when (initialTab) {
                            AdminEntryTab.STUDENT -> StudentEntryForm(studentForm)
                            AdminEntryTab.STAFF -> StaffEntryForm(staffForm)
                        }

                        if (error != null) {
                            Text(
                                text = error.orEmpty(),
                                color = ErrorRed,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }

                        if (notice != null) {
                            Text(
                                text = notice.orEmpty(),
                                color = SuccessGreen,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    Button(
                        onClick = ::submit,
                        enabled = !loading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Navy,
                            contentColor = Color.White
                        )
                    ) {
                        if (loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = GoldLight,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                Icons.Outlined.PersonAdd,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = if (initialTab == AdminEntryTab.STUDENT) "Register Student" else "Register Staff",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}


internal fun censorEmail(email: String?): String {
    val mail = email?.trim().orEmpty()
    if (!mail.contains('@')) return mail
    val parts = mail.split("@")
    val local = parts[0]
    val domain = parts[1]
    if (local.length <= 2) {
        return "${local.take(1)}*@$domain"
    }
    val visible = local.take(2)
    val hidden = "*".repeat(local.length - 2)
    return "$visible$hidden@$domain"
}

// ---------------------------------------------------------------------------------------------
// Forms
// ---------------------------------------------------------------------------------------------

@Composable
private fun StudentEntryForm(form: StudentFormState) {
    val scope = rememberCoroutineScope()

    var courses by remember { mutableStateOf<List<CourseRow>>(emptyList()) }
    var selectedCourse by remember { mutableStateOf<CourseRow?>(null) }
    var courseExpanded by remember { mutableStateOf(false) }

    var branches by remember { mutableStateOf<List<BranchRow>>(emptyList()) }
    var selectedBranch by remember { mutableStateOf<BranchRow?>(null) }
    var branchExpanded by remember { mutableStateOf(false) }

    var classes by remember { mutableStateOf<List<ClassRow>>(emptyList()) }
    var selectedClass by remember { mutableStateOf<ClassRow?>(null) }
    var classExpanded by remember { mutableStateOf(false) }

    var groups by remember { mutableStateOf<List<ClassGroupRow>>(emptyList()) }
    var isLoadingData by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isLoadingData = true
        courses = AdminRepository.getCourses()
        if (courses.isNotEmpty()) {
            selectedCourse = courses.first()
        }
        isLoadingData = false
    }

    LaunchedEffect(selectedCourse) {
        val c = selectedCourse
        if (c != null) {
            isLoadingData = true
            branches = AdminRepository.getBranches(c.id)
            selectedBranch = branches.firstOrNull()
            isLoadingData = false
        } else {
            branches = emptyList()
            selectedBranch = null
        }
    }

    LaunchedEffect(selectedBranch) {
        val b = selectedBranch
        if (b != null) {
            isLoadingData = true
            classes = AdminRepository.getClassesForBranch(b.id)
            selectedClass = classes.firstOrNull()
            form.classId = selectedClass?.id
            isLoadingData = false
        } else {
            classes = emptyList()
            selectedClass = null
            form.classId = null
        }
    }

    LaunchedEffect(form.classId) {
        val cid = form.classId
        if (cid != null && cid > 0) {
            groups = AdminRepository.getClassGroups(cid)
        } else {
            groups = emptyList()
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        AnimatedVisibility(visible = isLoadingData) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .padding(bottom = 8.dp),
                color = Gold,
                trackColor = Slate
            )
        }

        FormSectionLabel("ACCOUNT CREDENTIALS")

        AdminField(
            value = form.email,
            onValueChange = { form.email = it },
            label = "Student Email address",
            leadingIcon = Icons.Outlined.Email,
            keyboardType = KeyboardType.Email
        )
        Spacer(Modifier.height(12.dp))
        AdminPasswordField(
            value = form.password,
            onValueChange = { form.password = it },
            imeAction = ImeAction.Next
        )

        Spacer(Modifier.height(8.dp))
        FormSectionLabel("STUDENT PERSONAL DETAILS")

        AdminField(
            value = form.name,
            onValueChange = { form.name = it },
            label = "Full Name",
            leadingIcon = Icons.Outlined.Person
        )
        Spacer(Modifier.height(12.dp))
        AdminField(
            value = form.serialNo,
            onValueChange = { form.serialNo = it },
            label = "Serial / Roll No",
            leadingIcon = Icons.Outlined.Badge
        )
        Spacer(Modifier.height(12.dp))
        AdminField(
            value = form.fatherName,
            onValueChange = { form.fatherName = it },
            label = "Father's Name",
            leadingIcon = Icons.Outlined.SupervisorAccount
        )
        Spacer(Modifier.height(12.dp))
        AdminField(
            value = form.studentPhoneNo,
            onValueChange = { form.studentPhoneNo = it },
            label = "Student Phone No",
            leadingIcon = Icons.Outlined.ContactPhone,
            keyboardType = KeyboardType.Phone
        )
        Spacer(Modifier.height(12.dp))
        AdminField(
            value = form.fatherPhoneNo,
            onValueChange = { form.fatherPhoneNo = it },
            label = "Father Phone No",
            leadingIcon = Icons.Outlined.ContactPhone,
            keyboardType = KeyboardType.Phone
        )

        Spacer(Modifier.height(8.dp))
        FormSectionLabel("ACADEMIC STRUCTURE & PLACEMENT")

        // 1. Course Dropdown
        Text(text = "1. Select Course:", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    if (courses.isEmpty()) {
                        scope.launch {
                            isLoadingData = true
                            courses = AdminRepository.getCourses()
                            if (courses.isNotEmpty()) selectedCourse = courses.first()
                            isLoadingData = false
                        }
                    } else {
                        courseExpanded = true
                    }
                }
        ) {
            OutlinedTextField(
                value = selectedCourse?.name ?: if (courses.isEmpty()) "Tap to fetch courses" else "Select Course",
                onValueChange = {},
                readOnly = true,
                leadingIcon = { Icon(Icons.Outlined.School, contentDescription = null, tint = Navy) },
                trailingIcon = { Icon(Icons.Outlined.ArrowDropDown, contentDescription = null, tint = Navy) },
                modifier = Modifier.fillMaxWidth(),
                enabled = false,
                shape = RoundedCornerShape(12.dp),
                colors = adminFieldColors()
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable {
                        if (courses.isEmpty()) {
                            scope.launch {
                                isLoadingData = true
                                courses = AdminRepository.getCourses()
                                if (courses.isNotEmpty()) selectedCourse = courses.first()
                                isLoadingData = false
                            }
                        } else {
                            courseExpanded = true
                        }
                    }
            )
            DropdownMenu(
                expanded = courseExpanded,
                onDismissRequest = { courseExpanded = false },
                modifier = Modifier.fillMaxWidth(0.8f)
            ) {
                courses.forEach { course ->
                    DropdownMenuItem(
                        text = { Text(course.name, fontWeight = FontWeight.SemiBold) },
                        onClick = {
                            selectedCourse = course
                            courseExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // 2. Branch Dropdown
        Text(text = "2. Select Branch:", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        Spacer(Modifier.height(4.dp))
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = selectedBranch?.name ?: if (branches.isEmpty()) "No Branches Available" else "Select Branch",
                onValueChange = {},
                readOnly = true,
                leadingIcon = { Icon(Icons.Outlined.School, contentDescription = null, tint = Navy) },
                trailingIcon = { Icon(Icons.Outlined.ArrowDropDown, contentDescription = null, tint = Navy) },
                modifier = Modifier.fillMaxWidth(),
                enabled = false,
                shape = RoundedCornerShape(12.dp),
                colors = adminFieldColors()
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable(enabled = branches.isNotEmpty()) { branchExpanded = true }
            )
            DropdownMenu(
                expanded = branchExpanded,
                onDismissRequest = { branchExpanded = false },
                modifier = Modifier.fillMaxWidth(0.8f)
            ) {
                branches.forEach { branch ->
                    DropdownMenuItem(
                        text = { Text(branch.name, fontWeight = FontWeight.SemiBold) },
                        onClick = {
                            selectedBranch = branch
                            branchExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // 3. Class Dropdown
        Text(text = "3. Select Semester & Section:", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        Spacer(Modifier.height(4.dp))
        Box(modifier = Modifier.fillMaxWidth()) {
            val classLabel = if (selectedClass != null) {
                "Sem ${selectedClass?.semester} - Section ${selectedClass?.section} (${selectedClass?.academicYear})"
            } else if (classes.isEmpty()) {
                "No Active Cohorts for Branch"
            } else {
                "Select Semester & Section"
            }

            OutlinedTextField(
                value = classLabel,
                onValueChange = {},
                readOnly = true,
                leadingIcon = { Icon(Icons.Outlined.School, contentDescription = null, tint = Navy) },
                trailingIcon = { Icon(Icons.Outlined.ArrowDropDown, contentDescription = null, tint = Navy) },
                modifier = Modifier.fillMaxWidth(),
                enabled = false,
                shape = RoundedCornerShape(12.dp),
                colors = adminFieldColors()
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable(enabled = classes.isNotEmpty()) { classExpanded = true }
            )
            DropdownMenu(
                expanded = classExpanded,
                onDismissRequest = { classExpanded = false },
                modifier = Modifier.fillMaxWidth(0.8f)
            ) {
                classes.forEach { cls ->
                    DropdownMenuItem(
                        text = { Text("Sem ${cls.semester} - Section ${cls.section} (${cls.academicYear})", fontWeight = FontWeight.SemiBold) },
                        onClick = {
                            selectedClass = cls
                            form.classId = cls.id
                            classExpanded = false
                        }
                    )
                }
            }
        }

        // 4. Lab Group Selection
        if (groups.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = "4. Select Lab Group (Optional):",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (form.groupId == null) Navy else Slate)
                        .clickable { form.groupId = null }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Whole Class",
                        color = if (form.groupId == null) Color.White else TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                groups.forEach { grp ->
                    val selected = form.groupId == grp.id
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selected) Navy else Slate)
                            .clickable { form.groupId = grp.id }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = grp.name,
                            color = if (selected) Color.White else TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StaffEntryForm(form: StaffFormState) {
    var roleExpanded by remember { mutableStateOf(false) }
    val rolesList = listOf("Teacher", "Mentor", "Coordinator", "HOD", "Dean")

    Column(modifier = Modifier.fillMaxWidth()) {
        FormSectionLabel("ACCOUNT CREDENTIALS")

        AdminField(
            value = form.email,
            onValueChange = { form.email = it },
            label = "Staff Email address",
            leadingIcon = Icons.Outlined.Email,
            keyboardType = KeyboardType.Email
        )
        Spacer(Modifier.height(12.dp))
        AdminPasswordField(
            value = form.password,
            onValueChange = { form.password = it },
            imeAction = ImeAction.Next
        )

        Spacer(Modifier.height(8.dp))
        FormSectionLabel("STAFF DETAILS")

        AdminField(
            value = form.name,
            onValueChange = { form.name = it },
            label = "Full Name",
            leadingIcon = Icons.Outlined.Person
        )
        Spacer(Modifier.height(12.dp))
        AdminField(
            value = form.designation,
            onValueChange = { form.designation = it },
            label = "Designation (e.g. Assistant Professor)",
            leadingIcon = Icons.Outlined.AssignmentInd
        )
        Spacer(Modifier.height(12.dp))
        AdminField(
            value = form.department,
            onValueChange = { form.department = it },
            label = "Department (e.g. CSE)",
            leadingIcon = Icons.Outlined.Work
        )
        Spacer(Modifier.height(12.dp))

        // Role Dropdown Selection
        Text(text = "Select Staff Role:", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        Spacer(Modifier.height(4.dp))
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = form.role.ifBlank { "Select Role" },
                onValueChange = {},
                readOnly = true,
                leadingIcon = { Icon(Icons.Outlined.SupervisorAccount, contentDescription = null, tint = Navy) },
                trailingIcon = { Icon(Icons.Outlined.ArrowDropDown, contentDescription = null, tint = Navy) },
                modifier = Modifier.fillMaxWidth(),
                enabled = false,
                shape = RoundedCornerShape(12.dp),
                colors = adminFieldColors()
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable { roleExpanded = true }
            )
            DropdownMenu(
                expanded = roleExpanded,
                onDismissRequest = { roleExpanded = false },
                modifier = Modifier.fillMaxWidth(0.8f)
            ) {
                rolesList.forEach { r ->
                    DropdownMenuItem(
                        text = { Text(r, fontWeight = FontWeight.SemiBold) },
                        onClick = {
                            form.role = r
                            roleExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        FormSectionLabel("CONTACT & PROFILE")

        AdminField(
            value = form.phoneNo,
            onValueChange = { form.phoneNo = it },
            label = "Phone No",
            leadingIcon = Icons.Outlined.ContactPhone,
            keyboardType = KeyboardType.Phone
        )
        Spacer(Modifier.height(12.dp))

        // Gender Selection Chips
        Text(text = "Select Gender:", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        Spacer(Modifier.height(6.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            listOf("Male", "Female").forEach { g ->
                val selected = form.gender.equals(g, ignoreCase = true)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selected) Navy else Slate)
                        .clickable { form.gender = g }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = g,
                        color = if (selected) Color.White else TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun FormSectionLabel(text: String) {
    Text(
        text = text,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = TextSecondary,
        letterSpacing = 1.5.sp,
        modifier = Modifier.padding(top = 14.dp, bottom = 8.dp)
    )
}

@Composable
private fun AdminField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    leadingIcon: ImageVector,
    isRequired: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label)
                if (isRequired) {
                    Text(text = " *", color = ErrorRed, fontWeight = FontWeight.Bold)
                }
            }
        },
        singleLine = true,
        leadingIcon = { Icon(leadingIcon, contentDescription = null, tint = Navy) },
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = imeAction
        ),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = adminFieldColors()
    )
}

@Composable
private fun AdminPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    imeAction: ImeAction
) {
    var visible by rememberSaveable { mutableStateOf(false) }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Password")
                Text(text = " *", color = ErrorRed, fontWeight = FontWeight.Bold)
            }
        },
        singleLine = true,
        leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null, tint = Navy) },
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
                Icon(
                    imageVector = if (visible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                    contentDescription = if (visible) "Hide password" else "Show password",
                    tint = TextSecondary
                )
            }
        },
        visualTransformation = if (visible) {
            VisualTransformation.None
        } else {
            PasswordVisualTransformation()
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = imeAction
        ),
        keyboardActions = KeyboardActions(onDone = { /* submission is driven by the button */ }),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = adminFieldColors()
    )
}

@Composable
private fun adminFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Navy,
    unfocusedBorderColor = DividerSoft,
    focusedLabelColor = Navy,
    unfocusedLabelColor = TextSecondary,
    cursorColor = Navy,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    focusedLeadingIconColor = Navy,
    unfocusedLeadingIconColor = TextSecondary,
    disabledBorderColor = DividerSoft,
    disabledLeadingIconColor = Navy,
    disabledTextColor = TextPrimary,
    disabledLabelColor = TextSecondary,
    unfocusedContainerColor = SurfaceWhite,
    focusedContainerColor = SurfaceWhite,
    disabledContainerColor = SurfaceWhite
)

private class StudentFormState {
    var email by mutableStateOf("")
    var password by mutableStateOf("")
    var name by mutableStateOf("")
    var serialNo by mutableStateOf("")
    var fatherName by mutableStateOf("")
    var studentPhoneNo by mutableStateOf("")
    var fatherPhoneNo by mutableStateOf("")
    var classId by mutableStateOf<Int?>(null)
    var groupId by mutableStateOf<Int?>(null)

    fun clearAll() {
        email = ""
        password = ""
        name = ""
        serialNo = ""
        fatherName = ""
        studentPhoneNo = ""
        fatherPhoneNo = ""
    }

    fun resetIdentity() {
        clearAll()
    }
}

private class StaffFormState {
    var email by mutableStateOf("")
    var password by mutableStateOf("")
    var name by mutableStateOf("")
    var designation by mutableStateOf("")
    var department by mutableStateOf("")
    var role by mutableStateOf("Teacher")
    var phoneNo by mutableStateOf("")
    var gender by mutableStateOf("Male")

    fun clearAll() {
        email = ""
        password = ""
        name = ""
        designation = ""
        department = ""
        role = "Teacher"
        phoneNo = ""
        gender = "Male"
    }

    fun resetIdentity() {
        clearAll()
    }
}

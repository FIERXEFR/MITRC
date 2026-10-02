package com.mitrc.ac.`in`.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.AssignmentInd
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.ContactPhone
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.SupervisorAccount
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class AdminEntryTab(val label: String) {
    STUDENT("STUDENT"),
    STAFF("STAFF")
}

@Composable
fun AdminPanelScreen(onSignedOut: () -> Unit) {
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val user = AuthRepository.currentUser

    var selectedTab by remember { mutableStateOf(AdminEntryTab.STUDENT) }
    var tabSwitching by remember { mutableStateOf(false) }

    var initialLoading by remember { mutableStateOf(true) }
    var adminName by remember { mutableStateOf<String?>(null) }

    // First time startup spinner + Fetch Admin's name
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
        }
        delay(1000) // Authentic 1-second startup spinner
        initialLoading = false
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val textPulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val studentForm = remember { StudentFormState() }
    val staffForm = remember { StaffFormState() }

    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }

    fun switchTab(tab: AdminEntryTab) {
        if (tab == selectedTab || tabSwitching) return
        tabSwitching = true
        error = null
        notice = null
        scope.launch {
            delay(1000)
            selectedTab = tab
            tabSwitching = false
        }
    }

    fun submit() {
        error = null
        notice = null

        if (!SupabaseManager.isConfigured) {
            error = "Supabase is not configured yet"
            return
        }

        if (selectedTab == AdminEntryTab.STUDENT) {
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header with Roll-Up Typography Animation
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(listOf(NavyDeep, Navy)),
                        shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)
                    )
                    .padding(top = 48.dp, bottom = 28.dp, start = 20.dp, end = 20.dp)
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(140.dp)
                        .offset(x = 35.dp, y = (-40).dp)
                        .background(Gold.copy(alpha = 0.07f), CircleShape)
                )

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Image(
                                painter = painterResource(R.drawable.logo_mitrc_white),
                                contentDescription = "MITRC Alwar Logo",
                                modifier = Modifier.width(170.dp)
                            )
                            Spacer(Modifier.height(6.dp))

                            // Vertical Roll-Up Typography Animation
                            AnimatedContent(
                                targetState = selectedTab,
                                transitionSpec = {
                                    (fadeIn(animationSpec = tween(350)) + slideInVertically(animationSpec = tween(350)) { -it })
                                        .togetherWith(fadeOut(animationSpec = tween(350)) + slideOutVertically(animationSpec = tween(350)) { it })
                                },
                                label = "rollUpTitleAnimation"
                            ) { tab ->
                                Text(
                                    text = "${tab.label} REGISTRATION",
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = (-0.5).sp,
                                    modifier = Modifier.alpha(textPulseAlpha)
                                )
                            }
                        }

                        Surface(
                            shape = CircleShape,
                            color = Gold.copy(alpha = 0.15f),
                            border = BorderStroke(1.5.dp, GoldLight),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Outlined.AdminPanelSettings,
                                    contentDescription = "Admin",
                                    tint = GoldLight,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .background(
                                    color = Gold.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "ADMIN",
                                color = GoldLight,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp
                            )
                        }
                        Spacer(Modifier.width(10.dp))

                        val displayName = adminName ?: "Administrator"
                        val censoredMail = censorEmail(user?.email ?: "ukgankit@gmail.com")
                        Text(
                            text = "$displayName ($censoredMail)",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            if (initialLoading) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
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
            } else {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    // Card View
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = SurfaceWhite,
                        shadowElevation = 10.dp,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 18.dp, vertical = 18.dp)
                        ) {
                            AdminEntryTabs(
                                selected = selectedTab,
                                onSelected = ::switchTab
                            )

                            Spacer(Modifier.height(14.dp))

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState())
                            ) {
                                if (tabSwitching) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(220.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            CircularProgressIndicator(color = Navy, modifier = Modifier.size(36.dp))
                                            Spacer(Modifier.height(14.dp))
                                            Text(
                                                text = "Fetching...",
                                                color = TextSecondary,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                } else {
                                    AnimatedContent(
                                        targetState = selectedTab,
                                        transitionSpec = {
                                            (fadeIn(animationSpec = tween(350)) + slideInVertically(animationSpec = tween(350)) { -it })
                                                .togetherWith(fadeOut(animationSpec = tween(350)) + slideOutVertically(animationSpec = tween(350)) { it })
                                        },
                                        label = "rollUpCategoryFormAnimation"
                                    ) { tab ->
                                        when (tab) {
                                            AdminEntryTab.STUDENT -> StudentEntryForm(studentForm)
                                            AdminEntryTab.STAFF -> StaffEntryForm(staffForm)
                                        }
                                    }
                                }

                                AnimatedVisibility(visible = error != null) {
                                    Text(
                                        text = error.orEmpty(),
                                        color = ErrorRed,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(top = 10.dp)
                                    )
                                }

                                AnimatedVisibility(visible = notice != null) {
                                    Text(
                                        text = notice.orEmpty(),
                                        color = SuccessGreen,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(top = 10.dp)
                                    )
                                }
                            }

                            Spacer(Modifier.height(12.dp))

                            Button(
                                onClick = ::submit,
                                enabled = !loading && !tabSwitching,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Navy,
                                    contentColor = Color.White,
                                    disabledContainerColor = Navy.copy(alpha = 0.55f),
                                    disabledContentColor = Color.White.copy(alpha = 0.7f)
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
                                        text = if (selectedTab == AdminEntryTab.STUDENT) "Register Student Account" else "Register Staff Account",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    // Fixed Footer
                    Button(
                        onClick = {
                            scope.launch {
                                AuthRepository.signOut()
                                onSignedOut()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFEE2E2),
                            contentColor = Color(0xFFDC2626)
                        )
                    ) {
                        Icon(
                            Icons.Outlined.Logout,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Sign Out from Portal",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(Modifier.height(6.dp))

                    Text(
                        text = "© 2026 MITRC, Alwar. All Rights Reserved.",
                        color = TextSecondary,
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

private fun censorEmail(email: String?): String {
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

@Composable
private fun AdminEntryTabs(
    selected: AdminEntryTab,
    onSelected: (AdminEntryTab) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Slate, RoundedCornerShape(12.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        AdminEntryTab.entries.forEach { tab ->
            AdminEntryTabPill(
                label = tab.label,
                selected = selected == tab,
                onClick = { onSelected(tab) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun AdminEntryTabPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) Navy else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = if (selected) Color.White else TextSecondary
        )
    }
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

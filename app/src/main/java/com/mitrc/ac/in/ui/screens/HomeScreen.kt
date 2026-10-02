package com.mitrc.ac.`in`.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mitrc.ac.`in`.auth.AuthRepository
import com.mitrc.ac.`in`.data.PortalRepository
import com.mitrc.ac.`in`.ui.theme.Gold
import com.mitrc.ac.`in`.ui.theme.GoldLight
import com.mitrc.ac.`in`.ui.theme.Navy
import com.mitrc.ac.`in`.ui.theme.NavyDeep
import com.mitrc.ac.`in`.ui.theme.Slate
import com.mitrc.ac.`in`.ui.theme.SurfaceWhite
import com.mitrc.ac.`in`.ui.theme.TextPrimary
import com.mitrc.ac.`in`.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class StaffRole(val label: String, val isComingSoon: Boolean = false) {
    STUDENT("Student"),
    TEACHER("Teacher"),
    COORDINATOR("Co-ordinator"),
    HOD("HOD (Soon)", true),
    DEAN("Dean (Soon)", true)
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun HomeScreen(onSignedOut: () -> Unit) {
    val scope = rememberCoroutineScope()
    val user = AuthRepository.currentUser
    val userUid = user?.uid.orEmpty()

    var selectedRole by remember { mutableStateOf(StaffRole.STUDENT) }

    // Resolve default role dynamically on sign-in
    LaunchedEffect(userUid) {
        if (userUid.isNotEmpty()) {
            val studentProfile = PortalRepository.getStudentProfile(userUid).getOrNull()
            if (studentProfile != null) {
                selectedRole = StaffRole.STUDENT
                return@LaunchedEffect
            }

            val teacherProfile = PortalRepository.getTeacherProfile(userUid).getOrNull()
            if (teacherProfile != null) {
                selectedRole = StaffRole.TEACHER
                return@LaunchedEffect
            }

            val coordProfile = PortalRepository.getCoordinatorProfile(userUid).getOrNull()
            if (coordProfile != null) {
                selectedRole = StaffRole.COORDINATOR
                return@LaunchedEffect
            }
        }
    }

    var animateTrigger by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(100)
        animateTrigger = true
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(listOf(NavyDeep, Navy)),
                        shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
                    )
                    .padding(top = 64.dp, bottom = 36.dp, start = 24.dp, end = 24.dp)
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(160.dp)
                        .offset(x = 40.dp, y = (-50).dp)
                        .background(Gold.copy(alpha = 0.07f), CircleShape)
                )

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "MITRC ALWAR",
                                color = GoldLight,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.5.sp,
                                modifier = Modifier.alpha(textPulseAlpha)
                            )
                            Spacer(Modifier.height(4.dp))
                            AnimatedContent(
                                targetState = selectedRole,
                                transitionSpec = {
                                    (fadeIn(animationSpec = tween(400)) + slideInVertically(animationSpec = tween(400)) { -it })
                                        .togetherWith(fadeOut(animationSpec = tween(400)) + slideOutVertically(animationSpec = tween(400)) { it })
                                },
                                label = "titleAnimation"
                            ) { role ->
                                Text(
                                    text = "${role.label} Portal",
                                    color = Color.White,
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = (-0.5).sp
                                )
                            }
                        }

                        Surface(
                            shape = CircleShape,
                            color = Gold.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, GoldLight),
                            modifier = Modifier.size(46.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Outlined.Person,
                                    contentDescription = null,
                                    tint = GoldLight,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    Text(
                        text = user?.email ?: "portal.user@mitrc.ac.in",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 14.sp,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Role Switcher Bar - Hidden when logged in as Student
            if (selectedRole != StaffRole.STUDENT) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp)
                ) {
                    Text(
                        text = "SWITCH PORTAL MODE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        letterSpacing = 1.5.sp,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StaffRole.entries.filter { it != StaffRole.STUDENT }.forEach { role ->
                            val isSelected = selectedRole == role
                            val scale by animateFloatAsState(if (isSelected) 1.04f else 1f, label = "tabScale")

                            Box(
                                modifier = Modifier
                                    .scale(scale)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        when {
                                            isSelected -> Navy
                                            role.isComingSoon -> Slate
                                            else -> SurfaceWhite
                                        }
                                    )
                                    .clickable(enabled = !role.isComingSoon) {
                                        selectedRole = role
                                    }
                                    .padding(horizontal = 18.dp, vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (role.isComingSoon) {
                                        Icon(
                                            Icons.Outlined.Lock,
                                            contentDescription = null,
                                            tint = TextSecondary.copy(alpha = 0.6f),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                    } else if (isSelected) {
                                        Icon(
                                            Icons.Outlined.Star,
                                            contentDescription = null,
                                            tint = GoldLight,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                    }
                                    Text(
                                        text = role.label,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = when {
                                            isSelected -> Color.White
                                            role.isComingSoon -> TextSecondary.copy(alpha = 0.5f)
                                            else -> TextPrimary
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Dashboard Content
            AnimatedVisibility(
                visible = animateTrigger,
                enter = fadeIn(animationSpec = tween(500)) + slideInVertically(
                    initialOffsetY = { 60 },
                    animationSpec = tween(500)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 20.dp)
                ) {
                    when (selectedRole) {
                        StaffRole.STUDENT -> StudentPortalView(userUid)
                        StaffRole.TEACHER -> TeacherAttendanceView(userUid)
                        StaffRole.COORDINATOR -> CoordinatorTimetableView(userUid)
                        else -> StudentPortalView(userUid)
                    }

                    Spacer(Modifier.height(30.dp))

                    Button(
                        onClick = {
                            scope.launch {
                                AuthRepository.signOut()
                                onSignedOut()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(16.dp),
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
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "Sign Out from Portal",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(Modifier.height(32.dp))
                }
            }
        }
    }
}

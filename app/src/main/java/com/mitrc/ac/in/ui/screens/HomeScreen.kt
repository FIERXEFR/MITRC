package com.mitrc.ac.`in`.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.Animatable
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
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.automirrored.outlined.ListAlt
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.AssignmentInd
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.Class
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PostAdd
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mitrc.ac.`in`.auth.AuthRepository
import com.mitrc.ac.`in`.ui.theme.DividerSoft
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

enum class StaffRole(val label: String, val isComingSoon: Boolean = false) {
    COORDINATOR("Co-ordinator"),
    HOD("HOD"),
    TEACHER("Teacher"),
    DEAN("Dean (Soon)", true),
    DIRECTOR("Director (Soon)", true),
    HR("HR (Soon)", true)
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun HomeScreen(onSignedOut: () -> Unit) {
    val scope = rememberCoroutineScope()
    val user = AuthRepository.currentUser
    var selectedRole by remember { mutableStateOf(StaffRole.COORDINATOR) }

    // Smooth staggered enter animations
    var animateTrigger by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(100)
        animateTrigger = true
    }

    // Glowing typography pulse animation
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
            // Premium Header with gradient mesh
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(listOf(NavyDeep, Navy)),
                        shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
                    )
                    .padding(top = 64.dp, bottom = 36.dp, start = 24.dp, end = 24.dp)
            ) {
                // Secondary decorative circles
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
                            // Typography animation for welcome title
                            AnimatedContent(
                                targetState = selectedRole,
                                transitionSpec = {
                                    (fadeIn(animationSpec = tween(400)) + slideInVertically(animationSpec = tween(400)) { -it })
                                        .togetherWith(fadeOut(animationSpec = tween(400)) + slideOutVertically(animationSpec = tween(400)) { it })
                                },
                                label = "titleAnimation"
                            ) { role ->
                                Text(
                                    text = "${role.label} Desk",
                                    color = Color.White,
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = (-0.5).sp
                                )
                            }
                        }

                        // Profile badge
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
                        text = user?.email ?: "coordinator.mgmt@mitrc.ac.in",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 14.sp,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Role Switcher Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp)
            ) {
                Text(
                    text = "SWITCH CURRENT ACTIVE ROLE",
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
                    StaffRole.values().forEach { role ->
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
                        StaffRole.COORDINATOR -> CoordinatorDashboardView()
                        StaffRole.HOD -> HodDashboardView()
                        StaffRole.TEACHER -> TeacherDashboardView()
                        else -> CoordinatorDashboardView()
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

@Composable
fun CoordinatorDashboardView() {
    Column {
        Text(
            text = "CO-ORDINATOR OVERVIEW",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = TextSecondary,
            letterSpacing = 1.2.sp
        )
        Spacer(Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            MetricCard(
                title = "Monitored Students",
                value = "426 Students",
                subtext = "B.Tech III & IV Year",
                icon = Icons.Outlined.Group,
                color = NavySoft,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "Today's Attendance",
                value = "89.4 %",
                subtext = "+2.4% from yesterday",
                icon = Icons.Outlined.Analytics,
                color = SuccessGreen,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(14.dp))

        MetricCardFullWidth(
            title = "Academic Notice Approvals Pending",
            value = "4 Drafts Awaiting Release",
            progress = 0.65f,
            icon = Icons.Outlined.NotificationsActive
        )

        Spacer(Modifier.height(24.dp))

        Text(
            text = "MANAGEMENT & QUICK ACTIONS",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = TextSecondary,
            letterSpacing = 1.2.sp
        )
        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ActionCard(
                title = "Broadcast Notice",
                desc = "Push to student feeds",
                icon = Icons.Outlined.Campaign,
                modifier = Modifier.weight(1f)
            )
            ActionCard(
                title = "Modify Timetable",
                desc = "Adjust classes & labs",
                icon = Icons.Outlined.CalendarMonth,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ActionCard(
                title = "Assign Faculty",
                desc = "Link teachers to batches",
                icon = Icons.Outlined.AssignmentInd,
                modifier = Modifier.weight(1f)
            )
            ActionCard(
                title = "Exam Schedules",
                desc = "Mid-terms / Practicals",
                icon = Icons.Outlined.Class,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(26.dp))

        Text(
            text = "RECENT ANNOUNCEMENTS BY YOU",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = TextSecondary,
            letterSpacing = 1.2.sp
        )
        Spacer(Modifier.height(12.dp))

        NoticeListItem(
            tag = "CRITICAL",
            title = "Mid-Term Attendance shortage list publication",
            time = "Updated 2 hours ago"
        )
        NoticeListItem(
            tag = "ACADEMICS",
            title = "Registration link for IBM Hackathon batch 2026",
            time = "Updated yesterday"
        )
    }
}

@Composable
fun HodDashboardView() {
    Column {
        Text(
            text = "HOD DEPARTMENT DESK",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = TextSecondary,
            letterSpacing = 1.2.sp
        )
        Spacer(Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            MetricCard(
                title = "Faculty Strength",
                value = "24 Active",
                subtext = "2 Leave applications",
                icon = Icons.Outlined.School,
                color = Navy,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "Syllabus Status",
                value = "74% Covered",
                subtext = "Aligned with schedule",
                icon = Icons.AutoMirrored.Outlined.ListAlt,
                color = Gold,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(16.dp))
        ActionCard(
            title = "Department Review & Approvals",
            desc = "Review course logs, faculty requests and budget items",
            icon = Icons.Outlined.Dashboard,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun TeacherDashboardView() {
    Column {
        Text(
            text = "TEACHER / MENTOR CLASSROOM",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = TextSecondary,
            letterSpacing = 1.2.sp
        )
        Spacer(Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            MetricCard(
                title = "Today's Lectures",
                value = "3 Scheduled",
                subtext = "Next: 11:30 AM (Lab 3)",
                icon = Icons.Outlined.Class,
                color = NavySoft,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "Mentee Count",
                value = "22 Students",
                subtext = "Weekly meet pending",
                icon = Icons.Outlined.Person,
                color = SuccessGreen,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(16.dp))
        ActionCard(
            title = "Mark Quick Attendance",
            desc = "Open scanner or manual roster check-in",
            icon = Icons.Outlined.PostAdd,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    subtext: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = color.copy(alpha = 0.1f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(text = value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(Modifier.height(4.dp))
            Text(text = title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
            Spacer(Modifier.height(2.dp))
            Text(text = subtext, fontSize = 11.sp, color = TextSecondary.copy(alpha = 0.8f))
        }
    }
}

@Composable
fun MetricCardFullWidth(
    title: String,
    value: String,
    progress: Float,
    icon: ImageVector
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Gold.copy(alpha = 0.1f),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = GoldDeep, modifier = Modifier.size(22.dp))
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
                Spacer(Modifier.height(4.dp))
                Text(text = value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape),
                    color = Gold,
                    trackColor = DividerSoft
                )
            }
        }
    }
}

@Composable
fun ActionCard(
    title: String,
    desc: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }
    val cardScale by animateFloatAsState(if (isPressed) 0.97f else 1f, label = "actionScale")

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
            .scale(cardScale)
            .clickable {
                isPressed = true
                isPressed = false
            }
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Navy,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = desc,
                fontSize = 11.sp,
                color = TextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 14.sp
            )
        }
    }
}

@Composable
fun NoticeListItem(
    tag: String,
    title: String,
    time: String
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .background(
                        color = if (tag == "CRITICAL") Color(0xFFFEE2E2) else Slate,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = tag,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (tag == "CRITICAL") Color(0xFFEF4444) else NavySoft
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = time,
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }
        }
    }
}

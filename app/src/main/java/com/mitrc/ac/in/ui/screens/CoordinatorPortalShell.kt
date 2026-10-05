package com.mitrc.ac.`in`.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.AssignmentInd
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.Class
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
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

sealed class CoordinatorTab(val route: String, val label: String, val icon: ImageVector) {
    data object Dashboard : CoordinatorTab("dashboard", "Overview", Icons.Outlined.Dashboard)
    data object Timetable : CoordinatorTab("timetable", "Schedule", Icons.Outlined.CalendarMonth)
    data object Faculty : CoordinatorTab("faculty", "Staff", Icons.Outlined.Badge)
    data object Notices : CoordinatorTab("notices", "Broadcast", Icons.Outlined.Campaign)
}

@Composable
fun CoordinatorPortalShell(
    userUid: String,
    onSignedOut: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableStateOf<CoordinatorTab>(CoordinatorTab.Dashboard) }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = SurfaceWhite,
                tonalElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                val tabs = listOf(
                    CoordinatorTab.Dashboard,
                    CoordinatorTab.Timetable,
                    CoordinatorTab.Faculty,
                    CoordinatorTab.Notices
                )

                tabs.forEach { tab ->
                    val isSelected = selectedTab == tab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { selectedTab = tab },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.label,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = {
                            Text(
                                text = tab.label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Navy,
                            selectedTextColor = Navy,
                            indicatorColor = Gold.copy(alpha = 0.2f),
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Slate)
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    fadeIn().togetherWith(fadeOut())
                },
                label = "CoordinatorPortalContent"
            ) { targetTab ->
                when (targetTab) {
                    CoordinatorTab.Dashboard -> {
                        CoordinatorDashboardView(
                            onSignOut = {
                                scope.launch {
                                    AuthRepository.signOut()
                                    onSignedOut()
                                }
                            }
                        )
                    }
                    CoordinatorTab.Timetable -> {
                        Box(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                            CoordinatorTimetableView(userUid = userUid)
                        }
                    }
                    CoordinatorTab.Faculty -> {
                        PlaceholderView("Faculty Directory & Assignment")
                    }
                    CoordinatorTab.Notices -> {
                        PlaceholderView("Official Notices & Circulars")
                    }
                }
            }
        }
    }
}

@Composable
fun CoordinatorDashboardView(onSignOut: () -> Unit) {
    var entranceVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(100)
        entranceVisible = true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Premium Mesh Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(listOf(NavyDeep, Navy)),
                    shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
                )
                .padding(top = 54.dp, bottom = 40.dp, start = 24.dp, end = 24.dp)
        ) {
            // Ambient Decorative Circle
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(160.dp)
                    .offset(x = 40.dp, y = (-50).dp)
                    .background(Gold.copy(alpha = 0.08f), CircleShape)
            )

            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "MANAGEMENT DESK",
                            color = GoldLight,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Hi, Coordinator",
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.5).sp
                        )
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
                
                Spacer(Modifier.height(18.dp))
                
                Text(
                    text = "Welcome to your workspace. Monitor academics and manage faculty schedules from one central place.",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }
        }

        AnimatedVisibility(
            visible = entranceVisible,
            enter = fadeIn(animationSpec = tween(600)) + slideInVertically(
                initialOffsetY = { 40 },
                animationSpec = tween(600, easing = FastOutSlowInEasing)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = "ACADEMIC SNAPSHOT",
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
                        title = "Students",
                        value = "426",
                        subtext = "Active III/IV Yr",
                        icon = Icons.Outlined.Group,
                        color = NavySoft,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Attendance",
                        value = "89.4%",
                        subtext = "Daily Average",
                        icon = Icons.Outlined.Analytics,
                        color = SuccessGreen,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(14.dp))

                MetricCardFullWidth(
                    title = "Academic Notice Approvals Pending",
                    value = "4 Drafts for Review",
                    progress = 0.65f,
                    icon = Icons.Outlined.NotificationsActive
                )

                Spacer(Modifier.height(28.dp))

                Text(
                    text = "QUICK MANAGEMENT ACTIONS",
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
                        title = "Broadcast",
                        desc = "Push notices",
                        icon = Icons.Outlined.Campaign,
                        modifier = Modifier.weight(1f)
                    )
                    ActionCard(
                        title = "Timetable",
                        desc = "Edit schedules",
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
                        title = "Assign Staff",
                        desc = "Faculty links",
                        icon = Icons.Outlined.AssignmentInd,
                        modifier = Modifier.weight(1f)
                    )
                    ActionCard(
                        title = "Exams",
                        desc = "Mid-term dates",
                        icon = Icons.Outlined.Class,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(32.dp))

                Button(
                    onClick = onSignOut,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFEE2E2),
                        contentColor = Color(0xFFDC2626)
                    )
                ) {
                    Icon(Icons.AutoMirrored.Outlined.Logout, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(text = "Sign Out from Portal", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun MetricCard(
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
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(color.copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text(text = value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(Modifier.height(2.dp))
            Text(text = title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
            Text(text = subtext, fontSize = 10.sp, color = TextSecondary.copy(alpha = 0.7f))
        }
    }
}

@Composable
private fun MetricCardFullWidth(
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
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(Gold.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = GoldDeep, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
                Spacer(Modifier.height(4.dp))
                Text(text = value, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
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
private fun ActionCard(
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
            Spacer(Modifier.height(10.dp))
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = desc,
                fontSize = 11.sp,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 14.sp
            )
        }
    }
}

@Composable
private fun NoticeListItem(
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

@Composable
private fun PlaceholderView(title: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Outlined.Analytics,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = Navy.copy(alpha = 0.3f)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Navy.copy(alpha = 0.6f)
            )
            Text(
                text = "Module coming soon in the next update.",
                fontSize = 13.sp,
                color = Color.Gray
            )
        }
    }
}

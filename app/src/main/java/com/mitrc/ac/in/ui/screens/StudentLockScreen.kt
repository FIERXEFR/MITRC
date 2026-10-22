package com.mitrc.ac.`in`.ui.screens

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mitrc.ac.`in`.R
import com.mitrc.ac.`in`.data.StudentCoordinatorContact
import com.mitrc.ac.`in`.data.StudentProfileView
import com.mitrc.ac.`in`.ui.theme.Gold
import com.mitrc.ac.`in`.ui.theme.GoldLight
import com.mitrc.ac.`in`.ui.theme.Navy
import com.mitrc.ac.`in`.ui.theme.NavyDeep
import com.mitrc.ac.`in`.ui.theme.TextPrimary
import com.mitrc.ac.`in`.ui.theme.TextSecondary
import kotlinx.coroutines.delay

/**
 * Full-screen, non-dismissable state shown when a coordinator locked the profile or scheduled a
 * PTM. It is rendered by [StudentPortalShell] instead of the portal itself, so the tabs, header
 * and sign-out flow are unreachable while it is visible.
 *
 * Two things can dismiss it: the coordinator unlocking the profile (the screen re-checks
 * [onRefresh] in the background, and the shell recomposes out of this state) or the student
 * leaving the app with "Exit".
 */
@Composable
internal fun StudentLockScreen(
    profile: StudentProfileView,
    coordinator: StudentCoordinatorContact?,
    onRefresh: suspend () -> Unit
) {
    val context = LocalContext.current

    val locked = profile.isLocked == true
    val ptmAt = formatPtmSchedule(profile.ptmSchedule)
    val coordinatorName = coordinator?.name?.takeIf { it.isNotBlank() }

    val onCall: (String) -> Unit = { number ->
        val cleaned = number.filter { it.isDigit() || it == '+' }
        runCatching {
            context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleaned")))
        }
    }
    val onMail: (String) -> Unit = { address ->
        runCatching {
            context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$address")))
        }
    }

    // The only back gesture on this screen is swallowed: there is nothing behind it to return to.
    BackHandler { }

    // Quiet poll so the screen closes on its own the moment the coordinator restores access.
    LaunchedEffect(profile.studentUid, profile.isLocked, profile.isPtm) {
        while (true) {
            delay(20_000)
            runCatching { onRefresh() }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(NavyDeep, Navy)))
    ) {
        // The same gold orbs as the login and portal headers, at a lower opacity so the content
        // stays the only focal point.
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 70.dp, y = (-80).dp)
                .size(220.dp)
                .background(Gold.copy(alpha = 0.09f), CircleShape)
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = (-90).dp, y = 110.dp)
                .size(210.dp)
                .background(Gold.copy(alpha = 0.05f), CircleShape)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(R.drawable.logo_mitrc_white),
                contentDescription = "MITRC logo",
                contentScale = ContentScale.Fit,
                alignment = Alignment.CenterStart,
                modifier = Modifier
                    .width(74.dp)
                    .height(16.dp)
            )

            Spacer(Modifier.height(30.dp))

            LockBadge(locked = locked)

            Spacer(Modifier.height(18.dp))

            if (!locked) {
                Text(
                    text = "PTM SCHEDULED",
                    color = GoldLight,
                    fontSize = 10.sp,
                    letterSpacing = 1.8.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
            }

            Text(
                text = if (locked) "Profile Locked" else "PTM Scheduled",
                color = Color.White,
                fontSize = 26.sp,
                lineHeight = 30.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.4).sp,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(10.dp))

            Text(
                text = if (locked) {
                    "Your Profile is locked by ${coordinatorName ?: "your class coordinator"}."
                } else {
                    "A Parent-Teacher Meeting has been scheduled for you."
                },
                color = Color.White.copy(alpha = 0.78f),
                fontSize = 13.5.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 6.dp)
            )

            val studentLabel = listOfNotNull(
                profile.name?.trim()?.takeIf { it.isNotEmpty() },
                profile.serialNo?.trim()?.takeIf { it.isNotEmpty() }?.let { "Roll No: $it" }
            ).joinToString(" • ")

            if (studentLabel.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = studentLabel,
                    color = Color.White.copy(alpha = 0.55f),
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(24.dp))

            if (locked) {
                val reason = profile.lockReason?.trim()?.takeIf { it.isNotEmpty() }
                LockCard {
                    LockCardHeading(
                        icon = Icons.Outlined.Info,
                        label = "REASON"
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = reason ?: "No reason was provided by the coordinator.",
                        color = if (reason != null) TextPrimary else TextSecondary,
                        fontSize = 13.5.sp,
                        lineHeight = 20.sp,
                        fontWeight = if (reason != null) FontWeight.Medium else FontWeight.Normal
                    )
                }
                Spacer(Modifier.height(12.dp))
            }

            if (ptmAt != null) {
                LockCard {
                    LockCardHeading(
                        icon = Icons.Outlined.CalendarMonth,
                        label = "PTM SCHEDULED FOR"
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = ptmAt,
                        color = TextPrimary,
                        fontSize = 16.sp,
                        lineHeight = 21.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Please be available at the scheduled time.",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )

                    Spacer(Modifier.height(14.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(TextSecondary.copy(alpha = 0.15f))
                    )
                    Spacer(Modifier.height(12.dp))

                    LockCardHeading(icon = Icons.Outlined.School, label = "COORDINATOR")
                    Spacer(Modifier.height(10.dp))
                    CoordinatorDetails(
                        coordinator = coordinator,
                        locked = locked,
                        onCall = onCall,
                        onMail = onMail
                    )
                }
                Spacer(Modifier.height(12.dp))
            } else {
                LockCard {
                    LockCardHeading(icon = Icons.Outlined.School, label = "COORDINATOR")
                    Spacer(Modifier.height(10.dp))
                    CoordinatorDetails(
                        coordinator = coordinator,
                        locked = locked,
                        onCall = onCall,
                        onMail = onMail
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    // Leaves the app; reopening re-runs the lock check, so nothing is bypassed.
                    (context as? Activity)?.finish()
                },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Gold,
                    contentColor = Navy
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    text = "Exit",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(14.dp))

            Text(
                text = "This screen closes automatically once your coordinator restores access.",
                color = Color.White.copy(alpha = 0.45f),
                fontSize = 11.sp,
                lineHeight = 15.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun LockBadge(locked: Boolean) {
    Box(
        modifier = Modifier
            .size(78.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.08f)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(78.dp)
                .clip(CircleShape)
                .background(Gold.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (locked) Icons.Outlined.Lock else Icons.Outlined.CalendarMonth,
                contentDescription = null,
                tint = GoldLight,
                modifier = Modifier.size(34.dp)
            )
        }
    }
}

@Composable
private fun LockCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp), content = content)
    }
}

@Composable
private fun LockCardHeading(icon: ImageVector, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = Navy, modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(7.dp))
        Text(
            text = label,
            color = TextSecondary,
            fontSize = 10.sp,
            letterSpacing = 1.3.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun CoordinatorDetails(
    coordinator: StudentCoordinatorContact?,
    locked: Boolean,
    onCall: (String) -> Unit,
    onMail: (String) -> Unit
) {
    val hasContact = coordinator != null && (
        !coordinator.phoneNo.isNullOrBlank() || !coordinator.email.isNullOrBlank()
        )

    if (coordinator == null || (!hasContact && coordinator.name.isNullOrBlank())) {
        Text(
            text = if (locked) {
                "Contact your class coordinator to restore access to your portal."
            } else {
                "Contact your class coordinator for details about the scheduled PTM."
            },
            color = TextSecondary,
            fontSize = 13.sp,
            lineHeight = 19.sp
        )
        return
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Brush.verticalGradient(listOf(Navy, NavyDeep))),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = coordinator.name?.firstOrNull()?.uppercaseChar()?.toString() ?: "C",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = coordinator.name ?: "Class Coordinator",
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2
            )
            val role = coordinator.designation?.trim()?.takeIf { it.isNotEmpty() } ?: "Coordinator"
            Text(
                text = role,
                color = TextSecondary,
                fontSize = 11.5.sp
            )
        }
    }

    if (hasContact) {
        Spacer(Modifier.height(14.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(TextSecondary.copy(alpha = 0.15f))
        )
        Spacer(Modifier.height(8.dp))

        coordinator.phoneNo?.takeIf { it.isNotBlank() }?.let { phone ->
            ContactLine(
                icon = Icons.Outlined.Call,
                label = "Phone",
                value = phone,
                onClick = { onCall(phone) }
            )
        }
        coordinator.email?.takeIf { it.isNotBlank() }?.let { mail ->
            ContactLine(
                icon = Icons.Outlined.Email,
                label = "Email",
                value = mail,
                onClick = { onMail(mail) }
            )
        }
    }
}

@Composable
private fun ContactLine(
    icon: ImageVector,
    label: String,
    value: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Navy, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(10.dp))
        Text(
            text = label,
            color = TextSecondary,
            fontSize = 11.5.sp,
            modifier = Modifier.width(46.dp)
        )
        Text(
            text = value,
            color = TextPrimary,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
    }
}

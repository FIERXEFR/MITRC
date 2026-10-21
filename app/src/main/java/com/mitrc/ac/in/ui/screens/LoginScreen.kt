package com.mitrc.ac.`in`.ui.screens

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Login
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mitrc.ac.`in`.R
import com.mitrc.ac.`in`.auth.AdminRepository
import com.mitrc.ac.`in`.auth.AuthRepository
import com.mitrc.ac.`in`.data.OnboardingStore
import com.mitrc.ac.`in`.data.PasswordResetStore
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private const val TAG = "LoginScreen"

/**
 * @param onLoggedIn routed to the normal student/co-ordinator portal.
 * @param onAdminLoggedIn routed to [com.mitrc.ac.in.AdminPanelActivity] when the signed-in UID
 * has a row in the `admin_db` table.
 */
@Composable
fun LoginScreen(onLoggedIn: () -> Unit, onAdminLoggedIn: () -> Unit) {
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var isStaff by rememberSaveable { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    var sendingReset by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }

    fun signIn() {
        error = null
        notice = null
        val mail = email.trim()
        when {
            !mail.contains('@') || !mail.contains('.') -> {
                error = "Enter a valid email address"
                return
            }

            password.length < 6 -> {
                error = "Password must be at least 6 characters"
                return
            }
        }
        focusManager.clearFocus()
        loading = true
        scope.launch {
            val result = AuthRepository.signIn(mail, password)
            result.fold(
                onSuccess = { user ->
                    OnboardingStore.markCompleted()
                    // Mint a fresh ID token before the portal's first Supabase queries. Without
                    // this the student path can fire requests with a token Firebase has not
                    // finished issuing yet, which surfaces as a JWT error on first login.
                    runCatching { user.getIdToken(true).await() }

                    if (!isStaff) {
                        loading = false
                        onLoggedIn()
                    } else {
                        when (val gate = AdminRepository.resolveStaffGate(user.uid, user.email)) {
                            AdminRepository.StaffGate.Admin -> {
                                loading = false
                                onAdminLoggedIn()
                            }

                            AdminRepository.StaffGate.Staff -> {
                                loading = false
                                onLoggedIn()
                            }

                            AdminRepository.StaffGate.Neither -> {
                                // faculty_master (and the other staff probes) are gated by the
                                // `is_college_user()` RLS policy, so a legitimate staff account can
                                // still come back unmatched here - e.g. an unlinked firebase_uid or a
                                // slow first token. Signing out and hard-blocking locked staff out of
                                // the portal. Sign in anyway and let HomeScreen's role router link the
                                // account and pick the right portal; RLS still protects every row.
                                Log.i(TAG, "Staff gate unmatched for uid=${user.uid}; deferring role to HomeScreen")
                                loading = false
                                onLoggedIn()
                            }

                            is AdminRepository.StaffGate.Failed -> {
                                // Transient auth/RLS/network failure while checking - the portal's own
                                // error recovery handles it, so don't bounce the user back to login.
                                Log.w(TAG, "Staff gate check failed (${gate.reason}); deferring role to HomeScreen")
                                loading = false
                                onLoggedIn()
                            }
                        }
                    }
                },
                onFailure = {
                    loading = false
                    error = AuthRepository.friendlyMessage(it)
                }
            )
        }
    }

    fun requestPasswordReset() {
        error = null
        notice = null
        val mail = email.trim()
        if (!mail.contains('@') || !mail.contains('.')) {
            error = "Enter your email above first, then tap Forgot password"
            return
        }
        val waitMs = PasswordResetStore.remainingMillis(mail)
        if (waitMs > 0L) {
            val minutes = ((waitMs + 59_999L) / 60_000L).coerceAtLeast(1L)
            error = "A reset link was already sent from this device. Try again in $minutes min."
            return
        }
        showResetDialog = true
    }

    fun resetPassword() {
        val mail = email.trim()
        error = null
        notice = null
        sendingReset = true
        scope.launch {
            val result = AuthRepository.sendPasswordReset(mail)
            sendingReset = false
            showResetDialog = false
            result.fold(
                onSuccess = {
                    PasswordResetStore.markSent(mail)
                    notice = "Password reset link sent to $mail"
                },
                onFailure = { error = AuthRepository.friendlyMessage(it) }
            )
        }
    }

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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(272.dp)
                    .background(
                        brush = Brush.verticalGradient(listOf(NavyDeep, Navy)),
                        shape = RoundedCornerShape(bottomStart = 36.dp, bottomEnd = 36.dp)
                    )
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(210.dp)
                        .offset(x = 72.dp, y = (-76).dp)
                        .background(Gold.copy(alpha = 0.09f), CircleShape)
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = painterResource(R.drawable.logo_mitrc_white),
                        contentDescription = "MITRC Alwar",
                        modifier = Modifier.width(205.dp)
                    )
                    Spacer(Modifier.height(22.dp))
                    Text(
                        text = "STAFF & STUDENT PORTAL",
                        color = GoldLight,
                        fontSize = 12.sp,
                        letterSpacing = 2.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Sign in to continue",
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 14.sp
                    )
                }
            }

            Box(
                modifier = Modifier
                    .offset(y = (-30).dp)
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
            ) {
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = SurfaceWhite,
                    shadowElevation = 14.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 28.dp)
                    ) {
                        LoginRoleTabs(
                            isStaff = isStaff,
                            onTabChange = { isStaff = it }
                        )

                        Spacer(Modifier.height(24.dp))

                        Text(
                            text = "Welcome back",
                            style = MaterialTheme.typography.headlineSmall,
                            color = TextPrimary
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = if (isStaff) {
                                "Staff login - use your registered college email"
                            } else {
                                "Student login - use your registered college email"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        Spacer(Modifier.height(26.dp))

                    OutlinedTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            error = null
                        },
                        label = { Text("Email address") },
                        singleLine = true,
                        leadingIcon = {
                            Icon(Icons.Outlined.Email, contentDescription = null, tint = Navy)
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = mitrcFieldColors()
                    )

                    Spacer(Modifier.height(16.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            error = null
                        },
                        label = { Text("Password") },
                        singleLine = true,
                        leadingIcon = {
                            Icon(Icons.Outlined.Lock, contentDescription = null, tint = Navy)
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                    contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                    tint = TextSecondary
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { signIn() }),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = mitrcFieldColors()
                    )

                    AnimatedVisibility(visible = error != null) {
                        Text(
                            text = error.orEmpty(),
                            color = ErrorRed,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 12.dp)
                        )
                    }

                    AnimatedVisibility(visible = notice != null) {
                        Text(
                            text = notice.orEmpty(),
                            color = SuccessGreen,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 12.dp)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = ::requestPasswordReset, enabled = !loading) {
                            Text("Forgot password?", color = Navy)
                        }
                    }

                    Button(
                        onClick = ::signIn,
                        enabled = !loading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
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
                                modifier = Modifier.size(22.dp),
                                color = GoldLight,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                Icons.Outlined.Login,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = "Sign In",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(Modifier.height(22.dp))

                    Surface(
                        color = Slate,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Outlined.Info,
                                contentDescription = null,
                                tint = Navy,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "No account yet?",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Navy
                                )
                                Text(
                                    text = "Contact your department admin to activate your ID.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
            }
            }

            Text(
                text = "© 2026 MITRC, Alwar. All Rights Reserved.",
                color = TextSecondary,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 28.dp)
            )
        }
    }

    if (showResetDialog) {
        ResetPasswordDialog(
            email = email.trim(),
            sending = sendingReset,
            onDismiss = { if (!sendingReset) showResetDialog = false },
            onConfirm = ::resetPassword
        )
    }
}

@Composable
private fun ResetPasswordDialog(
    email: String,
    sending: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    Dialog(
        onDismissRequest = { if (!sending) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
                .padding(horizontal = 32.dp),
            contentAlignment = Alignment.Center
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = scaleIn(
                    initialScale = 0.9f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                ) + fadeIn(tween(220)),
                exit = scaleOut(targetScale = 0.92f, animationSpec = tween(160)) + fadeOut(tween(140))
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = SurfaceWhite,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 22.dp, vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(Navy, NavyDeep))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Outlined.Lock,
                                contentDescription = null,
                                tint = GoldLight,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(Modifier.height(14.dp))

                        Text(
                            text = "Send reset link?",
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold
                        )

                        Spacer(Modifier.height(8.dp))

                        Text(
                            text = "We'll email a password reset link to\n$email",
                            color = TextSecondary,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            textAlign = TextAlign.Center
                        )

                        Spacer(Modifier.height(20.dp))

                        if (sending) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    color = Navy,
                                    strokeWidth = 2.5.dp,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = "Sending link...",
                                    color = TextSecondary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedButton(
                                    onClick = onDismiss,
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, DividerSoft),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(46.dp)
                                ) {
                                    Text("Cancel", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                }
                                Button(
                                    onClick = onConfirm,
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Navy,
                                        contentColor = Color.White
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(46.dp)
                                ) {
                                    Text("Send link", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LoginRoleTabs(
    isStaff: Boolean,
    onTabChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Slate, RoundedCornerShape(14.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        RoleTab(
            label = "Student",
            selected = !isStaff,
            onClick = { onTabChange(false) },
            modifier = Modifier.weight(1f)
        )
        RoleTab(
            label = "Staff",
            selected = isStaff,
            onClick = { onTabChange(true) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun RoleTab(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) Navy else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 11.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) Color.White else TextSecondary
        )
    }
}

@Composable
private fun mitrcFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Navy,
    unfocusedBorderColor = DividerSoft,
    focusedLabelColor = Navy,
    unfocusedLabelColor = TextSecondary,
    cursorColor = Navy,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    focusedLeadingIconColor = Navy,
    unfocusedLeadingIconColor = TextSecondary,
    unfocusedContainerColor = SurfaceWhite,
    focusedContainerColor = SurfaceWhite
)

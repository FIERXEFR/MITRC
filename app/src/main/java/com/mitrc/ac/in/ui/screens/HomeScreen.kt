package com.mitrc.ac.`in`.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.mitrc.ac.`in`.auth.AuthRepository
import com.mitrc.ac.`in`.data.PortalRepository
import com.mitrc.ac.`in`.data.PushRepository
import com.mitrc.ac.`in`.notifications.NotificationOpenRequest
import com.mitrc.ac.`in`.notifications.PushPermissionStore
import com.mitrc.ac.`in`.ui.theme.ErrorRed
import com.mitrc.ac.`in`.ui.theme.Gold
import com.mitrc.ac.`in`.ui.theme.GoldLight
import com.mitrc.ac.`in`.ui.theme.Navy
import com.mitrc.ac.`in`.ui.theme.NavyDeep
import com.mitrc.ac.`in`.ui.theme.NavySoft
import com.mitrc.ac.`in`.ui.theme.SurfaceWhite
import com.mitrc.ac.`in`.ui.theme.TextPrimary
import com.mitrc.ac.`in`.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class StaffRole {
    STUDENT, TEACHER, COORDINATOR, NOT_LINKED, LOADING
}

private const val TAG = "HomeScreen"

/**
 * Smart Router for the MITRC App.
 * Detects role from Supabase schema and delegates to the appropriate Portal Shell.
 */
@Composable
fun HomeScreen(onSignedOut: () -> Unit) {
    val user = AuthRepository.currentUser
    val userUid = user?.uid.orEmpty()
    var resolvedRole by remember { mutableStateOf(StaffRole.LOADING) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isNotificationPermissionGranted by remember {
        mutableStateOf(hasNotificationPermission(context))
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val permissionOk = granted && hasNotificationPermission(context)
        isNotificationPermissionGranted = permissionOk
        if (permissionOk) {
            scope.launch {
                PushRepository.registerCurrentDevice(PushRepository.appVersion(context))
            }
        }
    }

    // Returning from the system Settings screen does not re-run the launcher
    // callback, so re-read the permission whenever the app comes back.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isNotificationPermissionGranted = hasNotificationPermission(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(userUid) {
        if (userUid.isEmpty()) {
            onSignedOut()
            return@LaunchedEffect
        }

        runCatching {
            PortalRepository.linkMyFacultyAccount()
        }.onFailure {
            Log.w(TAG, "link_my_faculty_account failed: ${it.message}")
        }

        val userEmail = user?.email.orEmpty()
        Log.d(TAG, "Resolving role for userUid=$userUid, email=$userEmail")

        val portals = PortalRepository.getMyPortals().getOrNull()
        Log.d(TAG, "my_portals: $portals")

        // `my_portals` is the single authoritative role RPC. When it succeeds, trust its flags
        // and return immediately; the old fallback probes (coordinator classes, staff directory,
        // faculty lookup, student profile) were only needed when that RPC was broken or a row had
        // not been linked yet. Running them for every student added four or five serial requests
        // before the skeleton could even start.
        if (portals != null) {
            if (portals.isCoordinator) {
                Log.i(TAG, "Role resolved as COORDINATOR via my_portals")
                resolvedRole = StaffRole.COORDINATOR
                return@LaunchedEffect
            }
            if (portals.isTeacher || portals.isFaculty || portals.isMentor || portals.isHod) {
                Log.i(TAG, "Role resolved as TEACHER via my_portals")
                resolvedRole = StaffRole.TEACHER
                return@LaunchedEffect
            }
            if (portals.isStudent) {
                Log.i(TAG, "Role resolved as STUDENT via my_portals")
                resolvedRole = StaffRole.STUDENT
                return@LaunchedEffect
            }
        }

        // Fallbacks for a failed `my_portals` call or an older deployment whose RPC does not
        // expose a role yet.
        val myClassesRes = PortalRepository.getMyCoordinatorClasses()
        val myClasses = myClassesRes.getOrNull()
        Log.d(TAG, "v_my_coordinator_classes rows=${myClasses?.size}")
        if (!myClasses.isNullOrEmpty()) {
            Log.i(TAG, "Role resolved as COORDINATOR via v_my_coordinator_classes (${myClasses.size} classes)")
            resolvedRole = StaffRole.COORDINATOR
            return@LaunchedEffect
        }

        val coordsRes = PortalRepository.getCoordinatorsList()
        val coordinatorMatch = coordsRes.getOrNull()?.firstOrNull { c ->
            (!c.email.isNullOrBlank() && c.email.equals(userEmail, ignoreCase = true))
        }
        if (coordinatorMatch != null) {
            Log.i(TAG, "Role resolved as COORDINATOR via v_coordinators match (${coordinatorMatch.course})")
            resolvedRole = StaffRole.COORDINATOR
            return@LaunchedEffect
        }

        val staffRow = PortalRepository.getStaffDirectory().getOrNull()?.firstOrNull { row ->
            (!row.firebaseUid.isNullOrBlank() && row.firebaseUid == userUid) ||
                (!row.email.isNullOrBlank() && row.email.equals(userEmail, ignoreCase = true))
        }
        if (staffRow != null && staffRow.isCoordinator) {
            Log.i(TAG, "Role resolved as COORDINATOR via v_staff_directory (isCoordinator=true)")
            resolvedRole = StaffRole.COORDINATOR
            return@LaunchedEffect
        }

        val facultyRow = PortalRepository.getFacultyByUidOrEmail(userUid, userEmail).getOrNull()
        if (facultyRow != null) {
            Log.i(TAG, "Role resolved as TEACHER via faculty table match (id=${facultyRow.id})")
            if (facultyRow.firebaseUid.isNullOrBlank() && userEmail.isNotBlank()) {
                PortalRepository.linkFacultyUidByEmail(userUid, userEmail)
            }
            resolvedRole = StaffRole.TEACHER
            return@LaunchedEffect
        }

        if (staffRow != null) {
            Log.i(TAG, "Role resolved as TEACHER via v_staff_directory")
            resolvedRole = StaffRole.TEACHER
            return@LaunchedEffect
        }

        // 3. Check Student Profile
        val student = PortalRepository.getStudentProfile(userUid).getOrNull()
        if (student != null) {
            Log.i(TAG, "Role resolved as STUDENT via student profile")
            resolvedRole = StaffRole.STUDENT
            return@LaunchedEffect
        }

        Log.w(TAG, "No role matched for uid=$userUid, email=$userEmail. Blocking portal access")
        resolvedRole = StaffRole.NOT_LINKED
    }

    // Teacher and not-linked screens have no notifications sheet. Drop a pending
    // push deep-link so it cannot survive until a later student login. Keyed on
    // the request too, so a tap that arrives while the portal is already open is
    // still cleared.
    val openNotificationsRequest = NotificationOpenRequest.pending
    LaunchedEffect(openNotificationsRequest, resolvedRole) {
        if (openNotificationsRequest &&
            (resolvedRole == StaffRole.TEACHER || resolvedRole == StaffRole.NOT_LINKED)
        ) {
            NotificationOpenRequest.consume()
        }
    }

    // Verify permission once the account reaches a real portal. The mandatory
    // dialog below drives the actual request, so this only keeps state in sync.
    LaunchedEffect(resolvedRole) {
        if (resolvedRole == StaffRole.LOADING || resolvedRole == StaffRole.NOT_LINKED) {
            return@LaunchedEffect
        }
        isNotificationPermissionGranted = hasNotificationPermission(context)
    }

    // Register whenever the portal is ready and permission is granted, including
    // after the user enables notifications from system Settings.
    LaunchedEffect(resolvedRole, isNotificationPermissionGranted) {
        if (resolvedRole == StaffRole.LOADING || resolvedRole == StaffRole.NOT_LINKED) {
            return@LaunchedEffect
        }
        if (isNotificationPermissionGranted) {
            PushRepository.registerCurrentDevice(PushRepository.appVersion(context))
        }
    }

    when (resolvedRole) {
        StaffRole.LOADING -> {
            // The splash has already branded the launch. Keep the same portal background
            // instead of flashing a progress spinner while the role RPC settles.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(PortalBackground)
            )
        }
        StaffRole.STUDENT -> {
            StudentPortalShell(userUid = userUid, onSignedOut = onSignedOut)
        }
        StaffRole.TEACHER -> {
            TeacherPortalShell(userUid = userUid, onSignedOut = onSignedOut)
        }
        StaffRole.COORDINATOR -> {
            CoordinatorPortalShell(userUid = userUid, onSignedOut = onSignedOut)
        }
        StaffRole.NOT_LINKED -> {
            NotLinkedScreen(onSignedOut = onSignedOut)
        }
    }

    val isLoggedIn = resolvedRole == StaffRole.STUDENT ||
        resolvedRole == StaffRole.TEACHER ||
        resolvedRole == StaffRole.COORDINATOR

    if (isLoggedIn && !isNotificationPermissionGranted) {
        RequiredNotificationPermissionDialog(
            onAllow = {
                if (hasNotificationPermission(context)) {
                    // Granted while the dialog was up (e.g. from system Settings);
                    // just let the state change dismiss it.
                    isNotificationPermissionGranted = true
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val shouldShowRationale = context.findHostActivity()
                        ?.shouldShowRequestPermissionRationale(
                            Manifest.permission.POST_NOTIFICATIONS
                        ) ?: false

                    if (!shouldShowRationale && PushPermissionStore.wasPrompted(context)) {
                        // Permanently denied: the system prompt will not re-appear.
                        openNotificationSettings(context)
                    } else {
                        PushPermissionStore.markPrompted(context)
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                } else {
                    openNotificationSettings(context)
                }
            },
            onExit = {
                context.findHostActivity()?.finishAffinity()
            }
        )
    }
}

/**
 * Non-cancelable custom notification permission dialog styled as per the app theme.
 */
@Composable
private fun RequiredNotificationPermissionDialog(
    onAllow: () -> Unit,
    onExit: () -> Unit
) {
    BackHandler(enabled = true) { }
    Dialog(
        onDismissRequest = { },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.65f))
                .padding(horizontal = 28.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(26.dp),
                color = SurfaceWhite,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(NavySoft, NavyDeep))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.Notifications,
                            contentDescription = null,
                            tint = GoldLight,
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    Spacer(Modifier.height(18.dp))

                    Text(
                        text = "Notification Permission Required",
                        color = TextPrimary,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = "To receive official notices, timetable updates, and attendance alerts, notification access is required. Please allow permission to proceed.",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 18.5.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onExit,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed)
                        ) {
                            Text("Exit", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }

                        Button(
                            onClick = onAllow,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Navy,
                                contentColor = Color.White
                            )
                        ) {
                            Text("Allow", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}

/** True when the app can actually post notifications on this OS version. */
internal fun hasNotificationPermission(context: Context): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED &&
            NotificationManagerCompat.from(context).areNotificationsEnabled()
    } else {
        NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

/** Unwraps the Activity hosting this context (Compose may hand us a ContextWrapper). */
private fun Context.findHostActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return current as? Activity
}

/** Opens per-app notification settings, falling back to the app details page. */
internal fun openNotificationSettings(context: Context) {
    val notificationSettings = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching { context.startActivity(notificationSettings) }.onFailure {
        val appDetails = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(appDetails) }
    }
}

@Composable
private fun NotLinkedScreen(onSignedOut: () -> Unit) {
    val scope = rememberCoroutineScope()
    var signingOut by remember { mutableStateOf(false) }

    // Unlinked accounts never reach a portal: sign them out automatically within 3 seconds.
    LaunchedEffect(Unit) {
        delay(2500)
        if (!signingOut) {
            signingOut = true
            AuthRepository.signOut()
            onSignedOut()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Account not linked",
            style = MaterialTheme.typography.headlineSmall,
            color = TextPrimary
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Your account isn't linked to any student or staff record. You'll be signed out automatically. Contact the admin panel to activate your ID.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        if (signingOut) {
            CircularProgressIndicator(
                color = Gold,
                strokeWidth = 2.5.dp,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Signing out...",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        } else {
            Button(
                onClick = {
                    signingOut = true
                    scope.launch {
                        AuthRepository.signOut()
                        onSignedOut()
                    }
                },
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Sign Out Now")
            }
        }
    }
}

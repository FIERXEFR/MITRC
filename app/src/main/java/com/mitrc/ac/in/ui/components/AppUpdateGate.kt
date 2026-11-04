package com.mitrc.ac.`in`.ui.components

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.mitrc.ac.`in`.R
import com.mitrc.ac.`in`.data.AppUpdateRepository
import com.mitrc.ac.`in`.data.AppUpdateRow
import com.mitrc.ac.`in`.data.AppUpdateStore
import com.mitrc.ac.`in`.ui.theme.ErrorRed
import com.mitrc.ac.`in`.ui.theme.Gold
import com.mitrc.ac.`in`.ui.theme.GoldLight
import com.mitrc.ac.`in`.ui.theme.Navy
import com.mitrc.ac.`in`.ui.theme.NavyDeep
import com.mitrc.ac.`in`.ui.theme.SuccessGreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private sealed interface GateState {
    data object Clear : GateState
    data class Maintenance(val row: AppUpdateRow) : GateState
    data class BigUpdate(val row: AppUpdateRow) : GateState
    data class SmallUpdate(val row: AppUpdateRow) : GateState
}

/**
 * Startup/resume gate for maintenance and release control.
 *
 * Add it once at the root of the normal app activity. The admin activity intentionally bypasses
 * it, so an administrator can still sign in and turn maintenance off if the app is blocked.
 */
@Composable
fun AppUpdateGate() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    var row by remember { mutableStateOf<AppUpdateRow?>(null) }
    var loaded by remember { mutableStateOf(false) }
    var smallSeenVersion by remember { mutableStateOf(AppUpdateStore.smallVersion) }
    var dismissedBigVersion by remember { mutableStateOf(AppUpdateStore.dismissedBigVersion) }

    suspend fun refresh() {
        AppUpdateRepository.fetchLatest()
            .onSuccess { latest ->
                row = latest
                loaded = true
            }
    }

    LaunchedEffect(Unit) { refresh() }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                scope.launch { refresh() }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val state = if (!loaded) {
        GateState.Clear
    } else {
        resolveGateState(context, row, smallSeenVersion, dismissedBigVersion)
    }

    // If maintenance has an end time, poll while it is showing so the app recovers without a
    // force-stop once the administrator clears it.
    val maintenanceShowing = state is GateState.Maintenance
    LaunchedEffect(maintenanceShowing) {
        if (maintenanceShowing) {
            while (isActive) {
                delay(30_000)
                refresh()
            }
        }
    }

    when (state) {
        GateState.Clear -> Unit
        is GateState.Maintenance -> MaintenanceDialog(
            row = state.row,
            onRetry = { scope.launch { refresh() } }
        )

        is GateState.BigUpdate -> BigUpdateDialog(
            row = state.row,
            currentVersion = AppUpdateRepository.currentVersionName(context),
            onDismiss = {
                val version = state.row.bigVersion.orEmpty()
                AppUpdateStore.markBigDismissed(version)
                dismissedBigVersion = version
            },
            onUpdate = { AppUpdateRepository.openDownloadPage(context, state.row.downloadUrl) }
        )

        is GateState.SmallUpdate -> SmallUpdateDialog(
            row = state.row,
            onDismiss = {
                val version = state.row.smallVersion.orEmpty()
                AppUpdateStore.markSmallSeen(version, state.row.smallPayload)
                smallSeenVersion = version
            }
        )
    }
}

private fun resolveGateState(
    context: Context,
    row: AppUpdateRow?,
    smallSeenVersion: String,
    dismissedBigVersion: String,
): GateState {
    if (row == null) return GateState.Clear
    val status = row.appStatus?.trim()?.lowercase().orEmpty()
    if (status in setOf("inactive", "disabled", "draft", "off", "false")) return GateState.Clear

    val maintenance = (row.isMaintenance || status == "maintenance") &&
        !isMaintenanceExpired(row.maintenanceUntil)
    if (maintenance) return GateState.Maintenance(row)

    if (row.isBigPacket) {
        val currentVersion = AppUpdateRepository.currentVersionName(context)
        val remoteVersion = row.bigVersion.orEmpty()
        if (AppUpdateRepository.isNewer(remoteVersion, currentVersion)) {
            val dismissed = remoteVersion.equals(dismissedBigVersion, ignoreCase = true)
            if (row.isForce || !dismissed) return GateState.BigUpdate(row)
        }
    }

    if (AppUpdateRepository.isNewer(row.smallVersion, smallSeenVersion)) {
        return GateState.SmallUpdate(row)
    }

    return GateState.Clear
}

private fun isMaintenanceExpired(until: String?): Boolean {
    if (until.isNullOrBlank()) return false
    return runCatching {
        OffsetDateTime.parse(until).toInstant().isBefore(Instant.now())
    }.getOrDefault(false)
}

// ---------------------------------------------------------------------------------------------
// Maintenance
// ---------------------------------------------------------------------------------------------

@Composable
private fun MaintenanceDialog(row: AppUpdateRow, onRetry: () -> Unit) {
    val until = formatMaintenanceUntil(row.maintenanceUntil)
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(NavyDeep, Navy, Color(0xFF002E57))))
        ) {
            // Soft light pools keep the full-screen gate from reading as a flat slab.
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 140.dp, y = (-170).dp)
                    .size(360.dp)
                    .background(
                        Brush.radialGradient(
                            listOf(Gold.copy(alpha = 0.14f), Color.Transparent)
                        ),
                        CircleShape
                    )
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset(x = (-130).dp, y = 150.dp)
                    .size(330.dp)
                    .background(
                        Brush.radialGradient(
                            listOf(Color.White.copy(alpha = 0.06f), Color.Transparent)
                        ),
                        CircleShape
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 28.dp, vertical = 24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.logo_mitrc_white),
                    contentDescription = "MITRC",
                    modifier = Modifier.width(168.dp)
                )

                Spacer(Modifier.height(24.dp))
                StatusPill("SYSTEM MAINTENANCE", Gold)
                Spacer(Modifier.height(22.dp))

                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .border(1.dp, Gold.copy(alpha = 0.28f), CircleShape)
                        .padding(7.dp)
                        .background(
                            Brush.linearGradient(
                                listOf(Gold.copy(alpha = 0.22f), Color.Transparent)
                            ),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Build,
                        contentDescription = null,
                        tint = GoldLight,
                        modifier = Modifier.size(38.dp)
                    )
                }

                Spacer(Modifier.height(24.dp))
                Text(
                    text = row.maintenanceTitle?.blankToNull() ?: "We'll be right back",
                    color = Color.White,
                    fontSize = 27.sp,
                    lineHeight = 34.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = row.maintenanceMessage?.blankToNull()
                        ?: "MITRC Campus OS is being upgraded. Please check back in a little while.",
                    color = Color.White.copy(alpha = 0.72f),
                    fontSize = 14.sp,
                    lineHeight = 21.sp,
                    textAlign = TextAlign.Center
                )

                if (until != null) {
                    Spacer(Modifier.height(20.dp))
                    Surface(
                        color = Color.White.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Schedule,
                                contentDescription = null,
                                tint = GoldLight,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "EXPECTED BACK",
                                    color = Color.White.copy(alpha = 0.48f),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.2.sp
                                )
                                Text(
                                    text = until,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(28.dp))
                Button(
                    onClick = onRetry,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Gold,
                        contentColor = NavyDeep
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(19.dp)
                    )
                    Spacer(Modifier.width(9.dp))
                    Text("Check again", fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "Your data is safe. We're making things better.",
                    color = Color.White.copy(alpha = 0.46f),
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(20.dp))
                Text(
                    text = "MITRC CAMPUS OS",
                    color = Color.White.copy(alpha = 0.28f),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.8.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Update dialogs
// ---------------------------------------------------------------------------------------------

@Composable
private fun BigUpdateDialog(
    row: AppUpdateRow,
    currentVersion: String,
    onDismiss: () -> Unit,
    onUpdate: () -> Unit,
) {
    val forced = row.isForce
    val url = row.downloadUrl?.trim().orEmpty()
    val title = row.dialogTitle?.blankToNull()
        ?: if (forced) "A required update is ready" else "A new MITRC build is ready"

    UpdateDialogFrame(
        dismissible = !forced,
        onDismiss = onDismiss,
        icon = Icons.Outlined.SystemUpdate,
        eyebrow = if (forced) "REQUIRED UPDATE" else "RELEASE UPDATE",
        title = title,
        accentText = if (forced) "ACTION REQUIRED" else "RECOMMENDED",
        accentColor = if (forced) ErrorRed else Gold
    ) {
        Text(
            text = row.dialogBody?.blankToNull()
                ?: "Install the latest MITRC Campus OS build to keep using the portal.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.5.sp,
            lineHeight = 20.sp
        )

        VersionTransition(
            currentVersion = currentVersion,
            newVersion = row.bigVersion.orEmpty()
        )

        val whatsNew = row.whatsNew?.trim().orEmpty()
        if (whatsNew.isNotEmpty()) {
            WhatsNewPanel(whatsNew)
        }

        if (url.isEmpty()) {
            Surface(
                color = ErrorRed.copy(alpha = 0.10f),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = "No download link is configured. Please contact the MITRC admin team.",
                    color = ErrorRed,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        PrimaryDialogButton(
            text = if (forced) "Update now" else "Install update",
            icon = Icons.Outlined.Download,
            enabled = url.isNotEmpty(),
            onClick = onUpdate
        )

        if (!forced) {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Maybe later",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Text(
            text = if (forced) {
                "This build is required to continue. Your account and data stay the same."
            } else {
                "You can install this update later from the portal settings."
            },
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun SmallUpdateDialog(row: AppUpdateRow, onDismiss: () -> Unit) {
    val body = row.dialogBody?.blankToNull()
    val whatsNew = row.whatsNew?.trim().orEmpty()
    val title = row.dialogTitle?.blankToNull() ?: "A quick update is ready"
    val version = row.smallVersion?.trim().orEmpty()

    UpdateDialogFrame(
        dismissible = true,
        onDismiss = onDismiss,
        icon = Icons.Outlined.AutoAwesome,
        eyebrow = "QUICK UPDATE",
        title = title,
        accentText = "NO INSTALL NEEDED",
        accentColor = SuccessGreen
    ) {
        Text(
            text = body ?: "We've refreshed a few things behind the scenes. No APK download is needed.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.5.sp,
            lineHeight = 20.sp
        )

        if (version.isNotEmpty()) {
            Surface(
                color = Gold.copy(alpha = 0.11f),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Gold.copy(alpha = 0.22f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = null,
                        tint = Gold,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(Modifier.width(9.dp))
                    Text(
                        text = "Small packet",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = "v$version",
                        color = Gold,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (whatsNew.isNotEmpty()) {
            WhatsNewPanel(whatsNew)
        }

        PrimaryDialogButton(
            text = "Continue",
            icon = Icons.Outlined.Check,
            onClick = onDismiss
        )

        Text(
            text = "Applied instantly on this device. No Play Store or APK download required.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun UpdateDialogFrame(
    dismissible: Boolean,
    onDismiss: () -> Unit,
    icon: ImageVector,
    eyebrow: String,
    title: String,
    accentText: String?,
    accentColor: Color,
    content: @Composable ColumnScope.() -> Unit,
) {
    Dialog(
        onDismissRequest = { if (dismissible) onDismiss() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = dismissible,
            dismissOnClickOutside = dismissible,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.62f))
                .padding(horizontal = 20.dp, vertical = 28.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .widthIn(max = 480.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 24.dp
            ) {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    listOf(NavyDeep, Navy, Color(0xFF003A6B))
                                )
                            )
                            .padding(22.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .background(Gold.copy(alpha = 0.14f), CircleShape)
                                        .border(
                                            1.dp,
                                            Gold.copy(alpha = 0.26f),
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = GoldLight,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                                Spacer(Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = eyebrow,
                                        color = GoldLight.copy(alpha = 0.90f),
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.5.sp
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = title,
                                        color = Color.White,
                                        fontSize = 21.sp,
                                        lineHeight = 26.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                                if (dismissible) {
                                    IconButton(onClick = onDismiss) {
                                        Icon(
                                            imageVector = Icons.Outlined.Close,
                                            contentDescription = "Close",
                                            tint = Color.White.copy(alpha = 0.65f),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }

                            if (accentText != null) {
                                Spacer(Modifier.height(14.dp))
                                StatusPill(accentText, accentColor)
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Gold, Gold.copy(alpha = 0.30f), Color.Transparent)
                                )
                            )
                    )

                    Column(
                        modifier = Modifier.padding(22.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        content = content
                    )
                }
            }
        }
    }
}

@Composable
private fun VersionTransition(currentVersion: String, newVersion: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        VersionTile(
            label = "INSTALLED",
            value = "v$currentVersion",
            highlighted = false,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "→",
            color = Gold,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 9.dp)
        )
        VersionTile(
            label = "AVAILABLE",
            value = "v$newVersion",
            highlighted = true,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun VersionTile(
    label: String,
    value: String,
    highlighted: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = if (highlighted) {
            Gold.copy(alpha = 0.12f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.40f)
        },
        shape = RoundedCornerShape(16.dp),
        border = if (highlighted) BorderStroke(1.dp, Gold.copy(alpha = 0.24f)) else null
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(
                text = label,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.1.sp
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = value,
                color = if (highlighted) {
                    if (isSystemInDarkTheme()) GoldLight else Navy
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
private fun WhatsNewPanel(rawText: String) {
    val bullets = remember(rawText) {
        rawText.lines()
            .map { it.trim().removePrefix("•").removePrefix("-").trim() }
            .filter { it.isNotEmpty() }
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.AutoAwesome,
                    contentDescription = null,
                    tint = Gold,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "WHAT'S NEW",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )
            }

            Column(
                modifier = Modifier
                    .heightIn(max = 190.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                bullets.forEach { line ->
                    Row(verticalAlignment = Alignment.Top) {
                        Box(
                            modifier = Modifier
                                .padding(top = 6.dp)
                                .size(5.dp)
                                .background(Gold, CircleShape)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = line,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 12.5.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusPill(text: String, color: Color) {
    val contentColor = when (color) {
        SuccessGreen -> Color(0xFF86EFAC)
        ErrorRed -> Color(0xFFFCA5A5)
        else -> GoldLight
    }
    Surface(
        color = color.copy(alpha = 0.13f),
        shape = CircleShape,
        border = BorderStroke(1.dp, color.copy(alpha = 0.34f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .background(contentColor, CircleShape)
            )
            Spacer(Modifier.width(7.dp))
            Text(
                text = text,
                color = contentColor,
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            )
        }
    }
}

@Composable
private fun PrimaryDialogButton(
    text: String,
    icon: ImageVector,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val dark = isSystemInDarkTheme()
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (dark) Gold else Navy,
            contentColor = if (dark) NavyDeep else Color.White,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(19.dp)
        )
        Spacer(Modifier.width(9.dp))
        Text(text = text, fontWeight = FontWeight.Bold)
    }
}

private fun formatMaintenanceUntil(value: String?): String? {
    if (value.isNullOrBlank()) return null
    return runCatching {
        OffsetDateTime.parse(value)
            .atZoneSameInstant(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("dd MMM, h:mm a"))
    }.getOrNull()
}

private fun String?.blankToNull(): String? = this?.trim()?.takeIf { it.isNotEmpty() }

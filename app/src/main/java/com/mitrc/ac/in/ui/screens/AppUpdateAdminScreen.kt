package com.mitrc.ac.`in`.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mitrc.ac.`in`.data.AppUpdateRepository
import com.mitrc.ac.`in`.data.AppUpdateRow
import com.mitrc.ac.`in`.data.AppUpdateStore
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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import java.time.OffsetDateTime

/**
 * Admin-side editor for the single `public.app_update` row.
 *
 * The normal app reads that row through [com.mitrc.ac.in.ui.components.AppUpdateGate]. This screen
 * is only reachable from [AdminPanelScreen], which itself is only opened after `is_admin()`.
 */
@Composable
fun AppUpdateAdminScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var loading by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }

    var published by remember { mutableStateOf(true) }
    var maintenance by remember { mutableStateOf(false) }
    var bigPacket by remember { mutableStateOf(false) }
    var force by remember { mutableStateOf(true) }

    var maintenanceTitle by remember { mutableStateOf("") }
    var maintenanceMessage by remember { mutableStateOf("") }
    var maintenanceUntil by remember { mutableStateOf("") }

    var bigVersion by remember { mutableStateOf("") }
    var downloadUrl by remember { mutableStateOf("") }
    var dialogTitle by remember { mutableStateOf("") }
    var dialogBody by remember { mutableStateOf("") }
    var whatsNew by remember { mutableStateOf("") }

    var smallVersion by remember { mutableStateOf("") }
    var smallPayload by remember { mutableStateOf("{}") }

    fun load(row: AppUpdateRow?) {
        published = row == null || row.appStatus?.lowercase() !in setOf("inactive", "disabled", "draft", "off")
        maintenance = row?.isMaintenance == true
        bigPacket = row?.isBigPacket == true
        force = row?.isForce ?: true

        maintenanceTitle = row?.maintenanceTitle.orEmpty()
        maintenanceMessage = row?.maintenanceMessage.orEmpty()
        maintenanceUntil = row?.maintenanceUntil.orEmpty()

        bigVersion = row?.bigVersion.orEmpty()
        downloadUrl = row?.downloadUrl.orEmpty()
        dialogTitle = row?.dialogTitle.orEmpty()
        dialogBody = row?.dialogBody.orEmpty()
        whatsNew = row?.whatsNew.orEmpty()

        smallVersion = row?.smallVersion.orEmpty()
        smallPayload = row?.smallPayload?.toString() ?: "{}"
    }

    LaunchedEffect(Unit) {
        AppUpdateRepository.fetchLatest()
            .onSuccess { load(it) }
            .onFailure { error = it.message ?: "Could not load app_update" }
        loading = false
    }

    fun save() {
        error = null
        notice = null

        if (bigPacket) {
            if (bigVersion.isBlank()) {
                error = "Big update needs a version (for example 1.1)."
                return
            }
            if (!downloadUrl.trim().startsWith("https://")) {
                error = "Big update needs an https:// download URL."
                return
            }
        }

        if (maintenanceUntil.isNotBlank()) {
            val parsed = runCatching { OffsetDateTime.parse(maintenanceUntil.trim()) }.getOrNull()
            if (parsed == null) {
                error = "Maintenance end must be ISO-8601, e.g. 2026-10-31T18:00:00+05:30."
                return
            }
        }

        val payload = if (smallPayload.isBlank()) {
            JsonObject(emptyMap())
        } else {
            runCatching { Json.parseToJsonElement(smallPayload) as? JsonObject }.getOrNull()
                ?: run {
                    error = "Small payload must be a valid JSON object."
                    return
                }
        }

        val row = AppUpdateRow(
            id = 1,
            isBigPacket = bigPacket,
            smallVersion = smallVersion.trim().takeIf { it.isNotEmpty() },
            bigVersion = bigVersion.trim().takeIf { it.isNotEmpty() },
            isForce = force,
            downloadUrl = downloadUrl.trim().takeIf { it.isNotEmpty() },
            appStatus = if (published) "active" else "inactive",
            dialogTitle = dialogTitle.trim().takeIf { it.isNotEmpty() },
            dialogBody = dialogBody.trim().takeIf { it.isNotEmpty() },
            whatsNew = whatsNew.trim().takeIf { it.isNotEmpty() },
            isMaintenance = maintenance,
            maintenanceTitle = maintenanceTitle.trim().takeIf { it.isNotEmpty() },
            maintenanceMessage = maintenanceMessage.trim().takeIf { it.isNotEmpty() },
            maintenanceUntil = maintenanceUntil.trim().takeIf { it.isNotEmpty() },
            smallPayload = payload
        )

        saving = true
        scope.launch {
            AppUpdateRepository.save(row).fold(
                onSuccess = {
                    notice = "App control saved."
                    AppUpdateStore.markBigDismissed("")
                },
                onFailure = { error = it.message ?: "Save failed" }
            )
            saving = false
        }
    }

    val currentVersion = AppUpdateRepository.currentVersionName(context)

    Box(modifier = Modifier.fillMaxSize().background(Slate)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(NavyDeep, Navy)))
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 18.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                    Column {
                        Text(
                            text = "App Control",
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "Updates & maintenance • installed v$currentVersion",
                            color = Color.White.copy(alpha = 0.68f),
                            fontSize = 12.sp
                        )
                    }
                }
            }

            if (loading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Navy)
                }
                return@Column
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                SectionCard("RELEASE STATUS") {
                    SwitchLine(
                        label = "Published",
                        supporting = "When off, the app ignores this row and continues normally.",
                        checked = published,
                        onCheckedChange = { published = it }
                    )
                }

                SectionCard("MAINTENANCE") {
                    SwitchLine(
                        label = "Maintenance mode",
                        supporting = "Blocks the normal app. Admin panel stays accessible.",
                        checked = maintenance,
                        onCheckedChange = { maintenance = it }
                    )
                    OutlinedTextField(
                        value = maintenanceTitle,
                        onValueChange = { maintenanceTitle = it },
                        label = { Text("Title") },
                        placeholder = { Text("We'll be right back") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = maintenanceMessage,
                        onValueChange = { maintenanceMessage = it },
                        label = { Text("Message") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = maintenanceUntil,
                        onValueChange = { maintenanceUntil = it },
                        label = { Text("Ends at (optional, ISO-8601)") },
                        placeholder = { Text("2026-10-31T18:00:00+05:30") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                SectionCard("BIG UPDATE (APK)") {
                    SwitchLine(
                        label = "Big packet",
                        supporting = "Shows when the server version is newer than installed v$currentVersion.",
                        checked = bigPacket,
                        onCheckedChange = { bigPacket = it }
                    )
                    OutlinedTextField(
                        value = bigVersion,
                        onValueChange = { bigVersion = it },
                        label = { Text("New version name") },
                        placeholder = { Text("1.1") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = downloadUrl,
                        onValueChange = { downloadUrl = it },
                        label = { Text("APK download URL") },
                        placeholder = { Text("https://...") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        modifier = Modifier.fillMaxWidth()
                    )
                    SwitchLine(
                        label = "Force update",
                        supporting = "Force hides the Later button and blocks the app.",
                        checked = force,
                        onCheckedChange = { force = it }
                    )
                }

                SectionCard("SMALL PACKET (REMOTE CONFIG)") {
                    OutlinedTextField(
                        value = smallVersion,
                        onValueChange = { smallVersion = it },
                        label = { Text("Small version") },
                        placeholder = { Text("1.0.1") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = dialogTitle,
                        onValueChange = { dialogTitle = it },
                        label = { Text("Dialog title (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = dialogBody,
                        onValueChange = { dialogBody = it },
                        label = { Text("Dialog body (optional)") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = whatsNew,
                        onValueChange = { whatsNew = it },
                        label = { Text("What's new") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = smallPayload,
                        onValueChange = { smallPayload = it },
                        label = { Text("Payload JSON") },
                        supportingText = {
                            Text("Feature flags / copy read by AppRemoteConfig, e.g. {\"support_email\":\"...\"}")
                        },
                        minLines = 4,
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (error != null) {
                    Text(
                        text = error.orEmpty(),
                        color = ErrorRed,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                if (notice != null) {
                    Text(
                        text = notice.orEmpty(),
                        color = SuccessGreen,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Button(
                    onClick = ::save,
                    enabled = !saving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Navy,
                        contentColor = Color.White
                    )
                ) {
                    if (saving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = GoldLight,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Outlined.Save, contentDescription = null, modifier = Modifier.size(19.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Save App Control", fontWeight = FontWeight.Bold)
                    }
                }

                Text(
                    text = "Small packets are remote config, not new compiled code. Anything you want to change with them must already read AppRemoteConfig.",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(bottom = 10.dp)
                )
            }
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Surface(
        color = SurfaceWhite,
        shape = RoundedCornerShape(18.dp),
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.CloudUpload,
                    contentDescription = null,
                    tint = Gold,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = title,
                    color = TextPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.1.sp
                )
            }
            content()
        }
    }
}

@Composable
private fun SwitchLine(
    label: String,
    supporting: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(text = supporting, color = TextSecondary, fontSize = 11.sp, lineHeight = 15.sp)
        }
        Spacer(Modifier.width(10.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Navy
            )
        )
    }
}

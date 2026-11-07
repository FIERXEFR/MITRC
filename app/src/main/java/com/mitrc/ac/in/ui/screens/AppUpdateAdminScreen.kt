package com.mitrc.ac.`in`.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.mitrc.ac.`in`.data.AppUpdateRepository
import com.mitrc.ac.`in`.data.AppUpdateRow
import com.mitrc.ac.`in`.data.AppUpdateStore
import com.mitrc.ac.`in`.data.BranchRow
import com.mitrc.ac.`in`.data.ClassGroupRow
import com.mitrc.ac.`in`.data.ClassRow
import com.mitrc.ac.`in`.data.CourseRow
import com.mitrc.ac.`in`.data.NewNotificationRow
import com.mitrc.ac.`in`.data.NotificationRow
import com.mitrc.ac.`in`.auth.AdminRepository
import com.mitrc.ac.`in`.ui.theme.DividerSoft
import com.mitrc.ac.`in`.ui.theme.ErrorRed
import com.mitrc.ac.`in`.ui.theme.Gold
import com.mitrc.ac.`in`.ui.theme.GoldLight
import com.mitrc.ac.`in`.ui.theme.Navy
import com.mitrc.ac.`in`.ui.theme.Slate
import com.mitrc.ac.`in`.ui.theme.SuccessGreen
import com.mitrc.ac.`in`.ui.theme.SurfaceWhite
import com.mitrc.ac.`in`.ui.theme.TextPrimary
import com.mitrc.ac.`in`.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** The three independent tools of the Control Panel, edited as sub-sections. */
enum class AppManagementSection(val label: String) {
    UPDATE("Update"),
    MAINTENANCE("Maintenance"),
    NOTIFICATIONS("Notifications"),
}

/**
 * "Control Panel" tab of the admin console.
 *
 * Row 1 (`app_update`) and row 2 (`app_maintenance`) are edited in separate sub-sections, each
 * with its own save button. A save writes only its own row, so toggling maintenance can never
 * overwrite release settings (and vice versa). The Notifications sub-section posts a row into
 * `public.notifications`, which the existing database trigger fans out through `send-push`.
 *
 * The normal app reads both app rows through [com.mitrc.ac.in.ui.components.AppUpdateGate].
 */
@Composable
fun AdminAppManagementTab() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var section by remember { mutableStateOf(AppManagementSection.UPDATE) }

    var loading by remember { mutableStateOf(true) }
    var savingUpdate by remember { mutableStateOf(false) }
    var savingMaintenance by remember { mutableStateOf(false) }
    var loadError by remember { mutableStateOf<String?>(null) }

    // Row 1 - release / update.
    var published by remember { mutableStateOf(true) }
    var bigPacket by remember { mutableStateOf(false) }
    var force by remember { mutableStateOf(true) }
    var bigVersion by remember { mutableStateOf("") }
    var downloadUrl by remember { mutableStateOf("") }
    var dialogTitle by remember { mutableStateOf("") }
    var dialogBody by remember { mutableStateOf("") }
    var whatsNew by remember { mutableStateOf("") }
    var smallVersion by remember { mutableStateOf("") }
    var smallPayload by remember { mutableStateOf("{}") }
    var updateError by remember { mutableStateOf<String?>(null) }
    var updateNotice by remember { mutableStateOf<String?>(null) }

    // Row 2 - maintenance.
    var maintenance by remember { mutableStateOf(false) }
    var maintenanceTitle by remember { mutableStateOf("Under Maintenance") }
    var maintenanceMessage by remember { mutableStateOf("") }
    var maintenanceUntil by remember { mutableStateOf("") }
    var maintenanceError by remember { mutableStateOf<String?>(null) }
    var maintenanceNotice by remember { mutableStateOf<String?>(null) }

    fun applyUpdate(row: AppUpdateRow) {
        published = row.appStatus?.trim()?.lowercase() !in setOf("inactive", "disabled", "draft", "off")
        bigPacket = row.isBigPacket
        force = row.isForce
        bigVersion = row.bigVersion.orEmpty()
        downloadUrl = row.downloadUrl.orEmpty()
        dialogTitle = row.dialogTitle.orEmpty()
        dialogBody = row.dialogBody.orEmpty()
        whatsNew = row.whatsNew.orEmpty()
        smallVersion = row.smallVersion.orEmpty()
        smallPayload = row.smallPayload.toString()
    }

    fun applyMaintenance(row: AppUpdateRow) {
        maintenance = row.isMaintenance
        maintenanceTitle = row.maintenanceTitle.orEmpty()
        maintenanceMessage = row.maintenanceMessage.orEmpty()
        maintenanceUntil = row.maintenanceUntil.orEmpty()
    }

    LaunchedEffect(Unit) {
        AppUpdateRepository.fetchConfig().fold(
            onSuccess = { config ->
                config.update?.let(::applyUpdate)
                config.maintenance?.let(::applyMaintenance)
            },
            onFailure = { loadError = it.message ?: "Could not load app control rows" }
        )
        loading = false
    }

    fun saveUpdate() {
        updateError = null
        updateNotice = null

        if (bigPacket) {
            if (bigVersion.isBlank()) {
                updateError = "Big update needs a version (for example 1.1)."
                return
            }
            if (!downloadUrl.trim().startsWith("https://")) {
                updateError = "Big update needs an https:// download URL."
                return
            }
        }

        val payload = if (smallPayload.isBlank()) {
            JsonObject(emptyMap())
        } else {
            runCatching { Json.parseToJsonElement(smallPayload) as? JsonObject }.getOrNull()
                ?: run {
                    updateError = "Small payload must be a valid JSON object."
                    return
                }
        }

        val row = AppUpdateRow(
            id = AppUpdateRepository.UPDATE_ROW_ID,
            configKey = AppUpdateRepository.CONFIG_UPDATE,
            isBigPacket = bigPacket,
            smallVersion = smallVersion.trim().takeIf { it.isNotEmpty() },
            bigVersion = bigVersion.trim().takeIf { it.isNotEmpty() },
            isForce = force,
            downloadUrl = downloadUrl.trim().takeIf { it.isNotEmpty() },
            appStatus = if (published) "active" else "inactive",
            dialogTitle = dialogTitle.trim().takeIf { it.isNotEmpty() },
            dialogBody = dialogBody.trim().takeIf { it.isNotEmpty() },
            whatsNew = whatsNew.trim().takeIf { it.isNotEmpty() },
            smallPayload = payload,
        )

        savingUpdate = true
        scope.launch {
            AppUpdateRepository.saveUpdate(row).fold(
                onSuccess = {
                    updateNotice = "Release settings saved to row 1."
                    // A new release must be evaluated again even if a previous one was dismissed.
                    AppUpdateStore.markBigDismissed("")
                },
                onFailure = { updateError = it.message ?: "Could not save release settings" }
            )
            savingUpdate = false
        }
    }

    fun saveMaintenance() {
        maintenanceError = null
        maintenanceNotice = null

        if (maintenanceUntil.isNotBlank()) {
            val parsed = runCatching { OffsetDateTime.parse(maintenanceUntil.trim()) }.getOrNull()
            if (parsed == null) {
                maintenanceError = "End time must be ISO-8601, e.g. 2026-10-31T18:00:00+05:30."
                return
            }
        }

        val row = AppUpdateRow(
            id = AppUpdateRepository.MAINTENANCE_ROW_ID,
            configKey = AppUpdateRepository.CONFIG_MAINTENANCE,
            isMaintenance = maintenance,
            maintenanceTitle = maintenanceTitle.trim().takeIf { it.isNotEmpty() },
            maintenanceMessage = maintenanceMessage.trim().takeIf { it.isNotEmpty() },
            maintenanceUntil = maintenanceUntil.trim().takeIf { it.isNotEmpty() },
        )

        savingMaintenance = true
        scope.launch {
            AppUpdateRepository.saveMaintenance(row).fold(
                onSuccess = {
                    maintenanceNotice = if (maintenance) {
                        "Maintenance is ON. Users are blocked on their next check."
                    } else {
                        "Maintenance is OFF. Users recover within ~30 seconds."
                    }
                },
                onFailure = { maintenanceError = it.message ?: "Could not save maintenance" }
            )
            savingMaintenance = false
        }
    }

    val currentVersion = AppUpdateRepository.currentVersionName(context)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        PortalSegmentedTabs(
            labels = AppManagementSection.entries.map { it.label },
            selectedIndex = AppManagementSection.entries.indexOf(section),
            onSelect = { section = AppManagementSection.entries[it] }
        )

        if (loading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Navy)
            }
            return@Column
        }

        if (loadError != null) {
            Surface(
                color = ErrorRed.copy(alpha = 0.08f),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = loadError.orEmpty(),
                    color = ErrorRed,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        when (section) {
            AppManagementSection.UPDATE -> {
                Text(
                    text = "Row 1 • config_key: ${AppUpdateRepository.CONFIG_UPDATE} • installed v$currentVersion",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )

                SectionCard("RELEASE STATUS") {
                    SwitchLine(
                        label = "Published",
                        supporting = "When off, the app ignores the release row and continues normally.",
                        checked = published,
                        onCheckedChange = { published = it }
                    )
                }

                SectionCard("BIG PACKET (APK)") {
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
                        colors = mitrcAdminFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = downloadUrl,
                        onValueChange = { downloadUrl = it },
                        label = { Text("APK download URL") },
                        placeholder = { Text("https://...") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        colors = mitrcAdminFieldColors(),
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
                        colors = mitrcAdminFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = dialogTitle,
                        onValueChange = { dialogTitle = it },
                        label = { Text("Dialog title (optional)") },
                        singleLine = true,
                        colors = mitrcAdminFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = dialogBody,
                        onValueChange = { dialogBody = it },
                        label = { Text("Dialog body (optional)") },
                        minLines = 2,
                        colors = mitrcAdminFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = whatsNew,
                        onValueChange = { whatsNew = it },
                        label = { Text("What's new") },
                        minLines = 3,
                        colors = mitrcAdminFieldColors(),
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
                        colors = mitrcAdminFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                StatusLine(error = updateError, notice = updateNotice)

                SaveButton(
                    label = "Save App Update",
                    saving = savingUpdate,
                    onClick = ::saveUpdate
                )

                Text(
                    text = "Small packets are remote config, not new compiled code: anything you change there must already read AppRemoteConfig.",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(bottom = 10.dp)
                )
            }

            AppManagementSection.MAINTENANCE -> {
                Text(
                    text = "Row 2 • config_key: ${AppUpdateRepository.CONFIG_MAINTENANCE}",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )

                SectionCard("MAINTENANCE MODE") {
                    SwitchLine(
                        label = "Maintenance mode",
                        supporting = "Blocks the normal app for students, teachers, coordinators and HODs. Admin panel stays accessible.",
                        checked = maintenance,
                        onCheckedChange = { maintenance = it }
                    )
                    OutlinedTextField(
                        value = maintenanceTitle,
                        onValueChange = { maintenanceTitle = it },
                        label = { Text("Title") },
                        placeholder = { Text("We'll be right back") },
                        singleLine = true,
                        colors = mitrcAdminFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = maintenanceMessage,
                        onValueChange = { maintenanceMessage = it },
                        label = { Text("Message") },
                        minLines = 2,
                        colors = mitrcAdminFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = maintenanceUntil,
                        onValueChange = { maintenanceUntil = it },
                        label = { Text("Ends at (optional, ISO-8601)") },
                        placeholder = { Text("2026-10-31T18:00:00+05:30") },
                        singleLine = true,
                        colors = mitrcAdminFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                StatusLine(error = maintenanceError, notice = maintenanceNotice)

                SaveButton(
                    label = "Save Maintenance",
                    saving = savingMaintenance,
                    onClick = ::saveMaintenance
                )

                Text(
                    text = "Maintenance writes only row 2, so release settings are never touched. Users recover automatically within ~30 seconds of switching it off.",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(bottom = 10.dp)
                )
            }

            AppManagementSection.NOTIFICATIONS -> NotificationComposerSection()
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Notifications
// ---------------------------------------------------------------------------------------------

private enum class NotificationAudience(val value: String, val label: String) {
    EVERYONE("everyone", "Everyone"),
    STUDENTS("all_students", "Students"),
    STAFF("all_staff", "Staff"),
    COURSE("course", "Course"),
    CLASS("class", "Class"),
    GROUP("group", "Group"),
}

private val NotificationCategories = listOf(
    "announcement" to "Announcement",
    "exam" to "Exam",
    "holiday" to "Holiday",
    "event" to "Event",
    "timetable" to "Timetable",
    "notes" to "Notes",
    "attendance" to "Attendance",
    "marks" to "Marks",
    "alert" to "Alert",
    "system" to "System",
)

private val NotificationPriorities = listOf(
    "normal" to "Normal",
    "high" to "High",
    "urgent" to "Urgent",
)

/**
 * Compose-and-post form for `public.notifications`.
 *
 * An insert is all that is needed: `trg_notifications_send_push` calls the `send-push` Edge
 * Function, which resolves the audience to Firebase UIDs, looks up their device tokens and
 * writes the delivery counters back onto the row. The form polls those counters so the admin
 * can see the result of a real on-device push test without leaving the panel.
 */
@Composable
private fun NotificationComposerSection() {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // The admin panel never prompts for POST_NOTIFICATIONS (that prompt belongs to the student /
    // teacher / coordinator portal), so this device may have notifications blocked. The push would
    // still be sent but never appear, which makes an on-device test look broken.
    var notificationsEnabled by remember { mutableStateOf(hasNotificationPermission(context)) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notificationsEnabled = hasNotificationPermission(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var title by rememberSaveable { mutableStateOf("") }
    var body by rememberSaveable { mutableStateOf("") }
    var audienceName by rememberSaveable { mutableStateOf(NotificationAudience.EVERYONE.name) }
    val audience = NotificationAudience.entries.firstOrNull { it.name == audienceName }
        ?: NotificationAudience.EVERYONE
    var category by rememberSaveable { mutableStateOf("announcement") }
    var priority by rememberSaveable { mutableStateOf("normal") }
    var expiresAt by rememberSaveable { mutableStateOf("") }

    var courses by remember { mutableStateOf<List<CourseRow>>(emptyList()) }
    var selectedCourse by remember { mutableStateOf<CourseRow?>(null) }
    var branches by remember { mutableStateOf<List<BranchRow>>(emptyList()) }
    var selectedBranch by remember { mutableStateOf<BranchRow?>(null) }
    var classes by remember { mutableStateOf<List<ClassRow>>(emptyList()) }
    var selectedClass by remember { mutableStateOf<ClassRow?>(null) }
    var groups by remember { mutableStateOf<List<ClassGroupRow>>(emptyList()) }
    var selectedGroup by remember { mutableStateOf<ClassGroupRow?>(null) }
    var loadingTargets by remember { mutableStateOf(false) }

    var posting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    var delivery by remember { mutableStateOf<String?>(null) }
    var checkingDelivery by remember { mutableStateOf(false) }

    var recent by remember { mutableStateOf<List<NotificationRow>>(emptyList()) }
    var recentError by remember { mutableStateOf<String?>(null) }
    var cancellingId by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(Unit) {
        AdminRepository.getRecentNotifications().fold(
            onSuccess = { recent = it },
            onFailure = { recentError = it.message ?: "Could not load recent notices" },
        )
    }

    // Only load the pickers the current audience needs.
    LaunchedEffect(audience) {
        if (audience == NotificationAudience.COURSE ||
            audience == NotificationAudience.CLASS ||
            audience == NotificationAudience.GROUP
        ) {
            if (courses.isEmpty()) {
                loadingTargets = true
                courses = AdminRepository.getCourses()
                loadingTargets = false
            }
            if (selectedCourse == null) selectedCourse = courses.firstOrNull()
        }
    }

    LaunchedEffect(selectedCourse?.id, audience) {
        if (audience == NotificationAudience.CLASS || audience == NotificationAudience.GROUP) {
            val course = selectedCourse
            if (course == null) {
                branches = emptyList()
                selectedBranch = null
            } else {
                loadingTargets = true
                branches = AdminRepository.getBranches(course.id)
                if (selectedBranch?.courseId != course.id) {
                    selectedBranch = branches.firstOrNull()
                }
                loadingTargets = false
            }
        }
    }

    LaunchedEffect(selectedBranch?.id, audience) {
        if (audience == NotificationAudience.CLASS || audience == NotificationAudience.GROUP) {
            val branch = selectedBranch
            if (branch == null) {
                classes = emptyList()
                selectedClass = null
            } else {
                loadingTargets = true
                classes = AdminRepository.getClassesForBranch(branch.id)
                if (selectedClass?.branchId != branch.id) {
                    selectedClass = classes.firstOrNull()
                }
                loadingTargets = false
            }
        }
    }

    LaunchedEffect(selectedClass?.id, audience) {
        if (audience == NotificationAudience.GROUP) {
            val cls = selectedClass
            if (cls == null) {
                groups = emptyList()
                selectedGroup = null
            } else {
                loadingTargets = true
                groups = AdminRepository.getClassGroups(cls.id)
                if (selectedGroup?.classId != cls.id) {
                    selectedGroup = groups.firstOrNull()
                }
                loadingTargets = false
            }
        }
    }

    suspend fun pollDelivery(id: Long) {
        checkingDelivery = true
        var latest: NotificationRow? = null
        var attempts = 0
        while (attempts < 25) {
            delay(1_000)
            attempts += 1
            val row = AdminRepository.getNotificationDelivery(id).getOrNull() ?: continue
            latest = row
            if (row.pushSentAt != null) break
        }
        checkingDelivery = false

        val row = latest
        delivery = when {
            row == null ->
                "Notice posted, but the delivery counters could not be read. Reopen this tab to refresh."
            row.pushSentAt == null ->
                "Notice posted. The push function has not reported back yet; the retry job runs every 5 minutes."
            row.pushSentCount == 0 && row.pushFailedCount == 0 ->
                "Notice posted, but no registered device matched this audience. Sign in on a device with notifications allowed."
            else -> buildString {
                append("Push delivered to ")
                append(row.pushSentCount)
                append(if (row.pushSentCount == 1) " device" else " devices")
                if (row.pushFailedCount > 0) {
                    append(" - ")
                    append(row.pushFailedCount)
                    append(" failed")
                }
                row.pushError?.takeIf { it.isNotBlank() }?.let {
                    append(". ")
                    append(it)
                }
            }
        }

        if (row != null) {
            recent = recent.map { if (it.id == id) row else it }
        } else {
            recent = AdminRepository.getRecentNotifications().getOrDefault(recent)
        }
    }

    fun post() {
        error = null
        notice = null
        delivery = null

        val cleanTitle = title.trim()
        val cleanBody = body.trim()
        if (cleanTitle.isEmpty()) {
            error = "Enter a notification title."
            return
        }
        if (cleanTitle.length > 120) {
            error = "The title must be 120 characters or fewer."
            return
        }
        if (cleanBody.length > 2000) {
            error = "The body must be 2000 characters or fewer."
            return
        }

        val expiry = expiresAt.trim().takeIf { it.isNotEmpty() }
        if (expiry != null && runCatching { OffsetDateTime.parse(expiry) }.isFailure) {
            error = "Expiry must be ISO-8601, e.g. 2026-11-10T18:00:00+05:30."
            return
        }

        when (audience) {
            NotificationAudience.COURSE -> if (selectedCourse == null) {
                error = "Pick the course that should receive this notice."
                return
            }

            NotificationAudience.CLASS -> if (selectedClass == null) {
                error = "Pick the class that should receive this notice."
                return
            }

            NotificationAudience.GROUP -> if (selectedGroup == null) {
                error = "Pick the lab group that should receive this notice."
                return
            }

            else -> Unit
        }

        val row = NewNotificationRow(
            title = cleanTitle,
            body = cleanBody.takeIf { it.isNotEmpty() },
            category = category,
            priority = priority,
            audience = audience.value,
            courseId = if (audience == NotificationAudience.COURSE) selectedCourse?.id else null,
            classId = if (audience == NotificationAudience.CLASS) selectedClass?.id else null,
            groupId = if (audience == NotificationAudience.GROUP) selectedGroup?.id else null,
            expiresAt = expiry,
        )

        posting = true
        scope.launch {
            val result = AdminRepository.sendNotification(row)
            posting = false
            result.fold(
                onSuccess = { created ->
                    notice = "Notice #${created.id} posted. The push service is fanning it out now."
                    recent = listOf(created) + recent.filterNot { it.id == created.id }
                    title = ""
                    body = ""
                    expiresAt = ""
                },
                onFailure = {
                    error = it.message?.ifBlank { "Could not post the notice." }
                        ?: "Could not post the notice."
                },
            )
            result.getOrNull()?.let { pollDelivery(it.id) }
        }
    }

    fun cancel(id: Long) {
        cancellingId = id
        scope.launch {
            AdminRepository.cancelNotification(id).fold(
                onSuccess = {
                    recent = recent.map { if (it.id == id) it.copy(isCancelled = true) else it }
                    notice = "Notice #$id cancelled."
                },
                onFailure = { error = it.message ?: "Could not cancel the notice." },
            )
            cancellingId = null
        }
    }

    Text(
        text = "Posts a real notice and asks the existing send-push function to deliver it to every device registered for the chosen audience.",
        color = TextSecondary,
        fontSize = 11.sp,
        lineHeight = 16.sp,
    )

    if (!notificationsEnabled) {
        Surface(
            color = ErrorRed.copy(alpha = 0.08f),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "Notifications are blocked on this device, so a delivered notice will not appear here.",
                    color = ErrorRed,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                )
                TextButton(
                    onClick = { openNotificationSettings(context) },
                    contentPadding = PaddingValues(0.dp),
                ) {
                    Text(
                        text = "Open notification settings",
                        color = Navy,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }

    SectionCard("COMPOSE NOTICE") {
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Title *") },
            placeholder = { Text("Holiday tomorrow") },
            singleLine = true,
            colors = mitrcAdminFieldColors(),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = body,
            onValueChange = { body = it },
            label = { Text("Message") },
            placeholder = { Text("Classes resume on Monday at the usual time.") },
            minLines = 3,
            maxLines = 6,
            colors = mitrcAdminFieldColors(),
            modifier = Modifier.fillMaxWidth(),
        )

        Text(
            text = "AUDIENCE",
            color = TextSecondary,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            NotificationAudience.entries.forEach { option ->
                AdminChoiceChip(
                    label = option.label,
                    selected = audience == option,
                    onClick = { audienceName = option.name },
                )
            }
        }

        if (audience == NotificationAudience.COURSE ||
            audience == NotificationAudience.CLASS ||
            audience == NotificationAudience.GROUP
        ) {
            AdminPickerField(
                label = "Course",
                value = selectedCourse?.name,
                placeholder = if (loadingTargets && courses.isEmpty()) {
                    "Loading courses..."
                } else {
                    "Select a course"
                },
                icon = Icons.Outlined.School,
                options = courses.map { course -> course.name to { selectedCourse = course } },
            )
        }

        if (audience == NotificationAudience.CLASS || audience == NotificationAudience.GROUP) {
            AdminPickerField(
                label = "Branch",
                value = selectedBranch?.name,
                placeholder = if (branches.isEmpty()) {
                    "No branches for this course"
                } else {
                    "Select a branch"
                },
                icon = Icons.Outlined.School,
                options = branches.map { branch -> branch.name to { selectedBranch = branch } },
            )
            AdminPickerField(
                label = "Semester & section",
                value = selectedClass?.let(::classLabel),
                placeholder = if (classes.isEmpty()) {
                    "No classes for this branch"
                } else {
                    "Select a class"
                },
                icon = Icons.Outlined.School,
                options = classes.map { cls -> classLabel(cls) to { selectedClass = cls } },
            )
        }

        if (audience == NotificationAudience.GROUP) {
            AdminPickerField(
                label = "Lab group",
                value = selectedGroup?.let { "Group ${it.name}" },
                placeholder = if (groups.isEmpty()) {
                    "No groups for this class"
                } else {
                    "Select a group"
                },
                icon = Icons.Outlined.Groups,
                options = groups.map { group -> "Group ${group.name}" to { selectedGroup = group } },
            )
        }

        Text(
            text = "CATEGORY",
            color = TextSecondary,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            NotificationCategories.forEach { (value, label) ->
                AdminChoiceChip(
                    label = label,
                    selected = category == value,
                    onClick = { category = value },
                )
            }
        }

        Text(
            text = "PRIORITY",
            color = TextSecondary,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            NotificationPriorities.forEach { (value, label) ->
                AdminChoiceChip(
                    label = label,
                    selected = priority == value,
                    onClick = { priority = value },
                )
            }
        }

        OutlinedTextField(
            value = expiresAt,
            onValueChange = { expiresAt = it },
            label = { Text("Expires at (optional, ISO-8601)") },
            placeholder = { Text("2026-11-10T18:00:00+05:30") },
            singleLine = true,
            leadingIcon = { Icon(Icons.Outlined.Schedule, contentDescription = null, tint = Navy) },
            colors = mitrcAdminFieldColors(),
            modifier = Modifier.fillMaxWidth(),
        )
    }

    StatusLine(error = error, notice = notice)

    Button(
        onClick = ::post,
        enabled = !posting,
        modifier = Modifier.fillMaxWidth(),
    ) {
        if (posting) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = GoldLight,
                strokeWidth = 2.dp,
            )
        } else {
            Icon(Icons.Outlined.Send, contentDescription = null, modifier = Modifier.size(19.dp))
            Spacer(Modifier.width(8.dp))
            Text("Post notification", fontWeight = FontWeight.Bold)
        }
    }

    if (checkingDelivery || delivery != null) {
        Surface(
            color = Navy.copy(alpha = 0.06f),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (checkingDelivery) {
                    CircularProgressIndicator(
                        color = Navy,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                }
                Text(
                    text = delivery ?: "Waiting for the push service...",
                    color = TextPrimary,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                )
            }
        }
    }

    SectionCard("RECENT NOTICES") {
        when {
            recentError != null -> Text(
                text = recentError.orEmpty(),
                color = ErrorRed,
                fontSize = 12.sp,
                lineHeight = 17.sp,
            )

            recent.isEmpty() -> Text(
                text = "No notifications have been posted yet.",
                color = TextSecondary,
                fontSize = 12.sp,
            )

            else -> recent.forEachIndexed { index, item ->
                NotificationRecentItem(
                    item = item,
                    cancelling = cancellingId == item.id,
                    onCancel = { cancel(item.id) },
                )
                if (index != recent.lastIndex) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(DividerSoft)
                    )
                }
            }
        }
    }

    Text(
        text = "Tip: keep this device signed in with notifications allowed. The audience is resolved from the same account, so \"Everyone\" or \"Staff\" will include this admin device only after it has registered its FCM token.",
        color = TextSecondary,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        modifier = Modifier.padding(bottom = 10.dp),
    )
}

@Composable
private fun NotificationRecentItem(
    item: NotificationRow,
    cancelling: Boolean,
    onCancel: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier
                    .padding(top = 5.dp)
                    .size(8.dp)
                    .background(notificationPriorityColor(item.priority), CircleShape)
            )
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = buildString {
                        append(notificationAudienceLabel(item.audience))
                        formatNoticeTime(item.createdAt)?.let {
                            append(" • ")
                            append(it)
                        }
                    },
                    color = TextSecondary,
                    fontSize = 10.5.sp,
                    lineHeight = 14.sp,
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = notificationDeliveryLabel(item),
                color = notificationDeliveryColor(item),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        item.pushError
            ?.takeIf { it.isNotBlank() && item.pushSentCount == 0 && !item.isCancelled }
            ?.let {
                Text(
                    text = it,
                    color = ErrorRed,
                    fontSize = 10.sp,
                    lineHeight = 14.sp,
                )
            }

        if (!item.isCancelled) {
            TextButton(
                onClick = onCancel,
                enabled = !cancelling,
                contentPadding = PaddingValues(0.dp),
            ) {
                if (cancelling) {
                    CircularProgressIndicator(
                        color = Navy,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(14.dp),
                    )
                } else {
                    Text(
                        text = "Cancel notice",
                        color = ErrorRed,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun AdminChoiceChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        color = if (selected) Navy else Slate,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Text(
            text = label,
            color = if (selected) Color.White else TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun AdminPickerField(
    label: String,
    value: String?,
    placeholder: String,
    icon: ImageVector,
    options: List<Pair<String, () -> Unit>>,
) {
    var expanded by remember { mutableStateOf(false) }
    val enabled = options.isNotEmpty()

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value ?: placeholder,
            onValueChange = {},
            readOnly = true,
            enabled = false,
            label = { Text(label) },
            leadingIcon = { Icon(icon, contentDescription = null, tint = Navy) },
            trailingIcon = {
                Icon(Icons.Outlined.ArrowDropDown, contentDescription = null, tint = Navy)
            },
            shape = RoundedCornerShape(12.dp),
            colors = mitrcAdminFieldColors(),
            modifier = Modifier.fillMaxWidth(),
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable(enabled = enabled) { expanded = true }
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth(0.8f),
        ) {
            options.forEach { (optionLabel, onPick) ->
                DropdownMenuItem(
                    text = { Text(optionLabel, fontWeight = FontWeight.SemiBold) },
                    onClick = {
                        onPick()
                        expanded = false
                    },
                )
            }
        }
    }
}

private fun classLabel(cls: ClassRow): String =
    "Sem ${cls.semester} - Section ${cls.section} (${cls.academicYear})"

private fun notificationAudienceLabel(audience: String): String = when (audience) {
    "everyone" -> "Everyone"
    "all_students" -> "Students"
    "all_staff" -> "Staff"
    "course" -> "Course"
    "class" -> "Class"
    "group" -> "Lab group"
    "class_subject" -> "Subject"
    else -> audience.replace('_', ' ')
}

private fun notificationPriorityColor(priority: String): Color = when (priority.lowercase()) {
    "urgent" -> ErrorRed
    "high" -> Gold
    else -> Navy
}

private fun notificationDeliveryLabel(item: NotificationRow): String = when {
    item.isCancelled -> "CANCELLED"
    item.pushSentAt != null && item.pushSentCount > 0 -> "${item.pushSentCount} SENT"
    item.pushSentAt != null && item.pushFailedCount > 0 -> "${item.pushFailedCount} FAILED"
    item.pushSentAt != null -> "NO DEVICES"
    isPushInFlight(item.createdAt) -> "SENDING"
    // Notices posted before the push pipeline existed have no delivery stamp; calling them
    // "SENDING" forever would look like a stuck push.
    else -> "NOT SENT"
}

private fun notificationDeliveryColor(item: NotificationRow): Color = when {
    item.isCancelled -> TextSecondary
    item.pushSentAt != null && item.pushSentCount > 0 -> SuccessGreen
    item.pushSentAt != null -> ErrorRed
    isPushInFlight(item.createdAt) -> Gold
    else -> TextSecondary
}

/** True when [raw] is a recent timestamp, i.e. the push may still be in flight. */
private fun isPushInFlight(raw: String?): Boolean {
    val created = raw?.trim().orEmpty()
    if (created.isEmpty()) return false
    val instant = runCatching { OffsetDateTime.parse(created).toInstant() }.getOrNull() ?: return false
    val ageSeconds = Duration.between(instant, Instant.now()).seconds
    return ageSeconds in 0L..300L
}

private val noticeTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM, HH:mm")

/** `2026-11-04T10:24:22.095229+00:00` -> `04 Nov, 15:54` in the device's time zone. */
private fun formatNoticeTime(raw: String?): String? {
    val text = raw?.trim().orEmpty()
    if (text.isEmpty()) return null
    return runCatching { OffsetDateTime.parse(text) }
        .getOrNull()
        ?.atZoneSameInstant(ZoneId.systemDefault())
        ?.let { noticeTimeFormatter.format(it) }
}


@Composable
private fun mitrcAdminFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    cursorColor = Navy,
    focusedBorderColor = Navy,
    unfocusedBorderColor = DividerSoft,
    focusedContainerColor = SurfaceWhite,
    unfocusedContainerColor = SurfaceWhite,
    focusedLeadingIconColor = Navy,
    unfocusedLeadingIconColor = TextSecondary,
    focusedTrailingIconColor = TextSecondary,
    unfocusedTrailingIconColor = TextSecondary,
    focusedPlaceholderColor = TextSecondary.copy(alpha = 0.6f),
    unfocusedPlaceholderColor = TextSecondary.copy(alpha = 0.6f),
    // The audience/class pickers are read-only fields disabled to block the keyboard; keep them
    // looking like normal inputs instead of fading the chosen value out.
    disabledTextColor = TextPrimary,
    disabledBorderColor = DividerSoft,
    disabledContainerColor = SurfaceWhite,
    disabledLeadingIconColor = Navy,
    disabledTrailingIconColor = TextSecondary,
    disabledPlaceholderColor = TextSecondary.copy(alpha = 0.6f)
)

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        color = SurfaceWhite,
        shape = RoundedCornerShape(20.dp),
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = title,
                color = Navy,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            )
            content()
        }
    }
}

@Composable
private fun StatusLine(error: String?, notice: String?) {
    if (error != null) {
        Surface(
            color = ErrorRed.copy(alpha = 0.08f),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = error,
                color = ErrorRed,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                modifier = Modifier.padding(12.dp)
            )
        }
    }
    if (notice != null) {
        Surface(
            color = SuccessGreen.copy(alpha = 0.08f),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = notice,
                color = SuccessGreen,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}

@Composable
private fun SaveButton(
    label: String,
    saving: Boolean,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = !saving,
        modifier = Modifier.fillMaxWidth()
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
            Text(label, fontWeight = FontWeight.Bold)
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

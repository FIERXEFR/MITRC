package com.mitrc.ac.`in`.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mitrc.ac.`in`.data.ClassGroupRow
import com.mitrc.ac.`in`.data.ClassSubjectView
import com.mitrc.ac.`in`.data.NewNotificationRow
import com.mitrc.ac.`in`.data.NotificationRow
import com.mitrc.ac.`in`.data.PortalRepository
import com.mitrc.ac.`in`.data.TeacherClassInfo
import com.mitrc.ac.`in`.ui.theme.DividerSoft
import com.mitrc.ac.`in`.ui.theme.ErrorRed
import com.mitrc.ac.`in`.ui.theme.Gold
import com.mitrc.ac.`in`.ui.theme.Navy
import com.mitrc.ac.`in`.ui.theme.NavyDeep
import com.mitrc.ac.`in`.ui.theme.Slate
import com.mitrc.ac.`in`.ui.theme.SuccessGreen
import com.mitrc.ac.`in`.ui.theme.SurfaceWhite
import com.mitrc.ac.`in`.ui.theme.TextPrimary
import com.mitrc.ac.`in`.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val TeacherNoticeCategories = listOf(
    "announcement" to "Announcement",
    "exam" to "Exam",
    "holiday" to "Holiday",
    "timetable" to "Timetable",
    "notes" to "Notes",
    "attendance" to "Attendance",
    "marks" to "Marks",
    "alert" to "Alert",
)

private val TeacherNoticePriorities = listOf(
    "normal" to "Normal",
    "high" to "High",
    "urgent" to "Urgent",
)

@Composable
fun TeacherNoticesView(
    subjects: List<ClassSubjectView>,
    selectedSubject: ClassSubjectView?,
    classInfo: Map<Int, TeacherClassInfo>,
    onSelectSubject: (ClassSubjectView) -> Unit,
) {
    var notices by remember { mutableStateOf<List<NotificationRow>>(emptyList()) }
    var groups by remember { mutableStateOf<List<ClassGroupRow>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var refreshTick by remember { mutableIntStateOf(0) }
    var showComposer by remember { mutableStateOf(false) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(selectedSubject?.classSubjectId) {
        successMessage = null
        error = null
    }

    val loadNotices: suspend () -> Unit = {
        val subject = selectedSubject
        if (subject == null) {
            notices = emptyList()
            groups = emptyList()
            loading = false
        } else {
            loading = true
            error = null
            groups = PortalRepository.getClassGroups(subject.classId).getOrDefault(emptyList())
            val result = PortalRepository.getNotificationsForSubjects(
                classSubjectIds = listOf(subject.classSubjectId),
                groupIds = groups.map { it.id },
            )
            notices = result.getOrDefault(emptyList())
            error = result.exceptionOrNull()?.message
            loading = false
        }
    }

    LaunchedEffect(selectedSubject?.classSubjectId, refreshTick) {
        loadNotices()
    }

    LaunchedEffect(successMessage) {
        if (!successMessage.isNullOrBlank()) {
            delay(5_000)
            successMessage = null
        }
    }

    val groupNames = remember(groups) { groups.associate { it.id to it.name } }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate),
    ) {
        TeacherRefreshBox(
            onRefresh = loadNotices,
            skeleton = { TeacherTabSkeleton(TeacherTabSkeletonKind.Notices) },
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 26.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item {
                    TeacherClassPickerCard(
                        subjects = subjects,
                        selectedSubject = selectedSubject,
                        classInfo = classInfo,
                        label = "POSTING FOR",
                        onSelectSubject = onSelectSubject,
                    )
                }

                if (selectedSubject == null) {
                    item {
                        TeacherEmptyState(
                            icon = Icons.Outlined.NotificationsNone,
                            title = "Choose a class first",
                            body = "Select one of your assigned classes before posting a notice.",
                        )
                    }
                } else {
                    item {
                        NoticeComposerHero(
                            count = notices.size,
                            onCompose = { showComposer = true },
                        )
                    }

                    item {
                        TeacherStatusBanner(successMessage, isError = false)
                    }

                    item {
                        Text(
                            text = "POSTED NOTICES (${notices.size})",
                            color = TextSecondary,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                        )
                    }

                    when {
                        loading -> item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 30.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = Navy,
                                    strokeWidth = 2.5.dp,
                                )
                                Spacer(Modifier.width(10.dp))
                                Text("Loading notices...", color = TextSecondary, fontSize = 12.sp)
                            }
                        }

                        error != null && notices.isEmpty() -> item {
                            TeacherEmptyState(
                                icon = Icons.Outlined.NotificationsNone,
                                title = "Could not load notices",
                                body = error.orEmpty(),
                                actionLabel = "Try again",
                                onAction = { refreshTick++ },
                            )
                        }

                        notices.isEmpty() -> item {
                            TeacherEmptyState(
                                icon = Icons.Outlined.Campaign,
                                title = "No notices yet",
                                body = "Post the first update for this class.",
                                actionLabel = "Post a notice",
                                onAction = { showComposer = true },
                            )
                        }

                        else -> items(notices, key = { it.id }) { notice ->
                            TeacherNoticeCard(
                                notice = notice,
                                groupName = notice.groupId?.let { groupNames[it] ?: "Group $it" },
                            )
                        }
                    }

                    item { Spacer(Modifier.height(4.dp)) }
                }
            }
        }
    }

    if (showComposer && selectedSubject != null) {
        TeacherComposeNoticeDialog(
            subject = selectedSubject,
            groups = groups,
            onDismiss = { showComposer = false },
            onPosted = {
                showComposer = false
                successMessage = "Notice posted to ${selectedSubject.subjectName}."
                refreshTick++
            },
        )
    }
}

@Composable
private fun NoticeComposerHero(
    count: Int,
    onCompose: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Navy,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onCompose),
    ) {
        Row(
            modifier = Modifier.padding(17.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Gold.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.Campaign, contentDescription = null, tint = Gold, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(13.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Post a class notice",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = if (count == 0) {
                        "Keep students informed with a clear update"
                    } else {
                        "$count notice${if (count == 1) "" else "s"} posted for this class"
                    },
                    color = Color.White.copy(alpha = 0.66f),
                    fontSize = 10.5.sp,
                    lineHeight = 14.sp,
                )
            }
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(Color.White.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.Add, contentDescription = "Post notice", tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun TeacherNoticeCard(
    notice: NotificationRow,
    groupName: String?,
) {
    val accent = teacherPriorityColor(notice.priority)
    val categoryLabel = TeacherNoticeCategories.firstOrNull { it.first == notice.category }?.second
        ?: notice.category.replaceFirstChar { it.uppercase() }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = SurfaceWhite,
        border = BorderStroke(1.dp, DividerSoft),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(15.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(accent.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.Campaign, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = notice.title,
                        color = TextPrimary,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        lineHeight = 18.sp,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = "$categoryLabel  ·  ${groupName ?: "Whole class"}",
                        color = TextSecondary,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(accent.copy(alpha = 0.11f))
                        .padding(horizontal = 7.dp, vertical = 3.dp),
                ) {
                    Text(
                        text = notice.priority.uppercase(),
                        color = if (accent == Gold) NavyDeep else accent,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.4.sp,
                    )
                }
            }

            if (!notice.body.isNullOrBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = notice.body,
                    color = TextSecondary,
                    fontSize = 11.5.sp,
                    lineHeight = 16.sp,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(Modifier.height(10.dp))
            Text(
                text = teacherShortTimestamp(notice.createdAt),
                color = TextSecondary,
                fontSize = 10.sp,
            )
        }
    }
}

private fun teacherPriorityColor(priority: String): Color = when (priority.lowercase()) {
    "urgent" -> ErrorRed
    "high" -> Gold
    else -> SuccessGreen
}

@Composable
private fun TeacherComposeNoticeDialog(
    subject: ClassSubjectView,
    groups: List<ClassGroupRow>,
    onDismiss: () -> Unit,
    onPosted: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("announcement") }
    var priority by remember { mutableStateOf("normal") }
    var selectedGroupId by remember { mutableStateOf<Int?>(null) }
    var posting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    TeacherDialogShell(
        title = "Post a notice",
        subtitle = "${subject.subjectCode} · ${subject.subjectName}",
        icon = Icons.Outlined.Campaign,
        onDismiss = { if (!posting) onDismiss() },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Notice title") },
                placeholder = { Text("Lab record submission on Friday") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = teacherFieldColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .trackTeacherFieldFocus(),
            )

            OutlinedTextField(
                value = body,
                onValueChange = { body = it },
                label = { Text("Details") },
                placeholder = { Text("Add the date, time or action students need to take.") },
                minLines = 3,
                maxLines = 5,
                shape = RoundedCornerShape(12.dp),
                colors = teacherFieldColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .trackTeacherFieldFocus(),
            )

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
                TeacherNoticeCategories.forEach { (value, label) ->
                    TeacherChoiceChip(
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
                TeacherNoticePriorities.forEach { (value, label) ->
                    TeacherChoiceChip(
                        label = label,
                        selected = priority == value,
                        onClick = { priority = value },
                    )
                }
            }

            Text(
                text = "SHARE WITH",
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
                TeacherChoiceChip(
                    label = "Whole class",
                    selected = selectedGroupId == null,
                    onClick = { selectedGroupId = null },
                )
                groups.forEach { group ->
                    TeacherChoiceChip(
                        label = "Group ${group.name}",
                        selected = selectedGroupId == group.id,
                        onClick = { selectedGroupId = group.id },
                    )
                }
            }

            TeacherStatusBanner(error, isError = true)

            TeacherDialogActions(
                confirmLabel = "Post notice",
                confirmEnabled = title.isNotBlank(),
                loading = posting,
                onConfirm = {
                    if (title.isBlank()) {
                        error = "Enter a notice title."
                    } else {
                        posting = true
                        error = null
                        scope.launch {
                            val isGroupNotice = selectedGroupId != null
                            val result = PortalRepository.createNotification(
                                NewNotificationRow(
                                    title = title.trim(),
                                    body = body.trim().ifBlank { null },
                                    category = category,
                                    priority = priority,
                                    audience = if (isGroupNotice) "group" else "class_subject",
                                    classId = null,
                                    groupId = selectedGroupId,
                                    classSubjectId = if (isGroupNotice) null else subject.classSubjectId,
                                ),
                            )
                            posting = false
                            result.fold(
                                onSuccess = { onPosted() },
                                onFailure = { postError ->
                                    error = postError.message?.ifBlank {
                                        "The notice could not be posted."
                                    } ?: "The notice could not be posted."
                                },
                            )
                        }
                    }
                },
                onCancel = { if (!posting) onDismiss() },
            )
        }
    }
}

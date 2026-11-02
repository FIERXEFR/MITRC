package com.mitrc.ac.`in`.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mitrc.ac.`in`.data.ClassGroupRow
import com.mitrc.ac.`in`.data.ClassSubjectView
import com.mitrc.ac.`in`.data.GoogleDriveStorage
import com.mitrc.ac.`in`.data.NoteInsertRow
import com.mitrc.ac.`in`.data.NoteRow
import com.mitrc.ac.`in`.data.PortalRepository
import com.mitrc.ac.`in`.ui.theme.DividerSoft
import com.mitrc.ac.`in`.ui.theme.ErrorRed
import com.mitrc.ac.`in`.ui.theme.Gold
import com.mitrc.ac.`in`.ui.theme.Navy
import com.mitrc.ac.`in`.ui.theme.Slate
import com.mitrc.ac.`in`.ui.theme.SuccessGreen
import com.mitrc.ac.`in`.ui.theme.SurfaceWhite
import com.mitrc.ac.`in`.ui.theme.TextPrimary
import com.mitrc.ac.`in`.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val TeacherNoteCategories = listOf(
    "notes" to "Notes",
    "assignment" to "Assignment",
    "lab_manual" to "Lab manual",
    "syllabus" to "Syllabus",
    "question_paper" to "Question paper",
    "other" to "Other",
)

@Composable
fun TeacherResourcesView(
    selectedSubject: ClassSubjectView?,
    refreshKey: Int = 0,
) {
    var notes by remember { mutableStateOf<List<NoteRow>>(emptyList()) }
    var groups by remember { mutableStateOf<List<ClassGroupRow>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var refreshTick by remember { mutableIntStateOf(0) }
    var showUpload by remember { mutableStateOf(false) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(selectedSubject?.classSubjectId) {
        successMessage = null
        error = null
    }

    LaunchedEffect(selectedSubject?.classSubjectId, refreshTick, refreshKey) {
        val subject = selectedSubject
        if (subject == null) {
            notes = emptyList()
            groups = emptyList()
            return@LaunchedEffect
        }

        loading = true
        error = null
        groups = PortalRepository.getClassGroups(subject.classId).getOrDefault(emptyList())
        val result = PortalRepository.getNotesForSubject(subject.classSubjectId)
        notes = result.getOrDefault(emptyList())
        error = result.exceptionOrNull()?.message
        loading = false
    }

    LaunchedEffect(successMessage) {
        if (!successMessage.isNullOrBlank()) {
            delay(5_000)
            successMessage = null
        }
    }

    val groupNames = remember(groups) { groups.associate { it.id to it.name } }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "MATERIALS",
                color = TextSecondary,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                modifier = Modifier.weight(1f),
            )
            IconButton(
                onClick = {
                    successMessage = null
                    refreshTick++
                },
                enabled = selectedSubject != null && !loading,
            ) {
                Icon(Icons.Outlined.Refresh, contentDescription = "Refresh materials", tint = Navy)
            }
        }

        if (selectedSubject == null) {
            TeacherEmptyState(
                icon = Icons.Outlined.FolderOpen,
                title = "Choose a class first",
                body = "Select one of your assigned classes to see and upload material.",
            )
        } else {
            MaterialUploadHero(
                count = notes.size,
                onUpload = { showUpload = true },
            )

            TeacherStatusBanner(successMessage, isError = false)

            Text(
                text = "CLASS MATERIALS (${notes.size})",
                color = TextSecondary,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
            )

            when {
                loading -> Row(
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
                    Text("Loading materials...", color = TextSecondary, fontSize = 12.sp)
                }

                error != null && notes.isEmpty() -> TeacherEmptyState(
                    icon = Icons.Outlined.FolderOpen,
                    title = "Could not load materials",
                    body = error.orEmpty(),
                    actionLabel = "Try again",
                    onAction = { refreshTick++ },
                )

                notes.isEmpty() -> TeacherEmptyState(
                    icon = Icons.Outlined.Description,
                    title = "No material yet",
                    body = "Upload the first PDF or document for this class.",
                    actionLabel = "Upload material",
                    onAction = { showUpload = true },
                )

                else -> notes.forEach { note ->
                    TeacherMaterialCard(
                        note = note,
                        groupName = note.groupId?.let { groupNames[it] ?: "Group $it" } ?: "Whole class",
                    )
                }
            }

            Spacer(Modifier.height(4.dp))
        }
    }

    if (showUpload && selectedSubject != null) {
        TeacherUploadMaterialDialog(
            subject = selectedSubject,
            groups = groups,
            onDismiss = { showUpload = false },
            onUploaded = {
                showUpload = false
                successMessage = "Material added to ${selectedSubject.subjectName}."
                refreshTick++
            },
        )
    }
}

@Composable
private fun MaterialUploadHero(
    count: Int,
    onUpload: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Navy,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onUpload),
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
                Icon(Icons.Outlined.CloudUpload, contentDescription = null, tint = Gold, modifier = Modifier.size(23.dp))
            }
            Spacer(Modifier.width(13.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Upload class material",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = if (count == 0) {
                        "Start the resource library for this class"
                    } else {
                        "$count item${if (count == 1) "" else "s"} shared with this class"
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
                Icon(Icons.Outlined.Add, contentDescription = "Upload material", tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun TeacherMaterialCard(
    note: NoteRow,
    groupName: String,
) {
    val context = LocalContext.current
    val categoryLabel = TeacherNoteCategories.firstOrNull { it.first == note.category }?.second
        ?: note.category.replaceFirstChar { it.uppercase() }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = SurfaceWhite,
        border = BorderStroke(1.dp, DividerSoft),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable {
                if (note.driveUrl.isBlank()) {
                    Toast.makeText(context, "This file has no link.", Toast.LENGTH_SHORT).show()
                } else {
                    runCatching {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(note.driveUrl)))
                    }.onFailure {
                        Toast.makeText(context, "No app can open this file.", Toast.LENGTH_SHORT).show()
                    }
                }
            },
    ) {
        Column(modifier = Modifier.padding(15.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Navy.copy(alpha = 0.07f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.Description, contentDescription = null, tint = Navy, modifier = Modifier.size(21.dp))
                }
                Spacer(Modifier.width(11.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = note.title,
                        color = TextPrimary,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        lineHeight = 18.sp,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = "$categoryLabel  ·  $groupName",
                        color = TextSecondary,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Icon(
                    Icons.Outlined.OpenInNew,
                    contentDescription = "Open material",
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp),
                )
            }

            if (!note.description.isNullOrBlank()) {
                Spacer(Modifier.height(9.dp))
                Text(
                    text = note.description,
                    color = TextSecondary,
                    fontSize = 11.5.sp,
                    lineHeight = 16.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                CategoryPill(categoryLabel)
                Spacer(Modifier.weight(1f))
                Text(
                    text = teacherShortTimestamp(note.createdAt),
                    color = TextSecondary,
                    fontSize = 10.sp,
                )
            }
        }
    }
}

@Composable
private fun CategoryPill(label: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Gold.copy(alpha = 0.14f))
            .padding(horizontal = 7.dp, vertical = 3.dp),
    ) {
        Text(
            text = label.uppercase(),
            color = Navy,
            fontSize = 8.5.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.4.sp,
        )
    }
}

@Composable
private fun TeacherUploadMaterialDialog(
    subject: ClassSubjectView,
    groups: List<ClassGroupRow>,
    onDismiss: () -> Unit,
    onUploaded: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf<String?>(null) }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("notes") }
    var selectedGroupId by remember { mutableStateOf<Int?>(null) }
    var uploading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            selectedUri = uri
            selectedFileName = context.teacherDisplayName(uri)
            error = null
        }
    }

    TeacherDialogShell(
        title = "Upload material",
        subtitle = "${subject.subjectCode} · ${subject.subjectName}",
        icon = Icons.Outlined.UploadFile,
        onDismiss = { if (!uploading) onDismiss() },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = {
                    picker.launch(
                        arrayOf(
                            "application/pdf",
                            "application/msword",
                            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                            "text/plain",
                            "image/*",
                        ),
                    )
                },
                enabled = !uploading,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, DividerSoft),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 50.dp),
            ) {
                Icon(Icons.Outlined.UploadFile, contentDescription = null, tint = Navy)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = selectedFileName ?: "Choose PDF or document",
                    color = if (selectedFileName == null) TextSecondary else TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                placeholder = { Text("Unit 2 — lecture notes") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = teacherFieldColors(),
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description (optional)") },
                placeholder = { Text("What should students know about this material?") },
                minLines = 2,
                maxLines = 4,
                shape = RoundedCornerShape(12.dp),
                colors = teacherFieldColors(),
                modifier = Modifier.fillMaxWidth(),
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
                TeacherNoteCategories.forEach { (value, label) ->
                    TeacherChoiceChip(
                        label = label,
                        selected = category == value,
                        onClick = { category = value },
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
                confirmLabel = "Upload material",
                confirmEnabled = selectedUri != null && title.isNotBlank(),
                loading = uploading,
                onConfirm = {
                    val uri = selectedUri
                    if (uri == null || title.isBlank()) {
                        error = "Choose a file and enter a title."
                    } else {
                        uploading = true
                        error = null
                        scope.launch {
                            val stored = GoogleDriveStorage
                                .upload(context, uri, folder = "Notes")
                                .getOrElse { uploadError ->
                                    uploading = false
                                    error = uploadError.message?.ifBlank {
                                        "The file could not be uploaded to Drive."
                                    } ?: "The file could not be uploaded to Drive."
                                    return@launch
                                }

                            val result = PortalRepository.createNote(
                                NoteInsertRow(
                                    classSubjectId = subject.classSubjectId,
                                    groupId = selectedGroupId,
                                    title = title.trim(),
                                    description = description.trim().ifBlank { null },
                                    category = category,
                                    driveUrl = stored.url,
                                    isVisible = true,
                                ),
                            )
                            uploading = false
                            result.fold(
                                onSuccess = { onUploaded() },
                                onFailure = { saveError ->
                                    error = saveError.message?.ifBlank {
                                        "The material could not be saved."
                                    } ?: "The material could not be saved."
                                },
                            )
                        }
                    }
                },
                onCancel = { if (!uploading) onDismiss() },
            )
        }
    }
}

private fun Context.teacherDisplayName(uri: Uri): String? = runCatching {
    contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
    }
}.getOrNull()

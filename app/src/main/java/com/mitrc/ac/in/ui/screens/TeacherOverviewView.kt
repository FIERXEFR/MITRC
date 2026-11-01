package com.mitrc.ac.`in`.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.FactCheck
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mitrc.ac.`in`.data.ClassSubjectView
import com.mitrc.ac.`in`.data.FacultyMasterRow
import com.mitrc.ac.`in`.data.PortalRepository
import com.mitrc.ac.`in`.data.TeacherClassInfo
import com.mitrc.ac.`in`.ui.theme.DividerSoft
import com.mitrc.ac.`in`.ui.theme.ErrorRed
import com.mitrc.ac.`in`.ui.theme.Gold
import com.mitrc.ac.`in`.ui.theme.Navy
import com.mitrc.ac.`in`.ui.theme.NavyDeep
import com.mitrc.ac.`in`.ui.theme.NavySoft
import com.mitrc.ac.`in`.ui.theme.Slate
import com.mitrc.ac.`in`.ui.theme.SurfaceWhite
import com.mitrc.ac.`in`.ui.theme.TextPrimary
import com.mitrc.ac.`in`.ui.theme.TextSecondary

@Composable
fun TeacherOverviewView(
    teacher: FacultyMasterRow,
    subjects: List<ClassSubjectView>,
    selectedSubject: ClassSubjectView?,
    classInfo: Map<Int, TeacherClassInfo>,
    onSelectSubject: (ClassSubjectView) -> Unit,
    onOpenClassroom: () -> Unit,
    onOpenMaterials: () -> Unit,
    onOpenNotices: () -> Unit,
    onSignOut: () -> Unit,
) {
    var selectedStudentCount by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(selectedSubject?.classId) {
        val classId = selectedSubject?.classId ?: return@LaunchedEffect
        selectedStudentCount = PortalRepository
            .getEnrolledStudentsForClass(classId)
            .getOrNull()
            ?.size
    }

    val distinctClasses = remember(subjects) { subjects.map { it.classId }.distinct().size }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            TeacherSectionHeader(
                eyebrow = "Faculty workspace",
                title = "Overview",
                subtitle = "Your classes, attendance and class updates in one calm place.",
            )
        }

        item {
            TeacherHeroCard(
                teacher = teacher,
                subjects = subjects,
                selectedSubject = selectedSubject,
                classInfo = classInfo,
                onSelectSubject = onSelectSubject,
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TeacherMetricCard(
                    label = "Classes",
                    value = distinctClasses.toString(),
                    hint = "assigned",
                    modifier = Modifier.weight(1f),
                )
                TeacherMetricCard(
                    label = "Subjects",
                    value = subjects.size.toString(),
                    hint = "teaching",
                    modifier = Modifier.weight(1f),
                )
                TeacherMetricCard(
                    label = "Students",
                    value = selectedStudentCount?.toString() ?: "—",
                    hint = "in selected class",
                    modifier = Modifier.weight(1f),
                )
            }
        }

        item {
            Text(
                text = "QUICK ACTIONS",
                color = TextSecondary,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                TeacherActionTile(
                    title = "Take attendance",
                    subtitle = "Open the selected class roster and save today's attendance.",
                    icon = Icons.Outlined.FactCheck,
                    onClick = onOpenClassroom,
                )
                TeacherActionTile(
                    title = "Upload material",
                    subtitle = "Share notes, assignments and lab documents with a class.",
                    icon = Icons.Outlined.MenuBook,
                    onClick = onOpenMaterials,
                )
                TeacherActionTile(
                    title = "Post a notice",
                    subtitle = "Send a clear update to the whole class or a specific group.",
                    icon = Icons.Outlined.Campaign,
                    onClick = onOpenNotices,
                )
            }
        }

        item {
            Text(
                text = "YOUR ASSIGNMENTS",
                color = TextSecondary,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
            )
        }

        items(subjects, key = { it.classSubjectId }) { subject ->
            TeacherClassSummaryCard(
                subject = subject,
                info = classInfo[subject.classId],
                label = if (subject.classSubjectId == selectedSubject?.classSubjectId) {
                    "SELECTED CLASS"
                } else {
                    "ASSIGNED CLASS"
                },
                onClick = {
                    onSelectSubject(subject)
                    onOpenClassroom()
                },
            )
        }

        item {
            Spacer(Modifier.height(2.dp))
            OutlinedButton(
                onClick = onSignOut,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, DividerSoft),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 50.dp),
            ) {
                Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Sign out of this device",
                    color = ErrorRed,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun TeacherHeroCard(
    teacher: FacultyMasterRow,
    subjects: List<ClassSubjectView>,
    selectedSubject: ClassSubjectView?,
    classInfo: Map<Int, TeacherClassInfo>,
    onSelectSubject: (ClassSubjectView) -> Unit,
) {
    var classMenuOpen by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    listOf(NavyDeep, Navy, NavySoft),
                ),
            )
            .padding(18.dp),
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(Gold.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = teacherInitials(teacher.name),
                        color = Gold,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "FACULTY WORKSPACE",
                        color = Gold.copy(alpha = 0.9f),
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.1.sp,
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = teacher.name?.trim()?.takeIf { it.isNotEmpty() } ?: "Faculty member",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = listOfNotNull(
                            teacher.designation?.trim()?.takeIf { it.isNotEmpty() },
                            teacher.email?.trim()?.takeIf { it.isNotEmpty() },
                        ).joinToString("  ·  ").ifEmpty { "Faculty account" },
                        color = Color.White.copy(alpha = 0.65f),
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Spacer(Modifier.height(15.dp))
            TeacherGoldRule()
            Spacer(Modifier.height(13.dp))

            Box {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { classMenuOpen = true }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Outlined.School, contentDescription = null, tint = Gold, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "SELECTED CLASS",
                            color = Color.White.copy(alpha = 0.55f),
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.9.sp,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = selectedSubject?.let { subject ->
                                "${subject.subjectCode}  ·  ${teacherClassLabel(subject, classInfo[subject.classId])}"
                            } ?: "Choose a class from the list below",
                            color = Color.White,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        Icons.Outlined.ExpandMore,
                        contentDescription = "Change class",
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp),
                    )
                }

                DropdownMenu(
                    expanded = classMenuOpen,
                    onDismissRequest = { classMenuOpen = false },
                    modifier = Modifier.fillMaxWidth(0.9f),
                ) {
                    subjects.forEach { subject ->
                        val isSelected = subject.classSubjectId == selectedSubject?.classSubjectId
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(
                                        text = "${subject.subjectCode}  ·  ${subject.subjectName}",
                                        color = TextPrimary,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = teacherClassLabel(subject, classInfo[subject.classId]),
                                        color = TextSecondary,
                                        fontSize = 10.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Outlined.School,
                                    contentDescription = null,
                                    tint = if (isSelected) Gold else Navy.copy(alpha = 0.55f),
                                    modifier = Modifier.size(18.dp),
                                )
                            },
                            trailingIcon = if (isSelected) {
                                {
                                    Icon(
                                        Icons.Outlined.Check,
                                        contentDescription = null,
                                        tint = Gold,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            } else {
                                null
                            },
                            onClick = {
                                classMenuOpen = false
                                onSelectSubject(subject)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TeacherMetricCard(
    label: String,
    value: String,
    hint: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = SurfaceWhite,
        border = BorderStroke(1.dp, DividerSoft),
        modifier = modifier,
    ) {
        Column(modifier = Modifier.padding(horizontal = 11.dp, vertical = 12.dp)) {
            Text(
                text = value,
                color = Navy,
                fontSize = 21.sp,
                fontWeight = FontWeight.ExtraBold,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = label,
                color = TextPrimary,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = hint,
                color = TextSecondary,
                fontSize = 8.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

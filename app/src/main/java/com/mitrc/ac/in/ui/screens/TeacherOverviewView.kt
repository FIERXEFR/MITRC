package com.mitrc.ac.`in`.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.FactCheck
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mitrc.ac.`in`.data.ClassSubjectView
import com.mitrc.ac.`in`.data.PortalRepository
import com.mitrc.ac.`in`.data.TeacherClassInfo
import com.mitrc.ac.`in`.ui.theme.DividerSoft
import com.mitrc.ac.`in`.ui.theme.Navy
import com.mitrc.ac.`in`.ui.theme.Slate
import com.mitrc.ac.`in`.ui.theme.SurfaceWhite
import com.mitrc.ac.`in`.ui.theme.TextPrimary
import com.mitrc.ac.`in`.ui.theme.TextSecondary

@Composable
fun TeacherOverviewView(
    subjects: List<ClassSubjectView>,
    selectedSubject: ClassSubjectView?,
    classInfo: Map<Int, TeacherClassInfo>,
    onSelectSubject: (ClassSubjectView) -> Unit,
    onOpenClassroom: () -> Unit,
    onOpenMaterials: () -> Unit,
    onOpenNotices: () -> Unit,
) {
    var selectedStudentCount by remember { mutableStateOf<Int?>(null) }

    val loadSelectedClass: suspend () -> Unit = {
        val classId = selectedSubject?.classId
        selectedStudentCount = if (classId == null) {
            null
        } else {
            PortalRepository.getEnrolledStudentsForClass(classId).getOrNull()?.size
        }
    }

    LaunchedEffect(selectedSubject?.classId) {
        loadSelectedClass()
    }

    val distinctClasses = remember(subjects) { subjects.map { it.classId }.distinct().size }

    TeacherRefreshBox(
        onRefresh = loadSelectedClass,
        skeleton = { TeacherTabSkeleton(TeacherTabSkeletonKind.Overview) },
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Slate),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                TeacherClassPickerCard(
                    subjects = subjects,
                    selectedSubject = selectedSubject,
                    classInfo = classInfo,
                    label = "CURRENT CLASS",
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

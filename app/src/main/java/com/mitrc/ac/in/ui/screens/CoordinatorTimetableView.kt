package com.mitrc.ac.`in`.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Publish
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mitrc.ac.`in`.auth.AdminRepository
import com.mitrc.ac.`in`.data.ClassGroupRow
import com.mitrc.ac.`in`.data.CoordinatorRow
import com.mitrc.ac.`in`.data.PortalRepository
import com.mitrc.ac.`in`.data.StudentDirectoryView
import com.mitrc.ac.`in`.data.SubjectTeacherView
import com.mitrc.ac.`in`.data.TimetableEntryRow
import com.mitrc.ac.`in`.data.TimetableEntryView
import com.mitrc.ac.`in`.data.TimetableRow
import com.mitrc.ac.`in`.ui.theme.ErrorRed
import com.mitrc.ac.`in`.ui.theme.Gold
import com.mitrc.ac.`in`.ui.theme.Navy
import com.mitrc.ac.`in`.ui.theme.NavySoft
import com.mitrc.ac.`in`.ui.theme.Slate
import com.mitrc.ac.`in`.ui.theme.SuccessGreen
import com.mitrc.ac.`in`.ui.theme.SurfaceWhite
import com.mitrc.ac.`in`.ui.theme.TextPrimary
import com.mitrc.ac.`in`.ui.theme.TextSecondary
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Composable
fun CoordinatorTimetableView(userUid: String) {
    val scope = rememberCoroutineScope()

    var loading by remember { mutableStateOf(true) }
    var coordinator by remember { mutableStateOf<CoordinatorRow?>(null) }
    var timetables by remember { mutableStateOf<List<TimetableRow>>(emptyList()) }
    var selectedTimetable by remember { mutableStateOf<TimetableRow?>(null) }

    var entries by remember { mutableStateOf<List<TimetableEntryView>>(emptyList()) }
    var subjects by remember { mutableStateOf<List<SubjectTeacherView>>(emptyList()) }
    var groups by remember { mutableStateOf<List<ClassGroupRow>>(emptyList()) }

    var actionMsg by remember { mutableStateOf<String?>(null) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    var newEffectiveFrom by remember { mutableStateOf(getTodayIstDate()) }

    LaunchedEffect(userUid) {
        loading = true
        val cRes = PortalRepository.getCoordinatorProfile(userUid)
        coordinator = cRes.getOrNull()

        val ttRes = PortalRepository.getTimetables()
        timetables = ttRes.getOrDefault(emptyList())
        if (timetables.isNotEmpty()) {
            selectedTimetable = timetables.first()
        }
        loading = false
    }

    LaunchedEffect(selectedTimetable) {
        actionMsg = null
        errorMsg = null
        val tt = selectedTimetable
        if (tt != null) {
            val entriesRes = PortalRepository.getTimetableEntries(tt.id)
            entries = entriesRes.getOrDefault(emptyList())

            val subTeachersRes = PortalRepository.getSubjectTeachers(tt.classId)
            subjects = subTeachersRes.getOrDefault(emptyList())

            groups = AdminRepository.getClassGroups(tt.classId)
        }
    }

    if (loading) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Navy)
        }
        return
    }

    val coord = coordinator
    if (coord == null || (!coord.haveRights && !coord.isTbIncharge)) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Coordinator Access Required",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = ErrorRed
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Your account does not have active coordinator rights. Contact admin.",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            }
        }
        return
    }

    Column {
        Text(
            text = "TIMETABLE MANAGER & DRAFTS",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = TextSecondary,
            letterSpacing = 1.5.sp
        )
        Spacer(Modifier.height(10.dp))

        if (timetables.isEmpty()) {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.padding(18.dp)) {
                    Text(text = "No timetables created yet.", fontSize = 13.sp, color = TextSecondary)
                }
            }
        } else {
            // Timetable selector chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                timetables.forEach { tt ->
                    val isSel = selectedTimetable?.id == tt.id
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSel) Navy else SurfaceWhite)
                            .clickable { selectedTimetable = tt }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = "TT #${tt.id} (w.e.f. ${tt.effectiveFrom})${if (tt.isPublished) " [PUB]" else " [DRAFT]"}",
                            color = if (isSel) Color.White else TextPrimary,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            val currentTT = selectedTimetable
            if (currentTT != null) {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Class ID: ${currentTT.classId} | w.e.f. ${currentTT.effectiveFrom}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Navy
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (currentTT.isPublished) SuccessGreen else Gold)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (currentTT.isPublished) "PUBLISHED" else "DRAFT",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(Modifier.height(14.dp))

                        // Duplicate Timetable RPC Action
                        OutlinedTextField(
                            value = newEffectiveFrom,
                            onValueChange = { newEffectiveFrom = it.trim() },
                            label = { Text("New Effective Date (YYYY-MM-DD)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    busy = true
                                    actionMsg = null
                                    errorMsg = null
                                    scope.launch {
                                        val res = PortalRepository.duplicateTimetable(currentTT.id, newEffectiveFrom)
                                        busy = false
                                        res.fold(
                                            onSuccess = { newId ->
                                                actionMsg = "Duplicated as new draft TT #$newId"
                                                val refresh = PortalRepository.getTimetables()
                                                timetables = refresh.getOrDefault(emptyList())
                                            },
                                            onFailure = { err ->
                                                errorMsg = err.message ?: "Failed to duplicate timetable"
                                            }
                                        )
                                    }
                                },
                                enabled = !busy,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = NavySoft),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Outlined.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(text = "Duplicate Draft", fontSize = 12.sp)
                            }

                            Button(
                                onClick = {
                                    busy = true
                                    actionMsg = null
                                    errorMsg = null
                                    scope.launch {
                                        val updated = currentTT.copy(isPublished = !currentTT.isPublished)
                                        val res = PortalRepository.updateTimetableHeader(updated)
                                        busy = false
                                        res.fold(
                                            onSuccess = {
                                                selectedTimetable = updated
                                                actionMsg = "Timetable status updated to ${if (updated.isPublished) "Published" else "Draft"}"
                                                val refresh = PortalRepository.getTimetables()
                                                timetables = refresh.getOrDefault(emptyList())
                                            },
                                            onFailure = { err ->
                                                errorMsg = err.message ?: "Failed to update timetable status"
                                            }
                                        )
                                    }
                                },
                                enabled = !busy,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = if (currentTT.isPublished) Gold else SuccessGreen),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Outlined.Publish, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(text = if (currentTT.isPublished) "Unpublish" else "Publish", fontSize = 12.sp)
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        AnimatedVisibility(visible = actionMsg != null) {
                            Text(text = actionMsg.orEmpty(), color = SuccessGreen, fontSize = 12.sp)
                        }
                        AnimatedVisibility(visible = errorMsg != null) {
                            Text(text = errorMsg.orEmpty(), color = ErrorRed, fontSize = 12.sp)
                        }

                        Spacer(Modifier.height(16.dp))

                        Text(
                            text = "TIMETABLE SLOTS (${entries.size})",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            letterSpacing = 1.2.sp
                        )
                        Spacer(Modifier.height(8.dp))

                        if (entries.isEmpty()) {
                            Text(text = "No slot entries in this draft yet.", fontSize = 12.sp, color = TextSecondary)
                        } else {
                            entries.forEach { entry ->
                                SlotEntryRow(
                                    entry = entry,
                                    onDelete = {
                                        scope.launch {
                                            PortalRepository.deleteTimetableEntry(entry.entryId)
                                            val refreshEntries = PortalRepository.getTimetableEntries(currentTT.id)
                                            entries = refreshEntries.getOrDefault(emptyList())
                                        }
                                    }
                                )
                                Spacer(Modifier.height(6.dp))
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Signatories Section from Coordinators
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "OFFICIAL SIGNATORIES",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 1.2.sp
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(text = coord.name ?: "Coordinator", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text(
                            text = if (coord.isTbIncharge) "Time Table Incharge" else "Coordinator",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                    if (!coord.department.isNull_or_blank()) {
                        Text(text = coord.department.orEmpty(), fontSize = 12.sp, color = Navy, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

private fun String?.isNull_or_blank(): Boolean = this == null || this.trim().isEmpty()

@Composable
private fun SlotEntryRow(entry: TimetableEntryView, onDelete: () -> Unit) {
    val daysMap = mapOf(1 to "Mon", 2 to "Tue", 3 to "Wed", 4 to "Thu", 5 to "Fri", 6 to "Sat")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Slate)
            .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${daysMap[entry.dayOfWeek]} | Period ${entry.startPeriod}${if (entry.endPeriod > entry.startPeriod) "-${entry.endPeriod}" else ""}",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Navy
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "${entry.subjectName} (${entry.subjectCode})",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            if (!entry.teacherName.isNull_or_blank()) {
                Text(text = "Teacher: ${entry.teacherName}", fontSize = 11.sp, color = TextSecondary)
            }
        }

        IconButton(onClick = onDelete) {
            Icon(Icons.Outlined.Delete, contentDescription = "Delete Entry", tint = ErrorRed, modifier = Modifier.size(18.dp))
        }
    }
}

private fun getTodayIstDate(): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    sdf.timeZone = TimeZone.getTimeZone("Asia/Kolkata")
    return sdf.format(Date())
}

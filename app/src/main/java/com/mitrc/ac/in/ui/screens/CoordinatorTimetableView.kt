package com.mitrc.ac.`in`.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Publish
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mitrc.ac.`in`.data.ClassGroupRow
import com.mitrc.ac.`in`.data.ClassSubjectView
import com.mitrc.ac.`in`.data.CoordinatorAdminRepository
import com.mitrc.ac.`in`.data.FacultyMasterRow
import com.mitrc.ac.`in`.data.MyCoordinatorClassView
import com.mitrc.ac.`in`.data.NewTimetableEntryRow
import com.mitrc.ac.`in`.data.PortalRepository
import com.mitrc.ac.`in`.data.TimetableEntryRow
import com.mitrc.ac.`in`.data.TimetableEntryView
import com.mitrc.ac.`in`.data.TimetablePeriodRow
import com.mitrc.ac.`in`.data.TimetableRow
import com.mitrc.ac.`in`.ui.theme.DividerSoft
import com.mitrc.ac.`in`.ui.theme.ErrorRed
import com.mitrc.ac.`in`.ui.theme.Gold
import com.mitrc.ac.`in`.ui.theme.GoldLight
import com.mitrc.ac.`in`.ui.theme.Navy
import com.mitrc.ac.`in`.ui.theme.NavySoft
import com.mitrc.ac.`in`.ui.theme.Slate
import com.mitrc.ac.`in`.ui.theme.SuccessGreen
import com.mitrc.ac.`in`.ui.theme.SurfaceWhite
import com.mitrc.ac.`in`.ui.theme.TextPrimary
import com.mitrc.ac.`in`.ui.theme.TextSecondary
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Composable
private fun mitrcFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    disabledTextColor = TextPrimary.copy(alpha = 0.6f),
    cursorColor = Navy,
    focusedBorderColor = Navy,
    unfocusedBorderColor = DividerSoft,
    focusedContainerColor = SurfaceWhite,
    unfocusedContainerColor = SurfaceWhite,
    focusedLabelColor = TextSecondary,
    unfocusedLabelColor = TextSecondary,
    focusedLeadingIconColor = Navy,
    unfocusedLeadingIconColor = Navy,
    focusedTrailingIconColor = TextSecondary,
    unfocusedTrailingIconColor = TextSecondary,
    focusedPlaceholderColor = TextSecondary.copy(alpha = 0.6f),
    unfocusedPlaceholderColor = TextSecondary.copy(alpha = 0.6f)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoordinatorTimetableView(userUid: String) {
    val scope = rememberCoroutineScope()

    var loading by remember { mutableStateOf(true) }
    var coordinatorProfile by remember { mutableStateOf<FacultyMasterRow?>(null) }
    var availableClasses by remember { mutableStateOf<List<MyCoordinatorClassView>>(emptyList()) }
    var selectedClass by remember { mutableStateOf<MyCoordinatorClassView?>(null) }

    var timetables by remember { mutableStateOf<List<TimetableRow>>(emptyList()) }
    var selectedTimetable by remember { mutableStateOf<TimetableRow?>(null) }

    var entries by remember { mutableStateOf<List<TimetableEntryView>>(emptyList()) }
    var periods by remember { mutableStateOf<List<TimetablePeriodRow>>(emptyList()) }
    var subjects by remember { mutableStateOf<List<ClassSubjectView>>(emptyList()) }
    var groups by remember { mutableStateOf<List<ClassGroupRow>>(emptyList()) }

    var actionMsg by remember { mutableStateOf<String?>(null) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    var roomDraft by remember { mutableStateOf("") }
    var effectiveDateDraft by remember { mutableStateOf("") }
    var newDuplicateDate by remember { mutableStateOf(getTodayIstDate()) }

    var showSlotDialog by remember { mutableStateOf(false) }
    var editingEntry by remember { mutableStateOf<TimetableEntryView?>(null) }
    var pendingDay by remember { mutableStateOf<Int?>(null) }
    var pendingPeriod by remember { mutableStateOf<Int?>(null) }

    var showNewTTDialog by remember { mutableStateOf(false) }
    var newTTEffectiveFrom by remember { mutableStateOf(getTodayIstDate()) }
    var newTTRoom by remember { mutableStateOf("") }
    var showNewTTDatePicker by remember { mutableStateOf(false) }

    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showDuplicateDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(userUid) {
        loading = true
        val profRes = PortalRepository.getTeacherProfile(userUid)
        coordinatorProfile = profRes.getOrNull()

        val ruledRes = PortalRepository.getMyCoordinatorClasses()
        val ruledList = ruledRes.getOrDefault(emptyList())

        if (ruledList.isNotEmpty()) {
            availableClasses = ruledList
        } else {
            val fallbackRes = CoordinatorAdminRepository.getAllClassesWithDetails()
            availableClasses = fallbackRes.getOrDefault(emptyList())
        }

        if (availableClasses.isNotEmpty()) {
            selectedClass = availableClasses.first()
        }
        loading = false
    }

    LaunchedEffect(selectedClass) {
        actionMsg = null
        errorMsg = null
        val targetClass = selectedClass
        if (targetClass != null) {
            val ttRes = CoordinatorAdminRepository.getTimetables(listOf(targetClass.classId))
            timetables = ttRes.getOrDefault(emptyList())
            selectedTimetable = timetables.firstOrNull()
        } else {
            timetables = emptyList()
            selectedTimetable = null
        }
    }

    LaunchedEffect(selectedTimetable) {
        actionMsg = null
        errorMsg = null
        val tt = selectedTimetable
        if (tt != null) {
            roomDraft = tt.room.orEmpty()
            effectiveDateDraft = tt.effectiveFrom

            val entriesRes = CoordinatorAdminRepository.getEntries(tt.id)
            entries = entriesRes.getOrDefault(emptyList())

            periods = CoordinatorAdminRepository.getPeriods(tt.id).getOrDefault(emptyList())

            val subjectsRes = CoordinatorAdminRepository.getSubjects(tt.classId)
            subjects = subjectsRes.getOrDefault(emptyList())

            val groupsRes = CoordinatorAdminRepository.getGroups(tt.classId)
            groups = groupsRes.getOrDefault(emptyList())
        } else {
            entries = emptyList()
            periods = emptyList()
            subjects = emptyList()
            groups = emptyList()
        }
    }

    if (loading) {
        CoordinatorTimetableSkeleton()
        return
    }

    CoordinatorRefreshBox(
        onRefresh = {
            val targetClass = selectedClass
            if (targetClass != null) {
                val ttRes = CoordinatorAdminRepository.getTimetables(listOf(targetClass.classId))
                timetables = ttRes.getOrDefault(emptyList())
                val curr = selectedTimetable ?: timetables.firstOrNull()
                if (curr != null) {
                    roomDraft = curr.room.orEmpty()
                    effectiveDateDraft = curr.effectiveFrom
                    entries = CoordinatorAdminRepository.getEntries(curr.id).getOrDefault(emptyList())
                    periods = CoordinatorAdminRepository.getPeriods(curr.id).getOrDefault(emptyList())
                    subjects = CoordinatorAdminRepository.getSubjects(curr.classId).getOrDefault(emptyList())
                    groups = CoordinatorAdminRepository.getGroups(curr.classId).getOrDefault(emptyList())
                }
            }
        },
        skeleton = { CoordinatorTimetableSkeleton() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(
                text = "SELECT CLASS / BATCH",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 1.5.sp
            )
            Spacer(Modifier.height(8.dp))

            if (availableClasses.isEmpty()) {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "No classes found in the system.",
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    availableClasses.forEach { cls ->
                        val isSelected = selectedClass?.classId == cls.classId
                        val label = buildString {
                            if (!cls.course.isNullOrBlank()) append("${cls.course} ")
                            if (!cls.branch.isNullOrBlank()) append("${cls.branch} ")
                            append("Sem ${cls.semester} ${cls.section}")
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) Navy else SurfaceWhite)
                                .clickable { selectedClass = cls }
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) Color.White else TextPrimary,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TIMETABLE DRAFTS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 1.5.sp
                )

                if (selectedClass != null) {
                    TextButton(
                        onClick = {
                            newTTEffectiveFrom = getTodayIstDate()
                            newTTRoom = ""
                            showNewTTDialog = true
                        }
                    ) {
                        Icon(
                            Icons.Outlined.Add,
                            contentDescription = null,
                            tint = Navy,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "New Draft",
                            color = Navy,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            if (timetables.isEmpty()) {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No timetables created for this class yet.",
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                        Spacer(Modifier.height(10.dp))
                        Button(
                            onClick = {
                                newTTEffectiveFrom = getTodayIstDate()
                                newTTRoom = ""
                                showNewTTDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Navy),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Create First Timetable Draft", fontSize = 12.sp)
                        }
                    }
                }
            } else {
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
                                    text = "Timetable #${currentTT.id} Details",
                                    fontSize = 15.sp,
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

                            AnimatedVisibility(visible = actionMsg != null) {
                                Text(
                                    text = actionMsg.orEmpty(),
                                    color = SuccessGreen,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            AnimatedVisibility(visible = errorMsg != null) {
                                Text(
                                    text = errorMsg.orEmpty(),
                                    color = ErrorRed,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            if (actionMsg != null || errorMsg != null) {
                                Spacer(Modifier.height(10.dp))
                            }

                            Text(
                                text = "CLASS ROOM",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary,
                                letterSpacing = 1.2.sp
                            )
                            Spacer(Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = roomDraft,
                                    onValueChange = { roomDraft = it },
                                    label = { Text("Room (e.g. 204)") },
                                    singleLine = true,
                                    colors = mitrcFieldColors(),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        busy = true
                                        actionMsg = null
                                        errorMsg = null
                                        scope.launch {
                                            val res = CoordinatorAdminRepository.updateRoom(currentTT.id, roomDraft)
                                            busy = false
                                            res.fold(
                                                onSuccess = {
                                                    val updated = currentTT.copy(
                                                        room = roomDraft.trim().takeIf { it.isNotEmpty() }
                                                    )
                                                    selectedTimetable = updated
                                                    timetables = timetables.map { if (it.id == updated.id) updated else it }
                                                    actionMsg = "Class room saved."
                                                },
                                                onFailure = { err ->
                                                    errorMsg = err.message ?: "Failed to save the room"
                                                }
                                            )
                                        }
                                    },
                                    enabled = !busy,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Navy)
                                ) {
                                    Text(text = "Save Room", fontSize = 12.sp)
                                }
                            }

                            Spacer(Modifier.height(12.dp))

                            Text(
                                text = "EFFECTIVE DATE (YYYY-MM-DD)",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary,
                                letterSpacing = 1.2.sp
                            )
                            Spacer(Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = effectiveDateDraft,
                                    onValueChange = { effectiveDateDraft = it.trim() },
                                    label = { Text("Effective Date") },
                                    singleLine = true,
                                    colors = mitrcFieldColors(),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        busy = true
                                        actionMsg = null
                                        errorMsg = null
                                        scope.launch {
                                            val res = CoordinatorAdminRepository.updateEffectiveFrom(currentTT.id, effectiveDateDraft)
                                            busy = false
                                            res.fold(
                                                onSuccess = {
                                                    val updated = currentTT.copy(effectiveFrom = effectiveDateDraft)
                                                    selectedTimetable = updated
                                                    timetables = timetables.map { if (it.id == updated.id) updated else it }
                                                    actionMsg = "Effective date updated."
                                                },
                                                onFailure = { err ->
                                                    errorMsg = err.message ?: "Failed to update effective date"
                                                }
                                            )
                                        }
                                    },
                                    enabled = !busy,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Navy)
                                ) {
                                    Text(text = "Update Date", fontSize = 12.sp)
                                }
                            }

                            Spacer(Modifier.height(14.dp))

                            Text(
                                text = "DUPLICATE AS DRAFT",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary,
                                letterSpacing = 1.2.sp
                            )
                            Spacer(Modifier.height(6.dp))
                            OutlinedButton(
                                onClick = { showDuplicateDatePicker = true },
                                enabled = !busy,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Navy),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    Icons.Outlined.CalendarMonth,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(text = "New effective date: $newDuplicateDate", fontSize = 12.sp)
                            }

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
                                            val res = CoordinatorAdminRepository.duplicateTimetable(currentTT.id, newDuplicateDate)
                                            busy = false
                                            res.fold(
                                                onSuccess = { newId ->
                                                    actionMsg = "Duplicated as new draft TT #$newId"
                                                    val targetClass = selectedClass
                                                    if (targetClass != null) {
                                                        val refresh = CoordinatorAdminRepository
                                                            .getTimetables(listOf(targetClass.classId))
                                                            .getOrDefault(timetables)
                                                        timetables = refresh
                                                        selectedTimetable = refresh.firstOrNull { it.id == newId }
                                                            ?: selectedTimetable
                                                    }
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
                                    Text(text = "Duplicate", fontSize = 12.sp)
                                }

                                Button(
                                    onClick = {
                                        busy = true
                                        actionMsg = null
                                        errorMsg = null
                                        scope.launch {
                                            val nextPublished = !currentTT.isPublished
                                            val res = CoordinatorAdminRepository.setPublished(currentTT.id, nextPublished)
                                            busy = false
                                            res.fold(
                                                onSuccess = {
                                                    val updated = currentTT.copy(
                                                        isPublished = nextPublished,
                                                        publishedAt = if (nextPublished) java.time.Instant.now().toString() else null
                                                    )
                                                    selectedTimetable = updated
                                                    timetables = timetables.map { if (it.id == updated.id) updated else it }
                                                    actionMsg = "Status changed to ${if (updated.isPublished) "Published" else "Draft"}"
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

                            Spacer(Modifier.height(8.dp))

                            Button(
                                onClick = { showDeleteConfirm = true },
                                enabled = !busy,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ErrorRed.copy(alpha = 0.15f), contentColor = ErrorRed),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Outlined.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(text = "Delete Timetable Draft", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            Spacer(Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "WEEKLY TIMETABLE",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextSecondary,
                                        letterSpacing = 1.2.sp
                                    )
                                    Spacer(Modifier.height(3.dp))
                                    Text(
                                        text = "${entries.size} slots · swipe sideways for all days",
                                        fontSize = 10.5.sp,
                                        color = TextSecondary
                                    )
                                }
                                TextButton(
                                    onClick = {
                                        editingEntry = null
                                        pendingDay = null
                                        pendingPeriod = null
                                        showSlotDialog = true
                                    }
                                ) {
                                    Icon(
                                        Icons.Outlined.Add,
                                        contentDescription = null,
                                        tint = Navy,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(text = "Add slot", color = Navy, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                            Spacer(Modifier.height(10.dp))

                            TimetableGridTable(
                                entries = entries,
                                periods = periods,
                                lunchAfterPeriod = currentTT.lunchAfterPeriod ?: DEFAULT_LUNCH_AFTER_PERIOD,
                                lunchStart = currentTT.lunchStart ?: DEFAULT_LUNCH_START,
                                lunchEnd = currentTT.lunchEnd ?: DEFAULT_LUNCH_END,
                                onSlotClick = { entry ->
                                    editingEntry = entry
                                    pendingDay = null
                                    pendingPeriod = null
                                    showSlotDialog = true
                                },
                                onEmptyClick = { day, period ->
                                    editingEntry = null
                                    pendingDay = day
                                    pendingPeriod = period
                                    showSlotDialog = true
                                }
                            )

                            Spacer(Modifier.height(16.dp))
                            HorizontalDivider(color = DividerSoft, thickness = 0.6.dp)
                            Spacer(Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "LUNCH BREAK",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextSecondary,
                                        letterSpacing = 1.2.sp
                                    )
                                    Spacer(Modifier.height(3.dp))
                                    Text(
                                        text = lunchSummary(currentTT),
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary
                                    )
                                }
                            }
                        }
                    }

                    if (showSlotDialog) {
                        TimetableSlotDialog(
                            timetableId = currentTT.id,
                            entry = editingEntry,
                            initialDay = pendingDay,
                            initialPeriod = pendingPeriod,
                            subjects = subjects,
                            groups = groups,
                            onDismiss = { showSlotDialog = false },
                            onSaved = {
                                showSlotDialog = false
                                actionMsg = if (editingEntry == null) "Slot added." else "Slot saved."
                                errorMsg = null
                                scope.launch {
                                    entries = CoordinatorAdminRepository
                                        .getEntries(currentTT.id)
                                        .getOrDefault(entries)
                                }
                            },
                            onDelete = editingEntry?.let { target ->
                                {
                                    busy = true
                                    errorMsg = null
                                    scope.launch {
                                        CoordinatorAdminRepository.deleteEntry(target.entryId).fold(
                                            onSuccess = {
                                                busy = false
                                                showSlotDialog = false
                                                editingEntry = null
                                                actionMsg = "Slot deleted."
                                                entries = CoordinatorAdminRepository
                                                    .getEntries(currentTT.id)
                                                    .getOrDefault(entries)
                                            },
                                            onFailure = { err ->
                                                busy = false
                                                errorMsg = err.message ?: "Failed to delete the slot"
                                            }
                                        )
                                    }
                                }
                            },
                            onError = { message ->
                                errorMsg = message
                            }
                        )
                    }

                    if (showDeleteConfirm) {
                        AlertDialog(
                            onDismissRequest = { showDeleteConfirm = false },
                            title = { Text("Delete Timetable Draft?", color = Navy, fontWeight = FontWeight.Bold) },
                            text = { Text("This will permanently delete Timetable #${currentTT.id} and all its slots.", fontSize = 13.sp) },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        showDeleteConfirm = false
                                        busy = true
                                        scope.launch {
                                            CoordinatorAdminRepository.deleteTimetable(currentTT.id).fold(
                                                onSuccess = {
                                                    busy = false
                                                    actionMsg = "Timetable deleted."
                                                    val targetClass = selectedClass
                                                    if (targetClass != null) {
                                                        val refresh = CoordinatorAdminRepository
                                                            .getTimetables(listOf(targetClass.classId))
                                                            .getOrDefault(emptyList())
                                                        timetables = refresh
                                                        selectedTimetable = refresh.firstOrNull()
                                                    }
                                                },
                                                onFailure = { err ->
                                                    busy = false
                                                    errorMsg = err.message ?: "Failed to delete timetable"
                                                }
                                            )
                                        }
                                    }
                                ) {
                                    Text("Delete", color = ErrorRed, fontWeight = FontWeight.Bold)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showDeleteConfirm = false }) {
                                    Text("Cancel", color = TextSecondary)
                                }
                            },
                            containerColor = SurfaceWhite
                        )
                    }
                }
            }

            if (showDuplicateDatePicker) {
                val duplicatePickerState = rememberDatePickerState(
                    initialSelectedDateMillis = istDateToEpochMillis(newDuplicateDate)
                )
                DatePickerDialog(
                    onDismissRequest = { showDuplicateDatePicker = false },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                duplicatePickerState.selectedDateMillis?.let { picked ->
                                    newDuplicateDate = formatPickerDate(picked)
                                }
                                showDuplicateDatePicker = false
                            }
                        ) {
                            Text("Use date", color = Navy, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDuplicateDatePicker = false }) {
                            Text("Cancel", color = TextSecondary)
                        }
                    }
                ) {
                    DatePicker(
                        state = duplicatePickerState,
                        title = null,
                        headline = null,
                        showModeToggle = false
                    )
                }
            }

            if (showNewTTDatePicker) {
                val newTTPickerState = rememberDatePickerState(
                    initialSelectedDateMillis = istDateToEpochMillis(newTTEffectiveFrom)
                )
                DatePickerDialog(
                    onDismissRequest = { showNewTTDatePicker = false },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                newTTPickerState.selectedDateMillis?.let { picked ->
                                    newTTEffectiveFrom = formatPickerDate(picked)
                                }
                                showNewTTDatePicker = false
                            }
                        ) {
                            Text("Use date", color = Navy, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showNewTTDatePicker = false }) {
                            Text("Cancel", color = TextSecondary)
                        }
                    }
                ) {
                    DatePicker(
                        state = newTTPickerState,
                        title = null,
                        headline = null,
                        showModeToggle = false
                    )
                }
            }

            if (showNewTTDialog && selectedClass != null) {
                val targetClass = selectedClass!!
                AlertDialog(
                    onDismissRequest = { showNewTTDialog = false },
                    title = {
                        Text(
                            text = "New Timetable Draft",
                            color = Navy,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "Class: ${targetClass.course ?: "Class"} ${targetClass.branch ?: ""} Sem ${targetClass.semester} ${targetClass.section}",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            OutlinedTextField(
                                value = newTTEffectiveFrom,
                                onValueChange = {},
                                label = { Text("Effective Date (YYYY-MM-DD)") },
                                singleLine = true,
                                readOnly = true,
                                colors = mitrcFieldColors(),
                                trailingIcon = {
                                    IconButton(onClick = { showNewTTDatePicker = true }) {
                                        Icon(
                                            Icons.Outlined.CalendarMonth,
                                            contentDescription = "Pick effective date",
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                            OutlinedTextField(
                                value = newTTRoom,
                                onValueChange = { newTTRoom = it },
                                label = { Text("Class Room (optional, e.g. 204)") },
                                singleLine = true,
                                colors = mitrcFieldColors(),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showNewTTDialog = false
                                busy = true
                                scope.launch {
                                    val res = CoordinatorAdminRepository.createTimetable(
                                        classId = targetClass.classId,
                                        effectiveFrom = newTTEffectiveFrom,
                                        room = newTTRoom
                                    )
                                    busy = false
                                    res.fold(
                                        onSuccess = { createdTT ->
                                            actionMsg = "Created Timetable #${createdTT.id}."
                                            val refresh = CoordinatorAdminRepository
                                                .getTimetables(listOf(targetClass.classId))
                                                .getOrDefault(emptyList())
                                            timetables = refresh
                                            selectedTimetable = refresh.firstOrNull { it.id == createdTT.id } ?: createdTT
                                        },
                                        onFailure = { err ->
                                            errorMsg = err.message ?: "Failed to create timetable"
                                        }
                                    )
                                }
                            }
                        ) {
                            Text("Create", color = Navy, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showNewTTDialog = false }) {
                            Text("Cancel", color = TextSecondary)
                        }
                    },
                    containerColor = SurfaceWhite
                )
            }

            Spacer(Modifier.height(24.dp))

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
                            Text(
                                text = coordinatorProfile?.name ?: "Course Coordinator",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = coordinatorProfile?.designation ?: "Coordinator",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TimetableGridTable(
    entries: List<TimetableEntryView>,
    periods: List<TimetablePeriodRow>,
    lunchAfterPeriod: Int,
    lunchStart: String,
    lunchEnd: String,
    onSlotClick: (TimetableEntryView) -> Unit,
    onEmptyClick: (Int, Int) -> Unit
) {
    val periodRows = remember(periods) { periods.associateBy { it.period } }
    val allPeriods = remember(entries, periods) {
        val lastEntry = entries.maxOfOrNull { it.endPeriod } ?: 0
        val lastDefined = periods.maxOfOrNull { it.period } ?: 0
        (1..maxOf(lastEntry, lastDefined, 6)).toList()
    }
    val lunchAfter = lunchAfterPeriod.coerceIn(1, allPeriods.last())
    val beforeLunch = allPeriods.filter { it <= lunchAfter }
    val afterLunch = allPeriods.filter { it > lunchAfter }
    val entriesByDay = remember(entries) { entries.groupBy { it.dayOfWeek } }
    val today = remember { LocalDate.now().dayOfWeek.value }
    val tableWidth = GridTimeWidth + GridDayWidth * gridDays.size + GridGap * gridDays.size

    if (entries.isEmpty() && periods.isEmpty()) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = Slate,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "No slots in this draft yet. Use Add slot to create the first class.",
                fontSize = 12.sp,
                color = TextSecondary,
                modifier = Modifier.padding(14.dp)
            )
        }
        return
    }

    Column(modifier = Modifier.horizontalScroll(rememberScrollState())) {
        Row(horizontalArrangement = Arrangement.spacedBy(GridGap)) {
            GridHeaderCell(label = "TIME", width = GridTimeWidth)
            gridDays.forEach { (day, label) ->
                GridHeaderCell(
                    label = label,
                    width = GridDayWidth,
                    highlighted = day == today
                )
            }
        }

        Spacer(Modifier.height(GridGap))

        GridPeriodSection(
            sectionPeriods = beforeLunch,
            periodRows = periodRows,
            entriesByDay = entriesByDay,
            onSlotClick = onSlotClick,
            onEmptyClick = onEmptyClick
        )

        LunchBreakBand(
            width = tableWidth,
            start = lunchStart,
            end = lunchEnd,
            afterPeriod = lunchAfter
        )

        GridPeriodSection(
            sectionPeriods = afterLunch,
            periodRows = periodRows,
            entriesByDay = entriesByDay,
            onSlotClick = onSlotClick,
            onEmptyClick = onEmptyClick
        )
    }
}

@Composable
private fun GridPeriodSection(
    sectionPeriods: List<Int>,
    periodRows: Map<Int, TimetablePeriodRow>,
    entriesByDay: Map<Int, List<TimetableEntryView>>,
    onSlotClick: (TimetableEntryView) -> Unit,
    onEmptyClick: (Int, Int) -> Unit
) {
    if (sectionPeriods.isEmpty()) return

    Row(horizontalArrangement = Arrangement.spacedBy(GridGap)) {
        Column(verticalArrangement = Arrangement.spacedBy(GridGap)) {
            sectionPeriods.forEach { period ->
                GridTimeCell(period = period, row = periodRows[period])
            }
        }
        gridDays.forEach { (day, _) ->
            GridDayColumn(
                day = day,
                sectionPeriods = sectionPeriods,
                dayEntries = entriesByDay[day].orEmpty(),
                onSlotClick = onSlotClick,
                onEmptyClick = onEmptyClick
            )
        }
    }
}

@Composable
private fun GridDayColumn(
    day: Int,
    sectionPeriods: List<Int>,
    dayEntries: List<TimetableEntryView>,
    onSlotClick: (TimetableEntryView) -> Unit,
    onEmptyClick: (Int, Int) -> Unit
) {
    val byStart = remember(dayEntries) { dayEntries.groupBy { it.startPeriod } }
    val sectionStart = sectionPeriods.first()
    val sectionEnd = sectionPeriods.last()
    val carryOver = dayEntries.filter { it.startPeriod < sectionStart && it.endPeriod >= sectionStart }
    val carrySpan = carryOver.maxOfOrNull {
        (minOf(it.endPeriod, sectionEnd) - sectionStart + 1).coerceAtLeast(1)
    } ?: 0

    Column(verticalArrangement = Arrangement.spacedBy(GridGap)) {
        var index = 0
        if (carryOver.isNotEmpty() && carrySpan > 0) {
            GridSlotCell(
                entries = carryOver,
                span = carrySpan.coerceAtMost(sectionPeriods.size),
                onClick = { onSlotClick(carryOver.first()) }
            )
            index = carrySpan.coerceAtMost(sectionPeriods.size)
        }
        while (index < sectionPeriods.size) {
            val period = sectionPeriods[index]
            val cellEntries = byStart[period].orEmpty()
            if (cellEntries.isEmpty()) {
                GridEmptyCell(onClick = { onEmptyClick(day, period) })
                index += 1
            } else {
                val lastPeriod = cellEntries.maxOf { it.endPeriod }
                val span = (lastPeriod - period + 1).coerceIn(1, sectionPeriods.size - index)
                GridSlotCell(
                    entries = cellEntries,
                    span = span,
                    onClick = { onSlotClick(cellEntries.first()) }
                )
                index += span
            }
        }
    }
}

@Composable
private fun GridHeaderCell(label: String, width: Dp, highlighted: Boolean = false) {
    Box(
        modifier = Modifier
            .width(width)
            .height(28.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (highlighted) NavySoft else Navy),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (highlighted) GoldLight else Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun GridTimeCell(period: Int, row: TimetablePeriodRow?) {
    Surface(
        shape = RoundedCornerShape(7.dp),
        color = Slate,
        border = BorderStroke(1.dp, DividerSoft),
        modifier = Modifier
            .width(GridTimeWidth)
            .height(GridRowHeight)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = "P$period", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Navy)
            if (row != null) {
                Spacer(Modifier.height(2.dp))
                Text(text = formatClock(row.startTime, "h:mm"), fontSize = 9.sp, color = TextSecondary)
                Text(text = formatClock(row.endTime, "h:mm"), fontSize = 9.sp, color = TextSecondary)
            }
        }
    }
}

@Composable
private fun GridEmptyCell(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .width(GridDayWidth)
            .height(GridRowHeight)
            .clip(RoundedCornerShape(7.dp))
            .background(Slate)
            .border(1.dp, DividerSoft.copy(alpha = 0.7f), RoundedCornerShape(7.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "+",
            color = TextSecondary.copy(alpha = 0.4f),
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun GridSlotCell(
    entries: List<TimetableEntryView>,
    span: Int,
    onClick: () -> Unit
) {
    val height = GridRowHeight * span + GridGap * (span - 1)
    val primary = entries.first()
    val isLab = entries.any { it.groupId != null } || primary.subjectKind.equals("lab", true)
    val accent = if (isLab) Gold else Navy

    Surface(
        shape = RoundedCornerShape(7.dp),
        color = accent.copy(alpha = 0.07f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.3f)),
        modifier = Modifier
            .width(GridDayWidth)
            .height(height)
            .clip(RoundedCornerShape(7.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 5.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.Center
        ) {
            entries.forEachIndexed { index, entry ->
                if (index > 0) {
                    HorizontalDivider(color = accent.copy(alpha = 0.2f), thickness = 0.5.dp)
                    Spacer(Modifier.height(3.dp))
                }

                val code = entry.subjectCode?.takeIf { it.isNotBlank() }
                val free = code == null && entry.subjectName.isNullOrBlank()

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = code ?: "FREE",
                        color = if (free) TextSecondary else Navy,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    val group = entry.groupName?.takeIf { it.isNotBlank() }
                    if (group != null) {
                        Spacer(Modifier.width(3.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(3.dp))
                                .background(Gold.copy(alpha = 0.22f))
                                .padding(horizontal = 3.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = group,
                                color = Navy,
                                fontSize = 7.5.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }
                }

                if (!free) {
                    val teacher = entry.teacherName?.takeIf { it.isNotBlank() }
                    if (teacher != null) {
                        Text(
                            text = teacher,
                            color = TextSecondary,
                            fontSize = 8.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (entries.size == 1) {
                        val place = entry.room?.takeIf { it.isNotBlank() }
                            ?: entry.classRoom?.takeIf { it.isNotBlank() }
                        if (place != null) {
                            Text(
                                text = place,
                                color = TextSecondary,
                                fontSize = 8.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LunchBreakBand(width: Dp, start: String, end: String, afterPeriod: Int) {
    Column(modifier = Modifier.width(width)) {
        Spacer(Modifier.height(GridGap))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(GridLunchHeight)
                .clip(RoundedCornerShape(7.dp))
                .background(Gold.copy(alpha = 0.14f)),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "LUNCH BREAK",
                color = Navy,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "${formatClock(start, "h:mm a")} – ${formatClock(end, "h:mm a")}",
                color = TextPrimary,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.width(8.dp))
            Text(text = "after P$afterPeriod", color = TextSecondary, fontSize = 9.sp)
        }
        Spacer(Modifier.height(GridGap))
    }
}

private const val DEFAULT_LUNCH_AFTER_PERIOD = 4
private const val DEFAULT_LUNCH_START = "13:15:00"
private const val DEFAULT_LUNCH_END = "13:45:00"

private val gridDays = listOf(
    1 to "MON",
    2 to "TUE",
    3 to "WED",
    4 to "THU",
    5 to "FRI",
    6 to "SAT"
)

private val GridTimeWidth = 58.dp
private val GridDayWidth = 98.dp
private val GridGap = 3.dp
private val GridRowHeight = 72.dp
private val GridLunchHeight = 28.dp

private fun formatClock(raw: String?, pattern: String): String {
    val text = raw.orEmpty().trim()
    val time = runCatching { LocalTime.parse(text) }.getOrNull() ?: return text
    return time.format(DateTimeFormatter.ofPattern(pattern, Locale.US))
}

private fun lunchSummary(timetable: TimetableRow): String {
    val after = timetable.lunchAfterPeriod ?: DEFAULT_LUNCH_AFTER_PERIOD
    val start = formatClock(timetable.lunchStart ?: DEFAULT_LUNCH_START, "h:mm a")
    val end = formatClock(timetable.lunchEnd ?: DEFAULT_LUNCH_END, "h:mm a")
    return "$start – $end · after period $after"
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TimetableSlotDialog(
    timetableId: Int,
    entry: TimetableEntryView?,
    initialDay: Int?,
    initialPeriod: Int?,
    subjects: List<ClassSubjectView>,
    groups: List<ClassGroupRow>,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    onDelete: (() -> Unit)?,
    onError: (String) -> Unit
) {
    val scope = rememberCoroutineScope()

    val initialSubjectId = remember(entry, subjects) {
        entry?.let { target ->
            subjects.firstOrNull {
                it.subjectCode == target.subjectCode && it.subjectName == target.subjectName
            }?.classSubjectId
        } ?: subjects.firstOrNull()?.classSubjectId ?: 0
    }

    var day by remember(entry, initialDay) {
        mutableIntStateOf(entry?.dayOfWeek ?: initialDay ?: 1)
    }
    var startPeriod by remember(entry, initialPeriod) {
        mutableIntStateOf(entry?.startPeriod ?: initialPeriod ?: 1)
    }
    var endPeriod by remember(entry, initialPeriod) {
        mutableIntStateOf(entry?.endPeriod ?: entry?.startPeriod ?: initialPeriod ?: 1)
    }
    var subjectId by remember(entry, initialSubjectId) { mutableIntStateOf(initialSubjectId) }
    var groupId by remember(entry) { mutableStateOf(entry?.groupId) }
    var room by remember(entry) { mutableStateOf(entry?.room.orEmpty()) }

    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val selectedSubject = subjects.firstOrNull { it.classSubjectId == subjectId }
    val days = listOf(1 to "Mon", 2 to "Tue", 3 to "Wed", 4 to "Thu", 5 to "Fri", 6 to "Sat")
    val periodChoices = (1..10).toList()

    suspend fun save() {
        if (startPeriod > endPeriod) {
            error = "The start period must be before the end period."
            return
        }

        saving = true
        error = null
        val cleanSubjectId = subjectId.takeIf { it > 0 }
        val facultyId = selectedSubject?.facultyId?.takeIf { cleanSubjectId != null && it > 0 }
        val cleanRoom = room.trim().takeIf { it.isNotEmpty() }

        val result = if (entry == null) {
            CoordinatorAdminRepository.createEntry(
                NewTimetableEntryRow(
                    timetableId = timetableId,
                    dayOfWeek = day,
                    startPeriod = startPeriod,
                    endPeriod = endPeriod,
                    classSubjectId = cleanSubjectId,
                    groupId = groupId,
                    facultyId = facultyId,
                    room = cleanRoom
                )
            )
        } else {
            CoordinatorAdminRepository.updateEntry(
                TimetableEntryRow(
                    id = entry.entryId,
                    timetableId = timetableId,
                    dayOfWeek = day,
                    startPeriod = startPeriod,
                    endPeriod = endPeriod,
                    classSubjectId = cleanSubjectId,
                    groupId = groupId,
                    facultyId = facultyId,
                    room = cleanRoom
                )
            )
        }

        saving = false
        result.fold(
            onSuccess = { onSaved() },
            onFailure = { failure ->
                val message = failure.message ?: "Couldn't save this slot."
                error = message
                onError(message)
            }
        )
    }

    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        title = {
            Text(
                text = if (entry == null) "Add Timetable Slot" else "Edit Timetable Slot",
                color = Navy,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                SlotDialogLabel("Day")
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    days.forEach { (value, label) ->
                        TimetableChoiceChip(label = label, selected = day == value) { day = value }
                    }
                }

                SlotDialogLabel("Start period")
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    periodChoices.forEach { period ->
                        TimetableChoiceChip(
                            label = "$period",
                            selected = startPeriod == period
                        ) {
                            startPeriod = period
                            if (endPeriod < period) endPeriod = period
                        }
                    }
                }

                SlotDialogLabel("End period")
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    periodChoices.forEach { period ->
                        TimetableChoiceChip(
                            label = "$period",
                            selected = endPeriod == period
                        ) { endPeriod = period }
                    }
                }

                SlotDialogLabel("Subject")
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TimetableChoiceChip(
                        label = "⚡ Blank / Free",
                        selected = subjectId <= 0
                    ) { subjectId = 0 }

                    subjects.forEach { subject ->
                        TimetableChoiceChip(
                            label = subject.subjectCode,
                            selected = subjectId == subject.classSubjectId
                        ) { subjectId = subject.classSubjectId }
                    }
                }
                if (subjectId > 0 && selectedSubject != null) {
                    Text(
                        text = "${selectedSubject.subjectName} · ${selectedSubject.teacherName}",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                } else if (subjectId <= 0) {
                    Text(
                        text = "Neutral slot (no subject or teacher assigned)",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }

                if (groups.isNotEmpty()) {
                    SlotDialogLabel("Group (optional)")
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        TimetableChoiceChip(label = "All groups", selected = groupId == null) {
                            groupId = null
                        }
                        groups.forEach { group ->
                            TimetableChoiceChip(
                                label = group.name,
                                selected = groupId == group.id
                            ) { groupId = group.id }
                        }
                    }
                }

                OutlinedTextField(
                    value = room,
                    onValueChange = { room = it },
                    label = { Text("Room (optional)") },
                    singleLine = true,
                    colors = mitrcFieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                if (error != null) {
                    Text(text = error.orEmpty(), color = ErrorRed, fontSize = 11.5.sp)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { scope.launch { save() } },
                enabled = !saving && subjects.isNotEmpty()
            ) {
                Text(
                    text = if (saving) "Saving..." else "Save slot",
                    color = if (saving) TextSecondary else Navy,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        },
        dismissButton = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onDelete != null) {
                    TextButton(onClick = onDelete, enabled = !saving) {
                        Text("Delete", color = ErrorRed, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                TextButton(onClick = onDismiss, enabled = !saving) {
                    Text("Cancel", color = TextSecondary, fontSize = 13.sp)
                }
            }
        },
        containerColor = SurfaceWhite
    )
}

@Composable
private fun SlotDialogLabel(text: String) {
    Text(
        text = text.uppercase(),
        color = TextSecondary,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp
    )
}

@Composable
private fun TimetableChoiceChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(9.dp))
            .background(if (selected) Navy else Slate)
            .clickable(onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 7.dp)
    ) {
        Text(
            text = label,
            color = if (selected) Color.White else TextPrimary,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

private fun getTodayIstDate(): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    sdf.timeZone = TimeZone.getTimeZone("Asia/Kolkata")
    return sdf.format(Date())
}

private fun istDateToEpochMillis(date: String): Long? = runCatching {
    SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }.parse(date)?.time
}.getOrNull()

private fun formatPickerDate(epochMillis: Long): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }.format(Date(epochMillis))

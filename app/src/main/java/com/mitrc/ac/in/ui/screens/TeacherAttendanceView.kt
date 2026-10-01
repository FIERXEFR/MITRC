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
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
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
import com.mitrc.ac.`in`.data.AssessmentRow
import com.mitrc.ac.`in`.data.MarkRow
import com.mitrc.ac.`in`.data.PortalRepository
import com.mitrc.ac.`in`.data.StudentDirectoryView
import com.mitrc.ac.`in`.data.SubjectTeacherView
import com.mitrc.ac.`in`.data.TakeAttendanceRecord
import com.mitrc.ac.`in`.data.TeacherRow
import com.mitrc.ac.`in`.ui.theme.ErrorRed
import com.mitrc.ac.`in`.ui.theme.Gold
import com.mitrc.ac.`in`.ui.theme.Navy
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

enum class TeacherMode(val label: String) {
    ATTENDANCE("Mark Attendance"),
    MARKS("Enter Marks")
}

@Composable
fun TeacherAttendanceView(userUid: String) {
    val scope = rememberCoroutineScope()

    var activeMode by remember { mutableStateOf(TeacherMode.ATTENDANCE) }

    var loading by remember { mutableStateOf(true) }
    var teacher by remember { mutableStateOf<TeacherRow?>(null) }
    var subjects by remember { mutableStateOf<List<SubjectTeacherView>>(emptyList()) }

    var selectedSubject by remember { mutableStateOf<SubjectTeacherView?>(null) }
    var selectedDate by remember { mutableStateOf(getTodayIstDate()) }
    var selectedPeriod by remember { mutableStateOf(1) }

    var students by remember { mutableStateOf<List<StudentDirectoryView>>(emptyList()) }
    val attendanceMap = remember { mutableStateMapOf<String, String>() }

    var saving by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(userUid) {
        loading = true
        val tRes = PortalRepository.getTeacherProfile(userUid)
        teacher = tRes.getOrNull()

        val t = teacher
        if (t != null) {
            val subRes = PortalRepository.getTeacherSubjects(t.id)
            subjects = subRes.getOrDefault(emptyList())
            if (subjects.isNotEmpty()) {
                selectedSubject = subjects.first()
            }
        }
        loading = false
    }

    LaunchedEffect(selectedSubject, selectedDate, selectedPeriod) {
        statusMessage = null
        errorMessage = null
        val sub = selectedSubject
        if (sub != null) {
            val stdRes = PortalRepository.getEnrolledStudentsForClass(sub.classId)
            students = stdRes.getOrDefault(emptyList())

            val existingRes = PortalRepository.getExistingAttendance(sub.classSubjectId, selectedDate, selectedPeriod)
            val existing = existingRes.getOrDefault(emptyList())

            attendanceMap.clear()
            students.forEach { s ->
                val rec = existing.firstOrNull { it.studentUid == s.uid }
                attendanceMap[s.uid] = rec?.status ?: "P"
            }
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

    if (teacher == null) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Account not linked to a teacher profile.",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = ErrorRed
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Contact admin.",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            }
        }
        return
    }

    Column {
        // Mode Switcher Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Slate, RoundedCornerShape(12.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            TeacherMode.entries.forEach { mode ->
                val isSel = activeMode == mode
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSel) Navy else Color.Transparent)
                        .clickable { activeMode = mode }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = mode.label,
                        color = if (isSel) Color.White else TextSecondary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        if (activeMode == TeacherMode.MARKS) {
            TeacherMarksSection(subjects = subjects)
            return
        }

        Text(
            text = "MARK CLASS ATTENDANCE",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = TextSecondary,
            letterSpacing = 1.5.sp
        )
        Spacer(Modifier.height(10.dp))

        if (subjects.isEmpty()) {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.padding(16.dp)) {
                    Text(text = "No subjects assigned to your teacher profile.", fontSize = 13.sp, color = TextSecondary)
                }
            }
            return
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            subjects.forEach { sub ->
                val isSelected = selectedSubject?.classSubjectId == sub.classSubjectId
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) Navy else SurfaceWhite)
                        .clickable { selectedSubject = sub }
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = "${sub.subjectCode} (${sub.subjectKind.uppercase()})",
                        color = if (isSelected) Color.White else TextPrimary,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 13.sp
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Class Date (YYYY-MM-DD)", fontSize = 11.sp, color = TextSecondary)
                Spacer(Modifier.height(4.dp))
                OutlinedTextField(
                    value = selectedDate,
                    onValueChange = { selectedDate = it.trim() },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Period (1..6)", fontSize = 11.sp, color = TextSecondary)
                Spacer(Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    (1..6).forEach { pNum ->
                        val isSel = selectedPeriod == pNum
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSel) Navy else SurfaceWhite)
                                .clickable { selectedPeriod = pNum }
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "P$pNum",
                                color = if (isSel) Color.White else TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        if (students.isEmpty()) {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.padding(18.dp), contentAlignment = Alignment.Center) {
                    Text(text = "No enrolled students found for this subject class.", fontSize = 13.sp, color = TextSecondary)
                }
            }
        } else {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "ENROLLED STUDENTS (${students.size})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        letterSpacing = 1.2.sp
                    )
                    Spacer(Modifier.height(12.dp))

                    students.forEach { st ->
                        val currentStatus = attendanceMap[st.uid] ?: "P"
                        StudentAttendanceRow(
                            student = st,
                            currentStatus = currentStatus,
                            onStatusChanged = { newStatus -> attendanceMap[st.uid] = newStatus }
                        )
                        Spacer(Modifier.height(8.dp))
                    }

                    Spacer(Modifier.height(12.dp))

                    AnimatedVisibility(visible = errorMessage != null) {
                        Text(
                            text = errorMessage.orEmpty(),
                            color = ErrorRed,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(bottom = 10.dp)
                        )
                    }

                    AnimatedVisibility(visible = statusMessage != null) {
                        Text(
                            text = statusMessage.orEmpty(),
                            color = SuccessGreen,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(bottom = 10.dp)
                        )
                    }

                    Button(
                        onClick = {
                            val sub = selectedSubject ?: return@Button
                            if (selectedDate > getTodayIstDate()) {
                                errorMessage = "Cannot take attendance for a future date"
                                return@Button
                            }

                            saving = true
                            errorMessage = null
                            statusMessage = null

                            val records = students.map { s ->
                                TakeAttendanceRecord(
                                    uid = s.uid,
                                    status = attendanceMap[s.uid] ?: "P"
                                )
                            }

                            scope.launch {
                                val res = PortalRepository.saveAttendance(
                                    classSubjectId = sub.classSubjectId,
                                    classDate = selectedDate,
                                    period = selectedPeriod,
                                    records = records
                                )
                                saving = false
                                res.fold(
                                    onSuccess = {
                                        statusMessage = "Attendance saved successfully!"
                                    },
                                    onFailure = { err ->
                                        val msg = err.message.orEmpty()
                                        errorMessage = when {
                                            msg.contains("42501") || msg.contains("row-level security", ignoreCase = true) ->
                                                "You are not assigned to this subject."
                                            else -> msg.ifEmpty { "Failed to save attendance" }
                                        }
                                    }
                                )
                            }
                        },
                        enabled = !saving,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Navy)
                    ) {
                        if (saving) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                        } else {
                            Icon(Icons.Outlined.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(text = "Save Attendance Batch", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TeacherMarksSection(subjects: List<SubjectTeacherView>) {
    val scope = rememberCoroutineScope()

    var selectedSubject by remember { mutableStateOf(subjects.firstOrNull()) }
    var assessments by remember { mutableStateOf<List<AssessmentRow>>(emptyList()) }
    var selectedAssessment by remember { mutableStateOf<AssessmentRow?>(null) }

    var kind by remember { mutableStateOf("UT") }
    var numberStr by remember { mutableStateOf("1") }
    var maxMarksStr by remember { mutableStateOf("20.0") }

    var students by remember { mutableStateOf<List<StudentDirectoryView>>(emptyList()) }
    val marksMap = remember { mutableStateMapOf<String, String>() }

    var saving by remember { mutableStateOf(false) }
    var statusMsg by remember { mutableStateOf<String?>(null) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(selectedSubject) {
        val sub = selectedSubject
        if (sub != null) {
            val aRes = PortalRepository.getAssessments(sub.classSubjectId)
            assessments = aRes.getOrDefault(emptyList())
            selectedAssessment = assessments.firstOrNull()

            val stdRes = PortalRepository.getEnrolledStudentsForClass(sub.classId)
            students = stdRes.getOrDefault(emptyList())
        }
    }

    Column {
        Text(
            text = "EVALUATION & MARKS ENTRY",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = TextSecondary,
            letterSpacing = 1.5.sp
        )
        Spacer(Modifier.height(10.dp))

        if (subjects.isEmpty()) {
            Text(text = "No subjects assigned.", fontSize = 13.sp, color = TextSecondary)
            return
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            subjects.forEach { sub ->
                val isSelected = selectedSubject?.classSubjectId == sub.classSubjectId
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) Navy else SurfaceWhite)
                        .clickable { selectedSubject = sub }
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = "${sub.subjectCode} (${sub.subjectKind.uppercase()})",
                        color = if (isSelected) Color.White else TextPrimary,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 13.sp
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "ASSESSMENT DEFINITION",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary
                )
                Spacer(Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf("UT", "EXP", "ASSIGNMENT", "MID", "END").forEach { k ->
                        val isSel = kind == k
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) Navy else Slate)
                                .clickable { kind = k }
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = k,
                                color = if (isSel) Color.White else TextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = numberStr,
                        onValueChange = { numberStr = it },
                        label = { Text("Number") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )
                    OutlinedTextField(
                        value = maxMarksStr,
                        onValueChange = { maxMarksStr = it },
                        label = { Text("Max Marks") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                Spacer(Modifier.height(10.dp))

                Button(
                    onClick = {
                        val sub = selectedSubject ?: return@Button
                        val num = numberStr.toIntOrNull() ?: 1
                        val maxM = maxMarksStr.toDoubleOrNull() ?: 20.0
                        saving = true
                        statusMsg = null
                        errorMsg = null
                        scope.launch {
                            val newAss = AssessmentRow(
                                classSubjectId = sub.classSubjectId,
                                kind = kind,
                                number = num,
                                maxMarks = maxM
                            )
                            val res = PortalRepository.createAssessment(newAss)
                            saving = false
                            res.fold(
                                onSuccess = { created ->
                                    selectedAssessment = created
                                    statusMsg = "Assessment created/loaded!"
                                    val refresh = PortalRepository.getAssessments(sub.classSubjectId)
                                    assessments = refresh.getOrDefault(emptyList())
                                },
                                onFailure = { err ->
                                    errorMsg = err.message ?: "Failed to create assessment"
                                }
                            )
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Navy),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "Initialize Assessment Slot", fontSize = 13.sp)
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        if (students.isNotEmpty() && selectedAssessment != null) {
            val activeAss = selectedAssessment!!
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "STUDENT SCORES (${activeAss.kind} #${activeAss.number} - Max: ${activeAss.maxMarks ?: 10.0})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                    Spacer(Modifier.height(12.dp))

                    students.forEach { st ->
                        val currentVal = marksMap[st.uid] ?: ""
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Slate)
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = st.name ?: "Student", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text(text = "Serial: ${st.serialNo ?: "N/A"}", fontSize = 11.sp, color = TextSecondary)
                            }
                            OutlinedTextField(
                                value = currentVal,
                                onValueChange = { marksMap[st.uid] = it },
                                placeholder = { Text("Marks") },
                                singleLine = true,
                                modifier = Modifier.width(90.dp),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    AnimatedVisibility(visible = errorMsg != null) {
                        Text(text = errorMsg.orEmpty(), color = ErrorRed, fontSize = 12.sp)
                    }
                    AnimatedVisibility(visible = statusMsg != null) {
                        Text(text = statusMsg.orEmpty(), color = SuccessGreen, fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            saving = true
                            statusMsg = null
                            errorMsg = null
                            val markRows = students.mapNotNull { st ->
                                val score = marksMap[st.uid]?.toDoubleOrNull() ?: return@mapNotNull null
                                MarkRow(
                                    assessmentId = activeAss.id,
                                    studentUid = st.uid,
                                    marks = score
                                )
                            }
                            scope.launch {
                                val res = PortalRepository.saveMarks(markRows)
                                saving = false
                                res.fold(
                                    onSuccess = { statusMsg = "Marks saved successfully!" },
                                    onFailure = { err ->
                                        val msg = err.message.orEmpty()
                                        errorMsg = when {
                                            msg.contains("42501") || msg.contains("row-level security", ignoreCase = true) ->
                                                "You are not assigned to this subject."
                                            else -> msg.ifEmpty { "Failed to save marks" }
                                        }
                                    }
                                )
                            }
                        },
                        enabled = !saving,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Navy),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "Save Marks Roster", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun StudentAttendanceRow(
    student: StudentDirectoryView,
    currentStatus: String,
    onStatusChanged: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Slate)
            .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = student.name ?: "Student",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = "Serial: ${student.serialNo ?: "N/A"}",
                fontSize = 11.sp,
                color = TextSecondary
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf("P", "A", "L", "OD").forEach { code ->
                val isSel = currentStatus == code
                val bgColor = when {
                    !isSel -> SurfaceWhite
                    code == "P" -> SuccessGreen
                    code == "A" -> ErrorRed
                    code == "L" -> Gold
                    else -> Navy
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(bgColor)
                        .clickable { onStatusChanged(code) }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = code,
                        color = if (isSel) Color.White else TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

private fun getTodayIstDate(): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    sdf.timeZone = TimeZone.getTimeZone("Asia/Kolkata")
    return sdf.format(Date())
}

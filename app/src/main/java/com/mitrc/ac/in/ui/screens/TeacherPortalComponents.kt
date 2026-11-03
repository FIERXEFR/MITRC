package com.mitrc.ac.`in`.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import kotlinx.coroutines.delay
import com.mitrc.ac.`in`.data.ClassSubjectView
import com.mitrc.ac.`in`.data.TeacherClassInfo
import com.mitrc.ac.`in`.ui.theme.DividerSoft
import com.mitrc.ac.`in`.ui.theme.ErrorRed
import com.mitrc.ac.`in`.ui.theme.Gold
import com.mitrc.ac.`in`.ui.theme.GoldDeep
import com.mitrc.ac.`in`.ui.theme.Navy
import com.mitrc.ac.`in`.ui.theme.NavyDeep
import com.mitrc.ac.`in`.ui.theme.NavySoft
import com.mitrc.ac.`in`.ui.theme.Slate
import com.mitrc.ac.`in`.ui.theme.SuccessGreen
import com.mitrc.ac.`in`.ui.theme.SurfaceWhite
import com.mitrc.ac.`in`.ui.theme.TextPrimary
import com.mitrc.ac.`in`.ui.theme.TextSecondary
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val TeacherSuccessInk = Color(0xFF15803D)

internal fun teacherInitials(name: String?): String {
    val parts = name.orEmpty().trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    return when {
        parts.isEmpty() -> "F"
        parts.size == 1 -> parts.first().take(2).uppercase()
        else -> "${parts.first().first()}${parts.last().first()}".uppercase()
    }
}

internal fun teacherClassLabel(subject: ClassSubjectView, info: TeacherClassInfo?): String =
    info?.label ?: "Class ${subject.classId}"

internal fun teacherSubjectLabel(subject: ClassSubjectView): String =
    "${subject.subjectCode}  ·  ${subject.subjectName}"

internal fun teacherKindLabel(kind: String): String = when (kind.lowercase()) {
    "lab" -> "Lab"
    "theory" -> "Theory"
    else -> kind.replaceFirstChar { it.uppercase() }
}

internal fun teacherFileLabel(name: String?): String = name.orEmpty().trim().ifEmpty { "Selected file" }

internal fun teacherShortTimestamp(raw: String?): String {
    val value = raw?.trim().orEmpty()
    if (value.isEmpty()) return "—"
    return runCatching {
        OffsetDateTime.parse(value.replace(' ', 'T'))
            .atZoneSameInstant(ZoneId.of("Asia/Kolkata"))
            .format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.US))
    }.getOrElse { value.take(10) }
}

@Composable
internal fun TeacherSectionHeader(
    eyebrow: String,
    title: String,
    subtitle: String? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = eyebrow.uppercase(),
                color = TextSecondary,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
            )
            Spacer(Modifier.height(5.dp))
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 25.sp,
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(12.dp))
            trailing()
        }
    }
}

@Composable
internal fun TeacherClassSummaryCard(
    subject: ClassSubjectView,
    info: TeacherClassInfo?,
    label: String = "CURRENT CLASS",
    compact: Boolean = false,
    onClick: (() -> Unit)? = null,
    showChevron: Boolean = onClick != null,
    expanded: Boolean = false,
) {
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(220, easing = FastOutSlowInEasing),
        label = "classPickerChevron",
    )
    val borderColor by animateColorAsState(
        targetValue = if (expanded) Gold.copy(alpha = 0.62f) else DividerSoft,
        animationSpec = tween(180, easing = FastOutSlowInEasing),
        label = "classPickerBorder",
    )

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = SurfaceWhite,
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
    ) {
        Row(
            modifier = Modifier.padding(if (compact) 13.dp else 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(if (compact) 38.dp else 44.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(Navy.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = subject.subjectCode.take(3).uppercase(),
                    color = Navy,
                    fontSize = if (compact) 10.5.sp else 11.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label.uppercase(),
                    color = TextSecondary,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = subject.subjectName,
                    color = TextPrimary,
                    fontSize = if (compact) 13.sp else 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${teacherClassLabel(subject, info)}  ·  ${teacherKindLabel(subject.subjectKind)}",
                    color = TextSecondary,
                    fontSize = 10.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (showChevron) {
                Icon(
                    imageVector = Icons.Outlined.ExpandMore,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier
                        .size(20.dp)
                        .rotate(chevronRotation),
                )
            }
        }
    }
}

/**
 * The single class control used by Overview, Classroom and Notices. The card itself mirrors the
 * "Marking for" / "Materials for" summary, while the popup keeps the same white surface language
 * as the rest of the portal instead of inheriting a dark Material menu container.
 */
@Composable
internal fun TeacherClassPickerCard(
    subjects: List<ClassSubjectView>,
    selectedSubject: ClassSubjectView?,
    classInfo: Map<Int, TeacherClassInfo>,
    label: String,
    compact: Boolean = false,
    onSelectSubject: (ClassSubjectView) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var menuMounted by remember { mutableStateOf(false) }
    var anchorWidthPx by remember { mutableIntStateOf(0) }
    var anchorHeightPx by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val menuTransition = remember { MutableTransitionState(false) }
    val current = selectedSubject ?: subjects.firstOrNull()
    val menuWidth = with(density) { anchorWidthPx.toDp() }.coerceAtMost(420.dp)

    LaunchedEffect(expanded) {
        if (expanded) {
            menuMounted = true
            menuTransition.targetState = true
        } else {
            menuTransition.targetState = false
            if (menuMounted) {
                delay(150)
                menuMounted = false
            }
        }
    }

    Box(
        modifier = Modifier.onGloballyPositioned { coordinates ->
            anchorWidthPx = coordinates.size.width
            anchorHeightPx = coordinates.size.height
        },
    ) {
        if (current == null) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = SurfaceWhite,
                border = BorderStroke(1.dp, DividerSoft),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "No class assigned yet",
                    color = TextSecondary,
                    fontSize = 12.5.sp,
                    modifier = Modifier.padding(16.dp),
                )
            }
        } else {
            TeacherClassSummaryCard(
                subject = current,
                info = classInfo[current.classId],
                label = label,
                compact = compact,
                onClick = { expanded = true },
                expanded = expanded,
            )
        }

        if (menuMounted && current != null && anchorWidthPx > 0) {
            Popup(
                alignment = Alignment.TopStart,
                offset = IntOffset(0, anchorHeightPx + with(density) { 6.dp.roundToPx() }),
                onDismissRequest = { expanded = false },
                properties = PopupProperties(focusable = true),
            ) {
                AnimatedVisibility(
                    visibleState = menuTransition,
                    enter = fadeIn(tween(170)) +
                        scaleIn(
                            initialScale = 0.94f,
                            animationSpec = tween(220, easing = FastOutSlowInEasing),
                            transformOrigin = TransformOrigin(0.5f, 0f),
                        ) +
                        slideInVertically(
                            animationSpec = tween(220, easing = FastOutSlowInEasing),
                        ) { -it / 20 },
                    exit = fadeOut(tween(120)) +
                        scaleOut(
                            targetScale = 0.97f,
                            animationSpec = tween(140, easing = FastOutSlowInEasing),
                            transformOrigin = TransformOrigin(0.5f, 0f),
                        ) +
                        slideOutVertically(animationSpec = tween(140)) { -it / 24 },
                ) {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = SurfaceWhite,
                        border = BorderStroke(1.dp, DividerSoft),
                        shadowElevation = 18.dp,
                        modifier = Modifier.width(menuWidth),
                    ) {
                        Column(
                            modifier = Modifier
                                .heightIn(max = 360.dp)
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 6.dp, vertical = 6.dp),
                        ) {
                            Text(
                                text = "SWITCH CLASS",
                                color = TextSecondary,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.1.sp,
                                modifier = Modifier.padding(start = 10.dp, end = 10.dp, top = 6.dp, bottom = 5.dp),
                            )

                            subjects.forEach { subject ->
                                TeacherClassPickerRow(
                                    subject = subject,
                                    info = classInfo[subject.classId],
                                    selected = subject.classSubjectId == current?.classSubjectId,
                                    onClick = {
                                        expanded = false
                                        onSelectSubject(subject)
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TeacherClassPickerRow(
    subject: ClassSubjectView,
    info: TeacherClassInfo?,
    selected: Boolean,
    onClick: () -> Unit,
) {
    TeacherPickerMenuRow(
        badge = subject.subjectCode.take(3).uppercase(),
        title = "${subject.subjectCode}  ·  ${subject.subjectName}",
        subtitle = teacherClassLabel(subject, info),
        selected = selected,
        onClick = onClick,
    )
}

@Composable
private fun TeacherPickerMenuRow(
    badge: String,
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) Navy.copy(alpha = 0.055f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (selected) Navy.copy(alpha = 0.1f) else Slate),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = badge,
                color = if (selected) Navy else TextSecondary,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.ExtraBold,
            )
        }

        Spacer(Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = TextSecondary,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        if (selected) {
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = null,
                tint = GoldDeep,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
internal fun TeacherAttendanceFilterPicker(
    selected: String?,
    total: Int,
    present: Int,
    absent: Int,
    leave: Int,
    onDuty: Int,
    onSelect: (String?) -> Unit,
) {
    val options = listOf(
        TeacherFilterOption(
            key = null,
            badge = "ALL",
            title = "All students",
            subtitle = teacherStudentCountLabel(total),
            count = total,
        ),
        TeacherFilterOption(
            key = "P",
            badge = "P",
            title = "Present",
            subtitle = teacherStudentCountLabel(present),
            count = present,
        ),
        TeacherFilterOption(
            key = "A",
            badge = "A",
            title = "Absent",
            subtitle = teacherStudentCountLabel(absent),
            count = absent,
        ),
        TeacherFilterOption(
            key = "L",
            badge = "L",
            title = "Leave",
            subtitle = teacherStudentCountLabel(leave),
            count = leave,
        ),
        TeacherFilterOption(
            key = "OD",
            badge = "OD",
            title = "On duty",
            subtitle = teacherStudentCountLabel(onDuty),
            count = onDuty,
        ),
    )
    val current = options.firstOrNull { it.key == selected } ?: options.first()

    var expanded by remember { mutableStateOf(false) }
    var menuMounted by remember { mutableStateOf(false) }
    var anchorWidthPx by remember { mutableIntStateOf(0) }
    var anchorHeightPx by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val menuTransition = remember { MutableTransitionState(false) }
    val menuWidth = with(density) { anchorWidthPx.toDp() }.coerceAtMost(420.dp)
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(220, easing = FastOutSlowInEasing),
        label = "attendanceFilterChevron",
    )
    val borderColor by animateColorAsState(
        targetValue = if (expanded) Gold.copy(alpha = 0.62f) else DividerSoft,
        animationSpec = tween(180, easing = FastOutSlowInEasing),
        label = "attendanceFilterBorder",
    )

    LaunchedEffect(expanded) {
        if (expanded) {
            menuMounted = true
            menuTransition.targetState = true
        } else {
            menuTransition.targetState = false
            if (menuMounted) {
                delay(150)
                menuMounted = false
            }
        }
    }

    Box(
        modifier = Modifier.onGloballyPositioned { coordinates ->
            anchorWidthPx = coordinates.size.width
            anchorHeightPx = coordinates.size.height
        },
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = SurfaceWhite,
            border = BorderStroke(1.dp, borderColor),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true },
        ) {
            Row(
                modifier = Modifier.padding(13.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(Navy.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = current.badge,
                        color = Navy,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "FILTER STUDENTS",
                        color = TextSecondary,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = current.title,
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = if (current.key == null) {
                            teacherStudentCountLabel(total)
                        } else {
                            teacherFilterSummary(current.count, total)
                        },
                        color = TextSecondary,
                        fontSize = 10.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Icon(
                    imageVector = Icons.Outlined.ExpandMore,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier
                        .size(20.dp)
                        .rotate(chevronRotation),
                )
            }
        }

        if (menuMounted && anchorWidthPx > 0) {
            Popup(
                alignment = Alignment.TopStart,
                offset = IntOffset(0, anchorHeightPx + with(density) { 6.dp.roundToPx() }),
                onDismissRequest = { expanded = false },
                properties = PopupProperties(focusable = true),
            ) {
                AnimatedVisibility(
                    visibleState = menuTransition,
                    enter = fadeIn(tween(170)) +
                        scaleIn(
                            initialScale = 0.94f,
                            animationSpec = tween(220, easing = FastOutSlowInEasing),
                            transformOrigin = TransformOrigin(0.5f, 0f),
                        ) +
                        slideInVertically(
                            animationSpec = tween(220, easing = FastOutSlowInEasing),
                        ) { -it / 20 },
                    exit = fadeOut(tween(120)) +
                        scaleOut(
                            targetScale = 0.97f,
                            animationSpec = tween(140, easing = FastOutSlowInEasing),
                            transformOrigin = TransformOrigin(0.5f, 0f),
                        ) +
                        slideOutVertically(animationSpec = tween(140)) { -it / 24 },
                ) {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = SurfaceWhite,
                        border = BorderStroke(1.dp, DividerSoft),
                        shadowElevation = 18.dp,
                        modifier = Modifier.width(menuWidth),
                    ) {
                        Column(
                            modifier = Modifier
                                .heightIn(max = 360.dp)
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 6.dp, vertical = 6.dp),
                        ) {
                            Text(
                                text = "FILTER STUDENTS",
                                color = TextSecondary,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.1.sp,
                                modifier = Modifier.padding(start = 10.dp, end = 10.dp, top = 6.dp, bottom = 5.dp),
                            )

                            options.forEach { option ->
                                TeacherPickerMenuRow(
                                    badge = option.badge,
                                    title = option.title,
                                    subtitle = option.subtitle,
                                    selected = option.key == current.key,
                                    onClick = {
                                        expanded = false
                                        onSelect(option.key)
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class TeacherFilterOption(
    val key: String?,
    val badge: String,
    val title: String,
    val subtitle: String,
    val count: Int,
)

private fun teacherStudentCountLabel(count: Int): String =
    if (count == 1) "1 student" else "$count students"

private fun teacherFilterSummary(count: Int, total: Int): String =
    if (total == 1) "1 of 1 student" else "$count of $total students"

@Composable
internal fun TeacherClassSelectorCard(
    subject: ClassSubjectView,
    info: TeacherClassInfo?,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val container = if (selected) Navy else SurfaceWhite
    val primary = if (selected) Color.White else TextPrimary
    val secondary = if (selected) Color.White.copy(alpha = 0.68f) else TextSecondary

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = container,
        border = BorderStroke(1.dp, if (selected) Gold else DividerSoft),
        shadowElevation = if (selected) 5.dp else 0.dp,
        modifier = modifier
            .width(238.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(if (selected) Gold.copy(alpha = 0.2f) else Slate),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = subject.subjectCode.take(3).uppercase(),
                        color = if (selected) Gold else Navy,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
                Spacer(Modifier.width(9.dp))
                Text(
                    text = subject.subjectCode,
                    color = primary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (selected) Gold.copy(alpha = 0.18f) else Gold.copy(alpha = 0.14f))
                        .padding(horizontal = 7.dp, vertical = 3.dp),
                ) {
                    Text(
                        text = teacherKindLabel(subject.subjectKind).uppercase(),
                        color = if (selected) Gold else Navy,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = subject.subjectName,
                color = primary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                lineHeight = 18.sp,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(5.dp))
            Text(
                text = teacherClassLabel(subject, info),
                color = secondary,
                fontSize = 10.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
internal fun TeacherClassQuickSwitch(
    subjects: List<ClassSubjectView>,
    selectedClassSubjectId: Int?,
    onSelectSubject: (ClassSubjectView) -> Unit,
) {
    if (subjects.size <= 1) return

    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text(
            text = "SWITCH CLASS",
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
            subjects.forEach { subject ->
                TeacherChoiceChip(
                    label = subject.subjectCode,
                    selected = subject.classSubjectId == selectedClassSubjectId,
                    onClick = { onSelectSubject(subject) },
                )
            }
        }
    }
}

@Composable
internal fun TeacherChoiceChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val container = if (selected) Navy else Slate
    val content = if (selected) Color.White else TextSecondary
    Box(
        modifier = Modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(container)
            .border(1.dp, if (selected) Navy else DividerSoft, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = content,
            fontSize = 11.5.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
        )
    }
}

@Composable
internal fun TeacherPrimaryButton(
    label: String,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    loading: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Navy,
            contentColor = Color.White,
            disabledContainerColor = Navy.copy(alpha = 0.35f),
            disabledContentColor = Color.White.copy(alpha = 0.8f),
        ),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 50.dp),
    ) {
        if (loading) {
            androidx.compose.material3.CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = Color.White,
                strokeWidth = 2.dp,
            )
        } else {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(label, fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
internal fun TeacherActionTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color = Navy,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = SurfaceWhite,
        border = BorderStroke(1.dp, DividerSoft),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(15.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(accent.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(21.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = TextPrimary, fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(2.dp))
                Text(
                    subtitle,
                    color = TextSecondary,
                    fontSize = 10.5.sp,
                    maxLines = 2,
                    lineHeight = 14.sp,
                )
            }
            Icon(
                Icons.Outlined.ExpandMore,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier
                    .size(19.dp)
                    .rotate(-90f),
            )
        }
    }
}

@Composable
internal fun TeacherEmptyState(
    icon: ImageVector,
    title: String,
    body: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 34.dp, horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(Navy.copy(alpha = 0.07f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = Navy.copy(alpha = 0.65f), modifier = Modifier.size(26.dp))
        }
        Spacer(Modifier.height(14.dp))
        Text(
            text = title,
            color = TextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(5.dp))
        Text(
            text = body,
            color = TextSecondary,
            fontSize = 12.sp,
            lineHeight = 17.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = onAction) {
                Text(actionLabel, color = Navy, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
internal fun TeacherStatusBanner(
    message: String?,
    isError: Boolean = false,
) {
    AnimatedVisibility(
        visible = !message.isNullOrBlank(),
        enter = expandVertically(animationSpec = tween(180, easing = FastOutSlowInEasing)) + fadeIn(tween(160)),
        exit = shrinkVertically(animationSpec = tween(140)) + fadeOut(tween(120)),
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (isError) ErrorRed.copy(alpha = 0.08f) else SuccessGreen.copy(alpha = 0.09f),
            border = BorderStroke(1.dp, if (isError) ErrorRed.copy(alpha = 0.2f) else SuccessGreen.copy(alpha = 0.2f)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = message.orEmpty(),
                color = if (isError) ErrorRed else TeacherSuccessInk,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 16.sp,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            )
        }
    }
}

@Composable
internal fun teacherFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    disabledTextColor = TextPrimary.copy(alpha = 0.6f),
    cursorColor = Navy,
    focusedBorderColor = Navy,
    unfocusedBorderColor = DividerSoft,
    focusedContainerColor = SurfaceWhite,
    unfocusedContainerColor = SurfaceWhite,
    focusedLabelColor = Navy,
    unfocusedLabelColor = TextSecondary,
    focusedPlaceholderColor = TextSecondary.copy(alpha = 0.75f),
    unfocusedPlaceholderColor = TextSecondary.copy(alpha = 0.75f),
)

@Composable
internal fun TeacherDialogShell(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = SurfaceWhite,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .heightIn(max = 680.dp)
                .imePadding(),
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                listOf(NavyDeep, Navy, NavySoft),
                            ),
                        )
                        .padding(start = 18.dp, end = 8.dp, top = 16.dp, bottom = 16.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Gold.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(icon, contentDescription = null, tint = Gold, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(11.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = title,
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = subtitle,
                                color = Color.White.copy(alpha = 0.68f),
                                fontSize = 10.5.sp,
                                maxLines = 2,
                                lineHeight = 14.sp,
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(
                                Icons.Outlined.Close,
                                contentDescription = "Close dialog",
                                tint = Color.White.copy(alpha = 0.85f),
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(18.dp),
                ) {
                    content()
                }
            }
        }
    }
}

@Composable
internal fun TeacherDialogActions(
    confirmLabel: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    confirmEnabled: Boolean = true,
    loading: Boolean = false,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        TextButton(
            onClick = onCancel,
            enabled = !loading,
            modifier = Modifier.heightIn(min = 48.dp),
        ) {
            Text("Cancel", color = TextSecondary, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
        }
        TeacherPrimaryButton(
            label = confirmLabel,
            enabled = confirmEnabled,
            loading = loading,
            onClick = onConfirm,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
internal fun TeacherDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(DividerSoft),
    )
}

@Composable
internal fun TeacherGoldRule(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .width(34.dp)
            .height(3.dp)
            .clip(CircleShape)
            .background(Brush.horizontalGradient(listOf(GoldDeep, Gold))),
    )
}

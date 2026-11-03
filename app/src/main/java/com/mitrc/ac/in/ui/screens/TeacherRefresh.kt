package com.mitrc.ac.`in`.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mitrc.ac.`in`.ui.theme.DividerSoft
import com.mitrc.ac.`in`.ui.theme.Slate
import com.mitrc.ac.`in`.ui.theme.SurfaceWhite
import kotlinx.coroutines.launch

/**
 * Shared pull-to-refresh wrapper for the teacher tabs. It mirrors the student and coordinator
 * portals: a light haptic tick on the gesture, then the tab's shimmer skeleton until the
 * refresh coroutine completes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TeacherRefreshBox(
    onRefresh: suspend () -> Unit,
    skeleton: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    var isRefreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            scope.launch {
                isRefreshing = true
                // `finally` keeps a failed query from wedging the skeleton on screen.
                try {
                    onRefresh()
                } finally {
                    isRefreshing = false
                }
            }
        },
        modifier = Modifier.fillMaxSize(),
    ) {
        if (isRefreshing) skeleton() else content()
    }
}

internal enum class TeacherTabSkeletonKind {
    Overview,
    Classroom,
    Notices,
}

@Composable
internal fun TeacherTabSkeleton(kind: TeacherTabSkeletonKind) {
    // One clock for the whole skeleton. Each block used to remember its own infinite transition,
    // so a 20-block overview recomposed twenty times per frame on cold start and on every refresh.
    val shimmer = rememberInfiniteTransition(label = "teacherSkeletonShimmer")
    val shimmerProgress = shimmer.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "teacherSkeletonShimmerProgress",
    )

    CompositionLocalProvider(LocalShimmerProgress provides shimmerProgress) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            when (kind) {
                TeacherTabSkeletonKind.Overview -> OverviewSkeleton()
                TeacherTabSkeletonKind.Classroom -> ClassroomSkeleton()
                TeacherTabSkeletonKind.Notices -> NoticesSkeleton()
            }
        }
    }
}

@Composable
private fun TeacherListSkeletonShimmer(content: @Composable () -> Unit) {
    val shimmer = rememberInfiniteTransition(label = "teacherListSkeletonShimmer")
    val shimmerProgress = shimmer.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "teacherListSkeletonShimmerProgress",
    )
    CompositionLocalProvider(LocalShimmerProgress provides shimmerProgress) {
        content()
    }
}

@Composable
internal fun TeacherAttendanceListSkeleton(count: Int = 3) {
    TeacherListSkeletonShimmer {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            repeat(count) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = SurfaceWhite,
                    border = BorderStroke(1.dp, DividerSoft),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(13.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SkeletonCircle(42.dp)
                            Spacer(Modifier.width(11.dp))
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                SkeletonBlock(Modifier.fillMaxWidth(0.56f).height(13.dp))
                                SkeletonBlock(Modifier.width(92.dp).height(9.dp))
                            }
                            SkeletonBlock(Modifier.width(58.dp).height(20.dp))
                        }
                        Spacer(Modifier.height(11.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            repeat(4) {
                                SkeletonBlock(
                                    Modifier
                                        .weight(1f)
                                        .height(34.dp),
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
internal fun TeacherMarksListSkeleton(count: Int = 3) {
    TeacherListSkeletonShimmer {
        Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
            repeat(count) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Slate)
                        .padding(horizontal = 11.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    SkeletonCircle(42.dp)
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        SkeletonBlock(Modifier.fillMaxWidth(0.5f).height(12.dp))
                        SkeletonBlock(Modifier.width(72.dp).height(9.dp))
                    }
                    SkeletonCircle(58.dp)
                    SkeletonBlock(Modifier.size(36.dp))
                }
            }
        }
    }
}

@Composable
private fun SkeletonCircle(size: Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .shimmerEffect(),
    )
}

@Composable
private fun OverviewSkeleton() {
    SummaryCardSkeleton()
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        repeat(3) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SurfaceWhite,
                border = BorderStroke(1.dp, DividerSoft),
                modifier = Modifier.weight(1f),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    SkeletonBlock(Modifier.width(30.dp).height(20.dp))
                    SkeletonBlock(Modifier.fillMaxWidth(0.72f).height(10.dp))
                    SkeletonBlock(Modifier.fillMaxWidth(0.5f).height(8.dp))
                }
            }
        }
    }
    SkeletonLabel()
    repeat(3) { ActionTileSkeleton() }
}

@Composable
private fun ClassroomSkeleton() {
    SummaryCardSkeleton()
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        repeat(3) {
            SkeletonBlock(
                Modifier
                    .weight(1f)
                    .height(48.dp),
            )
        }
    }
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = SurfaceWhite,
        border = BorderStroke(1.dp, DividerSoft),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SkeletonBlock(Modifier.width(84.dp).height(9.dp))
            SkeletonBlock(Modifier.fillMaxWidth(0.62f).height(18.dp))
            SkeletonBlock(Modifier.fillMaxWidth().height(66.dp))
        }
    }
    repeat(4) { ListRowSkeleton() }
}

@Composable
private fun NoticesSkeleton() {
    SummaryCardSkeleton()
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = SurfaceWhite,
        border = BorderStroke(1.dp, DividerSoft),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(17.dp),
            horizontalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            SkeletonBlock(Modifier.size(46.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                SkeletonBlock(Modifier.width(150.dp).height(14.dp))
                SkeletonBlock(Modifier.fillMaxWidth(0.78f).height(10.dp))
            }
        }
    }
    repeat(4) { NoticeCardSkeleton() }
}

@Composable
private fun SummaryCardSkeleton() {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = SurfaceWhite,
        border = BorderStroke(1.dp, DividerSoft),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SkeletonBlock(Modifier.size(38.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                SkeletonBlock(Modifier.width(72.dp).height(8.dp))
                SkeletonBlock(Modifier.fillMaxWidth(0.66f).height(14.dp))
                SkeletonBlock(Modifier.fillMaxWidth(0.46f).height(9.dp))
            }
            SkeletonBlock(Modifier.size(20.dp))
        }
    }
}

@Composable
private fun ActionTileSkeleton() {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = SurfaceWhite,
        border = BorderStroke(1.dp, DividerSoft),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SkeletonBlock(Modifier.size(38.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                SkeletonBlock(Modifier.width(138.dp).height(12.dp))
                SkeletonBlock(Modifier.fillMaxWidth(0.68f).height(9.dp))
            }
        }
    }
}

@Composable
private fun ListRowSkeleton() {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = SurfaceWhite,
        border = BorderStroke(1.dp, DividerSoft),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SkeletonBlock(Modifier.size(38.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                SkeletonBlock(Modifier.fillMaxWidth(0.48f).height(12.dp))
                SkeletonBlock(Modifier.width(76.dp).height(9.dp))
            }
            SkeletonBlock(Modifier.width(74.dp).height(34.dp))
        }
    }
}

@Composable
private fun NoticeCardSkeleton() {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = SurfaceWhite,
        border = BorderStroke(1.dp, DividerSoft),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(15.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                SkeletonBlock(Modifier.size(42.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    SkeletonBlock(Modifier.fillMaxWidth(0.7f).height(13.dp))
                    SkeletonBlock(Modifier.width(94.dp).height(8.dp))
                }
            }
            SkeletonBlock(Modifier.fillMaxWidth().height(9.dp))
            SkeletonBlock(Modifier.fillMaxWidth(0.62f).height(9.dp))
        }
    }
}

@Composable
private fun SkeletonLabel() {
    SkeletonBlock(Modifier.width(98.dp).height(9.dp))
}

@Composable
private fun SkeletonBlock(modifier: Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .shimmerEffect(),
    )
}

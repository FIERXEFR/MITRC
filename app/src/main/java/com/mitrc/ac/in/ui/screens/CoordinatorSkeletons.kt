package com.mitrc.ac.`in`.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * Shimmer skeletons for the coordinator portal. They mirror the real layouts (same card
 * heights, radii and paddings) so a pull-to-refresh swaps content for a preview of itself
 * instead of a spinner.
 */

@Composable
private fun SkeletonBlock(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(6.dp)
) {
    Box(modifier = modifier.clip(shape).shimmerEffect())
}

@Composable
private fun SkeletonCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(18.dp),
    content: @Composable () -> Unit
) {
    Surface(
        shape = shape,
        color = PortalCard,
        border = BorderStroke(1.dp, PortalStroke),
        modifier = modifier
    ) {
        content()
    }
}

@Composable
internal fun CoordinatorOverviewSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SkeletonBlock(Modifier.width(190.dp).height(13.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            repeat(2) {
                SkeletonCard(modifier = Modifier.weight(1f).height(116.dp)) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SkeletonBlock(Modifier.size(36.dp), RoundedCornerShape(10.dp))
                        SkeletonBlock(Modifier.width(52.dp).height(20.dp))
                        SkeletonBlock(Modifier.width(82.dp).height(11.dp))
                        SkeletonBlock(Modifier.width(64.dp).height(10.dp))
                    }
                }
            }
        }

        SkeletonCard(modifier = Modifier.fillMaxWidth().height(84.dp)) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SkeletonBlock(Modifier.width(140.dp).height(14.dp))
                    SkeletonBlock(Modifier.width(92.dp).height(11.dp))
                }
                SkeletonBlock(Modifier.fillMaxWidth().height(8.dp), CircleShape)
            }
        }

        SkeletonBlock(Modifier.width(210.dp).height(13.dp))

        repeat(3) {
            SkeletonCard(modifier = Modifier.fillMaxWidth().height(76.dp)) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SkeletonBlock(Modifier.size(40.dp), CircleShape)
                    Spacer(Modifier.width(12.dp))
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        SkeletonBlock(Modifier.width(140.dp).height(14.dp))
                        SkeletonBlock(Modifier.width(200.dp).height(11.dp))
                    }
                    SkeletonBlock(Modifier.width(62.dp).height(26.dp), RoundedCornerShape(8.dp))
                }
            }
        }

        SkeletonBlock(Modifier.width(200.dp).height(13.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            repeat(2) {
                SkeletonCard(modifier = Modifier.weight(1f).height(92.dp)) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SkeletonBlock(Modifier.size(22.dp))
                        SkeletonBlock(Modifier.width(72.dp).height(14.dp))
                        SkeletonBlock(Modifier.width(90.dp).height(11.dp))
                    }
                }
            }
        }
    }
}

@Composable
internal fun CoordinatorStudentListSkeleton() {
    Column(modifier = Modifier.fillMaxSize()) {
        SkeletonCard(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .height(56.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SkeletonBlock(Modifier.size(20.dp), CircleShape)
                Spacer(Modifier.width(12.dp))
                SkeletonBlock(Modifier.fillMaxWidth(0.6f).height(14.dp))
            }
        }

        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            repeat(6) {
                SkeletonCard(modifier = Modifier.fillMaxWidth().height(84.dp)) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SkeletonBlock(Modifier.size(46.dp), CircleShape)
                        Spacer(Modifier.width(14.dp))
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SkeletonBlock(Modifier.fillMaxWidth(0.55f).height(14.dp))
                            SkeletonBlock(Modifier.fillMaxWidth(0.75f).height(11.dp))
                            SkeletonBlock(Modifier.width(120.dp).height(18.dp), RoundedCornerShape(6.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun CoordinatorTimetableSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        SkeletonBlock(Modifier.width(180.dp).height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(3) {
                SkeletonBlock(Modifier.width(96.dp).height(36.dp), RoundedCornerShape(12.dp))
            }
        }

        SkeletonCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SkeletonBlock(Modifier.width(170.dp).height(14.dp))
                    SkeletonBlock(Modifier.width(72.dp).height(22.dp), RoundedCornerShape(6.dp))
                }

                SkeletonBlock(Modifier.fillMaxWidth().height(52.dp), RoundedCornerShape(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SkeletonBlock(Modifier.weight(1f).height(44.dp), RoundedCornerShape(12.dp))
                    SkeletonBlock(Modifier.weight(1f).height(44.dp), RoundedCornerShape(12.dp))
                }

                SkeletonBlock(Modifier.width(150.dp).height(12.dp))

                repeat(5) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            SkeletonBlock(Modifier.width(120.dp).height(11.dp))
                            SkeletonBlock(Modifier.width(180.dp).height(12.dp))
                            SkeletonBlock(Modifier.width(110.dp).height(11.dp))
                        }
                        SkeletonBlock(Modifier.size(20.dp), CircleShape)
                    }
                }
            }
        }
    }
}

@Composable
internal fun CoordinatorResourcesSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SkeletonBlock(Modifier.width(220.dp).height(12.dp))
        SkeletonCard(modifier = Modifier.fillMaxWidth().height(82.dp)) {
            Row(
                modifier = Modifier.padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SkeletonBlock(Modifier.size(44.dp), CircleShape)
                Spacer(Modifier.width(14.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SkeletonBlock(Modifier.width(170.dp).height(15.dp))
                    SkeletonBlock(Modifier.width(230.dp).height(11.dp))
                }
                SkeletonBlock(Modifier.size(20.dp))
            }
        }
    }
}

@Composable
internal fun CoordinatorSettingsSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SkeletonCard(modifier = Modifier.fillMaxWidth().height(122.dp)) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SkeletonBlock(Modifier.size(60.dp), CircleShape)
                Spacer(Modifier.width(16.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SkeletonBlock(Modifier.width(150.dp).height(18.dp))
                    SkeletonBlock(Modifier.width(190.dp).height(12.dp))
                    SkeletonBlock(Modifier.width(210.dp).height(11.dp))
                }
            }
        }

        SkeletonBlock(Modifier.width(160.dp).height(12.dp))

        SkeletonCard(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().height(62.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SkeletonBlock(Modifier.size(20.dp))
                Spacer(Modifier.width(14.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    SkeletonBlock(Modifier.width(130.dp).height(14.dp))
                    SkeletonBlock(Modifier.width(180.dp).height(11.dp))
                }
                SkeletonBlock(Modifier.size(20.dp))
            }
        }

        Spacer(Modifier.height(16.dp))
        SkeletonBlock(Modifier.fillMaxWidth().height(50.dp), RoundedCornerShape(14.dp))
        Spacer(Modifier.height(16.dp))
        SkeletonBlock(Modifier.fillMaxWidth(0.7f).height(11.dp))
    }
}

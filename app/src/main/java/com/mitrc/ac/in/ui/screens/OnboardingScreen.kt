package com.mitrc.ac.`in`.ui.screens

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.Login
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.HowToReg
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mitrc.ac.`in`.R
import com.mitrc.ac.`in`.ui.theme.Gold
import com.mitrc.ac.`in`.ui.theme.GoldLight
import com.mitrc.ac.`in`.ui.theme.Navy
import com.mitrc.ac.`in`.ui.theme.NavyDeep
import com.mitrc.ac.`in`.ui.theme.NavySoft
import com.mitrc.ac.`in`.ui.theme.SurfaceWhite
import com.mitrc.ac.`in`.ui.theme.TextPrimary
import com.mitrc.ac.`in`.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

private const val EXIT_DURATION_MS = 400
private const val EXIT_NAV_DELAY_MS = 380

private sealed interface OnboardingMedia {
    data class Photo(@param:DrawableRes val res: Int) : OnboardingMedia
    data object Features : OnboardingMedia
}

private data class OnboardingPage(
    val media: OnboardingMedia,
    val eyebrow: String,
    val title: String,
    val description: String
)

private val onboardingPages = listOf(
    OnboardingPage(
        media = OnboardingMedia.Photo(R.raw.mainbuilding),
        eyebrow = "ALWAR  \u00B7  EST. 2007",
        title = "Welcome to MITRC",
        description = "AICTE approved and affiliated to BTU Bikaner \u2014 a campus built for learning, growth and opportunity."
    ),
    OnboardingPage(
        media = OnboardingMedia.Photo(R.raw.ds),
        eyebrow = "ACADEMICS",
        title = "Learn by doing",
        description = "Modern computer labs, expert faculty and hands-on training that prepares you for the industry."
    ),
    OnboardingPage(
        media = OnboardingMedia.Photo(R.raw.transport),
        eyebrow = "CAMPUS LIFE",
        title = "Safe journeys, great days",
        description = "A dedicated bus fleet and a secure campus keep every student connected, wherever they live."
    ),
    OnboardingPage(
        media = OnboardingMedia.Features,
        eyebrow = "ALL IN ONE",
        title = "Your college in your pocket",
        description = "Notices, timetable, attendance and your profile \u2014 always just a tap away."
    )
)

/**
 * First-run introduction. Shown on every launch until the user has logged in at least once.
 *
 * @param onFinished invoked after the exit fade completes, when the user taps Skip or Get Started.
 */
@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { onboardingPages.size })
    val currentPage = pagerState.currentPage
    val isLastPage = currentPage == onboardingPages.lastIndex

    // ---- Exit fade (Skip / Get Started) -------------------------------------------------
    var exiting by remember { mutableStateOf(false) }

    fun finish() {
        if (exiting) return
        exiting = true
    }

    val exitAlpha by animateFloatAsState(
        targetValue = if (exiting) 0f else 1f,
        animationSpec = tween(EXIT_DURATION_MS, easing = FastOutSlowInEasing),
        label = "exitAlpha"
    )
    val exitScale by animateFloatAsState(
        targetValue = if (exiting) 0.96f else 1f,
        animationSpec = tween(EXIT_DURATION_MS, easing = FastOutSlowInEasing),
        label = "exitScale"
    )

    // Navigate only once the exit fade has fully covered the screen.
    LaunchedEffect(exiting) {
        if (exiting) {
            delay(EXIT_NAV_DELAY_MS.toLong())
            onFinished()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(NavyDeep, Navy, NavySoft)))
    ) {
        // Everything except the gradient fades out, so we end on a clean navy frame.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = exitAlpha
                    scaleX = exitScale
                    scaleY = exitScale
                }
        ) {
            // Decorative circles, same as the splash screen
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(280.dp)
                    .offset(x = 100.dp, y = (-90).dp)
                    .background(Gold.copy(alpha = 0.07f), CircleShape)
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .size(220.dp)
                    .offset(x = (-80).dp, y = 60.dp)
                    .background(Color.White.copy(alpha = 0.05f), CircleShape)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(top = 12.dp, bottom = 24.dp)
            ) {
                // Skip - top right corner
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    SkipButton(onClick = { finish() })
                }

                Spacer(Modifier.height(16.dp))

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    // Pre-compose the neighbouring slide so swipes never hitch on first reveal.
                    beyondViewportPageCount = 1,
                    userScrollEnabled = !exiting
                ) { page ->
                    OnboardingPageView(
                        page = onboardingPages[page],
                        pagerState = pagerState,
                        pageIndex = page,
                        // Each card sits inside its page, leaving room for its shadow and a
                        // clear gutter between cards while swiping.
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                    )
                }

                Spacer(Modifier.height(22.dp))

                PageIndicator(pagerState = pagerState, pageCount = onboardingPages.size)

                Spacer(Modifier.height(26.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                ) {
                    Button(
                        onClick = {
                            if (isLastPage) {
                                finish()
                            } else {
                                scope.launch {
                                    pagerState.animateScrollToPage(
                                        page = currentPage + 1,
                                        animationSpec = tween(560, easing = FastOutSlowInEasing)
                                    )
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Gold,
                            contentColor = NavyDeep,
                            disabledContainerColor = Gold.copy(alpha = 0.55f),
                            disabledContentColor = NavyDeep.copy(alpha = 0.7f)
                        )
                    ) {
                        Text(
                            text = if (isLastPage) "Get Started" else "Next",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.width(10.dp))
                        Icon(
                            imageVector = if (isLastPage) {
                                Icons.AutoMirrored.Outlined.Login
                            } else {
                                Icons.AutoMirrored.Outlined.ArrowForward
                            },
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SkipButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.12f))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Skip",
            color = GoldLight,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.6.sp
        )
        Spacer(Modifier.width(7.dp))
        Icon(
            imageVector = Icons.Outlined.Close,
            contentDescription = null,
            tint = GoldLight,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun PageIndicator(pagerState: PagerState, pageCount: Int) {
    // Read in composition on purpose: only this small row re-layouts while swiping.
    val progress = pagerState.currentPage + pagerState.currentPageOffsetFraction

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(pageCount) { index ->
            val active = (1f - abs(index - progress)).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .height(8.dp)
                    .width(8.dp + 18.dp * active)
                    .clip(CircleShape)
                    .background(lerp(Color.White.copy(alpha = 0.30f), Gold, active))
            )
        }
    }
}

/**
 * Staggered entrance + scroll-linked cross-fade. All State is read inside the layer block,
 * so swiping only redraws layers instead of recomposing the slide.
 */
private fun Modifier.staggerIn(
    progress: Animatable<Float, AnimationVector1D>,
    pagerState: PagerState,
    pageIndex: Int,
    rise: Dp = 26.dp,
    drift: Dp = 10.dp,
    crossFade: Float = 1.7f
): Modifier = graphicsLayer {
    val pageOffset = (pageIndex - pagerState.currentPage) + pagerState.currentPageOffsetFraction
    val scrollFade = (1f - abs(pageOffset) * crossFade).coerceIn(0f, 1f)
    alpha = progress.value * scrollFade
    translationY = (1f - progress.value) * rise.toPx()
    translationX = pageOffset * drift.toPx()
}

@Composable
private fun OnboardingPageView(
    page: OnboardingPage,
    pagerState: PagerState,
    pageIndex: Int,
    modifier: Modifier = Modifier
) {
    val mediaProgress = remember { Animatable(0f) }
    val ruleProgress = remember { Animatable(0f) }
    val eyebrowProgress = remember { Animatable(0f) }
    val titleProgress = remember { Animatable(0f) }
    val descriptionProgress = remember { Animatable(0f) }
    val mediaZoom = remember { Animatable(1.14f) }

    LaunchedEffect(page) {
        launch {
            mediaZoom.animateTo(1f, tween(1600, easing = FastOutSlowInEasing))
        }
        launch {
            mediaProgress.animateTo(1f, tween(700, easing = FastOutSlowInEasing))
        }
        launch {
            ruleProgress.animateTo(1f, tween(560, 60, easing = FastOutSlowInEasing))
        }
        launch {
            eyebrowProgress.animateTo(1f, tween(540, 200, easing = FastOutSlowInEasing))
        }
        launch {
            titleProgress.animateTo(1f, tween(660, 320, easing = FastOutSlowInEasing))
        }
        launch {
            descriptionProgress.animateTo(1f, tween(660, 470, easing = FastOutSlowInEasing))
        }
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .graphicsLayer {
                        val pageOffset =
                            (pageIndex - pagerState.currentPage) + pagerState.currentPageOffsetFraction
                        val scroll = abs(pageOffset).coerceIn(0f, 1f)
                        alpha = mediaProgress.value * (1f - scroll * 0.45f)
                    }
            ) {
                when (val media = page.media) {
                    is OnboardingMedia.Photo -> {
                        Image(
                            painter = painterResource(media.res),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    val pageOffset =
                                        (pageIndex - pagerState.currentPage) + pagerState.currentPageOffsetFraction
                                    val scroll = abs(pageOffset).coerceIn(0f, 1f)
                                    // Always >= 1 so the parallax can never expose an edge.
                                    scaleX = mediaZoom.value * 1.12f
                                    scaleY = mediaZoom.value
                                    alpha = 1f - scroll * 0.30f
                                    translationX = -pageOffset * 22.dp.toPx()
                                }
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        0.55f to Color.Transparent,
                                        1f to Color.White
                                    )
                                )
                        )
                    }

                    OnboardingMedia.Features -> FeaturesPanel()
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, top = 22.dp, bottom = 26.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(44.dp)
                        .height(4.dp)
                        .graphicsLayer {
                            val pageOffset =
                                (pageIndex - pagerState.currentPage) + pagerState.currentPageOffsetFraction
                            val scrollFade = (1f - abs(pageOffset) * 1.7f).coerceIn(0f, 1f)
                            alpha = ruleProgress.value * scrollFade
                            scaleX = ruleProgress.value
                            transformOrigin = TransformOrigin(0f, 0.5f)
                            translationX = pageOffset * 6.dp.toPx()
                        }
                        .background(
                            Brush.horizontalGradient(listOf(Gold, GoldLight, Gold)),
                            RoundedCornerShape(50)
                        )
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    text = page.eyebrow,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    letterSpacing = 1.8.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.staggerIn(eyebrowProgress, pagerState, pageIndex, rise = 18.dp, drift = 8.dp)
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = page.title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary,
                    modifier = Modifier.staggerIn(titleProgress, pagerState, pageIndex, rise = 32.dp, drift = 14.dp)
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = page.description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextSecondary,
                    modifier = Modifier.staggerIn(
                        descriptionProgress,
                        pagerState,
                        pageIndex,
                        rise = 24.dp,
                        drift = 10.dp
                    )
                )
            }
        }
    }
}

@Composable
private fun FeaturesPanel() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(NavySoft, Navy, NavyDeep)))
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(170.dp)
                .offset(x = 60.dp, y = (-50).dp)
                .background(Gold.copy(alpha = 0.10f), CircleShape)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(22.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                FeatureCard(
                    icon = Icons.Outlined.Campaign,
                    label = "Notices",
                    modifier = Modifier.weight(1f)
                )
                FeatureCard(
                    icon = Icons.Outlined.CalendarMonth,
                    label = "Timetable",
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                FeatureCard(
                    icon = Icons.Outlined.HowToReg,
                    label = "Attendance",
                    modifier = Modifier.weight(1f)
                )
                FeatureCard(
                    icon = Icons.Outlined.Person,
                    label = "My Profile",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun FeatureCard(icon: ImageVector, label: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(Gold, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = NavyDeep,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = Navy,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

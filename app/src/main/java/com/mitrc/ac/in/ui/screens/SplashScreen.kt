package com.mitrc.ac.`in`.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mitrc.ac.`in`.R
import com.mitrc.ac.`in`.ui.theme.Gold
import com.mitrc.ac.`in`.ui.theme.GoldLight
import com.mitrc.ac.`in`.ui.theme.Navy
import com.mitrc.ac.`in`.ui.theme.NavyDeep
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashContent(onFinished: () -> Unit) {
    val logoAlpha = remember { Animatable(0f) }
    val logoScale = remember { Animatable(0.85f) }
    val ruleScale = remember { Animatable(0f) }
    val titleAlpha = remember { Animatable(0f) }
    val subtitleAlpha = remember { Animatable(0f) }
    val footerAlpha = remember { Animatable(0f) }
    val sweep = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch {
            logoAlpha.animateTo(1f, tween(650, easing = FastOutSlowInEasing))
        }
        launch {
            logoScale.animateTo(
                1f,
                spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
        launch {
            ruleScale.animateTo(1f, tween(700, delayMillis = 380, easing = FastOutSlowInEasing))
        }
        launch {
            titleAlpha.animateTo(1f, tween(550, delayMillis = 780, easing = FastOutSlowInEasing))
        }
        launch {
            subtitleAlpha.animateTo(1f, tween(550, delayMillis = 1020, easing = FastOutSlowInEasing))
        }
        launch {
            footerAlpha.animateTo(1f, tween(550, delayMillis = 1350, easing = FastOutSlowInEasing))
        }
        launch {
            sweep.animateTo(1f, tween(1600, delayMillis = 900, easing = LinearEasing))
        }
        delay(2600)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(NavyDeep, Navy, Color(0xFF003366))
                )
            )
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(300.dp)
                .offset(x = 110.dp, y = (-100).dp)
                .background(Gold.copy(alpha = 0.07f), CircleShape)
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .size(230.dp)
                .offset(x = (-85).dp, y = 70.dp)
                .background(Color.White.copy(alpha = 0.05f), CircleShape)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(R.drawable.logo_mitrc_white),
                contentDescription = "MITRC",
                modifier = Modifier
                    .width(230.dp)
                    .graphicsLayer {
                        alpha = logoAlpha.value
                        scaleX = logoScale.value
                        scaleY = logoScale.value
                    }
            )
            Spacer(Modifier.height(24.dp))
            Box(
                modifier = Modifier
                    .width(170.dp)
                    .height(3.dp)
                    .graphicsLayer {
                        scaleX = ruleScale.value
                        alpha = ruleScale.value
                    }
                    .background(
                        Brush.horizontalGradient(listOf(Gold, GoldLight, Gold)),
                        RoundedCornerShape(50)
                    )
            )
            Spacer(Modifier.height(22.dp))
            Text(
                text = "MODERN INSTITUTE OF TECHNOLOGY\n& RESEARCH CENTRE",
                modifier = Modifier.graphicsLayer { alpha = titleAlpha.value },
                color = Color.White.copy(alpha = 0.92f),
                fontSize = 13.sp,
                lineHeight = 21.sp,
                letterSpacing = 1.6.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = "EST. 2007",
                modifier = Modifier.graphicsLayer { alpha = subtitleAlpha.value },
                color = GoldLight,
                fontSize = 13.sp,
                letterSpacing = 3.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .width(160.dp)
                    .height(3.dp)
                    .background(Color.White.copy(alpha = 0.14f), RoundedCornerShape(50))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction = sweep.value)
                        .fillMaxHeight()
                        .background(Gold, RoundedCornerShape(50))
                )
            }
            Spacer(Modifier.height(14.dp))
            Text(
                text = "AICTE Approved  •  Affiliated to BTU Bikaner",
                modifier = Modifier.graphicsLayer { alpha = footerAlpha.value },
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "www.mitrc.ac.in",
                modifier = Modifier.graphicsLayer { alpha = footerAlpha.value },
                color = Gold.copy(alpha = 0.9f),
                fontSize = 11.sp,
                letterSpacing = 0.8.sp
            )
        }
    }
}

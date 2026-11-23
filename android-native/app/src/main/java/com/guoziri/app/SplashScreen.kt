package com.guoziri.app

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.guoziri.app.ui.theme.LocalAppColors
import kotlinx.coroutines.delay

/**
 * 启动页，对齐 web 版 #splash：
 * 图标（splashPulse 脉冲）+ 名称 26/700/ls .02em + 进度条 200x4 + 提示 13px。
 */
@Composable
fun SplashOverlay(onDone: () -> Unit) {
    val c = LocalAppColors.current
    var visible by remember { mutableStateOf(true) }
    var fade by remember { mutableFloatStateOf(1f) }
    var progress by remember { mutableFloatStateOf(0f) }

    // splashPulse：scale 1→1.12，opacity 1→.7
    val pulse = rememberInfiniteTransition(label = "pulse")
    val scale by pulse.animateFloat(
        initialValue = 1f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "scale",
    )
    val iconAlpha by pulse.animateFloat(
        initialValue = 1f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "alpha",
    )

    LaunchedEffect(Unit) {
        progress = 0.35f
        delay(180)
        progress = 0.75f
        delay(180)
        progress = 1f
        delay(260)
        fade = 0f
        delay(320)
        visible = false
        onDone()
    }

    if (visible) {
        Box(
            Modifier
                .fillMaxSize()
                .background(c.bg)
                .alpha(fade),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                AppIcons.Draw(
                    AppIcons.Leaf,
                    size = 40.dp,
                    contentDescription = null,
                    tint = c.green,
                    modifier = Modifier.size(56.dp).scale(scale).alpha(iconAlpha),
                )
                Text(
                    "过日子",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.52.sp,
                    color = c.label,
                    modifier = Modifier.padding(top = 10.dp),
                )
                // 进度条：200x4，r2
                Box(
                    Modifier
                        .padding(top = 16.dp)
                        .size(width = 200.dp, height = 4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(c.fill),
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(progress)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(2.dp))
                            .background(Brush.horizontalGradient(listOf(c.tint, androidx.compose.ui.graphics.Color(0xFF5AC8FA)))),
                    )
                }
                Text(
                    "正在准备…",
                    fontSize = 13.sp,
                    color = c.secondary,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

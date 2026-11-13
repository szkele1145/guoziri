package com.guoziri.app

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 全局引导遮罩层：聚光挖洞 + 提示卡片 + 点击拦截。
 *
 * 「添加记录」那一步在 ModalBottomSheet 内部展示（弹窗是独立窗口，
 * 全局层盖不住它），所以这一层要跳过 inSheet 的步骤。
 */
@Composable
fun GuideOverlay(
    step: Int,
    anchors: GuideAnchors,
    scope: String,
    onNext: () -> Unit,
    onSkip: () -> Unit,
) {
    val cur = GuideSteps.STEPS[step]

    // 目标矩形（锚点是 window 坐标，叠层同坐标系）
    val targetRect = cur.target?.let { anchors.rect(it, scope) }
    val ringAlpha by animateFloatAsState(
        if (targetRect != null) 1f else 0f, label = "ring"
    )

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.42f))
            // 拦截触摸，防止穿透到底层页面误操作
            .pointerInput(Unit) {
                detectTapGestures { /* 吞掉 */ }
            },
    ) {
        // ---- 聚光挖洞 ----
        if (targetRect != null && ringAlpha > 0f) {
            SpotlightOverlay(rect = targetRect, alpha = ringAlpha, modifier = Modifier.fillMaxSize())
        }

        // ---- 提示卡片 ----
        Column(
            Modifier.fillMaxSize().padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.Bottom,
        ) {
            Spacer(Modifier.weight(1f))
            val screenH = LocalConfiguration.current.screenHeightDp.dp
            val cardAbove = targetRect != null && targetRect.bottom > screenH.value * 0.55f
            if (cardAbove) {
                Box(Modifier.height(200.dp))
            }
            GuideCard(
                step = step,
                total = GuideSteps.size,
                title = cur.title,
                body = cur.body,
                onSkip = onSkip,
                onNext = onNext,
                isLast = step >= GuideSteps.size - 1,
            )
            Spacer(Modifier.height(28.dp))
        }
    }
}

/* ---------------- 聚光层：目标区域挖洞 + 高亮描边 ---------------- */
@Composable
fun SpotlightOverlay(
    rect: Rect,
    alpha: Float,
    modifier: Modifier = Modifier,
) {
    val border by animateColorAsState(
        if (alpha > 0.5f) IosBlue else Color.Transparent, label = "border"
    )
    val density = LocalDensity.current
    val padPx = with(density) { 8.dp.toPx() }

    Canvas(modifier) {
        val r = Rect(
            left = rect.left - padPx,
            top = rect.top - padPx,
            right = rect.right + padPx,
            bottom = rect.bottom + padPx,
        )
        val canvasW = this.size.width
        val canvasH = this.size.height
        val shade = Color.Black.copy(alpha = 0.30f * alpha)
        // 洞四周加深：上 / 下 / 左 / 右 四块
        drawRect(
            color = shade,
            topLeft = Offset(0f, 0f),
            size = androidx.compose.ui.geometry.Size(canvasW, r.top),
        )
        drawRect(
            color = shade,
            topLeft = Offset(0f, r.bottom),
            size = androidx.compose.ui.geometry.Size(canvasW, canvasH - r.bottom),
        )
        drawRect(
            color = shade,
            topLeft = Offset(0f, r.top),
            size = androidx.compose.ui.geometry.Size(r.left, r.height),
        )
        drawRect(
            color = shade,
            topLeft = Offset(r.right, r.top),
            size = androidx.compose.ui.geometry.Size(canvasW - r.right, r.height),
        )
        // 洞边描边
        drawRoundRect(
            color = border.copy(alpha = alpha),
            topLeft = Offset(r.left, r.top),
            size = androidx.compose.ui.geometry.Size(r.width, r.height),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(10.dp.toPx()),
            style = Stroke(width = 2.dp.toPx()),
        )
    }
}

/* ---------------- 提示卡片 ---------------- */
@Composable
fun GuideCard(
    step: Int,
    total: Int,
    title: String,
    body: String,
    onSkip: () -> Unit,
    onNext: () -> Unit,
    isLast: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 12.dp,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${step + 1} / $total · 沙箱模式",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    modifier = Modifier.weight(1f),
                )
                // 进度圆点
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    repeat(total) { i ->
                        Box(
                            Modifier
                                .size(if (i == step) 7.dp else 5.dp)
                                .clip(CircleShape)
                                .background(
                                    if (i <= step) IosBlue
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f)
                                )
                        )
                    }
                }
            }
            Text(
                title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
            )
            Text(
                body,
                fontSize = 15.sp,
                lineHeight = 24.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
            )
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onSkip) {
                    Text("跳过", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                }
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = onNext,
                    colors = ButtonDefaults.buttonColors(containerColor = IosBlue),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 26.dp, vertical = 10.dp),
                ) {
                    Text(if (isLast) "完成，退出引导" else "下一步", fontSize = 15.sp)
                }
            }
        }
    }
}

package com.guoziri.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.guoziri.app.ui.theme.LocalAppColors

/**
 * Agent 工作日志弹窗：进度条 + 实时滚动日志。
 *
 * 用于「智能填充食物营养」这类后台任务，让用户看到每一步在干什么：
 *  正在搜索 → 解析页面 → 提取营养表 → 填充完成
 */
@Composable
fun AgentLogSheet(
    visible: Boolean,
    title: String,
    progress: Int,
    logs: List<String>,
    onDismiss: () -> Unit,
    /** 任务是否已结束（结束时显示"完成"按钮） */
    finished: Boolean = false,
    dismissText: String = "关闭",
) {
    val c = LocalAppColors.current
    if (!visible) return

    androidx.compose.ui.window.Dialog(onDismissRequest = { if (finished) onDismiss() }) {
        androidx.compose.material3.Surface(
            shape = RoundedCornerShape(14.dp),
            color = c.bgElevated,
            modifier = Modifier.width(300.dp),
        ) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 18.dp)) {
                // 标题
                Text(
                    title,
                    fontSize = Dimens2.SheetTitle,
                    fontWeight = FontWeight.SemiBold,
                    color = c.label,
                )
                Spacer(Modifier.height(14.dp))

                // 进度条
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(c.fill),
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(progress / 100f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (finished) c.green else c.tint),
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "$progress%",
                    fontSize = Dimens2.Caption,
                    color = c.secondary,
                    style = NumberStyle,
                    modifier = Modifier.align(Alignment.End),
                )
                Spacer(Modifier.height(12.dp))

                // 日志区（自动滚到底部）
                val scroll = rememberScrollState()
                LaunchedEffect(logs.size) {
                    if (logs.isNotEmpty()) scroll.animateScrollTo(scroll.maxValue)
                }
                Column(
                    Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(c.bg)
                        .verticalScroll(scroll)
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                ) {
                    logs.forEach { line ->
                        Text(
                            line,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = if (line.startsWith("❌") || line.startsWith("⚠️")) c.orange
                                    else if (line.startsWith("✅")) c.green
                                    else c.secondary,
                            lineHeight = 15.sp,
                            modifier = Modifier.padding(bottom = 3.dp),
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))
                // 完成后允许关闭
                if (finished) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clickableNoIndication { onDismiss() }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(dismissText, fontSize = Dimens2.SheetTitle, color = c.tint)
                    }
                }
            }
        }
    }
}

/** 无涟漪点击 */
private fun Modifier.clickableNoIndication(onClick: () -> Unit): Modifier =
    this.then(
        Modifier.clickable(
            interactionSource = androidx.compose.foundation.interaction.MutableInteractionSource(),
            indication = null,
            onClick = onClick,
        )
    )

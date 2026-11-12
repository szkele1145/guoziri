package com.guoziri.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 原生引导：底部卡片（贴 tabbar 上方，居中，窄边距）+ 目标位置不遮挡。
 * 每步一张卡片，下一步推进；沙箱模式真实数据不动。
 */
@Composable
fun GuideOverlay(repo: StoreRepo, onDone: () -> Unit) {
    var step by remember { mutableIntStateOf(0) }

    data class G(val title: String, val body: String)

    val steps = listOf(
        G("欢迎使用 过日子 🥬", "记账 · 营养 · AI 助手 · 日常记录，\n一个 App 管好一家人的餐桌。\n\n引导是沙箱模式，不会动真实数据。"),
        G("第 1 步 · 记账", "底部「记账」页，点右下角 ＋ 记一笔。\n菜名有 98 种食材联想；分类弹出菜单；\n单价+总价 或 斤/克 填一组自动算重量。"),
        G("第 2 步 · 营养", "自动按《中国居民膳食指南 2022》评估全家达标率；\n能回看历史日期、看 7 天趋势；\n家里人数在页面底部可调。"),
        G("第 3 步 · AI 助手", "一键总结今天花销 / 营养，也能自由提问。\n先到设置页选服务商、填 API Key。"),
        G("第 4 步 · 日常记录", "设置页开启「显示日记入口」后使用；\n时间段（开始/结束）选填，不填按保存时刻算。"),
        G("完成！", "引导结束，开始使用吧！\n真实数据零风险。"),
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.18f),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Bottom,
        ) {
            Spacer(Modifier.weight(1f))
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(22.dp)) {
                    Text(
                        "${step + 1} / ${steps.size} · 沙箱模式",
                        fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    )
                    Text(steps[step].title, fontSize = 20.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp, bottom = 10.dp))
                    Text(steps[step].body, fontSize = 15.sp, lineHeight = 24.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                    Spacer(Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = onDone) { Text("跳过", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)) }
                        Spacer(Modifier.weight(1f))
                        Button(
                            onClick = {
                                if (step >= steps.size - 1) onDone() else step += 1
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = IosBlue),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 28.dp, vertical = 10.dp),
                        ) {
                            Text(if (step >= steps.size - 1) "完成，退出引导" else "下一步", fontSize = 15.sp)
                        }
                    }
                }
            }
        }
    }
}
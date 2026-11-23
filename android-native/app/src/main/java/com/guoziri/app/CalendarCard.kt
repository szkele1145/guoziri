package com.guoziri.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.guoziri.app.ui.theme.LocalAppColors
import java.util.Calendar
import java.util.Locale

/**
 * .cal：日历卡片（padding 12 16 8，7 列 gap 2，格 aspect=1、圆 50%，有记录日右下角橙点）。
 * 周一为第一列，月中不含的上/下月日期灰显。
 */
@Composable
fun CalendarCard(
    year: Int,
    month: Int,                 // 1-12
    selected: String,
    recorded: Set<String>,
    onPick: (String) -> Unit,
    onMonthChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = LocalAppColors.current
    val todayStr = today()

    val first = remember(year, month) {
        Calendar.getInstance().apply {
            clear()
            set(year, month - 1, 1)
        }
    }
    val offset = remember(year, month) { (first.get(Calendar.DAY_OF_WEEK) + 5) % 7 }  // 周一开头
    val daysInMonth = remember(year, month) { first.getActualMaximum(Calendar.DAY_OF_MONTH) }
    val prevDays = remember(year, month) {
        Calendar.getInstance().apply {
            clear()
            set(year, month - 1, 1)
            add(Calendar.DAY_OF_MONTH, -1)
        }.get(Calendar.DAY_OF_MONTH)
    }

    GroupCard(modifier) {
        Column(Modifier.fillMaxWidth().padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 8.dp)) {
            // .cal-head：‹ 标题 ›
            Row(
                Modifier.fillMaxWidth().padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                CalNavButton(AppIcons.ChevronLeft) { onMonthChange(-1) }
                Text(
                    "$year 年 $month 月",
                    fontSize = Dimens2.CalTitle,
                    fontWeight = FontWeight.SemiBold,
                    color = c.label,
                )
                CalNavButton(AppIcons.ChevronRight) { onMonthChange(1) }
            }

            // 星期表头
            Row(Modifier.fillMaxWidth()) {
                listOf("一", "二", "三", "四", "五", "六", "日").forEach { wd ->
                    Text(
                        wd,
                        fontSize = Dimens2.CalWd,
                        color = c.tertiary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f).padding(vertical = 4.dp),
                    )
                }
            }

            // 日期格：gap 2，格宽 = (可用宽 - 6*2) / 7
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val gap = 2.dp
                val cell = (maxWidth - gap * 6) / 7
                val lead = offset
                val total = lead + daysInMonth
                val rows = (total + 6) / 7

                Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                    for (r in 0 until rows) {
                        Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                            for (col in 0 until 7) {
                                val idx = r * 7 + col
                                val dayNum = idx - lead + 1
                                if (dayNum < 1) {
                                    // 上月末
                                    Box(Modifier.size(cell), contentAlignment = Alignment.Center) {
                                        Text(
                                            "${prevDays + dayNum}",
                                            fontSize = Dimens2.CalDay,
                                            color = c.tertiary,
                                        )
                                    }
                                } else if (dayNum > daysInMonth) {
                                    // 下月初
                                    Box(Modifier.size(cell), contentAlignment = Alignment.Center) {
                                        Text(
                                            "${dayNum - daysInMonth}",
                                            fontSize = Dimens2.CalDay,
                                            color = c.tertiary,
                                        )
                                    }
                                } else {
                                    val ds = String.format(Locale.US, "%04d-%02d-%02d", year, month, dayNum)
                                    val isToday = ds == todayStr
                                    val isSel = ds == selected
                                    val hasRec = recorded.contains(ds)
                                    Box(
                                        Modifier
                                            .size(cell)
                                            .clip(CircleShape)
                                            .then(
                                                if (isSel) Modifier.background(c.tint) else Modifier
                                            )
                                            .clickable { onPick(ds) },
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            "$dayNum",
                                            fontSize = Dimens2.CalDay,
                                            fontWeight = when {
                                                isSel -> FontWeight.SemiBold
                                                isToday -> FontWeight.Bold
                                                else -> FontWeight.Normal
                                            },
                                            color = when {
                                                isSel -> Color.White
                                                isToday -> c.tint
                                                else -> c.label
                                            },
                                        )
                                        // 有记录的日子：底部 4px 橙点
                                        if (hasRec) {
                                            Box(
                                                Modifier
                                                    .align(Alignment.BottomCenter)
                                                    .padding(bottom = 4.dp)
                                                    .size(4.dp)
                                                    .clip(CircleShape)
                                                    .background(if (isSel) Color.White else c.orange),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // .hint：橙点 + 说明
            Row(
                Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(5.dp).clip(CircleShape).background(c.orange))
                Text(
                    "  有记录的日子 · 点日期看当天",
                    fontSize = Dimens2.Hint,
                    color = c.tertiary,
                )
            }
        }
    }
}

@Composable
private fun CalNavButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    val c = LocalAppColors.current
    Box(
        Modifier.size(34.dp).clip(CircleShape).clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        AppIcons.Draw(icon,, size = 22.dp, contentDescription = null, tint = c.tint, modifier = Modifier.size(22.dp))
    }
}

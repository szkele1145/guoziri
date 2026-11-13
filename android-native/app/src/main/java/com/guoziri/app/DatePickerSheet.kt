package com.guoziri.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * 日期选择器：日历弹窗，可回看任意有记录的日期。
 * 底部列出「有记录的日期」快捷入口，避免用户在小月里翻找。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerSheet(
    initial: String,
    recordedDates: List<String> = emptyList(),
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val cal = remember { Calendar.getInstance().apply { time = parseDate(initial) } }
    var year by remember { mutableIntStateOf(cal.get(Calendar.YEAR)) }
    var month by remember { mutableIntStateOf(cal.get(Calendar.MONTH)) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = {
                    val c = Calendar.getInstance().apply {
                        clear()
                        set(year, month - 1, 1)
                        add(Calendar.MONTH, -1)
                    }
                    year = c.get(Calendar.YEAR); month = c.get(Calendar.MONTH)
                }) { Text("‹", fontSize = 22.sp) }
                Text("$year 年 ${month + 1} 月", fontSize = 17.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                TextButton(onClick = {
                    val c = Calendar.getInstance().apply {
                        clear()
                        set(year, month - 1, 1)
                        add(Calendar.MONTH, 1)
                    }
                    year = c.get(Calendar.YEAR); month = c.get(Calendar.MONTH)
                }) { Text("›", fontSize = 22.sp) }
            }

            // 星期表头
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
                listOf("一", "二", "三", "四", "五", "六", "日").forEach { d ->
                    Text(d, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                        textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(4.dp))

            val first = Calendar.getInstance().apply { clear(); set(year, month - 1, 1) }
            val lead = (first.get(Calendar.DAY_OF_WEEK) + 5) % 7   // 周一为第一列
            val daysInMonth = Calendar.getInstance().apply {
                clear(); set(year, month - 1, 1); add(Calendar.MONTH, 1); add(Calendar.DAY_OF_MONTH, -1)
            }.get(Calendar.DAY_OF_MONTH)

            val cells = remember(year, month) {
                val list = mutableListOf<String?>()
                repeat(lead) { list.add(null) }
                for (d in 1..daysInMonth) list.add(
                    SimpleDateFormat("yyyy-MM-dd", Locale.US).format(
                        Calendar.getInstance().apply { clear(); set(year, month - 1, d) }.time
                    )
                )
                while (list.size % 7 != 0) list.add(null)
                list
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(7),
                modifier = Modifier.fillMaxWidth().height(260.dp).padding(horizontal = 12.dp),
            ) {
                items(cells) { ds ->
                    if (ds == null) {
                        Box(Modifier.size(40.dp))
                    } else {
                        val day = ds.substring(8).toInt()
                        val has = recordedDates.contains(ds)
                        val isToday = ds == today()
                        val isSel = ds == initial
                        Box(
                            Modifier.size(40.dp).padding(2.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isSel -> IosBlue
                                        has -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                        else -> androidx.compose.ui.graphics.Color.Transparent
                                    }
                                )
                                .clickable { onPick(ds) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "$day",
                                fontSize = 14.sp,
                                fontWeight = if (isSel || isToday) FontWeight.Bold else FontWeight.Normal,
                                color = when {
                                    isSel -> androidx.compose.ui.graphics.Color.White
                                    isToday -> IosBlue
                                    else -> MaterialTheme.colorScheme.onSurface
                                },
                            )
                        }
                    }
                }
            }

            if (recordedDates.isNotEmpty()) {
                Text("有记录的日期",
                    fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 6.dp))
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    recordedDates.sortedDescending().take(6).forEach { ds ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp, MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier.clickable { onPick(ds) },
                        ) {
                            Text(
                                if (ds == today()) "今天" else fmtDay(ds),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

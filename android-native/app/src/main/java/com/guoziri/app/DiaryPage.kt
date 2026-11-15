package com.guoziri.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.guoziri.app.ui.theme.LocalAppColors
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * 日记页，结构对齐 web 版 renderDiary。
 */
@Composable
fun DiaryPage(repo: StoreRepo, snackbar: (String) -> Unit) {
    val c = LocalAppColors.current
    val t = today()

    var content by remember { mutableStateOf("") }
    var dateInput by remember { mutableStateOf(t) }
    var startInput by remember { mutableStateOf("") }
    var endInput by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = Dimens.TabH + 24.dp),
    ) {
        PageTitle("日常记录", subtitle = "时间段选填，不填就按保存时刻算")

        GroupCard {
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // 日期（fill 底 r8 p7 10 15px）
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(c.fill)
                        .clickable { showDatePicker = true }
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                ) {
                    Text(
                        dateInput,
                        fontSize = 15.sp,
                        color = c.label,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                // 开始 / 结束
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("开始", fontSize = Dimens2.Caption, color = c.secondary, maxLines = 1)
                    TimeField(startInput, Modifier.weight(1f)) { startInput = it }
                    Text("结束", fontSize = Dimens2.Caption, color = c.secondary, maxLines = 1)
                    TimeField(endInput, Modifier.weight(1f)) { endInput = it }
                }

                // 内容 textarea
                Box(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 76.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(c.fill)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                ) {
                    if (content.isEmpty()) {
                        Text("今天做了什么…", fontSize = 16.sp, color = c.tertiary)
                    }
                    BasicTextField(
                        value = content,
                        onValueChange = { content = it },
                        textStyle = TextStyle(fontSize = 16.sp, color = c.label, lineHeight = 24.sp),
                        cursorBrush = SolidColor(c.tint),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                // 保存
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(c.tint)
                        .clickable {
                            if (content.isBlank()) { snackbar("写点什么再保存"); return@clickable }
                            if (startInput.isNotBlank() && endInput.isNotBlank() && endInput < startInput) {
                                snackbar("结束时间早于开始时间"); return@clickable
                            }
                            repo.addDiary(
                                DiaryEntry(
                                    id = repo.newId(),
                                    content = content.trim(),
                                    date = dateInput.ifBlank { today() },
                                    timeStart = startInput.ifBlank { null },
                                    timeEnd = endInput.ifBlank { null },
                                    time = startInput.ifBlank {
                                        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                                    },
                                    createdAt = repo.nowIso(),
                                ),
                            )
                            content = ""
                            startInput = ""
                            endInput = ""
                            snackbar("已保存")
                        },
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("保存", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Color.White)
                }
            }
        }

        if (repo.diary.isEmpty()) {
            EmptyState(AppIcons.Book, "还没有日常记录")
        } else {
            val groups = repo.diary.groupBy { it.date }
            groups.keys.sortedDescending().forEach { date ->
                GroupTitle(
                    text = (if (date == t) "今天 · " else "") + fmtDay(date),
                    topSpacing = 20.dp,
                )
                GroupCard {
                    val list = groups[date].orEmpty()
                    list.forEachIndexed { i, d ->
                        if (i > 0) CardDivider()
                        val timeStr = when {
                            !d.timeStart.isNullOrBlank() && !d.timeEnd.isNullOrBlank() -> d.timeStart + " – " + d.timeEnd
                            !d.timeStart.isNullOrBlank() -> d.timeStart + " 开始"
                            else -> d.time ?: ""
                        }
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = Dimens.CardPad, vertical = Dimens.CardPadV),
                            verticalAlignment = Alignment.Top,
                        ) {
                            Box(
                                Modifier
                                    .size(Dimens.IconTile)
                                    .background(Color(0xFFF3E8FF), RoundedCornerShape(Dimens.IconR)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    AppIcons.Clock,
                                    contentDescription = null,
                                    tint = Color(0xFF9C27B0),
                                    modifier = Modifier.size(17.dp),
                                )
                            }
                            Column(Modifier.weight(1f).padding(start = Dimens.CellGap)) {
                                if (timeStr.isNotBlank()) {
                                    Text(
                                        timeStr,
                                        fontSize = Dimens2.Caption,
                                        color = c.secondary,
                                        modifier = Modifier.padding(bottom = 2.dp),
                                    )
                                }
                                Text(
                                    d.content,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = c.label,
                                    lineHeight = 23.sp,
                                )
                            }
                            Text(
                                "删除",
                                fontSize = Dimens2.Action,
                                color = c.red,
                                modifier = Modifier
                                    .clickable { repo.deleteDiary(d.id) }
                                    .padding(start = 10.dp, top = 6.dp, bottom = 6.dp, end = 2.dp),
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        Dialog(onDismissRequest = { showDatePicker = false }) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = c.bg,
                modifier = Modifier.fillMaxWidth(),
            ) {
                val cal = remember { Calendar.getInstance() }
                var y by remember { mutableIntStateOf(cal.get(Calendar.YEAR)) }
                var m by remember { mutableIntStateOf(cal.get(Calendar.MONTH) + 1) }
                CalendarCard(
                    year = y,
                    month = m,
                    selected = dateInput,
                    recorded = repo.diary.map { it.date }.toSet(),
                    onPick = { dateInput = it; showDatePicker = false },
                    onMonthChange = { delta ->
                        var mm = m + delta
                        var yy = y
                        if (mm < 1) { mm = 12; yy -= 1 }
                        if (mm > 12) { mm = 1; yy += 1 }
                        m = mm; y = yy
                    },
                )
            }
        }
    }
}

/** fill 底、居中、15sp 的时间输入 */
@Composable
private fun TimeField(value: String, modifier: Modifier = Modifier, onChange: (String) -> Unit) {
    val c = LocalAppColors.current
    Box(
        modifier
            .height(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(c.fill)
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (value.isEmpty()) {
            Text("--:--", fontSize = 15.sp, color = c.tertiary, textAlign = TextAlign.Center)
        }
        BasicTextField(
            value = value,
            onValueChange = { v ->
                // 只留数字和冒号，最多 5 位（HH:mm）
                onChange(v.filter { it.isDigit() || it == ':' }.take(5))
            },
            singleLine = true,
            textStyle = TextStyle(fontSize = 15.sp, color = c.label, textAlign = TextAlign.Center),
            cursorBrush = SolidColor(c.tint),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

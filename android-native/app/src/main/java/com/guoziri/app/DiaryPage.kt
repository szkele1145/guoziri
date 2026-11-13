package com.guoziri.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DiaryPage(repo: StoreRepo, snackbar: (String) -> Unit) {
    val t = today()
    var content by remember { mutableStateOf("") }
    var dateInput by remember { mutableStateOf(t) }
    var timeStart by remember { mutableStateOf("") }
    var timeEnd by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize()) {
        PageTitle("日常记录", subtitle = "时间段选填，不填就按保存时刻算")

        GroupCard(Modifier.padding(horizontal = Dimens.PageH)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = Dimens.CardPad, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = dateInput,
                    onValueChange = { dateInput = it },
                    label = { Text("日期", fontSize = 11.sp) },
                    modifier = Modifier.weight(1.1f),
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 14.sp),
                    shape = RoundedCornerShape(9.dp),
                )
                OutlinedTextField(
                    value = timeStart,
                    onValueChange = { timeStart = it },
                    label = { Text("开始", fontSize = 11.sp) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 14.sp),
                    shape = RoundedCornerShape(9.dp),
                )
                OutlinedTextField(
                    value = timeEnd,
                    onValueChange = { timeEnd = it },
                    label = { Text("结束", fontSize = 11.sp) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 14.sp),
                    shape = RoundedCornerShape(9.dp),
                )
            }
            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = Dimens.CardPad),
                placeholder = { Text("今天做了什么…", fontSize = 14.sp) },
                minLines = 3,
                textStyle = TextStyle(fontSize = 15.sp, lineHeight = 21.sp),
                shape = RoundedCornerShape(10.dp),
            )
            Button(
                onClick = {
                    if (content.isBlank()) { snackbar("写点什么再保存"); return@Button }
                    if (timeStart.isNotBlank() && timeEnd.isNotBlank() && timeEnd < timeStart) {
                        snackbar("结束时间早于开始时间"); return@Button
                    }
                    repo.addDiary(
                        DiaryEntry(
                            id = repo.newId(),
                            content = content.trim(),
                            date = dateInput.ifBlank { today() },
                            timeStart = timeStart.ifBlank { null },
                            timeEnd = timeEnd.ifBlank { null },
                            time = timeStart.ifBlank {
                                SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                            },
                            createdAt = repo.nowIso(),
                        ),
                    )
                    content = ""
                    timeStart = ""
                    timeEnd = ""
                    snackbar("已保存")
                },
                modifier = Modifier.fillMaxWidth().padding(Dimens.CardPad).height(44.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = IosBlue),
            ) {
                Text("保存", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        if (repo.diary.isEmpty()) {
            EmptyState(
                Icons.Rounded.Menu,
                "还没有日常记录",
                hint = "写下今天的一件小事",
            )
        } else {
            val groups = repo.diary.groupBy { it.date }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 110.dp),
            ) {
                for (date in groups.keys.sortedDescending()) {
                    val entries = groups[date]!!
                    item(key = "dh$date") {
                        GroupTitle(if (date == today()) "今天 · ${fmtDay(date)}" else fmtDay(date))
                    }
                    item(key = "dl$date") {
                        GroupCard(Modifier.padding(horizontal = Dimens.PageH)) {
                            entries.forEachIndexed { i, d ->
                                if (i > 0) CardDivider()
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = Dimens.CardPad, vertical = 12.dp),
                                    verticalAlignment = Alignment.Top,
                                ) {
                                    Icon(
                                        Icons.Rounded.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.32f),
                                        modifier = Modifier.size(15.dp).padding(top = 2.dp),
                                    )
                                    Column(Modifier.weight(1f).padding(start = 10.dp)) {
                                        val timeStr = d.timeStart?.let { s -> "$s – ${d.timeEnd ?: ""}" } ?: d.time ?: ""
                                        if (timeStr.isNotBlank()) {
                                            Text(
                                                timeStr,
                                                fontSize = Dimens2.Micro,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                            )
                                        }
                                        Text(
                                            d.content,
                                            fontSize = Dimens2.Body,
                                            lineHeight = 21.sp,
                                            modifier = Modifier.padding(top = if (timeStr.isBlank()) 0.dp else 3.dp),
                                        )
                                    }
                                    IconButton(
                                        onClick = { repo.deleteDiary(d.id) },
                                        modifier = Modifier.size(34.dp),
                                    ) {
                                        Icon(
                                            Icons.Rounded.Delete,
                                            contentDescription = "删除",
                                            tint = IosRed.copy(alpha = 0.75f),
                                            modifier = Modifier.size(17.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

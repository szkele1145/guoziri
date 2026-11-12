package com.guoziri.app

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DiaryPage(repo: StoreRepo, snackbar: (String) -> Unit) {
    val t = today()
    var content by remember { mutableStateOf("") }
    var dateInput by remember { mutableStateOf(t) }
    var timeStart by remember { mutableStateOf("") }
    var timeEnd by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize()) {
        Text("日常记录", fontSize = 34.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 20.dp, top = 12.dp, bottom = 4.dp))
        Text("时间段选填，不填就按保存时刻算", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), modifier = Modifier.padding(start = 20.dp))

        GroupCard {
            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = dateInput, onValueChange = { dateInput = it },
                        label = { Text("日期", fontSize = 13.sp) },
                        modifier = Modifier.weight(1f).padding(vertical = 8.dp),
                        singleLine = true, shape = RoundedCornerShape(8.dp),
                    )
                    OutlinedTextField(
                        value = timeStart, onValueChange = { timeStart = it },
                        modifier = Modifier.weight(1f).padding(start = 8.dp),
                        label = { Text("开始", fontSize = 13.sp) },
                        singleLine = true, shape = RoundedCornerShape(8.dp),
                    )
                    OutlinedTextField(
                        value = timeEnd, onValueChange = { timeEnd = it },
                        modifier = Modifier.weight(1f).padding(start = 8.dp),
                        label = { Text("结束", fontSize = 13.sp) },
                        singleLine = true, shape = RoundedCornerShape(8.dp),
                    )
                }
                OutlinedTextField(
                    value = content, onValueChange = { content = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("今天做了什么…", fontSize = 15.sp) },
                    minLines = 3, shape = RoundedCornerShape(10.dp),
                )
                Button(onClick = {
                    if (content.isBlank()) { snackbar("写点什么再保存"); return@Button }
                    if (timeStart.isNotBlank() && timeEnd.isNotBlank() && timeEnd < timeStart) {
                        snackbar("结束时间早于开始时间"); return@Button
                    }
                    val rec = DiaryEntry(
                        id = repo.newId(),
                        content = content.trim(),
                        date = dateInput.ifBlank { today() },
                        timeStart = timeStart.ifBlank { null },
                        timeEnd = timeEnd.ifBlank { null },
                        time = timeStart.ifBlank { java.text.SimpleDateFormat("HH:mm", Locale.getDefault()).format(java.util.Date()) },
                        createdAt = repo.nowIso(),
                    )
                    repo.addDiary(rec)
                    content = ""
                    timeStart = ""
                    timeEnd = ""
                    snackbar("已保存")
                    // 刷新会在父组件自动触发
                },
                modifier = Modifier.fillMaxWidth().height(44.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = IosBlue),
            ) { Text("保存", fontSize = 15.sp, fontWeight = FontWeight.SemiBold) }
        }
    }

    if (repo.diary.isEmpty()) {
        EmptyState("📖", "还没有日常记录")
    } else {
        val groups = repo.diary.groupBy { it.date }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 110.dp),
        ) {
            for (date in groups.keys.sortedDescending()) {
                item(key = "dh$date") {
                    Text(date == today() ? "今天 · ${fmtDay(date)}" : fmtDay(date), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 6.dp))
                    GroupCard {
                        entries.forEach { d ->
                            val timeStr = d.timeStart?.let { s -> "$s – ${d.timeEnd ?: ""}" } ?: d.time ?: ""
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.Top,
                            ) {
                                Text("🕒", fontSize = 15.sp)
                                Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                                    Text(timeStr, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                    Text(d.content, fontSize = 15.sp, lineHeight = 1.55.sp)
                                }
                                TextButton(onClick = { repo.deleteDiary(d.id) }) {
                                    Text("删除", fontSize = 14.sp, color = IosRed)
                                }
                            }
                            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                }
            }
        }
    }
}
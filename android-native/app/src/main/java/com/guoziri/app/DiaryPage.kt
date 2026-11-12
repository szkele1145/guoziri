package com.guoziri.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date

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

        GroupCard(Modifier.padding(top = 10.dp)) {
            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = dateInput, onValueChange = { dateInput = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("日期 (yyyy-MM-dd)", fontSize = 13.sp) },
                    singleLine = true, shape = RoundedCornerShape(10.dp),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("开始", fontSize = 14.sp, modifier = Modifier.align(Alignment.CenterVertically))
                    OutlinedTextField(
                        value = timeStart, onValueChange = { timeStart = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("开始 (HH:MM 选填)", fontSize = 13.sp) },
                        singleLine = true, shape = RoundedCornerShape(10.dp),
                    )
                    Text("结束", fontSize = 14.sp, modifier = Modifier.align(Alignment.CenterVertically))
                    OutlinedTextField(
                        value = timeEnd, onValueChange = { timeEnd = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("结束 (选填)", fontSize = 13.sp) },
                        singleLine = true, shape = RoundedCornerShape(10.dp),
                    )
                }
                OutlinedTextField(
                    value = content, onValueChange = { content = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("今天做了什么…", fontSize = 15.sp) },
                    minLines = 3, shape = RoundedCornerShape(10.dp),
                )
                Button(
                    onClick = {
                        if (content.isBlank()) { snackbar("写点什么再保存"); return@Button }
                        if (timeStart.isNotBlank() && timeEnd.isNotBlank() && timeEnd < timeStart) {
                            snackbar("结束时间早于开始时间"); return@Button
                        }
                        repo.addDiary(DiaryEntry(
                            id = repo.newId(), content = content.trim(),
                            date = dateInput.ifBlank { t },
                            timeStart = timeStart.ifBlank { null },
                            timeEnd = timeEnd.ifBlank { null },
                            time = timeStart.ifBlank { SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()) },
                            createdAt = repo.nowIso(),
                        ))
                        content = ""; timeStart = ""; timeEnd = ""
                        snackbar("已保存")
                    },
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = IosBlue),
                ) { Text("保存", fontSize = 15.sp) }
            }
        }

        if (repo.diary.isEmpty()) {
            EmptyState("📖", "还没有日常记录")
        } else {
            val groups = repo.diary.groupBy { it.date }
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 110.dp)) {
                for ((date, entries) in groups) {
                    item(key = "dh$date") {
                        Text((if (date == t) "今天 · " else "") + fmtDay(date),
                            fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 6.dp))
                        GroupCard {
                            entries.forEach { d ->
                                val ts = d.timeStart; val te = d.timeEnd
                                val timeStr = if (!ts.isNullOrBlank() && !te.isNullOrBlank()) "$ts – $te"
                                              else ts ?: d.time ?: ""
                                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
                                    Text("🕒", fontSize = 15.sp)
                                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                                        Text(timeStr, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                        Text(d.content, fontSize = 15.sp, lineHeight = 24.sp)
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
}

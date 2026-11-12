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
fun SettingsPage(repo: StoreRepo, snackbar: (String) -> Unit, onReplayGuide: () -> Unit) {
    val s = repo.settings
    val context = LocalContext.current
    var keyInput by remember { mutableStateOf("") }
    var modelInput by remember { mutableStateOf(s.apiModel.ifBlank { "deepseek-chat" }) }
    var showThemePicker by remember { mutableStateOf(false) }
    var showClear by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 110.dp)) {
        Text("设置", fontSize = 34.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 20.dp, top = 12.dp, bottom = 6.dp))

        GroupTitle("AI 服务商")
        GroupCard(id = "preset-group") {
            PRESETS.forEach { preset ->
                PresetCell(
                    preset = preset,
                    isSelected = s.apiProvider == preset.id,
                    onClick = {
                        repo.updateSettings { cur ->
                            cur.copy(
                                apiProvider = it.id,
                                apiBaseUrl = preset.baseUrl.ifBlank { cur.apiBaseUrl },
                                apiModel = preset.model.ifBlank { cur.apiModel },
                            )
                        }
                        toast("已切换")
                    }
                }
            }
        }
        Text("切换服务商自动带出地址和默认模型，Key 各自独立保存", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), modifier = Modifier.padding(horizontal = 20.dp, top = 8.dp))

        GroupTitle("当前服务配置${curPreset?.let { " · ${it.label}" } ?: ""}")
        GroupCard {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("API 状态", fontSize = 16.sp)
                    Text(s.hasKey.then { "● 已配置" } ?: "○ 未配置", fontSize = 13.sp, color = if (s.hasKey) IosGreen else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                }
                Text(s.apiBaseUrl.ifBlank { "未设置" }, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            OutlinedTextField(
                value = keyInput,
                onValueChange = { keyInput = it },
                label = { Text("API Key") },
                placeholder = { Text(s.hasKey.then { "已保存（输入可覆盖）" } ?: "sk-…") },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                singleLine = true,
                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                shape = RoundedCornerShape(10.dp),
            )
            OutlinedTextField(
                value = modelInput,
                onValueChange = { modelInput = it },
                label = { Text("模型") },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
            )
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.End) {
                Button(onClick = {
                    val key = keyInput.trim()
                    val model = modelInput.trim()
                    val patch = mutableMapOf<String, Any>()
                    if (key.isNotBlank()) patch["apiKey"] = key
                    if (model.isNotBlank()) patch["apiModel"] = model
                    repo.updateSettings(patch)
                    keyInput = ""
                    toast("已保存")
                }, modifier = Modifier.padding(vertical = 8.dp)) {
                    Text("保存", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        Text("Key 只存在本机数据目录，不上传、不备份、不写日志",
            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))

        GroupTitle("通用")
        GroupCard {
            SwitchRow("记录排序", "「全部」列表的顺序", s.sortOrder == "desc") { checked ->
                repo.updateSettings { it.copy(sortOrder = if (checked) "desc" else "asc") }
            }
            SwitchRow("显示日记入口", "在底部导航栏加入「日记」", s.hiddenEnabled) { checked ->
                repo.updateSettings { it.copy(hiddenEnabled = checked) }
            }
        }

        GroupTitle("外观")
        GroupCard {
            val themeLabel = when (s.theme) {
                "light" -> "☀️ 浅色"
                "dark" -> "🌙 深色"
                else -> "🌤 自动"
            }
            SwitchRow("外观", themeLabel, s.theme != "auto") { checked ->
                repo.updateSettings { it.copy(theme = if (checked) "dark" else "light") }
            }
        }

        GroupTitle("数据")
        GroupCard {
            ExportCell(onClick = exportData)
            ClearCell(onClick = { showClear = true })
        }

        Text("过日子 v1.0-native · 数据保存在本机",
            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp), textAlign = TextAlign.Center
        )
    }

    if (showThemePicker) {
        AlertDialog(
            onDismissRequest = { showThemePicker = false },
            confirmButton = {},
            title = { Text("外观") },
            text = {
                Column {
                    listOf("auto" to "🌤 自动（跟随系统）", "light" to "☀️ 浅色", "dark" to "🌙 深色").forEach { (v, label) ->
                        Text(
                            label,
                            fontSize = 16.sp,
                            color = if (s.theme == v) IosBlue else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    repo.updateSettings { it.copy(theme = v) }
                                    ThemePrefs.theme.value = v
                                    showThemePicker = false
                                }
                                .padding(vertical = 12.dp)
                        }
                    }
                }
            },
        )
    }

    if (showClear) {
        AlertDialog(
            onDismissRequest = { showClear = false },
            title = { Text("清空全部数据？") },
            text = { Text("买菜记录和日记都会删除，无法恢复。") },
            confirmButton = { TextButton(onClick = { repo.clearAll(); showClear = false; toast("已清空") }) { Text("清空", color = IosRed) } },
            dismissButton = { TextButton(onClick = { showClear = false }) { Text("取消") } },
        )
    }
}

private fun SwitchRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp)
            Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

fun ExportCell(onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("导出备份", fontSize = 16.sp, color = IosBlue, modifier = Modifier.weight(1f))
        Icon(Icons.Default.ArrowForward, contentDescription = "导出", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
    }
}

fun ClearCell(onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End,
    ) {
        Text("清空全部数据", fontSize = 16.sp, color = IosRed)
        Icon(Icons.Default.ArrowForward, contentDescription = "清空", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
    }
}

fun SwitchRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp)
            Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

fun ExportCell(onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("导出备份", fontSize = 16.sp, color = IosBlue, modifier = Modifier.weight(1f))
        Icon(Icons.Default.ArrowForward, contentDescription = "导出", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
    }
}

fun ClearCell(onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End,
    ) {
        Text("清空全部数据", fontSize = 16.sp, color = IosRed)
        Icon(Icons.Default.ArrowForward, contentDescription = "清空", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
    }
}

fun SwitchRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp)
            Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

fun ExportCell(onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("导出备份", fontSize = 16.sp, color = IosBlue, modifier = Modifier.weight(1f))
        Icon(Icons.Default.ArrowForward, contentDescription = "导出", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
    }
}

fun ClearCell(onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End,
    ) {
        Text("清空全部数据", fontSize = 16.sp, color = IosRed)
        Icon(Icons.Default.ArrowForward, contentDescription = "清空", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
    }
}

fun SwitchRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp)
            Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

fun ExportCell(onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("导出备份", fontSize = 16.sp, color = IosBlue, modifier = Modifier.weight(1f))
        Icon(Icons.Default.ArrowForward, contentDescription = "导出", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
    }
}

fun ClearCell(onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End,
    ) {
        Text("清空全部数据", fontSize = 16.sp, color = IosRed)
        Icon(Icons.Default.ArrowForward, contentDescription = "清空", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
    }
}

fun SwitchRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp)
            Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

fun exportData() {
    val data = mapOf(
        "purchases" to repo.purchases,
        "diary" to repo.diary,
        "family" to repo.settings.family,
        "settings" to mapOf(
            "sortOrder" to repo.settings.sortOrder,
            "family" to repo.settings.family,
            "hiddenEnabled" to repo.settings.hiddenEnabled,
            "theme" to repo.settings.theme,
            "apiProvider" to repo.settings.apiProvider,
            "apiBaseUrl" to repo.settings.apiBaseUrl,
            "apiModel" to repo.settings.apiModel,
        ),
        "exportedAt" to java.time.Instant.now().toString(),
    )
    val blob = Blob(gson.toJson(mapOf(
        "purchases" to repo.purchases,
        "diary" to repo.diary,
        "family" to repo.settings.family,
        "settings" to mapOf(
            "sortOrder" to repo.settings.sortOrder,
            "family" to repo.settings.family,
            "hiddenEnabled" to repo.settings.hiddenEnabled,
            "theme" to repo.settings.theme,
            "apiProvider" to repo.settings.apiProvider,
            "apiBaseUrl" to repo.settings.apiBaseUrl,
            "apiModel" to repo.settings.apiModel,
        ), null, 2))
    val url = URL.createObjectURL(blob)
    val a = document.createElement("a")
    a.href = url
    a.download = "guoziri-backup-${today()}.json"
    a.click()
    URL.revokeObjectURL(url)
    toast("已导出")
}

fun confirmClear() {
    AlertDialog(
        onDismissRequest = { showClear = false },
        title = { Text("清空全部数据？") },
        text = { Text("买菜记录和日记都会删除，无法恢复。") },
        confirmButton = { TextButton(onClick = { repo.clearAll(); showClear = false; toast("已清空"); renderPage("settings") }) { Text("清空", color = IosRed) } },
        dismissButton = { TextButton(onClick = { showClear = false }) { Text("取消") } },
    )
}

private fun SwitchRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp)
            Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

fun ExportCell(onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("导出备份", fontSize = 16.sp, color = IosBlue, modifier = Modifier.weight(1f))
        Icon(Icons.Default.ArrowForward, contentDescription = "导出", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
    }
}

fun ClearCell(onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End,
    ) {
        Text("清空全部数据", fontSize = 16.sp, color = IosRed)
        Icon(Icons.Default.ArrowForward, contentDescription = "清空", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
    }
}

fun SwitchRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp)
            Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

fun ExportCell(onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("导出备份", fontSize = 16.sp, color = IosBlue, modifier = Modifier.weight(1f))
        Icon(Icons.Default.ArrowForward, contentDescription = "导出", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
    }
}

fun ClearCell(onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End,
    ) {
        Text("清空全部数据", fontSize = 16.sp, color = IosRed)
        Icon(Icons.Default.ArrowForward, contentDescription = "清空", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
    }
}

fun SwitchRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp)
            Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

fun exportData() {
    val data = mapOf(
        "purchases" to repo.purchases,
        "diary" to repo.diary,
        "family" to repo.settings.family,
        "settings" to mapOf(
            "sortOrder" to repo.settings.sortOrder,
            "family" to repo.settings.family,
            "hiddenEnabled" to repo.settings.hiddenEnabled,
            "theme" to repo.settings.theme,
            "apiProvider" to repo.settings.apiProvider,
            "apiBaseUrl" to repo.settings.apiBaseUrl,
            "apiModel" to repo.settings.apiModel,
        ),
        "exportedAt" to java.time.Instant.now().toString(),
    )
    val blob = Blob(gson.toJson(mapOf(
        "purchases" to repo.purchases,
        "diary" to repo.diary,
        "family" to repo.settings.family,
        "settings" to mapOf(
            "sortOrder" to repo.settings.sortOrder,
            "family" to repo.settings.family,
            "hiddenEnabled" to repo.settings.hiddenEnabled,
            "theme" to repo.settings.theme,
            "apiProvider" to repo.settings.apiProvider,
            "apiBaseUrl" to repo.settings.apiBaseUrl,
            "apiModel" to repo.settings.apiModel,
        ), null, 2))
    val url = URL.createObjectURL(blob)
    val a = document.createElement("a")
    a.href = url
    a.download = "guoziri-backup-${today()}.json"
    a.click()
    URL.revokeObjectURL(url)
    toast("已导出")
}

fun confirmClear() {
    AlertDialog(
        onDismissRequest = { showClear = false },
        title = { Text("清空全部数据？") },
        text = { Text("买菜记录和日记都会删除，无法恢复。") },
        confirmButton = { TextButton(onClick = { repo.clearAll(); showClear = false; toast("已清空"); renderPage("settings") }) { Text("清空", color = IosRed) } },
        dismissButton = { TextButton(onClick = { showClear = false }) { Text("取消") } },
    )
}

private fun SwitchRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp)
            Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

fun ExportCell(onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("导出备份", fontSize = 16.sp, color = IosBlue, modifier = Modifier.weight(1f))
        Icon(Icons.Default.ArrowForward, contentDescription = "导出", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
    }
}

fun ClearCell(onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End,
    ) {
        Text("清空全部数据", fontSize = 16.sp, color = IosRed)
        Icon(Icons.Default.ArrowForward, contentDescription = "清空", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
    }
}

fun SwitchRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp)
            Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

fun ExportCell(onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("导出备份", fontSize = 16.sp, color = IosBlue, modifier = Modifier.weight(1f))
        Icon(Icons.Default.ArrowForward, contentDescription = "导出", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
    }
}

fun ClearCell(onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End,
    ) {
        Text("清空全部数据", fontSize = 16.sp, color = IosRed)
        Icon(Icons.Default.ArrowForward, contentDescription = "清空", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
    }
}

fun SwitchRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp)
            Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

fun exportData() {
    val data = mapOf(
        "purchases" to repo.purchases,
        "diary" to repo.diary,
        "family" to repo.settings.family,
        "settings" to mapOf(
            "sortOrder" to repo.settings.sortOrder,
            "family" to repo.settings.family,
            "hiddenEnabled" to repo.settings.hiddenEnabled,
            "theme" to repo.settings.theme,
            "apiProvider" to repo.settings.apiProvider,
            "apiBaseUrl" to repo.settings.apiBaseUrl,
            "apiModel" to repo.settings.apiModel,
        ),
        "exportedAt" to java.time.Instant.now().toString(),
    )
    val blob = Blob(gson.toJson(mapOf(
        "purchases" to repo.purchases,
        "diary" to repo.diary,
        "family" to repo.settings.family,
        "settings" to mapOf(
            "sortOrder" to repo.settings.sortOrder,
            "family" to repo.settings.family,
            "hiddenEnabled" to repo.settings.hiddenEnabled,
            "theme" to repo.settings.theme,
            "apiProvider" to repo.settings.apiProvider,
            "apiBaseUrl" to repo.settings.apiBaseUrl,
            "apiModel" to repo.settings.apiModel,
        ), null, 2))
    val url = URL.createObjectURL(blob)
    val a = document.createElement("a")
    a.href = url
    a.download = "guoziri-backup-${today()}.json"
    a.click()
    URL.revokeObjectURL(url)
    toast("已导出")
}

fun confirmClear() {
    AlertDialog(
        onDismissRequest = { showClear = false },
        title = { Text("清空全部数据？") },
        text = { Text("买菜记录和日记都会删除，无法恢复。") },
        confirmButton = { TextButton(onClick = { repo.clearAll(); showClear = false; toast("已清空"); renderPage("settings") }) { Text("清空", color = IosRed) } },
        dismissButton = { TextButton(onClick = { showClear = false }) { Text("取消") } },
    )
}

private fun SwitchRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp)
            Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

fun ExportCell(onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("导出备份", fontSize = 16.sp, color = IosBlue, modifier = Modifier.weight(1f))
        Icon(Icons.Default.ArrowForward, contentDescription = "导出", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
    }
}

fun ClearCell(onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End,
    ) {
        Text("清空全部数据", fontSize = 16.sp, color = IosRed)
        Icon(Icons.Default.ArrowForward, contentDescription = "清空", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
    }
}

fun SwitchRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp)
            Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

fun ExportCell(onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("导出备份", fontSize = 16.sp, color = IosBlue, modifier = Modifier.weight(1f))
        Icon(Icons.Default.ArrowForward, contentDescription = "导出", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
    }
}

fun ClearCell(onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End,
    ) {
        Text("清空全部数据", fontSize = 16.sp, color = IosRed)
        Icon(Icons.Default.ArrowForward, contentDescription = "清空", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
    }
}

fun SwitchRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp)
            Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

fun exportData() {
    val data = mapOf(
        "purchases" to repo.purchases,
        "diary" to repo.diary,
        "family" to repo.settings.family,
        "settings" to mapOf(
            "sortOrder" to repo.settings.sortOrder,
            "family" to repo.settings.family,
            "hiddenEnabled" to repo.settings.hiddenEnabled,
            "theme" to repo.settings.theme,
            "apiProvider" to repo.settings.apiProvider,
            "apiBaseUrl" to repo.settings.apiBaseUrl,
            "apiModel" to repo.settings.apiModel,
        ),
        "exportedAt" to java.time.Instant.now().toString(),
    )
    val blob = Blob(gson.toJson(mapOf(
        "purchases" to repo.purchases,
        "diary" to repo.diary,
        "family" to repo.settings.family,
        "settings" to mapOf(
            "sortOrder" to repo.settings.sortOrder,
            "family" to repo.settings.family,
            "hiddenEnabled" to repo.settings.hiddenEnabled,
            "theme" to repo.settings.theme,
            "apiProvider" to repo.settings.apiProvider,
            "apiBaseUrl" to repo.settings.apiBaseUrl,
            "apiModel" to repo.settings.apiModel,
        ), null, 2))
    val url = URL.createObjectURL(blob)
    val a = document.createElement("a")
    a.href = url
    a.download = "guoziri-backup-${today()}.json"
    a.click()
    URL.revokeObjectURL(url)
    toast("已导出")
}

fun confirmClear() {
    AlertDialog(
        onDismissRequest = { showClear = false },
        title = { Text("清空全部数据？") },
        text = { Text("买菜记录和日记都会删除，无法恢复。") },
        confirmButton = { TextButton(onClick = { repo.clearAll(); showClear = false; toast("已清空"); renderPage("settings") }) { Text("清空", color = IosRed) } },
        dismissButton = { TextButton(onClick = { showClear = false }) { Text("取消") } },
    )
}

private fun SwitchRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp)
            Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

fun ExportCell(onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("导出备份", fontSize = 16.sp, color = IosBlue, modifier = Modifier.weight(1f))
        Icon(Icons.Default.ArrowForward, contentDescription = "导出", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
    }
}

fun ClearCell(onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End,
    ) {
        Text("清空全部数据", fontSize = 16.sp, color = IosRed)
        Icon(Icons.Default.ArrowForward, contentDescription = "清空", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
    }
}

fun SwitchRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp)
            Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

fun exportData() {
    val data = mapOf(
        "purchases" to repo.purchases,
        "diary" to repo.diary,
        "family" to repo.settings.family,
        "settings" to mapOf(
            "sortOrder" to repo.settings.sortOrder,
            "family" to repo.settings.family,
            "hiddenEnabled" to repo.settings.hiddenEnabled,
            "theme" to repo.settings.theme,
            "apiProvider" to repo.settings.apiProvider,
            "apiBaseUrl" to repo.settings.apiBaseUrl,
            "apiModel" to repo.settings.apiModel,
        ),
        "exportedAt" to java.time.Instant.now().toString(),
    )
    val blob = Blob(gson.toJson(mapOf(
        "purchases" to repo.purchases,
        "diary" to repo.diary,
        "family" to repo.settings.family,
        "settings" to mapOf(
            "sortOrder" to repo.settings.sortOrder,
            "family" to repo.settings.family,
            "hiddenEnabled" to repo.settings.hiddenEnabled,
            "theme" to repo.settings.theme,
            "apiProvider" to repo.settings.apiProvider,
            "apiBaseUrl" to repo.settings.apiBaseUrl,
            "apiModel" to repo.settings.apiModel,
        ), null, 2))
    val url = URL.createObjectURL(blob)
    val a = document.createElement("a")
    a.href = url
    a.download = "guoziri-backup-${today()}.json"
    a.click()
    URL.revokeObjectURL(url)
    toast("已导出")
}

fun confirmClear() {
    AlertDialog(
        onDismissRequest = { showClear = false },
        title = { Text("清空全部数据？") },
        text = { Text("买菜记录和日记都会删除，无法恢复。") },
        confirmButton = { TextButton(onClick = { repo.clearAll(); showClear = false; toast("已清空"); renderPage("settings") }) { Text("清空", color = IosRed) } },
        dismissButton = { TextButton(onClick = { showClear = false }) { Text("取消") } },
    )
}

private fun SwitchRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp)
            Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

fun ExportCell(onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("导出备份", fontSize = 16.sp, color = IosBlue, modifier = Modifier.weight(1f))
        Icon(Icons.Default.ArrowForward, contentDescription = "导出", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
    }
}

fun ClearCell(onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End,
    ) {
        Text("清空全部数据", fontSize = 16.sp, color = IosRed)
        Icon(Icons.Default.ArrowForward, contentDescription = "清空", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
    }
}

fun SwitchRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp)
            Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

fun exportData() {
    val data = mapOf(
        "purchases" to repo.purchases,
        "diary" to repo.diary,
        "family" to repo.settings.family,
        "settings" to mapOf(
            "sortOrder" to repo.settings.sortOrder,
            "family" to repo.settings.family,
            "hiddenEnabled" to repo.settings.hiddenEnabled,
            "theme" to repo.settings.theme,
            "apiProvider" to repo.settings.apiProvider,
            "apiBaseUrl" to repo.settings.apiBaseUrl,
            "apiModel" to repo.settings.apiModel,
        ),
        "exportedAt" to java.time.Instant.now().toString(),
    )
    val blob = Blob(gson.toJson(mapOf(
        "purchases" to repo.purchases,
        "diary" to repo.diary,
        "family" to repo.settings.family,
        "settings" to mapOf(
            "sortOrder" to repo.settings.sortOrder,
            "family" to repo.settings.family,
            "hiddenEnabled" to repo.settings.hiddenEnabled,
            "theme" to repo.settings.theme,
            "apiProvider" to repo.settings.apiProvider,
            "apiBaseUrl" to repo.settings.apiBaseUrl,
            "apiModel" to repo.settings.apiModel,
        ), null, 2))
    val url = URL.createObjectURL(blob)
    val a = document.createElement("a")
    a.href = url
    a.download = "guoziri-backup-${today()}.json"
    a.click()
    URL.revokeObjectURL(url)
    toast("已导出")
}

fun confirmClear() {
    AlertDialog(
        onDismissRequest = { showClear = false },
        title = { Text("清空全部数据？") },
        text = { Text("买菜记录和日记都会删除，无法恢复。") },
        confirmButton = { TextButton(onClick = { repo.clearAll(); showClear = false; toast("已清空"); renderPage("settings") }) { Text("清空", color = IosRed) } },
        dismissButton = { TextButton(onClick = { showClear = false }) { Text("取消") } },
    )
}

private fun SwitchRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp)
            Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

fun ExportCell(onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("导出备份", fontSize = 16.sp, color = IosBlue, modifier = Modifier.weight(1f))
        Icon(Icons.Default.ArrowForward, contentDescription = "导出", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
    }
}

fun ClearCell(onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End,
    ) {
        Text("清空全部数据", fontSize = 16.sp, color = IosRed)
        Icon(Icons.Default.ArrowForward, contentDescription = "清空", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
    }
}

fun SwitchRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp)
            Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

fun exportData() {
    val data = mapOf(
        "purchases" to repo.purchases,
        "diary" to repo.diary,
        "family" to repo.settings.family,
        "settings" to mapOf(
            "sortOrder" to repo.settings.sortOrder,
            "family" to repo.settings.family,
            "hiddenEnabled" to repo.settings.hiddenEnabled,
            "theme" to repo.settings.theme,
            "apiProvider" to repo.settings.apiProvider,
            "apiBaseUrl" to repo.settings.apiBaseUrl,
            "apiModel" to repo.settings.apiModel,
        ),
        "exportedAt" to java.time.Instant.now().toString(),
    )
    val blob = Blob(gson.toJson(mapOf(
        "purchases" to repo.purchases,
        "diary" to repo.diary,
        "family" to repo.settings.family,
        "settings" to mapOf(
            "sortOrder" to repo.settings.sortOrder,
            "family" to repo.settings.family,
            "hiddenEnabled" to repo.settings.hiddenEnabled,
            "theme" to repo.settings.theme,
            "apiProvider" to repo.settings.apiProvider,
            "apiBaseUrl" to repo.settings.apiBaseUrl,
            "apiModel" to repo.settings.apiModel,
        ), null, 2))
    val url = URL.createObjectURL(blob)
    val a = document.createElement("a")
    a.href = url
    a.download = "guoziri-backup-${today()}.json"
    a.click()
    URL.revokeObjectURL(url)
    toast("已导出")
}

fun confirmClear() {
    AlertDialog(
        onDismissRequest = { showClear = false },
        title = { Text("清空全部数据？") },
        text = { Text("买菜记录和日记都会删除，无法恢复。") },
        confirmButton = { TextButton(onClick = { repo.clearAll(); showClear = false; toast("已清空"); renderPage("settings") }) { Text("清空", color = IosRed) } },
        dismissButton = { TextButton(onClick = { showClear = false }) { Text("取消") } },
    )
}

private fun SwitchRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp)
            Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

fun ExportCell(onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("导出备份", fontSize = 16.sp, color = IosBlue, modifier = Modifier.weight(1f))
        Icon(Icons.Default.ArrowForward, contentDescription = "导出", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
    }
}

fun ClearCell(onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End,
    ) {
        Text("清空全部数据", fontSize = 16.sp, color = IosRed)
        Icon(Icons.Default.ArrowForward, contentDescription = "清空", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
    }
}

fun SwitchRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp)
            Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

fun exportData() {
    val data = mapOf(
        "purchases" to repo.purchases,
        "diary" to repo.diary,
        "family" to repo.settings.family,
        "settings" to mapOf(
            "sortOrder" to repo.settings.sortOrder,
            "family" to repo.settings.family,
            "hiddenEnabled" to repo.settings.hiddenEnabled,
            "theme" to repo.settings.theme,
            "apiProvider" to repo.settings.apiProvider,
            "apiBaseUrl" to repo.settings.apiBaseUrl,
            "apiModel" to repo.settings.apiModel,
        ),
        "exportedAt" to java.time.Instant.now().toString(),
    )
    val blob = Blob(gson.toJson(mapOf(
        "purchases" to repo.purchases,
        "diary" to repo.diary,
        "family" to repo.settings.family,
        "settings" to mapOf(
            "sortOrder" to repo.settings.sortOrder,
            "family" to repo.settings.family,
            "hiddenEnabled" to repo.settings.hiddenEnabled,
            "theme" to repo.settings.theme,
            "apiProvider" to repo.settings.apiProvider,
            "apiBaseUrl" to repo.settings.apiBaseUrl,
            "apiModel" to repo.settings.apiModel,
        ), null, 2))
    val url = URL.createObjectURL(blob)
    val a = document.createElement("a")
    a.href = url
    a.download = "guoziri-backup-${today()}.json"
    a.click()
    URL.revokeObjectURL(url)
    toast("已导出")
}

fun confirmClear() {
    AlertDialog(
        onDismissRequest = { showClear = false },
        title = { Text("清空全部数据？") },
        text = { Text("买菜记录和日记都会删除，无法恢复。") },
        confirmButton = { TextButton(onClick = { repo.clearAll(); showClear = false; toast("已清空"); renderPage("settings") }) { Text("清空", color = IosRed) } },
        dismissButton = { TextButton(onClick = { showClear = false }) { Text("取消") } },
    )
}

private fun SwitchRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp)
            Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

fun ExportCell(onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("导出备份", fontSize = 16.sp, color = IosBlue, modifier = Modifier.weight(1f))
        Icon(Icons.Default.ArrowForward, contentDescription = "导出", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
    }
}

fun ClearCell(onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End,
    ) {
        Text("清空全部数据", fontSize = 16.sp, color = IosRed)
        Icon(Icons.Default.ArrowForward, contentDescription = "清空", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
    }
}

fun SwitchRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp)
            Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

fun exportData() {
    val data = mapOf(
        "purchases" to repo.purchases,
        "diary" to repo.diary,
        "family" to repo.settings.family,
        "settings" to mapOf(
            "sortOrder" to repo.settings.sortOrder,
            "family" to repo.settings.family,
            "hiddenEnabled" to repo.settings.hiddenEnabled,
            "theme" to repo.settings.theme,
            "apiProvider" to repo.settings.apiProvider,
            "apiBaseUrl" to repo.settings.apiBaseUrl,
            "apiModel" to repo.settings.apiModel,
        ),
        "exportedAt" to java.time.Instant.now().toString(),
    )
    val blob = Blob(gson.toJson(mapOf(
        "purchases" to repo.purchases,
        "diary" to repo.diary,
        "family" to repo.settings.family,
        "settings" to mapOf(
            "sortOrder" to repo.settings.sortOrder,
            "family" to repo.settings.family,
            "hiddenEnabled" to repo.settings.hiddenEnabled,
            "theme" to repo.settings.theme,
            "apiProvider" to repo.settings.apiProvider,
            "apiBaseUrl" to repo.settings.apiBaseUrl,
            "apiModel" to repo.settings.apiModel,
        ), null, 2))
    val url = URL.createObjectURL(blob)
    val a = document.createElement("a")
    a.href = url
    a.download = "guoziri-backup-${today()}.json"
    a.click()
    URL.revokeObjectURL(url)
    toast("已导出")
}

fun confirmClear() {
    AlertDialog(
        onDismissRequest = { showClear = false },
        title = { Text("清空全部数据？") },
        text = { Text("买菜记录和日记都会删除，无法恢复。") },
        confirmButton = { TextButton(onClick = { repo.clearAll(); showClear = false; toast("已清空"); renderPage("settings") }) { Text("清空", color = IosRed) } },
        dismissButton = { TextButton(onClick = { showClear = false }) { Text("取消") } },
    )
}

private fun SwitchRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp)
            Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

fun ExportCell(onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("导出备份", fontSize = 16.sp, color = IosBlue, modifier = Modifier.weight(1f))
        Icon(Icons.Default.ArrowForward, contentDescription = "导出", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
    }
}

fun ClearCell(onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End,
    ) {
        Text("清空全部数据", fontSize = 16.sp, color = IosRed)
        Icon(Icons.Default.ArrowForward, contentDescription = "清空", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
    }
}

fun SwitchRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp)
            Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

fun exportData() {
    val data = mapOf(
        "purchases" to repo.purchases,
        "diary" to repo.diary,
        "family" to repo.settings.family,
        "settings" to mapOf(
            "sortOrder" to repo.settings.sortOrder,
            "family" to repo.settings.family,
            "hiddenEnabled" to repo.settings.hiddenEnabled,
            "theme" to repo.settings.theme,
            "apiProvider" to repo.settings.apiProvider,
            "apiBaseUrl" to repo.settings.apiBaseUrl,
            "apiModel" to repo.settings.apiModel,
        ),
        "exportedAt" to java.time.Instant.now().toString(),
    )
    val blob = Blob(gson.toJson(mapOf(
        "purchases" to repo.purchases,
        "diary" to repo.diary,
        "family" to repo.settings.family,
        "settings" to mapOf(
            "sortOrder" to repo.settings.sortOrder,
            "family" to repo.settings.family,
            "hiddenEnabled" to repo.settings.hiddenEnabled,
            "theme" to repo.settings.theme,
            "apiProvider" to repo.settings.apiProvider,
            "apiBaseUrl" to repo.settings.apiBaseUrl,
            "apiModel" to repo.settings.apiModel,
        ), null, 2))
    val url = URL.createObjectURL(blob)
    val a = document.createElement("a")
    a.href = url
    a.download = "guoziri-backup-${today()}.json"
    a.click()
    URL.revokeObjectURL(url)
    toast("已导出")
}

fun confirmClear() {
    AlertDialog(
        onDismissRequest = { showClear = false },
        title = { Text("清空全部数据？") },
        text = { Text("买菜记录和日记都会删除，无法恢复。") },
        confirmButton = { TextButton(onClick = { repo.clearAll(); showClear = false; toast("已清空"); renderPage("settings") }) { Text("清空", color = IosRed) } },
        dismissButton = { TextButton(onClick = { showClear = false }) { Text("取消") } },
    )
}

private fun SwitchRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp)
            Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

fun ExportCell(onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("导出备份", fontSize = 16.sp, color = IosBlue, modifier = Modifier.weight(1f))
        Icon(Icons.Default.ArrowForward, contentDescription = "导出", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
    }
}

fun ClearCell(onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End,
    ) {
        Text("清空全部数据", fontSize = 16.sp, color = IosRed)
        Icon(Icons.Default.ArrowForward, contentDescription = "清空", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
    }
}

fun SwitchRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp)
            Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

fun exportData() {
    val data = mapOf(
        "purchases" to repo.purchases,
        "diary" to repo.diary,
        "family" to repo.settings.family,
        "settings" to mapOf(
            "sortOrder" to repo.settings.sortOrder,
            "family" to repo.settings.family,
            "hiddenEnabled" to repo.settings.hiddenEnabled,
            "theme" to repo.settings.theme,
            "apiProvider" to repo.settings.apiProvider,
            "apiBaseUrl" to repo.settings.apiBaseUrl,
            "apiModel" to repo.settings.apiModel,
        ),
        "exportedAt" to java.time.Instant.now().toString(),
    )
    val blob = Blob(gson.toJson(mapOf(
        "purchases" to repo.purchases,
        "diary" to repo.diary,
        "family" to repo.settings.family,
        "settings" to mapOf(
            "sortOrder" to repo.settings.sortOrder,
            "family" to repo.settings.family,
            "hiddenEnabled" to repo.settings.hiddenEnabled,
            "theme" to repo.settings.theme,
            "apiProvider" to repo.settings.apiProvider,
            "apiBaseUrl" to repo.settings.apiBaseUrl,
            "apiModel" to repo.settings.apiModel,
        ), null, 2))
    val url = URL.createObjectURL(blob)
    val a = document.createElement("a")
    a.href = url
    a.download = "guoziri-backup-${today()}.json"
    a.click()
    URL.revokeObjectURL(url)
    toast("已导出")
}

fun confirmClear() {
    AlertDialog(
        onDismissRequest = { showClear = false },
        title = { Text("清空全部数据？") },
        text = { Text("买菜记录和日记都会删除，无法恢复。") },
        confirmButton = { TextButton(onClick = { repo.clearAll(); showClear = false; toast("已清空"); renderPage("settings") }) { Text("清空", color = IosRed) } },
        dismissButton = { TextButton(onClick = { showClear = false }) { Text("取消") } },
    )
package com.guoziri.app

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class Preset(val id: String, val label: String, val baseUrl: String, val model: String, val keyHint: String)

val PRESETS = listOf(
    Preset("deepseek", "DeepSeek", "https://api.deepseek.com/v1", "deepseek-chat", "sk-…"),
    Preset("glm", "智谱 GLM", "https://open.bigmodel.cn/api/paas/v4", "glm-4-flash", "id…"),
    Preset("nvidia", "NVIDIA NIM", "https://integrate.api.nvidia.com/v1", "meta/llama-3.1-8b-instruct", "nvapi-…"),
    Preset("moonshot", "Moonshot Kimi", "https://api.moonshot.cn/v1", "moonshot-v1-8k", "sk-…"),
    Preset("qwen", "阿里通义千问", "https://dashscope.aliyuncs.com/compatible-mode/v1", "qwen-turbo", "sk-…"),
    Preset("siliconflow", "硅基流动", "https://api.siliconflow.cn/v1", "Qwen/Qwen2.5-7B-Instruct", "sk-…"),
    Preset("openrouter", "OpenRouter", "https://openrouter.ai/api/v1", "openai/gpt-4o-mini", "sk-or-…"),
    Preset("custom", "自定义 (OpenAI 兼容)", "", "", ""),
)

@Composable
fun SettingsPage(repo: StoreRepo, snackbar: (String) -> Unit, onReplayGuide: () -> Unit) {
    val s = repo.settings
    val context = LocalContext.current
    var keyInput by remember { mutableStateOf("") }
    var modelInput by remember { mutableStateOf(s.apiModel.ifBlank { "deepseek-chat" }) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 110.dp)) {
        Text("设置", fontSize = 34.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 20.dp, top = 12.dp, bottom = 6.dp))

        GroupTitle("AI 服务商")
        GroupCard {
            PRESETS.forEach { preset ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable {
                        repo.updateSettings { cur ->
                            cur.copy(
                                apiProvider = preset.id,
                                apiBaseUrl = preset.baseUrl.ifBlank { cur.apiBaseUrl },
                                apiModel = preset.model.ifBlank { cur.apiModel },
                            )
                        }
                        snackbar("已切换到 ${preset.label}")
                    }.padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(preset.label, fontSize = 16.sp)
                        if (preset.baseUrl.isNotEmpty())
                            Text(preset.baseUrl.removePrefix("https://"), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f))
                    }
                    if (s.apiProvider == preset.id) Text("✓", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = IosBlue)
                }
            }
        }

        GroupTitle("当前服务 · Key / 模型")
        GroupCard {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("API 状态", fontSize = 16.sp)
                Text(if (s.apiKey.isNotBlank()) "● 已配置" else "○ 未配置", fontSize = 14.sp,
                    color = if (s.apiKey.isNotBlank()) IosGreen else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("API 地址", fontSize = 16.sp)
                Text(s.apiBaseUrl.ifBlank { "未设置" }, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                    modifier = Modifier.widthIn(max = 180.dp), maxLines = 1)
            }
            OutlinedTextField(
                value = keyInput, onValueChange = { keyInput = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                placeholder = { Text((PRESETS.find { it.id == s.apiProvider }?.keyHint ?: "sk-…"), fontSize = 14.sp) },
                label = { Text("API Key（输入即覆盖）", fontSize = 13.sp) },
                singleLine = true, visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                shape = RoundedCornerShape(10.dp),
            )
            OutlinedTextField(
                value = modelInput, onValueChange = { modelInput = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                label = { Text("模型", fontSize = 13.sp) },
                singleLine = true, shape = RoundedCornerShape(10.dp),
            )
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = {
                    val key = keyInput.trim()
                    val model = modelInput.trim()
                    if (key.isNotEmpty() || (model.isNotEmpty() && model != s.apiModel)) {
                        repo.updateSettings { cur ->
                            var next = cur
                            if (key.isNotEmpty()) next = next.copy(apiKey = key)
                            if (model.isNotEmpty()) next = next.copy(apiModel = model)
                            next
                        }
                        keyInput = ""
                        snackbar("已保存")
                    }
                }) { Text("保存 Key / 模型", fontSize = 15.sp) }
            }
        }
        Text("Key 只存在本机数据目录，不上传、不备份、不写日志",
            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))

        GroupTitle("通用")
        GroupCard {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("记录排序", fontSize = 16.sp)
                    Text("「全部」列表的顺序", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
                Switch(checked = s.sortOrder == "desc", onCheckedChange = { checked ->
                    repo.updateSettings { it.copy(sortOrder = if (checked) "desc" else "asc") }
                })
            }
            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("显示日记入口", fontSize = 16.sp)
                    Text("在底部导航栏加入「日记」", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
                Switch(checked = s.hiddenEnabled, onCheckedChange = { checked ->
                    repo.updateSettings { it.copy(hiddenEnabled = checked) }
                })
            }
            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
            Row(Modifier.fillMaxWidth().clickable { showThemePicker = true }.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("外观", fontSize = 16.sp)
                    Text("深浅色", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
                Text(when (s.theme) { "light" -> "☀️ 浅色"; "dark" -> "🌙 深色"; else -> "🌤 自动" }, fontSize = 15.sp)
                Text(" ›", fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
            }
        }

        GroupTitle("数据")
        GroupCard {
            Row(Modifier.fillMaxWidth().clickable {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/json"
                    putExtra(Intent.EXTRA_TEXT, repo.exportJson())
                }
                context.startActivity(Intent.createChooser(intent, "导出备份"))
                snackbar("已通过分享导出")
            }.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("导出备份", fontSize = 16.sp, color = IosBlue, modifier = Modifier.weight(1f))
                Text("›", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
            }
            Row(Modifier.fillMaxWidth().clickable { onReplayGuide() }.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("重播新手引导", fontSize = 16.sp, color = IosBlue, modifier = Modifier.weight(1f))
                Text("›", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
            }
            Row(Modifier.fillMaxWidth().clickable { showClear = true }.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("清空全部数据", fontSize = 16.sp, color = IosRed, modifier = Modifier.weight(1f))
                Text("›", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
            }
        }

        Text("过日子 v1.0-native · 数据保存在本机",
            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
            modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }

    // 外观三档弹窗
    if (showThemePicker) {
        AlertDialog(
            onDismissRequest = { showThemePicker = false },
            confirmButton = {},
            title = { Text("外观") },
            text = {
                Column {
                    listOf("auto" to "🌤 自动（跟随系统）", "light" to "☀️ 浅色", "dark" to "🌙 深色").forEach { (v, label) ->
                        Text(label, fontSize = 16.sp, color = if (s.theme == v) IosBlue else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.fillMaxWidth().clickable {
                                repo.updateSettings { it.copy(theme = v) }
                                ThemePrefs.theme.value = v
                                showThemePicker = false
                            }.padding(vertical = 12.dp))
                    }
                }
            },
        )
    }
    // 清空确认
    if (showClear) {
        AlertDialog(
            onDismissRequest = { showClear = false },
            title = { Text("清空全部数据？") },
            text = { Text("买菜记录和日记都会删除，无法恢复。") },
            confirmButton = { TextButton(onClick = { repo.clearAll(); showClear = false; snackbar("已清空") }) { Text("清空", color = IosRed) } },
            dismissButton = { TextButton(onClick = { showClear = false }) { Text("取消") } },
        )
    }
}

private var showThemePicker by mutableStateOf(false)
private var showClear by mutableStateOf(false)

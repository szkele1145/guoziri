package com.guoziri.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class Preset(
    val id: String,
    val label: String,
    val baseUrl: String,
    val model: String,
    val keyHint: String,
)

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
fun SettingsPage(
    repo: StoreRepo,
    snackbar: (String) -> Unit,
) {
    val s = repo.settings
    var keyInput by remember { mutableStateOf("") }
    var modelInput by remember { mutableStateOf(s.apiModel.ifBlank { "deepseek-chat" }) }
    var showThemePicker by remember { mutableStateOf(false) }
    var showClear by remember { mutableStateOf(false) }
    var showCustomFoods by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 40.dp)) {
        PageTitle("设置", subtitle = "过日子 v1.0 · 数据只保存在这台手机上")

        GroupTitle("AI 服务商")
        GroupCard(Modifier.padding(horizontal = Dimens.PageH)) {
            PRESETS.forEachIndexed { i, preset ->
                if (i > 0) CardDivider()
                PresetCell(
                    preset = preset,
                    isSelected = s.apiProvider == preset.id,
                    onClick = {
                        repo.updateSettings { cur ->
                            cur.copy(
                                apiProvider = preset.id,
                                apiBaseUrl = preset.baseUrl.ifBlank { cur.apiBaseUrl },
                                apiModel = preset.model.ifBlank { cur.apiModel },
                            )
                        }
                        snackbar("已切换到 ${preset.label}")
                    },
                )
            }
        }

        GroupTitle("当前服务配置")
        GroupCard(Modifier.padding(horizontal = Dimens.PageH)) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = Dimens.CardPad, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (s.hasKey) IosGreen else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)),
                )
                Spacer(Modifier.width(9.dp))
                Text(if (s.hasKey) "已配置" else "未配置", fontSize = Dimens2.Body, modifier = Modifier.weight(1f))
                Text(
                    s.apiBaseUrl.ifBlank { "未设置" },
                    fontSize = Dimens2.Caption,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.42f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            OutlinedTextField(
                value = keyInput,
                onValueChange = { keyInput = it },
                label = { Text("API Key", fontSize = 12.sp) },
                placeholder = { Text(if (s.hasKey) "已保存，输入可覆盖" else "sk-…", fontSize = 14.sp) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = Dimens.CardPad, vertical = 4.dp),
                singleLine = true,
                textStyle = TextStyle(fontSize = 15.sp),
                visualTransformation = PasswordVisualTransformation(),
                shape = RoundedCornerShape(10.dp),
            )
            OutlinedTextField(
                value = modelInput,
                onValueChange = { modelInput = it },
                label = { Text("模型", fontSize = 12.sp) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = Dimens.CardPad, vertical = 4.dp),
                singleLine = true,
                textStyle = TextStyle(fontSize = 15.sp),
                shape = RoundedCornerShape(10.dp),
            )
            Row(
                Modifier.fillMaxWidth().padding(start = Dimens.CardPad, end = Dimens.CardPad, top = 4.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                Button(
                    onClick = {
                        val key = keyInput.trim()
                        val model = modelInput.trim()
                        if (key.isNotBlank() || (model.isNotBlank() && model != s.apiModel)) {
                            repo.updateSettings { cur ->
                                var next = cur
                                if (key.isNotBlank()) next = next.copy(apiKey = key)
                                if (model.isNotBlank()) next = next.copy(apiModel = model)
                                next
                            }
                            keyInput = ""
                            snackbar("已保存")
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = IosBlue),
                ) {
                    Text("保存", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        Text(
            "Key 只存在本机数据目录，不上传、不备份、不写日志",
            fontSize = Dimens2.Micro,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.32f),
            modifier = Modifier.padding(start = Dimens.PageH, top = 6.dp, end = Dimens.PageH),
        )

        GroupTitle("通用")
        GroupCard(Modifier.padding(horizontal = Dimens.PageH)) {
            SettingRow(
                icon = Icons.Rounded.List,
                title = "记录排序",
                subtitle = if (s.sortOrder == "desc") "最新的在最前面" else "最旧的在最前面",
                trailing = {
                    Text(
                        if (s.sortOrder == "desc") "倒序" else "正序",
                        fontSize = Dimens2.Caption,
                        color = IosBlue,
                    )
                },
                onClick = {
                    repo.updateSettings { it.copy(sortOrder = if (it.sortOrder == "desc") "asc" else "desc") }
                },
            )
            CardDivider()
            SettingRow(
                icon = Icons.Rounded.Menu,
                title = "显示日记入口",
                subtitle = "在底部导航栏加入「日记」",
                trailing = {
                    Switch(
                        checked = s.hiddenEnabled,
                        onCheckedChange = { checked ->
                            repo.updateSettings { it.copy(hiddenEnabled = checked) }
                        },
                    )
                },
            )
        }

        GroupTitle("外观")
        GroupCard(Modifier.padding(horizontal = Dimens.PageH)) {
            SettingRow(
                icon = when (s.theme) {
                    "light" -> Icons.Rounded.Star
                    "dark" -> Icons.Rounded.Star
                    else -> Icons.Rounded.Info
                },
                title = "主题",
                subtitle = when (s.theme) {
                    "light" -> "浅色"
                    "dark" -> "深色"
                    else -> "跟随系统"
                },
                trailing = {
                    Icon(
                        Icons.Rounded.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
                        modifier = Modifier.size(18.dp),
                    )
                },
                onClick = { showThemePicker = true },
            )
        }

        GroupTitle("数据")
        GroupCard(Modifier.padding(horizontal = Dimens.PageH)) {
            SettingRow(
                icon = Icons.Rounded.List,
                title = "导出备份",
                subtitle = "把数据导出成 JSON 文件",
                trailing = {
                    Icon(
                        Icons.Rounded.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
                        modifier = Modifier.size(18.dp),
                    )
                },
                onClick = { snackbar("导出功能开发中") },
            )
            CardDivider()
            SettingRow(
                icon = Icons.Rounded.Delete,
                title = "清空全部数据",
                subtitle = "删除所有买菜记录和日记",
                iconTint = IosRed,
                onClick = { showClear = true },
            )
        }

        GroupTitle("帮助")
        GroupCard(Modifier.padding(horizontal = Dimens.PageH)) {
            SettingRow(
                icon = Icons.Rounded.Menu,
                title = "我录入的食物",
                subtitle = if (repo.settings.customFoods.isEmpty()) "还没有录入过"
                else "共 ${repo.settings.customFoods.size} 种",
                trailing = {
                    Icon(
                        Icons.Rounded.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
                        modifier = Modifier.size(18.dp),
                    )
                },
                onClick = { showCustomFoods = true },
            )
        }

        Spacer(Modifier.height(30.dp))
    }

    if (showThemePicker) {
        AlertDialog(
            onDismissRequest = { showThemePicker = false },
            title = { Text("主题") },
            text = {
                Column {
                    listOf(
                        "auto" to ("跟随系统" to Icons.Rounded.Info),
                        "light" to ("浅色" to Icons.Rounded.Star),
                        "dark" to ("深色" to Icons.Rounded.Star),
                    ).forEach { (v, pair) ->
                        val (label, icon) = pair
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    repo.updateSettings { it.copy(theme = v) }
                                    ThemePrefs.theme.value = v
                                    showThemePicker = false
                                }
                                .padding(vertical = 12.dp),
                        ) {
                            Icon(
                                icon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                                modifier = Modifier.size(19.dp),
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(label, fontSize = Dimens2.Body, modifier = Modifier.weight(1f))
                            if (s.theme == v) {
                                Icon(
                                    Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = IosBlue,
                                    modifier = Modifier.size(19.dp),
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showThemePicker = false }) { Text("完成") } },
        )
    }

    if (showCustomFoods) {
        CustomFoodDialog(repo, snackbar) { showCustomFoods = false }
    }

    if (showClear) {
        AlertDialog(
            onDismissRequest = { showClear = false },
            title = { Text("清空全部数据？") },
            text = { Text("买菜记录和日记都会删除，无法恢复。") },
            confirmButton = {
                TextButton(onClick = { repo.clearAll(); showClear = false; snackbar("已清空") }) {
                    Text("清空", color = IosRed)
                }
            },
            dismissButton = { TextButton(onClick = { showClear = false }) { Text("取消") } },
        )
    }
}

@Composable
fun PresetCell(preset: Preset, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = Dimens.CardPad, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                preset.label,
                fontSize = Dimens2.Body,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isSelected) IosBlue else MaterialTheme.colorScheme.onSurface,
            )
            if (preset.model.isNotBlank()) {
                Text(
                    preset.model,
                    fontSize = Dimens2.Micro,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        if (isSelected) {
            Icon(
                Icons.Rounded.Check,
                contentDescription = null,
                tint = IosBlue,
                modifier = Modifier.size(19.dp),
            )
        }
    }
}

@Composable
fun CustomFoodDialog(repo: StoreRepo, snackbar: (String) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("我录入的食物") },
        text = {
            Column {
                if (repo.settings.customFoods.isEmpty()) {
                    Text(
                        "还没有。记账时输入食材库外的名字，就能手动录营养。",
                        fontSize = Dimens2.Body,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        lineHeight = 21.sp,
                    )
                } else {
                    repo.settings.customFoods.forEachIndexed { i, f ->
                        if (i > 0) CardDivider(modifier = Modifier.padding(start = 0.dp))
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(f.name, fontSize = Dimens2.Body)
                                Text(
                                    "${f.category} · ${f.calories.toInt()} kcal/100g",
                                    fontSize = Dimens2.Caption,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                                    modifier = Modifier.padding(top = 2.dp),
                                )
                            }
                            TextButton(onClick = {
                                repo.removeCustomFood(f.name)
                                snackbar("已删除")
                            }) { Text("删除", fontSize = 13.sp, color = IosRed) }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("关闭") } },
    )
}

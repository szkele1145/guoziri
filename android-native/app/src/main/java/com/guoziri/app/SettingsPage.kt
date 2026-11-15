package com.guoziri.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.guoziri.app.ui.theme.LocalAppColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

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

/**
 * 设置页，结构对齐 web 版 renderSettings。
 */
@Composable
fun SettingsPage(
    repo: StoreRepo,
    snackbar: (String) -> Unit,
) {
    val c = LocalAppColors.current
    val s = repo.settings
    val cur = PRESETS.firstOrNull { it.id == s.apiProvider } ?: PRESETS.firstOrNull()

    var keyInput by remember { mutableStateOf("") }
    var modelInput by remember { mutableStateOf(s.apiModel.ifBlank { "deepseek-chat" }) }
    var showThemePicker by remember { mutableStateOf(false) }
    var showClear by remember { mutableStateOf(false) }
    var showCustomFoods by remember { mutableStateOf(false) }
    var showModels by remember { mutableStateOf(false) }
    var models by remember { mutableStateOf<List<String>>(emptyList()) }
    var loadingModels by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun saveKey() {
        val v = keyInput.trim()
        if (v.isEmpty()) return
        repo.updateSettings { it.copy(apiKey = v) }
        keyInput = ""
        snackbar("已自动保存")
    }

    fun saveModel() {
        val v = modelInput.trim()
        if (v.isEmpty() || v == s.apiModel) return
        repo.updateSettings { it.copy(apiModel = v) }
        snackbar("已自动保存")
    }

    fun fetchModels() {
        if (s.apiKey.isBlank()) { snackbar("先填并保存 API Key"); return }
        loadingModels = true
        scope.launch {
            val got = withContext(Dispatchers.IO) {
                try {
                    val client = OkHttpClient.Builder()
                        .connectTimeout(15, TimeUnit.SECONDS)
                        .readTimeout(30, TimeUnit.SECONDS)
                        .build()
                    val req = Request.Builder()
                        .url(s.apiBaseUrl.trimEnd('/') + "/models")
                        .header("Authorization", "Bearer " + s.apiKey)
                        .build()
                    client.newCall(req).execute().use { resp ->
                        val body = resp.body?.string().orEmpty()
                        val arr = JSONObject(body).optJSONArray("data")
                        val list = mutableListOf<String>()
                        if (arr != null) {
                            for (i in 0 until arr.length()) {
                                val id = arr.optJSONObject(i)?.optString("id").orEmpty()
                                if (id.isNotEmpty()) list.add(id)
                            }
                        }
                        list
                    }
                } catch (e: Exception) {
                    emptyList()
                }
            }
            loadingModels = false
            models = got
            if (got.isEmpty()) snackbar("没拉到模型列表，检查 Key 或地址")
            else showModels = true
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = Dimens.TabH + 24.dp),
    ) {
        PageTitle("设置")

        // ---- AI 服务商 ----
        GroupTitle("AI 服务商")
        GroupCard {
            PRESETS.forEachIndexed { i, p ->
                if (i > 0) CardDivider()
                Row(
                    Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = Dimens.RowH)
                        .clickable {
                            repo.updateSettings {
                                it.copy(
                                    apiProvider = p.id,
                                    apiBaseUrl = p.baseUrl.ifBlank { it.apiBaseUrl },
                                    apiModel = p.model.ifBlank { it.apiModel },
                                )
                            }
                            modelInput = p.model.ifBlank { s.apiModel }
                            snackbar("已切换")
                        }
                        .padding(horizontal = Dimens.CardPad, vertical = Dimens.CardPadV),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            p.label,
                            fontSize = 16.sp,
                            color = if (s.apiProvider == p.id) c.tint else c.label,
                            fontWeight = if (s.apiProvider == p.id) FontWeight.Bold else FontWeight.Normal,
                        )
                        Text(
                            if (p.baseUrl.isNotBlank()) p.baseUrl.removePrefix("https://") else "手动填地址",
                            fontSize = Dimens2.Micro,
                            color = c.secondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 1.dp),
                        )
                    }
                    if (s.apiProvider == p.id) {
                        Text("✓", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = c.tint)
                    }
                }
            }
        }
        Hint("切换服务商自动带出地址和默认模型，Key 各自独立保存")

        // ---- 当前服务配置 ----
        GroupTitle("当前服务配置" + (cur?.let { " · " + it.label } ?: ""))
        GroupCard {
            // API 状态
            Cell(
                title = "API 状态",
                value = if (s.hasKey) "已配置" else "未配置",
                leading = null,
                trailing = {
                    Box(
                        Modifier
                            .padding(start = 6.dp)
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (s.hasKey) c.green else c.tertiary),
                    )
                },
            )
            CardDivider()
            // API 地址
            Row(
                Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = Dimens.RowH)
                    .padding(horizontal = Dimens.CardPad, vertical = Dimens.CardPadV),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("API 地址", fontSize = Dimens2.Body, color = c.label)
                Text(
                    s.apiBaseUrl.ifBlank { "未设置" },
                    fontSize = 12.sp,
                    color = c.secondary,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(start = 12.dp),
                )
            }
            CardDivider()
            // API Key
            InputCell(
                label = "API Key",
                value = keyInput,
                onChange = { keyInput = it },
                placeholder = if (s.hasKey) "已保存（输入可覆盖）" else (cur?.keyHint ?: "sk-…"),
                password = true,
                onBlur = { saveKey() },
            )
            CardDivider()
            // 模型
            InputCell(
                label = "模型",
                value = modelInput,
                onChange = { modelInput = it },
                placeholder = "deepseek-chat",
                password = false,
                onBlur = { saveModel() },
            )
            CardDivider()
            // 拉取模型列表
            Row(
                Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 44.dp)
                    .padding(horizontal = Dimens.CardPad, vertical = 6.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    if (loadingModels) "拉取中…" else "拉取模型列表",
                    fontSize = 15.sp,
                    color = c.secondary,
                    modifier = Modifier
                        .clickable { if (!loadingModels) fetchModels() }
                        .padding(horizontal = 4.dp, vertical = 6.dp),
                )
            }
        }
        Hint("修改 Key/模型后点别处即自动保存 · Key 只存本机，不上传不备份")

        // ---- 通用 ----
        GroupTitle("通用")
        GroupCard {
            Cell(
                title = "记录排序",
                subtitle = "「全部」列表的顺序",
                trailing = { AppToggle(s.sortOrder == "desc") { on -> repo.updateSettings { it.copy(sortOrder = if (on) "desc" else "asc") } } },
            )
            CardDivider()
            Cell(
                title = "显示日记入口",
                subtitle = "在底部导航栏加入「日记」",
                trailing = { AppToggle(s.hiddenEnabled) { on -> repo.updateSettings { it.copy(hiddenEnabled = on) } } },
            )
            CardDivider()
            Cell(
                title = "外观",
                subtitle = "深浅色",
                value = when (s.theme) { "light" -> "浅色"; "dark" -> "深色"; else -> "自动" },
                onClick = { showThemePicker = true },
                trailing = {
                    Icon(
                        AppIcons.ChevronRight,
                        contentDescription = null,
                        tint = c.tertiary,
                        modifier = Modifier.size(18.dp),
                    )
                },
            )
        }

        // ---- 数据 ----
        GroupTitle("数据")
        GroupCard {
            Cell(
                title = "导出备份",
                onClick = {
                    repo.exportJson()
                    snackbar("已导出 JSON 到本机目录")
                },
                titleColor = c.tint,
                trailing = { Icon(AppIcons.ChevronRight, null, tint = c.tertiary, modifier = Modifier.size(18.dp)) },
            )
            CardDivider()
            Cell(
                title = "我录入的食物",
                onClick = { showCustomFoods = true },
                titleColor = c.tint,
                value = "${s.customFoods.size} 项",
                trailing = { Icon(AppIcons.ChevronRight, null, tint = c.tertiary, modifier = Modifier.size(18.dp)) },
            )
            CardDivider()
            Cell(
                title = "清空全部数据",
                onClick = { showClear = true },
                titleColor = c.red,
                trailing = { Icon(AppIcons.ChevronRight, null, tint = c.tertiary, modifier = Modifier.size(18.dp)) },
            )
        }

        Text(
            "过日子 v1.2 · 数据保存在本机",
            fontSize = 12.sp,
            color = c.tertiary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 24.dp),
        )
    }

    // 外观三档
    if (showThemePicker) {
        AlertDialog(
            onDismissRequest = { showThemePicker = false },
            containerColor = c.bgElevated,
            shape = RoundedCornerShape(14.dp),
            title = { Text("外观", fontSize = Dimens2.SheetTitle, fontWeight = FontWeight.SemiBold) },
            text = {
                Column {
                    listOf("auto" to "🌤 自动（跟随系统）", "light" to "☀️ 浅色", "dark" to "🌙 深色")
                        .forEach { (v, label) ->
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
                                Text(label, fontSize = Dimens2.Body, color = c.label, modifier = Modifier.weight(1f))
                                if (s.theme == v) {
                                    Icon(AppIcons.Check, null, tint = c.tint, modifier = Modifier.size(19.dp))
                                }
                            }
                        }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemePicker = false }) {
                    Text("完成", fontSize = Dimens2.SheetTitle, color = c.tint)
                }
            },
        )
    }

    // 模型列表
    if (showModels) {
        AlertDialog(
            onDismissRequest = { showModels = false },
            containerColor = c.bgElevated,
            shape = RoundedCornerShape(14.dp),
            title = { Text("选择模型", fontSize = Dimens2.SheetTitle, fontWeight = FontWeight.SemiBold) },
            text = {
                Column(Modifier.heightIn(max = 260.dp).verticalScroll(rememberScrollState())) {
                    models.forEach { m ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    repo.updateSettings { it.copy(apiModel = m) }
                                    modelInput = m
                                    showModels = false
                                    snackbar("已选择 $m")
                                }
                                .padding(horizontal = 10.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                m,
                                fontSize = 15.sp,
                                color = if (m == s.apiModel) c.tint else c.label,
                                fontWeight = if (m == s.apiModel) FontWeight.SemiBold else FontWeight.Normal,
                                modifier = Modifier.weight(1f),
                            )
                            if (m == s.apiModel) Text("✓", fontSize = 15.sp, color = c.tint)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showModels = false }) {
                    Text("关闭", fontSize = Dimens2.SheetTitle, color = c.tint)
                }
            },
        )
    }

    if (showCustomFoods) {
        CustomFoodDialog(repo, snackbar) { showCustomFoods = false }
    }

    if (showClear) {
        AlertDialog(
            onDismissRequest = { showClear = false },
            containerColor = c.bgElevated,
            shape = RoundedCornerShape(14.dp),
            title = { Text("清空全部数据？", fontSize = Dimens2.SheetTitle, fontWeight = FontWeight.SemiBold) },
            text = {
                Text(
                    "买菜记录和日记都会删除，无法恢复。",
                    fontSize = Dimens2.AlertMsg,
                    color = c.label2,
                    lineHeight = 19.sp,
                )
            },
            confirmButton = {
                TextButton(onClick = { repo.clearAll(); showClear = false; snackbar("已清空") }) {
                    Text("清空", fontSize = Dimens2.SheetTitle, fontWeight = FontWeight.SemiBold, color = c.red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClear = false }) {
                    Text("取消", fontSize = Dimens2.SheetTitle, color = c.tint)
                }
            },
        )
    }
}

/** .hint：12px tertiary 居中 */
@Composable
fun Hint(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        fontSize = Dimens2.Hint,
        color = SemanticColors.TextTertiary,
        textAlign = TextAlign.Center,
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
    )
}

/** .cell 内的无边框输入行（17px，右对齐） */
@Composable
private fun InputCell(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
    password: Boolean,
    onBlur: () -> Unit,
) {
    val c = LocalAppColors.current
    var focused by remember { mutableStateOf(false) }
    Row(
        Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = Dimens.RowH)
            .padding(horizontal = Dimens.CardPad, vertical = Dimens.CardPadV),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, fontSize = Dimens2.Body, color = c.label)
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            textStyle = TextStyle(fontSize = Dimens2.Body, color = c.label, textAlign = TextAlign.End),
            cursorBrush = SolidColor(c.tint),
            visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp)
                .onFocusChanged { st ->
                    if (focused && !st.isFocused) onBlur()
                    focused = st.isFocused
                },
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterEnd) {
                    if (value.isEmpty()) {
                        Text(
                            placeholder,
                            fontSize = Dimens2.Body,
                            color = c.tertiary,
                            textAlign = TextAlign.End,
                            maxLines = 1,
                        )
                    }
                    inner()
                }
            },
        )
    }
}

@Composable
fun CustomFoodDialog(repo: StoreRepo, snackbar: (String) -> Unit, onDismiss: () -> Unit) {
    val c = LocalAppColors.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.bgElevated,
        shape = RoundedCornerShape(14.dp),
        title = { Text("我录入的食物", fontSize = Dimens2.SheetTitle, fontWeight = FontWeight.SemiBold) },
        text = {
            Column(Modifier.heightIn(max = 300.dp).verticalScroll(rememberScrollState())) {
                if (repo.settings.customFoods.isEmpty()) {
                    Text(
                        "还没有。记账时输入食材库外的名字，就能手动录营养。",
                        fontSize = Dimens2.AlertMsg,
                        color = c.secondary,
                        lineHeight = 19.sp,
                    )
                } else {
                    repo.settings.customFoods.forEachIndexed { i, f ->
                        if (i > 0) CardDivider(modifier = Modifier.padding(start = 0.dp))
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(f.name, fontSize = Dimens2.Body, color = c.label)
                                Text(
                                    "${f.category} · ${f.calories.toInt()} kcal/100g",
                                    fontSize = Dimens2.Caption,
                                    color = c.secondary,
                                    modifier = Modifier.padding(top = 2.dp),
                                )
                            }
                            Text(
                                "删除",
                                fontSize = 15.sp,
                                color = c.red,
                                modifier = Modifier
                                    .clickable {
                                        repo.removeCustomFood(f.name)
                                        snackbar("已删除")
                                    }
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭", fontSize = Dimens2.SheetTitle, color = c.tint)
            }
        },
    )
}

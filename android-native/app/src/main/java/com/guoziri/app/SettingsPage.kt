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
import androidx.compose.material3.HorizontalDivider
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
import com.guoziri.app.UpdateManager
import androidx.compose.ui.platform.LocalContext

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
    val context = LocalContext.current

    var keyInput by remember { mutableStateOf("") }
    var modelInput by remember { mutableStateOf(s.apiModel.ifBlank { "deepseek-chat" }) }
    var showThemePicker by remember { mutableStateOf(false) }
    var showClear by remember { mutableStateOf(false) }
    var showCustomFoods by remember { mutableStateOf(false) }
    var showModels by remember { mutableStateOf(false) }
    var models by remember { mutableStateOf<List<String>>(emptyList()) }
    var loadingModels by remember { mutableStateOf(false) }
    // 更新相关状态
    var showUpdateDialog by remember { mutableStateOf(false) }
    var pendingReleases by remember { mutableStateOf<List<UpdateManager.ReleaseInfo>>(emptyList()) }
    // 下载进度弹窗状态
    var downloadingVersion by remember { mutableStateOf<String?>(null) }   // 非 null = 下载中
    var downloadPercent by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    var updateManager by remember { mutableStateOf(UpdateManager(context)) }
    var checkingUpdate by remember { mutableStateOf(false) }
    var updateProgress by remember { mutableStateOf(0) }
    var showProgress by remember { mutableStateOf(false) }

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
            .padding(bottom = 24.dp),
    ) {
        PageTitle("设置")

        // ---- AI 服务商 ----
        GroupTitle("AI 服务商", topSpacing = 8.dp)
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
            // API 状态（.dot-ok / .dot-no：8px 圆点在文字左侧）
            Row(
                Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = Dimens.RowH)
                    .padding(horizontal = Dimens.CardPad, vertical = Dimens.CardPadV),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("API 状态", fontSize = Dimens2.Body, color = c.label, modifier = Modifier.weight(1f))
                Box(
                    Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (s.hasKey) c.green else c.tertiary),
                )
                Text(
                    if (s.hasKey) "已配置" else "未配置",
                    fontSize = Dimens2.Body,
                    color = c.secondary,
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
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
                    .defaultMinSize(minHeight = Dimens.RowH)
                    .padding(horizontal = Dimens.CardPad, vertical = Dimens.CardPadV),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    if (loadingModels) "拉取中…" else "拉取模型列表",
                    fontSize = 15.sp,
                    color = c.secondary,
                    lineHeight = (15.sp * 1.2),
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
                    AppIcons.Draw(
                    AppIcons.ChevronRight,
                    size = 18.dp,
                    contentDescription = null,
                        tint = c.tertiary,
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
                trailing = { AppIcons.Draw(AppIcons.ChevronRight, size = 18.dp, contentDescription = null, tint = c.tertiary) },
            )
            CardDivider()
            Cell(
                title = "我录入的食物",
                onClick = { showCustomFoods = true },
                titleColor = c.tint,
                value = "${s.customFoods.size} 项",
                trailing = { AppIcons.Draw(AppIcons.ChevronRight, size = 18.dp, contentDescription = null, tint = c.tertiary) },
            )
            CardDivider()
            // 检查更新
            Cell(
                title = "检查更新",
                onClick = {
                    if (checkingUpdate) return@Cell
                    checkingUpdate = true
                    updateManager.checkUpdate { releases, error ->
                        checkingUpdate = false
                        if (releases.isEmpty()) {
                            // 没有新版本（或查询失败）都提示，不再静默
                            snackbar(error?.let { "检查失败：$it" } ?: "已是最新版本")
                            return@checkUpdate
                        }
                        // 有可用版本，弹出让用户选
                        pendingReleases = releases
                        showUpdateDialog = true
                    }
                },
                titleColor = c.tint,
                trailing = {
                    if (checkingUpdate) {
                        androidx.compose.material3.CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = c.tint
                        )
                    } else {
                        AppIcons.Draw(AppIcons.ChevronRight, size = 18.dp, contentDescription = null, tint = c.tertiary)
                    }
                },
            )
            CardDivider()
            Cell(
                title = "清空全部数据",
                onClick = { showClear = true },
                titleColor = c.red,
                trailing = { AppIcons.Draw(AppIcons.ChevronRight, size = 18.dp, contentDescription = null, tint = c.tertiary) },
            )
        }

        Text(
            "过日子 v${appVersion()} · 数据保存在本机",
            fontSize = 12.sp,
            color = c.tertiary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 24.dp),
        )
    }

    // 外观三档（底部 action-sheet）
    if (showThemePicker) {
        SheetPicker(
            title = "外观",
            options = listOf(
                PickerOption("auto", "🌤 自动（跟随系统）", s.theme.isBlank() || s.theme == "auto"),
                PickerOption("light", "☀️ 浅色", s.theme == "light"),
                PickerOption("dark", "🌙 深色", s.theme == "dark"),
            ),
            onPick = { v ->
                repo.updateSettings { it.copy(theme = v) }
                ThemePrefs.theme.value = v
                showThemePicker = false
            },
            onDismiss = { showThemePicker = false },
        )
    }


    // 模型列表（底部 action-sheet）
    if (showModels) {
        SheetPicker(
            title = "选择模型",
            options = models.map { PickerOption(it, it, it == s.apiModel) },
            onPick = { m ->
                repo.updateSettings { it.copy(apiModel = m) }
                modelInput = m
                showModels = false
                snackbar("已选择 " + m)
            },
            onDismiss = { showModels = false },
        )
    }


    if (showCustomFoods) {
        CustomFoodDialog(repo, snackbar) { showCustomFoods = false }
    }

    if (showClear) {
        AppAlert(
            title = "清空全部数据？",
            message = "买菜记录和日记都会删除，无法恢复。",
            confirmText = "清空",
            dismissText = "取消",
            danger = true,
            onConfirm = {
                repo.clearAll()
                showClear = false
                snackbar("已清空")
            },
            onDismiss = { showClear = false },
        )
    }

    // 更新版本选择面板：正式版 / 测试版 都列出来，用户自选
    if (showUpdateDialog && pendingReleases.isNotEmpty()) {
        UpdateChooserSheet(
            releases = pendingReleases,
            currentVersion = appVersion(),
            onPick = { info ->
                showUpdateDialog = false
                pendingReleases = emptyList()
                // 弹出应用内进度弹窗
                downloadingVersion = info.displayVersion
                downloadPercent = 0
                updateManager.downloadAndInstall(
                    releaseInfo = info,
                    onProgress = { pct -> downloadPercent = pct },
                    onComplete = { success, msg ->
                        downloadingVersion = null
                        snackbar(msg)
                    }
                )
            },
            onDismiss = {
                showUpdateDialog = false
                pendingReleases = emptyList()
            },
        )
    }

    // 下载进度弹窗
    if (downloadingVersion != null) {
        DownloadProgressSheet(
            version = downloadingVersion!!,
            percent = downloadPercent,
        )
    }
}

/**
 * 下载进度弹窗：进度条 + 百分比，下载完成后自动消失并调起安装。
 */
@Composable
private fun DownloadProgressSheet(version: String, percent: Int) {
    val c = LocalAppColors.current
    androidx.compose.ui.window.Dialog(
        onDismissRequest = { /* 下载中不允许关闭 */ },
        properties = androidx.compose.ui.window.DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
    ) {
        androidx.compose.material3.Surface(
            shape = RoundedCornerShape(14.dp),
            color = c.bgElevated,
            modifier = Modifier.width(270.dp),
        ) {
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "正在下载 v$version",
                    fontSize = Dimens2.SheetTitle,
                    fontWeight = FontWeight.SemiBold,
                    color = c.label,
                )
                Spacer(Modifier.height(16.dp))
                // 进度条
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(c.fill),
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(percent / 100f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(c.tint),
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    "$percent%",
                    fontSize = Dimens2.Caption,
                    color = c.secondary,
                    style = NumberStyle,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "下载完成后将自动打开安装程序",
                    fontSize = 12.sp,
                    color = c.tertiary,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/**
 * 版本选择面板：列出正式版和测试版，让用户挑一个更新。
 * 测试版会明确标注「不建议更新」。
 */
@Composable
private fun UpdateChooserSheet(
    releases: List<UpdateManager.ReleaseInfo>,
    currentVersion: String,
    onPick: (UpdateManager.ReleaseInfo) -> Unit,
    onDismiss: () -> Unit,
) {
    val c = LocalAppColors.current

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        androidx.compose.material3.Surface(
            shape = RoundedCornerShape(14.dp),
            color = c.bgElevated,
            modifier = Modifier.width(300.dp),
        ) {
            Column(Modifier.fillMaxWidth()) {
                Text(
                    "发现新版本",
                    fontSize = Dimens2.SheetTitle,
                    fontWeight = FontWeight.SemiBold,
                    color = c.label,
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 18.dp, end = 16.dp, bottom = 10.dp),
                )
                Text(
                    "当前 v$currentVersion · 选择要安装的版本",
                    fontSize = Dimens2.Caption,
                    color = c.secondary,
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                )

                releases.forEachIndexed { i, r ->
                    if (i > 0) CardDivider(modifier = Modifier.padding(start = 0.dp))
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onPick(r) }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "v${r.displayVersion}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = c.label,
                            )
                            Spacer(Modifier.width(8.dp))
                            // 渠道标签
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (r.channel.recommend) c.tint.copy(alpha = 0.12f) else c.fill)
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                            ) {
                                Text(
                                    r.channel.label,
                                    fontSize = 11.sp,
                                    color = if (r.channel.recommend) c.tint else c.secondary,
                                )
                            }
                            Spacer(Modifier.weight(1f))
                            AppIcons.Draw(
                                AppIcons.ChevronRight,
                                size = 16.dp,
                                contentDescription = null,
                                tint = c.tertiary,
                            )
                        }
                        // 测试版明确标注不建议
                        if (!r.channel.recommend) {
                            Text(
                                "测试版，不建议更新",
                                fontSize = 12.sp,
                                color = c.orange,
                                modifier = Modifier.padding(top = 3.dp),
                            )
                        }
                        if (r.body.isNotBlank()) {
                            Text(
                                r.body.take(80),
                                fontSize = 12.sp,
                                color = c.secondary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                lineHeight = 16.sp,
                                modifier = Modifier.padding(top = 3.dp),
                            )
                        }
                    }
                }

                HorizontalDivider(thickness = 0.5.dp, color = c.separator)
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onDismiss() }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("稍后再说", fontSize = Dimens2.SheetTitle, color = c.tint)
                }
            }
        }
    }
}

/** 读取 App 真实版本号（避免硬编码忘记更新） */
@Composable
fun appVersion(): String {
    val ctx = LocalContext.current
    return remember(ctx) {
        runCatching {
            ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: "?"
        }.getOrDefault("?")
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
        lineHeight = (Dimens2.Hint.value * 1.3).sp,
        modifier = modifier.fillMaxWidth().padding(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 12.dp),
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
            textStyle = TextStyle(
                fontSize = Dimens2.Body,
                color = c.label,
                textAlign = TextAlign.End,
                lineHeight = (Dimens2.Body.value * 1.2).sp,
            ),
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
                            lineHeight = (Dimens2.Body.value * 1.2).sp,
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
        modifier = Modifier.width(270.dp),
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

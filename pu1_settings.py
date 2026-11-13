import io
import os

BASE = r"C:\Users\一只屑\Desktop\HousewifeAppProto\android-native\app\src\main\java\com\guoziri\app"


def read(name):
    with io.open(os.path.join(BASE, name), encoding="utf-8") as f:
        return f.read()


def write(name, content):
    with io.open(os.path.join(BASE, name), "w", encoding="utf-8", newline="\n") as f:
        f.write(content)
    print("written:", name)


def sub(src, old, new, label):
    assert old in src, "NOT FOUND: " + label
    return src.replace(old, new, 1)


src = read("SettingsPage.kt")

# --- 导入 ---
src = sub(
    src,
    "import androidx.compose.material3.*\n",
    "import androidx.compose.material.icons.Icons\nimport androidx.compose.material.icons.rounded.*\nimport androidx.compose.material3.*\n",
    "SettingsPage icons import",
)

# --- 标题 ---
old = """        Text("设置", fontSize = 34.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 20.dp, top = 12.dp, bottom = 6.dp))
"""
new = """        PageTitle("设置", subtitle = "过日子 v1.0 · 数据只保存在这台手机上")
"""
src = sub(src, old, new, "SettingsPage title")

# --- AI 服务商：分组标题 + 卡片内边距 ---
old = """        GroupTitle("AI 服务商")
        GroupCard {
            PRESETS.forEach { preset ->
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
                        snackbar("已切换")
                    }
                )
            }
        }
        Text("切换服务商自动带出地址和默认模型，Key 各自独立保存", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), modifier = Modifier.padding(start = 20.dp, top = 8.dp, end = 20.dp))
"""
new = """        GroupTitle("AI 服务商")
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
                    }
                )
            }
        }
"""
src = sub(src, old, new, "SettingsPage provider list")

# --- 当前服务配置 ---
old = """        GroupTitle("当前服务配置")
        GroupCard(Modifier.guideAnchor(anchors, "settings-key", "settings")) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("API 状态", fontSize = 16.sp)
                    Text(if (s.hasKey) "● 已配置" else "○ 未配置", fontSize = 13.sp, color = if (s.hasKey) IosGreen else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                }
                Text(s.apiBaseUrl.ifBlank { "未设置" }, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            OutlinedTextField(
                value = keyInput,
                onValueChange = { keyInput = it },
                label = { Text("API Key") },
                placeholder = { Text(if (s.hasKey) "已保存（输入可覆盖）" else "sk-…") },
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
                }, modifier = Modifier.padding(vertical = 8.dp)) {
                    Text("保存", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        Text("Key 只存在本机数据目录，不上传、不备份、不写日志",
            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
"""
new = """        GroupTitle("当前服务配置")
        GroupCard(Modifier.padding(horizontal = Dimens.PageH).guideAnchor(anchors, "settings-key", "settings")) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = Dimens.CardPad, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.size(8.dp).clip(androidx.compose.foundation.shape.CircleShape)
                        .background(if (s.hasKey) IosGreen else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
                )
                Spacer(Modifier.width(9.dp))
                Text(
                    if (s.hasKey) "已配置" else "未配置",
                    fontSize = Dimens2.Body,
                    modifier = Modifier.weight(1f),
                )
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
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 15.sp),
                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                shape = RoundedCornerShape(10.dp),
            )
            OutlinedTextField(
                value = modelInput,
                onValueChange = { modelInput = it },
                label = { Text("模型", fontSize = 12.sp) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = Dimens.CardPad, vertical = 4.dp),
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 15.sp),
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
"""
src = sub(src, old, new, "SettingsPage key section")

# --- 通用 / 外观 / 数据 / 帮助 分组 ---
old = """        GroupTitle("通用")
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
            ExportCell(onClick = { snackbar("导出功能开发中") })
            ClearCell(onClick = { showClear = true })
        }

        GroupTitle("帮助")
        GroupCard {
            ReplayGuideCell(onClick = { onGuideCellClick() })
            CustomFoodCell(repo = repo, snackbar = snackbar)
        }

        Text("过日子 v1.0-native · 数据保存在本机",
            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp), textAlign = TextAlign.Center
        )
    }"""
new = """        GroupTitle("通用")
        GroupCard(Modifier.padding(horizontal = Dimens.PageH)) {
            SettingRow(
                icon = Icons.Rounded.Sort,
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
                icon = Icons.Rounded.MenuBook,
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
                    "light" -> Icons.Rounded.LightMode
                    "dark" -> Icons.Rounded.DarkMode
                    else -> Icons.Rounded.BrightnessAuto
                },
                title = "主题",
                subtitle = when (s.theme) {
                    "light" -> "浅色"
                    "dark" -> "深色"
                    else -> "跟随系统"
                },
                trailing = {
                    Icon(
                        Icons.Rounded.ChevronRight,
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
                icon = Icons.Rounded.FileUpload,
                title = "导出备份",
                subtitle = "把数据导出成 JSON 文件",
                trailing = {
                    Icon(
                        Icons.Rounded.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
                        modifier = Modifier.size(18.dp),
                    )
                },
                onClick = { snackbar("导出功能开发中") },
            )
            CardDivider()
            SettingRow(
                icon = Icons.Rounded.DeleteOutline,
                title = "清空全部数据",
                subtitle = "删除所有买菜记录和日记",
                iconTint = IosRed,
                onClick = { showClear = true },
            )
        }

        GroupTitle("帮助")
        GroupCard(Modifier.padding(horizontal = Dimens.PageH)) {
            SettingRow(
                icon = Icons.Rounded.Replay,
                title = "重播新手引导",
                subtitle = "再看一遍使用教程",
                trailing = {
                    Icon(
                        Icons.Rounded.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
                        modifier = Modifier.size(18.dp),
                    )
                },
                onClick = onReplayGuide,
            )
            CardDivider()
            SettingRow(
                icon = Icons.Rounded.BookmarkBorder,
                title = "我录入的食物",
                subtitle = if (repo.settings.customFoods.isEmpty()) "还没有录入过"
                else "共 ${repo.settings.customFoods.size} 种",
                trailing = {
                    Icon(
                        Icons.Rounded.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
                        modifier = Modifier.size(18.dp),
                    )
                },
                onClick = { showCustomFoods = true },
            )
        }

        Spacer(Modifier.height(30.dp))
    }"""
src = sub(src, old, new, "SettingsPage groups")

# 主题选择器：去掉 emoji
old = """                    listOf("auto" to "🌤 自动（跟随系统）", "light" to "☀️ 浅色", "dark" to "🌙 深色").forEach { (v, label) ->
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
                        )
                    }"""
new = """                    listOf(
                        "auto" to ("跟随系统" to Icons.Rounded.BrightnessAuto),
                        "light" to ("浅色" to Icons.Rounded.LightMode),
                        "dark" to ("深色" to Icons.Rounded.DarkMode),
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
                            Text(
                                label,
                                fontSize = Dimens2.Body,
                                modifier = Modifier.weight(1f),
                            )
                            if (s.theme == v) {
                                Icon(
                                    Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = IosBlue,
                                    modifier = Modifier.size(19.dp),
                                )
                            }
                        }
                    }"""
src = sub(src, old, new, "SettingsPage theme picker")

# PresetCell：加图标
old = """@Composable
fun PresetCell(preset: Preset, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(preset.label, fontSize = 16.sp, modifier = Modifier.weight(1f))
        if (isSelected) Text("✓", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = IosBlue)
    }
}"""
new = """@Composable
fun PresetCell(preset: Preset, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = Dimens.CardPad, vertical = 12.dp),
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
}"""
src = sub(src, old, new, "SettingsPage PresetCell")

# 替换底部的 cell 组件
old = """@Composable
fun ReplayGuideCell(onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("重播新手引导", fontSize = 16.sp, color = IosBlue, modifier = Modifier.weight(1f))
        Text("›", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
    }
}

@Composable
fun CustomFoodCell(repo: StoreRepo, snackbar: (String) -> Unit) {
    var showList by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth().clickable { showList = true }.padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("我录入的食物", fontSize = 16.sp, modifier = Modifier.weight(1f))
        Text("${repo.settings.customFoods.size}", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f))
        Text("›", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
    }
    if (showList) {
        AlertDialog(
            onDismissRequest = { showList = false },
            title = { Text("我录入的食物") },
            text = {
                Column {
                    if (repo.settings.customFoods.isEmpty()) {
                        Text("还没有。记账时输入食材库外的名字，就能手动录营养。",
                            fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    } else {
                        repo.settings.customFoods.forEach { f ->
                            Row(
                                Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(f.name, fontSize = 15.sp)
                                    Text("${f.category} · ${f.calories.toInt()} kcal/100g",
                                        fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
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
            confirmButton = { TextButton(onClick = { showList = false }) { Text("关闭") } },
        )
    }
}"""
new = """@Composable
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
}"""
src = sub(src, old, new, "SettingsPage cells")

# 状态变量
src = sub(
    src,
    '    var showClear by remember { mutableStateOf(false) }',
    '    var showClear by remember { mutableStateOf(false) }\n    var showCustomFoods by remember { mutableStateOf(false) }',
    "SettingsPage state",
)

# 自定义食物弹窗
src = sub(
    src,
    """    if (showClear) {""",
    """    if (showCustomFoods) {
        CustomFoodDialog(repo, snackbar) { showCustomFoods = false }
    }

    if (showClear) {""",
    "SettingsPage custom food dialog",
)

write("SettingsPage.kt", src)
print("SETTINGS PAGE REWRITTEN")

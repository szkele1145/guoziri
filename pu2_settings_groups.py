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
            ReplayGuideCell(onClick = { onReplayGuide() })
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
write("SettingsPage.kt", src)
print("OK")

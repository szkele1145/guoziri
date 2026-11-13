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


# =====================================================================
# LedgerPage: 加 anchors 参数
# =====================================================================
src = read("LedgerPage.kt")
src = sub(
    src,
    "fun LedgerPage(repo: StoreRepo, onAdd: () -> Unit, onEdit: (Purchase) -> Unit, onRequestDelete: (Purchase) -> Unit) {",
    "fun LedgerPage(\n    repo: StoreRepo,\n    anchors: GuideAnchors,\n    onAdd: () -> Unit,\n    onEdit: (Purchase) -> Unit,\n    onRequestDelete: (Purchase) -> Unit,\n) {",
    "LedgerPage anchors param",
)
write("LedgerPage.kt", src)

# =====================================================================
# NutritionPage: 加 anchors 参数 + 圆环锚点
# =====================================================================
src = read("NutritionPage.kt")
src = sub(
    src,
    "fun NutritionPage(repo: StoreRepo, date: String) {",
    "fun NutritionPage(repo: StoreRepo, date: String, anchors: GuideAnchors) {",
    "NutritionPage anchors param",
)
src = sub(
    src,
    '                Box(Modifier.size(96.dp), contentAlignment = Alignment.Center) {',
    '                Box(Modifier.size(96.dp).guideAnchor(anchors, "nutrition-ring"), contentAlignment = Alignment.Center) {',
    "NutritionPage ring anchor",
)
write("NutritionPage.kt", src)

# =====================================================================
# AiPage: 加 anchors 参数 + 快捷卡片锚点
# =====================================================================
src = read("AiPage.kt")
src = sub(
    src,
    "fun AiPage(repo: StoreRepo, chatState: ChatState, snackbar: (String) -> Unit) {",
    "fun AiPage(repo: StoreRepo, chatState: ChatState, anchors: GuideAnchors, snackbar: (String) -> Unit) {",
    "AiPage anchors param",
)
src = sub(
    src,
    '            AiCard("💰", Color(0xFFE8F0FE), "总结今天花销", "值不值、哪儿能省", enabled = hasP) {',
    '            AiCard("💰", Color(0xFFE8F0FE), "总结今天花销", "值不值、哪儿能省", enabled = hasP, anchorId = "ai-quick", anchors = anchors) {',
    "AiPage quick anchor",
)
src = sub(
    src,
    "fun AiCard(icon: String, bg: Color, title: String, sub: String, enabled: Boolean, onClick: () -> Unit) {",
    "fun AiCard(\n    icon: String,\n    bg: Color,\n    title: String,\n    sub: String,\n    enabled: Boolean,\n    anchorId: String? = null,\n    anchors: GuideAnchors? = null,\n    onClick: () -> Unit,\n) {",
    "AiPage AiCard signature",
)
src = sub(
    src,
    """    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
            .background(if (enabled) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surface.copy(alpha = 0.45f))
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {""",
    """    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
            .background(if (enabled) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surface.copy(alpha = 0.45f))
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .let { m -> if (anchorId != null && anchors != null) m.guideAnchor(anchors, anchorId) else m },
    ) {""",
    "AiPage AiCard anchor modifier",
)
write("AiPage.kt", src)

# =====================================================================
# SettingsPage: 加 anchors 参数 + Key 区锚点 + 重播引导入口
# =====================================================================
src = read("SettingsPage.kt")
src = sub(
    src,
    "fun SettingsPage(repo: StoreRepo, snackbar: (String) -> Unit, onReplayGuide: () -> Unit) {",
    "fun SettingsPage(\n    repo: StoreRepo,\n    snackbar: (String) -> Unit,\n    anchors: GuideAnchors,\n    onReplayGuide: () -> Unit,\n) {",
    "SettingsPage anchors param",
)
src = sub(
    src,
    '        GroupTitle("当前服务配置")\n        GroupCard {',
    '        GroupTitle("当前服务配置")\n        GroupCard(Modifier.guideAnchor(anchors, "settings-key")) {',
    "SettingsPage key anchor",
)
# 数据组里加「重播新手引导」
src = sub(
    src,
    """        GroupTitle("数据")
        GroupCard {
            ExportCell(onClick = { snackbar("导出功能开发中") })
            ClearCell(onClick = { showClear = true })
        }""",
    """        GroupTitle("数据")
        GroupCard {
            ExportCell(onClick = { snackbar("导出功能开发中") })
            ClearCell(onClick = { showClear = true })
        }

        GroupTitle("帮助")
        GroupCard {
            ReplayGuideCell(onClick = { onReplayGuide() })
            CustomFoodCell(repo = repo, snackbar = snackbar)
        }""",
    "SettingsPage replay guide cell",
)
# 追加两个 cell 组件
src += """

@Composable
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
}
"""
write("SettingsPage.kt", src)

# =====================================================================
# PurchaseSheet: 存储按钮锚点
# =====================================================================
src = read("PurchaseSheet.kt")
src = sub(
    src,
    """                }) { Text("存储", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = IosBlue) }""",
    """                }, modifier = Modifier.guideAnchor(anchors, "purchase-save")) {
                    Text("存储", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = IosBlue)
                }""",
    "PurchaseSheet save anchor",
)
src = sub(
    src,
    """fun PurchaseSheet(
    repo: StoreRepo,
    editing: Purchase?,                 // null = 新增
    onDismiss: () -> Unit,
    snackbar: (String) -> Unit,
) {""",
    """fun PurchaseSheet(
    repo: StoreRepo,
    editing: Purchase?,                 // null = 新增
    anchors: GuideAnchors,
    onDismiss: () -> Unit,
    snackbar: (String) -> Unit,
) {""",
    "PurchaseSheet anchors param",
)
write("PurchaseSheet.kt", src)

print("PAGE ANCHORS PATCHED")

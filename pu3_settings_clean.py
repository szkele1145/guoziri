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

# --- 替换 CustomFoodCell 为弹窗版 ---
old = """@Composable
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
src = sub(src, old, new, "SettingsPage CustomFoodCell")

# --- 移除旧的 SwitchRow / ExportCell / ClearCell ---
for fn in ["SwitchRow", "ExportCell", "ClearCell"]:
    marker = "@Composable\nfun %s(" % fn
    i = src.find(marker)
    if i >= 0:
        # 找到该函数的结束（下一个 @Composable 或文件尾）
        j = src.find("\n@Composable", i + 10)
        if j < 0:
            j = len(src)
        else:
            j += 1
        src = src[:i] + src[j:]

# --- 状态 + 弹窗挂载 ---
src = sub(
    src,
    '    var showClear by remember { mutableStateOf(false) }',
    '    var showClear by remember { mutableStateOf(false) }\n    var showCustomFoods by remember { mutableStateOf(false) }',
    "SettingsPage state",
)
src = sub(
    src,
    "    if (showClear) {",
    """    if (showCustomFoods) {
        CustomFoodDialog(repo, snackbar) { showCustomFoods = false }
    }

    if (showClear) {""",
    "SettingsPage custom food dialog mount",
)

write("SettingsPage.kt", src)
print("OK")

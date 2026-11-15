# -*- coding: utf-8 -*-
"""把三处选择器改成底部 action-sheet（对齐 HTML #action-sheet）"""
import io
import os

BASE = r"C:\Users\一只屑\Desktop\HousewifeAppProto\android-native\app\src\main\java\com\guoziri\app"


def read_lines(name):
    p = os.path.join(BASE, name)
    with io.open(p, encoding="utf-8") as f:
        return p, f.read().split("\n")


def write_lines(p, lines):
    with io.open(p, "w", encoding="utf-8", newline="\n") as f:
        f.write("\n".join(lines))


# ---------- PurchaseSheet：分类选择 ----------
p, L = read_lines("PurchaseSheet.kt")
start = next(i for i, l in enumerate(L) if l.strip() == "// 分类选择")
end = next(i for i, l in enumerate(L) if "sheet 内 0.5px 分隔线" in l)
new_block = '''    // 分类选择（底部 action-sheet，对齐 web 版 #action-sheet）
    if (showCatPicker) {
        SheetPicker(
            title = "选择分类",
            options = CATEGORIES.map { PickerOption(it, it, it == category) },
            onPick = { category = it; showCatPicker = false },
            onDismiss = { showCatPicker = false },
        )
    }
}

'''.split("\n")
L = L[:start] + new_block + L[end:]
write_lines(p, L)
print("PurchaseSheet: 分类选择改为底部面板")

# ---------- SettingsPage：外观 + 模型 ----------
p, L = read_lines("SettingsPage.kt")
s1 = next(i for i, l in enumerate(L) if l.strip() == "// 外观三档")
s2 = next(i for i, l in enumerate(L) if l.strip() == "// 模型列表")
s3 = next(i for i, l in enumerate(L) if "if (showCustomFoods)" in l)

theme_block = '''    // 外观三档（底部 action-sheet）
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

'''.split("\n")

model_block = '''    // 模型列表（底部 action-sheet）
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

'''.split("\n")

L = L[:s1] + theme_block + model_block + L[s3:]
write_lines(p, L)
print("SettingsPage: 外观/模型选择改为底部面板")

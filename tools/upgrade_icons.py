# -*- coding: utf-8 -*-
"""把所有 Icon(AppIcons.X, ...) 改成 AppIcons.Draw(AppIcons.X, ..., size = ...)"""
import io
import os
import re

BASE = r"C:\Users\一只屑\Desktop\HousewifeAppProto\android-native\app\src\main\java\com\guoziri\app"

# 每个调用点期望的图标边长
SIZES = {
    "AppRoot.kt": {"AppIcons.Add": 26, "tabIconVector": 22},
    "Common.kt": {"icon": 18, "AppIcons.ChevronRight": 18},
    "SheetParts.kt": {"AppIcons.ChevronRight": 18},
    "AiPage.kt": {"AppIcons.Add": 21, "icon": 21},
    "SettingsPage.kt": {"AppIcons.ChevronRight": 18},
    "Chat.kt": {"AppIcons.Send": 18},
    "CalendarCard.kt": {"icon": 22},
    "NutritionPage.kt": {"AppIcons.ChevronLeft": 18, "AppIcons.ChevronRight": 18},
    "DiaryPage.kt": {"AppIcons.Add": 22},
    "SplashScreen.kt": {"AppIcons.Leaf": 40},
}

# 简单的多行 Icon( 替换：把 Icon( 换成 AppIcons.Draw( 并在末尾插入 size
PATTERN = re.compile(r"Icon\(\s*(AppIcons\.[A-Za-z0-9_]+|icon|tabIconVector\([^)]*\))\s*,\s*", re.M)


def process(fname):
    path = os.path.join(BASE, fname)
    if not os.path.exists(path):
        return
    with io.open(path, encoding="utf-8") as f:
        src = f.read()
    if "AppIcons.Draw(" in src:
        return

    out = []
    idx = 0
    changed = 0
    for m in PATTERN.finditer(src):
        name = m.group(1)
        key = name
        if key not in SIZES.get(fname, {}):
            # 尝试用前缀匹配
            for k in SIZES.get(fname, {}):
                if name.startswith(k):
                    key = k
                    break
            else:
                key = None
        if key is None:
            continue
        size = SIZES[fname][key]
        out.append(src[idx:m.start()])
        out.append("AppIcons.Draw(\n                    %s,\n                    size = %d.dp,\n                    " % (name, size))
        idx = m.end()
        changed += 1
    out.append(src[idx:])
    if changed:
        result = "".join(out)
        # 补 import
        if "import androidx.compose.ui.unit.dp" not in result:
            result = result.replace(
                "import androidx.compose.runtime.*",
                "import androidx.compose.runtime.*\nimport androidx.compose.ui.unit.dp", 1)
        with io.open(path, "w", encoding="utf-8", newline="\n") as f:
            f.write(result)
        print("  %s: 替换 %d 处" % (fname, changed))


for fn in sorted(os.listdir(BASE)):
    if fn.endswith(".kt"):
        process(fn)
print("完成")

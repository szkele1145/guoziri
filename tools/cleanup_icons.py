# -*- coding: utf-8 -*-
"""清理批量替换后的图标调用，并替换剩余的 Icon( 调用"""
import io
import os
import re

BASE = r"C:\Users\一只屑\Desktop\HousewifeAppProto\android-native\app\src\main\java\com\guoziri\app"

# 清理多行调用的缩进
MULTI = re.compile(
    r"AppIcons\.Draw\(\s*\n\s*([^\n]+?),\s*\n\s*size = (\d+)\.dp,\s*\n\s*([^\n]*?)\)",
    re.S
)

REMAINING = {
    "AiPage.kt": [("AppIcons.ChevronRight", 18)],
    "DiaryPage.kt": [("AppIcons.Add", 22)],
    "NutritionPage.kt": [("AppIcons.ChevronLeft", 18), ("AppIcons.ChevronRight", 18)],
}


def cleanup(fname):
    path = os.path.join(BASE, fname)
    with io.open(path, encoding="utf-8") as f:
        src = f.read()

    # 1. 把 AppIcons.Draw(\n  X,\n  size = N.dp,\n  <rest>) 合成一行
    def repl(m):
        name = m.group(1).strip()
        size = m.group(2)
        rest = m.group(3).strip()
        # 移除 rest 里重复的 modifier = Modifier.size(N.dp)
        rest = re.sub(r"modifier\s*=\s*Modifier\.size\(\d+\.dp\)", "", rest)
        rest = rest.strip().rstrip(",")
        parts = ["%s," % name, "size = %s.dp" % size]
        if rest:
            parts.append(rest.rstrip(","))
        return "AppIcons.Draw(%s)" % ", ".join(parts)

    src = MULTI.sub(repl, src)

    # 2. 剩余的 Icon( AppIcons.X, ... ) 改成 AppIcons.Draw
    for icon, size in REMAINING.get(fname, []):
        pat = re.compile(
            r"Icon\(\s*" + re.escape(icon) + r",\s*contentDescription\s*=\s*null,\s*tint\s*=\s*([^,]+),\s*modifier\s*=\s*Modifier\.size\(\d+\.dp\),\s*\)",
            re.S)
        src = pat.sub(
            lambda m: "AppIcons.Draw(%s, size = %d.dp, tint = %s)" % (icon, size, m.group(1).strip()),
            src)

    with io.open(path, "w", encoding="utf-8", newline="\n") as f:
        f.write(src)
    print("  %s 已清理" % fname)


for fn in sorted(os.listdir(BASE)):
    if fn.endswith(".kt"):
        cleanup(fn)
print("完成")

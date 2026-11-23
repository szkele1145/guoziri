# -*- coding: utf-8 -*-
"""清理批量替换遗留的语法问题：双逗号、重复 modifier、多余括号"""
import io
import os
import re

BASE = r"C:\Users\一只屑\Desktop\HousewifeAppProto\android-native\app\src\main\java\com\guoziri\app"

# 1. 单行：AppIcons.Draw(X,, size = N.dp, ...) -> 去掉双逗号
ONE_LINE = re.compile(r"AppIcons\.Draw\(([^,()]+),,\s*")

# 2. 单行：去掉 modifier = Modifier.size(...) （Draw 内部已按 size 计算）
MODIFIER_SIZE = re.compile(r",\s*modifier\s*=\s*Modifier\.size\(\d+\.dp\)")

# 3. 单行：AppIcons.Draw(X, size = N.dp, null, tint = ...) -> contentDescription = null
NULL_ARG = re.compile(r"(AppIcons\.Draw\([^)]*?size\s*=\s*[\d.]+\.dp,\s*)null,")


def fix(fname):
    path = os.path.join(BASE, fname)
    if not os.path.exists(path):
        return False
    with io.open(path, encoding="utf-8") as f:
        src = f.read()
    orig = src

    # 处理单行
    src = NULL_ARG.sub(r"\1contentDescription = null,", src)
    src = ONE_LINE.sub(r"AppIcons.Draw(\1, ", src)
    src = MODIFIER_SIZE.sub("", src)

    # 处理多行形式：
    # AppIcons.Draw(
    #     X,
    #     size = N.dp,
    #     contentDescription = ...,
    #     tint = ...,
    #     modifier = Modifier.size(N.dp),      <-- 去掉
    # )
    src = re.sub(
        r"(AppIcons\.Draw\(\s*\n\s*[^,]+,\s*\n\s*size\s*=\s*[\d.]+\.dp,\s*\n(?:.*\n)*?\s*tint\s*=\s*[^,\n]+,)\s*\n\s*modifier\s*=\s*Modifier\.size\([\d.]+\.dp\),\s*\n",
        r"\1\n",
        src,
    )
    # 多行结尾多余的 modifier（没有逗号结尾）
    src = re.sub(
        r"(AppIcons\.Draw\(\s*\n\s*[^,]+,\s*\n\s*size\s*=\s*[\d.]+\.dp,\s*\n(?:.*\n)*?\s*tint\s*=\s*[^,\n]+,)\s*\n\s*modifier\s*=\s*Modifier\.size\([\d.]+\.dp\)\s*\n",
        r"\1\n",
        src,
    )

    if src != orig:
        with io.open(path, "w", encoding="utf-8", newline="\n") as f:
            f.write(src)
        print("  %s 已清理" % fname)
        return True
    return False


for fn in sorted(os.listdir(BASE)):
    if fn.endswith(".kt"):
        fix(fn)
print("完成")

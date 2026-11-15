# -*- coding: utf-8 -*-
"""修正全局行高：CSS 的 min-height 是 content-box，需叠加 padding"""
import io
import os
import re

BASE = r"C:\Users\一只屑\Desktop\HousewifeAppProto\android-native\app\src\main\java\com\guoziri\app"


def load(name):
    p = os.path.join(BASE, name)
    with io.open(p, encoding="utf-8") as f:
        return p, f.read()


def save(p, t):
    with io.open(p, "w", encoding="utf-8", newline="\n") as f:
        f.write(t)


# 1) Dimens.RowH：52 -> 68（.cell min-height 52 是 content-box，含 padding 8*2）
p, t = load("Common.kt")
t2 = t.replace(
    """    /** .cell min-height 52 */
    val RowH = 52.dp""",
    """    /** .cell：min-height 52 是 content-box，含上下 padding 8 后总高 68 */
    val RowH = 68.dp""")
if t2 == t:
    # 兼容不同注释
    t2 = t.replace("val RowH = 52.dp", "val RowH = 68.dp")
if t2 != t:
    save(p, t2)
    print("  Common.kt: Dimens.RowH 52 -> 68")

# 2) BarRow：.bar-row min-height 44 + padding 4*2 = 52
p, t = load("Common.kt")
t2 = t.replace(
    """            .defaultMinSize(minHeight = 44.dp)
            .padding(horizontal = Dimens.CardPad, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),""",
    """            .defaultMinSize(minHeight = 52.dp)   // 44 + padding 4*2
            .padding(horizontal = Dimens.CardPad, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),""")
if t2 != t:
    save(p, t2)
    print("  Common.kt: BarRow 44 -> 52")

# 3) 记账页合计行：min-height 40 + padding 8*2 = 56
p, t = load("LedgerPage.kt")
t2 = t.replace(
    "                .defaultMinSize(minHeight = 40.dp)",
    "                .defaultMinSize(minHeight = 56.dp)   // 40 + padding 8*2")
if t2 != t:
    save(p, t2)
    print("  LedgerPage.kt: 合计行 40 -> 56")

# 4) 营养页未计入行：min-height 44 + padding 8*2 = 60
p, t = load("NutritionPage.kt")
n = t.count("                                .defaultMinSize(minHeight = 44.dp)")
t2 = t.replace(
    "                                .defaultMinSize(minHeight = 44.dp)",
    "                                .defaultMinSize(minHeight = 60.dp)")
n2 = t2.count("                            .defaultMinSize(minHeight = 44.dp)")
t2 = t2.replace(
    "                            .defaultMinSize(minHeight = 44.dp)",
    "                            .defaultMinSize(minHeight = 60.dp)")
if t2 != t:
    save(p, t2)
    print("  NutritionPage.kt: 未计入行 44 -> 60（%d 处）" % (n + n2))

print("完成")

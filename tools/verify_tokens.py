# -*- coding: utf-8 -*-
"""核对：Kotlin 设计令牌 vs web/style.css 实际值"""
import io
import os
import re

BASE = r"C:\Users\一只屑\Desktop\HousewifeAppProto"
CSS = os.path.join(BASE, "web", "style.css")
COMMON = os.path.join(BASE, "android-native", "app", "src", "main", "java", "com", "guoziri", "app", "Common.kt")

css = io.open(CSS, encoding="utf-8").read()
css = re.sub(r"/\*.*?\*/", "", css, flags=re.S)
common = io.open(COMMON, encoding="utf-8").read()


def css_var(name, block=0):
    """取 :root 或 dark 块里的变量值"""
    blocks = re.findall(r":root\s*\{(.*?)\}", css, re.S)
    if block >= len(blocks):
        return None
    m = re.search(re.escape(name) + r"\s*:\s*([^;]+);", blocks[block])
    return m.group(1).strip() if m else None


def css_prop(selector, prop):
    """取某选择器某属性的第一个值"""
    for m in re.finditer(r"([^{}]+)\{([^{}]*)\}", css):
        sel = m.group(1).strip()
        if sel == selector:
            pm = re.search(re.escape(prop) + r"\s*:\s*([^;]+);", m.group(2))
            if pm:
                return pm.group(1).strip()
    return None


def kotlin_val(obj, name):
    m = re.search(re.escape(obj) + r"\s*\{([\s\S]*?)\n\}", common)
    if not m:
        return None
    body = m.group(1)
    vm = re.search(r"val\s+" + re.escape(name) + r"\s*=\s*([\d.]+)\.(dp|sp)", body)
    return (float(vm.group(1)), vm.group(2)) if vm else None


def norm(v):
    """'16px' -> 16.0 ; '0.5px' -> 0.5"""
    if v is None:
        return None
    m = re.match(r"([\d.]+)\s*px", v)
    return float(m.group(1)) if m else v


rows = []


def chk(label, css_v, kt_obj, kt_name, unit="dp"):
    kt = kotlin_val(kt_obj, kt_name)
    exp = norm(css_v)
    if kt is None:
        rows.append((label, css_v, "缺失", "✗"))
        return
    kt_val, kt_unit = kt
    ok = (exp is not None and abs(kt_val - exp) < 0.01)
    rows.append((label, "%s" % css_v, "%s %s" % (kt_val, kt_unit), "✓" if ok else "✗"))


# ---- 令牌 ----
chk("卡片圆角 --radius-card", css_var("--radius-card"), "Dimens", "CardR")
chk("图标圆角 --radius-icon", css_var("--radius-icon"), "Dimens", "IconR")
chk("底部栏高 --tab-h", css_var("--tab-h"), "Dimens", "TabH")
chk("页面左右 .page", css_prop(".page", "padding").split()[1] if css_prop(".page", "padding") else None, "Dimens", "PageH")
chk("导航左右 .nav", css_prop(".nav", "padding").split()[1] if css_prop(".nav", "padding") else None, "Dimens", "NavH")
chk("单元格最小高 .cell", css_prop(".cell, .ios-cell", "min-height") or "52px", "Dimens", "RowH")
chk("单元格内边距 .cell", (css_prop(".cell, .ios-cell", "padding") or "8px 16px").split()[0], "Dimens", "CardPadV")
chk("单元格横向 .cell", (css_prop(".cell, .ios-cell", "padding") or "8px 16px").split()[1], "Dimens", "CardPad")
chk("单元格间距 .cell", css_prop(".cell, .ios-cell", "gap") or "12px", "Dimens", "CellGap")
chk("分组标题上距 .group-title", (css_prop(".group-title", "margin-top") or "18px"), "Dimens", "GroupTop")
chk("分组标题下距 .group-title", (css_prop(".group-title", "padding") or "0 16px 6px").split()[2], "Dimens", "GroupBottom")
chk("图标底座 .cell-icon", (css_prop(".cell-icon", "width") or "30px"), "Dimens", "IconTile")
chk("FAB 尺寸 .fab", (css_prop(".fab", "width") or "54px"), "Dimens", "Fab")
chk("FAB 右边距 .fab", (css_prop(".fab", "right") or "20px"), "Dimens", "FabRight")

# ---- 字号 ----
chk("页面大标题 .nav-title", (css_prop(".nav-title", "font-size") or "34px"), "Dimens2", "Title", "sp")
chk("正文 .cell-title", (css_prop(".cell-title", "font-size") or "17px"), "Dimens2", "Body", "sp")
chk("副文 .cell-sub", (css_prop(".cell-sub", "font-size") or "13px"), "Dimens2", "Caption", "sp")
chk("分组标题 .group-title", (css_prop(".group-title", "font-size") or "13px"), "Dimens2", "Label", "sp")
chk("统计数值 .stat .v", (css_prop(".stat .v", "font-size") or "19px"), "Dimens2", "StatValue", "sp")
chk("统计标签 .stat .l", (css_prop(".stat .l", "font-size") or "11px"), "Dimens2", "StatLabel", "sp")
chk("环内百分比 .ring-center .pct", (css_prop(".ring-center .pct", "font-size") or "22px"), "Dimens2", "RingPct", "sp")
chk("环内说明 .ring-center .cap", (css_prop(".ring-center .cap", "font-size") or "10px"), "Dimens2", "RingCap", "sp")
chk("达标条标签 .b-label", (css_prop(".bar-row .b-label", "font-size") or "15px"), "Dimens2", "BarLabel", "sp")
chk("达标条数值 .b-val", (css_prop(".bar-row .b-val", "font-size") or "12px"), "Dimens2", "BarValue", "sp")
chk("日历标题 .cal-title", (css_prop(".cal-title", "font-size") or "16px"), "Dimens2", "CalTitle", "sp")
chk("日历星期 .cal-wd", (css_prop(".cal-wd", "font-size") or "11px"), "Dimens2", "CalWd", "sp")
chk("日历日号 .cal-day", (css_prop(".cal-day", "font-size") or "15px"), "Dimens2", "CalDay", "sp")
chk("聊天气泡 .msg", (css_prop(".msg", "font-size") or "16px"), "Dimens2", "Msg", "sp")
chk("弹窗标题 .sheet-title", (css_prop(".sheet-title", "font-size") or "17px"), "Dimens2", "SheetTitle", "sp")
chk("提示文字 .hint", (css_prop(".hint", "font-size") or "12px"), "Dimens2", "Hint", "sp")
chk("空态文字 .empty", (css_prop(".empty", "font-size") or "15px"), "Dimens2", "Empty", "sp")
chk("步进数值 .st-val", (css_prop(".stepper .st-val", "font-size") or "17px"), "Dimens2", "StepperVal", "sp")
chk("Tab 文字 .tab-item span", (css_prop(".tab-item span", "font-size") or "9px"), "Dimens2", "TabLabel", "sp")
chk("AI 卡图标 .ai-card .cell-icon", (css_prop(".ai-card .cell-icon", "font-size") or "19px"), "Dimens2", "AiCardIcon", "sp")

print("%-34s %-14s %-12s %s" % ("项目", "style.css", "Compose", "结果"))
print("-" * 74)
bad = 0
for label, exp, got, ok in rows:
    if ok == "✗":
        bad += 1
    print("%-34s %-14s %-12s %s" % (label, str(exp), str(got), ok))
print("-" * 74)
print("共 %d 项，不一致 %d 项" % (len(rows), bad))

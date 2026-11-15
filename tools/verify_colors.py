# -*- coding: utf-8 -*-
"""核对：Theme.kt 的 AppColors vs web/style.css 的 :root 与 dark 变量"""
import io
import os
import re

BASE = r"C:\Users\一只屑\Desktop\HousewifeAppProto"
CSS = os.path.join(BASE, "web", "style.css")
THEME = os.path.join(BASE, "android-native", "app", "src", "main", "java", "com", "guoziri", "app", "ui", "theme", "Theme.kt")

css = io.open(CSS, encoding="utf-8").read()
css = re.sub(r"/\*.*?\*/", "", css, flags=re.S)
theme = io.open(THEME, encoding="utf-8").read()

light_block = re.search(r":root\s*\{(.*?)\}", css, re.S).group(1)
dark_block = re.search(r'html\[data-theme="dark"\]\s*\{(.*?)\}', css, re.S).group(1)


def cssmap(block):
    d = {}
    for m in re.finditer(r"(--[\w-]+)\s*:\s*([^;]+);", block):
        d[m.group(1)] = m.group(2).strip()
    return d


def kt_colors(obj):
    m = re.search(r"val\s+" + obj + r"\s*=\s*AppColors\(([\s\S]*?)\n\)", theme)
    if not m:
        return {}
    d = {}
    for m2 in re.finditer(r"(\w+)\s*=\s*Color\((0x[0-9A-Fa-f]+)\)", m.group(1)):
        d[m2.group(1)] = m2.group(2)
    return d


def rgba_to_argb(v):
    """rgba(60,60,67,0.6) / #007aff -> 0xAARRGGBB"""
    v = v.strip()
    m = re.match(r"rgba?\(\s*(\d+)\s*,\s*(\d+)\s*,\s*(\d+)\s*(?:,\s*([\d.]+))?\s*\)", v)
    if m:
        r, g, b = int(m.group(1)), int(m.group(2)), int(m.group(3))
        a = float(m.group(4)) if m.group(4) else 1.0
        return (round(a * 255) << 24) | (r << 16) | (g << 8) | b
    m = re.match(r"#([0-9a-fA-F]{6})", v)
    if m:
        return (0xFF << 24) | int(m.group(1), 16)
    return None


LIGHT_MAP = [
    ("bg", "--bg"), ("bgElevated", "--bg-elevated"), ("bgSecondary", "--bg-secondary"),
    ("label", "--label"), ("secondary", "--secondary"), ("tertiary", "--tertiary"),
    ("separator", "--separator"), ("fill", "--fill"), ("tint", "--tint"),
    ("tintSoft", "--tint-soft"), ("green", "--green"), ("orange", "--orange"),
    ("red", "--red"), ("yellow", "--yellow"),
]
DARK_MAP = [
    ("bg", "--bg"), ("bgElevated", "--bg-elevated"), ("bgSecondary", "--bg-secondary"),
    ("label", "--label"), ("secondary", "--secondary"), ("tertiary", "--tertiary"),
    ("separator", "--separator"), ("fill", "--fill"), ("tint", "--tint"),
    ("tintSoft", "--tint-soft"), ("green", "--green"), ("orange", "--orange"),
    ("red", "--red"), ("yellow", "--yellow"),
]

lc = cssmap(light_block)
dc = cssmap(dark_block)
L = kt_colors("LightAppColors")
D = kt_colors("DarkAppColors")


def check(title, mapping, cssmap_, ktmap_):
    print()
    print("== %s ==" % title)
    print("%-14s %-26s %-12s %s" % ("字段", "style.css", "Compose", "结果"))
    print("-" * 70)
    bad = 0
    for field, var in mapping:
        if var not in cssmap_:
            continue
        exp = rgba_to_argb(cssmap_[var])
        got = int(ktmap_.get(field, "0x0"), 16)
        ok = exp is not None and exp == got
        if not ok:
            bad += 1
        print("%-14s %-26s 0x%08X %s" % (field, cssmap_[var], got, "✓" if ok else "✗ 期望 0x%08X" % (exp or 0)))
    return bad


b1 = check("亮色", LIGHT_MAP, lc, L)
b2 = check("暗色", DARK_MAP, dc, D)
print()
print("颜色不一致：亮色 %d 项，暗色 %d 项" % (b1, b2))

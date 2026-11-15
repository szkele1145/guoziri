# -*- coding: utf-8 -*-
"""把 app.js 里的 SVG 图标转换成纯 path data（rect/circle/line/polyline 全部转 path）"""
import io
import re

SRC = r"C:\Users\一只屑\Desktop\HousewifeAppProto\web\app.js"


def rect_to_path(x, y, w, h, rx):
    x2, y2 = x + w, y + h
    if rx <= 0:
        return f"M{x},{y} H{x2} V{y2} H{x} Z"
    return (f"M{x + rx},{y} H{x2 - rx} A{rx},{rx} 0 0 1 {x2},{y + rx} "
            f"V{y2 - rx} A{rx},{rx} 0 0 1 {x2 - rx},{y2} "
            f"H{x + rx} A{rx},{rx} 0 0 1 {x},{y2 - rx} "
            f"V{y + rx} A{rx},{rx} 0 0 1 {x + rx},{y} Z")


def circle_to_path(cx, cy, r):
    return (f"M{cx - r},{cy} A{r},{r} 0 1 1 {cx + r},{cy} "
            f"A{r},{r} 0 1 1 {cx - r},{cy} Z")


def parse_svg(svg):
    """返回 [(d, is_fill)]"""
    out = []
    for m in re.finditer(r"<(rect|circle|path|line|polyline)\b([^>]*)/?>", svg):
        tag, attrs = m.group(1), m.group(2)
        a = dict(re.findall(r'([\w-]+)="([^"]*)"', attrs))

        def f(k, default=0.0):
            v = a.get(k, "")
            v = v.replace("px", "").strip()
            try:
                return float(v)
            except ValueError:
                return default

        if tag == "rect":
            d = rect_to_path(f("x"), f("y"), f("width"), f("height"), f("rx"))
            out.append((d, False))
        elif tag == "circle":
            out.append((circle_to_path(f("cx"), f("cy"), f("r")), False))
        elif tag == "path":
            d = a.get("d", "").strip()
            is_fill = a.get("fill", "none") != "none"
            out.append((d, is_fill))
        elif tag == "line":
            out.append((f"M{f('x1')},{f('y1')} L{f('x2')},{f('y2')}", False))
        elif tag == "polyline":
            pts = a.get("points", "").strip().split()
            if pts:
                coords = [tuple(map(float, p.split(","))) for p in pts]
                d = "M" + " L".join(f"{x},{y}" for x, y in coords)
                out.append((d, False))
    return out


text = io.open(SRC, encoding="utf-8").read()
start = text.find("const ICONS")
block = text[start:text.find("};", start)]

print("=== 提取到的图标 ===")
for m in re.finditer(r"(\w+):\s*'(<svg.*?</svg>)'", block, re.S):
    name, svg = m.group(1), m.group(2)
    paths = parse_svg(svg)
    print("  %s -> %d 条路径" % (name, len(paths)))
    for d, is_fill in paths:
        d1 = re.sub(r"\s+", " ", d).strip()
        print("      [%s] %s" % ("FILL" if is_fill else "stroke", d1))

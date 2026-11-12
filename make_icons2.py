# -*- coding: utf-8 -*-
"""v2 图标：蓝紫渐变底 + 白色太阳/日 呼应「过日子」，前景圆形日轮 + 光晕"""
import struct, zlib, os, math

RES = r"C:\Users\一只屑\Desktop\HousewifeAppProto\android-native\app\src\main\res"

def png(w, h, pixel_fn):
    raw = b""
    for y in range(h):
        raw += b"\x00"
        for x in range(w):
            raw += bytes(pixel_fn(x, y, w, h))
    def chunk(tag, data):
        c = struct.pack(">I", len(data)) + tag + data
        return c + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)
    ihdr = struct.pack(">IIBBBBB", w, h, 8, 2, 0, 0, 0)
    return b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", ihdr) + chunk(b"IDAT", zlib.compress(raw, 9)) + chunk(b"IEND", b"")

def lerp(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))

C1 = (0x0A, 0x84, 0xFF)   # 蓝
C2 = (0x5A, 0xC8, 0xFA)   # 浅蓝
WHITE = (255, 255, 255)

def icon(x, y, w, h):
    # 对角渐变底
    t = (x + y) / (2.0 * w)
    base = lerp(C1, C2, min(1.0, t * 1.2))
    cx, cy = w / 2, h / 2
    dx, dy = x - cx, y - cy
    r = math.hypot(dx, dy)
    # 白色日轮（实心圆，半径 0.26w）
    if r <= w * 0.26:
        # 日轮内部：上弦月缺口做「日」字意象——顶部挖弧
        ang = math.atan2(dy, dx)
        # 顶部月牙缺口（-90°方向挖一块）
        mx, my = 0.0, -w * 0.10
        mr = w * 0.22
        if math.hypot(dx - mx, dy - my) < mr and dy < 0:
            return base
        return WHITE
    # 外圈光晕细环（0.33w 处 2px 白色半透明环）
    if abs(r - w * 0.33) < w * 0.012:
        return WHITE
    return base

sizes = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}
for dpi, size in sizes.items():
    d = os.path.join(RES, "mipmap-" + dpi)
    os.makedirs(d, exist_ok=True)
    data = png(size, size, icon)
    for name in ("ic_launcher.png", "ic_launcher_round.png"):
        io_open = open(os.path.join(d, name), "wb")
        io_open.write(data)
        io_open.close()
    print(dpi, size, "ok")
print("DONE")

# -*- coding: utf-8 -*-
"""v4 图标：渐变底 + 白色半日（平切口露出底色）+ 白色托举弧 —— 升起的太阳"""
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

C1 = (0x0A, 0x84, 0xFF)
C2 = (0x5A, 0xC8, 0xFA)
WHITE = (255, 255, 255)

def icon(x, y, w, h):
    t = (x + y) / (2.0 * w)
    base = lerp(C1, C2, min(1.0, t * 1.2))
    cx, cy = w * 0.5, h * 0.46          # 日轮中心略上
    dx, dy = x - cy * 0 + (x - cx), (y - cy)
    r = math.hypot(dx, dy)

    cut = cy + w * 0.10                  # 平切口 y 坐标

    # 白色半日：圆内且在切口之上
    if r <= w * 0.25 and y <= cut:
        return WHITE
    # 托举弧：切口下方一条白弧（圆心在切口上，半径 0.3w，只取下缘）
    if y > cut:
        arc_r = w * 0.30
        arc_dy = y - cut
        if arc_dy > 0:
            half_chord = math.sqrt(max(0.0, arc_r * arc_r - arc_dy * arc_dy))
            if abs(abs(dx) - half_chord) < w * 0.018 and arc_dy < w * 0.16:
                return WHITE
    return base

sizes = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}
for dpi, size in sizes.items():
    d = os.path.join(RES, "mipmap-" + dpi)
    os.makedirs(d, exist_ok=True)
    data = png(size, size, icon)
    for name in ("ic_launcher.png", "ic_launcher_round.png"):
        with open(os.path.join(d, name), "wb") as f:
            f.write(data)
    print(dpi, size, "ok")
print("DONE")

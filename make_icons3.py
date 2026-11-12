# -*- coding: utf-8 -*-
"""v3 图标：蓝紫渐变底 + 白色圆环 + 底部托举弧（「日」升意象，简洁不杂）"""
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
    cx, cy = w * 0.5, h * 0.52          # 圆心略上移
    dx, dy = x - cx, y - cy
    r = math.hypot(dx, dy)

    # 白色实心日轮（半日）：上半是圆，下半截平——像升起的太阳
    if r <= w * 0.24:
        return WHITE
    # 下半截平线以下挖空（把圆下半切平）：圆心以下 0.06w 处全部清除
    if dy > w * 0.06 and r <= w * 0.30:
        return base
    # 底部托举弧线（在切口下方的浅弧）
    arc_cy = cy + w * 0.06
    if dy + w * 0.06 > 0 and abs(math.hypot(dx, (y - arc_cy)) - w * 0.17) < w * 0.02 and y > cy:
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

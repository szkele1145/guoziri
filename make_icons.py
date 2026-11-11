# -*- coding: utf-8 -*-
"""生成 API 24/25 回退 PNG 启动图标（蓝底白圈）"""
import struct
import zlib
import os

RES = r"C:\Users\一只屑\Desktop\HousewifeAppProto\android\app\src\main\res"


def png(width, height, pixel_fn):
    """手写 PNG 生成器，不依赖 Pillow"""
    raw = b""
    for y in range(height):
        raw += b"\x00"  # filter: none
        for x in range(width):
            r, g, b = pixel_fn(x, y, width, height)
            raw += bytes((r, g, b))

    def chunk(tag, data):
        c = struct.pack(">I", len(data)) + tag + data
        c += struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)
        return c

    ihdr = struct.pack(">IIBBBBB", width, height, 8, 2, 0, 0, 0)  # RGB8
    return (b"\x89PNG\r\n\x1a\n"
            + chunk(b"IHDR", ihdr)
            + chunk(b"IDAT", zlib.compress(raw, 9))
            + chunk(b"IEND", b""))


def icon(x, y, w, h):
    # 蓝底 #007AFF，中间白色圆环（简约「过」意象：圆环+缺口）
    cx, cy = w / 2, h / 2
    dx, dy = x - cx, y - cy
    r = (dx * dx + dy * dy) ** 0.5
    outer = w * 0.30
    inner = w * 0.22
    if inner <= r <= outer:
        return (255, 255, 255)   # 白环
    return (0, 122, 255)          # 蓝底


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

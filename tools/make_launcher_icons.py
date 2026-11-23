# -*- coding: utf-8 -*-
"""
生成「过日子」App 桌面图标

设计理念：
  - 品牌蓝圆角方块底
  - 中间一个白色「日」字（实心方块 + 两道蓝色横槽），既像账本，也像「过日子」的「日」
  - 底部三道蓝色短线 = 记账流水
  - 缩到 48px 依然能认出「日」和账本的形
"""
import os

from PIL import Image, ImageDraw

RES = r"C:\Users\一只屑\Desktop\HousewifeAppProto\android-native\app\src\main\res"
BLUE = (10, 132, 255, 255)
WHITE = (255, 255, 255, 255)

DENSITIES = {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192,
}


def draw_icon(size, round_icon=False):
    ss = 8
    S = size * ss
    img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)

    # 背景
    if round_icon:
        d.ellipse([0, 0, S - 1, S - 1], fill=BLUE)
    else:
        d.rounded_rectangle([0, 0, S - 1, S - 1], radius=int(S * 0.225), fill=BLUE)

    # 内容整体缩小，四周留出明显白边（视觉更透气）
    pw = S * 0.40           # 主体宽
    ph = S * 0.46           # 主体高
    cx, cy = S / 2, S * 0.435
    left, top = cx - pw / 2, cy - ph / 2
    right, bottom = cx + pw / 2, cy + ph / 2
    r = int(pw * 0.24)

    d.rounded_rectangle([left, top, right, bottom], radius=r, fill=WHITE)

    # 两道蓝色横槽，把白块切成「日」的三格
    slot_h = int(ph * 0.115)
    slot_w = pw * 0.58
    for frac in (0.335, 0.665):
        sy = top + ph * frac - slot_h / 2
        d.rounded_rectangle(
            [cx - slot_w / 2, sy, cx + slot_w / 2, sy + slot_h],
            radius=slot_h / 2, fill=BLUE
        )

    # 底部三道记账短线
    lw = int(S * 0.026)
    y = bottom + S * 0.072
    x0 = cx - pw * 0.40
    for i, frac in enumerate((0.88, 0.68, 0.46)):
        d.rounded_rectangle(
            [x0, y + i * S * 0.062, x0 + pw * frac, y + i * S * 0.062 + lw],
            radius=lw / 2, fill=WHITE
        )

    return img.resize((size, size), Image.LANCZOS)


def draw_fg(size):
    """自适应前景层（108dp 视口，内容占 ~36dp 居中，四周留白充足）"""
    S = size * 4
    img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)

    pw = S * 36 / 108
    ph = S * 41 / 108
    cx, cy = S / 2, S * 0.442
    left, top = cx - pw / 2, cy - ph / 2
    right, bottom = cx + pw / 2, cy + ph / 2
    r = int(pw * 0.24)

    d.rounded_rectangle([left, top, right, bottom], radius=r, fill=WHITE)

    slot_h = int(ph * 0.115)
    slot_w = pw * 0.58
    for frac in (0.335, 0.665):
        sy = top + ph * frac - slot_h / 2
        d.rounded_rectangle(
            [cx - slot_w / 2, sy, cx + slot_w / 2, sy + slot_h],
            radius=slot_h / 2, fill=BLUE
        )

    lw = int(S * 0.0145)
    y = bottom + S * 0.040
    x0 = cx - pw * 0.40
    for i, frac in enumerate((0.88, 0.68, 0.46)):
        d.rounded_rectangle(
            [x0, y + i * S * 0.0345, x0 + pw * frac, y + i * S * 0.0345 + lw],
            radius=lw / 2, fill=WHITE
        )

    return img.resize((size, size), Image.LANCZOS)


def main():
    for folder, size in DENSITIES.items():
        outdir = os.path.join(RES, folder)
        os.makedirs(outdir, exist_ok=True)
        draw_icon(size, round_icon=False).save(os.path.join(outdir, "ic_launcher.png"))
        draw_icon(size, round_icon=True).save(os.path.join(outdir, "ic_launcher_round.png"))
        print("  %s: %dx%d ✓" % (folder, size, size))

    outdir = os.path.join(RES, "mipmap-anydpi-v26")
    os.makedirs(outdir, exist_ok=True)
    draw_fg(432).save(os.path.join(outdir, "ic_launcher_fg.png"))
    print("  mipmap-anydpi-v26: ic_launcher_fg.png ✓")


if __name__ == "__main__":
    main()
    print("图标生成完成")

# -*- coding: utf-8 -*-
"""细节对齐：AlertDialog 宽度 270、首项分组标题 8dp"""
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


# 1) AlertDialog 宽度 270（.alert-box width: 270px）
for name in ("AppRoot.kt", "SettingsPage.kt"):
    p, t = load(name)
    n = 0
    def add_mod(m):
        global n
        indent = m.group(1)
        n += 1
        return "AlertDialog(\n%smodifier = Modifier.width(270.dp),\n%sonDismissRequest" % (indent, indent)
    t2 = re.sub(r"AlertDialog\(\n(\s*)onDismissRequest", add_mod, t)
    if t2 != t:
        save(p, t2)
        print("  %s: 加了 %d 个 AlertDialog 宽度" % (name, n))

# 2) 首个分组标题 margin-top 8（.group-title:first-child）
p, t = load("AiPage.kt")
t2 = t.replace('        GroupTitle("一键总结")', '        GroupTitle("一键总结", topSpacing = 8.dp)')
if t2 != t:
    save(p, t2)
    print("  AiPage.kt: 首个分组标题 8dp")

p, t = load("SettingsPage.kt")
t2 = t.replace('        GroupTitle("AI 服务商")', '        GroupTitle("AI 服务商", topSpacing = 8.dp)')
if t2 != t:
    save(p, t2)
    print("  SettingsPage.kt: 首个分组标题 8dp")

# 3) PurchaseSheet 底部留 safe area
p, t = load("PurchaseSheet.kt")
old = """                    .padding(horizontal = 16.dp)
                    .padding(top = 16.dp, bottom = 24.dp),"""
new = """                    .padding(horizontal = 16.dp)
                    .navigationBarsPadding()
                    .padding(top = 16.dp, bottom = 24.dp),"""
if old in t:
    t = t.replace(old, new)
    save(p, t)
    print("  PurchaseSheet.kt: 底部加 safe area")
else:
    print("  PurchaseSheet.kt: 未找到目标片段（跳过）")

print("完成")

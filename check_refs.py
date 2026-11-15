import os
import re
import glob

BASE = r"C:\Users\一只屑\Desktop\HousewifeAppProto\android-native\app\src\main\java\com\guoziri\app"
files = glob.glob(os.path.join(BASE, "*.kt")) + glob.glob(os.path.join(BASE, "ui", "theme", "*.kt"))

texts = {}
for f in files:
    with open(f, encoding="utf-8") as fh:
        texts[f] = fh.read()

# 1. 收集 object 定义及其成员
objects = {}
for f, t in texts.items():
    for m in re.finditer(r"object\s+([A-Za-z_]\w*)\s*\{", t):
        name = m.group(1)
        # 简单括号匹配取 body
        start = m.end() - 1
        depth = 0
        i = start
        while i < len(t):
            if t[i] == '{':
                depth += 1
            elif t[i] == '}':
                depth -= 1
                if depth == 0:
                    break
            i += 1
        body = t[start:i]
        members = set(re.findall(r"\b(?:val|var|fun)\s+([A-Za-z_]\w*)", body))
        objects.setdefault(name, set()).update(members)

# 2. 顶层 val / fun
top_vals = set()
for t in texts.values():
    top_vals |= set(re.findall(r"^val\s+([A-Za-z_]\w*)", t, re.M))

# 3. 扫描引用 X.Member
print("=== 自定义 object 成员引用检查 ===")
problems = []
for f, t in texts.items():
    for m in re.finditer(r"\b([A-Z][A-Za-z0-9_]*)\.([A-Za-z_][A-Za-z0-9_]*)", t):
        obj, mem = m.group(1), m.group(2)
        if obj in objects:
            if mem not in objects[obj]:
                # 排除 Kotlin/Compose 标准对象的成员（Icons.Rounded 等）
                if obj in ("Icons",):
                    continue
                problems.append((os.path.basename(f), obj, mem))

seen = set()
for f, obj, mem in problems:
    key = (obj, mem)
    if key in seen:
        continue
    seen.add(key)
    print(f"  {f}: {obj}.{mem}")

if not problems:
    print("  全部成员引用都有定义")

print()
print("=== 已定义 object ===")
for k, v in sorted(objects.items()):
    print(f"  {k} ({len(v)} 个成员)")

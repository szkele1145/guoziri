# -*- coding: utf-8 -*-
"""
实测 AI 营养查询（模拟 FoodSearchService.askAI 的调用方式）
需要一个真实的 API Key —— 从 App 设置里读，或环境变量传入
"""
import json
import os
import sys
import urllib.request

# 尝试从环境变量拿 Key
KEY = os.environ.get("AI_KEY", "")
BASE = os.environ.get("AI_BASE", "https://api.deepseek.com/v1")
MODEL = os.environ.get("AI_MODEL", "deepseek-chat")

if not KEY:
    print("⚠️ 未提供 AI_KEY 环境变量，跳过实测")
    print("用法: set AI_KEY=sk-xxx && python test_ai_food.py 秋葵炒蛋")
    sys.exit(0)

food = sys.argv[1] if len(sys.argv) > 1 else "秋葵炒蛋"
prompt = """你是一个营养数据库。请给出「%s」每 100 克可食部分的营养成分。
只输出 JSON，不要任何其他文字，格式：
{"calories":数字,"protein":数字,"fat":数字,"carbs":数字,"fiber":数字,"vitaminC":数字,"calcium":数字,"iron":数字}
单位：calories=kcal，其余分别 g/g/g/g/g/mg/mg/mg。数值为合理均值即可。""" % food

body = json.dumps({
    "model": MODEL,
    "messages": [
        {"role": "system", "content": "你是营养数据 API，只输出 JSON。"},
        {"role": "user", "content": prompt},
    ],
    "temperature": 0.1,
}).encode()

req = urllib.request.Request(
    BASE.rstrip("/") + "/chat/completions",
    data=body,
    headers={
        "Authorization": "Bearer " + KEY,
        "Content-Type": "application/json",
    },
    method="POST",
)

print("=== 请求 %s (%s) ===" % (BASE, MODEL))
resp = urllib.request.urlopen(req, timeout=60)
text = json.loads(resp.read())
content = text["choices"][0]["message"]["content"]
print("AI 原始回复:")
print(content[:400])

m = re.search(r"\{[^{}]*\}", content) if (re := __import__("re")) else None
if m:
    data = json.loads(m.group(0))
    print("\n✅ 解析成功:")
    for k, v in data.items():
        print("    %s = %s" % (k, v))
else:
    print("\n❌ 未能提取 JSON")

# -*- coding: utf-8 -*-
"""GitHub 搜索中文食物营养数据集仓库"""
import json, urllib.request, urllib.parse, time

QUERIES = [
    "中国食物成分表",
    "食物成分表 json",
    "食物营养 数据库",
    "china food composition",
    "chinese food nutrition dataset",
    "FoodKG",
    "中文食物 知识图谱",
]
seen = {}
for q in QUERIES:
    url = "https://api.github.com/search/repositories?" + urllib.parse.urlencode(
        {"q": q, "sort": "stars", "order": "desc", "per_page": 8})
    req = urllib.request.Request(url, headers={
        "Accept": "application/vnd.github+json",
        "User-Agent": "food-db-research/1.0",
    })
    try:
        with urllib.request.urlopen(req, timeout=15) as r:
            data = json.loads(r.read().decode("utf-8"))
        for item in data.get("items", []):
            full = item["full_name"]
            if full not in seen:
                seen[full] = {
                    "stars": item["stargazers_count"],
                    "desc": (item.get("description") or "")[:120],
                    "lang": item.get("language"),
                    "updated": item["updated_at"][:10],
                    "query": q,
                }
        print(f"[OK] {q}: total={data.get('total_count')}")
    except Exception as e:
        print(f"[FAIL] {q}: {type(e).__name__}: {e}")
    time.sleep(7)  # 匿名搜索限速 10 req/min

print("\n=== 汇总（按 star 排序） ===")
for full, info in sorted(seen.items(), key=lambda kv: -kv[1]["stars"]):
    print(f"{info['stars']:>6}★  {full}  [{info['lang']}]  {info['desc']}  ({info['updated']}, q={info['query']})")

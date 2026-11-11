# -*- coding: utf-8 -*-
"""端到端联调测试：走 HTTP 调真实 Flask"""
import json
import urllib.request

BASE = "http://127.0.0.1:5000"


def call(method, path, body=None):
    data = json.dumps(body).encode() if body is not None else None
    req = urllib.request.Request(BASE + path, data=data, method=method)
    req.add_header("Content-Type", "application/json")
    status = 0
    try:
        with urllib.request.urlopen(req) as r:
            status = r.status
            raw = r.read()
    except urllib.error.HTTPError as e:
        status = e.code
        raw = e.read()
    try:
        return status, json.loads(raw.decode("utf-8"))
    except (ValueError, UnicodeDecodeError):
        # 非 JSON（HTML/字体等二进制）
        return status, raw


passed, failed = 0, 0


def check(name, cond, extra=""):
    global passed, failed
    if cond:
        passed += 1
        print("  OK   " + name)
    else:
        failed += 1
        print("  FAIL " + name + ("  -> " + str(extra) if extra else ""))


print("=== 页面 ===")
s, _ = call("GET", "/")
check("首页 200", s == 200, s)
s, _ = call("GET", "/style.css")
check("CSS 200", s == 200, s)
s, _ = call("GET", "/app.js")
check("JS 200", s == 200, s)
s, _ = call("GET", "/fonts/HarmonyOS_Sans_SC.ttf")
check("字体 200", s == 200, s)

print("=== 设置 ===")
s, st = call("GET", "/api/settings")
check("读设置", s == 200 and st.get("family"), st)
check("hasKey 字段存在", "hasKey" in st)
check("apiKey 不下发", "apiKey" not in st)
s, st = call("PUT", "/api/settings", {"family": {"adults": 2, "children": 1}, "hiddenEnabled": True})
check("写设置", s == 200 and st["family"]["adults"] == 2, st)
check("隐藏页开启", st.get("hiddenEnabled") is True)
# key 安全测试
s, st = call("PUT", "/api/settings", {"apiKey": "sk-test-secret-123"})
check("key 可写入", s == 200 and st.get("hasKey") is True)
check("key 不回传", "apiKey" not in json.dumps(st))
s, st = call("GET", "/api/settings")
check("key 存储后不泄漏", "apiKey" not in json.dumps(st) and st["hasKey"] is True)

print("=== 记账 ===")
s, p1 = call("POST", "/api/purchases", {"name": "西兰花", "category": "蔬菜", "pricePerJin": 6.5, "totalCost": 9.8, "date": "2026-11-11"})
check("新增记录", s == 201 and p1["id"], p1)
s, p2 = call("POST", "/api/purchases", {"name": "鸡胸肉", "category": "肉类", "totalJin": 1.8, "totalCost": 21.6, "date": "2026-11-11"})
check("第二条", s == 201)
s, lst = call("GET", "/api/purchases")
check("列表 2 条", s == 200 and len(lst) == 2, len(lst))
check("无空名记录", all(p["name"] for p in lst))
s, p3 = call("POST", "/api/purchases", {"name": "", "category": "蔬菜"})
check("空名被拒 400", s == 400, s)
s, pu = call("PUT", "/api/purchases/" + p1["id"], {"name": "西兰花(改)", "category": "蔬菜", "totalCost": 10.5, "date": "2026-11-11"})
check("编辑成功", s == 200 and pu["name"] == "西兰花(改)" and pu["totalCost"] == 10.5, pu)
s, r = call("DELETE", "/api/purchases/" + p2["id"])
check("删除成功", s == 200 and r.get("ok"))
s, lst = call("GET", "/api/purchases")
check("删除后剩 1 条", len(lst) == 1, len(lst))
s, _ = call("DELETE", "/api/purchases/notexist")
check("删不存在 404", s == 404, s)

print("=== 营养 ===")
# 加回鸡胸肉，且用 单价+总价 推重量
s, _ = call("POST", "/api/purchases", {"name": "鸡胸肉", "category": "肉类", "pricePerJin": 12, "totalCost": 24, "date": "2026-11-11"})
check("加鸡胸肉", s == 201)
s, n = call("GET", "/api/nutrition?date=2026-11-11")
check("营养计算 200", s == 200)
check("assessment 8 项", len(n.get("assessment", [])) == 8, n)
cal = next((a for a in n["assessment"] if a["key"] == "calories"), None)
check("热量>0", cal and cal["actual"] > 0, cal)
# 西兰花 10.5/6.5*500=807g + 鸡胸肉 24/12*500=1000g
# 热量 = 33*8.07 + 133*10 = 266.3 + 1330 = 1596.3
check("热量数值正确(~1596)", cal and 1590 < cal["actual"] < 1600, cal)
s, names = call("GET", "/api/food-names")
check("食材表 90+", len(names) >= 90, len(names))

print("=== 日记 ===")
s, d1 = call("POST", "/api/diary", {"content": "测试日记内容", "date": "2026-11-11"})
check("新增日记", s == 201 and d1["id"], d1)
check("time 自动补", d1.get("time"), d1)
s, dl = call("GET", "/api/diary")
check("日记列表", len(dl) == 1)
s, _ = call("DELETE", "/api/diary/" + d1["id"])
check("删除日记", s == 200)

print("=== AI ===")
# 未填 key → 428；假 key（sk-test-secret-123 是 sk- 开头格式合法）→ 转发 401 → 502
call("PUT", "/api/settings", {"apiKey": "sk-test-secret-123"})
s, r = call("POST", "/api/ai/chat", {"messages": [{"role": "user", "content": "hi"}]})
check("假 key 转发失败 502", s == 502, (s, r))
check("错误信息无泄漏", "sk-test" not in json.dumps(r), r)
# 非法格式 key → 428
call("PUT", "/api/settings", {"apiKey": "not-a-valid-key"})
s, r = call("POST", "/api/ai/chat", {"messages": [{"role": "user", "content": "hi"}]})
check("非法 key 428", s == 428, (s, r))

# 清理 key
call("PUT", "/api/settings", {"apiKey": ""})
s, st = call("GET", "/api/settings")
check("key 已清除", st["hasKey"] is False)

print("\n===== %d 通过, %d 失败 =====" % (passed, failed))
raise SystemExit(1 if failed else 0)

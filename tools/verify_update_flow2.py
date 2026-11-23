# -*- coding: utf-8 -*-
"""验证修复后的 Atom feed 解析 + expanded_assets 抓 APK"""
import re
import urllib.request

UA = {"User-Agent": "Guoziri-App"}


def get(url, headers=None):
    h = dict(UA)
    h.update(headers or {})
    req = urllib.request.Request(url, headers=h)
    return urllib.request.urlopen(req, timeout=25).read().decode("utf-8", errors="replace")


APK_PAT = re.compile(r'href="([^"]*/releases/download/[^"]*\.apk)"')

print("=== Atom feed entry 解析（修复后）===")
feed = get("https://github.com/szkele1145/guoziri/releases.atom")
i = feed.find("<entry>")
entry = feed[i:] if i >= 0 else ""
m = re.search(r"<id>[^<]*/([^/<]+)</id>", entry)
print("  entry tag =", m.group(1) if m else "(未找到)")

print()
print("=== expanded_assets 抓 APK（第三级兜底）===")
try:
    html = get(
        "https://github.com/szkele1145/guoziri/releases/expanded_assets/v1.3",
        {"Accept": "text/html"},
    )
    m2 = APK_PAT.search(html)
    print("  apk =", m2.group(1) if m2 else "(未找到)")
    print("  html 长度 =", len(html))
except Exception as e:
    print("  FAIL", e)

print()
print("=== REST API 直连（当前状态）===")
try:
    d = get("https://api.github.com/repos/szkele1145/guoziri/releases/latest",
            {"Accept": "application/vnd.github+json"})
    import json
    j = json.loads(d)
    print("  OK  tag =", j["tag_name"])
except Exception as e:
    print("  FAIL（可能触发速率限制）:", e)

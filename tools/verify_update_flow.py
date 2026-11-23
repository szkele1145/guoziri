# -*- coding: utf-8 -*-
"""验证自动更新的三条检查路径是否都能工作"""
import json
import re
import urllib.request

UA = {"User-Agent": "Guoziri-App", "Accept": "application/vnd.github+json"}
APK_LINK_PAT = re.compile(r'href="([^"]*/releases/download/[^"]*\.apk)"')


def get(url, headers=None):
    h = dict(UA)
    if headers:
        h.update(headers)
    req = urllib.request.Request(url, headers=h)
    return urllib.request.urlopen(req, timeout=25).read().decode("utf-8", errors="replace")


print("=== 1. Atom feed（App 首选）===")
try:
    feed = get("https://github.com/szkele1145/guoziri/releases.atom")
    m = re.search(r"<id>[^<]*/([^/<]+)</id>", feed)
    print("  OK   最新 tag =", m.group(1) if m else "(未解析出)")
except Exception as e:
    print("  FAIL", e)

print()
print("=== 2. REST API（回退）===")
try:
    d = json.loads(get("https://api.github.com/repos/szkele1145/guoziri/releases/latest"))
    print("  OK   tag =", d["tag_name"])
    for a in d["assets"]:
        print("       asset:", a["name"], a["size"], "bytes")
        print("       url  :", a["browser_download_url"])
except Exception as e:
    print("  FAIL", e)

print()
print("=== 3. releases 页面抓 APK（第三级兜底）===")
try:
    html = get("https://github.com/szkele1145/guoziri/releases/tag/v1.3")
    m = APK_LINK_PAT.search(html)
    print("  OK   apk link =", m.group(1) if m else "(未找到)")
except Exception as e:
    print("  FAIL", e)

print()
print("=== 4. 版本比较逻辑（当前 1.3 vs 最新）===")


def is_newer(latest, current):
    def parts(v):
        return [int(x) if x.isdigit() else 0 for x in v.replace("v", "").split(".")]

    l, c = parts(latest), parts(current)
    for i in range(max(len(l), len(c))):
        lv = l[i] if i < len(l) else 0
        cv = c[i] if i < len(c) else 0
        if lv != cv:
            return lv > cv
    return False


for tag, cur in [("v1.3", "1.3"), ("v1.4", "1.3"), ("v2.0", "1.3"), ("v1.2", "1.3")]:
    print("  最新 %-6s 当前 %-4s -> %s" % (tag, cur, "有更新" if is_newer(tag, cur) else "已是最新"))

# -*- coding: utf-8 -*-
"""检查 GitHub API 返回的 browser_download_url 到底是什么形式"""
import json
import urllib.request

UA = {"User-Agent": "Guoziri-App", "Accept": "application/vnd.github+json"}


def get(url):
    return urllib.request.urlopen(urllib.request.Request(url, headers=UA), timeout=20).read()


print("=== 直连 API ===")
try:
    d = json.loads(get("https://api.github.com/repos/szkele1145/guoziri/releases/latest"))
    print("  tag_name =", d.get("tag_name"))
    for a in d.get("assets", []):
        print("  name    =", a.get("name"))
        print("  size    =", a.get("size"))
        print("  url     =", repr(a.get("browser_download_url")))
        print("  是完整 URL？", str(a.get("browser_download_url")).startswith("http"))
except Exception as e:
    print("  FAIL", e)

print()
print("=== 通过 gh-proxy.com 代理 API ===")
try:
    d = json.loads(get("https://gh-proxy.com/https://api.github.com/repos/szkele1145/guoziri/releases/latest"))
    print("  tag_name =", d.get("tag_name"))
    for a in d.get("assets", []):
        u = a.get("browser_download_url")
        print("  url     =", repr(u))
        print("  是完整 URL？", str(u).startswith("http"))
except Exception as e:
    print("  FAIL", e)

print()
print("=== expanded_assets 里的链接形式 ===")
try:
    import re
    body = get("https://github.com/szkele1145/guoziri/releases/expanded_assets/v1.3")
    text = body.decode("utf-8", errors="ignore")
    m = re.search(r'href="([^"]*/releases/download/[^"]*\.apk)"', text)
    print("  原始 href =", repr(m.group(1)) if m else "(未找到)")
    print("  是相对路径？", m and m.group(1).startswith("/"))
except Exception as e:
    print("  FAIL", e)

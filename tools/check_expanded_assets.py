# -*- coding: utf-8 -*-
"""验证 expanded_assets 按 tag 直接抓（区分 releases / test 通道）"""
import re
import urllib.request

UA = {"User-Agent": "Guoziri-App", "Accept": "text/html"}
PAT = re.compile(r'href="([^"]*/releases/download/[^"]*\.apk)"')

for ch in ["releases", "test"]:
    url = "https://github.com/szkele1145/guoziri/releases/expanded_assets/" + ch
    try:
        body = urllib.request.urlopen(
            urllib.request.Request(url, headers=UA), timeout=15
        ).read().decode("utf-8", errors="ignore")
        m = PAT.search(body)
        link = m.group(1) if m else "(未找到)"
        print("  %-10s -> %s" % (ch, link))
    except Exception as e:
        print("  %-10s -> 失败 %s" % (ch, str(e)[:60]))

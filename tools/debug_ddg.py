# -*- coding: utf-8 -*-
"""诊断 DuckDuckGo 搜索返回内容"""
import urllib.request
import urllib.parse

UA = "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36"

food = "西兰花"
q = urllib.parse.quote("%s 营养成分表 每100克" % food)
url = "https://html.duckduckgo.com/html/?q=%s" % q
print("URL:", url)

req = urllib.request.Request(url, headers={"User-Agent": UA, "Accept": "text/html"})
try:
    body = urllib.request.urlopen(req, timeout=15).read().decode("utf-8", errors="ignore")
    print("长度:", len(body))
    print("--- 前 800 字符 ---")
    print(body[:800])
    print("--- 找 result 链接 ---")
    import re
    for pat in [r'class="result__a"', r'class="result"', r"uddg=", r"<a[^>]*href"]:
        ms = re.findall(pat, body)
        print("%-20s -> %d 处" % (pat, len(ms)))
except Exception as e:
    print("FAIL:", e)

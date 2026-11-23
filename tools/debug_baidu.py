# -*- coding: utf-8 -*-
"""看百度搜索结果页的原始链接结构"""
import re
import urllib.request
import urllib.parse

UA = "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120 Mobile Safari/537.36"

food = "西兰花 营养成分表 每100克"
q = urllib.parse.quote(food)
html = urllib.request.urlopen(
    urllib.request.Request("https://www.baidu.com/s?wd=%s&rn=10" % q,
                           headers={"User-Agent": UA}), timeout=15
).read().decode("utf-8", errors="ignore")

print("总长度:", len(html))
print("\n=== 所有 href 样本（去重，前 25 个）===")
hrefs = re.findall(r'href="([^"]{10,120})"', html)
seen = set()
count = 0
for h in hrefs:
    if h in seen or "baidu.com" in h and "link" not in h:
        continue
    seen.add(h)
    count += 1
    print("  ", h[:100])
    if count >= 25:
        break

print("\n=== 搜 link?url ===")
m = re.findall(r'(https?://www\.baidu\.com/link\?url=[A-Za-z0-9_\-=&]{10,80})', html)
print("  数量:", len(m))
for u in m[:5]:
    print("  ", u[:90])

print("\n=== 搜 mu= （百度结果的真实地址参数）===")
mus = re.findall(r'mu=([^&"\']+)', html)
import urllib.parse
for u in mus[:8]:
    try:
        print("  ", urllib.parse.unquote(u)[:90])
    except:
        pass

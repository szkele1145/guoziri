# -*- coding: utf-8 -*-
"""用百度移动版 UA 测试搜索结果链接"""
import re
import urllib.request
import urllib.parse

# 移动版 UA
UA = "Mozilla/5.0 (Linux; Android 10; V2353A) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36 baidubrowser"

food = "西兰花 营养成分表 每100克"
q = urllib.parse.quote(food)

for name, url in [
    ("移动版百度", "https://m.baidu.com/s?wd=%s" % q),
    ("百度PC版", "https://www.baidu.com/s?wd=%s&rn=10" % q),
]:
    print("\n=== %s ===" % name)
    try:
        html = urllib.request.urlopen(
            urllib.request.Request(url, headers={"User-Agent": UA}), timeout=15
        ).read().decode("utf-8", errors="ignore")
        print("长度:", len(html))
        # 找跳转链接
        jumps = re.findall(r'(https?://(?:www\.)?baidu\.com/link\?url=[A-Za-z0-9_\-=&%]+)', html)
        print("link?url 跳转:", len(jumps))
        for u in jumps[:4]:
            print("  ", u[:100])
        # mu= 参数
        mus = re.findall(r'"mu":"(https?://[^"]+)"', html)
        print('mu= 真实地址:', len(mus))
        for u in mus[:5]:
            print("  ", u[:100])
        # data-href / log-info
        dh = re.findall(r'data-href="(https?://[^"]+)"', html)
        print("data-href:", len(dh))
        for u in dh[:5]:
            print("  ", u[:100])
    except Exception as e:
        print("FAIL:", str(e)[:70])

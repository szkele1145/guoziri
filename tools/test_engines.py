# -*- coding: utf-8 -*-
"""对比测试：百度搜索 vs DDG vs 必应，找到国内可用的搜索源"""
import re
import urllib.request
import urllib.parse

UA = "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120 Mobile Safari/537.36"


def fetch(url):
    req = urllib.request.Request(url, headers={
        "User-Agent": UA,
        "Accept": "text/html,application/xhtml+xml",
        "Accept-Language": "zh-CN,zh;q=0.9",
    })
    return urllib.request.urlopen(req, timeout=15).read().decode("utf-8", errors="ignore")


def test(name, url, link_pat):
    print("\n=== %s ===" % name)
    try:
        html = fetch(url)
        print("  长度:", len(html))
        links = re.findall(link_pat, html)
        print("  链接:", len(links), "个")
        for u in links[:3]:
            print("   -", u[:70])
        return links
    except Exception as e:
        print("  FAIL:", str(e)[:70])
        return []


food = "西兰花 营养成分表 每100克"
q = urllib.parse.quote(food)

# 百度
test("百度", "https://www.baidu.com/s?wd=%s&rn=10" % q,
     r'href="(https?://www\.baidu\.com/link\?url=[^"]+|https?://(?!www\.baidu)[^"]+)"')

# 必应中国
test("必应中国", "https://cn.bing.com/search?q=%s" % q,
     r'<h2><a href="(https?://[^"]+)"')

# 搜狗
test("搜狗", "https://www.sogou.com/web?query=%s" % q,
     r'<h3[^>]*>\s*<a[^>]*href="(https?://[^"]+)"')

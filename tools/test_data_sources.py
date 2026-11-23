# -*- coding: utf-8 -*-
"""测试国内食物营养数据源：薄荷健康、fatsecret、百科"""
import re
import urllib.request
import urllib.parse

UA = "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120 Mobile Safari/537.36"


def fetch(url, timeout=15):
    req = urllib.request.Request(url, headers={
        "User-Agent": UA,
        "Accept": "text/html,application/json",
        "Accept-Language": "zh-CN,zh;q=0.9",
    })
    return urllib.request.urlopen(req, timeout=timeout).read().decode("utf-8", errors="ignore")


food = "西兰花"
q = urllib.parse.quote(food)

print("=== 1. 薄荷健康食物页 ===")
try:
    # 薄荷的食物搜索页
    html = fetch("https://www.boohee.com/food/search?keyword=%s" % q)
    print("  长度:", len(html))
    # 找食物详情链接
    links = re.findall(r'href="(/food/\d+[^"]*)"', html)
    print("  食物链接:", len(links), links[:4])
except Exception as e:
    print("  FAIL:", str(e)[:70])

print("\n=== 2. 薄荷 API ===")
try:
    r = fetch("https://www.boohee.com/food/api/search?keyword=%s" % q)
    print("  返回:", r[:200])
except Exception as e:
    print("  FAIL:", str(e)[:70])

print("\n=== 3. 百度百科 ===")
try:
    html = fetch("https://baike.baidu.com/item/%s" % q)
    print("  长度:", len(html))
    # 百科里找营养成分
    m = re.search(r"热量[^\d]{0,20}(\d+\.?\d*)\s*(?:千卡|kcal|大卡)", html, re.I)
    print("  热量命中:", m.group(1) if m else "无")
    p = re.search(r"蛋白质[^\d]{0,20}(\d+\.?\d*)\s*g", html, re.I)
    print("  蛋白质命中:", p.group(1) if p else "无")
except Exception as e:
    print("  FAIL:", str(e)[:70])

print("\n=== 4. fatsecret.cn ===")
try:
    html = fetch("https://cn.fatsecret.cn/%E7%83%AD%E9%87%8F-%E8%90%A5%E5%85%BB/%E8%A5%BF%E5%85%B0%E8%8A%B1")
    print("  长度:", len(html))
except Exception as e:
    print("  FAIL:", str(e)[:70])

# -*- coding: utf-8 -*-
"""细看薄荷健康搜索页结构（有响应 16KB，但链接模式不对）"""
import re
import urllib.request
import urllib.parse

UA = "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120 Mobile Safari/537.36"

food = "西兰花"
q = urllib.parse.quote(food)
html = urllib.request.urlopen(
    urllib.request.Request("https://www.boohee.com/food/search?keyword=%s" % q,
                           headers={"User-Agent": UA}), timeout=15
).read().decode("utf-8", errors="ignore")

print("长度:", len(html))
print("--- 所有 href（去重前 20）---")
for h in sorted(set(re.findall(r'href="([^"]+)"', html)))[:20]:
    print("  ", h[:90])

print("\n--- 找食物相关区块 ---")
# 薄荷的食物卡片一般是 <a href="/food/xxx">名字</a>
for pat in [r'/food/view/\d+', r'/food/\d+', r'item/\d+', r'"food_name"', r'食物']:
    ms = re.findall(pat, html)
    print("%-16s -> %d" % (pat, len(ms)))

print("\n--- 页面文本样本 ---")
text = re.sub(r"<script.*?</script>", "", html, flags=re.S)
text = re.sub(r"<[^>]+>", "|", text)
text = re.sub(r"\|+", "|", text)
print(text[:600])

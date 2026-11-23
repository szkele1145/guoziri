# -*- coding: utf-8 -*-
"""
实测 FoodSearchService 的搜索+解析逻辑（复刻 Kotlin 实现）
验证 DuckDuckGo HTML 搜索 + 营养表正则提取是否真的能拿到数据
"""
import re
import urllib.request
import urllib.parse

UA = "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36"
NUTRITION_KEYS = ["热量", "蛋白质", "脂肪", "碳水", "膳食纤维", "维生素c", "钙", "铁"]
UNIT_MAP = {"热量": "kcal|千卡|大卡", "蛋白质": "g|克", "脂肪": "g|克", "碳水": "g|克",
            "膳食纤维": "g|克", "维生素c": "mg|毫克", "钙": "mg|毫克", "铁": "mg|毫克"}


def fetch(url):
    req = urllib.request.Request(url, headers={"User-Agent": UA, "Accept": "text/html"})
    return urllib.request.urlopen(req, timeout=15).read().decode("utf-8", errors="ignore")


def search(food):
    q = urllib.parse.quote("%s 营养成分表 每100克" % food)
    html = fetch("https://html.duckduckgo.com/html/?q=%s" % q)
    # DDG 结果链接（result__a 的 href 是 uuencdoed 的，需解码）
    links = re.findall(r'class="result__a"[^>]*href="([^"]+)"', html)
    out = []
    for u in links[:5]:
        # DDG 链接形如 //duckduckgo.com/l/?uddg=<encoded>
        m = re.search(r"uddg=([^&]+)", u)
        if m:
            out.append(urllib.parse.unquote(m.group(1)))
        elif u.startswith("http"):
            out.append(u)
    return out[:3]


def parse_page(html):
    """复刻 Kotlin parseNutritionTable：先表格、后全文兜底"""
    result = {}
    # 1. 表格行
    for m in re.finditer(r"<tr[^>]*>.*?<t[dh][^>]*>([^<]+)</t[dh]>.*?<t[dh][^>]*>([^<]+)</t[dh]>.*?</tr>",
                         html, re.S | re.I):
        k, v = m.group(1).strip(), m.group(2).strip()
        p = parse_kv(k, v)
        if p:
            result[p[0]] = p[1]
    if len(result) >= 3:
        return result
    # 2. th/td
    for m in re.finditer(r"<th[^>]*>([^<]+)</th>\s*<td[^>]*>([^<]+)</td>", html, re.S | re.I):
        p = parse_kv(m.group(1).strip(), m.group(2).strip())
        if p:
            result[p[0]] = p[1]
    if len(result) >= 3:
        return result
    # 3. 全文兜底
    text = re.sub(r"<[^>]+>", " ", html)
    text = re.sub(r"\s+", " ", text)
    for key in NUTRITION_KEYS:
        pat = r"%s[:：]?\s*([\d.]+)\s*(?:%s)" % (key, UNIT_MAP[key])
        m = re.search(pat, text, re.I)
        if m:
            result[key] = float(m.group(1))
    return result


def parse_kv(key, value):
    k = key.strip().lower()
    matched = next((kk for kk in NUTRITION_KEYS if kk in k), None)
    if not matched:
        return None
    m = re.search(r"([\d.]+)", value)
    if not m:
        return None
    try:
        return (matched, float(m.group(1)))
    except ValueError:
        return None


if __name__ == "__main__":
    import sys
    food = sys.argv[1] if len(sys.argv) > 1 else "西兰花"
    print("=== 搜索: %s ===" % food)
    try:
        links = search(food)
        print("候选页面: %d 个" % len(links))
        for u in links:
            print("  -", u[:70])
        for i, link in enumerate(links):
            print("\n--- 解析第 %d 页 ---" % (i + 1))
            try:
                html = fetch(link)
                r = parse_page(html)
                if len(r) >= 3:
                    print("✅ 成功提取 %d 项:" % len(r))
                    for k, v in r.items():
                        print("    %s: %s" % (k, v))
                    break
                else:
                    print("⚠️ 仅 %d 项，尝试下一页" % len(r))
            except Exception as e:
                print("✗ 页面失败:", str(e)[:60])
    except Exception as e:
        print("搜索失败:", e)

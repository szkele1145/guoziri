# -*- coding: utf-8 -*-
"""
完整实测：百度搜索 → 解析跳转 → 抓营养页面 → 提取营养表
这是 FoodSearchService.kt 的 Python 原型验证
"""
import re
import urllib.request
import urllib.parse

UA = "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120 Mobile Safari/537.36"
NUTRITION_KEYS = ["热量", "蛋白质", "脂肪", "碳水", "膳食纤维", "维生素c", "钙", "铁"]
UNIT_MAP = {"热量": "kcal|千卡|大卡", "蛋白质": "g|克", "脂肪": "g|克", "碳水": "g|克",
            "膳食纤维": "g|克", "维生素c": "mg|毫克", "钙": "mg|毫克", "铁": "mg|毫克"}


def fetch(url, timeout=15):
    req = urllib.request.Request(url, headers={
        "User-Agent": UA,
        "Accept": "text/html,application/xhtml+xml",
        "Accept-Language": "zh-CN,zh;q=0.9",
    })
    return urllib.request.urlopen(req, timeout=timeout).read().decode("utf-8", errors="ignore")


def baidu_search(food):
    """百度搜索，返回真实 URL（跟随跳转）"""
    q = urllib.parse.quote("%s 营养成分表 每100克" % food)
    html = fetch("https://www.baidu.com/s?wd=%s&rn=10" % q)
    # 提取百度跳转链接
    raw = re.findall(r'href="(https?://www\.baidu\.com/link\?url=[^"]+)"', html)
    # 也直接提取真实站点链接（百度有时直接给目标 URL）
    direct = [u for u in re.findall(r'href="(https?://(?!www\.baidu|baidu\.com|m\.baidu|psstatic)[^"]+)"', html)
              if any(d in u for d in ["bohai", "baike", "food", "health", "msn", "xiangha", "yangsheng", "360", "sina", "sohu", "163", "qq", "cnhubei", "fy356"])]
    return raw[:6], direct[:6]


def resolve(url):
    """跟随百度跳转拿真实 URL"""
    try:
        req = urllib.request.Request(url, headers={"User-Agent": UA})
        resp = urllib.request.urlopen(req, timeout=12)
        return resp.geturl(), resp.read().decode("utf-8", errors="ignore")
    except Exception as e:
        return None, None


def parse_nutrition(html):
    result = {}
    # 表格行
    for m in re.finditer(r"<tr[^>]*>.*?<t[dh][^>]*>([^<]+)</t[dh]>.*?<t[dh][^>]*>([^<]+)</t[dh]>.*?</tr>",
                         html, re.S | re.I):
        p = parse_kv(m.group(1).strip(), m.group(2).strip())
        if p:
            result[p[0]] = p[1]
    if len(result) >= 3:
        return result
    # th/td
    for m in re.finditer(r"<th[^>]*>([^<]+)</th>\s*<td[^>]*>([^<]+)</td>", html, re.S | re.I):
        p = parse_kv(m.group(1).strip(), m.group(2).strip())
        if p:
            result[p[0]] = p[1]
    if len(result) >= 3:
        return result
    # 全文兜底
    text = re.sub(r"<[^>]+>", " ", html)
    text = re.sub(r"\s+", " ", text)
    for key in NUTRITION_KEYS:
        m = re.search(r"%s[:：]?\s*([\d.]+)\s*(?:%s)" % (key, UNIT_MAP[key]), text, re.I)
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
    jump_links, direct_links = baidu_search(food)
    print("跳转链接: %d, 直链: %d" % (len(jump_links), len(direct_links)))

    candidates = []
    for u in direct_links:
        candidates.append(u)
    for u in jump_links:
        real, _ = resolve(u)
        if real and "baidu" not in real:
            candidates.append(real)

    print("\n=== 依次解析（最多 4 页）===")
    for i, link in enumerate(candidates[:4]):
        print("\n[%d] %s" % (i + 1, link[:75]))
        try:
            html = fetch(link)
            r = parse_nutrition(html)
            if len(r) >= 3:
                print("  ✅ 成功提取 %d 项:" % len(r))
                for k, v in r.items():
                    print("      %s: %s" % (k, v))
                break
            print("  ⚠️ 仅 %d 项" % len(r))
        except Exception as e:
            print("  ✗ 失败:", str(e)[:60])
    else:
        print("\n❌ 所有页面都没提取到完整营养表")

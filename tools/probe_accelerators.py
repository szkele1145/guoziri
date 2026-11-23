# -*- coding: utf-8 -*-
"""实测各加速源对不同 URL 的拼接效果，找出真正可用的组合"""
import urllib.request

UA = {"User-Agent": "Guoziri-App"}

# 加速源：前缀 + 原始 URL
ACCELERATORS = [
    ("ghfast.top", "https://ghfast.top/"),
    ("ghproxy.net", "https://ghproxy.net/"),
    ("gh-proxy.com", "https://gh-proxy.com/"),
    ("moeyy", "https://github.moeyy.xyz/"),
    ("gitmirror", "https://hub.gitmirror.com/"),
    ("ghproxy.com", "https://ghproxy.com/"),
    ("直连", ""),
]

TARGETS = [
    ("feed", "https://github.com/szkele1145/guoziri/releases.atom", "text/html"),
    ("api", "https://api.github.com/repos/szkele1145/guoziri/releases/latest", "application/vnd.github+json"),
    ("assets", "https://github.com/szkele1145/guoziri/releases/expanded_assets/v1.3", "text/html"),
    ("apk", "https://github.com/szkele1145/guoziri/releases/download/v1.3/guoziri-v1.3.apk", "*/*"),
]


def probe(url, accept):
    """返回 (status, bytes, body_head)"""
    try:
        h = dict(UA)
        h["Accept"] = accept
        req = urllib.request.Request(url, headers=h)
        resp = urllib.request.urlopen(req, timeout=15)
        data = resp.read()
        return resp.status, len(data), data
    except Exception as e:
        return None, 0, str(e)[:60].encode()


def valid(kind, body):
    """校验响应内容是否有效（防止代理返回自己的广告页）"""
    if kind == "apk":
        return body[:4] == b"PK\x03\x04"
    try:
        text = body.decode("utf-8", errors="ignore")
    except Exception:
        return False
    if kind == "feed":
        return "<entry>" in text and "v1." in text
    if kind == "api":
        return '"tag_name"' in text
    if kind == "assets":
        return "releases/download" in text and ".apk" in text
    return False


print("=" * 78)
for kind, origin, accept in TARGETS:
    print("\n### %s" % kind)
    print("  原地址: %s" % origin[:72])
    for name, prefix in ACCELERATORS:
        url = prefix + origin
        status, length, body = probe(url, accept)
        if status == 200:
            ok = "有效" if valid(kind, body) else "内容异常"
        elif status:
            ok = "HTTP %d" % status
        else:
            ok = "连不上"
        print("  %-12s 状态=%-4s 字节=%-9s %s" % (name, status or "-", length, ok))

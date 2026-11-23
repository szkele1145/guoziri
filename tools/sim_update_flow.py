# -*- coding: utf-8 -*-
"""
复刻 UpdateManager.kt 的完整检查逻辑，实测每一步，找出真正的失败点。
"""
import json
import re
import urllib.request

UA = {"User-Agent": "Guoziri-App"}

# 与 App 里一致的加速源列表（含最后的直连兜底）
ACCELERATORS = [
    "https://ghfast.top/",
    "https://ghproxy.net/",
    "https://gh-proxy.com/",
    "https://github.moeyy.xyz/",
    "https://hub.gitmirror.com/",
    "",
]

API_URL = "https://api.github.com/repos/szkele1145/guoziri/releases/latest"
FEED_URL = "https://github.com/szkele1145/guoziri/releases.atom"


def fetch(url, accept):
    """复刻 fetchText"""
    try:
        h = dict(UA)
        h["Accept"] = accept
        req = urllib.request.Request(url, headers=h)
        resp = urllib.request.urlopen(req, timeout=12)
        if resp.status != 200:
            return None
        return resp.read()
    except Exception:
        return None


def parse_feed_tag(feed):
    """复刻 parseFeedTag：只在 <entry> 块里找"""
    if not feed:
        return None
    text = feed.decode("utf-8", errors="ignore")
    i = text.find("<entry>")
    if i < 0:
        return None
    m = re.search(r"<id>[^<]*/([^/<]+)</id>", text[i:])
    return m.group(1) if m else None


def parse_release_json(body):
    """复刻 parseRelease"""
    if not body:
        return None
    try:
        obj = json.loads(body.decode("utf-8"))
        tag = obj.get("tag_name")
        if not tag:
            return None
        url, size = "", 0
        for a in obj.get("assets", []):
            if a.get("name", "").endswith(".apk"):
                url = a.get("browser_download_url", "")
                size = a.get("size", 0)
                break
        if not url:
            return None
        return {"tag": tag, "body": obj.get("body", ""), "url": url, "size": size}
    except Exception:
        return None


def fetch_apk_url(tag):
    """复刻 fetchApkUrl：走 expanded_assets"""
    body = fetch(
        "https://github.com/szkele1145/guoziri/releases/expanded_assets/" + tag,
        "text/html",
    )
    if not body:
        return None
    m = re.search(rb'href="([^"]*/releases/download/[^"]*\.apk)"', body)
    if not m:
        return None
    url = m.group(1).decode("utf-8").replace("&amp;", "&")
    if url.startswith("/"):
        url = "https://github.com" + url
    return url


def is_newer(latest, current):
    def parts(v):
        return [int(x) if x.isdigit() else 0 for x in v.replace("v", "").split(".")]

    l, c = parts(latest), parts(current)
    for i in range(max(len(l), len(c))):
        lv = l[i] if i < len(l) else 0
        cv = c[i] if i < len(c) else 0
        if lv != cv:
            return lv > cv
    return False


def check(current_version="1.3"):
    """完整复刻 checkUpdate"""
    print("【第1步】Atom feed 取版本号")
    latest_tag = None
    for acc in ACCELERATORS:
        body = fetch(acc + FEED_URL, "application/atom+xml")
        if not body:
            print("   ✗ %-30s 连不上" % (acc or "直连"))
            continue
        latest_tag = parse_feed_tag(body)
        if latest_tag:
            print("   ✓ %-30s → %s" % (acc or "直连", latest_tag))
            break
    if not latest_tag:
        print("   → 全部失败\n")
        return None

    print("\n【第2步】REST API 取完整信息")
    for acc in ACCELERATORS:
        body = fetch(acc + API_URL, "application/vnd.github+json")
        if not body:
            print("   ✗ %-30s 连不上" % (acc or "直连"))
            continue
        info = parse_release_json(body)
        if info:
            print("   ✓ %-30s → tag=%s apk=%s" % (acc or "直连", info["tag"], info["url"][-40:]))
            return info
        print("   ✗ %-30s 内容异常" % (acc or "直连"))

    print("\n【第3步】expanded_assets 兜底")
    url = fetch_apk_url(latest_tag)
    if url:
        print("   ✓ 直连 expanded_assets → %s" % url[-40:])
        return {"tag": latest_tag, "body": "", "url": url, "size": 0}
    print("   ✗ 兜底也失败\n")
    return None


def download_probe(url):
    """测试 APK 下载（只看能否拿到 PK 头）"""
    try:
        h = dict(UA)
        h["Accept"] = "*/*"
        req = urllib.request.Request(url, headers=h)
        resp = urllib.request.urlopen(req, timeout=20)
        head = resp.read(4)
        return resp.status, resp.headers.get("Content-Length"), head == b"PK\x03\x04"
    except Exception as e:
        return None, 0, False


if __name__ == "__main__":
    print("=" * 70)
    print("模拟 App 检查更新流程（当前版本 1.3，最新应为 v1.3 → 已是最新）")
    print("=" * 70)
    info = check("1.3")
    if info:
        print("\n【第4步】下载 APK 可用性")
        for acc in ACCELERATORS:
            status, clen, ok = download_probe(acc + info["url"])
            print("   %-30s 状态=%-4s 大小=%-10s PK头=%s"
                  % (acc or "直连", status or "-", clen or "-", ok))

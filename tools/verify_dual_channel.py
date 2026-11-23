# -*- coding: utf-8 -*-
"""
验证 App 的双通道检查逻辑：
  查 releases 通道 + test 通道，各自能否拿到 tag 和 APK 链接
"""
import json
import re
import urllib.request

UA = {"User-Agent": "Guoziri-App"}
OWNER, REPO = "szkele1145", "guoziri"

META_ACC = ["https://gh-proxy.com/", ""]
DL_ACC = ["https://ghproxy.net/", "https://gh-proxy.com/", ""]


def fetch(url, accept="application/vnd.github+json"):
    try:
        h = dict(UA)
        h["Accept"] = accept
        req = urllib.request.Request(url, headers=h)
        resp = urllib.request.urlopen(req, timeout=15)
        if resp.status != 200:
            return None
        return resp.read()
    except Exception:
        return None


def parse_feed_tag(feed):
    i = feed.find(b"<entry>")
    if i < 0:
        return None
    m = re.search(rb"<id>[^<]*/([^/<]+)</id>", feed[i:])
    return m.group(1).decode() if m else None


def parse_release(body, channel):
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
        return {"tag": tag, "url": url, "size": size, "channel": channel}
    except Exception:
        return None


def fetch_apk_url(tag):
    body = fetch(
        "https://github.com/%s/%s/releases/expanded_assets/%s" % (OWNER, REPO, tag),
        "text/html")
    if not body:
        return None
    m = re.search(rb'href="([^"]*/releases/download/[^"]*\.apk)"', body)
    if not m:
        return None
    url = m.group(1).decode().replace("&amp;", "&")
    if url.startswith("/"):
        url = "https://github.com" + url
    return url


def query_channel(channel):
    print("\n### 通道 %s" % channel)
    latest = None
    for acc in META_ACC:
        body = fetch(acc + "https://github.com/%s/%s/releases.atom" % (OWNER, REPO),
                     "application/atom+xml")
        if not body:
            print("   feed ✗ %s" % (acc or "直连"))
            continue
        latest = parse_feed_tag(body)
        print("   feed ✓ %s → %s" % (acc or "直连", latest))
        if latest:
            break

    for acc in META_ACC:
        body = fetch(acc + "https://api.github.com/repos/%s/%s/releases/latest" % (OWNER, REPO))
        if not body:
            print("   api  ✗ %s" % (acc or "直连"))
            continue
        info = parse_release(body, channel)
        if info:
            print("   api  ✓ %s → tag=%s apk=%s" % (acc or "直连", info["tag"], info["url"].split("/")[-1]))
            return info
        print("   api  ✗ 内容异常 %s" % (acc or "直连"))

    if latest:
        url = fetch_apk_url(latest)
        if url:
            print("   assets ✓ → %s" % url.split("/")[-1])
            return {"tag": latest, "url": url, "size": 0, "channel": channel}
        print("   assets ✗")
    return None


def download_probe(url):
    try:
        h = dict(UA)
        h["Accept"] = "*/*"
        req = urllib.request.Request(url, headers=h)
        resp = urllib.request.urlopen(req, timeout=20)
        head = resp.read(4)
        return resp.status, resp.headers.get("Content-Length"), head == b"PK\x03\x04"
    except Exception:
        return None, 0, False


print("=" * 72)
results = []
for ch in ["releases", "test"]:
    results.append((ch, query_channel(ch)))

print("\n" + "=" * 72)
print("汇总：")
for ch, info in results:
    if info:
        print("  %-10s → %s" % (ch, info["tag"]))
    else:
        print("  %-10s → 未查到" % ch)

print("\n下载可用性：")
for ch, info in results:
    if not info:
        continue
    for acc in DL_ACC:
        st, cl, ok = download_probe(acc + info["url"])
        print("  %-10s %-24s 状态=%-4s 大小=%-10s PK=%s"
              % (ch, (acc or "直连")[:24], st or "-", cl or "-", ok))

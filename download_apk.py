# -*- coding: utf-8 -*-
"""下载 artifact zip：带 Range 续传，代理断流也能补完"""
import json
import os
import time
import urllib.request

TOKEN = open(r"F:\vibe coding工程\sfx\.ghtoken").read().strip()
REPO = "szkele1145/guoziri"
DESKTOP = r"C:\Users\一只屑\Desktop"
OUT = os.path.join(DESKTOP, "guoziri-apk.zip")


def get_json(url):
    req = urllib.request.Request(url)
    req.add_header("Authorization", "Bearer " + TOKEN)
    req.add_header("Accept", "application/vnd.github+json")
    with urllib.request.urlopen(req) as r:
        return json.loads(r.read())


def resolve_redirect(url):
    """拿 302 的真实下载地址"""
    req = urllib.request.Request(url)
    req.add_header("Authorization", "Bearer " + TOKEN)

    class NoRedirect(urllib.request.HTTPRedirectHandler):
        def redirect_request(self, *a, **k):
            return None

    try:
        urllib.request.build_opener(NoRedirect).open(req).read()
    except urllib.error.HTTPError as e:
        if e.code in (301, 302, 303, 307):
            return e.headers["Location"]
        raise


def download_with_resume(url, out):
    """Range 续传循环：每次断掉从已写字节继续，直到完整"""
    expected = None
    for attempt in range(20):
        have = os.path.getsize(out) if os.path.exists(out) else 0
        req = urllib.request.Request(url)
        if have:
            req.add_header("Range", "bytes=%d-" % have)
        try:
            with urllib.request.urlopen(req, timeout=60) as r:
                if expected is None:
                    cr = r.headers.get("Content-Range")
                    if cr and "/" in cr:
                        expected = int(cr.split("/")[-1])
                    else:
                        expected = have + int(r.headers.get("Content-Length", 0))
                mode = "ab" if have and r.status == 206 else "wb"
                with open(out, mode) as f:
                    total = 0
                    while True:
                        chunk = r.read(65536)
                        if not chunk:
                            break
                        f.write(chunk)
                        total += len(chunk)
        except Exception as e:
            print("  attempt %d 断于 %d/%s: %s" % (attempt + 1, os.path.getsize(out) if os.path.exists(out) else 0, expected, e))
            time.sleep(2)
            continue
        size = os.path.getsize(out)
        print("  attempt %d 完成，已有 %d/%s" % (attempt + 1, size, expected))
        if expected and size >= expected:
            return size
    raise SystemExit("重试 20 次仍未完整")


# 1. 找到最新成功 run 的 artifact
runs = get_json("https://api.github.com/repos/%s/actions/runs?per_page=3" % REPO)
run = next(r for r in runs["workflow_runs"] if r["conclusion"] == "success")
print("run", run["id"], "success @", run["head_sha"][:7])

arts = get_json("https://api.github.com/repos/%s/actions/runs/%d/artifacts" % (REPO, run["id"]))
art = next(a for a in arts["artifacts"] if a["name"].endswith("-apk"))
print("artifact:", art["name"], "%.1f MB" % (art["size_in_bytes"] / 1048576))

# 2. 续传下载
real = resolve_redirect("https://api.github.com/repos/%s/actions/artifacts/%d/zip" % (REPO, art["id"]))
size = download_with_resume(real, OUT)
print("下载完整: %.1f MB" % (size / 1048576))

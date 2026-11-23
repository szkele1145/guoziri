# -*- coding: utf-8 -*-
"""发布 v1.3.1 到 releases 正式通道（带重试）"""
import json
import time
import urllib.request

TOKEN = open(r"F:\vibe coding工程\sfx\.ghtoken").read().strip()
REPO = "szkele1145/guoziri"
APK = r"C:\Users\一只屑\Desktop\guoziri-apk-extract\app-release.apk"
VERSION = "1.3.1"
FILENAME = "guoziri-v%s.apk" % VERSION

BODY = """## 过日子 v%s（正式版）

稳定通道，**推荐更新**。

### 本次更新
- 🆕 食物营养智能填充：记账时库外食物一键自动填营养
  - 三层降级：内置库匹配 → AI 联网查询 → 分类估算
  - 全程日志弹窗，Agent 每步可见
- 🐛 修复：版本号显示错误（vreleases → v%s）
- 🐛 修复：底部 tab 图标偏上
- 🐛 修复：下载 16 并发超时，改为 32 块 × 6 并发（12MB/30秒零失败）

### 升级方式
设置 → 数据 → 检查更新 → 选择「正式版」→ 立即更新

> 数据保存在本机，升级后账本、日记、设置全部保留
""" % (VERSION, VERSION)


def req(url, data=None, method=None, headers=None, raw=False, tries=5):
    last = None
    for i in range(tries):
        try:
            h = {"Authorization": "Bearer " + TOKEN, "Accept": "application/vnd.github+json"}
            if headers:
                h.update(headers)
            r = urllib.request.Request(url, data=data, method=method, headers=h)
            resp = urllib.request.urlopen(r, timeout=300)
            body = resp.read()
            return body if raw else json.loads(body or b"{}")
        except Exception as e:
            last = e
            print("  retry %d: %s" % (i + 1, str(e)[:70]))
            time.sleep(3 * (i + 1))
    raise last


print("=== 发布 v%s 到 releases ===" % VERSION)
rel = req("https://api.github.com/repos/%s/releases/tags/releases" % REPO)
rid = rel["id"]
print("Release id:", rid)

for a in rel["assets"]:
    try:
        req("https://api.github.com/repos/%s/releases/assets/%d" % (REPO, a["id"]), method="DELETE")
        print("  删除旧 asset:", a["name"])
    except Exception as e:
        print("  删除失败(可能已删):", str(e)[:50])

data = open(APK, "rb").read()
url = "https://uploads.github.com/repos/%s/releases/%d/assets?name=%s" % (REPO, rid, FILENAME)
req(url, data=data, method="POST",
    headers={"Content-Type": "application/vnd.android.package-archive"}, raw=True)
print("  上传完成: %s (%.2f MB)" % (FILENAME, len(data) / 1048576))

req("https://api.github.com/repos/%s/releases/%d" % (REPO, rid),
    data=json.dumps({"name": "过日子 v%s" % VERSION, "body": BODY,
                     "draft": False, "prerelease": False}, ensure_ascii=False).encode(),
    method="PATCH")
print("  Release 标题/说明已更新")

print("\n=== 最终校验 ===")
rel2 = req("https://api.github.com/repos/%s/releases/tags/releases" % REPO)
print("  tag:", rel2["tag_name"], " name:", rel2["name"])
for a in rel2["assets"]:
    print("  asset:", a["name"], "%.2f MB" % (a["size"] / 1048576))

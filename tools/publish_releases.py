# -*- coding: utf-8 -*-
"""
重建 GitHub Release 双通道：
  - releases  正式版（稳定通道）
  - test      测试版（预发布，标注不建议更新）
并把当前构建好的 APK 上传到两个通道。
"""
import json
import os
import urllib.request

TOKEN = open(r"F:\vibe coding工程\sfx\.ghtoken").read().strip()
REPO = "szkele1145/guoziri"
APK = r"C:\Users\一只屑\Desktop\guoziri-apk-extract\app-release.apk"
VERSION = "1.3"
TEST_VERSION = "1.3.0-test1"


def req(url, data=None, method=None, headers=None, raw=False):
    h = {"Authorization": "Bearer " + TOKEN, "Accept": "application/vnd.github+json"}
    if headers:
        h.update(headers)
    r = urllib.request.Request(url, data=data, method=method, headers=h)
    resp = urllib.request.urlopen(r, timeout=240)
    body = resp.read()
    return body if raw else json.loads(body or b"{}")


def list_releases():
    return req("https://api.github.com/repos/%s/releases?per_page=20" % REPO)


def ensure_release(tag, name, body, prerelease):
    """确保 tag 对应的 Release 存在，返回 release id"""
    for r in list_releases():
        if r["tag_name"] == tag:
            print("  已有 Release %s (id=%d)" % (tag, r["id"]))
            # 清理旧 asset
            for a in r["assets"]:
                try:
                    req("https://api.github.com/repos/%s/releases/assets/%d" % (REPO, a["id"]),
                        method="DELETE")
                    print("    删除旧 asset:", a["name"])
                except Exception as e:
                    print("    删除失败:", e)
            return r["id"]
    print("  创建 Release %s" % tag)
    rel = req("https://api.github.com/repos/%s/releases" % REPO,
              data=json.dumps({
                  "tag_name": tag,
                  "name": name,
                  "body": body,
                  "draft": False,
                  "prerelease": prerelease,
              }, ensure_ascii=False).encode(),
              method="POST")
    return rel["id"]


def upload(rid, filename):
    data = open(APK, "rb").read()
    url = ("https://uploads.github.com/repos/%s/releases/%d/assets?name=%s"
           % (REPO, rid, filename))
    req(url, data=data, method="POST",
        headers={"Content-Type": "application/vnd.android.package-archive"}, raw=True)
    print("  上传 %s (%.2f MB)" % (filename, len(data) / 1048576))


print("=== 正式版通道 releases ===")
rid = ensure_release(
    "releases",
    "过日子 v%s" % VERSION,
    """## 过日子 v%s（正式版）

稳定通道，**推荐更新**。

### 本次更新
- 桌面图标全新设计：品牌蓝「日」字 + 记账流水线条
- 底部弹窗不再跳动（自绘 AppBottomSheet 替代 ModalBottomSheet）
- 输入框不再被输入法遮挡
- 图标渲染统一，小尺寸更清晰
- 走势图数值标签不再压住折线
- 自动更新支持国内加速源 + 断点续传 + 通知栏进度
- 食物库扩充至 200+ 种

### 升级方式
设置 → 数据 → 检查更新 → 选择「正式版」→ 立即更新

> 数据保存在本机，升级后账本、日记、设置全部保留
""" % VERSION,
    prerelease=False,
)
upload(rid, "guoziri-v%s.apk" % VERSION)

print()
print("=== 测试版通道 test ===")
rid = ensure_release(
    "test",
    "过日子 v%s" % TEST_VERSION,
    """## 日子 v%s（测试版）

⚠️ **测试版，不建议更新**

抢先体验通道，可能存在未修复的问题。正式稳定版请看 `releases` 通道。

### 本次更新
- 与正式版 v%s 相同的改动
- 额外包含实验性功能

### 升级方式
设置 → 数据 → 检查更新 → 选择「测试版」→ 立即更新
""" % (TEST_VERSION, VERSION),
    prerelease=True,
)
upload(rid, "guoziri-v%s.apk" % TEST_VERSION)

print()
print("=== 最终状态 ===")
for r in list_releases():
    print("  tag=%-10s prerelease=%-6s assets=%s"
          % (r["tag_name"], r["prerelease"], [a["name"] for a in r["assets"]]))

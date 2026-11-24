# -*- coding: utf-8 -*-
"""发布 v1.3.4 到 releases 通道"""
import json
import re
import urllib.request

TOKEN = open(r"F:\vibe coding工程\sfx\.ghtoken").read().strip()
REPO = "szkele1145/guoziri"
APK = r"C:\Users\一只屑\Desktop\guoziri-apk-extract\app-release.apk"
VERSION = "1.3.4"


def req(url, data=None, method=None, headers=None, raw=False):
    h = {"Authorization": "Bearer " + TOKEN, "Accept": "application/vnd.github+json"}
    if headers:
        h.update(headers)
    r = urllib.request.Request(url, data=data, method=method, headers=h)
    resp = urllib.request.urlopen(r, timeout=240)
    return resp.read() if raw else json.loads(resp.read() or b"{}")


print("=== 发布 v1.3.4 到 releases ===")
rel = req("https://api.github.com/repos/%s/releases/tags/releases" % REPO)
rid = rel["id"]
print("Release id:", rid)

# 删除旧 asset
for a in rel["assets"]:
    try:
        req("https://api.github.com/repos/%s/releases/assets/%d" % (REPO, a["id"]), method="DELETE")
        print("删除旧 asset:", a["name"])
    except Exception as e:
        print("删除失败:", str(e)[:50])

# 上传新 APK
data = open(r"C:\Users\一只屑\Desktop\guoziri-apk-extract\app-release.apk", "rb").read()
url = "https://uploads.github.com/repos/%s/releases/%d/assets?name=guoziri-v1.3.4.apk" % (REPO, rid)
req(url, data=data, method="POST",
    headers={"Content-Type": "application/vnd.android.package-archive"}, raw=True)
print("上传完成: guoziri-v1.3.4.apk (%.2f MB)" % (len(data) / 1048576))

# 更新 Release 说明
req("https://api.github.com/repos/%s/releases/%d" % (REPO, rid),
    data=json.dumps({
        "name": "过日子 v1.3.4",
        "body": """## 过日子 v1.3.4（正式版）

稳定通道，**推荐更新**。

### 本次更新
- 食材库扩充至 1650 种（中国食物成分表第6版）
- 食物联想菜单：输入联想、别名支持
- 价格互算：单价/总价/斤/克 四字段联动
- 版本检查修复：语义化版本比较
- 智能填充三层 Agent：库内→AI→估算
- 空指针安全修复
- 版本检查修复：语义化版本比较

> 数据保存在本机，升级后账本、日记、设置全部保留""",
        "draft": False,
        "prerelease": False
    }, ensure_ascii=False).encode(),
    method="PATCH")
print("Release 说明已更新")

# 校验
rel2 = req("https://api.github.com/repos/%s/releases/tags/releases" % REPO)
print("tag:", rel2["tag_name"], "| name:", rel2["name"])
for a in rel2["assets"]:
    print("asset:", a["name"], "%.2f MB" % (a["size"] / 1048576))

print("完成！")
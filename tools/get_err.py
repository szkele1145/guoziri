# -*- coding: utf-8 -*-
"""获取最近一次失败的 Build APK 日志中的编译错误"""
import io
import json
import time
import urllib.request
import zipfile

tok = open(r"F:\vibe coding工程\sfx\.ghtoken").read().strip()


def req(u):
    return urllib.request.urlopen(
        urllib.request.Request(u, headers={
            "Authorization": "Bearer " + tok,
            "Accept": "application/vnd.github+json",
        }), timeout=60).read()


for attempt in range(4):
    try:
        d = json.loads(req("https://api.github.com/repos/szkele1145/guoziri/actions/runs?per_page=5"))
        rid = None
        for r in d["workflow_runs"]:
            if r["name"] == "Build APK" and r["conclusion"] == "failure":
                rid = r["id"]
                break
        if not rid:
            print("没有失败的 Build APK")
            break
        log = req("https://api.github.com/repos/szkele1145/guoziri/actions/runs/%d/logs" % rid)
        z = zipfile.ZipFile(io.BytesIO(log))
        for n in z.namelist():
            c = z.read(n).decode("utf-8", errors="replace")
            if "e: file" in c:
                for l in c.split("\n"):
                    if "e: file" in l:
                        print(l.split("app/")[-1][:180])
                break
        break
    except Exception as e:
        print("retry", attempt + 1, str(e)[:60])
        time.sleep(20)

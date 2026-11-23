# -*- coding: utf-8 -*-
"""获取完整的 CI 错误日志"""
import io
import json
import sys
import urllib.request
import zipfile

tok = open(r"F:\vibe coding工程\sfx\.ghtoken").read().strip()


def req(url, headers=None):
    h = {"Authorization": "Bearer " + tok, "Accept": "application/vnd.github+json"}
    if headers:
        h.update(headers)
    return urllib.request.urlopen(urllib.request.Request(url, headers=h), timeout=60).read()


d = json.loads(req("https://api.github.com/repos/szkele1145/guoziri/actions/runs?per_page=1"))
run = d["workflow_runs"][0]
print("Run:", run["name"], run["head_sha"][:8], run["status"], run["conclusion"])

log = req("https://api.github.com/repos/szkele1145/guoziri/actions/runs/%d/logs" % run["id"])
z = zipfile.ZipFile(io.BytesIO(log))
for n in z.namelist():
    c = z.read(n).decode("utf-8", errors="replace")
    if "NutritionDB" in c and ("error" in c.lower() or "e: file" in c.lower()):
        lines = c.split("\n")
        for i, l in enumerate(lines):
            if "NutritionDB" in l and ("error" in l.lower() or "e: file" in l.lower()):
                start = max(0, i - 5)
                end = min(len(lines), i + 10)
                for j in range(start, end):
                    print(f"{j+1}: {lines[j][:200]}")
                print("---")
        break
# -*- coding: utf-8 -*-
"""下载并打印 CI 构建日志里的错误"""
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
    if "e: file" in c or "FAILURE" in c or "Error:" in c:
        lines = c.split("\n")
        printed = 0
        for i, l in enumerate(lines):
            low = l.lower()
            if "e: file" in l or "error:" in low and ".kt" in low or "what went wrong" in low:
                start = max(0, i - 2)
                for j in range(start, min(len(lines), i + 6)):
                    print(lines[j][:220])
                print("---")
                printed += 1
                if printed > 12:
                    break
        break

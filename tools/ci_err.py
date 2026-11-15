# -*- coding: utf-8 -*-
"""拉取最新一次 Actions 失败的编译错误（可复用）"""
import json
import time
import urllib.request

TOKEN = open(r"F:\vibe coding工程\sfx\.ghtoken").read().strip()
REPO = "szkele1145/guoziri"


def get(url):
    last = None
    for i in range(5):
        req = urllib.request.Request(url)
        req.add_header("Authorization", "Bearer " + TOKEN)
        req.add_header("Accept", "application/vnd.github+json")
        try:
            with urllib.request.urlopen(req, timeout=30) as r:
                return json.loads(r.read())
        except Exception as e:
            last = e
            time.sleep(2 * (i + 1))
    raise last


def log_text(job_id):
    req = urllib.request.Request("https://api.github.com/repos/%s/actions/jobs/%d/logs" % (REPO, job_id))
    req.add_header("Authorization", "Bearer " + TOKEN)

    class NoRedirect(urllib.request.HTTPRedirectHandler):
        def redirect_request(self, *a, **k):
            return None

    try:
        return urllib.request.build_opener(NoRedirect).open(req).read()
    except urllib.error.HTTPError as e:
        if e.code in (301, 302, 303, 307):
            return urllib.request.urlopen(e.headers["Location"]).read()
        raise


run = get("https://api.github.com/repos/%s/actions/runs?per_page=1" % REPO)["workflow_runs"][0]
print("run %s %s/%s" % (run["head_sha"][:8], run["status"], run["conclusion"]))
jobs = get("https://api.github.com/repos/%s/actions/runs/%d/jobs" % (REPO, run["id"]))
text = log_text(jobs["jobs"][0]["id"]).decode("utf-8", errors="replace")

n = 0
for line in text.split("\n"):
    if "e: file://" in line:
        msg = line.split("e: file://")[1]
        msg = msg.replace("/home/runner/work/guoziri/guoziri/android-native/app/src/main/java/com/guoziri/app/", "")
        print("  " + msg[:280])
        n += 1
        if n > 60:
            break
if n == 0:
    print("（无 e: 行，末尾 25 行）")
    for line in text.split("\n")[-25:]:
        print("  " + line[:240])

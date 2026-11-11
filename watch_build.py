# -*- coding: utf-8 -*-
"""盯 Actions 构建并下载 APK（可复用版）"""
import json
import os
import time
import urllib.request

TOKEN = open(r"F:\vibe coding工程\sfx\.ghtoken").read().strip()
REPO = "szkele1145/guoziri"
DESKTOP = r"C:\Users\一只屑\Desktop"


def get(url, raw=False):
    req = urllib.request.Request(url)
    req.add_header("Authorization", "Bearer " + TOKEN)
    req.add_header("Accept", "application/vnd.github+json")
    with urllib.request.urlopen(req) as r:
        return r.read() if raw else json.loads(r.read())


def latest_run():
    return get("https://api.github.com/repos/%s/actions/runs?per_page=1" % REPO)["workflow_runs"][0]


def wait_done(timeout_s=1500):
    t0 = time.time()
    while time.time() - t0 < timeout_s:
        run = latest_run()
        print("[%5ds] %s / %s" % (time.time() - t0, run["status"], run["conclusion"]))
        if run["status"] == "completed":
            return run
        time.sleep(20)
    raise SystemExit("超时")


def fetch_logs_following_redirect(url):
    req = urllib.request.Request(url)
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


def get_failed_log(run_id):
    jobs = get("https://api.github.com/repos/%s/actions/runs/%d/jobs" % (REPO, run_id))
    job_id = jobs["jobs"][0]["id"]
    text = fetch_logs_following_redirect(
        "https://api.github.com/repos/%s/actions/jobs/%d/logs" % (REPO, job_id)
    ).decode("utf-8", errors="replace")
    errs = [l[:300] for l in text.split("\n")
            if "error:" in l.lower() or "FAILURE" in l or "Caused by" in l]
    return errs[:40]


def download_apk(run_id):
    arts = get("https://api.github.com/repos/%s/actions/runs/%d/artifacts" % (REPO, run_id))
    for a in arts["artifacts"]:
        if a["name"].endswith("-apk"):
            data = fetch_logs_following_redirect(
                "https://api.github.com/repos/%s/actions/artifacts/%d/zip" % (REPO, a["id"]))
            zpath = os.path.join(DESKTOP, "guoziri-apk.zip")
            with open(zpath, "wb") as f:
                f.write(data)
            print("APK zip: %s (%.1f MB)" % (zpath, len(data) / 1048576))
            return zpath
    print("无 artifact")


run = wait_done()
if run["conclusion"] == "success":
    download_apk(run["id"])
else:
    print("构建失败：")
    for e in get_failed_log(run["id"]):
        print(" ", e)

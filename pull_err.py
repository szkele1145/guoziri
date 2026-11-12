import watch_build as w

jobs = w.get("https://api.github.com/repos/%s/actions/runs/43163935938/jobs" % w.REPO)
jid = jobs["jobs"][0]["id"]
text = w.fetch_logs_following_redirect(
    "https://api.github.com/repos/%s/actions/jobs/%d/logs" % (w.REPO, jid)
).decode("utf-8", errors="replace")
lines = text.split("\n")
for i, l in enumerate(lines):
    if "AAR metadata" in l or "minCompileSdk" in l or "issues were found" in l:
        for ctx in lines[i: i + 14]:
            print(ctx[:280])
        break

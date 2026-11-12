import watch_build as w

jobs = w.get("https://api.github.com/repos/%s/actions/runs/43163098963/jobs" % w.REPO)
jid = jobs["jobs"][0]["id"]
text = w.fetch_logs_following_redirect(
    "https://api.github.com/repos/%s/actions/jobs/%d/logs" % (w.REPO, jid)
).decode("utf-8", errors="replace")
lines = text.split("\n")
idx = None
for i, l in enumerate(lines):
    if "What went wrong" in l:
        idx = i
        break
if idx:
    for ctx in lines[idx: idx + 25]:
        print(ctx[:280])
else:
    print("no What-went-wrong; tail:")
    for ctx in lines[-40:]:
        print(ctx[:280])

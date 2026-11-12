import watch_build as w
import re

jobs = w.get("https://api.github.com/repos/%s/actions/runs/43164249592/jobs" % w.REPO)
jid = jobs["jobs"][0]["id"]
text = w.fetch_logs_following_redirect(
    "https://api.github.com/repos/%s/actions/jobs/%d/logs" % (w.REPO, jid)
).decode("utf-8", errors="replace")

# 宽松抓取所有含 .kt 的行（CI 编译错误可能不带 "e: " 前缀而是内嵌）
hits = []
for l in text.split("\n"):
    if re.search(r"\.kt:\d+", l):
        hits.append(l[:300])
print("kt lines:", len(hits))
for h in hits[:35]:
    print(h)

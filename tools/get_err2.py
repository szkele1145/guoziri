import json,urllib.request
tok=open(r"F:\vibe coding工程\sfx\.ghtoken").read().strip()
req=urllib.request.Request("https://api.github.com/repos/szkele1145/guoziri/actions/runs/45329200658/jobs", headers={"Authorization":"Bearer "+tok,"Accept":"application/vnd.github+json"})
d=json.loads(urllib.request.urlopen(req,timeout=30).read())
for job in d["jobs"]:
    if job["name"]=="Build APK" and job["conclusion"]=="failure":
        print("job_id:", job["id"])
        log_url = job.get("logs_url") or "https://api.github.com/repos/szkele1145/guoziri/actions/jobs/"+str(job["id"])+"/logs"
        print("log_url:", log_url)
        log=urllib.request.urlopen(urllib.request.Request(log_url, headers={"Authorization":"Bearer "+tok,"Accept":"application/vnd.github+json"})).read().decode("utf-8",errors="replace")
        lines=log.split("\n")
        for i,l in enumerate(lines):
            if "e: file" in l or ("error" in l.lower() and ".kt" in l):
                for j in range(max(0,i-5),min(len(lines),i+10)):
                    print(lines[j][:200])
                print("---")
                break
        break
import subprocess

edge = r"C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe"
import os
if not os.path.exists(edge):
    edge = r"C:\Program Files\Microsoft\Edge\Application\msedge.exe"

out = subprocess.check_output([
    edge, "--headless", "--disable-gpu", "--dump-dom",
    "--virtual-time-budget=15000",
    "http://127.0.0.1:5000/?guideStep=5"
], timeout=60)

dom = out.decode("utf-8", errors="replace")
i = dom.find("guide-ring")
print("ring 段:", dom[max(0, i - 40): i + 300] if i >= 0 else "无 ring")
j = dom.find("ring-wrap")
print("营养页 ring-wrap:", "存在" if j >= 0 else "未渲染")

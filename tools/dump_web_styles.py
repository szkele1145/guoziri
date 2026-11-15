# -*- coding: utf-8 -*-
"""用 Edge headless 导出 web 版各页的真实计算样式，作为原生对齐的硬基准"""
import io
import json
import os
import re
import subprocess

EDGE = r"C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe"
WEB = r"C:\Users\一只屑\Desktop\HousewifeAppProto\web"
OUT = r"C:\Users\一只屑\Desktop\HousewifeAppProto\docs\web-computed.json"
BASE = "http://127.0.0.1:5000"

TARGETS = {
    "nav_title": ".nav-title",
    "nav_sub": ".nav-sub",
    "page": ".page.active",
    "group": ".page.active .group",
    "group_title": ".page.active .group-title",
    "cell": ".page.active .cell",
    "cell_icon": ".page.active .cell-icon",
    "cell_title": ".page.active .cell-title",
    "cell_sub": ".page.active .cell-sub",
    "cell_value": ".page.active .cell-value",
    "cell_edit": ".page.active .cell-edit",
    "cell_del": ".page.active .cell-del",
    "stat_grid": ".stat-grid",
    "stat": ".stat",
    "stat_v": ".stat .v",
    "stat_l": ".stat .l",
    "seg": ".seg",
    "seg_btn": ".seg button",
    "tabbar": ".tabbar",
    "tab_item": ".tab-item",
    "tab_label": ".tab-item span",
    "fab": ".fab",
    "hint": ".hint",
    "empty": ".empty",
    "ring": ".ring",
    "ring_pct": ".ring-center .pct",
    "ring_cap": ".ring-center .cap",
    "ring_big": ".ring-info .big",
    "ring_sub": ".ring-info .sub",
    "ring_wrap": ".ring-wrap",
    "bar_row": ".bar-row",
    "b_label": ".bar-row .b-label",
    "b_track": ".bar-row .b-track",
    "b_val": ".bar-row .b-val",
    "stepper_btn": ".stepper button",
    "st_val": ".stepper .st-val",
    "ai_card": ".ai-card",
    "ai_icon": ".ai-card .cell-icon",
    "toggle": ".toggle",
    "toggle_tg": ".toggle .tg",
    "cal": ".cal",
    "cal_title": ".cal-title",
    "cal_wd": ".cal-wd",
    "cal_day": ".cal-day",
    "model_item": ".model-item",
    "sheet_title": ".sheet-title",
}

PROPS = ["fontSize", "fontWeight", "letterSpacing", "lineHeight", "color",
         "backgroundColor", "borderRadius", "paddingTop", "paddingBottom",
         "paddingLeft", "paddingRight", "marginTop", "marginBottom",
         "height", "width", "minHeight", "gap", "boxShadow"]

JS_TMPL = """
<script>
(function(){
  var T = %s;
  var P = %s;
  function grab(sel){
    var el = document.querySelector(sel);
    if(!el) return null;
    var cs = getComputedStyle(el);
    var r = el.getBoundingClientRect();
    var o = {_w: Math.round(r.width*10)/10, _h: Math.round(r.height*10)/10};
    P.forEach(function(p){ o[p] = cs[p]; });
    return o;
  }
  window.__dump = function(){
    var out = {_innerW: window.innerWidth, _innerH: window.innerHeight, _dpr: window.devicePixelRatio};
    for (var k in T){ out[k] = grab(T[k]); }
    var pre = document.getElementById('__measure__');
    if(!pre){ pre = document.createElement('pre'); pre.id = '__measure__'; document.body.appendChild(pre); }
    pre.textContent = JSON.stringify(out);
  };
  setTimeout(function(){
    try{
      document.querySelectorAll('.guide-wrap,#guide-spotlight,#splash').forEach(function(e){e.remove()});
      %s
    }catch(e){
      var pre=document.getElementById('__measure__')||document.createElement('pre');
      pre.id='__measure__'; pre.textContent=JSON.stringify({_error:String(e)});
      document.body.appendChild(pre);
    }
  }, 2000);
})();
</script>
"""

# (页面, 切换脚本, 切换后等待毫秒)
PAGES = [
    ("ledger", "", 0),
    ("nutrition", "renderPage('nutrition');", 2500),
    ("ai", "renderPage('ai');", 300),
    ("settings", "renderPage('settings');", 300),
    ("diary", "renderPage('diary');", 500),
]


def dump(page_key, nav_js, wait_ms):
    src = io.open(os.path.join(WEB, "index.html"), encoding="utf-8").read()
    if wait_ms:
        tail = nav_js + "setTimeout(function(){ window.__dump(); }, %d);" % wait_ms
    else:
        tail = "window.__dump();"
    inject = JS_TMPL % (json.dumps(TARGETS, ensure_ascii=False), json.dumps(PROPS), tail)
    src = src.replace("</body>", inject + "\n</body>")
    tmp = os.path.join(WEB, "_dump_%s.html" % page_key)
    io.open(tmp, "w", encoding="utf-8", newline="\n").write(src)

    url = "%s/_dump_%s.html" % (BASE, page_key)
    cmd = [EDGE, "--headless=new", "--disable-gpu", "--dump-dom",
           "--virtual-time-budget=12000", url]
    try:
        p = subprocess.run(cmd, capture_output=True, timeout=120)
        html = p.stdout.decode("utf-8", errors="replace")
    except Exception as e:
        print("  %s: 失败 %s" % (page_key, e))
        return None
    m = re.search(r'<pre id="__measure__"[^>]*>(.*?)</pre>', html, re.S)
    if not m:
        print("  %s: 未找到测量节点" % page_key)
        return None
    raw = m.group(1)
    raw = (raw.replace("&quot;", '"').replace("&amp;", "&")
              .replace("&lt;", "<").replace("&gt;", ">"))
    try:
        return json.loads(raw)
    except Exception as e:
        print("  %s: JSON 解析失败 %s" % (page_key, e))
        return None


result = {}
for key, js, wait in PAGES:
    print("采集中: %s" % key)
    d = dump(key, js, wait)
    if d:
        result[key] = d
        filled = sum(1 for k, v in d.items() if isinstance(v, dict))
        print("  采集到 %d 个元素" % filled)

io.open(OUT, "w", encoding="utf-8", newline="\n").write(
    json.dumps(result, ensure_ascii=False, indent=1))
print("已写入 %s（%d 个页面）" % (OUT, len(result)))

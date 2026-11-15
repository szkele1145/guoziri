# -*- coding: utf-8 -*-
"""导出 web 版各弹窗的真实计算样式（记录弹窗 / AI 对话 / action-sheet / alert / toast）"""
import io
import json
import os
import re
import subprocess

EDGE = r"C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe"
WEB = r"C:\Users\一只屑\Desktop\HousewifeAppProto\web"
OUT = r"C:\Users\一只屑\Desktop\HousewifeAppProto\docs\web-computed-sheets.json"
BASE = "http://127.0.0.1:5000"

TARGETS = {
    # 记录弹窗
    "p_panel": "#sheet-purchase .sheet-panel",
    "p_grabber": "#sheet-purchase .sheet-grabber",
    "p_head": "#sheet-purchase .sheet-head",
    "p_title": "#sheet-purchase .sheet-title",
    "p_btn_text": "#sheet-purchase .sheet-btn.text",
    "p_btn_done": "#sheet-purchase .sheet-btn.done",
    "p_body": "#sheet-purchase .sheet-body",
    "p_group": "#sheet-purchase .ios-group",
    "p_cell": "#sheet-purchase .ios-cell",
    "p_label": "#sheet-purchase .ios-cell label",
    "p_input": "#sheet-purchase .ios-cell input",
    "p_sel": "#sheet-purchase .sel-display",
    "p_hint": "#sheet-purchase .hint",
    # AI 对话
    "ai_panel": "#sheet-ai .sheet-panel",
    "ai_head": "#sheet-ai .sheet-head",
    "ai_chat": "#sheet-ai .ai-chat",
    "ai_msg_bot": "#sheet-ai .msg.bot",
    "ai_input_wrap": "#sheet-ai .ai-input",
    "ai_input": "#sheet-ai .ai-input input",
    "ai_send": "#sheet-ai .send-btn",
    # action-sheet
    "as_panel": "#action-sheet .sheet-panel",
    "as_head": "#action-sheet .sheet-head",
    "as_title": "#action-sheet .sheet-title",
    "as_list": "#action-sheet .model-list",
    "as_item": "#action-sheet .model-item",
    # alert
    "al_box": "#alert .alert-box",
    "al_title": "#alert .alert-title",
    "al_msg": "#alert .alert-msg",
    "al_actions": "#alert .alert-actions",
    "al_btn": "#alert .alert-actions button",
    # toast
    "toast": "#toast",
}

PROPS = ["fontSize", "fontWeight", "letterSpacing", "lineHeight", "color",
         "backgroundColor", "borderRadius", "paddingTop", "paddingBottom",
         "paddingLeft", "paddingRight", "marginTop", "marginBottom",
         "height", "width", "minHeight", "maxHeight", "gap", "boxShadow"]

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
    var out = {_innerW: window.innerWidth, _innerH: window.innerHeight};
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
      pre.id='__measure__'; pre.textContent=JSON.stringify({_error:String(e), _stack:(e&&e.stack)||''});
      document.body.appendChild(pre);
    }
  }, 2000);
})();
</script>
"""

# (名称, 打开动作, 采集前等待 ms)
SCENES = [
    ("purchase", "openPurchaseSheet();", 900),
    ("ai", "openAI('chat');", 1400),
    ("action", "openActionSheet({title:'选择分类', items:[{value:'蔬菜',label:'蔬菜'},{value:'水果',label:'水果'}], onPick:function(){}});", 900),
    ("alert", "store.loadPurchases().then(function(){ confirmDelPurchase(state.purchases[0].id); });", 1500),
    ("toast", "toast('已保存');", 400),
]


def dump(name, action_js, wait_ms):
    src = io.open(os.path.join(WEB, "index.html"), encoding="utf-8").read()
    tail = action_js + "setTimeout(function(){ window.__dump(); }, %d);" % wait_ms
    inject = JS_TMPL % (json.dumps(TARGETS, ensure_ascii=False), json.dumps(PROPS), tail)
    src = src.replace("</body>", inject + "\n</body>")
    tmp = os.path.join(WEB, "_dump_%s.html" % name)
    io.open(tmp, "w", encoding="utf-8", newline="\n").write(src)

    url = "%s/_dump_%s.html" % (BASE, name)
    cmd = [EDGE, "--headless=new", "--disable-gpu", "--dump-dom",
           "--virtual-time-budget=14000", url]
    try:
        p = subprocess.run(cmd, capture_output=True, timeout=150)
        html = p.stdout.decode("utf-8", errors="replace")
    except Exception as e:
        print("  %s: 失败 %s" % (name, e))
        return None
    m = re.search(r'<pre id="__measure__"[^>]*>(.*?)</pre>', html, re.S)
    if not m:
        print("  %s: 未找到测量节点" % name)
        return None
    raw = m.group(1)
    raw = (raw.replace("&quot;", '"').replace("&amp;", "&")
              .replace("&lt;", "<").replace("&gt;", ">"))
    try:
        d = json.loads(raw)
    except Exception as e:
        print("  %s: JSON 失败 %s" % (name, e))
        return None
    if "_error" in d:
        print("  %s: 页面报错 %s" % (name, d["_error"][:120]))
    return d


result = {}
for name, js, wait in SCENES:
    print("采集中: %s" % name)
    d = dump(name, js, wait)
    if d:
        got = sum(1 for k, v in d.items() if isinstance(v, dict))
        print("  采集到 %d 个元素" % got)
        result[name] = d

io.open(OUT, "w", encoding="utf-8", newline="\n").write(
    json.dumps(result, ensure_ascii=False, indent=1))
print("已写入 %s（%d 个场景）" % (OUT, len(result)))

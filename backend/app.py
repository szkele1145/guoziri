# -*- coding: utf-8 -*-
"""
过日子 · 本地后端 (Flask)

最终形态：打包进 Android APK (Chaquopy)，由 WebView 加载 http://127.0.0.1:5000
- 数据存 app 私有目录 JSON 文件（Android 上为 getFilesDir()，通过环境变量 DATA_DIR 注入）
- API Key 只存在后端私有目录，永不返回给前端、永不写日志
- AI 请求由后端转发，前端只跟 127.0.0.1 通信
"""
import json
import os
import re
import threading
from datetime import datetime, date

import requests
from flask import Flask, jsonify, request, send_from_directory, Response

import nutrition

# ---------------------------------------------------------------- 路径
# Android 打包后由 Java 层注入 DATA_DIR=getFilesDir()；桌面开发默认放在 backend/data
BASE_DIR = os.path.dirname(os.path.abspath(__file__))
DATA_DIR = os.environ.get("DATA_DIR") or os.path.join(BASE_DIR, "data")
os.makedirs(DATA_DIR, exist_ok=True)

# 前端静态目录（打包时整个 web 目录进 assets）
WEB_DIR = os.path.join(os.path.dirname(BASE_DIR), "web")

_lock = threading.Lock()  # JSON 文件读写锁（Flask 默认多线程）

FILES = {
    "purchases": os.path.join(DATA_DIR, "purchases.json"),
    "diary": os.path.join(DATA_DIR, "diary.json"),
    "settings": os.path.join(DATA_DIR, "settings.json"),
}

DEFAULT_SETTINGS = {
    "sortOrder": "desc",
    "family": {"adults": 2, "children": 1},
    "hiddenEnabled": False,
    "theme": "auto",   # auto | light | dark
    # AI 配置：key 只存这里，永不下发
    "apiProvider": "deepseek",
    "apiBaseUrl": "https://api.deepseek.com/v1",
    "apiKey": "",
    "apiModel": "deepseek-chat",
}

# 预设服务商（OpenAI 兼容端点）。label/顺序给前端渲染用，base_url 是后端实际请求的地址
# keyPattern：该家 key 的形态校验（只验前缀和长度，不锁具体内容）
API_PRESETS = [
    {"id": "deepseek", "label": "DeepSeek", "baseUrl": "https://api.deepseek.com/v1",
     "model": "deepseek-chat", "keyHint": "sk-…", "keyPattern": r"^sk-[A-Za-z0-9_-]{8,}$"},
    {"id": "glm", "label": "智谱 GLM", "baseUrl": "https://open.bigmodel.cn/api/paas/v4",
     "model": "glm-4-flash", "keyHint": "……", "keyPattern": r"^[A-Za-z0-9.\-]{16,}$"},
    {"id": "nvidia", "label": "NVIDIA NIM", "baseUrl": "https://integrate.api.nvidia.com/v1",
     "model": "meta/llama-3.1-8b-instruct", "keyHint": "nvapi-…", "keyPattern": r"^nvapi-[A-Za-z0-9_\-]{16,}$"},
    {"id": "moonshot", "label": "Moonshot Kimi", "baseUrl": "https://api.moonshot.cn/v1",
     "model": "moonshot-v1-8k", "keyHint": "sk-…", "keyPattern": r"^sk-[A-Za-z0-9_-]{8,}$"},
    {"id": "qwen", "label": "阿里通义千问", "baseUrl": "https://dashscope.aliyuncs.com/compatible-mode/v1",
     "model": "qwen-turbo", "keyHint": "sk-…", "keyPattern": r"^sk-[A-Za-z0-9_-]{8,}$"},
    {"id": "siliconflow", "label": "硅基流动", "baseUrl": "https://api.siliconflow.cn/v1",
     "model": "Qwen/Qwen2.5-7B-Instruct", "keyHint": "sk-…", "keyPattern": r"^sk-[A-Za-z0-9_-]{8,}$"},
    {"id": "openrouter", "label": "OpenRouter", "baseUrl": "https://openrouter.ai/api/v1",
     "model": "openai/gpt-4o-mini", "keyHint": "sk-or-…", "keyPattern": r"^sk-or-[A-Za-z0-9_\-]{10,}$"},
    {"id": "custom", "label": "自定义 (OpenAI 兼容)", "baseUrl": "", "model": "", "keyHint": "",
     "keyPattern": r"^\S{8,}$"},
]

app = Flask(__name__, static_folder=None)

# 禁止 Flask 访问日志泄露任何请求内容（含 key 的请求体不会进日志）
import logging

logging.getLogger("werkzeug").setLevel(logging.ERROR)


# ---------------------------------------------------------------- 存储工具
def _read_json(key, default):
    path = FILES[key]
    if not os.path.exists(path):
        return default
    try:
        with open(path, "r", encoding="utf-8") as f:
            return json.load(f)
    except (ValueError, OSError):
        return default


def _write_json(key, data):
    path = FILES[key]
    tmp = path + ".tmp"
    with open(tmp, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=1)
    os.replace(tmp, path)  # 原子替换，写坏不丢数据


def _get_settings():
    s = dict(DEFAULT_SETTINGS)
    s.update(_read_json("settings", {}))
    return s


def _today():
    return date.today().isoformat()


def _now_iso():
    return datetime.now().isoformat(timespec="seconds")


def _new_id():
    return str(int(datetime.now().timestamp() * 1000))


# ---------------------------------------------------------------- 静态页面
@app.route("/")
def index():
    return send_from_directory(WEB_DIR, "index.html")


@app.route("/<path:filename>")
def static_files(filename):
    return send_from_directory(WEB_DIR, filename)


# ---------------------------------------------------------------- 记账 API
def _clean_purchase(body):
    """白名单字段清洗"""
    num = lambda v: (float(v) if v not in (None, "", 0) else None)
    return {
        "name": str(body.get("name", "")).strip()[:40],
        "category": str(body.get("category", "其他")).strip()[:10],
        "pricePerJin": num(body.get("pricePerJin")),
        "totalCost": num(body.get("totalCost")),
        "totalJin": num(body.get("totalJin")),
        "weightGrams": num(body.get("weightGrams")),
        "date": str(body.get("date") or _today())[:10],
        "note": str(body.get("note", "")).strip()[:200],
    }


@app.get("/api/purchases")
def list_purchases():
    return jsonify(_read_json("purchases", []))


@app.post("/api/purchases")
def add_purchase():
    body = request.get_json(force=True, silent=True) or {}
    item = _clean_purchase(body)
    if not item["name"]:
        return jsonify(error="菜名不能为空"), 400
    with _lock:
        lst = _read_json("purchases", [])
        rec = {"id": _new_id(), "createdAt": _now_iso(), **item}
        lst.insert(0, rec)
        _write_json("purchases", lst)
    return jsonify(rec), 201


@app.put("/api/purchases/<pid>")
def update_purchase(pid):
    body = request.get_json(force=True, silent=True) or {}
    item = _clean_purchase(body)
    with _lock:
        lst = _read_json("purchases", [])
        for i, p in enumerate(lst):
            if p["id"] == pid:
                # 编辑时：值为 None 的可选字段不覆盖旧值（避免误清空）
                merged = dict(p)
                for k, v in item.items():
                    if v is not None or k in ("name", "category", "date", "note"):
                        merged[k] = v
                lst[i] = {**merged, "id": pid}
                _write_json("purchases", lst)
                return jsonify(lst[i])
    return jsonify(error="记录不存在"), 404


@app.delete("/api/purchases/<pid>")
def delete_purchase(pid):
    with _lock:
        lst = _read_json("purchases", [])
        lst2 = [p for p in lst if p["id"] != pid]
        if len(lst2) == len(lst):
            return jsonify(error="记录不存在"), 404
        _write_json("purchases", lst2)
    return jsonify(ok=True)


# ---------------------------------------------------------------- 日记 API
def _clean_diary(body):
    """日记字段清洗：开始/结束时间段选填，都不填用保存时刻兜底"""
    hhmm = lambda v: bool(re.match(r"^\d{2}:\d{2}$", v)) if v else False
    start = str(body.get("timeStart") or "").strip()[:5]
    end = str(body.get("timeEnd") or "").strip()[:5]
    legacy = str(body.get("time") or "").strip()[:5]   # 兼容旧单时间点
    if not start and legacy:
        start = legacy
    if start and not hhmm(start):
        start = ""
    if end and not hhmm(end):
        end = ""
    return {
        "content": str(body.get("content", "")).strip()[:5000],
        "date": str(body.get("date") or _today())[:10],
        "timeStart": start,
        "timeEnd": end,
        # time 保留为开始时间（兼容 AI 上下文与旧数据）
        "time": start or datetime.now().strftime("%H:%M"),
    }


@app.get("/api/diary")
def list_diary():
    return jsonify(_read_json("diary", []))


@app.post("/api/diary")
def add_diary():
    body = request.get_json(force=True, silent=True) or {}
    item = _clean_diary(body)
    if not item["content"]:
        return jsonify(error="内容不能为空"), 400
    with _lock:
        lst = _read_json("diary", [])
        rec = {"id": _new_id(), "createdAt": _now_iso(), **item}
        lst.insert(0, rec)
        _write_json("diary", lst)
    return jsonify(rec), 201


@app.delete("/api/diary/<did>")
def delete_diary(did):
    with _lock:
        lst = _read_json("diary", [])
        lst2 = [d for d in lst if d["id"] != did]
        if len(lst2) == len(lst):
            return jsonify(error="记录不存在"), 404
        _write_json("diary", lst2)
    return jsonify(ok=True)


# ---------------------------------------------------------------- 设置 API
@app.get("/api/settings")
def get_settings():
    """返回设置但抹掉 apiKey —— 前端只看到 hasKey 布尔"""
    s = _get_settings()
    s["hasKey"] = bool(s.get("apiKey"))
    s.pop("apiKey", None)
    return jsonify(s)


@app.get("/api/ai/presets")
def ai_presets():
    """服务商预设列表（公开信息，不含任何密钥）"""
    return jsonify(presets=API_PRESETS)


@app.put("/api/settings")
def put_settings():
    body = request.get_json(force=True, silent=True) or {}
    with _lock:
        s = _get_settings()
        # 白名单更新
        if "sortOrder" in body:
            s["sortOrder"] = "asc" if body["sortOrder"] == "asc" else "desc"
        if isinstance(body.get("family"), dict):
            fam = s.get("family", {})
            try:
                fam["adults"] = max(0, min(20, int(body["family"].get("adults", fam.get("adults", 2)))))
                fam["children"] = max(0, min(10, int(body["family"].get("children", fam.get("children", 1)))))
            except (TypeError, ValueError):
                pass
            s["family"] = fam
        if "hiddenEnabled" in body:
            s["hiddenEnabled"] = bool(body["hiddenEnabled"])
        if "theme" in body:
            t = str(body["theme"]).strip()
            s["theme"] = t if t in ("auto", "light", "dark") else "auto"
        if "apiModel" in body:
            m = str(body["apiModel"]).strip()[:80]
            if m:
                s["apiModel"] = m
        if "apiBaseUrl" in body:
            url = str(body["apiBaseUrl"]).strip().rstrip("/")
            # 只允许 http(s)，防 file/ftp 之类
            if re.match(r"^https?://", url):
                s["apiBaseUrl"] = url
        if "apiProvider" in body:
            pid = str(body["apiProvider"]).strip()[:20]
            # 预设切换：自动带出对应的 baseUrl / 默认模型（key 不动）
            preset = next((p for p in API_PRESETS if p["id"] == pid), None)
            if preset:
                s["apiProvider"] = pid
                if preset["baseUrl"]:
                    s["apiBaseUrl"] = preset["baseUrl"]
                if preset["model"]:
                    s["apiModel"] = preset["model"]
        # key 传空字符串 = 清除；不传 = 保持不变
        if "apiKey" in body:
            s["apiKey"] = str(body["apiKey"]).strip()
        _write_json("settings", s)
    s["hasKey"] = bool(s.get("apiKey"))
    s.pop("apiKey", None)
    return jsonify(s)


# ---------------------------------------------------------------- 营养 API
@app.get("/api/nutrition")
def get_nutrition():
    d = request.args.get("date") or _today()
    purchases = [p for p in _read_json("purchases", []) if p.get("date") == d]
    settings = _get_settings()
    return jsonify(nutrition.assess(purchases, settings.get("family", {})))


@app.get("/api/food-names")
def food_names():
    return jsonify(list(nutrition.NUTRITION_DB.keys()))


# ---------------------------------------------------------------- AI API
def _key_ok(s):
    """按当前服务商的 keyPattern 校验 key 形态；未知服务商放行（交给上游判）"""
    key = s.get("apiKey") or ""
    preset = next((p for p in API_PRESETS if p["id"] == s.get("apiProvider")), None)
    if not preset or not preset.get("keyPattern"):
        return True
    return bool(re.match(preset["keyPattern"], key))


@app.get("/api/ai/models")
def ai_models():
    """拉取当前 API 服务可用的模型列表（OpenAI 兼容 /models 接口）"""
    s = _get_settings()
    if not s.get("apiKey"):
        return jsonify(error="未配置 API Key，请到设置页填写"), 428
    if not _key_ok(s):
        return jsonify(error="Key 形态与所选服务商不符，请检查是否填对"), 428

    try:
        resp = requests.get(
            s["apiBaseUrl"].rstrip("/") + "/models",
            headers={"Authorization": "Bearer " + s["apiKey"]},
            timeout=30,
        )
    except requests.RequestException as e:
        return jsonify(error="网络请求失败: %s" % e.__class__.__name__), 502

    if resp.status_code != 200:
        return jsonify(error="拉取模型列表失败（HTTP %d）" % resp.status_code), 502

    try:
        data = resp.json().get("data", [])
        # 每家字段名不一，统一取 id；排个序好找
        ids = sorted({str(m.get("id", "")).strip() for m in data if m.get("id")})
    except (ValueError, KeyError, AttributeError):
        return jsonify(error="模型列表格式异常"), 502
    return jsonify(models=ids)


@app.post("/api/ai/chat")
def ai_chat():
    """非流式：一次返回完整回复（兜底用）"""
    body = request.get_json(force=True, silent=True) or {}
    messages = body.get("messages")
    if not isinstance(messages, list) or not messages:
        return jsonify(error="messages 不能为空"), 400

    s = _get_settings()
    if not s.get("apiKey"):
        return jsonify(error="未配置 API Key，请到设置页填写"), 428
    if not _key_ok(s):
        return jsonify(error="Key 形态与所选服务商不符，请检查是否填对"), 428

    msgs = _prep_messages(messages)
    try:
        resp = requests.post(
            s["apiBaseUrl"].rstrip("/") + "/chat/completions",
            headers={"Authorization": "Bearer " + s["apiKey"]},
            json={"model": s.get("apiModel") or "deepseek-chat", "messages": msgs, "temperature": 0.7, "max_tokens": 2000},
            timeout=60,
        )
    except requests.RequestException as e:
        return jsonify(error="网络请求失败: %s" % e.__class__.__name__), 502

    if resp.status_code != 200:
        # 不透传上游 body，避免泄露任何敏感信息
        return jsonify(error="AI 服务返回 HTTP %d" % resp.status_code), 502

    try:
        reply = resp.json()["choices"][0]["message"]["content"]
    except (KeyError, IndexError, ValueError):
        return jsonify(error="AI 返回格式异常"), 502
    return jsonify(reply=reply)


def _prep_messages(messages):
    """注入 system 上下文 + 历史截断（流式/非流式共用）"""
    ctx = _build_context()
    msgs = [{"role": "system", "content": ctx}]
    for m in messages[-20:]:
        if isinstance(m, dict) and m.get("role") in ("user", "assistant") and isinstance(m.get("content"), str):
            msgs.append({"role": m["role"], "content": m["content"][:4000]})
    return msgs


@app.post("/api/ai/chat/stream")
def ai_chat_stream():
    """SSE 流式：逐段转发上游 delta，体感快数倍"""
    body = request.get_json(force=True, silent=True) or {}
    messages = body.get("messages")
    if not isinstance(messages, list) or not messages:
        return jsonify(error="messages 不能为空"), 400

    s = _get_settings()
    if not s.get("apiKey"):
        return jsonify(error="未配置 API Key，请到设置页填写"), 428
    if not _key_ok(s):
        return jsonify(error="Key 形态与所选服务商不符，请检查是否填对"), 428

    msgs = _prep_messages(messages)

    def generate():
        try:
            up = requests.post(
                s["apiBaseUrl"].rstrip("/") + "/chat/completions",
                headers={"Authorization": "Bearer " + s["apiKey"]},
                json={"model": s.get("apiModel") or "deepseek-chat", "messages": msgs,
                      "temperature": 0.7, "max_tokens": 2000, "stream": True},
                timeout=(10, 120), stream=True,
            )
        except requests.RequestException as e:
            yield "data: %s\n\n" % json.dumps({"error": "网络请求失败: %s" % e.__class__.__name__})
            return
        if up.status_code != 200:
            yield "data: %s\n\n" % json.dumps({"error": "AI 服务返回 HTTP %d" % up.status_code})
            return
        try:
            for line in up.iter_lines(decode_unicode=True):
                if not line or not line.startswith("data:"):
                    continue
                payload = line[5:].strip()
                if payload == "[DONE]":
                    break
                try:
                    delta = json.loads(payload)["choices"][0]["delta"].get("content")
                except (ValueError, KeyError, IndexError, AttributeError):
                    continue
                if delta:
                    yield "data: %s\n\n" % json.dumps({"delta": delta})
        except requests.RequestException:
            yield "data: %s\n\n" % json.dumps({"error": "连接中断"})
        yield "data: [DONE]\n\n"

    return Response(generate(), mimetype="text/event-stream",
                    headers={"Cache-Control": "no-cache", "X-Accel-Buffering": "no"})


def _build_context():
    """拼 AI 上下文：今天买菜 + 营养评估 + 今日日记"""
    d = _today()
    settings = _get_settings()
    fam = settings.get("family", {})
    lines = ["你是一位贴心的家庭营养师和理财顾问。今天是 %s。" % d,
             "家庭结构：%d 位成人，%d 位儿童。" % (fam.get("adults", 0), fam.get("children", 0))]

    purchases = [p for p in _read_json("purchases", []) if p.get("date") == d]
    if purchases:
        lines.append("今日买菜记录：")
        for p in purchases:
            cost = " 共¥%.2f" % p["totalCost"] if p.get("totalCost") else ""
            price = " @¥%.2f/斤" % p["pricePerJin"] if p.get("pricePerJin") else ""
            lines.append("- %s (%s)%s%s" % (p["name"], p["category"], cost, price))
        result = nutrition.assess(purchases, fam)
        t = result["total"]
        lines.append("营养摄入概况：热量%.0fkcal、蛋白质%.1fg、脂肪%.1fg、碳水%.1fg、纤维%.1fg、维C%.0fmg、钙%.0fmg、铁%.1fmg"
                     % (t["calories"], t["protein"], t["fat"], t["carbs"], t["fiber"], t["vitaminC"], t["calcium"], t["iron"]))
        low = [a for a in result["assessment"] if a["percent"] < 70]
        if low:
            lines.append("明显不足：" + "、".join("%s(%d%%)" % (a["label"], a["percent"]) for a in low))
    else:
        lines.append("今日暂无买菜记录。")

    diary = [x for x in _read_json("diary", []) if x.get("date") == d]
    if diary:
        lines.append("今日日记：")
        for x in diary:
            ts, te = x.get("timeStart", ""), x.get("timeEnd", "")
            when = "%s-%s" % (ts, te) if ts and te else (ts or x.get("time", ""))
            lines.append("- (%s) %s" % (when, x["content"][:500]))

    return "\n".join(lines)


# ---------------------------------------------------------------- 入口
if __name__ == "__main__":
    # Android 上由 Java 层以 127.0.0.1 启动；桌面调试同理，不对外网开放
    print("DATA_DIR =", DATA_DIR)
    app.run(host="127.0.0.1", port=5000, threaded=True, use_reloader=False)

/* ============================================================
   过日子 · 前端逻辑（Flask 后端版）
   所有数据走 /api/*，Key 只在后端，前端永不接触
   ============================================================ */
"use strict";

/* ---------------- 状态 ---------------- */
const state = {
  page: "ledger",
  purchases: [],
  diary: [],
  family: { adults: 2, children: 1 },
  settings: { sortOrder: "desc", hiddenEnabled: false, hasKey: false, apiBaseUrl: "https://api.deepseek.com/v1", apiModel: "deepseek-chat" },
  editingId: null,
  foodNames: [],
};

/* ---------------- API 层 ---------------- */
async function api(path, opts) {
  const res = await fetch("/api" + path, Object.assign({
    headers: { "Content-Type": "application/json" },
  }, opts || {}));
  if (!res.ok) {
    let msg = "HTTP " + res.status;
    try { const j = await res.json(); if (j.error) msg = j.error; } catch (e) {}
    throw new Error(msg);
  }
  return res.json();
}

const store = {
  async loadPurchases() { state.purchases = await api("/purchases"); },
  async loadDiary() { state.diary = await api("/diary"); },
  async loadSettings() { state.settings = await api("/settings"); state.family = state.settings.family || state.family; },
  async saveSettings(patch) { state.settings = await api("/settings", { method: "PUT", body: JSON.stringify(patch) }); state.family = state.settings.family || state.family; },
  async savePurchase(rec) {
    if (state.editingId) { await api("/purchases/" + state.editingId, { method: "PUT", body: JSON.stringify(rec) }); }
    else { await api("/purchases", { method: "POST", body: JSON.stringify(rec) }); }
  },
  async deletePurchase(id) { await api("/purchases/" + id, { method: "DELETE" }); },
  async saveDiary(rec) { await api("/diary", { method: "POST", body: JSON.stringify(rec) }); },
  async deleteDiary(id) { await api("/diary/" + id, { method: "DELETE" }); },
  async nutrition() { return api("/nutrition"); },
  async aiChat(messages) { return api("/ai/chat", { method: "POST", body: JSON.stringify({ messages }) }); },
};

/* ---------------- 工具 ---------------- */
const $ = (s, el) => (el || document).querySelector(s);
const $$ = (s, el) => Array.from((el || document).querySelectorAll(s));
const esc = (s) => String(s).replace(/[&<>"']/g, c => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));
const money = (n) => "¥" + Number(n || 0).toFixed(2);
const today = () => { const d = new Date(); return d.getFullYear() + "-" + String(d.getMonth() + 1).padStart(2, "0") + "-" + String(d.getDate()).padStart(2, "0"); };
const fmtDay = (ds) => { const d = new Date(ds + "T00:00:00"); return (d.getMonth() + 1) + "月" + d.getDate() + "日"; };
const weekDay = (ds) => "周" + "日一二三四五六"[new Date(ds + "T00:00:00").getDay()];

function toast(msg) {
  const t = $("#toast");
  t.textContent = msg;
  t.classList.add("show");
  clearTimeout(t._tm);
  t._tm = setTimeout(() => t.classList.remove("show"), 1800);
}

function parseWeight(p) {
  if (p.weightGrams) return +p.weightGrams;
  if (p.pricePerJin && p.totalCost) return (p.totalCost / p.pricePerJin) * 500;
  if (p.totalJin) return p.totalJin * 500;
  return 0;
}

/* ---------------- SVG 图标 ---------------- */
const ICONS = {
  ledger: '<svg viewBox="0 0 28 28" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"><rect x="5" y="3.5" width="18" height="21" rx="3.5"/><path d="M9.5 9.5h9M9.5 14h9M9.5 18.5h5.5"/></svg>',
  nutrition: '<svg viewBox="0 0 28 28" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"><path d="M14 24c-5-2.5-8-7-8-11.5C6 7 9.5 4 14 4s8 3 8 8.5C22 17 19 21.5 14 24z"/><path d="M14 24V10"/><path d="M14 14l4-3M14 17l-4-3"/></svg>',
  ai: '<svg viewBox="0 0 28 28" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M14 4l1.8 4.6a4 4 0 0 0 2.6 2.6L23 13l-4.6 1.8a4 4 0 0 0-2.6 2.6L14 22l-1.8-4.6a4 4 0 0 0-2.6-2.6L5 13l4.6-1.8a4 4 0 0 0 2.6-2.6L14 4z"/><circle cx="22.5" cy="21.5" r="2.2"/></svg>',
  settings: '<svg viewBox="0 0 28 28" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"><circle cx="14" cy="14" r="3.2"/><path d="M14 3.5v3M14 21.5v3M3.5 14h3M21.5 14h3M6.6 6.6l2.1 2.1M19.3 19.3l2.1 2.1M21.4 6.6l-2.1 2.1M8.7 19.3l-2.1 2.1"/></svg>',
  diary: '<svg viewBox="0 0 28 28" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"><rect x="5" y="4" width="18" height="20" rx="3"/><path d="M9 9h10M9 13.5h10M9 18h6"/><path d="M5 8.5h3M5 14h3M5 19.5h3"/></svg>',
};
const CAT_ICON = { 蔬菜: "🥬", 水果: "🍎", 肉类: "🥩", 海鲜: "🦐", 蛋奶: "🥛", 主食: "🍚", 调料: "🧂", 零食: "🍪", 其他: "🛒" };
const CAT_COLOR = { 蔬菜: "#e8f5e9", 水果: "#fff3e0", 肉类: "#ffebee", 海鲜: "#e3f2fd", 蛋奶: "#fff8e1", 主食: "#ede7f6", 调料: "#efebe9", 零食: "#fce4ec", 其他: "#f5f5f5" };
const CATEGORIES = Object.keys(CAT_ICON);

/* ---------------- Tab 栏 ---------------- */
function tabbarHTML() {
  const tabs = [
    { id: "ledger", label: "记账", icon: ICONS.ledger },
    { id: "nutrition", label: "营养", icon: ICONS.nutrition },
    { id: "ai", label: "AI", icon: ICONS.ai },
    { id: "settings", label: "设置", icon: ICONS.settings },
  ];
  if (state.settings.hiddenEnabled) {
    tabs.splice(3, 0, { id: "diary", label: "日记", icon: ICONS.diary });
  }
  return tabs.map(t =>
    `<button class="tab-item ${state.page === t.id ? "active" : ""}" data-tab="${t.id}">${t.icon}<span>${t.label}</span></button>`
  ).join("");
}

/* ---------------- 记账页 ---------------- */
function renderLedger(el) {
  const groups = {};
  const list = [...state.purchases].sort((a, b) =>
    state.settings.sortOrder === "desc"
      ? (b.date + (b.createdAt || "")).localeCompare(a.date + (a.createdAt || ""))
      : (a.date + (a.createdAt || "")).localeCompare(b.date + (b.createdAt || "")));
  list.forEach(p => { (groups[p.date] = groups[p.date] || []).push(p); });

  const t = today();
  const todayList = state.purchases.filter(p => p.date === t);
  const todaySum = todayList.reduce((s, p) => s + (+p.totalCost || 0), 0);
  const month = t.slice(0, 7);
  const monthSum = state.purchases.filter(p => (p.date || "").startsWith(month)).reduce((s, p) => s + (+p.totalCost || 0), 0);

  let html = `
    <div class="nav"><div class="nav-title">记账</div>
    <div class="nav-sub">${state.settings.sortOrder === "desc" ? "最新在前" : "最早在前"}</div></div>
    <div class="stat-grid">
      <div class="stat"><div class="v">${todayList.length}</div><div class="l">今日笔数</div></div>
      <div class="stat"><div class="v">${money(todaySum)}</div><div class="l">今日花销</div></div>
      <div class="stat"><div class="v">${money(monthSum)}</div><div class="l">本月累计</div></div>
      <div class="stat"><div class="v">${Object.keys(groups).length}</div><div class="l">记账天数</div></div>
    </div>`;

  if (!list.length) {
    html += `<div class="empty"><div class="e-icon">🛒</div>还没有记录<br>点右下角 ＋ 记第一笔</div>`;
    el.innerHTML = html;
    return;
  }

  for (const date of Object.keys(groups).sort((a, b) => state.settings.sortOrder === "desc" ? b.localeCompare(a) : a.localeCompare(b))) {
    const items = groups[date];
    const sum = items.reduce((s, p) => s + (+p.totalCost || 0), 0);
    const isToday = date === t;
    html += `<div class="group-title" style="margin-top:22px">${isToday ? "今天 · " : ""}${fmtDay(date)} ${weekDay(date)}</div>
      <div class="group">
        <div class="cell" style="min-height:40px"><div class="cell-main"><span class="cell-sub" style="margin:0">${items.length} 笔</span></div><div class="cell-value money">${money(sum)}</div></div>`;
    for (const p of items) {
      const w = parseWeight(p);
      const wTxt = w >= 1000 ? (w / 1000).toFixed(2) + "kg" : w > 0 ? Math.round(w) + "g" : "";
      html += `
        <div class="cell">
          <div class="cell-icon" style="background:${CAT_COLOR[p.category] || "#f5f5f5"};font-size:16px">${CAT_ICON[p.category] || "🛒"}</div>
          <div class="cell-main">
            <div class="cell-title">${esc(p.name)}${p.note ? `<span class="cell-sub" style="display:inline;margin-left:6px">${esc(p.note)}</span>` : ""}</div>
            <div class="cell-sub">${p.pricePerJin ? money(p.pricePerJin) + "/斤" : ""}${wTxt ? " · " + wTxt : ""}</div>
          </div>
          <div class="cell-value money">${p.totalCost ? money(p.totalCost) : "—"}</div>
          <button class="cell-edit" data-edit="${p.id}">编辑</button>
          <button class="cell-del" data-del="${p.id}">删除</button>
        </div>`;
    }
    html += `</div>`;
  }
  el.innerHTML = html;
}

/* ---------------- 营养页 ---------------- */
function renderNutrition(el, nutri) {
  const t = today();
  const a = (nutri && nutri.assessment) || [];
  const total = (nutri && nutri.total) || {};
  const counted = (nutri && nutri.details && nutri.details.length) || 0;
  const overall = a.length ? Math.round(a.reduce((s, x) => s + Math.min(x.percent, 100), 0) / a.length) : 0;
  const ringColor = overall >= 90 ? "var(--green)" : overall >= 60 ? "var(--orange)" : "var(--red)";
  const C = 2 * Math.PI * 42;
  const dash = C * Math.min(overall, 100) / 100;

  let html = `
    <div class="nav"><div class="nav-title">营养</div>
    <div class="nav-sub">今天 · ${fmtDay(t)} · 依据《中国居民膳食指南 2022》</div></div>

    <div class="group-title">今日总评</div>
    <div class="group"><div class="ring-wrap">
      <div class="ring">
        <svg viewBox="0 0 96 96">
          <circle class="track" cx="48" cy="48" r="42" fill="none" stroke-width="8"/>
          <circle class="val" cx="48" cy="48" r="42" fill="none" stroke-width="8"
            stroke-dasharray="${C}" stroke-dashoffset="${C - dash}" style="stroke:${ringColor}"/>
        </svg>
        <div class="ring-center"><div class="pct">${overall}%</div><div class="cap">综合达标</div></div>
      </div>
      <div class="ring-info">
        <div class="big">${counted ? "已按 " + counted + " 种食材计算" : "今天还没有可计算的食材"}</div>
        <div class="sub">热量 ${Math.round(total.calories || 0)} / ${Math.round(familyDRI().calories)} kcal<br>蛋白质 ${(total.protein || 0).toFixed(1)} / ${familyDRI().protein} g</div>
      </div>
    </div></div>`;

  if (counted && a.length) {
    html += `<div class="group-title">各项达标</div><div class="group">`;
    for (const x of a) {
      const cls = x.percent >= 100 ? "good" : x.percent >= 70 ? "warn" : x.percent > 130 ? "over" : "low";
      const w = Math.min(x.percent, 130) / 1.3;
      html += `<div class="bar-row">
        <div class="b-label">${x.label}</div>
        <div class="b-track"><div class="b-fill ${cls}" style="width:${w}%"></div></div>
        <div class="b-val">${(x.actual || 0).toFixed(x.unit === "kcal" ? 0 : 1)}/${x.target}${x.unit} · ${x.percent}%</div>
      </div>`;
    }
    html += `</div>
    <div class="hint">绿 ≥100% 达标 · 黄 70-99% 接近 · 红 &lt;70% 不足 · 橙 &gt;130% 偏多</div>`;
  } else {
    html += `<div class="empty"><div class="e-icon">🥗</div>去记账页添加食材<br>这里会自动算营养</div>`;
  }

  html += `
    <div class="group-title">家里几口人</div>
    <div class="group">
      <div class="cell"><div class="cell-main"><div class="cell-title">大人</div></div>
        <div class="stepper"><button data-fam="adults|-1">−</button><span class="st-val" id="fam-adults">${state.family.adults}</span><button data-fam="adults|1">＋</button></div></div>
      <div class="cell"><div class="cell-main"><div class="cell-title">小孩</div></div>
        <div class="stepper"><button data-fam="children|-1">−</button><span class="st-val" id="fam-children">${state.family.children}</span><button data-fam="children|1">＋</button></div></div>
    </div>`;

  el.innerHTML = html;
}

function familyDRI() {
  // 与后端一致的展示估算（仅用于页面文案）
  const AD = { calories: 1800, protein: 55 }, CH = { calories: 1350, protein: 30 };
  return {
    calories: AD.calories * state.family.adults + CH.calories * state.family.children,
    protein: AD.protein * state.family.adults + CH.protein * state.family.children,
  };
}

/* ---------------- AI 页 ---------------- */
function renderAI(el) {
  const t = today();
  const hasP = state.purchases.some(p => p.date === t);
  const hasD = state.diary.some(d => d.date === t);

  let html = `<div class="nav"><div class="nav-title">AI 助手</div>
    <div class="nav-sub">${state.settings.hasKey ? "已连接 · 数据仅在本机处理" : "先到设置页填 API Key"}</div></div>

    <div class="group-title">一键总结</div>
    <div class="group">
      <div class="cell ai-card" data-ai="cost" ${hasP ? "" : 'style="opacity:.45"'}>
        <div class="cell-icon" style="background:#e8f0fe">💰</div>
        <div class="cell-main"><div class="cell-title">总结今天花销</div><div class="cell-sub">值不值、哪儿能省</div></div>
        <div class="cell-arrow">›</div>
      </div>
      <div class="cell ai-card" data-ai="nutrition" ${hasP ? "" : 'style="opacity:.45"'}>
        <div class="cell-icon" style="background:#e8f5e9">🥗</div>
        <div class="cell-main"><div class="cell-title">总结今天营养</div><div class="cell-sub">补得怎么样、明天买什么</div></div>
        <div class="cell-arrow">›</div>
      </div>
      ${state.settings.hiddenEnabled && hasD ? `
      <div class="cell ai-card" data-ai="diary">
        <div class="cell-icon" style="background:#f3e8ff">📖</div>
        <div class="cell-main"><div class="cell-title">结合日记总结今天</div><div class="cell-sub">检测到今日日记</div></div>
        <div class="cell-arrow">›</div>
      </div>` : ""}
    </div>

    <div class="group-title">自由提问</div>
    <div class="group">
      <div class="cell ai-card" data-ai="chat">
        <div class="cell-icon" style="background:#e8f0fe">💬</div>
        <div class="cell-main"><div class="cell-title">随便聊聊</div><div class="cell-sub">食谱、省钱、囤货…什么都能问</div></div>
        <div class="cell-arrow">›</div>
      </div>
    </div>

    <div class="group-title">试试这些</div>
    <div class="group">
      ${["这周买什么菜性价比高？", "孩子不爱吃青菜怎么办？", "冰箱里该常备什么？", "怎么做一顿 20 元的营养晚餐？"].map(q => `
      <div class="cell" data-prompt="${esc(q)}" style="cursor:pointer">
        <div class="cell-main"><div class="cell-title" style="font-size:15px">${esc(q)}</div></div>
        <div class="cell-arrow">›</div>
      </div>`).join("")}
    </div>`;
  el.innerHTML = html;
}

/* ---------------- 设置页 ---------------- */
function renderSettings(el) {
  const s = state.settings;
  let html = `<div class="nav"><div class="nav-title">设置</div></div>

    <div class="group-title">AI 服务</div>
    <div class="group">
      <div class="cell"><div class="cell-main"><div class="cell-title">API 状态</div></div>
        <div class="cell-value">${s.hasKey ? '<span class="dot-ok"></span> 已配置' : '<span class="dot-no"></span> 未配置'}</div></div>
      <div class="cell"><div class="cell-main"><div class="cell-title">API 地址</div></div>
        <div class="cell-value" style="font-size:13px">${esc(s.apiBaseUrl || "https://api.deepseek.com/v1")}</div></div>
      <div class="cell"><label>API Key</label><input id="set-key" class="left type-key" type="password" placeholder="${s.hasKey ? "已保存（输入可覆盖）" : "sk-…"}" autocomplete="off"></div>
      <div class="cell"><label>模型</label><input id="set-model" class="left" type="text" value="${esc(s.apiModel || "deepseek-chat")}" placeholder="deepseek-chat"></div>
      <div class="cell" style="justify-content:flex-end">
        <button class="sheet-btn done" id="set-save" style="font-size:16px">保存</button>
      </div>
    </div>
    <div class="hint">Key 只存在本机数据目录，不上传、不备份、不写日志</div>

    <div class="group-title">通用</div>
    <div class="group">
      <div class="cell"><div class="cell-main"><div class="cell-title">记录排序</div><div class="cell-sub">最新在前 / 最早在前</div></div>
        <div class="toggle"><input type="checkbox" id="set-sort" ${s.sortOrder === "desc" ? "checked" : ""}><span class="tg"></span></div></div>
      <div class="cell"><div class="cell-main"><div class="cell-title">显示日记入口</div><div class="cell-sub">在底部导航栏加入「日记」</div></div>
        <div class="toggle"><input type="checkbox" id="set-hidden" ${s.hiddenEnabled ? "checked" : ""}><span class="tg"></span></div></div>
    </div>

    <div class="group-title">数据</div>
    <div class="group">
      <div class="cell" id="btn-export" style="cursor:pointer"><div class="cell-main"><div class="cell-title" style="color:var(--tint)">导出备份</div></div><div class="cell-arrow">›</div></div>
      <div class="cell" id="btn-clear" style="cursor:pointer"><div class="cell-main"><div class="cell-title" style="color:var(--red)">清空全部数据</div></div><div class="cell-arrow">›</div></div>
    </div>

    <div style="text-align:center;color:var(--tertiary);font-size:12px;padding:8px 0 24px">过日子 v0.1 · 数据保存在本机</div>`;
  el.innerHTML = html;

  $("#set-sort").addEventListener("change", async e => {
    try {
      await store.saveSettings({ sortOrder: e.target.checked ? "desc" : "asc" });
      toast("已保存");
    } catch (err) { toast(err.message); }
  });
  $("#set-hidden").addEventListener("change", async e => {
    try {
      await store.saveSettings({ hiddenEnabled: e.target.checked });
      renderTabbar();
      toast(e.target.checked ? "日记入口已开启" : "日记入口已隐藏");
    } catch (err) { toast(err.message); }
  });
  $("#set-save").addEventListener("click", async () => {
    const key = $("#set-key").value.trim();
    const model = $("#set-model").value.trim() || "deepseek-chat";
    try {
      const patch = { apiModel: model };
      if (key) patch.apiKey = key;      // 不填 = 不改动后端已存的
      await store.saveSettings(patch);
      $("#set-key").value = "";
      toast("已保存"); renderPage("settings");
    } catch (err) { toast(err.message); }
  });
  $("#btn-export").addEventListener("click", exportData);
  $("#btn-clear").addEventListener("click", confirmClear);
}

/* ---------------- 日记页 ---------------- */
function renderDiary(el) {
  const t = today();
  let html = `<div class="nav"><div class="nav-title">日常记录</div>
    <div class="nav-sub">随手记，不填时间就按保存时刻算</div></div>

    <div class="group-title">今天 · ${fmtDay(t)}</div>
    <div class="group">
      <div class="cell" style="flex-direction:column;align-items:stretch;gap:8px;padding:12px 16px">
        <div style="display:flex;gap:8px;align-items:center">
          <input id="d-time" type="time" style="text-align:left;background:var(--fill);border-radius:8px;padding:7px 10px;width:110px;font-size:15px">
          <input id="d-date" type="date" value="${t}" style="text-align:left;background:var(--fill);border-radius:8px;padding:7px 10px;flex:1;font-size:15px">
        </div>
        <textarea id="d-content" rows="3" placeholder="今天做了什么…" style="border:none;outline:none;background:var(--fill);border-radius:10px;padding:10px 12px;font-family:inherit;font-size:16px;resize:none"></textarea>
        <button id="d-save" class="send-btn" style="width:100%;border-radius:10px;height:40px;font-size:15px;font-family:inherit">保存</button>
      </div>
    </div>`;

  if (!state.diary.length) {
    html += `<div class="empty"><div class="e-icon">📖</div>还没有日常记录</div>`;
  } else {
    const groups = {};
    state.diary.forEach(d => { (groups[d.date] = groups[d.date] || []).push(d); });
    for (const date of Object.keys(groups).sort((a, b) => b.localeCompare(a))) {
      html += `<div class="group-title" style="margin-top:20px">${date === t ? "今天 · " : ""}${fmtDay(date)}</div><div class="group">`;
      for (const d of groups[date]) {
        html += `<div class="cell" style="align-items:flex-start">
          <div class="cell-icon" style="background:#f3e8ff;font-size:15px">🕒</div>
          <div class="cell-main">
            <div class="cell-sub" style="margin-bottom:2px">${esc(d.time || "")}</div>
            <div class="cell-title" style="font-size:15px;font-weight:400;white-space:pre-wrap;line-height:1.55">${esc(d.content)}</div>
          </div>
          <button class="cell-del" data-ddel="${d.id}">删除</button>
        </div>`;
      }
      html += `</div>`;
    }
  }
  el.innerHTML = html;

  $("#d-time").value = new Date().toTimeString().slice(0, 5);
  $("#d-save").addEventListener("click", async () => {
    const content = $("#d-content").value.trim();
    if (!content) { toast("写点什么再保存"); return; }
    try {
      await store.saveDiary({
        content,
        date: $("#d-date").value || today(),
        time: $("#d-time").value || new Date().toTimeString().slice(0, 5),
      });
      $("#d-content").value = "";
      toast("已保存");
      state.diary = await api("/diary");
      renderPage("diary");
    } catch (err) { toast(err.message); }
  });
}

/* ---------------- 页面调度 ---------------- */
const RENDERERS = {
  ledger: (el) => renderLedger(el),
  nutrition: async (el) => {
    el.innerHTML = '<div class="empty">计算中…</div>';
    try { renderNutrition(el, await store.nutrition()); }
    catch (err) { el.innerHTML = `<div class="empty">加载失败<br>${esc(err.message)}</div>`; }
  },
  ai: (el) => renderAI(el),
  settings: (el) => renderSettings(el),
  diary: (el) => renderDiary(el),
};

async function renderPage(name) {
  state.page = name;
  const pages = $("#pages");
  let el = $("#" + name + "-page");
  if (!el) {
    el = document.createElement("div");
    el.className = "page";
    el.id = name + "-page";
    pages.appendChild(el);
  }
  $$(".page", pages).forEach(p => p.classList.toggle("active", p === el));
  $("#fab").classList.toggle("show", name === "ledger");
  renderTabbar();
  await RENDERERS[name](el);
}

function renderTabbar() { $("#tabbar").innerHTML = tabbarHTML(); }

/* ---------------- Sheet ---------------- */
function openSheet(id) { $(id).classList.add("open"); }
function closeSheets() {
  $$(".sheet").forEach(s => s.classList.remove("open"));
  $("#alert").classList.remove("open");
}

function openPurchaseSheet(id) {
  state.editingId = id;
  const p = id ? state.purchases.find(x => x.id === id) : null;
  $("#purchase-title").textContent = p ? "编辑记录" : "新增记录";
  $("#p-name").value = p ? p.name : "";
  $("#p-category").value = p ? p.category : "蔬菜";
  $("#p-price").value = p && p.pricePerJin != null ? p.pricePerJin : "";
  $("#p-total").value = p && p.totalCost != null ? p.totalCost : "";
  $("#p-jin").value = p && p.totalJin != null ? p.totalJin : "";
  $("#p-grams").value = p && p.weightGrams != null ? p.weightGrams : "";
  $("#p-date").value = p ? p.date : today();
  $("#p-note").value = p ? p.note || "" : "";
  openSheet("#sheet-purchase");
  setTimeout(() => $("#p-name").focus(), 350);
}

async function savePurchase() {
  const name = $("#p-name").value.trim();
  if (!name) { toast("先填菜名"); return; }
  const num = (id) => { const v = $(id).value; return v === "" ? null : +v; };
  const rec = {
    name,
    category: $("#p-category").value,
    pricePerJin: num("#p-price"),
    totalCost: num("#p-total"),
    totalJin: num("#p-jin"),
    weightGrams: num("#p-grams"),
    date: $("#p-date").value || today(),
    note: $("#p-note").value.trim(),
  };
  try {
    await store.savePurchase(rec);
    state.editingId = null;
    await store.loadPurchases();
    closeSheets(); toast("已保存");
    renderPage(state.page);
  } catch (err) { toast(err.message); }
}

/* ---------------- AI 对话（真实后端） ---------------- */
const AI_PROMPTS = {
  cost: "请总结今天的买菜花销，值不值？哪里能省？",
  nutrition: "评估一下今天全家的营养，缺什么？明天建议买什么？",
  diary: "结合买菜记录和日记，总结一下今天过得怎么样？",
  chat: "",
};

let aiMessages = [];   // 当前对话历史（不含 system，system 由后端注入）

function openAI(mode) {
  openSheet("#sheet-ai");
  const chat = $("#ai-chat");
  if (!chat.children.length) {
    addMsg("bot", "你好，我是「过日子」的 AI 助手 🌿\n今天的记账和日记会自动带给我，直接问就行。");
  }
  const q = AI_PROMPTS[mode];
  if (q) setTimeout(() => { pushUser(q); }, 300);
  setTimeout(() => $("#ai-input").focus(), 400);
}

function addMsg(role, text) {
  const chat = $("#ai-chat");
  const div = document.createElement("div");
  div.className = "msg " + (role === "user" ? "user" : "bot");
  div.textContent = text;
  chat.appendChild(div);
  chat.scrollTop = chat.scrollHeight;
  return div;
}

async function pushUser(text) {
  addMsg("user", text);
  aiMessages.push({ role: "user", content: text });
  const loading = addMsg("bot", "正在思考…");
  loading.classList.add("loading");
  try {
    const r = await store.aiChat(aiMessages);
    loading.textContent = r.reply;
    loading.classList.remove("loading");
    aiMessages.push({ role: "assistant", content: r.reply });
  } catch (err) {
    loading.textContent = "出错了：" + err.message;
    loading.classList.remove("loading");
  }
}

function sendAI() {
  const input = $("#ai-input");
  const q = input.value.trim();
  if (!q) return;
  input.value = "";
  pushUser(q);
}

/* ---------------- 数据操作 ---------------- */
function exportData() {
  const blob = new Blob([JSON.stringify({ purchases: state.purchases, diary: state.diary, family: state.family }, null, 2)], { type: "application/json" });
  const a = document.createElement("a");
  a.href = URL.createObjectURL(blob);
  a.download = "guoziri-backup-" + today() + ".json";
  a.click();
  URL.revokeObjectURL(a.href);
  toast("已导出");
}

function confirmClear() {
  $("#alert-title").textContent = "清空全部数据？";
  $("#alert-msg").textContent = "买菜记录和日记都会删除，无法恢复。";
  openSheet("#alert");
  $("#alert-ok").onclick = async () => {
    closeSheets();
    for (const p of [...state.purchases]) await store.deletePurchase(p.id).catch(() => {});
    for (const d of [...state.diary]) await store.deleteDiary(d.id).catch(() => {});
    await store.loadPurchases(); await store.loadDiary();
    toast("已清空"); renderPage(state.page);
  };
  $("#alert-cancel").onclick = closeSheets;
}

function confirmDelPurchase(id) {
  const p = state.purchases.find(x => x.id === id);
  $("#alert-title").textContent = "删除这条记录？";
  $("#alert-msg").textContent = p ? p.name : "";
  openSheet("#alert");
  $("#alert-ok").onclick = async () => {
    closeSheets();
    try { await store.deletePurchase(id); await store.loadPurchases(); toast("已删除"); renderPage(state.page); }
    catch (err) { toast(err.message); }
  };
  $("#alert-cancel").onclick = closeSheets;
}

function confirmDelDiary(id) {
  $("#alert-title").textContent = "删除这条日记？";
  openSheet("#alert");
  $("#alert-ok").onclick = async () => {
    closeSheets();
    try { await store.deleteDiary(id); await store.loadDiary(); toast("已删除"); renderPage(state.page); }
    catch (err) { toast(err.message); }
  };
  $("#alert-cancel").onclick = closeSheets;
}

/* ---------------- 事件 ---------------- */
function bindEvents() {
  $("#tabbar").addEventListener("click", e => {
    const btn = e.target.closest("[data-tab]");
    if (btn) renderPage(btn.dataset.tab);
  });

  $("#fab").addEventListener("click", () => openPurchaseSheet(null));

  $("#pages").addEventListener("click", e => {
    const edit = e.target.closest("[data-edit]");
    if (edit) { openPurchaseSheet(edit.dataset.edit); return; }
    const del = e.target.closest("[data-del]");
    if (del) { confirmDelPurchase(del.dataset.del); return; }
    const ddel = e.target.closest("[data-ddel]");
    if (ddel) { confirmDelDiary(ddel.dataset.ddel); return; }
    const ai = e.target.closest("[data-ai]");
    if (ai) { openAI(ai.dataset.ai); return; }
    const prompt = e.target.closest("[data-prompt]");
    if (prompt) { openAI("chat"); setTimeout(() => pushUser(prompt.dataset.prompt), 350); return; }
    const fam = e.target.closest("[data-fam]");
    if (fam) {
      const [field, dirS] = fam.dataset.fam.split("|");
      const dir = +dirS;
      const v = Math.max(0, Math.min(field === "adults" ? 20 : 10, state.family[field] + dir));
      if (v !== state.family[field]) {
        state.family[field] = v;
        store.saveSettings({ family: state.family }).then(() => renderPage("nutrition")).catch(err => toast(err.message));
      }
    }
  });

  $$("[data-close]").forEach(el => el.addEventListener("click", closeSheets));
  $$("[data-alert-close]").forEach(el => el.addEventListener("click", closeSheets));
  $("#purchase-save").addEventListener("click", savePurchase);
  $("#ai-send").addEventListener("click", sendAI);
  $("#ai-input").addEventListener("keydown", e => { if (e.key === "Enter") sendAI(); });
  $("#ai-clear").addEventListener("click", () => { $("#ai-chat").innerHTML = ""; aiMessages = []; });

  document.addEventListener("keydown", e => { if (e.key === "Escape") closeSheets(); });
}

/* ---------------- 启动 ---------------- */
async function init() {
  $("#app").innerHTML = `
    <div id="pages" class="pages"></div>
    <button id="fab" class="fab">＋</button>
    <nav id="tabbar" class="tabbar"></nav>`;
  bindEvents();

  try {
    await Promise.all([store.loadPurchases(), store.loadDiary(), store.loadSettings()]);
  } catch (err) {
    // 后端未启动时的友好提示
    const isFile = location.protocol === "file:";
    $("#pages").innerHTML = `<div class="empty" style="padding-top:34vh">
      ${isFile
        ? `你是双击打开的文件，这样连不上后端<br><br>
           请在浏览器地址栏访问：<br>
           <b style="user-select:all">http://127.0.0.1:5000</b><br><br>
           <span style="font-size:12px;color:var(--tertiary)">（APK 里不存在这个问题，WebView 会自动指向本地服务）</span>`
        : `连不上本地服务<br><span style="font-size:12px;color:var(--tertiary)">${esc(err.message)}</span><br><br>
           确认后端已启动：<br><span style="font-size:12px;color:var(--tertiary)">backend 目录里双击「启动后端.bat」</span><br><br>
           <button class="send-btn" style="border-radius:10px;height:38px;padding:0 20px;font-family:inherit" onclick="location.reload()">重试</button>`}
    </div>`;
    return;
  }
  // 食材联想列表（后端提供）
  api("/food-names").then(names => { state.foodNames = names; }).catch(() => {});

  renderPage("ledger");
}

init();

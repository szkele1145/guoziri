/* ============================================================
   过日子 · 前端逻辑（Flask 后端版 · 完整功能）
   ============================================================ */
"use strict";

/* ---------------- 状态 ---------------- */
const state = {
  page: "ledger",
  ledgerTab: "today",          // today | calendar | all
  calYear: new Date().getFullYear(),
  calMonth: new Date().getMonth() + 1,   // 1-12
  calSelected: null,           // 'YYYY-MM-DD'
  nutriDate: null,             // 营养页查看的日期，null=今天
  purchases: [],
  diary: [],
  family: { adults: 2, children: 1 },
  settings: { sortOrder: "desc", hiddenEnabled: false, hasKey: false, apiBaseUrl: "https://api.deepseek.com/v1", apiModel: "deepseek-chat", theme: "auto" },
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
    if (sandboxMode) {
      if (state.editingId) {
        const i = state.purchases.findIndex(x => x.id === state.editingId);
        if (i >= 0) state.purchases[i] = { ...state.purchases[i], ...rec };
      } else {
        state.purchases.unshift({ id: "g" + Date.now(), createdAt: new Date().toISOString(), ...rec });
      }
      return;
    }
    if (state.editingId) { await api("/purchases/" + state.editingId, { method: "PUT", body: JSON.stringify(rec) }); }
    else { await api("/purchases", { method: "POST", body: JSON.stringify(rec) }); }
  },
  async deletePurchase(id) {
    if (sandboxMode) { state.purchases = state.purchases.filter(p => p.id !== id); return; }
    await api("/purchases/" + id, { method: "DELETE" });
  },
  async saveDiary(rec) {
    if (sandboxMode) {
      state.diary.unshift({ id: "gd" + Date.now(), createdAt: new Date().toISOString(), ...rec });
      return;
    }
    await api("/diary", { method: "POST", body: JSON.stringify(rec) });
  },
  async deleteDiary(id) {
    if (sandboxMode) { state.diary = state.diary.filter(d => d.id !== id); return; }
    await api("/diary/" + id, { method: "DELETE" });
  },
  async nutrition(date) { return api("/nutrition?date=" + date); },
  /* 近 7 天趋势：并发拉 7 天的综合达标率 */
  async nutritionTrend() {
    const days = [];
    for (let i = 6; i >= 0; i--) {
      const d = new Date();
      d.setDate(d.getDate() - i);
      days.push(d.getFullYear() + "-" + pad(d.getMonth() + 1) + "-" + pad(d.getDate()));
    }
    const results = await Promise.all(days.map(ds =>
      api("/nutrition?date=" + ds).then(n => ({
        date: ds,
        overall: n.assessment && n.assessment.length
          ? Math.round(n.assessment.reduce((s, x) => s + Math.min(x.percent, 100), 0) / n.assessment.length)
          : null,
      })).catch(() => ({ date: ds, overall: null }))
    ));
    return results;
  },
  async aiChat(messages) { return api("/ai/chat", { method: "POST", body: JSON.stringify({ messages }) }); },
  /* 流式对话：onDelta(增量文本) 回调，返回完整文本 */
  aiChatStream(messages, onDelta) {
    return new Promise((resolve, reject) => {
      fetch("/api/ai/chat/stream", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ messages }),
      }).then(res => {
        if (!res.ok || !res.body) {
          res.json().then(j => reject(new Error(j.error || "HTTP " + res.status))).catch(() => reject(new Error("HTTP " + res.status)));
          return;
        }
        const reader = res.body.getReader();
        const dec = new TextDecoder();
        let buf = "", full = "";
        function pump() {
          reader.read().then(({ done, value }) => {
            if (done) { resolve(full); return; }
            buf += dec.decode(value, { stream: true });
            let idx;
            while ((idx = buf.indexOf("\n\n")) >= 0) {
              const raw = buf.slice(0, idx).trim();
              buf = buf.slice(idx + 2);
              if (!raw.startsWith("data:")) continue;
              const payload = raw.slice(5).trim();
              if (payload === "[DONE]") { resolve(full); return; }
              try {
                const j = JSON.parse(payload);
                if (j.error) { reject(new Error(j.error)); return; }
                if (j.delta) { full += j.delta; onDelta(j.delta); }
              } catch (e) {}
            }
            pump();
          }).catch(reject);
        }
        pump();
      }).catch(reject);
    });
  },
};

/* ---------------- 工具 ---------------- */
const $ = (s, el) => (el || document).querySelector(s);
const $$ = (s, el) => Array.from((el || document).querySelectorAll(s));
const esc = (s) => String(s).replace(/[&<>"']/g, c => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));
const money = (n) => "¥" + Number(n || 0).toFixed(2);
const pad = (n) => String(n).padStart(2, "0");
const today = () => { const d = new Date(); return d.getFullYear() + "-" + pad(d.getMonth() + 1) + "-" + pad(d.getDate()); };
const fmtDay = (ds) => { const d = new Date(ds + "T00:00:00"); return (d.getMonth() + 1) + "月" + d.getDate() + "日"; };
const weekDay = (ds) => "周" + "日一二三四五六"[new Date(ds + "T00:00:00").getDay()];
const timeOf = (iso) => { try { return new Date(iso).toTimeString().slice(0, 5); } catch (e) { return ""; } };

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

function groupByDate(list) {
  const g = {};
  for (const p of list) (g[p.date] = g[p.date] || []).push(p);
  return g;
}

/* ---------------- 图标 ---------------- */
const ICONS = {
  ledger: '<svg viewBox="0 0 28 28" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"><rect x="5" y="3.5" width="18" height="21" rx="3.5"/><path d="M9.5 9.5h9M9.5 14h9M9.5 18.5h5.5"/></svg>',
  nutrition: '<svg viewBox="0 0 28 28" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M14 25c-1 0-1.9-.4-2.6-1.1C7.4 19.7 4.5 15.6 4.5 11.5 4.5 6.6 8.7 3 14 3s9.5 3.6 9.5 8.5c0 4.1-2.9 8.2-6.9 12.4-.7.7-1.6 1.1-2.6 1.1z" fill="currentColor" fill-opacity=".15"/><path d="M14 24V9"/><path d="M14 14l4.5-3.5"/><path d="M14 18.5L9.5 15"/></svg>',
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
function segHTML() {
  const tabs = [["today", "今天"], ["calendar", "日历"], ["all", "全部"]];
  return `<div class="seg">${tabs.map(([id, label]) =>
    `<button class="${state.ledgerTab === id ? "on" : ""}" data-ltab="${id}">${label}</button>`).join("")}</div>`;
}

function dayGroupHTML(date, items, opts) {
  const sum = items.reduce((s, p) => s + (+p.totalCost || 0), 0);
  const t = today();
  let html = `<div class="group-title" style="margin-top:22px">${date === t ? "今天 · " : ""}${fmtDay(date)} ${weekDay(date)}</div>
    <div class="group">
    <div class="cell" style="min-height:40px"><div class="cell-main"><span class="cell-sub" style="margin:0">${items.length} 笔</span></div><div class="cell-value money">${money(sum)}</div></div>`;
  for (const p of items) {
    const w = parseWeight(p);
    const wTxt = w >= 1000 ? (w / 1000).toFixed(2) + "kg" : w > 0 ? Math.round(w) + "g" : "";
    const tm = timeOf(p.createdAt);
    html += `
      <div class="swipe-wrap" data-swipe="${p.id}">
        <button class="swipe-del" data-swipe-del="${p.id}">删除</button>
        <div class="swipe-content">
          <div class="cell">
            <div class="cell-icon" style="background:${CAT_COLOR[p.category] || "#f5f5f5"};font-size:16px">${CAT_ICON[p.category] || "🛒"}</div>
            <div class="cell-main">
              <div class="cell-title">${esc(p.name)}${p.note ? `<span class="cell-sub" style="display:inline;margin-left:6px">${esc(p.note)}</span>` : ""}</div>
              <div class="cell-sub">${tm ? tm + " · " : ""}${p.pricePerJin ? money(p.pricePerJin) + "/斤" : ""}${wTxt ? " · " + wTxt : ""}</div>
            </div>
            <div class="cell-value money">${p.totalCost ? money(p.totalCost) : "—"}</div>
            <button class="cell-edit" data-edit="${p.id}">编辑</button>
          </div>
        </div>
      </div>`;
  }
  html += `</div>`;
  return html;
}

function calendarHTML(y, m, selected) {
  const t = today();
  const hasRecord = {};
  for (const p of state.purchases) hasRecord[p.date] = true;

  const first = new Date(y, m - 1, 1);
  const daysInMonth = new Date(y, m, 0).getDate();
  const offset = (first.getDay() + 6) % 7;   // 周一开头
  const prevDays = new Date(y, m - 1, 0).getDate();

  let cells = "";
  for (const wd of ["一", "二", "三", "四", "五", "六", "日"]) cells += `<div class="cal-wd">${wd}</div>`;
  for (let i = offset - 1; i >= 0; i--) cells += `<div class="cal-day" disabled>${prevDays - i}</div>`;
  for (let d = 1; d <= daysInMonth; d++) {
    const ds = `${y}-${pad(m)}-${pad(d)}`;
    const cls = [];
    if (ds === t) cls.push("tod");
    if (ds === selected) cls.push("sel");
    cells += `<button class="cal-day ${cls.join(" ")}" data-day="${ds}">${d}${hasRecord[ds] ? "<i></i>" : ""}</button>`;
  }
  const tail = (offset + daysInMonth) % 7;
  for (let d = 1; tail && d <= 7 - tail; d++) cells += `<div class="cal-day" disabled>${d}</div>`;

  return `<div class="group cal">
    <div class="cal-head">
      <button data-cal="-1">‹</button>
      <div class="cal-title">${y} 年 ${m} 月</div>
      <button data-cal="1">›</button>
    </div>
    <div class="cal-grid">${cells}</div>
    <div class="hint"><span class="cal-dot"></span> 有记录的日子 · 点日期看当天</div>
  </div>`;
}

function renderLedger(el) {
  const t = today();
  const groups = groupByDate(state.purchases);

  const todayList = state.purchases.filter(p => p.date === t);
  const todaySum = todayList.reduce((s, p) => s + (+p.totalCost || 0), 0);
  const month = t.slice(0, 7);
  const monthSum = state.purchases.filter(p => (p.date || "").startsWith(month)).reduce((s, p) => s + (+p.totalCost || 0), 0);

  let html = `
    <div class="nav"><div class="nav-title">记账</div></div>
    <div class="stat-grid">
      <div class="stat"><div class="v">${todayList.length}</div><div class="l">今日笔数</div></div>
      <div class="stat"><div class="v">${money(todaySum)}</div><div class="l">今日花销</div></div>
      <div class="stat"><div class="v">${money(monthSum)}</div><div class="l">本月累计</div></div>
      <div class="stat"><div class="v">${Object.keys(groups).length}</div><div class="l">记账天数</div></div>
    </div>
    ${segHTML()}`;

  if (state.ledgerTab === "today") {
    html += todayList.length
      ? dayGroupHTML(t, todayList)
      : `<div class="empty"><div class="e-icon">🛒</div>今天还没有记录<br>点右下角 ＋ 记一笔</div>`;
  } else if (state.ledgerTab === "calendar") {
    if (!state.calSelected) state.calSelected = t;
    html += calendarHTML(state.calYear, state.calMonth, state.calSelected);
    const selList = groups[state.calSelected] || [];
    html += selList.length
      ? dayGroupHTML(state.calSelected, selList)
      : `<div class="empty">${fmtDay(state.calSelected)}没有记录</div>`;
  } else {
    // 全部
    const desc = state.settings.sortOrder !== "asc";
    const dates = Object.keys(groups).sort((a, b) => desc ? b.localeCompare(a) : a.localeCompare(b));
    html += `<div class="group" style="margin-top:14px">
      <div class="cell" style="min-height:44px">
        <div class="cell-main"><div class="cell-title" style="font-size:15px">排序</div></div>
        <div class="seg mini" style="margin:0;width:150px">
          <button class="${desc ? "on" : ""}" data-sort="desc">最新在前</button>
          <button class="${!desc ? "on" : ""}" data-sort="asc">最早在前</button>
        </div>
      </div>
    </div>`;
    if (!dates.length) {
      html += `<div class="empty"><div class="e-icon">🛒</div>还没有记录<br>点右下角 ＋ 记第一笔</div>`;
    } else {
      for (const date of dates) html += dayGroupHTML(date, groups[date]);
    }
  }
  el.innerHTML = html;
}

/* ---------------- 营养页 ---------------- */
function familyDRI() {
  const AD = { calories: 1800, protein: 55 }, CH = { calories: 1350, protein: 30 };
  return {
    calories: AD.calories * state.family.adults + CH.calories * state.family.children,
    protein: AD.protein * state.family.adults + CH.protein * state.family.children,
  };
}

/* 近 7 天趋势曲线（纯 SVG，无依赖） */
function renderTrend(el, trend) {
  const old = $("#trend-card");
  if (old) old.remove();
  if (!trend || !trend.some(x => x.overall != null)) return;

  const W = 320, H = 120, P = 10;
  const pts = trend.map((x, i) => {
    const x0 = P + i * (W - 2 * P) / 6;
    const y0 = x.overall == null ? null : H - P - (x.overall / 100) * (H - 2 * P);
    return { ...x, x: x0, y: y0 };
  });
  const valid = pts.filter(p => p.y != null);
  if (!valid.length) return;

  // 折线 path（断日跳过）
  let path = "";
  valid.forEach((p, i) => { path += (i ? "L" : "M") + p.x.toFixed(1) + " " + p.y.toFixed(1); });

  // 面积
  const area = path + `L${valid[valid.length - 1].x.toFixed(1)} ${H - P} L${valid[0].x.toFixed(1)} ${H - P} Z`;

  const dots = valid.map(p =>
    `<circle cx="${p.x.toFixed(1)}" cy="${p.y.toFixed(1)}" r="3" fill="var(--tint)"/>` +
    `<text x="${p.x.toFixed(1)}" y="${(p.y - 8).toFixed(1)}" text-anchor="middle" font-size="9" fill="var(--secondary)">${p.overall}</text>`
  ).join("");

  const labels = pts.map((p, i) => {
    const d = new Date(p.date + "T00:00:00");
    return `<text x="${p.x.toFixed(1)}" y="${H - 1}" text-anchor="middle" font-size="9" fill="${p.date === today() ? "var(--tint)" : "var(--tertiary)"}">${p.date === today() ? "今天" : (d.getMonth() + 1) + "/" + d.getDate()}</text>`;
  }).join("");

  // 100% 参考线
  const y100 = H - P - (H - 2 * P);
  const grid = `<line x1="${P}" y1="${y100}" x2="${W - P}" y2="${y100}" stroke="var(--separator)" stroke-dasharray="3 3" stroke-width="1"/>`;

  const card = document.createElement("div");
  card.id = "trend-card";
  card.innerHTML = `
    <div class="group-title">近 7 天走势</div>
    <div class="group" style="padding:14px 8px 6px">
      <svg viewBox="0 0 ${W} ${H}" style="width:100%;display:block">
        <defs><linearGradient id="tg" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0" stop-color="var(--tint)" stop-opacity=".18"/>
          <stop offset="1" stop-color="var(--tint)" stop-opacity="0"/>
        </linearGradient></defs>
        ${grid}
        <path d="${area}" fill="url(#tg)"/>
        <path d="${path}" fill="none" stroke="var(--tint)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
        ${dots}${labels}
      </svg>
      <div class="hint" style="margin:2px 0 6px">综合达标率 % · 虚线 = 100% 达标线</div>
    </div>`;
  // 插到「各项达标」之前（若存在），否则插到总评卡之后
  const anchor = $("#trend-anchor") || el.querySelector(".ring-wrap").parentElement;
  anchor.insertAdjacentElement("afterend", card);
}

function renderNutrition(el, nutri, dateStr) {
  const t = today();
  const a = (nutri && nutri.assessment) || [];
  const total = (nutri && nutri.total) || {};
  const counted = (nutri && nutri.details && nutri.details.length) || 0;
  const overall = a.length ? Math.round(a.reduce((s, x) => s + Math.min(x.percent, 100), 0) / a.length) : 0;
  const ringColor = overall >= 90 ? "var(--green)" : overall >= 60 ? "var(--orange)" : "var(--red)";
  const C = 2 * Math.PI * 42;
  const dash = C * Math.min(overall, 100) / 100;
  const isToday = dateStr === t;

  let html = `
    <div class="nav"><div class="nav-title">营养</div>
    <div class="nav-sub">${isToday ? "今天" : fmtDay(dateStr)} · ${weekDay(dateStr)} · 《中国居民膳食指南 2022》</div></div>

    <div class="group">
      <div class="cell"><div class="cell-main"><div class="cell-title">查看日期</div></div>
        <input id="nutri-date" type="date" value="${dateStr}" max="${t}" style="text-align:right"></div>
    </div>

    <div class="group-title">总评</div>
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
        <div class="big">${counted ? "按 " + counted + " 种食材计算" : "当天没有可计算的食材"}</div>
        <div class="sub">热量 ${Math.round(total.calories || 0)} / ${Math.round(familyDRI().calories)} kcal<br>蛋白质 ${(total.protein || 0).toFixed(1)} / ${familyDRI().protein} g</div>
      </div>
    </div></div>
    <div id="trend-anchor"></div>`;

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

    // 未匹配食材：显示但不计入营养（不在食材库 / 没填重量）
    const unmatched = nutri.unmatched || [];
    if (unmatched.length) {
      html += `<div class="group-title">未计入营养</div><div class="group">`;
      for (const u of unmatched) {
        html += `<div class="cell" style="min-height:44px">
          <div class="cell-icon" style="background:var(--fill);font-size:14px">❔</div>
          <div class="cell-main"><div class="cell-title" style="font-size:15px">${esc(u.name)}</div>
          <div class="cell-sub">${esc(u.reason)}</div></div>
        </div>`;
      }
      html += `</div><div class="hint">这些记录不会影响营养计算</div>`;
    }
  } else {
    // 记账有数据但都未匹配/无重量：也要显示（不能卡成空）
    const purchases = state.purchases.filter(p => p.date === dateStr);
    if (purchases.length) {
      html += `<div class="group-title">今天记了这些（营养未计入）</div><div class="group">`;
      for (const p of purchases) {
        html += `<div class="cell" style="min-height:44px">
          <div class="cell-icon" style="background:var(--fill);font-size:14px">❔</div>
          <div class="cell-main"><div class="cell-title" style="font-size:15px">${esc(p.name)}</div>
          <div class="cell-sub">不在食材库或未填重量</div></div>
        </div>`;
      }
      html += `</div><div class="hint">填上单价+总价或重量，即可自动算营养</div>`;
    } else {
      html += `<div class="empty"><div class="e-icon">🥗</div>${isToday ? "去记账页添加食材" : "这一天没有记账"}<br>这里会自动算营养</div>`;
    }
  }

  html += `
    <div class="group-title">家里几口人</div>
    <div class="group">
      <div class="cell"><div class="cell-main"><div class="cell-title">大人</div></div>
        <div class="stepper"><button data-fam="adults|-1">−</button><span class="st-val">${state.family.adults}</span><button data-fam="adults|1">＋</button></div></div>
      <div class="cell"><div class="cell-main"><div class="cell-title">小孩</div></div>
        <div class="stepper"><button data-fam="children|-1">−</button><span class="st-val">${state.family.children}</span><button data-fam="children|1">＋</button></div></div>
    </div>`;

  el.innerHTML = html;

  $("#nutri-date").addEventListener("change", e => {
    state.nutriDate = e.target.value || today();
    renderPage("nutrition");
  });
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
  const presets = state.presets || [];
  const curPreset = presets.find(p => p.id === s.apiProvider) || presets[0];

  let html = `<div class="nav"><div class="nav-title">设置</div></div>

    <div class="group-title">AI 服务商</div>
    <div class="group" id="preset-group">
      ${presets.map(p => `
        <div class="cell preset-cell" data-preset="${esc(p.id)}" style="cursor:pointer">
          <div class="cell-main">
            <div class="cell-title" style="font-size:16px">${esc(p.label)}</div>
            ${p.baseUrl ? `<div class="cell-sub" style="font-size:11px">${esc(p.baseUrl.replace("https://", ""))}</div>` : "<div class=\"cell-sub\" style=\"font-size:11px\">手动填地址</div>"}
          </div>
          ${s.apiProvider === p.id ? '<span style="color:var(--tint);font-weight:700">✓</span>' : ""}
        </div>`).join("")}
    </div>
    <div class="hint">切换服务商自动带出地址和默认模型，Key 各自独立保存</div>

    <div class="group-title">当前服务配置${curPreset ? " · " + esc(curPreset.label) : ""}</div>
    <div class="group">
      <div class="cell"><div class="cell-main"><div class="cell-title">API 状态</div></div>
        <div class="cell-value">${s.hasKey ? '<span class="dot-ok"></span> 已配置' : '<span class="dot-no"></span> 未配置'}</div></div>
      <div class="cell"><div class="cell-main"><div class="cell-title">API 地址</div></div>
        <div class="cell-value" style="font-size:12px;max-width:170px;overflow:hidden;text-overflow:ellipsis">${esc(s.apiBaseUrl || "未设置")}</div></div>
      <div class="cell"><label>API Key</label><input id="set-key" class="left type-key" type="password" placeholder="${s.hasKey ? "已保存（输入可覆盖）" : (curPreset && curPreset.keyHint) || "sk-…"}" autocomplete="off"></div>
      <div class="cell"><label>模型</label><input id="set-model" class="left" type="text" value="${esc(s.apiModel || "deepseek-chat")}" placeholder="deepseek-chat" list="model-list"><datalist id="model-list"></datalist></div>
      <div class="cell" style="justify-content:flex-end">
        <button class="sheet-btn text" id="btn-models" style="font-size:15px">拉取模型列表</button>
      </div>
    </div>
    <div class="hint">修改 Key/模型后点别处即自动保存 · Key 只存本机，不上传不备份</div>

    <div class="group-title">通用</div>
    <div class="group">
      <div class="cell"><div class="cell-main"><div class="cell-title">记录排序</div><div class="cell-sub">「全部」列表的顺序</div></div>
        <label class="toggle"><input type="checkbox" id="set-sort" ${s.sortOrder === "desc" ? "checked" : ""}><span class="tg"></span></label></div>
      <div class="cell"><div class="cell-main"><div class="cell-title">显示日记入口</div><div class="cell-sub">在底部导航栏加入「日记」</div></div>
        <label class="toggle"><input type="checkbox" id="set-hidden" ${s.hiddenEnabled ? "checked" : ""}><span class="tg"></span></label></div>
      <div class="cell" id="set-theme-cell" style="cursor:pointer">
        <div class="cell-main"><div class="cell-title">外观</div><div class="cell-sub">深浅色</div></div>
        <span class="sel-display" id="theme-display">${{ auto: "自动", light: "浅色", dark: "深色" }[s.theme || "auto"]}</span>
        <span class="chevron"></span>
      </div>
    </div>

    <div class="group-title">数据</div>
    <div class="group">
      <div class="cell" id="btn-export" style="cursor:pointer"><div class="cell-main"><div class="cell-title" style="color:var(--tint)">导出备份</div></div><div class="cell-arrow">›</div></div>
      <div class="cell" id="btn-guide" style="cursor:pointer"><div class="cell-main"><div class="cell-title" style="color:var(--tint)">重播新手引导</div></div><div class="cell-arrow">›</div></div>
      <div class="cell" id="btn-clear" style="cursor:pointer"><div class="cell-main"><div class="cell-title" style="color:var(--red)">清空全部数据</div></div><div class="cell-arrow">›</div></div>
    </div>

    <div style="text-align:center;color:var(--tertiary);font-size:12px;padding:8px 0 24px">过日子 v0.4 · 数据保存在本机</div>`;
  el.innerHTML = html;

  $("#preset-group").addEventListener("click", async e => {
    const cell = e.target.closest("[data-preset]");
    if (!cell) return;
    try {
      await store.saveSettings({ apiProvider: cell.dataset.preset });
      toast("已切换");
      renderPage("settings");
    } catch (err) { toast(err.message); }
  });
  $("#set-sort").addEventListener("change", async e => {
    try { await store.saveSettings({ sortOrder: e.target.checked ? "desc" : "asc" }); toast("已保存"); }
    catch (err) { toast(err.message); }
  });
  $("#set-hidden").addEventListener("change", async e => {
    try {
      await store.saveSettings({ hiddenEnabled: e.target.checked });
      renderTabbar();
      toast(e.target.checked ? "日记入口已开启" : "日记入口已隐藏");
    } catch (err) { toast(err.message); }
  });
  // Key/模型自动保存：输入框失焦（blur）或回车时，有改动就存
  const autoSave = async (id, field, transform) => {
    const el = $(id);
    el.addEventListener("blur", async () => {
      const v = el.value.trim();
      const cur = state.settings[field] || "";
      if (!v || v === cur) return;               // 空 = 不动，未变 = 不存
      try {
        await store.saveSettings({ [field]: transform ? transform(v) : v });
        toast("已自动保存");
        if (id === "#set-key") el.value = "";    // Key 存完即清
        if (state.page === "settings") renderPage("settings");
      } catch (err) { toast(err.message); }
    });
    el.addEventListener("keydown", e => { if (e.key === "Enter") el.blur(); });
  };
  autoSave("#set-key", "apiKey");
  autoSave("#set-model", "apiModel");
  // 外观三档选择
  $("#set-theme-cell").addEventListener("click", () => {
    const cur = state.settings.theme || "auto";
    openPickerSheet({
      title: "外观",
      items: [
        { value: "auto", label: "🌤 自动（跟随系统）", current: cur === "auto" },
        { value: "light", label: "☀️ 浅色", current: cur === "light" },
        { value: "dark", label: "🌙 深色", current: cur === "dark" },
      ],
      onPick: async (theme) => {
        try {
          await store.saveSettings({ theme });
          applyTheme(theme);
          closeSheets(); restoreAlert();
          toast("外观已切换");
          renderPage("settings");
        } catch (err) { toast(err.message); }
      },
    });
  });
  $("#btn-models").addEventListener("click", fetchModels);
  $("#btn-export").addEventListener("click", exportData);
  $("#btn-guide").addEventListener("click", replayGuide);
  $("#btn-clear").addEventListener("click", confirmClear);
}

/* 拉取模型列表：填了 key 就直接拉；没填则提示先保存 */
async function fetchModels() {
  const btn = $("#btn-models");
  const keyInput = $("#set-key").value.trim();
  const hasKey = state.settings.hasKey || keyInput;

  if (!hasKey) { toast("先填 API Key 并保存，再拉取列表"); return; }

  // key 输入框里有新值：先静默保存，保证后端用的是刚填的
  if (keyInput) {
    try {
      await store.saveSettings({ apiKey: keyInput });
      $("#set-key").value = "";
    } catch (err) { toast(err.message); return; }
  }

  const old = btn.textContent;
  btn.textContent = "拉取中…";
  btn.disabled = true;
  try {
    const r = await api("/ai/models");
    const models = r.models || [];
    if (!models.length) { toast("服务端返回空列表"); return; }
    state.modelList = models;

    // 填 datalist 联想
    const dl = $("#model-list");
    dl.innerHTML = models.map(m => `<option value="${esc(m)}">`).join("");

    // iOS action sheet 选择器
    openModelSheet(models);
  } catch (err) {
    toast(err.message);
  } finally {
    btn.textContent = old;
    btn.disabled = false;
  }
}

function openModelSheet(models) {
  openPickerSheet({
    title: "选择模型",
    items: models.map(m => ({ value: m, label: m, current: m === state.settings.apiModel })),
    onPick: async (model) => {
      try {
        await store.saveSettings({ apiModel: model });
        closeSheets(); restoreAlert();
        toast("已选用 " + model);
        renderPage("settings");
      } catch (err) { toast(err.message); }
    },
  });
}

/* 通用 iOS 选择器：把 alert 弹窗变成 action sheet 列表 */
function openPickerSheet({ title, items, onPick }) {
  const box = $("#alert-box");
  $("#alert-title").textContent = title;
  const listHTML = `
    <div class="model-list">
      ${items.map(it => `
        <button class="model-item ${it.current ? "cur" : ""}" data-pick="${esc(it.value)}">
          <span>${esc(it.label)}</span>${it.current ? '<b>✓</b>' : ""}
        </button>`).join("")}
    </div>`;
  const msgEl = $("#alert-msg");
  if (msgEl) msgEl.outerHTML = listHTML;
  else $(".alert-box .alert-actions").insertAdjacentHTML("beforebegin", listHTML);

  const cancel = $("#alert-cancel");
  cancel.textContent = "关闭";
  $("#alert-ok").style.display = "none";
  openSheet("#alert");

  box.onclick = async e => {
    const item = e.target.closest("[data-pick]");
    if (!item) return;
    await onPick(item.dataset.pick);
  };
  cancel.onclick = () => { closeSheets(); restoreAlert(); };
}

/* 弹窗被模型列表借用后，还原标准确认框结构 */
function restoreAlert() {
  $("#alert-title").textContent = "确认";
  if (!$("#alert-msg")) {
    const actions = $(".alert-box .alert-actions") || $(".alert-actions");
    if (actions) actions.insertAdjacentHTML("beforebegin", '<div class="alert-msg" id="alert-msg"></div>');
  }
  $("#alert-cancel").textContent = "取消";
  $("#alert-ok").style.display = "";
  const box = $("#alert-box");
  if (box) box.onclick = null;   // 清掉模型选择的委托，防误触
}

/* ---------------- 日记页 ---------------- */
function renderDiary(el) {
  const t = today();
  let html = `<div class="nav"><div class="nav-title">日常记录</div>
    <div class="nav-sub">时间段选填，不填就按保存时刻算</div></div>

    <div class="group">
      <div class="cell" style="flex-direction:column;align-items:stretch;gap:8px;padding:12px 16px">
        <div style="display:flex;gap:8px;align-items:center">
          <input id="d-date" type="date" value="${t}" style="text-align:left;background:var(--fill);border-radius:8px;padding:7px 10px;flex:1;font-size:15px">
        </div>
        <div style="display:flex;gap:8px;align-items:center">
          <span class="cell-sub" style="white-space:nowrap">开始</span>
          <input id="d-time-start" type="time" style="text-align:center;background:var(--fill);border-radius:8px;padding:7px 10px;flex:1;font-size:15px">
          <span class="cell-sub" style="white-space:nowrap">结束</span>
          <input id="d-time-end" type="time" style="text-align:center;background:var(--fill);border-radius:8px;padding:7px 10px;flex:1;font-size:15px">
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
        const timeStr = d.timeStart && d.timeEnd ? `${d.timeStart} – ${d.timeEnd}`
                      : d.timeStart ? `${d.timeStart} 开始`
                      : d.time || "";
        html += `<div class="cell" style="align-items:flex-start">
          <div class="cell-icon" style="background:#f3e8ff;font-size:15px">🕒</div>
          <div class="cell-main">
            <div class="cell-sub" style="margin-bottom:2px">${esc(timeStr)}</div>
            <div class="cell-title" style="font-size:15px;font-weight:400;white-space:pre-wrap;line-height:1.55">${esc(d.content)}</div>
          </div>
          <button class="cell-del" data-ddel="${d.id}">删除</button>
        </div>`;
      }
      html += `</div>`;
    }
  }
  el.innerHTML = html;

  $("#d-save").addEventListener("click", () => {
    const content = $("#d-content").value.trim();
    if (!content) { toast("写点什么再保存"); return; }
    const start = $("#d-time-start").value;
    const end = $("#d-time-end").value;
    if (start && end && end < start) { toast("结束时间早于开始时间"); return; }
    withBusy($("#d-save"), "保存中…", async () => {
      try {
        await store.saveDiary({
          content,
          date: $("#d-date").value || today(),
          // 都不填 → 后端按保存时刻补；只填开始 → timeStart；都填 → 区间
          timeStart: start || null,
          timeEnd: end || null,
        });
        $("#d-content").value = "";
        $("#d-time-start").value = "";
        $("#d-time-end").value = "";
        toast("已保存");
        state.diary = await api("/diary");
        renderPage("diary");
      } catch (err) { toast(err.message); }
    });
  });
}

/* ---------------- 页面调度 ---------------- */
const RENDERERS = {
  ledger: (el) => renderLedger(el),
  nutrition: async (el) => {
    const d = state.nutriDate || today();
    el.innerHTML = '<div class="empty" style="padding-top:20vh">计算中…</div>';
    try {
      const nutri = await store.nutrition(d);
      // 记账为空：直接渲染空态，不再并发拉 7 天趋势（防卡）
      const isEmpty = !nutri.details || !nutri.details.length;
      renderNutrition(el, nutri, d);
      if (!isEmpty) {
        store.nutritionTrend().then(trend => {
          if (state.page === "nutrition") renderTrend(el, trend);
        }).catch(() => {});
      }
    }
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

/* 按钮防重：submit 期间禁用 + 文案反馈 */
async function withBusy(btn, label, fn) {
  if (btn.disabled) return;
  const old = btn.textContent;
  btn.disabled = true;
  btn.textContent = label;
  try { await fn(); }
  finally { btn.disabled = false; btn.textContent = old; }
}

function openPurchaseSheet(id) {
  state.editingId = id;
  const p = id ? state.purchases.find(x => x.id === id) : null;
  $("#purchase-title").textContent = p ? "编辑记录" : "新增记录";
  $("#p-name").value = p ? p.name : "";
  // 分类：记录当前值并同步显示
  state.purchaseCategory = p ? p.category : "蔬菜";
  $("#p-category-display").textContent = state.purchaseCategory;
  $("#p-price").value = p && p.pricePerJin != null ? p.pricePerJin : "";
  $("#p-total").value = p && p.totalCost != null ? p.totalCost : "";
  $("#p-jin").value = p && p.totalJin != null ? p.totalJin : "";
  $("#p-grams").value = p && p.weightGrams != null ? p.weightGrams : "";
  $("#p-date").value = p ? p.date : today();
  $("#p-note").value = p ? p.note || "" : "";
  openSheet("#sheet-purchase");
}

/* 分类选择：专用 iOS action sheet（不再借用 alert，杜绝与确认框互相干扰） */
function openCategoryPicker() {
  openActionSheet({
    title: "选择分类",
    items: CATEGORIES.map(c => ({ value: c, label: `${CAT_ICON[c] || "🛒"} ${c}`, current: c === state.purchaseCategory })),
    onPick: (cat) => {
      state.purchaseCategory = cat;
      $("#p-category-display").textContent = cat;
    },
  });
}

/* 专用 action sheet：底部升起、点选即收、蒙层可关 */
function openActionSheet({ title, items, onPick }) {
  // 复用已有 DOM（避免重复 id），每次重建内容
  const host = $("#action-sheet");
  $("#action-title").textContent = title;
  $("#action-list").innerHTML = items.map(it => `
    <button class="model-item ${it.current ? "cur" : ""}" data-pick="${esc(it.value)}">
      <span>${esc(it.label)}</span>${it.current ? '<b>✓</b>' : ""}
    </button>`).join("");
  openSheet("#action-sheet");

  const handler = async e => {
    const item = e.target.closest("[data-pick]");
    if (item) {
      $("#action-sheet").removeEventListener("click", handler);
      closeSheets();
      await onPick(item.dataset.pick);
      return;
    }
    if (e.target.closest("[data-action-close]") || e.target.classList.contains("sheet-mask")) {
      $("#action-sheet").removeEventListener("click", handler);
      closeSheets();
    }
  };
  $("#action-sheet").addEventListener("click", handler);
}

/* 数字联动：总价/单价 → 斤 → 克，顺着填自动算出其余（留过的空不覆盖）*/
function bindAutoCalc() {
  const g = (id) => $(id);
  const setIfEmpty = (id, v) => { if (v != null && !isNaN(v)) $(id).value = Math.round(v * 100) / 100; };
  function recalc(src) {
    const price = parseFloat(g("#p-price").value);
    const total = parseFloat(g("#p-total").value);
    const jin = parseFloat(g("#p-jin").value);
    const grams = parseFloat(g("#p-grams").value);
    if (src === "#p-jin" && jin > 0) {
      setIfEmpty("#p-grams", jin * 500);
      if (total > 0) setIfEmpty("#p-price", total / jin);
      else if (price > 0) setIfEmpty("#p-total", price * jin);
    } else if (src === "#p-grams" && grams > 0) {
      setIfEmpty("#p-jin", grams / 500);
      if (total > 0) setIfEmpty("#p-price", total / (grams / 500));
    } else if (src === "#p-total" && total > 0) {
      if (jin > 0) setIfEmpty("#p-price", total / jin);
      else if (grams > 0) setIfEmpty("#p-price", total / (grams / 500));
      else if (price > 0) setIfEmpty("#p-jin", total / price);
    } else if (src === "#p-price" && price > 0) {
      if (total > 0) setIfEmpty("#p-jin", total / price);
      else if (jin > 0) setIfEmpty("#p-total", price * jin);
    }
  }
  ["#p-price", "#p-total", "#p-jin", "#p-grams"].forEach(id => {
    g(id).addEventListener("input", () => recalc(id));
  });
}

async function savePurchase() {
  const name = $("#p-name").value.trim();
  if (!name) { toast("先填菜名"); return; }
  const num = (id) => { const v = $(id).value; return v === "" ? null : +v; };
  const rec = {
    name,
    category: state.purchaseCategory || "蔬菜",
    pricePerJin: num("#p-price"),
    totalCost: num("#p-total"),
    totalJin: num("#p-jin"),
    weightGrams: num("#p-grams"),
    date: $("#p-date").value || today(),
    note: $("#p-note").value.trim(),
  };
  await withBusy($("#purchase-save"), "保存中…", async () => {
    try {
      await store.savePurchase(rec);
      state.editingId = null;
      await store.loadPurchases();
      closeSheets(); toast("已保存");
      renderPage(state.page);
    } catch (err) { toast(err.message); }
  });
}

/* ---------------- AI 对话 ---------------- */
const AI_PROMPTS = {
  cost: "请总结今天的买菜花销，值不值？哪里能省？",
  nutrition: "评估一下今天全家的营养，缺什么？明天建议买什么？",
  diary: "结合买菜记录和日记，总结一下今天过得怎么样？",
  chat: "",
};

let aiMessages = [];

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
  const bubble = addMsg("bot", "");
  bubble.classList.add("loading");
  bubble.textContent = "▍";
  let acc = "";
  try {
    await store.aiChatStream(aiMessages, delta => {
      acc += delta;
      bubble.textContent = acc + "▍";   // 打字机光标
      const chat = $("#ai-chat");
      chat.scrollTop = chat.scrollHeight;
    });
    bubble.textContent = acc || "（空回复）";
    bubble.classList.remove("loading");
    aiMessages.push({ role: "assistant", content: acc });
  } catch (err) {
    bubble.textContent = acc ? acc + "\n\n[中断] " + err.message : "出错了：" + err.message;
    bubble.classList.remove("loading");
    // 已有部分内容也进历史，方便继续追问
    if (acc) aiMessages.push({ role: "assistant", content: acc });
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
  restoreAlert();   // 先还原结构（可能刚被模型列表借用过）
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
  restoreAlert();
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
  restoreAlert();
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
    const ltab = e.target.closest("[data-ltab]");
    if (ltab) { state.ledgerTab = ltab.dataset.ltab; renderPage("ledger"); return; }

    const sort = e.target.closest("[data-sort]");
    if (sort) {
      state.settings.sortOrder = sort.dataset.sort;
      store.saveSettings({ sortOrder: sort.dataset.sort }).catch(() => {});
      renderPage("ledger");
      return;
    }

    const cal = e.target.closest("[data-cal]");
    if (cal) {
      let m = state.calMonth + (+cal.dataset.cal);
      let y = state.calYear;
      if (m < 1) { m = 12; y--; }
      if (m > 12) { m = 1; y++; }
      state.calYear = y; state.calMonth = m;
      renderPage("ledger");
      return;
    }

    const day = e.target.closest("[data-day]");
    if (day) { state.calSelected = day.dataset.day; renderPage("ledger"); return; }

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
  bindAutoCalc();

  // 分类行：点击弹 iOS picker
  $("#p-category-cell").addEventListener("click", openCategoryPicker);

  $("#ai-send").addEventListener("click", sendAI);
  $("#ai-input").addEventListener("keydown", e => { if (e.key === "Enter") sendAI(); });
  $("#ai-clear").addEventListener("click", () => { $("#ai-chat").innerHTML = ""; aiMessages = []; });

  document.addEventListener("keydown", e => { if (e.key === "Escape") closeSheets(); });

  bindSwipe();
}

/* ---- 左滑删除手势（触屏 & 鼠标都支持）---- */
function bindSwipe() {
  let active = null;   // {wrap, content, startX, startY, dx, open}
  const THRESHOLD = 60;

  function closeAll(except) {
    $$(".swipe-wrap.open").forEach(w => { if (w !== except) w.classList.remove("open"); });
  }

  document.addEventListener("touchstart", e => {
    const content = e.target.closest(".swipe-content");
    if (!content) { closeAll(null); return; }
    const t = e.touches[0];
    active = { wrap: content.parentElement, content, startX: t.clientX, startY: t.clientY, dx: 0, locked: false };
    content.classList.add("dragging");
  }, { passive: true });

  document.addEventListener("touchmove", e => {
    if (!active) return;
    const t = e.touches[0];
    const dx = t.clientX - active.startX;
    const dy = t.clientY - active.startY;
    if (!active.locked) {
      if (Math.abs(dx) < 8 && Math.abs(dy) < 8) return;
      active.locked = true;
      if (Math.abs(dy) > Math.abs(dx)) { active = null; return; }  // 纵向滚动让路
    }
    const base = active.wrap.classList.contains("open") ? -72 : 0;
    const x = Math.max(-90, Math.min(12, base + dx));
    active.content.style.transform = `translateX(${x}px)`;
    active.dx = x;
  }, { passive: true });

  document.addEventListener("touchend", () => {
    if (!active) return;
    active.content.classList.remove("dragging");
    const shouldOpen = active.dx < -THRESHOLD;
    active.content.style.transform = "";
    active.wrap.classList.toggle("open", shouldOpen);
    if (shouldOpen) closeAll(active.wrap);
    active = null;
  });

  // 桌面鼠标拖拽同款体验
  document.addEventListener("mousedown", e => {
    const content = e.target.closest(".swipe-content");
    if (!content) return;
    active = { wrap: content.parentElement, content, startX: e.clientX, startY: e.clientY, dx: 0, locked: false, mouse: true };
    content.classList.add("dragging");
    const move = ev => {
      if (!active) return;
      const dx = ev.clientX - active.startX;
      const base = active.wrap.classList.contains("open") ? -72 : 0;
      const x = Math.max(-90, Math.min(12, base + dx));
      active.content.style.transform = `translateX(${x}px)`;
      active.dx = x;
    };
    const up = () => {
      document.removeEventListener("mousemove", move);
      document.removeEventListener("mouseup", up);
      if (!active) return;
      active.content.classList.remove("dragging");
      const shouldOpen = active.dx < -THRESHOLD;
      active.content.style.transform = "";
      active.wrap.classList.toggle("open", shouldOpen);
      if (shouldOpen) closeAll(active.wrap);
      active = null;
    };
    document.addEventListener("mousemove", move);
    document.addEventListener("mouseup", up);
  });

  // 露出的删除按钮
  $("#pages").addEventListener("click", e => {
    const del = e.target.closest("[data-swipe-del]");
    if (del) {
      const id = del.dataset.swipeDel;
      confirmDelPurchase(id);
      // 删除确认后收起
      const wrap = del.closest(".swipe-wrap");
      if (wrap) setTimeout(() => wrap.classList.remove("open"), 350);
    }
  });
}

/* 深浅色应用：auto=移除属性跟随系统；light/dark=强制 */
function applyTheme(theme) {
  if (theme === "dark") document.documentElement.dataset.theme = "dark";
  else if (theme === "light") document.documentElement.dataset.theme = "light";
  else delete document.documentElement.dataset.theme;
}

/* 启动屏进度：分步推进（解压→服务→数据→就绪） */
function splashStep(percent, tip) {
  const fill = $("#splash-fill");
  const tipEl = $("#splash-tip");
  if (fill) fill.style.width = percent + "%";
  if (tipEl && tip) tipEl.textContent = tip;
}

/* ---------------- 新手引导（沙箱模式）----------------
   引导期间真实数据被沙箱接管：演示数据走内存副本，结束恢复真实数据。
   「重播引导」也不会碰用户已有数据。 */
const GUIDE_KEY = "gz_guide_done";

let sandboxMode = false;          // true = 引导运行中
let savedRealData = null;         // 引导前的真实数据快照

/* 沙箱打开：截留所有写请求到内存，读请求返回内存副本 */
function openSandbox() {
  sandboxMode = true;
  savedRealData = {
    purchases: JSON.parse(JSON.stringify(state.purchases)),
    diary: JSON.parse(JSON.stringify(state.diary)),
  };
}

function closeSandbox() {
  sandboxMode = false;
  if (savedRealData) {
    state.purchases = savedRealData.purchases;
    state.diary = savedRealData.diary;
    savedRealData = null;
  }
}

/* 引导用演示数据（只在沙箱内存在，结束即消失） */
function guideDemoData() {
  const t = today();
  return {
    purchases: [
      { id: "g1", name: "西兰花", category: "蔬菜", pricePerJin: 6.5, totalCost: 9.8, totalJin: 1.5, date: t, note: "", createdAt: new Date().toISOString() },
      { id: "g2", name: "鸡蛋", category: "蛋奶", pricePerJin: 5.8, totalCost: 11.6, totalJin: 2, date: t, note: "", createdAt: new Date().toISOString() },
    ],
    diary: [
      { id: "gd1", content: "引导示例：买完菜、做完饭，随手把一天记下来。", date: t, timeStart: "18:00", timeEnd: "18:30", time: "18:00", createdAt: new Date().toISOString() },
    ],
  };
}

function guideDone() { return localStorage.getItem(GUIDE_KEY) === "1"; }

function startGuide() {
  openSandbox();

  const demo = guideDemoData();
  state.purchases = [];
  state.diary = [];

  let step = 0;
  const steps = [
    {
      title: "欢迎使用 过日子 🥬",
      body: "记账 · 营养 · AI 助手 · 日常记录，\n一个 App 管好一家人的餐桌。\n\n引导是沙箱模式，不会动真实数据。\n跟着高亮一步步走。",
      apply: async () => { renderPage("ledger"); },
      target: null,
    },
    {
      title: "底部导航 · 四个主页面",
      body: "这四个图标随时切换页面：\n记账 / 营养 / AI / 设置。\n开启日记入口后这里会出现第 5 个。",
      apply: async () => { renderPage("ledger"); },
      target: "#tabbar",
      pos: "top",
    },
    {
      title: "记一笔 · 点这里",
      body: "右下角蓝色 ＋ 就是添加按钮。\n沙箱里随便点，不会保存。",
      apply: async () => { state.purchases = [demo.purchases[0]]; renderPage("ledger"); },
      target: "#fab",
      pos: "left",
    },
    {
      title: "添加记录 · 这样填",
      body: "菜名可输 98 种食材联想；\n点「分类」行弹出统一菜单；\n单价+总价 或 斤/克 填一组自动算重量；\n填完点右上「存储」。",
      apply: async () => { openPurchaseSheet(null); },
      target: "#purchase-save",
      pos: "bottom",
    },
    {
      title: "营养页 · 自动评估",
      body: "圆环是全家总评，下面 8 项是各项达标。\n人数在页面底部可调；\n还能选日期回看、看 7 天趋势。",
      apply: async () => { state.purchases = demo.purchases; closeSheets(); renderPage("nutrition"); },
      target: "#nutrition-page .ring-wrap",
      pos: "bottom",
    },
    {
      title: "AI 助手 · 一键总结",
      body: "两个快捷按钮：总结今天花销 / 营养，\n点开即对话、可追问。\n使用前在设置页填 API Key。",
      apply: async () => { renderPage("ai"); },
      target: "#ai-page .group:nth-of-type(2) .ai-card",
      pos: "bottom",
    },
    {
      title: "设置 · API Key 在这填",
      body: "选服务商 → 填 Key → 失焦自动保存；\n可拉取模型列表、开日记入口、\n切深浅色、重播本引导。",
      apply: async () => { renderPage("settings"); },
      target: "#settings-page .group:nth-of-type(2)",
      pos: "bottom",
    },
    {
      title: "完成！",
      body: "引导结束，沙箱关闭，\n真实数据原样保留。\n\n去记第一笔真实的账吧！",
      apply: async () => { closeSheets(); renderPage("ledger"); },
      target: null,
    },
  ];

  let spotlightEl = null;

  function ensureSpotlight() {
    if (!spotlightEl || !document.body.contains(spotlightEl)) {
      spotlightEl = document.createElement("div");
      spotlightEl.id = "guide-spotlight";
      spotlightEl.innerHTML =
        '<div class="guide-mask"></div>' +
        '<div class="guide-hole"></div>' +
        '<div class="guide-tip-card">' +
        '  <div class="guide-step"></div>' +
        '  <div class="guide-title"></div>' +
        '  <div class="guide-body"></div>' +
        '  <div class="guide-dots"></div>' +
        '  <div class="guide-actions">' +
        '    <button class="guide-skip2">跳过</button>' +
        '    <button class="send-btn guide-btn" id="guide-next"></button>' +
        '  </div>' +
        '</div>';
      document.body.appendChild(spotlightEl);
      spotlightEl.addEventListener("click", e => {
        if (e.target.closest("#guide-next")) { e.stopPropagation(); next(); }
        else if (e.target.closest(".guide-skip2")) { e.stopPropagation(); finish(); }
      });
    }
    return spotlightEl;
  }

  function placeHole(targetSel, pos) {
    const hole = spotlightEl.querySelector(".guide-hole");
    const card = spotlightEl.querySelector(".guide-tip-card");
    const el = targetSel ? document.querySelector(targetSel) : null;

    if (!el) {
      hole.style.display = "none";
      card.style.position = "fixed";
      card.style.left = "50%"; card.style.top = "50%";
      card.style.transform = "translate(-50%, -50%)";
      card.style.width = "min(340px, 88vw)";
      return;
    }
    const r = el.getBoundingClientRect();
    const pad = 10;
    hole.style.display = "block";
    hole.style.left = (r.left - pad) + "px";
    hole.style.top = (r.top - pad) + "px";
    hole.style.width = (r.width + pad * 2) + "px";
    hole.style.height = (r.height + pad * 2) + "px";

    card.style.position = "fixed";
    card.style.transform = "none";
    card.style.width = "min(330px, calc(100vw - 32px))";
    const cardH = card.offsetHeight || 240;
    const margin = 16;
    let top = pos === "top" ? r.top - cardH - margin : r.bottom + margin;
    if (top < margin + 50) top = r.bottom + margin;
    if (top + cardH > innerHeight - margin - 70) {
      top = r.top - cardH - margin;
      if (top < margin) top = Math.max(margin, (innerHeight - cardH) / 2);
    }
    card.style.top = top + "px";
    // 水平：始终屏幕居中（下方留出 tabbar 空间），不贴边
    card.style.left = "50%";
    card.style.transform = "translateX(-50%)";
  }

  function renderStep() {
    const s = steps[step];
    const sp = ensureSpotlight();
    sp.style.display = "block";
    sp.querySelector(".guide-step").textContent = (step + 1) + " / " + steps.length + " · 沙箱模式";
    sp.querySelector(".guide-title").textContent = s.title;
    sp.querySelector(".guide-body").textContent = s.body;
    sp.querySelector(".guide-dots").innerHTML = steps.map((_, i) => '<i class="' + (i <= step ? "on" : "") + '"></i>').join("");
    sp.querySelector("#guide-next").textContent = step === steps.length - 1 ? "完成，退出引导" : "下一步";
    requestAnimationFrame(() => placeHole(s.target, s.pos));
  }

  async function next() {
    const s = steps[step];
    closeSheets();   // 任何步骤切换前都收起遗留 sheet
    await s.apply();
    step++;
    renderStep();
  }

  function finish() {
    closeSheets();
    closeSandbox();
    if (spotlightEl) { spotlightEl.remove(); spotlightEl = null; }
    localStorage.setItem(GUIDE_KEY, "1");
    renderPage("ledger");
    toast("引导完成，开始使用吧！");
  }

  renderStep();
}

/* 设置页的重播引导入口（沙箱隔离，重播多少次都不碰真实数据） */
function replayGuide() {
  localStorage.removeItem(GUIDE_KEY);
  startGuide();
}
async function init() {
  $("#app").innerHTML = `
    <div id="pages" class="pages"></div>
    <button id="fab" class="fab">＋</button>
    <nav id="tabbar" class="tabbar"></nav>`;
  bindEvents();

  splashStep(35, "连接本地服务…");
  try {
    await Promise.all([store.loadPurchases(), store.loadDiary(), store.loadSettings()]);
    splashStep(70, "加载记录…");
  } catch (err) {
    const isFile = location.protocol === "file:";
    $("#pages").innerHTML = `<div class="empty" style="padding-top:34vh">
      ${isFile
        ? `你是双击打开的文件，这样连不上后端<br><br>
           请在浏览器地址栏访问：<br>
           <b style="user-select:all">http://127.0.0.1:5000</b><br><br>
           <span style="font-size:12px;color:var(--tertiary)">（APK 里不存在这个问题）</span>`
        : `连不上本地服务<br><span style="font-size:12px;color:var(--tertiary)">${esc(err.message)}</span><br><br>
           确认后端已启动：<br><span style="font-size:12px;color:var(--tertiary)">backend 目录里双击「启动后端.bat」</span><br><br>
           <button class="send-btn" style="border-radius:10px;height:38px;padding:0 20px;font-family:inherit" onclick="location.reload()">重试</button>`}
    </div>`;
    return;
  }
  api("/food-names").then(names => {
    state.foodNames = names;
    const dl = $("#food-list");
    if (dl) dl.innerHTML = names.map(n => `<option value="${esc(n)}">`).join("");
  }).catch(() => {});
  // 分类下拉填充改为内存数组（picker 用），不再依赖原生 select
  if (!state.categories) state.categories = CATEGORIES;
  api("/ai/presets").then(r => {
    state.presets = r.presets || [];
    // 设置页可能已渲染（无预设空列表），补渲染一次
    if (state.page === "settings") renderPage("settings");
  }).catch(() => {});

  renderPage("ledger");

  // 应用保存的外观偏好
  applyTheme(state.settings.theme || "auto");

  // 数据就绪 → 进度推到 100% → 收起启动屏 → 未做过引导则进入引导
  splashStep(100, "就绪");
  const splash = document.getElementById("splash");
  if (splash) {
    setTimeout(() => {
      splash.classList.add("fade");
      setTimeout(() => {
        splash.remove();
        if (!guideDone()) startGuide();
      }, 350);
    }, 450);
  }
}

init();





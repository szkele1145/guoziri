/* ===== segHTML ===== */
function segHTML() {
  const tabs = [["today", "今天"], ["calendar", "日历"], ["all", "全部"]];
  return `<div class="seg">${tabs.map(([id, label]) =>
    `<button class="${state.ledgerTab === id ? "on" : ""}" data-ltab="${id}">${label}</button>`).join("")}</div>`;
}



/* ===== renderNutrition ===== */
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


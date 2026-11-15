# 过日子 · Web 版 UI 规范（自动从 web/style.css 提取）

> 本文件由脚本从 `web/style.css` 逐条解析生成，是原生 Android 版 UI 对齐的**唯一依据**。
> 换算约定：CSS `px` → Compose `dp/sp`（1:1），`rem`/`em` 按根字号 17px 折。

## 1. 设计令牌

### `:root`

| 变量 | 值 |
|---|---|
| `--bg` | `#f2f2f7` |
| `--bg-elevated` | `#ffffff` |
| `--bg-secondary` | `#f2f2f7` |
| `--label` | `#1c1c1e` |
| `--label-2` | `#3c3c43` |
| `--secondary` | `rgba(60, 60, 67, 0.6)` |
| `--tertiary` | `rgba(60, 60, 67, 0.3)` |
| `--separator` | `rgba(60, 60, 67, 0.29)` |
| `--fill` | `rgba(120, 120, 128, 0.12)` |
| `--tint` | `#007aff` |
| `--tint-soft` | `rgba(0, 122, 255, 0.12)` |
| `--green` | `#34c759` |
| `--orange` | `#ff9500` |
| `--red` | `#ff3b30` |
| `--yellow` | `#ffcc00` |
| `--nav-h` | `52px` |
| `--tab-h` | `58px` |
| `--safe-b` | `env(safe-area-inset-bottom, 0px)` |
| `--radius-card` | `12px` |
| `--radius-icon` | `8px` |
| `--font` | `"HarmonyOS Sans SC", -apple-system, BlinkMacSystemFont, "SF Pro Text",
          "PingFang SC", "HarmonyOS Sans SC", "MiSans", "Microsoft YaHei", system-ui, sans-serif` |
| `--mono` | `"SF Mono", ui-monospace, Menlo, Consolas, monospace` |

### `html[data-theme="dark"]`

| 变量 | 值 |
|---|---|
| `--bg` | `#000000` |
| `--bg-elevated` | `#1c1c1e` |
| `--bg-secondary` | `#1c1c1e` |
| `--label` | `#ffffff` |
| `--label-2` | `#ebebf5` |
| `--secondary` | `rgba(235, 235, 245, 0.6)` |
| `--tertiary` | `rgba(235, 235, 245, 0.3)` |
| `--separator` | `rgba(84, 84, 88, 0.6)` |
| `--fill` | `rgba(120, 120, 128, 0.24)` |
| `--tint` | `#0a84ff` |
| `--tint-soft` | `rgba(10, 132, 255, 0.2)` |
| `--green` | `#30d158` |
| `--orange` | `#ff9f0a` |
| `--red` | `#ff453a` |
| `--yellow` | `#ffd60a` |

## 2. 动效关键帧

- **`@keyframes splashPulse`**：`0%, 100% { transform: scale(1); opacity: 1; } 50% { transform: scale(1.12); opacity: .7; }`
- **`@keyframes alertIn`**：`from { opacity: 0; transform: translate(-50%, -46%) scale(1.06); } to { opacity: 1; transform: translate(-50%, -50%) scale(1); }`
- **`@keyframes pageIn`**：`from { opacity: 0; transform: translateY(6px); } to { opacity: 1; transform: none; }`
- **`@keyframes ringPulse`**：`0%,100% { box-shadow: 0 0 0 4px rgba(0,122,255,0.15); } 50% { box-shadow: 0 0 0 8px rgba(0,122,255,0.28); }`
- **`@keyframes guideIn`**：`from { opacity: 0; transform: translateX(-50%) translateY(8px); } to { opacity: 1; transform: translateX(-50%); }`
- **`@keyframes listIn`**：`from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: none; }`
- **`@keyframes fabIn`**：`from { opacity: 0; transform: scale(.6); } to { opacity: 1; transform: scale(1); }`

## 3. 组件规格

### `@font-face`

```css
font-family: "HarmonyOS Sans SC";
src: url("fonts/HarmonyOS_Sans_SC.ttf") format("truetype");
font-weight: 100 900;
font-display: swap;
```

### `@media (prefers-color-scheme: dark) >> html[data-theme="auto"], html:not([data-theme])`

```css
--bg: #000000;
--bg-elevated: #1c1c1e;
--bg-secondary: #1c1c1e;
--label: #ffffff;
--label-2: #ebebf5;
--secondary: rgba(235, 235, 245, 0.6);
--tertiary: rgba(235, 235, 245, 0.3);
--separator: rgba(84, 84, 88, 0.6);
--fill: rgba(120, 120, 128, 0.24);
--tint: #0a84ff;
--tint-soft: rgba(10, 132, 255, 0.2);
--green: #30d158;
--orange: #ff9f0a;
--red: #ff453a;
--yellow: #ffd60a;
```

### `@media (prefers-color-scheme: dark) >> html[data-theme="auto"] .cell-icon, html:not([data-theme]) .cell-icon`

```css
filter: brightness(0.88);
```

### `@media (prefers-color-scheme: dark) >> html[data-theme="auto"] .stat, html:not([data-theme]) .stat`

```css
background: var(--bg-elevated);
```

### `@media (prefers-color-scheme: dark) >> html[data-theme="auto"] .tabbar, html:not([data-theme]) .tabbar`

```css
background: rgba(22, 22, 24, 0.94);
```

### `@media (prefers-color-scheme: dark) >> html[data-theme="auto"] .alert-box, html[data-theme="auto"] #toast,   html:not([data-theme]) .alert-box, html:not([data-theme]) #toast`

```css
background: rgba(40, 40, 42, 0.97);
```

### `html[data-theme="dark"] .cell-icon`

```css
filter: brightness(0.88);
```

### `html[data-theme="dark"] .tabbar`

```css
background: rgba(22, 22, 24, 0.94);
```

### `html[data-theme="dark"] .alert-box, html[data-theme="dark"] #toast`

```css
background: rgba(40, 40, 42, 0.97);
```

### `html, body`

```css
margin: 0;
padding: 0;
background: var(--bg);
```

### `#splash`

```css
position: fixed;
inset: 0;
z-index: 999;
background: var(--bg);
display: flex;
flex-direction: column;
align-items: center;
justify-content: center;
gap: 14px;
transition: opacity .35s;
```

### `#splash.fade`

```css
opacity: 0;
pointer-events: none;
```

### `#splash .splash-icon`

```css
font-size: 56px;
animation: splashPulse 1.4s ease-in-out infinite;
```

### `#splash .splash-name`

```css
font-size: 26px;
font-weight: 700;
letter-spacing: 0.02em;
```

### `#splash .splash-bar`

```css
width: 200px;
height: 4px;
background: var(--fill);
border-radius: 2px;
overflow: hidden;
margin-top: 6px;
```

### `#splash .splash-fill`

```css
height: 100%;
width: 0%;
background: linear-gradient(90deg, var(--tint), #5ac8fa);
border-radius: 2px;
transition: width .4s ease;
```

### `#splash .splash-tip`

```css
font-size: 13px;
color: var(--secondary);
```

### `body`

```css
font-family: var(--font);
color: var(--label);
font-size: 17px;
line-height: 1.45;
-webkit-font-smoothing: antialiased;
-webkit-tap-highlight-color: transparent;
overflow: hidden;
height: 100dvh;
```

### `#app`

```css
height: 100dvh;
display: flex;
flex-direction: column;
```

### `.nav`

```css
flex-shrink: 0;
padding: 14px 20px 6px;
background: transparent;
```

### `.nav-title`

```css
font-size: 34px;
font-weight: 700;
letter-spacing: 0.37px;
line-height: 1.2;
```

### `.nav-sub`

```css
font-size: 13px;
color: var(--secondary);
margin-top: 2px;
```

### `.nav-right`

```css
position: absolute;
right: 20px;
top: 22px;
display: flex;
gap: 14px;
```

### `.pages`

```css
flex: 1;
position: relative;
overflow: hidden;
```

### `.page`

```css
position: absolute;
inset: 0;
overflow-y: auto;
-webkit-overflow-scrolling: touch;
padding: 4px 16px calc(var(--tab-h) + var(--safe-b) + 24px);
display: none;
```

### `.page.active`

```css
display: block;
```

### `.group`

```css
background: var(--bg-elevated);
border-radius: var(--radius-card);
margin: 8px 0 20px;
overflow: hidden;
```

### `.group-title`

```css
font-size: 13px;
color: var(--secondary);
text-transform: uppercase;
letter-spacing: 0.06em;
padding: 0 16px 6px;
margin-top: 18px;
```

### `.group-title:first-child`

```css
margin-top: 8px;
```

### `.cell, .ios-cell`

```css
display: flex;
align-items: center;
gap: 12px;
min-height: 52px;
padding: 8px 16px;
position: relative;
```

### `.cell + .cell::before, .ios-cell + .ios-cell::before`

```css
content: "";
position: absolute;
top: 0;
left: 16px;
right: 0;
height: 0.5px;
background: var(--separator);
transform: scaleY(0.5);
```

### `.cell:active, .ios-cell:active`

```css
background: var(--fill);
```

### `.swipe-wrap`

```css
position: relative;
overflow: hidden;
```

### `.swipe-wrap .swipe-del`

```css
position: absolute;
top: 0;
right: 0;
bottom: 0;
width: 72px;
border: none;
background: var(--red);
color: #fff;
font-size: 14px;
font-weight: 600;
font-family: inherit;
display: flex;
align-items: center;
justify-content: center;
cursor: pointer;
```

### `.swipe-wrap .swipe-content`

```css
position: relative;
background: var(--bg-elevated);
transition: transform .18s ease-out;
touch-action: pan-y;
```

### `.swipe-wrap .swipe-content.dragging`

```css
transition: none;
```

### `.swipe-wrap.open .swipe-content`

```css
transform: translateX(-72px);
```

### `.cell-icon`

```css
width: 30px;
height: 30px;
flex-shrink: 0;
border-radius: var(--radius-icon);
display: flex;
align-items: center;
justify-content: center;
font-size: 15px;
```

### `.cell-main`

```css
flex: 1;
min-width: 0;
```

### `.cell-title`

```css
font-size: 17px;
```

### `.cell-sub`

```css
font-size: 13px;
color: var(--secondary);
margin-top: 1px;
display: flex;
gap: 8px;
flex-wrap: wrap;
```

### `.cell-value`

```css
font-size: 17px;
color: var(--secondary);
white-space: nowrap;
```

### `.cell-value.money`

```css
font-variant-numeric: tabular-nums;
color: var(--label);
font-weight: 600;
```

### `.cell-arrow`

```css
color: var(--tertiary);
font-size: 17px;
```

### `.cell-del`

```css
border: none;
background: none;
color: var(--red);
font-size: 15px;
padding: 6px 2px 6px 10px;
flex-shrink: 0;
font-family: inherit;
```

### `.cell-edit`

```css
border: none;
background: none;
color: var(--tint);
font-size: 15px;
padding: 6px 10px 6px 2px;
flex-shrink: 0;
font-family: inherit;
```

### `.tabbar`

```css
position: fixed;
left: 0;
right: 0;
bottom: 0;
height: calc(58px + var(--safe-b));
padding-bottom: var(--safe-b);
background: rgba(242, 242, 247, 0.94);
backdrop-filter: saturate(180%) blur(20px);
-webkit-backdrop-filter: saturate(180%) blur(20px);
border-top: 0.5px solid var(--separator);
display: flex;
z-index: 100;
```

### `@media (prefers-color-scheme: dark) >> .tabbar`

```css
background: rgba(22, 22, 24, 0.94);
```

### `.tab-item`

```css
flex: 1;
border: none;
background: none;
font-family: inherit;
display: flex;
flex-direction: column;
align-items: center;
justify-content: center;
gap: 2px;
padding-bottom: 6px;
color: var(--secondary);
cursor: pointer;
```

### `.tab-item .ticon`

```css
width: 20px;
height: 20px;
```

### `.tab-item .ticon svg`

```css
width: 20px;
height: 20px;
```

### `.tab-item span`

```css
font-size: 9px;
font-weight: 500;
```

### `.tab-item.active`

```css
color: var(--tint);
```

### `.fab`

```css
position: fixed;
right: 20px;
bottom: calc(var(--tab-h) + var(--safe-b) + 20px);
width: 54px;
height: 54px;
border-radius: 50%;
border: none;
background: var(--tint);
color: #fff;
font-size: 26px;
line-height: 1;
box-shadow: 0 4px 16px rgba(0, 122, 255, 0.35);
z-index: 90;
display: none;
align-items: center;
justify-content: center;
cursor: pointer;
```

### `.fab.show`

```css
display: flex;
```

### `.fab:active`

```css
transform: scale(0.92);
```

### `.sheet`

```css
position: fixed;
inset: 0;
z-index: 200;
display: none;
```

### `.sheet.open`

```css
display: block;
```

### `.sheet-mask`

```css
position: absolute;
inset: 0;
background: rgba(0, 0, 0, 0.4);
opacity: 0;
transition: opacity .28s;
```

### `.sheet.open .sheet-mask`

```css
opacity: 1;
```

### `.sheet-panel`

```css
position: absolute;
left: 0;
right: 0;
bottom: 0;
background: var(--bg-elevated);
border-radius: 12px 12px 0 0;
max-height: 92dvh;
display: flex;
flex-direction: column;
transform: translateY(100%);
transition: transform .32s cubic-bezier(.32,.72,0,1);
```

### `.sheet.open .sheet-panel`

```css
transform: translateY(0);
```

### `.sheet-grabber`

```css
width: 36px;
height: 5px;
border-radius: 3px;
background: var(--fill);
margin: 8px auto 0;
```

### `.sheet-head`

```css
display: flex;
align-items: center;
justify-content: space-between;
padding: 10px 16px 12px;
border-bottom: 0.5px solid var(--separator);
```

### `.sheet-title`

```css
font-size: 17px;
font-weight: 600;
```

### `.sheet-btn`

```css
border: none;
background: none;
font-family: inherit;
font-size: 17px;
cursor: pointer;
padding: 4px;
```

### `.sheet-btn.text`

```css
color: var(--secondary);
```

### `.sheet-btn.done`

```css
color: var(--tint);
font-weight: 600;
```

### `.sheet-body`

```css
overflow-y: auto;
padding: 16px 16px calc(24px + var(--safe-b));
```

### `.ios-group`

```css
background: var(--bg-elevated);
border-radius: var(--radius-card);
margin-bottom: 16px;
overflow: hidden;
```

### `.sheet-body .ios-group`

```css
background: var(--bg-secondary);
```

### `.cell input, .cell select, .ios-cell input, .ios-cell select`

```css
border: none;
outline: none;
background: transparent;
font-family: inherit;
font-size: 17px;
color: var(--label);
text-align: right;
flex: 1;
min-width: 0;
```

### `.cell input::placeholder, .ios-cell input::placeholder`

```css
color: var(--tertiary);
```

### `.cell label, .ios-cell label`

```css
flex-shrink: 0;
font-size: 17px;
```

### `.select-cell`

```css
position: relative;
cursor: pointer;
```

### `.select-cell .sel-display`

```css
flex: 1;
text-align: right;
font-size: 17px;
color: var(--label);
```

### `.chevron`

```css
width: 8px;
height: 8px;
border-right: 2px solid var(--tertiary);
border-bottom: 2px solid var(--tertiary);
transform: rotate(45deg);
margin-left: 6px;
flex-shrink: 0;
```

### `.hint`

```css
font-size: 12px;
color: var(--tertiary);
text-align: center;
margin: 4px 0 12px;
```

### `.ai-panel`

```css
height: 88dvh;
```

### `.ai-chat`

```css
flex: 1;
overflow-y: auto;
padding: 16px;
display: flex;
flex-direction: column;
gap: 12px;
background: var(--bg);
```

### `.msg`

```css
max-width: 78%;
padding: 10px 14px;
border-radius: 18px;
font-size: 16px;
line-height: 1.5;
white-space: pre-wrap;
word-break: break-word;
```

### `.msg.user`

```css
align-self: flex-end;
background: var(--tint);
color: #fff;
border-bottom-right-radius: 6px;
```

### `.msg.bot`

```css
align-self: flex-start;
background: var(--bg-elevated);
border-bottom-left-radius: 6px;
box-shadow: 0 1px 2px rgba(0,0,0,0.04);
```

### `.msg.bot.loading`

```css
color: var(--secondary);
```

### `.ai-input`

```css
flex-shrink: 0;
display: flex;
gap: 10px;
align-items: center;
padding: 10px 14px calc(14px + var(--safe-b));
background: var(--bg-elevated);
border-top: 0.5px solid var(--separator);
```

### `.ai-input input`

```css
flex: 1;
border: none;
outline: none;
background: var(--fill);
border-radius: 18px;
padding: 9px 16px;
font-size: 16px;
font-family: inherit;
color: var(--label);
```

### `.send-btn`

```css
width: 36px;
height: 36px;
border-radius: 50%;
border: none;
background: var(--tint);
color: #fff;
cursor: pointer;
display: flex;
align-items: center;
justify-content: center;
```

### `.send-btn:disabled`

```css
opacity: 0.4;
```

### `.send-arrow`

```css
font-size: 18px;
font-weight: 700;
margin-top: -2px;
```

### `.alert`

```css
position: fixed;
inset: 0;
z-index: 300;
display: none;
```

### `.alert.open`

```css
display: block;
```

### `.alert-mask`

```css
position: absolute;
inset: 0;
background: rgba(0,0,0,0.4);
```

### `.alert-box`

```css
position: absolute;
top: 50%;
left: 50%;
transform: translate(-50%, -50%);
width: 270px;
background: rgba(250, 250, 250, 0.97);
backdrop-filter: blur(20px);
-webkit-backdrop-filter: blur(20px);
border-radius: 14px;
overflow: hidden;
text-align: center;
animation: alertIn .18s ease-out;
```

### `.alert-title`

```css
font-size: 17px;
font-weight: 600;
padding: 19px 16px 4px;
```

### `.alert-msg`

```css
font-size: 13px;
color: var(--label-2);
padding: 0 16px 18px;
line-height: 1.45;
```

### `.alert-actions`

```css
display: flex;
border-top: 0.5px solid var(--separator);
```

### `.alert-actions button`

```css
flex: 1;
padding: 12px;
border: none;
background: none;
font-size: 17px;
font-family: inherit;
cursor: pointer;
color: var(--tint);
```

### `.alert-actions button + button`

```css
border-left: 0.5px solid var(--separator);
```

### `.alert-actions .danger`

```css
color: var(--red);
font-weight: 600;
```

### `.alert-actions button:active`

```css
background: var(--fill);
```

### `@media (prefers-color-scheme: dark) >> .alert-box, #toast`

```css
background: rgba(40, 40, 42, 0.97);
```

### `@media (prefers-color-scheme: dark) >> .send-btn:disabled`

```css
opacity: 0.35;
```

### `.model-list`

```css
max-height: 260px;
overflow-y: auto;
padding: 4px 8px 8px;
text-align: left;
```

### `.model-item`

```css
display: flex;
align-items: center;
justify-content: space-between;
width: 100%;
border: none;
background: none;
font-family: inherit;
font-size: 15px;
color: var(--label);
padding: 11px 10px;
border-radius: 8px;
cursor: pointer;
text-align: left;
```

### `.model-item:active`

```css
background: var(--fill);
```

### `.model-item.cur`

```css
color: var(--tint);
font-weight: 600;
```

### `.model-item b`

```css
color: var(--tint);
```

### `.model-item + .model-item`

```css
border-top: 0.5px solid var(--separator);
```

### `#toast`

```css
position: fixed;
top: 18dvh;
left: 50%;
transform: translateX(-50%);
background: rgba(250, 250, 250, 0.97);
backdrop-filter: blur(20px);
color: var(--label);
font-size: 14px;
padding: 10px 18px;
border-radius: 20px;
box-shadow: 0 4px 20px rgba(0,0,0,0.12);
z-index: 400;
opacity: 0;
pointer-events: none;
transition: opacity .25s;
max-width: 80vw;
```

### `#toast.show`

```css
opacity: 1;
```

### `.seg`

```css
display: flex;
background: var(--fill);
border-radius: 9px;
padding: 2px;
margin: 12px 0 4px;
```

### `.seg button`

```css
flex: 1;
border: none;
background: none;
font-family: inherit;
font-size: 13px;
font-weight: 500;
padding: 6px 0;
border-radius: 7px;
color: var(--secondary);
cursor: pointer;
```

### `.seg button.on`

```css
background: var(--bg-elevated);
color: var(--label);
font-weight: 600;
box-shadow: 0 1px 3px rgba(0,0,0,0.08);
```

### `.seg.mini button`

```css
font-size: 12px;
padding: 5px 0;
```

### `.cal`

```css
padding: 12px 16px 8px;
```

### `.cal-head`

```css
display: flex;
align-items: center;
justify-content: space-between;
margin-bottom: 8px;
```

### `.cal-head button`

```css
width: 34px;
height: 34px;
border: none;
background: none;
font-size: 20px;
color: var(--tint);
cursor: pointer;
font-family: inherit;
border-radius: 50%;
```

### `.cal-head button:active`

```css
background: var(--fill);
```

### `.cal-title`

```css
font-size: 16px;
font-weight: 600;
font-variant-numeric: tabular-nums;
```

### `.cal-grid`

```css
display: grid;
grid-template-columns: repeat(7, 1fr);
gap: 2px;
```

### `.cal-wd`

```css
text-align: center;
font-size: 11px;
color: var(--tertiary);
padding: 4px 0;
```

### `.cal-day`

```css
position: relative;
aspect-ratio: 1;
border: none;
background: none;
font-family: inherit;
font-size: 15px;
color: var(--label);
border-radius: 50%;
cursor: pointer;
display: flex;
align-items: center;
justify-content: center;
font-variant-numeric: tabular-nums;
```

### `.cal-day[disabled]`

```css
color: var(--tertiary);
```

### `.cal-day:active`

```css
background: var(--fill);
```

### `.cal-day.tod`

```css
font-weight: 700;
color: var(--tint);
```

### `.cal-day.sel`

```css
background: var(--tint);
color: #fff;
font-weight: 600;
```

### `.cal-day i`

```css
position: absolute;
bottom: 4px;
left: 50%;
transform: translateX(-50%);
width: 4px;
height: 4px;
border-radius: 50%;
background: var(--orange);
```

### `.cal-day.sel i`

```css
background: #fff;
```

### `.cal-dot`

```css
display: inline-block;
width: 5px;
height: 5px;
border-radius: 50%;
background: var(--orange);
vertical-align: middle;
margin-right: 4px;
```

### `.day-header`

```css
display: flex;
align-items: baseline;
justify-content: space-between;
padding: 14px 4px 6px;
```

### `.day-header .d-title`

```css
font-size: 20px;
font-weight: 700;
```

### `.day-header .d-sum`

```css
font-size: 15px;
color: var(--secondary);
font-variant-numeric: tabular-nums;
```

### `.day-header .d-sum b`

```css
color: var(--label);
font-weight: 700;
```

### `.stat-grid`

```css
display: grid;
grid-template-columns: repeat(4, 1fr);
gap: 8px;
margin: 10px 0 4px;
```

### `.stat`

```css
background: var(--bg-elevated);
border-radius: var(--radius-card);
padding: 12px 6px;
text-align: center;
```

### `.stat .v`

```css
font-size: 19px;
font-weight: 700;
font-variant-numeric: tabular-nums;
```

### `.stat .l`

```css
font-size: 11px;
color: var(--secondary);
margin-top: 2px;
```

### `.ring-wrap`

```css
display: flex;
align-items: center;
gap: 18px;
padding: 18px 16px;
```

### `.ring`

```css
position: relative;
width: 96px;
height: 96px;
flex-shrink: 0;
```

### `.ring svg`

```css
width: 96px;
height: 96px;
transform: rotate(-90deg);
```

### `.ring .track`

```css
stroke: var(--fill);
```

### `.ring .val`

```css
stroke: var(--green);
stroke-linecap: round;
transition: stroke-dashoffset .6s ease;
```

### `.ring-center`

```css
position: absolute;
inset: 0;
display: flex;
flex-direction: column;
align-items: center;
justify-content: center;
```

### `.ring-center .pct`

```css
font-size: 22px;
font-weight: 700;
font-variant-numeric: tabular-nums;
```

### `.ring-center .cap`

```css
font-size: 10px;
color: var(--secondary);
```

### `.ring-info`

```css
flex: 1;
```

### `.ring-info .big`

```css
font-size: 15px;
font-weight: 600;
```

### `.ring-info .sub`

```css
font-size: 13px;
color: var(--secondary);
margin-top: 2px;
line-height: 1.5;
```

### `.bar-row`

```css
display: flex;
align-items: center;
gap: 10px;
min-height: 44px;
padding: 4px 16px;
position: relative;
```

### `.bar-row + .bar-row::before`

```css
content: "";
position: absolute;
top: 0;
left: 16px;
right: 0;
height: 0.5px;
background: var(--separator);
transform: scaleY(0.5);
```

### `.bar-row .b-label`

```css
width: 76px;
font-size: 15px;
flex-shrink: 0;
```

### `.bar-row .b-track`

```css
flex: 1;
height: 6px;
background: var(--fill);
border-radius: 3px;
overflow: hidden;
```

### `.bar-row .b-fill`

```css
height: 100%;
border-radius: 3px;
transition: width .5s ease;
```

### `.bar-row .b-val`

```css
width: 92px;
text-align: right;
font-size: 12px;
color: var(--secondary);
font-variant-numeric: tabular-nums;
flex-shrink: 0;
```

### `.b-fill.good`

```css
background: var(--green);
```

### `.b-fill.warn`

```css
background: var(--yellow);
```

### `.b-fill.low`

```css
background: var(--red);
```

### `.b-fill.over`

```css
background: var(--orange);
```

### `.stepper`

```css
display: flex;
align-items: center;
gap: 0;
```

### `.stepper button`

```css
width: 34px;
height: 32px;
border: none;
background: var(--fill);
font-size: 18px;
cursor: pointer;
font-family: inherit;
color: var(--tint);
```

### `.stepper button:first-child`

```css
border-radius: 8px 0 0 8px;
```

### `.stepper button:last-child`

```css
border-radius: 0 8px 8px 0;
```

### `.stepper button + button`

```css
margin-left: 1px;
```

### `.stepper .st-val`

```css
min-width: 28px;
text-align: center;
font-size: 17px;
font-weight: 600;
font-variant-numeric: tabular-nums;
```

### `.ai-card`

```css
display: flex;
align-items: center;
gap: 14px;
padding: 14px 16px;
min-height: 68px;
```

### `.ai-card .cell-icon`

```css
width: 42px;
height: 42px;
border-radius: 10px;
font-size: 19px;
```

### `.ai-card:active`

```css
background: var(--fill);
```

### `.ai-badge`

```css
font-size: 12px;
color: var(--secondary);
```

### `.toggle`

```css
position: relative;
width: 51px;
height: 31px;
flex-shrink: 0;
display: inline-block;
cursor: pointer;
```

### `.toggle input`

```css
opacity: 0;
width: 0;
height: 0;
position: absolute;
margin: 0;
```

### `.toggle .tg`

```css
position: absolute;
inset: 0;
display: block;
background: var(--fill);
border-radius: 16px;
transition: background .2s;
cursor: pointer;
```

### `.toggle .tg::after`

```css
content: "";
position: absolute;
top: 2px;
left: 2px;
width: 27px;
height: 27px;
border-radius: 50%;
background: #fff;
box-shadow: 0 2px 4px rgba(0,0,0,0.15), 0 0 0 0.5px rgba(0,0,0,0.04);
transition: transform .2s;
```

### `.toggle input:checked + .tg`

```css
background: var(--green);
```

### `.toggle input:checked + .tg::after`

```css
transform: translateX(20px);
```

### `.dot-ok`

```css
width: 8px;
height: 8px;
border-radius: 50%;
background: var(--green);
display: inline-block;
```

### `.dot-no`

```css
width: 8px;
height: 8px;
border-radius: 50%;
background: var(--tertiary);
display: inline-block;
```

### `.cell input.left`

```css
text-align: left;
```

### `.cell input.type-key`

```css
-webkit-text-security: disc;
```

### `.cell[data-preset], .cell[id^="btn-"], .ai-card`

```css
cursor: pointer;
```

### `.cell-edit, .cell-del`

```css
min-width: 44px;
min-height: 44px;
```

### `.page.active`

```css
animation: pageIn .22s ease-out;
```

### `.stat .v, .cell-value.money, .b-val, .st-val`

```css
font-variant-numeric: tabular-nums;
```

### `.cell-edit:active, .cell-del:active`

```css
opacity: .5;
```

### `.empty`

```css
text-align: center;
padding: 44px 20px;
color: var(--secondary);
font-size: 15px;
```

### `.empty .e-icon`

```css
font-size: 40px;
margin-bottom: 10px;
opacity: .4;
```

### `.press`

```css
transition: transform .1s;
```

### `.press:active`

```css
transform: scale(.97);
```

### `@media (min-width: 520px) >> body`

```css
background: #e5e5ea;
overflow: auto;
```

### `@media (min-width: 520px) >> #phone`

```css
width: 390px;
height: 844px;
margin: 24px auto;
border-radius: 40px;
box-shadow: 0 0 0 10px #1c1c1e, 0 24px 60px rgba(0,0,0,.35);
overflow: hidden;
position: relative;
background: var(--bg);
display: flex;
flex-direction: column;
```

### `@media (min-width: 520px) >> #phone .tabbar`

```css
position: absolute;
left: 0;
right: 0;
bottom: 0;
```

### `@media (min-width: 520px) >> #phone .fab`

```css
position: absolute;
```

### `@media (min-width: 520px) >> #phone .page`

```css
padding-bottom: calc(var(--tab-h) + 24px);
```

### `@media (min-width: 520px) >> #phone .sheet, #phone .alert`

```css
position: absolute;
```

### `input[type="date"], input[type="time"]`

```css
-webkit-appearance: none;
appearance: none;
background: var(--fill);
border: none;
border-radius: 10px;
padding: 9px 12px;
font-family: inherit;
font-size: 15px;
color: var(--label);
min-height: 40px;
```

### `.cell, .swipe-wrap`

```css
animation: listIn .28s ease-out both;
```

### `.fab.show`

```css
animation: fabIn .3s cubic-bezier(.34,1.56,.64,1);
```

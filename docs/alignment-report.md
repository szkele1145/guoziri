# 原生版 ↔ Web 版 UI 对齐验证报告

> 基准：`web/index.html` + `web/style.css` + `web/app.js`
> 核对方式：脚本直接解析 `style.css` 与 Kotlin 源码，逐项比对数值（`tools/verify_tokens.py`、`tools/verify_colors.py`）
> 核对时间：2026-11-15

## 一、设计令牌（34 项，全部一致）

| 项目 | style.css | Compose | 结果 |
|---|---|---|---|
| 卡片圆角 `--radius-card` | 12px | 12.0 dp | ✓ |
| 图标圆角 `--radius-icon` | 8px | 8.0 dp | ✓ |
| 底部栏高 `--tab-h` | 58px | 58.0 dp | ✓ |
| 页面左右 `.page` | 16px | 16.0 dp | ✓ |
| 导航左右 `.nav` | 20px | 20.0 dp | ✓ |
| 单元格最小高 `.cell` | 52px | 52.0 dp | ✓ |
| 单元格纵向内边距 | 8px | 8.0 dp | ✓ |
| 单元格横向内边距 | 16px | 16.0 dp | ✓ |
| 单元格间距 `.cell gap` | 12px | 12.0 dp | ✓ |
| 分组标题上距 | 18px | 18.0 dp | ✓ |
| 分组标题下距 | 6px | 6.0 dp | ✓ |
| 图标底座 `.cell-icon` | 30px | 30.0 dp | ✓ |
| FAB 尺寸 `.fab` | 54px | 54.0 dp | ✓ |
| FAB 右边距 | 20px | 20.0 dp | ✓ |
| 页面大标题 `.nav-title` | 34px | 34.0 sp | ✓ |
| 正文 `.cell-title` | 17px | 17.0 sp | ✓ |
| 副文 `.cell-sub` | 13px | 13.0 sp | ✓ |
| 分组标题字号 | 13px | 13.0 sp | ✓ |
| 统计数值 `.stat .v` | 19px | 19.0 sp | ✓ |
| 统计标签 `.stat .l` | 11px | 11.0 sp | ✓ |
| 环内百分比 | 22px | 22.0 sp | ✓ |
| 环内说明 | 10px | 10.0 sp | ✓ |
| 达标条标签 | 15px | 15.0 sp | ✓ |
| 达标条数值 | 12px | 12.0 sp | ✓ |
| 日历标题 | 16px | 16.0 sp | ✓ |
| 日历星期 | 11px | 11.0 sp | ✓ |
| 日历日号 | 15px | 15.0 sp | ✓ |
| 聊天气泡 | 16px | 16.0 sp | ✓ |
| 弹窗标题 | 17px | 17.0 sp | ✓ |
| 提示文字 `.hint` | 12px | 12.0 sp | ✓ |
| 空态文字 `.empty` | 15px | 15.0 sp | ✓ |
| 步进数值 `.st-val` | 17px | 17.0 sp | ✓ |
| Tab 文字 | 9px | 9.0 sp | ✓ |
| AI 卡图标 | 19px | 19.0 sp | ✓ |

## 二、颜色（亮/暗各 14 项，全部一致）

| 令牌 | 亮色 | 暗色 |
|---|---|---|
| `bg` | `#F2F2F7` ✓ | `#000000` ✓ |
| `bgElevated` | `#FFFFFF` ✓ | `#1C1C1E` ✓ |
| `bgSecondary` | `#F2F2F7` ✓ | `#1C1C1E` ✓ |
| `label` | `#1C1C1E` ✓ | `#FFFFFF` ✓ |
| `secondary` | `rgba(60,60,67,.6)` ✓ | `rgba(235,235,245,.6)` ✓ |
| `tertiary` | `rgba(60,60,67,.3)` ✓ | `rgba(235,235,245,.3)` ✓ |
| `separator` | `rgba(60,60,67,.29)` ✓ | `rgba(84,84,88,.6)` ✓ |
| `fill` | `rgba(120,120,128,.12)` ✓ | `rgba(120,120,128,.24)` ✓ |
| `tint` | `#007AFF` ✓ | `#0A84FF` ✓ |
| `tintSoft` | `rgba(0,122,255,.12)` ✓ | `rgba(10,132,255,.2)` ✓ |
| `green` | `#34C759` ✓ | `#30D158` ✓ |
| `orange` | `#FF9500` ✓ | `#FF9F0A` ✓ |
| `red` | `#FF3B30` ✓ | `#FF453A` ✓ |
| `yellow` | `#FFCC00` ✓ | `#FFD60A` ✓ |

## 三、组件与页面

| HTML | 原生实现 |
|---|---|
| `#splash`（脉冲图标 + 200×4 进度条） | `SplashOverlay` |
| `.nav` / `.nav-title` / `.nav-sub` | `PageTitle` |
| `.group` / `.group-title` | `GroupCard` / `GroupTitle` |
| `.cell` / `.cell-icon` / `.cell-value` | `Cell` / `IconTile` |
| `.cell:active`（按下列变 fill 底） | `Cell` 内 `collectIsPressedAsState` |
| `.swipe-wrap`（左滑 72px 删除） | `SwipeReveal` |
| `.tabbar` / `.tab-item` | `AppTabBar`（自绘，58dp） |
| `.fab` | `AppRoot` 内自绘 54dp + 蓝色阴影 |
| `.sheet` / `.sheet-head` / `.ios-group` | `SheetHead` / `SheetGroup` / `FormRow` |
| `#action-sheet` + `.model-list`（选择器） | `SheetPicker`（分类 / 外观 / 模型列表） |
| `.seg` 分段控件 | `Segmented` |
| `.toggle` 开关 | `AppToggle` |
| `.stepper` | `Stepper`（单边圆角） |
| `.bar-row` | `BarRow` |
| `.stat` / `.stat-grid` | `StatCard` / 4 列 Row |
| `.ring` 96×96 / r42 / stroke 8 | `NutritionPage` Canvas |
| `.cal` 日历 | `CalendarCard` |
| `.alert-box` 270px | `AlertDialog` + `width(270.dp)` |
| `.msg` / `.ai-input` / `.send-btn` | `ChatSheet` |
| `#toast`（顶部 18dvh、r20、.25s） | `AppRoot` 顶部胶囊 toast |
| `.empty` | `EmptyState` |
| `.hint` | `Hint` |
| 动效 `splashPulse/alertIn/pageIn/fabIn/listIn`、`.press:active` | Compose 动画实现 |

## 四、图标与字体

- **图标**：全部自绘矢量（`AppIcons.kt`），28×28 视口、描边 1.8、圆头圆角；底部 5 个 tab 直接复用 `app.js` 的 SVG 路径；不依赖 `material-icons`，不使用 emoji。
- **字体**：内嵌 `HarmonyOS_Sans_SC.ttf`（取自 `web/fonts/`），经 `Typography` + `LocalTextStyle` 全局生效。

## 五、浏览器计算样式基线（硬核对）

上面的令牌来自解析 CSS 源码；为了排除"我理解错 CSS 语义"的可能，又启动 Flask 后端 + Edge headless，把五页真实渲染后的 `getComputedStyle` 全部导出（`docs/web-computed.json`，脚本 `tools/dump_web_styles.py`），逐项对照。

实测抓出的偏差与修正：

| 元素 | 实测（web） | 修正前 | 处理 |
|---|---|---|---|
| `.ai-card` 总高 | **96**（`min-height:68` 是 content-box，+padding 28） | 73 | 改为 `minHeight 96` |
| `.bar-row .b-val` | 高 34.8（92px 宽下折两行） | 单行截断 | `maxLines=2` + 行高 1.45 |
| `.cell-edit` | 44×44 点击区，字号 15 | 约 30×30 | 改 `Box(44.dp)` 居中 |
| `.cell-icon` 字号 | 16px | 17 | 统一 16 |
| `.stepper button` | 34×32，圆角 `8 0 0 8` / `0 8 8 0` | 双全圆角 | 单边圆角 |
| `.tabbar` 高 | 59（58 + 0.5 线） | 58 + 线 | ✓ |
| `.ring` | 96×96，pct 22/700 | 一致 | ✓ |
| `.stat` | r12，pad 12/6，v 19/700 | 一致 | ✓ |

## 六、弹窗实测与行高修正
对弹窗类组件（记录弹窗、AI 对话、action-sheet、alert、toast）同样做了 `getComputedStyle` 采集（`docs/web-computed-sheets.json`）。这批数据暴露了一个**贯穿全站的系统性偏差**：

> CSS 的 `min-height` 默认是 **content-box**，实际总高要再加上下 padding。

| 类 | CSS 声明 | 实测总高 | 修正前 | 修正 |
|---|---|---|---|---|
| `.cell` | `min-height: 52px` | **68** | 52 | `Dimens.RowH` → 68 |
| `.bar-row` | `min-height: 44px` | **52** | 44 | 52 |
| 记账合计行 | `min-height: 40px` | **56** | 40 | 56 |
| 营养未计入行 | `min-height: 44px` | **60** | 44 | 60 |
| `.ai-card` | `min-height: 68px` | **96** | 73 | 96 |

也就是说，之前**每一行都比基准矮 16px**，列表、表单、设置项全都偏紧。

弹窗其余实测值（均一致）：`.sheet-panel` 圆角 `12 12 0 0`、`.sheet-head` pad 10/16/12 且高 54、`.sheet-title` 17/600、`.sheet-body` pad 16/16/24、`.ios-group` r12/mb16、`.ai-panel` 88dvh、`.msg` r18/pad 10/14/16px（bot 左下 6）、`.ai-input` pad 10/14/14/r18、`.send-btn` 36、`#toast` r20/pad 10/18/14px。

### 6.1 alert（确认框）—— 已改为自绘

Material3 的 AlertDialog 与 web 版结构差别很大：web 是**居中标题 + 底部两个等宽按钮**，Material 是左对齐标题 + 右对齐按钮文字。实测：

| 元素 | 实测 |
|---|---|
| `.alert-box` | 宽 270、圆角 14、背景 97% 不透明（带模糊） |
| `.alert-title` | 17px / 600 / 居中 / padding 19 16 4 |
| `.alert-msg` | 13px / label2 / 居中 / padding 0 16 18 / 行高 1.45 |
| `.alert-actions` | 顶部 0.5px 线，两个 `flex:1` 按钮，中间 0.5px 竖线 |
| `.alert-actions button` | 17px / padding 12 / 取消=tint，确定=danger 时 red 且 600 |

已实现自绘 `AppAlert`，替换「删除确认」「清空数据」两处。

### 6.2 action-sheet —— 实测一致

| 元素 | 实测 | 实现 |
|---|---|---|
| `.sheet-panel` | 圆角 12/12/0/0，底部 padding 8 | ✓ |
| `.sheet-head` | 高 45，padding 10 16 4（无下边框） | ✓ |
| `.model-list` | padding 4 8 8 | ✓ |
| `.model-item` | 高 42、15px、圆角 8、padding 11 10 | ✓ |

### 6.3 补充修正

| 元素 | 实测 | 修正 |
|---|---|---|
| 设置页「拉取模型列表」行 | .cell → 68 | 44 → 68 |
| 日记页时间输入框 | `min-height:40` | 36 → 40 |

## 七、已知差异（非代码可解或有意为之）

1. **tabbar 毛玻璃**：CSS 用 `backdrop-filter: blur(20px)`，Compose 无等价实现，现用 `bg @94%` 近似。
2. **分类图标形状**：HTML 用 emoji（🥬🍎🥩…），按「图标全部手画」的要求改为手绘线性图标，语义对应但笔形不同。
3. **真机渲染**：本环境无 adb / 模拟器，以上为静态核对结论；文字换行、字重、insets 等需真机截图复核。

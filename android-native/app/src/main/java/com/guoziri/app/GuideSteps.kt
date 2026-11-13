package com.guoziri.app

/**
 * 新手引导的步骤定义。全局遮罩层和弹窗内的引导卡共用这一份。
 *
 * 关于窗口层级：Material3 的 ModalBottomSheet 是独立窗口，
 * 永远盖在主窗口之上。所以「添加记录」这一步的引导卡和高亮
 * 必须画在弹窗内部（见 PurchaseSheet 的 guideSlot），
 * 其余步骤画在全局遮罩层（GuideOverlay）。
 */
object GuideSteps {

    data class Step(
        val title: String,
        val body: String,
        val target: String?,           // 锚点 id，null = 不高亮
        val tab: String? = null,       // 该步要切到哪个页
        val inSheet: Boolean = false,  // 该步是否在记账弹窗内展示
    )

    /** 在记账弹窗内展示的步骤索引 */
    const val SHEET_STEP = 3

    val STEPS: List<Step> = listOf(
        Step(
            "欢迎使用 过日子 🥬",
            "记账 · 营养 · AI 助手 · 日常记录，\n一个 App 管好一家人的餐桌。\n\n引导是沙箱模式，不会动真实数据。\n跟着高亮一步步走。",
            target = null,
            tab = "ledger",
        ),
        Step(
            "底部导航 · 四个主页面",
            "这四个图标随时切换页面：\n记账 / 营养 / AI / 设置。\n开启日记入口后这里会出现第 5 个。",
            target = "tabbar",
            tab = "ledger",
        ),
        Step(
            "记一笔 · 点这里",
            "右下角蓝色 ＋ 就是添加按钮，点它弹出表单。\n（下一步会自动打开给你看）",
            target = "fab",
            tab = "ledger",
        ),
        Step(
            "添加记录 · 这样填",
            "菜名有 98 种食材联想，输入会自动提示；\n点「分类」行弹菜单；\n单价+总价 或 斤/克 填一组自动算重量；\n表外的菜可以手动录营养，之后就记住了。",
            target = "purchase-save",
            tab = "ledger",
            inSheet = true,
        ),
        Step(
            "营养页 · 自动评估",
            "圆环是全家总评，下面 8 项是各项达标。\n食材明细会标出哪些是库内、哪些是估算；\n人数在页面底部可调，还能选日期回看、看 7 天趋势。",
            target = "nutrition-ring",
            tab = "nutrition",
        ),
        Step(
            "AI 助手 · 一键总结",
            "两个快捷按钮：总结今天花销 / 营养，\n点开即对话、可追问。\n使用前在设置页填 API Key。",
            target = "ai-quick",
            tab = "ai",
        ),
        Step(
            "设置 · API Key 在这填",
            "选服务商 → 填 Key → 点保存；\n可开日记入口、切深浅色、\n也可以从这里重播本引导。",
            target = "settings-key",
            tab = "settings",
        ),
        Step(
            "完成！",
            "引导结束，沙箱关闭，\n真实数据原样保留。\n\n去记第一笔真实的账吧！",
            target = null,
            tab = "ledger",
        ),
    )

    val size: Int get() = STEPS.size
}

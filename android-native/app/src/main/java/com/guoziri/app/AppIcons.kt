package com.guoziri.app

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * 手绘图标集：全部为 28×28 视口的矢量路径，描边 1.8、圆头圆角。
 * 不依赖 material-icons 库，也不使用 emoji。
 *
 * 底部 5 个 tab 的路径直接取自 web 版 app.js 的 ICONS（一字不差）；
 * 分类与功能图标按同一视觉规范手绘。
 */
object AppIcons {

    /** 一条矢量路径：d 为 SVG path data，fillAlpha>0 时同时填充 */
    private data class P(val d: String, val fillAlpha: Float = 0f, val w: Float = 1.8f)

    private fun s(d: String, w: Float = 1.8f) = P(d, 0f, w)
    private fun f(d: String, alpha: Float = 1f, w: Float = 1.8f) = P(d, alpha, w)

    private fun build(name: String, vararg parts: P): ImageVector {
        val b = ImageVector.Builder(
            name = name,
            defaultWidth = 28.dp,
            defaultHeight = 28.dp,
            viewportWidth = 28f,
            viewportHeight = 28f,
        )
        parts.forEach { p ->
            b.addPath(
                pathData = PathParser().parsePathString(p.d).toNodes(),
                fill = if (p.fillAlpha > 0f) SolidColor(Color.Black) else null,
                fillAlpha = p.fillAlpha,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = p.w,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
        return b.build()
    }

    /* ==================== 底部导航（取自 web 版 SVG） ==================== */

    /** 笔记本：外框 + 3 条横线 */
    val Ledger: ImageVector by lazy {
        build(
            "tab_ledger",
            s("M8.5,3.5 H19.5 A3.5,3.5 0 0 1 23,7 V21 A3.5,3.5 0 0 1 19.5,24.5 H8.5 A3.5,3.5 0 0 1 5,21 V7 A3.5,3.5 0 0 1 8.5,3.5 Z"),
            s("M9.5 9.5h9M9.5 14h9M9.5 18.5h5.5"),
        )
    }

    /** 叶子 + 叶脉（叶面 15% 填充） */
    val Nutrition: ImageVector by lazy {
        build(
            "tab_nutrition",
            f("M14 25c-1 0-1.9-.4-2.6-1.1C7.4 19.7 4.5 15.6 4.5 11.5 4.5 6.6 8.7 3 14 3s9.5 3.6 9.5 8.5c0 4.1-2.9 8.2-6.9 12.4-.7.7-1.6 1.1-2.6 1.1z", 0.15f),
            s("M14 24V9"),
            s("M14 14l4.5-3.5"),
            s("M14 18.5L9.5 15"),
        )
    }

    /** 四角星 + 小圆点 */
    val Ai: ImageVector by lazy {
        build(
            "tab_ai",
            s("M14 4l1.8 4.6a4 4 0 0 0 2.6 2.6L23 13l-4.6 1.8a4 4 0 0 0-2.6 2.6L14 22l-1.8-4.6a4 4 0 0 0-2.6-2.6L5 13l4.6-1.8a4 4 0 0 0 2.6-2.6L14 4z"),
            s("M20.3,21.5 A2.2,2.2 0 1 1 24.7,21.5 A2.2,2.2 0 1 1 20.3,21.5 Z"),
        )
    }

    /** 齿轮：中心圆 + 8 条辐条 */
    val Settings: ImageVector by lazy {
        build(
            "tab_settings",
            s("M10.8,14 A3.2,3.2 0 1 1 17.2,14 A3.2,3.2 0 1 1 10.8,14 Z"),
            s("M14 3.5v3M14 21.5v3M3.5 14h3M21.5 14h3M6.6 6.6l2.1 2.1M19.3 19.3l2.1 2.1M21.4 6.6l-2.1 2.1M8.7 19.3l-2.1 2.1"),
        )
    }

    /** 日记本：外框 + 3 行 + 书脊 */
    val Diary: ImageVector by lazy {
        build(
            "tab_diary",
            s("M8,4 H20 A3,3 0 0 1 23,7 V21 A3,3 0 0 1 20,24 H8 A3,3 0 0 1 5,21 V7 A3,3 0 0 1 8,4 Z"),
            s("M9 9h10M9 13.5h10M9 18h6"),
            s("M5 8.5h3M5 14h3M5 19.5h3"),
        )
    }

    /* ==================== 分类（手绘） ==================== */

    /** 蔬菜：叶球 + 中脉 + 侧脉 */
    val Veggie: ImageVector by lazy {
        build(
            "cat_veggie",
            s("M14 25c-5 0-8-4-8-9 0-5 3.5-8 8-8s8 3 8 8c0 5-3 9-8 9z"),
            s("M14 25V8"),
            s("M14 15l4-3.5"),
            s("M14 19l-4-3.5"),
        )
    }

    /** 水果：苹果身 + 梗 + 叶 */
    val Fruit: ImageVector by lazy {
        build(
            "cat_fruit",
            s("M14 10c-3.5-2-7 0-7 4.5 0 5 3 9.5 7 9.5s7-4.5 7-9.5C21 10 17.5 8 14 10z"),
            s("M14 10V5.5"),
            s("M14 6.5c1.5-1.8 4-2 5-0.5-1.5 1.8-3.8 1.8-5 .5z"),
        )
    }

    /** 肉类：肉排 + 两道纹理 */
    val Meat: ImageVector by lazy {
        build(
            "cat_meat",
            s("M8 13c0-4 3-7 7-7s8 3 8 7c0 5-4 9-8.5 9S8 18 8 13z"),
            s("M11.5 13c0.8-1.6 2.2-2.4 3.5-1.8"),
            s("M11.5 17.5c0.8-1.6 2.2-2.4 3.5-1.8"),
        )
    }

    /** 海鲜：鱼身 + 尾 + 眼 */
    val Seafood: ImageVector by lazy {
        build(
            "cat_seafood",
            s("M6 14c3-4 8-5 12-2l4-3-1 5 1 5-4-3c-4 3-9 2-12-2z"),
            f("M11.5,12.5 A1,1 0 1 1 13.5,12.5 A1,1 0 1 1 11.5,12.5 Z", 1f, 0f),
        )
    }

    /** 蛋奶：蛋形 */
    val Dairy: ImageVector by lazy {
        build(
            "cat_dairy",
            s("M14 4c-3.5 0-6.5 5-6.5 10S10.5 23 14 23s6.5-4 6.5-9S17.5 4 14 4z"),
            s("M11 13c0.5-2 1.5-3.5 3-4"),
        )
    }

    /** 主食：碗 + 热气 */
    val Staple: ImageVector by lazy {
        build(
            "cat_staple",
            s("M5 13h18c0 5.5-4 9-9 9s-9-3.5-9-9z"),
            s("M9 13v9M19 13v9"),
            s("M11 8.5c0-1.5 1.5-1.5 1.5-3"),
            s("M15.5 8.5c0-1.5 1.5-1.5 1.5-3"),
        )
    }

    /** 调料：盐瓶 */
    val Seasoning: ImageVector by lazy {
        build(
            "cat_seasoning",
            s("M10 12h8v10c0 1-.8 2-1.8 2h-4.4C10.8 24 10 23 10 22z"),
            s("M11 8h6v4h-6z"),
            s("M13 10h2"),
        )
    }

    /** 零食：饼干 + 3 个糖粒 */
    val Snack: ImageVector by lazy {
        build(
            "cat_snack",
            s("M14 5 A9 9 0 1 1 13.99 5 Z"),
            f("M10.5,10.5 A1.1,1.1 0 1 1 12.7,10.5 A1.1,1.1 0 1 1 10.5,10.5 Z", 1f, 0f),
            f("M16.3,12.8 A1.1,1.1 0 1 1 18.5,12.8 A1.1,1.1 0 1 1 16.3,12.8 Z", 1f, 0f),
            f("M11.6,16.4 A1.1,1.1 0 1 1 13.8,16.4 A1.1,1.1 0 1 1 11.6,16.4 Z", 1f, 0f),
        )
    }

    /** 其他：购物袋 */
    val Other: ImageVector by lazy {
        build(
            "cat_other",
            s("M6 10h16l-1.5 13c-.1.6-.5 1-1.1 1H8.6c-.6 0-1-.4-1.1-1z"),
            s("M10.5 10V8c0-2 1.5-3.5 3.5-3.5S17.5 6 17.5 8v2"),
        )
    }

    /* ==================== 功能（手绘） ==================== */

    /** 加号 */
    val Add: ImageVector by lazy { build("i_add", s("M14 6v16"), s("M6 14h16")) }

    /** 减号 */
    val Remove: ImageVector by lazy { build("i_remove", s("M6 14h16")) }

    /** 搜索 */
    val Search: ImageVector by lazy {
        build(
            "i_search",
            s("M11.5,4 A7.5,7.5 0 1 1 11.49,4 Z"),
            s("M17 17l5 5"),
        )
    }

    /** 铅笔 */
    val Edit: ImageVector by lazy {
        build(
            "i_edit",
            s("M6 22l1.8-6L18.5 5.3l4.2 4.2L12 20.2z"),
            s("M16.5 7.3l4.2 4.2"),
        )
    }

    /** 垃圾桶 */
    val Delete: ImageVector by lazy {
        build(
            "i_delete",
            s("M6 9h16"),
            s("M11 9V6h6v3"),
            s("M8 9l1 14h10l1-14"),
            s("M12 13v6M16 13v6"),
        )
    }

    /** 勾 */
    val Check: ImageVector by lazy { build("i_check", s("M6 14l5 5L22 8")) }

    /** 叉 */
    val Close: ImageVector by lazy { build("i_close", s("M7 7l14 14"), s("M21 7L7 21")) }

    /** 右尖角 */
    val ChevronRight: ImageVector by lazy { build("i_chev_r", s("M11 6l8 8-8 8")) }

    /** 左尖角 */
    val ChevronLeft: ImageVector by lazy { build("i_chev_l", s("M17 6l-8 8 8 8")) }

    /** 下尖角 */
    val ChevronDown: ImageVector by lazy { build("i_chev_d", s("M6 11l8 8 8-8")) }

    /** 时钟 */
    val Clock: ImageVector by lazy {
        build(
            "i_clock",
            s("M14 5 A9 9 0 1 1 13.99 5 Z"),
            s("M14 8.5V14l4 2.5"),
        )
    }

    /** 日历 */
    val Calendar: ImageVector by lazy {
        build(
            "i_calendar",
            s("M7,6 H21 A2,2 0 0 1 23,8 V22 A2,2 0 0 1 21,24 H7 A2,2 0 0 1 5,22 V8 A2,2 0 0 1 7,6 Z"),
            s("M5 11h18"),
            s("M10 3.5V8M18 3.5V8"),
        )
    }

    /** 问号（未识别食材） */
    val Help: ImageVector by lazy {
        build(
            "i_help",
            s("M14 5 A9 9 0 1 1 13.99 5 Z"),
            s("M11 11.5c0-1.7 1.3-3 3-3s3 1.3 3 3c0 2.2-3 2.4-3 5"),
            f("M13.1,20 A1.1,1.1 0 1 1 15.3,20 A1.1,1.1 0 1 1 13.1,20 Z", 1f, 0f),
        )
    }

    /** 发送（上） */
    val Send: ImageVector by lazy { build("i_send", s("M14 21V7"), s("M8 13l6-6 6 6")) }

    /** 钱（¥） */
    val Money: ImageVector by lazy {
        build(
            "i_money",
            s("M9 7l5 7 5-7"),
            s("M14 14v8"),
            s("M10 16h8M10 19.5h8"),
        )
    }

    /** 聊天气泡 */
    val Chat: ImageVector by lazy {
        build(
            "i_chat",
            s("M6 9c0-1.7 1.3-3 3-3h10c1.7 0 3 1.3 3 3v7c0 1.7-1.3 3-3 3h-7l-5 4v-4c-.6-.4-1-1.1-1-2z"),
        )
    }

    /** 更多（三点） */
    val More: ImageVector by lazy {
        build(
            "i_more",
            f("M6.4,14 A1.6,1.6 0 1 1 9.6,14 A1.6,1.6 0 1 1 6.4,14 Z", 1f, 0f),
            f("M12.4,14 A1.6,1.6 0 1 1 15.6,14 A1.6,1.6 0 1 1 12.4,14 Z", 1f, 0f),
            f("M18.4,14 A1.6,1.6 0 1 1 21.6,14 A1.6,1.6 0 1 1 18.4,14 Z", 1f, 0f),
        )
    }

    /** 三角警示 */
    val Warning: ImageVector by lazy {
        build(
            "i_warning",
            s("M14 4l10 17H4z"),
            s("M14 10v5"),
            f("M12.9,19 A1.1,1.1 0 1 1 15.1,19 A1.1,1.1 0 1 1 12.9,19 Z", 1f, 0f),
        )
    }

    /** 书 */
    val Book: ImageVector by lazy {
        build(
            "i_book",
            s("M8,4 H20 A3,3 0 0 1 23,7 V21 A3,3 0 0 1 20,24 H8 A3,3 0 0 1 5,21 V7 A3,3 0 0 1 8,4 Z"),
            s("M9 9h10M9 13.5h10M9 18h6"),
        )
    }

    /** 餐具 */
    val Restaurant: ImageVector by lazy {
        build(
            "i_restaurant",
            s("M9 4v7c0 1.7 1.3 3 3 3v10"),
            s("M12 14V4"),
            s("M18.5 4c-1.4 1.5-2 3.5-2 6 0 1.7.7 3 2 3.2V24"),
        )
    }

    /** 叶子（空态/启动页） */
    val Leaf: ImageVector by lazy {
        build(
            "i_leaf",
            s("M5 23C5 13 12 6 23 5c1 11-6 18-16 18z"),
            s("M5 23c4-5 8-8 13-10"),
        )
    }
}

/** 分类 → 手绘图标 */
fun categoryIcon(category: String): ImageVector = when (category) {
    "蔬菜" -> AppIcons.Veggie
    "水果" -> AppIcons.Fruit
    "肉类" -> AppIcons.Meat
    "海鲜" -> AppIcons.Seafood
    "蛋奶" -> AppIcons.Dairy
    "主食" -> AppIcons.Staple
    "调料" -> AppIcons.Seasoning
    "零食" -> AppIcons.Snack
    else -> AppIcons.Other
}

/** 分类 → 底色（与 web 版 CAT_COLOR 一致） */
fun categoryTint(category: String): Color = when (category) {
    "蔬菜" -> Color(0xFFE8F5E9)
    "水果" -> Color(0xFFFFF3E0)
    "肉类" -> Color(0xFFFFEBEE)
    "海鲜" -> Color(0xFFE3F2FD)
    "蛋奶" -> Color(0xFFFFF8E1)
    "主食" -> Color(0xFFEDE7F6)
    "调料" -> Color(0xFFEFEBE9)
    "零食" -> Color(0xFFFCE4EC)
    else -> Color(0xFFF5F5F5)
}

/** 分类 → 图标着色 */
fun categoryIconTint(category: String): Color = when (category) {
    "蔬菜" -> Color(0xFF34A853)
    "水果" -> Color(0xFFFF8A00)
    "肉类" -> Color(0xFFEA4335)
    "海鲜" -> Color(0xFF4285F4)
    "蛋奶" -> Color(0xFFFBBC04)
    "主食" -> Color(0xFF9C27B0)
    "调料" -> Color(0xFF8D6E63)
    "零食" -> Color(0xFFD81B60)
    else -> Color(0xFF9E9E9E)
}

/** tab id → 手绘图标 */
fun tabIconVector(id: String): ImageVector = when (id) {
    "ledger" -> AppIcons.Ledger
    "nutrition" -> AppIcons.Nutrition
    "ai" -> AppIcons.Ai
    "diary" -> AppIcons.Diary
    else -> AppIcons.Settings
}

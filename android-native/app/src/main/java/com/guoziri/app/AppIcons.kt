package com.guoziri.app

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * 统一图标体系。
 * 全部用 Material Icons 矢量图标，替代 emoji —— emoji 在各家
 * ROM 上渲染差异大、基线不齐、还会撑破行高。
 */
object Icons2 {
    // 底部导航
    val Ledger = Icons.Rounded.ReceiptLong
    val Nutrition = Icons.Rounded.Restaurant
    val Ai = Icons.Rounded.AutoAwesome
    val Diary = Icons.Rounded.MenuBook
    val Settings = Icons.Rounded.Settings

    // 分类
    val Veggie = Icons.Rounded.Eco
    val Fruit = Icons.Rounded.Apple
    val Meat = Icons.Rounded.SetMeal
    val Seafood = Icons.Rounded.Anchor
    val Dairy = Icons.Rounded.Egg
    val Staple = Icons.Rounded.RiceBowl
    val Seasoning = Icons.Rounded.SoupKitchen
    val Snack = Icons.Rounded.Cookie
    val Other = Icons.Rounded.ShoppingBasket

    // 功能
    val Add = Icons.Rounded.Add
    val Search = Icons.Rounded.Search
    val Edit = Icons.Rounded.Edit
    val Delete = Icons.Rounded.DeleteOutline
    val Check = Icons.Rounded.Check
    val Close = Icons.Rounded.Close
    val Chevron = Icons.Rounded.ChevronRight
    val ChevronLeft = Icons.Rounded.ChevronLeft
    val Schedule = Icons.Rounded.Schedule
    val Money = Icons.Rounded.Payments
    val Trend = Icons.Rounded.TrendingUp
    val Calendar = Icons.Rounded.CalendarMonth
    val Info = Icons.Rounded.InfoOutline
    val Replay = Icons.Rounded.Replay
    val Bookmark = Icons.Rounded.BookmarkBorder
    val Warning = Icons.Rounded.WarningAmber
    val Sparkle = Icons.Rounded.AutoAwesome
}

/** 分类 → 图标 */
fun categoryIcon(category: String): ImageVector = when (category) {
    "蔬菜" -> Icons2.Veggie
    "水果" -> Icons2.Fruit
    "肉类" -> Icons2.Meat
    "海鲜" -> Icons2.Seafood
    "蛋奶" -> Icons2.Dairy
    "主食" -> Icons2.Staple
    "调料" -> Icons2.Seasoning
    "零食" -> Icons2.Snack
    else -> Icons2.Other
}

/** 分类 → 底色（淡雅色，卡片内嵌方块用） */
fun categoryTint(category: String): androidx.compose.ui.graphics.Color = when (category) {
    "蔬菜" -> androidx.compose.ui.graphics.Color(0xFFE8F5E9)
    "水果" -> androidx.compose.ui.graphics.Color(0xFFFFF3E0)
    "肉类" -> androidx.compose.ui.graphics.Color(0xFFFFEBEE)
    "海鲜" -> androidx.compose.ui.graphics.Color(0xFFE3F2FD)
    "蛋奶" -> androidx.compose.ui.graphics.Color(0xFFFFF8E1)
    "主食" -> androidx.compose.ui.graphics.Color(0xFFEDE7F6)
    "调料" -> androidx.compose.ui.graphics.Color(0xFFEFEBE9)
    "零食" -> androidx.compose.ui.graphics.Color(0xFFFCE4EC)
    else -> androidx.compose.ui.graphics.Color(0xFFF5F5F5)
}

/** 分类 → 图标着色 */
fun categoryIconTint(category: String): androidx.compose.ui.graphics.Color = when (category) {
    "蔬菜" -> androidx.compose.ui.graphics.Color(0xFF34A853)
    "水果" -> androidx.compose.ui.graphics.Color(0xFFFF8A00)
    "肉类" -> androidx.compose.ui.graphics.Color(0xFFEA4335)
    "海鲜" -> androidx.compose.ui.graphics.Color(0xFF4285F4)
    "蛋奶" -> androidx.compose.ui.graphics.Color(0xFFFBBC04)
    "主食" -> androidx.compose.ui.graphics.Color(0xFF9C27B0)
    "调料" -> androidx.compose.ui.graphics.Color(0xFF8D6E63)
    "零食" -> androidx.compose.ui.graphics.Color(0xFFD81B60)
    else -> androidx.compose.ui.graphics.Color(0xFF9E9E9E)
}

/** tab id → 图标 */
fun tabIconVector(id: String): ImageVector = when (id) {
    "ledger" -> Icons2.Ledger
    "nutrition" -> Icons2.Nutrition
    "ai" -> Icons2.Ai
    "diary" -> Icons2.Diary
    else -> Icons2.Settings
}

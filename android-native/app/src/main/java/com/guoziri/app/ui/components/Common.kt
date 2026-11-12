package com.guoziri.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

val CAT_ICON = mapOf(
    "蔬菜" to "🥬", "水果" to "🍎", "肉类" to "🥩", "海鲜" to "🦐", "蛋奶" to "🥛",
    "主食" to "🍚", "调料" to "🧂", "零食" to "🍪", "其他" to "🛒",
)
val CAT_COLOR = mapOf(
    "蔬菜" to Color(0xFFE8F5E9), "水果" to Color(0xFFFFF3E0), "肉类" to Color(0xFFFFEBEE),
    "海鲜" to Color(0xFFE3F2FD), "蛋奶" to Color(0xFFFFF8E1), "主食" to Color(0xFFEDE7F6),
    "调料" to Color(0xFFEFEBE9), "零食" to Color(0xFFFCE4EC), "其他" to Color(0xFFF5F5F5),
)
val CATEGORIES = CAT_ICON.keys.toList()

fun money(n: Double?) = "¥" + String.format(Locale.US, "%.2f", n ?: 0.0)
fun today(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
fun fmtDay(ds: String): String = try {
    val p = ds.split("-")
    "${p[1].toInt()}月${p[2].toInt()}日"
} catch (e: Exception) { ds }
fun weekDay(ds: String): String = try {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    "周" + "日一二三四五六"[sdf.parse(ds)!!.day]
} catch (e: Exception) { "" }
fun weightText(p: Purchase): String {
    val w = Nutrition.parseWeight(p)
    return when {
        w >= 1000 -> String.format(Locale.US, "%.2fkg", w / 1000)
        w > 0 -> "${w.toInt()}g"
        else -> ""
    }
}

/* ---------------- 通用小组件 ---------------- */
@Composable
fun StatCard(value: String, label: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
        ) {
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f), modifier = Modifier.padding(top = 2.dp))
        }
    )
}

@Composable
fun EmptyState(icon: String, text: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
    ) {
        Text(icon, fontSize = 44.sp)
        Text(text, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), lineHeight = 24.sp)
    }
}

@Composable
fun GroupCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(content = content)
    }
}

@Composable
fun GroupTitle(text: String) {
    Text(
        text,
        fontSize = 13.sp,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
        modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 6.dp),
    )
}
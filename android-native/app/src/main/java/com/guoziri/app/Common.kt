package com.guoziri.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/* ==================================================================
   设计规范
   ================================================================== */
object Dimens {
    /** 页面左右安全边距 */
    val PageH = 20.dp
    /** 卡片圆角 */
    val CardR = 16.dp
    /** 卡片内边距 */
    val CardPad = 16.dp
    /** 分组标题上下留白 */
    val GroupTop = 22.dp
    val GroupBottom = 8.dp
    /** 分隔线 */
    val Hairline = 0.5.dp
    /** 列表行高 */
    val RowH = 56.dp
}

object Dimens2 {
    val Title = 30.sp
    val Section = 15.sp
    val Body = 15.sp
    val Label = 13.sp
    val Caption = 12.sp
    val Micro = 11.sp
    val StatValue = 19.sp
}

val CAT_COLOR = mapOf(
    "蔬菜" to Color(0xFFE8F5E9), "水果" to Color(0xFFFFF3E0), "肉类" to Color(0xFFFFEBEE),
    "海鲜" to Color(0xFFE3F2FD), "蛋奶" to Color(0xFFFFF8E1), "主食" to Color(0xFFEDE7F6),
    "调料" to Color(0xFFEFEBE9), "零食" to Color(0xFFFCE4EC), "其他" to Color(0xFFF5F5F5),
)
val CATEGORIES = listOf(
    "蔬菜", "水果", "肉类", "海鲜", "蛋奶", "主食", "调料", "零食", "其他",
)

fun money(n: Double?): String {
    val v = n ?: 0.0
    return if (v >= 10000) "¥%.1f万".format(Locale.US, v / 10000)
    else "¥%.2f".format(Locale.US, v)
}

fun moneyShort(n: Double?): String {
    val v = n ?: 0.0
    return "¥" + String.format(Locale.US, "%.0f", v)
}

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

/* ==================================================================
   通用组件
   ================================================================== */

/** 页面大标题 */
@Composable
fun PageTitle(text: String, modifier: Modifier = Modifier, subtitle: String? = null) {
    Column(modifier.padding(start = Dimens.PageH, top = 8.dp, end = Dimens.PageH, bottom = 14.dp)) {
        Text(
            text,
            fontSize = Dimens2.Title,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.5).sp,
        )
        if (subtitle != null) {
            Text(
                subtitle,
                fontSize = Dimens2.Caption,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                modifier = Modifier.padding(top = 3.dp),
            )
        }
    }
}

/** 统计卡片：数值单行不换行 */
@Composable
fun StatCard(value: String, label: String, modifier: Modifier = Modifier, accent: Color? = null) {
    Surface(
        shape = RoundedCornerShape(Dimens.CardR),
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 12.dp),
        ) {
            Text(
                value,
                fontSize = Dimens2.StatValue,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                softWrap = false,
                color = accent ?: MaterialTheme.colorScheme.onSurface,
            )
            Text(
                label,
                fontSize = Dimens2.Micro,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 3.dp),
            )
        }
    }
}

/** 空状态 */
@Composable
fun EmptyState(icon: ImageVector, text: String, modifier: Modifier = Modifier, hint: String? = null) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 40.dp, vertical = 56.dp),
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f),
            modifier = Modifier.size(46.dp),
        )
        Text(
            text,
            fontSize = Dimens2.Body,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
            textAlign = TextAlign.Center,
            lineHeight = 22.sp,
            modifier = Modifier.padding(top = 14.dp),
        )
        if (hint != null) {
            Text(
                hint,
                fontSize = Dimens2.Caption,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

/** 卡片容器 */
@Composable
fun GroupCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(Dimens.CardR),
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(content = content)
    }
}

/** 分组标题 */
@Composable
fun GroupTitle(text: String, modifier: Modifier = Modifier, trailing: (@Composable () -> Unit)? = null) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(start = Dimens.PageH, top = Dimens.GroupTop, end = Dimens.PageH, bottom = Dimens.GroupBottom),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text,
            fontSize = Dimens2.Label,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            modifier = Modifier.weight(1f),
        )
        trailing?.invoke()
    }
}

/** 卡片内的一行：标题 + 副标题 + 右侧内容 */
@Composable
fun SettingRow(
    icon: ImageVector?,
    title: String,
    subtitle: String? = null,
    trailing: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    iconTint: Color? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = Dimens.CardPad, vertical = 12.dp),
    ) {
        if (icon != null) {
            Icon(
                icon,
                contentDescription = null,
                tint = iconTint ?: MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                modifier = Modifier.size(19.dp),
            )
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = Dimens2.Body, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) {
                Text(
                    subtitle,
                    fontSize = Dimens2.Caption,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.42f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        trailing?.invoke()
    }
}

/** 卡片内分隔线 */
@Composable
fun CardDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        thickness = Dimens.Hairline,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
        modifier = modifier.padding(start = Dimens.CardPad),
    )
}

/** 圆角方形图标底座 */
@Composable
fun IconTile(
    icon: ImageVector,
    bg: Color,
    tint: Color,
    size: Dp = 38.dp,
    iconSize: Dp = 20.dp,
) {
    Box(
        Modifier.size(size).clip(RoundedCornerShape(11.dp)).background(bg),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(iconSize))
    }
}

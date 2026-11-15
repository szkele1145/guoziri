package com.guoziri.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.guoziri.app.ui.theme.LocalAppColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/* ==================================================================
   设计规范 —— 全部数值 1:1 取自 web/style.css
   ================================================================== */
object Dimens {
    /** .page padding 左右 16 */
    val PageH = 16.dp
    /** .nav padding 左右 20 */
    val NavH = 20.dp
    /** --radius-card 12px */
    val CardR = 12.dp
    /** .cell padding 左右 16 / 上下 8 */
    val CardPad = 16.dp
    val CardPadV = 8.dp
    /** .group-title margin-top 18 / padding-bottom 6 */
    val GroupTop = 18.dp
    val GroupBottom = 6.dp
    /** .cell 分隔线 0.5px，左起 16 */
    val Hairline = 0.5.dp
    /** .cell min-height 52 */
    val RowH = 52.dp
    /** .cell gap 12 */
    val CellGap = 12.dp
    /** .group margin 8 0 20 */
    val GroupGapTop = 8.dp
    val GroupGapBottom = 20.dp
    /** 按钮/输入圆角 */
    val BtnR = 10.dp
    val InputR = 10.dp
    /** --radius-icon 8px */
    val IconR = 8.dp
    /** .cell-icon 30x30 */
    val IconTile = 30.dp
    /** --tab-h 58 */
    val TabH = 58.dp
    /** --nav-h 52 */
    val TopH = 52.dp
    /** .fab 54x54，right 20，bottom tab+20 */
    val Fab = 54.dp
    val FabRight = 20.dp
    val FabBottom = 20.dp
}

object Dimens2 {
    /** .nav-title 34 / 700 / ls .37px / lh 1.2 */
    val Title = 34.sp
    /** .nav-sub 13 */
    val NavSub = 13.sp
    /** .group-title 13 */
    val Label = 13.sp
    /** .cell-title / .cell-value / 表单 17 */
    val Body = 17.sp
    /** .cell-sub 13 */
    val Caption = 13.sp
    /** .stat .l 11 / .cal-wd 11 */
    val Micro = 11.sp
    /** .stat .v 19 */
    val StatValue = 19.sp
    val StatLabel = 11.sp
    /** .ring-center .pct 22 / .cap 10 */
    val RingPct = 22.sp
    val RingCap = 10.sp
    /** .ring-info .big 15 / .sub 13 */
    val RingBig = 15.sp
    val RingSub = 13.sp
    /** .bar-row .b-label 15 / .b-val 12 */
    val BarLabel = 15.sp
    val BarValue = 12.sp
    /** .day-header .d-title 20 / .d-sum 15 */
    val DayTitle = 20.sp
    val DaySum = 15.sp
    /** .cal-title 16 / .cal-wd 11 / .cal-day 15 */
    val CalTitle = 16.sp
    val CalWd = 11.sp
    val CalDay = 15.sp
    /** .msg 16 */
    val Msg = 16.sp
    /** .sheet-title / .alert-title 17 */
    val SheetTitle = 17.sp
    /** .alert-msg 13 */
    val AlertMsg = 13.sp
    /** .hint 12 */
    val Hint = 12.sp
    /** .empty 15 */
    val Empty = 15.sp
    /** .cell-edit / .cell-del 15 */
    val Action = 15.sp
    /** .stepper .st-val 17 */
    val StepperVal = 17.sp
    /** .tab-item span 9 */
    val TabLabel = 9.sp
    /** .ai-card .cell-icon 19 / .ai-badge 12 */
    val AiCardIcon = 19.sp
    val AiBadge = 12.sp
    /** 兼容旧键 */
    val Section = 15.sp
}

/**
 * 语义色：全部取自 LocalAppColors（即 web/style.css 的令牌）。
 * 页面里不要直接写十六进制色值。
 */
object SemanticColors {
    val Primary: Color @Composable get() = LocalAppColors.current.tint
    val Accent: Color @Composable get() = LocalAppColors.current.orange
    val Success: Color @Composable get() = LocalAppColors.current.green
    val Warning: Color @Composable get() = LocalAppColors.current.orange
    val Yellow: Color @Composable get() = LocalAppColors.current.yellow
    val Error: Color @Composable get() = LocalAppColors.current.red
    val Disabled: Color @Composable get() = LocalAppColors.current.tertiary
    val Divider: Color @Composable get() = LocalAppColors.current.separator
    val InputBg: Color @Composable get() = LocalAppColors.current.fill
    val Fill: Color @Composable get() = LocalAppColors.current.fill
    val TextPrimary: Color @Composable get() = LocalAppColors.current.label
    val TextSecondary: Color @Composable get() = LocalAppColors.current.secondary
    val TextTertiary: Color @Composable get() = LocalAppColors.current.tertiary
    val TextQuaternary: Color @Composable get() = LocalAppColors.current.tertiary
    val TintSoft: Color @Composable get() = LocalAppColors.current.tintSoft
    val Label2: Color @Composable get() = LocalAppColors.current.label2
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

/** .nav + .nav-title + .nav-sub（padding 14 20 6，34/700/ls .37px/lh 1.2） */
@Composable
fun PageTitle(
    text: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(start = Dimens.NavH, top = 14.dp, end = Dimens.NavH, bottom = 6.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text,
                fontSize = Dimens2.Title,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.37.sp,
                lineHeight = (Dimens2.Title.value * 1.2f).sp,
                color = SemanticColors.TextPrimary,
            )
            if (subtitle != null) {
                Text(
                    subtitle,
                    fontSize = Dimens2.NavSub,
                    color = SemanticColors.TextSecondary,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        trailing?.invoke(this)
    }
}

/** .group：bg-elevated + 卡片圆角 12 */
@Composable
fun GroupCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(Dimens.CardR),
        color = LocalAppColors.current.bgElevated,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(content = content)
    }
}

/** .group-title：13 / secondary / ls .06em / padding 0 16 6 / margin-top 18 */
@Composable
fun GroupTitle(
    text: String,
    modifier: Modifier = Modifier,
    topSpacing: Dp = Dimens.GroupTop,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(start = Dimens.PageH, top = topSpacing, end = Dimens.PageH, bottom = Dimens.GroupBottom),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text,
            fontSize = Dimens2.Label,
            letterSpacing = (Dimens2.Label.value * 0.06f).sp,
            color = SemanticColors.TextSecondary,
            modifier = Modifier.weight(1f),
        )
        trailing?.invoke()
    }
}

/** .cell 分隔线：0.5px，左起 16 */
@Composable
fun CardDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        thickness = Dimens.Hairline,
        color = LocalAppColors.current.separator,
        modifier = modifier.padding(start = Dimens.CardPad),
    )
}

/**
 * .cell：min-height 52，padding 8 16，gap 12。
 * title 17 / sub 13-secondary / value 17。
 */
@Composable
fun Cell(
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
    title: String? = null,
    titleColor: Color? = null,
    subtitle: String? = null,
    value: String? = null,
    valueColor: Color? = null,
    valueWeight: FontWeight = FontWeight.Normal,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    content: (@Composable RowScope.() -> Unit)? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = Dimens.RowH)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = Dimens.CardPad, vertical = Dimens.CardPadV),
    ) {
        leading?.invoke()
        if (title != null) {
            Column(
                Modifier
                    .weight(1f)
                    .padding(start = if (leading != null) Dimens.CellGap else 0.dp)
            ) {
                Text(
                    title,
                    fontSize = Dimens2.Body,
                    color = titleColor ?: SemanticColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (subtitle != null) {
                    Text(
                        subtitle,
                        fontSize = Dimens2.Caption,
                        color = SemanticColors.TextSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 1.dp),
                    )
                }
            }
        }
        content?.invoke(this)
        if (value != null) {
            Text(
                value,
                fontSize = Dimens2.Body,
                color = valueColor ?: SemanticColors.TextSecondary,
                fontWeight = valueWeight,
                maxLines = 1,
                modifier = Modifier.padding(start = if (title != null) 8.dp else 0.dp),
            )
        }
        trailing?.invoke()
    }
}

/** .cell-icon：30x30，圆角 8 */
@Composable
fun IconTile(
    icon: ImageVector,
    bg: Color,
    tint: Color,
    size: Dp = Dimens.IconTile,
    iconSize: Dp = 18.dp,
) {
    Box(
        Modifier.size(size).clip(RoundedCornerShape(Dimens.IconR)).background(bg),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(iconSize))
    }
}

/** .stat：bg-elevated + r12 + padding 12 6；.v 19/700；.l 11/secondary/mt2 */
@Composable
fun StatCard(value: String, label: String, modifier: Modifier = Modifier, accent: Color? = null) {
    Surface(
        shape = RoundedCornerShape(Dimens.CardR),
        color = LocalAppColors.current.bgElevated,
        modifier = modifier,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 12.dp),
        ) {
            Text(
                value,
                fontSize = Dimens2.StatValue,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                color = accent ?: SemanticColors.TextPrimary,
            )
            Text(
                label,
                fontSize = Dimens2.StatLabel,
                color = SemanticColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

/** .stat-grid：4 列，gap 8，margin 10 0 4 */
@Composable
fun StatGrid(cards: List<Pair<String, String>>, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().padding(top = 10.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        cards.forEach { (v, l) ->
            StatCard(v, l, Modifier.weight(1f))
        }
    }
}

/** .empty：padding 44 20，15/secondary；图标 40、opacity .4、mb 10 */
@Composable
fun EmptyState(
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier,
    hint: String? = null,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 44.dp),
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = SemanticColors.TextSecondary,
            modifier = Modifier.size(40.dp).alpha(0.4f),
        )
        Text(
            text,
            fontSize = Dimens2.Empty,
            color = SemanticColors.TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 10.dp),
        )
        if (hint != null) {
            Text(
                hint,
                fontSize = Dimens2.Hint,
                color = SemanticColors.TextTertiary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
        if (actionText != null && onAction != null) {
            Button(
                onClick = onAction,
                shape = RoundedCornerShape(Dimens.BtnR),
                colors = ButtonDefaults.buttonColors(containerColor = SemanticColors.Primary),
                modifier = Modifier.padding(top = 16.dp).height(42.dp),
            ) {
                Text(actionText, fontSize = Dimens2.Body, color = Color.White)
            }
        }
    }
}

/** .bar-row：min-height 44，padding 4 16，gap 10；label 76/15；track 6/r3；val 92/12 */
@Composable
fun BarRow(
    label: String,
    valueText: String,
    percent: Float,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 44.dp)
            .padding(horizontal = Dimens.CardPad, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            label,
            fontSize = Dimens2.BarLabel,
            color = SemanticColors.TextPrimary,
            maxLines = 1,
            modifier = Modifier.width(76.dp),
        )
        Box(
            Modifier
                .weight(1f)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(LocalAppColors.current.fill),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(percent.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(3.dp))
                    .background(color),
            )
        }
        Text(
            valueText,
            fontSize = Dimens2.BarValue,
            color = SemanticColors.TextSecondary,
            textAlign = TextAlign.End,
            maxLines = 1,
            modifier = Modifier.width(92.dp),
        )
    }
}

/** .stepper：按钮 34x32 fill 底、首尾 r8；数值 17/600、min-width 28 */
@Composable
fun Stepper(
    value: Int,
    onChange: (Int) -> Unit,
    min: Int = 0,
    max: Int = 20,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        // 减号：仅左侧圆角（.stepper button:first-child）
        StepperButton(AppIcons.Remove, value > min, StepperShapeLeft) { onChange((value - 1).coerceAtLeast(min)) }
        Text(
            "$value",
            fontSize = Dimens2.StepperVal,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            color = SemanticColors.TextPrimary,
            modifier = Modifier.widthIn(min = 28.dp).padding(horizontal = 2.dp),
        )
        // 加号：仅右侧圆角（.stepper button:last-child）
        StepperButton(AppIcons.Add, value < max, StepperShapeRight) { onChange((value + 1).coerceAtMost(max)) }
    }
}

private val StepperShapeLeft = RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp)
private val StepperShapeRight = RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp)

@Composable
private fun StepperButton(
    icon: ImageVector,
    enabled: Boolean,
    shape: androidx.compose.ui.graphics.Shape,
    onClick: () -> Unit,
) {
    Surface(
        shape = shape,
        color = LocalAppColors.current.fill,
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.width(34.dp).height(32.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (enabled) SemanticColors.Primary else SemanticColors.TextTertiary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

/** .toggle：51x31，轨道 r16，滑块 27，开=green */
@Composable
fun AppToggle(checked: Boolean, onChange: (Boolean) -> Unit) {
    Box(
        Modifier
            .size(width = 51.dp, height = 31.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (checked) LocalAppColors.current.green else LocalAppColors.current.fill)
            .clickable { onChange(!checked) },
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .padding(horizontal = 2.dp)
                .size(27.dp)
                .clip(CircleShape)
                .background(Color.White),
        )
    }
}

/** .seg：fill 底、r9、padding 2；按钮 13/500、r7，选中 bg-elevated */
@Composable
fun Segmented(
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 4.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(LocalAppColors.current.fill)
            .padding(2.dp),
    ) {
        options.forEachIndexed { i, label ->
            val on = i == selected
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(7.dp))
                    .then(if (on) Modifier.background(LocalAppColors.current.bgElevated) else Modifier)
                    .clickable { onSelect(i) }
                    .padding(vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    fontSize = Dimens2.Label,
                    fontWeight = if (on) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (on) SemanticColors.TextPrimary else SemanticColors.TextSecondary,
                )
            }
        }
    }
}

/** 主按钮 */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(48.dp),
        enabled = enabled,
        shape = RoundedCornerShape(Dimens.BtnR),
        colors = ButtonDefaults.buttonColors(
            containerColor = SemanticColors.Primary,
            contentColor = Color.White,
        ),
    ) {
        Text(text, fontSize = Dimens2.Body, fontWeight = FontWeight.SemiBold, color = Color.White)
    }
}

/** yyyy-MM-dd 解析，失败回落到今天 */
fun parseDateOrToday(ds: String): java.util.Date = try {
    SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(ds)!!
} catch (e: Exception) { Date() }

/* ==================================================================
   交互修饰符（对齐 web/style.css 的 .press:active 与 listIn）
   ================================================================== */

/**
 * .press：按压时整体缩到 0.97。
 * 用法：Modifier.pressable { onClick() }
 */
@Composable
fun Modifier.pressable(enabled: Boolean = true, onClick: () -> Unit): Modifier {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.97f else 1f,
        animationSpec = tween(100),
        label = "pressScale",
    )
    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interaction,
            indication = null,
            enabled = enabled,
            onClick = onClick,
        )
}

/**
 * listIn：列表项进入时从下方 10px 淡入（.28s，最多错峰 8 项）。
 */
@Composable
fun Modifier.listItemIn(index: Int = 0): Modifier {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val delay = (index.coerceAtMost(8)) * 20
    val alpha by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = tween(280, delayMillis = delay),
        label = "listAlpha",
    )
    val dy by animateDpAsState(
        targetValue = if (shown) 0.dp else 10.dp,
        animationSpec = tween(280, delayMillis = delay),
        label = "listDy",
    )
    return this.alpha(alpha).offset(y = dy)
}
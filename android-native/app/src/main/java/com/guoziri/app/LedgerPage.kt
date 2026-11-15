package com.guoziri.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.guoziri.app.ui.theme.LocalAppColors
import java.util.Calendar
import kotlin.math.roundToInt

/**
 * 记账页，结构对齐 web 版 renderLedger：
 * nav → stat-grid(4) → seg(今天/日历/全部) → 对应内容
 */
@Composable
fun LedgerPage(
    repo: StoreRepo,
    onEdit: (Purchase) -> Unit,
    onRequestDelete: (Purchase) -> Unit,
    onAdd: () -> Unit = {},
) {
    val t = today()
    var tab by remember { mutableIntStateOf(0) }   // 0 今天 / 1 日历 / 2 全部

    val cal = remember { Calendar.getInstance() }
    var calYear by remember { mutableIntStateOf(cal.get(Calendar.YEAR)) }
    var calMonth by remember { mutableIntStateOf(cal.get(Calendar.MONTH) + 1) }
    var calSelected by remember { mutableStateOf(t) }

    val purchases = repo.purchases
    val sortOrder = repo.settings.sortOrder

    val todayList = purchases.filter { it.date == t }
    val todaySum = todayList.sumOf { it.totalCost ?: 0.0 }
    val monthPrefix = t.substring(0, 7)
    val monthSum = purchases.filter { it.date.startsWith(monthPrefix) }.sumOf { it.totalCost ?: 0.0 }

    // 日期分组：新→旧（asc 时相反）
    val groups = remember(purchases.size, sortOrder) {
        val m = purchases.groupBy { it.date }
        val keys = if (sortOrder == "asc") m.keys.sorted() else m.keys.sortedDescending()
        keys.associateWith { d -> m[d]!!.sortedBy { it.createdAt } }
    }
    val recorded = remember(purchases.size) { purchases.map { it.date }.toSet() }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
    ) {
        PageTitle("记账")

        // stat-grid：4 项
        Row(
            Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StatCard("${todayList.size}", "今日笔数", Modifier.weight(1f))
            StatCard(money(todaySum), "今日花销", Modifier.weight(1f))
            StatCard(money(monthSum), "本月累计", Modifier.weight(1f))
            StatCard("${groups.size}", "记账天数", Modifier.weight(1f))
        }

        // seg：今天 / 日历 / 全部
        Segmented(
            options = listOf("今天", "日历", "全部"),
            selected = tab,
            onSelect = { tab = it },
        )

        when (tab) {
            0 -> {
                if (todayList.isEmpty()) {
                    EmptyState(
                        AppIcons.Other,
                        "今天还没有记录",
                        hint = "点右下角 ＋ 记一笔",
                        actionText = "去记账",
                        onAction = onAdd,
                    )
                } else {
                    DayGroup(
                        date = t,
                        items = todayList,
                        onEdit = onEdit,
                        onRequestDelete = onRequestDelete,
                    )
                }
            }

            1 -> {
                CalendarCard(
                    year = calYear,
                    month = calMonth,
                    selected = calSelected,
                    recorded = recorded,
                    onPick = { calSelected = it },
                    onMonthChange = { delta ->
                        var m = calMonth + delta
                        var y = calYear
                        if (m < 1) { m = 12; y -= 1 }
                        if (m > 12) { m = 1; y += 1 }
                        calMonth = m
                        calYear = y
                    },
                )
                val selList = groups[calSelected].orEmpty()
                if (selList.isEmpty()) {
                    EmptyState(
                        AppIcons.Ledger,
                        "${fmtDay(calSelected)} 没有记录",
                        hint = "点上面的日期换一天",
                    )
                } else {
                    DayGroup(
                        date = calSelected,
                        items = selList,
                        onEdit = onEdit,
                        onRequestDelete = onRequestDelete,
                    )
                }
            }

            else -> {
                if (groups.isEmpty()) {
                    EmptyState(
                        AppIcons.Other,
                        "还没有记录",
                        hint = "点右下角 ＋ 记第一笔",
                        actionText = "去记账",
                        onAction = onAdd,
                    )
                } else {
                    groups.forEach { (date, list) ->
                        DayGroup(
                            date = date,
                            items = list,
                            onEdit = onEdit,
                            onRequestDelete = onRequestDelete,
                        )
                    }
                }
            }
        }
    }
}

/** .group-title（margin-top 22）+ 合计行 + 每条记录 */
@Composable
private fun DayGroup(
    date: String,
    items: List<Purchase>,
    onEdit: (Purchase) -> Unit,
    onRequestDelete: (Purchase) -> Unit,
) {
    val c = LocalAppColors.current
    val t = today()
    val sum = items.sumOf { it.totalCost ?: 0.0 }

    GroupTitle(
        text = (if (date == t) "今天 · " else "") + fmtDay(date) + " " + weekDay(date),
        topSpacing = 22.dp,
    )
    GroupCard {
        // 合计行：min-height 40
        Row(
            Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 40.dp)
                .padding(horizontal = Dimens.CardPad, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "${items.size} 笔",
                fontSize = Dimens2.Caption,
                color = c.secondary,
                modifier = Modifier.weight(1f),
            )
            Text(
                money(sum),
                fontSize = Dimens2.Body,
                fontWeight = FontWeight.SemiBold,
                style = NumberStyle,
                color = c.label,
            )
        }
        items.forEachIndexed { idx, p ->
            PurchaseRow(p, index = idx, onEdit = { onEdit(p) }, onDelete = { onRequestDelete(p) })
        }
    }
}

/** 单条记录：左滑露出删除 + 右侧编辑 */
@Composable
fun PurchaseRow(p: Purchase, index: Int = 0, onEdit: () -> Unit, onDelete: () -> Unit) {
    SwipeReveal(onDelete = onDelete) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .listItemIn(index)
                .clickable { onEdit() }
                .padding(horizontal = Dimens.CardPad, vertical = 8.dp)
                .defaultMinSize(minHeight = Dimens.RowH),
        ) {
            IconTile(
                icon = categoryIcon(p.category),
                bg = categoryTint(p.category),
                tint = categoryIconTint(p.category),
                size = Dimens.IconTile,
                iconSize = 17.dp,
            )
            Column(
                Modifier
                    .weight(1f)
                    .padding(start = Dimens.CellGap)
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        p.name,
                        fontSize = Dimens2.Body,
                        color = LocalAppColors.current.label,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (!p.note.isNullOrBlank()) {
                        Text(
                            p.note!!,
                            fontSize = Dimens2.Caption,
                            color = LocalAppColors.current.secondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(start = 6.dp),
                        )
                    }
                }
                val parts = mutableListOf<String>()
                val tm = timeOf(p.createdAt)
                if (tm.isNotBlank()) parts.add(tm)
                if (p.pricePerJin != null) parts.add(money(p.pricePerJin) + "/斤")
                val wTxt = weightText(p)
                if (wTxt.isNotBlank()) parts.add(wTxt)
                if (parts.isNotEmpty()) {
                    Text(
                        parts.joinToString(" · "),
                        fontSize = Dimens2.Caption,
                        color = LocalAppColors.current.secondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 1.dp),
                    )
                }
            }
            Text(
                p.totalCost?.let { money(it) } ?: "—",
                fontSize = Dimens2.Body,
                fontWeight = FontWeight.SemiBold,
                style = NumberStyle,
                color = LocalAppColors.current.label,
                maxLines = 1,
                modifier = Modifier.padding(start = 8.dp),
            )
            Text(
                "编辑",
                fontSize = Dimens2.Action,
                color = LocalAppColors.current.tint,
                modifier = Modifier
                    .padding(start = 2.dp, end = 2.dp)
                    .clickable { onEdit() }
                    .padding(horizontal = 8.dp, vertical = 6.dp),
            )
        }
    }
}

/** 从 ISO 时间取 HH:mm */
fun timeOf(createdAt: String?): String {
    if (createdAt.isNullOrBlank()) return ""
    val i = createdAt.indexOf('T')
    return if (i > 0 && createdAt.length >= i + 6) createdAt.substring(i + 1, i + 6) else ""
}

/**
 * 左滑露出 72dp 红色「删除」，行为对齐 .swipe-wrap。
 */
@Composable
fun SwipeReveal(onDelete: () -> Unit, content: @Composable () -> Unit) {
    val c = LocalAppColors.current
    val reveal = with(LocalDensity.current) { 72.dp.toPx() }
    var offset by remember { mutableFloatStateOf(0f) }

    Box(Modifier.fillMaxWidth()) {
        // 底层删除按钮
        Row(
            Modifier.matchParentSize().background(c.red),
            horizontalArrangement = Arrangement.End,
        ) {
            Box(
                Modifier
                    .width(72.dp)
                    .fillMaxHeight()
                    .clickable { offset = 0f; onDelete() },
                contentAlignment = Alignment.Center,
            ) {
                Text("删除", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            }
        }
        // 上层内容
        Box(
            Modifier
                .offset { IntOffset(offset.roundToInt(), 0) }
                .background(c.bgElevated)
                .pointerInput(reveal) {
                    detectHorizontalDragGestures(
                        onDragEnd = { offset = if (offset < -reveal / 2) -reveal else 0f },
                        onDragCancel = { offset = 0f },
                    ) { change, drag ->
                        change.consume()
                        offset = (offset + drag).coerceIn(-reveal, 0f)
                    }
                },
        ) {
            content()
        }
    }
}

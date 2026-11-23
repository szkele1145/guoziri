package com.guoziri.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.guoziri.app.ui.theme.LocalAppColors
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * 营养页，结构对齐 web 版 renderNutrition：
 * nav → 查看日期 → 总评(ring 96) → 近7天走势(折线) → 各项达标(bar-row) → 未计入营养 → 家里几口人
 */
@Composable
fun NutritionPage(repo: StoreRepo, date: String) {
    var pickedDate by remember { mutableStateOf(date) }
    var showPicker by remember { mutableStateOf(false) }
    val c = LocalAppColors.current
    val customFoods = repo.settings.customFoods
    val fam = repo.settings.family

    val result = remember(pickedDate, repo.purchases.size, fam, customFoods.size) {
        Nutrition.assess(repo.purchases.filter { it.date == pickedDate }, fam, customFoods)
    }
    val a = result.assessment
    val overall = if (a.isNotEmpty()) (a.sumOf { minOf(it.percent, 100) } / a.size).toInt() else 0
    val ringColor = when {
        overall >= 90 -> c.green
        overall >= 60 -> c.orange
        else -> c.red
    }
    val counted = result.details.size
    val t = today()
    val isToday = pickedDate == t

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
    ) {
        PageTitle(
            "营养",
            subtitle = (if (isToday) "今天" else fmtDay(pickedDate)) + " · " +
                weekDay(pickedDate) + " · 《中国居民膳食指南 2022》",
        )

        // 查看日期
        GroupCard(Modifier.padding(top = Dimens.GroupGapTop)) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = Dimens.RowH)
                    .clickable { showPicker = true }
                    .padding(horizontal = Dimens.CardPad, vertical = Dimens.CardPadV),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("查看日期", fontSize = Dimens2.Body, color = c.label, modifier = Modifier.weight(1f))
                // input[type="date"]：fill 底、r10、padding 9 12、15px
                Box(
                    Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(c.fill)
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                ) {
                    Text(pickedDate, fontSize = 15.sp, color = c.label)
                }
            }
        }

        // 总评
        GroupTitle("总评")
        GroupCard {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // .ring 96x96 / r42 / stroke 8
                Box(Modifier.size(96.dp), contentAlignment = Alignment.Center) {
                    Canvas(Modifier.fillMaxSize()) {
                        val stroke = 8.dp.toPx()
                        val r = 42.dp.toPx()
                        val center = Offset(size.width / 2, size.height / 2)
                        val topLeft = Offset(center.x - r, center.y - r)
                        val arcSize = Size(r * 2, r * 2)
                        drawArc(
                            color = c.fill,
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = stroke),
                        )
                        if (overall > 0) {
                            drawArc(
                                color = ringColor,
                                startAngle = -90f,
                                sweepAngle = 360f * minOf(overall, 100) / 100f,
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(width = stroke, cap = StrokeCap.Round),
                            )
                        }
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "$overall%",
                            fontSize = Dimens2.RingPct,
                            fontWeight = FontWeight.Bold,
                            style = NumberStyle,
                            color = c.label,
                        )
                        Text("综合达标", fontSize = Dimens2.RingCap, color = c.secondary)
                    }
                }
                // .ring-info
                Column(Modifier.padding(start = 18.dp)) {
                    Text(
                        if (counted > 0) "按 $counted 种食材计算" else "当天没有可计算的食材",
                        fontSize = Dimens2.RingBig,
                        fontWeight = FontWeight.SemiBold,
                        color = c.label,
                    )
                    val calA = a.firstOrNull { it.key == "calories" }?.actual ?: 0.0
                    val calT = a.firstOrNull { it.key == "calories" }?.target ?: 0.0
                    val proA = a.firstOrNull { it.key == "protein" }?.actual ?: 0.0
                    val proT = a.firstOrNull { it.key == "protein" }?.target ?: 0.0
                    Text(
                        "热量 " + Math.round(calA) + " / " + Math.round(calT) + " kcal\n" +
                            "蛋白质 " + String.format(Locale.US, "%.1f", proA) + " / " + proT.toInt() + " g",
                        fontSize = Dimens2.RingSub,
                        color = c.secondary,
                        lineHeight = 20.sp,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
        }

        // 近 7 天走势
        TrendChart(repo, pickedDate, customFoods)

        if (counted > 0 && a.isNotEmpty()) {
            GroupTitle("各项达标")
            GroupCard {
                a.forEachIndexed { i, x ->
                    if (i > 0) CardDivider()
                    val cls = when {
                        x.percent >= 100 -> c.green
                        x.percent >= 70 -> c.yellow
                        else -> c.red
                    }
                    val valTxt = fmtActual(x.actual, x.unit) + "/" + x.target.toInt() + x.unit + " · " + x.percent + "%"
                    BarRow(
                        label = x.label,
                        valueText = valTxt,
                        percent = minOf(x.percent, 130) / 130f,
                        color = cls,
                    )
                }
            }
            Text(
                "绿 ≥100% 达标 · 黄 70-99% 接近 · 红 <70% 不足 · 橙 >130% 偏多",
                fontSize = Dimens2.Hint,
                color = c.tertiary,
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            )

            // 未计入营养
            if (result.unmatched.isNotEmpty()) {
                GroupTitle("未计入营养")
                GroupCard {
                    result.unmatched.forEachIndexed { i, pair ->
                        if (i > 0) CardDivider()
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 60.dp)
                                .padding(horizontal = Dimens.CardPad, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                Modifier
                                    .size(Dimens.IconTile)
                                    .background(c.fill, RoundedCornerShape(Dimens.IconR)),
                                contentAlignment = Alignment.Center,
                            ) {
                                AppIcons.Draw(
                                    AppIcons.Help,
                                    size = 16.dp,
                                    contentDescription = null,
                                    tint = c.tertiary,
                                )
                            }
                            Column(Modifier.weight(1f).padding(start = Dimens.CellGap)) {
                                Text(pair.first, fontSize = 15.sp, color = c.label)
                                Text(pair.second, fontSize = Dimens2.Caption, color = c.secondary)
                            }
                        }
                    }
                }
                Text(
                    "这些记录不会影响营养计算",
                    fontSize = Dimens2.Hint,
                    color = c.tertiary,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                )
            }
        } else {
            val dayPurchases = repo.purchases.filter { it.date == pickedDate }
            if (dayPurchases.isNotEmpty()) {
                GroupTitle("今天记了这些（营养未计入）")
                GroupCard {
                    dayPurchases.forEachIndexed { i, p ->
                        if (i > 0) CardDivider()
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 60.dp)
                                .padding(horizontal = Dimens.CardPad, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                Modifier
                                    .size(Dimens.IconTile)
                                    .background(c.fill, RoundedCornerShape(Dimens.IconR)),
                                contentAlignment = Alignment.Center,
                            ) {
                                AppIcons.Draw(
                                    AppIcons.Help,
                                    size = 16.dp,
                                    contentDescription = null,
                                    tint = c.tertiary,
                                )
                            }
                            Column(Modifier.weight(1f).padding(start = Dimens.CellGap)) {
                                Text(p.name, fontSize = 15.sp, color = c.label)
                                Text("不在食材库或未填重量", fontSize = Dimens2.Caption, color = c.secondary)
                            }
                        }
                    }
                }
                Text(
                    "填上单价+总价或重量，即可自动算营养",
                    fontSize = Dimens2.Hint,
                    color = c.tertiary,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                )
            } else {
                EmptyState(
                    AppIcons.Restaurant,
                    if (isToday) "去记账页添加食材" else "这一天没有记账",
                    hint = "这里会自动算营养",
                )
            }
        }

        // 家里几口人
        GroupTitle("家里几口人")
        GroupCard {
            FamilyRow("大人", fam.adults) { v ->
                repo.updateSettings { it.copy(family = it.family.copy(adults = v)) }
            }
            CardDivider()
            FamilyRow("小孩", fam.children) { v ->
                repo.updateSettings { it.copy(family = it.family.copy(children = v)) }
            }
        }
    }

    if (showPicker) {
        Dialog(onDismissRequest = { showPicker = false }) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = c.bg,
                modifier = Modifier.fillMaxWidth(),
            ) {
                val cal = remember { Calendar.getInstance() }
                var y by remember { mutableIntStateOf(cal.get(Calendar.YEAR)) }
                var m by remember { mutableIntStateOf(cal.get(Calendar.MONTH) + 1) }
                CalendarCard(
                    year = y,
                    month = m,
                    selected = pickedDate,
                    recorded = repo.purchases.map { it.date }.toSet(),
                    onPick = { pickedDate = it; showPicker = false },
                    onMonthChange = { delta ->
                        var mm = m + delta
                        var yy = y
                        if (mm < 1) { mm = 12; yy -= 1 }
                        if (mm > 12) { mm = 1; yy += 1 }
                        m = mm; y = yy
                    },
                )
            }
        }
    }
}

/** 实际值格式：kcal 取整，其余 1 位小数 */
private fun fmtActual(v: Double, unit: String): String =
    if (unit == "kcal") Math.round(v).toString() else String.format(Locale.US, "%.1f", v)

@Composable
private fun FamilyRow(label: String, value: Int, onChange: (Int) -> Unit) {
    val c = LocalAppColors.current
    Row(
        Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = Dimens.RowH)
            .padding(horizontal = Dimens.CardPad, vertical = Dimens.CardPadV),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, fontSize = Dimens2.Body, color = c.label, modifier = Modifier.weight(1f))
        Stepper(value = value, onChange = onChange)
    }
}

/**
 * 近 7 天走势：折线 + 面积渐变 + 圆点 + 100% 虚线（对齐 web 版 renderTrend，viewBox 320x120，P=10）
 */
@Composable
fun TrendChart(repo: StoreRepo, endDate: String, customFoods: List<CustomFood>) {
    val c = LocalAppColors.current
    val fam = repo.settings.family
    val days = remember(endDate, repo.purchases.size, customFoods.size) {
        val base = parseDateOrToday(endDate)
        (0..6).map { offset ->
            val d = Calendar.getInstance().apply {
                time = base
                add(Calendar.DAY_OF_YEAR, offset - 6)
            }
            val ds = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(d.time)
            val r = Nutrition.assess(repo.purchases.filter { it.date == ds }, fam, customFoods)
            val list = r.assessment
            val ov = if (list.isNotEmpty()) list.sumOf { minOf(it.percent, 100) } / list.size else null
            ds to ov
        }
    }
    if (days.all { it.second == null }) return

    val tm = rememberTextMeasurer()
    val labelStyle = TextStyle(fontSize = 9.sp, color = c.secondary)
    val dateStyle = TextStyle(fontSize = 9.sp, color = c.tertiary)
    val dateTodayStyle = TextStyle(fontSize = 9.sp, color = c.tint)
    val todayStr = today()

    GroupTitle("近 7 天走势")
    GroupCard {
        Column(Modifier.fillMaxWidth().padding(start = 8.dp, top = 14.dp, end = 8.dp, bottom = 6.dp)) {
            Canvas(Modifier.fillMaxWidth().height(120.dp)) {
                val w = size.width
                val h = size.height
                val p = 10.dp.toPx()
                val stepX = (w - 2 * p) / 6f

                // 100% 虚线（y = p）
                var dashX = p
                while (dashX < w - p) {
                    drawLine(
                        color = c.separator,
                        start = Offset(dashX, p),
                        end = Offset(minOf(dashX + 3.dp.toPx(), w - p), p),
                        strokeWidth = 1.dp.toPx(),
                    )
                    dashX += 6.dp.toPx()
                }

                data class Pt(val x: Float, val y: Float, val v: Int)
                // 关键：x 必须按原始索引算，缺数据的日子不能压缩，否则和日期标签错位
                val pts = days.mapIndexedNotNull { i, item ->
                    val ov = item.second
                    if (ov == null) null
                    else Pt(
                        x = p + i * stepX,
                        y = h - p - (ov / 100f * (h - 2 * p)),
                        v = ov.toInt(),
                    )
                }
                if (pts.isNotEmpty()) {
                    if (pts.size >= 2) {
                        val areaPath = Path().apply {
                            moveTo(pts[0].x, h - p)
                            pts.forEach { pt -> lineTo(pt.x, pt.y) }
                            lineTo(pts[pts.size - 1].x, h - p)
                            close()
                        }
                        drawPath(
                            path = areaPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(c.tint.copy(alpha = 0.18f), c.tint.copy(alpha = 0f)),
                                startY = p,
                                endY = h - p,
                            ),
                        )
                        val linePath = Path().apply {
                            moveTo(pts[0].x, pts[0].y)
                            for (i in 1 until pts.size) lineTo(pts[i].x, pts[i].y)
                        }
                        drawPath(
                            path = linePath,
                            color = c.tint,
                            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
                        )
                    }

                    pts.forEach { pt ->
                        drawCircle(color = c.tint, radius = 3.dp.toPx(), center = Offset(pt.x, pt.y))
                        // 数值标签画在点下方，避免压住折线和 100% 虚线
                        val layout = tm.measure(pt.v.toString(), labelStyle)
                        val labelX = (pt.x - layout.size.width / 2f)
                            .coerceIn(0f, (w - layout.size.width).coerceAtLeast(0f))
                        drawText(
                            textLayoutResult = layout,
                            topLeft = Offset(labelX, pt.y + 6.dp.toPx()),
                        )
                    }
                }

                // 日期标签
                days.forEachIndexed { i, item ->
                    val ds = item.first
                    val txt = if (ds == todayStr) "今天" else daysShort(ds)
                    val layout = tm.measure(txt, if (ds == todayStr) dateTodayStyle else dateStyle)
                    val labelX = (p + i * stepX - layout.size.width / 2f)
                        .coerceIn(0f, (w - layout.size.width).coerceAtLeast(0f))
                    drawText(
                        textLayoutResult = layout,
                        topLeft = Offset(labelX, h - layout.size.height),
                    )
                }
            }
            Text(
                "综合达标率 % · 虚线 = 100% 达标线",
                fontSize = Dimens2.Hint,
                color = c.tertiary,
                modifier = Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 6.dp),
            )
        }
    }
}

private fun daysShort(ds: String): String = try {
    val parts = ds.split("-")
    parts[1].toInt().toString() + "/" + parts[2].toInt()
} catch (e: Exception) { ds }

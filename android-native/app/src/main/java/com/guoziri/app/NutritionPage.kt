package com.guoziri.app

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun NutritionPage(repo: StoreRepo, date: String) {
    var pickedDate by remember { mutableStateOf(date) }
    var showDatePicker by remember { mutableStateOf(false) }
    val customFoods = repo.settings.customFoods

    val result = remember(pickedDate, repo.purchases.size, repo.settings.family, customFoods.size) {
        Nutrition.assess(repo.purchases.filter { it.date == pickedDate }, repo.settings.family, customFoods)
    }
    val overall = if (result.assessment.isNotEmpty())
        result.assessment.sumOf { minOf(it.percent, 100) } / result.assessment.size else 0
    val ringColor = when {
        overall >= 90 -> IosGreen
        overall >= 60 -> IosOrange
        else -> IosRed
    }
    val t = today()

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 40.dp)) {
        // 标题 + 日期切换
        Row(
            Modifier.fillMaxWidth().padding(start = Dimens.PageH, top = 8.dp, end = Dimens.PageH, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "营养",
                    fontSize = Dimens2.Title,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp,
                )
                Text(
                    "${if (pickedDate == t) "今天" else fmtDay(pickedDate)} · ${weekDay(pickedDate)} · 膳食指南 2022",
                    fontSize = Dimens2.Caption,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                onClick = { showDatePicker = true },
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                ) {
                    Icon(
                        Icons.Rounded.DateRange,
                        contentDescription = null,
                        tint = IosBlue,
                        modifier = Modifier.size(15.dp),
                    )
                    Spacer(Modifier.width(5.dp))
                    Text("选日期", fontSize = Dimens2.Caption, color = IosBlue)
                }
            }
        }

        // ---- 总评 ----
        GroupTitle("总评")
        GroupCard(Modifier.padding(horizontal = Dimens.PageH)) {
            Row(
                Modifier.fillMaxWidth().padding(Dimens.CardPad),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(92.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = { minOf(overall, 100) / 100f },
                        modifier = Modifier.fillMaxSize(),
                        color = ringColor,
                        trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                        strokeWidth = 9.dp,
                        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round,
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "$overall%",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = ringColor,
                        )
                        Text(
                            "综合达标",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                        )
                    }
                }
                Spacer(Modifier.width(18.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        if (result.items.isNotEmpty()) "按 ${result.items.size} 种食材计算"
                        else "当天没有可计算的食材",
                        fontSize = Dimens2.Body,
                        fontWeight = FontWeight.Medium,
                    )
                    val cal = result.assessment.firstOrNull()
                    val pro = result.assessment.getOrNull(1)
                    Text(
                        "热量 ${cal?.actual?.toInt() ?: 0} / ${cal?.target?.toInt() ?: 0} kcal",
                        fontSize = Dimens2.Caption,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                        modifier = Modifier.padding(top = 6.dp),
                    )
                    Text(
                        "蛋白质 ${pro?.actual?.let { String.format(Locale.US, "%.1f", it) } ?: "0"} / ${pro?.target?.toInt() ?: 0} g",
                        fontSize = Dimens2.Caption,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
        }

        // ---- 各项达标 ----
        if (result.items.isNotEmpty()) {
            GroupTitle("各项达标")
            GroupCard(Modifier.padding(horizontal = Dimens.PageH)) {
                result.assessment.forEachIndexed { i, x ->
                    if (i > 0) CardDivider()
                    val cls = when {
                        x.percent >= 100 -> IosGreen
                        x.percent >= 70 -> IosYellow
                        x.percent > 130 -> IosOrange
                        else -> IosRed
                    }
                    Column(Modifier.padding(horizontal = Dimens.CardPad, vertical = 11.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(x.label, fontSize = Dimens2.Body, modifier = Modifier.weight(1f))
                            Text(
                                "${String.format(Locale.US, "%.0f", x.actual)}/${x.target.toInt()}${x.unit}",
                                fontSize = Dimens2.Caption,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.42f),
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "${x.percent}%",
                                fontSize = Dimens2.Caption,
                                fontWeight = FontWeight.Medium,
                                color = cls,
                                modifier = Modifier.width(42.dp),
                                textAlign = TextAlign.End,
                            )
                        }
                        val frac = animateFloatAsState(
                            if (x.percent > 0) minOf(x.percent, 130) / 130f else 0f,
                            label = "bar",
                        )
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .padding(top = 7.dp)
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)),
                        ) {
                            Box(
                                Modifier
                                    .fillMaxWidth(frac.value)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(cls),
                            )
                        }
                    }
                }
            }
            Text(
                "绿 ≥100% 达标 · 黄 70-99% 接近 · 红 <70% 不足 · 橙 >130% 偏多",
                fontSize = Dimens2.Micro,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.32f),
                modifier = Modifier.padding(start = Dimens.PageH, top = 6.dp, end = Dimens.PageH),
            )
        }

        // ---- 食材明细 ----
        if (result.items.isNotEmpty()) {
            GroupTitle("食材明细")
            GroupCard(Modifier.padding(horizontal = Dimens.PageH)) {
                result.items.forEachIndexed { i, it ->
                    if (i > 0) CardDivider()
                    val tag = when (it.source) {
                        "exact" -> "库内" to IosGreen
                        "alias" -> "别名" to IosGreen
                        "custom" -> "自填" to IosBlue
                        else -> "估算" to IosOrange
                    }
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = Dimens.CardPad, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            it.name,
                            fontSize = Dimens2.Body,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            "${it.weight.toInt()}g",
                            fontSize = Dimens2.Caption,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.42f),
                        )
                        Spacer(Modifier.width(10.dp))
                        Surface(
                            shape = RoundedCornerShape(5.dp),
                            color = tag.second.copy(alpha = 0.13f),
                        ) {
                            Text(
                                tag.first,
                                fontSize = 10.sp,
                                color = tag.second,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                }
            }
            if (result.estimatedCount > 0) {
                Row(
                    Modifier.padding(start = Dimens.PageH, top = 8.dp, end = Dimens.PageH),
                    verticalAlignment = Alignment.Top,
                ) {
                    Icon(
                        Icons.Rounded.Warning,
                        contentDescription = null,
                        tint = IosOrange,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "${result.estimatedCount} 种食材不在库中，按分类估算；可在记账时手动录入精确值",
                        fontSize = Dimens2.Micro,
                        color = IosOrange,
                        lineHeight = 15.sp,
                    )
                }
            }
        }

        if (result.unmatched.isNotEmpty()) {
            GroupTitle("没算上的")
            GroupCard(Modifier.padding(horizontal = Dimens.PageH)) {
                result.unmatched.forEachIndexed { i, (name, reason) ->
                    if (i > 0) CardDivider()
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = Dimens.CardPad, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(name, fontSize = Dimens2.Body, modifier = Modifier.weight(1f))
                        Text(
                            reason,
                            fontSize = Dimens2.Caption,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.42f),
                        )
                    }
                }
            }
        }

        if (result.items.isEmpty() && result.unmatched.isEmpty()) {
            EmptyState(
                Icons.Rounded.List,
                if (pickedDate == t) "去记账页添加食材" else "这一天没有记账",
                hint = if (pickedDate == t) "这里会自动算出全家营养达标率" else null,
            )
        }

        // ---- 7 天趋势 ----
        if (result.items.isNotEmpty()) {
            GroupTitle("近 7 天")
            GroupCard(Modifier.padding(horizontal = Dimens.PageH)) {
                TrendChart(repo, pickedDate, customFoods)
            }
        }

        // ---- 家里几口人 ----
        GroupTitle("家里几口人")
        GroupCard(Modifier.padding(horizontal = Dimens.PageH)) {
            FamilyRow("大人", repo.settings.family.adults) { v ->
                repo.updateSettings { it.copy(family = it.family.copy(adults = v)) }
            }
            FamilyRow("小孩", repo.settings.family.children) { v ->
                repo.updateSettings { it.copy(family = it.family.copy(children = v)) }
            }
        }

        Spacer(Modifier.height(30.dp))
    }

    if (showDatePicker) {
        DatePickerSheet(
            initial = pickedDate,
            recordedDates = repo.purchases.map { it.date }.distinct(),
            onPick = { pickedDate = it; showDatePicker = false },
            onDismiss = { showDatePicker = false },
        )
    }
}

/* ---------------- 7 天趋势（柱状图） ---------------- */
@Composable
fun TrendChart(repo: StoreRepo, endDate: String, customFoods: List<CustomFood>) {
    val fam = repo.settings.family
    val days = remember(endDate, repo.purchases.size, customFoods.size) {
        val base = parseDateOrToday(endDate)
        (6 downTo 0).map { i ->
            val d = Calendar.getInstance().apply {
                time = base
                add(Calendar.DAY_OF_YEAR, -i)
            }
            val ds = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(d.time)
            val r = Nutrition.assess(repo.purchases.filter { it.date == ds }, fam, customFoods)
            val a = r.assessment
            val overall = if (a.isNotEmpty()) a.sumOf { minOf(it.percent, 100) } / a.size else 0
            Triple(ds, overall, r.total["calories"] ?: 0.0)
        }
    }
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom,
    ) {
        days.forEach { (ds, pct, _) ->
            val isSel = ds == endDate
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    if (pct > 0) "$pct" else "—",
                    fontSize = 10.sp,
                    color = if (isSel) IosBlue else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                )
                Spacer(Modifier.height(4.dp))
                Box(
                    Modifier
                        .width(20.dp)
                        .height((pct.coerceIn(0, 100) / 100f * 84f).dp.coerceAtLeast(3.dp))
                        .clip(RoundedCornerShape(5.dp))
                        .background(
                            when {
                                pct >= 100 -> IosGreen
                                pct >= 70 -> IosYellow
                                pct > 0 -> IosOrange
                                else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)
                            },
                        ),
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    if (ds == today()) "今天" else SimpleDateFormat("M/d", Locale.US).format(parseDateOrToday(ds)),
                    fontSize = 10.sp,
                    color = if (isSel) IosBlue else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                )
            }
        }
    }
    val avg = days.map { it.second }.filter { it > 0 }
    Text(
        if (avg.isEmpty()) "近 7 天还没有记录"
        else "平均达标 ${avg.sum() / avg.size}%，最高 ${avg.max()}%，共 ${days.count { it.second > 0 }} 天有记录",
        fontSize = Dimens2.Micro,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
        modifier = Modifier.padding(start = Dimens.CardPad, end = Dimens.CardPad, bottom = 12.dp),
    )
}

fun parseDateOrToday(ds: String): java.util.Date =
    try { SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(ds)!! }
    catch (e: Exception) { java.util.Date() }

@Composable
fun FamilyRow(label: String, value: Int, onChange: (Int) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = Dimens.CardPad, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, fontSize = Dimens2.Body, modifier = Modifier.weight(1f))
        StepperButton(Icons.Rounded.Remove, value > 0) { onChange((value - 1).coerceAtLeast(0)) }
        Text(
            "$value",
            fontSize = Dimens2.Body,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(min = 36.dp),
        )
        StepperButton(Icons.Rounded.Add, value < 20) { onChange((value + 1).coerceAtMost(20)) }
    }
}

@Composable
private fun StepperButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(9.dp),
        color = if (enabled) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f),
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(32.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (enabled) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                modifier = Modifier.size(17.dp),
            )
        }
    }
}

package com.guoziri.app

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun NutritionPage(repo: StoreRepo, date: String, anchors: GuideAnchors) {
    val result = remember(date, pickedDate, repo.purchases.size, repo.settings.family, customFoods.size) {
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

    var pickedDate by remember { mutableStateOf(date) }
    var showDatePicker by remember { mutableStateOf(false) }
    val customFoods = repo.settings.customFoods

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 110.dp)) {
        Text("营养", fontSize = 34.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 20.dp, top = 12.dp, bottom = 6.dp))
        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("${if (pickedDate == t) "今天" else fmtDay(pickedDate)} · ${weekDay(pickedDate)} · 《中国居民膳食指南 2022》",
                fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                modifier = Modifier.weight(1f))
            TextButton(onClick = { showDatePicker = true }) {
                Text("选日期", fontSize = 13.sp, color = IosBlue)
            }
        }

        GroupTitle("总评")
        GroupCard {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(96.dp).guideAnchor(anchors, "nutrition-ring", "nutrition"), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = { minOf(overall, 100) / 100f },
                        modifier = Modifier.fillMaxSize(),
                        color = ringColor,
                        trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                        strokeWidth = 10.dp,
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$overall%", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Text("综合达标", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }
                }
                Column(Modifier.padding(start = 18.dp)) {
                    Text(if (result.details.isNotEmpty()) "按 ${result.details.size} 种食材计算" else "当天没有可计算的食材",
                        fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    val caloriesTarget = result.assessment.firstOrNull()?.target ?: 0.0
                    val calA = result.assessment.firstOrNull()?.actual ?: 0.0
                    val proteinA = result.assessment.getOrNull(1)?.actual ?: 0.0
                    val proteinT = result.assessment.getOrNull(1)?.target ?: 0.0
                    Text("热量 ${calA.toInt()} / ${caloriesTarget.toInt()} kcal\n蛋白质 ${String.format("%.1f", proteinA)} / ${proteinT.toInt()} g",
                        fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f), lineHeight = 20.sp,
                        modifier = Modifier.padding(top = 4.dp))
                }
            }
        }

        if (result.details.isNotEmpty()) {
            GroupTitle("各项达标")
            GroupCard {
                result.assessment.forEach { x ->
                    val cls = when {
                        x.percent >= 100 -> IosGreen
                        x.percent >= 70 -> IosYellow
                        x.percent > 130 -> IosOrange
                        else -> IosRed
                    }
                    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(x.label, fontSize = 15.sp)
                            Text("${String.format("%.1f", x.actual)}/${x.target}${x.unit} · ${x.percent}%",
                                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        }
                        val frac = animateFloatAsState(if (x.percent > 0) minOf(x.percent, 130) / 130f else 0f, label = "bar")
                        Box(Modifier.fillMaxWidth().padding(top = 6.dp).height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))) {
                            Box(Modifier.fillMaxWidth(frac.value).fillMaxHeight().clip(RoundedCornerShape(3.dp)).background(cls))
                        }
                    }
                }
            }
            Text("绿 ≥100% 达标 · 黄 70-99% 接近 · 红 <70% 不足 · 橙 >130% 偏多",
                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp))
        }

        // 食材明细：标出库内 / 自定义 / 估算
        if (result.items.isNotEmpty()) {
            GroupTitle("食材明细")
            GroupCard {
                result.items.forEach { it ->
                    val tag = when (it.source) {
                        "exact" -> "库内" to IosGreen
                        "alias" -> "别名→${it.matchedAs}" to IosGreen
                        "custom" -> "自填" to IosBlue
                        else -> "估算" to IosOrange
                    }
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(it.name, fontSize = 15.sp, modifier = Modifier.weight(1f))
                        Text("${it.weight.toInt()}g", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        Text(tag.first, fontSize = 11.sp, color = tag.second, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
            if (result.estimatedCount > 0) {
                Text("${result.estimatedCount} 种食材不在库中，按分类估算；可在记账时手动录入精确值",
                    fontSize = 12.sp, color = IosOrange,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp))
            }
        }

        if (result.unmatched.isNotEmpty()) {
            GroupTitle("没算上的")
            GroupCard {
                result.unmatched.forEach { (name, reason) ->
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("❔", fontSize = 15.sp)
                        Column(Modifier.padding(start = 12.dp)) {
                            Text(name, fontSize = 15.sp)
                            Text(reason, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        }
                    }
                }
            }
        }

        // 7 天趋势
        GroupTitle("近 7 天")
        GroupCard {
            TrendChart(repo, pickedDate, customFoods)
        }

        if (result.details.isEmpty() && result.unmatched.isEmpty()) {
            EmptyState("🥗", if (date == t) "去记账页添加食材\n这里会自动算营养" else "这一天没有记账")
        }

        GroupTitle("家里几口人")
        GroupCard {
            FamilyRow("大人", repo.settings.family.adults) { v ->
                repo.updateSettings { it.copy(family = it.family.copy(adults = v)) }
            }
            FamilyRow("小孩", repo.settings.family.children) { v ->
                repo.updateSettings { it.copy(family = it.family.copy(children = v)) }
            }
        }
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

/* ---------------- 7 天趋势（简易柱状图） ---------------- */
@Composable
fun TrendChart(repo: StoreRepo, endDate: String, customFoods: List<CustomFood>) {
    val fam = repo.settings.family
    val days = remember(endDate, repo.purchases.size, customFoods.size) {
        val base = parseDate(endDate)
        (6 downTo 0).map { i ->
            val d = java.util.Calendar.getInstance().apply {
                time = base
                add(java.util.Calendar.DAY_OF_YEAR, -i)
            }
            val ds = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(d.time)
            val r = Nutrition.assess(repo.purchases.filter { it.date == ds }, fam, customFoods)
            val a = r.assessment
            val overall = if (a.isNotEmpty()) a.sumOf { minOf(it.percent, 100) } / a.size else 0
            Triple(ds, overall, r.total["calories"] ?: 0.0)
        }
    }
    val maxPct = 100
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom,
    ) {
        days.forEach { (ds, pct, kcal) ->
            val isSel = ds == endDate
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(if (pct > 0) "$pct" else "-", fontSize = 10.sp,
                    color = if (isSel) IosBlue else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f))
                Spacer(Modifier.height(4.dp))
                Box(
                    Modifier.width(22.dp).height((pct.coerceIn(0, maxPct) / 100f * 96f).dp.coerceAtLeast(3.dp))
                        .clip(RoundedCornerShape(5.dp))
                        .background(if (pct >= 100) IosGreen else if (pct >= 70) IosYellow else if (pct > 0) IosOrange else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    if (ds == today()) "今天" else SimpleDateFormat("M/d", Locale.US).format(parseDate(ds)),
                    fontSize = 10.sp,
                    color = if (isSel) IosBlue else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                )
            }
        }
    }
    val avg = days.map { it.second }.filter { it > 0 }
    if (avg.isNotEmpty()) {
        Text("平均达标 ${avg.sum() / avg.size}%，最高 ${avg.max()}%，共 ${days.count { it.second > 0 }} 天有记录",
            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp))
    } else {
        Text("近 7 天还没有记录",
            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp))
    }
}

private fun parseDate(ds: String): java.util.Date =
    try { SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(ds)!! }
    catch (e: Exception) { java.util.Date() }

@Composable
fun FamilyRow(label: String, value: Int, onChange: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 17.sp, modifier = Modifier.weight(1f))
        Button(
            onClick = { onChange((value - 1).coerceAtLeast(0)) },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
        ) { Text("-", fontSize = 18.sp, color = MaterialTheme.colorScheme.primary) }
        Text("$value", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(min = 32.dp))
        Button(
            onClick = { onChange((value + 1).coerceAtMost(20)) },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
        ) { Text("+", fontSize = 18.sp, color = MaterialTheme.colorScheme.primary) }
    }
}

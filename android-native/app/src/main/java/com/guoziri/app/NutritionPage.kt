package com.guoziri.app

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
        overall >= 90 -> SemanticColors.Success
        overall >= 60 -> SemanticColors.Warning
        else -> SemanticColors.Error
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
                    "${if (pickedDate == t) \"今天\" else fmtDay(pickedDate)} \u00b7 ${weekDay(pickedDate)} \u00b7 \u8170\u9910\u6307\u5357 2022",
                    fontSize = 13.sp, color = SemanticColors.TextSecondary,
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
                        tint = SemanticColors.Primary,
                        modifier = Modifier.size(15.dp),
                    )
                    Spacer(Modifier.width(5.dp))
                    Text("选日期", fontSize = Dimens2.Caption, color = SemanticColors.Primary)
                }
            }
        }

        // ---- 总评 ----
        GroupTitle("总评")
        GroupCard(Modifier.padding(horizontal = Dimens.PageH)) {
            Row(Modifier.fillMaxWidth().padding(Dimens.CardPad), verticalAlignment = Alignment.CenterVertically) {
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
                        Text("$overall%", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = ringColor)
                        Text("综合达标", fontSize = 10.sp, color = SemanticColors.TextSecondary)
                    }
                }
                Column(Modifier.padding(start = 18.dp)) {
                    Text(if (result.details.isNotEmpty()) "按 ${result.details.size} 种食材计算" else "当天没有可计算的食材", fontSize = Dimens2.Body, fontWeight = FontWeight.Medium)
                    val cal = result.assessment.firstOrNull()?.target ?: 0.0
                    val calA = result.assessment.firstOrNull()?.actual ?: 0.0
                    val proteinA = result.assessment.getOrNull(1)?.actual ?: 0.0
                    val proteinT = result.assessment.getOrNull(1)?.target ?: 0.0
                    Text("热量 ${calA.toInt()} / ${caloriesTarget.toInt()} kcal\n蛋白质 ${String.format("%.1f", proteinA)} / ${proteinT.toInt()} g", fontSize = 13.sp, color = SemanticColors.TextSecondary, lineHeight = 20.sp, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }

        // 各项达标
        if (result.details.isNotEmpty()) {
            GroupTitle("各项达标")
            GroupCard(Modifier.padding(horizontal = Dimens.PageH)) {
                result.assessment.forEach { x ->
                    val cls = when {
                        x.percent >= 100 -> SemanticColors.Success
                        x.percent >= 70 -> SemanticColors.Warning
                        x.percent > 130 -> SemanticColors.Warning
                        else -> SemanticColors.Error
                    }
                    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(x.label, fontSize = 15.sp)
                            Text("${String.format("%.0f", x.actual)}/${x.target.toInt()}${x.unit} \u00b7 ${x.percent}%", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            Spacer(Modifier.width(10.dp))
                            Text("${x.percent}%", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = cls, modifier = Modifier.width(42.dp), textAlign = TextAlign.End)
                        }
                        val frac = animateFloatAsState(if (x.percent > 0) minOf(x.percent, 130) / 130f else 0f, label = "bar")
                        Box(Modifier.fillMaxWidth().padding(top = 7.dp).height(5.dp).clip(RoundedCornerShape(3.dp)).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))) {
                            Box(Modifier.fillMaxWidth(frac.value).fillMaxHeight().clip(RoundedCornerShape(3.dp)).background(cls))
                        }
                    }
                }
            }
            Text("绿 \u2265100% \u8fbe\u6807 \u00b7 \u9ec4 70-99% \u63a5\u8fd1 \u00b7 \u7ea2 <70% \u4e0d\u8db3 \u00b7 \u6a59 >130% \u507b\u591a", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f), modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp))
        }

        // 食材明细
        if (result.details.isNotEmpty()) {
            GroupTitle("食材明细")
            GroupCard(Modifier.padding(horizontal = Dimens.PageH)) {
                result.details.forEachIndexed { i, it ->
                    if (i > 0) CardDivider()
                    val tag = when (it.source) {
                        "exact" -> "库内" to SemanticColors.Success
                        "alias" -> "别名" to SemanticColors.Success
                        "custom" -> "自填" to SemanticColors.Primary
                        else -> "估算" to SemanticColors.Warning
                    }
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(it.name, fontSize = 15.sp, modifier = Modifier.weight(1f))
                        Text("${it.weight.toInt()}g", fontSize = 12.sp, color = SemanticColors.TextSecondary)
                        Spacer(Modifier.width(10.dp))
                        Surface(shape = RoundedCornerShape(3.dp), color = tag.second.copy(alpha = 0.13f)) { Text(tag.first, fontSize = 10.sp, color = tag.second, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)) }
                    }
                }
            }
            if (result.estimatedCount > 0) {
                Row(Modifier.padding(start = 20.dp, top = 8.dp, end = 20.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Rounded.Warning, contentDescription = null, tint = SemanticColors.Warning, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("${result.estimatedCount} 种食材不在库中，按分类估算；可在记账时手动录入精确值", fontSize = 12.sp, color = SemanticColors.Warning, lineHeight = 15.sp)
                }
            }
        }

        if (result.unmatched.isNotEmpty()) {
            GroupTitle("没算上的")
            GroupCard(Modifier.padding(horizontal = Dimens.PageH)) {
                result.unmatched.forEachIndexed { i, (name, reason) ->
                    if (i > 0) CardDivider()
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(name, fontSize = 15.sp, modifier = Modifier.weight(1f))
                        Text(reason, fontSize = 12.sp, color = SemanticColors.TextSecondary)
                    }
                }
            }
        }

        if (result.details.isEmpty() && result.unmatched.isEmpty()) {
            EmptyState(Icons.Rounded.List, if (pickedDate == today()) "去记账页添加食材" else "这一天没有记账", hint = if (pickedDate == today()) "这里会自动算出全家营养达标率" else null)
        }

        // 7 天趋势
        if (result.details.isNotEmpty()) {
            GroupTitle("近 7 天")
            GroupCard(Modifier.padding(horizontal = Dimens.PageH)) {
                TrendChart(repo, pickedDate, customFoods)
            }
        }

        // 家里几口人
        GroupTitle("家里几口人")
        GroupCard(Modifier.padding(horizontal = Dimens.PageH)) {
            FamilyRow("大人", repo.settings.family.adults) { v -> repo.updateSettings { it.copy(family = it.family.copy(adults = v)) } }
            FamilyRow("小孩", repo.settings.family.children) { v -> repo.updateSettings { it.copy(family = it.family.copy(children = v)) } }
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
            val d = Calendar.getInstance().apply { time = base; add(Calendar.DAY_OF_YEAR, -i) }
            val ds = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(d.time)
            val r = Nutrition.assess(repo.purchases.filter { it.date == ds }, fam, customFoods)
            val a = r.assessment
            val overall = if (a.isNotEmpty()) a.sumOf { minOf(it.percent, 100) } / a.size else 0
            Triple(ds, overall, r.total["calories"] ?: 0.0)
        }
    }
    Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 16.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Bottom) {
        days.forEach { (ds, pct, _) ->
            val isSel = ds == endDate
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(if (pct > 0) "$pct" else "\u2014", fontSize = 10.sp, color = if (isSel) SemanticColors.Primary else SemanticColors.TextSecondary)
                Spacer(Modifier.height(4.dp))
                Box(Modifier.width(20.dp).height((pct.coerceIn(0, 100) / 100f * 84f).dp.coerceAtLeast(3.dp)).clip(RoundedCornerShape(3.dp)).background(when {
                    pct >= 100 -> SemanticColors.Success
                    pct >= 70 -> SemanticColors.Warning
                    pct > 0 -> SemanticColors.Warning
                    else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                })
                Spacer(Modifier.height(6.dp))
                Text(if (ds == today()) "今天" else SimpleDateFormat("M/d", Locale.US).format(parseDateOrToday(ds)), fontSize = 10.sp, color = if (isSel) SemanticColors.Primary else SemanticColors.TextSecondary)
            }
        }
    }
    val avg = days.map { it.second }.filter { it > 0 }
    Text(if (avg.isEmpty()) "近 7 天还没有记录" else "平均达标 ${avg.sum() / avg.size}%，最高 ${avg.max()}%，共 ${days.count { it.second > 0 }} 天有记录", fontSize = 12.sp, color = SemanticColors.TextSecondary, modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp))
}

fun parseDateOrToday(ds: String): java.util.Date = try { SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(ds)!! } catch (e: Exception) { java.util.Date() }

@Composable
fun FamilyRow(label: String, value: Int, onChange: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 17.sp, modifier = Modifier.weight(1f))
        StepperButton(Icons.Rounded.Remove, value > 0) { onChange((value - 1).coerceAtLeast(0)) }
        Text("$value", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, modifier = Modifier.widthIn(min = 32.dp))
        StepperButton(Icons.Rounded.Add, value < 20) { onChange((value + 1).coerceAtMost(20)) }
    }
}

@Composable
private fun StepperButton(icon: androidx.compose.ui.graphics.vector.ImageVector, enabled: Boolean, onClick: () -> Unit) {
    Surface(shape = RoundedCornerShape(9.dp), color = if (enabled) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f), onClick = onClick, enabled = enabled, modifier = Modifier.size(32.dp)) {
        Box(contentAlignment = Alignment.Center) { Icon(icon, contentDescription = null, tint = if (enabled) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f), modifier = Modifier.size(17.dp)) }
    }
}

@Composable
fun FamilyRow(label: String, value: Int, onChange: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 17.sp, modifier = Modifier.weight(1f))
        StepperButton(Icons.Rounded.Remove, value > 0) { onChange((value - 1).coerceAtLeast(0)) }
        Text("$value", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, modifier = Modifier.widthIn(min = 32.dp))
        StepperButton(Icons.Rounded.Add, value < 20) { onChange((value + 1).coerceAtMost(20)) }
    }
}

@Composable
private fun StepperButton(icon: androidx.compose.ui.graphics.vector.ImageVector, enabled: Boolean, onClick: () -> Unit) {
    Surface(shape = RoundedCornerShape(9.dp), color = if (enabled) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f), onClick = onClick, enabled = enabled, modifier = Modifier.size(32.dp)) {
        Box(contentAlignment = Alignment.Center) { Icon(icon, contentDescription = null, tint = if (enabled) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f), modifier = Modifier.size(17.dp)) }
    }
}

@Composable
fun FamilyRow(label: String, value: Int, onChange: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 17.sp, modifier = Modifier.weight(1f))
        StepperButton(Icons.Rounded.Remove, value > 0) { onChange((value - 1).coerceAtLeast(0)) }
        Text("$value", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, modifier = Modifier.widthIn(min = 32.dp))
        StepperButton(Icons.Rounded.Add, value < 20) { onChange((value + 1).coerceAtMost(20)) }
    }
}

@Composable
private fun StepperButton(icon: androidx.compose.ui.graphics.vector.ImageVector, enabled: Boolean, onClick: () -> Unit) {
    Surface(shape = RoundedCornerShape(9.dp), color = if (enabled) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f), onClick = onClick, enabled = enabled, modifier = Modifier.size(32.dp)) {
        Box(contentAlignment = Alignment.Center) { Icon(icon, contentDescription = null, tint = if (enabled) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f), modifier = Modifier.size(17.dp)) }
    }
}
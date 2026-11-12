package com.guoziri.app

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun NutritionPage(repo: StoreRepo, date: String) {
    val result = remember(date, repo.purchases.size, repo.settings.family) {
        Nutrition.assess(repo.purchases.filter { it.date == date }, repo.settings.family)
    }
    val overall = if (result.assessment.isNotEmpty())
        result.assessment.sumOf { minOf(it.percent, 100) } / result.assessment.size else 0
    val ringColor = when {
        overall >= 90 -> IosGreen
        overall >= 60 -> IosOrange
        else -> IosRed
    }
    val t = today()

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 110.dp)) {
        Text("营养", fontSize = 34.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 20.dp, top = 12.dp, bottom = 6.dp))
        Text("${if (date == t) "今天" else fmtDay(date)} · ${weekDay(date)} · 《中国居民膳食指南 2022》",
            fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            modifier = Modifier.padding(start = 20.dp))

        GroupTitle("总评")
        GroupCard {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(96.dp), contentAlignment = Alignment.Center) {
                    Canvas(Modifier.fillMaxSize()) {
                        val stroke = 20f
                        drawArc(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                            startAngle = 0f, sweepAngle = 360f, useCenter = false,
                            style = Stroke(stroke))
                        drawArc(color = ringColor,
                            startAngle = -90f, sweepAngle = 360f * minOf(overall, 100) / 100f, useCenter = false,
                            style = Stroke(stroke, cap = StrokeCap.Round))
                    }
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

        if (result.unmatched.isNotEmpty()) {
            GroupTitle("未计入营养")
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
}

@Composable
fun FamilyRow(label: String, value: Int, onChange: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 17.sp, modifier = Modifier.weight(1f))
        Button(
            onClick = { onChange((value - 1).coerceAtLeast(0)) },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
        ) { Text("−", fontSize = 18.sp, color = MaterialTheme.colorScheme.primary) }
        Text("$value", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.widthIn(min = 32.dp))
        Button(
            onClick = { onChange((value + 1).coerceAtMost(20)) },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
        ) { Text("＋", fontSize = 18.sp, color = MaterialTheme.colorScheme.primary) }
    }
}

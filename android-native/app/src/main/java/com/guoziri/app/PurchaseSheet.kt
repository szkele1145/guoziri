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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchaseSheet(
    repo: StoreRepo,
    editing: Purchase?,                 // null = 新增
    anchors: GuideAnchors,
    guideStep: Int? = null,             // 非 null 时在弹窗内显示引导
    onNext: (() -> Unit)? = null,
    onSkip: (() -> Unit)? = null,
    onDismiss: () -> Unit,
    snackbar: (String) -> Unit,
) {
    var name by remember { mutableStateOf(editing?.name ?: "") }
    var category by remember { mutableStateOf(editing?.category ?: "蔬菜") }
    var price by remember { mutableStateOf(editing?.pricePerJin?.toString() ?: "") }
    var total by remember { mutableStateOf(editing?.totalCost?.toString() ?: "") }
    var jin by remember { mutableStateOf(editing?.totalJin?.toString() ?: "") }
    var grams by remember { mutableStateOf(editing?.weightGrams?.toString() ?: "") }
    var date by remember { mutableStateOf(editing?.date ?: today()) }
    var note by remember { mutableStateOf(editing?.note ?: "") }
    var showCatPicker by remember { mutableStateOf(false) }
    var showSuggest by remember { mutableStateOf(false) }
    var cfCalories by remember { mutableStateOf("") }
    var cfProtein by remember { mutableStateOf("") }
    var cfFat by remember { mutableStateOf("") }
    var cfCarbs by remember { mutableStateOf("") }
    var cfFiber by remember { mutableStateOf("") }
    var cfVitC by remember { mutableStateOf("") }
    var cfCalcium by remember { mutableStateOf("") }
    var cfIron by remember { mutableStateOf("") }

    val gStep = guideStep
    val gTarget = if (gStep != null) GuideSteps.STEPS.getOrNull(gStep)?.target else null
    val gRect = if (gTarget != null) anchors.rect(gTarget, "purchase") else null

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Box(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(bottom = if (gStep != null) 190.dp else 20.dp)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onDismiss) { Text("取消", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)) }
                Text(if (editing != null) "编辑记录" else "新增记录", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                TextButton(onClick = {
                    if (name.isBlank()) { snackbar("先填菜名"); return@TextButton }
                    val num: (String) -> Double? = { v -> v.trim().toDoubleOrNull() }
                    val rec = Purchase(
                        id = editing?.id ?: repo.newId(),
                        name = name.trim(),
                        category = category,
                        pricePerJin = num(price),
                        totalCost = num(total),
                        totalJin = num(jin),
                        weightGrams = num(grams),
                        date = date.ifBlank { today() },
                        note = note.trim().ifBlank { null },
                        createdAt = editing?.createdAt ?: repo.nowIso(),
                    )
                    if (editing != null) repo.updatePurchase(editing.id, rec) else repo.addPurchase(rec)
                    onDismiss()
                    snackbar(if (editing != null) "已更新" else "已保存")
                }, modifier = Modifier.guideAnchor(anchors, "purchase-save", "purchase")) {
                    Text("存储", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = IosBlue)
                }
            }

            GroupCard(Modifier.padding(horizontal = 14.dp)) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("菜名", fontSize = 16.sp)
                    Spacer(Modifier.width(20.dp))
                    OutlinedTextField(
                        value = name, onValueChange = { name = it; showSuggest = true },
                        placeholder = { Text("如：西兰花", fontSize = 15.sp) },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = Color.Transparent, focusedBorderColor = Color.Transparent),
                    )
                }
                // 联想下拉：库内食材 + 自定义食物
                val suggestions = remember(name, repo.settings.customFoods.size) {
                    val q = name.trim()
                    if (q.isEmpty()) emptyList()
                    else {
                        val lib = NutritionDB.DB.keys.filter { it.contains(q) || q.contains(it) }
                        val custom = repo.settings.customFoods
                            .filter { it.name.contains(q) || q.contains(it.name) }
                            .map { it.name }
                        (custom + lib).distinct().take(8)
                    }
                }
                if (showSuggest && suggestions.isNotEmpty()) {
                    suggestions.forEach { s ->
                        Row(
                            Modifier.fillMaxWidth().clickable { name = s; showSuggest = false }
                                .padding(horizontal = 16.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("🔍", fontSize = 13.sp)
                            Text(s, fontSize = 15.sp, modifier = Modifier.padding(start = 10.dp))
                            if (repo.findCustomFood(s) != null) {
                                Spacer(Modifier.width(8.dp))
                                Text("自定义", fontSize = 11.sp, color = IosBlue)
                            }
                        }
                        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
                // 命中提示
                val matched = remember(name) { NutritionDB.matchName(name) }
                if (name.isNotBlank() && matched != null) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                        Text("✓ 识别为「$matched」，按库内数据计算营养",
                            fontSize = 12.sp, color = IosGreen)
                    }
                } else if (name.isNotBlank() && repo.findCustomFood(name) != null) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                        Text("✓ 使用你录入的营养数据", fontSize = 12.sp, color = IosGreen)
                    }
                } else if (name.isNotBlank()) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                        Text("不在食材库，将按「$category」分类估算",
                            fontSize = 12.sp, color = IosOrange)
                    }
                }
                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                Row(
                    Modifier.fillMaxWidth().clickable { showCatPicker = true }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("分类", fontSize = 16.sp)
                    Spacer(Modifier.weight(1f))
                    Text("$category ˅", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f))
                }
            }

            // 表外食物：手动录入营养（每 100g），存成自定义食物
            val needCustom = name.isNotBlank() &&
                NutritionDB.matchName(name) == null &&
                repo.findCustomFood(name) == null
            if (needCustom) {
                GroupTitle("不在食材库？手动填营养")
                GroupCard(Modifier.padding(horizontal = 14.dp)) {
                    Text("按每 100g 填，保存后下次直接识别",
                        fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                    NutriInput("热量", "kcal", cfCalories) { cfCalories = it }
                    NutriInput("蛋白质", "g", cfProtein) { cfProtein = it }
                    NutriInput("脂肪", "g", cfFat) { cfFat = it }
                    NutriInput("碳水", "g", cfCarbs) { cfCarbs = it }
                    NutriInput("膳食纤维", "g", cfFiber) { cfFiber = it }
                    NutriInput("维生素C", "mg", cfVitC) { cfVitC = it }
                    NutriInput("钙", "mg", cfCalcium) { cfCalcium = it }
                    NutriInput("铁", "mg", cfIron) { cfIron = it }
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = {
                            cfCalories = ""; cfProtein = ""; cfFat = ""; cfCarbs = ""
                            cfFiber = ""; cfVitC = ""; cfCalcium = ""; cfIron = ""
                        }) { Text("清空", fontSize = 14.sp) }
                        Button(onClick = {
                            val d: (String) -> Double = { it.trim().toDoubleOrNull() ?: 0.0 }
                            if (d(cfCalories) <= 0.0) {
                                snackbar("至少填一下热量")
                                return@Button
                            }
                            repo.addCustomFood(CustomFood(
                                name = name.trim(),
                                category = category,
                                calories = d(cfCalories), protein = d(cfProtein),
                                fat = d(cfFat), carbs = d(cfCarbs),
                                fiber = d(cfFiber), vitaminC = d(cfVitC),
                                calcium = d(cfCalcium), iron = d(cfIron),
                            ))
                            snackbar("已记住「${name.trim()}」的营养")
                        }, colors = ButtonDefaults.buttonColors(containerColor = IosBlue)) {
                            Text("记住这个食物", fontSize = 14.sp)
                        }
                    }
                }
            }

            GroupCard(Modifier.padding(horizontal = 14.dp)) {
                NumericRow("单价（元/斤）", price) { v ->
                    price = v
                    val c = calcDerived(price, total, jin, "price")
                    if (c.jin != null) jin = c.jin
                }
                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                NumericRow("总价（元）", total) { v ->
                    total = v
                    val c = calcDerived(price, total, jin, "total")
                    if (c.price != null) price = c.price
                }
                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                NumericRow("重量（斤）", jin) { v ->
                    jin = v
                    val c = calcDerived(price, total, jin, "jin")
                    if (c.price != null) price = c.price
                    autoCalc2(jin, grams) { grams = it }
                }
                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                NumericRow("重量（克）", grams) { v ->
                    grams = v
                    autoCalc2(jin, grams) { jin = it }
                }
            }

            GroupCard(Modifier.padding(horizontal = 14.dp)) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("日期", fontSize = 16.sp)
                    Spacer(Modifier.width(20.dp))
                    OutlinedTextField(
                        value = date, onValueChange = { date = it },
                        placeholder = { Text(today()) }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = Color.Transparent, focusedBorderColor = Color.Transparent),
                    )
                }
                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("备注", fontSize = 16.sp)
                    Spacer(Modifier.width(20.dp))
                    OutlinedTextField(
                        value = note, onValueChange = { note = it },
                        placeholder = { Text("选填") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = Color.Transparent, focusedBorderColor = Color.Transparent),
                    )
                }
            }
            Text("总价/单价、斤/克，填一组就能自动算出营养",
                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp), textAlign = TextAlign.Center)
        }

        // 弹窗内的引导层：聚光高亮 + 提示卡
        if (gStep != null) {
            if (gRect != null) {
                SpotlightOverlay(rect = gRect, alpha = 1f, modifier = Modifier.matchParentSize())
            }
            Box(
                Modifier
                    .matchParentSize()
                    .background(Color.Black.copy(alpha = 0.30f))
                    .pointerInput(Unit) { detectTapGestures { /* 吞掉点击 */ } },
            )
            GuideCard(
                step = gStep,
                total = GuideSteps.size,
                title = GuideSteps.STEPS[gStep].title,
                body = GuideSteps.STEPS[gStep].body,
                onSkip = { onSkip?.invoke() },
                onNext = { onNext?.invoke() },
                isLast = gStep >= GuideSteps.size - 1,
                modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
            )
        }
        }
    }

    if (showCatPicker) {
        AlertDialog(
            onDismissRequest = { showCatPicker = false },
            title = { Text("选择分类") },
            text = {
                Column {
                    CATEGORIES.forEach { c ->
                        Text("${CAT_ICON[c]} $c", fontSize = 16.sp,
                            color = if (c == category) IosBlue else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.fillMaxWidth().clickable {
                                category = c; showCatPicker = false
                            }.padding(vertical = 12.dp))
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showCatPicker = false }) { Text("确定") } },
            dismissButton = { TextButton(onClick = { showCatPicker = false }) { Text("取消") } },
        )
    }
}

@Composable
fun NutriInput(label: String, unit: String, value: String, onChange: (String) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, fontSize = 14.sp, modifier = Modifier.width(78.dp))
        OutlinedTextField(
            value = value, onValueChange = { v -> onChange(v.filter { it.isDigit() || it == '.' }) },
            placeholder = { Text("0", fontSize = 13.sp) },
            modifier = Modifier.weight(1f), singleLine = true,
            textStyle = TextStyle(fontSize = 14.sp),
            shape = RoundedCornerShape(8.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(unit, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
            modifier = Modifier.width(34.dp))
    }
}

@Composable
fun NumericRow(label: String, value: String, onChange: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 16.sp)
        Spacer(Modifier.width(20.dp))
        OutlinedTextField(
            value = value, onValueChange = { onChange(it) },
            placeholder = { Text("选填", fontSize = 15.sp) },
            modifier = Modifier.fillMaxWidth(), singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = Color.Transparent, focusedBorderColor = Color.Transparent),
        )
    }
}

/** 自动联动计算结果 */
data class CalcResult(val price: String? = null, val jin: String? = null)

/** 简化版自动联动：填总价+斤 → 算单价；总价+单价 → 算斤 */
fun calcDerived(priceS: String, totalS: String, jinS: String, src: String): CalcResult {
    val price = priceS.trim().toDoubleOrNull() ?: return CalcResult()
    val total = totalS.trim().toDoubleOrNull() ?: return CalcResult()
    val jin = jinS.trim().toDoubleOrNull()
    return when (src) {
        "total" -> if (jin != null && jin > 0)
            CalcResult(price = (Math.round(total / jin * 100) / 100.0).toString())
        else CalcResult()
        "price" -> if (total > 0 && jin == null)
            CalcResult(jin = (Math.round(total / price * 100) / 100.0).toString())
        else CalcResult()
        "jin" -> if (total > 0 && jin != null && jin > 0)
            CalcResult(price = (Math.round(total / jin * 100) / 100.0).toString())
        else CalcResult()
        else -> CalcResult()
    }
}

fun autoCalc2(jinS: String, gramsS: String, setGrams: (String) -> Unit) {
    val jin = jinS.toDoubleOrNull() ?: return
    if (gramsS.isBlank() || gramsS.toDoubleOrNull() == null) setGrams((jin * 500).toInt().toString())
}
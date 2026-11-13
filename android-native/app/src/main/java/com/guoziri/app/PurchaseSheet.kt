package com.guoziri.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchaseSheet(
    repo: StoreRepo,
    editing: Purchase?,                 // null = 新增
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

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp),
        ) {
            // ---- 标题栏 ----
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onDismiss) {
                    Text("取消", fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f))
                }
                Text(
                    if (editing != null) "编辑记录" else "新增记录",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                )
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
                }) {
                    Text("存储", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = IosBlue)
                }
            }

            // ---- 菜名 + 联想 ----
            GroupCard(Modifier.padding(horizontal = Dimens.PageH)) {
                FieldRow("菜名") {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it; showSuggest = true },
                        placeholder = { Text("如：西兰花", fontSize = 14.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        textStyle = TextStyle(fontSize = 15.sp),
                        shape = RoundedCornerShape(9.dp),
                    )
                }
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
                                .padding(horizontal = Dimens.CardPad, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Rounded.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                modifier = Modifier.size(15.dp),
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                s,
                                fontSize = Dimens2.Body,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            if (repo.findCustomFood(s) != null) {
                                Text("自填", fontSize = 10.sp, color = IosBlue)
                            }
                        }
                        CardDivider(modifier = Modifier.padding(start = Dimens.CardPad))
                    }
                }
                // 命中状态提示
                val matched = remember(name) { NutritionDB.matchName(name) }
                val customHit = remember(name) { repo.findCustomFood(name) }
                when {
                    name.isBlank() -> Unit
                    matched != null -> HintRow(Icons.Rounded.Check, IosGreen, "识别为「$matched」，按库内数据计算营养")
                    customHit != null -> HintRow(Icons.Rounded.Check, IosGreen, "使用你录入的营养数据")
                    else -> HintRow(null, IosOrange, "不在食材库，将按「$category」分类估算")
                }
                CardDivider()
                Row(
                    Modifier.fillMaxWidth().clickable { showCatPicker = true }
                        .padding(horizontal = Dimens.CardPad, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("分类", fontSize = Dimens2.Body, modifier = Modifier.weight(1f))
                    IconTile(categoryIcon(category), categoryTint(category), categoryIconTint(category), 26.dp, 15.dp)
                    Spacer(Modifier.width(8.dp))
                    Text(category, fontSize = Dimens2.Body)
                    Icon(
                        Icons.Rounded.KeyboardArrowDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            // ---- 价格 / 重量 ----
            GroupCard(Modifier.padding(horizontal = Dimens.PageH).padding(top = 12.dp)) {
                FieldRow("单价") {
                    OutlinedTextField(
                        value = price,
                        onValueChange = { v ->
                            price = v
                            val c = calcDerived(price, total, jin, "price")
                            if (c.jin != null) jin = c.jin
                        },
                        placeholder = { Text("元/斤", fontSize = 13.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        textStyle = TextStyle(fontSize = 15.sp),
                        shape = RoundedCornerShape(9.dp),
                    )
                }
                CardDivider()
                FieldRow("总价") {
                    OutlinedTextField(
                        value = total,
                        onValueChange = { v ->
                            total = v
                            val c = calcDerived(price, total, jin, "total")
                            if (c.price != null) price = c.price
                        },
                        placeholder = { Text("元", fontSize = 13.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        textStyle = TextStyle(fontSize = 15.sp),
                        shape = RoundedCornerShape(9.dp),
                    )
                }
                CardDivider()
                FieldRow("重量") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = jin,
                            onValueChange = { v ->
                                jin = v
                                val c = calcDerived(price, total, jin, "jin")
                                if (c.price != null) price = c.price
                                autoCalc2(jin, grams) { grams = it }
                            },
                            placeholder = { Text("斤", fontSize = 13.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            textStyle = TextStyle(fontSize = 15.sp),
                            shape = RoundedCornerShape(9.dp),
                        )
                        Spacer(Modifier.width(10.dp))
                        OutlinedTextField(
                            value = grams,
                            onValueChange = { v ->
                                grams = v
                                autoCalc2(jin, grams) { jin = it }
                            },
                            placeholder = { Text("克", fontSize = 13.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            textStyle = TextStyle(fontSize = 15.sp),
                            shape = RoundedCornerShape(9.dp),
                        )
                    }
                }
            }

            // ---- 表外食物：手动录营养 ----
            val needCustom = name.isNotBlank() &&
                NutritionDB.matchName(name) == null &&
                repo.findCustomFood(name) == null
            if (needCustom) {
                GroupTitle("不在食材库？手动填营养")
                GroupCard(Modifier.padding(horizontal = Dimens.PageH)) {
                    Text(
                        "按每 100g 填，保存后下次直接识别",
                        fontSize = Dimens2.Micro,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        modifier = Modifier.padding(horizontal = Dimens.CardPad, vertical = 9.dp),
                    )
                    NutriInput("热量", "kcal", cfCalories) { cfCalories = it }
                    NutriInput("蛋白质", "g", cfProtein) { cfProtein = it }
                    NutriInput("脂肪", "g", cfFat) { cfFat = it }
                    NutriInput("碳水", "g", cfCarbs) { cfCarbs = it }
                    NutriInput("膳食纤维", "g", cfFiber) { cfFiber = it }
                    NutriInput("维生素C", "mg", cfVitC) { cfVitC = it }
                    NutriInput("钙", "mg", cfCalcium) { cfCalcium = it }
                    NutriInput("铁", "mg", cfIron) { cfIron = it }
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = Dimens.CardPad, vertical = 10.dp),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(onClick = {
                            cfCalories = ""; cfProtein = ""; cfFat = ""; cfCarbs = ""
                            cfFiber = ""; cfVitC = ""; cfCalcium = ""; cfIron = ""
                        }) { Text("清空", fontSize = 14.sp) }
                        Spacer(Modifier.width(6.dp))
                        Button(
                            onClick = {
                                val d: (String) -> Double = { it.trim().toDoubleOrNull() ?: 0.0 }
                                if (d(cfCalories) <= 0.0) { snackbar("至少填一下热量"); return@Button }
                                repo.addCustomFood(
                                    CustomFood(
                                        name = name.trim(),
                                        category = category,
                                        calories = d(cfCalories), protein = d(cfProtein),
                                        fat = d(cfFat), carbs = d(cfCarbs),
                                        fiber = d(cfFiber), vitaminC = d(cfVitC),
                                        calcium = d(cfCalcium), iron = d(cfIron),
                                    ),
                                )
                                snackbar("已记住「${name.trim()}」的营养")
                            },
                            shape = RoundedCornerShape(9.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = IosBlue),
                        ) { Text("记住这个食物", fontSize = 14.sp) }
                    }
                }
            }

            // ---- 日期 / 备注 ----
            GroupCard(Modifier.padding(horizontal = Dimens.PageH).padding(top = 12.dp)) {
                FieldRow("日期") {
                    OutlinedTextField(
                        value = date,
                        onValueChange = { date = it },
                        placeholder = { Text(today(), fontSize = 13.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        textStyle = TextStyle(fontSize = 15.sp),
                        shape = RoundedCornerShape(9.dp),
                    )
                }
                CardDivider()
                FieldRow("备注") {
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        placeholder = { Text("选填", fontSize = 13.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        textStyle = TextStyle(fontSize = 15.sp),
                        shape = RoundedCornerShape(9.dp),
                    )
                }
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
                        Row(
                            Modifier.fillMaxWidth().clickable { category = c; showCatPicker = false }
                                .padding(vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            IconTile(categoryIcon(c), categoryTint(c), categoryIconTint(c), 28.dp, 16.dp)
                            Spacer(Modifier.width(11.dp))
                            Text(c, fontSize = Dimens2.Body, modifier = Modifier.weight(1f))
                            if (c == category) {
                                Icon(
                                    Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = IosBlue,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showCatPicker = false }) { Text("完成") } },
        )
    }
}

/* ---------------- 表单行 ---------------- */
@Composable
private fun FieldRow(label: String, content: @Composable () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = Dimens.CardPad, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, fontSize = Dimens2.Body, modifier = Modifier.width(52.dp))
        Box(Modifier.weight(1f)) { content() }
    }
}

/* ---------------- 提示行 ---------------- */
@Composable
private fun HintRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector?,
    tint: Color,
    text: String,
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = Dimens.CardPad, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(text, fontSize = Dimens2.Micro, color = tint)
    }
}

/* ---------------- 营养录入行 ---------------- */
@Composable
fun NutriInput(label: String, unit: String, value: String, onChange: (String) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = Dimens.CardPad, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, fontSize = Dimens2.Caption, modifier = Modifier.width(76.dp))
        OutlinedTextField(
            value = value,
            onValueChange = { v -> onChange(v.filter { it.isDigit() || it == '.' }) },
            placeholder = { Text("0", fontSize = 13.sp) },
            modifier = Modifier.weight(1f),
            singleLine = true,
            textStyle = TextStyle(fontSize = 14.sp),
            shape = RoundedCornerShape(8.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            unit,
            fontSize = Dimens2.Micro,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            modifier = Modifier.width(32.dp),
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
    val jin = jinS.trim().toDoubleOrNull() ?: return
    if (gramsS.isBlank() || gramsS.trim().toDoubleOrNull() == null) {
        setGrams((jin * 500).toInt().toString())
    }
}

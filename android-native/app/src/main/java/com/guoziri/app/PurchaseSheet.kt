package com.guoziri.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.guoziri.app.ui.theme.LocalAppColors

/**
 * 新增/编辑记录，结构对齐 web 版 #sheet-purchase。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchaseSheet(
    repo: StoreRepo,
    editing: Purchase?,
    onDismiss: () -> Unit,
    snackbar: (String) -> Unit,
) {
    val c = LocalAppColors.current

    var name by remember { mutableStateOf(editing?.name ?: "") }
    var category by remember { mutableStateOf(editing?.category ?: "蔬菜") }
    var price by remember { mutableStateOf(editing?.pricePerJin?.toString() ?: "") }
    var total by remember { mutableStateOf(editing?.totalCost?.toString() ?: "") }
    var jin by remember { mutableStateOf(editing?.totalJin?.toString() ?: "") }
    var grams by remember { mutableStateOf(editing?.weightGrams?.toString() ?: "") }
    var date by remember { mutableStateOf(editing?.date ?: today()) }
    var note by remember { mutableStateOf(editing?.note ?: "") }

    var showCatPicker by remember { mutableStateOf(false) }
    var showExtra by remember { mutableStateOf(false) }

    var cfCalories by remember { mutableStateOf("") }
    var cfProtein by remember { mutableStateOf("") }
    var cfFat by remember { mutableStateOf("") }
    var cfCarbs by remember { mutableStateOf("") }
    var cfFiber by remember { mutableStateOf("") }
    var cfVitC by remember { mutableStateOf("") }
    var cfCalcium by remember { mutableStateOf("") }
    var cfIron by remember { mutableStateOf("") }

    val matched = remember(name) { NutritionDB.matchName(name) }
    val customHit = remember(name) { repo.findCustomFood(name) }
    val needCustom = name.isNotBlank() && matched == null && customHit == null

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
        dragHandle = null,
        containerColor = c.bgElevated,
    ) {
        Column(Modifier.fillMaxWidth()) {
            SheetGrabber()
            SheetHead(
                title = if (editing != null) "编辑记录" else "新增记录",
                leftText = "取消",
                rightText = "存储",
                onLeft = onDismiss,
                onRight = {
                    if (name.isBlank()) {
                        snackbar("先填菜名")
                    } else {
                        val num: (String) -> Double? = { v -> v.trim().toDoubleOrNull() }
                        
                        // 自动保存自定义营养：如果用户填了营养但没点"记住这个食物"
                        if (needCustom) {
                            val d: (String) -> Double = { it.trim().toDoubleOrNull() ?: 0.0 }
                            if (d(cfCalories) > 0.0) {
                                repo.addCustomFood(
                                    CustomFood(
                                        name = name.trim(),
                                        category = category,
                                        calories = d(cfCalories),
                                        protein = d(cfProtein),
                                        fat = d(cfFat),
                                        carbs = d(cfCarbs),
                                        fiber = d(cfFiber),
                                        vitaminC = d(cfVitC),
                                        calcium = d(cfCalcium),
                                        iron = d(cfIron),
                                    ),
                                )
                                showExtra = false
                            }
                        }
                        
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
                    }
                },
            )

            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .navigationBarsPadding()
                    .padding(top = 16.dp, bottom = 24.dp),
            ) {
                // 菜名 + 分类
                SheetGroup {
                    FormRow("菜名", value = name, onChange = { name = it }, placeholder = "如：西兰花")
                    SheetDivider()
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = Dimens.RowH)
                            .clickable { showCatPicker = true }
                            .padding(horizontal = Dimens.CardPad, vertical = Dimens.CardPadV),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("分类", fontSize = Dimens2.Body, color = c.label, modifier = Modifier.weight(1f))
                        Text(category, fontSize = Dimens2.Body, color = c.label)
                        Chevron()
                    }
                }

                // 价格 / 重量
                SheetGroup {
                    FormRow(
                        "单价（元/斤）",
                        value = price,
                        onChange = { v ->
                            price = v
                            val r = calcDerived(price, total, jin, "price")
                            if (r.jin != null) jin = r.jin
                        },
                        keyboardType = KeyboardType.Decimal,
                        numericFilter = true,
                    )
                    SheetDivider()
                    FormRow(
                        "总价（元）",
                        value = total,
                        onChange = { v ->
                            total = v
                            val r = calcDerived(price, total, jin, "total")
                            if (r.price != null) price = r.price
                        },
                        keyboardType = KeyboardType.Decimal,
                        numericFilter = true,
                    )
                    SheetDivider()
                    FormRow(
                        "重量（斤）",
                        value = jin,
                        onChange = { v ->
                            jin = v
                            val r = calcDerived(price, total, jin, "jin")
                            if (r.price != null) price = r.price
                            autoCalc2(jin, grams) { grams = it }
                        },
                        keyboardType = KeyboardType.Decimal,
                        numericFilter = true,
                    )
                    SheetDivider()
                    FormRow(
                        "重量（克）",
                        value = grams,
                        onChange = { v ->
                            grams = v
                            autoCalc2(jin, grams) { jin = it }
                        },
                        keyboardType = KeyboardType.Number,
                        numericFilter = true,
                    )
                }

                // 日期 / 备注
                SheetGroup {
                    FormRow("日期", value = date, onChange = { date = it }, placeholder = today())
                    SheetDivider()
                    FormRow("备注", value = note, onChange = { note = it })
                }

                Hint("总价 / 单价、斤 / 克，填一组就能自动算出营养")

                // 表外食物：手动录营养（原生补充能力）
                if (needCustom) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { showExtra = !showExtra }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            if (showExtra) "收起自定义营养" else "「" + name + "」不在食材库，手动录入营养",
                            fontSize = Dimens2.Caption,
                            color = c.tint,
                        )
                    }
                    if (showExtra) {
                        SheetGroup {
                            FormRow("热量(kcal)", value = cfCalories, onChange = { cfCalories = it }, placeholder = "必填", numericFilter = true, keyboardType = KeyboardType.Decimal)
                            SheetDivider()
                            FormRow("蛋白质(g)", value = cfProtein, onChange = { cfProtein = it }, numericFilter = true, keyboardType = KeyboardType.Decimal)
                            SheetDivider()
                            FormRow("脂肪(g)", value = cfFat, onChange = { cfFat = it }, numericFilter = true, keyboardType = KeyboardType.Decimal)
                            SheetDivider()
                            FormRow("碳水(g)", value = cfCarbs, onChange = { cfCarbs = it }, numericFilter = true, keyboardType = KeyboardType.Decimal)
                            SheetDivider()
                            FormRow("膳食纤维(g)", value = cfFiber, onChange = { cfFiber = it }, numericFilter = true, keyboardType = KeyboardType.Decimal)
                            SheetDivider()
                            FormRow("维生素C(mg)", value = cfVitC, onChange = { cfVitC = it }, numericFilter = true, keyboardType = KeyboardType.Decimal)
                            SheetDivider()
                            FormRow("钙(mg)", value = cfCalcium, onChange = { cfCalcium = it }, numericFilter = true, keyboardType = KeyboardType.Decimal)
                            SheetDivider()
                            FormRow("铁(mg)", value = cfIron, onChange = { cfIron = it }, numericFilter = true, keyboardType = KeyboardType.Decimal)
                        }
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "记住这个食物",
                                fontSize = Dimens2.Body,
                                color = Color.White,
                                modifier = Modifier
                                    .background(c.tint, RoundedCornerShape(10.dp))
                                    .clickable {
                                        val d: (String) -> Double = { it.trim().toDoubleOrNull() ?: 0.0 }
                                        if (d(cfCalories) <= 0.0) {
                                            snackbar("至少填一下热量")
                                        } else {
                                            repo.addCustomFood(
                                                CustomFood(
                                                    name = name.trim(),
                                                    category = category,
                                                    calories = d(cfCalories),
                                                    protein = d(cfProtein),
                                                    fat = d(cfFat),
                                                    carbs = d(cfCarbs),
                                                    fiber = d(cfFiber),
                                                    vitaminC = d(cfVitC),
                                                    calcium = d(cfCalcium),
                                                    iron = d(cfIron),
                                                ),
                                            )
                                            snackbar("已记住「" + name.trim() + "」")
                                            showExtra = false
                                        }
                                    }
                                    .padding(horizontal = 20.dp, vertical = 10.dp),
                            )
                        }
                    }
                }
            }
        }
    }

    // 分类选择（底部 action-sheet，对齐 web 版 #action-sheet）
    if (showCatPicker) {
        SheetPicker(
            title = "选择分类",
            options = CATEGORIES.map { PickerOption(it, it, it == category) },
            onPick = { category = it; showCatPicker = false },
            onDismiss = { showCatPicker = false },
        )
    }
}


/** sheet 内 0.5px 分隔线（左起 16） */
@Composable
private fun SheetDivider() {
    HorizontalDivider(
        thickness = 0.5.dp,
        color = LocalAppColors.current.separator,
        modifier = Modifier.padding(start = 16.dp),
    )
}

/* ---------------- 价格联动计算 ---------------- */
data class CalcResult(val price: String? = null, val jin: String? = null)

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

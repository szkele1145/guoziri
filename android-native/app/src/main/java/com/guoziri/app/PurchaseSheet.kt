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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.guoziri.app.ui.theme.LocalAppColors
import kotlinx.coroutines.launch

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

    // ---------- 价格/数量互算 ----------
    // lastEdited 记录用户最后改的是哪个字段，联动时跳过它，避免光标跳动/覆盖输入
    var lastEdited by remember { mutableStateOf("") }

    // total = price × jin；jin = total / price；grams = jin × 500；jin = grams / 500
    fun syncFrom(which: String) {
        val p = price.toDoubleOrNull()
        val t = total.toDoubleOrNull()
        val j = jin.toDoubleOrNull()
        val g = grams.toDoubleOrNull()
        when (which) {
            "price" -> {
                // 单价变了：优先用总价算斤，否则用斤算总价
                if (t != null && p != null && p > 0) {
                    val nj = t / p
                    if ((jin.toDoubleOrNull() ?: -1.0) != nj) jin = fmt(nj)
                    if ((grams.toDoubleOrNull() ?: -1.0) != nj * 500) grams = fmt(nj * 500)
                } else if (j != null && p != null && p > 0) {
                    val nt = j * p
                    if ((total.toDoubleOrNull() ?: -1.0) != nt) total = fmt(nt)
                }
            }
            "total" -> {
                // 总价变了：有单价 → 算斤和克
                if (t != null && p != null && p > 0) {
                    val nj = t / p
                    if ((jin.toDoubleOrNull() ?: -1.0) != nj) jin = fmt(nj)
                    if ((grams.toDoubleOrNull() ?: -1.0) != nj * 500) grams = fmt(nj * 500)
                }
            }
            "jin" -> {
                // 斤变了：算总价和克
                if (j != null) {
                    if (p != null && p > 0 && (total.toDoubleOrNull() ?: -1.0) != j * p) total = fmt(j * p)
                    if ((grams.toDoubleOrNull() ?: -1.0) != j * 500) grams = fmt(j * 500)
                }
            }
            "grams" -> {
                // 克变了：算斤（和总价）
                if (g != null) {
                    val nj = g / 500
                    if ((jin.toDoubleOrNull() ?: -1.0) != nj) jin = fmt(nj)
                    if (p != null && p > 0 && (total.toDoubleOrNull() ?: -1.0) != nj * p) total = fmt(nj * p)
                }
            }
        }
    }

    // 智能填充（营养查询 Agent）状态
    var agentVisible by remember { mutableStateOf(false) }
    var agentLogs by remember { mutableStateOf(listOf<String>()) }
    var agentProgress by remember { mutableIntStateOf(0) }
    var agentFinished by remember { mutableStateOf(false) }

    /** 数值格式化：去尾零，最多 1 位小数 */
    fun fmt(v: Double): String = if (v == v.toLong().toDouble()) v.toLong().toString()
        else "%.1f".format(v).trimEnd('0').trimEnd('.')

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

    AppBottomSheet(
        visible = true,
        onDismiss = onDismiss,
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
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(top = 16.dp, bottom = 24.dp),
            ) {
                // 菜名 + 分类
                SheetGroup {
                    FoodNameField(
                        value = name,
                        onChange = { name = it },
                        suggestions = foodSuggestions(name, repo),
                    )
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
                            syncFrom("price")
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
                            syncFrom("total")
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
                            syncFrom("jin")
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
                            syncFrom("grams")
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
                        // 联网自动填充：调 Agent 搜索营养表并填入
                        Box(
                            Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "🔍 智能填充「" + name.trim() + "」营养",
                                fontSize = Dimens2.Body,
                                color = c.tint,
                                modifier = Modifier
                                    .background(c.tint.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
                                    .clickable {
                                        if (agentVisible) return@clickable
                                        agentVisible = true
                                        agentFinished = false
                                        agentProgress = 0
                                        agentLogs = listOf("🚀 启动营养查询 Agent")
                                        val task = FoodSearchService.buildTask(
                                            foodName = name.trim(),
                                            settings = repo.settings,
                                            onLog = { line -> agentLogs = agentLogs + line },
                                            onProgress = { p -> agentProgress = p },
                                            onResult = { food, err ->
                                                if (food != null) {
                                                    agentLogs = agentLogs + "📝 正在填充到表单..."
                                                    cfCalories = fmt(food.calories)
                                                    cfProtein = fmt(food.protein)
                                                    cfFat = fmt(food.fat)
                                                    cfCarbs = fmt(food.carbs)
                                                    cfFiber = fmt(food.fiber)
                                                    cfVitC = fmt(food.vitaminC)
                                                    cfCalcium = fmt(food.calcium)
                                                    cfIron = fmt(food.iron)
                                                    agentLogs = agentLogs + "✅ 填充完成，请核对数值"
                                                    agentProgress = 100
                                                } else {
                                                    agentLogs = agentLogs + ("❌ " + (err ?: "未知错误"))
                                                }
                                                agentFinished = true
                                            },
                                        )
                                        task()
                                    }
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                            )
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

    // 智能填充 Agent 日志弹窗
    AgentLogSheet(
        visible = agentVisible,
        title = "营养搜索 Agent",
        progress = agentProgress,
        logs = agentLogs,
        finished = agentFinished,
        onDismiss = { agentVisible = false },
    )
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

/* ---------------- 价格联动计算 ----------------
   逻辑已内联到 PurchaseSheet.syncFrom()：总价=单价×斤，斤↔克互算，
   修改任一字段实时联动其余字段。 */

/**
 * 菜名联想候选：输入前缀/包含匹配内置库 + 自定义食物 + 别名。
 * 最多返回 6 条；空输入返回空。
 */
fun foodSuggestions(input: String, repo: StoreRepo): List<String> {
    val q = input.trim()
    if (q.isEmpty()) return emptyList()
    val all = LinkedHashSet<String>()
    // 内置库（含别名）
    NutritionDB.allNames().forEach { all.add(it) }
    // 自定义食物
    repo.settings.customFoods.forEach { all.add(it.name) }
    // 匹配：前缀优先，其次包含
    val starts = all.filter { it.startsWith(q) }
    val contains = all.filter { !it.startsWith(q) && it.contains(q) }
    return (starts + contains).take(6)
}

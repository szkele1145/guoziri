package com.guoziri.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
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
                    Text("取消", fontSize = 15.sp, color = SemanticColors.TextSecondary)
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
                    Text("存储", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = SemanticColors.Primary)
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
                                tint = SemanticColors.TextTertiary,
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
                                Text("自定义", fontSize = 10.sp, color = SemanticColors.Primary)
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
                    matched != null -> HintRow(Icons.Rounded.Check, SemanticColors.Success, "识别为「$matched」，按库内数据计算营养")
                    customHit != null -> HintRow(Icons.Rounded.Check, SemanticColors.Success, "使用你录入的营养数据")
                    else -> HintRow(null, SemanticColors.Warning, "不在食材库，将按「$category」分类估算")
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
                        Icons.Rounded.ExpandMore,
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
                            colors = ButtonDefaults.buttonColors(containerColor = SemanticColors.Primary),
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
            Text("总价/单价、斤/克，填一组就能自动算出营养",
                fontSize = Dimens2.Micro, color = SemanticColors.TextQuaternary,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp), textAlign = TextAlign.Center)
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
                                    tint = SemanticColors.Primary,
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
fun HintRow(
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
            onValueChange = { onChange(v.filter { it.isDigit() || it == '.' }) },
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

/** 卡片内的金额显示 */
@Composable
fun MoneyDisplay(
    amount: Double?,
    fontSize: androidx.compose.ui.unit.TextUnit = Dimens2.Body,
    color: Color = SemanticColors.TextPrimary,
    fontWeight: FontWeight = FontWeight.Normal,
    showSign: Boolean = false
) {
    val text = money(amount)
    val displayText = if (showSign && amount != null && amount > 0) "+$text" else text
    Text(
        text = displayText,
        fontSize = fontSize,
        fontWeight = fontWeight,
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

/** 卡片内的分隔线 */
@Composable
fun CardDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        thickness = Dimens.Hairline,
        color = SemanticColors.Divider.copy(alpha = 0.5f),
        modifier = modifier.padding(start = Dimens.CardPad),
    )
}

/* ==================================================================
   按钮辅助
   ================================================================== */

/** 主按钮 - 主要操作 */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    loading: Boolean = false
) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(48.dp),
        enabled = enabled && !loading,
        colors = ButtonDefaults.buttonColors(
            containerColor = SemanticColors.Primary,
            contentColor = Color.White,
            disabledContainerColor = SemanticColors.Disabled,
            disabledContentColor = SemanticColors.TextQuaternary,
        ),
        shape = RoundedCornerShape(Dimens.BtnR),
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = Color.White,
                strokeWidth = 2.5.dp,
            )
        } else {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                icon?.let {
                    Icon(imageVector = it, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp).padding(end = 8.dp))
                }
                Text(text, fontSize = Dimens2.Body, fontWeight = FontWeight.SemiBold, color = Color.White)
            }
        }
    }
}

/** 次要按钮 - 次要操作 */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null
) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(44.dp),
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = SemanticColors.Primary,
            disabledContainerColor = SemanticColors.Disabled.copy(alpha = 0.1f),
            disabledContentColor = SemanticColors.TextQuaternary,
        ),
        shape = RoundedCornerShape(Dimens.BtnR),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            icon?.let {
                Icon(imageVector = it, contentDescription = null, tint = SemanticColors.Primary, modifier = Modifier.size(18.dp).padding(end = 8.dp))
            }
            Text(text, fontSize = Dimens2.Body, fontWeight = FontWeight.SemiBold, color = SemanticColors.Primary)
        }
    }
}

/** 轮廓按钮 - 轻量操作 */
@Composable
fun OutlineButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    tint: Color = SemanticColors.Primary
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(44.dp),
        enabled = enabled,
        colors = OutlinedButtonDefaults.buttonColors(
            contentColor = tint,
            disabledContentColor = SemanticColors.TextQuaternary,
            outlineColor = SemanticColors.Divider,
            disabledOutlineColor = SemanticColors.Divider.copy(alpha = 0.3f),
        ),
        shape = RoundedCornerShape(Dimens.BtnR),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            icon?.let {
                Icon(imageVector = it, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp).padding(end = 8.dp))
            }
            Text(text, fontSize = Dimens2.Body, fontWeight = FontWeight.Medium, color = tint)
        }
    }
}

/** 文本按钮 - 纯文本操作 */
@Composable
fun TextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color = SemanticColors.Primary,
    fontSize: androidx.compose.ui.unit.TextUnit = Dimens2.Body
) {
    TextButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = TextButtonDefaults.textButtonColors(
            contentColor = color,
            disabledContentColor = SemanticColors.TextQuaternary,
        ),
    ) {
        Text(text, fontSize = fontSize, fontWeight = FontWeight.Medium, color = color)
    }
}

/* ==================================================================
   表单组件
   ================================================================== */

/** 带标签的输入框行 */
@Composable
fun LabeledInput(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    placeholderText: String = "",
    error: String? = null,
    helperText: String? = null,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    onTrailingClick: (() -> Unit)? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 11.sp, color = SemanticColors.TextSecondary) },
        placeholder = { Text(placeholderText, fontSize = 14.sp) },
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        singleLine = singleLine,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = visualTransformation,
        placeholder = { Text(placeholderText, fontSize = 14.sp, color = SemanticColors.TextTertiary) },
        shape = RoundedCornerShape(Dimens.InputR),
        textStyle = TextStyle(fontSize = 15.sp),
        isError = error != null,
        leadingIcon = leadingIcon?.let { { Icon(it, contentDescription = null, tint = SemanticColors.TextSecondary, modifier = Modifier.size(18.dp)) } },
        trailingIcon = trailingIcon?.let { { IconButton(onClick = onTrailingClick) { Icon(it, contentDescription = null, tint = SemanticColors.TextSecondary, modifier = Modifier.size(18.dp)) } } },
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = SemanticColors.InputBg,
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedBorderColor = SemanticColors.Divider.copy(alpha = 0.5f),
            focusedBorderColor = SemanticColors.Primary,
            errorBorderColor = SemanticColors.Error,
            disabledContainerColor = SemanticColors.InputBg.copy(alpha = 0.5f),
            disabledBorderColor = SemanticColors.Divider.copy(alpha = 0.3f),
            labelColor = SemanticColors.TextSecondary,
            focusedLabelColor = SemanticColors.Primary,
            errorLabelColor = SemanticColors.Error,
        ),
    )
}

/** 密码输入框 */
@Composable
fun PasswordField(
    label: String = "密码",
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "请输入密码",
    modifier: Modifier = Modifier,
    error: String? = null,
    showToggle: Boolean = true,
) {
    var showPassword by remember { mutableStateOf(false) }
    LabeledInput(
        label = label,
        value = value,
        onValueChange = onValueChange,
        placeholder = placeholder,
        modifier = modifier,
        singleLine = true,
        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
        placeholderText = placeholder,
        error = error,
        trailingIcon = if (showToggle) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
        onTrailingClick = { showPassword = !showPassword },
    )
}

/* ==================================================================
   列表项组件
   ================================================================== */

/** 列表项 - 标准行 */
@Composable
fun ListItem(
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
    title: String,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    divider: Boolean = true,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = Dimens.PageH, vertical = 12.dp),
    ) {
        leading?.invoke()
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = Dimens2.Body, maxLines = 1, overflow = TextOverflow.Ellipsis, color = SemanticColors.TextPrimary)
            subtitle?.let {
                Text(
                    it,
                    fontSize = Dimens2.Caption,
                    color = SemanticColors.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        trailing?.invoke()
        if (onClick != null) {
            Icon(Icons.Rounded.ChevronRight, contentDescription = "进入", tint = SemanticColors.TextTertiary, modifier = Modifier.size(18.dp).padding(start = 8.dp))
        }
        if (divider) CardDivider()
    }
}

/** 购买记录行 */
@Composable
fun PurchaseRow(
    p: Purchase,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() }
            .padding(horizontal = Dimens.PageH, vertical = 9.dp),
    ) {
        IconTile(
            icon = categoryIcon(p.category),
            bg = categoryTint(p.category),
            tint = categoryIconTint(p.category),
        )
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(
                p.name,
                fontSize = Dimens2.Body,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val wTxt = weightText(p)
            val parts = mutableListOf<String>()
            if (p.pricePerJin != null) parts.add("${money(p.pricePerJin)}/斤")
            if (wTxt.isNotBlank()) parts.add(wTxt)
            if (parts.isNotEmpty()) {
                Text(
                    parts.joinToString(" · "),
                    fontSize = Dimens2.Caption,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.42f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        Text(
            p.totalCost?.let { money(it) } ?: "—",
            fontSize = Dimens2.Body,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            textAlign = TextAlign.End,
            modifier = Modifier.padding(end = 4.dp),
        )
        // 更多按钮：编辑 / 删除
        var menuOpen by remember { mutableStateOf(false) }
        Box {
            IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Rounded.MoreVert,
                    contentDescription = "更多",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    modifier = Modifier.size(19.dp),
                )
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text("编辑", fontSize = 14.sp) },
                    leadingIcon = { Icon(Icons.Rounded.Edit, null, Modifier.size(17.dp)) },
                    onClick = { menuOpen = false; onEdit() },
                )
                DropdownMenuItem(
                    text = { Text("删除", fontSize = 14.sp, color = SemanticColors.Error) },
                    leadingIcon = { Icon(Icons.Rounded.Delete, null, Modifier.size(17.dp), tint = SemanticColors.Error) },
                    onClick = { menuOpen = false; onDelete() },
                )
            }
        }
    }
}

/* ==================================================================
   弹窗组件
   ================================================================== */

/** 确认弹窗 */
@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String = "确定",
    dismissText: String = "取消",
    confirmColor: Color = SemanticColors.Primary,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontSize = 17.sp, fontWeight = FontWeight.Bold) },
        text = { Text(message, fontSize = 15.sp, color = SemanticColors.TextSecondary, lineHeight = 22.sp) },
        confirmButton = { TextButton(onClick = { onConfirm(); onDismiss() }) { Text(confirmText, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = confirmColor) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(dismissText, fontSize = 14.sp, color = SemanticColors.TextSecondary) } },
    )
}

/** 底部操作表 */
@Composable
fun BottomActionSheet(
    title: String,
    items: List<ActionItem>,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        Column(Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            Text(title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            Spacer(Modifier.height(8.dp))
            HorizontalDivider(thickness = Dimens.Hairline, color = SemanticColors.Divider)
            Column {
                items.forEach { item ->
                    Text(
                        text = item.text,
                        fontSize = 16.sp,
                        color = if (item.destructive) SemanticColors.Error else SemanticColors.TextPrimary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                            .clickable { item.action(); onDismiss() }
                            .background(MaterialTheme.colorScheme.surface),
                    )
                    if (item != items.last()) HorizontalDivider(thickness = Dimens.Hairline, color = SemanticColors.Divider)
                }
            }
        }
    }
}

data class ActionItem(
    val text: String,
    val destructive: Boolean = false,
    val action: () -> Unit
)

/* ==================================================================
   加载指示器
   ================================================================== */

@Composable
fun LoadingOverlay(
    message: String = "加载中...",
    modifier: Modifier = Modifier,
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.3f))
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp),
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(28.dp),
                color = SemanticColors.Primary,
                strokeWidth = 3.dp,
            )
            Spacer(Modifier.height(16.dp))
            Text(message, fontSize = Dimens2.Body, color = Color.White, fontWeight = FontWeight.Medium)
        }
    }
}

/* ==================================================================
   Toast / Snackbar 封装
   ================================================================== */

@Composable
fun rememberSnackbarHost(): SnackbarHostState = remember { SnackbarHostState() }

fun showSnackbar(
    host: SnackbarHostState,
    message: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    duration: Int = SnackbarDuration.Short,
) {
    host.showSnackbar(
        message = message,
        actionLabel = actionLabel,
        onAction = onAction,
        duration = duration,
        backgroundColor = MaterialTheme.colorScheme.inverseSurface,
        contentColor = MaterialTheme.colorScheme.inverseOnSurface,
        actionColor = SemanticColors.Primary,
    )
}

/* ==================================================================
   工具函数
   ================================================================== */

fun today(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

fun fmtDay(ds: String): String = try {
    val p = ds.split("-")
    "${p[1].toInt()}月${p[2].toInt()}日"
} catch (e: Exception) { ds }

fun weekDay(ds: String): String = try {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    "周" + "日一二三四五六"[sdf.parse(ds)!!.day]
} catch (e: Exception) { "" }

fun weightText(p: Purchase): String {
    val w = Nutrition.parseWeight(p)
    return when {
        w >= 1000 -> String.format(Locale.US, "%.2fkg", w / 1000)
        w > 0 -> "${w.toInt()}g"
        else -> ""
    }
}

/** 格式化日期：M月d日 */
fun fmtDay(ds: String): String = try {
    val p = ds.split("-")
    "${p[1].toInt()}月${p[2].toInt()}日"
} catch (e: Exception) { ds }

/** 星期几 */
fun weekDay(ds: String): String = try {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    "周" + "日一二三四五六"[sdf.parse(ds)!!.day]
} catch (e: Exception) { "" }

/** 重量文本 */
fun weightText(p: Purchase): String {
    val w = Nutrition.parseWeight(p)
    return when {
        w >= 1000 -> String.format(Locale.US, "%.2fkg", w / 1000)
        w > 0 -> "${w.toInt()}g"
        else -> ""
    }
}

/** 格式化日期：M月d日 */
fun fmtDay(ds: String): String = try {
    val p = ds.split("-")
    "${p[1].toInt()}月${p[2].toInt()}日"
} catch (e: Exception) { ds }

/** 星期几 */
fun weekDay(ds: String): String = try {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    "周" + "日一二三四五六"[sdf.parse(ds)!!.day]
} catch (e: Exception) { "" }

/** 重量文本 */
fun weightText(p: Purchase): String {
    val w = Nutrition.parseWeight(p)
    return when {
        w >= 1000 -> String.format(Locale.US, "%.2fkg", w / 1000)
        w > 0 -> "${w.toInt()}g"
        else -> ""
    }
}
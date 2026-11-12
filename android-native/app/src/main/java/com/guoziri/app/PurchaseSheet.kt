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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(bottom = 20.dp)) {
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
                }) { Text("存储", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = IosBlue) }
            }

            GroupCard(Modifier.padding(horizontal = 14.dp)) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("菜名", fontSize = 16.sp)
                    Spacer(Modifier.width(20.dp))
                    OutlinedTextField(
                        value = name, onValueChange = { name = it },
                        placeholder = { Text("如：西兰花", fontSize = 15.sp) },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = Color.Transparent, focusedBorderColor = Color.Transparent),
                    )
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

            GroupCard(Modifier.padding(horizontal = 14.dp)) {
                NumericRow("单价（元/斤）", price) { price = it; autoCalc(price, total, jin, grams, "price", { _, v -> total = v.toString() }, { _, v -> jin = v.toString() }) }
                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                NumericRow("总价（元）", total) { total = it; autoCalc(price, total, jin, grams, "total", { _, v -> price = v.toString() }, { _, v -> jin = v.toString() }) }
                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                NumericRow("重量（斤）", jin) { jin = it; autoCalc(price, total, jin, grams, "jin", { _, v -> price = v.toString() }, { _, v -> total = v.toString() }); autoCalc2(jin, grams) { grams = it } }
                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                NumericRow("重量（克）", grams) { grams = it; autoCalc(price, total, jin, grams, "grams", { _, v -> price = v.toString() }, { _, v -> jin = v.toString() }); autoCalc2(jin, grams) { jin = it } }
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
    }

    if (showCatPicker) {
        AlertDialog(
            onDismissRequest = { showCatPicker = false },
            confirmButton = { },
            dismissButton = { },
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
        )
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

/** 简化版自动联动：填总价+斤 → 算单价；总价+单价 → 算斤 */
fun autoCalc(priceS: String, totalS: String, jinS: String, gramsS: String, src: String,
             setPrice: (String, String) -> Unit, setJin: (String, String) -> Unit) {
    val price = priceS.toDoubleOrNull() ?: return
    val total = totalS.toDoubleOrNull() ?: return
    val jin = jinS.toDoubleOrNull()
    when (src) {
        "total" -> if (jin != null && jin > 0) setPrice("", (Math.round(total / jin * 100) / 100.0).toString())
        "price" -> if (total > 0 && jin == null) setJin("", (Math.round(total / price * 100) / 100.0).toString())
        "jin" -> if (total > 0) setPrice("", (Math.round(total / jin * 100) / 100.0).toString())
    }
}

fun autoCalc2(jinS: String, gramsS: String, setGrams: (String) -> Unit) {
    val jin = jinS.toDoubleOrNull() ?: return
    if (gramsS.isBlank() || gramsS.toDoubleOrNull() == null) setGrams((jin * 500).toInt().toString())
}
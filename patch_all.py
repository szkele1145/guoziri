import io
import os

BASE = r"C:\Users\一只屑\Desktop\HousewifeAppProto\android-native\app\src\main\java\com\guoziri\app"


def read(name):
    with io.open(os.path.join(BASE, name), encoding="utf-8") as f:
        return f.read()


def write(name, content):
    with io.open(os.path.join(BASE, name), "w", encoding="utf-8", newline="\n") as f:
        f.write(content)
    print("written:", name)


def sub(src, old, new, label):
    assert old in src, "NOT FOUND: " + label
    return src.replace(old, new, 1)


# ---------------- 1) Chat.kt: MediaType.parse ----------------
src = read("Chat.kt")
src = sub(
    src,
    '.post(RequestBody.create(MediaType.get("application/json"), body.toString()))',
    '.post(RequestBody.create(MediaType.parse("application/json"), body.toString()))',
    "Chat MediaType",
)
write("Chat.kt", src)

# ---------------- 2) NutritionPage.kt: 用 CircularProgressIndicator 替换 Canvas ----------------
src = read("NutritionPage.kt")
old_ring = (
    '            val trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)\n'
    '            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {\n'
    '                Box(Modifier.size(96.dp), contentAlignment = Alignment.Center) {\n'
    '                    androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {\n'
    '                        val stroke = 20f\n'
    '                        drawArc(color = trackColor,\n'
    '                            startAngle = 0f, sweepAngle = 360f, useCenter = false,\n'
    '                            style = androidx.compose.ui.graphics.Stroke(stroke))\n'
    '                        drawArc(color = ringColor,\n'
    '                            startAngle = -90f, sweepAngle = 360f * minOf(overall, 100) / 100f, useCenter = false,\n'
    '                            style = androidx.compose.ui.graphics.Stroke(stroke, cap = StrokeCap.Round))\n'
    '                    }\n'
)
new_ring = (
    '            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {\n'
    '                Box(Modifier.size(96.dp), contentAlignment = Alignment.Center) {\n'
    '                    CircularProgressIndicator(\n'
    '                        progress = { minOf(overall, 100) / 100f },\n'
    '                        modifier = Modifier.fillMaxSize(),\n'
    '                        color = ringColor,\n'
    '                        trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),\n'
    '                        strokeWidth = 10.dp,\n'
    '                    )\n'
)
src = sub(src, old_ring, new_ring, "NutritionPage ring")
src = sub(src, "import androidx.compose.ui.graphics.StrokeCap\n", "", "NutritionPage StrokeCap import")
write("NutritionPage.kt", src)

# ---------------- 3) SettingsPage.kt: bodySmall -> fontSize ----------------
src = read("SettingsPage.kt")
src = sub(
    src,
    'Text("切换服务商自动带出地址和默认模型，Key 各自独立保存", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), modifier = Modifier.padding(horizontal = 20.dp, top = 8.dp))',
    'Text("切换服务商自动带出地址和默认模型，Key 各自独立保存", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), modifier = Modifier.padding(horizontal = 20.dp, top = 8.dp))',
    "SettingsPage bodySmall",
)
write("SettingsPage.kt", src)

# ---------------- 4) PurchaseSheet.kt: autoCalc 改成返回值式 ----------------
src = read("PurchaseSheet.kt")

old_calls = (
    '                NumericRow("单价（元/斤）", price) { price = it; autoCalc(price, total, jin, grams, "price", { _, v -> total = v }, { _, v -> jin = v }) }\n'
    '                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)\n'
    '                NumericRow("总价（元）", total) { total = it; autoCalc(price, total, jin, grams, "total", { _, v -> price = v }, { _, v -> jin = v }) }\n'
    '                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)\n'
    '                NumericRow("重量（斤）", jin) { jin = it; autoCalc(price, total, jin, grams, "jin", { _, v -> price = v }, { _, v -> total = v }); autoCalc2(jin, grams) { grams = it } }\n'
    '                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)\n'
    '                NumericRow("重量（克）", grams) { grams = it; autoCalc(price, total, jin, grams, "grams", { _, v -> price = v }, { _, v -> jin = v }); autoCalc2(jin, grams) { jin = it } }\n'
)
new_calls = (
    '                NumericRow("单价（元/斤）", price) { v ->\n'
    '                    price = v\n'
    '                    val c = calcDerived(price, total, jin, "price")\n'
    '                    if (c.jin != null) jin = c.jin\n'
    '                }\n'
    '                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)\n'
    '                NumericRow("总价（元）", total) { v ->\n'
    '                    total = v\n'
    '                    val c = calcDerived(price, total, jin, "total")\n'
    '                    if (c.price != null) price = c.price\n'
    '                }\n'
    '                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)\n'
    '                NumericRow("重量（斤）", jin) { v ->\n'
    '                    jin = v\n'
    '                    val c = calcDerived(price, total, jin, "jin")\n'
    '                    if (c.price != null) price = c.price\n'
    '                    autoCalc2(jin, grams) { grams = it }\n'
    '                }\n'
    '                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)\n'
    '                NumericRow("重量（克）", grams) { v ->\n'
    '                    grams = v\n'
    '                    autoCalc2(jin, grams) { jin = it }\n'
    '                }\n'
)
src = sub(src, old_calls, new_calls, "PurchaseSheet numeric rows")

old_fn = (
    '/** 简化版自动联动：填总价+斤 → 算单价；总价+单价 → 算斤 */\n'
    'fun autoCalc(priceS: String, totalS: String, jinS: String, gramsS: String, src: String,\n'
    '             setPrice: (String, String) -> Unit, setJin: (String, String) -> Unit) {\n'
    '    val price = priceS.toDoubleOrNull() ?: return\n'
    '    val total = totalS.toDoubleOrNull() ?: return\n'
    '    val jin = jinS.toDoubleOrNull()\n'
    '    when (src) {\n'
    '        "total" -> if (jin != null && jin > 0) setPrice(priceS, (Math.round(total / jin * 100) / 100.0).toString())\n'
    '        "price" -> if (total > 0 && jin == null) setJin(jinS, (Math.round(total / price * 100) / 100.0).toString())\n'
    '        "jin" -> if (total > 0) setPrice(priceS, (Math.round(total / jin * 100) / 100.0).toString())\n'
    '        "grams" -> if (total > 0) setJin(jinS, (Math.round(total / price * 100) / 100.0).toString())\n'
    '    }\n'
    '}\n'
)
new_fn = (
    '/** 自动联动计算结果 */\n'
    'data class CalcResult(val price: String? = null, val jin: String? = null)\n'
    '\n'
    '/** 简化版自动联动：填总价+斤 → 算单价；总价+单价 → 算斤 */\n'
    'fun calcDerived(priceS: String, totalS: String, jinS: String, src: String): CalcResult {\n'
    '    val price = priceS.trim().toDoubleOrNull() ?: return CalcResult()\n'
    '    val total = totalS.trim().toDoubleOrNull() ?: return CalcResult()\n'
    '    val jin = jinS.trim().toDoubleOrNull()\n'
    '    return when (src) {\n'
    '        "total" -> if (jin != null && jin > 0)\n'
    '            CalcResult(price = (Math.round(total / jin * 100) / 100.0).toString())\n'
    '        else CalcResult()\n'
    '        "price" -> if (total > 0 && jin == null)\n'
    '            CalcResult(jin = (Math.round(total / price * 100) / 100.0).toString())\n'
    '        else CalcResult()\n'
    '        "jin" -> if (total > 0 && jin != null && jin > 0)\n'
    '            CalcResult(price = (Math.round(total / jin * 100) / 100.0).toString())\n'
    '        else CalcResult()\n'
    '        else -> CalcResult()\n'
    '    }\n'
    '}\n'
)
src = sub(src, old_fn, new_fn, "PurchaseSheet autoCalc fn")
write("PurchaseSheet.kt", src)

print("ALL PATCHES APPLIED")

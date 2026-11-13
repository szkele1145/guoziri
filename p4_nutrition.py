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


src = read("NutritionPage.kt")

# --- 1) 头部加日期选择器 + 7 天趋势入口 ---
old = """    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 110.dp)) {
        Text("营养", fontSize = 34.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 20.dp, top = 12.dp, bottom = 6.dp))
        Text("${if (date == t) "今天" else fmtDay(date)} · ${weekDay(date)} · 《中国居民膳食指南 2022》",
            fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            modifier = Modifier.padding(start = 20.dp))
"""
new = """    var pickedDate by remember { mutableStateOf(date) }
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
"""
src = sub(src, old, new, "NutritionPage header date picker")

# --- 2) 结果计算用 pickedDate + customFoods ---
old = """    val result = remember(date, repo.purchases.size, repo.settings.family) {
        Nutrition.assess(repo.purchases.filter { it.date == date }, repo.settings.family)
    }"""
new = """    val result = remember(date, pickedDate, repo.purchases.size, repo.settings.family, customFoods.size) {
        Nutrition.assess(repo.purchases.filter { it.date == pickedDate }, repo.settings.family, customFoods)
    }"""
src = sub(src, old, new, "NutritionPage result")

# --- 3) 未计入营养 → 改为「按分类估算」提示 + 7 天趋势 ---
old = """        if (result.unmatched.isNotEmpty()) {
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
        }"""
new = """        // 食材明细：标出库内 / 自定义 / 估算
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
        }"""
src = sub(src, old, new, "NutritionPage details + trend")

# --- 4) 日期选择弹窗 ---
old = """        GroupTitle("家里几口人")
        GroupCard {
            FamilyRow("大人", repo.settings.family.adults) { v ->
                repo.updateSettings { it.copy(family = it.family.copy(adults = v)) }
            }
            FamilyRow("小孩", repo.settings.family.children) { v ->
                repo.updateSettings { it.copy(family = it.family.copy(children = v)) }
            }
        }
    }
}"""
new = """        GroupTitle("家里几口人")
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
    catch (e: Exception) { java.util.Date() }"""
src = sub(src, old, new, "NutritionPage date picker + trend")

write("NutritionPage.kt", src)
print("NUTRITION PAGE PATCHED")

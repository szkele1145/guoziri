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


src = read("PurchaseSheet.kt")

# --- 1) 菜名输入：联想下拉 + 表外检测 ---
old = """            GroupCard(Modifier.padding(horizontal = 14.dp)) {
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
            }"""
new = """            GroupCard(Modifier.padding(horizontal = 14.dp)) {
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
            }"""
src = sub(src, old, new, "PurchaseSheet name suggestion")

# --- 2) 新增 state ---
old = """    var note by remember { mutableStateOf(editing?.note ?: "") }
    var showCatPicker by remember { mutableStateOf(false) }"""
new = """    var note by remember { mutableStateOf(editing?.note ?: "") }
    var showCatPicker by remember { mutableStateOf(false) }
    var showSuggest by remember { mutableStateOf(false) }
    var cfCalories by remember { mutableStateOf("") }
    var cfProtein by remember { mutableStateOf("") }
    var cfFat by remember { mutableStateOf("") }
    var cfCarbs by remember { mutableStateOf("") }
    var cfFiber by remember { mutableStateOf("") }
    var cfVitC by remember { mutableStateOf("") }
    var cfCalcium by remember { mutableStateOf("") }
    var cfIron by remember { mutableStateOf("") }"""
src = sub(src, old, new, "PurchaseSheet states")

# --- 3) NutriInput 组件 ---
old = """@Composable
fun NumericRow(label: String, value: String, onChange: (String) -> Unit) {"""
new = """@Composable
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
fun NumericRow(label: String, value: String, onChange: (String) -> Unit) {"""
src = sub(src, old, new, "PurchaseSheet NutriInput")

# --- 4) 需要 TextStyle 导入 ---
old = "import androidx.compose.ui.text.font.FontWeight\n"
new = "import androidx.compose.ui.text.font.FontWeight\nimport androidx.compose.ui.text.TextStyle\n"
src = sub(src, old, new, "PurchaseSheet TextStyle import")

write("PurchaseSheet.kt", src)
print("PURCHASE SHEET PATCHED")

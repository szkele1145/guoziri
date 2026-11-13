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


# =====================================================================
# Store.kt
# =====================================================================
src = read("Store.kt")

# --- 1) 引入自定义食物模型 + 分类估算表 ---
old = """/* 营养项评估结果 */
data class NutrientAssess(
    val key: String, val label: String, val unit: String,
    val actual: Double, val target: Double, val percent: Int,
)
"""
new = """/* 营养项评估结果 */
data class NutrientAssess(
    val key: String, val label: String, val unit: String,
    val actual: Double, val target: Double, val percent: Int,
)

/* ---------------- 自定义食物 ----------------
   用户手工录入的营养数据（每 100g），存 settings.json 的 customFoods 里。
   字段与 NutritionDB 的 map 结构一致，可直接并入计算。 */
data class CustomFood(
    val name: String,
    val category: String = "其他",
    val calories: Double = 0.0,
    val protein: Double = 0.0,
    val fat: Double = 0.0,
    val carbs: Double = 0.0,
    val fiber: Double = 0.0,
    val vitaminC: Double = 0.0,
    val calcium: Double = 0.0,
    val iron: Double = 0.0,
    val createdAt: String = "",
) {
    /** 转成 NutritionDB 用的 map 形态 */
    fun toMap(): Map<String, Double> = mapOf(
        "calories" to calories, "protein" to protein, "fat" to fat,
        "carbs" to carbs, "fiber" to fiber, "vitaminC" to vitaminC,
        "calcium" to calcium, "iron" to iron,
    )
}
"""
src = sub(src, old, new, "Store CustomFood model")

# --- 2) Settings 增加 customFoods 字段 ---
old = """    val apiModel: String = "deepseek-chat",
    val guideDone: Boolean = false,
) {
    val hasKey: Boolean get() = apiKey.isNotBlank()
}"""
new = """    val apiModel: String = "deepseek-chat",
    val guideDone: Boolean = false,
    val customFoods: List<CustomFood> = emptyList(),
) {
    val hasKey: Boolean get() = apiKey.isNotBlank()
}"""
src = sub(src, old, new, "Store Settings customFoods")

# --- 3) NutritionDB 增加「按分类估算」的兜底 ---
old = """    fun find(name: String): Map<String, Double>? {
        if (name.isBlank()) return null
        DB.keys.forEach { k -> if (name.contains(k) || k.contains(name)) return DB[k] }
        return null
    }
}"""
new = """    fun find(name: String): Map<String, Double>? {
        if (name.isBlank()) return null
        DB.keys.forEach { k -> if (name.contains(k) || k.contains(name)) return DB[k] }
        return null
    }

    /* 表内食材的中文别名 → 库内标准名，用于「五花肉→猪肉」这类模糊命中 */
    private val ALIAS: Map<String, String> = mapOf(
        "五花肉" to "猪肉", "猪蹄" to "猪肉", "排骨" to "猪肉", "里脊" to "猪里脊",
        "牛腩" to "牛肉", "牛排" to "牛肉", "肥牛" to "牛肉", "牛肉粒" to "牛肉",
        "羊排" to "羊肉", "羊肉片" to "羊肉", "羊腿" to "羊肉",
        "鸡翅" to "鸡肉", "鸡腿" to "鸡肉", "鸡爪" to "鸡肉", "鸡胸" to "鸡胸肉",
        "鸭腿" to "鸭肉", "鸭脖" to "鸭肉", "鸭架" to "鸭肉",
        "大虾" to "虾", "基围虾" to "虾", "河虾" to "虾", "明虾" to "虾",
        "海米" to "虾", "虾皮" to "虾",
        "鲫鱼汤" to "鲫鱼", "草鱼片" to "草鱼", "鲈鱼片" to "鲈鱼",
        "三文鱼片" to "三文鱼", "生鱼片" to "三文鱼",
        "鸡蛋液" to "鸡蛋", "煎蛋" to "鸡蛋", "水煮蛋" to "鸡蛋", "荷包蛋" to "鸡蛋",
        "鸭蛋液" to "鸭蛋", "咸鸭蛋" to "鸭蛋",
        "牛奶片" to "牛奶", "纯牛奶" to "牛奶", "鲜奶" to "牛奶",
        "无糖酸奶" to "酸奶", "老酸奶" to "酸奶", "希腊酸奶" to "酸奶",
        "白米饭" to "大米", "米饭" to "大米", "香米" to "大米", "糙米" to "大米",
        "小米粥" to "小米", "玉米粒" to "玉米", "甜玉米" to "玉米",
        "馒头片" to "馒头", "花卷" to "馒头", "包子" to "馒头", "饺子皮" to "饺子",
        "面条丝" to "面条", "挂面" to "面条", "拉面" to "面条", "米线" to "面条",
        "豆腐脑" to "豆腐", "嫩豆腐" to "豆腐", "老豆腐" to "豆腐", "豆皮" to "豆腐",
        "香干" to "豆干", "豆腐干" to "豆干", "素鸡" to "豆干",
        "花生米" to "花生", "核桃仁" to "核桃",
        "青菜" to "油菜", "小白菜" to "白菜", "大白菜" to "白菜", "娃娃菜" to "白菜",
        "西红柿" to "番茄", "圣女果" to "番茄", "小番茄" to "番茄",
        "土豆丝" to "土豆", "马铃薯" to "土豆", "洋芋" to "土豆",
        "黄瓜条" to "黄瓜", "青瓜" to "黄瓜",
        "茄子条" to "茄子",
        "西兰花朵" to "西兰花", "花椰菜" to "西兰花", "西蓝花" to "西兰花",
        "豆角丝" to "豆角", "四季豆" to "豆角", "扁豆" to "豆角",
        "山药片" to "山药",
        "藕片" to "莲藕",
        "苦瓜片" to "苦瓜",
        "南瓜块" to "南瓜",
        "芹菜段" to "芹菜",
        "香菜段" to "香菜",
        "韭黄" to "韭菜",
        "蒜苔" to "蒜苗",
        "豆芽菜" to "豆芽",
        "竹笋片" to "竹笋",
        "苹果块" to "苹果", "香蕉段" to "香蕉", "橙瓣" to "橙子",
        "葡萄串" to "葡萄", "西瓜块" to "西瓜", "草莓颗" to "草莓",
        "火龙果块" to "火龙果", "猕猴桃片" to "猕猴桃", "蓝莓粒" to "蓝莓",
        "樱桃颗" to "樱桃", "桃块" to "桃子", "梨块" to "梨", "柚子瓣" to "柚子",
        "芒果块" to "芒果",
        "食用油" to "食用油", "花生油" to "食用油", "菜籽油" to "食用油",
        "色拉油" to "食用油", "大豆油" to "食用油", "调和油" to "食用油", "橄榄油" to "食用油",
        "生抽" to "酱油", "老抽" to "酱油", "味极鲜" to "酱油",
        "米醋" to "醋", "陈醋" to "醋", "白醋" to "醋",
        "白砂糖" to "白糖", "砂糖" to "白糖", "绵白糖" to "白糖",
    )

    /** 带别名和模糊匹配的查找 */
    fun findSmart(name: String): Map<String, Double>? {
        val direct = find(name)
        if (direct != null) return direct
        val n = name.trim()
        if (n.isEmpty()) return null
        // 精确别名
        ALIAS[n]?.let { DB[it]?.let { m -> return m } }
        // 别名包含
        ALIAS.forEach { (alias, std) ->
            if (n.contains(alias)) return DB[std]
        }
        // 库内名字互相包含（取最长匹配，避免「白菜」被「菜」类干扰）
        var best: Map<String, Double>? = null
        var bestLen = 0
        DB.keys.forEach { k ->
            if (n.contains(k) && k.length > bestLen) { best = DB[k]; bestLen = k.length }
        }
        return best
    }

    /** 模糊匹配到的标准名（用于 UI 提示） */
    fun matchName(name: String): String? {
        val n = name.trim()
        if (n.isEmpty()) return null
        ALIAS[n]?.let { return it }
        ALIAS.forEach { (alias, std) -> if (n.contains(alias)) return std }
        var best: String? = null
        var bestLen = 0
        DB.keys.forEach { k -> if (n.contains(k) && k.length > bestLen) { best = k; bestLen = k.length } }
        return best
    }
}"""
src = sub(src, old, new, "Store NutritionDB alias")

# --- 4) 分类估算兜底表 ---
old = """/* ---------------- 营养引擎 ---------------- */
object Nutrition {"""
new = """/* ---------------- 分类估算（表外食物兜底） ----------------
   表里查不到、又没手填营养的食物，按分类给一个粗略均值，
   保证「记录了就能参与计算」，并在 UI 上明确标注是估算值。 */
object Estimate {
    /** 分类 → 每 100g 的营养估值 */
    val BY_CATEGORY: Map<String, Map<String, Double>> = mapOf(
        "蔬菜" to mapOf("calories" to 25.0, "protein" to 1.5, "fat" to 0.3, "carbs" to 4.5, "fiber" to 1.5, "vitaminC" to 30.0, "calcium" to 50.0, "iron" to 1.2),
        "水果" to mapOf("calories" to 52.0, "protein" to 0.5, "fat" to 0.2, "carbs" to 13.0, "fiber" to 1.2, "vitaminC" to 20.0, "calcium" to 6.0, "iron" to 0.4),
        "肉类" to mapOf("calories" to 190.0, "protein" to 18.0, "fat" to 12.0, "carbs" to 0.5, "fiber" to 0.0, "vitaminC" to 0.0, "calcium" to 8.0, "iron" to 2.0),
        "海鲜" to mapOf("calories" to 95.0, "protein" to 17.5, "fat" to 2.0, "carbs" to 0.8, "fiber" to 0.0, "vitaminC" to 0.0, "calcium" to 80.0, "iron" to 1.6),
        "蛋奶" to mapOf("calories" to 90.0, "protein" to 7.5, "fat" to 5.5, "carbs" to 2.8, "fiber" to 0.0, "vitaminC" to 1.0, "calcium" to 90.0, "iron" to 1.2),
        "主食" to mapOf("calories" to 150.0, "protein" to 3.5, "fat" to 0.6, "carbs" to 31.0, "fiber" to 0.8, "vitaminC" to 0.0, "calcium" to 12.0, "iron" to 0.7),
        "调料" to mapOf("calories" to 60.0, "protein" to 2.0, "fat" to 1.0, "carbs" to 10.0, "fiber" to 0.5, "vitaminC" to 2.0, "calcium" to 40.0, "iron" to 3.0),
        "零食" to mapOf("calories" to 480.0, "protein" to 6.0, "fat" to 24.0, "carbs" to 60.0, "fiber" to 2.5, "vitaminC" to 2.0, "calcium" to 60.0, "iron" to 2.5),
        "其他" to mapOf("calories" to 100.0, "protein" to 2.5, "fat" to 2.0, "carbs" to 16.0, "fiber" to 0.8, "vitaminC" to 3.0, "calcium" to 20.0, "iron" to 1.0),
    )

    fun forCategory(category: String): Map<String, Double> =
        BY_CATEGORY[category] ?: BY_CATEGORY["其他"]!!
}

/* ---------------- 营养引擎 ---------------- */
object Nutrition {"""
src = sub(src, old, new, "Store Estimate object")

# --- 5) Result 增加来源标记 ---
old = """    data class Result(
        val total: Map<String, Double>,
        val details: List<Pair<String, Double>>,   // (name, weight)
        val unmatched: List<Pair<String, String>>, // (name, reason)
        val assessment: List<NutrientAssess>,
    )"""
new = """    /** 单条食材的计算结果 */
    data class Item(
        val name: String,
        val weight: Double,
        val source: String,   // "exact" 库内精确 / "alias" 别名命中 / "custom" 用户自填 / "estimate" 分类估算
        val matchedAs: String?,  // 命中的标准名
    ) {
        val isEstimated: Boolean get() = source == "estimate"
    }

    data class Result(
        val total: Map<String, Double>,
        val details: List<Pair<String, Double>>,   // (name, weight)
        val unmatched: List<Pair<String, String>>, // (name, reason)
        val assessment: List<NutrientAssess>,
        val items: List<Item> = emptyList(),
        val estimatedCount: Int = 0,
    )"""
src = sub(src, old, new, "Store Result items")

# --- 6) assess 重写：支持自定义库 + 分类估算 ---
old = """    fun assess(purchases: List<Purchase>, family: Family): Result {
        val total = mutableMapOf<String, Double>().apply { KEYS.forEach { put(it, 0.0) } }
        val details = mutableListOf<Pair<String, Double>>()
        val unmatched = mutableListOf<Pair<String, String>>()
        for (p in purchases) {
            val n = NutritionDB.find(p.name)
            val w = parseWeight(p)
            if (n == null) { unmatched.add(p.name to "不在食材库"); continue }
            if (w <= 0) { unmatched.add(p.name to "未填重量"); continue }
            val ratio = w / 100.0
            for (k in KEYS) total[k] = (total[k] ?: 0.0) + (n[k] ?: 0.0) * ratio
            details.add(p.name to w)
        }"""
new = """    /** 解析单条记录的营养来源 */
    fun resolve(p: Purchase, customFoods: List<CustomFood>): Triple<Map<String, Double>?, String, String?>? {
        val std = NutritionDB.matchName(p.name)
        if (std != null) {
            val m = NutritionDB.DB[std]
            if (m != null) {
                val exact = NutritionDB.find(p.name) != null
                return Triple(m, if (exact) "exact" else "alias", std)
            }
        }
        // 用户自填
        val cf = customFoods.firstOrNull { it.name.trim() == p.name.trim() }
        if (cf != null) return Triple(cf.toMap(), "custom", cf.name)
        return null
    }

    fun assess(purchases: List<Purchase>, family: Family, customFoods: List<CustomFood> = emptyList()): Result {
        val total = mutableMapOf<String, Double>().apply { KEYS.forEach { put(it, 0.0) } }
        val details = mutableListOf<Pair<String, Double>>()
        val unmatched = mutableListOf<Pair<String, String>>()
        val items = mutableListOf<Item>()
        for (p in purchases) {
            val w = parseWeight(p)
            if (w <= 0) { unmatched.add(p.name to "未填重量"); continue }
            val resolved = resolve(p, customFoods)
            val n: Map<String, Double>
            val source: String
            val matchedAs: String?
            if (resolved != null) {
                n = resolved.first
                source = resolved.second
                matchedAs = resolved.third
            } else {
                // 表外且未自填 → 按分类估算
                n = Estimate.forCategory(p.category)
                source = "estimate"
                matchedAs = null
            }
            val ratio = w / 100.0
            for (k in KEYS) total[k] = (total[k] ?: 0.0) + (n[k] ?: 0.0) * ratio
            details.add(p.name to w)
            items.add(Item(p.name, w, source, matchedAs))
        }"""
src = sub(src, old, new, "Store assess rewrite")

# --- 7) 返回 Result 时带上 items/estimatedCount ---
old = """        return Result(rounded, details, unmatched, assessment)
    }
}"""
new = """        return Result(
            rounded, details, unmatched, assessment,
            items = items,
            estimatedCount = items.count { it.isEstimated },
        )
    }
}"""
src = sub(src, old, new, "Store Result return")

write("Store.kt", src)

print("STORE PATCHED")

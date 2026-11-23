package com.guoziri.app

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/* ---------------- 数据模型（与旧 JSON 结构兼容） ---------------- */
data class Purchase(
    val id: String,
    val name: String,
    val category: String,
    val pricePerJin: Double? = null,
    val totalCost: Double? = null,
    val totalJin: Double? = null,
    val weightGrams: Double? = null,
    val date: String,
    val note: String? = null,
    val createdAt: String,
)

data class DiaryEntry(
    val id: String,
    val content: String,
    val date: String,
    val timeStart: String? = null,
    val timeEnd: String? = null,
    val time: String? = null,
    val createdAt: String,
)

data class Family(val adults: Int = 2, val children: Int = 1)

data class Settings(
    val sortOrder: String = "desc",
    val family: Family = Family(),
    val hiddenEnabled: Boolean = false,
    val theme: String = "auto",
    val apiProvider: String = "deepseek",
    val apiBaseUrl: String = "https://api.deepseek.com/v1",
    val apiKey: String = "",
    val apiModel: String = "deepseek-chat",
    val guideDone: Boolean = false,
    val customFoods: List<CustomFood> = emptyList(),
) {
    val hasKey: Boolean get() = apiKey.isNotBlank()
}

/* 营养项评估结果 */
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

/* ---------------- 食物营养库（每 100g） ---------------- */


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
object Nutrition {
    val KEYS = listOf("calories", "protein", "fat", "carbs", "fiber", "vitaminC", "calcium", "iron")
    val LABELS = mapOf(
        "calories" to ("热量" to "kcal"), "protein" to ("蛋白质" to "g"), "fat" to ("脂肪" to "g"),
        "carbs" to ("碳水" to "g"), "fiber" to ("膳食纤维" to "g"), "vitaminC" to ("维生素C" to "mg"),
        "calcium" to ("钙" to "mg"), "iron" to ("铁" to "mg"),
    )
    private val ADULT = mapOf("calories" to 1800.0, "protein" to 55.0, "fat" to 55.0, "carbs" to 240.0, "fiber" to 25.0, "vitaminC" to 100.0, "calcium" to 800.0, "iron" to 20.0)
    private val CHILD = mapOf("calories" to 1350.0, "protein" to 30.0, "fat" to 40.0, "carbs" to 180.0, "fiber" to 14.0, "vitaminC" to 60.0, "calcium" to 600.0, "iron" to 12.0)

    /** 单条食材的计算结果 */
    data class Item(
        val name: String,
        val weight: Double,
        val source: String,   // "exact" 库内精确 / "alias" 别名命中 / "custom" 用户自填 / "estimate" 分类估算
        val matchedAs: String?,  // 命中的标准名
    ) {
        val isEstimated: Boolean get() = source == "estimate"
    }

    /** 营养来源解析结果 */
    data class Resolved(
        val nutrients: Map<String, Double>,
        val source: String,        // exact / alias / custom
        val matchedAs: String?,
    )

    data class Result(
        val total: Map<String, Double>,
        val details: List<Pair<String, Double>>,   // (name, weight)
        val unmatched: List<Pair<String, String>>, // (name, reason)
        val assessment: List<NutrientAssess>,
        val items: List<Item> = emptyList(),
        val estimatedCount: Int = 0,
    )

    fun parseWeight(p: Purchase): Double = when {
        p.weightGrams != null && p.weightGrams > 0 -> p.weightGrams
        p.pricePerJin != null && p.totalCost != null && p.pricePerJin > 0 -> (p.totalCost / p.pricePerJin) * 500
        p.totalJin != null && p.totalJin > 0 -> p.totalJin * 500
        else -> 0.0
    }

    /** 解析单条记录的营养来源 */
    fun resolve(p: Purchase, customFoods: List<CustomFood>): Resolved? {
        val std = NutritionDB.matchName(p.name)
        if (std != null) {
            val m = NutritionDB.DB[std]
            if (m != null) {
                val exact = NutritionDB.find(p.name) != null
                return Resolved(m, if (exact) "exact" else "alias", std)
            }
        }
        // 用户自填
        val cf = customFoods.firstOrNull { it.name.trim() == p.name.trim() }
        if (cf != null) return Resolved(cf.toMap(), "custom", cf.name)
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
                n = resolved.nutrients
                source = resolved.source
                matchedAs = resolved.matchedAs
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
        }
        val rounded = total.mapValues { (_, v) -> Math.round(v * 10.0) / 10.0 }
        val dri = mutableMapOf<String, Double>().apply {
            KEYS.forEach { put(it, 0.0) }
            repeat(family.adults) { KEYS.forEach { k -> put(k, (get(k) ?: 0.0) + ADULT[k]!!) } }
            repeat(family.children) { KEYS.forEach { k -> put(k, (get(k) ?: 0.0) + CHILD[k]!!) } }
        }
        val assessment = KEYS.map { k ->
            val (label, unit) = LABELS[k]!!
            val t = dri[k] ?: 0.0
            NutrientAssess(k, label, unit, rounded[k] ?: 0.0, t, if (t > 0) Math.round((rounded[k] ?: 0.0) / t * 100).toInt() else 0)
        }
        return Result(
            rounded, details, unmatched, assessment,
            items = items,
            estimatedCount = items.count { it.isEstimated },
        )
    }
}

/* ---------------- 本地 JSON 仓库（线程安全） ---------------- */
class StoreRepo(context: Context) {
    private val gson = Gson()
    private val dir: File = context.filesDir
    private val purchasesFile = File(dir, "purchases.json")
    private val diaryFile = File(dir, "diary.json")
    private val settingsFile = File(dir, "settings.json")
    private val lock = Any()

    val purchases = mutableStateListOf<Purchase>()
    val diary = mutableStateListOf<DiaryEntry>()
    var settings by mutableStateOf(Settings())
        private set

    /* ---------------- 沙箱模式（新手引导用） ----------------
       引导期间，purchases/diary 指向内存副本，写真实数据被拦下；
       退出引导时 closeSandbox() 恢复真实数据。 */
    var sandboxActive by mutableStateOf(false)
        private set
    private var realPurchases: List<Purchase> = emptyList()
    private var realDiary: List<DiaryEntry> = emptyList()

    init {
        load()
    }

    fun openSandbox() {
        if (sandboxActive) return
        sandboxActive = true
        realPurchases = purchases.toList()
        realDiary = diary.toList()
        purchases.clear()
        diary.clear()
    }

    fun closeSandbox() {
        if (!sandboxActive) return
        sandboxActive = false
        purchases.clear()
        purchases.addAll(realPurchases)
        diary.clear()
        diary.addAll(realDiary)
        realPurchases = emptyList()
        realDiary = emptyList()
    }

    /** 沙箱里的演示数据 */
    fun loadSandboxDemo() {
        val t = today()
        purchases.clear()
        purchases.addAll(listOf(
            Purchase("gd1", "西兰花", "蔬菜", 6.5, 9.8, 1.5, null, t, null, nowIso()),
            Purchase("gd2", "鸡蛋", "蛋奶", 5.8, 11.6, 2.0, null, t, null, nowIso()),
            Purchase("gd3", "鲈鱼", "海鲜", 18.0, 54.0, 3.0, null, t, null, nowIso()),
        ))
        diary.clear()
        diary.addAll(listOf(
            DiaryEntry("gd4", "引导示例：买完菜、做完饭，随手把一天记下来。", t, "18:00", "18:30", "18:00", nowIso()),
        ))
    }

    private fun readList(file: File, type: java.lang.reflect.Type): List<*> {
        if (!file.exists()) return emptyList<Any>()
        return try { gson.fromJson(file.readText(StandardCharsets.UTF_8), type) ?: emptyList<Any>() } catch (e: Exception) { emptyList<Any>() }
    }

    fun load() {
        synchronized(lock) {
            val pType = object : TypeToken<List<Purchase>>() {}.type
            val dType = object : TypeToken<List<DiaryEntry>>() {}.type
            val sType = object : TypeToken<Settings>() {}.type
            purchases.clear()
            (readList(purchasesFile, pType) as List<*>).forEach { if (it is Purchase) purchases.add(it) }
            diary.clear()
            (readList(diaryFile, dType) as List<*>).forEach { if (it is DiaryEntry) diary.add(it) }
            settings = try {
                if (settingsFile.exists()) gson.fromJson(settingsFile.readText(StandardCharsets.UTF_8), sType) ?: Settings() else Settings()
            } catch (e: Exception) { Settings() }
        }
    }

    private fun writePurchases() { synchronized(lock) { purchasesFile.writeText(gson.toJson(purchases.toList()), StandardCharsets.UTF_8) } }
    private fun writeDiary() { synchronized(lock) { diaryFile.writeText(gson.toJson(diary.toList()), StandardCharsets.UTF_8) } }
    private fun writeSettings() { synchronized(lock) { settingsFile.writeText(gson.toJson(settings), StandardCharsets.UTF_8) } }

    fun newId() = System.currentTimeMillis().toString()
    fun nowIso(): String = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(Date())

    fun addPurchase(p: Purchase) {
        synchronized(lock) {
            purchases.add(0, p)
            if (!sandboxActive) writePurchases()
        }
    }
    fun updatePurchase(id: String, updated: Purchase) {
        synchronized(lock) {
            val i = purchases.indexOfFirst { it.id == id }
            if (i >= 0) {
                purchases[i] = updated
                if (!sandboxActive) writePurchases()
            }
        }
    }
    fun deletePurchase(id: String) {
        synchronized(lock) {
            purchases.removeAll { it.id == id }
            if (!sandboxActive) writePurchases()
        }
    }

    fun addDiary(d: DiaryEntry) {
        synchronized(lock) {
            diary.add(0, d)
            if (!sandboxActive) writeDiary()
        }
    }
    fun deleteDiary(id: String) {
        synchronized(lock) {
            diary.removeAll { it.id == id }
            if (!sandboxActive) writeDiary()
        }
    }

    fun updateSettings(patch: (Settings) -> Settings) {
        synchronized(lock) { settings = patch(settings); writeSettings() }
        ThemePrefs.theme.value = settings.theme
    }

    /* ---------------- 自定义食物 ---------------- */
    fun addCustomFood(f: CustomFood) {
        updateSettings { s ->
            val list = s.customFoods.filterNot { it.name.trim() == f.name.trim() }
            s.copy(customFoods = list + f.copy(createdAt = nowIso()))
        }
    }

    fun removeCustomFood(name: String) {
        updateSettings { s -> s.copy(customFoods = s.customFoods.filterNot { it.name.trim() == name.trim() }) }
    }

    fun findCustomFood(name: String): CustomFood? =
        settings.customFoods.firstOrNull { it.name.trim() == name.trim() }

    fun clearAll() {
        synchronized(lock) {
            purchases.clear(); diary.clear(); writePurchases(); writeDiary()
        }
    }

    fun exportJson(): String = gson.toJson(mapOf(
        "purchases" to purchases.toList(),
        "diary" to diary.toList(),
        "family" to settings.family,
    ))


/* ---------------- 食物营养库别名映射（兼容旧版 findSmart/matchName） ---------------- */
object NutritionDB {
    // 仅提供别名映射，供旧版 findSmart/matchName 兼容使用
    val ALIAS: Map<String, String> = mapOf(
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
    )
}

}
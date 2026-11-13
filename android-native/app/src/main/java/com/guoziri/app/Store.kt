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
object NutritionDB {
    val DB: Map<String, Map<String, Double>> = mapOf(
        "西兰花" to mapOf("calories" to 33.0, "protein" to 4.1, "fat" to 0.5, "carbs" to 4.8, "fiber" to 1.6, "vitaminC" to 56.0, "calcium" to 67.0, "iron" to 1.2),
        "菠菜" to mapOf("calories" to 28.0, "protein" to 2.9, "fat" to 0.3, "carbs" to 3.5, "fiber" to 1.7, "vitaminC" to 32.0, "calcium" to 66.0, "iron" to 2.9),
        "油菜" to mapOf("calories" to 25.0, "protein" to 1.8, "fat" to 0.3, "carbs" to 3.2, "fiber" to 1.1, "vitaminC" to 40.0, "calcium" to 153.0, "iron" to 1.1),
        "青椒" to mapOf("calories" to 23.0, "protein" to 0.9, "fat" to 0.2, "carbs" to 4.5, "fiber" to 1.3, "vitaminC" to 72.0, "calcium" to 11.0, "iron" to 0.5),
        "红椒" to mapOf("calories" to 26.0, "protein" to 1.0, "fat" to 0.2, "carbs" to 5.0, "fiber" to 1.4, "vitaminC" to 130.0, "calcium" to 12.0, "iron" to 0.6),
        "胡萝卜" to mapOf("calories" to 37.0, "protein" to 0.9, "fat" to 0.2, "carbs" to 8.1, "fiber" to 1.4, "vitaminC" to 6.0, "calcium" to 33.0, "iron" to 0.6),
        "土豆" to mapOf("calories" to 81.0, "protein" to 2.0, "fat" to 0.1, "carbs" to 17.8, "fiber" to 1.0, "vitaminC" to 9.0, "calcium" to 6.0, "iron" to 0.4),
        "番茄" to mapOf("calories" to 19.0, "protein" to 0.9, "fat" to 0.2, "carbs" to 3.5, "fiber" to 0.6, "vitaminC" to 14.0, "calcium" to 5.0, "iron" to 0.3),
        "黄瓜" to mapOf("calories" to 15.0, "protein" to 0.7, "fat" to 0.1, "carbs" to 3.1, "fiber" to 0.5, "vitaminC" to 8.0, "calcium" to 16.0, "iron" to 0.3),
        "茄子" to mapOf("calories" to 23.0, "protein" to 0.8, "fat" to 0.1, "carbs" to 4.9, "fiber" to 1.0, "vitaminC" to 5.0, "calcium" to 9.0, "iron" to 0.4),
        "白菜" to mapOf("calories" to 17.0, "protein" to 1.1, "fat" to 0.2, "carbs" to 2.5, "fiber" to 0.6, "vitaminC" to 27.0, "calcium" to 54.0, "iron" to 0.4),
        "生菜" to mapOf("calories" to 14.0, "protein" to 1.0, "fat" to 0.2, "carbs" to 2.0, "fiber" to 0.7, "vitaminC" to 15.0, "calcium" to 36.0, "iron" to 0.5),
        "豆角" to mapOf("calories" to 31.0, "protein" to 2.4, "fat" to 0.2, "carbs" to 5.2, "fiber" to 2.1, "vitaminC" to 18.0, "calcium" to 46.0, "iron" to 1.0),
        "豌豆" to mapOf("calories" to 76.0, "protein" to 5.4, "fat" to 0.4, "carbs" to 13.5, "fiber" to 5.5, "vitaminC" to 20.0, "calcium" to 25.0, "iron" to 1.5),
        "玉米" to mapOf("calories" to 106.0, "protein" to 3.4, "fat" to 1.2, "carbs" to 22.8, "fiber" to 2.9, "vitaminC" to 8.0, "calcium" to 2.0, "iron" to 0.5),
        "红薯" to mapOf("calories" to 102.0, "protein" to 1.3, "fat" to 0.2, "carbs" to 23.1, "fiber" to 1.6, "vitaminC" to 16.0, "calcium" to 18.0, "iron" to 0.5),
        "山药" to mapOf("calories" to 56.0, "protein" to 1.9, "fat" to 0.1, "carbs" to 12.4, "fiber" to 0.8, "vitaminC" to 10.0, "calcium" to 12.0, "iron" to 0.5),
        "莲藕" to mapOf("calories" to 46.0, "protein" to 1.2, "fat" to 0.1, "carbs" to 10.2, "fiber" to 1.0, "vitaminC" to 19.0, "calcium" to 16.0, "iron" to 0.4),
        "冬瓜" to mapOf("calories" to 11.0, "protein" to 0.4, "fat" to 0.1, "carbs" to 2.4, "fiber" to 0.6, "vitaminC" to 10.0, "calcium" to 14.0, "iron" to 0.2),
        "南瓜" to mapOf("calories" to 22.0, "protein" to 0.7, "fat" to 0.1, "carbs" to 4.8, "fiber" to 0.8, "vitaminC" to 9.0, "calcium" to 14.0, "iron" to 0.4),
        "苦瓜" to mapOf("calories" to 19.0, "protein" to 0.8, "fat" to 0.2, "carbs" to 3.5, "fiber" to 0.9, "vitaminC" to 56.0, "calcium" to 14.0, "iron" to 0.4),
        "芹菜" to mapOf("calories" to 15.0, "protein" to 0.6, "fat" to 0.1, "carbs" to 2.6, "fiber" to 1.4, "vitaminC" to 8.0, "calcium" to 120.0, "iron" to 0.8),
        "香菜" to mapOf("calories" to 28.0, "protein" to 2.3, "fat" to 0.4, "carbs" to 4.5, "fiber" to 1.2, "vitaminC" to 48.0, "calcium" to 101.0, "iron" to 2.1),
        "韭菜" to mapOf("calories" to 26.0, "protein" to 2.4, "fat" to 0.3, "carbs" to 3.8, "fiber" to 1.4, "vitaminC" to 26.0, "calcium" to 59.0, "iron" to 1.6),
        "蒜苗" to mapOf("calories" to 34.0, "protein" to 2.1, "fat" to 0.3, "carbs" to 5.7, "fiber" to 1.2, "vitaminC" to 31.0, "calcium" to 34.0, "iron" to 0.8),
        "豆芽" to mapOf("calories" to 18.0, "protein" to 2.4, "fat" to 0.2, "carbs" to 2.2, "fiber" to 1.0, "vitaminC" to 12.0, "calcium" to 21.0, "iron" to 0.6),
        "竹笋" to mapOf("calories" to 19.0, "protein" to 2.2, "fat" to 0.2, "carbs" to 3.0, "fiber" to 1.4, "vitaminC" to 11.0, "calcium" to 13.0, "iron" to 0.7),
        "苹果" to mapOf("calories" to 53.0, "protein" to 0.3, "fat" to 0.2, "carbs" to 13.5, "fiber" to 1.2, "vitaminC" to 4.0, "calcium" to 4.0, "iron" to 0.3),
        "香蕉" to mapOf("calories" to 91.0, "protein" to 1.2, "fat" to 0.2, "carbs" to 20.5, "fiber" to 1.1, "vitaminC" to 8.0, "calcium" to 5.0, "iron" to 0.4),
        "橙子" to mapOf("calories" to 47.0, "protein" to 0.8, "fat" to 0.2, "carbs" to 10.5, "fiber" to 0.7, "vitaminC" to 33.0, "calcium" to 20.0, "iron" to 0.2),
        "葡萄" to mapOf("calories" to 60.0, "protein" to 0.4, "fat" to 0.1, "carbs" to 14.8, "fiber" to 0.4, "vitaminC" to 4.0, "calcium" to 8.0, "iron" to 0.3),
        "西瓜" to mapOf("calories" to 31.0, "protein" to 0.6, "fat" to 0.1, "carbs" to 6.8, "fiber" to 0.2, "vitaminC" to 6.0, "calcium" to 4.0, "iron" to 0.2),
        "草莓" to mapOf("calories" to 32.0, "protein" to 0.8, "fat" to 0.2, "carbs" to 6.0, "fiber" to 1.1, "vitaminC" to 62.0, "calcium" to 18.0, "iron" to 0.5),
        "火龙果" to mapOf("calories" to 51.0, "protein" to 0.6, "fat" to 0.1, "carbs" to 12.2, "fiber" to 1.4, "vitaminC" to 4.0, "calcium" to 8.0, "iron" to 0.3),
        "猕猴桃" to mapOf("calories" to 56.0, "protein" to 1.0, "fat" to 0.6, "carbs" to 12.2, "fiber" to 2.6, "vitaminC" to 62.0, "calcium" to 26.0, "iron" to 0.6),
        "蓝莓" to mapOf("calories" to 57.0, "protein" to 0.7, "fat" to 0.3, "carbs" to 14.5, "fiber" to 2.4, "vitaminC" to 9.0, "calcium" to 6.0, "iron" to 0.3),
        "樱桃" to mapOf("calories" to 46.0, "protein" to 0.9, "fat" to 0.1, "carbs" to 10.6, "fiber" to 0.5, "vitaminC" to 10.0, "calcium" to 12.0, "iron" to 0.4),
        "桃子" to mapOf("calories" to 42.0, "protein" to 0.8, "fat" to 0.1, "carbs" to 9.5, "fiber" to 1.0, "vitaminC" to 7.0, "calcium" to 4.0, "iron" to 0.4),
        "梨" to mapOf("calories" to 42.0, "protein" to 0.3, "fat" to 0.1, "carbs" to 10.1, "fiber" to 1.0, "vitaminC" to 4.0, "calcium" to 4.0, "iron" to 0.2),
        "柚子" to mapOf("calories" to 41.0, "protein" to 0.7, "fat" to 0.1, "carbs" to 9.1, "fiber" to 0.6, "vitaminC" to 23.0, "calcium" to 15.0, "iron" to 0.2),
        "芒果" to mapOf("calories" to 65.0, "protein" to 0.6, "fat" to 0.3, "carbs" to 15.3, "fiber" to 1.3, "vitaminC" to 23.0, "calcium" to 10.0, "iron" to 0.3),
        "榴莲" to mapOf("calories" to 147.0, "protein" to 2.6, "fat" to 5.3, "carbs" to 25.3, "fiber" to 1.7, "vitaminC" to 2.0, "calcium" to 6.0, "iron" to 0.4),
        "猪肉" to mapOf("calories" to 395.0, "protein" to 13.2, "fat" to 37.0, "carbs" to 0.0, "fiber" to 0.0, "vitaminC" to 0.0, "calcium" to 6.0, "iron" to 1.3),
        "猪里脊" to mapOf("calories" to 143.0, "protein" to 20.3, "fat" to 6.2, "carbs" to 0.0, "fiber" to 0.0, "vitaminC" to 0.0, "calcium" to 4.0, "iron" to 2.4),
        "牛肉" to mapOf("calories" to 125.0, "protein" to 20.2, "fat" to 4.2, "carbs" to 0.0, "fiber" to 0.0, "vitaminC" to 0.0, "calcium" to 4.0, "iron" to 3.2),
        "羊肉" to mapOf("calories" to 203.0, "protein" to 17.8, "fat" to 14.1, "carbs" to 0.0, "fiber" to 0.0, "vitaminC" to 0.0, "calcium" to 7.0, "iron" to 2.0),
        "鸡肉" to mapOf("calories" to 167.0, "protein" to 19.3, "fat" to 9.4, "carbs" to 0.0, "fiber" to 0.0, "vitaminC" to 0.0, "calcium" to 11.0, "iron" to 0.9),
        "鸡胸肉" to mapOf("calories" to 133.0, "protein" to 21.5, "fat" to 4.8, "carbs" to 0.0, "fiber" to 0.0, "vitaminC" to 0.0, "calcium" to 11.0, "iron" to 0.7),
        "鸭肉" to mapOf("calories" to 240.0, "protein" to 16.5, "fat" to 19.7, "carbs" to 0.0, "fiber" to 0.0, "vitaminC" to 0.0, "calcium" to 11.0, "iron" to 2.4),
        "草鱼" to mapOf("calories" to 113.0, "protein" to 17.9, "fat" to 4.1, "carbs" to 0.5, "fiber" to 0.0, "vitaminC" to 0.0, "calcium" to 38.0, "iron" to 0.7),
        "鲤鱼" to mapOf("calories" to 109.0, "protein" to 17.1, "fat" to 4.1, "carbs" to 1.3, "fiber" to 0.0, "vitaminC" to 0.0, "calcium" to 55.0, "iron" to 0.9),
        "鲫鱼" to mapOf("calories" to 101.0, "protein" to 16.9, "fat" to 3.2, "carbs" to 0.6, "fiber" to 0.0, "vitaminC" to 0.0, "calcium" to 58.0, "iron" to 0.8),
        "三文鱼" to mapOf("calories" to 179.0, "protein" to 19.3, "fat" to 11.0, "carbs" to 0.0, "fiber" to 0.0, "vitaminC" to 0.0, "calcium" to 8.0, "iron" to 0.3),
        "鳕鱼" to mapOf("calories" to 75.0, "protein" to 16.5, "fat" to 0.5, "carbs" to 0.0, "fiber" to 0.0, "vitaminC" to 0.0, "calcium" to 13.0, "iron" to 0.2),
        "带鱼" to mapOf("calories" to 127.0, "protein" to 18.1, "fat" to 5.3, "carbs" to 0.1, "fiber" to 0.0, "vitaminC" to 0.0, "calcium" to 81.0, "iron" to 1.0),
        "鲈鱼" to mapOf("calories" to 97.0, "protein" to 18.6, "fat" to 2.2, "carbs" to 0.0, "fiber" to 0.0, "vitaminC" to 0.0, "calcium" to 17.0, "iron" to 0.4),
        "虾" to mapOf("calories" to 93.0, "protein" to 18.6, "fat" to 0.8, "carbs" to 1.3, "fiber" to 0.0, "vitaminC" to 0.0, "calcium" to 129.0, "iron" to 1.1),
        "蟹" to mapOf("calories" to 103.0, "protein" to 17.5, "fat" to 2.6, "carbs" to 2.3, "fiber" to 0.0, "vitaminC" to 0.0, "calcium" to 102.0, "iron" to 1.6),
        "扇贝" to mapOf("calories" to 75.0, "protein" to 14.1, "fat" to 0.3, "carbs" to 3.2, "fiber" to 0.0, "vitaminC" to 0.0, "calcium" to 28.0, "iron" to 2.6),
        "蛤蜊" to mapOf("calories" to 57.0, "protein" to 10.0, "fat" to 0.8, "carbs" to 2.5, "fiber" to 0.0, "vitaminC" to 0.0, "calcium" to 73.0, "iron" to 27.6),
        "牡蛎" to mapOf("calories" to 60.0, "protein" to 7.1, "fat" to 1.5, "carbs" to 3.3, "fiber" to 0.0, "vitaminC" to 0.0, "calcium" to 101.0, "iron" to 6.1),
        "鱿鱼" to mapOf("calories" to 75.0, "protein" to 15.3, "fat" to 0.9, "carbs" to 1.1, "fiber" to 0.0, "vitaminC" to 0.0, "calcium" to 21.0, "iron" to 0.8),
        "鸡蛋" to mapOf("calories" to 144.0, "protein" to 13.3, "fat" to 8.8, "carbs" to 2.8, "fiber" to 0.0, "vitaminC" to 0.0, "calcium" to 56.0, "iron" to 1.8),
        "鸭蛋" to mapOf("calories" to 180.0, "protein" to 13.5, "fat" to 13.3, "carbs" to 2.5, "fiber" to 0.0, "vitaminC" to 0.0, "calcium" to 64.0, "iron" to 3.8),
        "鹌鹑蛋" to mapOf("calories" to 158.0, "protein" to 12.7, "fat" to 11.1, "carbs" to 1.7, "fiber" to 0.0, "vitaminC" to 0.0, "calcium" to 61.0, "iron" to 3.2),
        "牛奶" to mapOf("calories" to 54.0, "protein" to 3.0, "fat" to 3.2, "carbs" to 3.4, "fiber" to 0.0, "vitaminC" to 1.0, "calcium" to 104.0, "iron" to 0.1),
        "酸奶" to mapOf("calories" to 62.0, "protein" to 2.9, "fat" to 2.8, "carbs" to 5.6, "fiber" to 0.0, "vitaminC" to 1.0, "calcium" to 118.0, "iron" to 0.1),
        "奶酪" to mapOf("calories" to 328.0, "protein" to 24.9, "fat" to 26.0, "carbs" to 0.8, "fiber" to 0.0, "vitaminC" to 0.0, "calcium" to 659.0, "iron" to 0.2),
        "大米" to mapOf("calories" to 116.0, "protein" to 2.6, "fat" to 0.3, "carbs" to 25.9, "fiber" to 0.3, "vitaminC" to 0.0, "calcium" to 7.0, "iron" to 0.4),
        "小米" to mapOf("calories" to 119.0, "protein" to 3.4, "fat" to 1.2, "carbs" to 24.6, "fiber" to 1.0, "vitaminC" to 0.0, "calcium" to 14.0, "iron" to 1.5),
        "馒头" to mapOf("calories" to 223.0, "protein" to 7.6, "fat" to 1.1, "carbs" to 46.9, "fiber" to 1.0, "vitaminC" to 0.0, "calcium" to 18.0, "iron" to 1.0),
        "面条" to mapOf("calories" to 136.0, "protein" to 4.2, "fat" to 0.5, "carbs" to 28.4, "fiber" to 1.0, "vitaminC" to 0.0, "calcium" to 11.0, "iron" to 0.8),
        "饺子" to mapOf("calories" to 200.0, "protein" to 7.0, "fat" to 6.0, "carbs" to 28.0, "fiber" to 1.2, "vitaminC" to 2.0, "calcium" to 15.0, "iron" to 1.2),
        "豆腐" to mapOf("calories" to 81.0, "protein" to 8.2, "fat" to 4.8, "carbs" to 1.2, "fiber" to 0.3, "vitaminC" to 0.0, "calcium" to 164.0, "iron" to 2.6),
        "豆干" to mapOf("calories" to 158.0, "protein" to 15.3, "fat" to 8.8, "carbs" to 3.2, "fiber" to 0.8, "vitaminC" to 0.0, "calcium" to 312.0, "iron" to 4.8),
        "豆浆" to mapOf("calories" to 16.0, "protein" to 1.8, "fat" to 0.7, "carbs" to 1.3, "fiber" to 0.4, "vitaminC" to 0.0, "calcium" to 10.0, "iron" to 0.5),
        "毛豆" to mapOf("calories" to 121.0, "protein" to 13.0, "fat" to 5.5, "carbs" to 8.6, "fiber" to 4.0, "vitaminC" to 27.0, "calcium" to 107.0, "iron" to 3.5),
        "香菇" to mapOf("calories" to 25.0, "protein" to 2.2, "fat" to 0.3, "carbs" to 4.7, "fiber" to 2.6, "vitaminC" to 2.0, "calcium" to 3.0, "iron" to 1.3),
        "木耳" to mapOf("calories" to 28.0, "protein" to 2.6, "fat" to 0.2, "carbs" to 5.1, "fiber" to 2.9, "vitaminC" to 0.0, "calcium" to 45.0, "iron" to 185.0),
        "平菇" to mapOf("calories" to 28.0, "protein" to 2.7, "fat" to 0.4, "carbs" to 4.5, "fiber" to 2.3, "vitaminC" to 2.0, "calcium" to 3.0, "iron" to 1.0),
        "金针菇" to mapOf("calories" to 26.0, "protein" to 2.4, "fat" to 0.3, "carbs" to 4.5, "fiber" to 2.7, "vitaminC" to 2.0, "calcium" to 5.0, "iron" to 1.2),
        "食用油" to mapOf("calories" to 899.0, "protein" to 0.0, "fat" to 99.9, "carbs" to 0.0, "fiber" to 0.0, "vitaminC" to 0.0, "calcium" to 0.0, "iron" to 0.0),
        "酱油" to mapOf("calories" to 52.0, "protein" to 6.8, "fat" to 0.1, "carbs" to 6.8, "fiber" to 0.0, "vitaminC" to 0.0, "calcium" to 18.0, "iron" to 3.2),
        "醋" to mapOf("calories" to 23.0, "protein" to 0.4, "fat" to 0.1, "carbs" to 5.2, "fiber" to 0.0, "vitaminC" to 0.0, "calcium" to 12.0, "iron" to 0.6),
        "白糖" to mapOf("calories" to 398.0, "protein" to 0.0, "fat" to 0.0, "carbs" to 99.5, "fiber" to 0.0, "vitaminC" to 0.0, "calcium" to 1.0, "iron" to 0.1),
        "花生" to mapOf("calories" to 574.0, "protein" to 24.6, "fat" to 44.3, "carbs" to 15.3, "fiber" to 9.0, "vitaminC" to 0.0, "calcium" to 79.0, "iron" to 2.7),
        "核桃" to mapOf("calories" to 654.0, "protein" to 14.9, "fat" to 63.0, "carbs" to 13.7, "fiber" to 6.7, "vitaminC" to 1.0, "calcium" to 56.0, "iron" to 2.1),
    )

    fun find(name: String): Map<String, Double>? {
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
}

/* ---------------- 分类估算（表外食物兜底） ----------------
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
}
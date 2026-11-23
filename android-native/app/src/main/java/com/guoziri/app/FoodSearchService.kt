package com.guoziri.app

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * 食物营养智能填充服务。
 *
 * 三层降级策略（每层都有日志回报给 AgentLogSheet）：
 *  1. 库内匹配  —— NutritionDB 精确/别名/模糊匹配，瞬时完成，数据最可靠
 *  2. AI 联网   —— 调用户配置的 LLM（DeepSeek/GLM/Kimi...）要营养数据，
 *                  相当于带联网知识的助手，覆盖面广
 *  3. 分类估算  —— Estimate.forCategory 给出该分类的平均值，明确标注是估算
 */
object FoodSearchService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)   // AI 生成可能慢
        .build()

    /** 依据设置里的 API 配置构造一个填充任务 */
    fun buildTask(
        foodName: String,
        settings: Settings,
        onLog: (String) -> Unit,
        onProgress: (Int) -> Unit,
        onResult: (CustomFood?, String?) -> Unit,
    ): () -> Unit = {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                onLog("🚀 启动营养查询 Agent（食物：$foodName）")
                onProgress(10)

                // ---------- 层1：库内匹配 ----------
                onLog("📚 第 1 层：查询内置食物库（200+ 种）")
                onProgress(25)
                val matchedName = NutritionDB.matchName(foodName)
                val direct = NutritionDB.find(foodName)
                if (direct != null) {
                    val displayName = matchedName ?: foodName
                    onLog("✅ 库内命中：$displayName")
                    onProgress(100)
                    onResult(foodFromMap(foodName, direct, "exact"), null)
                    return@launch
                }
                onLog("⚠️ 库内未命中，进入第 2 层")

                // ---------- 层2：AI 联网 ----------
                if (settings.hasKey) {
                    onLog("🤖 第 2 层：调用 AI（${settings.apiProvider}）联网查询")
                    onProgress(40)
                    val aiResult = askAI(foodName, settings) { line ->
                        onLog(line)
                    }
                    if (aiResult != null) {
                        onLog("✅ AI 返回营养数据")
                        onProgress(100)
                        onResult(aiResult, null)
                        return@launch
                    }
                    onLog("⚠️ AI 查询失败，进入第 3 层")
                } else {
                    onLog("⚠️ 未配置 AI Key，跳过第 2 层（设置里可配置）")
                }

                // ---------- 层3：分类估算 ----------
                onProgress(80)
                onLog("📊 第 3 层：按分类「估算」营养均值")
                val est = Estimate.forCategory(settingsCategoryOf(foodName))
                onLog("✅ 估算完成（数值是粗略均值，仅供参考）")
                onProgress(100)
                onResult(foodFromMap(foodName, est, "estimate"), null)
            } catch (e: Exception) {
                onLog("❌ 异常：${e.message}")
                onResult(null, e.message)
            }
        }
    }

    private fun settingsCategoryOf(name: String): String {
        // 简单的关键字分类猜测
        return when {
            name.contains("鱼") || name.contains("虾") || name.contains("蟹") || name.contains("贝") -> "海鲜"
            name.contains("猪") || name.contains("牛") || name.contains("羊") || name.contains("鸡") || name.contains("鸭") || name.contains("肉") -> "肉类"
            name.contains("奶") || name.contains("蛋") || name.contains("酪") -> "蛋奶"
            name.contains("米") || name.contains("面") || name.contains("粉") || name.contains("馒头") || name.contains("包") -> "主食"
            name.contains("果") || name.contains("莓") || name.contains("瓜") || name.contains("桃") || name.contains("梨") || name.contains("橘") || name.contains("橙") -> "水果"
            else -> "蔬菜"
        }
    }

    private fun foodFromMap(name: String, m: Map<String, Double>, source: String): CustomFood =
        CustomFood(
            name = name,
            calories = m["calories"] ?: 0.0,
            protein = m["protein"] ?: 0.0,
            fat = m["fat"] ?: 0.0,
            carbs = m["carbs"] ?: 0.0,
            fiber = m["fiber"] ?: 0.0,
            vitaminC = m["vitaminC"] ?: 0.0,
            calcium = m["calcium"] ?: 0.0,
            iron = m["iron"] ?: 0.0,
        )

    /** 把营养 map 转成 UI 表单需要的中文键 */
    fun toFormMap(m: Map<String, Double>): Map<String, Double> = mapOf(
        "热量" to (m["calories"] ?: 0.0),
        "蛋白质" to (m["protein"] ?: 0.0),
        "脂肪" to (m["fat"] ?: 0.0),
        "碳水" to (m["carbs"] ?: 0.0),
        "膳食纤维" to (m["fiber"] ?: 0.0),
        "维生素C" to (m["vitaminC"] ?: 0.0),
        "钙" to (m["calcium"] ?: 0.0),
        "铁" to (m["iron"] ?: 0.0),
    )

    /** 调用 OpenAI 兼容接口问 AI 营养数据 */
    private fun askAI(foodName: String, settings: Settings, onLog: (String) -> Unit): CustomFood? {
        return try {
            val prompt = """你是一个营养数据库。请给出「$foodName」每 100 克可食部分的营养成分。
只输出 JSON，不要任何其他文字，格式：
{"calories":数字,"protein":数字,"fat":数字,"carbs":数字,"fiber":数字,"vitaminC":数字,"calcium":数字,"iron":数字}
单位：calories=kcal，其余分别 g/g/g/g/g/mg/mg/mg。数值为合理均值即可。"""

            onLog("   连接 ${settings.apiBaseUrl.removePrefix("https://").take(30)}…")
            val jsonReq = JSONObject().apply {
                put("model", settings.apiModel)
                put("messages", org.json.JSONArray().apply {
                    put(JSONObject().put("role", "system").put("content", "你是营养数据 API，只输出 JSON。"))
                    put(JSONObject().put("role", "user").put("content", prompt))
                })
                put("temperature", 0.1)
            }
            val body = jsonReq.toString()
                .toRequestBody("application/json; charset=utf-8".toMediaType())
            val req = Request.Builder()
                .url(settings.apiBaseUrl.trimEnd('/') + "/chat/completions")
                .header("Authorization", "Bearer " + settings.apiKey)
                .post(body)
                .build()

            onLog("   等待 AI 生成…")
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    onLog("   接口返回 HTTP ${resp.code}")
                    return null
                }
                val text = resp.body?.string() ?: return null
                val content = JSONObject(text)
                    .optJSONArray("choices")?.optJSONObject(0)
                    ?.optJSONObject("message")?.optString("content") ?: return null
                onLog("   收到回复，解析 JSON…")
                // 提取 JSON 块（AI 可能带 ```json 包裹）
                val jsonText = Regex("""\{[^{}]*\}""").find(content)?.value ?: content
                val obj = JSONObject(jsonText)
                CustomFood(
                    name = foodName,
                    calories = obj.optDouble("calories", 0.0),
                    protein = obj.optDouble("protein", 0.0),
                    fat = obj.optDouble("fat", 0.0),
                    carbs = obj.optDouble("carbs", 0.0),
                    fiber = obj.optDouble("fiber", 0.0),
                    vitaminC = obj.optDouble("vitaminC", 0.0),
                    calcium = obj.optDouble("calcium", 0.0),
                    iron = obj.optDouble("iron", 0.0),
                )
            }
        } catch (e: Exception) {
            onLog("   AI 调用异常：${e.message?.take(60)}")
            null
        }
    }
}
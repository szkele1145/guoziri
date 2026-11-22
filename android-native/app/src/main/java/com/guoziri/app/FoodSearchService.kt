package com.guoziri.app

import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.regex.Pattern

/**
 * 搜索服务：DuckDuckGo HTML 抓取 + 营养表解析
 * 回调流程：onLog("正在搜索...") → onLog("解析营养表...") → onResult(营养数据)
 */
object FoodSearchService {

    private val USER_AGENT = "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36"
    private val SEARCH_URL = "https://html.duckduckgo.com/html/?q=%s+%s"
    private val NUTRITION_KEYS = listOf("热量", "蛋白质", "脂肪", "碳水", "膳食纤维", "维生素C", "钙", "铁")
    private val UNIT_MAP = mapOf(
        "热量" to "kcal", "蛋白质" to "g", "脂肪" to "g", "碳水" to "g",
        "膳食纤维" to "g", "维生素C" to "mg", "钙" to "mg", "铁" to "mg"
    )

    /**
     * 搜索并解析食物营养
     * @param foodName 食物名称
     * @param onLog 日志回调（实时流式）
     * @param onProgress 进度回调 0-100
     * @param onResult 结果回调 (营养Map, errorMsg)
     */
    fun searchNutrition(
        foodName: String,
        onLog: (String) -> Unit,
        onProgress: (Int) -> Unit,
        onResult: (Map<String, Double>?, String?) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                onLog("🔍 正在搜索: $foodName")
                onProgress(10)

                // 1. 搜索关键词
                val query = URLEncoder.encode("$foodName 营养成分表 每100克", "UTF-8")
                val searchUrl = "https://html.duckduckgo.com/html/?q=$query"

                onLog("📡 发起搜索请求...")
                onProgress(20)

                val html = fetchHtml(searchUrl) ?: run {
                    onLog("❌ 搜索请求失败")
                    onResult(null, "搜索请求失败")
                    return@launch
                }

                onLog("📄 解析搜索结果...")
                onProgress(30)

                // 2. 提取前 3 个结果链接
                val links = extractResultLinks(html).take(3)
                if (links.isEmpty()) {
                    onLog("❌ 未找到相关结果")
                    onResult(null, "未找到相关营养信息")
                    return@launch
                }

                onLog("🔗 找到 ${links.size} 个候选页面")
                onProgress(40)

                // 3. 依次访问页面解析营养表
                for ((idx, link) in links.withIndex()) {
                    onLog("📖 正在解析第 ${idx + 1} 个页面: $link")
                    onProgress(40 + idx * 15)

                    val pageHtml = fetchHtml(link) ?: continue
                    val nutrition = parseNutritionTable(pageHtml, foodName)
                    
                    if (nutrition.isNotEmpty()) {
                        onLog("✅ 解析成功！营养项: ${nutrition.size} 项")
                        onProgress(100)
                        onResult(nutrition, null)
                        return@launch
                    } else {
                        onLog("⚠️ 第 ${idx + 1} 页面无有效营养表，尝试下一个...")
                    }
                }

                onLog("❌ 所有页面均无有效营养表")
                onResult(null, "未能解析出有效营养数据")

            } catch (e: Exception) {
                e.printStackTrace()
                onLog("❌ 异常: ${e.message}")
                onResult(null, "搜索异常: ${e.message}")
            }
        }
    }

    private fun fetchHtml(url: String): String? {
        try {
            val url = URL(url)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 10000
            conn.readTimeout = 15000
            conn.addRequestProperty("User-Agent", USER_AGENT)
            conn.addRequestProperty("Accept", "text/html")

            if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                val html = conn.inputStream.readBytes().decodeToString()
                conn.disconnect()
                return html
            }
            conn.disconnect()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun extractResultLinks(html: String): List<String> {
        val links = mutableListOf<String>()
        // DuckDuckGo 结果链接 pattern: class="result__url" 或 <a class="result__snippet"
        val pattern = Pattern.compile("class=\"result__snippet\".*?href=\"([^\"]+)\"", Pattern.DOTALL)
        val matcher = pattern.matcher(html)
        while (matcher.find()) {
            val url = matcher.group(1)
            if (url.startsWith("http") && !url.contains("duckduckgo.com")) {
                links.add(url)
            }
        }
        return links.distinct()
    }

    private fun parseNutritionTable(html: String, foodName: String): Map<String, Double> {
        val nutrition = mutableMapOf<String, Double>()

        // 尝试多种表格结构
        val tablePatterns = listOf(
            // 标准表格
            "<table[^>]*>.*?</table>",
            // 列表形式
            "<dl[^>]*>.*?</dl>",
            // 简单列表
            "<ul[^>]*>.*?</ul>"
        )

        for (patternStr in tablePatterns) {
            val pattern = Pattern.compile(patternStr, Pattern.DOTALL | Pattern.CASE_INSENSITIVE)
            val matcher = pattern.matcher(html)
            while (matcher.find()) {
                val tableHtml = matcher.group()
                val parsed = parseTableContent(tableHtml)
                if (parsed.size >= 3) { // 至少 3 项才算有效
                    return parsed
                }
            }
        }

        // 兜底：全文正则匹配 "关键词 数字 单位"
        val fallback = parseFallback(html)
        return fallback
    }

    private fun parseTableContent(html: String): Map<String, Double> {
        val result = mutableMapOf<String, Double>()
        
        // 匹配 <tr><td>关键词</td><td>数值 单位</td></tr> 或类似
        val rowPattern = Pattern.compile(
            "<tr[^>]*>.*?<t[dh][^>]*>([^<]+)</t[dh]>.*?<t[dh][^>]*>([^<]+)</t[dh]>.*?</tr>",
            Pattern.DOTALL | Pattern.CASE_INSENSITIVE
        )
        val matcher = rowPattern.matcher(html)
        
        while (matcher.find()) {
            val key = matcher.group(1).trim().replace("\\s+", " ")
            val value = matcher.group(2).trim().replace("\\s+", " ")
            
            val parsed = parseKeyValue(key, value)
            if (parsed != null) {
                result[parsed.first] = parsed.second
            }
        }

        // 尝试 th/td 结构
        if (result.size < 3) {
            val thTdPattern = Pattern.compile(
                "<th[^>]*>([^<]+)</th>.*?<td[^>]*>([^<]+)</td>",
                Pattern.DOTALL | Pattern.CASE_INSENSITIVE
            )
            val matcher2 = thTdPattern.matcher(html)
            while (matcher2.find()) {
                val key = matcher2.group(1).trim()
                val value = matcher2.group(2).trim()
                val parsed = parseKeyValue(key, value)
                if (parsed != null) result[parsed.first] = parsed.second
            }
        }

        return result
    }

    private fun parseKeyValue(key: String, value: String): Pair<String, Double>? {
        val cleanKey = key.trim().lowercase()
        
        // 匹配营养素关键词
        val matchedKey = NUTRITION_KEYS.find { cleanKey.contains(it) }
        if (matchedKey == null) return null

        // 提取数字
        val numPattern = Pattern.compile("([\\d.]+)")
        val matcher = numPattern.matcher(value)
        if (matcher.find()) {
            val num = matcher.group(1).toDoubleOrNull() ?: return null
            return matchedKey to num
        }
        return null
    }

    private fun parseFallback(html: String): Map<String, Double> {
        val result = mutableMapOf<String, Double>()
        val text = html.replace("<[^>]+>", " ").replace("\\s+", " ")
        
        for (key in NUTRITION_KEYS) {
            // 匹配 "热量 123 kcal" 或 "热量：123kcal" 等
            val pattern = Pattern.compile("$key[:：]?\\s*([\\d.]+)\\s*${UNIT_MAP[key]}", Pattern.CASE_INSENSITIVE)
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                val num = matcher.group(1).toDoubleOrNull() ?: continue
                result[key] = num
            }
        }
        return result
    }
}
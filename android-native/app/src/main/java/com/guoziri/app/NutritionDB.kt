package com.guoziri.app

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.InputStreamReader

/**
 * 中国食物成分表第6版数据（运行时从 JSON 加载）
 * 来源: 中国食物成分表第6版 (json_data_v3_20260825_qwen38max_kimi_k3_fixed)
 * 食物种类: 1650 种
 * 字段: calories(kcal), protein(g), fat(g), carbs(g), fiber(g), vitaminC(mg), calcium(mg), iron(mg)
 */
object NutritionDB {

    private var _db: Map<String, Map<String, Double>>? = null
    private val _alias: Map<String, String>? = null

    /** 同步加载（首次调用时解析 JSON，后续直接返回缓存） */
    private val DB: Map<String, Map<String, Double>>
        get() {
            if (_db != null) return _db!!
            // 同步加载 JSON（首次启动时）
            val gson = Gson()
            val mapType = object : TypeToken<Map<String, Map<String, Double>>>() {}.type
            try {
                val context = com.guoziri.app.AppContext.context
                    ?: throw IllegalStateException("AppContext 未初始化")
                val inputStream = context.assets.open("nutrition_db.json")
                val reader = InputStreamReader(inputStream, "UTF-8")
                val db = gson.fromJson(reader, mapType)
                    ?: emptyMap()
                _db = db
                return db
            } catch (e: Exception) {
                throw RuntimeException("营养数据库加载失败", e)
            }
        }

    /** 别名映射：别名 -> 标准名（去重后，仅保留常用别名） */
    val ALIAS: Map<String, String> = mapOf(
        "上海青" to "青菜", "小油菜" to "青菜", "青菜心" to "青菜",
        "西红柿" to "番茄", "番茄" to "西红柿", "圣女果" to "番茄",
        "菜椒" to "青椒", "柿子椒" to "青椒", "青椒" to "菜椒", "尖椒" to "青椒",
        "圆椒" to "青椒", "灯笼椒" to "青椒", "彩椒" to "青椒", "甜椒" to "青椒",
        "胡萝卜" to "红萝卜", "红萝卜" to "胡萝卜", "白萝卜" to "萝卜", "萝卜" to "白萝卜",
        "土豆" to "马铃薯", "马铃薯" to "土豆", "洋芋" to "土豆", "山药蛋" to "山药",
        "香菇" to "冬菇", "冬菇" to "香菇", "平菇" to "平菇", "杏鲍菇" to "杏鲍菇",
        "金针菇" to "金针菇", "海鲜菇" to "海鲜菇", "蟹味菇" to "蟹味菇",
        "白灵菇" to "白灵菇", "茶树菇" to "茶树菇", "鸡腿菇" to "鸡腿菇",
        "秀珍菇" to "秀珍菇", "竹荪" to "竹荪", "竹笙" to "竹笙",
        "羊肚菌" to "羊肚菌", "松茸" to "松茸", "牛肝菌" to "牛肝菌",
        "干香菇" to "香菇", "干木耳" to "木耳", "黑木耳" to "木耳", "白木耳" to "银耳",
        "银耳" to "白木耳", "猴头菇" to "猴头菇", "羊肚菌干" to "羊肚菌",
        "松茸干" to "松茸", "牛肝菌干" to "牛肝菌",
        "猪五花" to "五花肉", "五花肉" to "猪五花", "猪排骨" to "排骨", "里脊肉" to "里脊",
        "牛腱子" to "牛腱子", "牛腩肉" to "牛腩", "羊排" to "羊肉", "羊腿" to "羊肉",
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

    /** 简单查找：名字包含关系即可 */
    fun find(name: String): Map<String, Double>? {
        if (name.isBlank()) return null
        DB.keys.forEach { k -> if (name.contains(k) || k.contains(name)) return DB[k] }
        return null
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

    /** 带别名和模糊匹配的查找 */
    fun findSmart(name: String): Map<String, Double>? {
        val direct = find(name)
        if (direct != null) return direct
        val n = name.trim()
        if (n.isEmpty()) return null
        ALIAS[n]?.let { DB[it]?.let { m -> return m } }
        ALIAS.forEach { (alias, std) ->
            if (n.contains(alias)) return DB[std]
        }
        var best: Map<String, Double>? = null
        var bestLen = 0
        DB.keys.forEach { k ->
            if (n.contains(k) && k.length > bestLen) { best = DB[k]; bestLen = k.length }
        }
        return best
    }

    /** 返回所有食材名（含别名），用于联想菜单 */
    fun allNames(): List<String> {
        return (DB.keys + ALIAS.keys).distinct()
    }
}

# -*- coding: utf-8 -*-
"""重写 NutritionDB.kt：保留 DB map，重新生成干净的 ALIAS 和方法"""
import re

path_db = r"C:\Users\一只屑\Desktop\HousewifeAppProto\android-native\app\src\main\java\com\guoziri\app\NutritionDB.kt"
with open(path_db, 'r', encoding='utf-8') as f:
    content = f.read()

# 1. 修复 Int/Double 类型：将所有整数值改为 .0
content = re.sub(r'"calories" to (\d+)(?=\s*,)', r'"calories" to \1.0', content)
content = re.sub(r'"protein" to (\d+)(?=\s*,)', r'"protein" to \1.0', content)
content = re.sub(r'"fat" to (\d+)(?=\s*,)', r'"fat" to \1.0', content)
content = re.sub(r'"carbs" to (\d+)(?=\s*,)', r'"carbs" to \1.0', content)
content = re.sub(r'"fiber" to (\d+)(?=\s*,)', r'"fiber" to \1.0', content)
content = re.sub(r'"vitaminC" to (\d+)(?=\s*,)', r'"vitaminC" to \1.0', content)
content = re.sub(r'"calcium" to (\d+)(?=\s*,)', r'"calcium" to \1.0', content)
content = re.sub(r'"iron" to (\d+)(?=\s*,)', r'"iron" to \1.0', content)

# 2. 删除自动生成的 ALIAS map 和后续添加的方法（从 "val ALIAS:" 到文件末尾）
alias_start = content.find('    // 别名 -> 标准名')
if alias_start == -1:
    alias_start = content.find('    val ALIAS: Map<String, String> = mapOf(')
if alias_start != -1:
    content = content[:alias_start]
    print("已截断自动生成的 ALIAS 及后续内容")

# 2. 添加干净的 ALIAS map 和方法
new_end = '''

    // 别名映射：别名 -> 标准名（去重后，仅保留常用别名）
    val ALIAS: Map<String, String> = mapOf(
        // 常见蔬菜别名
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
        // 常见肉类别名
        "猪五花" to "五花肉", "五花肉" to "猪五花", "猪排骨" to "排骨", "里脊肉" to "里脊",
        "牛腱子" to "牛腱子", "牛腩肉" to "牛腩", "羊排" to "羊肉", "羊腿" to "羊肉",
        "鸡翅" to "鸡肉", "鸡腿" to "鸡肉", "鸡爪" to "鸡肉", "鸡胸" to "鸡胸肉",
        "鸭腿" to "鸭肉", "鸭脖" to "鸭肉", "鸭架" to "鸭肉",
        // 常见水产别名
        "大虾" to "虾", "基围虾" to "虾", "河虾" to "虾", "明虾" to "虾",
        "海米" to "虾", "虾皮" to "虾",
        "鲫鱼汤" to "鲫鱼", "草鱼片" to "草鱼", "鲈鱼片" to "鲈鱼",
        "三文鱼片" to "三文鱼", "生鱼片" to "三文鱼",
        // 常见蛋奶别名
        "鸡蛋液" to "鸡蛋", "煎蛋" to "鸡蛋", "水煮蛋" to "鸡蛋", "荷包蛋" to "鸡蛋",
        "鸭蛋液" to "鸭蛋", "咸鸭蛋" to "鸭蛋",
        "牛奶片" to "牛奶", "纯牛奶" to "牛奶", "鲜奶" to "牛奶",
        "无糖酸奶" to "酸奶", "老酸奶" to "酸奶", "希腊酸奶" to "酸奶",
        // 常见主食别名
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
'''

# 找到最后的 } 位置
last_brace = content.rfind('}')
if last_brace != -1:
    content = content[:last_brace] + new_end + '\n' + content[last_brace:]

with open(path_db, 'w', encoding='utf-8') as f:
    f.write(content)
print("NutritionDB.kt 已重写完成")
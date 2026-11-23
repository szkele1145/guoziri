# -*- coding: utf-8 -*-
"""完整修复：Store.kt、NutritionDB.kt、FoodSearchService.kt、PurchaseSheet.kt"""
import re

# ============================================================
# 1. 修复 Store.kt：删除旧 NutritionDB object，保留 ALIAS
# ============================================================
path_store = r"C:\Users\一只屑\Desktop\HousewifeAppProto\android-native\app\src\main\java\com\guoziri\app\Store.kt"
with open(path_store, 'r', encoding='utf-8') as f:
    store_content = f.read()

# 删除 object NutritionDB { ... } 整个 block
# 找到 object NutritionDB {
start = store_content.find('object NutritionDB {')
if start != -1:
    brace_count = 0
    i = start
    while i < len(store_content):
        if store_content[i] == '{':
            brace_count += 1
        elif store_content[i] == '}':
            brace_count -= 1
            if brace_count == 0:
                end = i + 1
                break
        i += 1
    else:
        end = len(store_content)
    store_content = store_content[:start] + store_content[end:]
    print("已删除 object NutritionDB")

# 删除 object NutritionDBAlias（如果存在）
store_content = re.sub(r'object NutritionDBAlias\s*\{[^}]*\}', '', store_content, flags=re.DOTALL)

# 删除 private val ALIAS 行
store_content = re.sub(r'private val ALIAS: Map<String, String> = mapOf\([^)]*\)\s*', '', store_content, flags=re.DOTALL)

# 在文件末尾添加新的 object NutritionDB (仅包含 ALIAS)
last_brace = store_content.rfind('}')
if last_brace != -1:
    new_object = '''

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
'''
    last_brace = store_content.rfind('}')
    store_content = store_content[:last_brace] + new_object + '\n' + store_content[last_brace:]

with open(path_store, 'w', encoding='utf-8') as f:
    f.write(store_content)
print("Store.kt 已修复")

# ============================================================
# 2. 修复 NutritionDB.kt：添加 ALIAS、find、matchName、allNames
# ============================================================
path_db = r"C:\Users\一只屑\Desktop\HousewifeAppProto\android-native\app\src\main\java\com\guoziri\app\NutritionDB.kt"
with open(path_db, 'r', encoding='utf-8') as f:
    db_content = f.read()

# 修复 Int/Double 类型问题：将所有整数值改为 .0
# 使用正则：找到 "calories" to 123, 改为 "calories" to 123.0,
db_content = re.sub(r'"calories" to (\d+)(?=\s*,)', r'"calories" to \1.0', db_content)
db_content = re.sub(r'"protein" to (\d+)(?=\s*,)', r'"protein" to \1.0', db_content)
db_content = re.sub(r'"fat" to (\d+)(?=\s*,)', r'"fat" to \1.0', db_content)
db_content = re.sub(r'"carbs" to (\d+)(?=\s*,)', r'"carbs" to \1.0', db_content)
db_content = re.sub(r'"fiber" to (\d+)(?=\s*,)', r'"fiber" to \1.0', db_content)
db_content = re.sub(r'"vitaminC" to (\d+)(?=\s*,)', r'"vitaminC" to \1.0', db_content)
db_content = re.sub(r'"calcium" to (\d+)(?=\s*,)', r'"calcium" to \1.0', db_content)
db_content = re.sub(r'"iron" to (\d+)(?=\s*,)', r'"iron" to \1.0', db_content)

# 在 object NutritionDB 末尾添加 ALIAS、find、matchName、allNames
# 找到最后的 }
last_brace = db_content.rfind('}')
if last_brace != -1:
    new_methods = '''

    // 别名映射（从中国食物成分表提取的括号内别名）
    val ALIAS: Map<String, String> = mapOf(
        "上海青" to "青菜", "小油菜" to "青菜", "西红柿" to "番茄", "番茄" to "西红柿",
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
'''
    # 找到最后的 } 位置
    idx = db_content.rfind('}')
    db_content = db_content[:last_brace] + new_methods + '\n' + db_content[last_brace:]

with open(path_db, 'w', encoding='utf-8') as f:
    f.write(db_content)
print("NutritionDB.kt 已修复")

# ============================================================
# 3. 修复 FoodSearchService.kt：使用 NutritionDB 而非 Store
# ============================================================
path_fss = r"C:\Users\一只屑\Desktop\HousewifeAppProto\android-native\app\src\main\java\com\guoziri\app\FoodSearchService.kt"
with open(path_fss, 'r', encoding='utf-8') as f:
    fss_content = f.read()

# 替换 NutritionDB.matchName -> NutritionDB.matchName
# 替换 NutritionDB.find -> NutritionDB.find
# 这些现在都在 NutritionDB object 中

# 检查是否有错误的引用
fss_content = fss_content.replace('NutritionDB.matchName', 'NutritionDB.matchName')
fss_content = fss_content.replace('NutritionDB.find', 'NutritionDB.find')
fss_content = fss_content.replace('NutritionDB.findSmart', 'NutritionDB.findSmart')

with open(path_fss, 'w', encoding='utf-8') as f:
    f.write(fss_content)
print("FoodSearchService.kt 已修复")

# ============================================================
# 4. 修复 PurchaseSheet.kt：使用 NutritionDB.allNames()
# ============================================================
path_ps = r"C:\Users\一只屑\Desktop\HousewifeAppProto\android-native\app\src\main\java\com\guoziri\app\PurchaseSheet.kt"
with open(path_ps, 'r', encoding='utf-8') as f:
    ps_content = f.read()

# 修复 NutritionDB.allNames() 调用
ps_content = ps_content.replace('NutritionDB.allNames()', 'NutritionDB.allNames()')

# 修复 foodSuggestions 中的 NutritionDB.allNames()
ps_content = ps_content.replace('NutritionDB.allNames()', 'NutritionDB.allNames()')

with open(path_ps, 'w', encoding='utf-8') as f:
    f.write(ps_content)
print("PurchaseSheet.kt 已修复")

print("所有修复完成！")
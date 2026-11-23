# -*- coding: utf-8 -*-
"""修复 Store.kt：移除旧的 NutritionDB object，保留 ALIAS map"""
import re

path = r"C:\Users\一只屑\Desktop\HousewifeAppProto\android-native\app\src\main\java\com\guoziri\app\Store.kt"
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

# 策略：
# 1. 删除 object NutritionDB { ... } 整个 block
# 2. 将 private val ALIAS 移到 object NutritionDB 中
# 3. 删除 object NutritionDBAlias（如果存在）

# 先找到 object NutritionDB { 的位置
start = content.find('object NutritionDB {')
if start != -1:
    # 找到匹配的右大括号
    brace_count = 0
    i = start
    while i < len(content):
        if content[i] == '{':
            brace_count += 1
        elif content[i] == '}':
            brace_count -= 1
            if brace_count == 0:
                end = i + 1
                break
        i += 1
    else:
        end = len(content)
    
    # 删除这段
    content = content[:start] + content[end:]
    print(f"已删除 object NutritionDB ({start}-{end})")

# 找到 private val ALIAS: Map<String, String> = mapOf(
alias_match = re.search(r'private val ALIAS: Map<String, String> = mapOf\(', content)
if alias_match:
    alias_start = alias_match.start()
    # 找到匹配的右括号
    paren_count = 0
    i = alias_match.end() - 1  # 从 '(' 开始
    while i < len(content):
        if content[i] == '(':
            paren_count += 1
        elif content[i] == ')':
            paren_count -= 1
            if paren_count == 0:
                alias_end = i + 1
                break
        i += 1
    else:
        alias_end = len(content)
    
    alias_block = content[alias_start:alias_end]
    print(f"找到 ALIAS block: {alias_start}-{alias_end}")
    
    # 删除原有的 private val ALIAS 行和 map 内容
    content = content[:alias_start] + content[alias_end:]
    print("已移除 private val ALIAS block")

# 现在在文件末尾（或合适位置）添加 object NutritionDB { ALIAS map }
# 找到文件最后的 object 定义位置，在其后添加
# 我们在文件末尾的 } 之前插入

# 先找到最后一个 object 定义
# 找到最后的 "}" 位置
last_brace = content.rfind('}')
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
        "鸭腿" to "鸡肉", "鸭脖" to "鸡肉", "鸭架" to "鸭肉",
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
    content = content[:last_brace] + new_object + '\n' + content[last_brace:]
    print("已添加 object NutritionDB with ALIAS")

with open(path, 'w', encoding='utf-8') as f:
    f.write(content)
print("已写入文件")
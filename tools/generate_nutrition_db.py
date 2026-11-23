# -*- coding: utf-8 -*-
"""
合并中国食物成分表 JSON 数据，生成 NutritionDB.kt 格式的 Kotlin 代码
"""
import json
import os
from collections import defaultdict

# 源数据目录（固定版）
SRC_DIR = r"C:\Users\一只屑\Desktop\HousewifeAppProto\tools\food_datasets\china-food-composition-data\json_data_v3_20260825_qwen38max_kimi_k3_fixed"
# 备选目录（原始版）
SRC_DIR_ALT = r"C:\Users\一只屑\Desktop\HousewifeAppProto\tools\food_datasets\china-food-composition-data\json_data_v3_20260825_qwen38max_kimi_k3"

# 输出路径
OUT_KT = r"C:\Users\一只屑\Desktop\HousewifeAppProto\android-native\app\src\main\java\com\guoziri\app\NutritionDB.kt"
OUT_JSON = r"C:\Users\一只屑\Desktop\HousewifeAppProto\tools\merged_nutrition.json"

# 字段映射：JSON key -> Kotlin key
FIELD_MAP = {
    "energyKCal": "calories",      # 热量
    "protein": "protein",          # 蛋白质
    "fat": "fat",                  # 脂肪
    "CHO": "carbs",                # 碳水化合物
    "dietaryFiber": "fiber",       # 膳食纤维
    "vitaminC": "vitaminC",        # 维生素C
    "Ca": "calcium",               # 钙
    "Fe": "iron",                  # 铁
}

# 我们需要的字段（每100g）
NEEDED_FIELDS = list(FIELD_MAP.keys())

# 食物名去重：同名取第一个（通常是"代表值"）
# 别名映射：从 foodName 提取别名（括号内的内容）
import re

def extract_aliases(name):
    """从食物名提取别名，如 '大白菜（白梗）［黄芽白］' -> ['白梗', '黄芽白']"""
    aliases = []
    # 括号内容：中文括号、方括号、英文括号
    for m in re.finditer(r'[（\[\(]([^）\]\)]+)[）\]\)]', name):
        content = m.group(1).strip()
        if content and content not in aliases:
            aliases.append(content)
    # 如果名字包含 "（代表值）" 这种标记，去掉它得到标准名
    standard = re.sub(r'[（\[\(]代表值[）\]\)]', '', name).strip()
    return standard, aliases

def parse_value(v):
    """解析数值字符串，处理 Tr、空值等"""
    if v is None:
        return 0.0
    v = str(v).strip()
    if not v or v.lower() == 'tr':
        return 0.0
    try:
        return float(v)
    except ValueError:
        return 0.0

def process_file(filepath, all_foods, name_to_entries):
    """处理单个 JSON 文件"""
    try:
        with open(filepath, 'r', encoding='utf-8') as f:
            data = json.load(f)
    except Exception as e:
        print(f"  读取失败 {os.path.basename(filepath)}: {e}")
        return 0
    
    count = 0
    for item in data:
        name = item.get('foodName', '').strip()
        if not name:
            continue
        
        # 提取标准名和别名
        standard_name, aliases = extract_aliases(name)
        
        # 构建营养字典
        nutrition = {}
        for json_key, kt_key in FIELD_MAP.items():
            nutrition[kt_key] = parse_value(item.get(json_key))
        
        # 跳过全零营养的条目
        if all(v == 0 for v in nutrition.values()):
            continue
        
        # 记录到按标准名分组
        if standard_name not in name_to_entries:
            name_to_entries[standard_name] = []
        name_to_entries[standard_name].append({
            'original_name': name,
            'standard_name': standard_name,
            'aliases': aliases,
            'nutrition': nutrition,
            'foodCode': item.get('foodCode', ''),
        })
        count += 1
    
    return count

def main():
    # 优先用 fixed 版
    src_dir = SRC_DIR if os.path.exists(SRC_DIR) else SRC_DIR_ALT
    print(f"源目录: {src_dir}")
    
    if not os.path.exists(src_dir):
        print("源目录不存在！")
        return
    
    # 收集所有 JSON 文件
    json_files = []
    for root, dirs, files in os.walk(src_dir):
        for f in files:
            if f.endswith('.json') and f.startswith('merged_') and not f.endswith('_en.json'):
                json_files.append(os.path.join(root, f))
    
    print(f"找到 {len(json_files)} 个 JSON 文件")
    
    all_foods = {}
    name_to_entries = defaultdict(list)
    total_count = 0
    
    for jf in json_files:
        c = process_file(jf, all_foods, name_to_entries)
        total_count += c
        print(f"  {os.path.basename(jf)}: {c} 条")
    
    print(f"\n总计原始条目: {total_count}")
    print(f"去重后标准名数: {len(name_to_entries)}")
    
    # 去重：每个标准名取第一条（通常是"代表值"）
    final_db = {}
    alias_map = {}
    
    for std_name, entries in name_to_entries.items():
        # 优先选 "代表值" 条目
        rep_entry = next((e for e in entries if '代表值' in e['original_name']), entries[0])
        final_db[std_name] = rep_entry['nutrition']
        
        # 收集别名
        all_aliases = set()
        for e in entries:
            for a in e['aliases']:
                all_aliases.add(a)
        if all_aliases:
            alias_map[std_name] = list(all_aliases)
    
    print(f"最终食物种类: {len(final_db)}")
    
    # 生成 Kotlin 代码
    generate_kotlin(final_db, alias_map)
    
    # 同时保存 JSON 备用
    with open(OUT_JSON, 'w', encoding='utf-8') as f:
        json.dump({'db': final_db, 'alias': alias_map}, f, ensure_ascii=False, indent=2)
    print(f"已保存 JSON: {OUT_JSON}")

def generate_kotlin(db, alias_map):
    """生成 NutritionDB.kt"""
    lines = []
    lines.append("package com.guoziri.app")
    lines.append("")
    lines.append("/*")
    lines.append(" * 中国食物成分表第6版数据（自动生成）")
    lines.append(f" * 来源: 中国食物成分表第6版 (json_data_v3_20260825_qwen38max_kimi_k3_fixed)")
    lines.append(f" * 食物种类: {len(db)} 种")
    lines.append(" * 字段: calories(kcal), protein(g), fat(g), carbs(g), fiber(g), vitaminC(mg), calcium(mg), iron(mg)")
    lines.append(" */")
    lines.append("object NutritionDB {")
    lines.append("    val DB: Map<String, Map<String, Double>> = mapOf(")
    
    # 按名称排序输出
    for name in sorted(db.keys()):
        n = db[name]
        lines.append(f"        \"{name}\" to mapOf(")
        for key in ["calories", "protein", "fat", "carbs", "fiber", "vitaminC", "calcium", "iron"]:
            val = n.get(key, 0.0)
            # 格式化：整数不显示 .0
            if val == int(val):
                val_str = str(int(val))
            else:
                val_str = f"{val:.1f}".rstrip('0').rstrip('.')
            lines.append(f"            \"{key}\" to {val_str},")
        lines.append("        ),")
    
    lines.append("    )")
    lines.append("")
    
    # 别名映射
    if alias_map:
        lines.append("    // 别名 -> 标准名")
        lines.append("    val ALIAS: Map<String, String> = mapOf(")
        for std_name, aliases in sorted(alias_map.items()):
            for alias in sorted(aliases):
                lines.append(f"        \"{alias}\" to \"{std_name}\",")
        lines.append("    )")
    else:
        lines.append("    val ALIAS: Map<String, String> = emptyMap()")
    
    lines.append("}")
    
    content = "\n".join(lines)
    with open(OUT_KT, 'w', encoding='utf-8') as f:
        f.write(content)
    print(f"已生成 Kotlin: {OUT_KT}")

if __name__ == "__main__":
    main()
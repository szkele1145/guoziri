# -*- coding: utf-8 -*-
"""营养数据与计算引擎 —— 与前端共享同一套数据源（后端为准）"""

# 简化版《中国食物成分表》核心数据 (每100g可食部)
NUTRITION_DB = {
    # 蔬菜类
    '西兰花': dict(calories=33, protein=4.1, fat=0.5, carbs=4.8, fiber=1.6, vitaminC=56, calcium=67, iron=1.2),
    '菠菜': dict(calories=28, protein=2.9, fat=0.3, carbs=3.5, fiber=1.7, vitaminC=32, calcium=66, iron=2.9),
    '油菜': dict(calories=25, protein=1.8, fat=0.3, carbs=3.2, fiber=1.1, vitaminC=40, calcium=153, iron=1.1),
    '青椒': dict(calories=23, protein=0.9, fat=0.2, carbs=4.5, fiber=1.3, vitaminC=72, calcium=11, iron=0.5),
    '红椒': dict(calories=26, protein=1.0, fat=0.2, carbs=5.0, fiber=1.4, vitaminC=130, calcium=12, iron=0.6),
    '胡萝卜': dict(calories=37, protein=0.9, fat=0.2, carbs=8.1, fiber=1.4, vitaminC=6, calcium=33, iron=0.6),
    '土豆': dict(calories=81, protein=2.0, fat=0.1, carbs=17.8, fiber=1.0, vitaminC=9, calcium=6, iron=0.4),
    '番茄': dict(calories=19, protein=0.9, fat=0.2, carbs=3.5, fiber=0.6, vitaminC=14, calcium=5, iron=0.3),
    '黄瓜': dict(calories=15, protein=0.7, fat=0.1, carbs=3.1, fiber=0.5, vitaminC=8, calcium=16, iron=0.3),
    '茄子': dict(calories=23, protein=0.8, fat=0.1, carbs=4.9, fiber=1.0, vitaminC=5, calcium=9, iron=0.4),
    '白菜': dict(calories=17, protein=1.1, fat=0.2, carbs=2.5, fiber=0.6, vitaminC=27, calcium=54, iron=0.4),
    '生菜': dict(calories=14, protein=1.0, fat=0.2, carbs=2.0, fiber=0.7, vitaminC=15, calcium=36, iron=0.5),
    '豆角': dict(calories=31, protein=2.4, fat=0.2, carbs=5.2, fiber=2.1, vitaminC=18, calcium=46, iron=1.0),
    '豌豆': dict(calories=76, protein=5.4, fat=0.4, carbs=13.5, fiber=5.5, vitaminC=20, calcium=25, iron=1.5),
    '玉米': dict(calories=106, protein=3.4, fat=1.2, carbs=22.8, fiber=2.9, vitaminC=8, calcium=2, iron=0.5),
    '红薯': dict(calories=102, protein=1.3, fat=0.2, carbs=23.1, fiber=1.6, vitaminC=16, calcium=18, iron=0.5),
    '山药': dict(calories=56, protein=1.9, fat=0.1, carbs=12.4, fiber=0.8, vitaminC=10, calcium=12, iron=0.5),
    '莲藕': dict(calories=46, protein=1.2, fat=0.1, carbs=10.2, fiber=1.0, vitaminC=19, calcium=16, iron=0.4),
    '冬瓜': dict(calories=11, protein=0.4, fat=0.1, carbs=2.4, fiber=0.6, vitaminC=10, calcium=14, iron=0.2),
    '南瓜': dict(calories=22, protein=0.7, fat=0.1, carbs=4.8, fiber=0.8, vitaminC=9, calcium=14, iron=0.4),
    '苦瓜': dict(calories=19, protein=0.8, fat=0.2, carbs=3.5, fiber=0.9, vitaminC=56, calcium=14, iron=0.4),
    '芹菜': dict(calories=15, protein=0.6, fat=0.1, carbs=2.6, fiber=1.4, vitaminC=8, calcium=120, iron=0.8),
    '香菜': dict(calories=28, protein=2.3, fat=0.4, carbs=4.5, fiber=1.2, vitaminC=48, calcium=101, iron=2.1),
    '韭菜': dict(calories=26, protein=2.4, fat=0.3, carbs=3.8, fiber=1.4, vitaminC=26, calcium=59, iron=1.6),
    '蒜苗': dict(calories=34, protein=2.1, fat=0.3, carbs=5.7, fiber=1.2, vitaminC=31, calcium=34, iron=0.8),
    '豆芽': dict(calories=18, protein=2.4, fat=0.2, carbs=2.2, fiber=1.0, vitaminC=12, calcium=21, iron=0.6),
    '竹笋': dict(calories=19, protein=2.2, fat=0.2, carbs=3.0, fiber=1.4, vitaminC=11, calcium=13, iron=0.7),
    # 水果类
    '苹果': dict(calories=53, protein=0.3, fat=0.2, carbs=13.5, fiber=1.2, vitaminC=4, calcium=4, iron=0.3),
    '香蕉': dict(calories=91, protein=1.2, fat=0.2, carbs=20.5, fiber=1.1, vitaminC=8, calcium=5, iron=0.4),
    '橙子': dict(calories=47, protein=0.8, fat=0.2, carbs=10.5, fiber=0.7, vitaminC=33, calcium=20, iron=0.2),
    '葡萄': dict(calories=60, protein=0.4, fat=0.1, carbs=14.8, fiber=0.4, vitaminC=4, calcium=8, iron=0.3),
    '西瓜': dict(calories=31, protein=0.6, fat=0.1, carbs=6.8, fiber=0.2, vitaminC=6, calcium=4, iron=0.2),
    '草莓': dict(calories=32, protein=0.8, fat=0.2, carbs=6.0, fiber=1.1, vitaminC=62, calcium=18, iron=0.5),
    '火龙果': dict(calories=51, protein=0.6, fat=0.1, carbs=12.2, fiber=1.4, vitaminC=4, calcium=8, iron=0.3),
    '猕猴桃': dict(calories=56, protein=1.0, fat=0.6, carbs=12.2, fiber=2.6, vitaminC=62, calcium=26, iron=0.6),
    '蓝莓': dict(calories=57, protein=0.7, fat=0.3, carbs=14.5, fiber=2.4, vitaminC=9, calcium=6, iron=0.3),
    '樱桃': dict(calories=46, protein=0.9, fat=0.1, carbs=10.6, fiber=0.5, vitaminC=10, calcium=12, iron=0.4),
    '桃子': dict(calories=42, protein=0.8, fat=0.1, carbs=9.5, fiber=1.0, vitaminC=7, calcium=4, iron=0.4),
    '梨': dict(calories=42, protein=0.3, fat=0.1, carbs=10.1, fiber=1.0, vitaminC=4, calcium=4, iron=0.2),
    '柚子': dict(calories=41, protein=0.7, fat=0.1, carbs=9.1, fiber=0.6, vitaminC=23, calcium=15, iron=0.2),
    '芒果': dict(calories=65, protein=0.6, fat=0.3, carbs=15.3, fiber=1.3, vitaminC=23, calcium=10, iron=0.3),
    '榴莲': dict(calories=147, protein=2.6, fat=5.3, carbs=25.3, fiber=1.7, vitaminC=2, calcium=6, iron=0.4),
    # 肉类
    '猪肉': dict(calories=395, protein=13.2, fat=37.0, carbs=0, fiber=0, vitaminC=0, calcium=6, iron=1.3),
    '猪里脊': dict(calories=143, protein=20.3, fat=6.2, carbs=0, fiber=0, vitaminC=0, calcium=4, iron=2.4),
    '猪五花': dict(calories=518, protein=9.0, fat=53.0, carbs=0, fiber=0, vitaminC=0, calcium=5, iron=1.0),
    '牛肉': dict(calories=125, protein=20.2, fat=4.2, carbs=0, fiber=0, vitaminC=0, calcium=4, iron=3.2),
    '羊肉': dict(calories=203, protein=17.8, fat=14.1, carbs=0, fiber=0, vitaminC=0, calcium=7, iron=2.0),
    '鸡肉': dict(calories=167, protein=19.3, fat=9.4, carbs=0, fiber=0, vitaminC=0, calcium=11, iron=0.9),
    '鸡胸肉': dict(calories=133, protein=21.5, fat=4.8, carbs=0, fiber=0, vitaminC=0, calcium=11, iron=0.7),
    '鸭肉': dict(calories=240, protein=16.5, fat=19.7, carbs=0, fiber=0, vitaminC=0, calcium=11, iron=2.4),
    '鹅肉': dict(calories=317, protein=15.6, fat=28.4, carbs=0, fiber=0, vitaminC=0, calcium=10, iron=2.1),
    # 海鲜类
    '草鱼': dict(calories=113, protein=17.9, fat=4.1, carbs=0.5, fiber=0, vitaminC=0, calcium=38, iron=0.7),
    '鲤鱼': dict(calories=109, protein=17.1, fat=4.1, carbs=1.3, fiber=0, vitaminC=0, calcium=55, iron=0.9),
    '鲫鱼': dict(calories=101, protein=16.9, fat=3.2, carbs=0.6, fiber=0, vitaminC=0, calcium=58, iron=0.8),
    '三文鱼': dict(calories=179, protein=19.3, fat=11.0, carbs=0, fiber=0, vitaminC=0, calcium=8, iron=0.3),
    '鳕鱼': dict(calories=75, protein=16.5, fat=0.5, carbs=0, fiber=0, vitaminC=0, calcium=13, iron=0.2),
    '带鱼': dict(calories=127, protein=18.1, fat=5.3, carbs=0.1, fiber=0, vitaminC=0, calcium=81, iron=1.0),
    '黄鱼': dict(calories=95, protein=17.6, fat=2.4, carbs=0, fiber=0, vitaminC=0, calcium=36, iron=0.6),
    '鲈鱼': dict(calories=97, protein=18.6, fat=2.2, carbs=0, fiber=0, vitaminC=0, calcium=17, iron=0.4),
    '虾': dict(calories=93, protein=18.6, fat=0.8, carbs=1.3, fiber=0, vitaminC=0, calcium=129, iron=1.1),
    '蟹': dict(calories=103, protein=17.5, fat=2.6, carbs=2.3, fiber=0, vitaminC=0, calcium=102, iron=1.6),
    '扇贝': dict(calories=75, protein=14.1, fat=0.3, carbs=3.2, fiber=0, vitaminC=0, calcium=28, iron=2.6),
    '蛤蜊': dict(calories=57, protein=10.0, fat=0.8, carbs=2.5, fiber=0, vitaminC=0, calcium=73, iron=27.6),
    '牡蛎': dict(calories=60, protein=7.1, fat=1.5, carbs=3.3, fiber=0, vitaminC=0, calcium=101, iron=6.1),
    '鱿鱼': dict(calories=75, protein=15.3, fat=0.9, carbs=1.1, fiber=0, vitaminC=0, calcium=21, iron=0.8),
    '章鱼': dict(calories=67, protein=13.9, fat=0.7, carbs=2.0, fiber=0, vitaminC=0, calcium=24, iron=1.0),
    # 蛋奶类
    '鸡蛋': dict(calories=144, protein=13.3, fat=8.8, carbs=2.8, fiber=0, vitaminC=0, calcium=56, iron=1.8),
    '鸭蛋': dict(calories=180, protein=13.5, fat=13.3, carbs=2.5, fiber=0, vitaminC=0, calcium=64, iron=3.8),
    '鹌鹑蛋': dict(calories=158, protein=12.7, fat=11.1, carbs=1.7, fiber=0, vitaminC=0, calcium=61, iron=3.2),
    '牛奶': dict(calories=54, protein=3.0, fat=3.2, carbs=3.4, fiber=0, vitaminC=1, calcium=104, iron=0.1),
    '酸奶': dict(calories=62, protein=2.9, fat=2.8, carbs=5.6, fiber=0, vitaminC=1, calcium=118, iron=0.1),
    '奶酪': dict(calories=328, protein=24.9, fat=26.0, carbs=0.8, fiber=0, vitaminC=0, calcium=659, iron=0.2),
    # 主食类
    '大米': dict(calories=116, protein=2.6, fat=0.3, carbs=25.9, fiber=0.3, vitaminC=0, calcium=7, iron=0.4),
    '小米': dict(calories=119, protein=3.4, fat=1.2, carbs=24.6, fiber=1.0, vitaminC=0, calcium=14, iron=1.5),
    '玉米面': dict(calories=108, protein=2.9, fat=1.1, carbs=24.2, fiber=1.6, vitaminC=0, calcium=3, iron=0.7),
    '馒头': dict(calories=223, protein=7.6, fat=1.1, carbs=46.9, fiber=1.0, vitaminC=0, calcium=18, iron=1.0),
    '面条': dict(calories=136, protein=4.2, fat=0.5, carbs=28.4, fiber=1.0, vitaminC=0, calcium=11, iron=0.8),
    '饺子': dict(calories=200, protein=7.0, fat=6.0, carbs=28.0, fiber=1.2, vitaminC=2, calcium=15, iron=1.2),
    # 豆制品
    '豆腐': dict(calories=81, protein=8.2, fat=4.8, carbs=1.2, fiber=0.3, vitaminC=0, calcium=164, iron=2.6),
    '豆干': dict(calories=158, protein=15.3, fat=8.8, carbs=3.2, fiber=0.8, vitaminC=0, calcium=312, iron=4.8),
    '豆浆': dict(calories=16, protein=1.8, fat=0.7, carbs=1.3, fiber=0.4, vitaminC=0, calcium=10, iron=0.5),
    '纳豆': dict(calories=146, protein=16.5, fat=8.1, carbs=7.9, fiber=5.4, vitaminC=0, calcium=146, iron=3.8),
    '毛豆': dict(calories=121, protein=13.0, fat=5.5, carbs=8.6, fiber=4.0, vitaminC=27, calcium=107, iron=3.5),
    # 菌菇类
    '香菇': dict(calories=25, protein=2.2, fat=0.3, carbs=4.7, fiber=2.6, vitaminC=2, calcium=3, iron=1.3),
    '木耳': dict(calories=28, protein=2.6, fat=0.2, carbs=5.1, fiber=2.9, vitaminC=0, calcium=45, iron=185),
    '银耳': dict(calories=200, protein=10.0, fat=1.4, carbs=67.3, fiber=30.4, vitaminC=0, calcium=92, iron=21.2),
    '平菇': dict(calories=28, protein=2.7, fat=0.4, carbs=4.5, fiber=2.3, vitaminC=2, calcium=3, iron=1.0),
    '金针菇': dict(calories=26, protein=2.4, fat=0.3, carbs=4.5, fiber=2.7, vitaminC=2, calcium=5, iron=1.2),
    '海鲜菇': dict(calories=30, protein=2.8, fat=0.4, carbs=4.8, fiber=2.5, vitaminC=2, calcium=4, iron=1.1),
    # 调料/油
    '食用油': dict(calories=899, protein=0, fat=99.9, carbs=0, fiber=0, vitaminC=0, calcium=0, iron=0),
    '食盐': dict(calories=0, protein=0, fat=0, carbs=0, fiber=0, vitaminC=0, calcium=1, iron=0.1),
    '酱油': dict(calories=52, protein=6.8, fat=0.1, carbs=6.8, fiber=0, vitaminC=0, calcium=18, iron=3.2),
    '醋': dict(calories=23, protein=0.4, fat=0.1, carbs=5.2, fiber=0, vitaminC=0, calcium=12, iron=0.6),
    '白糖': dict(calories=398, protein=0, fat=0, carbs=99.5, fiber=0, vitaminC=0, calcium=1, iron=0.1),
    # 坚果
    '花生': dict(calories=574, protein=24.6, fat=44.3, carbs=15.3, fiber=9.0, vitaminC=0, calcium=79, iron=2.7),
    '核桃': dict(calories=654, protein=14.9, fat=63.0, carbs=13.7, fiber=6.7, vitaminC=1, calcium=56, iron=2.1),
    '杏仁': dict(calories=579, protein=21.2, fat=50.6, carbs=19.7, fiber=11.8, vitaminC=0, calcium=264, iron=3.7),
    '腰果': dict(calories=553, protein=18.2, fat=43.8, carbs=30.2, fiber=3.3, vitaminC=0, calcium=37, iron=3.7),
}

# 《中国居民膳食指南2022》推荐摄入量（简化分组）
DRIS = {
    'adult_male': dict(calories=2250, protein=65, fat=70, carbs=300, fiber=25, vitaminC=100, calcium=800, iron=12),
    'adult_female': dict(calories=1800, protein=55, fat=55, carbs=240, fiber=25, vitaminC=100, calcium=800, iron=20),
    'child_4_6': dict(calories=1350, protein=30, fat=40, carbs=180, fiber=14, vitaminC=60, calcium=600, iron=12),
    'child_7_10': dict(calories=1700, protein=40, fat=50, carbs=220, fiber=18, vitaminC=70, calcium=800, iron=15),
}

NUTRIENT_KEYS = ['calories', 'protein', 'fat', 'carbs', 'fiber', 'vitaminC', 'calcium', 'iron']
NUTRIENT_LABELS = {
    'calories': ('热量', 'kcal'), 'protein': ('蛋白质', 'g'), 'fat': ('脂肪', 'g'),
    'carbs': ('碳水', 'g'), 'fiber': ('膳食纤维', 'g'), 'vitaminC': ('维生素C', 'mg'),
    'calcium': ('钙', 'mg'), 'iron': ('铁', 'mg'),
}


def find_nutrition(name):
    """模糊匹配食材名"""
    if not name:
        return None
    for key in NUTRITION_DB:
        if key in name or name in key:
            return NUTRITION_DB[key]
    return None


def parse_weight(p):
    """解析重量（克）：优先直接填的克数，其次 单价+总价 推算，再次 斤数"""
    if p.get('weightGrams'):
        return float(p['weightGrams'])
    if p.get('pricePerJin') and p.get('totalCost'):
        return (float(p['totalCost']) / float(p['pricePerJin'])) * 500
    if p.get('totalJin'):
        return float(p['totalJin']) * 500
    return 0.0


def day_nutrition(purchases):
    """汇总某天所有采购的营养"""
    total = {k: 0.0 for k in NUTRIENT_KEYS}
    details = []
    for p in purchases:
        nutri = find_nutrition(p.get('name', ''))
        if not nutri:
            continue
        weight = parse_weight(p)
        if weight <= 0:
            continue
        ratio = weight / 100.0
        item = {k: round(nutri[k] * ratio, 2) for k in NUTRIENT_KEYS}
        for k in NUTRIENT_KEYS:
            total[k] += item[k]
        details.append({'name': p.get('name'), 'weight': weight, 'nutrition': item})
    total = {k: round(v, 1) for k, v in total.items()}
    return total, details


def family_dri(family):
    """家庭每日推荐总量：成人按女性保守值，儿童按 4-6 岁"""
    total = {k: 0.0 for k in NUTRIENT_KEYS}
    for _ in range(int(family.get('adults', 0))):
        for k in NUTRIENT_KEYS:
            total[k] += DRIS['adult_female'][k]
    for _ in range(int(family.get('children', 0))):
        for k in NUTRIENT_KEYS:
            total[k] += DRIS['child_4_6'][k]
    return total


def assess(purchases, family):
    """评估某天营养达标度，返回前端渲染所需结构"""
    total, details = day_nutrition(purchases)
    dri = family_dri(family)
    assessment = []
    for k in NUTRIENT_KEYS:
        label, unit = NUTRIENT_LABELS[k]
        target = dri[k]
        percent = round(total[k] / target * 100) if target else 0
        assessment.append({
            'key': k, 'label': label, 'unit': unit,
            'actual': total[k], 'target': target, 'percent': percent,
        })
    return {'total': total, 'details': details, 'assessment': assessment}

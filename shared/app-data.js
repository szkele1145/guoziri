// 共享数据层 & 工具函数
const STORAGE_KEYS = {
  API_CONFIG: 'hh_api_config',
  PURCHASES: 'hh_purchases',
  DIARY: 'hh_diary',
  FAMILY: 'hh_family',
  SETTINGS: 'hh_settings',
  HIDDEN_ENABLED: 'hh_hidden_enabled'
};

// 默认配置
const DEFAULT_FAMILY = { adults: 2, children: 1 };
const DEFAULT_SETTINGS = {
  sortOrder: 'desc', // 'asc' | 'desc'
  theme: 'warm',
  apiProvider: 'deepseek',
  apiBaseUrl: 'https://api.deepseek.com/v1',
  apiKey: ''
};

// 食物分类
const FOOD_CATEGORIES = ['蔬菜', '水果', '肉类', '海鲜', '蛋奶', '主食', '调料', '零食', '其他'];

// 简化版《中国食物成分表》核心数据 (每100g可食部)
const NUTRITION_DB = {
  // 蔬菜类
  '西兰花': { calories: 33, protein: 4.1, fat: 0.5, carbs: 4.8, fiber: 1.6, vitaminC: 56, calcium: 67, iron: 1.2 },
  '菠菜': { calories: 28, protein: 2.9, fat: 0.3, carbs: 3.5, fiber: 1.7, vitaminC: 32, calcium: 66, iron: 2.9 },
  '油菜': { calories: 25, protein: 1.8, fat: 0.3, carbs: 3.2, fiber: 1.1, vitaminC: 40, calcium: 153, iron: 1.1 },
  '青椒': { calories: 23, protein: 0.9, fat: 0.2, carbs: 4.5, fiber: 1.3, vitaminC: 72, calcium: 11, iron: 0.5 },
  '红椒': { calories: 26, protein: 1.0, fat: 0.2, carbs: 5.0, fiber: 1.4, vitaminC: 130, calcium: 12, iron: 0.6 },
  '胡萝卜': { calories: 37, protein: 0.9, fat: 0.2, carbs: 8.1, fiber: 1.4, vitaminC: 6, calcium: 33, iron: 0.6 },
  '土豆': { calories: 81, protein: 2.0, fat: 0.1, carbs: 17.8, fiber: 1.0, vitaminC: 9, calcium: 6, iron: 0.4 },
  '番茄': { calories: 19, protein: 0.9, fat: 0.2, carbs: 3.5, fiber: 0.6, vitaminC: 14, calcium: 5, iron: 0.3 },
  '黄瓜': { calories: 15, protein: 0.7, fat: 0.1, carbs: 3.1, fiber: 0.5, vitaminC: 8, calcium: 16, iron: 0.3 },
  '茄子': { calories: 23, protein: 0.8, fat: 0.1, carbs: 4.9, fiber: 1.0, vitaminC: 5, calcium: 9, iron: 0.4 },
  '白菜': { calories: 17, protein: 1.1, fat: 0.2, carbs: 2.5, fiber: 0.6, vitaminC: 27, calcium: 54, iron: 0.4 },
  '生菜': { calories: 14, protein: 1.0, fat: 0.2, carbs: 2.0, fiber: 0.7, vitaminC: 15, calcium: 36, iron: 0.5 },
  '豆角': { calories: 31, protein: 2.4, fat: 0.2, carbs: 5.2, fiber: 2.1, vitaminC: 18, calcium: 46, iron: 1.0 },
  '豌豆': { calories: 76, protein: 5.4, fat: 0.4, carbs: 13.5, fiber: 5.5, vitaminC: 20, calcium: 25, iron: 1.5 },
  '玉米': { calories: 106, protein: 3.4, fat: 1.2, carbs: 22.8, fiber: 2.9, vitaminC: 8, calcium: 2, iron: 0.5 },
  '红薯': { calories: 102, protein: 1.3, fat: 0.2, carbs: 23.1, fiber: 1.6, vitaminC: 16, calcium: 18, iron: 0.5 },
  '山药': { calories: 56, protein: 1.9, fat: 0.1, carbs: 12.4, fiber: 0.8, vitaminC: 10, calcium: 12, iron: 0.5 },
  '莲藕': { calories: 46, protein: 1.2, fat: 0.1, carbs: 10.2, fiber: 1.0, vitaminC: 19, calcium: 16, iron: 0.4 },
  '冬瓜': { calories: 11, protein: 0.4, fat: 0.1, carbs: 2.4, fiber: 0.6, vitaminC: 10, calcium: 14, iron: 0.2 },
  '南瓜': { calories: 22, protein: 0.7, fat: 0.1, carbs: 4.8, fiber: 0.8, vitaminC: 9, calcium: 14, iron: 0.4 },
  '苦瓜': { calories: 19, protein: 0.8, fat: 0.2, carbs: 3.5, fiber: 0.9, vitaminC: 56, calcium: 14, iron: 0.4 },
  '芹菜': { calories: 15, protein: 0.6, fat: 0.1, carbs: 2.6, fiber: 1.4, vitaminC: 8, calcium: 120, iron: 0.8 },
  '香菜': { calories: 28, protein: 2.3, fat: 0.4, carbs: 4.5, fiber: 1.2, vitaminC: 48, calcium: 101, iron: 2.1 },
  '韭菜': { calories: 26, protein: 2.4, fat: 0.3, carbs: 3.8, fiber: 1.4, vitaminC: 26, calcium: 59, iron: 1.6 },
  '蒜苗': { calories: 34, protein: 2.1, fat: 0.3, carbs: 5.7, fiber: 1.2, vitaminC: 31, calcium: 34, iron: 0.8 },
  '豆芽': { calories: 18, protein: 2.4, fat: 0.2, carbs: 2.2, fiber: 1.0, vitaminC: 12, calcium: 21, iron: 0.6 },
  '竹笋': { calories: 19, protein: 2.2, fat: 0.2, carbs: 3.0, fiber: 1.4, vitaminC: 11, calcium: 13, iron: 0.7 },
  
  // 水果类
  '苹果': { calories: 53, protein: 0.3, fat: 0.2, carbs: 13.5, fiber: 1.2, vitaminC: 4, calcium: 4, iron: 0.3 },
  '香蕉': { calories: 91, protein: 1.2, fat: 0.2, carbs: 20.5, fiber: 1.1, vitaminC: 8, calcium: 5, iron: 0.4 },
  '橙子': { calories: 47, protein: 0.8, fat: 0.2, carbs: 10.5, fiber: 0.7, vitaminC: 33, calcium: 20, iron: 0.2 },
  '葡萄': { calories: 60, protein: 0.4, fat: 0.1, carbs: 14.8, fiber: 0.4, vitaminC: 4, calcium: 8, iron: 0.3 },
  '西瓜': { calories: 31, protein: 0.6, fat: 0.1, carbs: 6.8, fiber: 0.2, vitaminC: 6, calcium: 4, iron: 0.2 },
  '草莓': { calories: 32, protein: 0.8, fat: 0.2, carbs: 6.0, fiber: 1.1, vitaminC: 62, calcium: 18, iron: 0.5 },
  '火龙果': { calories: 51, protein: 0.6, fat: 0.1, carbs: 12.2, fiber: 1.4, vitaminC: 4, calcium: 8, iron: 0.3 },
  '猕猴桃': { calories: 56, protein: 1.0, fat: 0.6, carbs: 12.2, fiber: 2.6, vitaminC: 62, calcium: 26, iron: 0.6 },
  '蓝莓': { calories: 57, protein: 0.7, fat: 0.3, carbs: 14.5, fiber: 2.4, vitaminC: 9, calcium: 6, iron: 0.3 },
  '樱桃': { calories: 46, protein: 0.9, fat: 0.1, carbs: 10.6, fiber: 0.5, vitaminC: 10, calcium: 12, iron: 0.4 },
  '桃子': { calories: 42, protein: 0.8, fat: 0.1, carbs: 9.5, fiber: 1.0, vitaminC: 7, calcium: 4, iron: 0.4 },
  '梨': { calories: 42, protein: 0.3, fat: 0.1, carbs: 10.1, fiber: 1.0, vitaminC: 4, calcium: 4, iron: 0.2 },
  '柚子': { calories: 41, protein: 0.7, fat: 0.1, carbs: 9.1, fiber: 0.6, vitaminC: 23, calcium: 15, iron: 0.2 },
  '芒果': { calories: 65, protein: 0.6, fat: 0.3, carbs: 15.3, fiber: 1.3, vitaminC: 23, calcium: 10, iron: 0.3 },
  '榴莲': { calories: 147, protein: 2.6, fat: 5.3, carbs: 25.3, fiber: 1.7, vitaminC: 2, calcium: 6, iron: 0.4 },
  
  // 肉类
  '猪肉': { calories: 395, protein: 13.2, fat: 37.0, carbs: 0, fiber: 0, vitaminC: 0, calcium: 6, iron: 1.3 },
  '猪里脊': { calories: 143, protein: 20.3, fat: 6.2, carbs: 0, fiber: 0, vitaminC: 0, calcium: 4, iron: 2.4 },
  '猪五花': { calories: 518, protein: 9.0, fat: 53.0, carbs: 0, fiber: 0, vitaminC: 0, calcium: 5, iron: 1.0 },
  '牛肉': { calories: 125, protein: 20.2, fat: 4.2, carbs: 0, fiber: 0, vitaminC: 0, calcium: 4, iron: 3.2 },
  '羊肉': { calories: 203, protein: 17.8, fat: 14.1, carbs: 0, fiber: 0, vitaminC: 0, calcium: 7, iron: 2.0 },
  '鸡肉': { calories: 167, protein: 19.3, fat: 9.4, carbs: 0, fiber: 0, vitaminC: 0, calcium: 11, iron: 0.9 },
  '鸡胸肉': { calories: 133, protein: 21.5, fat: 4.8, carbs: 0, fiber: 0, vitaminC: 0, calcium: 11, iron: 0.7 },
  '鸭肉': { calories: 240, protein: 16.5, fat: 19.7, carbs: 0, fiber: 0, vitaminC: 0, calcium: 11, iron: 2.4 },
  '鹅肉': { calories: 317, protein: 15.6, fat: 28.4, carbs: 0, fiber: 0, vitaminC: 0, calcium: 10, iron: 2.1 },
  
  // 海鲜类
  '草鱼': { calories: 113, protein: 17.9, fat: 4.1, carbs: 0.5, fiber: 0, vitaminC: 0, calcium: 38, iron: 0.7 },
  '鲤鱼': { calories: 109, protein: 17.1, fat: 4.1, carbs: 1.3, fiber: 0, vitaminC: 0, calcium: 55, iron: 0.9 },
  '鲫鱼': { calories: 101, protein: 16.9, fat: 3.2, carbs: 0.6, fiber: 0, vitaminC: 0, calcium: 58, iron: 0.8 },
  '三文鱼': { calories: 179, protein: 19.3, fat: 11.0, carbs: 0, fiber: 0, vitaminC: 0, calcium: 8, iron: 0.3 },
  '鳕鱼': { calories: 75, protein: 16.5, fat: 0.5, carbs: 0, fiber: 0, vitaminC: 0, calcium: 13, iron: 0.2 },
  '带鱼': { calories: 127, protein: 18.1, fat: 5.3, carbs: 0.1, fiber: 0, vitaminC: 0, calcium: 81, iron: 1.0 },
  '黄鱼': { calories: 95, protein: 17.6, fat: 2.4, carbs: 0, fiber: 0, vitaminC: 0, calcium: 36, iron: 0.6 },
  '鲈鱼': { calories: 97, protein: 18.6, fat: 2.2, carbs: 0, fiber: 0, vitaminC: 0, calcium: 17, iron: 0.4 },
  '虾': { calories: 93, protein: 18.6, fat: 0.8, carbs: 1.3, fiber: 0, vitaminC: 0, calcium: 129, iron: 1.1 },
  '蟹': { calories: 103, protein: 17.5, fat: 2.6, carbs: 2.3, fiber: 0, vitaminC: 0, calcium: 102, iron: 1.6 },
  '扇贝': { calories: 75, protein: 14.1, fat: 0.3, carbs: 3.2, fiber: 0, vitaminC: 0, calcium: 28, iron: 2.6 },
  '蛤蜊': { calories: 57, protein: 10.0, fat: 0.8, carbs: 2.5, fiber: 0, vitaminC: 0, calcium: 73, iron: 27.6 },
  '牡蛎': { calories: 60, protein: 7.1, fat: 1.5, carbs: 3.3, fiber: 0, vitaminC: 0, calcium: 101, iron: 6.1 },
  '鱿鱼': { calories: 75, protein: 15.3, fat: 0.9, carbs: 1.1, fiber: 0, vitaminC: 0, calcium: 21, iron: 0.8 },
  '章鱼': { calories: 67, protein: 13.9, fat: 0.7, carbs: 2.0, fiber: 0, vitaminC: 0, calcium: 24, iron: 1.0 },
  
  // 蛋奶类
  '鸡蛋': { calories: 144, protein: 13.3, fat: 8.8, carbs: 2.8, fiber: 0, vitaminC: 0, calcium: 56, iron: 1.8 },
  '鸭蛋': { calories: 180, protein: 13.5, fat: 13.3, carbs: 2.5, fiber: 0, vitaminC: 0, calcium: 64, iron: 3.8 },
  '鹌鹑蛋': { calories: 158, protein: 12.7, fat: 11.1, carbs: 1.7, fiber: 0, vitaminC: 0, calcium: 61, iron: 3.2 },
  '牛奶': { calories: 54, protein: 3.0, fat: 3.2, carbs: 3.4, fiber: 0, vitaminC: 1, calcium: 104, iron: 0.1 },
  '酸奶': { calories: 62, protein: 2.9, fat: 2.8, carbs: 5.6, fiber: 0, vitaminC: 1, calcium: 118, iron: 0.1 },
  '奶酪': { calories: 328, protein: 24.9, fat: 26.0, carbs: 0.8, fiber: 0, vitaminC: 0, calcium: 659, iron: 0.2 },
  
  // 主食类
  '大米': { calories: 116, protein: 2.6, fat: 0.3, carbs: 25.9, fiber: 0.3, vitaminC: 0, calcium: 7, iron: 0.4 },
  '小米': { calories: 119, protein: 3.4, fat: 1.2, carbs: 24.6, fiber: 1.0, vitaminC: 0, calcium: 14, iron: 1.5 },
  '玉米面': { calories: 108, protein: 2.9, fat: 1.1, carbs: 24.2, fiber: 1.6, vitaminC: 0, calcium: 3, iron: 0.7 },
  '红薯': { calories: 102, protein: 1.3, fat: 0.2, carbs: 23.1, fiber: 1.6, vitaminC: 16, calcium: 18, iron: 0.5 },
  '土豆': { calories: 81, protein: 2.0, fat: 0.1, carbs: 17.8, fiber: 1.0, vitaminC: 9, calcium: 6, iron: 0.4 },
  '馒头': { calories: 223, protein: 7.6, fat: 1.1, carbs: 46.9, fiber: 1.0, vitaminC: 0, calcium: 18, iron: 1.0 },
  '面条': { calories: 136, protein: 4.2, fat: 0.5, carbs: 28.4, fiber: 1.0, vitaminC: 0, calcium: 11, iron: 0.8 },
  '饺子': { calories: 200, protein: 7.0, fat: 6.0, carbs: 28.0, fiber: 1.2, vitaminC: 2, calcium: 15, iron: 1.2 },
  
  // 豆制品
  '豆腐': { calories: 81, protein: 8.2, fat: 4.8, carbs: 1.2, fiber: 0.3, vitaminC: 0, calcium: 164, iron: 2.6 },
  '豆干': { calories: 158, protein: 15.3, fat: 8.8, carbs: 3.2, fiber: 0.8, vitaminC: 0, calcium: 312, iron: 4.8 },
  '豆浆': { calories: 16, protein: 1.8, fat: 0.7, carbs: 1.3, fiber: 0.4, vitaminC: 0, calcium: 10, iron: 0.5 },
  '纳豆': { calories: 146, protein: 16.5, fat: 8.1, carbs: 7.9, fiber: 5.4, vitaminC: 0, calcium: 146, iron: 3.8 },
  '毛豆': { calories: 121, protein: 13.0, fat: 5.5, carbs: 8.6, fiber: 4.0, vitaminC: 27, calcium: 107, iron: 3.5 },
  
  // 菌菇类
  '香菇': { calories: 25, protein: 2.2, fat: 0.3, carbs: 4.7, fiber: 2.6, vitaminC: 2, calcium: 3, iron: 1.3 },
  '木耳': { calories: 28, protein: 2.6, fat: 0.2, carbs: 5.1, fiber: 2.9, vitaminC: 0, calcium: 45, iron: 185 },
  '银耳': { calories: 200, protein: 10.0, fat: 1.4, carbs: 67.3, fiber: 30.4, vitaminC: 0, calcium: 92, iron: 21.2 },
  '平菇': { calories: 28, protein: 2.7, fat: 0.4, carbs: 4.5, fiber: 2.3, vitaminC: 2, calcium: 3, iron: 1.0 },
  '金针菇': { calories: 26, protein: 2.4, fat: 0.3, carbs: 4.5, fiber: 2.7, vitaminC: 2, calcium: 5, iron: 1.2 },
  '海鲜菇': { calories: 30, protein: 2.8, fat: 0.4, carbs: 4.8, fiber: 2.5, vitaminC: 2, calcium: 4, iron: 1.1 },
  
  // 调料/油
  '食用油': { calories: 899, protein: 0, fat: 99.9, carbs: 0, fiber: 0, vitaminC: 0, calcium: 0, iron: 0 },
  '食盐': { calories: 0, protein: 0, fat: 0, carbs: 0, fiber: 0, vitaminC: 0, calcium: 1, iron: 0.1 },
  '酱油': { calories: 52, protein: 6.8, fat: 0.1, carbs: 6.8, fiber: 0, vitaminC: 0, calcium: 18, iron: 3.2 },
  '醋': { calories: 23, protein: 0.4, fat: 0.1, carbs: 5.2, fiber: 0, vitaminC: 0, calcium: 12, iron: 0.6 },
  '白糖': { calories: 398, protein: 0, fat: 0, carbs: 99.5, fiber: 0, vitaminC: 0, calcium: 1, iron: 0.1 },
  
  // 坚果
  '花生': { calories: 574, protein: 24.6, fat: 44.3, carbs: 15.3, fiber: 9.0, vitaminC: 0, calcium: 79, iron: 2.7 },
  '核桃': { calories: 654, protein: 14.9, fat: 63.0, carbs: 13.7, fiber: 6.7, vitaminC: 1, calcium: 56, iron: 2.1 },
  '杏仁': { calories: 579, protein: 21.2, fat: 50.6, carbs: 19.7, fiber: 11.8, vitaminC: 0, calcium: 264, iron: 3.7 },
  '腰果': { calories: 553, protein: 18.2, fat: 43.8, carbs: 30.2, fiber: 3.3, vitaminC: 0, calcium: 37, iron: 3.7 }
};

// 《中国居民膳食指南2022》推荐摄入量 (按性别/年龄分组简化版)
// 成年男性(18-49) / 成年女性(18-49) / 儿童(4-6) / 儿童(7-10) / 青少年(11-13) / 青少年(14-17)
const DRIS = {
  adult_male: { calories: 2250, protein: 65, fat: 70, carbs: 300, fiber: 25, vitaminC: 100, calcium: 800, iron: 12 },
  adult_female: { calories: 1800, protein: 55, fat: 55, carbs: 240, fiber: 25, vitaminC: 100, calcium: 800, iron: 20 },
  child_4_6: { calories: 1350, protein: 30, fat: 40, carbs: 180, fiber: 14, vitaminC: 60, calcium: 600, iron: 12 },
  child_7_10: { calories: 1700, protein: 40, fat: 50, carbs: 220, fiber: 18, vitaminC: 70, calcium: 800, iron: 15 },
  teen_male_11_13: { calories: 2100, protein: 55, fat: 65, carbs: 280, fiber: 22, vitaminC: 90, calcium: 1000, iron: 18 },
  teen_female_11_13: { calories: 1900, protein: 50, fat: 60, carbs: 250, fiber: 22, vitaminC: 90, calcium: 1000, iron: 24 },
  teen_male_14_17: { calories: 2450, protein: 70, fat: 75, carbs: 320, fiber: 25, vitaminC: 100, calcium: 1000, iron: 22 },
  teen_female_14_17: { calories: 2100, protein: 60, fat: 65, carbs: 280, fiber: 25, vitaminC: 100, calcium: 1000, iron: 27 }
};

// 存储工具
const Storage = {
  get(key, defaultVal = null) {
    try { return JSON.parse(localStorage.getItem(key) || 'null') ?? defaultVal; }
    catch { return defaultVal; }
  },
  set(key, val) { localStorage.setItem(key, JSON.stringify(val)); },
  remove(key) { localStorage.removeItem(key); }
};

// 数据管理器
const DataManager = {
  // 采购记录
  getPurchases() { return Storage.get(STORAGE_KEYS.PURCHASES, []); },
  savePurchases(list) { Storage.set(STORAGE_KEYS.PURCHASES, list); },
  addPurchase(item) {
    const list = this.getPurchases();
    list.unshift({ ...item, id: Date.now().toString(), createdAt: new Date().toISOString() });
    this.savePurchases(list);
    return list[0];
  },
  deletePurchase(id) {
    const list = this.getPurchases().filter(p => p.id !== id);
    this.savePurchases(list);
    return list;
  },
  updatePurchase(id, patch) {
    const list = this.getPurchases().map(p => p.id === id ? { ...p, ...patch } : p);
    this.savePurchases(list);
    return list.find(p => p.id === id);
  },
  getPurchasesByDate(dateStr) {
    return this.getPurchases().filter(p => p.date === dateStr);
  },
  getAllDates() {
    const dates = [...new Set(this.getPurchases().map(p => p.date))].sort((a,b) => b.localeCompare(a));
    return dates;
  },
  
  // 家庭成员
  getFamily() { return Storage.get(STORAGE_KEYS.FAMILY, DEFAULT_FAMILY); },
  saveFamily(f) { Storage.set(STORAGE_KEYS.FAMILY, f); },
  
  // 设置
  getSettings() { return Storage.get(STORAGE_KEYS.SETTINGS, DEFAULT_SETTINGS); },
  saveSettings(s) { Storage.set(STORAGE_KEYS.SETTINGS, s); },
  
  // API配置
  getApiConfig() { return Storage.get(STORAGE_KEYS.API_CONFIG, { provider: 'deepseek', baseUrl: 'https://api.deepseek.com/v1', key: '' }); },
  saveApiConfig(cfg) { Storage.set(STORAGE_KEYS.API_CONFIG, cfg); },
  
  // 日记
  getDiary() { return Storage.get(STORAGE_KEYS.DIARY, []); },
  saveDiary(list) { Storage.set(STORAGE_KEYS.DIARY, list); },
  addDiary(item) {
    const list = this.getDiary();
    list.unshift({ ...item, id: Date.now().toString(), createdAt: new Date().toISOString() });
    this.saveDiary(list);
    return list[0];
  },
  deleteDiary(id) {
    const list = this.getDiary().filter(d => d.id !== id);
    this.saveDiary(list);
    return list;
  },
  
  // 隐藏页面开关
  isHiddenEnabled() { return Storage.get(STORAGE_KEYS.HIDDEN_ENABLED, false); },
  setHiddenEnabled(v) { Storage.set(STORAGE_KEYS.HIDDEN_ENABLED, !!v); },
  
  // 统计
  getTodayStats() {
    const today = new Date().toISOString().split('T')[0];
    const purchases = this.getPurchasesByDate(today);
    return purchases.reduce((acc, p) => {
      acc.totalCost += p.totalCost || 0;
      acc.count += 1;
      return acc;
    }, { totalCost: 0, count: 0 });
  }
};

// 营养计算引擎
const NutritionEngine = {
  // 查找食材营养 (模糊匹配)
  findNutrition(name) {
    const key = Object.keys(NUTRITION_DB).find(k => name.includes(k) || k.includes(name));
    return key ? NUTRITION_DB[key] : null;
  },
  
  // 计算单项营养 (按重量)
  calcNutrition(nutriPer100g, weightGrams) {
    const ratio = weightGrams / 100;
    const result = {};
    for (const [k, v] of Object.entries(nutriPer100g)) {
      result[k] = Math.round(v * ratio * 100) / 100;
    }
    return result;
  },
  
  // 汇总某天采购的营养
  getDayNutrition(dateStr) {
    const purchases = DataManager.getPurchasesByDate(dateStr);
    const total = { calories: 0, protein: 0, fat: 0, carbs: 0, fiber: 0, vitaminC: 0, calcium: 0, iron: 0 };
    const details = [];
    
    for (const p of purchases) {
      const nutri = this.findNutrition(p.name);
      if (!nutri) continue;
      const weight = this.parseWeight(p);
      if (weight <= 0) continue;
      const itemNutri = this.calcNutrition(nutri, weight);
      for (const k of Object.keys(total)) total[k] += itemNutri[k];
      details.push({ name: p.name, weight, nutrition: itemNutri });
    }
    return { total, details };
  },
  
  // 解析重量 (克)
  parseWeight(p) {
    if (p.weightGrams) return p.weightGrams;
    if (p.pricePerJin && p.totalCost) return (p.totalCost / p.pricePerJin) * 500;
    if (p.totalJin) return p.totalJin * 500;
    return 0;
  },
  
  // 获取家庭推荐摄入量
  getFamilyDRI() {
    const fam = DataManager.getFamily();
    let total = { calories: 0, protein: 0, fat: 0, carbs: 0, fiber: 0, vitaminC: 0, calcium: 0, iron: 0 };
    
    for (let i = 0; i < fam.adults; i++) {
      const dri = DRIS.adult_female; // 默认按女性计算，更保守
      for (const k of Object.keys(total)) total[k] += dri[k];
    }
    for (let i = 0; i < fam.children; i++) {
      const dri = DRIS.child_4_6; // 简化按4-6岁计算
      for (const k of Object.keys(total)) total[k] += dri[k];
    }
    return total;
  },
  
  // 评估营养达标度
  assessNutrition(dateStr) {
    const { total } = this.getDayNutrition(dateStr);
    const dri = this.getFamilyDRI();
    const nutrients = [
      { key: 'calories', label: '热量', unit: 'kcal' },
      { key: 'protein', label: '蛋白质', unit: 'g' },
      { key: 'fat', label: '脂肪', unit: 'g' },
      { key: 'carbs', label: '碳水', unit: 'g' },
      { key: 'fiber', label: '膳食纤维', unit: 'g' },
      { key: 'vitaminC', label: '维生素C', unit: 'mg' },
      { key: 'calcium', label: '钙', unit: 'mg' },
      { key: 'iron', label: '铁', unit: 'mg' }
    ];
    return nutrients.map(n => ({
      ...n,
      actual: Math.round(total[n.key] * 10) / 10,
      target: dri[n.key],
      percent: dri[n.key] > 0 ? Math.round((total[n.key] / dri[n.key]) * 100) : 0
    }));
  }
};

// 日期工具
const DateUtils = {
  today() { return new Date().toISOString().split('T')[0]; },
  format(dateStr) {
    const d = new Date(dateStr);
    return `${d.getMonth()+1}月${d.getDate()}日`;
  },
  formatFull(dateStr) {
    const d = new Date(dateStr);
    return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}`;
  },
  parseLocal(dateStr) { return new Date(dateStr + 'T00:00:00'); }
};

// 货币格式化
const fmtMoney = (n) => '¥' + Number(n).toFixed(2);
const fmtNum = (n) => Number(n).toFixed(1);

// 导出
window.AppData = { STORAGE_KEYS, DEFAULT_FAMILY, DEFAULT_SETTINGS, FOOD_CATEGORIES, NUTRITION_DB, DRIS, Storage, DataManager, NutritionEngine, DateUtils, fmtMoney, fmtNum };
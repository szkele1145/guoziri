import io
import os

BASE = r"C:\Users\一只屑\Desktop\HousewifeAppProto\android-native\app\src\main\java\com\guoziri\app"


def read(name):
    with io.open(os.path.join(BASE, name), encoding="utf-8") as f:
        return f.read()


def write(name, content):
    with io.open(os.path.join(BASE, name), "w", encoding="utf-8", newline="\n") as f:
        f.write(content)
    print("written:", name)


def sub(src, old, new, label):
    assert old in src, "NOT FOUND: " + label
    return src.replace(old, new, 1)


src = read("Store.kt")

# --- StoreRepo: 沙箱模式 + 自定义食物 CRUD ---
old = """    val purchases = mutableStateListOf<Purchase>()
    val diary = mutableStateListOf<DiaryEntry>()
    var settings by mutableStateOf(Settings())
        private set

    init {
        load()
    }"""
new = """    val purchases = mutableStateListOf<Purchase>()
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
    }"""
src = sub(src, old, new, "StoreRepo sandbox")

# --- 写操作在沙箱期间不落盘 ---
old = """    fun addPurchase(p: Purchase) { synchronized(lock) { purchases.add(0, p); writePurchases() } }
    fun updatePurchase(id: String, updated: Purchase) {
        synchronized(lock) {
            val i = purchases.indexOfFirst { it.id == id }
            if (i >= 0) { purchases[i] = updated; writePurchases() }
        }
    }
    fun deletePurchase(id: String) { synchronized(lock) { purchases.removeAll { it.id == id }; writePurchases() } }

    fun addDiary(d: DiaryEntry) { synchronized(lock) { diary.add(0, d); writeDiary() } }
    fun deleteDiary(id: String) { synchronized(lock) { diary.removeAll { it.id == id }; writeDiary() } }"""
new = """    fun addPurchase(p: Purchase) {
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
    }"""
src = sub(src, old, new, "StoreRepo sandbox write guard")

# --- 自定义食物 CRUD ---
old = """    fun clearAll() {"""
new = """    /* ---------------- 自定义食物 ---------------- */
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

    fun clearAll() {"""
src = sub(src, old, new, "StoreRepo custom food CRUD")

write("Store.kt", src)
print("STORE REPO PATCHED")

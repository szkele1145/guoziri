return Result(rounded, details, unmatched, assessment)
    }
}

/* ---------------- 本地 JSON 仓库（线程安全） ---------------- */
class StoreRepo(context: Context) {
    private val gson = Gson()
    private val dir: File = context.filesDir
    private val purchasesFile = File(dir, "purchases.json")
    private val diaryFile = File(dir, "diary.json")
    private val settingsFile = File(dir, "settings.json")
    private val lock = Any()

    val purchases = mutableStateListOf<Purchase>()
    val diary = mutableStateListOf<DiaryEntry>()
    var settings by mutableStateOf(Settings())
        private set

    init {
        load()
    }

    private fun readList(file: File, type: java.lang.reflect.Type): List<*> {
        if (!file.exists()) return emptyList<Any>()
        return try { gson.fromJson(file.readText(StandardCharsets.UTF_8), type) ?: emptyList<Any>() } catch (e: Exception) { emptyList<Any>() }
    }

    fun load() {
        synchronized(lock) {
            val pType = object : TypeToken<List<Purchase>>() {}.type
            val dType = object : TypeToken<List<DiaryEntry>>() {}.type
            val sType = object : TypeToken<Settings>() {}.type
            purchases.clear()
            (readList(purchasesFile, pType) as List<*>).forEach { if (it is Purchase) purchases.add(it) }
            diary.clear()
            (readList(diaryFile, dType) as List<*>).forEach { if (it is DiaryEntry) diary.add(it) }
            settings = try {
                if (settingsFile.exists()) gson.fromJson(settingsFile.readText(StandardCharsets.UTF_8), sType) ?: Settings() else Settings()
            } catch (e: Exception) { Settings() }
        }
    }

    private fun writePurchases() { synchronized(lock) { purchasesFile.writeText(gson.toJson(purchases.toList()), StandardCharsets.UTF_8) } }
    private fun writeDiary() { synchronized(lock) { diaryFile.writeText(gson.toJson(diary.toList()), StandardCharsets.UTF_8) } }
    private fun writeSettings() { synchronized(lock) { settingsFile.writeText(gson.toJson(settings), StandardCharsets.UTF_8) } }

    fun newId() = System.currentTimeMillis().toString()
    fun nowIso(): String = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(Date())

    fun addPurchase(p: Purchase) { synchronized(lock) { purchases.add(0, p); writePurchases() } }
    fun updatePurchase(id: String, updated: Purchase) {
        synchronized(lock) {
            val i = purchases.indexOfFirst { it.id == id }
            if (i >= 0) { purchases[i] = updated; writePurchases() }
        }
    }
    fun deletePurchase(id: String) { synchronized(lock) { purchases.removeAll { it.id == id }; writePurchases() } }

    fun addDiary(d: DiaryEntry) { synchronized(lock) { diary.add(0, d); writeDiary() } }
    fun deleteDiary(id: String) { synchronized(lock) { diary.removeAll { it.id == id }; writeDiary() } }

    fun updateSettings(patch: (Settings) -> Settings) {
        synchronized(lock) { settings = patch(settings); writeSettings() }
        ThemePrefs.theme.value = settings.theme
    }

    fun clearAll() {
        synchronized(lock) {
            purchases.clear(); diary.clear(); writePurchases(); writeDiary()
        }
    }

    fun exportJson(): String = gson.toJson(mapOf(
        "purchases" to purchases.toList(),
        "diary" to diary.toList(),
        "family" to settings.family,
    ))
}
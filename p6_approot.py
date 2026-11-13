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


src = read("AppRoot.kt")

# --- 1) 引导状态 + 锚点 ---
old = """    val snackbarHost = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val chatState = remember { ChatState() }
    val showGuide = !repo.settings.guideDone

    fun toast(msg: String) { scope.launch { snackbarHost.showSnackbar(msg) } }"""
new = """    val snackbarHost = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val chatState = remember { ChatState() }
    val anchors = remember { GuideAnchors() }

    // 引导：首次进入自动跑；设置页「重播新手引导」也能再跑一次
    var showGuide by remember { mutableStateOf(!repo.settings.guideDone) }
    var guideSession by remember { mutableIntStateOf(0) }   // 每次 +1 触发重新开始

    fun toast(msg: String) { scope.launch { snackbarHost.showSnackbar(msg) } }

    fun startGuide() {
        repo.openSandbox()
        repo.loadSandboxDemo()
        showGuide = true
        guideSession += 1
        currentTab = "ledger"
    }

    fun endGuide() {
        repo.closeSandbox()
        showGuide = false
        repo.updateSettings { it.copy(guideDone = true) }
        toast("引导完成，开始使用吧！")
    }"""
src = sub(src, old, new, "AppRoot guide state")

# --- 2) 页面切换时把 anchors 传下去 ---
old = """            when (currentTab) {
                "ledger" -> LedgerPage(repo, onAdd = { editingPurchase = null; showPurchaseSheet = true },
                    onEdit = { editingPurchase = it; showPurchaseSheet = true },
                    onRequestDelete = { showDeleteConfirm = it })
                "nutrition" -> NutritionPage(repo, date = today())
                "ai" -> AiPage(repo, chatState) { toast(it) }
                "settings" -> SettingsPage(repo, { toast(it) }, onReplayGuide = { /* v6: guideDone=false 重启引导 */
                    repo.updateSettings { it.copy(guideDone = false) }
                })
                "diary" -> DiaryPage(repo) { toast(it) }
            }"""
new = """            when (currentTab) {
                "ledger" -> LedgerPage(repo,
                    anchors = anchors,
                    onAdd = { editingPurchase = null; showPurchaseSheet = true },
                    onEdit = { editingPurchase = it; showPurchaseSheet = true },
                    onRequestDelete = { showDeleteConfirm = it })
                "nutrition" -> NutritionPage(repo, date = today(), anchors = anchors)
                "ai" -> AiPage(repo, chatState, anchors) { toast(it) }
                "settings" -> SettingsPage(repo, { toast(it) },
                    anchors = anchors,
                    onReplayGuide = { startGuide() })
                "diary" -> DiaryPage(repo) { toast(it) }
            }"""
src = sub(src, old, new, "AppRoot page dispatch")

# --- 3) FAB 锚点 ---
old = """            if (currentTab == "ledger") {
                FloatingActionButton(
                    onClick = { editingPurchase = null; showPurchaseSheet = true },
                    containerColor = IosBlue, shape = CircleShape,
                    modifier = Modifier.size(56.dp),
                ) { Text("＋", fontSize = 26.sp, color = Color.White) }
            }"""
new = """            if (currentTab == "ledger") {
                FloatingActionButton(
                    onClick = { editingPurchase = null; showPurchaseSheet = true },
                    containerColor = IosBlue, shape = CircleShape,
                    modifier = Modifier.size(56.dp).guideAnchor(anchors, "fab"),
                ) { Text("＋", fontSize = 26.sp, color = Color.White) }
            }"""
src = sub(src, old, new, "AppRoot fab anchor")

# --- 4) 底部导航锚点 ---
old = """        bottomBar = { AppTabBar(currentTab, repo.settings.hiddenEnabled) { currentTab = it } },"""
new = """        bottomBar = {
            AppTabBar(currentTab, repo.settings.hiddenEnabled, anchors) { currentTab = it }
        },"""
src = sub(src, old, new, "AppRoot tabbar anchors")

# --- 5) 引导层 ---
old = """    if (showGuide) {
        GuideOverlay(
            repo = repo,
            onDone = {
                repo.updateSettings { it.copy(guideDone = true) }
            },
        )
    }
}"""
new = """    if (showGuide) {
        GuideOverlay(
            key = guideSession,
            repo = repo,
            anchors = anchors,
            onTabChange = { currentTab = it },
            onOpenPurchaseSheet = { editingPurchase = null; showPurchaseSheet = true },
            onDone = { endGuide() },
        )
    }
}"""
src = sub(src, old, new, "AppRoot guide overlay")

# --- 6) AppTabBar 支持锚点 ---
old = """@Composable
fun AppTabBar(current: String, hiddenEnabled: Boolean, onSelect: (String) -> Unit) {
    data class Tab(val id: String, val label: String)

    val tabs = buildList {
        add(Tab("ledger", "记账")); add(Tab("nutrition", "营养")); add(Tab("ai", "AI"))
        if (hiddenEnabled) add(Tab("diary", "日记"))
        add(Tab("settings", "设置"))
    }
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)) {
        for (t in tabs) {
            NavigationBarItem(
                selected = current == t.id,
                onClick = { onSelect(t.id) },
                icon = { Text(tabIcon(t.id), fontSize = 20.sp) },
                label = { Text(t.label, fontSize = 10.sp) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = IosBlue, selectedTextColor = IosBlue,
                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                ),
            )
        }
    }
}"""
new = """@Composable
fun AppTabBar(
    current: String,
    hiddenEnabled: Boolean,
    anchors: GuideAnchors,
    onSelect: (String) -> Unit,
) {
    data class Tab(val id: String, val label: String)

    val tabs = buildList {
        add(Tab("ledger", "记账")); add(Tab("nutrition", "营养")); add(Tab("ai", "AI"))
        if (hiddenEnabled) add(Tab("diary", "日记"))
        add(Tab("settings", "设置"))
    }
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        modifier = Modifier.guideAnchor(anchors, "tabbar"),
    ) {
        for (t in tabs) {
            NavigationBarItem(
                selected = current == t.id,
                onClick = { onSelect(t.id) },
                icon = { Text(tabIcon(t.id), fontSize = 20.sp) },
                label = { Text(t.label, fontSize = 10.sp) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = IosBlue, selectedTextColor = IosBlue,
                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                ),
            )
        }
    }
}"""
src = sub(src, old, new, "AppRoot AppTabBar anchors")

write("AppRoot.kt", src)
print("APPROOT PATCHED")

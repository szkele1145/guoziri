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

# --- 1) 引导状态：step 提升到 AppRoot ---
old = """    // 引导：首次进入自动跑；设置页「重播新手引导」也能再跑一次
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
new = """    // 引导：首次进入自动跑；设置页「重播新手引导」也能再跑一次
    var showGuide by remember { mutableStateOf(!repo.settings.guideDone) }
    var guideStep by remember { mutableIntStateOf(0) }

    fun toast(msg: String) { scope.launch { snackbarHost.showSnackbar(msg) } }

    fun startGuide() {
        repo.openSandbox()
        repo.loadSandboxDemo()
        guideStep = 0
        showGuide = true
        currentTab = "ledger"
    }

    fun endGuide() {
        repo.closeSandbox()
        showGuide = false
        guideStep = 0
        repo.updateSettings { it.copy(guideDone = true) }
        toast("引导完成，开始使用吧！")
    }

    /** 引导推进：处理切页 / 开弹窗 / 收尾 */
    fun guideNext() {
        val cur = GuideSteps.STEPS[guideStep]
        if (guideStep >= GuideSteps.size - 1) { endGuide(); return }
        val next = guideStep + 1
        val ns = GuideSteps.STEPS[next]
        guideStep = next
        if (ns.inSheet) {
            // 在弹窗里展示，先把弹窗关掉再由下一步打开
            showPurchaseSheet = false
            editingPurchase = null
        } else {
            showPurchaseSheet = false
            ns.tab?.let { currentTab = it }
        }
    }"""
src = sub(src, old, new, "AppRoot guide step state")

# --- 2) 当前步骤在弹窗内 → 打开弹窗 ---
old = """    if (showPurchaseSheet) {
        PurchaseSheet(repo, editingPurchase, anchors, onDismiss = { showPurchaseSheet = false }) { toast(it) }
    }"""
new = """    // 引导走到「添加记录」这一步时自动打开表单
    val guideInSheet = showGuide && GuideSteps.STEPS.getOrNull(guideStep)?.inSheet == true
    LaunchedEffect(guideInSheet) {
        if (guideInSheet) { editingPurchase = null; showPurchaseSheet = true }
    }

    if (showPurchaseSheet) {
        PurchaseSheet(
            repo = repo,
            editing = editingPurchase,
            anchors = anchors,
            guideStep = if (guideInSheet) guideStep else null,
            onNext = { guideNext() },
            onSkip = { endGuide() },
            onDismiss = { showPurchaseSheet = false },
        ) { toast(it) }
    }"""
src = sub(src, old, new, "AppRoot purchase sheet guide")

# --- 3) 引导遮罩层 ---
old = """    if (showGuide) {
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
new = """    // 弹窗内的那一步由 PurchaseSheet 自己画引导卡，这里不叠全局遮罩
    val guideOverlayVisible = showGuide && GuideSteps.STEPS.getOrNull(guideStep)?.inSheet != true
    if (guideOverlayVisible) {
        GuideOverlay(
            step = guideStep,
            anchors = anchors,
            scope = currentTab,
            onNext = { guideNext() },
            onSkip = { endGuide() },
        )
    }
}"""
src = sub(src, old, new, "AppRoot guide overlay")

write("AppRoot.kt", src)
print("APPROUT GUIDE STATE PATCHED")

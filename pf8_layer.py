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


# =====================================================================
# 1) GuideOverlay: 拦截层与卡片分层，卡片按钮才能点
# =====================================================================
src = read("GuideOverlay.kt")
old = """    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.42f))
            // 拦截触摸，防止穿透到底层页面误操作
            .pointerInput(Unit) {
                detectTapGestures { /* 吞掉 */ }
            },
    ) {
        // ---- 聚光挖洞 ----
        if (targetRect != null && ringAlpha > 0f) {
            SpotlightOverlay(rect = targetRect, alpha = ringAlpha, modifier = Modifier.fillMaxSize())
        }
"""
new = """    Box(Modifier.fillMaxSize()) {
        // ---- 遮罩 + 点击拦截 ----
        // 拦截层单独一层，放在卡片下面：卡片按钮才点得动，
        // 同时防止触摸穿透到底层页面误操作。
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.42f))
                .pointerInput(Unit) {
                    detectTapGestures { /* 吞掉 */ }
                },
        )

        // ---- 聚光挖洞 ----
        if (targetRect != null && ringAlpha > 0f) {
            SpotlightOverlay(rect = targetRect, alpha = ringAlpha, modifier = Modifier.fillMaxSize())
        }
"""
src = sub(src, old, new, "GuideOverlay layer split")
write("GuideOverlay.kt", src)

# =====================================================================
# 2) AppRoot: 首次进入引导时注入沙箱演示数据
# =====================================================================
src = read("AppRoot.kt")
old = """    fun toast(msg: String) { scope.launch { snackbarHost.showSnackbar(msg) } }
"""
new = """    fun toast(msg: String) { scope.launch { snackbarHost.showSnackbar(msg) } }

    // 首次进入引导时先把沙箱演示数据准备好（真实数据被快照保护）
    LaunchedEffect(Unit) {
        if (!repo.settings.guideDone && !repo.sandboxActive) {
            repo.openSandbox()
            repo.loadSandboxDemo()
        }
    }
"""
src = sub(src, old, new, "AppRoot sandbox init")

# 沙箱开启时不要重复 open（startGuide 里已经调了）
old = """    fun startGuide() {
        repo.openSandbox()
        repo.loadSandboxDemo()
        guideStep = 0
        showGuide = true
        currentTab = "ledger"
    }"""
new = """    fun startGuide() {
        repo.openSandbox()
        repo.loadSandboxDemo()
        guideStep = 0
        showGuide = true
        currentTab = "ledger"
    }

    /** 引导结束时把真实数据放回去 */
    fun endGuideKeepSandbox() {
        repo.closeSandbox()
        showGuide = false
        guideStep = 0
    }"""
src = sub(src, old, new, "AppRoot startGuide")

write("AppRoot.kt", src)
print("FIXED")

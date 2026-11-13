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


src = read("PurchaseSheet.kt")

# --- 1) 参数 ---
old = """fun PurchaseSheet(
    repo: StoreRepo,
    editing: Purchase?,                 // null = 新增
    anchors: GuideAnchors,
    onDismiss: () -> Unit,
    snackbar: (String) -> Unit,
) {"""
new = """fun PurchaseSheet(
    repo: StoreRepo,
    editing: Purchase?,                 // null = 新增
    anchors: GuideAnchors,
    guideStep: Int? = null,             // 非 null 时在弹窗内显示引导
    onNext: (() -> Unit)? = null,
    onSkip: (() -> Unit)? = null,
    onDismiss: () -> Unit,
    snackbar: (String) -> Unit,
) {"""
src = sub(src, old, new, "PurchaseSheet guide params")

# --- 2) 弹窗内容包一层 Box，高亮 + 引导卡 ---
old = """    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(bottom = 20.dp)) {"""
new = """    val gStep = guideStep
    val gTarget = if (gStep != null) GuideSteps.STEPS.getOrNull(gStep)?.target else null
    val gRect = if (gTarget != null) anchors.rect(gTarget, "purchase") else null

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Box(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(bottom = if (gStep != null) 190.dp else 20.dp)) {"""
src = sub(src, old, new, "PurchaseSheet box wrap")

# --- 3) 弹窗末尾加引导卡 ---
old = """            Text("总价/单价、斤/克，填一组就能自动算出营养",
                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp), textAlign = TextAlign.Center)
        }
    }"""
new = """            Text("总价/单价、斤/克，填一组就能自动算出营养",
                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp), textAlign = TextAlign.Center)
        }

        // 弹窗内的引导层：聚光高亮 + 提示卡
        if (gStep != null) {
            if (gRect != null) {
                SpotlightOverlay(rect = gRect, alpha = 1f, modifier = Modifier.matchParentSize())
            }
            Box(
                Modifier
                    .matchParentSize()
                    .background(Color.Black.copy(alpha = 0.30f))
                    .pointerInput(Unit) { detectTapGestures { /* 吞掉点击 */ } },
            )
            GuideCard(
                step = gStep,
                total = GuideSteps.size,
                title = GuideSteps.STEPS[gStep].title,
                body = GuideSteps.STEPS[gStep].body,
                onSkip = { onSkip?.invoke() },
                onNext = { onNext?.invoke() },
                isLast = gStep >= GuideSteps.size - 1,
                modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
            )
        }
        }
    }"""
src = sub(src, old, new, "PurchaseSheet guide card")

# --- 4) 存储按钮锚点 scope 改成 purchase ---
src = sub(
    src,
    '}, modifier = Modifier.guideAnchor(anchors, "purchase-save")) {',
    '}, modifier = Modifier.guideAnchor(anchors, "purchase-save", "purchase")) {',
    "PurchaseSheet save anchor scope",
)

# --- 5) 需要的新导入 ---
old = "import androidx.compose.ui.text.TextStyle\n"
new = """import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.graphics.Color
"""
src = sub(src, old, new, "PurchaseSheet imports")

write("PurchaseSheet.kt", src)
print("PURCHASE SHEET GUIDE PATCHED")

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


# ---------------- AppRoot: 锚点带 scope ----------------
src = read("AppRoot.kt")
src = sub(
    src,
    'modifier = Modifier.size(56.dp).guideAnchor(anchors, "fab"),',
    'modifier = Modifier.size(56.dp).guideAnchor(anchors, "fab", currentTab),',
    "AppRoot fab scope",
)
src = sub(
    src,
    'modifier = Modifier.guideAnchor(anchors, "tabbar"),',
    'modifier = Modifier.guideAnchor(anchors, "tabbar", "tabbar"),',
    "AppRoot tabbar scope",
)
write("AppRoot.kt", src)

# ---------------- LedgerPage: fab 用 scope ----------------
src = read("LedgerPage.kt")
src = sub(
    src,
    "fun LedgerPage(\n    repo: StoreRepo,\n    anchors: GuideAnchors,",
    "fun LedgerPage(\n    repo: StoreRepo,\n    anchors: GuideAnchors,",
    "LedgerPage signature",
)
write("LedgerPage.kt", src)

# ---------------- NutritionPage: ring scope ----------------
src = read("NutritionPage.kt")
src = sub(
    src,
    'Box(Modifier.size(96.dp).guideAnchor(anchors, "nutrition-ring"), contentAlignment = Alignment.Center) {',
    'Box(Modifier.size(96.dp).guideAnchor(anchors, "nutrition-ring", "nutrition"), contentAlignment = Alignment.Center) {',
    "NutritionPage ring scope",
)
write("NutritionPage.kt", src)

# ---------------- AiPage: quick scope ----------------
src = read("AiPage.kt")
src = sub(
    src,
    'anchorId = "ai-quick", anchors = anchors) {',
    'anchorId = "ai-quick", anchors = anchors, anchorScope = "ai") {',
    "AiPage quick scope",
)
src = sub(
    src,
    "    anchorId: String? = null,\n    anchors: GuideAnchors? = null,\n    onClick: () -> Unit,",
    "    anchorId: String? = null,\n    anchors: GuideAnchors? = null,\n    anchorScope: String = \"ai\",\n    onClick: () -> Unit,",
    "AiPage AiCard anchorScope param",
)
src = sub(
    src,
    ".let { m -> if (anchorId != null && anchors != null) m.guideAnchor(anchors, anchorId) else m },",
    ".let { m -> if (anchorId != null && anchors != null) m.guideAnchor(anchors, anchorId, anchorScope) else m },",
    "AiPage AiCard anchor call",
)
write("AiPage.kt", src)

# ---------------- SettingsPage: key scope ----------------
src = read("SettingsPage.kt")
src = sub(
    src,
    'GroupCard(Modifier.guideAnchor(anchors, "settings-key")) {',
    'GroupCard(Modifier.guideAnchor(anchors, "settings-key", "settings")) {',
    "SettingsPage key scope",
)
write("SettingsPage.kt", src)

print("SCOPES PATCHED")

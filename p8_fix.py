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
src = sub(
    src,
    "        PurchaseSheet(repo, editingPurchase, onDismiss = { showPurchaseSheet = false }) { toast(it) }",
    "        PurchaseSheet(repo, editingPurchase, anchors, onDismiss = { showPurchaseSheet = false }) { toast(it) }",
    "AppRoot PurchaseSheet call",
)
write("AppRoot.kt", src)

# 检查 PurchaseSheet 里 TextButton 的 modifier 是否合法
src = read("PurchaseSheet.kt")
print("--- 存储按钮上下文 ---")
i = src.find('purchase-save')
print(src[max(0, i - 700):i + 200])

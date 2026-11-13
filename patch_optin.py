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


# Chat.kt: 给 ChatSheet 加 OptIn
src = read("Chat.kt")
src = sub(
    src,
    "@Composable\nfun ChatSheet(",
    "@OptIn(ExperimentalMaterial3Api::class)\n@Composable\nfun ChatSheet(",
    "Chat OptIn",
)
write("Chat.kt", src)

# PurchaseSheet.kt: 给 PurchaseSheet 加 OptIn
src = read("PurchaseSheet.kt")
src = sub(
    src,
    "@Composable\nfun PurchaseSheet(",
    "@OptIn(ExperimentalMaterial3Api::class)\n@Composable\nfun PurchaseSheet(",
    "PurchaseSheet OptIn",
)
write("PurchaseSheet.kt", src)

print("OPIN PATCHED")

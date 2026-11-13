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


# ---------------- Chat.kt: toMediaType 扩展 ----------------
src = read("Chat.kt")
src = sub(
    src,
    "import okhttp3.MediaType\n",
    "import okhttp3.MediaType\nimport okhttp3.MediaType.Companion.toMediaType\n",
    "Chat toMediaType import",
)
src = sub(
    src,
    '.post(RequestBody.create(MediaType.parse("application/json"), body.toString()))',
    '.post(RequestBody.create("application/json".toMediaType(), body.toString()))',
    "Chat toMediaType usage",
)
write("Chat.kt", src)

# ---------------- SettingsPage.kt: padding(horizontal, top) 非法 ----------------
src = read("SettingsPage.kt")
src = sub(
    src,
    "modifier = Modifier.padding(horizontal = 20.dp, top = 8.dp))",
    "modifier = Modifier.padding(start = 20.dp, top = 8.dp, end = 20.dp))",
    "SettingsPage padding",
)
write("SettingsPage.kt", src)

print("FINAL PATCHES APPLIED")

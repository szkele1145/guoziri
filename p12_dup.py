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
src = sub(
    src,
    """import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)""",
    """import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)""",
    "dup Color",
)
# 去掉重复的 Color 导入行（保留第一处）
lines = src.split("\n")
seen = False
out = []
for ln in lines:
    if ln.strip() == "import androidx.compose.ui.graphics.Color":
        if seen:
            continue
        seen = True
    out.append(ln)
src = "\n".join(out)
write("PurchaseSheet.kt", src)
print("OK")

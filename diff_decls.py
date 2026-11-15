import subprocess
import re
import os

REPO = r"C:\Users\一只屑\Desktop\HousewifeAppProto"
REL = "android-native/app/src/main/java/com/guoziri/app"
BASE_COMMIT = "d593a4b"

FILES = [
    "AiPage.kt", "App.kt", "AppIcons.kt", "AppRoot.kt", "Chat.kt", "Colors.kt",
    "Common.kt", "DatePickerSheet.kt", "DiaryPage.kt", "LedgerPage.kt",
    "MainActivity.kt", "NutritionPage.kt", "PurchaseSheet.kt", "SettingsPage.kt",
    "Store.kt", "ThemePrefs.kt",
]

DECL = re.compile(
    r"^(?:@Composable\s*)?(?:private\s+)?(?:data\s+)?(?:class|object|fun|val|var)\s+([A-Za-z_]\w*)",
    re.M,
)


def decls(text):
    out = set()
    for m in DECL.finditer(text):
        out.add(m.group(1))
    # 多行 fun：匹配 "fun Name(" 任意位置
    for m in re.finditer(r"^\s*(?:@Composable\s*)?(?:private\s+)?fun\s+([A-Za-z_]\w*)", text, re.M):
        out.add(m.group(1))
    for m in re.finditer(r"^\s*(?:data\s+)?class\s+([A-Za-z_]\w*)", text, re.M):
        out.add(m.group(1))
    return out


def git_show(path):
    try:
        r = subprocess.run(
            ["git", "show", f"{BASE_COMMIT}:{path}"],
            cwd=REPO, capture_output=True, text=True, encoding="utf-8",
        )
        return r.stdout if r.returncode == 0 else ""
    except Exception:
        return ""


for f in FILES:
    old = decls(git_show(f"{REL}/{f}"))
    local_path = os.path.join(REPO, REL.replace("/", "\\"), f)
    if os.path.exists(local_path):
        with open(local_path, encoding="utf-8") as fh:
            new = decls(fh.read())
    else:
        new = set()

    missing = sorted(old - new)
    added = sorted(new - old)
    if missing or added:
        print(f"--- {f}")
        if missing:
            print(f"    丢失: {missing}")
        if added:
            print(f"    新增: {added}")

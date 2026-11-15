import io
import os
import re
import glob

BASE = r"C:\Users\一只屑\Desktop\HousewifeAppProto\android-native\app\src\main\java\com\guoziri\app"
files = glob.glob(os.path.join(BASE, "*.kt")) + glob.glob(os.path.join(BASE, "ui", "theme", "*.kt"))

MAP = {
    "Icons.Rounded.ShoppingCart": "AppIcons.Other",
    "Icons.Rounded.Receipt": "AppIcons.Ledger",
    "Icons.Rounded.KeyboardArrowRight": "AppIcons.ChevronRight",
    "Icons.Rounded.KeyboardArrowLeft": "AppIcons.ChevronLeft",
    "Icons.Rounded.ChevronRight": "AppIcons.ChevronRight",
    "Icons.Rounded.HelpOutline": "AppIcons.Help",
    "Icons.Rounded.Help": "AppIcons.Help",
    "Icons.Rounded.Restaurant": "AppIcons.Restaurant",
    "Icons.Rounded.Payments": "AppIcons.Money",
    "Icons.Rounded.MenuBook": "AppIcons.Book",
    "Icons.Rounded.Book": "AppIcons.Book",
    "Icons.Rounded.ChatBubbleOutline": "AppIcons.Chat",
    "Icons.Rounded.Check": "AppIcons.Check",
    "Icons.Rounded.Schedule": "AppIcons.Clock",
    "Icons.Rounded.ArrowUpward": "AppIcons.Send",
    "Icons.Rounded.Remove": "AppIcons.Remove",
    "Icons.Rounded.Add": "AppIcons.Add",
    "Icons.Rounded.Eco": "AppIcons.Leaf",
    "Icons.Rounded.List": "AppIcons.Ledger",
    "Icons.Rounded.Warning": "AppIcons.Warning",
    "Icons.Rounded.Edit": "AppIcons.Edit",
    "Icons.Rounded.Delete": "AppIcons.Delete",
    "Icons.Rounded.MoreVert": "AppIcons.More",
    "Icons.Rounded.ExpandMore": "AppIcons.ChevronDown",
    "Icons.Rounded.Search": "AppIcons.Search",
    "Icons.Rounded.Close": "AppIcons.Close",
    "Icons.Rounded.DateRange": "AppIcons.Calendar",
    "Icons.Rounded.Star": "AppIcons.Ai",
    "Icons.Rounded.Info": "AppIcons.Help",
    "Icons.Rounded.Refresh": "AppIcons.Send",
    "Icons.Rounded.Bookmark": "AppIcons.Book",
    "Icons.Rounded.AccessTime": "AppIcons.Clock",
    "Icons.Rounded.WarningAmber": "AppIcons.Warning",
    "Icons.Rounded.Payments": "AppIcons.Money",
    "Icons.Rounded.CalendarMonth": "AppIcons.Calendar",
    "Icons.Rounded.TrendingUp": "AppIcons.Warning",
    "Icons.Rounded.Settings": "AppIcons.Settings",
    "Icons.Rounded.Receipt": "AppIcons.Ledger",
}

# 长键优先，避免前缀误伤
keys = sorted(MAP.keys(), key=len, reverse=True)

changed = []
for f in files:
    if os.path.basename(f) == "AppIcons.kt":
        continue
    with io.open(f, encoding="utf-8") as fh:
        t = fh.read()
    orig = t
    for k in keys:
        t = t.replace(k, MAP[k])
    # 清理不再使用的 material icons 导入
    if "Icons." not in t:
        t = re.sub(r"^import androidx\.compose\.material\.icons\.Icons\s*$\n", "", t, flags=re.M)
        t = re.sub(r"^import androidx\.compose\.material\.icons\.rounded\.\*\s*$\n", "", t, flags=re.M)
        t = re.sub(r"^import androidx\.compose\.material\.icons\.rounded\.[A-Za-z]+\s*$\n", "", t, flags=re.M)
        t = re.sub(r"^import androidx\.compose\.material\.icons\.filled\.\*\s*$\n", "", t, flags=re.M)
    if t != orig:
        with io.open(f, "w", encoding="utf-8", newline="\n") as fh:
            fh.write(t)
        changed.append(os.path.basename(f))

print("已更新:", ", ".join(changed))

# 检查残留
print()
print("=== 残留的 Icons.Rounded 引用 ===")
found = False
for f in files:
    with io.open(f, encoding="utf-8") as fh:
        for i, line in enumerate(fh, 1):
            if "Icons.Rounded." in line or "Icons.Filled." in line:
                print("  %s:%d %s" % (os.path.basename(f), i, line.strip()[:110]))
                found = True
if not found:
    print("  无")

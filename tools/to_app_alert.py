# -*- coding: utf-8 -*-
"""把两处 AlertDialog 换成自绘 AppAlert（对齐 web 版 .alert）"""
import io
import os

BASE = r"C:\Users\一只屑\Desktop\HousewifeAppProto\android-native\app\src\main\java\com\guoziri\app"


def load(name):
    p = os.path.join(BASE, name)
    with io.open(p, encoding="utf-8") as f:
        return p, f.read()


def save(p, t):
    with io.open(p, "w", encoding="utf-8", newline="\n") as f:
        f.write(t)


# ---- AppRoot：删除确认 ----
p, t = load("AppRoot.kt")
start = t.find("    showDeleteConfirm?.let { p ->")
end = t.find("\n    }\n", start) + len("\n    }\n")
if start < 0 or end <= start:
    print("  AppRoot: 未定位删除确认块")
else:
    new = '''    showDeleteConfirm?.let { p ->
        AppAlert(
            title = "删除这条记录？",
            message = p.name,
            confirmText = "删除",
            dismissText = "取消",
            danger = true,
            onConfirm = {
                repo.deletePurchase(p.id)
                showDeleteConfirm = null
                toast("已删除")
            },
            onDismiss = { showDeleteConfirm = null },
        )
    }
'''
    t = t[:start] + new + t[end:]
    save(p, t)
    print("  AppRoot: 删除确认改为 AppAlert")

# ---- SettingsPage：清空数据 ----
p, t = load("SettingsPage.kt")
start = t.find("    if (showClear) {")
end = t.find("\n    }\n", start) + len("\n    }\n")
if start < 0 or end <= start:
    print("  SettingsPage: 未定位清空确认块")
else:
    new = '''    if (showClear) {
        AppAlert(
            title = "清空全部数据？",
            message = "买菜记录和日记都会删除，无法恢复。",
            confirmText = "清空",
            dismissText = "取消",
            danger = true,
            onConfirm = {
                repo.clearAll()
                showClear = false
                snackbar("已清空")
            },
            onDismiss = { showClear = false },
        )
    }
'''
    t = t[:start] + new + t[end:]
    save(p, t)
    print("  SettingsPage: 清空确认改为 AppAlert")

print("完成")

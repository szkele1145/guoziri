# -*- coding: utf-8 -*-
"""根据 web 版实测计算样式修正 4 处偏差"""
import io
import os

BASE = r"C:\Users\一只屑\Desktop\HousewifeAppProto\android-native\app\src\main\java\com\guoziri\app"


def edit(name, pairs):
    p = os.path.join(BASE, name)
    with io.open(p, encoding="utf-8") as f:
        t = f.read()
    for old, new in pairs:
        if old not in t:
            print("  [跳过] %s: %r" % (name, old[:50]))
            continue
        t = t.replace(old, new, 1)
        print("  [OK] %s: %r" % (name, old[:44]))
    with io.open(p, "w", encoding="utf-8", newline="\n") as f:
        f.write(t)


# 1) AI 卡：CSS min-height:68 是 content-box，含 padding 后总高 96
edit("AiPage.kt", [
    (".defaultMinSize(minHeight = 68.dp)",
     ".defaultMinSize(minHeight = 96.dp)   // .ai-card：content-box 68 + padding 28"),
])

# 2) 记账页：cell-icon 字号实测 16px；编辑按钮实测 44x44
edit("LedgerPage.kt", [
    ("                iconSize = 17.dp,", "                iconSize = 16.dp,"),
    ('''            Text(
                "编辑",
                fontSize = Dimens2.Action,
                color = LocalAppColors.current.tint,
                modifier = Modifier
                    .padding(start = 2.dp, end = 2.dp)
                    .clickable { onEdit() }
                    .padding(horizontal = 8.dp, vertical = 6.dp),
            )''',
     '''            // .cell-edit：min 44x44 点击区，字号 15
            Box(
                Modifier
                    .size(44.dp)
                    .clickable { onEdit() },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "编辑",
                    fontSize = Dimens2.Action,
                    color = LocalAppColors.current.tint,
                )
            }'''),
])

# 3) BarRow 的值允许换行（实测 92px 宽下会折成两行）
edit("Common.kt", [
    ('''        Text(
            valueText,
            fontSize = Dimens2.BarValue,
            color = SemanticColors.TextSecondary,
            textAlign = TextAlign.End,
            style = NumberStyle,
            maxLines = 1,
            modifier = Modifier.width(92.dp),
        )''',
     '''        Text(
            valueText,
            fontSize = Dimens2.BarValue,
            color = SemanticColors.TextSecondary,
            textAlign = TextAlign.End,
            style = NumberStyle,
            maxLines = 2,
            lineHeight = (Dimens2.BarValue.value * 1.45f).sp,
            modifier = Modifier.width(92.dp),
        )'''),
])

# 4) 日记页图标字号同样 16
edit("DiaryPage.kt", [
    ("                                    modifier = Modifier.size(17.dp),",
     "                                    modifier = Modifier.size(16.dp),"),
])

print("完成")

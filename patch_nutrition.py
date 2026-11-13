import io

p = r"C:\Users\一只屑\Desktop\HousewifeAppProto\android-native\app\src\main\java\com\guoziri\app\NutritionPage.kt"

with io.open(p, encoding="utf-8") as f:
    src = f.read()

old = (
    '        GroupTitle("总评")\n'
    '        GroupCard {\n'
    '            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {\n'
    '                Box(Modifier.size(96.dp), contentAlignment = Alignment.Center) {\n'
    '                    androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {\n'
    '                        val stroke = 20f\n'
    '                        drawArc(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),\n'
)

new = (
    '        GroupTitle("总评")\n'
    '        GroupCard {\n'
    '            val trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)\n'
    '            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {\n'
    '                Box(Modifier.size(96.dp), contentAlignment = Alignment.Center) {\n'
    '                    androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {\n'
    '                        val stroke = 20f\n'
    '                        drawArc(color = trackColor,\n'
)

assert old in src, "pattern not found"
src = src.replace(old, new, 1)

with io.open(p, "w", encoding="utf-8", newline="\n") as f:
    f.write(src)

print("patched OK")

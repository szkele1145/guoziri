import io
p = r'C:\Users\一只屑\Desktop\HousewifeAppProto\web\app.js'
s = io.open(p, encoding='utf-8').read()

# spotlight 结构：加独立 shade 层（洞区域镂空由 shade 的 clip-path 实现？简单起见 shade 仍全屏，
# 但 hole 在 shade 之上用 mix-blend? 太复杂。方案：shade 全屏 auto 拦截，hole 覆盖在 shade 上且 none，
# 点击洞区 → hole(none) → shade(auto) 拦截了！不行。
# 正解：shade 不全屏，而是 4 块矩形（上下左右）拼出洞的镂空，全部 pointer-events:auto。
s = s.replace(
    \"'        <div class=\\\"guide-hole\\\"></div>' +\",
    \"'        <div class=\\\"guide-shade g-top\\\"></div>' +\\n        '        <div class=\\\"guide-shade g-bottom\\\"></div>' +\\n        '        <div class=\\\"guide-shade g-left\\\"></div>' +\\n        '        <div class=\\\"guide-shade g-right\\\"></div>' +\"
)
io.open(p, 'w', encoding='utf-8', newline='\n').write(s)
print('structure patched')

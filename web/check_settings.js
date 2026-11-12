// 设置页绑定核对（PowerShell 正则转义太折腾，用 Node）
const fs = require("fs");
const src = fs.readFileSync("app.js", "utf8");

let fail = 0;
for (const id of ["preset-group", "set-sort", "set-hidden", "set-theme-cell", "btn-models", "btn-export", "btn-clear"]) {
  const bound = src.includes(`$("#${id}").addEventListener`) || src.includes(`autoSave("#${id}"`);
  console.log((bound ? "OK   " : "FAIL ") + "#" + id);
  if (!bound) fail++;
}
// autoSave 目标
for (const [id, field] of [["set-key", "apiKey"], ["set-model", "apiModel"]]) {
  const bound = src.includes(`autoSave("#${id}", "${field}")`);
  console.log((bound ? "OK   " : "FAIL ") + `#${id} autoSave(${field})`);
  if (!bound) fail++;
}
// renderSettings 里绑定段是否在 el.innerHTML 赋值之后（顺序 bug 检查）
const fn = src.slice(src.indexOf("function renderSettings"), src.indexOf("/* ---------------- AI 对话"));
const htmlSet = fn.indexOf("el.innerHTML = html;");
const firstBind = fn.indexOf("addEventListener");
console.log((firstBind > htmlSet ? "OK   " : "FAIL ") + "绑定在 innerHTML 赋值之后");
if (firstBind <= htmlSet) fail++;

// preset-group 的 click 委托：data-preset cell 在 innerHTML 里确实存在
console.log((fn.includes("data-preset=") ? "OK   " : "FAIL ") + "data-preset cell 已渲染");
if (!fn.includes("data-preset=")) fail++;

process.exit(fail ? 1 : 0);

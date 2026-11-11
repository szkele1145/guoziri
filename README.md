# 过日子 · 项目总览

家庭主妇专用：记账 → 营养评估 → AI 建议 → 日常记录

```
HousewifeAppProto/
├── backend/          Python Flask 后端（打进 APK，127.0.0.1:5000）
│   ├── app.py           全部 API：记账/日记/设置/营养/AI转发
│   ├── nutrition.py     食物成分表 + 膳食指南计算
│   ├── run_server.py    Android 专用入口
│   ├── test_api.py      35 项接口测试（全通过）
│   └── data/            运行时 JSON 数据（桌面调试用；APK 里在系统私有目录）
├── web/              前端（iOS 风格，HarmonyOS Sans）
│   ├── index.html
│   ├── style.css
│   ├── app.js            fetch('/api/...') 调后端
│   └── fonts/            HarmonyOS_Sans_SC.ttf
├── android/          Android 工程（Chaquopy），用 Studio 打开即可打包
├── backend/启动后端.bat   桌面调试：起服务+开浏览器
└── styles/ launcher.html 等   早期 4 风格原型（已废弃，仅存档）
```

## 桌面调试
```
backend\启动后端.bat   →   浏览器开 http://127.0.0.1:5000
```

## 打 APK
见 android/README.md（需要装 Android Studio 的电脑，点一次 Run 即可）

## 安全约定
- API Key 只存后端私有目录，接口永不回传，不写日志
- AI 请求由后端转发，前端只跟 127.0.0.1 通信
- 导出备份不含 Key

# 过日子 · Android APK 打包指南

## 前置条件（只需打包机上装一次）

1. **Android Studio**（Hedgehog 以上）：https://developer.android.com/studio
2. Android SDK：装完 Studio 首次启动会提示装（选 API 34）
3. Chaquopy 插件走 Maven 自动下载，无需手动装

## 步骤

1. 用 Android Studio 打开 `android/` 目录（File → Open）
2. 等 Gradle Sync 完成（首次 5-15 分钟，要下 Python 交叉编译工具链）
3. 插手机（开发者模式 + USB 调试）或建模拟器
4. 点 Run ▶ 装到手机
5. 正式包：菜单 Build → Generate Signed App Bundle / APK → 选 APK →
   没有签名就点 Create new 建一个 keystore（自己用，随便设密码）→ release

产物在 `android/app/build/outputs/apk/release/app-release.apk`

## 结构对应关系

```
android/
├── app/build.gradle            ← Chaquopy 配置：pip 装 flask/requests
├── app/src/main/
│   ├── AndroidManifest.xml     ← 仅 INTERNET 权限
│   └── java/com/guoziri/app/
│       ├── App.java            ← Application，最早启动 Python
│       ├── MainActivity.java   ← 起 Flask(127.0.0.1:5000) + WebView 加载
│       └── Assets.java         ← 把 assets/web 解压到私有目录给 Flask
backend/                        ← Python 源码原样打进 APK（Chaquopy 读 ../../backend）
  └── run_server.py             ← Android 专用入口：接收 dataDir/webDir
web/                            ← 前端原样打进 assets
```

## 数据位置（手机上）

`/data/data/com.guoziri.app/files/` —— 应用私有目录：
- `purchases.json` / `diary.json` / `settings.json`（含 API Key）
- 系统级隔离，其他 App 读不到；卸载 App 才会删
- 备份用页面里的「导出备份」

## 修改代码后重新打包

- 改了 `web/` 或 `backend/` → Android Studio 里 Build → Rebuild Project
- Chaquopy 会自动重新同步 Python 代码进 APK

## 常见问题

| 问题 | 处理 |
|---|---|
| Sync 报 chaquo.com 连不上 | 检查代理；settings.gradle.kts 里的 maven 仓库地址 |
| pip 依赖装不上 | Chaquopy 有自己的预编译 wheel 源，flask/requests 都是支持的 |
| 打开 App 白屏 | 看 Logcat 过滤 `guoziri`，多半是端口被占或 Python 报错 |
| 真机不识别 | 手机开发者选项里开 USB 调试；Windows 装 OEM USB 驱动 |

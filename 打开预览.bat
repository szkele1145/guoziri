@echo off
chcp 65001 >nul
cd /d "%~dp0"
echo.
echo   过日子 - 本地预览
echo   ==================
echo.
echo   正在打开界面选择页...
start "" "%~dp0launcher.html"
echo.
echo   如果浏览器没有自动打开，请手动双击 launcher.html
echo.
timeout /t 3 >nul

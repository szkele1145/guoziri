@echo off
chcp 65001 >nul
cd /d "%~dp0"
echo.
echo   过日子 · 本地后端
echo   ========================
echo.
echo   正在启动服务...
echo   启动后浏览器会自动打开 http://127.0.0.1:5000
echo.
echo   关闭本窗口 = 停止服务
echo.
timeout /t 2 >nul
start "" "http://127.0.0.1:5000"
python app.py
pause

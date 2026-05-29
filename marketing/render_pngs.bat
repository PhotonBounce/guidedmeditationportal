@echo off
REM =========================================================================
REM  Render SVG / HTML marketing assets to PNG using headless Edge.
REM  Microsoft Edge ships with every Windows 10/11 install - zero setup.
REM =========================================================================
setlocal enableextensions
cd /d "%~dp0"

set "EDGE=%ProgramFiles(x86)%\Microsoft\Edge\Application\msedge.exe"
if not exist "%EDGE%" set "EDGE=%ProgramFiles%\Microsoft\Edge\Application\msedge.exe"
if not exist "%EDGE%" (
    echo [ERROR] Microsoft Edge not found. This script needs Edge to render PNGs.
    pause & exit /b 1
)

echo === Rendering marketing PNGs ===
echo.

echo [1/5] icon_512.png
"%EDGE%" --headless --disable-gpu --hide-scrollbars --screenshot="%CD%\icon_512.png" --window-size=512,512 "file:///%CD:\=/%/icon_512.svg" >nul 2>&1

echo [2/5] feature_graphic.png
"%EDGE%" --headless --disable-gpu --hide-scrollbars --screenshot="%CD%\feature_graphic.png" --window-size=1024,500 "file:///%CD:\=/%/feature_graphic.svg" >nul 2>&1

echo [3/5] screenshot_1_main.png
"%EDGE%" --headless --disable-gpu --hide-scrollbars --screenshot="%CD%\screenshot_1_main.png" --window-size=1080,1920 "file:///%CD:\=/%/shot_main.html" >nul 2>&1

echo [4/5] screenshot_2_premium.png
"%EDGE%" --headless --disable-gpu --hide-scrollbars --screenshot="%CD%\screenshot_2_premium.png" --window-size=1080,1920 "file:///%CD:\=/%/shot_premium.html" >nul 2>&1

echo [5/5] screenshot_3_timer.png
"%EDGE%" --headless --disable-gpu --hide-scrollbars --screenshot="%CD%\screenshot_3_timer.png" --window-size=1080,1920 "file:///%CD:\=/%/shot_timer.html" >nul 2>&1

echo.
echo === DONE === 5 PNGs ready in marketing\
echo   icon_512.png            (Play Console: App icon)
echo   feature_graphic.png     (Play Console: Feature graphic)
echo   screenshot_1_main.png   (Play Console: Phone screenshot 1)
echo   screenshot_2_premium.png(Play Console: Phone screenshot 2)
echo   screenshot_3_timer.png  (Play Console: Phone screenshot 3)
echo.
pause

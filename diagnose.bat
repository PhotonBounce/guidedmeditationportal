@echo off
REM =========================================================================
REM  SoundPad - diagnostic helper (ASCII-only)
REM  Double-click and tell me what it prints.
REM =========================================================================
setlocal enableextensions
cd /d "%~dp0"

if exist ".tools\env.bat" call ".tools\env.bat"

echo.
echo  =================================================================
echo    SoundPad - diagnostic report
echo  =================================================================
echo.

echo --- Java -----------------------------------------------------------
where java 2>nul
java -version 2>&1
echo.

echo --- adb ------------------------------------------------------------
where adb 2>nul
echo.

echo --- Phones adb can see ---------------------------------------------
adb start-server 2>nul
adb devices -l
echo.
echo (Above must list at least one device with state "device".)
echo (If state is "unauthorized" you need to tap "Allow" on the phone.)
echo (If empty: USB Debugging is off, cable is charge-only, or driver missing.)
echo.

echo --- Built APK exists? ----------------------------------------------
if exist "app\build\outputs\apk\debug\app-debug.apk" (
    echo OK  app-debug.apk exists
    dir /b "app\build\outputs\apk\debug\app-debug.apk"
) else (
    echo NO  No APK has been built yet. Re-run SHIP_IT.bat first.
)
echo.

echo --- App installed on phone? ----------------------------------------
adb shell pm list packages com.soundpad.sleep 2>&1
echo.
echo (If you see "package:com.soundpad.sleep.debug" the install worked.)
echo.

echo --- Try launching it -----------------------------------------------
adb shell monkey -p com.soundpad.sleep.debug -c android.intent.category.LAUNCHER 1 2>&1
echo.

echo  =================================================================
echo    Copy everything above and send to Claude.
echo  =================================================================
pause

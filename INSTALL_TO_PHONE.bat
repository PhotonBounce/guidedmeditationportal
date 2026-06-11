@echo off
REM =========================================================================
REM  Guided Meditation Portal - Robust phone installer
REM  (ASCII-only for cmd.exe compatibility)
REM =========================================================================
setlocal enableextensions enabledelayedexpansion
cd /d "%~dp0"

cls
echo.
echo  =================================================================
echo    Guided Meditation Portal - installing to your phone
echo  =================================================================
echo.

REM --- 0. Bootstrap environment if missing --------------------------------
if not exist ".tools\env.bat" (
    echo [step 0] No portable JDK/SDK found - running setup_environment.bat
    echo          First time: ~5 min download. Later runs skip this.
    echo.
    call setup_environment.bat
    if errorlevel 1 (
        echo.
        echo SETUP FAILED. Scroll up - paste me the actual error.
        pause & exit /b 1
    )
)

REM Load portable JDK / SDK paths
call ".tools\env.bat"

REM Defensive: re-write local.properties if it's gone
if not exist "local.properties" (
    set "LP_SDK=!ANDROID_HOME:\=\\!"
    > local.properties echo sdk.dir=!LP_SDK!
)

REM --- 1. Build the APK ---------------------------------------------------
set "APK=app\build\outputs\apk\debug\app-debug.apk"
set "APK_DIR=app\build\outputs\apk\debug"
if exist "%APK%" (
    echo [step 1] Cleaning older build to force a fresh recompilation...
    del /f /q "%APK%"
)

echo [step 1] Building fresh debug APK...
call gradlew.bat assembleDebug --no-daemon
if errorlevel 1 (
    echo.
    echo BUILD FAILED. Scroll up - paste me the last 30 lines.
    pause & exit /b 1
)

if not exist "%APK%" (
    echo Build said success but APK file missing. Run SHIP_IT.bat fresh.
    pause & exit /b 1
)

echo.
echo          APK PATH: %CD%\%APK%
for %%I in ("%APK%") do echo          SIZE:     %%~zI bytes
echo.

REM --- 2. Locate adb ------------------------------------------------------
set "ADB="
if exist "%ANDROID_HOME%\platform-tools\adb.exe" set "ADB=%ANDROID_HOME%\platform-tools\adb.exe"
if "!ADB!"=="" where adb >nul 2>nul && set "ADB=adb"
if "!ADB!"=="" if exist "%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe" set "ADB=%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe"

if "!ADB!"=="" (
    echo [step 2] no adb found - falling through to manual sideload...
    goto manual_sideload
)
echo [step 2] adb located: !ADB!

REM --- 3. Restart adb server ----------------------------------------------
echo          Restarting adb server...
"!ADB!" kill-server  >nul 2>&1
"!ADB!" start-server >nul 2>&1
ping -n 3 127.0.0.1 >nul
echo.

REM --- 4. Wait up to 30s for a device -------------------------------------
echo [step 3] Waiting up to 30 seconds for your phone...
echo          ^(If RSA dialog pops up on phone: tap "Always allow"^)
echo.

set "FOUND="
for /l %%i in (1,1,15) do (
    for /f "skip=1 tokens=1,2" %%a in ('""!ADB!" devices"') do (
        if not "%%a"=="" if "%%b"=="device" set "FOUND=%%a"
    )
    if defined FOUND goto device_ready
    ping -n 3 127.0.0.1 >nul
)

REM --- No device --> manual sideload -------------------------------------
echo.
echo  =================================================================
echo    adb cannot see your phone after 30 seconds.
echo  =================================================================
echo    Check ALL of these on the phone:
echo      * Settings -^> About phone -^> tap Build number 7 times
echo      * Settings -^> Developer options -^> turn USB debugging ON
echo      * Notification shade -^> tap the USB notification
echo        -^> change "Charging only" to "File transfer" or "MTP"
echo      * If RSA prompt appeared and you dismissed it,
echo        unplug, replug, and tap "Always allow"
echo      * Try a different USB cable - many cables are charge-only
echo  =================================================================
echo.
goto manual_sideload

:device_ready
echo          Found device: !FOUND!
"!ADB!" -s "!FOUND!" devices -l
echo.

REM --- Install ------------------------------------------------------------
echo [step 4] Installing Guided Meditation Portal...
"!ADB!" -s "!FOUND!" install -r -t "%APK%"
if errorlevel 1 (
    echo.
    echo Install failed - falling through to manual sideload...
    goto manual_sideload
)

echo.
echo          Launching Guided Meditation Portal on the phone...
"!ADB!" -s "!FOUND!" shell monkey -p com.auroramind.meditation.debug -c android.intent.category.LAUNCHER 1 >nul 2>&1

echo.
echo  =================================================================
echo    DONE - check your phone, Guided Meditation Portal should be open.
echo  =================================================================
echo.
pause
exit /b 0

REM =========================================================================
:manual_sideload
REM =========================================================================
echo.
echo  =================================================================
echo    MANUAL SIDELOAD - no adb needed
echo  =================================================================
echo.
echo    Your APK is here:
echo.
echo      %CD%\%APK%
echo.
echo    I'm opening that folder in File Explorer now.
echo.
echo    Three easy ways to get it on your phone:
echo      A) Email yourself the .apk file, open it on phone, tap install
echo      B) Upload to Google Drive, open Drive on phone, tap install
echo      C) WhatsApp/Telegram/Messages it to yourself, tap on phone
echo.
echo    When you tap the APK on the phone, Android asks
echo    "Allow install from this source?" - tap Allow, then Install.
echo    Guided Meditation Portal icon appears on your home screen.
echo  =================================================================
echo.

start "" explorer.exe "%CD%\%APK_DIR%"

echo Press any key when you have sent the APK to your phone.
pause >nul
endlocal

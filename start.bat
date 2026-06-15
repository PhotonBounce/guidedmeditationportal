@echo off
REM =========================================================================
REM  Power of Mind - Build and run on THIS PC (auto-creates emulator if needed)
REM =========================================================================
setlocal enableextensions enabledelayedexpansion
cd /d "%~dp0"

cls
echo.
echo  =================================================================
echo    Power of Mind - Build and run on this PC
echo  =================================================================
echo.

REM --- Java ----------------------------------------------------------------
set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot"
if not exist "%JAVA_HOME%\bin\java.exe" (
    if exist ".tools\jdk\bin\java.exe" (
        set "JAVA_HOME=%CD%\.tools\jdk"
    )
)
set "PATH=%JAVA_HOME%\bin;%PATH%"

java -version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Java not found at %JAVA_HOME%
    echo         Install Temurin 17 or update the JAVA_HOME path in this file.
    pause & exit /b 1
)

REM --- Android SDK ---------------------------------------------------------
REM  SDK_MAIN  = writable local SDK (platform-tools + emulator + system images go here)
REM  SDK_TOOLS = read-only Program Files SDK (only used to run sdkmanager/avdmanager)
set "SDK_MAIN=%LOCALAPPDATA%\Android\Sdk"
set "SDK_TOOLS=%ProgramFiles(x86)%\Android\android-sdk"
set "ADB=%SDK_MAIN%\platform-tools\adb.exe"
set "EMULATOR=%SDK_MAIN%\emulator\emulator.exe"
set "SDKMGR=%SDK_TOOLS%\cmdline-tools\12.0\bin\sdkmanager.bat"
set "AVDMGR=%SDK_TOOLS%\cmdline-tools\12.0\bin\avdmanager.bat"
REM Tell all tools to use the writable local SDK
set "ANDROID_SDK_ROOT=%SDK_MAIN%"
set "SYSIMG=system-images;android-34;google_apis;x86_64"
set "AVD_NAME=MeditationPortalPC"

if not exist "%ADB%" (
    echo [ERROR] adb.exe not found: %ADB%
    pause & exit /b 1
)
if not exist "%EMULATOR%" (
    echo [ERROR] emulator.exe not found: %EMULATOR%
    pause & exit /b 1
)

REM --- local.properties ----------------------------------------------------
if not exist "local.properties" (
    set "SDK_ESC=!ANDROID_HOME:\=\\!"
    > local.properties echo sdk.dir=!SDK_ESC!
    echo [info] Created local.properties
)

REM --- Build ---------------------------------------------------------------
set "APK=app\build\outputs\apk\debug\app-debug.apk"

echo [1/3] Building debug APK...
echo.
call gradlew.bat assembleDebug --no-daemon
if errorlevel 1 (
    echo.
    echo  BUILD FAILED - scroll up to see the error.
    pause & exit /b 1
)

if not exist "%APK%" (
    echo  Build reported success but APK is missing. Something went wrong.
    pause & exit /b 1
)

echo.
echo  APK ready: %CD%\%APK%
echo.

REM --- Auto-setup emulator (one-time) --------------------------------------
REM  Step A: pre-write SDK license files (avoids interactive prompt)
echo [2/5] Accepting SDK licenses...
if not exist "%SDK_MAIN%\licenses" mkdir "%SDK_MAIN%\licenses"
(echo 8933bad161af4178b1185d1a37fbf41ea5269c55
echo d56f5187479451eabf01fb78af6dfcb131a6481e
echo 24333f8a63b6825ea9c5514f83c2829b004d1fee)>"%SDK_MAIN%\licenses\android-sdk-license"
(echo 33b6a2b64607f11b759f320ef9dff4ae5c47d97a)>"%SDK_MAIN%\licenses\google-gdk-license"
(echo d975f751698a77b662f1254ddbeed3901e976f5a)>"%SDK_MAIN%\licenses\intel-android-extra-license"

REM  Step B: install system image to writable SDK_MAIN if missing
if not exist "%SDK_MAIN%\system-images\android-34" (
    echo [2/5] Downloading Android 34 system image ^(one-time, ~1.5 GB^)...
    echo       This only happens once. Please wait...
    echo.
    "%SDKMGR%" --sdk_root="%SDK_MAIN%" "%SYSIMG%"
    if errorlevel 1 (
        echo.
        echo  [ERROR] System image download failed. Check internet connection.
        pause & exit /b 1
    )
    echo  System image installed.
    echo.
) else (
    echo [2/5] System image OK.
)

REM  Step C: copy cmdline-tools into local SDK (so avdmanager uses local system images)
if not exist "%SDK_MAIN%\cmdline-tools\12.0\bin\avdmanager.bat" (
    echo [3/5] Copying SDK tools to local SDK...
    robocopy "%SDK_TOOLS%\cmdline-tools" "%SDK_MAIN%\cmdline-tools" /E /NFL /NDL /NJH /NJS /nc /ns /np >nul
)
set "AVDMGR=%SDK_MAIN%\cmdline-tools\12.0\bin\avdmanager.bat"

REM  Step D: create AVD if missing
"%AVDMGR%" list avd 2>nul | findstr /C:"%AVD_NAME%" >nul 2>&1
if errorlevel 1 (
    echo [3/5] Creating emulator "%AVD_NAME%"...
    echo no | "%AVDMGR%" create avd -n "%AVD_NAME%" -k "%SYSIMG%" --device "pixel_7" --force
    if errorlevel 1 (
        echo  [ERROR] Could not create AVD.
        pause & exit /b 1
    )
    echo  Emulator created.
) else (
    echo [3/5] Emulator "%AVD_NAME%" already exists.
)

REM --- Start emulator if no device is running ------------------------------
echo.
echo [4/5] Starting emulator on your PC screen...
call :COUNT_DEVICES DEVCOUNT
if !DEVCOUNT! EQU 0 (
    start "" "%EMULATOR%" -avd "%AVD_NAME%" -no-snapshot-load -no-boot-anim -gpu angle_indirect
    echo  Emulator launching, waiting for boot...
    set /a WAIT=0
    :wait_online
        ping -n 5 127.0.0.1 >nul
        set /a WAIT+=4
        call :COUNT_DEVICES DEVCOUNT
        if !DEVCOUNT! GTR 0 goto :wait_boot
        if !WAIT! GEQ 120 (
            echo.
            echo  Emulator is taking a long time. Leave the emulator window open
            echo  and run START.bat again once it finishes booting.
            pause & exit /b 1
        )
        set /a SECS_LEFT=120-!WAIT!
        echo  Waiting for emulator... ^(!SECS_LEFT!s^)
    goto :wait_online
)

:wait_boot
echo  Device online, waiting for full boot...
set /a WAIT=0
:boot_loop
    ping -n 4 127.0.0.1 >nul
    set /a WAIT+=3
    for /f "tokens=*" %%B in ('"%ADB%" shell getprop sys.boot_completed 2^>nul') do (
        if "%%B"=="1" goto :settle
    )
    if !WAIT! GEQ 90 goto :settle
    echo  Booting... ^(!WAIT!s^)
goto :boot_loop

:settle
echo  Boot complete. Waiting for system to settle...
ping -n 11 127.0.0.1 >nul
REM Restart ADB server so it re-discovers the emulator after the boot reconnect
"%ADB%" kill-server >nul 2>&1
"%ADB%" start-server >nul 2>&1
ping -n 4 127.0.0.1 >nul

:install
echo.
echo [5/5] Installing Power of Mind...
"%ADB%" install -r "%APK%"
if errorlevel 1 (
    echo.
    echo  INSTALL FAILED. Try: adb install -r "%APK%"
    pause & exit /b 1
)

REM --- Launch --------------------------------------------------------------
echo.
echo  Launching app...
"%ADB%" shell am start -n "com.powerofmind.app.debug/com.auroramind.meditation.SplashActivity"

echo.
echo  =================================================================
echo    Power of Mind is running on your PC!
echo  =================================================================
echo.
pause
goto :eof

REM -------------------------------------------------------------------------
:manual_install
echo.
echo  ---------------------------------------------------------------
echo  To test the app you need either:
echo    A) A real Android phone connected via USB with USB Debugging ON
echo       (Settings ^> Developer Options ^> USB Debugging)
echo    B) An Android emulator created in Android Studio
echo       (Device Manager ^> Add Device, then run START.bat again)
echo.
echo  APK is ready at: %CD%\%APK%
echo  ---------------------------------------------------------------
echo.
pause
exit /b 1

REM -------------------------------------------------------------------------
:COUNT_DEVICES
set "%1=0"
for /f "skip=1 tokens=2" %%D in ('"%ADB%" devices 2^>nul') do (
    if "%%D"=="device" set /a %1+=1
)
exit /b 0

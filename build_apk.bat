@echo off
REM =========================================================================
REM  Guided Meditation Portal - Build Fresh Debug APK
REM =========================================================================
setlocal enableextensions enabledelayedexpansion
cd /d "%~dp0"

cls
echo.
echo  =================================================================
echo    Guided Meditation Portal - Rebuilding Fresh Debug APK
echo  =================================================================
echo.

REM --- 1. Detect Java JDK 17 ----------------------------------------------
set "JAVA_HOME="

if exist "D:\jdk17\bin\java.exe" (
    set "JAVA_HOME=D:\jdk17"
) else if exist "C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot\bin\java.exe" (
    set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot"
) else if exist "C:\Program Files\Java\jdk-17\bin\java.exe" (
    set "JAVA_HOME=C:\Program Files\Java\jdk-17"
) else if exist ".tools\jdk\bin\java.exe" (
    set "JAVA_HOME=%CD%\.tools\jdk"
)

if "%JAVA_HOME%"=="" (
    echo [ERROR] JDK 17 not found. 
    echo Please ensure Java 17 is installed or run setup_environment.bat
    pause & exit /b 1
)

set "PATH=%JAVA_HOME%\bin;%PATH%"
echo [Java] Found JDK at: %JAVA_HOME%

REM --- 2. Detect Android SDK ----------------------------------------------
set "ANDROID_HOME="

if exist "C:\Users\fucktrumpandrednecks\AppData\Local\Android\Sdk" (
    set "ANDROID_HOME=C:\Users\fucktrumpandrednecks\AppData\Local\Android\Sdk"
) else if exist ".tools\android-sdk" (
    set "ANDROID_HOME=%CD%\.tools\android-sdk"
)

if "%ANDROID_HOME%"=="" (
    echo [ERROR] Android SDK not found.
    echo Please install Android Studio or run setup_environment.bat
    pause & exit /b 1
)

set "ANDROID_SDK_ROOT=%ANDROID_HOME%"
echo [SDK]  Found SDK at: %ANDROID_HOME%
echo.

REM --- 3. Clean old APK to guarantee fresh rebuild ------------------------
set "APK=app\build\outputs\apk\debug\app-debug.apk"
set "APK_DIR=app\build\outputs\apk\debug"

if exist "%APK%" (
    echo [Clean] Deleting old APK to force a fresh rebuild...
    del /f /q "%APK%"
)

REM --- 4. Compile APK -----------------------------------------------------
echo [Build] Compiling fresh debug APK...
echo.
call gradlew.bat assembleDebug --no-daemon
if errorlevel 1 (
    echo.
    echo  [ERROR] Build failed. Scroll up to see the compiler output.
    pause & exit /b 1
)

if not exist "%APK%" (
    echo.
    echo  [ERROR] Build completed but APK was not found.
    pause & exit /b 1
)

echo.
echo  =================================================================
echo    BUILD SUCCESSFUL - FRESH APK READY!
echo  =================================================================
echo.
echo    Your new APK is saved at:
echo    %CD%\%APK%
echo.
for %%I in ("%APK%") do echo    Size: %%~zI bytes
echo.
echo  =================================================================
echo    HOW TO TEST ON YOUR PHONE:
echo  =================================================================
echo    1. Connect your phone via USB with USB Debugging enabled, 
echo       then run: build_and_install.bat
echo.
echo    2. OR, to install manually (no cables/setup required):
echo       We've opened the folder in File Explorer for you. Simply
echo       share the "app-debug.apk" file with your phone via:
echo         * Google Drive upload
echo         * Email attachment to yourself
echo         * WhatsApp / Telegram message to yourself
echo.
echo       Then, open it on your phone and tap Install!
echo  =================================================================
echo.

start "" explorer.exe "%CD%\%APK_DIR%"
pause
endlocal

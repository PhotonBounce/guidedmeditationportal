@echo off
REM =========================================================================
REM  Guided Meditation Portal - Build Fresh Release AAB (Android App Bundle)
REM =========================================================================
setlocal enableextensions enabledelayedexpansion
cd /d "%~dp0"

cls
echo.
echo  =================================================================
echo    Guided Meditation Portal - Rebuilding Fresh Release AAB
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

REM --- 3. Clean old AAB to guarantee fresh rebuild ------------------------
set "AAB=app\build\outputs\bundle\release\app-release.aab"
set "AAB_DIR=app\build\outputs\bundle\release"

if exist "%AAB%" (
    echo [Clean] Deleting old AAB to force a fresh rebuild...
    del /f /q "%AAB%"
)

REM --- 4. Compile AAB -----------------------------------------------------
echo [Build] Compiling fresh release AAB...
echo.
call gradlew.bat bundleRelease --no-daemon
if errorlevel 1 (
    echo.
    echo  [ERROR] Build failed. Scroll up to see the compiler output.
    pause & exit /b 1
)

if not exist "%AAB%" (
    echo.
    echo  [ERROR] Build completed but AAB was not found.
    pause & exit /b 1
)

echo.
echo  =================================================================
echo    BUILD SUCCESSFUL - FRESH RELEASE AAB READY!
echo  =================================================================
echo.
echo    Your new App Bundle (.aab) is saved at:
echo    %CD%\%AAB%
echo.
for %%I in ("%AAB%") do echo    Size: %%~zI bytes
echo.
echo  =================================================================
echo    HOW TO UPLOAD TO GOOGLE PLAY STORE:
echo  =================================================================
echo    We've opened the folder in File Explorer for you. Simply:
echo    1. Log in to your Google Play Developer Console.
echo    2. Select Guided Meditation Portal (com.auroramind.meditation).
echo    3. Go to "Production" or "Testing" on the left menu.
echo    4. Click "Create new release".
echo    5. Drag and drop the "app-release.aab" file into the App Bundles box.
echo    6. Save and publish!
echo  =================================================================
echo.

start "" explorer.exe "%CD%\%AAB_DIR%"
pause
endlocal

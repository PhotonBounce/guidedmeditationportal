@echo off
REM =========================================================================
REM  SoundPad - Zero-install environment bootstrapper
REM
REM  What this does (no admin required, ~5 minutes first run):
REM    1. Downloads JDK 17 (Eclipse Temurin) into .tools\jdk
REM    2. Downloads Android command-line tools into .tools\android-sdk
REM    3. Accepts SDK licenses
REM    4. Installs platform-tools (adb), platforms;android-34, build-tools 34.0.0
REM
REM  All downloaded into .tools\ in this repo - nothing touches your system.
REM  Re-running this is safe: it skips anything already installed.
REM =========================================================================
setlocal enableextensions enabledelayedexpansion
cd /d "%~dp0"

set "TOOLS=%CD%\.tools"
set "JDK_HOME=%TOOLS%\jdk"
set "SDK_HOME=%TOOLS%\android-sdk"
set "CMDLINE=%SDK_HOME%\cmdline-tools\latest"

if not exist "%TOOLS%" mkdir "%TOOLS%"

echo.
echo === SoundPad environment bootstrap ===
echo Installing into: %TOOLS%
echo.

REM --- 1. JDK 17 (Eclipse Temurin) ----------------------------------------
if exist "%JDK_HOME%\bin\java.exe" (
    echo [1/4] JDK 17 already present, skipping
) else (
    echo [1/4] Downloading JDK 17 ^(~190 MB, this is the slow one^)...
    set "JDK_URL=https://api.adoptium.net/v3/binary/latest/17/ga/windows/x64/jdk/hotspot/normal/eclipse?project=jdk"
    set "JDK_ZIP=%TOOLS%\jdk.zip"
    powershell -NoProfile -ExecutionPolicy Bypass -Command "$ProgressPreference='SilentlyContinue'; Invoke-WebRequest -UseBasicParsing -Uri '!JDK_URL!' -OutFile '!JDK_ZIP!'"
    if not exist "!JDK_ZIP!" (
        echo [ERROR] JDK download failed.
        pause & exit /b 1
    )
    echo       Extracting...
    powershell -NoProfile -ExecutionPolicy Bypass -Command "Expand-Archive -Force -Path '!JDK_ZIP!' -DestinationPath '%TOOLS%\jdk-tmp'"
    for /d %%D in ("%TOOLS%\jdk-tmp\jdk-*") do (
        if exist "%JDK_HOME%" rmdir /s /q "%JDK_HOME%"
        move "%%D" "%JDK_HOME%" >nul
    )
    rmdir /s /q "%TOOLS%\jdk-tmp" 2>nul
    del "!JDK_ZIP!" 2>nul
    if not exist "%JDK_HOME%\bin\java.exe" (
        echo [ERROR] JDK extraction failed.
        pause & exit /b 1
    )
)
set "JAVA_HOME=%JDK_HOME%"
set "PATH=%JAVA_HOME%\bin;%PATH%"
"%JAVA_HOME%\bin\java" -version 2>&1 | findstr /i "version"

REM --- 2. Android command-line tools --------------------------------------
if exist "%CMDLINE%\bin\sdkmanager.bat" (
    echo [2/4] Android command-line tools already present, skipping
) else (
    echo [2/4] Downloading Android command-line tools ^(~150 MB^)...
    set "CLT_URL=https://dl.google.com/android/repository/commandlinetools-win-11076708_latest.zip"
    set "CLT_ZIP=%TOOLS%\clt.zip"
    powershell -NoProfile -ExecutionPolicy Bypass -Command "$ProgressPreference='SilentlyContinue'; Invoke-WebRequest -UseBasicParsing -Uri '!CLT_URL!' -OutFile '!CLT_ZIP!'"
    if not exist "!CLT_ZIP!" (
        echo [ERROR] Android tools download failed.
        pause & exit /b 1
    )
    echo       Extracting...
    if not exist "%SDK_HOME%\cmdline-tools" mkdir "%SDK_HOME%\cmdline-tools"
    powershell -NoProfile -ExecutionPolicy Bypass -Command "Expand-Archive -Force -Path '!CLT_ZIP!' -DestinationPath '%SDK_HOME%\cmdline-tools\_tmp'"
    if exist "%CMDLINE%" rmdir /s /q "%CMDLINE%"
    move "%SDK_HOME%\cmdline-tools\_tmp\cmdline-tools" "%CMDLINE%" >nul
    rmdir /s /q "%SDK_HOME%\cmdline-tools\_tmp" 2>nul
    del "!CLT_ZIP!" 2>nul
    if not exist "%CMDLINE%\bin\sdkmanager.bat" (
        echo [ERROR] cmdline-tools extraction failed.
        pause & exit /b 1
    )
)
set "ANDROID_HOME=%SDK_HOME%"
set "ANDROID_SDK_ROOT=%SDK_HOME%"
set "PATH=%CMDLINE%\bin;%SDK_HOME%\platform-tools;%PATH%"

REM --- 3. Accept SDK licenses ---------------------------------------------
echo [3/4] Accepting SDK licenses...
(for /l %%i in (1,1,20) do @echo y) | "%CMDLINE%\bin\sdkmanager.bat" --licenses >nul 2>&1

REM --- 4. Install required SDK packages -----------------------------------
echo [4/4] Installing SDK packages ^(platform-tools, android-34, build-tools 34.0.0^)...
call "%CMDLINE%\bin\sdkmanager.bat" "platform-tools" "platforms;android-34" "build-tools;34.0.0" 1>nul

if not exist "%SDK_HOME%\platform-tools\adb.exe" (
    echo [ERROR] platform-tools installation failed.
    pause & exit /b 1
)
if not exist "%SDK_HOME%\platforms\android-34" (
    echo [ERROR] android-34 platform installation failed.
    pause & exit /b 1
)

REM --- 5. Write local.properties so Gradle finds the SDK -----------------
echo Writing local.properties...
set "LP_SDK=%SDK_HOME:\=\\%"
> local.properties echo sdk.dir=%LP_SDK%

REM --- 6. Persist environment to a sourceable file -----------------------
> .tools\env.bat echo @echo off
>> .tools\env.bat echo set "JAVA_HOME=%JDK_HOME%"
>> .tools\env.bat echo set "ANDROID_HOME=%SDK_HOME%"
>> .tools\env.bat echo set "ANDROID_SDK_ROOT=%SDK_HOME%"
>> .tools\env.bat echo set "PATH=%%JAVA_HOME%%\bin;%%ANDROID_HOME%%\platform-tools;%%ANDROID_HOME%%\cmdline-tools\latest\bin;%%PATH%%"

echo.
echo === DONE === Environment ready.
echo.
echo   JDK:      %JDK_HOME%
echo   SDK:      %SDK_HOME%
echo   adb:      %SDK_HOME%\platform-tools\adb.exe
echo.
echo Now run: build_and_install.bat  ^(or SHIP_IT.bat to do everything^)
echo.
endlocal

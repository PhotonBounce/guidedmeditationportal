@echo off
REM =========================================================================
REM  SoundPad - build signed RELEASE AAB for Play Store upload
REM
REM  Prereqs:
REM    1. Run create_keystore.bat ONCE (or Android Studio's wizard) to create
REM       soundpad-release.jks
REM    2. Set these env vars OR put them in %USERPROFILE%\.gradle\gradle.properties:
REM         SOUNDPAD_KEYSTORE=D:\soundpad-release.jks
REM         SOUNDPAD_KEY_ALIAS=soundpad-key
REM         SOUNDPAD_STORE_PASSWORD=your-password
REM         SOUNDPAD_KEY_PASSWORD=your-password
REM
REM  Output: app\build\outputs\bundle\release\app-release.aab
REM          (upload this to Play Console)
REM =========================================================================
setlocal enableextensions
cd /d "%~dp0"

echo.
echo === SoundPad RELEASE AAB build ===
echo.

if exist ".tools\env.bat" call ".tools\env.bat"

if "%SOUNDPAD_KEYSTORE%"=="" (
    if not exist "%USERPROFILE%\.gradle\gradle.properties" (
        echo [WARN] No signing config detected. AAB will be UNSIGNED and
        echo        cannot be uploaded to Play Console. Run create_keystore.bat
        echo        first, then set the SOUNDPAD_* env vars.
        echo.
    )
)

call gradlew.bat bundleRelease --no-daemon
if errorlevel 1 (
    echo.
    echo [ERROR] Build failed. See output above.
    pause & exit /b 1
)

echo.
echo === DONE ===
echo Your signed AAB is at:
echo    %CD%\app\build\outputs\bundle\release\app-release.aab
echo Upload it to Play Console -^> Production -^> Create new release.
echo.
pause

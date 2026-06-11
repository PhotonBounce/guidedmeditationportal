@echo off
REM =========================================================================
REM  Guided Meditation Portal - build signed RELEASE AAB for Play Store upload
REM
REM  Prereqs:
REM    1. A release keystore must exist (see create_keystore.bat).
REM    2. Set these env vars OR put them in %USERPROFILE%\.gradle\gradle.properties:
REM         MEDITATION_KEYSTORE=C:\path\to\release.jks
REM         MEDITATION_KEY_ALIAS=your-key-alias
REM         MEDITATION_STORE_PASSWORD=your-password
REM         MEDITATION_KEY_PASSWORD=your-password
REM
REM  Output: app\build\outputs\bundle\release\app-release.aab
REM          (upload this to Play Console)
REM =========================================================================
setlocal enableextensions
cd /d "%~dp0"

echo.
echo === Guided Meditation Portal RELEASE AAB build ===
echo.

if exist ".tools\env.bat" call ".tools\env.bat"

if "%MEDITATION_KEYSTORE%"=="" (
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

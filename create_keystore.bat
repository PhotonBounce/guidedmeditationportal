@echo off
REM =========================================================================
REM  Create the SoundPad release-signing keystore (do this ONCE, ever)
REM
REM  *** BACK UP THE .jks FILE TO GOOGLE DRIVE + A USB STICK ***
REM  *** WRITE DOWN THE PASSWORD IN A PASSWORD MANAGER       ***
REM  Lose the keystore and Google will NOT let you update the app again.
REM =========================================================================

setlocal
cd /d "%~dp0"
if exist ".tools\env.bat" call ".tools\env.bat"

where keytool >nul 2>nul
if errorlevel 1 (
    echo [ERROR] keytool not found. Run SHIP_IT.bat once first
    echo         (it installs JDK + keytool portably into .tools\).
    pause & exit /b 1
)

set "KEYSTORE_PATH=%USERPROFILE%\soundpad-release.jks"
if exist "%KEYSTORE_PATH%" (
    echo [WARN] %KEYSTORE_PATH% already exists. Not overwriting.
    echo         Move or rename it first if you need to recreate.
    pause & exit /b 0
)

echo.
echo === Creating SoundPad release keystore ===
echo Path: %KEYSTORE_PATH%
echo Alias: soundpad-key
echo Validity: 9125 days (25 years)
echo.
echo You'll be prompted for:
echo   - A keystore password (use a strong one - SAVE IT)
echo   - Your name / org / city info (any values are fine - not public)
echo.

keytool -genkey -v -keystore "%KEYSTORE_PATH%" -alias soundpad-key -keyalg RSA -keysize 2048 -validity 9125

if errorlevel 1 (
    echo [ERROR] Keystore creation failed.
    pause & exit /b 1
)

echo.
echo === DONE ===
echo.
echo NEXT STEPS:
echo   1. Back up %KEYSTORE_PATH% to Google Drive + USB stick
echo   2. Save the password in a password manager
echo   3. Add these to %USERPROFILE%\.gradle\gradle.properties:
echo.
echo        SOUNDPAD_KEYSTORE=%KEYSTORE_PATH%
echo        SOUNDPAD_KEY_ALIAS=soundpad-key
echo        SOUNDPAD_STORE_PASSWORD=^<the password you chose^>
echo        SOUNDPAD_KEY_PASSWORD=^<the password you chose^>
echo.
echo   4. Run build_release_aab.bat to produce your upload artifact.
echo.
pause

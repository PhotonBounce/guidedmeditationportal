@echo off
REM =========================================================================
REM  Guided Meditation Portal - SHIP IT  (one-button installer, ASCII-only)
REM =========================================================================
setlocal enableextensions
cd /d "%~dp0"

cls
echo.
echo  =================================================================
echo    Guided Meditation Portal - Ship It
echo  =================================================================
echo    Plug your phone in with USB Debugging enabled.
echo    Accept the RSA prompt on the phone when it appears.
echo.
echo    Press any key to start...
echo  =================================================================
pause >nul

echo.
echo  --- STEP 1 of 2: Bootstrap environment ---
echo.
if exist ".tools\env.bat" (
    echo Portable JDK + Android SDK already installed. Skipping setup.
) else (
    call setup_environment.bat
    if errorlevel 1 (
        echo.
        echo Environment setup FAILED. Scroll up for the actual error.
        pause & exit /b 1
    )
)

echo.
echo  --- STEP 2 of 2: Build APK and push to phone ---
echo.
call INSTALL_TO_PHONE.bat
exit /b %ERRORLEVEL%

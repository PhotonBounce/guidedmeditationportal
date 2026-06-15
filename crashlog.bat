@echo off
REM ==========================================================================
REM  Power of Mind - capture the latest crash from the connected device/emulator
REM  Run this right after the app crashes, then paste the output to Claude.
REM ==========================================================================
setlocal
set "ADB=%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe"
if not exist "%ADB%" set "ADB=adb"

echo Capturing the latest crash (AndroidRuntime FATAL) ...
echo --------------------------------------------------------------------------
"%ADB%" logcat -d AndroidRuntime:E *:S
"%ADB%" logcat -d AndroidRuntime:E *:S > crashlog.txt 2>&1
echo --------------------------------------------------------------------------
echo.
echo Copy the "FATAL EXCEPTION" block above (also saved to crashlog.txt) and
echo paste it to Claude so the exact line can be fixed.
echo.
pause

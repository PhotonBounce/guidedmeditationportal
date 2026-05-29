@echo off
REM =========================================================================
REM  SoundPad - build + install (legacy alias for INSTALL_TO_PHONE.bat)
REM =========================================================================
setlocal
cd /d "%~dp0"
call INSTALL_TO_PHONE.bat
exit /b %ERRORLEVEL%

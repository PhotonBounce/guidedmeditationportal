@echo off
REM =========================================================================
REM  Guided Meditation Portal - build + install (alias for INSTALL_TO_PHONE.bat)
REM =========================================================================
setlocal
cd /d "%~dp0"
call INSTALL_TO_PHONE.bat
exit /b %ERRORLEVEL%

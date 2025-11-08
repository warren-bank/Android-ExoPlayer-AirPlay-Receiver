@echo off

call "%~dp0.\env.bat"

node "%~dpn0.js" %json_file% %bash_file% %cmd_file%

echo.
pause

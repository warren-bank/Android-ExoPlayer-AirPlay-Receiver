@echo off

call "%~dp0.\env.bat"

node "%~dpn0.js" %json_file% %m3u_file%

echo.
pause

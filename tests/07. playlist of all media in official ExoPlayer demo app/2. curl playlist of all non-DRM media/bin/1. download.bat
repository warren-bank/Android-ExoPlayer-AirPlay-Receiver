@echo off

call "%~dp0.\env.bat"

set androidx_media3_tag=1.9.0-alpha01
set URL="https://github.com/androidx/media/raw/%androidx_media3_tag%/demos/main/src/main/assets/media.exolist.json"

wget -nv --no-check-certificate -O %json_file% %URL%

echo.
pause

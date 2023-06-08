@echo off

call "%~dp0..\..\..\0-tools\tools.bat"

serve --cors --listen 80 --config "%~dpn0.json" "%~dp0..\.." >"%~dpn0.log" 2>&1

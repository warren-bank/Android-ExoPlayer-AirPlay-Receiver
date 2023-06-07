@echo off

cd /D "%~dp0."

if not exist "node_modules" call npm install

set PATH=%~dp0.\node_modules\.bin;%PATH%

serve --cors --listen 80 --config "%~dpn0.json" "%~dp0..\.." >"%~dpn0.log" 2>&1

@echo off

cd /D "%~dp0."

if not exist "node_modules" call npm install

set PATH=%~dp0.\node_modules\.bin;%PATH%

nget --insecure -P "%~dp0..\.." -i "%~dpn0.txt"

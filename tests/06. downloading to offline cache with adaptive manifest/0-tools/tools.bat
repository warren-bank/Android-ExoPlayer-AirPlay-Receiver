@echo off

set USE_GLOBAL_TOOLS=0

if defined USE_GLOBAL_TOOLS if "%USE_GLOBAL_TOOLS%"=="1" goto :done

if exist "%~dp0.\node_modules" goto :set_path

:npm_install
pushd "%~dp0."
call npm install
popd

:set_path
set PATH=%~dp0.\node_modules\.bin;%PATH%

:done

@echo off

call "%~dp0.\0-env.bat"

set output_dir=%~dp0..\out\%~n0

set options=
set options=%options% --source-path "%~dp0..\lib;%~dp0..\..\..\..\android-studio-project\ExoPlayer-AirPlay-Receiver\src\main\java"
set options=%options% -d "%output_dir%"
set options=%options% -encoding "UTF-8"
set options=%options% -g:none

set sourcefile="%~dp0..\src\Main.java"

if exist "%output_dir%" rmdir /Q /S "%output_dir%"
mkdir "%output_dir%"

javac %options% %sourcefile%

echo.
pause

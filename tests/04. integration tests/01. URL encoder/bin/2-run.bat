@echo off

call "%~dp0.\0-env.bat"

set output_dir=%~dp0..\out\%~n0
set stdout_file="%output_dir%\stdout.txt"
set stderr_file="%output_dir%\stderr.txt"

set options=
set options=%options% --class-path "%output_dir%\..\1-compile"

set mainclass="Main"

if exist "%output_dir%" rmdir /Q /S "%output_dir%"
mkdir "%output_dir%"

java %options% %mainclass% 1>%stdout_file% 2>%stderr_file%

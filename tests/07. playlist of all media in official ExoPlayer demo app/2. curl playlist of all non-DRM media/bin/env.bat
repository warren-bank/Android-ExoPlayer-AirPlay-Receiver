@echo off

set temp_dir=%~dp0.\temp
set json_file="%temp_dir%\media.json"
set bash_file="%~dp0..\media.sh"
set cmd_file="%~dp0..\media.bat"

if not exist "%temp_dir%" mkdir "%temp_dir%"

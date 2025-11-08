@echo off

set temp_dir=%~dp0.\temp
set json_file="%temp_dir%\media.json"
set m3u_file="%~dp0..\media.m3u"

if not exist "%temp_dir%" mkdir "%temp_dir%"

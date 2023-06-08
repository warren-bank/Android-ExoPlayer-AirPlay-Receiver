@echo off

call "%~dp0..\..\..\0-tools\tools.bat"

nget --insecure -P "%~dp0..\.." -i "%~dpn0.txt"

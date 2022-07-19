@echo off

set default_airplay_ip=192.168.1.100:8192

cls
echo Network Address of AirPlay Receiver:
echo [default]   = %default_airplay_ip%
set /P airplay_ip=[host:port] = 

if "%airplay_ip%"=="" set airplay_ip=%default_airplay_ip%

java -jar "%~dp0..\airplay.jar" -h "%airplay_ip%" -d

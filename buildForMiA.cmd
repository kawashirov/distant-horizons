@echo off & setlocal enabledelayedexpansion

set version="1.20.2"
call .\gradlew.bat clean -PmcVer="!version!" --no-daemon
call .\gradlew.bat fabric:build -PmcVer="!version!" --no-daemon
call .\gradlew.bat mergeJars -PmcVer="!version!" --no-daemon

endlocal

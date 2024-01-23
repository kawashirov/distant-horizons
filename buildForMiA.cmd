@echo off & setlocal enabledelayedexpansion

mkdir buildForMiA

set version="1.20.4"
call .\gradlew.bat clean --no-daemon
call .\gradlew.bat build -PmcVer="!version!" --no-daemon
call .\gradlew.bat mergeJars -PmcVer="!version!" --no-daemon
move Merged\*.jar buildForMiA\

endlocal

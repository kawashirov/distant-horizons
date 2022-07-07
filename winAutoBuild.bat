@echo off

SETLOCAL
CALL :buildVersion "1.19"
CALL :buildVersion "1.18.2"
CALL :buildVersion "1.18.1"
CALL :buildVersion "1.17.1"
CALL :buildVersion "1.16.5"
EXIT /B %ERRORLEVEL%

:buildVersion
@echo on
call ./gradlew.bat clean -PmcVer="%~1" --no-daemon
call ./gradlew.bat build -PmcVer="%~1" --no-daemon
call ./gradlew.bat mergeJars -PmcVer="%~1" --no-daemon
@echo off
EXIT /B 0

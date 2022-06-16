@echo off

:buildVersion
call gradlew.bat -PmcVer=%~1 build
call gradlew.bat mergeJars -PmcVer=%~1
rm -R build/libs common/build/libs fabric/build/libs forge/build/libs
EXIT /B 0

SETLOCAL
CALL :buildVersion 1.19
CALL :buildVersion 1.18.2
CALL :buildVersion 1.18.1
CALL :buildVersion 1.17.1
CALL :buildVersion 1.16.5
EXIT /B %ERRORLEVEL%


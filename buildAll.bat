@echo off

echo Windows build all script needs to be rewritten
echo I dont use Windows so I cant really make this
echo So if someone does use Windows and knows how to script stuff then can you please port the "buildall" script I made for Unix




@REM Old BAT script if you need some help with this task

@REM SETLOCAL
@REM CALL :buildVersion "1.18.2"
@REM CALL :buildVersion "1.19.4"
@REM CALL :buildVersion "1.20.1"
@REM EXIT /B %ERRORLEVEL%
@REM
@REM :buildVersion
@REM @echo on
@REM call ./gradlew.bat clean -PmcVer="%~1" --no-daemon
@REM call ./gradlew.bat build -PmcVer="%~1" --no-daemon
@REM @echo off
@REM EXIT /B 0

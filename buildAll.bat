@echo off & setlocal enabledelayedexpansion

@rem Loop trough everything in the version properties folder
for %%f in (versionProperties\*) do (
    @rem Get the name of the version that is going to be compiled
    set version=%%~nf

    @rem Clean out the folders and build it
    echo Cleaning workspace to build !version!
    call .\gradlew.bat clean -PmcVer="!version!" --no-daemon
    echo Building !version!
    call .\gradlew.bat build -PmcVer="!version!" --no-daemon
)

endlocal

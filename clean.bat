@echo off
setlocal EnableExtensions EnableDelayedExpansion

cd /d "%~dp0"

echo [INFO] Working directory: %CD%
echo [INFO] Stopping Gradle daemons (best effort)...
if exist "gradlew.bat" (
    call gradlew.bat --stop >nul 2>&1
)

set /a REMOVED_DIRS=0
set /a REMOVED_FILES=0

call :removeDir ".gradle"
call :removeDir ".kotlin"

for /f "delims=" %%D in ('dir /s /b /ad build 2^>nul') do (
    call :removeDir "%%D"
)

for %%N in (.cxx .externalNativeBuild) do (
    for /f "delims=" %%D in ('dir /s /b /ad %%N 2^>nul') do (
        call :removeDir "%%D"
    )
)

call :removeFile "build_exit.txt"
call :removeFile "build_output.log"

echo [OK] Cleanup finished. Removed !REMOVED_DIRS! directories and !REMOVED_FILES! files.
echo [HINT] Run build.bat to rebuild from a clean state.
exit /b 0

:removeDir
set "TARGET=%~1"
if exist "%TARGET%\" (
    attrib -r -s -h "%TARGET%\*" /s /d >nul 2>&1
    rd /s /q "%TARGET%" >nul 2>&1
    if exist "%TARGET%\" (
        echo [WARN] Could not remove directory: %TARGET%
    ) else (
        echo [OK] Removed directory: %TARGET%
        set /a REMOVED_DIRS+=1
    )
)
exit /b 0

:removeFile
set "TARGET=%~1"
if exist "%TARGET%" (
    attrib -r -s -h "%TARGET%" >nul 2>&1
    del /f /q "%TARGET%" >nul 2>&1
    if exist "%TARGET%" (
        echo [WARN] Could not remove file: %TARGET%
    ) else (
        echo [OK] Removed file: %TARGET%
        set /a REMOVED_FILES+=1
    )
)
exit /b 0

@echo off
setlocal EnableExtensions

cd /d "%~dp0"

set "AS_JBR=C:\Program Files\Android\Android Studio\jbr"
if exist "%AS_JBR%\bin\java.exe" (
    set "JAVA_HOME=%AS_JBR%"
)

if not defined JAVA_HOME (
    echo [ERROR] JAVA_HOME is not set and Android Studio JBR was not found.
    echo [ERROR] Expected path: "%AS_JBR%"
    echo [HINT] Install Android Studio or set JAVA_HOME to a JDK 17+ path.
    exit /b 1
)

if not exist "%JAVA_HOME%\bin\java.exe" (
    echo [ERROR] JAVA_HOME points to an invalid path: "%JAVA_HOME%"
    exit /b 1
)

if not exist "gradlew.bat" (
    echo [ERROR] gradlew.bat was not found in "%CD%".
    exit /b 1
)

set "SDK_CANDIDATE="
if defined ANDROID_SDK_ROOT (
    if exist "%ANDROID_SDK_ROOT%\platforms" (
        set "SDK_CANDIDATE=%ANDROID_SDK_ROOT%"
    )
)

if not defined SDK_CANDIDATE (
    if defined ANDROID_HOME (
        if exist "%ANDROID_HOME%\platforms" (
            set "SDK_CANDIDATE=%ANDROID_HOME%"
        )
    )
)

if not defined SDK_CANDIDATE (
    if exist "%LOCALAPPDATA%\Android\Sdk\platforms" (
        set "SDK_CANDIDATE=%LOCALAPPDATA%\Android\Sdk"
    )
)

if not defined SDK_CANDIDATE (
    echo [ERROR] Android SDK was not found.
    echo [HINT] Install Android SDK in Android Studio or set ANDROID_SDK_ROOT.
    exit /b 1
)

set "ANDROID_SDK_ROOT=%SDK_CANDIDATE%"
set "ANDROID_HOME=%SDK_CANDIDATE%"

if not exist "local.properties" (
    > local.properties echo sdk.dir=%SDK_CANDIDATE:\=/%
    echo [INFO] Created local.properties with detected SDK path.
)

set "PATH=%JAVA_HOME%\bin;%PATH%"

if "%~1"=="" (
    set "GRADLE_TASKS=clean assembleRelease"
) else (
    set "GRADLE_TASKS=%*"
)

echo [INFO] JAVA_HOME=%JAVA_HOME%
echo [INFO] ANDROID_SDK_ROOT=%ANDROID_SDK_ROOT%
echo [INFO] Running: gradlew.bat --no-daemon %GRADLE_TASKS%
call gradlew.bat --no-daemon %GRADLE_TASKS%
set "EXIT_CODE=%ERRORLEVEL%"

if not "%EXIT_CODE%"=="0" (
    echo [ERROR] Build failed with exit code %EXIT_CODE%.
    exit /b %EXIT_CODE%
)

echo [OK] Build finished successfully.
if exist "app\build\outputs\apk\release\app-release.apk" (
    echo [OK] Release APK: app\build\outputs\apk\release\app-release.apk
) else if exist "app\build\outputs\apk\release\app-release-unsigned.apk" (
    echo [OK] Release APK: app\build\outputs\apk\release\app-release-unsigned.apk
) else (
    echo [INFO] APK path may vary; check app\build\outputs\apk\release\
)
exit /b 0

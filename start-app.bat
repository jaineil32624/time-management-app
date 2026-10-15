@echo off
setlocal

set "SDK=%LOCALAPPDATA%\Android\Sdk"
if defined ANDROID_HOME set "SDK=%ANDROID_HOME%"
if not exist "%SDK%\platform-tools\adb.exe" goto sdk_not_found

set "ADB=%SDK%\platform-tools\adb.exe"
set "EMULATOR=%SDK%\emulator\emulator.exe"
set "APK=%~dp0app\build\outputs\apk\debug\app-debug.apk"
set "AVD=Medium_Phone_API_37.0"

if not exist "%APK%" goto build_apk
goto apk_ready

:build_apk
if not exist "%~dp0gradlew.bat" goto gradle_not_found
if not exist "%JAVA_HOME%\bin\java.exe" set "JAVA_HOME=C:\Program Files\Android\Android Studio\jbr"
if not exist "%JAVA_HOME%\bin\java.exe" goto java_not_found
set "ANDROID_HOME=%SDK%"
set "ANDROID_SDK_ROOT=%SDK%"
echo Building the debug APK...
call "%~dp0gradlew.bat" assembleDebug
if errorlevel 1 goto build_failed
if not exist "%APK%" goto apk_not_found

:apk_ready
if not exist "%EMULATOR%" goto emulator_not_found
"%ADB%" start-server
if errorlevel 1 goto adb_failed

set "DEVICE="
for /f "skip=1 tokens=1,2" %%D in ('"%ADB%" devices') do if "%%E"=="device" if not defined DEVICE set "DEVICE=%%D"
if defined DEVICE goto wait_for_boot

"%EMULATOR%" -list-avds | findstr /X /C:"%AVD%" >nul
if errorlevel 1 goto avd_not_found
echo Starting Android emulator %AVD%...
start "Android Emulator" "%EMULATOR%" -avd "%AVD%" -no-boot-anim
"%ADB%" wait-for-device
if errorlevel 1 goto adb_failed
for /f "skip=1 tokens=1,2" %%D in ('"%ADB%" devices') do if "%%E"=="device" if not defined DEVICE set "DEVICE=%%D"
if not defined DEVICE goto device_not_found

:wait_for_boot
echo Waiting for Android to finish starting...
for /L %%I in (1,1,180) do (
    "%ADB%" -s "%DEVICE%" shell getprop sys.boot_completed | findstr /X "1" >nul
    if not errorlevel 1 goto launch_app
    ping -n 3 127.0.0.1 >nul
)
goto boot_timeout

:launch_app
echo Installing the app...
"%ADB%" -s "%DEVICE%" install -r "%APK%"
if errorlevel 1 goto install_failed
echo Opening Time Management App...
"%ADB%" -s "%DEVICE%" shell monkey -p com.timemanagementapp -c android.intent.category.LAUNCHER 1
if errorlevel 1 goto launch_failed
echo.
echo The app is open in the emulator.
exit /b 0

:sdk_not_found
echo Android SDK not found. Install it with Android Studio or set ANDROID_HOME.
goto failed
:gradle_not_found
echo Gradle wrapper not found. Make sure this script is in the project folder.
goto failed
:java_not_found
echo Java not found. Install Android Studio or set JAVA_HOME to a JDK.
goto failed
:build_failed
echo The APK build failed. Check the Gradle output above.
goto failed
:apk_not_found
echo The APK was not created at the expected project output path.
goto failed
:emulator_not_found
echo Android Emulator not found in the SDK. Install it from Android Studio SDK Manager.
goto failed
:adb_failed
echo Android Debug Bridge failed to start or communicate with the emulator.
goto failed
:avd_not_found
echo Emulator "%AVD%" was not found. Create it in Android Studio Device Manager,
echo or edit the AVD variable near the top of this script to match your emulator.
goto failed
:device_not_found
echo The emulator started, but ADB could not find it.
goto failed
:boot_timeout
echo The emulator did not finish starting within six minutes.
goto failed
:install_failed
echo Could not install the APK on the emulator.
goto failed
:launch_failed
echo The app could not be opened. Check the emulator and Android logs.
:failed
echo.
pause
exit /b 1

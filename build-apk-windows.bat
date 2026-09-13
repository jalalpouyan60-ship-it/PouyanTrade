@echo off
echo PouyanTrade APK build helper
if exist gradlew.bat (
  call gradlew.bat assembleDebug
) else (
  echo Android Studio/Gradle wrapper is required. Open the project in Android Studio and choose Build ^> Build APK(s).
)
pause

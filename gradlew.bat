@echo off
rem Fallback launcher: uses wrapper jar if present, else system gradle.
set DIR=%~dp0
if exist "%DIR%gradle\wrapper\gradle-wrapper.jar" (
  java -classpath "%DIR%gradle\wrapper\gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain %*
) else (
  where gradle >nul 2>nul
  if %ERRORLEVEL%==0 (
    gradle %*
  ) else (
    echo ERROR: gradle-wrapper.jar missing and no gradle on PATH. Run: gradle wrapper --gradle-version 8.7
    exit /b 1
  )
)

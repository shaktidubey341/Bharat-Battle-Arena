#!/bin/sh
# Bharat Battle Arena launcher.
# The binary gradle-wrapper.jar could not be bundled in this ZIP. This script:
#  1. uses the real wrapper if gradle/wrapper/gradle-wrapper.jar exists, else
#  2. falls back to a system-installed `gradle` (version 8.7+ recommended).
# To generate the real wrapper once:  gradle wrapper --gradle-version 8.7
DIR="$(cd "$(dirname "$0")" && pwd)"
JAR="$DIR/gradle/wrapper/gradle-wrapper.jar"
if [ -f "$JAR" ]; then
  exec java -classpath "$JAR" org.gradle.wrapper.GradleWrapperMain "$@"
elif command -v gradle >/dev/null 2>&1; then
  exec gradle "$@"
else
  echo "ERROR: gradle-wrapper.jar missing and no 'gradle' on PATH." >&2
  echo "Install Gradle 8.7 (e.g. 'pkg install gradle' in Termux, or sdkman) and run:" >&2
  echo "  gradle wrapper --gradle-version 8.7" >&2
  exit 1
fi

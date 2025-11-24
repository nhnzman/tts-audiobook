#!/usr/bin/env sh
# ------------------------------------------------------------------------------
# Gradle wrapper script for Unix‑like systems.
# This script uses the wrapper JAR in gradle/wrapper to download and invoke
# the correct Gradle distribution.  It is intentionally minimal to reduce
# errors when the wrapper is not yet present.  When executing on CI the
# wrapper JAR will be downloaded automatically.

BASEDIR="$(cd "$(dirname "$0")" && pwd)"
JAR="$BASEDIR/gradle/wrapper/gradle-wrapper.jar"

if [ ! -f "$JAR" ]; then
  echo "Gradle wrapper JAR not found at $JAR"
  echo "Please run 'gradle wrapper' or download the distribution specified in gradle/wrapper/gradle-wrapper.properties."
  exit 1
fi

exec java -classpath "$JAR" org.gradle.wrapper.GradleWrapperMain "$@"
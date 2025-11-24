@ECHO OFF
REM ---------------------------------------------------------------------------
REM Gradle wrapper script for Windows systems.
REM This batch file uses the wrapper JAR in gradle/wrapper to download and
REM invoke the correct Gradle distribution.  It contains minimal logic to
REM avoid problems when the wrapper is missing.

SET DIR=%~dp0
SET JAR=%DIR%gradle\wrapper\gradle-wrapper.jar

IF NOT EXIST "%JAR%" (
    ECHO Gradle wrapper JAR not found at %JAR%
    ECHO Please run 'gradle wrapper' or download the distribution specified in gradle/wrapper/gradle-wrapper.properties.
    EXIT /B 1
)

java -classpath "%JAR%" org.gradle.wrapper.GradleWrapperMain %*
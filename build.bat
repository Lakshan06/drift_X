@echo off
REM Set JAVA_HOME to use Java 17 for this build
set JAVA_HOME=%~dp0.java\jdk-17
echo Using Java 17 from: %JAVA_HOME%

REM Run gradle with the provided arguments
gradlew.bat %*

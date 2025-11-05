# Set JAVA_HOME to use Java 17 for this build
$env:JAVA_HOME = Join-Path $PSScriptRoot ".java\jdk-17"
Write-Host "Using Java 17 from: $env:JAVA_HOME" -ForegroundColor Green

# Run gradle with the provided arguments
& (Join-Path $PSScriptRoot "gradlew.bat") @args

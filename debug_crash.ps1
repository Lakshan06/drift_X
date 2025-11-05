# Drift Detector Crash Debug Script
# This script captures detailed logs during app launch to help diagnose crashes

Write-Host "======================================" -ForegroundColor Cyan
Write-Host "   Drift Detector Crash Debug Tool   " -ForegroundColor Cyan
Write-Host "======================================" -ForegroundColor Cyan
Write-Host ""

# Check if ADB is available
$adbPath = Get-Command adb -ErrorAction SilentlyContinue
if (-not $adbPath) {
    Write-Host "ERROR: ADB not found in PATH" -ForegroundColor Red
    Write-Host "Please install Android SDK Platform-Tools" -ForegroundColor Yellow
    exit 1
}

# Check if device is connected
$devices = adb devices | Select-String -Pattern "device$"
if ($devices.Count -eq 0) {
    Write-Host "ERROR: No Android device connected" -ForegroundColor Red
    Write-Host "Please connect a device or start an emulator" -ForegroundColor Yellow
    exit 1
}

Write-Host "✓ Device connected" -ForegroundColor Green

# Create logs directory if it doesn't exist
$logDir = "logs"
if (-not (Test-Path $logDir)) {
    New-Item -ItemType Directory -Path $logDir | Out-Null
}

$timestamp = Get-Date -Format "yyyy-MM-dd_HH-mm-ss"
$logFile = "$logDir/crash_debug_$timestamp.log"

Write-Host ""
Write-Host "Starting log capture..." -ForegroundColor Yellow
Write-Host "Log file: $logFile" -ForegroundColor Cyan
Write-Host ""

# Clear existing logs
adb logcat -c

# Uninstall existing app
Write-Host "[1/5] Uninstalling old app..." -ForegroundColor Yellow
adb uninstall com.driftdetector.app 2>&1 | Out-Null

# Build the app
Write-Host "[2/5] Building debug APK..." -ForegroundColor Yellow
& .\build.ps1 clean assembleDebug 2>&1 | Out-Null

if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: Build failed" -ForegroundColor Red
    exit 1
}

Write-Host "✓ Build successful" -ForegroundColor Green

# Install the app
Write-Host "[3/5] Installing app..." -ForegroundColor Yellow
& .\build.ps1 installDebug 2>&1 | Out-Null

if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: Installation failed" -ForegroundColor Red
    exit 1
}

Write-Host "✓ App installed" -ForegroundColor Green

# Start capturing logcat in background
Write-Host "[4/5] Starting log capture..." -ForegroundColor Yellow

$logcatJob = Start-Job -ScriptBlock {
    param($logFile)
    adb logcat -v time *:V | Out-File $logFile
} -ArgumentList $logFile

Start-Sleep -Seconds 2

# Launch the app
Write-Host "[5/5] Launching app..." -ForegroundColor Yellow
adb shell am start -n com.driftdetector.app/.presentation.MainActivity

Write-Host ""
Write-Host "======================================" -ForegroundColor Cyan
Write-Host "App launched - Monitoring for crashes" -ForegroundColor Cyan
Write-Host "======================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Press Ctrl+C to stop monitoring..." -ForegroundColor Yellow
Write-Host ""

# Monitor for specific log tags
Start-Sleep -Seconds 3

# Show relevant logs in real-time
try {
    adb logcat -v time | Select-String -Pattern "APP_INIT|ACTIVITY|KOIN|CRASH|AndroidRuntime|FATAL" | ForEach-Object {
        $line = $_.Line
        
        if ($line -match "FATAL|CRASH|AndroidRuntime.*FATAL") {
            Write-Host $line -ForegroundColor Red
        }
        elseif ($line -match "ERROR|✗") {
            Write-Host $line -ForegroundColor Red
        }
        elseif ($line -match "WARN|⚠") {
            Write-Host $line -ForegroundColor Yellow
        }
        elseif ($line -match "✓|SUCCESS") {
            Write-Host $line -ForegroundColor Green
        }
        else {
            Write-Host $line -ForegroundColor White
        }
    }
}
finally {
    # Stop the background logcat job
    Stop-Job $logcatJob
    Remove-Job $logcatJob
    
    Write-Host ""
    Write-Host "======================================" -ForegroundColor Cyan
    Write-Host "Log capture stopped" -ForegroundColor Cyan
    Write-Host "Full log saved to: $logFile" -ForegroundColor Green
    Write-Host "======================================" -ForegroundColor Cyan
}

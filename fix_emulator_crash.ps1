#!/usr/bin/env pwsh
# ========================================
# Emulator Crash Fix Script
# ========================================

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  DriftGuardAI Emulator Crash Fix" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Function to check if a command exists
function Test-Command {
    param($Command)
    try {
        if (Get-Command $Command -ErrorAction Stop) {
            return $true
        }
    } catch {
        return $false
    }
}

# Check if Android SDK tools are available
$adbPath = $null
if (Test-Command "adb") {
    $adbPath = "adb"
} elseif ($env:ANDROID_HOME) {
    $adbPath = Join-Path $env:ANDROID_HOME "platform-tools\adb.exe"
    if (-not (Test-Path $adbPath)) {
        $adbPath = $null
    }
}

Write-Host "Step 1: Checking environment..." -ForegroundColor Yellow
if ($adbPath) {
    Write-Host "✓ ADB found: $adbPath" -ForegroundColor Green
    
    # Check for connected devices
    $devices = & $adbPath devices | Select-Object -Skip 1 | Where-Object { $_ -match '\w' }
    if ($devices) {
        Write-Host "✓ Connected devices/emulators:" -ForegroundColor Green
        $devices | ForEach-Object { Write-Host "  $_" -ForegroundColor Gray }
    } else {
        Write-Host "⚠ No devices/emulators connected" -ForegroundColor Yellow
        Write-Host "  Please start an emulator or connect a device" -ForegroundColor Gray
    }
} else {
    Write-Host "⚠ ADB not found - some diagnostic features will be skipped" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "Step 2: Cleaning build artifacts..." -ForegroundColor Yellow

# Stop Gradle daemons
Write-Host "  Stopping Gradle daemons..." -ForegroundColor Gray
& ./gradlew --stop 2>&1 | Out-Null

# Clean project
Write-Host "  Cleaning project..." -ForegroundColor Gray
& ./gradlew clean

# Remove build directories
Write-Host "  Removing build directories..." -ForegroundColor Gray
if (Test-Path "app/build") {
    Remove-Item -Recurse -Force "app/build" -ErrorAction SilentlyContinue
}
if (Test-Path ".gradle") {
    Remove-Item -Recurse -Force ".gradle" -ErrorAction SilentlyContinue
}

Write-Host "✓ Build artifacts cleaned" -ForegroundColor Green

Write-Host ""
Write-Host "Step 3: Clearing app data on device..." -ForegroundColor Yellow

if ($adbPath -and $devices) {
    # Try to uninstall the app
    Write-Host "  Uninstalling app..." -ForegroundColor Gray
    & $adbPath uninstall com.driftdetector.app 2>&1 | Out-Null
    
    # Clear app data (in case uninstall didn't work)
    Write-Host "  Clearing app data..." -ForegroundColor Gray
    & $adbPath shell pm clear com.driftdetector.app 2>&1 | Out-Null
    
    Write-Host "✓ App data cleared" -ForegroundColor Green
} else {
    Write-Host "⚠ Skipped (no device connected)" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "Step 4: Rebuilding project..." -ForegroundColor Yellow
Write-Host "  This may take a few minutes..." -ForegroundColor Gray

$buildResult = & ./gradlew assembleDebug 2>&1
if ($LASTEXITCODE -eq 0) {
    Write-Host "✓ Build successful" -ForegroundColor Green
} else {
    Write-Host "✗ Build failed" -ForegroundColor Red
    Write-Host ""
    Write-Host "Build errors:" -ForegroundColor Red
    $buildResult | Select-String "error:" | ForEach-Object { Write-Host "  $_" -ForegroundColor Red }
    Write-Host ""
    Write-Host "Please fix the build errors and try again." -ForegroundColor Yellow
    exit 1
}

Write-Host ""
Write-Host "Step 5: Installing app..." -ForegroundColor Yellow

if ($adbPath -and $devices) {
    $apkPath = "app/build/outputs/apk/debug/app-debug.apk"
    if (Test-Path $apkPath) {
        Write-Host "  Installing APK..." -ForegroundColor Gray
        $installResult = & $adbPath install $apkPath 2>&1
        if ($LASTEXITCODE -eq 0) {
            Write-Host "✓ App installed successfully" -ForegroundColor Green
        } else {
            Write-Host "✗ Installation failed" -ForegroundColor Red
            Write-Host "  $installResult" -ForegroundColor Gray
        }
    } else {
        Write-Host "✗ APK not found at: $apkPath" -ForegroundColor Red
    }
} else {
    Write-Host "⚠ Skipped (no device connected)" -ForegroundColor Yellow
    Write-Host "  You can manually install using Android Studio" -ForegroundColor Gray
}

Write-Host ""
Write-Host "Step 6: Setting up crash monitoring..." -ForegroundColor Yellow

if ($adbPath -and $devices) {
    Write-Host "  Clearing logcat..." -ForegroundColor Gray
    & $adbPath logcat -c 2>&1 | Out-Null
    
    Write-Host "✓ Logcat cleared and ready for monitoring" -ForegroundColor Green
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host "  Next Steps" -ForegroundColor Cyan
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "1. Launch the app on your device/emulator" -ForegroundColor White
    Write-Host ""
    Write-Host "2. If it crashes, run this command to see logs:" -ForegroundColor White
    Write-Host "   adb logcat -d > crash_log.txt" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "3. Check the crash_log.txt file for error messages" -ForegroundColor White
    Write-Host ""
    Write-Host "4. You can also pull app-generated logs:" -ForegroundColor White
    Write-Host "   adb pull /data/data/com.driftdetector.app/files/ ./app_logs/" -ForegroundColor Yellow
    Write-Host ""
} else {
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host "  Next Steps" -ForegroundColor Cyan
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "1. Start an Android emulator or connect a device" -ForegroundColor White
    Write-Host ""
    Write-Host "2. In Android Studio, click the Run button" -ForegroundColor White
    Write-Host ""
    Write-Host "3. If the app crashes, check Logcat in Android Studio" -ForegroundColor White
    Write-Host "   (View -> Tool Windows -> Logcat)" -ForegroundColor Gray
    Write-Host ""
}

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Common Issues and Solutions" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Issue: Black screen or immediate crash" -ForegroundColor Yellow
Write-Host "  → Check for FATAL EXCEPTION in logcat" -ForegroundColor Gray
Write-Host "  → Run: adb logcat -v time *:E" -ForegroundColor Gray
Write-Host ""
Write-Host "Issue: Database errors" -ForegroundColor Yellow
Write-Host "  → Database was reset during this fix" -ForegroundColor Gray
Write-Host "  → If problems persist, check DATABASE_CRASH_FIX.md" -ForegroundColor Gray
Write-Host ""
Write-Host "Issue: Out of memory" -ForegroundColor Yellow
Write-Host "  → Already configured with largeHeap=true" -ForegroundColor Gray
Write-Host "  → Close other apps on emulator" -ForegroundColor Gray
Write-Host ""
Write-Host "Issue: Still crashing after fix" -ForegroundColor Yellow
Write-Host "  → Check CRASH_DIAGNOSIS_AND_FIX.md for detailed troubleshooting" -ForegroundColor Gray
Write-Host "  → Run diagnose_crash.ps1 for automated diagnosis" -ForegroundColor Gray
Write-Host ""

Write-Host "✓ Fix script completed!" -ForegroundColor Green
Write-Host ""

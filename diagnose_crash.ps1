# Black Screen Crash Diagnostic Script
# Run this to automatically diagnose why the app shows a black screen

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "DriftGuardAI - Black Screen Diagnostic" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Create logs directory
$logsDir = ".\crash_diagnosis_logs"
if (!(Test-Path $logsDir)) {
    New-Item -ItemType Directory -Path $logsDir | Out-Null
}

Write-Host "[1/8] Checking ADB connection..." -ForegroundColor Yellow
$adbDevices = adb devices
if ($adbDevices -match "device$") {
    Write-Host "✓ Device connected" -ForegroundColor Green
} else {
    Write-Host "✗ No device connected! Please connect your device/emulator." -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "[2/8] Clearing old logcat..." -ForegroundColor Yellow
adb logcat -c
Write-Host "✓ Logcat cleared" -ForegroundColor Green

Write-Host ""
Write-Host "[3/8] Checking if app is installed..." -ForegroundColor Yellow
$installed = adb shell pm list packages | Select-String "com.driftdetector.app"
if ($installed) {
    Write-Host "✓ App is installed" -ForegroundColor Green
    
    Write-Host ""
    Write-Host "[4/8] Clearing app data..." -ForegroundColor Yellow
    adb shell pm clear com.driftdetector.app
    Write-Host "✓ App data cleared" -ForegroundColor Green
} else {
    Write-Host "⚠ App not installed yet" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "[5/8] Starting logcat capture..." -ForegroundColor Yellow
$logcatJob = Start-Job -ScriptBlock {
    adb logcat -v time *:V > "$using:logsDir\full_logcat.txt"
}
Write-Host "✓ Logcat capture started (Job ID: $($logcatJob.Id))" -ForegroundColor Green

Write-Host ""
Write-Host "[6/8] Building and installing app..." -ForegroundColor Yellow
Write-Host "This may take a few minutes..." -ForegroundColor Gray

$buildOutput = & .\gradlew.bat installDebug 2>&1
$buildOutput | Out-File "$logsDir\build_output.txt"

if ($LASTEXITCODE -eq 0) {
    Write-Host "✓ Build and install successful" -ForegroundColor Green
} else {
    Write-Host "✗ Build failed! Check $logsDir\build_output.txt" -ForegroundColor Red
    Stop-Job $logcatJob
    Remove-Job $logcatJob
    exit 1
}

Write-Host ""
Write-Host "[7/8] Launching app..." -ForegroundColor Yellow
adb shell am start -n com.driftdetector.app/.presentation.MainActivity
Write-Host "✓ Launch command sent" -ForegroundColor Green

Write-Host ""
Write-Host "Waiting 10 seconds for app to start/crash..." -ForegroundColor Cyan
Start-Sleep -Seconds 10

Write-Host ""
Write-Host "[8/8] Stopping logcat and collecting logs..." -ForegroundColor Yellow
Stop-Job $logcatJob
Remove-Job $logcatJob
Write-Host "✓ Logcat saved to $logsDir\full_logcat.txt" -ForegroundColor Green

Write-Host ""
Write-Host "Pulling app-generated logs..." -ForegroundColor Yellow
adb pull /data/data/com.driftdetector.app/files/ "$logsDir\app_files\" 2>$null

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "DIAGNOSTIC COMPLETE" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Analyze logs
Write-Host "Analyzing crash logs..." -ForegroundColor Yellow
Write-Host ""

$logContent = Get-Content "$logsDir\full_logcat.txt" -ErrorAction SilentlyContinue

if ($logContent) {
    # Check for fatal exceptions
    $fatalExceptions = $logContent | Select-String "FATAL EXCEPTION"
    if ($fatalExceptions) {
        Write-Host "❌ FATAL EXCEPTION FOUND:" -ForegroundColor Red
        Write-Host ""
        $fatalExceptions | Select-Object -First 20 | ForEach-Object { Write-Host $_ -ForegroundColor Red }
        Write-Host ""
    }
    
    # Check for specific errors
    $koinErrors = $logContent | Select-String "KOIN.*FAILED"
    if ($koinErrors) {
        Write-Host "❌ KOIN INITIALIZATION FAILED:" -ForegroundColor Red
        $koinErrors | ForEach-Object { Write-Host $_ -ForegroundColor Red }
        Write-Host ""
    }
    
    $dbErrors = $logContent | Select-String "Database.*FAILED|SQLite"
    if ($dbErrors) {
        Write-Host "❌ DATABASE ERROR FOUND:" -ForegroundColor Red
        $dbErrors | Select-Object -First 10 | ForEach-Object { Write-Host $_ -ForegroundColor Red }
        Write-Host ""
    }
    
    $composeErrors = $logContent | Select-String "Compose.*exception|IllegalStateException"
    if ($composeErrors) {
        Write-Host "❌ COMPOSE ERROR FOUND:" -ForegroundColor Red
        $composeErrors | Select-Object -First 10 | ForEach-Object { Write-Host $_ -ForegroundColor Red }
        Write-Host ""
    }
    
    # Check for successful initialization
    $koinSuccess = $logContent | Select-String "✓ Koin initialized successfully"
    $dbSuccess = $logContent | Select-String "✓ Database created successfully"
    $mainActivityStart = $logContent | Select-String "MainActivity onCreate"
    
    Write-Host "Initialization Status:" -ForegroundColor Cyan
    if ($koinSuccess) {
        Write-Host "  ✓ Koin initialized" -ForegroundColor Green
    } else {
        Write-Host "  ✗ Koin NOT initialized" -ForegroundColor Red
    }
    
    if ($dbSuccess) {
        Write-Host "  ✓ Database created" -ForegroundColor Green
    } else {
        Write-Host "  ✗ Database NOT created" -ForegroundColor Red
    }
    
    if ($mainActivityStart) {
        Write-Host "  ✓ MainActivity started" -ForegroundColor Green
    } else {
        Write-Host "  ✗ MainActivity NOT started" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "DIAGNOSTIC RESULTS" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Logs saved in: $logsDir" -ForegroundColor Cyan
Write-Host ""
Write-Host "Files to check:" -ForegroundColor Yellow
Write-Host "  1. full_logcat.txt      - Complete system log" -ForegroundColor White
Write-Host "  2. build_output.txt     - Build process log" -ForegroundColor White
Write-Host "  3. app_files\app_init.log  - App initialization log" -ForegroundColor White
Write-Host "  4. app_files\crash_*.log   - Crash details (if any)" -ForegroundColor White
Write-Host ""
Write-Host "Next steps:" -ForegroundColor Yellow
Write-Host "  1. Check the files above for errors" -ForegroundColor White
Write-Host "  2. Read BLACK_SCREEN_FIX.md for solutions" -ForegroundColor White
Write-Host "  3. If still stuck, share these logs for help" -ForegroundColor White
Write-Host ""

# Open the logs directory
Write-Host "Opening logs directory..." -ForegroundColor Cyan
Invoke-Item $logsDir

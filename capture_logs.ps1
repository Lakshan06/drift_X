# Simple log capture script for Drift Detector
# Use this to capture logs from a running or crashed app

Write-Host "======================================" -ForegroundColor Cyan
Write-Host "   Log Capture Tool   " -ForegroundColor Cyan
Write-Host "======================================" -ForegroundColor Cyan
Write-Host ""

# Create logs directory
$logDir = "logs"
if (-not (Test-Path $logDir)) {
    New-Item -ItemType Directory -Path $logDir | Out-Null
}

$timestamp = Get-Date -Format "yyyy-MM-dd_HH-mm-ss"
$logFile = "$logDir/logcat_$timestamp.log"

Write-Host "Capturing logcat to: $logFile" -ForegroundColor Cyan
Write-Host ""

# Capture all logs
Write-Host "Dumping all logcat history..." -ForegroundColor Yellow
adb logcat -d -v time > $logFile

Write-Host "✓ Logs captured" -ForegroundColor Green
Write-Host ""

# Extract crash information
Write-Host "======================================" -ForegroundColor Cyan
Write-Host "Searching for crashes..." -ForegroundColor Yellow
Write-Host "======================================" -ForegroundColor Cyan
Write-Host ""

$crashLines = Select-String -Path $logFile -Pattern "FATAL|AndroidRuntime|CRASH" -Context 0,20

if ($crashLines.Count -gt 0) {
    Write-Host "CRASH FOUND!" -ForegroundColor Red
    Write-Host ""
    $crashLines | ForEach-Object {
        Write-Host $_.Line -ForegroundColor Red
    }
    Write-Host ""
} else {
    Write-Host "No fatal crashes found in logs" -ForegroundColor Green
}

# Show APP_INIT logs
Write-Host "======================================" -ForegroundColor Cyan
Write-Host "App Initialization Logs:" -ForegroundColor Yellow
Write-Host "======================================" -ForegroundColor Cyan
Write-Host ""

$initLogs = Select-String -Path $logFile -Pattern "APP_INIT"
if ($initLogs.Count -gt 0) {
    $initLogs | ForEach-Object {
        $line = $_.Line
        if ($line -match "✓|SUCCESS") {
            Write-Host $line -ForegroundColor Green
        } elseif ($line -match "✗|FAIL") {
            Write-Host $line -ForegroundColor Red
        } else {
            Write-Host $line
        }
    }
} else {
    Write-Host "No APP_INIT logs found" -ForegroundColor Yellow
}

Write-Host ""

# Show KOIN logs
Write-Host "======================================" -ForegroundColor Cyan
Write-Host "Dependency Injection (Koin) Logs:" -ForegroundColor Yellow
Write-Host "======================================" -ForegroundColor Cyan
Write-Host ""

$koinLogs = Select-String -Path $logFile -Pattern "KOIN"
if ($koinLogs.Count -gt 0) {
    $koinLogs | ForEach-Object {
        $line = $_.Line
        if ($line -match "✓|SUCCESS") {
            Write-Host $line -ForegroundColor Green
        } elseif ($line -match "✗|FAIL") {
            Write-Host $line -ForegroundColor Red
        } else {
            Write-Host $line
        }
    }
} else {
    Write-Host "No KOIN logs found" -ForegroundColor Yellow
}

Write-Host ""

# Show ACTIVITY logs
Write-Host "======================================" -ForegroundColor Cyan
Write-Host "Activity Lifecycle Logs:" -ForegroundColor Yellow
Write-Host "======================================" -ForegroundColor Cyan
Write-Host ""

$activityLogs = Select-String -Path $logFile -Pattern "ACTIVITY"
if ($activityLogs.Count -gt 0) {
    $activityLogs | ForEach-Object {
        $line = $_.Line
        if ($line -match "✓|SUCCESS") {
            Write-Host $line -ForegroundColor Green
        } elseif ($line -match "✗|FAIL") {
            Write-Host $line -ForegroundColor Red
        } else {
            Write-Host $line
        }
    }
} else {
    Write-Host "No ACTIVITY logs found" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "======================================" -ForegroundColor Cyan
Write-Host "Full log file: $logFile" -ForegroundColor Green
Write-Host "======================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "To view the full log, open: $logFile" -ForegroundColor Cyan
Write-Host ""

# Check for saved crash logs on device
Write-Host "Checking for crash logs on device..." -ForegroundColor Yellow
$deviceLogs = adb shell "ls /data/data/com.driftdetector.app/files/*.log" 2>$null

if ($deviceLogs) {
    Write-Host ""
    Write-Host "Device crash logs found:" -ForegroundColor Green
    Write-Host $deviceLogs
    Write-Host ""
    Write-Host "To retrieve them, run:" -ForegroundColor Cyan
    Write-Host "  adb shell 'cat /data/data/com.driftdetector.app/files/crash_*.log'" -ForegroundColor White
    Write-Host "  adb shell 'cat /data/data/com.driftdetector.app/files/app_init.log'" -ForegroundColor White
    Write-Host ""
    
    # Try to pull the logs
    Write-Host "Attempting to pull device logs..." -ForegroundColor Yellow
    adb pull /data/data/com.driftdetector.app/files/crash_*.log $logDir 2>$null
    adb pull /data/data/com.driftdetector.app/files/app_init.log $logDir 2>$null
    adb pull /data/data/com.driftdetector.app/files/timber.log $logDir 2>$null
    
    if (Test-Path "$logDir/app_init.log") {
        Write-Host ""
        Write-Host "======================================" -ForegroundColor Cyan
        Write-Host "Device App Init Log:" -ForegroundColor Yellow
        Write-Host "======================================" -ForegroundColor Cyan
        Get-Content "$logDir/app_init.log" | ForEach-Object {
            if ($_ -match "✓") {
                Write-Host $_ -ForegroundColor Green
            } elseif ($_ -match "✗") {
                Write-Host $_ -ForegroundColor Red
            } else {
                Write-Host $_
            }
        }
    }
}

Write-Host ""
Write-Host "Log capture complete!" -ForegroundColor Green

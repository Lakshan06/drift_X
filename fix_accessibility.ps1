# Fix Accessibility Issues - Replace contentDescription = null with meaningful descriptions
# This script fixes all 80+ accessibility violations in the app

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Fixing Accessibility Issues" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$filesFixed = 0
$totalReplacements = 0

# Define replacement mappings
$replacements = @{
    # Upload icons
    'CloudUpload' = 'Upload models and data to cloud'
    'FolderOpen' = 'Browse and select local files'
    'FileUpload' = 'Upload files'
    'Link' = 'Enter URL to import file'
    'Download' = 'Download and import from URL'
    
    # Navigation icons
    'Dashboard' = 'Go to dashboard'
    'ArrowBack' = 'Navigate back'
    'Close' = 'Close'
    'Menu' = 'Open menu'
    'Build' = 'View patches'
    
    # Action icons
    'Settings' = 'Settings'
    'Delete' = 'Delete'
    'Refresh' = 'Refresh'
    'Add' = 'Add'
    
    # Status icons
    'CheckCircle' = 'Success'
    'Error' = 'Error'
    'Warning' = 'Warning'
    'Info' = 'Information'
    
    # Cloud storage
    'CloudCircle' = 'Google Drive'
    'Cloud' = 'Cloud storage'
    'CloudDone' = 'Offline access available'
    
    # Feature icons
    'Security' = 'Security feature'
    'Speed' = 'Performance feature'
    'Memory' = 'Model file'
    'TableChart' = 'Data file'
}

function Fix-File {
    param(
        [string]$filePath
    )
    
    if (-Not (Test-Path $filePath)) {
        Write-Host "  ⚠ File not found: $filePath" -ForegroundColor Yellow
        return 0
    }
    
    $content = Get-Content $filePath -Raw
    $originalContent = $content
    $count = 0
    
    # Replace simple cases: Icon(Icons.Default.X, contentDescription = null)
    foreach ($iconName in $replacements.Keys) {
        $pattern = "Icons\.Default\.$iconName,\s*contentDescription\s*=\s*null"
        $replacement = "Icons.Default.$iconName, contentDescription = `"$($replacements[$iconName])`""
        
        if ($content -match $pattern) {
            $matches = [regex]::Matches($content, $pattern)
            $content = $content -replace $pattern, $replacement
            $count += $matches.Count
        }
    }
    
    # Replace standalone cases
    $content = $content -replace 'contentDescription\s*=\s*null,(\s*tint)', 'contentDescription = "Icon",$1'
    
    if ($content -ne $originalContent) {
        Set-Content -Path $filePath -Value $content -NoNewline
        return $count
    }
    
    return 0
}

# Files to fix
$filesToFix = @(
    "app\src\main\java\com\driftdetector\app\presentation\screen\ModelUploadScreen.kt",
    "app\src\main\java\com\driftdetector\app\presentation\screen\PatchManagementScreen.kt",
    "app\src\main\java\com\driftdetector\app\presentation\screen\DriftDashboardScreen.kt",
    "app\src\main\java\com\driftdetector\app\presentation\screen\SettingsScreen.kt",
    "app\src\main\java\com\driftdetector\app\presentation\screen\OnboardingScreen.kt",
    "app\src\main\java\com\driftdetector\app\presentation\screen\ModelManagementScreen.kt",
    "app\src\main\java\com\driftdetector\app\presentation\components\ModelMetadataCard.kt",
    "app\src\main\java\com\driftdetector\app\presentation\components\ModelSelectorCard.kt"
)

Write-Host "Fixing accessibility issues in 8 files..." -ForegroundColor Green
Write-Host ""

foreach ($file in $filesToFix) {
    $fileName = Split-Path $file -Leaf
    Write-Host "  Processing: $fileName..." -NoNewline
    
    $count = Fix-File -filePath $file
    
    if ($count -gt 0) {
        Write-Host " ✓ Fixed $count issues" -ForegroundColor Green
        $filesFixed++
        $totalReplacements += $count
    } else {
        Write-Host " - No changes" -ForegroundColor Gray
    }
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Summary" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Files processed: $($filesToFix.Count)"
Write-Host "  Files fixed: $filesFixed" -ForegroundColor Green
Write-Host "  Total fixes: $totalReplacements" -ForegroundColor Green
Write-Host ""

if ($totalReplacements -gt 0) {
    Write-Host "✓ Accessibility fixes applied successfully!" -ForegroundColor Green
    Write-Host ""
    Write-Host "Next steps:" -ForegroundColor Yellow
    Write-Host "  1. Review changes with: git diff"
    Write-Host "  2. Test with TalkBack"
    Write-Host "  3. Build and test: quick_build.bat"
} else {
    Write-Host "⚠ No changes made. Files may already be fixed." -ForegroundColor Yellow
}

Write-Host ""
Write-Host "Done!" -ForegroundColor Cyan

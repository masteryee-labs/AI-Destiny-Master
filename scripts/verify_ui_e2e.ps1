param(
    [string]$Variant = "withAdsDebug",
    [string]$Package = "com.aidestinymaster",
    [string]$AppMain = "com.aidestinymaster/.app.MainActivity"
)
$ErrorActionPreference = "Stop"

# Resolve adb path
$adb = $env:ANDROID_HOME + "\platform-tools\adb.exe"
if (!(Test-Path $adb)) {
    $adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
}
if (!(Test-Path $adb)) {
    $adb = "C:\Users\MasterYee\AppData\Local\Android\Sdk\platform-tools\adb.exe"
}
if (!(Test-Path $adb)) { throw "adb not found. Please ensure ANDROID_HOME is set." }

# Output files
$outDir = Join-Path $PSScriptRoot "..\app"
$logFile = Join-Path $PSScriptRoot "logcat_ui_e2e.txt"
$uiDumpDevice = "/sdcard/window_dump.xml"
$uiDumpLocal = Join-Path $PSScriptRoot "window_dump.xml"
$summaryFile = Join-Path $PSScriptRoot "verify_ui_e2e_summary.txt"

Write-Host "[1/6] Building & Installing $Variant" -ForegroundColor Cyan
$gradlew = Join-Path $PSScriptRoot "..\gradlew.bat"
& $gradlew ":app:install$Variant" --no-daemon --console=plain

Write-Host "[2/6] Clearing app data & logs" -ForegroundColor Cyan
& $adb shell pm clear $Package | Out-Null
& $adb logcat -c
Start-Sleep -Seconds 1

Write-Host "[3/6] Launching E2E deep link to auto-create and generate report" -ForegroundColor Cyan
$null = & $adb shell am start -a android.intent.action.VIEW -d "aidm://e2e/generate" $Package
Start-Sleep -Seconds 2
Write-Host "Ensuring MainActivity in foreground" -ForegroundColor DarkCyan
$null = & $adb shell am start -n $AppMain

Write-Host "[4/6] Waiting up to 30s for UI to reach ReportScreen..." -ForegroundColor Cyan
$hasKeyCard = $false
$hasStardust = $false
for ($i = 0; $i -lt 10; $i++) {
    Start-Sleep -Seconds 3
    $null = & $adb shell uiautomator dump $uiDumpDevice
    $null = & $adb pull $uiDumpDevice $uiDumpLocal
    if (Test-Path $uiDumpLocal) {
        $ui = Get-Content $uiDumpLocal -Raw
        $hasKeyCard = ($ui -match "key_card_") -or ($ui -match "key_cards_section")
        $hasStardust = $hasStardust -or ($ui -match "stardust_progress")
        if ($hasKeyCard) { break }
    }
}

Write-Host "[5/6] Capturing logcat (key filters)" -ForegroundColor Cyan
& $adb logcat -d -v time > $logFile

# Try to extract report id and open report screen explicitly
$reportId = ""
try {
    $m = Select-String -Path $logFile -Pattern "E2EReportId=(.+)" -SimpleMatch:$false | Select-Object -First 1
    if ($m) {
        $line = $m.Line
        $idx = $line.IndexOf("E2EReportId=")
        if ($idx -ge 0) {
            $reportId = $line.Substring($idx + 12).Trim()
        }
    }
} catch {}
if ($reportId -ne "") {
    Write-Host "Opening report via deeplink: aidm://report/$reportId" -ForegroundColor DarkCyan
    $null = & $adb shell am start -a android.intent.action.VIEW -d "aidm://report/$reportId" $Package
    Start-Sleep -Seconds 2
    $null = & $adb shell uiautomator dump $uiDumpDevice
    $null = & $adb pull $uiDumpDevice $uiDumpLocal
}

Write-Host "[6/6] Dumping UI hierarchy via uiautomator" -ForegroundColor Cyan
& $adb shell uiautomator dump $uiDumpDevice | Out-Null
& $adb pull $uiDumpDevice $uiDumpLocal | Out-Null

# Parse summary
$log = Get-Content $logFile -Raw
$hasInit = $log -match "AIDMModelInit|onCreate start"
$hasFatal = $log -match "FATAL EXCEPTION"
$hasWorkSuccess = $log -match "WorkInfo\s*State\.SUCCEEDED|Finished work|Returning\s*SUCCESS|ai_report_"

$ui = if (Test-Path $uiDumpLocal) { Get-Content $uiDumpLocal -Raw } else { "" }
$hasKeyCard = $hasKeyCard -or ($ui -match "key_card_") -or ($ui -match "key_cards_section")
$hasStardust = $hasStardust -or ($ui -match "stardust_progress")

Write-Host "===== E2E SUMMARY =====" -ForegroundColor Yellow
Write-Host ("ModelInitLogs: " + ($hasInit))
Write-Host ("StardustSeen:  " + ($hasStardust))
Write-Host ("WorkSuccess:   " + ($hasWorkSuccess))
Write-Host ("HasKeyCards:   " + ($hasKeyCard))
Write-Host ("HasFatal:      " + ($hasFatal))
Write-Host "Log: $logFile"
Write-Host "UI:  $uiDumpLocal"

# Also write to summary file for cmd callers
@(
    "ModelInitLogs: $hasInit",
    "StardustSeen:  $hasStardust",
    "WorkSuccess:   $hasWorkSuccess",
    "HasKeyCards:   $hasKeyCard",
    "HasFatal:      $hasFatal",
    "Log: $logFile",
    "UI:  $uiDumpLocal"
) | Out-File -FilePath $summaryFile -Encoding utf8

if ($hasFatal) { exit 2 }
if (-not $hasInit -or -not $hasKeyCard) { exit 3 }
exit 0

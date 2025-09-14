param(
  [string]$Avd = "Pixel6PlayApi35",
  [string]$Package = "com.aidestinymaster",
  [string]$OutFile = "log_modelinstall_fail.txt",
  [int]$WaitSeconds = 90
)
$ErrorActionPreference = 'Stop'

function Ensure-EmulatorAndApp {
  & "$PSScriptRoot/qa_run.ps1" -Avd $Avd -Package $Package -ApkPath "app/build/outputs/apk/debug/app-debug.apk" | Out-Host
}

function Clear-And-Launch {
  & adb shell pm clear $Package | Out-Host
  & adb shell monkey -p $Package -c android.intent.category.LAUNCHER 1 | Out-Host
}

function Wait-For-ModelSha([int]$timeoutSec) {
  $elapsed = 0
  while ($elapsed -lt $timeoutSec) {
    $probe = & adb shell run-as $Package sh -c 'ls -l files/models/tinyllama-q8.onnx.sha256 2>/dev/null || true'
    if ($probe -and ($probe -match 'tinyllama-q8.onnx.sha256')) { return $true }
    Start-Sleep -Seconds 3
    $elapsed += 3
  }
  return $false
}

function Corrupt-Sha {
  & adb shell run-as $Package sh -c 'echo 0000 > files/models/tinyllama-q8.onnx.sha256' | Out-Null
}

function Relaunch-And-Capture {
  & adb shell am force-stop $Package | Out-Null
  & adb shell monkey -p $Package -c android.intent.category.LAUNCHER 1 | Out-Null
  if (-not (Test-Path $OutFile)) { New-Item -Path $OutFile -ItemType File -Force | Out-Null }
  & "$PSScriptRoot/one_shot_model_logs.ps1" -Seconds 20 -OutFile $OutFile | Out-Host
}

try {
  Ensure-EmulatorAndApp
  Clear-And-Launch
  $ok = Wait-For-ModelSha -timeoutSec $WaitSeconds
  if (-not $ok) { Write-Host "models sha not ready within $WaitSeconds s" }
  if ($ok) { Corrupt-Sha }
  Relaunch-And-Capture
  Write-Host "Done."
  exit 0
} catch {
  Write-Error $_
  exit 1
}

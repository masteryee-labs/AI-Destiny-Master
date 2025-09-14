param(
  [string]$Avd = "Pixel6PlayApi35",
  [string]$Package = "com.aidestinymaster",
  [int]$Phase1Seconds = 40,
  [int]$Phase2Seconds = 20,
  [int]$WaitShaSeconds = 120,
  [string]$Out1 = "log_modelinstall_phase1.txt",
  [string]$Out2 = "log_modelinstall_fail.txt"
)
$ErrorActionPreference = 'Stop'

function Ensure-EmuApp {
  & "$PSScriptRoot/qa_run.ps1" -Avd $Avd -Package $Package -ApkPath "app/build/outputs/apk/debug/app-debug.apk" | Out-Host
}

function ClearData {
  & adb shell pm clear $Package | Out-Host
}

function CaptureOnce([int]$sec, [string]$outfile) {
  if (-not (Test-Path $outfile)) { New-Item -Path $outfile -ItemType File -Force | Out-Null }
  & "$PSScriptRoot/run_and_capture_model_logs.ps1" -Package $Package -Seconds $sec -OutFile $outfile | Out-Host
}

function WaitSha([int]$timeout) {
  $elapsed = 0
  while ($elapsed -lt $timeout) {
    $probe = & adb shell run-as $Package sh -c 'ls -l files/models/tinyllama-q8.onnx.sha256 2>/dev/null || true'
    if ($probe -and ($probe -match 'tinyllama-q8.onnx.sha256')) { return $true }
    Start-Sleep -Seconds 3
    $elapsed += 3
  }
  return $false
}

function CorruptSha {
  & adb shell run-as $Package sh -c 'echo 0000 > files/models/tinyllama-q8.onnx.sha256' | Out-Null
}

try {
  & adb logcat -c | Out-Null
  Ensure-EmuApp
  ClearData
  # Phase 1: first launch to install/unzip models
  CaptureOnce -sec $Phase1Seconds -outfile $Out1
  # Wait until sha exists (unzipped)
  $ok = WaitSha -timeout $WaitShaSeconds
  if (-not $ok) { Write-Host "WARN: sha file not found within $WaitShaSeconds s" }
  if ($ok) { CorruptSha }
  # Phase 2: relaunch and capture failure validation
  CaptureOnce -sec $Phase2Seconds -outfile $Out2
  Write-Host "Done"
  exit 0
} catch {
  Write-Error $_
  exit 1
}

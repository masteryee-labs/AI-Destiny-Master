param(
  [string]$Avd = "Pixel6PlayApi35",
  [string]$Package = "com.aidestinymaster",
  [string]$ApkPath = "",
  [switch]$StartLog = $true,
  [string]$LogPath = "log_app.txt"
)

$ErrorActionPreference = 'Stop'

function Test-EmulatorReady {
  $list = & adb devices 2>$null
  if (-not $list) { return $false }
  # Look for lines like: emulator-5554\tdevice
  return ($list -split "`n" | Where-Object { $_ -match "^emulator-\d+\s+device" }).Count -gt 0
}

function Ensure-Emulator {
  if (Test-EmulatorReady) { Write-Host "Emulator already running."; return }
  $emuExe = Join-Path $env:ANDROID_HOME 'emulator\emulator.exe'
  if (-not (Test-Path $emuExe)) { throw "Emulator not found: $emuExe" }
  Write-Host "Starting emulator $Avd ..."
  Start-Process -FilePath $emuExe -ArgumentList @('-avd', $Avd, '-no-boot-anim', '-netdelay', 'none', '-netspeed', 'full') | Out-Null
  & adb wait-for-device | Out-Null
  do {
    Start-Sleep -Seconds 2
    $boot = (& adb shell getprop sys.boot_completed) 2>$null
    $boot = ($boot | Out-String).Trim()
  } while ($boot -ne '1')
  Write-Host "Emulator boot completed."
}

function Install-AppIfNeeded {
  if ([string]::IsNullOrWhiteSpace($ApkPath)) { return }
  if (-not (Test-Path $ApkPath)) { Write-Host "APK not found: $ApkPath"; return }
  $pmPath = (& adb shell pm path $Package) 2>$null
  if (-not $pmPath) {
    Write-Host "Installing $ApkPath ..."
    & adb install -r $ApkPath | Write-Host
  } else {
    Write-Host "App already installed: $Package"
  }
}

function Launch-App {
  Write-Host "Launching $Package ..."
  & adb shell monkey -p $Package -c android.intent.category.LAUNCHER 1 | Write-Host
}

function Start-LogcatJob {
  $jobName = 'AIDMLogcat'
  $existing = Get-Job -Name $jobName -ErrorAction SilentlyContinue
  if ($existing) {
    if ($existing.State -eq 'Running') { Stop-Job $existing -Force }
    Remove-Job $existing -Force
  }
  Write-Host "Starting logcat to $LogPath (UTF-8) ..."
  Start-Job -Name $jobName -ScriptBlock {
    chcp 65001 > $null
    $log = $using:LogPath
    $adb = 'adb'
    & $adb logcat -v time *:V | Out-File -FilePath $log -Encoding utf8
  } | Out-Null
}

try {
  Ensure-Emulator
  Install-AppIfNeeded
  Launch-App
  if ($StartLog) { Start-LogcatJob }
  Write-Host "Done."
  exit 0
} catch {
  Write-Error $_
  exit 1
}

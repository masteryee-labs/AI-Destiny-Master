param(
  [int]$Seconds = 10,
  [string]$OutFile = "log_modelinstall.txt"
)
$ErrorActionPreference = 'Stop'
$abs = Resolve-Path $OutFile
# Start filtered logcat capturing only our tags to reduce noise
$proc = Start-Process -FilePath adb -ArgumentList @('logcat','-v','time','AIDMModelInit:V','ModelInstaller:V','*:S') -PassThru -WindowStyle Hidden -RedirectStandardOutput $abs
Start-Sleep -Seconds $Seconds
try { Stop-Process -Id $proc.Id -Force -ErrorAction SilentlyContinue } catch {}
Write-Host "Captured filtered logs ($Seconds s) -> $abs"

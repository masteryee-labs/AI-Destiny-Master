param(
  [string]$LogPath = "log_app.txt"
)
$ErrorActionPreference = 'Stop'
$abs = Resolve-Path $LogPath
# Stop existing adb logcat processes started by us is tricky, handled by separate stop script via jobs. Here we just start new.
Start-Process -WindowStyle Hidden -FilePath adb -ArgumentList @('logcat','-v','time','*:V') -RedirectStandardOutput $abs -RedirectStandardError $abs
Write-Host "logcat started -> $abs"

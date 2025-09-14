param(
  [int]$Seconds = 12,
  [string]$Filter = "",
  [string]$OutFile = "log_snippet.txt"
)
$ErrorActionPreference = 'Stop'
$cmd = 'cmd.exe'
# Build command: adb logcat -v time [| findstr /i "Filter"] 1> OutFile 2>&1
if ([string]::IsNullOrWhiteSpace($Filter)) {
  $args = @('/c', "adb logcat -v time 1> `"$OutFile`" 2>&1")
} else {
  $args = @('/c', "adb logcat -v time | findstr /i `"$Filter`" 1> `"$OutFile`" 2>&1")
}
$proc = Start-Process -FilePath $cmd -ArgumentList $args -PassThru -WindowStyle Hidden
Start-Sleep -Seconds $Seconds
try { Stop-Process -Id $proc.Id -Force -ErrorAction SilentlyContinue } catch {}
Write-Host "Captured $Seconds s logs -> $OutFile"

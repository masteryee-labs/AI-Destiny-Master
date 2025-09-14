param(
  [string]$Package = "com.aidestinymaster",
  [int]$Seconds = 30,
  [string]$OutFile = "log_capture.txt"
)
$ErrorActionPreference = 'Stop'
# Start filtered logcat in background and capture PID
$abs = Resolve-Path . | Join-Path -ChildPath $OutFile
if (-not (Test-Path $abs)) { New-Item -Path $abs -ItemType File -Force | Out-Null }
$startInfo = New-Object System.Diagnostics.ProcessStartInfo
$startInfo.FileName = "adb"
$startInfo.Arguments = "logcat -v time AIDMModelInit:V ModelInstaller:V *:S"
$startInfo.RedirectStandardOutput = $true
$startInfo.UseShellExecute = $false
$startInfo.CreateNoWindow = $true
$proc = New-Object System.Diagnostics.Process
$proc.StartInfo = $startInfo
[void]$proc.Start()
$stream = New-Object System.IO.StreamWriter($abs, $false, [System.Text.Encoding]::UTF8)
Start-Sleep -Milliseconds 200
# Launch app
& adb shell am force-stop $Package | Out-Null
& adb shell monkey -p $Package -c android.intent.category.LAUNCHER 1 | Out-Null
# Pump output for N seconds
$sw = [System.Diagnostics.Stopwatch]::StartNew()
while ($sw.Elapsed.TotalSeconds -lt $Seconds) {
  while (-not $proc.HasExited -and -not $proc.StandardOutput.EndOfStream) {
    $line = $proc.StandardOutput.ReadLine()
    $stream.WriteLine($line)
  }
  Start-Sleep -Milliseconds 100
}
try { if (-not $proc.HasExited) { $proc.Kill() } } catch {}
$stream.Flush(); $stream.Dispose()
Write-Host "Captured filtered logs ($Seconds s) -> $abs"

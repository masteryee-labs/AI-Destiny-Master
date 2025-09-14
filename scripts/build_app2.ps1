param(
    [string]$Tasks = ":app:assembleDebug",
    [int]$TimeoutSec = 3600
)

$ErrorActionPreference = "Stop"
$env:GRADLE_EXIT_CONSOLE = "true"

$psi = New-Object System.Diagnostics.ProcessStartInfo
$psi.FileName = ".\gradlew.bat"
$psi.Arguments = "$Tasks --no-daemon --console=plain"
$psi.WorkingDirectory = (Get-Location).Path
$psi.RedirectStandardOutput = $true
$psi.RedirectStandardError = $true
$psi.UseShellExecute = $false
$psi.CreateNoWindow = $true

$proc = [System.Diagnostics.Process]::Start($psi)
$sw = [System.Diagnostics.Stopwatch]::StartNew()

$outFile = "buildlog.txt"
$errFile = "buildlog-kotlin.txt"
$fsOut = [System.IO.StreamWriter]::new($outFile, $false, [System.Text.Encoding]::UTF8)
$fsErr = [System.IO.StreamWriter]::new($errFile, $false, [System.Text.Encoding]::UTF8)

try {
    while (-not $proc.HasExited) {
        if (-not $proc.StandardOutput.EndOfStream) {
            $line = $proc.StandardOutput.ReadLine()
            if ($null -ne $line) { Write-Output $line; $fsOut.WriteLine($line) }
        }
        if (-not $proc.StandardError.EndOfStream) {
            $eline = $proc.StandardError.ReadLine()
            if ($null -ne $eline) { Write-Output $eline; $fsErr.WriteLine($eline) }
        }
        if ($sw.Elapsed.TotalSeconds -ge $TimeoutSec) {
            Write-Warning "Gradle build timeout reached. Killing process..."
            try { $proc.Kill() } catch {}
            break
        }
        Start-Sleep -Milliseconds 100
    }
    while (-not $proc.StandardOutput.EndOfStream) {
        $line = $proc.StandardOutput.ReadLine()
        if ($null -ne $line) { Write-Output $line; $fsOut.WriteLine($line) }
    }
    while (-not $proc.StandardError.EndOfStream) {
        $eline = $proc.StandardError.ReadLine()
        if ($null -ne $eline) { Write-Output $eline; $fsErr.WriteLine($eline) }
    }
} finally {
    $fsOut.Flush(); $fsOut.Close()
    $fsErr.Flush(); $fsErr.Close()
}

$exit = 0
try { $exit = $proc.ExitCode } catch { $exit = 1 }
Write-Output ("Gradle ExitCode=" + $exit)
exit $exit

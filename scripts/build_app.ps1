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
    # Drain remaining streams
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
param(
    [string]$Tasks = ":app:assembleDebug",
    [int]$TimeoutSec = 3600
)

$ErrorActionPreference = "Stop"
$env:GRADLE_EXIT_CONSOLE = "true"

# 組合參數：關閉 Daemon，純文字輸出，避免卡住
$gradleArgs = @($Tasks, "--no-daemon", "--console=plain")

Write-Host "[Build] 執行 Gradle：./gradlew.bat $($gradleArgs -join ' ')" -ForegroundColor Cyan

# 以 Start-Process 啟動，確保可控的結束行為與逾時
$psi = New-Object System.Diagnostics.ProcessStartInfo
$psi.FileName = ".\gradlew.bat"
$psi.Arguments = ($gradleArgs -join ' ')
$psi.WorkingDirectory = (Get-Location).Path
$psi.RedirectStandardOutput = $true
$psi.RedirectStandardError = $true
$psi.UseShellExecute = $false
$psi.CreateNoWindow = $true

$proc = New-Object System.Diagnostics.Process
$proc.StartInfo = $psi
[void]$proc.Start()

# 同步讀取輸出並鏡像到主控台
$stdOutTask = [System.Threading.Tasks.Task]::Run({
    while (-not $proc.HasExited) {
        $line = $proc.StandardOutput.ReadLine()
        if ($null -ne $line) { Write-Host $line }
    }
    while (-not $proc.StandardOutput.EndOfStream) {
        Write-Host $proc.StandardOutput.ReadLine()
    }
})
$stdErrTask = [System.Threading.Tasks.Task]::Run({
    while (-not $proc.HasExited) {
        $line = $proc.StandardError.ReadLine()
        if ($null -ne $line) { Write-Host $line -ForegroundColor Yellow }
    }
    while (-not $proc.StandardError.EndOfStream) {
        Write-Host $proc.StandardError.ReadLine() -ForegroundColor Yellow
    }
})

$deadline = (Get-Date).AddSeconds($TimeoutSec)
while (-not $proc.HasExited) {
    Start-Sleep -Milliseconds 200
    if ((Get-Date) -gt $deadline) {
        Write-Warning "Build 逾時($TimeoutSec 秒)，嘗試終止 Gradle。"
        try { $proc.Kill() } catch {}
        break
    }
}

# 等待輸出任務收尾
[System.Threading.Tasks.Task]::WaitAll(@($stdOutTask, $stdErrTask))

$exit = $proc.ExitCode
Write-Host "[Build] Gradle 結束碼：$exit" -ForegroundColor Green
exit $exit

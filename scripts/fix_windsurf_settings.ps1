$ErrorActionPreference = 'Stop'
$settingsPath = Join-Path $env:APPDATA 'Windsurf\User\settings.json'
$backupPath   = Join-Path $env:APPDATA 'Windsurf\User\settings.backup.json'

if (-not (Test-Path -LiteralPath $settingsPath)) {
    Write-Error "Settings file not found: $settingsPath"
}

# Backup
Copy-Item -LiteralPath $settingsPath -Destination $backupPath -Force

# Load and modify JSON
$jsonText = Get-Content -LiteralPath $settingsPath -Raw
$jsonObj = $jsonText | ConvertFrom-Json
# Remove $schema if exists
$schemaProp = $jsonObj.PSObject.Properties | Where-Object { $_.Name -eq '$schema' }
if ($null -ne $schemaProp) {
    [void]$jsonObj.PSObject.Properties.Remove('$schema')
}

# Save back
$jsonOut = $jsonObj | ConvertTo-Json -Depth 100
Set-Content -LiteralPath $settingsPath -Value $jsonOut -Encoding UTF8

Write-Output "Updated settings.json (backup at $backupPath)."

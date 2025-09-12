$ErrorActionPreference = 'Stop'
$settingsDir = Join-Path $env:APPDATA 'Windsurf\User'
$settingsPath = Join-Path $settingsDir 'settings.json'

Write-Output "Checking: $settingsPath"
if (-not (Test-Path -LiteralPath $settingsPath)) {
  Write-Output "File not found: $settingsPath"
  exit 1
}

$matches = Select-String -LiteralPath $settingsPath -Pattern '"$schema"' -CaseSensitive -SimpleMatch
if ($matches) {
  Write-Output "Found occurrences of \"$schema\" in settings.json:"
  $matches | ForEach-Object { "${($_.Path)}:${($_.LineNumber)}: ${($_.Line)}" }
} else {
  Write-Output "No occurrences of \"$schema\" in settings.json."
}

Write-Output "\nScanning recursively for other settings.json files with $schema under $settingsDir\.."
$all = Get-ChildItem -Path (Join-Path $env:APPDATA 'Windsurf') -Filter 'settings.json' -Recurse -ErrorAction SilentlyContinue
foreach ($f in $all) {
  $m = Select-String -LiteralPath $f.FullName -Pattern '"$schema"' -CaseSensitive -SimpleMatch -ErrorAction SilentlyContinue
  if ($m) {
    Write-Output "${($f.FullName)}:${($m.LineNumber)}: ${($m.Line)}"
  }
}

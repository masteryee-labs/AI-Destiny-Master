param(
  [string]$Root = "c:\\Users\\MasterYee\\CascadeProjects\\AIDestinyMaster",
  [string]$Tag = "v2025.10.18"
)

$pubDist = Join-Path $Root "tmp_push_dist\\dist"
$assetsPath = Join-Path $pubDist "assets.json"
if (!(Test-Path $assetsPath)) { Write-Error "assets.json not found: $assetsPath"; exit 1 }

# Update URLs in assets.json
$base = "https://github.com/masteryee-labs/AI-Destiny-Master/releases/download/$Tag"
$obj = Get-Content $assetsPath -Raw | ConvertFrom-Json
foreach ($a in $obj.assets) {
  $a.url = "$base/$($a.name)"
}
($obj | ConvertTo-Json -Depth 8) | Set-Content -Encoding UTF8 $assetsPath

# Generate checksums.json from assets.json (use existing size and sha256)
$checks = [pscustomobject]@{ files = @() }
$now = [DateTime]::UtcNow.ToString("yyyy-MM-ddTHH:mm:ssZ")
foreach ($a in $obj.assets) {
  $checks.files += [pscustomobject]@{
    name        = $a.name
    sha256      = ($a.sha256 | ForEach-Object { $_.ToUpper() })
    size_bytes  = [int64]$a.size
    generated_at= $now
  }
}
$checksPath = Join-Path $pubDist "checksums.json"
($checks | ConvertTo-Json -Depth 8) | Set-Content -Encoding UTF8 $checksPath

Write-Host "Updated: $assetsPath"
Write-Host "Wrote:   $checksPath"

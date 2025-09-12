param(
  [string]$Timezone = "Asia/Taipei",
  [string]$Lang = "zh-TW"
)

$ErrorActionPreference = "Stop"

function Ensure-Tool($cmd, $installHint) {
  try {
    & $cmd --version *> $null
    return $true
  } catch {
    Write-Host "[ERROR] '$cmd' is not available. $installHint" -ForegroundColor Red
    return $false
  }
}

# 1) Ensure Ruby / Gem available (we do not auto-install; show hints instead)
$rubyOk = Ensure-Tool ruby "Please install Ruby (e.g. with Chocolatey: choco install ruby -y)"
if (-not $rubyOk) { exit 1 }

try { & gem -v *> $null } catch { Write-Host "[ERROR] 'gem' not available. RubyGems is required." -ForegroundColor Red; exit 1 }

# 2) Ensure solar_terms_24 gem exists
$hasGem = (& gem list -i solar_terms_24) -eq "true"
if (-not $hasGem) {
  Write-Host "[INFO] Installing gem 'solar_terms_24'..."
  & gem install solar_terms_24 | Out-Host
}

function New-OrderedDictionary {
  return New-Object System.Collections.Specialized.OrderedDictionary
}

function Write-Json($map, $outPath) {
  $dir = Split-Path -Parent $outPath
  if (-not (Test-Path $dir)) { New-Item -ItemType Directory -Force -Path $dir | Out-Null }
  $sb = New-Object System.Text.StringBuilder
  [void]$sb.AppendLine("{")
  $keys = $map.Keys | Sort-Object
  for ($i=0; $i -lt $keys.Count; $i++) {
    $k = $keys[$i]
    $v = $map[$k]
    $line = '  "{0}": "{1}"' -f $k, ($v -replace '\\','\\\\' -replace '"','\"')
    if ($i -lt $keys.Count - 1) { $line += "," }
    [void]$sb.AppendLine($line)
  }
  [void]$sb.AppendLine("}")
  $content = $sb.ToString()
  Set-Content -Path $outPath -Encoding UTF8 -NoNewline -Value $content
  Write-Host "[OK] Wrote $outPath ($($content.Length) bytes)"
}

# Mapping for Chinese names to match our project naming if needed (kept identity here)
function Normalize-Name($name) { return $name }

$shardA = New-OrderedDictionary  # 1900-1999
$shardB = New-OrderedDictionary  # 2000-2099

for ($year = 1900; $year -le 2100; $year++) {
  Write-Host "[RUN] solar_terms_24 list $year --timezone=$Timezone --lang=$Lang"
  $output = & solar_terms_24 list $year --timezone=$Timezone --lang=$Lang
  # Expected lines: "# 小寒: 2022-01-05 17:14" etc. Join to single string and split by '#'
  $joined = ($output | Out-String)
  $parts = $joined -split '#'
  foreach ($p in $parts) {
    $line = $p.Trim()
    if ([string]::IsNullOrWhiteSpace($line)) { continue }
    # pattern: NAME: YYYY-MM-DD HH:MM(:SS optional)
    if ($line -match '^(?<name>[^:]+):\s*(?<dt>\d{4}-\d{2}-\d{2})\s+\d{2}:\d{2}(:\d{2})?$') {
      $name = Normalize-Name($Matches['name'].Trim())
      $date = $Matches['dt']
      $yNum = [int]$date.Substring(0,4)
      if ($yNum -ge 1900 -and $yNum -le 1999) { $shardA[$date] = $name }
      elseif ($yNum -ge 2000 -and $yNum -le 2099) { $shardB[$date] = $name }
      elseif ($yNum -eq 2100) {
        # 2100 will be in next shard; keep for later if needed
      }
    }
  }
}

# 3) Write shards
Write-Json -map $shardA -outPath "core/lunar/src/test/resources/solar_terms_1900_1999.json"
Write-Json -map $shardB -outPath "core/lunar/src/test/resources/solar_terms_2000_2099.json"

Write-Host "[DONE] Exported shards 1900_1999 and 2000_2099. You can now run :core:lunar:test and enable long-range sampling if desired."

<#
  prepare_models.ps1
  - Download/verify model files into repo models/ directory (skip if already present)
  - Optional environment variables:
      MODEL_URL_TINYLLAMA         TinyLlama ONNX file URL (.onnx)
      MODEL_SHA256_TINYLLAMA      TinyLlama ONNX file SHA256 (64 hex)
      AIDM_MODELS_DIR             Target directory (default: repo/models)
  - If URL not provided, the script will skip download (not treated as error)

  Requirements: Windows PowerShell 5+ or PowerShell 7+
#>

$ErrorActionPreference = 'Stop'

function Write-Info($msg)  { Write-Host "[prepare_models] $msg" -ForegroundColor Cyan }
function Write-Warn($msg)  { Write-Host "[prepare_models] $msg" -ForegroundColor Yellow }
function Write-Err($msg)   { Write-Host "[prepare_models] $msg" -ForegroundColor Red }

# Read env
$modelsDir = $env:AIDM_MODELS_DIR
if ([string]::IsNullOrWhiteSpace($modelsDir)) {
  # Default: repo/models
  $repoRoot = Split-Path -Parent $PSScriptRoot
  $modelsDir = Join-Path $repoRoot 'models'
}

# Manage TinyLlama ONNX model for now; extend as needed
$modelUrlTiny = $env:MODEL_URL_TINYLLAMA
$modelShaTiny = $env:MODEL_SHA256_TINYLLAMA

# Targets
$targetOnnx = Join-Path $modelsDir 'tinyllama-q8.onnx'
$targetSha  = Join-Path $modelsDir 'tinyllama-q8.onnx.sha256'

# Util: compute SHA256
function Get-FileSha256($path) {
  if (-not (Test-Path $path)) { return $null }
  $h = Get-FileHash -Path $path -Algorithm SHA256
  return $h.Hash.ToLowerInvariant()
}

# Util: download with retry
function Download-File($url, $dest, [int]$retries = 3) {
  for ($i = 1; $i -le $retries; $i++) {
    try {
      Write-Info "download: $url -> $dest (try $i/$retries)"
      Invoke-WebRequest -Uri $url -OutFile $dest -UseBasicParsing -TimeoutSec 180
      return $true
    } catch {
      Write-Warn "download failed (try $i/$retries): $($_.Exception.Message)"
      Start-Sleep -Seconds ([Math]::Min(5 * $i, 20))
    }
  }
  return $false
}

# Ensure directory
if (-not (Test-Path $modelsDir)) {
  New-Item -ItemType Directory -Path $modelsDir | Out-Null
  Write-Info "create dir: $modelsDir"
}

# If file exists, verify if SHA is provided
if (Test-Path $targetOnnx) {
  Write-Info "exists: $targetOnnx"
  if (-not [string]::IsNullOrWhiteSpace($modelShaTiny)) {
    $hash = Get-FileSha256 $targetOnnx
    if ($hash -ne $modelShaTiny.ToLowerInvariant()) {
      Write-Warn "existing file SHA256 mismatch, will re-download"
      Remove-Item $targetOnnx -Force
    } else {
      Write-Info "SHA256 verified, skip download"
    }
  }
}

if (-not (Test-Path $targetOnnx)) {
  if ([string]::IsNullOrWhiteSpace($modelUrlTiny)) {
    Write-Warn "MODEL_URL_TINYLLAMA not provided, skip tinyllama-q8.onnx"
  } else {
    $ok = Download-File -url $modelUrlTiny -dest $targetOnnx -retries 3
    if (-not $ok) { throw "download failed: $modelUrlTiny" }

    if (-not [string]::IsNullOrWhiteSpace($modelShaTiny)) {
      $hash = Get-FileSha256 $targetOnnx
      if ($hash -ne $modelShaTiny.ToLowerInvariant()) {
        throw "SHA256 mismatch after download: $hash != $modelShaTiny"
      }
      Write-Info "downloaded and verified: $targetOnnx"
      Set-Content -Path $targetSha -Value $modelShaTiny -Encoding Ascii
    } else {
      Write-Warn "SHA256 not provided, downloaded without verification: $targetOnnx"
    }
  }
}

Write-Info "model preparation done"

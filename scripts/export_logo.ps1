param(
    [string]$SvgPath = "docs/brand/logo.svg",
    [string]$OutPng = "docs/brand/logo_120.png",
    [int]$Size = 120
)

$ErrorActionPreference = "Stop"

function Fail($msg) {
    Write-Error $msg
    exit 1
}

if (-not (Test-Path $SvgPath)) {
    Fail "SVG not found: $SvgPath"
}

# Try to find Inkscape
$inkscape = Get-Command inkscape -ErrorAction SilentlyContinue
if (-not $inkscape) {
    Write-Host "Inkscape not found in PATH. Please install Inkscape and ensure 'inkscape.exe' is available."
    Write-Host "Download: https://inkscape.org/release/"
    exit 2
}

# Create output directory if needed
$dir = Split-Path -Parent $OutPng
if ($dir -and -not (Test-Path $dir)) {
    New-Item -ItemType Directory -Path $dir | Out-Null
}

# Inkscape CLI export
& $inkscape.Source "--export-type=png" "--export-width=$Size" "--export-height=$Size" "--export-filename=$OutPng" $SvgPath

if ($LASTEXITCODE -ne 0) {
    Fail "Inkscape export failed with exit code $LASTEXITCODE"
}

# Verify size and file exists
if (-not (Test-Path $OutPng)) {
    Fail "Output PNG not created: $OutPng"
}

$fi = Get-Item $OutPng
Write-Host "Exported: $($fi.FullName)  Size: $([Math]::Round($fi.Length/1KB,2)) KB"

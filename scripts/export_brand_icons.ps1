param(
    [string]$SvgPath = "../docs/brand/logo.svg",
    [string]$OutDir = "../docs/brand"
)

$ErrorActionPreference = 'Stop'

function Has-Command {
  param([string]$Name)
  return [bool](Get-Command $Name -ErrorAction SilentlyContinue)
}

$resolvedSvg = Resolve-Path -LiteralPath $SvgPath
$resolvedOut = Resolve-Path -LiteralPath $OutDir

$targets = @(48,120,192,256,512,1024)

Write-Host "Exporting icons from" $resolvedSvg "to" $resolvedOut

if (Has-Command inkscape) {
  Write-Host "Using Inkscape for SVG rasterization"
  foreach ($size in $targets) {
    $outFile = Join-Path $resolvedOut ("logo_" + $size + ".png")
    inkscape -w $size -h $size $resolvedSvg -o $outFile
    Write-Host " ✓ $outFile"
  }
}
elseif (Has-Command magick) {
  Write-Host "Using ImageMagick (librsvg recommended)"
  foreach ($size in $targets) {
    $outFile = Join-Path $resolvedOut ("logo_" + $size + ".png")
    magick -background none $resolvedSvg -resize ${size}x${size} $outFile
    Write-Host " ✓ $outFile"
  }
}
else {
  Write-Error "Neither Inkscape nor ImageMagick (magick) found in PATH. Please install one and re-run."
}

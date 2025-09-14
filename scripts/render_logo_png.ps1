param(
    [string]$OutPng = "docs/brand/logo_120.png",
    [int]$Size = 120
)

$ErrorActionPreference = 'Stop'

Add-Type -AssemblyName System.Drawing

# Create bitmap and graphics
$bmp = New-Object System.Drawing.Bitmap($Size, $Size, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$g = [System.Drawing.Graphics]::FromImage($bmp)
$g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias

# Colors
$dark = [System.Drawing.ColorTranslator]::FromHtml('#10131A')
$gold = [System.Drawing.ColorTranslator]::FromHtml('#C9A46A')
$white = [System.Drawing.Color]::FromArgb(255,255,255,255)
$highlight = [System.Drawing.Color]::FromArgb([int](0.18*255), 255, 255, 255)

# Scale from 512-based SVG geometry to output size
$scale = $Size / 512.0
function S([double]$v) { return [float]($v * $scale) }

# Background transparent
$g.Clear([System.Drawing.Color]::FromArgb(0,0,0,0))

# Central dark sphere (center 256,256 radius 220)
$cx512 = 256.0; $cy512 = 256.0; $r512 = 220.0
$bgRect = New-Object System.Drawing.RectangleF((S($cx512 - $r512)), (S($cy512 - $r512)), (S(2*$r512)), (S(2*$r512)))
$g.FillEllipse((New-Object System.Drawing.SolidBrush($dark)), $bgRect)

# Orbit rings on bounding box (80,80)-(432,432)
$bbX = 80.0; $bbY = 80.0; $bbW = 432.0 - 80.0; $bbH = 432.0 - 80.0
$penGold = New-Object System.Drawing.Pen($gold, (S(8.0)))
$orbitRect = New-Object System.Drawing.RectangleF((S($bbX)), (S($bbY)), (S($bbW)), (S($bbH)))
# Approximate angles to resemble the SVG path strokes
# Top-left arc
$g.DrawArc($penGold, $orbitRect, 200, 80)
# Bottom-right arc
$g.DrawArc($penGold, $orbitRect, 20, 80)

# Compass 8-pointed star centered at (256,232)
$starCx = S(256.0); $starCy = S(232.0)
$outer = S(64.0); $inner = S(26.0)
$pts = @()
for ($i=0; $i -lt 8; $i++) {
    $angleOuter = ($i * 45) * [Math]::PI / 180.0
    $angleInner = ($i * 45 + 22.5) * [Math]::PI / 180.0
    $pts += New-Object System.Drawing.PointF(($starCx + $outer * [Math]::Cos($angleOuter)), ($starCy + $outer * [Math]::Sin($angleOuter)))
    $pts += New-Object System.Drawing.PointF(($starCx + $inner * [Math]::Cos($angleInner)), ($starCy + $inner * [Math]::Sin($angleInner)))
}
$brushWhite = New-Object System.Drawing.SolidBrush($white)
$g.FillPolygon($brushWhite, $pts)

# Inner golden dot at (256,232) r=10
$dotR = S(10.0)
$g.FillEllipse((New-Object System.Drawing.SolidBrush($gold)), ($starCx - $dotR), ($starCy - $dotR), (2*$dotR), (2*$dotR))

# Subtle highlight arc similar to SVG path M140,210c...
# Use an arc near the top with thicker stroke scaled from 8px
$penHighlight = New-Object System.Drawing.Pen($highlight, (S(8.0)))
$hiRect = New-Object System.Drawing.RectangleF((S(120)), (S(88)), (S(300)), (S(300)))
$g.DrawArc($penHighlight, $hiRect, 210, 50)

# Ensure directory exists and save PNG
$dir = Split-Path -Parent $OutPng
if ($dir -and -not (Test-Path $dir)) { New-Item -ItemType Directory -Path $dir | Out-Null }

$bmp.Save($OutPng, [System.Drawing.Imaging.ImageFormat]::Png)

$g.Dispose()
$bmp.Dispose()

$fi = Get-Item $OutPng
Write-Host "Exported: $($fi.FullName)  Size: $([Math]::Round($fi.Length/1KB,2)) KB"

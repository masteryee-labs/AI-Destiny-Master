# Requires: Windows PowerShell 5+ or PowerShell 7+
# 目的：在本機（離線環境可用）準備 TinyLlama 權重，匯出 ONNX、生成 SHA-256，並可選打包為 assets/models.zip
# 注意：請勿將模型權重提交到版本庫。此腳本只在本機執行。

param(
    [string]$ModelId = "TinyLlama/TinyLlama-1.1B-Chat-v1.0",
    [string]$PythonExe = "python",
    [switch]$CreateZip
)

$ErrorActionPreference = "Stop"

Write-Host "[1/5] 準備 Python 環境(可重用現有 venv)" -ForegroundColor Cyan
$venvPath = Join-Path $PSScriptRoot ".venv-onnx"
if (!(Test-Path $venvPath)) {
    & $PythonExe -m venv $venvPath
}
$pip = Join-Path $venvPath "Scripts/pip.exe"
$py  = Join-Path $venvPath "Scripts/python.exe"

Write-Host "[2/5] 安裝所需套件(transformers/optimum/onnx/onnxruntime/onnxruntime-tools)" -ForegroundColor Cyan
& $pip install --upgrade pip > $null
& $pip install transformers optimum onnx onnxruntime onnxruntime-tools > $null

Write-Host "[3/5] 匯出與量化(呼叫 scripts/export_to_onnx.py)" -ForegroundColor Cyan
$env:AIDM_MODEL_ID = $ModelId
$scriptPath = Join-Path $PSScriptRoot "export_to_onnx.py"
$outDir = Join-Path $PSScriptRoot "..\models"
& $py "$scriptPath" --model "$ModelId" --out "$outDir"

# 檢查輸出
$modelsDir = Join-Path $PSScriptRoot "..\models"
$onnxPath = Join-Path $modelsDir "tinyllama-q8.onnx"
$shaPath  = Join-Path $modelsDir "tinyllama-q8.onnx.sha256"
$tokDir   = Join-Path $modelsDir "tokenizer"
if (!(Test-Path $onnxPath)) { throw "未找到 $onnxPath" }
if (!(Test-Path $shaPath))  { throw "未找到 $shaPath" }
if (!(Test-Path $tokDir))   { throw "未找到 tokenizer 目錄：$tokDir" }

Write-Host "[4/5] 驗證 SHA-256" -ForegroundColor Cyan
$shaExpected = Get-Content -Raw -Encoding UTF8 $shaPath
$shaExpected = $shaExpected.Trim()
# 計算檔案 SHA256
$shaObj = Get-FileHash -Algorithm SHA256 $onnxPath
if ($shaObj.Hash.ToLower() -ne $shaExpected.ToLower()) {
    $nl = [Environment]::NewLine
    Write-Warning ("SHA256 不一致:" + $nl + "  檔案: " + $shaObj.Hash + $nl + "  檔案記錄: " + $shaExpected)
} else {
    Write-Host "SHA256 驗證通過：$shaExpected" -ForegroundColor Green
}

if ($CreateZip) {
    Write-Host "[5/5] 打包為 assets/models.zip(供 App 首次啟動解壓)" -ForegroundColor Cyan
    $assetsDir = Join-Path $PSScriptRoot "..\app\src\main\assets"
    if (!(Test-Path $assetsDir)) { New-Item -ItemType Directory -Path $assetsDir | Out-Null }
    $zipPath = Join-Path $assetsDir "models.zip"

    if (Test-Path $zipPath) { Remove-Item $zipPath -Force }

    # 將 models 目錄壓縮為 zip（使用 Compress-Archive 以提升相容性）
    Compress-Archive -Path (Join-Path $modelsDir '*') -DestinationPath $zipPath -Force
    Write-Host "已生成：$zipPath" -ForegroundColor Green
}

Write-Host "完成。輸出目錄：$modelsDir" -ForegroundColor Green

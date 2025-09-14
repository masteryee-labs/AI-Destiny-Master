# AI Destiny Master：命理・紫微・占星（工作代號）

AI Destiny Master 是一款以 Jetpack Compose 與本機推論為核心、聚焦「紫微／八字／占星」的 Android App。專案目標是以低維護成本提供可離線運作的命理計算與 AI 解讀，避免雲端後端依賴與費用。

## 目標
- 離線 AI（優先採 ONNX Runtime Mobile，小模型）
- 命理計算：紫微、八字、星盤、農曆曆法等
- 簡潔直覺的 Compose UI/UX，支援 N+1 綜合解讀
- 極低維護成本：無自架後端、最小化外部 API 依賴

## 建置需求
- JDK：OpenJDK/Temurin 17（`JAVA_HOME` 指向 JDK 17）
- Android SDK：Command-line Tools、Platform-Tools
- Gradle：使用專案自帶 Wrapper（無需手動安裝）

## 快速建置
- Windows PowerShell：`./gradlew.bat assembleDebug`
- macOS/Linux：`./gradlew assembleDebug`
- 執行單元測試：`./gradlew test`

輸出位置（預設）：`app/build/outputs/` 目錄下。

## 準備 LLM 權重（TinyLlama → ONNX + 量化）
注意：請勿將模型權重提交到版本庫。以下操作在本機離線環境執行即可。

1) 以 PowerShell 準備/匯出（建議）

```powershell
# Windows PowerShell（繁中環境無需更改編碼）
# 可選指定模型 ID（預設 TinyLlama/TinyLlama-1.1B-Chat-v1.0）
pwsh -NoLogo -NoProfile -ExecutionPolicy Bypass -File scripts/prepare_llm.ps1 -ModelId "TinyLlama/TinyLlama-1.1B-Chat-v1.0" -CreateZip
```

腳本會：
- 建立虛擬環境、安裝 transformers/optimum/onnxruntime 等
- 呼叫 `scripts/export_to_onnx.py` 匯出 ONNX 並保存 tokenizer
- 生成 `models/tinyllama-q8.onnx.sha256`
- 若加上 `-CreateZip`，會將 `models/` 打包為 `app/src/main/assets/models.zip`，App 首啟會自動解壓到 `files/models/`

2) 直接呼叫 Python 腳本（自訂環境）

```powershell
# 先於你的 venv 安裝： transformers optimum onnx onnxruntime onnxruntime-tools
$env:AIDM_MODEL_ID = "TinyLlama/TinyLlama-1.1B-Chat-v1.0"
python scripts/export_to_onnx.py --model $env:AIDM_MODEL_ID --out scripts/../models
```

輸出：
- `models/tinyllama-q8.onnx`
- `models/tinyllama-q8.onnx.sha256`
- `models/tokenizer/*`

備註：
- 可使用企業/本地鏡像，將模型 ID 指向本地路徑或設定 `AIDM_MODEL_ID`
- 請確認 `.gitignore` 已排除 `models/`（不提交權重）

3) 使用企業鏡像或本地路徑

```powershell
# 企業鏡像（私有 HF 代理）
pwsh -File scripts/prepare_llm.ps1 -ModelId "https://mirror.example.com/TinyLlama-1.1B-Chat-v1.0" -CreateZip

# 本地路徑（已下載解壓的模型目錄）
pwsh -File scripts/prepare_llm.ps1 -ModelId "D:\Models\TinyLlama-1.1B-Chat-v1.0" -CreateZip

# 或直接以 Python 腳本（自訂 venv）
$env:AIDM_MODEL_ID = "D:\Models\TinyLlama-1.1B-Chat-v1.0"
python scripts/export_to_onnx.py --model $env:AIDM_MODEL_ID --out scripts/../models
```

4) 首次啟動驗證（自動解壓 + SHA-256）

```powershell
./gradlew.bat :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb logcat | findstr AIDestinyMaster
```

期望行為：
- 首啟會將 `app/src/main/assets/models.zip` 解壓至 App 私有目錄 `files/models/`
- 讀取 `files/models/tinyllama-q8.onnx.sha256`，驗證 `tinyllama-q8.onnx` 的 SHA-256
- 驗證通過會在日誌顯示 "validateModelChecksum=true" 類似訊息；失敗則提示不一致

## 專案結構（規劃中，可能調整）
- `:app`：Application（Compose）
- `:core:*`：核心演算法/工具（astro、lunar、ai 等）
- `:data`：資料層（Room / DataStore）
- `:sync`：雲端同步（Google Sign-In / Drive App Folder）
- `:features:*`：功能模組（bazi、ziwei、astrochart、almanac 等）

## 開發流程（簡要）
- 分支策略：
  - `main`：穩定主線
  - `feature/<topic>`：新功能／重構
  - `chore/<topic>`：雜項、設定、文件
- Commit 規範（Conventional Commits）：`feat|fix|docs|chore|refactor|test|build|ci: 描述…`
- 推送：預設推送至 `origin main`

## 授權
- 專案授權：待定（TBD）
- 第三方授權彙整：請見 `LICENSES.txt`

---
本 README 僅為初版草稿，後續將隨模組化與發行流程更新。

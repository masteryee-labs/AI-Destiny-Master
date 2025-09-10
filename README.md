# AIDestinyMaster

一個以 Android 為主的命理/占星工具專案，目標逐步整合：紫微斗數、八字、占星盤、Human Design、易經與黃曆等模組，並提供在地化（繁體中文）體驗與可擴充的模組化架構。

## 功能目標（概述）
- 模組化：`:app`, `:core:*`, `:features:*`, `:data`, `:sync`, `:billing`, `:ads`。
- 本地計算為主：盡量避免雲端依賴，可離線運作。
- AI 輔助：後續以 ONNX Runtime Mobile 在端側執行精簡模型（如 TinyLlama）。
- 隱私優先：個資與報告皆以本地加密儲存，可選擇同步到雲端 App Folder。

## 建置需求
- JDK 17（Temurin / Adoptium 建議）
- Android SDK 與 Build-Tools（詳見 `task.md`/`plan.md`）
- Gradle（隨 Wrapper）

## 快速開始
```bash
# 於專案根目錄
./gradlew tasks        # 檢查可用任務
./gradlew assembleDebug
```

Windows PowerShell 可改用：
```powershell
# 於專案根目錄
./gradlew.bat tasks
./gradlew.bat assembleDebug
```

## 專案結構（規劃中）
- `:app`：主應用，Compose UI、Navigation、設定等。
- `:core:ai`：ONNX 推論、Tokenizer、Prompt 組裝。
- `:features:*`：各命理/占星功能模組（bazi/ziwei/astrochart/design/almanac/mix-ai）。
- `:data`：Room/Datastore，本地加密與資料模型。
- `:sync`：Google Sign-In 與 Drive App Folder 同步。
- `:billing` / `:ads`：內購與廣告整合。

## 授權
- 專案本體授權待定（暫時保留）。
- 第三方授權彙整請見 `LICENSES.txt`，目前先留白，後續整合各套件/資料來源時補上。

## 聯絡
若有建議或想法，歡迎開 Issue（目前私有倉庫，可先以筆記方式彙整）。

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

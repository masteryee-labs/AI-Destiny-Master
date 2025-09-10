# 貢獻指南（單人開發亦適用）

## 分支策略
- `main`：穩定主線，隨時可發行
- `feature/<topic>`：功能開發／重構
- `chore/<topic>`：設定、CI、文件等雜項

## Commit 規範（Conventional Commits）
- 類型：`feat|fix|docs|chore|refactor|test|build|ci`
- 範例：`feat(report): 新增紫微綜合解讀段落`

## 變更流程
1) 建立分支並實作
2) 自我檢查（lint/test/build）
3) 對 `main` 建立 Pull Request（或直接以 Squash 合併）
4) 合併後刪除分支

## 版本與釋出（預留）
- 標記：`vX.Y.Z`
- 發行內容、相容性與破壞性變更請於 Release Notes 記錄

## 風格與編碼
- 檔案編碼：UTF-8（請勿引入亂碼）
- 行尾：依系統預設（Windows CRLF / *nix LF 皆可）
- 提交前建議先跑：`./gradlew build`（或至少 `./gradlew assembleDebug`）


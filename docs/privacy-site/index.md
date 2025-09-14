---
layout: default
title: 隱私權政策 | Privacy Policy
---

# 隱私權政策 Privacy Policy

最後更新：2025-09-12

本政策適用於「AI 命理大師」(AI Destiny Master) Android 應用程式（以下稱「本 App」）。我們重視您的隱私，並致力以「離線優先、最小必要」為原則處理資料。

## 我們收集哪些資料 What We Collect

- 出生與命盤資料（使用者輸入）
  - 包含生日、時間、時區、地點等，以便產生八字／紫微／占星／能量相關分析。
  - 預設僅儲存在您的裝置本機（Room / DataStore），不會自動上傳至任何伺服器。
- 使用偏好（本機）
  - 語言、主題、是否啟用同步等設定，儲存在 Android DataStore。
- 內購與權益（本機＋Google Play）
  - 僅保存必要的購買權益資訊（如 SKU 與授權狀態），以便於離線存取與還原；實際交易資訊由 Google Play Billing 管理。
- 廣告識別與展示（如啟用）
  - 透過 Google Mobile Ads SDK（AdMob）顯示測試／正式廣告。
- 錯誤紀錄（最小化）
  - 僅在必要時於本機紀錄關鍵錯誤訊息（不含個資），以利除錯。

We collect only the minimum data required to provide features. Most data stays on-device by default. Purchases are handled by Google Play Billing.

## 我們如何使用資料 How We Use Data

- 產生離線 AI 文字分析（ONNX Runtime Mobile；TinyLlama 或其他支援模型）
- 計算命盤（八字、紫微、占星、黃曆與卦象等）
- 儲存／檢視報告與備註（本機）
- 依您設定執行背景任務（WorkManager）
- 驗證付費權益與顯示廣告（如啟用）

## 離線與同步 Offline and Sync

- 預設「離線」：所有個人資料與內容僅存在本機。
- 可選「雲端同步」：以 Google 帳號登入後，可將必要資料同步至 Google Drive App Folder（由 Google 帳號專屬且非公開）。
- 同步目的僅為備份與跨裝置使用；不會對外分享，亦不會建立公開檔案。

By default, all content remains on-device. Enabling sync stores a private backup in your Google Drive App Folder for your account only.

## 資料保存與安全 Retention & Security

- 本機資料：使用 Android Room / DataStore 儲存；敏感欄位可透過 AndroidX Security Crypto 加密。
- 雲端資料（如啟用）：儲存在您的 Google Drive App Folder（受 Google 帳號保護）。
- 我們不營運自有雲端伺服器，不會將個人資料傳送到第三方伺服器（除 Google SDK 與您啟用之 Google 雲端同步）。

We use on-device storage and optional Google Drive App Folder sync. We do not run our own backend for personal data.

## 加密與欄位 Encryption & Data Fields

- 本機資料庫（Room）：可將敏感欄位（如出生時間、地點、Email）透過 AndroidX Security Crypto（AES-GCM）加密保存（逐步導入）。
- 偏好（DataStore）：僅保存必要旗標（語言、主題、同步、導覽完成等）。
- 欄位範例（依功能不同略有差異）：
  - 出生年月日、時分、時區、出生地（城市/經緯度）
  - 計算後的命盤/星盤數據（行星黃經、相位、卦象代碼、黃曆文字）
  - 生成報告（文字內容、時間戳、收藏/備註）
  - 權益資訊（SKU、是否已承認 acknowledged、更新時間）

We plan to encrypt sensitive fields (e.g., birth time/place, email) on-device using AndroidX Security Crypto.

## AI 模型與權重來源 AI Models & Weights

- 本 App 使用 ONNX Runtime Mobile 執行離線語言模型（例如 TinyLlama 或相容模型）。
- 權重檔案（.onnx）與 tokenizer 由使用者端裝置載入與執行，不會上傳至我們的伺服器。
- 若釋出更新版模型，我們將於版本資訊或官方文件註明出處與授權（如適用）。

We run the language model fully on-device via ONNX Runtime. Model updates and attributions will be documented appropriately.

## 第三方服務 Third-party SDKs

- Google Play Billing（內購與訂閱）
- Google Mobile Ads（AdMob／Rewarded Ads）
- Google Identity / Credential Manager（登入）
- Google Drive API（可選同步）
- Google Play Services（相依元件）

Please review Google’s respective privacy policies for these SDKs.

## 使用者權利 Your Rights

- 存取：您可於 App 內檢視輸入與生成內容。
- 更正：可隨時更新出生資料與設定。
- 刪除：您可刪除報告與資料；若有啟用同步，也可於 Google Drive 刪除對應備份。
- 匯出：我們將提供「匯出」功能（逐步推出）以利您匯出個人資料或報告。
- 撤回同意：可於設定中關閉同步、移除 Google 登入，或重置導覽。（某些功能可能因此受限）

You can access, correct, delete, and export your data. You may withdraw consent for sync or ads anytime in Settings.

## 兒少保護 Children’s Privacy

本 App 之內容屬一般大眾使用，不特別鎖定 13 歲以下兒童。若您為未成年人，請在監護人同意下使用。

Not directed to children under 13. If you are a minor, use the App with guardian consent.

## 政策更新 Updates

如政策更新，我們會於本頁公告「最後更新」日期並在 App 合適位置提醒。重大變更將以醒目方式提示。

We will update this page and display changes in-app when necessary.

## 聯絡我們 Contact

如有隱私問題或資料請求，請至「聯絡方式」頁：

- 繁中／English: [Support](./support)

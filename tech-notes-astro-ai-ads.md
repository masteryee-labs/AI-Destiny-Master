# 核心技術筆記（Astro / ONNX / Ads & Billing）

## 1. 天文計算模組 (`core/astro/`)

### 1.1 重新導入 Astronomy Engine
- **目標**：以 Astronomy Engine 提供的高精度 API 取代現有 `AstroCalculator.kt`、`HouseSystem.kt`、`Aspects.kt` 的簡化版本，支援多緯度與 ΔT 校正。
- **主要 API**：
  - `Observer(latitude, longitude, elevation)`：建立觀測者座標。
  - `Time.FromTerrestrialDate(year, month, day, hour)` 或 `Time(julianDay - JULIAN_DAY_J2000)`：處理時刻。
  - `BodyVector()`／`Equator()` 搭配 `Aberration.Corrected`、`Refraction.None`：取得真實黃道座標。
  - `Horizon(time, observer, azimuthDegrees, altitudeDegrees)`：必要時校驗地平坐標。
  - `SiderealTime(time)`：作為宮位計算基礎。
  - ΔT 來源：內建 `Time.DeltaT()` 或 Astronomy Engine 官方 ΔT 表。

### 1.2 行星位置 (`AstroCalculator.computePlanets()`)
- **實作要點**：
  - 使用 `Equator(body, time, observer, Aberration.Corrected)` 取得赤道座標，再轉換成黃道經度。
  - 採預設 10 顆行星（Sun, Moon, Mercury … Pluto），保留擴充空間。
  - 以 `normalizeDegrees()` 確保 0–360 度；若需顯示逆行資訊，保留速度向量。

### 1.3 宮位計算 (`HouseSystem.computeHouses()`)
- **支援制式**：Whole Sign + Porphyry（或 Placidus，視實作難度）。
- **演算法建議**：
  - Whole Sign：根據上升點所在星座起算每 30°。
  - Porphyry/Placidus：
    - 計算上升、天頂（MC），再以 Astronomy Engine 的黃道轉換求取。
    - 對於 Placidus，如要實作需以時間步進獲得中間宮線，或引用外部演算法。
- **多緯度測試**：考量高緯 (>66°) 可能無法使用 Placidus，因此需 fallback Whole Sign 或提示不支援。

### 1.4 相位判斷 (`Aspects.detectAspects()`)
- **可調參數**：
  - Orb 表：依行星對組設定（如：太陽/月亮 ±8°，慢行星 ±5°）。
  - 支援主要相位：0°, 60°, 90°, 120°, 180°。
- **產出資訊**：
  - `Aspect` 增加 `exactLongitudeDelta`、`orbTolerance` 等欄位以利 UI 顯示。

### 1.5 單元測試計畫
- **測試覆蓋**：
  - 緯度：`0°`、`25°N`、`25°S`、`60°N`、`60°S`。
  - 時間：`1990-01-01T00:00Z`、`2020-06-15T12:00Z`。
  - 借助 NASA JPL / Swiss Ephemeris 資料（或 Astronomy Engine 官方案例）作為基準。
- **容忍度**：
  - 行星經度誤差 ≤ ±0.5°。
  - 宮位起點 ≤ ±1.0°。
- **極端案例**：
  - 高緯度 + Porphyry/Placidus → 應 fallback 或提示。
  - ΔT 資料不足時 → 需記錄 warning，並允許外部注入 ΔT 表。
- **測試檔案**：`core/astro/src/test/java/.../AstroCalculatorTest.kt`、`HouseSystemTest.kt`、`AspectsTest.kt`。

---

## 2. ONNX TinyLlama 串流 (`core/ai/OnnxLlamaSession.kt`)

### 2.1 Tensor 名稱與形狀盤點
- **建議流程**：
  1. `session.inputInfo` / `session.outputInfo` 打印所有名稱與 shape。
  2. 若模型導出使用 HuggingFace `optimum.onnxruntime`，常見輸入：
     - `input_ids` `[batch, seq]`
     - `attention_mask` `[batch, seq]`
     - `position_ids` `[batch, seq]`
     - `past_key_values.X.key/value` 或 `past_key_values` 列表 (decoder-only)。
  3. 輸出常見：
     - `logits` `[batch, seq, vocab]`
     - `past_key_values_out`（沿用邀請）或多個 `present_key_value_X`。
  4. 透過 Netron 驗證 (model assets: `core/ai/src/main/assets/model/tinyllama-chat-8bit.onnx`)。

### 2.2 `resolveIO()` 擴充
- 建立 `IOMap`：
  - `inputIds`
  - `attentionMask`（可 null）
  - `positionIds`（若存在）
  - `pastKeys`, `pastValues`（以列表或對應 map 表示）
  - `logits`
  - `presentKeys`, `presentValues`
- 若模型不含 cache，fallback 仍需全量推理，但要明確記錄 warning。

### 2.3 `runDecodeLoop()` 改造
- **初始化**：
  - 首次推理：送整段 prompt；若模型支援 KV cache，取回 `present` 存入本地結構。
- **循環步驟**：
  1. 建立 `input_ids`（僅新 token）。
  2. 為 cache 模型附加 `past_key_values` 參數。
  3. 將 `attention_mask` 更新為過去 + 新 token 長度。
  4. 執行 session → 解析 `logits` 最後時刻向量。
  5. 進行 `softmax`、`topP` 選 token，加入輸出串流。
  6. 更新本地 `past`（以 `OrtValue` 或轉 FloatBuffer 的方式）。
- **串流輸出**：當 `sb` 長度超過閾值或遇到 `
` 時 emit UI。
- **錯誤處理**：遇到 `OrtException` 時，紀錄 tensor 名稱與序列長度後 fallback 至 `HumanReportBuilder()`。

### 2.4 除錯與分析
- 新增 `debugLogIO()`：含輸入名稱、shape、thread 訊息。
- 在 Debug build 允許 `simulateMode` 自動轉 off；若模型載入失敗，再回 stub。
- 於 `ReportGenerationWorker` 中捕捉 `onChunk` latency，寫入 log（optional 以 `BuildConfig.DEBUG` 控制）。

### 2.5 測試與驗證
- **單元測試**：使用 mock session 或 `InMemorySession`（必要時以接口抽象）模擬輸入輸出，確保 KV cache 管線與抽樣流程。
- **整合測試**：在儀器測試 (`androidTest`) 中設定短 prompt，確認 UI 串流正常。
- **效能**：於高記憶體裝置測 700 token 生成，記錄平均耗時與記憶體峰值。

---

## 3. Ads / Billing 強化

### 3.1 `ads/AdsManager.kt`
- **Consent (UMP)**：
  - 初始化 `UserMessagingPlatform.getConsentInformation()`。
  - 只有當 consent 所需時才展示對話框；否則立即呼叫 `MobileAds.initialize()`。
  - 暴露 `StateFlow<ConsentState>` 給 UI。
- **冷卻與上限**：
  - 在 `CoinsService` 或 Room `wallet` 表增加欄位：`lastRewardedAt`, `dailyAdCount`, `hourlyAdCount`。
  - `AdsManager.showRewarded()` 時先檢查限制；若超過，直接回傳錯誤事件。
- **錯誤回報**：
  - 導出 `SharedFlow<AdsEvent>`，涵蓋 `Loaded`, `FailedToLoad`, `Shown`, `Rewarded`, `CooldownBlocked`。
  - UI 根據事件顯示 Snackbar 或對話框。
- **測試範建議**：
  - 使用 fake clock 驗證冷卻邏輯。
  - 模擬 `LoadAdError`、`AdError`，確保事件傳遞完整。

### 3.2 `billing/`
- **SKU 管理**：
  - 在 `BuildConst.kt` 或新 `BillingSkus.kt` 定義常數：`ASTRO_DEEP_ONE`, `SUB_VIP_MONTH`, `SUB_VIP_YEAR`。
  - 建立 `SkuConfig` 資料類別包含 `type`, `title`, `coinsEquivalent` 等。
- **權益持久化**：
  - `EntitlementStore` 改用 DataStore / Room 保存；開機時載入後同步至 `BillingManager.entitlements`。
- **UI 互動**：
  - `BillingManager` 對外提供 `StateFlow<BillingUiState>`：載入中、可購買商品、購買中、完成、錯誤。
  - Spec 錯誤時 `Toast/Snackbar` 不能直接在 Manager 執行，改於 UI。
- **測試案例**：
  - `queryPurchasesAsync()` 在 reviewer VIP 模式下的覆蓋。
  - 模擬 `acknowledge` 失敗 -> 重新排程。
  - 使用 `BillingClient` 測試購買 (Google 提供測試卡) → 驗證 entitlement set。

---

## 4. 後續行動摘要

1. 完成上述筆記後，更新 `task.md`：
   - 將天文計算、AI 串流、Ads 冷卻、Billing 權益等錯誤的 `[x]` 改回 `[ ]`。
   - 在對應章節新增新的待辦（如：「重寫 AstroCalculator」、「ONNX KV cache 串流」、「Ads consent 與冷卻」等）。
2. 提交 MR/PR 前，連同此筆記一起說明修正範圍與測試規畫。

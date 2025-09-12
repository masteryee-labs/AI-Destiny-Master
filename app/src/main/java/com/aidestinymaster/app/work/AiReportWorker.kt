package com.aidestinymaster.app.work

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.aidestinymaster.app.notifications.Notifications
import com.aidestinymaster.core.ai.OnnxAiEngine
import com.aidestinymaster.data.repository.ReportRepository
import com.aidestinymaster.features.mix_ai.MixedSummary
import com.aidestinymaster.features.mix_ai.PromptBuilder
import java.io.File
import java.util.Locale

class AiReportWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    companion object {
        const val KEY_CHART_IDS = "chartIds"
        const val KEY_MODE = "mode" // 保留字串，暫不解析枚舉以避免跨模組依賴
        const val KEY_LOCALE = "locale"
        const val NOTI_ID = 1001
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        Notifications.ensureAnalysisChannel(applicationContext)
        val n = Notifications.buildProgress(
            applicationContext,
            title = "AI 綜合分析",
            text = "初始化中…",
            progress = 2,
            ongoing = true
        )
        return ForegroundInfo(NOTI_ID, n)
    }

    override suspend fun doWork(): Result {
        Notifications.ensureAnalysisChannel(applicationContext)
        setForeground(getForegroundInfo())
        try {
            val chartIds = inputData.getStringArray(KEY_CHART_IDS)?.toList().orEmpty()
            val modeStr = inputData.getString(KEY_MODE) ?: "Quick"
            val localeTag = inputData.getString(KEY_LOCALE) ?: Locale.TAIWAN.toLanguageTag()
            val locale = Locale.forLanguageTag(localeTag)

            updateProgress(10, "準備 Prompt…")
            // TODO: 待各模組資料聚合完成後，改為真實 MixedCollector.collectSummaries(...)
            val mixed = MixedSummary(
                // 可按需填入設計/紫微/八字/卦象/黃曆等，目前先最小化
            )
            val prompt = PromptBuilder.buildPrompt(mixed, locale)

            updateProgress(30, "啟動引擎…")
            val modelPath = File(applicationContext.filesDir, "models/tinyllama-q8.onnx").absolutePath
            val tokenizerPath = File(applicationContext.filesDir, "models/tokenizer").absolutePath
            val engine = OnnxAiEngine(applicationContext, modelPath, tokenizerPath)

            val sb = StringBuilder()
            updateProgress(50, "生成內容中…")
            val res = engine.generateStreaming(prompt, maxTokens = 256, temperature = 0.8f, topP = 0.9f) { chunk ->
                sb.append(chunk)
                // 簡單以長度近似進度（示意）
                val len = sb.length.coerceAtMost(5000)
                val p = 50 + (len * 40 / 5000)
                updateProgress(p, "生成中…")
            }
            val content = res.getOrElse { throwable ->
                // 失敗則至少回寫 Prompt 以利偵錯
                "[生成失敗，回寫 Prompt]\n\n$prompt\n\n錯誤：${throwable.message}"
            }

            updateProgress(95, "寫入報告…")
            val repo = ReportRepository.from(applicationContext)
            val reportId = repo.createFromAi(
                type = "mix-ai",
                chartId = chartIds.firstOrNull() ?: "mixed",
                content = content
            )

            // 完成通知（點擊導向 Report 詳情頁）
            val title = "AI 分析完成"
            val text = "點擊查看報告"
            val deepLink = Uri.parse("aidm://report/$reportId")
            val noti = Notifications.buildCompleted(applicationContext, title, text, deepLink)
            NotificationManagerCompat.from(applicationContext).notify(NOTI_ID + 1, noti)

            updateProgress(100, "完成")
            return Result.success(workDataOf("reportId" to reportId))
        } catch (t: Throwable) {
            return Result.failure(workDataOf("error" to (t.message ?: "unknown")))
        }
    }

    private fun updateProgress(percent: Int, msg: String) {
        val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val n = Notifications.buildProgress(
            applicationContext,
            title = "AI 綜合分析",
            text = msg,
            progress = percent,
            ongoing = true
        )
        nm.notify(NOTI_ID, n)
    }
}
